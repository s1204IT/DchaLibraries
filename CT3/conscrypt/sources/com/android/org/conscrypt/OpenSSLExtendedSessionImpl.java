package com.android.org.conscrypt;

import java.security.Principal;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.List;
import javax.net.ssl.ExtendedSSLSession;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSessionContext;
import javax.security.cert.X509Certificate;

public class OpenSSLExtendedSessionImpl extends ExtendedSSLSession {
    private final OpenSSLSessionImpl delegate;

    public OpenSSLExtendedSessionImpl(OpenSSLSessionImpl delegate) {
        this.delegate = delegate;
    }

    public OpenSSLSessionImpl getDelegate() {
        return this.delegate;
    }

    @Override
    public String[] getLocalSupportedSignatureAlgorithms() {
        return new String[]{"SHA512withRSA", "SHA512withECDSA", "SHA384withRSA", "SHA384withECDSA", "SHA256withRSA", "SHA256withECDSA", "SHA224withRSA", "SHA224withECDSA", "SHA1withRSA", "SHA1withECDSA"};
    }

    @Override
    public String[] getPeerSupportedSignatureAlgorithms() {
        return new String[]{"SHA1withRSA", "SHA1withECDSA"};
    }

    @Override
    public List<SNIServerName> getRequestedServerNames() {
        String requestedServerName = this.delegate.getRequestedServerName();
        if (requestedServerName == null) {
            return null;
        }
        return Collections.singletonList(new SNIHostName(requestedServerName));
    }

    @Override
    public byte[] getId() {
        return this.delegate.getId();
    }

    @Override
    public SSLSessionContext getSessionContext() {
        return this.delegate.getSessionContext();
    }

    @Override
    public long getCreationTime() {
        return this.delegate.getCreationTime();
    }

    @Override
    public long getLastAccessedTime() {
        return this.delegate.getLastAccessedTime();
    }

    @Override
    public void invalidate() {
        this.delegate.invalidate();
    }

    @Override
    public boolean isValid() {
        return this.delegate.isValid();
    }

    @Override
    public void putValue(String name, Object value) {
        this.delegate.putValue(name, value);
    }

    @Override
    public Object getValue(String name) {
        return this.delegate.getValue(name);
    }

    @Override
    public void removeValue(String name) {
        this.delegate.removeValue(name);
    }

    @Override
    public String[] getValueNames() {
        return this.delegate.getValueNames();
    }

    @Override
    public Certificate[] getPeerCertificates() throws SSLPeerUnverifiedException {
        return this.delegate.getPeerCertificates();
    }

    @Override
    public Certificate[] getLocalCertificates() {
        return this.delegate.getLocalCertificates();
    }

    @Override
    public X509Certificate[] getPeerCertificateChain() throws SSLPeerUnverifiedException {
        return this.delegate.getPeerCertificateChain();
    }

    @Override
    public Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
        return this.delegate.getPeerPrincipal();
    }

    @Override
    public Principal getLocalPrincipal() {
        return this.delegate.getLocalPrincipal();
    }

    @Override
    public String getCipherSuite() {
        return this.delegate.getCipherSuite();
    }

    @Override
    public String getProtocol() {
        return this.delegate.getProtocol();
    }

    @Override
    public String getPeerHost() {
        return this.delegate.getPeerHost();
    }

    @Override
    public int getPeerPort() {
        return this.delegate.getPeerPort();
    }

    @Override
    public int getPacketBufferSize() {
        return this.delegate.getPacketBufferSize();
    }

    @Override
    public int getApplicationBufferSize() {
        return this.delegate.getApplicationBufferSize();
    }
}
