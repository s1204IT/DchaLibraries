package com.android.org.bouncycastle.math.ec;

import java.math.BigInteger;

public abstract class WNafUtil {
    private static int[] DEFAULT_WINDOW_SIZE_CUTOFFS = {13, 41, 121, 337, 897, 2305};

    public static int[] generateCompactNaf(BigInteger k) {
        int length;
        if ((k.bitLength() >>> 16) != 0) {
            throw new IllegalArgumentException("'k' must have bitlength < 2^16");
        }
        BigInteger _3k = k.shiftLeft(1).add(k);
        int digits = _3k.bitLength() - 1;
        int[] naf = new int[(digits + 1) >> 1];
        int zeroes = 0;
        int i = 1;
        int length2 = 0;
        while (i <= digits) {
            boolean _3kBit = _3k.testBit(i);
            boolean kBit = k.testBit(i);
            if (_3kBit == kBit) {
                zeroes++;
                length = length2;
            } else {
                int digit = kBit ? -1 : 1;
                length = length2 + 1;
                naf[length2] = (digit << 16) | zeroes;
                zeroes = 0;
            }
            i++;
            length2 = length;
        }
        if (naf.length > length2) {
            return trim(naf, length2);
        }
        return naf;
    }

    public static int[] generateCompactWindowNaf(int width, BigInteger k) {
        if (width == 2) {
            return generateCompactNaf(k);
        }
        if (width < 2 || width > 16) {
            throw new IllegalArgumentException("'width' must be in the range [2, 16]");
        }
        if ((k.bitLength() >>> 16) != 0) {
            throw new IllegalArgumentException("'k' must have bitlength < 2^16");
        }
        int[] wnaf = new int[(k.bitLength() / width) + 1];
        int pow2 = 1 << width;
        int mask = pow2 - 1;
        int sign = pow2 >>> 1;
        boolean carry = false;
        int length = 0;
        int pos = 0;
        while (pos <= k.bitLength()) {
            if (k.testBit(pos) == carry) {
                pos++;
            } else {
                k = k.shiftRight(pos);
                int digit = k.intValue() & mask;
                if (carry) {
                    digit++;
                }
                carry = (digit & sign) != 0;
                if (carry) {
                    digit -= pow2;
                }
                int zeroes = length > 0 ? pos - 1 : pos;
                wnaf[length] = (digit << 16) | zeroes;
                pos = width;
                length++;
            }
        }
        if (wnaf.length > length) {
            return trim(wnaf, length);
        }
        return wnaf;
    }

    public static byte[] generateJSF(BigInteger g, BigInteger h) {
        int digits = Math.max(g.bitLength(), h.bitLength()) + 1;
        byte[] jsf = new byte[digits];
        BigInteger k0 = g;
        BigInteger k1 = h;
        int j = 0;
        int d0 = 0;
        int d1 = 0;
        while (true) {
            if (k0.signum() <= 0 && k1.signum() <= 0 && d0 <= 0 && d1 <= 0) {
                break;
            }
            int n0 = (k0.intValue() + d0) & 7;
            int n1 = (k1.intValue() + d1) & 7;
            int u0 = n0 & 1;
            if (u0 != 0) {
                u0 -= n0 & 2;
                if (n0 + u0 == 4 && (n1 & 3) == 2) {
                    u0 = -u0;
                }
            }
            int u1 = n1 & 1;
            if (u1 != 0) {
                u1 -= n1 & 2;
                if (n1 + u1 == 4 && (n0 & 3) == 2) {
                    u1 = -u1;
                }
            }
            if ((d0 << 1) == u0 + 1) {
                d0 = 1 - d0;
            }
            if ((d1 << 1) == u1 + 1) {
                d1 = 1 - d1;
            }
            k0 = k0.shiftRight(1);
            k1 = k1.shiftRight(1);
            jsf[j] = (byte) ((u0 << 4) | (u1 & 15));
            j++;
        }
        if (jsf.length > j) {
            return trim(jsf, j);
        }
        return jsf;
    }

    public static byte[] generateNaf(BigInteger k) {
        int i;
        BigInteger _3k = k.shiftLeft(1).add(k);
        int digits = _3k.bitLength() - 1;
        byte[] naf = new byte[digits];
        for (int i2 = 1; i2 <= digits; i2++) {
            boolean _3kBit = _3k.testBit(i2);
            boolean kBit = k.testBit(i2);
            int i3 = i2 - 1;
            if (_3kBit == kBit) {
                i = 0;
            } else {
                i = kBit ? -1 : 1;
            }
            naf[i3] = (byte) i;
        }
        return naf;
    }

