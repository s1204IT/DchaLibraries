package android.security.keystore;

import java.security.PublicKey;
import java.util.Arrays;

public class AndroidKeyStorePublicKey extends AndroidKeyStoreKey implements PublicKey {
    private final byte[] mEncoded;

    public AndroidKeyStorePublicKey(String str, int i, String str2, byte[] bArr) {
        super(str, i, str2);
        this.mEncoded = ArrayUtils.cloneIfNotEmpty(bArr);
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
        return (31 * super.hashCode()) + Arrays.hashCode(this.mEncoded);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        return super.equals(obj) && getClass() == obj.getClass() && Arrays.equals(this.mEncoded, ((AndroidKeyStorePublicKey) obj).mEncoded);
    }
}
