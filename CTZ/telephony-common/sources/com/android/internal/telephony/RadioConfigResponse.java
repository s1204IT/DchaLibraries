package com.android.internal.telephony;

import android.hardware.radio.V1_0.RadioResponseInfo;
import android.hardware.radio.config.V1_0.IRadioConfigResponse;
import android.hardware.radio.config.V1_0.SimSlotStatus;
import android.telephony.Rlog;
import com.android.internal.telephony.uicc.IccSlotStatus;
import java.util.ArrayList;

public class RadioConfigResponse extends IRadioConfigResponse.Stub {
    private static final String TAG = "RadioConfigResponse";
    private final RadioConfig mRadioConfig;

    public RadioConfigResponse(RadioConfig radioConfig) {
        this.mRadioConfig = radioConfig;
    }

    @Override
    public void getSimSlotsStatusResponse(RadioResponseInfo radioResponseInfo, ArrayList<SimSlotStatus> arrayList) {
        RILRequest rILRequestProcessResponse = this.mRadioConfig.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            ArrayList<IccSlotStatus> arrayListConvertHalSlotStatus = RadioConfig.convertHalSlotStatus(arrayList);
            if (radioResponseInfo.error == 0) {
                RadioResponse.sendMessageResponse(rILRequestProcessResponse.mResult, arrayListConvertHalSlotStatus);
                StringBuilder sb = new StringBuilder();
                sb.append(rILRequestProcessResponse.serialString());
                sb.append("< ");
                RadioConfig radioConfig = this.mRadioConfig;
                sb.append(RadioConfig.requestToString(rILRequestProcessResponse.mRequest));
                sb.append(" ");
                sb.append(arrayListConvertHalSlotStatus.toString());
                Rlog.d(TAG, sb.toString());
                return;
            }
            rILRequestProcessResponse.onError(radioResponseInfo.error, arrayListConvertHalSlotStatus);
            StringBuilder sb2 = new StringBuilder();
            sb2.append(rILRequestProcessResponse.serialString());
            sb2.append("< ");
            RadioConfig radioConfig2 = this.mRadioConfig;
            sb2.append(RadioConfig.requestToString(rILRequestProcessResponse.mRequest));
            sb2.append(" error ");
            sb2.append(radioResponseInfo.error);
            Rlog.e(TAG, sb2.toString());
            return;
        }
        Rlog.e(TAG, "getSimSlotsStatusResponse: Error " + radioResponseInfo.toString());
    }

    @Override
    public void setSimSlotsMappingResponse(RadioResponseInfo radioResponseInfo) {
        RILRequest rILRequestProcessResponse = this.mRadioConfig.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            if (radioResponseInfo.error == 0) {
                RadioResponse.sendMessageResponse(rILRequestProcessResponse.mResult, null);
                StringBuilder sb = new StringBuilder();
                sb.append(rILRequestProcessResponse.serialString());
                sb.append("< ");
                RadioConfig radioConfig = this.mRadioConfig;
                sb.append(RadioConfig.requestToString(rILRequestProcessResponse.mRequest));
                Rlog.d(TAG, sb.toString());
                return;
            }
            rILRequestProcessResponse.onError(radioResponseInfo.error, null);
            StringBuilder sb2 = new StringBuilder();
            sb2.append(rILRequestProcessResponse.serialString());
            sb2.append("< ");
            RadioConfig radioConfig2 = this.mRadioConfig;
            sb2.append(RadioConfig.requestToString(rILRequestProcessResponse.mRequest));
            sb2.append(" error ");
            sb2.append(radioResponseInfo.error);
            Rlog.e(TAG, sb2.toString());
            return;
        }
        Rlog.e(TAG, "setSimSlotsMappingResponse: Error " + radioResponseInfo.toString());
    }
}
