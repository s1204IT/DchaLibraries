package com.android.launcher3;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import com.android.launcher3.compat.LauncherActivityInfoCompat;
import com.android.launcher3.compat.LauncherAppsCompat;
import com.android.launcher3.compat.UserHandleCompat;
import com.android.launcher3.compat.UserManagerCompat;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.model.PackageItemInfo;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.SQLiteCacheHelper;
import com.mediatek.launcher3.LauncherLog;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Stack;

public class IconCache {
    static final Object ICON_UPDATE_TOKEN = new Object();
    private final int mActivityBgColor;
    private final Context mContext;
    final IconDB mIconDb;
    private final int mIconDpi;
    private final LauncherAppsCompat mLauncherApps;
    private Bitmap mLowResBitmap;
    private Canvas mLowResCanvas;
    private final BitmapFactory.Options mLowResOptions;
    private Paint mLowResPaint;
    private final int mPackageBgColor;
    private final PackageManager mPackageManager;
    private String mSystemState;
    final UserManagerCompat mUserManager;
    final Handler mWorkerHandler;
    private final HashMap<UserHandleCompat, Bitmap> mDefaultIcons = new HashMap<>();
    final MainThreadExecutor mMainThreadExecutor = new MainThreadExecutor();
    private final HashMap<ComponentKey, CacheEntry> mCache = new HashMap<>(50);

    static class CacheEntry {
        public Bitmap icon;
        public boolean isLowResIcon;
        public CharSequence title = "";
        public CharSequence contentDescription = "";

        CacheEntry() {
        }
    }

