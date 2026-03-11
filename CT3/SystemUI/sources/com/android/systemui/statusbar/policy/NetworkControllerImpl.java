package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.MathUtils;
import com.android.settingslib.net.DataUsageController;
import com.android.systemui.DemoMode;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.MobileSignalController;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.WifiSignalController;
import com.mediatek.systemui.PluginManager;
import com.mediatek.systemui.ext.ISystemUIStatusBarExt;
import com.mediatek.systemui.statusbar.util.SIMHelper;
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

public class NetworkControllerImpl extends BroadcastReceiver implements NetworkController, DemoMode, DataUsageController.NetworkNameProvider {
    private final AccessPointControllerImpl mAccessPoints;
    private boolean mAirplaneMode;
    private final CallbackHandler mCallbackHandler;
    private Config mConfig;
    private final BitSet mConnectedTransports;
    private final ConnectivityManager mConnectivityManager;
    private final Context mContext;
    private List<SubscriptionInfo> mCurrentSubscriptions;
    private int mCurrentUserId;
    private final DataSaverController mDataSaverController;
    private final DataUsageController mDataUsageController;
    private MobileSignalController mDefaultSignalController;
    private boolean mDemoInetCondition;
    private boolean mDemoMode;
    private WifiSignalController.WifiState mDemoWifiState;
    private boolean[] mEmergencyPhone;
    private int mEmergencySource;
    final EthernetSignalController mEthernetSignalController;
    private final boolean mHasMobileDataFeature;
    private boolean mHasNoSims;
    private boolean mInetCondition;
    private boolean mIsEmergency;
    ServiceState mLastServiceState;
    boolean mListening;
    private Locale mLocale;
    final Map<Integer, MobileSignalController> mMobileSignalControllers;
    String[] mNetworkName;
    private final TelephonyManager mPhone;
    private final Handler mReceiverHandler;
    private final Runnable mRegisterListeners;
    int mSlotCount;
    private ISystemUIStatusBarExt mStatusBarSystemUIExt;
    private final SubscriptionDefaults mSubDefaults;
    private SubscriptionManager.OnSubscriptionsChangedListener mSubscriptionListener;
    private final SubscriptionManager mSubscriptionManager;
    private boolean mUserSetup;
    private final BitSet mValidatedTransports;
    private final WifiManager mWifiManager;
    final WifiSignalController mWifiSignalController;
    static final boolean DEBUG = Log.isLoggable("NetworkController", 3);
    static final boolean CHATTY = Log.isLoggable("NetworkControllerChat", 3);

    public NetworkControllerImpl(Context context, Looper bgLooper) {
        this(context, (ConnectivityManager) context.getSystemService("connectivity"), (TelephonyManager) context.getSystemService("phone"), (WifiManager) context.getSystemService("wifi"), SubscriptionManager.from(context), Config.readConfig(context), bgLooper, new CallbackHandler(), new AccessPointControllerImpl(context, bgLooper), new DataUsageController(context), new SubscriptionDefaults());
        this.mReceiverHandler.post(this.mRegisterListeners);
    }

