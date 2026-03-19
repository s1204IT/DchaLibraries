package com.android.server.connectivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.net.Uri;
import android.net.util.NetworkConstants;
import android.os.INetworkManagementService;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import com.android.server.pm.Settings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PermissionMonitor {
    private static final boolean DBG = true;
    private static final String TAG = "PermissionMonitor";
    private final Context mContext;
    private final INetworkManagementService mNetd;
    private final PackageManager mPackageManager;
    private final UserManager mUserManager;
    private static final Boolean SYSTEM = Boolean.TRUE;
    private static final Boolean NETWORK = Boolean.FALSE;
    private final Set<Integer> mUsers = new HashSet();
    private final Map<Integer, Boolean> mApps = new HashMap();
    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int intExtra = intent.getIntExtra("android.intent.extra.user_handle", -10000);
            int intExtra2 = intent.getIntExtra("android.intent.extra.UID", -1);
            Uri data = intent.getData();
            String schemeSpecificPart = data != null ? data.getSchemeSpecificPart() : null;
            if ("android.intent.action.USER_ADDED".equals(action)) {
                PermissionMonitor.this.onUserAdded(intExtra);
                return;
            }
            if ("android.intent.action.USER_REMOVED".equals(action)) {
                PermissionMonitor.this.onUserRemoved(intExtra);
            } else if ("android.intent.action.PACKAGE_ADDED".equals(action)) {
                PermissionMonitor.this.onAppAdded(schemeSpecificPart, intExtra2);
            } else if ("android.intent.action.PACKAGE_REMOVED".equals(action)) {
                PermissionMonitor.this.onAppRemoved(intExtra2);
            }
        }
    };

    public PermissionMonitor(Context context, INetworkManagementService iNetworkManagementService) {
        this.mContext = context;
        this.mPackageManager = context.getPackageManager();
        this.mUserManager = UserManager.get(context);
        this.mNetd = iNetworkManagementService;
    }

    public synchronized void startMonitoring() {
        Boolean bool;
        log("Monitoring");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_ADDED");
        intentFilter.addAction("android.intent.action.USER_REMOVED");
        this.mContext.registerReceiverAsUser(this.mIntentReceiver, UserHandle.ALL, intentFilter, null, null);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.intent.action.PACKAGE_ADDED");
        intentFilter2.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter2.addDataScheme(Settings.ATTR_PACKAGE);
        this.mContext.registerReceiverAsUser(this.mIntentReceiver, UserHandle.ALL, intentFilter2, null, null);
        List<PackageInfo> installedPackages = this.mPackageManager.getInstalledPackages(4096);
        if (installedPackages == null) {
            loge("No apps");
            return;
        }
        for (PackageInfo packageInfo : installedPackages) {
            int i = packageInfo.applicationInfo != null ? packageInfo.applicationInfo.uid : -1;
            if (i >= 0) {
                boolean zHasNetworkPermission = hasNetworkPermission(packageInfo);
                boolean zHasRestrictedNetworkPermission = hasRestrictedNetworkPermission(packageInfo);
                if ((zHasNetworkPermission || zHasRestrictedNetworkPermission) && ((bool = this.mApps.get(Integer.valueOf(i))) == null || bool == NETWORK)) {
                    this.mApps.put(Integer.valueOf(i), Boolean.valueOf(zHasRestrictedNetworkPermission));
                }
            }
        }
        List users = this.mUserManager.getUsers(true);
        if (users != null) {
            Iterator it = users.iterator();
            while (it.hasNext()) {
                this.mUsers.add(Integer.valueOf(((UserInfo) it.next()).id));
            }
        }
        log("Users: " + this.mUsers.size() + ", Apps: " + this.mApps.size());
        update(this.mUsers, this.mApps, true);
    }

    @VisibleForTesting
    boolean isPreinstalledSystemApp(PackageInfo packageInfo) {
        return ((packageInfo.applicationInfo != null ? packageInfo.applicationInfo.flags : 0) & NetworkConstants.ICMPV6_ECHO_REPLY_TYPE) != 0;
    }

    @VisibleForTesting
    boolean hasPermission(PackageInfo packageInfo, String str) {
        int iIndexOf;
        return (packageInfo.requestedPermissions == null || packageInfo.requestedPermissionsFlags == null || (iIndexOf = ArrayUtils.indexOf(packageInfo.requestedPermissions, str)) < 0 || iIndexOf >= packageInfo.requestedPermissionsFlags.length || (packageInfo.requestedPermissionsFlags[iIndexOf] & 2) == 0) ? false : true;
    }

    private boolean hasNetworkPermission(PackageInfo packageInfo) {
        return hasPermission(packageInfo, "android.permission.CHANGE_NETWORK_STATE");
    }

    private boolean hasRestrictedNetworkPermission(PackageInfo packageInfo) {
        return isPreinstalledSystemApp(packageInfo) || hasPermission(packageInfo, "android.permission.CONNECTIVITY_INTERNAL") || hasPermission(packageInfo, "android.permission.CONNECTIVITY_USE_RESTRICTED_NETWORKS");
    }

    private boolean hasUseBackgroundNetworksPermission(PackageInfo packageInfo) {
        return hasPermission(packageInfo, "android.permission.CHANGE_NETWORK_STATE") || hasPermission(packageInfo, "android.permission.CONNECTIVITY_USE_RESTRICTED_NETWORKS") || hasPermission(packageInfo, "android.permission.CONNECTIVITY_INTERNAL") || hasPermission(packageInfo, "android.permission.NETWORK_STACK") || isPreinstalledSystemApp(packageInfo);
    }

    public boolean hasUseBackgroundNetworksPermission(int i) {
        String[] packagesForUid = this.mPackageManager.getPackagesForUid(i);
        if (packagesForUid == null || packagesForUid.length == 0) {
            return false;
        }
        try {
            return hasUseBackgroundNetworksPermission(this.mPackageManager.getPackageInfoAsUser(packagesForUid[0], 4096, UserHandle.getUserId(i)));
        } catch (PackageManager.NameNotFoundException e) {
            loge("NameNotFoundException " + packagesForUid[0], e);
            return false;
        }
    }

    private int[] toIntArray(List<Integer> list) {
        int[] iArr = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            iArr[i] = list.get(i).intValue();
        }
        return iArr;
    }

    private void update(Set<Integer> set, Map<Integer, Boolean> map, boolean z) {
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        for (Map.Entry<Integer, Boolean> entry : map.entrySet()) {
            ArrayList arrayList3 = entry.getValue().booleanValue() ? arrayList2 : arrayList;
            Iterator<Integer> it = set.iterator();
            while (it.hasNext()) {
                arrayList3.add(Integer.valueOf(UserHandle.getUid(it.next().intValue(), entry.getKey().intValue())));
            }
        }
        try {
            if (z) {
                this.mNetd.setPermission("NETWORK", toIntArray(arrayList));
                this.mNetd.setPermission("SYSTEM", toIntArray(arrayList2));
            } else {
                this.mNetd.clearPermission(toIntArray(arrayList));
                this.mNetd.clearPermission(toIntArray(arrayList2));
            }
        } catch (RemoteException e) {
            loge("Exception when updating permissions: " + e);
        }
    }

    private synchronized void onUserAdded(int i) {
        if (i < 0) {
            loge("Invalid user in onUserAdded: " + i);
            return;
        }
        this.mUsers.add(Integer.valueOf(i));
        HashSet hashSet = new HashSet();
        hashSet.add(Integer.valueOf(i));
        update(hashSet, this.mApps, true);
    }

    private synchronized void onUserRemoved(int i) {
        if (i < 0) {
            loge("Invalid user in onUserRemoved: " + i);
            return;
        }
        this.mUsers.remove(Integer.valueOf(i));
        HashSet hashSet = new HashSet();
        hashSet.add(Integer.valueOf(i));
        update(hashSet, this.mApps, false);
    }

    private Boolean highestPermissionForUid(Boolean bool, String str) {
        if (bool == SYSTEM) {
            return bool;
        }
        try {
            PackageInfo packageInfo = this.mPackageManager.getPackageInfo(str, 4096);
            boolean zHasNetworkPermission = hasNetworkPermission(packageInfo);
            boolean zHasRestrictedNetworkPermission = hasRestrictedNetworkPermission(packageInfo);
            if (zHasNetworkPermission || zHasRestrictedNetworkPermission) {
                return Boolean.valueOf(zHasRestrictedNetworkPermission);
            }
            return bool;
        } catch (PackageManager.NameNotFoundException e) {
            loge("NameNotFoundException " + str);
            return bool;
        }
    }

    private synchronized void onAppAdded(String str, int i) {
        if (!TextUtils.isEmpty(str) && i >= 0) {
            Boolean boolHighestPermissionForUid = highestPermissionForUid(this.mApps.get(Integer.valueOf(i)), str);
            if (boolHighestPermissionForUid != this.mApps.get(Integer.valueOf(i))) {
                this.mApps.put(Integer.valueOf(i), boolHighestPermissionForUid);
                HashMap map = new HashMap();
                map.put(Integer.valueOf(i), boolHighestPermissionForUid);
                update(this.mUsers, map, true);
            }
            return;
        }
        loge("Invalid app in onAppAdded: " + str + " | " + i);
    }

    private synchronized void onAppRemoved(int i) {
        if (i < 0) {
            loge("Invalid app in onAppRemoved: " + i);
            return;
        }
        HashMap map = new HashMap();
        Boolean bool = null;
        String[] packagesForUid = this.mPackageManager.getPackagesForUid(i);
        if (packagesForUid != null && packagesForUid.length > 0) {
            Boolean boolHighestPermissionForUid = null;
            for (String str : packagesForUid) {
                boolHighestPermissionForUid = highestPermissionForUid(boolHighestPermissionForUid, str);
                if (boolHighestPermissionForUid == SYSTEM) {
                    return;
                }
            }
            bool = boolHighestPermissionForUid;
        }
        if (bool == this.mApps.get(Integer.valueOf(i))) {
            return;
        }
        if (bool != null) {
            this.mApps.put(Integer.valueOf(i), bool);
            map.put(Integer.valueOf(i), bool);
            update(this.mUsers, map, true);
        } else {
            this.mApps.remove(Integer.valueOf(i));
            map.put(Integer.valueOf(i), NETWORK);
            update(this.mUsers, map, false);
        }
    }

    private static void log(String str) {
        Log.d(TAG, str);
    }

    private static void loge(String str) {
        Log.e(TAG, str);
    }

    private static void loge(String str, Throwable th) {
        Log.e(TAG, str, th);
    }
}
