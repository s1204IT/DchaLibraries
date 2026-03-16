package javax.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

final class DefaultSocketFactory extends SocketFactory {
    DefaultSocketFactory() {
    }

    @Override
    public Socket createSocket() throws IOException {
        return new Socket();
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return new Socket(host, port);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        return new Socket(host, port, localHost, localPort);
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return new Socket(host, port);
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return new Socket(address, port, localAddress, localPort);
    }
}