    NetworkControllerImpl(Context context, ConnectivityManager connectivityManager, TelephonyManager telephonyManager, WifiManager wifiManager, SubscriptionManager subManager, Config config, Looper bgLooper, CallbackHandler callbackHandler, AccessPointControllerImpl accessPointController, DataUsageController dataUsageController, SubscriptionDefaults defaultsHandler) {
        this.mMobileSignalControllers = new HashMap();
        this.mConnectedTransports = new BitSet();
        this.mValidatedTransports = new BitSet();
        this.mAirplaneMode = false;
        this.mLocale = null;
        this.mCurrentSubscriptions = new ArrayList();
        this.mSlotCount = 0;
        this.mRegisterListeners = new Runnable() {
            @Override
            public void run() {
                NetworkControllerImpl.this.registerListeners();
            }
        };
        this.mContext = context;
        this.mConfig = config;
        this.mReceiverHandler = new Handler(bgLooper);
        this.mCallbackHandler = callbackHandler;
        this.mDataSaverController = new DataSaverController(context);
        this.mSubscriptionManager = subManager;
        this.mSubDefaults = defaultsHandler;
        this.mConnectivityManager = connectivityManager;
        this.mHasMobileDataFeature = this.mConnectivityManager.isNetworkSupported(0);
        this.mSlotCount = SIMHelper.getSlotCount();
        this.mNetworkName = new String[this.mSlotCount];
        this.mEmergencyPhone = new boolean[this.mSlotCount];
        this.mPhone = telephonyManager;
        this.mWifiManager = wifiManager;
        this.mLocale = this.mContext.getResources().getConfiguration().locale;
        this.mAccessPoints = accessPointController;
        this.mDataUsageController = dataUsageController;
        this.mDataUsageController.setNetworkController(this);
        this.mDataUsageController.setCallback(new DataUsageController.Callback() {
            @Override
            public void onMobileDataEnabled(boolean enabled) {
                NetworkControllerImpl.this.mCallbackHandler.setMobileDataEnabled(enabled);
            }
        });
        this.mWifiSignalController = new WifiSignalController(this.mContext, this.mHasMobileDataFeature, this.mCallbackHandler, this);
        this.mEthernetSignalController = new EthernetSignalController(this.mContext, this.mCallbackHandler, this);
        updateAirplaneMode(true);
        this.mStatusBarSystemUIExt = PluginManager.getSystemUIStatusBarExt(this.mContext);
    }

    @Override
    public DataSaverController getDataSaverController() {
        return this.mDataSaverController;
    }

    public void registerListeners() {
        SubListener subListener = null;
        for (MobileSignalController mobileSignalController : this.mMobileSignalControllers.values()) {
            mobileSignalController.registerListener();
        }
        if (this.mSubscriptionListener == null) {
            this.mSubscriptionListener = new SubListener(this, subListener);
        }
        this.mSubscriptionManager.addOnSubscriptionsChangedListener(this.mSubscriptionListener);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.wifi.RSSI_CHANGED");
        filter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        filter.addAction("android.net.wifi.STATE_CHANGE");
        filter.addAction("android.intent.action.SIM_STATE_CHANGED");
        filter.addAction("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED");
        filter.addAction("android.intent.action.ACTION_DEFAULT_VOICE_SUBSCRIPTION_CHANGED");
        filter.addAction("android.intent.action.SERVICE_STATE");
        filter.addAction("android.provider.Telephony.SPN_STRINGS_UPDATED");
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        filter.addAction("android.net.conn.INET_CONDITION_ACTION");
        filter.addAction("android.intent.action.AIRPLANE_MODE");
        addCustomizedAction(filter);
        this.mContext.registerReceiver(this, filter, null, this.mReceiverHandler);
        this.mListening = true;
        updateMobileControllers();
    }

    private void addCustomizedAction(IntentFilter filter) {
        filter.addAction("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED");
        filter.addAction("com.android.ims.IMS_STATE_CHANGED");
        filter.addAction("android.intent.action.ACTION_PREBOOT_IPO");
        filter.addAction("android.intent.action.ACTION_SHUTDOWN_IPO");
    }

    private void unregisterListeners() {
        this.mListening = false;
        for (MobileSignalController mobileSignalController : this.mMobileSignalControllers.values()) {
            mobileSignalController.unregisterListener();
        }
        this.mSubscriptionManager.removeOnSubscriptionsChangedListener(this.mSubscriptionListener);
        this.mContext.unregisterReceiver(this);
    }

    @Override
    public NetworkController.AccessPointController getAccessPointController() {
        return this.mAccessPoints;
    }

    @Override
    public DataUsageController getMobileDataController() {
        return this.mDataUsageController;
    }

