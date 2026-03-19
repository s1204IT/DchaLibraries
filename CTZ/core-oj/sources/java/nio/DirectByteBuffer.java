package java.nio;

import dalvik.system.VMRuntime;
import java.io.FileDescriptor;
import libcore.io.Memory;
import sun.misc.Cleaner;
import sun.nio.ch.DirectBuffer;

public class DirectByteBuffer extends MappedByteBuffer implements DirectBuffer {
    static final boolean $assertionsDisabled = false;
    final Cleaner cleaner;
    final MemoryRef memoryRef;

    static final class MemoryRef {
        long allocatedAddress;
        byte[] buffer;
        boolean isAccessible;
        boolean isFreed;
        final int offset;
        final Object originalBufferObject;

        MemoryRef(int i) {
            VMRuntime runtime = VMRuntime.getRuntime();
            this.buffer = (byte[]) runtime.newNonMovableArray(Byte.TYPE, i + 7);
            this.allocatedAddress = runtime.addressOf(this.buffer);
            this.offset = (int) (((this.allocatedAddress + 7) & (-8)) - this.allocatedAddress);
            this.isAccessible = true;
            this.isFreed = false;
            this.originalBufferObject = null;
        }

        MemoryRef(long j, Object obj) {
            this.buffer = null;
            this.allocatedAddress = j;
            this.offset = 0;
            this.originalBufferObject = obj;
            this.isAccessible = true;
        }

        void free() {
            this.buffer = null;
            this.allocatedAddress = 0L;
            this.isAccessible = false;
            this.isFreed = true;
        }
    }

    DirectByteBuffer(int i, MemoryRef memoryRef) {
        super(-1, 0, i, i, memoryRef.buffer, memoryRef.offset);
        this.memoryRef = memoryRef;
        this.address = memoryRef.allocatedAddress + ((long) memoryRef.offset);
        this.cleaner = null;
        this.isReadOnly = false;
    }

    private DirectByteBuffer(long j, int i) {
        super(-1, 0, i, i);
        this.memoryRef = new MemoryRef(j, this);
        this.address = j;
        this.cleaner = null;
    }

    public DirectByteBuffer(int i, long j, FileDescriptor fileDescriptor, Runnable runnable, boolean z) {
        super(-1, 0, i, i, fileDescriptor);
        this.isReadOnly = z;
        this.memoryRef = new MemoryRef(j, null);
        this.address = j;
        this.cleaner = Cleaner.create(this.memoryRef, runnable);
    }

    DirectByteBuffer(MemoryRef memoryRef, int i, int i2, int i3, int i4, int i5) {
        this(memoryRef, i, i2, i3, i4, i5, false);
    }

    DirectByteBuffer(MemoryRef memoryRef, int i, int i2, int i3, int i4, int i5, boolean z) {
        super(i, i2, i3, i4, memoryRef.buffer, i5);
        this.isReadOnly = z;
        this.memoryRef = memoryRef;
        this.address = memoryRef.allocatedAddress + ((long) i5);
        this.cleaner = null;
    }

    @Override
    public final Object attachment() {
        return this.memoryRef;
    }

    @Override
    public final Cleaner cleaner() {
        return this.cleaner;
    }

    @Override
    public final ByteBuffer slice() {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        int iPosition = position();
        int iLimit = limit();
        int i = iPosition <= iLimit ? iLimit - iPosition : 0;
        return new DirectByteBuffer(this.memoryRef, -1, 0, i, i, iPosition + this.offset, this.isReadOnly);
    }

    @Override
    public final ByteBuffer duplicate() {
        if (this.memoryRef.isFreed) {
            throw new IllegalStateException("buffer has been freed");
        }
        return new DirectByteBuffer(this.memoryRef, markValue(), position(), limit(), capacity(), this.offset, this.isReadOnly);
    }

    @Override
    public final ByteBuffer asReadOnlyBuffer() {
        if (this.memoryRef.isFreed) {
            throw new IllegalStateException("buffer has been freed");
        }
        return new DirectByteBuffer(this.memoryRef, markValue(), position(), limit(), capacity(), this.offset, true);
    }

    @Override
    public final long address() {
        return this.address;
    }

    private long ix(int i) {
        return this.address + ((long) i);
    }

    private byte get(long j) {
        return Memory.peekByte(j);
    }

    @Override
    public final byte get() {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        return get(ix(nextGetIndex()));
    }

