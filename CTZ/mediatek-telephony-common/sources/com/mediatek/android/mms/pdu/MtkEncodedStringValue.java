package com.mediatek.android.mms.pdu;

import android.util.Log;
import com.google.android.mms.pdu.EncodedStringValue;
import java.io.UnsupportedEncodingException;

public class MtkEncodedStringValue extends EncodedStringValue {
    private static final String TAG = "MtkEncodingStringValue";

    public MtkEncodedStringValue(int i, String str) {
        super(str);
        try {
            this.mData = str.getBytes(MtkCharacterSets.getMimeName(i));
            this.mCharacterSet = i;
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Default encoding must be supported.", e);
        }
    }

    public MtkEncodedStringValue(int i, byte[] bArr) {
        super(i, bArr);
    }

    public MtkEncodedStringValue(byte[] bArr) {
        super(bArr);
    }

    public MtkEncodedStringValue(String str) {
        super(str);
    }
}
