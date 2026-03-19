package com.android.org.conscrypt;

import com.android.org.conscrypt.NativeCrypto;
import com.android.org.conscrypt.SSLParametersImpl;
import com.android.org.conscrypt.ct.CTVerificationResult;
import com.android.org.conscrypt.ct.CTVerifier;
import com.android.org.conscrypt.util.ArrayUtils;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.interfaces.ECKey;
import java.security.spec.ECParameterSpec;
import java.util.ArrayList;
import javax.crypto.SecretKey;
import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLProtocolException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;

public class OpenSSLSocketImpl extends SSLSocket implements NativeCrypto.SSLHandshakeCallbacks, SSLParametersImpl.AliasChooser, SSLParametersImpl.PSKCallbacks {
    private static final boolean DBG_STATE = false;
    private static final int STATE_CLOSED = 5;
    private static final int STATE_HANDSHAKE_COMPLETED = 2;
    private static final int STATE_HANDSHAKE_STARTED = 1;
    private static final int STATE_NEW = 0;
    private static final int STATE_READY = 4;
    private static final int STATE_READY_HANDSHAKE_CUT_THROUGH = 3;
    private final boolean autoClose;
    OpenSSLKey channelIdPrivateKey;
    private final Object guard;
    private OpenSSLSessionImpl handshakeSession;
    private int handshakeTimeoutMilliseconds;
    private SSLInputStream is;
    private ArrayList<HandshakeCompletedListener> listeners;
    private SSLOutputStream os;
    private String peerHostname;
    private final int peerPort;
    private int readTimeoutMilliseconds;
    private final Socket socket;
    private long sslNativePointer;
    private final SSLParametersImpl sslParameters;
    private OpenSSLSessionImpl sslSession;
    private int state;
    private final Object stateLock;
    private int writeTimeoutMilliseconds;

    protected OpenSSLSocketImpl(SSLParametersImpl sslParameters) throws IOException {
        this.stateLock = new Object();
        this.state = 0;
        this.guard = Platform.closeGuardGet();
        this.readTimeoutMilliseconds = 0;
        this.writeTimeoutMilliseconds = 0;
        this.handshakeTimeoutMilliseconds = -1;
        this.socket = this;
        this.peerHostname = null;
        this.peerPort = -1;
        this.autoClose = DBG_STATE;
        this.sslParameters = sslParameters;
    }

    protected OpenSSLSocketImpl(String hostname, int port, SSLParametersImpl sslParameters) throws IOException {
        super(hostname, port);
        this.stateLock = new Object();
        this.state = 0;
        this.guard = Platform.closeGuardGet();
        this.readTimeoutMilliseconds = 0;
        this.writeTimeoutMilliseconds = 0;
        this.handshakeTimeoutMilliseconds = -1;
        this.socket = this;
        this.peerHostname = hostname;
        this.peerPort = port;
        this.autoClose = DBG_STATE;
        this.sslParameters = sslParameters;
    }

    protected OpenSSLSocketImpl(InetAddress address, int port, SSLParametersImpl sslParameters) throws IOException {
        super(address, port);
        this.stateLock = new Object();
        this.state = 0;
        this.guard = Platform.closeGuardGet();
        this.readTimeoutMilliseconds = 0;
        this.writeTimeoutMilliseconds = 0;
        this.handshakeTimeoutMilliseconds = -1;
        this.socket = this;
        this.peerHostname = null;
        this.peerPort = -1;
        this.autoClose = DBG_STATE;
        this.sslParameters = sslParameters;
    }

    protected OpenSSLSocketImpl(String hostname, int port, InetAddress clientAddress, int clientPort, SSLParametersImpl sslParameters) throws IOException {
        super(hostname, port, clientAddress, clientPort);
        this.stateLock = new Object();
        this.state = 0;
        this.guard = Platform.closeGuardGet();
        this.readTimeoutMilliseconds = 0;
        this.writeTimeoutMilliseconds = 0;
        this.handshakeTimeoutMilliseconds = -1;
        this.socket = this;
        this.peerHostname = hostname;
        this.peerPort = port;
        this.autoClose = DBG_STATE;
        this.sslParameters = sslParameters;
    }

