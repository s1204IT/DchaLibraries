package com.android.se.security.gpac;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public class BerTlv {
    private byte[] mRawData;
    private int mTag;
    private int mValueIndex;
    private int mValueLength;

    public BerTlv(byte[] bArr, int i, int i2, int i3) {
        this.mRawData = new byte[0];
        this.mTag = 0;
        this.mValueIndex = 0;
        this.mValueLength = 0;
        if (bArr != null) {
            this.mRawData = bArr;
        }
        this.mTag = i;
        this.mValueIndex = i2;
        this.mValueLength = i3;
    }

    public static String toHex(byte[] bArr) {
        StringBuilder sb = new StringBuilder(bArr.length * 2);
        for (byte b : bArr) {
            int i = b & 255;
            sb.append("0123456789abcdef".charAt(i >> 4));
            sb.append("0123456789abcdef".charAt(i & 15));
        }
        return sb.toString();
    }

    public static BerTlv decode(byte[] bArr, int i) throws ParserException {
        return decode(bArr, i, true);
    }

    public static BerTlv decode(byte[] bArr, int i, boolean z) throws ParserException {
        if (bArr == null || bArr.length == 0) {
            throw new ParserException("No data given!");
        }
        if (i < bArr.length) {
            int i2 = i + 1;
            int i3 = bArr[i] & 255;
            if (i3 == 223 || i3 == 255) {
                if (i2 < bArr.length) {
                    i3 = ((i3 & 255) << 8) | (bArr[i2] & 255);
                    i2++;
                } else {
                    throw new ParserException("Index " + i2 + " out of range! [0..[" + bArr.length);
                }
            }
            if (i2 < bArr.length) {
                int i4 = i2 + 1;
                int i5 = bArr[i2] & 255;
                if (i5 >= 128) {
                    if (i5 == 129) {
                        if (i4 < bArr.length) {
                            int i6 = i4 + 1;
                            int i7 = 255 & bArr[i4];
                            if (i7 < 128) {
                                throw new ParserException("Invalid TLV length encoding!");
                            }
                            if (z && bArr.length < i7 + i6) {
                                throw new ParserException("Not enough data provided!");
                            }
                            i4 = i6;
                            i5 = i7;
                        } else {
                            throw new ParserException("Index " + i4 + " out of range! [0..[" + bArr.length);
                        }
                    } else if (i5 == 130) {
                        int i8 = i4 + 1;
                        if (i8 < bArr.length) {
                            i5 = (bArr[i8] & 255) | ((bArr[i4] & 255) << 8);
                            i4 += 2;
                            if (i5 < 256) {
                                throw new ParserException("Invalid TLV length encoding!");
                            }
                            if (z && bArr.length < i5 + i4) {
                                throw new ParserException("Not enough data provided!");
                            }
                        } else {
                            throw new ParserException("Index out of range! [0..[" + bArr.length);
                        }
                    } else if (i5 == 131) {
                        int i9 = i4 + 2;
                        if (i9 < bArr.length) {
                            i5 = (bArr[i9] & 255) | ((bArr[i4] & 255) << 16) | ((bArr[i4 + 1] & 255) << 8);
                            i4 += 3;
                            if (i5 < 65536) {
                                throw new ParserException("Invalid TLV length encoding!");
                            }
                            if (z && bArr.length < i5 + i4) {
                                throw new ParserException("Not enough data provided!");
                            }
                        } else {
                            throw new ParserException("Index out of range! [0..[" + bArr.length);
                        }
                    } else {
                        throw new ParserException("Unsupported TLV length encoding!");
                    }
                }
                return new BerTlv(bArr, i3, i4, i5);
            }
            throw new ParserException("Index " + i2 + " out of range! [0..[" + bArr.length);
        }
        throw new ParserException("Index " + i + " out of range! [0..[" + bArr.length);
    }

    public static void encodeLength(int i, ByteArrayOutputStream byteArrayOutputStream) {
        if (i > 65535) {
            byteArrayOutputStream.write(131);
            byteArrayOutputStream.write((16711680 & i) >> 16);
            byteArrayOutputStream.write((65280 & i) >> 8);
            byteArrayOutputStream.write(i & 255);
            return;
        }
        if (i > 255) {
            byteArrayOutputStream.write(130);
            byteArrayOutputStream.write((65280 & i) >> 8);
            byteArrayOutputStream.write(i & 255);
        } else if (i > 127) {
            byteArrayOutputStream.write(129);
            byteArrayOutputStream.write(i & 255);
        } else {
            byteArrayOutputStream.write(i & 255);
        }
    }

    public void interpret() throws ParserException {
    }

    public void build(ByteArrayOutputStream byteArrayOutputStream) throws DO_Exception {
        if (this.mTag > 255) {
            byteArrayOutputStream.write((this.mTag & 65280) >> 8);
            byteArrayOutputStream.write(this.mTag & 255);
        } else {
            byteArrayOutputStream.write(this.mTag & 255);
        }
        encodeLength(this.mValueLength, byteArrayOutputStream);
        if (this.mValueLength > 0) {
            byteArrayOutputStream.write(this.mRawData, this.mValueIndex, this.mValueLength);
        }
    }

    public int getTag() {
        return this.mTag;
    }

    public int getValueIndex() {
        return this.mValueIndex;
    }

    public byte[] getValue() {
        if (this.mRawData == null || this.mValueLength == 0 || this.mValueIndex < 0 || this.mValueIndex > this.mRawData.length || this.mValueIndex + this.mValueLength > this.mRawData.length) {
            return new byte[0];
        }
        byte[] bArr = new byte[this.mValueLength];
        System.arraycopy(this.mRawData, this.mValueIndex, bArr, 0, this.mValueLength);
        return bArr;
    }

    protected byte[] getRawData() {
        return this.mRawData;
    }

    public int getValueLength() {
        return this.mValueLength;
    }

    public boolean equals(Object obj) {
        if ((obj instanceof BerTlv) && this.mTag == obj.mTag) {
            return Arrays.equals(getValue(), obj.getValue());
        }
        return false;
    }
}
