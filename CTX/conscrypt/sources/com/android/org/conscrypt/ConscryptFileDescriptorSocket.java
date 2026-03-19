package com.android.org.conscrypt;

import com.android.org.conscrypt.ExternalSession;
import com.android.org.conscrypt.NativeCrypto;
import com.android.org.conscrypt.NativeRef;
import com.android.org.conscrypt.SSLParametersImpl;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECKey;
import java.security.spec.ECParameterSpec;
import javax.crypto.SecretKey;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLProtocolException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;

class ConscryptFileDescriptorSocket extends OpenSSLSocketImpl implements NativeCrypto.SSLHandshakeCallbacks, SSLParametersImpl.AliasChooser, SSLParametersImpl.PSKCallbacks {
    private static final boolean DBG_STATE = false;
    private final ActiveSession activeSession;
    private OpenSSLKey channelIdPrivateKey;
    private SessionSnapshot closedSession;
    private final SSLSession externalSession;
    private final Object guard;
    private int handshakeTimeoutMilliseconds;
    private SSLInputStream is;
    private SSLOutputStream os;
    private final NativeSsl ssl;
    private final SSLParametersImpl sslParameters;
    private int state;
    private int writeTimeoutMilliseconds;

    ConscryptFileDescriptorSocket(SSLParametersImpl sSLParametersImpl) throws IOException {
        this.state = 0;
        this.guard = Platform.closeGuardGet();
        this.externalSession = Platform.wrapSSLSession(new ExternalSession(new ExternalSession.Provider() {
            @Override
            public ConscryptSession provideSession() {
                return ConscryptFileDescriptorSocket.this.provideSession();
            }
        }));
        this.writeTimeoutMilliseconds = 0;
        this.handshakeTimeoutMilliseconds = -1;
        this.sslParameters = sSLParametersImpl;
        this.ssl = newSsl(sSLParametersImpl, this);
        this.activeSession = new ActiveSession(this.ssl, sSLParametersImpl.getSessionContext());
    }

    ConscryptFileDescriptorSocket(String str, int i, SSLParametersImpl sSLParametersImpl) throws IOException {
        super(str, i);
        this.state = 0;
        this.guard = Platform.closeGuardGet();
        this.externalSession = Platform.wrapSSLSession(new ExternalSession(new ExternalSession.Provider() {
            @Override
            public ConscryptSession provideSession() {
                return ConscryptFileDescriptorSocket.this.provideSession();
            }
        }));
        this.writeTimeoutMilliseconds = 0;
        this.handshakeTimeoutMilliseconds = -1;
        this.sslParameters = sSLParametersImpl;
        this.ssl = newSsl(sSLParametersImpl, this);
        this.activeSession = new ActiveSession(this.ssl, sSLParametersImpl.getSessionContext());
    }

    ConscryptFileDescriptorSocket(InetAddress inetAddress, int i, SSLParametersImpl sSLParametersImpl) throws IOException {
        super(inetAddress, i);
        this.state = 0;
        this.guard = Platform.closeGuardGet();
        this.externalSession = Platform.wrapSSLSession(new ExternalSession(new ExternalSession.Provider() {
            @Override
            public ConscryptSession provideSession() {
                return ConscryptFileDescriptorSocket.this.provideSession();
            }
        }));
        this.writeTimeoutMilliseconds = 0;
        this.handshakeTimeoutMilliseconds = -1;
        this.sslParameters = sSLParametersImpl;
        this.ssl = newSsl(sSLParametersImpl, this);
        this.activeSession = new ActiveSession(this.ssl, sSLParametersImpl.getSessionContext());
    }

    ConscryptFileDescriptorSocket(String str, int i, InetAddress inetAddress, int i2, SSLParametersImpl sSLParametersImpl) throws IOException {
        super(str, i, inetAddress, i2);
        this.state = 0;
        this.guard = Platform.closeGuardGet();
        this.externalSession = Platform.wrapSSLSession(new ExternalSession(new ExternalSession.Provider() {
            @Override
            public ConscryptSession provideSession() {
                return ConscryptFileDescriptorSocket.this.provideSession();
            }
        }));
        this.writeTimeoutMilliseconds = 0;
        this.handshakeTimeoutMilliseconds = -1;
        this.sslParameters = sSLParametersImpl;
        this.ssl = newSsl(sSLParametersImpl, this);
        this.activeSession = new ActiveSession(this.ssl, sSLParametersImpl.getSessionContext());
    }

