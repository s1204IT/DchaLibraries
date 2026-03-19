package android.security.keystore;

import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;

public class AndroidKeyStoreECPublicKey extends AndroidKeyStorePublicKey implements ECPublicKey {
    private final ECParameterSpec mParams;
    private final ECPoint mW;

    public AndroidKeyStoreECPublicKey(String str, int i, byte[] bArr, ECParameterSpec eCParameterSpec, ECPoint eCPoint) {
        super(str, i, KeyProperties.KEY_ALGORITHM_EC, bArr);
        this.mParams = eCParameterSpec;
        this.mW = eCPoint;
    }

    public AndroidKeyStoreECPublicKey(String str, int i, ECPublicKey eCPublicKey) {
        this(str, i, eCPublicKey.getEncoded(), eCPublicKey.getParams(), eCPublicKey.getW());
        if (!"X.509".equalsIgnoreCase(eCPublicKey.getFormat())) {
            throw new IllegalArgumentException("Unsupported key export format: " + eCPublicKey.getFormat());
        }
    }

    @Override
    public ECParameterSpec getParams() {
        return this.mParams;
    }

    @Override
    public ECPoint getW() {
        return this.mW;
    }
}
