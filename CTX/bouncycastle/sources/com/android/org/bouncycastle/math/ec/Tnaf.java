package com.android.org.bouncycastle.math.ec;

import com.android.org.bouncycastle.math.ec.ECCurve;
import com.android.org.bouncycastle.math.ec.ECPoint;
import java.math.BigInteger;

class Tnaf {
    public static final byte POW_2_WIDTH = 16;
    public static final byte WIDTH = 4;
    private static final BigInteger MINUS_ONE = ECConstants.ONE.negate();
    private static final BigInteger MINUS_TWO = ECConstants.TWO.negate();
    private static final BigInteger MINUS_THREE = ECConstants.THREE.negate();
    public static final ZTauElement[] alpha0 = {null, new ZTauElement(ECConstants.ONE, ECConstants.ZERO), null, new ZTauElement(MINUS_THREE, MINUS_ONE), null, new ZTauElement(MINUS_ONE, MINUS_ONE), null, new ZTauElement(ECConstants.ONE, MINUS_ONE), null};
    public static final byte[][] alpha0Tnaf = {null, new byte[]{1}, null, new byte[]{-1, 0, 1}, null, new byte[]{1, 0, 1}, null, new byte[]{-1, 0, 0, 1}};
    public static final ZTauElement[] alpha1 = {null, new ZTauElement(ECConstants.ONE, ECConstants.ZERO), null, new ZTauElement(MINUS_THREE, ECConstants.ONE), null, new ZTauElement(MINUS_ONE, ECConstants.ONE), null, new ZTauElement(ECConstants.ONE, ECConstants.ONE), null};
    public static final byte[][] alpha1Tnaf = {null, new byte[]{1}, null, new byte[]{-1, 0, 1}, null, new byte[]{1, 0, 1}, null, new byte[]{-1, 0, 0, -1}};

    Tnaf() {
    }

    public static BigInteger norm(byte b, ZTauElement zTauElement) {
        BigInteger bigIntegerMultiply = zTauElement.u.multiply(zTauElement.u);
        BigInteger bigIntegerMultiply2 = zTauElement.u.multiply(zTauElement.v);
        BigInteger bigIntegerShiftLeft = zTauElement.v.multiply(zTauElement.v).shiftLeft(1);
        if (b == 1) {
            return bigIntegerMultiply.add(bigIntegerMultiply2).add(bigIntegerShiftLeft);
        }
        if (b == -1) {
            return bigIntegerMultiply.subtract(bigIntegerMultiply2).add(bigIntegerShiftLeft);
        }
        throw new IllegalArgumentException("mu must be 1 or -1");
    }

    public static SimpleBigDecimal norm(byte b, SimpleBigDecimal simpleBigDecimal, SimpleBigDecimal simpleBigDecimal2) {
        SimpleBigDecimal simpleBigDecimalMultiply = simpleBigDecimal.multiply(simpleBigDecimal);
        SimpleBigDecimal simpleBigDecimalMultiply2 = simpleBigDecimal.multiply(simpleBigDecimal2);
        SimpleBigDecimal simpleBigDecimalShiftLeft = simpleBigDecimal2.multiply(simpleBigDecimal2).shiftLeft(1);
        if (b == 1) {
            return simpleBigDecimalMultiply.add(simpleBigDecimalMultiply2).add(simpleBigDecimalShiftLeft);
        }
        if (b == -1) {
            return simpleBigDecimalMultiply.subtract(simpleBigDecimalMultiply2).add(simpleBigDecimalShiftLeft);
        }
        throw new IllegalArgumentException("mu must be 1 or -1");
    }

