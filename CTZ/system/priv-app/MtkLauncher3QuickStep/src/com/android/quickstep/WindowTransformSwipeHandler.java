package com.android.quickstep;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.UserHandle;
import android.support.annotation.AnyThread;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import android.support.v4.media.subtitle.Cea708CCParser;
import android.support.v4.view.InputDeviceCompat;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.Interpolator;
import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.BaseDraggingActivity;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.anim.AnimationSuccessListener;
import com.android.launcher3.anim.AnimatorPlaybackController;
import com.android.launcher3.anim.Interpolators;
import com.android.launcher3.logging.UserEventDispatcher;
import com.android.launcher3.util.MultiValueAlpha;
import com.android.launcher3.util.TraceHelper;
import com.android.launcher3.views.BaseDragLayer;
import com.android.quickstep.ActivityControlHelper;
import com.android.quickstep.util.ClipAnimationHelper;
import com.android.quickstep.util.RemoteAnimationTargetSet;
import com.android.quickstep.util.TransformedRect;
import com.android.quickstep.views.RecentsView;
import com.android.quickstep.views.TaskView;
import com.android.systemui.shared.recents.model.ThumbnailData;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.shared.system.InputConsumerController;
import com.android.systemui.shared.system.LatencyTrackerCompat;
import com.android.systemui.shared.system.RecentsAnimationControllerCompat;
import com.android.systemui.shared.system.RemoteAnimationTargetCompat;
import com.android.systemui.shared.system.WindowCallbacksCompat;
import com.android.systemui.shared.system.WindowManagerWrapper;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

