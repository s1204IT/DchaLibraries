package com.android.org.bouncycastle.crypto.signers;

import com.android.org.bouncycastle.crypto.CipherParameters;
import com.android.org.bouncycastle.crypto.DSA;
import com.android.org.bouncycastle.crypto.params.ECKeyParameters;
import com.android.org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import com.android.org.bouncycastle.crypto.params.ECPublicKeyParameters;
import com.android.org.bouncycastle.crypto.params.ParametersWithRandom;
import com.android.org.bouncycastle.math.ec.ECAlgorithms;
import com.android.org.bouncycastle.math.ec.ECConstants;
import com.android.org.bouncycastle.math.ec.ECPoint;
import java.math.BigInteger;
import java.security.SecureRandom;

public class ECDSASigner implements ECConstants, DSA {
    private final DSAKCalculator kCalculator;
    private ECKeyParameters key;
    private SecureRandom random;

    public ECDSASigner() {
        this.kCalculator = new RandomDSAKCalculator();
    }

    public ECDSASigner(DSAKCalculator kCalculator) {
        this.kCalculator = kCalculator;
    }

    @Override
    public void init(boolean forSigning, CipherParameters param) {
        if (forSigning) {
            if (param instanceof ParametersWithRandom) {
                ParametersWithRandom rParam = (ParametersWithRandom) param;
                this.random = rParam.getRandom();
                this.key = (ECPrivateKeyParameters) rParam.getParameters();
                return;
            } else {
                this.random = new SecureRandom();
                this.key = (ECPrivateKeyParameters) param;
                return;
            }
        }
        this.key = (ECPublicKeyParameters) param;
    }

    @Override
    public BigInteger[] generateSignature(byte[] message) {
        BigInteger k;
        BigInteger r;
        BigInteger s;
        BigInteger n = this.key.getParameters().getN();
        BigInteger e = calculateE(n, message);
        if (this.kCalculator.isDeterministic()) {
            this.kCalculator.init(n, ((ECPrivateKeyParameters) this.key).getD(), message);
        } else {
            this.kCalculator.init(n, this.random);
        }
        do {
            do {
                k = this.kCalculator.nextK();
                ECPoint p = this.key.getParameters().getG().multiply(k).normalize();
                BigInteger x = p.getAffineXCoord().toBigInteger();
                r = x.mod(n);
            } while (r.equals(ZERO));
            BigInteger d = ((ECPrivateKeyParameters) this.key).getD();
            s = k.modInverse(n).multiply(e.add(d.multiply(r))).mod(n);
        } while (s.equals(ZERO));
        BigInteger[] res = {r, s};
        return res;
    }

    @Override
    public boolean verifySignature(byte[] message, BigInteger r, BigInteger s) {
        BigInteger n = this.key.getParameters().getN();
        BigInteger e = calculateE(n, message);
        if (r.compareTo(ONE) < 0 || r.compareTo(n) >= 0) {
            return false;
        }
        if (s.compareTo(ONE) < 0 || s.compareTo(n) >= 0) {
            return false;
        }
        BigInteger c = s.modInverse(n);
        BigInteger u1 = e.multiply(c).mod(n);
        BigInteger u2 = r.multiply(c).mod(n);
        ECPoint G = this.key.getParameters().getG();
        ECPoint Q = ((ECPublicKeyParameters) this.key).getQ();
        ECPoint point = ECAlgorithms.sumOfTwoMultiplies(G, u1, Q, u2).normalize();
        if (point.isInfinity()) {
            return false;
        }
        BigInteger v = point.getAffineXCoord().toBigInteger().mod(n);
        return v.equals(r);
    }

    private BigInteger calculateE(BigInteger n, byte[] message) {
        int log2n = n.bitLength();
        int messageBitLength = message.length * 8;
        BigInteger e = new BigInteger(1, message);
        if (log2n < messageBitLength) {
            return e.shiftRight(messageBitLength - log2n);
        }
        return e;
    }
}