    public static ZTauElement round(SimpleBigDecimal simpleBigDecimal, SimpleBigDecimal simpleBigDecimal2, byte b) {
        SimpleBigDecimal simpleBigDecimalSubtract;
        SimpleBigDecimal simpleBigDecimalAdd;
        SimpleBigDecimal simpleBigDecimalSubtract2;
        byte b2;
        if (simpleBigDecimal2.getScale() != simpleBigDecimal.getScale()) {
            throw new IllegalArgumentException("lambda0 and lambda1 do not have same scale");
        }
        if (b != 1 && b != -1) {
            throw new IllegalArgumentException("mu must be 1 or -1");
        }
        BigInteger bigIntegerRound = simpleBigDecimal.round();
        BigInteger bigIntegerRound2 = simpleBigDecimal2.round();
        SimpleBigDecimal simpleBigDecimalSubtract3 = simpleBigDecimal.subtract(bigIntegerRound);
        SimpleBigDecimal simpleBigDecimalSubtract4 = simpleBigDecimal2.subtract(bigIntegerRound2);
        SimpleBigDecimal simpleBigDecimalAdd2 = simpleBigDecimalSubtract3.add(simpleBigDecimalSubtract3);
        if (b == 1) {
            simpleBigDecimalSubtract = simpleBigDecimalAdd2.add(simpleBigDecimalSubtract4);
        } else {
            simpleBigDecimalSubtract = simpleBigDecimalAdd2.subtract(simpleBigDecimalSubtract4);
        }
        SimpleBigDecimal simpleBigDecimalAdd3 = simpleBigDecimalSubtract4.add(simpleBigDecimalSubtract4).add(simpleBigDecimalSubtract4);
        SimpleBigDecimal simpleBigDecimalAdd4 = simpleBigDecimalAdd3.add(simpleBigDecimalSubtract4);
        if (b == 1) {
            simpleBigDecimalAdd = simpleBigDecimalSubtract3.subtract(simpleBigDecimalAdd3);
            simpleBigDecimalSubtract2 = simpleBigDecimalSubtract3.add(simpleBigDecimalAdd4);
        } else {
            simpleBigDecimalAdd = simpleBigDecimalSubtract3.add(simpleBigDecimalAdd3);
            simpleBigDecimalSubtract2 = simpleBigDecimalSubtract3.subtract(simpleBigDecimalAdd4);
        }
        int i = 0;
        if (simpleBigDecimalSubtract.compareTo(ECConstants.ONE) >= 0) {
            if (simpleBigDecimalAdd.compareTo(MINUS_ONE) < 0) {
                b2 = b;
            } else {
                b2 = 0;
                i = 1;
            }
        } else if (simpleBigDecimalSubtract2.compareTo(ECConstants.TWO) < 0) {
            b2 = 0;
        }
        if (simpleBigDecimalSubtract.compareTo(MINUS_ONE) < 0) {
            if (simpleBigDecimalAdd.compareTo(ECConstants.ONE) >= 0) {
                b2 = (byte) (-b);
            } else {
                i = -1;
            }
        } else if (simpleBigDecimalSubtract2.compareTo(MINUS_TWO) < 0) {
            b2 = (byte) (-b);
        }
        return new ZTauElement(bigIntegerRound.add(BigInteger.valueOf(i)), bigIntegerRound2.add(BigInteger.valueOf(b2)));
    }

    public static SimpleBigDecimal approximateDivisionByN(BigInteger bigInteger, BigInteger bigInteger2, BigInteger bigInteger3, byte b, int i, int i2) {
        BigInteger bigIntegerMultiply = bigInteger2.multiply(bigInteger.shiftRight(((i - r0) - 2) + b));
        BigInteger bigIntegerAdd = bigIntegerMultiply.add(bigInteger3.multiply(bigIntegerMultiply.shiftRight(i)));
        int i3 = (((i + 5) / 2) + i2) - i2;
        BigInteger bigIntegerShiftRight = bigIntegerAdd.shiftRight(i3);
        if (bigIntegerAdd.testBit(i3 - 1)) {
            bigIntegerShiftRight = bigIntegerShiftRight.add(ECConstants.ONE);
        }
        return new SimpleBigDecimal(bigIntegerShiftRight, i2);
    }

