package java.nio;

import libcore.io.Memory;

final class HeapByteBuffer extends ByteBuffer {
    HeapByteBuffer(int i, int i2) {
        this(i, i2, false);
    }

    private HeapByteBuffer(int i, int i2, boolean z) {
        super(-1, 0, i2, i, new byte[i], 0);
        this.isReadOnly = z;
    }

    HeapByteBuffer(byte[] bArr, int i, int i2) {
        this(bArr, i, i2, false);
    }

    private HeapByteBuffer(byte[] bArr, int i, int i2, boolean z) {
        super(-1, i, i + i2, bArr.length, bArr, 0);
        this.isReadOnly = z;
    }

    private HeapByteBuffer(byte[] bArr, int i, int i2, int i3, int i4, int i5, boolean z) {
        super(i, i2, i3, i4, bArr, i5);
        this.isReadOnly = z;
    }

    @Override
    public ByteBuffer slice() {
        return new HeapByteBuffer(this.hb, -1, 0, remaining(), remaining(), position() + this.offset, this.isReadOnly);
    }

    @Override
    public ByteBuffer duplicate() {
        return new HeapByteBuffer(this.hb, markValue(), position(), limit(), capacity(), this.offset, this.isReadOnly);
    }

    @Override
    public ByteBuffer asReadOnlyBuffer() {
        return new HeapByteBuffer(this.hb, markValue(), position(), limit(), capacity(), this.offset, true);
    }

    protected int ix(int i) {
        return i + this.offset;
    }

    @Override
    public byte get() {
        return this.hb[ix(nextGetIndex())];
    }

    @Override
    public byte get(int i) {
        return this.hb[ix(checkIndex(i))];
    }

    @Override
    public ByteBuffer get(byte[] bArr, int i, int i2) {
        checkBounds(i, i2, bArr.length);
        if (i2 > remaining()) {
            throw new BufferUnderflowException();
        }
        System.arraycopy(this.hb, ix(position()), bArr, i, i2);
        position(position() + i2);
        return this;
    }

    @Override
    public boolean isDirect() {
        return false;
    }

    @Override
    public boolean isReadOnly() {
        return this.isReadOnly;
    }

