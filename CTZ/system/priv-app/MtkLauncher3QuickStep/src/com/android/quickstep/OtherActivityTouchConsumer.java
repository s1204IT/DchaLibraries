package com.android.quickstep;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Looper;
import android.os.SystemClock;
import android.util.SparseArray;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import com.android.launcher3.MainThreadExecutor;
import com.android.launcher3.util.TraceHelper;
import com.android.quickstep.util.RemoteAnimationTargetSet;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.shared.system.AssistDataReceiver;
import com.android.systemui.shared.system.BackgroundExecutor;
import com.android.systemui.shared.system.NavigationBarCompat;
import com.android.systemui.shared.system.RecentsAnimationControllerCompat;
import com.android.systemui.shared.system.RecentsAnimationListener;
import com.android.systemui.shared.system.RemoteAnimationTargetCompat;
import com.android.systemui.shared.system.WindowManagerWrapper;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@TargetApi(28)
/* loaded from: classes.dex */
public class OtherActivityTouchConsumer extends ContextWrapper implements TouchConsumer {
    private static final long LAUNCHER_DRAW_TIMEOUT_MS = 150;
    private int mActivePointerId;
    private final ActivityControlHelper mActivityControlHelper;
    private final SparseArray<RecentsAnimationState> mAnimationStates;
    private final Choreographer mBackgroundThreadChoreographer;
    private int mDisplayRotation;
    private final PointF mDownPos;
    private MotionEventQueue mEventQueue;
    private final Intent mHomeIntent;
    private WindowTransformSwipeHandler mInteractionHandler;
    private final boolean mIsDeferredDownTarget;
    private boolean mIsGoingToHome;
    private final PointF mLastPos;
    private final MainThreadExecutor mMainThreadExecutor;
    private final OverviewCallbacks mOverviewCallbacks;
    private boolean mPassedInitialSlop;
    private int mQuickStepDragSlop;
    private final RecentsModel mRecentsModel;
    private final ActivityManager.RunningTaskInfo mRunningTask;
    private Rect mStableInsets;
    private float mStartDisplacement;
    private VelocityTracker mVelocityTracker;

    public OtherActivityTouchConsumer(Context context, ActivityManager.RunningTaskInfo runningTaskInfo, RecentsModel recentsModel, Intent intent, ActivityControlHelper activityControlHelper, MainThreadExecutor mainThreadExecutor, Choreographer choreographer, int i, OverviewCallbacks overviewCallbacks, VelocityTracker velocityTracker) {
        super(context);
        this.mAnimationStates = new SparseArray<>();
        this.mDownPos = new PointF();
        this.mLastPos = new PointF();
        this.mActivePointerId = -1;
        this.mStableInsets = new Rect();
        this.mRunningTask = runningTaskInfo;
        this.mRecentsModel = recentsModel;
        this.mHomeIntent = intent;
        this.mVelocityTracker = velocityTracker;
        this.mActivityControlHelper = activityControlHelper;
        this.mMainThreadExecutor = mainThreadExecutor;
        this.mBackgroundThreadChoreographer = choreographer;
        this.mIsDeferredDownTarget = activityControlHelper.deferStartingActivity(i);
        this.mOverviewCallbacks = overviewCallbacks;
    }

    @Override // com.android.quickstep.TouchConsumer
    public void onShowOverviewFromAltTab() throws InterruptedException {
        startTouchTrackingForWindowAnimation(SystemClock.uptimeMillis());
    }

