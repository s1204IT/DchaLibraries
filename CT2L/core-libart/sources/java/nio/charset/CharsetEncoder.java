package java.nio.charset;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Arrays;

public abstract class CharsetEncoder {
    private static final String END_OF_INPUT = "END_OF_INPUT";
    private static final String FLUSHED = "FLUSHED";
    private static final String ONGOING = "ONGOING";
    private static final String RESET = "RESET";
    private final float averageBytesPerChar;
    private final Charset charset;
    private CharsetDecoder decoder;
    private CodingErrorAction malformedInputAction;
    private final float maxBytesPerChar;
    private byte[] replacementBytes;
    private String state;
    private CodingErrorAction unmappableCharacterAction;

    protected abstract CoderResult encodeLoop(CharBuffer charBuffer, ByteBuffer byteBuffer);

    protected CharsetEncoder(Charset cs, float averageBytesPerChar, float maxBytesPerChar) {
        this(cs, averageBytesPerChar, maxBytesPerChar, new byte[]{63});
    }

    protected CharsetEncoder(Charset cs, float averageBytesPerChar, float maxBytesPerChar, byte[] replacement) {
        this(cs, averageBytesPerChar, maxBytesPerChar, replacement, false);
    }

    CharsetEncoder(Charset cs, float averageBytesPerChar, float maxBytesPerChar, byte[] replacement, boolean trusted) {
        this.state = RESET;
        this.malformedInputAction = CodingErrorAction.REPORT;
        this.unmappableCharacterAction = CodingErrorAction.REPORT;
        if (averageBytesPerChar <= 0.0f || maxBytesPerChar <= 0.0f) {
            throw new IllegalArgumentException("averageBytesPerChar and maxBytesPerChar must both be positive");
        }
        if (averageBytesPerChar > maxBytesPerChar) {
            throw new IllegalArgumentException("averageBytesPerChar is greater than maxBytesPerChar");
        }
        this.charset = cs;
        this.averageBytesPerChar = averageBytesPerChar;
        this.maxBytesPerChar = maxBytesPerChar;
        if (trusted) {
            this.replacementBytes = replacement;
        } else {
            replaceWith(replacement);
        }
    }

    public final float averageBytesPerChar() {
        return this.averageBytesPerChar;
    }

    public boolean canEncode(char c) {
        return canEncode(CharBuffer.wrap(new char[]{c}));
    }

