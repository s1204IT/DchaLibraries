package java.nio;

public abstract class ShortBuffer extends Buffer implements Comparable<ShortBuffer> {
    final short[] hb;
    boolean isReadOnly;
    final int offset;

    public abstract ShortBuffer asReadOnlyBuffer();

    public abstract ShortBuffer compact();

    public abstract ShortBuffer duplicate();

    public abstract short get();

    public abstract short get(int i);

    @Override
    public abstract boolean isDirect();

    public abstract ByteOrder order();

    public abstract ShortBuffer put(int i, short s);

    public abstract ShortBuffer put(short s);

    public abstract ShortBuffer slice();

    ShortBuffer(int i, int i2, int i3, int i4, short[] sArr, int i5) {
        super(i, i2, i3, i4, 1);
        this.hb = sArr;
        this.offset = i5;
    }

    ShortBuffer(int i, int i2, int i3, int i4) {
        this(i, i2, i3, i4, null, 0);
    }

    public static ShortBuffer allocate(int i) {
        if (i < 0) {
            throw new IllegalArgumentException();
        }
        return new HeapShortBuffer(i, i);
    }

    public static ShortBuffer wrap(short[] sArr, int i, int i2) {
        try {
            return new HeapShortBuffer(sArr, i, i2);
        } catch (IllegalArgumentException e) {
            throw new IndexOutOfBoundsException();
        }
    }

    public static ShortBuffer wrap(short[] sArr) {
        return wrap(sArr, 0, sArr.length);
    }

    public ShortBuffer get(short[] sArr, int i, int i2) {
        checkBounds(i, i2, sArr.length);
        if (i2 > remaining()) {
            throw new BufferUnderflowException();
        }
        int i3 = i2 + i;
        while (i < i3) {
            sArr[i] = get();
            i++;
        }
        return this;
    }

    public ShortBuffer get(short[] sArr) {
        return get(sArr, 0, sArr.length);
    }

    public ShortBuffer put(ShortBuffer shortBuffer) {
        if (shortBuffer == this) {
            throw new IllegalArgumentException();
        }
        int iRemaining = shortBuffer.remaining();
        if (iRemaining > remaining()) {
            throw new BufferOverflowException();
        }
        for (int i = 0; i < iRemaining; i++) {
            put(shortBuffer.get());
        }
        return this;
    }

    public ShortBuffer put(short[] sArr, int i, int i2) {
        checkBounds(i, i2, sArr.length);
        if (i2 > remaining()) {
            throw new BufferOverflowException();
        }
        int i3 = i2 + i;
        while (i < i3) {
            put(sArr[i]);
            i++;
        }
        return this;
    }

    public final ShortBuffer put(short[] sArr) {
        return put(sArr, 0, sArr.length);
    }

    @Override
    public final boolean hasArray() {
        return (this.hb == null || this.isReadOnly) ? false : true;
    }

    @Override
    public final short[] array() {
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
            i = get(iLimit) + (31 * i);
        }
        return i;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ShortBuffer)) {
            return false;
        }
        ShortBuffer shortBuffer = (ShortBuffer) obj;
        if (remaining() != shortBuffer.remaining()) {
            return false;
        }
        int iPosition = position();
        int iLimit = limit() - 1;
        int iLimit2 = shortBuffer.limit() - 1;
        while (iLimit >= iPosition) {
            if (!equals(get(iLimit), shortBuffer.get(iLimit2))) {
                return false;
            }
            iLimit--;
            iLimit2--;
        }
        return true;
    }

    private static boolean equals(short s, short s2) {
        return s == s2;
    }

    @Override
    public int compareTo(ShortBuffer shortBuffer) {
        int iPosition = position() + Math.min(remaining(), shortBuffer.remaining());
        int iPosition2 = position();
        int iPosition3 = shortBuffer.position();
        while (iPosition2 < iPosition) {
            int iCompare = compare(get(iPosition2), shortBuffer.get(iPosition3));
            if (iCompare == 0) {
                iPosition2++;
                iPosition3++;
            } else {
                return iCompare;
            }
        }
        return remaining() - shortBuffer.remaining();
    }

    private static int compare(short s, short s2) {
        return Short.compare(s, s2);
    }
}
