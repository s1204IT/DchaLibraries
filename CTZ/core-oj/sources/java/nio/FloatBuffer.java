package java.nio;

public abstract class FloatBuffer extends Buffer implements Comparable<FloatBuffer> {
    final float[] hb;
    boolean isReadOnly;
    final int offset;

    public abstract FloatBuffer asReadOnlyBuffer();

    public abstract FloatBuffer compact();

    public abstract FloatBuffer duplicate();

    public abstract float get();

    public abstract float get(int i);

    @Override
    public abstract boolean isDirect();

    public abstract ByteOrder order();

    public abstract FloatBuffer put(float f);

    public abstract FloatBuffer put(int i, float f);

    public abstract FloatBuffer slice();

    FloatBuffer(int i, int i2, int i3, int i4, float[] fArr, int i5) {
        super(i, i2, i3, i4, 2);
        this.hb = fArr;
        this.offset = i5;
    }

    FloatBuffer(int i, int i2, int i3, int i4) {
        this(i, i2, i3, i4, null, 0);
    }

    public static FloatBuffer allocate(int i) {
        if (i < 0) {
            throw new IllegalArgumentException();
        }
        return new HeapFloatBuffer(i, i);
    }

    public static FloatBuffer wrap(float[] fArr, int i, int i2) {
        try {
            return new HeapFloatBuffer(fArr, i, i2);
        } catch (IllegalArgumentException e) {
            throw new IndexOutOfBoundsException();
        }
    }

    public static FloatBuffer wrap(float[] fArr) {
        return wrap(fArr, 0, fArr.length);
    }

    public FloatBuffer get(float[] fArr, int i, int i2) {
        checkBounds(i, i2, fArr.length);
        if (i2 > remaining()) {
            throw new BufferUnderflowException();
        }
        int i3 = i2 + i;
        while (i < i3) {
            fArr[i] = get();
            i++;
        }
        return this;
    }

    public FloatBuffer get(float[] fArr) {
        return get(fArr, 0, fArr.length);
    }

    public FloatBuffer put(FloatBuffer floatBuffer) {
        if (floatBuffer == this) {
            throw new IllegalArgumentException();
        }
        int iRemaining = floatBuffer.remaining();
        if (iRemaining > remaining()) {
            throw new BufferOverflowException();
        }
        for (int i = 0; i < iRemaining; i++) {
            put(floatBuffer.get());
        }
        return this;
    }

    public FloatBuffer put(float[] fArr, int i, int i2) {
        checkBounds(i, i2, fArr.length);
        if (i2 > remaining()) {
            throw new BufferOverflowException();
        }
        int i3 = i2 + i;
        while (i < i3) {
            put(fArr[i]);
            i++;
        }
        return this;
    }

    public final FloatBuffer put(float[] fArr) {
        return put(fArr, 0, fArr.length);
    }

    @Override
    public final boolean hasArray() {
        return (this.hb == null || this.isReadOnly) ? false : true;
    }

    @Override
    public final float[] array() {
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
        if (!(obj instanceof FloatBuffer)) {
            return false;
        }
        FloatBuffer floatBuffer = (FloatBuffer) obj;
        if (remaining() != floatBuffer.remaining()) {
            return false;
        }
        int iPosition = position();
        int iLimit = limit() - 1;
        int iLimit2 = floatBuffer.limit() - 1;
        while (iLimit >= iPosition) {
            if (!equals(get(iLimit), floatBuffer.get(iLimit2))) {
                return false;
            }
            iLimit--;
            iLimit2--;
        }
        return true;
    }

    private static boolean equals(float f, float f2) {
        return f == f2 || (Float.isNaN(f) && Float.isNaN(f2));
    }

    @Override
    public int compareTo(FloatBuffer floatBuffer) {
        int iPosition = position() + Math.min(remaining(), floatBuffer.remaining());
        int iPosition2 = position();
        int iPosition3 = floatBuffer.position();
        while (iPosition2 < iPosition) {
            int iCompare = compare(get(iPosition2), floatBuffer.get(iPosition3));
            if (iCompare == 0) {
                iPosition2++;
                iPosition3++;
            } else {
                return iCompare;
            }
        }
        return remaining() - floatBuffer.remaining();
    }

    private static int compare(float f, float f2) {
        if (f >= f2) {
            if (f <= f2) {
                if (f == f2) {
                    return 0;
                }
                if (Float.isNaN(f)) {
                    if (Float.isNaN(f2)) {
                        return 0;
                    }
                }
            }
            return 1;
        }
        return -1;
    }
}
