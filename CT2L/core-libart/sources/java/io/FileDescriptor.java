package java.io;

import android.system.ErrnoException;
import android.system.OsConstants;
import libcore.io.Libcore;

public final class FileDescriptor {
    private int descriptor = -1;
    public static final FileDescriptor in = new FileDescriptor();
    public static final FileDescriptor out = new FileDescriptor();
    public static final FileDescriptor err = new FileDescriptor();

    private static native boolean isSocket(int i);

    static {
        in.descriptor = OsConstants.STDIN_FILENO;
        out.descriptor = OsConstants.STDOUT_FILENO;
        err.descriptor = OsConstants.STDERR_FILENO;
    }

    public void sync() throws SyncFailedException {
        try {
            if (Libcore.os.isatty(this)) {
                Libcore.os.tcdrain(this);
            } else {
                Libcore.os.fsync(this);
            }
        } catch (ErrnoException errnoException) {
            SyncFailedException sfe = new SyncFailedException(errnoException.getMessage());
            sfe.initCause(errnoException);
            throw sfe;
        }
    }

    public boolean valid() {
        return this.descriptor != -1;
    }

    public final int getInt$() {
        return this.descriptor;
    }

    public final void setInt$(int fd) {
        this.descriptor = fd;
    }

    public boolean isSocket() {
        return isSocket(this.descriptor);
    }

    public String toString() {
        return "FileDescriptor[" + this.descriptor + "]";
    }
}
