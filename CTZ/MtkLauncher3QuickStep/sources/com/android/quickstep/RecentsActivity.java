package com.android.quickstep;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.app.ActivityOptions;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.BaseDraggingActivity;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.LauncherAnimationRunner;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.R;
import com.android.launcher3.anim.Interpolators;
import com.android.launcher3.badge.BadgeInfo;
import com.android.launcher3.uioverrides.UiFactory;
import com.android.launcher3.util.Themes;
import com.android.launcher3.views.BaseDragLayer;
import com.android.quickstep.fallback.FallbackRecentsView;
import com.android.quickstep.fallback.RecentsRootView;
import com.android.quickstep.util.ClipAnimationHelper;
import com.android.quickstep.views.RecentsViewContainer;
import com.android.quickstep.views.TaskView;
import com.android.systemui.shared.system.ActivityOptionsCompat;
import com.android.systemui.shared.system.RemoteAnimationAdapterCompat;
import com.android.systemui.shared.system.RemoteAnimationTargetCompat;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class RecentsActivity extends BaseDraggingActivity {
    private FallbackRecentsView mFallbackRecentsView;
    private Configuration mOldConfig;
    private RecentsViewContainer mOverviewPanelContainer;
    private RecentsRootView mRecentsRootView;
    private Handler mUiHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mOldConfig = new Configuration(getResources().getConfiguration());
        initDeviceProfile();
        setContentView(R.layout.fallback_recents_activity);
        this.mRecentsRootView = (RecentsRootView) findViewById(R.id.drag_layer);
        this.mFallbackRecentsView = (FallbackRecentsView) findViewById(R.id.overview_panel);
        this.mOverviewPanelContainer = (RecentsViewContainer) findViewById(R.id.overview_panel_container);
        this.mRecentsRootView.setup();
        getSystemUiController().updateUiState(0, Themes.getAttrBoolean(this, R.attr.isWorkspaceDarkText));
        RecentsActivityTracker.onRecentsActivityCreate(this);
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        if ((configuration.diff(this.mOldConfig) & 1152) != 0) {
            onHandleConfigChanged();
        }
        this.mOldConfig.setTo(configuration);
        super.onConfigurationChanged(configuration);
    }

    @Override
    public void onMultiWindowModeChanged(boolean z, Configuration configuration) {
        onHandleConfigChanged();
        super.onMultiWindowModeChanged(z, configuration);
    }

    public void onRootViewSizeChanged() {
        if (isInMultiWindowModeCompat()) {
            onHandleConfigChanged();
        }
    }

    private void onHandleConfigChanged() {
        this.mUserEventDispatcher = null;
        initDeviceProfile();
        AbstractFloatingView.closeOpenViews(this, true, 399);
        dispatchDeviceProfileChanged();
        this.mRecentsRootView.setup();
        reapplyUi();
    }

    @Override
    protected void reapplyUi() {
        this.mRecentsRootView.dispatchInsets();
    }

    private void initDeviceProfile() {
        DeviceProfile deviceProfileCopy;
        LauncherAppState instanceNoCreate = LauncherAppState.getInstanceNoCreate();
        if (isInMultiWindowModeCompat()) {
            DeviceProfile deviceProfile = (instanceNoCreate == null ? new InvariantDeviceProfile(this) : instanceNoCreate.getInvariantDeviceProfile()).getDeviceProfile(this);
            this.mDeviceProfile = this.mRecentsRootView == null ? deviceProfile.copy(this) : deviceProfile.getMultiWindowProfile(this, this.mRecentsRootView.getLastKnownSize());
        } else {
            if (instanceNoCreate == null) {
                deviceProfileCopy = new InvariantDeviceProfile(this).getDeviceProfile(this);
            } else {
                deviceProfileCopy = instanceNoCreate.getInvariantDeviceProfile().getDeviceProfile(this).copy(this);
            }
            this.mDeviceProfile = deviceProfileCopy;
        }
        onDeviceProfileInitiated();
    }

    @Override
    public BaseDragLayer getDragLayer() {
        return this.mRecentsRootView;
    }

    @Override
    public View getRootView() {
        return this.mRecentsRootView;
    }

    @Override
    public <T extends View> T getOverviewPanel() {
        return this.mFallbackRecentsView;
    }

    public RecentsViewContainer getOverviewPanelContainer() {
        return this.mOverviewPanelContainer;
    }

    @Override
    public BadgeInfo getBadgeInfoForItem(ItemInfo itemInfo) {
        return null;
    }

    @Override
    public ActivityOptions getActivityLaunchOptions(View view) {
        if (!(view instanceof TaskView)) {
            return null;
        }
        final TaskView taskView = (TaskView) view;
        return ActivityOptionsCompat.makeRemoteAnimation(new RemoteAnimationAdapterCompat(new LauncherAnimationRunner(this.mUiHandler, true) {
            @Override
            public void onCreateAnimation(RemoteAnimationTargetCompat[] remoteAnimationTargetCompatArr, LauncherAnimationRunner.AnimationResult animationResult) {
                animationResult.setAnimation(RecentsActivity.this.composeRecentsLaunchAnimator(taskView, remoteAnimationTargetCompatArr));
            }
        }, 336L, 216L));
    }

    public AnimatorSet composeRecentsLaunchAnimator(TaskView taskView, RemoteAnimationTargetCompat[] remoteAnimationTargetCompatArr) {
        AnimatorSet animatorSet = new AnimatorSet();
        boolean zTaskIsATargetWithMode = TaskUtils.taskIsATargetWithMode(remoteAnimationTargetCompatArr, getTaskId(), 1);
        ClipAnimationHelper clipAnimationHelper = new ClipAnimationHelper();
        animatorSet.play(TaskUtils.getRecentsWindowAnimator(taskView, !zTaskIsATargetWithMode, remoteAnimationTargetCompatArr, clipAnimationHelper).setDuration(336L));
        if (zTaskIsATargetWithMode) {
            AnimatorSet animatorSetCreateAdjacentPageAnimForTaskLaunch = this.mFallbackRecentsView.createAdjacentPageAnimForTaskLaunch(taskView, clipAnimationHelper);
            animatorSetCreateAdjacentPageAnimForTaskLaunch.setInterpolator(Interpolators.TOUCH_RESPONSE_INTERPOLATOR);
            animatorSetCreateAdjacentPageAnimForTaskLaunch.setDuration(336L);
            animatorSetCreateAdjacentPageAnimForTaskLaunch.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    RecentsActivity.this.mFallbackRecentsView.resetTaskVisuals();
                }
            });
            animatorSet.play(animatorSetCreateAdjacentPageAnimForTaskLaunch);
        }
        return animatorSet;
    }

    @Override
    public void invalidateParent(ItemInfo itemInfo) {
    }

    @Override
    protected void onStart() {
        this.mFallbackRecentsView.setContentAlpha(1.0f);
        super.onStart();
        UiFactory.onStart(this);
        this.mFallbackRecentsView.resetTaskVisuals();
    }

    @Override
    protected void onStop() {
        super.onStop();
        onTrimMemory(20);
    }

    @Override
    public void onTrimMemory(int i) {
        super.onTrimMemory(i);
        UiFactory.onTrimMemory(this, i);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        RecentsActivityTracker.onRecentsActivityNewIntent(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RecentsActivityTracker.onRecentsActivityDestroy(this);
    }

    @Override
    public void onBackPressed() {
        startHome();
    }

    public void startHome() {
        startActivity(new Intent("android.intent.action.MAIN").addCategory("android.intent.category.HOME").setFlags(268435456));
    }

    @Override
    public void dump(String str, FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        super.dump(str, fileDescriptor, printWriter, strArr);
        printWriter.println(str + "Misc:");
        dumpMisc(printWriter);
    }
}
