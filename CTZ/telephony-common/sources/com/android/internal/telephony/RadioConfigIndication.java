package com.android.internal.telephony;

import android.hardware.radio.config.V1_0.IRadioConfigIndication;
import android.hardware.radio.config.V1_0.SimSlotStatus;
import android.os.AsyncResult;
import android.telephony.Rlog;
import com.android.internal.telephony.uicc.IccSlotStatus;
import java.util.ArrayList;

public class RadioConfigIndication extends IRadioConfigIndication.Stub {
    private static final String TAG = "RadioConfigIndication";
    private final RadioConfig mRadioConfig;

    public RadioConfigIndication(RadioConfig radioConfig) {
        this.mRadioConfig = radioConfig;
    }

    @Override
    public void simSlotsStatusChanged(int i, ArrayList<SimSlotStatus> arrayList) {
        ArrayList<IccSlotStatus> arrayListConvertHalSlotStatus = RadioConfig.convertHalSlotStatus(arrayList);
        Rlog.d(TAG, "[UNSL]<  UNSOL_SIM_SLOT_STATUS_CHANGED " + arrayListConvertHalSlotStatus.toString());
        if (this.mRadioConfig.mSimSlotStatusRegistrant != null) {
            this.mRadioConfig.mSimSlotStatusRegistrant.notifyRegistrant(new AsyncResult((Object) null, arrayListConvertHalSlotStatus, (Throwable) null));
        }
    }
}
