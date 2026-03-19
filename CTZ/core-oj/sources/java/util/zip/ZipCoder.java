package java.util.zip;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import sun.nio.cs.ArrayDecoder;
import sun.nio.cs.ArrayEncoder;

final class ZipCoder {
    private Charset cs;
    private CharsetDecoder dec;
    private CharsetEncoder enc;
    private boolean isUTF8;
    private ZipCoder utf8;

    String toString(byte[] bArr, int i) {
        CharsetDecoder charsetDecoderReset = decoder().reset();
        int iMaxCharsPerByte = (int) (i * charsetDecoderReset.maxCharsPerByte());
        char[] cArr = new char[iMaxCharsPerByte];
        if (iMaxCharsPerByte == 0) {
            return new String(cArr);
        }
        if (this.isUTF8 && (charsetDecoderReset instanceof ArrayDecoder)) {
            int iDecode = ((ArrayDecoder) charsetDecoderReset).decode(bArr, 0, i, cArr);
            if (iDecode == -1) {
                throw new IllegalArgumentException("MALFORMED");
            }
            return new String(cArr, 0, iDecode);
        }
        ByteBuffer byteBufferWrap = ByteBuffer.wrap(bArr, 0, i);
        CharBuffer charBufferWrap = CharBuffer.wrap(cArr);
        CoderResult coderResultDecode = charsetDecoderReset.decode(byteBufferWrap, charBufferWrap, true);
        if (!coderResultDecode.isUnderflow()) {
            throw new IllegalArgumentException(coderResultDecode.toString());
        }
        CoderResult coderResultFlush = charsetDecoderReset.flush(charBufferWrap);
        if (!coderResultFlush.isUnderflow()) {
            throw new IllegalArgumentException(coderResultFlush.toString());
        }
        return new String(cArr, 0, charBufferWrap.position());
    }

    String toString(byte[] bArr) {
        return toString(bArr, bArr.length);
    }

    byte[] getBytes(String str) {
        CharsetEncoder charsetEncoderReset = encoder().reset();
        char[] charArray = str.toCharArray();
        int length = (int) (charArray.length * charsetEncoderReset.maxBytesPerChar());
        byte[] bArr = new byte[length];
        if (length == 0) {
            return bArr;
        }
        if (this.isUTF8 && (charsetEncoderReset instanceof ArrayEncoder)) {
            int iEncode = ((ArrayEncoder) charsetEncoderReset).encode(charArray, 0, charArray.length, bArr);
            if (iEncode == -1) {
                throw new IllegalArgumentException("MALFORMED");
            }
            return Arrays.copyOf(bArr, iEncode);
        }
        ByteBuffer byteBufferWrap = ByteBuffer.wrap(bArr);
        CoderResult coderResultEncode = charsetEncoderReset.encode(CharBuffer.wrap(charArray), byteBufferWrap, true);
        if (!coderResultEncode.isUnderflow()) {
            throw new IllegalArgumentException(coderResultEncode.toString());
        }
        CoderResult coderResultFlush = charsetEncoderReset.flush(byteBufferWrap);
        if (!coderResultFlush.isUnderflow()) {
            throw new IllegalArgumentException(coderResultFlush.toString());
        }
        if (byteBufferWrap.position() == bArr.length) {
            return bArr;
        }
        return Arrays.copyOf(bArr, byteBufferWrap.position());
    }

    byte[] getBytesUTF8(String str) {
        if (this.isUTF8) {
            return getBytes(str);
        }
        if (this.utf8 == null) {
            this.utf8 = new ZipCoder(StandardCharsets.UTF_8);
        }
        return this.utf8.getBytes(str);
    }

    String toStringUTF8(byte[] bArr, int i) {
        if (this.isUTF8) {
            return toString(bArr, i);
        }
        if (this.utf8 == null) {
            this.utf8 = new ZipCoder(StandardCharsets.UTF_8);
        }
        return this.utf8.toString(bArr, i);
    }

    boolean isUTF8() {
        return this.isUTF8;
    }

    private ZipCoder(Charset charset) {
        this.cs = charset;
        this.isUTF8 = charset.name().equals(StandardCharsets.UTF_8.name());
    }

    static ZipCoder get(Charset charset) {
        return new ZipCoder(charset);
    }

    private CharsetDecoder decoder() {
        if (this.dec == null) {
            this.dec = this.cs.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
        }
        return this.dec;
    }

    private CharsetEncoder encoder() {
        if (this.enc == null) {
            this.enc = this.cs.newEncoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
        }
        return this.enc;
    }
}