@TargetApi(26)
/* loaded from: classes.dex */
public class WindowTransformSwipeHandler<T extends BaseDraggingActivity> {
    private static final boolean DEBUG_STATES = false;
    private static final int LAUNCHER_UI_STATES = 15;
    private static final int LONG_SWIPE_ENTER_STATE = 26;
    private static final int LONG_SWIPE_START_STATE = 32794;
    public static final long MAX_SWIPE_DURATION = 350;
    private static final float MIN_PROGRESS_FOR_OVERVIEW = 0.5f;
    public static final long MIN_SWIPE_DURATION = 80;
    private static final int STATE_ACTIVITY_MULTIPLIER_COMPLETE = 8;
    private static final int STATE_APP_CONTROLLER_RECEIVED = 16;
    private static final int STATE_CAPTURE_SCREENSHOT = 16384;
    private static final int STATE_CURRENT_TASK_FINISHED = 2048;
    private static final int STATE_GESTURE_CANCELLED = 512;
    private static final int STATE_GESTURE_COMPLETED = 1024;
    private static final int STATE_GESTURE_STARTED = 256;
    private static final int STATE_HANDLER_INVALIDATED = 128;
    private static final int STATE_LAUNCHER_DRAWN = 4;
    private static final int STATE_LAUNCHER_PRESENT = 1;
    private static final int STATE_LAUNCHER_STARTED = 2;
    private static final int STATE_QUICK_SCRUB_END = 8192;
    private static final int STATE_QUICK_SCRUB_START = 4096;
    private static final int STATE_RESUME_LAST_TASK = 65536;
    private static final int STATE_SCALED_CONTROLLER_APP = 64;
    private static final int STATE_SCALED_CONTROLLER_RECENTS = 32;
    private static final int STATE_SCREENSHOT_CAPTURED = 32768;
    public final int id;
    private T mActivity;
    private final ActivityControlHelper<T> mActivityControlHelper;
    private final ActivityControlHelper.ActivityInitListener mActivityInitListener;
    private final Context mContext;
    private float mCurrentQuickScrubProgress;
    private DeviceProfile mDp;
    protected Runnable mGestureEndCallback;
    private boolean mGestureStarted;
    protected boolean mIsGoingToHome;
    private Runnable mLauncherDrawnCallback;
    private long mLauncherFrameDrawnTime;
    private AnimatorPlaybackController mLauncherTransitionController;
    private ActivityControlHelper.LayoutListener mLayoutListener;
    private LongSwipeHelper mLongSwipeController;
    private boolean mQuickScrubBlocked;
    private QuickScrubController mQuickScrubController;
    private RecentsView mRecentsView;
    private final int mRunningTaskId;
    private final ActivityManager.RunningTaskInfo mRunningTaskInfo;
    private MultiStateCallback mStateCallback;
    private ThumbnailData mTaskSnapshot;
    private final long mTouchTimeMs;
    private int mTransitionDragLength;
    private boolean mWasLauncherAlreadyVisible;
    private static final String TAG = WindowTransformSwipeHandler.class.getSimpleName();
    private static final String[] STATES = {"STATE_LAUNCHER_PRESENT", "STATE_LAUNCHER_STARTED", "STATE_LAUNCHER_DRAWN", "STATE_ACTIVITY_MULTIPLIER_COMPLETE", "STATE_APP_CONTROLLER_RECEIVED", "STATE_SCALED_CONTROLLER_RECENTS", "STATE_SCALED_CONTROLLER_APP", "STATE_HANDLER_INVALIDATED", "STATE_GESTURE_STARTED", "STATE_GESTURE_CANCELLED", "STATE_GESTURE_COMPLETED", "STATE_CURRENT_TASK_FINISHED", "STATE_QUICK_SCRUB_START", "STATE_QUICK_SCRUB_END", "STATE_CAPTURE_SCREENSHOT", "STATE_SCREENSHOT_CAPTURED", "STATE_RESUME_LAST_TASK"};
    private static final float SWIPE_DURATION_MULTIPLIER = Math.min(2.0f, 2.0f);
    private final ClipAnimationHelper mClipAnimationHelper = new ClipAnimationHelper();
    private final AnimatedFloat mCurrentShift = new AnimatedFloat(new Runnable() { // from class: com.android.quickstep.-$$Lambda$WindowTransformSwipeHandler$_o6wAUXibcgiHBuNnSt2vs4EZ40
        @Override // java.lang.Runnable
        public final void run() {
            this.f$0.updateFinalShift();
        }
    });
    private final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());
    private ActivityControlHelper.AnimationFactory mAnimationFactory = new ActivityControlHelper.AnimationFactory() { // from class: com.android.quickstep.-$$Lambda$WindowTransformSwipeHandler$mVJiFdNS_bbSM0ubx6z-4DqcV3E
        @Override // com.android.quickstep.ActivityControlHelper.AnimationFactory
        public final void createActivityController(long j, int i) {
            WindowTransformSwipeHandler.lambda$new$0(j, i);
        }
    };
    private int mLogAction = 3;
    private int mInteractionType = 0;
    private InputConsumerController mInputConsumer = InputConsumerController.getRecentsAnimationInputConsumer();
    private final RecentsAnimationWrapper mRecentsAnimationWrapper = new RecentsAnimationWrapper();
    private boolean mBgLongSwipeMode = false;
    private boolean mUiLongSwipeMode = false;
    private float mLongSwipeDisplacement = 0.0f;

    static /* synthetic */ void lambda$new$0(long j, int i) {
    }

    WindowTransformSwipeHandler(int i, ActivityManager.RunningTaskInfo runningTaskInfo, Context context, long j, ActivityControlHelper<T> activityControlHelper) {
        this.id = i;
        this.mContext = context;
        this.mRunningTaskInfo = runningTaskInfo;
        this.mRunningTaskId = runningTaskInfo.id;
        this.mTouchTimeMs = j;
        this.mActivityControlHelper = activityControlHelper;
        this.mActivityInitListener = this.mActivityControlHelper.createActivityInitListener(new BiPredicate() { // from class: com.android.quickstep.-$$Lambda$WindowTransformSwipeHandler$xkYtGAhXw1754_DaDTNxV_iBEyU
            @Override // java.util.function.BiPredicate
            public final boolean test(Object obj, Object obj2) {
                return this.f$0.onActivityInit((BaseDraggingActivity) obj, (Boolean) obj2);
            }
        });
        initStateCallbacks();
        final InputConsumerController inputConsumerController = this.mInputConsumer;
        Objects.requireNonNull(inputConsumerController);
        executeOnUiThread(new Runnable() { // from class: com.android.quickstep.-$$Lambda$uDY6C_772m3f91--2BEsCWoTw-I
            @Override // java.lang.Runnable
            public final void run() {
                inputConsumerController.registerInputConsumer();
            }
        });
    }

    /* renamed from: com.android.quickstep.WindowTransformSwipeHandler$1 */
    class AnonymousClass1 extends MultiStateCallback {
        AnonymousClass1() {
        }

        @Override // com.android.quickstep.MultiStateCallback
        public void setState(int i) {
            WindowTransformSwipeHandler.this.debugNewState(i);
            super.setState(i);
        }
    }

    private void initStateCallbacks() {
        this.mStateCallback = new MultiStateCallback() { // from class: com.android.quickstep.WindowTransformSwipeHandler.1
            AnonymousClass1() {
            }

            @Override // com.android.quickstep.MultiStateCallback
            public void setState(int i) {
                WindowTransformSwipeHandler.this.debugNewState(i);
                super.setState(i);
            }
        };
        this.mStateCallback.addCallback(260, new Runnable() { // from class: com.android.quickstep.-$$Lambda$WindowTransformSwipeHandler$D5vbNV4bxsq8rVoE-i_oGhlL8SA
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.initializeLauncherAnimationController();
            }
        });
        this.mStateCallback.addCallback(5, new Runnable() { // from class: com.android.quickstep.-$$Lambda$WindowTransformSwipeHandler$fDWyfwHdv0bkcbrM1tKEDBg8Juc
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.launcherFrameDrawn();
            }
        });
        this.mStateCallback.addCallback(InputDeviceCompat.SOURCE_KEYBOARD, new Runnable() { // from class: com.android.quickstep.-$$Lambda$WindowTransformSwipeHandler$uPM78g3dmJQFfIligkz6nKCoQYM
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.notifyGestureStartedAsync();
            }
        });
        this.mStateCallback.addCallback(515, new Runnable() { // from class: com.android.quickstep.-$$Lambda$WindowTransformSwipeHandler$ufp7KV7A4ZxmVp01kL-HJqVq_6k
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.resetStateForAnimationCancel();
            }
        });
        this.mStateCallback.addCallback(18, new Runnable() { // from class: com.android.quickstep.-$$Lambda$WindowTransformSwipeHandler$T-uYruHnpO5zu1J7Sr-ZwkSOcws
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.sendRemoteAnimationsToAnimationFactory();
            }
        });
        this.mStateCallback.addCallback(65, new Runnable() { // from class: com.android.quickstep.-$$Lambda$WindowTransformSwipeHandler$2fIBXEAGpgMZOGzzbKDk1ECbmAU
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.resumeLastTaskForQuickstep();
            }
        });
        this.mStateCallback.addCallback(65552, new Runnable() { // from class: com.android.quickstep.-$$Lambda$WindowTransformSwipeHandler$ReBKPKJUm0D82MQ4VDMhANnTnlY
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.resumeLastTask();
            }
        });
        this.mStateCallback.addCallback(16409, new Runnable() { // from class: com.android.quickstep.-$$Lambda$WindowTransformSwipeHandler$wbwRicTS3LODLKMncpTzS0agA9s
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.switchToScreenshot();
            }
        });
        this.mStateCallback.addCallback(33824, new Runnable() { // from class: com.android.quickstep.-$$Lambda$WindowTransformSwipeHandler$jhbW7WfJ0m3MDKiTPbxvu2MiEfI
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.finishCurrentTransitionToHome();
            }
        });
        this.mStateCallback.addCallback(3129, new Runnable() { // from class: com.android.quickstep.-$$Lambda$WindowTransformSwipeHandler$nVkIZDyrwlt_qJBdchnI-v7Clks
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.setupLauncherUiAfterSwipeUpAnimation();
            }
        });
        this.mStateCallback.addCallback(128, new Runnable() { // from class: com.android.quickstep.-$$Lambda$WindowTransformSwipeHandler$yNlE7QZP9JpHsXLnlEBCe9U2yTo
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.invalidateHandler();
            }
        });
        this.mStateCallback.addCallback(Cea708CCParser.Const.CODE_C1_CW1, new Runnable() { // from class: com.android.quickstep.-$$Lambda$WindowTransformSwipeHandler$vaj6vPSn8to-YxJ1FKLZyt05Ycc
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.invalidateHandlerWithLauncher();
            }
        });
        this.mStateCallback.addCallback(193, new Runnable() { // from class: com.android.quickstep.-$$Lambda$WindowTransformSwipeHandler$dWZdedMRd28Sj7qzwVSfm0HSr1Y
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.notifyTransitionCancelled();
            }
        });
        this.mStateCallback.addCallback(4114, new Runnable() { // from class: com.android.quickstep.-$$Lambda$WindowTransformSwipeHandler$qIZ4mA0aVL2s1JRDFuXW1ks1Oa8
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.onQuickScrubStart();
            }
        });
        this.mStateCallback.addCallback(4130, new Runnable() { // from class: com.android.quickstep.-$$Lambda$WindowTransformSwipeHandler$CKvWzVP65bzNF-wvQXDZgCRvUq8
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.onFinishedTransitionToQuickScrub();
            }
        });
        this.mStateCallback.addCallback(10242, new Runnable() { // from class: com.android.quickstep.-$$Lambda$WindowTransformSwipeHandler$V8Py6CkYhBHFh0F174w6Blon-hE
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.switchToFinalAppAfterQuickScrub();
            }
        });
        this.mStateCallback.addCallback(26, new Runnable() { // from class: com.android.quickstep.-$$Lambda$WindowTransformSwipeHandler$Nt67n14zenGCzlsN_rZXk1uSVgQ
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.checkLongSwipeCanEnter();
            }
        });
        this.mStateCallback.addCallback(LONG_SWIPE_START_STATE, new Runnable() { // from class: com.android.quickstep.-$$Lambda$WindowTransformSwipeHandler$HqNPGFLqPTVOBHoy1QPexWWLbTo
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.checkLongSwipeCanStart();
            }
        });
    }

    private void executeOnUiThread(Runnable runnable) {
        if (Looper.myLooper() == this.mMainThreadHandler.getLooper()) {
            runnable.run();
        } else {
            Utilities.postAsyncCallback(this.mMainThreadHandler, runnable);
        }
    }

    private void setStateOnUiThread(final int i) {
        if (Looper.myLooper() == this.mMainThreadHandler.getLooper()) {
            this.mStateCallback.setState(i);
        } else {
            Utilities.postAsyncCallback(this.mMainThreadHandler, new Runnable() { // from class: com.android.quickstep.-$$Lambda$WindowTransformSwipeHandler$eXD4DYzJHA9dUfTFDBSndx_uSos
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.mStateCallback.setState(i);
                }
            });
        }
    }

    private void initTransitionEndpoints(DeviceProfile deviceProfile) {
        this.mDp = deviceProfile;
        TransformedRect transformedRect = new TransformedRect();
        this.mTransitionDragLength = this.mActivityControlHelper.getSwipeUpDestinationAndLength(deviceProfile, this.mContext, this.mInteractionType, transformedRect);
        this.mClipAnimationHelper.updateTargetRect(transformedRect);
    }

    private long getFadeInDuration() {
        if (this.mCurrentShift.getCurrentAnimation() == null) {
            return 350L;
        }
        ObjectAnimator currentAnimation = this.mCurrentShift.getCurrentAnimation();
        return Math.min(350L, Math.max(currentAnimation.getDuration() - currentAnimation.getCurrentPlayTime(), 80L));
    }

    public void initWhenReady() {
        this.mActivityInitListener.register();
    }

    private boolean onActivityInit(T t, Boolean bool) {
        if (this.mActivity == t) {
            return true;
        }
        if (this.mActivity != null) {
            int state = this.mStateCallback.getState() & (-16);
            initStateCallbacks();
            this.mStateCallback.setState(state);
            this.mLayoutListener.setHandler(null);
        }
        this.mWasLauncherAlreadyVisible = bool.booleanValue();
        this.mActivity = t;
        if (bool.booleanValue()) {
            this.mActivity.clearForceInvisibleFlag(1);
        } else {
            this.mActivity.addForceInvisibleFlag(1);
        }
        this.mRecentsView = (RecentsView) t.getOverviewPanel();
        this.mQuickScrubController = this.mRecentsView.getQuickScrubController();
        this.mLayoutListener = this.mActivityControlHelper.createLayoutListener(this.mActivity);
        this.mStateCallback.setState(1);
        if (bool.booleanValue()) {
            onLauncherStart(t);
        } else {
            t.setOnStartCallback(new BaseDraggingActivity.OnStartCallback() { // from class: com.android.quickstep.-$$Lambda$WindowTransformSwipeHandler$oTmdTx9KEnt4AhNDszi5oKfUOY4
                @Override // com.android.launcher3.BaseDraggingActivity.OnStartCallback
                public final void onActivityStart(BaseDraggingActivity baseDraggingActivity) {
                    this.f$0.onLauncherStart(baseDraggingActivity);
                }
            });
        }
        return true;
    }

    private void onLauncherStart(T t) {
        if (this.mActivity != t || this.mStateCallback.hasStates(128)) {
            return;
        }
        this.mAnimationFactory = this.mActivityControlHelper.prepareRecentsUI(this.mActivity, this.mWasLauncherAlreadyVisible, new Consumer() { // from class: com.android.quickstep.-$$Lambda$WindowTransformSwipeHandler$gtOwtfGRmuJn7gvSGsWt7rLq4r4
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                this.f$0.onAnimatorPlaybackControllerCreated((AnimatorPlaybackController) obj);
            }
        });
        AbstractFloatingView.closeAllOpenViews(t, this.mWasLauncherAlreadyVisible);
        if (this.mWasLauncherAlreadyVisible) {
            this.mStateCallback.setState(12);
        } else {
            TraceHelper.beginSection("WTS-init");
            BaseDragLayer dragLayer = t.getDragLayer();
            this.mActivityControlHelper.getAlphaProperty(t).setValue(0.0f);
            dragLayer.getViewTreeObserver().addOnDrawListener(new AnonymousClass2(dragLayer, t));
        }
        this.mRecentsView.showTask(this.mRunningTaskId);
        this.mRecentsView.setRunningTaskHidden(true);
        this.mRecentsView.setRunningTaskIconScaledDown(true, false);
        this.mLayoutListener.open();
        this.mStateCallback.setState(2);
    }

    /* renamed from: com.android.quickstep.WindowTransformSwipeHandler$2 */
    class AnonymousClass2 implements ViewTreeObserver.OnDrawListener {
        final /* synthetic */ BaseDraggingActivity val$activity;
        final /* synthetic */ View val$dragLayer;

        AnonymousClass2(View view, BaseDraggingActivity baseDraggingActivity) {
            this.val$dragLayer = view;
            this.val$activity = baseDraggingActivity;
        }

        @Override // android.view.ViewTreeObserver.OnDrawListener
        public void onDraw() {
            TraceHelper.endSection("WTS-init", "Launcher frame is drawn");
            View view = this.val$dragLayer;
            final View view2 = this.val$dragLayer;
            view.post(new Runnable() { // from class: com.android.quickstep.-$$Lambda$WindowTransformSwipeHandler$2$uKDKINCEs57gcAw5S9npG75WLKs
                @Override // java.lang.Runnable
                public final void run() {
                    view2.getViewTreeObserver().removeOnDrawListener(this.f$0);
                }
            });
            if (this.val$activity == WindowTransformSwipeHandler.this.mActivity) {
                WindowTransformSwipeHandler.this.mStateCallback.setState(4);
            }
        }
    }

    public void setLauncherOnDrawCallback(Runnable runnable) {
        this.mLauncherDrawnCallback = runnable;
    }

    private void launcherFrameDrawn() {
        MultiValueAlpha.AlphaProperty alphaProperty = this.mActivityControlHelper.getAlphaProperty(this.mActivity);
        if (alphaProperty.getValue() < 1.0f) {
            if (this.mGestureStarted) {
                MultiStateCallback multiStateCallback = this.mStateCallback;
                ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(alphaProperty, MultiValueAlpha.VALUE, 1.0f);
                objectAnimatorOfFloat.setDuration(getFadeInDuration()).addListener(new AnimatorListenerAdapter() { // from class: com.android.quickstep.WindowTransformSwipeHandler.3
                    final /* synthetic */ MultiStateCallback val$callback;

                    AnonymousClass3(MultiStateCallback multiStateCallback2) {
                        multiStateCallback = multiStateCallback2;
                    }

                    @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                    public void onAnimationEnd(Animator animator) {
                        multiStateCallback.setState(8);
                    }
                });
                objectAnimatorOfFloat.start();
            } else {
                alphaProperty.setValue(1.0f);
                this.mStateCallback.setState(8);
            }
        }
        if (this.mLauncherDrawnCallback != null) {
            this.mLauncherDrawnCallback.run();
        }
        this.mLauncherFrameDrawnTime = SystemClock.uptimeMillis();
    }

    /* renamed from: com.android.quickstep.WindowTransformSwipeHandler$3 */
    class AnonymousClass3 extends AnimatorListenerAdapter {
        final /* synthetic */ MultiStateCallback val$callback;

        AnonymousClass3(MultiStateCallback multiStateCallback2) {
            multiStateCallback = multiStateCallback2;
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            multiStateCallback.setState(8);
        }
    }

    private void sendRemoteAnimationsToAnimationFactory() {
        this.mAnimationFactory.onRemoteAnimationReceived(this.mRecentsAnimationWrapper.targetSet);
    }

    private void initializeLauncherAnimationController() {
        this.mLayoutListener.setHandler(this);
        buildAnimationController();
        if (LatencyTrackerCompat.isEnabled(this.mContext)) {
            LatencyTrackerCompat.logToggleRecents((int) (this.mLauncherFrameDrawnTime - this.mTouchTimeMs));
        }
    }

    public void updateInteractionType(int i) {
        if (this.mInteractionType != 0) {
            throw new IllegalArgumentException("Can't change interaction type from " + this.mInteractionType);
        }
        if (i != 1) {
            throw new IllegalArgumentException("Can't change interaction type to " + i);
        }
        this.mInteractionType = i;
        this.mRecentsAnimationWrapper.runOnInit(new Runnable() { // from class: com.android.quickstep.-$$Lambda$WindowTransformSwipeHandler$l_5fZspC7Bjh488BDsaCdx7rMtM
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.shiftAnimationDestinationForQuickscrub();
            }
        });
        setStateOnUiThread(5120);
        animateToProgress(this.mCurrentShift.value, 1.0f, 240L, Interpolators.LINEAR);
    }

    private void shiftAnimationDestinationForQuickscrub() {
        float f;
        TransformedRect transformedRect = new TransformedRect();
        this.mActivityControlHelper.getSwipeUpDestinationAndLength(this.mDp, this.mContext, this.mInteractionType, transformedRect);
        this.mClipAnimationHelper.updateTargetRect(transformedRect);
        float translationYForQuickScrub = this.mActivityControlHelper.getTranslationYForQuickScrub(transformedRect, this.mDp, this.mContext);
        Resources resources = this.mContext.getResources();
        float curveScaleForInterpolation = 1.0f;
        if (ActivityManagerWrapper.getInstance().getRecentTasks(2, UserHandle.myUserId()).size() < 2) {
            f = 0.0f;
        } else {
            float dimensionPixelSize = resources.getDimensionPixelSize(R.dimen.recents_page_spacing) + transformedRect.rect.width();
            curveScaleForInterpolation = TaskView.getCurveScaleForInterpolation(Math.min(1.0f, dimensionPixelSize / (((this.mDp.widthPx / 2) + (transformedRect.rect.width() / 2)) + resources.getDimensionPixelSize(R.dimen.recents_page_spacing))));
            f = dimensionPixelSize;
        }
        ClipAnimationHelper clipAnimationHelper = this.mClipAnimationHelper;
        if (Utilities.isRtl(resources)) {
            f = -f;
        }
        clipAnimationHelper.offsetTarget(curveScaleForInterpolation, f, translationYForQuickScrub, QuickScrubController.QUICK_SCRUB_START_INTERPOLATOR);
    }

    @WorkerThread
    public void updateDisplacement(float f) {
        float f2 = -f;
        if (f2 > this.mTransitionDragLength) {
            this.mCurrentShift.updateValue(1.0f);
            if (!this.mBgLongSwipeMode) {
                this.mBgLongSwipeMode = true;
                executeOnUiThread(new Runnable() { // from class: com.android.quickstep.-$$Lambda$WindowTransformSwipeHandler$HTJl-1f8sUPfbYXN4PSm-fGlWdc
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.onLongSwipeEnabledUi();
                    }
                });
            }
            this.mLongSwipeDisplacement = f2 - this.mTransitionDragLength;
            executeOnUiThread(new Runnable() { // from class: com.android.quickstep.-$$Lambda$WindowTransformSwipeHandler$3KqUawC6cDg3a8xDvrEyMF8py-A
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.onLongSwipeDisplacementUpdated();
                }
            });
            return;
        }
        if (this.mBgLongSwipeMode) {
            this.mBgLongSwipeMode = false;
            executeOnUiThread(new Runnable() { // from class: com.android.quickstep.-$$Lambda$WindowTransformSwipeHandler$jrbQAEcKiAcXYujTeF_kWzYVpFU
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.onLongSwipeDisabledUi();
                }
            });
        }
        this.mCurrentShift.updateValue(this.mTransitionDragLength != 0 ? Math.max(f2, 0.0f) / this.mTransitionDragLength : 0.0f);
    }

    public void buildAnimationController() {
        initTransitionEndpoints(this.mActivity.getDeviceProfile());
        this.mAnimationFactory.createActivityController(this.mTransitionDragLength, this.mInteractionType);
    }

    private void onAnimatorPlaybackControllerCreated(AnimatorPlaybackController animatorPlaybackController) {
        this.mLauncherTransitionController = animatorPlaybackController;
        this.mLauncherTransitionController.dispatchOnStart();
        this.mLauncherTransitionController.setPlayFraction(this.mCurrentShift.value);
    }

    @WorkerThread
    private void updateFinalShift() {
        boolean z;
        float f = this.mCurrentShift.value;
        if (this.mRecentsAnimationWrapper.getController() != null) {
            this.mClipAnimationHelper.applyTransform(this.mRecentsAnimationWrapper.targetSet, f);
            if (f <= 0.12f) {
                z = false;
            } else {
                z = true;
            }
            this.mRecentsAnimationWrapper.setAnimationTargetsBehindSystemBars(!z);
            if (this.mActivityControlHelper.shouldMinimizeSplitScreen()) {
                this.mRecentsAnimationWrapper.setSplitScreenMinimizedForTransaction(z);
            }
        }
        executeOnUiThread(new Runnable() { // from class: com.android.quickstep.-$$Lambda$WindowTransformSwipeHandler$I9ePn2nv3_aEtnVdLe7ekbDR8Wk
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.updateFinalShiftUi();
            }
        });
    }

    private void updateFinalShiftUi() {
        if (this.mLauncherTransitionController == null) {
            return;
        }
        this.mLauncherTransitionController.setPlayFraction(this.mCurrentShift.value);
    }

    public void onRecentsAnimationStart(RecentsAnimationControllerCompat recentsAnimationControllerCompat, RemoteAnimationTargetSet remoteAnimationTargetSet, Rect rect, Rect rect2) {
        Rect rect3;
        DeviceProfile deviceProfileCopy;
        LauncherAppState instanceNoCreate = LauncherAppState.getInstanceNoCreate();
        DeviceProfile deviceProfile = (instanceNoCreate == null ? new InvariantDeviceProfile(this.mContext) : instanceNoCreate.getInvariantDeviceProfile()).getDeviceProfile(this.mContext);
        RemoteAnimationTargetCompat remoteAnimationTargetCompatFindTask = remoteAnimationTargetSet.findTask(this.mRunningTaskId);
        if (rect2 != null && remoteAnimationTargetCompatFindTask != null) {
            rect3 = this.mActivityControlHelper.getOverviewWindowBounds(rect2, remoteAnimationTargetCompatFindTask);
            deviceProfileCopy = deviceProfile.getMultiWindowProfile(this.mContext, new Point(rect2.width(), rect2.height()));
            deviceProfileCopy.updateInsets(rect);
        } else {
            rect3 = new Rect(0, 0, deviceProfile.widthPx, deviceProfile.heightPx);
            Rect rect4 = new Rect();
            WindowManagerWrapper.getInstance().getStableInsets(rect4);
            deviceProfileCopy = deviceProfile.copy(this.mContext);
            deviceProfileCopy.updateInsets(rect4);
        }
        deviceProfileCopy.updateIsSeascape((WindowManager) this.mContext.getSystemService(WindowManager.class));
        if (remoteAnimationTargetCompatFindTask != null) {
            this.mClipAnimationHelper.updateSource(rect3, remoteAnimationTargetCompatFindTask);
        }
        this.mClipAnimationHelper.prepareAnimation(false);
        initTransitionEndpoints(deviceProfileCopy);
        this.mRecentsAnimationWrapper.setController(recentsAnimationControllerCompat, remoteAnimationTargetSet);
        setStateOnUiThread(16);
    }

    public void onRecentsAnimationCanceled() {
        this.mRecentsAnimationWrapper.setController(null, null);
        this.mActivityInitListener.unregister();
        setStateOnUiThread(640);
    }

    public void onGestureStarted() {
        notifyGestureStartedAsync();
        setStateOnUiThread(256);
        this.mGestureStarted = true;
        this.mRecentsAnimationWrapper.hideCurrentInputMethod();
        this.mRecentsAnimationWrapper.enableInputConsumer();
    }

    @AnyThread
    private void notifyGestureStartedAsync() {
        if (this.mActivity != null) {
            this.mActivity.clearForceInvisibleFlag(1);
        }
    }

    @WorkerThread
    public void onGestureEnded(final float f) {
        final boolean z = this.mGestureStarted && Math.abs(f) > this.mContext.getResources().getDimension(R.dimen.quickstep_fling_threshold_velocity);
        setStateOnUiThread(1024);
        this.mLogAction = z ? 4 : 3;
        if (this.mBgLongSwipeMode) {
            executeOnUiThread(new Runnable() { // from class: com.android.quickstep.-$$Lambda$WindowTransformSwipeHandler$mBQq44kZtxDM81nTc9lXIxILtLg
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.onLongSwipeGestureFinishUi(f, z);
                }
            });
        } else {
            handleNormalGestureEnd(f, z);
        }
    }

    private void handleNormalGestureEnd(float f, boolean z) {
        float fBoundToRange;
        float f2;
        long jMin;
        float f3 = 1.0f;
        long jMin2 = 350;
        if (!z) {
            if (this.mCurrentShift.value < 0.5f || !this.mGestureStarted) {
                f3 = 0.0f;
            }
            jMin = Math.min(350L, Math.abs(Math.round((f3 - this.mCurrentShift.value) * 350.0f * SWIPE_DURATION_MULTIPLIER)));
            f2 = f3;
            fBoundToRange = this.mCurrentShift.value;
        } else {
            float f4 = f < 0.0f ? 1.0f : 0.0f;
            if (Math.abs(f) > this.mContext.getResources().getDimension(R.dimen.quickstep_fling_min_velocity) && this.mTransitionDragLength > 0) {
                jMin2 = Math.min(350L, 2 * Math.round(1000.0f * Math.abs(((f4 - this.mCurrentShift.value) * this.mTransitionDragLength) / f)));
            }
            fBoundToRange = Utilities.boundToRange(this.mCurrentShift.value - ((f * 16.0f) / (this.mTransitionDragLength * 1000)), 0.0f, 1.0f);
            f2 = f4;
            jMin = jMin2;
        }
        animateToProgress(fBoundToRange, f2, jMin, Interpolators.DEACCEL);
    }

    private void doLogGesture(boolean z) {
        int i;
        if (this.mDp.isVerticalBarLayout()) {
            i = this.mDp.isSeascape() ^ z ? 3 : 4;
        } else {
            i = z ? 1 : 2;
        }
        UserEventDispatcher.newInstance(this.mContext, this.mDp).logStateChangeAction(this.mLogAction, i, 11, 13, z ? 12 : 13, 0);
    }

    private void animateToProgress(float f, float f2, long j, Interpolator interpolator) {
        this.mIsGoingToHome = Float.compare(f2, 1.0f) == 0;
        final ObjectAnimator duration = this.mCurrentShift.animateToValue(f, f2).setDuration(j);
        duration.setInterpolator(interpolator);
        duration.addListener(new AnimationSuccessListener() { // from class: com.android.quickstep.WindowTransformSwipeHandler.4
            AnonymousClass4() {
            }

            @Override // com.android.launcher3.anim.AnimationSuccessListener
            public void onAnimationSuccess(Animator animator) {
                int i;
                WindowTransformSwipeHandler windowTransformSwipeHandler = WindowTransformSwipeHandler.this;
                if (WindowTransformSwipeHandler.this.mIsGoingToHome) {
                    i = 16416;
                } else {
                    i = 64;
                }
                windowTransformSwipeHandler.setStateOnUiThread(i);
            }
        });
        RecentsAnimationWrapper recentsAnimationWrapper = this.mRecentsAnimationWrapper;
        Objects.requireNonNull(duration);
        recentsAnimationWrapper.runOnInit(new Runnable() { // from class: com.android.quickstep.-$$Lambda$WindowTransformSwipeHandler$Kl5gxuNvnXBwN1w09-yFSVtnpbs
            @Override // java.lang.Runnable
            public final void run() {
                duration.start();
            }
        });
    }

    /* renamed from: com.android.quickstep.WindowTransformSwipeHandler$4 */
    class AnonymousClass4 extends AnimationSuccessListener {
        AnonymousClass4() {
        }

        @Override // com.android.launcher3.anim.AnimationSuccessListener
        public void onAnimationSuccess(Animator animator) {
            int i;
            WindowTransformSwipeHandler windowTransformSwipeHandler = WindowTransformSwipeHandler.this;
            if (WindowTransformSwipeHandler.this.mIsGoingToHome) {
                i = 16416;
            } else {
                i = 64;
            }
            windowTransformSwipeHandler.setStateOnUiThread(i);
        }
    }

    @UiThread
    private void resumeLastTaskForQuickstep() {
        setStateOnUiThread(65536);
        doLogGesture(false);
        reset();
    }

    @UiThread
    private void resumeLastTask() {
        this.mRecentsAnimationWrapper.finish(false, null);
    }

    public void reset() {
        if (this.mInteractionType != 1) {
            setStateOnUiThread(128);
        }
    }

    private void invalidateHandler() {
        this.mCurrentShift.finishAnimation();
        if (this.mGestureEndCallback != null) {
            this.mGestureEndCallback.run();
        }
        this.mActivityInitListener.unregister();
        this.mInputConsumer.unregisterInputConsumer();
        this.mTaskSnapshot = null;
    }

    private void invalidateHandlerWithLauncher() {
        this.mLauncherTransitionController = null;
        this.mLayoutListener.finish();
        this.mActivityControlHelper.getAlphaProperty(this.mActivity).setValue(1.0f);
        this.mRecentsView.setRunningTaskHidden(false);
        this.mRecentsView.setRunningTaskIconScaledDown(false, false);
        this.mQuickScrubController.cancelActiveQuickscrub();
    }

    private void notifyTransitionCancelled() {
        this.mAnimationFactory.onTransitionCancelled();
    }

    private void resetStateForAnimationCancel() {
        this.mActivityControlHelper.onTransitionCancelled(this.mActivity, this.mWasLauncherAlreadyVisible || this.mGestureStarted);
    }

    public void layoutListenerClosed() {
        if (this.mWasLauncherAlreadyVisible && this.mLauncherTransitionController != null) {
            this.mLauncherTransitionController.setPlayFraction(1.0f);
        }
    }

    private void switchToScreenshot() {
        RecentsAnimationControllerCompat controller = this.mRecentsAnimationWrapper.getController();
        boolean zAttach = false;
        if (controller != null) {
            if (this.mTaskSnapshot == null) {
                this.mTaskSnapshot = controller.screenshotTask(this.mRunningTaskId);
            }
            TaskView taskViewUpdateThumbnail = this.mRecentsView.updateThumbnail(this.mRunningTaskId, this.mTaskSnapshot);
            this.mRecentsView.setRunningTaskHidden(false);
            if (taskViewUpdateThumbnail != null) {
                zAttach = new WindowCallbacksCompat(taskViewUpdateThumbnail) { // from class: com.android.quickstep.WindowTransformSwipeHandler.5
                    AnonymousClass5(View taskViewUpdateThumbnail2) {
                        super(taskViewUpdateThumbnail2);
                    }

                    @Override // com.android.systemui.shared.system.WindowCallbacksCompat
                    public void onPostDraw(Canvas canvas) {
                        WindowTransformSwipeHandler.this.setStateOnUiThread(32768);
                        detach();
                    }
                }.attach();
            }
        }
        if (!zAttach) {
            setStateOnUiThread(32768);
        }
    }

    /* renamed from: com.android.quickstep.WindowTransformSwipeHandler$5 */
    class AnonymousClass5 extends WindowCallbacksCompat {
        AnonymousClass5(View taskViewUpdateThumbnail2) {
            super(taskViewUpdateThumbnail2);
        }

        @Override // com.android.systemui.shared.system.WindowCallbacksCompat
        public void onPostDraw(Canvas canvas) {
            WindowTransformSwipeHandler.this.setStateOnUiThread(32768);
            detach();
        }
    }

    private void finishCurrentTransitionToHome() {
        synchronized (this.mRecentsAnimationWrapper) {
            this.mRecentsAnimationWrapper.finish(true, new Runnable() { // from class: com.android.quickstep.-$$Lambda$WindowTransformSwipeHandler$tz2B3S9Hl7OB8nIDmCuCjhGpgtE
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.setStateOnUiThread(2048);
                }
            });
        }
    }

    private void setupLauncherUiAfterSwipeUpAnimation() {
        if (this.mLauncherTransitionController != null) {
            this.mLauncherTransitionController.getAnimationPlayer().end();
            this.mLauncherTransitionController = null;
        }
        this.mActivityControlHelper.onSwipeUpComplete(this.mActivity);
        this.mRecentsView.setRunningTaskIconScaledDown(false, true);
        this.mRecentsView.setSwipeDownShouldLaunchApp(true);
        RecentsModel.getInstance(this.mContext).onOverviewShown(false, TAG);
        doLogGesture(true);
        reset();
    }

    private void onQuickScrubStart() {
        if (!this.mQuickScrubController.prepareQuickScrub(TAG)) {
            this.mQuickScrubBlocked = true;
            setStateOnUiThread(65664);
            return;
        }
        if (this.mLauncherTransitionController != null) {
            this.mLauncherTransitionController.getAnimationPlayer().end();
            this.mLauncherTransitionController = null;
        }
        this.mActivityControlHelper.onQuickInteractionStart(this.mActivity, this.mRunningTaskInfo, false);
        this.mQuickScrubController.onQuickScrubProgress(this.mCurrentQuickScrubProgress);
    }

    private void onFinishedTransitionToQuickScrub() {
        if (this.mQuickScrubBlocked) {
            return;
        }
        this.mQuickScrubController.onFinishedTransitionToQuickScrub();
    }

    public void onQuickScrubProgress(float f) {
        this.mCurrentQuickScrubProgress = f;
        if (Looper.myLooper() != Looper.getMainLooper() || this.mQuickScrubController == null || this.mQuickScrubBlocked) {
            return;
        }
        this.mQuickScrubController.onQuickScrubProgress(f);
    }

    public void onQuickScrubEnd() {
        setStateOnUiThread(8192);
    }

    private void switchToFinalAppAfterQuickScrub() {
        if (this.mQuickScrubBlocked) {
            return;
        }
        this.mQuickScrubController.onQuickScrubEnd();
        setStateOnUiThread(128);
    }

    private void debugNewState(int i) {
    }

    public void setGestureEndCallback(Runnable runnable) {
        this.mGestureEndCallback = runnable;
    }

    private void onLongSwipeEnabledUi() {
        this.mUiLongSwipeMode = true;
        checkLongSwipeCanEnter();
        checkLongSwipeCanStart();
    }

    private void onLongSwipeDisabledUi() {
        this.mUiLongSwipeMode = false;
        if (this.mLongSwipeController != null) {
            this.mLongSwipeController.destroy();
            buildAnimationController();
        }
    }

    private void onLongSwipeDisplacementUpdated() {
        if (!this.mUiLongSwipeMode || this.mLongSwipeController == null) {
            return;
        }
        this.mLongSwipeController.onMove(this.mLongSwipeDisplacement);
    }

    private void checkLongSwipeCanEnter() {
        if (!this.mUiLongSwipeMode || !this.mStateCallback.hasStates(26) || !this.mActivityControlHelper.supportsLongSwipe(this.mActivity)) {
            return;
        }
        this.mStateCallback.setState(16384);
    }

    private void checkLongSwipeCanStart() {
        if (!this.mUiLongSwipeMode || !this.mStateCallback.hasStates(LONG_SWIPE_START_STATE) || !this.mActivityControlHelper.supportsLongSwipe(this.mActivity) || this.mRecentsAnimationWrapper.targetSet == null) {
            return;
        }
        this.mLongSwipeController = this.mActivityControlHelper.getLongSwipeController(this.mActivity, this.mRecentsAnimationWrapper.targetSet);
        onLongSwipeDisplacementUpdated();
    }

    private void onLongSwipeGestureFinishUi(float f, boolean z) {
        if (!this.mUiLongSwipeMode || this.mLongSwipeController == null) {
            this.mUiLongSwipeMode = false;
            handleNormalGestureEnd(f, z);
        } else {
            this.mUiLongSwipeMode = false;
            finishCurrentTransitionToHome();
            this.mLongSwipeController.end(f, z, new Runnable() { // from class: com.android.quickstep.-$$Lambda$WindowTransformSwipeHandler$UlYAiBBFZWVupBaQsWT6bm2Rhtw
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.setStateOnUiThread(128);
                }
            });
        }
    }
}
