package libcore.net;

import android.icu.impl.locale.XLocaleDistance;
import android.icu.text.PluralRules;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

public abstract class UriCodec {
    private static final char INVALID_INPUT_CHARACTER = 65533;

    protected abstract boolean isRetained(char c);

    private static boolean isWhitelisted(char c) {
        return ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z') || ('0' <= c && c <= '9');
    }

    private boolean isWhitelistedOrRetained(char c) {
        return isWhitelisted(c) || isRetained(c);
    }

    public final String validate(String str, int i, int i2, String str2) throws URISyntaxException {
        int i3;
        for (int i4 = i; i4 < i2; i4 = i3) {
            i3 = i4 + 1;
            char cCharAt = str.charAt(i4);
            if (!isWhitelistedOrRetained(cCharAt)) {
                if (cCharAt != '%') {
                    throw unexpectedCharacterException(str, str2, cCharAt, i3 - 1);
                }
                int i5 = 0;
                while (i5 < 2) {
                    int i6 = i3 + 1;
                    char nextCharacter = getNextCharacter(str, i3, i2, str2);
                    if (hexCharToValue(nextCharacter) >= 0) {
                        i5++;
                        i3 = i6;
                    } else {
                        throw unexpectedCharacterException(str, str2, nextCharacter, i6 - 1);
                    }
                }
            }
        }
        return str.substring(i, i2);
    }

    private static int hexCharToValue(char c) {
        if ('0' <= c && c <= '9') {
            return c - '0';
        }
        if ('a' <= c && c <= 'f') {
            return ('\n' + c) - 97;
        }
        if ('A' <= c && c <= 'F') {
            return ('\n' + c) - 65;
        }
        return -1;
    }

    private static URISyntaxException unexpectedCharacterException(String str, String str2, char c, int i) {
        String str3;
        if (str2 == null) {
            str3 = "";
        } else {
            str3 = " in [" + str2 + "]";
        }
        return new URISyntaxException(str, "Unexpected character" + str3 + PluralRules.KEYWORD_RULE_SEPARATOR + c, i);
    }

    private static char getNextCharacter(String str, int i, int i2, String str2) throws URISyntaxException {
        String str3;
        if (i >= i2) {
            if (str2 != null) {
                str3 = " in [" + str2 + "]";
            } else {
                str3 = "";
            }
            throw new URISyntaxException(str, "Unexpected end of string" + str3, i);
        }
        return str.charAt(i);
    }

    public static void validateSimple(String str, String str2) throws URISyntaxException {
        for (int i = 0; i < str.length(); i++) {
            char cCharAt = str.charAt(i);
            if (!isWhitelisted(cCharAt) && str2.indexOf(cCharAt) < 0) {
                throw unexpectedCharacterException(str, null, cCharAt, i);
            }
        }
    }

    public final String encode(String str, Charset charset) {
        StringBuilder sb = new StringBuilder(str.length());
        appendEncoded(sb, str, charset, false);
        return sb.toString();
    }

    public final void appendEncoded(StringBuilder sb, String str) {
        appendEncoded(sb, str, StandardCharsets.UTF_8, false);
    }

    public final void appendPartiallyEncoded(StringBuilder sb, String str) {
        appendEncoded(sb, str, StandardCharsets.UTF_8, true);
    }

