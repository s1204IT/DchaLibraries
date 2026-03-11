package com.android.settings.network;

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
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.net.LegacyVpnInfo;
import com.android.internal.net.VpnConfig;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.utils.ThreadUtils;

public class VpnPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin, LifecycleObserver, OnPause, OnResume {
    private static final NetworkRequest REQUEST = new NetworkRequest.Builder().removeCapability(15).removeCapability(13).removeCapability(14).build();
    private final ConnectivityManager mConnectivityManager;
    private final IConnectivityManager mConnectivityManagerService;
    private final ConnectivityManager.NetworkCallback mNetworkCallback;
    private Preference mPreference;
    private final String mToggleable;
    private final UserManager mUserManager;

    public VpnPreferenceController(Context context) {
        super(context);
        this.mNetworkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                VpnPreferenceController.this.updateSummary();
            }

            @Override
            public void onLost(Network network) {
                VpnPreferenceController.this.updateSummary();
            }
        };
        this.mToggleable = Settings.Global.getString(context.getContentResolver(), "airplane_mode_toggleable_radios");
        this.mUserManager = (UserManager) context.getSystemService("user");
        this.mConnectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
        this.mConnectivityManagerService = IConnectivityManager.Stub.asInterface(ServiceManager.getService("connectivity"));
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mPreference = preferenceScreen.findPreference("vpn_settings");
        if ((this.mToggleable == null || !this.mToggleable.contains("wifi")) && this.mPreference != null) {
            this.mPreference.setDependency("airplane_mode");
        }
    }

    @Override
    public boolean isAvailable() {
        return !RestrictedLockUtils.hasBaseUserRestriction(this.mContext, "no_config_vpn", UserHandle.myUserId());
    }

    @Override
    public String getPreferenceKey() {
        return "vpn_settings";
    }

    @Override
    public void onPause() {
        if (isAvailable()) {
            this.mConnectivityManager.unregisterNetworkCallback(this.mNetworkCallback);
        }
    }

    @Override
    public void onResume() {
        if (isAvailable()) {
            this.mConnectivityManager.registerNetworkCallback(REQUEST, this.mNetworkCallback);
        }
    }

    void updateSummary() {
        int i;
        final String nameForVpnConfig;
        LegacyVpnInfo legacyVpnInfo;
        if (this.mPreference == null) {
            return;
        }
        SparseArray sparseArray = new SparseArray();
        try {
            for (UserInfo userInfo : this.mUserManager.getUsers()) {
                VpnConfig vpnConfig = this.mConnectivityManagerService.getVpnConfig(userInfo.id);
                if (vpnConfig != null && (!vpnConfig.legacy || ((legacyVpnInfo = this.mConnectivityManagerService.getLegacyVpnInfo(userInfo.id)) != null && legacyVpnInfo.state == 3))) {
                    sparseArray.put(userInfo.id, vpnConfig);
                }
            }
            UserInfo userInfo2 = this.mUserManager.getUserInfo(UserHandle.myUserId());
            if (userInfo2.isRestricted()) {
                i = userInfo2.restrictedProfileParentId;
            } else {
                i = userInfo2.id;
            }
            VpnConfig vpnConfig2 = (VpnConfig) sparseArray.get(i);
            if (vpnConfig2 == null) {
                nameForVpnConfig = this.mContext.getString(R.string.vpn_disconnected_summary);
            } else {
                nameForVpnConfig = getNameForVpnConfig(vpnConfig2, UserHandle.of(i));
            }
            ThreadUtils.postOnMainThread(new Runnable() {
                @Override
                public final void run() {
                    this.f$0.mPreference.setSummary(nameForVpnConfig);
                }
            });
        } catch (RemoteException e) {
            Log.e("VpnPreferenceController", "Unable to list active VPNs", e);
        }
    }

    String getNameForVpnConfig(VpnConfig vpnConfig, UserHandle userHandle) {
        if (vpnConfig.legacy) {
            return this.mContext.getString(R.string.wifi_display_status_connected);
        }
        String str = vpnConfig.user;
        try {
            return VpnConfig.getVpnLabel(this.mContext.createPackageContextAsUser(this.mContext.getPackageName(), 0, userHandle), str).toString();
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("VpnPreferenceController", "Package " + str + " is not present", e);
            return null;
        }
    }
}
