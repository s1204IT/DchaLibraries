package android.os;

import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.Log;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.util.Arrays;
import libcore.io.IoBridge;
import libcore.io.IoUtils;
import libcore.io.Memory;
import libcore.io.Streams;

@Deprecated
public class FileBridge extends Thread {
    private static final int CMD_CLOSE = 3;
    private static final int CMD_FSYNC = 2;
    private static final int CMD_WRITE = 1;
    private static final int MSG_LENGTH = 8;
    private static final String TAG = "FileBridge";
    private volatile boolean mClosed;
    private FileDescriptor mTarget;
    private final FileDescriptor mServer = new FileDescriptor();
    private final FileDescriptor mClient = new FileDescriptor();

    public FileBridge() {
        try {
            Os.socketpair(OsConstants.AF_UNIX, OsConstants.SOCK_STREAM, 0, this.mServer, this.mClient);
        } catch (ErrnoException e) {
            throw new RuntimeException("Failed to create bridge");
        }
    }

    public boolean isClosed() {
        return this.mClosed;
    }

    public void forceClose() {
        IoUtils.closeQuietly(this.mTarget);
        IoUtils.closeQuietly(this.mServer);
        IoUtils.closeQuietly(this.mClient);
        this.mClosed = true;
    }

    public void setTargetFile(FileDescriptor fileDescriptor) {
        this.mTarget = fileDescriptor;
    }

    public FileDescriptor getClientSocket() {
        return this.mClient;
    }

    @Override
    public void run() {
        byte[] bArr = new byte[8192];
        while (true) {
            try {
                try {
                    if (IoBridge.read(this.mServer, bArr, 0, 8) != 8) {
                        break;
                    }
                    int iPeekInt = Memory.peekInt(bArr, 0, ByteOrder.BIG_ENDIAN);
                    if (iPeekInt == 1) {
                        int iPeekInt2 = Memory.peekInt(bArr, 4, ByteOrder.BIG_ENDIAN);
                        while (iPeekInt2 > 0) {
                            int i = IoBridge.read(this.mServer, bArr, 0, Math.min(bArr.length, iPeekInt2));
                            if (i == -1) {
                                throw new IOException("Unexpected EOF; still expected " + iPeekInt2 + " bytes");
                            }
                            IoBridge.write(this.mTarget, bArr, 0, i);
                            iPeekInt2 -= i;
                        }
                    } else if (iPeekInt == 2) {
                        Os.fsync(this.mTarget);
                        IoBridge.write(this.mServer, bArr, 0, 8);
                    } else if (iPeekInt == 3) {
                        Os.fsync(this.mTarget);
                        Os.close(this.mTarget);
                        this.mClosed = true;
                        IoBridge.write(this.mServer, bArr, 0, 8);
                        break;
                    }
                } catch (ErrnoException | IOException e) {
                    Log.wtf(TAG, "Failed during bridge", e);
                }
            } finally {
                forceClose();
            }
        }
    }

    public static class FileBridgeOutputStream extends OutputStream {
        private final FileDescriptor mClient;
        private final ParcelFileDescriptor mClientPfd;
        private final byte[] mTemp;

        public FileBridgeOutputStream(ParcelFileDescriptor parcelFileDescriptor) {
            this.mTemp = new byte[8];
            this.mClientPfd = parcelFileDescriptor;
            this.mClient = parcelFileDescriptor.getFileDescriptor();
        }

        public FileBridgeOutputStream(FileDescriptor fileDescriptor) {
            this.mTemp = new byte[8];
            this.mClientPfd = null;
            this.mClient = fileDescriptor;
        }

        @Override
        public void close() throws IOException {
            try {
                writeCommandAndBlock(3, "close()");
            } finally {
                IoBridge.closeAndSignalBlockedThreads(this.mClient);
                IoUtils.closeQuietly(this.mClientPfd);
            }
        }

        public void fsync() throws IOException {
            writeCommandAndBlock(2, "fsync()");
        }

        private void writeCommandAndBlock(int i, String str) throws IOException {
            Memory.pokeInt(this.mTemp, 0, i, ByteOrder.BIG_ENDIAN);
            IoBridge.write(this.mClient, this.mTemp, 0, 8);
            if (IoBridge.read(this.mClient, this.mTemp, 0, 8) == 8 && Memory.peekInt(this.mTemp, 0, ByteOrder.BIG_ENDIAN) == i) {
                return;
            }
            throw new IOException("Failed to execute " + str + " across bridge");
        }

        @Override
        public void write(byte[] bArr, int i, int i2) throws IOException {
            Arrays.checkOffsetAndCount(bArr.length, i, i2);
            Memory.pokeInt(this.mTemp, 0, 1, ByteOrder.BIG_ENDIAN);
            Memory.pokeInt(this.mTemp, 4, i2, ByteOrder.BIG_ENDIAN);
            IoBridge.write(this.mClient, this.mTemp, 0, 8);
            IoBridge.write(this.mClient, bArr, i, i2);
        }

        @Override
        public void write(int i) throws IOException {
            Streams.writeSingleByte(this, i);
        }
    }
}