    protected OpenSSLSocketImpl(InetAddress address, int port, InetAddress clientAddress, int clientPort, SSLParametersImpl sslParameters) throws IOException {
        super(address, port, clientAddress, clientPort);
        this.stateLock = new Object();
        this.state = 0;
        this.guard = Platform.closeGuardGet();
        this.readTimeoutMilliseconds = 0;
        this.writeTimeoutMilliseconds = 0;
        this.handshakeTimeoutMilliseconds = -1;
        this.socket = this;
        this.peerHostname = null;
        this.peerPort = -1;
        this.autoClose = DBG_STATE;
        this.sslParameters = sslParameters;
    }

    protected OpenSSLSocketImpl(Socket socket, String hostname, int port, boolean autoClose, SSLParametersImpl sslParameters) throws IOException {
        this.stateLock = new Object();
        this.state = 0;
        this.guard = Platform.closeGuardGet();
        this.readTimeoutMilliseconds = 0;
        this.writeTimeoutMilliseconds = 0;
        this.handshakeTimeoutMilliseconds = -1;
        this.socket = socket;
        this.peerHostname = hostname;
        this.peerPort = port;
        this.autoClose = autoClose;
        this.sslParameters = sslParameters;
    }

    @Override
    public void connect(SocketAddress endpoint) throws IOException {
        connect(endpoint, 0);
    }

