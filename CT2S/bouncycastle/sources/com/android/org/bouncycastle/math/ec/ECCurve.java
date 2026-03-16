package com.android.org.bouncycastle.math.ec;

import com.android.org.bouncycastle.math.ec.ECFieldElement;
import com.android.org.bouncycastle.math.ec.ECPoint;
import com.android.org.bouncycastle.util.BigIntegers;
import java.math.BigInteger;
import java.util.Random;

public abstract class ECCurve {
    public static final int COORD_AFFINE = 0;
    public static final int COORD_HOMOGENEOUS = 1;
    public static final int COORD_JACOBIAN = 2;
    public static final int COORD_JACOBIAN_CHUDNOVSKY = 3;
    public static final int COORD_JACOBIAN_MODIFIED = 4;
    public static final int COORD_LAMBDA_AFFINE = 5;
    public static final int COORD_LAMBDA_PROJECTIVE = 6;
    public static final int COORD_SKEWED = 7;
    protected ECFieldElement a;
    protected ECFieldElement b;
    protected int coord = 0;
    protected ECMultiplier multiplier = null;

    protected abstract ECCurve cloneCurve();

    protected abstract ECPoint createRawPoint(ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, boolean z);

    protected abstract ECPoint decompressPoint(int i, BigInteger bigInteger);

    public abstract ECFieldElement fromBigInteger(BigInteger bigInteger);

    public abstract int getFieldSize();

    public abstract ECPoint getInfinity();

    public static int[] getAllCoordinateSystems() {
        return new int[]{0, 1, 2, 3, 4, 5, 6, 7};
    }

    public class Config {
        protected int coord;
        protected ECMultiplier multiplier;

        Config(int coord, ECMultiplier multiplier) {
            this.coord = coord;
            this.multiplier = multiplier;
        }

        public Config setCoordinateSystem(int coord) {
            this.coord = coord;
            return this;
        }

        public Config setMultiplier(ECMultiplier multiplier) {
            this.multiplier = multiplier;
            return this;
        }

        public ECCurve create() {
            if (!ECCurve.this.supportsCoordinateSystem(this.coord)) {
                throw new IllegalStateException("unsupported coordinate system");
            }
            ECCurve c = ECCurve.this.cloneCurve();
            if (c == ECCurve.this) {
                throw new IllegalStateException("implementation returned current curve");
            }
            c.coord = this.coord;
            c.multiplier = this.multiplier;
            return c;
        }
    }

    public Config configure() {
        return new Config(this.coord, this.multiplier);
    }

    public ECPoint createPoint(BigInteger x, BigInteger y) {
        return createPoint(x, y, false);
    }

    public ECPoint createPoint(BigInteger x, BigInteger y, boolean withCompression) {
        return createRawPoint(fromBigInteger(x), fromBigInteger(y), withCompression);
    }

    protected ECMultiplier createDefaultMultiplier() {
        return new WNafL2RMultiplier();
    }

    public boolean supportsCoordinateSystem(int coord) {
        return coord == 0;
    }

    public PreCompInfo getPreCompInfo(ECPoint p) {
        checkPoint(p);
        return p.preCompInfo;
    }

    public void setPreCompInfo(ECPoint point, PreCompInfo preCompInfo) {
        checkPoint(point);
        point.preCompInfo = preCompInfo;
    }

    public ECPoint importPoint(ECPoint p) {
        if (this == p.getCurve()) {
            return p;
        }
        if (p.isInfinity()) {
            return getInfinity();
        }
        ECPoint p2 = p.normalize();
        return createPoint(p2.getXCoord().toBigInteger(), p2.getYCoord().toBigInteger(), p2.withCompression);
    }

    public void normalizeAll(ECPoint[] points) {
        checkPoints(points);
        if (getCoordinateSystem() != 0) {
            ECFieldElement[] zs = new ECFieldElement[points.length];
            int[] indices = new int[points.length];
            int count = 0;
            for (int i = 0; i < points.length; i++) {
                ECPoint p = points[i];
                if (p != null && !p.isNormalized()) {
                    zs[count] = p.getZCoord(0);
                    indices[count] = i;
                    count++;
                }
            }
            if (count != 0) {
                ECAlgorithms.implMontgomeryTrick(zs, 0, count);
                for (int j = 0; j < count; j++) {
                    int index = indices[j];
                    points[index] = points[index].normalize(zs[j]);
                }
            }
        }
    }