    private void appendEncoded(StringBuilder sb, String str, Charset charset, boolean z) {
        CharsetEncoder charsetEncoderOnUnmappableCharacter = charset.newEncoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
        CharBuffer charBufferAllocate = CharBuffer.allocate(str.length());
        for (int i = 0; i < str.length(); i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt == '%' && z) {
                flushEncodingCharBuffer(sb, charsetEncoderOnUnmappableCharacter, charBufferAllocate);
                sb.append('%');
            } else if (cCharAt == ' ' && isRetained(' ')) {
                flushEncodingCharBuffer(sb, charsetEncoderOnUnmappableCharacter, charBufferAllocate);
                sb.append('+');
            } else if (isWhitelistedOrRetained(cCharAt)) {
                flushEncodingCharBuffer(sb, charsetEncoderOnUnmappableCharacter, charBufferAllocate);
                sb.append(cCharAt);
            } else {
                charBufferAllocate.put(cCharAt);
            }
        }
        flushEncodingCharBuffer(sb, charsetEncoderOnUnmappableCharacter, charBufferAllocate);
    }

    private static void flushEncodingCharBuffer(StringBuilder sb, CharsetEncoder charsetEncoder, CharBuffer charBuffer) {
        if (charBuffer.position() == 0) {
            return;
        }
        charBuffer.flip();
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(charBuffer.remaining() * ((int) Math.ceil(charsetEncoder.maxBytesPerChar())));
        byteBufferAllocate.position(0);
        CoderResult coderResultEncode = charsetEncoder.encode(charBuffer, byteBufferAllocate, true);
        if (coderResultEncode != CoderResult.UNDERFLOW) {
            throw new IllegalArgumentException("Error encoding, unexpected result [" + coderResultEncode.toString() + "] using encoder for [" + charsetEncoder.charset().name() + "]");
        }
        if (charBuffer.hasRemaining()) {
            throw new IllegalArgumentException("Encoder for [" + charsetEncoder.charset().name() + "] failed with underflow with remaining input [" + ((Object) charBuffer) + "]");
        }
        charsetEncoder.flush(byteBufferAllocate);
        if (coderResultEncode != CoderResult.UNDERFLOW) {
            throw new IllegalArgumentException("Error encoding, unexpected result [" + coderResultEncode.toString() + "] flushing encoder for [" + charsetEncoder.charset().name() + "]");
        }
        charsetEncoder.reset();
        byteBufferAllocate.flip();
        while (byteBufferAllocate.hasRemaining()) {
            byte b = byteBufferAllocate.get();
            sb.append('%');
            sb.append(intToHexDigit((b & 240) >>> 4));
            sb.append(intToHexDigit(b & 15));
        }
        charBuffer.flip();
        charBuffer.limit(charBuffer.capacity());
    }

    private static char intToHexDigit(int i) {
        if (i >= 10) {
            return (char) ((65 + i) - 10);
        }
        return (char) (48 + i);
    }

    public static String decode(String str, boolean z, Charset charset, boolean z2) {
        StringBuilder sb = new StringBuilder(str.length());
        appendDecoded(sb, str, z, charset, z2);
        return sb.toString();
    }

    private static void appendDecoded(StringBuilder sb, String str, boolean z, Charset charset, boolean z2) {
        CharsetDecoder charsetDecoderOnUnmappableCharacter = charset.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).replaceWith(XLocaleDistance.ANY).onUnmappableCharacter(CodingErrorAction.REPORT);
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(str.length());
        int i = 0;
        while (i < str.length()) {
            char cCharAt = str.charAt(i);
            i++;
            if (cCharAt == '%') {
                byte b = 0;
                int i2 = i;
                int i3 = 0;
                while (true) {
                    if (i3 >= 2) {
                        break;
                    }
                    try {
                        char nextCharacter = getNextCharacter(str, i2, str.length(), null);
                        i2++;
                        int iHexCharToValue = hexCharToValue(nextCharacter);
                        if (iHexCharToValue < 0) {
                            if (z2) {
                                throw new IllegalArgumentException(unexpectedCharacterException(str, null, nextCharacter, i2 - 1));
                            }
                            flushDecodingByteAccumulator(sb, charsetDecoderOnUnmappableCharacter, byteBufferAllocate, z2);
                            sb.append(INVALID_INPUT_CHARACTER);
                        } else {
                            b = (byte) ((b * 16) + iHexCharToValue);
                            i3++;
                        }
                    } catch (URISyntaxException e) {
                        if (z2) {
                            throw new IllegalArgumentException(e);
                        }
                        flushDecodingByteAccumulator(sb, charsetDecoderOnUnmappableCharacter, byteBufferAllocate, z2);
                        sb.append(INVALID_INPUT_CHARACTER);
                        return;
                    }
                }
            } else {
                if (cCharAt == '+') {
                    flushDecodingByteAccumulator(sb, charsetDecoderOnUnmappableCharacter, byteBufferAllocate, z2);
                    sb.append(z ? ' ' : '+');
                } else {
                    flushDecodingByteAccumulator(sb, charsetDecoderOnUnmappableCharacter, byteBufferAllocate, z2);
                    sb.append(cCharAt);
                }
            }
        }
        flushDecodingByteAccumulator(sb, charsetDecoderOnUnmappableCharacter, byteBufferAllocate, z2);
    }

    private static void flushDecodingByteAccumulator(StringBuilder sb, CharsetDecoder charsetDecoder, ByteBuffer byteBuffer, boolean z) {
        if (byteBuffer.position() == 0) {
            return;
        }
        byteBuffer.flip();
        try {
            try {
                sb.append((CharSequence) charsetDecoder.decode(byteBuffer));
            } catch (CharacterCodingException e) {
                if (z) {
                    throw new IllegalArgumentException(e);
                }
                sb.append(INVALID_INPUT_CHARACTER);
            }
        } finally {
            byteBuffer.flip();
            byteBuffer.limit(byteBuffer.capacity());
        }
    }

    public static String decode(String str) {
        return decode(str, false, StandardCharsets.UTF_8, true);
    }
}
