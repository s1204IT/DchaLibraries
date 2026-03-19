package java.security;

public class KeyManagementException extends KeyException {
    private static final long serialVersionUID = 947674216157062695L;

    public KeyManagementException() {
    }

    public KeyManagementException(String str) {
        super(str);
    }

    public KeyManagementException(String str, Throwable th) {
        super(str, th);
    }

    public KeyManagementException(Throwable th) {
        super(th);
    }
}
