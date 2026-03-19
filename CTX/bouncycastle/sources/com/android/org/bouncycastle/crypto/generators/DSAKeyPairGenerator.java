package com.android.org.bouncycastle.crypto.generators;

import com.android.org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import com.android.org.bouncycastle.crypto.AsymmetricCipherKeyPairGenerator;
import com.android.org.bouncycastle.crypto.KeyGenerationParameters;
import com.android.org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import com.android.org.bouncycastle.crypto.params.DSAKeyGenerationParameters;
import com.android.org.bouncycastle.crypto.params.DSAParameters;
import com.android.org.bouncycastle.crypto.params.DSAPrivateKeyParameters;
import com.android.org.bouncycastle.crypto.params.DSAPublicKeyParameters;
import com.android.org.bouncycastle.math.ec.WNafUtil;
import com.android.org.bouncycastle.util.BigIntegers;
import java.math.BigInteger;
import java.security.SecureRandom;

public class DSAKeyPairGenerator implements AsymmetricCipherKeyPairGenerator {
    private static final BigInteger ONE = BigInteger.valueOf(1);
    private DSAKeyGenerationParameters param;

    @Override
    public void init(KeyGenerationParameters keyGenerationParameters) {
        this.param = (DSAKeyGenerationParameters) keyGenerationParameters;
    }

    @Override
    public AsymmetricCipherKeyPair generateKeyPair() {
        DSAParameters parameters = this.param.getParameters();
        BigInteger bigIntegerGeneratePrivateKey = generatePrivateKey(parameters.getQ(), this.param.getRandom());
        return new AsymmetricCipherKeyPair((AsymmetricKeyParameter) new DSAPublicKeyParameters(calculatePublicKey(parameters.getP(), parameters.getG(), bigIntegerGeneratePrivateKey), parameters), (AsymmetricKeyParameter) new DSAPrivateKeyParameters(bigIntegerGeneratePrivateKey, parameters));
    }

    private static BigInteger generatePrivateKey(BigInteger bigInteger, SecureRandom secureRandom) {
        BigInteger bigIntegerCreateRandomInRange;
        int iBitLength = bigInteger.bitLength() >>> 2;
        do {
            bigIntegerCreateRandomInRange = BigIntegers.createRandomInRange(ONE, bigInteger.subtract(ONE), secureRandom);
        } while (WNafUtil.getNafWeight(bigIntegerCreateRandomInRange) < iBitLength);
        return bigIntegerCreateRandomInRange;
    }

    private static BigInteger calculatePublicKey(BigInteger bigInteger, BigInteger bigInteger2, BigInteger bigInteger3) {
        return bigInteger2.modPow(bigInteger3, bigInteger);
    }
}
