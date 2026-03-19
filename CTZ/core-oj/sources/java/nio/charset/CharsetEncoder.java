package java.nio.charset;

import java.lang.ref.WeakReference;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Arrays;

public abstract class CharsetEncoder {
    static final boolean $assertionsDisabled = false;
    private static final int ST_CODING = 1;
    private static final int ST_END = 2;
    private static final int ST_FLUSHED = 3;
    private static final int ST_RESET = 0;
    private static String[] stateNames = {"RESET", "CODING", "CODING_END", "FLUSHED"};
    private final float averageBytesPerChar;
    private WeakReference<CharsetDecoder> cachedDecoder;
    private final Charset charset;
    private CodingErrorAction malformedInputAction;
    private final float maxBytesPerChar;
    private byte[] replacement;
    private int state;
    private CodingErrorAction unmappableCharacterAction;

    protected abstract CoderResult encodeLoop(CharBuffer charBuffer, ByteBuffer byteBuffer);

    protected CharsetEncoder(Charset charset, float f, float f2, byte[] bArr) {
        this(charset, f, f2, bArr, $assertionsDisabled);
    }

    CharsetEncoder(Charset charset, float f, float f2, byte[] bArr, boolean z) {
        this.malformedInputAction = CodingErrorAction.REPORT;
        this.unmappableCharacterAction = CodingErrorAction.REPORT;
        this.state = 0;
        this.cachedDecoder = null;
        this.charset = charset;
        if (f <= 0.0f) {
            throw new IllegalArgumentException("Non-positive averageBytesPerChar");
        }
        if (f2 <= 0.0f) {
            throw new IllegalArgumentException("Non-positive maxBytesPerChar");
        }
        if (!Charset.atBugLevel("1.4") && f > f2) {
            throw new IllegalArgumentException("averageBytesPerChar exceeds maxBytesPerChar");
        }
        this.replacement = bArr;
        this.averageBytesPerChar = f;
        this.maxBytesPerChar = f2;
        if (!z) {
            replaceWith(bArr);
        }
    }

    protected CharsetEncoder(Charset charset, float f, float f2) {
        this(charset, f, f2, new byte[]{63});
    }

    public final Charset charset() {
        return this.charset;
    }

    public final byte[] replacement() {
        return Arrays.copyOf(this.replacement, this.replacement.length);
    }

    public final CharsetEncoder replaceWith(byte[] bArr) {
        if (bArr == null) {
            throw new IllegalArgumentException("Null replacement");
        }
        int length = bArr.length;
        if (length == 0) {
            throw new IllegalArgumentException("Empty replacement");
        }
        if (length > this.maxBytesPerChar) {
            throw new IllegalArgumentException("Replacement too long");
        }
        if (!isLegalReplacement(bArr)) {
            throw new IllegalArgumentException("Illegal replacement");
        }
        this.replacement = Arrays.copyOf(bArr, bArr.length);
        implReplaceWith(this.replacement);
        return this;
    }

    protected void implReplaceWith(byte[] bArr) {
    }

    public boolean isLegalReplacement(byte[] bArr) {
        CharsetDecoder charsetDecoderNewDecoder;
        WeakReference<CharsetDecoder> weakReference = this.cachedDecoder;
        if (weakReference == null || (charsetDecoderNewDecoder = weakReference.get()) == null) {
            charsetDecoderNewDecoder = charset().newDecoder();
            charsetDecoderNewDecoder.onMalformedInput(CodingErrorAction.REPORT);
            charsetDecoderNewDecoder.onUnmappableCharacter(CodingErrorAction.REPORT);
            this.cachedDecoder = new WeakReference<>(charsetDecoderNewDecoder);
        } else {
            charsetDecoderNewDecoder.reset();
        }
        ByteBuffer byteBufferWrap = ByteBuffer.wrap(bArr);
        return !charsetDecoderNewDecoder.decode(byteBufferWrap, CharBuffer.allocate((int) (byteBufferWrap.remaining() * charsetDecoderNewDecoder.maxCharsPerByte())), true).isError();
    }

    public CodingErrorAction malformedInputAction() {
        return this.malformedInputAction;
    }

