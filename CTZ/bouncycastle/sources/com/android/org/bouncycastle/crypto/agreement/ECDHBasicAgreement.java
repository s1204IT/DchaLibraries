package com.android.org.bouncycastle.crypto.agreement;

import com.android.org.bouncycastle.crypto.BasicAgreement;
import com.android.org.bouncycastle.crypto.CipherParameters;
import com.android.org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import com.android.org.bouncycastle.crypto.params.ECPublicKeyParameters;
import com.android.org.bouncycastle.math.ec.ECCurve;
import com.android.org.bouncycastle.math.ec.ECPoint;
import java.math.BigInteger;

public class ECDHBasicAgreement implements BasicAgreement {
    private ECPrivateKeyParameters key;

    @Override
    public void init(CipherParameters cipherParameters) {
        this.key = (ECPrivateKeyParameters) cipherParameters;
    }

    @Override
    public int getFieldSize() {
        return (this.key.getParameters().getCurve().getFieldSize() + 7) / 8;
    }

    @Override
    public BigInteger calculateAgreement(CipherParameters cipherParameters) {
        ECPublicKeyParameters eCPublicKeyParameters = (ECPublicKeyParameters) cipherParameters;
        ECPoint q = eCPublicKeyParameters.getQ();
        ECCurve curve = this.key.getParameters().getCurve();
        if (q.isInfinity()) {
            throw new IllegalStateException("Infinity is not a valid public key for ECDH");
        }
        try {
            curve.validatePoint(q.getXCoord().toBigInteger(), q.getYCoord().toBigInteger());
            ECPoint eCPointCreatePoint = curve.createPoint(q.getXCoord().toBigInteger(), q.getYCoord().toBigInteger());
            if (!eCPublicKeyParameters.getParameters().equals(this.key.getParameters())) {
                throw new IllegalStateException("ECDH public key has wrong domain parameters");
            }
            ECPoint eCPointNormalize = eCPointCreatePoint.multiply(this.key.getD()).normalize();
            if (eCPointNormalize.isInfinity()) {
                throw new IllegalStateException("Infinity is not a valid agreement value for ECDH");
            }
            return eCPointNormalize.getAffineXCoord().toBigInteger();
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("The peer public key must be on the curve for ECDH");
        }
    }
}