    ConscryptFileDescriptorSocket(InetAddress inetAddress, int i, InetAddress inetAddress2, int i2, SSLParametersImpl sSLParametersImpl) throws IOException {
        super(inetAddress, i, inetAddress2, i2);
        this.state = 0;
        this.guard = Platform.closeGuardGet();
        this.externalSession = Platform.wrapSSLSession(new ExternalSession(new ExternalSession.Provider() {
            @Override
            public ConscryptSession provideSession() {
                return ConscryptFileDescriptorSocket.this.provideSession();
            }
        }));
        this.writeTimeoutMilliseconds = 0;
        this.handshakeTimeoutMilliseconds = -1;
        this.sslParameters = sSLParametersImpl;
        this.ssl = newSsl(sSLParametersImpl, this);
        this.activeSession = new ActiveSession(this.ssl, sSLParametersImpl.getSessionContext());
    }

    ConscryptFileDescriptorSocket(Socket socket, String str, int i, boolean z, SSLParametersImpl sSLParametersImpl) throws IOException {
        super(socket, str, i, z);
        this.state = 0;
        this.guard = Platform.closeGuardGet();
        this.externalSession = Platform.wrapSSLSession(new ExternalSession(new ExternalSession.Provider() {
            @Override
            public ConscryptSession provideSession() {
                return ConscryptFileDescriptorSocket.this.provideSession();
            }
        }));
        this.writeTimeoutMilliseconds = 0;
        this.handshakeTimeoutMilliseconds = -1;
        this.sslParameters = sSLParametersImpl;
        this.ssl = newSsl(sSLParametersImpl, this);
        this.activeSession = new ActiveSession(this.ssl, sSLParametersImpl.getSessionContext());
    }

    private static NativeSsl newSsl(SSLParametersImpl sSLParametersImpl, ConscryptFileDescriptorSocket conscryptFileDescriptorSocket) throws SSLException {
        return NativeSsl.newInstance(sSLParametersImpl, conscryptFileDescriptorSocket, conscryptFileDescriptorSocket, conscryptFileDescriptorSocket);
    }

