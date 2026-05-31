package service;

import model.Job;

class RunningJob {
    private final Job job;
    private final Process process;

    RunningJob(Job job, Process process) {
        this.job = job;
        this.process = process;
    }

    Job getJob() {
        return job;
    }

    Process getProcess() {
        return process;
    }
}
