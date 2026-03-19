package com.mediatek.server.wifi;

import android.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.IState;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.internal.util.WakeupMessage;
import com.android.server.wifi.FrameworkFacade;
import com.android.server.wifi.SoftApManager;
import com.android.server.wifi.SoftApModeConfiguration;
import com.android.server.wifi.WifiApConfigStore;
import com.android.server.wifi.WifiMetrics;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.util.ApConfigUtil;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileDescriptor;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;
import mediatek.net.wifi.HotspotClient;

public class MtkSoftApManager extends SoftApManager {
    private static final String ALLOWED_LIST_FILE = Environment.getDataDirectory() + "/misc/wifi/allowed_list.conf";
    static final int BASE = 131072;
    private static final int MIN_SOFT_AP_TIMEOUT_DELAY_MS = 600000;
    public static final int M_CMD_ALLOW_DEVICE = 131378;
    public static final int M_CMD_BLOCK_CLIENT = 131372;
    public static final int M_CMD_DISALLOW_DEVICE = 131379;
    public static final int M_CMD_GET_ALLOWED_DEVICES = 131380;
    public static final int M_CMD_GET_CLIENTS_LIST = 131374;
    public static final int M_CMD_IS_ALL_DEVICES_ALLOWED = 131376;
    public static final int M_CMD_SET_ALL_DEVICES_ALLOWED = 131377;
    public static final int M_CMD_START_AP_WPS = 131375;
    public static final int M_CMD_UNBLOCK_CLIENT = 131373;

    @VisibleForTesting
    public static final String SOFT_AP_SEND_MESSAGE_TIMEOUT_TAG = "MtkSoftApManager Soft AP Send Message Timeout";
    private static final String TAG = "MtkSoftApManager";
    private static LinkedHashMap<String, HotspotClient> sAllowedDevices;
    private WifiConfiguration mApConfig;
    private String mApInterfaceName;
    private final WifiManager.SoftApCallback mCallback;
    private final Context mContext;
    private final String mCountryCode;
    private final FrameworkFacade mFrameworkFacade;
    private HashMap<String, HotspotClient> mHotspotClients;
    private boolean mIfaceIsUp;
    private Looper mLooper;
    private final int mMode;
    private int mNumAssociatedStations;
    private int mReportedBandwidth;
    private int mReportedFrequency;
    private final WifiNative.SoftApListener mSoftApListener;
    private final SoftApStateMachine mStateMachine;
    private boolean mTimeoutEnabled;
    private final WifiApConfigStore mWifiApConfigStore;
    private final WifiMetrics mWifiMetrics;
    private final WifiNative mWifiNative;
    private final BroadcastReceiver mWifiP2pReceiver;

