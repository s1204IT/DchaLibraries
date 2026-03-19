package android.security.net.config;

import android.security.keystore.KeyProperties;
import java.util.Arrays;

public final class Pin {
    public final byte[] digest;
    public final String digestAlgorithm;
    private final int mHashCode;

    public Pin(String str, byte[] bArr) {
        this.digestAlgorithm = str;
        this.digest = bArr;
        this.mHashCode = str.hashCode() ^ Arrays.hashCode(bArr);
    }

    public static boolean isSupportedDigestAlgorithm(String str) {
        return KeyProperties.DIGEST_SHA256.equalsIgnoreCase(str);
    }

    public static int getDigestLength(String str) {
        if (KeyProperties.DIGEST_SHA256.equalsIgnoreCase(str)) {
            return 32;
        }
        throw new IllegalArgumentException("Unsupported digest algorithm: " + str);
    }

    public int hashCode() {
        return this.mHashCode;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Pin)) {
            return false;
        }
        Pin pin = (Pin) obj;
        return pin.hashCode() == this.mHashCode && Arrays.equals(this.digest, pin.digest) && this.digestAlgorithm.equals(pin.digestAlgorithm);
    }
}
