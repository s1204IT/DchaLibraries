package java.net;

import java.io.InterruptedIOException;

public class SocketTimeoutException extends InterruptedIOException {
    private static final long serialVersionUID = -8846654841826352300L;

    public SocketTimeoutException(String str) {
        super(str);
    }

    public SocketTimeoutException() {
    }

    public SocketTimeoutException(Throwable th) {
        super(th);
    }

    public SocketTimeoutException(String str, Throwable th) {
        super(str, th);
    }
}
