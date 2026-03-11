package com.android.launcher2;

import android.R;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Process;
import android.os.UserHandle;
import java.util.HashMap;

public class IconCache {
    private final HashMap<CacheKey, CacheEntry> mCache = new HashMap<>(50);
    private final LauncherApplication mContext;
    private final Bitmap mDefaultIcon;
    private int mIconDpi;
    private final PackageManager mPackageManager;

    private static class CacheEntry {
        public CharSequence contentDescription;
        public Bitmap icon;
        public String title;

        private CacheEntry() {
        }
    }

    private static class CacheKey {
        public ComponentName componentName;
        public UserHandle user;

        CacheKey(ComponentName componentName, UserHandle user) {
            this.componentName = componentName;
            this.user = user;
        }

        public int hashCode() {
            return this.componentName.hashCode() + this.user.hashCode();
        }

        public boolean equals(Object o) {
            CacheKey other = (CacheKey) o;
            return other.componentName.equals(this.componentName) && other.user.equals(this.user);
        }
    }

    public IconCache(LauncherApplication context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService("activity");
        this.mContext = context;
        this.mPackageManager = context.getPackageManager();
        this.mIconDpi = activityManager.getLauncherLargeIconDensity();
        this.mDefaultIcon = makeDefaultIcon();
    }

    public Drawable getFullResDefaultActivityIcon() {
        return getFullResIcon(Resources.getSystem(), R.mipmap.sym_def_app_icon, Process.myUserHandle());
    }

    public Drawable getFullResIcon(Resources resources, int iconId, UserHandle user) {
        Drawable d;
        try {
            d = resources.getDrawableForDensity(iconId, this.mIconDpi);
        } catch (Resources.NotFoundException e) {
            d = null;
        }
        if (d == null) {
            d = getFullResDefaultActivityIcon();
        }
        return this.mPackageManager.getUserBadgedIcon(d, user);
    }

    public Drawable getFullResIcon(String packageName, int iconId, UserHandle user) {
        Resources resources;
        try {
            resources = this.mPackageManager.getResourcesForApplication(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            resources = null;
        }
        if (resources != null && iconId != 0) {
            return getFullResIcon(resources, iconId, user);
        }
        return getFullResDefaultActivityIcon();
    }

    public Drawable getFullResIcon(ResolveInfo info, UserHandle user) {
        return getFullResIcon(info.activityInfo, user);
    }

    public Drawable getFullResIcon(ActivityInfo info, UserHandle user) {
        Resources resources;
        int iconId;
        try {
            resources = this.mPackageManager.getResourcesForApplication(info.applicationInfo);
        } catch (PackageManager.NameNotFoundException e) {
            resources = null;
        }
        if (resources != null && (iconId = info.getIconResource()) != 0) {
            return getFullResIcon(resources, iconId, user);
        }
        return getFullResDefaultActivityIcon();
    }

    private Bitmap makeDefaultIcon() {
        Drawable d = getFullResDefaultActivityIcon();
        Bitmap b = Bitmap.createBitmap(Math.max(d.getIntrinsicWidth(), 1), Math.max(d.getIntrinsicHeight(), 1), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        d.setBounds(0, 0, b.getWidth(), b.getHeight());
        d.draw(c);
        c.setBitmap(null);
        return b;
    }

    public void remove(ComponentName componentName) {
        synchronized (this.mCache) {
            this.mCache.remove(componentName);
        }
    }

    public void flush() {
        synchronized (this.mCache) {
            this.mCache.clear();
        }
    }

    public void getTitleAndIcon(ApplicationInfo application, LauncherActivityInfo info, HashMap<Object, CharSequence> labelCache) {
        synchronized (this.mCache) {
            CacheEntry entry = cacheLocked(application.componentName, info, labelCache, info.getUser());
            application.title = entry.title;
            application.iconBitmap = entry.icon;
            application.contentDescription = entry.contentDescription;
        }
    }

    public Bitmap getIcon(Intent intent, UserHandle user) {
        Bitmap bitmap;
        synchronized (this.mCache) {
            LauncherApps launcherApps = (LauncherApps) this.mContext.getSystemService("launcherapps");
            LauncherActivityInfo launcherActInfo = launcherApps.resolveActivity(intent, user);
            ComponentName component = intent.getComponent();
            if (launcherActInfo == null || component == null) {
                bitmap = this.mDefaultIcon;
            } else {
                CacheEntry entry = cacheLocked(component, launcherActInfo, null, user);
                bitmap = entry.icon;
            }
        }
        return bitmap;
    }

    public Bitmap getIcon(ComponentName component, LauncherActivityInfo info, HashMap<Object, CharSequence> labelCache) {
        Bitmap bitmap;
        synchronized (this.mCache) {
            if (info == null || component == null) {
                bitmap = null;
            } else {
                CacheEntry entry = cacheLocked(component, info, labelCache, info.getUser());
                bitmap = entry.icon;
            }
        }
        return bitmap;
    }

    public boolean isDefaultIcon(Bitmap icon) {
        return this.mDefaultIcon == icon;
    }

    private CacheEntry cacheLocked(ComponentName componentName, LauncherActivityInfo info, HashMap<Object, CharSequence> labelCache, UserHandle user) {
        CacheKey cacheKey = new CacheKey(componentName, user);
        CacheEntry entry = this.mCache.get(cacheKey);
        if (entry == null) {
            entry = new CacheEntry();
            this.mCache.put(cacheKey, entry);
            ComponentName key = info.getComponentName();
            if (labelCache != null && labelCache.containsKey(key)) {
                entry.title = labelCache.get(key).toString();
            } else {
                entry.title = info.getLabel().toString();
                if (labelCache != null) {
                    labelCache.put(key, entry.title);
                }
            }
            if (entry.title == null) {
                entry.title = info.getComponentName().getShortClassName();
            }
            entry.contentDescription = this.mPackageManager.getUserBadgedLabel(entry.title, user);
            entry.icon = Utilities.createIconBitmap(info.getBadgedIcon(this.mIconDpi), this.mContext);
        }
        return entry;
    }
}
