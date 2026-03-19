package com.android.deskclock.data;

import com.android.deskclock.Utils;

public final class Stopwatch {
    private final long mAccumulatedTime;
    private final long mLastStartTime;
    private final long mLastStartWallClockTime;
    private final State mState;
    static final long UNUSED = Long.MIN_VALUE;
    private static final Stopwatch RESET_STOPWATCH = new Stopwatch(State.RESET, UNUSED, UNUSED, 0);

    public enum State {
        RESET,
        RUNNING,
        PAUSED
    }

    Stopwatch(State state, long j, long j2, long j3) {
        this.mState = state;
        this.mLastStartTime = j;
        this.mLastStartWallClockTime = j2;
        this.mAccumulatedTime = j3;
    }

    public State getState() {
        return this.mState;
    }

    public long getLastStartTime() {
        return this.mLastStartTime;
    }

    public long getLastWallClockTime() {
        return this.mLastStartWallClockTime;
    }

    public boolean isReset() {
        return this.mState == State.RESET;
    }

    public boolean isPaused() {
        return this.mState == State.PAUSED;
    }

    public boolean isRunning() {
        return this.mState == State.RUNNING;
    }

    public long getTotalTime() {
        if (this.mState != State.RUNNING) {
            return this.mAccumulatedTime;
        }
        return this.mAccumulatedTime + Math.max(0L, Utils.now() - this.mLastStartTime);
    }

    public long getAccumulatedTime() {
        return this.mAccumulatedTime;
    }

    Stopwatch start() {
        if (this.mState == State.RUNNING) {
            return this;
        }
        return new Stopwatch(State.RUNNING, Utils.now(), Utils.wallClock(), getTotalTime());
    }

    Stopwatch pause() {
        if (this.mState != State.RUNNING) {
            return this;
        }
        return new Stopwatch(State.PAUSED, UNUSED, UNUSED, getTotalTime());
    }

    Stopwatch reset() {
        return RESET_STOPWATCH;
    }

    Stopwatch updateAfterReboot() {
        if (this.mState != State.RUNNING) {
            return this;
        }
        long jNow = Utils.now();
        long jWallClock = Utils.wallClock();
        return new Stopwatch(this.mState, jNow, jWallClock, this.mAccumulatedTime + Math.max(0L, jWallClock - this.mLastStartWallClockTime));
    }

    Stopwatch updateAfterTimeSet() {
        if (this.mState != State.RUNNING) {
            return this;
        }
        long jNow = Utils.now();
        long jWallClock = Utils.wallClock();
        long j = jNow - this.mLastStartTime;
        if (j < 0) {
            return this;
        }
        return new Stopwatch(this.mState, jNow, jWallClock, this.mAccumulatedTime + j);
    }
}
