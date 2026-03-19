package java.nio;

public abstract class IntBuffer extends Buffer implements Comparable<IntBuffer> {
    final int[] hb;
    boolean isReadOnly;
    final int offset;

    public abstract IntBuffer asReadOnlyBuffer();

    public abstract IntBuffer compact();

    public abstract IntBuffer duplicate();

    public abstract int get();

    public abstract int get(int i);

    @Override
    public abstract boolean isDirect();

    public abstract ByteOrder order();

    public abstract IntBuffer put(int i);

    public abstract IntBuffer put(int i, int i2);

    public abstract IntBuffer slice();

    IntBuffer(int i, int i2, int i3, int i4, int[] iArr, int i5) {
        super(i, i2, i3, i4, 2);
        this.hb = iArr;
        this.offset = i5;
    }

    IntBuffer(int i, int i2, int i3, int i4) {
        this(i, i2, i3, i4, null, 0);
    }

    public static IntBuffer allocate(int i) {
        if (i < 0) {
            throw new IllegalArgumentException();
        }
        return new HeapIntBuffer(i, i);
    }

    public static IntBuffer wrap(int[] iArr, int i, int i2) {
        try {
            return new HeapIntBuffer(iArr, i, i2);
        } catch (IllegalArgumentException e) {
            throw new IndexOutOfBoundsException();
        }
    }

    public static IntBuffer wrap(int[] iArr) {
        return wrap(iArr, 0, iArr.length);
    }

    public IntBuffer get(int[] iArr, int i, int i2) {
        checkBounds(i, i2, iArr.length);
        if (i2 > remaining()) {
            throw new BufferUnderflowException();
        }
        int i3 = i2 + i;
        while (i < i3) {
            iArr[i] = get();
            i++;
        }
        return this;
    }

    public IntBuffer get(int[] iArr) {
        return get(iArr, 0, iArr.length);
    }

    public IntBuffer put(IntBuffer intBuffer) {
        if (intBuffer == this) {
            throw new IllegalArgumentException();
        }
        int iRemaining = intBuffer.remaining();
        if (iRemaining > remaining()) {
            throw new BufferOverflowException();
        }
        for (int i = 0; i < iRemaining; i++) {
            put(intBuffer.get());
        }
        return this;
    }

    public IntBuffer put(int[] iArr, int i, int i2) {
        checkBounds(i, i2, iArr.length);
        if (i2 > remaining()) {
            throw new BufferOverflowException();
        }
        int i3 = i2 + i;
        while (i < i3) {
            put(iArr[i]);
            i++;
        }
        return this;
    }

    public final IntBuffer put(int[] iArr) {
        return put(iArr, 0, iArr.length);
    }

    @Override
    public final boolean hasArray() {
        return (this.hb == null || this.isReadOnly) ? false : true;
    }

    @Override
    public final int[] array() {
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
        if (!(obj instanceof IntBuffer)) {
            return false;
        }
        IntBuffer intBuffer = (IntBuffer) obj;
        if (remaining() != intBuffer.remaining()) {
            return false;
        }
        int iPosition = position();
        int iLimit = limit() - 1;
        int iLimit2 = intBuffer.limit() - 1;
        while (iLimit >= iPosition) {
            if (!equals(get(iLimit), intBuffer.get(iLimit2))) {
                return false;
            }
            iLimit--;
            iLimit2--;
        }
        return true;
    }

    private static boolean equals(int i, int i2) {
        return i == i2;
    }

    @Override
    public int compareTo(IntBuffer intBuffer) {
        int iPosition = position() + Math.min(remaining(), intBuffer.remaining());
        int iPosition2 = position();
        int iPosition3 = intBuffer.position();
        while (iPosition2 < iPosition) {
            int iCompare = compare(get(iPosition2), intBuffer.get(iPosition3));
            if (iCompare == 0) {
                iPosition2++;
                iPosition3++;
            } else {
                return iCompare;
            }
        }
        return remaining() - intBuffer.remaining();
    }

    private static int compare(int i, int i2) {
        return Integer.compare(i, i2);
    }
}
