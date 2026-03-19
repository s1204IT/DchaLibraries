package com.android.org.conscrypt;

import com.android.org.conscrypt.ct.CTConstants;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.security.PrivateKey;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;

class ConscryptEngineSocket extends OpenSSLSocketImpl {
    private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);
    private final ConscryptEngine engine;
    private final Object handshakeLock;
    private SSLInputStream in;
    private SSLOutputStream out;
    private int state;
    private final Object stateLock;

    ConscryptEngineSocket(SSLParametersImpl sSLParametersImpl) throws IOException {
        this.stateLock = new Object();
        this.handshakeLock = new Object();
        this.state = 0;
        this.engine = newEngine(sSLParametersImpl, this);
    }

    ConscryptEngineSocket(String str, int i, SSLParametersImpl sSLParametersImpl) throws IOException {
        super(str, i);
        this.stateLock = new Object();
        this.handshakeLock = new Object();
        this.state = 0;
        this.engine = newEngine(sSLParametersImpl, this);
    }

    ConscryptEngineSocket(InetAddress inetAddress, int i, SSLParametersImpl sSLParametersImpl) throws IOException {
        super(inetAddress, i);
        this.stateLock = new Object();
        this.handshakeLock = new Object();
        this.state = 0;
        this.engine = newEngine(sSLParametersImpl, this);
    }

    ConscryptEngineSocket(String str, int i, InetAddress inetAddress, int i2, SSLParametersImpl sSLParametersImpl) throws IOException {
        super(str, i, inetAddress, i2);
        this.stateLock = new Object();
        this.handshakeLock = new Object();
        this.state = 0;
        this.engine = newEngine(sSLParametersImpl, this);
    }

    ConscryptEngineSocket(InetAddress inetAddress, int i, InetAddress inetAddress2, int i2, SSLParametersImpl sSLParametersImpl) throws IOException {
        super(inetAddress, i, inetAddress2, i2);
        this.stateLock = new Object();
        this.handshakeLock = new Object();
        this.state = 0;
        this.engine = newEngine(sSLParametersImpl, this);
    }

    ConscryptEngineSocket(Socket socket, String str, int i, boolean z, SSLParametersImpl sSLParametersImpl) throws IOException {
        super(socket, str, i, z);
        this.stateLock = new Object();
        this.handshakeLock = new Object();
        this.state = 0;
        this.engine = newEngine(sSLParametersImpl, this);
    }

    private static ConscryptEngine newEngine(SSLParametersImpl sSLParametersImpl, ConscryptEngineSocket conscryptEngineSocket) {
        ConscryptEngine conscryptEngine = new ConscryptEngine(sSLParametersImpl, conscryptEngineSocket.peerInfoProvider());
        conscryptEngine.setHandshakeListener(new HandshakeListener() {
            @Override
            public void onHandshakeFinished() {
                ConscryptEngineSocket.this.onHandshakeFinished();
            }
        });
        conscryptEngine.setUseClientMode(sSLParametersImpl.getUseClientMode());
        return conscryptEngine;
    }

    @Override
    public final SSLParameters getSSLParameters() {
        return this.engine.getSSLParameters();
    }

    @Override
    public final void setSSLParameters(SSLParameters sSLParameters) {
        this.engine.setSSLParameters(sSLParameters);
    }

    @Override
    public final void startHandshake() throws IOException {
        checkOpen();
        try {
            synchronized (this.handshakeLock) {
                synchronized (this.stateLock) {
                    if (this.state == 0) {
                        this.state = 2;
                        this.engine.beginHandshake();
                        this.in = new SSLInputStream();
                        this.out = new SSLOutputStream();
                        doHandshake();
                    }
                }
            }
        } catch (SSLException e) {
            close();
            throw e;
        } catch (IOException e2) {
            close();
            throw e2;
        } catch (Exception e3) {
            close();
            throw SSLUtils.toSSLHandshakeException(e3);
        }
    }

    private void doHandshake() throws IOException {
        boolean z = false;
        while (!z) {
            try {
                switch (AnonymousClass2.$SwitchMap$javax$net$ssl$SSLEngineResult$HandshakeStatus[this.engine.getHandshakeStatus().ordinal()]) {
                    case 1:
                        if (this.in.readInternal(EmptyArray.BYTE, 0, 0) < 0) {
                            throw SSLUtils.toSSLHandshakeException(new EOFException());
                        }
                        break;
                        break;
                    case 2:
                        this.out.writeInternal(EMPTY_BUFFER);
                        this.out.flushInternal();
                        break;
                    case CTConstants.CERTIFICATE_LENGTH_BYTES:
                        throw new IllegalStateException("Engine tasks are unsupported");
                    case 4:
                    case 5:
                        z = true;
                        break;
                    default:
                        throw new IllegalStateException("Unknown handshake status: " + this.engine.getHandshakeStatus());
                }
            } catch (SSLException e) {
                close();
                throw e;
            } catch (IOException e2) {
                close();
                throw e2;
            } catch (Exception e3) {
                close();
                throw SSLUtils.toSSLHandshakeException(e3);
            }
        }
    }

    @Override
    public final InputStream getInputStream() throws IOException {
        checkOpen();
        waitForHandshake();
        return this.in;
    }

    @Override
    public final OutputStream getOutputStream() throws IOException {
        checkOpen();
        waitForHandshake();
        return this.out;
    }

    @Override
    public final SSLSession getHandshakeSession() {
        return this.engine.handshakeSession();
    }

    @Override
    public final SSLSession getSession() {
        SSLSession session = this.engine.getSession();
        if (SSLNullSession.isNullSession(session)) {
            boolean z = false;
            try {
                if (isConnected()) {
                    waitForHandshake();
                    z = true;
                }
            } catch (IOException e) {
            }
            if (!z) {
                return session;
            }
            return this.engine.getSession();
        }
        return session;
    }

    @Override
    final SSLSession getActiveSession() {
        return this.engine.getSession();
    }

    @Override
    public final boolean getEnableSessionCreation() {
        return this.engine.getEnableSessionCreation();
    }

    @Override
    public final void setEnableSessionCreation(boolean z) {
        this.engine.setEnableSessionCreation(z);
    }

    @Override
    public final String[] getSupportedCipherSuites() {
        return this.engine.getSupportedCipherSuites();
    }

    @Override
    public final String[] getEnabledCipherSuites() {
        return this.engine.getEnabledCipherSuites();
    }

    @Override
    public final void setEnabledCipherSuites(String[] strArr) {
        this.engine.setEnabledCipherSuites(strArr);
    }

    @Override
    public final String[] getSupportedProtocols() {
        return this.engine.getSupportedProtocols();
    }

    @Override
    public final String[] getEnabledProtocols() {
        return this.engine.getEnabledProtocols();
    }

    @Override
    public final void setEnabledProtocols(String[] strArr) {
        this.engine.setEnabledProtocols(strArr);
    }

    @Override
    public final void setHostname(String str) {
        this.engine.setHostname(str);
        super.setHostname(str);
    }

    @Override
    public final void setUseSessionTickets(boolean z) {
        this.engine.setUseSessionTickets(z);
    }

    @Override
    public final void setChannelIdEnabled(boolean z) {
        this.engine.setChannelIdEnabled(z);
    }

    @Override
    public final byte[] getChannelId() throws SSLException {
        return this.engine.getChannelId();
    }

    @Override
    public final void setChannelIdPrivateKey(PrivateKey privateKey) {
        this.engine.setChannelIdPrivateKey(privateKey);
    }

    @Override
    byte[] getTlsUnique() {
        return this.engine.getTlsUnique();
    }

    @Override
    public final boolean getUseClientMode() {
        return this.engine.getUseClientMode();
    }

    @Override
    public final void setUseClientMode(boolean z) {
        this.engine.setUseClientMode(z);
    }

    @Override
    public final boolean getWantClientAuth() {
        return this.engine.getWantClientAuth();
    }

    @Override
    public final boolean getNeedClientAuth() {
        return this.engine.getNeedClientAuth();
    }

    @Override
    public final void setNeedClientAuth(boolean z) {
        this.engine.setNeedClientAuth(z);
    }

    @Override
    public final void setWantClientAuth(boolean z) {
        this.engine.setWantClientAuth(z);
    }

    @Override
    public final void close() throws IOException {
        if (this.stateLock == null) {
            return;
        }
        synchronized (this.stateLock) {
            if (this.state == 8) {
                return;
            }
            this.state = 8;
            this.stateLock.notifyAll();
            super.close();
            this.engine.closeInbound();
            this.engine.closeOutbound();
        }
    }

    @Override
    final void setApplicationProtocols(String[] strArr) {
        this.engine.setApplicationProtocols(strArr);
    }

    @Override
    final String[] getApplicationProtocols() {
        return this.engine.getApplicationProtocols();
    }

    @Override
    public final String getApplicationProtocol() {
        return this.engine.getApplicationProtocol();
    }

    @Override
    public final String getHandshakeApplicationProtocol() {
        return this.engine.getHandshakeApplicationProtocol();
    }

    @Override
    public final void setApplicationProtocolSelector(ApplicationProtocolSelector applicationProtocolSelector) {
        setApplicationProtocolSelector(applicationProtocolSelector == null ? null : new ApplicationProtocolSelectorAdapter(this, applicationProtocolSelector));
    }

    @Override
    final void setApplicationProtocolSelector(ApplicationProtocolSelectorAdapter applicationProtocolSelectorAdapter) {
        this.engine.setApplicationProtocolSelector(applicationProtocolSelectorAdapter);
    }

    private void onHandshakeFinished() {
        boolean z;
        synchronized (this.stateLock) {
            if (this.state != 8) {
                if (this.state == 2) {
                    this.state = 4;
                } else if (this.state == 3) {
                    this.state = 5;
                }
                this.stateLock.notifyAll();
                z = true;
            } else {
                z = false;
            }
        }
        if (z) {
            notifyHandshakeCompletedListeners();
        }
    }

    private void waitForHandshake() throws IOException {
        startHandshake();
        synchronized (this.stateLock) {
            while (this.state != 5 && this.state != 4 && this.state != 8) {
                try {
                    this.stateLock.wait();
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

    private OutputStream getUnderlyingOutputStream() throws IOException {
        return super.getOutputStream();
    }

    private InputStream getUnderlyingInputStream() throws IOException {
        return super.getInputStream();
    }

    private final class SSLOutputStream extends OutputStream {
        private OutputStream socketOutputStream;
        private final ByteBuffer target;
        private final int targetArrayOffset;
        private final Object writeLock = new Object();

        SSLOutputStream() {
            this.target = ByteBuffer.allocate(ConscryptEngineSocket.this.engine.getSession().getPacketBufferSize());
            this.targetArrayOffset = this.target.arrayOffset();
        }

        @Override
        public void close() throws IOException {
            ConscryptEngineSocket.this.close();
        }

        @Override
        public void write(int i) throws IOException {
            ConscryptEngineSocket.this.startHandshake();
            synchronized (this.writeLock) {
                write(new byte[]{(byte) i});
            }
        }

        @Override
        public void write(byte[] bArr) throws IOException {
            ConscryptEngineSocket.this.startHandshake();
            synchronized (this.writeLock) {
                writeInternal(ByteBuffer.wrap(bArr));
            }
        }

        @Override
        public void write(byte[] bArr, int i, int i2) throws IOException {
            ConscryptEngineSocket.this.startHandshake();
            synchronized (this.writeLock) {
                writeInternal(ByteBuffer.wrap(bArr, i, i2));
            }
        }

        private void writeInternal(ByteBuffer byteBuffer) throws IOException {
            Platform.blockGuardOnNetwork();
            ConscryptEngineSocket.this.checkOpen();
            init();
            int iRemaining = byteBuffer.remaining();
            do {
                this.target.clear();
                SSLEngineResult sSLEngineResultWrap = ConscryptEngineSocket.this.engine.wrap(byteBuffer, this.target);
                if (sSLEngineResultWrap.getStatus() != SSLEngineResult.Status.OK) {
                    throw new SSLException("Unexpected engine result " + sSLEngineResultWrap.getStatus());
                }
                if (this.target.position() != sSLEngineResultWrap.bytesProduced()) {
                    throw new SSLException("Engine bytesProduced " + sSLEngineResultWrap.bytesProduced() + " does not match bytes written " + this.target.position());
                }
                iRemaining -= sSLEngineResultWrap.bytesConsumed();
                if (iRemaining != byteBuffer.remaining()) {
                    throw new SSLException("Engine did not read the correct number of bytes");
                }
                this.target.flip();
                writeToSocket();
            } while (iRemaining > 0);
        }

        @Override
        public void flush() throws IOException {
            ConscryptEngineSocket.this.startHandshake();
            synchronized (this.writeLock) {
                flushInternal();
            }
        }

        private void flushInternal() throws IOException {
            ConscryptEngineSocket.this.checkOpen();
            init();
            this.socketOutputStream.flush();
        }

        private void init() throws IOException {
            if (this.socketOutputStream == null) {
                this.socketOutputStream = ConscryptEngineSocket.this.getUnderlyingOutputStream();
            }
        }

        private void writeToSocket() throws IOException {
            this.socketOutputStream.write(this.target.array(), this.targetArrayOffset, this.target.limit());
        }
    }

    private final class SSLInputStream extends InputStream {
        private final ByteBuffer fromEngine;
        private final ByteBuffer fromSocket;
        private final int fromSocketArrayOffset;
        private final Object readLock = new Object();
        private final byte[] singleByte = new byte[1];
        private InputStream socketInputStream;

        SSLInputStream() {
            this.fromEngine = ByteBuffer.allocateDirect(ConscryptEngineSocket.this.engine.getSession().getApplicationBufferSize());
            this.fromEngine.flip();
            this.fromSocket = ByteBuffer.allocate(ConscryptEngineSocket.this.engine.getSession().getPacketBufferSize());
            this.fromSocketArrayOffset = this.fromSocket.arrayOffset();
        }

        @Override
        public void close() throws IOException {
            ConscryptEngineSocket.this.close();
        }

        @Override
        public int read() throws IOException {
            ConscryptEngineSocket.this.startHandshake();
            synchronized (this.readLock) {
                int i = read(this.singleByte, 0, 1);
                if (i == -1) {
                    return -1;
                }
                if (i != 1) {
                    throw new SSLException("read incorrect number of bytes " + i);
                }
                return this.singleByte[0];
            }
        }

        @Override
        public int read(byte[] bArr) throws IOException {
            int i;
            ConscryptEngineSocket.this.startHandshake();
            synchronized (this.readLock) {
                i = read(bArr, 0, bArr.length);
            }
            return i;
        }

        @Override
        public int read(byte[] bArr, int i, int i2) throws IOException {
            int internal;
            ConscryptEngineSocket.this.startHandshake();
            synchronized (this.readLock) {
                internal = readInternal(bArr, i, i2);
            }
            return internal;
        }

        @Override
        public int available() throws IOException {
            int iRemaining;
            ConscryptEngineSocket.this.startHandshake();
            synchronized (this.readLock) {
                init();
                iRemaining = this.fromEngine.remaining() + ((this.fromSocket.hasRemaining() || this.socketInputStream.available() > 0) ? 1 : 0);
            }
            return iRemaining;
        }

        private boolean isHandshaking(SSLEngineResult.HandshakeStatus handshakeStatus) {
            switch (AnonymousClass2.$SwitchMap$javax$net$ssl$SSLEngineResult$HandshakeStatus[handshakeStatus.ordinal()]) {
                case 1:
                case 2:
                case CTConstants.CERTIFICATE_LENGTH_BYTES:
                    return true;
                default:
                    return false;
            }
        }

        private int readInternal(byte[] bArr, int i, int i2) throws IOException {
            Platform.blockGuardOnNetwork();
            ConscryptEngineSocket.this.checkOpen();
            init();
            while (this.fromEngine.remaining() <= 0) {
                boolean z = true;
                this.fromSocket.flip();
                this.fromEngine.clear();
                boolean zIsHandshaking = isHandshaking(ConscryptEngineSocket.this.engine.getHandshakeStatus());
                SSLEngineResult sSLEngineResultUnwrap = ConscryptEngineSocket.this.engine.unwrap(this.fromSocket, this.fromEngine);
                this.fromSocket.compact();
                this.fromEngine.flip();
                switch (AnonymousClass2.$SwitchMap$javax$net$ssl$SSLEngineResult$Status[sSLEngineResultUnwrap.getStatus().ordinal()]) {
                    case 1:
                        if (sSLEngineResultUnwrap.bytesProduced() != 0) {
                            z = false;
                        }
                        if (z && sSLEngineResultUnwrap.bytesProduced() == 0) {
                            return 0;
                        }
                        if (!z && readFromSocket() == -1) {
                            return -1;
                        }
                        break;
                        break;
                    case 2:
                        if (!zIsHandshaking && isHandshaking(sSLEngineResultUnwrap.getHandshakeStatus()) && isHandshakeFinished()) {
                            renegotiate();
                            return 0;
                        }
                        z = false;
                        if (z) {
                        }
                        if (!z) {
                        }
                        break;
                    case CTConstants.CERTIFICATE_LENGTH_BYTES:
                        return -1;
                    default:
                        throw new SSLException("Unexpected engine result " + sSLEngineResultUnwrap.getStatus());
                }
            }
            int iMin = Math.min(this.fromEngine.remaining(), i2);
            this.fromEngine.get(bArr, i, iMin);
            return iMin;
        }

        private boolean isHandshakeFinished() {
            boolean z;
            synchronized (ConscryptEngineSocket.this.stateLock) {
                z = ConscryptEngineSocket.this.state >= 4;
            }
            return z;
        }

        private void renegotiate() throws IOException {
            synchronized (ConscryptEngineSocket.this.handshakeLock) {
                ConscryptEngineSocket.this.doHandshake();
            }
        }

        private void init() throws IOException {
            if (this.socketInputStream == null) {
                this.socketInputStream = ConscryptEngineSocket.this.getUnderlyingInputStream();
            }
        }

        private int readFromSocket() throws IOException {
            try {
                int iPosition = this.fromSocket.position();
                int i = this.socketInputStream.read(this.fromSocket.array(), this.fromSocketArrayOffset + iPosition, this.fromSocket.limit() - iPosition);
                if (i > 0) {
                    this.fromSocket.position(iPosition + i);
                }
                return i;
            } catch (EOFException e) {
                return -1;
            }
        }
    }

    static class AnonymousClass2 {
        static final int[] $SwitchMap$javax$net$ssl$SSLEngineResult$HandshakeStatus;
        static final int[] $SwitchMap$javax$net$ssl$SSLEngineResult$Status = new int[SSLEngineResult.Status.values().length];

        static {
            try {
                $SwitchMap$javax$net$ssl$SSLEngineResult$Status[SSLEngineResult.Status.BUFFER_UNDERFLOW.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$javax$net$ssl$SSLEngineResult$Status[SSLEngineResult.Status.OK.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$javax$net$ssl$SSLEngineResult$Status[SSLEngineResult.Status.CLOSED.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            $SwitchMap$javax$net$ssl$SSLEngineResult$HandshakeStatus = new int[SSLEngineResult.HandshakeStatus.values().length];
            try {
                $SwitchMap$javax$net$ssl$SSLEngineResult$HandshakeStatus[SSLEngineResult.HandshakeStatus.NEED_UNWRAP.ordinal()] = 1;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$javax$net$ssl$SSLEngineResult$HandshakeStatus[SSLEngineResult.HandshakeStatus.NEED_WRAP.ordinal()] = 2;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$javax$net$ssl$SSLEngineResult$HandshakeStatus[SSLEngineResult.HandshakeStatus.NEED_TASK.ordinal()] = 3;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$javax$net$ssl$SSLEngineResult$HandshakeStatus[SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING.ordinal()] = 4;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$javax$net$ssl$SSLEngineResult$HandshakeStatus[SSLEngineResult.HandshakeStatus.FINISHED.ordinal()] = 5;
            } catch (NoSuchFieldError e8) {
            }
        }
    }
}
