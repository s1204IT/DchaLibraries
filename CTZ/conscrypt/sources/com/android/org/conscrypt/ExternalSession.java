package com.android.org.conscrypt;

import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSessionBindingEvent;
import javax.net.ssl.SSLSessionBindingListener;
import javax.net.ssl.SSLSessionContext;

final class ExternalSession implements SessionDecorator {
    private final Provider provider;
    private final HashMap<String, Object> values = new HashMap<>(2);

    interface Provider {
        ConscryptSession provideSession();
    }

    public ExternalSession(Provider provider) {
        this.provider = provider;
    }

    @Override
    public ConscryptSession getDelegate() {
        return this.provider.provideSession();
    }

    @Override
    public String getRequestedServerName() {
        return getDelegate().getRequestedServerName();
    }

    @Override
    public List<byte[]> getStatusResponses() {
        return getDelegate().getStatusResponses();
    }

    @Override
    public byte[] getPeerSignedCertificateTimestamp() {
        return getDelegate().getPeerSignedCertificateTimestamp();
    }

    @Override
    public byte[] getId() {
        return getDelegate().getId();
    }

    @Override
    public SSLSessionContext getSessionContext() {
        return getDelegate().getSessionContext();
    }

    @Override
    public long getCreationTime() {
        return getDelegate().getCreationTime();
    }

    @Override
    public long getLastAccessedTime() {
        return getDelegate().getLastAccessedTime();
    }

    @Override
    public void invalidate() {
        getDelegate().invalidate();
    }

    @Override
    public boolean isValid() {
        return getDelegate().isValid();
    }

    @Override
    public X509Certificate[] getPeerCertificates() throws SSLPeerUnverifiedException {
        return getDelegate().getPeerCertificates();
    }

    @Override
    public Certificate[] getLocalCertificates() {
        return getDelegate().getLocalCertificates();
    }

    @Override
    public javax.security.cert.X509Certificate[] getPeerCertificateChain() throws SSLPeerUnverifiedException {
        return getDelegate().getPeerCertificateChain();
    }

    @Override
    public Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
        return getDelegate().getPeerPrincipal();
    }

    @Override
    public Principal getLocalPrincipal() {
        return getDelegate().getLocalPrincipal();
    }

    @Override
    public String getCipherSuite() {
        return getDelegate().getCipherSuite();
    }

    @Override
    public String getProtocol() {
        return getDelegate().getProtocol();
    }

    @Override
    public String getPeerHost() {
        return getDelegate().getPeerHost();
    }

    @Override
    public int getPeerPort() {
        return getDelegate().getPeerPort();
    }

    @Override
    public int getPacketBufferSize() {
        return getDelegate().getPacketBufferSize();
    }

    @Override
    public int getApplicationBufferSize() {
        return getDelegate().getApplicationBufferSize();
    }

    @Override
    public Object getValue(String str) {
        if (str == null) {
            throw new IllegalArgumentException("name == null");
        }
        return this.values.get(str);
    }

    @Override
    public String[] getValueNames() {
        return (String[]) this.values.keySet().toArray(new String[this.values.size()]);
    }

    @Override
    public void putValue(String str, Object obj) {
        if (str == null || obj == null) {
            throw new IllegalArgumentException("name == null || value == null");
        }
        Object objPut = this.values.put(str, obj);
        if (obj instanceof SSLSessionBindingListener) {
            ((SSLSessionBindingListener) obj).valueBound(new SSLSessionBindingEvent(this, str));
        }
        if (objPut instanceof SSLSessionBindingListener) {
            ((SSLSessionBindingListener) objPut).valueUnbound(new SSLSessionBindingEvent(this, str));
        }
    }

    @Override
    public void removeValue(String str) {
        if (str == null) {
            throw new IllegalArgumentException("name == null");
        }
        Object objRemove = this.values.remove(str);
        if (objRemove instanceof SSLSessionBindingListener) {
            ((SSLSessionBindingListener) objRemove).valueUnbound(new SSLSessionBindingEvent(this, str));
        }
    }
}
