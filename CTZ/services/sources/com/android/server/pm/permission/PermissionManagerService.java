package com.android.server.pm.permission;

import android.content.Context;
import android.content.pm.PackageManagerInternal;
import android.content.pm.PackageParser;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.metrics.LogMaker;
import android.os.BenesseExtension;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManagerInternal;
import android.os.storage.StorageManager;
import android.os.storage.StorageManagerInternal;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.EventLog;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.os.RoSystemProperties;
import com.android.internal.util.ArrayUtils;
import com.android.server.LocalServices;
import com.android.server.ServiceThread;
import com.android.server.SystemConfig;
import com.android.server.Watchdog;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.PackageManagerServiceUtils;
import com.android.server.pm.PackageSetting;
import com.android.server.pm.SharedUserSetting;
import com.android.server.pm.UserManagerService;
import com.android.server.pm.permission.DefaultPermissionGrantPolicy;
import com.android.server.pm.permission.PermissionManagerInternal;
import com.android.server.pm.permission.PermissionsState;
import com.mediatek.cta.CtaManager;
import com.mediatek.cta.CtaManagerFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import libcore.util.EmptyArray;

public class PermissionManagerService {
    private static final int GRANT_DENIED = 1;
    private static final int GRANT_INSTALL = 2;
    private static final int GRANT_RUNTIME = 3;
    private static final int GRANT_UPGRADE = 4;
    private static final int MAX_PERMISSION_TREE_FOOTPRINT = 32768;
    private static final String TAG = "PackageManager";
    private static final int UPDATE_PERMISSIONS_ALL = 1;
    private static final int UPDATE_PERMISSIONS_REPLACE_ALL = 4;
    private static final int UPDATE_PERMISSIONS_REPLACE_PKG = 2;
    private final Context mContext;
    private final DefaultPermissionGrantPolicy mDefaultPermissionGrantPolicy;
    private final int[] mGlobalGids;
    private final Handler mHandler;
    private final Object mLock;

    @GuardedBy("mLock")
    private ArraySet<String> mPrivappPermissionsViolations;

    @GuardedBy("mLock")
    private final PermissionSettings mSettings;
    private final SparseArray<ArraySet<String>> mSystemPermissions;

    @GuardedBy("mLock")
    private boolean mSystemReady;
    private static final int[] EMPTY_INT_ARRAY = new int[0];
    private static final CtaManager sCtaManager = CtaManagerFactory.getInstance().makeCtaManager();
    private final MetricsLogger mMetricsLogger = new MetricsLogger();
    private final PackageManagerInternal mPackageManagerInt = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
    private final UserManagerInternal mUserManagerInt = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
    private final HandlerThread mHandlerThread = new ServiceThread(TAG, 10, true);

    PermissionManagerService(Context context, DefaultPermissionGrantPolicy.DefaultPermissionGrantedCallback defaultPermissionGrantedCallback, Object obj) {
        this.mContext = context;
        this.mLock = obj;
        this.mSettings = new PermissionSettings(context, this.mLock);
        this.mHandlerThread.start();
        this.mHandler = new Handler(this.mHandlerThread.getLooper());
        Watchdog.getInstance().addThread(this.mHandler);
        this.mDefaultPermissionGrantPolicy = new DefaultPermissionGrantPolicy(context, this.mHandlerThread.getLooper(), defaultPermissionGrantedCallback, this);
        SystemConfig systemConfig = SystemConfig.getInstance();
        this.mSystemPermissions = systemConfig.getSystemPermissions();
        this.mGlobalGids = systemConfig.getGlobalGids();
        ArrayMap permissions = SystemConfig.getInstance().getPermissions();
        synchronized (this.mLock) {
            for (int i = 0; i < permissions.size(); i++) {
                SystemConfig.PermissionEntry permissionEntry = (SystemConfig.PermissionEntry) permissions.valueAt(i);
                BasePermission permissionLocked = this.mSettings.getPermissionLocked(permissionEntry.name);
                if (permissionLocked == null) {
                    permissionLocked = new BasePermission(permissionEntry.name, PackageManagerService.PLATFORM_PACKAGE_NAME, 1);
                    this.mSettings.putPermissionLocked(permissionEntry.name, permissionLocked);
                }
                if (permissionEntry.gids != null) {
                    permissionLocked.setGids(permissionEntry.gids, permissionEntry.perUser);
                }
            }
        }
        LocalServices.addService(PermissionManagerInternal.class, new PermissionManagerInternalImpl());
    }

    public static PermissionManagerInternal create(Context context, DefaultPermissionGrantPolicy.DefaultPermissionGrantedCallback defaultPermissionGrantedCallback, Object obj) {
        PermissionManagerInternal permissionManagerInternal = (PermissionManagerInternal) LocalServices.getService(PermissionManagerInternal.class);
        if (permissionManagerInternal != null) {
            return permissionManagerInternal;
        }
        new PermissionManagerService(context, defaultPermissionGrantedCallback, obj);
        return (PermissionManagerInternal) LocalServices.getService(PermissionManagerInternal.class);
    }

    BasePermission getPermission(String str) {
        BasePermission permissionLocked;
        synchronized (this.mLock) {
            permissionLocked = this.mSettings.getPermissionLocked(str);
        }
        return permissionLocked;
    }

    private int checkPermission(String str, String str2, int i, int i2) {
        PackageParser.Package r5;
        if (!this.mUserManagerInt.exists(i2) || (r5 = this.mPackageManagerInt.getPackage(str2)) == null || r5.mExtras == null || this.mPackageManagerInt.filterAppAccess(r5, i, i2)) {
            return -1;
        }
        PackageSetting packageSetting = (PackageSetting) r5.mExtras;
        boolean instantApp = packageSetting.getInstantApp(i2);
        PermissionsState permissionsState = packageSetting.getPermissionsState();
        if (permissionsState.hasPermission(str, i2)) {
            if (!instantApp) {
                return 0;
            }
            synchronized (this.mLock) {
                BasePermission permissionLocked = this.mSettings.getPermissionLocked(str);
                if (permissionLocked != null && permissionLocked.isInstant()) {
                    return 0;
                }
            }
        }
        return ("android.permission.ACCESS_COARSE_LOCATION".equals(str) && permissionsState.hasPermission("android.permission.ACCESS_FINE_LOCATION", i2)) ? 0 : -1;
    }

    private int checkUidPermission(String str, PackageParser.Package r9, int i, int i2) {
        int userId = UserHandle.getUserId(i2);
        boolean z = this.mPackageManagerInt.getInstantAppPackageName(i2) != null;
        boolean z2 = this.mPackageManagerInt.getInstantAppPackageName(i) != null;
        int userId2 = UserHandle.getUserId(i);
        if (!this.mUserManagerInt.exists(userId2)) {
            return -1;
        }
        if (r9 != null) {
            if (r9.mSharedUserId != null) {
                if (z) {
                    return -1;
                }
            } else if (this.mPackageManagerInt.filterAppAccess(r9, i2, userId)) {
                return -1;
            }
            PermissionsState permissionsState = ((PackageSetting) r9.mExtras).getPermissionsState();
            if (permissionsState.hasPermission(str, userId2) && (!z2 || this.mSettings.isPermissionInstant(str))) {
                return 0;
            }
            if ("android.permission.ACCESS_COARSE_LOCATION".equals(str) && permissionsState.hasPermission("android.permission.ACCESS_FINE_LOCATION", userId2)) {
                return 0;
            }
        } else {
            ArraySet<String> arraySet = this.mSystemPermissions.get(i);
            if (arraySet != null) {
                if (arraySet.contains(str)) {
                    return 0;
                }
                if ("android.permission.ACCESS_COARSE_LOCATION".equals(str) && arraySet.contains("android.permission.ACCESS_FINE_LOCATION")) {
                    return 0;
                }
            }
        }
        return -1;
    }

    private PermissionGroupInfo getPermissionGroupInfo(String str, int i, int i2) {
        PermissionGroupInfo permissionGroupInfoGeneratePermissionGroupInfo;
        if (this.mPackageManagerInt.getInstantAppPackageName(i2) != null) {
            return null;
        }
        synchronized (this.mLock) {
            permissionGroupInfoGeneratePermissionGroupInfo = PackageParser.generatePermissionGroupInfo(this.mSettings.mPermissionGroups.get(str), i);
        }
        return permissionGroupInfoGeneratePermissionGroupInfo;
    }

    private List<PermissionGroupInfo> getAllPermissionGroups(int i, int i2) {
        ArrayList arrayList;
        if (this.mPackageManagerInt.getInstantAppPackageName(i2) != null) {
            return null;
        }
        synchronized (this.mLock) {
            arrayList = new ArrayList(this.mSettings.mPermissionGroups.size());
            Iterator<PackageParser.PermissionGroup> it = this.mSettings.mPermissionGroups.values().iterator();
            while (it.hasNext()) {
                arrayList.add(PackageParser.generatePermissionGroupInfo(it.next(), i));
            }
        }
        return arrayList;
    }

    private PermissionInfo getPermissionInfo(String str, String str2, int i, int i2) {
        if (this.mPackageManagerInt.getInstantAppPackageName(i2) != null) {
            return null;
        }
        synchronized (this.mLock) {
            BasePermission permissionLocked = this.mSettings.getPermissionLocked(str);
            if (permissionLocked == null) {
                return null;
            }
            return permissionLocked.generatePermissionInfo(adjustPermissionProtectionFlagsLocked(permissionLocked.getProtectionLevel(), str2, i2), i);
        }
    }

