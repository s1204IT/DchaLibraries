package org.apache.http.util;

import java.io.UnsupportedEncodingException;

@Deprecated
public final class EncodingUtils {
    public static String getString(byte[] bArr, int i, int i2, String str) {
        if (bArr == null) {
            throw new IllegalArgumentException("Parameter may not be null");
        }
        if (str == null || str.length() == 0) {
            throw new IllegalArgumentException("charset may not be null or empty");
        }
        try {
            return new String(bArr, i, i2, str);
        } catch (UnsupportedEncodingException e) {
            return new String(bArr, i, i2);
        }
    }

    public static String getString(byte[] bArr, String str) {
        if (bArr == null) {
            throw new IllegalArgumentException("Parameter may not be null");
        }
        return getString(bArr, 0, bArr.length, str);
    }

    public static byte[] getBytes(String str, String str2) {
        if (str == null) {
            throw new IllegalArgumentException("data may not be null");
        }
        if (str2 == null || str2.length() == 0) {
            throw new IllegalArgumentException("charset may not be null or empty");
        }
        try {
            return str.getBytes(str2);
        } catch (UnsupportedEncodingException e) {
            return str.getBytes();
        }
    }

    public static byte[] getAsciiBytes(String str) {
        if (str == null) {
            throw new IllegalArgumentException("Parameter may not be null");
        }
        try {
            return str.getBytes("US-ASCII");
        } catch (UnsupportedEncodingException e) {
            throw new Error("HttpClient requires ASCII support");
        }
    }

    public static String getAsciiString(byte[] bArr, int i, int i2) {
        if (bArr == null) {
            throw new IllegalArgumentException("Parameter may not be null");
        }
        try {
            return new String(bArr, i, i2, "US-ASCII");
        } catch (UnsupportedEncodingException e) {
            throw new Error("HttpClient requires ASCII support");
        }
    }

    public static String getAsciiString(byte[] bArr) {
        if (bArr == null) {
            throw new IllegalArgumentException("Parameter may not be null");
        }
        return getAsciiString(bArr, 0, bArr.length);
    }

    private EncodingUtils() {
    }
}
