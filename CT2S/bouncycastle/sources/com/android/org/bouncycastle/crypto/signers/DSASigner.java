package com.android.org.bouncycastle.crypto.signers;

import com.android.org.bouncycastle.crypto.CipherParameters;
import com.android.org.bouncycastle.crypto.DSA;
import com.android.org.bouncycastle.crypto.params.DSAKeyParameters;
import com.android.org.bouncycastle.crypto.params.DSAParameters;
import com.android.org.bouncycastle.crypto.params.DSAPrivateKeyParameters;
import com.android.org.bouncycastle.crypto.params.DSAPublicKeyParameters;
import com.android.org.bouncycastle.crypto.params.ParametersWithRandom;
import java.math.BigInteger;
import java.security.SecureRandom;

public class DSASigner implements DSA {
    private final DSAKCalculator kCalculator;
    private DSAKeyParameters key;
    private SecureRandom random;

    public DSASigner() {
        this.kCalculator = new RandomDSAKCalculator();
    }

    public DSASigner(DSAKCalculator kCalculator) {
        this.kCalculator = kCalculator;
    }

    @Override
    public void init(boolean forSigning, CipherParameters param) {
        if (forSigning) {
            if (param instanceof ParametersWithRandom) {
                ParametersWithRandom rParam = (ParametersWithRandom) param;
                this.random = rParam.getRandom();
                this.key = (DSAPrivateKeyParameters) rParam.getParameters();
                return;
            } else {
                this.random = new SecureRandom();
                this.key = (DSAPrivateKeyParameters) param;
                return;
            }
        }
        this.key = (DSAPublicKeyParameters) param;
    }

    @Override
    public BigInteger[] generateSignature(byte[] message) {
        DSAParameters params = this.key.getParameters();
        BigInteger m = calculateE(params.getQ(), message);
        if (this.kCalculator.isDeterministic()) {
            this.kCalculator.init(params.getQ(), ((DSAPrivateKeyParameters) this.key).getX(), message);
        } else {
            this.kCalculator.init(params.getQ(), this.random);
        }
        BigInteger k = this.kCalculator.nextK();
        BigInteger r = params.getG().modPow(k, params.getP()).mod(params.getQ());
        BigInteger s = k.modInverse(params.getQ()).multiply(m.add(((DSAPrivateKeyParameters) this.key).getX().multiply(r))).mod(params.getQ());
        BigInteger[] res = {r, s};
        return res;
    }

    @Override
    public boolean verifySignature(byte[] message, BigInteger r, BigInteger s) {
        DSAParameters params = this.key.getParameters();
        BigInteger m = calculateE(params.getQ(), message);
        BigInteger zero = BigInteger.valueOf(0L);
        if (zero.compareTo(r) >= 0 || params.getQ().compareTo(r) <= 0 || zero.compareTo(s) >= 0 || params.getQ().compareTo(s) <= 0) {
            return false;
        }
        BigInteger w = s.modInverse(params.getQ());
        BigInteger u1 = m.multiply(w).mod(params.getQ());
        BigInteger u2 = r.multiply(w).mod(params.getQ());
        BigInteger v = params.getG().modPow(u1, params.getP()).multiply(((DSAPublicKeyParameters) this.key).getY().modPow(u2, params.getP())).mod(params.getP()).mod(params.getQ());
        return v.equals(r);
    }

    private BigInteger calculateE(BigInteger n, byte[] message) {
        if (n.bitLength() >= message.length * 8) {
            return new BigInteger(1, message);
        }
        byte[] trunc = new byte[n.bitLength() / 8];
        System.arraycopy(message, 0, trunc, 0, trunc.length);
        return new BigInteger(1, trunc);
    }
}
