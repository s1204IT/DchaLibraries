package android.security.keystore;

import java.util.Date;

abstract class Utils {
    private Utils() {
    }

    static Date cloneIfNotNull(Date date) {
        if (date != null) {
            return (Date) date.clone();
        }
        return null;
    }

    static byte[] cloneIfNotNull(byte[] bArr) {
        if (bArr != null) {
            return (byte[]) bArr.clone();
        }
        return null;
    }
}
