package com.android.launcher3;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;
import android.util.Property;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.InsettableFrameLayout;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAnimationRunner;
import com.android.launcher3.LauncherAppTransitionManagerImpl;
import com.android.launcher3.allapps.AllAppsContainerView;
import com.android.launcher3.allapps.AllAppsTransitionController;
import com.android.launcher3.anim.AnimatorPlaybackController;
import com.android.launcher3.anim.Interpolators;
import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.graphics.DrawableFactory;
import com.android.launcher3.shortcuts.DeepShortcutView;
import com.android.launcher3.util.MultiValueAlpha;
import com.android.quickstep.TaskUtils;
import com.android.quickstep.util.ClipAnimationHelper;
import com.android.quickstep.util.MultiValueUpdateListener;
import com.android.quickstep.util.RemoteAnimationProvider;
import com.android.quickstep.views.RecentsView;
import com.android.quickstep.views.TaskView;
import com.android.systemui.shared.system.ActivityCompat;
import com.android.systemui.shared.system.ActivityOptionsCompat;
import com.android.systemui.shared.system.RemoteAnimationAdapterCompat;
import com.android.systemui.shared.system.RemoteAnimationDefinitionCompat;
import com.android.systemui.shared.system.RemoteAnimationRunnerCompat;
import com.android.systemui.shared.system.RemoteAnimationTargetCompat;
import com.android.systemui.shared.system.TransactionCompat;

