package com.android.quickstep;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import com.android.launcher3.BaseDraggingActivity;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherInitListener;
import com.android.launcher3.LauncherState;
import com.android.launcher3.R;
import com.android.launcher3.allapps.AllAppsTransitionController;
import com.android.launcher3.allapps.DiscoveryBounce;
import com.android.launcher3.anim.AnimatorPlaybackController;
import com.android.launcher3.anim.Interpolators;
import com.android.launcher3.uioverrides.FastOverviewState;
import com.android.launcher3.util.MultiValueAlpha;
import com.android.quickstep.ActivityControlHelper;
import com.android.quickstep.util.LayoutUtils;
import com.android.quickstep.util.RemoteAnimationProvider;
import com.android.quickstep.util.RemoteAnimationTargetSet;
import com.android.quickstep.util.TransformedRect;
import com.android.quickstep.views.LauncherLayoutListener;
import com.android.quickstep.views.RecentsView;
import com.android.quickstep.views.RecentsViewContainer;
import com.android.systemui.shared.system.RemoteAnimationTargetCompat;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

@TargetApi(28)
/* loaded from: classes.dex */
public interface ActivityControlHelper<T extends BaseDraggingActivity> {

    public interface ActivityInitListener {
        void register();

        void registerAndStartActivity(Intent intent, RemoteAnimationProvider remoteAnimationProvider, Context context, Handler handler, long j);

        void unregister();
    }

    public interface LayoutListener {
        void finish();

        void open();

        void setHandler(WindowTransformSwipeHandler windowTransformSwipeHandler);
    }

    ActivityInitListener createActivityInitListener(BiPredicate<T, Boolean> biPredicate);

    LayoutListener createLayoutListener(T t);

    boolean deferStartingActivity(int i);

    void executeOnWindowAvailable(T t, Runnable runnable);

    MultiValueAlpha.AlphaProperty getAlphaProperty(T t);

    int getContainerType();

    @Nullable
    T getCreatedActivity();

    LongSwipeHelper getLongSwipeController(T t, RemoteAnimationTargetSet remoteAnimationTargetSet);

    Rect getOverviewWindowBounds(Rect rect, RemoteAnimationTargetCompat remoteAnimationTargetCompat);

    int getSwipeUpDestinationAndLength(DeviceProfile deviceProfile, Context context, int i, TransformedRect transformedRect);

    float getTranslationYForQuickScrub(TransformedRect transformedRect, DeviceProfile deviceProfile, Context context);

    @UiThread
    @Nullable
    RecentsView getVisibleRecentsView();

    void onQuickInteractionStart(T t, @Nullable ActivityManager.RunningTaskInfo runningTaskInfo, boolean z);

    void onSwipeUpComplete(T t);

    void onTransitionCancelled(T t, boolean z);

    AnimationFactory prepareRecentsUI(T t, boolean z, Consumer<AnimatorPlaybackController> consumer);

    boolean shouldMinimizeSplitScreen();

    boolean supportsLongSwipe(T t);

    @UiThread
    boolean switchToRecentsIfVisible(boolean z);

    public static class LauncherActivityControllerHelper implements ActivityControlHelper<Launcher> {
        @Override // com.android.quickstep.ActivityControlHelper
        public /* bridge */ /* synthetic */ AnimationFactory prepareRecentsUI(BaseDraggingActivity baseDraggingActivity, boolean z, Consumer consumer) {
            return prepareRecentsUI((Launcher) baseDraggingActivity, z, (Consumer<AnimatorPlaybackController>) consumer);
        }

        /* JADX DEBUG: Method merged with bridge method: createLayoutListener(Lcom/android/launcher3/BaseDraggingActivity;)Lcom/android/quickstep/ActivityControlHelper$LayoutListener; */
        @Override // com.android.quickstep.ActivityControlHelper
        public LayoutListener createLayoutListener(Launcher launcher) {
            return new LauncherLayoutListener(launcher);
        }

