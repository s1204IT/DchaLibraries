package gov.nist.core.net;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class DefaultNetworkLayer implements NetworkLayer {
    public static final DefaultNetworkLayer SINGLETON = new DefaultNetworkLayer();
    private SSLServerSocketFactory sslServerSocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
    private SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();

    private DefaultNetworkLayer() {
    }

    @Override
    public ServerSocket createServerSocket(int i, int i2, InetAddress inetAddress) throws IOException {
        return new ServerSocket(i, i2, inetAddress);
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
        return new Socket(inetAddress, i);
    }

    @Override
    public DatagramSocket createDatagramSocket() throws SocketException {
        return new DatagramSocket();
    }

    @Override
    public DatagramSocket createDatagramSocket(int i, InetAddress inetAddress) throws SocketException {
        if (inetAddress.isMulticastAddress()) {
            try {
                MulticastSocket multicastSocket = new MulticastSocket(i);
                multicastSocket.joinGroup(inetAddress);
                return multicastSocket;
            } catch (IOException e) {
                throw new SocketException(e.getLocalizedMessage());
            }
        }
        return new DatagramSocket(i, inetAddress);
    }

    @Override
    public SSLServerSocket createSSLServerSocket(int i, int i2, InetAddress inetAddress) throws IOException {
        return (SSLServerSocket) this.sslServerSocketFactory.createServerSocket(i, i2, inetAddress);
    }

    @Override
    public SSLSocket createSSLSocket(InetAddress inetAddress, int i) throws IOException {
        return (SSLSocket) this.sslSocketFactory.createSocket(inetAddress, i);
    }

    @Override
    public SSLSocket createSSLSocket(InetAddress inetAddress, int i, InetAddress inetAddress2) throws IOException {
        return (SSLSocket) this.sslSocketFactory.createSocket(inetAddress, i, inetAddress2, 0);
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress2) throws IOException {
        if (inetAddress2 != null) {
            return new Socket(inetAddress, i, inetAddress2, 0);
        }
        return new Socket(inetAddress, i);
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress2, int i2) throws IOException {
        if (inetAddress2 != null) {
            return new Socket(inetAddress, i, inetAddress2, i2);
        }
        if (i != 0) {
            Socket socket = new Socket();
            socket.bind(new InetSocketAddress(i));
            socket.connect(new InetSocketAddress(inetAddress, i));
            return socket;
        }
        return new Socket(inetAddress, i);
    }
}
