package java.nio;

import java.nio.DirectByteBuffer;
import libcore.io.Memory;

public abstract class ByteBuffer extends Buffer implements Comparable<ByteBuffer> {
    boolean bigEndian;
    final byte[] hb;
    boolean isReadOnly;
    boolean nativeByteOrder;
    final int offset;

    abstract byte _get(int i);

    abstract void _put(int i, byte b);

    public abstract CharBuffer asCharBuffer();

    public abstract DoubleBuffer asDoubleBuffer();

    public abstract FloatBuffer asFloatBuffer();

    public abstract IntBuffer asIntBuffer();

    public abstract LongBuffer asLongBuffer();

    public abstract ByteBuffer asReadOnlyBuffer();

    public abstract ShortBuffer asShortBuffer();

    public abstract ByteBuffer compact();

    public abstract ByteBuffer duplicate();

    public abstract byte get();

    public abstract byte get(int i);

    public abstract char getChar();

    public abstract char getChar(int i);

    public abstract double getDouble();

    public abstract double getDouble(int i);

    public abstract float getFloat();

    public abstract float getFloat(int i);

    public abstract int getInt();

    public abstract int getInt(int i);

    public abstract long getLong();

    public abstract long getLong(int i);

    public abstract short getShort();

    public abstract short getShort(int i);

    @Override
    public abstract boolean isDirect();

    public abstract ByteBuffer put(byte b);

    public abstract ByteBuffer put(int i, byte b);

    public abstract ByteBuffer putChar(char c);

    public abstract ByteBuffer putChar(int i, char c);

    public abstract ByteBuffer putDouble(double d);

    public abstract ByteBuffer putDouble(int i, double d);

    public abstract ByteBuffer putFloat(float f);

    public abstract ByteBuffer putFloat(int i, float f);

    public abstract ByteBuffer putInt(int i);

    public abstract ByteBuffer putInt(int i, int i2);

    public abstract ByteBuffer putLong(int i, long j);

    public abstract ByteBuffer putLong(long j);

    public abstract ByteBuffer putShort(int i, short s);

    public abstract ByteBuffer putShort(short s);

    public abstract ByteBuffer slice();

    ByteBuffer(int i, int i2, int i3, int i4, byte[] bArr, int i5) {
        super(i, i2, i3, i4, 0);
        this.bigEndian = true;
        this.nativeByteOrder = Bits.byteOrder() == ByteOrder.BIG_ENDIAN;
        this.hb = bArr;
        this.offset = i5;
    }

    ByteBuffer(int i, int i2, int i3, int i4) {
        this(i, i2, i3, i4, null, 0);
    }

    public static ByteBuffer allocateDirect(int i) {
        if (i < 0) {
            throw new IllegalArgumentException("capacity < 0: " + i);
        }
        return new DirectByteBuffer(i, new DirectByteBuffer.MemoryRef(i));
    }

    public static ByteBuffer allocate(int i) {
        if (i < 0) {
            throw new IllegalArgumentException();
        }
        return new HeapByteBuffer(i, i);
    }

    public static ByteBuffer wrap(byte[] bArr, int i, int i2) {
        try {
            return new HeapByteBuffer(bArr, i, i2);
        } catch (IllegalArgumentException e) {
            throw new IndexOutOfBoundsException();
        }
    }

    public static ByteBuffer wrap(byte[] bArr) {
        return wrap(bArr, 0, bArr.length);
    }

    public ByteBuffer get(byte[] bArr, int i, int i2) {
        checkBounds(i, i2, bArr.length);
        if (i2 > remaining()) {
            throw new BufferUnderflowException();
        }
        int i3 = i2 + i;
        while (i < i3) {
            bArr[i] = get();
            i++;
        }
        return this;
    }

    public ByteBuffer get(byte[] bArr) {
        return get(bArr, 0, bArr.length);
    }

    public ByteBuffer put(ByteBuffer byteBuffer) {
        Object obj;
        Object obj2;
        if (!isAccessible()) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        if (byteBuffer == this) {
            throw new IllegalArgumentException();
        }
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        int iRemaining = byteBuffer.remaining();
        if (iRemaining > remaining()) {
            throw new BufferOverflowException();
        }
        if (this.hb != null && byteBuffer.hb != null) {
            System.arraycopy(byteBuffer.hb, byteBuffer.position() + byteBuffer.offset, this.hb, position() + this.offset, iRemaining);
        } else {
            if (!byteBuffer.isDirect()) {
                obj = byteBuffer.hb;
            } else {
                obj = byteBuffer;
            }
            int iPosition = byteBuffer.position();
            if (!byteBuffer.isDirect()) {
                iPosition += byteBuffer.offset;
            }
            int i = iPosition;
            if (!isDirect()) {
                obj2 = this.hb;
            } else {
                obj2 = this;
            }
            int iPosition2 = position();
            if (!isDirect()) {
                iPosition2 += this.offset;
            }
            Memory.memmove(obj2, iPosition2, obj, i, iRemaining);
        }
        byteBuffer.position(byteBuffer.limit());
        position(position() + iRemaining);
        return this;
    }

