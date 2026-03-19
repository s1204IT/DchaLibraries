package sun.nio.ch;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;

public class IOUtil {
    static final boolean $assertionsDisabled = false;
    static final int IOV_MAX = iovMax();

    public static native void configureBlocking(FileDescriptor fileDescriptor, boolean z) throws IOException;

    static native boolean drain(int i) throws IOException;

    static native int fdLimit();

    public static native int fdVal(FileDescriptor fileDescriptor);

    static native int iovMax();

    static native long makePipe(boolean z);

    static native boolean randomBytes(byte[] bArr);

    static native void setfdVal(FileDescriptor fileDescriptor, int i);

    private IOUtil() {
    }

    static int write(FileDescriptor fileDescriptor, ByteBuffer byteBuffer, long j, NativeDispatcher nativeDispatcher) throws IOException {
        if (byteBuffer instanceof DirectBuffer) {
            return writeFromNativeBuffer(fileDescriptor, byteBuffer, j, nativeDispatcher);
        }
        int iPosition = byteBuffer.position();
        int iLimit = byteBuffer.limit();
        ByteBuffer temporaryDirectBuffer = Util.getTemporaryDirectBuffer(iPosition <= iLimit ? iLimit - iPosition : 0);
        try {
            temporaryDirectBuffer.put(byteBuffer);
            temporaryDirectBuffer.flip();
            byteBuffer.position(iPosition);
            int iWriteFromNativeBuffer = writeFromNativeBuffer(fileDescriptor, temporaryDirectBuffer, j, nativeDispatcher);
            if (iWriteFromNativeBuffer > 0) {
                byteBuffer.position(iPosition + iWriteFromNativeBuffer);
            }
            return iWriteFromNativeBuffer;
        } finally {
            Util.offerFirstTemporaryDirectBuffer(temporaryDirectBuffer);
        }
    }

    private static int writeFromNativeBuffer(FileDescriptor fileDescriptor, ByteBuffer byteBuffer, long j, NativeDispatcher nativeDispatcher) throws IOException {
        int iWrite;
        int iPosition = byteBuffer.position();
        int iLimit = byteBuffer.limit();
        int i = iPosition <= iLimit ? iLimit - iPosition : 0;
        if (i == 0) {
            return 0;
        }
        if (j != -1) {
            iWrite = nativeDispatcher.pwrite(fileDescriptor, ((DirectBuffer) byteBuffer).address() + ((long) iPosition), i, j);
        } else {
            iWrite = nativeDispatcher.write(fileDescriptor, ((DirectBuffer) byteBuffer).address() + ((long) iPosition), i);
        }
        if (iWrite > 0) {
            byteBuffer.position(iPosition + iWrite);
        }
        return iWrite;
    }

    static long write(FileDescriptor fileDescriptor, ByteBuffer[] byteBufferArr, NativeDispatcher nativeDispatcher) throws IOException {
        return write(fileDescriptor, byteBufferArr, 0, byteBufferArr.length, nativeDispatcher);
    }

    static long write(FileDescriptor fileDescriptor, ByteBuffer[] byteBufferArr, int i, int i2, NativeDispatcher nativeDispatcher) throws IOException {
        IOVecWrapper iOVecWrapper = IOVecWrapper.get(i2);
        int i3 = i2 + i;
        int i4 = 0;
        int i5 = 0;
        while (i < i3) {
            try {
                if (i5 >= IOV_MAX) {
                    break;
                }
                ByteBuffer byteBuffer = byteBufferArr[i];
                int iPosition = byteBuffer.position();
                int iLimit = byteBuffer.limit();
                int i6 = iPosition <= iLimit ? iLimit - iPosition : 0;
                if (i6 > 0) {
                    iOVecWrapper.setBuffer(i5, byteBuffer, iPosition, i6);
                    boolean z = byteBuffer instanceof DirectBuffer;
                    Object obj = byteBuffer;
                    if (!z) {
                        ByteBuffer temporaryDirectBuffer = Util.getTemporaryDirectBuffer(i6);
                        temporaryDirectBuffer.put(byteBuffer);
                        temporaryDirectBuffer.flip();
                        iOVecWrapper.setShadow(i5, temporaryDirectBuffer);
                        byteBuffer.position(iPosition);
                        iPosition = temporaryDirectBuffer.position();
                        obj = temporaryDirectBuffer;
                    }
                    iOVecWrapper.putBase(i5, ((DirectBuffer) obj).address() + ((long) iPosition));
                    iOVecWrapper.putLen(i5, i6);
                    i5++;
                }
                i++;
            } finally {
                while (i4 < i5) {
                    ByteBuffer shadow = iOVecWrapper.getShadow(i4);
                    if (shadow != null) {
                        Util.offerLastTemporaryDirectBuffer(shadow);
                    }
                    iOVecWrapper.clearRefs(i4);
                    i4++;
                }
            }
        }
        if (i5 == 0) {
            return 0L;
        }
        long jWritev = nativeDispatcher.writev(fileDescriptor, iOVecWrapper.address, i5);
        long j = jWritev;
        for (int i7 = 0; i7 < i5; i7++) {
            if (j > 0) {
                ByteBuffer buffer = iOVecWrapper.getBuffer(i7);
                int position = iOVecWrapper.getPosition(i7);
                int remaining = iOVecWrapper.getRemaining(i7);
                if (j <= remaining) {
                    remaining = (int) j;
                }
                buffer.position(position + remaining);
                j -= (long) remaining;
            }
            ByteBuffer shadow2 = iOVecWrapper.getShadow(i7);
            if (shadow2 != null) {
                Util.offerLastTemporaryDirectBuffer(shadow2);
            }
            iOVecWrapper.clearRefs(i7);
        }
        return jWritev;
    }

