package com.android.internal.telephony.cat;

import java.util.Iterator;
import java.util.List;

public class BerTlv {
    public static final int BER_EVENT_DOWNLOAD_TAG = 214;
    public static final int BER_MENU_SELECTION_TAG = 211;
    public static final int BER_PROACTIVE_COMMAND_TAG = 208;
    public static final int BER_UNKNOWN_TAG = 0;
    private List<ComprehensionTlv> mCompTlvs;
    private boolean mLengthValid;
    private int mTag;

    private BerTlv(int i, List<ComprehensionTlv> list, boolean z) {
        this.mTag = 0;
        this.mCompTlvs = null;
        this.mLengthValid = true;
        this.mTag = i;
        this.mCompTlvs = list;
        this.mLengthValid = z;
    }

    public List<ComprehensionTlv> getComprehensionTlvs() {
        return this.mCompTlvs;
    }

    public int getTag() {
        return this.mTag;
    }

    public boolean isLengthValid() {
        return this.mLengthValid;
    }

    public static BerTlv decode(byte[] bArr) throws ResultException {
        int i;
        int i2;
        int i3;
        int length = bArr.length;
        int i4 = 3;
        boolean z = true;
        try {
            try {
                i = bArr[0] & 255;
            } catch (ResultException e) {
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD, e.explanation());
            }
        } catch (IndexOutOfBoundsException e2) {
            i4 = 1;
        }
        if (i == 208) {
            try {
                i2 = bArr[1] & 255;
            } catch (IndexOutOfBoundsException e3) {
                i4 = 2;
            }
            if (i2 >= 128) {
                if (i2 == 129) {
                    try {
                        i3 = bArr[2] & 255;
                        if (i3 < 128) {
                            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD, "length < 0x80 length=" + Integer.toHexString(0) + " curIndex=3 endIndex=" + length);
                        }
                    } catch (IndexOutOfBoundsException e4) {
                    }
                } else {
                    throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD, "Expected first byte to be length or a length tag and < 0x81 byte= " + Integer.toHexString(i2) + " curIndex=2 endIndex=" + length);
                }
                throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING, "IndexOutOfBoundsException  curIndex=" + i4 + " endIndex=" + length);
            }
            i4 = 2;
            i3 = i2;
        } else if (ComprehensionTlvTag.COMMAND_DETAILS.value() == (i & (-129))) {
            i4 = 0;
            i3 = 0;
            i = 0;
        } else {
            i4 = 1;
            i3 = 0;
        }
        if (length - i4 < i3) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD, "Command had extra data endIndex=" + length + " curIndex=" + i4 + " length=" + i3);
        }
        List<ComprehensionTlv> listDecodeMany = ComprehensionTlv.decodeMany(bArr, i4);
        if (i == 208) {
            Iterator<ComprehensionTlv> it = listDecodeMany.iterator();
            int i5 = 0;
            while (it.hasNext()) {
                int length2 = it.next().getLength();
                if (length2 >= 128 && length2 <= 255) {
                    i5 += length2 + 3;
                } else if (length2 >= 0 && length2 < 128) {
                    i5 += length2 + 2;
                } else {
                    z = false;
                    break;
                }
            }
            if (i3 != i5) {
                z = false;
            }
        }
        return new BerTlv(i, listDecodeMany, z);
    }
}
