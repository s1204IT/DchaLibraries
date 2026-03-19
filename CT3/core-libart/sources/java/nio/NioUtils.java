package java.nio;

import android.system.OsConstants;
import java.io.Closeable;
import java.io.FileDescriptor;
import java.nio.channels.FileChannel;
import sun.nio.ch.FileChannelImpl;

public final class NioUtils {
    private NioUtils() {
    }

    public static void freeDirectBuffer(ByteBuffer buffer) {
        if (buffer == null) {
            return;
        }
        DirectByteBuffer dbb = (DirectByteBuffer) buffer;
        if (dbb.cleaner != null) {
            dbb.cleaner.clean();
        }
        dbb.memoryRef.free();
    }

    public static FileDescriptor getFD(FileChannel fc) {
        return ((FileChannelImpl) fc).fd;
    }

    public static FileChannel newFileChannel(Closeable ioObject, FileDescriptor fd, int mode) {
        boolean readable = (((OsConstants.O_RDONLY | OsConstants.O_RDWR) | OsConstants.O_SYNC) & mode) != 0;
        boolean writable = (((OsConstants.O_WRONLY | OsConstants.O_RDWR) | OsConstants.O_SYNC) & mode) != 0;
        boolean append = (OsConstants.O_APPEND & mode) != 0;
        return FileChannelImpl.open(fd, (String) null, readable, writable, append, ioObject);
    }

    public static byte[] unsafeArray(ByteBuffer b) {
        return b.array();
    }

    public static int unsafeArrayOffset(ByteBuffer b) {
        return b.arrayOffset();
    }
}