        /* JADX DEBUG: Method merged with bridge method: onQuickInteractionStart(Lcom/android/launcher3/BaseDraggingActivity;Landroid/app/ActivityManager$RunningTaskInfo;Z)V */
        @Override // com.android.quickstep.ActivityControlHelper
        public void onQuickInteractionStart(Launcher launcher, ActivityManager.RunningTaskInfo runningTaskInfo, boolean z) {
            LauncherState state = launcher.getStateManager().getState();
            launcher.getStateManager().goToState(LauncherState.FAST_OVERVIEW, z);
            ((RecentsView) launcher.getOverviewPanel()).getQuickScrubController().onQuickScrubStart(z && !state.overviewUi, this);
        }

        @Override // com.android.quickstep.ActivityControlHelper
        public float getTranslationYForQuickScrub(TransformedRect transformedRect, DeviceProfile deviceProfile, Context context) throws Resources.NotFoundException {
            return 0.4f * (((deviceProfile.availableHeightPx + deviceProfile.getInsets().top) - transformedRect.rect.bottom) - ((transformedRect.rect.top - context.getResources().getDimensionPixelSize(R.dimen.task_thumbnail_top_margin)) - deviceProfile.getInsets().top));
        }

        /* JADX DEBUG: Method merged with bridge method: executeOnWindowAvailable(Lcom/android/launcher3/BaseDraggingActivity;Ljava/lang/Runnable;)V */
        @Override // com.android.quickstep.ActivityControlHelper
        public void executeOnWindowAvailable(Launcher launcher, Runnable runnable) {
            launcher.getWorkspace().runOnOverlayHidden(runnable);
        }

        @Override // com.android.quickstep.ActivityControlHelper
        public int getSwipeUpDestinationAndLength(DeviceProfile deviceProfile, Context context, int i, TransformedRect transformedRect) throws Resources.NotFoundException {
            LayoutUtils.calculateLauncherTaskSize(context, deviceProfile, transformedRect.rect);
            if (i == 1) {
                transformedRect.scale = FastOverviewState.getOverviewScale(deviceProfile, transformedRect.rect, context);
            }
            if (deviceProfile.isVerticalBarLayout()) {
                Rect insets = deviceProfile.getInsets();
                return deviceProfile.hotseatBarSizePx + deviceProfile.hotseatBarSidePaddingPx + (deviceProfile.isSeascape() ? insets.left : insets.right);
            }
            return deviceProfile.heightPx - transformedRect.rect.bottom;
        }

        /* JADX DEBUG: Method merged with bridge method: onTransitionCancelled(Lcom/android/launcher3/BaseDraggingActivity;Z)V */
        @Override // com.android.quickstep.ActivityControlHelper
        public void onTransitionCancelled(Launcher launcher, boolean z) {
            launcher.getStateManager().goToState(launcher.getStateManager().getRestState(), z);
        }

        /* JADX DEBUG: Method merged with bridge method: onSwipeUpComplete(Lcom/android/launcher3/BaseDraggingActivity;)V */
        @Override // com.android.quickstep.ActivityControlHelper
        public void onSwipeUpComplete(Launcher launcher) {
            launcher.getStateManager().reapplyState();
            DiscoveryBounce.showForOverviewIfNeeded(launcher);
        }

        public AnimationFactory prepareRecentsUI(final Launcher launcher, final boolean z, final Consumer<AnimatorPlaybackController> consumer) {
            LauncherState restState;
            final LauncherState state = launcher.getStateManager().getState();
            if (state.disableRestore) {
                restState = launcher.getStateManager().getRestState();
            } else {
                restState = state;
            }
            launcher.getStateManager().setRestState(restState);
            if (!z) {
                launcher.getAppsView().reset(false);
                launcher.getStateManager().goToState(LauncherState.OVERVIEW, false);
                launcher.getAppsView().getContentView().setVisibility(8);
            }
            return new AnimationFactory() { // from class: com.android.quickstep.ActivityControlHelper.LauncherActivityControllerHelper.1
                @Override // com.android.quickstep.ActivityControlHelper.AnimationFactory
                public void createActivityController(long j, int i) {
                    LauncherActivityControllerHelper.this.createActivityControllerInternal(launcher, z, state, j, i, consumer);
                }

                @Override // com.android.quickstep.ActivityControlHelper.AnimationFactory
                public void onTransitionCancelled() {
                    launcher.getStateManager().goToState(state, false);
                }
            };
        }

