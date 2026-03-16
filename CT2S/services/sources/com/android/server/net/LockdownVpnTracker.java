package com.android.server.net;

import android.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.NetworkInfo;
import android.os.INetworkManagementService;
import android.os.RemoteException;
import android.security.KeyStore;
import android.system.Os;
import android.text.TextUtils;
import android.util.Slog;
import com.android.internal.net.VpnConfig;
import com.android.internal.net.VpnProfile;
import com.android.internal.util.Preconditions;
import com.android.server.ConnectivityService;
import com.android.server.EventLogTags;
import com.android.server.SystemService;
import com.android.server.connectivity.Vpn;
import java.util.List;

public class LockdownVpnTracker {
    private static final String ACTION_LOCKDOWN_RESET = "com.android.server.action.LOCKDOWN_RESET";
    private static final String ACTION_VPN_SETTINGS = "android.net.vpn.SETTINGS";
    private static final String EXTRA_PICK_LOCKDOWN = "android.net.vpn.PICK_LOCKDOWN";
    private static final int MAX_ERROR_COUNT = 4;
    private static final int ROOT_UID = 0;
    private static final String TAG = "LockdownVpnTracker";
    private String mAcceptedEgressIface;
    private String mAcceptedIface;
    private List<LinkAddress> mAcceptedSourceAddr;
    private final PendingIntent mConfigIntent;
    private final ConnectivityService mConnService;
    private final Context mContext;
    private int mErrorCount;
    private final INetworkManagementService mNetService;
    private final VpnProfile mProfile;
    private final PendingIntent mResetIntent;
    private final Vpn mVpn;
    private final Object mStateLock = new Object();
    private BroadcastReceiver mResetReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            LockdownVpnTracker.this.reset();
        }
    };

    public static boolean isEnabled() {
        return KeyStore.getInstance().contains("LOCKDOWN_VPN");
    }

    public LockdownVpnTracker(Context context, INetworkManagementService netService, ConnectivityService connService, Vpn vpn, VpnProfile profile) {
        this.mContext = (Context) Preconditions.checkNotNull(context);
        this.mNetService = (INetworkManagementService) Preconditions.checkNotNull(netService);
        this.mConnService = (ConnectivityService) Preconditions.checkNotNull(connService);
        this.mVpn = (Vpn) Preconditions.checkNotNull(vpn);
        this.mProfile = (VpnProfile) Preconditions.checkNotNull(profile);
        Intent configIntent = new Intent(ACTION_VPN_SETTINGS);
        configIntent.putExtra(EXTRA_PICK_LOCKDOWN, true);
        this.mConfigIntent = PendingIntent.getActivity(this.mContext, 0, configIntent, 0);
        Intent resetIntent = new Intent(ACTION_LOCKDOWN_RESET);
        resetIntent.addFlags(1073741824);
        this.mResetIntent = PendingIntent.getBroadcast(this.mContext, 0, resetIntent, 0);
    }

    private void handleStateChangedLocked() {
        NetworkInfo egressInfo = this.mConnService.getActiveNetworkInfoUnfiltered();
        LinkProperties egressProp = this.mConnService.getActiveLinkProperties();
        NetworkInfo vpnInfo = this.mVpn.getNetworkInfo();
        VpnConfig vpnConfig = this.mVpn.getLegacyVpnConfig();
        boolean egressDisconnected = egressInfo == null || NetworkInfo.State.DISCONNECTED.equals(egressInfo.getState());
        boolean egressChanged = egressProp == null || !TextUtils.equals(this.mAcceptedEgressIface, egressProp.getInterfaceName());
        String egressTypeName = egressInfo == null ? null : ConnectivityManager.getNetworkTypeName(egressInfo.getType());
        String egressIface = egressProp == null ? null : egressProp.getInterfaceName();
        Slog.d(TAG, "handleStateChanged: egress=" + egressTypeName + " " + this.mAcceptedEgressIface + "->" + egressIface);
        if (egressDisconnected || egressChanged) {
            clearSourceRulesLocked();
            this.mAcceptedEgressIface = null;
            this.mVpn.stopLegacyVpnPrivileged();
        }
        if (egressDisconnected) {
            hideNotification();
            return;
        }
        int egressType = egressInfo.getType();
        if (vpnInfo.getDetailedState() == NetworkInfo.DetailedState.FAILED) {
            EventLogTags.writeLockdownVpnError(egressType);
        }
        if (this.mErrorCount > 4) {
            showNotification(R.string.keyguard_password_enter_pin_prompt, R.drawable.perm_group_read_media_aural);
            return;
        }
        if (egressInfo.isConnected() && !vpnInfo.isConnectedOrConnecting()) {
            if (this.mProfile.isValidLockdownProfile()) {
                Slog.d(TAG, "Active network connected; starting VPN");
                EventLogTags.writeLockdownVpnConnecting(egressType);
                showNotification(R.string.keyguard_password_enter_pin_code, R.drawable.perm_group_read_media_aural);
                this.mAcceptedEgressIface = egressProp.getInterfaceName();
                try {
                    this.mVpn.startLegacyVpnPrivileged(this.mProfile, KeyStore.getInstance(), egressProp);
                    return;
                } catch (IllegalStateException e) {
                    this.mAcceptedEgressIface = null;
                    Slog.e(TAG, "Failed to start VPN", e);
                    showNotification(R.string.keyguard_password_enter_pin_prompt, R.drawable.perm_group_read_media_aural);
                    return;
                }
            }
            Slog.e(TAG, "Invalid VPN profile; requires IP-based server and DNS");
            showNotification(R.string.keyguard_password_enter_pin_prompt, R.drawable.perm_group_read_media_aural);
            return;
        }
        if (vpnInfo.isConnected() && vpnConfig != null) {
            String iface = vpnConfig.interfaze;
            List<LinkAddress> sourceAddrs = vpnConfig.addresses;
            if (!TextUtils.equals(iface, this.mAcceptedIface) || !sourceAddrs.equals(this.mAcceptedSourceAddr)) {
                Slog.d(TAG, "VPN connected using iface=" + iface + ", sourceAddr=" + sourceAddrs.toString());
                EventLogTags.writeLockdownVpnConnected(egressType);
                showNotification(R.string.keyguard_password_enter_pin_password_code, R.drawable.perm_group_phone_calls);
                try {
                    clearSourceRulesLocked();
                    this.mNetService.setFirewallInterfaceRule(iface, true);
                    for (LinkAddress addr : sourceAddrs) {
                        setFirewallEgressSourceRule(addr, true);
                    }
                    this.mNetService.setFirewallUidRule(0, true);
                    this.mNetService.setFirewallUidRule(Os.getuid(), true);
                    this.mErrorCount = 0;
                    this.mAcceptedIface = iface;
                    this.mAcceptedSourceAddr = sourceAddrs;
                    this.mConnService.sendConnectedBroadcast(augmentNetworkInfo(egressInfo));
                } catch (RemoteException e2) {
                    throw new RuntimeException("Problem setting firewall rules", e2);
                }
            }
        }
    }

    public void init() {
        synchronized (this.mStateLock) {
            initLocked();
        }
    }

    private void initLocked() {
        Slog.d(TAG, "initLocked()");
        this.mVpn.setEnableTeardown(false);
        IntentFilter resetFilter = new IntentFilter(ACTION_LOCKDOWN_RESET);
        this.mContext.registerReceiver(this.mResetReceiver, resetFilter, "android.permission.CONNECTIVITY_INTERNAL", null);
        try {
            this.mNetService.setFirewallEgressDestRule(this.mProfile.server, SystemService.PHASE_SYSTEM_SERVICES_READY, true);
            this.mNetService.setFirewallEgressDestRule(this.mProfile.server, 4500, true);
            this.mNetService.setFirewallEgressDestRule(this.mProfile.server, 1701, true);
            synchronized (this.mStateLock) {
                handleStateChangedLocked();
            }
        } catch (RemoteException e) {
            throw new RuntimeException("Problem setting firewall rules", e);
        }
    }

    public void shutdown() {
        synchronized (this.mStateLock) {
            shutdownLocked();
        }
    }

    private void shutdownLocked() {
        Slog.d(TAG, "shutdownLocked()");
        this.mAcceptedEgressIface = null;
        this.mErrorCount = 0;
        this.mVpn.stopLegacyVpnPrivileged();
        try {
            this.mNetService.setFirewallEgressDestRule(this.mProfile.server, SystemService.PHASE_SYSTEM_SERVICES_READY, false);
            this.mNetService.setFirewallEgressDestRule(this.mProfile.server, 4500, false);
            this.mNetService.setFirewallEgressDestRule(this.mProfile.server, 1701, false);
            clearSourceRulesLocked();
            hideNotification();
            this.mContext.unregisterReceiver(this.mResetReceiver);
            this.mVpn.setEnableTeardown(true);
        } catch (RemoteException e) {
            throw new RuntimeException("Problem setting firewall rules", e);
        }
    }

    public void reset() {
        Slog.d(TAG, "reset()");
        synchronized (this.mStateLock) {
            shutdownLocked();
            initLocked();
            handleStateChangedLocked();
        }
    }

    private void clearSourceRulesLocked() {
        try {
            if (this.mAcceptedIface != null) {
                this.mNetService.setFirewallInterfaceRule(this.mAcceptedIface, false);
                this.mAcceptedIface = null;
            }
            if (this.mAcceptedSourceAddr != null) {
                for (LinkAddress addr : this.mAcceptedSourceAddr) {
                    setFirewallEgressSourceRule(addr, false);
                }
                this.mNetService.setFirewallUidRule(0, false);
                this.mNetService.setFirewallUidRule(Os.getuid(), false);
                this.mAcceptedSourceAddr = null;
            }
        } catch (RemoteException e) {
            throw new RuntimeException("Problem setting firewall rules", e);
        }
    }

    private void setFirewallEgressSourceRule(LinkAddress address, boolean allow) throws RemoteException {
        String addrString = address.getAddress().getHostAddress();
        this.mNetService.setFirewallEgressSourceRule(addrString, allow);
    }

    public void onNetworkInfoChanged() {
        synchronized (this.mStateLock) {
            handleStateChangedLocked();
        }
    }

    public void onVpnStateChanged(NetworkInfo info) {
        if (info.getDetailedState() == NetworkInfo.DetailedState.FAILED) {
            this.mErrorCount++;
        }
        synchronized (this.mStateLock) {
            handleStateChangedLocked();
        }
    }

    public NetworkInfo augmentNetworkInfo(NetworkInfo info) {
        if (info.isConnected()) {
            NetworkInfo vpnInfo = this.mVpn.getNetworkInfo();
            NetworkInfo info2 = new NetworkInfo(info);
            info2.setDetailedState(vpnInfo.getDetailedState(), vpnInfo.getReason(), null);
            return info2;
        }
        return info;
    }

    private void showNotification(int titleRes, int iconRes) {
        Notification.Builder builder = new Notification.Builder(this.mContext).setWhen(0L).setSmallIcon(iconRes).setContentTitle(this.mContext.getString(titleRes)).setContentText(this.mContext.getString(R.string.keyguard_password_enter_puk_code)).setContentIntent(this.mConfigIntent).setPriority(-1).setOngoing(true).addAction(R.drawable.ic_collapse_bundle, this.mContext.getString(R.string.keyguard_password_wrong_pin_code), this.mResetIntent).setColor(this.mContext.getResources().getColor(R.color.system_accent3_600));
        NotificationManager.from(this.mContext).notify(TAG, 0, builder.build());
    }

    private void hideNotification() {
        NotificationManager.from(this.mContext).cancel(TAG, 0);
    }
}
