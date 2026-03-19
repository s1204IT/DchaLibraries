package com.android.ims;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.ims.ImsCall;
import com.android.ims.internal.IImsCallSession;
import com.android.ims.internal.IImsCallSessionListener;
import com.android.ims.internal.IImsConfig;
import com.android.ims.internal.IImsEcbm;
import com.android.ims.internal.IImsMultiEndpoint;
import com.android.ims.internal.IImsRegistrationListener;
import com.android.ims.internal.IImsService;
import com.android.ims.internal.IImsUt;
import com.android.ims.internal.ImsCallSession;
import com.android.internal.telephony.IPhoneSubInfo;
import com.mediatek.common.MPlugin;
import com.mediatek.common.ims.IImsManagerExt;
import com.mediatek.internal.telephony.ITelephonyEx;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;
import org.gsma.joyn.JoynServiceListener;
import org.gsma.joyn.capability.CapabilityService;
import org.gsma.joyn.chat.ChatService;
import org.gsma.joyn.contacts.ContactsService;
import org.gsma.joyn.ft.FileTransferService;
import org.gsma.joyn.gsh.GeolocSharingService;
import org.gsma.joyn.ish.ImageSharingService;
import org.gsma.joyn.vsh.VideoSharingService;

public class ImsManager {
    public static final String ACTION_IMS_INCOMING_CALL = "com.android.ims.IMS_INCOMING_CALL";
    public static final String ACTION_IMS_INCOMING_CALL_INDICATION = "com.android.ims.IMS_INCOMING_CALL_INDICATION";
    public static final String ACTION_IMS_RADIO_STATE_CHANGED = "com.android.ims.IMS_RADIO_STATE_CHANGED";
    public static final String ACTION_IMS_REGISTRATION_ERROR = "com.android.ims.REGISTRATION_ERROR";
    public static final String ACTION_IMS_RTP_INFO = "com.android.ims.IMS_RTP_INFO";
    public static final String ACTION_IMS_SERVICE_DEREGISTERED = "com.android.ims.IMS_SERVICE_DEREGISTERED";
    public static final String ACTION_IMS_SERVICE_DOWN = "com.android.ims.IMS_SERVICE_DOWN";
    public static final String ACTION_IMS_SERVICE_UP = "com.android.ims.IMS_SERVICE_UP";
    public static final String ACTION_IMS_STATE_CHANGED = "com.android.ims.IMS_STATE_CHANGED";
    private static final boolean DBG = true;
    public static final String EXTRA_CALL_ID = "android:imsCallID";
    public static final String EXTRA_CALL_MODE = "android:imsCallMode";
    public static final String EXTRA_DIAL_STRING = "android:imsDialString";
    public static final String EXTRA_IMS_DISABLE_CAP_KEY = "android:disablecap";
    public static final String EXTRA_IMS_ENABLE_CAP_KEY = "android:enablecap";
    public static final String EXTRA_IMS_RADIO_STATE = "android:imsRadioState";
    public static final String EXTRA_IMS_REG_ERROR_KEY = "android:regError";
    public static final String EXTRA_IMS_REG_STATE_KEY = "android:regState";
    public static final String EXTRA_IS_UNKNOWN_CALL = "android:isUnknown";
    public static final String EXTRA_PHONE_ID = "android:phone_id";
    public static final String EXTRA_RTP_NETWORK_ID = "android:rtpNetworkId";
    public static final String EXTRA_RTP_PDN_ID = "android:rtpPdnId";
    public static final String EXTRA_RTP_RECV_PKT_LOST = "android:rtpRecvPktLost";
    public static final String EXTRA_RTP_SEND_PKT_LOST = "android:rtpSendPktLost";
    public static final String EXTRA_RTP_TIMER = "android:rtpTimer";
    public static final String EXTRA_SEQ_NUM = "android:imsSeqNum";
    public static final String EXTRA_SERVICE_ID = "android:imsServiceId";
    public static final String EXTRA_USSD = "android:ussd";
    public static final String IMS_SERVICE = "ims";
    public static final int INCOMING_CALL_RESULT_CODE = 101;
    private static final String LTE_SUPPORT = "ro.boot.opt_lte_support";
    private static final String MULTI_IMS_SUPPORT = "ro.mtk_multiple_ims_support";
    public static final String PROPERTY_DBG_VOLTE_AVAIL_OVERRIDE = "persist.dbg.volte_avail_ovr";
    public static final int PROPERTY_DBG_VOLTE_AVAIL_OVERRIDE_DEFAULT = 0;
    public static final String PROPERTY_DBG_VT_AVAIL_OVERRIDE = "persist.dbg.vt_avail_ovr";
    public static final int PROPERTY_DBG_VT_AVAIL_OVERRIDE_DEFAULT = 0;
    public static final String PROPERTY_DBG_WFC_AVAIL_OVERRIDE = "persist.dbg.wfc_avail_ovr";
    public static final int PROPERTY_DBG_WFC_AVAIL_OVERRIDE_DEFAULT = 0;
    private static final String TAG = "ImsManager";
    private static final String TTY_MODE = "tty_mode";
    private static final String VILTE_SETTING = "vilte_setting";
    private static final String VOLTE_SETTING = "volte_setting";
    private static final String WFC_MODE_SETTING = "wfc_mode_setting";
    private static final String WFC_ROAMING_SETTING = "wfc_roaming_setting";
    private static final String WFC_SETTING = "wfc_setting";
    private CapabilityService mCapabilitiesApi;
    private ChatService mChatApi;
    private ContactsService mContactsApi;
    private Context mContext;
    private FileTransferService mFileTransferApi;
    private GeolocSharingService mGeolocSharingApi;
    private ImageSharingService mImageSharingApi;
    private ImsConfigListener mImsConfigListener;
    private int mPhoneId;
    private VideoSharingService mVideoSharingApi;
    private static HashMap<Integer, ImsManager> sImsManagerInstances = new HashMap<>();
    private static IImsManagerExt mImsManagerExt = null;
    private IImsService mImsService = null;
    private ImsServiceDeathRecipient mDeathRecipient = new ImsServiceDeathRecipient(this, null);
    private ImsUt mUt = null;
    private ImsConfig mConfig = null;
    private boolean mConfigUpdated = false;
    private ImsEcbm mEcbm = null;
    private ImsMultiEndpoint mMultiEndpoint = null;

    public static ImsManager getInstance(Context context, int phoneId) {
        log("getInstance() : phoneId" + phoneId);
        synchronized (sImsManagerInstances) {
            if (sImsManagerInstances.containsKey(Integer.valueOf(phoneId))) {
                return sImsManagerInstances.get(Integer.valueOf(phoneId));
            }
            ImsManager mgr = new ImsManager(context, phoneId);
            sImsManagerInstances.put(Integer.valueOf(phoneId), mgr);
            return mgr;
        }
    }

    public static boolean isEnhanced4gLteModeSettingEnabledByUser(Context context) {
        return isEnhanced4gLteModeSettingEnabledByUser(context, getMainCapabilityPhoneId(context));
    }

    public static boolean isEnhanced4gLteModeSettingEnabledByUser(Context context, int phoneId) {
        int enabled;
        if (SystemProperties.getInt(MULTI_IMS_SUPPORT, 1) == 1) {
            if (!getBooleanCarrierConfig(context, "editable_enhanced_4g_lte_bool")) {
                return true;
            }
            enabled = Settings.Global.getInt(context.getContentResolver(), "volte_vt_enabled", 1);
        } else {
            if (!getBooleanCarrierConfig(context, "editable_enhanced_4g_lte_bool", phoneId)) {
                return true;
            }
            enabled = getSettingValueByKey(context, VOLTE_SETTING, phoneId);
        }
        return enabled == 1;
    }

    public static void setEnhanced4gLteModeSetting(Context context, boolean enabled) {
        setEnhanced4gLteModeSetting(context, enabled, getMainCapabilityPhoneId(context));
    }

    public static void setEnhanced4gLteModeSetting(Context context, boolean enabled, int phoneId) {
        ImsManager imsManager;
        int value = enabled ? 1 : 0;
        if (SystemProperties.getInt(MULTI_IMS_SUPPORT, 1) == 1) {
            phoneId = getMainCapabilityPhoneId(context);
            Settings.Global.putInt(context.getContentResolver(), "volte_vt_enabled", value);
        } else {
            setSettingValueByKey(context, VOLTE_SETTING, value, phoneId);
        }
        if (!isNonTtyOrTtyOnVolteEnabled(context, phoneId) || (imsManager = getInstance(context, phoneId)) == null) {
            return;
        }
        try {
            imsManager.setAdvanced4GMode(enabled);
        } catch (ImsException e) {
        }
    }

