package com.android.server.am;

import android.app.ActivityOptions;
import android.content.pm.ActivityInfo;
import android.graphics.Rect;
import com.android.server.am.LaunchParamsController;

public class ActivityLaunchParamsModifier implements LaunchParamsController.LaunchParamsModifier {
    private final ActivityStackSupervisor mSupervisor;

    ActivityLaunchParamsModifier(ActivityStackSupervisor activityStackSupervisor) {
        this.mSupervisor = activityStackSupervisor;
    }

    @Override
    public int onCalculate(TaskRecord taskRecord, ActivityInfo.WindowLayout windowLayout, ActivityRecord activityRecord, ActivityRecord activityRecord2, ActivityOptions activityOptions, LaunchParamsController.LaunchParams launchParams, LaunchParamsController.LaunchParams launchParams2) {
        Rect launchBounds;
        if (activityRecord == null || !this.mSupervisor.canUseActivityOptionsLaunchBounds(activityOptions) || ((!activityRecord.isResizeable() && (taskRecord == null || !taskRecord.isResizeable())) || (launchBounds = activityOptions.getLaunchBounds()) == null || launchBounds.isEmpty())) {
            return 0;
        }
        launchParams2.mBounds.set(launchBounds);
        return 1;
    }
}
