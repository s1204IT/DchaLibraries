package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneStateListener;
import android.telephony.PreciseDataConnectionState;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.feature.ImsFeature;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settingslib.graph.SignalDrawable;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NetworkControllerImpl;
import com.android.systemui.statusbar.policy.SignalController;
import com.mediatek.ims.MtkImsConnectionStateListener;
import com.mediatek.systemui.ext.IMobileIconExt;
import com.mediatek.systemui.ext.ISystemUIStatusBarExt;
import com.mediatek.systemui.ext.OpSystemUICustomizationFactoryBase;
import com.mediatek.systemui.statusbar.networktype.NetworkTypeUtils;
import com.mediatek.systemui.statusbar.util.FeatureOptions;
import com.mediatek.systemui.statusbar.util.SIMHelper;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Objects;

public class MobileSignalController extends SignalController<MobileState, MobileIconGroup> {
    private CarrierConfigManager mCarrierConfigManager;
    private NetworkControllerImpl.Config mConfig;
    private int mDataNetType;
    private int mDataState;
    private MobileIconGroup mDefaultIcons;
    private final NetworkControllerImpl.SubscriptionDefaults mDefaults;
    private ImsFeature.Capabilities mImsCapConfig;

    @VisibleForTesting
    MtkImsConnectionStateListener mImsConnectionStateListener;
    private ImsManager mImsManager;
    private int mImsRadioTech;
    private IMobileIconExt mMobileIconExt;
    private final String mNetworkNameDefault;
    private final String mNetworkNameSeparator;
    final SparseArray<MobileIconGroup> mNetworkToIconLookup;
    private final ContentObserver mObserver;
    private final TelephonyManager mPhone;

    @VisibleForTesting
    final PhoneStateListener mPhoneStateListener;
    private Handler mReceiverHandler;
    private ServiceState mServiceState;
    private SignalStrength mSignalStrength;
    private ISystemUIStatusBarExt mStatusBarExt;
    SubscriptionInfo mSubscriptionInfo;

