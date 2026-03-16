package com.android.server.wifi;

import android.R;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.DhcpResults;
import android.net.NetworkUtils;
import android.net.wifi.BatchedScanResult;
import android.net.wifi.BatchedScanSettings;
import android.net.wifi.IWifiManager;
import android.net.wifi.ScanResult;
import android.net.wifi.ScanSettings;
import android.net.wifi.WifiActivityEnergyInfo;
import android.net.wifi.WifiChannel;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConnectionStatistics;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiLinkLayerStats;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.WorkSource;
import android.provider.Settings;
import android.util.Slog;
import com.android.internal.app.IBatteryStats;
import com.android.internal.util.AsyncChannel;
import com.android.server.am.BatteryStatsService;
import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class WifiServiceImpl extends IWifiManager.Stub {
    private static final boolean DBG = true;
    private static final String TAG = "WifiService";
    private final AppOpsManager mAppOps;
    final boolean mBatchedScanSupported;
    private final IBatteryStats mBatteryStats;
    private ClientHandler mClientHandler;
    private final Context mContext;
    private int mFullHighPerfLocksAcquired;
    private int mFullHighPerfLocksReleased;
    private int mFullLocksAcquired;
    private int mFullLocksReleased;
    private int mMulticastDisabled;
    private int mMulticastEnabled;
    private WifiNotificationController mNotificationController;
    private int mScanLocksAcquired;
    private int mScanLocksReleased;
    final WifiSettingsStore mSettingsStore;
    private WifiTrafficPoller mTrafficPoller;
    private WifiController mWifiController;
    private WifiManager.WifiLock mWifiHighPerfLock;
    final WifiStateMachine mWifiStateMachine;
    private AsyncChannel mWifiStateMachineChannel;
    WifiStateMachineHandler mWifiStateMachineHandler;
    private WifiWatchdogStateMachine mWifiWatchdogStateMachine;
    final LockList mLocks = new LockList();
    private final List<Multicaster> mMulticasters = new ArrayList();
    private int scanRequestCounter = 0;
    private final List<BatchedScanRequest> mBatchedScanners = new ArrayList();
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
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
            } else if (action.equals("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED")) {
                int state = intent.getIntExtra("android.bluetooth.adapter.extra.CONNECTION_STATE", 0);
                WifiServiceImpl.this.mWifiStateMachine.sendBluetoothAdapterStateChange(state);
            } else if (action.equals("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED")) {
                boolean emergencyMode = intent.getBooleanExtra("phoneinECMState", false);
                WifiServiceImpl.this.mWifiController.sendMessage(155649, emergencyMode ? 1 : 0, 0);
            }
        }
    };
    private String mInterfaceName = SystemProperties.get("wifi.interface", "wlan0");

    private class ClientHandler extends Handler {
        ClientHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 69632:
                    if (msg.arg1 == 0) {
                        Slog.d(WifiServiceImpl.TAG, "New client listening to asynchronous messages");
                        WifiServiceImpl.this.mTrafficPoller.addClient(msg.replyTo);
                    } else {
                        Slog.e(WifiServiceImpl.TAG, "Client connection failure, error=" + msg.arg1);
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
                        if (config != null) {
                            if (config.networkId == -1) {
                                config.creatorUid = Binder.getCallingUid();
                            } else {
                                config.lastUpdateUid = Binder.getCallingUid();
                            }
                        }
                        Slog.e("WiFiServiceImpl ", "SAVE nid=" + Integer.toString(networkId) + " uid=" + Integer.toString(config.creatorUid) + "/" + Integer.toString(config.lastUpdateUid));
                    }
                    if (msg.what == 151553) {
                        if (config != null) {
                            if (config.networkId == -1) {
                                config.creatorUid = Binder.getCallingUid();
                            } else {
                                config.lastUpdateUid = Binder.getCallingUid();
                            }
                        }
                        Slog.e("WiFiServiceImpl ", "CONNECT  nid=" + Integer.toString(networkId) + " uid=" + Binder.getCallingUid());
                    }
                    if (config != null && config.isValid()) {
                        Slog.d(WifiServiceImpl.TAG, "Connect with config" + config);
                        WifiServiceImpl.this.mWifiStateMachine.sendMessage(Message.obtain(msg));
                    } else if (config == null && networkId != -1) {
                        Slog.d(WifiServiceImpl.TAG, "Connect with networkId" + networkId);
                        WifiServiceImpl.this.mWifiStateMachine.sendMessage(Message.obtain(msg));
                    } else {
                        Slog.e(WifiServiceImpl.TAG, "ClientHandler.handleMessage ignoring invalid msg=" + msg);
                        if (msg.what == 151553) {
                            replyFailed(msg, 151554, 8);
                        } else {
                            replyFailed(msg, 151560, 8);
                        }
                    }
                    break;
                case 151556:
                    if (WifiServiceImpl.this.isOwner(msg.sendingUid)) {
                        WifiServiceImpl.this.mWifiStateMachine.sendMessage(Message.obtain(msg));
                    } else {
                        Slog.e(WifiServiceImpl.TAG, "Forget is not authorized for user");
                        replyFailed(msg, 151557, 9);
                    }
                    break;
                case 151562:
                case 151566:
                case 151569:
                case 151572:
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

    private void grabWifiHighPerfLock() {
        if (this.mWifiHighPerfLock == null) {
            Slog.d(TAG, "acquire wifi high perf lock");
            this.mWifiHighPerfLock = ((WifiManager) this.mContext.getSystemService("wifi")).createWifiLock(3, TAG);
            this.mWifiHighPerfLock.setReferenceCounted(false);
            this.mWifiHighPerfLock.acquire();
        }
    }

    private void releaseWifiHighPerfLock() {
        if (this.mWifiHighPerfLock != null) {
            Slog.d(TAG, "release wifi high perf lock");
            this.mWifiHighPerfLock.release();
            this.mWifiHighPerfLock = null;
        }
    }

    public WifiServiceImpl(Context context) {
        this.mContext = context;
        this.mTrafficPoller = new WifiTrafficPoller(this.mContext, this, this.mInterfaceName);
        this.mWifiStateMachine = new WifiStateMachine(this.mContext, this.mInterfaceName, this.mTrafficPoller);
        this.mWifiStateMachine.enableRssiPolling(true);
        this.mBatteryStats = BatteryStatsService.getService();
        this.mAppOps = (AppOpsManager) context.getSystemService("appops");
        this.mNotificationController = new WifiNotificationController(this.mContext, this.mWifiStateMachine);
        this.mSettingsStore = new WifiSettingsStore(this.mContext);
        HandlerThread wifiThread = new HandlerThread(TAG);
        wifiThread.start();
        this.mClientHandler = new ClientHandler(wifiThread.getLooper());
        this.mWifiStateMachineHandler = new WifiStateMachineHandler(wifiThread.getLooper());
        this.mWifiController = new WifiController(this.mContext, this, wifiThread.getLooper());
        this.mBatchedScanSupported = this.mContext.getResources().getBoolean(R.^attr-private.borderLeft);
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String voipSwitch = intent.getStringExtra("voipEvent");
                if (voipSwitch.equals("on")) {
                    WifiServiceImpl.this.grabWifiHighPerfLock();
                } else {
                    WifiServiceImpl.this.releaseWifiHighPerfLock();
                }
            }
        }, new IntentFilter("android.net.conn.WIFI_VOIP_SWITCH_EVENT"));
    }

    public void checkAndStartWifi() {
        boolean wifiEnabled = this.mSettingsStore.isWifiToggleEnabled();
        Slog.i(TAG, "WifiService starting up with Wi-Fi " + (wifiEnabled ? "enabled" : "disabled"));
        registerForScanModeChange();
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (WifiServiceImpl.this.mSettingsStore.handleAirplaneModeToggled()) {
                    WifiServiceImpl.this.mWifiController.sendMessage(155657);
                }
            }
        }, new IntentFilter("android.intent.action.AIRPLANE_MODE"));
        registerForBroadcasts();
        this.mWifiController.start();
        if (wifiEnabled) {
            setWifiEnabled(wifiEnabled);
        }
        this.mWifiWatchdogStateMachine = WifiWatchdogStateMachine.makeWifiWatchdogStateMachine(this.mContext, this.mWifiStateMachine.getMessenger());
    }

    public boolean pingSupplicant() {
        enforceAccessPermission();
        if (this.mWifiStateMachineChannel != null) {
            return this.mWifiStateMachine.syncPingSupplicant(this.mWifiStateMachineChannel);
        }
        Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
        return false;
    }

    public List<WifiChannel> getChannelList() {
        enforceAccessPermission();
        if (this.mWifiStateMachineChannel != null) {
            return this.mWifiStateMachine.syncGetChannelList(this.mWifiStateMachineChannel);
        }
        Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
        return null;
    }

    public void startLocationRestrictedScan(WorkSource workSource) {
        enforceChangePermission();
        enforceLocationHardwarePermission();
        List<WifiChannel> channels = getChannelList();
        if (channels == null) {
            Slog.e(TAG, "startLocationRestrictedScan cant get channels");
            return;
        }
        ScanSettings settings = new ScanSettings();
        for (WifiChannel channel : channels) {
            if (!channel.isDFS) {
                settings.channelSet.add(channel);
            }
        }
        if (workSource == null) {
            workSource = new WorkSource(-6);
        }
        startScan(settings, workSource);
    }

    public void startScan(ScanSettings settings, WorkSource workSource) {
        enforceChangePermission();
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
        WifiStateMachine wifiStateMachine = this.mWifiStateMachine;
        int callingUid = Binder.getCallingUid();
        int i = this.scanRequestCounter;
        this.scanRequestCounter = i + 1;
        wifiStateMachine.startScan(callingUid, i, settings, workSource);
    }

    private class BatchedScanRequest extends DeathRecipient {
        final int pid;
        final BatchedScanSettings settings;
        final int uid;
        final WorkSource workSource;

        BatchedScanRequest(BatchedScanSettings settings, IBinder binder, WorkSource ws) {
            super(0, null, binder, null);
            this.settings = settings;
            this.uid = Binder.getCallingUid();
            this.pid = Binder.getCallingPid();
            this.workSource = ws;
        }

        @Override
        public void binderDied() {
            WifiServiceImpl.this.stopBatchedScan(this.settings, this.uid, this.pid);
        }

        public String toString() {
            return "BatchedScanRequest{settings=" + this.settings + ", binder=" + this.mBinder + "}";
        }

        public boolean isSameApp(int uid, int pid) {
            return this.uid == uid && this.pid == pid;
        }
    }

    public boolean isBatchedScanSupported() {
        return this.mBatchedScanSupported;
    }

    public void pollBatchedScan() {
        enforceChangePermission();
        if (this.mBatchedScanSupported) {
            this.mWifiStateMachine.requestBatchedScanPoll();
        }
    }

    public String getWpsNfcConfigurationToken(int netId) {
        enforceConnectivityInternalPermission();
        return this.mWifiStateMachine.syncGetWpsNfcConfigurationToken(netId);
    }

    public boolean requestBatchedScan(BatchedScanSettings requested, IBinder binder, WorkSource workSource) {
        enforceChangePermission();
        if (workSource != null) {
            enforceWorkSourcePermission();
            workSource.clearNames();
        }
        if (!this.mBatchedScanSupported) {
            return false;
        }
        BatchedScanSettings requested2 = new BatchedScanSettings(requested);
        if (requested2.isInvalid()) {
            return false;
        }
        BatchedScanRequest r = new BatchedScanRequest(requested2, binder, workSource);
        synchronized (this.mBatchedScanners) {
            this.mBatchedScanners.add(r);
            resolveBatchedScannersLocked();
        }
        return true;
    }

    public List<BatchedScanResult> getBatchedScanResults(String callingPackage) {
        List<BatchedScanResult> listSyncGetBatchedScanResultsList;
        enforceAccessPermission();
        if (!this.mBatchedScanSupported) {
            return new ArrayList();
        }
        int uid = Binder.getCallingUid();
        int userId = UserHandle.getCallingUserId();
        boolean hasInteractUsersFull = checkInteractAcrossUsersFull();
        long ident = Binder.clearCallingIdentity();
        try {
            if (this.mAppOps.noteOp(10, uid, callingPackage) != 0) {
                listSyncGetBatchedScanResultsList = new ArrayList<>();
            } else if (isCurrentProfile(userId) || hasInteractUsersFull) {
                listSyncGetBatchedScanResultsList = this.mWifiStateMachine.syncGetBatchedScanResultsList();
                Binder.restoreCallingIdentity(ident);
            } else {
                listSyncGetBatchedScanResultsList = new ArrayList<>();
                Binder.restoreCallingIdentity(ident);
            }
            return listSyncGetBatchedScanResultsList;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void stopBatchedScan(BatchedScanSettings settings) {
        enforceChangePermission();
        if (this.mBatchedScanSupported) {
            stopBatchedScan(settings, getCallingUid(), getCallingPid());
        }
    }

    private void stopBatchedScan(BatchedScanSettings settings, int uid, int pid) {
        ArrayList<BatchedScanRequest> found = new ArrayList<>();
        synchronized (this.mBatchedScanners) {
            for (BatchedScanRequest r : this.mBatchedScanners) {
                if (r.isSameApp(uid, pid) && (settings == null || settings.equals(r.settings))) {
                    found.add(r);
                    if (settings != null) {
                        break;
                    }
                }
            }
            Iterator<BatchedScanRequest> it = found.iterator();
            while (it.hasNext()) {
                this.mBatchedScanners.remove(it.next());
            }
            if (found.size() != 0) {
                resolveBatchedScannersLocked();
            }
        }
    }

    private void resolveBatchedScannersLocked() {
        int currentChannelCount;
        int currentScanInterval;
        BatchedScanSettings setting = new BatchedScanSettings();
        WorkSource responsibleWorkSource = null;
        int responsibleUid = 0;
        double responsibleCsph = 0.0d;
        if (this.mBatchedScanners.size() == 0) {
            this.mWifiStateMachine.setBatchedScanSettings(null, 0, 0, null);
            return;
        }
        for (BatchedScanRequest r : this.mBatchedScanners) {
            BatchedScanSettings s = r.settings;
            if (s.channelSet == null || s.channelSet.isEmpty()) {
                currentChannelCount = 20;
            } else {
                currentChannelCount = s.channelSet.size();
                if (s.channelSet.contains("A")) {
                    currentChannelCount += 8;
                }
                if (s.channelSet.contains("B")) {
                    currentChannelCount += 10;
                }
            }
            if (s.scanIntervalSec == Integer.MAX_VALUE) {
                currentScanInterval = 30;
            } else {
                currentScanInterval = s.scanIntervalSec;
            }
            double currentCsph = (currentChannelCount * 3600) / currentScanInterval;
            if (currentCsph > responsibleCsph) {
                responsibleUid = r.uid;
                responsibleWorkSource = r.workSource;
                responsibleCsph = currentCsph;
            }
            if (s.maxScansPerBatch != Integer.MAX_VALUE && s.maxScansPerBatch < setting.maxScansPerBatch) {
                setting.maxScansPerBatch = s.maxScansPerBatch;
            }
            if (s.maxApPerScan != Integer.MAX_VALUE && (setting.maxApPerScan == Integer.MAX_VALUE || s.maxApPerScan > setting.maxApPerScan)) {
                setting.maxApPerScan = s.maxApPerScan;
            }
            if (s.scanIntervalSec != Integer.MAX_VALUE && s.scanIntervalSec < setting.scanIntervalSec) {
                setting.scanIntervalSec = s.scanIntervalSec;
            }
            if (s.maxApForDistance != Integer.MAX_VALUE && (setting.maxApForDistance == Integer.MAX_VALUE || s.maxApForDistance > setting.maxApForDistance)) {
                setting.maxApForDistance = s.maxApForDistance;
            }
            if (s.channelSet != null && s.channelSet.size() != 0) {
                if (setting.channelSet == null || setting.channelSet.size() != 0) {
                    if (setting.channelSet == null) {
                        setting.channelSet = new ArrayList();
                    }
                    for (String i : s.channelSet) {
                        if (!setting.channelSet.contains(i)) {
                            setting.channelSet.add(i);
                        }
                    }
                }
            } else if (setting.channelSet == null || setting.channelSet.size() != 0) {
                setting.channelSet = new ArrayList();
            }
        }
        setting.constrain();
        this.mWifiStateMachine.setBatchedScanSettings(setting, responsibleUid, (int) responsibleCsph, responsibleWorkSource);
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

    public synchronized boolean setWifiEnabled(boolean enable) {
        enforceChangePermission();
        Slog.d(TAG, "setWifiEnabled: " + enable + " pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
        Slog.e(TAG, "Invoking mWifiStateMachine.setWifiEnabled\n");
        long ident = Binder.clearCallingIdentity();
        try {
            if (this.mSettingsStore.handleWifiToggled(enable)) {
                Binder.restoreCallingIdentity(ident);
                this.mWifiController.sendMessage(155656);
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
        return true;
    }

    public boolean enableActiveRoaming(boolean enable) {
        enforceChangePermission();
        return this.mWifiStateMachine.setActiveRoaming(enable);
    }

    public int getWifiEnabledState() {
        enforceAccessPermission();
        return this.mWifiStateMachine.syncGetWifiState();
    }

    public void setWifiApEnabled(WifiConfiguration wifiConfig, boolean enabled) {
        enforceChangePermission();
        ConnectivityManager.enforceTetherChangePermission(this.mContext);
        UserManager um = UserManager.get(this.mContext);
        if (um.hasUserRestriction("no_config_tethering")) {
            throw new SecurityException("DISALLOW_CONFIG_TETHERING is enabled for this user.");
        }
        if (wifiConfig != null && !wifiConfig.isValid()) {
            Slog.e(TAG, "Invalid WifiConfiguration");
        } else {
            this.mWifiController.obtainMessage(155658, enabled ? 1 : 0, 0, wifiConfig).sendToTarget();
        }
    }

    public int getWifiApEnabledState() {
        enforceAccessPermission();
        return this.mWifiStateMachine.syncGetWifiApState();
    }

    public WifiConfiguration getWifiApConfiguration() {
        enforceAccessPermission();
        return this.mWifiStateMachine.syncGetWifiApConfiguration();
    }

    public void setWifiApConfiguration(WifiConfiguration wifiConfig) {
        enforceChangePermission();
        if (wifiConfig != null) {
            if (wifiConfig.isValid()) {
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
        enforceChangePermission();
        this.mWifiStateMachine.disconnectCommand();
    }

    public void reconnect() {
        enforceChangePermission();
        this.mWifiStateMachine.reconnectCommand();
    }

    public void reassociate() {
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

    public WifiActivityEnergyInfo reportActivityInfo() {
        enforceAccessPermission();
        WifiActivityEnergyInfo energyInfo = null;
        if (this.mWifiStateMachineChannel != null) {
            WifiLinkLayerStats stats = this.mWifiStateMachine.syncGetLinkLayerStats(this.mWifiStateMachineChannel);
            if (stats != null) {
                energyInfo = new WifiActivityEnergyInfo(3, stats.tx_time, stats.rx_time, (stats.on_time - stats.tx_time) - stats.rx_time, 0);
            }
            return energyInfo;
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

    public int addOrUpdateNetwork(WifiConfiguration config) {
        enforceChangePermission();
        if (config.isValid()) {
            Slog.e("addOrUpdateNetwork", " uid = " + Integer.toString(Binder.getCallingUid()) + " SSID " + config.SSID + " nid=" + Integer.toString(config.networkId));
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
        Slog.e(TAG, "bad network configuration");
        return -1;
    }

    public boolean removeNetwork(int netId) {
        enforceChangePermission();
        if (!isOwner(Binder.getCallingUid())) {
            Slog.e(TAG, "Remove is not authorized for user");
            return false;
        }
        if (this.mWifiStateMachineChannel != null) {
            return this.mWifiStateMachine.syncRemoveNetwork(this.mWifiStateMachineChannel, netId);
        }
        Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
        return false;
    }

    public boolean enableNetwork(int netId, boolean disableOthers) {
        enforceChangePermission();
        if (this.mWifiStateMachineChannel != null) {
            return this.mWifiStateMachine.syncEnableNetwork(this.mWifiStateMachineChannel, netId, disableOthers);
        }
        Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
        return false;
    }

    public boolean disableNetwork(int netId) {
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
        List<ScanResult> listSyncGetScanResultsList;
        enforceAccessPermission();
        int userId = UserHandle.getCallingUserId();
        int uid = Binder.getCallingUid();
        boolean hasInteractUsersFull = checkInteractAcrossUsersFull();
        long ident = Binder.clearCallingIdentity();
        try {
            if (this.mAppOps.noteOp(10, uid, callingPackage) != 0) {
                listSyncGetScanResultsList = new ArrayList<>();
            } else if (!isCurrentProfile(userId) && !hasInteractUsersFull) {
                listSyncGetScanResultsList = new ArrayList<>();
            } else {
                listSyncGetScanResultsList = this.mWifiStateMachine.syncGetScanResultsList();
            }
            return listSyncGetScanResultsList;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private boolean checkInteractAcrossUsersFull() {
        return this.mContext.checkCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL") == 0;
    }

    private boolean isCurrentProfile(int userId) {
        int currentUser = ActivityManager.getCurrentUser();
        if (userId == currentUser) {
            return true;
        }
        List<UserInfo> profiles = UserManager.get(this.mContext).getProfiles(currentUser);
        for (UserInfo user : profiles) {
            if (userId == user.id) {
                return true;
            }
        }
        return false;
    }

    private boolean isOwner(int uid) {
        long ident = Binder.clearCallingIdentity();
        int userId = UserHandle.getUserId(uid);
        if (userId == 0) {
            return true;
        }
        try {
            List<UserInfo> profiles = UserManager.get(this.mContext).getProfiles(0);
            for (UserInfo profile : profiles) {
                if (userId == profile.id) {
                    return true;
                }
            }
            return false;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public boolean saveConfiguration() {
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
            this.mWifiStateMachine.setCountryCode(countryCode, persist);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
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
        return this.mContext.getResources().getBoolean(R.^attr-private.autofillDatasetPickerMaxHeight);
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
        InetAddress serverAddress = dhcpResults.serverAddress;
        if (serverAddress instanceof Inet4Address) {
            info.serverAddress = NetworkUtils.inetAddressToInt((Inet4Address) serverAddress);
        }
        info.leaseDuration = dhcpResults.leaseDuration;
        return info;
    }

    public void startWifi() {
        enforceConnectivityInternalPermission();
        this.mWifiStateMachine.setDriverStart(true);
        this.mWifiStateMachine.reconnectCommand();
    }

    public void stopWifi() {
        enforceConnectivityInternalPermission();
        this.mWifiStateMachine.setDriverStart(false);
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
            } catch (FileNotFoundException e) {
            } catch (IOException e2) {
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
                        reader2 = reader;
                    } catch (IOException e3) {
                        reader2 = reader;
                    }
                } else {
                    reader2 = reader;
                }
            } catch (FileNotFoundException e4) {
                reader2 = reader;
                Slog.e(WifiServiceImpl.TAG, "Could not open /proc/net/arp to lookup mac address");
                if (reader2 != null) {
                    try {
                        reader2.close();
                    } catch (IOException e5) {
                    }
                }
            } catch (IOException e6) {
                reader2 = reader;
                Slog.e(WifiServiceImpl.TAG, "Could not read /proc/net/arp to lookup mac address");
                if (reader2 != null) {
                    try {
                        reader2.close();
                    } catch (IOException e7) {
                    }
                }
            } catch (Throwable th2) {
                th = th2;
                reader2 = reader;
                if (reader2 != null) {
                    try {
                        reader2.close();
                    } catch (IOException e8) {
                    }
                }
                throw th;
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
        this.mContext.registerReceiver(this.mReceiver, intentFilter);
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            pw.println("Permission Denial: can't dump WifiService from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            return;
        }
        pw.println("Wi-Fi is " + this.mWifiStateMachine.syncGetWifiStateByName());
        pw.println("Stay-awake conditions: " + Settings.Global.getInt(this.mContext.getContentResolver(), "stay_on_while_plugged_in", 0));
        pw.println("mMulticastEnabled " + this.mMulticastEnabled);
        pw.println("mMulticastDisabled " + this.mMulticastDisabled);
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
        this.mWifiWatchdogStateMachine.dump(fd, pw, args);
        pw.println();
        this.mWifiStateMachine.dump(fd, pw, args);
        pw.println();
    }

    private class WifiLock extends DeathRecipient {
        WifiLock(int lockMode, String tag, IBinder binder, WorkSource ws) {
            super(lockMode, tag, binder, ws);
        }

        @Override
        public void binderDied() {
            synchronized (WifiServiceImpl.this.mLocks) {
                WifiServiceImpl.this.releaseWifiLockLocked(this.mBinder);
            }
        }

        public String toString() {
            return "WifiLock{" + this.mTag + " type=" + this.mMode + " binder=" + this.mBinder + "}";
        }
    }

    class LockList {
        private List<WifiLock> mList;

        private LockList() {
            this.mList = new ArrayList();
        }

        synchronized boolean hasLocks() {
            return !this.mList.isEmpty();
        }

        synchronized int getStrongestLockMode() {
            int i = 1;
            synchronized (this) {
                if (!this.mList.isEmpty()) {
                    if (WifiServiceImpl.this.mFullHighPerfLocksAcquired <= WifiServiceImpl.this.mFullHighPerfLocksReleased) {
                        if (WifiServiceImpl.this.mFullLocksAcquired <= WifiServiceImpl.this.mFullLocksReleased) {
                            i = 2;
                        }
                    } else {
                        i = 3;
                    }
                }
            }
            return i;
        }

        synchronized void updateWorkSource(WorkSource ws) {
            for (int i = 0; i < WifiServiceImpl.this.mLocks.mList.size(); i++) {
                ws.add(WifiServiceImpl.this.mLocks.mList.get(i).mWorkSource);
            }
        }

        private void addLock(WifiLock lock) {
            if (findLockByBinder(lock.mBinder) < 0) {
                this.mList.add(lock);
            }
        }

        private WifiLock removeLock(IBinder binder) {
            int index = findLockByBinder(binder);
            if (index < 0) {
                return null;
            }
            WifiLock ret = this.mList.remove(index);
            ret.unlinkDeathRecipient();
            return ret;
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
        if (uid != Process.myUid()) {
            this.mContext.enforcePermission("android.permission.UPDATE_DEVICE_STATS", pid, uid, null);
        }
    }

    public boolean acquireWifiLock(IBinder binder, int lockMode, String tag, WorkSource ws) {
        boolean zAcquireWifiLockLocked;
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
                if (index >= 0) {
                    WifiLock wl = (WifiLock) this.mLocks.mList.get(index);
                    noteReleaseWifiLock(wl);
                    wl.mWorkSource = ws != null ? new WorkSource(ws) : new WorkSource(uid);
                    noteAcquireWifiLock(wl);
                } else {
                    throw new IllegalArgumentException("Wifi lock not active");
                }
            }
        } catch (RemoteException e) {
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public boolean releaseWifiLock(IBinder lock) {
        boolean zReleaseWifiLockLocked;
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
        int mMode;
        String mTag;
        WorkSource mWorkSource;

        DeathRecipient(int mode, String tag, IBinder binder, WorkSource ws) {
            this.mTag = tag;
            this.mMode = mode;
            this.mBinder = binder;
            this.mWorkSource = ws;
            try {
                this.mBinder.linkToDeath(this, 0);
            } catch (RemoteException e) {
                binderDied();
            }
        }

        void unlinkDeathRecipient() {
            this.mBinder.unlinkToDeath(this, 0);
        }
    }

    private class Multicaster extends DeathRecipient {
        Multicaster(String tag, IBinder binder) {
            super(Binder.getCallingUid(), tag, binder, null);
        }

        @Override
        public void binderDied() {
            Slog.e(WifiServiceImpl.TAG, "Multicaster binderDied");
            synchronized (WifiServiceImpl.this.mMulticasters) {
                int i = WifiServiceImpl.this.mMulticasters.indexOf(this);
                if (i != -1) {
                    WifiServiceImpl.this.removeMulticasterLocked(i, this.mMode);
                }
            }
        }

        public String toString() {
            return "Multicaster{" + this.mTag + " binder=" + this.mBinder + "}";
        }

        public int getUid() {
            return this.mMode;
        }
    }

    public void initializeMulticastFiltering() {
        enforceMulticastChangePermission();
        synchronized (this.mMulticasters) {
            if (this.mMulticasters.size() == 0) {
                this.mWifiStateMachine.startFilteringMulticastV4Packets();
            }
        }
    }

    public void acquireMulticastLock(IBinder binder, String tag) {
        enforceMulticastChangePermission();
        synchronized (this.mMulticasters) {
            this.mMulticastEnabled++;
            this.mMulticasters.add(new Multicaster(tag, binder));
            this.mWifiStateMachine.stopFilteringMulticastV4Packets();
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
            this.mWifiStateMachine.startFilteringMulticastV4Packets();
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

    public WifiMonitor getWifiMonitor() {
        return this.mWifiStateMachine.getWifiMonitor();
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

    public WifiConnectionStatistics getConnectionStatistics() {
        enforceAccessPermission();
        enforceReadCredentialPermission();
        if (this.mWifiStateMachineChannel != null) {
            return this.mWifiStateMachine.syncGetConnectionStatistics(this.mWifiStateMachineChannel);
        }
        Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
        return null;
    }
}
