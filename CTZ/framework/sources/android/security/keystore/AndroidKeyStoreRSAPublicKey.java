package android.security.keystore;

import java.math.BigInteger;
import java.security.interfaces.RSAPublicKey;

public class AndroidKeyStoreRSAPublicKey extends AndroidKeyStorePublicKey implements RSAPublicKey {
    private final BigInteger mModulus;
    private final BigInteger mPublicExponent;

    public AndroidKeyStoreRSAPublicKey(String str, int i, byte[] bArr, BigInteger bigInteger, BigInteger bigInteger2) {
        super(str, i, KeyProperties.KEY_ALGORITHM_RSA, bArr);
        this.mModulus = bigInteger;
        this.mPublicExponent = bigInteger2;
    }

    public AndroidKeyStoreRSAPublicKey(String str, int i, RSAPublicKey rSAPublicKey) {
        this(str, i, rSAPublicKey.getEncoded(), rSAPublicKey.getModulus(), rSAPublicKey.getPublicExponent());
        if (!"X.509".equalsIgnoreCase(rSAPublicKey.getFormat())) {
            throw new IllegalArgumentException("Unsupported key export format: " + rSAPublicKey.getFormat());
        }
    }

    @Override
    public BigInteger getModulus() {
        return this.mModulus;
    }

    @Override
    public BigInteger getPublicExponent() {
        return this.mPublicExponent;
    }
}
