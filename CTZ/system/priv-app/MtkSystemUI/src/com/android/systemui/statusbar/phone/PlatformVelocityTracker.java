package com.android.systemui.statusbar.phone;

import android.util.Pools;
import android.view.MotionEvent;
import android.view.VelocityTracker;

/* loaded from: classes.dex */
public class PlatformVelocityTracker implements VelocityTrackerInterface {
    private static final Pools.SynchronizedPool<PlatformVelocityTracker> sPool = new Pools.SynchronizedPool<>(2);
    private VelocityTracker mTracker;

    public static PlatformVelocityTracker obtain() {
        PlatformVelocityTracker platformVelocityTracker = (PlatformVelocityTracker) sPool.acquire();
        if (platformVelocityTracker == null) {
            platformVelocityTracker = new PlatformVelocityTracker();
        }
        platformVelocityTracker.setTracker(VelocityTracker.obtain());
        return platformVelocityTracker;
    }

    public void setTracker(VelocityTracker velocityTracker) {
        this.mTracker = velocityTracker;
    }

    @Override // com.android.systemui.statusbar.phone.VelocityTrackerInterface
    public void addMovement(MotionEvent motionEvent) {
        this.mTracker.addMovement(motionEvent);
    }

    @Override // com.android.systemui.statusbar.phone.VelocityTrackerInterface
    public void computeCurrentVelocity(int i) {
        this.mTracker.computeCurrentVelocity(i);
    }

    @Override // com.android.systemui.statusbar.phone.VelocityTrackerInterface
    public float getXVelocity() {
        return this.mTracker.getXVelocity();
    }

    @Override // com.android.systemui.statusbar.phone.VelocityTrackerInterface
    public float getYVelocity() {
        return this.mTracker.getYVelocity();
    }

    @Override // com.android.systemui.statusbar.phone.VelocityTrackerInterface
    public void recycle() {
        this.mTracker.recycle();
        sPool.release(this);
    }
}