    public static byte[] tauAdicNaf(byte b, ZTauElement zTauElement) {
        BigInteger bigIntegerSubtract;
        if (b != 1 && b != -1) {
            throw new IllegalArgumentException("mu must be 1 or -1");
        }
        int iBitLength = norm(b, zTauElement).bitLength();
        byte[] bArr = new byte[iBitLength > 30 ? iBitLength + 4 : 34];
        BigInteger bigIntegerAdd = zTauElement.u;
        BigInteger bigInteger = zTauElement.v;
        int i = 0;
        int i2 = 0;
        while (true) {
            if (!bigIntegerAdd.equals(ECConstants.ZERO) || !bigInteger.equals(ECConstants.ZERO)) {
                if (bigIntegerAdd.testBit(0)) {
                    bArr[i2] = (byte) ECConstants.TWO.subtract(bigIntegerAdd.subtract(bigInteger.shiftLeft(1)).mod(ECConstants.FOUR)).intValue();
                    if (bArr[i2] == 1) {
                        bigIntegerAdd = bigIntegerAdd.clearBit(0);
                    } else {
                        bigIntegerAdd = bigIntegerAdd.add(ECConstants.ONE);
                    }
                    i = i2;
                } else {
                    bArr[i2] = 0;
                }
                BigInteger bigIntegerShiftRight = bigIntegerAdd.shiftRight(1);
                if (b == 1) {
                    bigIntegerSubtract = bigInteger.add(bigIntegerShiftRight);
                } else {
                    bigIntegerSubtract = bigInteger.subtract(bigIntegerShiftRight);
                }
                BigInteger bigIntegerNegate = bigIntegerAdd.shiftRight(1).negate();
                i2++;
                bigIntegerAdd = bigIntegerSubtract;
                bigInteger = bigIntegerNegate;
            } else {
                int i3 = i + 1;
                byte[] bArr2 = new byte[i3];
                System.arraycopy(bArr, 0, bArr2, 0, i3);
                return bArr2;
            }
        }
    }

    public static ECPoint.AbstractF2m tau(ECPoint.AbstractF2m abstractF2m) {
        return abstractF2m.tau();
    }

    public static byte getMu(ECCurve.AbstractF2m abstractF2m) {
        if (!abstractF2m.isKoblitz()) {
            throw new IllegalArgumentException("No Koblitz curve (ABC), TNAF multiplication not possible");
        }
        if (abstractF2m.getA().isZero()) {
            return (byte) -1;
        }
        return (byte) 1;
    }

    public static byte getMu(ECFieldElement eCFieldElement) {
        return (byte) (eCFieldElement.isZero() ? -1 : 1);
    }

    public static byte getMu(int i) {
        return (byte) (i == 0 ? -1 : 1);
    }

    public static BigInteger[] getLucas(byte b, int i, boolean z) {
        BigInteger bigInteger;
        BigInteger bigIntegerValueOf;
        BigInteger bigIntegerNegate;
        if (b != 1 && b != -1) {
            throw new IllegalArgumentException("mu must be 1 or -1");
        }
        if (z) {
            bigInteger = ECConstants.TWO;
            bigIntegerValueOf = BigInteger.valueOf(b);
        } else {
            bigInteger = ECConstants.ZERO;
            bigIntegerValueOf = ECConstants.ONE;
        }
        BigInteger bigIntegerSubtract = bigIntegerValueOf;
        BigInteger bigInteger2 = bigInteger;
        int i2 = 1;
        while (i2 < i) {
            if (b != 1) {
                bigIntegerNegate = bigIntegerSubtract.negate();
            } else {
                bigIntegerNegate = bigIntegerSubtract;
            }
            i2++;
            BigInteger bigInteger3 = bigIntegerSubtract;
            bigIntegerSubtract = bigIntegerNegate.subtract(bigInteger2.shiftLeft(1));
            bigInteger2 = bigInteger3;
        }
        return new BigInteger[]{bigInteger2, bigIntegerSubtract};
    }

    public static BigInteger getTw(byte b, int i) {
        if (i == 4) {
            if (b == 1) {
                return BigInteger.valueOf(6L);
            }
            return BigInteger.valueOf(10L);
        }
        BigInteger[] lucas = getLucas(b, i, false);
        BigInteger bit = ECConstants.ZERO.setBit(i);
        return ECConstants.TWO.multiply(lucas[0]).multiply(lucas[1].modInverse(bit)).mod(bit);
    }

