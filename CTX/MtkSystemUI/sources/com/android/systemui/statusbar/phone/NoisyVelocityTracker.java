package com.android.systemui.statusbar.phone;

import android.util.Pools;
import android.view.MotionEvent;
import java.util.ArrayDeque;

public class NoisyVelocityTracker implements VelocityTrackerInterface {
    private static final Pools.SynchronizedPool<NoisyVelocityTracker> sNoisyPool = new Pools.SynchronizedPool<>(2);
    private float mVX;
    private final int MAX_EVENTS = 8;
    private ArrayDeque<MotionEventCopy> mEventBuf = new ArrayDeque<>(8);
    private float mVY = 0.0f;

    private static class MotionEventCopy {
        long t;
        float x;
        float y;

        public MotionEventCopy(float f, float f2, long j) {
            this.x = f;
            this.y = f2;
            this.t = j;
        }
    }

    public static NoisyVelocityTracker obtain() {
        NoisyVelocityTracker noisyVelocityTracker = (NoisyVelocityTracker) sNoisyPool.acquire();
        return noisyVelocityTracker != null ? noisyVelocityTracker : new NoisyVelocityTracker();
    }

    private NoisyVelocityTracker() {
    }

    @Override
    public void addMovement(MotionEvent motionEvent) {
        if (this.mEventBuf.size() == 8) {
            this.mEventBuf.remove();
        }
        this.mEventBuf.add(new MotionEventCopy(motionEvent.getX(), motionEvent.getY(), motionEvent.getEventTime()));
    }

    @Override
    public void computeCurrentVelocity(int i) {
        this.mVY = 0.0f;
        this.mVX = 0.0f;
        MotionEventCopy motionEventCopy = null;
        float f = 10.0f;
        float f2 = 0.0f;
        for (MotionEventCopy motionEventCopy2 : this.mEventBuf) {
            if (motionEventCopy != null) {
                float f3 = (motionEventCopy2.t - motionEventCopy.t) / i;
                float f4 = motionEventCopy2.x - motionEventCopy.x;
                float f5 = motionEventCopy2.y - motionEventCopy.y;
                if (motionEventCopy2.t != motionEventCopy.t) {
                    this.mVX += (f4 * f) / f3;
                    this.mVY += (f5 * f) / f3;
                    f2 += f;
                    f *= 0.75f;
                }
            }
            motionEventCopy = motionEventCopy2;
        }
        if (f2 > 0.0f) {
            this.mVX /= f2;
            this.mVY /= f2;
        } else {
            this.mVY = 0.0f;
            this.mVX = 0.0f;
        }
    }

    @Override
    public float getXVelocity() {
        if (Float.isNaN(this.mVX) || Float.isInfinite(this.mVX)) {
            this.mVX = 0.0f;
        }
        return this.mVX;
    }

    @Override
    public float getYVelocity() {
        if (Float.isNaN(this.mVY) || Float.isInfinite(this.mVX)) {
            this.mVY = 0.0f;
        }
        return this.mVY;
    }

    @Override
    public void recycle() {
        this.mEventBuf.clear();
        sNoisyPool.release(this);
    }
}
