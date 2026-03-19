package java.nio.charset;

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
    private long converterHandle;
    private final int[] data;
    private int inEnd;
    private byte[] input;
    private int outEnd;
    private char[] output;

    public static CharsetDecoderICU newInstance(Charset cs, String icuCanonicalName) {
        long address = 0;
        try {
            long address2 = NativeConverter.openConverter(icuCanonicalName);
            float averageCharsPerByte = NativeConverter.getAveCharsPerByte(address2);
            CharsetDecoderICU result = new CharsetDecoderICU(cs, averageCharsPerByte, address2);
            address = 0;
            result.updateCallback();
            return result;
        } catch (Throwable th) {
            if (address != 0) {
                NativeConverter.closeConverter(address);
            }
            throw th;
        }
    }

    private CharsetDecoderICU(Charset cs, float averageCharsPerByte, long address) {
        super(cs, averageCharsPerByte, 2.0f);
        this.data = new int[3];
        this.converterHandle = 0L;
        this.input = null;
        this.output = null;
        this.allocatedInput = null;
        this.allocatedOutput = null;
        this.converterHandle = address;
        NativeConverter.registerConverter(this, this.converterHandle);
    }

    @Override
    protected void implReplaceWith(String newReplacement) {
        updateCallback();
    }

    @Override
    protected final void implOnMalformedInput(CodingErrorAction newAction) {
        updateCallback();
    }

    @Override
    protected final void implOnUnmappableCharacter(CodingErrorAction newAction) {
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
    protected final CoderResult implFlush(CharBuffer out) {
        try {
            this.input = EmptyArray.BYTE;
            this.inEnd = 0;
            this.data[0] = 0;
            this.data[1] = getArray(out);
            this.data[2] = 0;
            int error = NativeConverter.decode(this.converterHandle, this.input, this.inEnd, this.output, this.outEnd, this.data, true);
            if (ICU.U_FAILURE(error)) {
                if (error == 15) {
                    return CoderResult.OVERFLOW;
                }
                if (error == 11 && this.data[2] > 0) {
                    return CoderResult.malformedForLength(this.data[2]);
                }
            }
            return CoderResult.UNDERFLOW;
        } finally {
            setPosition(out);
            implReset();
        }
    }

    @Override
    protected CoderResult decodeLoop(ByteBuffer in, CharBuffer out) {
        if (!in.hasRemaining()) {
            return CoderResult.UNDERFLOW;
        }
        this.data[0] = getArray(in);
        this.data[1] = getArray(out);
        try {
            int error = NativeConverter.decode(this.converterHandle, this.input, this.inEnd, this.output, this.outEnd, this.data, false);
            if (!ICU.U_FAILURE(error)) {
                return CoderResult.UNDERFLOW;
            }
            if (error == 15) {
                return CoderResult.OVERFLOW;
            }
            if (error == 10) {
                return CoderResult.unmappableForLength(this.data[2]);
            }
            if (error == 12) {
                return CoderResult.malformedForLength(this.data[2]);
            }
            throw new AssertionError(error);
        } finally {
            setPosition(in);
            setPosition(out);
        }
    }

    private int getArray(CharBuffer out) {
        if (out.hasArray()) {
            this.output = out.array();
            this.outEnd = out.arrayOffset() + out.limit();
            return out.arrayOffset() + out.position();
        }
        this.outEnd = out.remaining();
        if (this.allocatedOutput == null || this.outEnd > this.allocatedOutput.length) {
            this.allocatedOutput = new char[this.outEnd];
        }
        this.output = this.allocatedOutput;
        return 0;
    }

    private int getArray(ByteBuffer in) {
        if (in.hasArray()) {
            this.input = in.array();
            this.inEnd = in.arrayOffset() + in.limit();
            return in.arrayOffset() + in.position();
        }
        this.inEnd = in.remaining();
        if (this.allocatedInput == null || this.inEnd > this.allocatedInput.length) {
            this.allocatedInput = new byte[this.inEnd];
        }
        int pos = in.position();
        in.get(this.allocatedInput, 0, this.inEnd);
        in.position(pos);
        this.input = this.allocatedInput;
        return 0;
    }

    private void setPosition(CharBuffer out) {
        if (out.hasArray()) {
            out.position(out.position() + this.data[1]);
        } else {
            out.put(this.output, 0, this.data[1]);
        }
        this.output = null;
    }

    private void setPosition(ByteBuffer in) {
        in.position(in.position() + this.data[0]);
        this.input = null;
    }
}
