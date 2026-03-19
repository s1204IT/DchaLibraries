package javax.net.ssl;

public class SSLEngineResult {
    private final int bytesConsumed;
    private final int bytesProduced;
    private final HandshakeStatus handshakeStatus;
    private final Status status;

    public enum HandshakeStatus {
        NOT_HANDSHAKING,
        FINISHED,
        NEED_TASK,
        NEED_WRAP,
        NEED_UNWRAP
    }

    public enum Status {
        BUFFER_UNDERFLOW,
        BUFFER_OVERFLOW,
        OK,
        CLOSED
    }

    public SSLEngineResult(Status status, HandshakeStatus handshakeStatus, int i, int i2) {
        if (status == null || handshakeStatus == null || i < 0 || i2 < 0) {
            throw new IllegalArgumentException("Invalid Parameter(s)");
        }
        this.status = status;
        this.handshakeStatus = handshakeStatus;
        this.bytesConsumed = i;
        this.bytesProduced = i2;
    }

    public final Status getStatus() {
        return this.status;
    }

    public final HandshakeStatus getHandshakeStatus() {
        return this.handshakeStatus;
    }

    public final int bytesConsumed() {
        return this.bytesConsumed;
    }

    public final int bytesProduced() {
        return this.bytesProduced;
    }

    public String toString() {
        return "Status = " + ((Object) this.status) + " HandshakeStatus = " + ((Object) this.handshakeStatus) + "\nbytesConsumed = " + this.bytesConsumed + " bytesProduced = " + this.bytesProduced;
    }
}