    static int read(FileDescriptor fileDescriptor, ByteBuffer byteBuffer, long j, NativeDispatcher nativeDispatcher) throws IOException {
        if (byteBuffer.isReadOnly()) {
            throw new IllegalArgumentException("Read-only buffer");
        }
        if (byteBuffer instanceof DirectBuffer) {
            return readIntoNativeBuffer(fileDescriptor, byteBuffer, j, nativeDispatcher);
        }
        ByteBuffer temporaryDirectBuffer = Util.getTemporaryDirectBuffer(byteBuffer.remaining());
        try {
            int intoNativeBuffer = readIntoNativeBuffer(fileDescriptor, temporaryDirectBuffer, j, nativeDispatcher);
            temporaryDirectBuffer.flip();
            if (intoNativeBuffer > 0) {
                byteBuffer.put(temporaryDirectBuffer);
            }
            return intoNativeBuffer;
        } finally {
            Util.offerFirstTemporaryDirectBuffer(temporaryDirectBuffer);
        }
    }

    private static int readIntoNativeBuffer(FileDescriptor fileDescriptor, ByteBuffer byteBuffer, long j, NativeDispatcher nativeDispatcher) throws IOException {
        int iPread;
        int iPosition = byteBuffer.position();
        int iLimit = byteBuffer.limit();
        int i = iPosition <= iLimit ? iLimit - iPosition : 0;
        if (i == 0) {
            return 0;
        }
        if (j != -1) {
            iPread = nativeDispatcher.pread(fileDescriptor, ((DirectBuffer) byteBuffer).address() + ((long) iPosition), i, j);
        } else {
            iPread = nativeDispatcher.read(fileDescriptor, ((DirectBuffer) byteBuffer).address() + ((long) iPosition), i);
        }
        if (iPread > 0) {
            byteBuffer.position(iPosition + iPread);
        }
        return iPread;
    }

    static long read(FileDescriptor fileDescriptor, ByteBuffer[] byteBufferArr, NativeDispatcher nativeDispatcher) throws IOException {
        return read(fileDescriptor, byteBufferArr, 0, byteBufferArr.length, nativeDispatcher);
    }

    static long read(FileDescriptor fileDescriptor, ByteBuffer[] byteBufferArr, int i, int i2, NativeDispatcher nativeDispatcher) throws IOException {
        IOVecWrapper iOVecWrapper = IOVecWrapper.get(i2);
        int i3 = i2 + i;
        int i4 = 0;
        int i5 = 0;
        while (i < i3) {
            try {
                if (i5 >= IOV_MAX) {
                    break;
                }
                ByteBuffer byteBuffer = byteBufferArr[i];
                if (byteBuffer.isReadOnly()) {
                    throw new IllegalArgumentException("Read-only buffer");
                }
                int iPosition = byteBuffer.position();
                int iLimit = byteBuffer.limit();
                int i6 = iPosition <= iLimit ? iLimit - iPosition : 0;
                if (i6 > 0) {
                    iOVecWrapper.setBuffer(i5, byteBuffer, iPosition, i6);
                    boolean z = byteBuffer instanceof DirectBuffer;
                    Object obj = byteBuffer;
                    if (!z) {
                        ByteBuffer temporaryDirectBuffer = Util.getTemporaryDirectBuffer(i6);
                        iOVecWrapper.setShadow(i5, temporaryDirectBuffer);
                        iPosition = temporaryDirectBuffer.position();
                        obj = temporaryDirectBuffer;
                    }
                    iOVecWrapper.putBase(i5, ((DirectBuffer) obj).address() + ((long) iPosition));
                    iOVecWrapper.putLen(i5, i6);
                    i5++;
                }
                i++;
            } finally {
                while (i4 < i5) {
                    ByteBuffer shadow = iOVecWrapper.getShadow(i4);
                    if (shadow != null) {
                        Util.offerLastTemporaryDirectBuffer(shadow);
                    }
                    iOVecWrapper.clearRefs(i4);
                    i4++;
                }
            }
        }
        if (i5 == 0) {
            return 0L;
        }
        long vVar = nativeDispatcher.readv(fileDescriptor, iOVecWrapper.address, i5);
        long j = vVar;
        for (int i7 = 0; i7 < i5; i7++) {
            ByteBuffer shadow2 = iOVecWrapper.getShadow(i7);
            if (j > 0) {
                ByteBuffer buffer = iOVecWrapper.getBuffer(i7);
                int remaining = iOVecWrapper.getRemaining(i7);
                if (j <= remaining) {
                    remaining = (int) j;
                }
                if (shadow2 == null) {
                    buffer.position(iOVecWrapper.getPosition(i7) + remaining);
                } else {
                    shadow2.limit(shadow2.position() + remaining);
                    buffer.put(shadow2);
                }
                j -= (long) remaining;
            }
            if (shadow2 != null) {
                Util.offerLastTemporaryDirectBuffer(shadow2);
            }
            iOVecWrapper.clearRefs(i7);
        }
        return vVar;
    }

    public static FileDescriptor newFD(int i) {
        FileDescriptor fileDescriptor = new FileDescriptor();
        setfdVal(fileDescriptor, i);
        return fileDescriptor;
    }
}
