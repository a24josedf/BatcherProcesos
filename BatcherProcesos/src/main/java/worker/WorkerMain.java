package worker;

import java.util.Locale;

public class WorkerMain {
    public static void main(String[] args) throws InterruptedException {
        if (args.length < 4) {
            System.err.println("Uso: WorkerMain <jobId> <durationMs> <cpuCores> <memMb>");
            System.exit(2);
        }

        String jobId = args[0];
        long durationMs = Long.parseLong(args[1]);
        int cpuCores = Integer.parseInt(args[2]);
        int memMb = Integer.parseInt(args[3]);
        long start = System.currentTimeMillis();
        long end = start + durationMs;

        while (System.currentTimeMillis() < end) {
            doWork(cpuCores);
            long now = System.currentTimeMillis();
            double progress = Math.min(1.0, (double) (now - start) / durationMs);
            System.out.println("{\"jobId\":\"" + jobId + "\",\"progress\":"
                    + String.format(Locale.US, "%.2f", progress)
                    + ",\"memMb\":" + memMb + "}");
            System.out.flush();
            Thread.sleep(500);
        }

        System.out.println("{\"jobId\":\"" + jobId + "\",\"progress\":1.00}");
        System.exit(0);
    }

    private static void doWork(int cpuCores) {
        long iterations = Math.max(1, cpuCores) * 15_000L;
        double value = 0;
        for (long i = 0; i < iterations; i++) {
            value += Math.sqrt(i + value);
        }
        if (value == -1) {
            System.out.println(value);
        }
    }
}
