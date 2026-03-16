package java.io;

import android.system.ErrnoException;
import android.system.OsConstants;
import dalvik.system.CloseGuard;
import java.nio.NioUtils;
import java.nio.channels.FileChannel;
import libcore.io.IoBridge;
import libcore.io.Libcore;
import libcore.io.Streams;

public class FileInputStream extends InputStream {
    private FileChannel channel;
    private FileDescriptor fd;
    private final CloseGuard guard;
    private final boolean shouldClose;

    public FileInputStream(File file) throws FileNotFoundException {
        this.guard = CloseGuard.get();
        if (file == null) {
            throw new NullPointerException("file == null");
        }
        this.fd = IoBridge.open(file.getPath(), OsConstants.O_RDONLY);
        this.shouldClose = true;
        this.guard.open("close");
    }

    public FileInputStream(FileDescriptor fd) {
        this.guard = CloseGuard.get();
        if (fd == null) {
            throw new NullPointerException("fd == null");
        }
        this.fd = fd;
        this.shouldClose = false;
    }

    public FileInputStream(String path) throws FileNotFoundException {
        this(new File(path));
    }

    @Override
    public int available() throws IOException {
        return IoBridge.available(this.fd);
    }

    @Override
    public void close() throws IOException {
        this.guard.close();
        synchronized (this) {
            if (this.channel != null) {
                this.channel.close();
            }
            if (this.shouldClose) {
                IoBridge.closeAndSignalBlockedThreads(this.fd);
            } else {
                this.fd = new FileDescriptor();
            }
        }
    }

    protected void finalize() throws IOException {
        AssertionError assertionError;
        try {
            if (this.guard != null) {
                this.guard.warnIfOpen();
            }
            close();
            try {
                super.finalize();
            } finally {
            }
        } catch (Throwable th) {
            try {
                super.finalize();
                throw th;
            } finally {
            }
        }
    }

    public FileChannel getChannel() {
        FileChannel fileChannel;
        synchronized (this) {
            if (this.channel == null) {
                this.channel = NioUtils.newFileChannel(this, this.fd, OsConstants.O_RDONLY);
            }
            fileChannel = this.channel;
        }
        return fileChannel;
    }

    public final FileDescriptor getFD() throws IOException {
        return this.fd;
    }

    @Override
    public int read() throws IOException {
        return Streams.readSingleByte(this);
    }

    @Override
    public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
        return IoBridge.read(this.fd, buffer, byteOffset, byteCount);
    }

    @Override
    public long skip(long byteCount) throws IOException {
        if (byteCount < 0) {
            throw new IOException("byteCount < 0: " + byteCount);
        }
        try {
            Libcore.os.lseek(this.fd, byteCount, OsConstants.SEEK_CUR);
            return byteCount;
        } catch (ErrnoException errnoException) {
            if (errnoException.errno == OsConstants.ESPIPE) {
                return super.skip(byteCount);
            }
            throw errnoException.rethrowAsIOException();
        }
    }
}
