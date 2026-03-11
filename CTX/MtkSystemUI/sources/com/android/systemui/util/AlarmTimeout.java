package com.android.systemui.util;

import android.app.AlarmManager;
import android.os.Handler;
import android.os.SystemClock;

public class AlarmTimeout implements AlarmManager.OnAlarmListener {
    private final AlarmManager mAlarmManager;
    private final Handler mHandler;
    private final AlarmManager.OnAlarmListener mListener;
    private boolean mScheduled;
    private final String mTag;

    public AlarmTimeout(AlarmManager alarmManager, AlarmManager.OnAlarmListener onAlarmListener, String str, Handler handler) {
        this.mAlarmManager = alarmManager;
        this.mListener = onAlarmListener;
        this.mTag = str;
        this.mHandler = handler;
    }

    public void schedule(long j, int i) {
        switch (i) {
            case 0:
                if (this.mScheduled) {
                    throw new IllegalStateException(this.mTag + " timeout is already scheduled");
                }
                break;
            case 1:
                if (this.mScheduled) {
                    return;
                }
                break;
            case 2:
                if (this.mScheduled) {
                    cancel();
                }
                break;
            default:
                throw new IllegalArgumentException("Illegal mode: " + i);
        }
        this.mAlarmManager.setExact(2, SystemClock.elapsedRealtime() + j, this.mTag, this, this.mHandler);
        this.mScheduled = true;
    }

    public boolean isScheduled() {
        return this.mScheduled;
    }

    public void cancel() {
        if (this.mScheduled) {
            this.mAlarmManager.cancel(this);
            this.mScheduled = false;
        }
    }

    @Override
    public void onAlarm() {
        if (!this.mScheduled) {
            return;
        }
        this.mScheduled = false;
        this.mListener.onAlarm();
    }
}