    @Override
    public void addEmergencyListener(NetworkController.EmergencyListener listener) {
        this.mCallbackHandler.setListening(listener, true);
        this.mCallbackHandler.setEmergencyCallsOnly(isEmergencyOnly());
    }

    @Override
    public void removeEmergencyListener(NetworkController.EmergencyListener listener) {
        this.mCallbackHandler.setListening(listener, false);
    }

    @Override
    public boolean hasMobileDataFeature() {
        return this.mHasMobileDataFeature;
    }

    @Override
    public boolean hasVoiceCallingFeature() {
        return this.mPhone.getPhoneType() != 0;
    }

    private MobileSignalController getDataController() {
        int dataSubId = this.mSubDefaults.getDefaultDataSubId();
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

    @Override
    public String getMobileDataNetworkName() {
        MobileSignalController controller = getDataController();
        return controller != null ? controller.getState().networkNameData : "";
    }

    public boolean isEmergencyOnly() {
        if (this.mMobileSignalControllers.size() == 0) {
            Log.d("NetworkController", "isEmergencyOnly No sims ");
            this.mEmergencySource = 0;
            for (int i = 0; i < this.mEmergencyPhone.length; i++) {
                if (this.mEmergencyPhone[i]) {
                    if (DEBUG) {
                        Log.d("NetworkController", "Found emergency in phone " + i);
                    }
                    return true;
                }
            }
            return false;
        }
        int voiceSubId = this.mSubDefaults.getDefaultVoiceSubId();
        if (!SubscriptionManager.isValidSubscriptionId(voiceSubId)) {
            for (MobileSignalController mobileSignalController : this.mMobileSignalControllers.values()) {
                if (!mobileSignalController.getState().isEmergency) {
                    this.mEmergencySource = mobileSignalController.mSubscriptionInfo.getSubscriptionId() + 100;
                    if (DEBUG) {
                        Log.d("NetworkController", "Found emergency " + mobileSignalController.mTag);
                    }
                    return false;
                }
            }
        }
        if (this.mMobileSignalControllers.containsKey(Integer.valueOf(voiceSubId))) {
            this.mEmergencySource = voiceSubId + 200;
            if (DEBUG) {
                Log.d("NetworkController", "Getting emergency from " + voiceSubId);
            }
            return this.mMobileSignalControllers.get(Integer.valueOf(voiceSubId)).getState().isEmergency;
        }
        if (DEBUG) {
            Log.e("NetworkController", "Cannot find controller for voice sub: " + voiceSubId);
        }
        this.mEmergencySource = voiceSubId + 300;
        return true;
    }

    void recalculateEmergency() {
        this.mIsEmergency = isEmergencyOnly();
        this.mCallbackHandler.setEmergencyCallsOnly(this.mIsEmergency);
    }

    @Override
    public void addSignalCallback(NetworkController.SignalCallback cb) {
        cb.setSubs(this.mCurrentSubscriptions);
        cb.setIsAirplaneMode(new NetworkController.IconState(this.mAirplaneMode, R.drawable.stat_sys_airplane_mode, R.string.accessibility_airplane_mode, this.mContext));
        this.mWifiSignalController.notifyListeners(cb);
        this.mEthernetSignalController.notifyListeners(cb);
        for (MobileSignalController mobileSignalController : this.mMobileSignalControllers.values()) {
            mobileSignalController.notifyListeners(cb);
        }
        this.mCallbackHandler.setListening(cb, true);
        cb.setNoSims(this.mHasNoSims);
    }

    @Override
    public void removeSignalCallback(NetworkController.SignalCallback cb) {
        this.mCallbackHandler.setListening(cb, false);
    }

    @Override
    public void setWifiEnabled(final boolean enabled) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            public Void doInBackground(Void... args) {
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
    public void onReceive(Context context, Intent intent) {
        if (CHATTY) {
            Log.d("NetworkController", "onReceive: intent=" + intent);
        }
        String action = intent.getAction();
        if (action.equals("android.net.conn.CONNECTIVITY_CHANGE") || action.equals("android.net.conn.INET_CONDITION_ACTION")) {
            updateConnectivity();
            return;
        }
        if (action.equals("android.intent.action.AIRPLANE_MODE")) {
            refreshLocale();
            boolean airplaneMode = intent.getBooleanExtra("state", false);
            updateAirplaneMode(airplaneMode, false);
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
        if (action.equals("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED")) {
            updateMobileControllersEx(intent);
            refreshPlmnCarrierLabel();
            return;
        }
        if (action.equals("android.intent.action.SERVICE_STATE")) {
            this.mLastServiceState = ServiceState.newFromBundle(intent.getExtras());
            if (this.mLastServiceState == null) {
                return;
            }
            int phoneId = intent.getIntExtra("phone", 0);
            this.mEmergencyPhone[phoneId] = this.mLastServiceState.isEmergencyOnly();
            if (DEBUG) {
                Log.d("NetworkController", "Service State changed...phoneId: " + phoneId + " ,isEmergencyOnly: " + this.mEmergencyPhone[phoneId]);
            }
            if (this.mMobileSignalControllers.size() != 0) {
                return;
            }
            recalculateEmergency();
            return;
        }
        if (action.equals("com.android.ims.IMS_STATE_CHANGED")) {
            Log.d("NetworkController", "onRecevie ACTION_IMS_STATE_CHANGED");
            handleIMSAction(intent);
            return;
        }
        if (action.equals("android.intent.action.ACTION_PREBOOT_IPO")) {
            updateAirplaneMode(false);
            return;
        }
        if (action.equals("android.intent.action.ACTION_SHUTDOWN_IPO")) {
            Log.d("NetworkController", "IPO SHUTDOWN!!!");
            List<SubscriptionInfo> subscriptions = Collections.emptyList();
            setCurrentSubscriptions(subscriptions);
            updateNoSims();
            recalculateEmergency();
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

    public void onConfigurationChanged() {
        this.mConfig = Config.readConfig(this.mContext);
        this.mReceiverHandler.post(new Runnable() {
            @Override
            public void run() {
                NetworkControllerImpl.this.handleConfigurationChanged();
            }
        });
    }

    void handleConfigurationChanged() {
        for (MobileSignalController mobileSignalController : this.mMobileSignalControllers.values()) {
            mobileSignalController.setConfiguration(this.mConfig);
        }
        refreshLocale();
    }

    private void updateMobileControllersEx(Intent intent) {
        int detectedType = 4;
        if (intent != null) {
            detectedType = intent.getIntExtra("simDetectStatus", 0);
            Log.d("NetworkController", "updateMobileControllers detectedType: " + detectedType);
        }
        if (detectedType != 3) {
            updateNoSims();
        } else {
            updateMobileControllers();
        }
    }

    public void updateMobileControllers() {
        SIMHelper.updateSIMInfos(this.mContext);
        if (!this.mListening) {
            if (DEBUG) {
                Log.d("NetworkController", "updateMobileControllers: it's not listening");
                return;
            }
            return;
        }
        doUpdateMobileControllers();
    }

    void doUpdateMobileControllers() {
        List<SubscriptionInfo> subscriptions = this.mSubscriptionManager.getActiveSubscriptionInfoList();
        if (subscriptions == null) {
            Log.d("NetworkController", "subscriptions is null");
            subscriptions = Collections.emptyList();
        }
        if (hasCorrectMobileControllers(subscriptions)) {
            updateNoSims();
            return;
        }
        setCurrentSubscriptions(subscriptions);
        updateNoSims();
        recalculateEmergency();
    }

    protected void updateNoSims() {
        boolean hasNoSims = this.mHasMobileDataFeature && this.mMobileSignalControllers.size() == 0;
        if (hasNoSims == this.mHasNoSims) {
            return;
        }
        this.mHasNoSims = hasNoSims;
        this.mCallbackHandler.setNoSims(this.mHasNoSims);
    }

    void setCurrentSubscriptions(List<SubscriptionInfo> subscriptions) {
        Collections.sort(subscriptions, new Comparator<SubscriptionInfo>() {
            @Override
            public int compare(SubscriptionInfo lhs, SubscriptionInfo rhs) {
                if (lhs.getSimSlotIndex() == rhs.getSimSlotIndex()) {
                    return lhs.getSubscriptionId() - rhs.getSubscriptionId();
                }
                return lhs.getSimSlotIndex() - rhs.getSimSlotIndex();
            }
        });
        this.mCurrentSubscriptions = subscriptions;
        HashMap<Integer, MobileSignalController> cachedControllers = new HashMap<>(this.mMobileSignalControllers);
        this.mMobileSignalControllers.clear();
        int num = subscriptions.size();
        for (int i = 0; i < num; i++) {
            int subId = subscriptions.get(i).getSubscriptionId();
            if (cachedControllers.containsKey(Integer.valueOf(subId))) {
                MobileSignalController msc = cachedControllers.remove(Integer.valueOf(subId));
                msc.mSubscriptionInfo = subscriptions.get(i);
                this.mMobileSignalControllers.put(Integer.valueOf(subId), msc);
            } else {
                MobileSignalController controller = new MobileSignalController(this.mContext, this.mConfig, this.mHasMobileDataFeature, this.mPhone, this.mCallbackHandler, this, subscriptions.get(i), this.mSubDefaults, this.mReceiverHandler.getLooper());
                controller.setUserSetupComplete(this.mUserSetup);
                this.mMobileSignalControllers.put(Integer.valueOf(subId), controller);
                if (subscriptions.get(i).getSimSlotIndex() == 0) {
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
        this.mCallbackHandler.setSubs(subscriptions);
        notifyAllListeners();
        pushConnectivityToSignals();
        updateAirplaneMode(true);
    }

    public void setUserSetupComplete(final boolean userSetup) {
        this.mReceiverHandler.post(new Runnable() {
            @Override
            public void run() {
                NetworkControllerImpl.this.handleSetUserSetupComplete(userSetup);
            }
        });
    }

    void handleSetUserSetupComplete(boolean userSetup) {
        this.mUserSetup = userSetup;
        for (MobileSignalController controller : this.mMobileSignalControllers.values()) {
            controller.setUserSetupComplete(this.mUserSetup);
        }
    }

    boolean hasCorrectMobileControllers(List<SubscriptionInfo> allSubscriptions) {
        if (allSubscriptions.size() != this.mMobileSignalControllers.size()) {
            Log.d("NetworkController", "size not equals, reset subInfo");
            return false;
        }
        for (SubscriptionInfo info : allSubscriptions) {
            MobileSignalController msc = this.mMobileSignalControllers.get(Integer.valueOf(info.getSubscriptionId()));
            if (msc == null || msc.mSubscriptionInfo.getSimSlotIndex() != info.getSimSlotIndex()) {
                Log.d("NetworkController", "info_subId = " + info.getSubscriptionId() + " info_slotId = " + info.getSimSlotIndex());
                return false;
            }
        }
        return true;
    }

    private void updateAirplaneMode(boolean force) {
        boolean airplaneMode = Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) == 1;
        updateAirplaneMode(airplaneMode, force);
    }

    private void updateAirplaneMode(boolean airplaneMode, boolean force) {
        if (airplaneMode == this.mAirplaneMode && !force) {
            return;
        }
        this.mAirplaneMode = airplaneMode;
        for (MobileSignalController mobileSignalController : this.mMobileSignalControllers.values()) {
            mobileSignalController.setAirplaneMode(this.mAirplaneMode);
        }
        notifyListeners();
    }

    private void refreshLocale() {
        Locale current = this.mContext.getResources().getConfiguration().locale;
        if (current.equals(this.mLocale)) {
            return;
        }
        this.mLocale = current;
        notifyAllListeners();
    }

    private void notifyAllListeners() {
        notifyListeners();
        for (MobileSignalController mobileSignalController : this.mMobileSignalControllers.values()) {
            mobileSignalController.notifyListeners();
        }
        this.mWifiSignalController.notifyListeners();
        this.mEthernetSignalController.notifyListeners();
    }

    private void notifyListeners() {
        this.mCallbackHandler.setIsAirplaneMode(new NetworkController.IconState(this.mAirplaneMode, R.drawable.stat_sys_airplane_mode, R.string.accessibility_airplane_mode, this.mContext));
        this.mCallbackHandler.setNoSims(this.mHasNoSims);
    }

    private void updateConnectivity() {
        this.mConnectedTransports.clear();
        this.mValidatedTransports.clear();
        for (NetworkCapabilities nc : this.mConnectivityManager.getDefaultNetworkCapabilitiesForUser(this.mCurrentUserId)) {
            for (int transportType : nc.getTransportTypes()) {
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
        this.mInetCondition = this.mValidatedTransports.isEmpty() ? false : true;
        pushConnectivityToSignals();
    }

    private void pushConnectivityToSignals() {
        for (MobileSignalController mobileSignalController : this.mMobileSignalControllers.values()) {
            mobileSignalController.updateConnectivity(this.mConnectedTransports, this.mValidatedTransports);
        }
        this.mWifiSignalController.updateConnectivity(this.mConnectedTransports, this.mValidatedTransports);
        this.mEthernetSignalController.updateConnectivity(this.mConnectedTransports, this.mValidatedTransports);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("NetworkController state:");
        pw.println("  - telephony ------");
        pw.print("  hasVoiceCallingFeature()=");
        pw.println(hasVoiceCallingFeature());
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
        pw.print("  mLastServiceState=");
        pw.println(this.mLastServiceState);
        pw.print("  mIsEmergency=");
        pw.println(this.mIsEmergency);
        pw.print("  mEmergencySource=");
        pw.println(emergencyToString(this.mEmergencySource));
        for (MobileSignalController mobileSignalController : this.mMobileSignalControllers.values()) {
            mobileSignalController.dump(pw);
        }
        this.mWifiSignalController.dump(pw);
        this.mEthernetSignalController.dump(pw);
        this.mAccessPoints.dump(pw);
    }

    private static final String emergencyToString(int emergencySource) {
        if (emergencySource > 300) {
            return "NO_SUB(" + (emergencySource - 300) + ")";
        }
        if (emergencySource > 200) {
            return "VOICE_CONTROLLER(" + (emergencySource - 200) + ")";
        }
        if (emergencySource > 100) {
            return "FIRST_CONTROLLER(" + (emergencySource - 100) + ")";
        }
        if (emergencySource == 0) {
            return "NO_CONTROLLERS";
        }
        return "UNKNOWN_SOURCE";
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
            this.mDemoInetCondition = this.mInetCondition;
            this.mDemoWifiState = this.mWifiSignalController.getState();
            return;
        }
        if (this.mDemoMode && command.equals("exit")) {
            if (DEBUG) {
                Log.d("NetworkController", "Exiting demo mode");
            }
            this.mDemoMode = false;
            updateMobileControllers();
            Iterator controller$iterator = this.mMobileSignalControllers.values().iterator();
            while (controller$iterator.hasNext()) {
                ((MobileSignalController) controller$iterator.next()).resetLastState();
            }
            this.mWifiSignalController.resetLastState();
            this.mReceiverHandler.post(this.mRegisterListeners);
            notifyAllListeners();
            return;
        }
        if (!this.mDemoMode || !command.equals("network")) {
            return;
        }
        String airplane = args.getString("airplane");
        if (airplane != null) {
            boolean show = airplane.equals("show");
            this.mCallbackHandler.setIsAirplaneMode(new NetworkController.IconState(show, R.drawable.stat_sys_airplane_mode, R.string.accessibility_airplane_mode, this.mContext));
        }
        String fully = args.getString("fully");
        if (fully != null) {
            this.mDemoInetCondition = Boolean.parseBoolean(fully);
            BitSet connected = new BitSet();
            if (this.mDemoInetCondition) {
                connected.set(this.mWifiSignalController.mTransportType);
            }
            this.mWifiSignalController.updateConnectivity(connected, connected);
            for (MobileSignalController controller : this.mMobileSignalControllers.values()) {
                if (this.mDemoInetCondition) {
                    connected.set(controller.mTransportType);
                }
                controller.updateConnectivity(connected, connected);
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
            int num = MathUtils.constrain(Integer.parseInt(sims), 1, 8);
            List<SubscriptionInfo> subs = new ArrayList<>();
            if (num != this.mMobileSignalControllers.size()) {
                this.mMobileSignalControllers.clear();
                int start = this.mSubscriptionManager.getActiveSubscriptionInfoCountMax();
                for (int i = start; i < start + num; i++) {
                    subs.add(addSignalController(i, i));
                }
                this.mCallbackHandler.setSubs(subs);
            }
        }
        String nosim = args.getString("nosim");
        if (nosim != null) {
            this.mHasNoSims = nosim.equals("show");
            this.mCallbackHandler.setNoSims(this.mHasNoSims);
        }
        String mobile = args.getString("mobile");
        if (mobile != null) {
            boolean show3 = mobile.equals("show");
            String datatype = args.getString("datatype");
            String slotString = args.getString("slot");
            int slot = MathUtils.constrain(TextUtils.isEmpty(slotString) ? 0 : Integer.parseInt(slotString), 0, 8);
            List<SubscriptionInfo> subs2 = new ArrayList<>();
            while (this.mMobileSignalControllers.size() <= slot) {
                int nextSlot = this.mMobileSignalControllers.size();
                subs2.add(addSignalController(nextSlot, nextSlot));
            }
            if (!subs2.isEmpty()) {
                this.mCallbackHandler.setSubs(subs2);
            }
            MobileSignalController controller2 = ((MobileSignalController[]) this.mMobileSignalControllers.values().toArray(new MobileSignalController[0]))[slot];
            controller2.getState().dataSim = datatype != null;
            if (datatype != null) {
                MobileSignalController.MobileState state = controller2.getState();
                if (datatype.equals("1x")) {
                    mobileIconGroup = TelephonyIcons.ONE_X;
                } else if (datatype.equals("3g")) {
                    mobileIconGroup = TelephonyIcons.THREE_G;
                } else if (datatype.equals("4g")) {
                    mobileIconGroup = TelephonyIcons.FOUR_G;
                } else if (datatype.equals("e")) {
                    mobileIconGroup = TelephonyIcons.E;
                } else if (datatype.equals("g")) {
                    mobileIconGroup = TelephonyIcons.G;
                } else if (datatype.equals("h")) {
                    mobileIconGroup = TelephonyIcons.H;
                } else if (datatype.equals("lte")) {
                    mobileIconGroup = TelephonyIcons.LTE;
                } else {
                    mobileIconGroup = datatype.equals("roam") ? TelephonyIcons.ROAMING : TelephonyIcons.UNKNOWN;
                }
                state.iconGroup = mobileIconGroup;
            }
            int[][] icons = TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH;
            String level2 = args.getString("level");
            if (level2 != null) {
                controller2.getState().level = level2.equals("null") ? -1 : Math.min(Integer.parseInt(level2), icons[0].length - 1);
                controller2.getState().connected = controller2.getState().level >= 0;
            }
            controller2.getState().enabled = show3;
            controller2.notifyListeners();
        }
        String carrierNetworkChange = args.getString("carriernetworkchange");
        if (carrierNetworkChange == null) {
            return;
        }
        boolean show4 = carrierNetworkChange.equals("show");
        Iterator controller$iterator2 = this.mMobileSignalControllers.values().iterator();
        while (controller$iterator2.hasNext()) {
            ((MobileSignalController) controller$iterator2.next()).setCarrierNetworkChangeMode(show4);
        }
    }

    private SubscriptionInfo addSignalController(int id, int simSlotIndex) {
        SubscriptionInfo info = new SubscriptionInfo(id, "", simSlotIndex, "", "", 0, 0, "", 0, null, 0, 0, "", 0);
        this.mMobileSignalControllers.put(Integer.valueOf(id), new MobileSignalController(this.mContext, this.mConfig, this.mHasMobileDataFeature, this.mPhone, this.mCallbackHandler, this, info, this.mSubDefaults, this.mReceiverHandler.getLooper()));
        return info;
    }

    private class SubListener extends SubscriptionManager.OnSubscriptionsChangedListener {
        SubListener(NetworkControllerImpl this$0, SubListener subListener) {
            this();
        }

        private SubListener() {
        }

        @Override
        public void onSubscriptionsChanged() {
            NetworkControllerImpl.this.updateMobileControllers();
        }
    }

    public static class SubscriptionDefaults {
        public int getDefaultVoiceSubId() {
            return SubscriptionManager.getDefaultVoiceSubscriptionId();
        }

        public int getDefaultDataSubId() {
            return SubscriptionManager.getDefaultDataSubscriptionId();
        }
    }

    public static class Config {
        public boolean hspaDataDistinguishable;
        public boolean showAtLeast3G = false;
        public boolean alwaysShowCdmaRssi = false;
        public boolean show4gForLte = false;

        static Config readConfig(Context context) {
            Config config = new Config();
            Resources res = context.getResources();
            config.showAtLeast3G = res.getBoolean(R.bool.config_showMin3G);
            config.alwaysShowCdmaRssi = res.getBoolean(android.R.^attr-private.headerRemoveIconIfEmpty);
            config.show4gForLte = res.getBoolean(R.bool.config_show4GForLTE);
            config.hspaDataDistinguishable = res.getBoolean(R.bool.config_hspa_data_distinguishable);
            return config;
        }
    }

    void handleIMSAction(Intent intent) {
        int phoneId = intent.getIntExtra("android:phone_id", -1);
        this.mStatusBarSystemUIExt.setImsSlotId(phoneId);
        for (MobileSignalController controller : this.mMobileSignalControllers.values()) {
            if (controller.getControllerSubInfo().getSimSlotIndex() == intent.getIntExtra("android:phone_id", -1)) {
                controller.handleBroadcast(intent);
                return;
            }
        }
    }

    public void refreshPlmnCarrierLabel() {
        for (int i = 0; i < this.mSlotCount; i++) {
            boolean found = false;
            Iterator entry$iterator = this.mMobileSignalControllers.entrySet().iterator();
            while (true) {
                if (!entry$iterator.hasNext()) {
                    break;
                }
                Map.Entry<Integer, MobileSignalController> entry = (Map.Entry) entry$iterator.next();
                entry.getKey().intValue();
                int slotId = -1;
                MobileSignalController controller = entry.getValue();
                if (controller.getControllerSubInfo() != null) {
                    slotId = controller.getControllerSubInfo().getSimSlotIndex();
                }
                if (i == slotId) {
                    this.mNetworkName[slotId] = ((MobileSignalController.MobileState) controller.mCurrentState).networkName;
                    PluginManager.getStatusBarPlmnPlugin(this.mContext).updateCarrierLabel(i, true, controller.getControllserHasService(), this.mNetworkName);
                    this.mStatusBarSystemUIExt.setSimInserted(i, true);
                    found = true;
                    break;
                }
            }
            if (!found) {
                this.mNetworkName[i] = this.mContext.getString(android.R.string.config_help_url_action_disabled_by_advanced_protection);
                PluginManager.getStatusBarPlmnPlugin(this.mContext).updateCarrierLabel(i, false, false, this.mNetworkName);
                this.mStatusBarSystemUIExt.setSimInserted(i, false);
            }
        }
    }
}
