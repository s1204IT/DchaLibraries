package com.android.org.bouncycastle.math.ec;

import com.android.org.bouncycastle.util.Arrays;
import com.android.org.bouncycastle.util.BigIntegers;
import java.math.BigInteger;
import java.util.Random;

public abstract class ECFieldElement implements ECConstants {
    public abstract ECFieldElement add(ECFieldElement eCFieldElement);

    public abstract ECFieldElement addOne();

    public abstract ECFieldElement divide(ECFieldElement eCFieldElement);

    public abstract String getFieldName();

    public abstract int getFieldSize();

    public abstract ECFieldElement invert();

    public abstract ECFieldElement multiply(ECFieldElement eCFieldElement);

    public abstract ECFieldElement negate();

    public abstract ECFieldElement sqrt();

    public abstract ECFieldElement square();

    public abstract ECFieldElement subtract(ECFieldElement eCFieldElement);

    public abstract BigInteger toBigInteger();

    public int bitLength() {
        return toBigInteger().bitLength();
    }

    public boolean isZero() {
        return toBigInteger().signum() == 0;
    }

    public boolean testBitZero() {
        return toBigInteger().testBit(0);
    }

    public String toString() {
        return toBigInteger().toString(16);
    }

    public byte[] getEncoded() {
        return BigIntegers.asUnsignedByteArray((getFieldSize() + 7) / 8, toBigInteger());
    }

    public static class Fp extends ECFieldElement {
        BigInteger q;
        BigInteger r;
        BigInteger x;

        static BigInteger calculateResidue(BigInteger p) {
            int bitLength = p.bitLength();
            if (bitLength > 128) {
                BigInteger firstWord = p.shiftRight(bitLength - 64);
                if (firstWord.longValue() == -1) {
                    return ONE.shiftLeft(bitLength).subtract(p);
                }
            }
            return null;
        }

        public Fp(BigInteger q, BigInteger x) {
            this(q, calculateResidue(q), x);
        }

        Fp(BigInteger q, BigInteger r, BigInteger x) {
            if (x == null || x.signum() < 0 || x.compareTo(q) >= 0) {
                throw new IllegalArgumentException("x value invalid in Fp field element");
            }
            this.q = q;
            this.r = r;
            this.x = x;
        }

        @Override
        public BigInteger toBigInteger() {
            return this.x;
        }

        @Override
        public String getFieldName() {
            return "Fp";
        }

        @Override
        public int getFieldSize() {
            return this.q.bitLength();
        }

        public BigInteger getQ() {
            return this.q;
        }

        @Override
        public ECFieldElement add(ECFieldElement b) {
            return new Fp(this.q, this.r, modAdd(this.x, b.toBigInteger()));
        }

        @Override
        public ECFieldElement addOne() {
            BigInteger x2 = this.x.add(ECConstants.ONE);
            if (x2.compareTo(this.q) == 0) {
                x2 = ECConstants.ZERO;
            }
            return new Fp(this.q, this.r, x2);
        }

        @Override
        public ECFieldElement subtract(ECFieldElement b) {
            BigInteger x2 = b.toBigInteger();
            BigInteger x3 = this.x.subtract(x2);
            if (x3.signum() < 0) {
                x3 = x3.add(this.q);
            }
            return new Fp(this.q, this.r, x3);
        }

        @Override
        public ECFieldElement multiply(ECFieldElement b) {
            return new Fp(this.q, this.r, modMult(this.x, b.toBigInteger()));
        }

        @Override
        public ECFieldElement divide(ECFieldElement b) {
            return new Fp(this.q, modMult(this.x, b.toBigInteger().modInverse(this.q)));
        }

        @Override
        public ECFieldElement negate() {
            BigInteger x2;
            if (this.x.signum() == 0) {
                x2 = this.x;
            } else if (ONE.equals(this.r)) {
                x2 = this.q.xor(this.x);
            } else {
                x2 = this.q.subtract(this.x);
            }
            return new Fp(this.q, this.r, x2);
        }

        @Override
        public ECFieldElement square() {
            return new Fp(this.q, this.r, modMult(this.x, this.x));
        }

        @Override
        public ECFieldElement invert() {
            return new Fp(this.q, this.r, this.x.modInverse(this.q));
        }