    private List<PermissionInfo> getPermissionInfoByGroup(String str, int i, int i2) {
        if (this.mPackageManagerInt.getInstantAppPackageName(i2) != null) {
            return null;
        }
        synchronized (this.mLock) {
            if (str != null) {
                try {
                    if (!this.mSettings.mPermissionGroups.containsKey(str)) {
                        return null;
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
            ArrayList arrayList = new ArrayList(10);
            Iterator<BasePermission> it = this.mSettings.mPermissions.values().iterator();
            while (it.hasNext()) {
                PermissionInfo permissionInfoGeneratePermissionInfo = it.next().generatePermissionInfo(str, i);
                if (permissionInfoGeneratePermissionInfo != null) {
                    arrayList.add(permissionInfoGeneratePermissionInfo);
                }
            }
            return arrayList;
        }
    }

    private int adjustPermissionProtectionFlagsLocked(int i, String str, int i2) {
        int appId;
        PackageParser.Package r5;
        int i3 = i & 3;
        if (i3 == 2 || (appId = UserHandle.getAppId(i2)) == 1000 || appId == 0 || appId == 2000 || (r5 = this.mPackageManagerInt.getPackage(str)) == null) {
            return i;
        }
        if (r5.applicationInfo.targetSdkVersion < 26) {
            return i3;
        }
        PackageSetting packageSetting = (PackageSetting) r5.mExtras;
        if (packageSetting != null && packageSetting.getAppId() != appId) {
            return i;
        }
        return i;
    }

    private void revokeRuntimePermissionsIfGroupChanged(PackageParser.Package r24, PackageParser.Package r25, ArrayList<String> arrayList, PermissionManagerInternal.PermissionCallback permissionCallback) {
        int i;
        int i2;
        int i3;
        String str;
        String str2;
        int size = r25.permissions.size();
        ArrayMap arrayMap = new ArrayMap(size);
        int i4 = 0;
        for (int i5 = 0; i5 < size; i5++) {
            PackageParser.Permission permission = (PackageParser.Permission) r25.permissions.get(i5);
            if (permission.group != null) {
                arrayMap.put(permission.info.name, permission.group.info.name);
            }
        }
        int size2 = r24.permissions.size();
        int i6 = 0;
        while (i6 < size2) {
            PackageParser.Permission permission2 = (PackageParser.Permission) r24.permissions.get(i6);
            char c = 1;
            if ((permission2.info.getProtection() & 1) != 0) {
                String str3 = permission2.info.name;
                String str4 = permission2.group == null ? null : permission2.group.info.name;
                String str5 = (String) arrayMap.get(str3);
                if (str4 != null && !str4.equals(str5)) {
                    int[] userIds = this.mUserManagerInt.getUserIds();
                    int length = userIds.length;
                    int i7 = i4;
                    while (i7 < length) {
                        int i8 = userIds[i7];
                        int size3 = arrayList.size();
                        int[] iArr = userIds;
                        int i9 = i4;
                        while (i9 < size3) {
                            int i10 = length;
                            int i11 = i9;
                            String str6 = arrayList.get(i9);
                            if (checkPermission(str3, str6, i4, i8) != 0) {
                                i = size3;
                                i2 = i8;
                                i3 = i7;
                                str = str5;
                                str2 = str4;
                            } else {
                                Object[] objArr = new Object[3];
                                objArr[i4] = "72710897";
                                objArr[c] = Integer.valueOf(r24.applicationInfo.uid);
                                objArr[2] = "Revoking permission " + str3 + " from package " + str6 + " as the group changed from " + str5 + " to " + str4;
                                EventLog.writeEvent(1397638484, objArr);
                                i = size3;
                                i2 = i8;
                                i3 = i7;
                                str = str5;
                                str2 = str4;
                                try {
                                    revokeRuntimePermission(str3, str6, false, 1000, i2, permissionCallback);
                                } catch (IllegalArgumentException e) {
                                    Slog.e(TAG, "Could not revoke " + str3 + " from " + str6, e);
                                }
                            }
                            i9 = i11 + 1;
                            str5 = str;
                            size3 = i;
                            length = i10;
                            i8 = i2;
                            i7 = i3;
                            str4 = str2;
                            i4 = 0;
                            c = 1;
                        }
                        i7++;
                        userIds = iArr;
                        i4 = 0;
                        c = 1;
                    }
                }
            }
            i6++;
            i4 = 0;
        }
    }

    private void revokeRuntimePermissionsIfPermissionDefinitionChanged(List<String> list, ArrayList<String> arrayList, PermissionManagerInternal.PermissionCallback permissionCallback) {
        int i;
        int i2;
        int i3;
        BasePermission basePermission;
        String str;
        PermissionManagerService permissionManagerService = this;
        int[] userIds = permissionManagerService.mUserManagerInt.getUserIds();
        int size = list.size();
        int length = userIds.length;
        int size2 = arrayList.size();
        int callingUid = Binder.getCallingUid();
        int i4 = 0;
        int i5 = 0;
        while (i5 < size) {
            String str2 = list.get(i5);
            BasePermission permission = permissionManagerService.mSettings.getPermission(str2);
            if (permission == null || !permission.isRuntime()) {
                i = i4;
            } else {
                int i6 = i4;
                while (i6 < length) {
                    int i7 = userIds[i6];
                    int i8 = i4;
                    while (i8 < size2) {
                        int i9 = i6;
                        String str3 = arrayList.get(i8);
                        int packageUid = permissionManagerService.mPackageManagerInt.getPackageUid(str3, i4, i7);
                        if (packageUid < 10000) {
                            i2 = i8;
                            i3 = i7;
                            basePermission = permission;
                            str = str2;
                        } else {
                            int iCheckPermission = permissionManagerService.checkPermission(str2, str3, callingUid, i7);
                            int permissionFlags = permissionManagerService.getPermissionFlags(str2, str3, callingUid, i7);
                            if (iCheckPermission == 0 && (permissionFlags & 52) == 0) {
                                EventLog.writeEvent(1397638484, "154505240", Integer.valueOf(packageUid), "Revoking permission " + str2 + " from package " + str3 + " due to definition change");
                                EventLog.writeEvent(1397638484, "168319670", Integer.valueOf(packageUid), "Revoking permission " + str2 + " from package " + str3 + " due to definition change");
                                Slog.e(TAG, "Revoking permission " + str2 + " from package " + str3 + " due to definition change");
                                i2 = i8;
                                i3 = i7;
                                basePermission = permission;
                                String str4 = str2;
                                try {
                                    permissionManagerService.revokeRuntimePermission(str2, str3, false, callingUid, i3, permissionCallback);
                                    str = str4;
                                } catch (Exception e) {
                                    StringBuilder sb = new StringBuilder();
                                    sb.append("Could not revoke ");
                                    str = str4;
                                    sb.append(str);
                                    sb.append(" from ");
                                    sb.append(str3);
                                    Slog.e(TAG, sb.toString(), e);
                                }
                            }
                        }
                        i8 = i2 + 1;
                        str2 = str;
                        i6 = i9;
                        i7 = i3;
                        permission = basePermission;
                        permissionManagerService = this;
                        i4 = 0;
                    }
                    i6++;
                    permissionManagerService = this;
                    i4 = 0;
                }
                i = 0;
                permission.setPermissionDefinitionChanged(false);
            }
            i5++;
            i4 = i;
            permissionManagerService = this;
        }
    }

    private List<String> addAllPermissions(PackageParser.Package r9, boolean z) {
        BasePermission basePermissionCreateOrUpdate;
        int size = ArrayUtils.size(r9.permissions);
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < size; i++) {
            PackageParser.Permission permission = (PackageParser.Permission) r9.permissions.get(i);
            permission.info.flags &= -1073741825;
            synchronized (this.mLock) {
                if (r9.applicationInfo.targetSdkVersion > 22) {
                    permission.group = this.mSettings.mPermissionGroups.get(permission.info.group);
                    if (PackageManagerService.DEBUG_PERMISSIONS && permission.info.group != null && permission.group == null) {
                        Slog.i(TAG, "Permission " + permission.info.name + " from package " + permission.info.packageName + " in an unknown group " + permission.info.group);
                    }
                }
                if (permission.tree) {
                    basePermissionCreateOrUpdate = BasePermission.createOrUpdate(this.mSettings.getPermissionTreeLocked(permission.info.name), permission, r9, this.mSettings.getAllPermissionTreesLocked(), z);
                    this.mSettings.putPermissionTreeLocked(permission.info.name, basePermissionCreateOrUpdate);
                } else {
                    basePermissionCreateOrUpdate = BasePermission.createOrUpdate(this.mSettings.getPermissionLocked(permission.info.name), permission, r9, this.mSettings.getAllPermissionTreesLocked(), z);
                    this.mSettings.putPermissionLocked(permission.info.name, basePermissionCreateOrUpdate);
                }
                if (basePermissionCreateOrUpdate.isPermissionDefinitionChanged()) {
                    arrayList.add(permission.info.name);
                }
            }
        }
        return arrayList;
    }

    private void addAllPermissionGroups(PackageParser.Package r12, boolean z) {
        String str;
        int size = r12.permissionGroups.size();
        StringBuilder sb = null;
        for (int i = 0; i < size; i++) {
            PackageParser.PermissionGroup permissionGroup = (PackageParser.PermissionGroup) r12.permissionGroups.get(i);
            PackageParser.PermissionGroup permissionGroup2 = this.mSettings.mPermissionGroups.get(permissionGroup.info.name);
            if (permissionGroup2 != null) {
                str = permissionGroup2.info.packageName;
            } else {
                str = null;
            }
            boolean zEquals = permissionGroup.info.packageName.equals(str);
            if (permissionGroup2 == null || zEquals) {
                this.mSettings.mPermissionGroups.put(permissionGroup.info.name, permissionGroup);
                if (z && PackageManagerService.DEBUG_PACKAGE_SCANNING) {
                    if (sb == null) {
                        sb = new StringBuilder(256);
                    } else {
                        sb.append(' ');
                    }
                    if (zEquals) {
                        sb.append("UPD:");
                    }
                    sb.append(permissionGroup.info.name);
                }
            } else {
                Slog.w(TAG, "Permission group " + permissionGroup.info.name + " from package " + permissionGroup.info.packageName + " ignored: original from " + permissionGroup2.info.packageName);
                if (z && PackageManagerService.DEBUG_PACKAGE_SCANNING) {
                    if (sb == null) {
                        sb = new StringBuilder(256);
                    } else {
                        sb.append(' ');
                    }
                    sb.append("DUP:");
                    sb.append(permissionGroup.info.name);
                }
            }
        }
        if (sb != null && PackageManagerService.DEBUG_PACKAGE_SCANNING) {
            Log.d(TAG, "  Permission Groups: " + ((Object) sb));
        }
    }

    private void removeAllPermissions(PackageParser.Package r10, boolean z) {
        ArraySet<String> arraySet;
        ArraySet<String> arraySet2;
        synchronized (this.mLock) {
            int size = r10.permissions.size();
            StringBuilder sb = null;
            for (int i = 0; i < size; i++) {
                PackageParser.Permission permission = (PackageParser.Permission) r10.permissions.get(i);
                BasePermission basePermission = this.mSettings.mPermissions.get(permission.info.name);
                if (basePermission == null) {
                    basePermission = this.mSettings.mPermissionTrees.get(permission.info.name);
                }
                if (basePermission != null && basePermission.isPermission(permission)) {
                    basePermission.setPermission(null);
                    if (PackageManagerService.DEBUG_REMOVE && z) {
                        if (sb == null) {
                            sb = new StringBuilder(256);
                        } else {
                            sb.append(' ');
                        }
                        sb.append(permission.info.name);
                    }
                }
                if (permission.isAppOp() && (arraySet2 = this.mSettings.mAppOpPermissionPackages.get(permission.info.name)) != null) {
                    arraySet2.remove(r10.packageName);
                }
            }
            if (sb != null && PackageManagerService.DEBUG_REMOVE) {
                Log.d(TAG, "  Permissions: " + ((Object) sb));
            }
            int size2 = r10.requestedPermissions.size();
            for (int i2 = 0; i2 < size2; i2++) {
                String str = (String) r10.requestedPermissions.get(i2);
                if (this.mSettings.isPermissionAppOp(str) && (arraySet = this.mSettings.mAppOpPermissionPackages.get(str)) != null) {
                    arraySet.remove(r10.packageName);
                    if (arraySet.isEmpty()) {
                        this.mSettings.mAppOpPermissionPackages.remove(str);
                    }
                }
            }
        }
    }

    private boolean addDynamicPermission(PermissionInfo permissionInfo, int i, PermissionManagerInternal.PermissionCallback permissionCallback) {
        boolean z;
        boolean zAddToTree;
        if (this.mPackageManagerInt.getInstantAppPackageName(i) != null) {
            throw new SecurityException("Instant apps can't add permissions");
        }
        if (permissionInfo.labelRes == 0 && permissionInfo.nonLocalizedLabel == null) {
            throw new SecurityException("Label must be specified in permission");
        }
        BasePermission basePermissionEnforcePermissionTree = this.mSettings.enforcePermissionTree(permissionInfo.name, i);
        synchronized (this.mLock) {
            BasePermission permissionLocked = this.mSettings.getPermissionLocked(permissionInfo.name);
            z = permissionLocked == null;
            int iFixProtectionLevel = PermissionInfo.fixProtectionLevel(permissionInfo.protectionLevel);
            if (z) {
                enforcePermissionCapLocked(permissionInfo, basePermissionEnforcePermissionTree);
                permissionLocked = new BasePermission(permissionInfo.name, basePermissionEnforcePermissionTree.getSourcePackageName(), 2);
            } else if (!permissionLocked.isDynamic()) {
                throw new SecurityException("Not allowed to modify non-dynamic permission " + permissionInfo.name);
            }
            zAddToTree = permissionLocked.addToTree(iFixProtectionLevel, permissionInfo, basePermissionEnforcePermissionTree);
            if (z) {
                this.mSettings.putPermissionLocked(permissionInfo.name, permissionLocked);
            }
        }
        if (zAddToTree && permissionCallback != null) {
            permissionCallback.onPermissionChanged();
        }
        return z;
    }

    private void removeDynamicPermission(String str, int i, PermissionManagerInternal.PermissionCallback permissionCallback) {
        if (this.mPackageManagerInt.getInstantAppPackageName(i) != null) {
            throw new SecurityException("Instant applications don't have access to this method");
        }
        this.mSettings.enforcePermissionTree(str, i);
        synchronized (this.mLock) {
            BasePermission permissionLocked = this.mSettings.getPermissionLocked(str);
            if (permissionLocked == null) {
                return;
            }
            if (permissionLocked.isDynamic()) {
                Slog.wtf(TAG, "Not allowed to modify non-dynamic permission " + str);
            }
            this.mSettings.removePermissionLocked(str);
            if (permissionCallback != null) {
                permissionCallback.onPermissionRemoved();
            }
        }
    }

    private void grantPermissions(PackageParser.Package r28, boolean z, String str, PermissionManagerInternal.PermissionCallback permissionCallback) {
        PermissionsState permissionsState;
        boolean z2;
        int[] iArr;
        boolean z3;
        PackageSetting packageSetting;
        int i;
        String str2;
        PackageSetting packageSetting2;
        boolean z4;
        int[] iArr2;
        boolean z5;
        PackageSetting packageSetting3;
        int i2;
        int[] iArr3;
        int[] iArrAppendInt;
        boolean z6;
        int[] iArrAppendInt2;
        String str3 = str;
        PackageSetting packageSetting4 = (PackageSetting) r28.mExtras;
        if (packageSetting4 == null) {
            return;
        }
        boolean zIsLegacySystemApp = this.mPackageManagerInt.isLegacySystemApp(r28);
        PermissionsState permissionsState2 = packageSetting4.getPermissionsState();
        int[] userIds = UserManagerService.getInstance().getUserIds();
        int[] iArrRevokeUnusedSharedUserPermissionsLocked = EMPTY_INT_ARRAY;
        if (z) {
            packageSetting4.setInstallPermissionsFixed(false);
            if (packageSetting4.isSharedUser()) {
                synchronized (this.mLock) {
                    iArrRevokeUnusedSharedUserPermissionsLocked = revokeUnusedSharedUserPermissionsLocked(packageSetting4.getSharedUser(), UserManagerService.getInstance().getUserIds());
                    z2 = !ArrayUtils.isEmpty(iArrRevokeUnusedSharedUserPermissionsLocked);
                }
                permissionsState = permissionsState2;
                permissionsState2.setGlobalGids(this.mGlobalGids);
                synchronized (this.mLock) {
                    int size = r28.requestedPermissions.size();
                    boolean zIsPackageNeedsReview = isPackageNeedsReview(r28, packageSetting4.getSharedUser());
                    iArr = iArrRevokeUnusedSharedUserPermissionsLocked;
                    int i3 = 0;
                    boolean z7 = false;
                    while (i3 < size) {
                        String str4 = (String) r28.requestedPermissions.get(i3);
                        int i4 = size;
                        BasePermission permissionLocked = this.mSettings.getPermissionLocked(str4);
                        boolean z8 = z2;
                        boolean z9 = r28.applicationInfo.targetSdkVersion >= 23;
                        if (PackageManagerService.DEBUG_INSTALL) {
                            StringBuilder sb = new StringBuilder();
                            i = i3;
                            sb.append("Package ");
                            sb.append(r28.packageName);
                            sb.append(" checking ");
                            sb.append(str4);
                            sb.append(": ");
                            sb.append(permissionLocked);
                            Log.i(TAG, sb.toString());
                        } else {
                            i = i3;
                        }
                        if (permissionLocked == null || permissionLocked.getSourcePackageSetting() == null) {
                            str2 = str3;
                            packageSetting2 = packageSetting4;
                            z4 = zIsLegacySystemApp;
                            iArr2 = userIds;
                            if ((str2 == null || str2.equals(r28.packageName)) && PackageManagerService.DEBUG_PERMISSIONS) {
                                Slog.i(TAG, "Unknown permission " + str4 + " in package " + r28.packageName);
                            }
                        } else {
                            if (!r28.applicationInfo.isInstantApp() || permissionLocked.isInstant()) {
                                if (!permissionLocked.isRuntimeOnly() || z9) {
                                    String name = permissionLocked.getName();
                                    if (permissionLocked.isAppOp()) {
                                        this.mSettings.addAppOpPackage(name, r28.packageName);
                                    }
                                    char c = 4;
                                    if (permissionLocked.isNormal()) {
                                        c = 2;
                                        z5 = false;
                                        if (PackageManagerService.DEBUG_PERMISSIONS) {
                                        }
                                        if (c != 1) {
                                        }
                                        z7 = true;
                                    } else if (!permissionLocked.isRuntime()) {
                                        if (permissionLocked.isSignature()) {
                                            z5 = grantSignaturePermission(name, r28, permissionLocked, permissionsState) || !(packageSetting4.isSystem() || BenesseExtension.getDchaState() == 0);
                                            if (z5) {
                                                c = 2;
                                            }
                                            if (PackageManagerService.DEBUG_PERMISSIONS) {
                                            }
                                            if (c != 1) {
                                            }
                                            z7 = true;
                                        } else {
                                            z5 = false;
                                        }
                                        c = 1;
                                        if (PackageManagerService.DEBUG_PERMISSIONS) {
                                        }
                                        if (c != 1) {
                                        }
                                        z7 = true;
                                    } else if ((z9 || this.mSettings.mPermissionReviewRequired) && (packageSetting4.isSystem() || BenesseExtension.getDchaState() == 0)) {
                                        if (!permissionsState.hasInstallPermission(permissionLocked.getName()) && !zIsLegacySystemApp) {
                                            c = 3;
                                        }
                                        z5 = false;
                                        if (PackageManagerService.DEBUG_PERMISSIONS) {
                                            z4 = zIsLegacySystemApp;
                                            Slog.i(TAG, "Granting permission " + name + " to package " + r28.packageName);
                                        } else {
                                            z4 = zIsLegacySystemApp;
                                        }
                                        if (c != 1) {
                                            if (!packageSetting4.isSystem() && packageSetting4.areInstallPermissionsFixed() && !z5 && !permissionsState.hasInstallPermission(name) && !isNewPlatformPermissionForPackage(name, r28)) {
                                                c = 1;
                                            }
                                            switch (c) {
                                                case 2:
                                                    packageSetting2 = packageSetting4;
                                                    iArr2 = userIds;
                                                    int[] iArrAppendInt3 = iArr;
                                                    for (int i5 : UserManagerService.getInstance().getUserIds()) {
                                                        if (permissionsState.getRuntimePermissionState(name, i5) != null) {
                                                            permissionsState.revokeRuntimePermission(permissionLocked, i5);
                                                            permissionsState.updatePermissionFlags(permissionLocked, i5, 255, 0);
                                                            iArrAppendInt3 = ArrayUtils.appendInt(iArrAppendInt3, i5);
                                                        }
                                                    }
                                                    if (permissionsState2.grantInstallPermission(permissionLocked) != -1) {
                                                        iArr = iArrAppendInt3;
                                                        str2 = str;
                                                    } else {
                                                        iArr = iArrAppendInt3;
                                                        str2 = str;
                                                    }
                                                    break;
                                                case 3:
                                                    int[] userIds2 = UserManagerService.getInstance().getUserIds();
                                                    int length = userIds2.length;
                                                    int[] iArrAppendInt4 = iArr;
                                                    int i6 = 0;
                                                    while (i6 < length) {
                                                        int i7 = userIds2[i6];
                                                        int[] iArr4 = userIds2;
                                                        PermissionsState.PermissionState runtimePermissionState = permissionsState.getRuntimePermissionState(name, i7);
                                                        int flags = runtimePermissionState != null ? runtimePermissionState.getFlags() : 0;
                                                        if (permissionsState.hasRuntimePermission(name, i7)) {
                                                            boolean z10 = (flags & 8) != 0;
                                                            if (z10) {
                                                                flags &= -9;
                                                                iArrAppendInt4 = ArrayUtils.appendInt(iArrAppendInt4, i7);
                                                            }
                                                            i2 = length;
                                                            if (this.mSettings.mPermissionReviewRequired && z10) {
                                                                iArr3 = userIds;
                                                            } else {
                                                                iArr3 = userIds;
                                                                if (permissionsState2.grantRuntimePermission(permissionLocked, i7) == -1) {
                                                                    iArrAppendInt4 = ArrayUtils.appendInt(iArrAppendInt4, i7);
                                                                }
                                                            }
                                                            if (this.mSettings.mPermissionReviewRequired && z9 && (flags & 64) != 0) {
                                                                packageSetting3 = packageSetting4;
                                                                if (sCtaManager.needClearReviewFlagAfterUpgrade(zIsPackageNeedsReview, permissionLocked.getSourcePackageName(), permissionLocked.getName())) {
                                                                    flags &= -65;
                                                                    iArrAppendInt4 = ArrayUtils.appendInt(iArrAppendInt4, i7);
                                                                }
                                                            } else {
                                                                packageSetting3 = packageSetting4;
                                                            }
                                                        } else {
                                                            packageSetting3 = packageSetting4;
                                                            i2 = length;
                                                            iArr3 = userIds;
                                                            if (this.mSettings.mPermissionReviewRequired && !z9) {
                                                                if (sCtaManager.isPlatformPermission(permissionLocked.getSourcePackageName(), permissionLocked.getName()) && zIsPackageNeedsReview && (flags & 64) == 0) {
                                                                    flags |= 64;
                                                                    iArrAppendInt4 = ArrayUtils.appendInt(iArrAppendInt4, i7);
                                                                }
                                                                if (permissionsState2.grantRuntimePermission(permissionLocked, i7) != -1) {
                                                                    iArrAppendInt = ArrayUtils.appendInt(iArrAppendInt4, i7);
                                                                    iArrAppendInt4 = iArrAppendInt;
                                                                }
                                                            } else if (z9 && zIsPackageNeedsReview && sCtaManager.isPlatformPermission(permissionLocked.getSourcePackageName(), permissionLocked.getName()) && (flags & 64) == 0 && (flags & 16) == 0) {
                                                                flags |= 64;
                                                                iArrAppendInt = ArrayUtils.appendInt(iArrAppendInt4, i7);
                                                                iArrAppendInt4 = iArrAppendInt;
                                                            }
                                                        }
                                                        permissionsState2.updatePermissionFlags(permissionLocked, i7, flags, flags);
                                                        i6++;
                                                        userIds2 = iArr4;
                                                        length = i2;
                                                        userIds = iArr3;
                                                        packageSetting4 = packageSetting3;
                                                    }
                                                    packageSetting2 = packageSetting4;
                                                    iArr2 = userIds;
                                                    iArr = iArrAppendInt4;
                                                    str2 = str;
                                                    break;
                                                case 4:
                                                    PermissionsState.PermissionState installPermissionState = permissionsState.getInstallPermissionState(name);
                                                    int flags2 = installPermissionState != null ? installPermissionState.getFlags() : 0;
                                                    if (permissionsState.revokeInstallPermission(permissionLocked) != -1) {
                                                        permissionsState.updatePermissionFlags(permissionLocked, -1, 255, 0);
                                                        z6 = true;
                                                    } else {
                                                        z6 = z7;
                                                    }
                                                    if ((flags2 & 8) == 0) {
                                                        int length2 = userIds.length;
                                                        iArrAppendInt2 = iArr;
                                                        int i8 = 0;
                                                        while (i8 < length2) {
                                                            int i9 = userIds[i8];
                                                            int i10 = length2;
                                                            if (permissionsState2.grantRuntimePermission(permissionLocked, i9) != -1) {
                                                                permissionsState2.updatePermissionFlags(permissionLocked, i9, flags2, flags2);
                                                                iArrAppendInt2 = ArrayUtils.appendInt(iArrAppendInt2, i9);
                                                            }
                                                            i8++;
                                                            length2 = i10;
                                                        }
                                                    } else {
                                                        iArrAppendInt2 = iArr;
                                                    }
                                                    packageSetting2 = packageSetting4;
                                                    iArr2 = userIds;
                                                    iArr = iArrAppendInt2;
                                                    z7 = z6;
                                                    str2 = str;
                                                    break;
                                                default:
                                                    packageSetting2 = packageSetting4;
                                                    iArr2 = userIds;
                                                    str2 = str;
                                                    if ((str2 == null || str2.equals(r28.packageName)) && PackageManagerService.DEBUG_PERMISSIONS) {
                                                        Slog.i(TAG, "Not granting permission " + name + " to package " + r28.packageName + " because it was previously installed without");
                                                    }
                                                    break;
                                            }
                                        } else {
                                            packageSetting2 = packageSetting4;
                                            iArr2 = userIds;
                                            str2 = str;
                                            if (permissionsState2.revokeInstallPermission(permissionLocked) != -1) {
                                                permissionsState2.updatePermissionFlags(permissionLocked, -1, 255, 0);
                                                Slog.i(TAG, "Un-granting permission " + name + " from package " + r28.packageName + " (protectionLevel=" + permissionLocked.getProtectionLevel() + " flags=0x" + Integer.toHexString(r28.applicationInfo.flags) + ")");
                                            } else if (permissionLocked.isAppOp() && PackageManagerService.DEBUG_PERMISSIONS && (str2 == null || str2.equals(r28.packageName))) {
                                                Slog.i(TAG, "Not granting permission " + name + " to package " + r28.packageName + " (protectionLevel=" + permissionLocked.getProtectionLevel() + " flags=0x" + Integer.toHexString(r28.applicationInfo.flags) + ")");
                                            }
                                        }
                                        z7 = true;
                                    } else {
                                        c = 2;
                                        z5 = false;
                                        if (PackageManagerService.DEBUG_PERMISSIONS) {
                                        }
                                        if (c != 1) {
                                        }
                                        z7 = true;
                                    }
                                } else if (PackageManagerService.DEBUG_PERMISSIONS) {
                                    Log.i(TAG, "Denying runtime-only permission " + permissionLocked.getName() + " for package " + r28.packageName);
                                }
                            } else if (PackageManagerService.DEBUG_PERMISSIONS) {
                                Log.i(TAG, "Denying non-ephemeral permission " + permissionLocked.getName() + " for package " + r28.packageName);
                            }
                            str2 = str3;
                            packageSetting2 = packageSetting4;
                            z4 = zIsLegacySystemApp;
                            iArr2 = userIds;
                        }
                        i3 = i + 1;
                        str3 = str2;
                        size = i4;
                        z2 = z8;
                        zIsLegacySystemApp = z4;
                        userIds = iArr2;
                        packageSetting4 = packageSetting2;
                    }
                    PackageSetting packageSetting5 = packageSetting4;
                    z3 = z2;
                    if (z7 || z) {
                        packageSetting = packageSetting5;
                        if (!packageSetting.areInstallPermissionsFixed() && !packageSetting.isSystem()) {
                            packageSetting.setInstallPermissionsFixed(true);
                        }
                    }
                    packageSetting = packageSetting5;
                    if (packageSetting.isUpdatedSystem()) {
                    }
                }
                if (permissionCallback != null) {
                    permissionCallback.onPermissionUpdated(iArr, z3);
                    return;
                }
                return;
            }
            permissionsState = new PermissionsState(permissionsState2);
            permissionsState2.reset();
        } else {
            permissionsState = permissionsState2;
        }
        z2 = false;
        permissionsState2.setGlobalGids(this.mGlobalGids);
        synchronized (this.mLock) {
        }
    }

    private boolean isNewPlatformPermissionForPackage(String str, PackageParser.Package r7) {
        int length = PackageParser.NEW_PERMISSIONS.length;
        for (int i = 0; i < length; i++) {
            PackageParser.NewPermissionInfo newPermissionInfo = PackageParser.NEW_PERMISSIONS[i];
            if (newPermissionInfo.name.equals(str) && r7.applicationInfo.targetSdkVersion < newPermissionInfo.sdkVersion) {
                Log.i(TAG, "Auto-granting " + str + " to old pkg " + r7.packageName);
                return true;
            }
        }
        return false;
    }

    private boolean hasPrivappWhitelistEntry(String str, PackageParser.Package r5) {
        ArraySet privAppPermissions;
        if (r5.isVendor()) {
            privAppPermissions = SystemConfig.getInstance().getVendorPrivAppPermissions(r5.packageName);
        } else if (r5.isProduct()) {
            privAppPermissions = SystemConfig.getInstance().getProductPrivAppPermissions(r5.packageName);
        } else {
            privAppPermissions = SystemConfig.getInstance().getPrivAppPermissions(r5.packageName);
        }
        return (privAppPermissions != null && privAppPermissions.contains(str)) || (r5.parentPackage != null && hasPrivappWhitelistEntry(str, r5.parentPackage));
    }

    private boolean grantSignaturePermission(String str, PackageParser.Package r12, BasePermission basePermission, PermissionsState permissionsState) {
        boolean z;
        boolean zHasInstallPermission;
        PackageSetting packageSetting;
        PackageSetting packageSetting2;
        ArraySet privAppDenyPermissions;
        boolean zIsOEM = basePermission.isOEM();
        boolean zIsVendorPrivileged = basePermission.isVendorPrivileged();
        boolean z2 = basePermission.isPrivileged() || basePermission.isVendorPrivileged();
        boolean z3 = RoSystemProperties.CONTROL_PRIVAPP_PERMISSIONS_DISABLE;
        boolean zEquals = PackageManagerService.PLATFORM_PACKAGE_NAME.equals(basePermission.getSourcePackageName());
        boolean zEquals2 = PackageManagerService.PLATFORM_PACKAGE_NAME.equals(r12.packageName);
        if (!z3 && z2 && r12.isPrivileged() && !zEquals2 && zEquals && !hasPrivappWhitelistEntry(str, r12)) {
            if (!this.mSystemReady && !r12.isUpdatedSystemApp()) {
                if (r12.isVendor()) {
                    privAppDenyPermissions = SystemConfig.getInstance().getVendorPrivAppDenyPermissions(r12.packageName);
                } else if (r12.isProduct()) {
                    privAppDenyPermissions = SystemConfig.getInstance().getProductPrivAppDenyPermissions(r12.packageName);
                } else {
                    privAppDenyPermissions = SystemConfig.getInstance().getPrivAppDenyPermissions(r12.packageName);
                }
                if (!(privAppDenyPermissions == null || !privAppDenyPermissions.contains(str))) {
                    return false;
                }
                Slog.w(TAG, "Privileged permission " + str + " for package " + r12.packageName + " - not in privapp-permissions whitelist");
                if (RoSystemProperties.CONTROL_PRIVAPP_PERMISSIONS_ENFORCE) {
                    if (this.mPrivappPermissionsViolations == null) {
                        this.mPrivappPermissionsViolations = new ArraySet<>();
                    }
                    this.mPrivappPermissionsViolations.add(r12.packageName + ": " + str);
                }
            }
            if (RoSystemProperties.CONTROL_PRIVAPP_PERMISSIONS_ENFORCE) {
                return false;
            }
        }
        PackageParser.Package r5 = this.mPackageManagerInt.getPackage(this.mPackageManagerInt.getKnownPackageName(0, 0));
        boolean z4 = r12.mSigningDetails.hasAncestorOrSelf(basePermission.getSourcePackageSetting().getSigningDetails()) || basePermission.getSourcePackageSetting().getSigningDetails().checkCapability(r12.mSigningDetails, 4) || r12.mSigningDetails.hasAncestorOrSelf(r5.mSigningDetails) || r5.mSigningDetails.checkCapability(r12.mSigningDetails, 4);
        if (z4 || !((z2 || zIsOEM) && r12.isSystem())) {
            z = z4;
        } else {
            if (r12.isUpdatedSystemApp()) {
                PackageParser.Package disabledPackage = this.mPackageManagerInt.getDisabledPackage(r12.packageName);
                PackageSetting packageSetting3 = disabledPackage != null ? (PackageSetting) disabledPackage.mExtras : null;
                if (packageSetting3 != null && packageSetting3.getPermissionsState().hasInstallPermission(str)) {
                    if ((!z2 || !packageSetting3.isPrivileged()) && (!zIsOEM || !packageSetting3.isOem() || !canGrantOemPermission(packageSetting3, str))) {
                        z4 = true;
                        break;
                    }
                    z = z4;
                } else {
                    if (packageSetting3 != null && disabledPackage != null && isPackageRequestingPermission(disabledPackage, str) && ((z2 && packageSetting3.isPrivileged()) || (zIsOEM && packageSetting3.isOem() && canGrantOemPermission(packageSetting3, str)))) {
                        z4 = true;
                    }
                    if (r12.parentPackage != null) {
                        PackageParser.Package disabledPackage2 = this.mPackageManagerInt.getDisabledPackage(r12.parentPackage.packageName);
                        if (disabledPackage2 != null) {
                            packageSetting = (PackageSetting) disabledPackage2.mExtras;
                        } else {
                            packageSetting = null;
                        }
                        if (disabledPackage2 != null && ((z2 && packageSetting.isPrivileged()) || (zIsOEM && packageSetting.isOem()))) {
                            if (!isPackageRequestingPermission(disabledPackage2, str) || !canGrantOemPermission(packageSetting, str)) {
                                if (disabledPackage2.childPackages != null) {
                                    for (PackageParser.Package r6 : disabledPackage2.childPackages) {
                                        if (r6 != null) {
                                            packageSetting2 = (PackageSetting) r6.mExtras;
                                        } else {
                                            packageSetting2 = null;
                                        }
                                        if (isPackageRequestingPermission(r6, str) && canGrantOemPermission(packageSetting2, str)) {
                                            z4 = true;
                                            break;
                                        }
                                    }
                                }
                            } else {
                                z4 = true;
                                break;
                            }
                        }
                    }
                    z = z4;
                }
            } else {
                z = (z2 && r12.isPrivileged()) || (zIsOEM && r12.isOem() && canGrantOemPermission((PackageSetting) r12.mExtras, str));
            }
            if (z && z2 && !zIsVendorPrivileged && r12.isVendor()) {
                Slog.w(TAG, "Permission " + str + " cannot be granted to privileged vendor apk " + r12.packageName + " because it isn't a 'vendorPrivileged' permission.");
                z = false;
            }
        }
        if (z) {
            return z;
        }
        if (!z && basePermission.isPre23() && r12.applicationInfo.targetSdkVersion < 23) {
            z = true;
        }
        if (!z && basePermission.isInstaller() && r12.packageName.equals(this.mPackageManagerInt.getKnownPackageName(2, 0))) {
            z = true;
        }
        if (!z && basePermission.isVerifier() && r12.packageName.equals(this.mPackageManagerInt.getKnownPackageName(3, 0))) {
            z = true;
        }
        if (!z && basePermission.isPreInstalled() && r12.isSystem()) {
            z = true;
        }
        if (!z && basePermission.isDevelopment()) {
            zHasInstallPermission = permissionsState.hasInstallPermission(str);
        } else {
            zHasInstallPermission = z;
        }
        if (!zHasInstallPermission && basePermission.isSetup() && r12.packageName.equals(this.mPackageManagerInt.getKnownPackageName(1, 0))) {
            zHasInstallPermission = true;
        }
        if (!zHasInstallPermission && basePermission.isSystemTextClassifier() && r12.packageName.equals(this.mPackageManagerInt.getKnownPackageName(5, 0))) {
            return true;
        }
        return zHasInstallPermission;
    }

    private static boolean canGrantOemPermission(PackageSetting packageSetting, String str) {
        if (!packageSetting.isOem()) {
            return false;
        }
        Boolean bool = (Boolean) SystemConfig.getInstance().getOemPermissions(packageSetting.name).get(str);
        if (bool != null) {
            return Boolean.TRUE == bool;
        }
        throw new IllegalStateException("OEM permission" + str + " requested by package " + packageSetting.name + " must be explicitly declared granted or not");
    }

    private boolean isPermissionsReviewRequired(PackageParser.Package r4, int i) {
        if (!this.mSettings.mPermissionReviewRequired) {
            Log.d(TAG, "!mSettings.mPermissionReviewRequired return");
            return false;
        }
        if ((r4.applicationInfo.targetSdkVersion >= 23 && !sCtaManager.isCtaSupported()) || r4 == null || r4.mExtras == null) {
            return false;
        }
        boolean zIsPermissionReviewRequired = ((PackageSetting) r4.mExtras).getPermissionsState().isPermissionReviewRequired(i);
        return sCtaManager.isCtaSupported() ? sCtaManager.isPermissionReviewRequired(r4, i, zIsPermissionReviewRequired) : zIsPermissionReviewRequired;
    }

    private boolean isPackageRequestingPermission(PackageParser.Package r5, String str) {
        int size = r5.requestedPermissions.size();
        for (int i = 0; i < size; i++) {
            if (str.equals((String) r5.requestedPermissions.get(i))) {
                return true;
            }
        }
        return false;
    }

    @GuardedBy("mLock")
    private void grantRuntimePermissionsGrantedToDisabledPackageLocked(PackageParser.Package r18, int i, PermissionManagerInternal.PermissionCallback permissionCallback) {
        PackageParser.Package disabledPackage;
        int i2;
        if (r18.parentPackage == null || r18.requestedPermissions == null || (disabledPackage = this.mPackageManagerInt.getDisabledPackage(r18.parentPackage.packageName)) == null || disabledPackage.mExtras == null) {
            return;
        }
        PackageSetting packageSetting = (PackageSetting) disabledPackage.mExtras;
        if (!packageSetting.isPrivileged() || packageSetting.hasChildPackages()) {
            return;
        }
        int size = r18.requestedPermissions.size();
        for (int i3 = 0; i3 < size; i3++) {
            String str = (String) r18.requestedPermissions.get(i3);
            BasePermission permissionLocked = this.mSettings.getPermissionLocked(str);
            if (permissionLocked != null && (permissionLocked.isRuntime() || permissionLocked.isDevelopment())) {
                int[] userIds = this.mUserManagerInt.getUserIds();
                int length = userIds.length;
                int i4 = 0;
                while (i4 < length) {
                    int i5 = userIds[i4];
                    if (!packageSetting.getPermissionsState().hasRuntimePermission(str, i5)) {
                        i2 = i4;
                    } else {
                        i2 = i4;
                        grantRuntimePermission(str, r18.packageName, false, i, i5, permissionCallback);
                    }
                    i4 = i2 + 1;
                }
            }
        }
    }

    private void grantRequestedRuntimePermissions(PackageParser.Package r9, int[] iArr, String[] strArr, int i, PermissionManagerInternal.PermissionCallback permissionCallback) {
        for (int i2 : iArr) {
            grantRequestedRuntimePermissionsForUser(r9, i2, strArr, i, permissionCallback);
        }
    }

    private void grantRequestedRuntimePermissionsForUser(PackageParser.Package r17, int i, String[] strArr, int i2, PermissionManagerInternal.PermissionCallback permissionCallback) {
        BasePermission permissionLocked;
        PackageSetting packageSetting = (PackageSetting) r17.mExtras;
        if (packageSetting == null) {
            return;
        }
        PermissionsState permissionsState = packageSetting.getPermissionsState();
        boolean z = r17.applicationInfo.targetSdkVersion >= 23;
        boolean zIsInstantApp = this.mPackageManagerInt.isInstantApp(r17.packageName, i);
        for (String str : r17.requestedPermissions) {
            synchronized (this.mLock) {
                permissionLocked = this.mSettings.getPermissionLocked(str);
            }
            if (permissionLocked != null && (permissionLocked.isRuntime() || permissionLocked.isDevelopment())) {
                if (!zIsInstantApp || permissionLocked.isInstant()) {
                    if (z || !permissionLocked.isRuntimeOnly()) {
                        if (strArr == null || ArrayUtils.contains(strArr, str)) {
                            int permissionFlags = permissionsState.getPermissionFlags(str, i);
                            if (z) {
                                if ((permissionFlags & 20) == 0) {
                                    grantRuntimePermission(str, r17.packageName, false, i2, i, permissionCallback);
                                }
                            } else if (this.mSettings.mPermissionReviewRequired && (permissionFlags & 64) != 0) {
                                updatePermissionFlags(str, r17.packageName, 64, 0, i2, i, permissionCallback);
                            }
                        }
                    }
                }
            }
        }
    }

    private void grantRuntimePermission(String str, String str2, boolean z, int i, int i2, PermissionManagerInternal.PermissionCallback permissionCallback) {
        BasePermission permissionLocked;
        if (!this.mUserManagerInt.exists(i2)) {
            Log.e(TAG, "No such user:" + i2);
            return;
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.GRANT_RUNTIME_PERMISSIONS", "grantRuntimePermission");
        enforceCrossUserPermission(i, i2, true, true, false, "grantRuntimePermission");
        PackageParser.Package r1 = this.mPackageManagerInt.getPackage(str2);
        if (r1 == null || r1.mExtras == null) {
            throw new IllegalArgumentException("Unknown package: " + str2);
        }
        synchronized (this.mLock) {
            permissionLocked = this.mSettings.getPermissionLocked(str);
        }
        if (permissionLocked == null) {
            throw new IllegalArgumentException("Unknown permission: " + str);
        }
        if (this.mPackageManagerInt.filterAppAccess(r1, i, i2)) {
            throw new IllegalArgumentException("Unknown package: " + str2);
        }
        permissionLocked.enforceDeclaredUsedAndRuntimeOrDevelopment(r1);
        if (this.mSettings.mPermissionReviewRequired && r1.applicationInfo.targetSdkVersion < 23 && permissionLocked.isRuntime()) {
            return;
        }
        int uid = UserHandle.getUid(i2, r1.applicationInfo.uid);
        PackageSetting packageSetting = (PackageSetting) r1.mExtras;
        PermissionsState permissionsState = packageSetting.getPermissionsState();
        int permissionFlags = permissionsState.getPermissionFlags(str, i2);
        if ((permissionFlags & 16) != 0) {
            throw new SecurityException("Cannot grant system fixed permission " + str + " for package " + str2);
        }
        if (!z && (permissionFlags & 4) != 0) {
            throw new SecurityException("Cannot grant policy fixed permission " + str + " for package " + str2);
        }
        if (permissionLocked.isDevelopment()) {
            if (permissionsState.grantInstallPermission(permissionLocked) != -1 && permissionCallback != null) {
                permissionCallback.onInstallPermissionGranted();
                return;
            }
            return;
        }
        if (packageSetting.getInstantApp(i2) && !permissionLocked.isInstant()) {
            throw new SecurityException("Cannot grant non-ephemeral permission" + str + " for package " + str2);
        }
        if (r1.applicationInfo.targetSdkVersion < 23) {
            Slog.w(TAG, "Cannot grant runtime permission to a legacy app");
            return;
        }
        int iGrantRuntimePermission = permissionsState.grantRuntimePermission(permissionLocked, i2);
        if (iGrantRuntimePermission == -1) {
            return;
        }
        if (iGrantRuntimePermission == 1 && permissionCallback != null) {
            permissionCallback.onGidsChanged(UserHandle.getAppId(r1.applicationInfo.uid), i2);
        }
        if (permissionLocked.isRuntime()) {
            logPermission(1243, str, str2);
        }
        if (permissionCallback != null) {
            permissionCallback.onPermissionGranted(uid, i2);
        }
        if ("android.permission.READ_EXTERNAL_STORAGE".equals(str) || "android.permission.WRITE_EXTERNAL_STORAGE".equals(str)) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                if (this.mUserManagerInt.isUserInitialized(i2)) {
                    ((StorageManagerInternal) LocalServices.getService(StorageManagerInternal.class)).onExternalStoragePolicyChanged(uid, str2);
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    public boolean isPackageNeedsReview(PackageParser.Package r6, SharedUserSetting sharedUserSetting) {
        if (!sCtaManager.isCtaSupported()) {
            return false;
        }
        boolean z = r6.applicationInfo.targetSdkVersion >= 23;
        if (r6.mSharedUserId == null) {
            return (z && isSystemApp(r6)) ? false : true;
        }
        if (sharedUserSetting != null) {
            for (PackageParser.Package r7 : sharedUserSetting.getPackages()) {
                if (z) {
                    if (isSystemApp(r7)) {
                        return false;
                    }
                } else if (r7.applicationInfo.targetSdkVersion >= 23) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isSystemApp(PackageParser.Package r1) {
        return (r1.applicationInfo.flags & 1) != 0;
    }

    private void revokeRuntimePermission(String str, String str2, boolean z, int i, int i2, PermissionManagerInternal.PermissionCallback permissionCallback) {
        if (!this.mUserManagerInt.exists(i2)) {
            Log.e(TAG, "No such user:" + i2);
            return;
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.REVOKE_RUNTIME_PERMISSIONS", "revokeRuntimePermission");
        enforceCrossUserPermission(Binder.getCallingUid(), i2, true, true, false, "revokeRuntimePermission");
        PackageParser.Package r13 = this.mPackageManagerInt.getPackage(str2);
        if (r13 == null || r13.mExtras == null) {
            throw new IllegalArgumentException("Unknown package: " + str2);
        }
        if (this.mPackageManagerInt.filterAppAccess(r13, Binder.getCallingUid(), i2)) {
            throw new IllegalArgumentException("Unknown package: " + str2);
        }
        BasePermission permissionLocked = this.mSettings.getPermissionLocked(str);
        if (permissionLocked == null) {
            throw new IllegalArgumentException("Unknown permission: " + str);
        }
        permissionLocked.enforceDeclaredUsedAndRuntimeOrDevelopment(r13);
        if (this.mSettings.mPermissionReviewRequired && r13.applicationInfo.targetSdkVersion < 23 && permissionLocked.isRuntime()) {
            return;
        }
        PermissionsState permissionsState = ((PackageSetting) r13.mExtras).getPermissionsState();
        int permissionFlags = permissionsState.getPermissionFlags(str, i2);
        if ((permissionFlags & 16) != 0 && UserHandle.getCallingAppId() != 1000) {
            throw new SecurityException("Non-System UID cannot revoke system fixed permission " + str + " for package " + str2);
        }
        if (!z && (permissionFlags & 4) != 0) {
            throw new SecurityException("Cannot revoke policy fixed permission " + str + " for package " + str2);
        }
        if (permissionLocked.isDevelopment()) {
            if (permissionsState.revokeInstallPermission(permissionLocked) != -1 && permissionCallback != null) {
                permissionCallback.onInstallPermissionRevoked();
                return;
            }
            return;
        }
        if (permissionsState.revokeRuntimePermission(permissionLocked, i2) == -1) {
            return;
        }
        if (permissionLocked.isRuntime()) {
            logPermission(1245, str, str2);
        }
        if (permissionCallback != null) {
            UserHandle.getUid(i2, r13.applicationInfo.uid);
            permissionCallback.onPermissionRevoked(r13.applicationInfo.uid, i2);
        }
    }

    @GuardedBy("mLock")
    private int[] revokeUnusedSharedUserPermissionsLocked(SharedUserSetting sharedUserSetting, int[] iArr) {
        BasePermission permissionLocked;
        BasePermission permissionLocked2;
        ArraySet arraySet = new ArraySet();
        List<PackageParser.Package> packages = sharedUserSetting.getPackages();
        if (packages == null || packages.size() == 0) {
            return EmptyArray.INT;
        }
        Iterator<PackageParser.Package> it = packages.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            PackageParser.Package next = it.next();
            if (next.requestedPermissions != null) {
                int size = next.requestedPermissions.size();
                for (int i = 0; i < size; i++) {
                    String str = (String) next.requestedPermissions.get(i);
                    if (this.mSettings.getPermissionLocked(str) != null) {
                        arraySet.add(str);
                    }
                }
            }
        }
        PermissionsState permissionsState = sharedUserSetting.getPermissionsState();
        List<PermissionsState.PermissionState> installPermissionStates = permissionsState.getInstallPermissionStates();
        int size2 = installPermissionStates.size();
        while (true) {
            size2--;
            if (size2 < 0) {
                break;
            }
            PermissionsState.PermissionState permissionState = installPermissionStates.get(size2);
            if (!arraySet.contains(permissionState.getName()) && (permissionLocked2 = this.mSettings.getPermissionLocked(permissionState.getName())) != null) {
                permissionsState.revokeInstallPermission(permissionLocked2);
                permissionsState.updatePermissionFlags(permissionLocked2, -1, 255, 0);
            }
        }
        int[] iArrAppendInt = EmptyArray.INT;
        for (int i2 : iArr) {
            List<PermissionsState.PermissionState> runtimePermissionStates = permissionsState.getRuntimePermissionStates(i2);
            for (int size3 = runtimePermissionStates.size() - 1; size3 >= 0; size3--) {
                PermissionsState.PermissionState permissionState2 = runtimePermissionStates.get(size3);
                if (!arraySet.contains(permissionState2.getName()) && (permissionLocked = this.mSettings.getPermissionLocked(permissionState2.getName())) != null) {
                    permissionsState.revokeRuntimePermission(permissionLocked, i2);
                    permissionsState.updatePermissionFlags(permissionLocked, i2, 255, 0);
                    iArrAppendInt = ArrayUtils.appendInt(iArrAppendInt, i2);
                }
            }
        }
        return iArrAppendInt;
    }

    private String[] getAppOpPermissionPackages(String str) {
        if (this.mPackageManagerInt.getInstantAppPackageName(Binder.getCallingUid()) != null) {
            return null;
        }
        synchronized (this.mLock) {
            ArraySet<String> arraySet = this.mSettings.mAppOpPermissionPackages.get(str);
            if (arraySet == null) {
                return null;
            }
            return (String[]) arraySet.toArray(new String[arraySet.size()]);
        }
    }

    private int getPermissionFlags(String str, String str2, int i, int i2) {
        if (!this.mUserManagerInt.exists(i2)) {
            return 0;
        }
        enforceGrantRevokeRuntimePermissionPermissions("getPermissionFlags");
        enforceCrossUserPermission(i, i2, true, false, false, "getPermissionFlags");
        PackageParser.Package r11 = this.mPackageManagerInt.getPackage(str2);
        if (r11 == null || r11.mExtras == null) {
            return 0;
        }
        synchronized (this.mLock) {
            if (this.mSettings.getPermissionLocked(str) == null) {
                return 0;
            }
            if (this.mPackageManagerInt.filterAppAccess(r11, i, i2)) {
                return 0;
            }
            return ((PackageSetting) r11.mExtras).getPermissionsState().getPermissionFlags(str, i2);
        }
    }

    private void updatePermissions(String str, PackageParser.Package r11, boolean z, Collection<PackageParser.Package> collection, PermissionManagerInternal.PermissionCallback permissionCallback) {
        int i = (r11 != null ? 1 : 0) | (z ? 2 : 0);
        updatePermissions(str, r11, getVolumeUuidForPackage(r11), i, collection, permissionCallback);
        if (r11 != null && r11.childPackages != null) {
            for (PackageParser.Package r4 : r11.childPackages) {
                updatePermissions(r4.packageName, r4, getVolumeUuidForPackage(r4), i, collection, permissionCallback);
            }
        }
    }

    private void updateAllPermissions(String str, boolean z, Collection<PackageParser.Package> collection, PermissionManagerInternal.PermissionCallback permissionCallback) {
        int i;
        if (z) {
            i = 6;
        } else {
            i = 0;
        }
        updatePermissions(null, null, str, 1 | i, collection, permissionCallback);
    }

    private void updatePermissions(String str, PackageParser.Package r9, String str2, int i, Collection<PackageParser.Package> collection, PermissionManagerInternal.PermissionCallback permissionCallback) {
        int iUpdatePermissions = updatePermissions(str, r9, updatePermissionTrees(str, r9, i));
        Trace.traceBegin(262144L, "grantPermissions");
        boolean z = false;
        if ((iUpdatePermissions & 1) != 0) {
            for (PackageParser.Package r0 : collection) {
                if (r0 != r9) {
                    grantPermissions(r0, (iUpdatePermissions & 4) != 0 && Objects.equals(str2, getVolumeUuidForPackage(r0)), str, permissionCallback);
                }
            }
        }
        if (r9 != null) {
            String volumeUuidForPackage = getVolumeUuidForPackage(r9);
            if ((iUpdatePermissions & 2) != 0 && Objects.equals(str2, volumeUuidForPackage)) {
                z = true;
            }
            grantPermissions(r9, z, str, permissionCallback);
        }
        Trace.traceEnd(262144L);
    }

    private int updatePermissions(String str, PackageParser.Package r9, int i) {
        ArraySet<BasePermission> arraySet;
        synchronized (this.mLock) {
            Iterator<BasePermission> it = this.mSettings.mPermissions.values().iterator();
            arraySet = null;
            while (it.hasNext()) {
                BasePermission next = it.next();
                if (next.isDynamic()) {
                    next.updateDynamicPermission(this.mSettings.mPermissionTrees.values());
                }
                if (next.getSourcePackageSetting() != null) {
                    if (str != null && str.equals(next.getSourcePackageName()) && (r9 == null || !hasPermission(r9, next.getName()))) {
                        Slog.i(TAG, "Removing old permission tree: " + next.getName() + " from package " + next.getSourcePackageName());
                        i |= 1;
                        it.remove();
                    }
                } else {
                    if (arraySet == null) {
                        arraySet = new ArraySet(this.mSettings.mPermissions.size());
                    }
                    arraySet.add(next);
                }
            }
        }
        if (arraySet != null) {
            for (BasePermission basePermission : arraySet) {
                PackageParser.Package r0 = this.mPackageManagerInt.getPackage(basePermission.getSourcePackageName());
                synchronized (this.mLock) {
                    if (r0 != null) {
                        try {
                            if (r0.mExtras != null) {
                                PackageSetting packageSetting = (PackageSetting) r0.mExtras;
                                if (basePermission.getSourcePackageSetting() == null) {
                                    basePermission.setSourcePackageSetting(packageSetting);
                                }
                            }
                        } finally {
                        }
                    }
                    Slog.w(TAG, "Removing dangling permission: " + basePermission.getName() + " from package " + basePermission.getSourcePackageName());
                    this.mSettings.removePermissionLocked(basePermission.getName());
                }
            }
        }
        return i;
    }

    private int updatePermissionTrees(String str, PackageParser.Package r9, int i) {
        ArraySet<BasePermission> arraySet;
        synchronized (this.mLock) {
            Iterator<BasePermission> it = this.mSettings.mPermissionTrees.values().iterator();
            arraySet = null;
            while (it.hasNext()) {
                BasePermission next = it.next();
                if (next.getSourcePackageSetting() != null) {
                    if (str != null && str.equals(next.getSourcePackageName()) && (r9 == null || !hasPermission(r9, next.getName()))) {
                        Slog.i(TAG, "Removing old permission tree: " + next.getName() + " from package " + next.getSourcePackageName());
                        i |= 1;
                        it.remove();
                    }
                } else {
                    if (arraySet == null) {
                        arraySet = new ArraySet(this.mSettings.mPermissionTrees.size());
                    }
                    arraySet.add(next);
                }
            }
        }
        if (arraySet != null) {
            for (BasePermission basePermission : arraySet) {
                PackageParser.Package r0 = this.mPackageManagerInt.getPackage(basePermission.getSourcePackageName());
                synchronized (this.mLock) {
                    if (r0 != null) {
                        try {
                            if (r0.mExtras != null) {
                                PackageSetting packageSetting = (PackageSetting) r0.mExtras;
                                if (basePermission.getSourcePackageSetting() == null) {
                                    basePermission.setSourcePackageSetting(packageSetting);
                                }
                            }
                        } finally {
                        }
                    }
                    Slog.w(TAG, "Removing dangling permission tree: " + basePermission.getName() + " from package " + basePermission.getSourcePackageName());
                    this.mSettings.removePermissionLocked(basePermission.getName());
                }
            }
        }
        return i;
    }

    private void updatePermissionFlags(String str, String str2, int i, int i2, int i3, int i4, PermissionManagerInternal.PermissionCallback permissionCallback) {
        BasePermission permissionLocked;
        if (!this.mUserManagerInt.exists(i4)) {
            return;
        }
        enforceGrantRevokeRuntimePermissionPermissions("updatePermissionFlags");
        enforceCrossUserPermission(i3, i4, true, true, false, "updatePermissionFlags");
        if (i3 != 1000) {
            i = i & (-17) & (-33);
            i2 = i2 & (-17) & (-33) & (-65);
        }
        PackageParser.Package r0 = this.mPackageManagerInt.getPackage(str2);
        if (r0 == null || r0.mExtras == null) {
            throw new IllegalArgumentException("Unknown package: " + str2);
        }
        if (this.mPackageManagerInt.filterAppAccess(r0, i3, i4)) {
            throw new IllegalArgumentException("Unknown package: " + str2);
        }
        synchronized (this.mLock) {
            permissionLocked = this.mSettings.getPermissionLocked(str);
        }
        if (permissionLocked == null) {
            throw new IllegalArgumentException("Unknown permission: " + str);
        }
        PermissionsState permissionsState = ((PackageSetting) r0.mExtras).getPermissionsState();
        boolean z = permissionsState.getRuntimePermissionState(str, i4) != null;
        if (permissionsState.updatePermissionFlags(permissionLocked, i4, i, i2) && permissionCallback != null) {
            if (sCtaManager.isCtaSupported() && (i & 64) != 0 && (i2 & 64) == 0 && r0.mSharedUserId != null && !permissionsState.isPermissionReviewRequired(i4)) {
                permissionsState.updateReviewRequiredCache(i4);
            }
            if (permissionsState.getInstallPermissionState(str) != null) {
                permissionCallback.onInstallPermissionUpdated();
            } else if (permissionsState.getRuntimePermissionState(str, i4) != null || z) {
                permissionCallback.onPermissionUpdated(new int[]{i4}, false);
            }
        }
    }

    private boolean updatePermissionFlagsForAllApps(int i, int i2, int i3, int i4, Collection<PackageParser.Package> collection, PermissionManagerInternal.PermissionCallback permissionCallback) {
        boolean zUpdatePermissionFlagsForAllPermissions = false;
        if (!this.mUserManagerInt.exists(i4)) {
            return false;
        }
        enforceGrantRevokeRuntimePermissionPermissions("updatePermissionFlagsForAllApps");
        enforceCrossUserPermission(i3, i4, true, true, false, "updatePermissionFlagsForAllApps");
        if (i3 != 1000) {
            i &= -17;
            i2 &= -17;
        }
        Iterator<PackageParser.Package> it = collection.iterator();
        while (it.hasNext()) {
            PackageSetting packageSetting = (PackageSetting) it.next().mExtras;
            if (packageSetting != null) {
                zUpdatePermissionFlagsForAllPermissions |= packageSetting.getPermissionsState().updatePermissionFlagsForAllPermissions(i4, i, i2);
            }
        }
        return zUpdatePermissionFlagsForAllPermissions;
    }

    private void enforceGrantRevokeRuntimePermissionPermissions(String str) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.GRANT_RUNTIME_PERMISSIONS") != 0 && this.mContext.checkCallingOrSelfPermission("android.permission.REVOKE_RUNTIME_PERMISSIONS") != 0) {
            throw new SecurityException(str + " requires android.permission.GRANT_RUNTIME_PERMISSIONS or android.permission.REVOKE_RUNTIME_PERMISSIONS");
        }
    }

    private void enforceCrossUserPermission(int i, int i2, boolean z, boolean z2, boolean z3, String str) {
        if (i2 < 0) {
            throw new IllegalArgumentException("Invalid userId " + i2);
        }
        if (z2) {
            PackageManagerServiceUtils.enforceShellRestriction("no_debugging_features", i, i2);
        }
        if ((z3 || i2 != UserHandle.getUserId(i)) && i != 1000 && i != 0) {
            if (z) {
                this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", str);
                return;
            }
            try {
                this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", str);
            } catch (SecurityException e) {
                this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS", str);
            }
        }
    }

    private int calculateCurrentPermissionFootprintLocked(BasePermission basePermission) {
        Iterator<BasePermission> it = this.mSettings.mPermissions.values().iterator();
        int iCalculateFootprint = 0;
        while (it.hasNext()) {
            iCalculateFootprint += basePermission.calculateFootprint(it.next());
        }
        return iCalculateFootprint;
    }

    private void enforcePermissionCapLocked(PermissionInfo permissionInfo, BasePermission basePermission) {
        if (basePermission.getUid() != 1000 && calculateCurrentPermissionFootprintLocked(basePermission) + permissionInfo.calculateFootprint() > 32768) {
            throw new SecurityException("Permission tree size cap exceeded");
        }
    }

    private void systemReady() {
        this.mSystemReady = true;
        if (this.mPrivappPermissionsViolations != null) {
            throw new IllegalStateException("Signature|privileged permissions not in privapp-permissions whitelist: " + this.mPrivappPermissionsViolations);
        }
    }

    private static String getVolumeUuidForPackage(PackageParser.Package r1) {
        if (r1 == null) {
            return StorageManager.UUID_PRIVATE_INTERNAL;
        }
        if (r1.isExternal()) {
            if (TextUtils.isEmpty(r1.volumeUuid)) {
                return "primary_physical";
            }
            return r1.volumeUuid;
        }
        return StorageManager.UUID_PRIVATE_INTERNAL;
    }

    private static boolean hasPermission(PackageParser.Package r3, String str) {
        for (int size = r3.permissions.size() - 1; size >= 0; size--) {
            if (((PackageParser.Permission) r3.permissions.get(size)).info.name.equals(str)) {
                return true;
            }
        }
        return false;
    }

    private void logPermission(int i, String str, String str2) {
        LogMaker logMaker = new LogMaker(i);
        logMaker.setPackageName(str2);
        logMaker.addTaggedData(1241, str);
        this.mMetricsLogger.write(logMaker);
    }

    private class PermissionManagerInternalImpl extends PermissionManagerInternal {
        private PermissionManagerInternalImpl() {
        }

        @Override
        public void systemReady() {
            PermissionManagerService.this.systemReady();
        }

        @Override
        public boolean isPermissionsReviewRequired(PackageParser.Package r2, int i) {
            return PermissionManagerService.this.isPermissionsReviewRequired(r2, i);
        }

        @Override
        public void revokeRuntimePermissionsIfGroupChanged(PackageParser.Package r2, PackageParser.Package r3, ArrayList<String> arrayList, PermissionManagerInternal.PermissionCallback permissionCallback) {
            PermissionManagerService.this.revokeRuntimePermissionsIfGroupChanged(r2, r3, arrayList, permissionCallback);
        }

        @Override
        public void revokeRuntimePermissionsIfPermissionDefinitionChanged(List<String> list, ArrayList<String> arrayList, PermissionManagerInternal.PermissionCallback permissionCallback) {
            PermissionManagerService.this.revokeRuntimePermissionsIfPermissionDefinitionChanged(list, arrayList, permissionCallback);
        }

        @Override
        public List<String> addAllPermissions(PackageParser.Package r2, boolean z) {
            return PermissionManagerService.this.addAllPermissions(r2, z);
        }

        @Override
        public void addAllPermissionGroups(PackageParser.Package r2, boolean z) {
            PermissionManagerService.this.addAllPermissionGroups(r2, z);
        }

        @Override
        public void removeAllPermissions(PackageParser.Package r2, boolean z) {
            PermissionManagerService.this.removeAllPermissions(r2, z);
        }

        @Override
        public boolean addDynamicPermission(PermissionInfo permissionInfo, boolean z, int i, PermissionManagerInternal.PermissionCallback permissionCallback) {
            return PermissionManagerService.this.addDynamicPermission(permissionInfo, i, permissionCallback);
        }

        @Override
        public void removeDynamicPermission(String str, int i, PermissionManagerInternal.PermissionCallback permissionCallback) {
            PermissionManagerService.this.removeDynamicPermission(str, i, permissionCallback);
        }

        @Override
        public void grantRuntimePermission(String str, String str2, boolean z, int i, int i2, PermissionManagerInternal.PermissionCallback permissionCallback) {
            PermissionManagerService.this.grantRuntimePermission(str, str2, z, i, i2, permissionCallback);
        }

        @Override
        public void grantRequestedRuntimePermissions(PackageParser.Package r7, int[] iArr, String[] strArr, int i, PermissionManagerInternal.PermissionCallback permissionCallback) {
            PermissionManagerService.this.grantRequestedRuntimePermissions(r7, iArr, strArr, i, permissionCallback);
        }

        @Override
        public void grantRuntimePermissionsGrantedToDisabledPackage(PackageParser.Package r2, int i, PermissionManagerInternal.PermissionCallback permissionCallback) {
            PermissionManagerService.this.grantRuntimePermissionsGrantedToDisabledPackageLocked(r2, i, permissionCallback);
        }

        @Override
        public void revokeRuntimePermission(String str, String str2, boolean z, int i, int i2, PermissionManagerInternal.PermissionCallback permissionCallback) {
            PermissionManagerService.this.revokeRuntimePermission(str, str2, z, i, i2, permissionCallback);
        }

        @Override
        public void updatePermissions(String str, PackageParser.Package r8, boolean z, Collection<PackageParser.Package> collection, PermissionManagerInternal.PermissionCallback permissionCallback) {
            PermissionManagerService.this.updatePermissions(str, r8, z, collection, permissionCallback);
        }

        @Override
        public void updateAllPermissions(String str, boolean z, Collection<PackageParser.Package> collection, PermissionManagerInternal.PermissionCallback permissionCallback) {
            PermissionManagerService.this.updateAllPermissions(str, z, collection, permissionCallback);
        }

        @Override
        public String[] getAppOpPermissionPackages(String str) {
            return PermissionManagerService.this.getAppOpPermissionPackages(str);
        }

        @Override
        public int getPermissionFlags(String str, String str2, int i, int i2) {
            return PermissionManagerService.this.getPermissionFlags(str, str2, i, i2);
        }

        @Override
        public void updatePermissionFlags(String str, String str2, int i, int i2, int i3, int i4, PermissionManagerInternal.PermissionCallback permissionCallback) {
            PermissionManagerService.this.updatePermissionFlags(str, str2, i, i2, i3, i4, permissionCallback);
        }

        @Override
        public boolean updatePermissionFlagsForAllApps(int i, int i2, int i3, int i4, Collection<PackageParser.Package> collection, PermissionManagerInternal.PermissionCallback permissionCallback) {
            return PermissionManagerService.this.updatePermissionFlagsForAllApps(i, i2, i3, i4, collection, permissionCallback);
        }

        @Override
        public void enforceCrossUserPermission(int i, int i2, boolean z, boolean z2, String str) {
            PermissionManagerService.this.enforceCrossUserPermission(i, i2, z, z2, false, str);
        }

        @Override
        public void enforceCrossUserPermission(int i, int i2, boolean z, boolean z2, boolean z3, String str) {
            PermissionManagerService.this.enforceCrossUserPermission(i, i2, z, z2, z3, str);
        }

        @Override
        public void enforceGrantRevokeRuntimePermissionPermissions(String str) {
            PermissionManagerService.this.enforceGrantRevokeRuntimePermissionPermissions(str);
        }

        @Override
        public int checkPermission(String str, String str2, int i, int i2) {
            return PermissionManagerService.this.checkPermission(str, str2, i, i2);
        }

        @Override
        public int checkUidPermission(String str, PackageParser.Package r3, int i, int i2) {
            return PermissionManagerService.this.checkUidPermission(str, r3, i, i2);
        }

        @Override
        public PermissionGroupInfo getPermissionGroupInfo(String str, int i, int i2) {
            return PermissionManagerService.this.getPermissionGroupInfo(str, i, i2);
        }

        @Override
        public List<PermissionGroupInfo> getAllPermissionGroups(int i, int i2) {
            return PermissionManagerService.this.getAllPermissionGroups(i, i2);
        }

        @Override
        public PermissionInfo getPermissionInfo(String str, String str2, int i, int i2) {
            return PermissionManagerService.this.getPermissionInfo(str, str2, i, i2);
        }

        @Override
        public List<PermissionInfo> getPermissionInfoByGroup(String str, int i, int i2) {
            return PermissionManagerService.this.getPermissionInfoByGroup(str, i, i2);
        }

        @Override
        public PermissionSettings getPermissionSettings() {
            return PermissionManagerService.this.mSettings;
        }

        @Override
        public DefaultPermissionGrantPolicy getDefaultPermissionGrantPolicy() {
            return PermissionManagerService.this.mDefaultPermissionGrantPolicy;
        }

        @Override
        public BasePermission getPermissionTEMP(String str) {
            BasePermission permissionLocked;
            synchronized (PermissionManagerService.this.mLock) {
                permissionLocked = PermissionManagerService.this.mSettings.getPermissionLocked(str);
            }
            return permissionLocked;
        }
    }
}
