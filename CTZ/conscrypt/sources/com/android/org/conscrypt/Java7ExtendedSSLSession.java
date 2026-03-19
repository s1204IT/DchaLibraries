package com.android.org.conscrypt;

import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;
import javax.net.ssl.ExtendedSSLSession;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSessionContext;

class Java7ExtendedSSLSession extends ExtendedSSLSession implements SessionDecorator {
    private static final String[] LOCAL_SUPPORTED_SIGNATURE_ALGORITHMS = {"SHA512withRSA", "SHA512withECDSA", "SHA384withRSA", "SHA384withECDSA", "SHA256withRSA", "SHA256withECDSA", "SHA224withRSA", "SHA224withECDSA", "SHA1withRSA", "SHA1withECDSA"};
    private static final String[] PEER_SUPPORTED_SIGNATURE_ALGORITHMS = {"SHA1withRSA", "SHA1withECDSA"};
    private final ConscryptSession delegate;

    Java7ExtendedSSLSession(ConscryptSession conscryptSession) {
        this.delegate = conscryptSession;
    }

    @Override
    public final ConscryptSession getDelegate() {
        return this.delegate;
    }

    @Override
    public final String[] getLocalSupportedSignatureAlgorithms() {
        return (String[]) LOCAL_SUPPORTED_SIGNATURE_ALGORITHMS.clone();
    }

    @Override
    public final String[] getPeerSupportedSignatureAlgorithms() {
        return (String[]) PEER_SUPPORTED_SIGNATURE_ALGORITHMS.clone();
    }

    @Override
    public final String getRequestedServerName() {
        return getDelegate().getRequestedServerName();
    }

    @Override
    public final List<byte[]> getStatusResponses() {
        return getDelegate().getStatusResponses();
    }

    @Override
    public final byte[] getPeerSignedCertificateTimestamp() {
        return getDelegate().getPeerSignedCertificateTimestamp();
    }

    @Override
    public final byte[] getId() {
        return getDelegate().getId();
    }

    @Override
    public final SSLSessionContext getSessionContext() {
        return getDelegate().getSessionContext();
    }

    @Override
    public final long getCreationTime() {
        return getDelegate().getCreationTime();
    }

    @Override
    public final long getLastAccessedTime() {
        return getDelegate().getLastAccessedTime();
    }

    @Override
    public final void invalidate() {
        getDelegate().invalidate();
    }

    @Override
    public final boolean isValid() {
        return getDelegate().isValid();
    }

    @Override
    public final void putValue(String str, Object obj) {
        getDelegate().putValue(str, obj);
    }

    @Override
    public final Object getValue(String str) {
        return getDelegate().getValue(str);
    }

    @Override
    public final void removeValue(String str) {
        getDelegate().removeValue(str);
    }

    @Override
    public final String[] getValueNames() {
        return getDelegate().getValueNames();
    }

    @Override
    public X509Certificate[] getPeerCertificates() throws SSLPeerUnverifiedException {
        return getDelegate().getPeerCertificates();
    }

    @Override
    public final Certificate[] getLocalCertificates() {
        return getDelegate().getLocalCertificates();
    }

    @Override
    public final javax.security.cert.X509Certificate[] getPeerCertificateChain() throws SSLPeerUnverifiedException {
        return getDelegate().getPeerCertificateChain();
    }

    @Override
    public final Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
        return getDelegate().getPeerPrincipal();
    }

    @Override
    public final Principal getLocalPrincipal() {
        return getDelegate().getLocalPrincipal();
    }

    @Override
    public final String getCipherSuite() {
        return getDelegate().getCipherSuite();
    }

    @Override
    public final String getProtocol() {
        return getDelegate().getProtocol();
    }

    @Override
    public final String getPeerHost() {
        return getDelegate().getPeerHost();
    }

    @Override
    public final int getPeerPort() {
        return getDelegate().getPeerPort();
    }

    @Override
    public final int getPacketBufferSize() {
        return getDelegate().getPacketBufferSize();
    }

    @Override
    public final int getApplicationBufferSize() {
        return getDelegate().getApplicationBufferSize();
    }
}
