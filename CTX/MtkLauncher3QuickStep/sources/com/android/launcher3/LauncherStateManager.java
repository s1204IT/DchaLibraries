package com.android.launcher3;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.os.Handler;
import android.os.Looper;
import com.android.launcher3.anim.AnimationSuccessListener;
import com.android.launcher3.anim.AnimatorPlaybackController;
import com.android.launcher3.anim.AnimatorSetBuilder;
import com.android.launcher3.anim.Interpolators;
import com.android.launcher3.anim.PropertySetter;
import com.android.launcher3.uioverrides.UiFactory;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

public class LauncherStateManager {
    public static final int ANIM_ALL = 3;
    public static final int ATOMIC_COMPONENT = 2;
    public static final int NON_ATOMIC_COMPONENT = 1;
    public static final String TAG = "StateManager";
    private final Launcher mLauncher;
    private LauncherState mRestState;
    private StateHandler[] mStateHandlers;
    private final AnimationConfig mConfig = new AnimationConfig();
    private final ArrayList<StateListener> mListeners = new ArrayList<>();
    private LauncherState mState = LauncherState.NORMAL;
    private LauncherState mLastStableState = LauncherState.NORMAL;
    private LauncherState mCurrentStableState = LauncherState.NORMAL;
    private final Handler mUiHandler = new Handler(Looper.getMainLooper());

    @Retention(RetentionPolicy.SOURCE)
    public @interface AnimationComponents {
    }

    public interface StateHandler {
        void setState(LauncherState launcherState);

        void setStateWithAnimation(LauncherState launcherState, AnimatorSetBuilder animatorSetBuilder, AnimationConfig animationConfig);
    }

    public interface StateListener {
        void onStateSetImmediately(LauncherState launcherState);

        void onStateTransitionComplete(LauncherState launcherState);

        void onStateTransitionStart(LauncherState launcherState);
    }

    public LauncherStateManager(Launcher launcher) {
        this.mLauncher = launcher;
    }

    public LauncherState getState() {
        return this.mState;
    }

    public StateHandler[] getStateHandlers() {
        if (this.mStateHandlers == null) {
            this.mStateHandlers = UiFactory.getStateHandler(this.mLauncher);
        }
        return this.mStateHandlers;
    }

    public void addStateListener(StateListener stateListener) {
        this.mListeners.add(stateListener);
    }

    public void removeStateListener(StateListener stateListener) {
        this.mListeners.remove(stateListener);
    }

    public void goToState(LauncherState launcherState) {
        goToState(launcherState, !this.mLauncher.isForceInvisible() && this.mLauncher.isStarted());
    }

    public void goToState(LauncherState launcherState, boolean z) {
        goToState(launcherState, z, 0L, null);
    }

    public void goToState(LauncherState launcherState, boolean z, Runnable runnable) {
        goToState(launcherState, z, 0L, runnable);
    }

    public void goToState(LauncherState launcherState, long j, Runnable runnable) {
        goToState(launcherState, true, j, runnable);
    }

    public void goToState(LauncherState launcherState, long j) {
        goToState(launcherState, true, j, null);
    }

    public void reapplyState() {
        reapplyState(false);
    }

    public void reapplyState(boolean z) {
        if (z) {
            cancelAnimation();
        }
        if (this.mConfig.mCurrentAnimation == null) {
            for (StateHandler stateHandler : getStateHandlers()) {
                stateHandler.setState(this.mState);
            }
        }
    }

    private void goToState(LauncherState launcherState, boolean z, long j, final Runnable runnable) {
        if (this.mLauncher.isInState(launcherState)) {
            if (this.mConfig.mCurrentAnimation == null) {
                if (runnable != null) {
                    runnable.run();
                    return;
                }
                return;
            } else if (!this.mConfig.userControlled && z && this.mConfig.mTargetState == launcherState) {
                if (runnable == null) {
                    return;
                }
                this.mConfig.mCurrentAnimation.addListener(new AnimationSuccessListener() {
                    @Override
                    public void onAnimationSuccess(Animator animator) {
                        runnable.run();
                    }
                });
                return;
            }
        }
        LauncherState launcherState2 = this.mState;
        this.mConfig.reset();
        if (!z) {
            onStateTransitionStart(launcherState);
            for (StateHandler stateHandler : getStateHandlers()) {
                stateHandler.setState(launcherState);
            }
            for (int size = this.mListeners.size() - 1; size >= 0; size--) {
                this.mListeners.get(size).onStateSetImmediately(launcherState);
            }
            onStateTransitionEnd(launcherState);
            if (runnable != null) {
                runnable.run();
                return;
            }
            return;
        }
        this.mConfig.duration = launcherState == LauncherState.NORMAL ? launcherState2.transitionDuration : launcherState.transitionDuration;
        AnimatorSetBuilder animatorSetBuilder = new AnimatorSetBuilder();
        prepareForAtomicAnimation(launcherState2, launcherState, animatorSetBuilder);
        StartAnimRunnable startAnimRunnable = new StartAnimRunnable(createAnimationToNewWorkspaceInternal(launcherState, animatorSetBuilder, runnable));
        if (j > 0) {
            this.mUiHandler.postDelayed(startAnimRunnable, j);
        } else {
            this.mUiHandler.post(startAnimRunnable);
        }
    }

