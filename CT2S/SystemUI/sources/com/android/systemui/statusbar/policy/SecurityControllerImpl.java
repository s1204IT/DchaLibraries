package com.android.systemui.statusbar.policy;

import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.IConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.net.VpnConfig;
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
    private VpnConfig mVpnConfig;
    private String mVpnName;
    private final IConnectivityManager mConnectivityService = IConnectivityManager.Stub.asInterface(ServiceManager.getService("connectivity"));
    private final ArrayList<SecurityController.SecurityControllerCallback> mCallbacks = new ArrayList<>();
    private int mCurrentVpnNetworkId = -1;
    private final ConnectivityManager.NetworkCallback mNetworkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            NetworkCapabilities networkCapabilities = SecurityControllerImpl.this.mConnectivityManager.getNetworkCapabilities(network);
            if (SecurityControllerImpl.DEBUG) {
                Log.d("SecurityController", "onAvailable " + network.netId + " : " + networkCapabilities);
            }
            if (networkCapabilities.hasTransport(4)) {
                SecurityControllerImpl.this.setCurrentNetid(network.netId);
            }
        }

        @Override
        public void onLost(Network network) {
            if (SecurityControllerImpl.DEBUG) {
                Log.d("SecurityController", "onLost " + network.netId);
            }
            if (SecurityControllerImpl.this.mCurrentVpnNetworkId == network.netId) {
                SecurityControllerImpl.this.setCurrentNetid(-1);
            }
        }
    };

    public SecurityControllerImpl(Context context) {
        this.mContext = context;
        this.mDevicePolicyManager = (DevicePolicyManager) context.getSystemService("device_policy");
        this.mConnectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
        this.mConnectivityManager.registerNetworkCallback(REQUEST, this.mNetworkCallback);
        this.mCurrentUserId = ActivityManager.getCurrentUser();
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("SecurityController state:");
        pw.print("  mCurrentVpnNetworkId=");
        pw.println(this.mCurrentVpnNetworkId);
        pw.print("  mVpnConfig=");
        pw.println(this.mVpnConfig);
        pw.print("  mVpnName=");
        pw.println(this.mVpnName);
    }

    @Override
    public boolean hasDeviceOwner() {
        return !TextUtils.isEmpty(this.mDevicePolicyManager.getDeviceOwner());
    }

    @Override
    public boolean hasProfileOwner() {
        return !TextUtils.isEmpty(this.mDevicePolicyManager.getProfileOwnerNameAsUser(this.mCurrentUserId));
    }

    @Override
    public String getDeviceOwnerName() {
        return this.mDevicePolicyManager.getDeviceOwnerName();
    }

    @Override
    public String getProfileOwnerName() {
        return this.mDevicePolicyManager.getProfileOwnerNameAsUser(this.mCurrentUserId);
    }

    @Override
    public boolean isVpnEnabled() {
        return this.mCurrentVpnNetworkId != -1;
    }

    @Override
    public boolean isLegacyVpn() {
        return this.mVpnConfig.legacy;
    }

    @Override
    public String getVpnApp() {
        return this.mVpnName;
    }

    @Override
    public String getLegacyVpnName() {
        return this.mVpnConfig.session;
    }

    @Override
    public void disconnectFromVpn() {
        try {
            if (isLegacyVpn()) {
                this.mConnectivityService.prepareVpn("[Legacy VPN]", "[Legacy VPN]");
            } else {
                this.mConnectivityService.setVpnPackageAuthorization(false);
                this.mConnectivityService.prepareVpn(this.mVpnConfig.user, "[Legacy VPN]");
            }
        } catch (Exception e) {
            Log.e("SecurityController", "Unable to disconnect from VPN", e);
        }
    }

    @Override
    public void removeCallback(SecurityController.SecurityControllerCallback callback) {
        if (callback != null) {
            if (DEBUG) {
                Log.d("SecurityController", "removeCallback " + callback);
            }
            this.mCallbacks.remove(callback);
        }
    }

    @Override
    public void addCallback(SecurityController.SecurityControllerCallback callback) {
        if (callback != null && !this.mCallbacks.contains(callback)) {
            if (DEBUG) {
                Log.d("SecurityController", "addCallback " + callback);
            }
            this.mCallbacks.add(callback);
        }
    }

    @Override
    public void onUserSwitched(int newUserId) {
        this.mCurrentUserId = newUserId;
        fireCallbacks();
    }

    private void setCurrentNetid(int netId) {
        if (netId != this.mCurrentVpnNetworkId) {
            this.mCurrentVpnNetworkId = netId;
            updateState();
            fireCallbacks();
        }
    }

    private void fireCallbacks() {
        for (SecurityController.SecurityControllerCallback callback : this.mCallbacks) {
            callback.onStateChanged();
        }
    }

    private void updateState() {
        try {
            this.mVpnConfig = this.mConnectivityService.getVpnConfig();
            if (this.mVpnConfig != null && !this.mVpnConfig.legacy) {
                this.mVpnName = VpnConfig.getVpnLabel(this.mContext, this.mVpnConfig.user).toString();
            }
        } catch (PackageManager.NameNotFoundException | RemoteException e) {
            Log.w("SecurityController", "Unable to get current VPN", e);
        }
    }
}
