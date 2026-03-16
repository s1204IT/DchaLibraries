package javax.net.ssl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import libcore.util.EmptyArray;

class DefaultSSLSocketFactory extends SSLSocketFactory {
    private final String errMessage;

    DefaultSSLSocketFactory(String mes) {
        this.errMessage = mes;
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return EmptyArray.STRING;
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return EmptyArray.STRING;
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        throw new SocketException(this.errMessage);
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        throw new SocketException(this.errMessage);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        throw new SocketException(this.errMessage);
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        throw new SocketException(this.errMessage);
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        throw new SocketException(this.errMessage);
    }
}
