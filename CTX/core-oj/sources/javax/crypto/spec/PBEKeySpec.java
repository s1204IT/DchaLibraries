package javax.crypto.spec;

import java.security.spec.KeySpec;

public class PBEKeySpec implements KeySpec {
    private int iterationCount;
    private int keyLength;
    private char[] password;
    private byte[] salt;

    public PBEKeySpec(char[] cArr) {
        this.salt = null;
        this.iterationCount = 0;
        this.keyLength = 0;
        if (cArr == null || cArr.length == 0) {
            this.password = new char[0];
        } else {
            this.password = (char[]) cArr.clone();
        }
    }

    public PBEKeySpec(char[] cArr, byte[] bArr, int i, int i2) {
        this.salt = null;
        this.iterationCount = 0;
        this.keyLength = 0;
        if (cArr == null || cArr.length == 0) {
            this.password = new char[0];
        } else {
            this.password = (char[]) cArr.clone();
        }
        if (bArr == null) {
            throw new NullPointerException("the salt parameter must be non-null");
        }
        if (bArr.length == 0) {
            throw new IllegalArgumentException("the salt parameter must not be empty");
        }
        this.salt = (byte[]) bArr.clone();
        if (i <= 0) {
            throw new IllegalArgumentException("invalid iterationCount value");
        }
        if (i2 <= 0) {
            throw new IllegalArgumentException("invalid keyLength value");
        }
        this.iterationCount = i;
        this.keyLength = i2;
    }

    public PBEKeySpec(char[] cArr, byte[] bArr, int i) {
        this.salt = null;
        this.iterationCount = 0;
        this.keyLength = 0;
        if (cArr == null || cArr.length == 0) {
            this.password = new char[0];
        } else {
            this.password = (char[]) cArr.clone();
        }
        if (bArr == null) {
            throw new NullPointerException("the salt parameter must be non-null");
        }
        if (bArr.length == 0) {
            throw new IllegalArgumentException("the salt parameter must not be empty");
        }
        this.salt = (byte[]) bArr.clone();
        if (i <= 0) {
            throw new IllegalArgumentException("invalid iterationCount value");
        }
        this.iterationCount = i;
    }

    public final void clearPassword() {
        if (this.password != null) {
            for (int i = 0; i < this.password.length; i++) {
                this.password[i] = ' ';
            }
            this.password = null;
        }
    }

    public final char[] getPassword() {
        if (this.password == null) {
            throw new IllegalStateException("password has been cleared");
        }
        return (char[]) this.password.clone();
    }

    public final byte[] getSalt() {
        if (this.salt != null) {
            return (byte[]) this.salt.clone();
        }
        return null;
    }

    public final int getIterationCount() {
        return this.iterationCount;
    }

    public final int getKeyLength() {
        return this.keyLength;
    }
}
