package com.mediatek.camera.common.debug.profiler;

public abstract class ProfileBase implements IPerformanceProfile {
    protected LogFormatter mFormatter;
    private long mLastMark;
    private long mStartNanos;

    public ProfileBase(String str) {
        this.mFormatter = new LogFormatter(str);
    }

    @Override
    public final IPerformanceProfile start() {
        this.mStartNanos = System.nanoTime();
        this.mLastMark = this.mStartNanos;
        onStart();
        return this;
    }

    @Override
    public final void mark(String str) {
        long jNanoTime = System.nanoTime();
        onMark(getTotalMillis(jNanoTime), getTimeFromLastMillis(jNanoTime), str);
        this.mLastMark = jNanoTime;
    }

    @Override
    public final void stop() {
        long jNanoTime = System.nanoTime();
        onStop(getTotalMillis(jNanoTime), getTimeFromLastMillis(jNanoTime));
        this.mLastMark = jNanoTime;
    }

    protected void onStart() {
    }

    protected void onMark(double d, double d2, String str) {
    }

    protected void onStop(double d, double d2) {
    }

    private double getTotalMillis(long j) {
        return nanoToMillis(j - this.mStartNanos);
    }

    private double getTimeFromLastMillis(long j) {
        return nanoToMillis(j - this.mLastMark);
    }

    private double nanoToMillis(long j) {
        return j / 1000000.0d;
    }
}
