package java.nio.charset;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

public abstract class CharsetDecoder {
    static final boolean $assertionsDisabled = false;
    private static final int ST_CODING = 1;
    private static final int ST_END = 2;
    private static final int ST_FLUSHED = 3;
    private static final int ST_RESET = 0;
    private static String[] stateNames = {"RESET", "CODING", "CODING_END", "FLUSHED"};
    private final float averageCharsPerByte;
    private final Charset charset;
    private CodingErrorAction malformedInputAction;
    private final float maxCharsPerByte;
    private String replacement;
    private int state;
    private CodingErrorAction unmappableCharacterAction;

    protected abstract CoderResult decodeLoop(ByteBuffer byteBuffer, CharBuffer charBuffer);

    private CharsetDecoder(Charset charset, float f, float f2, String str) {
        this.malformedInputAction = CodingErrorAction.REPORT;
        this.unmappableCharacterAction = CodingErrorAction.REPORT;
        this.state = 0;
        this.charset = charset;
        if (f <= 0.0f) {
            throw new IllegalArgumentException("Non-positive averageCharsPerByte");
        }
        if (f2 <= 0.0f) {
            throw new IllegalArgumentException("Non-positive maxCharsPerByte");
        }
        if (!Charset.atBugLevel("1.4") && f > f2) {
            throw new IllegalArgumentException("averageCharsPerByte exceeds maxCharsPerByte");
        }
        this.replacement = str;
        this.averageCharsPerByte = f;
        this.maxCharsPerByte = f2;
    }

    protected CharsetDecoder(Charset charset, float f, float f2) {
        this(charset, f, f2, "�");
    }

    public final Charset charset() {
        return this.charset;
    }

    public final String replacement() {
        return this.replacement;
    }

    public final CharsetDecoder replaceWith(String str) {
        if (str == null) {
            throw new IllegalArgumentException("Null replacement");
        }
        int length = str.length();
        if (length == 0) {
            throw new IllegalArgumentException("Empty replacement");
        }
        if (length > this.maxCharsPerByte) {
            throw new IllegalArgumentException("Replacement too long");
        }
        this.replacement = str;
        implReplaceWith(this.replacement);
        return this;
    }

    protected void implReplaceWith(String str) {
    }

    public CodingErrorAction malformedInputAction() {
        return this.malformedInputAction;
    }

    public final CharsetDecoder onMalformedInput(CodingErrorAction codingErrorAction) {
        if (codingErrorAction == null) {
            throw new IllegalArgumentException("Null action");
        }
        this.malformedInputAction = codingErrorAction;
        implOnMalformedInput(codingErrorAction);
        return this;
    }

    protected void implOnMalformedInput(CodingErrorAction codingErrorAction) {
    }

    public CodingErrorAction unmappableCharacterAction() {
        return this.unmappableCharacterAction;
    }

    public final CharsetDecoder onUnmappableCharacter(CodingErrorAction codingErrorAction) {
        if (codingErrorAction == null) {
            throw new IllegalArgumentException("Null action");
        }
        this.unmappableCharacterAction = codingErrorAction;
        implOnUnmappableCharacter(codingErrorAction);
        return this;
    }

    protected void implOnUnmappableCharacter(CodingErrorAction codingErrorAction) {
    }

    public final float averageCharsPerByte() {
        return this.averageCharsPerByte;
    }

    public final float maxCharsPerByte() {
        return this.maxCharsPerByte;
    }

    public final CoderResult decode(ByteBuffer byteBuffer, CharBuffer charBuffer, boolean z) {
        int i = z ? 2 : 1;
        if (this.state != 0 && this.state != 1 && (!z || this.state != 2)) {
            throwIllegalStateException(this.state, i);
        }
        this.state = i;
        while (true) {
            try {
                CoderResult coderResultDecodeLoop = decodeLoop(byteBuffer, charBuffer);
                if (coderResultDecodeLoop.isOverflow()) {
                    return coderResultDecodeLoop;
                }
                if (coderResultDecodeLoop.isUnderflow()) {
                    if (!z || !byteBuffer.hasRemaining()) {
                        break;
                    }
                    coderResultDecodeLoop = CoderResult.malformedForLength(byteBuffer.remaining());
                }
                CodingErrorAction codingErrorAction = null;
                if (coderResultDecodeLoop.isMalformed()) {
                    codingErrorAction = this.malformedInputAction;
                } else if (coderResultDecodeLoop.isUnmappable()) {
                    codingErrorAction = this.unmappableCharacterAction;
                }
                if (codingErrorAction == CodingErrorAction.REPORT) {
                    return coderResultDecodeLoop;
                }
                if (codingErrorAction == CodingErrorAction.REPLACE) {
                    if (charBuffer.remaining() < this.replacement.length()) {
                        return CoderResult.OVERFLOW;
                    }
                    charBuffer.put(this.replacement);
                }
                if (codingErrorAction == CodingErrorAction.IGNORE || codingErrorAction == CodingErrorAction.REPLACE) {
                    byteBuffer.position(byteBuffer.position() + coderResultDecodeLoop.length());
                }
            } catch (BufferOverflowException e) {
                throw new CoderMalfunctionError(e);
            } catch (BufferUnderflowException e2) {
                throw new CoderMalfunctionError(e2);
            }
        }
    }

    public final CoderResult flush(CharBuffer charBuffer) {
        if (this.state == 2) {
            CoderResult coderResultImplFlush = implFlush(charBuffer);
            if (coderResultImplFlush.isUnderflow()) {
                this.state = 3;
            }
            return coderResultImplFlush;
        }
        if (this.state != 3) {
            throwIllegalStateException(this.state, 3);
        }
        return CoderResult.UNDERFLOW;
    }

    protected CoderResult implFlush(CharBuffer charBuffer) {
        return CoderResult.UNDERFLOW;
    }

    public final CharsetDecoder reset() {
        implReset();
        this.state = 0;
        return this;
    }

    protected void implReset() {
    }

    public final CharBuffer decode(ByteBuffer byteBuffer) throws CharacterCodingException {
        int iRemaining = (int) (byteBuffer.remaining() * averageCharsPerByte());
        CharBuffer charBufferAllocate = CharBuffer.allocate(iRemaining);
        if (iRemaining == 0 && byteBuffer.remaining() == 0) {
            return charBufferAllocate;
        }
        reset();
        while (true) {
            CoderResult coderResultDecode = byteBuffer.hasRemaining() ? decode(byteBuffer, charBufferAllocate, true) : CoderResult.UNDERFLOW;
            if (coderResultDecode.isUnderflow()) {
                coderResultDecode = flush(charBufferAllocate);
            }
            if (!coderResultDecode.isUnderflow()) {
                if (coderResultDecode.isOverflow()) {
                    iRemaining = (2 * iRemaining) + 1;
                    CharBuffer charBufferAllocate2 = CharBuffer.allocate(iRemaining);
                    charBufferAllocate.flip();
                    charBufferAllocate2.put(charBufferAllocate);
                    charBufferAllocate = charBufferAllocate2;
                } else {
                    coderResultDecode.throwException();
                }
            } else {
                charBufferAllocate.flip();
                return charBufferAllocate;
            }
        }
    }

    public boolean isAutoDetecting() {
        return $assertionsDisabled;
    }

    public boolean isCharsetDetected() {
        throw new UnsupportedOperationException();
    }

    public Charset detectedCharset() {
        throw new UnsupportedOperationException();
    }

    private void throwIllegalStateException(int i, int i2) {
        throw new IllegalStateException("Current state = " + stateNames[i] + ", new state = " + stateNames[i2]);
    }
}
