package com.android.internal.telephony.cat;

import android.telephony.Rlog;
import java.util.ArrayList;
import java.util.List;

public class ComprehensionTlv {
    private static final String LOG_TAG = "ComprehensionTlv";
    private boolean mCr;
    private int mLength;
    private byte[] mRawValue;
    private int mTag;
    private int mValueIndex;

    protected ComprehensionTlv(int i, boolean z, int i2, byte[] bArr, int i3) {
        this.mTag = i;
        this.mCr = z;
        this.mLength = i2;
        this.mValueIndex = i3;
        this.mRawValue = bArr;
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

    public static List<ComprehensionTlv> decodeMany(byte[] bArr, int i) throws ResultException {
        ArrayList arrayList = new ArrayList();
        int length = bArr.length;
        while (true) {
            if (i < length) {
                ComprehensionTlv comprehensionTlvDecode = decode(bArr, i);
                if (comprehensionTlvDecode != null) {
                    arrayList.add(comprehensionTlvDecode);
                    i = comprehensionTlvDecode.mLength + comprehensionTlvDecode.mValueIndex;
                } else {
                    CatLog.d(LOG_TAG, "decodeMany: ctlv is null, stop decoding");
                    break;
                }
            } else {
                break;
            }
        }
        return arrayList;
    }

    public static ComprehensionTlv decode(byte[] bArr, int i) throws ResultException {
        int i2;
        boolean z;
        int i3;
        int i4;
        int length = bArr.length;
        int i5 = i + 1;
        try {
            int i6 = bArr[i] & 255;
            if (i6 != 0 && i6 != 255) {
                switch (i6) {
                    case 127:
                        int i7 = ((bArr[i5] & 255) << 8) | (bArr[i5 + 1] & 255);
                        z = (32768 & i7) != 0;
                        i3 = i7 & (-32769);
                        i5 += 2;
                        break;
                    case 128:
                        break;
                    default:
                        z = (i6 & 128) != 0;
                        i3 = i6 & (-129);
                        break;
                }
                int i8 = i3;
                boolean z2 = z;
                i2 = i5 + 1;
                try {
                    int i9 = bArr[i5] & 255;
                    if (i9 >= 128) {
                        if (i9 == 129) {
                            int i10 = i2 + 1;
                            int i11 = bArr[i2] & 255;
                            if (i11 < 128) {
                                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD, "length < 0x80 length=" + Integer.toHexString(i11) + " startIndex=" + i + " curIndex=" + i10 + " endIndex=" + length);
                            }
                            i4 = i11;
                            i2 = i10;
                        } else {
                            if (i9 == 130) {
                                i9 = ((bArr[i2] & 255) << 8) | (255 & bArr[i2 + 1]);
                                i2 += 2;
                                if (i9 < 256) {
                                    throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD, "two byte length < 0x100 length=" + Integer.toHexString(i9) + " startIndex=" + i + " curIndex=" + i2 + " endIndex=" + length);
                                }
                            } else if (i9 == 131) {
                                i9 = ((bArr[i2] & 255) << 16) | ((bArr[i2 + 1] & 255) << 8) | (255 & bArr[i2 + 2]);
                                i2 += 3;
                                if (i9 < 65536) {
                                    throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD, "three byte length < 0x10000 length=0x" + Integer.toHexString(i9) + " startIndex=" + i + " curIndex=" + i2 + " endIndex=" + length);
                                }
                            } else {
                                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD, "Bad length modifer=" + i9 + " startIndex=" + i + " curIndex=" + i2 + " endIndex=" + length);
                            }
                            i4 = i9;
                        }
                    } else {
                        i4 = i9;
                    }
                    return new ComprehensionTlv(i8, z2, i4, bArr, i2);
                } catch (IndexOutOfBoundsException e) {
                    throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD, "IndexOutOfBoundsException startIndex=" + i + " curIndex=" + i2 + " endIndex=" + length);
                }
            }
            Rlog.d("CAT     ", "decode: unexpected first tag byte=" + Integer.toHexString(i6) + ", startIndex=" + i + " curIndex=" + i5 + " endIndex=" + length);
            return null;
        } catch (IndexOutOfBoundsException e2) {
            i2 = i5;
        }
    }
}
