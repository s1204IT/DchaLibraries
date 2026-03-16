package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.util.AsyncChannel;
import com.android.systemui.DemoMode;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.MobileDataControllerImpl;
import com.android.systemui.statusbar.policy.NetworkController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class NetworkControllerImpl extends BroadcastReceiver implements DemoMode, NetworkController {
    private final AccessPointControllerImpl mAccessPoints;
    private boolean mAirplaneMode;
    private boolean mBluetoothTethered;
    private ArrayList<CarrierLabelListener> mCarrierListeners;
    private Config mConfig;
    private boolean mConnected;
    private final BitSet mConnectedTransports;
    private final ConnectivityManager mConnectivityManager;
    private final Context mContext;
    private List<SubscriptionInfo> mCurrentSubscriptions;
    private int mCurrentUserId;
    private MobileSignalController mDefaultSignalController;
    private int mDemoInetCondition;
    private boolean mDemoMode;
    private WifiSignalController.WifiState mDemoWifiState;
    private ArrayList<EmergencyListener> mEmergencyListeners;
    private boolean mEthernetConnected;
    private final boolean mHasMobileDataFeature;
    private boolean mHasNoSims;
    private boolean mInetCondition;
    boolean mListening;
    private Locale mLocale;
    private final MobileDataControllerImpl mMobileDataController;
    final Map<Integer, MobileSignalController> mMobileSignalControllers;
    private final TelephonyManager mPhone;
    private ArrayList<SignalCluster> mSignalClusters;
    private ArrayList<NetworkController.NetworkSignalChangedCallback> mSignalsChangedCallbacks;
    private final SubscriptionManager.OnSubscriptionsChangedListener mSubscriptionListener;
    private final SubscriptionManager mSubscriptionManager;
    private final BitSet mValidatedTransports;
    private final WifiManager mWifiManager;
    final WifiSignalController mWifiSignalController;
    static final boolean DEBUG = Log.isLoggable("NetworkController", 3);
    static final boolean CHATTY = Log.isLoggable("NetworkController.Chat", 3);

    public interface CarrierLabelListener {
        void setCarrierLabel(String str);
    }

    public interface EmergencyListener {
        void setEmergencyCallsOnly(boolean z);
    }

    public interface SignalCluster {
        void setIsAirplaneMode(boolean z, int i, int i2);

        void setMobileDataIndicators(boolean z, int i, int i2, String str, String str2, boolean z2, int i3);

        void setNoSims(boolean z);

        void setSubs(List<SubscriptionInfo> list);

        void setWifiIndicators(boolean z, int i, String str);
    }

    public NetworkControllerImpl(Context context) {
        this(context, (ConnectivityManager) context.getSystemService("connectivity"), (TelephonyManager) context.getSystemService("phone"), (WifiManager) context.getSystemService("wifi"), SubscriptionManager.from(context), Config.readConfig(context), new AccessPointControllerImpl(context), new MobileDataControllerImpl(context));
        registerListeners();
    }

    NetworkControllerImpl(Context context, ConnectivityManager connectivityManager, TelephonyManager telephonyManager, WifiManager wifiManager, SubscriptionManager subManager, Config config, AccessPointControllerImpl accessPointController, MobileDataControllerImpl mobileDataController) {
        this.mMobileSignalControllers = new HashMap();
        this.mBluetoothTethered = false;
        this.mEthernetConnected = false;
        this.mConnected = false;
        this.mConnectedTransports = new BitSet();
        this.mValidatedTransports = new BitSet();
        this.mAirplaneMode = false;
        this.mLocale = null;
        this.mCurrentSubscriptions = new ArrayList();
        this.mEmergencyListeners = new ArrayList<>();
        this.mCarrierListeners = new ArrayList<>();
        this.mSignalClusters = new ArrayList<>();
        this.mSignalsChangedCallbacks = new ArrayList<>();
        this.mSubscriptionListener = new SubscriptionManager.OnSubscriptionsChangedListener() {
            @Override
            public void onSubscriptionsChanged() {
                NetworkControllerImpl.this.updateMobileControllers();
            }
        };
        this.mContext = context;
        this.mConfig = config;
        this.mSubscriptionManager = subManager;
        this.mConnectivityManager = connectivityManager;
        this.mHasMobileDataFeature = this.mConnectivityManager.isNetworkSupported(0);
        this.mPhone = (TelephonyManager) this.mContext.getSystemService("phone");
        this.mWifiManager = wifiManager;
        this.mLocale = this.mContext.getResources().getConfiguration().locale;
        this.mAccessPoints = accessPointController;
        this.mMobileDataController = mobileDataController;
        this.mMobileDataController.setNetworkController(this);
        this.mMobileDataController.setCallback(new MobileDataControllerImpl.Callback() {
            @Override
            public void onMobileDataEnabled(boolean enabled) {
                NetworkControllerImpl.this.notifyMobileDataEnabled(enabled);
            }
        });
        this.mWifiSignalController = new WifiSignalController(this.mContext, this.mHasMobileDataFeature, this.mSignalsChangedCallbacks, this.mSignalClusters, this);
        updateAirplaneMode(true);
        this.mAccessPoints.setNetworkController(this);
    }

    private void registerListeners() {
        for (MobileSignalController mobileSignalController : this.mMobileSignalControllers.values()) {
            mobileSignalController.registerListener();
        }
        this.mSubscriptionManager.addOnSubscriptionsChangedListener(this.mSubscriptionListener);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.wifi.RSSI_CHANGED");
        filter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        filter.addAction("android.net.wifi.STATE_CHANGE");
        filter.addAction("android.intent.action.SIM_STATE_CHANGED");
        filter.addAction("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED");
        filter.addAction("android.intent.action.ACTION_DEFAULT_VOICE_SUBSCRIPTION_CHANGED");
        filter.addAction("android.provider.Telephony.SPN_STRINGS_UPDATED");
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE_IMMEDIATE");
        filter.addAction("android.net.conn.INET_CONDITION_ACTION");
        filter.addAction("android.intent.action.CONFIGURATION_CHANGED");
        filter.addAction("android.intent.action.AIRPLANE_MODE");
        this.mContext.registerReceiver(this, filter);
        this.mListening = true;
        updateMobileControllers();
    }

    private void unregisterListeners() {
        this.mListening = false;
        for (MobileSignalController mobileSignalController : this.mMobileSignalControllers.values()) {
            mobileSignalController.unregisterListener();
        }
        this.mSubscriptionManager.removeOnSubscriptionsChangedListener(this.mSubscriptionListener);
        this.mContext.unregisterReceiver(this);
    }

    public int getConnectedWifiLevel() {
        return this.mWifiSignalController.getState().level;
    }

    @Override
    public NetworkController.AccessPointController getAccessPointController() {
        return this.mAccessPoints;
    }

    @Override
    public NetworkController.MobileDataController getMobileDataController() {
        return this.mMobileDataController;
    }

    public void addEmergencyListener(EmergencyListener listener) {
        this.mEmergencyListeners.add(listener);
        listener.setEmergencyCallsOnly(isEmergencyOnly());
    }

    public void addCarrierLabel(CarrierLabelListener listener) {
        this.mCarrierListeners.add(listener);
        refreshCarrierLabel();
    }

    private void notifyMobileDataEnabled(boolean enabled) {
        int length = this.mSignalsChangedCallbacks.size();
        for (int i = 0; i < length; i++) {
            this.mSignalsChangedCallbacks.get(i).onMobileDataEnabled(enabled);
        }
    }

    @Override
    public boolean hasMobileDataFeature() {
        return this.mHasMobileDataFeature;
    }

    public boolean hasVoiceCallingFeature() {
        return this.mPhone.getPhoneType() != 0;
    }

    private MobileSignalController getDataController() {
        int dataSubId = SubscriptionManager.getDefaultDataSubId();
        if (!SubscriptionManager.isValidSubscriptionId(dataSubId)) {
            if (DEBUG) {
                Log.e("NetworkController", "No data sim selected");
            }
            return this.mDefaultSignalController;
        }
        if (this.mMobileSignalControllers.containsKey(Integer.valueOf(dataSubId))) {
            return this.mMobileSignalControllers.get(Integer.valueOf(dataSubId));
        }
        if (DEBUG) {
            Log.e("NetworkController", "Cannot find controller for data sub: " + dataSubId);
        }
        return this.mDefaultSignalController;
    }

    public String getMobileNetworkName() {
        MobileSignalController controller = getDataController();
        return controller != null ? controller.getState().networkName : "";
    }

    public boolean isEmergencyOnly() {
        int voiceSubId = SubscriptionManager.getDefaultVoiceSubId();
        if (!SubscriptionManager.isValidSubscriptionId(voiceSubId)) {
            for (MobileSignalController mobileSignalController : this.mMobileSignalControllers.values()) {
                if (!mobileSignalController.isEmergencyOnly()) {
                    return false;
                }
            }
        }
        if (this.mMobileSignalControllers.containsKey(Integer.valueOf(voiceSubId))) {
            return this.mMobileSignalControllers.get(Integer.valueOf(voiceSubId)).isEmergencyOnly();
        }
        if (DEBUG) {
            Log.e("NetworkController", "Cannot find controller for voice sub: " + voiceSubId);
        }
        return true;
    }

    void recalculateEmergency() {
        boolean emergencyOnly = isEmergencyOnly();
        int length = this.mEmergencyListeners.size();
        for (int i = 0; i < length; i++) {
            this.mEmergencyListeners.get(i).setEmergencyCallsOnly(emergencyOnly);
        }
        refreshCarrierLabel();
    }

    public void addSignalCluster(SignalCluster cluster) {
        this.mSignalClusters.add(cluster);
        cluster.setSubs(this.mCurrentSubscriptions);
        cluster.setIsAirplaneMode(this.mAirplaneMode, R.drawable.stat_sys_airplane_mode, R.string.accessibility_airplane_mode);
        cluster.setNoSims(this.mHasNoSims);
        this.mWifiSignalController.notifyListeners();
        for (MobileSignalController mobileSignalController : this.mMobileSignalControllers.values()) {
            mobileSignalController.notifyListeners();
        }
    }

    @Override
    public void addNetworkSignalChangedCallback(NetworkController.NetworkSignalChangedCallback cb) {
        this.mSignalsChangedCallbacks.add(cb);
        cb.onAirplaneModeChanged(this.mAirplaneMode);
        cb.onNoSimVisibleChanged(this.mHasNoSims);
        this.mWifiSignalController.notifyListeners();
        for (MobileSignalController mobileSignalController : this.mMobileSignalControllers.values()) {
            mobileSignalController.notifyListeners();
        }
    }

    @Override
    public void removeNetworkSignalChangedCallback(NetworkController.NetworkSignalChangedCallback cb) {
        this.mSignalsChangedCallbacks.remove(cb);
    }

    @Override
    public void setWifiEnabled(final boolean enabled) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... args) {
                int wifiApState = NetworkControllerImpl.this.mWifiManager.getWifiApState();
                if (enabled && (wifiApState == 12 || wifiApState == 13)) {
                    NetworkControllerImpl.this.mWifiManager.setWifiApEnabled(null, false);
                }
                NetworkControllerImpl.this.mWifiManager.setWifiEnabled(enabled);
                return null;
            }
        }.execute(new Void[0]);
    }

    @Override
    public void onUserSwitched(int newUserId) {
        this.mCurrentUserId = newUserId;
        this.mAccessPoints.onUserSwitched(newUserId);
        updateConnectivity();
        refreshCarrierLabel();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (CHATTY) {
            Log.d("NetworkController", "onReceive: intent=" + intent);
        }
        String action = intent.getAction();
        if (action.equals("android.net.conn.CONNECTIVITY_CHANGE_IMMEDIATE") || action.equals("android.net.conn.INET_CONDITION_ACTION")) {
            updateConnectivity();
            refreshCarrierLabel();
            return;
        }
        if (action.equals("android.intent.action.CONFIGURATION_CHANGED")) {
            this.mConfig = Config.readConfig(this.mContext);
            handleConfigurationChanged();
            return;
        }
        if (action.equals("android.intent.action.AIRPLANE_MODE")) {
            refreshLocale();
            updateAirplaneMode(false);
            refreshCarrierLabel();
            return;
        }
        if (action.equals("android.intent.action.ACTION_DEFAULT_VOICE_SUBSCRIPTION_CHANGED")) {
            recalculateEmergency();
            return;
        }
        if (action.equals("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED")) {
            for (MobileSignalController controller : this.mMobileSignalControllers.values()) {
                controller.handleBroadcast(intent);
            }
            return;
        }
        if (action.equals("android.intent.action.SIM_STATE_CHANGED")) {
            updateMobileControllers();
            return;
        }
        int subId = intent.getIntExtra("subscription", -1);
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            if (this.mMobileSignalControllers.containsKey(Integer.valueOf(subId))) {
                this.mMobileSignalControllers.get(Integer.valueOf(subId)).handleBroadcast(intent);
                return;
            } else {
                updateMobileControllers();
                return;
            }
        }
        this.mWifiSignalController.handleBroadcast(intent);
    }

    void handleConfigurationChanged() {
        for (MobileSignalController mobileSignalController : this.mMobileSignalControllers.values()) {
            mobileSignalController.setConfiguration(this.mConfig);
        }
        refreshLocale();
        refreshCarrierLabel();
    }

    private void updateMobileControllers() {
        if (this.mListening) {
            List<SubscriptionInfo> subscriptions = this.mSubscriptionManager.getActiveSubscriptionInfoList();
            if (subscriptions == null) {
                subscriptions = Collections.emptyList();
            }
            if (hasCorrectMobileControllers(subscriptions)) {
                updateNoSims();
            } else {
                setCurrentSubscriptions(subscriptions);
                updateNoSims();
            }
        }
    }

    protected void updateNoSims() {
        boolean hasNoSims = this.mHasMobileDataFeature && this.mMobileSignalControllers.size() == 0;
        if (hasNoSims != this.mHasNoSims) {
            this.mHasNoSims = hasNoSims;
            notifyListeners();
        }
    }

    void setCurrentSubscriptions(List<SubscriptionInfo> subscriptions) {
        Collections.sort(subscriptions, new Comparator<SubscriptionInfo>() {
            @Override
            public int compare(SubscriptionInfo lhs, SubscriptionInfo rhs) {
                return lhs.getSimSlotIndex() == rhs.getSimSlotIndex() ? lhs.getSubscriptionId() - rhs.getSubscriptionId() : lhs.getSimSlotIndex() - rhs.getSimSlotIndex();
            }
        });
        int length = this.mSignalClusters.size();
        for (int i = 0; i < length; i++) {
            this.mSignalClusters.get(i).setSubs(subscriptions);
        }
        this.mCurrentSubscriptions = subscriptions;
        HashMap<Integer, MobileSignalController> cachedControllers = new HashMap<>(this.mMobileSignalControllers);
        this.mMobileSignalControllers.clear();
        int num = subscriptions.size();
        for (int i2 = 0; i2 < num; i2++) {
            int subId = subscriptions.get(i2).getSubscriptionId();
            if (cachedControllers.containsKey(Integer.valueOf(subId))) {
                this.mMobileSignalControllers.put(Integer.valueOf(subId), cachedControllers.remove(Integer.valueOf(subId)));
            } else {
                MobileSignalController controller = new MobileSignalController(this.mContext, this.mConfig, this.mHasMobileDataFeature, this.mPhone, this.mSignalsChangedCallbacks, this.mSignalClusters, this, subscriptions.get(i2));
                this.mMobileSignalControllers.put(Integer.valueOf(subId), controller);
                if (subscriptions.get(i2).getSimSlotIndex() == 0) {
                    this.mDefaultSignalController = controller;
                }
                if (this.mListening) {
                    controller.registerListener();
                }
            }
        }
        if (this.mListening) {
            for (Integer key : cachedControllers.keySet()) {
                if (cachedControllers.get(key) == this.mDefaultSignalController) {
                    this.mDefaultSignalController = null;
                }
                cachedControllers.get(key).unregisterListener();
            }
        }
        pushConnectivityToSignals();
        updateAirplaneMode(true);
    }

    boolean hasCorrectMobileControllers(List<SubscriptionInfo> allSubscriptions) {
        if (allSubscriptions.size() != this.mMobileSignalControllers.size()) {
            return false;
        }
        for (SubscriptionInfo info : allSubscriptions) {
            if (!this.mMobileSignalControllers.containsKey(Integer.valueOf(info.getSubscriptionId()))) {
                return false;
            }
        }
        return true;
    }

    private void updateAirplaneMode(boolean force) {
        boolean airplaneMode = Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) == 1;
        if (airplaneMode != this.mAirplaneMode || force) {
            this.mAirplaneMode = airplaneMode;
            for (MobileSignalController mobileSignalController : this.mMobileSignalControllers.values()) {
                mobileSignalController.setAirplaneMode(this.mAirplaneMode);
            }
            notifyListeners();
            refreshCarrierLabel();
        }
    }

    private void refreshLocale() {
        Locale current = this.mContext.getResources().getConfiguration().locale;
        if (!current.equals(this.mLocale)) {
            this.mLocale = current;
            notifyAllListeners();
        }
    }

    private void notifyAllListeners() {
        notifyListeners();
        for (MobileSignalController mobileSignalController : this.mMobileSignalControllers.values()) {
            mobileSignalController.notifyListeners();
        }
        this.mWifiSignalController.notifyListeners();
    }

    private void notifyListeners() {
        int length = this.mSignalClusters.size();
        for (int i = 0; i < length; i++) {
            this.mSignalClusters.get(i).setIsAirplaneMode(this.mAirplaneMode, R.drawable.stat_sys_airplane_mode, R.string.accessibility_airplane_mode);
            this.mSignalClusters.get(i).setNoSims(this.mHasNoSims);
        }
        int signalsChangedLength = this.mSignalsChangedCallbacks.size();
        for (int i2 = 0; i2 < signalsChangedLength; i2++) {
            this.mSignalsChangedCallbacks.get(i2).onAirplaneModeChanged(this.mAirplaneMode);
            this.mSignalsChangedCallbacks.get(i2).onNoSimVisibleChanged(this.mHasNoSims);
        }
    }

    private void updateConnectivity() {
        this.mConnectedTransports.clear();
        this.mValidatedTransports.clear();
        NetworkCapabilities[] arr$ = this.mConnectivityManager.getDefaultNetworkCapabilitiesForUser(this.mCurrentUserId);
        for (NetworkCapabilities nc : arr$) {
            int[] arr$2 = nc.getTransportTypes();
            for (int transportType : arr$2) {
                this.mConnectedTransports.set(transportType);
                if (nc.hasCapability(16)) {
                    this.mValidatedTransports.set(transportType);
                }
            }
        }
        if (CHATTY) {
            Log.d("NetworkController", "updateConnectivity: mConnectedTransports=" + this.mConnectedTransports);
            Log.d("NetworkController", "updateConnectivity: mValidatedTransports=" + this.mValidatedTransports);
        }
        this.mConnected = !this.mConnectedTransports.isEmpty();
        this.mInetCondition = this.mValidatedTransports.isEmpty() ? false : true;
        this.mBluetoothTethered = this.mConnectedTransports.get(2);
        this.mEthernetConnected = this.mConnectedTransports.get(3);
        pushConnectivityToSignals();
    }

    private void pushConnectivityToSignals() {
        for (MobileSignalController mobileSignalController : this.mMobileSignalControllers.values()) {
            mobileSignalController.setInetCondition(this.mInetCondition ? 1 : 0, this.mValidatedTransports.get(mobileSignalController.getTransportType()) ? 1 : 0);
        }
        this.mWifiSignalController.setInetCondition(this.mValidatedTransports.get(this.mWifiSignalController.getTransportType()) ? 1 : 0);
    }

    void refreshCarrierLabel() {
        Context context = this.mContext;
        WifiSignalController.WifiState wifiState = this.mWifiSignalController.getState();
        String label = "";
        for (MobileSignalController controller : this.mMobileSignalControllers.values()) {
            label = controller.getLabel(label, this.mConnected, this.mHasMobileDataFeature);
        }
        if (this.mBluetoothTethered && !this.mHasMobileDataFeature) {
            label = this.mContext.getString(R.string.bluetooth_tethered);
        }
        if (this.mEthernetConnected && !this.mHasMobileDataFeature) {
            label = context.getString(R.string.ethernet_label);
        }
        if (this.mAirplaneMode && !isEmergencyOnly()) {
            if (wifiState.connected && this.mHasMobileDataFeature) {
                label = "";
            } else if (!this.mHasMobileDataFeature) {
                label = context.getString(R.string.status_bar_settings_signal_meter_disconnected);
            }
        } else if (!isMobileDataConnected() && !wifiState.connected && !this.mBluetoothTethered && !this.mEthernetConnected && !this.mHasMobileDataFeature) {
            label = context.getString(R.string.status_bar_settings_signal_meter_disconnected);
        }
        int length = this.mCarrierListeners.size();
        for (int i = 0; i < length; i++) {
            this.mCarrierListeners.get(i).setCarrierLabel(label);
        }
    }

    private boolean isMobileDataConnected() {
        MobileSignalController controller = getDataController();
        if (controller != null) {
            return controller.getState().dataConnected;
        }
        return false;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("NetworkController state:");
        pw.println("  - telephony ------");
        pw.print("  hasVoiceCallingFeature()=");
        pw.println(hasVoiceCallingFeature());
        pw.println("  - Bluetooth ----");
        pw.print("  mBtReverseTethered=");
        pw.println(this.mBluetoothTethered);
        pw.println("  - connectivity ------");
        pw.print("  mConnectedTransports=");
        pw.println(this.mConnectedTransports);
        pw.print("  mValidatedTransports=");
        pw.println(this.mValidatedTransports);
        pw.print("  mInetCondition=");
        pw.println(this.mInetCondition);
        pw.print("  mAirplaneMode=");
        pw.println(this.mAirplaneMode);
        pw.print("  mLocale=");
        pw.println(this.mLocale);
        for (MobileSignalController mobileSignalController : this.mMobileSignalControllers.values()) {
            mobileSignalController.dump(pw);
        }
        this.mWifiSignalController.dump(pw);
    }

    @Override
    public void dispatchDemoCommand(String command, Bundle args) {
        MobileSignalController.MobileIconGroup mobileIconGroup;
        if (!this.mDemoMode && command.equals("enter")) {
            if (DEBUG) {
                Log.d("NetworkController", "Entering demo mode");
            }
            unregisterListeners();
            this.mDemoMode = true;
            this.mDemoInetCondition = this.mInetCondition ? 1 : 0;
            this.mDemoWifiState = this.mWifiSignalController.getState();
            return;
        }
        if (this.mDemoMode && command.equals("exit")) {
            if (DEBUG) {
                Log.d("NetworkController", "Exiting demo mode");
            }
            this.mDemoMode = false;
            updateMobileControllers();
            Iterator<MobileSignalController> it = this.mMobileSignalControllers.values().iterator();
            while (it.hasNext()) {
                it.next().resetLastState();
            }
            this.mWifiSignalController.resetLastState();
            registerListeners();
            notifyAllListeners();
            refreshCarrierLabel();
            return;
        }
        if (this.mDemoMode && command.equals("network")) {
            String airplane = args.getString("airplane");
            if (airplane != null) {
                boolean show = airplane.equals("show");
                int length = this.mSignalClusters.size();
                for (int i = 0; i < length; i++) {
                    this.mSignalClusters.get(i).setIsAirplaneMode(show, R.drawable.stat_sys_airplane_mode, R.string.accessibility_airplane_mode);
                }
            }
            String fully = args.getString("fully");
            if (fully != null) {
                this.mDemoInetCondition = Boolean.parseBoolean(fully) ? 1 : 0;
                this.mWifiSignalController.setInetCondition(this.mDemoInetCondition);
                Iterator<MobileSignalController> it2 = this.mMobileSignalControllers.values().iterator();
                while (it2.hasNext()) {
                    it2.next().setInetCondition(this.mDemoInetCondition, this.mDemoInetCondition);
                }
            }
            String wifi = args.getString("wifi");
            if (wifi != null) {
                boolean show2 = wifi.equals("show");
                String level = args.getString("level");
                if (level != null) {
                    this.mDemoWifiState.level = level.equals("null") ? -1 : Math.min(Integer.parseInt(level), WifiIcons.WIFI_LEVEL_COUNT - 1);
                    this.mDemoWifiState.connected = this.mDemoWifiState.level >= 0;
                }
                this.mDemoWifiState.enabled = show2;
                this.mWifiSignalController.notifyListeners();
            }
            String sims = args.getString("sims");
            if (sims != null) {
                int num = Integer.parseInt(sims);
                List<SubscriptionInfo> subs = new ArrayList<>();
                if (num != this.mMobileSignalControllers.size()) {
                    this.mMobileSignalControllers.clear();
                    int start = this.mSubscriptionManager.getActiveSubscriptionInfoCountMax();
                    for (int i2 = start; i2 < start + num; i2++) {
                        SubscriptionInfo info = new SubscriptionInfo(i2, "", i2, "", "", 0, 0, "", 0, null, 0, 0, "");
                        subs.add(info);
                        this.mMobileSignalControllers.put(Integer.valueOf(i2), new MobileSignalController(this.mContext, this.mConfig, this.mHasMobileDataFeature, this.mPhone, this.mSignalsChangedCallbacks, this.mSignalClusters, this, info));
                    }
                }
                int n = this.mSignalClusters.size();
                for (int i3 = 0; i3 < n; i3++) {
                    this.mSignalClusters.get(i3).setSubs(subs);
                }
            }
            String nosim = args.getString("nosim");
            if (nosim != null) {
                boolean show3 = nosim.equals("show");
                int n2 = this.mSignalClusters.size();
                for (int i4 = 0; i4 < n2; i4++) {
                    this.mSignalClusters.get(i4).setNoSims(show3);
                }
            }
            String mobile = args.getString("mobile");
            if (mobile != null) {
                boolean show4 = mobile.equals("show");
                String datatype = args.getString("datatype");
                String slotString = args.getString("slot");
                int slot = TextUtils.isEmpty(slotString) ? 0 : Integer.parseInt(slotString);
                MobileSignalController controller = ((MobileSignalController[]) this.mMobileSignalControllers.values().toArray(new MobileSignalController[0]))[slot];
                controller.getState().dataSim = datatype != null;
                if (datatype != null) {
                    MobileSignalController.MobileState state = controller.getState();
                    if (datatype.equals("1x")) {
                        mobileIconGroup = TelephonyIcons.ONE_X;
                    } else {
                        mobileIconGroup = datatype.equals("3g") ? TelephonyIcons.THREE_G : datatype.equals("4g") ? TelephonyIcons.FOUR_G : datatype.equals("e") ? TelephonyIcons.E : datatype.equals("g") ? TelephonyIcons.G : datatype.equals("h") ? TelephonyIcons.H : datatype.equals("lte") ? TelephonyIcons.LTE : datatype.equals("roam") ? TelephonyIcons.ROAMING : TelephonyIcons.UNKNOWN;
                    }
                    state.iconGroup = mobileIconGroup;
                }
                int[][] icons = TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH;
                String level2 = args.getString("level");
                if (level2 != null) {
                    controller.getState().level = level2.equals("null") ? -1 : Math.min(Integer.parseInt(level2), icons[0].length - 1);
                    controller.getState().connected = controller.getState().level >= 0;
                }
                controller.getState().enabled = show4;
                controller.notifyListeners();
            }
            refreshCarrierLabel();
        }
    }

    static class WifiSignalController extends SignalController<WifiState, SignalController.IconGroup> {
        private final boolean mHasMobileData;
        private final AsyncChannel mWifiChannel;
        private final WifiManager mWifiManager;

        public WifiSignalController(Context context, boolean hasMobileData, List<NetworkController.NetworkSignalChangedCallback> signalCallbacks, List<SignalCluster> signalClusters, NetworkControllerImpl networkController) {
            super("WifiSignalController", context, 1, signalCallbacks, signalClusters, networkController);
            this.mWifiManager = (WifiManager) context.getSystemService("wifi");
            this.mHasMobileData = hasMobileData;
            Handler handler = new WifiHandler();
            this.mWifiChannel = new AsyncChannel();
            Messenger wifiMessenger = this.mWifiManager.getWifiServiceMessenger();
            if (wifiMessenger != null) {
                this.mWifiChannel.connect(context, handler, wifiMessenger);
            }
            WifiState wifiState = (WifiState) this.mCurrentState;
            WifiState wifiState2 = (WifiState) this.mLastState;
            SignalController.IconGroup iconGroup = new SignalController.IconGroup("Wi-Fi Icons", WifiIcons.WIFI_SIGNAL_STRENGTH, WifiIcons.QS_WIFI_SIGNAL_STRENGTH, AccessibilityContentDescriptions.WIFI_CONNECTION_STRENGTH, R.drawable.stat_sys_wifi_signal_null, R.drawable.ic_qs_wifi_no_network, R.drawable.stat_sys_wifi_signal_null, R.drawable.ic_qs_wifi_no_network, R.string.accessibility_no_wifi);
            wifiState2.iconGroup = iconGroup;
            wifiState.iconGroup = iconGroup;
        }

        @Override
        protected WifiState cleanState() {
            return new WifiState();
        }

        @Override
        public void notifyListeners() {
            boolean wifiVisible = ((WifiState) this.mCurrentState).enabled && (((WifiState) this.mCurrentState).connected || !this.mHasMobileData);
            String wifiDesc = wifiVisible ? ((WifiState) this.mCurrentState).ssid : null;
            boolean ssidPresent = wifiVisible && ((WifiState) this.mCurrentState).ssid != null;
            String contentDescription = getStringIfExists(getContentDescription());
            int length = this.mSignalsChangedCallbacks.size();
            for (int i = 0; i < length; i++) {
                this.mSignalsChangedCallbacks.get(i).onWifiSignalChanged(((WifiState) this.mCurrentState).enabled, ((WifiState) this.mCurrentState).connected, getQsCurrentIconId(), ssidPresent && ((WifiState) this.mCurrentState).activityIn, ssidPresent && ((WifiState) this.mCurrentState).activityOut, contentDescription, wifiDesc);
            }
            int signalClustersLength = this.mSignalClusters.size();
            for (int i2 = 0; i2 < signalClustersLength; i2++) {
                this.mSignalClusters.get(i2).setWifiIndicators(wifiVisible, getCurrentIconId(), contentDescription);
            }
        }

        public void handleBroadcast(Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.net.wifi.WIFI_STATE_CHANGED")) {
                ((WifiState) this.mCurrentState).enabled = intent.getIntExtra("wifi_state", 4) == 3;
            } else if (action.equals("android.net.wifi.STATE_CHANGE")) {
                NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                ((WifiState) this.mCurrentState).connected = networkInfo != null && networkInfo.isConnected();
                if (((WifiState) this.mCurrentState).connected) {
                    WifiInfo info = intent.getParcelableExtra("wifiInfo") != null ? (WifiInfo) intent.getParcelableExtra("wifiInfo") : this.mWifiManager.getConnectionInfo();
                    if (info != null) {
                        ((WifiState) this.mCurrentState).ssid = getSsid(info);
                    } else {
                        ((WifiState) this.mCurrentState).ssid = null;
                    }
                } else if (!((WifiState) this.mCurrentState).connected) {
                    ((WifiState) this.mCurrentState).ssid = null;
                }
            } else if (action.equals("android.net.wifi.RSSI_CHANGED")) {
                ((WifiState) this.mCurrentState).rssi = intent.getIntExtra("newRssi", -200);
                ((WifiState) this.mCurrentState).level = WifiManager.calculateSignalLevel(((WifiState) this.mCurrentState).rssi, WifiIcons.WIFI_LEVEL_COUNT);
            }
            notifyListenersIfNecessary();
        }

        private String getSsid(WifiInfo info) {
            String ssid = info.getSSID();
            if (ssid == null) {
                List<WifiConfiguration> networks = this.mWifiManager.getConfiguredNetworks();
                int length = networks.size();
                for (int i = 0; i < length; i++) {
                    if (networks.get(i).networkId == info.getNetworkId()) {
                        return networks.get(i).SSID;
                    }
                }
                return null;
            }
            return ssid;
        }

        void setActivity(int wifiActivity) {
            ((WifiState) this.mCurrentState).activityIn = wifiActivity == 3 || wifiActivity == 1;
            ((WifiState) this.mCurrentState).activityOut = wifiActivity == 3 || wifiActivity == 2;
            notifyListenersIfNecessary();
        }

        class WifiHandler extends Handler {
            WifiHandler() {
            }

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        WifiSignalController.this.setActivity(msg.arg1);
                        break;
                    case 69632:
                        if (msg.arg1 == 0) {
                            WifiSignalController.this.mWifiChannel.sendMessage(Message.obtain(this, 69633));
                        } else {
                            Log.e(WifiSignalController.this.mTag, "Failed to connect to wifi");
                        }
                        break;
                }
            }
        }

        static class WifiState extends SignalController.State {
            String ssid;

            WifiState() {
            }

            @Override
            public void copyFrom(SignalController.State s) {
                super.copyFrom(s);
                WifiState state = (WifiState) s;
                this.ssid = state.ssid;
            }

            @Override
            protected void toString(StringBuilder builder) {
                super.toString(builder);
                builder.append(',').append("ssid=").append(this.ssid);
            }

            @Override
            public boolean equals(Object o) {
                return super.equals(o) && Objects.equals(((WifiState) o).ssid, this.ssid);
            }
        }
    }

    public static class MobileSignalController extends SignalController<MobileState, MobileIconGroup> {
        private Config mConfig;
        private int mDataNetType;
        private int mDataState;
        private MobileIconGroup mDefaultIcons;
        private final String mNetworkNameDefault;
        private final String mNetworkNameSeparator;
        final SparseArray<MobileIconGroup> mNetworkToIconLookup;
        private final TelephonyManager mPhone;
        final PhoneStateListener mPhoneStateListener;
        private ServiceState mServiceState;
        private SignalStrength mSignalStrength;
        private IccCardConstants.State mSimState;
        private final SubscriptionInfo mSubscriptionInfo;
        private final SubscriptionManager mSubscriptionManager;

        @Override
        public int getContentDescription() {
            return super.getContentDescription();
        }

        @Override
        public int getCurrentIconId() {
            return super.getCurrentIconId();
        }

        @Override
        public int getQsCurrentIconId() {
            return super.getQsCurrentIconId();
        }

        @Override
        public int getTransportType() {
            return super.getTransportType();
        }

        @Override
        public boolean isDirty() {
            return super.isDirty();
        }

        @Override
        public void notifyListenersIfNecessary() {
            super.notifyListenersIfNecessary();
        }

        @Override
        public void saveLastState() {
            super.saveLastState();
        }

        @Override
        public void setInetCondition(int i) {
            super.setInetCondition(i);
        }

        public MobileSignalController(Context context, Config config, boolean hasMobileData, TelephonyManager phone, List<NetworkController.NetworkSignalChangedCallback> signalCallbacks, List<SignalCluster> signalClusters, NetworkControllerImpl networkController, SubscriptionInfo info) {
            super("MobileSignalController(" + info.getSubscriptionId() + ")", context, 0, signalCallbacks, signalClusters, networkController);
            this.mSimState = IccCardConstants.State.READY;
            this.mDataNetType = 0;
            this.mDataState = 0;
            this.mSubscriptionManager = SubscriptionManager.from(context);
            this.mNetworkToIconLookup = new SparseArray<>();
            this.mConfig = config;
            this.mPhone = phone;
            this.mSubscriptionInfo = info;
            this.mPhoneStateListener = new MobilePhoneStateListener(info.getSubscriptionId());
            this.mNetworkNameSeparator = getStringIfExists(R.string.status_bar_network_name_separator);
            this.mNetworkNameDefault = getStringIfExists(android.R.string.duration_minutes_shortest);
            mapIconSets();
            MobileState mobileState = (MobileState) this.mLastState;
            MobileState mobileState2 = (MobileState) this.mCurrentState;
            String str = this.mNetworkNameDefault;
            mobileState2.networkName = str;
            mobileState.networkName = str;
            MobileState mobileState3 = (MobileState) this.mLastState;
            ((MobileState) this.mCurrentState).enabled = hasMobileData;
            mobileState3.enabled = hasMobileData;
            MobileState mobileState4 = (MobileState) this.mLastState;
            MobileState mobileState5 = (MobileState) this.mCurrentState;
            MobileIconGroup mobileIconGroup = this.mDefaultIcons;
            mobileState5.iconGroup = mobileIconGroup;
            mobileState4.iconGroup = mobileIconGroup;
            updateDataSim();
        }

        public void setConfiguration(Config config) {
            this.mConfig = config;
            mapIconSets();
            updateTelephony();
        }

        public String getLabel(String currentLabel, boolean connected, boolean isMobileLabel) {
            if (!((MobileState) this.mCurrentState).enabled) {
                return "";
            }
            String mobileLabel = "";
            if (((MobileState) this.mCurrentState).dataConnected) {
                mobileLabel = ((MobileState) this.mCurrentState).networkName;
            } else if (connected || ((MobileState) this.mCurrentState).isEmergency) {
                if (((MobileState) this.mCurrentState).connected || ((MobileState) this.mCurrentState).isEmergency) {
                    mobileLabel = ((MobileState) this.mCurrentState).networkName;
                }
            } else {
                mobileLabel = this.mContext.getString(R.string.status_bar_settings_signal_meter_disconnected);
            }
            if (currentLabel.length() != 0) {
                currentLabel = currentLabel + this.mNetworkNameSeparator;
            }
            if (isMobileLabel) {
                return currentLabel + mobileLabel;
            }
            StringBuilder sbAppend = new StringBuilder().append(currentLabel);
            if (!((MobileState) this.mCurrentState).dataConnected) {
                mobileLabel = currentLabel;
            }
            return sbAppend.append(mobileLabel).toString();
        }

        public void setAirplaneMode(boolean airplaneMode) {
            ((MobileState) this.mCurrentState).airplaneMode = airplaneMode;
            notifyListenersIfNecessary();
        }

        public void setInetCondition(int inetCondition, int inetConditionForNetwork) {
            ((MobileState) this.mCurrentState).inetForNetwork = inetConditionForNetwork;
            setInetCondition(inetCondition);
        }

        public void registerListener() {
            this.mPhone.listen(this.mPhoneStateListener, 481);
        }

        public void unregisterListener() {
            this.mPhone.listen(this.mPhoneStateListener, 0);
        }

        private boolean isCMCC() {
            char code;
            String operator = TelephonyManager.from(this.mContext).getSimOperator();
            return operator != null && operator.startsWith("4600") && operator.length() >= 5 && ((code = operator.charAt(4)) == '0' || code == '2' || code == '7' || code == '8');
        }

        private void mapIconSets() {
            this.mNetworkToIconLookup.clear();
            this.mNetworkToIconLookup.put(5, TelephonyIcons.THREE_G);
            this.mNetworkToIconLookup.put(6, TelephonyIcons.THREE_G);
            this.mNetworkToIconLookup.put(12, TelephonyIcons.THREE_G);
            this.mNetworkToIconLookup.put(14, TelephonyIcons.THREE_G);
            this.mNetworkToIconLookup.put(3, TelephonyIcons.THREE_G);
            if (!this.mConfig.showAtLeast3G) {
                this.mNetworkToIconLookup.put(0, TelephonyIcons.UNKNOWN);
                this.mNetworkToIconLookup.put(2, TelephonyIcons.E);
                this.mNetworkToIconLookup.put(4, TelephonyIcons.ONE_X);
                this.mNetworkToIconLookup.put(7, TelephonyIcons.ONE_X);
                this.mDefaultIcons = TelephonyIcons.G;
            } else {
                this.mNetworkToIconLookup.put(0, TelephonyIcons.THREE_G);
                this.mNetworkToIconLookup.put(2, TelephonyIcons.THREE_G);
                this.mNetworkToIconLookup.put(4, TelephonyIcons.THREE_G);
                this.mNetworkToIconLookup.put(7, TelephonyIcons.THREE_G);
                this.mDefaultIcons = TelephonyIcons.THREE_G;
            }
            MobileIconGroup hGroup = TelephonyIcons.THREE_G;
            MobileIconGroup hpGroup = TelephonyIcons.THREE_G;
            if (this.mConfig.hspaDataDistinguishable && !isCMCC()) {
                hGroup = TelephonyIcons.H;
                hpGroup = TelephonyIcons.HP;
            }
            this.mNetworkToIconLookup.put(8, hGroup);
            this.mNetworkToIconLookup.put(9, hGroup);
            this.mNetworkToIconLookup.put(10, hGroup);
            this.mNetworkToIconLookup.put(15, hpGroup);
            if (this.mConfig.show4gForLte) {
                this.mNetworkToIconLookup.put(13, TelephonyIcons.FOUR_G);
            } else {
                this.mNetworkToIconLookup.put(13, TelephonyIcons.LTE);
            }
        }

        @Override
        public void notifyListeners() {
            MobileIconGroup icons = getIcons();
            String contentDescription = getStringIfExists(getContentDescription());
            String dataContentDescription = getStringIfExists(icons.mDataContentDescription);
            boolean showDataIcon = (((MobileState) this.mCurrentState).dataConnected && ((MobileState) this.mCurrentState).inetForNetwork != 0) || ((MobileState) this.mCurrentState).iconGroup == TelephonyIcons.ROAMING;
            if (((MobileState) this.mCurrentState).dataSim) {
                int qsTypeIcon = showDataIcon ? icons.mQsDataType[((MobileState) this.mCurrentState).inetForNetwork] : 0;
                int length = this.mSignalsChangedCallbacks.size();
                for (int i = 0; i < length; i++) {
                    this.mSignalsChangedCallbacks.get(i).onMobileDataSignalChanged(((MobileState) this.mCurrentState).enabled && !((MobileState) this.mCurrentState).isEmergency, getQsCurrentIconId(), contentDescription, qsTypeIcon, ((MobileState) this.mCurrentState).dataConnected && ((MobileState) this.mCurrentState).activityIn, ((MobileState) this.mCurrentState).dataConnected && ((MobileState) this.mCurrentState).activityOut, dataContentDescription, ((MobileState) this.mCurrentState).isEmergency ? null : ((MobileState) this.mCurrentState).networkName, icons.mIsWide && qsTypeIcon != 0);
                }
            }
            int typeIcon = showDataIcon ? icons.mDataType : 0;
            int signalClustersLength = this.mSignalClusters.size();
            for (int i2 = 0; i2 < signalClustersLength; i2++) {
                this.mSignalClusters.get(i2).setMobileDataIndicators(((MobileState) this.mCurrentState).enabled && !((MobileState) this.mCurrentState).airplaneMode, getCurrentIconId(), typeIcon, contentDescription, dataContentDescription, icons.mIsWide && typeIcon != 0, this.mSubscriptionInfo.getSubscriptionId());
            }
        }

        @Override
        protected MobileState cleanState() {
            return new MobileState();
        }

        private boolean hasService() {
            if (this.mServiceState == null) {
                return false;
            }
            switch (this.mServiceState.getVoiceRegState()) {
                case 1:
                case 2:
                    return this.mServiceState.getDataRegState() == 0;
                case 3:
                    return false;
                default:
                    return true;
            }
        }

        private boolean isCdma() {
            return (this.mSignalStrength == null || this.mSignalStrength.isGsm()) ? false : true;
        }

        public boolean isEmergencyOnly() {
            return this.mServiceState != null && this.mServiceState.isEmergencyOnly();
        }

        private boolean isRoaming() {
            if (!isCdma()) {
                return this.mServiceState != null && this.mServiceState.getRoaming();
            }
            int iconMode = this.mServiceState.getCdmaEriIconMode();
            return this.mServiceState.getCdmaEriIconIndex() != 1 && (iconMode == 0 || iconMode == 1);
        }

        public void handleBroadcast(Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.provider.Telephony.SPN_STRINGS_UPDATED")) {
                updateNetworkName(intent.getBooleanExtra("showSpn", false), intent.getStringExtra("spn"), intent.getBooleanExtra("showPlmn", false), intent.getStringExtra("plmn"));
                notifyListenersIfNecessary();
            } else if (action.equals("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED")) {
                updateDataSim();
            }
        }

        private void updateDataSim() {
            int defaultDataSub = SubscriptionManager.getDefaultDataSubId();
            if (SubscriptionManager.isValidSubscriptionId(defaultDataSub)) {
                ((MobileState) this.mCurrentState).dataSim = defaultDataSub == this.mSubscriptionInfo.getSubscriptionId();
            } else {
                ((MobileState) this.mCurrentState).dataSim = true;
            }
            notifyListenersIfNecessary();
        }

        void updateNetworkName(boolean showSpn, String spn, boolean showPlmn, String plmn) {
            if (NetworkControllerImpl.CHATTY) {
                Log.d("CarrierLabel", "updateNetworkName showSpn=" + showSpn + " spn=" + spn + " showPlmn=" + showPlmn + " plmn=" + plmn);
            }
            StringBuilder str = new StringBuilder();
            if (showPlmn && plmn != null) {
                str.append(plmn);
            }
            if (showSpn && spn != null) {
                if (str.length() != 0) {
                    str.append(this.mNetworkNameSeparator);
                }
                str.append(spn);
            }
            if (str.length() != 0) {
                ((MobileState) this.mCurrentState).networkName = str.toString();
            } else {
                ((MobileState) this.mCurrentState).networkName = this.mNetworkNameDefault;
            }
        }

        private final void updateTelephony() {
            if (NetworkControllerImpl.DEBUG) {
                Log.d("NetworkController", "updateTelephonySignalStrength: hasService=" + hasService() + " ss=" + this.mSignalStrength);
            }
            int subId = this.mSubscriptionInfo.getSubscriptionId();
            if (this.mSubscriptionManager.getActiveSubscriptionInfo(subId) == null) {
                if (NetworkControllerImpl.DEBUG) {
                    Log.d("NetworkController", "updateTelephony subId not active now" + subId);
                    return;
                }
                return;
            }
            ((MobileState) this.mCurrentState).connected = hasService() && this.mSignalStrength != null;
            if (((MobileState) this.mCurrentState).connected) {
                if (!this.mSignalStrength.isGsm() && this.mConfig.alwaysShowCdmaRssi) {
                    ((MobileState) this.mCurrentState).level = this.mSignalStrength.getCdmaLevel();
                } else {
                    ((MobileState) this.mCurrentState).level = this.mSignalStrength.getLevel();
                }
            }
            if (this.mNetworkToIconLookup.indexOfKey(this.mDataNetType) >= 0) {
                ((MobileState) this.mCurrentState).iconGroup = this.mNetworkToIconLookup.get(this.mDataNetType);
            } else {
                ((MobileState) this.mCurrentState).iconGroup = this.mDefaultIcons;
            }
            ((MobileState) this.mCurrentState).dataConnected = ((MobileState) this.mCurrentState).connected && this.mDataState == 2;
            if (isRoaming()) {
                ((MobileState) this.mCurrentState).iconGroup = TelephonyIcons.ROAMING;
            }
            if (isEmergencyOnly() != ((MobileState) this.mCurrentState).isEmergency) {
                ((MobileState) this.mCurrentState).isEmergency = isEmergencyOnly();
                this.mNetworkController.recalculateEmergency();
            }
            if (((MobileState) this.mCurrentState).networkName == this.mNetworkNameDefault && this.mServiceState != null && this.mServiceState.getOperatorAlphaShort() != null) {
                ((MobileState) this.mCurrentState).networkName = this.mServiceState.getOperatorAlphaShort();
            }
            notifyListenersIfNecessary();
        }

        void setActivity(int activity) {
            ((MobileState) this.mCurrentState).activityIn = activity == 3 || activity == 1;
            ((MobileState) this.mCurrentState).activityOut = activity == 3 || activity == 2;
            notifyListenersIfNecessary();
        }

        @Override
        public void dump(PrintWriter pw) {
            super.dump(pw);
            pw.println("  mSubscription=" + this.mSubscriptionInfo + ",");
            pw.println("  mServiceState=" + this.mServiceState + ",");
            pw.println("  mSignalStrength=" + this.mSignalStrength + ",");
            pw.println("  mDataState=" + this.mDataState + ",");
            pw.println("  mDataNetType=" + this.mDataNetType + ",");
        }

        class MobilePhoneStateListener extends PhoneStateListener {
            public MobilePhoneStateListener(int subId) {
                super(subId);
            }

            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                if (NetworkControllerImpl.DEBUG) {
                    Log.d(MobileSignalController.this.mTag, "onSignalStrengthsChanged signalStrength=" + signalStrength + (signalStrength == null ? "" : " level=" + signalStrength.getLevel()));
                }
                MobileSignalController.this.mSignalStrength = signalStrength;
                MobileSignalController.this.updateTelephony();
            }

            @Override
            public void onServiceStateChanged(ServiceState state) {
                if (NetworkControllerImpl.DEBUG) {
                    Log.d(MobileSignalController.this.mTag, "onServiceStateChanged voiceState=" + state.getVoiceRegState() + " dataState=" + state.getDataRegState());
                }
                MobileSignalController.this.mServiceState = state;
                MobileSignalController.this.updateTelephony();
            }

            @Override
            public void onDataConnectionStateChanged(int state, int networkType) {
                if (NetworkControllerImpl.DEBUG) {
                    Log.d(MobileSignalController.this.mTag, "onDataConnectionStateChanged: state=" + state + " type=" + networkType);
                }
                MobileSignalController.this.mDataState = state;
                MobileSignalController.this.mDataNetType = networkType;
                MobileSignalController.this.updateTelephony();
            }

            @Override
            public void onDataActivity(int direction) {
                if (NetworkControllerImpl.DEBUG) {
                    Log.d(MobileSignalController.this.mTag, "onDataActivity: direction=" + direction);
                }
                MobileSignalController.this.setActivity(direction);
            }
        }

        static class MobileIconGroup extends SignalController.IconGroup {
            final int mDataContentDescription;
            final int mDataType;
            final boolean mIsWide;
            final int[] mQsDataType;

            public MobileIconGroup(String name, int[][] sbIcons, int[][] qsIcons, int[] contentDesc, int sbNullState, int qsNullState, int sbDiscState, int qsDiscState, int discContentDesc, int dataContentDesc, int dataType, boolean isWide, int[] qsDataType) {
                super(name, sbIcons, qsIcons, contentDesc, sbNullState, qsNullState, sbDiscState, qsDiscState, discContentDesc);
                this.mDataContentDescription = dataContentDesc;
                this.mDataType = dataType;
                this.mIsWide = isWide;
                this.mQsDataType = qsDataType;
            }
        }

        static class MobileState extends SignalController.State {
            boolean airplaneMode;
            boolean dataConnected;
            boolean dataSim;
            int inetForNetwork;
            boolean isEmergency;
            String networkName;

            MobileState() {
            }

            @Override
            public void copyFrom(SignalController.State s) {
                super.copyFrom(s);
                MobileState state = (MobileState) s;
                this.dataSim = state.dataSim;
                this.networkName = state.networkName;
                this.dataConnected = state.dataConnected;
                this.inetForNetwork = state.inetForNetwork;
                this.isEmergency = state.isEmergency;
                this.airplaneMode = state.airplaneMode;
            }

            @Override
            protected void toString(StringBuilder builder) {
                super.toString(builder);
                builder.append(',');
                builder.append("dataSim=").append(this.dataSim).append(',');
                builder.append("networkName=").append(this.networkName).append(',');
                builder.append("dataConnected=").append(this.dataConnected).append(',');
                builder.append("inetForNetwork=").append(this.inetForNetwork).append(',');
                builder.append("isEmergency=").append(this.isEmergency).append(',');
                builder.append("airplaneMode=").append(this.airplaneMode);
            }

            @Override
            public boolean equals(Object o) {
                return super.equals(o) && Objects.equals(((MobileState) o).networkName, this.networkName) && ((MobileState) o).dataSim == this.dataSim && ((MobileState) o).dataConnected == this.dataConnected && ((MobileState) o).isEmergency == this.isEmergency && ((MobileState) o).airplaneMode == this.airplaneMode && ((MobileState) o).inetForNetwork == this.inetForNetwork;
            }
        }
    }

    static abstract class SignalController<T extends State, I extends IconGroup> {
        protected final Context mContext;
        private int mHistoryIndex;
        protected final NetworkControllerImpl mNetworkController;
        protected final List<SignalCluster> mSignalClusters;
        protected final List<NetworkController.NetworkSignalChangedCallback> mSignalsChangedCallbacks;
        protected final String mTag;
        protected final int mTransportType;
        protected final T mCurrentState = (T) cleanState();
        protected final T mLastState = (T) cleanState();
        private final State[] mHistory = new State[16];

        protected abstract T cleanState();

        public abstract void notifyListeners();

        public SignalController(String str, Context context, int i, List<NetworkController.NetworkSignalChangedCallback> list, List<SignalCluster> list2, NetworkControllerImpl networkControllerImpl) {
            this.mTag = "NetworkController." + str;
            this.mNetworkController = networkControllerImpl;
            this.mTransportType = i;
            this.mContext = context;
            this.mSignalsChangedCallbacks = list;
            this.mSignalClusters = list2;
            for (int i2 = 0; i2 < 16; i2++) {
                this.mHistory[i2] = cleanState();
            }
        }

        public T getState() {
            return this.mCurrentState;
        }

        public int getTransportType() {
            return this.mTransportType;
        }

        public void setInetCondition(int inetCondition) {
            this.mCurrentState.inetCondition = inetCondition;
            notifyListenersIfNecessary();
        }

        void resetLastState() {
            this.mCurrentState.copyFrom(this.mLastState);
        }

        public boolean isDirty() {
            if (this.mLastState.equals(this.mCurrentState)) {
                return false;
            }
            if (NetworkControllerImpl.DEBUG) {
                Log.d(this.mTag, "Change in state from: " + this.mLastState + "\n\tto: " + this.mCurrentState);
            }
            return true;
        }

        public void saveLastState() {
            recordLastState();
            this.mCurrentState.time = System.currentTimeMillis();
            this.mLastState.copyFrom(this.mCurrentState);
        }

        public int getQsCurrentIconId() {
            if (this.mCurrentState.connected) {
                return getIcons().mQsIcons[this.mCurrentState.inetCondition][this.mCurrentState.level];
            }
            if (this.mCurrentState.enabled) {
                return getIcons().mQsDiscState;
            }
            return getIcons().mQsNullState;
        }

        public int getCurrentIconId() {
            if (this.mCurrentState.connected) {
                return getIcons().mSbIcons[this.mCurrentState.inetCondition][this.mCurrentState.level];
            }
            if (this.mCurrentState.enabled) {
                return getIcons().mSbDiscState;
            }
            return getIcons().mSbNullState;
        }

        public int getContentDescription() {
            return this.mCurrentState.connected ? getIcons().mContentDesc[this.mCurrentState.level] : getIcons().mDiscContentDesc;
        }

        public void notifyListenersIfNecessary() {
            if (isDirty()) {
                saveLastState();
                notifyListeners();
                this.mNetworkController.refreshCarrierLabel();
            }
        }

        protected String getStringIfExists(int resId) {
            return resId != 0 ? this.mContext.getString(resId) : "";
        }

        protected I getIcons() {
            return (I) this.mCurrentState.iconGroup;
        }

        protected void recordLastState() {
            State[] stateArr = this.mHistory;
            int i = this.mHistoryIndex;
            this.mHistoryIndex = i + 1;
            stateArr[i & 15].copyFrom(this.mLastState);
        }

        public void dump(PrintWriter pw) {
            pw.println("  - " + this.mTag + " -----");
            pw.println("  Current State: " + this.mCurrentState);
            int size = 0;
            for (int i = 0; i < 16; i++) {
                if (this.mHistory[i].time != 0) {
                    size++;
                }
            }
            for (int i2 = (this.mHistoryIndex + 16) - 1; i2 >= (this.mHistoryIndex + 16) - size; i2--) {
                pw.println("  Previous State(" + ((this.mHistoryIndex + 16) - i2) + ": " + this.mHistory[i2 & 15]);
            }
        }

        static class IconGroup {
            final int[] mContentDesc;
            final int mDiscContentDesc;
            final String mName;
            final int mQsDiscState;
            final int[][] mQsIcons;
            final int mQsNullState;
            final int mSbDiscState;
            final int[][] mSbIcons;
            final int mSbNullState;

            public IconGroup(String name, int[][] sbIcons, int[][] qsIcons, int[] contentDesc, int sbNullState, int qsNullState, int sbDiscState, int qsDiscState, int discContentDesc) {
                this.mName = name;
                this.mSbIcons = sbIcons;
                this.mQsIcons = qsIcons;
                this.mContentDesc = contentDesc;
                this.mSbNullState = sbNullState;
                this.mQsNullState = qsNullState;
                this.mSbDiscState = sbDiscState;
                this.mQsDiscState = qsDiscState;
                this.mDiscContentDesc = discContentDesc;
            }

            public String toString() {
                return "IconGroup(" + this.mName + ")";
            }
        }

        static class State {
            boolean activityIn;
            boolean activityOut;
            boolean connected;
            boolean enabled;
            IconGroup iconGroup;
            int inetCondition;
            int level;
            int rssi;
            long time;

            State() {
            }

            public void copyFrom(State state) {
                this.connected = state.connected;
                this.enabled = state.enabled;
                this.level = state.level;
                this.iconGroup = state.iconGroup;
                this.inetCondition = state.inetCondition;
                this.activityIn = state.activityIn;
                this.activityOut = state.activityOut;
                this.rssi = state.rssi;
                this.time = state.time;
            }

            public String toString() {
                if (this.time == 0) {
                    return "Empty " + getClass().getSimpleName();
                }
                StringBuilder builder = new StringBuilder();
                toString(builder);
                return builder.toString();
            }

            protected void toString(StringBuilder builder) {
                builder.append("connected=").append(this.connected).append(',').append("enabled=").append(this.enabled).append(',').append("level=").append(this.level).append(',').append("inetCondition=").append(this.inetCondition).append(',').append("iconGroup=").append(this.iconGroup).append(',').append("activityIn=").append(this.activityIn).append(',').append("activityOut=").append(this.activityOut).append(',').append("rssi=").append(this.rssi).append(',').append("lastModified=").append(DateFormat.format("MM-dd hh:mm:ss", this.time));
            }

            public boolean equals(Object o) {
                if (!o.getClass().equals(getClass())) {
                    return false;
                }
                State other = (State) o;
                return other.connected == this.connected && other.enabled == this.enabled && other.level == this.level && other.inetCondition == this.inetCondition && other.iconGroup == this.iconGroup && other.activityIn == this.activityIn && other.activityOut == this.activityOut && other.rssi == this.rssi;
            }
        }
    }

    static class Config {
        boolean hspaDataDistinguishable;
        boolean showAtLeast3G = false;
        boolean alwaysShowCdmaRssi = false;
        boolean show4gForLte = false;

        Config() {
        }

        static Config readConfig(Context context) {
            Config config = new Config();
            Resources res = context.getResources();
            config.showAtLeast3G = res.getBoolean(R.bool.config_showMin3G);
            config.alwaysShowCdmaRssi = res.getBoolean(android.R.^attr-private.frameDuration);
            config.show4gForLte = res.getBoolean(R.bool.config_show4GForLTE);
            config.hspaDataDistinguishable = res.getBoolean(R.bool.config_hspa_data_distinguishable);
            return config;
        }
    }
}
