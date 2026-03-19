package com.android.server.policy;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.os.Handler;
import java.io.PrintWriter;

public abstract class WakeGestureListener {
    private static final String TAG = "WakeGestureListener";
    private final Handler mHandler;
    private Sensor mSensor;
    private final SensorManager mSensorManager;
    private boolean mTriggerRequested;
    private final Object mLock = new Object();
    private final TriggerEventListener mListener = new TriggerEventListener() {
        @Override
        public void onTrigger(TriggerEvent triggerEvent) {
            synchronized (WakeGestureListener.this.mLock) {
                WakeGestureListener.this.mTriggerRequested = false;
                WakeGestureListener.this.mHandler.post(WakeGestureListener.this.mWakeUpRunnable);
            }
        }
    };
    private final Runnable mWakeUpRunnable = new Runnable() {
        @Override
        public void run() {
            WakeGestureListener.this.onWakeUp();
        }
    };

    public abstract void onWakeUp();

    public WakeGestureListener(Context context, Handler handler) {
        this.mSensorManager = (SensorManager) context.getSystemService("sensor");
        this.mHandler = handler;
        this.mSensor = this.mSensorManager.getDefaultSensor(23);
    }

    public boolean isSupported() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mSensor != null;
        }
        return z;
    }

    public void requestWakeUpTrigger() {
        synchronized (this.mLock) {
            if (this.mSensor != null && !this.mTriggerRequested) {
                this.mTriggerRequested = true;
                this.mSensorManager.requestTriggerSensor(this.mListener, this.mSensor);
            }
        }
    }

    public void cancelWakeUpTrigger() {
        synchronized (this.mLock) {
            if (this.mSensor != null && this.mTriggerRequested) {
                this.mTriggerRequested = false;
                this.mSensorManager.cancelTriggerSensor(this.mListener, this.mSensor);
            }
        }
    }

    public void dump(PrintWriter printWriter, String str) {
        synchronized (this.mLock) {
            printWriter.println(str + TAG);
            String str2 = str + "  ";
            printWriter.println(str2 + "mTriggerRequested=" + this.mTriggerRequested);
            printWriter.println(str2 + "mSensor=" + this.mSensor);
        }
    }
}
