package com.android.settingslib.users;

import android.app.AppGlobals;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
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
    private final IPackageManager mIPm;
    private final Injector mInjector;
    private boolean mLeanback;
    private final PackageManager mPackageManager;
    private final boolean mRestrictedProfile;
    HashMap<String, Boolean> mSelectedPackages;
    private final UserHandle mUser;
    private final UserManager mUserManager;
    private List<SelectableAppInfo> mVisibleApps;

    public interface OnDisableUiForPackageListener {
        void onDisableUiForPackage(String str);
    }

    public AppRestrictionsHelper(Context context, UserHandle userHandle) {
        this(new Injector(context, userHandle));
    }

    AppRestrictionsHelper(Injector injector) {
        this.mSelectedPackages = new HashMap<>();
        this.mInjector = injector;
        this.mContext = this.mInjector.getContext();
        this.mPackageManager = this.mInjector.getPackageManager();
        this.mIPm = this.mInjector.getIPackageManager();
        this.mUser = this.mInjector.getUser();
        this.mUserManager = this.mInjector.getUserManager();
        this.mRestrictedProfile = this.mUserManager.getUserInfo(this.mUser.getIdentifier()).isRestricted();
    }

    public void setPackageSelected(String str, boolean z) {
        this.mSelectedPackages.put(str, Boolean.valueOf(z));
    }

    public boolean isPackageSelected(String str) {
        return this.mSelectedPackages.get(str).booleanValue();
    }

    public List<SelectableAppInfo> getVisibleApps() {
        return this.mVisibleApps;
    }

    public void applyUserAppsStates(OnDisableUiForPackageListener onDisableUiForPackageListener) {
        if (!this.mRestrictedProfile && this.mUser.getIdentifier() != UserHandle.myUserId()) {
            Log.e("AppRestrictionsHelper", "Cannot apply application restrictions on another user!");
            return;
        }
        for (Map.Entry<String, Boolean> entry : this.mSelectedPackages.entrySet()) {
            applyUserAppState(entry.getKey(), entry.getValue().booleanValue(), onDisableUiForPackageListener);
        }
    }

    public void applyUserAppState(String str, boolean z, OnDisableUiForPackageListener onDisableUiForPackageListener) {
        int identifier = this.mUser.getIdentifier();
        if (z) {
            try {
                ApplicationInfo applicationInfo = this.mIPm.getApplicationInfo(str, 4194304, identifier);
                if (applicationInfo == null || !applicationInfo.enabled || (applicationInfo.flags & 8388608) == 0) {
                    this.mIPm.installExistingPackageAsUser(str, this.mUser.getIdentifier(), 0, 0);
                }
                if (applicationInfo != null && (1 & applicationInfo.privateFlags) != 0 && (applicationInfo.flags & 8388608) != 0) {
                    onDisableUiForPackageListener.onDisableUiForPackage(str);
                    this.mIPm.setApplicationHiddenSettingAsUser(str, false, identifier);
                    return;
                }
                return;
            } catch (RemoteException e) {
                return;
            }
        }
        try {
            if (this.mIPm.getApplicationInfo(str, 0, identifier) != null) {
                if (this.mRestrictedProfile) {
                    this.mPackageManager.deletePackageAsUser(str, null, 4, this.mUser.getIdentifier());
                } else {
                    onDisableUiForPackageListener.onDisableUiForPackage(str);
                    this.mIPm.setApplicationHiddenSettingAsUser(str, true, identifier);
                }
            }
        } catch (RemoteException e2) {
        }
    }

    public void fetchAndMergeApps() {
        List<ApplicationInfo> list;
        this.mVisibleApps = new ArrayList();
        PackageManager packageManager = this.mPackageManager;
        IPackageManager iPackageManager = this.mIPm;
        HashSet hashSet = new HashSet();
        addSystemImes(hashSet);
        Intent intent = new Intent("android.intent.action.MAIN");
        if (this.mLeanback) {
            intent.addCategory("android.intent.category.LEANBACK_LAUNCHER");
        } else {
            intent.addCategory("android.intent.category.LAUNCHER");
        }
        addSystemApps(this.mVisibleApps, intent, hashSet);
        addSystemApps(this.mVisibleApps, new Intent("android.appwidget.action.APPWIDGET_UPDATE"), hashSet);
        for (ApplicationInfo applicationInfo : packageManager.getInstalledApplications(4194304)) {
            if ((8388608 & applicationInfo.flags) != 0) {
                if ((applicationInfo.flags & 1) == 0 && (applicationInfo.flags & 128) == 0) {
                    SelectableAppInfo selectableAppInfo = new SelectableAppInfo();
                    selectableAppInfo.packageName = applicationInfo.packageName;
                    selectableAppInfo.appName = applicationInfo.loadLabel(packageManager);
                    selectableAppInfo.activityName = selectableAppInfo.appName;
                    selectableAppInfo.icon = applicationInfo.loadIcon(packageManager);
                    this.mVisibleApps.add(selectableAppInfo);
                } else {
                    try {
                        PackageInfo packageInfo = packageManager.getPackageInfo(applicationInfo.packageName, 0);
                        if (this.mRestrictedProfile && packageInfo.requiredAccountType != null && packageInfo.restrictedAccountType == null) {
                            this.mSelectedPackages.put(applicationInfo.packageName, false);
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                    }
                }
            }
        }
        try {
            ParceledListSlice installedApplications = iPackageManager.getInstalledApplications(8192, this.mUser.getIdentifier());
            if (installedApplications != null) {
                list = installedApplications.getList();
            } else {
                list = null;
            }
        } catch (RemoteException e2) {
            list = null;
        }
        if (list != null) {
            for (ApplicationInfo applicationInfo2 : list) {
                if ((applicationInfo2.flags & 8388608) != 0 && (applicationInfo2.flags & 1) == 0 && (applicationInfo2.flags & 128) == 0) {
                    SelectableAppInfo selectableAppInfo2 = new SelectableAppInfo();
                    selectableAppInfo2.packageName = applicationInfo2.packageName;
                    selectableAppInfo2.appName = applicationInfo2.loadLabel(packageManager);
                    selectableAppInfo2.activityName = selectableAppInfo2.appName;
                    selectableAppInfo2.icon = applicationInfo2.loadIcon(packageManager);
                    this.mVisibleApps.add(selectableAppInfo2);
                }
            }
        }
        Collections.sort(this.mVisibleApps, new AppLabelComparator());
        HashSet hashSet2 = new HashSet();
        for (int size = this.mVisibleApps.size() - 1; size >= 0; size--) {
            SelectableAppInfo selectableAppInfo3 = this.mVisibleApps.get(size);
            String str = selectableAppInfo3.packageName + "+" + ((Object) selectableAppInfo3.activityName);
            if (!TextUtils.isEmpty(selectableAppInfo3.packageName) && !TextUtils.isEmpty(selectableAppInfo3.activityName) && hashSet2.contains(str)) {
                this.mVisibleApps.remove(size);
            } else {
                hashSet2.add(str);
            }
        }
        HashMap map = new HashMap();
        for (SelectableAppInfo selectableAppInfo4 : this.mVisibleApps) {
            if (map.containsKey(selectableAppInfo4.packageName)) {
                selectableAppInfo4.masterEntry = (SelectableAppInfo) map.get(selectableAppInfo4.packageName);
            } else {
                map.put(selectableAppInfo4.packageName, selectableAppInfo4);
            }
        }
    }

    private void addSystemImes(Set<String> set) {
        for (InputMethodInfo inputMethodInfo : this.mInjector.getInputMethodList()) {
            try {
                if (inputMethodInfo.isDefault(this.mContext) && isSystemPackage(inputMethodInfo.getPackageName())) {
                    set.add(inputMethodInfo.getPackageName());
                }
            } catch (Resources.NotFoundException e) {
            }
        }
    }

    private void addSystemApps(List<SelectableAppInfo> list, Intent intent, Set<String> set) {
        int applicationEnabledSetting;
        ApplicationInfo appInfoForUser;
        PackageManager packageManager = this.mPackageManager;
        for (ResolveInfo resolveInfo : packageManager.queryIntentActivities(intent, 8704)) {
            if (resolveInfo.activityInfo != null && resolveInfo.activityInfo.applicationInfo != null) {
                String str = resolveInfo.activityInfo.packageName;
                int i = resolveInfo.activityInfo.applicationInfo.flags;
                if ((i & 1) != 0 || (i & 128) != 0) {
                    if (!set.contains(str) && (((applicationEnabledSetting = packageManager.getApplicationEnabledSetting(str)) != 4 && applicationEnabledSetting != 2) || ((appInfoForUser = getAppInfoForUser(str, 0, this.mUser)) != null && (appInfoForUser.flags & 8388608) != 0))) {
                        SelectableAppInfo selectableAppInfo = new SelectableAppInfo();
                        selectableAppInfo.packageName = resolveInfo.activityInfo.packageName;
                        selectableAppInfo.appName = resolveInfo.activityInfo.applicationInfo.loadLabel(packageManager);
                        selectableAppInfo.icon = resolveInfo.activityInfo.loadIcon(packageManager);
                        selectableAppInfo.activityName = resolveInfo.activityInfo.loadLabel(packageManager);
                        if (selectableAppInfo.activityName == null) {
                            selectableAppInfo.activityName = selectableAppInfo.appName;
                        }
                        list.add(selectableAppInfo);
                    }
                }
            }
        }
    }

    private boolean isSystemPackage(String str) {
        try {
            PackageInfo packageInfo = this.mPackageManager.getPackageInfo(str, 0);
            if (packageInfo.applicationInfo == null) {
                return false;
            }
            int i = packageInfo.applicationInfo.flags;
            if ((i & 1) != 0 || (i & 128) != 0) {
                return true;
            }
        } catch (PackageManager.NameNotFoundException e) {
        }
        return false;
    }

    private ApplicationInfo getAppInfoForUser(String str, int i, UserHandle userHandle) {
        try {
            return this.mIPm.getApplicationInfo(str, i, userHandle.getIdentifier());
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
            return this.packageName + ": appName=" + ((Object) this.appName) + "; activityName=" + ((Object) this.activityName) + "; icon=" + this.icon + "; masterEntry=" + this.masterEntry;
        }
    }

    private static class AppLabelComparator implements Comparator<SelectableAppInfo> {
        private AppLabelComparator() {
        }

        @Override
        public int compare(SelectableAppInfo selectableAppInfo, SelectableAppInfo selectableAppInfo2) {
            return selectableAppInfo.activityName.toString().toLowerCase().compareTo(selectableAppInfo2.activityName.toString().toLowerCase());
        }
    }

    static class Injector {
        private Context mContext;
        private UserHandle mUser;

        Injector(Context context, UserHandle userHandle) {
            this.mContext = context;
            this.mUser = userHandle;
        }

        Context getContext() {
            return this.mContext;
        }

        UserHandle getUser() {
            return this.mUser;
        }

        PackageManager getPackageManager() {
            return this.mContext.getPackageManager();
        }

        IPackageManager getIPackageManager() {
            return AppGlobals.getPackageManager();
        }

        UserManager getUserManager() {
            return (UserManager) this.mContext.getSystemService(UserManager.class);
        }

        List<InputMethodInfo> getInputMethodList() {
            return ((InputMethodManager) getContext().getSystemService("input_method")).getInputMethodList();
        }
    }
}
