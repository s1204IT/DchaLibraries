package android.security.keystore;

import java.security.interfaces.ECKey;
import java.security.spec.ECParameterSpec;

public class AndroidKeyStoreECPrivateKey extends AndroidKeyStorePrivateKey implements ECKey {
    private final ECParameterSpec mParams;

    public AndroidKeyStoreECPrivateKey(String str, int i, ECParameterSpec eCParameterSpec) {
        super(str, i, KeyProperties.KEY_ALGORITHM_EC);
        this.mParams = eCParameterSpec;
    }

    @Override
    public ECParameterSpec getParams() {
        return this.mParams;
    }
}
