package javax.net.ssl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;

class DefaultSSLServerSocketFactory extends SSLServerSocketFactory {
    private final Exception reason;

    DefaultSSLServerSocketFactory(Exception exc) {
        this.reason = exc;
    }

    private ServerSocket throwException() throws SocketException {
        throw ((SocketException) new SocketException(this.reason.toString()).initCause(this.reason));
    }

    @Override
    public ServerSocket createServerSocket() throws IOException {
        return throwException();
    }

    @Override
    public ServerSocket createServerSocket(int i) throws IOException {
        return throwException();
    }

    @Override
    public ServerSocket createServerSocket(int i, int i2) throws IOException {
        return throwException();
    }

    @Override
    public ServerSocket createServerSocket(int i, int i2, InetAddress inetAddress) throws IOException {
        return throwException();
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return new String[0];
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return new String[0];
    }
}
