package service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import model.Job;
import model.JobState;
import model.SchedulerPolicy;

public class Batcher {
    private final int totalCores;
    private final int totalMemory;
    private final SchedulerPolicy policy;
    private final long quantum;
    private final List<Job> jobs = new ArrayList<>();
    private final Queue<Job> newQueue = new LinkedList<>();
    private final Queue<Job> readyQueue = new LinkedList<>();
    private final Queue<Job> waitingQueue = new LinkedList<>();
    private final Map<String, RunningJob> runningMap = new LinkedHashMap<>();
    private final List<Job> doneList = new ArrayList<>();
    private final List<Job> failedList = new ArrayList<>();
    private final Instant startTime = Instant.now();

    private int usedCores;
    private int usedMemory;

    public Batcher(int totalCores, int totalMemory, SchedulerPolicy policy, long quantum) {
        this.totalCores = totalCores;
        this.totalMemory = totalMemory;
        this.policy = policy;
        this.quantum = quantum;
    }

    public void loadJobs(List<Job> jobList) {
        for (Job job : jobList) {
            if (findById(job.getId()) != null) {
                System.err.println("[BATCHER] Job duplicado rechazado: " + job.getId());
                continue;
            }
            jobs.add(job);
            newQueue.add(job);
        }
    }

    public void run() throws InterruptedException {
        Runtime.getRuntime().addShutdownHook(new Thread(this::stopWorkers));

        while (!isFinished()) {
            admit();
            startReadyJobs();
            checkRunningJobs();
            showMonitor();
            Thread.sleep(250);
        }

        showMonitor();
        System.out.println("\nSimulacion terminada. DONE=" + doneList.size() + " FAILED=" + failedList.size());
    }

    private void admit() {
        moveToReady(newQueue);
        moveToReady(waitingQueue);
    }

    private void moveToReady(Queue<Job> queue) {
        int total = queue.size();
        for (int i = 0; i < total; i++) {
            Job job = queue.poll();
            if (job == null) {
                return;
            }

            if (job.getCpuCores() > totalCores || job.getMemMb() > totalMemory) {
                job.setState(JobState.FAILED);
                job.setErrorMessage("Pide mas recursos que los disponibles en el sistema");
                job.setEndTime(Instant.now());
                failedList.add(job);
            } else if (hasSpace(job)) {
                useResources(job);
                job.setState(JobState.READY);
                readyQueue.add(job);
            } else {
                job.setState(JobState.WAITING);
                waitingQueue.add(job);
            }
        }
    }

    private boolean hasSpace(Job job) {
        return usedCores + job.getCpuCores() <= totalCores
                && usedMemory + job.getMemMb() <= totalMemory;
    }

    private void useResources(Job job) {
        usedCores += job.getCpuCores();
        usedMemory += job.getMemMb();
    }

    private void freeResources(Job job) {
        usedCores -= job.getCpuCores();
        usedMemory -= job.getMemMb();
        if (usedCores < 0) {
            usedCores = 0;
        }
        if (usedMemory < 0) {
            usedMemory = 0;
        }
    }

    private void startReadyJobs() {
        int total = readyQueue.size();
        for (int i = 0; i < total; i++) {
            Job job = readyQueue.poll();
            if (job == null) {
                return;
            }

            try {
                startWorker(job);
            } catch (IOException e) {
                job.setState(JobState.FAILED);
                job.setErrorMessage("No se pudo lanzar WorkerMain: " + e.getMessage());
                job.setEndTime(Instant.now());
                freeResources(job);
                failedList.add(job);
            }

            if (policy == SchedulerPolicy.RR && !readyQueue.isEmpty()) {
                sleep(Math.min(quantum, 50));
            }
        }
    }

    private void startWorker(Job job) throws IOException {
        String javaBin = System.getProperty("java.home") + "\\bin\\java";
        String classpath = System.getProperty("java.class.path");
        ProcessBuilder builder = new ProcessBuilder(
                javaBin,
                "-cp",
                classpath,
                "worker.WorkerMain",
                job.getId(),
                Long.toString(job.getDurationMs()),
                Integer.toString(job.getCpuCores()),
                Integer.toString(job.getMemMb()));
        builder.redirectErrorStream(true);

        Process process = builder.start();
        job.setState(JobState.RUNNING);
        job.setStartTime(Instant.now());
        job.setPid(process.pid());
        runningMap.put(job.getId(), new RunningJob(job, process));
        readOutput(job, process);
    }