@TargetApi(26)
public class LauncherAppTransitionManagerImpl extends LauncherAppTransitionManager implements DeviceProfile.OnDeviceProfileChangeListener {
    public static final float ALL_APPS_PROGRESS_OFF_SCREEN = 1.3059858f;
    private static final int APP_LAUNCH_ALPHA_DURATION = 50;
    private static final int APP_LAUNCH_ALPHA_START_DELAY = 32;
    private static final int APP_LAUNCH_CURVED_DURATION = 250;
    private static final float APP_LAUNCH_DOWN_DUR_SCALE_FACTOR = 0.8f;
    private static final int APP_LAUNCH_DURATION = 500;
    private static final int CLOSING_TRANSITION_DURATION_MS = 250;
    private static final String CONTROL_REMOTE_APP_TRANSITION_PERMISSION = "android.permission.CONTROL_REMOTE_APP_TRANSITION_ANIMATIONS";
    private static final int LAUNCHER_RESUME_START_DELAY = 100;
    public static final int RECENTS_LAUNCH_DURATION = 336;
    public static final int RECENTS_QUICKSCRUB_LAUNCH_DURATION = 300;
    public static final int STATUS_BAR_TRANSITION_DURATION = 120;
    private static final String TAG = "LauncherTransition";
    private final float mClosingWindowTransY;
    private final float mContentTransY;
    private DeviceProfile mDeviceProfile;
    private final DragLayer mDragLayer;
    private final MultiValueAlpha.AlphaProperty mDragLayerAlpha;
    private View mFloatingView;
    private final AnimatorListenerAdapter mForceInvisibleListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationStart(Animator animator) {
            LauncherAppTransitionManagerImpl.this.mLauncher.addForceInvisibleFlag(2);
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            LauncherAppTransitionManagerImpl.this.mLauncher.clearForceInvisibleFlag(2);
        }
    };
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final boolean mIsRtl;
    private final Launcher mLauncher;
    private RemoteAnimationProvider mRemoteAnimationProvider;
    private final float mWorkspaceTransY;

    public LauncherAppTransitionManagerImpl(Context context) {
        this.mLauncher = Launcher.getLauncher(context);
        this.mDragLayer = this.mLauncher.getDragLayer();
        this.mDragLayerAlpha = this.mDragLayer.getAlphaProperty(2);
        this.mIsRtl = Utilities.isRtl(this.mLauncher.getResources());
        this.mDeviceProfile = this.mLauncher.getDeviceProfile();
        Resources resources = this.mLauncher.getResources();
        this.mContentTransY = resources.getDimensionPixelSize(R.dimen.content_trans_y);
        this.mWorkspaceTransY = resources.getDimensionPixelSize(R.dimen.workspace_trans_y);
        this.mClosingWindowTransY = resources.getDimensionPixelSize(R.dimen.closing_window_trans_y);
        this.mLauncher.addOnDeviceProfileChangeListener(this);
        registerRemoteAnimations();
    }

    @Override
    public void onDeviceProfileChanged(DeviceProfile deviceProfile) {
        this.mDeviceProfile = deviceProfile;
    }

    @Override
    public ActivityOptions getActivityLaunchOptions(Launcher launcher, final View view) {
        if (hasControlRemoteAppTransitionPermission()) {
            return ActivityOptionsCompat.makeRemoteAnimation(new RemoteAnimationAdapterCompat(new LauncherAnimationRunner(this.mHandler, true) {
                @Override
                public void onCreateAnimation(RemoteAnimationTargetCompat[] remoteAnimationTargetCompatArr, LauncherAnimationRunner.AnimationResult animationResult) {
                    AnimatorSet animatorSet = new AnimatorSet();
                    boolean zLauncherIsATargetWithMode = LauncherAppTransitionManagerImpl.this.launcherIsATargetWithMode(remoteAnimationTargetCompatArr, 1);
                    if (!LauncherAppTransitionManagerImpl.this.composeRecentsLaunchAnimator(view, remoteAnimationTargetCompatArr, animatorSet)) {
                        LauncherAppTransitionManagerImpl.this.mLauncher.getStateManager().setCurrentAnimation(animatorSet, new Animator[0]);
                        Rect windowTargetBounds = LauncherAppTransitionManagerImpl.this.getWindowTargetBounds(remoteAnimationTargetCompatArr);
                        animatorSet.play(LauncherAppTransitionManagerImpl.this.getIconAnimator(view, windowTargetBounds));
                        if (zLauncherIsATargetWithMode) {
                            final Pair launcherContentAnimator = LauncherAppTransitionManagerImpl.this.getLauncherContentAnimator(true);
                            animatorSet.play((Animator) launcherContentAnimator.first);
                            animatorSet.addListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animator) {
                                    ((Runnable) launcherContentAnimator.second).run();
                                }
                            });
                        }
                        animatorSet.play(LauncherAppTransitionManagerImpl.this.getOpeningWindowAnimators(view, remoteAnimationTargetCompatArr, windowTargetBounds));
                    }
                    if (zLauncherIsATargetWithMode) {
                        animatorSet.addListener(LauncherAppTransitionManagerImpl.this.mForceInvisibleListener);
                    }
                    animationResult.setAnimation(animatorSet);
                }
            }, TaskUtils.findTaskViewToLaunch(launcher, view, null) != null ? RECENTS_LAUNCH_DURATION : 500, r8 - 120));
        }
        return super.getActivityLaunchOptions(launcher, view);
    }

    public Rect getWindowTargetBounds(RemoteAnimationTargetCompat[] remoteAnimationTargetCompatArr) {
        Rect rect = new Rect(0, 0, this.mDeviceProfile.widthPx, this.mDeviceProfile.heightPx);
        if (this.mLauncher.isInMultiWindowModeCompat()) {
            for (RemoteAnimationTargetCompat remoteAnimationTargetCompat : remoteAnimationTargetCompatArr) {
                if (remoteAnimationTargetCompat.mode == 0) {
                    rect.set(remoteAnimationTargetCompat.sourceContainerBounds);
                    rect.offsetTo(remoteAnimationTargetCompat.position.x, remoteAnimationTargetCompat.position.y);
                    return rect;
                }
            }
        }
        return rect;
    }

    public void setRemoteAnimationProvider(final RemoteAnimationProvider remoteAnimationProvider, CancellationSignal cancellationSignal) {
        this.mRemoteAnimationProvider = remoteAnimationProvider;
        cancellationSignal.setOnCancelListener(new CancellationSignal.OnCancelListener() {
            @Override
            public final void onCancel() {
                LauncherAppTransitionManagerImpl.lambda$setRemoteAnimationProvider$0(this.f$0, remoteAnimationProvider);
            }
        });
    }

    public static void lambda$setRemoteAnimationProvider$0(LauncherAppTransitionManagerImpl launcherAppTransitionManagerImpl, RemoteAnimationProvider remoteAnimationProvider) {
        if (remoteAnimationProvider == launcherAppTransitionManagerImpl.mRemoteAnimationProvider) {
            launcherAppTransitionManagerImpl.mRemoteAnimationProvider = null;
        }
    }

    public boolean composeRecentsLaunchAnimator(View view, RemoteAnimationTargetCompat[] remoteAnimationTargetCompatArr, AnimatorSet animatorSet) {
        int i;
        Animator duration;
        AnimatorListenerAdapter animatorListenerAdapter;
        if (!this.mLauncher.getStateManager().getState().overviewUi) {
            return false;
        }
        RecentsView recentsView = (RecentsView) this.mLauncher.getOverviewPanel();
        boolean zLauncherIsATargetWithMode = launcherIsATargetWithMode(remoteAnimationTargetCompatArr, 1);
        boolean z = !zLauncherIsATargetWithMode;
        boolean zIsWaitingForTaskLaunch = recentsView.getQuickScrubController().isWaitingForTaskLaunch();
        TaskView taskViewFindTaskViewToLaunch = TaskUtils.findTaskViewToLaunch(this.mLauncher, view, remoteAnimationTargetCompatArr);
        if (taskViewFindTaskViewToLaunch == null) {
            return false;
        }
        if (zIsWaitingForTaskLaunch) {
            i = 300;
        } else {
            i = RECENTS_LAUNCH_DURATION;
        }
        ClipAnimationHelper clipAnimationHelper = new ClipAnimationHelper();
        ValueAnimator recentsWindowAnimator = TaskUtils.getRecentsWindowAnimator(taskViewFindTaskViewToLaunch, z, remoteAnimationTargetCompatArr, clipAnimationHelper);
        long j = i;
        animatorSet.play(recentsWindowAnimator.setDuration(j));
        AnimatorSet target = null;
        if (zLauncherIsATargetWithMode) {
            duration = recentsView.createAdjacentPageAnimForTaskLaunch(taskViewFindTaskViewToLaunch, clipAnimationHelper);
            duration.setInterpolator(Interpolators.TOUCH_RESPONSE_INTERPOLATOR);
            duration.setDuration(j);
            animatorListenerAdapter = new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    LauncherAppTransitionManagerImpl.this.mLauncher.getStateManager().moveToRestState();
                    LauncherAppTransitionManagerImpl.this.mLauncher.getStateManager().reapplyState();
                }
            };
        } else {
            AnimatorPlaybackController animatorPlaybackControllerCreateAnimationToNewWorkspace = this.mLauncher.getStateManager().createAnimationToNewWorkspace(LauncherState.NORMAL, j);
            animatorPlaybackControllerCreateAnimationToNewWorkspace.dispatchOnStart();
            target = animatorPlaybackControllerCreateAnimationToNewWorkspace.getTarget();
            duration = animatorPlaybackControllerCreateAnimationToNewWorkspace.getAnimationPlayer().setDuration(j);
            animatorListenerAdapter = new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    LauncherAppTransitionManagerImpl.this.mLauncher.getStateManager().goToState(LauncherState.NORMAL, false);
                }
            };
        }
        animatorSet.play(duration);
        this.mLauncher.getStateManager().setCurrentAnimation(animatorSet, target);
        animatorSet.addListener(animatorListenerAdapter);
        return true;
    }

    public Pair<AnimatorSet, Runnable> getLauncherContentAnimator(boolean z) {
        float[] fArr;
        float[] fArr2;
        Runnable runnable;
        AnimatorSet animatorSet = new AnimatorSet();
        if (z) {
            fArr = new float[]{1.0f, 0.0f};
        } else {
            fArr = new float[]{0.0f, 1.0f};
        }
        if (z) {
            fArr2 = new float[]{0.0f, this.mContentTransY};
        } else {
            fArr2 = new float[]{-this.mContentTransY, 0.0f};
        }
        if (this.mLauncher.isInState(LauncherState.ALL_APPS)) {
            final AllAppsContainerView appsView = this.mLauncher.getAppsView();
            final float alpha = appsView.getAlpha();
            final float translationY = appsView.getTranslationY();
            appsView.setAlpha(fArr[0]);
            appsView.setTranslationY(fArr2[0]);
            ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(appsView, (Property<AllAppsContainerView, Float>) View.ALPHA, fArr);
            objectAnimatorOfFloat.setDuration(217L);
            objectAnimatorOfFloat.setInterpolator(Interpolators.LINEAR);
            appsView.setLayerType(2, null);
            objectAnimatorOfFloat.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    appsView.setLayerType(0, null);
                }
            });
            ObjectAnimator objectAnimatorOfFloat2 = ObjectAnimator.ofFloat(appsView, (Property<AllAppsContainerView, Float>) View.TRANSLATION_Y, fArr2);
            objectAnimatorOfFloat2.setInterpolator(Interpolators.AGGRESSIVE_EASE);
            objectAnimatorOfFloat2.setDuration(350L);
            animatorSet.play(objectAnimatorOfFloat);
            animatorSet.play(objectAnimatorOfFloat2);
            runnable = new Runnable() {
                @Override
                public final void run() {
                    LauncherAppTransitionManagerImpl.lambda$getLauncherContentAnimator$1(appsView, alpha, translationY);
                }
            };
        } else if (this.mLauncher.isInState(LauncherState.OVERVIEW)) {
            AllAppsTransitionController allAppsController = this.mLauncher.getAllAppsController();
            animatorSet.play(ObjectAnimator.ofFloat(allAppsController, AllAppsTransitionController.ALL_APPS_PROGRESS, allAppsController.getProgress(), 1.3059858f));
            final View overviewPanelContainer = this.mLauncher.getOverviewPanelContainer();
            ObjectAnimator objectAnimatorOfFloat3 = ObjectAnimator.ofFloat(overviewPanelContainer, (Property<View, Float>) View.ALPHA, fArr);
            objectAnimatorOfFloat3.setDuration(217L);
            objectAnimatorOfFloat3.setInterpolator(Interpolators.LINEAR);
            animatorSet.play(objectAnimatorOfFloat3);
            ObjectAnimator objectAnimatorOfFloat4 = ObjectAnimator.ofFloat(overviewPanelContainer, (Property<View, Float>) View.TRANSLATION_Y, fArr2);
            objectAnimatorOfFloat4.setInterpolator(Interpolators.AGGRESSIVE_EASE);
            objectAnimatorOfFloat4.setDuration(350L);
            animatorSet.play(objectAnimatorOfFloat4);
            overviewPanelContainer.setLayerType(2, null);
            runnable = new Runnable() {
                @Override
                public final void run() {
                    LauncherAppTransitionManagerImpl.lambda$getLauncherContentAnimator$2(this.f$0, overviewPanelContainer);
                }
            };
        } else {
            this.mDragLayerAlpha.setValue(fArr[0]);
            ObjectAnimator objectAnimatorOfFloat5 = ObjectAnimator.ofFloat(this.mDragLayerAlpha, MultiValueAlpha.VALUE, fArr);
            objectAnimatorOfFloat5.setDuration(217L);
            objectAnimatorOfFloat5.setInterpolator(Interpolators.LINEAR);
            animatorSet.play(objectAnimatorOfFloat5);
            this.mDragLayer.setTranslationY(fArr2[0]);
            ObjectAnimator objectAnimatorOfFloat6 = ObjectAnimator.ofFloat(this.mDragLayer, (Property<DragLayer, Float>) View.TRANSLATION_Y, fArr2);
            objectAnimatorOfFloat6.setInterpolator(Interpolators.AGGRESSIVE_EASE);
            objectAnimatorOfFloat6.setDuration(350L);
            animatorSet.play(objectAnimatorOfFloat6);
            this.mDragLayer.getScrim().hideSysUiScrim(true);
            this.mLauncher.getWorkspace().getPageIndicator().pauseAnimations();
            this.mDragLayer.setLayerType(2, null);
            runnable = new Runnable() {
                @Override
                public final void run() {
                    this.f$0.resetContentView();
                }
            };
        }
        return new Pair<>(animatorSet, runnable);
    }

    static void lambda$getLauncherContentAnimator$1(View view, float f, float f2) {
        view.setAlpha(f);
        view.setTranslationY(f2);
        view.setLayerType(0, null);
    }

    public static void lambda$getLauncherContentAnimator$2(LauncherAppTransitionManagerImpl launcherAppTransitionManagerImpl, View view) {
        view.setLayerType(0, null);
        view.setAlpha(1.0f);
        view.setTranslationY(0.0f);
        launcherAppTransitionManagerImpl.mLauncher.getStateManager().reapplyState();
    }

    public AnimatorSet getIconAnimator(final View view, Rect rect) {
        int iWidth;
        float marginStart;
        boolean z = view instanceof BubbleTextView;
        this.mFloatingView = new View(this.mLauncher);
        if (z && (view.getTag() instanceof ItemInfoWithIcon)) {
            this.mFloatingView.setBackground(DrawableFactory.get(this.mLauncher).newIcon((ItemInfoWithIcon) view.getTag()));
        }
        Rect rect2 = new Rect();
        boolean z2 = view.getParent() instanceof DeepShortcutView;
        if (z2) {
            this.mDragLayer.getDescendantRectRelativeToSelf(((DeepShortcutView) view.getParent()).getIconView(), rect2);
        } else {
            this.mDragLayer.getDescendantRectRelativeToSelf(view, rect2);
        }
        int i = rect2.left;
        int i2 = rect2.top;
        float animatedScale = 1.0f;
        if (!z || z2) {
            rect2.set(0, 0, rect2.width(), rect2.height());
        } else {
            BubbleTextView bubbleTextView = (BubbleTextView) view;
            bubbleTextView.getIconBounds(rect2);
            Drawable icon = bubbleTextView.getIcon();
            if (icon instanceof FastBitmapDrawable) {
                animatedScale = ((FastBitmapDrawable) icon).getAnimatedScale();
            }
        }
        int i3 = i + rect2.left;
        int i4 = i2 + rect2.top;
        if (this.mIsRtl) {
            iWidth = rect.width() - rect2.right;
        } else {
            iWidth = i3;
        }
        InsettableFrameLayout.LayoutParams layoutParams = new InsettableFrameLayout.LayoutParams(rect2.width(), rect2.height());
        layoutParams.ignoreInsets = true;
        layoutParams.setMarginStart(iWidth);
        layoutParams.topMargin = i4;
        this.mFloatingView.setLayoutParams(layoutParams);
        this.mFloatingView.setLeft(i3);
        this.mFloatingView.setTop(i4);
        this.mFloatingView.setRight(i3 + rect2.width());
        this.mFloatingView.setBottom(i4 + rect2.height());
        ((ViewGroup) this.mDragLayer.getParent()).addView(this.mFloatingView);
        view.setVisibility(4);
        AnimatorSet animatorSet = new AnimatorSet();
        int[] iArr = new int[2];
        this.mDragLayer.getLocationOnScreen(iArr);
        float fCenterX = rect.centerX() - iArr[0];
        float fCenterY = rect.centerY() - iArr[1];
        if (this.mIsRtl) {
            marginStart = (rect.width() - layoutParams.getMarginStart()) - rect2.width();
        } else {
            marginStart = layoutParams.getMarginStart();
        }
        float f = (fCenterX - marginStart) - (layoutParams.width / 2);
        float f2 = (fCenterY - layoutParams.topMargin) - (layoutParams.height / 2);
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(this.mFloatingView, (Property<View, Float>) View.TRANSLATION_X, 0.0f, f);
        ObjectAnimator objectAnimatorOfFloat2 = ObjectAnimator.ofFloat(this.mFloatingView, (Property<View, Float>) View.TRANSLATION_Y, 0.0f, f2);
        boolean z3 = ((float) layoutParams.topMargin) > fCenterY || Math.abs(f2) < ((float) this.mLauncher.getDeviceProfile().cellHeightPx);
        if (z3) {
            objectAnimatorOfFloat.setDuration(250L);
            objectAnimatorOfFloat2.setDuration(500L);
        } else {
            objectAnimatorOfFloat.setDuration(400L);
            objectAnimatorOfFloat2.setDuration(200L);
        }
        objectAnimatorOfFloat.setInterpolator(Interpolators.AGGRESSIVE_EASE);
        objectAnimatorOfFloat2.setInterpolator(Interpolators.AGGRESSIVE_EASE);
        animatorSet.play(objectAnimatorOfFloat);
        animatorSet.play(objectAnimatorOfFloat2);
        ObjectAnimator objectAnimatorOfFloat3 = ObjectAnimator.ofFloat(this.mFloatingView, LauncherAnimUtils.SCALE_PROPERTY, animatedScale, Math.max(rect.width() / rect2.width(), rect.height() / rect2.height()));
        objectAnimatorOfFloat3.setDuration(500L).setInterpolator(Interpolators.EXAGGERATED_EASE);
        animatorSet.play(objectAnimatorOfFloat3);
        ObjectAnimator objectAnimatorOfFloat4 = ObjectAnimator.ofFloat(this.mFloatingView, (Property<View, Float>) View.ALPHA, 1.0f, 0.0f);
        if (z3) {
            objectAnimatorOfFloat4.setStartDelay(32L);
            objectAnimatorOfFloat4.setDuration(50L);
        } else {
            objectAnimatorOfFloat4.setStartDelay(25L);
            objectAnimatorOfFloat4.setDuration(40L);
        }
        objectAnimatorOfFloat4.setInterpolator(Interpolators.LINEAR);
        animatorSet.play(objectAnimatorOfFloat4);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                view.setVisibility(0);
                ((ViewGroup) LauncherAppTransitionManagerImpl.this.mDragLayer.getParent()).removeView(LauncherAppTransitionManagerImpl.this.mFloatingView);
            }
        });
        return animatorSet;
    }

    public ValueAnimator getOpeningWindowAnimators(View view, final RemoteAnimationTargetCompat[] remoteAnimationTargetCompatArr, final Rect rect) {
        final Rect rect2 = new Rect();
        if (view.getParent() instanceof DeepShortcutView) {
            this.mDragLayer.getDescendantRectRelativeToSelf(((DeepShortcutView) view.getParent()).getIconView(), rect2);
        } else if (view instanceof BubbleTextView) {
            ((BubbleTextView) view).getIconBounds(rect2);
        } else {
            this.mDragLayer.getDescendantRectRelativeToSelf(view, rect2);
        }
        final int[] iArr = new int[2];
        final Rect rect3 = new Rect();
        final Matrix matrix = new Matrix();
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
        valueAnimatorOfFloat.setDuration(500L);
        valueAnimatorOfFloat.addUpdateListener(new MultiValueUpdateListener() {
            MultiValueUpdateListener.FloatProp mAlpha = new MultiValueUpdateListener.FloatProp(0.0f, 1.0f, 0.0f, 60.0f, Interpolators.LINEAR);
            boolean isFirstFrame = true;

            @Override
            public void onUpdate(float f) {
                Surface surface = com.android.systemui.shared.recents.utilities.Utilities.getSurface(LauncherAppTransitionManagerImpl.this.mFloatingView);
                if ((surface != null ? com.android.systemui.shared.recents.utilities.Utilities.getNextFrameNumber(surface) : -1L) == -1) {
                    Log.w(LauncherAppTransitionManagerImpl.TAG, "Failed to animate, surface got destroyed.");
                    return;
                }
                float interpolation = Interpolators.AGGRESSIVE_EASE.getInterpolation(f);
                float fWidth = rect2.width() * LauncherAppTransitionManagerImpl.this.mFloatingView.getScaleX();
                float fHeight = rect2.height() * LauncherAppTransitionManagerImpl.this.mFloatingView.getScaleY();
                float fMin = Math.min(1.0f, Math.min(fWidth / rect.width(), fHeight / rect.height()));
                matrix.setScale(fMin, fMin);
                int iWidth = rect.width();
                float f2 = iWidth;
                float fHeight2 = rect.height();
                LauncherAppTransitionManagerImpl.this.mFloatingView.getLocationOnScreen(iArr);
                matrix.postTranslate(iArr[0] - (((f2 * fMin) - fWidth) / 2.0f), iArr[1] - (((fMin * fHeight2) - fHeight) / 2.0f));
                float f3 = 1.0f - interpolation;
                rect3.left = 0;
                rect3.top = (int) (((r6 - iWidth) / 2.0f) * f3);
                rect3.right = iWidth;
                rect3.bottom = (int) (rect3.top + (fHeight2 * interpolation) + (f2 * f3));
                TransactionCompat transactionCompat = new TransactionCompat();
                if (this.isFirstFrame) {
                    RemoteAnimationProvider.prepareTargetsForFirstFrame(remoteAnimationTargetCompatArr, transactionCompat, 0);
                    this.isFirstFrame = false;
                }
                for (RemoteAnimationTargetCompat remoteAnimationTargetCompat : remoteAnimationTargetCompatArr) {
                    if (remoteAnimationTargetCompat.mode == 0) {
                        transactionCompat.setAlpha(remoteAnimationTargetCompat.leash, this.mAlpha.value);
                        transactionCompat.setMatrix(remoteAnimationTargetCompat.leash, matrix);
                        transactionCompat.setWindowCrop(remoteAnimationTargetCompat.leash, rect3);
                        transactionCompat.deferTransactionUntil(remoteAnimationTargetCompat.leash, surface, com.android.systemui.shared.recents.utilities.Utilities.getNextFrameNumber(surface));
                    }
                }
                transactionCompat.setEarlyWakeup();
                transactionCompat.apply();
                matrix.reset();
            }
        });
        return valueAnimatorOfFloat;
    }

    private void registerRemoteAnimations() {
        if (hasControlRemoteAppTransitionPermission()) {
            RemoteAnimationDefinitionCompat remoteAnimationDefinitionCompat = new RemoteAnimationDefinitionCompat();
            remoteAnimationDefinitionCompat.addRemoteAnimation(13, 1, new RemoteAnimationAdapterCompat(getWallpaperOpenRunner(), 250L, 0L));
            new ActivityCompat(this.mLauncher).registerRemoteAnimations(remoteAnimationDefinitionCompat);
        }
    }

    public boolean launcherIsATargetWithMode(RemoteAnimationTargetCompat[] remoteAnimationTargetCompatArr, int i) {
        return TaskUtils.taskIsATargetWithMode(remoteAnimationTargetCompatArr, this.mLauncher.getTaskId(), i);
    }

    class AnonymousClass8 extends LauncherAnimationRunner {
        AnonymousClass8(Handler handler, boolean z) {
            super(handler, z);
        }

        @Override
        public void onCreateAnimation(final RemoteAnimationTargetCompat[] remoteAnimationTargetCompatArr, final LauncherAnimationRunner.AnimationResult animationResult) {
            if (!LauncherAppTransitionManagerImpl.this.mLauncher.hasBeenResumed()) {
                LauncherAppTransitionManagerImpl.this.mLauncher.setOnResumeCallback(new Launcher.OnResumeCallback() {
                    @Override
                    public final void onLauncherResume() {
                        LauncherAppTransitionManagerImpl.AnonymousClass8 anonymousClass8 = this.f$0;
                        Utilities.postAsyncCallback(LauncherAppTransitionManagerImpl.this.mHandler, new Runnable() {
                            @Override
                            public final void run() {
                                this.f$0.onCreateAnimation(remoteAnimationTargetCompatArr, animationResult);
                            }
                        });
                    }
                });
                return;
            }
            AnimatorSet animatorSet = null;
            RemoteAnimationProvider remoteAnimationProvider = LauncherAppTransitionManagerImpl.this.mRemoteAnimationProvider;
            if (remoteAnimationProvider != null) {
                animatorSet = remoteAnimationProvider.createWindowAnimation(remoteAnimationTargetCompatArr);
            }
            if (animatorSet == null) {
                animatorSet = new AnimatorSet();
                animatorSet.play(LauncherAppTransitionManagerImpl.this.getClosingWindowAnimators(remoteAnimationTargetCompatArr));
                if (LauncherAppTransitionManagerImpl.this.launcherIsATargetWithMode(remoteAnimationTargetCompatArr, 0) || LauncherAppTransitionManagerImpl.this.mLauncher.isForceInvisible()) {
                    LauncherAppTransitionManagerImpl.this.mLauncher.getStateManager().setCurrentAnimation(animatorSet, new Animator[0]);
                    LauncherAppTransitionManagerImpl.this.createLauncherResumeAnimation(animatorSet);
                }
            }
            LauncherAppTransitionManagerImpl.this.mLauncher.clearForceInvisibleFlag(3);
            animationResult.setAnimation(animatorSet);
        }
    }

    private RemoteAnimationRunnerCompat getWallpaperOpenRunner() {
        return new AnonymousClass8(this.mHandler, false);
    }

    public Animator getClosingWindowAnimators(final RemoteAnimationTargetCompat[] remoteAnimationTargetCompatArr) {
        final Matrix matrix = new Matrix();
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
        final int i = 250;
        valueAnimatorOfFloat.setDuration(250);
        valueAnimatorOfFloat.addUpdateListener(new MultiValueUpdateListener() {
            MultiValueUpdateListener.FloatProp mDy;
            MultiValueUpdateListener.FloatProp mScale;
            MultiValueUpdateListener.FloatProp mAlpha = new MultiValueUpdateListener.FloatProp(1.0f, 0.0f, 25.0f, 125.0f, Interpolators.LINEAR);
            boolean isFirstFrame = true;

            {
                this.mDy = new MultiValueUpdateListener.FloatProp(0.0f, LauncherAppTransitionManagerImpl.this.mClosingWindowTransY, 0.0f, i, Interpolators.DEACCEL_1_7);
                this.mScale = new MultiValueUpdateListener.FloatProp(1.0f, 1.0f, 0.0f, i, Interpolators.DEACCEL_1_7);
            }

            @Override
            public void onUpdate(float f) {
                TransactionCompat transactionCompat = new TransactionCompat();
                if (this.isFirstFrame) {
                    RemoteAnimationProvider.prepareTargetsForFirstFrame(remoteAnimationTargetCompatArr, transactionCompat, 1);
                    this.isFirstFrame = false;
                }
                for (RemoteAnimationTargetCompat remoteAnimationTargetCompat : remoteAnimationTargetCompatArr) {
                    if (remoteAnimationTargetCompat.mode == 1) {
                        transactionCompat.setAlpha(remoteAnimationTargetCompat.leash, this.mAlpha.value);
                        matrix.setScale(this.mScale.value, this.mScale.value, remoteAnimationTargetCompat.sourceContainerBounds.centerX(), remoteAnimationTargetCompat.sourceContainerBounds.centerY());
                        matrix.postTranslate(0.0f, this.mDy.value);
                        matrix.postTranslate(remoteAnimationTargetCompat.position.x, remoteAnimationTargetCompat.position.y);
                        transactionCompat.setMatrix(remoteAnimationTargetCompat.leash, matrix);
                    }
                }
                transactionCompat.setEarlyWakeup();
                transactionCompat.apply();
                matrix.reset();
            }
        });
        return valueAnimatorOfFloat;
    }

    public void createLauncherResumeAnimation(AnimatorSet animatorSet) {
        if (this.mLauncher.isInState(LauncherState.ALL_APPS)) {
            final Pair<AnimatorSet, Runnable> launcherContentAnimator = getLauncherContentAnimator(false);
            ((AnimatorSet) launcherContentAnimator.first).setStartDelay(100L);
            animatorSet.play((Animator) launcherContentAnimator.first);
            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    ((Runnable) launcherContentAnimator.second).run();
                }
            });
            return;
        }
        AnimatorSet animatorSet2 = new AnimatorSet();
        this.mDragLayer.setTranslationY(-this.mWorkspaceTransY);
        animatorSet2.play(ObjectAnimator.ofFloat(this.mDragLayer, (Property<DragLayer, Float>) View.TRANSLATION_Y, -this.mWorkspaceTransY, 0.0f));
        this.mDragLayerAlpha.setValue(0.0f);
        animatorSet2.play(ObjectAnimator.ofFloat(this.mDragLayerAlpha, MultiValueAlpha.VALUE, 0.0f, 1.0f));
        animatorSet2.setStartDelay(100L);
        animatorSet2.setDuration(333L);
        animatorSet2.setInterpolator(Interpolators.DEACCEL_1_7);
        this.mDragLayer.getScrim().hideSysUiScrim(true);
        this.mLauncher.getWorkspace().getPageIndicator().pauseAnimations();
        this.mDragLayer.setLayerType(2, null);
        animatorSet2.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                LauncherAppTransitionManagerImpl.this.resetContentView();
            }
        });
        animatorSet.play(animatorSet2);
    }

    public void resetContentView() {
        this.mLauncher.getWorkspace().getPageIndicator().skipAnimationsToEnd();
        this.mDragLayerAlpha.setValue(1.0f);
        this.mDragLayer.setLayerType(0, null);
        this.mDragLayer.setTranslationY(0.0f);
        this.mDragLayer.getScrim().hideSysUiScrim(false);
    }

    private boolean hasControlRemoteAppTransitionPermission() {
        return this.mLauncher.checkSelfPermission(CONTROL_REMOTE_APP_TRANSITION_PERMISSION) == 0;
    }
}