        @Override
        public ECFieldElement sqrt() {
            if (!this.q.testBit(0)) {
                throw new RuntimeException("not done yet");
            }
            if (this.q.testBit(1)) {
                ECFieldElement z = new Fp(this.q, this.r, this.x.modPow(this.q.shiftRight(2).add(ECConstants.ONE), this.q));
                if (!z.square().equals(this)) {
                    return null;
                }
                return z;
            }
            BigInteger qMinusOne = this.q.subtract(ECConstants.ONE);
            BigInteger legendreExponent = qMinusOne.shiftRight(1);
            if (!this.x.modPow(legendreExponent, this.q).equals(ECConstants.ONE)) {
                return null;
            }
            BigInteger u = qMinusOne.shiftRight(2);
            BigInteger k = u.shiftLeft(1).add(ECConstants.ONE);
            BigInteger Q = this.x;
            BigInteger fourQ = modDouble(modDouble(Q));
            Random rand = new Random();
            while (true) {
                BigInteger P = new BigInteger(this.q.bitLength(), rand);
                if (P.compareTo(this.q) < 0 && P.multiply(P).subtract(fourQ).modPow(legendreExponent, this.q).equals(qMinusOne)) {
                    BigInteger[] result = lucasSequence(P, Q, k);
                    BigInteger U = result[0];
                    BigInteger V = result[1];
                    if (modMult(V, V).equals(fourQ)) {
                        if (V.testBit(0)) {
                            V = V.add(this.q);
                        }
                        return new Fp(this.q, this.r, V.shiftRight(1));
                    }
                    if (!U.equals(ECConstants.ONE) && !U.equals(qMinusOne)) {
                        return null;
                    }
                }
            }
        }

        private BigInteger[] lucasSequence(BigInteger P, BigInteger Q, BigInteger k) {
            int n = k.bitLength();
            int s = k.getLowestSetBit();
            BigInteger Uh = ECConstants.ONE;
            BigInteger Vl = ECConstants.TWO;
            BigInteger Vh = P;
            BigInteger Ql = ECConstants.ONE;
            BigInteger Qh = ECConstants.ONE;
            for (int j = n - 1; j >= s + 1; j--) {
                Ql = modMult(Ql, Qh);
                if (k.testBit(j)) {
                    Qh = modMult(Ql, Q);
                    Uh = modMult(Uh, Vh);
                    Vl = modReduce(Vh.multiply(Vl).subtract(P.multiply(Ql)));
                    Vh = modReduce(Vh.multiply(Vh).subtract(Qh.shiftLeft(1)));
                } else {
                    Qh = Ql;
                    Uh = modReduce(Uh.multiply(Vl).subtract(Ql));
                    Vh = modReduce(Vh.multiply(Vl).subtract(P.multiply(Ql)));
                    Vl = modReduce(Vl.multiply(Vl).subtract(Ql.shiftLeft(1)));
                }
            }
            BigInteger Ql2 = modMult(Ql, Qh);
            BigInteger Qh2 = modMult(Ql2, Q);
            BigInteger Uh2 = modReduce(Uh.multiply(Vl).subtract(Ql2));
            BigInteger Vl2 = modReduce(Vh.multiply(Vl).subtract(P.multiply(Ql2)));
            BigInteger Ql3 = modMult(Ql2, Qh2);
            for (int j2 = 1; j2 <= s; j2++) {
                Uh2 = modMult(Uh2, Vl2);
                Vl2 = modReduce(Vl2.multiply(Vl2).subtract(Ql3.shiftLeft(1)));
                Ql3 = modMult(Ql3, Ql3);
            }
            return new BigInteger[]{Uh2, Vl2};
        }

        protected BigInteger modAdd(BigInteger x1, BigInteger x2) {
            BigInteger x3 = x1.add(x2);
            if (x3.compareTo(this.q) >= 0) {
                return x3.subtract(this.q);
            }
            return x3;
        }

        protected BigInteger modDouble(BigInteger x) {
            BigInteger _2x = x.shiftLeft(1);
            if (_2x.compareTo(this.q) >= 0) {
                return _2x.subtract(this.q);
            }
            return _2x;
        }

        protected BigInteger modMult(BigInteger x1, BigInteger x2) {
            return modReduce(x1.multiply(x2));
        }

        protected BigInteger modReduce(BigInteger x) {
            if (this.r != null) {
                int qLen = this.q.bitLength();
                while (x.bitLength() > qLen + 1) {
                    BigInteger u = x.shiftRight(qLen);
                    BigInteger v = x.subtract(u.shiftLeft(qLen));
                    if (!this.r.equals(ONE)) {
                        u = u.multiply(this.r);
                    }
                    x = u.add(v);
                }
                while (x.compareTo(this.q) >= 0) {
                    x = x.subtract(this.q);
                }
                return x;
            }
            return x.mod(this.q);
        }

        public boolean equals(Object other) {
            if (other == this) {
                return true;
            }
            if (!(other instanceof Fp)) {
                return false;
            }
            Fp o = (Fp) other;
            return this.q.equals(o.q) && this.x.equals(o.x);
        }