    private void readOutput(Job job, Process process) {
        Thread thread = new Thread(() -> {
            try (BufferedReader stdout = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = stdout.readLine()) != null) {
                    Double progress = getProgressFromLine(line);
                    if (progress != null) {
                        job.setProgress(progress);
                    }
                }
            } catch (IOException e) {
                job.setErrorMessage("Error leyendo stdout: " + e.getMessage());
            }
        }, "stdout-" + job.getId());
        thread.setDaemon(true);
        thread.start();
    }

    private void checkRunningJobs() {
        List<String> finished = new ArrayList<>();

        for (RunningJob running : runningMap.values()) {
            Process process = running.getProcess();
            if (!process.isAlive()) {
                finish(running, process.exitValue());
                finished.add(running.getJob().getId());
            }
        }

        for (String jobId : finished) {
            runningMap.remove(jobId);
        }
    }

    private Double getProgressFromLine(String line) {
        int marker = line.indexOf("\"progress\":");
        if (marker < 0) {
            return null;
        }
        int start = marker + "\"progress\":".length();
        int comma = line.indexOf(',', start);
        int brace = line.indexOf('}', start);
        int end;
        if (comma < 0 && brace < 0) {
            end = line.length();
        } else if (comma < 0) {
            end = brace;
        } else if (brace < 0) {
            end = comma;
        } else {
            end = Math.min(comma, brace);
        }
        try {
            return Double.parseDouble(line.substring(start, end).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void finish(RunningJob running, int exitCode) {
        Job job = running.getJob();
        job.setEndTime(Instant.now());
        if (exitCode == 0) {
            job.setProgress(1.0);
        }
        freeResources(job);

        if (exitCode == 0) {
            job.setState(JobState.DONE);
            doneList.add(job);
        } else {
            job.setState(JobState.FAILED);
            job.setErrorMessage("Worker finalizo con codigo " + exitCode);
            failedList.add(job);
        }
    }

    private void showMonitor() {
        clearConsole();
        String title = "BATCHER MONITOR - Politica: " + policy;
        if (policy == SchedulerPolicy.RR) {
            title = title + " (quantum=" + quantum + "ms)";
        }
        title = title + " - Uptime: " + formatDuration(Duration.between(startTime, Instant.now()));
        System.out.println(title);
        System.out.println("=".repeat(Math.max(80, title.length())));
        System.out.println("Recursos: CPU " + usedCores + "/" + totalCores
                + " | RAM " + usedMemory + "/" + totalMemory + " MB"
                + " | Utilizacion CPU: " + (usedCores * 100 / Math.max(1, totalCores)) + "%");
        System.out.println("-".repeat(80));
        System.out.println("READY  (" + readyQueue.size() + "): " + formatJobs(readyQueue));
        System.out.println("WAITING(" + waitingQueue.size() + "): " + formatJobs(waitingQueue));
        System.out.println("DONE   (" + doneList.size() + "): " + formatIds(doneList));
        System.out.println("FAILED (" + failedList.size() + "): " + formatIds(failedList));
        System.out.println("-".repeat(80));
        System.out.println("RUNNING(" + runningMap.size() + "):");
        System.out.printf("%-12s %-8s %-5s %-6s %-8s %-9s %-9s %-9s %-7s%n",
                "ID", "PID", "PRIO", "CORES", "MEM", "PROGRESO", "T_ESPERA", "T_EJEC", "ESTADO");

        for (RunningJob running : runningMap.values()) {
            Job job = running.getJob();
            Duration waitTime = Duration.between(job.getArrivalTime(), job.getStartTime());
            Duration runTime = Duration.between(job.getStartTime(), Instant.now());
            System.out.printf("%-12s %-8d %-5d %-6d %-8s %-9s %-9s %-9s %-7s%n",
                    job.getId(),
                    job.getPid(),
                    job.getPriority(),
                    job.getCpuCores(),
                    job.getMemMb() + "MB",
                    Math.round(job.getProgress() * 100) + "%",
                    formatDuration(waitTime),
                    formatDuration(runTime),
                    "RUN");
        }
    }

    private String formatJobs(Collection<Job> jobs) {
        return jobs.stream().map(Job::toString).toList().toString();
    }

    private String formatIds(Collection<Job> jobs) {
        return jobs.stream().map(Job::getId).toList().toString();
    }

    private String formatDuration(Duration duration) {
        long seconds = Math.max(0, duration.toSeconds());
        return String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
    }

    private void clearConsole() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private boolean isFinished() {
        return newQueue.isEmpty() && readyQueue.isEmpty() && waitingQueue.isEmpty() && runningMap.isEmpty();
    }

    private Job findById(String id) {
        return jobs.stream().filter(job -> job.getId().equals(id)).findFirst().orElse(null);
    }

    private void stopWorkers() {
        for (RunningJob running : runningMap.values()) {
            if (running.getProcess().isAlive()) {
                running.getProcess().destroy();
            }
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
