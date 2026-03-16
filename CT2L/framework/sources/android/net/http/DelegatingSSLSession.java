package android.net.http;

import java.security.Principal;
import java.security.cert.Certificate;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionContext;
import javax.security.cert.X509Certificate;

public class DelegatingSSLSession implements SSLSession {
    protected DelegatingSSLSession() {
    }

    public static class CertificateWrap extends DelegatingSSLSession {
        private final Certificate mCertificate;

        public CertificateWrap(Certificate certificate) {
            this.mCertificate = certificate;
        }

        @Override
        public Certificate[] getPeerCertificates() throws SSLPeerUnverifiedException {
            return new Certificate[]{this.mCertificate};
        }
    }

    @Override
    public int getApplicationBufferSize() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getCipherSuite() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getCreationTime() {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] getId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getLastAccessedTime() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Certificate[] getLocalCertificates() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Principal getLocalPrincipal() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getPacketBufferSize() {
        throw new UnsupportedOperationException();
    }

    @Override
    public X509Certificate[] getPeerCertificateChain() throws SSLPeerUnverifiedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Certificate[] getPeerCertificates() throws SSLPeerUnverifiedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getPeerHost() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getPeerPort() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getProtocol() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SSLSessionContext getSessionContext() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getValue(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getValueNames() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void invalidate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isValid() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putValue(String name, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeValue(String name) {
        throw new UnsupportedOperationException();
    }
}
