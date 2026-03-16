package com.android.org.conscrypt;

import java.io.IOException;
import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionBindingEvent;
import javax.net.ssl.SSLSessionBindingListener;
import javax.net.ssl.SSLSessionContext;
import javax.security.cert.CertificateException;

public class OpenSSLSessionImpl implements SSLSession {
    private String cipherSuite;
    private long creationTime;
    private byte[] id;
    private boolean isValid;
    long lastAccessedTime;
    final X509Certificate[] localCertificates;
    private volatile javax.security.cert.X509Certificate[] peerCertificateChain;
    final X509Certificate[] peerCertificates;
    private String peerHost;
    private int peerPort;
    private String protocol;
    private AbstractSessionContext sessionContext;
    protected long sslSessionNativePointer;
    private final Map<String, Object> values;

    protected OpenSSLSessionImpl(long sslSessionNativePointer, X509Certificate[] localCertificates, X509Certificate[] peerCertificates, String peerHost, int peerPort, AbstractSessionContext sessionContext) {
        this.creationTime = 0L;
        this.lastAccessedTime = 0L;
        this.isValid = true;
        this.values = new HashMap();
        this.peerPort = -1;
        this.sslSessionNativePointer = sslSessionNativePointer;
        this.localCertificates = localCertificates;
        this.peerCertificates = peerCertificates;
        this.peerHost = peerHost;
        this.peerPort = peerPort;
        this.sessionContext = sessionContext;
    }

    OpenSSLSessionImpl(byte[] derData, String peerHost, int peerPort, X509Certificate[] peerCertificates, AbstractSessionContext sessionContext) throws IOException {
        this(NativeCrypto.d2i_SSL_SESSION(derData), null, peerCertificates, peerHost, peerPort, sessionContext);
        if (this.sslSessionNativePointer == 0) {
            throw new IOException("Invalid session data");
        }
    }

    @Override
    public byte[] getId() {
        if (this.id == null) {
            resetId();
        }
        return this.id;
    }

    void resetId() {
        this.id = NativeCrypto.SSL_SESSION_session_id(this.sslSessionNativePointer);
    }

    byte[] getEncoded() {
        return NativeCrypto.i2d_SSL_SESSION(this.sslSessionNativePointer);
    }

    @Override
    public long getCreationTime() {
        if (this.creationTime == 0) {
            this.creationTime = NativeCrypto.SSL_SESSION_get_time(this.sslSessionNativePointer);
        }
        return this.creationTime;
    }

    @Override
    public long getLastAccessedTime() {
        return this.lastAccessedTime == 0 ? getCreationTime() : this.lastAccessedTime;
    }

    @Override
    public int getApplicationBufferSize() {
        return 16384;
    }

    @Override
    public int getPacketBufferSize() {
        return 18437;
    }

    @Override
    public Principal getLocalPrincipal() {
        if (this.localCertificates == null || this.localCertificates.length <= 0) {
            return null;
        }
        return this.localCertificates[0].getSubjectX500Principal();
    }

    @Override
    public Certificate[] getLocalCertificates() {
        return this.localCertificates;
    }

    @Override
    public javax.security.cert.X509Certificate[] getPeerCertificateChain() throws SSLPeerUnverifiedException {
        checkPeerCertificatesPresent();
        javax.security.cert.X509Certificate[] result = this.peerCertificateChain;
        if (result == null) {
            javax.security.cert.X509Certificate[] result2 = createPeerCertificateChain();
            this.peerCertificateChain = result2;
            return result2;
        }
        return result;
    }

