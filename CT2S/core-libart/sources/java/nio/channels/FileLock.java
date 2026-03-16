package java.nio.channels;

import java.io.IOException;

public abstract class FileLock implements AutoCloseable {
    private final FileChannel channel;
    private final long position;
    private final boolean shared;
    private final long size;

    public abstract boolean isValid();

    public abstract void release() throws IOException;

    protected FileLock(FileChannel channel, long position, long size, boolean shared) {
        if (position < 0 || size < 0 || position + size < 0) {
            throw new IllegalArgumentException("position=" + position + " size=" + size);
        }
        this.channel = channel;
        this.position = position;
        this.size = size;
        this.shared = shared;
    }

    public final FileChannel channel() {
        return this.channel;
    }

    public final long position() {
        return this.position;
    }

    public final long size() {
        return this.size;
    }

    public final boolean isShared() {
        return this.shared;
    }

    public final boolean overlaps(long start, long length) {
        long end = (this.position + this.size) - 1;
        long newEnd = (start + length) - 1;
        return end >= start && this.position <= newEnd;
    }

    @Override
    public final void close() throws IOException {
        release();
    }

    public final String toString() {
        return "FileLock[position=" + this.position + ", size=" + this.size + ", shared=" + this.shared + "]";
    }
}
