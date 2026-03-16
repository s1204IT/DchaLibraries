package javax.crypto.spec;

import java.security.InvalidKeyException;
import java.security.spec.KeySpec;

public class DESedeKeySpec implements KeySpec {
    public static final int DES_EDE_KEY_LEN = 24;
    private final byte[] key;

    public DESedeKeySpec(byte[] key) throws InvalidKeyException {
        if (key == null) {
            throw new NullPointerException("key == null");
        }
        if (key.length < 24) {
            throw new InvalidKeyException();
        }
        this.key = new byte[24];
        System.arraycopy(key, 0, this.key, 0, 24);
    }

    public DESedeKeySpec(byte[] key, int offset) throws InvalidKeyException {
        if (key == null) {
            throw new NullPointerException("key == null");
        }
        if (key.length - offset < 24) {
            throw new InvalidKeyException();
        }
        this.key = new byte[24];
        System.arraycopy(key, offset, this.key, 0, 24);
    }

    public byte[] getKey() {
        byte[] result = new byte[24];
        System.arraycopy(this.key, 0, result, 0, 24);
        return result;
    }

    public static boolean isParityAdjusted(byte[] key, int offset) throws InvalidKeyException {
        if (key.length - offset < 24) {
            throw new InvalidKeyException();
        }
        for (int i = offset; i < offset + 24; i++) {
            int b = key[i];
            if ((((b & 1) + ((b & 2) >> 1) + ((b & 4) >> 2) + ((b & 8) >> 3) + ((b & 16) >> 4) + ((b & 32) >> 5) + ((b & 64) >> 6)) & 1) == ((b & 128) >> 7)) {
                return false;
            }
        }
        return true;
    }
}