    private javax.security.cert.X509Certificate[] createPeerCertificateChain() throws SSLPeerUnverifiedException {
        try {
            javax.security.cert.X509Certificate[] chain = new javax.security.cert.X509Certificate[this.peerCertificates.length];
            for (int i = 0; i < this.peerCertificates.length; i++) {
                byte[] encoded = this.peerCertificates[i].getEncoded();
                chain[i] = javax.security.cert.X509Certificate.getInstance(encoded);
            }
            return chain;
        } catch (CertificateEncodingException e) {
            SSLPeerUnverifiedException sSLPeerUnverifiedException = new SSLPeerUnverifiedException(e.getMessage());
            sSLPeerUnverifiedException.initCause(sSLPeerUnverifiedException);
            throw sSLPeerUnverifiedException;
        } catch (CertificateException e2) {
            SSLPeerUnverifiedException sSLPeerUnverifiedException2 = new SSLPeerUnverifiedException(e2.getMessage());
            sSLPeerUnverifiedException2.initCause(sSLPeerUnverifiedException2);
            throw sSLPeerUnverifiedException2;
        }
    }

    @Override
    public Certificate[] getPeerCertificates() throws SSLPeerUnverifiedException {
        checkPeerCertificatesPresent();
        return this.peerCertificates;
    }

    private void checkPeerCertificatesPresent() throws SSLPeerUnverifiedException {
        if (this.peerCertificates == null || this.peerCertificates.length == 0) {
            throw new SSLPeerUnverifiedException("No peer certificates");
        }
    }

    @Override
    public Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
        checkPeerCertificatesPresent();
        return this.peerCertificates[0].getSubjectX500Principal();
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
    public String getCipherSuite() {
        if (this.cipherSuite == null) {
            String name = NativeCrypto.SSL_SESSION_cipher(this.sslSessionNativePointer);
            this.cipherSuite = NativeCrypto.OPENSSL_TO_STANDARD_CIPHER_SUITES.get(name);
            if (this.cipherSuite == null) {
                this.cipherSuite = name;
            }
        }
        return this.cipherSuite;
    }

    @Override
    public String getProtocol() {
        if (this.protocol == null) {
            this.protocol = NativeCrypto.SSL_SESSION_get_version(this.sslSessionNativePointer);
        }
        return this.protocol;
    }

    @Override
    public SSLSessionContext getSessionContext() {
        return this.sessionContext;
    }

    @Override
    public boolean isValid() {
        int timeoutSeconds;
        if (!this.isValid) {
            return false;
        }
        SSLSessionContext context = this.sessionContext;
        if (context != null && (timeoutSeconds = context.getSessionTimeout()) != 0) {
            long creationTimestampMillis = getCreationTime();
            long ageSeconds = (System.currentTimeMillis() - creationTimestampMillis) / 1000;
            if (ageSeconds < timeoutSeconds && ageSeconds >= 0) {
                return true;
            }
            this.isValid = false;
            return false;
        }
        return true;
    }

    @Override
    public void invalidate() {
        this.isValid = false;
        this.sessionContext = null;
    }

    @Override
    public Object getValue(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name == null");
        }
        return this.values.get(name);
    }

    @Override
    public String[] getValueNames() {
        return (String[]) this.values.keySet().toArray(new String[this.values.size()]);
    }

    @Override
    public void putValue(String name, Object value) {
        if (name == null || value == null) {
            throw new IllegalArgumentException("name == null || value == null");
        }
        Object old = this.values.put(name, value);
        if (value instanceof SSLSessionBindingListener) {
            ((SSLSessionBindingListener) value).valueBound(new SSLSessionBindingEvent(this, name));
        }
        if (old instanceof SSLSessionBindingListener) {
            ((SSLSessionBindingListener) old).valueUnbound(new SSLSessionBindingEvent(this, name));
        }
    }

    @Override
    public void removeValue(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name == null");
        }
        Object old = this.values.remove(name);
        if (old instanceof SSLSessionBindingListener) {
            SSLSessionBindingListener listener = (SSLSessionBindingListener) old;
            listener.valueUnbound(new SSLSessionBindingEvent(this, name));
        }
    }

    protected void finalize() throws Throwable {
        try {
            NativeCrypto.SSL_SESSION_free(this.sslSessionNativePointer);
        } finally {
            super.finalize();
        }
    }
}
