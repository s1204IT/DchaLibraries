package com.android.bluetooth;

import com.android.bluetooth.map.BluetoothMapUtils;
import java.io.UnsupportedEncodingException;

public class SignedLongLong implements Comparable<SignedLongLong> {
    private long mLeastSigBits;
    private long mMostSigBits;

    public SignedLongLong(long j, long j2) {
        this.mMostSigBits = j2;
        this.mLeastSigBits = j;
    }

    public static SignedLongLong fromString(String str) throws UnsupportedEncodingException {
        long j;
        if (str == null) {
            throw new NullPointerException();
        }
        String strTrim = str.trim();
        int length = strTrim.length();
        if (length == 0 || length > 32) {
            throw new NumberFormatException("invalid string length: " + length);
        }
        if (length > 16) {
            int i = length - 16;
            String strSubstring = strTrim.substring(i, length);
            long longFromString = BluetoothMapUtils.getLongFromString(strTrim.substring(0, i));
            strTrim = strSubstring;
            j = longFromString;
        } else {
            j = 0;
        }
        return new SignedLongLong(BluetoothMapUtils.getLongFromString(strTrim), j);
    }

    @Override
    public int compareTo(SignedLongLong signedLongLong) {
        if (this.mMostSigBits != signedLongLong.mMostSigBits) {
            return this.mMostSigBits < signedLongLong.mMostSigBits ? -1 : 1;
        }
        if (this.mLeastSigBits == signedLongLong.mLeastSigBits) {
            return 0;
        }
        return this.mLeastSigBits < signedLongLong.mLeastSigBits ? -1 : 1;
    }

    public String toString() {
        return toHexString();
    }

    public String toHexString() {
        return BluetoothMapUtils.getLongLongAsString(this.mLeastSigBits, this.mMostSigBits);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SignedLongLong signedLongLong = (SignedLongLong) obj;
        if (this.mLeastSigBits == signedLongLong.mLeastSigBits && this.mMostSigBits == signedLongLong.mMostSigBits) {
            return true;
        }
        return false;
    }

    public long getMostSignificantBits() {
        return this.mMostSigBits;
    }

    public long getLeastSignificantBits() {
        return this.mLeastSigBits;
    }
}
