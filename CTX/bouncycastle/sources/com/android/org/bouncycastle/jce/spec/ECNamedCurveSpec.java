package com.android.org.bouncycastle.jce.spec;

import com.android.org.bouncycastle.math.ec.ECAlgorithms;
import com.android.org.bouncycastle.math.ec.ECCurve;
import com.android.org.bouncycastle.math.field.FiniteField;
import com.android.org.bouncycastle.math.field.Polynomial;
import com.android.org.bouncycastle.math.field.PolynomialExtensionField;
import com.android.org.bouncycastle.util.Arrays;
import java.math.BigInteger;
import java.security.spec.ECField;
import java.security.spec.ECFieldF2m;
import java.security.spec.ECFieldFp;
import java.security.spec.ECPoint;
import java.security.spec.EllipticCurve;

public class ECNamedCurveSpec extends java.security.spec.ECParameterSpec {
    private String name;

    private static EllipticCurve convertCurve(ECCurve eCCurve, byte[] bArr) {
        return new EllipticCurve(convertField(eCCurve.getField()), eCCurve.getA().toBigInteger(), eCCurve.getB().toBigInteger(), bArr);
    }

    private static ECField convertField(FiniteField finiteField) {
        if (ECAlgorithms.isFpField(finiteField)) {
            return new ECFieldFp(finiteField.getCharacteristic());
        }
        Polynomial minimalPolynomial = ((PolynomialExtensionField) finiteField).getMinimalPolynomial();
        int[] exponentsPresent = minimalPolynomial.getExponentsPresent();
        return new ECFieldF2m(minimalPolynomial.getDegree(), Arrays.reverse(Arrays.copyOfRange(exponentsPresent, 1, exponentsPresent.length - 1)));
    }

    private static ECPoint convertPoint(com.android.org.bouncycastle.math.ec.ECPoint eCPoint) {
        com.android.org.bouncycastle.math.ec.ECPoint eCPointNormalize = eCPoint.normalize();
        return new ECPoint(eCPointNormalize.getAffineXCoord().toBigInteger(), eCPointNormalize.getAffineYCoord().toBigInteger());
    }

    public ECNamedCurveSpec(String str, ECCurve eCCurve, com.android.org.bouncycastle.math.ec.ECPoint eCPoint, BigInteger bigInteger) {
        super(convertCurve(eCCurve, null), convertPoint(eCPoint), bigInteger, 1);
        this.name = str;
    }

    public ECNamedCurveSpec(String str, EllipticCurve ellipticCurve, ECPoint eCPoint, BigInteger bigInteger) {
        super(ellipticCurve, eCPoint, bigInteger, 1);
        this.name = str;
    }

    public ECNamedCurveSpec(String str, ECCurve eCCurve, com.android.org.bouncycastle.math.ec.ECPoint eCPoint, BigInteger bigInteger, BigInteger bigInteger2) {
        super(convertCurve(eCCurve, null), convertPoint(eCPoint), bigInteger, bigInteger2.intValue());
        this.name = str;
    }

    public ECNamedCurveSpec(String str, EllipticCurve ellipticCurve, ECPoint eCPoint, BigInteger bigInteger, BigInteger bigInteger2) {
        super(ellipticCurve, eCPoint, bigInteger, bigInteger2.intValue());
        this.name = str;
    }

    public ECNamedCurveSpec(String str, ECCurve eCCurve, com.android.org.bouncycastle.math.ec.ECPoint eCPoint, BigInteger bigInteger, BigInteger bigInteger2, byte[] bArr) {
        super(convertCurve(eCCurve, bArr), convertPoint(eCPoint), bigInteger, bigInteger2.intValue());
        this.name = str;
    }

    public String getName() {
        return this.name;
    }
}