    @Override
    public ByteBuffer put(byte b) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        this.hb[ix(nextPutIndex())] = b;
        return this;
    }

    @Override
    public ByteBuffer put(int i, byte b) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        this.hb[ix(checkIndex(i))] = b;
        return this;
    }

    @Override
    public ByteBuffer put(byte[] bArr, int i, int i2) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        checkBounds(i, i2, bArr.length);
        if (i2 > remaining()) {
            throw new BufferOverflowException();
        }
        System.arraycopy(bArr, i, this.hb, ix(position()), i2);
        position(position() + i2);
        return this;
    }

    @Override
    public ByteBuffer compact() {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        System.arraycopy(this.hb, ix(position()), this.hb, ix(0), remaining());
        position(remaining());
        limit(capacity());
        discardMark();
        return this;
    }

    @Override
    byte _get(int i) {
        return this.hb[i];
    }

    @Override
    void _put(int i, byte b) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        this.hb[i] = b;
    }

    @Override
    public char getChar() {
        return Bits.getChar(this, ix(nextGetIndex(2)), this.bigEndian);
    }

    @Override
    public char getChar(int i) {
        return Bits.getChar(this, ix(checkIndex(i, 2)), this.bigEndian);
    }

    @Override
    char getCharUnchecked(int i) {
        return Bits.getChar(this, ix(i), this.bigEndian);
    }

    @Override
    void getUnchecked(int i, char[] cArr, int i2, int i3) {
        Memory.unsafeBulkGet(cArr, i2, i3 * 2, this.hb, ix(i), 2, !this.nativeByteOrder);
    }

    @Override
    public ByteBuffer putChar(char c) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        Bits.putChar(this, ix(nextPutIndex(2)), c, this.bigEndian);
        return this;
    }

    @Override
    public ByteBuffer putChar(int i, char c) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        Bits.putChar(this, ix(checkIndex(i, 2)), c, this.bigEndian);
        return this;
    }

    @Override
    void putCharUnchecked(int i, char c) {
        Bits.putChar(this, ix(i), c, this.bigEndian);
    }

    @Override
    void putUnchecked(int i, char[] cArr, int i2, int i3) {
        Memory.unsafeBulkPut(this.hb, ix(i), i3 * 2, cArr, i2, 2, !this.nativeByteOrder);
    }

    @Override
    public CharBuffer asCharBuffer() {
        int iRemaining = remaining() >> 1;
        return new ByteBufferAsCharBuffer(this, -1, 0, iRemaining, iRemaining, position(), order());
    }

    @Override
    public short getShort() {
        return Bits.getShort(this, ix(nextGetIndex(2)), this.bigEndian);
    }

    @Override
    public short getShort(int i) {
        return Bits.getShort(this, ix(checkIndex(i, 2)), this.bigEndian);
    }

    @Override
    short getShortUnchecked(int i) {
        return Bits.getShort(this, ix(i), this.bigEndian);
    }

    @Override
    void getUnchecked(int i, short[] sArr, int i2, int i3) {
        Memory.unsafeBulkGet(sArr, i2, i3 * 2, this.hb, ix(i), 2, !this.nativeByteOrder);
    }

    @Override
    public ByteBuffer putShort(short s) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        Bits.putShort(this, ix(nextPutIndex(2)), s, this.bigEndian);
        return this;
    }

    @Override
    public ByteBuffer putShort(int i, short s) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        Bits.putShort(this, ix(checkIndex(i, 2)), s, this.bigEndian);
        return this;
    }

    @Override
    void putShortUnchecked(int i, short s) {
        Bits.putShort(this, ix(i), s, this.bigEndian);
    }

    @Override
    void putUnchecked(int i, short[] sArr, int i2, int i3) {
        Memory.unsafeBulkPut(this.hb, ix(i), i3 * 2, sArr, i2, 2, !this.nativeByteOrder);
    }

    @Override
    public ShortBuffer asShortBuffer() {
        int iRemaining = remaining() >> 1;
        return new ByteBufferAsShortBuffer(this, -1, 0, iRemaining, iRemaining, position(), order());
    }

    @Override
    public int getInt() {
        return Bits.getInt(this, ix(nextGetIndex(4)), this.bigEndian);
    }

    @Override
    public int getInt(int i) {
        return Bits.getInt(this, ix(checkIndex(i, 4)), this.bigEndian);
    }

    @Override
    int getIntUnchecked(int i) {
        return Bits.getInt(this, ix(i), this.bigEndian);
    }

    @Override
    void getUnchecked(int i, int[] iArr, int i2, int i3) {
        Memory.unsafeBulkGet(iArr, i2, i3 * 4, this.hb, ix(i), 4, !this.nativeByteOrder);
    }

    @Override
    public ByteBuffer putInt(int i) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        Bits.putInt(this, ix(nextPutIndex(4)), i, this.bigEndian);
        return this;
    }

    @Override
    public ByteBuffer putInt(int i, int i2) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        Bits.putInt(this, ix(checkIndex(i, 4)), i2, this.bigEndian);
        return this;
    }

    @Override
    void putIntUnchecked(int i, int i2) {
        Bits.putInt(this, ix(i), i2, this.bigEndian);
    }

    @Override
    void putUnchecked(int i, int[] iArr, int i2, int i3) {
        Memory.unsafeBulkPut(this.hb, ix(i), i3 * 4, iArr, i2, 4, !this.nativeByteOrder);
    }

    @Override
    public IntBuffer asIntBuffer() {
        int iRemaining = remaining() >> 2;
        return new ByteBufferAsIntBuffer(this, -1, 0, iRemaining, iRemaining, position(), order());
    }

    @Override
    public long getLong() {
        return Bits.getLong(this, ix(nextGetIndex(8)), this.bigEndian);
    }

    @Override
    public long getLong(int i) {
        return Bits.getLong(this, ix(checkIndex(i, 8)), this.bigEndian);
    }

    @Override
    long getLongUnchecked(int i) {
        return Bits.getLong(this, ix(i), this.bigEndian);
    }

    @Override
    void getUnchecked(int i, long[] jArr, int i2, int i3) {
        Memory.unsafeBulkGet(jArr, i2, i3 * 8, this.hb, ix(i), 8, !this.nativeByteOrder);
    }

    @Override
    public ByteBuffer putLong(long j) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        Bits.putLong(this, ix(nextPutIndex(8)), j, this.bigEndian);
        return this;
    }

    @Override
    public ByteBuffer putLong(int i, long j) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        Bits.putLong(this, ix(checkIndex(i, 8)), j, this.bigEndian);
        return this;
    }

    @Override
    void putLongUnchecked(int i, long j) {
        Bits.putLong(this, ix(i), j, this.bigEndian);
    }

    @Override
    void putUnchecked(int i, long[] jArr, int i2, int i3) {
        Memory.unsafeBulkPut(this.hb, ix(i), i3 * 8, jArr, i2, 8, !this.nativeByteOrder);
    }

    @Override
    public LongBuffer asLongBuffer() {
        int iRemaining = remaining() >> 3;
        return new ByteBufferAsLongBuffer(this, -1, 0, iRemaining, iRemaining, position(), order());
    }

    @Override
    public float getFloat() {
        return Bits.getFloat(this, ix(nextGetIndex(4)), this.bigEndian);
    }

    @Override
    public float getFloat(int i) {
        return Bits.getFloat(this, ix(checkIndex(i, 4)), this.bigEndian);
    }

    @Override
    float getFloatUnchecked(int i) {
        return Bits.getFloat(this, ix(i), this.bigEndian);
    }

    @Override
    void getUnchecked(int i, float[] fArr, int i2, int i3) {
        Memory.unsafeBulkGet(fArr, i2, i3 * 4, this.hb, ix(i), 4, !this.nativeByteOrder);
    }

    @Override
    public ByteBuffer putFloat(float f) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        Bits.putFloat(this, ix(nextPutIndex(4)), f, this.bigEndian);
        return this;
    }

    @Override
    public ByteBuffer putFloat(int i, float f) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        Bits.putFloat(this, ix(checkIndex(i, 4)), f, this.bigEndian);
        return this;
    }

    @Override
    void putFloatUnchecked(int i, float f) {
        Bits.putFloat(this, ix(i), f, this.bigEndian);
    }

    @Override
    void putUnchecked(int i, float[] fArr, int i2, int i3) {
        Memory.unsafeBulkPut(this.hb, ix(i), i3 * 4, fArr, i2, 4, !this.nativeByteOrder);
    }

    @Override
    public FloatBuffer asFloatBuffer() {
        int iRemaining = remaining() >> 2;
        return new ByteBufferAsFloatBuffer(this, -1, 0, iRemaining, iRemaining, position(), order());
    }

    @Override
    public double getDouble() {
        return Bits.getDouble(this, ix(nextGetIndex(8)), this.bigEndian);
    }

    @Override
    public double getDouble(int i) {
        return Bits.getDouble(this, ix(checkIndex(i, 8)), this.bigEndian);
    }

    @Override
    double getDoubleUnchecked(int i) {
        return Bits.getDouble(this, ix(i), this.bigEndian);
    }

    @Override
    void getUnchecked(int i, double[] dArr, int i2, int i3) {
        Memory.unsafeBulkGet(dArr, i2, i3 * 8, this.hb, ix(i), 8, !this.nativeByteOrder);
    }

    @Override
    public ByteBuffer putDouble(double d) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        Bits.putDouble(this, ix(nextPutIndex(8)), d, this.bigEndian);
        return this;
    }

    @Override
    public ByteBuffer putDouble(int i, double d) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        Bits.putDouble(this, ix(checkIndex(i, 8)), d, this.bigEndian);
        return this;
    }

    @Override
    void putDoubleUnchecked(int i, double d) {
        Bits.putDouble(this, ix(i), d, this.bigEndian);
    }

    @Override
    void putUnchecked(int i, double[] dArr, int i2, int i3) {
        Memory.unsafeBulkPut(this.hb, ix(i), i3 * 8, dArr, i2, 8, !this.nativeByteOrder);
    }

    @Override
    public DoubleBuffer asDoubleBuffer() {
        int iRemaining = remaining() >> 3;
        return new ByteBufferAsDoubleBuffer(this, -1, 0, iRemaining, iRemaining, position(), order());
    }
}