    public void prepareForAtomicAnimation(LauncherState launcherState, LauncherState launcherState2, AnimatorSetBuilder animatorSetBuilder) {
        if (launcherState == LauncherState.NORMAL && launcherState2.overviewUi) {
            animatorSetBuilder.setInterpolator(1, Interpolators.OVERSHOOT_1_2);
            animatorSetBuilder.setInterpolator(2, Interpolators.OVERSHOOT_1_2);
            animatorSetBuilder.setInterpolator(3, Interpolators.OVERSHOOT_1_2);
            animatorSetBuilder.setInterpolator(4, Interpolators.OVERSHOOT_1_2);
            UiFactory.prepareToShowOverview(this.mLauncher);
            return;
        }
        if (launcherState.overviewUi && launcherState2 == LauncherState.NORMAL) {
            animatorSetBuilder.setInterpolator(1, Interpolators.DEACCEL);
            animatorSetBuilder.setInterpolator(2, Interpolators.ACCEL);
            animatorSetBuilder.setInterpolator(3, Interpolators.clampToProgress(Interpolators.ACCEL, 0.0f, 0.9f));
            animatorSetBuilder.setInterpolator(4, Interpolators.DEACCEL_1_7);
            Workspace workspace = this.mLauncher.getWorkspace();
            boolean z = workspace.getVisibility() == 0;
            if (z) {
                CellLayout cellLayout = (CellLayout) workspace.getChildAt(workspace.getCurrentPage());
                z = cellLayout.getVisibility() == 0 && cellLayout.getShortcutsAndWidgets().getAlpha() > 0.0f;
            }
            if (!z) {
                workspace.setScaleX(0.92f);
                workspace.setScaleY(0.92f);
            }
        }
    }

    public AnimatorPlaybackController createAnimationToNewWorkspace(LauncherState launcherState, long j) {
        return createAnimationToNewWorkspace(launcherState, j, 3);
    }

    public AnimatorPlaybackController createAnimationToNewWorkspace(LauncherState launcherState, long j, int i) {
        return createAnimationToNewWorkspace(launcherState, new AnimatorSetBuilder(), j, null, i);
    }

    public AnimatorPlaybackController createAnimationToNewWorkspace(LauncherState launcherState, AnimatorSetBuilder animatorSetBuilder, long j, Runnable runnable, int i) {
        this.mConfig.reset();
        this.mConfig.userControlled = true;
        this.mConfig.animComponents = i;
        this.mConfig.duration = j;
        this.mConfig.playbackController = AnimatorPlaybackController.wrap(createAnimationToNewWorkspaceInternal(launcherState, animatorSetBuilder, null), j, runnable);
        return this.mConfig.playbackController;
    }