    public IconCache(Context context, InvariantDeviceProfile inv) {
        this.mContext = context;
        this.mPackageManager = context.getPackageManager();
        this.mUserManager = UserManagerCompat.getInstance(this.mContext);
        this.mLauncherApps = LauncherAppsCompat.getInstance(this.mContext);
        this.mIconDpi = inv.fillResIconDpi;
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher.IconCache", "IconCache, mIconDpi = " + this.mIconDpi);
        }
        this.mIconDb = new IconDB(context, inv.iconBitmapSize);
        this.mWorkerHandler = new Handler(LauncherModel.getWorkerLooper());
        this.mActivityBgColor = context.getResources().getColor(R.color.quantum_panel_bg_color);
        this.mPackageBgColor = context.getResources().getColor(R.color.quantum_panel_bg_color_dark);
        this.mLowResOptions = new BitmapFactory.Options();
        this.mLowResOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        updateSystemStateString();
    }

    private Drawable getFullResDefaultActivityIcon() {
        return getFullResIcon(Resources.getSystem(), android.R.mipmap.sym_def_app_icon);
    }

    private Drawable getFullResIcon(Resources resources, int iconId) {
        Drawable drawableForDensity;
        try {
            drawableForDensity = resources.getDrawableForDensity(iconId, this.mIconDpi);
        } catch (Resources.NotFoundException e) {
            drawableForDensity = null;
        }
        if (drawableForDensity != null) {
            return drawableForDensity;
        }
        Drawable d = getFullResDefaultActivityIcon();
        return d;
    }

    public Drawable getFullResIcon(String packageName, int iconId) {
        Resources resourcesForApplication;
        try {
            resourcesForApplication = this.mPackageManager.getResourcesForApplication(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            resourcesForApplication = null;
        }
        if (resourcesForApplication != null && iconId != 0) {
            return getFullResIcon(resourcesForApplication, iconId);
        }
        return getFullResDefaultActivityIcon();
    }

    public Drawable getFullResIcon(ActivityInfo info) {
        Resources resourcesForApplication;
        int iconId;
        try {
            resourcesForApplication = this.mPackageManager.getResourcesForApplication(info.applicationInfo);
        } catch (PackageManager.NameNotFoundException e) {
            resourcesForApplication = null;
        }
        if (resourcesForApplication != null && (iconId = info.getIconResource()) != 0) {
            return getFullResIcon(resourcesForApplication, iconId);
        }
        return getFullResDefaultActivityIcon();
    }

    private Bitmap makeDefaultIcon(UserHandleCompat user) {
        Drawable unbadged = getFullResDefaultActivityIcon();
        return Utilities.createBadgedIconBitmap(unbadged, user, this.mContext);
    }

    public synchronized void remove(ComponentName componentName, UserHandleCompat user) {
        this.mCache.remove(new ComponentKey(componentName, user));
    }

    private void removeFromMemCacheLocked(String packageName, UserHandleCompat user) {
        HashSet<ComponentKey> forDeletion = new HashSet<>();
        for (ComponentKey key : this.mCache.keySet()) {
            if (key.componentName.getPackageName().equals(packageName) && key.user.equals(user)) {
                forDeletion.add(key);
            }
        }
        for (ComponentKey condemned : forDeletion) {
            this.mCache.remove(condemned);
        }
    }

    public synchronized void updateIconsForPkg(String packageName, UserHandleCompat user) {
        removeIconsForPkg(packageName, user);
        try {
            PackageInfo info = this.mPackageManager.getPackageInfo(packageName, 8192);
            long userSerial = this.mUserManager.getSerialNumberForUser(user);
            for (LauncherActivityInfoCompat app : this.mLauncherApps.getActivityList(packageName, user)) {
                addIconToDBAndMemCache(app, info, userSerial);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.d("Launcher.IconCache", "Package not found", e);
        }
    }

    public synchronized void removeIconsForPkg(String packageName, UserHandleCompat user) {
        removeFromMemCacheLocked(packageName, user);
        long userSerial = this.mUserManager.getSerialNumberForUser(user);
        this.mIconDb.delete("componentName LIKE ? AND profileId = ?", new String[]{packageName + "/%", Long.toString(userSerial)});
    }

    public void updateDbIcons(Set<String> ignorePackagesForMainUser) {
        UserHandleCompat user;
        List<LauncherActivityInfoCompat> apps;
        this.mWorkerHandler.removeCallbacksAndMessages(ICON_UPDATE_TOKEN);
        updateSystemStateString();
        Iterator user$iterator = this.mUserManager.getUserProfiles().iterator();
        while (user$iterator.hasNext() && (apps = this.mLauncherApps.getActivityList(null, (user = (UserHandleCompat) user$iterator.next()))) != null && !apps.isEmpty()) {
            updateDBIcons(user, apps, UserHandleCompat.myUserHandle().equals(user) ? ignorePackagesForMainUser : Collections.emptySet());
        }
    }

    private void updateDBIcons(UserHandleCompat user, List<LauncherActivityInfoCompat> apps, Set<String> ignorePackages) {
        long userSerial = this.mUserManager.getSerialNumberForUser(user);
        PackageManager pm = this.mContext.getPackageManager();
        HashMap<String, PackageInfo> pkgInfoMap = new HashMap<>();
        for (PackageInfo info : pm.getInstalledPackages(8192)) {
            pkgInfoMap.put(info.packageName, info);
        }
        HashMap<ComponentName, LauncherActivityInfoCompat> componentMap = new HashMap<>();
        for (LauncherActivityInfoCompat app : apps) {
            componentMap.put(app.getComponentName(), app);
        }
        HashSet<Integer> itemsToRemove = new HashSet<>();
        Stack<LauncherActivityInfoCompat> appsToUpdate = new Stack<>();
        Cursor c = null;
        try {
            try {
                c = this.mIconDb.query(new String[]{"rowid", "componentName", "lastUpdated", "version", "system_state"}, "profileId = ? ", new String[]{Long.toString(userSerial)});
                int indexComponent = c.getColumnIndex("componentName");
                int indexLastUpdate = c.getColumnIndex("lastUpdated");
                int indexVersion = c.getColumnIndex("version");
                int rowIndex = c.getColumnIndex("rowid");
                int systemStateIndex = c.getColumnIndex("system_state");
                while (c.moveToNext()) {
                    String cn = c.getString(indexComponent);
                    ComponentName component = ComponentName.unflattenFromString(cn);
                    PackageInfo info2 = pkgInfoMap.get(component.getPackageName());
                    if (info2 == null) {
                        if (!ignorePackages.contains(component.getPackageName())) {
                            remove(component, user);
                            itemsToRemove.add(Integer.valueOf(c.getInt(rowIndex)));
                        }
                    } else if ((info2.applicationInfo.flags & 16777216) == 0) {
                        long updateTime = c.getLong(indexLastUpdate);
                        int version = c.getInt(indexVersion);
                        LauncherActivityInfoCompat app2 = componentMap.remove(component);
                        if (version != info2.versionCode || updateTime != info2.lastUpdateTime || !TextUtils.equals(this.mSystemState, c.getString(systemStateIndex))) {
                            if (app2 == null) {
                                remove(component, user);
                                itemsToRemove.add(Integer.valueOf(c.getInt(rowIndex)));
                            } else {
                                appsToUpdate.add(app2);
                            }
                        }
                    }
                }
                if (c != null) {
                    c.close();
                }
            } catch (SQLiteException e) {
                Log.d("Launcher.IconCache", "Error reading icon cache", e);
                if (c != null) {
                    c.close();
                }
            }
            if (!itemsToRemove.isEmpty()) {
                this.mIconDb.delete(Utilities.createDbSelectionQuery("rowid", itemsToRemove), null);
            }
            if (componentMap.isEmpty() && appsToUpdate.isEmpty()) {
                return;
            }
            Stack<LauncherActivityInfoCompat> appsToAdd = new Stack<>();
            appsToAdd.addAll(componentMap.values());
            new SerializedIconUpdateTask(userSerial, pkgInfoMap, appsToAdd, appsToUpdate).scheduleNext();
        } catch (Throwable th) {
            if (c != null) {
                c.close();
            }
            throw th;
        }
    }

    void addIconToDBAndMemCache(LauncherActivityInfoCompat app, PackageInfo info, long userSerial) {
        ContentValues values = updateCacheAndGetContentValues(app, false);
        addIconToDB(values, app.getComponentName(), info, userSerial);
    }

    private void addIconToDB(ContentValues values, ComponentName key, PackageInfo info, long userSerial) {
        values.put("componentName", key.flattenToString());
        values.put("profileId", Long.valueOf(userSerial));
        values.put("lastUpdated", Long.valueOf(info.lastUpdateTime));
        values.put("version", Integer.valueOf(info.versionCode));
        this.mIconDb.insertOrReplace(values);
    }

    ContentValues updateCacheAndGetContentValues(LauncherActivityInfoCompat app, boolean replaceExisting) {
        ComponentKey key = new ComponentKey(app.getComponentName(), app.getUser());
        CacheEntry entry = null;
        if (!replaceExisting) {
            CacheEntry entry2 = this.mCache.get(key);
            entry = entry2;
            if (entry == null || entry.isLowResIcon || entry.icon == null) {
                entry = null;
            }
        }
        if (entry == null) {
            entry = new CacheEntry();
            entry.icon = Utilities.createBadgedIconBitmap(app.getIcon(this.mIconDpi), app.getUser(), this.mContext);
        }
        entry.title = app.getLabel();
        entry.contentDescription = this.mUserManager.getBadgedLabelForUser(entry.title, app.getUser());
        this.mCache.put(new ComponentKey(app.getComponentName(), app.getUser()), entry);
        return newContentValues(entry.icon, entry.title.toString(), this.mActivityBgColor);
    }

    public IconLoadRequest updateIconInBackground(final BubbleTextView caller, final ItemInfo info) {
        Runnable request = new Runnable() {
            @Override
            public void run() {
                if (info instanceof AppInfo) {
                    IconCache.this.getTitleAndIcon((AppInfo) info, null, false);
                } else if (info instanceof ShortcutInfo) {
                    ShortcutInfo st = (ShortcutInfo) info;
                    IconCache.this.getTitleAndIcon(st, st.promisedIntent != null ? st.promisedIntent : st.intent, st.user, false);
                } else if (info instanceof PackageItemInfo) {
                    PackageItemInfo pti = (PackageItemInfo) info;
                    IconCache.this.getTitleAndIconForApp(pti.packageName, pti.user, false, pti);
                }
                MainThreadExecutor mainThreadExecutor = IconCache.this.mMainThreadExecutor;
                final BubbleTextView bubbleTextView = caller;
                final ItemInfo itemInfo = info;
                mainThreadExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        bubbleTextView.reapplyItemInfo(itemInfo);
                    }
                });
            }
        };
        this.mWorkerHandler.post(request);
        return new IconLoadRequest(request, this.mWorkerHandler);
    }

    private Bitmap getNonNullIcon(CacheEntry entry, UserHandleCompat user) {
        return entry.icon == null ? getDefaultIcon(user) : entry.icon;
    }

    public synchronized void getTitleAndIcon(AppInfo application, LauncherActivityInfoCompat info, boolean useLowResIcon) {
        UserHandleCompat user = info == null ? application.user : info.getUser();
        CacheEntry entry = cacheLocked(application.componentName, info, user, false, useLowResIcon);
        application.title = Utilities.trim(entry.title);
        application.iconBitmap = getNonNullIcon(entry, user);
        application.contentDescription = entry.contentDescription;
        application.usingLowResIcon = entry.isLowResIcon;
    }

    public synchronized void updateTitleAndIcon(AppInfo application) {
        CacheEntry entry = cacheLocked(application.componentName, null, application.user, false, application.usingLowResIcon);
        if (entry.icon != null && !isDefaultIcon(entry.icon, application.user)) {
            application.title = Utilities.trim(entry.title);
            application.iconBitmap = entry.icon;
            application.contentDescription = entry.contentDescription;
            application.usingLowResIcon = entry.isLowResIcon;
        }
    }

    public synchronized Bitmap getIcon(Intent intent, UserHandleCompat user) {
        ComponentName component = intent.getComponent();
        if (component == null) {
            return getDefaultIcon(user);
        }
        LauncherActivityInfoCompat launcherActInfo = this.mLauncherApps.resolveActivity(intent, user);
        CacheEntry entry = cacheLocked(component, launcherActInfo, user, true, false);
        return entry.icon;
    }

    public synchronized void getTitleAndIcon(ShortcutInfo shortcutInfo, Intent intent, UserHandleCompat user, boolean useLowResIcon) {
        ComponentName component = intent.getComponent();
        if (component == null) {
            shortcutInfo.setIcon(getDefaultIcon(user));
            shortcutInfo.title = "";
            shortcutInfo.usingFallbackIcon = true;
            shortcutInfo.usingLowResIcon = false;
        } else {
            LauncherActivityInfoCompat info = this.mLauncherApps.resolveActivity(intent, user);
            getTitleAndIcon(shortcutInfo, component, info, user, true, useLowResIcon);
        }
    }

    public synchronized void getTitleAndIcon(ShortcutInfo shortcutInfo, ComponentName component, LauncherActivityInfoCompat info, UserHandleCompat user, boolean usePkgIcon, boolean useLowResIcon) {
        CacheEntry entry = cacheLocked(component, info, user, usePkgIcon, useLowResIcon);
        shortcutInfo.setIcon(getNonNullIcon(entry, user));
        shortcutInfo.title = Utilities.trim(entry.title);
        shortcutInfo.usingFallbackIcon = isDefaultIcon(entry.icon, user);
        shortcutInfo.usingLowResIcon = entry.isLowResIcon;
    }

    public synchronized void getTitleAndIconForApp(String packageName, UserHandleCompat user, boolean useLowResIcon, PackageItemInfo infoOut) {
        CacheEntry entry = getEntryForPackageLocked(packageName, user, useLowResIcon);
        infoOut.iconBitmap = getNonNullIcon(entry, user);
        infoOut.title = Utilities.trim(entry.title);
        infoOut.usingLowResIcon = entry.isLowResIcon;
        infoOut.contentDescription = entry.contentDescription;
    }

    public synchronized Bitmap getDefaultIcon(UserHandleCompat user) {
        if (!this.mDefaultIcons.containsKey(user)) {
            this.mDefaultIcons.put(user, makeDefaultIcon(user));
        }
        return this.mDefaultIcons.get(user);
    }

    public boolean isDefaultIcon(Bitmap icon, UserHandleCompat user) {
        return this.mDefaultIcons.get(user) == icon;
    }

    private CacheEntry cacheLocked(ComponentName componentName, LauncherActivityInfoCompat info, UserHandleCompat user, boolean usePackageIcon, boolean useLowResIcon) {
        CacheEntry packageEntry;
        if (LauncherLog.DEBUG_LAYOUT) {
            LauncherLog.d("Launcher.IconCache", "cacheLocked: componentName = " + componentName + ", info = " + info);
        }
        ComponentKey cacheKey = new ComponentKey(componentName, user);
        CacheEntry entry = this.mCache.get(cacheKey);
        if (entry == null || (entry.isLowResIcon && !useLowResIcon)) {
            entry = new CacheEntry();
            this.mCache.put(cacheKey, entry);
            if (!getEntryFromDB(cacheKey, entry, useLowResIcon)) {
                if (info != null) {
                    entry.icon = Utilities.createBadgedIconBitmap(info.getIcon(this.mIconDpi), info.getUser(), this.mContext);
                } else {
                    if (usePackageIcon && (packageEntry = getEntryForPackageLocked(componentName.getPackageName(), user, false)) != null) {
                        entry.icon = packageEntry.icon;
                        entry.title = packageEntry.title;
                        entry.contentDescription = packageEntry.contentDescription;
                        if (LauncherLog.DEBUG_LOADERS) {
                            LauncherLog.d("Launcher.IconCache", "CacheLocked get title from pms: title = " + entry.title);
                        }
                    }
                    if (entry.icon == null) {
                        entry.icon = getDefaultIcon(user);
                    }
                }
            }
            if (TextUtils.isEmpty(entry.title) && info != null) {
                entry.title = info.getLabel();
                entry.contentDescription = this.mUserManager.getBadgedLabelForUser(entry.title, user);
            }
        }
        if (info != null) {
            entry.title = info.getLabel();
        }
        return entry;
    }

    public synchronized void cachePackageInstallInfo(String packageName, UserHandleCompat user, Bitmap icon, CharSequence title) {
        removeFromMemCacheLocked(packageName, user);
        ComponentKey cacheKey = getPackageKey(packageName, user);
        CacheEntry entry = this.mCache.get(cacheKey);
        if (entry == null) {
            entry = new CacheEntry();
            this.mCache.put(cacheKey, entry);
        }
        if (!TextUtils.isEmpty(title)) {
            entry.title = title;
        }
        if (icon != null) {
            entry.icon = Utilities.createIconBitmap(icon, this.mContext);
        }
    }

    private static ComponentKey getPackageKey(String packageName, UserHandleCompat user) {
        ComponentName cn = new ComponentName(packageName, packageName + ".");
        return new ComponentKey(cn, user);
    }

    private CacheEntry getEntryForPackageLocked(String packageName, UserHandleCompat user, boolean useLowResIcon) {
        ComponentKey cacheKey = getPackageKey(packageName, user);
        CacheEntry entry = this.mCache.get(cacheKey);
        if (entry == null || (entry.isLowResIcon && !useLowResIcon)) {
            entry = new CacheEntry();
            boolean entryUpdated = true;
            if (!getEntryFromDB(cacheKey, entry, useLowResIcon)) {
                try {
                    int flags = UserHandleCompat.myUserHandle().equals(user) ? 0 : 8192;
                    PackageInfo info = this.mPackageManager.getPackageInfo(packageName, flags);
                    ApplicationInfo appInfo = info.applicationInfo;
                    if (appInfo == null) {
                        throw new PackageManager.NameNotFoundException("ApplicationInfo is null");
                    }
                    entry.icon = Utilities.createBadgedIconBitmap(appInfo.loadIcon(this.mPackageManager), user, this.mContext);
                    entry.title = appInfo.loadLabel(this.mPackageManager);
                    entry.contentDescription = this.mUserManager.getBadgedLabelForUser(entry.title, user);
                    entry.isLowResIcon = false;
                    ContentValues values = newContentValues(entry.icon, entry.title.toString(), this.mPackageBgColor);
                    addIconToDB(values, cacheKey.componentName, info, this.mUserManager.getSerialNumberForUser(user));
                } catch (PackageManager.NameNotFoundException e) {
                    entryUpdated = false;
                }
            }
            if (entryUpdated) {
                this.mCache.put(cacheKey, entry);
            }
        }
        return entry;
    }

    public void preloadIcon(ComponentName componentName, Bitmap icon, int dpi, String label, long userSerial, InvariantDeviceProfile idp) {
        try {
            PackageManager packageManager = this.mContext.getPackageManager();
            packageManager.getActivityIcon(componentName);
        } catch (PackageManager.NameNotFoundException e) {
            ContentValues values = newContentValues(Bitmap.createScaledBitmap(icon, idp.iconBitmapSize, idp.iconBitmapSize, true), label, 0);
            values.put("componentName", componentName.flattenToString());
            values.put("profileId", Long.valueOf(userSerial));
            this.mIconDb.insertOrReplace(values);
        }
    }

    private boolean getEntryFromDB(ComponentKey cacheKey, CacheEntry entry, boolean lowRes) {
        Cursor c;
        Cursor cursor = null;
        try {
            try {
                IconDB iconDB = this.mIconDb;
                String[] strArr = new String[2];
                strArr[0] = lowRes ? "icon_low_res" : "icon";
                strArr[1] = "label";
                c = iconDB.query(strArr, "componentName = ? AND profileId = ?", new String[]{cacheKey.componentName.flattenToString(), Long.toString(this.mUserManager.getSerialNumberForUser(cacheKey.user))});
            } catch (SQLiteException e) {
                Log.d("Launcher.IconCache", "Error reading icon cache", e);
                if (0 != 0) {
                    cursor.close();
                }
            }
            if (!c.moveToNext()) {
                if (c != null) {
                    c.close();
                }
                return false;
            }
            entry.icon = loadIconNoResize(c, 0, lowRes ? this.mLowResOptions : null);
            entry.isLowResIcon = lowRes;
            entry.title = c.getString(1);
            if (entry.title == null) {
                entry.title = "";
                entry.contentDescription = "";
            } else {
                entry.contentDescription = this.mUserManager.getBadgedLabelForUser(entry.title, cacheKey.user);
            }
            if (c != null) {
                c.close();
            }
            return true;
        } catch (Throwable th) {
            if (0 != 0) {
                cursor.close();
            }
            throw th;
        }
    }

    public static class IconLoadRequest {
        private final Handler mHandler;
        private final Runnable mRunnable;

        IconLoadRequest(Runnable runnable, Handler handler) {
            this.mRunnable = runnable;
            this.mHandler = handler;
        }

        public void cancel() {
            this.mHandler.removeCallbacks(this.mRunnable);
        }
    }

    class SerializedIconUpdateTask implements Runnable {
        private final Stack<LauncherActivityInfoCompat> mAppsToAdd;
        private final Stack<LauncherActivityInfoCompat> mAppsToUpdate;
        private final HashMap<String, PackageInfo> mPkgInfoMap;
        private final HashSet<String> mUpdatedPackages = new HashSet<>();
        private final long mUserSerial;

        SerializedIconUpdateTask(long userSerial, HashMap<String, PackageInfo> pkgInfoMap, Stack<LauncherActivityInfoCompat> appsToAdd, Stack<LauncherActivityInfoCompat> appsToUpdate) {
            this.mUserSerial = userSerial;
            this.mPkgInfoMap = pkgInfoMap;
            this.mAppsToAdd = appsToAdd;
            this.mAppsToUpdate = appsToUpdate;
        }

        @Override
        public void run() {
            if (!this.mAppsToUpdate.isEmpty()) {
                LauncherActivityInfoCompat app = this.mAppsToUpdate.pop();
                String cn = app.getComponentName().flattenToString();
                ContentValues values = IconCache.this.updateCacheAndGetContentValues(app, true);
                IconCache.this.mIconDb.update(values, "componentName = ? AND profileId = ?", new String[]{cn, Long.toString(this.mUserSerial)});
                this.mUpdatedPackages.add(app.getComponentName().getPackageName());
                if (this.mAppsToUpdate.isEmpty() && !this.mUpdatedPackages.isEmpty()) {
                    LauncherAppState.getInstance().getModel().onPackageIconsUpdated(this.mUpdatedPackages, IconCache.this.mUserManager.getUserForSerialNumber(this.mUserSerial));
                }
                scheduleNext();
                return;
            }
            if (this.mAppsToAdd.isEmpty()) {
                return;
            }
            LauncherActivityInfoCompat app2 = this.mAppsToAdd.pop();
            PackageInfo info = this.mPkgInfoMap.get(app2.getComponentName().getPackageName());
            if (info != null) {
                synchronized (IconCache.this) {
                    IconCache.this.addIconToDBAndMemCache(app2, info, this.mUserSerial);
                }
            }
            if (this.mAppsToAdd.isEmpty()) {
                return;
            }
            scheduleNext();
        }

        public void scheduleNext() {
            IconCache.this.mWorkerHandler.postAtTime(this, IconCache.ICON_UPDATE_TOKEN, SystemClock.uptimeMillis() + 1);
        }
    }

    private void updateSystemStateString() {
        this.mSystemState = Locale.getDefault().toString();
    }

    private static final class IconDB extends SQLiteCacheHelper {
        private static final int RELEASE_VERSION;

        static {
            RELEASE_VERSION = (FeatureFlags.LAUNCHER3_ICON_NORMALIZATION ? 1 : 0) + 7;
        }

        public IconDB(Context context, int iconPixelSize) {
            super(context, "app_icons.db", (RELEASE_VERSION << 16) + iconPixelSize, "icons");
        }

        @Override
        protected void onCreateTable(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS icons (componentName TEXT NOT NULL, profileId INTEGER NOT NULL, lastUpdated INTEGER NOT NULL DEFAULT 0, version INTEGER NOT NULL DEFAULT 0, icon BLOB, icon_low_res BLOB, label TEXT, system_state TEXT, PRIMARY KEY (componentName, profileId) );");
        }
    }

    private ContentValues newContentValues(Bitmap icon, String label, int lowResBackgroundColor) {
        ContentValues values = new ContentValues();
        values.put("icon", Utilities.flattenBitmap(icon));
        values.put("label", label);
        values.put("system_state", this.mSystemState);
        if (lowResBackgroundColor == 0) {
            values.put("icon_low_res", Utilities.flattenBitmap(Bitmap.createScaledBitmap(icon, icon.getWidth() / 5, icon.getHeight() / 5, true)));
        } else {
            synchronized (this) {
                if (this.mLowResBitmap == null) {
                    this.mLowResBitmap = Bitmap.createBitmap(icon.getWidth() / 5, icon.getHeight() / 5, Bitmap.Config.RGB_565);
                    this.mLowResCanvas = new Canvas(this.mLowResBitmap);
                    this.mLowResPaint = new Paint(3);
                }
                this.mLowResCanvas.drawColor(lowResBackgroundColor);
                this.mLowResCanvas.drawBitmap(icon, new Rect(0, 0, icon.getWidth(), icon.getHeight()), new Rect(0, 0, this.mLowResBitmap.getWidth(), this.mLowResBitmap.getHeight()), this.mLowResPaint);
                values.put("icon_low_res", Utilities.flattenBitmap(this.mLowResBitmap));
            }
        }
        return values;
    }

    private static Bitmap loadIconNoResize(Cursor c, int iconIndex, BitmapFactory.Options options) {
        byte[] data = c.getBlob(iconIndex);
        try {
            return BitmapFactory.decodeByteArray(data, 0, data.length, options);
        } catch (Exception e) {
            return null;
        }
    }
}
