package gov.nist.core.net;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.sip.ListeningPoint;

public class SslNetworkLayer implements NetworkLayer {
    private SSLServerSocketFactory sslServerSocketFactory;
    private SSLSocketFactory sslSocketFactory;

    public SslNetworkLayer(String str, String str2, char[] cArr, String str3) throws GeneralSecurityException, IOException {
        SSLContext sSLContext = SSLContext.getInstance(ListeningPoint.TLS);
        String defaultAlgorithm = KeyManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(defaultAlgorithm);
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(defaultAlgorithm);
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextInt();
        KeyStore keyStore = KeyStore.getInstance(str3);
        KeyStore keyStore2 = KeyStore.getInstance(str3);
        keyStore.load(new FileInputStream(str2), cArr);
        keyStore2.load(new FileInputStream(str), cArr);
        trustManagerFactory.init(keyStore2);
        keyManagerFactory.init(keyStore, cArr);
        sSLContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), secureRandom);
        this.sslServerSocketFactory = sSLContext.getServerSocketFactory();
        this.sslSocketFactory = sSLContext.getSocketFactory();
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
