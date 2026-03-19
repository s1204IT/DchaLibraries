package android.system;

import java.net.UnknownHostException;
import libcore.io.Libcore;

public final class GaiException extends RuntimeException {
    public final int error;
    private final String functionName;

    public GaiException(String str, int i) {
        this.functionName = str;
        this.error = i;
    }

    public GaiException(String str, int i, Throwable th) {
        super(th);
        this.functionName = str;
        this.error = i;
    }

    @Override
    public String getMessage() {
        String strGaiName = OsConstants.gaiName(this.error);
        if (strGaiName == null) {
            strGaiName = "GAI_ error " + this.error;
        }
        return this.functionName + " failed: " + strGaiName + " (" + Libcore.os.gai_strerror(this.error) + ")";
    }

    public UnknownHostException rethrowAsUnknownHostException(String str) throws UnknownHostException {
        UnknownHostException unknownHostException = new UnknownHostException(str);
        unknownHostException.initCause(this);
        throw unknownHostException;
    }

    public UnknownHostException rethrowAsUnknownHostException() throws UnknownHostException {
        throw rethrowAsUnknownHostException(getMessage());
    }
}
