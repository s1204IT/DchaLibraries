package com.android.org.conscrypt;

import java.util.HashMap;
import java.util.Map;

public final class ClientSessionContext extends AbstractSessionContext {
    private SSLClientSessionCache persistentCache;
    private final Map<HostAndPort, NativeSslSession> sessionsByHostAndPort;

    ClientSessionContext() {
        super(10);
        this.sessionsByHostAndPort = new HashMap();
    }

    public void setPersistentCache(SSLClientSessionCache sSLClientSessionCache) {
        this.persistentCache = sSLClientSessionCache;
    }

    NativeSslSession getCachedSession(String str, int i, SSLParametersImpl sSLParametersImpl) {
        NativeSslSession session;
        boolean z;
        if (str == null || (session = getSession(str, i)) == null) {
            return null;
        }
        String protocol = session.getProtocol();
        String[] strArr = sSLParametersImpl.enabledProtocols;
        int length = strArr.length;
        boolean z2 = false;
        int i2 = 0;
        while (true) {
            if (i2 < length) {
                if (!protocol.equals(strArr[i2])) {
                    i2++;
                } else {
                    z = true;
                    break;
                }
            } else {
                z = false;
                break;
            }
        }
        if (!z) {
            return null;
        }
        String cipherSuite = session.getCipherSuite();
        String[] strArr2 = sSLParametersImpl.enabledCipherSuites;
        int length2 = strArr2.length;
        int i3 = 0;
        while (true) {
            if (i3 >= length2) {
                break;
            }
            if (!cipherSuite.equals(strArr2[i3])) {
                i3++;
            } else {
                z2 = true;
                break;
            }
        }
        if (!z2) {
            return null;
        }
        return session;
    }

    int size() {
        return this.sessionsByHostAndPort.size();
    }

    private NativeSslSession getSession(String str, int i) {
        NativeSslSession nativeSslSession;
        byte[] sessionData;
        NativeSslSession nativeSslSessionNewInstance;
        if (str == null) {
            return null;
        }
        HostAndPort hostAndPort = new HostAndPort(str, i);
        synchronized (this.sessionsByHostAndPort) {
            nativeSslSession = this.sessionsByHostAndPort.get(hostAndPort);
        }
        if (nativeSslSession != null && nativeSslSession.isValid()) {
            return nativeSslSession;
        }
        if (this.persistentCache == null || (sessionData = this.persistentCache.getSessionData(str, i)) == null || (nativeSslSessionNewInstance = NativeSslSession.newInstance(this, sessionData, str, i)) == null || !nativeSslSessionNewInstance.isValid()) {
            return null;
        }
        synchronized (this.sessionsByHostAndPort) {
            this.sessionsByHostAndPort.put(hostAndPort, nativeSslSessionNewInstance);
        }
        return nativeSslSessionNewInstance;
    }

    @Override
    void onBeforeAddSession(NativeSslSession nativeSslSession) {
        byte[] bytes;
        String peerHost = nativeSslSession.getPeerHost();
        int peerPort = nativeSslSession.getPeerPort();
        if (peerHost == null) {
            return;
        }
        HostAndPort hostAndPort = new HostAndPort(peerHost, peerPort);
        synchronized (this.sessionsByHostAndPort) {
            this.sessionsByHostAndPort.put(hostAndPort, nativeSslSession);
        }
        if (this.persistentCache != null && (bytes = nativeSslSession.toBytes()) != null) {
            this.persistentCache.putSessionData(nativeSslSession.toSSLSession(), bytes);
        }
    }

    @Override
    void onBeforeRemoveSession(NativeSslSession nativeSslSession) {
        String peerHost = nativeSslSession.getPeerHost();
        if (peerHost == null) {
            return;
        }
        HostAndPort hostAndPort = new HostAndPort(peerHost, nativeSslSession.getPeerPort());
        synchronized (this.sessionsByHostAndPort) {
            this.sessionsByHostAndPort.remove(hostAndPort);
        }
    }

    @Override
    NativeSslSession getSessionFromPersistentCache(byte[] bArr) {
        return null;
    }

    private static final class HostAndPort {
        final String host;
        final int port;

        HostAndPort(String str, int i) {
            this.host = str;
            this.port = i;
        }

        public int hashCode() {
            return (this.host.hashCode() * 31) + this.port;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof HostAndPort)) {
                return false;
            }
            HostAndPort hostAndPort = (HostAndPort) obj;
            return this.host.equals(hostAndPort.host) && this.port == hostAndPort.port;
        }
    }
}
