package java.io;

import dalvik.annotation.optimization.ReachabilitySensitive;
import dalvik.system.BlockGuard;
import dalvik.system.CloseGuard;
import java.nio.channels.FileChannel;
import libcore.io.IoBridge;
import libcore.io.IoTracker;
import sun.nio.ch.FileChannelImpl;

public class FileOutputStream extends OutputStream {
    private final boolean append;
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

    private native void open0(String str, boolean z) throws FileNotFoundException;

    public FileOutputStream(String str) throws FileNotFoundException {
        this(str != null ? new File(str) : null, false);
    }

    public FileOutputStream(String str, boolean z) throws FileNotFoundException {
        this(str != null ? new File(str) : null, z);
    }

    public FileOutputStream(File file) throws FileNotFoundException {
        this(file, false);
    }

    public FileOutputStream(File file, boolean z) throws FileNotFoundException {
        this.closeLock = new Object();
        this.closed = false;
        this.guard = CloseGuard.get();
        this.tracker = new IoTracker();
        String path = file != null ? file.getPath() : null;
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkWrite(path);
        }
        if (path == null) {
            throw new NullPointerException();
        }
        if (file.isInvalid()) {
            throw new FileNotFoundException("Invalid file path");
        }
        this.fd = new FileDescriptor();
        this.isFdOwner = true;
        this.append = z;
        this.path = path;
        BlockGuard.getThreadPolicy().onWriteToDisk();
        open(path, z);
        this.guard.open("close");
    }

    public FileOutputStream(FileDescriptor fileDescriptor) {
        this(fileDescriptor, false);
    }

    public FileOutputStream(FileDescriptor fileDescriptor, boolean z) {
        this.closeLock = new Object();
        this.closed = false;
        this.guard = CloseGuard.get();
        this.tracker = new IoTracker();
        if (fileDescriptor == null) {
            throw new NullPointerException("fdObj == null");
        }
        this.fd = fileDescriptor;
        this.append = false;
        this.path = null;
        this.isFdOwner = z;
    }

    private void open(String str, boolean z) throws FileNotFoundException {
        open0(str, z);
    }

    @Override
    public void write(int i) throws IOException {
        write(new byte[]{(byte) i}, 0, 1);
    }

    @Override
    public void write(byte[] bArr) throws IOException {
        write(bArr, 0, bArr.length);
    }

    @Override
    public void write(byte[] bArr, int i, int i2) throws IOException {
        if (this.closed && i2 > 0) {
            throw new IOException("Stream Closed");
        }
        this.tracker.trackIo(i2);
        IoBridge.write(this.fd, bArr, i, i2);
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

    @ReachabilitySensitive
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
                this.channel = FileChannelImpl.open(this.fd, this.path, false, true, this.append, this);
            }
            fileChannel = this.channel;
        }
        return fileChannel;
    }

    protected void finalize() throws IOException {
        if (this.guard != null) {
            this.guard.warnIfOpen();
        }
        if (this.fd != null) {
            if (this.fd == FileDescriptor.out || this.fd == FileDescriptor.err) {
                flush();
            } else {
                close();
            }
        }
    }
}
