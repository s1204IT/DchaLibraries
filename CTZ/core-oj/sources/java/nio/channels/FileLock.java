package java.nio.channels;

import java.io.IOException;

public abstract class FileLock implements AutoCloseable {
    private final Channel channel;
    private final long position;
    private final boolean shared;
    private final long size;

    public abstract boolean isValid();

    public abstract void release() throws IOException;

    protected FileLock(FileChannel fileChannel, long j, long j2, boolean z) {
        if (j < 0) {
            throw new IllegalArgumentException("Negative position");
        }
        if (j2 < 0) {
            throw new IllegalArgumentException("Negative size");
        }
        if (j + j2 < 0) {
            throw new IllegalArgumentException("Negative position + size");
        }
        this.channel = fileChannel;
        this.position = j;
        this.size = j2;
        this.shared = z;
    }

    protected FileLock(AsynchronousFileChannel asynchronousFileChannel, long j, long j2, boolean z) {
        if (j < 0) {
            throw new IllegalArgumentException("Negative position");
        }
        if (j2 < 0) {
            throw new IllegalArgumentException("Negative size");
        }
        if (j + j2 < 0) {
            throw new IllegalArgumentException("Negative position + size");
        }
        this.channel = asynchronousFileChannel;
        this.position = j;
        this.size = j2;
        this.shared = z;
    }

    public final FileChannel channel() {
        if (this.channel instanceof FileChannel) {
            return (FileChannel) this.channel;
        }
        return null;
    }

    public Channel acquiredBy() {
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

    public final boolean overlaps(long j, long j2) {
        return j2 + j > this.position && this.position + this.size > j;
    }

    @Override
    public final void close() throws IOException {
        release();
    }

    public final String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getName());
        sb.append("[");
        sb.append(this.position);
        sb.append(":");
        sb.append(this.size);
        sb.append(" ");
        sb.append(this.shared ? "shared" : "exclusive");
        sb.append(" ");
        sb.append(isValid() ? "valid" : "invalid");
        sb.append("]");
        return sb.toString();
    }
}
