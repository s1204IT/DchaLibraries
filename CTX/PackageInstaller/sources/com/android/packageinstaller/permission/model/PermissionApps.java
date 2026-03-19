package com.android.packageinstaller.permission.model;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.IconDrawableFactory;
import android.util.Log;
import android.util.SparseArray;
import com.android.packageinstaller.R;
import com.android.packageinstaller.permission.utils.Utils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class PermissionApps {
    private ArrayMap<String, PermissionApp> mAppLookup;
    private final PmCache mCache;
    private final Callback mCallback;
    private final Context mContext;
    private final String mGroupName;
    private Drawable mIcon;
    private CharSequence mLabel;
    private List<PermissionApp> mPermApps;
    private final PackageManager mPm;
    private boolean mRefreshing;
    private boolean mSkipUi;

    public interface Callback {
        void onPermissionsLoaded(PermissionApps permissionApps);
    }

    public PermissionApps(Context context, String str, Callback callback) {
        this(context, str, callback, null);
    }

    public PermissionApps(Context context, String str, Callback callback, PmCache pmCache) {
        this.mCache = pmCache;
        this.mContext = context;
        this.mPm = this.mContext.getPackageManager();
        this.mGroupName = str;
        this.mCallback = callback;
        loadGroupInfo();
    }

    public String getGroupName() {
        return this.mGroupName;
    }

    public void refresh(boolean z) {
        if (this.mCallback == null) {
            throw new IllegalStateException("callback needs to be set");
        }
        if (!this.mRefreshing) {
            this.mRefreshing = true;
            this.mSkipUi = !z;
            new PermissionAppsLoader().execute(new Void[0]);
        }
    }

    public void refreshSync() {
        this.mSkipUi = true;
        createMap(loadPermissionApps());
    }

    public int getGrantedCount(ArraySet<String> arraySet) {
        int i = 0;
        for (PermissionApp permissionApp : this.mPermApps) {
            if (Utils.shouldShowPermission(permissionApp) && !Utils.isSystem(permissionApp, arraySet) && permissionApp.areRuntimePermissionsGranted()) {
                i++;
            }
        }
        return i;
    }

    public int getTotalCount(ArraySet<String> arraySet) {
        int i = 0;
        for (PermissionApp permissionApp : this.mPermApps) {
            if (Utils.shouldShowPermission(permissionApp) && !Utils.isSystem(permissionApp, arraySet)) {
                i++;
            }
        }
        return i;
    }

    public List<PermissionApp> getApps() {
        return this.mPermApps;
    }

    public PermissionApp getApp(String str) {
        return this.mAppLookup.get(str);
    }

    public CharSequence getLabel() {
        return this.mLabel;
    }

    public Drawable getIcon() {
        return this.mIcon;
    }

    private List<PermissionApp> loadPermissionApps() {
        List<PackageInfo> installedPackagesAsUser;
        PermissionInfo next;
        AppPermissionGroup appPermissionGroupCreate;
        PackageItemInfo groupInfo = getGroupInfo(this.mGroupName);
        if (groupInfo != null) {
            List<PermissionInfo> groupPermissionInfos = getGroupPermissionInfos(this.mGroupName);
            if (groupPermissionInfos == null) {
                return Collections.emptyList();
            }
            ArrayList arrayList = new ArrayList();
            IconDrawableFactory iconDrawableFactoryNewInstance = IconDrawableFactory.newInstance(this.mContext);
            for (UserHandle userHandle : ((UserManager) this.mContext.getSystemService(UserManager.class)).getUserProfiles()) {
                if (this.mCache != null) {
                    installedPackagesAsUser = this.mCache.getPackages(userHandle.getIdentifier());
                } else {
                    installedPackagesAsUser = this.mPm.getInstalledPackagesAsUser(4096, userHandle.getIdentifier());
                }
                int size = installedPackagesAsUser.size();
                for (int i = 0; i < size; i++) {
                    PackageInfo packageInfo = installedPackagesAsUser.get(i);
                    if (packageInfo.requestedPermissions != null) {
                        int i2 = 0;
                        while (true) {
                            if (i2 >= packageInfo.requestedPermissions.length) {
                                break;
                            }
                            String str = packageInfo.requestedPermissions[i2];
                            Iterator<PermissionInfo> it = groupPermissionInfos.iterator();
                            while (true) {
                                if (it.hasNext()) {
                                    next = it.next();
                                    if (str.equals(next.name)) {
                                        break;
                                    }
                                } else {
                                    next = null;
                                    break;
                                }
                            }
                            if (next == null || (next.protectionLevel & 15) != 1 || (next.flags & 1073741824) == 0 || (next.flags & 2) != 0 || (appPermissionGroupCreate = AppPermissionGroup.create(this.mContext, packageInfo, groupInfo, groupPermissionInfos, userHandle)) == null) {
                                i2++;
                            } else {
                                arrayList.add(new PermissionApp(packageInfo.packageName, appPermissionGroupCreate, this.mSkipUi ? packageInfo.packageName : packageInfo.applicationInfo.loadLabel(this.mPm).toString(), this.mSkipUi ? null : iconDrawableFactoryNewInstance.getBadgedIcon(packageInfo.applicationInfo, UserHandle.getUserId(appPermissionGroupCreate.getApp().applicationInfo.uid)), packageInfo.applicationInfo));
                            }
                        }
                    }
                }
            }
            Collections.sort(arrayList);
            return arrayList;
        }
        return Collections.emptyList();
    }

    private void createMap(List<PermissionApp> list) {
        this.mAppLookup = new ArrayMap<>();
        for (PermissionApp permissionApp : list) {
            this.mAppLookup.put(permissionApp.getKey(), permissionApp);
        }
        this.mPermApps = list;
    }

    private PackageItemInfo getGroupInfo(String str) {
        try {
            return this.mContext.getPackageManager().getPermissionGroupInfo(str, 0);
        } catch (PackageManager.NameNotFoundException e) {
            try {
                return this.mContext.getPackageManager().getPermissionInfo(str, 0);
            } catch (PackageManager.NameNotFoundException e2) {
                return null;
            }
        }
    }

    private List<PermissionInfo> getGroupPermissionInfos(String str) {
        try {
            return this.mContext.getPackageManager().queryPermissionsByGroup(str, 0);
        } catch (PackageManager.NameNotFoundException e) {
            try {
                PermissionInfo permissionInfo = this.mContext.getPackageManager().getPermissionInfo(str, 0);
                ArrayList arrayList = new ArrayList();
                arrayList.add(permissionInfo);
                return arrayList;
            } catch (PackageManager.NameNotFoundException e2) {
                return null;
            }
        }
    }

    private void loadGroupInfo() {
        PackageItemInfo permissionGroupInfo;
        try {
            permissionGroupInfo = this.mPm.getPermissionGroupInfo(this.mGroupName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            try {
                PermissionInfo permissionInfo = this.mPm.getPermissionInfo(this.mGroupName, 0);
                int i = permissionInfo.protectionLevel & 15;
                permissionGroupInfo = permissionInfo;
                if (i != 1) {
                    Log.w("PermissionApps", this.mGroupName + " is not a runtime permission");
                    return;
                }
            } catch (PackageManager.NameNotFoundException e2) {
                Log.w("PermissionApps", "Can't find permission: " + this.mGroupName, e2);
                return;
            }
        }
        this.mLabel = permissionGroupInfo.loadLabel(this.mPm);
        if (permissionGroupInfo.icon != 0) {
            this.mIcon = permissionGroupInfo.loadUnbadgedIcon(this.mPm);
        } else {
            this.mIcon = this.mContext.getDrawable(R.drawable.ic_perm_device_info);
        }
        this.mIcon = Utils.applyTint(this.mContext, this.mIcon, android.R.attr.colorControlNormal);
    }

    public static class PermissionApp implements Comparable<PermissionApp> {
        private final AppPermissionGroup mAppPermissionGroup;
        private final Drawable mIcon;
        private final ApplicationInfo mInfo;
        private final String mLabel;
        private final String mPackageName;

        public PermissionApp(String str, AppPermissionGroup appPermissionGroup, String str2, Drawable drawable, ApplicationInfo applicationInfo) {
            this.mPackageName = str;
            this.mAppPermissionGroup = appPermissionGroup;
            this.mLabel = str2;
            this.mIcon = drawable;
            this.mInfo = applicationInfo;
        }

        public ApplicationInfo getAppInfo() {
            return this.mInfo;
        }

        public String getKey() {
            return this.mPackageName + getUid();
        }

        public String getLabel() {
            return this.mLabel;
        }

        public Drawable getIcon() {
            return this.mIcon;
        }

        public boolean areRuntimePermissionsGranted() {
            return this.mAppPermissionGroup.areRuntimePermissionsGranted();
        }

        public boolean isReviewRequired() {
            return this.mAppPermissionGroup.isReviewRequired();
        }

        public void grantRuntimePermissions() {
            this.mAppPermissionGroup.grantRuntimePermissions(false);
        }

        public void revokeRuntimePermissions() {
            this.mAppPermissionGroup.revokeRuntimePermissions(false);
        }

        public boolean isPolicyFixed() {
            return this.mAppPermissionGroup.isPolicyFixed();
        }

        public boolean isSystemFixed() {
            return this.mAppPermissionGroup.isSystemFixed();
        }

        public boolean hasGrantedByDefaultPermissions() {
            return this.mAppPermissionGroup.hasGrantedByDefaultPermission();
        }

        public boolean doesSupportRuntimePermissions() {
            return this.mAppPermissionGroup.doesSupportRuntimePermissions();
        }

        public int getUserId() {
            return this.mAppPermissionGroup.getUserId();
        }

        public String getPackageName() {
            return this.mPackageName;
        }

        public AppPermissionGroup getPermissionGroup() {
            return this.mAppPermissionGroup;
        }

        @Override
        public int compareTo(PermissionApp permissionApp) {
            int iCompareTo = this.mLabel.compareTo(permissionApp.mLabel);
            if (iCompareTo == 0) {
                return getKey().compareTo(permissionApp.getKey());
            }
            return iCompareTo;
        }

        public int getUid() {
            return this.mAppPermissionGroup.getApp().applicationInfo.uid;
        }
    }

    private class PermissionAppsLoader extends AsyncTask<Void, Void, List<PermissionApp>> {
        private PermissionAppsLoader() {
        }

        @Override
        protected List<PermissionApp> doInBackground(Void... voidArr) {
            return PermissionApps.this.loadPermissionApps();
        }

        @Override
        protected void onPostExecute(List<PermissionApp> list) {
            PermissionApps.this.mRefreshing = false;
            PermissionApps.this.createMap(list);
            if (PermissionApps.this.mCallback != null) {
                PermissionApps.this.mCallback.onPermissionsLoaded(PermissionApps.this);
            }
        }
    }

    public static class PmCache {
        private final SparseArray<List<PackageInfo>> mPackageInfoCache = new SparseArray<>();
        private final PackageManager mPm;

        public PmCache(PackageManager packageManager) {
            this.mPm = packageManager;
        }

        public synchronized List<PackageInfo> getPackages(int i) {
            List installedPackagesAsUser;
            installedPackagesAsUser = this.mPackageInfoCache.get(i);
            if (installedPackagesAsUser == null) {
                installedPackagesAsUser = this.mPm.getInstalledPackagesAsUser(4096, i);
                this.mPackageInfoCache.put(i, installedPackagesAsUser);
            }
            return installedPackagesAsUser;
        }
    }
}
