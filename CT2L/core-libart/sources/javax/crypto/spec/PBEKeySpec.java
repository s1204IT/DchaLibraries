package javax.crypto.spec;

import java.security.spec.KeySpec;
import java.util.Arrays;
import libcore.util.EmptyArray;

public class PBEKeySpec implements KeySpec {
    private final int iterationCount;
    private final int keyLength;
    private char[] password;
    private final byte[] salt;

    public PBEKeySpec(char[] password) {
        if (password == null) {
            this.password = EmptyArray.CHAR;
        } else {
            this.password = new char[password.length];
            System.arraycopy(password, 0, this.password, 0, password.length);
        }
        this.salt = null;
        this.iterationCount = 0;
        this.keyLength = 0;
    }

    public PBEKeySpec(char[] password, byte[] salt, int iterationCount, int keyLength) {
        if (salt == null) {
            throw new NullPointerException("salt == null");
        }
        if (salt.length == 0) {
            throw new IllegalArgumentException("salt.length == 0");
        }
        if (iterationCount <= 0) {
            throw new IllegalArgumentException("iterationCount <= 0");
        }
        if (keyLength <= 0) {
            throw new IllegalArgumentException("keyLength <= 0");
        }
        if (password == null) {
            this.password = EmptyArray.CHAR;
        } else {
            this.password = new char[password.length];
            System.arraycopy(password, 0, this.password, 0, password.length);
        }
        this.salt = new byte[salt.length];
        System.arraycopy(salt, 0, this.salt, 0, salt.length);
        this.iterationCount = iterationCount;
        this.keyLength = keyLength;
    }

    public PBEKeySpec(char[] password, byte[] salt, int iterationCount) {
        if (salt == null) {
            throw new NullPointerException("salt == null");
        }
        if (salt.length == 0) {
            throw new IllegalArgumentException("salt.length == 0");
        }
        if (iterationCount <= 0) {
            throw new IllegalArgumentException("iterationCount <= 0");
        }
        if (password == null) {
            this.password = EmptyArray.CHAR;
        } else {
            this.password = new char[password.length];
            System.arraycopy(password, 0, this.password, 0, password.length);
        }
        this.salt = new byte[salt.length];
        System.arraycopy(salt, 0, this.salt, 0, salt.length);
        this.iterationCount = iterationCount;
        this.keyLength = 0;
    }

    public final void clearPassword() {
        Arrays.fill(this.password, '?');
        this.password = null;
    }

    public final char[] getPassword() {
        if (this.password == null) {
            throw new IllegalStateException("The password has been cleared");
        }
        char[] result = new char[this.password.length];
        System.arraycopy(this.password, 0, result, 0, this.password.length);
        return result;
    }

    public final byte[] getSalt() {
        if (this.salt == null) {
            return null;
        }
        byte[] result = new byte[this.salt.length];
        System.arraycopy(this.salt, 0, result, 0, this.salt.length);
        return result;
    }

    public final int getIterationCount() {
        return this.iterationCount;
    }

    public final int getKeyLength() {
        return this.keyLength;
    }
}
