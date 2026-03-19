package java.nio;

import java.io.IOException;
import java.util.Spliterator;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

public abstract class CharBuffer extends Buffer implements Comparable<CharBuffer>, Appendable, CharSequence, Readable {
    final char[] hb;
    boolean isReadOnly;
    final int offset;

    public abstract CharBuffer asReadOnlyBuffer();

    public abstract CharBuffer compact();

    public abstract CharBuffer duplicate();

    public abstract char get();

    public abstract char get(int i);

    abstract char getUnchecked(int i);

    @Override
    public abstract boolean isDirect();

    public abstract ByteOrder order();

    public abstract CharBuffer put(char c);

    public abstract CharBuffer put(int i, char c);

    public abstract CharBuffer slice();

    @Override
    public abstract CharBuffer subSequence(int i, int i2);

    abstract String toString(int i, int i2);

    CharBuffer(int i, int i2, int i3, int i4, char[] cArr, int i5) {
        super(i, i2, i3, i4, 1);
        this.hb = cArr;
        this.offset = i5;
    }

    CharBuffer(int i, int i2, int i3, int i4) {
        this(i, i2, i3, i4, null, 0);
    }

    public static CharBuffer allocate(int i) {
        if (i < 0) {
            throw new IllegalArgumentException();
        }
        return new HeapCharBuffer(i, i);
    }

    public static CharBuffer wrap(char[] cArr, int i, int i2) {
        try {
            return new HeapCharBuffer(cArr, i, i2);
        } catch (IllegalArgumentException e) {
            throw new IndexOutOfBoundsException();
        }
    }

    public static CharBuffer wrap(char[] cArr) {
        return wrap(cArr, 0, cArr.length);
    }

    @Override
    public int read(CharBuffer charBuffer) throws IOException {
        int iRemaining = charBuffer.remaining();
        int iRemaining2 = remaining();
        if (iRemaining2 == 0) {
            return -1;
        }
        int iMin = Math.min(iRemaining2, iRemaining);
        int iLimit = limit();
        if (iRemaining < iRemaining2) {
            limit(position() + iMin);
        }
        if (iMin > 0) {
            try {
                charBuffer.put(this);
            } finally {
                limit(iLimit);
            }
        }
        return iMin;
    }

    public static CharBuffer wrap(CharSequence charSequence, int i, int i2) {
        try {
            return new StringCharBuffer(charSequence, i, i2);
        } catch (IllegalArgumentException e) {
            throw new IndexOutOfBoundsException();
        }
    }

    public static CharBuffer wrap(CharSequence charSequence) {
        return wrap(charSequence, 0, charSequence.length());
    }

    public CharBuffer get(char[] cArr, int i, int i2) {
        checkBounds(i, i2, cArr.length);
        if (i2 > remaining()) {
            throw new BufferUnderflowException();
        }
        int i3 = i2 + i;
        while (i < i3) {
            cArr[i] = get();
            i++;
        }
        return this;
    }

    public CharBuffer get(char[] cArr) {
        return get(cArr, 0, cArr.length);
    }

    public CharBuffer put(CharBuffer charBuffer) {
        if (charBuffer == this) {
            throw new IllegalArgumentException();
        }
        int iRemaining = charBuffer.remaining();
        if (iRemaining > remaining()) {
            throw new BufferOverflowException();
        }
        for (int i = 0; i < iRemaining; i++) {
            put(charBuffer.get());
        }
        return this;
    }

    public CharBuffer put(char[] cArr, int i, int i2) {
        checkBounds(i, i2, cArr.length);
        if (i2 > remaining()) {
            throw new BufferOverflowException();
        }
        int i3 = i2 + i;
        while (i < i3) {
            put(cArr[i]);
            i++;
        }
        return this;
    }

    public final CharBuffer put(char[] cArr) {
        return put(cArr, 0, cArr.length);
    }

    public CharBuffer put(String str, int i, int i2) {
        int i3 = i2 - i;
        checkBounds(i, i3, str.length());
        if (i == i2) {
            return this;
        }
        if (isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        if (i3 > remaining()) {
            throw new BufferOverflowException();
        }
        while (i < i2) {
            put(str.charAt(i));
            i++;
        }
        return this;
    }

    public final CharBuffer put(String str) {
        return put(str, 0, str.length());
    }

    @Override
    public final boolean hasArray() {
        return (this.hb == null || this.isReadOnly) ? false : true;
    }

    @Override
    public final char[] array() {
        if (this.hb == null) {
            throw new UnsupportedOperationException();
        }
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        return this.hb;
    }

    @Override
    public final int arrayOffset() {
        if (this.hb == null) {
            throw new UnsupportedOperationException();
        }
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        return this.offset;
    }

    public int hashCode() {
        int iPosition = position();
        int i = 1;
        for (int iLimit = limit() - 1; iLimit >= iPosition; iLimit--) {
            i = get(iLimit) + (31 * i);
        }
        return i;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CharBuffer)) {
            return false;
        }
        CharBuffer charBuffer = (CharBuffer) obj;
        if (remaining() != charBuffer.remaining()) {
            return false;
        }
        int iPosition = position();
        int iLimit = limit() - 1;
        int iLimit2 = charBuffer.limit() - 1;
        while (iLimit >= iPosition) {
            if (!equals(get(iLimit), charBuffer.get(iLimit2))) {
                return false;
            }
            iLimit--;
            iLimit2--;
        }
        return true;
    }

    private static boolean equals(char c, char c2) {
        return c == c2;
    }

    @Override
    public int compareTo(CharBuffer charBuffer) {
        int iPosition = position() + Math.min(remaining(), charBuffer.remaining());
        int iPosition2 = position();
        int iPosition3 = charBuffer.position();
        while (iPosition2 < iPosition) {
            int iCompare = compare(get(iPosition2), charBuffer.get(iPosition3));
            if (iCompare == 0) {
                iPosition2++;
                iPosition3++;
            } else {
                return iCompare;
            }
        }
        return remaining() - charBuffer.remaining();
    }

    private static int compare(char c, char c2) {
        return Character.compare(c, c2);
    }

    @Override
    public String toString() {
        return toString(position(), limit());
    }

    @Override
    public final int length() {
        return remaining();
    }

    @Override
    public final char charAt(int i) {
        return get(position() + checkIndex(i, 1));
    }

    @Override
    public CharBuffer append(CharSequence charSequence) {
        if (charSequence == null) {
            return put("null");
        }
        return put(charSequence.toString());
    }

    @Override
    public CharBuffer append(CharSequence charSequence, int i, int i2) {
        if (charSequence == null) {
            charSequence = "null";
        }
        return put(charSequence.subSequence(i, i2).toString());
    }

    @Override
    public CharBuffer append(char c) {
        return put(c);
    }

    @Override
    public IntStream chars() {
        return StreamSupport.intStream(new Supplier() {
            @Override
            public final Object get() {
                return CharBuffer.lambda$chars$0(this.f$0);
            }
        }, 16464, false);
    }

    static Spliterator.OfInt lambda$chars$0(CharBuffer charBuffer) {
        return new CharBufferSpliterator(charBuffer);
    }
}
