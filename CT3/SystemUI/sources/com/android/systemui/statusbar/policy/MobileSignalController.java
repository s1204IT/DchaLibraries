package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.os.SystemProperties;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NetworkControllerImpl;
import com.android.systemui.statusbar.policy.SignalController;
import com.mediatek.systemui.PluginManager;
import com.mediatek.systemui.ext.IMobileIconExt;
import com.mediatek.systemui.ext.ISystemUIStatusBarExt;
import com.mediatek.systemui.statusbar.networktype.NetworkTypeUtils;
import com.mediatek.telephony.TelephonyManagerEx;
import java.io.PrintWriter;
import java.util.BitSet;
import java.util.Objects;

public class MobileSignalController extends SignalController<MobileState, MobileIconGroup> {
    private NetworkControllerImpl.Config mConfig;
    private int mDataNetType;
    private int mDataState;
    private MobileIconGroup mDefaultIcons;
    private final NetworkControllerImpl.SubscriptionDefaults mDefaults;
    private IMobileIconExt mMobileIconExt;
    private final String mNetworkNameDefault;
    private final String mNetworkNameSeparator;
    final SparseArray<MobileIconGroup> mNetworkToIconLookup;
    private final TelephonyManager mPhone;
    final PhoneStateListener mPhoneStateListener;
    private ServiceState mServiceState;
    private SignalStrength mSignalStrength;
    private ISystemUIStatusBarExt mStatusBarExt;
    SubscriptionInfo mSubscriptionInfo;

    public MobileSignalController(Context context, NetworkControllerImpl.Config config, boolean hasMobileData, TelephonyManager phone, CallbackHandler callbackHandler, NetworkControllerImpl networkController, SubscriptionInfo info, NetworkControllerImpl.SubscriptionDefaults defaults, Looper receiverLooper) {
        super("MobileSignalController(" + info.getSubscriptionId() + ")", context, 0, callbackHandler, networkController);
        this.mDataNetType = 0;
        this.mDataState = 0;
        this.mNetworkToIconLookup = new SparseArray<>();
        this.mConfig = config;
        this.mPhone = phone;
        this.mDefaults = defaults;
        this.mSubscriptionInfo = info;
        this.mMobileIconExt = PluginManager.getMobileIconExt(context);
        this.mStatusBarExt = PluginManager.getSystemUIStatusBarExt(context);
        this.mPhoneStateListener = new MobilePhoneStateListener(info.getSubscriptionId(), receiverLooper);
        this.mNetworkNameSeparator = getStringIfExists(R.string.status_bar_network_name_separator);
        this.mNetworkNameDefault = getStringIfExists(android.R.string.config_help_url_action_disabled_by_advanced_protection);
        mapIconSets();
        String networkName = info.getCarrierName() != null ? info.getCarrierName().toString() : this.mNetworkNameDefault;
        MobileState mobileState = (MobileState) this.mLastState;
        ((MobileState) this.mCurrentState).networkName = networkName;
        mobileState.networkName = networkName;
        MobileState mobileState2 = (MobileState) this.mLastState;
        ((MobileState) this.mCurrentState).networkNameData = networkName;
        mobileState2.networkNameData = networkName;
        MobileState mobileState3 = (MobileState) this.mLastState;
        ((MobileState) this.mCurrentState).enabled = hasMobileData;
        mobileState3.enabled = hasMobileData;
        MobileState mobileState4 = (MobileState) this.mLastState;
        MobileIconGroup mobileIconGroup = this.mDefaultIcons;
        ((MobileState) this.mCurrentState).iconGroup = mobileIconGroup;
        mobileState4.iconGroup = mobileIconGroup;
        initImsRegisterState();
        updateDataSim();
    }

