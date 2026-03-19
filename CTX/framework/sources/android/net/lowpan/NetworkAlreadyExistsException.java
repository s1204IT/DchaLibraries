package android.net.lowpan;

public class NetworkAlreadyExistsException extends LowpanException {
    public NetworkAlreadyExistsException() {
    }

    public NetworkAlreadyExistsException(String str) {
        super(str, null);
    }

    public NetworkAlreadyExistsException(String str, Throwable th) {
        super(str, th);
    }

    public NetworkAlreadyExistsException(Exception exc) {
        super(exc);
    }
}
