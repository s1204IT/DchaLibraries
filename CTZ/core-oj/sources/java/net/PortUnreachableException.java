package java.net;

public class PortUnreachableException extends SocketException {
    private static final long serialVersionUID = 8462541992376507323L;

    public PortUnreachableException(String str) {
        super(str);
    }

    public PortUnreachableException() {
    }

    public PortUnreachableException(String str, Throwable th) {
        super(str, th);
    }
}
