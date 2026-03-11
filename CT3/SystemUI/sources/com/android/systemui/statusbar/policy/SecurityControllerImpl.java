package com.android.systemui.statusbar.policy;

import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.net.ConnectivityManager;
import android.net.IConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.net.LegacyVpnInfo;
import com.android.internal.net.VpnConfig;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.SecurityController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;

public class SecurityControllerImpl implements SecurityController {
    private static final boolean DEBUG = Log.isLoggable("SecurityController", 3);
    private static final NetworkRequest REQUEST = new NetworkRequest.Builder().removeCapability(15).removeCapability(13).removeCapability(14).build();
    private final ConnectivityManager mConnectivityManager;
    private final Context mContext;
    private int mCurrentUserId;
    private final DevicePolicyManager mDevicePolicyManager;
    private final UserManager mUserManager;
    private int mVpnUserId;

    @GuardedBy("mCallbacks")
    private final ArrayList<SecurityController.SecurityControllerCallback> mCallbacks = new ArrayList<>();
    private SparseArray<VpnConfig> mCurrentVpns = new SparseArray<>();
    private final ConnectivityManager.NetworkCallback mNetworkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            if (SecurityControllerImpl.DEBUG) {
                Log.d("SecurityController", "onAvailable " + network.netId);
            }
            SecurityControllerImpl.this.updateState();
            SecurityControllerImpl.this.fireCallbacks();
        }

        @Override
        public void onLost(Network network) {
            if (SecurityControllerImpl.DEBUG) {
                Log.d("SecurityController", "onLost " + network.netId);
            }
            SecurityControllerImpl.this.updateState();
            SecurityControllerImpl.this.fireCallbacks();
        }
    };
    private final IConnectivityManager mConnectivityManagerService = IConnectivityManager.Stub.asInterface(ServiceManager.getService("connectivity"));

    public SecurityControllerImpl(Context context) {
        this.mContext = context;
        this.mDevicePolicyManager = (DevicePolicyManager) context.getSystemService("device_policy");
        this.mConnectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
        this.mUserManager = (UserManager) context.getSystemService("user");
        this.mConnectivityManager.registerNetworkCallback(REQUEST, this.mNetworkCallback);
        onUserSwitched(ActivityManager.getCurrentUser());
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("SecurityController state:");
        pw.print("  mCurrentVpns={");
        for (int i = 0; i < this.mCurrentVpns.size(); i++) {
            if (i > 0) {
                pw.print(", ");
            }
            pw.print(this.mCurrentVpns.keyAt(i));
            pw.print('=');
            pw.print(this.mCurrentVpns.valueAt(i).user);
        }
        pw.println("}");
    }

    @Override
    public boolean isDeviceManaged() {
        return this.mDevicePolicyManager.isDeviceManaged();
    }

    @Override
    public String getDeviceOwnerName() {
        return this.mDevicePolicyManager.getDeviceOwnerNameOnAnyUser();
    }

    @Override
    public boolean hasProfileOwner() {
        return this.mDevicePolicyManager.getProfileOwnerAsUser(this.mCurrentUserId) != null;
    }

    @Override
    public String getProfileOwnerName() {
        for (int profileId : this.mUserManager.getProfileIdsWithDisabled(this.mCurrentUserId)) {
            String name = this.mDevicePolicyManager.getProfileOwnerNameAsUser(profileId);
            if (name != null) {
                return name;
            }
        }
        return null;
    }

    @Override
    public String getPrimaryVpnName() {
        VpnConfig cfg = this.mCurrentVpns.get(this.mVpnUserId);
        if (cfg != null) {
            return getNameForVpnConfig(cfg, new UserHandle(this.mVpnUserId));
        }
        return null;
    }

    @Override
    public String getProfileVpnName() {
        VpnConfig cfg;
        for (int profileId : this.mUserManager.getProfileIdsWithDisabled(this.mVpnUserId)) {
            if (profileId != this.mVpnUserId && (cfg = this.mCurrentVpns.get(profileId)) != null) {
                return getNameForVpnConfig(cfg, UserHandle.of(profileId));
            }
        }
        return null;
    }

    @Override
    public boolean isVpnEnabled() {
        for (int profileId : this.mUserManager.getProfileIdsWithDisabled(this.mVpnUserId)) {
            if (this.mCurrentVpns.get(profileId) != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isVpnRestricted() {
        UserHandle currentUser = new UserHandle(this.mCurrentUserId);
        if (this.mUserManager.getUserInfo(this.mCurrentUserId).isRestricted()) {
            return true;
        }
        return this.mUserManager.hasUserRestriction("no_config_vpn", currentUser);
    }

    @Override
    public void removeCallback(SecurityController.SecurityControllerCallback callback) {
        synchronized (this.mCallbacks) {
            if (callback == null) {
                return;
            }
            if (DEBUG) {
                Log.d("SecurityController", "removeCallback " + callback);
            }
            this.mCallbacks.remove(callback);
        }
    }

    @Override
    public void addCallback(SecurityController.SecurityControllerCallback callback) {
        synchronized (this.mCallbacks) {
            if (callback != null) {
                if (!this.mCallbacks.contains(callback)) {
                    if (DEBUG) {
                        Log.d("SecurityController", "addCallback " + callback);
                    }
                    this.mCallbacks.add(callback);
                }
            }
        }
    }

    public void onUserSwitched(int newUserId) {
        this.mCurrentUserId = newUserId;
        UserInfo newUserInfo = this.mUserManager.getUserInfo(newUserId);
        if (newUserInfo.isRestricted()) {
            this.mVpnUserId = newUserInfo.restrictedProfileParentId;
        } else {
            this.mVpnUserId = this.mCurrentUserId;
        }
        fireCallbacks();
    }

    private String getNameForVpnConfig(VpnConfig cfg, UserHandle user) {
        if (cfg.legacy) {
            return this.mContext.getString(R.string.legacy_vpn_name);
        }
        String vpnPackage = cfg.user;
        try {
            Context userContext = this.mContext.createPackageContextAsUser(this.mContext.getPackageName(), 0, user);
            return VpnConfig.getVpnLabel(userContext, vpnPackage).toString();
        } catch (PackageManager.NameNotFoundException nnfe) {
            Log.e("SecurityController", "Package " + vpnPackage + " is not present", nnfe);
            return null;
        }
    }

    public void fireCallbacks() {
        synchronized (this.mCallbacks) {
            for (SecurityController.SecurityControllerCallback callback : this.mCallbacks) {
                callback.onStateChanged();
            }
        }
    }

    public void updateState() {
        LegacyVpnInfo legacyVpn;
        SparseArray<VpnConfig> vpns = new SparseArray<>();
        try {
            for (UserInfo user : this.mUserManager.getUsers()) {
                VpnConfig cfg = this.mConnectivityManagerService.getVpnConfig(user.id);
                if (cfg != null && (!cfg.legacy || ((legacyVpn = this.mConnectivityManagerService.getLegacyVpnInfo(user.id)) != null && legacyVpn.state == 3))) {
                    vpns.put(user.id, cfg);
                }
            }
            this.mCurrentVpns = vpns;
        } catch (RemoteException rme) {
            Log.e("SecurityController", "Unable to list active VPNs", rme);
        }
    }
}
