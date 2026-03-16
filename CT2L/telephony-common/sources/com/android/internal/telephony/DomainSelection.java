package com.android.internal.telephony;

import android.R;
import android.content.Context;
import android.os.SystemProperties;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import com.android.ims.ImsManager;

public class DomainSelection {
    private static final int ATT_IOT = 32;
    private static final int CS_VOICE_ONLY = 1;
    private static final int CS_VOICE_PREFERRED = 2;
    private static final int IMS_OVER_WIFI_ENABLED = 8;
    private static final int IMS_PS_VOICE_ONLY = 4;
    private static final int IMS_PS_VOICE_PREFERRED = 3;
    private static final int IMS_SMS_ENABLED = 4;
    private static final int LOCAL_CW = 16;
    private static final boolean LOCAL_DEBUG = true;
    static final String LOG_TAG = "DomainSelection";
    private static final String PROP_DOMAIN_SETTING = "persist.sys.domain.sel";
    private static final String PROP_DOMAIN_SETTING2 = "persist.sys.domain.sel2";
    private static final String PROP_IMS_CONFIG = "persist.radio.ims.config";
    private static final String PROP_SIM_TYPE = "ril.telephony.properties";
    private static final boolean VDBG = false;
    static Context mContext;
    Phone mPhone;

    protected DomainSelection(Context context, Phone phone) {
        mContext = context;
        this.mPhone = phone;
    }

    private String domainToString(int domain) {
        switch (domain) {
            case 1:
                return "CS VOICE ONLY";
            case 2:
                return "CS VOICE PREFERRED";
            case 3:
                return "IMS PS VOICE PREFERRED";
            case 4:
                return "IMS PS VOICE ONLY";
            default:
                return "UNKNOWN DOMAIN";
        }
    }

    private int getDomainSetting() {
        return Dsds.isSim2Master() ? (SystemProperties.getInt(PROP_DOMAIN_SETTING2, 0) & 3) + 1 : (SystemProperties.getInt(PROP_DOMAIN_SETTING, 0) & 3) + 1;
    }

    private boolean isImsAvailable() {
        boolean imsUseEnabled = ImsManager.isVolteEnabledByPlatform(mContext) && ImsManager.isEnhanced4gLteModeSettingEnabledByUser(mContext);
        Phone imsPhone = this.mPhone.getImsPhone();
        return imsUseEnabled && imsPhone != null;
    }

    private boolean isEmergencyCallViaIms() {
        return isImsAvailable() && mContext.getResources().getBoolean(R.^attr-private.layout_ignoreOffset);
    }

    public boolean useVoLTE(String dialString) {
        boolean z = true;
        if (PhoneNumberUtils.isEmergencyNumber(dialString)) {
            return isEmergencyCallViaIms();
        }
        int voiceDomain = getDomainSetting();
        if (voiceDomain == 2 || voiceDomain == 1) {
            Rlog.d(LOG_TAG, "forced to CS due to Voice Domain is " + domainToString(voiceDomain));
            return false;
        }
        if (this.mPhone.getServiceState().getVoiceNetworkType() != 13 && !isImsOverWifiEnabled()) {
            Rlog.d(LOG_TAG, "forced to CS due to voice network type is not LTE");
            return false;
        }
        Phone imsPhone = this.mPhone.getImsPhone();
        if (!isImsAvailable() || imsPhone.getServiceState().getState() != 0) {
            z = false;
        }
        return z;
    }

    private boolean isImsOverWifiEnabled() {
        return (SystemProperties.getInt(PROP_IMS_CONFIG, 0) & 8) == 8;
    }

    public boolean isLocalCallWaitingEnabled() {
        return (SystemProperties.getInt(PROP_IMS_CONFIG, 0) & 16) == 16;
    }

    public boolean utInterfaceEnabled() {
        int simType = SystemProperties.getInt(PROP_SIM_TYPE, 0);
        if ((simType & 2) != 2) {
            return false;
        }
        Rlog.d(LOG_TAG, "USIM used. Use Ut for SS");
        return isImsAvailable();
    }

    public boolean smsOverImsEnabled() {
        int enabled = SystemProperties.getInt(PROP_IMS_CONFIG, 0);
        if ((enabled & 4) != 4) {
            return false;
        }
        Rlog.d(LOG_TAG, "SMS over IMS enabled");
        return isImsAvailable();
    }
}
