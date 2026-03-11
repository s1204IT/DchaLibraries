package com.android.systemui.doze;

import android.R;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Trace;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.doze.DozeMachine;

public class DozeScreenBrightness implements SensorEventListener, DozeMachine.Part {
    private final Context mContext;
    private int mDefaultDozeBrightness;
    private final DozeHost mDozeHost;
    private final DozeMachine.Service mDozeService;
    private final Handler mHandler;
    private int mLastSensorValue;
    private final Sensor mLightSensor;
    private boolean mPaused;
    private boolean mRegistered;
    private final SensorManager mSensorManager;
    private final int[] mSensorToBrightness;
    private final int[] mSensorToScrimOpacity;

    public DozeScreenBrightness(Context context, DozeMachine.Service service, SensorManager sensorManager, Sensor sensor, DozeHost dozeHost, Handler handler, int i, int[] iArr, int[] iArr2) {
        this.mPaused = false;
        this.mLastSensorValue = -1;
        this.mContext = context;
        this.mDozeService = service;
        this.mSensorManager = sensorManager;
        this.mLightSensor = sensor;
        this.mDozeHost = dozeHost;
        this.mHandler = handler;
        this.mDefaultDozeBrightness = i;
        this.mSensorToBrightness = iArr;
        this.mSensorToScrimOpacity = iArr2;
    }

    @VisibleForTesting
    public DozeScreenBrightness(Context context, DozeMachine.Service service, SensorManager sensorManager, Sensor sensor, DozeHost dozeHost, Handler handler, AlwaysOnDisplayPolicy alwaysOnDisplayPolicy) {
        this(context, service, sensorManager, sensor, dozeHost, handler, context.getResources().getInteger(R.integer.config_doubleTapPowerGestureMode), alwaysOnDisplayPolicy.screenBrightnessArray, alwaysOnDisplayPolicy.dimmingScrimArray);
    }

    @Override
    public void transitionTo(DozeMachine.State state, DozeMachine.State state2) {
        switch (state2) {
            case INITIALIZED:
                resetBrightnessToDefault();
                break;
            case DOZE_AOD:
            case DOZE_REQUEST_PULSE:
                setLightSensorEnabled(true);
                break;
            case DOZE:
                setLightSensorEnabled(false);
                resetBrightnessToDefault();
                break;
            case FINISH:
                setLightSensorEnabled(false);
                break;
        }
        if (state2 != DozeMachine.State.FINISH) {
            setPaused(state2 == DozeMachine.State.DOZE_AOD_PAUSED);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Trace.beginSection("DozeScreenBrightness.onSensorChanged" + sensorEvent.values[0]);
        try {
            if (this.mRegistered) {
                this.mLastSensorValue = (int) sensorEvent.values[0];
                updateBrightnessAndReady();
            }
        } finally {
            Trace.endSection();
        }
    }

    private void updateBrightnessAndReady() {
        if (this.mRegistered) {
            int iComputeBrightness = computeBrightness(this.mLastSensorValue);
            boolean z = iComputeBrightness > 0;
            if (z) {
                this.mDozeService.setDozeScreenBrightness(iComputeBrightness);
            }
            int iComputeScrimOpacity = -1;
            if (this.mPaused) {
                iComputeScrimOpacity = 255;
            } else if (z) {
                iComputeScrimOpacity = computeScrimOpacity(this.mLastSensorValue);
            }
            if (iComputeScrimOpacity >= 0) {
                this.mDozeHost.setAodDimmingScrim(iComputeScrimOpacity / 255.0f);
            }
        }
    }

    private int computeScrimOpacity(int i) {
        if (i < 0 || i >= this.mSensorToScrimOpacity.length) {
            return -1;
        }
        return this.mSensorToScrimOpacity[i];
    }

    private int computeBrightness(int i) {
        if (i < 0 || i >= this.mSensorToBrightness.length) {
            return -1;
        }
        return this.mSensorToBrightness[i];
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    private void resetBrightnessToDefault() {
        this.mDozeService.setDozeScreenBrightness(this.mDefaultDozeBrightness);
        this.mDozeHost.setAodDimmingScrim(0.0f);
    }

    private void setLightSensorEnabled(boolean z) {
        if (z && !this.mRegistered && this.mLightSensor != null) {
            this.mRegistered = this.mSensorManager.registerListener(this, this.mLightSensor, 3, this.mHandler);
            this.mLastSensorValue = -1;
        } else if (!z && this.mRegistered) {
            this.mSensorManager.unregisterListener(this);
            this.mRegistered = false;
            this.mLastSensorValue = -1;
        }
    }

    private void setPaused(boolean z) {
        if (this.mPaused != z) {
            this.mPaused = z;
            updateBrightnessAndReady();
        }
    }
}
