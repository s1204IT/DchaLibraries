package com.android.quickstep;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.ViewConfiguration;
import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.BaseDraggingActivity;
import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.MainThreadExecutor;
import com.android.launcher3.anim.AnimationSuccessListener;
import com.android.launcher3.anim.AnimatorPlaybackController;
import com.android.launcher3.anim.Interpolators;
import com.android.launcher3.logging.UserEventDispatcher;
import com.android.quickstep.ActivityControlHelper;
import com.android.quickstep.OverviewCommandHelper;
import com.android.quickstep.util.ClipAnimationHelper;
import com.android.quickstep.util.RemoteAnimationProvider;
import com.android.quickstep.util.RemoteAnimationTargetSet;
import com.android.quickstep.util.TransformedRect;
import com.android.quickstep.views.RecentsView;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.shared.system.LatencyTrackerCompat;
import com.android.systemui.shared.system.PackageManagerWrapper;
import com.android.systemui.shared.system.RemoteAnimationTargetCompat;
import com.android.systemui.shared.system.TransactionCompat;
import java.util.ArrayList;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

@TargetApi(28)
public class OverviewCommandHelper {
    private static final long RECENTS_LAUNCH_DURATION = 250;
    private static final String TAG = "OverviewCommandHelper";
    private ActivityControlHelper mActivityControlHelper;
    private final Context mContext;
    private long mLastToggleTime;
    private final ComponentName mMyHomeComponent;
    private final RecentsModel mRecentsModel;
    private String mUpdateRegisteredPackage;
    public ComponentName overviewComponent;
    public Intent overviewIntent;
    private final BroadcastReceiver mUserPreferenceChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            OverviewCommandHelper.this.initOverviewTargets();
        }
    };
    private final BroadcastReceiver mOtherHomeAppUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            OverviewCommandHelper.this.initOverviewTargets();
        }
    };
    private final ActivityManagerWrapper mAM = ActivityManagerWrapper.getInstance();
    private final MainThreadExecutor mMainThreadExecutor = new MainThreadExecutor();

    public OverviewCommandHelper(Context context) {
        this.mContext = context;
        this.mRecentsModel = RecentsModel.getInstance(this.mContext);
        this.mMyHomeComponent = new ComponentName(context.getPackageName(), context.getPackageManager().resolveActivity(new Intent("android.intent.action.MAIN").addCategory("android.intent.category.HOME").setPackage(this.mContext.getPackageName()), 0).activityInfo.name);
        this.mContext.registerReceiver(this.mUserPreferenceChangeReceiver, new IntentFilter(PackageManagerWrapper.ACTION_PREFERRED_ACTIVITY_CHANGED));
        initOverviewTargets();
    }

    public void initOverviewTargets() {
        String str;
        ComponentName homeActivities = PackageManagerWrapper.getInstance().getHomeActivities(new ArrayList());
        if (homeActivities == null || this.mMyHomeComponent.equals(homeActivities)) {
            this.overviewComponent = this.mMyHomeComponent;
            this.mActivityControlHelper = new ActivityControlHelper.LauncherActivityControllerHelper();
            str = "android.intent.category.HOME";
            if (this.mUpdateRegisteredPackage != null) {
                this.mContext.unregisterReceiver(this.mOtherHomeAppUpdateReceiver);
                this.mUpdateRegisteredPackage = null;
            }
        } else {
            this.overviewComponent = new ComponentName(this.mContext, (Class<?>) RecentsActivity.class);
            this.mActivityControlHelper = new ActivityControlHelper.FallbackActivityControllerHelper(homeActivities);
            str = "android.intent.category.DEFAULT";
            if (!homeActivities.getPackageName().equals(this.mUpdateRegisteredPackage)) {
                if (this.mUpdateRegisteredPackage != null) {
                    this.mContext.unregisterReceiver(this.mOtherHomeAppUpdateReceiver);
                }
                this.mUpdateRegisteredPackage = homeActivities.getPackageName();
                IntentFilter intentFilter = new IntentFilter("android.intent.action.PACKAGE_ADDED");
                intentFilter.addAction("android.intent.action.PACKAGE_CHANGED");
                intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
                intentFilter.addDataScheme("package");
                intentFilter.addDataSchemeSpecificPart(this.mUpdateRegisteredPackage, 0);
                this.mContext.registerReceiver(this.mOtherHomeAppUpdateReceiver, intentFilter);
            }
        }
        this.overviewIntent = new Intent("android.intent.action.MAIN").addCategory(str).setComponent(this.overviewComponent).setFlags(268435456);
    }

    public void onDestroy() {
        this.mContext.unregisterReceiver(this.mUserPreferenceChangeReceiver);
        if (this.mUpdateRegisteredPackage != null) {
            this.mContext.unregisterReceiver(this.mOtherHomeAppUpdateReceiver);
            this.mUpdateRegisteredPackage = null;
        }
    }

    public void onOverviewToggle() {
        if (this.mAM.isScreenPinningActive()) {
            return;
        }
        this.mAM.closeSystemWindows(ActivityManagerWrapper.CLOSE_SYSTEM_WINDOWS_REASON_RECENTS);
        this.mMainThreadExecutor.execute(new RecentsActivityCommand());
    }

    public void onOverviewShown() {
        this.mMainThreadExecutor.execute(new ShowRecentsCommand());
    }

    public void onTip(final int i, final int i2) {
        this.mMainThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                UserEventDispatcher.newInstance(OverviewCommandHelper.this.mContext, new InvariantDeviceProfile(OverviewCommandHelper.this.mContext).getDeviceProfile(OverviewCommandHelper.this.mContext)).logActionTip(i, i2);
            }
        });
    }

    public ActivityControlHelper getActivityControlHelper() {
        return this.mActivityControlHelper;
    }

    private class ShowRecentsCommand extends RecentsActivityCommand {
        private ShowRecentsCommand() {
            super();
        }

        @Override
        protected boolean handleCommand(long j) {
            return this.mHelper.getVisibleRecentsView() != null;
        }
    }

    class RecentsActivityCommand<T extends BaseDraggingActivity> implements Runnable {
        private T mActivity;
        protected final ActivityControlHelper<T> mHelper;
        private ActivityControlHelper.ActivityInitListener mListener;
        private RecentsView mRecentsView;
        private final int mRunningTaskId;
        private boolean mUserEventLogged;
        private final long mToggleClickedTime = SystemClock.uptimeMillis();
        private final long mCreateTime = SystemClock.elapsedRealtime();

        public RecentsActivityCommand() {
            this.mHelper = OverviewCommandHelper.this.getActivityControlHelper();
            this.mRunningTaskId = OverviewCommandHelper.this.mAM.getRunningTask().id;
            OverviewCommandHelper.this.mRecentsModel.loadTasks(this.mRunningTaskId, null);
        }

        @Override
        public void run() {
            long j = this.mCreateTime - OverviewCommandHelper.this.mLastToggleTime;
            OverviewCommandHelper.this.mLastToggleTime = this.mCreateTime;
            if (!handleCommand(j) && !this.mHelper.switchToRecentsIfVisible(true)) {
                this.mListener = this.mHelper.createActivityInitListener(new BiPredicate() {
                    @Override
                    public final boolean test(Object obj, Object obj2) {
                        return this.f$0.onActivityReady((BaseDraggingActivity) obj, (Boolean) obj2);
                    }
                });
                this.mListener.registerAndStartActivity(OverviewCommandHelper.this.overviewIntent, new RemoteAnimationProvider() {
                    @Override
                    public final AnimatorSet createWindowAnimation(RemoteAnimationTargetCompat[] remoteAnimationTargetCompatArr) {
                        return this.f$0.createWindowAnimation(remoteAnimationTargetCompatArr);
                    }
                }, OverviewCommandHelper.this.mContext, OverviewCommandHelper.this.mMainThreadExecutor.getHandler(), OverviewCommandHelper.RECENTS_LAUNCH_DURATION);
            }
        }

        protected boolean handleCommand(long j) {
            RecentsView visibleRecentsView = this.mHelper.getVisibleRecentsView();
            if (visibleRecentsView != null) {
                visibleRecentsView.showNextTask();
                return true;
            }
            if (j < ViewConfiguration.getDoubleTapTimeout()) {
                return true;
            }
            return false;
        }

        public boolean onActivityReady(T t, Boolean bool) {
            ((RecentsView) t.getOverviewPanel()).setCurrentTask(this.mRunningTaskId);
            AbstractFloatingView.closeAllOpenViews(t, bool.booleanValue());
            ActivityControlHelper.AnimationFactory animationFactoryPrepareRecentsUI = this.mHelper.prepareRecentsUI(t, bool.booleanValue(), new Consumer() {
                @Override
                public final void accept(Object obj) {
                    OverviewCommandHelper.RecentsActivityCommand.lambda$onActivityReady$0((AnimatorPlaybackController) obj);
                }
            });
            animationFactoryPrepareRecentsUI.onRemoteAnimationReceived(null);
            if (bool.booleanValue()) {
                animationFactoryPrepareRecentsUI.createActivityController(OverviewCommandHelper.RECENTS_LAUNCH_DURATION, 0);
            }
            this.mActivity = t;
            this.mRecentsView = (RecentsView) this.mActivity.getOverviewPanel();
            this.mRecentsView.setRunningTaskIconScaledDown(true, false);
            if (!this.mUserEventLogged) {
                t.getUserEventDispatcher().logActionCommand(6, this.mHelper.getContainerType(), 12);
                this.mUserEventLogged = true;
            }
            return false;
        }

        static void lambda$onActivityReady$0(AnimatorPlaybackController animatorPlaybackController) {
            animatorPlaybackController.dispatchOnStart();
            ValueAnimator duration = animatorPlaybackController.getAnimationPlayer().setDuration(OverviewCommandHelper.RECENTS_LAUNCH_DURATION);
            duration.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
            duration.start();
        }

        public AnimatorSet createWindowAnimation(RemoteAnimationTargetCompat[] remoteAnimationTargetCompatArr) {
            if (LatencyTrackerCompat.isEnabled(OverviewCommandHelper.this.mContext)) {
                LatencyTrackerCompat.logToggleRecents((int) (SystemClock.uptimeMillis() - this.mToggleClickedTime));
            }
            if (this.mListener != null) {
                this.mListener.unregister();
            }
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.addListener(new AnimationSuccessListener() {
                @Override
                public void onAnimationSuccess(Animator animator) {
                    if (RecentsActivityCommand.this.mRecentsView != null) {
                        RecentsActivityCommand.this.mRecentsView.setRunningTaskIconScaledDown(false, true);
                    }
                }
            });
            if (this.mActivity == null) {
                Log.e(OverviewCommandHelper.TAG, "Animation created, before activity");
                animatorSet.play(ValueAnimator.ofInt(0, 1).setDuration(100L));
                return animatorSet;
            }
            final RemoteAnimationTargetSet remoteAnimationTargetSet = new RemoteAnimationTargetSet(remoteAnimationTargetCompatArr, 1);
            RemoteAnimationTargetCompat remoteAnimationTargetCompatFindTask = remoteAnimationTargetSet.findTask(this.mRunningTaskId);
            if (remoteAnimationTargetCompatFindTask == null) {
                Log.e(OverviewCommandHelper.TAG, "No closing app");
                animatorSet.play(ValueAnimator.ofInt(0, 1).setDuration(100L));
                return animatorSet;
            }
            final ClipAnimationHelper clipAnimationHelper = new ClipAnimationHelper();
            int[] iArr = new int[2];
            View rootView = this.mActivity.getRootView();
            rootView.getLocationOnScreen(iArr);
            clipAnimationHelper.updateSource(new Rect(iArr[0], iArr[1], iArr[0] + rootView.getWidth(), iArr[1] + rootView.getHeight()), remoteAnimationTargetCompatFindTask);
            TransformedRect transformedRect = new TransformedRect();
            this.mHelper.getSwipeUpDestinationAndLength(this.mActivity.getDeviceProfile(), this.mActivity, 0, transformedRect);
            clipAnimationHelper.updateTargetRect(transformedRect);
            clipAnimationHelper.prepareAnimation(false);
            ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
            valueAnimatorOfFloat.setDuration(OverviewCommandHelper.RECENTS_LAUNCH_DURATION);
            valueAnimatorOfFloat.setInterpolator(Interpolators.TOUCH_RESPONSE_INTERPOLATOR);
            valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                    clipAnimationHelper.applyTransform(remoteAnimationTargetSet, ((Float) valueAnimator.getAnimatedValue()).floatValue());
                }
            });
            if (remoteAnimationTargetSet.isAnimatingHome()) {
                final RemoteAnimationTargetSet remoteAnimationTargetSet2 = new RemoteAnimationTargetSet(remoteAnimationTargetCompatArr, 0);
                final TransactionCompat transactionCompat = new TransactionCompat();
                valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                        OverviewCommandHelper.RecentsActivityCommand.lambda$createWindowAnimation$2(remoteAnimationTargetSet2, transactionCompat, valueAnimator);
                    }
                });
            }
            animatorSet.play(valueAnimatorOfFloat);
            return animatorSet;
        }

        static void lambda$createWindowAnimation$2(RemoteAnimationTargetSet remoteAnimationTargetSet, TransactionCompat transactionCompat, ValueAnimator valueAnimator) {
            for (RemoteAnimationTargetCompat remoteAnimationTargetCompat : remoteAnimationTargetSet.apps) {
                transactionCompat.setAlpha(remoteAnimationTargetCompat.leash, ((Float) valueAnimator.getAnimatedValue()).floatValue());
            }
            transactionCompat.apply();
        }
    }
}
