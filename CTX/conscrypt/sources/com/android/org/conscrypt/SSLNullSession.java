package com.android.org.conscrypt;

import java.security.Principal;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.List;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionContext;
import javax.security.cert.X509Certificate;

final class SSLNullSession implements ConscryptSession, Cloneable {
    static final String INVALID_CIPHER = "SSL_NULL_WITH_NULL_NULL";
    private long creationTime;
    private long lastAccessedTime;

    private static class DefaultHolder {
        static final SSLNullSession NULL_SESSION = new SSLNullSession();

        private DefaultHolder() {
        }
    }

    static ConscryptSession getNullSession() {
        return DefaultHolder.NULL_SESSION;
    }

    static boolean isNullSession(SSLSession sSLSession) {
        return SSLUtils.unwrapSession(sSLSession) == DefaultHolder.NULL_SESSION;
    }

    private SSLNullSession() {
        this.creationTime = System.currentTimeMillis();
        this.lastAccessedTime = this.creationTime;
    }

    @Override
    public String getRequestedServerName() {
        return null;
    }

    @Override
    public List<byte[]> getStatusResponses() {
        return Collections.emptyList();
    }

    @Override
    public byte[] getPeerSignedCertificateTimestamp() {
        return EmptyArray.BYTE;
    }

    @Override
    public int getApplicationBufferSize() {
        return 16384;
    }

    @Override
    public String getCipherSuite() {
        return INVALID_CIPHER;
    }

    @Override
    public long getCreationTime() {
        return this.creationTime;
    }

    @Override
    public byte[] getId() {
        return EmptyArray.BYTE;
    }

    @Override
    public long getLastAccessedTime() {
        return this.lastAccessedTime;
    }

    @Override
    public Certificate[] getLocalCertificates() {
        return null;
    }

    @Override
    public Principal getLocalPrincipal() {
        return null;
    }

    @Override
    public int getPacketBufferSize() {
        return 16709;
    }

    @Override
    public X509Certificate[] getPeerCertificateChain() throws SSLPeerUnverifiedException {
        throw new SSLPeerUnverifiedException("No peer certificate");
    }

    @Override
    public java.security.cert.X509Certificate[] getPeerCertificates() throws SSLPeerUnverifiedException {
        throw new SSLPeerUnverifiedException("No peer certificate");
    }

    @Override
    public String getPeerHost() {
        return null;
    }

    @Override
    public int getPeerPort() {
        return -1;
    }

    @Override
    public Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
        throw new SSLPeerUnverifiedException("No peer certificate");
    }

    @Override
    public String getProtocol() {
        return "NONE";
    }

    @Override
    public SSLSessionContext getSessionContext() {
        return null;
    }

    @Override
    public Object getValue(String str) {
        throw new UnsupportedOperationException("All calls to this method should be intercepted by ProvidedSessionDecorator.");
    }

    @Override
    public String[] getValueNames() {
        throw new UnsupportedOperationException("All calls to this method should be intercepted by ProvidedSessionDecorator.");
    }

    @Override
    public void invalidate() {
    }

    @Override
    public boolean isValid() {
        return false;
    }

    @Override
    public void putValue(String str, Object obj) {
        throw new UnsupportedOperationException("All calls to this method should be intercepted by ProvidedSessionDecorator.");
    }

    @Override
    public void removeValue(String str) {
        throw new UnsupportedOperationException("All calls to this method should be intercepted by ProvidedSessionDecorator.");
    }
}
