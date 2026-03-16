package java.nio;

import java.util.Arrays;

final class CharSequenceAdapter extends CharBuffer {
    final CharSequence sequence;

    static CharSequenceAdapter copy(CharSequenceAdapter other) {
        CharSequenceAdapter buf = new CharSequenceAdapter(other.sequence);
        buf.limit = other.limit;
        buf.position = other.position;
        buf.mark = other.mark;
        return buf;
    }

    CharSequenceAdapter(CharSequence chseq) {
        super(chseq.length(), 0L);
        this.sequence = chseq;
    }

    @Override
    public CharBuffer asReadOnlyBuffer() {
        return duplicate();
    }

    @Override
    public CharBuffer compact() {
        throw new ReadOnlyBufferException();
    }

    @Override
    public CharBuffer duplicate() {
        return copy(this);
    }

    @Override
    public char get() {
        if (this.position == this.limit) {
            throw new BufferUnderflowException();
        }
        CharSequence charSequence = this.sequence;
        int i = this.position;
        this.position = i + 1;
        return charSequence.charAt(i);
    }

    @Override
    public char get(int index) {
        checkIndex(index);
        return this.sequence.charAt(index);
    }

    @Override
    public final CharBuffer get(char[] dst, int dstOffset, int charCount) {
        Arrays.checkOffsetAndCount(dst.length, dstOffset, charCount);
        if (charCount > remaining()) {
            throw new BufferUnderflowException();
        }
        int newPosition = this.position + charCount;
        this.sequence.toString().getChars(this.position, newPosition, dst, dstOffset);
        this.position = newPosition;
        return this;
    }

    @Override
    public boolean isDirect() {
        return false;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public ByteOrder order() {
        return ByteOrder.nativeOrder();
    }

    @Override
    char[] protectedArray() {
        throw new UnsupportedOperationException();
    }

    @Override
    int protectedArrayOffset() {
        throw new UnsupportedOperationException();
    }

    @Override
    boolean protectedHasArray() {
        return false;
    }

    @Override
    public CharBuffer put(char c) {
        throw new ReadOnlyBufferException();
    }

    @Override
    public CharBuffer put(int index, char c) {
        throw new ReadOnlyBufferException();
    }

    @Override
    public final CharBuffer put(char[] src, int srcOffset, int charCount) {
        throw new ReadOnlyBufferException();
    }

    @Override
    public CharBuffer put(String src, int start, int end) {
        throw new ReadOnlyBufferException();
    }

    @Override
    public CharBuffer slice() {
        return new CharSequenceAdapter(this.sequence.subSequence(this.position, this.limit));
    }

    @Override
    public CharBuffer subSequence(int start, int end) {
        checkStartEndRemaining(start, end);
        CharSequenceAdapter result = copy(this);
        result.position = this.position + start;
        result.limit = this.position + end;
        return result;
    }
}
