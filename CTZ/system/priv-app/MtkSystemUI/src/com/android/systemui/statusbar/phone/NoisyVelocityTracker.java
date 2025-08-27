package com.android.systemui.statusbar.phone;

import android.util.Pools;
import android.view.MotionEvent;
import java.util.ArrayDeque;
import java.util.Iterator;

/* loaded from: classes.dex */
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

    @Override // com.android.systemui.statusbar.phone.VelocityTrackerInterface
    public void addMovement(MotionEvent motionEvent) {
        if (this.mEventBuf.size() == 8) {
            this.mEventBuf.remove();
        }
        this.mEventBuf.add(new MotionEventCopy(motionEvent.getX(), motionEvent.getY(), motionEvent.getEventTime()));
    }

    @Override // com.android.systemui.statusbar.phone.VelocityTrackerInterface
    public void computeCurrentVelocity(int i) {
        this.mVY = 0.0f;
        this.mVX = 0.0f;
        Iterator<MotionEventCopy> it = this.mEventBuf.iterator();
        MotionEventCopy motionEventCopy = null;
        float f = 10.0f;
        float f2 = 0.0f;
        while (it.hasNext()) {
            MotionEventCopy next = it.next();
            if (motionEventCopy != null) {
                float f3 = (next.t - motionEventCopy.t) / i;
                float f4 = next.x - motionEventCopy.x;
                float f5 = next.y - motionEventCopy.y;
                if (next.t != motionEventCopy.t) {
                    this.mVX += (f4 * f) / f3;
                    this.mVY += (f5 * f) / f3;
                    f2 += f;
                    f *= 0.75f;
                }
            }
            motionEventCopy = next;
        }
        if (f2 > 0.0f) {
            this.mVX /= f2;
            this.mVY /= f2;
        } else {
            this.mVY = 0.0f;
            this.mVX = 0.0f;
        }
    }

    @Override // com.android.systemui.statusbar.phone.VelocityTrackerInterface
    public float getXVelocity() {
        if (Float.isNaN(this.mVX) || Float.isInfinite(this.mVX)) {
            this.mVX = 0.0f;
        }
        return this.mVX;
    }

    @Override // com.android.systemui.statusbar.phone.VelocityTrackerInterface
    public float getYVelocity() {
        if (Float.isNaN(this.mVY) || Float.isInfinite(this.mVX)) {
            this.mVY = 0.0f;
        }
        return this.mVY;
    }

    @Override // com.android.systemui.statusbar.phone.VelocityTrackerInterface
    public void recycle() {
        this.mEventBuf.clear();
        sNoisyPool.release(this);
    }
}