    private void initImsRegisterState() {
        int phoneId = SubscriptionManager.getPhoneId(this.mSubscriptionInfo.getSubscriptionId());
        try {
            boolean imsRegStatus = ImsManager.getInstance(this.mContext, phoneId).getImsRegInfo();
            ((MobileState) this.mCurrentState).imsRegState = imsRegStatus ? 0 : 1;
            Log.d(this.mTag, "init imsRegState:" + ((MobileState) this.mCurrentState).imsRegState + ",phoneId:" + phoneId);
        } catch (ImsException e) {
            Log.e(this.mTag, "Fail to get Ims Status");
        }
    }

    public void setConfiguration(NetworkControllerImpl.Config config) {
        this.mConfig = config;
        mapIconSets();
        updateTelephony();
    }

    public void setAirplaneMode(boolean airplaneMode) {
        ((MobileState) this.mCurrentState).airplaneMode = airplaneMode;
        notifyListenersIfNecessary();
    }

    public void setUserSetupComplete(boolean userSetup) {
        ((MobileState) this.mCurrentState).userSetup = userSetup;
        notifyListenersIfNecessary();
    }

    @Override
    public void updateConnectivity(BitSet connectedTransports, BitSet validatedTransports) {
        boolean isValidated = validatedTransports.get(this.mTransportType);
        ((MobileState) this.mCurrentState).isDefault = connectedTransports.get(this.mTransportType);
        ((MobileState) this.mCurrentState).inetCondition = (isValidated || !((MobileState) this.mCurrentState).isDefault) ? 1 : 0;
        Log.d(this.mTag, "mCurrentState.inetCondition = " + ((MobileState) this.mCurrentState).inetCondition);
        ((MobileState) this.mCurrentState).inetCondition = this.mMobileIconExt.customizeMobileNetCondition(((MobileState) this.mCurrentState).inetCondition);
        notifyListenersIfNecessary();
    }

    public void setCarrierNetworkChangeMode(boolean carrierNetworkChangeMode) {
        ((MobileState) this.mCurrentState).carrierNetworkChangeMode = carrierNetworkChangeMode;
        updateTelephony();
    }

    public void registerListener() {
        this.mPhone.listen(this.mPhoneStateListener, 66017);
        this.mStatusBarExt.registerOpStateListener();
    }

    public void unregisterListener() {
        this.mPhone.listen(this.mPhoneStateListener, 0);
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
        if (this.mConfig.hspaDataDistinguishable) {
            hGroup = TelephonyIcons.H;
        }
        this.mNetworkToIconLookup.put(8, hGroup);
        this.mNetworkToIconLookup.put(9, hGroup);
        this.mNetworkToIconLookup.put(10, hGroup);
        this.mNetworkToIconLookup.put(15, hGroup);
        if (this.mConfig.show4gForLte) {
            this.mNetworkToIconLookup.put(13, TelephonyIcons.FOUR_G);
        } else {
            this.mNetworkToIconLookup.put(13, TelephonyIcons.LTE);
        }
        this.mNetworkToIconLookup.put(18, TelephonyIcons.WFC);
        this.mNetworkToIconLookup.put(139, TelephonyIcons.FOUR_GA);
    }

