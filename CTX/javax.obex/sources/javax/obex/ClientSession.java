package javax.obex;

import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class ClientSession extends ObexSession {
    private static final String TAG = "ClientSession";
    private static final boolean V = ObexHelper.VDBG;
    private final InputStream mInput;
    private final boolean mLocalSrmSupported;
    private boolean mObexConnected;
    private final OutputStream mOutput;
    private final ObexTransport mTransport;
    private byte[] mConnectionId = null;
    private int mMaxTxPacketSize = 255;
    private boolean mOpen = true;
    private boolean mRequestActive = false;

    public ClientSession(ObexTransport obexTransport) throws IOException {
        this.mInput = obexTransport.openInputStream();
        this.mOutput = obexTransport.openOutputStream();
        this.mLocalSrmSupported = obexTransport.isSrmSupported();
        this.mTransport = obexTransport;
    }

    public ClientSession(ObexTransport obexTransport, boolean z) throws IOException {
        this.mInput = obexTransport.openInputStream();
        this.mOutput = obexTransport.openOutputStream();
        this.mLocalSrmSupported = z;
        this.mTransport = obexTransport;
    }

    public HeaderSet connect(HeaderSet headerSet) throws IOException {
        int length;
        ensureOpen();
        if (this.mObexConnected) {
            throw new IOException("Already connected to server");
        }
        setRequestActive();
        byte[] bArrCreateHeader = null;
        if (headerSet != null) {
            if (headerSet.nonce != null) {
                this.mChallengeDigest = new byte[16];
                System.arraycopy(headerSet.nonce, 0, this.mChallengeDigest, 0, 16);
            }
            bArrCreateHeader = ObexHelper.createHeader(headerSet, false);
            length = bArrCreateHeader.length + 4;
        } else {
            length = 4;
        }
        byte[] bArr = new byte[length];
        int maxRxPacketSize = ObexHelper.getMaxRxPacketSize(this.mTransport);
        bArr[0] = 16;
        bArr[1] = 0;
        bArr[2] = (byte) (maxRxPacketSize >> 8);
        bArr[3] = (byte) (maxRxPacketSize & 255);
        if (bArrCreateHeader != null) {
            System.arraycopy(bArrCreateHeader, 0, bArr, 4, bArrCreateHeader.length);
        }
        if (bArr.length + 3 > 65534) {
            throw new IOException("Packet size exceeds max packet size for connect");
        }
        HeaderSet headerSet2 = new HeaderSet();
        sendRequest(128, bArr, headerSet2, null, false);
        if (headerSet2.responseCode == 160) {
            this.mObexConnected = true;
        }
        setRequestInactive();
        return headerSet2;
    }

    public Operation get(HeaderSet headerSet) throws IOException {
        if (!this.mObexConnected) {
            throw new IOException("Not connected to the server");
        }
        setRequestActive();
        ensureOpen();
        if (headerSet == null) {
            headerSet = new HeaderSet();
        } else if (headerSet.nonce != null) {
            this.mChallengeDigest = new byte[16];
            System.arraycopy(headerSet.nonce, 0, this.mChallengeDigest, 0, 16);
        }
        if (this.mConnectionId != null) {
            headerSet.mConnectionID = new byte[4];
            System.arraycopy(this.mConnectionId, 0, headerSet.mConnectionID, 0, 4);
        }
        if (this.mLocalSrmSupported) {
            headerSet.setHeader(HeaderSet.SINGLE_RESPONSE_MODE, (byte) 1);
        }
        return new ClientOperation(this.mMaxTxPacketSize, this, headerSet, true);
    }

    public void setConnectionID(long j) {
        if (j < 0 || j > 4294967295L) {
            throw new IllegalArgumentException("Connection ID is not in a valid range");
        }
        this.mConnectionId = ObexHelper.convertToByteArray(j);
    }

    public HeaderSet delete(HeaderSet headerSet) throws IOException {
        Operation operationPut = put(headerSet);
        operationPut.getResponseCode();
        HeaderSet receivedHeader = operationPut.getReceivedHeader();
        operationPut.close();
        return receivedHeader;
    }

    public HeaderSet disconnect(HeaderSet headerSet) throws IOException {
        if (!this.mObexConnected) {
            throw new IOException("Not connected to the server");
        }
        setRequestActive();
        ensureOpen();
        byte[] bArrCreateHeader = null;
        if (headerSet != null) {
            if (headerSet.nonce != null) {
                this.mChallengeDigest = new byte[16];
                System.arraycopy(headerSet.nonce, 0, this.mChallengeDigest, 0, 16);
            }
            if (this.mConnectionId != null) {
                headerSet.mConnectionID = new byte[4];
                System.arraycopy(this.mConnectionId, 0, headerSet.mConnectionID, 0, 4);
            }
            bArrCreateHeader = ObexHelper.createHeader(headerSet, false);
            if (bArrCreateHeader.length + 3 > this.mMaxTxPacketSize) {
                throw new IOException("Packet size exceeds max packet size");
            }
        } else if (this.mConnectionId != null) {
            bArrCreateHeader = new byte[5];
            bArrCreateHeader[0] = -53;
            System.arraycopy(this.mConnectionId, 0, bArrCreateHeader, 1, 4);
        }
        HeaderSet headerSet2 = new HeaderSet();
        sendRequest(ObexHelper.OBEX_OPCODE_DISCONNECT, bArrCreateHeader, headerSet2, null, false);
        synchronized (this) {
            this.mObexConnected = false;
            setRequestInactive();
        }
        return headerSet2;
    }

    public long getConnectionID() {
        if (this.mConnectionId == null) {
            return -1L;
        }
        return ObexHelper.convertToLong(this.mConnectionId);
    }

    public Operation put(HeaderSet headerSet) throws IOException {
        if (!this.mObexConnected) {
            throw new IOException("Not connected to the server");
        }
        setRequestActive();
        ensureOpen();
        if (headerSet == null) {
            headerSet = new HeaderSet();
        } else if (headerSet.nonce != null) {
            this.mChallengeDigest = new byte[16];
            System.arraycopy(headerSet.nonce, 0, this.mChallengeDigest, 0, 16);
        }
        if (this.mConnectionId != null) {
            headerSet.mConnectionID = new byte[4];
            System.arraycopy(this.mConnectionId, 0, headerSet.mConnectionID, 0, 4);
        }
        if (this.mLocalSrmSupported) {
            headerSet.setHeader(HeaderSet.SINGLE_RESPONSE_MODE, (byte) 1);
        }
        return new ClientOperation(this.mMaxTxPacketSize, this, headerSet, false);
    }

    public void setAuthenticator(Authenticator authenticator) throws IOException {
        if (authenticator == null) {
            throw new IOException("Authenticator may not be null");
        }
        this.mAuthenticator = authenticator;
    }

    public HeaderSet setPath(HeaderSet headerSet, boolean z, boolean z2) throws IOException {
        if (!this.mObexConnected) {
            throw new IOException("Not connected to the server");
        }
        setRequestActive();
        ensureOpen();
        if (headerSet == null) {
            headerSet = new HeaderSet();
        } else if (headerSet.nonce != null) {
            this.mChallengeDigest = new byte[16];
            System.arraycopy(headerSet.nonce, 0, this.mChallengeDigest, 0, 16);
        }
        if (headerSet.nonce != null) {
            this.mChallengeDigest = new byte[16];
            System.arraycopy(headerSet.nonce, 0, this.mChallengeDigest, 0, 16);
        }
        if (this.mConnectionId != null) {
            headerSet.mConnectionID = new byte[4];
            System.arraycopy(this.mConnectionId, 0, headerSet.mConnectionID, 0, 4);
        }
        byte[] bArrCreateHeader = ObexHelper.createHeader(headerSet, false);
        int length = bArrCreateHeader.length + 2;
        if (length > this.mMaxTxPacketSize) {
            throw new IOException("Packet size exceeds max packet size");
        }
        int i = z ? 1 : 0;
        if (!z2) {
            i |= 2;
        }
        byte[] bArr = new byte[length];
        bArr[0] = (byte) i;
        bArr[1] = 0;
        if (headerSet != null) {
            System.arraycopy(bArrCreateHeader, 0, bArr, 2, bArrCreateHeader.length);
        }
        HeaderSet headerSet2 = new HeaderSet();
        sendRequest(ObexHelper.OBEX_OPCODE_SETPATH, bArr, headerSet2, null, false);
        setRequestInactive();
        return headerSet2;
    }

    public synchronized void ensureOpen() throws IOException {
        if (!this.mOpen) {
            throw new IOException("Connection closed");
        }
    }

    synchronized void setRequestInactive() {
        this.mRequestActive = false;
    }

    private synchronized void setRequestActive() throws IOException {
        if (this.mRequestActive) {
            throw new IOException("OBEX request is already being performed");
        }
        this.mRequestActive = true;
    }

    public boolean sendRequest(int i, byte[] bArr, HeaderSet headerSet, PrivateInputStream privateInputStream, boolean z) throws IOException {
        boolean z2;
        boolean z3;
        byte[] bArr2;
        if (V) {
            Log.d(TAG, "session send request opcode = " + i);
        }
        if (bArr != null && bArr.length + 3 > 65534) {
            throw new IOException("header too large ");
        }
        if (!z) {
            z2 = false;
            z3 = false;
        } else if (i != 2 && i != 3) {
            if (i == 131) {
                z3 = false;
                z2 = true;
            }
        } else {
            z2 = false;
            z3 = true;
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write((byte) i);
        if (bArr != null) {
            byteArrayOutputStream.write((byte) ((bArr.length + 3) >> 8));
            byteArrayOutputStream.write((byte) (bArr.length + 3));
            byteArrayOutputStream.write(bArr);
        } else {
            byteArrayOutputStream.write(0);
            byteArrayOutputStream.write(3);
        }
        if (V) {
            Log.d(TAG, "start to write socket length = " + byteArrayOutputStream.toByteArray().length);
        }
        if (!z2) {
            this.mOutput.write(byteArrayOutputStream.toByteArray());
            this.mOutput.flush();
        }
        if (V) {
            Log.d(TAG, "end of writing socket");
        }
        if (!z3) {
            if (V) {
                Log.d(TAG, "start to read input");
            }
            headerSet.responseCode = this.mInput.read();
            if (V) {
                Log.d(TAG, "end to reading input. response code = " + headerSet.responseCode);
            }
            int i2 = (this.mInput.read() << 8) | this.mInput.read();
            if (i2 > ObexHelper.getMaxRxPacketSize(this.mTransport)) {
                throw new IOException("Packet received exceeds packet size limit");
            }
            if (i2 > 3) {
                if (i == 128) {
                    this.mInput.read();
                    this.mInput.read();
                    this.mMaxTxPacketSize = (this.mInput.read() << 8) + this.mInput.read();
                    if (this.mMaxTxPacketSize > 64512) {
                        this.mMaxTxPacketSize = ObexHelper.MAX_CLIENT_PACKET_SIZE;
                    }
                    if (this.mMaxTxPacketSize > ObexHelper.getMaxTxPacketSize(this.mTransport)) {
                        Log.w(TAG, "An OBEX packet size of " + this.mMaxTxPacketSize + "was requested. Transport only allows: " + ObexHelper.getMaxTxPacketSize(this.mTransport) + " Lowering limit to this value.");
                        this.mMaxTxPacketSize = ObexHelper.getMaxTxPacketSize(this.mTransport);
                    }
                    if (i2 <= 7) {
                        return true;
                    }
                    int i3 = i2 - 7;
                    bArr2 = new byte[i3];
                    int i4 = this.mInput.read(bArr2);
                    while (i4 != i3) {
                        i4 += this.mInput.read(bArr2, i4, bArr2.length - i4);
                    }
                } else {
                    int i5 = i2 - 3;
                    bArr2 = new byte[i5];
                    int i6 = this.mInput.read(bArr2);
                    while (i6 != i5) {
                        i6 += this.mInput.read(bArr2, i6, bArr2.length - i6);
                    }
                    if (i == 255) {
                        return true;
                    }
                }
                byte[] bArrUpdateHeaderSet = ObexHelper.updateHeaderSet(headerSet, bArr2);
                if (privateInputStream != null && bArrUpdateHeaderSet != null) {
                    privateInputStream.writeBytes(bArrUpdateHeaderSet, 1);
                }
                if (headerSet.mConnectionID != null) {
                    this.mConnectionId = new byte[4];
                    System.arraycopy(headerSet.mConnectionID, 0, this.mConnectionId, 0, 4);
                }
                if (headerSet.mAuthResp != null && !handleAuthResp(headerSet.mAuthResp)) {
                    setRequestInactive();
                    throw new IOException("Authentication Failed");
                }
                if (headerSet.responseCode == 193 && headerSet.mAuthChall != null && handleAuthChall(headerSet)) {
                    byteArrayOutputStream.write(78);
                    byteArrayOutputStream.write((byte) ((headerSet.mAuthResp.length + 3) >> 8));
                    byteArrayOutputStream.write((byte) (headerSet.mAuthResp.length + 3));
                    byteArrayOutputStream.write(headerSet.mAuthResp);
                    headerSet.mAuthChall = null;
                    headerSet.mAuthResp = null;
                    byte[] bArr3 = new byte[byteArrayOutputStream.size() - 3];
                    System.arraycopy(byteArrayOutputStream.toByteArray(), 3, bArr3, 0, bArr3.length);
                    return sendRequest(i, bArr3, headerSet, privateInputStream, false);
                }
            }
        }
        return true;
    }

    public void close() throws IOException {
        this.mOpen = false;
        this.mInput.close();
        this.mOutput.close();
    }

    public boolean isSrmSupported() {
        return this.mLocalSrmSupported;
    }
}
