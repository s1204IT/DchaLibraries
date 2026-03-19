package com.android.server.am;

import android.app.ActivityManager;
import android.app.WindowConfiguration;
import android.util.SparseArray;
import com.android.server.am.TaskRecord;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

class RunningTasks {
    private static final Comparator<TaskRecord> LAST_ACTIVE_TIME_COMPARATOR = new Comparator() {
        @Override
        public final int compare(Object obj, Object obj2) {
            return Long.signum(((TaskRecord) obj2).lastActiveTime - ((TaskRecord) obj).lastActiveTime);
        }
    };
    private final TaskRecord.TaskActivitiesReport mTmpReport = new TaskRecord.TaskActivitiesReport();
    private final TreeSet<TaskRecord> mTmpSortedSet = new TreeSet<>(LAST_ACTIVE_TIME_COMPARATOR);
    private final ArrayList<TaskRecord> mTmpStackTasks = new ArrayList<>();

    RunningTasks() {
    }

    void getTasks(int i, List<ActivityManager.RunningTaskInfo> list, @WindowConfiguration.ActivityType int i2, @WindowConfiguration.WindowingMode int i3, SparseArray<ActivityDisplay> sparseArray, int i4, boolean z) {
        if (i <= 0) {
            return;
        }
        this.mTmpSortedSet.clear();
        this.mTmpStackTasks.clear();
        int size = sparseArray.size();
        for (int i5 = 0; i5 < size; i5++) {
            ActivityDisplay activityDisplayValueAt = sparseArray.valueAt(i5);
            for (int childCount = activityDisplayValueAt.getChildCount() - 1; childCount >= 0; childCount--) {
                activityDisplayValueAt.getChildAt(childCount).getRunningTasks(this.mTmpStackTasks, i2, i3, i4, z);
                for (int size2 = this.mTmpStackTasks.size() - 1; size2 >= 0; size2--) {
                    this.mTmpSortedSet.addAll(this.mTmpStackTasks);
                }
            }
        }
        Iterator<TaskRecord> it = this.mTmpSortedSet.iterator();
        for (int i6 = i; it.hasNext() && i6 != 0; i6--) {
            list.add(createRunningTaskInfo(it.next()));
        }
    }

    private ActivityManager.RunningTaskInfo createRunningTaskInfo(TaskRecord taskRecord) {
        taskRecord.getNumRunningActivities(this.mTmpReport);
        ActivityManager.RunningTaskInfo runningTaskInfo = new ActivityManager.RunningTaskInfo();
        runningTaskInfo.id = taskRecord.taskId;
        runningTaskInfo.stackId = taskRecord.getStackId();
        runningTaskInfo.baseActivity = this.mTmpReport.base.intent.getComponent();
        runningTaskInfo.topActivity = this.mTmpReport.top.intent.getComponent();
        runningTaskInfo.lastActiveTime = taskRecord.lastActiveTime;
        runningTaskInfo.description = taskRecord.lastDescription;
        runningTaskInfo.numActivities = this.mTmpReport.numActivities;
        runningTaskInfo.numRunning = this.mTmpReport.numRunning;
        runningTaskInfo.supportsSplitScreenMultiWindow = taskRecord.supportsSplitScreenWindowingMode();
        runningTaskInfo.resizeMode = taskRecord.mResizeMode;
        runningTaskInfo.configuration.setTo(taskRecord.getConfiguration());
        return runningTaskInfo;
    }
}