        private void createActivityControllerInternal(Launcher launcher, boolean z, LauncherState launcherState, long j, int i, Consumer<AnimatorPlaybackController> consumer) {
            LauncherState launcherState2 = i == 1 ? LauncherState.FAST_OVERVIEW : LauncherState.OVERVIEW;
            if (z) {
                DeviceProfile deviceProfile = launcher.getDeviceProfile();
                long jMax = 2 * Math.max(deviceProfile.widthPx, deviceProfile.heightPx);
                launcher.getStateManager().goToState(launcherState, false);
                consumer.accept(launcher.getStateManager().createAnimationToNewWorkspace(launcherState2, jMax));
                return;
            }
            if (launcher.getDeviceProfile().isVerticalBarLayout()) {
                return;
            }
            AllAppsTransitionController allAppsController = launcher.getAllAppsController();
            AnimatorSet animatorSet = new AnimatorSet();
            float fMax = j / Math.max(allAppsController.getShiftRange(), 1.0f);
            float verticalProgress = launcherState2.getVerticalProgress(launcher);
            ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(allAppsController, AllAppsTransitionController.ALL_APPS_PROGRESS, fMax + verticalProgress, verticalProgress);
            objectAnimatorOfFloat.setInterpolator(Interpolators.LINEAR);
            animatorSet.play(objectAnimatorOfFloat);
            long j2 = j * 2;
            animatorSet.setDuration(j2);
            launcher.getStateManager().setCurrentAnimation(animatorSet, new Animator[0]);
            consumer.accept(AnimatorPlaybackController.wrap(animatorSet, j2));
        }

        @Override // com.android.quickstep.ActivityControlHelper
        public ActivityInitListener createActivityInitListener(BiPredicate<Launcher, Boolean> biPredicate) {
            return new LauncherInitListener(biPredicate);
        }

        /* JADX DEBUG: Method merged with bridge method: getCreatedActivity()Lcom/android/launcher3/BaseDraggingActivity; */
        @Override // com.android.quickstep.ActivityControlHelper
        @Nullable
        public Launcher getCreatedActivity() {
            LauncherAppState instanceNoCreate = LauncherAppState.getInstanceNoCreate();
            if (instanceNoCreate == null) {
                return null;
            }
            return (Launcher) instanceNoCreate.getModel().getCallback();
        }

        @UiThread
        @Nullable
        private Launcher getVisibleLaucher() {
            Launcher createdActivity = getCreatedActivity();
            if (createdActivity != null && createdActivity.isStarted() && createdActivity.hasWindowFocus()) {
                return createdActivity;
            }
            return null;
        }

        @Override // com.android.quickstep.ActivityControlHelper
        @Nullable
        public RecentsView getVisibleRecentsView() {
            Launcher visibleLaucher = getVisibleLaucher();
            if (visibleLaucher == null || !visibleLaucher.getStateManager().getState().overviewUi) {
                return null;
            }
            return (RecentsView) visibleLaucher.getOverviewPanel();
        }

        @Override // com.android.quickstep.ActivityControlHelper
        public boolean switchToRecentsIfVisible(boolean z) {
            Launcher visibleLaucher = getVisibleLaucher();
            if (visibleLaucher != null) {
                if (z) {
                    visibleLaucher.getUserEventDispatcher().logActionCommand(6, getContainerType(), 12);
                }
                visibleLaucher.getStateManager().goToState(LauncherState.OVERVIEW);
                return true;
            }
            return false;
        }

        @Override // com.android.quickstep.ActivityControlHelper
        public boolean deferStartingActivity(int i) {
            return i == 1 || i == 4;
        }

        @Override // com.android.quickstep.ActivityControlHelper
        public Rect getOverviewWindowBounds(Rect rect, RemoteAnimationTargetCompat remoteAnimationTargetCompat) {
            return rect;
        }