    @Override
    public void notifyListeners(NetworkController.SignalCallback callback) {
        boolean dataDisabled;
        boolean z;
        boolean z2;
        boolean z3;
        MobileIconGroup icons = getIcons();
        String contentDescription = getStringIfExists(getContentDescription());
        String dataContentDescription = getStringIfExists(icons.mDataContentDescription);
        if (((MobileState) this.mCurrentState).iconGroup != TelephonyIcons.DATA_DISABLED) {
            dataDisabled = false;
        } else {
            dataDisabled = ((MobileState) this.mCurrentState).userSetup;
        }
        int iconId = getCurrentIconId();
        int iconId2 = this.mStatusBarExt.getCustomizeSignalStrengthIcon(this.mSubscriptionInfo.getSubscriptionId(), iconId, this.mSignalStrength, this.mDataNetType, this.mServiceState);
        if (((MobileState) this.mCurrentState).dataConnected || ((MobileState) this.mCurrentState).iconGroup == TelephonyIcons.ROAMING) {
            z = true;
        } else {
            z = dataDisabled;
        }
        NetworkController.IconState statusIcon = new NetworkController.IconState(((MobileState) this.mCurrentState).enabled && !((MobileState) this.mCurrentState).airplaneMode, iconId2, contentDescription);
        int qsTypeIcon = 0;
        NetworkController.IconState qsIcon = null;
        String description = null;
        if (((MobileState) this.mCurrentState).dataSim) {
            qsTypeIcon = z ? icons.mQsDataType : 0;
            boolean z4 = ((MobileState) this.mCurrentState).enabled && !((MobileState) this.mCurrentState).isEmergency;
            qsIcon = new NetworkController.IconState(z4, getQsCurrentIconId(), contentDescription);
            description = ((MobileState) this.mCurrentState).isEmergency ? null : ((MobileState) this.mCurrentState).networkName;
        }
        if (!((MobileState) this.mCurrentState).dataConnected || ((MobileState) this.mCurrentState).carrierNetworkChangeMode) {
            z2 = false;
        } else {
            z2 = ((MobileState) this.mCurrentState).activityIn;
        }
        if (!((MobileState) this.mCurrentState).dataConnected || ((MobileState) this.mCurrentState).carrierNetworkChangeMode) {
            z3 = false;
        } else {
            z3 = ((MobileState) this.mCurrentState).activityOut;
        }
        if (((MobileState) this.mCurrentState).isDefault || ((MobileState) this.mCurrentState).iconGroup == TelephonyIcons.ROAMING) {
            dataDisabled = true;
        }
        boolean showDataIcon = z & dataDisabled;
        int typeIcon = showDataIcon ? icons.mDataType : 0;
        int networkIcon = ((MobileState) this.mCurrentState).networkIcon;
        int volteIcon = (!((MobileState) this.mCurrentState).airplaneMode || isWfcEnable()) ? ((MobileState) this.mCurrentState).volteIcon : 0;
        callback.setMobileDataIndicators(statusIcon, qsIcon, this.mStatusBarExt.getDataTypeIcon(this.mSubscriptionInfo.getSubscriptionId(), typeIcon, this.mDataNetType, ((MobileState) this.mCurrentState).dataConnected ? 2 : 0, this.mServiceState), this.mStatusBarExt.getNetworkTypeIcon(this.mSubscriptionInfo.getSubscriptionId(), networkIcon, this.mDataNetType, this.mServiceState), volteIcon, qsTypeIcon, z2, z3, dataContentDescription, description, icons.mIsWide, this.mSubscriptionInfo.getSubscriptionId());
        this.mNetworkController.refreshPlmnCarrierLabel();
    }

    @Override
    public MobileState cleanState() {
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
        if (this.mServiceState != null) {
            return this.mServiceState.isEmergencyOnly();
        }
        return false;
    }

    private boolean isRoaming() {
        if (isCdma()) {
            if (this.mServiceState == null) {
                return false;
            }
            int iconMode = this.mServiceState.getCdmaEriIconMode();
            if (this.mServiceState == null || this.mServiceState.getCdmaEriIconIndex() == 1) {
                return false;
            }
            return iconMode == 0 || iconMode == 1;
        }
        return this.mStatusBarExt.needShowRoamingIcons(this.mServiceState != null ? this.mServiceState.getRoaming() : false);
    }

    public boolean isLteNetWork() {
        return this.mDataNetType == 13 || this.mDataNetType == 139;
    }

    private boolean isCarrierNetworkChangeActive() {
        return ((MobileState) this.mCurrentState).carrierNetworkChangeMode;
    }

