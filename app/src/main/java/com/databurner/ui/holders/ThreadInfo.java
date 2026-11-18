package com.databurner.ui.holders;

public class ThreadInfo {
    public int threadId;
    public boolean isActive;
    public boolean isRunning;
    public float speedMb;

    public ThreadInfo(int threadId, boolean isActive, boolean isRunning, float speedMb) {
        this.threadId = threadId;
        this.isActive = isActive;
        this.isRunning = isRunning;
        this.speedMb = speedMb;
    }
}
