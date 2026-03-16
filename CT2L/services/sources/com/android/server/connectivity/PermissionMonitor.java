package com.android.server.connectivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.net.Uri;
import android.os.INetworkManagementService;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.pm.PackageManagerService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PermissionMonitor {
    private static final boolean DBG = true;
    private static final boolean NETWORK = false;
    private static final boolean SYSTEM = true;
    private static final String TAG = "PermissionMonitor";
    private final Context mContext;
    private final INetworkManagementService mNetd;
    private final PackageManager mPackageManager;
    private final UserManager mUserManager;
    private final Set<Integer> mUsers = new HashSet();
    private final Map<Integer, Boolean> mApps = new HashMap();
    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int user = intent.getIntExtra("android.intent.extra.user_handle", -10000);
            int appUid = intent.getIntExtra("android.intent.extra.UID", -1);
            Uri appData = intent.getData();
            String appName = appData != null ? appData.getSchemeSpecificPart() : null;
            if ("android.intent.action.USER_ADDED".equals(action)) {
                PermissionMonitor.this.onUserAdded(user);
                return;
            }
            if ("android.intent.action.USER_REMOVED".equals(action)) {
                PermissionMonitor.this.onUserRemoved(user);
            } else if ("android.intent.action.PACKAGE_ADDED".equals(action)) {
                PermissionMonitor.this.onAppAdded(appName, appUid);
            } else if ("android.intent.action.PACKAGE_REMOVED".equals(action)) {
                PermissionMonitor.this.onAppRemoved(appUid);
            }
        }
    };

    public PermissionMonitor(Context context, INetworkManagementService netd) {
        this.mContext = context;
        this.mPackageManager = context.getPackageManager();
        this.mUserManager = UserManager.get(context);
        this.mNetd = netd;
    }

    public synchronized void startMonitoring() {
        log("Monitoring");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_ADDED");
        intentFilter.addAction("android.intent.action.USER_REMOVED");
        this.mContext.registerReceiverAsUser(this.mIntentReceiver, UserHandle.ALL, intentFilter, null, null);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.intent.action.PACKAGE_ADDED");
        intentFilter2.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter2.addDataScheme("package");
        this.mContext.registerReceiverAsUser(this.mIntentReceiver, UserHandle.ALL, intentFilter2, null, null);
        List<PackageInfo> apps = this.mPackageManager.getInstalledPackages(PackageManagerService.DumpState.DUMP_VERSION);
        if (apps == null) {
            loge("No apps");
        } else {
            for (PackageInfo app : apps) {
                int uid = app.applicationInfo != null ? app.applicationInfo.uid : -1;
                if (uid >= 0) {
                    boolean isNetwork = hasNetworkPermission(app);
                    boolean isSystem = hasSystemPermission(app);
                    if (isNetwork || isSystem) {
                        Boolean permission = this.mApps.get(Integer.valueOf(uid));
                        if (permission == null || !permission.booleanValue()) {
                            this.mApps.put(Integer.valueOf(uid), Boolean.valueOf(isSystem));
                        }
                    }
                }
            }
            List<UserInfo> users = this.mUserManager.getUsers(true);
            if (users != null) {
                for (UserInfo user : users) {
                    this.mUsers.add(Integer.valueOf(user.id));
                }
            }
            log("Users: " + this.mUsers.size() + ", Apps: " + this.mApps.size());
            update(this.mUsers, this.mApps, true);
        }
    }

    private boolean hasPermission(PackageInfo app, String permission) {
        if (app.requestedPermissions != null) {
            String[] arr$ = app.requestedPermissions;
            for (String p : arr$) {
                if (permission.equals(p)) {
                    return true;
                }
            }
        }
        return NETWORK;
    }

    private boolean hasNetworkPermission(PackageInfo app) {
        return hasPermission(app, "android.permission.CHANGE_NETWORK_STATE");
    }

    private boolean hasSystemPermission(PackageInfo app) {
        int flags = app.applicationInfo != null ? app.applicationInfo.flags : 0;
        if ((flags & 1) != 0 || (flags & 128) != 0) {
            return true;
        }
        return hasPermission(app, "android.permission.CONNECTIVITY_INTERNAL");
    }

    private int[] toIntArray(List<Integer> list) {
        int[] array = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i).intValue();
        }
        return array;
    }

    private void update(Set<Integer> users, Map<Integer, Boolean> apps, boolean add) {
        List<Integer> network = new ArrayList<>();
        List<Integer> system = new ArrayList<>();
        for (Map.Entry<Integer, Boolean> app : apps.entrySet()) {
            List<Integer> list = app.getValue().booleanValue() ? system : network;
            Iterator<Integer> it = users.iterator();
            while (it.hasNext()) {
                int user = it.next().intValue();
                list.add(Integer.valueOf(UserHandle.getUid(user, app.getKey().intValue())));
            }
        }
        try {
            if (add) {
                this.mNetd.setPermission("NETWORK", toIntArray(network));
                this.mNetd.setPermission("SYSTEM", toIntArray(system));
            } else {
                this.mNetd.clearPermission(toIntArray(network));
                this.mNetd.clearPermission(toIntArray(system));
            }
        } catch (RemoteException e) {
            loge("Exception when updating permissions: " + e);
        }
    }

    private synchronized void onUserAdded(int user) {
        if (user < 0) {
            loge("Invalid user in onUserAdded: " + user);
        } else {
            this.mUsers.add(Integer.valueOf(user));
            Set<Integer> users = new HashSet<>();
            users.add(Integer.valueOf(user));
            update(users, this.mApps, true);
        }
    }

    private synchronized void onUserRemoved(int user) {
        if (user < 0) {
            loge("Invalid user in onUserRemoved: " + user);
        } else {
            this.mUsers.remove(Integer.valueOf(user));
            Set<Integer> users = new HashSet<>();
            users.add(Integer.valueOf(user));
            update(users, this.mApps, NETWORK);
        }
    }

    private synchronized void onAppAdded(String appName, int appUid) {
        Boolean permission;
        if (TextUtils.isEmpty(appName) || appUid < 0) {
            loge("Invalid app in onAppAdded: " + appName + " | " + appUid);
        } else {
            try {
                PackageInfo app = this.mPackageManager.getPackageInfo(appName, PackageManagerService.DumpState.DUMP_VERSION);
                boolean isNetwork = hasNetworkPermission(app);
                boolean isSystem = hasSystemPermission(app);
                if ((isNetwork || isSystem) && ((permission = this.mApps.get(Integer.valueOf(appUid))) == null || !permission.booleanValue())) {
                    this.mApps.put(Integer.valueOf(appUid), Boolean.valueOf(isSystem));
                    Map<Integer, Boolean> apps = new HashMap<>();
                    apps.put(Integer.valueOf(appUid), Boolean.valueOf(isSystem));
                    update(this.mUsers, apps, true);
                }
            } catch (PackageManager.NameNotFoundException e) {
                loge("NameNotFoundException in onAppAdded: " + e);
            }
        }
    }

    private synchronized void onAppRemoved(int appUid) {
        if (appUid < 0) {
            loge("Invalid app in onAppRemoved: " + appUid);
        } else {
            this.mApps.remove(Integer.valueOf(appUid));
            Map<Integer, Boolean> apps = new HashMap<>();
            apps.put(Integer.valueOf(appUid), Boolean.valueOf(NETWORK));
            update(this.mUsers, apps, NETWORK);
        }
    }

    private static void log(String s) {
        Log.d(TAG, s);
    }

    private static void loge(String s) {
        Log.e(TAG, s);
    }
}
