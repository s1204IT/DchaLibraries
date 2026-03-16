package com.android.ims;

import android.content.Context;
import android.os.RemoteException;
import android.telephony.Rlog;
import com.android.ims.internal.IImsConfig;

public class ImsConfig {
    private static final String MODIFY_PHONE_STATE = "android.permission.MODIFY_PHONE_STATE";
    private static final String TAG = "ImsConfig";
    private boolean DBG = true;
    private Context mContext;
    private final IImsConfig miConfig;

    public static class ConfigConstants {
        public static final int AVAILABILITY_CACHE_EXPIRATION = 18;
        public static final int CANCELLATION_TIMER = 4;
        public static final int CAPABILITIES_CACHE_EXPIRATION = 17;
        public static final int CAPABILITIES_POLL_INTERVAL = 19;
        public static final int CAPAB_POLL_LIST_SUB_EXP = 22;
        public static final int CONFIG_START = 0;
        public static final int DOMAIN_NAME = 12;
        public static final int EAB_SETTING_ENABLED = 24;
        public static final int GZIP_FLAG = 23;
        public static final int LVC_SETTING_ENABLED = 11;
        public static final int MAX_NUMENTRIES_IN_RCL = 21;
        public static final int MIN_SE = 3;
        public static final int PROVISIONED_CONFIG_END = 24;
        public static final int PROVISIONED_CONFIG_START = 0;
        public static final int PUBLISH_TIMER = 15;
        public static final int PUBLISH_TIMER_EXTENDED = 16;
        public static final int SILENT_REDIAL_ENABLE = 6;
        public static final int SIP_SESSION_TIMER = 2;
        public static final int SIP_T1_TIMER = 7;
        public static final int SIP_T2_TIMER = 8;
        public static final int SIP_TF_TIMER = 9;
        public static final int SMS_FORMAT = 13;
        public static final int SMS_OVER_IP = 14;
        public static final int SOURCE_THROTTLE_PUBLISH = 20;
        public static final int TDELAY = 5;
        public static final int VLT_SETTING_ENABLED = 10;
        public static final int VOCODER_AMRMODESET = 0;
        public static final int VOCODER_AMRWBMODESET = 1;
    }

    public static class FeatureConstants {
        public static final int FEATURE_TYPE_UNKNOWN = -1;
        public static final int FEATURE_TYPE_VIDEO_OVER_LTE = 1;
        public static final int FEATURE_TYPE_VIDEO_OVER_WIFI = 3;
        public static final int FEATURE_TYPE_VOICE_OVER_LTE = 0;
        public static final int FEATURE_TYPE_VOICE_OVER_WIFI = 2;
    }

    public static class FeatureValueConstants {
        public static final int OFF = 0;
        public static final int ON = 1;
    }

    public static class OperationStatusConstants {
        public static final int FAILED = 1;
        public static final int SUCCESS = 0;
        public static final int UNKNOWN = -1;
        public static final int UNSUPPORTED_CAUSE_DISABLED = 4;
        public static final int UNSUPPORTED_CAUSE_NONE = 2;
        public static final int UNSUPPORTED_CAUSE_RAT = 3;
    }

    public ImsConfig(IImsConfig iconfig, Context context) {
        if (this.DBG) {
            Rlog.d(TAG, "ImsConfig creates");
        }
        this.miConfig = iconfig;
        this.mContext = context;
    }

    public int getProvisionedValue(int item) throws ImsException {
        try {
            int ret = this.miConfig.getProvisionedValue(item);
            if (this.DBG) {
                Rlog.d(TAG, "getProvisionedValue(): item = " + item + ", ret =" + ret);
            }
            return ret;
        } catch (RemoteException e) {
            throw new ImsException("getValue()", e, 131);
        }
    }

    public String getProvisionedStringValue(int item) throws ImsException {
        try {
            String ret = this.miConfig.getProvisionedStringValue(item);
            if (this.DBG) {
                Rlog.d(TAG, "getProvisionedStringValue(): item = " + item + ", ret =" + ret);
            }
            return ret;
        } catch (RemoteException e) {
            throw new ImsException("getProvisionedStringValue()", e, 131);
        }
    }

    public int setProvisionedValue(int item, int value) throws ImsException {
        this.mContext.enforceCallingOrSelfPermission(MODIFY_PHONE_STATE, null);
        if (this.DBG) {
            Rlog.d(TAG, "setProvisionedValue(): item = " + item + "value = " + value);
        }
        try {
            int ret = this.miConfig.setProvisionedValue(item, value);
            if (this.DBG) {
                Rlog.d(TAG, "setProvisionedValue(): item = " + item + " value = " + value + " ret = " + ret);
            }
            return ret;
        } catch (RemoteException e) {
            throw new ImsException("setProvisionedValue()", e, 131);
        }
    }

    public int setProvisionedStringValue(int item, String value) throws ImsException {
        this.mContext.enforceCallingOrSelfPermission(MODIFY_PHONE_STATE, null);
        try {
            int ret = this.miConfig.setProvisionedStringValue(item, value);
            if (this.DBG) {
                Rlog.d(TAG, "setProvisionedStringValue(): item = " + item + ", value =" + value);
            }
            return ret;
        } catch (RemoteException e) {
            throw new ImsException("setProvisionedStringValue()", e, 131);
        }
    }

    public void getFeatureValue(int feature, int network, ImsConfigListener listener) throws ImsException {
        if (this.DBG) {
            Rlog.d(TAG, "getFeatureValue: feature = " + feature + ", network =" + network + ", listener =" + listener);
        }
        try {
            this.miConfig.getFeatureValue(feature, network, listener);
        } catch (RemoteException e) {
            throw new ImsException("getFeatureValue()", e, 131);
        }
    }

    public void setFeatureValue(int feature, int network, int value, ImsConfigListener listener) throws ImsException {
        this.mContext.enforceCallingOrSelfPermission(MODIFY_PHONE_STATE, null);
        if (this.DBG) {
            Rlog.d(TAG, "setFeatureValue: feature = " + feature + ", network =" + network + ", value =" + value + ", listener =" + listener);
        }
        try {
            this.miConfig.setFeatureValue(feature, network, value, listener);
        } catch (RemoteException e) {
            throw new ImsException("setFeatureValue()", e, 131);
        }
    }

    public boolean getVolteProvisioned() throws ImsException {
        try {
            return this.miConfig.getVolteProvisioned();
        } catch (RemoteException e) {
            throw new ImsException("getVolteProvisioned()", e, 131);
        }
    }
}
