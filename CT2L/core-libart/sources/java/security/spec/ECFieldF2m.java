package java.security.spec;

import java.math.BigInteger;
import java.util.Arrays;

public class ECFieldF2m implements ECField {
    private static final int PPB_LEN = 5;
    private static final int PPB_MID_LEN = 3;
    private static final int TPB_LEN = 3;
    private static final int TPB_MID_LEN = 1;
    private final int[] ks;
    private final int m;
    private final BigInteger rp;

    public ECFieldF2m(int m) {
        this.m = m;
        if (this.m <= 0) {
            throw new IllegalArgumentException("m <= 0");
        }
        this.rp = null;
        this.ks = null;
    }

    public ECFieldF2m(int m, BigInteger rp) {
        this.m = m;
        if (this.m <= 0) {
            throw new IllegalArgumentException("m <= 0");
        }
        this.rp = rp;
        if (this.rp == null) {
            throw new NullPointerException("rp == null");
        }
        int rp_bc = this.rp.bitCount();
        if (this.rp.bitLength() != m + 1 || ((rp_bc != 3 && rp_bc != 5) || !this.rp.testBit(0) || !this.rp.testBit(m))) {
            throw new IllegalArgumentException("rp is invalid");
        }
        this.ks = new int[rp_bc - 2];
        BigInteger rpTmp = rp.clearBit(0);
        for (int i = this.ks.length - 1; i >= 0; i--) {
            this.ks[i] = rpTmp.getLowestSetBit();
            rpTmp = rpTmp.clearBit(this.ks[i]);
        }
    }

    public ECFieldF2m(int m, int[] ks) {
        this.m = m;
        if (this.m <= 0) {
            throw new IllegalArgumentException("m <= 0");
        }
        this.ks = new int[ks.length];
        System.arraycopy(ks, 0, this.ks, 0, this.ks.length);
        if (this.ks.length != 1 && this.ks.length != 3) {
            throw new IllegalArgumentException("the length of ks is invalid");
        }
        boolean checkFailed = false;
        int prev = this.m;
        int i = 0;
        while (true) {
            if (i >= this.ks.length) {
                break;
            }
            if (this.ks[i] < prev) {
                prev = this.ks[i];
                i++;
            } else {
                checkFailed = true;
                break;
            }
        }
        if (checkFailed || prev < 1) {
            throw new IllegalArgumentException("ks is invalid");
        }
        BigInteger rpTmp = BigInteger.ONE.setBit(this.m);
        for (int i2 = 0; i2 < this.ks.length; i2++) {
            rpTmp = rpTmp.setBit(this.ks[i2]);
        }
        this.rp = rpTmp;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ECFieldF2m) {
            ECFieldF2m o = (ECFieldF2m) obj;
            if (this.m == o.m) {
                if (this.rp == null) {
                    if (o.rp == null) {
                        return true;
                    }
                } else {
                    return Arrays.equals(this.ks, o.ks);
                }
            }
        }
        return false;
    }

    @Override
    public int getFieldSize() {
        return this.m;
    }

    public int getM() {
        return this.m;
    }

    public int[] getMidTermsOfReductionPolynomial() {
        if (this.ks == null) {
            return null;
        }
        int[] ret = new int[this.ks.length];
        System.arraycopy(this.ks, 0, ret, 0, ret.length);
        return ret;
    }

    public BigInteger getReductionPolynomial() {
        return this.rp;
    }

    public int hashCode() {
        return this.rp == null ? this.m : this.m + this.rp.hashCode();
    }
}
