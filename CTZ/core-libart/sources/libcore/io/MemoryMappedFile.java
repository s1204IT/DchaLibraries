package libcore.io;

import android.system.ErrnoException;
import android.system.OsConstants;
import java.io.FileDescriptor;
import java.nio.ByteOrder;

public final class MemoryMappedFile implements AutoCloseable {
    private final long address;
    private boolean closed;
    private final int size;

    public MemoryMappedFile(long j, long j2) {
        this.address = j;
        if (j2 < 0 || j2 > 2147483647L) {
            throw new IllegalArgumentException("Unsupported file size=" + j2);
        }
        this.size = (int) j2;
    }

    public static MemoryMappedFile mmapRO(String str) throws ErrnoException {
        FileDescriptor fileDescriptorOpen = Libcore.os.open(str, OsConstants.O_RDONLY, 0);
        try {
            long j = Libcore.os.fstat(fileDescriptorOpen).st_size;
            return new MemoryMappedFile(Libcore.os.mmap(0L, j, OsConstants.PROT_READ, OsConstants.MAP_SHARED, fileDescriptorOpen, 0L), j);
        } finally {
            Libcore.os.close(fileDescriptorOpen);
        }
    }

    @Override
    public void close() throws ErrnoException {
        if (!this.closed) {
            this.closed = true;
            Libcore.os.munmap(this.address, this.size);
        }
    }

    public boolean isClosed() {
        return this.closed;
    }

    public BufferIterator bigEndianIterator() {
        return new NioBufferIterator(this, this.address, this.size, ByteOrder.nativeOrder() != ByteOrder.BIG_ENDIAN);
    }

    public BufferIterator littleEndianIterator() {
        return new NioBufferIterator(this, this.address, this.size, ByteOrder.nativeOrder() != ByteOrder.LITTLE_ENDIAN);
    }

    void checkNotClosed() {
        if (this.closed) {
            throw new IllegalStateException("MemoryMappedFile is closed");
        }
    }

    public int size() {
        checkNotClosed();
        return this.size;
    }
}
