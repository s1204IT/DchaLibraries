package com.android.org.bouncycastle.crypto.engines;

import com.android.org.bouncycastle.crypto.AsymmetricBlockCipher;
import com.android.org.bouncycastle.crypto.CipherParameters;
import com.android.org.bouncycastle.crypto.params.ParametersWithRandom;
import com.android.org.bouncycastle.crypto.params.RSAKeyParameters;
import com.android.org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import com.android.org.bouncycastle.util.BigIntegers;
import java.math.BigInteger;
import java.security.SecureRandom;

public class RSABlindedEngine implements AsymmetricBlockCipher {
    private static final BigInteger ONE = BigInteger.valueOf(1);
    private RSACoreEngine core = new RSACoreEngine();
    private RSAKeyParameters key;
    private SecureRandom random;

    @Override
    public void init(boolean forEncryption, CipherParameters param) {
        this.core.init(forEncryption, param);
        if (param instanceof ParametersWithRandom) {
            ParametersWithRandom rParam = (ParametersWithRandom) param;
            this.key = (RSAKeyParameters) rParam.getParameters();
            this.random = rParam.getRandom();
        } else {
            this.key = (RSAKeyParameters) param;
            this.random = new SecureRandom();
        }
    }

    @Override
    public int getInputBlockSize() {
        return this.core.getInputBlockSize();
    }

    @Override
    public int getOutputBlockSize() {
        return this.core.getOutputBlockSize();
    }

    @Override
    public byte[] processBlock(byte[] in, int inOff, int inLen) {
        BigInteger result;
        RSAPrivateCrtKeyParameters k;
        BigInteger e;
        if (this.key == null) {
            throw new IllegalStateException("RSA engine not initialised");
        }
        BigInteger input = this.core.convertInput(in, inOff, inLen);
        if ((this.key instanceof RSAPrivateCrtKeyParameters) && (e = (k = (RSAPrivateCrtKeyParameters) this.key).getPublicExponent()) != null) {
            BigInteger m = k.getModulus();
            BigInteger r = BigIntegers.createRandomInRange(ONE, m.subtract(ONE), this.random);
            BigInteger blindedInput = r.modPow(e, m).multiply(input).mod(m);
            BigInteger blindedResult = this.core.processBlock(blindedInput);
            BigInteger rInv = r.modInverse(m);
            result = blindedResult.multiply(rInv).mod(m);
        } else {
            result = this.core.processBlock(input);
        }
        return this.core.convertOutput(result);
    }
}
