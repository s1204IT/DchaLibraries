package java.nio;

import java.util.Arrays;
import libcore.io.Memory;

public abstract class ByteBuffer extends Buffer implements Comparable<ByteBuffer> {
    ByteOrder order;

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

    abstract byte[] protectedArray();

    abstract int protectedArrayOffset();

    abstract boolean protectedHasArray();

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

    public static ByteBuffer allocate(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity < 0: " + capacity);
        }
        return new ByteArrayBuffer(new byte[capacity]);
    }

    public static ByteBuffer allocateDirect(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity < 0: " + capacity);
        }
        MemoryBlock memoryBlock = MemoryBlock.allocate(capacity + 7);
        long address = memoryBlock.toLong();
        long alignedAddress = (7 + address) & (-8);
        return new DirectByteBuffer(memoryBlock, capacity, (int) (alignedAddress - address), false, null);
    }

    public static ByteBuffer wrap(byte[] array) {
        return new ByteArrayBuffer(array);
    }

    public static ByteBuffer wrap(byte[] array, int start, int byteCount) {
        Arrays.checkOffsetAndCount(array.length, start, byteCount);
        ByteBuffer buf = new ByteArrayBuffer(array);
        buf.position = start;
        buf.limit = start + byteCount;
        return buf;
    }

    ByteBuffer(int capacity, long effectiveDirectAddress) {
        super(0, capacity, effectiveDirectAddress);
        this.order = ByteOrder.BIG_ENDIAN;
    }

    @Override
    public final byte[] array() {
        return protectedArray();
    }

    @Override
    public final int arrayOffset() {
        return protectedArrayOffset();
    }

    @Override
    public int compareTo(ByteBuffer otherBuffer) {
        int thisPos = this.position;
        int otherPos = otherBuffer.position;
        for (int compareRemaining = remaining() < otherBuffer.remaining() ? remaining() : otherBuffer.remaining(); compareRemaining > 0; compareRemaining--) {
            byte thisByte = get(thisPos);
            byte otherByte = otherBuffer.get(otherPos);
            if (thisByte != otherByte) {
                return thisByte < otherByte ? -1 : 1;
            }
            thisPos++;
            otherPos++;
        }
        return remaining() - otherBuffer.remaining();
    }

    public boolean equals(Object other) {
        if (!(other instanceof ByteBuffer)) {
            return false;
        }
        ByteBuffer otherBuffer = (ByteBuffer) other;
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

    public ByteBuffer get(byte[] dst) {
        return get(dst, 0, dst.length);
    }

    public ByteBuffer get(byte[] dst, int dstOffset, int byteCount) {
        Arrays.checkOffsetAndCount(dst.length, dstOffset, byteCount);
        if (byteCount > remaining()) {
            throw new BufferUnderflowException();
        }
        for (int i = dstOffset; i < dstOffset + byteCount; i++) {
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

    public boolean isAccessible() {
        return true;
    }

    public void setAccessible(boolean accessible) {
        throw new UnsupportedOperationException();
    }

    public final ByteOrder order() {
        return this.order;
    }

    public final ByteBuffer order(ByteOrder byteOrder) {
        if (byteOrder == null) {
            byteOrder = ByteOrder.LITTLE_ENDIAN;
        }
        this.order = byteOrder;
        return this;
    }

    public final ByteBuffer put(byte[] src) {
        return put(src, 0, src.length);
    }

    public ByteBuffer put(byte[] src, int srcOffset, int byteCount) {
        Arrays.checkOffsetAndCount(src.length, srcOffset, byteCount);
        if (byteCount > remaining()) {
            throw new BufferOverflowException();
        }
        for (int i = srcOffset; i < srcOffset + byteCount; i++) {
            put(src[i]);
        }
        return this;
    }

    public ByteBuffer put(ByteBuffer src) {
        Object dstObject;
        if (!isAccessible()) {
            throw new IllegalStateException("buffer is inaccessible");
        }
        if (isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        if (src == this) {
            throw new IllegalArgumentException("src == this");
        }
        if (!src.isAccessible()) {
            throw new IllegalStateException("src buffer is inaccessible");
        }
        int srcByteCount = src.remaining();
        if (srcByteCount > remaining()) {
            throw new BufferOverflowException();
        }
        Object srcObject = src.isDirect() ? src : NioUtils.unsafeArray(src);
        int srcOffset = src.position();
        if (!src.isDirect()) {
            srcOffset += NioUtils.unsafeArrayOffset(src);
        }
        if (isDirect()) {
            dstObject = this;
        } else {
            dstObject = NioUtils.unsafeArray(this);
        }
        int dstOffset = position();
        if (!isDirect()) {
            dstOffset += NioUtils.unsafeArrayOffset(this);
        }
        Memory.memmove(dstObject, dstOffset, srcObject, srcOffset, srcByteCount);
        src.position(src.limit());
        position(position() + srcByteCount);
        return this;
    }
}
