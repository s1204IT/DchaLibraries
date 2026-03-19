package java.nio;

public abstract class DoubleBuffer extends Buffer implements Comparable<DoubleBuffer> {
    final double[] hb;
    boolean isReadOnly;
    final int offset;

    public abstract DoubleBuffer asReadOnlyBuffer();

    public abstract DoubleBuffer compact();

    public abstract DoubleBuffer duplicate();

    public abstract double get();

    public abstract double get(int i);

    @Override
    public abstract boolean isDirect();

    public abstract ByteOrder order();

    public abstract DoubleBuffer put(double d);

    public abstract DoubleBuffer put(int i, double d);

    public abstract DoubleBuffer slice();

    DoubleBuffer(int i, int i2, int i3, int i4, double[] dArr, int i5) {
        super(i, i2, i3, i4, 3);
        this.hb = dArr;
        this.offset = i5;
    }

    DoubleBuffer(int i, int i2, int i3, int i4) {
        this(i, i2, i3, i4, null, 0);
    }

    public static DoubleBuffer allocate(int i) {
        if (i < 0) {
            throw new IllegalArgumentException();
        }
        return new HeapDoubleBuffer(i, i);
    }

    public static DoubleBuffer wrap(double[] dArr, int i, int i2) {
        try {
            return new HeapDoubleBuffer(dArr, i, i2);
        } catch (IllegalArgumentException e) {
            throw new IndexOutOfBoundsException();
        }
    }

    public static DoubleBuffer wrap(double[] dArr) {
        return wrap(dArr, 0, dArr.length);
    }

    public DoubleBuffer get(double[] dArr, int i, int i2) {
        checkBounds(i, i2, dArr.length);
        if (i2 > remaining()) {
            throw new BufferUnderflowException();
        }
        int i3 = i2 + i;
        while (i < i3) {
            dArr[i] = get();
            i++;
        }
        return this;
    }

    public DoubleBuffer get(double[] dArr) {
        return get(dArr, 0, dArr.length);
    }

    public DoubleBuffer put(DoubleBuffer doubleBuffer) {
        if (doubleBuffer == this) {
            throw new IllegalArgumentException();
        }
        int iRemaining = doubleBuffer.remaining();
        if (iRemaining > remaining()) {
            throw new BufferOverflowException();
        }
        for (int i = 0; i < iRemaining; i++) {
            put(doubleBuffer.get());
        }
        return this;
    }

    public DoubleBuffer put(double[] dArr, int i, int i2) {
        checkBounds(i, i2, dArr.length);
        if (i2 > remaining()) {
            throw new BufferOverflowException();
        }
        int i3 = i2 + i;
        while (i < i3) {
            put(dArr[i]);
            i++;
        }
        return this;
    }

    public final DoubleBuffer put(double[] dArr) {
        return put(dArr, 0, dArr.length);
    }

    @Override
    public final boolean hasArray() {
        return (this.hb == null || this.isReadOnly) ? false : true;
    }

    @Override
    public final double[] array() {
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
        if (!(obj instanceof DoubleBuffer)) {
            return false;
        }
        DoubleBuffer doubleBuffer = (DoubleBuffer) obj;
        if (remaining() != doubleBuffer.remaining()) {
            return false;
        }
        int iPosition = position();
        int iLimit = limit() - 1;
        int iLimit2 = doubleBuffer.limit() - 1;
        while (iLimit >= iPosition) {
            if (!equals(get(iLimit), doubleBuffer.get(iLimit2))) {
                return false;
            }
            iLimit--;
            iLimit2--;
        }
        return true;
    }

    private static boolean equals(double d, double d2) {
        return d == d2 || (Double.isNaN(d) && Double.isNaN(d2));
    }

    @Override
    public int compareTo(DoubleBuffer doubleBuffer) {
        int iPosition = position() + Math.min(remaining(), doubleBuffer.remaining());
        int iPosition2 = position();
        int iPosition3 = doubleBuffer.position();
        while (iPosition2 < iPosition) {
            int iCompare = Double.compare(get(iPosition2), doubleBuffer.get(iPosition3));
            if (iCompare == 0) {
                iPosition2++;
                iPosition3++;
            } else {
                return iCompare;
            }
        }
        return remaining() - doubleBuffer.remaining();
    }

    private static int compare(double d, double d2) {
        if (d >= d2) {
            if (d <= d2) {
                if (d == d2) {
                    return 0;
                }
                if (Double.isNaN(d)) {
                    if (Double.isNaN(d2)) {
                        return 0;
                    }
                }
            }
            return 1;
        }
        return -1;
    }
}
