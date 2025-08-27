package com.android.quickstep;

import android.annotation.TargetApi;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.VelocityTracker;

@TargetApi(28)
/* loaded from: classes.dex */
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

    /* JADX DEBUG: Method merged with bridge method: accept(Ljava/lang/Object;)V */
    @Override // java.util.function.Consumer
    public void accept(MotionEvent motionEvent) {
        this.mTarget.accept(motionEvent);
    }

    @Override // com.android.quickstep.TouchConsumer
    public void reset() {
        this.mTarget.reset();
    }

    @Override // com.android.quickstep.TouchConsumer
    public void updateTouchTracking(int i) {
        this.mTarget.updateTouchTracking(i);
    }

    @Override // com.android.quickstep.TouchConsumer
    public void onQuickScrubEnd() {
        this.mTarget.onQuickScrubEnd();
    }

    @Override // com.android.quickstep.TouchConsumer
    public void onQuickScrubProgress(float f) {
        this.mTarget.onQuickScrubProgress(f);
    }

    @Override // com.android.quickstep.TouchConsumer
    public void onQuickStep(MotionEvent motionEvent) {
        this.mTarget.onQuickStep(motionEvent);
    }

    @Override // com.android.quickstep.TouchConsumer
    public void onCommand(int i) {
        this.mTarget.onCommand(i);
    }

    @Override // com.android.quickstep.TouchConsumer
    public void preProcessMotionEvent(MotionEvent motionEvent) {
        this.mVelocityTracker.addMovement(motionEvent);
    }

    @Override // com.android.quickstep.TouchConsumer
    public Choreographer getIntrimChoreographer(MotionEventQueue motionEventQueue) {
        this.mMyQueue = motionEventQueue;
        return null;
    }

    @Override // com.android.quickstep.TouchConsumer
    public void deferInit() {
        this.mTarget = this.mTouchProvider.createTouchConsumer(this.mVelocityTracker);
        this.mTarget.getIntrimChoreographer(this.mMyQueue);
    }

    @Override // com.android.quickstep.TouchConsumer
    public boolean forceToLauncherConsumer() {
        return this.mTarget.forceToLauncherConsumer();
    }

    @Override // com.android.quickstep.TouchConsumer
    public boolean deferNextEventToMainThread() {
        TouchConsumer touchConsumer = this.mTarget;
        if (touchConsumer == null) {
            return true;
        }
        return touchConsumer.deferNextEventToMainThread();
    }

    @Override // com.android.quickstep.TouchConsumer
    public void onShowOverviewFromAltTab() {
        this.mTarget.onShowOverviewFromAltTab();
    }
}