        @Override // com.android.quickstep.ActivityControlHelper
        public boolean shouldMinimizeSplitScreen() {
            return true;
        }

        /* JADX DEBUG: Method merged with bridge method: supportsLongSwipe(Lcom/android/launcher3/BaseDraggingActivity;)Z */
        @Override // com.android.quickstep.ActivityControlHelper
        public boolean supportsLongSwipe(Launcher launcher) {
            return !launcher.getDeviceProfile().isVerticalBarLayout();
        }

        /* JADX DEBUG: Method merged with bridge method: getLongSwipeController(Lcom/android/launcher3/BaseDraggingActivity;Lcom/android/quickstep/util/RemoteAnimationTargetSet;)Lcom/android/quickstep/LongSwipeHelper; */
        @Override // com.android.quickstep.ActivityControlHelper
        public LongSwipeHelper getLongSwipeController(Launcher launcher, RemoteAnimationTargetSet remoteAnimationTargetSet) {
            if (launcher.getDeviceProfile().isVerticalBarLayout()) {
                return null;
            }
            return new LongSwipeHelper(launcher, remoteAnimationTargetSet);
        }

        /* JADX DEBUG: Method merged with bridge method: getAlphaProperty(Lcom/android/launcher3/BaseDraggingActivity;)Lcom/android/launcher3/util/MultiValueAlpha$AlphaProperty; */
        @Override // com.android.quickstep.ActivityControlHelper
        public MultiValueAlpha.AlphaProperty getAlphaProperty(Launcher launcher) {
            return launcher.getDragLayer().getAlphaProperty(3);
        }

        @Override // com.android.quickstep.ActivityControlHelper
        public int getContainerType() {
            Launcher visibleLaucher = getVisibleLaucher();
            if (visibleLaucher != null) {
                return visibleLaucher.getStateManager().getState().containerType;
            }
            return 13;
        }
    }

    public static class FallbackActivityControllerHelper implements ActivityControlHelper<RecentsActivity> {
        private final ComponentName mHomeComponent;
        private final Handler mUiHandler = new Handler(Looper.getMainLooper());

        @Override // com.android.quickstep.ActivityControlHelper
        public /* bridge */ /* synthetic */ AnimationFactory prepareRecentsUI(BaseDraggingActivity baseDraggingActivity, boolean z, Consumer consumer) {
            return prepareRecentsUI((RecentsActivity) baseDraggingActivity, z, (Consumer<AnimatorPlaybackController>) consumer);
        }

        public FallbackActivityControllerHelper(ComponentName componentName) {
            this.mHomeComponent = componentName;
        }

        /* JADX DEBUG: Method merged with bridge method: onQuickInteractionStart(Lcom/android/launcher3/BaseDraggingActivity;Landroid/app/ActivityManager$RunningTaskInfo;Z)V */
        @Override // com.android.quickstep.ActivityControlHelper
        public void onQuickInteractionStart(RecentsActivity recentsActivity, ActivityManager.RunningTaskInfo runningTaskInfo, boolean z) {
            final QuickScrubController quickScrubController = ((RecentsView) recentsActivity.getOverviewPanel()).getQuickScrubController();
            quickScrubController.onQuickScrubStart(!z && (runningTaskInfo == null || Objects.equals(runningTaskInfo.topActivity, this.mHomeComponent)), this);
            if (z) {
                Handler handler = this.mUiHandler;
                Objects.requireNonNull(quickScrubController);
                handler.postDelayed(new Runnable() { // from class: com.android.quickstep.-$$Lambda$td222kNA73L1CFdKbwzs-qgIBcg
                    @Override // java.lang.Runnable
                    public final void run() {
                        quickScrubController.onFinishedTransitionToQuickScrub();
                    }
                }, 250L);
            }
        }

        @Override // com.android.quickstep.ActivityControlHelper
        public float getTranslationYForQuickScrub(TransformedRect transformedRect, DeviceProfile deviceProfile, Context context) {
            return 0.0f;
        }

