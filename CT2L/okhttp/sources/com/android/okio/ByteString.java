package com.android.okio;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public final class ByteString {
    final byte[] data;
    private transient int hashCode;
    private transient String utf8;
    private static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    public static final ByteString EMPTY = of(new byte[0]);

    ByteString(byte[] data) {
        this.data = data;
    }

    public static ByteString of(byte... data) {
        return new ByteString((byte[]) data.clone());
    }

    public static ByteString encodeUtf8(String s) {
        ByteString byteString = new ByteString(s.getBytes(Util.UTF_8));
        byteString.utf8 = s;
        return byteString;
    }

    public String utf8() {
        String result = this.utf8;
        if (result != null) {
            return result;
        }
        String result2 = new String(this.data, Util.UTF_8);
        this.utf8 = result2;
        return result2;
    }

    public String base64() {
        return Base64.encode(this.data);
    }

    public static ByteString decodeBase64(String base64) {
        byte[] decoded = Base64.decode(base64);
        if (decoded != null) {
            return new ByteString(decoded);
        }
        return null;
    }

    public String hex() {
        char[] result = new char[this.data.length * 2];
        byte[] arr$ = this.data;
        int c = 0;
        for (byte b : arr$) {
            int c2 = c + 1;
            result[c] = HEX_DIGITS[(b >> 4) & 15];
            c = c2 + 1;
            result[c2] = HEX_DIGITS[b & 15];
        }
        return new String(result);
    }

    public static ByteString decodeHex(String hex) {
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Unexpected hex string: " + hex);
        }
        byte[] result = new byte[hex.length() / 2];
        for (int i = 0; i < result.length; i++) {
            int d1 = decodeHexDigit(hex.charAt(i * 2)) << 4;
            int d2 = decodeHexDigit(hex.charAt((i * 2) + 1));
            result[i] = (byte) (d1 + d2);
        }
        return of(result);
    }

    private static int decodeHexDigit(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f') {
            return (c - 'a') + 10;
        }
        if (c < 'A' || c > 'F') {
            throw new IllegalArgumentException("Unexpected hex digit: " + c);
        }
        return (c - 'A') + 10;
    }

    public static ByteString read(InputStream in, int byteCount) throws IOException {
        byte[] result = new byte[byteCount];
        int offset = 0;
        while (offset < byteCount) {
            int read = in.read(result, offset, byteCount - offset);
            if (read == -1) {
                throw new EOFException();
            }
            offset += read;
        }
        return new ByteString(result);
    }

    public ByteString toAsciiLowercase() {
        for (int i = 0; i < this.data.length; i++) {
            byte c = this.data[i];
            if (c >= 65 && c <= 90) {
                byte[] lowercase = (byte[]) this.data.clone();
                lowercase[i] = (byte) (c + 32);
                for (int i2 = i + 1; i2 < lowercase.length; i2++) {
                    byte c2 = lowercase[i2];
                    if (c2 >= 65 && c2 <= 90) {
                        lowercase[i2] = (byte) (c2 + 32);
                    }
                }
                return new ByteString(lowercase);
            }
        }
        return this;
    }

    public byte getByte(int pos) {
        return this.data[pos];
    }

    public int size() {
        return this.data.length;
    }

    public byte[] toByteArray() {
        return (byte[]) this.data.clone();
    }

    public void write(OutputStream out) throws IOException {
        out.write(this.data);
    }

    public boolean equals(Object o) {
        return o == this || ((o instanceof ByteString) && Arrays.equals(((ByteString) o).data, this.data));
    }

    public int hashCode() {
        int result = this.hashCode;
        if (result != 0) {
            return result;
        }
        int result2 = Arrays.hashCode(this.data);
        this.hashCode = result2;
        return result2;
    }

    public String toString() {
        if (this.data.length == 0) {
            return "ByteString[size=0]";
        }
        if (this.data.length <= 16) {
            return String.format("ByteString[size=%s data=%s]", Integer.valueOf(this.data.length), hex());
        }
        try {
            return String.format("ByteString[size=%s md5=%s]", Integer.valueOf(this.data.length), of(MessageDigest.getInstance("MD5").digest(this.data)).hex());
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError();
        }
    }
}
