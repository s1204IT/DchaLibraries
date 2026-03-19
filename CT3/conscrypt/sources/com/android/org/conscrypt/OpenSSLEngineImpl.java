package com.android.org.conscrypt;

import com.android.org.conscrypt.NativeCrypto;
import com.android.org.conscrypt.SSLParametersImpl;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import javax.crypto.SecretKey;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;

public class OpenSSLEngineImpl extends SSLEngine implements NativeCrypto.SSLHandshakeCallbacks, SSLParametersImpl.AliasChooser, SSLParametersImpl.PSKCallbacks {

    private static final int[] f3x9dd0b943 = null;
    private static OpenSSLBIOSource nullSource = OpenSSLBIOSource.wrap(ByteBuffer.allocate(0));
    OpenSSLKey channelIdPrivateKey;
    private EngineState engineState;
    private OpenSSLSessionImpl handshakeSession;
    private OpenSSLBIOSink handshakeSink;
    private final OpenSSLBIOSink localToRemoteSink;
    private long sslNativePointer;
    private final SSLParametersImpl sslParameters;
    private OpenSSLSessionImpl sslSession;
    private final Object stateLock;

    private static int[] m3x6d6f9ee7() {
        if (f3x9dd0b943 != null) {
            return f3x9dd0b943;
        }
        int[] iArr = new int[EngineState.valuesCustom().length];
        try {
            iArr[EngineState.CLOSED.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[EngineState.CLOSED_INBOUND.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[EngineState.CLOSED_OUTBOUND.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[EngineState.HANDSHAKE_COMPLETED.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[EngineState.HANDSHAKE_STARTED.ordinal()] = 5;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[EngineState.HANDSHAKE_WANTED.ordinal()] = 6;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[EngineState.MODE_SET.ordinal()] = 7;
        } catch (NoSuchFieldError e7) {
        }
        try {
            iArr[EngineState.NEW.ordinal()] = 8;
        } catch (NoSuchFieldError e8) {
        }
        try {
            iArr[EngineState.READY.ordinal()] = 9;
        } catch (NoSuchFieldError e9) {
        }
        try {
            iArr[EngineState.READY_HANDSHAKE_CUT_THROUGH.ordinal()] = 10;
        } catch (NoSuchFieldError e10) {
        }
        f3x9dd0b943 = iArr;
        return iArr;
    }

    private enum EngineState {
        NEW,
        MODE_SET,
        HANDSHAKE_WANTED,
        HANDSHAKE_STARTED,
        HANDSHAKE_COMPLETED,
        READY_HANDSHAKE_CUT_THROUGH,
        READY,
        CLOSED_INBOUND,
        CLOSED_OUTBOUND,
        CLOSED;

        public static EngineState[] valuesCustom() {
            return values();
        }
    }

    public OpenSSLEngineImpl(SSLParametersImpl sslParameters) {
        this.stateLock = new Object();
        this.engineState = EngineState.NEW;
        this.localToRemoteSink = OpenSSLBIOSink.create();
        this.sslParameters = sslParameters;
    }

    public OpenSSLEngineImpl(String host, int port, SSLParametersImpl sslParameters) {
        super(host, port);
        this.stateLock = new Object();
        this.engineState = EngineState.NEW;
        this.localToRemoteSink = OpenSSLBIOSink.create();
        this.sslParameters = sslParameters;
    }

    @Override
    public void beginHandshake() throws SSLException {
        synchronized (this.stateLock) {
            if (this.engineState == EngineState.CLOSED || this.engineState == EngineState.CLOSED_OUTBOUND || this.engineState == EngineState.CLOSED_INBOUND) {
                throw new IllegalStateException("Engine has already been closed");
            }
            if (this.engineState == EngineState.HANDSHAKE_STARTED) {
                throw new IllegalStateException("Handshake has already been started");
            }
            if (this.engineState != EngineState.MODE_SET) {
                throw new IllegalStateException("Client/server mode must be set before handshake");
            }
            if (getUseClientMode()) {
                this.engineState = EngineState.HANDSHAKE_WANTED;
            } else {
                this.engineState = EngineState.HANDSHAKE_STARTED;
            }
        }
        try {
            try {
                AbstractSessionContext sessionContext = this.sslParameters.getSessionContext();
                long sslCtxNativePointer = sessionContext.sslCtxNativePointer;
                this.sslNativePointer = NativeCrypto.SSL_new(sslCtxNativePointer);
                this.sslSession = this.sslParameters.getSessionToReuse(this.sslNativePointer, getPeerHost(), getPeerPort());
                this.sslParameters.setSSLParameters(sslCtxNativePointer, this.sslNativePointer, this, this, getPeerHost());
                this.sslParameters.setCertificateValidation(this.sslNativePointer);
                this.sslParameters.setTlsChannelId(this.sslNativePointer, this.channelIdPrivateKey);
                if (getUseClientMode()) {
                    NativeCrypto.SSL_set_connect_state(this.sslNativePointer);
                } else {
                    NativeCrypto.SSL_set_accept_state(this.sslNativePointer);
                }
                this.handshakeSink = OpenSSLBIOSink.create();
                if (0 == 0) {
                    return;
                }
                synchronized (this.stateLock) {
                    this.engineState = EngineState.CLOSED;
                }
                shutdownAndFreeSslNative();
            } catch (IOException e) {
                String message = e.getMessage();
                if (message.contains("unexpected CCS")) {
                    String logMessage = String.format("ssl_unexpected_ccs: host=%s", getPeerHost());
                    Platform.logEvent(logMessage);
                }
                throw new SSLException(e);
            }
        } catch (Throwable th) {
            if (1 != 0) {
                synchronized (this.stateLock) {
                    this.engineState = EngineState.CLOSED;
                    shutdownAndFreeSslNative();
                }
            }
            throw th;
        }
    }

    @Override
    public void closeInbound() throws SSLException {
        synchronized (this.stateLock) {
            if (this.engineState == EngineState.CLOSED) {
                return;
            }
            if (this.engineState == EngineState.CLOSED_OUTBOUND) {
                this.engineState = EngineState.CLOSED;
            } else {
                this.engineState = EngineState.CLOSED_INBOUND;
            }
        }
    }

    @Override
    public void closeOutbound() {
        synchronized (this.stateLock) {
            if (this.engineState == EngineState.CLOSED || this.engineState == EngineState.CLOSED_OUTBOUND) {
                return;
            }
            if (this.engineState != EngineState.MODE_SET && this.engineState != EngineState.NEW) {
                shutdownAndFreeSslNative();
            }
            if (this.engineState == EngineState.CLOSED_INBOUND) {
                this.engineState = EngineState.CLOSED;
            } else {
                this.engineState = EngineState.CLOSED_OUTBOUND;
            }
            shutdown();
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
    public SSLEngineResult.HandshakeStatus getHandshakeStatus() {
        synchronized (this.stateLock) {
            switch (m3x6d6f9ee7()[this.engineState.ordinal()]) {
                case 1:
                case 2:
                case 3:
                case 7:
                case 8:
                case 9:
                case 10:
                    return SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;
                case 4:
                    if (this.handshakeSink.available() == 0) {
                        this.handshakeSink = null;
                        this.engineState = EngineState.READY;
                        return SSLEngineResult.HandshakeStatus.FINISHED;
                    }
                    return SSLEngineResult.HandshakeStatus.NEED_WRAP;
                case 5:
                    if (this.handshakeSink.available() > 0) {
                        return SSLEngineResult.HandshakeStatus.NEED_WRAP;
                    }
                    return SSLEngineResult.HandshakeStatus.NEED_UNWRAP;
                case 6:
                    if (getUseClientMode()) {
                        return SSLEngineResult.HandshakeStatus.NEED_WRAP;
                    }
                    return SSLEngineResult.HandshakeStatus.NEED_UNWRAP;
                default:
                    throw new IllegalStateException("Unexpected engine state: " + this.engineState);
            }
        }
    }

    @Override
    public boolean getNeedClientAuth() {
        return this.sslParameters.getNeedClientAuth();
    }

    @Override
    public SSLSession getSession() {
        if (this.sslSession == null) {
            return SSLNullSession.getNullSession();
        }
        return this.sslSession;
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
        boolean z = true;
        if (this.sslNativePointer != 0) {
            return (NativeCrypto.SSL_get_shutdown(this.sslNativePointer) & 2) != 0;
        }
        synchronized (this.stateLock) {
            if (this.engineState != EngineState.CLOSED) {
                if (this.engineState != EngineState.CLOSED_INBOUND) {
                    z = false;
                }
            }
        }
        return z;
    }

    @Override
    public boolean isOutboundDone() {
        boolean z = true;
        if (this.sslNativePointer != 0) {
            return (NativeCrypto.SSL_get_shutdown(this.sslNativePointer) & 1) != 0;
        }
        synchronized (this.stateLock) {
            if (this.engineState != EngineState.CLOSED) {
                if (this.engineState != EngineState.CLOSED_OUTBOUND) {
                    z = false;
                }
            }
        }
        return z;
    }

    @Override
    public void setEnabledCipherSuites(String[] suites) {
        this.sslParameters.setEnabledCipherSuites(suites);
    }

    @Override
    public void setEnabledProtocols(String[] protocols) {
        this.sslParameters.setEnabledProtocols(protocols);
    }

    @Override
    public void setEnableSessionCreation(boolean flag) {
        this.sslParameters.setEnableSessionCreation(flag);
    }

    @Override
    public void setNeedClientAuth(boolean need) {
        this.sslParameters.setNeedClientAuth(need);
    }

    @Override
    public void setUseClientMode(boolean mode) {
        synchronized (this.stateLock) {
            if (this.engineState != EngineState.MODE_SET && this.engineState != EngineState.NEW) {
                throw new IllegalArgumentException("Can not change mode after handshake: engineState == " + this.engineState);
            }
            this.engineState = EngineState.MODE_SET;
        }
        this.sslParameters.setUseClientMode(mode);
    }

    @Override
    public void setWantClientAuth(boolean want) {
        this.sslParameters.setWantClientAuth(want);
    }

    private static void checkIndex(int length, int offset, int count) {
        if (offset < 0) {
            throw new IndexOutOfBoundsException("offset < 0");
        }
        if (count < 0) {
            throw new IndexOutOfBoundsException("count < 0");
        }
        if (offset > length) {
            throw new IndexOutOfBoundsException("offset > length");
        }
        if (offset <= length - count) {
        } else {
            throw new IndexOutOfBoundsException("offset + count > length");
        }
    }

    @Override
    public SSLEngineResult unwrap(ByteBuffer src, ByteBuffer[] dsts, int offset, int length) throws Throwable {
        OpenSSLBIOSource source;
        long sslSessionCtx;
        if (src == null) {
            throw new IllegalArgumentException("src == null");
        }
        if (dsts == null) {
            throw new IllegalArgumentException("dsts == null");
        }
        checkIndex(dsts.length, offset, length);
        int dstRemaining = 0;
        for (int i = 0; i < dsts.length; i++) {
            ByteBuffer dst = dsts[i];
            if (dst == null) {
                throw new IllegalArgumentException("one of the dst == null");
            }
            if (dst.isReadOnly()) {
                throw new ReadOnlyBufferException();
            }
            if (i >= offset && i < offset + length) {
                dstRemaining += dst.remaining();
            }
        }
        synchronized (this.stateLock) {
            if (this.engineState == EngineState.CLOSED || this.engineState == EngineState.CLOSED_INBOUND) {
                return new SSLEngineResult(SSLEngineResult.Status.CLOSED, getHandshakeStatus(), 0, 0);
            }
            if (this.engineState == EngineState.NEW || this.engineState == EngineState.MODE_SET) {
                beginHandshake();
            }
            SSLEngineResult.HandshakeStatus handshakeStatus = getHandshakeStatus();
            if (handshakeStatus != SSLEngineResult.HandshakeStatus.NEED_UNWRAP) {
                if (handshakeStatus != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
                    return new SSLEngineResult(SSLEngineResult.Status.OK, handshakeStatus, 0, 0);
                }
                if (dstRemaining == 0) {
                    return new SSLEngineResult(SSLEngineResult.Status.BUFFER_OVERFLOW, getHandshakeStatus(), 0, 0);
                }
                ByteBuffer srcDuplicate = src.duplicate();
                source = OpenSSLBIOSource.wrap(srcDuplicate);
                try {
                    try {
                        int positionBeforeRead = srcDuplicate.position();
                        int produced = 0;
                        boolean shouldStop = false;
                        while (!shouldStop) {
                            ByteBuffer dst2 = getNextAvailableByteBuffer(dsts, offset, length);
                            if (dst2 == null) {
                                shouldStop = true;
                            } else {
                                ByteBuffer arrayDst = dst2;
                                if (dst2.isDirect()) {
                                    arrayDst = ByteBuffer.allocate(dst2.remaining());
                                }
                                int dstOffset = arrayDst.arrayOffset() + arrayDst.position();
                                int internalProduced = NativeCrypto.SSL_read_BIO(this.sslNativePointer, arrayDst.array(), dstOffset, dst2.remaining(), source.getContext(), this.localToRemoteSink.getContext(), this);
                                if (internalProduced <= 0) {
                                    shouldStop = true;
                                } else {
                                    arrayDst.position(arrayDst.position() + internalProduced);
                                    produced += internalProduced;
                                    if (dst2 != arrayDst) {
                                        arrayDst.flip();
                                        dst2.put(arrayDst);
                                    }
                                }
                            }
                        }
                        int consumed = srcDuplicate.position() - positionBeforeRead;
                        src.position(srcDuplicate.position());
                        return new SSLEngineResult(consumed > 0 ? SSLEngineResult.Status.OK : SSLEngineResult.Status.BUFFER_UNDERFLOW, getHandshakeStatus(), consumed, produced);
                    } catch (IOException e) {
                        throw new SSLException(e);
                    }
                } finally {
                    source.release();
                }
            }
            int positionBeforeHandshake = src.position();
            source = OpenSSLBIOSource.wrap(src);
            try {
                sslSessionCtx = NativeCrypto.SSL_do_handshake_bio(this.sslNativePointer, source.getContext(), this.handshakeSink.getContext(), this, getUseClientMode(), this.sslParameters.npnProtocols, this.sslParameters.alpnProtocols);
                if (sslSessionCtx != 0) {
                    try {
                        try {
                            if (this.sslSession != null && this.engineState == EngineState.HANDSHAKE_STARTED) {
                                this.engineState = EngineState.READY_HANDSHAKE_CUT_THROUGH;
                            }
                            this.sslSession = this.sslParameters.setupSession(sslSessionCtx, this.sslNativePointer, this.sslSession, getPeerHost(), getPeerPort(), true);
                        } catch (Exception e2) {
                            e = e2;
                            throw ((SSLHandshakeException) new SSLHandshakeException("Handshake failed").initCause(e));
                        }
                    } catch (Throwable th) {
                        th = th;
                        if (this.sslSession == null && sslSessionCtx != 0) {
                            NativeCrypto.SSL_SESSION_free(sslSessionCtx);
                        }
                        throw th;
                    }
                }
                int bytesWritten = this.handshakeSink.position();
                int bytesConsumed = src.position() - positionBeforeHandshake;
                SSLEngineResult sSLEngineResult = new SSLEngineResult(bytesConsumed > 0 ? SSLEngineResult.Status.OK : SSLEngineResult.Status.BUFFER_UNDERFLOW, getHandshakeStatus(), bytesConsumed, bytesWritten);
                if (this.sslSession == null && sslSessionCtx != 0) {
                    NativeCrypto.SSL_SESSION_free(sslSessionCtx);
                }
                return sSLEngineResult;
            } catch (Exception e3) {
                e = e3;
                sslSessionCtx = 0;
            } catch (Throwable th2) {
                th = th2;
                sslSessionCtx = 0;
                if (this.sslSession == null) {
                    NativeCrypto.SSL_SESSION_free(sslSessionCtx);
                }
                throw th;
            }
        }
    }

    private ByteBuffer getNextAvailableByteBuffer(ByteBuffer[] buffers, int offset, int length) {
        for (int i = offset; i < length; i++) {
            if (buffers[i].remaining() > 0) {
                return buffers[i];
            }
        }
        return null;
    }

    @Override
    public SSLEngineResult wrap(ByteBuffer[] srcs, int offset, int length, ByteBuffer dst) throws Throwable {
        if (srcs == null) {
            throw new IllegalArgumentException("srcs == null");
        }
        if (dst == null) {
            throw new IllegalArgumentException("dst == null");
        }
        if (dst.isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        for (ByteBuffer byteBuffer : srcs) {
            if (byteBuffer == null) {
                throw new IllegalArgumentException("one of the src == null");
            }
        }
        checkIndex(srcs.length, offset, length);
        if (dst.remaining() < 16709) {
            return new SSLEngineResult(SSLEngineResult.Status.BUFFER_OVERFLOW, getHandshakeStatus(), 0, 0);
        }
        synchronized (this.stateLock) {
            if (this.engineState == EngineState.CLOSED || this.engineState == EngineState.CLOSED_OUTBOUND) {
                return new SSLEngineResult(SSLEngineResult.Status.CLOSED, getHandshakeStatus(), 0, 0);
            }
            if (this.engineState == EngineState.NEW || this.engineState == EngineState.MODE_SET) {
                beginHandshake();
            }
            SSLEngineResult.HandshakeStatus handshakeStatus = getHandshakeStatus();
            if (handshakeStatus == SSLEngineResult.HandshakeStatus.NEED_WRAP) {
                if (this.handshakeSink.available() == 0) {
                    try {
                        try {
                            long sslSessionCtx = NativeCrypto.SSL_do_handshake_bio(this.sslNativePointer, nullSource.getContext(), this.handshakeSink.getContext(), this, getUseClientMode(), this.sslParameters.npnProtocols, this.sslParameters.alpnProtocols);
                            if (sslSessionCtx != 0) {
                                try {
                                    if (this.sslSession != null && this.engineState == EngineState.HANDSHAKE_STARTED) {
                                        this.engineState = EngineState.READY_HANDSHAKE_CUT_THROUGH;
                                    }
                                    this.sslSession = this.sslParameters.setupSession(sslSessionCtx, this.sslNativePointer, this.sslSession, getPeerHost(), getPeerPort(), true);
                                } catch (Exception e) {
                                    e = e;
                                    throw ((SSLHandshakeException) new SSLHandshakeException("Handshake failed").initCause(e));
                                }
                            }
                            if (this.sslSession == null && sslSessionCtx != 0) {
                                NativeCrypto.SSL_SESSION_free(sslSessionCtx);
                            }
                        } catch (Throwable th) {
                            th = th;
                            if (this.sslSession == null && 0 != 0) {
                                NativeCrypto.SSL_SESSION_free(0L);
                            }
                            throw th;
                        }
                    } catch (Exception e2) {
                        e = e2;
                    } catch (Throwable th2) {
                        th = th2;
                        if (this.sslSession == null) {
                            NativeCrypto.SSL_SESSION_free(0L);
                        }
                        throw th;
                    }
                }
                int bytesWritten = writeSinkToByteBuffer(this.handshakeSink, dst);
                return new SSLEngineResult(SSLEngineResult.Status.OK, getHandshakeStatus(), 0, bytesWritten);
            }
            if (handshakeStatus != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
                return new SSLEngineResult(SSLEngineResult.Status.OK, handshakeStatus, 0, 0);
            }
            int totalRead = 0;
            byte[] buffer = null;
            try {
                for (ByteBuffer src : srcs) {
                    int toRead = src.remaining();
                    if (buffer == null || toRead > buffer.length) {
                        buffer = new byte[toRead];
                    }
                    src.duplicate().get(buffer, 0, toRead);
                    int numRead = NativeCrypto.SSL_write_BIO(this.sslNativePointer, buffer, toRead, this.localToRemoteSink.getContext(), this);
                    if (numRead > 0) {
                        src.position(src.position() + numRead);
                        totalRead += numRead;
                    }
                }
                return new SSLEngineResult(SSLEngineResult.Status.OK, getHandshakeStatus(), totalRead, writeSinkToByteBuffer(this.localToRemoteSink, dst));
            } catch (IOException e3) {
                throw new SSLException(e3);
            }
        }
    }

    private static int writeSinkToByteBuffer(OpenSSLBIOSink sink, ByteBuffer dst) {
        int toWrite = Math.min(sink.available(), dst.remaining());
        dst.put(sink.toByteArray(), sink.position(), toWrite);
        sink.skip(toWrite);
        return toWrite;
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
        synchronized (this.stateLock) {
            switch (type) {
                case 16:
                    this.engineState = EngineState.HANDSHAKE_STARTED;
                    break;
                case 32:
                    if (this.engineState != EngineState.HANDSHAKE_STARTED && this.engineState != EngineState.READY_HANDSHAKE_CUT_THROUGH) {
                        throw new IllegalStateException("Completed handshake while in mode " + this.engineState);
                    }
                    this.engineState = EngineState.HANDSHAKE_COMPLETED;
                    break;
                    break;
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
                this.handshakeSession = new OpenSSLSessionImpl(sslSessionNativePtr, null, peerCertChain, getPeerHost(), getPeerPort(), null);
                boolean client = this.sslParameters.getUseClientMode();
                if (client) {
                    Platform.checkServerTrusted(x509tm, peerCertChain, authMethod, this);
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
    public void clientCertificateRequested(byte[] keyTypeBytes, byte[][] asn1DerEncodedPrincipals) throws SSLException, CertificateEncodingException {
        this.sslParameters.chooseClientCertificate(keyTypeBytes, asn1DerEncodedPrincipals, this.sslNativePointer, this);
    }

    private void shutdown() {
        try {
            NativeCrypto.SSL_shutdown_BIO(this.sslNativePointer, nullSource.getContext(), this.localToRemoteSink.getContext(), this);
        } catch (IOException e) {
        }
    }

    private void shutdownAndFreeSslNative() {
        try {
            shutdown();
        } finally {
            free();
        }
    }

    private void free() {
        if (this.sslNativePointer == 0) {
            return;
        }
        NativeCrypto.SSL_free(this.sslNativePointer);
        this.sslNativePointer = 0L;
    }

    protected void finalize() throws Throwable {
        try {
            free();
        } finally {
            super.finalize();
        }
    }

    @Override
    public SSLSession getHandshakeSession() {
        return this.handshakeSession;
    }

    @Override
    public String chooseServerAlias(X509KeyManager x509KeyManager, String keyType) {
        if (x509KeyManager instanceof X509ExtendedKeyManager) {
            return x509KeyManager.chooseEngineServerAlias(keyType, null, this);
        }
        return x509KeyManager.chooseServerAlias(keyType, null, null);
    }

    @Override
    public String chooseClientAlias(X509KeyManager x509KeyManager, X500Principal[] issuers, String[] keyTypes) {
        if (x509KeyManager instanceof X509ExtendedKeyManager) {
            return x509KeyManager.chooseEngineClientAlias(keyTypes, issuers, this);
        }
        return x509KeyManager.chooseClientAlias(keyTypes, issuers, null);
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
