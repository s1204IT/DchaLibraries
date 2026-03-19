package com.mediatek.ims.internal;

import android.content.Context;
import android.os.RemoteException;
import android.telephony.Rlog;
import com.android.ims.ImsConfigListener;
import com.android.ims.ImsException;
import mediatek.telephony.MtkServiceState;

public class MtkImsConfig {
    public static final int MTK_CONFIG_START = 1000;
    private static final String TAG = "MtkImsConfig";
    private boolean DBG = true;
    private Context mContext;
    private final IMtkImsConfig miConfig;

    public static class ConfigConstants {
        public static final int CONFIG_START = 1000;
        public static final int EPDG_ADDRESS = 1000;
        public static final int PROVISIONED_CONFIG_END = 1000;
        public static final int PROVISIONED_CONFIG_START = 1000;
    }

    public static class WfcModeFeatureValueConstants {
        public static final int CELLULAR_ONLY = 3;
        public static final int CELLULAR_PREFERRED = 1;
        public static final int WIFI_ONLY = 0;
        public static final int WIFI_PREFERRED = 2;
    }

    public MtkImsConfig(IMtkImsConfig iMtkImsConfig, Context context) {
        this.miConfig = iMtkImsConfig;
        this.mContext = context;
    }

    public int getProvisionedValue(int i) throws ImsException {
        try {
            int provisionedValue = this.miConfig.getProvisionedValue(i);
            if (this.DBG) {
                Rlog.d(TAG, "getProvisionedValue(): item = " + i + ", ret =" + provisionedValue);
            }
            return provisionedValue;
        } catch (RemoteException e) {
            throw new ImsException("getValue()", e, MtkServiceState.RIL_RADIO_TECHNOLOGY_HSUPAP);
        }
    }

    public String getProvisionedStringValue(int i) throws ImsException {
        try {
            String provisionedStringValue = this.miConfig.getProvisionedStringValue(i);
            if (this.DBG) {
                Rlog.d(TAG, "getProvisionedStringValue(): item = " + i + ", ret =" + provisionedStringValue);
            }
            return provisionedStringValue;
        } catch (RemoteException e) {
            throw new ImsException("getProvisionedStringValue()", e, MtkServiceState.RIL_RADIO_TECHNOLOGY_HSUPAP);
        }
    }

    public int setProvisionedValue(int i, int i2) throws ImsException {
        try {
            return this.miConfig.setProvisionedValue(i, i2);
        } catch (RemoteException e) {
            throw new ImsException("setProvisionedValue()", e, MtkServiceState.RIL_RADIO_TECHNOLOGY_HSUPAP);
        }
    }

    public int setProvisionedStringValue(int i, String str) throws ImsException {
        try {
            return this.miConfig.setProvisionedStringValue(i, str);
        } catch (RemoteException e) {
            throw new ImsException("setProvisionedStringValue()", e, MtkServiceState.RIL_RADIO_TECHNOLOGY_HSUPAP);
        }
    }

    public void getFeatureValue(int i, int i2, ImsConfigListener imsConfigListener) throws ImsException {
        if (this.DBG) {
            Rlog.d(TAG, "getFeatureValue: feature = " + i + ", network =" + i2 + ", listener =" + imsConfigListener);
        }
        try {
            this.miConfig.getFeatureValue(i, i2, imsConfigListener);
        } catch (RemoteException e) {
            throw new ImsException("getFeatureValue()", e, MtkServiceState.RIL_RADIO_TECHNOLOGY_HSUPAP);
        }
    }

    public void setFeatureValue(int i, int i2, int i3, ImsConfigListener imsConfigListener) throws ImsException {
        try {
            this.miConfig.setFeatureValue(i, i2, i3, imsConfigListener);
        } catch (RemoteException e) {
            throw new ImsException("setFeatureValue()", e, MtkServiceState.RIL_RADIO_TECHNOLOGY_HSUPAP);
        }
    }

    public void setMultiFeatureValues(int[] iArr, int[] iArr2, int[] iArr3, ImsConfigListener imsConfigListener) throws ImsException {
        try {
            Rlog.d(TAG, "setMultiFeatureValues()");
            this.miConfig.setMultiFeatureValues(iArr, iArr2, iArr3, imsConfigListener);
        } catch (RemoteException e) {
            throw new ImsException("setMultiFeatureValues()", e, MtkServiceState.RIL_RADIO_TECHNOLOGY_HSUPAP);
        }
    }

    public void setImsResCapability(int i, int i2) throws ImsException {
        try {
            this.miConfig.setImsResCapability(i, i2);
        } catch (RemoteException e) {
            throw new ImsException("setImsResCapability()", e, MtkServiceState.RIL_RADIO_TECHNOLOGY_HSUPAP);
        }
    }

    public int getImsResCapability(int i) throws ImsException {
        try {
            return this.miConfig.getImsResCapability(i);
        } catch (RemoteException e) {
            throw new ImsException("getImsResCapability()", e, MtkServiceState.RIL_RADIO_TECHNOLOGY_HSUPAP);
        }
    }

    public void setWfcMode(int i) throws ImsException {
        try {
            this.miConfig.setWfcMode(i);
        } catch (RemoteException e) {
            throw new ImsException("setWfcMode()", e, MtkServiceState.RIL_RADIO_TECHNOLOGY_HSUPAP);
        }
    }

    public void setVoltePreference(int i) throws ImsException {
        try {
            this.miConfig.setVoltePreference(i);
        } catch (RemoteException e) {
            throw new ImsException("setVoltePreference()", e, MtkServiceState.RIL_RADIO_TECHNOLOGY_HSUPAP);
        }
    }

    public int[] setModemImsCfg(String[] strArr, String[] strArr2, int i) throws ImsException {
        try {
            return this.miConfig.setModemImsCfg(strArr, strArr2, i);
        } catch (RemoteException e) {
            throw new ImsException("setModemImsCfg()", e, MtkServiceState.RIL_RADIO_TECHNOLOGY_HSUPAP);
        }
    }

    public int[] setModemImsWoCfg(String[] strArr, String[] strArr2, int i) throws ImsException {
        try {
            return this.miConfig.setModemImsWoCfg(strArr, strArr2, i);
        } catch (RemoteException e) {
            throw new ImsException("setModemImsWoCfg()", e, MtkServiceState.RIL_RADIO_TECHNOLOGY_HSUPAP);
        }
    }

    public int[] setModemImsIwlanCfg(String[] strArr, String[] strArr2, int i) throws ImsException {
        try {
            return this.miConfig.setModemImsIwlanCfg(strArr, strArr2, i);
        } catch (RemoteException e) {
            throw new ImsException("setImsModemIwlanCfg()", e, MtkServiceState.RIL_RADIO_TECHNOLOGY_HSUPAP);
        }
    }
}
