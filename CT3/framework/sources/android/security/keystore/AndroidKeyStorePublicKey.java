package android.security.keystore;

import java.security.PublicKey;
import java.util.Arrays;

public class AndroidKeyStorePublicKey extends AndroidKeyStoreKey implements PublicKey {
    private final byte[] mEncoded;

    public AndroidKeyStorePublicKey(String alias, int uid, String algorithm, byte[] x509EncodedForm) {
        super(alias, uid, algorithm);
        this.mEncoded = ArrayUtils.cloneIfNotEmpty(x509EncodedForm);
    }

    @Override
    public String getFormat() {
        return "X.509";
    }

    @Override
    public byte[] getEncoded() {
        return ArrayUtils.cloneIfNotEmpty(this.mEncoded);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        return (result * 31) + Arrays.hashCode(this.mEncoded);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj) || getClass() != obj.getClass()) {
            return false;
        }
        AndroidKeyStorePublicKey other = (AndroidKeyStorePublicKey) obj;
        return Arrays.equals(this.mEncoded, other.mEncoded);
    }
}
