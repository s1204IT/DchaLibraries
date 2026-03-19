package com.android.server.wifi.util;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.RemoteException;
import android.os.UserManager;
import android.util.EventLog;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.WifiLog;
import com.android.server.wifi.WifiSettingsStore;
import java.util.Iterator;

public class WifiPermissionsUtil {
    private static final String TAG = "WifiPermissionsUtil";
    private final AppOpsManager mAppOps;
    private final Context mContext;
    private WifiLog mLog;
    private final WifiSettingsStore mSettingsStore;
    private final UserManager mUserManager;
    private final WifiPermissionsWrapper mWifiPermissionsWrapper;

    public WifiPermissionsUtil(WifiPermissionsWrapper wifiPermissionsWrapper, Context context, WifiSettingsStore wifiSettingsStore, UserManager userManager, WifiInjector wifiInjector) {
        this.mWifiPermissionsWrapper = wifiPermissionsWrapper;
        this.mContext = context;
        this.mUserManager = userManager;
        this.mAppOps = (AppOpsManager) this.mContext.getSystemService("appops");
        this.mSettingsStore = wifiSettingsStore;
        this.mLog = wifiInjector.makeLog(TAG);
    }

    public boolean checkConfigOverridePermission(int i) {
        try {
            return this.mWifiPermissionsWrapper.getOverrideWifiConfigPermission(i) == 0;
        } catch (RemoteException e) {
            this.mLog.err("Error checking for permission: %").r(e.getMessage()).flush();
            return false;
        }
    }

    public boolean checkChangePermission(int i) {
        try {
            return this.mWifiPermissionsWrapper.getChangeWifiConfigPermission(i) == 0;
        } catch (RemoteException e) {
            this.mLog.err("Error checking for permission: %").r(e.getMessage()).flush();
            return false;
        }
    }

    public boolean checkWifiAccessPermission(int i) {
        try {
            return this.mWifiPermissionsWrapper.getAccessWifiStatePermission(i) == 0;
        } catch (RemoteException e) {
            this.mLog.err("Error checking for permission: %").r(e.getMessage()).flush();
            return false;
        }
    }

    public void enforceLocationPermission(String str, int i) {
        if (!checkCallersLocationPermission(str, i)) {
            throw new SecurityException("UID " + i + " does not have Coarse Location permission");
        }
    }

    public boolean checkCallersLocationPermission(String str, int i) {
        return this.mWifiPermissionsWrapper.getUidPermission("android.permission.ACCESS_COARSE_LOCATION", i) == 0 && checkAppOpAllowed(0, str, i);
    }

    public void enforceFineLocationPermission(String str, int i) {
        if (!checkCallersFineLocationPermission(str, i)) {
            throw new SecurityException("UID " + i + " does not have Fine Location permission");
        }
    }

    public boolean checkCallersFineLocationPermission(String str, int i) {
        if (this.mWifiPermissionsWrapper.getUidPermission("android.permission.ACCESS_FINE_LOCATION", i) == 0 && checkAppOpAllowed(1, str, i)) {
            return true;
        }
        return false;
    }

    public void enforceCanAccessScanResults(String str, int i) throws SecurityException {
        this.mAppOps.checkPackage(i, str);
        if (checkNetworkSettingsPermission(i) || checkNetworkSetupWizardPermission(i)) {
            return;
        }
        if (!isLocationModeEnabled()) {
            throw new SecurityException("Location mode is disabled for the device");
        }
        boolean zCheckCallerHasPeersMacAddressPermission = checkCallerHasPeersMacAddressPermission(i);
        boolean zCheckCallersLocationPermission = checkCallersLocationPermission(str, i);
        if (!zCheckCallerHasPeersMacAddressPermission && !zCheckCallersLocationPermission) {
            throw new SecurityException("UID " + i + " has no location permission");
        }
        if (!isScanAllowedbyApps(str, i)) {
            throw new SecurityException("UID " + i + " has no wifi scan permission");
        }
        if (!isCurrentProfile(i) && !checkInteractAcrossUsersFull(i)) {
            throw new SecurityException("UID " + i + " profile not permitted");
        }
    }

    private boolean checkCallerHasPeersMacAddressPermission(int i) {
        return this.mWifiPermissionsWrapper.getUidPermission("android.permission.PEERS_MAC_ADDRESS", i) == 0;
    }

    private boolean isScanAllowedbyApps(String str, int i) {
        return checkAppOpAllowed(10, str, i);
    }

    private boolean checkInteractAcrossUsersFull(int i) {
        return this.mWifiPermissionsWrapper.getUidPermission("android.permission.INTERACT_ACROSS_USERS_FULL", i) == 0;
    }

    private boolean isCurrentProfile(int i) {
        int currentUser = this.mWifiPermissionsWrapper.getCurrentUser();
        int callingUserId = this.mWifiPermissionsWrapper.getCallingUserId(i);
        if (callingUserId == currentUser) {
            return true;
        }
        Iterator it = this.mUserManager.getProfiles(currentUser).iterator();
        while (it.hasNext()) {
            if (((UserInfo) it.next()).id == callingUserId) {
                return true;
            }
        }
        return false;
    }

    public boolean isLegacyVersion(String str, int i) {
        if (this.mContext.getPackageManager().getApplicationInfo(str, 0).targetSdkVersion >= i) {
            return false;
        }
        return true;
    }

    private boolean checkAppOpAllowed(int i, String str, int i2) {
        return this.mAppOps.noteOp(i, i2, str) == 0;
    }

    private boolean isLocationModeEnabled() {
        return this.mSettingsStore.getLocationModeSetting(this.mContext) != 0;
    }

    public boolean checkNetworkSettingsPermission(int i) {
        return this.mWifiPermissionsWrapper.getUidPermission("android.permission.NETWORK_SETTINGS", i) == 0;
    }

    public boolean checkNetworkSetupWizardPermission(int i) {
        return this.mWifiPermissionsWrapper.getUidPermission("android.permission.NETWORK_SETUP_WIZARD", i) == 0;
    }

    public boolean doesUidBelongToCurrentUser(int i) {
        if (i == 1000 || checkNetworkSettingsPermission(i)) {
            return true;
        }
        boolean zIsCurrentProfile = isCurrentProfile(i);
        if (!zIsCurrentProfile) {
            EventLog.writeEvent(1397638484, "174749461", -1, "Non foreground user trying to modify wifi configuration");
        }
        return zIsCurrentProfile;
    }
}