    public MtkSoftApManager(Context context, Looper looper, FrameworkFacade frameworkFacade, WifiNative wifiNative, String str, WifiManager.SoftApCallback softApCallback, WifiApConfigStore wifiApConfigStore, SoftApModeConfiguration softApModeConfiguration, WifiMetrics wifiMetrics) {
        super(context, looper, frameworkFacade, wifiNative, str, softApCallback, wifiApConfigStore, softApModeConfiguration, wifiMetrics);
        this.mReportedFrequency = -1;
        this.mReportedBandwidth = -1;
        this.mNumAssociatedStations = 0;
        this.mTimeoutEnabled = false;
        this.mSoftApListener = new WifiNative.SoftApListener() {
            @Override
            public void onNumAssociatedStationsChanged(int i) {
                MtkSoftApManager.this.mStateMachine.sendMessage(4, i);
            }

            @Override
            public void onSoftApChannelSwitched(int i, int i2) {
                MtkSoftApManager.this.mStateMachine.sendMessage(9, i, i2);
            }
        };
        this.mHotspotClients = new HashMap<>();
        this.mWifiP2pReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if (intent.getAction().equals("android.net.wifi.p2p.CONNECTION_STATE_CHANGE")) {
                    boolean zIsConnected = ((NetworkInfo) intent.getParcelableExtra("networkInfo")).isConnected();
                    Log.d(MtkSoftApManager.TAG, "[STA+SAP] Received WIFI_P2P_CONNECTION_CHANGED_ACTION: isConnected = " + zIsConnected);
                    if (zIsConnected) {
                        Log.d(MtkSoftApManager.TAG, "[STA+SAP] Stop softap due to p2p is connected");
                        ((WifiManager) MtkSoftApManager.this.mContext.getSystemService("wifi")).stopSoftAp();
                    }
                }
            }
        };
        this.mLooper = looper;
        this.mContext = context;
        this.mFrameworkFacade = frameworkFacade;
        this.mWifiNative = wifiNative;
        this.mCountryCode = str;
        this.mCallback = softApCallback;
        this.mWifiApConfigStore = wifiApConfigStore;
        this.mMode = softApModeConfiguration.getTargetMode();
        WifiConfiguration wifiConfiguration = softApModeConfiguration.getWifiConfiguration();
        if (wifiConfiguration == null) {
            this.mApConfig = this.mWifiApConfigStore.getApConfiguration();
        } else {
            this.mApConfig = wifiConfiguration;
        }
        this.mWifiMetrics = wifiMetrics;
        this.mStateMachine = new SoftApStateMachine(looper);
    }

    @Override
    public void start() {
        this.mStateMachine.sendMessage(0, this.mApConfig);
    }

    @Override
    public void stop() {
        Log.d(TAG, " currentstate: " + getCurrentStateName());
        if (this.mApInterfaceName != null) {
            if (this.mIfaceIsUp) {
                updateApState(10, 13, 0);
            } else {
                updateApState(10, 12, 0);
            }
        }
        this.mStateMachine.quitNow();
    }

    @Override
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("--Dump of SoftApManager--");
        printWriter.println("current StateMachine mode: " + getCurrentStateName());
        printWriter.println("mApInterfaceName: " + this.mApInterfaceName);
        printWriter.println("mIfaceIsUp: " + this.mIfaceIsUp);
        printWriter.println("mMode: " + this.mMode);
        printWriter.println("mCountryCode: " + this.mCountryCode);
        if (this.mApConfig != null) {
            printWriter.println("mApConfig.SSID: " + this.mApConfig.SSID);
            printWriter.println("mApConfig.apBand: " + this.mApConfig.apBand);
            printWriter.println("mApConfig.hiddenSSID: " + this.mApConfig.hiddenSSID);
        } else {
            printWriter.println("mApConfig: null");
        }
        printWriter.println("mNumAssociatedStations: " + this.mNumAssociatedStations);
        printWriter.println("mTimeoutEnabled: " + this.mTimeoutEnabled);
        printWriter.println("mReportedFrequency: " + this.mReportedFrequency);
        printWriter.println("mReportedBandwidth: " + this.mReportedBandwidth);
    }

    private String getCurrentStateName() {
        IState currentState = this.mStateMachine.getCurrentState();
        if (currentState != null) {
            return currentState.getName();
        }
        return "StateMachine not active";
    }

    public List<HotspotClient> getHotspotClientsList() {
        ArrayList arrayList = new ArrayList();
        synchronized (this.mHotspotClients) {
            Iterator<HotspotClient> it = this.mHotspotClients.values().iterator();
            while (it.hasNext()) {
                arrayList.add(new HotspotClient(it.next()));
            }
        }
        return arrayList;
    }

    public boolean syncBlockClient(HotspotClient hotspotClient) {
        boolean zBlockClient;
        synchronized (this.mHotspotClients) {
            zBlockClient = MtkHostapdHal.blockClient(hotspotClient.deviceAddress);
            if (zBlockClient) {
                HotspotClient hotspotClient2 = this.mHotspotClients.get(hotspotClient.deviceAddress);
                if (hotspotClient2 != null) {
                    hotspotClient2.isBlocked = true;
                } else {
                    Log.e(TAG, "Failed to get " + hotspotClient.deviceAddress);
                }
                sendClientsChangedBroadcast();
            } else {
                Log.e(TAG, "Failed to block " + hotspotClient.deviceAddress);
            }
        }
        return zBlockClient;
    }

    public boolean syncUnblockClient(HotspotClient hotspotClient) {
        boolean zUnblockClient;
        synchronized (this.mHotspotClients) {
            zUnblockClient = MtkHostapdHal.unblockClient(hotspotClient.deviceAddress);
            if (zUnblockClient) {
                this.mHotspotClients.remove(hotspotClient.deviceAddress);
                sendClientsChangedBroadcast();
            } else {
                Log.e(TAG, "Failed to unblock " + hotspotClient.deviceAddress);
            }
        }
        return zUnblockClient;
    }

    public boolean syncSetAllDevicesAllowed(boolean z, boolean z2) {
        if (!z) {
            synchronized (this.mHotspotClients) {
                initAllowedListIfNecessary();
                if (z2 && this.mHotspotClients.size() > 0) {
                    String str = "";
                    for (HotspotClient hotspotClient : this.mHotspotClients.values()) {
                        if (!hotspotClient.isBlocked && !sAllowedDevices.containsKey(hotspotClient.deviceAddress)) {
                            sAllowedDevices.put(hotspotClient.deviceAddress, new HotspotClient(hotspotClient));
                            str = str + hotspotClient.deviceAddress + "\n";
                        }
                    }
                    if (!str.equals("")) {
                        writeAllowedList();
                        updateAcceptMacFile(str);
                    }
                }
            }
        }
        return MtkHostapdHal.setAllDevicesAllowed(z);
    }

    public static void addDeviceToAllowedList(HotspotClient hotspotClient) {
        StringBuilder sb = new StringBuilder();
        sb.append("addDeviceToAllowedList device = ");
        sb.append(hotspotClient);
        sb.append(", is name null?");
        sb.append(hotspotClient.name == null);
        Log.d(TAG, sb.toString());
        initAllowedListIfNecessary();
        if (!sAllowedDevices.containsKey(hotspotClient.deviceAddress)) {
            sAllowedDevices.put(hotspotClient.deviceAddress, hotspotClient);
        }
        writeAllowedList();
    }

    public void syncAllowDevice(String str) {
        updateAcceptMacFile(str);
    }

    public static void removeDeviceFromAllowedList(String str) {
        Log.d(TAG, "removeDeviceFromAllowedList address = " + str);
        initAllowedListIfNecessary();
        sAllowedDevices.remove(str);
        writeAllowedList();
    }

    public void syncDisallowDevice(String str) {
        updateAcceptMacFile("-" + str);
    }

    public static List<HotspotClient> getAllowedDevices() {
        Log.d(TAG, "getAllowedDevices");
        initAllowedListIfNecessary();
        ArrayList arrayList = new ArrayList();
        for (HotspotClient hotspotClient : sAllowedDevices.values()) {
            arrayList.add(new HotspotClient(hotspotClient));
            Log.d(TAG, "device = " + hotspotClient);
        }
        return arrayList;
    }

    private static void initAllowedListIfNecessary() {
        if (sAllowedDevices == null) {
            sAllowedDevices = new LinkedHashMap<>();
            try {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(ALLOWED_LIST_FILE));
                String line = bufferedReader.readLine();
                while (line != null) {
                    String[] strArrSplit = line.split("\t");
                    if (strArrSplit != null) {
                        String str = strArrSplit[0];
                        sAllowedDevices.put(str, new HotspotClient(str, strArrSplit[1].equals("1"), strArrSplit.length == 3 ? strArrSplit[2] : ""));
                        line = bufferedReader.readLine();
                    }
                }
                bufferedReader.close();
            } catch (IOException e) {
                Log.e(TAG, e.toString(), new Throwable("initAllowedListIfNecessary"));
            }
        }
    }

    private static void writeAllowedList() {
        String str = "";
        for (HotspotClient hotspotClient : sAllowedDevices.values()) {
            String str2 = hotspotClient.isBlocked ? "1" : "0";
            str = hotspotClient.name != null ? str + hotspotClient.deviceAddress + "\t" + str2 + "\t" + hotspotClient.name + "\n" : str + hotspotClient.deviceAddress + "\t" + str2 + "\n";
        }
        Log.d(TAG, "writeAllowedLis content = " + str);
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(ALLOWED_LIST_FILE));
            bufferedWriter.write(str);
            bufferedWriter.close();
        } catch (IOException e) {
            Log.e(TAG, e.toString(), new Throwable("writeAllowedList"));
        }
    }

    private void updateAcceptMacFile(String str) {
        Log.d(TAG, "updateAllowedList content = " + str);
        MtkHostapdHal.updateAllowedList(str);
    }

    private void updateApState(int i, int i2, int i3) {
        this.mCallback.onStateChanged(i, i3);
        Intent intent = new Intent("android.net.wifi.WIFI_AP_STATE_CHANGED");
        intent.addFlags(67108864);
        intent.putExtra("wifi_state", i);
        intent.putExtra("previous_wifi_state", i2);
        if (i == 14) {
            intent.putExtra("wifi_ap_error_code", i3);
        }
        intent.putExtra("wifi_ap_interface_name", this.mApInterfaceName);
        intent.putExtra("wifi_ap_mode", this.mMode);
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    private int startSoftAp(WifiConfiguration wifiConfiguration) {
        int i;
        if (wifiConfiguration == null || wifiConfiguration.SSID == null) {
            Log.e(TAG, "Unable to start soft AP without valid configuration");
            return 2;
        }
        WifiConfiguration wifiConfiguration2 = new WifiConfiguration(wifiConfiguration);
        if (this.mCountryCode != null && !this.mWifiNative.setCountryCodeHal(this.mApInterfaceName, this.mCountryCode.toUpperCase(Locale.ROOT)) && wifiConfiguration.apBand == 1) {
            Log.e(TAG, "Failed to set country code, required for setting up soft ap in 5GHz");
            return 2;
        }
        WifiManager wifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        if (wifiManager.getCurrentNetwork() != null) {
            int iConvertFrequencyToChannel = ApConfigUtil.convertFrequencyToChannel(wifiManager.getConnectionInfo().getFrequency());
            Log.e(TAG, "[STA+SAP] Need to config channel for STA+SAP case, getCurrentNetwork = " + wifiManager.getCurrentNetwork() + ", staChannel = " + iConvertFrequencyToChannel + ", Build.HARDWARE = " + Build.HARDWARE);
            if (Build.HARDWARE.equals("mt6779")) {
                if ((iConvertFrequencyToChannel >= 1 && iConvertFrequencyToChannel <= 14 && wifiConfiguration2.apBand == 0) || (iConvertFrequencyToChannel >= 34 && wifiConfiguration2.apBand == 1)) {
                    wifiConfiguration2.apChannel = iConvertFrequencyToChannel;
                }
            } else if (iConvertFrequencyToChannel >= 1 && iConvertFrequencyToChannel <= 14) {
                wifiConfiguration2.apBand = 0;
                wifiConfiguration2.apChannel = iConvertFrequencyToChannel;
            } else if (iConvertFrequencyToChannel >= 34) {
                wifiConfiguration2.apBand = 1;
                wifiConfiguration2.apChannel = iConvertFrequencyToChannel;
            }
            Log.e(TAG, "[STA+SAP] apBand = " + wifiConfiguration2.apBand + ", apChannel = " + wifiConfiguration2.apChannel);
        }
        if (this.mWifiNative.getClientInterfaceName() != null) {
            WifiP2pManager wifiP2pManager = (WifiP2pManager) this.mContext.getSystemService("wifip2p");
            WifiP2pManager.Channel channelInitialize = wifiP2pManager.initialize(this.mContext, this.mLooper, null);
            wifiP2pManager.removeGroup(channelInitialize, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.i(MtkSoftApManager.TAG, "[STA+SAP] Disconnect p2p successfully");
                }

                @Override
                public void onFailure(int i2) {
                    Log.i(MtkSoftApManager.TAG, "[STA+SAP] Disconnect p2p failed, reason = " + i2);
                }
            });
            wifiP2pManager.cancelConnect(channelInitialize, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.i(MtkSoftApManager.TAG, "[STA+SAP] Cancel connect p2p successfully");
                }

                @Override
                public void onFailure(int i2) {
                    Log.i(MtkSoftApManager.TAG, "[STA+SAP] Cancel connect p2p failed, reason = " + i2);
                }
            });
        }
        int iUpdateApChannelConfig = ApConfigUtil.updateApChannelConfig(this.mWifiNative, this.mCountryCode, this.mWifiApConfigStore.getAllowed2GChannel(), wifiConfiguration2);
        if (iUpdateApChannelConfig != 0) {
            Log.e(TAG, "Failed to update AP band and channel");
            return iUpdateApChannelConfig;
        }
        if (wifiConfiguration2.hiddenSSID) {
            Log.d(TAG, "SoftAP is a hidden network");
        }
        String str = SystemProperties.get("wifi.tethering.channel");
        if (str != null && str.length() > 0 && (i = Integer.parseInt(str)) >= 0) {
            wifiConfiguration2.apChannel = i;
        }
        if (!this.mWifiNative.startSoftAp(this.mApInterfaceName, wifiConfiguration2, this.mSoftApListener)) {
            Log.e(TAG, "Soft AP start failed");
            return 2;
        }
        if (!MtkHostapdHal.registerCallback(new MtkHostapdHalCallback())) {
            Log.d(TAG, "Failed to register MtkHostapdHalCallback");
            return 2;
        }
        Log.d(TAG, "Soft AP is started");
        return 0;
    }

    private void stopSoftAp() {
        this.mWifiNative.teardownInterface(this.mApInterfaceName);
        Log.d(TAG, "Soft AP is stopped");
    }

    private void sendClientsChangedBroadcast() {
        Intent intent = new Intent("android.net.wifi.WIFI_HOTSPOT_CLIENTS_CHANGED");
        intent.addFlags(67108864);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void sendClientsIpReadyBroadcast(String str, String str2, String str3) {
        Intent intent = new Intent("android.net.wifi.WIFI_HOTSPOT_CLIENTS_IP_READY");
        intent.addFlags(67108864);
        intent.putExtra("deviceAddress", str);
        intent.putExtra("ipAddress", str2);
        intent.putExtra("deviceName", str3);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    private class SoftApStateMachine extends StateMachine {
        public static final int CMD_INTERFACE_DESTROYED = 7;
        public static final int CMD_INTERFACE_DOWN = 8;
        public static final int CMD_INTERFACE_STATUS_CHANGED = 3;
        public static final int CMD_NO_ASSOCIATED_STATIONS_TIMEOUT = 5;
        public static final int CMD_NUM_ASSOCIATED_STATIONS_CHANGED = 4;
        public static final int CMD_POLL_IP_ADDRESS = 100;
        public static final int CMD_SOFT_AP_CHANNEL_SWITCHED = 9;
        public static final int CMD_START = 0;
        public static final int CMD_TIMEOUT_TOGGLE_CHANGED = 6;
        private static final int POLL_IP_ADDRESS_INTERVAL_MSECS = 2000;
        private static final int POLL_IP_TIMES = 15;
        private static final int Wifi_FAILURE = -1;
        private static final int Wifi_SUCCESS = 1;
        private final State mIdleState;
        private final State mStartedState;
        private final WifiManager mWifiManager;
        private final WifiNative.InterfaceCallback mWifiNativeInterfaceCallback;

        SoftApStateMachine(Looper looper) {
            super(MtkSoftApManager.TAG, looper);
            this.mIdleState = new IdleState();
            this.mStartedState = new StartedState();
            this.mWifiNativeInterfaceCallback = new WifiNative.InterfaceCallback() {
                @Override
                public void onDestroyed(String str) {
                    if (MtkSoftApManager.this.mApInterfaceName != null && MtkSoftApManager.this.mApInterfaceName.equals(str)) {
                        SoftApStateMachine.this.sendMessage(7);
                    }
                }

                @Override
                public void onUp(String str) {
                    if (MtkSoftApManager.this.mApInterfaceName != null && MtkSoftApManager.this.mApInterfaceName.equals(str)) {
                        SoftApStateMachine.this.sendMessage(3, 1);
                    }
                }

                @Override
                public void onDown(String str) {
                    if (MtkSoftApManager.this.mApInterfaceName != null && MtkSoftApManager.this.mApInterfaceName.equals(str)) {
                        SoftApStateMachine.this.sendMessage(3, 0);
                    }
                }
            };
            addState(this.mIdleState);
            addState(this.mStartedState);
            setInitialState(this.mIdleState);
            start();
            this.mWifiManager = (WifiManager) MtkSoftApManager.this.mContext.getSystemService("wifi");
        }

        private class IdleState extends State {
            private IdleState() {
            }

            public void enter() {
                MtkSoftApManager.this.mApInterfaceName = null;
                MtkSoftApManager.this.mIfaceIsUp = false;
            }

            public boolean processMessage(Message message) {
                if (message.what == 0) {
                    MtkSoftApManager.this.mApInterfaceName = MtkSoftApManager.this.mWifiNative.setupInterfaceForSoftApMode(SoftApStateMachine.this.mWifiNativeInterfaceCallback);
                    if (!TextUtils.isEmpty(MtkSoftApManager.this.mApInterfaceName)) {
                        MtkSoftApManager.this.updateApState(12, 11, 0);
                        int iStartSoftAp = MtkSoftApManager.this.startSoftAp((WifiConfiguration) message.obj);
                        if (iStartSoftAp != 0) {
                            int i = iStartSoftAp == 1 ? 1 : 0;
                            MtkSoftApManager.this.updateApState(14, 12, i);
                            MtkSoftApManager.this.stopSoftAp();
                            MtkSoftApManager.this.mWifiMetrics.incrementSoftApStartResult(false, i);
                        } else {
                            MtkWifiApMonitor.registerHandler(MtkSoftApManager.this.mApInterfaceName, 147498, SoftApStateMachine.this.getHandler());
                            MtkWifiApMonitor.registerHandler(MtkSoftApManager.this.mApInterfaceName, 147497, SoftApStateMachine.this.getHandler());
                            MtkWifiApMonitor.startMonitoring(MtkSoftApManager.this.mApInterfaceName);
                            SoftApStateMachine.this.transitionTo(SoftApStateMachine.this.mStartedState);
                        }
                    } else {
                        Log.e(MtkSoftApManager.TAG, "setup failure when creating ap interface.");
                        MtkSoftApManager.this.updateApState(14, 11, 0);
                        MtkSoftApManager.this.mWifiMetrics.incrementSoftApStartResult(false, 0);
                    }
                }
                return true;
            }
        }

        private class StartedState extends State {
            private SoftApTimeoutEnabledSettingObserver mSettingObserver;
            private WakeupMessage mSoftApTimeoutMessage;
            private int mTimeoutDelay;

            private StartedState() {
            }

            private class SoftApTimeoutEnabledSettingObserver extends ContentObserver {
                SoftApTimeoutEnabledSettingObserver(Handler handler) {
                    super(handler);
                }

                public void register() {
                    MtkSoftApManager.this.mFrameworkFacade.registerContentObserver(MtkSoftApManager.this.mContext, Settings.Global.getUriFor("soft_ap_timeout_enabled"), true, this);
                    MtkSoftApManager.this.mTimeoutEnabled = getValue();
                }

                public void unregister() {
                    MtkSoftApManager.this.mFrameworkFacade.unregisterContentObserver(MtkSoftApManager.this.mContext, this);
                }

                @Override
                public void onChange(boolean z) {
                    super.onChange(z);
                    MtkSoftApManager.this.mStateMachine.sendMessage(6, getValue() ? 1 : 0);
                }

                private boolean getValue() {
                    return MtkSoftApManager.this.mFrameworkFacade.getIntegerSetting(MtkSoftApManager.this.mContext, "soft_ap_timeout_enabled", 1) == 1;
                }
            }

            private int getConfigSoftApTimeoutDelay() {
                int integer = MtkSoftApManager.this.mContext.getResources().getInteger(R.integer.config_longPressOnPowerDurationMs);
                if (integer < MtkSoftApManager.MIN_SOFT_AP_TIMEOUT_DELAY_MS) {
                    Log.w(MtkSoftApManager.TAG, "Overriding timeout delay with minimum limit value");
                    integer = MtkSoftApManager.MIN_SOFT_AP_TIMEOUT_DELAY_MS;
                }
                Log.d(MtkSoftApManager.TAG, "Timeout delay: " + integer);
                return integer;
            }

            private void scheduleTimeoutMessage() {
                if (!MtkSoftApManager.this.mTimeoutEnabled) {
                    return;
                }
                this.mSoftApTimeoutMessage.schedule(SystemClock.elapsedRealtime() + ((long) this.mTimeoutDelay));
                Log.d(MtkSoftApManager.TAG, "Timeout message scheduled");
            }

            private void cancelTimeoutMessage() {
                this.mSoftApTimeoutMessage.cancel();
                Log.d(MtkSoftApManager.TAG, "Timeout message canceled");
            }

            private void setNumAssociatedStations(int i) {
                if (MtkSoftApManager.this.mNumAssociatedStations != i) {
                    MtkSoftApManager.this.mNumAssociatedStations = i;
                    Log.d(MtkSoftApManager.TAG, "Number of associated stations changed: " + MtkSoftApManager.this.mNumAssociatedStations);
                    if (MtkSoftApManager.this.mCallback != null) {
                        MtkSoftApManager.this.mCallback.onNumClientsChanged(MtkSoftApManager.this.mNumAssociatedStations);
                    } else {
                        Log.e(MtkSoftApManager.TAG, "SoftApCallback is null. Dropping NumClientsChanged event.");
                    }
                    MtkSoftApManager.this.mWifiMetrics.addSoftApNumAssociatedStationsChangedEvent(MtkSoftApManager.this.mNumAssociatedStations, MtkSoftApManager.this.mMode);
                    if (MtkSoftApManager.this.mNumAssociatedStations == 0) {
                        scheduleTimeoutMessage();
                    } else {
                        cancelTimeoutMessage();
                    }
                }
            }

            private void onUpChanged(boolean z) {
                if (z != MtkSoftApManager.this.mIfaceIsUp) {
                    MtkSoftApManager.this.mIfaceIsUp = z;
                    if (z) {
                        Log.d(MtkSoftApManager.TAG, "SoftAp is ready for use");
                        MtkSoftApManager.this.updateApState(13, 12, 0);
                        MtkSoftApManager.this.mWifiMetrics.incrementSoftApStartResult(true, 0);
                        if (MtkSoftApManager.this.mCallback != null) {
                            MtkSoftApManager.this.mCallback.onNumClientsChanged(MtkSoftApManager.this.mNumAssociatedStations);
                        }
                    } else {
                        SoftApStateMachine.this.sendMessage(8);
                    }
                    MtkSoftApManager.this.mWifiMetrics.addSoftApUpChangedEvent(z, MtkSoftApManager.this.mMode);
                }
            }

            public void enter() {
                MtkSoftApManager.this.mIfaceIsUp = false;
                onUpChanged(MtkSoftApManager.this.mWifiNative.isInterfaceUp(MtkSoftApManager.this.mApInterfaceName));
                this.mTimeoutDelay = getConfigSoftApTimeoutDelay();
                Handler handler = MtkSoftApManager.this.mStateMachine.getHandler();
                this.mSoftApTimeoutMessage = new WakeupMessage(MtkSoftApManager.this.mContext, handler, MtkSoftApManager.SOFT_AP_SEND_MESSAGE_TIMEOUT_TAG, 5);
                this.mSettingObserver = new SoftApTimeoutEnabledSettingObserver(handler);
                if (this.mSettingObserver != null) {
                    this.mSettingObserver.register();
                }
                Log.d(MtkSoftApManager.TAG, "Resetting num stations on start");
                MtkSoftApManager.this.mNumAssociatedStations = 0;
                scheduleTimeoutMessage();
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction("android.net.wifi.p2p.CONNECTION_STATE_CHANGE");
                MtkSoftApManager.this.mContext.registerReceiver(MtkSoftApManager.this.mWifiP2pReceiver, intentFilter);
            }

            public void exit() {
                if (MtkSoftApManager.this.mApInterfaceName != null) {
                    MtkSoftApManager.this.stopSoftAp();
                }
                if (this.mSettingObserver != null) {
                    this.mSettingObserver.unregister();
                }
                Log.d(MtkSoftApManager.TAG, "Resetting num stations on stop");
                MtkSoftApManager.this.mNumAssociatedStations = 0;
                cancelTimeoutMessage();
                MtkSoftApManager.this.mWifiMetrics.addSoftApUpChangedEvent(false, MtkSoftApManager.this.mMode);
                MtkSoftApManager.this.updateApState(11, 10, 0);
                MtkWifiApMonitor.deregisterAllHandler();
                MtkWifiApMonitor.stopMonitoring(MtkSoftApManager.this.mApInterfaceName);
                synchronized (MtkSoftApManager.this.mHotspotClients) {
                    MtkSoftApManager.this.mHotspotClients.clear();
                }
                MtkSoftApManager.this.sendClientsChangedBroadcast();
                MtkSoftApManager.this.mContext.unregisterReceiver(MtkSoftApManager.this.mWifiP2pReceiver);
                MtkSoftApManager.this.mApInterfaceName = null;
                MtkSoftApManager.this.mIfaceIsUp = false;
                MtkSoftApManager.this.mStateMachine.quitNow();
            }

            public boolean processMessage(Message message) {
                int i = message.what;
                if (i != 0) {
                    if (i != 100) {
                        switch (i) {
                            case 3:
                                onUpChanged(message.arg1 == 1);
                                break;
                            case 4:
                                if (message.arg1 < 0) {
                                    Log.e(MtkSoftApManager.TAG, "Invalid number of associated stations: " + message.arg1);
                                } else {
                                    Log.d(MtkSoftApManager.TAG, "Setting num stations on CMD_NUM_ASSOCIATED_STATIONS_CHANGED");
                                    setNumAssociatedStations(message.arg1);
                                }
                                break;
                            case 5:
                                if (MtkSoftApManager.this.mTimeoutEnabled) {
                                    if (MtkSoftApManager.this.mNumAssociatedStations != 0) {
                                        Log.wtf(MtkSoftApManager.TAG, "Timeout message received but has clients. Dropping.");
                                    } else {
                                        Log.i(MtkSoftApManager.TAG, "Timeout message received. Stopping soft AP.");
                                        MtkSoftApManager.this.updateApState(10, 13, 0);
                                        SoftApStateMachine.this.transitionTo(SoftApStateMachine.this.mIdleState);
                                    }
                                } else {
                                    Log.wtf(MtkSoftApManager.TAG, "Timeout message received while timeout is disabled. Dropping.");
                                }
                                break;
                            case 6:
                                boolean z = message.arg1 == 1;
                                if (MtkSoftApManager.this.mTimeoutEnabled != z) {
                                    MtkSoftApManager.this.mTimeoutEnabled = z;
                                    if (!MtkSoftApManager.this.mTimeoutEnabled) {
                                        cancelTimeoutMessage();
                                    }
                                    if (MtkSoftApManager.this.mTimeoutEnabled && MtkSoftApManager.this.mNumAssociatedStations == 0) {
                                        scheduleTimeoutMessage();
                                    }
                                }
                                break;
                            case 7:
                                Log.d(MtkSoftApManager.TAG, "Interface was cleanly destroyed.");
                                MtkSoftApManager.this.updateApState(10, 13, 0);
                                MtkSoftApManager.this.mApInterfaceName = null;
                                SoftApStateMachine.this.transitionTo(SoftApStateMachine.this.mIdleState);
                                break;
                            case 8:
                                Log.w(MtkSoftApManager.TAG, "interface error, stop and report failure");
                                MtkSoftApManager.this.updateApState(14, 13, 0);
                                MtkSoftApManager.this.updateApState(10, 14, 0);
                                SoftApStateMachine.this.transitionTo(SoftApStateMachine.this.mIdleState);
                                break;
                            case 9:
                                MtkSoftApManager.this.mReportedFrequency = message.arg1;
                                MtkSoftApManager.this.mReportedBandwidth = message.arg2;
                                Log.d(MtkSoftApManager.TAG, "Channel switched. Frequency: " + MtkSoftApManager.this.mReportedFrequency + " Bandwidth: " + MtkSoftApManager.this.mReportedBandwidth);
                                MtkSoftApManager.this.mWifiMetrics.addSoftApChannelSwitchedEvent(MtkSoftApManager.this.mReportedFrequency, MtkSoftApManager.this.mReportedBandwidth, MtkSoftApManager.this.mMode);
                                int[] array = new int[0];
                                if (MtkSoftApManager.this.mApConfig.apBand == 0) {
                                    array = MtkSoftApManager.this.mWifiNative.getChannelsForBand(1);
                                } else if (MtkSoftApManager.this.mApConfig.apBand == 1) {
                                    array = MtkSoftApManager.this.mWifiNative.getChannelsForBand(2);
                                } else if (MtkSoftApManager.this.mApConfig.apBand == -1) {
                                    array = Stream.concat(Arrays.stream(MtkSoftApManager.this.mWifiNative.getChannelsForBand(1)).boxed(), Arrays.stream(MtkSoftApManager.this.mWifiNative.getChannelsForBand(2)).boxed()).mapToInt(new ToIntFunction() {
                                        @Override
                                        public final int applyAsInt(Object obj) {
                                            return Integer.valueOf(((Integer) obj).intValue()).intValue();
                                        }
                                    }).toArray();
                                }
                                if (!ArrayUtils.contains(array, MtkSoftApManager.this.mReportedFrequency)) {
                                    Log.e(MtkSoftApManager.TAG, "Channel does not satisfy user band preference: " + MtkSoftApManager.this.mReportedFrequency);
                                    MtkSoftApManager.this.mWifiMetrics.incrementNumSoftApUserBandPreferenceUnsatisfied();
                                }
                                break;
                            default:
                                switch (i) {
                                    case 147497:
                                        Log.d(MtkSoftApManager.TAG, "AP STA DISCONNECTED:" + message.obj);
                                        String str = (String) message.obj;
                                        synchronized (MtkSoftApManager.this.mHotspotClients) {
                                            HotspotClient hotspotClient = (HotspotClient) MtkSoftApManager.this.mHotspotClients.get(str);
                                            if (hotspotClient != null && !hotspotClient.isBlocked) {
                                                MtkSoftApManager.this.mHotspotClients.remove(str);
                                            }
                                            break;
                                        }
                                        MtkSoftApManager.this.sendClientsChangedBroadcast();
                                        break;
                                    case 147498:
                                        Log.d(MtkSoftApManager.TAG, "AP STA CONNECTED:" + message.obj);
                                        String str2 = (String) message.obj;
                                        synchronized (MtkSoftApManager.this.mHotspotClients) {
                                            if (!MtkSoftApManager.this.mHotspotClients.containsKey(str2)) {
                                                MtkSoftApManager.this.mHotspotClients.put(str2, new HotspotClient(str2, false));
                                            }
                                            break;
                                        }
                                        SoftApStateMachine.this.sendMessageDelayed(100, 1, 0, str2, 2000L);
                                        MtkSoftApManager.this.sendClientsChangedBroadcast();
                                        break;
                                    default:
                                        return false;
                                }
                                break;
                        }
                    } else {
                        String str3 = (String) message.obj;
                        int i2 = message.arg1;
                        String clientIp = SoftApStateMachine.this.mWifiManager.getWifiHotspotManager().getClientIp(str3);
                        String clientDeviceName = SoftApStateMachine.this.mWifiManager.getWifiHotspotManager().getClientDeviceName(str3);
                        Log.d(MtkSoftApManager.TAG, "CMD_POLL_IP_ADDRESS ,deviceAddress = " + message.obj + " ipAddress = " + clientIp + ", count = " + i2);
                        if (clientIp == null && i2 < 15) {
                            SoftApStateMachine.this.sendMessageDelayed(100, i2 + 1, 0, str3, 2000L);
                        } else if (clientIp != null) {
                            MtkSoftApManager.this.sendClientsIpReadyBroadcast(str3, clientIp, clientDeviceName);
                        }
                    }
                }
                return true;
            }
        }
    }
}
