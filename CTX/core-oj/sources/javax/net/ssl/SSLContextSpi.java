package javax.net.ssl;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.SecureRandom;

public abstract class SSLContextSpi {
    protected abstract SSLEngine engineCreateSSLEngine();

    protected abstract SSLEngine engineCreateSSLEngine(String str, int i);

    protected abstract SSLSessionContext engineGetClientSessionContext();

    protected abstract SSLSessionContext engineGetServerSessionContext();

    protected abstract SSLServerSocketFactory engineGetServerSocketFactory();

    protected abstract SSLSocketFactory engineGetSocketFactory();

    protected abstract void engineInit(KeyManager[] keyManagerArr, TrustManager[] trustManagerArr, SecureRandom secureRandom) throws KeyManagementException;

    private SSLSocket getDefaultSocket() {
        try {
            return (SSLSocket) engineGetSocketFactory().createSocket();
        } catch (IOException e) {
            throw new UnsupportedOperationException("Could not obtain parameters", e);
        }
    }

    protected SSLParameters engineGetDefaultSSLParameters() {
        return getDefaultSocket().getSSLParameters();
    }

    protected SSLParameters engineGetSupportedSSLParameters() {
        SSLSocket defaultSocket = getDefaultSocket();
        SSLParameters sSLParameters = new SSLParameters();
        sSLParameters.setCipherSuites(defaultSocket.getSupportedCipherSuites());
        sSLParameters.setProtocols(defaultSocket.getSupportedProtocols());
        return sSLParameters;
    }
}
