package com.android.internal.telephony.cat;

import com.android.internal.telephony.GsmAlphabet;
import java.io.ByteArrayOutputStream;

class LanguageResponseData extends ResponseData {
    private String mLang;

    public LanguageResponseData(String str) {
        this.mLang = str;
    }

    @Override
    public void format(ByteArrayOutputStream byteArrayOutputStream) {
        byte[] bArrStringToGsm8BitPacked;
        if (byteArrayOutputStream == null) {
            return;
        }
        byteArrayOutputStream.write(128 | ComprehensionTlvTag.LANGUAGE.value());
        if (this.mLang != null && this.mLang.length() > 0) {
            bArrStringToGsm8BitPacked = GsmAlphabet.stringToGsm8BitPacked(this.mLang);
        } else {
            bArrStringToGsm8BitPacked = new byte[0];
        }
        byteArrayOutputStream.write(bArrStringToGsm8BitPacked.length);
        for (byte b : bArrStringToGsm8BitPacked) {
            byteArrayOutputStream.write(b);
        }
    }
}