    /* JADX DEBUG: Method merged with bridge method: accept(Ljava/lang/Object;)V */
    @Override // java.util.function.Consumer
    public void accept(MotionEvent motionEvent) throws InterruptedException {
        if (this.mVelocityTracker == null) {
            return;
        }
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked != 6) {
            switch (actionMasked) {
                case 0:
                    TraceHelper.beginSection("TouchInt");
                    this.mActivePointerId = motionEvent.getPointerId(0);
                    this.mDownPos.set(motionEvent.getX(), motionEvent.getY());
                    this.mLastPos.set(this.mDownPos);
                    this.mPassedInitialSlop = false;
                    this.mQuickStepDragSlop = NavigationBarCompat.getQuickStepDragSlopPx();
                    if (!this.mIsDeferredDownTarget) {
                        startTouchTrackingForWindowAnimation(motionEvent.getEventTime());
                    }
                    this.mDisplayRotation = ((WindowManager) getSystemService(WindowManager.class)).getDefaultDisplay().getRotation();
                    WindowManagerWrapper.getInstance().getStableInsets(this.mStableInsets);
                    break;
                case 1:
                case 3:
                    TraceHelper.endSection("TouchInt");
                    finishTouchTracking(motionEvent);
                    break;
                case 2:
                    int iFindPointerIndex = motionEvent.findPointerIndex(this.mActivePointerId);
                    if (iFindPointerIndex != -1) {
                        this.mLastPos.set(motionEvent.getX(iFindPointerIndex), motionEvent.getY(iFindPointerIndex));
                        float displacement = getDisplacement(motionEvent);
                        if (!this.mPassedInitialSlop && !this.mIsDeferredDownTarget && Math.abs(displacement) > this.mQuickStepDragSlop) {
                            this.mPassedInitialSlop = true;
                            this.mStartDisplacement = displacement;
                        }
                        if (this.mPassedInitialSlop && this.mInteractionHandler != null) {
                            this.mInteractionHandler.updateDisplacement(displacement - this.mStartDisplacement);
                            break;
                        }
                    }
                    break;
            }
            return;
        }
        int actionIndex = motionEvent.getActionIndex();
        if (motionEvent.getPointerId(actionIndex) == this.mActivePointerId) {
            int i = actionIndex != 0 ? 0 : 1;
            this.mDownPos.set(motionEvent.getX(i) - (this.mLastPos.x - this.mDownPos.x), motionEvent.getY(i) - (this.mLastPos.y - this.mDownPos.y));
            this.mLastPos.set(motionEvent.getX(i), motionEvent.getY(i));
            this.mActivePointerId = motionEvent.getPointerId(i);
        }
    }

    private void notifyGestureStarted() {
        if (this.mInteractionHandler == null) {
            return;
        }
        this.mOverviewCallbacks.closeAllWindows();
        ActivityManagerWrapper.getInstance().closeSystemWindows(ActivityManagerWrapper.CLOSE_SYSTEM_WINDOWS_REASON_RECENTS);
        this.mInteractionHandler.onGestureStarted();
    }

    private boolean isNavBarOnRight() {
        return this.mDisplayRotation == 1 && this.mStableInsets.right > 0;
    }

    private boolean isNavBarOnLeft() {
        return this.mDisplayRotation == 3 && this.mStableInsets.left > 0;
    }

    private void startTouchTrackingForWindowAnimation(long j) throws InterruptedException {
        final RecentsAnimationState recentsAnimationState = new RecentsAnimationState();
        final WindowTransformSwipeHandler windowTransformSwipeHandler = new WindowTransformSwipeHandler(recentsAnimationState.id, this.mRunningTask, this, j, this.mActivityControlHelper);
        this.mRecentsModel.loadTasks(this.mRunningTask.id, null);
        this.mInteractionHandler = windowTransformSwipeHandler;
        final MotionEventQueue motionEventQueue = this.mEventQueue;
        Objects.requireNonNull(motionEventQueue);
        windowTransformSwipeHandler.setGestureEndCallback(new Runnable() { // from class: com.android.quickstep.-$$Lambda$acy6R1xKzaFdF7tRPnHofYaRRNY
            @Override // java.lang.Runnable
            public final void run() {
                motionEventQueue.reset();
            }
        });
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        windowTransformSwipeHandler.setLauncherOnDrawCallback(new Runnable() { // from class: com.android.quickstep.-$$Lambda$OtherActivityTouchConsumer$8_w8V8eDDkACVaOsP_z8ZYoSs94
            @Override // java.lang.Runnable
            public final void run() {
                OtherActivityTouchConsumer.lambda$startTouchTrackingForWindowAnimation$0(this.f$0, countDownLatch, windowTransformSwipeHandler);
            }
        });
        windowTransformSwipeHandler.initWhenReady();
        TraceHelper.beginSection("RecentsController");
        Runnable runnable = new Runnable() { // from class: com.android.quickstep.-$$Lambda$OtherActivityTouchConsumer$_dUNPyE5JlPRA8-dJMCuLuoAgvU
            @Override // java.lang.Runnable
            public final void run() {
                OtherActivityTouchConsumer otherActivityTouchConsumer = this.f$0;
                ActivityManagerWrapper.getInstance().startRecentsActivity(otherActivityTouchConsumer.mHomeIntent, new AssistDataReceiver() { // from class: com.android.quickstep.OtherActivityTouchConsumer.1
                    @Override // com.android.systemui.shared.system.AssistDataReceiver
                    public void onHandleAssistData(Bundle bundle) {
                        OtherActivityTouchConsumer.this.mRecentsModel.preloadAssistData(OtherActivityTouchConsumer.this.mRunningTask.id, bundle);
                    }
                }, recentsAnimationState, null, null);
            }
        };
        if (Looper.myLooper() != Looper.getMainLooper()) {
            runnable.run();
            try {
                countDownLatch.await(LAUNCHER_DRAW_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                return;
            } catch (Exception e) {
                return;
            }
        }
        BackgroundExecutor.get().submit(runnable);
    }

    public static /* synthetic */ void lambda$startTouchTrackingForWindowAnimation$0(OtherActivityTouchConsumer otherActivityTouchConsumer, CountDownLatch countDownLatch, WindowTransformSwipeHandler windowTransformSwipeHandler) {
        countDownLatch.countDown();
        if (windowTransformSwipeHandler == otherActivityTouchConsumer.mInteractionHandler) {
            otherActivityTouchConsumer.switchToMainChoreographer();
        }
    }

    @Override // com.android.quickstep.TouchConsumer
    public void onCommand(int i) {
        RecentsAnimationState recentsAnimationState = this.mAnimationStates.get(i);
        if (recentsAnimationState != null) {
            recentsAnimationState.execute();
        }
    }

    private void finishTouchTracking(MotionEvent motionEvent) {
        float yVelocity;
        if (this.mPassedInitialSlop && this.mInteractionHandler != null) {
            this.mInteractionHandler.updateDisplacement(getDisplacement(motionEvent) - this.mStartDisplacement);
            this.mVelocityTracker.computeCurrentVelocity(1000, ViewConfiguration.get(this).getScaledMaximumFlingVelocity());
            if (isNavBarOnRight()) {
                yVelocity = this.mVelocityTracker.getXVelocity(this.mActivePointerId);
            } else {
                yVelocity = isNavBarOnLeft() ? -this.mVelocityTracker.getXVelocity(this.mActivePointerId) : this.mVelocityTracker.getYVelocity(this.mActivePointerId);
            }
            this.mInteractionHandler.onGestureEnded(yVelocity);
        } else {
            reset();
            ActivityManagerWrapper.getInstance().cancelRecentsAnimation(true);
        }
        this.mVelocityTracker.recycle();
        this.mVelocityTracker = null;
    }

    @Override // com.android.quickstep.TouchConsumer
    public void reset() {
        if (this.mInteractionHandler != null) {
            final WindowTransformSwipeHandler windowTransformSwipeHandler = this.mInteractionHandler;
            this.mInteractionHandler = null;
            this.mIsGoingToHome = windowTransformSwipeHandler.mIsGoingToHome;
            MainThreadExecutor mainThreadExecutor = this.mMainThreadExecutor;
            Objects.requireNonNull(windowTransformSwipeHandler);
            mainThreadExecutor.execute(new Runnable() { // from class: com.android.quickstep.-$$Lambda$wvt2sKsglkn8HS_eF7-cDtuBhS8
                @Override // java.lang.Runnable
                public final void run() {
                    windowTransformSwipeHandler.reset();
                }
            });
        }
    }

    @Override // com.android.quickstep.TouchConsumer
    public void updateTouchTracking(int i) throws InterruptedException {
        if (!this.mPassedInitialSlop && this.mIsDeferredDownTarget && this.mInteractionHandler == null) {
            startTouchTrackingForWindowAnimation(SystemClock.uptimeMillis());
            this.mPassedInitialSlop = true;
        }
        if (this.mInteractionHandler != null) {
            this.mInteractionHandler.updateInteractionType(i);
        }
        notifyGestureStarted();
    }

    @Override // com.android.quickstep.TouchConsumer
    public Choreographer getIntrimChoreographer(MotionEventQueue motionEventQueue) {
        this.mEventQueue = motionEventQueue;
        return this.mBackgroundThreadChoreographer;
    }

    @Override // com.android.quickstep.TouchConsumer
    public void onQuickScrubEnd() {
        if (this.mInteractionHandler != null) {
            this.mInteractionHandler.onQuickScrubEnd();
        }
    }

    @Override // com.android.quickstep.TouchConsumer
    public void onQuickScrubProgress(float f) {
        if (this.mInteractionHandler != null) {
            this.mInteractionHandler.onQuickScrubProgress(f);
        }
    }

    @Override // com.android.quickstep.TouchConsumer
    public void onQuickStep(MotionEvent motionEvent) throws InterruptedException {
        if (this.mIsDeferredDownTarget) {
            startTouchTrackingForWindowAnimation(motionEvent.getEventTime());
            this.mPassedInitialSlop = true;
            this.mStartDisplacement = getDisplacement(motionEvent);
        }
        notifyGestureStarted();
    }

    private float getDisplacement(MotionEvent motionEvent) {
        float x = motionEvent.getX();
        float y = motionEvent.getY() - this.mDownPos.y;
        if (isNavBarOnRight()) {
            return x - this.mDownPos.x;
        }
        if (isNavBarOnLeft()) {
            return this.mDownPos.x - x;
        }
        return y;
    }

    public void switchToMainChoreographer() {
        this.mEventQueue.setInterimChoreographer(null);
    }

    @Override // com.android.quickstep.TouchConsumer
    public void preProcessMotionEvent(MotionEvent motionEvent) {
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.addMovement(motionEvent);
            if (motionEvent.getActionMasked() == 6) {
                this.mVelocityTracker.clear();
            }
        }
    }

    @Override // com.android.quickstep.TouchConsumer
    public boolean forceToLauncherConsumer() {
        return this.mIsGoingToHome;
    }

    @Override // com.android.quickstep.TouchConsumer
    public boolean deferNextEventToMainThread() {
        return this.mInteractionHandler != null;
    }

    private class RecentsAnimationState implements RecentsAnimationListener {
        private final int id;
        private boolean mCancelled;
        private RecentsAnimationControllerCompat mController;
        private Rect mHomeContentInsets;
        private Rect mMinimizedHomeBounds;
        private RemoteAnimationTargetSet mTargets;

        public RecentsAnimationState() {
            this.id = OtherActivityTouchConsumer.this.mAnimationStates.size();
            OtherActivityTouchConsumer.this.mAnimationStates.put(this.id, this);
        }

        @Override // com.android.systemui.shared.system.RecentsAnimationListener
        public void onAnimationStart(RecentsAnimationControllerCompat recentsAnimationControllerCompat, RemoteAnimationTargetCompat[] remoteAnimationTargetCompatArr, Rect rect, Rect rect2) {
            this.mController = recentsAnimationControllerCompat;
            this.mTargets = new RemoteAnimationTargetSet(remoteAnimationTargetCompatArr, 1);
            this.mHomeContentInsets = rect;
            this.mMinimizedHomeBounds = rect2;
            OtherActivityTouchConsumer.this.mEventQueue.onCommand(this.id);
        }

        @Override // com.android.systemui.shared.system.RecentsAnimationListener
        public void onAnimationCanceled() {
            this.mCancelled = true;
            OtherActivityTouchConsumer.this.mEventQueue.onCommand(this.id);
        }

        public void execute() {
            if (OtherActivityTouchConsumer.this.mInteractionHandler == null || OtherActivityTouchConsumer.this.mInteractionHandler.id != this.id) {
                if (!this.mCancelled && this.mController != null) {
                    TraceHelper.endSection("RecentsController", "Finishing no handler");
                    this.mController.finish(false);
                    return;
                }
                return;
            }
            if (this.mCancelled) {
                TraceHelper.endSection("RecentsController", "Cancelled: " + OtherActivityTouchConsumer.this.mInteractionHandler);
                OtherActivityTouchConsumer.this.mInteractionHandler.onRecentsAnimationCanceled();
                return;
            }
            TraceHelper.partitionSection("RecentsController", "Received");
            OtherActivityTouchConsumer.this.mInteractionHandler.onRecentsAnimationStart(this.mController, this.mTargets, this.mHomeContentInsets, this.mMinimizedHomeBounds);
        }
    }
}
