package java.nio;

public abstract class LongBuffer extends Buffer implements Comparable<LongBuffer> {
    final long[] hb;
    boolean isReadOnly;
    final int offset;

    public abstract LongBuffer asReadOnlyBuffer();

    public abstract LongBuffer compact();

    public abstract LongBuffer duplicate();

    public abstract long get();

    public abstract long get(int i);

    @Override
    public abstract boolean isDirect();

    public abstract ByteOrder order();

    public abstract LongBuffer put(int i, long j);

    public abstract LongBuffer put(long j);

    public abstract LongBuffer slice();

    LongBuffer(int i, int i2, int i3, int i4, long[] jArr, int i5) {
        super(i, i2, i3, i4, 3);
        this.hb = jArr;
        this.offset = i5;
    }

    LongBuffer(int i, int i2, int i3, int i4) {
        this(i, i2, i3, i4, null, 0);
    }

    public static LongBuffer allocate(int i) {
        if (i < 0) {
            throw new IllegalArgumentException();
        }
        return new HeapLongBuffer(i, i);
    }

    public static LongBuffer wrap(long[] jArr, int i, int i2) {
        try {
            return new HeapLongBuffer(jArr, i, i2);
        } catch (IllegalArgumentException e) {
            throw new IndexOutOfBoundsException();
        }
    }

    public static LongBuffer wrap(long[] jArr) {
        return wrap(jArr, 0, jArr.length);
    }

    public LongBuffer get(long[] jArr, int i, int i2) {
        checkBounds(i, i2, jArr.length);
        if (i2 > remaining()) {
            throw new BufferUnderflowException();
        }
        int i3 = i2 + i;
        while (i < i3) {
            jArr[i] = get();
            i++;
        }
        return this;
    }

    public LongBuffer get(long[] jArr) {
        return get(jArr, 0, jArr.length);
    }

    public LongBuffer put(LongBuffer longBuffer) {
        if (longBuffer == this) {
            throw new IllegalArgumentException();
        }
        int iRemaining = longBuffer.remaining();
        if (iRemaining > remaining()) {
            throw new BufferOverflowException();
        }
        for (int i = 0; i < iRemaining; i++) {
            put(longBuffer.get());
        }
        return this;
    }

    public LongBuffer put(long[] jArr, int i, int i2) {
        checkBounds(i, i2, jArr.length);
        if (i2 > remaining()) {
            throw new BufferOverflowException();
        }
        int i3 = i2 + i;
        while (i < i3) {
            put(jArr[i]);
            i++;
        }
        return this;
    }

    public final LongBuffer put(long[] jArr) {
        return put(jArr, 0, jArr.length);
    }

    @Override
    public final boolean hasArray() {
        return (this.hb == null || this.isReadOnly) ? false : true;
    }

    @Override
    public final long[] array() {
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

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(getClass().getName());
        stringBuffer.append("[pos=");
        stringBuffer.append(position());
        stringBuffer.append(" lim=");
        stringBuffer.append(limit());
        stringBuffer.append(" cap=");
        stringBuffer.append(capacity());
        stringBuffer.append("]");
        return stringBuffer.toString();
    }

    public int hashCode() {
        int iPosition = position();
        int i = 1;
        for (int iLimit = limit() - 1; iLimit >= iPosition; iLimit--) {
            i = ((int) get(iLimit)) + (31 * i);
        }
        return i;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof LongBuffer)) {
            return false;
        }
        LongBuffer longBuffer = (LongBuffer) obj;
        if (remaining() != longBuffer.remaining()) {
            return false;
        }
        int iPosition = position();
        int iLimit = limit() - 1;
        int iLimit2 = longBuffer.limit() - 1;
        while (iLimit >= iPosition) {
            if (!equals(get(iLimit), longBuffer.get(iLimit2))) {
                return false;
            }
            iLimit--;
            iLimit2--;
        }
        return true;
    }

    private static boolean equals(long j, long j2) {
        return j == j2;
    }

    @Override
    public int compareTo(LongBuffer longBuffer) {
        int iPosition = position() + Math.min(remaining(), longBuffer.remaining());
        int iPosition2 = position();
        int iPosition3 = longBuffer.position();
        while (iPosition2 < iPosition) {
            int iCompare = compare(get(iPosition2), longBuffer.get(iPosition3));
            if (iCompare == 0) {
                iPosition2++;
                iPosition3++;
            } else {
                return iCompare;
            }
        }
        return remaining() - longBuffer.remaining();
    }

    private static int compare(long j, long j2) {
        return Long.compare(j, j2);
    }
}
