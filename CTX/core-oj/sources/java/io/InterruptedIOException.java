package java.io;

public class InterruptedIOException extends IOException {
    private static final long serialVersionUID = 4020568460727500567L;
    public int bytesTransferred;

    public InterruptedIOException() {
        this.bytesTransferred = 0;
    }

    public InterruptedIOException(String str) {
        super(str);
        this.bytesTransferred = 0;
    }

    public InterruptedIOException(Throwable th) {
        super(th);
        this.bytesTransferred = 0;
    }

    public InterruptedIOException(String str, Throwable th) {
        super(str, th);
        this.bytesTransferred = 0;
    }
}
