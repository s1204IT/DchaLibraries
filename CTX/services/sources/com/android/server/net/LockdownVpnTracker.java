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
import android.os.BenesseExtension;
import android.os.INetworkManagementService;
import android.security.KeyStore;
import android.text.TextUtils;
import android.util.Slog;
import com.android.internal.net.VpnConfig;
import com.android.internal.net.VpnProfile;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.util.Preconditions;
import com.android.server.ConnectivityService;
import com.android.server.EventLogTags;
import com.android.server.connectivity.Vpn;
import java.util.List;

public class LockdownVpnTracker {
    private static final String ACTION_LOCKDOWN_RESET = "com.android.server.action.LOCKDOWN_RESET";
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

    public LockdownVpnTracker(Context context, INetworkManagementService iNetworkManagementService, ConnectivityService connectivityService, Vpn vpn, VpnProfile vpnProfile) {
        this.mContext = (Context) Preconditions.checkNotNull(context);
        this.mNetService = (INetworkManagementService) Preconditions.checkNotNull(iNetworkManagementService);
        this.mConnService = (ConnectivityService) Preconditions.checkNotNull(connectivityService);
        this.mVpn = (Vpn) Preconditions.checkNotNull(vpn);
        this.mProfile = (VpnProfile) Preconditions.checkNotNull(vpnProfile);
        this.mConfigIntent = BenesseExtension.getDchaState() == 0 ? PendingIntent.getActivity(this.mContext, 0, new Intent("android.settings.VPN_SETTINGS"), 0) : null;
        Intent intent = new Intent(ACTION_LOCKDOWN_RESET);
        intent.addFlags(1073741824);
        this.mResetIntent = PendingIntent.getBroadcast(this.mContext, 0, intent, 0);
    }

