package com.android.internal.telephony.cat;

import com.android.internal.telephony.EncodeException;
import com.android.internal.telephony.GsmAlphabet;
import com.google.android.mms.pdu.CharacterSets;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
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

    public GetInkeyInputResponseData(String inData, boolean ucs2, boolean packed) {
        this.mIsUcs2 = ucs2;
        this.mIsPacked = packed;
        this.mInData = inData;
        this.mIsYesNo = false;
    }

    public GetInkeyInputResponseData(boolean yesNoResponse) {
        this.mIsUcs2 = false;
        this.mIsPacked = false;
        this.mInData = UsimPBMemInfo.STRING_NOT_SET;
        this.mIsYesNo = true;
        this.mYesNoResponse = yesNoResponse;
    }

    @Override
    public void format(ByteArrayOutputStream buf) {
        byte[] data;
        if (buf == null) {
            return;
        }
        int tag = ComprehensionTlvTag.TEXT_STRING.value() | 128;
        buf.write(tag);
        if (this.mIsYesNo) {
            data = new byte[1];
            data[0] = this.mYesNoResponse ? (byte) 1 : (byte) 0;
        } else if (this.mInData != null && this.mInData.length() > 0) {
            try {
                if (this.mIsUcs2) {
                    data = this.mInData.getBytes(CharacterSets.MIMENAME_UTF_16BE);
                } else if (this.mIsPacked) {
                    byte[] tempData = GsmAlphabet.stringToGsm7BitPacked(this.mInData, 0, 0);
                    data = new byte[tempData.length - 1];
                    System.arraycopy(tempData, 1, data, 0, tempData.length - 1);
                } else {
                    data = GsmAlphabet.stringToGsm8BitPacked(this.mInData);
                }
            } catch (UnsupportedEncodingException e) {
                data = new byte[0];
            } catch (EncodeException e2) {
                data = new byte[0];
            }
        } else {
            data = new byte[0];
        }
        if (data.length + 1 <= 255) {
            writeLength(buf, data.length + 1);
        } else {
            data = new byte[0];
        }
        if (this.mIsUcs2) {
            buf.write(8);
        } else if (this.mIsPacked) {
            buf.write(0);
        } else {
            buf.write(4);
        }
        for (byte b : data) {
            buf.write(b);
        }
    }
}
