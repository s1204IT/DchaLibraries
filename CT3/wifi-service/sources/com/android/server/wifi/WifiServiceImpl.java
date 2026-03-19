package com.android.server.wifi;

import android.R;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.DhcpResults;
import android.net.Network;
import android.net.NetworkScorerAppManager;
import android.net.NetworkUtils;
import android.net.Uri;
import android.net.wifi.IWifiManager;
import android.net.wifi.PPPOEInfo;
import android.net.wifi.PasspointManagementObjectDefinition;
import android.net.wifi.ScanResult;
import android.net.wifi.ScanSettings;
import android.net.wifi.WifiActivityEnergyInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConnectionStatistics;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiLinkLayerStats;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.WorkSource;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import com.android.internal.app.IBatteryStats;
import com.android.internal.util.AsyncChannel;
import com.android.server.am.BatteryStatsService;
import com.android.server.wifi.configparse.ConfigBuilder;
import com.mediatek.cta.CtaUtils;
import com.mediatek.server.wifi.WifiNvRamAgent;
import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.xml.sax.SAXException;

public class WifiServiceImpl extends IWifiManager.Stub {
    private static final String BOOT_DEFAULT_WIFI_COUNTRY_CODE = "ro.boot.wificountrycode";
    private static final boolean DBG = true;
    private static final int DISABLE_WIFI_FLIGHTMODE = 1;
    private static final int DISABLE_WIFI_SETTING = 0;
    private static final String NORMAL_BOOT_ACTION = "android.intent.action.normal.boot";
    private static final String TAG = "WifiService";
    private static final boolean VDBG = false;
    private final AppOpsManager mAppOps;
    private final IBatteryStats mBatteryStats;
    private final WifiCertManager mCertManager;
    private ClientHandler mClientHandler;
    private final Context mContext;
    private final WifiCountryCode mCountryCode;
    private int mFullHighPerfLocksAcquired;
    private int mFullHighPerfLocksReleased;
    private int mFullLocksAcquired;
    private int mFullLocksReleased;
    boolean mInIdleMode;
    private int mMulticastDisabled;
    private int mMulticastEnabled;
    private WifiNotificationController mNotificationController;
    private final PowerManager mPowerManager;
    private int mScanLocksAcquired;
    private int mScanLocksReleased;
    boolean mScanPending;
    final WifiSettingsStore mSettingsStore;
    private WifiTrafficPoller mTrafficPoller;
    private final UserManager mUserManager;
    private WifiController mWifiController;
    private final WifiMetrics mWifiMetrics;
    final WifiStateMachine mWifiStateMachine;
    private AsyncChannel mWifiStateMachineChannel;
    WifiStateMachineHandler mWifiStateMachineHandler;
    final LockList mLocks = new LockList(this, null);
    private final List<Multicaster> mMulticasters = new ArrayList();
    private int scanRequestCounter = 0;
    private boolean mIsSim1Ready = false;
    private boolean mIsSim2Ready = false;
    private boolean mIsFirstTime = true;
    private boolean mWifiIpoOff = false;
    private boolean mIsReceiverRegistered = false;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Slog.d(WifiServiceImpl.TAG, "onReceive, action:" + action);
            if (action.equals("android.intent.action.SCREEN_ON")) {
                WifiServiceImpl.this.mWifiController.sendMessage(155650);
                return;
            }
            if (action.equals("android.intent.action.USER_PRESENT")) {
                WifiServiceImpl.this.mWifiController.sendMessage(155660);
                return;
            }
            if (action.equals("android.intent.action.SCREEN_OFF")) {
                WifiServiceImpl.this.mWifiController.sendMessage(155651);
                return;
            }
            if (action.equals("android.intent.action.BATTERY_CHANGED")) {
                int pluggedType = intent.getIntExtra("plugged", 0);
                WifiServiceImpl.this.mWifiController.sendMessage(155652, pluggedType, 0, null);
                return;
            }
            if (action.equals("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED")) {
                int state = intent.getIntExtra("android.bluetooth.adapter.extra.CONNECTION_STATE", 0);
                WifiServiceImpl.this.mWifiStateMachine.sendBluetoothAdapterStateChange(state);
                return;
            }
            if (action.equals("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED")) {
                boolean emergencyMode = intent.getBooleanExtra("phoneinECMState", false);
                WifiServiceImpl.this.mWifiController.sendMessage(155649, emergencyMode ? 1 : 0, 0);
                return;
            }
            if (action.equals("android.intent.action.EMERGENCY_CALL_STATE_CHANGED")) {
                boolean inCall = intent.getBooleanExtra("phoneInEmergencyCall", false);
                WifiServiceImpl.this.mWifiController.sendMessage(155662, inCall ? 1 : 0, 0);
                return;
            }
            if (action.equals("android.os.action.DEVICE_IDLE_MODE_CHANGED")) {
                WifiServiceImpl.this.handleIdleModeChanged();
                return;
            }
            if (!action.equals("mediatek.intent.action.LOCATED_PLMN_CHANGED")) {
                return;
            }
            String plmn = (String) intent.getExtra("plmn");
            String iso = (String) intent.getExtra("iso");
            Log.d(WifiServiceImpl.TAG, "ACTION_LOCATED_PLMN_CHANGED: " + plmn + " iso =" + iso);
            if (iso == null || plmn == null) {
                return;
            }
            WifiServiceImpl.this.mCountryCode.setCountryCode(iso, false);
        }
    };
    private int mWifiOffListenerCount = 0;
    private final WifiInjector mWifiInjector = WifiInjector.getInstance();
    private final FrameworkFacade mFacade = new FrameworkFacade();

    private class ClientHandler extends Handler {
        ClientHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 69632:
                    if (msg.arg1 != 0) {
                        Slog.e(WifiServiceImpl.TAG, "Client connection failure, error=" + msg.arg1);
                    } else {
                        Slog.d(WifiServiceImpl.TAG, "New client listening to asynchronous messages");
                        WifiServiceImpl.this.mTrafficPoller.addClient(msg.replyTo);
                    }
                    break;
                case 69633:
                    AsyncChannel ac = new AsyncChannel();
                    ac.connect(WifiServiceImpl.this.mContext, this, msg.replyTo);
                    break;
                case 69636:
                    if (msg.arg1 == 2) {
                        Slog.d(WifiServiceImpl.TAG, "Send failed, client connection lost");
                    } else {
                        Slog.d(WifiServiceImpl.TAG, "Client connection lost with reason: " + msg.arg1);
                    }
                    WifiServiceImpl.this.mTrafficPoller.removeClient(msg.replyTo);
                    break;
                case 151553:
                case 151559:
                    WifiConfiguration config = (WifiConfiguration) msg.obj;
                    int networkId = msg.arg1;
                    if (msg.what == 151559) {
                        Slog.d("WiFiServiceImpl ", "SAVE nid=" + Integer.toString(networkId) + " uid=" + msg.sendingUid + " name=" + WifiServiceImpl.this.mContext.getPackageManager().getNameForUid(msg.sendingUid));
                    }
                    if (msg.what == 151553) {
                        Slog.d("WiFiServiceImpl ", "CONNECT  nid=" + Integer.toString(networkId) + " uid=" + msg.sendingUid + " name=" + WifiServiceImpl.this.mContext.getPackageManager().getNameForUid(msg.sendingUid));
                    }
                    if (config != null && WifiServiceImpl.isValid(config)) {
                        Slog.d(WifiServiceImpl.TAG, "Connect with config" + config);
                        WifiServiceImpl.this.mWifiStateMachine.sendMessage(Message.obtain(msg));
                    } else if (config == null && networkId != -1) {
                        Slog.d(WifiServiceImpl.TAG, "Connect with networkId" + networkId);
                        WifiServiceImpl.this.mWifiStateMachine.sendMessage(Message.obtain(msg));
                    } else {
                        Slog.e(WifiServiceImpl.TAG, "ClientHandler.handleMessage ignoring invalid msg=" + msg);
                        if (msg.what != 151553) {
                            replyFailed(msg, 151560, 8);
                        } else {
                            replyFailed(msg, 151554, 8);
                        }
                    }
                    break;
                case 151556:
                    WifiServiceImpl.this.mWifiStateMachine.sendMessage(Message.obtain(msg));
                    break;
                case 151562:
                case 151566:
                case 151569:
                case 151572:
                case 151575:
                case 151578:
                case 151612:
                    WifiServiceImpl.this.mWifiStateMachine.sendMessage(Message.obtain(msg));
                    break;
                default:
                    Slog.d(WifiServiceImpl.TAG, "ClientHandler.handleMessage ignoring msg=" + msg);
                    break;
            }
        }

        private void replyFailed(Message msg, int what, int why) {
            Message reply = Message.obtain();
            reply.what = what;
            reply.arg1 = why;
            try {
                msg.replyTo.send(reply);
            } catch (RemoteException e) {
            }
        }
    }

    private class WifiStateMachineHandler extends Handler {
        private AsyncChannel mWsmChannel;

        WifiStateMachineHandler(Looper looper) {
            super(looper);
            this.mWsmChannel = new AsyncChannel();
            this.mWsmChannel.connect(WifiServiceImpl.this.mContext, this, WifiServiceImpl.this.mWifiStateMachine.getHandler());
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 69632:
                    if (msg.arg1 == 0) {
                        WifiServiceImpl.this.mWifiStateMachineChannel = this.mWsmChannel;
                    } else {
                        Slog.e(WifiServiceImpl.TAG, "WifiStateMachine connection failure, error=" + msg.arg1);
                        WifiServiceImpl.this.mWifiStateMachineChannel = null;
                    }
                    break;
                case 69636:
                    Slog.e(WifiServiceImpl.TAG, "WifiStateMachine channel lost, msg.arg1 =" + msg.arg1);
                    WifiServiceImpl.this.mWifiStateMachineChannel = null;
                    this.mWsmChannel.connect(WifiServiceImpl.this.mContext, this, WifiServiceImpl.this.mWifiStateMachine.getHandler());
                    break;
                default:
                    Slog.d(WifiServiceImpl.TAG, "WifiStateMachineHandler.handleMessage ignoring msg=" + msg);
                    break;
            }
        }
    }

    public WifiServiceImpl(Context context) {
        this.mContext = context;
        HandlerThread wifiThread = new HandlerThread(TAG);
        wifiThread.start();
        this.mWifiMetrics = this.mWifiInjector.getWifiMetrics();
        this.mTrafficPoller = new WifiTrafficPoller(this.mContext, wifiThread.getLooper(), WifiNative.getWlanNativeInterface().getInterfaceName());
        this.mUserManager = UserManager.get(this.mContext);
        HandlerThread wifiStateMachineThread = new HandlerThread("WifiStateMachine");
        wifiStateMachineThread.start();
        this.mCountryCode = new WifiCountryCode(WifiNative.getWlanNativeInterface(), SystemProperties.get(BOOT_DEFAULT_WIFI_COUNTRY_CODE), this.mFacade.getStringSetting(this.mContext, "wifi_country_code"), this.mContext.getResources().getBoolean(R.^attr-private.backgroundRequestDetail));
        this.mWifiStateMachine = new WifiStateMachine(this.mContext, this.mFacade, wifiStateMachineThread.getLooper(), this.mUserManager, this.mWifiInjector, new BackupManagerProxy(), this.mCountryCode);
        this.mSettingsStore = new WifiSettingsStore(this.mContext);
        this.mWifiStateMachine.enableRssiPolling(true);
        this.mBatteryStats = BatteryStatsService.getService();
        this.mPowerManager = (PowerManager) context.getSystemService(PowerManager.class);
        this.mAppOps = (AppOpsManager) context.getSystemService("appops");
        this.mCertManager = new WifiCertManager(this.mContext);
        this.mNotificationController = new WifiNotificationController(this.mContext, wifiThread.getLooper(), this.mWifiStateMachine, this.mFacade, null);
        this.mClientHandler = new ClientHandler(wifiThread.getLooper());
        this.mWifiStateMachineHandler = new WifiStateMachineHandler(wifiThread.getLooper());
        this.mWifiController = new WifiController(this.mContext, this.mWifiStateMachine, this.mSettingsStore, this.mLocks, wifiThread.getLooper(), this.mFacade);
        initializeExtra();
    }

    public void checkAndStartWifi() {
        boolean isAlarmBoot = false;
        this.mWifiStateMachine.autoConnectInit();
        Slog.d(TAG, "mIsFirstTime: " + this.mIsFirstTime);
        if (this.mIsFirstTime) {
            Slog.d(TAG, "mWifiController.start");
            this.mWifiController.start();
            this.mIsFirstTime = false;
        }
        String bootReason = SystemProperties.get("sys.boot.reason");
        if (bootReason != null && bootReason.equals("1")) {
            isAlarmBoot = true;
        }
        if (isAlarmBoot) {
            Slog.i(TAG, "isAlarmBoot = true don't start wifi");
            this.mContext.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Slog.i(WifiServiceImpl.TAG, "receive NORMAL_BOOT_ACTION for alarm boot");
                    WifiServiceImpl.this.mContext.unregisterReceiver(this);
                    WifiServiceImpl.this.checkAndStartWifi();
                }
            }, new IntentFilter(NORMAL_BOOT_ACTION));
            return;
        }
        boolean wifiEnabled = this.mSettingsStore.isWifiToggleEnabled();
        Slog.i(TAG, "WifiService starting up with Wi-Fi " + (wifiEnabled ? "enabled" : "disabled"));
        registerForScanModeChange();
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean isAirplaneModeOn = intent.getBooleanExtra("state", false);
                Slog.i(WifiServiceImpl.TAG, "ACTION_AIRPLANE_MODE_CHANGED isAirplaneModeOn=" + isAirplaneModeOn);
                if (WifiServiceImpl.this.mSettingsStore.handleAirplaneModeToggled()) {
                    if (isAirplaneModeOn) {
                        if (WifiServiceImpl.this.mWifiOffListenerCount > 0) {
                            Slog.d(WifiServiceImpl.TAG, "mWifiOffListenerCount: " + WifiServiceImpl.this.mWifiOffListenerCount);
                            WifiServiceImpl.this.reportWifiOff(1);
                            Slog.d(WifiServiceImpl.TAG, "Let callback to call wifi off");
                            return;
                        }
                        Slog.d(WifiServiceImpl.TAG, "mWifiOffListenerCount < 0");
                    }
                    WifiServiceImpl.this.mWifiController.sendMessage(155657);
                }
                if (!WifiServiceImpl.this.mSettingsStore.isAirplaneModeOn()) {
                    return;
                }
                Log.d(WifiServiceImpl.TAG, "resetting country code because Airplane mode is ON");
                WifiServiceImpl.this.mCountryCode.airplaneModeEnabled();
            }
        }, new IntentFilter("android.intent.action.AIRPLANE_MODE"));
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String state = intent.getStringExtra("ss");
                int simSlot = intent.getIntExtra("slot", -1);
                Log.d(WifiServiceImpl.TAG, "onReceive ACTION_SIM_STATE_CHANGED iccState: " + state + ", simSlot: " + simSlot);
                if ("ABSENT".equals(state)) {
                    if (simSlot == 0) {
                        WifiServiceImpl.this.mIsSim1Ready = false;
                    } else if (1 == simSlot) {
                        WifiServiceImpl.this.mIsSim2Ready = false;
                    }
                    Log.d(WifiServiceImpl.TAG, "resetting networks because SIM was removed, simSlot: " + simSlot);
                    WifiServiceImpl.this.mWifiStateMachine.resetSimAuthNetworks(simSlot);
                    if (WifiServiceImpl.this.mIsSim1Ready || WifiServiceImpl.this.mIsSim2Ready) {
                        return;
                    }
                    Log.d(WifiServiceImpl.TAG, "All sim card is absent, resetting country code because SIM is removed");
                    WifiServiceImpl.this.mCountryCode.simCardRemoved();
                    return;
                }
                if (!"LOADED".equals(state)) {
                    return;
                }
                if (simSlot == 0) {
                    WifiServiceImpl.this.mIsSim1Ready = true;
                } else {
                    if (1 != simSlot) {
                        return;
                    }
                    WifiServiceImpl.this.mIsSim2Ready = true;
                }
            }
        }, new IntentFilter("android.intent.action.SIM_STATE_CHANGED"));
        registerForBroadcasts();
        registerForPackageOrUserRemoval();
        this.mInIdleMode = this.mPowerManager.isDeviceIdleMode();
        if (wifiEnabled) {
            setWifiEnabled(wifiEnabled);
        }
    }

    public void handleUserSwitch(int userId) {
        this.mWifiStateMachine.handleUserSwitch(userId);
    }

    public boolean pingSupplicant() {
        enforceAccessPermission();
        if (this.mWifiStateMachineChannel != null) {
            return this.mWifiStateMachine.syncPingSupplicant(this.mWifiStateMachineChannel);
        }
        Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
        return false;
    }

    public void startScan(ScanSettings settings, WorkSource workSource) {
        enforceChangePermission();
        synchronized (this) {
            if (this.mInIdleMode) {
                long callingIdentity = Binder.clearCallingIdentity();
                try {
                    this.mWifiStateMachine.sendScanResultsAvailableBroadcast(false);
                    Binder.restoreCallingIdentity(callingIdentity);
                    this.mScanPending = true;
                    return;
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(callingIdentity);
                    throw th;
                }
            }
            if (settings != null) {
                ScanSettings settings2 = new ScanSettings(settings);
                if (!settings2.isValid()) {
                    Slog.e(TAG, "invalid scan setting");
                    return;
                }
                settings = settings2;
            }
            if (workSource != null) {
                enforceWorkSourcePermission();
                workSource.clearNames();
            }
            if (workSource == null && Binder.getCallingUid() >= 0) {
                workSource = new WorkSource(Binder.getCallingUid());
            }
            WifiStateMachine wifiStateMachine = this.mWifiStateMachine;
            int callingUid = Binder.getCallingUid();
            int i = this.scanRequestCounter;
            this.scanRequestCounter = i + 1;
            wifiStateMachine.startScan(callingUid, i, settings, workSource);
        }
    }

    public String getWpsNfcConfigurationToken(int netId) {
        enforceConnectivityInternalPermission();
        return this.mWifiStateMachine.syncGetWpsNfcConfigurationToken(netId);
    }

    void handleIdleModeChanged() {
        boolean doScan = false;
        synchronized (this) {
            boolean idle = this.mPowerManager.isDeviceIdleMode();
            if (this.mInIdleMode != idle) {
                this.mInIdleMode = idle;
                if (!idle && this.mScanPending) {
                    this.mScanPending = false;
                    doScan = true;
                }
            }
        }
        if (!doScan) {
            return;
        }
        startScan(null, null);
    }

    private void enforceAccessPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_WIFI_STATE", TAG);
    }

    private void enforceChangePermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CHANGE_WIFI_STATE", TAG);
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

    private void enforceTetheringRestriction() {
        UserManager um = UserManager.get(this.mContext);
        UserHandle userHandle = Binder.getCallingUserHandle();
        Slog.d(TAG, "setWifiApEnabled - calling userId: " + userHandle.getIdentifier());
        if (!um.hasUserRestriction("no_config_tethering", userHandle)) {
        } else {
            throw new SecurityException("DISALLOW_CONFIG_TETHERING is enabled for this user.");
        }
    }

    public synchronized boolean setWifiEnabled(boolean enable) {
        synchronized (this) {
            enforceChangePermission();
            Slog.d(TAG, "setWifiEnabled: " + enable + " pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            if (enable) {
                CtaUtils.enforceCheckPermission("com.mediatek.permission.CTA_ENABLE_WIFI", "Enable WiFi");
            }
            if (!enable) {
                if (this.mWifiOffListenerCount > 0) {
                    Slog.d(TAG, "mWifiOffListenerCount: " + this.mWifiOffListenerCount);
                    reportWifiOff(0);
                    Slog.d(TAG, "Let callback to call wifi off");
                    return true;
                }
                Slog.d(TAG, "mWifiOffListenerCount < 0");
            }
            long ident = Binder.clearCallingIdentity();
            try {
                if (!this.mSettingsStore.handleWifiToggled(enable)) {
                    return true;
                }
                Binder.restoreCallingIdentity(ident);
                this.mWifiController.obtainMessage(155656, this.mWifiIpoOff ? 1 : 0, Binder.getCallingUid()).sendToTarget();
                return true;
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    public synchronized boolean setWifiEnabledForIPO(boolean enable) {
        synchronized (this) {
            Slog.d(TAG, "setWifiEnabledForIPO:" + enable + ", pid:" + Binder.getCallingPid() + ", uid:" + Binder.getCallingUid());
            enforceChangePermission();
            if (enable) {
                this.mWifiIpoOff = false;
            } else {
                this.mWifiIpoOff = true;
                this.mSettingsStore.setCheckSavedStateAtBoot(false);
            }
            this.mWifiController.obtainMessage(155656, this.mWifiIpoOff ? 1 : 0, Binder.getCallingUid()).sendToTarget();
            if (enable) {
                if (!this.mIsReceiverRegistered) {
                    registerForBroadcasts();
                    this.mIsReceiverRegistered = true;
                }
            } else if (this.mIsReceiverRegistered) {
                this.mContext.unregisterReceiver(this.mReceiver);
                this.mIsReceiverRegistered = false;
            }
        }
        return true;
    }

    public int getWifiEnabledState() {
        enforceAccessPermission();
        return this.mWifiStateMachine.syncGetWifiState();
    }

    public void setWifiApEnabled(WifiConfiguration wifiConfig, boolean enabled) {
        Slog.d(TAG, "setWifiApEnabled: " + enabled + " pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
        enforceChangePermission();
        ConnectivityManager.enforceTetherChangePermission(this.mContext);
        enforceTetheringRestriction();
        Slog.d(TAG, "setWifiApEnabled - passed the config_tethering check");
        if (Binder.getCallingUserHandle().getIdentifier() != 0) {
            Slog.e(TAG, "Only the device owner can enable wifi tethering");
        } else if (wifiConfig == null || isValid(wifiConfig)) {
            this.mWifiController.obtainMessage(155658, enabled ? 1 : 0, 0, wifiConfig).sendToTarget();
        } else {
            Slog.e(TAG, "Invalid WifiConfiguration");
        }
    }

    public int getWifiApEnabledState() {
        enforceAccessPermission();
        return this.mWifiStateMachine.syncGetWifiApState();
    }

    public WifiConfiguration getWifiApConfiguration() {
        enforceAccessPermission();
        enforceTetheringRestriction();
        if (Binder.getCallingUserHandle().getIdentifier() != 0) {
            Slog.e(TAG, "Only the device owner can retrieve the ap config");
            return null;
        }
        return this.mWifiStateMachine.syncGetWifiApConfiguration();
    }

    public WifiConfiguration buildWifiConfig(String uriString, String mimeType, byte[] data) {
        if (mimeType.equals(ConfigBuilder.WifiConfigType)) {
            try {
                return ConfigBuilder.buildConfig(uriString, data, this.mContext);
            } catch (IOException | GeneralSecurityException | SAXException e) {
                Log.e(TAG, "Failed to parse wi-fi configuration: " + e);
                return null;
            }
        }
        Log.i(TAG, "Unknown wi-fi config type: " + mimeType);
        return null;
    }

    public void setWifiApConfiguration(WifiConfiguration wifiConfig) {
        Slog.d(TAG, "setWifiApConfiguration: " + wifiConfig);
        enforceChangePermission();
        enforceTetheringRestriction();
        if (Binder.getCallingUserHandle().getIdentifier() != 0) {
            Slog.e(TAG, "Only the device owner can set the ap config");
        } else {
            if (wifiConfig == null) {
                return;
            }
            if (isValid(wifiConfig)) {
                this.mWifiStateMachine.setWifiApConfiguration(wifiConfig);
            } else {
                Slog.e(TAG, "Invalid WifiConfiguration");
            }
        }
    }

    public boolean isScanAlwaysAvailable() {
        enforceAccessPermission();
        return this.mSettingsStore.isScanAlwaysAvailable();
    }

    public void disconnect() {
        Slog.d(TAG, "disconnect, pid:" + Binder.getCallingPid() + ", uid:" + Binder.getCallingUid());
        enforceChangePermission();
        this.mWifiStateMachine.disconnectCommand();
    }

    public void reconnect() {
        Slog.d(TAG, "reconnect, pid:" + Binder.getCallingPid() + ", uid:" + Binder.getCallingUid());
        enforceChangePermission();
        this.mWifiStateMachine.reconnectCommand();
    }

    public void reassociate() {
        Slog.d(TAG, "reassociate, pid:" + Binder.getCallingPid() + ", uid:" + Binder.getCallingUid());
        enforceChangePermission();
        this.mWifiStateMachine.reassociateCommand();
    }

    public int getSupportedFeatures() {
        enforceAccessPermission();
        if (this.mWifiStateMachineChannel != null) {
            return this.mWifiStateMachine.syncGetSupportedFeatures(this.mWifiStateMachineChannel);
        }
        Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
        return 0;
    }

    public void requestActivityInfo(ResultReceiver result) {
        Bundle bundle = new Bundle();
        bundle.putParcelable("controller_activity", reportActivityInfo());
        result.send(0, bundle);
    }

    public WifiActivityEnergyInfo reportActivityInfo() {
        long[] txTimePerLevel;
        enforceAccessPermission();
        if ((getSupportedFeatures() & 65536) == 0) {
            return null;
        }
        WifiActivityEnergyInfo wifiActivityEnergyInfo = null;
        if (this.mWifiStateMachineChannel != null) {
            WifiLinkLayerStats stats = this.mWifiStateMachine.syncGetLinkLayerStats(this.mWifiStateMachineChannel);
            if (stats != null) {
                long rxIdleCurrent = this.mContext.getResources().getInteger(R.integer.config_brightness_ramp_rate_fast);
                long rxCurrent = this.mContext.getResources().getInteger(R.integer.config_brightness_ramp_rate_slow);
                long txCurrent = this.mContext.getResources().getInteger(R.integer.config_burnInProtectionMaxHorizontalOffset);
                double voltage = ((double) this.mContext.getResources().getInteger(R.integer.config_burnInProtectionMaxRadius)) / 1000.0d;
                long rxIdleTime = (stats.on_time - stats.tx_time) - stats.rx_time;
                if (stats.tx_time_per_level != null) {
                    txTimePerLevel = new long[stats.tx_time_per_level.length];
                    for (int i = 0; i < txTimePerLevel.length; i++) {
                        txTimePerLevel[i] = stats.tx_time_per_level[i];
                    }
                } else {
                    txTimePerLevel = new long[0];
                }
                long energyUsed = (long) (((((long) stats.tx_time) * txCurrent) + (((long) stats.rx_time) * rxCurrent) + (rxIdleTime * rxIdleCurrent)) * voltage);
                if (rxIdleTime < 0 || stats.on_time < 0 || stats.tx_time < 0 || stats.rx_time < 0 || energyUsed < 0) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(" rxIdleCur=").append(rxIdleCurrent);
                    sb.append(" rxCur=").append(rxCurrent);
                    sb.append(" txCur=").append(txCurrent);
                    sb.append(" voltage=").append(voltage);
                    sb.append(" on_time=").append(stats.on_time);
                    sb.append(" tx_time=").append(stats.tx_time);
                    sb.append(" tx_time_per_level=").append(Arrays.toString(txTimePerLevel));
                    sb.append(" rx_time=").append(stats.rx_time);
                    sb.append(" rxIdleTime=").append(rxIdleTime);
                    sb.append(" energy=").append(energyUsed);
                    Log.d(TAG, " reportActivityInfo: " + sb.toString());
                }
                wifiActivityEnergyInfo = new WifiActivityEnergyInfo(SystemClock.elapsedRealtime(), 3, stats.tx_time, txTimePerLevel, stats.rx_time, rxIdleTime, energyUsed);
            }
            if (wifiActivityEnergyInfo != null && wifiActivityEnergyInfo.isValid()) {
                return wifiActivityEnergyInfo;
            }
            return null;
        }
        Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
        return null;
    }

    public List<WifiConfiguration> getConfiguredNetworks() {
        enforceAccessPermission();
        if (this.mWifiStateMachineChannel != null) {
            return this.mWifiStateMachine.syncGetConfiguredNetworks(Binder.getCallingUid(), this.mWifiStateMachineChannel);
        }
        Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
        return null;
    }

    public List<WifiConfiguration> getPrivilegedConfiguredNetworks() {
        enforceReadCredentialPermission();
        enforceAccessPermission();
        if (this.mWifiStateMachineChannel != null) {
            return this.mWifiStateMachine.syncGetPrivilegedConfiguredNetwork(this.mWifiStateMachineChannel);
        }
        Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
        return null;
    }

    public WifiConfiguration getMatchingWifiConfig(ScanResult scanResult) {
        enforceAccessPermission();
        return this.mWifiStateMachine.syncGetMatchingWifiConfig(scanResult, this.mWifiStateMachineChannel);
    }

    public int addOrUpdateNetwork(WifiConfiguration config) {
        Slog.d(TAG, "addOrUpdateNetwork, pid:" + Binder.getCallingPid() + ", uid:" + Binder.getCallingUid() + ", config:" + config);
        enforceChangePermission();
        if (!isValid(config) || !isValidPasspoint(config)) {
            Slog.e(TAG, "bad network configuration");
            return -1;
        }
        if (config.isPasspoint()) {
            config.allowedProtocols.set(1);
        }
        WifiEnterpriseConfig enterpriseConfig = config.enterpriseConfig;
        if (config.isPasspoint() && (enterpriseConfig.getEapMethod() == 1 || enterpriseConfig.getEapMethod() == 2)) {
            if (config.updateIdentifier != null) {
                enforceAccessPermission();
            } else if (SystemProperties.get("persist.wifi.hs20.test.mode").equals("1")) {
                Slog.d(TAG, "In HS20 test mode. do not need verifyCert");
            } else {
                try {
                    verifyCert(enterpriseConfig.getCaCertificate());
                } catch (IOException | GeneralSecurityException e) {
                    Slog.e(TAG, "Failed to verify certificate" + enterpriseConfig.getCaCertificate().getSubjectX500Principal() + ": " + e);
                    return -1;
                } catch (CertPathValidatorException cpve) {
                    Slog.e(TAG, "CA Cert " + enterpriseConfig.getCaCertificate().getSubjectX500Principal() + " untrusted: " + cpve.getMessage() + " certificate path: " + cpve.getCertPath());
                    return -1;
                }
            }
        }
        Slog.i("addOrUpdateNetwork", " uid = " + Integer.toString(Binder.getCallingUid()) + " SSID " + config.SSID + " nid=" + Integer.toString(config.networkId));
        if (config.networkId == -1) {
            config.creatorUid = Binder.getCallingUid();
        } else {
            config.lastUpdateUid = Binder.getCallingUid();
        }
        if (this.mWifiStateMachineChannel != null) {
            return this.mWifiStateMachine.syncAddOrUpdateNetwork(this.mWifiStateMachineChannel, config);
        }
        Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
        return -1;
    }

    public static void verifyCert(X509Certificate caCert) throws GeneralSecurityException, IOException {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        CertPathValidator validator = CertPathValidator.getInstance(CertPathValidator.getDefaultType());
        CertPath path = factory.generateCertPath(Arrays.asList(caCert));
        KeyStore ks = KeyStore.getInstance("AndroidCAStore");
        ks.load(null, null);
        PKIXParameters params = new PKIXParameters(ks);
        params.setRevocationEnabled(false);
        validator.validate(path, params);
    }

    public boolean removeNetwork(int netId) {
        Slog.d(TAG, "removeNetwork, pid:" + Binder.getCallingPid() + ", uid:" + Binder.getCallingUid() + ", netId:" + netId);
        enforceChangePermission();
        if (this.mWifiStateMachineChannel != null) {
            return this.mWifiStateMachine.syncRemoveNetwork(this.mWifiStateMachineChannel, netId);
        }
        Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
        return false;
    }

    public boolean enableNetwork(int netId, boolean disableOthers) {
        Slog.d(TAG, "enableNetwork, pid:" + Binder.getCallingPid() + ", uid:" + Binder.getCallingUid() + ", netId:" + netId + ", disableOthers:" + disableOthers);
        enforceChangePermission();
        if (this.mWifiStateMachineChannel != null) {
            return this.mWifiStateMachine.syncEnableNetwork(this.mWifiStateMachineChannel, netId, disableOthers);
        }
        Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
        return false;
    }

    public boolean disableNetwork(int netId) {
        Slog.d(TAG, "disableNetwork, pid:" + Binder.getCallingPid() + ", uid:" + Binder.getCallingUid() + ", netId:" + netId);
        enforceChangePermission();
        if (this.mWifiStateMachineChannel != null) {
            return this.mWifiStateMachine.syncDisableNetwork(this.mWifiStateMachineChannel, netId);
        }
        Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
        return false;
    }

    public WifiInfo getConnectionInfo() {
        enforceAccessPermission();
        return this.mWifiStateMachine.syncRequestConnectionInfo();
    }

    public List<ScanResult> getScanResults(String callingPackage) {
        enforceAccessPermission();
        int userId = UserHandle.getCallingUserId();
        int uid = Binder.getCallingUid();
        boolean canReadPeerMacAddresses = checkPeersMacAddress();
        boolean isActiveNetworkScorer = NetworkScorerAppManager.isCallerActiveScorer(this.mContext, uid);
        boolean hasInteractUsersFull = checkInteractAcrossUsersFull();
        long ident = Binder.clearCallingIdentity();
        if (!canReadPeerMacAddresses && !isActiveNetworkScorer) {
            try {
                if (!isLocationEnabled(callingPackage)) {
                    return new ArrayList();
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
        return (canReadPeerMacAddresses || isActiveNetworkScorer || checkCallerCanAccessScanResults(callingPackage, uid)) ? this.mAppOps.noteOp(10, uid, callingPackage) != 0 ? new ArrayList() : (isCurrentProfile(userId) || hasInteractUsersFull) ? this.mWifiStateMachine.syncGetScanResultsList() : new ArrayList() : new ArrayList();
    }

    public int addPasspointManagementObject(String mo) {
        return this.mWifiStateMachine.syncAddPasspointManagementObject(this.mWifiStateMachineChannel, mo);
    }

    public int modifyPasspointManagementObject(String fqdn, List<PasspointManagementObjectDefinition> mos) {
        return this.mWifiStateMachine.syncModifyPasspointManagementObject(this.mWifiStateMachineChannel, fqdn, mos);
    }

    public void queryPasspointIcon(long bssid, String fileName) {
        this.mWifiStateMachine.syncQueryPasspointIcon(this.mWifiStateMachineChannel, bssid, fileName);
    }

    public int matchProviderWithCurrentNetwork(String fqdn) {
        return this.mWifiStateMachine.matchProviderWithCurrentNetwork(this.mWifiStateMachineChannel, fqdn);
    }

    public void deauthenticateNetwork(long holdoff, boolean ess) {
        this.mWifiStateMachine.deauthenticateNetwork(this.mWifiStateMachineChannel, holdoff, ess);
    }

    private boolean isLocationEnabled(String callingPackage) {
        boolean legacyForegroundApp = !isMApp(this.mContext, callingPackage) ? isForegroundApp(callingPackage) : false;
        return legacyForegroundApp || Settings.Secure.getInt(this.mContext.getContentResolver(), "location_mode", 0) != 0;
    }

    private boolean checkInteractAcrossUsersFull() {
        return this.mContext.checkCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL") == 0;
    }

    private boolean checkPeersMacAddress() {
        return this.mContext.checkCallingOrSelfPermission("android.permission.PEERS_MAC_ADDRESS") == 0;
    }

    private boolean isCurrentProfile(int userId) {
        int currentUser = ActivityManager.getCurrentUser();
        if (userId == currentUser) {
            return true;
        }
        List<UserInfo> profiles = this.mUserManager.getProfiles(currentUser);
        for (UserInfo user : profiles) {
            if (userId == user.id) {
                return true;
            }
        }
        return false;
    }

    public boolean saveConfiguration() {
        Slog.d(TAG, "saveConfiguration, pid:" + Binder.getCallingPid() + ", uid:" + Binder.getCallingUid());
        enforceChangePermission();
        if (this.mWifiStateMachineChannel != null) {
            return this.mWifiStateMachine.syncSaveConfig(this.mWifiStateMachineChannel);
        }
        Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
        return false;
    }

    public void setCountryCode(String countryCode, boolean persist) {
        Slog.i(TAG, "WifiService trying to set country code to " + countryCode + " with persist set to " + persist);
        enforceConnectivityInternalPermission();
        long token = Binder.clearCallingIdentity();
        try {
            if (this.mCountryCode.setCountryCode(countryCode, persist) && persist) {
                this.mFacade.setStringSetting(this.mContext, "wifi_country_code", countryCode);
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public String getCountryCode() {
        enforceConnectivityInternalPermission();
        String country = this.mCountryCode.getCurrentCountryCode();
        return country;
    }

    public void setFrequencyBand(int band, boolean persist) {
        enforceChangePermission();
        if (isDualBandSupported()) {
            Slog.i(TAG, "WifiService trying to set frequency band to " + band + " with persist set to " + persist);
            long token = Binder.clearCallingIdentity();
            try {
                this.mWifiStateMachine.setFrequencyBand(band, persist);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
    }

    public int getFrequencyBand() {
        enforceAccessPermission();
        return this.mWifiStateMachine.getFrequencyBand();
    }

    public boolean isDualBandSupported() {
        return this.mContext.getResources().getBoolean(R.^attr-private.autofillDatasetPickerMaxWidth);
    }

    public DhcpInfo getDhcpInfo() {
        enforceAccessPermission();
        DhcpResults dhcpResults = this.mWifiStateMachine.syncGetDhcpResults();
        DhcpInfo info = new DhcpInfo();
        if (dhcpResults.ipAddress != null && (dhcpResults.ipAddress.getAddress() instanceof Inet4Address)) {
            info.ipAddress = NetworkUtils.inetAddressToInt((Inet4Address) dhcpResults.ipAddress.getAddress());
        }
        if (dhcpResults.gateway != null) {
            info.gateway = NetworkUtils.inetAddressToInt((Inet4Address) dhcpResults.gateway);
        }
        int dnsFound = 0;
        for (InetAddress dns : dhcpResults.dnsServers) {
            if (dns instanceof Inet4Address) {
                if (dnsFound == 0) {
                    info.dns1 = NetworkUtils.inetAddressToInt((Inet4Address) dns);
                } else {
                    info.dns2 = NetworkUtils.inetAddressToInt((Inet4Address) dns);
                }
                dnsFound++;
                if (dnsFound > 1) {
                    break;
                }
            }
        }
        Inet4Address serverAddress = dhcpResults.serverAddress;
        if (serverAddress != null) {
            info.serverAddress = NetworkUtils.inetAddressToInt(serverAddress);
        }
        info.leaseDuration = dhcpResults.leaseDuration;
        return info;
    }

    public void addToBlacklist(String bssid) {
        enforceChangePermission();
        this.mWifiStateMachine.addToBlacklist(bssid);
    }

    public void clearBlacklist() {
        enforceChangePermission();
        this.mWifiStateMachine.clearBlacklist();
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
        protected Integer doInBackground(TdlsTaskParams... params) throws Throwable {
            BufferedReader reader;
            TdlsTaskParams param = params[0];
            String remoteIpAddress = param.remoteIpAddress.trim();
            boolean enable = param.enable;
            String macAddress = null;
            BufferedReader reader2 = null;
            try {
                try {
                    reader = new BufferedReader(new FileReader("/proc/net/arp"));
                } catch (Throwable th) {
                    th = th;
                }
                try {
                    reader.readLine();
                    while (true) {
                        String line = reader.readLine();
                        if (line == null) {
                            break;
                        }
                        String[] tokens = line.split("[ ]+");
                        if (tokens.length >= 6) {
                            String ip = tokens[0];
                            String mac = tokens[3];
                            if (remoteIpAddress.equals(ip)) {
                                macAddress = mac;
                                break;
                            }
                        }
                    }
                    if (macAddress == null) {
                        Slog.w(WifiServiceImpl.TAG, "Did not find remoteAddress {" + remoteIpAddress + "} in /proc/net/arp");
                    } else {
                        WifiServiceImpl.this.enableTdlsWithMacAddress(macAddress, enable);
                    }
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                        }
                    }
                    reader2 = reader;
                } catch (FileNotFoundException e2) {
                    reader2 = reader;
                    Slog.e(WifiServiceImpl.TAG, "Could not open /proc/net/arp to lookup mac address");
                    if (reader2 != null) {
                        try {
                            reader2.close();
                        } catch (IOException e3) {
                        }
                    }
                } catch (IOException e4) {
                    reader2 = reader;
                    Slog.e(WifiServiceImpl.TAG, "Could not read /proc/net/arp to lookup mac address");
                    if (reader2 != null) {
                        try {
                            reader2.close();
                        } catch (IOException e5) {
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                    reader2 = reader;
                    if (reader2 != null) {
                        try {
                            reader2.close();
                        } catch (IOException e6) {
                        }
                    }
                    throw th;
                }
            } catch (FileNotFoundException e7) {
            } catch (IOException e8) {
            }
            return 0;
        }
    }

    public void enableTdls(String remoteAddress, boolean enable) {
        if (remoteAddress == null) {
            throw new IllegalArgumentException("remoteAddress cannot be null");
        }
        TdlsTaskParams params = new TdlsTaskParams();
        params.remoteIpAddress = remoteAddress;
        params.enable = enable;
        new TdlsTask().execute(params);
    }

    public void enableTdlsWithMacAddress(String remoteMacAddress, boolean enable) {
        if (remoteMacAddress == null) {
            throw new IllegalArgumentException("remoteMacAddress cannot be null");
        }
        this.mWifiStateMachine.enableTdls(remoteMacAddress, enable);
    }

    public Messenger getWifiServiceMessenger() {
        Slog.d(TAG, "getWifiServiceMessenger, pid:" + Binder.getCallingPid() + ", uid:" + Binder.getCallingUid());
        enforceAccessPermission();
        enforceChangePermission();
        return new Messenger(this.mClientHandler);
    }

    public void disableEphemeralNetwork(String SSID) {
        enforceAccessPermission();
        enforceChangePermission();
        this.mWifiStateMachine.disableEphemeralNetwork(SSID);
    }

    public String getConfigFile() {
        enforceAccessPermission();
        return this.mWifiStateMachine.getConfigFile();
    }

    private void registerForScanModeChange() {
        ContentObserver contentObserver = new ContentObserver(null) {
            @Override
            public void onChange(boolean selfChange) {
                WifiServiceImpl.this.mSettingsStore.handleWifiScanAlwaysAvailableToggled();
                WifiServiceImpl.this.mWifiController.sendMessage(155655);
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("wifi_scan_always_enabled"), false, contentObserver);
    }

    private void registerForBroadcasts() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        intentFilter.addAction("android.intent.action.USER_PRESENT");
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        intentFilter.addAction("android.intent.action.BATTERY_CHANGED");
        intentFilter.addAction("android.net.wifi.STATE_CHANGE");
        intentFilter.addAction("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED");
        intentFilter.addAction("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED");
        intentFilter.addAction("android.os.action.DEVICE_IDLE_MODE_CHANGED");
        intentFilter.addAction("mediatek.intent.action.LOCATED_PLMN_CHANGED");
        boolean trackEmergencyCallState = this.mContext.getResources().getBoolean(R.^attr-private.borderBottom);
        if (trackEmergencyCallState) {
            intentFilter.addAction("android.intent.action.EMERGENCY_CALL_STATE_CHANGED");
        }
        this.mContext.registerReceiver(this.mReceiver, intentFilter);
    }

    private void registerForPackageOrUserRemoval() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter.addAction("android.intent.action.USER_REMOVED");
        this.mContext.registerReceiverAsUser(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (!action.equals("android.intent.action.PACKAGE_REMOVED")) {
                    if (!action.equals("android.intent.action.USER_REMOVED")) {
                        return;
                    }
                    int userHandle = intent.getIntExtra("android.intent.extra.user_handle", 0);
                    WifiServiceImpl.this.mWifiStateMachine.removeUserConfigs(userHandle);
                    return;
                }
                if (intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                    return;
                }
                int uid = intent.getIntExtra("android.intent.extra.UID", -1);
                Uri uri = intent.getData();
                if (uid == -1 || uri == null) {
                    return;
                }
                String pkgName = uri.getSchemeSpecificPart();
                WifiServiceImpl.this.mWifiStateMachine.removeAppConfigs(pkgName, uid);
            }
        }, UserHandle.ALL, intentFilter, null, null);
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            pw.println("Permission Denial: can't dump WifiService from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            return;
        }
        if (args.length > 0 && WifiMetrics.PROTO_DUMP_ARG.equals(args[0])) {
            this.mWifiStateMachine.updateWifiMetrics();
            this.mWifiMetrics.dump(fd, pw, args);
            return;
        }
        if (args.length > 0 && "ipmanager".equals(args[0])) {
            String[] ipManagerArgs = new String[args.length - 1];
            System.arraycopy(args, 1, ipManagerArgs, 0, ipManagerArgs.length);
            this.mWifiStateMachine.dumpIpManager(fd, pw, ipManagerArgs);
            return;
        }
        pw.println("Wi-Fi is " + this.mWifiStateMachine.syncGetWifiStateByName());
        pw.println("Stay-awake conditions: " + Settings.Global.getInt(this.mContext.getContentResolver(), "stay_on_while_plugged_in", 0));
        pw.println("mMulticastEnabled " + this.mMulticastEnabled);
        pw.println("mMulticastDisabled " + this.mMulticastDisabled);
        pw.println("mInIdleMode " + this.mInIdleMode);
        pw.println("mScanPending " + this.mScanPending);
        this.mWifiController.dump(fd, pw, args);
        this.mSettingsStore.dump(fd, pw, args);
        this.mNotificationController.dump(fd, pw, args);
        this.mTrafficPoller.dump(fd, pw, args);
        pw.println("Latest scan results:");
        List<ScanResult> scanResults = this.mWifiStateMachine.syncGetScanResultsList();
        long nowMs = System.currentTimeMillis();
        if (scanResults != null && scanResults.size() != 0) {
            pw.println("    BSSID              Frequency  RSSI    Age      SSID                                 Flags");
            for (ScanResult r : scanResults) {
                long ageSec = 0;
                long ageMilli = 0;
                if (nowMs > r.seen && r.seen > 0) {
                    ageSec = (nowMs - r.seen) / 1000;
                    ageMilli = (nowMs - r.seen) % 1000;
                }
                String candidate = r.isAutoJoinCandidate > 0 ? "+" : " ";
                Object[] objArr = new Object[8];
                objArr[0] = r.BSSID;
                objArr[1] = Integer.valueOf(r.frequency);
                objArr[2] = Integer.valueOf(r.level);
                objArr[3] = Long.valueOf(ageSec);
                objArr[4] = Long.valueOf(ageMilli);
                objArr[5] = candidate;
                objArr[6] = r.SSID == null ? "" : r.SSID;
                objArr[7] = r.capabilities;
                pw.printf("  %17s  %9d  %5d  %3d.%03d%s   %-32s  %s\n", objArr);
            }
        }
        pw.println();
        pw.println("Locks acquired: " + this.mFullLocksAcquired + " full, " + this.mFullHighPerfLocksAcquired + " full high perf, " + this.mScanLocksAcquired + " scan");
        pw.println("Locks released: " + this.mFullLocksReleased + " full, " + this.mFullHighPerfLocksReleased + " full high perf, " + this.mScanLocksReleased + " scan");
        pw.println();
        pw.println("Locks held:");
        this.mLocks.dump(pw);
        pw.println("Multicast Locks held:");
        for (Multicaster l : this.mMulticasters) {
            pw.print("    ");
            pw.println(l);
        }
        pw.println();
        this.mWifiStateMachine.dump(fd, pw, args);
        pw.println();
    }

    private class WifiLock extends DeathRecipient {
        int mMode;
        WorkSource mWorkSource;

        WifiLock(int lockMode, String tag, IBinder binder, WorkSource ws) {
            super(tag, binder);
            this.mMode = lockMode;
            this.mWorkSource = ws;
        }

        @Override
        public void binderDied() {
            synchronized (WifiServiceImpl.this.mLocks) {
                WifiServiceImpl.this.releaseWifiLockLocked(this.mBinder);
            }
        }

        public String toString() {
            return "WifiLock{" + this.mTag + " type=" + this.mMode + " uid=" + this.mUid + "}";
        }
    }

    public class LockList {
        private List<WifiLock> mList;

        LockList(WifiServiceImpl this$0, LockList lockList) {
            this();
        }

        private LockList() {
            this.mList = new ArrayList();
        }

        synchronized boolean hasLocks() {
            return !this.mList.isEmpty();
        }

        synchronized int getStrongestLockMode() {
            if (this.mList.isEmpty()) {
                return 1;
            }
            if (WifiServiceImpl.this.mFullHighPerfLocksAcquired > WifiServiceImpl.this.mFullHighPerfLocksReleased) {
                return 3;
            }
            return WifiServiceImpl.this.mFullLocksAcquired > WifiServiceImpl.this.mFullLocksReleased ? 1 : 2;
        }

        synchronized void updateWorkSource(WorkSource ws) {
            for (int i = 0; i < WifiServiceImpl.this.mLocks.mList.size(); i++) {
                ws.add(WifiServiceImpl.this.mLocks.mList.get(i).mWorkSource);
            }
        }

        private void addLock(WifiLock lock) {
            if (findLockByBinder(lock.mBinder) >= 0) {
                return;
            }
            this.mList.add(lock);
        }

        private WifiLock removeLock(IBinder binder) {
            int index = findLockByBinder(binder);
            if (index >= 0) {
                WifiLock ret = this.mList.remove(index);
                ret.unlinkDeathRecipient();
                return ret;
            }
            return null;
        }

        private int findLockByBinder(IBinder binder) {
            int size = this.mList.size();
            for (int i = size - 1; i >= 0; i--) {
                if (this.mList.get(i).mBinder == binder) {
                    return i;
                }
            }
            return -1;
        }

        private void dump(PrintWriter pw) {
            for (WifiLock l : this.mList) {
                pw.print("    ");
                pw.println(l);
            }
        }
    }

    void enforceWakeSourcePermission(int uid, int pid) {
        if (uid == Process.myUid()) {
            return;
        }
        this.mContext.enforcePermission("android.permission.UPDATE_DEVICE_STATS", pid, uid, null);
    }

    public boolean acquireWifiLock(IBinder binder, int lockMode, String tag, WorkSource ws) {
        boolean zAcquireWifiLockLocked;
        Slog.d(TAG, "acquireWifiLock, pid:" + Binder.getCallingPid() + ", uid:" + Binder.getCallingUid());
        this.mContext.enforceCallingOrSelfPermission("android.permission.WAKE_LOCK", null);
        if (lockMode != 1 && lockMode != 2 && lockMode != 3) {
            Slog.e(TAG, "Illegal argument, lockMode= " + lockMode);
            throw new IllegalArgumentException("lockMode=" + lockMode);
        }
        if (ws != null && ws.size() == 0) {
            ws = null;
        }
        if (ws != null) {
            enforceWakeSourcePermission(Binder.getCallingUid(), Binder.getCallingPid());
        }
        if (ws == null) {
            ws = new WorkSource(Binder.getCallingUid());
        }
        WifiLock wifiLock = new WifiLock(lockMode, tag, binder, ws);
        synchronized (this.mLocks) {
            zAcquireWifiLockLocked = acquireWifiLockLocked(wifiLock);
        }
        return zAcquireWifiLockLocked;
    }

    private void noteAcquireWifiLock(WifiLock wifiLock) throws RemoteException {
        switch (wifiLock.mMode) {
            case 1:
            case 2:
            case 3:
                this.mBatteryStats.noteFullWifiLockAcquiredFromSource(wifiLock.mWorkSource);
                break;
        }
    }

    private void noteReleaseWifiLock(WifiLock wifiLock) throws RemoteException {
        switch (wifiLock.mMode) {
            case 1:
            case 2:
            case 3:
                this.mBatteryStats.noteFullWifiLockReleasedFromSource(wifiLock.mWorkSource);
                break;
        }
    }

    private boolean acquireWifiLockLocked(WifiLock wifiLock) {
        Slog.d(TAG, "acquireWifiLockLocked: " + wifiLock);
        this.mLocks.addLock(wifiLock);
        long ident = Binder.clearCallingIdentity();
        try {
            noteAcquireWifiLock(wifiLock);
            switch (wifiLock.mMode) {
                case 1:
                    this.mFullLocksAcquired++;
                    break;
                case 2:
                    this.mScanLocksAcquired++;
                    break;
                case 3:
                    this.mFullHighPerfLocksAcquired++;
                    break;
            }
            this.mWifiController.sendMessage(155654);
            return true;
        } catch (RemoteException e) {
            return false;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void updateWifiLockWorkSource(IBinder lock, WorkSource ws) {
        int uid = Binder.getCallingUid();
        int pid = Binder.getCallingPid();
        if (ws != null && ws.size() == 0) {
            ws = null;
        }
        if (ws != null) {
            enforceWakeSourcePermission(uid, pid);
        }
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLocks) {
                int index = this.mLocks.findLockByBinder(lock);
                if (index < 0) {
                    throw new IllegalArgumentException("Wifi lock not active");
                }
                WifiLock wl = (WifiLock) this.mLocks.mList.get(index);
                noteReleaseWifiLock(wl);
                wl.mWorkSource = ws != null ? new WorkSource(ws) : new WorkSource(uid);
                noteAcquireWifiLock(wl);
            }
        } catch (RemoteException e) {
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public boolean releaseWifiLock(IBinder lock) {
        boolean zReleaseWifiLockLocked;
        Slog.d(TAG, "releaseWifiLock, pid:" + Binder.getCallingPid() + ", uid:" + Binder.getCallingUid());
        this.mContext.enforceCallingOrSelfPermission("android.permission.WAKE_LOCK", null);
        synchronized (this.mLocks) {
            zReleaseWifiLockLocked = releaseWifiLockLocked(lock);
        }
        return zReleaseWifiLockLocked;
    }

    private boolean releaseWifiLockLocked(IBinder lock) {
        WifiLock wifiLock = this.mLocks.removeLock(lock);
        Slog.d(TAG, "releaseWifiLockLocked: " + wifiLock);
        boolean hadLock = wifiLock != null;
        long ident = Binder.clearCallingIdentity();
        if (hadLock) {
            try {
                noteReleaseWifiLock(wifiLock);
                switch (wifiLock.mMode) {
                    case 1:
                        this.mFullLocksReleased++;
                        break;
                    case 2:
                        this.mScanLocksReleased++;
                        break;
                    case 3:
                        this.mFullHighPerfLocksReleased++;
                        break;
                }
                this.mWifiController.sendMessage(155654);
            } catch (RemoteException e) {
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
        return hadLock;
    }

    private abstract class DeathRecipient implements IBinder.DeathRecipient {
        IBinder mBinder;
        String mTag;
        int mUid = Binder.getCallingUid();

        DeathRecipient(String tag, IBinder binder) {
            this.mTag = tag;
            this.mBinder = binder;
            try {
                this.mBinder.linkToDeath(this, 0);
            } catch (RemoteException e) {
                binderDied();
            }
        }

        void unlinkDeathRecipient() {
            this.mBinder.unlinkToDeath(this, 0);
        }

        public int getUid() {
            return this.mUid;
        }
    }

    private class Multicaster extends DeathRecipient {
        Multicaster(String tag, IBinder binder) {
            super(tag, binder);
        }

        @Override
        public void binderDied() {
            Slog.e(WifiServiceImpl.TAG, "Multicaster binderDied");
            synchronized (WifiServiceImpl.this.mMulticasters) {
                int i = WifiServiceImpl.this.mMulticasters.indexOf(this);
                if (i != -1) {
                    WifiServiceImpl.this.removeMulticasterLocked(i, this.mUid);
                }
            }
        }

        public String toString() {
            return "Multicaster{" + this.mTag + " uid=" + this.mUid + "}";
        }
    }

    public void initializeMulticastFiltering() {
        enforceMulticastChangePermission();
        synchronized (this.mMulticasters) {
            if (this.mMulticasters.size() != 0) {
                return;
            }
            this.mWifiStateMachine.startFilteringMulticastPackets();
        }
    }

    public void acquireMulticastLock(IBinder binder, String tag) {
        enforceMulticastChangePermission();
        synchronized (this.mMulticasters) {
            this.mMulticastEnabled++;
            this.mMulticasters.add(new Multicaster(tag, binder));
            this.mWifiStateMachine.stopFilteringMulticastPackets();
        }
        int uid = Binder.getCallingUid();
        long ident = Binder.clearCallingIdentity();
        try {
            this.mBatteryStats.noteWifiMulticastEnabled(uid);
        } catch (RemoteException e) {
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void releaseMulticastLock() {
        Slog.d(TAG, "releaseMulticastLock, pid:" + Binder.getCallingPid() + ", uid:" + Binder.getCallingUid());
        enforceMulticastChangePermission();
        int uid = Binder.getCallingUid();
        synchronized (this.mMulticasters) {
            this.mMulticastDisabled++;
            int size = this.mMulticasters.size();
            for (int i = size - 1; i >= 0; i--) {
                Multicaster m = this.mMulticasters.get(i);
                if (m != null && m.getUid() == uid) {
                    removeMulticasterLocked(i, uid);
                }
            }
        }
    }

    private void removeMulticasterLocked(int i, int uid) {
        Multicaster removed = this.mMulticasters.remove(i);
        if (removed != null) {
            removed.unlinkDeathRecipient();
        }
        if (this.mMulticasters.size() == 0) {
            this.mWifiStateMachine.startFilteringMulticastPackets();
        }
        long ident = Binder.clearCallingIdentity();
        try {
            this.mBatteryStats.noteWifiMulticastDisabled(uid);
        } catch (RemoteException e) {
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public boolean isMulticastEnabled() {
        boolean z;
        enforceAccessPermission();
        synchronized (this.mMulticasters) {
            z = this.mMulticasters.size() > 0;
        }
        return z;
    }

    public void enableVerboseLogging(int verbose) {
        enforceAccessPermission();
        this.mWifiStateMachine.enableVerboseLogging(verbose);
    }

    public int getVerboseLoggingLevel() {
        enforceAccessPermission();
        return this.mWifiStateMachine.getVerboseLoggingLevel();
    }

    public void enableAggressiveHandover(int enabled) {
        enforceAccessPermission();
        this.mWifiStateMachine.enableAggressiveHandover(enabled);
    }

    public int getAggressiveHandover() {
        enforceAccessPermission();
        return this.mWifiStateMachine.getAggressiveHandover();
    }

    public void setAllowScansWithTraffic(int enabled) {
        enforceAccessPermission();
        this.mWifiStateMachine.setAllowScansWithTraffic(enabled);
    }

    public int getAllowScansWithTraffic() {
        enforceAccessPermission();
        return this.mWifiStateMachine.getAllowScansWithTraffic();
    }

    public boolean setEnableAutoJoinWhenAssociated(boolean enabled) {
        enforceChangePermission();
        return this.mWifiStateMachine.setEnableAutoJoinWhenAssociated(enabled);
    }

    public boolean getEnableAutoJoinWhenAssociated() {
        enforceAccessPermission();
        return this.mWifiStateMachine.getEnableAutoJoinWhenAssociated();
    }

    public WifiConnectionStatistics getConnectionStatistics() {
        enforceAccessPermission();
        enforceReadCredentialPermission();
        if (this.mWifiStateMachineChannel != null) {
            return this.mWifiStateMachine.syncGetConnectionStatistics(this.mWifiStateMachineChannel);
        }
        Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
        return null;
    }

    public void factoryReset() {
        enforceConnectivityInternalPermission();
        if (this.mUserManager.hasUserRestriction("no_network_reset")) {
            return;
        }
        if (!this.mUserManager.hasUserRestriction("no_config_tethering")) {
            setWifiApEnabled(null, false);
        }
        if (this.mUserManager.hasUserRestriction("no_config_wifi")) {
            return;
        }
        setWifiEnabled(true);
        this.mWifiStateMachine.factoryReset(Binder.getCallingUid());
    }

    static boolean logAndReturnFalse(String s) {
        Log.d(TAG, s);
        return false;
    }

    public static boolean isValid(WifiConfiguration config) {
        String validity = checkValidity(config);
        if (validity != null) {
            return logAndReturnFalse(validity);
        }
        return true;
    }

    public static boolean isValidPasspoint(WifiConfiguration config) {
        String validity = checkPasspointValidity(config);
        if (validity != null) {
            return logAndReturnFalse(validity);
        }
        return true;
    }

    public static String checkValidity(WifiConfiguration config) {
        if (config.allowedKeyManagement == null) {
            return "allowed kmgmt";
        }
        if (config.allowedKeyManagement.cardinality() > 1) {
            if (config.allowedKeyManagement.cardinality() != 2) {
                return "cardinality != 2";
            }
            if (!config.allowedKeyManagement.get(2)) {
                return "not WPA_EAP";
            }
            if (!config.allowedKeyManagement.get(3) && !config.allowedKeyManagement.get(1)) {
                return "not PSK or 8021X";
            }
        }
        return null;
    }

    public static String checkPasspointValidity(WifiConfiguration config) {
        if (!TextUtils.isEmpty(config.FQDN)) {
            if (!TextUtils.isEmpty(config.SSID)) {
                return "SSID not expected for Passpoint: '" + config.SSID + "' FQDN " + toHexString(config.FQDN);
            }
            if (TextUtils.isEmpty(config.providerFriendlyName)) {
                return "no provider friendly name";
            }
            WifiEnterpriseConfig enterpriseConfig = config.enterpriseConfig;
            if (enterpriseConfig == null || enterpriseConfig.getEapMethod() == -1) {
                return "no enterprise config";
            }
            if ((enterpriseConfig.getEapMethod() == 1 || enterpriseConfig.getEapMethod() == 2 || enterpriseConfig.getEapMethod() == 0) && enterpriseConfig.getCaCertificate() == null) {
                return "no CA certificate";
            }
        }
        return null;
    }

    public Network getCurrentNetwork() {
        enforceAccessPermission();
        return this.mWifiStateMachine.getCurrentNetwork();
    }

    public static String toHexString(String s) {
        if (s == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('\'').append(s).append('\'');
        for (int n = 0; n < s.length(); n++) {
            sb.append(String.format(" %02x", Integer.valueOf(s.charAt(n) & 65535)));
        }
        return sb.toString();
    }

    private boolean checkCallerCanAccessScanResults(String callingPackage, int uid) {
        if (ActivityManager.checkUidPermission("android.permission.ACCESS_FINE_LOCATION", uid) == 0 && checkAppOppAllowed(1, callingPackage, uid)) {
            return true;
        }
        if (ActivityManager.checkUidPermission("android.permission.ACCESS_COARSE_LOCATION", uid) == 0 && checkAppOppAllowed(0, callingPackage, uid)) {
            return true;
        }
        boolean apiLevel23App = isMApp(this.mContext, callingPackage);
        if (!apiLevel23App && isForegroundApp(callingPackage)) {
            return true;
        }
        Log.e(TAG, "Permission denial: Need ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION permission to get scan results");
        return false;
    }

    private boolean checkAppOppAllowed(int op, String callingPackage, int uid) {
        return this.mAppOps.noteOp(op, uid, callingPackage) == 0;
    }

    private static boolean isMApp(Context context, String pkgName) {
        try {
            return context.getPackageManager().getApplicationInfo(pkgName, 0).targetSdkVersion >= 23;
        } catch (PackageManager.NameNotFoundException e) {
            return true;
        }
    }

    public void hideCertFromUnaffiliatedUsers(String alias) {
        this.mCertManager.hideCertFromUnaffiliatedUsers(alias);
    }

    public String[] listClientCertsForCurrentUser() {
        return this.mCertManager.listClientCertsForCurrentUser();
    }

    private boolean isForegroundApp(String pkgName) {
        ActivityManager am = (ActivityManager) this.mContext.getSystemService("activity");
        List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
        if (tasks.isEmpty()) {
            return false;
        }
        return pkgName.equals(tasks.get(0).topActivity.getPackageName());
    }

    public void enableWifiConnectivityManager(boolean enabled) {
        enforceConnectivityInternalPermission();
        this.mWifiStateMachine.enableWifiConnectivityManager(enabled);
    }

    public boolean doCtiaTestOn() {
        enforceChangePermission();
        if (this.mWifiStateMachineChannel != null) {
            return this.mWifiStateMachine.syncDoCtiaTestOn(this.mWifiStateMachineChannel);
        }
        Slog.e(TAG, "mWifiStateMachineChannel is not initialized!");
        return false;
    }

    public boolean doCtiaTestOff() {
        enforceChangePermission();
        if (this.mWifiStateMachineChannel != null) {
            return this.mWifiStateMachine.syncDoCtiaTestOff(this.mWifiStateMachineChannel);
        }
        Slog.e(TAG, "mWifiStateMachineChannel is not initialized!");
        return false;
    }

    public boolean doCtiaTestRate(int rate) {
        enforceChangePermission();
        if (this.mWifiStateMachineChannel != null) {
            return this.mWifiStateMachine.syncDoCtiaTestRate(this.mWifiStateMachineChannel, rate);
        }
        Slog.e(TAG, "mWifiStateMachineChannel is not initialized!");
        return false;
    }

    public boolean setTxPowerEnabled(boolean enable) {
        enforceChangePermission();
        if (this.mWifiStateMachineChannel != null) {
            return this.mWifiStateMachine.syncSetTxPowerEnabled(this.mWifiStateMachineChannel, enable);
        }
        Slog.e(TAG, "mWifiStateMachineChannel is not initialized!");
        return false;
    }

    public boolean setTxPower(int offset) {
        enforceChangePermission();
        if (this.mWifiStateMachineChannel != null) {
            return this.mWifiStateMachine.syncSetTxPower(this.mWifiStateMachineChannel, offset);
        }
        Slog.e(TAG, "mWifiStateMachineChannel is not initialized!");
        return false;
    }

    public void addSimCardAuthenticationService(String name, IBinder binder) {
        enforceChangePermission();
        ServiceManager.addService(name, binder);
    }

    public void suspendNotification(int type) {
        enforceChangePermission();
        this.mWifiStateMachine.suspendNotification(type);
    }

    public boolean hasConnectableAp() {
        enforceAccessPermission();
        boolean value = this.mSettingsStore.hasConnectableAp();
        if (!value) {
            return false;
        }
        boolean result = this.mWifiStateMachine.hasConnectableAp();
        if (result) {
        }
        return result;
    }

    private void initializeExtra() {
        this.mWifiOffListenerCount = 0;
    }

    public String getWifiStatus() {
        enforceAccessPermission();
        if (this.mWifiStateMachineChannel != null) {
            String result = this.mWifiStateMachine.getWifiStatus(this.mWifiStateMachineChannel);
            return result;
        }
        Slog.e(TAG, "mWifiStateMachineChannel is not initialized!");
        return "";
    }

    public void setPowerSavingMode(boolean mode) {
        enforceAccessPermission();
        this.mWifiStateMachine.setPowerSavingMode(mode);
    }

    public int syncGetConnectingNetworkId() {
        return -1;
    }

    public PPPOEInfo getPPPOEInfo() {
        enforceAccessPermission();
        return this.mWifiStateMachine.syncGetPppoeInfo();
    }

    public boolean setWoWlanNormalMode() {
        enforceChangePermission();
        Slog.d(TAG, "setWoWlanNormalMode");
        if (this.mWifiStateMachineChannel != null) {
            return this.mWifiStateMachine.syncSetWoWlanNormalMode(this.mWifiStateMachineChannel);
        }
        Slog.e(TAG, "mWifiStateMachineChannel is not initialized!");
        return false;
    }

    public boolean setWoWlanMagicMode() {
        enforceChangePermission();
        Slog.d(TAG, "setWoWlanMagicMode");
        if (this.mWifiStateMachineChannel != null) {
            return this.mWifiStateMachine.syncSetWoWlanMagicMode(this.mWifiStateMachineChannel);
        }
        Slog.e(TAG, "mWifiStateMachineChannel is not initialized!");
        return false;
    }

    public boolean is5gBandSupported() {
        int wifi5gBandSupported = 0;
        try {
            WifiNvRamAgent agent = WifiNvRamAgent.Stub.asInterface(ServiceManager.getService("NvRAMAgent"));
            byte[] buffer = agent.readFileByName("/data/nvram/APCFG/APRDEB/WIFI");
            wifi5gBandSupported = buffer[197] & buffer[262];
            Log.i(TAG, "wifiSupport5g:" + wifi5gBandSupported + ":" + ((int) buffer[197]) + ":" + ((int) buffer[262]));
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (IndexOutOfBoundsException ee) {
            ee.printStackTrace();
        }
        return wifi5gBandSupported == 1;
    }

    public boolean setHotspotOptimization(boolean enable) {
        this.mWifiStateMachine.setHotspotOptimization(enable);
        return true;
    }

    public String getTestEnv(int channel) {
        enforceAccessPermission();
        if (this.mWifiStateMachineChannel != null) {
            return this.mWifiStateMachine.syncGetTestEnv(this.mWifiStateMachineChannel, channel);
        }
        Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
        return null;
    }

    public void setTdlsPowerSave(boolean enable) {
        this.mWifiStateMachine.setTdlsPowerSave(enable);
    }

    public boolean setWifiDisabled(int flag) {
        Slog.d(TAG, "setWifiDisabled:" + flag);
        if (flag == 0) {
            long ident = Binder.clearCallingIdentity();
            try {
                if (!this.mSettingsStore.handleWifiToggled(false)) {
                    return true;
                }
                Binder.restoreCallingIdentity(ident);
                this.mWifiController.obtainMessage(155656, this.mWifiIpoOff ? 1 : 0, 0).sendToTarget();
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        } else if (flag == 1) {
            this.mWifiController.sendMessage(155657, 1);
        }
        return true;
    }

    public void registerWifiOffListener() {
        this.mWifiOffListenerCount++;
        Slog.d(TAG, "registWifiOffListener, mWifiOffListenerCount: " + this.mWifiOffListenerCount);
    }

    public void unregisterWifiOffListener() {
        this.mWifiOffListenerCount--;
        Slog.d(TAG, "unregistWifiOffListener, mWifiOffListenerCount: " + this.mWifiOffListenerCount);
    }

    private void reportWifiOff(int reason) {
        Slog.d(TAG, "reportWifiOff, reason: " + (reason == 0 ? "wifi setting" : "airplane mode on"));
        Intent intent = new Intent("com.mediatek.android.wifi_off_notify");
        intent.putExtra("wifi_off_reason", reason);
        this.mContext.sendBroadcast(intent);
    }
}