        /* JADX DEBUG: Method merged with bridge method: executeOnWindowAvailable(Lcom/android/launcher3/BaseDraggingActivity;Ljava/lang/Runnable;)V */
        @Override // com.android.quickstep.ActivityControlHelper
        public void executeOnWindowAvailable(RecentsActivity recentsActivity, Runnable runnable) {
            runnable.run();
        }

        /* JADX DEBUG: Method merged with bridge method: onTransitionCancelled(Lcom/android/launcher3/BaseDraggingActivity;Z)V */
        @Override // com.android.quickstep.ActivityControlHelper
        public void onTransitionCancelled(RecentsActivity recentsActivity, boolean z) {
        }

        @Override // com.android.quickstep.ActivityControlHelper
        public int getSwipeUpDestinationAndLength(DeviceProfile deviceProfile, Context context, int i, TransformedRect transformedRect) throws Resources.NotFoundException {
            LayoutUtils.calculateFallbackTaskSize(context, deviceProfile, transformedRect.rect);
            if (deviceProfile.isVerticalBarLayout()) {
                Rect insets = deviceProfile.getInsets();
                return deviceProfile.hotseatBarSizePx + deviceProfile.hotseatBarSidePaddingPx + (deviceProfile.isSeascape() ? insets.left : insets.right);
            }
            return deviceProfile.heightPx - transformedRect.rect.bottom;
        }

        /* JADX DEBUG: Method merged with bridge method: onSwipeUpComplete(Lcom/android/launcher3/BaseDraggingActivity;)V */
        @Override // com.android.quickstep.ActivityControlHelper
        public void onSwipeUpComplete(RecentsActivity recentsActivity) {
        }

        public AnimationFactory prepareRecentsUI(final RecentsActivity recentsActivity, boolean z, final Consumer<AnimatorPlaybackController> consumer) {
            if (z) {
                return new AnimationFactory() { // from class: com.android.quickstep.-$$Lambda$ActivityControlHelper$FallbackActivityControllerHelper$na8EFVH2JvC3dmTt0x9CezgYGmc
                    @Override // com.android.quickstep.ActivityControlHelper.AnimationFactory
                    public final void createActivityController(long j, int i) {
                        ActivityControlHelper.FallbackActivityControllerHelper.lambda$prepareRecentsUI$0(j, i);
                    }
                };
            }
            final RecentsViewContainer overviewPanelContainer = recentsActivity.getOverviewPanelContainer();
            overviewPanelContainer.setContentAlpha(0.0f);
            return new AnimationFactory() { // from class: com.android.quickstep.ActivityControlHelper.FallbackActivityControllerHelper.1
                boolean isAnimatingHome = false;

                @Override // com.android.quickstep.ActivityControlHelper.AnimationFactory
                public void onRemoteAnimationReceived(RemoteAnimationTargetSet remoteAnimationTargetSet) {
                    this.isAnimatingHome = remoteAnimationTargetSet != null && remoteAnimationTargetSet.isAnimatingHome();
                    if (!this.isAnimatingHome) {
                        overviewPanelContainer.setContentAlpha(1.0f);
                    }
                    createActivityController(FallbackActivityControllerHelper.this.getSwipeUpDestinationAndLength(recentsActivity.getDeviceProfile(), recentsActivity, 0, new TransformedRect()), 0);
                }

                @Override // com.android.quickstep.ActivityControlHelper.AnimationFactory
                public void createActivityController(long j, int i) {
                    if (!this.isAnimatingHome) {
                        return;
                    }
                    ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(overviewPanelContainer, RecentsViewContainer.CONTENT_ALPHA, 0.0f, 1.0f);
                    objectAnimatorOfFloat.setDuration(j).setInterpolator(Interpolators.LINEAR);
                    AnimatorSet animatorSet = new AnimatorSet();
                    animatorSet.play(objectAnimatorOfFloat);
                    consumer.accept(AnimatorPlaybackController.wrap(animatorSet, j));
                }
            };
        }

        static /* synthetic */ void lambda$prepareRecentsUI$0(long j, int i) {
        }

