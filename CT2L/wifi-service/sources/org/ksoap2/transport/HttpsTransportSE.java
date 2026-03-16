package org.ksoap2.transport;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class HttpsTransportSE extends HttpTransportSE {
    static final String PROTOCOL = "https";
    private final String file;
    private final String host;
    private final int port;
    private ServiceConnection serviceConnection;
    private final int timeout;

    public HttpsTransportSE(String host, int port, String file, int timeout) {
        super("https://" + host + ":" + port + file);
        this.serviceConnection = null;
        System.out.println("Establistion connection to: https://" + host + ":" + port + file);
        this.host = host;
        this.port = port;
        this.file = file;
        this.timeout = timeout;
    }

    @Override
    public ServiceConnection getServiceConnection() throws IOException {
        if (this.serviceConnection == null) {
            this.serviceConnection = new HttpsServiceConnectionSE(this.host, this.port, this.file, this.timeout);
        }
        return this.serviceConnection;
    }

    @Override
    public String getHost() {
        try {
            String retVal = new URL(this.url).getHost();
            return retVal;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public int getPort() {
        try {
            int retVal = new URL(this.url).getPort();
            return retVal;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    @Override
    public String getPath() {
        try {
            String retVal = new URL(this.url).getPath();
            return retVal;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
