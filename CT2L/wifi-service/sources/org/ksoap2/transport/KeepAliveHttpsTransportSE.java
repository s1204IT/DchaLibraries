package org.ksoap2.transport;

import java.io.IOException;

public class KeepAliveHttpsTransportSE extends HttpsTransportSE {
    private final String file;
    private final String host;
    private final int port;
    private ServiceConnection serviceConnection;
    private final int timeout;

    public KeepAliveHttpsTransportSE(String host, int port, String file, int timeout) {
        super(host, port, file, timeout);
        this.host = host;
        this.port = port;
        this.file = file;
        this.timeout = timeout;
    }

    @Override
    public ServiceConnection getServiceConnection() throws IOException {
        if (this.serviceConnection == null) {
            this.serviceConnection = new HttpsServiceConnectionSEIgnoringConnectionClose(this.host, this.port, this.file, this.timeout);
            this.serviceConnection.setRequestProperty("Connection", "keep-alive");
        }
        return this.serviceConnection;
    }
}
