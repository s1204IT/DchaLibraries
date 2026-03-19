package java.nio.charset;

import dalvik.annotation.optimization.ReachabilitySensitive;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import libcore.icu.ICU;
import libcore.icu.NativeConverter;
import libcore.util.EmptyArray;

final class CharsetDecoderICU extends CharsetDecoder {
    private static final int INPUT_OFFSET = 0;
    private static final int INVALID_BYTE_COUNT = 2;
    private static final int MAX_CHARS_PER_BYTE = 2;
    private static final int OUTPUT_OFFSET = 1;
    private byte[] allocatedInput;
    private char[] allocatedOutput;

    @ReachabilitySensitive
    private long converterHandle;
    private final int[] data;
    private int inEnd;
    private byte[] input;
    private int outEnd;
    private char[] output;

    public static CharsetDecoderICU newInstance(Charset charset, String str) throws Throwable {
        long jOpenConverter;
        try {
            jOpenConverter = NativeConverter.openConverter(str);
        } catch (Throwable th) {
            th = th;
            jOpenConverter = 0;
        }
        try {
            CharsetDecoderICU charsetDecoderICU = new CharsetDecoderICU(charset, NativeConverter.getAveCharsPerByte(jOpenConverter), jOpenConverter);
            NativeConverter.registerConverter(charsetDecoderICU, jOpenConverter);
            charsetDecoderICU.updateCallback();
            return charsetDecoderICU;
        } catch (Throwable th2) {
            th = th2;
            if (jOpenConverter != 0) {
                NativeConverter.closeConverter(jOpenConverter);
            }
            throw th;
        }
    }

    private CharsetDecoderICU(Charset charset, float f, long j) {
        super(charset, f, 2.0f);
        this.data = new int[3];
        this.converterHandle = 0L;
        this.input = null;
        this.output = null;
        this.allocatedInput = null;
        this.allocatedOutput = null;
        this.converterHandle = j;
    }

    @Override
    protected void implReplaceWith(String str) {
        updateCallback();
    }

    @Override
    protected final void implOnMalformedInput(CodingErrorAction codingErrorAction) {
        updateCallback();
    }

    @Override
    protected final void implOnUnmappableCharacter(CodingErrorAction codingErrorAction) {
        updateCallback();
    }

    private void updateCallback() {
        NativeConverter.setCallbackDecode(this.converterHandle, this);
    }

    @Override
    protected void implReset() {
        NativeConverter.resetByteToChar(this.converterHandle);
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
    protected final CoderResult implFlush(CharBuffer charBuffer) {
        try {
            this.input = EmptyArray.BYTE;
            this.inEnd = 0;
            this.data[0] = 0;
            this.data[1] = getArray(charBuffer);
            this.data[2] = 0;
            int iDecode = NativeConverter.decode(this.converterHandle, this.input, this.inEnd, this.output, this.outEnd, this.data, true);
            if (ICU.U_FAILURE(iDecode)) {
                if (iDecode == 15) {
                    return CoderResult.OVERFLOW;
                }
                if (iDecode == 11 && this.data[2] > 0) {
                    return CoderResult.malformedForLength(this.data[2]);
                }
            }
            return CoderResult.UNDERFLOW;
        } finally {
            setPosition(charBuffer);
            implReset();
        }
    }

    @Override
    protected CoderResult decodeLoop(ByteBuffer byteBuffer, CharBuffer charBuffer) {
        if (!byteBuffer.hasRemaining()) {
            return CoderResult.UNDERFLOW;
        }
        this.data[0] = getArray(byteBuffer);
        this.data[1] = getArray(charBuffer);
        try {
            int iDecode = NativeConverter.decode(this.converterHandle, this.input, this.inEnd, this.output, this.outEnd, this.data, false);
            if (!ICU.U_FAILURE(iDecode)) {
                return CoderResult.UNDERFLOW;
            }
            if (iDecode == 15) {
                return CoderResult.OVERFLOW;
            }
            if (iDecode == 10) {
                return CoderResult.unmappableForLength(this.data[2]);
            }
            if (iDecode == 12) {
                return CoderResult.malformedForLength(this.data[2]);
            }
            throw new AssertionError(iDecode);
        } finally {
            setPosition(byteBuffer);
            setPosition(charBuffer);
        }
    }

    private int getArray(CharBuffer charBuffer) {
        if (charBuffer.hasArray()) {
            this.output = charBuffer.array();
            this.outEnd = charBuffer.arrayOffset() + charBuffer.limit();
            return charBuffer.arrayOffset() + charBuffer.position();
        }
        this.outEnd = charBuffer.remaining();
        if (this.allocatedOutput == null || this.outEnd > this.allocatedOutput.length) {
            this.allocatedOutput = new char[this.outEnd];
        }
        this.output = this.allocatedOutput;
        return 0;
    }

    private int getArray(ByteBuffer byteBuffer) {
        if (byteBuffer.hasArray()) {
            this.input = byteBuffer.array();
            this.inEnd = byteBuffer.arrayOffset() + byteBuffer.limit();
            return byteBuffer.arrayOffset() + byteBuffer.position();
        }
        this.inEnd = byteBuffer.remaining();
        if (this.allocatedInput == null || this.inEnd > this.allocatedInput.length) {
            this.allocatedInput = new byte[this.inEnd];
        }
        int iPosition = byteBuffer.position();
        byteBuffer.get(this.allocatedInput, 0, this.inEnd);
        byteBuffer.position(iPosition);
        this.input = this.allocatedInput;
        return 0;
    }

    private void setPosition(CharBuffer charBuffer) {
        if (charBuffer.hasArray()) {
            charBuffer.position(charBuffer.position() + this.data[1]);
        } else {
            charBuffer.put(this.output, 0, this.data[1]);
        }
        this.output = null;
    }

    private void setPosition(ByteBuffer byteBuffer) {
        byteBuffer.position(byteBuffer.position() + this.data[0]);
        this.input = null;
    }
}