    public static boolean isNonTtyOrTtyOnVolteEnabled(Context context) {
        return isNonTtyOrTtyOnVolteEnabled(context, getMainCapabilityPhoneId(context));
    }

    public static boolean isNonTtyOrTtyOnVolteEnabled(Context context, int phoneId) {
        return SystemProperties.getInt(MULTI_IMS_SUPPORT, 1) == 1 ? getBooleanCarrierConfig(context, "carrier_volte_tty_supported_bool") || Settings.Secure.getInt(context.getContentResolver(), "preferred_tty_mode", 0) == 0 : getBooleanCarrierConfig(context, "carrier_volte_tty_supported_bool", phoneId) || getSettingValueByKey(context, TTY_MODE, phoneId) == 0;
    }

    public static boolean isVolteEnabledByPlatform(Context context) {
        return isVolteEnabledByPlatform(context, getMainCapabilityPhoneId(context));
    }

    public static boolean isVolteEnabledByPlatform(Context context, int phoneId) {
        if (SystemProperties.getInt(PROPERTY_DBG_VOLTE_AVAIL_OVERRIDE, 0) == 1) {
            return true;
        }
        if (SystemProperties.getInt(MULTI_IMS_SUPPORT, 1) == 1) {
            phoneId = getMainCapabilityPhoneId(context);
        }
        boolean isResourceSupport = isImsResourceSupport(context, 0, phoneId);
        if (SystemProperties.getInt("persist.mtk_volte_support", 0) == 1 && SystemProperties.getInt(LTE_SUPPORT, 0) == 1 && isResourceSupport && getBooleanCarrierConfig(context, "carrier_volte_available_bool", phoneId) && isGbaValid(context, phoneId)) {
            return isFeatureEnabledByPlatformExt(context, 0);
        }
        return false;
    }

    public static boolean isVolteProvisionedOnDevice(Context context) {
        return isVolteProvisionedOnDevice(context, getMainCapabilityPhoneId(context));
    }

    public static boolean isVolteProvisionedOnDevice(Context context, int phoneId) {
        if (SystemProperties.getInt(MULTI_IMS_SUPPORT, 1) == 1) {
            phoneId = getMainCapabilityPhoneId(context);
        }
        if (!getBooleanCarrierConfig(context, "carrier_volte_provisioning_required_bool", phoneId)) {
            return true;
        }
        ImsManager mgr = getInstance(context, phoneId);
        if (mgr == null) {
            return false;
        }
        try {
            ImsConfig config = mgr.getConfigInterface();
            if (config == null) {
                return false;
            }
            boolean isProvisioned = config.getVolteProvisioned();
            return isProvisioned;
        } catch (ImsException e) {
            return false;
        }
    }

    public static boolean isVtEnabledByPlatform(Context context) {
        return isVtEnabledByPlatform(context, getMainCapabilityPhoneId(context));
    }

    public static boolean isVtEnabledByPlatform(Context context, int phoneId) {
        if (SystemProperties.getInt(PROPERTY_DBG_VT_AVAIL_OVERRIDE, 0) == 1) {
            return true;
        }
        if (SystemProperties.getInt(MULTI_IMS_SUPPORT, 1) == 1) {
            phoneId = getMainCapabilityPhoneId(context);
        }
        boolean isResourceSupport = isImsResourceSupport(context, 1, phoneId);
        if (SystemProperties.getInt("persist.mtk_vilte_support", 0) == 1 && SystemProperties.getInt(LTE_SUPPORT, 0) == 1 && isResourceSupport && getBooleanCarrierConfig(context, "carrier_vt_available_bool", phoneId) && isGbaValid(context, phoneId)) {
            return isFeatureEnabledByPlatformExt(context, 1);
        }
        return false;
    }

    public static boolean isVtEnabledByUser(Context context) {
        return isVtEnabledByUser(context, getMainCapabilityPhoneId(context));
    }

    public static boolean isVtEnabledByUser(Context context, int phoneId) {
        int enabled;
        if (SystemProperties.getInt(MULTI_IMS_SUPPORT, 1) == 1) {
            enabled = Settings.Global.getInt(context.getContentResolver(), "vt_ims_enabled", 1);
        } else {
            enabled = getSettingValueByKey(context, VILTE_SETTING, phoneId);
        }
        return enabled == 1;
    }

    public static void setVtSetting(Context context, boolean enabled) {
        setVtSetting(context, enabled, getMainCapabilityPhoneId(context));
    }

    public static void setVtSetting(Context context, boolean enabled, int phoneId) {
        int value = enabled ? 1 : 0;
        if (SystemProperties.getInt(MULTI_IMS_SUPPORT, 1) == 1) {
            phoneId = getMainCapabilityPhoneId(context);
            Settings.Global.putInt(context.getContentResolver(), "vt_ims_enabled", value);
        } else {
            setSettingValueByKey(context, VILTE_SETTING, value, phoneId);
        }
        ImsManager imsManager = getInstance(context, phoneId);
        if (imsManager == null) {
            return;
        }
        try {
            ImsConfig config = imsManager.getConfigInterface();
            config.setFeatureValue(1, 13, enabled ? 1 : 0, imsManager.mImsConfigListener);
            if (enabled) {
                imsManager.turnOnIms();
            } else if (getBooleanCarrierConfig(context, "carrier_allow_turnoff_ims_bool", phoneId) && (!isVolteEnabledByPlatform(context, phoneId) || !isEnhanced4gLteModeSettingEnabledByUser(context, phoneId))) {
                log("setVtSetting() : imsServiceAllowTurnOff -> turnOffIms");
                imsManager.turnOffIms();
            }
        } catch (ImsException e) {
            loge("setVtSetting(): " + e);
        }
    }

    public static boolean isWfcEnabledByUser(Context context) {
        return isWfcEnabledByUser(context, getMainCapabilityPhoneId(context));
    }

    public static boolean isWfcEnabledByUser(Context context, int phoneId) {
        int enabled;
        if (SystemProperties.getInt(MULTI_IMS_SUPPORT, 1) == 1) {
            phoneId = getMainCapabilityPhoneId(context);
        }
        int isDefaultWFCIMSEnabledByCarrier = getBooleanCarrierConfig(context, "carrier_default_wfc_ims_enabled_bool", phoneId) ? 1 : 0;
        log("isWfcEnabledByUser - phoneId=" + phoneId + " isDefaultWFCIMSEnabledByCarrier=" + isDefaultWFCIMSEnabledByCarrier);
        if (SystemProperties.getInt(MULTI_IMS_SUPPORT, 1) == 1) {
            enabled = Settings.Global.getInt(context.getContentResolver(), "wfc_ims_enabled", isDefaultWFCIMSEnabledByCarrier);
        } else {
            enabled = getSettingValueByKey(context, WFC_SETTING, phoneId);
        }
        return enabled == 1;
    }

    public static void setWfcSetting(Context context, boolean enabled) {
        setWfcSetting(context, enabled, getMainCapabilityPhoneId(context));
    }

    public static void setWfcSetting(Context context, boolean enabled, int phoneId) {
        int value = enabled ? 1 : 0;
        if (SystemProperties.getInt(MULTI_IMS_SUPPORT, 1) == 1) {
            phoneId = getMainCapabilityPhoneId(context);
            Settings.Global.putInt(context.getContentResolver(), "wfc_ims_enabled", value);
        } else {
            setSettingValueByKey(context, WFC_SETTING, value, phoneId);
        }
        ImsManager imsManager = getInstance(context, phoneId);
        if (imsManager == null) {
            return;
        }
        try {
            ImsConfig config = imsManager.getConfigInterface();
            config.setFeatureValue(2, 18, enabled ? 1 : 0, imsManager.mImsConfigListener);
            if (enabled) {
                imsManager.turnOnIms();
            } else if (getBooleanCarrierConfig(context, "carrier_allow_turnoff_ims_bool", phoneId) && (!isVolteEnabledByPlatform(context, phoneId) || !isEnhanced4gLteModeSettingEnabledByUser(context, phoneId))) {
                log("setWfcSetting() : imsServiceAllowTurnOff -> turnOffIms");
                imsManager.turnOffIms();
            }
            setWfcModeInternal(context, enabled ? getWfcMode(context) : 1, phoneId);
        } catch (ImsException e) {
            loge("setWfcSetting(): " + e);
        }
    }

    public static int getWfcMode(Context context) {
        return getWfcMode(context, getMainCapabilityPhoneId(context));
    }

