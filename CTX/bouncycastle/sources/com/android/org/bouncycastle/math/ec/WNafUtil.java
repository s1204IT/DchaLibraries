package com.android.org.bouncycastle.math.ec;

import java.math.BigInteger;

public abstract class WNafUtil {
    private static final int[] DEFAULT_WINDOW_SIZE_CUTOFFS = {13, 41, 121, 337, 897, 2305};
    private static final byte[] EMPTY_BYTES = new byte[0];
    private static final int[] EMPTY_INTS = new int[0];
    private static final ECPoint[] EMPTY_POINTS = new ECPoint[0];
    public static final String PRECOMP_NAME = "bc_wnaf";

    public static int[] generateCompactNaf(BigInteger bigInteger) {
        if ((bigInteger.bitLength() >>> 16) != 0) {
            throw new IllegalArgumentException("'k' must have bitlength < 2^16");
        }
        if (bigInteger.signum() == 0) {
            return EMPTY_INTS;
        }
        BigInteger bigIntegerAdd = bigInteger.shiftLeft(1).add(bigInteger);
        int iBitLength = bigIntegerAdd.bitLength();
        int[] iArr = new int[iBitLength >> 1];
        BigInteger bigIntegerXor = bigIntegerAdd.xor(bigInteger);
        int i = iBitLength - 1;
        int i2 = 0;
        int i3 = 0;
        int i4 = 1;
        while (i4 < i) {
            if (!bigIntegerXor.testBit(i4)) {
                i3++;
            } else {
                iArr[i2] = i3 | ((bigInteger.testBit(i4) ? -1 : 1) << 16);
                i4++;
                i3 = 1;
                i2++;
            }
            i4++;
        }
        int i5 = i2 + 1;
        iArr[i2] = 65536 | i3;
        if (iArr.length > i5) {
            return trim(iArr, i5);
        }
        return iArr;
    }

    public static int[] generateCompactWindowNaf(int i, BigInteger bigInteger) {
        if (i == 2) {
            return generateCompactNaf(bigInteger);
        }
        if (i < 2 || i > 16) {
            throw new IllegalArgumentException("'width' must be in the range [2, 16]");
        }
        if ((bigInteger.bitLength() >>> 16) != 0) {
            throw new IllegalArgumentException("'k' must have bitlength < 2^16");
        }
        if (bigInteger.signum() == 0) {
            return EMPTY_INTS;
        }
        int[] iArr = new int[(bigInteger.bitLength() / i) + 1];
        int i2 = 1 << i;
        int i3 = i2 - 1;
        int i4 = i2 >>> 1;
        BigInteger bigIntegerShiftRight = bigInteger;
        int i5 = 0;
        boolean z = false;
        int i6 = 0;
        while (i5 <= bigIntegerShiftRight.bitLength()) {
            if (bigIntegerShiftRight.testBit(i5) == z) {
                i5++;
            } else {
                bigIntegerShiftRight = bigIntegerShiftRight.shiftRight(i5);
                int iIntValue = bigIntegerShiftRight.intValue() & i3;
                if (z) {
                    iIntValue++;
                }
                z = (iIntValue & i4) != 0;
                if (z) {
                    iIntValue -= i2;
                }
                if (i6 > 0) {
                    i5--;
                }
                iArr[i6] = i5 | (iIntValue << 16);
                i5 = i;
                i6++;
            }
        }
        if (iArr.length > i6) {
            return trim(iArr, i6);
        }
        return iArr;
    }

    public static byte[] generateJSF(BigInteger bigInteger, BigInteger bigInteger2) {
        byte[] bArr = new byte[Math.max(bigInteger.bitLength(), bigInteger2.bitLength()) + 1];
        BigInteger bigIntegerShiftRight = bigInteger;
        BigInteger bigIntegerShiftRight2 = bigInteger2;
        int i = 0;
        int i2 = 0;
        int i3 = 0;
        int i4 = 0;
        while (true) {
            if ((i | i2) == 0 && bigIntegerShiftRight.bitLength() <= i3 && bigIntegerShiftRight2.bitLength() <= i3) {
                break;
            }
            int iIntValue = ((bigIntegerShiftRight.intValue() >>> i3) + i) & 7;
            int iIntValue2 = ((bigIntegerShiftRight2.intValue() >>> i3) + i2) & 7;
            int i5 = iIntValue & 1;
            if (i5 != 0) {
                i5 -= iIntValue & 2;
                if (iIntValue + i5 == 4 && (iIntValue2 & 3) == 2) {
                    i5 = -i5;
                }
            }
            int i6 = iIntValue2 & 1;
            if (i6 != 0) {
                i6 -= iIntValue2 & 2;
                if (iIntValue2 + i6 == 4 && (iIntValue & 3) == 2) {
                    i6 = -i6;
                }
            }
            if ((i << 1) == 1 + i5) {
                i ^= 1;
            }
            if ((i2 << 1) == 1 + i6) {
                i2 ^= 1;
            }
            i3++;
            if (i3 == 30) {
                bigIntegerShiftRight = bigIntegerShiftRight.shiftRight(30);
                bigIntegerShiftRight2 = bigIntegerShiftRight2.shiftRight(30);
                i3 = 0;
            }
            bArr[i4] = (byte) ((i5 << 4) | (i6 & 15));
            i4++;
        }
        if (bArr.length > i4) {
            return trim(bArr, i4);
        }
        return bArr;
    }