    @Override
    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        if (this.peerHostname == null && (endpoint instanceof InetSocketAddress)) {
            this.peerHostname = Platform.getHostStringFromInetSocketAddress((InetSocketAddress) endpoint);
        }
        super.connect(endpoint, timeout);
    }

    private void checkOpen() throws SocketException {
        if (!isClosed()) {
        } else {
            throw new SocketException("Socket is closed");
        }
    }

    @Override
    public void startHandshake() throws IOException {
        boolean releaseResources;
        checkOpen();
        synchronized (this.stateLock) {
            if (this.state == 0) {
                this.state = 1;
                if (!NativeCrypto.isBoringSSL) {
                    SecureRandom secureRandom = this.sslParameters.getSecureRandomMember();
                    if (secureRandom == null) {
                        NativeCrypto.RAND_load_file("/dev/urandom", 1024L);
                    } else {
                        NativeCrypto.RAND_seed(secureRandom.generateSeed(1024));
                    }
                }
                boolean client = this.sslParameters.getUseClientMode();
                this.sslNativePointer = 0L;
                try {
                    try {
                        AbstractSessionContext sessionContext = this.sslParameters.getSessionContext();
                        long sslCtxNativePointer = sessionContext.sslCtxNativePointer;
                        this.sslNativePointer = NativeCrypto.SSL_new(sslCtxNativePointer);
                        Platform.closeGuardOpen(this.guard, "close");
                        boolean enableSessionCreation = getEnableSessionCreation();
                        if (!enableSessionCreation) {
                            NativeCrypto.SSL_set_session_creation_enabled(this.sslNativePointer, enableSessionCreation);
                        }
                        NativeCrypto.SSL_set_reject_peer_renegotiations(this.sslNativePointer, DBG_STATE);
                        if (client && this.sslParameters.isCTVerificationEnabled(getHostname())) {
                            NativeCrypto.SSL_enable_signed_cert_timestamps(this.sslNativePointer);
                            NativeCrypto.SSL_enable_ocsp_stapling(this.sslNativePointer);
                        }
                        OpenSSLSessionImpl sessionToReuse = this.sslParameters.getSessionToReuse(this.sslNativePointer, getHostnameOrIP(), getPort());
                        this.sslParameters.setSSLParameters(sslCtxNativePointer, this.sslNativePointer, this, this, getHostname());
                        this.sslParameters.setCertificateValidation(this.sslNativePointer);
                        this.sslParameters.setTlsChannelId(this.sslNativePointer, this.channelIdPrivateKey);
                        int savedReadTimeoutMilliseconds = getSoTimeout();
                        int savedWriteTimeoutMilliseconds = getSoWriteTimeout();
                        if (this.handshakeTimeoutMilliseconds >= 0) {
                            setSoTimeout(this.handshakeTimeoutMilliseconds);
                            setSoWriteTimeout(this.handshakeTimeoutMilliseconds);
                        }
                        synchronized (this.stateLock) {
                            if (this.state == STATE_CLOSED) {
                                if (1 != 0) {
                                    synchronized (this.stateLock) {
                                        this.state = STATE_CLOSED;
                                        this.stateLock.notifyAll();
                                    }
                                    try {
                                        shutdownAndFreeSslNative();
                                        return;
                                    } catch (IOException e) {
                                        return;
                                    }
                                }
                                return;
                            }
                            try {
                                long sslSessionNativePointer = NativeCrypto.SSL_do_handshake(this.sslNativePointer, Platform.getFileDescriptor(this.socket), this, getSoTimeout(), client, this.sslParameters.npnProtocols, client ? null : this.sslParameters.alpnProtocols);
                                boolean handshakeCompleted = DBG_STATE;
                                synchronized (this.stateLock) {
                                    if (this.state == 2) {
                                        handshakeCompleted = true;
                                    } else if (this.state == STATE_CLOSED) {
                                        if (1 != 0) {
                                            synchronized (this.stateLock) {
                                                this.state = STATE_CLOSED;
                                                this.stateLock.notifyAll();
                                            }
                                            try {
                                                shutdownAndFreeSslNative();
                                                return;
                                            } catch (IOException e2) {
                                                return;
                                            }
                                        }
                                        return;
                                    }
                                    this.sslSession = this.sslParameters.setupSession(sslSessionNativePointer, this.sslNativePointer, sessionToReuse, getHostnameOrIP(), getPort(), handshakeCompleted);
                                    if (this.handshakeTimeoutMilliseconds >= 0) {
                                        setSoTimeout(savedReadTimeoutMilliseconds);
                                        setSoWriteTimeout(savedWriteTimeoutMilliseconds);
                                    }
                                    if (handshakeCompleted) {
                                        String cipherName = this.sslSession.getCipherSuite();
                                        if (cipherName.length() > 0) {
                                            System.out.println("gba_cipher_suite:" + cipherName);
                                            System.setProperty("gba.ciper.suite", cipherName);
                                        }
                                        notifyHandshakeCompletedListeners();
                                    }
                                    synchronized (this.stateLock) {
                                        releaseResources = this.state == STATE_CLOSED ? true : DBG_STATE;
                                        if (this.state == 1) {
                                            this.state = 3;
                                        } else if (this.state == 2) {
                                            this.state = 4;
                                        }
                                        if (!releaseResources) {
                                            this.stateLock.notifyAll();
                                        }
                                    }
                                    if (releaseResources) {
                                        synchronized (this.stateLock) {
                                            this.state = STATE_CLOSED;
                                            this.stateLock.notifyAll();
                                        }
                                        try {
                                            shutdownAndFreeSslNative();
                                        } catch (IOException e3) {
                                        }
                                    }
                                }
                            } catch (CertificateException e4) {
                                SSLHandshakeException wrapper = new SSLHandshakeException(e4.getMessage());
                                wrapper.initCause(e4);
                                throw wrapper;
                            } catch (SSLException e5) {
                                synchronized (this.stateLock) {
                                    if (this.state != STATE_CLOSED) {
                                        String message = e5.getMessage();
                                        if (message.contains("unexpected CCS")) {
                                            String logMessage = String.format("ssl_unexpected_ccs: host=%s", getHostnameOrIP());
                                            Platform.logEvent(logMessage);
                                        }
                                        throw e5;
                                    }
                                    if (1 != 0) {
                                        synchronized (this.stateLock) {
                                            this.state = STATE_CLOSED;
                                            this.stateLock.notifyAll();
                                            try {
                                                shutdownAndFreeSslNative();
                                            } catch (IOException e6) {
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Throwable th) {
                        if (1 != 0) {
                            synchronized (this.stateLock) {
                                this.state = STATE_CLOSED;
                                this.stateLock.notifyAll();
                                try {
                                    shutdownAndFreeSslNative();
                                } catch (IOException e7) {
                                }
                            }
                        }
                        throw th;
                    }
                } catch (SSLProtocolException e8) {
                    System.out.println("SSLProtocolException:" + e8.getMessage());
                    throw ((SSLHandshakeException) new SSLHandshakeException("Handshake failed").initCause(e8));
                }
            }
        }
    }

    public String getHostname() {
        return this.peerHostname;
    }

    public String getHostnameOrIP() {
        if (this.peerHostname != null) {
            return this.peerHostname;
        }
        InetAddress peerAddress = getInetAddress();
        if (peerAddress != null) {
            return peerAddress.getHostAddress();
        }
        return null;
    }

    @Override
    public int getPort() {
        return this.peerPort == -1 ? super.getPort() : this.peerPort;
    }

    @Override
    public void clientCertificateRequested(byte[] keyTypeBytes, byte[][] asn1DerEncodedPrincipals) throws SSLException, CertificateEncodingException {
        this.sslParameters.chooseClientCertificate(keyTypeBytes, asn1DerEncodedPrincipals, this.sslNativePointer, this);
    }

    @Override
    public int clientPSKKeyRequested(String identityHint, byte[] identity, byte[] key) {
        return this.sslParameters.clientPSKKeyRequested(identityHint, identity, key, this);
    }

    @Override
    public int serverPSKKeyRequested(String identityHint, String identity, byte[] key) {
        return this.sslParameters.serverPSKKeyRequested(identityHint, identity, key, this);
    }

    @Override
    public void onSSLStateChange(long sslSessionNativePtr, int type, int val) {
        AbstractSessionContext sessionContext;
        if (type != 32) {
            return;
        }
        synchronized (this.stateLock) {
            if (this.state == 1) {
                this.state = 2;
                return;
            }
            if (this.state != 3 && this.state == STATE_CLOSED) {
                return;
            }
            this.sslSession.resetId();
            if (this.sslParameters.getUseClientMode()) {
                sessionContext = this.sslParameters.getClientSessionContext();
            } else {
                sessionContext = this.sslParameters.getServerSessionContext();
            }
            sessionContext.putSession(this.sslSession);
            notifyHandshakeCompletedListeners();
            synchronized (this.stateLock) {
                this.state = 4;
                this.stateLock.notifyAll();
            }
        }
    }

    private void notifyHandshakeCompletedListeners() {
        if (this.listeners == null || this.listeners.isEmpty()) {
            return;
        }
        HandshakeCompletedEvent event = new HandshakeCompletedEvent(this, this.sslSession);
        for (HandshakeCompletedListener listener : this.listeners) {
            try {
                listener.handshakeCompleted(event);
            } catch (RuntimeException e) {
                Thread thread = Thread.currentThread();
                thread.getUncaughtExceptionHandler().uncaughtException(thread, e);
            }
        }
    }

    @Override
    public void verifyCertificateChain(long sslSessionNativePtr, long[] certRefs, String authMethod) throws CertificateException {
        try {
            try {
                X509TrustManager x509tm = this.sslParameters.getX509TrustManager();
                if (x509tm == null) {
                    throw new CertificateException("No X.509 TrustManager");
                }
                if (certRefs == null || certRefs.length == 0) {
                    throw new SSLException("Peer sent no certificate");
                }
                OpenSSLX509Certificate[] peerCertChain = new OpenSSLX509Certificate[certRefs.length];
                for (int i = 0; i < certRefs.length; i++) {
                    peerCertChain[i] = new OpenSSLX509Certificate(certRefs[i]);
                }
                this.handshakeSession = new OpenSSLSessionImpl(sslSessionNativePtr, null, peerCertChain, getHostnameOrIP(), getPort(), null);
                boolean client = this.sslParameters.getUseClientMode();
                if (client) {
                    Platform.checkServerTrusted(x509tm, peerCertChain, authMethod, this);
                    if (this.sslParameters.isCTVerificationEnabled(getHostname())) {
                        byte[] tlsData = NativeCrypto.SSL_get_signed_cert_timestamp_list(this.sslNativePointer);
                        byte[] ocspData = NativeCrypto.SSL_get_ocsp_response(this.sslNativePointer);
                        CTVerifier ctVerifier = this.sslParameters.getCTVerifier();
                        CTVerificationResult result = ctVerifier.verifySignedCertificateTimestamps(peerCertChain, tlsData, ocspData);
                        if (result.getValidSCTs().size() == 0) {
                            throw new CertificateException("No valid SCT found");
                        }
                    }
                } else {
                    String authType = peerCertChain[0].getPublicKey().getAlgorithm();
                    Platform.checkClientTrusted(x509tm, peerCertChain, authType, this);
                }
            } catch (CertificateException e) {
                throw e;
            } catch (Exception e2) {
                throw new CertificateException(e2);
            }
        } finally {
            this.handshakeSession = null;
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        InputStream returnVal;
        checkOpen();
        synchronized (this.stateLock) {
            if (this.state == STATE_CLOSED) {
                throw new SocketException("Socket is closed.");
            }
            if (this.is == null) {
                this.is = new SSLInputStream();
            }
            returnVal = this.is;
        }
        waitForHandshake();
        return returnVal;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        OutputStream returnVal;
        checkOpen();
        synchronized (this.stateLock) {
            if (this.state == STATE_CLOSED) {
                throw new SocketException("Socket is closed.");
            }
            if (this.os == null) {
                this.os = new SSLOutputStream();
            }
            returnVal = this.os;
        }
        waitForHandshake();
        return returnVal;
    }

    private void assertReadableOrWriteableState() {
        if (this.state == 4 || this.state == 3) {
        } else {
            throw new AssertionError("Invalid state: " + this.state);
        }
    }

    private void waitForHandshake() throws IOException {
        startHandshake();
        synchronized (this.stateLock) {
            while (this.state != 4 && this.state != 3 && this.state != STATE_CLOSED) {
                try {
                    this.stateLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    IOException ioe = new IOException("Interrupted waiting for handshake");
                    ioe.initCause(e);
                    throw ioe;
                }
            }
            if (this.state == STATE_CLOSED) {
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
            byte[] buffer = new byte[1];
            int result = read(buffer, 0, 1);
            if (result != -1) {
                return buffer[0] & 255;
            }
            return -1;
        }

        @Override
        public int read(byte[] buf, int offset, int byteCount) throws IOException {
            int iSSL_read;
            Platform.blockGuardOnNetwork();
            OpenSSLSocketImpl.this.checkOpen();
            ArrayUtils.checkOffsetAndCount(buf.length, offset, byteCount);
            if (byteCount == 0) {
                return 0;
            }
            synchronized (this.readLock) {
                synchronized (OpenSSLSocketImpl.this.stateLock) {
                    if (OpenSSLSocketImpl.this.state == OpenSSLSocketImpl.STATE_CLOSED) {
                        throw new SocketException("socket is closed");
                    }
                }
                iSSL_read = NativeCrypto.SSL_read(OpenSSLSocketImpl.this.sslNativePointer, Platform.getFileDescriptor(OpenSSLSocketImpl.this.socket), OpenSSLSocketImpl.this, buf, offset, byteCount, OpenSSLSocketImpl.this.getSoTimeout());
            }
            return iSSL_read;
        }

        public void awaitPendingOps() {
            synchronized (this.readLock) {
            }
        }
    }

    private class SSLOutputStream extends OutputStream {
        private final Object writeLock = new Object();

        SSLOutputStream() {
        }

        @Override
        public void write(int oneByte) throws IOException {
            byte[] buffer = {(byte) (oneByte & 255)};
            write(buffer);
        }

        @Override
        public void write(byte[] buf, int offset, int byteCount) throws IOException {
            Platform.blockGuardOnNetwork();
            OpenSSLSocketImpl.this.checkOpen();
            ArrayUtils.checkOffsetAndCount(buf.length, offset, byteCount);
            if (byteCount == 0) {
                return;
            }
            synchronized (this.writeLock) {
                synchronized (OpenSSLSocketImpl.this.stateLock) {
                    if (OpenSSLSocketImpl.this.state == OpenSSLSocketImpl.STATE_CLOSED) {
                        throw new SocketException("socket is closed");
                    }
                }
                NativeCrypto.SSL_write(OpenSSLSocketImpl.this.sslNativePointer, Platform.getFileDescriptor(OpenSSLSocketImpl.this.socket), OpenSSLSocketImpl.this, buf, offset, byteCount, OpenSSLSocketImpl.this.writeTimeoutMilliseconds);
            }
        }

        public void awaitPendingOps() {
            synchronized (this.writeLock) {
            }
        }
    }

    @Override
    public SSLSession getSession() {
        if (this.sslSession == null) {
            try {
                waitForHandshake();
            } catch (IOException e) {
                return SSLNullSession.getNullSession();
            }
        }
        return Platform.wrapSSLSession(this.sslSession);
    }

    @Override
    public SSLSession getHandshakeSession() {
        return this.handshakeSession;
    }

    @Override
    public void addHandshakeCompletedListener(HandshakeCompletedListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Provided listener is null");
        }
        if (this.listeners == null) {
            this.listeners = new ArrayList<>();
        }
        this.listeners.add(listener);
    }

    @Override
    public void removeHandshakeCompletedListener(HandshakeCompletedListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Provided listener is null");
        }
        if (this.listeners == null) {
            throw new IllegalArgumentException("Provided listener is not registered");
        }
        if (this.listeners.remove(listener)) {
        } else {
            throw new IllegalArgumentException("Provided listener is not registered");
        }
    }

    @Override
    public boolean getEnableSessionCreation() {
        return this.sslParameters.getEnableSessionCreation();
    }

    @Override
    public void setEnableSessionCreation(boolean flag) {
        this.sslParameters.setEnableSessionCreation(flag);
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return NativeCrypto.getSupportedCipherSuites();
    }

    @Override
    public String[] getEnabledCipherSuites() {
        return this.sslParameters.getEnabledCipherSuites();
    }

    @Override
    public void setEnabledCipherSuites(String[] suites) {
        this.sslParameters.setEnabledCipherSuites(suites);
    }

    @Override
    public String[] getSupportedProtocols() {
        return NativeCrypto.getSupportedProtocols();
    }

    @Override
    public String[] getEnabledProtocols() {
        return this.sslParameters.getEnabledProtocols();
    }

    @Override
    public void setEnabledProtocols(String[] protocols) {
        this.sslParameters.setEnabledProtocols(protocols);
    }

    public void setUseSessionTickets(boolean useSessionTickets) {
        this.sslParameters.useSessionTickets = useSessionTickets;
    }

    public void setHostname(String hostname) {
        this.sslParameters.setUseSni(hostname != null ? true : DBG_STATE);
        this.peerHostname = hostname;
    }

    public void setChannelIdEnabled(boolean enabled) {
        if (getUseClientMode()) {
            throw new IllegalStateException("Client mode");
        }
        synchronized (this.stateLock) {
            if (this.state != 0) {
                throw new IllegalStateException("Could not enable/disable Channel ID after the initial handshake has begun.");
            }
        }
        this.sslParameters.channelIdEnabled = enabled;
    }

    public byte[] getChannelId() throws SSLException {
        if (getUseClientMode()) {
            throw new IllegalStateException("Client mode");
        }
        synchronized (this.stateLock) {
            if (this.state != 4) {
                throw new IllegalStateException("Channel ID is only available after handshake completes");
            }
        }
        return NativeCrypto.SSL_get_tls_channel_id(this.sslNativePointer);
    }

    public void setChannelIdPrivateKey(PrivateKey privateKey) {
        if (!getUseClientMode()) {
            throw new IllegalStateException("Server mode");
        }
        synchronized (this.stateLock) {
            if (this.state != 0) {
                throw new IllegalStateException("Could not change Channel ID private key after the initial handshake has begun.");
            }
        }
        if (privateKey == null) {
            this.sslParameters.channelIdEnabled = DBG_STATE;
            this.channelIdPrivateKey = null;
            return;
        }
        this.sslParameters.channelIdEnabled = true;
        ECParameterSpec ecParams = null;
        try {
            if (privateKey instanceof ECKey) {
                ecParams = ((ECKey) privateKey).getParams();
            }
            if (ecParams == null) {
                ecParams = OpenSSLECGroupContext.getCurveByName("prime256v1").getECParameterSpec();
            }
            this.channelIdPrivateKey = OpenSSLKey.fromECPrivateKeyForTLSStackOnly(privateKey, ecParams);
        } catch (InvalidKeyException e) {
        }
    }

    @Override
    public boolean getUseClientMode() {
        return this.sslParameters.getUseClientMode();
    }

    @Override
    public void setUseClientMode(boolean mode) {
        synchronized (this.stateLock) {
            if (this.state != 0) {
                throw new IllegalArgumentException("Could not change the mode after the initial handshake has begun.");
            }
        }
        this.sslParameters.setUseClientMode(mode);
    }

    @Override
    public boolean getWantClientAuth() {
        return this.sslParameters.getWantClientAuth();
    }

    @Override
    public boolean getNeedClientAuth() {
        return this.sslParameters.getNeedClientAuth();
    }

    @Override
    public void setNeedClientAuth(boolean need) {
        this.sslParameters.setNeedClientAuth(need);
    }

    @Override
    public void setWantClientAuth(boolean want) {
        this.sslParameters.setWantClientAuth(want);
    }

    @Override
    public void sendUrgentData(int data) throws IOException {
        throw new SocketException("Method sendUrgentData() is not supported.");
    }

    @Override
    public void setOOBInline(boolean on) throws SocketException {
        throw new SocketException("Methods sendUrgentData, setOOBInline are not supported.");
    }

    @Override
    public void setSoTimeout(int readTimeoutMilliseconds) throws SocketException {
        if (this.socket != this) {
            this.socket.setSoTimeout(readTimeoutMilliseconds);
        } else {
            super.setSoTimeout(readTimeoutMilliseconds);
        }
        this.readTimeoutMilliseconds = readTimeoutMilliseconds;
    }

    @Override
    public int getSoTimeout() throws SocketException {
        return this.readTimeoutMilliseconds;
    }

    public void setSoWriteTimeout(int writeTimeoutMilliseconds) throws SocketException {
        this.writeTimeoutMilliseconds = writeTimeoutMilliseconds;
        Platform.setSocketWriteTimeout(this, writeTimeoutMilliseconds);
    }

    public int getSoWriteTimeout() throws SocketException {
        return this.writeTimeoutMilliseconds;
    }

    public void setHandshakeTimeout(int handshakeTimeoutMilliseconds) throws SocketException {
        this.handshakeTimeoutMilliseconds = handshakeTimeoutMilliseconds;
    }

    @Override
    public void close() throws IOException {
        synchronized (this.stateLock) {
            if (this.state == STATE_CLOSED) {
                return;
            }
            int oldState = this.state;
            this.state = STATE_CLOSED;
            if (oldState == 0) {
                closeUnderlyingSocket();
                this.stateLock.notifyAll();
                return;
            }
            if (oldState != 4 && oldState != 3) {
                NativeCrypto.SSL_interrupt(this.sslNativePointer);
                this.stateLock.notifyAll();
                return;
            }
            this.stateLock.notifyAll();
            SSLInputStream sslInputStream = this.is;
            SSLOutputStream sslOutputStream = this.os;
            if (sslInputStream != null || sslOutputStream != null) {
                NativeCrypto.SSL_interrupt(this.sslNativePointer);
            }
            if (sslInputStream != null) {
                sslInputStream.awaitPendingOps();
            }
            if (sslOutputStream != null) {
                sslOutputStream.awaitPendingOps();
            }
            shutdownAndFreeSslNative();
        }
    }

    private void shutdownAndFreeSslNative() throws IOException {
        try {
            Platform.blockGuardOnNetwork();
            NativeCrypto.SSL_shutdown(this.sslNativePointer, Platform.getFileDescriptor(this.socket), this);
        } catch (IOException e) {
        } finally {
            free();
            closeUnderlyingSocket();
        }
    }

    private void closeUnderlyingSocket() throws IOException {
        if (this.socket != this) {
            if (!this.autoClose || this.socket.isClosed()) {
                return;
            }
            this.socket.close();
            return;
        }
        if (super.isClosed()) {
            return;
        }
        super.close();
    }

    private void free() {
        if (this.sslNativePointer == 0) {
            return;
        }
        NativeCrypto.SSL_free(this.sslNativePointer);
        this.sslNativePointer = 0L;
        Platform.closeGuardClose(this.guard);
    }

    protected void finalize() throws Throwable {
        try {
            if (this.guard != null) {
                Platform.closeGuardWarnIfOpen(this.guard);
            }
            free();
        } finally {
            super.finalize();
        }
    }

    public FileDescriptor getFileDescriptor$() {
        if (this.socket == this) {
            return Platform.getFileDescriptorFromSSLSocket(this);
        }
        return Platform.getFileDescriptor(this.socket);
    }

    public byte[] getNpnSelectedProtocol() {
        return NativeCrypto.SSL_get_npn_negotiated_protocol(this.sslNativePointer);
    }

    public byte[] getAlpnSelectedProtocol() {
        return NativeCrypto.SSL_get0_alpn_selected(this.sslNativePointer);
    }

    public void setNpnProtocols(byte[] npnProtocols) {
        if (npnProtocols != null && npnProtocols.length == 0) {
            throw new IllegalArgumentException("npnProtocols.length == 0");
        }
        this.sslParameters.npnProtocols = npnProtocols;
    }

    public void setAlpnProtocols(byte[] alpnProtocols) {
        if (alpnProtocols != null && alpnProtocols.length == 0) {
            throw new IllegalArgumentException("alpnProtocols.length == 0");
        }
        this.sslParameters.alpnProtocols = alpnProtocols;
    }

    @Override
    public SSLParameters getSSLParameters() {
        SSLParameters params = super.getSSLParameters();
        Platform.getSSLParameters(params, this.sslParameters, this);
        return params;
    }

    @Override
    public void setSSLParameters(SSLParameters p) {
        super.setSSLParameters(p);
        Platform.setSSLParameters(p, this.sslParameters, this);
    }

    @Override
    public String chooseServerAlias(X509KeyManager keyManager, String keyType) {
        return keyManager.chooseServerAlias(keyType, null, this);
    }

    @Override
    public String chooseClientAlias(X509KeyManager keyManager, X500Principal[] issuers, String[] keyTypes) {
        return keyManager.chooseClientAlias(keyTypes, null, this);
    }

    @Override
    public String chooseServerPSKIdentityHint(PSKKeyManager keyManager) {
        return keyManager.chooseServerKeyIdentityHint(this);
    }

    @Override
    public String chooseClientPSKIdentity(PSKKeyManager keyManager, String identityHint) {
        return keyManager.chooseClientKeyIdentity(identityHint, this);
    }

    @Override
    public SecretKey getPSKKey(PSKKeyManager keyManager, String identityHint, String identity) {
        return keyManager.getKey(identityHint, identity, this);
    }
}
