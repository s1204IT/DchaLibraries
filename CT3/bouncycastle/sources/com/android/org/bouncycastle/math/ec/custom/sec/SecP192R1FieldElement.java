package com.android.org.bouncycastle.math.ec.custom.sec;

import com.android.org.bouncycastle.math.ec.ECFieldElement;
import com.android.org.bouncycastle.math.raw.Mod;
import com.android.org.bouncycastle.math.raw.Nat192;
import com.android.org.bouncycastle.util.Arrays;
import java.math.BigInteger;

public class SecP192R1FieldElement extends ECFieldElement {
    public static final BigInteger Q = SecP192R1Curve.q;
    protected int[] x;

    public SecP192R1FieldElement(BigInteger x) {
        if (x == null || x.signum() < 0 || x.compareTo(Q) >= 0) {
            throw new IllegalArgumentException("x value invalid for SecP192R1FieldElement");
        }
        this.x = SecP192R1Field.fromBigInteger(x);
    }

    public SecP192R1FieldElement() {
        this.x = Nat192.create();
    }

    protected SecP192R1FieldElement(int[] x) {
        this.x = x;
    }

    @Override
    public boolean isZero() {
        return Nat192.isZero(this.x);
    }

    @Override
    public boolean isOne() {
        return Nat192.isOne(this.x);
    }

    @Override
    public boolean testBitZero() {
        return Nat192.getBit(this.x, 0) == 1;
    }

    @Override
    public BigInteger toBigInteger() {
        return Nat192.toBigInteger(this.x);
    }

    @Override
    public String getFieldName() {
        return "SecP192R1Field";
    }

    @Override
    public int getFieldSize() {
        return Q.bitLength();
    }

    @Override
    public ECFieldElement add(ECFieldElement b) {
        int[] z = Nat192.create();
        SecP192R1Field.add(this.x, ((SecP192R1FieldElement) b).x, z);
        return new SecP192R1FieldElement(z);
    }

    @Override
    public ECFieldElement addOne() {
        int[] z = Nat192.create();
        SecP192R1Field.addOne(this.x, z);
        return new SecP192R1FieldElement(z);
    }

    @Override
    public ECFieldElement subtract(ECFieldElement b) {
        int[] z = Nat192.create();
        SecP192R1Field.subtract(this.x, ((SecP192R1FieldElement) b).x, z);
        return new SecP192R1FieldElement(z);
    }

    @Override
    public ECFieldElement multiply(ECFieldElement b) {
        int[] z = Nat192.create();
        SecP192R1Field.multiply(this.x, ((SecP192R1FieldElement) b).x, z);
        return new SecP192R1FieldElement(z);
    }

    @Override
    public ECFieldElement divide(ECFieldElement b) {
        int[] z = Nat192.create();
        Mod.invert(SecP192R1Field.P, ((SecP192R1FieldElement) b).x, z);
        SecP192R1Field.multiply(z, this.x, z);
        return new SecP192R1FieldElement(z);
    }

    @Override
    public ECFieldElement negate() {
        int[] z = Nat192.create();
        SecP192R1Field.negate(this.x, z);
        return new SecP192R1FieldElement(z);
    }

    @Override
    public ECFieldElement square() {
        int[] z = Nat192.create();
        SecP192R1Field.square(this.x, z);
        return new SecP192R1FieldElement(z);
    }

    @Override
    public ECFieldElement invert() {
        int[] z = Nat192.create();
        Mod.invert(SecP192R1Field.P, this.x, z);
        return new SecP192R1FieldElement(z);
    }

    @Override
    public ECFieldElement sqrt() {
        int[] x1 = this.x;
        if (Nat192.isZero(x1) || Nat192.isOne(x1)) {
            return this;
        }
        int[] t1 = Nat192.create();
        int[] t2 = Nat192.create();
        SecP192R1Field.square(x1, t1);
        SecP192R1Field.multiply(t1, x1, t1);
        SecP192R1Field.squareN(t1, 2, t2);
        SecP192R1Field.multiply(t2, t1, t2);
        SecP192R1Field.squareN(t2, 4, t1);
        SecP192R1Field.multiply(t1, t2, t1);
        SecP192R1Field.squareN(t1, 8, t2);
        SecP192R1Field.multiply(t2, t1, t2);
        SecP192R1Field.squareN(t2, 16, t1);
        SecP192R1Field.multiply(t1, t2, t1);
        SecP192R1Field.squareN(t1, 32, t2);
        SecP192R1Field.multiply(t2, t1, t2);
        SecP192R1Field.squareN(t2, 64, t1);
        SecP192R1Field.multiply(t1, t2, t1);
        SecP192R1Field.squareN(t1, 62, t1);
        SecP192R1Field.square(t1, t2);
        if (Nat192.eq(x1, t2)) {
            return new SecP192R1FieldElement(t1);
        }
        return null;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof SecP192R1FieldElement)) {
            return false;
        }
        return Nat192.eq(this.x, obj.x);
    }

    public int hashCode() {
        return Q.hashCode() ^ Arrays.hashCode(this.x, 0, 6);
    }
}
