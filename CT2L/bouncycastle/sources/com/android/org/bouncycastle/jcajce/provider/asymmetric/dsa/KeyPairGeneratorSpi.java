package com.android.org.bouncycastle.jcajce.provider.asymmetric.dsa;

import com.android.org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import com.android.org.bouncycastle.crypto.generators.DSAKeyPairGenerator;
import com.android.org.bouncycastle.crypto.generators.DSAParametersGenerator;
import com.android.org.bouncycastle.crypto.params.DSAKeyGenerationParameters;
import com.android.org.bouncycastle.crypto.params.DSAParameters;
import com.android.org.bouncycastle.crypto.params.DSAPrivateKeyParameters;
import com.android.org.bouncycastle.crypto.params.DSAPublicKeyParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.DSAParameterSpec;

public class KeyPairGeneratorSpi extends KeyPairGenerator {
    int certainty;
    DSAKeyPairGenerator engine;
    boolean initialised;
    DSAKeyGenerationParameters param;
    SecureRandom random;
    int strength;

    public KeyPairGeneratorSpi() {
        super("DSA");
        this.engine = new DSAKeyPairGenerator();
        this.strength = 1024;
        this.certainty = 20;
        this.random = new SecureRandom();
        this.initialised = false;
    }

    @Override
    public void initialize(int strength, SecureRandom random) {
        if (strength < 512 || strength > 1024 || strength % 64 != 0) {
            throw new InvalidParameterException("strength must be from 512 - 1024 and a multiple of 64");
        }
        this.strength = strength;
        this.random = random;
    }

    @Override
    public void initialize(AlgorithmParameterSpec params, SecureRandom random) throws InvalidAlgorithmParameterException {
        if (!(params instanceof DSAParameterSpec)) {
            throw new InvalidAlgorithmParameterException("parameter object not a DSAParameterSpec");
        }
        DSAParameterSpec dsaParams = (DSAParameterSpec) params;
        this.param = new DSAKeyGenerationParameters(random, new DSAParameters(dsaParams.getP(), dsaParams.getQ(), dsaParams.getG()));
        this.engine.init(this.param);
        this.initialised = true;
    }

    @Override
    public KeyPair generateKeyPair() {
        if (!this.initialised) {
            DSAParametersGenerator pGen = new DSAParametersGenerator();
            pGen.init(this.strength, this.certainty, this.random);
            this.param = new DSAKeyGenerationParameters(this.random, pGen.generateParameters());
            this.engine.init(this.param);
            this.initialised = true;
        }
        AsymmetricCipherKeyPair pair = this.engine.generateKeyPair();
        DSAPublicKeyParameters pub = (DSAPublicKeyParameters) pair.getPublic();
        DSAPrivateKeyParameters priv = (DSAPrivateKeyParameters) pair.getPrivate();
        return new KeyPair(new BCDSAPublicKey(pub), new BCDSAPrivateKey(priv));
    }
}