    public static byte[] generateNaf(BigInteger bigInteger) {
        if (bigInteger.signum() == 0) {
            return EMPTY_BYTES;
        }
        BigInteger bigIntegerAdd = bigInteger.shiftLeft(1).add(bigInteger);
        int iBitLength = bigIntegerAdd.bitLength() - 1;
        byte[] bArr = new byte[iBitLength];
        BigInteger bigIntegerXor = bigIntegerAdd.xor(bigInteger);
        int i = 1;
        while (i < iBitLength) {
            if (bigIntegerXor.testBit(i)) {
                bArr[i - 1] = (byte) (bigInteger.testBit(i) ? -1 : 1);
                i++;
            }
            i++;
        }
        bArr[iBitLength - 1] = 1;
        return bArr;
    }

    public static byte[] generateWindowNaf(int i, BigInteger bigInteger) {
        if (i == 2) {
            return generateNaf(bigInteger);
        }
        if (i < 2 || i > 8) {
            throw new IllegalArgumentException("'width' must be in the range [2, 8]");
        }
        if (bigInteger.signum() == 0) {
            return EMPTY_BYTES;
        }
        byte[] bArr = new byte[bigInteger.bitLength() + 1];
        int i2 = 1 << i;
        int i3 = i2 - 1;
        int i4 = i2 >>> 1;
        BigInteger bigIntegerShiftRight = bigInteger;
        int i5 = 0;
        boolean z = false;
        int i6 = 0;
        while (i5 <= bigIntegerShiftRight.bitLength()) {
            if (bigIntegerShiftRight.testBit(i5) == z) {
                i5++;
            } else {
                bigIntegerShiftRight = bigIntegerShiftRight.shiftRight(i5);
                int iIntValue = bigIntegerShiftRight.intValue() & i3;
                if (z) {
                    iIntValue++;
                }
                z = (iIntValue & i4) != 0;
                if (z) {
                    iIntValue -= i2;
                }
                if (i6 > 0) {
                    i5--;
                }
                int i7 = i6 + i5;
                bArr[i7] = (byte) iIntValue;
                i6 = i7 + 1;
                i5 = i;
            }
        }
        if (bArr.length > i6) {
            return trim(bArr, i6);
        }
        return bArr;
    }

    public static int getNafWeight(BigInteger bigInteger) {
        if (bigInteger.signum() == 0) {
            return 0;
        }
        return bigInteger.shiftLeft(1).add(bigInteger).xor(bigInteger).bitCount();
    }

    public static WNafPreCompInfo getWNafPreCompInfo(ECPoint eCPoint) {
        return getWNafPreCompInfo(eCPoint.getCurve().getPreCompInfo(eCPoint, PRECOMP_NAME));
    }

    public static WNafPreCompInfo getWNafPreCompInfo(PreCompInfo preCompInfo) {
        if (preCompInfo != null && (preCompInfo instanceof WNafPreCompInfo)) {
            return (WNafPreCompInfo) preCompInfo;
        }
        return new WNafPreCompInfo();
    }

    public static int getWindowSize(int i) {
        return getWindowSize(i, DEFAULT_WINDOW_SIZE_CUTOFFS);
    }

    public static int getWindowSize(int i, int[] iArr) {
        int i2 = 0;
        while (i2 < iArr.length && i >= iArr[i2]) {
            i2++;
        }
        return i2 + 2;
    }

