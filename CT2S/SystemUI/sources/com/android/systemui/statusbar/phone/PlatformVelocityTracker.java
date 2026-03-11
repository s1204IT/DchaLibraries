package com.android.systemui.statusbar.phone;

import android.util.Pools;
import android.view.MotionEvent;
import android.view.VelocityTracker;

public class PlatformVelocityTracker implements VelocityTrackerInterface {
    private static final Pools.SynchronizedPool<PlatformVelocityTracker> sPool = new Pools.SynchronizedPool<>(2);
    private VelocityTracker mTracker;

    public static PlatformVelocityTracker obtain() {
        PlatformVelocityTracker tracker = (PlatformVelocityTracker) sPool.acquire();
        if (tracker == null) {
            tracker = new PlatformVelocityTracker();
        }
        tracker.setTracker(VelocityTracker.obtain());
        return tracker;
    }

    public void setTracker(VelocityTracker tracker) {
        this.mTracker = tracker;
    }

    @Override
    public void addMovement(MotionEvent event) {
        this.mTracker.addMovement(event);
    }

    @Override
    public void computeCurrentVelocity(int units) {
        this.mTracker.computeCurrentVelocity(units);
    }

    @Override
    public float getXVelocity() {
        return this.mTracker.getXVelocity();
    }

    @Override
    public float getYVelocity() {
        return this.mTracker.getYVelocity();
    }

    @Override
    public void recycle() {
        this.mTracker.recycle();
        sPool.release(this);
    }
}
