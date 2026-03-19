package android.system;

import java.io.IOException;
import java.net.SocketException;
import libcore.io.Libcore;

public final class ErrnoException extends Exception {
    public final int errno;
    private final String functionName;

    public ErrnoException(String str, int i) {
        this.functionName = str;
        this.errno = i;
    }

    public ErrnoException(String str, int i, Throwable th) {
        super(th);
        this.functionName = str;
        this.errno = i;
    }

    @Override
    public String getMessage() {
        String strErrnoName = OsConstants.errnoName(this.errno);
        if (strErrnoName == null) {
            strErrnoName = "errno " + this.errno;
        }
        return this.functionName + " failed: " + strErrnoName + " (" + Libcore.os.strerror(this.errno) + ")";
    }

    public IOException rethrowAsIOException() throws IOException {
        IOException iOException = new IOException(getMessage());
        iOException.initCause(this);
        throw iOException;
    }

    public SocketException rethrowAsSocketException() throws SocketException {
        throw new SocketException(getMessage(), this);
    }
}