    public static byte[] generateWindowNaf(int width, BigInteger k) {
        if (width == 2) {
            return generateNaf(k);
        }
        if (width < 2 || width > 8) {
            throw new IllegalArgumentException("'width' must be in the range [2, 8]");
        }
        byte[] wnaf = new byte[k.bitLength() + 1];
        int pow2 = 1 << width;
        int mask = pow2 - 1;
        int sign = pow2 >>> 1;
        boolean carry = false;
        int length = 0;
        int pos = 0;
        while (pos <= k.bitLength()) {
            if (k.testBit(pos) == carry) {
                pos++;
            } else {
                k = k.shiftRight(pos);
                int digit = k.intValue() & mask;
                if (carry) {
                    digit++;
                }
                carry = (digit & sign) != 0;
                if (carry) {
                    digit -= pow2;
                }
                if (length > 0) {
                    pos--;
                }
                int length2 = length + pos;
                wnaf[length2] = (byte) digit;
                pos = width;
                length = length2 + 1;
            }
        }
        if (wnaf.length > length) {
            return trim(wnaf, length);
        }
        return wnaf;
    }

    public static WNafPreCompInfo getWNafPreCompInfo(PreCompInfo preCompInfo) {
        return (preCompInfo == null || !(preCompInfo instanceof WNafPreCompInfo)) ? new WNafPreCompInfo() : (WNafPreCompInfo) preCompInfo;
    }

    public static int getWindowSize(int bits) {
        return getWindowSize(bits, DEFAULT_WINDOW_SIZE_CUTOFFS);
    }

    public static int getWindowSize(int bits, int[] windowSizeCutoffs) {
        int w = 0;
        while (w < windowSizeCutoffs.length && bits >= windowSizeCutoffs[w]) {
            w++;
        }
        return w + 2;
    }

    public static WNafPreCompInfo precompute(ECPoint p, int width, boolean includeNegated) {
        int pos;
        ECCurve c = p.getCurve();
        WNafPreCompInfo wnafPreCompInfo = getWNafPreCompInfo(c.getPreCompInfo(p));
        ECPoint[] preComp = wnafPreCompInfo.getPreComp();
        if (preComp == null) {
            preComp = new ECPoint[]{p};
        }
        int preCompLen = preComp.length;
        int reqPreCompLen = 1 << Math.max(0, width - 2);
        if (preCompLen < reqPreCompLen) {
            ECPoint twiceP = wnafPreCompInfo.getTwiceP();
            if (twiceP == null) {
                twiceP = preComp[0].twice().normalize();
                wnafPreCompInfo.setTwiceP(twiceP);
            }
            preComp = resizeTable(preComp, reqPreCompLen);
            for (int i = preCompLen; i < reqPreCompLen; i++) {
                preComp[i] = twiceP.add(preComp[i - 1]);
            }
            c.normalizeAll(preComp);
        }
        wnafPreCompInfo.setPreComp(preComp);
        if (includeNegated) {
            ECPoint[] preCompNeg = wnafPreCompInfo.getPreCompNeg();
            if (preCompNeg == null) {
                pos = 0;
                preCompNeg = new ECPoint[reqPreCompLen];
            } else {
                pos = preCompNeg.length;
                if (pos < reqPreCompLen) {
                    preCompNeg = resizeTable(preCompNeg, reqPreCompLen);
                }
            }
            while (pos < reqPreCompLen) {
                preCompNeg[pos] = preComp[pos].negate();
                pos++;
            }
            wnafPreCompInfo.setPreCompNeg(preCompNeg);
        }
        c.setPreCompInfo(p, wnafPreCompInfo);
        return wnafPreCompInfo;
    }

    private static byte[] trim(byte[] a, int length) {
        byte[] result = new byte[length];
        System.arraycopy(a, 0, result, 0, result.length);
        return result;
    }

    private static int[] trim(int[] a, int length) {
        int[] result = new int[length];
        System.arraycopy(a, 0, result, 0, result.length);
        return result;
    }

    private static ECPoint[] resizeTable(ECPoint[] a, int length) {
        ECPoint[] result = new ECPoint[length];
        System.arraycopy(a, 0, result, 0, a.length);
        return result;
    }
}
