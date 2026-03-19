package javax.net.ssl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

class DefaultSSLSocketFactory extends SSLSocketFactory {
    private Exception reason;

    DefaultSSLSocketFactory(Exception exc) {
        this.reason = exc;
    }

    private Socket throwException() throws SocketException {
        throw ((SocketException) new SocketException(this.reason.toString()).initCause(this.reason));
    }

    @Override
    public Socket createSocket() throws IOException {
        return throwException();
    }

    @Override
    public Socket createSocket(String str, int i) throws IOException {
        return throwException();
    }

    @Override
    public Socket createSocket(Socket socket, String str, int i, boolean z) throws IOException {
        return throwException();
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
        return throwException();
    }

    @Override
    public Socket createSocket(String str, int i, InetAddress inetAddress, int i2) throws IOException {
        return throwException();
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress2, int i2) throws IOException {
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
