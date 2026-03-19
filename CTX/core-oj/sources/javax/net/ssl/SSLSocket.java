package javax.net.ssl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public abstract class SSLSocket extends Socket {
    public abstract void addHandshakeCompletedListener(HandshakeCompletedListener handshakeCompletedListener);

    public abstract boolean getEnableSessionCreation();

    public abstract String[] getEnabledCipherSuites();

    public abstract String[] getEnabledProtocols();

    public abstract boolean getNeedClientAuth();

    public abstract SSLSession getSession();

    public abstract String[] getSupportedCipherSuites();

    public abstract String[] getSupportedProtocols();

    public abstract boolean getUseClientMode();

    public abstract boolean getWantClientAuth();

    public abstract void removeHandshakeCompletedListener(HandshakeCompletedListener handshakeCompletedListener);

    public abstract void setEnableSessionCreation(boolean z);

    public abstract void setEnabledCipherSuites(String[] strArr);

    public abstract void setEnabledProtocols(String[] strArr);

    public abstract void setNeedClientAuth(boolean z);

    public abstract void setUseClientMode(boolean z);

    public abstract void setWantClientAuth(boolean z);

    public abstract void startHandshake() throws IOException;

    protected SSLSocket() {
    }

    protected SSLSocket(String str, int i) throws IOException {
        super(str, i);
    }

    protected SSLSocket(InetAddress inetAddress, int i) throws IOException {
        super(inetAddress, i);
    }

    protected SSLSocket(String str, int i, InetAddress inetAddress, int i2) throws IOException {
        super(str, i, inetAddress, i2);
    }

    protected SSLSocket(InetAddress inetAddress, int i, InetAddress inetAddress2, int i2) throws IOException {
        super(inetAddress, i, inetAddress2, i2);
    }

    public SSLSession getHandshakeSession() {
        throw new UnsupportedOperationException();
    }

    public SSLParameters getSSLParameters() {
        SSLParameters sSLParameters = new SSLParameters();
        sSLParameters.setCipherSuites(getEnabledCipherSuites());
        sSLParameters.setProtocols(getEnabledProtocols());
        if (getNeedClientAuth()) {
            sSLParameters.setNeedClientAuth(true);
        } else if (getWantClientAuth()) {
            sSLParameters.setWantClientAuth(true);
        }
        return sSLParameters;
    }

    public void setSSLParameters(SSLParameters sSLParameters) {
        String[] cipherSuites = sSLParameters.getCipherSuites();
        if (cipherSuites != null) {
            setEnabledCipherSuites(cipherSuites);
        }
        String[] protocols = sSLParameters.getProtocols();
        if (protocols != null) {
            setEnabledProtocols(protocols);
        }
        if (sSLParameters.getNeedClientAuth()) {
            setNeedClientAuth(true);
        } else if (sSLParameters.getWantClientAuth()) {
            setWantClientAuth(true);
        } else {
            setWantClientAuth(false);
        }
    }

    @Override
    public String toString() {
        return "SSL" + super.toString();
    }
}
