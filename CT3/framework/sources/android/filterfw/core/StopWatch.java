package android.filterfw.core;

import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Log;

class StopWatch {
    private int STOP_WATCH_LOGGING_PERIOD;
    private String mName;
    private String TAG = "MFF";
    private long mStartTime = -1;
    private long mTotalTime = 0;
    private int mNumCalls = 0;

    public StopWatch(String name) {
        this.STOP_WATCH_LOGGING_PERIOD = 200;
        this.mName = name;
        this.STOP_WATCH_LOGGING_PERIOD = SystemProperties.getInt("debug.swm.period", 200);
        Log.v(this.TAG, "StopWatch param: period= " + this.STOP_WATCH_LOGGING_PERIOD);
    }

    public void start() {
        if (this.mStartTime != -1) {
            throw new RuntimeException("Calling start with StopWatch already running");
        }
        this.mStartTime = SystemClock.elapsedRealtime();
    }

    public void stop() {
        if (this.mStartTime == -1) {
            throw new RuntimeException("Calling stop with StopWatch already stopped");
        }
        Frame.wait3DReady();
        long stopTime = SystemClock.elapsedRealtime();
        this.mTotalTime += stopTime - this.mStartTime;
        this.mNumCalls++;
        this.mStartTime = -1L;
        if (this.mNumCalls % this.STOP_WATCH_LOGGING_PERIOD != 0) {
            return;
        }
        Log.i(this.TAG, "AVG ms/call " + this.mName + ": " + String.format("%.1f", Float.valueOf((this.mTotalTime * 1.0f) / this.mNumCalls)));
        this.mTotalTime = 0L;
        this.mNumCalls = 0;
    }
}
