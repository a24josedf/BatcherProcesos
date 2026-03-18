package model;

import java.sql.Timestamp;

/**
 *
 * @author carlos
 */
public class Job {
    
    private int id;
    private String name;
    private int priority;
    private int cpuCores;
    private String memMb;
    private int durationMs;
    private enum state { NEW, READY, WAITING, RUNNING, DONE, FAILED}
    private Timestamp arrivalTime;
    private Timestamp startTime;
    private Timestamp endTime;

    public Job(int id, String name, int priority, int cpuCores, String memMb, int durationMs, Timestamp arrivalTime, Timestamp startTime, Timestamp endTime) {
        this.id = id;
        this.name = name;
        this.priority = priority;
        this.cpuCores = cpuCores;
        this.memMb = memMb;
        this.durationMs = durationMs;
        this.arrivalTime = arrivalTime;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    
    
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getCpuCores() {
        return cpuCores;
    }

    public void setCpuCores(int cpuCores) {
        this.cpuCores = cpuCores;
    }

    public String getMemMb() {
        return memMb;
    }

    public void setMemMb(String memMb) {
        this.memMb = memMb;
    }

    public int getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(int durationMs) {
        this.durationMs = durationMs;
    }

    public Timestamp getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(Timestamp arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    public Timestamp getEndTime() {
        return endTime;
    }

    public void setEndTime(Timestamp endTime) {
        this.endTime = endTime;
    }
    
    
}
