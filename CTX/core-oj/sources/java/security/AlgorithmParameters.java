package java.security;

import java.io.IOException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import sun.security.jca.Providers;

public class AlgorithmParameters {
    private String algorithm;
    private boolean initialized = false;
    private AlgorithmParametersSpi paramSpi;
    private Provider provider;

    protected AlgorithmParameters(AlgorithmParametersSpi algorithmParametersSpi, Provider provider, String str) {
        this.paramSpi = algorithmParametersSpi;
        this.provider = provider;
        this.algorithm = str;
    }

    public final String getAlgorithm() {
        return this.algorithm;
    }

    public static AlgorithmParameters getInstance(String str) throws NoSuchAlgorithmException {
        try {
            Object[] impl = Security.getImpl(str, "AlgorithmParameters", (String) null);
            return new AlgorithmParameters((AlgorithmParametersSpi) impl[0], (Provider) impl[1], str);
        } catch (NoSuchProviderException e) {
            throw new NoSuchAlgorithmException(str + " not found");
        }
    }

    public static AlgorithmParameters getInstance(String str, String str2) throws NoSuchAlgorithmException, NoSuchProviderException {
        if (str2 == null || str2.length() == 0) {
            throw new IllegalArgumentException("missing provider");
        }
        Providers.checkBouncyCastleDeprecation(str2, "AlgorithmParameters", str);
        Object[] impl = Security.getImpl(str, "AlgorithmParameters", str2);
        return new AlgorithmParameters((AlgorithmParametersSpi) impl[0], (Provider) impl[1], str);
    }

    public static AlgorithmParameters getInstance(String str, Provider provider) throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        Providers.checkBouncyCastleDeprecation(provider, "AlgorithmParameters", str);
        Object[] impl = Security.getImpl(str, "AlgorithmParameters", provider);
        return new AlgorithmParameters((AlgorithmParametersSpi) impl[0], (Provider) impl[1], str);
    }

    public final Provider getProvider() {
        return this.provider;
    }

    public final void init(AlgorithmParameterSpec algorithmParameterSpec) throws InvalidParameterSpecException {
        if (this.initialized) {
            throw new InvalidParameterSpecException("already initialized");
        }
        this.paramSpi.engineInit(algorithmParameterSpec);
        this.initialized = true;
    }

    public final void init(byte[] bArr) throws IOException {
        if (this.initialized) {
            throw new IOException("already initialized");
        }
        this.paramSpi.engineInit(bArr);
        this.initialized = true;
    }

    public final void init(byte[] bArr, String str) throws IOException {
        if (this.initialized) {
            throw new IOException("already initialized");
        }
        this.paramSpi.engineInit(bArr, str);
        this.initialized = true;
    }

    public final <T extends AlgorithmParameterSpec> T getParameterSpec(Class<T> cls) throws InvalidParameterSpecException {
        if (!this.initialized) {
            throw new InvalidParameterSpecException("not initialized");
        }
        return (T) this.paramSpi.engineGetParameterSpec(cls);
    }

    public final byte[] getEncoded() throws IOException {
        if (!this.initialized) {
            throw new IOException("not initialized");
        }
        return this.paramSpi.engineGetEncoded();
    }

    public final byte[] getEncoded(String str) throws IOException {
        if (!this.initialized) {
            throw new IOException("not initialized");
        }
        return this.paramSpi.engineGetEncoded(str);
    }

    public final String toString() {
        if (!this.initialized) {
            return null;
        }
        return this.paramSpi.engineToString();
    }
}
