package java.nio;

import java.io.FileDescriptor;
import sun.misc.Unsafe;

public abstract class MappedByteBuffer extends ByteBuffer {
    private static byte unused;
    private final FileDescriptor fd;

    private native void force0(FileDescriptor fileDescriptor, long j, long j2);

    private native boolean isLoaded0(long j, long j2, int i);

    private native void load0(long j, long j2);

    MappedByteBuffer(int i, int i2, int i3, int i4, FileDescriptor fileDescriptor) {
        super(i, i2, i3, i4);
        this.fd = fileDescriptor;
    }

    MappedByteBuffer(int i, int i2, int i3, int i4, byte[] bArr, int i5) {
        super(i, i2, i3, i4, bArr, i5);
        this.fd = null;
    }

    MappedByteBuffer(int i, int i2, int i3, int i4) {
        super(i, i2, i3, i4);
        this.fd = null;
    }

    private void checkMapped() {
        if (this.fd == null) {
            throw new UnsupportedOperationException();
        }
    }

    private long mappingOffset() {
        long jPageSize = Bits.pageSize();
        long j = this.address % jPageSize;
        return j >= 0 ? j : j + jPageSize;
    }

    private long mappingAddress(long j) {
        return this.address - j;
    }

    private long mappingLength(long j) {
        return ((long) capacity()) + j;
    }

    public final boolean isLoaded() {
        checkMapped();
        if (this.address == 0 || capacity() == 0) {
            return true;
        }
        long jMappingOffset = mappingOffset();
        long jMappingLength = mappingLength(jMappingOffset);
        return isLoaded0(mappingAddress(jMappingOffset), jMappingLength, Bits.pageCount(jMappingLength));
    }

    public final MappedByteBuffer load() {
        checkMapped();
        if (this.address == 0 || capacity() == 0) {
            return this;
        }
        long jMappingOffset = mappingOffset();
        long jMappingLength = mappingLength(jMappingOffset);
        load0(mappingAddress(jMappingOffset), jMappingLength);
        Unsafe unsafe = Unsafe.getUnsafe();
        int iPageSize = Bits.pageSize();
        int iPageCount = Bits.pageCount(jMappingLength);
        long jMappingAddress = mappingAddress(jMappingOffset);
        byte b = 0;
        for (int i = 0; i < iPageCount; i++) {
            b = (byte) (b ^ unsafe.getByte(jMappingAddress));
            jMappingAddress += (long) iPageSize;
        }
        if (unused != 0) {
            unused = b;
        }
        return this;
    }

    public final MappedByteBuffer force() {
        checkMapped();
        if (this.address != 0 && capacity() != 0) {
            long jMappingOffset = mappingOffset();
            force0(this.fd, mappingAddress(jMappingOffset), mappingLength(jMappingOffset));
        }
        return this;
    }
}
