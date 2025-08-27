package com.android.quickstep;

import android.util.Log;
import android.view.animation.Interpolator;
import com.android.launcher3.Alarm;
import com.android.launcher3.BaseActivity;
import com.android.launcher3.OnAlarmListener;
import com.android.launcher3.Utilities;
import com.android.launcher3.anim.Interpolators;
import com.android.quickstep.views.RecentsView;
import com.android.quickstep.views.TaskView;
import java.util.function.Consumer;

/* loaded from: classes.dex */
public class QuickScrubController implements OnAlarmListener {
    private static final long AUTO_ADVANCE_DELAY = 500;
    private static final boolean ENABLE_AUTO_ADVANCE = true;
    private static final int QUICKSCRUB_END_SNAP_DURATION_PER_PAGE = 60;
    private static final int QUICKSCRUB_SNAP_DURATION_PER_PAGE = 325;
    public static final int QUICK_SCRUB_FROM_APP_START_DURATION = 240;
    public static final int QUICK_SCRUB_FROM_HOME_START_DURATION = 200;
    public static final Interpolator QUICK_SCRUB_START_INTERPOLATOR = Interpolators.FAST_OUT_SLOW_IN;
    private static final float[] QUICK_SCRUB_THRESHOLDS = {0.05f, 0.2f, 0.35f, 0.5f, 0.65f, 0.8f, 0.95f};
    public static final float QUICK_SCRUB_TRANSLATION_Y_FACTOR = 0.8333333f;
    private static final String TAG = "QuickScrubController";
    private final BaseActivity mActivity;
    private ActivityControlHelper mActivityControlHelper;
    private final Alarm mAutoAdvanceAlarm = new Alarm();
    private boolean mFinishedTransitionToQuickScrub;
    private boolean mInQuickScrub;
    private Runnable mOnFinishedTransitionToQuickScrubRunnable;
    private int mQuickScrubSection;
    private final RecentsView mRecentsView;
    private boolean mStartedFromHome;
    private boolean mWaitingForTaskLaunch;

    public QuickScrubController(BaseActivity baseActivity, RecentsView recentsView) {
        this.mActivity = baseActivity;
        this.mRecentsView = recentsView;
        this.mAutoAdvanceAlarm.setOnAlarmListener(this);
    }

    public void onQuickScrubStart(boolean z, ActivityControlHelper activityControlHelper) {
        prepareQuickScrub(TAG);
        this.mInQuickScrub = true;
        this.mStartedFromHome = z;
        this.mQuickScrubSection = 0;
        this.mFinishedTransitionToQuickScrub = false;
        this.mActivityControlHelper = activityControlHelper;
        snapToNextTaskIfAvailable();
        this.mActivity.getUserEventDispatcher().resetActionDurationMillis();
    }

    public void onQuickScrubEnd() {
        this.mInQuickScrub = false;
        this.mAutoAdvanceAlarm.cancelAlarm();
        final int nextPage = this.mRecentsView.getNextPage();
        Runnable runnable = new Runnable() { // from class: com.android.quickstep.-$$Lambda$QuickScrubController$iIcrBVScs5EFuJBcCXy6xjHbFJ4
            @Override // java.lang.Runnable
            public final void run() {
                QuickScrubController.lambda$onQuickScrubEnd$1(this.f$0, nextPage);
            }
        };
        int iAbs = Math.abs(nextPage - this.mRecentsView.getPageNearestToCenterOfScreen()) * QUICKSCRUB_END_SNAP_DURATION_PER_PAGE;
        if (this.mRecentsView.getChildCount() > 0 && this.mRecentsView.snapToPage(nextPage, iAbs)) {
            this.mRecentsView.setNextPageSwitchRunnable(runnable);
        } else if (this.mFinishedTransitionToQuickScrub) {
            runnable.run();
        } else {
            this.mOnFinishedTransitionToQuickScrubRunnable = runnable;
        }
    }

