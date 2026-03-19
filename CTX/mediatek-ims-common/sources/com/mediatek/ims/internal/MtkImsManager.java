package com.mediatek.ims.internal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.Rlog;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.ims.ImsCallProfile;
import android.telephony.ims.ImsCallSession;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.aidl.IImsCallSessionListener;
import android.telephony.ims.feature.CapabilityChangeRequest;
import android.telephony.ims.feature.ImsFeature;
import android.telephony.ims.feature.MmTelFeature;
import android.telephony.ims.stub.ImsRegistrationImplBase;
import android.text.TextUtils;
import android.util.Log;
import com.android.ims.ImsCall;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.ims.ImsUtInterface;
import com.android.ims.internal.IImsCallSession;
import com.android.ims.internal.IImsRegistrationListener;
import com.android.ims.internal.IImsUt;
import com.mediatek.ims.MtkImsCall;
import com.mediatek.ims.MtkImsConnectionStateListener;
import com.mediatek.ims.MtkImsUt;
import com.mediatek.ims.internal.IMtkImsService;
import com.mediatek.ims.internal.ext.IImsManagerExt;
import com.mediatek.ims.internal.ext.OpImsCustomizationUtils;
import com.mediatek.internal.telephony.IMtkPhoneSubInfoEx;
import com.mediatek.internal.telephony.MtkSubscriptionManager;
import com.mediatek.telephony.MtkTelephonyManagerEx;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import mediatek.telephony.MtkServiceState;

public class MtkImsManager extends ImsManager {
    public static final String ACTION_IMS_INCOMING_CALL_INDICATION = "com.android.ims.IMS_INCOMING_CALL_INDICATION";
    public static final String ACTION_IMS_NOT_RINGING_INCOMING_CALL = "com.mediatek.ims.NOT_RINGING_INCOMING_CALL";
    public static final String ACTION_IMS_RADIO_STATE_CHANGED = "com.android.ims.IMS_RADIO_STATE_CHANGED";
    public static final String ACTION_IMS_RTP_INFO = "com.android.ims.IMS_RTP_INFO";
    public static final String ACTION_IMS_SERVICE_DEREGISTERED = "com.android.ims.IMS_SERVICE_DEREGISTERED";
    public static final String DATA_ENABLED_PROP = "net.lte.ims.data.enabled";
    public static final String DATA_ROAMING_PROP = "net.lte.data.roaming";
    public static final String DATA_ROAMING_SETTING_PROP = "net.lte.data.roaming.setting";
    private static final boolean DBG = true;
    public static final String ENHANCED_4G_MODE_ENABLED_SIM2 = "volte_vt_enabled_sim2";
    public static final String ENHANCED_4G_MODE_ENABLED_SIM3 = "volte_vt_enabled_sim3";
    public static final String ENHANCED_4G_MODE_ENABLED_SIM4 = "volte_vt_enabled_sim4";
    public static final String EXTRA_CALL_MODE = "android:imsCallMode";
    public static final String EXTRA_DIAL_STRING = "android:imsDialString";
    public static final String EXTRA_IMS_DISABLE_CAP_KEY = "android:disablecap";
    public static final String EXTRA_IMS_ENABLE_CAP_KEY = "android:enablecap";
    public static final String EXTRA_IMS_RADIO_STATE = "android:imsRadioState";
    public static final String EXTRA_IMS_REG_ERROR_KEY = "android:regError";
    public static final String EXTRA_IMS_REG_STATE_KEY = "android:regState";
    public static final String EXTRA_MT_TO_NUMBER = "mediatek:mtToNumber";
    public static final String EXTRA_PHONE_ID = "android:phoneId";
    public static final String EXTRA_RTP_NETWORK_ID = "android:rtpNetworkId";
    public static final String EXTRA_RTP_PDN_ID = "android:rtpPdnId";
    public static final String EXTRA_RTP_RECV_PKT_LOST = "android:rtpRecvPktLost";
    public static final String EXTRA_RTP_SEND_PKT_LOST = "android:rtpSendPktLost";
    public static final String EXTRA_RTP_TIMER = "android:rtpTimer";
    public static final String EXTRA_RTT_INCOMING_CALL = "rtt_feature:rtt_incoming_call";
    public static final String EXTRA_SEQ_NUM = "android:imsSeqNum";
    public static final String MTK_IMS_SERVICE = "mtkIms";
    private static final String MULTI_IMS_SUPPORT = "persist.vendor.mims_support";
    public static final int OOS_END_WITH_DISCONN = 0;
    public static final int OOS_END_WITH_RESUME = 2;
    public static final int OOS_START = 1;
    public static final String PREFERRED_TTY_MODE = "preferred_tty_mode";
    public static final String PREFERRED_TTY_MODE_SIM2 = "preferred_tty_mode_sim2";
    public static final String PREFERRED_TTY_MODE_SIM3 = "preferred_tty_mode_sim3";
    public static final String PREFERRED_TTY_MODE_SIM4 = "preferred_tty_mode_sim4";
    private static final String PROPERTY_CAPABILITY_SWITCH = "persist.vendor.radio.simswitch";
    private static final String PROPERTY_CT_VOLTE_SUPPORT = "persist.vendor.mtk_ct_volte_support";
    private static final String PROPERTY_DYNAMIC_IMS_SWITCH = "persist.vendor.mtk_dynamic_ims_switch";
    private static final String PROPERTY_IMSCONFIG_FORCE_NOTIFY = "vendor.ril.imsconfig.force.notify";
    private static final String PROPERTY_IMS_SUPPORT = "persist.vendor.ims_support";
    private static final String PROPERTY_MTK_VILTE_SUPPORT = "persist.vendor.vilte_support";
    private static final String PROPERTY_MTK_VOLTE_SUPPORT = "persist.vendor.volte_support";
    private static final String PROPERTY_MTK_WFC_SUPPORT = "persist.vendor.mtk_wfc_support";
    private static final String PROPERTY_TEST_SIM1 = "vendor.gsm.sim.ril.testsim";
    private static final String PROPERTY_TEST_SIM2 = "vendor.gsm.sim.ril.testsim.2";
    private static final String PROPERTY_TEST_SIM3 = "vendor.gsm.sim.ril.testsim.3";
    private static final String PROPERTY_TEST_SIM4 = "vendor.gsm.sim.ril.testsim.4";
    private static final String PROPERTY_VILTE_ENALBE = "persist.vendor.mtk.vilte.enable";
    private static final String PROPERTY_VIWIFI_ENALBE = "persist.vendor.mtk.viwifi.enable";
    private static final String PROPERTY_VOLTE_ENALBE = "persist.vendor.mtk.volte.enable";
    private static final String PROPERTY_WFC_ENALBE = "persist.vendor.mtk.wfc.enable";
    public static final int SERVICE_REG_CAPABILITY_EVENT_ADDED = 1;
    public static final int SERVICE_REG_CAPABILITY_EVENT_ECC_NOT_SUPPORT = 4;
    public static final int SERVICE_REG_CAPABILITY_EVENT_ECC_SUPPORT = 2;
    public static final int SERVICE_REG_CAPABILITY_EVENT_REMOVED = 0;
    public static final int SERVICE_REG_EVENT_WIFI_PDN_OOS_END_WITH_DISCONN = 6;
    public static final int SERVICE_REG_EVENT_WIFI_PDN_OOS_END_WITH_RESUME = 7;
    public static final int SERVICE_REG_EVENT_WIFI_PDN_OOS_START = 5;
    protected static final int SIM_ID_1 = 0;
    protected static final int SIM_ID_2 = 1;
    protected static final int SIM_ID_3 = 2;
    protected static final int SIM_ID_4 = 3;
    private static final String TAG = "MtkImsManager";
    private static final String TTY_MODE = "tty_mode";
    private static final String VILTE_SETTING = "vilte_setting";
    private static final String VOLTE_SETTING = "volte_setting";
    public static final String VT_IMS_ENABLED_SIM2 = "vt_ims_enabled_sim2";
    public static final String VT_IMS_ENABLED_SIM3 = "vt_ims_enabled_sim3";
    public static final String VT_IMS_ENABLED_SIM4 = "vt_ims_enabled_sim4";
    public static final String WFC_IMS_ENABLED_SIM2 = "wfc_ims_enabled_sim2";
    public static final String WFC_IMS_ENABLED_SIM3 = "wfc_ims_enabled_sim3";
    public static final String WFC_IMS_ENABLED_SIM4 = "wfc_ims_enabled_sim4";
    public static final String WFC_IMS_MODE_SIM2 = "wfc_ims_mode_sim2";
    public static final String WFC_IMS_MODE_SIM3 = "wfc_ims_mode_sim3";
    public static final String WFC_IMS_MODE_SIM4 = "wfc_ims_mode_sim4";
    public static final String WFC_IMS_ROAMING_ENABLED_SIM2 = "wfc_ims_roaming_enabled_sim2";
    public static final String WFC_IMS_ROAMING_ENABLED_SIM3 = "wfc_ims_roaming_enabled_sim3";
    public static final String WFC_IMS_ROAMING_ENABLED_SIM4 = "wfc_ims_roaming_enabled_sim4";
    public static final String WFC_IMS_ROAMING_MODE_SIM2 = "wfc_ims_roaming_mode_sim2";
    public static final String WFC_IMS_ROAMING_MODE_SIM3 = "wfc_ims_roaming_mode_sim3";
    public static final String WFC_IMS_ROAMING_MODE_SIM4 = "wfc_ims_roaming_mode_sim4";
    private static final String WFC_MODE_SETTING = "wfc_mode_setting";
    private static final String WFC_ROAMING_MODE_SETTING = "wfc_roaming_mode_setting";
    private static final String WFC_ROAMING_SETTING = "wfc_roaming_setting";
    private static final String WFC_SETTING = "wfc_setting";
    private final BroadcastReceiver mBroadcastReceiver;
    private ArrayList<ImsRegistrationImplBase.Callback> mCallbacks;
    protected IImsRegistrationListener mListener;
    private MtkImsServiceDeathRecipient mMtkDeathRecipient;
    private IMtkImsService mMtkImsService;
    private MtkImsUt mMtkUt;
    private boolean mNotifyOnly;
    private static final String[] PROPERTY_ICCID_SIM = {"vendor.ril.iccid.sim1", "vendor.ril.iccid.sim2", "vendor.ril.iccid.sim3", "vendor.ril.iccid.sim4"};
    private static IImsManagerExt sImsManagerExt = null;
    private static final boolean mSupportImsiSwitch = SystemProperties.get("ro.vendor.mtk_imsi_switch_support", "0").equals("1");

