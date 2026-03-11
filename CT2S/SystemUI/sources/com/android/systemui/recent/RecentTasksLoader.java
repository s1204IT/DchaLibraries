package com.android.systemui.recent;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import com.android.systemui.R;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class RecentTasksLoader implements View.OnTouchListener {
    static final boolean DEBUG;
    private static RecentTasksLoader sInstance;
    boolean mCancelPreloadingFirstTask;
    private Context mContext;
    private ColorDrawableWithDimensions mDefaultIconBackground;
    private ColorDrawableWithDimensions mDefaultThumbnailBackground;
    private boolean mFirstScreenful;
    private TaskDescription mFirstTask;
    private boolean mFirstTaskLoaded;
    private int mIconDpi;
    private ArrayList<TaskDescription> mLoadedTasks;
    boolean mPreloadingFirstTask;
    private RecentsPanelView mRecentsPanel;
    private AsyncTask<Void, ArrayList<TaskDescription>, Void> mTaskLoader;
    private AsyncTask<Void, TaskDescription, Void> mThumbnailLoader;
    private Object mFirstTaskLock = new Object();
    private int mNumTasksInFirstScreenful = Integer.MAX_VALUE;
    private State mState = State.CANCELLED;
    Runnable mPreloadTasksRunnable = new Runnable() {
        @Override
        public void run() {
            RecentTasksLoader.this.loadTasksInBackground();
        }
    };
    private Handler mHandler = new Handler();

    private enum State {
        LOADING,
        LOADED,
        CANCELLED
    }

    static {
        DEBUG = PhoneStatusBar.DEBUG;
    }

    public static RecentTasksLoader getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new RecentTasksLoader(context);
        }
        return sInstance;
    }

    private RecentTasksLoader(Context context) {
        this.mContext = context;
        Resources res = context.getResources();
        boolean isTablet = res.getBoolean(R.bool.config_recents_interface_for_tablets);
        if (isTablet) {
            ActivityManager activityManager = (ActivityManager) context.getSystemService("activity");
            this.mIconDpi = activityManager.getLauncherLargeIconDensity();
        } else {
            this.mIconDpi = res.getDisplayMetrics().densityDpi;
        }
        int defaultIconSize = res.getDimensionPixelSize(android.R.dimen.app_icon_size);
        int iconSize = (this.mIconDpi * defaultIconSize) / res.getDisplayMetrics().densityDpi;
        this.mDefaultIconBackground = new ColorDrawableWithDimensions(0, iconSize, iconSize);
        int thumbnailWidth = res.getDimensionPixelSize(android.R.dimen.thumbnail_width);
        int thumbnailHeight = res.getDimensionPixelSize(android.R.dimen.thumbnail_height);
        int color = res.getColor(R.drawable.status_bar_recents_app_thumbnail_background);
        this.mDefaultThumbnailBackground = new ColorDrawableWithDimensions(color, thumbnailWidth, thumbnailHeight);
    }

    public void setRecentsPanel(RecentsPanelView newRecentsPanel, RecentsPanelView caller) {
        if (newRecentsPanel != null || this.mRecentsPanel == caller) {
            this.mRecentsPanel = newRecentsPanel;
            if (this.mRecentsPanel != null) {
                this.mNumTasksInFirstScreenful = this.mRecentsPanel.numItemsInOneScreenful();
            }
        }
    }

    public Drawable getDefaultThumbnail() {
        return this.mDefaultThumbnailBackground;
    }

    public Drawable getDefaultIcon() {
        return this.mDefaultIconBackground;
    }

    public ArrayList<TaskDescription> getLoadedTasks() {
        return this.mLoadedTasks;
    }

    public void remove(TaskDescription td) {
        this.mLoadedTasks.remove(td);
    }

    public boolean isFirstScreenful() {
        return this.mFirstScreenful;
    }

    public boolean isCurrentHomeActivity(ComponentName component, ActivityInfo homeInfo) {
        if (homeInfo == null) {
            PackageManager pm = this.mContext.getPackageManager();
            homeInfo = new Intent("android.intent.action.MAIN").addCategory("android.intent.category.HOME").resolveActivityInfo(pm, 0);
        }
        return homeInfo != null && homeInfo.packageName.equals(component.getPackageName()) && homeInfo.name.equals(component.getClassName());
    }

    TaskDescription createTaskDescription(int taskId, int persistentTaskId, Intent baseIntent, ComponentName origActivity, CharSequence description, int userId) {
        Intent intent = new Intent(baseIntent);
        if (origActivity != null) {
            intent.setComponent(origActivity);
        }
        PackageManager pm = this.mContext.getPackageManager();
        IPackageManager ipm = AppGlobals.getPackageManager();
        intent.setFlags((intent.getFlags() & (-2097153)) | 268435456);
        ResolveInfo resolveInfo = null;
        try {
            resolveInfo = ipm.resolveIntent(intent, (String) null, 0, userId);
        } catch (RemoteException e) {
        }
        if (resolveInfo != null) {
            ActivityInfo info = resolveInfo.activityInfo;
            String title = info.loadLabel(pm).toString();
            if (title != null && title.length() > 0) {
                if (DEBUG) {
                    Log.v("RecentTasksLoader", "creating activity desc for id=" + persistentTaskId + ", label=" + title);
                }
                TaskDescription item = new TaskDescription(taskId, persistentTaskId, resolveInfo, baseIntent, info.packageName, description, userId);
                item.setLabel(title);
                return item;
            }
            if (DEBUG) {
                Log.v("RecentTasksLoader", "SKIPPING item " + persistentTaskId);
            }
        }
        return null;
    }

    void loadThumbnailAndIcon(TaskDescription td) {
        ActivityManager am = (ActivityManager) this.mContext.getSystemService("activity");
        PackageManager pm = this.mContext.getPackageManager();
        Bitmap thumbnail = SystemServicesProxy.getThumbnail(am, td.persistentTaskId);
        Drawable icon = getFullResIcon(td.resolveInfo, pm);
        if (td.userId != UserHandle.myUserId()) {
            icon = this.mContext.getPackageManager().getUserBadgedIcon(icon, new UserHandle(td.userId));
        }
        if (DEBUG) {
            Log.v("RecentTasksLoader", "Loaded bitmap for task " + td + ": " + thumbnail);
        }
        synchronized (td) {
            if (thumbnail != null) {
                td.setThumbnail(new BitmapDrawable(this.mContext.getResources(), thumbnail));
            } else {
                td.setThumbnail(this.mDefaultThumbnailBackground);
            }
            if (icon != null) {
                td.setIcon(icon);
            }
            td.setLoaded(true);
        }
    }

    Drawable getFullResDefaultActivityIcon() {
        return getFullResIcon(Resources.getSystem(), android.R.mipmap.sym_def_app_icon);
    }

    Drawable getFullResIcon(Resources resources, int iconId) {
        try {
            return resources.getDrawableForDensity(iconId, this.mIconDpi);
        } catch (Resources.NotFoundException e) {
            return getFullResDefaultActivityIcon();
        }
    }

    private Drawable getFullResIcon(ResolveInfo info, PackageManager packageManager) {
        Resources resources;
        int iconId;
        try {
            resources = packageManager.getResourcesForApplication(info.activityInfo.applicationInfo);
        } catch (PackageManager.NameNotFoundException e) {
            resources = null;
        }
        if (resources != null && (iconId = info.activityInfo.getIconResource()) != 0) {
            return getFullResIcon(resources, iconId);
        }
        return getFullResDefaultActivityIcon();
    }

    @Override
    public boolean onTouch(View v, MotionEvent ev) {
        int action = ev.getAction() & 255;
        if (action == 0) {
            preloadRecentTasksList();
            return false;
        }
        if (action == 3) {
            cancelPreloadingRecentTasksList();
            return false;
        }
        if (action == 1) {
            this.mHandler.removeCallbacks(this.mPreloadTasksRunnable);
            if (!v.isPressed()) {
                cancelLoadingThumbnailsAndIcons();
                return false;
            }
            return false;
        }
        return false;
    }

    public void preloadRecentTasksList() {
        this.mHandler.post(this.mPreloadTasksRunnable);
    }

    public void cancelPreloadingRecentTasksList() {
        cancelLoadingThumbnailsAndIcons();
        this.mHandler.removeCallbacks(this.mPreloadTasksRunnable);
    }

    public void cancelLoadingThumbnailsAndIcons(RecentsPanelView caller) {
        if (this.mRecentsPanel == caller) {
            cancelLoadingThumbnailsAndIcons();
        }
    }

    private void cancelLoadingThumbnailsAndIcons() {
        if (this.mRecentsPanel == null || !this.mRecentsPanel.isShowing()) {
            if (this.mTaskLoader != null) {
                this.mTaskLoader.cancel(false);
                this.mTaskLoader = null;
            }
            if (this.mThumbnailLoader != null) {
                this.mThumbnailLoader.cancel(false);
                this.mThumbnailLoader = null;
            }
            this.mLoadedTasks = null;
            if (this.mRecentsPanel != null) {
                this.mRecentsPanel.onTaskLoadingCancelled();
            }
            this.mFirstScreenful = false;
            this.mState = State.CANCELLED;
        }
    }

    public void clearFirstTask() {
        synchronized (this.mFirstTaskLock) {
            this.mFirstTask = null;
            this.mFirstTaskLoaded = false;
        }
    }

    public void preloadFirstTask() {
        Thread bgLoad = new Thread() {
            @Override
            public void run() {
                TaskDescription first = RecentTasksLoader.this.loadFirstTask();
                synchronized (RecentTasksLoader.this.mFirstTaskLock) {
                    if (RecentTasksLoader.this.mCancelPreloadingFirstTask) {
                        RecentTasksLoader.this.clearFirstTask();
                    } else {
                        RecentTasksLoader.this.mFirstTask = first;
                        RecentTasksLoader.this.mFirstTaskLoaded = true;
                    }
                    RecentTasksLoader.this.mPreloadingFirstTask = false;
                }
            }
        };
        synchronized (this.mFirstTaskLock) {
            if (!this.mPreloadingFirstTask) {
                clearFirstTask();
                this.mPreloadingFirstTask = true;
                bgLoad.start();
            }
        }
    }

    public void cancelPreloadingFirstTask() {
        synchronized (this.mFirstTaskLock) {
            if (this.mPreloadingFirstTask) {
                this.mCancelPreloadingFirstTask = true;
            } else {
                clearFirstTask();
            }
        }
    }

    public TaskDescription getFirstTask() {
        TaskDescription taskDescription;
        while (true) {
            synchronized (this.mFirstTaskLock) {
                if (this.mFirstTaskLoaded) {
                    taskDescription = this.mFirstTask;
                } else if (!this.mFirstTaskLoaded && !this.mPreloadingFirstTask) {
                    this.mFirstTask = loadFirstTask();
                    this.mFirstTaskLoaded = true;
                    taskDescription = this.mFirstTask;
                }
            }
        }
        return taskDescription;
    }

    public TaskDescription loadFirstTask() {
        ActivityManager am = (ActivityManager) this.mContext.getSystemService("activity");
        List<ActivityManager.RecentTaskInfo> recentTasks = am.getRecentTasksForUser(1, 6, UserHandle.CURRENT.getIdentifier());
        if (recentTasks.size() <= 0) {
            return null;
        }
        ActivityManager.RecentTaskInfo recentInfo = recentTasks.get(0);
        Intent intent = new Intent(recentInfo.baseIntent);
        if (recentInfo.origActivity != null) {
            intent.setComponent(recentInfo.origActivity);
        }
        if (isCurrentHomeActivity(intent.getComponent(), null) || intent.getComponent().getPackageName().equals(this.mContext.getPackageName())) {
            return null;
        }
        TaskDescription item = createTaskDescription(recentInfo.id, recentInfo.persistentId, recentInfo.baseIntent, recentInfo.origActivity, recentInfo.description, recentInfo.userId);
        if (item != null) {
            loadThumbnailAndIcon(item);
        }
        return item;
    }

    public void loadTasksInBackground() {
        loadTasksInBackground(false);
    }

    public void loadTasksInBackground(boolean zeroeth) {
        if (this.mState == State.CANCELLED) {
            this.mState = State.LOADING;
            this.mFirstScreenful = true;
            final LinkedBlockingQueue<TaskDescription> tasksWaitingForThumbnails = new LinkedBlockingQueue<>();
            this.mTaskLoader = new AsyncTask<Void, ArrayList<TaskDescription>, Void>() {
                @Override
                public void onProgressUpdate(ArrayList<TaskDescription>... values) {
                    if (!isCancelled()) {
                        ArrayList<TaskDescription> newTasks = values[0];
                        if (RecentTasksLoader.this.mRecentsPanel != null) {
                            RecentTasksLoader.this.mRecentsPanel.onTasksLoaded(newTasks, RecentTasksLoader.this.mFirstScreenful);
                        }
                        if (RecentTasksLoader.this.mLoadedTasks == null) {
                            RecentTasksLoader.this.mLoadedTasks = new ArrayList();
                        }
                        RecentTasksLoader.this.mLoadedTasks.addAll(newTasks);
                        RecentTasksLoader.this.mFirstScreenful = false;
                    }
                }

                @Override
                public Void doInBackground(Void... params) {
                    TaskDescription item;
                    int origPri = Process.getThreadPriority(Process.myTid());
                    Process.setThreadPriority(10);
                    PackageManager pm = RecentTasksLoader.this.mContext.getPackageManager();
                    ActivityManager am = (ActivityManager) RecentTasksLoader.this.mContext.getSystemService("activity");
                    List<ActivityManager.RecentTaskInfo> recentTasks = am.getRecentTasks(21, 6);
                    int numTasks = recentTasks.size();
                    ActivityInfo homeInfo = new Intent("android.intent.action.MAIN").addCategory("android.intent.category.HOME").resolveActivityInfo(pm, 0);
                    boolean firstScreenful = true;
                    ArrayList<TaskDescription> tasks = new ArrayList<>();
                    int index = 0;
                    for (int i = 0; i < numTasks && index < 21 && !isCancelled(); i++) {
                        ActivityManager.RecentTaskInfo recentInfo = recentTasks.get(i);
                        Intent intent = new Intent(recentInfo.baseIntent);
                        if (recentInfo.origActivity != null) {
                            intent.setComponent(recentInfo.origActivity);
                        }
                        if (!RecentTasksLoader.this.isCurrentHomeActivity(intent.getComponent(), homeInfo) && !intent.getComponent().getPackageName().equals(RecentTasksLoader.this.mContext.getPackageName()) && (item = RecentTasksLoader.this.createTaskDescription(recentInfo.id, recentInfo.persistentId, recentInfo.baseIntent, recentInfo.origActivity, recentInfo.description, recentInfo.userId)) != null) {
                            while (true) {
                                try {
                                    tasksWaitingForThumbnails.put(item);
                                    break;
                                } catch (InterruptedException e) {
                                }
                            }
                            tasks.add(item);
                            if (firstScreenful && tasks.size() == RecentTasksLoader.this.mNumTasksInFirstScreenful) {
                                publishProgress(tasks);
                                tasks = new ArrayList<>();
                                firstScreenful = false;
                            }
                            index++;
                        }
                    }
                    if (!isCancelled()) {
                        publishProgress(tasks);
                        if (firstScreenful) {
                            publishProgress(new ArrayList());
                        }
                    }
                    while (true) {
                        try {
                            tasksWaitingForThumbnails.put(new TaskDescription());
                            Process.setThreadPriority(origPri);
                            return null;
                        } catch (InterruptedException e2) {
                        }
                    }
                }
            };
            this.mTaskLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
            loadThumbnailsAndIconsInBackground(tasksWaitingForThumbnails);
        }
    }

    private void loadThumbnailsAndIconsInBackground(final BlockingQueue<TaskDescription> tasksWaitingForThumbnails) {
        this.mThumbnailLoader = new AsyncTask<Void, TaskDescription, Void>() {
            @Override
            public void onProgressUpdate(TaskDescription... values) {
                if (!isCancelled()) {
                    TaskDescription td = values[0];
                    if (!td.isNull()) {
                        if (RecentTasksLoader.this.mRecentsPanel != null) {
                            RecentTasksLoader.this.mRecentsPanel.onTaskThumbnailLoaded(td);
                        }
                    } else {
                        RecentTasksLoader.this.mState = State.LOADED;
                    }
                }
            }

            @Override
            public Void doInBackground(Void... params) {
                int origPri = Process.getThreadPriority(Process.myTid());
                Process.setThreadPriority(10);
                while (true) {
                    if (isCancelled()) {
                        break;
                    }
                    TaskDescription td = null;
                    while (td == null) {
                        try {
                            td = (TaskDescription) tasksWaitingForThumbnails.take();
                        } catch (InterruptedException e) {
                        }
                    }
                    if (td.isNull()) {
                        publishProgress(td);
                        break;
                    }
                    RecentTasksLoader.this.loadThumbnailAndIcon(td);
                    publishProgress(td);
                }
                Process.setThreadPriority(origPri);
                return null;
            }
        };
        this.mThumbnailLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }
}
