package com.android.se.security;

import com.android.se.internal.ByteArrayConverter;
import com.android.se.internal.Util;

public class ApduFilter {
    public static final int LENGTH = 8;
    protected byte[] mApdu;
    protected byte[] mMask;

    protected ApduFilter() {
    }

    public ApduFilter(byte[] bArr, byte[] bArr2) {
        if (bArr.length != 4) {
            throw new IllegalArgumentException("apdu length must be 4 bytes");
        }
        if (bArr2.length != 4) {
            throw new IllegalArgumentException("mask length must be 4 bytes");
        }
        this.mApdu = bArr;
        this.mMask = bArr2;
    }

    public ApduFilter(byte[] bArr) {
        if (bArr.length != 8) {
            throw new IllegalArgumentException("filter length must be 8 bytes");
        }
        this.mApdu = Util.getMid(bArr, 0, 4);
        this.mMask = Util.getMid(bArr, 4, 4);
    }

    public ApduFilter m0clone() {
        ApduFilter apduFilter = new ApduFilter();
        apduFilter.setApdu((byte[]) this.mApdu.clone());
        apduFilter.setMask((byte[]) this.mMask.clone());
        return apduFilter;
    }

    public byte[] getApdu() {
        return this.mApdu;
    }

    public void setApdu(byte[] bArr) {
        if (bArr.length != 4) {
            throw new IllegalArgumentException("apdu length must be 4 bytes");
        }
        this.mApdu = bArr;
    }

    public byte[] getMask() {
        return this.mMask;
    }

    public void setMask(byte[] bArr) {
        if (bArr.length != 4) {
            throw new IllegalArgumentException("mask length must be 4 bytes");
        }
        this.mMask = bArr;
    }

    public byte[] toBytes() {
        return Util.mergeBytes(this.mApdu, this.mMask);
    }

    public String toString() {
        return "APDU Filter [apdu=" + ByteArrayConverter.byteArrayToHexString(this.mApdu) + ", mask=" + ByteArrayConverter.byteArrayToHexString(this.mMask) + "]";
    }

    public int getLength() {
        return 8;
    }
}
