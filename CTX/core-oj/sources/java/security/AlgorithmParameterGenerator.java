package java.security;

import java.security.spec.AlgorithmParameterSpec;

public class AlgorithmParameterGenerator {
    private String algorithm;
    private AlgorithmParameterGeneratorSpi paramGenSpi;
    private Provider provider;

    protected AlgorithmParameterGenerator(AlgorithmParameterGeneratorSpi algorithmParameterGeneratorSpi, Provider provider, String str) {
        this.paramGenSpi = algorithmParameterGeneratorSpi;
        this.provider = provider;
        this.algorithm = str;
    }

    public final String getAlgorithm() {
        return this.algorithm;
    }

    public static AlgorithmParameterGenerator getInstance(String str) throws NoSuchAlgorithmException {
        try {
            Object[] impl = Security.getImpl(str, "AlgorithmParameterGenerator", (String) null);
            return new AlgorithmParameterGenerator((AlgorithmParameterGeneratorSpi) impl[0], (Provider) impl[1], str);
        } catch (NoSuchProviderException e) {
            throw new NoSuchAlgorithmException(str + " not found");
        }
    }

    public static AlgorithmParameterGenerator getInstance(String str, String str2) throws NoSuchAlgorithmException, NoSuchProviderException {
        if (str2 == null || str2.length() == 0) {
            throw new IllegalArgumentException("missing provider");
        }
        Object[] impl = Security.getImpl(str, "AlgorithmParameterGenerator", str2);
        return new AlgorithmParameterGenerator((AlgorithmParameterGeneratorSpi) impl[0], (Provider) impl[1], str);
    }

    public static AlgorithmParameterGenerator getInstance(String str, Provider provider) throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        Object[] impl = Security.getImpl(str, "AlgorithmParameterGenerator", provider);
        return new AlgorithmParameterGenerator((AlgorithmParameterGeneratorSpi) impl[0], (Provider) impl[1], str);
    }

    public final Provider getProvider() {
        return this.provider;
    }

    public final void init(int i) {
        this.paramGenSpi.engineInit(i, new SecureRandom());
    }

    public final void init(int i, SecureRandom secureRandom) {
        this.paramGenSpi.engineInit(i, secureRandom);
    }

    public final void init(AlgorithmParameterSpec algorithmParameterSpec) throws InvalidAlgorithmParameterException {
        this.paramGenSpi.engineInit(algorithmParameterSpec, new SecureRandom());
    }

    public final void init(AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidAlgorithmParameterException {
        this.paramGenSpi.engineInit(algorithmParameterSpec, secureRandom);
    }

    public final AlgorithmParameters generateParameters() {
        return this.paramGenSpi.engineGenerateParameters();
    }
}
