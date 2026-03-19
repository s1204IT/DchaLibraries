package android.security.keystore;

import javax.crypto.SecretKey;

public class AndroidKeyStoreSecretKey extends AndroidKeyStoreKey implements SecretKey {
    public AndroidKeyStoreSecretKey(String str, int i, String str2) {
        super(str, i, str2);
    }
}