    @Override
    public final void startHandshake() throws IOException {
        NativeSslSession cachedSession;
        checkOpen();
        synchronized (this.ssl) {
            if (this.state == 0) {
                transitionTo(2);
                boolean z = true;
                try {
                    try {
                        Platform.closeGuardOpen(this.guard, "close");
                        this.ssl.initialize(getHostname(), this.channelIdPrivateKey);
                        if (getUseClientMode() && (cachedSession = clientSessionContext().getCachedSession(getHostnameOrIP(), getPort(), this.sslParameters)) != null) {
                            cachedSession.offerToResume(this.ssl);
                        }
                        int soTimeout = getSoTimeout();
                        int soWriteTimeout = getSoWriteTimeout();
                        if (this.handshakeTimeoutMilliseconds >= 0) {
                            setSoTimeout(this.handshakeTimeoutMilliseconds);
                            setSoWriteTimeout(this.handshakeTimeoutMilliseconds);
                        }
                        synchronized (this.ssl) {
                            if (this.state == 8) {
                                synchronized (this.ssl) {
                                    transitionTo(8);
                                    this.ssl.notifyAll();
                                }
                                try {
                                    shutdownAndFreeSslNative();
                                    return;
                                } catch (IOException e) {
                                    return;
                                }
                            }
                            try {
                                this.ssl.doHandshake(Platform.getFileDescriptor(this.socket), getSoTimeout());
                                this.activeSession.onPeerCertificateAvailable(getHostnameOrIP(), getPort());
                                synchronized (this.ssl) {
                                    if (this.state == 8) {
                                        synchronized (this.ssl) {
                                            transitionTo(8);
                                            this.ssl.notifyAll();
                                        }
                                        try {
                                            shutdownAndFreeSslNative();
                                            return;
                                        } catch (IOException e2) {
                                            return;
                                        }
                                    }
                                    if (this.handshakeTimeoutMilliseconds >= 0) {
                                        setSoTimeout(soTimeout);
                                        setSoWriteTimeout(soWriteTimeout);
                                    }
                                    synchronized (this.ssl) {
                                        if (this.state != 8) {
                                            z = false;
                                        }
                                        if (this.state == 2) {
                                            transitionTo(4);
                                        } else {
                                            transitionTo(5);
                                        }
                                        if (!z) {
                                            this.ssl.notifyAll();
                                        }
                                    }
                                    if (z) {
                                        synchronized (this.ssl) {
                                            transitionTo(8);
                                            this.ssl.notifyAll();
                                        }
                                        try {
                                            shutdownAndFreeSslNative();
                                        } catch (IOException e3) {
                                        }
                                    }
                                }
                            } catch (CertificateException e4) {
                                SSLHandshakeException sSLHandshakeException = new SSLHandshakeException(e4.getMessage());
                                sSLHandshakeException.initCause(e4);
                                throw sSLHandshakeException;
                            } catch (SSLException e5) {
                                synchronized (this.ssl) {
                                    if (this.state != 8) {
                                        if (e5.getMessage().contains("unexpected CCS")) {
                                            Platform.logEvent(String.format("ssl_unexpected_ccs: host=%s", getHostnameOrIP()));
                                        }
                                        throw e5;
                                    }
                                    synchronized (this.ssl) {
                                        transitionTo(8);
                                        this.ssl.notifyAll();
                                        try {
                                            shutdownAndFreeSslNative();
                                        } catch (IOException e6) {
                                        }
                                    }
                                }
                            }
                        }
                    } catch (SSLProtocolException e7) {
                        throw ((SSLHandshakeException) new SSLHandshakeException("Handshake failed").initCause(e7));
                    }
                } catch (Throwable th) {
                    if (1 != 0) {
                        synchronized (this.ssl) {
                            transitionTo(8);
                            this.ssl.notifyAll();
                            try {
                                shutdownAndFreeSslNative();
                            } catch (IOException e8) {
                            }
                        }
                    }
                    throw th;
                }
            }
        }
    }

    @Override
    public final void clientCertificateRequested(byte[] bArr, byte[][] bArr2) throws SSLException, CertificateEncodingException {
        this.ssl.chooseClientCertificate(bArr, bArr2);
    }

    @Override
    public final int clientPSKKeyRequested(String str, byte[] bArr, byte[] bArr2) {
        return this.ssl.clientPSKKeyRequested(str, bArr, bArr2);
    }

    @Override
    public final int serverPSKKeyRequested(String str, String str2, byte[] bArr) {
        return this.ssl.serverPSKKeyRequested(str, str2, bArr);
    }

    @Override
    public final void onSSLStateChange(int i, int i2) {
        if (i != 32) {
            return;
        }
        synchronized (this.ssl) {
            if (this.state == 8) {
                return;
            }
            transitionTo(5);
            notifyHandshakeCompletedListeners();
            synchronized (this.ssl) {
                this.ssl.notifyAll();
            }
        }
    }

    @Override
    public final void onNewSessionEstablished(long j) {
        try {
            NativeCrypto.SSL_SESSION_up_ref(j);
            sessionContext().cacheSession(NativeSslSession.newInstance(new NativeRef.SSL_SESSION(j), this.activeSession));
        } catch (Exception e) {
        }
    }

    @Override
    public final long serverSessionRequested(byte[] bArr) {
        return 0L;
    }

