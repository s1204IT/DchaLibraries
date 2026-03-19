package com.android.org.bouncycastle.math.ec.custom.sec;

import com.android.org.bouncycastle.math.ec.ECConstants;
import com.android.org.bouncycastle.math.ec.ECCurve;
import com.android.org.bouncycastle.math.ec.ECFieldElement;
import com.android.org.bouncycastle.math.ec.ECPoint;
import com.android.org.bouncycastle.util.encoders.Hex;
import java.math.BigInteger;

public class SecP256K1Curve extends ECCurve.AbstractFp {
    private static final int SECP256K1_DEFAULT_COORDS = 2;
    public static final BigInteger q = new BigInteger(1, Hex.decode("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F"));
    protected SecP256K1Point infinity;

    public SecP256K1Curve() {
        super(q);
        this.infinity = new SecP256K1Point(this, null, null);
        this.a = fromBigInteger(ECConstants.ZERO);
        this.b = fromBigInteger(BigInteger.valueOf(7L));
        this.order = new BigInteger(1, Hex.decode("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141"));
        this.cofactor = BigInteger.valueOf(1L);
        this.coord = 2;
    }

    @Override
    protected ECCurve cloneCurve() {
        return new SecP256K1Curve();
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
        return new SecP256K1FieldElement(bigInteger);
    }

    @Override
    protected ECPoint createRawPoint(ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, boolean z) {
        return new SecP256K1Point(this, eCFieldElement, eCFieldElement2, z);
    }

    @Override
    protected ECPoint createRawPoint(ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, ECFieldElement[] eCFieldElementArr, boolean z) {
        return new SecP256K1Point(this, eCFieldElement, eCFieldElement2, eCFieldElementArr, z);
    }

    @Override
    public ECPoint getInfinity() {
        return this.infinity;
    }
}
