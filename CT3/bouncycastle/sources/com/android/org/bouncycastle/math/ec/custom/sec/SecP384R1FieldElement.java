package com.android.org.bouncycastle.math.ec.custom.sec;

import com.android.org.bouncycastle.math.ec.ECFieldElement;
import com.android.org.bouncycastle.math.raw.Mod;
import com.android.org.bouncycastle.math.raw.Nat;
import com.android.org.bouncycastle.util.Arrays;
import java.math.BigInteger;

public class SecP384R1FieldElement extends ECFieldElement {
    public static final BigInteger Q = SecP384R1Curve.q;
    protected int[] x;

    public SecP384R1FieldElement(BigInteger x) {
        if (x == null || x.signum() < 0 || x.compareTo(Q) >= 0) {
            throw new IllegalArgumentException("x value invalid for SecP384R1FieldElement");
        }
        this.x = SecP384R1Field.fromBigInteger(x);
    }

    public SecP384R1FieldElement() {
        this.x = Nat.create(12);
    }

    protected SecP384R1FieldElement(int[] x) {
        this.x = x;
    }

    @Override
    public boolean isZero() {
        return Nat.isZero(12, this.x);
    }

    @Override
    public boolean isOne() {
        return Nat.isOne(12, this.x);
    }

    @Override
    public boolean testBitZero() {
        return Nat.getBit(this.x, 0) == 1;
    }

    @Override
    public BigInteger toBigInteger() {
        return Nat.toBigInteger(12, this.x);
    }

    @Override
    public String getFieldName() {
        return "SecP384R1Field";
    }

    @Override
    public int getFieldSize() {
        return Q.bitLength();
    }

    @Override
    public ECFieldElement add(ECFieldElement b) {
        int[] z = Nat.create(12);
        SecP384R1Field.add(this.x, ((SecP384R1FieldElement) b).x, z);
        return new SecP384R1FieldElement(z);
    }

    @Override
    public ECFieldElement addOne() {
        int[] z = Nat.create(12);
        SecP384R1Field.addOne(this.x, z);
        return new SecP384R1FieldElement(z);
    }

    @Override
    public ECFieldElement subtract(ECFieldElement b) {
        int[] z = Nat.create(12);
        SecP384R1Field.subtract(this.x, ((SecP384R1FieldElement) b).x, z);
        return new SecP384R1FieldElement(z);
    }

    @Override
    public ECFieldElement multiply(ECFieldElement b) {
        int[] z = Nat.create(12);
        SecP384R1Field.multiply(this.x, ((SecP384R1FieldElement) b).x, z);
        return new SecP384R1FieldElement(z);
    }

    @Override
    public ECFieldElement divide(ECFieldElement b) {
        int[] z = Nat.create(12);
        Mod.invert(SecP384R1Field.P, ((SecP384R1FieldElement) b).x, z);
        SecP384R1Field.multiply(z, this.x, z);
        return new SecP384R1FieldElement(z);
    }

    @Override
    public ECFieldElement negate() {
        int[] z = Nat.create(12);
        SecP384R1Field.negate(this.x, z);
        return new SecP384R1FieldElement(z);
    }

    @Override
    public ECFieldElement square() {
        int[] z = Nat.create(12);
        SecP384R1Field.square(this.x, z);
        return new SecP384R1FieldElement(z);
    }

    @Override
    public ECFieldElement invert() {
        int[] z = Nat.create(12);
        Mod.invert(SecP384R1Field.P, this.x, z);
        return new SecP384R1FieldElement(z);
    }

    @Override
    public ECFieldElement sqrt() {
        int[] x1 = this.x;
        if (Nat.isZero(12, x1) || Nat.isOne(12, x1)) {
            return this;
        }
        int[] t1 = Nat.create(12);
        int[] t2 = Nat.create(12);
        int[] t3 = Nat.create(12);
        int[] t4 = Nat.create(12);
        SecP384R1Field.square(x1, t1);
        SecP384R1Field.multiply(t1, x1, t1);
        SecP384R1Field.squareN(t1, 2, t2);
        SecP384R1Field.multiply(t2, t1, t2);
        SecP384R1Field.square(t2, t2);
        SecP384R1Field.multiply(t2, x1, t2);
        SecP384R1Field.squareN(t2, 5, t3);
        SecP384R1Field.multiply(t3, t2, t3);
        SecP384R1Field.squareN(t3, 5, t4);
        SecP384R1Field.multiply(t4, t2, t4);
        SecP384R1Field.squareN(t4, 15, t2);
        SecP384R1Field.multiply(t2, t4, t2);
        SecP384R1Field.squareN(t2, 2, t3);
        SecP384R1Field.multiply(t1, t3, t1);
        SecP384R1Field.squareN(t3, 28, t3);
        SecP384R1Field.multiply(t2, t3, t2);
        SecP384R1Field.squareN(t2, 60, t3);
        SecP384R1Field.multiply(t3, t2, t3);
        SecP384R1Field.squareN(t3, 120, t2);
        SecP384R1Field.multiply(t2, t3, t2);
        SecP384R1Field.squareN(t2, 15, t2);
        SecP384R1Field.multiply(t2, t4, t2);
        SecP384R1Field.squareN(t2, 33, t2);
        SecP384R1Field.multiply(t2, t1, t2);
        SecP384R1Field.squareN(t2, 64, t2);
        SecP384R1Field.multiply(t2, x1, t2);
        SecP384R1Field.squareN(t2, 30, t1);
        SecP384R1Field.square(t1, t2);
        if (Nat.eq(12, x1, t2)) {
            return new SecP384R1FieldElement(t1);
        }
        return null;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof SecP384R1FieldElement)) {
            return false;
        }
        return Nat.eq(12, this.x, obj.x);
    }

    public int hashCode() {
        return Q.hashCode() ^ Arrays.hashCode(this.x, 0, 12);
    }
}
