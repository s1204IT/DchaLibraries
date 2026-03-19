package com.android.org.bouncycastle.math.ec;

import com.android.org.bouncycastle.math.ec.ECFieldElement;
import java.math.BigInteger;
import java.util.Hashtable;

public abstract class ECPoint {
    protected static ECFieldElement[] EMPTY_ZS = new ECFieldElement[0];
    protected ECCurve curve;
    protected Hashtable preCompTable;
    protected boolean withCompression;
    protected ECFieldElement x;
    protected ECFieldElement y;
    protected ECFieldElement[] zs;

    public abstract ECPoint add(ECPoint eCPoint);

    protected abstract ECPoint detach();

    protected abstract boolean getCompressionYTilde();

    public abstract ECPoint negate();

    protected abstract boolean satisfiesCurveEquation();

    public abstract ECPoint subtract(ECPoint eCPoint);

    public abstract ECPoint twice();

    protected static ECFieldElement[] getInitialZCoords(ECCurve eCCurve) {
        int coordinateSystem;
        if (eCCurve != null) {
            coordinateSystem = eCCurve.getCoordinateSystem();
        } else {
            coordinateSystem = 0;
        }
        if (coordinateSystem == 0 || coordinateSystem == 5) {
            return EMPTY_ZS;
        }
        ECFieldElement eCFieldElementFromBigInteger = eCCurve.fromBigInteger(ECConstants.ONE);
        if (coordinateSystem != 6) {
            switch (coordinateSystem) {
                case 1:
                case 2:
                    break;
                case 3:
                    return new ECFieldElement[]{eCFieldElementFromBigInteger, eCFieldElementFromBigInteger, eCFieldElementFromBigInteger};
                case 4:
                    return new ECFieldElement[]{eCFieldElementFromBigInteger, eCCurve.getA()};
                default:
                    throw new IllegalArgumentException("unknown coordinate system");
            }
        }
        return new ECFieldElement[]{eCFieldElementFromBigInteger};
    }

    protected ECPoint(ECCurve eCCurve, ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2) {
        this(eCCurve, eCFieldElement, eCFieldElement2, getInitialZCoords(eCCurve));
    }

    protected ECPoint(ECCurve eCCurve, ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, ECFieldElement[] eCFieldElementArr) {
        this.preCompTable = null;
        this.curve = eCCurve;
        this.x = eCFieldElement;
        this.y = eCFieldElement2;
        this.zs = eCFieldElementArr;
    }

    protected boolean satisfiesCofactor() {
        BigInteger cofactor = this.curve.getCofactor();
        return cofactor == null || cofactor.equals(ECConstants.ONE) || !ECAlgorithms.referenceMultiply(this, cofactor).isInfinity();
    }

    public final ECPoint getDetachedPoint() {
        return normalize().detach();
    }

    public ECCurve getCurve() {
        return this.curve;
    }

    protected int getCurveCoordinateSystem() {
        if (this.curve == null) {
            return 0;
        }
        return this.curve.getCoordinateSystem();
    }

    public ECFieldElement getX() {
        return normalize().getXCoord();
    }

    public ECFieldElement getY() {
        return normalize().getYCoord();
    }

    public ECFieldElement getAffineXCoord() {
        checkNormalized();
        return getXCoord();
    }

    public ECFieldElement getAffineYCoord() {
        checkNormalized();
        return getYCoord();
    }

    public ECFieldElement getXCoord() {
        return this.x;
    }

    public ECFieldElement getYCoord() {
        return this.y;
    }

    public ECFieldElement getZCoord(int i) {
        if (i < 0 || i >= this.zs.length) {
            return null;
        }
        return this.zs[i];
    }

    public ECFieldElement[] getZCoords() {
        int length = this.zs.length;
        if (length == 0) {
            return EMPTY_ZS;
        }
        ECFieldElement[] eCFieldElementArr = new ECFieldElement[length];
        System.arraycopy(this.zs, 0, eCFieldElementArr, 0, length);
        return eCFieldElementArr;
    }

    public final ECFieldElement getRawXCoord() {
        return this.x;
    }

    public final ECFieldElement getRawYCoord() {
        return this.y;
    }

    protected final ECFieldElement[] getRawZCoords() {
        return this.zs;
    }

    protected void checkNormalized() {
        if (!isNormalized()) {
            throw new IllegalStateException("point not in normal form");
        }
    }

    public boolean isNormalized() {
        int curveCoordinateSystem = getCurveCoordinateSystem();
        return curveCoordinateSystem == 0 || curveCoordinateSystem == 5 || isInfinity() || this.zs[0].isOne();
    }

    public ECPoint normalize() {
        int curveCoordinateSystem;
        if (isInfinity() || (curveCoordinateSystem = getCurveCoordinateSystem()) == 0 || curveCoordinateSystem == 5) {
            return this;
        }
        ECFieldElement zCoord = getZCoord(0);
        if (zCoord.isOne()) {
            return this;
        }
        return normalize(zCoord.invert());
    }

    ECPoint normalize(ECFieldElement eCFieldElement) {
        int curveCoordinateSystem = getCurveCoordinateSystem();
        if (curveCoordinateSystem != 6) {
            switch (curveCoordinateSystem) {
                case 1:
                    break;
                case 2:
                case 3:
                case 4:
                    ECFieldElement eCFieldElementSquare = eCFieldElement.square();
                    return createScaledPoint(eCFieldElementSquare, eCFieldElementSquare.multiply(eCFieldElement));
                default:
                    throw new IllegalStateException("not a projective coordinate system");
            }
        }
        return createScaledPoint(eCFieldElement, eCFieldElement);
    }

    protected ECPoint createScaledPoint(ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2) {
        return getCurve().createRawPoint(getRawXCoord().multiply(eCFieldElement), getRawYCoord().multiply(eCFieldElement2), this.withCompression);
    }

    public boolean isInfinity() {
        return this.x == null || this.y == null || (this.zs.length > 0 && this.zs[0].isZero());
    }

    public boolean isCompressed() {
        return this.withCompression;
    }

    public boolean isValid() {
        return isInfinity() || getCurve() == null || (satisfiesCurveEquation() && satisfiesCofactor());
    }

    public ECPoint scaleX(ECFieldElement eCFieldElement) {
        if (isInfinity()) {
            return this;
        }
        return getCurve().createRawPoint(getRawXCoord().multiply(eCFieldElement), getRawYCoord(), getRawZCoords(), this.withCompression);
    }

    public ECPoint scaleY(ECFieldElement eCFieldElement) {
        if (isInfinity()) {
            return this;
        }
        return getCurve().createRawPoint(getRawXCoord(), getRawYCoord().multiply(eCFieldElement), getRawZCoords(), this.withCompression);
    }