    public static /* synthetic */ void lambda$onQuickScrubEnd$1(final QuickScrubController quickScrubController, final int i) {
        final TaskView pageAt = quickScrubController.mRecentsView.getPageAt(i);
        if (pageAt != null) {
            quickScrubController.mWaitingForTaskLaunch = true;
            pageAt.launchTask(true, new Consumer() { // from class: com.android.quickstep.-$$Lambda$QuickScrubController$hSsri-NuYy5GIuelGzr3-0JPFWU
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    QuickScrubController.lambda$onQuickScrubEnd$0(this.f$0, pageAt, i, (Boolean) obj);
                }
            }, pageAt.getHandler());
        } else {
            quickScrubController.breakOutOfQuickScrub();
        }
        quickScrubController.mActivityControlHelper = null;
    }

    public static /* synthetic */ void lambda$onQuickScrubEnd$0(QuickScrubController quickScrubController, TaskView taskView, int i, Boolean bool) {
        if (!bool.booleanValue()) {
            taskView.notifyTaskLaunchFailed(TAG);
            quickScrubController.breakOutOfQuickScrub();
        } else {
            quickScrubController.mActivity.getUserEventDispatcher().logTaskLaunchOrDismiss(2, 0, i, TaskUtils.getComponentKeyForTask(taskView.getTask().key));
        }
        quickScrubController.mWaitingForTaskLaunch = false;
    }

    public void cancelActiveQuickscrub() {
        if (!this.mInQuickScrub) {
            return;
        }
        Log.d(TAG, "Quickscrub was active, cancelling");
        this.mInQuickScrub = false;
        this.mActivityControlHelper = null;
        this.mOnFinishedTransitionToQuickScrubRunnable = null;
        this.mRecentsView.setNextPageSwitchRunnable(null);
    }

    public boolean prepareQuickScrub(String str) {
        if (this.mWaitingForTaskLaunch || this.mInQuickScrub) {
            Log.d(str, "Waiting for last scrub to finish, will skip this interaction");
            return false;
        }
        this.mOnFinishedTransitionToQuickScrubRunnable = null;
        this.mRecentsView.setNextPageSwitchRunnable(null);
        return true;
    }

    public boolean isWaitingForTaskLaunch() {
        return this.mWaitingForTaskLaunch;
    }

    private void breakOutOfQuickScrub() {
        if (this.mRecentsView.getChildCount() == 0 || this.mActivityControlHelper == null || !this.mActivityControlHelper.switchToRecentsIfVisible(false)) {
            this.mActivity.onBackPressed();
        }
    }

    public void onQuickScrubProgress(float f) {
        float[] fArr = QUICK_SCRUB_THRESHOLDS;
        int length = fArr.length;
        int i = 0;
        for (int i2 = 0; i2 < length && f >= fArr[i2]; i2++) {
            i++;
        }
        if (i != this.mQuickScrubSection) {
            boolean z = this.mQuickScrubSection == QUICK_SCRUB_THRESHOLDS.length || this.mQuickScrubSection == 0;
            int nextPage = (this.mRecentsView.getNextPage() + i) - this.mQuickScrubSection;
            if (this.mFinishedTransitionToQuickScrub && !z) {
                goToPageWithHaptic(nextPage);
            }
            if (i == QUICK_SCRUB_THRESHOLDS.length || i == 0) {
                this.mAutoAdvanceAlarm.setAlarm(AUTO_ADVANCE_DELAY);
            } else {
                this.mAutoAdvanceAlarm.cancelAlarm();
            }
            this.mQuickScrubSection = i;
        }
    }

    public void onFinishedTransitionToQuickScrub() {
        this.mFinishedTransitionToQuickScrub = true;
        Runnable runnable = this.mOnFinishedTransitionToQuickScrubRunnable;
        this.mOnFinishedTransitionToQuickScrubRunnable = null;
        if (runnable != null) {
            runnable.run();
        }
    }

    public void snapToNextTaskIfAvailable() {
        if (this.mInQuickScrub && this.mRecentsView.getChildCount() > 0) {
            goToPageWithHaptic(this.mStartedFromHome ? 0 : this.mRecentsView.getNextPage() + 1, this.mStartedFromHome ? 200 : QUICK_SCRUB_FROM_APP_START_DURATION, true, QUICK_SCRUB_START_INTERPOLATOR);
        }
    }

    private void goToPageWithHaptic(int i) {
        goToPageWithHaptic(i, -1, false, null);
    }

    private void goToPageWithHaptic(int i, int i2, boolean z, Interpolator interpolator) {
        int iBoundToRange = Utilities.boundToRange(i, 0, this.mRecentsView.getPageCount() - 1);
        boolean z2 = iBoundToRange != this.mRecentsView.getNextPage();
        if (z2) {
            if (i2 <= -1) {
                i2 = Math.abs(iBoundToRange - this.mRecentsView.getNextPage()) * QUICKSCRUB_SNAP_DURATION_PER_PAGE;
            }
            this.mRecentsView.snapToPage(iBoundToRange, i2, interpolator);
        }
        if (z2 || z) {
            this.mRecentsView.performHapticFeedback(1, 1);
        }
    }

    @Override // com.android.launcher3.OnAlarmListener
    public void onAlarm(Alarm alarm) {
        boolean z;
        int nextPage = this.mRecentsView.getNextPage();
        if (this.mActivityControlHelper == null || this.mActivityControlHelper.getVisibleRecentsView() == null) {
            z = false;
        } else {
            z = true;
        }
        if (!z) {
            Log.w(TAG, "Failed to auto advance; recents not visible");
            return;
        }
        if (this.mQuickScrubSection == QUICK_SCRUB_THRESHOLDS.length && nextPage < this.mRecentsView.getPageCount() - 1) {
            goToPageWithHaptic(nextPage + 1);
        } else if (this.mQuickScrubSection == 0 && nextPage > 0) {
            goToPageWithHaptic(nextPage - 1);
        }
        this.mAutoAdvanceAlarm.setAlarm(AUTO_ADVANCE_DELAY);
    }
}