    public final CharsetEncoder onMalformedInput(CodingErrorAction codingErrorAction) {
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

    public final CharsetEncoder onUnmappableCharacter(CodingErrorAction codingErrorAction) {
        if (codingErrorAction == null) {
            throw new IllegalArgumentException("Null action");
        }
        this.unmappableCharacterAction = codingErrorAction;
        implOnUnmappableCharacter(codingErrorAction);
        return this;
    }

    protected void implOnUnmappableCharacter(CodingErrorAction codingErrorAction) {
    }

    public final float averageBytesPerChar() {
        return this.averageBytesPerChar;
    }

    public final float maxBytesPerChar() {
        return this.maxBytesPerChar;
    }

    public final CoderResult encode(CharBuffer charBuffer, ByteBuffer byteBuffer, boolean z) {
        int i = z ? 2 : 1;
        if (this.state != 0 && this.state != 1 && (!z || this.state != 2)) {
            throwIllegalStateException(this.state, i);
        }
        this.state = i;
        while (true) {
            try {
                CoderResult coderResultEncodeLoop = encodeLoop(charBuffer, byteBuffer);
                if (coderResultEncodeLoop.isOverflow()) {
                    return coderResultEncodeLoop;
                }
                if (coderResultEncodeLoop.isUnderflow()) {
                    if (!z || !charBuffer.hasRemaining()) {
                        break;
                    }
                    coderResultEncodeLoop = CoderResult.malformedForLength(charBuffer.remaining());
                }
                CodingErrorAction codingErrorAction = null;
                if (coderResultEncodeLoop.isMalformed()) {
                    codingErrorAction = this.malformedInputAction;
                } else if (coderResultEncodeLoop.isUnmappable()) {
                    codingErrorAction = this.unmappableCharacterAction;
                }
                if (codingErrorAction == CodingErrorAction.REPORT) {
                    return coderResultEncodeLoop;
                }
                if (codingErrorAction == CodingErrorAction.REPLACE) {
                    if (byteBuffer.remaining() < this.replacement.length) {
                        return CoderResult.OVERFLOW;
                    }
                    byteBuffer.put(this.replacement);
                }
                if (codingErrorAction == CodingErrorAction.IGNORE || codingErrorAction == CodingErrorAction.REPLACE) {
                    charBuffer.position(charBuffer.position() + coderResultEncodeLoop.length());
                }
            } catch (BufferOverflowException e) {
                throw new CoderMalfunctionError(e);
            } catch (BufferUnderflowException e2) {
                throw new CoderMalfunctionError(e2);
            }
        }
    }

    public final CoderResult flush(ByteBuffer byteBuffer) {
        if (this.state == 2) {
            CoderResult coderResultImplFlush = implFlush(byteBuffer);
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

    protected CoderResult implFlush(ByteBuffer byteBuffer) {
        return CoderResult.UNDERFLOW;
    }

    public final CharsetEncoder reset() {
        implReset();
        this.state = 0;
        return this;
    }

    protected void implReset() {
    }

    public final ByteBuffer encode(CharBuffer charBuffer) throws CharacterCodingException {
        int iRemaining = (int) (charBuffer.remaining() * averageBytesPerChar());
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(iRemaining);
        if (iRemaining == 0 && charBuffer.remaining() == 0) {
            return byteBufferAllocate;
        }
        reset();
        while (true) {
            CoderResult coderResultEncode = charBuffer.hasRemaining() ? encode(charBuffer, byteBufferAllocate, true) : CoderResult.UNDERFLOW;
            if (coderResultEncode.isUnderflow()) {
                coderResultEncode = flush(byteBufferAllocate);
            }
            if (!coderResultEncode.isUnderflow()) {
                if (coderResultEncode.isOverflow()) {
                    iRemaining = (2 * iRemaining) + 1;
                    ByteBuffer byteBufferAllocate2 = ByteBuffer.allocate(iRemaining);
                    byteBufferAllocate.flip();
                    byteBufferAllocate2.put(byteBufferAllocate);
                    byteBufferAllocate = byteBufferAllocate2;
                } else {
                    coderResultEncode.throwException();
                }
            } else {
                byteBufferAllocate.flip();
                return byteBufferAllocate;
            }
        }
    }

    private boolean canEncode(CharBuffer charBuffer) {
        if (this.state == 3) {
            reset();
        } else if (this.state != 0) {
            throwIllegalStateException(this.state, 1);
        }
        if (!charBuffer.hasRemaining()) {
            return true;
        }
        CodingErrorAction codingErrorActionMalformedInputAction = malformedInputAction();
        CodingErrorAction codingErrorActionUnmappableCharacterAction = unmappableCharacterAction();
        try {
            onMalformedInput(CodingErrorAction.REPORT);
            onUnmappableCharacter(CodingErrorAction.REPORT);
            encode(charBuffer);
            return true;
        } catch (CharacterCodingException e) {
            return $assertionsDisabled;
        } finally {
            onMalformedInput(codingErrorActionMalformedInputAction);
            onUnmappableCharacter(codingErrorActionUnmappableCharacterAction);
            reset();
        }
    }

    public boolean canEncode(char c) {
        CharBuffer charBufferAllocate = CharBuffer.allocate(1);
        charBufferAllocate.put(c);
        charBufferAllocate.flip();
        return canEncode(charBufferAllocate);
    }

    public boolean canEncode(CharSequence charSequence) {
        CharBuffer charBufferWrap;
        if (charSequence instanceof CharBuffer) {
            charBufferWrap = ((CharBuffer) charSequence).duplicate();
        } else {
            charBufferWrap = CharBuffer.wrap(charSequence);
        }
        return canEncode(charBufferWrap);
    }

    private void throwIllegalStateException(int i, int i2) {
        throw new IllegalStateException("Current state = " + stateNames[i] + ", new state = " + stateNames[i2]);
    }
}