        public int hashCode() {
            return this.q.hashCode() ^ this.x.hashCode();
        }
    }

    public static class F2m extends ECFieldElement {
        public static final int GNB = 1;
        public static final int PPB = 3;
        public static final int TPB = 2;
        private int[] ks;
        private int m;
        private int representation;
        private LongArray x;

        public F2m(int m, int k1, int k2, int k3, BigInteger x) {
            if (k2 == 0 && k3 == 0) {
                this.representation = 2;
                this.ks = new int[]{k1};
            } else {
                if (k2 >= k3) {
                    throw new IllegalArgumentException("k2 must be smaller than k3");
                }
                if (k2 <= 0) {
                    throw new IllegalArgumentException("k2 must be larger than 0");
                }
                this.representation = 3;
                this.ks = new int[]{k1, k2, k3};
            }
            this.m = m;
            this.x = new LongArray(x);
        }

        public F2m(int m, int k, BigInteger x) {
            this(m, k, 0, 0, x);
        }

        private F2m(int m, int[] ks, LongArray x) {
            this.m = m;
            this.representation = ks.length == 1 ? 2 : 3;
            this.ks = ks;
            this.x = x;
        }

        @Override
        public int bitLength() {
            return this.x.degree();
        }

        @Override
        public boolean isZero() {
            return this.x.isZero();
        }

        @Override
        public boolean testBitZero() {
            return this.x.testBitZero();
        }

        @Override
        public BigInteger toBigInteger() {
            return this.x.toBigInteger();
        }

        @Override
        public String getFieldName() {
            return "F2m";
        }

        @Override
        public int getFieldSize() {
            return this.m;
        }

        public static void checkFieldElements(ECFieldElement a, ECFieldElement b) {
            if (!(a instanceof F2m) || !(b instanceof F2m)) {
                throw new IllegalArgumentException("Field elements are not both instances of ECFieldElement.F2m");
            }
            F2m aF2m = (F2m) a;
            F2m bF2m = (F2m) b;
            if (aF2m.representation != bF2m.representation) {
                throw new IllegalArgumentException("One of the F2m field elements has incorrect representation");
            }
            if (aF2m.m != bF2m.m || !Arrays.areEqual(aF2m.ks, bF2m.ks)) {
                throw new IllegalArgumentException("Field elements are not elements of the same field F2m");
            }
        }

        @Override
        public ECFieldElement add(ECFieldElement b) {
            LongArray iarrClone = (LongArray) this.x.clone();
            F2m bF2m = (F2m) b;
            iarrClone.addShiftedByWords(bF2m.x, 0);
            return new F2m(this.m, this.ks, iarrClone);
        }

        @Override
        public ECFieldElement addOne() {
            return new F2m(this.m, this.ks, this.x.addOne());
        }

        @Override
        public ECFieldElement subtract(ECFieldElement b) {
            return add(b);
        }

        @Override
        public ECFieldElement multiply(ECFieldElement b) {
            return new F2m(this.m, this.ks, this.x.modMultiply(((F2m) b).x, this.m, this.ks));
        }

        @Override
        public ECFieldElement divide(ECFieldElement b) {
            ECFieldElement bInv = b.invert();
            return multiply(bInv);
        }

        @Override
        public ECFieldElement negate() {
            return this;
        }

        @Override
        public ECFieldElement square() {
            return new F2m(this.m, this.ks, this.x.modSquare(this.m, this.ks));
        }

        @Override
        public ECFieldElement invert() {
            return new F2m(this.m, this.ks, this.x.modInverse(this.m, this.ks));
        }

        @Override
        public ECFieldElement sqrt() {
            throw new RuntimeException("Not implemented");
        }

        public int getRepresentation() {
            return this.representation;
        }

        public int getM() {
            return this.m;
        }

        public int getK1() {
            return this.ks[0];
        }

        public int getK2() {
            if (this.ks.length >= 2) {
                return this.ks[1];
            }
            return 0;
        }

        public int getK3() {
            if (this.ks.length >= 3) {
                return this.ks[2];
            }
            return 0;
        }

        public boolean equals(Object anObject) {
            if (anObject == this) {
                return true;
            }
            if (!(anObject instanceof F2m)) {
                return false;
            }
            F2m b = (F2m) anObject;
            return this.m == b.m && this.representation == b.representation && Arrays.areEqual(this.ks, b.ks) && this.x.equals(b.x);
        }

        public int hashCode() {
            return (this.x.hashCode() ^ this.m) ^ Arrays.hashCode(this.ks);
        }
    }
}