    @Override
    public final void verifyCertificateChain(byte[][] bArr, String str) throws CertificateException {
        if (bArr != null) {
            try {
                if (bArr.length != 0) {
                    X509Certificate[] x509CertificateArrDecodeX509CertificateChain = SSLUtils.decodeX509CertificateChain(bArr);
                    X509TrustManager x509TrustManager = this.sslParameters.getX509TrustManager();
                    if (x509TrustManager == null) {
                        throw new CertificateException("No X.509 TrustManager");
                    }
                    this.activeSession.onPeerCertificatesReceived(getHostnameOrIP(), getPort(), x509CertificateArrDecodeX509CertificateChain);
                    if (getUseClientMode()) {
                        Platform.checkServerTrusted(x509TrustManager, x509CertificateArrDecodeX509CertificateChain, str, this);
                        return;
                    } else {
                        Platform.checkClientTrusted(x509TrustManager, x509CertificateArrDecodeX509CertificateChain, x509CertificateArrDecodeX509CertificateChain[0].getPublicKey().getAlgorithm(), this);
                        return;
                    }
                }
            } catch (CertificateException e) {
                throw e;
            } catch (Exception e2) {
                throw new CertificateException(e2);
            }
        }
        throw new CertificateException("Peer sent no certificate");
    }

    @Override
    public final InputStream getInputStream() throws IOException {
        SSLInputStream sSLInputStream;
        checkOpen();
        synchronized (this.ssl) {
            if (this.state == 8) {
                throw new SocketException("Socket is closed.");
            }
            if (this.is == null) {
                this.is = new SSLInputStream();
            }
            sSLInputStream = this.is;
        }
        waitForHandshake();
        return sSLInputStream;
    }

    @Override
    public final OutputStream getOutputStream() throws IOException {
        SSLOutputStream sSLOutputStream;
        checkOpen();
        synchronized (this.ssl) {
            if (this.state == 8) {
                throw new SocketException("Socket is closed.");
            }
            if (this.os == null) {
                this.os = new SSLOutputStream();
            }
            sSLOutputStream = this.os;
        }
        waitForHandshake();
        return sSLOutputStream;
    }

    private void assertReadableOrWriteableState() {
        if (this.state == 5 || this.state == 4) {
            return;
        }
        throw new AssertionError("Invalid state: " + this.state);
    }

    private void waitForHandshake() throws IOException {
        startHandshake();
        synchronized (this.ssl) {
            while (this.state != 5 && this.state != 4 && this.state != 8) {
                try {
                    this.ssl.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted waiting for handshake", e);
                }
            }
            if (this.state == 8) {
                throw new SocketException("Socket is closed");
            }
        }
    }

    private class SSLInputStream extends InputStream {
        private final Object readLock = new Object();

        SSLInputStream() {
        }

        @Override
        public int read() throws IOException {
            byte[] bArr = new byte[1];
            if (read(bArr, 0, 1) != -1) {
                return bArr[0] & 255;
            }
            return -1;
        }

        @Override
        public int read(byte[] bArr, int i, int i2) throws IOException {
            int i3;
            Platform.blockGuardOnNetwork();
            ConscryptFileDescriptorSocket.this.checkOpen();
            ArrayUtils.checkOffsetAndCount(bArr.length, i, i2);
            if (i2 == 0) {
                return 0;
            }
            synchronized (this.readLock) {
                synchronized (ConscryptFileDescriptorSocket.this.ssl) {
                    if (ConscryptFileDescriptorSocket.this.state == 8) {
                        throw new SocketException("socket is closed");
                    }
                }
                i3 = ConscryptFileDescriptorSocket.this.ssl.read(Platform.getFileDescriptor(ConscryptFileDescriptorSocket.this.socket), bArr, i, i2, ConscryptFileDescriptorSocket.this.getSoTimeout());
                if (i3 == -1) {
                    synchronized (ConscryptFileDescriptorSocket.this.ssl) {
                        if (ConscryptFileDescriptorSocket.this.state == 8) {
                            throw new SocketException("socket is closed");
                        }
                    }
                }
            }
            return i3;
        }

        void awaitPendingOps() {
            synchronized (this.readLock) {
            }
        }
    }

    private class SSLOutputStream extends OutputStream {
        private final Object writeLock = new Object();

        SSLOutputStream() {
        }