    public void handleBroadcast(Intent intent) {
        String action = intent.getAction();
        if (action.equals("android.provider.Telephony.SPN_STRINGS_UPDATED")) {
            updateNetworkName(intent.getBooleanExtra("showSpn", false), intent.getStringExtra("spn"), intent.getStringExtra("spnData"), intent.getBooleanExtra("showPlmn", false), intent.getStringExtra("plmn"));
            notifyListenersIfNecessary();
        } else if (action.equals("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED")) {
            updateDataSim();
            notifyListenersIfNecessary();
        } else {
            if (!action.equals("com.android.ims.IMS_STATE_CHANGED")) {
                return;
            }
            handleImsAction(intent);
            notifyListenersIfNecessary();
        }
    }

    private void handleImsAction(Intent intent) {
        ((MobileState) this.mCurrentState).imsRegState = intent.getIntExtra("android:regState", 1);
        ((MobileState) this.mCurrentState).imsCap = getImsEnableCap(intent);
        ((MobileState) this.mCurrentState).volteIcon = getVolteIcon();
        Log.d(this.mTag, "handleImsAction imsRegstate=" + ((MobileState) this.mCurrentState).imsRegState + ",imsCap = " + ((MobileState) this.mCurrentState).imsCap + ",volteIconId=" + ((MobileState) this.mCurrentState).volteIcon);
    }

    private int getVolteIcon() {
        if (isImsOverWfc()) {
            boolean isNonSsProject = SystemProperties.get("persist.radio.multisim.config", "ss").equals("ss") ? false : true;
            if (!isNonSsProject) {
                return 0;
            }
            return R.drawable.stat_sys_wfc;
        }
        if (!isImsOverVoice() || !isLteNetWork() || ((MobileState) this.mCurrentState).imsRegState != 0) {
            return 0;
        }
        return R.drawable.stat_sys_volte;
    }

    private int getImsEnableCap(Intent intent) {
        boolean[] enabledFeatures = intent.getBooleanArrayExtra("android:enablecap");
        if (enabledFeatures == null) {
            return -1;
        }
        if (enabledFeatures[2]) {
            return 2;
        }
        if (!enabledFeatures[0]) {
            return -1;
        }
        return 0;
    }

    public boolean isImsOverWfc() {
        return ((MobileState) this.mCurrentState).imsCap == 2;
    }

    private boolean isImsOverVoice() {
        return ((MobileState) this.mCurrentState).imsCap == 0;
    }

    public boolean isWfcEnable() {
        boolean isWfcEnabled = TelephonyManagerEx.getDefault().isWifiCallingEnabled(this.mSubscriptionInfo.getSubscriptionId());
        return isWfcEnabled;
    }

    private void updateDataSim() {
        int defaultDataSub = this.mDefaults.getDefaultDataSubId();
        if (SubscriptionManager.isValidSubscriptionId(defaultDataSub)) {
            ((MobileState) this.mCurrentState).dataSim = defaultDataSub == this.mSubscriptionInfo.getSubscriptionId();
        } else {
            ((MobileState) this.mCurrentState).dataSim = true;
        }
    }

