package com.android.settingslib.users;

import android.app.AppGlobals;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AppRestrictionsHelper {
    private final Context mContext;
    private boolean mLeanback;
    private final PackageManager mPackageManager;
    private final boolean mRestrictedProfile;
    private final UserHandle mUser;
    private final UserManager mUserManager;
    private List<SelectableAppInfo> mVisibleApps;
    HashMap<String, Boolean> mSelectedPackages = new HashMap<>();
    private final IPackageManager mIPm = AppGlobals.getPackageManager();

    public interface OnDisableUiForPackageListener {
        void onDisableUiForPackage(String str);
    }

    public AppRestrictionsHelper(Context context, UserHandle user) {
        this.mContext = context;
        this.mPackageManager = context.getPackageManager();
        this.mUser = user;
        this.mUserManager = (UserManager) context.getSystemService("user");
        this.mRestrictedProfile = this.mUserManager.getUserInfo(this.mUser.getIdentifier()).isRestricted();
    }

    public void setPackageSelected(String packageName, boolean selected) {
        this.mSelectedPackages.put(packageName, Boolean.valueOf(selected));
    }

    public boolean isPackageSelected(String packageName) {
        return this.mSelectedPackages.get(packageName).booleanValue();
    }

    public List<SelectableAppInfo> getVisibleApps() {
        return this.mVisibleApps;
    }

    public void applyUserAppsStates(OnDisableUiForPackageListener listener) {
        int userId = this.mUser.getIdentifier();
        if (!this.mUserManager.getUserInfo(userId).isRestricted() && userId != UserHandle.myUserId()) {
            Log.e("AppRestrictionsHelper", "Cannot apply application restrictions on another user!");
            return;
        }
        for (Map.Entry<String, Boolean> entry : this.mSelectedPackages.entrySet()) {
            String packageName = entry.getKey();
            boolean enabled = entry.getValue().booleanValue();
            applyUserAppState(packageName, enabled, listener);
        }
    }

    public void applyUserAppState(String packageName, boolean enabled, OnDisableUiForPackageListener listener) {
        int userId = this.mUser.getIdentifier();
        if (enabled) {
            try {
                ApplicationInfo info = this.mIPm.getApplicationInfo(packageName, 8192, userId);
                if (info == null || !info.enabled || (info.flags & 8388608) == 0) {
                    this.mIPm.installExistingPackageAsUser(packageName, this.mUser.getIdentifier());
                }
                if (info == null || (info.privateFlags & 1) == 0 || (info.flags & 8388608) == 0) {
                    return;
                }
                listener.onDisableUiForPackage(packageName);
                this.mIPm.setApplicationHiddenSettingAsUser(packageName, false, userId);
                return;
            } catch (RemoteException e) {
                return;
            }
        }
        try {
            if (this.mIPm.getApplicationInfo(packageName, 0, userId) != null) {
                if (this.mRestrictedProfile) {
                    this.mIPm.deletePackageAsUser(packageName, (IPackageDeleteObserver) null, this.mUser.getIdentifier(), 4);
                } else {
                    listener.onDisableUiForPackage(packageName);
                    this.mIPm.setApplicationHiddenSettingAsUser(packageName, true, userId);
                }
            }
        } catch (RemoteException e2) {
        }
    }

    public void fetchAndMergeApps() {
        this.mVisibleApps = new ArrayList();
        PackageManager pm = this.mPackageManager;
        IPackageManager ipm = this.mIPm;
        HashSet<String> excludePackages = new HashSet<>();
        addSystemImes(excludePackages);
        Intent launcherIntent = new Intent("android.intent.action.MAIN");
        if (this.mLeanback) {
            launcherIntent.addCategory("android.intent.category.LEANBACK_LAUNCHER");
        } else {
            launcherIntent.addCategory("android.intent.category.LAUNCHER");
        }
        addSystemApps(this.mVisibleApps, launcherIntent, excludePackages);
        Intent widgetIntent = new Intent("android.appwidget.action.APPWIDGET_UPDATE");
        addSystemApps(this.mVisibleApps, widgetIntent, excludePackages);
        List<ApplicationInfo> installedApps = pm.getInstalledApplications(8192);
        for (ApplicationInfo app : installedApps) {
            if ((app.flags & 8388608) != 0) {
                if ((app.flags & 1) == 0 && (app.flags & 128) == 0) {
                    SelectableAppInfo info = new SelectableAppInfo();
                    info.packageName = app.packageName;
                    info.appName = app.loadLabel(pm);
                    info.activityName = info.appName;
                    info.icon = app.loadIcon(pm);
                    this.mVisibleApps.add(info);
                } else {
                    try {
                        PackageInfo pi = pm.getPackageInfo(app.packageName, 0);
                        if (this.mRestrictedProfile && pi.requiredAccountType != null && pi.restrictedAccountType == null) {
                            this.mSelectedPackages.put(app.packageName, false);
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                    }
                }
            }
        }
        List<ApplicationInfo> userApps = null;
        try {
            ParceledListSlice<ApplicationInfo> listSlice = ipm.getInstalledApplications(8192, this.mUser.getIdentifier());
            if (listSlice != null) {
                userApps = listSlice.getList();
            }
        } catch (RemoteException e2) {
        }
        if (userApps != null) {
            for (ApplicationInfo app2 : userApps) {
                if ((app2.flags & 8388608) != 0 && (app2.flags & 1) == 0 && (app2.flags & 128) == 0) {
                    SelectableAppInfo info2 = new SelectableAppInfo();
                    info2.packageName = app2.packageName;
                    info2.appName = app2.loadLabel(pm);
                    info2.activityName = info2.appName;
                    info2.icon = app2.loadIcon(pm);
                    this.mVisibleApps.add(info2);
                }
            }
        }
        Collections.sort(this.mVisibleApps, new AppLabelComparator(null));
        Set<String> dedupPackageSet = new HashSet<>();
        for (int i = this.mVisibleApps.size() - 1; i >= 0; i--) {
            SelectableAppInfo info3 = this.mVisibleApps.get(i);
            String both = info3.packageName + "+" + info3.activityName;
            if (!TextUtils.isEmpty(info3.packageName) && !TextUtils.isEmpty(info3.activityName) && dedupPackageSet.contains(both)) {
                this.mVisibleApps.remove(i);
            } else {
                dedupPackageSet.add(both);
            }
        }
        HashMap<String, SelectableAppInfo> packageMap = new HashMap<>();
        for (SelectableAppInfo info4 : this.mVisibleApps) {
            if (packageMap.containsKey(info4.packageName)) {
                info4.masterEntry = packageMap.get(info4.packageName);
            } else {
                packageMap.put(info4.packageName, info4);
            }
        }
    }

    private void addSystemImes(Set<String> excludePackages) {
        InputMethodManager imm = (InputMethodManager) this.mContext.getSystemService("input_method");
        List<InputMethodInfo> imis = imm.getInputMethodList();
        for (InputMethodInfo imi : imis) {
            try {
                if (imi.isDefault(this.mContext) && isSystemPackage(imi.getPackageName())) {
                    excludePackages.add(imi.getPackageName());
                }
            } catch (Resources.NotFoundException e) {
            }
        }
    }

    private void addSystemApps(List<SelectableAppInfo> visibleApps, Intent intent, Set<String> excludePackages) {
        int enabled;
        ApplicationInfo targetUserAppInfo;
        PackageManager pm = this.mPackageManager;
        List<ResolveInfo> launchableApps = pm.queryIntentActivities(intent, 8704);
        for (ResolveInfo app : launchableApps) {
            if (app.activityInfo != null && app.activityInfo.applicationInfo != null) {
                String packageName = app.activityInfo.packageName;
                int flags = app.activityInfo.applicationInfo.flags;
                if ((flags & 1) != 0 || (flags & 128) != 0) {
                    if (!excludePackages.contains(packageName) && (((enabled = pm.getApplicationEnabledSetting(packageName)) != 4 && enabled != 2) || ((targetUserAppInfo = getAppInfoForUser(packageName, 0, this.mUser)) != null && (targetUserAppInfo.flags & 8388608) != 0))) {
                        SelectableAppInfo info = new SelectableAppInfo();
                        info.packageName = app.activityInfo.packageName;
                        info.appName = app.activityInfo.applicationInfo.loadLabel(pm);
                        info.icon = app.activityInfo.loadIcon(pm);
                        info.activityName = app.activityInfo.loadLabel(pm);
                        if (info.activityName == null) {
                            info.activityName = info.appName;
                        }
                        visibleApps.add(info);
                    }
                }
            }
        }
    }

    private boolean isSystemPackage(String packageName) {
        try {
            PackageInfo pi = this.mPackageManager.getPackageInfo(packageName, 0);
            if (pi.applicationInfo == null) {
                return false;
            }
            int flags = pi.applicationInfo.flags;
            if ((flags & 1) != 0 || (flags & 128) != 0) {
                return true;
            }
        } catch (PackageManager.NameNotFoundException e) {
        }
        return false;
    }

    private ApplicationInfo getAppInfoForUser(String packageName, int flags, UserHandle user) {
        try {
            return this.mIPm.getApplicationInfo(packageName, flags, user.getIdentifier());
        } catch (RemoteException e) {
            return null;
        }
    }

    public static class SelectableAppInfo {
        public CharSequence activityName;
        public CharSequence appName;
        public Drawable icon;
        public SelectableAppInfo masterEntry;
        public String packageName;

        public String toString() {
            return this.packageName + ": appName=" + this.appName + "; activityName=" + this.activityName + "; icon=" + this.icon + "; masterEntry=" + this.masterEntry;
        }
    }

    private static class AppLabelComparator implements Comparator<SelectableAppInfo> {
        AppLabelComparator(AppLabelComparator appLabelComparator) {
            this();
        }

        private AppLabelComparator() {
        }

        @Override
        public int compare(SelectableAppInfo lhs, SelectableAppInfo rhs) {
            String lhsLabel = lhs.activityName.toString();
            String rhsLabel = rhs.activityName.toString();
            return lhsLabel.toLowerCase().compareTo(rhsLabel.toLowerCase());
        }
    }
}
