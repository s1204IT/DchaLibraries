package com.android.settings.fuelgauge;

import android.os.IDeviceIdleController;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.ArraySet;
import android.util.Log;

public class PowerWhitelistBackend {
    private static final PowerWhitelistBackend INSTANCE = new PowerWhitelistBackend();
    private final ArraySet<String> mWhitelistedApps = new ArraySet<>();
    private final ArraySet<String> mSysWhitelistedApps = new ArraySet<>();
    private final IDeviceIdleController mDeviceIdleService = IDeviceIdleController.Stub.asInterface(ServiceManager.getService("deviceidle"));

    public PowerWhitelistBackend() {
        refreshList();
    }

    public boolean isSysWhitelisted(String pkg) {
        return this.mSysWhitelistedApps.contains(pkg);
    }

    public boolean isWhitelisted(String pkg) {
        return this.mWhitelistedApps.contains(pkg);
    }

    public void addApp(String pkg) {
        try {
            this.mDeviceIdleService.addPowerSaveWhitelistApp(pkg);
            this.mWhitelistedApps.add(pkg);
        } catch (RemoteException e) {
            Log.w("PowerWhitelistBackend", "Unable to reach IDeviceIdleController", e);
        }
    }

    public void removeApp(String pkg) {
        try {
            this.mDeviceIdleService.removePowerSaveWhitelistApp(pkg);
            this.mWhitelistedApps.remove(pkg);
        } catch (RemoteException e) {
            Log.w("PowerWhitelistBackend", "Unable to reach IDeviceIdleController", e);
        }
    }

    private void refreshList() {
        this.mSysWhitelistedApps.clear();
        this.mWhitelistedApps.clear();
        try {
            String[] whitelistedApps = this.mDeviceIdleService.getFullPowerWhitelist();
            for (String app : whitelistedApps) {
                this.mWhitelistedApps.add(app);
            }
            String[] sysWhitelistedApps = this.mDeviceIdleService.getSystemPowerWhitelist();
            for (String app2 : sysWhitelistedApps) {
                this.mSysWhitelistedApps.add(app2);
            }
        } catch (RemoteException e) {
            Log.w("PowerWhitelistBackend", "Unable to reach IDeviceIdleController", e);
        }
    }

    public static PowerWhitelistBackend getInstance() {
        return INSTANCE;
    }
}
