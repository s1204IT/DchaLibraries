package com.android.managedprovisioning.task;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import com.android.managedprovisioning.ProvisionLogger;

public class SetDevicePolicyTask {
    private String mAdminReceiver;
    private final Callback mCallback;
    private final Context mContext;
    private DevicePolicyManager mDevicePolicyManager;
    private final String mOwner;
    private PackageManager mPackageManager;
    private final String mPackageName;

    public static abstract class Callback {
        public abstract void onError(int i);

        public abstract void onSuccess();
    }

    public SetDevicePolicyTask(Context context, String packageName, String owner, Callback callback) {
        this.mCallback = callback;
        this.mContext = context;
        this.mPackageName = packageName;
        this.mOwner = owner;
        this.mPackageManager = this.mContext.getPackageManager();
        this.mDevicePolicyManager = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
    }

    public void run() {
        if (isPackageInstalled()) {
            enableDevicePolicyApp();
            setActiveAdmin();
            setDeviceOwner();
            this.mCallback.onSuccess();
        }
    }

    private boolean isPackageInstalled() {
        try {
            PackageInfo pi = this.mPackageManager.getPackageInfo(this.mPackageName, 2);
            ActivityInfo[] arr$ = pi.receivers;
            for (ActivityInfo ai : arr$) {
                if (!TextUtils.isEmpty(ai.permission) && ai.permission.equals("android.permission.BIND_DEVICE_ADMIN")) {
                    this.mAdminReceiver = ai.name;
                    return true;
                }
            }
            this.mCallback.onError(1);
            return false;
        } catch (PackageManager.NameNotFoundException e) {
            this.mCallback.onError(0);
            return false;
        }
    }

    private void enableDevicePolicyApp() {
        int enabledSetting = this.mPackageManager.getApplicationEnabledSetting(this.mPackageName);
        if (enabledSetting != 0) {
            this.mPackageManager.setApplicationEnabledSetting(this.mPackageName, 0, 0);
        }
    }

    public void setActiveAdmin() {
        ProvisionLogger.logd("Setting " + this.mPackageName + " as active admin.");
        ComponentName component = new ComponentName(this.mPackageName, this.mAdminReceiver);
        this.mDevicePolicyManager.setActiveAdmin(component, true);
    }

    public void setDeviceOwner() {
        ProvisionLogger.logd("Setting " + this.mPackageName + " as device owner " + this.mOwner + ".");
        if (!this.mDevicePolicyManager.isDeviceOwner(this.mPackageName)) {
            this.mDevicePolicyManager.setDeviceOwner(this.mPackageName, this.mOwner);
        }
    }
}
