package com.android.gallery3d.app;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.util.GalleryUtils;

public class EyePosition {
    private static final float USER_ANGEL = (float) Math.toRadians(10.0d);
    private static final float USER_ANGEL_COS = (float) Math.cos(USER_ANGEL);
    private static final float USER_ANGEL_SIN = (float) Math.sin(USER_ANGEL);
    private Context mContext;
    private Display mDisplay;
    private EyePositionListener mListener;
    private Sensor mSensor;
    private float mX;
    private float mY;
    private float mZ;
    private long mStartTime = -1;
    private PositionListener mPositionListener = new PositionListener();
    private int mGyroscopeCountdown = 0;
    private final float mUserDistance = GalleryUtils.meterToPixel(0.3f);
    private final float mLimit = this.mUserDistance * 0.5f;

    public interface EyePositionListener {
        void onEyePositionChanged(float f, float f2, float f3);
    }

    public EyePosition(Context context, EyePositionListener eyePositionListener) {
        this.mContext = context;
        this.mListener = eyePositionListener;
        this.mDisplay = ((WindowManager) this.mContext.getSystemService("window")).getDefaultDisplay();
    }

    public void resetPosition() {
        this.mStartTime = -1L;
        this.mY = 0.0f;
        this.mX = 0.0f;
        this.mZ = -this.mUserDistance;
        this.mListener.onEyePositionChanged(this.mX, this.mY, this.mZ);
    }

    private void onAccelerometerChanged(float f, float f2, float f3) {
        switch (this.mDisplay.getRotation()) {
            case 1:
                f2 = -f2;
                float f4 = f2;
                f2 = f;
                f = f4;
                break;
            case 2:
                f = -f;
                f2 = -f2;
                break;
            case 3:
                f = -f;
                float f42 = f2;
                f2 = f;
                f = f42;
                break;
        }
        float f5 = (f * f) + (f2 * f2) + (f3 * f3);
        float f6 = (-f2) / f5;
        float f7 = f6 * f;
        float f8 = (-1.0f) + (f6 * f2);
        float f9 = f6 * f3;
        float fSqrt = (float) Math.sqrt((f7 * f7) + (f8 * f8) + (f9 * f9));
        float fSqrt2 = (float) Math.sqrt(f5);
        this.mX = Utils.clamp((((f * USER_ANGEL_COS) / fSqrt2) + ((f7 * USER_ANGEL_SIN) / fSqrt)) * this.mUserDistance, -this.mLimit, this.mLimit);
        this.mY = -Utils.clamp((((f2 * USER_ANGEL_COS) / fSqrt2) + ((f8 * USER_ANGEL_SIN) / fSqrt)) * this.mUserDistance, -this.mLimit, this.mLimit);
        this.mZ = (float) (-Math.sqrt(((this.mUserDistance * this.mUserDistance) - (this.mX * this.mX)) - (this.mY * this.mY)));
        this.mListener.onEyePositionChanged(this.mX, this.mY, this.mZ);
    }

    private void onGyroscopeChanged(float f, float f2, float f3) {
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        float f4 = (f > 0.0f ? f : -f) + (f2 > 0.0f ? f2 : -f2);
        if (f4 < 0.15f || f4 > 10.0f || this.mGyroscopeCountdown > 0) {
            this.mGyroscopeCountdown--;
            this.mStartTime = jElapsedRealtime;
            float f5 = this.mUserDistance / 20.0f;
            if (this.mX <= f5) {
                float f6 = -f5;
                if (this.mX >= f6 && this.mY <= f5 && this.mY >= f6) {
                    return;
                }
            }
            this.mX *= 0.995f;
            this.mY *= 0.995f;
            this.mZ = (float) (-Math.sqrt(((this.mUserDistance * this.mUserDistance) - (this.mX * this.mX)) - (this.mY * this.mY)));
            this.mListener.onEyePositionChanged(this.mX, this.mY, this.mZ);
            return;
        }
        float f7 = ((jElapsedRealtime - this.mStartTime) / 1000.0f) * this.mUserDistance * (-this.mZ);
        this.mStartTime = jElapsedRealtime;
        float f8 = -f2;
        float f9 = -f;
        switch (this.mDisplay.getRotation()) {
            case 1:
                f = f9;
                break;
            case 2:
                f2 = f;
                f = f2;
                break;
            case 3:
                f2 = f8;
                break;
            default:
                f = f8;
                f2 = f9;
                break;
        }
        this.mX = Utils.clamp((float) (((double) this.mX) + (((double) (f * f7)) / Math.hypot(this.mZ, this.mX))), -this.mLimit, this.mLimit) * 0.995f;
        this.mY = Utils.clamp((float) (((double) this.mY) + (((double) (f2 * f7)) / Math.hypot(this.mZ, this.mY))), -this.mLimit, this.mLimit) * 0.995f;
        this.mZ = (float) (-Math.sqrt(((this.mUserDistance * this.mUserDistance) - (this.mX * this.mX)) - (this.mY * this.mY)));
        this.mListener.onEyePositionChanged(this.mX, this.mY, this.mZ);
    }

    private class PositionListener implements SensorEventListener {
        private PositionListener() {
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            int type = sensorEvent.sensor.getType();
            if (type == 1) {
                EyePosition.this.onAccelerometerChanged(sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]);
            } else if (type == 4) {
                EyePosition.this.onGyroscopeChanged(sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]);
            }
        }
    }

    public void pause() {
        if (this.mSensor != null) {
            ((SensorManager) this.mContext.getSystemService("sensor")).unregisterListener(this.mPositionListener);
        }
    }

    public void resume() {
        if (this.mSensor != null) {
            ((SensorManager) this.mContext.getSystemService("sensor")).registerListener(this.mPositionListener, this.mSensor, 1);
        }
        this.mStartTime = -1L;
        this.mGyroscopeCountdown = 15;
        this.mY = 0.0f;
        this.mX = 0.0f;
        this.mZ = -this.mUserDistance;
        this.mListener.onEyePositionChanged(this.mX, this.mY, this.mZ);
    }

    public int getLayoutType() {
        if (this.mDisplay == null) {
            return -1;
        }
        WindowManager windowManager = (WindowManager) this.mContext.getSystemService("window");
        DisplayMetrics displayMetrics = new DisplayMetrics();
        if (Build.VERSION.SDK_INT >= 17) {
            windowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
        } else {
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        }
        if (displayMetrics.widthPixels < displayMetrics.heightPixels) {
            return 1;
        }
        return 0;
    }
}
