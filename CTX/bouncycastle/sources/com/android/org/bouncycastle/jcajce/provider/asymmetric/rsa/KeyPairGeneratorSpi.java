package com.android.org.bouncycastle.jcajce.provider.asymmetric.rsa;

import com.android.org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import com.android.org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import com.android.org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import com.android.org.bouncycastle.crypto.params.RSAKeyParameters;
import com.android.org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import com.android.org.bouncycastle.jcajce.provider.asymmetric.util.PrimeCertaintyCalculator;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.RSAKeyGenParameterSpec;

public class KeyPairGeneratorSpi extends KeyPairGenerator {
    static final BigInteger defaultPublicExponent = BigInteger.valueOf(65537);
    RSAKeyPairGenerator engine;
    RSAKeyGenerationParameters param;

    public KeyPairGeneratorSpi(String str) {
        super(str);
    }

    public KeyPairGeneratorSpi() {
        super("RSA");
        this.engine = new RSAKeyPairGenerator();
        this.param = new RSAKeyGenerationParameters(defaultPublicExponent, new SecureRandom(), 2048, PrimeCertaintyCalculator.getDefaultCertainty(2048));
        this.engine.init(this.param);
    }

    @Override
    public void initialize(int i, SecureRandom secureRandom) {
        BigInteger bigInteger = defaultPublicExponent;
        if (secureRandom == null) {
            secureRandom = new SecureRandom();
        }
        this.param = new RSAKeyGenerationParameters(bigInteger, secureRandom, i, PrimeCertaintyCalculator.getDefaultCertainty(i));
        this.engine.init(this.param);
    }

    @Override
    public void initialize(AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidAlgorithmParameterException {
        if (!(algorithmParameterSpec instanceof RSAKeyGenParameterSpec)) {
            throw new InvalidAlgorithmParameterException("parameter object not a RSAKeyGenParameterSpec");
        }
        RSAKeyGenParameterSpec rSAKeyGenParameterSpec = (RSAKeyGenParameterSpec) algorithmParameterSpec;
        BigInteger publicExponent = rSAKeyGenParameterSpec.getPublicExponent();
        if (secureRandom == null) {
            secureRandom = new SecureRandom();
        }
        this.param = new RSAKeyGenerationParameters(publicExponent, secureRandom, rSAKeyGenParameterSpec.getKeysize(), PrimeCertaintyCalculator.getDefaultCertainty(2048));
        this.engine.init(this.param);
    }

    @Override
    public KeyPair generateKeyPair() {
        AsymmetricCipherKeyPair asymmetricCipherKeyPairGenerateKeyPair = this.engine.generateKeyPair();
        return new KeyPair(new BCRSAPublicKey((RSAKeyParameters) asymmetricCipherKeyPairGenerateKeyPair.getPublic()), new BCRSAPrivateCrtKey((RSAPrivateCrtKeyParameters) asymmetricCipherKeyPairGenerateKeyPair.getPrivate()));
    }
}