    protected AnimatorSet createAnimationToNewWorkspaceInternal(final LauncherState launcherState, AnimatorSetBuilder animatorSetBuilder, final Runnable runnable) {
        for (StateHandler stateHandler : getStateHandlers()) {
            animatorSetBuilder.startTag(stateHandler);
            stateHandler.setStateWithAnimation(launcherState, animatorSetBuilder, this.mConfig);
        }
        AnimatorSet animatorSetBuild = animatorSetBuilder.build();
        animatorSetBuild.addListener(new AnimationSuccessListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                LauncherStateManager.this.onStateTransitionStart(launcherState);
                for (int size = LauncherStateManager.this.mListeners.size() - 1; size >= 0; size--) {
                    ((StateListener) LauncherStateManager.this.mListeners.get(size)).onStateTransitionStart(launcherState);
                }
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                super.onAnimationCancel(animator);
                LauncherStateManager.this.mState = LauncherStateManager.this.mCurrentStableState;
            }

            @Override
            public void onAnimationSuccess(Animator animator) {
                if (runnable != null) {
                    runnable.run();
                }
                LauncherStateManager.this.onStateTransitionEnd(launcherState);
                for (int size = LauncherStateManager.this.mListeners.size() - 1; size >= 0; size--) {
                    ((StateListener) LauncherStateManager.this.mListeners.get(size)).onStateTransitionComplete(launcherState);
                }
            }
        });
        this.mConfig.setAnimation(animatorSetBuild, launcherState);
        return this.mConfig.mCurrentAnimation;
    }

    private void onStateTransitionStart(LauncherState launcherState) {
        this.mState.onStateDisabled(this.mLauncher);
        this.mState = launcherState;
        this.mState.onStateEnabled(this.mLauncher);
        this.mLauncher.getAppWidgetHost().setResumed(launcherState == LauncherState.NORMAL);
        if (launcherState.disablePageClipping) {
            this.mLauncher.getWorkspace().setClipChildren(false);
        }
        UiFactory.onLauncherStateOrResumeChanged(this.mLauncher);
    }

    private void onStateTransitionEnd(LauncherState launcherState) {
        if (launcherState != this.mCurrentStableState) {
            this.mLastStableState = launcherState.getHistoryForState(this.mCurrentStableState);
            this.mCurrentStableState = launcherState;
        }
        launcherState.onStateTransitionEnd(this.mLauncher);
        this.mLauncher.getWorkspace().setClipChildren(!launcherState.disablePageClipping);
        this.mLauncher.finishAutoCancelActionMode();
        if (launcherState == LauncherState.NORMAL) {
            setRestState(null);
        }
        UiFactory.onLauncherStateOrResumeChanged(this.mLauncher);
        this.mLauncher.getDragLayer().requestFocus();
    }

    public void onWindowFocusChanged() {
        UiFactory.onLauncherStateOrFocusChanged(this.mLauncher);
    }

    public LauncherState getLastState() {
        return this.mLastStableState;
    }

    public void moveToRestState() {
        if ((this.mConfig.mCurrentAnimation == null || !this.mConfig.userControlled) && this.mState.disableRestore) {
            goToState(getRestState());
            this.mLastStableState = LauncherState.NORMAL;
        }
    }

    public LauncherState getRestState() {
        return this.mRestState == null ? LauncherState.NORMAL : this.mRestState;
    }

    public void setRestState(LauncherState launcherState) {
        this.mRestState = launcherState;
    }

    public void cancelAnimation() {
        this.mConfig.reset();
    }

    public void setCurrentUserControlledAnimation(AnimatorPlaybackController animatorPlaybackController) {
        setCurrentAnimation(animatorPlaybackController.getTarget(), new Animator[0]);
        this.mConfig.userControlled = true;
        this.mConfig.playbackController = animatorPlaybackController;
    }

    public void setCurrentAnimation(AnimatorSet animatorSet, Animator... animatorArr) {
        int length = animatorArr.length;
        int i = 0;
        while (true) {
            if (i >= length) {
                break;
            }
            Animator animator = animatorArr[i];
            if (animator != null) {
                if (this.mConfig.playbackController != null && this.mConfig.playbackController.getTarget() == animator) {
                    clearCurrentAnimation();
                    break;
                } else if (this.mConfig.mCurrentAnimation == animator) {
                    clearCurrentAnimation();
                    break;
                }
            }
            i++;
        }
        boolean z = this.mConfig.mCurrentAnimation != null;
        cancelAnimation();
        if (z) {
            reapplyState();
        }
        this.mConfig.setAnimation(animatorSet, null);
    }

    private void clearCurrentAnimation() {
        if (this.mConfig.mCurrentAnimation != null) {
            this.mConfig.mCurrentAnimation.removeListener(this.mConfig);
            this.mConfig.mCurrentAnimation = null;
        }
        this.mConfig.playbackController = null;
    }

    private class StartAnimRunnable implements Runnable {
        private final AnimatorSet mAnim;

        public StartAnimRunnable(AnimatorSet animatorSet) {
            this.mAnim = animatorSet;
        }

        @Override
        public void run() {
            if (LauncherStateManager.this.mConfig.mCurrentAnimation != this.mAnim) {
                return;
            }
            this.mAnim.start();
        }
    }

    public static class AnimationConfig extends AnimatorListenerAdapter {
        public int animComponents = 3;
        public long duration;
        private AnimatorSet mCurrentAnimation;
        private PropertySetter mPropertySetter;
        private LauncherState mTargetState;
        public AnimatorPlaybackController playbackController;
        public boolean userControlled;

        public void reset() {
            this.duration = 0L;
            this.userControlled = false;
            this.animComponents = 3;
            this.mPropertySetter = null;
            this.mTargetState = null;
            if (this.playbackController != null) {
                this.playbackController.getAnimationPlayer().cancel();
                this.playbackController.dispatchOnCancel();
            } else if (this.mCurrentAnimation != null) {
                this.mCurrentAnimation.setDuration(0L);
                this.mCurrentAnimation.cancel();
            }
            this.mCurrentAnimation = null;
            this.playbackController = null;
        }

        public PropertySetter getPropertySetter(AnimatorSetBuilder animatorSetBuilder) {
            if (this.mPropertySetter == null) {
                this.mPropertySetter = this.duration == 0 ? PropertySetter.NO_ANIM_PROPERTY_SETTER : new PropertySetter.AnimatedPropertySetter(this.duration, animatorSetBuilder);
            }
            return this.mPropertySetter;
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            if (this.mCurrentAnimation == animator) {
                this.mCurrentAnimation = null;
            }
        }

        public void setAnimation(AnimatorSet animatorSet, LauncherState launcherState) {
            this.mCurrentAnimation = animatorSet;
            this.mTargetState = launcherState;
            this.mCurrentAnimation.addListener(this);
        }

        public boolean playAtomicComponent() {
            return (this.animComponents & 2) != 0;
        }

        public boolean playNonAtomicComponent() {
            return (this.animComponents & 1) != 0;
        }
    }
}
