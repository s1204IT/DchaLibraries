package javax.crypto.spec;

import java.security.InvalidKeyException;
import java.security.spec.KeySpec;

public class DESKeySpec implements KeySpec {
    public static final int DES_KEY_LEN = 8;
    private static final byte[][] SEMIWEAKS = {new byte[]{-32, 1, -32, 1, -15, 1, -15, 1}, new byte[]{1, -32, 1, -32, 1, -15, 1, -15}, new byte[]{-2, 31, -2, 31, -2, 14, -2, 14}, new byte[]{31, -2, 31, -2, 14, -2, 14, -2}, new byte[]{-32, 31, -32, 31, -15, 14, -15, 14}, new byte[]{31, -32, 31, -32, 14, -15, 14, -15}, new byte[]{1, -2, 1, -2, 1, -2, 1, -2}, new byte[]{-2, 1, -2, 1, -2, 1, -2, 1}, new byte[]{1, 31, 1, 31, 1, 14, 1, 14}, new byte[]{31, 1, 31, 1, 14, 1, 14, 1}, new byte[]{-32, -2, -32, -2, -15, -2, -15, -2}, new byte[]{-2, -32, -2, -32, -2, -15, -2, -15}, new byte[]{1, 1, 1, 1, 1, 1, 1, 1}, new byte[]{-2, -2, -2, -2, -2, -2, -2, -2}, new byte[]{-32, -32, -32, -32, -15, -15, -15, -15}, new byte[]{31, 31, 31, 31, 14, 14, 14, 14}};
    private final byte[] key;

    public DESKeySpec(byte[] key) throws InvalidKeyException {
        this(key, 0);
    }

    public DESKeySpec(byte[] key, int offset) throws InvalidKeyException {
        if (key == null) {
            throw new NullPointerException("key == null");
        }
        if (key.length - offset < 8) {
            throw new InvalidKeyException("key too short");
        }
        this.key = new byte[8];
        System.arraycopy(key, offset, this.key, 0, 8);
    }

    public byte[] getKey() {
        byte[] result = new byte[8];
        System.arraycopy(this.key, 0, result, 0, 8);
        return result;
    }

    public static boolean isParityAdjusted(byte[] key, int offset) throws InvalidKeyException {
        if (key == null) {
            throw new InvalidKeyException("key == null");
        }
        if (key.length - offset < 8) {
            throw new InvalidKeyException("key too short");
        }
        for (int i = offset; i < 8; i++) {
            int byteKey = key[i];
            int byteKey2 = byteKey ^ (byteKey >> 1);
            int byteKey3 = byteKey2 ^ (byteKey2 >> 2);
            if (((byteKey3 ^ (byteKey3 >> 4)) & 1) == 0) {
                return false;
            }
        }
        return true;
    }

    public static boolean isWeak(byte[] key, int offset) throws InvalidKeyException {
        if (key == null) {
            throw new InvalidKeyException("key == null");
        }
        if (key.length - offset < 8) {
            throw new InvalidKeyException("key too short");
        }
        int i = 0;
        while (i < SEMIWEAKS.length) {
            for (int j = 0; j < 8; j++) {
                if (SEMIWEAKS[i][j] != key[offset + j]) {
                    break;
                }
            }
            return true;
        }
        return false;
    }
}