        @Override
        public void write(int i) throws IOException {
            write(new byte[]{(byte) (i & 255)});
        }

        @Override
        public void write(byte[] bArr, int i, int i2) throws IOException {
            Platform.blockGuardOnNetwork();
            ConscryptFileDescriptorSocket.this.checkOpen();
            ArrayUtils.checkOffsetAndCount(bArr.length, i, i2);
            if (i2 == 0) {
                return;
            }
            synchronized (this.writeLock) {
                synchronized (ConscryptFileDescriptorSocket.this.ssl) {
                    if (ConscryptFileDescriptorSocket.this.state == 8) {
                        throw new SocketException("socket is closed");
                    }
                }
                ConscryptFileDescriptorSocket.this.ssl.write(Platform.getFileDescriptor(ConscryptFileDescriptorSocket.this.socket), bArr, i, i2, ConscryptFileDescriptorSocket.this.writeTimeoutMilliseconds);
                synchronized (ConscryptFileDescriptorSocket.this.ssl) {
                    if (ConscryptFileDescriptorSocket.this.state == 8) {
                        throw new SocketException("socket is closed");
                    }
                }
            }
        }

        void awaitPendingOps() {
            synchronized (this.writeLock) {
            }
        }
    }

    @Override
    public final SSLSession getSession() {
        return this.externalSession;
    }

    private ConscryptSession provideSession() {
        synchronized (this.ssl) {
            if (this.state == 8) {
                return this.closedSession != null ? this.closedSession : SSLNullSession.getNullSession();
            }
            boolean z = DBG_STATE;
            try {
                if (this.state >= 5) {
                    z = true;
                }
                if (!z && isConnected()) {
                    waitForHandshake();
                    z = true;
                }
            } catch (IOException e) {
            }
            if (!z) {
                return SSLNullSession.getNullSession();
            }
            return this.activeSession;
        }
    }

    private ConscryptSession provideHandshakeSession() {
        ConscryptSession nullSession;
        synchronized (this.ssl) {
            nullSession = (this.state < 2 || this.state >= 5) ? SSLNullSession.getNullSession() : this.activeSession;
        }
        return nullSession;
    }

    @Override
    final SSLSession getActiveSession() {
        return this.activeSession;
    }

    @Override
    public final SSLSession getHandshakeSession() {
        synchronized (this.ssl) {
            if (this.state >= 2 && this.state < 5) {
                return Platform.wrapSSLSession(new ExternalSession(new ExternalSession.Provider() {
                    @Override
                    public ConscryptSession provideSession() {
                        return ConscryptFileDescriptorSocket.this.provideHandshakeSession();
                    }
                }));
            }
            return null;
        }
    }

    @Override
    public final boolean getEnableSessionCreation() {
        return this.sslParameters.getEnableSessionCreation();
    }

    @Override
    public final void setEnableSessionCreation(boolean z) {
        this.sslParameters.setEnableSessionCreation(z);
    }

    @Override
    public final String[] getSupportedCipherSuites() {
        return NativeCrypto.getSupportedCipherSuites();
    }

    @Override
    public final String[] getEnabledCipherSuites() {
        return this.sslParameters.getEnabledCipherSuites();
    }

    @Override
    public final void setEnabledCipherSuites(String[] strArr) {
        this.sslParameters.setEnabledCipherSuites(strArr);
    }

    @Override
    public final String[] getSupportedProtocols() {
        return NativeCrypto.getSupportedProtocols();
    }

    @Override
    public final String[] getEnabledProtocols() {
        return this.sslParameters.getEnabledProtocols();
    }

    @Override
    public final void setEnabledProtocols(String[] strArr) {
        this.sslParameters.setEnabledProtocols(strArr);
    }

    @Override
    public final void setUseSessionTickets(boolean z) {
        this.sslParameters.setUseSessionTickets(z);
    }

    @Override
    public final void setHostname(String str) {
        this.sslParameters.setUseSni(str != null ? true : DBG_STATE);
        super.setHostname(str);
    }

