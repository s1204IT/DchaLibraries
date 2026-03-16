package com.android.org.conscrypt;

import com.android.org.conscrypt.util.EmptyArray;
import java.security.Principal;
import java.security.cert.Certificate;
import java.util.HashMap;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionBindingEvent;
import javax.net.ssl.SSLSessionBindingListener;
import javax.net.ssl.SSLSessionContext;
import javax.security.cert.X509Certificate;

public final class SSLNullSession implements SSLSession, Cloneable {
    private final HashMap<String, Object> values = new HashMap<>();
    long creationTime = System.currentTimeMillis();
    long lastAccessedTime = this.creationTime;

    private static class DefaultHolder {
        public static final SSLNullSession NULL_SESSION = new SSLNullSession();

        private DefaultHolder() {
        }
    }

    public static SSLSession getNullSession() {
        return DefaultHolder.NULL_SESSION;
    }

    @Override
    public int getApplicationBufferSize() {
        return 16384;
    }

    @Override
    public String getCipherSuite() {
        return "SSL_NULL_WITH_NULL_NULL";
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
        return 18437;
    }

    @Override
    public X509Certificate[] getPeerCertificateChain() throws SSLPeerUnverifiedException {
        throw new SSLPeerUnverifiedException("No peer certificate");
    }

    @Override
    public Certificate[] getPeerCertificates() throws SSLPeerUnverifiedException {
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
    public void invalidate() {
    }

    @Override
    public boolean isValid() {
        return false;
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
}
