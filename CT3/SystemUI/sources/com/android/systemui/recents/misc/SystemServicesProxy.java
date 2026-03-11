package com.android.systemui.recents.misc;

import android.R;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.ActivityOptions;
import android.app.AppGlobals;
import android.app.IActivityManager;
import android.app.ITaskStackListener;
import android.app.UiModeManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
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
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.IRemoteCallback;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.ArraySet;
import android.util.Log;
import android.util.MutableBoolean;
import android.view.Display;
import android.view.IAppTransitionAnimationSpecsFuture;
import android.view.IDockedStackListener;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.view.accessibility.AccessibilityManager;
import com.android.internal.app.AssistUtils;
import com.android.internal.os.BackgroundThread;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.model.ThumbnailData;
import com.mediatek.systemui.statusbar.util.FeatureOptions;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class SystemServicesProxy {
    static final BitmapFactory.Options sBitmapOptions = new BitmapFactory.Options();
    static final List<String> sRecentsBlacklist;
    private static SystemServicesProxy sSystemServicesProxy;
    AccessibilityManager mAccm;
    ActivityManager mAm;
    ComponentName mAssistComponent;
    AssistUtils mAssistUtils;
    Canvas mBgProtectionCanvas;
    Paint mBgProtectionPaint;
    Display mDisplay;
    int mDummyThumbnailHeight;
    int mDummyThumbnailWidth;
    boolean mHasFreeformWorkspaceSupport;
    boolean mIsSafeMode;
    PackageManager mPm;
    String mRecentsPackage;
    UserManager mUm;
    WindowManager mWm;
    private final Handler mHandler = new H(this, null);
    private ITaskStackListener.Stub mTaskStackListener = new ITaskStackListener.Stub() {
        public void onTaskStackChanged() throws RemoteException {
            SystemServicesProxy.this.mHandler.removeMessages(1);
            SystemServicesProxy.this.mHandler.sendEmptyMessage(1);
        }

        public void onActivityPinned() throws RemoteException {
            SystemServicesProxy.this.mHandler.removeMessages(2);
            SystemServicesProxy.this.mHandler.sendEmptyMessage(2);
        }

        public void onPinnedActivityRestartAttempt() throws RemoteException {
            SystemServicesProxy.this.mHandler.removeMessages(3);
            SystemServicesProxy.this.mHandler.sendEmptyMessage(3);
        }

        public void onPinnedStackAnimationEnded() throws RemoteException {
            SystemServicesProxy.this.mHandler.removeMessages(4);
            SystemServicesProxy.this.mHandler.sendEmptyMessage(4);
        }

        public void onActivityForcedResizable(String packageName, int taskId) throws RemoteException {
            SystemServicesProxy.this.mHandler.obtainMessage(5, taskId, 0, packageName).sendToTarget();
        }

        public void onActivityDismissingDockedStack() throws RemoteException {
            SystemServicesProxy.this.mHandler.sendEmptyMessage(6);
        }
    };
    private List<TaskStackListener> mTaskStackListeners = new ArrayList();
    IActivityManager mIam = ActivityManagerNative.getDefault();
    IPackageManager mIpm = AppGlobals.getPackageManager();

    static {
        sBitmapOptions.inMutable = true;
        sBitmapOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        if (FeatureOptions.LOW_RAM_SUPPORT) {
            sBitmapOptions.inSampleSize = 2;
        }
        sRecentsBlacklist = new ArrayList();
        sRecentsBlacklist.add("com.android.systemui.tv.pip.PipOnboardingActivity");
        sRecentsBlacklist.add("com.android.systemui.tv.pip.PipMenuActivity");
    }

    public static abstract class TaskStackListener {
        public void onTaskStackChanged() {
        }

        public void onActivityPinned() {
        }

        public void onPinnedActivityRestartAttempt() {
        }

        public void onPinnedStackAnimationEnded() {
        }

        public void onActivityForcedResizable(String packageName, int taskId) {
        }

        public void onActivityDismissingDockedStack() {
        }
    }

    private SystemServicesProxy(Context context) {
        this.mAccm = AccessibilityManager.getInstance(context);
        this.mAm = (ActivityManager) context.getSystemService("activity");
        this.mPm = context.getPackageManager();
        this.mAssistUtils = new AssistUtils(context);
        this.mWm = (WindowManager) context.getSystemService("window");
        this.mUm = UserManager.get(context);
        this.mDisplay = this.mWm.getDefaultDisplay();
        this.mRecentsPackage = context.getPackageName();
        boolean z = this.mPm.hasSystemFeature("android.software.freeform_window_management") || Settings.Global.getInt(context.getContentResolver(), "enable_freeform_support", 0) != 0;
        this.mHasFreeformWorkspaceSupport = z;
        this.mIsSafeMode = this.mPm.isSafeMode();
        Resources res = context.getResources();
        this.mDummyThumbnailWidth = res.getDimensionPixelSize(R.dimen.thumbnail_width);
        this.mDummyThumbnailHeight = res.getDimensionPixelSize(R.dimen.thumbnail_height);
        this.mBgProtectionPaint = new Paint();
        this.mBgProtectionPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_ATOP));
        this.mBgProtectionPaint.setColor(-1);
        this.mBgProtectionCanvas = new Canvas();
        this.mAssistComponent = this.mAssistUtils.getAssistComponentForUser(UserHandle.myUserId());
        UiModeManager uiModeManager = (UiModeManager) context.getSystemService("uimode");
        if (uiModeManager.getCurrentModeType() != 4) {
            return;
        }
        Collections.addAll(sRecentsBlacklist, res.getStringArray(com.android.systemui.R.array.recents_tv_blacklist_array));
    }

    public static SystemServicesProxy getInstance(Context context) {
        if (!Looper.getMainLooper().isCurrentThread()) {
            throw new RuntimeException("Must be called on the UI thread");
        }
        if (sSystemServicesProxy == null) {
            sSystemServicesProxy = new SystemServicesProxy(context);
        }
        return sSystemServicesProxy;
    }

    public List<ActivityManager.RecentTaskInfo> getRecentTasks(int numLatestTasks, int userId, boolean includeFrontMostExcludedTask, ArraySet<Integer> quietProfileIds) {
        if (this.mAm == null) {
            return null;
        }
        int numTasksToQuery = Math.max(10, numLatestTasks);
        int flags = includeFrontMostExcludedTask ? 63 : 62;
        List<ActivityManager.RecentTaskInfo> tasks = null;
        try {
            tasks = this.mAm.getRecentTasksForUser(numTasksToQuery, flags, userId);
        } catch (Exception e) {
            Log.e("SystemServicesProxy", "Failed to get recent tasks", e);
        }
        if (tasks == null) {
            return new ArrayList();
        }
        boolean isFirstValidTask = true;
        Iterator<ActivityManager.RecentTaskInfo> iter = tasks.iterator();
        while (iter.hasNext()) {
            ActivityManager.RecentTaskInfo t = iter.next();
            if (sRecentsBlacklist.contains(t.realActivity.getClassName()) || sRecentsBlacklist.contains(t.realActivity.getPackageName())) {
                iter.remove();
            } else {
                boolean isExcluded = ((t.baseIntent.getFlags() & 8388608) == 8388608) | quietProfileIds.contains(Integer.valueOf(t.userId));
                Task.TaskKey taskKey = new Task.TaskKey(t.persistentId, t.stackId, t.baseIntent, t.userId, t.firstActiveTime, t.lastActiveTime);
                Log.d("SystemServicesProxy", "getRecentTasks:TASK = " + taskKey.toString() + "/isExcluded = " + isExcluded + "/includeFrontMostExcludedTask = " + includeFrontMostExcludedTask + "/isFirstValidTask = " + isFirstValidTask + "/t.id = " + t.id);
                if (isExcluded && (!isFirstValidTask || !includeFrontMostExcludedTask)) {
                    iter.remove();
                }
                isFirstValidTask = false;
            }
        }
        return tasks.subList(0, Math.min(tasks.size(), numLatestTasks));
    }

    public ActivityManager.RunningTaskInfo getRunningTask() {
        List<ActivityManager.RunningTaskInfo> tasks = this.mAm.getRunningTasks(1);
        if (tasks == null || tasks.isEmpty()) {
            return null;
        }
        Log.d("SystemServicesProxy", "getTopMostTask: tasks: " + tasks.get(0).id);
        return tasks.get(0);
    }

    public boolean isRecentsActivityVisible() {
        return isRecentsActivityVisible(null);
    }

    public boolean isRecentsActivityVisible(MutableBoolean isHomeStackVisible) {
        if (this.mIam == null) {
            return false;
        }
        try {
            ActivityManager.StackInfo stackInfo = this.mIam.getStackInfo(0);
            ActivityManager.StackInfo fullscreenStackInfo = this.mIam.getStackInfo(1);
            ComponentName topActivity = stackInfo.topActivity;
            boolean homeStackVisibleNotOccluded = stackInfo.visible;
            if (fullscreenStackInfo != null) {
                boolean isFullscreenStackOccludingHome = fullscreenStackInfo.visible && fullscreenStackInfo.position > stackInfo.position;
                homeStackVisibleNotOccluded &= !isFullscreenStackOccludingHome;
            }
            if (isHomeStackVisible != null) {
                isHomeStackVisible.value = homeStackVisibleNotOccluded;
            }
            if (!homeStackVisibleNotOccluded || topActivity == null || !topActivity.getPackageName().equals("com.android.systemui")) {
                return false;
            }
            if (topActivity.getClassName().equals("com.android.systemui.recents.RecentsActivity")) {
                return true;
            }
            return topActivity.getClassName().equals("com.android.systemui.recents.tv.RecentsTvActivity");
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean hasFreeformWorkspaceSupport() {
        return this.mHasFreeformWorkspaceSupport;
    }

    public boolean isInSafeMode() {
        return this.mIsSafeMode;
    }

    public boolean startTaskInDockedMode(int taskId, int createMode) {
        if (this.mIam == null) {
            return false;
        }
        try {
            ActivityOptions options = ActivityOptions.makeBasic();
            options.setDockCreateMode(createMode);
            options.setLaunchStackId(3);
            this.mIam.startActivityFromRecents(taskId, options.toBundle());
            return true;
        } catch (Exception e) {
            Log.e("SystemServicesProxy", "Failed to dock task: " + taskId + " with createMode: " + createMode, e);
            return false;
        }
    }

    public boolean moveTaskToDockedStack(int taskId, int createMode, Rect initialBounds) {
        if (this.mIam == null) {
            return false;
        }
        try {
            return this.mIam.moveTaskToDockedStack(taskId, createMode, true, false, initialBounds, true);
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isHomeStack(int stackId) {
        return stackId == 0;
    }

    public static boolean isFreeformStack(int stackId) {
        return stackId == 2;
    }

    public boolean hasDockedTask() {
        if (this.mIam == null) {
            return false;
        }
        ActivityManager.StackInfo stackInfo = null;
        try {
            stackInfo = this.mIam.getStackInfo(3);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        if (stackInfo == null) {
            return false;
        }
        int userId = getCurrentUser();
        boolean hasUserTask = false;
        for (int i = stackInfo.taskUserIds.length - 1; i >= 0 && !hasUserTask; i--) {
            hasUserTask = stackInfo.taskUserIds[i] == userId;
        }
        return hasUserTask;
    }

    public boolean hasSoftNavigationBar() {
        try {
            return WindowManagerGlobal.getWindowManagerService().hasNavigationBar();
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean hasTransposedNavigationBar() {
        Rect insets = new Rect();
        getStableInsets(insets);
        return insets.right > 0;
    }

    public void cancelWindowTransition(int taskId) {
        if (this.mWm == null) {
            return;
        }
        try {
            WindowManagerGlobal.getWindowManagerService().cancelTaskWindowTransition(taskId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void cancelThumbnailTransition(int taskId) {
        if (this.mWm == null) {
            return;
        }
        try {
            WindowManagerGlobal.getWindowManagerService().cancelTaskThumbnailTransition(taskId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public ThumbnailData getTaskThumbnail(int taskId) {
        if (this.mAm == null) {
            return null;
        }
        ThumbnailData thumbnailData = new ThumbnailData();
        getThumbnail(taskId, thumbnailData);
        if (thumbnailData.thumbnail != null) {
            thumbnailData.thumbnail.setHasAlpha(false);
            if (Color.alpha(thumbnailData.thumbnail.getPixel(0, 0)) == 0) {
                this.mBgProtectionCanvas.setBitmap(thumbnailData.thumbnail);
                this.mBgProtectionCanvas.drawRect(0.0f, 0.0f, thumbnailData.thumbnail.getWidth(), thumbnailData.thumbnail.getHeight(), this.mBgProtectionPaint);
                this.mBgProtectionCanvas.setBitmap(null);
                Log.e("SystemServicesProxy", "Invalid screenshot detected from getTaskThumbnail()");
            }
        }
        return thumbnailData;
    }

    public void getThumbnail(int taskId, ThumbnailData thumbnailDataOut) {
        ActivityManager.TaskThumbnail taskThumbnail;
        if (this.mAm == null || (taskThumbnail = this.mAm.getTaskThumbnail(taskId)) == null) {
            return;
        }
        Bitmap thumbnail = taskThumbnail.mainThumbnail;
        ParcelFileDescriptor descriptor = taskThumbnail.thumbnailFileDescriptor;
        if (thumbnail == null && descriptor != null) {
            thumbnail = BitmapFactory.decodeFileDescriptor(descriptor.getFileDescriptor(), null, sBitmapOptions);
        } else if (thumbnail != null && FeatureOptions.LOW_RAM_SUPPORT) {
            ByteArrayOutputStream tbOutput = new ByteArrayOutputStream();
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, tbOutput);
            ByteArrayInputStream tbInput = new ByteArrayInputStream(tbOutput.toByteArray());
            thumbnail = BitmapFactory.decodeStream(tbInput, null, sBitmapOptions);
            if (tbOutput != null) {
                try {
                    tbOutput.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (tbInput != null) {
                tbInput.close();
            }
        }
        if (descriptor != null) {
            try {
                descriptor.close();
            } catch (IOException e2) {
            }
        }
        thumbnailDataOut.thumbnail = thumbnail;
        thumbnailDataOut.thumbnailInfo = taskThumbnail.thumbnailInfo;
    }

    public void moveTaskToStack(int taskId, int stackId) {
        if (this.mIam == null) {
            return;
        }
        try {
            this.mIam.positionTaskInStack(taskId, stackId, 0);
        } catch (RemoteException | IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    public void removeTask(final int taskId) {
        if (this.mAm == null) {
            return;
        }
        BackgroundThread.getHandler().post(new Runnable() {
            @Override
            public void run() {
                SystemServicesProxy.this.mAm.removeTask(taskId);
            }
        });
    }

    public void sendCloseSystemWindows(String reason) {
        if (!ActivityManagerNative.isSystemReady()) {
            return;
        }
        try {
            this.mIam.closeSystemDialogs(reason);
        } catch (RemoteException e) {
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

    public String getBadgedActivityLabel(ActivityInfo info, int userId) {
        if (this.mPm == null) {
            return null;
        }
        return getBadgedLabel(info.loadLabel(this.mPm).toString(), userId);
    }

    public String getBadgedApplicationLabel(ApplicationInfo appInfo, int userId) {
        if (this.mPm == null) {
            return null;
        }
        return getBadgedLabel(appInfo.loadLabel(this.mPm).toString(), userId);
    }

    public String getBadgedContentDescription(ActivityInfo info, int userId, Resources res) {
        String activityLabel = info.loadLabel(this.mPm).toString();
        String applicationLabel = info.applicationInfo.loadLabel(this.mPm).toString();
        String badgedApplicationLabel = getBadgedLabel(applicationLabel, userId);
        return applicationLabel.equals(activityLabel) ? badgedApplicationLabel : res.getString(com.android.systemui.R.string.accessibility_recents_task_header, badgedApplicationLabel, activityLabel);
    }

    public Drawable getBadgedActivityIcon(ActivityInfo info, int userId) {
        if (this.mPm == null) {
            return null;
        }
        Drawable icon = info.loadIcon(this.mPm);
        return getBadgedIcon(icon, userId);
    }

    public Drawable getBadgedApplicationIcon(ApplicationInfo appInfo, int userId) {
        if (this.mPm == null) {
            return null;
        }
        Drawable icon = appInfo.loadIcon(this.mPm);
        return getBadgedIcon(icon, userId);
    }

    public Drawable getBadgedTaskDescriptionIcon(ActivityManager.TaskDescription taskDescription, int userId, Resources res) {
        Bitmap tdIcon = taskDescription.getInMemoryIcon();
        if (tdIcon == null) {
            tdIcon = ActivityManager.TaskDescription.loadTaskDescriptionIcon(taskDescription.getIconFilename(), userId);
        }
        if (tdIcon != null) {
            return getBadgedIcon(new BitmapDrawable(res, tdIcon), userId);
        }
        return null;
    }

    private Drawable getBadgedIcon(Drawable icon, int userId) {
        if (userId != UserHandle.myUserId()) {
            return this.mPm.getUserBadgedIcon(icon, new UserHandle(userId));
        }
        return icon;
    }

    private String getBadgedLabel(String label, int userId) {
        if (userId != UserHandle.myUserId()) {
            return this.mPm.getUserBadgedLabel(label, new UserHandle(userId)).toString();
        }
        return label;
    }

    public boolean isSystemUser(int userId) {
        return userId == 0;
    }

    public int getCurrentUser() {
        if (this.mAm == null) {
            return 0;
        }
        ActivityManager activityManager = this.mAm;
        return ActivityManager.getCurrentUser();
    }

    public int getProcessUser() {
        if (this.mUm == null) {
            return 0;
        }
        return this.mUm.getUserHandle();
    }

    public boolean isTouchExplorationEnabled() {
        if (this.mAccm != null && this.mAccm.isEnabled()) {
            return this.mAccm.isTouchExplorationEnabled();
        }
        return false;
    }

    public boolean isScreenPinningActive() {
        if (this.mIam == null) {
            return false;
        }
        try {
            return this.mIam.isInLockTaskMode();
        } catch (RemoteException e) {
            return false;
        }
    }

    public int getSystemSetting(Context context, String setting) {
        ContentResolver cr = context.getContentResolver();
        return Settings.System.getInt(cr, setting, 0);
    }

    public int getDeviceSmallestWidth() {
        if (this.mDisplay == null) {
            return 0;
        }
        Point smallestSizeRange = new Point();
        Point largestSizeRange = new Point();
        this.mDisplay.getCurrentSizeRange(smallestSizeRange, largestSizeRange);
        return smallestSizeRange.x;
    }

    public Rect getDisplayRect() {
        Rect displayRect = new Rect();
        if (this.mDisplay == null) {
            return displayRect;
        }
        Point p = new Point();
        this.mDisplay.getRealSize(p);
        displayRect.set(0, 0, p.x, p.y);
        return displayRect;
    }

    public Rect getWindowRect() {
        Rect windowRect = new Rect();
        if (this.mIam == null) {
            return windowRect;
        }
        try {
            try {
                ActivityManager.StackInfo stackInfo = this.mIam.getStackInfo(0);
                if (stackInfo != null) {
                    windowRect.set(stackInfo.bounds);
                }
                return windowRect;
            } catch (RemoteException e) {
                e.printStackTrace();
                return windowRect;
            }
        } catch (Throwable th) {
            return windowRect;
        }
    }

    public boolean startActivityFromRecents(Context context, Task.TaskKey taskKey, String taskName, ActivityOptions options) {
        if (this.mIam != null) {
            try {
                if (taskKey.stackId == 3) {
                    if (options == null) {
                        options = ActivityOptions.makeBasic();
                    }
                    options.setLaunchStackId(1);
                }
                this.mIam.startActivityFromRecents(taskKey.id, options != null ? options.toBundle() : null);
                return true;
            } catch (Exception e) {
                Log.e("SystemServicesProxy", context.getString(com.android.systemui.R.string.recents_launch_error_message, taskName), e);
            }
        }
        return false;
    }

    public void startInPlaceAnimationOnFrontMostApplication(ActivityOptions opts) {
        if (this.mIam == null) {
            return;
        }
        try {
            this.mIam.startInPlaceAnimationOnFrontMostApplication(opts);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void registerTaskStackListener(TaskStackListener listener) {
        if (this.mIam == null) {
            return;
        }
        this.mTaskStackListeners.add(listener);
        if (this.mTaskStackListeners.size() != 1) {
            return;
        }
        try {
            this.mIam.registerTaskStackListener(this.mTaskStackListener);
        } catch (Exception e) {
            Log.w("SystemServicesProxy", "Failed to call registerTaskStackListener", e);
        }
    }

    public void endProlongedAnimations() {
        if (this.mWm == null) {
            return;
        }
        try {
            WindowManagerGlobal.getWindowManagerService().endProlongedAnimations();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void registerDockedStackListener(IDockedStackListener listener) {
        if (this.mWm == null) {
            return;
        }
        try {
            WindowManagerGlobal.getWindowManagerService().registerDockedStackListener(listener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getDockedDividerSize(Context context) {
        Resources res = context.getResources();
        int dividerWindowWidth = res.getDimensionPixelSize(R.dimen.action_bar_elevation_material);
        int dividerInsets = res.getDimensionPixelSize(R.dimen.action_bar_icon_vertical_padding);
        return dividerWindowWidth - (dividerInsets * 2);
    }

    public void requestKeyboardShortcuts(Context context, WindowManager.KeyboardShortcutsReceiver receiver, int deviceId) {
        this.mWm.requestAppKeyboardShortcuts(receiver, deviceId);
    }

    public void getStableInsets(Rect outStableInsets) {
        if (this.mWm == null) {
            return;
        }
        try {
            WindowManagerGlobal.getWindowManagerService().getStableInsets(outStableInsets);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void overridePendingAppTransitionMultiThumbFuture(IAppTransitionAnimationSpecsFuture future, IRemoteCallback animStartedListener, boolean scaleUp) {
        try {
            WindowManagerGlobal.getWindowManagerService().overridePendingAppTransitionMultiThumbFuture(future, animStartedListener, scaleUp);
        } catch (RemoteException e) {
            Log.w("SystemServicesProxy", "Failed to override transition: " + e);
        }
    }

    private final class H extends Handler {
        H(SystemServicesProxy this$0, H h) {
            this();
        }

        private H() {
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    for (int i = SystemServicesProxy.this.mTaskStackListeners.size() - 1; i >= 0; i--) {
                        ((TaskStackListener) SystemServicesProxy.this.mTaskStackListeners.get(i)).onTaskStackChanged();
                    }
                    break;
                case 2:
                    for (int i2 = SystemServicesProxy.this.mTaskStackListeners.size() - 1; i2 >= 0; i2--) {
                        ((TaskStackListener) SystemServicesProxy.this.mTaskStackListeners.get(i2)).onActivityPinned();
                    }
                    break;
                case 3:
                    for (int i3 = SystemServicesProxy.this.mTaskStackListeners.size() - 1; i3 >= 0; i3--) {
                        ((TaskStackListener) SystemServicesProxy.this.mTaskStackListeners.get(i3)).onPinnedActivityRestartAttempt();
                    }
                    break;
                case 4:
                    for (int i4 = SystemServicesProxy.this.mTaskStackListeners.size() - 1; i4 >= 0; i4--) {
                        ((TaskStackListener) SystemServicesProxy.this.mTaskStackListeners.get(i4)).onPinnedStackAnimationEnded();
                    }
                    break;
                case 5:
                    for (int i5 = SystemServicesProxy.this.mTaskStackListeners.size() - 1; i5 >= 0; i5--) {
                        ((TaskStackListener) SystemServicesProxy.this.mTaskStackListeners.get(i5)).onActivityForcedResizable((String) msg.obj, msg.arg1);
                    }
                    break;
                case 6:
                    for (int i6 = SystemServicesProxy.this.mTaskStackListeners.size() - 1; i6 >= 0; i6--) {
                        ((TaskStackListener) SystemServicesProxy.this.mTaskStackListeners.get(i6)).onActivityDismissingDockedStack();
                    }
                    break;
            }
        }
    }

    public void restoreWindow() {
        Log.d("SystemServicesProxy", "restoreWindow");
        if (this.mIam == null) {
            return;
        }
        try {
            this.mIam.restoreWindow();
        } catch (Exception e) {
            Log.e("SystemServicesProxy", "restoreWindow", e);
        }
    }
}