    public static int getWfcMode(Context context, int phoneId) {
        int setting;
        if (SystemProperties.getInt(MULTI_IMS_SUPPORT, 1) == 1) {
            phoneId = getMainCapabilityPhoneId(context);
        }
        int defaultWFCIMSModeByCarrier = getIntCarrierConfig(context, "carrier_default_wfc_ims_mode_int", phoneId);
        log("getWfcMode - phoneId=" + phoneId + " defaultWFCIMSModeByCarrier=" + defaultWFCIMSModeByCarrier);
        if (SystemProperties.getInt(MULTI_IMS_SUPPORT, 1) == 1) {
            setting = Settings.Global.getInt(context.getContentResolver(), "wfc_ims_mode", defaultWFCIMSModeByCarrier);
        } else {
            setting = getSettingValueByKey(context, WFC_MODE_SETTING, phoneId);
        }
        log("getWfcMode() " + setting);
        return setting;
    }

    public static void setWfcMode(Context context, int wfcMode) {
        setWfcMode(context, wfcMode, getMainCapabilityPhoneId(context));
    }

    public static void setWfcMode(Context context, int wfcMode, int phoneId) {
        log("setWfcMode() " + wfcMode);
        if (SystemProperties.getInt(MULTI_IMS_SUPPORT, 1) == 1) {
            phoneId = getMainCapabilityPhoneId(context);
            Settings.Global.putInt(context.getContentResolver(), "wfc_ims_mode", wfcMode);
        } else {
            setSettingValueByKey(context, WFC_MODE_SETTING, wfcMode, phoneId);
        }
        setWfcModeInternal(context, wfcMode, phoneId);
    }

    private static void setWfcModeInternal(Context context, int wfcMode) {
        setWfcModeInternal(context, wfcMode, getMainCapabilityPhoneId(context));
    }

