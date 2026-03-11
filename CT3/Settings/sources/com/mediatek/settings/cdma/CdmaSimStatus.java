package com.mediatek.settings.cdma;

import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.CellLocation;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.Phone;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class CdmaSimStatus {
    private String mDefaultText;
    private SettingsPreferenceFragment mFragment;
    private Phone mPhone;
    private PreferenceScreen mPreferenceScreen;
    private ServiceState mServiceState;
    private SubscriptionInfo mSubInfo;
    private TelephonyManager mTelephonyManager;

    public CdmaSimStatus(SettingsPreferenceFragment fragment, SubscriptionInfo subInfo) {
        this.mFragment = fragment;
        this.mSubInfo = subInfo;
        this.mPreferenceScreen = fragment.getPreferenceScreen();
        this.mTelephonyManager = (TelephonyManager) fragment.getContext().getSystemService("phone");
        this.mDefaultText = fragment.getString(R.string.device_info_default);
    }

    public void setPhoneInfos(Phone phone) {
        int phoneType = 0;
        if (phone != null) {
            setServiceState(phone.getServiceState());
            phoneType = phone.getPhoneType();
        } else {
            Log.e("CdmaSimStatus", "No phone available");
        }
        Log.d("CdmaSimStatus", "setPhoneInfos phoneType = " + phoneType);
        this.mPhone = phone;
    }

    public void setSubscriptionInfo(SubscriptionInfo subInfo) {
        this.mSubInfo = subInfo;
        Log.d("CdmaSimStatus", "setSubscriptionInfo = " + this.mSubInfo);
    }

    public void updateCdmaPreference(SettingsPreferenceFragment fragment, SubscriptionInfo subInfo) {
        int subId = subInfo.getSubscriptionId();
        PreferenceScreen prefScreen = fragment.getPreferenceScreen();
        Log.d("CdmaSimStatus", "subId = " + subId);
        if (CdmaUtils.isSupportCdma(subId)) {
            boolean isAdded = prefScreen.findPreference("current_operators_mccmnc") != null;
            Log.d("CdmaSimStatus", "isAdded = " + isAdded);
            if (!isAdded) {
                fragment.addPreferencesFromResource(R.xml.current_networkinfo_status);
            }
            setMccMnc();
            setCdmaSidNid();
            setCellId();
            return;
        }
        removeCdmaItems();
    }

    public void setServiceState(ServiceState state) {
        Log.d("CdmaSimStatus", "setServiceState with state = " + state);
        this.mServiceState = state;
    }

    public void updateNetworkType(String key, String networktype) {
        Log.d("CdmaSimStatus", "updateNetworkType with networktype = " + networktype);
        if (!CdmaUtils.isSupportCdma(this.mSubInfo.getSubscriptionId()) || !"LTE".equals(networktype) || this.mServiceState == null || this.mServiceState.getVoiceRegState() != 0) {
            return;
        }
        String voiceNetworkName = renameNetworkTypeName(TelephonyManager.getNetworkTypeName(this.mServiceState.getVoiceNetworkType()));
        Log.d("CdmaSimStatus", "voiceNetworkName = " + voiceNetworkName);
        setSummaryText(key, voiceNetworkName + " , " + networktype);
    }

    public void updateSignalStrength(SignalStrength signal, Preference preference) {
        if (!CdmaUtils.isSupportCdma(this.mSubInfo.getSubscriptionId()) || signal.isGsm() || !isRegisterUnderLteNetwork()) {
            return;
        }
        setCdmaSignalStrength(signal, preference);
        int lteSignalDbm = signal.getLteDbm();
        int lteSignalAsu = signal.getLteAsuLevel();
        if (-1 == lteSignalDbm) {
            lteSignalDbm = 0;
        }
        if (-1 == lteSignalAsu) {
            lteSignalAsu = 0;
        }
        Log.d("CdmaSimStatus", "lteSignalDbm = " + lteSignalDbm + " lteSignalAsu = " + lteSignalAsu);
        String lteSignal = this.mFragment.getString(R.string.sim_signal_strength, new Object[]{Integer.valueOf(lteSignalDbm), Integer.valueOf(lteSignalAsu)});
        String cdmaSignal = preference.getSummary().toString();
        Log.d("CdmaSimStatus", "cdmaSignal = " + cdmaSignal + " lteSignal = " + lteSignal);
        String summary = this.mFragment.getString(R.string.status_cdma_signal_strength, new Object[]{cdmaSignal, lteSignal});
        Log.d("CdmaSimStatus", "summary = " + summary);
        preference.setSummary(summary);
    }

    private void setCdmaSignalStrength(SignalStrength signalStrength, Preference preference) {
        Log.d("CdmaSimStatus", "setCdmaSignalStrength() for 1x cdma network type");
        if (!"CDMA 1x".equals(getNetworkType())) {
            return;
        }
        int signalDbm = signalStrength.getCdmaDbm();
        int signalAsu = signalStrength.getCdmaAsuLevel();
        if (-1 == signalDbm) {
            signalDbm = 0;
        }
        if (-1 == signalAsu) {
            signalAsu = 0;
        }
        Log.d("CdmaSimStatus", "Cdma 1x signalDbm = " + signalDbm + " signalAsu = " + signalAsu);
        preference.setSummary(this.mFragment.getString(R.string.sim_signal_strength, new Object[]{Integer.valueOf(signalDbm), Integer.valueOf(signalAsu)}));
    }

    private String getNetworkType() {
        String networktype = null;
        int actualDataNetworkType = this.mTelephonyManager.getDataNetworkType(this.mSubInfo.getSubscriptionId());
        int actualVoiceNetworkType = this.mTelephonyManager.getVoiceNetworkType(this.mSubInfo.getSubscriptionId());
        Log.d("CdmaSimStatus", "actualDataNetworkType = " + actualDataNetworkType + "actualVoiceNetworkType = " + actualVoiceNetworkType);
        if (actualDataNetworkType != 0) {
            TelephonyManager telephonyManager = this.mTelephonyManager;
            networktype = TelephonyManager.getNetworkTypeName(actualDataNetworkType);
        } else if (actualVoiceNetworkType != 0) {
            TelephonyManager telephonyManager2 = this.mTelephonyManager;
            networktype = TelephonyManager.getNetworkTypeName(actualVoiceNetworkType);
        }
        Log.d("CdmaSimStatus", "getNetworkType() networktype = " + networktype);
        return renameNetworkTypeName(networktype);
    }

    private ServiceState getServiceState() {
        ServiceState serviceState = this.mPhone.getServiceState();
        Log.d("CdmaSimStatus", "serviceState = " + serviceState);
        return serviceState;
    }

    private boolean isRegisterUnderLteNetwork() {
        ServiceState serviceState = getServiceState();
        Log.d("CdmaSimStatus", "isRegisterUnderLteNetwork with serviceState = " + serviceState);
        boolean isLteNetwork = false;
        if (serviceState != null && serviceState.getDataNetworkType() == 13 && serviceState.getDataRegState() == 0) {
            isLteNetwork = true;
        }
        Log.d("CdmaSimStatus", "isLteNetwork = " + isLteNetwork);
        return isLteNetwork;
    }

    private void removeCdmaItems() {
        removePreferenceFromScreen("current_operators_mccmnc");
        removePreferenceFromScreen("current_sidnid");
        removePreferenceFromScreen("current_cellid");
    }

    private void setMccMnc() {
        String numeric = getMccMncProperty(this.mPhone);
        Log.d("CdmaSimStatus", "setMccMnc, numeric=" + numeric);
        if (numeric.length() <= 3) {
            return;
        }
        String mcc = numeric.substring(0, 3);
        String mnc = numeric.substring(3);
        String mccmnc = mcc + "," + mnc;
        Log.d("CdmaSimStatus", "mccmnc = " + mccmnc);
        setSummaryText("current_operators_mccmnc", mccmnc);
    }

    private String getMccMncProperty(Phone phone) {
        int phoneId = 0;
        if (phone != null) {
            phoneId = phone.getPhoneId();
        }
        TelephonyManager telephonyManager = this.mTelephonyManager;
        String value = TelephonyManager.getTelephonyProperty(phoneId, "gsm.operator.numeric", "");
        Log.d("CdmaSimStatus", "value = " + value);
        return value;
    }

    private void setCdmaSidNid() {
        if (this.mPhone == null || this.mPhone.getPhoneType() != 2) {
            return;
        }
        int sid = this.mPhone.getServiceState().getSystemId();
        int nid = this.mPhone.getServiceState().getNetworkId();
        String sidnid = sid + "," + nid;
        Log.d("CdmaSimStatus", "sidnid = " + sidnid);
        setSummaryText("current_sidnid", sidnid);
    }

    private void setCellId() {
        if (this.mPhone == null || this.mPhone.getPhoneType() != 2) {
            return;
        }
        CellLocation cellLocation = this.mPhone.getCellLocation();
        if (!(cellLocation instanceof CdmaCellLocation)) {
            return;
        }
        String cellId = Integer.toString(((CdmaCellLocation) cellLocation).getBaseStationId());
        Log.d("CdmaSimStatus", "cellId = " + cellId);
        setSummaryText("current_cellid", cellId);
    }

    private void setSummaryText(String key, String text) {
        if (TextUtils.isEmpty(text)) {
            text = this.mDefaultText;
        }
        Preference preference = this.mFragment.findPreference(key);
        if (preference == null) {
            return;
        }
        preference.setSummary(text);
    }

    static String renameNetworkTypeName(String netWorkTypeName) {
        Log.d("CdmaSimStatus", "renameNetworkTypeNameForCTSpec, netWorkTypeName=" + netWorkTypeName);
        return ("CDMA - EvDo rev. 0".equals(netWorkTypeName) || "CDMA - EvDo rev. A".equals(netWorkTypeName) || "CDMA - EvDo rev. B".equals(netWorkTypeName)) ? "CDMA EVDO" : "CDMA - 1xRTT".equals(netWorkTypeName) ? "CDMA 1x" : ("GPRS".equals(netWorkTypeName) || "EDGE".equals(netWorkTypeName)) ? "GSM" : ("HSDPA".equals(netWorkTypeName) || "HSUPA".equals(netWorkTypeName) || "HSPA".equals(netWorkTypeName) || "HSPA+".equals(netWorkTypeName) || "UMTS".equals(netWorkTypeName)) ? "WCDMA" : "CDMA - eHRPD".equals(netWorkTypeName) ? "eHRPD" : netWorkTypeName;
    }

    private void removePreferenceFromScreen(String key) {
        Preference pref = this.mFragment.findPreference(key);
        if (pref == null) {
            return;
        }
        this.mPreferenceScreen.removePreference(pref);
    }
}
