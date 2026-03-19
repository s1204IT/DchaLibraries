package com.android.packageinstaller.permission.model;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.BidiFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

public final class AppPermissions {
    private final CharSequence mAppLabel;
    private final Context mContext;
    private final String[] mFilterPermissions;
    private final ArrayList<AppPermissionGroup> mGroups = new ArrayList<>();
    private final LinkedHashMap<String, AppPermissionGroup> mNameToGroupMap = new LinkedHashMap<>();
    private final Runnable mOnErrorCallback;
    private PackageInfo mPackageInfo;
    private final boolean mSortGroups;

    public AppPermissions(Context context, PackageInfo packageInfo, String[] strArr, boolean z, Runnable runnable) {
        this.mContext = context;
        this.mPackageInfo = packageInfo;
        this.mFilterPermissions = strArr;
        this.mAppLabel = BidiFormatter.getInstance().unicodeWrap(packageInfo.applicationInfo.loadSafeLabel(context.getPackageManager()).toString());
        this.mSortGroups = z;
        this.mOnErrorCallback = runnable;
        loadPermissionGroups();
    }

    public PackageInfo getPackageInfo() {
        return this.mPackageInfo;
    }

    public void refresh() {
        loadPackageInfo();
        loadPermissionGroups();
    }

    public CharSequence getAppLabel() {
        return this.mAppLabel;
    }

    public AppPermissionGroup getPermissionGroup(String str) {
        return this.mNameToGroupMap.get(str);
    }

    public List<AppPermissionGroup> getPermissionGroups() {
        return this.mGroups;
    }

    public boolean isReviewRequired() {
        if (!this.mContext.getPackageManager().isPermissionReviewModeEnabled()) {
            return false;
        }
        int size = this.mGroups.size();
        for (int i = 0; i < size; i++) {
            if (this.mGroups.get(i).isReviewRequired()) {
                return true;
            }
        }
        return false;
    }

    private void loadPackageInfo() {
        try {
            this.mPackageInfo = this.mContext.getPackageManager().getPackageInfo(this.mPackageInfo.packageName, 4096);
        } catch (PackageManager.NameNotFoundException e) {
            if (this.mOnErrorCallback != null) {
                this.mOnErrorCallback.run();
            }
        }
    }

    private void loadPermissionGroups() {
        this.mGroups.clear();
        if (this.mPackageInfo.requestedPermissions == null) {
            return;
        }
        if (this.mFilterPermissions != null) {
            for (String str : this.mFilterPermissions) {
                String[] strArr = this.mPackageInfo.requestedPermissions;
                int length = strArr.length;
                int i = 0;
                while (true) {
                    if (i < length) {
                        String str2 = strArr[i];
                        if (!str.equals(str2)) {
                            i++;
                        } else {
                            addPermissionGroupIfNeeded(str2);
                            break;
                        }
                    }
                }
            }
        } else {
            for (String str3 : this.mPackageInfo.requestedPermissions) {
                addPermissionGroupIfNeeded(str3);
            }
        }
        if (this.mSortGroups) {
            Collections.sort(this.mGroups);
        }
        this.mNameToGroupMap.clear();
        for (AppPermissionGroup appPermissionGroup : this.mGroups) {
            this.mNameToGroupMap.put(appPermissionGroup.getName(), appPermissionGroup);
        }
    }

    private void addPermissionGroupIfNeeded(String str) {
        AppPermissionGroup appPermissionGroupCreate;
        if (getGroupForPermission(str) != null || (appPermissionGroupCreate = AppPermissionGroup.create(this.mContext, this.mPackageInfo, str)) == null) {
            return;
        }
        this.mGroups.add(appPermissionGroupCreate);
    }

    public AppPermissionGroup getGroupForPermission(String str) {
        for (AppPermissionGroup appPermissionGroup : this.mGroups) {
            if (appPermissionGroup.hasPermission(str)) {
                return appPermissionGroup;
            }
        }
        return null;
    }
}
