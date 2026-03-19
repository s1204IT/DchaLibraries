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

    protected static ECFieldElement[] getInitialZCoords(ECCurve curve) {
        int coord = curve == null ? 0 : curve.getCoordinateSystem();
        switch (coord) {
            case 0:
            case 5:
                return EMPTY_ZS;
            default:
                ECFieldElement one = curve.fromBigInteger(ECConstants.ONE);
                switch (coord) {
                    case 1:
                    case 2:
                    case 6:
                        return new ECFieldElement[]{one};
                    case 3:
                        return new ECFieldElement[]{one, one, one};
                    case 4:
                        return new ECFieldElement[]{one, curve.getA()};
                    case 5:
                    default:
                        throw new IllegalArgumentException("unknown coordinate system");
                }
        }
    }

    protected ECPoint(ECCurve curve, ECFieldElement x, ECFieldElement y) {
        this(curve, x, y, getInitialZCoords(curve));
    }

    protected ECPoint(ECCurve curve, ECFieldElement x, ECFieldElement y, ECFieldElement[] zs) {
        this.preCompTable = null;
        this.curve = curve;
        this.x = x;
        this.y = y;
        this.zs = zs;
    }

    protected boolean satisfiesCofactor() {
        BigInteger h = this.curve.getCofactor();
        return h == null || h.equals(ECConstants.ONE) || !ECAlgorithms.referenceMultiply(this, h).isInfinity();
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

    public ECFieldElement getZCoord(int index) {
        if (index < 0 || index >= this.zs.length) {
            return null;
        }
        return this.zs[index];
    }

    public ECFieldElement[] getZCoords() {
        int zsLen = this.zs.length;
        if (zsLen == 0) {
            return EMPTY_ZS;
        }
        ECFieldElement[] copy = new ECFieldElement[zsLen];
        System.arraycopy(this.zs, 0, copy, 0, zsLen);
        return copy;
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
        if (isNormalized()) {
        } else {
            throw new IllegalStateException("point not in normal form");
        }
    }

    public boolean isNormalized() {
        int coord = getCurveCoordinateSystem();
        if (coord == 0 || coord == 5 || isInfinity()) {
            return true;
        }
        return this.zs[0].isOne();
    }

    public ECPoint normalize() {
        if (isInfinity()) {
            return this;
        }
        switch (getCurveCoordinateSystem()) {
            case 0:
            case 5:
                break;
            default:
                ECFieldElement Z1 = getZCoord(0);
                if (!Z1.isOne()) {
                    break;
                }
                break;
        }
        return this;
    }

    ECPoint normalize(ECFieldElement zInv) {
        switch (getCurveCoordinateSystem()) {
            case 1:
            case 6:
                return createScaledPoint(zInv, zInv);
            case 2:
            case 3:
            case 4:
                ECFieldElement zInv2 = zInv.square();
                ECFieldElement zInv3 = zInv2.multiply(zInv);
                return createScaledPoint(zInv2, zInv3);
            case 5:
            default:
                throw new IllegalStateException("not a projective coordinate system");
        }
    }

    protected ECPoint createScaledPoint(ECFieldElement sx, ECFieldElement sy) {
        return getCurve().createRawPoint(getRawXCoord().multiply(sx), getRawYCoord().multiply(sy), this.withCompression);
    }

    public boolean isInfinity() {
        if (this.x == null || this.y == null) {
            return true;
        }
        if (this.zs.length > 0) {
            return this.zs[0].isZero();
        }
        return false;
    }

    public boolean isCompressed() {
        return this.withCompression;
    }

    public boolean isValid() {
        if (isInfinity()) {
            return true;
        }
        ECCurve curve = getCurve();
        return curve == null || (satisfiesCurveEquation() && satisfiesCofactor());
    }

    public ECPoint scaleX(ECFieldElement scale) {
        return isInfinity() ? this : getCurve().createRawPoint(getRawXCoord().multiply(scale), getRawYCoord(), getRawZCoords(), this.withCompression);
    }

    public ECPoint scaleY(ECFieldElement scale) {
        return isInfinity() ? this : getCurve().createRawPoint(getRawXCoord(), getRawYCoord().multiply(scale), getRawZCoords(), this.withCompression);
    }

    public boolean equals(ECPoint other) {
        if (other == null) {
            return false;
        }
        ECCurve c1 = getCurve();
        ECCurve c2 = other.getCurve();
        boolean n1 = c1 == null;
        boolean n2 = c2 == null;
        boolean i1 = isInfinity();
        boolean i2 = other.isInfinity();
        if (i1 || i2) {
            if (!i1 || !i2) {
                return false;
            }
            if (n1 || n2) {
                return true;
            }
            return c1.equals(c2);
        }
        ECPoint p1 = this;
        ECPoint p2 = other;
        if (!n1 || !n2) {
            if (n1) {
                p2 = other.normalize();
            } else if (n2) {
                p1 = normalize();
            } else {
                if (!c1.equals(c2)) {
                    return false;
                }
                ECPoint[] points = {this, c1.importPoint(other)};
                c1.normalizeAll(points);
                p1 = points[0];
                p2 = points[1];
            }
        }
        if (p1.getXCoord().equals(p2.getXCoord())) {
            return p1.getYCoord().equals(p2.getYCoord());
        }
        return false;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ECPoint) {
            return equals((ECPoint) obj);
        }
        return false;
    }

    public int hashCode() {
        ECCurve c = getCurve();
        int hc = c == null ? 0 : ~c.hashCode();
        if (!isInfinity()) {
            ECPoint p = normalize();
            return (hc ^ (p.getXCoord().hashCode() * 17)) ^ (p.getYCoord().hashCode() * 257);
        }
        return hc;
    }

    public String toString() {
        if (isInfinity()) {
            return "INF";
        }
        StringBuffer sb = new StringBuffer();
        sb.append('(');
        sb.append(getRawXCoord());
        sb.append(',');
        sb.append(getRawYCoord());
        for (int i = 0; i < this.zs.length; i++) {
            sb.append(',');
            sb.append(this.zs[i]);
        }
        sb.append(')');
        return sb.toString();
    }

    public byte[] getEncoded() {
        return getEncoded(this.withCompression);
    }

    public byte[] getEncoded(boolean compressed) {
        if (isInfinity()) {
            return new byte[1];
        }
        ECPoint normed = normalize();
        byte[] X = normed.getXCoord().getEncoded();
        if (compressed) {
            byte[] PO = new byte[X.length + 1];
            PO[0] = (byte) (normed.getCompressionYTilde() ? 3 : 2);
            System.arraycopy(X, 0, PO, 1, X.length);
            return PO;
        }
        byte[] Y = normed.getYCoord().getEncoded();
        byte[] PO2 = new byte[X.length + Y.length + 1];
        PO2[0] = 4;
        System.arraycopy(X, 0, PO2, 1, X.length);
        System.arraycopy(Y, 0, PO2, X.length + 1, Y.length);
        return PO2;
    }

    public ECPoint timesPow2(int e) {
        if (e < 0) {
            throw new IllegalArgumentException("'e' cannot be negative");
        }
        ECPoint p = this;
        while (true) {
            e--;
            if (e >= 0) {
                p = p.twice();
            } else {
                return p;
            }
        }
    }

    public ECPoint twicePlus(ECPoint b) {
        return twice().add(b);
    }

    public ECPoint threeTimes() {
        return twicePlus(this);
    }

    public ECPoint multiply(BigInteger k) {
        return getCurve().getMultiplier().multiply(this, k);
    }

    public static abstract class AbstractFp extends ECPoint {
        protected AbstractFp(ECCurve curve, ECFieldElement x, ECFieldElement y) {
            super(curve, x, y);
        }

        protected AbstractFp(ECCurve curve, ECFieldElement x, ECFieldElement y, ECFieldElement[] zs) {
            super(curve, x, y, zs);
        }

        @Override
        protected boolean getCompressionYTilde() {
            return getAffineYCoord().testBitZero();
        }

        @Override
        protected boolean satisfiesCurveEquation() {
            ECFieldElement X = this.x;
            ECFieldElement Y = this.y;
            ECFieldElement A = this.curve.getA();
            ECFieldElement B = this.curve.getB();
            ECFieldElement lhs = Y.square();
            switch (getCurveCoordinateSystem()) {
                case 0:
                    break;
                case 1:
                    ECFieldElement Z = this.zs[0];
                    if (!Z.isOne()) {
                        ECFieldElement Z2 = Z.square();
                        ECFieldElement Z3 = Z.multiply(Z2);
                        lhs = lhs.multiply(Z);
                        A = A.multiply(Z2);
                        B = B.multiply(Z3);
                    }
                    break;
                case 2:
                case 3:
                case 4:
                    ECFieldElement Z4 = this.zs[0];
                    if (!Z4.isOne()) {
                        ECFieldElement Z22 = Z4.square();
                        ECFieldElement Z42 = Z22.square();
                        ECFieldElement Z6 = Z22.multiply(Z42);
                        A = A.multiply(Z42);
                        B = B.multiply(Z6);
                    }
                    break;
                default:
                    throw new IllegalStateException("unsupported coordinate system");
            }
            ECFieldElement rhs = X.square().add(A).multiply(X).add(B);
            return lhs.equals(rhs);
        }

        @Override
        public ECPoint subtract(ECPoint b) {
            if (b.isInfinity()) {
                return this;
            }
            return add(b.negate());
        }
    }

    public static class Fp extends AbstractFp {
        public Fp(ECCurve curve, ECFieldElement x, ECFieldElement y) {
            this(curve, x, y, false);
        }

        public Fp(ECCurve curve, ECFieldElement x, ECFieldElement y, boolean withCompression) {
            super(curve, x, y);
            if ((x == null) != (y == null)) {
                throw new IllegalArgumentException("Exactly one of the field elements is null");
            }
            this.withCompression = withCompression;
        }

        Fp(ECCurve curve, ECFieldElement x, ECFieldElement y, ECFieldElement[] zs, boolean withCompression) {
            super(curve, x, y, zs);
            this.withCompression = withCompression;
        }

        @Override
        protected ECPoint detach() {
            return new Fp(null, getAffineXCoord(), getAffineYCoord());
        }

        @Override
        public ECFieldElement getZCoord(int index) {
            if (index == 1 && 4 == getCurveCoordinateSystem()) {
                return getJacobianModifiedW();
            }
            return super.getZCoord(index);
        }

        @Override
        public ECPoint add(ECPoint b) {
            ECFieldElement U2;
            ECFieldElement S2;
            ECFieldElement U1;
            ECFieldElement S1;
            ECFieldElement X3;
            ECFieldElement Y3;
            ECFieldElement Z3;
            ECFieldElement[] zs;
            ECFieldElement w;
            if (isInfinity()) {
                return b;
            }
            if (b.isInfinity()) {
                return this;
            }
            if (this == b) {
                return twice();
            }
            ECCurve curve = getCurve();
            int coord = curve.getCoordinateSystem();
            ECFieldElement X1 = this.x;
            ECFieldElement Y1 = this.y;
            ECFieldElement X2 = b.x;
            ECFieldElement Y2 = b.y;
            switch (coord) {
                case 0:
                    ECFieldElement dx = X2.subtract(X1);
                    ECFieldElement dy = Y2.subtract(Y1);
                    if (dx.isZero()) {
                        if (dy.isZero()) {
                            return twice();
                        }
                        return curve.getInfinity();
                    }
                    ECFieldElement gamma = dy.divide(dx);
                    ECFieldElement X32 = gamma.square().subtract(X1).subtract(X2);
                    return new Fp(curve, X32, gamma.multiply(X1.subtract(X32)).subtract(Y1), this.withCompression);
                case 1:
                    ECFieldElement Z1 = this.zs[0];
                    ECFieldElement Z2 = b.zs[0];
                    boolean Z1IsOne = Z1.isOne();
                    boolean Z2IsOne = Z2.isOne();
                    ECFieldElement u1 = Z1IsOne ? Y2 : Y2.multiply(Z1);
                    ECFieldElement u2 = Z2IsOne ? Y1 : Y1.multiply(Z2);
                    ECFieldElement u = u1.subtract(u2);
                    ECFieldElement v1 = Z1IsOne ? X2 : X2.multiply(Z1);
                    ECFieldElement v2 = Z2IsOne ? X1 : X1.multiply(Z2);
                    ECFieldElement v = v1.subtract(v2);
                    if (v.isZero()) {
                        if (u.isZero()) {
                            return twice();
                        }
                        return curve.getInfinity();
                    }
                    if (Z1IsOne) {
                        w = Z2;
                    } else {
                        w = Z2IsOne ? Z1 : Z1.multiply(Z2);
                    }
                    ECFieldElement vSquared = v.square();
                    ECFieldElement vCubed = vSquared.multiply(v);
                    ECFieldElement vSquaredV2 = vSquared.multiply(v2);
                    ECFieldElement A = u.square().multiply(w).subtract(vCubed).subtract(two(vSquaredV2));
                    ECFieldElement X33 = v.multiply(A);
                    ECFieldElement Y32 = vSquaredV2.subtract(A).multiplyMinusProduct(u, u2, vCubed);
                    ECFieldElement Z32 = vCubed.multiply(w);
                    return new Fp(curve, X33, Y32, new ECFieldElement[]{Z32}, this.withCompression);
                case 2:
                case 4:
                    ECFieldElement Z12 = this.zs[0];
                    ECFieldElement Z22 = b.zs[0];
                    boolean Z1IsOne2 = Z12.isOne();
                    ECFieldElement Z3Squared = null;
                    if (!Z1IsOne2 && Z12.equals(Z22)) {
                        ECFieldElement dx2 = X1.subtract(X2);
                        ECFieldElement dy2 = Y1.subtract(Y2);
                        if (dx2.isZero()) {
                            if (dy2.isZero()) {
                                return twice();
                            }
                            return curve.getInfinity();
                        }
                        ECFieldElement C = dx2.square();
                        ECFieldElement W1 = X1.multiply(C);
                        ECFieldElement W2 = X2.multiply(C);
                        ECFieldElement A1 = W1.subtract(W2).multiply(Y1);
                        X3 = dy2.square().subtract(W1).subtract(W2);
                        Y3 = W1.subtract(X3).multiply(dy2).subtract(A1);
                        Z3 = dx2.multiply(Z12);
                    } else {
                        if (Z1IsOne2) {
                            U2 = X2;
                            S2 = Y2;
                        } else {
                            ECFieldElement Z1Squared = Z12.square();
                            U2 = Z1Squared.multiply(X2);
                            ECFieldElement Z1Cubed = Z1Squared.multiply(Z12);
                            S2 = Z1Cubed.multiply(Y2);
                        }
                        boolean Z2IsOne2 = Z22.isOne();
                        if (Z2IsOne2) {
                            U1 = X1;
                            S1 = Y1;
                        } else {
                            ECFieldElement Z2Squared = Z22.square();
                            U1 = Z2Squared.multiply(X1);
                            ECFieldElement Z2Cubed = Z2Squared.multiply(Z22);
                            S1 = Z2Cubed.multiply(Y1);
                        }
                        ECFieldElement H = U1.subtract(U2);
                        ECFieldElement R = S1.subtract(S2);
                        if (H.isZero()) {
                            if (R.isZero()) {
                                return twice();
                            }
                            return curve.getInfinity();
                        }
                        ECFieldElement HSquared = H.square();
                        ECFieldElement G = HSquared.multiply(H);
                        ECFieldElement V = HSquared.multiply(U1);
                        X3 = R.square().add(G).subtract(two(V));
                        Y3 = V.subtract(X3).multiplyMinusProduct(R, G, S1);
                        Z3 = H;
                        if (!Z1IsOne2) {
                            Z3 = H.multiply(Z12);
                        }
                        if (!Z2IsOne2) {
                            Z3 = Z3.multiply(Z22);
                        }
                        if (Z3 == H) {
                            Z3Squared = HSquared;
                        }
                    }
                    if (coord == 4) {
                        ECFieldElement W3 = calculateJacobianModifiedW(Z3, Z3Squared);
                        zs = new ECFieldElement[]{Z3, W3};
                    } else {
                        zs = new ECFieldElement[]{Z3};
                    }
                    return new Fp(curve, X3, Y3, zs, this.withCompression);
                case 3:
                default:
                    throw new IllegalStateException("unsupported coordinate system");
            }
        }

        @Override
        public ECPoint twice() {
            ECFieldElement M;
            ECFieldElement S;
            if (isInfinity()) {
                return this;
            }
            ECCurve curve = getCurve();
            ECFieldElement Y1 = this.y;
            if (Y1.isZero()) {
                return curve.getInfinity();
            }
            int coord = curve.getCoordinateSystem();
            ECFieldElement X1 = this.x;
            switch (coord) {
                case 0:
                    ECFieldElement X1Squared = X1.square();
                    ECFieldElement gamma = three(X1Squared).add(getCurve().getA()).divide(two(Y1));
                    ECFieldElement X3 = gamma.square().subtract(two(X1));
                    ECFieldElement Y3 = gamma.multiply(X1.subtract(X3)).subtract(Y1);
                    return new Fp(curve, X3, Y3, this.withCompression);
                case 1:
                    ECFieldElement Z1 = this.zs[0];
                    boolean Z1IsOne = Z1.isOne();
                    ECFieldElement w = curve.getA();
                    if (!w.isZero() && !Z1IsOne) {
                        w = w.multiply(Z1.square());
                    }
                    ECFieldElement w2 = w.add(three(X1.square()));
                    ECFieldElement s = Z1IsOne ? Y1 : Y1.multiply(Z1);
                    ECFieldElement t = Z1IsOne ? Y1.square() : s.multiply(Y1);
                    ECFieldElement B = X1.multiply(t);
                    ECFieldElement _4B = four(B);
                    ECFieldElement h = w2.square().subtract(two(_4B));
                    ECFieldElement _2s = two(s);
                    ECFieldElement X32 = h.multiply(_2s);
                    ECFieldElement _2t = two(t);
                    ECFieldElement Y32 = _4B.subtract(h).multiply(w2).subtract(two(_2t.square()));
                    ECFieldElement _4sSquared = Z1IsOne ? two(_2t) : _2s.square();
                    return new Fp(curve, X32, Y32, new ECFieldElement[]{two(_4sSquared).multiply(s)}, this.withCompression);
                case 2:
                    ECFieldElement Z12 = this.zs[0];
                    boolean Z1IsOne2 = Z12.isOne();
                    ECFieldElement Y1Squared = Y1.square();
                    ECFieldElement T = Y1Squared.square();
                    ECFieldElement a4 = curve.getA();
                    ECFieldElement a4Neg = a4.negate();
                    if (a4Neg.toBigInteger().equals(BigInteger.valueOf(3L))) {
                        ECFieldElement Z1Squared = Z1IsOne2 ? Z12 : Z12.square();
                        M = three(X1.add(Z1Squared).multiply(X1.subtract(Z1Squared)));
                        S = four(Y1Squared.multiply(X1));
                    } else {
                        ECFieldElement X1Squared2 = X1.square();
                        M = three(X1Squared2);
                        if (Z1IsOne2) {
                            M = M.add(a4);
                        } else if (!a4.isZero()) {
                            ECFieldElement Z1Pow4 = Z12.square().square();
                            if (a4Neg.bitLength() < a4.bitLength()) {
                                M = M.subtract(Z1Pow4.multiply(a4Neg));
                            } else {
                                M = M.add(Z1Pow4.multiply(a4));
                            }
                        }
                        S = four(X1.multiply(Y1Squared));
                    }
                    ECFieldElement X33 = M.square().subtract(two(S));
                    ECFieldElement Y33 = S.subtract(X33).multiply(M).subtract(eight(T));
                    ECFieldElement Z3 = two(Y1);
                    if (!Z1IsOne2) {
                        Z3 = Z3.multiply(Z12);
                    }
                    return new Fp(curve, X33, Y33, new ECFieldElement[]{Z3}, this.withCompression);
                case 3:
                default:
                    throw new IllegalStateException("unsupported coordinate system");
                case 4:
                    return twiceJacobianModified(true);
            }
        }

        @Override
        public ECPoint twicePlus(ECPoint b) {
            if (this == b) {
                return threeTimes();
            }
            if (isInfinity()) {
                return b;
            }
            if (b.isInfinity()) {
                return twice();
            }
            ECFieldElement Y1 = this.y;
            if (Y1.isZero()) {
                return b;
            }
            ECCurve curve = getCurve();
            int coord = curve.getCoordinateSystem();
            switch (coord) {
                case 0:
                    ECFieldElement X1 = this.x;
                    ECFieldElement X2 = b.x;
                    ECFieldElement Y2 = b.y;
                    ECFieldElement dx = X2.subtract(X1);
                    ECFieldElement dy = Y2.subtract(Y1);
                    if (dx.isZero()) {
                        if (dy.isZero()) {
                            return threeTimes();
                        }
                        return this;
                    }
                    ECFieldElement X = dx.square();
                    ECFieldElement Y = dy.square();
                    ECFieldElement d = X.multiply(two(X1).add(X2)).subtract(Y);
                    if (d.isZero()) {
                        return curve.getInfinity();
                    }
                    ECFieldElement D = d.multiply(dx);
                    ECFieldElement I = D.invert();
                    ECFieldElement L1 = d.multiply(I).multiply(dy);
                    ECFieldElement L2 = two(Y1).multiply(X).multiply(dx).multiply(I).subtract(L1);
                    ECFieldElement X4 = L2.subtract(L1).multiply(L1.add(L2)).add(X2);
                    ECFieldElement Y4 = X1.subtract(X4).multiply(L2).subtract(Y1);
                    return new Fp(curve, X4, Y4, this.withCompression);
                case 4:
                    return twiceJacobianModified(false).add(b);
                default:
                    return twice().add(b);
            }
        }

        @Override
        public ECPoint threeTimes() {
            if (isInfinity()) {
                return this;
            }
            ECFieldElement Y1 = this.y;
            if (Y1.isZero()) {
                return this;
            }
            ECCurve curve = getCurve();
            int coord = curve.getCoordinateSystem();
            switch (coord) {
                case 0:
                    ECFieldElement X1 = this.x;
                    ECFieldElement _2Y1 = two(Y1);
                    ECFieldElement X = _2Y1.square();
                    ECFieldElement Z = three(X1.square()).add(getCurve().getA());
                    ECFieldElement Y = Z.square();
                    ECFieldElement d = three(X1).multiply(X).subtract(Y);
                    if (!d.isZero()) {
                        ECFieldElement D = d.multiply(_2Y1);
                        ECFieldElement I = D.invert();
                        ECFieldElement L1 = d.multiply(I).multiply(Z);
                        ECFieldElement L2 = X.square().multiply(I).subtract(L1);
                        ECFieldElement X4 = L2.subtract(L1).multiply(L1.add(L2)).add(X1);
                        ECFieldElement Y4 = X1.subtract(X4).multiply(L2).subtract(Y1);
                    }
                    break;
                case 4:
                    break;
            }
            return this;
        }

        @Override
        public ECPoint timesPow2(int e) {
            if (e < 0) {
                throw new IllegalArgumentException("'e' cannot be negative");
            }
            if (e == 0 || isInfinity()) {
                return this;
            }
            if (e == 1) {
                return twice();
            }
            ECCurve curve = getCurve();
            ECFieldElement Y1 = this.y;
            if (Y1.isZero()) {
                return curve.getInfinity();
            }
            int coord = curve.getCoordinateSystem();
            ECFieldElement W1 = curve.getA();
            ECFieldElement X1 = this.x;
            ECFieldElement Z1 = this.zs.length < 1 ? curve.fromBigInteger(ECConstants.ONE) : this.zs[0];
            if (!Z1.isOne()) {
                switch (coord) {
                    case 0:
                        break;
                    case 1:
                        ECFieldElement Z1Sq = Z1.square();
                        X1 = X1.multiply(Z1);
                        Y1 = Y1.multiply(Z1Sq);
                        W1 = calculateJacobianModifiedW(Z1, Z1Sq);
                        break;
                    case 2:
                        W1 = calculateJacobianModifiedW(Z1, null);
                        break;
                    case 3:
                    default:
                        throw new IllegalStateException("unsupported coordinate system");
                    case 4:
                        W1 = getJacobianModifiedW();
                        break;
                }
            }
            for (int i = 0; i < e; i++) {
                if (Y1.isZero()) {
                    return curve.getInfinity();
                }
                ECFieldElement X1Squared = X1.square();
                ECFieldElement M = three(X1Squared);
                ECFieldElement _2Y1 = two(Y1);
                ECFieldElement _2Y1Squared = _2Y1.multiply(Y1);
                ECFieldElement S = two(X1.multiply(_2Y1Squared));
                ECFieldElement _4T = _2Y1Squared.square();
                ECFieldElement _8T = two(_4T);
                if (!W1.isZero()) {
                    M = M.add(W1);
                    W1 = two(_8T.multiply(W1));
                }
                X1 = M.square().subtract(two(S));
                Y1 = M.multiply(S.subtract(X1)).subtract(_8T);
                Z1 = Z1.isOne() ? _2Y1 : _2Y1.multiply(Z1);
            }
            switch (coord) {
                case 0:
                    ECFieldElement zInv = Z1.invert();
                    ECFieldElement zInv2 = zInv.square();
                    ECFieldElement zInv3 = zInv2.multiply(zInv);
                    return new Fp(curve, X1.multiply(zInv2), Y1.multiply(zInv3), this.withCompression);
                case 1:
                    return new Fp(curve, X1.multiply(Z1), Y1, new ECFieldElement[]{Z1.multiply(Z1.square())}, this.withCompression);
                case 2:
                    return new Fp(curve, X1, Y1, new ECFieldElement[]{Z1}, this.withCompression);
                case 3:
                default:
                    throw new IllegalStateException("unsupported coordinate system");
                case 4:
                    return new Fp(curve, X1, Y1, new ECFieldElement[]{Z1, W1}, this.withCompression);
            }
        }

        protected ECFieldElement two(ECFieldElement x) {
            return x.add(x);
        }

        protected ECFieldElement three(ECFieldElement x) {
            return two(x).add(x);
        }

        protected ECFieldElement four(ECFieldElement x) {
            return two(two(x));
        }

        protected ECFieldElement eight(ECFieldElement x) {
            return four(two(x));
        }

        protected ECFieldElement doubleProductFromSquares(ECFieldElement a, ECFieldElement b, ECFieldElement aSquared, ECFieldElement bSquared) {
            return a.add(b).square().subtract(aSquared).subtract(bSquared);
        }

        @Override
        public ECPoint negate() {
            if (isInfinity()) {
                return this;
            }
            ECCurve curve = getCurve();
            int coord = curve.getCoordinateSystem();
            if (coord != 0) {
                return new Fp(curve, this.x, this.y.negate(), this.zs, this.withCompression);
            }
            return new Fp(curve, this.x, this.y.negate(), this.withCompression);
        }

        protected ECFieldElement calculateJacobianModifiedW(ECFieldElement Z, ECFieldElement ZSquared) {
            ECFieldElement a4 = getCurve().getA();
            if (a4.isZero() || Z.isOne()) {
                return a4;
            }
            if (ZSquared == null) {
                ZSquared = Z.square();
            }
            ECFieldElement W = ZSquared.square();
            ECFieldElement a4Neg = a4.negate();
            if (a4Neg.bitLength() < a4.bitLength()) {
                return W.multiply(a4Neg).negate();
            }
            return W.multiply(a4);
        }

        protected ECFieldElement getJacobianModifiedW() {
            ECFieldElement W = this.zs[1];
            if (W == null) {
                ECFieldElement[] eCFieldElementArr = this.zs;
                ECFieldElement W2 = calculateJacobianModifiedW(this.zs[0], null);
                eCFieldElementArr[1] = W2;
                return W2;
            }
            return W;
        }

        protected Fp twiceJacobianModified(boolean calculateW) {
            ECFieldElement X1 = this.x;
            ECFieldElement Y1 = this.y;
            ECFieldElement Z1 = this.zs[0];
            ECFieldElement W1 = getJacobianModifiedW();
            ECFieldElement X1Squared = X1.square();
            ECFieldElement M = three(X1Squared).add(W1);
            ECFieldElement _2Y1 = two(Y1);
            ECFieldElement _2Y1Squared = _2Y1.multiply(Y1);
            ECFieldElement S = two(X1.multiply(_2Y1Squared));
            ECFieldElement X3 = M.square().subtract(two(S));
            ECFieldElement _4T = _2Y1Squared.square();
            ECFieldElement _8T = two(_4T);
            ECFieldElement Y3 = M.multiply(S.subtract(X3)).subtract(_8T);
            ECFieldElement eCFieldElementTwo = calculateW ? two(_8T.multiply(W1)) : null;
            ECFieldElement Z3 = Z1.isOne() ? _2Y1 : _2Y1.multiply(Z1);
            return new Fp(getCurve(), X3, Y3, new ECFieldElement[]{Z3, eCFieldElementTwo}, this.withCompression);
        }
    }

    public static abstract class AbstractF2m extends ECPoint {
        protected AbstractF2m(ECCurve curve, ECFieldElement x, ECFieldElement y) {
            super(curve, x, y);
        }

        protected AbstractF2m(ECCurve curve, ECFieldElement x, ECFieldElement y, ECFieldElement[] zs) {
            super(curve, x, y, zs);
        }

        @Override
        protected boolean satisfiesCurveEquation() {
            ECFieldElement lhs;
            ECFieldElement rhs;
            ECCurve curve = getCurve();
            ECFieldElement X = this.x;
            ECFieldElement A = curve.getA();
            ECFieldElement B = curve.getB();
            int coord = curve.getCoordinateSystem();
            if (coord == 6) {
                ECFieldElement Z = this.zs[0];
                boolean ZIsOne = Z.isOne();
                if (X.isZero()) {
                    ECFieldElement lhs2 = this.y.square();
                    ECFieldElement rhs2 = B;
                    if (!ZIsOne) {
                        rhs2 = B.multiply(Z.square());
                    }
                    return lhs2.equals(rhs2);
                }
                ECFieldElement L = this.y;
                ECFieldElement X2 = X.square();
                if (ZIsOne) {
                    lhs = L.square().add(L).add(A);
                    rhs = X2.square().add(B);
                } else {
                    ECFieldElement Z2 = Z.square();
                    ECFieldElement Z4 = Z2.square();
                    lhs = L.add(Z).multiplyPlusProduct(L, A, Z2);
                    rhs = X2.squarePlusProduct(B, Z4);
                }
                return lhs.multiply(X2).equals(rhs);
            }
            ECFieldElement Y = this.y;
            ECFieldElement lhs3 = Y.add(X).multiply(Y);
            switch (coord) {
                case 0:
                    break;
                case 1:
                    ECFieldElement Z3 = this.zs[0];
                    if (!Z3.isOne()) {
                        ECFieldElement Z32 = Z3.multiply(Z3.square());
                        lhs3 = lhs3.multiply(Z3);
                        A = A.multiply(Z3);
                        B = B.multiply(Z32);
                    }
                    break;
                default:
                    throw new IllegalStateException("unsupported coordinate system");
            }
            ECFieldElement rhs3 = X.add(A).multiply(X.square()).add(B);
            return lhs3.equals(rhs3);
        }

        @Override
        public ECPoint scaleX(ECFieldElement scale) {
            if (isInfinity()) {
                return this;
            }
            int coord = getCurveCoordinateSystem();
            switch (coord) {
                case 5:
                    ECFieldElement X = getRawXCoord();
                    ECFieldElement L = getRawYCoord();
                    ECFieldElement L2 = L.add(X).divide(scale).add(X.multiply(scale));
                    return getCurve().createRawPoint(X, L2, getRawZCoords(), this.withCompression);
                case 6:
                    ECFieldElement X2 = getRawXCoord();
                    ECFieldElement L3 = getRawYCoord();
                    ECFieldElement Z = getRawZCoords()[0];
                    ECFieldElement X22 = X2.multiply(scale.square());
                    ECFieldElement L22 = L3.add(X2).add(X22);
                    ECFieldElement Z2 = Z.multiply(scale);
                    return getCurve().createRawPoint(X22, L22, new ECFieldElement[]{Z2}, this.withCompression);
                default:
                    return super.scaleX(scale);
            }
        }

        @Override
        public ECPoint scaleY(ECFieldElement scale) {
            if (isInfinity()) {
                return this;
            }
            int coord = getCurveCoordinateSystem();
            switch (coord) {
                case 5:
                case 6:
                    ECFieldElement X = getRawXCoord();
                    ECFieldElement L = getRawYCoord();
                    ECFieldElement L2 = L.add(X).multiply(scale).add(X);
                    return getCurve().createRawPoint(X, L2, getRawZCoords(), this.withCompression);
                default:
                    return super.scaleY(scale);
            }
        }

        @Override
        public ECPoint subtract(ECPoint b) {
            if (b.isInfinity()) {
                return this;
            }
            return add(b.negate());
        }

        public AbstractF2m tau() {
            if (isInfinity()) {
                return this;
            }
            ECCurve curve = getCurve();
            int coord = curve.getCoordinateSystem();
            ECFieldElement X1 = this.x;
            switch (coord) {
                case 0:
                case 5:
                    ECFieldElement Y1 = this.y;
                    return (AbstractF2m) curve.createRawPoint(X1.square(), Y1.square(), this.withCompression);
                case 1:
                case 6:
                    ECFieldElement Y12 = this.y;
                    ECFieldElement Z1 = this.zs[0];
                    return (AbstractF2m) curve.createRawPoint(X1.square(), Y12.square(), new ECFieldElement[]{Z1.square()}, this.withCompression);
                case 2:
                case 3:
                case 4:
                default:
                    throw new IllegalStateException("unsupported coordinate system");
            }
        }

        public AbstractF2m tauPow(int pow) {
            if (isInfinity()) {
                return this;
            }
            ECCurve curve = getCurve();
            int coord = curve.getCoordinateSystem();
            ECFieldElement X1 = this.x;
            switch (coord) {
                case 0:
                case 5:
                    ECFieldElement Y1 = this.y;
                    return (AbstractF2m) curve.createRawPoint(X1.squarePow(pow), Y1.squarePow(pow), this.withCompression);
                case 1:
                case 6:
                    ECFieldElement Y12 = this.y;
                    ECFieldElement Z1 = this.zs[0];
                    return (AbstractF2m) curve.createRawPoint(X1.squarePow(pow), Y12.squarePow(pow), new ECFieldElement[]{Z1.squarePow(pow)}, this.withCompression);
                case 2:
                case 3:
                case 4:
                default:
                    throw new IllegalStateException("unsupported coordinate system");
            }
        }
    }

    public static class F2m extends AbstractF2m {
        public F2m(ECCurve curve, ECFieldElement x, ECFieldElement y) {
            this(curve, x, y, false);
        }

        public F2m(ECCurve curve, ECFieldElement x, ECFieldElement y, boolean withCompression) {
            super(curve, x, y);
            if ((x == null) != (y == null)) {
                throw new IllegalArgumentException("Exactly one of the field elements is null");
            }
            if (x != null) {
                ECFieldElement.F2m.checkFieldElements(this.x, this.y);
                if (curve != null) {
                    ECFieldElement.F2m.checkFieldElements(this.x, this.curve.getA());
                }
            }
            this.withCompression = withCompression;
        }

        F2m(ECCurve curve, ECFieldElement x, ECFieldElement y, ECFieldElement[] zs, boolean withCompression) {
            super(curve, x, y, zs);
            this.withCompression = withCompression;
        }

        @Override
        protected ECPoint detach() {
            return new F2m(null, getAffineXCoord(), getAffineYCoord());
        }

        @Override
        public ECFieldElement getYCoord() {
            int coord = getCurveCoordinateSystem();
            switch (coord) {
                case 5:
                case 6:
                    ECFieldElement X = this.x;
                    ECFieldElement L = this.y;
                    if (isInfinity() || X.isZero()) {
                        return L;
                    }
                    ECFieldElement Y = L.add(X).multiply(X);
                    if (6 == coord) {
                        ECFieldElement Z = this.zs[0];
                        if (!Z.isOne()) {
                            return Y.divide(Z);
                        }
                        return Y;
                    }
                    return Y;
                default:
                    return this.y;
            }
        }

        @Override
        protected boolean getCompressionYTilde() {
            ECFieldElement X = getRawXCoord();
            if (X.isZero()) {
                return false;
            }
            ECFieldElement Y = getRawYCoord();
            switch (getCurveCoordinateSystem()) {
                case 5:
                case 6:
                    return Y.testBitZero() != X.testBitZero();
                default:
                    return Y.divide(X).testBitZero();
            }
        }

        @Override
        public ECPoint add(ECPoint b) {
            ECFieldElement X3;
            ECFieldElement L3;
            ECFieldElement Z3;
            if (isInfinity()) {
                return b;
            }
            if (b.isInfinity()) {
                return this;
            }
            ECCurve curve = getCurve();
            int coord = curve.getCoordinateSystem();
            ECFieldElement X1 = this.x;
            ECFieldElement X2 = b.x;
            switch (coord) {
                case 0:
                    ECFieldElement Y1 = this.y;
                    ECFieldElement Y2 = b.y;
                    ECFieldElement dx = X1.add(X2);
                    ECFieldElement dy = Y1.add(Y2);
                    if (dx.isZero()) {
                        if (dy.isZero()) {
                            return twice();
                        }
                        return curve.getInfinity();
                    }
                    ECFieldElement L = dy.divide(dx);
                    ECFieldElement X32 = L.square().add(L).add(dx).add(curve.getA());
                    ECFieldElement Y3 = L.multiply(X1.add(X32)).add(X32).add(Y1);
                    return new F2m(curve, X32, Y3, this.withCompression);
                case 1:
                    ECFieldElement Y12 = this.y;
                    ECFieldElement Z1 = this.zs[0];
                    ECFieldElement Y22 = b.y;
                    ECFieldElement Z2 = b.zs[0];
                    boolean Z2IsOne = Z2.isOne();
                    ECFieldElement U1 = Z1.multiply(Y22);
                    ECFieldElement U2 = Z2IsOne ? Y12 : Y12.multiply(Z2);
                    ECFieldElement U = U1.add(U2);
                    ECFieldElement V1 = Z1.multiply(X2);
                    ECFieldElement V2 = Z2IsOne ? X1 : X1.multiply(Z2);
                    ECFieldElement V = V1.add(V2);
                    if (V.isZero()) {
                        if (U.isZero()) {
                            return twice();
                        }
                        return curve.getInfinity();
                    }
                    ECFieldElement VSq = V.square();
                    ECFieldElement VCu = VSq.multiply(V);
                    ECFieldElement W = Z2IsOne ? Z1 : Z1.multiply(Z2);
                    ECFieldElement uv = U.add(V);
                    ECFieldElement A = uv.multiplyPlusProduct(U, VSq, curve.getA()).multiply(W).add(VCu);
                    ECFieldElement X33 = V.multiply(A);
                    ECFieldElement VSqZ2 = Z2IsOne ? VSq : VSq.multiply(Z2);
                    ECFieldElement Y32 = U.multiplyPlusProduct(X1, V, Y12).multiplyPlusProduct(VSqZ2, uv, A);
                    return new F2m(curve, X33, Y32, new ECFieldElement[]{VCu.multiply(W)}, this.withCompression);
                case 6:
                    if (X1.isZero()) {
                        if (X2.isZero()) {
                            return curve.getInfinity();
                        }
                        return b.add(this);
                    }
                    ECFieldElement L1 = this.y;
                    ECFieldElement Z12 = this.zs[0];
                    ECFieldElement L2 = b.y;
                    ECFieldElement Z22 = b.zs[0];
                    boolean Z1IsOne = Z12.isOne();
                    ECFieldElement U22 = X2;
                    ECFieldElement S2 = L2;
                    if (!Z1IsOne) {
                        U22 = X2.multiply(Z12);
                        S2 = L2.multiply(Z12);
                    }
                    boolean Z2IsOne2 = Z22.isOne();
                    ECFieldElement U12 = X1;
                    ECFieldElement S1 = L1;
                    if (!Z2IsOne2) {
                        U12 = X1.multiply(Z22);
                        S1 = L1.multiply(Z22);
                    }
                    ECFieldElement A2 = S1.add(S2);
                    ECFieldElement B = U12.add(U22);
                    if (B.isZero()) {
                        if (A2.isZero()) {
                            return twice();
                        }
                        return curve.getInfinity();
                    }
                    if (X2.isZero()) {
                        ECPoint p = normalize();
                        ECFieldElement X12 = p.getXCoord();
                        ECFieldElement Y13 = p.getYCoord();
                        ECFieldElement L4 = Y13.add(L2).divide(X12);
                        X3 = L4.square().add(L4).add(X12).add(curve.getA());
                        if (X3.isZero()) {
                            return new F2m(curve, X3, curve.getB().sqrt(), this.withCompression);
                        }
                        ECFieldElement Y33 = L4.multiply(X12.add(X3)).add(X3).add(Y13);
                        L3 = Y33.divide(X3).add(X3);
                        Z3 = curve.fromBigInteger(ECConstants.ONE);
                    } else {
                        ECFieldElement B2 = B.square();
                        ECFieldElement AU1 = A2.multiply(U12);
                        ECFieldElement AU2 = A2.multiply(U22);
                        X3 = AU1.multiply(AU2);
                        if (X3.isZero()) {
                            return new F2m(curve, X3, curve.getB().sqrt(), this.withCompression);
                        }
                        ECFieldElement ABZ2 = A2.multiply(B2);
                        if (!Z2IsOne2) {
                            ABZ2 = ABZ2.multiply(Z22);
                        }
                        L3 = AU2.add(B2).squarePlusProduct(ABZ2, L1.add(Z12));
                        Z3 = ABZ2;
                        if (!Z1IsOne) {
                            Z3 = Z3.multiply(Z12);
                        }
                    }
                    return new F2m(curve, X3, L3, new ECFieldElement[]{Z3}, this.withCompression);
                default:
                    throw new IllegalStateException("unsupported coordinate system");
            }
        }

        @Override
        public ECPoint twice() {
            ECFieldElement L3;
            ECFieldElement t2;
            if (isInfinity()) {
                return this;
            }
            ECCurve curve = getCurve();
            ECFieldElement X1 = this.x;
            if (X1.isZero()) {
                return curve.getInfinity();
            }
            int coord = curve.getCoordinateSystem();
            switch (coord) {
                case 0:
                    ECFieldElement L1 = this.y.divide(X1).add(X1);
                    ECFieldElement X3 = L1.square().add(L1).add(curve.getA());
                    ECFieldElement Y3 = X1.squarePlusProduct(X3, L1.addOne());
                    return new F2m(curve, X3, Y3, this.withCompression);
                case 1:
                    ECFieldElement Y1 = this.y;
                    ECFieldElement Z1 = this.zs[0];
                    boolean Z1IsOne = Z1.isOne();
                    ECFieldElement X1Z1 = Z1IsOne ? X1 : X1.multiply(Z1);
                    ECFieldElement Y1Z1 = Z1IsOne ? Y1 : Y1.multiply(Z1);
                    ECFieldElement X1Sq = X1.square();
                    ECFieldElement S = X1Sq.add(Y1Z1);
                    ECFieldElement vSquared = X1Z1.square();
                    ECFieldElement sv = S.add(X1Z1);
                    ECFieldElement h = sv.multiplyPlusProduct(S, vSquared, curve.getA());
                    ECFieldElement X32 = X1Z1.multiply(h);
                    ECFieldElement Y32 = X1Sq.square().multiplyPlusProduct(X1Z1, h, sv);
                    return new F2m(curve, X32, Y32, new ECFieldElement[]{X1Z1.multiply(vSquared)}, this.withCompression);
                case 6:
                    ECFieldElement L12 = this.y;
                    ECFieldElement Z12 = this.zs[0];
                    boolean Z1IsOne2 = Z12.isOne();
                    ECFieldElement L1Z1 = Z1IsOne2 ? L12 : L12.multiply(Z12);
                    ECFieldElement Z1Sq = Z1IsOne2 ? Z12 : Z12.square();
                    ECFieldElement a = curve.getA();
                    ECFieldElement aZ1Sq = Z1IsOne2 ? a : a.multiply(Z1Sq);
                    ECFieldElement T = L12.square().add(L1Z1).add(aZ1Sq);
                    if (T.isZero()) {
                        return new F2m(curve, T, curve.getB().sqrt(), this.withCompression);
                    }
                    ECFieldElement X33 = T.square();
                    ECFieldElement Z3 = Z1IsOne2 ? T : T.multiply(Z1Sq);
                    ECFieldElement b = curve.getB();
                    if (b.bitLength() < (curve.getFieldSize() >> 1)) {
                        ECFieldElement t1 = L12.add(X1).square();
                        if (b.isOne()) {
                            t2 = aZ1Sq.add(Z1Sq).square();
                        } else {
                            t2 = aZ1Sq.squarePlusProduct(b, Z1Sq.square());
                        }
                        L3 = t1.add(T).add(Z1Sq).multiply(t1).add(t2).add(X33);
                        if (a.isZero()) {
                            L3 = L3.add(Z3);
                        } else if (!a.isOne()) {
                            L3 = L3.add(a.addOne().multiply(Z3));
                        }
                    } else {
                        L3 = (Z1IsOne2 ? X1 : X1.multiply(Z12)).squarePlusProduct(T, L1Z1).add(X33).add(Z3);
                    }
                    return new F2m(curve, X33, L3, new ECFieldElement[]{Z3}, this.withCompression);
                default:
                    throw new IllegalStateException("unsupported coordinate system");
            }
        }

        @Override
        public ECPoint twicePlus(ECPoint b) {
            if (isInfinity()) {
                return b;
            }
            if (b.isInfinity()) {
                return twice();
            }
            ECCurve curve = getCurve();
            ECFieldElement X1 = this.x;
            if (X1.isZero()) {
                return b;
            }
            int coord = curve.getCoordinateSystem();
            switch (coord) {
                case 6:
                    ECFieldElement X2 = b.x;
                    ECFieldElement Z2 = b.zs[0];
                    if (X2.isZero() || !Z2.isOne()) {
                        return twice().add(b);
                    }
                    ECFieldElement L1 = this.y;
                    ECFieldElement Z1 = this.zs[0];
                    ECFieldElement L2 = b.y;
                    ECFieldElement X1Sq = X1.square();
                    ECFieldElement L1Sq = L1.square();
                    ECFieldElement Z1Sq = Z1.square();
                    ECFieldElement L1Z1 = L1.multiply(Z1);
                    ECFieldElement T = curve.getA().multiply(Z1Sq).add(L1Sq).add(L1Z1);
                    ECFieldElement L2plus1 = L2.addOne();
                    ECFieldElement A = curve.getA().add(L2plus1).multiply(Z1Sq).add(L1Sq).multiplyPlusProduct(T, X1Sq, Z1Sq);
                    ECFieldElement X2Z1Sq = X2.multiply(Z1Sq);
                    ECFieldElement B = X2Z1Sq.add(T).square();
                    if (B.isZero()) {
                        if (A.isZero()) {
                            return b.twice();
                        }
                        return curve.getInfinity();
                    }
                    if (A.isZero()) {
                        return new F2m(curve, A, curve.getB().sqrt(), this.withCompression);
                    }
                    ECFieldElement X3 = A.square().multiply(X2Z1Sq);
                    ECFieldElement Z3 = A.multiply(B).multiply(Z1Sq);
                    ECFieldElement L3 = A.add(B).square().multiplyPlusProduct(T, L2plus1, Z3);
                    return new F2m(curve, X3, L3, new ECFieldElement[]{Z3}, this.withCompression);
                default:
                    return twice().add(b);
            }
        }

        @Override
        public ECPoint negate() {
            if (isInfinity()) {
                return this;
            }
            ECFieldElement X = this.x;
            if (X.isZero()) {
                return this;
            }
            switch (getCurveCoordinateSystem()) {
                case 0:
                    ECFieldElement Y = this.y;
                    return new F2m(this.curve, X, Y.add(X), this.withCompression);
                case 1:
                    ECFieldElement Y2 = this.y;
                    return new F2m(this.curve, X, Y2.add(X), new ECFieldElement[]{this.zs[0]}, this.withCompression);
                case 2:
                case 3:
                case 4:
                default:
                    throw new IllegalStateException("unsupported coordinate system");
                case 5:
                    ECFieldElement L = this.y;
                    return new F2m(this.curve, X, L.addOne(), this.withCompression);
                case 6:
                    ECFieldElement L2 = this.y;
                    ECFieldElement Z = this.zs[0];
                    return new F2m(this.curve, X, L2.add(Z), new ECFieldElement[]{Z}, this.withCompression);
            }
        }
    }
}