    public static boolean isSupportMims() {
        if (SystemProperties.getInt(MULTI_IMS_SUPPORT, 1) > 1) {
            return DBG;
        }
        return false;
    }

    public static int getMainPhoneIdForSingleIms(Context context) {
        int i = SystemProperties.getInt(PROPERTY_CAPABILITY_SWITCH, 1) - 1;
        log("[getMainPhoneIdForSingleIms] : " + i);
        return i;
    }

    public static boolean isEnhanced4gLteModeSettingEnabledByUser(Context context, int i) {
        if (!isSupportMims()) {
            i = getMainPhoneIdForSingleIms(context);
        }
        return getAppropriateManagerForPlugin(context, i).isEnhanced4gLteModeSettingEnabledByUser();
    }

    public boolean isEnhanced4gLteModeSettingEnabledByUser() {
        int subId = getSubId();
        int integerSubscriptionProperty = SubscriptionManager.getIntegerSubscriptionProperty(subId, "volte_vt_enabled", -1, this.mContext);
        boolean booleanCarrierConfig = getBooleanCarrierConfig("enhanced_4g_lte_on_by_default_bool");
        boolean zIsPhoneIdSupportIms = isPhoneIdSupportIms(this.mPhoneId);
        if (subId == -1) {
            booleanCarrierConfig = false;
        }
        if (!getBooleanCarrierConfig("editable_enhanced_4g_lte_bool") || integerSubscriptionProperty == -1) {
            if (!booleanCarrierConfig) {
                return booleanCarrierConfig;
            }
            String str = SystemProperties.get(PROPERTY_ICCID_SIM[this.mPhoneId], "N/A");
            if (TextUtils.isEmpty(str) || !isOp09SimCard(str)) {
                return booleanCarrierConfig;
            }
            log("volte_setting, Replace volte value for CT card case");
            return false;
        }
        if (integerSubscriptionProperty != 1 || !zIsPhoneIdSupportIms) {
            return false;
        }
        return DBG;
    }

    public static void setEnhanced4gLteModeSetting(Context context, boolean z, int i) {
        if (!isSupportMims()) {
            i = getMainPhoneIdForSingleIms(context);
        }
        getAppropriateManagerForPlugin(context, i).setEnhanced4gLteModeSetting(z);
    }

    protected boolean shouldForceUpdated() {
        if (SystemProperties.getInt(PROPERTY_IMSCONFIG_FORCE_NOTIFY, 0) != 0) {
            return DBG;
        }
        return false;
    }

    public static boolean isNonTtyOrTtyOnVolteEnabled(Context context, int i) {
        if (!isSupportMims()) {
            i = getMainPhoneIdForSingleIms(context);
        }
        return getAppropriateManagerForPlugin(context, i).isNonTtyOrTtyOnVolteEnabled();
    }

    public boolean isVolteEnabledByPlatform() {
        if (SystemProperties.getInt("persist.dbg.volte_avail_ovr" + Integer.toString(this.mPhoneId), -1) == 1 || SystemProperties.getInt("persist.dbg.volte_avail_ovr", -1) == 1) {
            return DBG;
        }
        boolean zIsImsResourceSupport = isImsResourceSupport(0);
        boolean booleanCarrierConfig = getBooleanCarrierConfig("carrier_volte_available_bool");
        boolean zIsGbaValid = isGbaValid();
        boolean zIsFeatureEnabledByPlatformExt = isFeatureEnabledByPlatformExt(0);
        boolean zIsPhoneIdSupportIms = isPhoneIdSupportIms(this.mPhoneId);
        log("Volte, isResourceSupport:" + zIsImsResourceSupport + ", isCarrierConfigSupport:" + booleanCarrierConfig + ", isGbaValidSupport:" + zIsGbaValid + ", isFeatureEnableByPlatformExt:" + zIsFeatureEnabledByPlatformExt + ", isPSsupport:" + zIsPhoneIdSupportIms);
        if (SystemProperties.getInt(PROPERTY_MTK_VOLTE_SUPPORT, 0) == 1 && isLteSupported() && zIsImsResourceSupport && booleanCarrierConfig && zIsGbaValid && zIsFeatureEnabledByPlatformExt && zIsPhoneIdSupportIms) {
            return DBG;
        }
        return false;
    }

    public static void setVoltePreferSetting(Context context, int i, int i2) {
        if (!isSupportMims()) {
            i2 = getMainPhoneIdForSingleIms(context);
        }
        getAppropriateManagerForPlugin(context, i2).setVoltePreferSetting(i);
    }

    public void setVoltePreferSetting(int i) {
        try {
            MtkImsConfig configInterfaceEx = getConfigInterfaceEx();
            if (configInterfaceEx != null) {
                configInterfaceEx.setVoltePreference(i);
            }
        } catch (ImsException e) {
            loge("setVoltePreferSetting(): " + e);
        }
    }

    public static boolean isVtEnabledByPlatform(Context context, int i) {
        if (!isSupportMims()) {
            i = getMainPhoneIdForSingleIms(context);
        }
        return getAppropriateManagerForPlugin(context, i).isVtEnabledByPlatform();
    }

    protected boolean isDataRoaming() {
        if (SystemProperties.getInt(MULTI_IMS_SUPPORT, 1) == 1) {
            return SystemProperties.getBoolean(DATA_ROAMING_PROP + String.valueOf(getMainCapabilityPhoneId(this.mContext)), false);
        }
        return SystemProperties.getBoolean(DATA_ROAMING_PROP + String.valueOf(this.mPhoneId), false);
    }

    public void setDataRoaming(boolean z) {
        if (SystemProperties.getInt(MULTI_IMS_SUPPORT, 1) == 1) {
            int mainCapabilityPhoneId = getMainCapabilityPhoneId(this.mContext);
            log("[" + mainCapabilityPhoneId + "] setDataEnabled: " + z);
            SystemProperties.set(DATA_ROAMING_PROP + String.valueOf(mainCapabilityPhoneId), z ? "true" : "false");
            return;
        }
        log("[" + this.mPhoneId + "] setDataEnabled: " + z);
        SystemProperties.set(DATA_ROAMING_PROP + String.valueOf(this.mPhoneId), z ? "true" : "false");
    }

    protected boolean isDataRoamingSettingsEnabled() {
        if (SystemProperties.getInt(MULTI_IMS_SUPPORT, 1) == 1) {
            return SystemProperties.getBoolean(DATA_ROAMING_SETTING_PROP + String.valueOf(getMainCapabilityPhoneId(this.mContext)), false);
        }
        return SystemProperties.getBoolean(DATA_ROAMING_SETTING_PROP + String.valueOf(this.mPhoneId), false);
    }

