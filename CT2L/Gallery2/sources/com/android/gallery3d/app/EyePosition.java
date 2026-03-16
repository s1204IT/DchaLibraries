package com.android.gallery3d.app;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.util.FloatMath;
import android.view.Display;
import android.view.WindowManager;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.util.GalleryUtils;

public class EyePosition {
    private static final float USER_ANGEL = (float) Math.toRadians(10.0d);
    private static final float USER_ANGEL_COS = FloatMath.cos(USER_ANGEL);
    private static final float USER_ANGEL_SIN = FloatMath.sin(USER_ANGEL);
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

    public EyePosition(Context context, EyePositionListener listener) {
        this.mContext = context;
        this.mListener = listener;
        WindowManager wManager = (WindowManager) this.mContext.getSystemService("window");
        this.mDisplay = wManager.getDefaultDisplay();
    }

    public void resetPosition() {
        this.mStartTime = -1L;
        this.mY = 0.0f;
        this.mX = 0.0f;
        this.mZ = -this.mUserDistance;
        this.mListener.onEyePositionChanged(this.mX, this.mY, this.mZ);
    }

    private void onAccelerometerChanged(float gx, float gy, float gz) {
        float x = gx;
        float y = gy;
        switch (this.mDisplay.getRotation()) {
            case 1:
                x = -gy;
                y = gx;
                break;
            case 2:
                x = -gx;
                y = -gy;
                break;
            case 3:
                x = gy;
                y = -gx;
                break;
        }
        float temp = (x * x) + (y * y) + (gz * gz);
        float t = (-y) / temp;
        float tx = t * x;
        float ty = (-1.0f) + (t * y);
        float tz = t * gz;
        float length = FloatMath.sqrt((tx * tx) + (ty * ty) + (tz * tz));
        float glength = FloatMath.sqrt(temp);
        this.mX = Utils.clamp((((USER_ANGEL_COS * x) / glength) + ((USER_ANGEL_SIN * tx) / length)) * this.mUserDistance, -this.mLimit, this.mLimit);
        this.mY = -Utils.clamp((((USER_ANGEL_COS * y) / glength) + ((USER_ANGEL_SIN * ty) / length)) * this.mUserDistance, -this.mLimit, this.mLimit);
        this.mZ = -FloatMath.sqrt(((this.mUserDistance * this.mUserDistance) - (this.mX * this.mX)) - (this.mY * this.mY));
        this.mListener.onEyePositionChanged(this.mX, this.mY, this.mZ);
    }

    private void onGyroscopeChanged(float gx, float gy, float gz) {
        long now = SystemClock.elapsedRealtime();
        float distance = (gx > 0.0f ? gx : -gx) + (gy > 0.0f ? gy : -gy);
        if (distance < 0.15f || distance > 10.0f || this.mGyroscopeCountdown > 0) {
            this.mGyroscopeCountdown--;
            this.mStartTime = now;
            float limit = this.mUserDistance / 20.0f;
            if (this.mX > limit || this.mX < (-limit) || this.mY > limit || this.mY < (-limit)) {
                this.mX *= 0.995f;
                this.mY *= 0.995f;
                this.mZ = (float) (-Math.sqrt(((this.mUserDistance * this.mUserDistance) - (this.mX * this.mX)) - (this.mY * this.mY)));
                this.mListener.onEyePositionChanged(this.mX, this.mY, this.mZ);
                return;
            }
            return;
        }
        float t = ((now - this.mStartTime) / 1000.0f) * this.mUserDistance * (-this.mZ);
        this.mStartTime = now;
        float x = -gy;
        float y = -gx;
        switch (this.mDisplay.getRotation()) {
            case 1:
                x = -gx;
                y = gy;
                break;
            case 2:
                x = gy;
                y = gx;
                break;
            case 3:
                x = gx;
                y = -gy;
                break;
        }
        this.mX = Utils.clamp((float) (((double) this.mX) + (((double) (x * t)) / Math.hypot(this.mZ, this.mX))), -this.mLimit, this.mLimit) * 0.995f;
        this.mY = Utils.clamp((float) (((double) this.mY) + (((double) (y * t)) / Math.hypot(this.mZ, this.mY))), -this.mLimit, this.mLimit) * 0.995f;
        this.mZ = -FloatMath.sqrt(((this.mUserDistance * this.mUserDistance) - (this.mX * this.mX)) - (this.mY * this.mY));
        this.mListener.onEyePositionChanged(this.mX, this.mY, this.mZ);
    }

    private class PositionListener implements SensorEventListener {
        private PositionListener() {
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            switch (event.sensor.getType()) {
                case 1:
                    EyePosition.this.onAccelerometerChanged(event.values[0], event.values[1], event.values[2]);
                    break;
                case 4:
                    EyePosition.this.onGyroscopeChanged(event.values[0], event.values[1], event.values[2]);
                    break;
            }
        }
    }

    public void pause() {
        if (this.mSensor != null) {
            SensorManager sManager = (SensorManager) this.mContext.getSystemService("sensor");
            sManager.unregisterListener(this.mPositionListener);
        }
    }

    public void resume() {
        if (this.mSensor != null) {
            SensorManager sManager = (SensorManager) this.mContext.getSystemService("sensor");
            sManager.registerListener(this.mPositionListener, this.mSensor, 1);
        }
        this.mStartTime = -1L;
        this.mGyroscopeCountdown = 15;
        this.mY = 0.0f;
        this.mX = 0.0f;
        this.mZ = -this.mUserDistance;
        this.mListener.onEyePositionChanged(this.mX, this.mY, this.mZ);
    }
}
