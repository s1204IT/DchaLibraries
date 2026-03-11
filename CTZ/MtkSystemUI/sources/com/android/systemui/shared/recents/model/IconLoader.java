package com.android.systemui.shared.recents.model;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.util.IconDrawableFactory;
import android.util.Log;
import android.util.LruCache;
import com.android.systemui.shared.recents.model.Task;
import com.android.systemui.shared.system.PackageManagerWrapper;

public abstract class IconLoader {
    protected final LruCache<ComponentName, ActivityInfo> mActivityInfoCache;
    protected final Context mContext;
    protected final TaskKeyLruCache<Drawable> mIconCache;

    protected abstract Drawable createBadgedDrawable(Drawable drawable, int i, ActivityManager.TaskDescription taskDescription);

    protected abstract Drawable getBadgedActivityIcon(ActivityInfo activityInfo, int i, ActivityManager.TaskDescription taskDescription);

    public abstract Drawable getDefaultIcon(int i);

    public IconLoader(Context context, TaskKeyLruCache<Drawable> taskKeyLruCache, LruCache<ComponentName, ActivityInfo> lruCache) {
        this.mContext = context;
        this.mIconCache = taskKeyLruCache;
        this.mActivityInfoCache = lruCache;
    }

    public ActivityInfo getAndUpdateActivityInfo(Task.TaskKey taskKey) {
        ComponentName component = taskKey.getComponent();
        ActivityInfo activityInfo = this.mActivityInfoCache.get(component);
        if (activityInfo == null) {
            activityInfo = PackageManagerWrapper.getInstance().getActivityInfo(component, taskKey.userId);
            if (component == null || activityInfo == null) {
                Log.e("IconLoader", "Unexpected null component name or activity info: " + component + ", " + activityInfo);
                return null;
            }
            this.mActivityInfoCache.put(component, activityInfo);
        }
        return activityInfo;
    }

    public Drawable getIcon(Task task) {
        Drawable drawable = this.mIconCache.get(task.key);
        if (drawable == null) {
            Drawable drawableCreateNewIconForTask = createNewIconForTask(task.key, task.taskDescription, true);
            this.mIconCache.put(task.key, drawableCreateNewIconForTask);
            return drawableCreateNewIconForTask;
        }
        return drawable;
    }

    public Drawable getAndInvalidateIfModified(Task.TaskKey taskKey, ActivityManager.TaskDescription taskDescription, boolean z) {
        Drawable drawableCreateNewIconForTask;
        Drawable andInvalidateIfModified = this.mIconCache.getAndInvalidateIfModified(taskKey);
        if (andInvalidateIfModified != null) {
            return andInvalidateIfModified;
        }
        if (z && (drawableCreateNewIconForTask = createNewIconForTask(taskKey, taskDescription, false)) != null) {
            this.mIconCache.put(taskKey, drawableCreateNewIconForTask);
            return drawableCreateNewIconForTask;
        }
        return null;
    }

    private Drawable createNewIconForTask(Task.TaskKey taskKey, ActivityManager.TaskDescription taskDescription, boolean z) {
        Drawable badgedActivityIcon;
        int i = taskKey.userId;
        Bitmap inMemoryIcon = taskDescription.getInMemoryIcon();
        if (inMemoryIcon != null) {
            return createDrawableFromBitmap(inMemoryIcon, i, taskDescription);
        }
        if (taskDescription.getIconResource() != 0) {
            try {
                PackageManager packageManager = this.mContext.getPackageManager();
                return createBadgedDrawable(packageManager.getResourcesForApplication(packageManager.getApplicationInfo(taskKey.getPackageName(), 4194304)).getDrawable(taskDescription.getIconResource(), null), i, taskDescription);
            } catch (PackageManager.NameNotFoundException | Resources.NotFoundException e) {
                Log.e("IconLoader", "Could not find icon drawable from resource", e);
            }
        }
        Bitmap bitmapLoadTaskDescriptionIcon = ActivityManager.TaskDescription.loadTaskDescriptionIcon(taskDescription.getIconFilename(), i);
        if (bitmapLoadTaskDescriptionIcon != null) {
            return createDrawableFromBitmap(bitmapLoadTaskDescriptionIcon, i, taskDescription);
        }
        ActivityInfo andUpdateActivityInfo = getAndUpdateActivityInfo(taskKey);
        if (andUpdateActivityInfo != null && (badgedActivityIcon = getBadgedActivityIcon(andUpdateActivityInfo, i, taskDescription)) != null) {
            return badgedActivityIcon;
        }
        if (z) {
            return getDefaultIcon(i);
        }
        return null;
    }

    protected Drawable createDrawableFromBitmap(Bitmap bitmap, int i, ActivityManager.TaskDescription taskDescription) {
        return createBadgedDrawable(new BitmapDrawable(this.mContext.getResources(), bitmap), i, taskDescription);
    }

    public static class DefaultIconLoader extends IconLoader {
        private final BitmapDrawable mDefaultIcon;
        private final IconDrawableFactory mDrawableFactory;

        public DefaultIconLoader(Context context, TaskKeyLruCache<Drawable> taskKeyLruCache, LruCache<ComponentName, ActivityInfo> lruCache) {
            super(context, taskKeyLruCache, lruCache);
            Bitmap bitmapCreateBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8);
            bitmapCreateBitmap.eraseColor(0);
            this.mDefaultIcon = new BitmapDrawable(context.getResources(), bitmapCreateBitmap);
            this.mDrawableFactory = IconDrawableFactory.newInstance(context);
        }

        @Override
        public Drawable getDefaultIcon(int i) {
            return this.mDefaultIcon;
        }

        @Override
        protected Drawable createBadgedDrawable(Drawable drawable, int i, ActivityManager.TaskDescription taskDescription) {
            if (i != UserHandle.myUserId()) {
                return this.mContext.getPackageManager().getUserBadgedIcon(drawable, new UserHandle(i));
            }
            return drawable;
        }

        @Override
        protected Drawable getBadgedActivityIcon(ActivityInfo activityInfo, int i, ActivityManager.TaskDescription taskDescription) {
            return this.mDrawableFactory.getBadgedIcon(activityInfo, activityInfo.applicationInfo, i);
        }
    }
}
