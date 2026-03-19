package com.android.internal.telephony;

import android.hardware.radio.V1_0.RadioResponseInfo;
import android.hardware.radio.deprecated.V1_0.IOemHookResponse;
import java.util.ArrayList;

public class OemHookResponse extends IOemHookResponse.Stub {
    RIL mRil;

    public OemHookResponse(RIL ril) {
        this.mRil = ril;
    }

    @Override
    public void sendRequestRawResponse(RadioResponseInfo radioResponseInfo, ArrayList<Byte> arrayList) {
        RILRequest rILRequestProcessResponse = this.mRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            byte[] bArrArrayListToPrimitiveArray = null;
            if (radioResponseInfo.error == 0) {
                bArrArrayListToPrimitiveArray = RIL.arrayListToPrimitiveArray(arrayList);
                RadioResponse.sendMessageResponse(rILRequestProcessResponse.mResult, bArrArrayListToPrimitiveArray);
            }
            this.mRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, bArrArrayListToPrimitiveArray);
        }
    }

    @Override
    public void sendRequestStringsResponse(RadioResponseInfo radioResponseInfo, ArrayList<String> arrayList) {
        RadioResponse.responseStringArrayList(this.mRil, radioResponseInfo, arrayList);
    }
}