    public MobileSignalController(Context context, NetworkControllerImpl.Config config, boolean z, TelephonyManager telephonyManager, CallbackHandler callbackHandler, NetworkControllerImpl networkControllerImpl, SubscriptionInfo subscriptionInfo, NetworkControllerImpl.SubscriptionDefaults subscriptionDefaults, Looper looper) {
        super("MobileSignalController(" + subscriptionInfo.getSubscriptionId() + ")(" + subscriptionInfo.getSimSlotIndex() + ")", context, 0, callbackHandler, networkControllerImpl);
        this.mDataNetType = 0;
        this.mDataState = 0;
        this.mImsConnectionStateListener = new MtkImsConnectionStateListener() {
            public void onImsConnected(final int i) {
                MobileSignalController.this.mReceiverHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (SignalController.DEBUG) {
                            Log.d(MobileSignalController.this.mTag, "onImsConnected STATE_IN_SERVICE, imsRadioTech = " + i);
                        }
                        MobileSignalController.this.mImsRadioTech = i;
                        ((MobileState) MobileSignalController.this.mCurrentState).imsRegState = 0;
                        MobileSignalController.this.updateIms();
                    }
                });
            }

            public void onImsDisconnected(ImsReasonInfo imsReasonInfo) {
                MobileSignalController.this.mReceiverHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (SignalController.DEBUG) {
                            Log.d(MobileSignalController.this.mTag, "onImsDisconnected STATE_OUT_OF_SERVICE");
                        }
                        ((MobileState) MobileSignalController.this.mCurrentState).imsRegState = 1;
                        ((MobileState) MobileSignalController.this.mCurrentState).imsCap = -1;
                        MobileSignalController.this.updateTelephony();
                    }
                });
            }

            public void onCapabilitiesStatusChanged(final ImsFeature.Capabilities capabilities) {
                MobileSignalController.this.mReceiverHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (SignalController.DEBUG) {
                            Log.d(MobileSignalController.this.mTag, "onCapabilitiesStatusChanged:" + capabilities);
                        }
                        MobileSignalController.this.mImsCapConfig = capabilities;
                        MobileSignalController.this.updateIms();
                    }
                });
            }

            public void onWifiPdnOOSStateChanged(final int i) {
                MobileSignalController.this.mReceiverHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (SignalController.DEBUG) {
                            Log.d(MobileSignalController.this.mTag, "onWifiPdnOOSStateChanged oosState:" + i);
                        }
                        if (i == 0 || i == 1) {
                            ((MobileState) MobileSignalController.this.mCurrentState).imsRegState = 1;
                        } else if (i == 2) {
                            ((MobileState) MobileSignalController.this.mCurrentState).imsRegState = 0;
                        }
                        MobileSignalController.this.updateTelephony();
                    }
                });
            }
        };
        this.mNetworkToIconLookup = new SparseArray<>();
        this.mConfig = config;
        this.mPhone = telephonyManager;
        this.mDefaults = subscriptionDefaults;
        this.mSubscriptionInfo = subscriptionInfo;
        this.mMobileIconExt = OpSystemUICustomizationFactoryBase.getOpFactory(context).makeMobileIcon();
        this.mStatusBarExt = OpSystemUICustomizationFactoryBase.getOpFactory(context).makeSystemUIStatusBar(context);
        this.mPhoneStateListener = new MobilePhoneStateListener(subscriptionInfo.getSubscriptionId(), looper);
        this.mReceiverHandler = new Handler(looper);
        this.mNetworkNameSeparator = getStringIfExists(R.string.status_bar_network_name_separator);
        this.mNetworkNameDefault = getStringIfExists(android.R.string.contentServiceTooManyDeletesNotificationDesc);
        mapIconSets();
        String string = subscriptionInfo.getCarrierName() != null ? subscriptionInfo.getCarrierName().toString() : this.mNetworkNameDefault;
        MobileState mobileState = (MobileState) this.mLastState;
        ((MobileState) this.mCurrentState).networkName = string;
        mobileState.networkName = string;
        MobileState mobileState2 = (MobileState) this.mLastState;
        ((MobileState) this.mCurrentState).networkNameData = string;
        mobileState2.networkNameData = string;
        MobileState mobileState3 = (MobileState) this.mLastState;
        ((MobileState) this.mCurrentState).enabled = z;
        mobileState3.enabled = z;
        MobileState mobileState4 = (MobileState) this.mLastState;
        MobileState mobileState5 = (MobileState) this.mCurrentState;
        MobileIconGroup mobileIconGroup = this.mDefaultIcons;
        mobileState5.iconGroup = mobileIconGroup;
        mobileState4.iconGroup = mobileIconGroup;
        this.mCarrierConfigManager = (CarrierConfigManager) context.getSystemService("carrier_config");
        updateDataSim();
        this.mObserver = new ContentObserver(new Handler(looper)) {
            @Override
            public void onChange(boolean z2) {
                MobileSignalController.this.updateTelephony();
            }
        };
    }

    private void updateIms() {
        onFeatureCapabilityChangedAdapter(this.mImsRadioTech, this.mImsCapConfig);
        updateTelephony();
    }

    private void onFeatureCapabilityChangedAdapter(int i, ImsFeature.Capabilities capabilities) {
        if (capabilities == null) {
            Log.w(this.mTag, "onFeatureCapabilityChangedAdapter, capabilities is null, return");
            return;
        }
        int[] iArr = new int[6];
        Arrays.fill(iArr, -1);
        int[] iArr2 = new int[6];
        Arrays.fill(iArr2, -1);
        switch (i) {
            case 0:
                if (capabilities.isCapable(1)) {
                    iArr[0] = 0;
                }
                if (capabilities.isCapable(2)) {
                    iArr[1] = 1;
                }
                if (capabilities.isCapable(4)) {
                    iArr[4] = 4;
                }
                break;
            case 1:
                if (capabilities.isCapable(1)) {
                    iArr[2] = 2;
                }
                if (capabilities.isCapable(2)) {
                    iArr[3] = 3;
                }
                if (capabilities.isCapable(4)) {
                    iArr[5] = 5;
                }
                break;
        }
        for (int i2 = 0; i2 < iArr.length; i2++) {
            if (iArr[i2] != i2) {
                iArr2[i2] = i2;
            }
        }
        ((MobileState) this.mCurrentState).imsCap = getImsEnableCap(iArr);
    }

    private int getImsEnableCap(int[] iArr) {
        if (iArr != null) {
            if (iArr[2] == 2) {
                return 2;
            }
            if (iArr[0] == 0) {
                return 0;
            }
        }
        return -1;
    }

    private int getVolteIcon() {
        int i = 0;
        if (isImsOverWfc()) {
            if (this.mStatusBarExt.needShowWfcIcon() && ((MobileState) this.mCurrentState).imsRegState == 0) {
                i = R.drawable.stat_sys_wfc;
            }
        } else if (isImsOverVoice() && isLteNetWork()) {
            if (((MobileState) this.mCurrentState).imsRegState == 0) {
                i = R.drawable.stat_sys_volte;
            } else if (FeatureOptions.MTK_CT_MIXED_VOLTE_SUPPORT && SIMHelper.isSecondaryCSIMForMixedVolte(this.mSubscriptionInfo.getSubscriptionId()) && ((MobileState) this.mCurrentState).imsRegState == 1) {
                if (DEBUG) {
                    Log.d(this.mTag, "set dis volte icon");
                }
                i = R.drawable.stat_sys_volte_dis;
            }
        }
        this.mStatusBarExt.setImsRegInfo(this.mSubscriptionInfo.getSubscriptionId(), ((MobileState) this.mCurrentState).imsRegState, isImsOverWfc(), isImsOverVoice());
        return i;
    }

    public boolean isImsOverWfc() {
        return ((MobileState) this.mCurrentState).imsCap == 2;
    }

    private boolean isImsOverVoice() {
        return ((MobileState) this.mCurrentState).imsCap == 0;
    }

    public boolean isLteNetWork() {
        return this.mDataNetType == 13 || this.mDataNetType == 19;
    }

    public void setConfiguration(NetworkControllerImpl.Config config) {
        this.mConfig = config;
        mapIconSets();
        updateTelephony();
    }

    public void setAirplaneMode(boolean z) {
        ((MobileState) this.mCurrentState).airplaneMode = z;
        notifyListenersIfNecessary();
    }

    public void setUserSetupComplete(boolean z) {
        ((MobileState) this.mCurrentState).userSetup = z;
        notifyListenersIfNecessary();
    }

    @Override
    public void updateConnectivity(BitSet bitSet, BitSet bitSet2) {
        boolean z = bitSet2.get(this.mTransportType);
        ((MobileState) this.mCurrentState).isDefault = bitSet.get(this.mTransportType) && this.mNetworkController.isCellularConnected(this.mSubscriptionInfo.getSubscriptionId());
        ((MobileState) this.mCurrentState).inetCondition = (z || !((MobileState) this.mCurrentState).isDefault) ? 1 : 0;
        notifyListenersIfNecessary();
    }

    public void setCarrierNetworkChangeMode(boolean z) {
        ((MobileState) this.mCurrentState).carrierNetworkChangeMode = z;
        updateTelephony();
    }

    public void registerListener() {
        Log.d(this.mTag, "registerListener");
        registerImsListener();
        this.mPhone.listen(this.mPhoneStateListener, 70113);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("mobile_data"), true, this.mObserver);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("mobile_data" + this.mSubscriptionInfo.getSubscriptionId()), true, this.mObserver);
        this.mStatusBarExt.registerOpStateListener();
    }

    public void unregisterListener() {
        Log.d(this.mTag, "unregisterListener");
        this.mPhone.listen(this.mPhoneStateListener, 0);
        this.mContext.getContentResolver().unregisterContentObserver(this.mObserver);
        unregisterImsListener();
    }

    private void mapIconSets() {
        this.mNetworkToIconLookup.clear();
        this.mNetworkToIconLookup.put(5, TelephonyIcons.THREE_G);
        this.mNetworkToIconLookup.put(6, TelephonyIcons.THREE_G);
        this.mNetworkToIconLookup.put(12, TelephonyIcons.THREE_G);
        this.mNetworkToIconLookup.put(14, TelephonyIcons.THREE_G);
        this.mNetworkToIconLookup.put(3, TelephonyIcons.THREE_G);
        this.mNetworkToIconLookup.put(17, TelephonyIcons.THREE_G);
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
        MobileIconGroup mobileIconGroup = TelephonyIcons.THREE_G;
        MobileIconGroup mobileIconGroup2 = TelephonyIcons.THREE_G;
        if (this.mConfig.hspaDataDistinguishable) {
            mobileIconGroup = TelephonyIcons.H;
            mobileIconGroup2 = TelephonyIcons.H_PLUS;
        }
        this.mNetworkToIconLookup.put(8, mobileIconGroup);
        this.mNetworkToIconLookup.put(9, mobileIconGroup);
        this.mNetworkToIconLookup.put(10, mobileIconGroup);
        this.mNetworkToIconLookup.put(15, mobileIconGroup2);
        if (this.mConfig.show4gForLte) {
            this.mNetworkToIconLookup.put(13, TelephonyIcons.FOUR_G);
            if (this.mConfig.hideLtePlus) {
                this.mNetworkToIconLookup.put(19, TelephonyIcons.FOUR_G);
            } else {
                this.mNetworkToIconLookup.put(19, TelephonyIcons.FOUR_G_PLUS);
            }
        } else {
            this.mNetworkToIconLookup.put(13, TelephonyIcons.LTE);
            if (this.mConfig.hideLtePlus) {
                this.mNetworkToIconLookup.put(19, TelephonyIcons.LTE);
            } else {
                this.mNetworkToIconLookup.put(19, TelephonyIcons.LTE_PLUS);
            }
        }
        this.mNetworkToIconLookup.put(18, TelephonyIcons.WFC);
    }

    private int getNumLevels() {
        if (this.mConfig.inflateSignalStrengths) {
            return 6;
        }
        return 5;
    }

    @Override
    public int getCurrentIconId() {
        if (((MobileState) this.mCurrentState).iconGroup == TelephonyIcons.CARRIER_NETWORK_CHANGE) {
            return SignalDrawable.getCarrierChangeState(getNumLevels());
        }
        if (((MobileState) this.mCurrentState).connected) {
            int i = ((MobileState) this.mCurrentState).level;
            if (this.mConfig.inflateSignalStrengths) {
                i++;
            }
            return SignalDrawable.getState(i, getNumLevels(), (((MobileState) this.mCurrentState).userSetup && ((MobileState) this.mCurrentState).iconGroup == TelephonyIcons.DATA_DISABLED) || (((MobileState) this.mCurrentState).inetCondition == 0));
        }
        if (((MobileState) this.mCurrentState).enabled) {
            return SignalDrawable.getEmptyState(getNumLevels());
        }
        return 0;
    }

    @Override
    public int getQsCurrentIconId() {
        if (((MobileState) this.mCurrentState).airplaneMode) {
            return SignalDrawable.getAirplaneModeState(getNumLevels());
        }
        return getCurrentIconId();
    }

    @Override
    public void notifyListeners(NetworkController.SignalCallback signalCallback) {
        int i;
        String str;
        int i2;
        int i3;
        int i4;
        MobileIconGroup icons = getIcons();
        String stringIfExists = getStringIfExists(getContentDescription());
        String stringIfExists2 = getStringIfExists(icons.mDataContentDescription);
        if (((MobileState) this.mCurrentState).inetCondition == 0) {
            stringIfExists2 = this.mContext.getString(R.string.data_connection_no_internet);
        }
        String str2 = stringIfExists2;
        boolean z = ((MobileState) this.mCurrentState).iconGroup == TelephonyIcons.DATA_DISABLED && ((MobileState) this.mCurrentState).userSetup;
        int customizeSignalStrengthIcon = this.mStatusBarExt.getCustomizeSignalStrengthIcon(this.mSubscriptionInfo.getSubscriptionId(), getCurrentIconId(), this.mSignalStrength, this.mDataNetType, this.mServiceState);
        boolean z2 = ((MobileState) this.mCurrentState).dataConnected || z;
        NetworkController.IconState iconState = new NetworkController.IconState(((MobileState) this.mCurrentState).enabled && !((MobileState) this.mCurrentState).airplaneMode, customizeSignalStrengthIcon, stringIfExists);
        if (((MobileState) this.mCurrentState).dataSim) {
            if (z2 || this.mConfig.alwaysShowDataRatIcon) {
                i4 = icons.mQsDataType;
            } else {
                i4 = 0;
            }
            Object iconState2 = new NetworkController.IconState(((MobileState) this.mCurrentState).enabled && !((MobileState) this.mCurrentState).isEmergency, getQsCurrentIconId(), stringIfExists);
            str = ((MobileState) this.mCurrentState).isEmergency ? null : ((MobileState) this.mCurrentState).networkName;
            obj = iconState2;
            i = i4;
        } else {
            i = 0;
            str = 0;
        }
        boolean z3 = ((MobileState) this.mCurrentState).dataConnected && !((MobileState) this.mCurrentState).carrierNetworkChangeMode && ((MobileState) this.mCurrentState).activityIn;
        boolean z4 = ((MobileState) this.mCurrentState).dataConnected && !((MobileState) this.mCurrentState).carrierNetworkChangeMode && ((MobileState) this.mCurrentState).activityOut;
        boolean z5 = (((MobileState) this.mCurrentState).isDefault || z) & z2;
        if (z5 || this.mConfig.alwaysShowDataRatIcon) {
            i2 = icons.mDataType;
        } else {
            i2 = 0;
        }
        if (((MobileState) this.mCurrentState).lwaRegState == 1 && z5) {
            i2 = R.drawable.stat_sys_data_fully_connected_4gaw;
        }
        int i5 = i2;
        int i6 = ((MobileState) this.mCurrentState).networkIcon;
        if (!((MobileState) this.mCurrentState).airplaneMode || isImsOverWfc()) {
            i3 = ((MobileState) this.mCurrentState).volteIcon;
        } else {
            i3 = 0;
        }
        this.mStatusBarExt.isDataDisabled(this.mSubscriptionInfo.getSubscriptionId(), z);
        signalCallback.setMobileDataIndicators(iconState, obj, this.mStatusBarExt.getDataTypeIcon(this.mSubscriptionInfo.getSubscriptionId(), i5, this.mDataNetType, ((MobileState) this.mCurrentState).dataConnected ? 2 : 0, this.mServiceState), this.mStatusBarExt.getNetworkTypeIcon(this.mSubscriptionInfo.getSubscriptionId(), i6, this.mDataNetType, this.mServiceState), i3, i, z3, z4, str2, str, icons.mIsWide, this.mSubscriptionInfo.getSubscriptionId(), ((MobileState) this.mCurrentState).roaming, ((MobileState) this.mCurrentState).isDefaultData);
        this.mNetworkController.refreshPlmnCarrierLabel();
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
        PersistableBundle configForSubId;
        boolean z = false;
        if (isCarrierNetworkChangeActive()) {
            return false;
        }
        if (isCdma() && this.mServiceState != null) {
            int cdmaEriIconMode = this.mServiceState.getCdmaEriIconMode();
            if (this.mServiceState.getCdmaEriIconIndex() != 1) {
                return cdmaEriIconMode == 0 || cdmaEriIconMode == 1;
            }
            return false;
        }
        if (this.mServiceState != null && this.mServiceState.getRoaming()) {
            z = true;
        }
        if (z && (configForSubId = this.mCarrierConfigManager.getConfigForSubId(this.mSubscriptionInfo.getSubscriptionId())) != null) {
            z = configForSubId.getBoolean("mtk_key_carrier_need_show_roaming_icon");
            if (DEBUG) {
                Log.d(this.mTag, "get roaming state from MtkCarrierConfigManager: " + z);
            }
        }
        return z;
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
        } else if (action.equals("com.mediatek.server.lwa.LWA_STATE_CHANGE_ACTION")) {
            handleLwaAction(intent);
            notifyListenersIfNecessary();
        }
    }

    private void handleLwaAction(Intent intent) {
        ((MobileState) this.mCurrentState).lwaRegState = intent.getIntExtra("com.mediatek.server.lwa.EXTRA_STATE", -1);
    }

    private void updateDataSim() {
        int defaultDataSubId = this.mDefaults.getDefaultDataSubId();
        if (SubscriptionManager.isValidSubscriptionId(defaultDataSubId)) {
            ((MobileState) this.mCurrentState).dataSim = defaultDataSubId == this.mSubscriptionInfo.getSubscriptionId();
        } else {
            ((MobileState) this.mCurrentState).dataSim = true;
        }
        if (this.mStatusBarExt.disableHostFunction()) {
            ((MobileState) this.mCurrentState).isDefaultData = defaultDataSubId == this.mSubscriptionInfo.getSubscriptionId();
        }
    }

    void updateNetworkName(boolean z, String str, String str2, boolean z2, String str3) {
        if (CHATTY) {
            Log.d("CarrierLabel", "updateNetworkName showSpn=" + z + " spn=" + str + " dataSpn=" + str2 + " showPlmn=" + z2 + " plmn=" + str3);
        }
        StringBuilder sb = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        if (z2 && str3 != null) {
            sb.append(str3);
            sb2.append(str3);
        }
        if (z && str != null) {
            if (sb.length() != 0) {
                sb.append(this.mNetworkNameSeparator);
            }
            sb.append(str);
        }
        if (sb.length() != 0) {
            ((MobileState) this.mCurrentState).networkName = sb.toString();
        } else {
            ((MobileState) this.mCurrentState).networkName = this.mNetworkNameDefault;
        }
        if (z && str2 != null) {
            if (sb2.length() != 0) {
                sb2.append(this.mNetworkNameSeparator);
            }
            sb2.append(str2);
        }
        if (sb2.length() == 0 && z && str != null) {
            Log.d("CarrierLabel", "show spn instead 'no service' here: " + str);
            sb2.append(str);
        }
        if (sb2.length() != 0) {
            ((MobileState) this.mCurrentState).networkNameData = sb2.toString();
        } else {
            ((MobileState) this.mCurrentState).networkNameData = this.mNetworkNameDefault;
        }
    }

    private final void updateTelephony() {
        if (DEBUG && FeatureOptions.LOG_ENABLE) {
            Log.d(this.mTag, "updateTelephonySignalStrength: hasService=" + hasService() + ", ss=" + this.mSignalStrength + ", mConfig.alwaysShowDataRatIcon = " + this.mConfig.alwaysShowDataRatIcon + ", mDataState = " + this.mDataState);
        }
        boolean z = false;
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
        ((MobileState) this.mCurrentState).isInCsCall = this.mStatusBarExt.isInCsCall();
        ((MobileState) this.mCurrentState).customizedSignalStrengthIcon = this.mStatusBarExt.getCustomizeSignalStrengthIcon(this.mSubscriptionInfo.getSubscriptionId(), ((MobileState) this.mCurrentState).customizedSignalStrengthIcon, this.mSignalStrength, this.mDataNetType, this.mServiceState);
        ((MobileState) this.mCurrentState).roaming = isRoaming();
        if (isCarrierNetworkChangeActive()) {
            ((MobileState) this.mCurrentState).iconGroup = TelephonyIcons.CARRIER_NETWORK_CHANGE;
        } else if (isDataDisabled() && !this.mConfig.alwaysShowDataRatIcon) {
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
        boolean z = !this.mPhone.getDataEnabled(this.mSubscriptionInfo.getSubscriptionId());
        if (DEBUG) {
            Log.d(this.mTag, "isDataDisabled = " + z);
        }
        return z;
    }

    private void handleIWLANNetwork() {
        if (((MobileState) this.mCurrentState).connected && this.mServiceState != null && this.mServiceState.getDataNetworkType() == 18 && this.mServiceState.getCellularRegState() != 0) {
            Log.d(this.mTag, "Current is IWLAN network only, no cellular network available");
            ((MobileState) this.mCurrentState).connected = false;
        }
    }

    @VisibleForTesting
    void setActivity(int i) {
        ((MobileState) this.mCurrentState).activityIn = i == 3 || i == 1;
        ((MobileState) this.mCurrentState).activityOut = i == 3 || i == 2;
        notifyListenersIfNecessary();
    }

    @Override
    public void dump(PrintWriter printWriter) {
        super.dump(printWriter);
        printWriter.println("  mSubscription=" + this.mSubscriptionInfo + ",");
        printWriter.println("  mServiceState=" + this.mServiceState + ",");
        printWriter.println("  mSignalStrength=" + this.mSignalStrength + ",");
        printWriter.println("  mDataState=" + this.mDataState + ",");
        printWriter.println("  mDataNetType=" + this.mDataNetType + ",");
    }

    class MobilePhoneStateListener extends PhoneStateListener {
        public MobilePhoneStateListener(int i, Looper looper) {
            super(Integer.valueOf(i), looper);
        }

        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            String str;
            if (SignalController.DEBUG) {
                String str2 = MobileSignalController.this.mTag;
                StringBuilder sb = new StringBuilder();
                sb.append("onSignalStrengthsChanged signalStrength=");
                sb.append(signalStrength);
                if (signalStrength == null) {
                    str = "";
                } else {
                    str = " level=" + signalStrength.getLevel();
                }
                sb.append(str);
                Log.d(str2, sb.toString());
            }
            MobileSignalController.this.mSignalStrength = signalStrength;
            MobileSignalController.this.updateTelephony();
        }

        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            if (SignalController.DEBUG) {
                Log.d(MobileSignalController.this.mTag, "onServiceStateChanged: " + MobileSignalController.this.dumpServiceState(serviceState));
            }
            MobileSignalController.this.mServiceState = serviceState;
            if (serviceState != null) {
                MobileSignalController.this.mDataNetType = serviceState.getDataNetworkType();
                if (MobileSignalController.this.mDataNetType == 13 && MobileSignalController.this.mServiceState != null && MobileSignalController.this.mServiceState.isUsingCarrierAggregation()) {
                    MobileSignalController.this.mDataNetType = 19;
                }
            }
            MobileSignalController.this.updateTelephony();
        }

        @Override
        public void onDataConnectionStateChanged(int i, int i2) {
            if (SignalController.DEBUG) {
                Log.d(MobileSignalController.this.mTag, "onDataConnectionStateChanged: state=" + i + " type=" + i2);
            }
            MobileSignalController.this.mDataState = i;
            MobileSignalController.this.mDataNetType = i2;
            if (MobileSignalController.this.mDataNetType == 13 && MobileSignalController.this.mServiceState != null && MobileSignalController.this.mServiceState.isUsingCarrierAggregation()) {
                MobileSignalController.this.mDataNetType = 19;
            }
            MobileSignalController.this.updateTelephony();
        }

        @Override
        public void onPreciseDataConnectionStateChanged(PreciseDataConnectionState preciseDataConnectionState) {
            if (preciseDataConnectionState == null) {
                Log.w(MobileSignalController.this.mTag, "onPreciseDataConnectionStateChanged: dataConnectionState is null");
                return;
            }
            String dataConnectionAPNType = preciseDataConnectionState.getDataConnectionAPNType();
            if (dataConnectionAPNType != null && dataConnectionAPNType.equals("preempt") && preciseDataConnectionState.getDataConnectionState() != -1) {
                if (SignalController.DEBUG) {
                    Log.d(MobileSignalController.this.mTag, "onPreciseDataConnectionStateChanged: dataConnectionState=" + preciseDataConnectionState);
                }
                MobileSignalController.this.mDataState = preciseDataConnectionState.getDataConnectionState();
                MobileSignalController.this.mDataNetType = preciseDataConnectionState.getDataConnectionNetworkType();
                if (MobileSignalController.this.mDataNetType == 13 && MobileSignalController.this.mServiceState != null && MobileSignalController.this.mServiceState.isUsingCarrierAggregation()) {
                    MobileSignalController.this.mDataNetType = 19;
                }
                MobileSignalController.this.updateTelephony();
                return;
            }
            if (dataConnectionAPNType != null && dataConnectionAPNType.equals("default") && MobileSignalController.this.mDataState != preciseDataConnectionState.getDataConnectionState()) {
                if (SignalController.DEBUG) {
                    Log.d(MobileSignalController.this.mTag, "onPreciseDataConnectionStateChanged: APN_TYPE_DEFAULT, dataConnectionState=" + preciseDataConnectionState);
                }
                MobileSignalController.this.mDataState = preciseDataConnectionState.getDataConnectionState();
                MobileSignalController.this.updateTelephony();
            }
        }

        @Override
        public void onDataActivity(int i) {
            if (SignalController.DEBUG && FeatureOptions.LOG_ENABLE) {
                Log.d(MobileSignalController.this.mTag, "onDataActivity: direction=" + i);
            }
            MobileSignalController.this.setActivity(i);
        }

        public void onCarrierNetworkChange(boolean z) {
            if (SignalController.DEBUG && FeatureOptions.LOG_ENABLE) {
                Log.d(MobileSignalController.this.mTag, "onCarrierNetworkChange: active=" + z);
            }
            ((MobileState) MobileSignalController.this.mCurrentState).carrierNetworkChangeMode = z;
            MobileSignalController.this.updateTelephony();
        }

        @Override
        public void onCallStateChanged(int i, String str) {
            if (MobileSignalController.this.mStatusBarExt.handleCallStateChanged(MobileSignalController.this.mSubscriptionInfo.getSubscriptionId(), i, str, MobileSignalController.this.mServiceState)) {
                MobileSignalController.this.updateTelephony();
            }
        }
    }

    static class MobileIconGroup extends SignalController.IconGroup {
        final int mDataContentDescription;
        final int mDataType;
        final boolean mIsWide;
        final int mQsDataType;

        public MobileIconGroup(String str, int[][] iArr, int[][] iArr2, int[] iArr3, int i, int i2, int i3, int i4, int i5, int i6, int i7, boolean z) {
            super(str, iArr, iArr2, iArr3, i, i2, i3, i4, i5);
            this.mDataContentDescription = i6;
            this.mDataType = i7;
            this.mIsWide = z;
            this.mQsDataType = i7;
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
        boolean isDefault;
        boolean isDefaultData;
        boolean isEmergency;
        boolean isInCsCall;
        int networkIcon;
        String networkName;
        String networkNameData;
        boolean roaming;
        boolean userSetup;
        int volteIcon;
        int lwaRegState = -1;
        int imsRegState = 3;
        int imsCap = -1;

        MobileState() {
        }

        @Override
        public void copyFrom(SignalController.State state) {
            super.copyFrom(state);
            MobileState mobileState = (MobileState) state;
            this.dataSim = mobileState.dataSim;
            this.networkName = mobileState.networkName;
            this.networkNameData = mobileState.networkNameData;
            this.dataConnected = mobileState.dataConnected;
            this.isDefault = mobileState.isDefault;
            this.isEmergency = mobileState.isEmergency;
            this.airplaneMode = mobileState.airplaneMode;
            this.carrierNetworkChangeMode = mobileState.carrierNetworkChangeMode;
            this.userSetup = mobileState.userSetup;
            this.networkIcon = mobileState.networkIcon;
            this.dataNetType = mobileState.dataNetType;
            this.customizedState = mobileState.customizedState;
            this.isInCsCall = mobileState.isInCsCall;
            this.isDefaultData = mobileState.isDefaultData;
            this.customizedSignalStrengthIcon = mobileState.customizedSignalStrengthIcon;
            this.imsRegState = mobileState.imsRegState;
            this.imsCap = mobileState.imsCap;
            this.volteIcon = mobileState.volteIcon;
            this.roaming = mobileState.roaming;
            this.lwaRegState = mobileState.lwaRegState;
        }

        @Override
        protected void toString(StringBuilder sb) {
            super.toString(sb);
            sb.append(',');
            sb.append("dataSim=");
            sb.append(this.dataSim);
            sb.append(',');
            sb.append("networkName=");
            sb.append(this.networkName);
            sb.append(',');
            sb.append("networkNameData=");
            sb.append(this.networkNameData);
            sb.append(',');
            sb.append("dataConnected=");
            sb.append(this.dataConnected);
            sb.append(',');
            sb.append("roaming=");
            sb.append(this.roaming);
            sb.append(',');
            sb.append("isDefault=");
            sb.append(this.isDefault);
            sb.append(',');
            sb.append("isEmergency=");
            sb.append(this.isEmergency);
            sb.append(',');
            sb.append("airplaneMode=");
            sb.append(this.airplaneMode);
            sb.append(',');
            sb.append("lwaRegState=");
            sb.append(this.lwaRegState);
            sb.append(',');
            sb.append("carrierNetworkChangeMode=");
            sb.append(this.carrierNetworkChangeMode);
            sb.append(',');
            sb.append("userSetup=");
            sb.append(this.userSetup);
            sb.append(",");
            sb.append("networkIcon=");
            sb.append(this.networkIcon);
            sb.append(',');
            sb.append("dataNetType=");
            sb.append(this.dataNetType);
            sb.append(',');
            sb.append("customizedState=");
            sb.append(this.customizedState);
            sb.append(',');
            sb.append("isInCsCall=");
            sb.append(this.isInCsCall);
            sb.append(',');
            sb.append("isDefaultData=");
            sb.append(this.isDefaultData);
            sb.append(',');
            sb.append("customizedSignalStrengthIcon=");
            sb.append(this.customizedSignalStrengthIcon);
            sb.append(',');
            sb.append("imsRegState=");
            sb.append(this.imsRegState);
            sb.append(',');
            sb.append("imsCap=");
            sb.append(this.imsCap);
            sb.append(',');
            sb.append("volteIconId=");
            sb.append(this.volteIcon);
            sb.append(',');
            sb.append("carrierNetworkChangeMode=");
            sb.append(this.carrierNetworkChangeMode);
        }

        @Override
        public boolean equals(Object obj) {
            if (super.equals(obj)) {
                MobileState mobileState = (MobileState) obj;
                if (Objects.equals(mobileState.networkName, this.networkName) && Objects.equals(mobileState.networkNameData, this.networkNameData) && mobileState.dataSim == this.dataSim && mobileState.dataConnected == this.dataConnected && mobileState.isEmergency == this.isEmergency && mobileState.airplaneMode == this.airplaneMode && mobileState.carrierNetworkChangeMode == this.carrierNetworkChangeMode && mobileState.lwaRegState == this.lwaRegState && mobileState.networkIcon == this.networkIcon && mobileState.volteIcon == this.volteIcon && mobileState.dataNetType == this.dataNetType && mobileState.customizedState == this.customizedState && mobileState.isInCsCall == this.isInCsCall && mobileState.isDefaultData == this.isDefaultData && mobileState.customizedSignalStrengthIcon == this.customizedSignalStrengthIcon && mobileState.userSetup == this.userSetup && mobileState.isDefault == this.isDefault && mobileState.roaming == this.roaming) {
                    return true;
                }
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

    public void registerImsListener() {
        Log.i(this.mTag, "registerImsListener >>>");
        this.mImsManager = ImsManager.getInstance(this.mContext, this.mSubscriptionInfo.getSimSlotIndex());
        unregisterImsListener();
        try {
            this.mImsManager.addImsConnectionStateListener(this.mImsConnectionStateListener);
            Log.i(this.mTag, "register ims succeed, " + this.mImsConnectionStateListener);
        } catch (ImsException e) {
            Log.w(this.mTag, "register ims fail!");
        }
    }

    public void unregisterImsListener() {
        if (this.mImsManager != null) {
            try {
                this.mImsManager.removeImsConnectionStateListener(this.mImsConnectionStateListener);
                Log.i(this.mTag, "unregister ims succeed, " + this.mImsConnectionStateListener);
            } catch (ImsException e) {
                Log.w(this.mTag, "unregister ims fail!");
            }
        }
    }

    public String dumpServiceState(ServiceState serviceState) {
        if (serviceState == null) {
            return "ServiceState is null";
        }
        return "[ServiceState]dataNetworkType = " + serviceState.getDataNetworkType() + ", voiceNetworkType = " + serviceState.getVoiceNetworkType() + ", dataRegState = " + serviceState.getDataRegState() + ", voiceRegState = " + serviceState.getVoiceRegState() + ", isUsingCarrierAggregation = " + serviceState.isUsingCarrierAggregation() + ", cdmaEriIconMode = " + serviceState.getCdmaEriIconMode() + ", cdmaEriIconIndex = " + serviceState.getCdmaEriIconIndex() + ", roaming = " + serviceState.getRoaming();
    }

    public String toString() {
        return "[MobileSignalController: subid = " + this.mSubscriptionInfo.getSubscriptionId() + ", phoneId = " + this.mSubscriptionInfo.getSimSlotIndex() + "] " + super.toString();
    }
}
