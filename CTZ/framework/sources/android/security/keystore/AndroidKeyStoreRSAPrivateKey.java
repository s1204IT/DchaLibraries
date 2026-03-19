package android.security.keystore;

import java.math.BigInteger;
import java.security.interfaces.RSAKey;

public class AndroidKeyStoreRSAPrivateKey extends AndroidKeyStorePrivateKey implements RSAKey {
    private final BigInteger mModulus;

    public AndroidKeyStoreRSAPrivateKey(String str, int i, BigInteger bigInteger) {
        super(str, i, KeyProperties.KEY_ALGORITHM_RSA);
        this.mModulus = bigInteger;
    }

    @Override
    public BigInteger getModulus() {
        return this.mModulus;
    }
}
