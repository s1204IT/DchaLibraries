package java.nio;

import java.io.IOException;
import java.util.Arrays;

public abstract class CharBuffer extends Buffer implements Comparable<CharBuffer>, CharSequence, Appendable, Readable {
    public abstract CharBuffer asReadOnlyBuffer();

    public abstract CharBuffer compact();

    public abstract CharBuffer duplicate();

    public abstract char get();

    public abstract char get(int i);

    @Override
    public abstract boolean isDirect();

    public abstract ByteOrder order();

    abstract char[] protectedArray();

    abstract int protectedArrayOffset();

    abstract boolean protectedHasArray();

    public abstract CharBuffer put(char c);

    public abstract CharBuffer put(int i, char c);

    public abstract CharBuffer slice();

    @Override
    public abstract CharBuffer subSequence(int i, int i2);

    public static CharBuffer allocate(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity < 0: " + capacity);
        }
        return new CharArrayBuffer(new char[capacity]);
    }

    public static CharBuffer wrap(char[] array) {
        return wrap(array, 0, array.length);
    }

    public static CharBuffer wrap(char[] array, int start, int charCount) {
        Arrays.checkOffsetAndCount(array.length, start, charCount);
        CharBuffer buf = new CharArrayBuffer(array);
        buf.position = start;
        buf.limit = start + charCount;
        return buf;
    }

    public static CharBuffer wrap(CharSequence chseq) {
        return new CharSequenceAdapter(chseq);
    }

    public static CharBuffer wrap(CharSequence cs, int start, int end) {
        if (start < 0 || end < start || end > cs.length()) {
            throw new IndexOutOfBoundsException("cs.length()=" + cs.length() + ", start=" + start + ", end=" + end);
        }
        CharBuffer result = new CharSequenceAdapter(cs);
        result.position = start;
        result.limit = end;
        return result;
    }

    CharBuffer(int capacity, long effectiveDirectAddress) {
        super(1, capacity, effectiveDirectAddress);
    }

    @Override
    public final char[] array() {
        return protectedArray();
    }

    @Override
    public final int arrayOffset() {
        return protectedArrayOffset();
    }

    @Override
    public final char charAt(int index) {
        if (index < 0 || index >= remaining()) {
            throw new IndexOutOfBoundsException("index=" + index + ", remaining()=" + remaining());
        }
        return get(this.position + index);
    }

    @Override
    public int compareTo(CharBuffer otherBuffer) {
        int thisPos = this.position;
        int otherPos = otherBuffer.position;
        for (int compareRemaining = remaining() < otherBuffer.remaining() ? remaining() : otherBuffer.remaining(); compareRemaining > 0; compareRemaining--) {
            char thisByte = get(thisPos);
            char otherByte = otherBuffer.get(otherPos);
            if (thisByte != otherByte) {
                return thisByte < otherByte ? -1 : 1;
            }
            thisPos++;
            otherPos++;
        }
        return remaining() - otherBuffer.remaining();
    }

    public boolean equals(Object other) {
        if (!(other instanceof CharBuffer)) {
            return false;
        }
        CharBuffer otherBuffer = (CharBuffer) other;
        if (remaining() != otherBuffer.remaining()) {
            return false;
        }
        int myPosition = this.position;
        int otherPosition = otherBuffer.position;
        boolean equalSoFar = true;
        int otherPosition2 = otherPosition;
        int myPosition2 = myPosition;
        while (equalSoFar && myPosition2 < this.limit) {
            int myPosition3 = myPosition2 + 1;
            int otherPosition3 = otherPosition2 + 1;
            equalSoFar = get(myPosition2) == otherBuffer.get(otherPosition2);
            otherPosition2 = otherPosition3;
            myPosition2 = myPosition3;
        }
        return equalSoFar;
    }

    public CharBuffer get(char[] dst) {
        return get(dst, 0, dst.length);
    }

    public CharBuffer get(char[] dst, int dstOffset, int charCount) {
        Arrays.checkOffsetAndCount(dst.length, dstOffset, charCount);
        if (charCount > remaining()) {
            throw new BufferUnderflowException();
        }
        for (int i = dstOffset; i < dstOffset + charCount; i++) {
            dst[i] = get();
        }
        return this;
    }

    @Override
    public final boolean hasArray() {
        return protectedHasArray();
    }

    public int hashCode() {
        int hash = 0;
        for (int myPosition = this.position; myPosition < this.limit; myPosition++) {
            hash += get(myPosition);
        }
        return hash;
    }

    @Override
    public final int length() {
        return remaining();
    }

    public final CharBuffer put(char[] src) {
        return put(src, 0, src.length);
    }

    public CharBuffer put(char[] src, int srcOffset, int charCount) {
        Arrays.checkOffsetAndCount(src.length, srcOffset, charCount);
        if (charCount > remaining()) {
            throw new BufferOverflowException();
        }
        for (int i = srcOffset; i < srcOffset + charCount; i++) {
            put(src[i]);
        }
        return this;
    }

    public CharBuffer put(CharBuffer src) {
        if (isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        if (src == this) {
            throw new IllegalArgumentException("src == this");
        }
        if (src.remaining() > remaining()) {
            throw new BufferOverflowException();
        }
        char[] contents = new char[src.remaining()];
        src.get(contents);
        put(contents);
        return this;
    }

    public final CharBuffer put(String str) {
        return put(str, 0, str.length());
    }

    public CharBuffer put(String str, int start, int end) {
        if (isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        if (start < 0 || end < start || end > str.length()) {
            throw new IndexOutOfBoundsException("str.length()=" + str.length() + ", start=" + start + ", end=" + end);
        }
        if (end - start > remaining()) {
            throw new BufferOverflowException();
        }
        for (int i = start; i < end; i++) {
            put(str.charAt(i));
        }
        return this;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(this.limit - this.position);
        for (int i = this.position; i < this.limit; i++) {
            result.append(get(i));
        }
        return result.toString();
    }

    @Override
    public CharBuffer append(char c) {
        return put(c);
    }

    @Override
    public CharBuffer append(CharSequence csq) {
        return csq != null ? put(csq.toString()) : put("null");
    }

    @Override
    public CharBuffer append(CharSequence csq, int start, int end) {
        if (csq == null) {
            csq = "null";
        }
        CharSequence cs = csq.subSequence(start, end);
        if (cs.length() > 0) {
            return put(cs.toString());
        }
        return this;
    }

    @Override
    public int read(CharBuffer target) throws IOException {
        int remaining = remaining();
        if (target == this) {
            if (remaining == 0) {
                return -1;
            }
            throw new IllegalArgumentException("target == this");
        }
        if (remaining == 0) {
            return (this.limit <= 0 || target.remaining() != 0) ? -1 : 0;
        }
        int remaining2 = Math.min(target.remaining(), remaining);
        if (remaining2 > 0) {
            char[] chars = new char[remaining2];
            get(chars);
            target.put(chars);
        }
        return remaining2;
    }
}
