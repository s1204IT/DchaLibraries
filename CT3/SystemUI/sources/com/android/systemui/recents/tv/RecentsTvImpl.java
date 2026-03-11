package com.android.systemui.recents.tv;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.SystemClock;
import android.os.UserHandle;
import com.android.systemui.SystemUIApplication;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.RecentsActivityLaunchState;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.RecentsImpl;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.activity.RecentsActivityStartingEvent;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.model.RecentsTaskLoader;
import com.android.systemui.recents.model.TaskStack;
import com.android.systemui.recents.model.ThumbnailData;
import com.android.systemui.recents.tv.views.TaskCardView;
import com.android.systemui.statusbar.tv.TvStatusBar;
import com.android.systemui.tv.pip.PipManager;

public class RecentsTvImpl extends RecentsImpl {
    private static final PipManager mPipManager = PipManager.getInstance();

    public RecentsTvImpl(Context context) {
        super(context);
    }

    @Override
    protected void startRecentsActivity(ActivityManager.RunningTaskInfo runningTask, boolean isHomeStackVisible, boolean animate, int growTarget) {
        RecentsTaskLoader loader = Recents.getTaskLoader();
        if (this.mTriggeredFromAltTab || sInstanceLoadPlan == null) {
            sInstanceLoadPlan = loader.createLoadPlan(this.mContext);
        }
        if (this.mTriggeredFromAltTab || !sInstanceLoadPlan.hasTasks()) {
            loader.preloadTasks(sInstanceLoadPlan, runningTask.id, !isHomeStackVisible);
        }
        TaskStack stack = sInstanceLoadPlan.getTaskStack();
        if (!animate) {
            startRecentsActivity(runningTask, ActivityOptions.makeCustomAnimation(this.mContext, -1, -1), false, false);
            return;
        }
        boolean hasRecentTasks = stack.getTaskCount() > 0;
        boolean useThumbnailTransition = (runningTask == null || isHomeStackVisible) ? false : hasRecentTasks;
        if (useThumbnailTransition) {
            ActivityOptions opts = getThumbnailTransitionActivityOptionsForTV(runningTask, stack.getTaskCount());
            if (opts != null) {
                startRecentsActivity(runningTask, opts, false, true);
            } else {
                useThumbnailTransition = false;
            }
        }
        if (!useThumbnailTransition) {
            startRecentsActivity(runningTask, (ActivityOptions) null, true, false);
        }
        this.mLastToggleTime = SystemClock.elapsedRealtime();
    }

    protected void startRecentsActivity(ActivityManager.RunningTaskInfo runningTask, ActivityOptions opts, boolean fromHome, boolean fromThumbnail) {
        RecentsConfiguration config = Recents.getConfiguration();
        RecentsActivityLaunchState launchState = config.getLaunchState();
        launchState.launchedFromHome = fromHome;
        launchState.launchedFromApp = fromThumbnail;
        launchState.launchedToTaskId = runningTask != null ? runningTask.id : -1;
        launchState.launchedWithAltTab = this.mTriggeredFromAltTab;
        Intent intent = new Intent();
        intent.setClassName("com.android.systemui", "com.android.systemui.recents.tv.RecentsTvActivity");
        intent.setFlags(276840448);
        if (opts != null) {
            this.mContext.startActivityAsUser(intent, opts.toBundle(), UserHandle.CURRENT);
        } else {
            this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
        }
        EventBus.getDefault().send(new RecentsActivityStartingEvent());
    }

    private ActivityOptions getThumbnailTransitionActivityOptionsForTV(ActivityManager.RunningTaskInfo runningTask, int numTasks) {
        Rect rect = TaskCardView.getStartingCardThumbnailRect(this.mContext, !mPipManager.isPipShown(), numTasks);
        SystemServicesProxy ssp = Recents.getSystemServices();
        ThumbnailData thumbnailData = ssp.getTaskThumbnail(runningTask.id);
        if (thumbnailData.thumbnail != null) {
            Bitmap thumbnail = Bitmap.createScaledBitmap(thumbnailData.thumbnail, rect.width(), rect.height(), false);
            return ActivityOptions.makeThumbnailAspectScaleDownAnimation(this.mDummyStackView, thumbnail, rect.left, rect.top, rect.width(), rect.height(), this.mHandler, null);
        }
        return getUnknownTransitionActivityOptions();
    }

    @Override
    public void onVisibilityChanged(Context context, boolean visible) {
        SystemUIApplication app = (SystemUIApplication) context;
        TvStatusBar statusBar = (TvStatusBar) app.getComponent(TvStatusBar.class);
        if (statusBar == null) {
            return;
        }
        statusBar.updateRecentsVisibility(visible);
    }
}