    @Override
    public final void setChannelIdEnabled(boolean z) {
        if (getUseClientMode()) {
            throw new IllegalStateException("Client mode");
        }
        synchronized (this.ssl) {
            if (this.state != 0) {
                throw new IllegalStateException("Could not enable/disable Channel ID after the initial handshake has begun.");
            }
        }
        this.sslParameters.channelIdEnabled = z;
    }

    @Override
    public final byte[] getChannelId() throws SSLException {
        if (getUseClientMode()) {
            throw new IllegalStateException("Client mode");
        }
        synchronized (this.ssl) {
            if (this.state != 5) {
                throw new IllegalStateException("Channel ID is only available after handshake completes");
            }
        }
        return this.ssl.getTlsChannelId();
    }

    @Override
    public final void setChannelIdPrivateKey(PrivateKey privateKey) {
        if (!getUseClientMode()) {
            throw new IllegalStateException("Server mode");
        }
        synchronized (this.ssl) {
            if (this.state != 0) {
                throw new IllegalStateException("Could not change Channel ID private key after the initial handshake has begun.");
            }
        }
        ECParameterSpec eCParameterSpec = null;
        if (privateKey == null) {
            this.sslParameters.channelIdEnabled = DBG_STATE;
            this.channelIdPrivateKey = null;
            return;
        }
        this.sslParameters.channelIdEnabled = true;
        try {
            if (privateKey instanceof ECKey) {
                eCParameterSpec = ((ECKey) privateKey).getParams();
            }
            if (eCParameterSpec == null) {
                eCParameterSpec = OpenSSLECGroupContext.getCurveByName("prime256v1").getECParameterSpec();
            }
            this.channelIdPrivateKey = OpenSSLKey.fromECPrivateKeyForTLSStackOnly(privateKey, eCParameterSpec);
        } catch (InvalidKeyException e) {
        }
    }

    @Override
    byte[] getTlsUnique() {
        return this.ssl.getTlsUnique();
    }

    @Override
    public final boolean getUseClientMode() {
        return this.sslParameters.getUseClientMode();
    }

    @Override
    public final void setUseClientMode(boolean z) {
        synchronized (this.ssl) {
            if (this.state != 0) {
                throw new IllegalArgumentException("Could not change the mode after the initial handshake has begun.");
            }
        }
        this.sslParameters.setUseClientMode(z);
    }

    @Override
    public final boolean getWantClientAuth() {
        return this.sslParameters.getWantClientAuth();
    }

    @Override
    public final boolean getNeedClientAuth() {
        return this.sslParameters.getNeedClientAuth();
    }

    @Override
    public final void setNeedClientAuth(boolean z) {
        this.sslParameters.setNeedClientAuth(z);
    }

    @Override
    public final void setWantClientAuth(boolean z) {
        this.sslParameters.setWantClientAuth(z);
    }

    @Override
    public final void setSoWriteTimeout(int i) throws SocketException {
        this.writeTimeoutMilliseconds = i;
        Platform.setSocketWriteTimeout(this, i);
    }

    @Override
    public final int getSoWriteTimeout() throws SocketException {
        return this.writeTimeoutMilliseconds;
    }

    @Override
    public final void setHandshakeTimeout(int i) throws SocketException {
        this.handshakeTimeoutMilliseconds = i;
    }

    @Override
    public final void close() throws IOException {
        if (this.ssl == null) {
            return;
        }
        synchronized (this.ssl) {
            if (this.state == 8) {
                return;
            }
            int i = this.state;
            transitionTo(8);
            if (i == 0) {
                free();
                closeUnderlyingSocket();
                this.ssl.notifyAll();
                return;
            }
            if (i != 5 && i != 4) {
                this.ssl.interrupt();
                this.ssl.notifyAll();
                return;
            }
            this.ssl.notifyAll();
            SSLInputStream sSLInputStream = this.is;
            SSLOutputStream sSLOutputStream = this.os;
            if (sSLInputStream != null || sSLOutputStream != null) {
                this.ssl.interrupt();
            }
            if (sSLInputStream != null) {
                sSLInputStream.awaitPendingOps();
            }
            if (sSLOutputStream != null) {
                sSLOutputStream.awaitPendingOps();
            }
            shutdownAndFreeSslNative();
        }
    }

