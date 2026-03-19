package java.util.logging;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import libcore.net.NetworkSecurityPolicy;

public class SocketHandler extends StreamHandler {
    private String host;
    private int port;
    private Socket sock;

    private void configure() {
        LogManager logManager = LogManager.getLogManager();
        String name = getClass().getName();
        setLevel(logManager.getLevelProperty(name + ".level", Level.ALL));
        setFilter(logManager.getFilterProperty(name + ".filter", null));
        setFormatter(logManager.getFormatterProperty(name + ".formatter", new XMLFormatter()));
        try {
            setEncoding(logManager.getStringProperty(name + ".encoding", null));
        } catch (Exception e) {
            try {
                setEncoding(null);
            } catch (Exception e2) {
            }
        }
        this.port = logManager.getIntProperty(name + ".port", 0);
        this.host = logManager.getStringProperty(name + ".host", null);
    }

    public SocketHandler() throws IOException {
        this.sealed = false;
        configure();
        try {
            connect();
            this.sealed = true;
        } catch (IOException e) {
            System.err.println("SocketHandler: connect failed to " + this.host + ":" + this.port);
            throw e;
        }
    }

    public SocketHandler(String str, int i) throws IOException {
        this.sealed = false;
        configure();
        this.sealed = true;
        this.port = i;
        this.host = str;
        connect();
    }

    private void connect() throws IOException {
        if (this.port == 0) {
            throw new IllegalArgumentException("Bad port: " + this.port);
        }
        if (this.host == null) {
            throw new IllegalArgumentException("Null host name: " + this.host);
        }
        if (!NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted()) {
            throw new IOException("Cleartext traffic not permitted");
        }
        this.sock = new Socket(this.host, this.port);
        setOutputStream(new BufferedOutputStream(this.sock.getOutputStream()));
    }

    @Override
    public synchronized void close() throws SecurityException {
        super.close();
        if (this.sock != null) {
            try {
                this.sock.close();
            } catch (IOException e) {
            }
        }
        this.sock = null;
    }

    @Override
    public synchronized void publish(LogRecord logRecord) {
        if (isLoggable(logRecord)) {
            super.publish(logRecord);
            flush();
        }
    }
}
