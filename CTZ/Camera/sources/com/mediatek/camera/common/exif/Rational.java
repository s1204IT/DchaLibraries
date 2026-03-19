package com.mediatek.camera.common.exif;

public class Rational {
    private final long mDenominator;
    private final long mNumerator;

    public Rational(long j, long j2) {
        this.mNumerator = j;
        this.mDenominator = j2;
    }

    public long getNumerator() {
        return this.mNumerator;
    }

    public long getDenominator() {
        return this.mDenominator;
    }

    public boolean equals(Object obj) {
        if (obj == 0) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Rational) || this.mNumerator != obj.mNumerator || this.mDenominator != obj.mDenominator) {
            return false;
        }
        return true;
    }

    public String toString() {
        return this.mNumerator + "/" + this.mDenominator;
    }
}
