package com.android.quickstep;

import android.annotation.TargetApi;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.VelocityTracker;

@TargetApi(28)
public class DeferredTouchConsumer implements TouchConsumer {
    private MotionEventQueue mMyQueue;
    private TouchConsumer mTarget;
    private final DeferredTouchProvider mTouchProvider;
    private final VelocityTracker mVelocityTracker = VelocityTracker.obtain();

    public interface DeferredTouchProvider {
        TouchConsumer createTouchConsumer(VelocityTracker velocityTracker);
    }

    public DeferredTouchConsumer(DeferredTouchProvider deferredTouchProvider) {
        this.mTouchProvider = deferredTouchProvider;
    }

    @Override
    public void accept(MotionEvent motionEvent) {
        this.mTarget.accept(motionEvent);
    }

    @Override
    public void reset() {
        this.mTarget.reset();
    }

    @Override
    public void updateTouchTracking(int i) {
        this.mTarget.updateTouchTracking(i);
    }

    @Override
    public void onQuickScrubEnd() {
        this.mTarget.onQuickScrubEnd();
    }

    @Override
    public void onQuickScrubProgress(float f) {
        this.mTarget.onQuickScrubProgress(f);
    }

    @Override
    public void onQuickStep(MotionEvent motionEvent) {
        this.mTarget.onQuickStep(motionEvent);
    }

    @Override
    public void onCommand(int i) {
        this.mTarget.onCommand(i);
    }

    @Override
    public void preProcessMotionEvent(MotionEvent motionEvent) {
        this.mVelocityTracker.addMovement(motionEvent);
    }

    @Override
    public Choreographer getIntrimChoreographer(MotionEventQueue motionEventQueue) {
        this.mMyQueue = motionEventQueue;
        return null;
    }

    @Override
    public void deferInit() {
        this.mTarget = this.mTouchProvider.createTouchConsumer(this.mVelocityTracker);
        this.mTarget.getIntrimChoreographer(this.mMyQueue);
    }

    @Override
    public boolean forceToLauncherConsumer() {
        return this.mTarget.forceToLauncherConsumer();
    }

    @Override
    public boolean deferNextEventToMainThread() {
        TouchConsumer touchConsumer = this.mTarget;
        if (touchConsumer == null) {
            return true;
        }
        return touchConsumer.deferNextEventToMainThread();
    }

    @Override
    public void onShowOverviewFromAltTab() {
        this.mTarget.onShowOverviewFromAltTab();
    }
}
