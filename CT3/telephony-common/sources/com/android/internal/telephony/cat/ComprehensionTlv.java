package com.android.internal.telephony.cat;

import android.telephony.Rlog;
import com.android.internal.telephony.CallFailCause;
import com.mediatek.internal.telephony.ppl.PplMessageManager;
import java.util.ArrayList;
import java.util.List;

class ComprehensionTlv {
    private static final String LOG_TAG = "ComprehensionTlv";
    private boolean mCr;
    private int mLength;
    private byte[] mRawValue;
    private int mTag;
    private int mValueIndex;

    protected ComprehensionTlv(int tag, boolean cr, int length, byte[] data, int valueIndex) {
        this.mTag = tag;
        this.mCr = cr;
        this.mLength = length;
        this.mValueIndex = valueIndex;
        this.mRawValue = data;
    }

    public int getTag() {
        return this.mTag;
    }

    public boolean isComprehensionRequired() {
        return this.mCr;
    }

    public int getLength() {
        return this.mLength;
    }

    public int getValueIndex() {
        return this.mValueIndex;
    }

    public byte[] getRawValue() {
        return this.mRawValue;
    }

    public static List<ComprehensionTlv> decodeMany(byte[] data, int startIndex) throws ResultException {
        ArrayList<ComprehensionTlv> items = new ArrayList<>();
        int endIndex = data.length;
        while (true) {
            if (startIndex < endIndex) {
                ComprehensionTlv ctlv = decode(data, startIndex);
                if (ctlv != null) {
                    items.add(ctlv);
                    startIndex = ctlv.mValueIndex + ctlv.mLength;
                } else {
                    CatLog.d(LOG_TAG, "decodeMany: ctlv is null, stop decoding");
                    break;
                }
            } else {
                break;
            }
        }
        return items;
    }

    public static ComprehensionTlv decode(byte[] data, int startIndex) throws ResultException {
        boolean cr;
        int tag;
        int curIndex;
        int length;
        int endIndex = data.length;
        int curIndex2 = startIndex + 1;
        try {
            int temp = data[startIndex] & PplMessageManager.Type.INVALID;
            switch (temp) {
                case 0:
                case 128:
                case 255:
                    Rlog.d("CAT     ", "decode: unexpected first tag byte=" + Integer.toHexString(temp) + ", startIndex=" + startIndex + " curIndex=" + curIndex2 + " endIndex=" + endIndex);
                    return null;
                case CallFailCause.INTERWORKING_UNSPECIFIED:
                    int tag2 = ((data[curIndex2] & PplMessageManager.Type.INVALID) << 8) | (data[curIndex2 + 1] & PplMessageManager.Type.INVALID);
                    cr = (32768 & tag2) != 0;
                    tag = tag2 & (-32769);
                    curIndex = curIndex2 + 2;
                    break;
                default:
                    cr = (temp & 128) != 0;
                    tag = temp & (-129);
                    curIndex = curIndex2;
                    break;
            }
            int curIndex3 = curIndex + 1;
            int temp2 = data[curIndex] & PplMessageManager.Type.INVALID;
            if (temp2 < 128) {
                length = temp2;
            } else if (temp2 == 129) {
                int curIndex4 = curIndex3 + 1;
                try {
                    length = data[curIndex3] & PplMessageManager.Type.INVALID;
                    if (length < 128) {
                        throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD, "length < 0x80 length=" + Integer.toHexString(length) + " startIndex=" + startIndex + " curIndex=" + curIndex4 + " endIndex=" + endIndex);
                    }
                    curIndex3 = curIndex4;
                } catch (IndexOutOfBoundsException e) {
                    curIndex2 = curIndex4;
                    throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD, "IndexOutOfBoundsException startIndex=" + startIndex + " curIndex=" + curIndex2 + " endIndex=" + endIndex);
                }
            } else if (temp2 == 130) {
                length = ((data[curIndex3] & PplMessageManager.Type.INVALID) << 8) | (data[curIndex3 + 1] & PplMessageManager.Type.INVALID);
                curIndex3 += 2;
                if (length < 256) {
                    throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD, "two byte length < 0x100 length=" + Integer.toHexString(length) + " startIndex=" + startIndex + " curIndex=" + curIndex3 + " endIndex=" + endIndex);
                }
            } else {
                if (temp2 != 131) {
                    throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD, "Bad length modifer=" + temp2 + " startIndex=" + startIndex + " curIndex=" + curIndex3 + " endIndex=" + endIndex);
                }
                length = ((data[curIndex3] & PplMessageManager.Type.INVALID) << 16) | ((data[curIndex3 + 1] & PplMessageManager.Type.INVALID) << 8) | (data[curIndex3 + 2] & PplMessageManager.Type.INVALID);
                curIndex3 += 3;
                if (length < 65536) {
                    throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD, "three byte length < 0x10000 length=0x" + Integer.toHexString(length) + " startIndex=" + startIndex + " curIndex=" + curIndex3 + " endIndex=" + endIndex);
                }
            }
            return new ComprehensionTlv(tag, cr, length, data, curIndex3);
        } catch (IndexOutOfBoundsException e2) {
        }
    }
}
