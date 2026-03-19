package javax.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

public abstract class SocketFactory {
    private static SocketFactory theFactory;

    public abstract Socket createSocket(String str, int i) throws IOException;

    public abstract Socket createSocket(String str, int i, InetAddress inetAddress, int i2) throws IOException;

    public abstract Socket createSocket(InetAddress inetAddress, int i) throws IOException;

    public abstract Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress2, int i2) throws IOException;

    protected SocketFactory() {
    }

    public static SocketFactory getDefault() {
        synchronized (SocketFactory.class) {
            if (theFactory == null) {
                theFactory = new DefaultSocketFactory();
            }
        }
        return theFactory;
    }

    public static void setDefault(SocketFactory socketFactory) {
        synchronized (SocketFactory.class) {
            theFactory = socketFactory;
        }
    }

    public Socket createSocket() throws IOException {
        UnsupportedOperationException unsupportedOperationException = new UnsupportedOperationException();
        SocketException socketException = new SocketException("Unconnected sockets not implemented");
        socketException.initCause(unsupportedOperationException);
        throw socketException;
    }
}