    public ByteBuffer put(byte[] bArr, int i, int i2) {
        checkBounds(i, i2, bArr.length);
        if (i2 > remaining()) {
            throw new BufferOverflowException();
        }
        int i3 = i2 + i;
        while (i < i3) {
            put(bArr[i]);
            i++;
        }
        return this;
    }

    public final ByteBuffer put(byte[] bArr) {
        return put(bArr, 0, bArr.length);
    }

    @Override
    public final boolean hasArray() {
        return (this.hb == null || this.isReadOnly) ? false : true;
    }

    @Override
    public final byte[] array() {
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
        if (!(obj instanceof ByteBuffer)) {
            return false;
        }
        ByteBuffer byteBuffer = (ByteBuffer) obj;
        if (remaining() != byteBuffer.remaining()) {
            return false;
        }
        int iPosition = position();
        int iLimit = limit() - 1;
        int iLimit2 = byteBuffer.limit() - 1;
        while (iLimit >= iPosition) {
            if (!equals(get(iLimit), byteBuffer.get(iLimit2))) {
                return false;
            }
            iLimit--;
            iLimit2--;
        }
        return true;
    }

    private static boolean equals(byte b, byte b2) {
        return b == b2;
    }

    @Override
    public int compareTo(ByteBuffer byteBuffer) {
        int iPosition = position() + Math.min(remaining(), byteBuffer.remaining());
        int iPosition2 = position();
        int iPosition3 = byteBuffer.position();
        while (iPosition2 < iPosition) {
            int iCompare = compare(get(iPosition2), byteBuffer.get(iPosition3));
            if (iCompare == 0) {
                iPosition2++;
                iPosition3++;
            } else {
                return iCompare;
            }
        }
        return remaining() - byteBuffer.remaining();
    }

    private static int compare(byte b, byte b2) {
        return Byte.compare(b, b2);
    }

    public final ByteOrder order() {
        return this.bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
    }

    public final ByteBuffer order(ByteOrder byteOrder) {
        this.bigEndian = byteOrder == ByteOrder.BIG_ENDIAN;
        this.nativeByteOrder = this.bigEndian == (Bits.byteOrder() == ByteOrder.BIG_ENDIAN);
        return this;
    }

    char getCharUnchecked(int i) {
        throw new UnsupportedOperationException();
    }

    void getUnchecked(int i, char[] cArr, int i2, int i3) {
        throw new UnsupportedOperationException();
    }

    void putCharUnchecked(int i, char c) {
        throw new UnsupportedOperationException();
    }

    void putUnchecked(int i, char[] cArr, int i2, int i3) {
        throw new UnsupportedOperationException();
    }

    short getShortUnchecked(int i) {
        throw new UnsupportedOperationException();
    }

    void getUnchecked(int i, short[] sArr, int i2, int i3) {
        throw new UnsupportedOperationException();
    }

    void putShortUnchecked(int i, short s) {
        throw new UnsupportedOperationException();
    }

    void putUnchecked(int i, short[] sArr, int i2, int i3) {
        throw new UnsupportedOperationException();
    }

    int getIntUnchecked(int i) {
        throw new UnsupportedOperationException();
    }

    void getUnchecked(int i, int[] iArr, int i2, int i3) {
        throw new UnsupportedOperationException();
    }

    void putIntUnchecked(int i, int i2) {
        throw new UnsupportedOperationException();
    }

    void putUnchecked(int i, int[] iArr, int i2, int i3) {
        throw new UnsupportedOperationException();
    }

    long getLongUnchecked(int i) {
        throw new UnsupportedOperationException();
    }

    void getUnchecked(int i, long[] jArr, int i2, int i3) {
        throw new UnsupportedOperationException();
    }

    void putLongUnchecked(int i, long j) {
        throw new UnsupportedOperationException();
    }

    void putUnchecked(int i, long[] jArr, int i2, int i3) {
        throw new UnsupportedOperationException();
    }

    float getFloatUnchecked(int i) {
        throw new UnsupportedOperationException();
    }

    void getUnchecked(int i, float[] fArr, int i2, int i3) {
        throw new UnsupportedOperationException();
    }

    void putFloatUnchecked(int i, float f) {
        throw new UnsupportedOperationException();
    }

    void putUnchecked(int i, float[] fArr, int i2, int i3) {
        throw new UnsupportedOperationException();
    }

    double getDoubleUnchecked(int i) {
        throw new UnsupportedOperationException();
    }

    void getUnchecked(int i, double[] dArr, int i2, int i3) {
        throw new UnsupportedOperationException();
    }

    void putDoubleUnchecked(int i, double d) {
        throw new UnsupportedOperationException();
    }

    void putUnchecked(int i, double[] dArr, int i2, int i3) {
        throw new UnsupportedOperationException();
    }

    public boolean isAccessible() {
        return true;
    }

    public void setAccessible(boolean z) {
        throw new UnsupportedOperationException();
    }
}
