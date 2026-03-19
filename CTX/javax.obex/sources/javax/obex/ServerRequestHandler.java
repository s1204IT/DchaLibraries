package javax.obex;

public class ServerRequestHandler {
    private long mConnectionId = -1;

    protected ServerRequestHandler() {
    }

    public void setConnectionId(long j) {
        if (j < -1 || j > 4294967295L) {
            throw new IllegalArgumentException("Illegal Connection ID");
        }
        this.mConnectionId = j;
    }

    public long getConnectionId() {
        return this.mConnectionId;
    }

    public int onConnect(HeaderSet headerSet, HeaderSet headerSet2) {
        return ResponseCodes.OBEX_HTTP_OK;
    }

    public void onDisconnect(HeaderSet headerSet, HeaderSet headerSet2) {
    }

    public int onSetPath(HeaderSet headerSet, HeaderSet headerSet2, boolean z, boolean z2) {
        return ResponseCodes.OBEX_HTTP_NOT_IMPLEMENTED;
    }

    public int onDelete(HeaderSet headerSet, HeaderSet headerSet2) {
        return ResponseCodes.OBEX_HTTP_NOT_IMPLEMENTED;
    }

    public int onAbort(HeaderSet headerSet, HeaderSet headerSet2) {
        return ResponseCodes.OBEX_HTTP_NOT_IMPLEMENTED;
    }

    public int onPut(Operation operation) {
        return ResponseCodes.OBEX_HTTP_NOT_IMPLEMENTED;
    }

    public int onGet(Operation operation) {
        return ResponseCodes.OBEX_HTTP_NOT_IMPLEMENTED;
    }

    public void onAuthenticationFailure(byte[] bArr) {
    }

    public void updateStatus(String str) {
    }

    public void onClose() {
    }

    public boolean isSrmSupported() {
        return false;
    }
}
