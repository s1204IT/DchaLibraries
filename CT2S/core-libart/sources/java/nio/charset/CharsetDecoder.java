package java.nio.charset;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

public abstract class CharsetDecoder {
    private static final String END_OF_INPUT = "END_OF_INPUT";
    private static final String FLUSHED = "FLUSHED";
    private static final String ONGOING = "ONGOING";
    private static final String RESET = "RESET";
    private final float averageCharsPerByte;
    private final Charset charset;
    private final float maxCharsPerByte;
    private String replacementChars = "�";
    private String state = RESET;
    private CodingErrorAction malformedInputAction = CodingErrorAction.REPORT;
    private CodingErrorAction unmappableCharacterAction = CodingErrorAction.REPORT;

    protected abstract CoderResult decodeLoop(ByteBuffer byteBuffer, CharBuffer charBuffer);

    protected CharsetDecoder(Charset charset, float averageCharsPerByte, float maxCharsPerByte) {
        if (averageCharsPerByte <= 0.0f || maxCharsPerByte <= 0.0f) {
            throw new IllegalArgumentException("averageCharsPerByte and maxCharsPerByte must be positive");
        }
        if (averageCharsPerByte > maxCharsPerByte) {
            throw new IllegalArgumentException("averageCharsPerByte is greater than maxCharsPerByte");
        }
        this.averageCharsPerByte = averageCharsPerByte;
        this.maxCharsPerByte = maxCharsPerByte;
        this.charset = charset;
    }

    public final float averageCharsPerByte() {
        return this.averageCharsPerByte;
    }

    public final Charset charset() {
        return this.charset;
    }

    public final CharBuffer decode(ByteBuffer in) throws CharacterCodingException {
        int length = (int) (in.remaining() * this.averageCharsPerByte);
        CharBuffer out = CharBuffer.allocate(length);
        reset();
        while (this.state != FLUSHED) {
            CoderResult result = decode(in, out, true);
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
        if (result.isMalformed() && this.malformedInputAction == CodingErrorAction.REPORT) {
            throw new MalformedInputException(result.length());
        }
        if (result.isUnmappable() && this.unmappableCharacterAction == CodingErrorAction.REPORT) {
            throw new UnmappableCharacterException(result.length());
        }
    }

    private CharBuffer allocateMore(CharBuffer output) {
        if (output.capacity() == 0) {
            return CharBuffer.allocate(1);
        }
        CharBuffer result = CharBuffer.allocate(output.capacity() * 2);
        output.flip();
        result.put(output);
        return result;
    }

    public final CoderResult decode(ByteBuffer in, CharBuffer out, boolean endOfInput) {
        if (this.state != RESET && this.state != ONGOING && (!endOfInput || this.state != END_OF_INPUT)) {
            throw illegalStateException();
        }
        this.state = endOfInput ? END_OF_INPUT : ONGOING;
        while (true) {
            try {
                CoderResult result = decodeLoop(in, out);
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
                    if (out.remaining() < this.replacementChars.length()) {
                        return CoderResult.OVERFLOW;
                    }
                    out.put(this.replacementChars);
                }
                in.position(in.position() + result.length());
            } catch (BufferOverflowException ex) {
                throw new CoderMalfunctionError(ex);
            } catch (BufferUnderflowException ex2) {
                throw new CoderMalfunctionError(ex2);
            }
        }
    }

    public Charset detectedCharset() {
        throw new UnsupportedOperationException();
    }

    public final CoderResult flush(CharBuffer out) {
        if (this.state != FLUSHED && this.state != END_OF_INPUT) {
            throw illegalStateException();
        }
        CoderResult result = implFlush(out);
        if (result == CoderResult.UNDERFLOW) {
            this.state = FLUSHED;
        }
        return result;
    }

    protected CoderResult implFlush(CharBuffer out) {
        return CoderResult.UNDERFLOW;
    }

    protected void implOnMalformedInput(CodingErrorAction newAction) {
    }

    protected void implOnUnmappableCharacter(CodingErrorAction newAction) {
    }

    protected void implReplaceWith(String newReplacement) {
    }

    protected void implReset() {
    }

    public boolean isAutoDetecting() {
        return false;
    }

    public boolean isCharsetDetected() {
        throw new UnsupportedOperationException();
    }

    public CodingErrorAction malformedInputAction() {
        return this.malformedInputAction;
    }

    public final float maxCharsPerByte() {
        return this.maxCharsPerByte;
    }

    public final CharsetDecoder onMalformedInput(CodingErrorAction newAction) {
        if (newAction == null) {
            throw new IllegalArgumentException("newAction == null");
        }
        this.malformedInputAction = newAction;
        implOnMalformedInput(newAction);
        return this;
    }

    public final CharsetDecoder onUnmappableCharacter(CodingErrorAction newAction) {
        if (newAction == null) {
            throw new IllegalArgumentException("newAction == null");
        }
        this.unmappableCharacterAction = newAction;
        implOnUnmappableCharacter(newAction);
        return this;
    }

    public final String replacement() {
        return this.replacementChars;
    }

    public final CharsetDecoder replaceWith(String replacement) {
        if (replacement == null) {
            throw new IllegalArgumentException("replacement == null");
        }
        if (replacement.isEmpty()) {
            throw new IllegalArgumentException("replacement.isEmpty()");
        }
        if (replacement.length() > maxCharsPerByte()) {
            throw new IllegalArgumentException("replacement length > maxCharsPerByte: " + replacement.length() + " > " + maxCharsPerByte());
        }
        this.replacementChars = replacement;
        implReplaceWith(replacement);
        return this;
    }

    public final CharsetDecoder reset() {
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
