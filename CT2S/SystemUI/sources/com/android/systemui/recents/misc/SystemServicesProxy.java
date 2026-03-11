package com.android.systemui.recents.misc;

import android.R;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.ActivityOptions;
import android.app.AppGlobals;
import android.app.IActivityManager;
import android.app.ITaskStackListener;
import android.app.SearchManager;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.util.Pair;
import android.view.Display;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class SystemServicesProxy {
    static final BitmapFactory.Options sBitmapOptions = new BitmapFactory.Options();
    AccessibilityManager mAccm;
    ActivityManager mAm;
    ComponentName mAssistComponent;
    AppWidgetManager mAwm;
    Canvas mBgProtectionCanvas;
    Paint mBgProtectionPaint;
    Display mDisplay;
    int mDummyThumbnailHeight;
    int mDummyThumbnailWidth;
    IActivityManager mIam = ActivityManagerNative.getDefault();
    IPackageManager mIpm = AppGlobals.getPackageManager();
    PackageManager mPm;
    String mRecentsPackage;
    SearchManager mSm;
    WindowManager mWm;

    static {
        sBitmapOptions.inMutable = true;
    }

    public SystemServicesProxy(Context context) {
        this.mAccm = AccessibilityManager.getInstance(context);
        this.mAm = (ActivityManager) context.getSystemService("activity");
        this.mAwm = AppWidgetManager.getInstance(context);
        this.mPm = context.getPackageManager();
        this.mSm = (SearchManager) context.getSystemService("search");
        this.mWm = (WindowManager) context.getSystemService("window");
        this.mDisplay = this.mWm.getDefaultDisplay();
        this.mRecentsPackage = context.getPackageName();
        Resources res = context.getResources();
        this.mDummyThumbnailWidth = res.getDimensionPixelSize(R.dimen.thumbnail_width);
        this.mDummyThumbnailHeight = res.getDimensionPixelSize(R.dimen.thumbnail_height);
        this.mBgProtectionPaint = new Paint();
        this.mBgProtectionPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_ATOP));
        this.mBgProtectionPaint.setColor(-1);
        this.mBgProtectionCanvas = new Canvas();
        Intent assist = this.mSm.getAssistIntent(context, false);
        if (assist != null) {
            this.mAssistComponent = assist.getComponent();
        }
    }

    public List<ActivityManager.RecentTaskInfo> getRecentTasks(int numLatestTasks, int userId, boolean isTopTaskHome) {
        if (this.mAm == null) {
            return null;
        }
        int numTasksToQuery = Math.max(10, numLatestTasks);
        List<ActivityManager.RecentTaskInfo> tasks = this.mAm.getRecentTasksForUser(numTasksToQuery, 15, userId);
        if (tasks == null) {
            return new ArrayList();
        }
        boolean isFirstValidTask = true;
        Iterator<ActivityManager.RecentTaskInfo> iter = tasks.iterator();
        while (iter.hasNext()) {
            ActivityManager.RecentTaskInfo t = iter.next();
            boolean isExcluded = (t.baseIntent.getFlags() & 8388608) == 8388608;
            if (isExcluded && (isTopTaskHome || !isFirstValidTask)) {
                iter.remove();
            } else {
                isFirstValidTask = false;
            }
        }
        return tasks.subList(0, Math.min(tasks.size(), numLatestTasks));
    }

    public List<ActivityManager.RunningTaskInfo> getRunningTasks(int numTasks) {
        if (this.mAm == null) {
            return null;
        }
        return this.mAm.getRunningTasks(numTasks);
    }

    public ActivityManager.RunningTaskInfo getTopMostTask() {
        List<ActivityManager.RunningTaskInfo> tasks = getRunningTasks(1);
        if (tasks.isEmpty()) {
            return null;
        }
        return tasks.get(0);
    }

    public boolean isRecentsTopMost(ActivityManager.RunningTaskInfo topTask, AtomicBoolean isHomeTopMost) {
        if (topTask == null) {
            return false;
        }
        ComponentName topActivity = topTask.topActivity;
        if (topActivity.getPackageName().equals("com.android.systemui") && topActivity.getClassName().equals("com.android.systemui.recents.RecentsActivity")) {
            if (isHomeTopMost != null) {
                isHomeTopMost.set(false);
            }
            return true;
        }
        if (isHomeTopMost == null) {
            return false;
        }
        isHomeTopMost.set(isInHomeStack(topTask.id));
        return false;
    }

    public boolean isInHomeStack(int taskId) {
        if (this.mAm == null) {
            return false;
        }
        return this.mAm.isInHomeStack(taskId);
    }

    public Bitmap getTaskThumbnail(int taskId) {
        if (this.mAm == null) {
            return null;
        }
        Bitmap thumbnail = getThumbnail(this.mAm, taskId);
        if (thumbnail != null) {
            thumbnail.setHasAlpha(false);
            if (Color.alpha(thumbnail.getPixel(0, 0)) == 0) {
                this.mBgProtectionCanvas.setBitmap(thumbnail);
                this.mBgProtectionCanvas.drawRect(0.0f, 0.0f, thumbnail.getWidth(), thumbnail.getHeight(), this.mBgProtectionPaint);
                this.mBgProtectionCanvas.setBitmap(null);
                Log.e("SystemServicesProxy", "Invalid screenshot detected from getTaskThumbnail()");
                return thumbnail;
            }
            return thumbnail;
        }
        return thumbnail;
    }

    public static Bitmap getThumbnail(ActivityManager activityManager, int taskId) {
        ActivityManager.TaskThumbnail taskThumbnail = activityManager.getTaskThumbnail(taskId);
        if (taskThumbnail == null) {
            return null;
        }
        Bitmap thumbnail = taskThumbnail.mainThumbnail;
        ParcelFileDescriptor descriptor = taskThumbnail.thumbnailFileDescriptor;
        if (thumbnail == null && descriptor != null) {
            thumbnail = BitmapFactory.decodeFileDescriptor(descriptor.getFileDescriptor(), null, sBitmapOptions);
        }
        if (descriptor != null) {
            try {
                descriptor.close();
                return thumbnail;
            } catch (IOException e) {
                return thumbnail;
            }
        }
        return thumbnail;
    }

    public void moveTaskToFront(int taskId, ActivityOptions opts) {
        if (this.mAm != null) {
            if (opts != null) {
                this.mAm.moveTaskToFront(taskId, 1, opts.toBundle());
            } else {
                this.mAm.moveTaskToFront(taskId, 1);
            }
        }
    }

    public void removeTask(int taskId) {
        if (this.mAm != null) {
            this.mAm.removeTask(taskId);
        }
    }

    public ActivityInfo getActivityInfo(ComponentName cn, int userId) {
        if (this.mIpm == null) {
            return null;
        }
        try {
            return this.mIpm.getActivityInfo(cn, 128, userId);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getActivityLabel(ActivityInfo info) {
        if (this.mPm == null) {
            return null;
        }
        return info.loadLabel(this.mPm).toString();
    }

    public Drawable getActivityIcon(ActivityInfo info, int userId) {
        if (this.mPm == null) {
            return null;
        }
        Drawable icon = info.loadIcon(this.mPm);
        return getBadgedIcon(icon, userId);
    }

    public Drawable getBadgedIcon(Drawable icon, int userId) {
        if (userId != UserHandle.myUserId()) {
            return this.mPm.getUserBadgedIcon(icon, new UserHandle(userId));
        }
        return icon;
    }

    public String getHomeActivityPackageName() {
        if (this.mPm == null) {
            return null;
        }
        ArrayList<ResolveInfo> homeActivities = new ArrayList<>();
        ComponentName defaultHomeActivity = this.mPm.getHomeActivities(homeActivities);
        if (defaultHomeActivity != null) {
            return defaultHomeActivity.getPackageName();
        }
        if (homeActivities.size() != 1) {
            return null;
        }
        ResolveInfo info = homeActivities.get(0);
        if (info.activityInfo != null) {
            return info.activityInfo.packageName;
        }
        return null;
    }

    public boolean isForegroundUserOwner() {
        if (this.mAm == null) {
            return false;
        }
        ActivityManager activityManager = this.mAm;
        return ActivityManager.getCurrentUser() == 0;
    }

    public AppWidgetProviderInfo resolveSearchAppWidget() {
        if (this.mAwm != null && this.mAssistComponent != null) {
            List<AppWidgetProviderInfo> widgets = this.mAwm.getInstalledProviders(4);
            for (AppWidgetProviderInfo info : widgets) {
                if (info.provider.getPackageName().equals(this.mAssistComponent.getPackageName())) {
                    return info;
                }
            }
            return null;
        }
        return null;
    }

    public Pair<Integer, AppWidgetProviderInfo> bindSearchAppWidget(AppWidgetHost host) {
        AppWidgetProviderInfo searchWidgetInfo;
        if (this.mAwm == null || this.mAssistComponent == null || (searchWidgetInfo = resolveSearchAppWidget()) == null) {
            return null;
        }
        int searchWidgetId = host.allocateAppWidgetId();
        Bundle opts = new Bundle();
        opts.putInt("appWidgetCategory", 4);
        if (!this.mAwm.bindAppWidgetIdIfAllowed(searchWidgetId, searchWidgetInfo.provider, opts)) {
            host.deleteAppWidgetId(searchWidgetId);
            return null;
        }
        return new Pair<>(Integer.valueOf(searchWidgetId), searchWidgetInfo);
    }

    public AppWidgetProviderInfo getAppWidgetInfo(int appWidgetId) {
        if (this.mAwm == null) {
            return null;
        }
        return this.mAwm.getAppWidgetInfo(appWidgetId);
    }

    public void unbindSearchAppWidget(AppWidgetHost host, int appWidgetId) {
        if (this.mAwm != null) {
            host.deleteAppWidgetId(appWidgetId);
        }
    }

    public boolean isTouchExplorationEnabled() {
        return this.mAccm != null && this.mAccm.isEnabled() && this.mAccm.isTouchExplorationEnabled();
    }

    public int getGlobalSetting(Context context, String setting) {
        ContentResolver cr = context.getContentResolver();
        return Settings.Global.getInt(cr, setting, 0);
    }

    public int getSystemSetting(Context context, String setting) {
        ContentResolver cr = context.getContentResolver();
        return Settings.System.getInt(cr, setting, 0);
    }

    public Rect getWindowRect() {
        Rect windowRect = new Rect();
        if (this.mWm != null) {
            Point p = new Point();
            this.mWm.getDefaultDisplay().getRealSize(p);
            windowRect.set(0, 0, p.x, p.y);
        }
        return windowRect;
    }

    public boolean startActivityFromRecents(Context context, int taskId, String taskName, ActivityOptions options) {
        if (this.mIam != null) {
            try {
                this.mIam.startActivityFromRecents(taskId, options == null ? null : options.toBundle());
                return true;
            } catch (Exception e) {
                Console.logError(context, context.getString(com.android.systemui.R.string.recents_launch_error_message, taskName));
            }
        }
        return false;
    }

    public void startInPlaceAnimationOnFrontMostApplication(ActivityOptions opts) {
        if (this.mIam != null) {
            try {
                this.mIam.startInPlaceAnimationOnFrontMostApplication(opts);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void registerTaskStackListener(ITaskStackListener listener) {
        if (this.mIam != null) {
            try {
                this.mIam.registerTaskStackListener(listener);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
