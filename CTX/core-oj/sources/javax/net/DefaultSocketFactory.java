package javax.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

class DefaultSocketFactory extends SocketFactory {
    DefaultSocketFactory() {
    }

    @Override
    public Socket createSocket() {
        return new Socket();
    }

    @Override
    public Socket createSocket(String str, int i) throws IOException {
        return new Socket(str, i);
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
        return new Socket(inetAddress, i);
    }

    @Override
    public Socket createSocket(String str, int i, InetAddress inetAddress, int i2) throws IOException {
        return new Socket(str, i, inetAddress, i2);
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress2, int i2) throws IOException {
        return new Socket(inetAddress, i, inetAddress2, i2);
    }
}