    public ECFieldElement getA() {
        return this.a;
    }

    public ECFieldElement getB() {
        return this.b;
    }

    public int getCoordinateSystem() {
        return this.coord;
    }

    public ECMultiplier getMultiplier() {
        if (this.multiplier == null) {
            this.multiplier = createDefaultMultiplier();
        }
        return this.multiplier;
    }

    public ECPoint decodePoint(byte[] encoded) {
        int expectedLength = (getFieldSize() + 7) / 8;
        switch (encoded[0]) {
            case 0:
                if (encoded.length != 1) {
                    throw new IllegalArgumentException("Incorrect length for infinity encoding");
                }
                ECPoint p = getInfinity();
                return p;
            case 1:
            case 5:
            default:
                throw new IllegalArgumentException("Invalid point encoding 0x" + Integer.toString(encoded[0], 16));
            case 2:
            case 3:
                if (encoded.length != expectedLength + 1) {
                    throw new IllegalArgumentException("Incorrect length for compressed encoding");
                }
                int yTilde = encoded[0] & 1;
                BigInteger X = BigIntegers.fromUnsignedByteArray(encoded, 1, expectedLength);
                ECPoint p2 = decompressPoint(yTilde, X);
                return p2;
            case 4:
            case 6:
            case 7:
                if (encoded.length != (expectedLength * 2) + 1) {
                    throw new IllegalArgumentException("Incorrect length for uncompressed/hybrid encoding");
                }
                BigInteger X2 = BigIntegers.fromUnsignedByteArray(encoded, 1, expectedLength);
                BigInteger Y = BigIntegers.fromUnsignedByteArray(encoded, expectedLength + 1, expectedLength);
                ECPoint p3 = createPoint(X2, Y);
                return p3;
        }
    }

    protected void checkPoint(ECPoint point) {
        if (point == null || this != point.getCurve()) {
            throw new IllegalArgumentException("'point' must be non-null and on this curve");
        }
    }

    protected void checkPoints(ECPoint[] points) {
        if (points == null) {
            throw new IllegalArgumentException("'points' cannot be null");
        }
        for (ECPoint point : points) {
            if (point != null && this != point.getCurve()) {
                throw new IllegalArgumentException("'points' entries must be null or on this curve");
            }
        }
    }

    public static class Fp extends ECCurve {
        private static final int FP_DEFAULT_COORDS = 4;
        ECPoint.Fp infinity = new ECPoint.Fp(this, null, null);
        BigInteger q;
        BigInteger r;

        public Fp(BigInteger q, BigInteger a, BigInteger b) {
            this.q = q;
            this.r = ECFieldElement.Fp.calculateResidue(q);
            this.a = fromBigInteger(a);
            this.b = fromBigInteger(b);
            this.coord = 4;
        }

        protected Fp(BigInteger q, BigInteger r, ECFieldElement a, ECFieldElement b) {
            this.q = q;
            this.r = r;
            this.a = a;
            this.b = b;
            this.coord = 4;
        }

        @Override
        protected ECCurve cloneCurve() {
            return new Fp(this.q, this.r, this.a, this.b);
        }

        @Override
        public boolean supportsCoordinateSystem(int coord) {
            switch (coord) {
                case 0:
                case 1:
                case 2:
                case 4:
                    return true;
                case 3:
                default:
                    return false;
            }
        }

        public BigInteger getQ() {
            return this.q;
        }

        @Override
        public int getFieldSize() {
            return this.q.bitLength();
        }

        @Override
        public ECFieldElement fromBigInteger(BigInteger x) {
            return new ECFieldElement.Fp(this.q, this.r, x);
        }

        @Override
        protected ECPoint createRawPoint(ECFieldElement x, ECFieldElement y, boolean withCompression) {
            return new ECPoint.Fp(this, x, y, withCompression);
        }

