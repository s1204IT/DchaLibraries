package com.mediatek.server.wifi;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiScanner;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.internal.telephony.ITelephony;
import com.android.internal.util.IState;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.wifi.NetworkUpdateResult;
import com.android.server.wifi.ScanDetail;
import com.android.server.wifi.StateChangeResult;
import com.android.server.wifi.WifiConnectivityManager;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.WifiMonitor;
import com.android.server.wifi.WifiNetworkSelector;
import com.android.server.wifi.WifiStateMachine;
import com.mediatek.common.util.OperatorCustomizationFactoryLoader;
import com.mediatek.server.wifi.MtkWifiServiceAdapter;
import com.mediatek.server.wifi.WifiOperatorFactoryBase;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public final class MtkWifiService implements MtkWifiServiceAdapter.IMtkWifiService {
    public static final String SYSUI_PACKAGE_NAME = "com.android.systemui";
    private static final int WIFI_DISABLED = 0;
    private static WifiStateTracker sWifiStateTracker;
    private static WifiStateMachineAdapter sWsmAdapter;
    private Context mContext;
    private static final String TAG = "MtkWifiService";
    private static final int WIFI_DISABLED_AIRPLANE_ON = 3;
    private static final boolean DEBUG = Log.isLoggable(TAG, WIFI_DISABLED_AIRPLANE_ON);
    private static final List<OperatorCustomizationFactoryLoader.OperatorFactoryInfo> sFactoryInfoList = new ArrayList();
    private WifiOperatorFactoryBase.IMtkWifiServiceExt mExt = null;
    private AutoConnectManager mACM = null;
    private BroadcastReceiver mOperatorReceiver = new MtkWifiOpReceiver();

    static {
        sFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("Op01WifiService.apk", "com.mediatek.op.wifi.Op01WifiOperatorFactory", "com.mediatek.server.wifi.op01", "OP01"));
        sWifiStateTracker = new WifiStateTracker();
        sWsmAdapter = null;
    }

    public synchronized WifiOperatorFactoryBase.IMtkWifiServiceExt getOpExt() {
        if (this.mExt == null) {
            WifiOperatorFactoryBase wifiOperatorFactoryBase = (WifiOperatorFactoryBase) OperatorCustomizationFactoryLoader.loadFactory(this.mContext, sFactoryInfoList);
            if (wifiOperatorFactoryBase == null) {
                wifiOperatorFactoryBase = new WifiOperatorFactoryBase();
            }
            log("Factory is : " + wifiOperatorFactoryBase.getClass());
            this.mExt = wifiOperatorFactoryBase.createWifiFwkExt(this.mContext, this);
            this.mExt.init();
        }
        return this.mExt;
    }

    public class MtkWifiOpReceiver extends BroadcastReceiver {
        public MtkWifiOpReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            MtkWifiService.log("mOperatorReceiver.onReceive: " + action);
            if (action.equals("com.mediatek.common.wifi.AUTOCONNECT_SETTINGS_CHANGE")) {
                MtkWifiService.this.getACM().updateAutoConnectSettings(MtkWifiService.sWsmAdapter.getLastNetworkId());
                return;
            }
            if (action.equals("android.net.wifi.WIFI_STATE_CHANGED")) {
                int intExtra = intent.getIntExtra("previous_wifi_state", -1);
                int intExtra2 = intent.getIntExtra("wifi_state", -1);
                MtkWifiService.log("previous state: " + intExtra);
                MtkWifiService.log("previous state: " + intExtra2);
                AutoConnectManager acm = MtkWifiService.this.getACM();
                acm.setWaitForScanResult(false);
                acm.setShowReselectDialog(false);
                if (intExtra2 == 2 && MtkWifiService.this.getOpExt().hasCustomizedAutoConnect()) {
                    MtkWifiService.this.getACM().resetDisconnectNetworkStates();
                }
                MtkWifiService.updateWifiState(intExtra, intExtra2);
                return;
            }
            if (!action.equals("android.net.wifi.STATE_CHANGE")) {
                if (action.equals("com.mediatek.wifi.ACTION_SUSPEND_NOTIFICATION")) {
                    MtkWifiService.this.getOpExt().suspendNotification(intent.getIntExtra("type", -1));
                    return;
                }
                return;
            }
            NetworkInfo.DetailedState detailedState = ((NetworkInfo) intent.getExtra("networkInfo")).getDetailedState();
            MtkWifiService.log("detailed state: " + detailedState);
            AutoConnectManager acm2 = MtkWifiService.this.getACM();
            if (detailedState == NetworkInfo.DetailedState.CONNECTED) {
                acm2.setWaitForScanResult(false);
                acm2.setDisconnectOperation(false);
            } else if (detailedState == NetworkInfo.DetailedState.DISCONNECTED) {
                acm2.handleNetworkDisconnect();
                acm2.setShowReselectDialog(false);
                if (acm2.getNetworkState() == NetworkInfo.DetailedState.CONNECTED) {
                    acm2.setWaitForScanResult(false);
                    acm2.handleWifiDisconnect();
                }
            }
            acm2.setNetworkState(detailedState);
            acm2.setLastNetworkId(MtkWifiService.sWsmAdapter.getLastNetworkId());
        }
    }

    private static class WifiStateTracker {
        int currentState;
        int previousState;

        private WifiStateTracker() {
        }
    }

    private static void updateWifiState(int i, int i2) {
        sWifiStateTracker.previousState = i2;
        sWifiStateTracker.currentState = i;
    }

    public synchronized AutoConnectManager getACM() {
        if (this.mACM == null) {
            this.mACM = new AutoConnectManager(this.mContext, getOpExt());
        }
        return this.mACM;
    }

    public class AutoConnectManager {
        private static final int MAX_RSSI = 256;
        private static final int MIN_RSSI = -200;
        private Context mContext;
        private WifiOperatorFactoryBase.IMtkWifiServiceExt mExt;
        private int mSystemUiUid;
        List<ScanResult> mScanResults = null;
        private List<Integer> mDisconnectNetworks = new ArrayList();
        private boolean mShowReselectDialog = false;
        private boolean mScanForWeakSignal = false;
        private int mDisconnectNetworkId = -1;
        private boolean mIsConnecting = false;
        private boolean mDisconnectOperation = false;
        private boolean mWaitForScanResult = false;
        private long mLastCheckWeakSignalTime = 0;
        private NetworkInfo.DetailedState mNetworkState = NetworkInfo.DetailedState.IDLE;
        private int mLastNetworkId = -1;

        public AutoConnectManager(Context context, WifiOperatorFactoryBase.IMtkWifiServiceExt iMtkWifiServiceExt) {
            this.mSystemUiUid = -1;
            this.mContext = context;
            this.mExt = iMtkWifiServiceExt;
            MtkWifiService.log("AutoConnectManager: mExt is " + this.mExt.getClass());
            try {
                this.mSystemUiUid = context.getPackageManager().getPackageUidAsUser(MtkWifiService.SYSUI_PACKAGE_NAME, 1048576, MtkWifiService.WIFI_DISABLED);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(MtkWifiService.TAG, "Unable to resolve SystemUI's UID.");
            }
        }

        public void setLastNetworkId(int i) {
            this.mLastNetworkId = i;
        }

        public void setNetworkState(NetworkInfo.DetailedState detailedState) {
            this.mNetworkState = detailedState;
        }

        public NetworkInfo.DetailedState getNetworkState() {
            return this.mNetworkState;
        }

        public void addDisconnectNetwork(int i) {
            MtkWifiService.log("addDisconnectNetwork: " + i);
            synchronized (this.mDisconnectNetworks) {
                this.mDisconnectNetworks.add(Integer.valueOf(i));
            }
        }

        public void removeDisconnectNetwork(int i) {
            MtkWifiService.log("removeDisconnectNetwork: " + i);
            synchronized (this.mDisconnectNetworks) {
                this.mDisconnectNetworks.remove(Integer.valueOf(i));
            }
        }

        public void clearDisconnectNetworks() {
            MtkWifiService.log("clearDisconnectNetworks");
            synchronized (this.mDisconnectNetworks) {
                this.mDisconnectNetworks.clear();
            }
        }

        public List<Integer> getDisconnectNetworks() {
            ArrayList arrayList = new ArrayList();
            synchronized (this.mDisconnectNetworks) {
                Iterator<Integer> it = this.mDisconnectNetworks.iterator();
                while (it.hasNext()) {
                    arrayList.add(it.next());
                }
            }
            return arrayList;
        }

        public boolean disableNetwork(int i) {
            return WifiInjector.getInstance().getWifiConfigManager().disableNetwork(i, this.mSystemUiUid);
        }

        public boolean enableNetwork(int i) {
            return WifiInjector.getInstance().getWifiConfigManager().enableNetwork(i, false, this.mSystemUiUid);
        }

        public boolean getShowReselectDialog() {
            return this.mShowReselectDialog;
        }

        public void setShowReselectDialog(boolean z) {
            MtkWifiService.log("setShowReselectDialog: " + this.mShowReselectDialog + " -> " + z);
            this.mShowReselectDialog = z;
        }

        public boolean getScanForWeakSignal() {
            return this.mScanForWeakSignal;
        }

        public void setScanForWeakSignal(boolean z) {
            MtkWifiService.log("setScanForWeakSignal: " + this.mScanForWeakSignal + " -> " + z);
            this.mScanForWeakSignal = z;
        }

        public void setWaitForScanResult(boolean z) {
            MtkWifiService.log("setWaitForScanResult: " + this.mWaitForScanResult + " -> " + z);
            this.mWaitForScanResult = z;
        }

        public boolean getWaitForScanResult() {
            return this.mWaitForScanResult;
        }

        private boolean getDisconnectOperation() {
            return this.mDisconnectOperation;
        }

        private void setDisconnectOperation(boolean z) {
            MtkWifiService.log("setDisconnectOperation: " + this.mDisconnectOperation + " -> " + z);
            new Throwable().printStackTrace();
            this.mDisconnectOperation = z;
        }

        public void showReselectionDialog() {
            setScanForWeakSignal(false);
            Log.d(MtkWifiService.TAG, "showReselectionDialog mDisconnectNetworkId:" + this.mDisconnectNetworkId);
            int highPriorityNetworkId = getHighPriorityNetworkId();
            if (highPriorityNetworkId == -1) {
                return;
            }
            if (this.mExt.shouldAutoConnect()) {
                if (!this.mIsConnecting && !"WpsRunningState".equals(MtkWifiService.sWsmAdapter.getCurrentState().getName())) {
                    MtkWifiService.sWsmAdapter.sendMessage(MtkWifiService.sWsmAdapter.obtainMessage(131126, highPriorityNetworkId, 1));
                    return;
                } else {
                    Log.d(MtkWifiService.TAG, "WiFi is connecting!");
                    return;
                }
            }
            setShowReselectDialog(this.mExt.handleNetworkReselection());
        }

        private int getHighPriorityNetworkId() {
            List<WifiConfiguration> savedNetworks = WifiInjector.getInstance().getWifiConfigManager().getSavedNetworks();
            if (savedNetworks == null || savedNetworks.size() == 0) {
                MtkWifiService.log("ACM: getHighPriorityNetworkId No configured networks");
                return -1;
            }
            LinkedList linkedList = new LinkedList();
            if (this.mScanResults != null && !this.mScanResults.isEmpty()) {
                for (WifiConfiguration wifiConfiguration : savedNetworks) {
                    if (wifiConfiguration.networkId != this.mDisconnectNetworkId) {
                        MtkWifiService.log("ACM: getHighPriorityNetworkId iterate scan result cache");
                        for (ScanResult scanResult : this.mScanResults) {
                            MtkWifiService.log("ACM: network.SSID = " + wifiConfiguration.SSID);
                            MtkWifiService.log("ACM: scanResult.SSID = " + scanResult.SSID);
                            MtkWifiService.log("ACM: getSecurity(network) = " + this.mExt.getSecurity(wifiConfiguration));
                            MtkWifiService.log("ACM: getSecurity(scanResult) = " + this.mExt.getSecurity(scanResult));
                            MtkWifiService.log("ACM: scanResult.level = " + scanResult.level);
                            if (wifiConfiguration.SSID != null && scanResult.SSID != null) {
                                if (wifiConfiguration.SSID.equals("\"" + scanResult.SSID + "\"") && this.mExt.getSecurity(wifiConfiguration) == this.mExt.getSecurity(scanResult) && scanResult.level > -79) {
                                    MtkWifiService.log("ACM: add network to found: " + wifiConfiguration);
                                    linkedList.add(wifiConfiguration);
                                }
                            }
                        }
                        MtkWifiService.log("ACM: getHighPriorityNetworkId iterate scan result cache done");
                    }
                }
            }
            MtkWifiService.log("ACM: found.size() = " + linkedList.size());
            if (linkedList.size() < 2) {
                MtkWifiService.log("ACM: getHighPriorityNetworkId Configured networks number less than two");
                return -1;
            }
            WifiConfiguration wifiConfiguration2 = (WifiConfiguration) Collections.max(linkedList, new Comparator<Object>() {
                @Override
                public int compare(Object obj, Object obj2) {
                    return ((WifiConfiguration) obj2).priority - ((WifiConfiguration) obj).priority;
                }
            });
            Log.d(MtkWifiService.TAG, "Found the highest priority AP, networkId:" + wifiConfiguration2.networkId);
            return wifiConfiguration2.networkId;
        }

        public void clearDisconnectNetworkId() {
            this.mDisconnectNetworkId = -1;
        }

        public int getDisconnectNetworkId() {
            return this.mDisconnectNetworkId;
        }

        public void setDisconnectNetworkId(int i) {
            this.mDisconnectNetworkId = i;
        }

        public void handleScanResults(List<ScanDetail> list, List<ScanDetail> list2) {
            MtkWifiService.log("ACM: handleScanResults enter");
            this.mScanResults = new ArrayList();
            Iterator<ScanDetail> it = list.iterator();
            while (it.hasNext()) {
                this.mScanResults.add(it.next().getScanResult());
            }
            MtkWifiService.log("ACM: handleScanResults scan results cache updated");
            if (this.mExt.hasCustomizedAutoConnect()) {
                MtkWifiService.log("ACM: unsaved size " + list2.size());
                if (list2.isEmpty()) {
                    if (getWaitForScanResult()) {
                        showSwitchDialog();
                    }
                } else {
                    if (isWifiConnecting()) {
                        return;
                    }
                    if (getWaitForScanResult()) {
                        showSwitchDialog();
                    }
                }
            }
            MtkWifiService.log("ACM: handleScanResults exit");
        }

        private boolean isDataAvailable() {
            boolean zHasIccCard;
            try {
                ITelephony iTelephonyAsInterface = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
                TelephonyManager telephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
                if (iTelephonyAsInterface != null && iTelephonyAsInterface.isRadioOn(this.mContext.getPackageName()) && telephonyManager != null) {
                    boolean zHasIccCard2 = telephonyManager.hasIccCard(MtkWifiService.WIFI_DISABLED);
                    if (TelephonyManager.getDefault().getPhoneCount() >= 2) {
                        zHasIccCard = telephonyManager.hasIccCard(1);
                    } else {
                        zHasIccCard = MtkWifiService.WIFI_DISABLED;
                    }
                    return zHasIccCard2 || zHasIccCard;
                }
                return false;
            } catch (RemoteException e) {
                e.printStackTrace();
                return false;
            }
        }

        private void showSwitchDialog() {
            setWaitForScanResult(false);
            if (!getShowReselectDialog() && isDataAvailable()) {
                turnOffDataConnection();
                sendWifiFailoverGprsDialog();
            }
        }

        private boolean isWifiConnecting() {
            return this.mExt.isWifiConnecting(this.mIsConnecting ? MtkWifiService.sWsmAdapter.getWifiInfo().getNetworkId() : -1, getDisconnectNetworks()) || "WpsRunningState".equals(MtkWifiService.sWsmAdapter.getCurrentState().getName());
        }

        public void setIsConnecting(boolean z) {
            this.mIsConnecting = z;
        }

        public boolean getIsConnecting() {
            return this.mIsConnecting;
        }

        public void resetStates() {
            Log.d(MtkWifiService.TAG, "resetStates");
            setDisconnectOperation(false);
            setScanForWeakSignal(false);
            setShowReselectDialog(false);
            setWaitForScanResult(false);
            this.mLastCheckWeakSignalTime = 0L;
            this.mIsConnecting = false;
            resetDisconnectNetworkStates();
        }

        private void resetDisconnectNetworkStates() {
            Log.d(MtkWifiService.TAG, "resetDisconnectNetworkStates");
            if (!this.mExt.shouldAutoConnect()) {
                disableAllNetworks(false);
            } else {
                enableNetworks(getDisconnectNetworks());
            }
            clearDisconnectNetworks();
        }

        private void enableNetworks(List<Integer> list) {
            if (list != null) {
                Iterator<Integer> it = list.iterator();
                while (it.hasNext()) {
                    int iIntValue = it.next().intValue();
                    MtkWifiService.log("enableNetwork: " + iIntValue);
                    if (!enableNetwork(iIntValue)) {
                        Log.e(MtkWifiService.TAG, "enableNetworks: failed to enable network " + iIntValue);
                    }
                }
            }
        }

        private void disableAllNetworks(boolean z) {
            Log.d(MtkWifiService.TAG, "disableAllNetworks, exceptLastNetwork:" + z);
            List<WifiConfiguration> savedNetworks = WifiInjector.getInstance().getWifiConfigManager().getSavedNetworks();
            if (z) {
                if (savedNetworks != null) {
                    for (WifiConfiguration wifiConfiguration : savedNetworks) {
                        if (wifiConfiguration.networkId != MtkWifiService.sWsmAdapter.getLastNetworkId() && wifiConfiguration.status != 1) {
                            disableNetwork(wifiConfiguration.networkId);
                        }
                    }
                    return;
                }
                return;
            }
            if (savedNetworks != null) {
                for (WifiConfiguration wifiConfiguration2 : savedNetworks) {
                    if (wifiConfiguration2.status != 1) {
                        disableNetwork(wifiConfiguration2.networkId);
                    }
                }
            }
        }

        public void updateRSSI(Integer num, int i, int i2) {
            if (this.mExt.hasCustomizedAutoConnect() && num != null && num.intValue() < -85) {
                long jElapsedRealtime = SystemClock.elapsedRealtime();
                boolean zShouldAutoConnect = this.mExt.shouldAutoConnect();
                Log.d(MtkWifiService.TAG, "fetchRssi, ip:" + i + ", mDisconnectOperation:" + this.mDisconnectOperation + ", time:" + jElapsedRealtime + ", lasttime:" + this.mLastCheckWeakSignalTime);
                long j = jElapsedRealtime - this.mLastCheckWeakSignalTime;
                if ((i != 0 && !this.mDisconnectOperation && j > 60000) || (zShouldAutoConnect && j > 10000)) {
                    Log.d(MtkWifiService.TAG, "Rssi < -85, scan to check signal!");
                    this.mLastCheckWeakSignalTime = jElapsedRealtime;
                    this.mDisconnectNetworkId = i2;
                    setScanForWeakSignal(true);
                    MtkWifiService.sWsmAdapter.startScan();
                }
            }
        }

        public void handleNetworkDisconnect() {
            if (this.mExt.hasCustomizedAutoConnect()) {
                MtkWifiService.log("handleNetworkDisconnect, oldState:" + this.mNetworkState + ", mDisconnectOperation:" + getDisconnectOperation());
                if (this.mNetworkState == NetworkInfo.DetailedState.CONNECTED) {
                    this.mDisconnectNetworkId = this.mLastNetworkId;
                    if (!getDisconnectOperation()) {
                        setScanForWeakSignal(true);
                        MtkWifiService.sWsmAdapter.startScan();
                    }
                }
                if (!this.mExt.shouldAutoConnect()) {
                    disableNetwork(this.mLastNetworkId);
                }
                setDisconnectOperation(false);
                this.mLastCheckWeakSignalTime = 0L;
            }
        }

        private boolean isPsDataAvailable() {
            boolean z;
            int length;
            TelephonyManager telephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
            if (telephonyManager == null) {
                MtkWifiService.log("TelephonyManager is null");
                return false;
            }
            int simCount = telephonyManager.getSimCount();
            int i = MtkWifiService.WIFI_DISABLED;
            while (true) {
                if (i < simCount) {
                    if (telephonyManager.getSimState(i) != 5) {
                        i++;
                    } else {
                        z = true;
                        break;
                    }
                } else {
                    z = MtkWifiService.WIFI_DISABLED;
                    break;
                }
            }
            MtkWifiService.log("isSIMReady: " + z);
            if (!z) {
                return false;
            }
            ITelephony iTelephonyAsInterface = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
            if (iTelephonyAsInterface == null) {
                MtkWifiService.log("ITelephony is null");
                return false;
            }
            SubscriptionManager subscriptionManagerFrom = SubscriptionManager.from(this.mContext);
            if (subscriptionManagerFrom == null) {
                MtkWifiService.log("SubscriptionManager is null");
                return false;
            }
            int[] activeSubscriptionIdList = subscriptionManagerFrom.getActiveSubscriptionIdList();
            if (activeSubscriptionIdList != null) {
                length = activeSubscriptionIdList.length;
            } else {
                length = MtkWifiService.WIFI_DISABLED;
            }
            int i2 = MtkWifiService.WIFI_DISABLED;
            ?? IsRadioOnForSubscriber = i2;
            while (i2 < length) {
                try {
                    IsRadioOnForSubscriber = iTelephonyAsInterface.isRadioOnForSubscriber(activeSubscriptionIdList[i2], this.mContext.getPackageName());
                } catch (RemoteException e) {
                    MtkWifiService.log("isRadioOnForSubscriber RemoteException");
                    IsRadioOnForSubscriber = MtkWifiService.WIFI_DISABLED;
                }
                if (IsRadioOnForSubscriber != 0) {
                    break;
                }
                i2++;
                IsRadioOnForSubscriber = IsRadioOnForSubscriber;
            }
            if (IsRadioOnForSubscriber == 0) {
                MtkWifiService.log("All sub Radio OFF");
                return false;
            }
            int i3 = Settings.System.getInt(this.mContext.getContentResolver(), "airplane_mode_on", MtkWifiService.WIFI_DISABLED);
            MtkWifiService.log("airplanMode:" + i3);
            return i3 != 1;
        }

        public void handleWifiDisconnect() {
            if (this.mExt.hasCustomizedAutoConnect()) {
                MtkWifiService.log("handleWifiDisconnect");
                if (Settings.System.getInt(this.mContext.getContentResolver(), "wifi_connect_reminder", MtkWifiService.WIFI_DISABLED) != 0) {
                    MtkWifiService.log("Not ask mode");
                    return;
                }
                boolean zIsPsDataAvailable = isPsDataAvailable();
                MtkWifiService.log("dataAvailable: " + zIsPsDataAvailable);
                if (zIsPsDataAvailable && !hasConnectableAp()) {
                    turnOffDataConnection();
                    sendWifiFailoverGprsDialog();
                }
            }
        }

        private void sendWifiFailoverGprsDialog() {
            Intent intent = new Intent("com.mediatek.intent.WIFI_FAILOVER_GPRS_DIALOG");
            intent.addFlags(67108864);
            intent.setClassName("com.mediatek.server.wifi.op01", "com.mediatek.op.wifi.DataConnectionReceiver");
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
            MtkWifiService.log("ACTION_WIFI_FAILOVER_GPRS_DIALOG sent");
        }

        public void updateAutoConnectSettings(int i) {
            boolean z = this.mIsConnecting || "WpsRunningState".equals(MtkWifiService.sWsmAdapter.getCurrentState().getName());
            Log.d(MtkWifiService.TAG, "updateAutoConnectSettings, isConnecting:" + z);
            List<WifiConfiguration> savedNetworks = WifiInjector.getInstance().getWifiConfigManager().getSavedNetworks();
            if (savedNetworks != null) {
                if (this.mExt.shouldAutoConnect()) {
                    if (!z) {
                        Collections.sort(savedNetworks, new Comparator<WifiConfiguration>() {
                            @Override
                            public int compare(WifiConfiguration wifiConfiguration, WifiConfiguration wifiConfiguration2) {
                                return wifiConfiguration2.priority - wifiConfiguration.priority;
                            }
                        });
                        for (WifiConfiguration wifiConfiguration : savedNetworks) {
                            if (wifiConfiguration.networkId != i) {
                                enableNetwork(wifiConfiguration.networkId);
                            }
                        }
                        return;
                    }
                    return;
                }
                if (!z) {
                    for (WifiConfiguration wifiConfiguration2 : savedNetworks) {
                        if (wifiConfiguration2.networkId != i && wifiConfiguration2.status != 1) {
                            disableNetwork(wifiConfiguration2.networkId);
                        }
                    }
                }
            }
        }

        public boolean preProcessMessage(State state, Message message) {
            MtkWifiService.log("preProcessMessage(" + state.getName() + ", " + message.what + ")");
            String name = state.getName();
            if (((name.hashCode() == -704611580 && name.equals("ConnectModeState")) ? MtkWifiService.WIFI_DISABLED : (byte) -1) == 0) {
                switch (message.what) {
                    case 131125:
                        if (this.mExt.hasCustomizedAutoConnect()) {
                            int i = message.arg1;
                            removeDisconnectNetwork(i);
                            if (i == MtkWifiService.sWsmAdapter.getWifiInfo().getNetworkId()) {
                                setDisconnectOperation(true);
                                setScanForWeakSignal(false);
                            }
                        }
                        return false;
                    case 131126:
                        if (this.mExt.hasCustomizedAutoConnect()) {
                            int i2 = message.arg1;
                            if (!(message.arg2 == 1 ? true : MtkWifiService.WIFI_DISABLED) && !this.mExt.shouldAutoConnect()) {
                                Log.d(MtkWifiService.TAG, "Shouldn't auto connect, ignore the enable network operation!");
                                MtkWifiService.sWsmAdapter.replyToMessage(message, message.what, 1);
                                return true;
                            }
                        }
                        return false;
                    default:
                        return false;
                }
            }
            MtkWifiService.log("State " + state.getName() + " NOT_HANDLED");
            return false;
        }

        public boolean postProcessMessage(State state, Message message, Object... objArr) {
            boolean zHasCredentialChanged;
            int i;
            WifiConfiguration configuredNetwork;
            MtkWifiService.log("postProcessMessage(" + state.getName() + ", " + message.what + ", " + objArr + ")");
            String name = state.getName();
            if (((name.hashCode() == -704611580 && name.equals("ConnectModeState")) ? MtkWifiService.WIFI_DISABLED : (byte) -1) == 0) {
                int i2 = message.what;
                if (i2 != 131126) {
                    if (i2 != 151553) {
                        if (i2 != 151556) {
                            if (i2 == 151569 && this.mExt.hasCustomizedAutoConnect() && (configuredNetwork = WifiInjector.getInstance().getWifiConfigManager().getConfiguredNetwork((i = message.arg1))) != null && configuredNetwork.getNetworkSelectionStatus() != null && !configuredNetwork.getNetworkSelectionStatus().isNetworkEnabled()) {
                                addDisconnectNetwork(i);
                                if (i == MtkWifiService.sWsmAdapter.getWifiInfo().getNetworkId()) {
                                    setDisconnectOperation(true);
                                    setScanForWeakSignal(false);
                                }
                            }
                        } else if (this.mExt.hasCustomizedAutoConnect()) {
                            int i3 = message.arg1;
                            if (WifiInjector.getInstance().getWifiConfigManager().getConfiguredNetwork(i3) == null) {
                                removeDisconnectNetwork(i3);
                                if (i3 == MtkWifiService.sWsmAdapter.getWifiInfo().getNetworkId()) {
                                    setDisconnectOperation(true);
                                    setScanForWeakSignal(false);
                                }
                            }
                        }
                    } else if (this.mExt.hasCustomizedAutoConnect()) {
                        int networkId = message.arg1;
                        NetworkUpdateResult networkUpdateResult = (NetworkUpdateResult) objArr[1];
                        if (networkUpdateResult != null) {
                            networkId = networkUpdateResult.getNetworkId();
                            zHasCredentialChanged = networkUpdateResult.hasCredentialChanged();
                        } else {
                            zHasCredentialChanged = MtkWifiService.WIFI_DISABLED;
                        }
                        if (MtkWifiService.sWsmAdapter.getWifiInfo().getNetworkId() != networkId || zHasCredentialChanged) {
                            setDisconnectOperation(true);
                        }
                        setScanForWeakSignal(false);
                        removeDisconnectNetwork(networkId);
                    }
                } else if (this.mExt.hasCustomizedAutoConnect()) {
                    int i4 = message.arg1;
                    boolean z = message.arg2 == 1 ? true : MtkWifiService.WIFI_DISABLED;
                    boolean zBooleanValue = ((Boolean) objArr[MtkWifiService.WIFI_DISABLED]).booleanValue();
                    if (z && zBooleanValue) {
                        removeDisconnectNetwork(i4);
                        setDisconnectOperation(true);
                        setScanForWeakSignal(false);
                    }
                }
                return false;
            }
            MtkWifiService.log("State " + state.getName() + " NOT_HANDLED");
            return false;
        }

        public void turnOffDataConnection() {
            TelephonyManager telephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
            Settings.System.putLong(this.mContext.getContentResolver(), "last_simid_before_wifi_disconnected", telephonyManager.getDataEnabled() ? 1L : -1L);
            if (telephonyManager != null) {
                telephonyManager.setDataEnabled(false);
            }
        }

        public boolean hasConnectableAp() {
            if (MtkWifiService.this.hasConnectableAp() && this.mExt.hasConnectableAp()) {
                setWaitForScanResult(true);
                return true;
            }
            return false;
        }

        public List<ScanResult> getLatestScanResults() {
            return this.mScanResults;
        }
    }

    public static class WifiStateMachineAdapter {
        static final int BASE = 131072;
        static final int CMD_ENABLE_NETWORK = 131126;
        static final int CMD_REMOVE_NETWORK = 131125;
        static final int FAILURE = -1;
        static final int SUCCESS = 1;
        private final WifiStateMachine mWsm;
        private final Class<?> mWsmCls;

        public WifiStateMachineAdapter(WifiStateMachine wifiStateMachine) {
            this.mWsm = wifiStateMachine;
            this.mWsmCls = wifiStateMachine.getClass();
        }

        public void replyToMessage(Message message, int i) {
            try {
                Method declaredMethod = this.mWsmCls.getDeclaredMethod("replyToMessage", Message.class, Integer.class);
                declaredMethod.setAccessible(true);
                declaredMethod.invoke(this.mWsm, message, Integer.valueOf(i));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void replyToMessage(Message message, int i, int i2) {
            try {
                Method declaredMethod = this.mWsmCls.getDeclaredMethod("replyToMessage", Message.class, Integer.class, Integer.class);
                declaredMethod.setAccessible(true);
                declaredMethod.invoke(this.mWsm, message, Integer.valueOf(i), Integer.valueOf(i2));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void replyToMessage(Message message, int i, Object obj) {
            try {
                Method declaredMethod = this.mWsmCls.getDeclaredMethod("replyToMessage", Message.class, Integer.class, Object.class);
                declaredMethod.setAccessible(true);
                declaredMethod.invoke(this.mWsm, message, Integer.valueOf(i), obj);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public WifiInfo getWifiInfo() {
            return this.mWsm.getWifiInfo();
        }

        public int getLastNetworkId() {
            try {
                try {
                    Field declaredField = this.mWsmCls.getDeclaredField("mLastNetworkId");
                    declaredField.setAccessible(true);
                    return ((Integer) declaredField.get(this.mWsm)).intValue();
                } catch (Exception e) {
                    e.printStackTrace();
                    return FAILURE;
                }
            } catch (Throwable th) {
                return FAILURE;
            }
        }

        public IState getCurrentState() {
            try {
                try {
                    Method declaredMethod = StateMachine.class.getDeclaredMethod("getCurrentState", new Class[MtkWifiService.WIFI_DISABLED]);
                    declaredMethod.setAccessible(true);
                    return (State) declaredMethod.invoke(this.mWsm, new Object[MtkWifiService.WIFI_DISABLED]);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            } catch (Throwable th) {
                return null;
            }
        }

        public void startScan() {
            WifiInjector wifiInjector = WifiInjector.getInstance();
            WifiScanner.ScanSettings scanSettings = new WifiScanner.ScanSettings();
            scanSettings.type = 2;
            scanSettings.band = 7;
            scanSettings.reportEvents = MtkWifiService.WIFI_DISABLED_AIRPLANE_ON;
            scanSettings.numBssidsPerScan = MtkWifiService.WIFI_DISABLED;
            List listRetrieveHiddenNetworkList = wifiInjector.getWifiConfigManager().retrieveHiddenNetworkList();
            scanSettings.hiddenNetworks = (WifiScanner.ScanSettings.HiddenNetwork[]) listRetrieveHiddenNetworkList.toArray(new WifiScanner.ScanSettings.HiddenNetwork[listRetrieveHiddenNetworkList.size()]);
            wifiInjector.getWifiScanner().startScan(scanSettings, new WifiScanner.ScanListener() {
                public void onSuccess() {
                }

                public void onFailure(int i, String str) {
                }

                public void onResults(WifiScanner.ScanData[] scanDataArr) {
                }

                public void onFullResult(ScanResult scanResult) {
                }

                public void onPeriodChanged(int i) {
                }
            });
        }

        public void sendMessage(int i) {
            this.mWsm.sendMessage(i);
        }

        public void sendMessage(Message message) {
            this.mWsm.sendMessage(message);
        }

        public Message obtainMessage(int i, int i2, int i3) {
            return this.mWsm.obtainMessage(i, i2, i3);
        }

        public String getInterfaceName() {
            try {
                try {
                    Field declaredField = this.mWsmCls.getDeclaredField("mInterfaceName");
                    declaredField.setAccessible(true);
                    return (String) declaredField.get(this.mWsm);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            } catch (Throwable th) {
                return null;
            }
        }
    }

    public MtkWifiService(Context context) {
        this.mContext = null;
        log("[MtkWifiService] " + context);
        this.mContext = context;
    }

    public void initialize() {
        log("[initialize]");
        WifiOperatorFactoryBase.IMtkWifiServiceExt opExt = getOpExt();
        WifiInjector wifiInjector = WifiInjector.getInstance();
        if (opExt.hasNetworkSelection() == 1) {
            try {
                Field declaredField = WifiInjector.class.getDeclaredField("mWifiNetworkSelector");
                declaredField.setAccessible(true);
                ((WifiNetworkSelector) declaredField.get(wifiInjector)).registerNetworkEvaluator(new MtkNetworkEvaluator(this.mContext, wifiInjector.getWifiConfigManager(), this), WIFI_DISABLED);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        int iDefaultFrameworkScanIntervalMs = opExt.defaultFrameworkScanIntervalMs();
        log("defaultFrameworkScanIntervalMs: " + iDefaultFrameworkScanIntervalMs);
        log("PERIODIC_SCAN_INTERVAL_MS: 20000");
        try {
            Field declaredField2 = WifiConnectivityManager.class.getDeclaredField("PERIODIC_SCAN_INTERVAL_MS");
            Field declaredField3 = Field.class.getDeclaredField("accessFlags");
            declaredField3.setAccessible(true);
            log("accessFlags: " + declaredField3.getInt(declaredField2));
            declaredField3.setInt(declaredField2, declaredField2.getModifiers() & (-17));
            log("accessFlags: " + declaredField3.getInt(declaredField2));
            log("old: " + declaredField2.getInt(null));
            declaredField2.setInt(null, iDefaultFrameworkScanIntervalMs);
            log("new: " + declaredField2.getInt(null));
        } catch (Exception e2) {
            e2.printStackTrace();
        }
        log("PERIODIC_SCAN_INTERVAL_MS: 20000");
        if (20000 != iDefaultFrameworkScanIntervalMs) {
            Log.i(TAG, "Failed to modify PERIODIC_SCAN_INTERVAL_MS");
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.mediatek.common.wifi.AUTOCONNECT_SETTINGS_CHANGE");
        intentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        intentFilter.addAction("android.net.wifi.STATE_CHANGE");
        intentFilter.addAction("com.mediatek.wifi.ACTION_SUSPEND_NOTIFICATION");
        this.mContext.registerReceiver(this.mOperatorReceiver, intentFilter);
        sWsmAdapter = new WifiStateMachineAdapter(wifiInjector.getWifiStateMachine());
        Log.d(TAG, "initialize done");
        Handler handler = new Handler() {
            @Override
            public final void handleMessage(Message message) {
                SupplicantState supplicantState;
                Log.d(MtkWifiService.TAG, "Supplicant message: " + message.what);
                int i = message.what;
                if (i == 147457) {
                    MtkWifiService.this.getACM().resetStates();
                    return;
                }
                if (i == 147462) {
                    StateChangeResult stateChangeResult = (StateChangeResult) message.obj;
                    try {
                        Field declaredField4 = StateChangeResult.class.getDeclaredField("state");
                        declaredField4.setAccessible(true);
                        supplicantState = (SupplicantState) declaredField4.get(stateChangeResult);
                    } catch (Exception e3) {
                        e3.printStackTrace();
                        supplicantState = null;
                    }
                    MtkWifiService.this.getACM().setIsConnecting(SupplicantState.isConnecting(supplicantState));
                    return;
                }
                Log.e(MtkWifiService.TAG, "Invalid message: " + message.what);
            }
        };
        String interfaceName = sWsmAdapter.getInterfaceName();
        WifiMonitor wifiMonitor = wifiInjector.getWifiMonitor();
        wifiMonitor.registerHandler(interfaceName, 147462, handler);
        wifiMonitor.registerHandler(interfaceName, 147457, handler);
    }

    private int getPersistedWifiState() {
        ContentResolver contentResolver = this.mContext.getContentResolver();
        try {
            return Settings.Global.getInt(contentResolver, "wifi_on");
        } catch (Settings.SettingNotFoundException e) {
            Settings.Global.putInt(contentResolver, "wifi_on", WIFI_DISABLED);
            return WIFI_DISABLED;
        }
    }

    public boolean hasConnectableAp() {
        int persistedWifiState = getPersistedWifiState();
        return (persistedWifiState == 0 || persistedWifiState == WIFI_DISABLED_AIRPLANE_ON) ? false : true;
    }

    public static void log(String str) {
        if (DEBUG) {
            Log.d(TAG, str);
        }
    }

    public void handleScanResults(List<ScanDetail> list, List<ScanDetail> list2) {
        getACM().handleScanResults(list, list2);
    }

    public void updateRSSI(Integer num, int i, int i2) {
        getACM().updateRSSI(num, i, i2);
    }

    public boolean preProcessMessage(State state, Message message) {
        return getACM().preProcessMessage(state, message);
    }

    public boolean postProcessMessage(State state, Message message, Object... objArr) {
        return getACM().postProcessMessage(state, message, objArr);
    }

    public List<ScanResult> getLatestScanResults() {
        return getACM().getLatestScanResults();
    }
}
