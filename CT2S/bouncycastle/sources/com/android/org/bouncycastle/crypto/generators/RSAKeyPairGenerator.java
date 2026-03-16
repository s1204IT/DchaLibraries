package com.android.org.bouncycastle.crypto.generators;

import com.android.org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import com.android.org.bouncycastle.crypto.AsymmetricCipherKeyPairGenerator;
import com.android.org.bouncycastle.crypto.KeyGenerationParameters;
import com.android.org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import com.android.org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import com.android.org.bouncycastle.crypto.params.RSAKeyParameters;
import com.android.org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import java.math.BigInteger;

public class RSAKeyPairGenerator implements AsymmetricCipherKeyPairGenerator {
    private static final BigInteger ONE = BigInteger.valueOf(1);
    private RSAKeyGenerationParameters param;

    @Override
    public void init(KeyGenerationParameters param) {
        this.param = (RSAKeyGenerationParameters) param;
    }

    @Override
    public AsymmetricCipherKeyPair generateKeyPair() {
        BigInteger p;
        BigInteger q;
        BigInteger n;
        int strength = this.param.getStrength();
        int pbitlength = (strength + 1) / 2;
        int qbitlength = strength - pbitlength;
        int mindiffbits = strength / 3;
        BigInteger e = this.param.getPublicExponent();
        while (true) {
            p = new BigInteger(pbitlength, 1, this.param.getRandom());
            if (!p.mod(e).equals(ONE) && p.isProbablePrime(this.param.getCertainty()) && e.gcd(p.subtract(ONE)).equals(ONE)) {
                break;
            }
        }
        while (true) {
            q = new BigInteger(qbitlength, 1, this.param.getRandom());
            if (q.subtract(p).abs().bitLength() >= mindiffbits && !q.mod(e).equals(ONE) && q.isProbablePrime(this.param.getCertainty()) && e.gcd(q.subtract(ONE)).equals(ONE)) {
                n = p.multiply(q);
                if (n.bitLength() == this.param.getStrength()) {
                    break;
                }
                p = p.max(q);
            }
        }
        if (p.compareTo(q) < 0) {
            BigInteger phi = p;
            p = q;
            q = phi;
        }
        BigInteger pSub1 = p.subtract(ONE);
        BigInteger qSub1 = q.subtract(ONE);
        BigInteger phi2 = pSub1.multiply(qSub1);
        BigInteger d = e.modInverse(phi2);
        BigInteger dP = d.remainder(pSub1);
        BigInteger dQ = d.remainder(qSub1);
        BigInteger qInv = q.modInverse(p);
        return new AsymmetricCipherKeyPair((AsymmetricKeyParameter) new RSAKeyParameters(false, n, e), (AsymmetricKeyParameter) new RSAPrivateCrtKeyParameters(n, e, d, p, q, dP, dQ, qInv));
    }
}
