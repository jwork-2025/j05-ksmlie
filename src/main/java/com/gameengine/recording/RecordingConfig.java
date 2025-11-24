package com.gameengine.recording;

public class RecordingConfig {
    private long warmupNs = 500_000_000L;
    private long intervalNs = 100_000_000L;
    private int[] keys = new int[]{32, 80, 87, 83, 65, 68, 38, 40, 37, 39, 49, 50, 51, 52, 53, 54, 55, 97, 98, 99, 100, 101, 102, 103};

    public long getWarmupNs() { return warmupNs; }
    public void setWarmupNs(long warmupNs) { this.warmupNs = warmupNs; }
    public long getIntervalNs() { return intervalNs; }
    public void setIntervalNs(long intervalNs) { this.intervalNs = intervalNs; }
    public int[] getKeys() { return keys; }
    public void setKeys(int[] keys) { this.keys = keys; }
}