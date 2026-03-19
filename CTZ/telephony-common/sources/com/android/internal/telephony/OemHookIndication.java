package com.android.internal.telephony;

import android.hardware.radio.deprecated.V1_0.IOemHookIndication;
import android.os.AsyncResult;
import com.android.internal.telephony.uicc.IccUtils;
import java.util.ArrayList;

public class OemHookIndication extends IOemHookIndication.Stub {
    RIL mRil;

    public OemHookIndication(RIL ril) {
        this.mRil = ril;
    }

    @Override
    public void oemHookRaw(int i, ArrayList<Byte> arrayList) {
        this.mRil.processIndication(i);
        byte[] bArrArrayListToPrimitiveArray = RIL.arrayListToPrimitiveArray(arrayList);
        this.mRil.unsljLogvRet(1028, IccUtils.bytesToHexString(bArrArrayListToPrimitiveArray));
        if (this.mRil.mUnsolOemHookRawRegistrant != null) {
            this.mRil.mUnsolOemHookRawRegistrant.notifyRegistrant(new AsyncResult((Object) null, bArrArrayListToPrimitiveArray, (Throwable) null));
        }
    }
}
