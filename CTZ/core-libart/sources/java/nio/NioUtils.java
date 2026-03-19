package java.nio;

import android.system.OsConstants;
import java.io.Closeable;
import java.io.FileDescriptor;
import java.nio.channels.FileChannel;
import sun.nio.ch.FileChannelImpl;

public final class NioUtils {
    private NioUtils() {
    }

    public static void freeDirectBuffer(ByteBuffer byteBuffer) {
        if (byteBuffer == null) {
            return;
        }
        DirectByteBuffer directByteBuffer = (DirectByteBuffer) byteBuffer;
        if (directByteBuffer.cleaner != null) {
            directByteBuffer.cleaner.clean();
        }
        directByteBuffer.memoryRef.free();
    }

    public static FileDescriptor getFD(FileChannel fileChannel) {
        return ((FileChannelImpl) fileChannel).fd;
    }

    public static FileChannel newFileChannel(Closeable closeable, FileDescriptor fileDescriptor, int i) {
        return FileChannelImpl.open(fileDescriptor, (String) null, (((OsConstants.O_RDONLY | OsConstants.O_RDWR) | OsConstants.O_SYNC) & i) != 0, (((OsConstants.O_WRONLY | OsConstants.O_RDWR) | OsConstants.O_SYNC) & i) != 0, (i & OsConstants.O_APPEND) != 0, closeable);
    }

    public static byte[] unsafeArray(ByteBuffer byteBuffer) {
        return byteBuffer.array();
    }

    public static int unsafeArrayOffset(ByteBuffer byteBuffer) {
        return byteBuffer.arrayOffset();
    }
}
