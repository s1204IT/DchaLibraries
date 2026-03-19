package org.apache.commons.codec.net;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.BitSet;
import org.apache.commons.codec.BinaryDecoder;
import org.apache.commons.codec.BinaryEncoder;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.StringDecoder;
import org.apache.commons.codec.StringEncoder;

@Deprecated
public class QuotedPrintableCodec implements BinaryEncoder, BinaryDecoder, StringEncoder, StringDecoder {
    private String charset;
    private static final BitSet PRINTABLE_CHARS = new BitSet(256);
    private static byte ESCAPE_CHAR = 61;
    private static byte TAB = 9;
    private static byte SPACE = 32;

    static {
        for (int i = 33; i <= 60; i++) {
            PRINTABLE_CHARS.set(i);
        }
        for (int i2 = 62; i2 <= 126; i2++) {
            PRINTABLE_CHARS.set(i2);
        }
        PRINTABLE_CHARS.set(TAB);
        PRINTABLE_CHARS.set(SPACE);
    }

    public QuotedPrintableCodec() {
        this.charset = "UTF-8";
    }

    public QuotedPrintableCodec(String str) {
        this.charset = "UTF-8";
        this.charset = str;
    }

    private static final void encodeQuotedPrintable(int i, ByteArrayOutputStream byteArrayOutputStream) {
        byteArrayOutputStream.write(ESCAPE_CHAR);
        char upperCase = Character.toUpperCase(Character.forDigit((i >> 4) & 15, 16));
        char upperCase2 = Character.toUpperCase(Character.forDigit(i & 15, 16));
        byteArrayOutputStream.write(upperCase);
        byteArrayOutputStream.write(upperCase2);
    }

    public static final byte[] encodeQuotedPrintable(BitSet bitSet, byte[] bArr) {
        if (bArr == null) {
            return null;
        }
        if (bitSet == null) {
            bitSet = PRINTABLE_CHARS;
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        for (int i = 0; i < bArr.length; i++) {
            int i2 = bArr[i];
            if (i2 < 0) {
                i2 += 256;
            }
            if (bitSet.get(i2)) {
                byteArrayOutputStream.write(i2);
            } else {
                encodeQuotedPrintable(i2, byteArrayOutputStream);
            }
        }
        return byteArrayOutputStream.toByteArray();
    }

    public static final byte[] decodeQuotedPrintable(byte[] bArr) throws DecoderException {
        if (bArr == null) {
            return null;
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int i = 0;
        while (i < bArr.length) {
            byte b = bArr[i];
            if (b == ESCAPE_CHAR) {
                int i2 = i + 1;
                try {
                    int iDigit = Character.digit((char) bArr[i2], 16);
                    i = i2 + 1;
                    int iDigit2 = Character.digit((char) bArr[i], 16);
                    if (iDigit == -1 || iDigit2 == -1) {
                        throw new DecoderException("Invalid quoted-printable encoding");
                    }
                    byteArrayOutputStream.write((char) ((iDigit << 4) + iDigit2));
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw new DecoderException("Invalid quoted-printable encoding");
                }
            } else {
                byteArrayOutputStream.write(b);
            }
            i++;
        }
        return byteArrayOutputStream.toByteArray();
    }

    @Override
    public byte[] encode(byte[] bArr) {
        return encodeQuotedPrintable(PRINTABLE_CHARS, bArr);
    }

    @Override
    public byte[] decode(byte[] bArr) throws DecoderException {
        return decodeQuotedPrintable(bArr);
    }

    @Override
    public String encode(String str) throws EncoderException {
        if (str == null) {
            return null;
        }
        try {
            return encode(str, getDefaultCharset());
        } catch (UnsupportedEncodingException e) {
            throw new EncoderException(e.getMessage());
        }
    }

    public String decode(String str, String str2) throws DecoderException, UnsupportedEncodingException {
        if (str == null) {
            return null;
        }
        return new String(decode(str.getBytes("US-ASCII")), str2);
    }

    @Override
    public String decode(String str) throws DecoderException {
        if (str == null) {
            return null;
        }
        try {
            return decode(str, getDefaultCharset());
        } catch (UnsupportedEncodingException e) {
            throw new DecoderException(e.getMessage());
        }
    }

    @Override
    public Object encode(Object obj) throws EncoderException {
        if (obj == 0) {
            return null;
        }
        if (obj instanceof byte[]) {
            return encode((byte[]) obj);
        }
        if (obj instanceof String) {
            return encode((String) obj);
        }
        throw new EncoderException("Objects of type " + obj.getClass().getName() + " cannot be quoted-printable encoded");
    }

    @Override
    public Object decode(Object obj) throws DecoderException {
        if (obj == 0) {
            return null;
        }
        if (obj instanceof byte[]) {
            return decode((byte[]) obj);
        }
        if (obj instanceof String) {
            return decode((String) obj);
        }
        throw new DecoderException("Objects of type " + obj.getClass().getName() + " cannot be quoted-printable decoded");
    }

    public String getDefaultCharset() {
        return this.charset;
    }

    public String encode(String str, String str2) throws UnsupportedEncodingException {
        if (str == null) {
            return null;
        }
        return new String(encode(str.getBytes(str2)), "US-ASCII");
    }
}