    private void shutdownAndFreeSslNative() throws IOException {
        try {
            Platform.blockGuardOnNetwork();
            this.ssl.shutdown(Platform.getFileDescriptor(this.socket));
        } catch (IOException e) {
        } catch (Throwable th) {
            free();
            closeUnderlyingSocket();
            throw th;
        }
        free();
        closeUnderlyingSocket();
    }

    private void closeUnderlyingSocket() throws IOException {
        super.close();
    }

    private void free() {
        if (!this.ssl.isClosed()) {
            this.ssl.close();
            Platform.closeGuardClose(this.guard);
        }
    }

    protected final void finalize() throws Throwable {
        try {
            if (this.guard != null) {
                Platform.closeGuardWarnIfOpen(this.guard);
            }
            synchronized (this.ssl) {
                transitionTo(8);
            }
        } finally {
            super.finalize();
        }
    }

    @Override
    public final void setApplicationProtocolSelector(ApplicationProtocolSelector applicationProtocolSelector) {
        setApplicationProtocolSelector(applicationProtocolSelector == null ? null : new ApplicationProtocolSelectorAdapter(this, applicationProtocolSelector));
    }

    @Override
    final void setApplicationProtocolSelector(ApplicationProtocolSelectorAdapter applicationProtocolSelectorAdapter) {
        this.sslParameters.setApplicationProtocolSelector(applicationProtocolSelectorAdapter);
    }

    @Override
    final void setApplicationProtocols(String[] strArr) {
        this.sslParameters.setApplicationProtocols(strArr);
    }

    @Override
    final String[] getApplicationProtocols() {
        return this.sslParameters.getApplicationProtocols();
    }

    @Override
    public final String getApplicationProtocol() {
        return SSLUtils.toProtocolString(this.ssl.getApplicationProtocol());
    }

    @Override
    public final String getHandshakeApplicationProtocol() {
        String applicationProtocol;
        synchronized (this.ssl) {
            applicationProtocol = (this.state < 2 || this.state >= 5) ? null : getApplicationProtocol();
        }
        return applicationProtocol;
    }

    @Override
    public final SSLParameters getSSLParameters() {
        SSLParameters sSLParameters = super.getSSLParameters();
        Platform.getSSLParameters(sSLParameters, this.sslParameters, this);
        return sSLParameters;
    }

    @Override
    public final void setSSLParameters(SSLParameters sSLParameters) {
        super.setSSLParameters(sSLParameters);
        Platform.setSSLParameters(sSLParameters, this.sslParameters, this);
    }

    @Override
    public final String chooseServerAlias(X509KeyManager x509KeyManager, String str) {
        return x509KeyManager.chooseServerAlias(str, null, this);
    }

    @Override
    public final String chooseClientAlias(X509KeyManager x509KeyManager, X500Principal[] x500PrincipalArr, String[] strArr) {
        return x509KeyManager.chooseClientAlias(strArr, x500PrincipalArr, this);
    }

    @Override
    public final String chooseServerPSKIdentityHint(PSKKeyManager pSKKeyManager) {
        return pSKKeyManager.chooseServerKeyIdentityHint(this);
    }

    @Override
    public final String chooseClientPSKIdentity(PSKKeyManager pSKKeyManager, String str) {
        return pSKKeyManager.chooseClientKeyIdentity(str, this);
    }

    @Override
    public final SecretKey getPSKKey(PSKKeyManager pSKKeyManager, String str, String str2) {
        return pSKKeyManager.getKey(str, str2, this);
    }

    private ClientSessionContext clientSessionContext() {
        return this.sslParameters.getClientSessionContext();
    }

    private AbstractSessionContext sessionContext() {
        return this.sslParameters.getSessionContext();
    }

    private void transitionTo(int i) {
        if (i == 8 && !this.ssl.isClosed() && this.state >= 2 && this.state < 8) {
            this.closedSession = new SessionSnapshot(this.activeSession);
        }
        this.state = i;
    }
}