        @Override
        public ECPoint importPoint(ECPoint p) {
            if (this != p.getCurve() && getCoordinateSystem() == 2 && !p.isInfinity()) {
                switch (p.getCurve().getCoordinateSystem()) {
                    case 2:
                    case 3:
                    case 4:
                        return new ECPoint.Fp(this, fromBigInteger(p.x.toBigInteger()), fromBigInteger(p.y.toBigInteger()), new ECFieldElement[]{fromBigInteger(p.zs[0].toBigInteger())}, p.withCompression);
                }
            }
            return super.importPoint(p);
        }

        @Override
        protected ECPoint decompressPoint(int yTilde, BigInteger X1) {
            ECFieldElement x = fromBigInteger(X1);
            ECFieldElement alpha = x.multiply(x.square().add(this.a)).add(this.b);
            ECFieldElement beta = alpha.sqrt();
            if (beta == null) {
                throw new RuntimeException("Invalid point compression");
            }
            BigInteger betaValue = beta.toBigInteger();
            if (betaValue.testBit(0) != (yTilde == 1)) {
                beta = fromBigInteger(this.q.subtract(betaValue));
            }
            return new ECPoint.Fp(this, x, beta, true);
        }

        @Override
        public ECPoint getInfinity() {
            return this.infinity;
        }

        public boolean equals(Object anObject) {
            if (anObject == this) {
                return true;
            }
            if (!(anObject instanceof Fp)) {
                return false;
            }
            Fp other = (Fp) anObject;
            return this.q.equals(other.q) && this.a.equals(other.a) && this.b.equals(other.b);
        }

        public int hashCode() {
            return (this.a.hashCode() ^ this.b.hashCode()) ^ this.q.hashCode();
        }
    }

    public static class F2m extends ECCurve {
        private static final int F2M_DEFAULT_COORDS = 0;
        private BigInteger h;
        private ECPoint.F2m infinity;
        private int k1;
        private int k2;
        private int k3;
        private int m;
        private byte mu;
        private BigInteger n;
        private BigInteger[] si;

        public F2m(int m, int k, BigInteger a, BigInteger b) {
            this(m, k, 0, 0, a, b, (BigInteger) null, (BigInteger) null);
        }

        public F2m(int m, int k, BigInteger a, BigInteger b, BigInteger n, BigInteger h) {
            this(m, k, 0, 0, a, b, n, h);
        }

        public F2m(int m, int k1, int k2, int k3, BigInteger a, BigInteger b) {
            this(m, k1, k2, k3, a, b, (BigInteger) null, (BigInteger) null);
        }

        public F2m(int m, int k1, int k2, int k3, BigInteger a, BigInteger b, BigInteger n, BigInteger h) {
            this.mu = (byte) 0;
            this.si = null;
            this.m = m;
            this.k1 = k1;
            this.k2 = k2;
            this.k3 = k3;
            this.n = n;
            this.h = h;
            if (k1 == 0) {
                throw new IllegalArgumentException("k1 must be > 0");
            }
            if (k2 == 0) {
                if (k3 != 0) {
                    throw new IllegalArgumentException("k3 must be 0 if k2 == 0");
                }
            } else {
                if (k2 <= k1) {
                    throw new IllegalArgumentException("k2 must be > k1");
                }
                if (k3 <= k2) {
                    throw new IllegalArgumentException("k3 must be > k2");
                }
            }
            this.infinity = new ECPoint.F2m(this, null, null);
            this.a = fromBigInteger(a);
            this.b = fromBigInteger(b);
            this.coord = 0;
        }

        protected F2m(int m, int k1, int k2, int k3, ECFieldElement a, ECFieldElement b, BigInteger n, BigInteger h) {
            this.mu = (byte) 0;
            this.si = null;
            this.m = m;
            this.k1 = k1;
            this.k2 = k2;
            this.k3 = k3;
            this.n = n;
            this.h = h;
            this.infinity = new ECPoint.F2m(this, null, null);
            this.a = a;
            this.b = b;
            this.coord = 0;
        }

        @Override
        protected ECCurve cloneCurve() {
            return new F2m(this.m, this.k1, this.k2, this.k3, this.a, this.b, this.n, this.h);
        }

        @Override
        public boolean supportsCoordinateSystem(int coord) {
            switch (coord) {
                case 0:
                case 1:
                case 6:
                    return true;
                default:
                    return false;
            }
        }

        @Override
        protected ECMultiplier createDefaultMultiplier() {
            return isKoblitz() ? new WTauNafMultiplier() : super.createDefaultMultiplier();
        }