    public static BigInteger[] getSi(ECCurve.AbstractF2m abstractF2m) {
        if (!abstractF2m.isKoblitz()) {
            throw new IllegalArgumentException("si is defined for Koblitz curves only");
        }
        int fieldSize = abstractF2m.getFieldSize();
        int iIntValue = abstractF2m.getA().toBigInteger().intValue();
        byte mu = getMu(iIntValue);
        int shiftsForCofactor = getShiftsForCofactor(abstractF2m.getCofactor());
        BigInteger[] lucas = getLucas(mu, (fieldSize + 3) - iIntValue, false);
        if (mu == 1) {
            lucas[0] = lucas[0].negate();
            lucas[1] = lucas[1].negate();
        }
        return new BigInteger[]{ECConstants.ONE.add(lucas[1]).shiftRight(shiftsForCofactor), ECConstants.ONE.add(lucas[0]).shiftRight(shiftsForCofactor).negate()};
    }

    public static BigInteger[] getSi(int i, int i2, BigInteger bigInteger) {
        byte mu = getMu(i2);
        int shiftsForCofactor = getShiftsForCofactor(bigInteger);
        BigInteger[] lucas = getLucas(mu, (i + 3) - i2, false);
        if (mu == 1) {
            lucas[0] = lucas[0].negate();
            lucas[1] = lucas[1].negate();
        }
        return new BigInteger[]{ECConstants.ONE.add(lucas[1]).shiftRight(shiftsForCofactor), ECConstants.ONE.add(lucas[0]).shiftRight(shiftsForCofactor).negate()};
    }

    protected static int getShiftsForCofactor(BigInteger bigInteger) {
        if (bigInteger != null) {
            if (bigInteger.equals(ECConstants.TWO)) {
                return 1;
            }
            if (bigInteger.equals(ECConstants.FOUR)) {
                return 2;
            }
        }
        throw new IllegalArgumentException("h (Cofactor) must be 2 or 4");
    }

    public static ZTauElement partModReduction(BigInteger bigInteger, int i, byte b, BigInteger[] bigIntegerArr, byte b2, byte b3) {
        BigInteger bigIntegerSubtract;
        if (b2 == 1) {
            bigIntegerSubtract = bigIntegerArr[0].add(bigIntegerArr[1]);
        } else {
            bigIntegerSubtract = bigIntegerArr[0].subtract(bigIntegerArr[1]);
        }
        BigInteger bigInteger2 = getLucas(b2, i, true)[1];
        ZTauElement zTauElementRound = round(approximateDivisionByN(bigInteger, bigIntegerArr[0], bigInteger2, b, i, b3), approximateDivisionByN(bigInteger, bigIntegerArr[1], bigInteger2, b, i, b3), b2);
        return new ZTauElement(bigInteger.subtract(bigIntegerSubtract.multiply(zTauElementRound.u)).subtract(BigInteger.valueOf(2L).multiply(bigIntegerArr[1]).multiply(zTauElementRound.v)), bigIntegerArr[1].multiply(zTauElementRound.u).subtract(bigIntegerArr[0].multiply(zTauElementRound.v)));
    }

    public static ECPoint.AbstractF2m multiplyRTnaf(ECPoint.AbstractF2m abstractF2m, BigInteger bigInteger) {
        ECCurve.AbstractF2m abstractF2m2 = (ECCurve.AbstractF2m) abstractF2m.getCurve();
        int fieldSize = abstractF2m2.getFieldSize();
        int iIntValue = abstractF2m2.getA().toBigInteger().intValue();
        return multiplyTnaf(abstractF2m, partModReduction(bigInteger, fieldSize, (byte) iIntValue, abstractF2m2.getSi(), getMu(iIntValue), (byte) 10));
    }

    public static ECPoint.AbstractF2m multiplyTnaf(ECPoint.AbstractF2m abstractF2m, ZTauElement zTauElement) {
        return multiplyFromTnaf(abstractF2m, tauAdicNaf(getMu(((ECCurve.AbstractF2m) abstractF2m.getCurve()).getA()), zTauElement));
    }