        /* JADX DEBUG: Method merged with bridge method: createLayoutListener(Lcom/android/launcher3/BaseDraggingActivity;)Lcom/android/quickstep/ActivityControlHelper$LayoutListener; */
        @Override // com.android.quickstep.ActivityControlHelper
        public LayoutListener createLayoutListener(RecentsActivity recentsActivity) {
            return new LayoutListener() { // from class: com.android.quickstep.ActivityControlHelper.FallbackActivityControllerHelper.2
                @Override // com.android.quickstep.ActivityControlHelper.LayoutListener
                public void open() {
                }

                @Override // com.android.quickstep.ActivityControlHelper.LayoutListener
                public void setHandler(WindowTransformSwipeHandler windowTransformSwipeHandler) {
                }

                @Override // com.android.quickstep.ActivityControlHelper.LayoutListener
                public void finish() {
                }
            };
        }

        @Override // com.android.quickstep.ActivityControlHelper
        public ActivityInitListener createActivityInitListener(BiPredicate<RecentsActivity, Boolean> biPredicate) {
            return new RecentsActivityTracker(biPredicate);
        }

        /* JADX DEBUG: Method merged with bridge method: getCreatedActivity()Lcom/android/launcher3/BaseDraggingActivity; */
        @Override // com.android.quickstep.ActivityControlHelper
        @Nullable
        public RecentsActivity getCreatedActivity() {
            return RecentsActivityTracker.getCurrentActivity();
        }

        @Override // com.android.quickstep.ActivityControlHelper
        @Nullable
        public RecentsView getVisibleRecentsView() {
            RecentsActivity createdActivity = getCreatedActivity();
            if (createdActivity != null && createdActivity.hasWindowFocus()) {
                return (RecentsView) createdActivity.getOverviewPanel();
            }
            return null;
        }

        @Override // com.android.quickstep.ActivityControlHelper
        public boolean switchToRecentsIfVisible(boolean z) {
            return false;
        }

        @Override // com.android.quickstep.ActivityControlHelper
        public boolean deferStartingActivity(int i) {
            return true;
        }

        @Override // com.android.quickstep.ActivityControlHelper
        public Rect getOverviewWindowBounds(Rect rect, RemoteAnimationTargetCompat remoteAnimationTargetCompat) {
            return remoteAnimationTargetCompat.sourceContainerBounds;
        }

        @Override // com.android.quickstep.ActivityControlHelper
        public boolean shouldMinimizeSplitScreen() {
            return false;
        }

        /* JADX DEBUG: Method merged with bridge method: supportsLongSwipe(Lcom/android/launcher3/BaseDraggingActivity;)Z */
        @Override // com.android.quickstep.ActivityControlHelper
        public boolean supportsLongSwipe(RecentsActivity recentsActivity) {
            return false;
        }

        /* JADX DEBUG: Method merged with bridge method: getLongSwipeController(Lcom/android/launcher3/BaseDraggingActivity;Lcom/android/quickstep/util/RemoteAnimationTargetSet;)Lcom/android/quickstep/LongSwipeHelper; */
        @Override // com.android.quickstep.ActivityControlHelper
        public LongSwipeHelper getLongSwipeController(RecentsActivity recentsActivity, RemoteAnimationTargetSet remoteAnimationTargetSet) {
            return null;
        }

        /* JADX DEBUG: Method merged with bridge method: getAlphaProperty(Lcom/android/launcher3/BaseDraggingActivity;)Lcom/android/launcher3/util/MultiValueAlpha$AlphaProperty; */
        @Override // com.android.quickstep.ActivityControlHelper
        public MultiValueAlpha.AlphaProperty getAlphaProperty(RecentsActivity recentsActivity) {
            return recentsActivity.getDragLayer().getAlphaProperty(0);
        }

        @Override // com.android.quickstep.ActivityControlHelper
        public int getContainerType() {
            return 15;
        }
    }

    public interface AnimationFactory {
        void createActivityController(long j, int i);

        default void onRemoteAnimationReceived(RemoteAnimationTargetSet remoteAnimationTargetSet) {
        }

        default void onTransitionCancelled() {
        }
    }
}
