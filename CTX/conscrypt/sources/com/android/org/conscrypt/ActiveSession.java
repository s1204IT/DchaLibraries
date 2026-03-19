package com.android.org.conscrypt;

import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSessionBindingEvent;
import javax.net.ssl.SSLSessionBindingListener;
import javax.net.ssl.SSLSessionContext;

final class ActiveSession implements ConscryptSession {
    private long creationTime;
    private byte[] id;
    private X509Certificate[] localCertificates;
    private volatile javax.security.cert.X509Certificate[] peerCertificateChain;
    private byte[] peerCertificateOcspData;
    private X509Certificate[] peerCertificates;
    private String peerHost;
    private byte[] peerTlsSctData;
    private String protocol;
    private AbstractSessionContext sessionContext;
    private final NativeSsl ssl;
    private int peerPort = -1;
    private long lastAccessedTime = 0;

    ActiveSession(NativeSsl nativeSsl, AbstractSessionContext abstractSessionContext) {
        this.ssl = (NativeSsl) Preconditions.checkNotNull(nativeSsl, "ssl");
        this.sessionContext = (AbstractSessionContext) Preconditions.checkNotNull(abstractSessionContext, "sessionContext");
    }

    @Override
    public byte[] getId() {
        if (this.id == null) {
            synchronized (this.ssl) {
                this.id = this.ssl.getSessionId();
            }
        }
        return this.id != null ? (byte[]) this.id.clone() : EmptyArray.BYTE;
    }

    void resetId() {
        this.id = null;
    }

    @Override
    public SSLSessionContext getSessionContext() {
        if (isValid()) {
            return this.sessionContext;
        }
        return null;
    }

    @Override
    public long getCreationTime() {
        if (this.creationTime == 0) {
            synchronized (this.ssl) {
                this.creationTime = this.ssl.getTime();
            }
        }
        return this.creationTime;
    }

    @Override
    public long getLastAccessedTime() {
        return this.lastAccessedTime == 0 ? getCreationTime() : this.lastAccessedTime;
    }

    void setLastAccessedTime(long j) {
        this.lastAccessedTime = j;
    }

    @Override
    public List<byte[]> getStatusResponses() {
        if (this.peerCertificateOcspData == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(this.peerCertificateOcspData.clone());
    }

    @Override
    public byte[] getPeerSignedCertificateTimestamp() {
        if (this.peerTlsSctData == null) {
            return null;
        }
        return (byte[]) this.peerTlsSctData.clone();
    }

    @Override
    public String getRequestedServerName() {
        String requestedServerName;
        synchronized (this.ssl) {
            requestedServerName = this.ssl.getRequestedServerName();
        }
        return requestedServerName;
    }

    @Override
    public void invalidate() {
        synchronized (this.ssl) {
            this.ssl.setTimeout(0L);
        }
    }

    @Override
    public boolean isValid() {
        boolean z;
        synchronized (this.ssl) {
            z = System.currentTimeMillis() - this.ssl.getTimeout() < this.ssl.getTime();
        }
        return z;
    }

    @Override
    public void putValue(String str, Object obj) {
        throw new UnsupportedOperationException("All calls to this method should be intercepted by ProvidedSessionDecorator.");
    }

    @Override
    public Object getValue(String str) {
        throw new UnsupportedOperationException("All calls to this method should be intercepted by ProvidedSessionDecorator.");
    }

    @Override
    public void removeValue(String str) {
        throw new UnsupportedOperationException("All calls to this method should be intercepted by ProvidedSessionDecorator.");
    }

    @Override
    public String[] getValueNames() {
        throw new UnsupportedOperationException("All calls to this method should be intercepted by ProvidedSessionDecorator.");
    }

    @Override
    public X509Certificate[] getPeerCertificates() throws SSLPeerUnverifiedException {
        checkPeerCertificatesPresent();
        return (X509Certificate[]) this.peerCertificates.clone();
    }

    @Override
    public Certificate[] getLocalCertificates() {
        if (this.localCertificates == null) {
            return null;
        }
        return (X509Certificate[]) this.localCertificates.clone();
    }

    @Override
    public javax.security.cert.X509Certificate[] getPeerCertificateChain() throws SSLPeerUnverifiedException {
        checkPeerCertificatesPresent();
        javax.security.cert.X509Certificate[] x509CertificateArr = this.peerCertificateChain;
        if (x509CertificateArr == null) {
            javax.security.cert.X509Certificate[] certificateChain = SSLUtils.toCertificateChain(this.peerCertificates);
            this.peerCertificateChain = certificateChain;
            return certificateChain;
        }
        return x509CertificateArr;
    }

    @Override
    public Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
        checkPeerCertificatesPresent();
        return this.peerCertificates[0].getSubjectX500Principal();
    }

    @Override
    public Principal getLocalPrincipal() {
        if (this.localCertificates != null && this.localCertificates.length > 0) {
            return this.localCertificates[0].getSubjectX500Principal();
        }
        return null;
    }

    @Override
    public String getCipherSuite() {
        String cipherSuite;
        synchronized (this.ssl) {
            cipherSuite = this.ssl.getCipherSuite();
        }
        return cipherSuite == null ? "SSL_NULL_WITH_NULL_NULL" : cipherSuite;
    }

    @Override
    public String getProtocol() {
        String version = this.protocol;
        if (version == null) {
            synchronized (this.ssl) {
                version = this.ssl.getVersion();
            }
            this.protocol = version;
        }
        return version;
    }

    @Override
    public String getPeerHost() {
        return this.peerHost;
    }

    @Override
    public int getPeerPort() {
        return this.peerPort;
    }

    @Override
    public int getPacketBufferSize() {
        return 16709;
    }

    @Override
    public int getApplicationBufferSize() {
        return 16384;
    }

    void onPeerCertificatesReceived(String str, int i, X509Certificate[] x509CertificateArr) {
        configurePeer(str, i, x509CertificateArr);
    }

    private void configurePeer(String str, int i, X509Certificate[] x509CertificateArr) {
        this.peerHost = str;
        this.peerPort = i;
        this.peerCertificates = x509CertificateArr;
        synchronized (this.ssl) {
            this.peerCertificateOcspData = this.ssl.getPeerCertificateOcspData();
            this.peerTlsSctData = this.ssl.getPeerTlsSctData();
        }
    }

    void onPeerCertificateAvailable(String str, int i) throws CertificateException {
        synchronized (this.ssl) {
            this.id = null;
            this.localCertificates = this.ssl.getLocalCertificates();
            if (this.peerCertificates == null) {
                configurePeer(str, i, this.ssl.getPeerCertificates());
            }
        }
    }

    private void checkPeerCertificatesPresent() throws SSLPeerUnverifiedException {
        if (this.peerCertificates == null || this.peerCertificates.length == 0) {
            throw new SSLPeerUnverifiedException("No peer certificates");
        }
    }

    private void notifyUnbound(Object obj, String str) {
        if (obj instanceof SSLSessionBindingListener) {
            ((SSLSessionBindingListener) obj).valueUnbound(new SSLSessionBindingEvent(this, str));
        }
    }
}
