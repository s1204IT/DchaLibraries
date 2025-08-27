package com.android.systemui.stackdivider;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.UserHandle;
import android.util.ArraySet;
import com.android.systemui.R;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.activity.AppTransitionFinishedEvent;
import com.android.systemui.recents.events.component.ShowUserToastEvent;
import com.android.systemui.recents.misc.SysUiTaskStackChangeListener;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.stackdivider.events.StartedDragingEvent;
import com.android.systemui.stackdivider.events.StoppedDragingEvent;

/* loaded from: classes.dex */
public class ForcedResizableInfoActivityController {
    private final Context mContext;
    private boolean mDividerDraging;
    private final Handler mHandler = new Handler();
    private final ArraySet<PendingTaskRecord> mPendingTasks = new ArraySet<>();
    private final ArraySet<String> mPackagesShownInSession = new ArraySet<>();
    private final Runnable mTimeoutRunnable = new Runnable() { // from class: com.android.systemui.stackdivider.ForcedResizableInfoActivityController.1
        @Override // java.lang.Runnable
        public void run() {
            ForcedResizableInfoActivityController.this.showPending();
        }
    };

    private class PendingTaskRecord {
        int reason;
        int taskId;

        PendingTaskRecord(int i, int i2) {
            this.taskId = i;
            this.reason = i2;
        }
    }

    public ForcedResizableInfoActivityController(Context context) throws NoSuchMethodException, SecurityException {
        this.mContext = context;
        EventBus.getDefault().register(this);
        ActivityManagerWrapper.getInstance().registerTaskStackListener(new SysUiTaskStackChangeListener() { // from class: com.android.systemui.stackdivider.ForcedResizableInfoActivityController.2
            @Override // com.android.systemui.shared.system.TaskStackChangeListener
            public void onActivityForcedResizable(String str, int i, int i2) {
                ForcedResizableInfoActivityController.this.activityForcedResizable(str, i, i2);
            }

            @Override // com.android.systemui.shared.system.TaskStackChangeListener
            public void onActivityDismissingDockedStack() {
                ForcedResizableInfoActivityController.this.activityDismissingDockedStack();
            }

            @Override // com.android.systemui.shared.system.TaskStackChangeListener
            public void onActivityLaunchOnSecondaryDisplayFailed() {
                ForcedResizableInfoActivityController.this.activityLaunchOnSecondaryDisplayFailed();
            }
        });
    }

    public void notifyDockedStackExistsChanged(boolean z) {
        if (!z) {
            this.mPackagesShownInSession.clear();
        }
    }

    public final void onBusEvent(AppTransitionFinishedEvent appTransitionFinishedEvent) {
        if (!this.mDividerDraging) {
            showPending();
        }
    }

    public final void onBusEvent(StartedDragingEvent startedDragingEvent) {
        this.mDividerDraging = true;
        this.mHandler.removeCallbacks(this.mTimeoutRunnable);
    }

    public final void onBusEvent(StoppedDragingEvent stoppedDragingEvent) {
        this.mDividerDraging = false;
        showPending();
    }

    private void activityForcedResizable(String str, int i, int i2) {
        if (debounce(str)) {
            return;
        }
        this.mPendingTasks.add(new PendingTaskRecord(i, i2));
        postTimeout();
    }

    private void activityDismissingDockedStack() {
        EventBus.getDefault().send(new ShowUserToastEvent(R.string.dock_non_resizeble_failed_to_dock_text, 0));
    }

    private void activityLaunchOnSecondaryDisplayFailed() {
        EventBus.getDefault().send(new ShowUserToastEvent(R.string.activity_launch_on_secondary_display_failed_text, 0));
    }

    private void showPending() {
        this.mHandler.removeCallbacks(this.mTimeoutRunnable);
        for (int size = this.mPendingTasks.size() - 1; size >= 0; size--) {
            PendingTaskRecord pendingTaskRecordValueAt = this.mPendingTasks.valueAt(size);
            Intent intent = new Intent(this.mContext, (Class<?>) ForcedResizableInfoActivity.class);
            ActivityOptions activityOptionsMakeBasic = ActivityOptions.makeBasic();
            activityOptionsMakeBasic.setLaunchTaskId(pendingTaskRecordValueAt.taskId);
            activityOptionsMakeBasic.setTaskOverlay(true, true);
            intent.putExtra("extra_forced_resizeable_reason", pendingTaskRecordValueAt.reason);
            this.mContext.startActivityAsUser(intent, activityOptionsMakeBasic.toBundle(), UserHandle.CURRENT);
        }
        this.mPendingTasks.clear();
    }

    private void postTimeout() {
        this.mHandler.removeCallbacks(this.mTimeoutRunnable);
        this.mHandler.postDelayed(this.mTimeoutRunnable, 1000L);
    }

    private boolean debounce(String str) {
        if (str == null) {
            return false;
        }
        if ("com.android.systemui".equals(str)) {
            return true;
        }
        boolean zContains = this.mPackagesShownInSession.contains(str);
        this.mPackagesShownInSession.add(str);
        return zContains;
    }
}
