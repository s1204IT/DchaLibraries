package com.android.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.telephony.CarrierConfigManager;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.util.SparseArray;
import android.util.SparseIntArray;
import java.util.Arrays;

public class RatRatcheter {
    private static final String LOG_TAG = "RilRatcheter";
    private final Phone mPhone;
    private final SparseArray<SparseIntArray> mRatFamilyMap = new SparseArray<>();
    private boolean mVoiceRatchetEnabled = true;
    private boolean mDataRatchetEnabled = true;
    private BroadcastReceiver mConfigChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.telephony.action.CARRIER_CONFIG_CHANGED".equals(intent.getAction())) {
                RatRatcheter.this.resetRatFamilyMap();
            }
        }
    };

    public static boolean updateBandwidths(int[] iArr, ServiceState serviceState) {
        if (iArr == null) {
            return false;
        }
        if (Arrays.stream(iArr).sum() <= Arrays.stream(serviceState.getCellBandwidths()).sum()) {
            return false;
        }
        serviceState.setCellBandwidths(iArr);
        return true;
    }

    public RatRatcheter(Phone phone) {
        this.mPhone = phone;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.telephony.action.CARRIER_CONFIG_CHANGED");
        phone.getContext().registerReceiverAsUser(this.mConfigChangedReceiver, UserHandle.ALL, intentFilter, null, null);
        resetRatFamilyMap();
    }

    private int ratchetRat(int i, int i2) {
        synchronized (this.mRatFamilyMap) {
            SparseIntArray sparseIntArray = this.mRatFamilyMap.get(i);
            if (sparseIntArray == null) {
                return i2;
            }
            SparseIntArray sparseIntArray2 = this.mRatFamilyMap.get(i2);
            if (sparseIntArray2 != sparseIntArray) {
                return i2;
            }
            if (sparseIntArray2.get(i, -1) <= sparseIntArray2.get(i2, -1)) {
                i = i2;
            }
            return i;
        }
    }

    public void ratchet(ServiceState serviceState, ServiceState serviceState2, boolean z) {
        if (!z && isSameRatFamily(serviceState, serviceState2)) {
            updateBandwidths(serviceState.getCellBandwidths(), serviceState2);
        }
        boolean z2 = false;
        if (z) {
            this.mVoiceRatchetEnabled = false;
            this.mDataRatchetEnabled = false;
            return;
        }
        if (this.mVoiceRatchetEnabled) {
            serviceState2.setRilVoiceRadioTechnology(ratchetRat(serviceState.getRilVoiceRadioTechnology(), serviceState2.getRilVoiceRadioTechnology()));
        } else if (serviceState.getRilVoiceRadioTechnology() != serviceState2.getRilVoiceRadioTechnology()) {
            this.mVoiceRatchetEnabled = true;
        }
        if (this.mDataRatchetEnabled) {
            serviceState2.setRilDataRadioTechnology(ratchetRat(serviceState.getRilDataRadioTechnology(), serviceState2.getRilDataRadioTechnology()));
        } else if (serviceState.getRilDataRadioTechnology() != serviceState2.getRilDataRadioTechnology()) {
            this.mDataRatchetEnabled = true;
        }
        if (serviceState.isUsingCarrierAggregation() || serviceState2.isUsingCarrierAggregation() || serviceState2.getCellBandwidths().length > 1) {
            z2 = true;
        }
        serviceState2.setIsUsingCarrierAggregation(z2);
    }

    private boolean isSameRatFamily(ServiceState serviceState, ServiceState serviceState2) {
        synchronized (this.mRatFamilyMap) {
            if (serviceState.getRilDataRadioTechnology() == serviceState2.getRilDataRadioTechnology()) {
                return true;
            }
            if (this.mRatFamilyMap.get(serviceState.getRilDataRadioTechnology()) == null) {
                return false;
            }
            return this.mRatFamilyMap.get(serviceState.getRilDataRadioTechnology()) == this.mRatFamilyMap.get(serviceState2.getRilDataRadioTechnology());
        }
    }

    private void resetRatFamilyMap() {
        synchronized (this.mRatFamilyMap) {
            this.mRatFamilyMap.clear();
            CarrierConfigManager carrierConfigManager = (CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config");
            if (carrierConfigManager == null) {
                return;
            }
            PersistableBundle config = carrierConfigManager.getConfig();
            if (config == null) {
                return;
            }
            String[] stringArray = config.getStringArray("ratchet_rat_families");
            if (stringArray == null) {
                return;
            }
            for (String str : stringArray) {
                String[] strArrSplit = str.split(",");
                if (strArrSplit.length >= 2) {
                    SparseIntArray sparseIntArray = new SparseIntArray(strArrSplit.length);
                    int length = strArrSplit.length;
                    int i = 0;
                    int i2 = 0;
                    while (true) {
                        if (i < length) {
                            String str2 = strArrSplit[i];
                            try {
                                int i3 = Integer.parseInt(str2.trim());
                                if (this.mRatFamilyMap.get(i3) != null) {
                                    Rlog.e(LOG_TAG, "RAT listed twice: " + str2);
                                    break;
                                }
                                sparseIntArray.put(i3, i2);
                                this.mRatFamilyMap.put(i3, sparseIntArray);
                                i++;
                                i2++;
                            } catch (NumberFormatException e) {
                                Rlog.e(LOG_TAG, "NumberFormatException on " + str2);
                            }
                        }
                    }
                }
            }
        }
    }
}
