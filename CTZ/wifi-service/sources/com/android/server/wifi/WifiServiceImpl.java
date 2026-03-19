package com.android.server.wifi;

import android.R;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.database.ContentObserver;
import android.net.DhcpInfo;
import android.net.DhcpResults;
import android.net.Network;
import android.net.NetworkUtils;
import android.net.Uri;
import android.net.wifi.ISoftApCallback;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiActivityEnergyInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiSsid;
import android.net.wifi.hotspot2.IProvisioningCallback;
import android.net.wifi.hotspot2.OsuProvider;
import android.net.wifi.hotspot2.PasspointConfiguration;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.WorkSource;
import android.provider.Settings;
import android.util.Log;
import android.util.MutableInt;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.PowerProfile;
import com.android.internal.util.AsyncChannel;
import com.android.server.wifi.LocalOnlyHotspotRequestInfo;
import com.android.server.wifi.hotspot2.PasspointProvider;
import com.android.server.wifi.util.GeneralUtil;
import com.android.server.wifi.util.WifiHandler;
import com.android.server.wifi.util.WifiPermissionsUtil;
import com.mediatek.cta.CtaManagerFactory;
import com.mediatek.server.wifi.MtkEapSimUtility;
import com.mediatek.server.wifi.MtkWifiServiceImpl;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class WifiServiceImpl extends MtkWifiServiceImpl {
    private static final int BACKGROUND_IMPORTANCE_CUTOFF = 125;
    private static final long DEFAULT_SCAN_BACKGROUND_THROTTLE_INTERVAL_MS = 1800000;
    private static final int NUM_SOFT_AP_CALLBACKS_WARN_LIMIT = 10;
    private static final int NUM_SOFT_AP_CALLBACKS_WTF_LIMIT = 20;
    private static final int RUN_WITH_SCISSORS_TIMEOUT_MILLIS = 4000;
    private static final String TAG = "WifiService";
    private static final boolean VDBG = false;
    private final ActivityManager mActivityManager;
    private final AppOpsManager mAppOps;
    private ClientHandler mClientHandler;
    private final Clock mClock;
    private final Context mContext;
    private final WifiCountryCode mCountryCode;
    private final FrameworkFacade mFacade;
    private final FrameworkFacade mFrameworkFacade;

    @GuardedBy("mLocalOnlyHotspotRequests")
    private final ConcurrentHashMap<String, Integer> mIfaceIpModes;
    boolean mInIdleMode;

    @GuardedBy("mLocalOnlyHotspotRequests")
    private WifiConfiguration mLocalOnlyHotspotConfig;

    @GuardedBy("mLocalOnlyHotspotRequests")
    private final HashMap<Integer, LocalOnlyHotspotRequestInfo> mLocalOnlyHotspotRequests;
    private WifiLog mLog;
    private final boolean mPermissionReviewRequired;
    private final PowerManager mPowerManager;
    PowerProfile mPowerProfile;
    private final BroadcastReceiver mReceiver;
    private final HashMap<Integer, ISoftApCallback> mRegisteredSoftApCallbacks;
    boolean mScanPending;
    final ScanRequestProxy mScanRequestProxy;
    final WifiSettingsStore mSettingsStore;
    private int mSoftApNumClients;
    private int mSoftApState;
    private WifiTrafficPoller mTrafficPoller;
    private final UserManager mUserManager;
    private boolean mVerboseLoggingEnabled;
    private WifiApConfigStore mWifiApConfigStore;
    private int mWifiApState;
    private final WifiBackupRestore mWifiBackupRestore;
    private WifiController mWifiController;
    private final WifiInjector mWifiInjector;
    private final WifiLockManager mWifiLockManager;
    private final WifiMetrics mWifiMetrics;
    private final WifiMulticastLockManager mWifiMulticastLockManager;
    private WifiPermissionsUtil mWifiPermissionsUtil;
    final WifiStateMachine mWifiStateMachine;
    private AsyncChannel mWifiStateMachineChannel;
    WifiStateMachineHandler mWifiStateMachineHandler;
    final WifiStateMachinePrime mWifiStateMachinePrime;
    private int scanRequestCounter;

    public final class LocalOnlyRequestorCallback implements LocalOnlyHotspotRequestInfo.RequestingApplicationDeathCallback {
        public LocalOnlyRequestorCallback() {
        }

        @Override
        public void onLocalOnlyHotspotRequestorDeath(LocalOnlyHotspotRequestInfo localOnlyHotspotRequestInfo) {
            WifiServiceImpl.this.unregisterCallingAppAndStopLocalOnlyHotspot(localOnlyHotspotRequestInfo);
        }
    }

    private class ClientHandler extends WifiHandler {
        ClientHandler(String str, Looper looper) {
            super(str, looper);
        }

        @Override
        public void handleMessage(Message message) {
            super.handleMessage(message);
            switch (message.what) {
                case 69632:
                    if (message.arg1 == 0) {
                        Slog.d(WifiServiceImpl.TAG, "New client listening to asynchronous messages");
                        WifiServiceImpl.this.mTrafficPoller.addClient(message.replyTo);
                    } else {
                        Slog.e(WifiServiceImpl.TAG, "Client connection failure, error=" + message.arg1);
                    }
                    break;
                case 69633:
                    WifiServiceImpl.this.mFrameworkFacade.makeWifiAsyncChannel(WifiServiceImpl.TAG).connect(WifiServiceImpl.this.mContext, this, message.replyTo);
                    break;
                case 69636:
                    if (message.arg1 == 2) {
                        Slog.w(WifiServiceImpl.TAG, "Send failed, client connection lost");
                    } else {
                        Slog.w(WifiServiceImpl.TAG, "Client connection lost with reason: " + message.arg1);
                    }
                    WifiServiceImpl.this.mTrafficPoller.removeClient(message.replyTo);
                    break;
                case 151553:
                    if (checkChangePermissionAndReplyIfNotAuthorized(message, 151554)) {
                        WifiConfiguration wifiConfiguration = (WifiConfiguration) message.obj;
                        int i = message.arg1;
                        Slog.d(WifiServiceImpl.TAG, "CONNECT  nid=" + Integer.toString(i) + " config=" + wifiConfiguration + " uid=" + message.sendingUid + " name=" + WifiServiceImpl.this.mContext.getPackageManager().getNameForUid(message.sendingUid));
                        if (wifiConfiguration != null) {
                            WifiServiceImpl.this.mWifiStateMachine.sendMessage(Message.obtain(message));
                        } else if (wifiConfiguration == null && i != -1) {
                            WifiServiceImpl.this.mWifiStateMachine.sendMessage(Message.obtain(message));
                        } else {
                            Slog.e(WifiServiceImpl.TAG, "ClientHandler.handleMessage ignoring invalid msg=" + message);
                            replyFailed(message, 151554, 8);
                        }
                    }
                    break;
                case 151556:
                    if (checkChangePermissionAndReplyIfNotAuthorized(message, 151557)) {
                        WifiServiceImpl.this.mWifiStateMachine.sendMessage(Message.obtain(message));
                    }
                    break;
                case 151559:
                    if (checkChangePermissionAndReplyIfNotAuthorized(message, 151560)) {
                        WifiConfiguration wifiConfiguration2 = (WifiConfiguration) message.obj;
                        Slog.d(WifiServiceImpl.TAG, "SAVE nid=" + Integer.toString(message.arg1) + " config=" + wifiConfiguration2 + " uid=" + message.sendingUid + " name=" + WifiServiceImpl.this.mContext.getPackageManager().getNameForUid(message.sendingUid));
                        if (wifiConfiguration2 != null) {
                            WifiServiceImpl.this.mWifiStateMachine.sendMessage(Message.obtain(message));
                        } else {
                            Slog.e(WifiServiceImpl.TAG, "ClientHandler.handleMessage ignoring invalid msg=" + message);
                            replyFailed(message, 151560, 8);
                        }
                    }
                    break;
                case 151562:
                    if (checkChangePermissionAndReplyIfNotAuthorized(message, 151564)) {
                        replyFailed(message, 151564, 0);
                    }
                    break;
                case 151566:
                    if (checkChangePermissionAndReplyIfNotAuthorized(message, 151567)) {
                        replyFailed(message, 151567, 0);
                    }
                    break;
                case 151569:
                    if (checkChangePermissionAndReplyIfNotAuthorized(message, 151570)) {
                        WifiServiceImpl.this.mWifiStateMachine.sendMessage(Message.obtain(message));
                    }
                    break;
                case 151572:
                    if (checkChangePermissionAndReplyIfNotAuthorized(message, 151574)) {
                        WifiServiceImpl.this.mWifiStateMachine.sendMessage(Message.obtain(message));
                    }
                    break;
                case 151612:
                    WifiServiceImpl.this.mWifiStateMachine.sendMessage(Message.obtain(message));
                    break;
                default:
                    Slog.d(WifiServiceImpl.TAG, "ClientHandler.handleMessage ignoring msg=" + message);
                    break;
            }
        }

        private boolean checkChangePermissionAndReplyIfNotAuthorized(Message message, int i) {
            if (!WifiServiceImpl.this.mWifiPermissionsUtil.checkChangePermission(message.sendingUid)) {
                Slog.e(WifiServiceImpl.TAG, "ClientHandler.handleMessage ignoring unauthorized msg=" + message);
                replyFailed(message, i, 9);
                return false;
            }
            return true;
        }

        private void replyFailed(Message message, int i, int i2) {
            if (message.replyTo == null) {
                return;
            }
            Message messageObtain = Message.obtain();
            messageObtain.what = i;
            messageObtain.arg1 = i2;
            try {
                message.replyTo.send(messageObtain);
            } catch (RemoteException e) {
            }
        }
    }

    private class WifiStateMachineHandler extends WifiHandler {
        private AsyncChannel mWsmChannel;

        WifiStateMachineHandler(String str, Looper looper, AsyncChannel asyncChannel) {
            super(str, looper);
            this.mWsmChannel = asyncChannel;
            this.mWsmChannel.connect(WifiServiceImpl.this.mContext, this, WifiServiceImpl.this.mWifiStateMachine.getHandler());
        }

        @Override
        public void handleMessage(Message message) {
            super.handleMessage(message);
            int i = message.what;
            if (i == 69632) {
                if (message.arg1 == 0) {
                    WifiServiceImpl.this.mWifiStateMachineChannel = this.mWsmChannel;
                    return;
                }
                Slog.e(WifiServiceImpl.TAG, "WifiStateMachine connection failure, error=" + message.arg1);
                WifiServiceImpl.this.mWifiStateMachineChannel = null;
                return;
            }
            if (i == 69636) {
                Slog.e(WifiServiceImpl.TAG, "WifiStateMachine channel lost, msg.arg1 =" + message.arg1);
                WifiServiceImpl.this.mWifiStateMachineChannel = null;
                this.mWsmChannel.connect(WifiServiceImpl.this.mContext, this, WifiServiceImpl.this.mWifiStateMachine.getHandler());
                return;
            }
            Slog.d(WifiServiceImpl.TAG, "WifiStateMachineHandler.handleMessage ignoring msg=" + message);
        }
    }

    public WifiServiceImpl(Context context, WifiInjector wifiInjector, AsyncChannel asyncChannel) {
        super(context, wifiInjector, asyncChannel);
        this.scanRequestCounter = 0;
        this.mVerboseLoggingEnabled = false;
        this.mLocalOnlyHotspotConfig = null;
        this.mWifiApState = 11;
        this.mSoftApState = 11;
        this.mSoftApNumClients = 0;
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if (action.equals("android.intent.action.USER_PRESENT")) {
                    WifiServiceImpl.this.mWifiController.sendMessage(155660);
                    return;
                }
                if (action.equals("android.intent.action.USER_REMOVED")) {
                    WifiServiceImpl.this.mWifiStateMachine.removeUserConfigs(intent.getIntExtra("android.intent.extra.user_handle", 0));
                    return;
                }
                if (action.equals("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED")) {
                    WifiServiceImpl.this.mWifiStateMachine.sendBluetoothAdapterStateChange(intent.getIntExtra("android.bluetooth.adapter.extra.CONNECTION_STATE", 0));
                    return;
                }
                if (action.equals("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED")) {
                    WifiServiceImpl.this.mWifiController.sendMessage(155649, intent.getBooleanExtra("phoneinECMState", false) ? 1 : 0, 0);
                } else if (action.equals("android.intent.action.EMERGENCY_CALL_STATE_CHANGED")) {
                    WifiServiceImpl.this.mWifiController.sendMessage(155662, intent.getBooleanExtra("phoneInEmergencyCall", false) ? 1 : 0, 0);
                } else if (action.equals("android.os.action.DEVICE_IDLE_MODE_CHANGED")) {
                    WifiServiceImpl.this.handleIdleModeChanged();
                }
            }
        };
        this.mContext = context;
        this.mWifiInjector = wifiInjector;
        this.mClock = wifiInjector.getClock();
        this.mFacade = this.mWifiInjector.getFrameworkFacade();
        this.mWifiMetrics = this.mWifiInjector.getWifiMetrics();
        this.mTrafficPoller = this.mWifiInjector.getWifiTrafficPoller();
        this.mUserManager = this.mWifiInjector.getUserManager();
        this.mCountryCode = this.mWifiInjector.getWifiCountryCode();
        this.mWifiStateMachine = this.mWifiInjector.getWifiStateMachine();
        this.mWifiStateMachinePrime = this.mWifiInjector.getWifiStateMachinePrime();
        this.mWifiStateMachine.enableRssiPolling(true);
        this.mScanRequestProxy = this.mWifiInjector.getScanRequestProxy();
        this.mSettingsStore = this.mWifiInjector.getWifiSettingsStore();
        this.mPowerManager = (PowerManager) this.mContext.getSystemService(PowerManager.class);
        this.mAppOps = (AppOpsManager) this.mContext.getSystemService("appops");
        this.mActivityManager = (ActivityManager) this.mContext.getSystemService("activity");
        this.mWifiLockManager = this.mWifiInjector.getWifiLockManager();
        this.mWifiMulticastLockManager = this.mWifiInjector.getWifiMulticastLockManager();
        HandlerThread wifiServiceHandlerThread = this.mWifiInjector.getWifiServiceHandlerThread();
        this.mClientHandler = new ClientHandler(TAG, wifiServiceHandlerThread.getLooper());
        this.mWifiStateMachineHandler = new WifiStateMachineHandler(TAG, wifiServiceHandlerThread.getLooper(), asyncChannel);
        this.mWifiController = this.mWifiInjector.getWifiController();
        this.mWifiBackupRestore = this.mWifiInjector.getWifiBackupRestore();
        this.mWifiApConfigStore = this.mWifiInjector.getWifiApConfigStore();
        this.mPermissionReviewRequired = Build.PERMISSIONS_REVIEW_REQUIRED || CtaManagerFactory.getInstance().makeCtaManager().isCtaSupported() || context.getResources().getBoolean(R.^attr-private.magnifierColorOverlay);
        this.mWifiPermissionsUtil = this.mWifiInjector.getWifiPermissionsUtil();
        this.mLog = this.mWifiInjector.makeLog(TAG);
        this.mFrameworkFacade = wifiInjector.getFrameworkFacade();
        this.mIfaceIpModes = new ConcurrentHashMap<>();
        this.mLocalOnlyHotspotRequests = new HashMap<>();
        enableVerboseLoggingInternal(getVerboseLoggingLevel());
        this.mRegisteredSoftApCallbacks = new HashMap<>();
        this.mWifiInjector.getWifiStateMachinePrime().registerSoftApCallback(new SoftApCallbackImpl());
        this.mPowerProfile = this.mWifiInjector.getPowerProfile();
        MtkEapSimUtility.init();
    }

    @VisibleForTesting
    public void setWifiHandlerLogForTest(WifiLog wifiLog) {
        this.mClientHandler.setWifiLog(wifiLog);
    }

    public void checkAndStartWifi() {
        if (this.mFrameworkFacade.inStorageManagerCryptKeeperBounce()) {
            Log.d(TAG, "Device still encrypted. Need to restart SystemServer.  Do not start wifi.");
            return;
        }
        boolean zIsWifiToggleEnabled = this.mSettingsStore.isWifiToggleEnabled();
        StringBuilder sb = new StringBuilder();
        sb.append("WifiService starting up with Wi-Fi ");
        sb.append(zIsWifiToggleEnabled ? "enabled" : "disabled");
        Slog.i(TAG, sb.toString());
        registerForScanModeChange();
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (WifiServiceImpl.this.mSettingsStore.handleAirplaneModeToggled()) {
                    WifiServiceImpl.this.mWifiController.sendMessage(155657);
                }
                if (WifiServiceImpl.this.mSettingsStore.isAirplaneModeOn()) {
                    Log.d(WifiServiceImpl.TAG, "resetting country code because Airplane mode is ON");
                    WifiServiceImpl.this.mCountryCode.airplaneModeEnabled();
                }
            }
        }, new IntentFilter("android.intent.action.AIRPLANE_MODE"));
        this.mContext.registerReceiver(new MtkEapSimUtility.MtkSimBroadcastReceiver(), new IntentFilter("android.intent.action.SIM_STATE_CHANGED"));
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                WifiServiceImpl.this.handleWifiApStateChange(intent.getIntExtra("wifi_state", 11), intent.getIntExtra("previous_wifi_state", 11), intent.getIntExtra("wifi_ap_error_code", -1), intent.getStringExtra("wifi_ap_interface_name"), intent.getIntExtra("wifi_ap_mode", -1));
            }
        }, new IntentFilter("android.net.wifi.WIFI_AP_STATE_CHANGED"));
        registerForBroadcasts();
        this.mInIdleMode = this.mPowerManager.isDeviceIdleMode();
        if (!this.mWifiStateMachine.syncInitialize(this.mWifiStateMachineChannel)) {
            Log.wtf(TAG, "Failed to initialize WifiStateMachine");
        }
        this.mWifiController.start();
        if (zIsWifiToggleEnabled) {
            try {
                setWifiEnabled(this.mContext.getPackageName(), zIsWifiToggleEnabled);
            } catch (RemoteException e) {
            }
        }
    }

    public void handleUserSwitch(int i) {
        this.mWifiStateMachine.handleUserSwitch(i);
    }

    public void handleUserUnlock(int i) {
        this.mWifiStateMachine.handleUserUnlock(i);
    }

    public void handleUserStop(int i) {
        this.mWifiStateMachine.handleUserStop(i);
    }

    public boolean startScan(final String str) {
        if (enforceChangePermission(str) != 0) {
            return false;
        }
        final int callingUid = Binder.getCallingUid();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        this.mLog.info("startScan uid=%").c(callingUid).flush();
        synchronized (this) {
            if (this.mInIdleMode) {
                sendFailedScanBroadcast();
                this.mScanPending = true;
                return false;
            }
            try {
                this.mWifiPermissionsUtil.enforceCanAccessScanResults(str, callingUid);
                final GeneralUtil.Mutable mutable = new GeneralUtil.Mutable();
                if (!this.mWifiInjector.getWifiStateMachineHandler().runWithScissors(new Runnable() {
                    @Override
                    public final void run() {
                        mutable.value = Boolean.valueOf(this.f$0.mScanRequestProxy.startScan(callingUid, str));
                    }
                }, 4000L)) {
                    Log.e(TAG, "Failed to post runnable to start scan");
                    sendFailedScanBroadcast();
                    return false;
                }
                if (((Boolean) mutable.value).booleanValue()) {
                    return true;
                }
                Log.e(TAG, "Failed to start scan");
                return false;
            } catch (SecurityException e) {
                return false;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    private void sendFailedScanBroadcast() {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            Intent intent = new Intent("android.net.wifi.SCAN_RESULTS");
            intent.addFlags(67108864);
            intent.putExtra("resultsUpdated", false);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public String getCurrentNetworkWpsNfcConfigurationToken() {
        enforceConnectivityInternalPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("getCurrentNetworkWpsNfcConfigurationToken uid=%").c(Binder.getCallingUid()).flush();
            return null;
        }
        return null;
    }

    void handleIdleModeChanged() {
        boolean z;
        synchronized (this) {
            boolean zIsDeviceIdleMode = this.mPowerManager.isDeviceIdleMode();
            z = false;
            if (this.mInIdleMode != zIsDeviceIdleMode) {
                this.mInIdleMode = zIsDeviceIdleMode;
                if (!zIsDeviceIdleMode && this.mScanPending) {
                    this.mScanPending = false;
                    z = true;
                }
            }
        }
        if (z) {
            startScan(this.mContext.getOpPackageName());
        }
    }

    private boolean checkNetworkSettingsPermission(int i, int i2) {
        return this.mContext.checkPermission("android.permission.NETWORK_SETTINGS", i, i2) == 0;
    }

    private void enforceNetworkSettingsPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.NETWORK_SETTINGS", TAG);
    }

    private void enforceNetworkStackPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.NETWORK_STACK", TAG);
    }

    private void enforceAccessPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_WIFI_STATE", TAG);
    }

    private int enforceChangePermission(String str) {
        if (checkNetworkSettingsPermission(Binder.getCallingPid(), Binder.getCallingUid())) {
            return 0;
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.CHANGE_WIFI_STATE", TAG);
        return this.mAppOps.noteOp("android:change_wifi_state", Binder.getCallingUid(), str);
    }

    private void enforceLocationHardwarePermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.LOCATION_HARDWARE", "LocationHardware");
    }

    private void enforceReadCredentialPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_WIFI_CREDENTIAL", TAG);
    }

    private void enforceWorkSourcePermission() {
        this.mContext.enforceCallingPermission("android.permission.UPDATE_DEVICE_STATS", TAG);
    }

    private void enforceMulticastChangePermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CHANGE_WIFI_MULTICAST_STATE", TAG);
    }

    private void enforceConnectivityInternalPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", "ConnectivityService");
    }

    private void enforceLocationPermission(String str, int i) {
        this.mWifiPermissionsUtil.enforceLocationPermission(str, i);
    }

    private boolean checkWifiPermissionWhenPermissionReviewRequired() {
        return this.mPermissionReviewRequired && this.mContext.checkCallingPermission("android.permission.MANAGE_WIFI_WHEN_PERMISSION_REVIEW_REQUIRED") == 0;
    }

    public synchronized boolean setWifiEnabled(String str, boolean z) throws RemoteException {
        if (enforceChangePermission(str) != 0) {
            return false;
        }
        Slog.d(TAG, "setWifiEnabled: " + z + " pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + ", package=" + str);
        this.mLog.info("setWifiEnabled package=% uid=% enable=%").c(str).c((long) Binder.getCallingUid()).c(z).flush();
        boolean zCheckNetworkSettingsPermission = checkNetworkSettingsPermission(Binder.getCallingPid(), Binder.getCallingUid());
        if (this.mSettingsStore.isAirplaneModeOn() && !zCheckNetworkSettingsPermission) {
            this.mLog.info("setWifiEnabled in Airplane mode: only Settings can enable wifi").flush();
            return false;
        }
        if ((this.mWifiApState == 13) && !zCheckNetworkSettingsPermission) {
            this.mLog.info("setWifiEnabled SoftAp not disabled: only Settings can enable wifi").flush();
            return false;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            if (!this.mSettingsStore.handleWifiToggled(z)) {
                return true;
            }
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            if (this.mPermissionReviewRequired && !CtaManagerFactory.getInstance().makeCtaManager().isSystemApp(this.mContext, str)) {
                int wifiEnabledState = getWifiEnabledState();
                if (z) {
                    if ((wifiEnabledState == 0 || wifiEnabledState == 1) && startConsentUi(str, Binder.getCallingUid(), "android.net.wifi.action.REQUEST_ENABLE")) {
                        return true;
                    }
                } else if ((wifiEnabledState == 2 || wifiEnabledState == 3) && startConsentUi(str, Binder.getCallingUid(), "android.net.wifi.action.REQUEST_DISABLE")) {
                    return true;
                }
            }
            this.mWifiController.sendMessage(155656);
            return true;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public int getWifiEnabledState() {
        enforceAccessPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("getWifiEnabledState uid=%").c(Binder.getCallingUid()).flush();
        }
        return this.mWifiStateMachine.syncGetWifiState();
    }

    public int getWifiApEnabledState() {
        enforceAccessPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("getWifiApEnabledState uid=%").c(Binder.getCallingUid()).flush();
        }
        final MutableInt mutableInt = new MutableInt(11);
        this.mClientHandler.runWithScissors(new Runnable() {
            @Override
            public final void run() {
                mutableInt.value = this.f$0.mWifiApState;
            }
        }, 4000L);
        return mutableInt.value;
    }

    public void updateInterfaceIpState(final String str, final int i) {
        enforceNetworkStackPermission();
        this.mLog.info("updateInterfaceIpState uid=%").c(Binder.getCallingUid()).flush();
        this.mClientHandler.post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.updateInterfaceIpStateInternal(str, i);
            }
        });
    }

    private void updateInterfaceIpStateInternal(String str, int i) {
        synchronized (this.mLocalOnlyHotspotRequests) {
            Integer numPut = -1;
            if (str != null) {
                numPut = this.mIfaceIpModes.put(str, Integer.valueOf(i));
            }
            Slog.d(TAG, "updateInterfaceIpState: ifaceName=" + str + " mode=" + i + " previous mode= " + numPut);
            switch (i) {
                case -1:
                    if (str == null) {
                        this.mIfaceIpModes.clear();
                        return;
                    }
                    break;
                case 0:
                    Slog.d(TAG, "IP mode config error - need to clean up");
                    if (this.mLocalOnlyHotspotRequests.isEmpty()) {
                        Slog.d(TAG, "no LOHS requests, stop softap");
                        stopSoftAp();
                    } else {
                        Slog.d(TAG, "we have LOHS requests, clean them up");
                        sendHotspotFailedMessageToAllLOHSRequestInfoEntriesLocked(2);
                    }
                    updateInterfaceIpStateInternal(null, -1);
                    break;
                case 1:
                    sendHotspotFailedMessageToAllLOHSRequestInfoEntriesLocked(3);
                    break;
                case 2:
                    if (this.mLocalOnlyHotspotRequests.isEmpty()) {
                        stopSoftAp();
                        updateInterfaceIpStateInternal(null, -1);
                        return;
                    }
                    sendHotspotStartedMessageToAllLOHSRequestInfoEntriesLocked();
                    break;
                default:
                    this.mLog.warn("updateInterfaceIpStateInternal: unknown mode %").c(i).flush();
                    break;
            }
        }
    }

    public boolean startSoftAp(WifiConfiguration wifiConfiguration) {
        boolean zStartSoftApInternal;
        enforceNetworkStackPermission();
        this.mLog.info("startSoftAp uid=%").c(Binder.getCallingUid()).flush();
        synchronized (this.mLocalOnlyHotspotRequests) {
            if (!this.mLocalOnlyHotspotRequests.isEmpty()) {
                stopSoftApInternal();
            }
            zStartSoftApInternal = startSoftApInternal(wifiConfiguration, 1);
        }
        return zStartSoftApInternal;
    }

    private boolean startSoftApInternal(WifiConfiguration wifiConfiguration, int i) {
        this.mLog.trace("startSoftApInternal uid=% mode=%").c(Binder.getCallingUid()).c(i).flush();
        if (wifiConfiguration == null || WifiApConfigStore.validateApWifiConfiguration(wifiConfiguration)) {
            this.mWifiController.sendMessage(155658, 1, 0, new SoftApModeConfiguration(i, wifiConfiguration));
            return true;
        }
        Slog.e(TAG, "Invalid WifiConfiguration");
        return false;
    }

    public boolean stopSoftAp() {
        boolean zStopSoftApInternal;
        enforceNetworkStackPermission();
        this.mLog.info("stopSoftAp uid=%").c(Binder.getCallingUid()).flush();
        synchronized (this.mLocalOnlyHotspotRequests) {
            if (!this.mLocalOnlyHotspotRequests.isEmpty()) {
                this.mLog.trace("Call to stop Tethering while LOHS is active, Registered LOHS callers will be updated when softap stopped.").flush();
            }
            zStopSoftApInternal = stopSoftApInternal();
        }
        return zStopSoftApInternal;
    }

    private boolean stopSoftApInternal() {
        this.mLog.trace("stopSoftApInternal uid=%").c(Binder.getCallingUid()).flush();
        this.mWifiController.sendMessage(155658, 0, 0);
        return true;
    }

    private final class SoftApCallbackImpl implements WifiManager.SoftApCallback {
        private SoftApCallbackImpl() {
        }

        public void onStateChanged(int i, int i2) {
            WifiServiceImpl.this.mSoftApState = i;
            Iterator it = WifiServiceImpl.this.mRegisteredSoftApCallbacks.values().iterator();
            while (it.hasNext()) {
                try {
                    ((ISoftApCallback) it.next()).onStateChanged(i, i2);
                } catch (RemoteException e) {
                    Log.e(WifiServiceImpl.TAG, "onStateChanged: remote exception -- " + e);
                    it.remove();
                }
            }
        }

        public void onNumClientsChanged(int i) {
            WifiServiceImpl.this.mSoftApNumClients = i;
            Iterator it = WifiServiceImpl.this.mRegisteredSoftApCallbacks.values().iterator();
            while (it.hasNext()) {
                try {
                    ((ISoftApCallback) it.next()).onNumClientsChanged(i);
                } catch (RemoteException e) {
                    Log.e(WifiServiceImpl.TAG, "onNumClientsChanged: remote exception -- " + e);
                    it.remove();
                }
            }
        }
    }

    public void registerSoftApCallback(IBinder iBinder, final ISoftApCallback iSoftApCallback, final int i) {
        if (iBinder == null) {
            throw new IllegalArgumentException("Binder must not be null");
        }
        if (iSoftApCallback == null) {
            throw new IllegalArgumentException("Callback must not be null");
        }
        enforceNetworkSettingsPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("registerSoftApCallback uid=%").c(Binder.getCallingUid()).flush();
        }
        try {
            iBinder.linkToDeath(new AnonymousClass3(iBinder, i), 0);
            this.mClientHandler.post(new Runnable() {
                @Override
                public final void run() {
                    WifiServiceImpl.lambda$registerSoftApCallback$3(this.f$0, i, iSoftApCallback);
                }
            });
        } catch (RemoteException e) {
            Log.e(TAG, "Error on linkToDeath - " + e);
        }
    }

    class AnonymousClass3 implements IBinder.DeathRecipient {
        final IBinder val$binder;
        final int val$callbackIdentifier;

        AnonymousClass3(IBinder iBinder, int i) {
            this.val$binder = iBinder;
            this.val$callbackIdentifier = i;
        }

        @Override
        public void binderDied() {
            this.val$binder.unlinkToDeath(this, 0);
            ClientHandler clientHandler = WifiServiceImpl.this.mClientHandler;
            final int i = this.val$callbackIdentifier;
            clientHandler.post(new Runnable() {
                @Override
                public final void run() {
                    WifiServiceImpl.this.mRegisteredSoftApCallbacks.remove(Integer.valueOf(i));
                }
            });
        }
    }

    public static void lambda$registerSoftApCallback$3(WifiServiceImpl wifiServiceImpl, int i, ISoftApCallback iSoftApCallback) {
        wifiServiceImpl.mRegisteredSoftApCallbacks.put(Integer.valueOf(i), iSoftApCallback);
        if (wifiServiceImpl.mRegisteredSoftApCallbacks.size() > 20) {
            Log.wtf(TAG, "Too many soft AP callbacks: " + wifiServiceImpl.mRegisteredSoftApCallbacks.size());
        } else if (wifiServiceImpl.mRegisteredSoftApCallbacks.size() > 10) {
            Log.w(TAG, "Too many soft AP callbacks: " + wifiServiceImpl.mRegisteredSoftApCallbacks.size());
        }
        try {
            iSoftApCallback.onStateChanged(wifiServiceImpl.mSoftApState, 0);
            iSoftApCallback.onNumClientsChanged(wifiServiceImpl.mSoftApNumClients);
        } catch (RemoteException e) {
            Log.e(TAG, "registerSoftApCallback: remote exception -- " + e);
        }
    }

    public void unregisterSoftApCallback(final int i) {
        enforceNetworkSettingsPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("unregisterSoftApCallback uid=%").c(Binder.getCallingUid()).flush();
        }
        this.mClientHandler.post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.mRegisteredSoftApCallbacks.remove(Integer.valueOf(i));
            }
        });
    }

    private void handleWifiApStateChange(int i, int i2, int i3, String str, int i4) {
        Slog.d(TAG, "handleWifiApStateChange: currentState=" + i + " previousState=" + i2 + " errorCode= " + i3 + " ifaceName=" + str + " mode=" + i4);
        this.mWifiApState = i;
        if (i == 14) {
            synchronized (this.mLocalOnlyHotspotRequests) {
                int i5 = 1;
                if (i3 != 1) {
                    i5 = 2;
                }
                sendHotspotFailedMessageToAllLOHSRequestInfoEntriesLocked(i5);
                updateInterfaceIpStateInternal(null, -1);
            }
            return;
        }
        if (i == 10 || i == 11) {
            synchronized (this.mLocalOnlyHotspotRequests) {
                if (this.mIfaceIpModes.contains(2)) {
                    sendHotspotStoppedMessageToAllLOHSRequestInfoEntriesLocked();
                } else {
                    sendHotspotFailedMessageToAllLOHSRequestInfoEntriesLocked(2);
                }
                updateInterfaceIpState(null, -1);
            }
        }
    }

    @GuardedBy("mLocalOnlyHotspotRequests")
    private void sendHotspotFailedMessageToAllLOHSRequestInfoEntriesLocked(int i) {
        for (LocalOnlyHotspotRequestInfo localOnlyHotspotRequestInfo : this.mLocalOnlyHotspotRequests.values()) {
            try {
                localOnlyHotspotRequestInfo.sendHotspotFailedMessage(i);
                localOnlyHotspotRequestInfo.unlinkDeathRecipient();
            } catch (RemoteException e) {
            }
        }
        this.mLocalOnlyHotspotRequests.clear();
    }

    @GuardedBy("mLocalOnlyHotspotRequests")
    private void sendHotspotStoppedMessageToAllLOHSRequestInfoEntriesLocked() {
        for (LocalOnlyHotspotRequestInfo localOnlyHotspotRequestInfo : this.mLocalOnlyHotspotRequests.values()) {
            try {
                localOnlyHotspotRequestInfo.sendHotspotStoppedMessage();
                localOnlyHotspotRequestInfo.unlinkDeathRecipient();
            } catch (RemoteException e) {
            }
        }
        this.mLocalOnlyHotspotRequests.clear();
    }

    @GuardedBy("mLocalOnlyHotspotRequests")
    private void sendHotspotStartedMessageToAllLOHSRequestInfoEntriesLocked() {
        Iterator<LocalOnlyHotspotRequestInfo> it = this.mLocalOnlyHotspotRequests.values().iterator();
        while (it.hasNext()) {
            try {
                it.next().sendHotspotStartedMessage(this.mLocalOnlyHotspotConfig);
            } catch (RemoteException e) {
            }
        }
    }

    @VisibleForTesting
    void registerLOHSForTest(int i, LocalOnlyHotspotRequestInfo localOnlyHotspotRequestInfo) {
        this.mLocalOnlyHotspotRequests.put(Integer.valueOf(i), localOnlyHotspotRequestInfo);
    }

    public int startLocalOnlyHotspot(Messenger messenger, IBinder iBinder, String str) {
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        if (enforceChangePermission(str) != 0) {
            return 2;
        }
        enforceLocationPermission(str, callingUid);
        if (this.mSettingsStore.getLocationModeSetting(this.mContext) == 0) {
            throw new SecurityException("Location mode is not enabled.");
        }
        if (this.mUserManager.hasUserRestriction("no_config_tethering")) {
            return 4;
        }
        try {
            if (!this.mFrameworkFacade.isAppForeground(callingUid)) {
                return 3;
            }
            this.mLog.info("startLocalOnlyHotspot uid=% pid=%").c(callingUid).c(callingPid).flush();
            synchronized (this.mLocalOnlyHotspotRequests) {
                if (this.mIfaceIpModes.contains(1)) {
                    this.mLog.info("Cannot start localOnlyHotspot when WiFi Tethering is active.").flush();
                    return 3;
                }
                if (this.mLocalOnlyHotspotRequests.get(Integer.valueOf(callingPid)) != null) {
                    this.mLog.trace("caller already has an active request").flush();
                    throw new IllegalStateException("Caller already has an active LocalOnlyHotspot request");
                }
                LocalOnlyHotspotRequestInfo localOnlyHotspotRequestInfo = new LocalOnlyHotspotRequestInfo(iBinder, messenger, new LocalOnlyRequestorCallback());
                if (this.mIfaceIpModes.contains(2)) {
                    try {
                        this.mLog.trace("LOHS already up, trigger onStarted callback").flush();
                        localOnlyHotspotRequestInfo.sendHotspotStartedMessage(this.mLocalOnlyHotspotConfig);
                    } catch (RemoteException e) {
                        return 2;
                    }
                } else if (this.mLocalOnlyHotspotRequests.isEmpty()) {
                    this.mLocalOnlyHotspotConfig = WifiApConfigStore.generateLocalOnlyHotspotConfig(this.mContext);
                    startSoftApInternal(this.mLocalOnlyHotspotConfig, 2);
                }
                this.mLocalOnlyHotspotRequests.put(Integer.valueOf(callingPid), localOnlyHotspotRequestInfo);
                return 0;
            }
        } catch (RemoteException e2) {
            this.mLog.warn("RemoteException during isAppForeground when calling startLOHS").flush();
            return 3;
        }
    }

    public void stopLocalOnlyHotspot() {
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        this.mLog.info("stopLocalOnlyHotspot uid=% pid=%").c(callingUid).c(callingPid).flush();
        synchronized (this.mLocalOnlyHotspotRequests) {
            LocalOnlyHotspotRequestInfo localOnlyHotspotRequestInfo = this.mLocalOnlyHotspotRequests.get(Integer.valueOf(callingPid));
            if (localOnlyHotspotRequestInfo == null) {
                return;
            }
            localOnlyHotspotRequestInfo.unlinkDeathRecipient();
            unregisterCallingAppAndStopLocalOnlyHotspot(localOnlyHotspotRequestInfo);
        }
    }

    private void unregisterCallingAppAndStopLocalOnlyHotspot(LocalOnlyHotspotRequestInfo localOnlyHotspotRequestInfo) {
        this.mLog.trace("unregisterCallingAppAndStopLocalOnlyHotspot pid=%").c(localOnlyHotspotRequestInfo.getPid()).flush();
        synchronized (this.mLocalOnlyHotspotRequests) {
            if (this.mLocalOnlyHotspotRequests.remove(Integer.valueOf(localOnlyHotspotRequestInfo.getPid())) == null) {
                this.mLog.trace("LocalOnlyHotspotRequestInfo not found to remove").flush();
                return;
            }
            if (this.mLocalOnlyHotspotRequests.isEmpty()) {
                this.mLocalOnlyHotspotConfig = null;
                updateInterfaceIpStateInternal(null, -1);
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    stopSoftApInternal();
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    throw th;
                }
            }
        }
    }

    public void startWatchLocalOnlyHotspot(Messenger messenger, IBinder iBinder) {
        enforceNetworkSettingsPermission();
        throw new UnsupportedOperationException("LocalOnlyHotspot is still in development");
    }

    public void stopWatchLocalOnlyHotspot() {
        enforceNetworkSettingsPermission();
        throw new UnsupportedOperationException("LocalOnlyHotspot is still in development");
    }

    public WifiConfiguration getWifiApConfiguration() {
        enforceAccessPermission();
        int callingUid = Binder.getCallingUid();
        if (!this.mWifiPermissionsUtil.checkConfigOverridePermission(callingUid)) {
            throw new SecurityException("App not allowed to read or update stored WiFi Ap config (uid = " + callingUid + ")");
        }
        this.mLog.info("getWifiApConfiguration uid=%").c(callingUid).flush();
        final GeneralUtil.Mutable mutable = new GeneralUtil.Mutable();
        if (this.mWifiInjector.getWifiStateMachineHandler().runWithScissors(new Runnable() {
            @Override
            public final void run() {
                mutable.value = this.f$0.mWifiApConfigStore.getApConfiguration();
            }
        }, 4000L)) {
            return (WifiConfiguration) mutable.value;
        }
        Log.e(TAG, "Failed to post runnable to fetch ap config");
        return new WifiConfiguration();
    }

    public boolean setWifiApConfiguration(final WifiConfiguration wifiConfiguration, String str) {
        if (enforceChangePermission(str) != 0) {
            return false;
        }
        int callingUid = Binder.getCallingUid();
        if (!this.mWifiPermissionsUtil.checkConfigOverridePermission(callingUid)) {
            throw new SecurityException("App not allowed to read or update stored WiFi AP config (uid = " + callingUid + ")");
        }
        this.mLog.info("setWifiApConfiguration uid=%").c(callingUid).flush();
        if (wifiConfiguration == null) {
            return false;
        }
        if (WifiApConfigStore.validateApWifiConfiguration(wifiConfiguration)) {
            this.mWifiStateMachineHandler.post(new Runnable() {
                @Override
                public final void run() {
                    this.f$0.mWifiApConfigStore.setApConfiguration(wifiConfiguration);
                }
            });
            return true;
        }
        Slog.e(TAG, "Invalid WifiConfiguration");
        return false;
    }

    public boolean isScanAlwaysAvailable() {
        enforceAccessPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("isScanAlwaysAvailable uid=%").c(Binder.getCallingUid()).flush();
        }
        return this.mSettingsStore.isScanAlwaysAvailable();
    }

    public void disconnect(String str) {
        if (enforceChangePermission(str) != 0) {
            return;
        }
        this.mLog.info("disconnect uid=%").c(Binder.getCallingUid()).flush();
        this.mWifiStateMachine.disconnectCommand();
    }

    public void reconnect(String str) {
        if (enforceChangePermission(str) != 0) {
            return;
        }
        this.mLog.info("reconnect uid=%").c(Binder.getCallingUid()).flush();
        this.mWifiStateMachine.reconnectCommand(new WorkSource(Binder.getCallingUid()));
    }

    public void reassociate(String str) {
        if (enforceChangePermission(str) != 0) {
            return;
        }
        this.mLog.info("reassociate uid=%").c(Binder.getCallingUid()).flush();
        this.mWifiStateMachine.reassociateCommand();
    }

    public int getSupportedFeatures() {
        enforceAccessPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("getSupportedFeatures uid=%").c(Binder.getCallingUid()).flush();
        }
        if (this.mWifiStateMachineChannel != null) {
            return this.mWifiStateMachine.syncGetSupportedFeatures(this.mWifiStateMachineChannel);
        }
        Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
        return 0;
    }

    public void requestActivityInfo(ResultReceiver resultReceiver) {
        Bundle bundle = new Bundle();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("requestActivityInfo uid=%").c(Binder.getCallingUid()).flush();
        }
        bundle.putParcelable("controller_activity", reportActivityInfo());
        resultReceiver.send(0, bundle);
    }

    public WifiActivityEnergyInfo reportActivityInfo() {
        WifiActivityEnergyInfo wifiActivityEnergyInfo;
        double d;
        long[] jArr;
        long j;
        enforceAccessPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("reportActivityInfo uid=%").c(Binder.getCallingUid()).flush();
        }
        if ((getSupportedFeatures() & 65536) == 0) {
            return null;
        }
        if (this.mWifiStateMachineChannel == null) {
            Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
            return null;
        }
        WifiLinkLayerStats wifiLinkLayerStatsSyncGetLinkLayerStats = this.mWifiStateMachine.syncGetLinkLayerStats(this.mWifiStateMachineChannel);
        if (wifiLinkLayerStatsSyncGetLinkLayerStats != null) {
            double averagePower = this.mPowerProfile.getAveragePower("wifi.controller.idle");
            double averagePower2 = this.mPowerProfile.getAveragePower("wifi.controller.rx");
            double averagePower3 = this.mPowerProfile.getAveragePower("wifi.controller.tx");
            double averagePower4 = this.mPowerProfile.getAveragePower("wifi.controller.voltage") / 1000.0d;
            long j2 = (wifiLinkLayerStatsSyncGetLinkLayerStats.on_time - wifiLinkLayerStatsSyncGetLinkLayerStats.tx_time) - wifiLinkLayerStatsSyncGetLinkLayerStats.rx_time;
            int i = 0;
            if (wifiLinkLayerStatsSyncGetLinkLayerStats.tx_time_per_level != null) {
                long[] jArr2 = new long[wifiLinkLayerStatsSyncGetLinkLayerStats.tx_time_per_level.length];
                while (i < jArr2.length) {
                    jArr2[i] = wifiLinkLayerStatsSyncGetLinkLayerStats.tx_time_per_level[i];
                    i++;
                    averagePower = averagePower;
                }
                d = averagePower;
                jArr = jArr2;
            } else {
                d = averagePower;
                jArr = new long[0];
            }
            long j3 = (long) (((((double) wifiLinkLayerStatsSyncGetLinkLayerStats.tx_time) * averagePower3) + (((double) wifiLinkLayerStatsSyncGetLinkLayerStats.rx_time) * averagePower2) + (j2 * d)) * averagePower4);
            if (j2 < 0 || wifiLinkLayerStatsSyncGetLinkLayerStats.on_time < 0 || wifiLinkLayerStatsSyncGetLinkLayerStats.tx_time < 0 || wifiLinkLayerStatsSyncGetLinkLayerStats.rx_time < 0 || wifiLinkLayerStatsSyncGetLinkLayerStats.on_time_scan < 0 || j3 < 0) {
                StringBuilder sb = new StringBuilder();
                sb.append(" rxIdleCur=" + d);
                sb.append(" rxCur=" + averagePower2);
                sb.append(" txCur=" + averagePower3);
                sb.append(" voltage=" + averagePower4);
                sb.append(" on_time=" + wifiLinkLayerStatsSyncGetLinkLayerStats.on_time);
                sb.append(" tx_time=" + wifiLinkLayerStatsSyncGetLinkLayerStats.tx_time);
                sb.append(" tx_time_per_level=" + Arrays.toString(jArr));
                sb.append(" rx_time=" + wifiLinkLayerStatsSyncGetLinkLayerStats.rx_time);
                sb.append(" rxIdleTime=" + j2);
                sb.append(" scan_time=" + wifiLinkLayerStatsSyncGetLinkLayerStats.on_time_scan);
                StringBuilder sb2 = new StringBuilder();
                sb2.append(" energy=");
                j = j3;
                sb2.append(j);
                sb.append(sb2.toString());
                Log.d(TAG, " reportActivityInfo: " + sb.toString());
            } else {
                j = j3;
            }
            wifiActivityEnergyInfo = new WifiActivityEnergyInfo(this.mClock.getElapsedSinceBootMillis(), 3, wifiLinkLayerStatsSyncGetLinkLayerStats.tx_time, jArr, wifiLinkLayerStatsSyncGetLinkLayerStats.rx_time, wifiLinkLayerStatsSyncGetLinkLayerStats.on_time_scan, j2, j);
        } else {
            wifiActivityEnergyInfo = null;
        }
        if (wifiActivityEnergyInfo != null && wifiActivityEnergyInfo.isValid()) {
            return wifiActivityEnergyInfo;
        }
        return null;
    }

    public ParceledListSlice<WifiConfiguration> getConfiguredNetworks() {
        enforceAccessPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("getConfiguredNetworks uid=%").c(Binder.getCallingUid()).flush();
        }
        if (this.mWifiStateMachineChannel != null) {
            List<WifiConfiguration> listSyncGetConfiguredNetworks = this.mWifiStateMachine.syncGetConfiguredNetworks(Binder.getCallingUid(), this.mWifiStateMachineChannel);
            if (listSyncGetConfiguredNetworks != null) {
                return new ParceledListSlice<>(listSyncGetConfiguredNetworks);
            }
            return null;
        }
        Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
        return null;
    }

    public ParceledListSlice<WifiConfiguration> getPrivilegedConfiguredNetworks() {
        enforceReadCredentialPermission();
        enforceAccessPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("getPrivilegedConfiguredNetworks uid=%").c(Binder.getCallingUid()).flush();
        }
        if (this.mWifiStateMachineChannel != null) {
            List<WifiConfiguration> listSyncGetPrivilegedConfiguredNetwork = this.mWifiStateMachine.syncGetPrivilegedConfiguredNetwork(this.mWifiStateMachineChannel);
            if (listSyncGetPrivilegedConfiguredNetwork != null) {
                return new ParceledListSlice<>(listSyncGetPrivilegedConfiguredNetwork);
            }
            return null;
        }
        Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
        return null;
    }

    public WifiConfiguration getMatchingWifiConfig(ScanResult scanResult) {
        enforceAccessPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("getMatchingWifiConfig uid=%").c(Binder.getCallingUid()).flush();
        }
        if (!this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi.passpoint")) {
            throw new UnsupportedOperationException("Passpoint not enabled");
        }
        return this.mWifiStateMachine.syncGetMatchingWifiConfig(scanResult, this.mWifiStateMachineChannel);
    }

    public List<WifiConfiguration> getAllMatchingWifiConfigs(ScanResult scanResult) {
        enforceAccessPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("getMatchingPasspointConfigurations uid=%").c(Binder.getCallingUid()).flush();
        }
        if (!this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi.passpoint")) {
            throw new UnsupportedOperationException("Passpoint not enabled");
        }
        return this.mWifiStateMachine.getAllMatchingWifiConfigs(scanResult, this.mWifiStateMachineChannel);
    }

    public List<OsuProvider> getMatchingOsuProviders(ScanResult scanResult) {
        enforceAccessPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("getMatchingOsuProviders uid=%").c(Binder.getCallingUid()).flush();
        }
        if (!this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi.passpoint")) {
            throw new UnsupportedOperationException("Passpoint not enabled");
        }
        return this.mWifiStateMachine.syncGetMatchingOsuProviders(scanResult, this.mWifiStateMachineChannel);
    }

    public int addOrUpdateNetwork(WifiConfiguration wifiConfiguration, String str) {
        if (enforceChangePermission(str) != 0) {
            return -1;
        }
        this.mLog.info("addOrUpdateNetwork uid=%").c(Binder.getCallingUid()).flush();
        if (wifiConfiguration.isPasspoint()) {
            PasspointConfiguration passpointConfigurationConvertFromWifiConfig = PasspointProvider.convertFromWifiConfig(wifiConfiguration);
            if (passpointConfigurationConvertFromWifiConfig.getCredential() == null) {
                Slog.e(TAG, "Missing credential for Passpoint profile");
                return -1;
            }
            passpointConfigurationConvertFromWifiConfig.getCredential().setCaCertificate(wifiConfiguration.enterpriseConfig.getCaCertificate());
            passpointConfigurationConvertFromWifiConfig.getCredential().setClientCertificateChain(wifiConfiguration.enterpriseConfig.getClientCertificateChain());
            passpointConfigurationConvertFromWifiConfig.getCredential().setClientPrivateKey(wifiConfiguration.enterpriseConfig.getClientPrivateKey());
            if (!addOrUpdatePasspointConfiguration(passpointConfigurationConvertFromWifiConfig, str)) {
                Slog.e(TAG, "Failed to add Passpoint profile");
                return -1;
            }
            return 0;
        }
        if (wifiConfiguration != null) {
            Slog.i("addOrUpdateNetwork", " uid = " + Integer.toString(Binder.getCallingUid()) + " SSID " + wifiConfiguration.SSID + " nid=" + Integer.toString(wifiConfiguration.networkId));
            if (wifiConfiguration.networkId == -1) {
                wifiConfiguration.creatorUid = Binder.getCallingUid();
            } else {
                wifiConfiguration.lastUpdateUid = Binder.getCallingUid();
            }
            if (this.mWifiStateMachineChannel != null) {
                return this.mWifiStateMachine.syncAddOrUpdateNetwork(this.mWifiStateMachineChannel, wifiConfiguration);
            }
            Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
            return -1;
        }
        Slog.e(TAG, "bad network configuration");
        return -1;
    }

    public static void verifyCert(X509Certificate x509Certificate) throws GeneralSecurityException, IOException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        CertPathValidator certPathValidator = CertPathValidator.getInstance(CertPathValidator.getDefaultType());
        CertPath certPathGenerateCertPath = certificateFactory.generateCertPath(Arrays.asList(x509Certificate));
        KeyStore keyStore = KeyStore.getInstance("AndroidCAStore");
        keyStore.load(null, null);
        PKIXParameters pKIXParameters = new PKIXParameters(keyStore);
        pKIXParameters.setRevocationEnabled(false);
        certPathValidator.validate(certPathGenerateCertPath, pKIXParameters);
    }

    public boolean removeNetwork(int i, String str) {
        if (enforceChangePermission(str) != 0) {
            return false;
        }
        this.mLog.info("removeNetwork uid=%").c(Binder.getCallingUid()).flush();
        if (this.mWifiStateMachineChannel != null) {
            return this.mWifiStateMachine.syncRemoveNetwork(this.mWifiStateMachineChannel, i);
        }
        Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
        return false;
    }

    public boolean enableNetwork(int i, boolean z, String str) {
        if (enforceChangePermission(str) != 0) {
            return false;
        }
        this.mLog.info("enableNetwork uid=% disableOthers=%").c(Binder.getCallingUid()).c(z).flush();
        if (this.mWifiStateMachineChannel != null) {
            return this.mWifiStateMachine.syncEnableNetwork(this.mWifiStateMachineChannel, i, z);
        }
        Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
        return false;
    }

    public boolean disableNetwork(int i, String str) {
        if (enforceChangePermission(str) != 0) {
            return false;
        }
        this.mLog.info("disableNetwork uid=%").c(Binder.getCallingUid()).flush();
        if (this.mWifiStateMachineChannel != null) {
            return this.mWifiStateMachine.syncDisableNetwork(this.mWifiStateMachineChannel, i);
        }
        Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
        return false;
    }

    public WifiInfo getConnectionInfo(String str) {
        boolean z;
        enforceAccessPermission();
        int callingUid = Binder.getCallingUid();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("getConnectionInfo uid=%").c(callingUid).flush();
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            WifiInfo wifiInfoSyncRequestConnectionInfo = this.mWifiStateMachine.syncRequestConnectionInfo();
            boolean z2 = true;
            try {
                z = this.mWifiInjector.getWifiPermissionsWrapper().getLocalMacAddressPermission(callingUid) != 0;
                try {
                    this.mWifiPermissionsUtil.enforceCanAccessScanResults(str, callingUid);
                    z2 = false;
                } catch (RemoteException e) {
                    e = e;
                    Log.e(TAG, "Error checking receiver permission", e);
                } catch (SecurityException e2) {
                }
            } catch (RemoteException e3) {
                e = e3;
                z = true;
            } catch (SecurityException e4) {
                z = true;
            }
            if (z) {
                wifiInfoSyncRequestConnectionInfo.setMacAddress("02:00:00:00:00:00");
            }
            if (z2) {
                wifiInfoSyncRequestConnectionInfo.setBSSID("02:00:00:00:00:00");
                wifiInfoSyncRequestConnectionInfo.setSSID(WifiSsid.createFromHex(null));
            }
            if (this.mVerboseLoggingEnabled && (z2 || z)) {
                this.mLog.v("getConnectionInfo: hideBssidAndSSid=" + z2 + ", hideDefaultMacAddress=" + z);
            }
            return wifiInfoSyncRequestConnectionInfo;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public List<ScanResult> getScanResults(String str) {
        enforceAccessPermission();
        int callingUid = Binder.getCallingUid();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("getScanResults uid=%").c(callingUid).flush();
        }
        try {
            this.mWifiPermissionsUtil.enforceCanAccessScanResults(str, callingUid);
            final ArrayList arrayList = new ArrayList();
            if (!this.mWifiInjector.getWifiStateMachineHandler().runWithScissors(new Runnable() {
                @Override
                public final void run() {
                    arrayList.addAll(this.f$0.mScanRequestProxy.getScanResults());
                }
            }, 4000L)) {
                Log.e(TAG, "Failed to post runnable to fetch scan results");
            }
            return arrayList;
        } catch (SecurityException e) {
            return new ArrayList();
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public boolean addOrUpdatePasspointConfiguration(PasspointConfiguration passpointConfiguration, String str) {
        if (enforceChangePermission(str) != 0) {
            return false;
        }
        this.mLog.info("addorUpdatePasspointConfiguration uid=%").c(Binder.getCallingUid()).flush();
        if (!this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi.passpoint")) {
            throw new UnsupportedOperationException("Passpoint not enabled");
        }
        return this.mWifiStateMachine.syncAddOrUpdatePasspointConfig(this.mWifiStateMachineChannel, passpointConfiguration, Binder.getCallingUid());
    }

    public boolean removePasspointConfiguration(String str, String str2) {
        if (enforceChangePermission(str2) != 0) {
            return false;
        }
        this.mLog.info("removePasspointConfiguration uid=%").c(Binder.getCallingUid()).flush();
        if (!this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi.passpoint")) {
            throw new UnsupportedOperationException("Passpoint not enabled");
        }
        return this.mWifiStateMachine.syncRemovePasspointConfig(this.mWifiStateMachineChannel, str);
    }

    public List<PasspointConfiguration> getPasspointConfigurations() {
        enforceAccessPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("getPasspointConfigurations uid=%").c(Binder.getCallingUid()).flush();
        }
        if (!this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi.passpoint")) {
            throw new UnsupportedOperationException("Passpoint not enabled");
        }
        return this.mWifiStateMachine.syncGetPasspointConfigs(this.mWifiStateMachineChannel);
    }

    public void queryPasspointIcon(long j, String str) {
        enforceAccessPermission();
        this.mLog.info("queryPasspointIcon uid=%").c(Binder.getCallingUid()).flush();
        if (!this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi.passpoint")) {
            throw new UnsupportedOperationException("Passpoint not enabled");
        }
        this.mWifiStateMachine.syncQueryPasspointIcon(this.mWifiStateMachineChannel, j, str);
    }

    public int matchProviderWithCurrentNetwork(String str) {
        this.mLog.info("matchProviderWithCurrentNetwork uid=%").c(Binder.getCallingUid()).flush();
        return this.mWifiStateMachine.matchProviderWithCurrentNetwork(this.mWifiStateMachineChannel, str);
    }

    public void deauthenticateNetwork(long j, boolean z) {
        this.mLog.info("deauthenticateNetwork uid=%").c(Binder.getCallingUid()).flush();
        this.mWifiStateMachine.deauthenticateNetwork(this.mWifiStateMachineChannel, j, z);
    }

    public void setCountryCode(String str) {
        Slog.i(TAG, "WifiService trying to set country code to " + str);
        enforceConnectivityInternalPermission();
        this.mLog.info("setCountryCode uid=%").c((long) Binder.getCallingUid()).flush();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        this.mCountryCode.setCountryCode(str);
        Binder.restoreCallingIdentity(jClearCallingIdentity);
    }

    public String getCountryCode() {
        enforceConnectivityInternalPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("getCountryCode uid=%").c(Binder.getCallingUid()).flush();
        }
        return this.mCountryCode.getCountryCode();
    }

    public boolean isDualBandSupported() {
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("isDualBandSupported uid=%").c(Binder.getCallingUid()).flush();
        }
        return this.mContext.getResources().getBoolean(R.^attr-private.preferenceHeaderPanelStyle);
    }

    public boolean needs5GHzToAnyApBandConversion() {
        enforceNetworkSettingsPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("needs5GHzToAnyApBandConversion uid=%").c(Binder.getCallingUid()).flush();
        }
        return this.mContext.getResources().getBoolean(R.^attr-private.preferenceFrameLayoutStyle);
    }

    @Deprecated
    public DhcpInfo getDhcpInfo() {
        enforceAccessPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("getDhcpInfo uid=%").c(Binder.getCallingUid()).flush();
        }
        DhcpResults dhcpResultsSyncGetDhcpResults = this.mWifiStateMachine.syncGetDhcpResults();
        DhcpInfo dhcpInfo = new DhcpInfo();
        if (dhcpResultsSyncGetDhcpResults.ipAddress != null && (dhcpResultsSyncGetDhcpResults.ipAddress.getAddress() instanceof Inet4Address)) {
            dhcpInfo.ipAddress = NetworkUtils.inetAddressToInt((Inet4Address) dhcpResultsSyncGetDhcpResults.ipAddress.getAddress());
        }
        if (dhcpResultsSyncGetDhcpResults.gateway != null) {
            dhcpInfo.gateway = NetworkUtils.inetAddressToInt((Inet4Address) dhcpResultsSyncGetDhcpResults.gateway);
        }
        int i = 0;
        for (InetAddress inetAddress : dhcpResultsSyncGetDhcpResults.dnsServers) {
            if (inetAddress instanceof Inet4Address) {
                if (i == 0) {
                    dhcpInfo.dns1 = NetworkUtils.inetAddressToInt((Inet4Address) inetAddress);
                } else {
                    dhcpInfo.dns2 = NetworkUtils.inetAddressToInt((Inet4Address) inetAddress);
                }
                i++;
                if (i > 1) {
                    break;
                }
            }
        }
        Inet4Address inet4Address = dhcpResultsSyncGetDhcpResults.serverAddress;
        if (inet4Address != null) {
            dhcpInfo.serverAddress = NetworkUtils.inetAddressToInt(inet4Address);
        }
        dhcpInfo.leaseDuration = dhcpResultsSyncGetDhcpResults.leaseDuration;
        return dhcpInfo;
    }

    class TdlsTaskParams {
        public boolean enable;
        public String remoteIpAddress;

        TdlsTaskParams() {
        }
    }

    class TdlsTask extends AsyncTask<TdlsTaskParams, Integer, Integer> {
        TdlsTask() {
        }

        @Override
        protected java.lang.Integer doInBackground(com.android.server.wifi.WifiServiceImpl.TdlsTaskParams... r8) {
            r8 = r8[0];
            r1 = r8.remoteIpAddress.trim();
            r8 = r8.enable;
            r2 = 0;
            r2 = null;
            r2 = 0;
            r2 = 0;
            r2 = 0;
            r3 = new java.io.BufferedReader(new java.io.FileReader("/proc/net/arp"));
            r3.readLine();
            while (true) {
                r4 = r3.readLine();
                if (r4 != null) {
                    r4 = r4.split("[ ]+");
                    if (r4.length < 6) {
                    } else {
                        r5 = r4[0];
                        r4 = r4[3];
                        if (r1.equals(r5)) {
                            r2 = r4;
                        }
                    }
                }
            }
            if (r2 == null) {
                r2 = new java.lang.StringBuilder();
                r2.append("Did not find remoteAddress {");
                r2.append(r1);
                r2.append("} in /proc/net/arp");
                android.util.Slog.w(com.android.server.wifi.WifiServiceImpl.TAG, r2.toString());
                r2 = r2;
            } else {
                com.android.server.wifi.WifiServiceImpl.this.enableTdlsWithMacAddress(r2, r8);
                r2 = r2;
            }
            r3.close();
            while (true) {
                return 0;
            }
        }
    }

    public void enableTdls(String str, boolean z) {
        if (str == null) {
            throw new IllegalArgumentException("remoteAddress cannot be null");
        }
        this.mLog.info("enableTdls uid=% enable=%").c(Binder.getCallingUid()).c(z).flush();
        TdlsTaskParams tdlsTaskParams = new TdlsTaskParams();
        tdlsTaskParams.remoteIpAddress = str;
        tdlsTaskParams.enable = z;
        new TdlsTask().execute(tdlsTaskParams);
    }

    public void enableTdlsWithMacAddress(String str, boolean z) {
        this.mLog.info("enableTdlsWithMacAddress uid=% enable=%").c(Binder.getCallingUid()).c(z).flush();
        if (str == null) {
            throw new IllegalArgumentException("remoteMacAddress cannot be null");
        }
        this.mWifiStateMachine.enableTdls(str, z);
    }

    public Messenger getWifiServiceMessenger(String str) throws RemoteException {
        enforceAccessPermission();
        if (enforceChangePermission(str) != 0) {
            throw new SecurityException("Could not create wifi service messenger");
        }
        this.mLog.info("getWifiServiceMessenger uid=%").c(Binder.getCallingUid()).flush();
        return new Messenger(this.mClientHandler);
    }

    public void disableEphemeralNetwork(String str, String str2) {
        enforceAccessPermission();
        if (enforceChangePermission(str2) != 0) {
            return;
        }
        this.mLog.info("disableEphemeralNetwork uid=%").c(Binder.getCallingUid()).flush();
        this.mWifiStateMachine.disableEphemeralNetwork(str);
    }

    private boolean startConsentUi(String str, int i, String str2) throws RemoteException {
        if (UserHandle.getAppId(i) == 1000 || checkWifiPermissionWhenPermissionReviewRequired()) {
            return false;
        }
        try {
            if (this.mContext.getPackageManager().getApplicationInfoAsUser(str, 268435456, UserHandle.getUserId(i)).uid != i) {
                throw new SecurityException("Package " + str + " not in uid " + i);
            }
            Intent intent = new Intent(str2);
            intent.addFlags(276824064);
            intent.putExtra("android.intent.extra.PACKAGE_NAME", str);
            this.mContext.startActivity(intent);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RemoteException(e.getMessage());
        }
    }

    private void registerForScanModeChange() {
        this.mFrameworkFacade.registerContentObserver(this.mContext, Settings.Global.getUriFor("wifi_scan_always_enabled"), false, new ContentObserver(null) {
            @Override
            public void onChange(boolean z) {
                WifiServiceImpl.this.mSettingsStore.handleWifiScanAlwaysAvailableToggled();
                WifiServiceImpl.this.mWifiController.sendMessage(155655);
            }
        });
    }

    private void registerForBroadcasts() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_PRESENT");
        intentFilter.addAction("android.intent.action.USER_REMOVED");
        intentFilter.addAction("android.net.wifi.STATE_CHANGE");
        intentFilter.addAction("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED");
        intentFilter.addAction("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED");
        intentFilter.addAction("android.os.action.DEVICE_IDLE_MODE_CHANGED");
        if (this.mContext.getResources().getBoolean(R.^attr-private.relativeTimeUnitDisplayLength)) {
            intentFilter.addAction("android.intent.action.EMERGENCY_CALL_STATE_CHANGED");
        }
        this.mContext.registerReceiver(this.mReceiver, intentFilter);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.intent.action.PACKAGE_FULLY_REMOVED");
        intentFilter2.addDataScheme("package");
        this.mContext.registerReceiver(new AnonymousClass6(), intentFilter2);
    }

    class AnonymousClass6 extends BroadcastReceiver {
        AnonymousClass6() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.PACKAGE_FULLY_REMOVED")) {
                final int intExtra = intent.getIntExtra("android.intent.extra.UID", -1);
                Uri data = intent.getData();
                if (intExtra == -1 || data == null) {
                    return;
                }
                final String schemeSpecificPart = data.getSchemeSpecificPart();
                WifiServiceImpl.this.mWifiStateMachine.removeAppConfigs(schemeSpecificPart, intExtra);
                WifiServiceImpl.this.mWifiStateMachineHandler.post(new Runnable() {
                    @Override
                    public final void run() {
                        WifiServiceImpl.this.mScanRequestProxy.clearScanRequestTimestampsForApp(schemeSpecificPart, intExtra);
                    }
                });
            }
        }
    }

    public void onShellCommand(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, FileDescriptor fileDescriptor3, String[] strArr, ShellCallback shellCallback, ResultReceiver resultReceiver) {
        new WifiShellCommand(this.mWifiStateMachine).exec(this, fileDescriptor, fileDescriptor2, fileDescriptor3, strArr, shellCallback, resultReceiver);
    }

    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            printWriter.println("Permission Denial: can't dump WifiService from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            return;
        }
        if (strArr != null && strArr.length > 0 && WifiMetrics.PROTO_DUMP_ARG.equals(strArr[0])) {
            this.mWifiStateMachine.updateWifiMetrics();
            this.mWifiMetrics.dump(fileDescriptor, printWriter, strArr);
            return;
        }
        if (strArr == null || strArr.length <= 0 || !"ipclient".equals(strArr[0])) {
            if (strArr != null && strArr.length > 0 && WifiScoreReport.DUMP_ARG.equals(strArr[0])) {
                WifiScoreReport wifiScoreReport = this.mWifiStateMachine.getWifiScoreReport();
                if (wifiScoreReport != null) {
                    wifiScoreReport.dump(fileDescriptor, printWriter, strArr);
                    return;
                }
                return;
            }
            printWriter.println("Wi-Fi is " + this.mWifiStateMachine.syncGetWifiStateByName());
            StringBuilder sb = new StringBuilder();
            sb.append("Verbose logging is ");
            sb.append(this.mVerboseLoggingEnabled ? "on" : "off");
            printWriter.println(sb.toString());
            printWriter.println("Stay-awake conditions: " + this.mFacade.getIntegerSetting(this.mContext, "stay_on_while_plugged_in", 0));
            printWriter.println("mInIdleMode " + this.mInIdleMode);
            printWriter.println("mScanPending " + this.mScanPending);
            this.mWifiController.dump(fileDescriptor, printWriter, strArr);
            this.mSettingsStore.dump(fileDescriptor, printWriter, strArr);
            this.mTrafficPoller.dump(fileDescriptor, printWriter, strArr);
            printWriter.println();
            printWriter.println("Locks held:");
            this.mWifiLockManager.dump(printWriter);
            printWriter.println();
            this.mWifiMulticastLockManager.dump(printWriter);
            printWriter.println();
            this.mWifiStateMachinePrime.dump(fileDescriptor, printWriter, strArr);
            printWriter.println();
            this.mWifiStateMachine.dump(fileDescriptor, printWriter, strArr);
            printWriter.println();
            this.mWifiStateMachine.updateWifiMetrics();
            this.mWifiMetrics.dump(fileDescriptor, printWriter, strArr);
            printWriter.println();
            this.mWifiBackupRestore.dump(fileDescriptor, printWriter, strArr);
            printWriter.println();
            printWriter.println("ScoringParams: settings put global wifi_score_params " + this.mWifiInjector.getScoringParams());
            printWriter.println();
            WifiScoreReport wifiScoreReport2 = this.mWifiStateMachine.getWifiScoreReport();
            if (wifiScoreReport2 != null) {
                printWriter.println("WifiScoreReport:");
                wifiScoreReport2.dump(fileDescriptor, printWriter, strArr);
            }
            printWriter.println();
            return;
        }
        String[] strArr2 = new String[strArr.length - 1];
        System.arraycopy(strArr, 1, strArr2, 0, strArr2.length);
        this.mWifiStateMachine.dumpIpClient(fileDescriptor, printWriter, strArr2);
    }

    public boolean acquireWifiLock(IBinder iBinder, int i, String str, WorkSource workSource) {
        this.mLog.info("acquireWifiLock uid=% lockMode=%").c(Binder.getCallingUid()).c(i).flush();
        if (this.mWifiLockManager.acquireWifiLock(i, str, iBinder, workSource)) {
            return true;
        }
        return false;
    }

    public void updateWifiLockWorkSource(IBinder iBinder, WorkSource workSource) {
        this.mLog.info("updateWifiLockWorkSource uid=%").c(Binder.getCallingUid()).flush();
        this.mWifiLockManager.updateWifiLockWorkSource(iBinder, workSource);
    }

    public boolean releaseWifiLock(IBinder iBinder) {
        this.mLog.info("releaseWifiLock uid=%").c(Binder.getCallingUid()).flush();
        if (this.mWifiLockManager.releaseWifiLock(iBinder)) {
            return true;
        }
        return false;
    }

    public void initializeMulticastFiltering() {
        enforceMulticastChangePermission();
        this.mLog.info("initializeMulticastFiltering uid=%").c(Binder.getCallingUid()).flush();
        this.mWifiMulticastLockManager.initializeFiltering();
    }

    public void acquireMulticastLock(IBinder iBinder, String str) {
        enforceMulticastChangePermission();
        this.mLog.info("acquireMulticastLock uid=%").c(Binder.getCallingUid()).flush();
        this.mWifiMulticastLockManager.acquireLock(iBinder, str);
    }

    public void releaseMulticastLock() {
        enforceMulticastChangePermission();
        this.mLog.info("releaseMulticastLock uid=%").c(Binder.getCallingUid()).flush();
        this.mWifiMulticastLockManager.releaseLock();
    }

    public boolean isMulticastEnabled() {
        enforceAccessPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("isMulticastEnabled uid=%").c(Binder.getCallingUid()).flush();
        }
        return this.mWifiMulticastLockManager.isMulticastEnabled();
    }

    public void enableVerboseLogging(int i) {
        enforceAccessPermission();
        enforceNetworkSettingsPermission();
        this.mLog.info("enableVerboseLogging uid=% verbose=%").c(Binder.getCallingUid()).c(i).flush();
        this.mFacade.setIntegerSetting(this.mContext, "wifi_verbose_logging_enabled", i);
        enableVerboseLoggingInternal(i);
        if (this.mWifiInjector.getPropertyService().get("ro.vendor.mtklog_internal", "").equals("1")) {
            this.mWifiInjector.getPropertyService().set("persist.vendor.logmuch", i > 0 ? "false" : "true");
        }
    }

    void enableVerboseLoggingInternal(int i) {
        this.mVerboseLoggingEnabled = i > 0;
        this.mWifiStateMachine.enableVerboseLogging(i);
        this.mWifiLockManager.enableVerboseLogging(i);
        this.mWifiMulticastLockManager.enableVerboseLogging(i);
        this.mWifiInjector.enableVerboseLogging(i);
        MtkEapSimUtility.enableVerboseLogging(i);
    }

    public int getVerboseLoggingLevel() {
        enforceAccessPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("getVerboseLoggingLevel uid=%").c(Binder.getCallingUid()).flush();
        }
        return this.mFacade.getIntegerSetting(this.mContext, "wifi_verbose_logging_enabled", 0);
    }

    public void factoryReset(String str) {
        List<WifiConfiguration> listSyncGetConfiguredNetworks;
        enforceConnectivityInternalPermission();
        if (enforceChangePermission(str) != 0) {
            return;
        }
        this.mLog.info("factoryReset uid=%").c(Binder.getCallingUid()).flush();
        if (this.mUserManager.hasUserRestriction("no_network_reset")) {
            return;
        }
        if (!this.mUserManager.hasUserRestriction("no_config_tethering")) {
            stopSoftApInternal();
        }
        if (!this.mUserManager.hasUserRestriction("no_config_wifi") && this.mWifiStateMachineChannel != null && (listSyncGetConfiguredNetworks = this.mWifiStateMachine.syncGetConfiguredNetworks(Binder.getCallingUid(), this.mWifiStateMachineChannel)) != null) {
            Iterator<WifiConfiguration> it = listSyncGetConfiguredNetworks.iterator();
            while (it.hasNext()) {
                removeNetwork(it.next().networkId, str);
            }
        }
    }

    static boolean logAndReturnFalse(String str) {
        Log.d(TAG, str);
        return false;
    }

    public Network getCurrentNetwork() {
        enforceAccessPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("getCurrentNetwork uid=%").c(Binder.getCallingUid()).flush();
        }
        return this.mWifiStateMachine.getCurrentNetwork();
    }

    public static String toHexString(String str) {
        if (str == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('\'');
        sb.append(str);
        sb.append('\'');
        for (int i = 0; i < str.length(); i++) {
            sb.append(String.format(" %02x", Integer.valueOf(str.charAt(i) & 65535)));
        }
        return sb.toString();
    }

    public void enableWifiConnectivityManager(boolean z) {
        enforceConnectivityInternalPermission();
        this.mLog.info("enableWifiConnectivityManager uid=% enabled=%").c(Binder.getCallingUid()).c(z).flush();
        this.mWifiStateMachine.enableWifiConnectivityManager(z);
    }

    public byte[] retrieveBackupData() {
        enforceNetworkSettingsPermission();
        this.mLog.info("retrieveBackupData uid=%").c(Binder.getCallingUid()).flush();
        if (this.mWifiStateMachineChannel == null) {
            Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
            return null;
        }
        Slog.d(TAG, "Retrieving backup data");
        byte[] bArrRetrieveBackupDataFromConfigurations = this.mWifiBackupRestore.retrieveBackupDataFromConfigurations(this.mWifiStateMachine.syncGetPrivilegedConfiguredNetwork(this.mWifiStateMachineChannel));
        Slog.d(TAG, "Retrieved backup data");
        return bArrRetrieveBackupDataFromConfigurations;
    }

    private void restoreNetworks(List<WifiConfiguration> list) {
        if (list == null) {
            Slog.e(TAG, "Backup data parse failed");
            return;
        }
        for (WifiConfiguration wifiConfiguration : list) {
            int iSyncAddOrUpdateNetwork = this.mWifiStateMachine.syncAddOrUpdateNetwork(this.mWifiStateMachineChannel, wifiConfiguration);
            if (iSyncAddOrUpdateNetwork == -1) {
                Slog.e(TAG, "Restore network failed: " + wifiConfiguration.configKey());
            } else {
                this.mWifiStateMachine.syncEnableNetwork(this.mWifiStateMachineChannel, iSyncAddOrUpdateNetwork, false);
            }
        }
    }

    public void restoreBackupData(byte[] bArr) {
        enforceNetworkSettingsPermission();
        this.mLog.info("restoreBackupData uid=%").c(Binder.getCallingUid()).flush();
        if (this.mWifiStateMachineChannel == null) {
            Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
            return;
        }
        Slog.d(TAG, "Restoring backup data");
        restoreNetworks(this.mWifiBackupRestore.retrieveConfigurationsFromBackupData(bArr));
        Slog.d(TAG, "Restored backup data");
    }

    public void restoreSupplicantBackupData(byte[] bArr, byte[] bArr2) {
        enforceNetworkSettingsPermission();
        this.mLog.trace("restoreSupplicantBackupData uid=%").c(Binder.getCallingUid()).flush();
        if (this.mWifiStateMachineChannel == null) {
            Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
            return;
        }
        Slog.d(TAG, "Restoring supplicant backup data");
        restoreNetworks(this.mWifiBackupRestore.retrieveConfigurationsFromSupplicantBackupData(bArr, bArr2));
        Slog.d(TAG, "Restored supplicant backup data");
    }

    public void setPowerSavingMode(boolean z) {
        enforceAccessPermission();
        this.mWifiStateMachine.setPowerSavingMode(z);
    }

    public void startSubscriptionProvisioning(OsuProvider osuProvider, IProvisioningCallback iProvisioningCallback) {
        if (osuProvider == null) {
            throw new IllegalArgumentException("Provider must not be null");
        }
        if (iProvisioningCallback == null) {
            throw new IllegalArgumentException("Callback must not be null");
        }
        enforceNetworkSettingsPermission();
        if (!this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi.passpoint")) {
            throw new UnsupportedOperationException("Passpoint not enabled");
        }
        int callingUid = Binder.getCallingUid();
        this.mLog.trace("startSubscriptionProvisioning uid=%").c(callingUid).flush();
        if (this.mWifiStateMachine.syncStartSubscriptionProvisioning(callingUid, osuProvider, iProvisioningCallback, this.mWifiStateMachineChannel)) {
            this.mLog.trace("Subscription provisioning started with %").c(osuProvider.toString()).flush();
        }
    }
}