    public void setDataRoamingSettingsEnabled(boolean z) {
        if (SystemProperties.getInt(MULTI_IMS_SUPPORT, 1) == 1) {
            int mainCapabilityPhoneId = getMainCapabilityPhoneId(this.mContext);
            log("[" + mainCapabilityPhoneId + "] setDataEnabled: " + z);
            SystemProperties.set(DATA_ROAMING_SETTING_PROP + String.valueOf(mainCapabilityPhoneId), z ? "true" : "false");
            return;
        }
        log("[" + this.mPhoneId + "] setDataEnabled: " + z);
        SystemProperties.set(DATA_ROAMING_SETTING_PROP + String.valueOf(this.mPhoneId), z ? "true" : "false");
    }

    public boolean isVtEnabledByPlatform() {
        boolean zIsImsResourceSupport;
        if (SystemProperties.getInt("persist.dbg.vt_avail_ovr" + Integer.toString(this.mPhoneId), -1) == 1 || SystemProperties.getInt("persist.dbg.vt_avail_ovr", -1) == 1) {
            return DBG;
        }
        if (!isTestSim()) {
            zIsImsResourceSupport = isImsResourceSupport(1);
        } else {
            zIsImsResourceSupport = true;
        }
        boolean booleanCarrierConfig = getBooleanCarrierConfig("carrier_vt_available_bool");
        boolean zIsGbaValid = isGbaValid();
        boolean zIsFeatureEnabledByPlatformExt = isFeatureEnabledByPlatformExt(1);
        log("Vt, isResourceSupport:" + zIsImsResourceSupport + ", isCarrierConfigSupport:" + booleanCarrierConfig + ", isGbaValidSupport:" + zIsGbaValid + ", isFeatureEnableByPlatformExt:" + zIsFeatureEnabledByPlatformExt);
        if (SystemProperties.getInt(PROPERTY_MTK_VILTE_SUPPORT, 0) == 1 && isLteSupported() && zIsImsResourceSupport && booleanCarrierConfig && zIsGbaValid && zIsFeatureEnabledByPlatformExt) {
            return DBG;
        }
        return false;
    }

    public static boolean isVtEnabledByUser(Context context, int i) {
        if (!isSupportMims()) {
            i = getMainPhoneIdForSingleIms(context);
        }
        return getAppropriateManagerForPlugin(context, i).isVtEnabledByUser();
    }

    public static void setVtSetting(Context context, boolean z, int i) {
        if (!isSupportMims()) {
            i = getMainPhoneIdForSingleIms(context);
        }
        getAppropriateManagerForPlugin(context, i).setVtSetting(z);
    }

    public void setVtSetting(boolean z) {
        SubscriptionManager.setSubscriptionProperty(getSubId(), "vt_ims_enabled", booleanToPropertyString(z));
        try {
            changeMmTelCapability(2, 0, z);
            changeMmTelCapability(2, 1, z);
            if (z) {
                log("setVtSetting(b) : turnOnIms");
                turnOnIms();
            } else if (isTurnOffImsAllowedByPlatform() && (!isVolteEnabledByPlatform() || !isEnhanced4gLteModeSettingEnabledByUser())) {
                log("setVtSetting(b) : imsServiceAllowTurnOff -> turnOffIms");
                turnOffIms();
            }
        } catch (ImsException e) {
            loge("setVtSetting(b): ", e);
        }
    }

    public static boolean isWfcEnabledByUser(Context context, int i) {
        if (!isSupportMims()) {
            i = getMainPhoneIdForSingleIms(context);
        }
        return getAppropriateManagerForPlugin(context, i).isWfcEnabledByUser();
    }

    public static void setWfcSetting(Context context, boolean z, int i) {
        if (!isSupportMims()) {
            i = getMainPhoneIdForSingleIms(context);
        }
        getAppropriateManagerForPlugin(context, i).setWfcSetting(z);
    }

    public static int getWfcMode(Context context, int i) {
        if (!isSupportMims()) {
            i = getMainPhoneIdForSingleIms(context);
        }
        return getAppropriateManagerForPlugin(context, i).getWfcMode();
    }

    public static void setWfcMode(Context context, int i, int i2) {
        log("setWfcMode(), setting=" + i + ", phoneId:" + i2);
        if (!isSupportMims()) {
            i2 = getMainPhoneIdForSingleIms(context);
        }
        getAppropriateManagerForPlugin(context, i2).setWfcMode(i);
    }

    public void setWfcMode(int i) {
        setWfcMode(i, false);
    }

    public static int getWfcMode(Context context, boolean z, int i) {
        if (!isSupportMims()) {
            i = getMainPhoneIdForSingleIms(context);
        }
        return getAppropriateManagerForPlugin(context, i).getWfcMode(z);
    }

    public static void setWfcMode(Context context, int i, boolean z, int i2) {
        log("setWfcMode(), wfcMode: " + i + ", roaming:" + z + ", phoneId:" + i2);
        if (!isSupportMims()) {
            i2 = getMainPhoneIdForSingleIms(context);
        }
        getAppropriateManagerForPlugin(context, i2).setWfcMode(i, z);
    }

    public void setWfcMode(int i, boolean z) {
        super.setWfcMode(i, z);
        if (z != ((((TelephonyManager) this.mContext.getSystemService("phone")).isNetworkRoaming(getSubId()) || isCellularDataRoaming()) ? DBG : false)) {
            setWfcModeInternal(i);
        }
    }

    protected void setWfcModeInternal(final int i) {
        setWfcModeConfigEx(i);
        new Thread(new Runnable() {
            @Override
            public final void run() {
                this.f$0.getConfigInterface().setConfig(27, i);
            }
        }).start();
    }

    private void setWfcModeConfigEx(int i) {
        log("setWfcModeConfigEx wfcMode:" + i + ", phoneId:" + this.mPhoneId);
        try {
            getConfigInterfaceEx().setWfcMode(i);
        } catch (ImsException e) {
        }
    }

    public static boolean isWfcRoamingEnabledByUser(Context context, int i) {
        if (!isSupportMims()) {
            i = getMainPhoneIdForSingleIms(context);
        }
        return getAppropriateManagerForPlugin(context, i).isWfcRoamingEnabledByUser();
    }

    public static void setWfcRoamingSetting(Context context, boolean z, int i) {
        log("setWfcRoamingSetting(), enabled: " + z + ", phoneId:" + i);
        if (!isSupportMims()) {
            i = getMainPhoneIdForSingleIms(context);
        }
        getAppropriateManagerForPlugin(context, i).setWfcRoamingSetting(z);
    }

    public boolean isWfcEnabledByPlatform() {
        if (SystemProperties.getInt("persist.dbg.wfc_avail_ovr" + Integer.toString(this.mPhoneId), -1) == 1 || SystemProperties.getInt("persist.dbg.wfc_avail_ovr", -1) == 1) {
            return DBG;
        }
        boolean zIsImsResourceSupport = isImsResourceSupport(2);
        boolean booleanCarrierConfig = getBooleanCarrierConfig("carrier_wfc_ims_available_bool");
        boolean zIsGbaValid = isGbaValid();
        boolean zIsFeatureEnabledByPlatformExt = isFeatureEnabledByPlatformExt(2);
        log("Wfc, isResourceSupport:" + zIsImsResourceSupport + ", isCarrierConfigSupport:" + booleanCarrierConfig + ", isGbaValidSupport:" + zIsGbaValid + ", isFeatureEnableByPlatformExt:" + zIsFeatureEnabledByPlatformExt);
        if (SystemProperties.getInt(PROPERTY_MTK_WFC_SUPPORT, 0) == 1 && isLteSupported() && zIsImsResourceSupport && booleanCarrierConfig && zIsGbaValid && zIsFeatureEnabledByPlatformExt) {
            return DBG;
        }
        return false;
    }

    private String getTtyModeSettingKeyForSlot() {
        if (this.mPhoneId == 1) {
            return PREFERRED_TTY_MODE_SIM2;
        }
        if (this.mPhoneId == 2) {
            return PREFERRED_TTY_MODE_SIM3;
        }
        if (this.mPhoneId == SIM_ID_4) {
            return PREFERRED_TTY_MODE_SIM4;
        }
        return PREFERRED_TTY_MODE;
    }

