package javax.net.ssl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import libcore.util.EmptyArray;

class DefaultSSLServerSocketFactory extends SSLServerSocketFactory {
    private final String errMessage;

    DefaultSSLServerSocketFactory(String mes) {
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
    public ServerSocket createServerSocket(int port) throws IOException {
        throw new SocketException(this.errMessage);
    }

    @Override
    public ServerSocket createServerSocket(int port, int backlog) throws IOException {
        throw new SocketException(this.errMessage);
    }

    @Override
    public ServerSocket createServerSocket(int port, int backlog, InetAddress iAddress) throws IOException {
        throw new SocketException(this.errMessage);
    }
}