    public boolean equals(ECPoint eCPoint) {
        boolean z;
        ECPoint eCPointNormalize;
        ECPoint eCPoint2;
        if (eCPoint == null) {
            return false;
        }
        ECCurve curve = getCurve();
        ECCurve curve2 = eCPoint.getCurve();
        if (curve != null) {
            z = false;
        } else {
            z = true;
        }
        boolean z2 = curve2 == null;
        boolean zIsInfinity = isInfinity();
        boolean zIsInfinity2 = eCPoint.isInfinity();
        if (zIsInfinity || zIsInfinity2) {
            if (!zIsInfinity || !zIsInfinity2) {
                return false;
            }
            if (!z && !z2 && !curve.equals(curve2)) {
                return false;
            }
            return true;
        }
        if (!z || !z2) {
            if (z) {
                eCPoint = eCPoint.normalize();
                eCPoint2 = eCPoint;
                eCPointNormalize = this;
            } else if (z2) {
                eCPoint2 = eCPoint;
                eCPointNormalize = normalize();
            } else {
                if (!curve.equals(curve2)) {
                    return false;
                }
                ECPoint[] eCPointArr = {this, curve.importPoint(eCPoint)};
                curve.normalizeAll(eCPointArr);
                eCPointNormalize = eCPointArr[0];
                eCPoint2 = eCPointArr[1];
            }
        } else {
            eCPoint2 = eCPoint;
            eCPointNormalize = this;
        }
        if (!eCPointNormalize.getXCoord().equals(eCPoint2.getXCoord()) || !eCPointNormalize.getYCoord().equals(eCPoint2.getYCoord())) {
            return false;
        }
        return true;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof ECPoint)) {
            return false;
        }
        return equals((ECPoint) obj);
    }

    public int hashCode() {
        ECCurve curve = getCurve();
        int i = curve == null ? 0 : ~curve.hashCode();
        if (!isInfinity()) {
            ECPoint eCPointNormalize = normalize();
            return (i ^ (eCPointNormalize.getXCoord().hashCode() * 17)) ^ (eCPointNormalize.getYCoord().hashCode() * 257);
        }
        return i;
    }

    public String toString() {
        if (isInfinity()) {
            return "INF";
        }
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append('(');
        stringBuffer.append(getRawXCoord());
        stringBuffer.append(',');
        stringBuffer.append(getRawYCoord());
        for (int i = 0; i < this.zs.length; i++) {
            stringBuffer.append(',');
            stringBuffer.append(this.zs[i]);
        }
        stringBuffer.append(')');
        return stringBuffer.toString();
    }

    public byte[] getEncoded() {
        return getEncoded(this.withCompression);
    }

    public byte[] getEncoded(boolean z) {
        if (isInfinity()) {
            return new byte[1];
        }
        ECPoint eCPointNormalize = normalize();
        byte[] encoded = eCPointNormalize.getXCoord().getEncoded();
        if (z) {
            byte[] bArr = new byte[encoded.length + 1];
            bArr[0] = (byte) (eCPointNormalize.getCompressionYTilde() ? 3 : 2);
            System.arraycopy(encoded, 0, bArr, 1, encoded.length);
            return bArr;
        }
        byte[] encoded2 = eCPointNormalize.getYCoord().getEncoded();
        byte[] bArr2 = new byte[encoded.length + encoded2.length + 1];
        bArr2[0] = 4;
        System.arraycopy(encoded, 0, bArr2, 1, encoded.length);
        System.arraycopy(encoded2, 0, bArr2, encoded.length + 1, encoded2.length);
        return bArr2;
    }

    public ECPoint timesPow2(int i) {
        if (i < 0) {
            throw new IllegalArgumentException("'e' cannot be negative");
        }
        ECPoint eCPointTwice = this;
        while (true) {
            i--;
            if (i >= 0) {
                eCPointTwice = eCPointTwice.twice();
            } else {
                return eCPointTwice;
            }
        }
    }

    public ECPoint twicePlus(ECPoint eCPoint) {
        return twice().add(eCPoint);
    }

    public ECPoint threeTimes() {
        return twicePlus(this);
    }

    public ECPoint multiply(BigInteger bigInteger) {
        return getCurve().getMultiplier().multiply(this, bigInteger);
    }

    public static abstract class AbstractFp extends ECPoint {
        protected AbstractFp(ECCurve eCCurve, ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2) {
            super(eCCurve, eCFieldElement, eCFieldElement2);
        }

        protected AbstractFp(ECCurve eCCurve, ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, ECFieldElement[] eCFieldElementArr) {
            super(eCCurve, eCFieldElement, eCFieldElement2, eCFieldElementArr);
        }

        @Override
        protected boolean getCompressionYTilde() {
            return getAffineYCoord().testBitZero();
        }

        @Override
        protected boolean satisfiesCurveEquation() {
            ECFieldElement eCFieldElement = this.x;
            ECFieldElement eCFieldElement2 = this.y;
            ECFieldElement a = this.curve.getA();
            ECFieldElement b = this.curve.getB();
            ECFieldElement eCFieldElementSquare = eCFieldElement2.square();
            switch (getCurveCoordinateSystem()) {
                case 0:
                    break;
                case 1:
                    ECFieldElement eCFieldElement3 = this.zs[0];
                    if (!eCFieldElement3.isOne()) {
                        ECFieldElement eCFieldElementSquare2 = eCFieldElement3.square();
                        ECFieldElement eCFieldElementMultiply = eCFieldElement3.multiply(eCFieldElementSquare2);
                        eCFieldElementSquare = eCFieldElementSquare.multiply(eCFieldElement3);
                        a = a.multiply(eCFieldElementSquare2);
                        b = b.multiply(eCFieldElementMultiply);
                    }
                    break;
                case 2:
                case 3:
                case 4:
                    ECFieldElement eCFieldElement4 = this.zs[0];
                    if (!eCFieldElement4.isOne()) {
                        ECFieldElement eCFieldElementSquare3 = eCFieldElement4.square();
                        ECFieldElement eCFieldElementSquare4 = eCFieldElementSquare3.square();
                        ECFieldElement eCFieldElementMultiply2 = eCFieldElementSquare3.multiply(eCFieldElementSquare4);
                        a = a.multiply(eCFieldElementSquare4);
                        b = b.multiply(eCFieldElementMultiply2);
                    }
                    break;
                default:
                    throw new IllegalStateException("unsupported coordinate system");
            }
            return eCFieldElementSquare.equals(eCFieldElement.square().add(a).multiply(eCFieldElement).add(b));
        }

        @Override
        public ECPoint subtract(ECPoint eCPoint) {
            if (eCPoint.isInfinity()) {
                return this;
            }
            return add(eCPoint.negate());
        }
    }

    public static class Fp extends AbstractFp {
        public Fp(ECCurve eCCurve, ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2) {
            this(eCCurve, eCFieldElement, eCFieldElement2, false);
        }

        public Fp(ECCurve eCCurve, ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, boolean z) {
            super(eCCurve, eCFieldElement, eCFieldElement2);
            if ((eCFieldElement == null) != (eCFieldElement2 == null)) {
                throw new IllegalArgumentException("Exactly one of the field elements is null");
            }
            this.withCompression = z;
        }

        Fp(ECCurve eCCurve, ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, ECFieldElement[] eCFieldElementArr, boolean z) {
            super(eCCurve, eCFieldElement, eCFieldElement2, eCFieldElementArr);
            this.withCompression = z;
        }

        @Override
        protected ECPoint detach() {
            return new Fp(null, getAffineXCoord(), getAffineYCoord());
        }

        @Override
        public ECFieldElement getZCoord(int i) {
            if (i == 1 && 4 == getCurveCoordinateSystem()) {
                return getJacobianModifiedW();
            }
            return super.getZCoord(i);
        }

        @Override
        public ECPoint add(ECPoint eCPoint) {
            ECFieldElement eCFieldElementSubtract;
            ECFieldElement eCFieldElementMultiplyMinusProduct;
            ECFieldElement eCFieldElementMultiply;
            ECFieldElement eCFieldElementMultiply2;
            ECFieldElement[] eCFieldElementArr;
            if (isInfinity()) {
                return eCPoint;
            }
            if (eCPoint.isInfinity()) {
                return this;
            }
            if (this == eCPoint) {
                return twice();
            }
            ECCurve curve = getCurve();
            int coordinateSystem = curve.getCoordinateSystem();
            ECFieldElement eCFieldElementMultiply3 = this.x;
            ECFieldElement eCFieldElementMultiply4 = this.y;
            ECFieldElement eCFieldElementMultiply5 = eCPoint.x;
            ECFieldElement eCFieldElementMultiply6 = eCPoint.y;
            if (coordinateSystem != 4) {
                switch (coordinateSystem) {
                    case 0:
                        ECFieldElement eCFieldElementSubtract2 = eCFieldElementMultiply5.subtract(eCFieldElementMultiply3);
                        ECFieldElement eCFieldElementSubtract3 = eCFieldElementMultiply6.subtract(eCFieldElementMultiply4);
                        if (eCFieldElementSubtract2.isZero()) {
                            if (eCFieldElementSubtract3.isZero()) {
                                return twice();
                            }
                            return curve.getInfinity();
                        }
                        ECFieldElement eCFieldElementDivide = eCFieldElementSubtract3.divide(eCFieldElementSubtract2);
                        ECFieldElement eCFieldElementSubtract4 = eCFieldElementDivide.square().subtract(eCFieldElementMultiply3).subtract(eCFieldElementMultiply5);
                        return new Fp(curve, eCFieldElementSubtract4, eCFieldElementDivide.multiply(eCFieldElementMultiply3.subtract(eCFieldElementSubtract4)).subtract(eCFieldElementMultiply4), this.withCompression);
                    case 1:
                        ECFieldElement eCFieldElementMultiply7 = this.zs[0];
                        ECFieldElement eCFieldElement = eCPoint.zs[0];
                        boolean zIsOne = eCFieldElementMultiply7.isOne();
                        boolean zIsOne2 = eCFieldElement.isOne();
                        if (!zIsOne) {
                            eCFieldElementMultiply6 = eCFieldElementMultiply6.multiply(eCFieldElementMultiply7);
                        }
                        if (!zIsOne2) {
                            eCFieldElementMultiply4 = eCFieldElementMultiply4.multiply(eCFieldElement);
                        }
                        ECFieldElement eCFieldElementSubtract5 = eCFieldElementMultiply6.subtract(eCFieldElementMultiply4);
                        if (!zIsOne) {
                            eCFieldElementMultiply5 = eCFieldElementMultiply5.multiply(eCFieldElementMultiply7);
                        }
                        if (!zIsOne2) {
                            eCFieldElementMultiply3 = eCFieldElementMultiply3.multiply(eCFieldElement);
                        }
                        ECFieldElement eCFieldElementSubtract6 = eCFieldElementMultiply5.subtract(eCFieldElementMultiply3);
                        if (eCFieldElementSubtract6.isZero()) {
                            if (eCFieldElementSubtract5.isZero()) {
                                return twice();
                            }
                            return curve.getInfinity();
                        }
                        if (!zIsOne) {
                            if (!zIsOne2) {
                                eCFieldElementMultiply7 = eCFieldElementMultiply7.multiply(eCFieldElement);
                            }
                        } else {
                            eCFieldElementMultiply7 = eCFieldElement;
                        }
                        ECFieldElement eCFieldElementSquare = eCFieldElementSubtract6.square();
                        ECFieldElement eCFieldElementMultiply8 = eCFieldElementSquare.multiply(eCFieldElementSubtract6);
                        ECFieldElement eCFieldElementMultiply9 = eCFieldElementSquare.multiply(eCFieldElementMultiply3);
                        ECFieldElement eCFieldElementSubtract7 = eCFieldElementSubtract5.square().multiply(eCFieldElementMultiply7).subtract(eCFieldElementMultiply8).subtract(two(eCFieldElementMultiply9));
                        return new Fp(curve, eCFieldElementSubtract6.multiply(eCFieldElementSubtract7), eCFieldElementMultiply9.subtract(eCFieldElementSubtract7).multiplyMinusProduct(eCFieldElementSubtract5, eCFieldElementMultiply4, eCFieldElementMultiply8), new ECFieldElement[]{eCFieldElementMultiply8.multiply(eCFieldElementMultiply7)}, this.withCompression);
                    case 2:
                        break;
                    default:
                        throw new IllegalStateException("unsupported coordinate system");
                }
            }
            ECFieldElement eCFieldElement2 = this.zs[0];
            ECFieldElement eCFieldElement3 = eCPoint.zs[0];
            boolean zIsOne3 = eCFieldElement2.isOne();
            if (!zIsOne3 && eCFieldElement2.equals(eCFieldElement3)) {
                ECFieldElement eCFieldElementSubtract8 = eCFieldElementMultiply3.subtract(eCFieldElementMultiply5);
                ECFieldElement eCFieldElementSubtract9 = eCFieldElementMultiply4.subtract(eCFieldElementMultiply6);
                if (eCFieldElementSubtract8.isZero()) {
                    if (eCFieldElementSubtract9.isZero()) {
                        return twice();
                    }
                    return curve.getInfinity();
                }
                ECFieldElement eCFieldElementSquare2 = eCFieldElementSubtract8.square();
                ECFieldElement eCFieldElementMultiply10 = eCFieldElementMultiply3.multiply(eCFieldElementSquare2);
                ECFieldElement eCFieldElementMultiply11 = eCFieldElementMultiply5.multiply(eCFieldElementSquare2);
                ECFieldElement eCFieldElementMultiply12 = eCFieldElementMultiply10.subtract(eCFieldElementMultiply11).multiply(eCFieldElementMultiply4);
                ECFieldElement eCFieldElementSubtract10 = eCFieldElementSubtract9.square().subtract(eCFieldElementMultiply10).subtract(eCFieldElementMultiply11);
                eCFieldElementMultiplyMinusProduct = eCFieldElementMultiply10.subtract(eCFieldElementSubtract10).multiply(eCFieldElementSubtract9).subtract(eCFieldElementMultiply12);
                eCFieldElementMultiply2 = eCFieldElementSubtract8.multiply(eCFieldElement2);
                eCFieldElementSubtract = eCFieldElementSubtract10;
            } else {
                if (!zIsOne3) {
                    ECFieldElement eCFieldElementSquare3 = eCFieldElement2.square();
                    eCFieldElementMultiply5 = eCFieldElementSquare3.multiply(eCFieldElementMultiply5);
                    eCFieldElementMultiply6 = eCFieldElementSquare3.multiply(eCFieldElement2).multiply(eCFieldElementMultiply6);
                }
                boolean zIsOne4 = eCFieldElement3.isOne();
                if (!zIsOne4) {
                    ECFieldElement eCFieldElementSquare4 = eCFieldElement3.square();
                    eCFieldElementMultiply3 = eCFieldElementSquare4.multiply(eCFieldElementMultiply3);
                    eCFieldElementMultiply4 = eCFieldElementSquare4.multiply(eCFieldElement3).multiply(eCFieldElementMultiply4);
                }
                ECFieldElement eCFieldElementSubtract11 = eCFieldElementMultiply3.subtract(eCFieldElementMultiply5);
                ECFieldElement eCFieldElementSubtract12 = eCFieldElementMultiply4.subtract(eCFieldElementMultiply6);
                if (eCFieldElementSubtract11.isZero()) {
                    if (eCFieldElementSubtract12.isZero()) {
                        return twice();
                    }
                    return curve.getInfinity();
                }
                ECFieldElement eCFieldElementSquare5 = eCFieldElementSubtract11.square();
                ECFieldElement eCFieldElementMultiply13 = eCFieldElementSquare5.multiply(eCFieldElementSubtract11);
                ECFieldElement eCFieldElementMultiply14 = eCFieldElementSquare5.multiply(eCFieldElementMultiply3);
                eCFieldElementSubtract = eCFieldElementSubtract12.square().add(eCFieldElementMultiply13).subtract(two(eCFieldElementMultiply14));
                eCFieldElementMultiplyMinusProduct = eCFieldElementMultiply14.subtract(eCFieldElementSubtract).multiplyMinusProduct(eCFieldElementSubtract12, eCFieldElementMultiply13, eCFieldElementMultiply4);
                if (!zIsOne3) {
                    eCFieldElementMultiply = eCFieldElementSubtract11.multiply(eCFieldElement2);
                } else {
                    eCFieldElementMultiply = eCFieldElementSubtract11;
                }
                if (!zIsOne4) {
                    eCFieldElementMultiply2 = eCFieldElementMultiply.multiply(eCFieldElement3);
                } else {
                    eCFieldElementMultiply2 = eCFieldElementMultiply;
                }
                ECFieldElement eCFieldElement4 = eCFieldElementMultiply2 == eCFieldElementSubtract11 ? eCFieldElementSquare5 : null;
                if (coordinateSystem != 4) {
                    eCFieldElementArr = new ECFieldElement[]{eCFieldElementMultiply2, calculateJacobianModifiedW(eCFieldElementMultiply2, eCFieldElement4)};
                } else {
                    eCFieldElementArr = new ECFieldElement[]{eCFieldElementMultiply2};
                }
                return new Fp(curve, eCFieldElementSubtract, eCFieldElementMultiplyMinusProduct, eCFieldElementArr, this.withCompression);
            }
            if (coordinateSystem != 4) {
            }
            return new Fp(curve, eCFieldElementSubtract, eCFieldElementMultiplyMinusProduct, eCFieldElementArr, this.withCompression);
        }

        @Override
        public ECPoint twice() {
            ECFieldElement eCFieldElementMultiply;
            ECFieldElement eCFieldElementAdd;
            ECFieldElement eCFieldElementFour;
            ECFieldElement eCFieldElementSquare;
            if (isInfinity()) {
                return this;
            }
            ECCurve curve = getCurve();
            ECFieldElement eCFieldElement = this.y;
            if (eCFieldElement.isZero()) {
                return curve.getInfinity();
            }
            int coordinateSystem = curve.getCoordinateSystem();
            ECFieldElement eCFieldElement2 = this.x;
            if (coordinateSystem != 4) {
                switch (coordinateSystem) {
                    case 0:
                        ECFieldElement eCFieldElementDivide = three(eCFieldElement2.square()).add(getCurve().getA()).divide(two(eCFieldElement));
                        ECFieldElement eCFieldElementSubtract = eCFieldElementDivide.square().subtract(two(eCFieldElement2));
                        return new Fp(curve, eCFieldElementSubtract, eCFieldElementDivide.multiply(eCFieldElement2.subtract(eCFieldElementSubtract)).subtract(eCFieldElement), this.withCompression);
                    case 1:
                        ECFieldElement eCFieldElement3 = this.zs[0];
                        boolean zIsOne = eCFieldElement3.isOne();
                        ECFieldElement a = curve.getA();
                        if (!a.isZero() && !zIsOne) {
                            a = a.multiply(eCFieldElement3.square());
                        }
                        ECFieldElement eCFieldElementAdd2 = a.add(three(eCFieldElement2.square()));
                        if (!zIsOne) {
                            eCFieldElementMultiply = eCFieldElement.multiply(eCFieldElement3);
                        } else {
                            eCFieldElementMultiply = eCFieldElement;
                        }
                        ECFieldElement eCFieldElementSquare2 = zIsOne ? eCFieldElement.square() : eCFieldElementMultiply.multiply(eCFieldElement);
                        ECFieldElement eCFieldElementFour2 = four(eCFieldElement2.multiply(eCFieldElementSquare2));
                        ECFieldElement eCFieldElementSubtract2 = eCFieldElementAdd2.square().subtract(two(eCFieldElementFour2));
                        ECFieldElement eCFieldElementTwo = two(eCFieldElementMultiply);
                        ECFieldElement eCFieldElementMultiply2 = eCFieldElementSubtract2.multiply(eCFieldElementTwo);
                        ECFieldElement eCFieldElementTwo2 = two(eCFieldElementSquare2);
                        return new Fp(curve, eCFieldElementMultiply2, eCFieldElementFour2.subtract(eCFieldElementSubtract2).multiply(eCFieldElementAdd2).subtract(two(eCFieldElementTwo2.square())), new ECFieldElement[]{two(zIsOne ? two(eCFieldElementTwo2) : eCFieldElementTwo.square()).multiply(eCFieldElementMultiply)}, this.withCompression);
                    case 2:
                        ECFieldElement eCFieldElement4 = this.zs[0];
                        boolean zIsOne2 = eCFieldElement4.isOne();
                        ECFieldElement eCFieldElementSquare3 = eCFieldElement.square();
                        ECFieldElement eCFieldElementSquare4 = eCFieldElementSquare3.square();
                        ECFieldElement a2 = curve.getA();
                        ECFieldElement eCFieldElementNegate = a2.negate();
                        if (eCFieldElementNegate.toBigInteger().equals(BigInteger.valueOf(3L))) {
                            if (!zIsOne2) {
                                eCFieldElementSquare = eCFieldElement4.square();
                            } else {
                                eCFieldElementSquare = eCFieldElement4;
                            }
                            eCFieldElementAdd = three(eCFieldElement2.add(eCFieldElementSquare).multiply(eCFieldElement2.subtract(eCFieldElementSquare)));
                            eCFieldElementFour = four(eCFieldElementSquare3.multiply(eCFieldElement2));
                        } else {
                            ECFieldElement eCFieldElementThree = three(eCFieldElement2.square());
                            if (zIsOne2) {
                                eCFieldElementAdd = eCFieldElementThree.add(a2);
                            } else if (!a2.isZero()) {
                                ECFieldElement eCFieldElementSquare5 = eCFieldElement4.square().square();
                                if (eCFieldElementNegate.bitLength() < a2.bitLength()) {
                                    eCFieldElementAdd = eCFieldElementThree.subtract(eCFieldElementSquare5.multiply(eCFieldElementNegate));
                                } else {
                                    eCFieldElementAdd = eCFieldElementThree.add(eCFieldElementSquare5.multiply(a2));
                                }
                            } else {
                                eCFieldElementAdd = eCFieldElementThree;
                            }
                            eCFieldElementFour = four(eCFieldElement2.multiply(eCFieldElementSquare3));
                        }
                        ECFieldElement eCFieldElementSubtract3 = eCFieldElementAdd.square().subtract(two(eCFieldElementFour));
                        ECFieldElement eCFieldElementSubtract4 = eCFieldElementFour.subtract(eCFieldElementSubtract3).multiply(eCFieldElementAdd).subtract(eight(eCFieldElementSquare4));
                        ECFieldElement eCFieldElementTwo3 = two(eCFieldElement);
                        if (!zIsOne2) {
                            eCFieldElementTwo3 = eCFieldElementTwo3.multiply(eCFieldElement4);
                        }
                        return new Fp(curve, eCFieldElementSubtract3, eCFieldElementSubtract4, new ECFieldElement[]{eCFieldElementTwo3}, this.withCompression);
                    default:
                        throw new IllegalStateException("unsupported coordinate system");
                }
            }
            return twiceJacobianModified(true);
        }

        @Override
        public ECPoint twicePlus(ECPoint eCPoint) {
            if (this == eCPoint) {
                return threeTimes();
            }
            if (isInfinity()) {
                return eCPoint;
            }
            if (eCPoint.isInfinity()) {
                return twice();
            }
            ECFieldElement eCFieldElement = this.y;
            if (eCFieldElement.isZero()) {
                return eCPoint;
            }
            ECCurve curve = getCurve();
            int coordinateSystem = curve.getCoordinateSystem();
            if (coordinateSystem != 0) {
                if (coordinateSystem == 4) {
                    return twiceJacobianModified(false).add(eCPoint);
                }
                return twice().add(eCPoint);
            }
            ECFieldElement eCFieldElement2 = this.x;
            ECFieldElement eCFieldElement3 = eCPoint.x;
            ECFieldElement eCFieldElement4 = eCPoint.y;
            ECFieldElement eCFieldElementSubtract = eCFieldElement3.subtract(eCFieldElement2);
            ECFieldElement eCFieldElementSubtract2 = eCFieldElement4.subtract(eCFieldElement);
            if (eCFieldElementSubtract.isZero()) {
                if (eCFieldElementSubtract2.isZero()) {
                    return threeTimes();
                }
                return this;
            }
            ECFieldElement eCFieldElementSquare = eCFieldElementSubtract.square();
            ECFieldElement eCFieldElementSubtract3 = eCFieldElementSquare.multiply(two(eCFieldElement2).add(eCFieldElement3)).subtract(eCFieldElementSubtract2.square());
            if (eCFieldElementSubtract3.isZero()) {
                return curve.getInfinity();
            }
            ECFieldElement eCFieldElementInvert = eCFieldElementSubtract3.multiply(eCFieldElementSubtract).invert();
            ECFieldElement eCFieldElementMultiply = eCFieldElementSubtract3.multiply(eCFieldElementInvert).multiply(eCFieldElementSubtract2);
            ECFieldElement eCFieldElementSubtract4 = two(eCFieldElement).multiply(eCFieldElementSquare).multiply(eCFieldElementSubtract).multiply(eCFieldElementInvert).subtract(eCFieldElementMultiply);
            ECFieldElement eCFieldElementAdd = eCFieldElementSubtract4.subtract(eCFieldElementMultiply).multiply(eCFieldElementMultiply.add(eCFieldElementSubtract4)).add(eCFieldElement3);
            return new Fp(curve, eCFieldElementAdd, eCFieldElement2.subtract(eCFieldElementAdd).multiply(eCFieldElementSubtract4).subtract(eCFieldElement), this.withCompression);
        }

        @Override
        public ECPoint threeTimes() {
            if (isInfinity()) {
                return this;
            }
            ECFieldElement eCFieldElement = this.y;
            if (eCFieldElement.isZero()) {
                return this;
            }
            ECCurve curve = getCurve();
            int coordinateSystem = curve.getCoordinateSystem();
            if (coordinateSystem != 0) {
                if (coordinateSystem == 4) {
                    return twiceJacobianModified(false).add(this);
                }
                return twice().add(this);
            }
            ECFieldElement eCFieldElement2 = this.x;
            ECFieldElement eCFieldElementTwo = two(eCFieldElement);
            ECFieldElement eCFieldElementSquare = eCFieldElementTwo.square();
            ECFieldElement eCFieldElementAdd = three(eCFieldElement2.square()).add(getCurve().getA());
            ECFieldElement eCFieldElementSubtract = three(eCFieldElement2).multiply(eCFieldElementSquare).subtract(eCFieldElementAdd.square());
            if (eCFieldElementSubtract.isZero()) {
                return getCurve().getInfinity();
            }
            ECFieldElement eCFieldElementInvert = eCFieldElementSubtract.multiply(eCFieldElementTwo).invert();
            ECFieldElement eCFieldElementMultiply = eCFieldElementSubtract.multiply(eCFieldElementInvert).multiply(eCFieldElementAdd);
            ECFieldElement eCFieldElementSubtract2 = eCFieldElementSquare.square().multiply(eCFieldElementInvert).subtract(eCFieldElementMultiply);
            ECFieldElement eCFieldElementAdd2 = eCFieldElementSubtract2.subtract(eCFieldElementMultiply).multiply(eCFieldElementMultiply.add(eCFieldElementSubtract2)).add(eCFieldElement2);
            return new Fp(curve, eCFieldElementAdd2, eCFieldElement2.subtract(eCFieldElementAdd2).multiply(eCFieldElementSubtract2).subtract(eCFieldElement), this.withCompression);
        }

        @Override
        public ECPoint timesPow2(int i) {
            if (i < 0) {
                throw new IllegalArgumentException("'e' cannot be negative");
            }
            if (i == 0 || isInfinity()) {
                return this;
            }
            if (i == 1) {
                return twice();
            }
            ECCurve curve = getCurve();
            ECFieldElement eCFieldElementMultiply = this.y;
            if (eCFieldElementMultiply.isZero()) {
                return curve.getInfinity();
            }
            int coordinateSystem = curve.getCoordinateSystem();
            ECFieldElement a = curve.getA();
            ECFieldElement eCFieldElementMultiply2 = this.x;
            ECFieldElement eCFieldElementFromBigInteger = this.zs.length < 1 ? curve.fromBigInteger(ECConstants.ONE) : this.zs[0];
            if (!eCFieldElementFromBigInteger.isOne()) {
                if (coordinateSystem != 4) {
                    switch (coordinateSystem) {
                        case 0:
                            break;
                        case 1:
                            ECFieldElement eCFieldElementSquare = eCFieldElementFromBigInteger.square();
                            eCFieldElementMultiply2 = eCFieldElementMultiply2.multiply(eCFieldElementFromBigInteger);
                            eCFieldElementMultiply = eCFieldElementMultiply.multiply(eCFieldElementSquare);
                            a = calculateJacobianModifiedW(eCFieldElementFromBigInteger, eCFieldElementSquare);
                            break;
                        case 2:
                            a = calculateJacobianModifiedW(eCFieldElementFromBigInteger, null);
                            break;
                        default:
                            throw new IllegalStateException("unsupported coordinate system");
                    }
                } else {
                    a = getJacobianModifiedW();
                }
            }
            ECFieldElement eCFieldElementMultiply3 = eCFieldElementFromBigInteger;
            ECFieldElement eCFieldElementTwo = a;
            ECFieldElement eCFieldElementSubtract = eCFieldElementMultiply;
            int i2 = 0;
            while (i2 < i) {
                if (eCFieldElementSubtract.isZero()) {
                    return curve.getInfinity();
                }
                ECFieldElement eCFieldElementThree = three(eCFieldElementMultiply2.square());
                ECFieldElement eCFieldElementTwo2 = two(eCFieldElementSubtract);
                ECFieldElement eCFieldElementMultiply4 = eCFieldElementTwo2.multiply(eCFieldElementSubtract);
                ECFieldElement eCFieldElementTwo3 = two(eCFieldElementMultiply2.multiply(eCFieldElementMultiply4));
                ECFieldElement eCFieldElementTwo4 = two(eCFieldElementMultiply4.square());
                if (!eCFieldElementTwo.isZero()) {
                    eCFieldElementThree = eCFieldElementThree.add(eCFieldElementTwo);
                    eCFieldElementTwo = two(eCFieldElementTwo4.multiply(eCFieldElementTwo));
                }
                ECFieldElement eCFieldElementSubtract2 = eCFieldElementThree.square().subtract(two(eCFieldElementTwo3));
                eCFieldElementSubtract = eCFieldElementThree.multiply(eCFieldElementTwo3.subtract(eCFieldElementSubtract2)).subtract(eCFieldElementTwo4);
                if (eCFieldElementMultiply3.isOne()) {
                    eCFieldElementMultiply3 = eCFieldElementTwo2;
                } else {
                    eCFieldElementMultiply3 = eCFieldElementTwo2.multiply(eCFieldElementMultiply3);
                }
                i2++;
                eCFieldElementMultiply2 = eCFieldElementSubtract2;
            }
            if (coordinateSystem != 4) {
                switch (coordinateSystem) {
                    case 0:
                        ECFieldElement eCFieldElementInvert = eCFieldElementMultiply3.invert();
                        ECFieldElement eCFieldElementSquare2 = eCFieldElementInvert.square();
                        return new Fp(curve, eCFieldElementMultiply2.multiply(eCFieldElementSquare2), eCFieldElementSubtract.multiply(eCFieldElementSquare2.multiply(eCFieldElementInvert)), this.withCompression);
                    case 1:
                        return new Fp(curve, eCFieldElementMultiply2.multiply(eCFieldElementMultiply3), eCFieldElementSubtract, new ECFieldElement[]{eCFieldElementMultiply3.multiply(eCFieldElementMultiply3.square())}, this.withCompression);
                    case 2:
                        return new Fp(curve, eCFieldElementMultiply2, eCFieldElementSubtract, new ECFieldElement[]{eCFieldElementMultiply3}, this.withCompression);
                    default:
                        throw new IllegalStateException("unsupported coordinate system");
                }
            }
            return new Fp(curve, eCFieldElementMultiply2, eCFieldElementSubtract, new ECFieldElement[]{eCFieldElementMultiply3, eCFieldElementTwo}, this.withCompression);
        }

        protected ECFieldElement two(ECFieldElement eCFieldElement) {
            return eCFieldElement.add(eCFieldElement);
        }

        protected ECFieldElement three(ECFieldElement eCFieldElement) {
            return two(eCFieldElement).add(eCFieldElement);
        }

        protected ECFieldElement four(ECFieldElement eCFieldElement) {
            return two(two(eCFieldElement));
        }

        protected ECFieldElement eight(ECFieldElement eCFieldElement) {
            return four(two(eCFieldElement));
        }

        protected ECFieldElement doubleProductFromSquares(ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, ECFieldElement eCFieldElement3, ECFieldElement eCFieldElement4) {
            return eCFieldElement.add(eCFieldElement2).square().subtract(eCFieldElement3).subtract(eCFieldElement4);
        }

        @Override
        public ECPoint negate() {
            if (isInfinity()) {
                return this;
            }
            ECCurve curve = getCurve();
            if (curve.getCoordinateSystem() != 0) {
                return new Fp(curve, this.x, this.y.negate(), this.zs, this.withCompression);
            }
            return new Fp(curve, this.x, this.y.negate(), this.withCompression);
        }

        protected ECFieldElement calculateJacobianModifiedW(ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2) {
            ECFieldElement a = getCurve().getA();
            if (a.isZero() || eCFieldElement.isOne()) {
                return a;
            }
            if (eCFieldElement2 == null) {
                eCFieldElement2 = eCFieldElement.square();
            }
            ECFieldElement eCFieldElementSquare = eCFieldElement2.square();
            ECFieldElement eCFieldElementNegate = a.negate();
            if (eCFieldElementNegate.bitLength() < a.bitLength()) {
                return eCFieldElementSquare.multiply(eCFieldElementNegate).negate();
            }
            return eCFieldElementSquare.multiply(a);
        }

        protected ECFieldElement getJacobianModifiedW() {
            ECFieldElement eCFieldElement = this.zs[1];
            if (eCFieldElement != null) {
                return eCFieldElement;
            }
            ECFieldElement[] eCFieldElementArr = this.zs;
            ECFieldElement eCFieldElementCalculateJacobianModifiedW = calculateJacobianModifiedW(this.zs[0], null);
            eCFieldElementArr[1] = eCFieldElementCalculateJacobianModifiedW;
            return eCFieldElementCalculateJacobianModifiedW;
        }

        protected Fp twiceJacobianModified(boolean z) {
            ECFieldElement eCFieldElement = this.x;
            ECFieldElement eCFieldElement2 = this.y;
            ECFieldElement eCFieldElement3 = this.zs[0];
            ECFieldElement jacobianModifiedW = getJacobianModifiedW();
            ECFieldElement eCFieldElementAdd = three(eCFieldElement.square()).add(jacobianModifiedW);
            ECFieldElement eCFieldElementTwo = two(eCFieldElement2);
            ECFieldElement eCFieldElementMultiply = eCFieldElementTwo.multiply(eCFieldElement2);
            ECFieldElement eCFieldElementTwo2 = two(eCFieldElement.multiply(eCFieldElementMultiply));
            ECFieldElement eCFieldElementSubtract = eCFieldElementAdd.square().subtract(two(eCFieldElementTwo2));
            ECFieldElement eCFieldElementTwo3 = two(eCFieldElementMultiply.square());
            ECFieldElement eCFieldElementSubtract2 = eCFieldElementAdd.multiply(eCFieldElementTwo2.subtract(eCFieldElementSubtract)).subtract(eCFieldElementTwo3);
            ECFieldElement eCFieldElementTwo4 = z ? two(eCFieldElementTwo3.multiply(jacobianModifiedW)) : null;
            if (!eCFieldElement3.isOne()) {
                eCFieldElementTwo = eCFieldElementTwo.multiply(eCFieldElement3);
            }
            return new Fp(getCurve(), eCFieldElementSubtract, eCFieldElementSubtract2, new ECFieldElement[]{eCFieldElementTwo, eCFieldElementTwo4}, this.withCompression);
        }
    }

    public static abstract class AbstractF2m extends ECPoint {
        protected AbstractF2m(ECCurve eCCurve, ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2) {
            super(eCCurve, eCFieldElement, eCFieldElement2);
        }

        protected AbstractF2m(ECCurve eCCurve, ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, ECFieldElement[] eCFieldElementArr) {
            super(eCCurve, eCFieldElement, eCFieldElement2, eCFieldElementArr);
        }

        @Override
        protected boolean satisfiesCurveEquation() {
            ECFieldElement eCFieldElementMultiplyPlusProduct;
            ECFieldElement eCFieldElementSquarePlusProduct;
            ECCurve curve = getCurve();
            ECFieldElement eCFieldElement = this.x;
            ECFieldElement a = curve.getA();
            ECFieldElement b = curve.getB();
            int coordinateSystem = curve.getCoordinateSystem();
            if (coordinateSystem == 6) {
                ECFieldElement eCFieldElement2 = this.zs[0];
                boolean zIsOne = eCFieldElement2.isOne();
                if (eCFieldElement.isZero()) {
                    ECFieldElement eCFieldElementSquare = this.y.square();
                    if (!zIsOne) {
                        b = b.multiply(eCFieldElement2.square());
                    }
                    return eCFieldElementSquare.equals(b);
                }
                ECFieldElement eCFieldElement3 = this.y;
                ECFieldElement eCFieldElementSquare2 = eCFieldElement.square();
                if (zIsOne) {
                    eCFieldElementMultiplyPlusProduct = eCFieldElement3.square().add(eCFieldElement3).add(a);
                    eCFieldElementSquarePlusProduct = eCFieldElementSquare2.square().add(b);
                } else {
                    ECFieldElement eCFieldElementSquare3 = eCFieldElement2.square();
                    ECFieldElement eCFieldElementSquare4 = eCFieldElementSquare3.square();
                    eCFieldElementMultiplyPlusProduct = eCFieldElement3.add(eCFieldElement2).multiplyPlusProduct(eCFieldElement3, a, eCFieldElementSquare3);
                    eCFieldElementSquarePlusProduct = eCFieldElementSquare2.squarePlusProduct(b, eCFieldElementSquare4);
                }
                return eCFieldElementMultiplyPlusProduct.multiply(eCFieldElementSquare2).equals(eCFieldElementSquarePlusProduct);
            }
            ECFieldElement eCFieldElement4 = this.y;
            ECFieldElement eCFieldElementMultiply = eCFieldElement4.add(eCFieldElement).multiply(eCFieldElement4);
            switch (coordinateSystem) {
                case 0:
                    break;
                case 1:
                    ECFieldElement eCFieldElement5 = this.zs[0];
                    if (!eCFieldElement5.isOne()) {
                        ECFieldElement eCFieldElementMultiply2 = eCFieldElement5.multiply(eCFieldElement5.square());
                        eCFieldElementMultiply = eCFieldElementMultiply.multiply(eCFieldElement5);
                        a = a.multiply(eCFieldElement5);
                        b = b.multiply(eCFieldElementMultiply2);
                    }
                    break;
                default:
                    throw new IllegalStateException("unsupported coordinate system");
            }
            return eCFieldElementMultiply.equals(eCFieldElement.add(a).multiply(eCFieldElement.square()).add(b));
        }

        @Override
        public ECPoint scaleX(ECFieldElement eCFieldElement) {
            if (isInfinity()) {
                return this;
            }
            switch (getCurveCoordinateSystem()) {
                case 5:
                    ECFieldElement rawXCoord = getRawXCoord();
                    return getCurve().createRawPoint(rawXCoord, getRawYCoord().add(rawXCoord).divide(eCFieldElement).add(rawXCoord.multiply(eCFieldElement)), getRawZCoords(), this.withCompression);
                case 6:
                    ECFieldElement rawXCoord2 = getRawXCoord();
                    ECFieldElement rawYCoord = getRawYCoord();
                    ECFieldElement eCFieldElement2 = getRawZCoords()[0];
                    ECFieldElement eCFieldElementMultiply = rawXCoord2.multiply(eCFieldElement.square());
                    return getCurve().createRawPoint(eCFieldElementMultiply, rawYCoord.add(rawXCoord2).add(eCFieldElementMultiply), new ECFieldElement[]{eCFieldElement2.multiply(eCFieldElement)}, this.withCompression);
                default:
                    return super.scaleX(eCFieldElement);
            }
        }

        @Override
        public ECPoint scaleY(ECFieldElement eCFieldElement) {
            if (isInfinity()) {
                return this;
            }
            switch (getCurveCoordinateSystem()) {
                case 5:
                case 6:
                    ECFieldElement rawXCoord = getRawXCoord();
                    return getCurve().createRawPoint(rawXCoord, getRawYCoord().add(rawXCoord).multiply(eCFieldElement).add(rawXCoord), getRawZCoords(), this.withCompression);
                default:
                    return super.scaleY(eCFieldElement);
            }
        }

        @Override
        public ECPoint subtract(ECPoint eCPoint) {
            if (eCPoint.isInfinity()) {
                return this;
            }
            return add(eCPoint.negate());
        }

        public AbstractF2m tau() {
            if (isInfinity()) {
                return this;
            }
            ECCurve curve = getCurve();
            int coordinateSystem = curve.getCoordinateSystem();
            ECFieldElement eCFieldElement = this.x;
            switch (coordinateSystem) {
                case 0:
                case 5:
                    return (AbstractF2m) curve.createRawPoint(eCFieldElement.square(), this.y.square(), this.withCompression);
                case 1:
                case 6:
                    return (AbstractF2m) curve.createRawPoint(eCFieldElement.square(), this.y.square(), new ECFieldElement[]{this.zs[0].square()}, this.withCompression);
                case 2:
                case 3:
                case 4:
                default:
                    throw new IllegalStateException("unsupported coordinate system");
            }
        }

        public AbstractF2m tauPow(int i) {
            if (isInfinity()) {
                return this;
            }
            ECCurve curve = getCurve();
            int coordinateSystem = curve.getCoordinateSystem();
            ECFieldElement eCFieldElement = this.x;
            switch (coordinateSystem) {
                case 0:
                case 5:
                    return (AbstractF2m) curve.createRawPoint(eCFieldElement.squarePow(i), this.y.squarePow(i), this.withCompression);
                case 1:
                case 6:
                    return (AbstractF2m) curve.createRawPoint(eCFieldElement.squarePow(i), this.y.squarePow(i), new ECFieldElement[]{this.zs[0].squarePow(i)}, this.withCompression);
                case 2:
                case 3:
                case 4:
                default:
                    throw new IllegalStateException("unsupported coordinate system");
            }
        }
    }

    public static class F2m extends AbstractF2m {
        public F2m(ECCurve eCCurve, ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2) {
            this(eCCurve, eCFieldElement, eCFieldElement2, false);
        }

        public F2m(ECCurve eCCurve, ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, boolean z) {
            super(eCCurve, eCFieldElement, eCFieldElement2);
            if ((eCFieldElement == null) != (eCFieldElement2 == null)) {
                throw new IllegalArgumentException("Exactly one of the field elements is null");
            }
            if (eCFieldElement != null) {
                ECFieldElement.F2m.checkFieldElements(this.x, this.y);
                if (eCCurve != null) {
                    ECFieldElement.F2m.checkFieldElements(this.x, this.curve.getA());
                }
            }
            this.withCompression = z;
        }

        F2m(ECCurve eCCurve, ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, ECFieldElement[] eCFieldElementArr, boolean z) {
            super(eCCurve, eCFieldElement, eCFieldElement2, eCFieldElementArr);
            this.withCompression = z;
        }

        @Override
        protected ECPoint detach() {
            return new F2m(null, getAffineXCoord(), getAffineYCoord());
        }

        @Override
        public ECFieldElement getYCoord() {
            int curveCoordinateSystem = getCurveCoordinateSystem();
            switch (curveCoordinateSystem) {
                case 5:
                case 6:
                    ECFieldElement eCFieldElement = this.x;
                    ECFieldElement eCFieldElement2 = this.y;
                    if (isInfinity() || eCFieldElement.isZero()) {
                        return eCFieldElement2;
                    }
                    ECFieldElement eCFieldElementMultiply = eCFieldElement2.add(eCFieldElement).multiply(eCFieldElement);
                    if (6 == curveCoordinateSystem) {
                        ECFieldElement eCFieldElement3 = this.zs[0];
                        if (!eCFieldElement3.isOne()) {
                            return eCFieldElementMultiply.divide(eCFieldElement3);
                        }
                        return eCFieldElementMultiply;
                    }
                    return eCFieldElementMultiply;
                default:
                    return this.y;
            }
        }

        @Override
        protected boolean getCompressionYTilde() {
            ECFieldElement rawXCoord = getRawXCoord();
            if (rawXCoord.isZero()) {
                return false;
            }
            ECFieldElement rawYCoord = getRawYCoord();
            switch (getCurveCoordinateSystem()) {
                case 5:
                case 6:
                    return rawYCoord.testBitZero() != rawXCoord.testBitZero();
                default:
                    return rawYCoord.divide(rawXCoord).testBitZero();
            }
        }

        @Override
        public ECPoint add(ECPoint eCPoint) {
            ECFieldElement eCFieldElementMultiply;
            ECFieldElement eCFieldElementMultiply2;
            ECFieldElement eCFieldElementMultiply3;
            ECFieldElement eCFieldElementMultiply4;
            ECFieldElement eCFieldElementAdd;
            ECFieldElement eCFieldElementAdd2;
            ECFieldElement eCFieldElementMultiply5;
            ECFieldElement eCFieldElementMultiply6;
            if (isInfinity()) {
                return eCPoint;
            }
            if (eCPoint.isInfinity()) {
                return this;
            }
            ECCurve curve = getCurve();
            int coordinateSystem = curve.getCoordinateSystem();
            ECFieldElement eCFieldElementMultiply7 = this.x;
            ECFieldElement eCFieldElement = eCPoint.x;
            if (coordinateSystem != 6) {
                switch (coordinateSystem) {
                    case 0:
                        ECFieldElement eCFieldElement2 = this.y;
                        ECFieldElement eCFieldElement3 = eCPoint.y;
                        ECFieldElement eCFieldElementAdd3 = eCFieldElementMultiply7.add(eCFieldElement);
                        ECFieldElement eCFieldElementAdd4 = eCFieldElement2.add(eCFieldElement3);
                        if (eCFieldElementAdd3.isZero()) {
                            if (eCFieldElementAdd4.isZero()) {
                                return twice();
                            }
                            return curve.getInfinity();
                        }
                        ECFieldElement eCFieldElementDivide = eCFieldElementAdd4.divide(eCFieldElementAdd3);
                        ECFieldElement eCFieldElementAdd5 = eCFieldElementDivide.square().add(eCFieldElementDivide).add(eCFieldElementAdd3).add(curve.getA());
                        return new F2m(curve, eCFieldElementAdd5, eCFieldElementDivide.multiply(eCFieldElementMultiply7.add(eCFieldElementAdd5)).add(eCFieldElementAdd5).add(eCFieldElement2), this.withCompression);
                    case 1:
                        ECFieldElement eCFieldElement4 = this.y;
                        ECFieldElement eCFieldElementMultiply8 = this.zs[0];
                        ECFieldElement eCFieldElement5 = eCPoint.y;
                        ECFieldElement eCFieldElement6 = eCPoint.zs[0];
                        boolean zIsOne = eCFieldElement6.isOne();
                        ECFieldElement eCFieldElementMultiply9 = eCFieldElementMultiply8.multiply(eCFieldElement5);
                        if (!zIsOne) {
                            eCFieldElementMultiply5 = eCFieldElement4.multiply(eCFieldElement6);
                        } else {
                            eCFieldElementMultiply5 = eCFieldElement4;
                        }
                        ECFieldElement eCFieldElementAdd6 = eCFieldElementMultiply9.add(eCFieldElementMultiply5);
                        ECFieldElement eCFieldElementMultiply10 = eCFieldElementMultiply8.multiply(eCFieldElement);
                        if (!zIsOne) {
                            eCFieldElementMultiply6 = eCFieldElementMultiply7.multiply(eCFieldElement6);
                        } else {
                            eCFieldElementMultiply6 = eCFieldElementMultiply7;
                        }
                        ECFieldElement eCFieldElementAdd7 = eCFieldElementMultiply10.add(eCFieldElementMultiply6);
                        if (eCFieldElementAdd7.isZero()) {
                            if (eCFieldElementAdd6.isZero()) {
                                return twice();
                            }
                            return curve.getInfinity();
                        }
                        ECFieldElement eCFieldElementSquare = eCFieldElementAdd7.square();
                        ECFieldElement eCFieldElementMultiply11 = eCFieldElementSquare.multiply(eCFieldElementAdd7);
                        if (!zIsOne) {
                            eCFieldElementMultiply8 = eCFieldElementMultiply8.multiply(eCFieldElement6);
                        }
                        ECFieldElement eCFieldElementAdd8 = eCFieldElementAdd6.add(eCFieldElementAdd7);
                        ECFieldElement eCFieldElementAdd9 = eCFieldElementAdd8.multiplyPlusProduct(eCFieldElementAdd6, eCFieldElementSquare, curve.getA()).multiply(eCFieldElementMultiply8).add(eCFieldElementMultiply11);
                        ECFieldElement eCFieldElementMultiply12 = eCFieldElementAdd7.multiply(eCFieldElementAdd9);
                        if (!zIsOne) {
                            eCFieldElementSquare = eCFieldElementSquare.multiply(eCFieldElement6);
                        }
                        return new F2m(curve, eCFieldElementMultiply12, eCFieldElementAdd6.multiplyPlusProduct(eCFieldElementMultiply7, eCFieldElementAdd7, eCFieldElement4).multiplyPlusProduct(eCFieldElementSquare, eCFieldElementAdd8, eCFieldElementAdd9), new ECFieldElement[]{eCFieldElementMultiply11.multiply(eCFieldElementMultiply8)}, this.withCompression);
                    default:
                        throw new IllegalStateException("unsupported coordinate system");
                }
            }
            if (eCFieldElementMultiply7.isZero()) {
                if (eCFieldElement.isZero()) {
                    return curve.getInfinity();
                }
                return eCPoint.add(this);
            }
            ECFieldElement eCFieldElement7 = this.y;
            ECFieldElement eCFieldElement8 = this.zs[0];
            ECFieldElement eCFieldElement9 = eCPoint.y;
            ECFieldElement eCFieldElement10 = eCPoint.zs[0];
            boolean zIsOne2 = eCFieldElement8.isOne();
            if (!zIsOne2) {
                eCFieldElementMultiply = eCFieldElement.multiply(eCFieldElement8);
                eCFieldElementMultiply2 = eCFieldElement9.multiply(eCFieldElement8);
            } else {
                eCFieldElementMultiply = eCFieldElement;
                eCFieldElementMultiply2 = eCFieldElement9;
            }
            boolean zIsOne3 = eCFieldElement10.isOne();
            if (!zIsOne3) {
                eCFieldElementMultiply7 = eCFieldElementMultiply7.multiply(eCFieldElement10);
                eCFieldElementMultiply3 = eCFieldElement7.multiply(eCFieldElement10);
            } else {
                eCFieldElementMultiply3 = eCFieldElement7;
            }
            ECFieldElement eCFieldElementAdd10 = eCFieldElementMultiply3.add(eCFieldElementMultiply2);
            ECFieldElement eCFieldElementAdd11 = eCFieldElementMultiply7.add(eCFieldElementMultiply);
            if (eCFieldElementAdd11.isZero()) {
                if (eCFieldElementAdd10.isZero()) {
                    return twice();
                }
                return curve.getInfinity();
            }
            if (eCFieldElement.isZero()) {
                ECPoint eCPointNormalize = normalize();
                ECFieldElement xCoord = eCPointNormalize.getXCoord();
                ECFieldElement yCoord = eCPointNormalize.getYCoord();
                ECFieldElement eCFieldElementDivide2 = yCoord.add(eCFieldElement9).divide(xCoord);
                eCFieldElementAdd2 = eCFieldElementDivide2.square().add(eCFieldElementDivide2).add(xCoord).add(curve.getA());
                if (eCFieldElementAdd2.isZero()) {
                    return new F2m(curve, eCFieldElementAdd2, curve.getB().sqrt(), this.withCompression);
                }
                eCFieldElementAdd = eCFieldElementDivide2.multiply(xCoord.add(eCFieldElementAdd2)).add(eCFieldElementAdd2).add(yCoord).divide(eCFieldElementAdd2).add(eCFieldElementAdd2);
                eCFieldElementMultiply4 = curve.fromBigInteger(ECConstants.ONE);
            } else {
                ECFieldElement eCFieldElementSquare2 = eCFieldElementAdd11.square();
                ECFieldElement eCFieldElementMultiply13 = eCFieldElementAdd10.multiply(eCFieldElementMultiply7);
                ECFieldElement eCFieldElementMultiply14 = eCFieldElementAdd10.multiply(eCFieldElementMultiply);
                ECFieldElement eCFieldElementMultiply15 = eCFieldElementMultiply13.multiply(eCFieldElementMultiply14);
                if (eCFieldElementMultiply15.isZero()) {
                    return new F2m(curve, eCFieldElementMultiply15, curve.getB().sqrt(), this.withCompression);
                }
                ECFieldElement eCFieldElementMultiply16 = eCFieldElementAdd10.multiply(eCFieldElementSquare2);
                if (!zIsOne3) {
                    eCFieldElementMultiply4 = eCFieldElementMultiply16.multiply(eCFieldElement10);
                } else {
                    eCFieldElementMultiply4 = eCFieldElementMultiply16;
                }
                ECFieldElement eCFieldElementSquarePlusProduct = eCFieldElementMultiply14.add(eCFieldElementSquare2).squarePlusProduct(eCFieldElementMultiply4, eCFieldElement7.add(eCFieldElement8));
                if (!zIsOne2) {
                    eCFieldElementMultiply4 = eCFieldElementMultiply4.multiply(eCFieldElement8);
                }
                eCFieldElementAdd = eCFieldElementSquarePlusProduct;
                eCFieldElementAdd2 = eCFieldElementMultiply15;
            }
            return new F2m(curve, eCFieldElementAdd2, eCFieldElementAdd, new ECFieldElement[]{eCFieldElementMultiply4}, this.withCompression);
        }

        @Override
        public ECPoint twice() {
            ECFieldElement eCFieldElementMultiply;
            ECFieldElement eCFieldElementSquare;
            ECFieldElement eCFieldElementMultiply2;
            ECFieldElement eCFieldElementMultiply3;
            ECFieldElement eCFieldElementAdd;
            ECFieldElement eCFieldElementSquarePlusProduct;
            ECFieldElement eCFieldElementMultiply4;
            if (isInfinity()) {
                return this;
            }
            ECCurve curve = getCurve();
            ECFieldElement eCFieldElementMultiply5 = this.x;
            if (eCFieldElementMultiply5.isZero()) {
                return curve.getInfinity();
            }
            int coordinateSystem = curve.getCoordinateSystem();
            if (coordinateSystem != 6) {
                switch (coordinateSystem) {
                    case 0:
                        ECFieldElement eCFieldElementAdd2 = this.y.divide(eCFieldElementMultiply5).add(eCFieldElementMultiply5);
                        ECFieldElement eCFieldElementAdd3 = eCFieldElementAdd2.square().add(eCFieldElementAdd2).add(curve.getA());
                        return new F2m(curve, eCFieldElementAdd3, eCFieldElementMultiply5.squarePlusProduct(eCFieldElementAdd3, eCFieldElementAdd2.addOne()), this.withCompression);
                    case 1:
                        ECFieldElement eCFieldElementMultiply6 = this.y;
                        ECFieldElement eCFieldElement = this.zs[0];
                        boolean zIsOne = eCFieldElement.isOne();
                        if (!zIsOne) {
                            eCFieldElementMultiply4 = eCFieldElementMultiply5.multiply(eCFieldElement);
                        } else {
                            eCFieldElementMultiply4 = eCFieldElementMultiply5;
                        }
                        if (!zIsOne) {
                            eCFieldElementMultiply6 = eCFieldElementMultiply6.multiply(eCFieldElement);
                        }
                        ECFieldElement eCFieldElementSquare2 = eCFieldElementMultiply5.square();
                        ECFieldElement eCFieldElementAdd4 = eCFieldElementSquare2.add(eCFieldElementMultiply6);
                        ECFieldElement eCFieldElementSquare3 = eCFieldElementMultiply4.square();
                        ECFieldElement eCFieldElementAdd5 = eCFieldElementAdd4.add(eCFieldElementMultiply4);
                        ECFieldElement eCFieldElementMultiplyPlusProduct = eCFieldElementAdd5.multiplyPlusProduct(eCFieldElementAdd4, eCFieldElementSquare3, curve.getA());
                        return new F2m(curve, eCFieldElementMultiply4.multiply(eCFieldElementMultiplyPlusProduct), eCFieldElementSquare2.square().multiplyPlusProduct(eCFieldElementMultiply4, eCFieldElementMultiplyPlusProduct, eCFieldElementAdd5), new ECFieldElement[]{eCFieldElementMultiply4.multiply(eCFieldElementSquare3)}, this.withCompression);
                    default:
                        throw new IllegalStateException("unsupported coordinate system");
                }
            }
            ECFieldElement eCFieldElement2 = this.y;
            ECFieldElement eCFieldElement3 = this.zs[0];
            boolean zIsOne2 = eCFieldElement3.isOne();
            if (!zIsOne2) {
                eCFieldElementMultiply = eCFieldElement2.multiply(eCFieldElement3);
            } else {
                eCFieldElementMultiply = eCFieldElement2;
            }
            if (!zIsOne2) {
                eCFieldElementSquare = eCFieldElement3.square();
            } else {
                eCFieldElementSquare = eCFieldElement3;
            }
            ECFieldElement a = curve.getA();
            if (!zIsOne2) {
                eCFieldElementMultiply2 = a.multiply(eCFieldElementSquare);
            } else {
                eCFieldElementMultiply2 = a;
            }
            ECFieldElement eCFieldElementAdd6 = eCFieldElement2.square().add(eCFieldElementMultiply).add(eCFieldElementMultiply2);
            if (eCFieldElementAdd6.isZero()) {
                return new F2m(curve, eCFieldElementAdd6, curve.getB().sqrt(), this.withCompression);
            }
            ECFieldElement eCFieldElementSquare4 = eCFieldElementAdd6.square();
            if (!zIsOne2) {
                eCFieldElementMultiply3 = eCFieldElementAdd6.multiply(eCFieldElementSquare);
            } else {
                eCFieldElementMultiply3 = eCFieldElementAdd6;
            }
            ECFieldElement b = curve.getB();
            if (b.bitLength() < (curve.getFieldSize() >> 1)) {
                ECFieldElement eCFieldElementSquare5 = eCFieldElement2.add(eCFieldElementMultiply5).square();
                if (b.isOne()) {
                    eCFieldElementSquarePlusProduct = eCFieldElementMultiply2.add(eCFieldElementSquare).square();
                } else {
                    eCFieldElementSquarePlusProduct = eCFieldElementMultiply2.squarePlusProduct(b, eCFieldElementSquare.square());
                }
                eCFieldElementAdd = eCFieldElementSquare5.add(eCFieldElementAdd6).add(eCFieldElementSquare).multiply(eCFieldElementSquare5).add(eCFieldElementSquarePlusProduct).add(eCFieldElementSquare4);
                if (a.isZero()) {
                    eCFieldElementAdd = eCFieldElementAdd.add(eCFieldElementMultiply3);
                } else if (!a.isOne()) {
                    eCFieldElementAdd = eCFieldElementAdd.add(a.addOne().multiply(eCFieldElementMultiply3));
                }
            } else {
                if (!zIsOne2) {
                    eCFieldElementMultiply5 = eCFieldElementMultiply5.multiply(eCFieldElement3);
                }
                eCFieldElementAdd = eCFieldElementMultiply5.squarePlusProduct(eCFieldElementAdd6, eCFieldElementMultiply).add(eCFieldElementSquare4).add(eCFieldElementMultiply3);
            }
            return new F2m(curve, eCFieldElementSquare4, eCFieldElementAdd, new ECFieldElement[]{eCFieldElementMultiply3}, this.withCompression);
        }

        @Override
        public ECPoint twicePlus(ECPoint eCPoint) {
            if (isInfinity()) {
                return eCPoint;
            }
            if (eCPoint.isInfinity()) {
                return twice();
            }
            ECCurve curve = getCurve();
            ECFieldElement eCFieldElement = this.x;
            if (eCFieldElement.isZero()) {
                return eCPoint;
            }
            if (curve.getCoordinateSystem() == 6) {
                ECFieldElement eCFieldElement2 = eCPoint.x;
                ECFieldElement eCFieldElement3 = eCPoint.zs[0];
                if (eCFieldElement2.isZero() || !eCFieldElement3.isOne()) {
                    return twice().add(eCPoint);
                }
                ECFieldElement eCFieldElement4 = this.y;
                ECFieldElement eCFieldElement5 = this.zs[0];
                ECFieldElement eCFieldElement6 = eCPoint.y;
                ECFieldElement eCFieldElementSquare = eCFieldElement.square();
                ECFieldElement eCFieldElementSquare2 = eCFieldElement4.square();
                ECFieldElement eCFieldElementSquare3 = eCFieldElement5.square();
                ECFieldElement eCFieldElementAdd = curve.getA().multiply(eCFieldElementSquare3).add(eCFieldElementSquare2).add(eCFieldElement4.multiply(eCFieldElement5));
                ECFieldElement eCFieldElementAddOne = eCFieldElement6.addOne();
                ECFieldElement eCFieldElementMultiplyPlusProduct = curve.getA().add(eCFieldElementAddOne).multiply(eCFieldElementSquare3).add(eCFieldElementSquare2).multiplyPlusProduct(eCFieldElementAdd, eCFieldElementSquare, eCFieldElementSquare3);
                ECFieldElement eCFieldElementMultiply = eCFieldElement2.multiply(eCFieldElementSquare3);
                ECFieldElement eCFieldElementSquare4 = eCFieldElementMultiply.add(eCFieldElementAdd).square();
                if (eCFieldElementSquare4.isZero()) {
                    if (eCFieldElementMultiplyPlusProduct.isZero()) {
                        return eCPoint.twice();
                    }
                    return curve.getInfinity();
                }
                if (eCFieldElementMultiplyPlusProduct.isZero()) {
                    return new F2m(curve, eCFieldElementMultiplyPlusProduct, curve.getB().sqrt(), this.withCompression);
                }
                ECFieldElement eCFieldElementMultiply2 = eCFieldElementMultiplyPlusProduct.square().multiply(eCFieldElementMultiply);
                ECFieldElement eCFieldElementMultiply3 = eCFieldElementMultiplyPlusProduct.multiply(eCFieldElementSquare4).multiply(eCFieldElementSquare3);
                return new F2m(curve, eCFieldElementMultiply2, eCFieldElementMultiplyPlusProduct.add(eCFieldElementSquare4).square().multiplyPlusProduct(eCFieldElementAdd, eCFieldElementAddOne, eCFieldElementMultiply3), new ECFieldElement[]{eCFieldElementMultiply3}, this.withCompression);
            }
            return twice().add(eCPoint);
        }

        @Override
        public ECPoint negate() {
            if (isInfinity()) {
                return this;
            }
            ECFieldElement eCFieldElement = this.x;
            if (eCFieldElement.isZero()) {
                return this;
            }
            switch (getCurveCoordinateSystem()) {
                case 0:
                    return new F2m(this.curve, eCFieldElement, this.y.add(eCFieldElement), this.withCompression);
                case 1:
                    return new F2m(this.curve, eCFieldElement, this.y.add(eCFieldElement), new ECFieldElement[]{this.zs[0]}, this.withCompression);
                case 2:
                case 3:
                case 4:
                default:
                    throw new IllegalStateException("unsupported coordinate system");
                case 5:
                    return new F2m(this.curve, eCFieldElement, this.y.addOne(), this.withCompression);
                case 6:
                    ECFieldElement eCFieldElement2 = this.y;
                    ECFieldElement eCFieldElement3 = this.zs[0];
                    return new F2m(this.curve, eCFieldElement, eCFieldElement2.add(eCFieldElement3), new ECFieldElement[]{eCFieldElement3}, this.withCompression);
            }
        }
    }
}
