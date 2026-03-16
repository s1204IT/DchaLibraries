package javax.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

final class DefaultServerSocketFactory extends ServerSocketFactory {
    DefaultServerSocketFactory() {
    }

    @Override
    public ServerSocket createServerSocket() throws IOException {
        return new ServerSocket();
    }

    @Override
    public ServerSocket createServerSocket(int port) throws IOException {
        return new ServerSocket(port);
    }

    @Override
    public ServerSocket createServerSocket(int port, int backlog) throws IOException {
        return new ServerSocket(port, backlog);
    }

    @Override
    public ServerSocket createServerSocket(int port, int backlog, InetAddress iAddress) throws IOException {
        return new ServerSocket(port, backlog, iAddress);
    }
}