    void updateNetworkName(boolean showSpn, String spn, String dataSpn, boolean showPlmn, String plmn) {
        Log.d("CarrierLabel", "updateNetworkName showSpn=" + showSpn + " spn=" + spn + " dataSpn=" + dataSpn + " showPlmn=" + showPlmn + " plmn=" + plmn);
        StringBuilder str = new StringBuilder();
        StringBuilder strData = new StringBuilder();
        if (showPlmn && plmn != null) {
            str.append(plmn);
            strData.append(plmn);
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
        if (showSpn && dataSpn != null) {
            if (strData.length() != 0) {
                strData.append(this.mNetworkNameSeparator);
            }
            strData.append(dataSpn);
        }
        if (strData.length() == 0 && showSpn && spn != null) {
            Log.d("CarrierLabel", "show spn instead 'no service' here: " + spn);
            strData.append(spn);
        }
        if (strData.length() != 0) {
            ((MobileState) this.mCurrentState).networkNameData = strData.toString();
        } else {
            ((MobileState) this.mCurrentState).networkNameData = this.mNetworkNameDefault;
        }
    }

    public final void updateTelephony() {
        boolean z = false;
        Log.d(this.mTag, "updateTelephonySignalStrength: hasService=" + hasService() + " ss=" + this.mSignalStrength);
        ((MobileState) this.mCurrentState).connected = hasService() && this.mSignalStrength != null;
        handleIWLANNetwork();
        if (((MobileState) this.mCurrentState).connected) {
            if (!this.mSignalStrength.isGsm() && this.mConfig.alwaysShowCdmaRssi) {
                ((MobileState) this.mCurrentState).level = this.mSignalStrength.getCdmaLevel();
            } else {
                ((MobileState) this.mCurrentState).level = this.mSignalStrength.getLevel();
            }
            ((MobileState) this.mCurrentState).level = this.mStatusBarExt.getCustomizeSignalStrengthLevel(((MobileState) this.mCurrentState).level, this.mSignalStrength, this.mServiceState);
        }
        if (this.mNetworkToIconLookup.indexOfKey(this.mDataNetType) >= 0) {
            ((MobileState) this.mCurrentState).iconGroup = this.mNetworkToIconLookup.get(this.mDataNetType);
        } else {
            ((MobileState) this.mCurrentState).iconGroup = this.mDefaultIcons;
        }
        ((MobileState) this.mCurrentState).dataNetType = this.mDataNetType;
        MobileState mobileState = (MobileState) this.mCurrentState;
        if (((MobileState) this.mCurrentState).connected && this.mDataState == 2) {
            z = true;
        }
        mobileState.dataConnected = z;
        ((MobileState) this.mCurrentState).customizedState = this.mStatusBarExt.getCustomizeCsState(this.mServiceState, ((MobileState) this.mCurrentState).customizedState);
        ((MobileState) this.mCurrentState).customizedSignalStrengthIcon = this.mStatusBarExt.getCustomizeSignalStrengthIcon(this.mSubscriptionInfo.getSubscriptionId(), ((MobileState) this.mCurrentState).customizedSignalStrengthIcon, this.mSignalStrength, this.mDataNetType, this.mServiceState);
        if (isCarrierNetworkChangeActive()) {
            ((MobileState) this.mCurrentState).iconGroup = TelephonyIcons.CARRIER_NETWORK_CHANGE;
        } else if (isRoaming()) {
            ((MobileState) this.mCurrentState).iconGroup = TelephonyIcons.ROAMING;
        } else if (isDataDisabled()) {
            ((MobileState) this.mCurrentState).iconGroup = TelephonyIcons.DATA_DISABLED;
        }
        if (isEmergencyOnly() != ((MobileState) this.mCurrentState).isEmergency) {
            ((MobileState) this.mCurrentState).isEmergency = isEmergencyOnly();
            this.mNetworkController.recalculateEmergency();
        }
        if (((MobileState) this.mCurrentState).networkName == this.mNetworkNameDefault && this.mServiceState != null && !TextUtils.isEmpty(this.mServiceState.getOperatorAlphaShort())) {
            ((MobileState) this.mCurrentState).networkName = this.mServiceState.getOperatorAlphaShort();
        }
        ((MobileState) this.mCurrentState).networkIcon = NetworkTypeUtils.getNetworkTypeIcon(this.mServiceState, this.mConfig, hasService());
        ((MobileState) this.mCurrentState).volteIcon = getVolteIcon();
        notifyListenersIfNecessary();
    }

    private boolean isDataDisabled() {
        return !this.mPhone.getDataEnabled(this.mSubscriptionInfo.getSubscriptionId());
    }

    private void handleIWLANNetwork() {
        if (((MobileState) this.mCurrentState).connected && this.mServiceState != null && this.mServiceState.getDataNetworkType() == 18 && this.mServiceState.getVoiceNetworkType() == 0) {
            Log.d(this.mTag, "Current is IWLAN network only, no cellular network available");
            ((MobileState) this.mCurrentState).connected = false;
        }
        ((MobileState) this.mCurrentState).connected = this.mStatusBarExt.updateSignalStrengthWifiOnlyMode(this.mServiceState, ((MobileState) this.mCurrentState).connected);
    }

    void setActivity(int activity) {
        boolean z = true;
        MobileState mobileState = (MobileState) this.mCurrentState;
        boolean z2 = activity == 3 || activity == 1;
        mobileState.activityIn = z2;
        MobileState mobileState2 = (MobileState) this.mCurrentState;
        if (activity != 3 && activity != 2) {
            z = false;
        }
        mobileState2.activityOut = z;
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
        public MobilePhoneStateListener(int subId, Looper looper) {
            super(subId, looper);
        }

        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            Log.d(MobileSignalController.this.mTag, "onSignalStrengthsChanged signalStrength=" + signalStrength + (signalStrength == null ? "" : " level=" + signalStrength.getLevel()));
            MobileSignalController.this.mSignalStrength = signalStrength;
            MobileSignalController.this.updateTelephony();
        }

        @Override
        public void onServiceStateChanged(ServiceState state) {
            Log.d(MobileSignalController.this.mTag, "onServiceStateChanged voiceState=" + state.getVoiceRegState() + " dataState=" + state.getDataRegState());
            MobileSignalController.this.mServiceState = state;
            MobileSignalController.this.mDataNetType = state.getDataNetworkType();
            MobileSignalController.this.mDataNetType = NetworkTypeUtils.getDataNetTypeFromServiceState(MobileSignalController.this.mDataNetType, MobileSignalController.this.mServiceState);
            MobileSignalController.this.updateTelephony();
        }

        @Override
        public void onDataConnectionStateChanged(int state, int networkType) {
            Log.d(MobileSignalController.this.mTag, "onDataConnectionStateChanged: state=" + state + " type=" + networkType);
            MobileSignalController.this.mDataState = state;
            MobileSignalController.this.mDataNetType = networkType;
            MobileSignalController.this.mDataNetType = NetworkTypeUtils.getDataNetTypeFromServiceState(MobileSignalController.this.mDataNetType, MobileSignalController.this.mServiceState);
            MobileSignalController.this.updateTelephony();
        }

        @Override
        public void onDataActivity(int direction) {
            Log.d(MobileSignalController.this.mTag, "onDataActivity: direction=" + direction);
            MobileSignalController.this.setActivity(direction);
        }

        public void onCarrierNetworkChange(boolean active) {
            Log.d(MobileSignalController.this.mTag, "onCarrierNetworkChange: active=" + active);
            ((MobileState) MobileSignalController.this.mCurrentState).carrierNetworkChangeMode = active;
            MobileSignalController.this.updateTelephony();
        }
    }

