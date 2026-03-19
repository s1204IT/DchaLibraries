package com.android.org.bouncycastle.math.ec.custom.sec;

import com.android.org.bouncycastle.math.ec.ECCurve;
import com.android.org.bouncycastle.math.ec.ECFieldElement;
import com.android.org.bouncycastle.math.ec.ECPoint;
import com.android.org.bouncycastle.util.encoders.Hex;
import java.math.BigInteger;

public class SecP224R1Curve extends ECCurve.AbstractFp {
    private static final int SecP224R1_DEFAULT_COORDS = 2;
    public static final BigInteger q = new BigInteger(1, Hex.decode("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF000000000000000000000001"));
    protected SecP224R1Point infinity;

    public SecP224R1Curve() {
        super(q);
        this.infinity = new SecP224R1Point(this, null, null);
        this.a = fromBigInteger(new BigInteger(1, Hex.decode("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFFFFFE")));
        this.b = fromBigInteger(new BigInteger(1, Hex.decode("B4050A850C04B3ABF54132565044B0B7D7BFD8BA270B39432355FFB4")));
        this.order = new BigInteger(1, Hex.decode("FFFFFFFFFFFFFFFFFFFFFFFFFFFF16A2E0B8F03E13DD29455C5C2A3D"));
        this.cofactor = BigInteger.valueOf(1L);
        this.coord = 2;
    }

    @Override
    protected ECCurve cloneCurve() {
        return new SecP224R1Curve();
    }

    @Override
    public boolean supportsCoordinateSystem(int i) {
        if (i == 2) {
            return true;
        }
        return false;
    }

    public BigInteger getQ() {
        return q;
    }

    @Override
    public int getFieldSize() {
        return q.bitLength();
    }

    @Override
    public ECFieldElement fromBigInteger(BigInteger bigInteger) {
        return new SecP224R1FieldElement(bigInteger);
    }

    @Override
    protected ECPoint createRawPoint(ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, boolean z) {
        return new SecP224R1Point(this, eCFieldElement, eCFieldElement2, z);
    }

    @Override
    protected ECPoint createRawPoint(ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, ECFieldElement[] eCFieldElementArr, boolean z) {
        return new SecP224R1Point(this, eCFieldElement, eCFieldElement2, eCFieldElementArr, z);
    }

    @Override
    public ECPoint getInfinity() {
        return this.infinity;
    }
}
