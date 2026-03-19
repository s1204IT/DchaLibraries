package com.android.internal.telephony.cat;

import com.android.internal.telephony.EncodeException;
import com.android.internal.telephony.GsmAlphabet;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

class GetInkeyInputResponseData extends ResponseData {
    protected static final byte GET_INKEY_NO = 0;
    protected static final byte GET_INKEY_YES = 1;
    public String mInData;
    private boolean mIsPacked;
    private boolean mIsUcs2;
    private boolean mIsYesNo;
    private boolean mYesNoResponse;

    public GetInkeyInputResponseData(String str, boolean z, boolean z2) {
        this.mIsUcs2 = z;
        this.mIsPacked = z2;
        this.mInData = str;
        this.mIsYesNo = false;
    }

    public GetInkeyInputResponseData(boolean z) {
        this.mIsUcs2 = false;
        this.mIsPacked = false;
        this.mInData = "";
        this.mIsYesNo = true;
        this.mYesNoResponse = z;
    }

    @Override
    public void format(ByteArrayOutputStream byteArrayOutputStream) {
        byte[] bArrStringToGsm8BitPacked;
        if (byteArrayOutputStream == null) {
            return;
        }
        byteArrayOutputStream.write(128 | ComprehensionTlvTag.TEXT_STRING.value());
        if (this.mIsYesNo) {
            bArrStringToGsm8BitPacked = new byte[]{this.mYesNoResponse};
        } else if (this.mInData != null && this.mInData.length() > 0) {
            try {
                if (this.mIsUcs2) {
                    bArrStringToGsm8BitPacked = this.mInData.getBytes("UTF-16BE");
                } else if (this.mIsPacked) {
                    byte[] bArrStringToGsm7BitPacked = GsmAlphabet.stringToGsm7BitPacked(this.mInData, 0, 0);
                    byte[] bArr = new byte[bArrStringToGsm7BitPacked.length - 1];
                    System.arraycopy(bArrStringToGsm7BitPacked, 1, bArr, 0, bArrStringToGsm7BitPacked.length - 1);
                    bArrStringToGsm8BitPacked = bArr;
                } else {
                    bArrStringToGsm8BitPacked = GsmAlphabet.stringToGsm8BitPacked(this.mInData);
                }
            } catch (UnsupportedEncodingException e) {
                bArrStringToGsm8BitPacked = new byte[0];
            } catch (EncodeException e2) {
                bArrStringToGsm8BitPacked = new byte[0];
            }
        } else {
            bArrStringToGsm8BitPacked = new byte[0];
        }
        if (bArrStringToGsm8BitPacked.length + 1 <= 255) {
            writeLength(byteArrayOutputStream, bArrStringToGsm8BitPacked.length + 1);
        } else {
            bArrStringToGsm8BitPacked = new byte[0];
        }
        if (this.mIsUcs2) {
            byteArrayOutputStream.write(8);
        } else if (this.mIsPacked) {
            byteArrayOutputStream.write(0);
        } else {
            byteArrayOutputStream.write(4);
        }
        for (byte b : bArrStringToGsm8BitPacked) {
            byteArrayOutputStream.write(b);
        }
    }
}
