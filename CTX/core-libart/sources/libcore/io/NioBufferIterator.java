package libcore.io;

public final class NioBufferIterator extends BufferIterator {
    private final long address;
    private final MemoryMappedFile file;
    private final int length;
    private int position;
    private final boolean swap;

    NioBufferIterator(MemoryMappedFile memoryMappedFile, long j, int i, boolean z) {
        memoryMappedFile.checkNotClosed();
        this.file = memoryMappedFile;
        this.address = j;
        if (i < 0) {
            throw new IllegalArgumentException("length < 0");
        }
        if (Long.compareUnsigned(j, (-1) - ((long) i)) > 0) {
            throw new IllegalArgumentException("length " + i + " would overflow 64-bit address space");
        }
        this.length = i;
        this.swap = z;
    }

    @Override
    public void seek(int i) {
        this.position = i;
    }

    @Override
    public void skip(int i) {
        this.position += i;
    }

    @Override
    public int pos() {
        return this.position;
    }

    @Override
    public void readByteArray(byte[] bArr, int i, int i2) {
        checkDstBounds(i, bArr.length, i2);
        this.file.checkNotClosed();
        checkReadBounds(this.position, this.length, i2);
        Memory.peekByteArray(this.address + ((long) this.position), bArr, i, i2);
        this.position += i2;
    }

    @Override
    public byte readByte() {
        this.file.checkNotClosed();
        checkReadBounds(this.position, this.length, 1);
        byte bPeekByte = Memory.peekByte(this.address + ((long) this.position));
        this.position++;
        return bPeekByte;
    }

    @Override
    public int readInt() {
        this.file.checkNotClosed();
        checkReadBounds(this.position, this.length, 4);
        int iPeekInt = Memory.peekInt(this.address + ((long) this.position), this.swap);
        this.position += 4;
        return iPeekInt;
    }

    @Override
    public void readIntArray(int[] iArr, int i, int i2) {
        checkDstBounds(i, iArr.length, i2);
        this.file.checkNotClosed();
        int i3 = 4 * i2;
        checkReadBounds(this.position, this.length, i3);
        Memory.peekIntArray(this.address + ((long) this.position), iArr, i, i2, this.swap);
        this.position += i3;
    }

    @Override
    public short readShort() {
        this.file.checkNotClosed();
        checkReadBounds(this.position, this.length, 2);
        short sPeekShort = Memory.peekShort(this.address + ((long) this.position), this.swap);
        this.position += 2;
        return sPeekShort;
    }

    private static void checkReadBounds(int i, int i2, int i3) {
        if (i < 0 || i3 < 0) {
            throw new IndexOutOfBoundsException("Invalid read args: position=" + i + ", byteCount=" + i3);
        }
        int i4 = i + i3;
        if (i4 < 0 || i4 > i2) {
            throw new IndexOutOfBoundsException("Read outside range: position=" + i + ", byteCount=" + i3 + ", length=" + i2);
        }
    }

    private static void checkDstBounds(int i, int i2, int i3) {
        if (i < 0 || i3 < 0) {
            throw new IndexOutOfBoundsException("Invalid dst args: offset=" + i2 + ", count=" + i3);
        }
        int i4 = i + i3;
        if (i4 < 0 || i4 > i2) {
            throw new IndexOutOfBoundsException("Write outside range: dst.length=" + i2 + ", offset=" + i + ", count=" + i3);
        }
    }
}