    public static ECPoint.AbstractF2m multiplyFromTnaf(ECPoint.AbstractF2m abstractF2m, byte[] bArr) {
        ECPoint.AbstractF2m abstractF2m2 = (ECPoint.AbstractF2m) abstractF2m.getCurve().getInfinity();
        ECPoint.AbstractF2m abstractF2m3 = (ECPoint.AbstractF2m) abstractF2m.negate();
        ECPoint.AbstractF2m abstractF2m4 = abstractF2m2;
        int i = 0;
        for (int length = bArr.length - 1; length >= 0; length--) {
            i++;
            byte b = bArr[length];
            if (b != 0) {
                abstractF2m4 = (ECPoint.AbstractF2m) abstractF2m4.tauPow(i).add(b > 0 ? abstractF2m : abstractF2m3);
                i = 0;
            }
        }
        if (i > 0) {
            return abstractF2m4.tauPow(i);
        }
        return abstractF2m4;
    }

    public static byte[] tauAdicWNaf(byte b, ZTauElement zTauElement, byte b2, BigInteger bigInteger, BigInteger bigInteger2, ZTauElement[] zTauElementArr) {
        BigInteger bigIntegerSubtract;
        byte bIntValue;
        byte b3;
        boolean z;
        if (b != 1 && b != -1) {
            throw new IllegalArgumentException("mu must be 1 or -1");
        }
        int iBitLength = norm(b, zTauElement).bitLength();
        byte[] bArr = new byte[(iBitLength > 30 ? iBitLength + 4 : 34) + b2];
        BigInteger bigIntegerShiftRight = bigInteger.shiftRight(1);
        BigInteger bigIntegerAdd = zTauElement.u;
        BigInteger bigIntegerAdd2 = zTauElement.v;
        int i = 0;
        while (true) {
            if (!bigIntegerAdd.equals(ECConstants.ZERO) || !bigIntegerAdd2.equals(ECConstants.ZERO)) {
                if (bigIntegerAdd.testBit(0)) {
                    BigInteger bigIntegerMod = bigIntegerAdd.add(bigIntegerAdd2.multiply(bigInteger2)).mod(bigInteger);
                    if (bigIntegerMod.compareTo(bigIntegerShiftRight) >= 0) {
                        bIntValue = (byte) bigIntegerMod.subtract(bigInteger).intValue();
                    } else {
                        bIntValue = (byte) bigIntegerMod.intValue();
                    }
                    bArr[i] = bIntValue;
                    if (bIntValue >= 0) {
                        b3 = bIntValue;
                        z = true;
                    } else {
                        b3 = (byte) (-bIntValue);
                        z = false;
                    }
                    if (z) {
                        bigIntegerAdd = bigIntegerAdd.subtract(zTauElementArr[b3].u);
                        bigIntegerAdd2 = bigIntegerAdd2.subtract(zTauElementArr[b3].v);
                    } else {
                        bigIntegerAdd = bigIntegerAdd.add(zTauElementArr[b3].u);
                        bigIntegerAdd2 = bigIntegerAdd2.add(zTauElementArr[b3].v);
                    }
                } else {
                    bArr[i] = 0;
                }
                if (b == 1) {
                    bigIntegerSubtract = bigIntegerAdd2.add(bigIntegerAdd.shiftRight(1));
                } else {
                    bigIntegerSubtract = bigIntegerAdd2.subtract(bigIntegerAdd.shiftRight(1));
                }
                BigInteger bigIntegerNegate = bigIntegerAdd.shiftRight(1).negate();
                i++;
                bigIntegerAdd = bigIntegerSubtract;
                bigIntegerAdd2 = bigIntegerNegate;
            } else {
                return bArr;
            }
        }
    }

    public static ECPoint.AbstractF2m[] getPreComp(ECPoint.AbstractF2m abstractF2m, byte b) {
        byte[][] bArr = b == 0 ? alpha0Tnaf : alpha1Tnaf;
        ECPoint.AbstractF2m[] abstractF2mArr = new ECPoint.AbstractF2m[(bArr.length + 1) >>> 1];
        abstractF2mArr[0] = abstractF2m;
        int length = bArr.length;
        for (int i = 3; i < length; i += 2) {
            abstractF2mArr[i >>> 1] = multiplyFromTnaf(abstractF2m, bArr[i]);
        }
        abstractF2m.getCurve().normalizeAll(abstractF2mArr);
        return abstractF2mArr;
    }
}
