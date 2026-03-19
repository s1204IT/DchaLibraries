package java.security.spec;

import java.math.BigInteger;
import java.util.Arrays;

public class ECFieldF2m implements ECField {
    private int[] ks;
    private int m;
    private BigInteger rp;

    public ECFieldF2m(int i) {
        if (i <= 0) {
            throw new IllegalArgumentException("m is not positive");
        }
        this.m = i;
        this.ks = null;
        this.rp = null;
    }

    public ECFieldF2m(int i, BigInteger bigInteger) {
        this.m = i;
        this.rp = bigInteger;
        if (i <= 0) {
            throw new IllegalArgumentException("m is not positive");
        }
        int iBitCount = this.rp.bitCount();
        if (!this.rp.testBit(0) || !this.rp.testBit(i) || (iBitCount != 3 && iBitCount != 5)) {
            throw new IllegalArgumentException("rp does not represent a valid reduction polynomial");
        }
        BigInteger bigIntegerClearBit = this.rp.clearBit(0).clearBit(i);
        this.ks = new int[iBitCount - 2];
        for (int length = this.ks.length - 1; length >= 0; length--) {
            int lowestSetBit = bigIntegerClearBit.getLowestSetBit();
            this.ks[length] = lowestSetBit;
            bigIntegerClearBit = bigIntegerClearBit.clearBit(lowestSetBit);
        }
    }

    public ECFieldF2m(int i, int[] iArr) {
        this.m = i;
        this.ks = (int[]) iArr.clone();
        if (i <= 0) {
            throw new IllegalArgumentException("m is not positive");
        }
        if (this.ks.length != 1 && this.ks.length != 3) {
            throw new IllegalArgumentException("length of ks is neither 1 nor 3");
        }
        for (int i2 = 0; i2 < this.ks.length; i2++) {
            if (this.ks[i2] < 1 || this.ks[i2] > i - 1) {
                throw new IllegalArgumentException("ks[" + i2 + "] is out of range");
            }
            if (i2 != 0 && this.ks[i2] >= this.ks[i2 - 1]) {
                throw new IllegalArgumentException("values in ks are not in descending order");
            }
        }
        this.rp = BigInteger.ONE;
        this.rp = this.rp.setBit(i);
        for (int i3 = 0; i3 < this.ks.length; i3++) {
            this.rp = this.rp.setBit(this.ks[i3]);
        }
    }

    @Override
    public int getFieldSize() {
        return this.m;
    }

    public int getM() {
        return this.m;
    }

    public BigInteger getReductionPolynomial() {
        return this.rp;
    }

    public int[] getMidTermsOfReductionPolynomial() {
        if (this.ks == null) {
            return null;
        }
        return (int[]) this.ks.clone();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ECFieldF2m)) {
            return false;
        }
        ECFieldF2m eCFieldF2m = (ECFieldF2m) obj;
        return this.m == eCFieldF2m.m && Arrays.equals(this.ks, eCFieldF2m.ks);
    }

    public int hashCode() {
        return (this.m << 5) + (this.rp == null ? 0 : this.rp.hashCode());
    }
}
