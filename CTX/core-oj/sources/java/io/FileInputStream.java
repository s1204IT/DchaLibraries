package java.io;

import dalvik.annotation.optimization.ReachabilitySensitive;
import dalvik.system.BlockGuard;
import dalvik.system.CloseGuard;
import java.nio.channels.FileChannel;
import libcore.io.IoBridge;
import libcore.io.IoTracker;
import sun.nio.ch.FileChannelImpl;

public class FileInputStream extends InputStream {
    private FileChannel channel;
    private final Object closeLock;
    private volatile boolean closed;

    @ReachabilitySensitive
    private final FileDescriptor fd;

    @ReachabilitySensitive
    private final CloseGuard guard;
    private final boolean isFdOwner;
    private final String path;
    private final IoTracker tracker;

    private native int available0() throws IOException;

    private native void open0(String str) throws FileNotFoundException;

    private native long skip0(long j) throws UseManualSkipException, IOException;

    public FileInputStream(String str) throws FileNotFoundException {
        this(str != null ? new File(str) : null);
    }

    public FileInputStream(File file) throws FileNotFoundException {
        this.channel = null;
        this.closeLock = new Object();
        this.closed = false;
        this.guard = CloseGuard.get();
        this.tracker = new IoTracker();
        String path = file != null ? file.getPath() : null;
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkRead(path);
        }
        if (path == null) {
            throw new NullPointerException();
        }
        if (file.isInvalid()) {
            throw new FileNotFoundException("Invalid file path");
        }
        this.fd = new FileDescriptor();
        this.isFdOwner = true;
        this.path = path;
        BlockGuard.getThreadPolicy().onReadFromDisk();
        open(path);
        this.guard.open("close");
    }

    public FileInputStream(FileDescriptor fileDescriptor) {
        this(fileDescriptor, false);
    }

    public FileInputStream(FileDescriptor fileDescriptor, boolean z) {
        this.channel = null;
        this.closeLock = new Object();
        this.closed = false;
        this.guard = CloseGuard.get();
        this.tracker = new IoTracker();
        if (fileDescriptor == null) {
            throw new NullPointerException("fdObj == null");
        }
        this.fd = fileDescriptor;
        this.path = null;
        this.isFdOwner = z;
    }

    private void open(String str) throws FileNotFoundException {
        open0(str);
    }

    @Override
    public int read() throws IOException {
        byte[] bArr = new byte[1];
        if (read(bArr, 0, 1) != -1) {
            return bArr[0] & Character.DIRECTIONALITY_UNDEFINED;
        }
        return -1;
    }

    @Override
    public int read(byte[] bArr) throws IOException {
        return read(bArr, 0, bArr.length);
    }

    @Override
    public int read(byte[] bArr, int i, int i2) throws IOException {
        if (this.closed && i2 > 0) {
            throw new IOException("Stream Closed");
        }
        this.tracker.trackIo(i2);
        return IoBridge.read(this.fd, bArr, i, i2);
    }

    @Override
    public long skip(long j) throws IOException {
        if (this.closed) {
            throw new IOException("Stream Closed");
        }
        try {
            BlockGuard.getThreadPolicy().onReadFromDisk();
            return skip0(j);
        } catch (UseManualSkipException e) {
            return super.skip(j);
        }
    }

    private static class UseManualSkipException extends Exception {
        private UseManualSkipException() {
        }
    }

    @Override
    public int available() throws IOException {
        if (this.closed) {
            throw new IOException("Stream Closed");
        }
        return available0();
    }

    @Override
    public void close() throws IOException {
        synchronized (this.closeLock) {
            if (this.closed) {
                return;
            }
            this.closed = true;
            this.guard.close();
            if (this.channel != null) {
                this.channel.close();
            }
            if (this.isFdOwner) {
                IoBridge.closeAndSignalBlockedThreads(this.fd);
            }
        }
    }

    public final FileDescriptor getFD() throws IOException {
        if (this.fd != null) {
            return this.fd;
        }
        throw new IOException();
    }

    public FileChannel getChannel() {
        FileChannel fileChannel;
        synchronized (this) {
            if (this.channel == null) {
                this.channel = FileChannelImpl.open(this.fd, this.path, true, false, this);
            }
            fileChannel = this.channel;
        }
        return fileChannel;
    }

    protected void finalize() throws IOException {
        if (this.guard != null) {
            this.guard.warnIfOpen();
        }
        if (this.fd != null && this.fd != FileDescriptor.in) {
            close();
        }
    }
}
