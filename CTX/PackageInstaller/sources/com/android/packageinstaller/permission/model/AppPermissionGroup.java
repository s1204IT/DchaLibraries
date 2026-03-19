package com.android.packageinstaller.permission.model;

import android.annotation.SystemApi;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.os.Process;
import android.os.UserHandle;
import android.util.ArrayMap;
import com.android.packageinstaller.R;
import com.android.packageinstaller.permission.utils.ArrayUtils;
import com.android.packageinstaller.permission.utils.LocationUtils;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class AppPermissionGroup implements Comparable<AppPermissionGroup> {
    private final ActivityManager mActivityManager;
    private final AppOpsManager mAppOps;
    private final boolean mAppSupportsRuntimePermissions;
    private final Collator mCollator;
    private boolean mContainsEphemeralPermission;
    private boolean mContainsPreRuntimePermission;
    private final Context mContext;
    private final String mDeclaringPackage;
    private final CharSequence mDescription;
    private final String mIconPkg;
    private final int mIconResId;
    private final boolean mIsEphemeralApp;
    private final CharSequence mLabel;
    private final String mName;
    private final PackageInfo mPackageInfo;
    private final PackageManager mPackageManager;
    private final ArrayMap<String, Permission> mPermissions = new ArrayMap<>();
    private final int mRequest;
    private final UserHandle mUserHandle;

    public static AppPermissionGroup create(Context context, PackageInfo packageInfo, String str) throws PackageManager.NameNotFoundException {
        List<PermissionInfo> listQueryPermissionsByGroup = null;
        try {
            PermissionInfo permissionInfo = context.getPackageManager().getPermissionInfo(str, 0);
            if ((permissionInfo.protectionLevel & 15) != 1 || (permissionInfo.flags & 1073741824) == 0 || (permissionInfo.flags & 2) != 0) {
                return null;
            }
            if (permissionInfo.group != null) {
                try {
                    permissionInfo = context.getPackageManager().getPermissionGroupInfo(permissionInfo.group, 0);
                } catch (PackageManager.NameNotFoundException e) {
                }
            }
            if (permissionInfo instanceof PermissionGroupInfo) {
                try {
                    listQueryPermissionsByGroup = context.getPackageManager().queryPermissionsByGroup(((PackageItemInfo) permissionInfo).name, 0);
                } catch (PackageManager.NameNotFoundException e2) {
                }
            }
            return create(context, packageInfo, permissionInfo, listQueryPermissionsByGroup, Process.myUserHandle());
        } catch (PackageManager.NameNotFoundException e3) {
            return null;
        }
    }

    public static AppPermissionGroup create(Context context, PackageInfo packageInfo, PackageItemInfo packageItemInfo, List<PermissionInfo> list, UserHandle userHandle) {
        List<PermissionInfo> arrayList;
        PermissionInfo next;
        String strPermissionToOp;
        AppPermissionGroup appPermissionGroup = new AppPermissionGroup(context, packageInfo, packageItemInfo.name, packageItemInfo.packageName, packageItemInfo.loadLabel(context.getPackageManager()), loadGroupDescription(context, packageItemInfo), getRequest(packageItemInfo), packageItemInfo.packageName, packageItemInfo.icon, userHandle);
        if (packageItemInfo instanceof PermissionInfo) {
            arrayList = new ArrayList<>();
            arrayList.add((PermissionInfo) packageItemInfo);
        } else {
            arrayList = list;
        }
        if (arrayList == null || arrayList.isEmpty()) {
            return null;
        }
        int length = packageInfo.requestedPermissions.length;
        for (int i = 0; i < length; i++) {
            String str = packageInfo.requestedPermissions[i];
            Iterator<PermissionInfo> it = arrayList.iterator();
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
            if (next != null && (next.protectionLevel & 15) == 1 && (packageInfo.applicationInfo.targetSdkVersion > 22 || "android".equals(packageItemInfo.packageName))) {
                boolean z = (packageInfo.requestedPermissionsFlags[i] & 2) != 0;
                if ("android".equals(next.packageName)) {
                    strPermissionToOp = AppOpsManager.permissionToOp(next.name);
                } else {
                    strPermissionToOp = null;
                }
                appPermissionGroup.addPermission(new Permission(str, z, strPermissionToOp, strPermissionToOp != null && ((AppOpsManager) context.getSystemService(AppOpsManager.class)).checkOpNoThrow(strPermissionToOp, packageInfo.applicationInfo.uid, packageInfo.packageName) == 0, context.getPackageManager().getPermissionFlags(str, packageInfo.packageName, userHandle), next.protectionLevel));
            }
        }
        return appPermissionGroup;
    }

    private static int getRequest(PackageItemInfo packageItemInfo) {
        if (packageItemInfo instanceof PermissionGroupInfo) {
            return ((PermissionGroupInfo) packageItemInfo).requestRes;
        }
        if (packageItemInfo instanceof PermissionInfo) {
            return ((PermissionInfo) packageItemInfo).requestRes;
        }
        return 0;
    }

    private static CharSequence loadGroupDescription(Context context, PackageItemInfo packageItemInfo) {
        CharSequence charSequenceLoadDescription;
        if (packageItemInfo instanceof PermissionGroupInfo) {
            charSequenceLoadDescription = ((PermissionGroupInfo) packageItemInfo).loadDescription(context.getPackageManager());
        } else if (packageItemInfo instanceof PermissionInfo) {
            charSequenceLoadDescription = ((PermissionInfo) packageItemInfo).loadDescription(context.getPackageManager());
        } else {
            charSequenceLoadDescription = null;
        }
        if (charSequenceLoadDescription == null || charSequenceLoadDescription.length() <= 0) {
            return context.getString(R.string.default_permission_description);
        }
        return charSequenceLoadDescription;
    }

    private AppPermissionGroup(Context context, PackageInfo packageInfo, String str, String str2, CharSequence charSequence, CharSequence charSequence2, int i, String str3, int i2, UserHandle userHandle) {
        this.mContext = context;
        this.mUserHandle = userHandle;
        this.mPackageManager = this.mContext.getPackageManager();
        this.mPackageInfo = packageInfo;
        this.mAppSupportsRuntimePermissions = packageInfo.applicationInfo.targetSdkVersion > 22;
        this.mIsEphemeralApp = packageInfo.applicationInfo.isInstantApp();
        this.mAppOps = (AppOpsManager) context.getSystemService(AppOpsManager.class);
        this.mActivityManager = (ActivityManager) context.getSystemService(ActivityManager.class);
        this.mDeclaringPackage = str2;
        this.mName = str;
        this.mLabel = charSequence;
        this.mDescription = charSequence2;
        this.mCollator = Collator.getInstance(context.getResources().getConfiguration().getLocales().get(0));
        this.mRequest = i;
        if (i2 != 0) {
            this.mIconPkg = str3;
            this.mIconResId = i2;
        } else {
            this.mIconPkg = context.getPackageName();
            this.mIconResId = R.drawable.ic_perm_device_info;
        }
    }

    public boolean doesSupportRuntimePermissions() {
        return this.mAppSupportsRuntimePermissions;
    }

    public boolean isGrantingAllowed() {
        return (!this.mIsEphemeralApp || this.mContainsEphemeralPermission) && (this.mAppSupportsRuntimePermissions || this.mContainsPreRuntimePermission);
    }

    public boolean isReviewRequired() {
        if (this.mAppSupportsRuntimePermissions) {
            return false;
        }
        int size = this.mPermissions.size();
        for (int i = 0; i < size; i++) {
            if (this.mPermissions.valueAt(i).isReviewRequired()) {
                return true;
            }
        }
        return false;
    }

    public void resetReviewRequired() {
        int size = this.mPermissions.size();
        for (int i = 0; i < size; i++) {
            Permission permissionValueAt = this.mPermissions.valueAt(i);
            if (permissionValueAt.isReviewRequired()) {
                permissionValueAt.resetReviewRequired();
                this.mPackageManager.updatePermissionFlags(permissionValueAt.getName(), this.mPackageInfo.packageName, 64, 0, this.mUserHandle);
            }
        }
    }

    public boolean hasGrantedByDefaultPermission() {
        int size = this.mPermissions.size();
        for (int i = 0; i < size; i++) {
            if (this.mPermissions.valueAt(i).isGrantedByDefault()) {
                return true;
            }
        }
        return false;
    }

    public PackageInfo getApp() {
        return this.mPackageInfo;
    }

    public String getName() {
        return this.mName;
    }

    public String getDeclaringPackage() {
        return this.mDeclaringPackage;
    }

    public String getIconPkg() {
        return this.mIconPkg;
    }

    public int getIconResId() {
        return this.mIconResId;
    }

    public CharSequence getLabel() {
        return this.mLabel;
    }

    @SystemApi
    public int getRequest() {
        return this.mRequest;
    }

    public CharSequence getDescription() {
        return this.mDescription;
    }

    public int getUserId() {
        return this.mUserHandle.getIdentifier();
    }

    public boolean hasPermission(String str) {
        return this.mPermissions.get(str) != null;
    }

    public boolean areRuntimePermissionsGranted() {
        return areRuntimePermissionsGranted(null);
    }

    public boolean areRuntimePermissionsGranted(String[] strArr) {
        if (LocationUtils.isLocationGroupAndProvider(this.mName, this.mPackageInfo.packageName)) {
            return LocationUtils.isLocationEnabled(this.mContext);
        }
        int size = this.mPermissions.size();
        for (int i = 0; i < size; i++) {
            Permission permissionValueAt = this.mPermissions.valueAt(i);
            if (strArr == null || ArrayUtils.contains(strArr, permissionValueAt.getName())) {
                if (this.mAppSupportsRuntimePermissions) {
                    if (permissionValueAt.isGranted()) {
                        return true;
                    }
                } else if (permissionValueAt.isGranted() && ((permissionValueAt.getAppOp() == null || permissionValueAt.isAppOpAllowed()) && !permissionValueAt.isReviewRequired())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean grantRuntimePermissions(boolean z) {
        return grantRuntimePermissions(z, null);
    }

    public boolean grantRuntimePermissions(boolean z, String[] strArr) {
        int i;
        int i2 = this.mPackageInfo.applicationInfo.uid;
        for (Permission permission : this.mPermissions.values()) {
            if (strArr == null || ArrayUtils.contains(strArr, permission.getName())) {
                if (permission.isGrantingAllowed(this.mIsEphemeralApp, this.mAppSupportsRuntimePermissions)) {
                    int i3 = 0;
                    if (this.mAppSupportsRuntimePermissions) {
                        if (permission.isSystemFixed()) {
                            return false;
                        }
                        if (permission.hasAppOp() && !permission.isAppOpAllowed()) {
                            permission.setAppOpAllowed(true);
                            this.mAppOps.setUidMode(permission.getAppOp(), i2, 0);
                        }
                        if (!permission.isGranted()) {
                            permission.setGranted(true);
                            this.mPackageManager.grantRuntimePermission(this.mPackageInfo.packageName, permission.getName(), this.mUserHandle);
                        }
                        if (!z && (permission.isUserFixed() || permission.isUserSet())) {
                            permission.setUserFixed(false);
                            permission.setUserSet(false);
                            this.mPackageManager.updatePermissionFlags(permission.getName(), this.mPackageInfo.packageName, 3, 0, this.mUserHandle);
                        }
                    } else if (permission.isGranted()) {
                        if (permission.hasAppOp()) {
                            if (permission.isAppOpAllowed()) {
                                i = -1;
                            } else {
                                permission.setAppOpAllowed(true);
                                this.mAppOps.setUidMode(permission.getAppOp(), i2, 0);
                                i = i2;
                            }
                            if (permission.shouldRevokeOnUpgrade()) {
                                permission.setRevokeOnUpgrade(false);
                                i3 = 8;
                            }
                        } else {
                            i = -1;
                        }
                        if (permission.isReviewRequired()) {
                            permission.resetReviewRequired();
                            i3 |= 64;
                        }
                        int i4 = i3;
                        if (i4 != 0) {
                            this.mPackageManager.updatePermissionFlags(permission.getName(), this.mPackageInfo.packageName, i4, 0, this.mUserHandle);
                        }
                        if (i != -1) {
                            this.mActivityManager.killUid(i2, "Permission related app op changed");
                        }
                    }
                } else {
                    continue;
                }
            }
        }
        return true;
    }

    public boolean revokeRuntimePermissions(boolean z) {
        return revokeRuntimePermissions(z, null);
    }

    public boolean revokeRuntimePermissions(boolean z, String[] strArr) {
        int i;
        int i2;
        int i3;
        int i4 = this.mPackageInfo.applicationInfo.uid;
        for (Permission permission : this.mPermissions.values()) {
            if (strArr == null || ArrayUtils.contains(strArr, permission.getName())) {
                int i5 = 0;
                if (this.mAppSupportsRuntimePermissions) {
                    if (permission.isSystemFixed()) {
                        return false;
                    }
                    if (permission.isGranted()) {
                        permission.setGranted(false);
                        this.mPackageManager.revokeRuntimePermission(this.mPackageInfo.packageName, permission.getName(), this.mUserHandle);
                    }
                    if (z) {
                        if (permission.isUserSet() || !permission.isUserFixed()) {
                            permission.setUserSet(false);
                            permission.setUserFixed(true);
                            this.mPackageManager.updatePermissionFlags(permission.getName(), this.mPackageInfo.packageName, 3, 2, this.mUserHandle);
                        }
                    } else if (!permission.isUserSet() || permission.isUserFixed()) {
                        permission.setUserSet(true);
                        permission.setUserFixed(false);
                        this.mPackageManager.updatePermissionFlags(permission.getName(), this.mPackageInfo.packageName, 3, 1, this.mUserHandle);
                    }
                } else if (permission.isGranted()) {
                    if (permission.hasAppOp()) {
                        if (permission.isAppOpAllowed()) {
                            permission.setAppOpAllowed(false);
                            this.mAppOps.setUidMode(permission.getAppOp(), i4, 1);
                            i3 = i4;
                        } else {
                            i3 = -1;
                        }
                        if (!permission.shouldRevokeOnUpgrade()) {
                            permission.setRevokeOnUpgrade(true);
                            i5 = 8;
                        }
                        i = i5;
                        i2 = i;
                    } else {
                        i = 0;
                        i2 = 0;
                        i3 = -1;
                    }
                    if (i != 0) {
                        this.mPackageManager.updatePermissionFlags(permission.getName(), this.mPackageInfo.packageName, i, i2, this.mUserHandle);
                    }
                    if (i3 != -1) {
                        this.mActivityManager.killUid(i4, "Permission related app op changed");
                    }
                }
            }
        }
        return true;
    }

    public void setPolicyFixed() {
        int size = this.mPermissions.size();
        for (int i = 0; i < size; i++) {
            Permission permissionValueAt = this.mPermissions.valueAt(i);
            permissionValueAt.setPolicyFixed(true);
            this.mPackageManager.updatePermissionFlags(permissionValueAt.getName(), this.mPackageInfo.packageName, 4, 4, this.mUserHandle);
        }
    }

    public List<Permission> getPermissions() {
        return new ArrayList(this.mPermissions.values());
    }

    public boolean isUserFixed() {
        int size = this.mPermissions.size();
        for (int i = 0; i < size; i++) {
            if (this.mPermissions.valueAt(i).isUserFixed()) {
                return true;
            }
        }
        return false;
    }

    public boolean isPolicyFixed() {
        int size = this.mPermissions.size();
        for (int i = 0; i < size; i++) {
            if (this.mPermissions.valueAt(i).isPolicyFixed()) {
                return true;
            }
        }
        return false;
    }

    public boolean isUserSet() {
        int size = this.mPermissions.size();
        for (int i = 0; i < size; i++) {
            if (this.mPermissions.valueAt(i).isUserSet()) {
                return true;
            }
        }
        return false;
    }

    public boolean isSystemFixed() {
        int size = this.mPermissions.size();
        for (int i = 0; i < size; i++) {
            if (this.mPermissions.valueAt(i).isSystemFixed()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int compareTo(AppPermissionGroup appPermissionGroup) {
        int iCompare = this.mCollator.compare(this.mLabel.toString(), appPermissionGroup.mLabel.toString());
        if (iCompare == 0) {
            return this.mPackageInfo.applicationInfo.uid - appPermissionGroup.mPackageInfo.applicationInfo.uid;
        }
        return iCompare;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        AppPermissionGroup appPermissionGroup = (AppPermissionGroup) obj;
        if (this.mName == null) {
            if (appPermissionGroup.mName != null) {
                return false;
            }
        } else if (!this.mName.equals(appPermissionGroup.mName)) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        if (this.mName != null) {
            return this.mName.hashCode();
        }
        return 0;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append("{name=");
        sb.append(this.mName);
        if (!this.mPermissions.isEmpty()) {
            sb.append(", <has permissions>}");
        } else {
            sb.append('}');
        }
        return sb.toString();
    }

    private void addPermission(Permission permission) {
        this.mPermissions.put(permission.getName(), permission);
        if (permission.isEphemeral()) {
            this.mContainsEphemeralPermission = true;
        }
        if (!permission.isRuntimeOnly()) {
            this.mContainsPreRuntimePermission = true;
        }
    }
}