        @Override
        public int getFieldSize() {
            return this.m;
        }

        @Override
        public ECFieldElement fromBigInteger(BigInteger x) {
            return new ECFieldElement.F2m(this.m, this.k1, this.k2, this.k3, x);
        }

        @Override
        public ECPoint createPoint(BigInteger x, BigInteger y, boolean withCompression) {
            ECFieldElement X = fromBigInteger(x);
            ECFieldElement Y = fromBigInteger(y);
            switch (getCoordinateSystem()) {
                case 5:
                case 6:
                    if (!X.isZero()) {
                        Y = Y.divide(X).add(X);
                    }
                    break;
            }
            return createRawPoint(X, Y, withCompression);
        }

        @Override
        protected ECPoint createRawPoint(ECFieldElement x, ECFieldElement y, boolean withCompression) {
            return new ECPoint.F2m(this, x, y, withCompression);
        }

        @Override
        public ECPoint getInfinity() {
            return this.infinity;
        }

        public boolean isKoblitz() {
            return this.n != null && this.h != null && this.a.bitLength() <= 1 && this.b.bitLength() == 1;
        }

        synchronized byte getMu() {
            if (this.mu == 0) {
                this.mu = Tnaf.getMu(this);
            }
            return this.mu;
        }

        synchronized BigInteger[] getSi() {
            if (this.si == null) {
                this.si = Tnaf.getSi(this);
            }
            return this.si;
        }

        @Override
        protected ECPoint decompressPoint(int yTilde, BigInteger X1) {
            ECFieldElement yp;
            ECFieldElement xp = fromBigInteger(X1);
            if (xp.isZero()) {
                yp = (ECFieldElement.F2m) this.b;
                for (int i = 0; i < this.m - 1; i++) {
                    yp = yp.square();
                }
            } else {
                ECFieldElement beta = xp.add(this.a).add(this.b.multiply(xp.square().invert()));
                ECFieldElement z = solveQuadraticEquation(beta);
                if (z == null) {
                    throw new IllegalArgumentException("Invalid point compression");
                }
                if (z.testBitZero() != (yTilde == 1)) {
                    z = z.addOne();
                }
                yp = xp.multiply(z);
                switch (getCoordinateSystem()) {
                    case 5:
                    case 6:
                        yp = yp.divide(xp).add(xp);
                        break;
                }
            }
            return new ECPoint.F2m(this, xp, yp, true);
        }

        private ECFieldElement solveQuadraticEquation(ECFieldElement beta) {
            ECFieldElement z;
            ECFieldElement gamma;
            if (!beta.isZero()) {
                ECFieldElement zeroElement = fromBigInteger(ECConstants.ZERO);
                Random rand = new Random();
                do {
                    ECFieldElement t = fromBigInteger(new BigInteger(this.m, rand));
                    z = zeroElement;
                    ECFieldElement w = beta;
                    for (int i = 1; i <= this.m - 1; i++) {
                        ECFieldElement w2 = w.square();
                        z = z.square().add(w2.multiply(t));
                        w = w2.add(beta);
                    }
                    if (!w.isZero()) {
                        return null;
                    }
                    gamma = z.square().add(z);
                } while (gamma.isZero());
                return z;
            }
            return beta;
        }

        public boolean equals(Object anObject) {
            if (anObject == this) {
                return true;
            }
            if (!(anObject instanceof F2m)) {
                return false;
            }
            F2m other = (F2m) anObject;
            return this.m == other.m && this.k1 == other.k1 && this.k2 == other.k2 && this.k3 == other.k3 && this.a.equals(other.a) && this.b.equals(other.b);
        }

        public int hashCode() {
            return ((((this.a.hashCode() ^ this.b.hashCode()) ^ this.m) ^ this.k1) ^ this.k2) ^ this.k3;
        }

        public int getM() {
            return this.m;
        }

        public boolean isTrinomial() {
            return this.k2 == 0 && this.k3 == 0;
        }

        public int getK1() {
            return this.k1;
        }

        public int getK2() {
            return this.k2;
        }

        public int getK3() {
            return this.k3;
        }

        public BigInteger getN() {
            return this.n;
        }

        public BigInteger getH() {
            return this.h;
        }
    }
}