    private void handleStateChangedLocked() {
        String networkTypeName;
        String interfaceName;
        NetworkInfo activeNetworkInfoUnfiltered = this.mConnService.getActiveNetworkInfoUnfiltered();
        LinkProperties activeLinkProperties = this.mConnService.getActiveLinkProperties();
        NetworkInfo networkInfo = this.mVpn.getNetworkInfo();
        VpnConfig legacyVpnConfig = this.mVpn.getLegacyVpnConfig();
        boolean z = true;
        boolean z2 = activeNetworkInfoUnfiltered == null || NetworkInfo.State.DISCONNECTED.equals(activeNetworkInfoUnfiltered.getState());
        if (activeLinkProperties != null && TextUtils.equals(this.mAcceptedEgressIface, activeLinkProperties.getInterfaceName())) {
            z = false;
        }
        if (activeNetworkInfoUnfiltered != null) {
            networkTypeName = ConnectivityManager.getNetworkTypeName(activeNetworkInfoUnfiltered.getType());
        } else {
            networkTypeName = null;
        }
        if (activeLinkProperties != null) {
            interfaceName = activeLinkProperties.getInterfaceName();
        } else {
            interfaceName = null;
        }
        Slog.d(TAG, "handleStateChanged: egress=" + networkTypeName + " " + this.mAcceptedEgressIface + "->" + interfaceName);
        if (z2 || z) {
            this.mAcceptedEgressIface = null;
            this.mVpn.stopLegacyVpnPrivileged();
        }
        if (z2) {
            hideNotification();
            return;
        }
        int type = activeNetworkInfoUnfiltered.getType();
        if (networkInfo.getDetailedState() == NetworkInfo.DetailedState.FAILED) {
            EventLogTags.writeLockdownVpnError(type);
        }
        if (this.mErrorCount > 4) {
            showNotification(R.string.negative_duration, R.drawable.popup_bottom_bright);
            return;
        }
        if (activeNetworkInfoUnfiltered.isConnected() && !networkInfo.isConnectedOrConnecting()) {
            if (this.mProfile.isValidLockdownProfile()) {
                Slog.d(TAG, "Active network connected; starting VPN");
                EventLogTags.writeLockdownVpnConnecting(type);
                showNotification(R.string.needPuk, R.drawable.popup_bottom_bright);
                this.mAcceptedEgressIface = activeLinkProperties.getInterfaceName();
                try {
                    this.mVpn.startLegacyVpnPrivileged(this.mProfile, KeyStore.getInstance(), activeLinkProperties);
                    return;
                } catch (IllegalStateException e) {
                    this.mAcceptedEgressIface = null;
                    Slog.e(TAG, "Failed to start VPN", e);
                    showNotification(R.string.negative_duration, R.drawable.popup_bottom_bright);
                    return;
                }
            }
            Slog.e(TAG, "Invalid VPN profile; requires IP-based server and DNS");
            showNotification(R.string.negative_duration, R.drawable.popup_bottom_bright);
            return;
        }
        if (networkInfo.isConnected() && legacyVpnConfig != null) {
            String str = legacyVpnConfig.interfaze;
            List list = legacyVpnConfig.addresses;
            if (TextUtils.equals(str, this.mAcceptedIface) && list.equals(this.mAcceptedSourceAddr)) {
                return;
            }
            Slog.d(TAG, "VPN connected using iface=" + str + ", sourceAddr=" + list.toString());
            EventLogTags.writeLockdownVpnConnected(type);
            showNotification(R.string.nas_upgrade_notification_title, R.drawable.popup_background_mtrl_mult);
            NetworkInfo networkInfo2 = new NetworkInfo(activeNetworkInfoUnfiltered);
            augmentNetworkInfo(networkInfo2);
            this.mConnService.sendConnectedBroadcast(networkInfo2);
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
        this.mVpn.setLockdown(true);
        this.mContext.registerReceiver(this.mResetReceiver, new IntentFilter(ACTION_LOCKDOWN_RESET), "android.permission.CONNECTIVITY_INTERNAL", null);
        handleStateChangedLocked();
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
        this.mVpn.setLockdown(false);
        hideNotification();
        this.mContext.unregisterReceiver(this.mResetReceiver);
        this.mVpn.setEnableTeardown(true);
    }

    public void reset() {
        Slog.d(TAG, "reset()");
        synchronized (this.mStateLock) {
            shutdownLocked();
            initLocked();
            handleStateChangedLocked();
        }
    }

    public void onNetworkInfoChanged() {
        synchronized (this.mStateLock) {
            handleStateChangedLocked();
        }
    }

    public void onVpnStateChanged(NetworkInfo networkInfo) {
        if (networkInfo.getDetailedState() == NetworkInfo.DetailedState.FAILED) {
            this.mErrorCount++;
        }
        synchronized (this.mStateLock) {
            handleStateChangedLocked();
        }
    }

    public void augmentNetworkInfo(NetworkInfo networkInfo) {
        if (networkInfo.isConnected()) {
            NetworkInfo networkInfo2 = this.mVpn.getNetworkInfo();
            networkInfo.setDetailedState(networkInfo2.getDetailedState(), networkInfo2.getReason(), null);
        }
    }

    private void showNotification(int i, int i2) {
        NotificationManager.from(this.mContext).notify(null, 20, new Notification.Builder(this.mContext, SystemNotificationChannels.VPN).setWhen(0L).setSmallIcon(i2).setContentTitle(this.mContext.getString(i)).setContentText(this.mContext.getString(R.string.nas_upgrade_notification_learn_more_content)).setContentIntent(this.mConfigIntent).setOngoing(true).addAction(R.drawable.ic_lockscreen_silent_activated, this.mContext.getString(R.string.kg_sim_unlock_progress_dialog_message), this.mResetIntent).setColor(this.mContext.getColor(R.color.car_colorPrimary)).build());
    }

    private void hideNotification() {
        NotificationManager.from(this.mContext).cancel(null, 20);
    }
}
