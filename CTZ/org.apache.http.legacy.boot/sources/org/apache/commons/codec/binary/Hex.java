package org.apache.commons.codec.binary;

import org.apache.commons.codec.BinaryDecoder;
import org.apache.commons.codec.BinaryEncoder;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.EncoderException;

@Deprecated
public class Hex implements BinaryEncoder, BinaryDecoder {
    private static final char[] DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public static byte[] decodeHex(char[] cArr) throws DecoderException {
        int length = cArr.length;
        if ((length & 1) != 0) {
            throw new DecoderException("Odd number of characters.");
        }
        byte[] bArr = new byte[length >> 1];
        int i = 0;
        int i2 = 0;
        while (i < length) {
            int digit = toDigit(cArr[i], i) << 4;
            int i3 = i + 1;
            int digit2 = digit | toDigit(cArr[i3], i3);
            i = i3 + 1;
            bArr[i2] = (byte) (digit2 & 255);
            i2++;
        }
        return bArr;
    }

    protected static int toDigit(char c, int i) throws DecoderException {
        int iDigit = Character.digit(c, 16);
        if (iDigit == -1) {
            throw new DecoderException("Illegal hexadecimal charcter " + c + " at index " + i);
        }
        return iDigit;
    }

    public static char[] encodeHex(byte[] bArr) {
        int length = bArr.length;
        char[] cArr = new char[length << 1];
        int i = 0;
        for (int i2 = 0; i2 < length; i2++) {
            int i3 = i + 1;
            cArr[i] = DIGITS[(240 & bArr[i2]) >>> 4];
            i = i3 + 1;
            cArr[i3] = DIGITS[15 & bArr[i2]];
        }
        return cArr;
    }

    @Override
    public byte[] decode(byte[] bArr) throws DecoderException {
        return decodeHex(new String(bArr).toCharArray());
    }

    @Override
    public Object decode(Object obj) throws DecoderException {
        try {
            return decodeHex(obj instanceof String ? obj.toCharArray() : (char[]) obj);
        } catch (ClassCastException e) {
            throw new DecoderException(e.getMessage());
        }
    }

    @Override
    public byte[] encode(byte[] bArr) {
        return new String(encodeHex(bArr)).getBytes();
    }

    @Override
    public Object encode(Object obj) throws EncoderException {
        try {
            return encodeHex(obj instanceof String ? obj.getBytes() : (byte[]) obj);
        } catch (ClassCastException e) {
            throw new EncoderException(e.getMessage());
        }
    }
}