    public static ECPoint mapPointWithPrecomp(ECPoint eCPoint, int i, boolean z, ECPointMap eCPointMap) {
        ECCurve curve = eCPoint.getCurve();
        WNafPreCompInfo wNafPreCompInfoPrecompute = precompute(eCPoint, i, z);
        ECPoint map = eCPointMap.map(eCPoint);
        WNafPreCompInfo wNafPreCompInfo = getWNafPreCompInfo(curve.getPreCompInfo(map, PRECOMP_NAME));
        ECPoint twice = wNafPreCompInfoPrecompute.getTwice();
        if (twice != null) {
            wNafPreCompInfo.setTwice(eCPointMap.map(twice));
        }
        ECPoint[] preComp = wNafPreCompInfoPrecompute.getPreComp();
        ECPoint[] eCPointArr = new ECPoint[preComp.length];
        for (int i2 = 0; i2 < preComp.length; i2++) {
            eCPointArr[i2] = eCPointMap.map(preComp[i2]);
        }
        wNafPreCompInfo.setPreComp(eCPointArr);
        if (z) {
            ECPoint[] eCPointArr2 = new ECPoint[eCPointArr.length];
            for (int i3 = 0; i3 < eCPointArr2.length; i3++) {
                eCPointArr2[i3] = eCPointArr[i3].negate();
            }
            wNafPreCompInfo.setPreCompNeg(eCPointArr2);
        }
        curve.setPreCompInfo(map, PRECOMP_NAME, wNafPreCompInfo);
        return map;
    }

    public static WNafPreCompInfo precompute(ECPoint eCPoint, int i, boolean z) {
        int length;
        int i2;
        ECCurve curve = eCPoint.getCurve();
        WNafPreCompInfo wNafPreCompInfo = getWNafPreCompInfo(curve.getPreCompInfo(eCPoint, PRECOMP_NAME));
        int length2 = 0;
        int iMax = 1 << Math.max(0, i - 2);
        ECPoint[] preComp = wNafPreCompInfo.getPreComp();
        if (preComp == null) {
            preComp = EMPTY_POINTS;
            length = 0;
        } else {
            length = preComp.length;
        }
        if (length < iMax) {
            preComp = resizeTable(preComp, iMax);
            if (iMax == 1) {
                preComp[0] = eCPoint.normalize();
            } else {
                if (length == 0) {
                    preComp[0] = eCPoint;
                    i2 = 1;
                } else {
                    i2 = length;
                }
                ECFieldElement zCoord = null;
                if (iMax == 2) {
                    preComp[1] = eCPoint.threeTimes();
                } else {
                    ECPoint twice = wNafPreCompInfo.getTwice();
                    ECPoint eCPointAdd = preComp[i2 - 1];
                    if (twice == null) {
                        twice = preComp[0].twice();
                        wNafPreCompInfo.setTwice(twice);
                        if (!twice.isInfinity() && ECAlgorithms.isFpCurve(curve) && curve.getFieldSize() >= 64) {
                            switch (curve.getCoordinateSystem()) {
                                case 2:
                                case 3:
                                case 4:
                                    zCoord = twice.getZCoord(0);
                                    twice = curve.createPoint(twice.getXCoord().toBigInteger(), twice.getYCoord().toBigInteger());
                                    ECFieldElement eCFieldElementSquare = zCoord.square();
                                    eCPointAdd = eCPointAdd.scaleX(eCFieldElementSquare).scaleY(eCFieldElementSquare.multiply(zCoord));
                                    if (length == 0) {
                                        preComp[0] = eCPointAdd;
                                    }
                                    break;
                            }
                        }
                    }
                    while (i2 < iMax) {
                        eCPointAdd = eCPointAdd.add(twice);
                        preComp[i2] = eCPointAdd;
                        i2++;
                    }
                }
                curve.normalizeAll(preComp, length, iMax - length, zCoord);
            }
        }
        wNafPreCompInfo.setPreComp(preComp);
        if (z) {
            ECPoint[] preCompNeg = wNafPreCompInfo.getPreCompNeg();
            if (preCompNeg == null) {
                preCompNeg = new ECPoint[iMax];
            } else {
                length2 = preCompNeg.length;
                if (length2 < iMax) {
                    preCompNeg = resizeTable(preCompNeg, iMax);
                }
            }
            while (length2 < iMax) {
                preCompNeg[length2] = preComp[length2].negate();
                length2++;
            }
            wNafPreCompInfo.setPreCompNeg(preCompNeg);
        }
        curve.setPreCompInfo(eCPoint, PRECOMP_NAME, wNafPreCompInfo);
        return wNafPreCompInfo;
    }

    private static byte[] trim(byte[] bArr, int i) {
        byte[] bArr2 = new byte[i];
        System.arraycopy(bArr, 0, bArr2, 0, bArr2.length);
        return bArr2;
    }

    private static int[] trim(int[] iArr, int i) {
        int[] iArr2 = new int[i];
        System.arraycopy(iArr, 0, iArr2, 0, iArr2.length);
        return iArr2;
    }

    private static ECPoint[] resizeTable(ECPoint[] eCPointArr, int i) {
        ECPoint[] eCPointArr2 = new ECPoint[i];
        System.arraycopy(eCPointArr, 0, eCPointArr2, 0, eCPointArr.length);
        return eCPointArr2;
    }
}
