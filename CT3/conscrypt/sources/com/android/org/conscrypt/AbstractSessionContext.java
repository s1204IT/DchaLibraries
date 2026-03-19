package com.android.org.conscrypt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionContext;

abstract class AbstractSessionContext implements SSLSessionContext {
    private static final int DEFAULT_SESSION_TIMEOUT_SECONDS = 28800;
    static final int OPEN_SSL = 1;
    volatile int maximumSize;
    volatile int timeout = DEFAULT_SESSION_TIMEOUT_SECONDS;
    final long sslCtxNativePointer = NativeCrypto.SSL_CTX_new();
    private final Map<ByteArray, SSLSession> sessions = new LinkedHashMap<ByteArray, SSLSession>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<ByteArray, SSLSession> entry) {
            boolean remove = AbstractSessionContext.this.maximumSize > 0 && size() > AbstractSessionContext.this.maximumSize;
            if (remove) {
                remove(entry.getKey());
                AbstractSessionContext.this.sessionRemoved(entry.getValue());
            }
            return false;
        }
    };

    protected abstract void sessionRemoved(SSLSession sSLSession);

    AbstractSessionContext(int maximumSize) {
        this.maximumSize = maximumSize;
    }

    private Iterator<SSLSession> sessionIterator() {
        Iterator<SSLSession> it;
        synchronized (this.sessions) {
            SSLSession[] array = (SSLSession[]) this.sessions.values().toArray(new SSLSession[this.sessions.size()]);
            it = Arrays.asList(array).iterator();
        }
        return it;
    }

    @Override
    public final Enumeration<byte[]> getIds() {
        final Iterator<SSLSession> i = sessionIterator();
        return new Enumeration<byte[]>() {
            private SSLSession next;

            @Override
            public boolean hasMoreElements() {
                if (this.next != null) {
                    return true;
                }
                while (i.hasNext()) {
                    SSLSession session = (SSLSession) i.next();
                    if (session.isValid()) {
                        this.next = session;
                        return true;
                    }
                }
                this.next = null;
                return false;
            }

            @Override
            public byte[] nextElement() {
                if (hasMoreElements()) {
                    byte[] id = this.next.getId();
                    this.next = null;
                    return id;
                }
                throw new NoSuchElementException();
            }
        };
    }

    @Override
    public final int getSessionCacheSize() {
        return this.maximumSize;
    }

    @Override
    public final int getSessionTimeout() {
        return this.timeout;
    }

    protected void trimToSize() {
        synchronized (this.sessions) {
            int size = this.sessions.size();
            if (size > this.maximumSize) {
                int removals = size - this.maximumSize;
                Iterator<SSLSession> i = this.sessions.values().iterator();
                do {
                    SSLSession session = i.next();
                    i.remove();
                    sessionRemoved(session);
                    removals--;
                } while (removals > 0);
            }
        }
    }

    @Override
    public void setSessionTimeout(int seconds) throws IllegalArgumentException {
        if (seconds < 0) {
            throw new IllegalArgumentException("seconds < 0");
        }
        this.timeout = seconds;
        synchronized (this.sessions) {
            Iterator<SSLSession> i = this.sessions.values().iterator();
            while (i.hasNext()) {
                SSLSession session = i.next();
                if (!session.isValid()) {
                    i.remove();
                    sessionRemoved(session);
                }
            }
        }
    }

    @Override
    public final void setSessionCacheSize(int size) throws IllegalArgumentException {
        if (size < 0) {
            throw new IllegalArgumentException("size < 0");
        }
        int oldMaximum = this.maximumSize;
        this.maximumSize = size;
        if (size >= oldMaximum) {
            return;
        }
        trimToSize();
    }

    byte[] toBytes(SSLSession session) {
        if (!(session instanceof OpenSSLSessionImpl)) {
            return null;
        }
        OpenSSLSessionImpl sslSession = (OpenSSLSessionImpl) session;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream daos = new DataOutputStream(baos);
            daos.writeInt(1);
            byte[] data = sslSession.getEncoded();
            daos.writeInt(data.length);
            daos.write(data);
            Certificate[] certs = session.getPeerCertificates();
            daos.writeInt(certs.length);
            for (Certificate cert : certs) {
                byte[] data2 = cert.getEncoded();
                daos.writeInt(data2.length);
                daos.write(data2);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            System.err.println("Failed to convert saved SSL Session: " + e.getMessage());
            return null;
        } catch (CertificateEncodingException e2) {
            log(e2);
            return null;
        }
    }

    OpenSSLSessionImpl toSession(byte[] data, String host, int port) {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream dais = new DataInputStream(bais);
        try {
            int type = dais.readInt();
            if (type != 1) {
                log(new AssertionError("Unexpected type ID: " + type));
                return null;
            }
            int length = dais.readInt();
            byte[] sessionData = new byte[length];
            dais.readFully(sessionData);
            int count = dais.readInt();
            X509Certificate[] certs = new X509Certificate[count];
            for (int i = 0; i < count; i++) {
                int length2 = dais.readInt();
                byte[] certData = new byte[length2];
                dais.readFully(certData);
                certs[i] = OpenSSLX509Certificate.fromX509Der(certData);
            }
            return new OpenSSLSessionImpl(sessionData, host, port, certs, this);
        } catch (IOException e) {
            log(e);
            return null;
        }
    }

    protected SSLSession wrapSSLSessionIfNeeded(SSLSession session) {
        if (session instanceof OpenSSLSessionImpl) {
            return Platform.wrapSSLSession((OpenSSLSessionImpl) session);
        }
        return session;
    }

    @Override
    public SSLSession getSession(byte[] sessionId) {
        SSLSession session;
        if (sessionId == null) {
            throw new NullPointerException("sessionId == null");
        }
        ByteArray key = new ByteArray(sessionId);
        synchronized (this.sessions) {
            session = this.sessions.get(key);
        }
        if (session == null || !session.isValid()) {
            return null;
        }
        if (session instanceof OpenSSLSessionImpl) {
            return Platform.wrapSSLSession((OpenSSLSessionImpl) session);
        }
        return session;
    }

    void putSession(SSLSession session) {
        byte[] id = session.getId();
        if (id.length == 0) {
            return;
        }
        ByteArray key = new ByteArray(id);
        synchronized (this.sessions) {
            this.sessions.put(key, session);
        }
    }

    static void log(Throwable t) {
        new Exception("Error converting session", t).printStackTrace();
    }

    protected void finalize() throws Throwable {
        try {
            NativeCrypto.SSL_CTX_free(this.sslCtxNativePointer);
        } finally {
            super.finalize();
        }
    }
}
