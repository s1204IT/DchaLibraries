package com.mediatek.server.am.AutoBootControl;

import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.ResolveInfo;
import android.os.IUserManager;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Log;
import com.android.internal.content.PackageMonitor;
import java.util.Iterator;
import java.util.List;

public class ReceiverController {
    static final boolean DEBUG = false;
    static final String TAG = "ReceiverController";
    private BootReceiverPolicy mBootReceiverPolicy;
    private static Context mContext = null;
    private static ReceiverController sInstance = null;
    private static boolean mMonitorEnabled = true;
    private ReceiverRecordHelper mRecordHelper = null;
    private final PackageMonitor mPackageMonitor = new PackageMonitor() {
        public void onPackageAdded(String packageName, int uid) {
            Log.d(ReceiverController.TAG, "onPackageAdded()");
            if (ReceiverController.this.mRecordHelper == null) {
                return;
            }
            ReceiverController.this.mRecordHelper.updateReceiverCache();
        }

        public void onPackageRemoved(String packageName, int uid) {
            Log.d(ReceiverController.TAG, "onPackageRemoved()");
            if (ReceiverController.this.mRecordHelper == null) {
                return;
            }
            ReceiverController.this.mRecordHelper.updateReceiverCache();
        }

        public void onPackagesAvailable(String[] packages) {
            Log.d(ReceiverController.TAG, "onPackagesAvailable()");
            if (ReceiverController.this.mRecordHelper == null) {
                return;
            }
            ReceiverController.this.mRecordHelper.updateReceiverCache();
        }

        public void onPackagesUnavailable(String[] packages) {
            Log.d(ReceiverController.TAG, "onPackagesUnavailable()");
            if (ReceiverController.this.mRecordHelper == null) {
                return;
            }
            ReceiverController.this.mRecordHelper.updateReceiverCache();
        }
    };

    public static ReceiverController getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ReceiverController(context);
        }
        return sInstance;
    }

    private ReceiverController(Context context) {
        this.mBootReceiverPolicy = null;
        mContext = context;
        this.mBootReceiverPolicy = BootReceiverPolicy.getInstance(mContext);
        initRecordHelper();
        this.mPackageMonitor.register(context, mContext.getMainLooper(), UserHandle.ALL, true);
        startMonitor("Normal Bootup Start");
    }

    public void startMonitor(String cause) {
        Log.d(TAG, "startMonitor(" + cause + ")");
    }

    public void stopMonitor(String cause) {
        Log.d(TAG, "stopMonitor(" + cause + ")");
    }

    private void initRecordHelper() {
        this.mRecordHelper = new ReceiverRecordHelper(mContext, getUserManagerService(), getPackageManagerService());
        this.mRecordHelper.initReceiverList();
        Log.d(TAG, "init ReceiverRecordHelper done.");
    }

    public void filterReceiver(Intent intent, List<ResolveInfo> resolveList, int userId) {
        String action = intent.getAction();
        if (!mMonitorEnabled) {
            return;
        }
        if (action == null) {
            Log.e(TAG, "filterReceiver() ignored with null action");
            return;
        }
        if (resolveList == null) {
            return;
        }
        if (!isValidUserId(userId)) {
            Log.e(TAG, "filterReceiver() ignored with invalid userId: " + userId);
            return;
        }
        if (!this.mBootReceiverPolicy.match(action)) {
            return;
        }
        this.mRecordHelper.updateReceiverCache();
        Iterator<ResolveInfo> itor = resolveList.iterator();
        while (itor.hasNext()) {
            ResolveInfo info = itor.next();
            if (info.activityInfo != null) {
                String packageName = info.activityInfo.packageName;
                Log.d(TAG, "filterReceiver() - package = " + packageName + " has action = " + action);
                if (!checkStrictPolicyAllowed(action, userId, packageName)) {
                    itor.remove();
                }
            }
        }
    }

    private boolean checkStrictPolicyAllowed(String action, int userId, String packageName) {
        boolean allowed = true;
        synchronized (this.mRecordHelper) {
            if (this.mRecordHelper != null && !this.mRecordHelper.getReceiverDataEnabled(userId, packageName)) {
                Log.d(TAG, "checkStrictPolicyAllowed() -  denied " + action + " to package: " + packageName + " at User(" + userId + ")");
                allowed = false;
            }
        }
        return allowed;
    }

    public static IPackageManager getPackageManagerService() {
        IPackageManager pm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        if (pm == null) {
            throw new RuntimeException("null package manager service");
        }
        return pm;
    }

    public static IUserManager getUserManagerService() {
        IUserManager um = IUserManager.Stub.asInterface(ServiceManager.getService("user"));
        if (um == null) {
            throw new RuntimeException("null user manager service");
        }
        return um;
    }

    public boolean isValidUserId(int userId) {
        if (userId >= 0 && userId < 100000) {
            return true;
        }
        Log.e(TAG, "Invalid userId: " + userId);
        return false;
    }
}
