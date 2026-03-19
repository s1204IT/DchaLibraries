package com.android.org.bouncycastle.crypto.agreement;

import com.android.org.bouncycastle.crypto.BasicAgreement;
import com.android.org.bouncycastle.crypto.CipherParameters;
import com.android.org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import com.android.org.bouncycastle.crypto.params.DHParameters;
import com.android.org.bouncycastle.crypto.params.DHPrivateKeyParameters;
import com.android.org.bouncycastle.crypto.params.DHPublicKeyParameters;
import com.android.org.bouncycastle.crypto.params.ParametersWithRandom;
import java.math.BigInteger;

public class DHBasicAgreement implements BasicAgreement {
    private DHParameters dhParams;
    private DHPrivateKeyParameters key;

    @Override
    public void init(CipherParameters cipherParameters) {
        DHPrivateKeyParameters dHPrivateKeyParameters;
        if (cipherParameters instanceof ParametersWithRandom) {
            dHPrivateKeyParameters = (AsymmetricKeyParameter) cipherParameters.getParameters();
        } else {
            dHPrivateKeyParameters = (AsymmetricKeyParameter) cipherParameters;
        }
        if (!(dHPrivateKeyParameters instanceof DHPrivateKeyParameters)) {
            throw new IllegalArgumentException("DHEngine expects DHPrivateKeyParameters");
        }
        this.key = dHPrivateKeyParameters;
        this.dhParams = this.key.getParameters();
    }

    @Override
    public int getFieldSize() {
        return (this.key.getParameters().getP().bitLength() + 7) / 8;
    }

    @Override
    public BigInteger calculateAgreement(CipherParameters pubKey) {
        DHPublicKeyParameters pub = (DHPublicKeyParameters) pubKey;
        if (!pub.getParameters().equals(this.dhParams)) {
            throw new IllegalArgumentException("Diffie-Hellman public key has wrong parameters.");
        }
        return pub.getY().modPow(this.key.getX(), this.dhParams.getP());
    }
}