    @Override
    public final byte get(int i) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        return get(ix(checkIndex(i)));
    }

    @Override
    public ByteBuffer get(byte[] bArr, int i, int i2) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        checkBounds(i, i2, bArr.length);
        int iPosition = position();
        int iLimit = limit();
        if (i2 > (iPosition <= iLimit ? iLimit - iPosition : 0)) {
            throw new BufferUnderflowException();
        }
        Memory.peekByteArray(ix(iPosition), bArr, i, i2);
        this.position = iPosition + i2;
        return this;
    }

    private ByteBuffer put(long j, byte b) {
        Memory.pokeByte(j, b);
        return this;
    }

    @Override
    public final ByteBuffer put(byte b) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        put(ix(nextPutIndex()), b);
        return this;
    }

    @Override
    public final ByteBuffer put(int i, byte b) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        put(ix(checkIndex(i)), b);
        return this;
    }

    @Override
    public ByteBuffer put(byte[] bArr, int i, int i2) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        checkBounds(i, i2, bArr.length);
        int iPosition = position();
        int iLimit = limit();
        if (i2 > (iPosition <= iLimit ? iLimit - iPosition : 0)) {
            throw new BufferOverflowException();
        }
        Memory.pokeByteArray(ix(iPosition), bArr, i, i2);
        this.position = iPosition + i2;
        return this;
    }

    @Override
    public final ByteBuffer compact() {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        int iPosition = position();
        int iLimit = limit();
        int i = iPosition <= iLimit ? iLimit - iPosition : 0;
        System.arraycopy(this.hb, this.position + this.offset, this.hb, this.offset, remaining());
        position(i);
        limit(capacity());
        discardMark();
        return this;
    }

    @Override
    public final boolean isDirect() {
        return true;
    }

    @Override
    public final boolean isReadOnly() {
        return this.isReadOnly;
    }

    @Override
    final byte _get(int i) {
        return get(i);
    }

    @Override
    final void _put(int i, byte b) {
        put(i, b);
    }

    @Override
    public final char getChar() {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        int i = this.position + 2;
        if (i > limit()) {
            throw new BufferUnderflowException();
        }
        char cPeekShort = (char) Memory.peekShort(ix(this.position), !this.nativeByteOrder);
        this.position = i;
        return cPeekShort;
    }

    @Override
    public final char getChar(int i) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        checkIndex(i, 2);
        return (char) Memory.peekShort(ix(i), !this.nativeByteOrder);
    }

    @Override
    char getCharUnchecked(int i) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        return (char) Memory.peekShort(ix(i), !this.nativeByteOrder);
    }

    @Override
    void getUnchecked(int i, char[] cArr, int i2, int i3) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        Memory.peekCharArray(ix(i), cArr, i2, i3, !this.nativeByteOrder);
    }

    private ByteBuffer putChar(long j, char c) {
        Memory.pokeShort(j, (short) c, !this.nativeByteOrder);
        return this;
    }

    @Override
    public final ByteBuffer putChar(char c) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        putChar(ix(nextPutIndex(2)), c);
        return this;
    }

    @Override
    public final ByteBuffer putChar(int i, char c) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        putChar(ix(checkIndex(i, 2)), c);
        return this;
    }

    @Override
    void putCharUnchecked(int i, char c) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        putChar(ix(i), c);
    }

    @Override
    void putUnchecked(int i, char[] cArr, int i2, int i3) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        Memory.pokeCharArray(ix(i), cArr, i2, i3, !this.nativeByteOrder);
    }

    @Override
    public final CharBuffer asCharBuffer() {
        if (this.memoryRef.isFreed) {
            throw new IllegalStateException("buffer has been freed");
        }
        int iPosition = position();
        int iLimit = limit();
        int i = (iPosition <= iLimit ? iLimit - iPosition : 0) >> 1;
        return new ByteBufferAsCharBuffer(this, -1, 0, i, i, iPosition, order());
    }

    private short getShort(long j) {
        return Memory.peekShort(j, !this.nativeByteOrder);
    }

    @Override
    public final short getShort() {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        return getShort(ix(nextGetIndex(2)));
    }

    @Override
    public final short getShort(int i) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        return getShort(ix(checkIndex(i, 2)));
    }

    @Override
    short getShortUnchecked(int i) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        return getShort(ix(i));
    }

    @Override
    void getUnchecked(int i, short[] sArr, int i2, int i3) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        Memory.peekShortArray(ix(i), sArr, i2, i3, !this.nativeByteOrder);
    }

    private ByteBuffer putShort(long j, short s) {
        Memory.pokeShort(j, s, !this.nativeByteOrder);
        return this;
    }

    @Override
    public final ByteBuffer putShort(short s) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        putShort(ix(nextPutIndex(2)), s);
        return this;
    }

    @Override
    public final ByteBuffer putShort(int i, short s) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        putShort(ix(checkIndex(i, 2)), s);
        return this;
    }

    @Override
    void putShortUnchecked(int i, short s) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        putShort(ix(i), s);
    }

    @Override
    void putUnchecked(int i, short[] sArr, int i2, int i3) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        Memory.pokeShortArray(ix(i), sArr, i2, i3, !this.nativeByteOrder);
    }

    @Override
    public final ShortBuffer asShortBuffer() {
        if (this.memoryRef.isFreed) {
            throw new IllegalStateException("buffer has been freed");
        }
        int iPosition = position();
        int iLimit = limit();
        int i = (iPosition <= iLimit ? iLimit - iPosition : 0) >> 1;
        return new ByteBufferAsShortBuffer(this, -1, 0, i, i, iPosition, order());
    }

    private int getInt(long j) {
        return Memory.peekInt(j, !this.nativeByteOrder);
    }

    @Override
    public int getInt() {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        return getInt(ix(nextGetIndex(4)));
    }

    @Override
    public int getInt(int i) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        return getInt(ix(checkIndex(i, 4)));
    }

    @Override
    final int getIntUnchecked(int i) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        return getInt(ix(i));
    }

    @Override
    final void getUnchecked(int i, int[] iArr, int i2, int i3) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        Memory.peekIntArray(ix(i), iArr, i2, i3, !this.nativeByteOrder);
    }

    private ByteBuffer putInt(long j, int i) {
        Memory.pokeInt(j, i, !this.nativeByteOrder);
        return this;
    }

    @Override
    public final ByteBuffer putInt(int i) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        putInt(ix(nextPutIndex(4)), i);
        return this;
    }

    @Override
    public final ByteBuffer putInt(int i, int i2) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        putInt(ix(checkIndex(i, 4)), i2);
        return this;
    }

    @Override
    final void putIntUnchecked(int i, int i2) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        putInt(ix(i), i2);
    }

    @Override
    final void putUnchecked(int i, int[] iArr, int i2, int i3) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        Memory.pokeIntArray(ix(i), iArr, i2, i3, !this.nativeByteOrder);
    }

    @Override
    public final IntBuffer asIntBuffer() {
        if (this.memoryRef.isFreed) {
            throw new IllegalStateException("buffer has been freed");
        }
        int iPosition = position();
        int iLimit = limit();
        int i = (iPosition <= iLimit ? iLimit - iPosition : 0) >> 2;
        return new ByteBufferAsIntBuffer(this, -1, 0, i, i, iPosition, order());
    }

    private long getLong(long j) {
        return Memory.peekLong(j, !this.nativeByteOrder);
    }

    @Override
    public final long getLong() {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        return getLong(ix(nextGetIndex(8)));
    }

    @Override
    public final long getLong(int i) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        return getLong(ix(checkIndex(i, 8)));
    }

    @Override
    final long getLongUnchecked(int i) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        return getLong(ix(i));
    }

    @Override
    final void getUnchecked(int i, long[] jArr, int i2, int i3) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        Memory.peekLongArray(ix(i), jArr, i2, i3, !this.nativeByteOrder);
    }

    private ByteBuffer putLong(long j, long j2) {
        Memory.pokeLong(j, j2, !this.nativeByteOrder);
        return this;
    }

    @Override
    public final ByteBuffer putLong(long j) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        putLong(ix(nextPutIndex(8)), j);
        return this;
    }

    @Override
    public final ByteBuffer putLong(int i, long j) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        putLong(ix(checkIndex(i, 8)), j);
        return this;
    }

    @Override
    final void putLongUnchecked(int i, long j) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        putLong(ix(i), j);
    }

    @Override
    final void putUnchecked(int i, long[] jArr, int i2, int i3) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        Memory.pokeLongArray(ix(i), jArr, i2, i3, !this.nativeByteOrder);
    }

    @Override
    public final LongBuffer asLongBuffer() {
        if (this.memoryRef.isFreed) {
            throw new IllegalStateException("buffer has been freed");
        }
        int iPosition = position();
        int iLimit = limit();
        int i = (iPosition <= iLimit ? iLimit - iPosition : 0) >> 3;
        return new ByteBufferAsLongBuffer(this, -1, 0, i, i, iPosition, order());
    }

    private float getFloat(long j) {
        return Float.intBitsToFloat(Memory.peekInt(j, !this.nativeByteOrder));
    }

    @Override
    public final float getFloat() {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        return getFloat(ix(nextGetIndex(4)));
    }

    @Override
    public final float getFloat(int i) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        return getFloat(ix(checkIndex(i, 4)));
    }

    @Override
    final float getFloatUnchecked(int i) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        return getFloat(ix(i));
    }

    @Override
    final void getUnchecked(int i, float[] fArr, int i2, int i3) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        Memory.peekFloatArray(ix(i), fArr, i2, i3, !this.nativeByteOrder);
    }

    private ByteBuffer putFloat(long j, float f) {
        Memory.pokeInt(j, Float.floatToRawIntBits(f), !this.nativeByteOrder);
        return this;
    }

    @Override
    public final ByteBuffer putFloat(float f) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        putFloat(ix(nextPutIndex(4)), f);
        return this;
    }

    @Override
    public final ByteBuffer putFloat(int i, float f) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        putFloat(ix(checkIndex(i, 4)), f);
        return this;
    }

    @Override
    final void putFloatUnchecked(int i, float f) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        putFloat(ix(i), f);
    }

    @Override
    final void putUnchecked(int i, float[] fArr, int i2, int i3) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        Memory.pokeFloatArray(ix(i), fArr, i2, i3, !this.nativeByteOrder);
    }

    @Override
    public final FloatBuffer asFloatBuffer() {
        if (this.memoryRef.isFreed) {
            throw new IllegalStateException("buffer has been freed");
        }
        int iPosition = position();
        int iLimit = limit();
        int i = (iPosition <= iLimit ? iLimit - iPosition : 0) >> 2;
        return new ByteBufferAsFloatBuffer(this, -1, 0, i, i, iPosition, order());
    }

    private double getDouble(long j) {
        return Double.longBitsToDouble(Memory.peekLong(j, !this.nativeByteOrder));
    }

    @Override
    public final double getDouble() {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        return getDouble(ix(nextGetIndex(8)));
    }

    @Override
    public final double getDouble(int i) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        return getDouble(ix(checkIndex(i, 8)));
    }

    @Override
    final double getDoubleUnchecked(int i) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        return getDouble(ix(i));
    }

    @Override
    final void getUnchecked(int i, double[] dArr, int i2, int i3) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        Memory.peekDoubleArray(ix(i), dArr, i2, i3, !this.nativeByteOrder);
    }

    private ByteBuffer putDouble(long j, double d) {
        Memory.pokeLong(j, Double.doubleToRawLongBits(d), !this.nativeByteOrder);
        return this;
    }

    @Override
    public final ByteBuffer putDouble(double d) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        putDouble(ix(nextPutIndex(8)), d);
        return this;
    }

    @Override
    public final ByteBuffer putDouble(int i, double d) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        putDouble(ix(checkIndex(i, 8)), d);
        return this;
    }

    @Override
    final void putDoubleUnchecked(int i, double d) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        putDouble(ix(i), d);
    }

    @Override
    final void putUnchecked(int i, double[] dArr, int i2, int i3) {
        if (!this.memoryRef.isAccessible) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        Memory.pokeDoubleArray(ix(i), dArr, i2, i3, !this.nativeByteOrder);
    }

    @Override
    public final DoubleBuffer asDoubleBuffer() {
        if (this.memoryRef.isFreed) {
            throw new IllegalStateException("buffer has been freed");
        }
        int iPosition = position();
        int iLimit = limit();
        int i = (iPosition <= iLimit ? iLimit - iPosition : 0) >> 3;
        return new ByteBufferAsDoubleBuffer(this, -1, 0, i, i, iPosition, order());
    }

    @Override
    public final boolean isAccessible() {
        return this.memoryRef.isAccessible;
    }

    @Override
    public final void setAccessible(boolean z) {
        this.memoryRef.isAccessible = z;
    }
}
