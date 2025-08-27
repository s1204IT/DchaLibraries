package com.mediatek.settings.cdma;

import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.deviceinfo.simstatus.SimStatusDialogFragment;

/* loaded from: classes.dex */
public class CdmaSimStatus {
    private SimStatusDialogFragment mFragment;
    private ServiceState mServiceState;
    private SubscriptionInfo mSubInfo;
    private TelephonyManager mTelephonyManager;

    public CdmaSimStatus(SimStatusDialogFragment simStatusDialogFragment, SubscriptionInfo subscriptionInfo) {
        this.mFragment = simStatusDialogFragment;
        this.mSubInfo = subscriptionInfo;
        this.mTelephonyManager = (TelephonyManager) simStatusDialogFragment.getContext().getSystemService("phone");
    }

    public void setServiceState(ServiceState serviceState) {
        Log.d("CdmaSimStatus", "setServiceState, serviceState=" + serviceState);
        this.mServiceState = serviceState;
    }

    public void updateSignalStrength(SignalStrength signalStrength, int i, String str) {
        Log.d("CdmaSimStatus", "updateSignalStrength, signalStrength=" + signalStrength);
        if (CdmaUtils.isSupportCdma(this.mSubInfo.getSubscriptionId()) && !signalStrength.isGsm() && isRegisterUnderLteNetwork()) {
            String cdmaSignalStrength = getCdmaSignalStrength(signalStrength, str);
            int lteDbm = signalStrength.getLteDbm();
            int lteAsuLevel = signalStrength.getLteAsuLevel();
            if (lteDbm == -1) {
                lteDbm = 0;
            }
            if (lteAsuLevel == -1) {
                lteAsuLevel = 0;
            }
            Log.d("CdmaSimStatus", "updateSignalStrength, lteSignalDbm=" + lteDbm + ", lteSignalAsu=" + lteAsuLevel);
            String string = this.mFragment.getString(R.string.sim_signal_strength, new Object[]{Integer.valueOf(lteDbm), Integer.valueOf(lteAsuLevel)});
            Log.d("CdmaSimStatus", "updateSignalStrength, cdmaSignal=" + cdmaSignalStrength + ", lteSignal=" + string);
            String string2 = this.mFragment.getString(R.string.status_cdma_signal_strength, new Object[]{cdmaSignalStrength, string});
            StringBuilder sb = new StringBuilder();
            sb.append("updateSignalStrength, summary=");
            sb.append(string2);
            Log.d("CdmaSimStatus", sb.toString());
            setSummaryText(i, string2);
        }
    }

    private String getCdmaSignalStrength(SignalStrength signalStrength, String str) {
        ServiceState serviceState = getServiceState();
        Log.d("CdmaSimStatus", "setCdmaSignalStrength, serviceState=" + serviceState);
        if (serviceState != null && serviceState.getVoiceNetworkType() == 7) {
            int cdmaDbm = signalStrength.getCdmaDbm();
            int cdmaAsuLevel = signalStrength.getCdmaAsuLevel();
            if (cdmaDbm == -1) {
                cdmaDbm = 0;
            }
            if (cdmaAsuLevel == -1) {
                cdmaAsuLevel = 0;
            }
            Log.d("CdmaSimStatus", "setCdmaSignalStrength, 1xRTT signalDbm=" + cdmaDbm + ", signalAsu=" + cdmaAsuLevel);
            return this.mFragment.getString(R.string.sim_signal_strength, new Object[]{Integer.valueOf(cdmaDbm), Integer.valueOf(cdmaAsuLevel)});
        }
        return str;
    }

    private ServiceState getServiceState() {
        return this.mServiceState;
    }

    private boolean isRegisterUnderLteNetwork() {
        boolean z;
        ServiceState serviceState = getServiceState();
        Log.d("CdmaSimStatus", "isRegisterUnderLteNetwork, serviceState=" + serviceState);
        if (serviceState != null && serviceState.getDataNetworkType() == 13 && serviceState.getDataRegState() == 0) {
            z = true;
        } else {
            z = false;
        }
        Log.d("CdmaSimStatus", "isRegisterUnderLteNetwork, lteNetwork=" + z);
        return z;
    }

    private void setSummaryText(int i, String str) {
        this.mFragment.setText(i, str);
    }
}
