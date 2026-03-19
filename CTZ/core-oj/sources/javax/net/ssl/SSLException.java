package javax.net.ssl;

import java.io.IOException;

public class SSLException extends IOException {
    private static final long serialVersionUID = 4511006460650708967L;

    public SSLException(String str) {
        super(str);
    }

    public SSLException(String str, Throwable th) {
        super(str);
        initCause(th);
    }

    public SSLException(Throwable th) {
        super(th == null ? null : th.toString());
        initCause(th);
    }
}