    public boolean canEncode(CharSequence sequence) {
        CharBuffer cb;
        if (sequence instanceof CharBuffer) {
            cb = ((CharBuffer) sequence).duplicate();
        } else {
            cb = CharBuffer.wrap(sequence);
        }
        if (this.state == FLUSHED) {
            reset();
        }
        if (this.state != RESET) {
            throw illegalStateException();
        }
        CodingErrorAction originalMalformedInputAction = this.malformedInputAction;
        CodingErrorAction originalUnmappableCharacterAction = this.unmappableCharacterAction;
        onMalformedInput(CodingErrorAction.REPORT);
        onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            encode(cb);
            return true;
        } catch (CharacterCodingException e) {
            return false;
        } finally {
            onMalformedInput(originalMalformedInputAction);
            onUnmappableCharacter(originalUnmappableCharacterAction);
            reset();
        }
    }

    public final Charset charset() {
        return this.charset;
    }

    public final ByteBuffer encode(CharBuffer in) throws CharacterCodingException {
        int length = (int) (in.remaining() * this.averageBytesPerChar);
        ByteBuffer out = ByteBuffer.allocate(length);
        reset();
        while (this.state != FLUSHED) {
            CoderResult result = encode(in, out, true);
            if (result == CoderResult.OVERFLOW) {
                out = allocateMore(out);
            } else {
                checkCoderResult(result);
                CoderResult result2 = flush(out);
                if (result2 == CoderResult.OVERFLOW) {
                    out = allocateMore(out);
                } else {
                    checkCoderResult(result2);
                }
            }
        }
        out.flip();
        return out;
    }

    private void checkCoderResult(CoderResult result) throws CharacterCodingException {
        if (this.malformedInputAction == CodingErrorAction.REPORT && result.isMalformed()) {
            throw new MalformedInputException(result.length());
        }
        if (this.unmappableCharacterAction == CodingErrorAction.REPORT && result.isUnmappable()) {
            throw new UnmappableCharacterException(result.length());
        }
    }

    private ByteBuffer allocateMore(ByteBuffer output) {
        if (output.capacity() == 0) {
            return ByteBuffer.allocate(1);
        }
        ByteBuffer result = ByteBuffer.allocate(output.capacity() * 2);
        output.flip();
        result.put(output);
        return result;
    }

    public final CoderResult encode(CharBuffer in, ByteBuffer out, boolean endOfInput) {
        if (this.state != RESET && this.state != ONGOING && (!endOfInput || this.state != END_OF_INPUT)) {
            throw illegalStateException();
        }
        this.state = endOfInput ? END_OF_INPUT : ONGOING;
        while (true) {
            try {
                CoderResult result = encodeLoop(in, out);
                if (result == CoderResult.UNDERFLOW) {
                    if (!endOfInput || !in.hasRemaining()) {
                        break;
                    }
                    result = CoderResult.malformedForLength(in.remaining());
                } else if (result == CoderResult.OVERFLOW) {
                    return result;
                }
                CodingErrorAction action = result.isUnmappable() ? this.unmappableCharacterAction : this.malformedInputAction;
                if (action == CodingErrorAction.REPORT) {
                    return result;
                }
                if (action == CodingErrorAction.REPLACE) {
                    if (out.remaining() < this.replacementBytes.length) {
                        return CoderResult.OVERFLOW;
                    }
                    out.put(this.replacementBytes);
                }
                in.position(in.position() + result.length());
            } catch (BufferOverflowException ex) {
                throw new CoderMalfunctionError(ex);
            } catch (BufferUnderflowException ex2) {
                throw new CoderMalfunctionError(ex2);
            }
        }
    }

    public final CoderResult flush(ByteBuffer out) {
        if (this.state != FLUSHED && this.state != END_OF_INPUT) {
            throw illegalStateException();
        }
        CoderResult result = implFlush(out);
        if (result == CoderResult.UNDERFLOW) {
            this.state = FLUSHED;
        }
        return result;
    }

    protected CoderResult implFlush(ByteBuffer out) {
        return CoderResult.UNDERFLOW;
    }

    protected void implOnMalformedInput(CodingErrorAction newAction) {
    }

    protected void implOnUnmappableCharacter(CodingErrorAction newAction) {
    }

    protected void implReplaceWith(byte[] newReplacement) {
    }

    protected void implReset() {
    }

    public boolean isLegalReplacement(byte[] replacement) {
        if (this.decoder == null) {
            this.decoder = this.charset.newDecoder();
            this.decoder.onMalformedInput(CodingErrorAction.REPORT);
            this.decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
        }
        ByteBuffer in = ByteBuffer.wrap(replacement);
        CharBuffer out = CharBuffer.allocate((int) (replacement.length * this.decoder.maxCharsPerByte()));
        CoderResult result = this.decoder.decode(in, out, true);
        return !result.isError();
    }

    public CodingErrorAction malformedInputAction() {
        return this.malformedInputAction;
    }

    public final float maxBytesPerChar() {
        return this.maxBytesPerChar;
    }

    public final CharsetEncoder onMalformedInput(CodingErrorAction newAction) {
        if (newAction == null) {
            throw new IllegalArgumentException("newAction == null");
        }
        this.malformedInputAction = newAction;
        implOnMalformedInput(newAction);
        return this;
    }

    public final CharsetEncoder onUnmappableCharacter(CodingErrorAction newAction) {
        if (newAction == null) {
            throw new IllegalArgumentException("newAction == null");
        }
        this.unmappableCharacterAction = newAction;
        implOnUnmappableCharacter(newAction);
        return this;
    }

    public final byte[] replacement() {
        return this.replacementBytes;
    }

    public final CharsetEncoder replaceWith(byte[] replacement) {
        if (replacement == null) {
            throw new IllegalArgumentException("replacement == null");
        }
        if (replacement.length == 0) {
            throw new IllegalArgumentException("replacement.length == 0");
        }
        if (replacement.length > maxBytesPerChar()) {
            throw new IllegalArgumentException("replacement.length > maxBytesPerChar: " + replacement.length + " > " + maxBytesPerChar());
        }
        if (!isLegalReplacement(replacement)) {
            throw new IllegalArgumentException("Bad replacement: " + Arrays.toString(replacement));
        }
        this.replacementBytes = replacement;
        implReplaceWith(this.replacementBytes);
        return this;
    }

    public final CharsetEncoder reset() {
        this.state = RESET;
        implReset();
        return this;
    }

    public CodingErrorAction unmappableCharacterAction() {
        return this.unmappableCharacterAction;
    }

    private IllegalStateException illegalStateException() {
        throw new IllegalStateException("State: " + this.state);
    }
}