    static class MobileIconGroup extends SignalController.IconGroup {
        final int mDataContentDescription;
        final int mDataType;
        final boolean mIsWide;
        final int mQsDataType;

        public MobileIconGroup(String name, int[][] sbIcons, int[][] qsIcons, int[] contentDesc, int sbNullState, int qsNullState, int sbDiscState, int qsDiscState, int discContentDesc, int dataContentDesc, int dataType, boolean isWide, int qsDataType) {
            super(name, sbIcons, qsIcons, contentDesc, sbNullState, qsNullState, sbDiscState, qsDiscState, discContentDesc);
            this.mDataContentDescription = dataContentDesc;
            this.mDataType = dataType;
            this.mIsWide = isWide;
            this.mQsDataType = qsDataType;
        }
    }

    static class MobileState extends SignalController.State {
        boolean airplaneMode;
        boolean carrierNetworkChangeMode;
        int customizedSignalStrengthIcon;
        int customizedState;
        boolean dataConnected;
        int dataNetType;
        boolean dataSim;
        int imsCap;
        int imsRegState = 3;
        boolean isDefault;
        boolean isEmergency;
        int networkIcon;
        String networkName;
        String networkNameData;
        boolean userSetup;
        int volteIcon;

        MobileState() {
        }

        @Override
        public void copyFrom(SignalController.State s) {
            super.copyFrom(s);
            MobileState state = (MobileState) s;
            this.dataSim = state.dataSim;
            this.networkName = state.networkName;
            this.networkNameData = state.networkNameData;
            this.dataConnected = state.dataConnected;
            this.isDefault = state.isDefault;
            this.isEmergency = state.isEmergency;
            this.airplaneMode = state.airplaneMode;
            this.carrierNetworkChangeMode = state.carrierNetworkChangeMode;
            this.userSetup = state.userSetup;
            this.networkIcon = state.networkIcon;
            this.dataNetType = state.dataNetType;
            this.customizedState = state.customizedState;
            this.customizedSignalStrengthIcon = state.customizedSignalStrengthIcon;
            this.imsRegState = state.imsRegState;
            this.imsCap = state.imsCap;
            this.volteIcon = state.volteIcon;
        }

