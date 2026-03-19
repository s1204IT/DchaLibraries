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
    public void init(boolean z, CipherParameters cipherParameters) {
        this.core.init(z, cipherParameters);
        if (cipherParameters instanceof ParametersWithRandom) {
            ParametersWithRandom parametersWithRandom = (ParametersWithRandom) cipherParameters;
            this.key = (RSAKeyParameters) parametersWithRandom.getParameters();
            this.random = parametersWithRandom.getRandom();
        } else {
            this.key = (RSAKeyParameters) cipherParameters;
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
    public byte[] processBlock(byte[] bArr, int i, int i2) {
        BigInteger bigIntegerProcessBlock;
        RSAPrivateCrtKeyParameters rSAPrivateCrtKeyParameters;
        BigInteger publicExponent;
        if (this.key == null) {
            throw new IllegalStateException("RSA engine not initialised");
        }
        BigInteger bigIntegerConvertInput = this.core.convertInput(bArr, i, i2);
        if ((this.key instanceof RSAPrivateCrtKeyParameters) && (publicExponent = (rSAPrivateCrtKeyParameters = (RSAPrivateCrtKeyParameters) this.key).getPublicExponent()) != null) {
            BigInteger modulus = rSAPrivateCrtKeyParameters.getModulus();
            BigInteger bigIntegerCreateRandomInRange = BigIntegers.createRandomInRange(ONE, modulus.subtract(ONE), this.random);
            bigIntegerProcessBlock = this.core.processBlock(bigIntegerCreateRandomInRange.modPow(publicExponent, modulus).multiply(bigIntegerConvertInput).mod(modulus)).multiply(bigIntegerCreateRandomInRange.modInverse(modulus)).mod(modulus);
            if (!bigIntegerConvertInput.equals(bigIntegerProcessBlock.modPow(publicExponent, modulus))) {
                throw new IllegalStateException("RSA engine faulty decryption/signing detected");
            }
        } else {
            bigIntegerProcessBlock = this.core.processBlock(bigIntegerConvertInput);
        }
        return this.core.convertOutput(bigIntegerProcessBlock);
    }
}
