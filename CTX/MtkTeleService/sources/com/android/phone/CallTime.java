package com.android.phone;

import android.os.Debug;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.Connection;
import java.io.File;
import java.util.List;

public class CallTime extends Handler {
    private static final boolean DBG = false;
    private static final String LOG_TAG = "PHONE/CallTime";
    static final boolean PROFILE = true;
    private static final int PROFILE_STATE_NONE = 0;
    private static final int PROFILE_STATE_READY = 1;
    private static final int PROFILE_STATE_RUNNING = 2;
    private static int sProfileState = 0;
    private Call mCall;
    private long mInterval;
    private long mLastReportedTime;
    private OnTickListener mListener;
    private PeriodicTimerCallback mTimerCallback = new PeriodicTimerCallback();
    private boolean mTimerRunning;

    interface OnTickListener {
        void onTickForCallTimeElapsed(long j);
    }

    public CallTime(OnTickListener onTickListener) {
        this.mListener = onTickListener;
    }

    void setActiveCallMode(Call call) {
        this.mCall = call;
        this.mInterval = 1000L;
    }

    void reset() {
        this.mLastReportedTime = SystemClock.uptimeMillis() - this.mInterval;
    }

    void periodicUpdateTimer() {
        if (!this.mTimerRunning) {
            this.mTimerRunning = PROFILE;
            long jUptimeMillis = SystemClock.uptimeMillis();
            long j = this.mLastReportedTime;
            long j2 = this.mInterval;
            while (true) {
                j += j2;
                if (jUptimeMillis < j) {
                    break;
                } else {
                    j2 = this.mInterval;
                }
            }
            postAtTime(this.mTimerCallback, j);
            this.mLastReportedTime = j;
            if (this.mCall != null && this.mCall.getState() == Call.State.ACTIVE) {
                updateElapsedTime(this.mCall);
            }
            if (isTraceReady()) {
                startTrace();
            }
        }
    }

    void cancelTimer() {
        removeCallbacks(this.mTimerCallback);
        this.mTimerRunning = false;
    }

    private void updateElapsedTime(Call call) {
        if (this.mListener != null) {
            this.mListener.onTickForCallTimeElapsed(getCallDuration(call) / 1000);
        }
    }

    static long getCallDuration(Call call) {
        List connections = call.getConnections();
        int size = connections.size();
        if (size == 1) {
            return ((Connection) connections.get(0)).getDurationMillis();
        }
        long j = 0;
        for (int i = 0; i < size; i++) {
            long durationMillis = ((Connection) connections.get(i)).getDurationMillis();
            if (durationMillis > j) {
                j = durationMillis;
            }
        }
        return j;
    }

    private static void log(String str) {
        Log.d(LOG_TAG, "[CallTime] " + str);
    }

    private class PeriodicTimerCallback implements Runnable {
        PeriodicTimerCallback() {
        }

        @Override
        public void run() {
            if (CallTime.this.isTraceRunning()) {
                CallTime.this.stopTrace();
            }
            CallTime.this.mTimerRunning = false;
            CallTime.this.periodicUpdateTimer();
        }
    }

    static void setTraceReady() {
        if (sProfileState == 0) {
            sProfileState = 1;
            log("trace ready...");
        } else {
            log("current trace state = " + sProfileState);
        }
    }

    boolean isTraceReady() {
        if (sProfileState == 1) {
            return PROFILE;
        }
        return false;
    }

    boolean isTraceRunning() {
        if (sProfileState == 2) {
            return PROFILE;
        }
        return false;
    }

    void startTrace() {
        if ((sProfileState == 1) & PROFILE) {
            File dir = PhoneGlobals.getInstance().getDir("phoneTrace", 0);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String str = dir.getPath() + File.separator + "callstate";
            String str2 = str + ".key";
            File file = new File(str + ".data");
            if (file.exists()) {
                file.delete();
            }
            File file2 = new File(str2);
            if (file2.exists()) {
                file2.delete();
            }
            sProfileState = 2;
            log("startTrace");
            Debug.startMethodTracing(str, 8388608);
        }
    }

    void stopTrace() {
        if (sProfileState == 2) {
            sProfileState = 0;
            log("stopTrace");
            Debug.stopMethodTracing();
        }
    }
}
