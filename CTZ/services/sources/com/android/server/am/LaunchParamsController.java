package com.android.server.am;

import android.app.ActivityOptions;
import android.content.pm.ActivityInfo;
import android.graphics.Rect;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

class LaunchParamsController {
    private final ActivityManagerService mService;
    private final List<LaunchParamsModifier> mModifiers = new ArrayList();
    private final LaunchParams mTmpParams = new LaunchParams();
    private final LaunchParams mTmpCurrent = new LaunchParams();
    private final LaunchParams mTmpResult = new LaunchParams();

    interface LaunchParamsModifier {
        public static final int RESULT_CONTINUE = 2;
        public static final int RESULT_DONE = 1;
        public static final int RESULT_SKIP = 0;

        @Retention(RetentionPolicy.SOURCE)
        public @interface Result {
        }

        int onCalculate(TaskRecord taskRecord, ActivityInfo.WindowLayout windowLayout, ActivityRecord activityRecord, ActivityRecord activityRecord2, ActivityOptions activityOptions, LaunchParams launchParams, LaunchParams launchParams2);
    }

    LaunchParamsController(ActivityManagerService activityManagerService) {
        this.mService = activityManagerService;
    }

    void registerDefaultModifiers(ActivityStackSupervisor activityStackSupervisor) {
        registerModifier(new TaskLaunchParamsModifier());
        registerModifier(new ActivityLaunchParamsModifier(activityStackSupervisor));
    }

    void calculate(TaskRecord taskRecord, ActivityInfo.WindowLayout windowLayout, ActivityRecord activityRecord, ActivityRecord activityRecord2, ActivityOptions activityOptions, LaunchParams launchParams) {
        launchParams.reset();
        for (int size = this.mModifiers.size() - 1; size >= 0; size--) {
            this.mTmpCurrent.set(launchParams);
            this.mTmpResult.reset();
            switch (this.mModifiers.get(size).onCalculate(taskRecord, windowLayout, activityRecord, activityRecord2, activityOptions, this.mTmpCurrent, this.mTmpResult)) {
                case 1:
                    launchParams.set(this.mTmpResult);
                    return;
                case 2:
                    launchParams.set(this.mTmpResult);
                    break;
            }
        }
    }

    boolean layoutTask(TaskRecord taskRecord, ActivityInfo.WindowLayout windowLayout) {
        return layoutTask(taskRecord, windowLayout, null, null, null);
    }

    boolean layoutTask(TaskRecord taskRecord, ActivityInfo.WindowLayout windowLayout, ActivityRecord activityRecord, ActivityRecord activityRecord2, ActivityOptions activityOptions) {
        calculate(taskRecord, windowLayout, activityRecord, activityRecord2, activityOptions, this.mTmpParams);
        if (this.mTmpParams.isEmpty()) {
            return false;
        }
        this.mService.mWindowManager.deferSurfaceLayout();
        try {
            if (this.mTmpParams.hasPreferredDisplay() && this.mTmpParams.mPreferredDisplayId != taskRecord.getStack().getDisplay().mDisplayId) {
                this.mService.moveStackToDisplay(taskRecord.getStackId(), this.mTmpParams.mPreferredDisplayId);
            }
            if (this.mTmpParams.hasWindowingMode() && this.mTmpParams.mWindowingMode != taskRecord.getStack().getWindowingMode()) {
                taskRecord.getStack().setWindowingMode(this.mTmpParams.mWindowingMode);
            }
            if (this.mTmpParams.mBounds.isEmpty()) {
                return false;
            }
            taskRecord.updateOverrideConfiguration(this.mTmpParams.mBounds);
            return true;
        } finally {
            this.mService.mWindowManager.continueSurfaceLayout();
        }
    }

    void registerModifier(LaunchParamsModifier launchParamsModifier) {
        if (this.mModifiers.contains(launchParamsModifier)) {
            return;
        }
        this.mModifiers.add(launchParamsModifier);
    }

    static class LaunchParams {
        final Rect mBounds = new Rect();
        int mPreferredDisplayId;
        int mWindowingMode;

        LaunchParams() {
        }

        void reset() {
            this.mBounds.setEmpty();
            this.mPreferredDisplayId = -1;
            this.mWindowingMode = 0;
        }

        void set(LaunchParams launchParams) {
            this.mBounds.set(launchParams.mBounds);
            this.mPreferredDisplayId = launchParams.mPreferredDisplayId;
            this.mWindowingMode = launchParams.mWindowingMode;
        }

        boolean isEmpty() {
            return this.mBounds.isEmpty() && this.mPreferredDisplayId == -1 && this.mWindowingMode == 0;
        }

        boolean hasWindowingMode() {
            return this.mWindowingMode != 0;
        }

        boolean hasPreferredDisplay() {
            return this.mPreferredDisplayId != -1;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            LaunchParams launchParams = (LaunchParams) obj;
            if (this.mPreferredDisplayId != launchParams.mPreferredDisplayId || this.mWindowingMode != launchParams.mWindowingMode) {
                return false;
            }
            if (this.mBounds != null) {
                return this.mBounds.equals(launchParams.mBounds);
            }
            if (launchParams.mBounds == null) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return (31 * (((this.mBounds != null ? this.mBounds.hashCode() : 0) * 31) + this.mPreferredDisplayId)) + this.mWindowingMode;
        }
    }
}
