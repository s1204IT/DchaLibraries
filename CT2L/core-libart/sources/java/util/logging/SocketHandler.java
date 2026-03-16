package java.util.logging;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

public class SocketHandler extends StreamHandler {
    private static final String DEFAULT_FORMATTER = "java.util.logging.XMLFormatter";
    private static final String DEFAULT_LEVEL = "ALL";
    private Socket socket;

    public SocketHandler() throws IOException {
        super(DEFAULT_LEVEL, null, DEFAULT_FORMATTER, null);
        initSocket(LogManager.getLogManager().getProperty("java.util.logging.SocketHandler.host"), LogManager.getLogManager().getProperty("java.util.logging.SocketHandler.port"));
    }

    public SocketHandler(String host, int port) throws IOException {
        super(DEFAULT_LEVEL, null, DEFAULT_FORMATTER, null);
        initSocket(host, String.valueOf(port));
    }

    private void initSocket(String host, String port) throws IOException {
        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("host == null || host.isEmpty()");
        }
        try {
            int p = Integer.parsePositiveInt(port);
            try {
                this.socket = new Socket(host, p);
                super.internalSetOutputStream(new BufferedOutputStream(this.socket.getOutputStream()));
            } catch (IOException e) {
                getErrorManager().error("Failed to establish the network connection", e, 4);
                throw e;
            }
        } catch (NumberFormatException e2) {
            throw new IllegalArgumentException("Illegal port argument " + port);
        }
    }

    @Override
    public void close() {
        try {
            super.close();
            if (this.socket != null) {
                this.socket.close();
                this.socket = null;
            }
        } catch (Exception e) {
            getErrorManager().error("Exception occurred when closing the socket handler", e, 3);
        }
    }

    @Override
    public void publish(LogRecord record) {
        super.publish(record);
        super.flush();
    }
}
