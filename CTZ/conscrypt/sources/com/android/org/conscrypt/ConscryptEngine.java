package com.android.org.conscrypt;

import com.android.org.conscrypt.ExternalSession;
import com.android.org.conscrypt.NativeCrypto;
import com.android.org.conscrypt.NativeRef;
import com.android.org.conscrypt.NativeSsl;
import com.android.org.conscrypt.SSLParametersImpl;
import com.android.org.conscrypt.ct.CTConstants;
import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECKey;
import java.security.spec.ECParameterSpec;
import javax.crypto.SecretKey;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;

final class ConscryptEngine extends AbstractConscryptEngine implements NativeCrypto.SSLHandshakeCallbacks, SSLParametersImpl.AliasChooser, SSLParametersImpl.PSKCallbacks {
    private final ActiveSession activeSession;
    private BufferAllocator bufferAllocator;
    private OpenSSLKey channelIdPrivateKey;
    private SessionSnapshot closedSession;
    private final SSLSession externalSession;
    private SSLException handshakeException;
    private boolean handshakeFinished;
    private HandshakeListener handshakeListener;
    private ByteBuffer lazyDirectBuffer;
    private int maxSealOverhead;
    private final NativeSsl.BioWrapper networkBio;
    private String peerHostname;
    private final PeerInfoProvider peerInfoProvider;
    private final ByteBuffer[] singleDstBuffer;
    private final ByteBuffer[] singleSrcBuffer;
    private final NativeSsl ssl;
    private final SSLParametersImpl sslParameters;
    private int state;
    private static final SSLEngineResult NEED_UNWRAP_OK = new SSLEngineResult(SSLEngineResult.Status.OK, SSLEngineResult.HandshakeStatus.NEED_UNWRAP, 0, 0);
    private static final SSLEngineResult NEED_UNWRAP_CLOSED = new SSLEngineResult(SSLEngineResult.Status.CLOSED, SSLEngineResult.HandshakeStatus.NEED_UNWRAP, 0, 0);
    private static final SSLEngineResult NEED_WRAP_OK = new SSLEngineResult(SSLEngineResult.Status.OK, SSLEngineResult.HandshakeStatus.NEED_WRAP, 0, 0);
    private static final SSLEngineResult NEED_WRAP_CLOSED = new SSLEngineResult(SSLEngineResult.Status.CLOSED, SSLEngineResult.HandshakeStatus.NEED_WRAP, 0, 0);
    private static final SSLEngineResult CLOSED_NOT_HANDSHAKING = new SSLEngineResult(SSLEngineResult.Status.CLOSED, SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING, 0, 0);
    private static final ByteBuffer EMPTY = ByteBuffer.allocateDirect(0);

    ConscryptEngine(SSLParametersImpl sSLParametersImpl) {
        this.state = 0;
        this.externalSession = Platform.wrapSSLSession(new ExternalSession(new ExternalSession.Provider() {
            @Override
            public ConscryptSession provideSession() {
                return ConscryptEngine.this.provideSession();
            }
        }));
        this.singleSrcBuffer = new ByteBuffer[1];
        this.singleDstBuffer = new ByteBuffer[1];
        this.sslParameters = sSLParametersImpl;
        this.peerInfoProvider = PeerInfoProvider.nullProvider();
        this.ssl = newSsl(sSLParametersImpl, this);
        this.networkBio = this.ssl.newBio();
        this.activeSession = new ActiveSession(this.ssl, sSLParametersImpl.getSessionContext());
    }

    ConscryptEngine(String str, int i, SSLParametersImpl sSLParametersImpl) {
        this.state = 0;
        this.externalSession = Platform.wrapSSLSession(new ExternalSession(new ExternalSession.Provider() {
            @Override
            public ConscryptSession provideSession() {
                return ConscryptEngine.this.provideSession();
            }
        }));
        this.singleSrcBuffer = new ByteBuffer[1];
        this.singleDstBuffer = new ByteBuffer[1];
        this.sslParameters = sSLParametersImpl;
        this.peerInfoProvider = PeerInfoProvider.forHostAndPort(str, i);
        this.ssl = newSsl(sSLParametersImpl, this);
        this.networkBio = this.ssl.newBio();
        this.activeSession = new ActiveSession(this.ssl, sSLParametersImpl.getSessionContext());
    }

    ConscryptEngine(SSLParametersImpl sSLParametersImpl, PeerInfoProvider peerInfoProvider) {
        this.state = 0;
        this.externalSession = Platform.wrapSSLSession(new ExternalSession(new ExternalSession.Provider() {
            @Override
            public ConscryptSession provideSession() {
                return ConscryptEngine.this.provideSession();
            }
        }));
        this.singleSrcBuffer = new ByteBuffer[1];
        this.singleDstBuffer = new ByteBuffer[1];
        this.sslParameters = sSLParametersImpl;
        this.peerInfoProvider = (PeerInfoProvider) Preconditions.checkNotNull(peerInfoProvider, "peerInfoProvider");
        this.ssl = newSsl(sSLParametersImpl, this);
        this.networkBio = this.ssl.newBio();
        this.activeSession = new ActiveSession(this.ssl, sSLParametersImpl.getSessionContext());
    }

