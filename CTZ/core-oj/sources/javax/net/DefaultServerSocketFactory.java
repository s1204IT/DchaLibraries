package javax.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

class DefaultServerSocketFactory extends ServerSocketFactory {
    DefaultServerSocketFactory() {
    }

    @Override
    public ServerSocket createServerSocket() throws IOException {
        return new ServerSocket();
    }

    @Override
    public ServerSocket createServerSocket(int i) throws IOException {
        return new ServerSocket(i);
    }

    @Override
    public ServerSocket createServerSocket(int i, int i2) throws IOException {
        return new ServerSocket(i, i2);
    }

    @Override
    public ServerSocket createServerSocket(int i, int i2, InetAddress inetAddress) throws IOException {
        return new ServerSocket(i, i2, inetAddress);
    }
}
