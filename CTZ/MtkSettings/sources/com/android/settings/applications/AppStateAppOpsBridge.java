package com.android.settings.applications;

import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;
import com.android.settings.applications.AppStateBaseBridge;
import com.android.settingslib.applications.ApplicationsState;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public abstract class AppStateAppOpsBridge extends AppStateBaseBridge {
    private final AppOpsManager mAppOpsManager;
    private final int[] mAppOpsOpCodes;
    private final Context mContext;
    private final IPackageManager mIPackageManager;
    private final String[] mPermissions;
    private final List<UserHandle> mProfiles;
    private final UserManager mUserManager;

    @Override
    protected abstract void updateExtraInfo(ApplicationsState.AppEntry appEntry, String str, int i);

    public AppStateAppOpsBridge(Context context, ApplicationsState applicationsState, AppStateBaseBridge.Callback callback, int i, String[] strArr) {
        this(context, applicationsState, callback, i, strArr, AppGlobals.getPackageManager());
    }

    AppStateAppOpsBridge(Context context, ApplicationsState applicationsState, AppStateBaseBridge.Callback callback, int i, String[] strArr, IPackageManager iPackageManager) {
        super(applicationsState, callback);
        this.mContext = context;
        this.mIPackageManager = iPackageManager;
        this.mUserManager = UserManager.get(context);
        this.mProfiles = this.mUserManager.getUserProfiles();
        this.mAppOpsManager = (AppOpsManager) context.getSystemService("appops");
        this.mAppOpsOpCodes = new int[]{i};
        this.mPermissions = strArr;
    }

    private boolean isThisUserAProfileOfCurrentUser(int i) {
        int size = this.mProfiles.size();
        for (int i2 = 0; i2 < size; i2++) {
            if (this.mProfiles.get(i2).getIdentifier() == i) {
                return true;
            }
        }
        return false;
    }

    private boolean doesAnyPermissionMatch(String str, String[] strArr) {
        for (String str2 : strArr) {
            if (str.equals(str2)) {
                return true;
            }
        }
        return false;
    }

    public PermissionState getPermissionInfo(String str, int i) {
        PermissionState permissionState = new PermissionState(str, new UserHandle(UserHandle.getUserId(i)));
        try {
            permissionState.packageInfo = this.mIPackageManager.getPackageInfo(str, 4198400, permissionState.userHandle.getIdentifier());
            if (permissionState.packageInfo != null) {
                String[] strArr = permissionState.packageInfo.requestedPermissions;
                int[] iArr = permissionState.packageInfo.requestedPermissionsFlags;
                if (strArr != null) {
                    int i2 = 0;
                    while (true) {
                        if (i2 >= strArr.length) {
                            break;
                        }
                        if (doesAnyPermissionMatch(strArr[i2], this.mPermissions)) {
                            permissionState.permissionDeclared = true;
                            if ((iArr[i2] & 2) != 0) {
                                permissionState.staticPermissionGranted = true;
                                break;
                            }
                        }
                        i2++;
                    }
                }
            }
            List opsForPackage = this.mAppOpsManager.getOpsForPackage(i, str, this.mAppOpsOpCodes);
            if (opsForPackage != null && opsForPackage.size() > 0 && ((AppOpsManager.PackageOps) opsForPackage.get(0)).getOps().size() > 0) {
                permissionState.appOpMode = ((AppOpsManager.OpEntry) ((AppOpsManager.PackageOps) opsForPackage.get(0)).getOps().get(0)).getMode();
            }
        } catch (RemoteException e) {
            Log.w("AppStateAppOpsBridge", "PackageManager is dead. Can't get package info " + str, e);
        }
        return permissionState;
    }

    @Override
    protected void loadAllExtraInfo() {
        SparseArray<ArrayMap<String, PermissionState>> entries = getEntries();
        loadPermissionsStates(entries);
        loadAppOpsStates(entries);
        ArrayList<ApplicationsState.AppEntry> allApps = this.mAppSession.getAllApps();
        int size = allApps.size();
        for (int i = 0; i < size; i++) {
            ApplicationsState.AppEntry appEntry = allApps.get(i);
            ArrayMap<String, PermissionState> arrayMap = entries.get(UserHandle.getUserId(appEntry.info.uid));
            appEntry.extraInfo = arrayMap != null ? arrayMap.get(appEntry.info.packageName) : null;
        }
    }

    private SparseArray<ArrayMap<String, PermissionState>> getEntries() {
        try {
            HashSet<String> hashSet = new HashSet();
            for (String str : this.mPermissions) {
                String[] appOpPermissionPackages = this.mIPackageManager.getAppOpPermissionPackages(str);
                if (appOpPermissionPackages != null) {
                    hashSet.addAll(Arrays.asList(appOpPermissionPackages));
                }
            }
            if (hashSet.isEmpty()) {
                return null;
            }
            SparseArray<ArrayMap<String, PermissionState>> sparseArray = new SparseArray<>();
            for (UserHandle userHandle : this.mProfiles) {
                ArrayMap<String, PermissionState> arrayMap = new ArrayMap<>();
                int identifier = userHandle.getIdentifier();
                sparseArray.put(identifier, arrayMap);
                for (String str2 : hashSet) {
                    boolean zIsPackageAvailable = this.mIPackageManager.isPackageAvailable(str2, identifier);
                    if (!shouldIgnorePackage(str2) && zIsPackageAvailable) {
                        arrayMap.put(str2, new PermissionState(str2, userHandle));
                    }
                }
            }
            return sparseArray;
        } catch (RemoteException e) {
            Log.w("AppStateAppOpsBridge", "PackageManager is dead. Can't get list of packages requesting " + this.mPermissions[0], e);
            return null;
        }
    }

    private void loadPermissionsStates(SparseArray<ArrayMap<String, PermissionState>> sparseArray) {
        if (sparseArray == null) {
            return;
        }
        try {
            Iterator<UserHandle> it = this.mProfiles.iterator();
            while (it.hasNext()) {
                int identifier = it.next().getIdentifier();
                ArrayMap<String, PermissionState> arrayMap = sparseArray.get(identifier);
                if (arrayMap != null) {
                    List list = this.mIPackageManager.getPackagesHoldingPermissions(this.mPermissions, 0, identifier).getList();
                    int size = list != null ? list.size() : 0;
                    for (int i = 0; i < size; i++) {
                        PackageInfo packageInfo = (PackageInfo) list.get(i);
                        PermissionState permissionState = arrayMap.get(packageInfo.packageName);
                        if (permissionState != null) {
                            permissionState.packageInfo = packageInfo;
                            permissionState.staticPermissionGranted = true;
                        }
                    }
                }
            }
        } catch (RemoteException e) {
            Log.w("AppStateAppOpsBridge", "PackageManager is dead. Can't get list of packages granted " + this.mPermissions, e);
        }
    }

    private void loadAppOpsStates(SparseArray<ArrayMap<String, PermissionState>> sparseArray) {
        ArrayMap<String, PermissionState> arrayMap;
        List packagesForOps = this.mAppOpsManager.getPackagesForOps(this.mAppOpsOpCodes);
        int size = packagesForOps != null ? packagesForOps.size() : 0;
        for (int i = 0; i < size; i++) {
            AppOpsManager.PackageOps packageOps = (AppOpsManager.PackageOps) packagesForOps.get(i);
            int userId = UserHandle.getUserId(packageOps.getUid());
            if (isThisUserAProfileOfCurrentUser(userId) && (arrayMap = sparseArray.get(userId)) != null) {
                PermissionState permissionState = arrayMap.get(packageOps.getPackageName());
                if (permissionState == null) {
                    Log.w("AppStateAppOpsBridge", "AppOp permission exists for package " + packageOps.getPackageName() + " of user " + userId + " but package doesn't exist or did not request " + this.mPermissions + " access");
                } else if (packageOps.getOps().size() >= 1) {
                    permissionState.appOpMode = ((AppOpsManager.OpEntry) packageOps.getOps().get(0)).getMode();
                } else {
                    Log.w("AppStateAppOpsBridge", "No AppOps permission exists for package " + packageOps.getPackageName());
                }
            }
        }
    }

    private boolean shouldIgnorePackage(String str) {
        return str.equals("android") || str.equals(this.mContext.getPackageName());
    }

    public static class PermissionState {
        public int appOpMode = 3;
        public PackageInfo packageInfo;
        public final String packageName;
        public boolean permissionDeclared;
        public boolean staticPermissionGranted;
        public final UserHandle userHandle;

        public PermissionState(String str, UserHandle userHandle) {
            this.packageName = str;
            this.userHandle = userHandle;
        }

        public boolean isPermissible() {
            if (this.appOpMode == 3) {
                return this.staticPermissionGranted;
            }
            return this.appOpMode == 0;
        }
    }
}