    private static void setWfcModeInternal(Context context, final int wfcMode, int phoneId) {
        if (SystemProperties.getInt(MULTI_IMS_SUPPORT, 1) == 1) {
            phoneId = getMainCapabilityPhoneId(context);
        }
        ImsManager imsManager = getInstance(context, phoneId);
        if (imsManager == null) {
            return;
        }
        try {
            imsManager.getConfigInterface().setWfcMode(wfcMode);
        } catch (ImsException e) {
        }
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ImsManager.this.getConfigInterface().setProvisionedValue(27, wfcMode);
                } catch (ImsException e2) {
                }
            }
        });
        thread.start();
    }

    public static boolean isWfcRoamingEnabledByUser(Context context) {
        return isWfcRoamingEnabledByUser(context, getMainCapabilityPhoneId(context));
    }

    public static boolean isWfcRoamingEnabledByUser(Context context, int phoneId) {
        int isRoamingEnableByCarrier;
        int enabled;
        if (SystemProperties.getInt(MULTI_IMS_SUPPORT, 1) == 1) {
            phoneId = getMainCapabilityPhoneId(context);
        }
        if (getBooleanCarrierConfig(context, "carrier_default_wfc_ims_roaming_enabled_bool", phoneId)) {
            isRoamingEnableByCarrier = 1;
        } else {
            isRoamingEnableByCarrier = 0;
        }
        log("isWfcRoamingEnabledByUser - phoneId=" + phoneId + " isRoamingEnableByCarrier=" + isRoamingEnableByCarrier);
        if (SystemProperties.getInt(MULTI_IMS_SUPPORT, 1) == 1) {
            enabled = Settings.Global.getInt(context.getContentResolver(), "wfc_ims_roaming_enabled", isRoamingEnableByCarrier);
        } else {
            enabled = getSettingValueByKey(context, WFC_ROAMING_SETTING, phoneId);
        }
        return enabled == 1;
    }

    public static void setWfcRoamingSetting(Context context, boolean enabled) {
        setWfcRoamingSetting(context, enabled, getMainCapabilityPhoneId(context));
    }

    public static void setWfcRoamingSetting(Context context, boolean enabled, int phoneId) {
        if (SystemProperties.getInt(MULTI_IMS_SUPPORT, 1) == 1) {
            Settings.Global.putInt(context.getContentResolver(), "wfc_ims_roaming_enabled", enabled ? 1 : 0);
        } else {
            int value = enabled ? 1 : 0;
            setSettingValueByKey(context, WFC_ROAMING_SETTING, value, phoneId);
        }
        setWfcRoamingSettingInternal(context, enabled, phoneId);
    }

    private static void setWfcRoamingSettingInternal(Context context, boolean enabled) {
        setWfcRoamingSettingInternal(context, enabled, getMainCapabilityPhoneId(context));
    }

    private static void setWfcRoamingSettingInternal(Context context, boolean enabled, int phoneId) {
        final int value;
        ImsManager imsManager = getInstance(context, phoneId);
        if (imsManager == null) {
            return;
        }
        if (enabled) {
            value = 1;
        } else {
            value = 0;
        }
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ImsManager.this.getConfigInterface().setProvisionedValue(26, value);
                } catch (ImsException e) {
                }
            }
        });
        thread.start();
    }

    public static boolean isWfcEnabledByPlatform(Context context) {
        return isWfcEnabledByPlatform(context, getMainCapabilityPhoneId(context));
    }

    public static boolean isWfcEnabledByPlatform(Context context, int phoneId) {
        if (SystemProperties.getInt(PROPERTY_DBG_WFC_AVAIL_OVERRIDE, 0) == 1) {
            return true;
        }
        if (SystemProperties.getInt(MULTI_IMS_SUPPORT, 1) == 1) {
            phoneId = getMainCapabilityPhoneId(context);
        }
        boolean isResourceSupport = isImsResourceSupport(context, 2, phoneId);
        if (SystemProperties.getInt("persist.mtk_wfc_support", 0) == 1 && SystemProperties.getInt(LTE_SUPPORT, 0) == 1 && isResourceSupport && getBooleanCarrierConfig(context, "carrier_wfc_ims_available_bool", phoneId) && isGbaValid(context, phoneId)) {
            return isFeatureEnabledByPlatformExt(context, 2);
        }
        return false;
    }

    private static boolean isGbaValid(Context context) {
        return isGbaValid(context, getMainCapabilityPhoneId(context));
    }

    private static boolean isGbaValid(Context context, int phoneId) {
        if (SystemProperties.getInt(MULTI_IMS_SUPPORT, 1) == 1) {
            phoneId = getMainCapabilityPhoneId(context);
        }
        int subId = SubscriptionManager.getSubIdUsingPhoneId(phoneId);
        if (!getBooleanCarrierConfig(context, "carrier_ims_gba_required_bool", phoneId)) {
            return true;
        }
        IPhoneSubInfo iSubInfo = getSubscriberInfo();
        String efIst = null;
        try {
            efIst = iSubInfo.getIsimIstForSubscriber(subId);
        } catch (RemoteException e) {
            loge("remote expcetion for getIsimIstForSubscriber");
        }
        if (efIst == null) {
            loge("ISF is NULL");
            return true;
        }
        boolean result = (efIst == null || efIst.length() <= 1 || (((byte) efIst.charAt(1)) & 2) == 0) ? false : true;
        log("GBA capable=" + result + ", ISF=" + efIst);
        return result;
    }

    public static void updateImsServiceConfig(Context context, int phoneId, boolean force) {
        if (!force && TelephonyManager.getDefault().getSimState() != 5) {
            log("updateImsServiceConfig: SIM not ready");
            return;
        }
        ImsManager imsManager = getInstance(context, phoneId);
        if (imsManager != null) {
            if (imsManager.mConfigUpdated && !force) {
                return;
            }
            log("updateImsServiceConfig() phoneId: " + phoneId + " force: " + force);
            try {
                boolean isImsUsed = imsManager.updateVolteFeatureValue();
                if ((isImsUsed | imsManager.updateVideoCallFeatureValue() | imsManager.updateWfcFeatureAndProvisionedValues()) || !getBooleanCarrierConfig(context, "carrier_allow_turnoff_ims_bool")) {
                    imsManager.turnOnIms();
                } else {
                    imsManager.turnOffIms();
                }
                imsManager.mConfigUpdated = true;
            } catch (ImsException e) {
                loge("updateImsServiceConfig: " + e);
                imsManager.mConfigUpdated = false;
            }
        }
    }

    private boolean updateVolteFeatureValue() throws ImsException {
        boolean available = isVolteEnabledByPlatform(this.mContext, this.mPhoneId);
        boolean enabled = isEnhanced4gLteModeSettingEnabledByUser(this.mContext, this.mPhoneId);
        boolean isNonTty = isNonTtyOrTtyOnVolteEnabled(this.mContext, this.mPhoneId);
        boolean z = (available && enabled) ? isNonTty : false;
        log("updateVolteFeatureValue: available = " + available + ", enabled = " + enabled + ", nonTTY = " + isNonTty);
        getConfigInterface().setFeatureValue(0, 13, z ? 1 : 0, this.mImsConfigListener);
        return z;
    }

    private boolean updateVideoCallFeatureValue() throws ImsException {
        boolean zIsVtEnabledByUser;
        boolean available = isVtEnabledByPlatform(this.mContext, this.mPhoneId);
        if (!isEnhanced4gLteModeSettingEnabledByUser(this.mContext, this.mPhoneId)) {
            zIsVtEnabledByUser = false;
        } else {
            zIsVtEnabledByUser = isVtEnabledByUser(this.mContext, this.mPhoneId);
        }
        boolean isNonTty = isNonTtyOrTtyOnVolteEnabled(this.mContext, this.mPhoneId);
        boolean z = (available && zIsVtEnabledByUser) ? isNonTty : false;
        log("updateVideoCallFeatureValue: available = " + available + ", enabled = " + zIsVtEnabledByUser + ", nonTTY = " + isNonTty);
        getConfigInterface().setFeatureValue(1, 13, z ? 1 : 0, this.mImsConfigListener);
        return z;
    }

    private boolean updateWfcFeatureAndProvisionedValues() throws ImsException {
        boolean available = isWfcEnabledByPlatform(this.mContext, this.mPhoneId);
        boolean enabled = isWfcEnabledByUser(this.mContext, this.mPhoneId);
        int mode = getWfcMode(this.mContext, this.mPhoneId);
        boolean roaming = isWfcRoamingEnabledByUser(this.mContext, this.mPhoneId);
        boolean z = available ? enabled : false;
        log("updateWfcFeatureAndProvisionedValues: available = " + available + ", enabled = " + enabled + ", mode = " + mode + ", roaming = " + roaming);
        getConfigInterface().setFeatureValue(2, 18, z ? 1 : 0, this.mImsConfigListener);
        if (!z) {
            mode = 1;
            roaming = false;
        }
        setWfcModeInternal(this.mContext, mode);
        setWfcRoamingSettingInternal(this.mContext, roaming);
        return z;
    }

    private ImsManager(Context context, int phoneId) {
        this.mContext = context;
        this.mPhoneId = phoneId;
        createImsService(true);
        createTerminalApiServices();
    }

    private static IPhoneSubInfo getSubscriberInfo() {
        return IPhoneSubInfo.Stub.asInterface(ServiceManager.getService("iphonesubinfo"));
    }

    private static int getSettingValueByKey(Context context, String key, int phoneId) {
        int isRoamingEnableByCarrier;
        int isDefaultWFCIMSEnabledByCarrier;
        if (key.equals(VOLTE_SETTING)) {
            if (phoneId == 0) {
                return Settings.Global.getInt(context.getContentResolver(), "volte_vt_enabled", 1);
            }
            if (phoneId == 1) {
                return Settings.Global.getInt(context.getContentResolver(), "volte_vt_enabled_sim2", 1);
            }
            if (phoneId == 2) {
                return Settings.Global.getInt(context.getContentResolver(), "volte_vt_enabled_sim3", 1);
            }
            if (phoneId == 3) {
                return Settings.Global.getInt(context.getContentResolver(), "volte_vt_enabled_sim4", 1);
            }
            return -1;
        }
        if (key.equals(TTY_MODE)) {
            if (phoneId == 0) {
                return Settings.Secure.getInt(context.getContentResolver(), "preferred_tty_mode", 0);
            }
            if (phoneId == 1) {
                return Settings.Secure.getInt(context.getContentResolver(), "preferred_tty_mode_sim2", 0);
            }
            if (phoneId == 2) {
                return Settings.Secure.getInt(context.getContentResolver(), "preferred_tty_mode_sim3", 0);
            }
            if (phoneId == 3) {
                return Settings.Secure.getInt(context.getContentResolver(), "preferred_tty_mode_sim4", 0);
            }
            return -1;
        }
        if (key.equals(VILTE_SETTING)) {
            if (phoneId == 0) {
                return Settings.Global.getInt(context.getContentResolver(), "vt_ims_enabled", 1);
            }
            if (phoneId == 1) {
                return Settings.Global.getInt(context.getContentResolver(), "vt_ims_enabled_sim2", 1);
            }
            if (phoneId == 2) {
                return Settings.Global.getInt(context.getContentResolver(), "vt_ims_enabled_sim3", 1);
            }
            if (phoneId == 3) {
                return Settings.Global.getInt(context.getContentResolver(), "vt_ims_enabled_sim4", 1);
            }
            return -1;
        }
        if (key.equals(WFC_SETTING)) {
            if (getBooleanCarrierConfig(context, "carrier_default_wfc_ims_enabled_bool", phoneId)) {
                isDefaultWFCIMSEnabledByCarrier = 1;
            } else {
                isDefaultWFCIMSEnabledByCarrier = 0;
            }
            if (phoneId == 0) {
                return Settings.Global.getInt(context.getContentResolver(), "wfc_ims_enabled", isDefaultWFCIMSEnabledByCarrier);
            }
            if (phoneId == 1) {
                return Settings.Global.getInt(context.getContentResolver(), "wfc_ims_enabled_sim2", isDefaultWFCIMSEnabledByCarrier);
            }
            if (phoneId == 2) {
                return Settings.Global.getInt(context.getContentResolver(), "wfc_ims_enabled_sim3", isDefaultWFCIMSEnabledByCarrier);
            }
            if (phoneId == 3) {
                return Settings.Global.getInt(context.getContentResolver(), "wfc_ims_enabled_sim4", isDefaultWFCIMSEnabledByCarrier);
            }
            return -1;
        }
        if (key.equals(WFC_MODE_SETTING)) {
            int defaultWFCIMSModeByCarrier = getIntCarrierConfig(context, "carrier_default_wfc_ims_mode_int", phoneId);
            if (phoneId == 0) {
                return Settings.Global.getInt(context.getContentResolver(), "wfc_ims_mode", defaultWFCIMSModeByCarrier);
            }
            if (phoneId == 1) {
                return Settings.Global.getInt(context.getContentResolver(), "wfc_ims_mode_sim2", defaultWFCIMSModeByCarrier);
            }
            if (phoneId == 2) {
                return Settings.Global.getInt(context.getContentResolver(), "wfc_ims_mode_sim3", defaultWFCIMSModeByCarrier);
            }
            if (phoneId == 3) {
                return Settings.Global.getInt(context.getContentResolver(), "wfc_ims_mode_sim4", defaultWFCIMSModeByCarrier);
            }
            return -1;
        }
        if (key.equals(WFC_ROAMING_SETTING)) {
            if (getBooleanCarrierConfig(context, "carrier_default_wfc_ims_roaming_enabled_bool", phoneId)) {
                isRoamingEnableByCarrier = 1;
            } else {
                isRoamingEnableByCarrier = 0;
            }
            if (phoneId == 0) {
                return Settings.Global.getInt(context.getContentResolver(), "wfc_ims_roaming_enabled", isRoamingEnableByCarrier);
            }
            if (phoneId == 1) {
                return Settings.Global.getInt(context.getContentResolver(), "wfc_ims_roaming_enabled_sim2", isRoamingEnableByCarrier);
            }
            if (phoneId == 2) {
                return Settings.Global.getInt(context.getContentResolver(), "wfc_ims_roaming_enabled_sim3", isRoamingEnableByCarrier);
            }
            if (phoneId == 3) {
                return Settings.Global.getInt(context.getContentResolver(), "wfc_ims_roaming_enabled_sim4", isRoamingEnableByCarrier);
            }
            return -1;
        }
        return -1;
    }

    private static void setSettingValueByKey(Context context, String key, int value, int phoneId) {
        if (key.equals(VOLTE_SETTING)) {
            if (phoneId == 0) {
                Settings.Global.putInt(context.getContentResolver(), "volte_vt_enabled", value);
                return;
            }
            if (phoneId == 1) {
                Settings.Global.putInt(context.getContentResolver(), "volte_vt_enabled_sim2", value);
                return;
            } else if (phoneId == 2) {
                Settings.Global.putInt(context.getContentResolver(), "volte_vt_enabled_sim3", value);
                return;
            } else {
                if (phoneId != 3) {
                    return;
                }
                Settings.Global.putInt(context.getContentResolver(), "volte_vt_enabled_sim4", value);
                return;
            }
        }
        if (key.equals(VILTE_SETTING)) {
            if (phoneId == 0) {
                Settings.Global.putInt(context.getContentResolver(), "vt_ims_enabled", value);
                return;
            }
            if (phoneId == 1) {
                Settings.Global.putInt(context.getContentResolver(), "vt_ims_enabled_sim2", value);
                return;
            } else if (phoneId == 2) {
                Settings.Global.putInt(context.getContentResolver(), "vt_ims_enabled_sim3", value);
                return;
            } else {
                if (phoneId != 3) {
                    return;
                }
                Settings.Global.putInt(context.getContentResolver(), "vt_ims_enabled_sim4", value);
                return;
            }
        }
        if (key.equals(WFC_SETTING)) {
            if (phoneId == 0) {
                Settings.Global.putInt(context.getContentResolver(), "wfc_ims_enabled", value);
                return;
            }
            if (phoneId == 1) {
                Settings.Global.putInt(context.getContentResolver(), "wfc_ims_enabled_sim2", value);
                return;
            } else if (phoneId == 2) {
                Settings.Global.putInt(context.getContentResolver(), "wfc_ims_enabled_sim3", value);
                return;
            } else {
                if (phoneId != 3) {
                    return;
                }
                Settings.Global.putInt(context.getContentResolver(), "wfc_ims_enabled_sim4", value);
                return;
            }
        }
        if (key.equals(WFC_MODE_SETTING)) {
            if (phoneId == 0) {
                Settings.Global.putInt(context.getContentResolver(), "wfc_ims_mode", value);
                return;
            }
            if (phoneId == 1) {
                Settings.Global.putInt(context.getContentResolver(), "wfc_ims_mode_sim2", value);
                return;
            } else if (phoneId == 2) {
                Settings.Global.putInt(context.getContentResolver(), "wfc_ims_mode_sim3", value);
                return;
            } else {
                if (phoneId != 3) {
                    return;
                }
                Settings.Global.putInt(context.getContentResolver(), "wfc_ims_mode_sim4", value);
                return;
            }
        }
        if (!key.equals(WFC_ROAMING_SETTING)) {
            return;
        }
        if (phoneId == 0) {
            Settings.Global.putInt(context.getContentResolver(), "wfc_ims_roaming_enabled", value);
            return;
        }
        if (phoneId == 1) {
            Settings.Global.putInt(context.getContentResolver(), "wfc_ims_roaming_enabled_sim2", value);
        } else if (phoneId == 2) {
            Settings.Global.putInt(context.getContentResolver(), "wfc_ims_roaming_enabled_sim3", value);
        } else {
            if (phoneId != 3) {
                return;
            }
            Settings.Global.putInt(context.getContentResolver(), "wfc_ims_roaming_enabled_sim4", value);
        }
    }

    private void createTerminalApiServices() {
        Log.d(TAG, "createTerminalApiServices entry");
        this.mCapabilitiesApi = new CapabilityService(this.mContext, new MyServiceListener());
        this.mCapabilitiesApi.connect();
        this.mChatApi = new ChatService(this.mContext, new MyServiceListener());
        this.mChatApi.connect();
        this.mContactsApi = new ContactsService(this.mContext, new MyServiceListener());
        this.mContactsApi.connect();
        this.mFileTransferApi = new FileTransferService(this.mContext, new MyServiceListener());
        this.mFileTransferApi.connect();
        this.mGeolocSharingApi = new GeolocSharingService(this.mContext, new MyServiceListener());
        this.mGeolocSharingApi.connect();
        this.mImageSharingApi = new ImageSharingService(this.mContext, new MyServiceListener());
        this.mImageSharingApi.connect();
        this.mVideoSharingApi = new VideoSharingService(this.mContext, new MyServiceListener());
        this.mVideoSharingApi.connect();
    }

    public CapabilityService getCapabilitiesService() {
        return this.mCapabilitiesApi;
    }

    public ChatService getChatService() {
        return this.mChatApi;
    }

    public FileTransferService getFileTransferService() {
        return this.mFileTransferApi;
    }

    public ContactsService getContactsService() {
        return this.mContactsApi;
    }

    public GeolocSharingService getGeolocSharingService() {
        return this.mGeolocSharingApi;
    }

    public ImageSharingService getImageSharingService() {
        return this.mImageSharingApi;
    }

    public VideoSharingService getVideoSharingService() {
        return this.mVideoSharingApi;
    }

    public class MyServiceListener implements JoynServiceListener {
        public MyServiceListener() {
        }

        @Override
        public void onServiceConnected() {
            Log.d(ImsManager.TAG, "onServiceConnected entry ");
        }

        @Override
        public void onServiceDisconnected(int error) {
            Log.d(ImsManager.TAG, "onServiceDisconnected entry " + error);
        }
    }

    public boolean isServiceAvailable() {
        if (this.mImsService != null) {
            return true;
        }
        IBinder binder = ServiceManager.checkService(getImsServiceName(this.mPhoneId));
        return binder != null;
    }

    public void setImsConfigListener(ImsConfigListener listener) {
        this.mImsConfigListener = listener;
    }

    public int open(int serviceClass, PendingIntent incomingCallPendingIntent, ImsConnectionStateListener listener) throws ImsException {
        checkAndThrowExceptionIfServiceUnavailable();
        log("open: phoneId=" + this.mPhoneId);
        if (incomingCallPendingIntent == null) {
            throw new NullPointerException("incomingCallPendingIntent can't be null");
        }
        if (listener == null) {
            throw new NullPointerException("listener can't be null");
        }
        try {
            int result = this.mImsService.open(this.mPhoneId, serviceClass, incomingCallPendingIntent, createRegistrationListenerProxy(serviceClass, listener));
            log("open: result=" + result);
            if (result <= 0) {
                throw new ImsException("open()", result * (-1));
            }
            return result;
        } catch (RemoteException e) {
            throw new ImsException("open()", e, 106);
        }
    }

    public void close(int i) throws ImsException {
        checkAndThrowExceptionIfServiceUnavailable();
        log("close");
        try {
            try {
                this.mImsService.close(i);
            } catch (RemoteException e) {
                throw new ImsException("close()", e, 106);
            }
        } finally {
            this.mUt = null;
            this.mConfig = null;
            this.mEcbm = null;
            this.mMultiEndpoint = null;
        }
    }

    public ImsUtInterface getSupplementaryServiceConfiguration(int serviceId) throws ImsException {
        if (this.mUt == null) {
            checkAndThrowExceptionIfServiceUnavailable();
            try {
                IImsUt iUt = this.mImsService.getUtInterface(serviceId);
                if (iUt == null) {
                    throw new ImsException("getSupplementaryServiceConfiguration()", 801);
                }
                this.mUt = new ImsUt(iUt);
            } catch (RemoteException e) {
                throw new ImsException("getSupplementaryServiceConfiguration()", e, 106);
            }
        }
        if (this.mUt != null) {
            this.mUt.updateListener();
        }
        return this.mUt;
    }

    public boolean isConnected(int serviceId, int serviceType, int callType) throws ImsException {
        checkAndThrowExceptionIfServiceUnavailable();
        try {
            return this.mImsService.isConnected(serviceId, serviceType, callType);
        } catch (RemoteException e) {
            throw new ImsException("isServiceConnected()", e, 106);
        }
    }

    public boolean isOpened(int serviceId) throws ImsException {
        checkAndThrowExceptionIfServiceUnavailable();
        try {
            return this.mImsService.isOpened(serviceId);
        } catch (RemoteException e) {
            throw new ImsException("isOpened()", e, 106);
        }
    }

    public ImsCallProfile createCallProfile(int serviceId, int serviceType, int callType) throws ImsException {
        checkAndThrowExceptionIfServiceUnavailable();
        try {
            return this.mImsService.createCallProfile(serviceId, serviceType, callType);
        } catch (RemoteException e) {
            throw new ImsException("createCallProfile()", e, 106);
        }
    }

    public ImsCall makeCall(int serviceId, ImsCallProfile profile, String[] callees, ImsCall.Listener listener) throws ImsException {
        log("makeCall :: serviceId=" + serviceId + ", profile=" + profile + ", callees=" + callees);
        checkAndThrowExceptionIfServiceUnavailable();
        ImsCall call = new ImsCall(this.mContext, profile);
        call.setListener(listener);
        ImsCallSession session = createCallSession(serviceId, profile);
        if (callees != null && callees.length == 1 && !profile.getCallExtraBoolean("conference")) {
            call.start(session, callees[0]);
        } else {
            call.start(session, callees);
        }
        return call;
    }

    public ImsCall takeCall(int serviceId, Intent incomingCallIntent, ImsCall.Listener listener) throws ImsException {
        log("takeCall :: serviceId=" + serviceId + ", incomingCall=" + incomingCallIntent);
        checkAndThrowExceptionIfServiceUnavailable();
        if (incomingCallIntent == null) {
            throw new ImsException("Can't retrieve session with null intent", INCOMING_CALL_RESULT_CODE);
        }
        int incomingServiceId = getServiceId(incomingCallIntent);
        if (serviceId != incomingServiceId) {
            throw new ImsException("Service id is mismatched in the incoming call intent", INCOMING_CALL_RESULT_CODE);
        }
        String callId = getCallId(incomingCallIntent);
        if (callId == null) {
            throw new ImsException("Call ID missing in the incoming call intent", INCOMING_CALL_RESULT_CODE);
        }
        try {
            IImsCallSession session = this.mImsService.getPendingCallSession(serviceId, callId);
            if (session == null) {
                throw new ImsException("No pending session for the call", 107);
            }
            ImsCall call = new ImsCall(this.mContext, session.getCallProfile());
            call.attachSession(new ImsCallSession(session));
            call.setListener(listener);
            return call;
        } catch (Throwable t) {
            throw new ImsException("takeCall()", t, 0);
        }
    }

    public ImsConfig getConfigInterface() throws ImsException {
        checkAndThrowExceptionIfServiceUnavailable();
        try {
            IImsConfig config = this.mImsService.getConfigInterface(this.mPhoneId);
            if (config == null) {
                throw new ImsException("getConfigInterface()", 131);
            }
            this.mConfig = new ImsConfig(config, this.mContext);
            log("getConfigInterface(), mConfig= " + this.mConfig);
            return this.mConfig;
        } catch (RemoteException e) {
            throw new ImsException("getConfigInterface()", e, 106);
        }
    }

    public void setUiTTYMode(Context context, int serviceId, int uiTtyMode, Message onComplete) throws ImsException {
        checkAndThrowExceptionIfServiceUnavailable();
        int phoneId = serviceId - 1;
        try {
            this.mImsService.setUiTTYMode(serviceId, uiTtyMode, onComplete);
            if (getBooleanCarrierConfig(context, "carrier_volte_tty_supported_bool", phoneId)) {
                return;
            }
            log("TTY over VoLTE not supported, ttyMode=" + uiTtyMode);
            setAdvanced4GMode(uiTtyMode == 0 ? isEnhanced4gLteModeSettingEnabledByUser(context, phoneId) : false);
        } catch (RemoteException e) {
            throw new ImsException("setTTYMode()", e, 106);
        }
    }

    private static boolean getBooleanCarrierConfig(Context context, String key) {
        return getBooleanCarrierConfig(context, key, getMainCapabilityPhoneId(context));
    }

    private static boolean getBooleanCarrierConfig(Context context, String key, int phoneId) {
        CarrierConfigManager configManager = (CarrierConfigManager) context.getSystemService("carrier_config");
        int subId = SubscriptionManager.getSubIdUsingPhoneId(phoneId);
        log("getBooleanCarrierConfig: phoneId=" + phoneId + " subId=" + subId);
        PersistableBundle b = null;
        if (configManager != null) {
            b = configManager.getConfigForSubId(subId);
        }
        if (b != null) {
            return b.getBoolean(key);
        }
        return CarrierConfigManager.getDefaultConfig().getBoolean(key);
    }

    private static int getIntCarrierConfig(Context context, String key) {
        return getIntCarrierConfig(context, key, getMainCapabilityPhoneId(context));
    }

    private static int getIntCarrierConfig(Context context, String key, int phoneId) {
        CarrierConfigManager configManager = (CarrierConfigManager) context.getSystemService("carrier_config");
        PersistableBundle b = null;
        int subId = SubscriptionManager.getSubIdUsingPhoneId(phoneId);
        log("getIntCarrierConfig: phoneId=" + phoneId + " subId=" + subId);
        if (configManager != null) {
            b = configManager.getConfigForSubId(subId);
        }
        if (b != null) {
            return b.getInt(key);
        }
        return CarrierConfigManager.getDefaultConfig().getInt(key);
    }

    private static String getCallId(Intent incomingCallIntent) {
        if (incomingCallIntent == null) {
            return null;
        }
        return incomingCallIntent.getStringExtra(EXTRA_CALL_ID);
    }

    private static String getCallNum(Intent incomingCallIntent) {
        if (incomingCallIntent == null) {
            return null;
        }
        return incomingCallIntent.getStringExtra(EXTRA_DIAL_STRING);
    }

    private static int getSeqNum(Intent incomingCallIntent) {
        if (incomingCallIntent == null) {
            return -1;
        }
        return incomingCallIntent.getIntExtra(EXTRA_SEQ_NUM, -1);
    }

    private static int getServiceId(Intent incomingCallIntent) {
        if (incomingCallIntent == null) {
            return -1;
        }
        return incomingCallIntent.getIntExtra(EXTRA_SERVICE_ID, -1);
    }

    private void checkAndThrowExceptionIfServiceUnavailable() throws ImsException {
        if (this.mImsService != null) {
            return;
        }
        createImsService(true);
        if (this.mImsService != null) {
        } else {
            throw new ImsException("Service is unavailable", 106);
        }
    }

    private static String getImsServiceName(int phoneId) {
        return IMS_SERVICE;
    }

    private void createImsService(boolean checkService) {
        if (checkService) {
            IBinder binder = ServiceManager.checkService(getImsServiceName(this.mPhoneId));
            if (binder == null) {
                log("createImsService binder is null");
                return;
            }
        }
        IBinder b = ServiceManager.getService(getImsServiceName(this.mPhoneId));
        if (b != null) {
            try {
                b.linkToDeath(this.mDeathRecipient, 0);
            } catch (RemoteException e) {
            }
        }
        this.mImsService = IImsService.Stub.asInterface(b);
        log("mImsService = " + this.mImsService);
    }

    private ImsCallSession createCallSession(int serviceId, ImsCallProfile profile) throws ImsException {
        try {
            return new ImsCallSession(this.mImsService.createCallSession(serviceId, profile, (IImsCallSessionListener) null));
        } catch (RemoteException e) {
            return null;
        }
    }

    private ImsRegistrationListenerProxy createRegistrationListenerProxy(int serviceClass, ImsConnectionStateListener listener) {
        ImsRegistrationListenerProxy proxy = new ImsRegistrationListenerProxy(serviceClass, listener);
        return proxy;
    }

    private static void log(String s) {
        Rlog.d(TAG, s);
    }

    private static void logw(String s) {
        Rlog.w(TAG, s);
    }

    private static void loge(String s) {
        Rlog.e(TAG, s);
    }

    private static void loge(String s, Throwable t) {
        Rlog.e(TAG, s, t);
    }

    private void turnOnIms() throws ImsException {
        checkAndThrowExceptionIfServiceUnavailable();
        try {
            this.mImsService.turnOnIms(this.mPhoneId);
        } catch (RemoteException e) {
            throw new ImsException("turnOnIms() ", e, 106);
        }
    }

    private boolean isImsTurnOffAllowed() {
        log("CarrierConfig:" + getBooleanCarrierConfig(this.mContext, "carrier_allow_turnoff_ims_bool", this.mPhoneId) + " wfcendablebyplateform:" + isWfcEnabledByPlatform(this.mContext) + " wfcenablebyUser:" + isWfcEnabledByUser(this.mContext, this.mPhoneId));
        if (getBooleanCarrierConfig(this.mContext, "carrier_allow_turnoff_ims_bool", this.mPhoneId)) {
            return (isWfcEnabledByPlatform(this.mContext) && isWfcEnabledByUser(this.mContext, this.mPhoneId)) ? false : true;
        }
        return false;
    }

    private void setAdvanced4GMode(boolean turnOn) throws ImsException {
        checkAndThrowExceptionIfServiceUnavailable();
        try {
            ImsConfig config = getConfigInterface();
            if (config != null) {
                config.setFeatureValue(0, 13, turnOn ? 1 : 0, this.mImsConfigListener);
                if (isVtEnabledByPlatform(this.mContext)) {
                    config.setFeatureValue(1, 13, turnOn ? isVtEnabledByUser(this.mContext, this.mPhoneId) : false ? 1 : 0, this.mImsConfigListener);
                }
            }
        } catch (ImsException e) {
            log("setAdvanced4GMode() : " + e);
        }
        log("setAdvanced4GMode():" + turnOn);
        if (turnOn) {
            turnOnIms();
        } else {
            if (!isImsTurnOffAllowed()) {
                return;
            }
            log("setAdvanced4GMode() : imsServiceAllowTurnOff -> turnOffIms");
            turnOffIms();
        }
    }

    private void turnOffIms() throws ImsException {
        checkAndThrowExceptionIfServiceUnavailable();
        try {
            this.mImsService.turnOffIms(this.mPhoneId);
        } catch (RemoteException e) {
            throw new ImsException("turnOffIms() ", e, 106);
        }
    }

    private class ImsServiceDeathRecipient implements IBinder.DeathRecipient {
        ImsServiceDeathRecipient(ImsManager this$0, ImsServiceDeathRecipient imsServiceDeathRecipient) {
            this();
        }

        private ImsServiceDeathRecipient() {
        }

        @Override
        public void binderDied() {
            ImsManager.this.mImsService = null;
            ImsManager.this.mUt = null;
            ImsManager.this.mConfig = null;
            ImsManager.this.mEcbm = null;
            ImsManager.this.mMultiEndpoint = null;
            if (ImsManager.this.mContext == null) {
                return;
            }
            Intent intent = new Intent(ImsManager.ACTION_IMS_SERVICE_DOWN);
            intent.putExtra(ImsManager.EXTRA_PHONE_ID, ImsManager.this.mPhoneId);
            ImsManager.this.mContext.sendBroadcast(new Intent(intent));
        }
    }

    private class ImsRegistrationListenerProxy extends IImsRegistrationListener.Stub {
        private ImsConnectionStateListener mListener;
        private int mServiceClass;

        public ImsRegistrationListenerProxy(int serviceClass, ImsConnectionStateListener listener) {
            this.mServiceClass = serviceClass;
            this.mListener = listener;
        }

        public boolean isSameProxy(int serviceClass) {
            return this.mServiceClass == serviceClass;
        }

        @Deprecated
        public void registrationConnected() {
            ImsManager.log("registrationConnected ::");
            if (this.mListener == null) {
                return;
            }
            this.mListener.onImsConnected();
        }

        @Deprecated
        public void registrationProgressing() {
            ImsManager.log("registrationProgressing ::");
            if (this.mListener == null) {
                return;
            }
            this.mListener.onImsProgressing();
        }

        public void registrationConnectedWithRadioTech(int imsRadioTech) {
            ImsManager.log("registrationConnectedWithRadioTech :: imsRadioTech=" + imsRadioTech);
            if (this.mListener == null) {
                return;
            }
            this.mListener.onImsConnected();
        }

        public void registrationProgressingWithRadioTech(int imsRadioTech) {
            ImsManager.log("registrationProgressingWithRadioTech :: imsRadioTech=" + imsRadioTech);
            if (this.mListener == null) {
                return;
            }
            this.mListener.onImsProgressing();
        }

        public void registrationDisconnected(ImsReasonInfo imsReasonInfo) {
            ImsManager.log("registrationDisconnected :: imsReasonInfo" + imsReasonInfo);
            if (this.mListener == null) {
                return;
            }
            this.mListener.onImsDisconnected(imsReasonInfo);
        }

        public void registrationResumed() {
            ImsManager.log("registrationResumed ::");
            if (this.mListener == null) {
                return;
            }
            this.mListener.onImsResumed();
        }

        public void registrationSuspended() {
            ImsManager.log("registrationSuspended ::");
            if (this.mListener == null) {
                return;
            }
            this.mListener.onImsSuspended();
        }

        public void registrationServiceCapabilityChanged(int serviceClass, int event) {
            ImsManager.log("registrationServiceCapabilityChanged :: serviceClass=" + serviceClass + ", event=" + event);
            if (this.mListener == null) {
                return;
            }
            this.mListener.onImsConnected();
        }

        public void registrationFeatureCapabilityChanged(int serviceClass, int[] enabledFeatures, int[] disabledFeatures) {
            ImsManager.log("registrationFeatureCapabilityChanged :: serviceClass=" + serviceClass);
            if (this.mListener == null) {
                return;
            }
            this.mListener.onFeatureCapabilityChanged(serviceClass, enabledFeatures, disabledFeatures);
        }

        public void voiceMessageCountUpdate(int count) {
            ImsManager.log("voiceMessageCountUpdate :: count=" + count);
            if (this.mListener == null) {
                return;
            }
            this.mListener.onVoiceMessageCountChanged(count);
        }

        public void registrationAssociatedUriChanged(Uri[] uris) {
            ImsManager.log("registrationAssociatedUriChanged ::");
            if (this.mListener == null) {
                return;
            }
            this.mListener.registrationAssociatedUriChanged(uris);
        }
    }

    public ImsEcbm getEcbmInterface(int serviceId) throws ImsException {
        if (this.mEcbm == null) {
            checkAndThrowExceptionIfServiceUnavailable();
            try {
                IImsEcbm iEcbm = this.mImsService.getEcbmInterface(serviceId);
                if (iEcbm == null) {
                    throw new ImsException("getEcbmInterface()", 901);
                }
                this.mEcbm = new ImsEcbm(iEcbm);
            } catch (RemoteException e) {
                throw new ImsException("getEcbmInterface()", e, 106);
            }
        }
        return this.mEcbm;
    }

    public ImsMultiEndpoint getMultiEndpointInterface(int serviceId) throws ImsException {
        if (this.mMultiEndpoint == null) {
            checkAndThrowExceptionIfServiceUnavailable();
            try {
                IImsMultiEndpoint iImsMultiEndpoint = this.mImsService.getMultiEndpointInterface(serviceId);
                if (iImsMultiEndpoint == null) {
                    throw new ImsException("getMultiEndpointInterface()", 902);
                }
                this.mMultiEndpoint = new ImsMultiEndpoint(iImsMultiEndpoint);
            } catch (RemoteException e) {
                throw new ImsException("getMultiEndpointInterface()", e, 106);
            }
        }
        return this.mMultiEndpoint;
    }

    public static void factoryReset(Context context) {
        if (SystemProperties.getInt(MULTI_IMS_SUPPORT, 1) == 1) {
            Settings.Global.putInt(context.getContentResolver(), "volte_vt_enabled", 1);
            Settings.Global.putInt(context.getContentResolver(), "wfc_ims_enabled", getBooleanCarrierConfig(context, "carrier_default_wfc_ims_enabled_bool") ? 1 : 0);
            Settings.Global.putInt(context.getContentResolver(), "wfc_ims_mode", getIntCarrierConfig(context, "carrier_default_wfc_ims_mode_int"));
            Settings.Global.putInt(context.getContentResolver(), "wfc_ims_roaming_enabled", getBooleanCarrierConfig(context, "carrier_default_wfc_ims_roaming_enabled_bool") ? 1 : 0);
            Settings.Global.putInt(context.getContentResolver(), "vt_ims_enabled", 1);
            updateImsServiceConfig(context, getMainCapabilityPhoneId(context), true);
            return;
        }
        Settings.Global.putInt(context.getContentResolver(), "volte_vt_enabled", 1);
        Settings.Global.putInt(context.getContentResolver(), "volte_vt_enabled_sim2", 1);
        Settings.Global.putInt(context.getContentResolver(), "volte_vt_enabled_sim3", 1);
        Settings.Global.putInt(context.getContentResolver(), "volte_vt_enabled_sim4", 1);
        Settings.Global.putInt(context.getContentResolver(), "wfc_ims_enabled", getBooleanCarrierConfig(context, "carrier_default_wfc_ims_enabled_bool", 0) ? 1 : 0);
        Settings.Global.putInt(context.getContentResolver(), "wfc_ims_enabled_sim2", getBooleanCarrierConfig(context, "carrier_default_wfc_ims_enabled_bool", 1) ? 1 : 0);
        Settings.Global.putInt(context.getContentResolver(), "wfc_ims_enabled_sim3", getBooleanCarrierConfig(context, "carrier_default_wfc_ims_enabled_bool", 2) ? 1 : 0);
        Settings.Global.putInt(context.getContentResolver(), "wfc_ims_enabled_sim4", getBooleanCarrierConfig(context, "carrier_default_wfc_ims_enabled_bool", 3) ? 1 : 0);
        Settings.Global.putInt(context.getContentResolver(), "wfc_ims_mode", getIntCarrierConfig(context, "carrier_default_wfc_ims_mode_int", 0));
        Settings.Global.putInt(context.getContentResolver(), "wfc_ims_mode_sim2", getIntCarrierConfig(context, "carrier_default_wfc_ims_mode_int", 1));
        Settings.Global.putInt(context.getContentResolver(), "wfc_ims_mode_sim3", getIntCarrierConfig(context, "carrier_default_wfc_ims_mode_int", 2));
        Settings.Global.putInt(context.getContentResolver(), "wfc_ims_mode_sim4", getIntCarrierConfig(context, "carrier_default_wfc_ims_mode_int", 3));
        Settings.Global.putInt(context.getContentResolver(), "wfc_ims_roaming_enabled", getBooleanCarrierConfig(context, "carrier_default_wfc_ims_roaming_enabled_bool", 0) ? 1 : 0);
        Settings.Global.putInt(context.getContentResolver(), "wfc_ims_roaming_enabled_sim2", getBooleanCarrierConfig(context, "carrier_default_wfc_ims_roaming_enabled_bool", 1) ? 1 : 0);
        Settings.Global.putInt(context.getContentResolver(), "wfc_ims_roaming_enabled_sim3", getBooleanCarrierConfig(context, "carrier_default_wfc_ims_roaming_enabled_bool", 2) ? 1 : 0);
        Settings.Global.putInt(context.getContentResolver(), "wfc_ims_roaming_enabled_sim4", getBooleanCarrierConfig(context, "carrier_default_wfc_ims_roaming_enabled_bool", 3) ? 1 : 0);
        Settings.Global.putInt(context.getContentResolver(), "vt_ims_enabled", 1);
        Settings.Global.putInt(context.getContentResolver(), "vt_ims_enabled_sim2", 1);
        Settings.Global.putInt(context.getContentResolver(), "vt_ims_enabled_sim3", 1);
        Settings.Global.putInt(context.getContentResolver(), "vt_ims_enabled_sim4", 1);
        int numPhones = TelephonyManager.getDefault().getPhoneCount();
        for (int i = 0; i < numPhones; i++) {
            updateImsServiceConfig(context, i + 0, true);
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("ImsManager:");
        pw.println("  mPhoneId = " + this.mPhoneId);
        pw.println("  mConfigUpdated = " + this.mConfigUpdated);
        pw.println("  mImsService = " + this.mImsService);
        pw.println("  isGbaValid = " + isGbaValid(this.mContext));
        pw.println("  isImsTurnOffAllowed = " + isImsTurnOffAllowed());
        pw.println("  isNonTtyOrTtyOnVolteEnabled = " + isNonTtyOrTtyOnVolteEnabled(this.mContext));
        pw.println("  isVolteEnabledByPlatform = " + isVolteEnabledByPlatform(this.mContext));
        pw.println("  isVolteProvisionedOnDevice = " + isVolteProvisionedOnDevice(this.mContext));
        pw.println("  isEnhanced4gLteModeSettingEnabledByUser = " + isEnhanced4gLteModeSettingEnabledByUser(this.mContext));
        pw.println("  isVtEnabledByPlatform = " + isVtEnabledByPlatform(this.mContext));
        pw.println("  isVtEnabledByUser = " + isVtEnabledByUser(this.mContext));
        pw.println("  isWfcEnabledByPlatform = " + isWfcEnabledByPlatform(this.mContext));
        pw.println("  isWfcEnabledByUser = " + isWfcEnabledByUser(this.mContext));
        pw.println("  getWfcMode = " + getWfcMode(this.mContext));
        pw.println("  isWfcRoamingEnabledByUser = " + isWfcRoamingEnabledByUser(this.mContext));
        pw.flush();
    }

    public void setCallIndication(int serviceId, Intent incomingCallIndication, boolean isAllow) throws ImsException {
        log("setCallIndication :: serviceId=" + serviceId + ", incomingCallIndication=" + incomingCallIndication);
        checkAndThrowExceptionIfServiceUnavailable();
        if (incomingCallIndication == null) {
            throw new ImsException("Can't retrieve session with null intent", INCOMING_CALL_RESULT_CODE);
        }
        int incomingServiceId = getServiceId(incomingCallIndication);
        if (serviceId != incomingServiceId) {
            throw new ImsException("Service id is mismatched in the incoming call intent", INCOMING_CALL_RESULT_CODE);
        }
        String callId = getCallId(incomingCallIndication);
        if (callId == null) {
            throw new ImsException("Call ID missing in the incoming call intent", INCOMING_CALL_RESULT_CODE);
        }
        String callNum = getCallNum(incomingCallIndication);
        if (callNum == null) {
            throw new ImsException("Call Num missing in the incoming call intent", INCOMING_CALL_RESULT_CODE);
        }
        int seqNum = getSeqNum(incomingCallIndication);
        if (seqNum == -1) {
            throw new ImsException("seqNum missing in the incoming call intent", INCOMING_CALL_RESULT_CODE);
        }
        try {
            this.mImsService.setCallIndication(serviceId, callId, callNum, seqNum, isAllow);
        } catch (RemoteException e) {
            throw new ImsException("setCallIndication()", e, 106);
        }
    }

    public int getImsState() throws ImsException {
        checkAndThrowExceptionIfServiceUnavailable();
        try {
            int imsState = this.mImsService.getImsState(this.mPhoneId);
            return imsState;
        } catch (RemoteException e) {
            throw new ImsException("getImsState()", e, 106);
        }
    }

    public boolean getImsRegInfo() throws ImsException {
        checkAndThrowExceptionIfServiceUnavailable();
        try {
            boolean isImsReg = this.mImsService.getImsRegInfo(this.mPhoneId);
            return isImsReg;
        } catch (RemoteException e) {
            throw new ImsException("getImsRegInfo", e, 106);
        }
    }

    public String getImsExtInfo() throws ImsException {
        checkAndThrowExceptionIfServiceUnavailable();
        try {
            String imsExtInfo = this.mImsService.getImsExtInfo(this.mPhoneId);
            return imsExtInfo;
        } catch (RemoteException e) {
            throw new ImsException("getImsExtInfo()", e, 106);
        }
    }

    public void hangupAllCall() throws ImsException {
        checkAndThrowExceptionIfServiceUnavailable();
        try {
            this.mImsService.hangupAllCall();
        } catch (RemoteException e) {
            throw new ImsException("hangupAll()", e, 106);
        }
    }

    public int getWfcStatusCode() {
        if (this.mImsService == null) {
            return 100;
        }
        try {
            return this.mImsService.getRegistrationStatus();
        } catch (RemoteException e) {
            return 100;
        }
    }

    private static boolean isImsResourceSupport(Context context, int feature, int phoneId) {
        boolean support = true;
        if ("1".equals(SystemProperties.get("persist.mtk_dynamic_ims_switch"))) {
            if (!SubscriptionManager.isValidPhoneId(phoneId)) {
                loge("Invalid main phone " + phoneId + ", return true as don't care");
                return true;
            }
            try {
                ImsConfig config = getConfigInterface(phoneId, context);
                if (config != null) {
                    support = config.getImsResCapability(feature) == 1;
                }
            } catch (ImsException e) {
                loge("isImsResourceSupport() failed!" + e);
            }
            log("isImsResourceSupport(" + feature + ") return " + support + " on phone: " + phoneId);
        }
        return support;
    }

    private static ImsConfig getConfigInterface(int phoneId, Context context) throws ImsException {
        try {
            IBinder b = ServiceManager.getService(getImsServiceName(phoneId));
            if (b == null) {
                throw new ImsException("getConfigInterface(), ImsService binder", 131);
            }
            IImsService service = IImsService.Stub.asInterface(b);
            IImsConfig binder = service.getConfigInterface(phoneId);
            if (binder == null) {
                throw new ImsException("getConfigInterface()", 131);
            }
            ImsConfig config = new ImsConfig(binder, context);
            return config;
        } catch (RemoteException e) {
            throw new ImsException("getConfigInterface()", e, 106);
        }
    }

    private static int getMainCapabilityPhoneId(Context context) {
        ITelephonyEx telephony = ITelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"));
        if (telephony != null) {
            try {
                return telephony.getMainCapabilityPhoneId();
            } catch (RemoteException e) {
                loge("getMainCapabilityPhoneId: remote exception");
                return -1;
            }
        }
        loge("ITelephonyEx service not ready!");
        int phoneId = SystemProperties.getInt("persist.radio.simswitch", 1) - 1;
        if (phoneId < 0 || phoneId >= TelephonyManager.getDefault().getPhoneCount()) {
            phoneId = -1;
        }
        Rlog.d(TAG, "getMainCapabilityPhoneId: phoneId = " + phoneId);
        return phoneId;
    }

    private static boolean isFeatureEnabledByPlatformExt(Context context, int feature) {
        if (context == null) {
            logw("Invalid context, return true");
            return true;
        }
        if (mImsManagerExt == null) {
            mImsManagerExt = (IImsManagerExt) MPlugin.createInstance(IImsManagerExt.class.getName(), context);
            if (mImsManagerExt == null) {
                logw("Unable to create imsManagerPlugin, return true");
                return true;
            }
        }
        boolean isEnabled = mImsManagerExt.isFeatureEnabledByPlatform(feature);
        log("isFeatureEnabledByPlatformExt(), feature:" + feature + ", isEnabled:" + isEnabled);
        return isEnabled;
    }
}
