package com.android.org.conscrypt;

import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.SSLSession;

public class ClientSessionContext extends AbstractSessionContext {
    private SSLClientSessionCache persistentCache;
    final Map<HostAndPort, SSLSession> sessionsByHostAndPort;

    @Override
    public SSLSession getSession(byte[] bArr) {
        return super.getSession(bArr);
    }

    @Override
    public void setSessionTimeout(int i) throws IllegalArgumentException {
        super.setSessionTimeout(i);
    }

    public ClientSessionContext() {
        super(10);
        this.sessionsByHostAndPort = new HashMap();
    }

    public int size() {
        return this.sessionsByHostAndPort.size();
    }

    public void setPersistentCache(SSLClientSessionCache persistentCache) {
        this.persistentCache = persistentCache;
    }

    @Override
    protected void sessionRemoved(SSLSession session) {
        String host = session.getPeerHost();
        int port = session.getPeerPort();
        if (host != null) {
            HostAndPort hostAndPortKey = new HostAndPort(host, port);
            synchronized (this.sessionsByHostAndPort) {
                this.sessionsByHostAndPort.remove(hostAndPortKey);
            }
        }
    }

    public SSLSession getSession(String host, int port) {
        SSLSession session;
        byte[] data;
        SSLSession session2;
        if (host == null) {
            return null;
        }
        HostAndPort hostAndPortKey = new HostAndPort(host, port);
        synchronized (this.sessionsByHostAndPort) {
            session = this.sessionsByHostAndPort.get(hostAndPortKey);
        }
        if (session == null || !session.isValid()) {
            if (this.persistentCache == null || (data = this.persistentCache.getSessionData(host, port)) == null || (session2 = toSession(data, host, port)) == null || !session2.isValid()) {
                return null;
            }
            super.putSession(session2);
            synchronized (this.sessionsByHostAndPort) {
                this.sessionsByHostAndPort.put(hostAndPortKey, session2);
            }
            return session2;
        }
        return session;
    }

    @Override
    public void putSession(SSLSession session) {
        byte[] data;
        super.putSession(session);
        String host = session.getPeerHost();
        int port = session.getPeerPort();
        if (host != null) {
            HostAndPort hostAndPortKey = new HostAndPort(host, port);
            synchronized (this.sessionsByHostAndPort) {
                this.sessionsByHostAndPort.put(hostAndPortKey, session);
            }
            if (this.persistentCache != null && (data = toBytes(session)) != null) {
                this.persistentCache.putSessionData(session, data);
            }
        }
    }

    static class HostAndPort {
        final String host;
        final int port;

        HostAndPort(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public int hashCode() {
            return (this.host.hashCode() * 31) + this.port;
        }

        public boolean equals(Object o) {
            if (!(o instanceof HostAndPort)) {
                return false;
            }
            HostAndPort lhs = (HostAndPort) o;
            return this.host.equals(lhs.host) && this.port == lhs.port;
        }
    }
}
