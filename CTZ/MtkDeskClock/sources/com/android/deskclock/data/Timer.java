package com.android.deskclock.data;

import android.text.TextUtils;
import com.android.deskclock.Utils;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public final class Timer {
    static final long MAX_LENGTH = 362439000;
    public static final long MIN_LENGTH = 1000;
    static final long UNUSED = Long.MIN_VALUE;
    private final boolean mDeleteAfterUse;
    private final int mId;
    private final String mLabel;
    private final long mLastStartTime;
    private final long mLastStartWallClockTime;
    private final long mLength;
    private final long mRemainingTime;
    private final State mState;
    private final long mTotalLength;
    static Comparator<Timer> ID_COMPARATOR = new Comparator<Timer>() {
        @Override
        public int compare(Timer timer, Timer timer2) {
            return Integer.compare(timer2.getId(), timer.getId());
        }
    };
    static Comparator<Timer> EXPIRY_COMPARATOR = new Comparator<Timer>() {
        private final List<State> stateExpiryOrder = Arrays.asList(State.MISSED, State.EXPIRED, State.RUNNING, State.PAUSED, State.RESET);

        @Override
        public int compare(Timer timer, Timer timer2) {
            int iCompare = Integer.compare(this.stateExpiryOrder.indexOf(timer.getState()), this.stateExpiryOrder.indexOf(timer2.getState()));
            if (iCompare == 0) {
                if (timer.getState() == State.RESET) {
                    return Long.compare(timer.getLength(), timer2.getLength());
                }
                return Long.compare(timer.getRemainingTime(), timer2.getRemainingTime());
            }
            return iCompare;
        }
    };

    public enum State {
        RUNNING(1),
        PAUSED(2),
        EXPIRED(3),
        RESET(4),
        MISSED(5);

        private final int mValue;

        State(int i) {
            this.mValue = i;
        }

        public int getValue() {
            return this.mValue;
        }

        public static State fromValue(int i) {
            for (State state : values()) {
                if (state.getValue() == i) {
                    return state;
                }
            }
            return null;
        }
    }

    Timer(int i, State state, long j, long j2, long j3, long j4, long j5, String str, boolean z) {
        this.mId = i;
        this.mState = state;
        this.mLength = j;
        this.mTotalLength = j2;
        this.mLastStartTime = j3;
        this.mLastStartWallClockTime = j4;
        this.mRemainingTime = j5;
        this.mLabel = str;
        this.mDeleteAfterUse = z;
    }

    public int getId() {
        return this.mId;
    }

    public State getState() {
        return this.mState;
    }

    public String getLabel() {
        return this.mLabel;
    }

    public long getLength() {
        return this.mLength;
    }

    public long getTotalLength() {
        return this.mTotalLength;
    }

    public boolean getDeleteAfterUse() {
        return this.mDeleteAfterUse;
    }

    public boolean isReset() {
        return this.mState == State.RESET;
    }

    public boolean isRunning() {
        return this.mState == State.RUNNING;
    }

    public boolean isPaused() {
        return this.mState == State.PAUSED;
    }

    public boolean isExpired() {
        return this.mState == State.EXPIRED;
    }

    public boolean isMissed() {
        return this.mState == State.MISSED;
    }

    public long getLastRemainingTime() {
        return this.mRemainingTime;
    }

    public long getRemainingTime() {
        if (this.mState == State.PAUSED || this.mState == State.RESET) {
            return this.mRemainingTime;
        }
        return this.mRemainingTime - Math.max(0L, Utils.now() - this.mLastStartTime);
    }

    public long getExpirationTime() {
        if (this.mState != State.RUNNING && this.mState != State.EXPIRED && this.mState != State.MISSED) {
            throw new IllegalStateException("cannot compute expiration time in state " + this.mState);
        }
        return this.mLastStartTime + this.mRemainingTime;
    }

    public long getWallClockExpirationTime() {
        if (this.mState != State.RUNNING && this.mState != State.EXPIRED && this.mState != State.MISSED) {
            throw new IllegalStateException("cannot compute expiration time in state " + this.mState);
        }
        return this.mLastStartWallClockTime + this.mRemainingTime;
    }

    public long getElapsedTime() {
        return getTotalLength() - getRemainingTime();
    }

    long getLastStartTime() {
        return this.mLastStartTime;
    }

    long getLastWallClockTime() {
        return this.mLastStartWallClockTime;
    }

    Timer start() {
        if (this.mState == State.RUNNING || this.mState == State.EXPIRED || this.mState == State.MISSED) {
            return this;
        }
        return new Timer(this.mId, State.RUNNING, this.mLength, this.mTotalLength, Utils.now(), Utils.wallClock(), this.mRemainingTime, this.mLabel, this.mDeleteAfterUse);
    }

    Timer pause() {
        if (this.mState == State.PAUSED || this.mState == State.RESET) {
            return this;
        }
        if (this.mState == State.EXPIRED || this.mState == State.MISSED) {
            return reset();
        }
        return new Timer(this.mId, State.PAUSED, this.mLength, this.mTotalLength, UNUSED, UNUSED, getRemainingTime(), this.mLabel, this.mDeleteAfterUse);
    }

    Timer expire() {
        if (this.mState == State.EXPIRED || this.mState == State.RESET || this.mState == State.MISSED) {
            return this;
        }
        return new Timer(this.mId, State.EXPIRED, this.mLength, 0L, Utils.now(), Utils.wallClock(), Math.min(0L, getRemainingTime()), this.mLabel, this.mDeleteAfterUse);
    }

    Timer miss() {
        if (this.mState == State.RESET || this.mState == State.MISSED) {
            return this;
        }
        return new Timer(this.mId, State.MISSED, this.mLength, 0L, Utils.now(), Utils.wallClock(), Math.min(0L, getRemainingTime()), this.mLabel, this.mDeleteAfterUse);
    }

    Timer reset() {
        if (this.mState == State.RESET) {
            return this;
        }
        return new Timer(this.mId, State.RESET, this.mLength, this.mLength, UNUSED, UNUSED, this.mLength, this.mLabel, this.mDeleteAfterUse);
    }

    Timer updateAfterReboot() {
        if (this.mState == State.RESET || this.mState == State.PAUSED) {
            return this;
        }
        long jNow = Utils.now();
        long jWallClock = Utils.wallClock();
        return new Timer(this.mId, this.mState, this.mLength, this.mTotalLength, jNow, jWallClock, this.mRemainingTime - Math.max(0L, jWallClock - this.mLastStartWallClockTime), this.mLabel, this.mDeleteAfterUse);
    }

    Timer updateAfterTimeSet() {
        if (this.mState == State.RESET || this.mState == State.PAUSED) {
            return this;
        }
        long jNow = Utils.now();
        long jWallClock = Utils.wallClock();
        long j = jNow - this.mLastStartTime;
        long j2 = this.mRemainingTime - j;
        if (j < 0) {
            return this;
        }
        return new Timer(this.mId, this.mState, this.mLength, this.mTotalLength, jNow, jWallClock, j2, this.mLabel, this.mDeleteAfterUse);
    }

    Timer setLabel(String str) {
        return TextUtils.equals(this.mLabel, str) ? this : new Timer(this.mId, this.mState, this.mLength, this.mTotalLength, this.mLastStartTime, this.mLastStartWallClockTime, this.mRemainingTime, str, this.mDeleteAfterUse);
    }

    Timer setLength(long j) {
        long j2;
        long j3;
        if (this.mLength == j || j <= 1000) {
            return this;
        }
        if (this.mState != State.RESET) {
            long j4 = this.mTotalLength;
            j2 = this.mRemainingTime;
            j3 = j4;
        } else {
            j3 = j;
            j2 = j3;
        }
        return new Timer(this.mId, this.mState, j, j3, this.mLastStartTime, this.mLastStartWallClockTime, j2, this.mLabel, this.mDeleteAfterUse);
    }

    Timer setRemainingTime(long j) {
        State state;
        long jNow;
        long jWallClock;
        if (this.mRemainingTime == j || this.mState == State.RESET) {
            return this;
        }
        long j2 = this.mTotalLength + (j - this.mRemainingTime);
        if (j > 0 && (this.mState == State.EXPIRED || this.mState == State.MISSED)) {
            state = State.RUNNING;
            jNow = Utils.now();
            jWallClock = Utils.wallClock();
        } else {
            state = this.mState;
            jNow = this.mLastStartTime;
            jWallClock = this.mLastStartWallClockTime;
        }
        long j3 = jWallClock;
        long j4 = jNow;
        return new Timer(this.mId, state, this.mLength, j2, j4, j3, j, this.mLabel, this.mDeleteAfterUse);
    }

    Timer addMinute() {
        if (this.mState == State.EXPIRED || this.mState == State.MISSED) {
            return setRemainingTime(60000L);
        }
        return setRemainingTime(this.mRemainingTime + 60000);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && getClass() == obj.getClass() && this.mId == ((Timer) obj).mId) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return this.mId;
    }
}
