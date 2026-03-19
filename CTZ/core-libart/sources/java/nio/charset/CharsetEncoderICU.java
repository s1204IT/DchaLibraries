package java.nio.charset;

import dalvik.annotation.optimization.ReachabilitySensitive;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.HashMap;
import java.util.Map;
import libcore.icu.ICU;
import libcore.icu.NativeConverter;
import libcore.util.EmptyArray;

final class CharsetEncoderICU extends CharsetEncoder {
    private static final Map<String, byte[]> DEFAULT_REPLACEMENTS = new HashMap();
    private static final int INPUT_OFFSET = 0;
    private static final int INVALID_CHAR_COUNT = 2;
    private static final int OUTPUT_OFFSET = 1;
    private char[] allocatedInput;
    private byte[] allocatedOutput;

    @ReachabilitySensitive
    private final long converterHandle;
    private int[] data;
    private int inEnd;
    private char[] input;
    private int outEnd;
    private byte[] output;

    static {
        byte[] bArr = {63};
        DEFAULT_REPLACEMENTS.put("UTF-8", bArr);
        DEFAULT_REPLACEMENTS.put("ISO-8859-1", bArr);
        DEFAULT_REPLACEMENTS.put("US-ASCII", bArr);
    }

    public static CharsetEncoderICU newInstance(Charset charset, String str) throws Throwable {
        long jOpenConverter;
        try {
            jOpenConverter = NativeConverter.openConverter(str);
            try {
                CharsetEncoderICU charsetEncoderICU = new CharsetEncoderICU(charset, NativeConverter.getAveBytesPerChar(jOpenConverter), NativeConverter.getMaxBytesPerChar(jOpenConverter), makeReplacement(str, jOpenConverter), jOpenConverter);
                NativeConverter.registerConverter(charsetEncoderICU, jOpenConverter);
                charsetEncoderICU.updateCallback();
                return charsetEncoderICU;
            } catch (Throwable th) {
                th = th;
                if (jOpenConverter != 0) {
                    NativeConverter.closeConverter(jOpenConverter);
                }
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
            jOpenConverter = 0;
        }
    }

    private static byte[] makeReplacement(String str, long j) {
        byte[] bArr = DEFAULT_REPLACEMENTS.get(str);
        if (bArr != null) {
            return (byte[]) bArr.clone();
        }
        return NativeConverter.getSubstitutionBytes(j);
    }

    private CharsetEncoderICU(Charset charset, float f, float f2, byte[] bArr, long j) {
        super(charset, f, f2, bArr, true);
        this.data = new int[3];
        this.input = null;
        this.output = null;
        this.allocatedInput = null;
        this.allocatedOutput = null;
        this.converterHandle = j;
    }

    @Override
    protected void implReplaceWith(byte[] bArr) {
        updateCallback();
    }

    @Override
    protected void implOnMalformedInput(CodingErrorAction codingErrorAction) {
        updateCallback();
    }

    @Override
    protected void implOnUnmappableCharacter(CodingErrorAction codingErrorAction) {
        updateCallback();
    }

    private void updateCallback() {
        NativeConverter.setCallbackEncode(this.converterHandle, this);
    }

    @Override
    protected void implReset() {
        NativeConverter.resetCharToByte(this.converterHandle);
        this.data[0] = 0;
        this.data[1] = 0;
        this.data[2] = 0;
        this.output = null;
        this.input = null;
        this.allocatedInput = null;
        this.allocatedOutput = null;
        this.inEnd = 0;
        this.outEnd = 0;
    }

    @Override
    protected CoderResult implFlush(ByteBuffer byteBuffer) {
        try {
            this.input = EmptyArray.CHAR;
            this.inEnd = 0;
            this.data[0] = 0;
            this.data[1] = getArray(byteBuffer);
            this.data[2] = 0;
            int iEncode = NativeConverter.encode(this.converterHandle, this.input, this.inEnd, this.output, this.outEnd, this.data, true);
            if (ICU.U_FAILURE(iEncode)) {
                if (iEncode == 15) {
                    return CoderResult.OVERFLOW;
                }
                if (iEncode == 11 && this.data[2] > 0) {
                    return CoderResult.malformedForLength(this.data[2]);
                }
            }
            return CoderResult.UNDERFLOW;
        } finally {
            setPosition(byteBuffer);
            implReset();
        }
    }

    @Override
    protected CoderResult encodeLoop(CharBuffer charBuffer, ByteBuffer byteBuffer) {
        if (!charBuffer.hasRemaining()) {
            return CoderResult.UNDERFLOW;
        }
        this.data[0] = getArray(charBuffer);
        this.data[1] = getArray(byteBuffer);
        this.data[2] = 0;
        try {
            int iEncode = NativeConverter.encode(this.converterHandle, this.input, this.inEnd, this.output, this.outEnd, this.data, false);
            if (!ICU.U_FAILURE(iEncode)) {
                return CoderResult.UNDERFLOW;
            }
            if (iEncode == 15) {
                return CoderResult.OVERFLOW;
            }
            if (iEncode == 10) {
                return CoderResult.unmappableForLength(this.data[2]);
            }
            if (iEncode == 12) {
                return CoderResult.malformedForLength(this.data[2]);
            }
            throw new AssertionError(iEncode);
        } finally {
            setPosition(charBuffer);
            setPosition(byteBuffer);
        }
    }

    private int getArray(ByteBuffer byteBuffer) {
        if (byteBuffer.hasArray()) {
            this.output = byteBuffer.array();
            this.outEnd = byteBuffer.arrayOffset() + byteBuffer.limit();
            return byteBuffer.arrayOffset() + byteBuffer.position();
        }
        this.outEnd = byteBuffer.remaining();
        if (this.allocatedOutput == null || this.outEnd > this.allocatedOutput.length) {
            this.allocatedOutput = new byte[this.outEnd];
        }
        this.output = this.allocatedOutput;
        return 0;
    }

    private int getArray(CharBuffer charBuffer) {
        if (charBuffer.hasArray()) {
            this.input = charBuffer.array();
            this.inEnd = charBuffer.arrayOffset() + charBuffer.limit();
            return charBuffer.arrayOffset() + charBuffer.position();
        }
        this.inEnd = charBuffer.remaining();
        if (this.allocatedInput == null || this.inEnd > this.allocatedInput.length) {
            this.allocatedInput = new char[this.inEnd];
        }
        int iPosition = charBuffer.position();
        charBuffer.get(this.allocatedInput, 0, this.inEnd);
        charBuffer.position(iPosition);
        this.input = this.allocatedInput;
        return 0;
    }

    private void setPosition(ByteBuffer byteBuffer) {
        if (byteBuffer.hasArray()) {
            byteBuffer.position(this.data[1] - byteBuffer.arrayOffset());
        } else {
            byteBuffer.put(this.output, 0, this.data[1]);
        }
        this.output = null;
    }

    private void setPosition(CharBuffer charBuffer) {
        int iPosition = (charBuffer.position() + this.data[0]) - this.data[2];
        if (iPosition < 0) {
            iPosition = 0;
        }
        charBuffer.position(iPosition);
        this.input = null;
    }
}