        @Override
        protected void toString(StringBuilder builder) {
            super.toString(builder);
            builder.append(',');
            builder.append("dataSim=").append(this.dataSim).append(',');
            builder.append("networkName=").append(this.networkName).append(',');
            builder.append("networkNameData=").append(this.networkNameData).append(',');
            builder.append("dataConnected=").append(this.dataConnected).append(',');
            builder.append("isDefault=").append(this.isDefault).append(',');
            builder.append("isEmergency=").append(this.isEmergency).append(',');
            builder.append("airplaneMode=").append(this.airplaneMode).append(',');
            builder.append("carrierNetworkChangeMode=").append(this.carrierNetworkChangeMode).append(',');
            builder.append("userSetup=").append(this.userSetup);
            builder.append("networkIcon").append(this.networkIcon).append(',');
            builder.append("dataNetType").append(this.dataNetType).append(',');
            builder.append("customizedState").append(this.customizedState).append(',');
            builder.append("customizedSignalStrengthIcon").append(this.customizedSignalStrengthIcon).append(',');
            builder.append("imsRegState=").append(this.imsRegState).append(',');
            builder.append("imsCap=").append(this.imsCap).append(',');
            builder.append("volteIconId=").append(this.volteIcon).append(',');
            builder.append("carrierNetworkChangeMode=").append(this.carrierNetworkChangeMode);
        }

        @Override
        public boolean equals(Object o) {
            if (super.equals(o) && Objects.equals(((MobileState) o).networkName, this.networkName) && Objects.equals(((MobileState) o).networkNameData, this.networkNameData) && ((MobileState) o).dataSim == this.dataSim && ((MobileState) o).dataConnected == this.dataConnected && ((MobileState) o).isEmergency == this.isEmergency && ((MobileState) o).airplaneMode == this.airplaneMode && ((MobileState) o).carrierNetworkChangeMode == this.carrierNetworkChangeMode && ((MobileState) o).networkIcon == this.networkIcon && ((MobileState) o).volteIcon == this.volteIcon && ((MobileState) o).dataNetType == this.dataNetType && ((MobileState) o).customizedState == this.customizedState && ((MobileState) o).customizedSignalStrengthIcon == this.customizedSignalStrengthIcon && ((MobileState) o).userSetup == this.userSetup) {
                return ((MobileState) o).isDefault == this.isDefault;
            }
            return false;
        }
    }

    public SubscriptionInfo getControllerSubInfo() {
        return this.mSubscriptionInfo;
    }

    public boolean getControllserHasService() {
        return hasService();
    }
}
