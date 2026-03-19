package java.nio.channels;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public abstract class AsynchronousFileChannel implements AsynchronousChannel {
    private static final FileAttribute<?>[] NO_ATTRIBUTES = new FileAttribute[0];

    public abstract void force(boolean z) throws IOException;

    public abstract Future<FileLock> lock(long j, long j2, boolean z);

    public abstract <A> void lock(long j, long j2, boolean z, A a, CompletionHandler<FileLock, ? super A> completionHandler);

    public abstract Future<Integer> read(ByteBuffer byteBuffer, long j);

    public abstract <A> void read(ByteBuffer byteBuffer, long j, A a, CompletionHandler<Integer, ? super A> completionHandler);

    public abstract long size() throws IOException;

    public abstract AsynchronousFileChannel truncate(long j) throws IOException;

    public abstract FileLock tryLock(long j, long j2, boolean z) throws IOException;

    public abstract Future<Integer> write(ByteBuffer byteBuffer, long j);

    public abstract <A> void write(ByteBuffer byteBuffer, long j, A a, CompletionHandler<Integer, ? super A> completionHandler);

    protected AsynchronousFileChannel() {
    }

    public static AsynchronousFileChannel open(Path path, Set<? extends OpenOption> set, ExecutorService executorService, FileAttribute<?>... fileAttributeArr) throws IOException {
        return path.getFileSystem().provider().newAsynchronousFileChannel(path, set, executorService, fileAttributeArr);
    }

    public static AsynchronousFileChannel open(Path path, OpenOption... openOptionArr) throws IOException {
        HashSet hashSet = new HashSet(openOptionArr.length);
        Collections.addAll(hashSet, openOptionArr);
        return open(path, hashSet, null, NO_ATTRIBUTES);
    }

    public final <A> void lock(A a, CompletionHandler<FileLock, ? super A> completionHandler) {
        lock(0L, Long.MAX_VALUE, false, a, completionHandler);
    }

    public final Future<FileLock> lock() {
        return lock(0L, Long.MAX_VALUE, false);
    }

    public final FileLock tryLock() throws IOException {
        return tryLock(0L, Long.MAX_VALUE, false);
    }
}
