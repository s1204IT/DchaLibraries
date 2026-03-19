package com.android.org.bouncycastle.jcajce.provider.asymmetric.dh;

import com.android.org.bouncycastle.crypto.generators.DHParametersGenerator;
import com.android.org.bouncycastle.crypto.params.DHParameters;
import com.android.org.bouncycastle.jcajce.provider.asymmetric.util.BaseAlgorithmParameterGeneratorSpi;
import com.android.org.bouncycastle.jcajce.provider.asymmetric.util.PrimeCertaintyCalculator;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.spec.DHGenParameterSpec;
import javax.crypto.spec.DHParameterSpec;

public class AlgorithmParameterGeneratorSpi extends BaseAlgorithmParameterGeneratorSpi {
    protected SecureRandom random;
    protected int strength = 2048;
    private int l = 0;

    @Override
    protected void engineInit(int i, SecureRandom secureRandom) {
        this.strength = i;
        this.random = secureRandom;
    }

    @Override
    protected void engineInit(AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidAlgorithmParameterException {
        if (!(algorithmParameterSpec instanceof DHGenParameterSpec)) {
            throw new InvalidAlgorithmParameterException("DH parameter generator requires a DHGenParameterSpec for initialisation");
        }
        DHGenParameterSpec dHGenParameterSpec = (DHGenParameterSpec) algorithmParameterSpec;
        this.strength = dHGenParameterSpec.getPrimeSize();
        this.l = dHGenParameterSpec.getExponentSize();
        this.random = secureRandom;
    }

    @Override
    protected AlgorithmParameters engineGenerateParameters() {
        DHParametersGenerator dHParametersGenerator = new DHParametersGenerator();
        int defaultCertainty = PrimeCertaintyCalculator.getDefaultCertainty(this.strength);
        if (this.random != null) {
            dHParametersGenerator.init(this.strength, defaultCertainty, this.random);
        } else {
            dHParametersGenerator.init(this.strength, defaultCertainty, new SecureRandom());
        }
        DHParameters dHParametersGenerateParameters = dHParametersGenerator.generateParameters();
        try {
            AlgorithmParameters algorithmParametersCreateParametersInstance = createParametersInstance("DH");
            algorithmParametersCreateParametersInstance.init(new DHParameterSpec(dHParametersGenerateParameters.getP(), dHParametersGenerateParameters.getG(), this.l));
            return algorithmParametersCreateParametersInstance;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