    private static NativeSsl newSsl(SSLParametersImpl sSLParametersImpl, ConscryptEngine conscryptEngine) {
        try {
            return NativeSsl.newInstance(sSLParametersImpl, conscryptEngine, conscryptEngine, conscryptEngine);
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    void setBufferAllocator(BufferAllocator bufferAllocator) {
        synchronized (this.ssl) {
            if (isHandshakeStarted()) {
                throw new IllegalStateException("Could not set buffer allocator after the initial handshake has begun.");
            }
            this.bufferAllocator = bufferAllocator;
        }
    }

    @Override
    int maxSealOverhead() {
        return this.maxSealOverhead;
    }

    @Override
    void setChannelIdEnabled(boolean z) {
        synchronized (this.ssl) {
            if (getUseClientMode()) {
                throw new IllegalStateException("Not allowed in client mode");
            }
            if (isHandshakeStarted()) {
                throw new IllegalStateException("Could not enable/disable Channel ID after the initial handshake has begun.");
            }
            this.sslParameters.channelIdEnabled = z;
        }
    }

    @Override
    byte[] getChannelId() throws SSLException {
        byte[] tlsChannelId;
        synchronized (this.ssl) {
            if (getUseClientMode()) {
                throw new IllegalStateException("Not allowed in client mode");
            }
            if (isHandshakeStarted()) {
                throw new IllegalStateException("Channel ID is only available after handshake completes");
            }
            tlsChannelId = this.ssl.getTlsChannelId();
        }
        return tlsChannelId;
    }

    @Override
    void setChannelIdPrivateKey(PrivateKey privateKey) {
        if (!getUseClientMode()) {
            throw new IllegalStateException("Not allowed in server mode");
        }
        synchronized (this.ssl) {
            if (isHandshakeStarted()) {
                throw new IllegalStateException("Could not change Channel ID private key after the initial handshake has begun.");
            }
            ECParameterSpec eCParameterSpec = null;
            if (privateKey == null) {
                this.sslParameters.channelIdEnabled = false;
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
    }

    @Override
    void setHandshakeListener(HandshakeListener handshakeListener) {
        synchronized (this.ssl) {
            if (isHandshakeStarted()) {
                throw new IllegalStateException("Handshake listener must be set before starting the handshake.");
            }
            this.handshakeListener = handshakeListener;
        }
    }

    private boolean isHandshakeStarted() {
        switch (this.state) {
            case 0:
            case 1:
                return false;
            default:
                return true;
        }
    }

    @Override
    void setHostname(String str) {
        this.sslParameters.setUseSni(str != null);
        this.peerHostname = str;
    }

    @Override
    String getHostname() {
        return this.peerHostname != null ? this.peerHostname : this.peerInfoProvider.getHostname();
    }

    @Override
    public String getPeerHost() {
        return this.peerHostname != null ? this.peerHostname : this.peerInfoProvider.getHostnameOrIP();
    }

    @Override
    public int getPeerPort() {
        return this.peerInfoProvider.getPort();
    }

    @Override
    public void beginHandshake() throws SSLException {
        synchronized (this.ssl) {
            beginHandshakeInternal();
        }
    }

    private void beginHandshakeInternal() throws SSLException {
        NativeSslSession cachedSession;
        int i = this.state;
        switch (i) {
            case 0:
                throw new IllegalStateException("Client/server mode must be set before handshake");
            case 1:
                transitionTo(2);
                try {
                    try {
                        this.ssl.initialize(getHostname(), this.channelIdPrivateKey);
                        if (getUseClientMode() && (cachedSession = clientSessionContext().getCachedSession(getHostname(), getPeerPort(), this.sslParameters)) != null) {
                            cachedSession.offerToResume(this.ssl);
                        }
                        this.maxSealOverhead = this.ssl.getMaxSealOverhead();
                        handshake();
                        return;
                    } catch (IOException e) {
                        if (e.getMessage().contains("unexpected CCS")) {
                            Platform.logEvent(String.format("ssl_unexpected_ccs: host=%s", getPeerHost()));
                        }
                        throw SSLUtils.toSSLHandshakeException(e);
                    }
                } catch (Throwable th) {
                    closeAndFreeResources();
                    throw th;
                }
            default:
                switch (i) {
                    case 6:
                    case 7:
                    case 8:
                        throw new IllegalStateException("Engine has already been closed");
                    default:
                        return;
                }
        }
    }

    @Override
    public void closeInbound() throws SSLException {
        synchronized (this.ssl) {
            if (this.state != 8 && this.state != 6) {
                if (isOutboundDone()) {
                    transitionTo(8);
                } else {
                    transitionTo(6);
                }
            }
        }
    }

    @Override
    public void closeOutbound() {
        synchronized (this.ssl) {
            if (this.state != 8 && this.state != 7) {
                if (isHandshakeStarted()) {
                    sendSSLShutdown();
                    if (isInboundDone()) {
                        closeAndFreeResources();
                    } else {
                        transitionTo(7);
                    }
                } else {
                    closeAndFreeResources();
                }
            }
        }
    }

    @Override
    public Runnable getDelegatedTask() {
        return null;
    }

    @Override
    public String[] getEnabledCipherSuites() {
        return this.sslParameters.getEnabledCipherSuites();
    }

    @Override
    public String[] getEnabledProtocols() {
        return this.sslParameters.getEnabledProtocols();
    }

    @Override
    public boolean getEnableSessionCreation() {
        return this.sslParameters.getEnableSessionCreation();
    }

    @Override
    public SSLParameters getSSLParameters() {
        SSLParameters sSLParameters = super.getSSLParameters();
        Platform.getSSLParameters(sSLParameters, this.sslParameters, this);
        return sSLParameters;
    }

    @Override
    public void setSSLParameters(SSLParameters sSLParameters) {
        super.setSSLParameters(sSLParameters);
        Platform.setSSLParameters(sSLParameters, this.sslParameters, this);
    }

    @Override
    public SSLEngineResult.HandshakeStatus getHandshakeStatus() {
        SSLEngineResult.HandshakeStatus handshakeStatusInternal;
        synchronized (this.ssl) {
            handshakeStatusInternal = getHandshakeStatusInternal();
        }
        return handshakeStatusInternal;
    }

    private SSLEngineResult.HandshakeStatus getHandshakeStatusInternal() {
        if (this.handshakeFinished) {
            return SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;
        }
        switch (this.state) {
            case 0:
            case 1:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
                return SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;
            case 2:
                return pendingStatus(pendingOutboundEncryptedBytes());
            case CTConstants.CERTIFICATE_LENGTH_BYTES:
                return SSLEngineResult.HandshakeStatus.NEED_WRAP;
            default:
                throw new IllegalStateException("Unexpected engine state: " + this.state);
        }
    }

    private int pendingOutboundEncryptedBytes() {
        return this.networkBio.getPendingWrittenBytes();
    }

    private int pendingInboundCleartextBytes() {
        return this.ssl.getPendingReadableBytes();
    }

    private static SSLEngineResult.HandshakeStatus pendingStatus(int i) {
        return i > 0 ? SSLEngineResult.HandshakeStatus.NEED_WRAP : SSLEngineResult.HandshakeStatus.NEED_UNWRAP;
    }

    @Override
    public boolean getNeedClientAuth() {
        return this.sslParameters.getNeedClientAuth();
    }

    @Override
    SSLSession handshakeSession() {
        synchronized (this.ssl) {
            if (this.state == 2) {
                return Platform.wrapSSLSession(new ExternalSession(new ExternalSession.Provider() {
                    @Override
                    public ConscryptSession provideSession() {
                        return ConscryptEngine.this.provideHandshakeSession();
                    }
                }));
            }
            return null;
        }
    }

    @Override
    public SSLSession getSession() {
        return this.externalSession;
    }

    private ConscryptSession provideSession() {
        synchronized (this.ssl) {
            if (this.state == 8) {
                return this.closedSession != null ? this.closedSession : SSLNullSession.getNullSession();
            }
            if (this.state < 3) {
                return SSLNullSession.getNullSession();
            }
            return this.activeSession;
        }
    }

    private ConscryptSession provideHandshakeSession() {
        ConscryptSession nullSession;
        synchronized (this.ssl) {
            nullSession = this.state == 2 ? this.activeSession : SSLNullSession.getNullSession();
        }
        return nullSession;
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return NativeCrypto.getSupportedCipherSuites();
    }

    @Override
    public String[] getSupportedProtocols() {
        return NativeCrypto.getSupportedProtocols();
    }

    @Override
    public boolean getUseClientMode() {
        return this.sslParameters.getUseClientMode();
    }

    @Override
    public boolean getWantClientAuth() {
        return this.sslParameters.getWantClientAuth();
    }

    @Override
    public boolean isInboundDone() {
        boolean z;
        synchronized (this.ssl) {
            z = this.state == 8 || this.state == 6 || this.ssl.wasShutdownReceived();
        }
        return z;
    }

    @Override
    public boolean isOutboundDone() {
        boolean z;
        synchronized (this.ssl) {
            z = this.state == 8 || this.state == 7 || this.ssl.wasShutdownSent();
        }
        return z;
    }

    @Override
    public void setEnabledCipherSuites(String[] strArr) {
        this.sslParameters.setEnabledCipherSuites(strArr);
    }

    @Override
    public void setEnabledProtocols(String[] strArr) {
        this.sslParameters.setEnabledProtocols(strArr);
    }

    @Override
    public void setEnableSessionCreation(boolean z) {
        this.sslParameters.setEnableSessionCreation(z);
    }

    @Override
    public void setNeedClientAuth(boolean z) {
        this.sslParameters.setNeedClientAuth(z);
    }

    @Override
    public void setUseClientMode(boolean z) {
        synchronized (this.ssl) {
            if (isHandshakeStarted()) {
                throw new IllegalArgumentException("Can not change mode after handshake: state == " + this.state);
            }
            transitionTo(1);
            this.sslParameters.setUseClientMode(z);
        }
    }

    @Override
    public void setWantClientAuth(boolean z) {
        this.sslParameters.setWantClientAuth(z);
    }

    @Override
    public SSLEngineResult unwrap(ByteBuffer byteBuffer, ByteBuffer byteBuffer2) throws SSLException {
        SSLEngineResult sSLEngineResultUnwrap;
        synchronized (this.ssl) {
            try {
                sSLEngineResultUnwrap = unwrap(singleSrcBuffer(byteBuffer), singleDstBuffer(byteBuffer2));
            } finally {
                resetSingleSrcBuffer();
                resetSingleDstBuffer();
            }
        }
        return sSLEngineResultUnwrap;
    }

    @Override
    public SSLEngineResult unwrap(ByteBuffer byteBuffer, ByteBuffer[] byteBufferArr) throws SSLException {
        SSLEngineResult sSLEngineResultUnwrap;
        synchronized (this.ssl) {
            try {
                sSLEngineResultUnwrap = unwrap(singleSrcBuffer(byteBuffer), byteBufferArr);
            } finally {
                resetSingleSrcBuffer();
            }
        }
        return sSLEngineResultUnwrap;
    }

    @Override
    public SSLEngineResult unwrap(ByteBuffer byteBuffer, ByteBuffer[] byteBufferArr, int i, int i2) throws SSLException {
        SSLEngineResult sSLEngineResultUnwrap;
        synchronized (this.ssl) {
            try {
                sSLEngineResultUnwrap = unwrap(singleSrcBuffer(byteBuffer), 0, 1, byteBufferArr, i, i2);
            } finally {
                resetSingleSrcBuffer();
            }
        }
        return sSLEngineResultUnwrap;
    }

    @Override
    SSLEngineResult unwrap(ByteBuffer[] byteBufferArr, ByteBuffer[] byteBufferArr2) throws SSLException {
        Preconditions.checkArgument(byteBufferArr != null, "srcs is null");
        Preconditions.checkArgument(byteBufferArr2 != null, "dsts is null");
        return unwrap(byteBufferArr, 0, byteBufferArr.length, byteBufferArr2, 0, byteBufferArr2.length);
    }

    @Override
    SSLEngineResult unwrap(ByteBuffer[] byteBufferArr, int i, int i2, ByteBuffer[] byteBufferArr2, int i3, int i4) throws SSLException {
        int encryptedPacketLength;
        int i5;
        int i6;
        boolean z = true;
        Preconditions.checkArgument(byteBufferArr != null, "srcs is null");
        Preconditions.checkArgument(byteBufferArr2 != null, "dsts is null");
        int i7 = i2 + i;
        Preconditions.checkPositionIndexes(i, i7, byteBufferArr.length);
        int i8 = i3 + i4;
        Preconditions.checkPositionIndexes(i3, i8, byteBufferArr2.length);
        int iCalcDstsLength = calcDstsLength(byteBufferArr2, i3, i4);
        long jCalcSrcsLength = calcSrcsLength(byteBufferArr, i, i7);
        synchronized (this.ssl) {
            int i9 = this.state;
            if (i9 == 6 || i9 == 8) {
                return new SSLEngineResult(SSLEngineResult.Status.CLOSED, getHandshakeStatusInternal(), 0, 0);
            }
            switch (i9) {
                case 0:
                    throw new IllegalStateException("Client/server mode must be set before calling unwrap");
                case 1:
                    beginHandshakeInternal();
                    break;
            }
            SSLEngineResult.HandshakeStatus handshakeStatusInternal = SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;
            if (!this.handshakeFinished) {
                handshakeStatusInternal = handshake();
                if (handshakeStatusInternal == SSLEngineResult.HandshakeStatus.NEED_WRAP) {
                    return NEED_WRAP_OK;
                }
                if (this.state == 8) {
                    return NEED_WRAP_CLOSED;
                }
            }
            if (pendingInboundCleartextBytes() > 0) {
                z = false;
            }
            if (jCalcSrcsLength <= 0 || !z) {
                if (z) {
                    return new SSLEngineResult(SSLEngineResult.Status.BUFFER_UNDERFLOW, getHandshakeStatus(), 0, 0);
                }
                encryptedPacketLength = 0;
            } else {
                if (jCalcSrcsLength < 5) {
                    return new SSLEngineResult(SSLEngineResult.Status.BUFFER_UNDERFLOW, getHandshakeStatus(), 0, 0);
                }
                encryptedPacketLength = SSLUtils.getEncryptedPacketLength(byteBufferArr, i);
                if (encryptedPacketLength < 0) {
                    throw new SSLException("Unable to parse TLS packet header");
                }
                if (jCalcSrcsLength < encryptedPacketLength) {
                    return new SSLEngineResult(SSLEngineResult.Status.BUFFER_UNDERFLOW, getHandshakeStatus(), 0, 0);
                }
            }
            if (encryptedPacketLength > 0 && i < i7) {
                i5 = 0;
                while (true) {
                    ByteBuffer byteBuffer = byteBufferArr[i];
                    int iRemaining = byteBuffer.remaining();
                    if (iRemaining == 0) {
                        i++;
                    } else {
                        int iWriteEncryptedData = writeEncryptedData(byteBuffer, Math.min(encryptedPacketLength, iRemaining));
                        if (iWriteEncryptedData > 0) {
                            i5 += iWriteEncryptedData;
                            encryptedPacketLength -= iWriteEncryptedData;
                            if (encryptedPacketLength != 0 && iWriteEncryptedData == iRemaining) {
                                i++;
                            }
                        } else {
                            NativeCrypto.SSL_clear_error();
                        }
                    }
                    if (i >= i7) {
                    }
                }
            } else {
                i5 = 0;
            }
            try {
                if (iCalcDstsLength > 0) {
                    i6 = 0;
                    while (i3 < i8) {
                        try {
                            ByteBuffer byteBuffer2 = byteBufferArr2[i3];
                            if (byteBuffer2.hasRemaining()) {
                                int plaintextData = readPlaintextData(byteBuffer2);
                                if (plaintextData > 0) {
                                    i6 += plaintextData;
                                    if (byteBuffer2.hasRemaining()) {
                                        if ((this.handshakeFinished ? pendingInboundCleartextBytes() : 0) > 0) {
                                            SSLEngineResult.Status status = SSLEngineResult.Status.BUFFER_OVERFLOW;
                                            if (handshakeStatusInternal != SSLEngineResult.HandshakeStatus.FINISHED) {
                                                handshakeStatusInternal = getHandshakeStatusInternal();
                                            }
                                            return new SSLEngineResult(status, mayFinishHandshake(handshakeStatusInternal), i5, i6);
                                        }
                                        return newResult(i5, i6, handshakeStatusInternal);
                                    }
                                } else {
                                    if (plaintextData != -6) {
                                        switch (plaintextData) {
                                            case -3:
                                            case -2:
                                                return newResult(i5, i6, handshakeStatusInternal);
                                            default:
                                                sendSSLShutdown();
                                                throw newSslExceptionWithMessage("SSL_read");
                                        }
                                    }
                                    closeInbound();
                                    sendSSLShutdown();
                                    return new SSLEngineResult(SSLEngineResult.Status.CLOSED, pendingOutboundEncryptedBytes() > 0 ? SSLEngineResult.HandshakeStatus.NEED_WRAP : SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING, i5, i6);
                                }
                            }
                            i3++;
                        } catch (InterruptedIOException e) {
                            return newResult(i5, i6, handshakeStatusInternal);
                        } catch (SSLException e2) {
                            e = e2;
                            if (pendingOutboundEncryptedBytes() <= 0) {
                                if (!this.handshakeFinished && this.handshakeException == null) {
                                    this.handshakeException = e;
                                }
                                return new SSLEngineResult(SSLEngineResult.Status.OK, SSLEngineResult.HandshakeStatus.NEED_WRAP, i5, i6);
                            }
                            sendSSLShutdown();
                            throw convertException(e);
                        }
                    }
                    if ((this.handshakeFinished ? pendingInboundCleartextBytes() : 0) > 0) {
                    }
                } else {
                    try {
                        readPlaintextData(EMPTY);
                        i6 = 0;
                        if ((this.handshakeFinished ? pendingInboundCleartextBytes() : 0) > 0) {
                        }
                    } catch (InterruptedIOException e3) {
                        i6 = 0;
                        return newResult(i5, i6, handshakeStatusInternal);
                    } catch (SSLException e4) {
                        e = e4;
                        i6 = 0;
                        if (pendingOutboundEncryptedBytes() <= 0) {
                        }
                    }
                }
            } catch (EOFException e5) {
                closeAll();
                throw convertException(e5);
            } catch (IOException e6) {
                sendSSLShutdown();
                throw convertException(e6);
            }
        }
    }

    private static int calcDstsLength(ByteBuffer[] byteBufferArr, int i, int i2) {
        int iRemaining = 0;
        for (int i3 = 0; i3 < byteBufferArr.length; i3++) {
            ByteBuffer byteBuffer = byteBufferArr[i3];
            Preconditions.checkArgument(byteBuffer != null, "dsts[%d] is null", Integer.valueOf(i3));
            if (byteBuffer.isReadOnly()) {
                throw new ReadOnlyBufferException();
            }
            if (i3 >= i && i3 < i + i2) {
                iRemaining += byteBuffer.remaining();
            }
        }
        return iRemaining;
    }

    private static long calcSrcsLength(ByteBuffer[] byteBufferArr, int i, int i2) {
        long jRemaining = 0;
        while (i < i2) {
            ByteBuffer byteBuffer = byteBufferArr[i];
            if (byteBuffer == null) {
                throw new IllegalArgumentException("srcs[" + i + "] is null");
            }
            jRemaining += (long) byteBuffer.remaining();
            i++;
        }
        return jRemaining;
    }

    private SSLEngineResult.HandshakeStatus handshake() throws SSLException {
        try {
            try {
                try {
                    if (this.handshakeException != null) {
                        if (pendingOutboundEncryptedBytes() > 0) {
                            return SSLEngineResult.HandshakeStatus.NEED_WRAP;
                        }
                        SSLException sSLException = this.handshakeException;
                        this.handshakeException = null;
                        throw sSLException;
                    }
                    switch (this.ssl.doHandshake()) {
                        case 2:
                            return pendingStatus(pendingOutboundEncryptedBytes());
                        case CTConstants.CERTIFICATE_LENGTH_BYTES:
                            return SSLEngineResult.HandshakeStatus.NEED_WRAP;
                        default:
                            this.activeSession.onPeerCertificateAvailable(getPeerHost(), getPeerPort());
                            finishHandshake();
                            return SSLEngineResult.HandshakeStatus.FINISHED;
                    }
                } catch (SSLException e) {
                    if (pendingOutboundEncryptedBytes() > 0) {
                        this.handshakeException = e;
                        return SSLEngineResult.HandshakeStatus.NEED_WRAP;
                    }
                    sendSSLShutdown();
                    throw e;
                }
            } catch (IOException e2) {
                sendSSLShutdown();
                throw e2;
            }
        } catch (Exception e3) {
            throw SSLUtils.toSSLHandshakeException(e3);
        }
    }

    private void finishHandshake() throws SSLException {
        this.handshakeFinished = true;
        if (this.handshakeListener != null) {
            this.handshakeListener.onHandshakeFinished();
        }
    }

    private int writePlaintextData(ByteBuffer byteBuffer, int i) throws Throwable {
        int iWritePlaintextDataHeap;
        try {
            int iPosition = byteBuffer.position();
            if (byteBuffer.isDirect()) {
                iWritePlaintextDataHeap = writePlaintextDataDirect(byteBuffer, iPosition, i);
            } else {
                iWritePlaintextDataHeap = writePlaintextDataHeap(byteBuffer, iPosition, i);
            }
            if (iWritePlaintextDataHeap > 0) {
                byteBuffer.position(iPosition + iWritePlaintextDataHeap);
            }
            return iWritePlaintextDataHeap;
        } catch (Exception e) {
            throw convertException(e);
        }
    }

    private int writePlaintextDataDirect(ByteBuffer byteBuffer, int i, int i2) throws IOException {
        return this.ssl.writeDirectByteBuffer(directByteBufferAddress(byteBuffer, i), i2);
    }

    private int writePlaintextDataHeap(ByteBuffer byteBuffer, int i, int i2) throws Throwable {
        AllocatedBuffer allocatedBufferAllocateDirectBuffer;
        ByteBuffer orCreateLazyDirectBuffer;
        try {
            if (this.bufferAllocator == null) {
                allocatedBufferAllocateDirectBuffer = null;
                orCreateLazyDirectBuffer = getOrCreateLazyDirectBuffer();
            } else {
                allocatedBufferAllocateDirectBuffer = this.bufferAllocator.allocateDirectBuffer(i2);
                try {
                    orCreateLazyDirectBuffer = allocatedBufferAllocateDirectBuffer.nioBuffer();
                } catch (Throwable th) {
                    th = th;
                    if (allocatedBufferAllocateDirectBuffer != null) {
                        allocatedBufferAllocateDirectBuffer.release();
                    }
                    throw th;
                }
            }
            int iLimit = byteBuffer.limit();
            int iMin = Math.min(i2, orCreateLazyDirectBuffer.remaining());
            byteBuffer.limit(i + iMin);
            orCreateLazyDirectBuffer.put(byteBuffer);
            orCreateLazyDirectBuffer.flip();
            byteBuffer.limit(iLimit);
            byteBuffer.position(i);
            int iWritePlaintextDataDirect = writePlaintextDataDirect(orCreateLazyDirectBuffer, 0, iMin);
            if (allocatedBufferAllocateDirectBuffer != null) {
                allocatedBufferAllocateDirectBuffer.release();
            }
            return iWritePlaintextDataDirect;
        } catch (Throwable th2) {
            th = th2;
            allocatedBufferAllocateDirectBuffer = null;
        }
    }

    private int readPlaintextData(ByteBuffer byteBuffer) throws IOException {
        try {
            int iPosition = byteBuffer.position();
            int iMin = Math.min(16709, byteBuffer.limit() - iPosition);
            if (byteBuffer.isDirect()) {
                int plaintextDataDirect = readPlaintextDataDirect(byteBuffer, iPosition, iMin);
                if (plaintextDataDirect > 0) {
                    byteBuffer.position(iPosition + plaintextDataDirect);
                }
                return plaintextDataDirect;
            }
            return readPlaintextDataHeap(byteBuffer, iMin);
        } catch (CertificateException e) {
            throw convertException(e);
        }
    }

    private int readPlaintextDataDirect(ByteBuffer byteBuffer, int i, int i2) throws IOException, CertificateException {
        return this.ssl.readDirectByteBuffer(directByteBufferAddress(byteBuffer, i), i2);
    }

    private int readPlaintextDataHeap(ByteBuffer byteBuffer, int i) throws Throwable {
        AllocatedBuffer allocatedBufferAllocateDirectBuffer;
        ByteBuffer orCreateLazyDirectBuffer;
        try {
            if (this.bufferAllocator == null) {
                allocatedBufferAllocateDirectBuffer = null;
                orCreateLazyDirectBuffer = getOrCreateLazyDirectBuffer();
            } else {
                allocatedBufferAllocateDirectBuffer = this.bufferAllocator.allocateDirectBuffer(i);
                try {
                    orCreateLazyDirectBuffer = allocatedBufferAllocateDirectBuffer.nioBuffer();
                } catch (Throwable th) {
                    th = th;
                    if (allocatedBufferAllocateDirectBuffer != null) {
                        allocatedBufferAllocateDirectBuffer.release();
                    }
                    throw th;
                }
            }
            int plaintextDataDirect = readPlaintextDataDirect(orCreateLazyDirectBuffer, 0, Math.min(i, orCreateLazyDirectBuffer.remaining()));
            if (plaintextDataDirect > 0) {
                orCreateLazyDirectBuffer.position(plaintextDataDirect);
                orCreateLazyDirectBuffer.flip();
                byteBuffer.put(orCreateLazyDirectBuffer);
            }
            if (allocatedBufferAllocateDirectBuffer != null) {
                allocatedBufferAllocateDirectBuffer.release();
            }
            return plaintextDataDirect;
        } catch (Throwable th2) {
            th = th2;
            allocatedBufferAllocateDirectBuffer = null;
        }
    }

    private SSLException convertException(Throwable th) {
        if ((th instanceof SSLHandshakeException) || !this.handshakeFinished) {
            return SSLUtils.toSSLHandshakeException(th);
        }
        return SSLUtils.toSSLException(th);
    }

    private int writeEncryptedData(ByteBuffer byteBuffer, int i) throws Throwable {
        int iWriteEncryptedDataHeap;
        try {
            int iPosition = byteBuffer.position();
            if (byteBuffer.isDirect()) {
                iWriteEncryptedDataHeap = writeEncryptedDataDirect(byteBuffer, iPosition, i);
            } else {
                iWriteEncryptedDataHeap = writeEncryptedDataHeap(byteBuffer, iPosition, i);
            }
            if (iWriteEncryptedDataHeap > 0) {
                byteBuffer.position(iPosition + iWriteEncryptedDataHeap);
            }
            return iWriteEncryptedDataHeap;
        } catch (IOException e) {
            throw new SSLException(e);
        }
    }

    private int writeEncryptedDataDirect(ByteBuffer byteBuffer, int i, int i2) throws IOException {
        return this.networkBio.writeDirectByteBuffer(directByteBufferAddress(byteBuffer, i), i2);
    }

    private int writeEncryptedDataHeap(ByteBuffer byteBuffer, int i, int i2) throws Throwable {
        AllocatedBuffer allocatedBufferAllocateDirectBuffer;
        ByteBuffer orCreateLazyDirectBuffer;
        try {
            if (this.bufferAllocator == null) {
                allocatedBufferAllocateDirectBuffer = null;
                orCreateLazyDirectBuffer = getOrCreateLazyDirectBuffer();
            } else {
                allocatedBufferAllocateDirectBuffer = this.bufferAllocator.allocateDirectBuffer(i2);
                try {
                    orCreateLazyDirectBuffer = allocatedBufferAllocateDirectBuffer.nioBuffer();
                } catch (Throwable th) {
                    th = th;
                    if (allocatedBufferAllocateDirectBuffer != null) {
                        allocatedBufferAllocateDirectBuffer.release();
                    }
                    throw th;
                }
            }
            int iLimit = byteBuffer.limit();
            int iMin = Math.min(Math.min(iLimit - i, i2), orCreateLazyDirectBuffer.remaining());
            byteBuffer.limit(i + iMin);
            orCreateLazyDirectBuffer.put(byteBuffer);
            byteBuffer.limit(iLimit);
            byteBuffer.position(i);
            int iWriteEncryptedDataDirect = writeEncryptedDataDirect(orCreateLazyDirectBuffer, 0, iMin);
            byteBuffer.position(i);
            if (allocatedBufferAllocateDirectBuffer != null) {
                allocatedBufferAllocateDirectBuffer.release();
            }
            return iWriteEncryptedDataDirect;
        } catch (Throwable th2) {
            th = th2;
            allocatedBufferAllocateDirectBuffer = null;
        }
    }

    private ByteBuffer getOrCreateLazyDirectBuffer() {
        if (this.lazyDirectBuffer == null) {
            this.lazyDirectBuffer = ByteBuffer.allocateDirect(Math.max(16384, 16709));
        }
        this.lazyDirectBuffer.clear();
        return this.lazyDirectBuffer;
    }

    private long directByteBufferAddress(ByteBuffer byteBuffer, int i) {
        return NativeCrypto.getDirectBufferAddress(byteBuffer) + ((long) i);
    }

    private SSLEngineResult readPendingBytesFromBIO(ByteBuffer byteBuffer, int i, int i2, SSLEngineResult.HandshakeStatus handshakeStatus) throws SSLException {
        try {
            int iPendingOutboundEncryptedBytes = pendingOutboundEncryptedBytes();
            if (iPendingOutboundEncryptedBytes > 0) {
                if (byteBuffer.remaining() < iPendingOutboundEncryptedBytes) {
                    SSLEngineResult.Status status = SSLEngineResult.Status.BUFFER_OVERFLOW;
                    if (handshakeStatus != SSLEngineResult.HandshakeStatus.FINISHED) {
                        handshakeStatus = getHandshakeStatus(iPendingOutboundEncryptedBytes);
                    }
                    return new SSLEngineResult(status, mayFinishHandshake(handshakeStatus), i, i2);
                }
                int encryptedData = readEncryptedData(byteBuffer, iPendingOutboundEncryptedBytes);
                if (encryptedData <= 0) {
                    NativeCrypto.SSL_clear_error();
                } else {
                    i2 += encryptedData;
                    iPendingOutboundEncryptedBytes -= encryptedData;
                }
                SSLEngineResult.Status engineStatus = getEngineStatus();
                if (handshakeStatus != SSLEngineResult.HandshakeStatus.FINISHED) {
                    handshakeStatus = getHandshakeStatus(iPendingOutboundEncryptedBytes);
                }
                return new SSLEngineResult(engineStatus, mayFinishHandshake(handshakeStatus), i, i2);
            }
            return null;
        } catch (Exception e) {
            throw convertException(e);
        }
    }

    private int readEncryptedData(ByteBuffer byteBuffer, int i) throws SSLException {
        try {
            int iPosition = byteBuffer.position();
            if (byteBuffer.remaining() < i) {
                return 0;
            }
            int iMin = Math.min(i, byteBuffer.limit() - iPosition);
            if (byteBuffer.isDirect()) {
                int encryptedDataDirect = readEncryptedDataDirect(byteBuffer, iPosition, iMin);
                if (encryptedDataDirect > 0) {
                    byteBuffer.position(iPosition + encryptedDataDirect);
                    return encryptedDataDirect;
                }
                return encryptedDataDirect;
            }
            return readEncryptedDataHeap(byteBuffer, iMin);
        } catch (Exception e) {
            throw convertException(e);
        }
    }

    private int readEncryptedDataDirect(ByteBuffer byteBuffer, int i, int i2) throws IOException {
        return this.networkBio.readDirectByteBuffer(directByteBufferAddress(byteBuffer, i), i2);
    }

    private int readEncryptedDataHeap(ByteBuffer byteBuffer, int i) throws Throwable {
        AllocatedBuffer allocatedBufferAllocateDirectBuffer;
        ByteBuffer orCreateLazyDirectBuffer;
        try {
            if (this.bufferAllocator == null) {
                allocatedBufferAllocateDirectBuffer = null;
                orCreateLazyDirectBuffer = getOrCreateLazyDirectBuffer();
            } else {
                allocatedBufferAllocateDirectBuffer = this.bufferAllocator.allocateDirectBuffer(i);
                try {
                    orCreateLazyDirectBuffer = allocatedBufferAllocateDirectBuffer.nioBuffer();
                } catch (Throwable th) {
                    th = th;
                    if (allocatedBufferAllocateDirectBuffer != null) {
                        allocatedBufferAllocateDirectBuffer.release();
                    }
                    throw th;
                }
            }
            int encryptedDataDirect = readEncryptedDataDirect(orCreateLazyDirectBuffer, 0, Math.min(i, orCreateLazyDirectBuffer.remaining()));
            if (encryptedDataDirect > 0) {
                orCreateLazyDirectBuffer.position(encryptedDataDirect);
                orCreateLazyDirectBuffer.flip();
                byteBuffer.put(orCreateLazyDirectBuffer);
            }
            if (allocatedBufferAllocateDirectBuffer != null) {
                allocatedBufferAllocateDirectBuffer.release();
            }
            return encryptedDataDirect;
        } catch (Throwable th2) {
            th = th2;
            allocatedBufferAllocateDirectBuffer = null;
        }
    }

    private SSLEngineResult.HandshakeStatus mayFinishHandshake(SSLEngineResult.HandshakeStatus handshakeStatus) throws SSLException {
        if (!this.handshakeFinished && handshakeStatus == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
            return handshake();
        }
        return handshakeStatus;
    }

    private SSLEngineResult.HandshakeStatus getHandshakeStatus(int i) {
        return !this.handshakeFinished ? pendingStatus(i) : SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;
    }

    private SSLEngineResult.Status getEngineStatus() {
        switch (this.state) {
            case 6:
            case 7:
            case 8:
                return SSLEngineResult.Status.CLOSED;
            default:
                return SSLEngineResult.Status.OK;
        }
    }

    private void closeAll() throws SSLException {
        closeOutbound();
        closeInbound();
    }

    private SSLException newSslExceptionWithMessage(String str) {
        if (!this.handshakeFinished) {
            return new SSLException(str);
        }
        return new SSLHandshakeException(str);
    }

    private SSLEngineResult newResult(int i, int i2, SSLEngineResult.HandshakeStatus handshakeStatus) throws SSLException {
        SSLEngineResult.Status engineStatus = getEngineStatus();
        if (handshakeStatus != SSLEngineResult.HandshakeStatus.FINISHED) {
            handshakeStatus = getHandshakeStatusInternal();
        }
        return new SSLEngineResult(engineStatus, mayFinishHandshake(handshakeStatus), i, i2);
    }

    @Override
    public SSLEngineResult wrap(ByteBuffer byteBuffer, ByteBuffer byteBuffer2) throws SSLException {
        SSLEngineResult sSLEngineResultWrap;
        synchronized (this.ssl) {
            try {
                sSLEngineResultWrap = wrap(singleSrcBuffer(byteBuffer), byteBuffer2);
            } finally {
                resetSingleSrcBuffer();
            }
        }
        return sSLEngineResultWrap;
    }

    @Override
    public SSLEngineResult wrap(ByteBuffer[] byteBufferArr, int i, int i2, ByteBuffer byteBuffer) throws SSLException {
        SSLEngineResult pendingBytesFromBIO;
        Preconditions.checkArgument(byteBufferArr != null, "srcs is null");
        Preconditions.checkArgument(byteBuffer != null, "dst is null");
        int i3 = i2 + i;
        Preconditions.checkPositionIndexes(i, i3, byteBufferArr.length);
        if (byteBuffer.isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        synchronized (this.ssl) {
            switch (this.state) {
                case 0:
                    throw new IllegalStateException("Client/server mode must be set before calling wrap");
                case 1:
                    beginHandshakeInternal();
                    break;
                case 7:
                case 8:
                    SSLEngineResult pendingBytesFromBIO2 = readPendingBytesFromBIO(byteBuffer, 0, 0, SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING);
                    return pendingBytesFromBIO2 != null ? pendingBytesFromBIO2 : new SSLEngineResult(SSLEngineResult.Status.CLOSED, getHandshakeStatusInternal(), 0, 0);
            }
            SSLEngineResult.HandshakeStatus handshakeStatusHandshake = SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;
            if (!this.handshakeFinished) {
                handshakeStatusHandshake = handshake();
                if (handshakeStatusHandshake == SSLEngineResult.HandshakeStatus.NEED_UNWRAP) {
                    return NEED_UNWRAP_OK;
                }
                if (this.state == 8) {
                    return NEED_UNWRAP_CLOSED;
                }
            }
            int iRemaining = 0;
            for (int i4 = i; i4 < i3; i4++) {
                ByteBuffer byteBuffer2 = byteBufferArr[i4];
                if (byteBuffer2 == null) {
                    throw new IllegalArgumentException("srcs[" + i4 + "] is null");
                }
                if (iRemaining != 16384 && ((iRemaining = iRemaining + byteBuffer2.remaining()) > 16384 || iRemaining < 0)) {
                    iRemaining = 16384;
                }
            }
            if (byteBuffer.remaining() < SSLUtils.calculateOutNetBufSize(iRemaining)) {
                return new SSLEngineResult(SSLEngineResult.Status.BUFFER_OVERFLOW, getHandshakeStatusInternal(), 0, 0);
            }
            int iBytesProduced = 0;
            int i5 = 0;
            while (i < i3) {
                ByteBuffer byteBuffer3 = byteBufferArr[i];
                Preconditions.checkArgument(byteBuffer3 != null, "srcs[%d] is null", Integer.valueOf(i));
                while (byteBuffer3.hasRemaining()) {
                    int iWritePlaintextData = writePlaintextData(byteBuffer3, Math.min(byteBuffer3.remaining(), 16384 - i5));
                    if (iWritePlaintextData > 0) {
                        i5 += iWritePlaintextData;
                        SSLEngineResult pendingBytesFromBIO3 = readPendingBytesFromBIO(byteBuffer, i5, iBytesProduced, handshakeStatusHandshake);
                        if (pendingBytesFromBIO3 != null) {
                            if (pendingBytesFromBIO3.getStatus() != SSLEngineResult.Status.OK) {
                                return pendingBytesFromBIO3;
                            }
                            iBytesProduced = pendingBytesFromBIO3.bytesProduced();
                        }
                        if (i5 == 16384) {
                            return (i5 == 0 || (pendingBytesFromBIO = readPendingBytesFromBIO(byteBuffer, 0, iBytesProduced, handshakeStatusHandshake)) == null) ? newResult(i5, iBytesProduced, handshakeStatusHandshake) : pendingBytesFromBIO;
                        }
                    } else {
                        int error = this.ssl.getError(iWritePlaintextData);
                        if (error == 6) {
                            closeAll();
                            SSLEngineResult pendingBytesFromBIO4 = readPendingBytesFromBIO(byteBuffer, i5, iBytesProduced, handshakeStatusHandshake);
                            if (pendingBytesFromBIO4 == null) {
                                pendingBytesFromBIO4 = CLOSED_NOT_HANDSHAKING;
                            }
                            return pendingBytesFromBIO4;
                        }
                        switch (error) {
                            case 2:
                                SSLEngineResult pendingBytesFromBIO5 = readPendingBytesFromBIO(byteBuffer, i5, iBytesProduced, handshakeStatusHandshake);
                                if (pendingBytesFromBIO5 == null) {
                                    pendingBytesFromBIO5 = new SSLEngineResult(getEngineStatus(), SSLEngineResult.HandshakeStatus.NEED_UNWRAP, i5, iBytesProduced);
                                }
                                return pendingBytesFromBIO5;
                            case CTConstants.CERTIFICATE_LENGTH_BYTES:
                                SSLEngineResult pendingBytesFromBIO6 = readPendingBytesFromBIO(byteBuffer, i5, iBytesProduced, handshakeStatusHandshake);
                                if (pendingBytesFromBIO6 == null) {
                                    pendingBytesFromBIO6 = NEED_WRAP_CLOSED;
                                }
                                return pendingBytesFromBIO6;
                            default:
                                sendSSLShutdown();
                                throw newSslExceptionWithMessage("SSL_write");
                        }
                    }
                }
                i++;
            }
            if (i5 == 0) {
            }
        }
    }

    @Override
    public int clientPSKKeyRequested(String str, byte[] bArr, byte[] bArr2) {
        return this.ssl.clientPSKKeyRequested(str, bArr, bArr2);
    }

    @Override
    public int serverPSKKeyRequested(String str, String str2, byte[] bArr) {
        return this.ssl.serverPSKKeyRequested(str, str2, bArr);
    }

    @Override
    public void onSSLStateChange(int i, int i2) {
        synchronized (this.ssl) {
            if (i == 16) {
                transitionTo(2);
            } else if (i == 32) {
                if (this.state != 2 && this.state != 4) {
                    throw new IllegalStateException("Completed handshake while in mode " + this.state);
                }
                transitionTo(3);
            }
        }
    }

    @Override
    public void onNewSessionEstablished(long j) {
        try {
            NativeCrypto.SSL_SESSION_up_ref(j);
            sessionContext().cacheSession(NativeSslSession.newInstance(new NativeRef.SSL_SESSION(j), this.activeSession));
        } catch (Exception e) {
        }
    }

    @Override
    public long serverSessionRequested(byte[] bArr) {
        return 0L;
    }

    @Override
    public void verifyCertificateChain(byte[][] bArr, String str) throws CertificateException {
        if (bArr != null) {
            try {
                if (bArr.length != 0) {
                    X509Certificate[] x509CertificateArrDecodeX509CertificateChain = SSLUtils.decodeX509CertificateChain(bArr);
                    X509TrustManager x509TrustManager = this.sslParameters.getX509TrustManager();
                    if (x509TrustManager == null) {
                        throw new CertificateException("No X.509 TrustManager");
                    }
                    this.activeSession.onPeerCertificatesReceived(getPeerHost(), getPeerPort(), x509CertificateArrDecodeX509CertificateChain);
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
    public void clientCertificateRequested(byte[] bArr, byte[][] bArr2) throws SSLException, CertificateEncodingException {
        this.ssl.chooseClientCertificate(bArr, bArr2);
    }

    private void sendSSLShutdown() {
        try {
            this.ssl.shutdown();
        } catch (IOException e) {
        }
    }

    private void closeAndFreeResources() {
        transitionTo(8);
        if (!this.ssl.isClosed()) {
            this.ssl.close();
            this.networkBio.close();
        }
    }

    protected void finalize() throws Throwable {
        try {
            transitionTo(8);
        } finally {
            super.finalize();
        }
    }

    @Override
    public String chooseServerAlias(X509KeyManager x509KeyManager, String str) {
        if (x509KeyManager instanceof X509ExtendedKeyManager) {
            return ((X509ExtendedKeyManager) x509KeyManager).chooseEngineServerAlias(str, null, this);
        }
        return x509KeyManager.chooseServerAlias(str, null, null);
    }

    @Override
    public String chooseClientAlias(X509KeyManager x509KeyManager, X500Principal[] x500PrincipalArr, String[] strArr) {
        if (x509KeyManager instanceof X509ExtendedKeyManager) {
            return ((X509ExtendedKeyManager) x509KeyManager).chooseEngineClientAlias(strArr, x500PrincipalArr, this);
        }
        return x509KeyManager.chooseClientAlias(strArr, x500PrincipalArr, null);
    }

    @Override
    public String chooseServerPSKIdentityHint(PSKKeyManager pSKKeyManager) {
        return pSKKeyManager.chooseServerKeyIdentityHint(this);
    }

    @Override
    public String chooseClientPSKIdentity(PSKKeyManager pSKKeyManager, String str) {
        return pSKKeyManager.chooseClientKeyIdentity(str, this);
    }

    @Override
    public SecretKey getPSKKey(PSKKeyManager pSKKeyManager, String str, String str2) {
        return pSKKeyManager.getKey(str, str2, this);
    }

    @Override
    void setUseSessionTickets(boolean z) {
        this.sslParameters.setUseSessionTickets(z);
    }

    @Override
    String[] getApplicationProtocols() {
        return this.sslParameters.getApplicationProtocols();
    }

    @Override
    void setApplicationProtocols(String[] strArr) {
        this.sslParameters.setApplicationProtocols(strArr);
    }

    @Override
    void setApplicationProtocolSelector(ApplicationProtocolSelector applicationProtocolSelector) {
        setApplicationProtocolSelector(applicationProtocolSelector == null ? null : new ApplicationProtocolSelectorAdapter(this, applicationProtocolSelector));
    }

    @Override
    byte[] getTlsUnique() {
        return this.ssl.getTlsUnique();
    }

    void setApplicationProtocolSelector(ApplicationProtocolSelectorAdapter applicationProtocolSelectorAdapter) {
        this.sslParameters.setApplicationProtocolSelector(applicationProtocolSelectorAdapter);
    }

    @Override
    public String getApplicationProtocol() {
        return SSLUtils.toProtocolString(this.ssl.getApplicationProtocol());
    }

    @Override
    public String getHandshakeApplicationProtocol() {
        String applicationProtocol;
        synchronized (this.ssl) {
            applicationProtocol = this.state == 2 ? getApplicationProtocol() : null;
        }
        return applicationProtocol;
    }

    private ByteBuffer[] singleSrcBuffer(ByteBuffer byteBuffer) {
        this.singleSrcBuffer[0] = byteBuffer;
        return this.singleSrcBuffer;
    }

    private void resetSingleSrcBuffer() {
        this.singleSrcBuffer[0] = null;
    }

    private ByteBuffer[] singleDstBuffer(ByteBuffer byteBuffer) {
        this.singleDstBuffer[0] = byteBuffer;
        return this.singleDstBuffer;
    }

    private void resetSingleDstBuffer() {
        this.singleDstBuffer[0] = null;
    }

    private ClientSessionContext clientSessionContext() {
        return this.sslParameters.getClientSessionContext();
    }

    private AbstractSessionContext sessionContext() {
        return this.sslParameters.getSessionContext();
    }

    private void transitionTo(int i) {
        if (i == 2) {
            this.handshakeFinished = false;
        } else if (i == 8 && !this.ssl.isClosed() && this.state >= 2 && this.state < 8) {
            this.closedSession = new SessionSnapshot(this.activeSession);
        }
        this.state = i;
    }
}
