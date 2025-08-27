package com.android.systemui.shared.recents.model;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
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

/* loaded from: classes.dex */
public abstract class IconLoader {
    private static final String TAG = "IconLoader";
    protected final LruCache<ComponentName, ActivityInfo> mActivityInfoCache;
    protected final Context mContext;
    protected final TaskKeyLruCache<Drawable> mIconCache;

    protected abstract Drawable createBadgedDrawable(Drawable drawable, int i, ActivityManager.TaskDescription taskDescription);

    protected abstract Drawable getBadgedActivityIcon(ActivityInfo activityInfo, int i, ActivityManager.TaskDescription taskDescription);

    public abstract Drawable getDefaultIcon(int i);

    public IconLoader(Context context, TaskKeyLruCache<Drawable> iconCache, LruCache<ComponentName, ActivityInfo> activityInfoCache) {
        this.mContext = context;
        this.mIconCache = iconCache;
        this.mActivityInfoCache = activityInfoCache;
    }

    public ActivityInfo getAndUpdateActivityInfo(Task.TaskKey taskKey) {
        ComponentName cn = taskKey.getComponent();
        ActivityInfo activityInfo = this.mActivityInfoCache.get(cn);
        if (activityInfo == null) {
            activityInfo = PackageManagerWrapper.getInstance().getActivityInfo(cn, taskKey.userId);
            if (cn == null || activityInfo == null) {
                Log.e(TAG, "Unexpected null component name or activity info: " + cn + ", " + activityInfo);
                return null;
            }
            this.mActivityInfoCache.put(cn, activityInfo);
        }
        return activityInfo;
    }

    public Drawable getIcon(Task t) throws PackageManager.NameNotFoundException {
        Drawable cachedIcon = this.mIconCache.get(t.key);
        if (cachedIcon == null) {
            Drawable cachedIcon2 = createNewIconForTask(t.key, t.taskDescription, true);
            this.mIconCache.put(t.key, cachedIcon2);
            return cachedIcon2;
        }
        return cachedIcon;
    }

    public Drawable getAndInvalidateIfModified(Task.TaskKey taskKey, ActivityManager.TaskDescription td, boolean loadIfNotCached) {
        Drawable icon;
        Drawable icon2 = this.mIconCache.getAndInvalidateIfModified(taskKey);
        if (icon2 != null) {
            return icon2;
        }
        if (loadIfNotCached && (icon = createNewIconForTask(taskKey, td, false)) != null) {
            this.mIconCache.put(taskKey, icon);
            return icon;
        }
        return null;
    }

    private Drawable createNewIconForTask(Task.TaskKey taskKey, ActivityManager.TaskDescription desc, boolean returnDefault) throws PackageManager.NameNotFoundException {
        Drawable icon;
        int userId = taskKey.userId;
        Bitmap tdIcon = desc.getInMemoryIcon();
        if (tdIcon != null) {
            return createDrawableFromBitmap(tdIcon, userId, desc);
        }
        if (desc.getIconResource() != 0) {
            try {
                PackageManager pm = this.mContext.getPackageManager();
                ApplicationInfo appInfo = pm.getApplicationInfo(taskKey.getPackageName(), 4194304);
                Resources res = pm.getResourcesForApplication(appInfo);
                return createBadgedDrawable(res.getDrawable(desc.getIconResource(), null), userId, desc);
            } catch (PackageManager.NameNotFoundException | Resources.NotFoundException e) {
                Log.e(TAG, "Could not find icon drawable from resource", e);
            }
        }
        Bitmap tdIcon2 = ActivityManager.TaskDescription.loadTaskDescriptionIcon(desc.getIconFilename(), userId);
        if (tdIcon2 != null) {
            return createDrawableFromBitmap(tdIcon2, userId, desc);
        }
        ActivityInfo activityInfo = getAndUpdateActivityInfo(taskKey);
        if (activityInfo != null && (icon = getBadgedActivityIcon(activityInfo, userId, desc)) != null) {
            return icon;
        }
        if (returnDefault) {
            return getDefaultIcon(userId);
        }
        return null;
    }

    protected Drawable createDrawableFromBitmap(Bitmap icon, int userId, ActivityManager.TaskDescription desc) {
        return createBadgedDrawable(new BitmapDrawable(this.mContext.getResources(), icon), userId, desc);
    }

    public static class DefaultIconLoader extends IconLoader {
        private final BitmapDrawable mDefaultIcon;
        private final IconDrawableFactory mDrawableFactory;

        public DefaultIconLoader(Context context, TaskKeyLruCache<Drawable> iconCache, LruCache<ComponentName, ActivityInfo> activityInfoCache) {
            super(context, iconCache, activityInfoCache);
            Bitmap icon = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8);
            icon.eraseColor(0);
            this.mDefaultIcon = new BitmapDrawable(context.getResources(), icon);
            this.mDrawableFactory = IconDrawableFactory.newInstance(context);
        }

        @Override // com.android.systemui.shared.recents.model.IconLoader
        public Drawable getDefaultIcon(int userId) {
            return this.mDefaultIcon;
        }

        @Override // com.android.systemui.shared.recents.model.IconLoader
        protected Drawable createBadgedDrawable(Drawable icon, int userId, ActivityManager.TaskDescription desc) {
            if (userId != UserHandle.myUserId()) {
                return this.mContext.getPackageManager().getUserBadgedIcon(icon, new UserHandle(userId));
            }
            return icon;
        }

        @Override // com.android.systemui.shared.recents.model.IconLoader
        protected Drawable getBadgedActivityIcon(ActivityInfo info, int userId, ActivityManager.TaskDescription desc) {
            return this.mDrawableFactory.getBadgedIcon(info, info.applicationInfo, userId);
        }
    }
}
