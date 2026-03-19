package javax.crypto.spec;

import java.security.InvalidKeyException;
import java.security.spec.KeySpec;

public class DESedeKeySpec implements KeySpec {
    public static final int DES_EDE_KEY_LEN = 24;
    private byte[] key;

    public DESedeKeySpec(byte[] bArr) throws InvalidKeyException {
        this(bArr, 0);
    }

    public DESedeKeySpec(byte[] bArr, int i) throws InvalidKeyException {
        if (bArr.length - i < 24) {
            throw new InvalidKeyException("Wrong key size");
        }
        this.key = new byte[24];
        System.arraycopy(bArr, i, this.key, 0, 24);
    }

    public byte[] getKey() {
        return (byte[]) this.key.clone();
    }

    public static boolean isParityAdjusted(byte[] bArr, int i) throws InvalidKeyException {
        if (bArr.length - i < 24) {
            throw new InvalidKeyException("Wrong key size");
        }
        if (!DESKeySpec.isParityAdjusted(bArr, i) || !DESKeySpec.isParityAdjusted(bArr, i + 8) || !DESKeySpec.isParityAdjusted(bArr, i + 16)) {
            return false;
        }
        return true;
    }
}
