package com.android.org.bouncycastle.jcajce.provider.asymmetric.dh;

import com.android.org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import com.android.org.bouncycastle.crypto.generators.DHBasicKeyPairGenerator;
import com.android.org.bouncycastle.crypto.generators.DHParametersGenerator;
import com.android.org.bouncycastle.crypto.params.DHKeyGenerationParameters;
import com.android.org.bouncycastle.crypto.params.DHParameters;
import com.android.org.bouncycastle.crypto.params.DHPrivateKeyParameters;
import com.android.org.bouncycastle.crypto.params.DHPublicKeyParameters;
import com.android.org.bouncycastle.jce.provider.BouncyCastleProvider;
import com.android.org.bouncycastle.util.Integers;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Hashtable;
import javax.crypto.spec.DHParameterSpec;

public class KeyPairGeneratorSpi extends KeyPairGenerator {
    int certainty;
    DHBasicKeyPairGenerator engine;
    boolean initialised;
    DHKeyGenerationParameters param;
    SecureRandom random;
    int strength;
    private static Hashtable params = new Hashtable();
    private static Object lock = new Object();

    public KeyPairGeneratorSpi() {
        super("DH");
        this.engine = new DHBasicKeyPairGenerator();
        this.strength = 1024;
        this.certainty = 20;
        this.random = new SecureRandom();
        this.initialised = false;
    }

    @Override
    public void initialize(int strength, SecureRandom random) {
        this.strength = strength;
        this.random = random;
    }

    @Override
    public void initialize(AlgorithmParameterSpec algorithmParameterSpec, SecureRandom random) throws InvalidAlgorithmParameterException {
        if (!(algorithmParameterSpec instanceof DHParameterSpec)) {
            throw new InvalidAlgorithmParameterException("parameter object not a DHParameterSpec");
        }
        this.param = new DHKeyGenerationParameters(random, new DHParameters(algorithmParameterSpec.getP(), algorithmParameterSpec.getG(), null, algorithmParameterSpec.getL()));
        this.engine.init(this.param);
        this.initialised = true;
    }

    @Override
    public KeyPair generateKeyPair() {
        if (!this.initialised) {
            Integer paramStrength = Integers.valueOf(this.strength);
            if (params.containsKey(paramStrength)) {
                this.param = (DHKeyGenerationParameters) params.get(paramStrength);
            } else {
                DHParameterSpec dhParams = BouncyCastleProvider.CONFIGURATION.getDHDefaultParameters(this.strength);
                if (dhParams != null) {
                    this.param = new DHKeyGenerationParameters(this.random, new DHParameters(dhParams.getP(), dhParams.getG(), null, dhParams.getL()));
                } else {
                    synchronized (lock) {
                        if (params.containsKey(paramStrength)) {
                            this.param = (DHKeyGenerationParameters) params.get(paramStrength);
                        } else {
                            DHParametersGenerator pGen = new DHParametersGenerator();
                            pGen.init(this.strength, this.certainty, this.random);
                            this.param = new DHKeyGenerationParameters(this.random, pGen.generateParameters());
                            params.put(paramStrength, this.param);
                        }
                    }
                }
            }
            this.engine.init(this.param);
            this.initialised = true;
        }
        AsymmetricCipherKeyPair pair = this.engine.generateKeyPair();
        DHPublicKeyParameters pub = (DHPublicKeyParameters) pair.getPublic();
        DHPrivateKeyParameters priv = (DHPrivateKeyParameters) pair.getPrivate();
        return new KeyPair(new BCDHPublicKey(pub), new BCDHPrivateKey(priv));
    }
}