    private MtkImsManager(Context context, int i) {
        super(context, i);
        this.mMtkImsService = null;
        this.mMtkDeathRecipient = new MtkImsServiceDeathRecipient();
        this.mMtkUt = null;
        this.mCallbacks = new ArrayList<>();
        this.mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                MtkImsManager.log("[onReceive] action=" + intent.getAction());
                if ("com.mediatek.common.carrierexpress.operator_config_changed".equals(intent.getAction())) {
                    MtkImsManager.log("[onReceive] intent =" + intent.getAction());
                    IImsManagerExt unused = MtkImsManager.sImsManagerExt = null;
                    MtkImsManager.getImsManagerPluginInstance(context2);
                    return;
                }
                if ("com.mediatek.ims.MTK_IMS_SERVICE_UP".equals(intent.getAction())) {
                    MtkImsManager.logi("[onReceive] intent =" + intent.getAction());
                    MtkImsManager.this.mNotifyOnly = false;
                }
            }
        };
        this.mListener = null;
        this.mNotifyOnly = DBG;
        createMtkImsService(DBG);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.mediatek.common.carrierexpress.operator_config_changed");
        intentFilter.addAction("com.mediatek.ims.MTK_IMS_SERVICE_UP");
        context.registerReceiver(this.mBroadcastReceiver, intentFilter);
        getImsManagerPluginInstance(context);
    }

    public boolean isServiceAvailable() {
        if (!super.isServiceAvailable()) {
            logw("ImsService binder is not available and rebind again");
            createImsService();
        }
        IMtkImsService iMtkImsService = this.mMtkImsService;
        boolean z = DBG;
        if (iMtkImsService == null) {
            createMtkImsService(DBG);
        }
        if (this.mMtkImsService == null) {
            z = false;
        }
        log("isServiceAvailable=" + z);
        return z;
    }

    public void open(MmTelFeature.Listener listener) throws ImsException {
        checkAndThrowExceptionIfServiceUnavailable();
        log("open: phoneId=" + this.mPhoneId);
        super.open(listener);
    }

    public void close() {
        log("close");
        super.close();
        this.mMtkUt = null;
    }

    public ImsUtInterface getSupplementaryServiceConfiguration() throws ImsException {
        if (this.mMtkUt == null || !this.mMtkUt.isBinderAlive()) {
            checkAndThrowExceptionIfServiceUnavailable();
            try {
                IImsUt utInterface = this.mMmTelFeatureConnection.getUtInterface();
                IMtkImsUt mtkUtInterface = this.mMtkImsService.getMtkUtInterface(this.mPhoneId);
                if (utInterface == null) {
                    throw new ImsException("getSupplementaryServiceConfiguration()", 801);
                }
                this.mMtkUt = new MtkImsUt(utInterface, mtkUtInterface);
            } catch (RemoteException e) {
                throw new ImsException("getSupplementaryServiceConfiguration()", e, 106);
            }
        }
        return this.mMtkUt;
    }

    public ImsCall makeCall(ImsCallProfile imsCallProfile, String[] strArr, ImsCall.Listener listener) throws ImsException {
        log("makeCall :: profile=" + imsCallProfile + ", callees=" + Arrays.toString(strArr));
        checkAndThrowExceptionIfServiceUnavailable();
        MtkImsCall mtkImsCall = new MtkImsCall(this.mContext, imsCallProfile);
        mtkImsCall.setListener(listener);
        ImsCallSession imsCallSessionCreateCallSession = createCallSession(imsCallProfile);
        if (strArr != null && strArr.length == 1 && !imsCallProfile.getCallExtraBoolean("conference")) {
            mtkImsCall.start(imsCallSessionCreateCallSession, strArr[0]);
        } else {
            mtkImsCall.start(imsCallSessionCreateCallSession, strArr);
        }
        return mtkImsCall;
    }

    protected boolean updateVideoCallFeatureValue() throws ImsException {
        boolean zIsVtEnabledByPlatform = isVtEnabledByPlatform();
        boolean z = (isEnhanced4gLteModeSettingEnabledByUser() || isWfcEnabledByUser()) && isVtEnabledByUser();
        boolean zIsNonTtyOrTtyOnVolteEnabled = isNonTtyOrTtyOnVolteEnabled();
        boolean zIsDataEnabled = isDataEnabled();
        boolean booleanCarrierConfig = getBooleanCarrierConfig("ignore_data_enabled_changed_for_video_calls");
        boolean z2 = !isDataRoaming() || isDataRoamingSettingsEnabled();
        boolean booleanCarrierConfig2 = getBooleanCarrierConfig("mtk_ignore_data_roaming_for_video_calls");
        boolean z3 = zIsVtEnabledByPlatform && z && zIsNonTtyOrTtyOnVolteEnabled && (booleanCarrierConfig || ((zIsDataEnabled && (booleanCarrierConfig2 || z2)) || isTestSim()));
        log("[" + this.mPhoneId + "] updateVideoCallFeatureValue: available = " + zIsVtEnabledByPlatform + ", enabled = " + z + ", nonTTY = " + zIsNonTtyOrTtyOnVolteEnabled + ", ignoreDataEnabledChanged = " + booleanCarrierConfig + ", data enabled = " + zIsDataEnabled + ", ignoreDataRoaming = " + booleanCarrierConfig2 + ", data roaming enabled = " + z2 + ", is test sim = " + isTestSim());
        changeMmTelCapability(2, 0, z3);
        changeMmTelCapability(2, 1, z3);
        return z3;
    }

    private void checkAndThrowExceptionIfServiceUnavailable() throws ImsException {
        if (this.mMmTelFeatureConnection == null || !this.mMmTelFeatureConnection.isBinderAlive()) {
            createImsService();
            if (this.mMmTelFeatureConnection == null) {
                throw new ImsException("Service is unavailable", 106);
            }
        }
        if (this.mMtkImsService == null) {
            createMtkImsService(DBG);
            if (this.mMtkImsService == null) {
                throw new ImsException("MtkImsService is unavailable", 106);
            }
        }
    }

    private static String getMtkImsServiceName(int i) {
        return "mtkIms";
    }

    private void createMtkImsService(boolean z) {
        if (z && ServiceManager.checkService(getMtkImsServiceName(this.mPhoneId)) == null) {
            log("createMtkImsService binder is null");
            return;
        }
        IBinder service = ServiceManager.getService(getMtkImsServiceName(this.mPhoneId));
        if (service != null) {
            try {
                service.linkToDeath(this.mMtkDeathRecipient, 0);
            } catch (RemoteException e) {
            }
        }
        this.mMtkImsService = IMtkImsService.Stub.asInterface(service);
        log("mMtkImsService = " + this.mMtkImsService);
    }

    private static void logi(String str) {
        Rlog.i(TAG, str);
    }

    private static void log(String str) {
        Rlog.d(TAG, str);
    }

    private static void logw(String str) {
        Rlog.w(TAG, str);
    }

    private static void loge(String str) {
        Rlog.e(TAG, str);
    }

    private static void loge(String str, Throwable th) {
        Rlog.e(TAG, str, th);
    }

    private class MtkImsServiceDeathRecipient implements IBinder.DeathRecipient {
        private MtkImsServiceDeathRecipient() {
        }

        @Override
        public void binderDied() {
            MtkImsManager.this.mMtkImsService = null;
            MtkImsManager.this.mMtkUt = null;
        }
    }

    private String getCallNum(Intent intent) {
        if (intent == null) {
            return null;
        }
        return intent.getStringExtra(EXTRA_DIAL_STRING);
    }

    private int getSeqNum(Intent intent) {
        if (intent == null) {
            return -1;
        }
        return intent.getIntExtra(EXTRA_SEQ_NUM, -1);
    }

    private String getMtToNumber(Intent intent) {
        if (intent == null) {
            return null;
        }
        return intent.getStringExtra(EXTRA_MT_TO_NUMBER);
    }

    public void setCallIndication(int i, Intent intent, boolean z) throws ImsException {
        log("setCallIndication :: phoneId=" + i + ", incomingCallIndication=" + intent);
        checkAndThrowExceptionIfServiceUnavailable();
        if (intent == null) {
            throw new ImsException("Can't retrieve session with null intent", 101);
        }
        if (i != getPhoneId(intent)) {
            throw new ImsException("Service id is mismatched in the incoming call intent", 101);
        }
        String callId = getCallId(intent.getExtras());
        if (callId == null) {
            throw new ImsException("Call ID missing in the incoming call intent", 101);
        }
        String callNum = getCallNum(intent);
        if (callNum == null) {
            throw new ImsException("Call Num missing in the incoming call intent", 101);
        }
        int seqNum = getSeqNum(intent);
        if (seqNum == -1) {
            throw new ImsException("seqNum missing in the incoming call intent", 101);
        }
        try {
            this.mMtkImsService.setCallIndication(i, callId, callNum, seqNum, getMtToNumber(intent), z);
        } catch (RemoteException e) {
            throw new ImsException("setCallIndication()", e, 106);
        }
    }

    public void hangupAllCall(int i) throws ImsException {
        checkAndThrowExceptionIfServiceUnavailable();
        try {
            this.mMtkImsService.hangupAllCall(i);
        } catch (RemoteException e) {
            throw new ImsException("hangupAll()", e, 106);
        }
    }

    private static int getPhoneId(Intent intent) {
        if (intent == null) {
            return -1;
        }
        return intent.getIntExtra(EXTRA_PHONE_ID, -1);
    }

    protected boolean isImsResourceSupport(int i) {
        log("isImsResourceSupport, feature:" + i);
        boolean zEquals = "1".equals(SystemProperties.get(PROPERTY_DYNAMIC_IMS_SWITCH));
        boolean z = DBG;
        if (zEquals) {
            if (!SubscriptionManager.isValidPhoneId(this.mPhoneId)) {
                loge("Invalid main phone " + this.mPhoneId + ", return true as don't care");
                return DBG;
            }
            try {
                MtkImsConfig configInterfaceEx = getConfigInterfaceEx();
                if (configInterfaceEx != null) {
                    if (configInterfaceEx.getImsResCapability(i) != 1) {
                        z = false;
                    }
                }
            } catch (ImsException e) {
                loge("isImsResourceSupport() failed!" + e);
            }
            log("isImsResourceSupport(" + i + ") return " + z + " on phone: " + this.mPhoneId);
        }
        return z;
    }

    public static void factoryReset(Context context, int i) {
        log("factoryReset: phoneId=" + i);
        if (!isSupportMims()) {
            int mainPhoneIdForSingleIms = getMainPhoneIdForSingleIms(context);
            if (SystemProperties.getInt(PROPERTY_CT_VOLTE_SUPPORT, 0) != 0 && mainPhoneIdForSingleIms != i) {
                log("factoryReset: mainPhoneId=" + mainPhoneIdForSingleIms);
                return;
            }
            ((MtkImsManager) ImsManager.getInstance(context, mainPhoneIdForSingleIms)).factoryReset();
            return;
        }
        getAppropriateManagerForPlugin(context, i).factoryReset();
    }

    public void factoryReset() {
        boolean z = SystemProperties.getInt(PROPERTY_CT_VOLTE_SUPPORT, 0) == 0 || getBooleanCarrierConfig("enhanced_4g_lte_on_by_default_bool");
        if (!isSubOfDualCtSim()) {
            if (z) {
                String simSerialNumber = ((TelephonyManager) this.mContext.getSystemService("phone")).getSimSerialNumber(MtkSubscriptionManager.getSubIdUsingPhoneId(this.mPhoneId));
                if (!TextUtils.isEmpty(simSerialNumber) && isOp09SimCard(simSerialNumber)) {
                    log("factoryReset, Replace volte value for CT roaming case");
                    z = false;
                }
            }
        } else {
            z = true;
        }
        SubscriptionManager.setSubscriptionProperty(getSubId(), "volte_vt_enabled", booleanToPropertyString(z));
        SubscriptionManager.setSubscriptionProperty(getSubId(), "wfc_ims_enabled", booleanToPropertyString(getBooleanCarrierConfig("carrier_default_wfc_ims_enabled_bool")));
        SubscriptionManager.setSubscriptionProperty(getSubId(), "wfc_ims_mode", Integer.toString(getIntCarrierConfig("carrier_default_wfc_ims_mode_int")));
        SubscriptionManager.setSubscriptionProperty(getSubId(), "wfc_ims_roaming_enabled", booleanToPropertyString(getBooleanCarrierConfig("carrier_default_wfc_ims_roaming_enabled_bool")));
        SubscriptionManager.setSubscriptionProperty(getSubId(), "vt_ims_enabled", booleanToPropertyString(DBG));
        updateImsServiceConfig(DBG);
    }

    public MtkImsConfig getConfigInterfaceEx() throws ImsException {
        checkAndThrowExceptionIfServiceUnavailable();
        try {
            IMtkImsConfig configInterfaceEx = this.mMtkImsService.getConfigInterfaceEx(this.mPhoneId);
            if (configInterfaceEx == null) {
                throw new ImsException("getConfigInterfaceEx()", 131);
            }
            return new MtkImsConfig(configInterfaceEx, this.mContext);
        } catch (RemoteException e) {
            throw new ImsException("getConfigInterfaceEx()", e, 106);
        }
    }

    public ImsCall takeCall(IImsCallSession iImsCallSession, Bundle bundle, ImsCall.Listener listener) throws ImsException {
        log("takeCall :: incomingCall=" + bundle);
        checkAndThrowExceptionIfServiceUnavailable();
        if (bundle == null) {
            throw new ImsException("Can't retrieve session with null intent", 101);
        }
        String callId = getCallId(bundle);
        if (callId == null) {
            throw new ImsException("Call ID missing in the incoming call intent", 101);
        }
        try {
            IMtkImsCallSession pendingMtkCallSession = this.mMtkImsService.getPendingMtkCallSession(callId);
            IImsCallSession iImsCallSession2 = pendingMtkCallSession.getIImsCallSession();
            if (pendingMtkCallSession == null) {
                throw new ImsException("No pending IMtkImsCallSession for the call", 107);
            }
            ImsCallProfile callProfile = pendingMtkCallSession.getCallProfile();
            if (callProfile == null) {
                throw new ImsException("takeCall(): profile is null", 0);
            }
            MtkImsCall mtkImsCall = new MtkImsCall(this.mContext, callProfile);
            mtkImsCall.attachSession(new MtkImsCallSession(iImsCallSession2, pendingMtkCallSession));
            mtkImsCall.setListener(listener);
            return mtkImsCall;
        } catch (Throwable th) {
            throw new ImsException("takeCall()", th, 0);
        }
    }

    protected ImsCallSession createCallSession(ImsCallProfile imsCallProfile) throws ImsException {
        try {
            log("createCallSession: profile = " + imsCallProfile);
            ImsCallSession imsCallSessionCreateCallSession = super.createCallSession(imsCallProfile);
            log("createCallSession: imsCallSession = " + imsCallSessionCreateCallSession);
            log("createCallSession: imsCallSession.getSession() = " + imsCallSessionCreateCallSession.getSession());
            return new MtkImsCallSession(imsCallSessionCreateCallSession.getSession(), this.mMtkImsService.createMtkCallSession(this.mPhoneId, imsCallProfile, (IImsCallSessionListener) null, imsCallSessionCreateCallSession.getSession()));
        } catch (RemoteException e) {
            Rlog.w(TAG, "CreateCallSession: Error, remote exception: " + e.getMessage());
            throw new ImsException("createCallSession()", e, 106);
        }
    }

    protected void setLteFeatureValues(boolean z) {
        log("setLteFeatureValues: " + z);
        CapabilityChangeRequest capabilityChangeRequest = new CapabilityChangeRequest();
        if (z) {
            capabilityChangeRequest.addCapabilitiesToEnableForTech(1, 0);
        } else {
            capabilityChangeRequest.addCapabilitiesToDisableForTech(1, 0);
        }
        if (isVolteEnabledByPlatform() && isVtEnabledByPlatform()) {
            if (z && isVtEnabledByUser() && (getBooleanCarrierConfig("ignore_data_enabled_changed_for_video_calls") || ((isDataEnabled() && (getBooleanCarrierConfig("mtk_ignore_data_roaming_for_video_calls") || (!isDataRoaming() || isDataRoamingSettingsEnabled()))) || isTestSim()))) {
                capabilityChangeRequest.addCapabilitiesToEnableForTech(2, 0);
                capabilityChangeRequest.addCapabilitiesToEnableForTech(2, 1);
            } else {
                capabilityChangeRequest.addCapabilitiesToDisableForTech(2, 0);
                capabilityChangeRequest.addCapabilitiesToDisableForTech(2, 1);
            }
        }
        try {
            this.mMmTelFeatureConnection.changeEnabledCapabilities(capabilityChangeRequest, (ImsFeature.CapabilityCallback) null);
        } catch (RemoteException e) {
            Log.e(TAG, "setLteFeatureValues: Exception: " + e.getMessage());
        }
    }

    private boolean isFeatureEnabledByPlatformExt(int i) {
        if (this.mContext == null) {
            logw("Invalid: context=" + this.mContext + ", return " + DBG);
            return DBG;
        }
        if (sImsManagerExt == null) {
            logw("plugin null=" + sImsManagerExt + ", return " + DBG);
            return DBG;
        }
        boolean zIsFeatureEnabledByPlatform = sImsManagerExt.isFeatureEnabledByPlatform(this.mContext, i, this.mPhoneId);
        log("isFeatureEnabledByPlatformExt(), feature:" + i + ", isEnabled:" + zIsFeatureEnabledByPlatform);
        return zIsFeatureEnabledByPlatform;
    }

    protected int getMainCapabilityPhoneId(Context context) {
        return getMainPhoneIdForSingleIms(context);
    }

    private static IImsManagerExt getImsManagerPluginInstance(Context context) {
        log("getImsManagerPluginInstance");
        if (sImsManagerExt == null) {
            sImsManagerExt = OpImsCustomizationUtils.getOpFactory(context).makeImsManagerExt(context);
            if (sImsManagerExt == null) {
                log("Unable to create ImsManagerPluginInstane");
            }
        }
        return sImsManagerExt;
    }

    protected boolean isTestSim() {
        boolean zEquals;
        int mainCapabilityPhoneId = this.mPhoneId;
        if (SystemProperties.getInt(MULTI_IMS_SUPPORT, 1) == 1) {
            mainCapabilityPhoneId = getMainCapabilityPhoneId(this.mContext);
        }
        switch (mainCapabilityPhoneId) {
            case 0:
                zEquals = "1".equals(SystemProperties.get(PROPERTY_TEST_SIM1, "0"));
                break;
            case 1:
                zEquals = "1".equals(SystemProperties.get(PROPERTY_TEST_SIM2, "0"));
                break;
            case 2:
                zEquals = "1".equals(SystemProperties.get(PROPERTY_TEST_SIM3, "0"));
                break;
            case SIM_ID_4:
                zEquals = "1".equals(SystemProperties.get(PROPERTY_TEST_SIM4, "0"));
                break;
            default:
                zEquals = false;
                break;
        }
        if (zEquals) {
            String simOperatorNumericForPhone = ((TelephonyManager) this.mContext.getSystemService("phone")).getSimOperatorNumericForPhone(mainCapabilityPhoneId);
            log("isTestSim, currentMccMnc:" + simOperatorNumericForPhone);
            if (simOperatorNumericForPhone != null && !simOperatorNumericForPhone.equals("") && !"00101".equals(simOperatorNumericForPhone) && !"11111".equals(simOperatorNumericForPhone) && !"46011".equals(simOperatorNumericForPhone)) {
                return false;
            }
        }
        return zEquals;
    }

    private boolean isSubOfDualCtSim() {
        if (isSupportMims() && SystemProperties.getInt(PROPERTY_CT_VOLTE_SUPPORT, 0) != 0 && getMainCapabilityPhoneId(this.mContext) != this.mPhoneId) {
            String str = SystemProperties.get(PROPERTY_ICCID_SIM[0], "N/A");
            String str2 = SystemProperties.get(PROPERTY_ICCID_SIM[1], "N/A");
            if (isOp09SimCard(str) && isOp09SimCard(str2)) {
                log("Two CT card case");
                return DBG;
            }
        }
        return false;
    }

    private static boolean isOp09SimCard(String str) {
        if (str.startsWith("898603") || str.startsWith("898611") || str.startsWith("8985302") || str.startsWith("8985307") || str.startsWith("8985231")) {
            return DBG;
        }
        return false;
    }

    private static MtkImsManager getAppropriateManagerForPlugin(Context context, int i) {
        if (sImsManagerExt == null) {
            getImsManagerPluginInstance(context);
        }
        if (sImsManagerExt != null) {
            i = sImsManagerExt.getImsPhoneId(context, i);
        }
        return (MtkImsManager) ImsManager.getInstance(context, i);
    }

    public void notifyRegServiceCapabilityChangedEvent(int i) {
        if (i == 2) {
            synchronized (this.mCallbacks) {
                Iterator<ImsRegistrationImplBase.Callback> it = this.mCallbacks.iterator();
                while (it.hasNext()) {
                    MtkImsConnectionStateListener mtkImsConnectionStateListener = (ImsRegistrationImplBase.Callback) it.next();
                    if (mtkImsConnectionStateListener instanceof MtkImsConnectionStateListener) {
                        mtkImsConnectionStateListener.onImsEmergencyCapabilityChanged(DBG);
                    }
                }
            }
            return;
        }
        switch (i) {
            case SERVICE_REG_CAPABILITY_EVENT_ECC_NOT_SUPPORT:
                synchronized (this.mCallbacks) {
                    Iterator<ImsRegistrationImplBase.Callback> it2 = this.mCallbacks.iterator();
                    while (it2.hasNext()) {
                        MtkImsConnectionStateListener mtkImsConnectionStateListener2 = (ImsRegistrationImplBase.Callback) it2.next();
                        if (mtkImsConnectionStateListener2 instanceof MtkImsConnectionStateListener) {
                            mtkImsConnectionStateListener2.onImsEmergencyCapabilityChanged(false);
                        }
                    }
                    break;
                }
                return;
            case SERVICE_REG_EVENT_WIFI_PDN_OOS_START:
                synchronized (this.mCallbacks) {
                    Iterator<ImsRegistrationImplBase.Callback> it3 = this.mCallbacks.iterator();
                    while (it3.hasNext()) {
                        MtkImsConnectionStateListener mtkImsConnectionStateListener3 = (ImsRegistrationImplBase.Callback) it3.next();
                        if (mtkImsConnectionStateListener3 instanceof MtkImsConnectionStateListener) {
                            mtkImsConnectionStateListener3.onWifiPdnOOSStateChanged(1);
                        }
                    }
                    break;
                }
                return;
            case SERVICE_REG_EVENT_WIFI_PDN_OOS_END_WITH_DISCONN:
                synchronized (this.mCallbacks) {
                    Iterator<ImsRegistrationImplBase.Callback> it4 = this.mCallbacks.iterator();
                    while (it4.hasNext()) {
                        MtkImsConnectionStateListener mtkImsConnectionStateListener4 = (ImsRegistrationImplBase.Callback) it4.next();
                        if (mtkImsConnectionStateListener4 instanceof MtkImsConnectionStateListener) {
                            mtkImsConnectionStateListener4.onWifiPdnOOSStateChanged(0);
                        }
                    }
                    break;
                }
                return;
            case SERVICE_REG_EVENT_WIFI_PDN_OOS_END_WITH_RESUME:
                synchronized (this.mCallbacks) {
                    Iterator<ImsRegistrationImplBase.Callback> it5 = this.mCallbacks.iterator();
                    while (it5.hasNext()) {
                        MtkImsConnectionStateListener mtkImsConnectionStateListener5 = (ImsRegistrationImplBase.Callback) it5.next();
                        if (mtkImsConnectionStateListener5 instanceof MtkImsConnectionStateListener) {
                            mtkImsConnectionStateListener5.onWifiPdnOOSStateChanged(2);
                        }
                    }
                    break;
                }
                return;
            default:
                return;
        }
    }

    private boolean isLteSupported() {
        return SystemProperties.get("ro.boot.opt_ps1_rat", "").contains("L");
    }

    private static int getFeaturePropValue(String str, int i) {
        int i2 = SystemProperties.getInt(str, 0);
        if (isSupportMims()) {
            if ((i2 & (1 << i)) <= 0) {
                return 0;
            }
        } else if ((i2 & 1) <= 0) {
            return 0;
        }
        return 1;
    }

    private void setComboFeatureValue(int i, int i2, int i3) {
        int[] iArr = {0, 1, SIM_ID_4, 2};
        int[] iArr2 = {13, 13, 18, 18};
        int[] iArr3 = {0, 0, 0, 0};
        int featurePropValue = getFeaturePropValue(PROPERTY_VOLTE_ENALBE, this.mPhoneId);
        int featurePropValue2 = getFeaturePropValue(PROPERTY_VILTE_ENALBE, this.mPhoneId);
        int featurePropValue3 = getFeaturePropValue(PROPERTY_VIWIFI_ENALBE, this.mPhoneId);
        int featurePropValue4 = getFeaturePropValue(PROPERTY_WFC_ENALBE, this.mPhoneId);
        if (i == -1) {
            i = featurePropValue;
        }
        iArr3[0] = i;
        if (i2 != -1) {
            featurePropValue2 = i2;
        }
        iArr3[1] = featurePropValue2;
        if (i2 == -1) {
            i2 = featurePropValue3;
        }
        iArr3[2] = i2;
        if (i3 == -1) {
            i3 = featurePropValue4;
        }
        iArr3[SIM_ID_4] = i3;
        try {
            MtkImsConfig configInterfaceEx = ((MtkImsManager) ImsManager.getInstance(this.mContext, this.mPhoneId)).getConfigInterfaceEx();
            if (configInterfaceEx != null) {
                configInterfaceEx.setMultiFeatureValues(iArr, iArr2, iArr3, this.mImsConfigListener);
            }
        } catch (ImsException e) {
            loge("setComboFeatureValue(): " + e);
        }
    }

    public void setEnhanced4gLteModeVtSetting(Context context, boolean z, boolean z2) {
        int i;
        int i2;
        ?? r1;
        ?? r5;
        boolean z3;
        ?? r52;
        ImsManager imsManager = ImsManager.getInstance(context, ((MtkImsManager) this).mPhoneId);
        if (imsManager != null) {
            try {
                imsManager.getConfigInterface();
                int i3 = 1;
                if (!isSupportMims()) {
                    boolean zIsEnhanced4gLteModeSettingEnabledByUser = isEnhanced4gLteModeSettingEnabledByUser(context, ((MtkImsManager) this).mPhoneId);
                    Settings.Global.putInt(context.getContentResolver(), "volte_vt_enabled", z ? 1 : 0);
                    Settings.Global.putInt(context.getContentResolver(), "vt_ims_enabled", z2 ? 1 : 0);
                    i2 = z ? 1 : 0;
                    r1 = zIsEnhanced4gLteModeSettingEnabledByUser;
                } else {
                    int i4 = getBooleanCarrierConfig("editable_enhanced_4g_lte_bool") ? z ? 1 : 0 : 1;
                    int integerSubscriptionProperty = SubscriptionManager.getIntegerSubscriptionProperty(getSubId(), "volte_vt_enabled", -1, ((MtkImsManager) this).mContext);
                    if (integerSubscriptionProperty != i4 || SystemProperties.getInt(PROPERTY_IMSCONFIG_FORCE_NOTIFY, 0) != 0) {
                        SubscriptionManager.setSubscriptionProperty(getSubId(), "volte_vt_enabled", Integer.toString(z ? 1 : 0));
                        i = z ? 1 : 0;
                    } else {
                        i = integerSubscriptionProperty;
                    }
                    SubscriptionManager.setSubscriptionProperty(getSubId(), "volte_vt_enabled", Integer.toString(i));
                    i2 = i;
                    r1 = integerSubscriptionProperty;
                }
                if (isNonTtyOrTtyOnVolteEnabled(context, ((MtkImsManager) this).mPhoneId)) {
                    r5 = i2;
                    if (isVolteEnabledByPlatform()) {
                        r5 = i2;
                        if (isVtEnabledByPlatform()) {
                            boolean booleanCarrierConfig = getBooleanCarrierConfig("ignore_data_enabled_changed_for_video_calls");
                            if (z && isVtEnabledByUser() && (booleanCarrierConfig || isDataEnabled() || isTestSim())) {
                                z3 = true;
                                r52 = i2;
                            } else {
                                r5 = i2;
                            }
                        }
                    }
                    if (z2 || !z3) {
                        i3 = 0;
                    }
                    setComboFeatureValue(r52, i3, -1);
                    if (!z && !z2) {
                        if (!ImsManager.isTurnOffImsAllowedByPlatform(context)) {
                            if (!imsManager.isVolteEnabledByPlatform() || !isEnhanced4gLteModeSettingEnabledByUser(context, ((MtkImsManager) this).mPhoneId)) {
                                if (!imsManager.isWfcEnabledByPlatform() || !isWfcEnabledByUser()) {
                                    log("setEnhanced4gLteModeVtSetting() : imsServiceAllowTurnOff -> turnOffIms");
                                    turnOffIms();
                                    return;
                                }
                                return;
                            }
                            return;
                        }
                        return;
                    }
                    log("setEnhanced4gLteModeVtSetting() : turnOnIms");
                    turnOnIms();
                    return;
                }
                r5 = r1;
                z3 = false;
                r52 = r5;
                if (z2) {
                    i3 = 0;
                }
                setComboFeatureValue(r52, i3, -1);
                if (!z) {
                    if (!ImsManager.isTurnOffImsAllowedByPlatform(context)) {
                    }
                }
                log("setEnhanced4gLteModeVtSetting() : turnOnIms");
                turnOnIms();
                return;
            } catch (ImsException e) {
                loge("setEnhanced4gLteModeVtSetting error");
                return;
            }
        }
        loge("setEnhanced4gLteModeVtSetting error");
        loge("getInstance null for phoneId=" + ((MtkImsManager) this).mPhoneId);
    }

    public MmTelFeature.MmTelCapabilities queryCapabilityStatus() {
        MmTelFeature.MmTelCapabilities mmTelCapabilities;
        StringBuilder sb;
        try {
            try {
                mmTelCapabilities = this.mMmTelFeatureConnection.queryCapabilityStatus();
                sb = new StringBuilder();
            } catch (RemoteException e) {
                loge("Fail to queryCapabilityStatus " + e.getMessage());
                mmTelCapabilities = new MmTelFeature.MmTelCapabilities();
                sb = new StringBuilder();
            }
            sb.append("queryCapabilityStatus = ");
            sb.append(mmTelCapabilities);
            log(sb.toString());
            return mmTelCapabilities;
        } catch (Throwable th) {
            log("queryCapabilityStatus = " + ((Object) null));
            return null;
        }
    }

    public void removeCapabilityCallback(ImsFeature.CapabilityCallback capabilityCallback) throws ImsException {
        if (capabilityCallback == null) {
            throw new NullPointerException("capabilities callback can't be null");
        }
        checkAndThrowExceptionIfServiceUnavailable();
        try {
            this.mMmTelFeatureConnection.removeCapabilityCallback(capabilityCallback);
            log("Capability Callback removed.");
        } catch (RemoteException e) {
            throw new ImsException("removeCapabilityCallback(IF)", e, 106);
        }
    }

    private boolean shouldEnableImsForIR(Context context, int i) {
        boolean z = mSupportImsiSwitch;
        boolean z2 = DBG;
        if (!z) {
            log("[IR] IMSI switch feature not supported");
            return DBG;
        }
        int subIdUsingPhoneId = MtkSubscriptionManager.getSubIdUsingPhoneId(i);
        if (subIdUsingPhoneId == -1) {
            log("[IR] shouldEnableImsForIR: Invalid subId so return");
            return DBG;
        }
        String operatorNumericFromImpi = getOperatorNumericFromImpi("0", i);
        String mccMncForSubId = getMccMncForSubId(subIdUsingPhoneId, SubscriptionManager.from(context));
        if (!operatorNumericFromImpi.equals(mccMncForSubId) && !"0".equals(operatorNumericFromImpi)) {
            z2 = false;
        }
        log("[IR] updateVolteFeatureValue: subId = " + subIdUsingPhoneId + ", phoneId = " + i + ", Current currentMccMnc = " + mccMncForSubId + ", permanentMccMnc = " + operatorNumericFromImpi + ", enableIms = " + z2);
        return z2;
    }

    private String getOperatorNumericFromImpi(String str, int i) {
        String[] strArr = {"405840", "405854", "405855", "405856", "405857", "405858", "405859", "405860", "405861", "405862", "405863", "405864", "405865", "405866", "405867", "405868", "405869", "405870", "405871", "405872", "405873", "405874"};
        if (strArr.length == 0) {
            log("[IR] mImsMccMncList is null, returning default mccmnc");
            return str;
        }
        log("[IR] IMPI requested by phoneId: " + i);
        String isimImpi = getIsimImpi(MtkSubscriptionManager.getSubIdUsingPhoneId(i));
        log("[IR] IMPI : " + isimImpi);
        if (isimImpi == null || isimImpi.equals("")) {
            log("[IR] impi is null/empty, returning default mccmnc");
            return str;
        }
        int iIndexOf = isimImpi.indexOf("mcc");
        int iIndexOf2 = isimImpi.indexOf("mnc");
        if (iIndexOf == -1 || iIndexOf2 == -1) {
            log("[IR] mcc/mnc position -1, returning default mccmnc");
            return str;
        }
        String str2 = isimImpi.substring("mcc".length() + iIndexOf, iIndexOf + "mcc".length() + SIM_ID_4) + isimImpi.substring("mnc".length() + iIndexOf2, iIndexOf2 + "mnc".length() + SIM_ID_4);
        log("[IR] MccMnc fetched from IMPI: " + str2);
        if (str2 == null || str2.equals("")) {
            log("[IR] IMPI MccMnc is null/empty, Returning default mccmnc: " + str);
            return str;
        }
        for (String str3 : strArr) {
            if (str2.equals(str3)) {
                log("[IR] mccMnc matched, Returning mccmnc from IMPI: " + str2);
                return str2;
            }
        }
        log("[IR] IMPI mcc/mnc not matched, returning default mccmnc");
        return str;
    }

    private IMtkPhoneSubInfoEx getMtkSubscriberInfoEx() {
        return IMtkPhoneSubInfoEx.Stub.asInterface(ServiceManager.getService("iphonesubinfoEx"));
    }

    private String getIsimImpi(int i) {
        if (i == -1) {
            log("[IR] getIsimImpi: Invalid subId so return");
            return null;
        }
        try {
            return getMtkSubscriberInfoEx().getIsimImpiForSubscriber(i);
        } catch (RemoteException e) {
            return null;
        } catch (NullPointerException e2) {
            return null;
        }
    }

    private static String getMccMncForSubId(int i, SubscriptionManager subscriptionManager) {
        String simOperator = TelephonyManager.getDefault().getSimOperator(i);
        if (simOperator != null && simOperator.length() > 0) {
            log("[IR] Getting mcc mnc from TelephonyManager.getSimOperator");
            return simOperator;
        }
        List<SubscriptionInfo> activeSubscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
        log("[IR] Getting mcc mnc from from subinfo for subId = " + i);
        if (activeSubscriptionInfoList != null && activeSubscriptionInfoList.size() > 0) {
            for (SubscriptionInfo subscriptionInfo : activeSubscriptionInfoList) {
                if (subscriptionInfo.getSubscriptionId() == i) {
                    String str = String.valueOf(subscriptionInfo.getMcc()) + String.valueOf(subscriptionInfo.getMnc());
                    log("[IR] getMccMncForSubId from subInfo = " + str);
                    return str;
                }
            }
        }
        return simOperator;
    }

    private void hookProprietaryImsListener() throws ImsException {
        if (this.mMtkImsService == null) {
            log("hookProprietaryImsListener get NULL mMtkImsService so create it");
            createMtkImsService(DBG);
        }
        if (this.mListener == null) {
            log("[" + this.mPhoneId + "] hook proprietary IMS listener");
            this.mNotifyOnly = false;
            this.mListener = new IImsRegistrationListener.Stub() {
                public void registrationConnected() throws RemoteException {
                }

                public void registrationProgressing() throws RemoteException {
                }

                public void registrationConnectedWithRadioTech(int i) throws RemoteException {
                    MtkImsManager.log("registrationConnectedWithRadioTech :: imsRadioTech=" + i);
                    synchronized (MtkImsManager.this.mCallbacks) {
                        for (MtkImsConnectionStateListener mtkImsConnectionStateListener : MtkImsManager.this.mCallbacks) {
                            if (mtkImsConnectionStateListener instanceof MtkImsConnectionStateListener) {
                                mtkImsConnectionStateListener.onImsConnected(i);
                            }
                        }
                    }
                }

                public void registrationProgressingWithRadioTech(int i) throws RemoteException {
                }

                public void registrationDisconnected(ImsReasonInfo imsReasonInfo) throws RemoteException {
                    MtkImsManager.log("registrationDisconnected :: imsReasonInfo=" + imsReasonInfo);
                    synchronized (MtkImsManager.this.mCallbacks) {
                        for (MtkImsConnectionStateListener mtkImsConnectionStateListener : MtkImsManager.this.mCallbacks) {
                            if (mtkImsConnectionStateListener instanceof MtkImsConnectionStateListener) {
                                mtkImsConnectionStateListener.onImsDisconnected(imsReasonInfo);
                            }
                        }
                    }
                }

                public void registrationResumed() throws RemoteException {
                }

                public void registrationSuspended() throws RemoteException {
                }

                public void registrationServiceCapabilityChanged(int i, int i2) throws RemoteException {
                    MtkImsManager.this.notifyRegServiceCapabilityChangedEvent(i2);
                }

                public void registrationFeatureCapabilityChanged(int i, int[] iArr, int[] iArr2) throws RemoteException {
                    ImsFeature.Capabilities capabilitiesConvertCapabilities = MtkImsManager.this.convertCapabilities(iArr);
                    MtkImsManager.log("registrationFeatureCapabilityChanged :: enabledFeatures=" + capabilitiesConvertCapabilities);
                    synchronized (MtkImsManager.this.mCallbacks) {
                        for (MtkImsConnectionStateListener mtkImsConnectionStateListener : MtkImsManager.this.mCallbacks) {
                            if (mtkImsConnectionStateListener instanceof MtkImsConnectionStateListener) {
                                mtkImsConnectionStateListener.onCapabilitiesStatusChanged(capabilitiesConvertCapabilities);
                            }
                        }
                    }
                }

                public void voiceMessageCountUpdate(int i) throws RemoteException {
                }

                public void registrationAssociatedUriChanged(Uri[] uriArr) throws RemoteException {
                }

                public void registrationChangeFailed(int i, ImsReasonInfo imsReasonInfo) throws RemoteException {
                }
            };
        } else {
            log("mListener was created, mNotifyOnly " + this.mNotifyOnly);
        }
        try {
            if (this.mMtkImsService != null) {
                this.mMtkImsService.registerProprietaryImsListener(this.mPhoneId, this.mListener, this.mNotifyOnly);
                this.mNotifyOnly = DBG;
            } else {
                log("mMtkImsService is not ready yet");
            }
        } catch (RemoteException e) {
            throw new ImsException("registerProprietaryImsListener(listener)", e, 106);
        }
    }

    private MmTelFeature.MmTelCapabilities convertCapabilities(int[] iArr) {
        boolean[] zArr = new boolean[iArr.length];
        for (int i = 0; i <= 5 && i < iArr.length; i++) {
            if (iArr[i] == i) {
                zArr[i] = DBG;
            } else if (iArr[i] == -1) {
                zArr[i] = false;
            }
        }
        MmTelFeature.MmTelCapabilities mmTelCapabilities = new MmTelFeature.MmTelCapabilities();
        if (zArr[0] || zArr[2]) {
            mmTelCapabilities.addCapabilities(1);
        }
        if (zArr[1] || zArr[SIM_ID_4]) {
            mmTelCapabilities.addCapabilities(2);
        }
        if (zArr[4] || zArr[5]) {
            mmTelCapabilities.addCapabilities(4);
        }
        log("convertCapabilities - capabilities: " + mmTelCapabilities);
        return mmTelCapabilities;
    }

    public void addImsConnectionStateListener(ImsRegistrationImplBase.Callback callback) throws ImsException {
        synchronized (this.mCallbacks) {
            log("ImsConnectionStateListener added: " + callback);
            this.mCallbacks.add(callback);
            hookProprietaryImsListener();
        }
    }

    public void removeImsConnectionStateListener(ImsRegistrationImplBase.Callback callback) throws ImsException {
        synchronized (this.mCallbacks) {
            this.mCallbacks.remove(callback);
            log("ImsConnectionStateListener removed: " + callback + ", size: " + this.mCallbacks.size());
        }
    }

    private boolean isPhoneIdSupportIms(int i) {
        int i2 = SystemProperties.getInt(PROPERTY_IMS_SUPPORT, 0);
        int i3 = SystemProperties.getInt(MULTI_IMS_SUPPORT, 1);
        if (TelephonyManager.getDefault().getMultiSimConfiguration() != TelephonyManager.MultiSimVariants.TSTS) {
            return DBG;
        }
        if (i2 == 0 || !SubscriptionManager.isValidPhoneId(i)) {
            log("[" + i + "] isPhoneIdSupportIms, not support IMS");
            return false;
        }
        if (i3 == 1) {
            if (getMainCapabilityPhoneId(this.mContext) == i) {
                return DBG;
            }
        } else {
            int protocolStackId = MtkTelephonyManagerEx.getDefault().getProtocolStackId(i);
            log("isPhoneIdSupportIms(), mimsCount:" + i3 + ", phoneId:" + i + ", protocalStackId:" + protocolStackId + ", MainCapabilityPhoneId:" + getMainCapabilityPhoneId(this.mContext));
            if (protocolStackId <= i3) {
                return DBG;
            }
        }
        return false;
    }

    private boolean isCellularDataRoaming() {
        TelephonyManager telephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        if (telephonyManager == null) {
            loge("isCellularDataRoaming(): TelephonyManager null");
            return false;
        }
        MtkServiceState serviceStateForSubscriber = telephonyManager.getServiceStateForSubscriber(getSubId());
        if (serviceStateForSubscriber == null) {
            loge("isCellularDataRoaming(): ServiceState null");
            return false;
        }
        if (serviceStateForSubscriber instanceof MtkServiceState) {
            MtkServiceState mtkServiceState = serviceStateForSubscriber;
            int cellularDataRegState = mtkServiceState.getCellularDataRegState();
            boolean cellularDataRoaming = mtkServiceState.getCellularDataRoaming();
            log("isCellularDataRoaming(): regState = " + cellularDataRegState + ", isDataroaming = " + cellularDataRoaming);
            if (cellularDataRegState == 0 && cellularDataRoaming) {
                return DBG;
            }
        } else {
            loge("isCellularDataRoaming(): not MtkServiceState");
        }
        return false;
    }
}
