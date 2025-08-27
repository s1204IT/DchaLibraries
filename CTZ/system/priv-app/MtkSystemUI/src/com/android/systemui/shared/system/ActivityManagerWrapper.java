package com.android.systemui.shared.system;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.AppGlobals;
import android.app.WindowConfiguration;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import com.android.systemui.shared.recents.model.Task;
import com.android.systemui.shared.recents.model.ThumbnailData;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/* loaded from: classes.dex */
public class ActivityManagerWrapper {
    private static final ActivityManagerWrapper sInstance = new ActivityManagerWrapper();
    private final PackageManager mPackageManager = AppGlobals.getInitialApplication().getPackageManager();
    private final BackgroundExecutor mBackgroundExecutor = BackgroundExecutor.get();
    private final TaskStackChangeListeners mTaskStackChangeListeners = new TaskStackChangeListeners(Looper.getMainLooper());

    private ActivityManagerWrapper() {
    }

    public static ActivityManagerWrapper getInstance() {
        return sInstance;
    }

    public int getCurrentUserId() {
        try {
            UserInfo currentUser = ActivityManager.getService().getCurrentUser();
            if (currentUser != null) {
                return currentUser.id;
            }
            return 0;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public ActivityManager.RunningTaskInfo getRunningTask() {
        return getRunningTask(3);
    }

    public ActivityManager.RunningTaskInfo getRunningTask(@WindowConfiguration.ActivityType int i) {
        try {
            List filteredTasks = ActivityManager.getService().getFilteredTasks(1, i, 2);
            if (filteredTasks.isEmpty()) {
                return null;
            }
            return (ActivityManager.RunningTaskInfo) filteredTasks.get(0);
        } catch (RemoteException e) {
            return null;
        }
    }

    public List<ActivityManager.RecentTaskInfo> getRecentTasks(int i, int i2) {
        try {
            return ActivityManager.getService().getRecentTasks(i, 2, i2).getList();
        } catch (RemoteException e) {
            Log.e("ActivityManagerWrapper", "Failed to get recent tasks", e);
            return new ArrayList();
        }
    }

    public ThumbnailData getTaskThumbnail(int i, boolean z) {
        ActivityManager.TaskSnapshot taskSnapshot;
        try {
            taskSnapshot = ActivityManager.getService().getTaskSnapshot(i, z);
        } catch (RemoteException e) {
            Log.w("ActivityManagerWrapper", "Failed to retrieve task snapshot", e);
            taskSnapshot = null;
        }
        if (taskSnapshot != null) {
            return new ThumbnailData(taskSnapshot);
        }
        return new ThumbnailData();
    }

    public String getBadgedActivityLabel(ActivityInfo activityInfo, int i) {
        return getBadgedLabel(activityInfo.loadLabel(this.mPackageManager).toString(), i);
    }

    public String getBadgedApplicationLabel(ApplicationInfo applicationInfo, int i) {
        return getBadgedLabel(applicationInfo.loadLabel(this.mPackageManager).toString(), i);
    }

    public String getBadgedContentDescription(ActivityInfo activityInfo, int i, ActivityManager.TaskDescription taskDescription) {
        String string;
        if (taskDescription != null && taskDescription.getLabel() != null) {
            string = taskDescription.getLabel();
        } else {
            string = activityInfo.loadLabel(this.mPackageManager).toString();
        }
        String string2 = activityInfo.applicationInfo.loadLabel(this.mPackageManager).toString();
        String badgedLabel = getBadgedLabel(string2, i);
        if (string2.equals(string)) {
            return badgedLabel;
        }
        return badgedLabel + " " + string;
    }

    private String getBadgedLabel(String str, int i) {
        if (i != UserHandle.myUserId()) {
            return this.mPackageManager.getUserBadgedLabel(str, new UserHandle(i)).toString();
        }
        return str;
    }

    public void startActivityFromRecentsAsync(Task.TaskKey taskKey, ActivityOptions activityOptions, Consumer<Boolean> consumer, Handler handler) {
        startActivityFromRecentsAsync(taskKey, activityOptions, 0, 0, consumer, handler);
    }

    public void startActivityFromRecentsAsync(final Task.TaskKey taskKey, ActivityOptions activityOptions, int i, int i2, final Consumer<Boolean> consumer, final Handler handler) {
        if (taskKey.windowingMode == 3) {
            if (activityOptions == null) {
                activityOptions = ActivityOptions.makeBasic();
            }
            activityOptions.setLaunchWindowingMode(4);
        } else if (i != 0 || i2 != 0) {
            if (activityOptions == null) {
                activityOptions = ActivityOptions.makeBasic();
            }
            activityOptions.setLaunchWindowingMode(i);
            activityOptions.setLaunchActivityType(i2);
        }
        final ActivityOptions activityOptions2 = activityOptions;
        this.mBackgroundExecutor.submit(new Runnable() { // from class: com.android.systemui.shared.system.ActivityManagerWrapper.5
            @Override // java.lang.Runnable
            public void run() {
                final boolean zStartActivityFromRecents;
                try {
                    zStartActivityFromRecents = ActivityManagerWrapper.this.startActivityFromRecents(taskKey.id, activityOptions2);
                } catch (Exception e) {
                    zStartActivityFromRecents = false;
                }
                if (consumer != null) {
                    handler.post(new Runnable() { // from class: com.android.systemui.shared.system.ActivityManagerWrapper.5.1
                        @Override // java.lang.Runnable
                        public void run() {
                            consumer.accept(Boolean.valueOf(zStartActivityFromRecents));
                        }
                    });
                }
            }
        });
    }

    public boolean startActivityFromRecents(int i, ActivityOptions activityOptions) {
        Bundle bundle;
        if (activityOptions == null) {
            bundle = null;
        } else {
            try {
                bundle = activityOptions.toBundle();
            } catch (Exception e) {
                return false;
            }
        }
        ActivityManager.getService().startActivityFromRecents(i, bundle);
        return true;
    }

    public void registerTaskStackListener(TaskStackChangeListener taskStackChangeListener) {
        synchronized (this.mTaskStackChangeListeners) {
            this.mTaskStackChangeListeners.addListener(ActivityManager.getService(), taskStackChangeListener);
        }
    }

    public void unregisterTaskStackListener(TaskStackChangeListener taskStackChangeListener) {
        synchronized (this.mTaskStackChangeListeners) {
            this.mTaskStackChangeListeners.removeListener(taskStackChangeListener);
        }
    }

    public void closeSystemWindows(final String str) {
        this.mBackgroundExecutor.submit(new Runnable() { // from class: com.android.systemui.shared.system.ActivityManagerWrapper.6
            @Override // java.lang.Runnable
            public void run() {
                try {
                    ActivityManager.getService().closeSystemDialogs(str);
                } catch (RemoteException e) {
                    Log.w("ActivityManagerWrapper", "Failed to close system windows", e);
                }
            }
        });
    }

    public void removeTask(final int i) {
        this.mBackgroundExecutor.submit(new Runnable() { // from class: com.android.systemui.shared.system.ActivityManagerWrapper.7
            @Override // java.lang.Runnable
            public void run() {
                try {
                    ActivityManager.getService().removeTask(i);
                } catch (RemoteException e) {
                    Log.w("ActivityManagerWrapper", "Failed to remove task=" + i, e);
                }
            }
        });
    }

    public void cancelWindowTransition(int i) {
        try {
            ActivityManager.getService().cancelTaskWindowTransition(i);
        } catch (RemoteException e) {
            Log.w("ActivityManagerWrapper", "Failed to cancel window transition for task=" + i, e);
        }
    }

    public boolean isScreenPinningActive() {
        try {
            return ActivityManager.getService().getLockTaskModeState() == 2;
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean isScreenPinningEnabled() {
        return Settings.System.getInt(AppGlobals.getInitialApplication().getContentResolver(), "lock_to_app_enabled", 0) != 0;
    }

    public boolean isLockToAppActive() {
        try {
            return ActivityManager.getService().getLockTaskModeState() != 0;
        } catch (RemoteException e) {
            return false;
        }
    }
}
