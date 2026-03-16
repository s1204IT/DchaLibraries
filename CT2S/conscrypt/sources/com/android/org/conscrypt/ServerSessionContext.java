package com.android.org.conscrypt;

import javax.net.ssl.SSLSession;

public class ServerSessionContext extends AbstractSessionContext {
    private SSLServerSessionCache persistentCache;

    @Override
    public void setSessionTimeout(int i) throws IllegalArgumentException {
        super.setSessionTimeout(i);
    }

    public ServerSessionContext() {
        super(100);
        NativeCrypto.SSL_CTX_set_session_id_context(this.sslCtxNativePointer, new byte[]{32});
    }

    public void setPersistentCache(SSLServerSessionCache persistentCache) {
        this.persistentCache = persistentCache;
    }

    @Override
    protected void sessionRemoved(SSLSession session) {
    }

    @Override
    public SSLSession getSession(byte[] sessionId) {
        byte[] data;
        SSLSession session;
        SSLSession session2 = super.getSession(sessionId);
        if (session2 != null) {
            return session2;
        }
        if (this.persistentCache == null || (data = this.persistentCache.getSessionData(sessionId)) == null || (session = toSession(data, null, -1)) == null || !session.isValid()) {
            return null;
        }
        super.putSession(session);
        return session;
    }

    @Override
    void putSession(SSLSession session) {
        byte[] data;
        super.putSession(session);
        if (this.persistentCache != null && (data = toBytes(session)) != null) {
            this.persistentCache.putSessionData(session, data);
        }
    }
}
