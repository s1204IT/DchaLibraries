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

    public ClientSession(ObexTransport trans) throws IOException {
        this.mInput = trans.openInputStream();
        this.mOutput = trans.openOutputStream();
        this.mLocalSrmSupported = trans.isSrmSupported();
        this.mTransport = trans;
    }

    public ClientSession(ObexTransport trans, boolean supportsSrm) throws IOException {
        this.mInput = trans.openInputStream();
        this.mOutput = trans.openOutputStream();
        this.mLocalSrmSupported = supportsSrm;
        this.mTransport = trans;
    }

    public HeaderSet connect(HeaderSet header) throws Throwable {
        ensureOpen();
        if (this.mObexConnected) {
            throw new IOException("Already connected to server");
        }
        setRequestActive();
        int totalLength = 4;
        byte[] head = null;
        if (header != null) {
            if (header.nonce != null) {
                this.mChallengeDigest = new byte[16];
                System.arraycopy(header.nonce, 0, this.mChallengeDigest, 0, 16);
            }
            head = ObexHelper.createHeader(header, false);
            totalLength = head.length + 4;
        }
        byte[] requestPacket = new byte[totalLength];
        int maxRxPacketSize = ObexHelper.getMaxRxPacketSize(this.mTransport);
        requestPacket[0] = 16;
        requestPacket[1] = 0;
        requestPacket[2] = (byte) (maxRxPacketSize >> 8);
        requestPacket[3] = (byte) (maxRxPacketSize & 255);
        if (head != null) {
            System.arraycopy(head, 0, requestPacket, 4, head.length);
        }
        if (requestPacket.length + 3 > 65534) {
            throw new IOException("Packet size exceeds max packet size for connect");
        }
        HeaderSet returnHeaderSet = new HeaderSet();
        sendRequest(128, requestPacket, returnHeaderSet, null, false);
        if (returnHeaderSet.responseCode == 160) {
            this.mObexConnected = true;
        }
        setRequestInactive();
        return returnHeaderSet;
    }

    public Operation get(HeaderSet header) throws IOException {
        HeaderSet head;
        if (!this.mObexConnected) {
            throw new IOException("Not connected to the server");
        }
        setRequestActive();
        ensureOpen();
        if (header == null) {
            head = new HeaderSet();
        } else {
            head = header;
            if (header.nonce != null) {
                this.mChallengeDigest = new byte[16];
                System.arraycopy(header.nonce, 0, this.mChallengeDigest, 0, 16);
            }
        }
        if (this.mConnectionId != null) {
            head.mConnectionID = new byte[4];
            System.arraycopy(this.mConnectionId, 0, head.mConnectionID, 0, 4);
        }
        if (this.mLocalSrmSupported) {
            head.setHeader(HeaderSet.SINGLE_RESPONSE_MODE, (byte) 1);
        }
        return new ClientOperation(this.mMaxTxPacketSize, this, head, true);
    }

    public void setConnectionID(long id) {
        if (id < 0 || id > 4294967295L) {
            throw new IllegalArgumentException("Connection ID is not in a valid range");
        }
        this.mConnectionId = ObexHelper.convertToByteArray(id);
    }

    public HeaderSet delete(HeaderSet header) throws IOException {
        Operation op = put(header);
        op.getResponseCode();
        HeaderSet returnValue = op.getReceivedHeader();
        op.close();
        return returnValue;
    }

    public HeaderSet disconnect(HeaderSet header) throws Throwable {
        if (!this.mObexConnected) {
            throw new IOException("Not connected to the server");
        }
        setRequestActive();
        ensureOpen();
        byte[] head = null;
        if (header != null) {
            if (header.nonce != null) {
                this.mChallengeDigest = new byte[16];
                System.arraycopy(header.nonce, 0, this.mChallengeDigest, 0, 16);
            }
            if (this.mConnectionId != null) {
                header.mConnectionID = new byte[4];
                System.arraycopy(this.mConnectionId, 0, header.mConnectionID, 0, 4);
            }
            head = ObexHelper.createHeader(header, false);
            if (head.length + 3 > this.mMaxTxPacketSize) {
                throw new IOException("Packet size exceeds max packet size");
            }
        } else if (this.mConnectionId != null) {
            head = new byte[5];
            head[0] = -53;
            System.arraycopy(this.mConnectionId, 0, head, 1, 4);
        }
        HeaderSet returnHeaderSet = new HeaderSet();
        sendRequest(ObexHelper.OBEX_OPCODE_DISCONNECT, head, returnHeaderSet, null, false);
        synchronized (this) {
            this.mObexConnected = false;
            setRequestInactive();
        }
        return returnHeaderSet;
    }

    public long getConnectionID() {
        if (this.mConnectionId == null) {
            return -1L;
        }
        return ObexHelper.convertToLong(this.mConnectionId);
    }

    public Operation put(HeaderSet header) throws IOException {
        HeaderSet head;
        if (!this.mObexConnected) {
            throw new IOException("Not connected to the server");
        }
        setRequestActive();
        ensureOpen();
        if (header == null) {
            head = new HeaderSet();
        } else {
            head = header;
            if (header.nonce != null) {
                this.mChallengeDigest = new byte[16];
                System.arraycopy(header.nonce, 0, this.mChallengeDigest, 0, 16);
            }
        }
        if (this.mConnectionId != null) {
            head.mConnectionID = new byte[4];
            System.arraycopy(this.mConnectionId, 0, head.mConnectionID, 0, 4);
        }
        if (this.mLocalSrmSupported) {
            head.setHeader(HeaderSet.SINGLE_RESPONSE_MODE, (byte) 1);
        }
        return new ClientOperation(this.mMaxTxPacketSize, this, head, false);
    }

    public void setAuthenticator(Authenticator auth) throws IOException {
        if (auth == null) {
            throw new IOException("Authenticator may not be null");
        }
        this.mAuthenticator = auth;
    }

    public HeaderSet setPath(HeaderSet header, boolean backup, boolean create) throws Throwable {
        HeaderSet headset;
        if (!this.mObexConnected) {
            throw new IOException("Not connected to the server");
        }
        setRequestActive();
        ensureOpen();
        if (header == null) {
            headset = new HeaderSet();
        } else {
            headset = header;
            if (header.nonce != null) {
                this.mChallengeDigest = new byte[16];
                System.arraycopy(header.nonce, 0, this.mChallengeDigest, 0, 16);
            }
        }
        if (headset.nonce != null) {
            this.mChallengeDigest = new byte[16];
            System.arraycopy(headset.nonce, 0, this.mChallengeDigest, 0, 16);
        }
        if (this.mConnectionId != null) {
            headset.mConnectionID = new byte[4];
            System.arraycopy(this.mConnectionId, 0, headset.mConnectionID, 0, 4);
        }
        byte[] head = ObexHelper.createHeader(headset, false);
        int totalLength = head.length + 2;
        if (totalLength > this.mMaxTxPacketSize) {
            throw new IOException("Packet size exceeds max packet size");
        }
        int flags = 0;
        if (backup) {
            flags = 1;
        }
        if (!create) {
            flags |= 2;
        }
        byte[] packet = new byte[totalLength];
        packet[0] = (byte) flags;
        packet[1] = 0;
        if (headset != null) {
            System.arraycopy(head, 0, packet, 2, head.length);
        }
        HeaderSet returnHeaderSet = new HeaderSet();
        sendRequest(ObexHelper.OBEX_OPCODE_SETPATH, packet, returnHeaderSet, null, false);
        setRequestInactive();
        return returnHeaderSet;
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

    public boolean sendRequest(int opCode, byte[] head, HeaderSet header, PrivateInputStream privateInput, boolean srmActive) throws IOException {
        byte[] data;
        if (head != null && head.length + 3 > 65534) {
            throw new IOException("header too large ");
        }
        boolean skipSend = false;
        boolean skipReceive = false;
        if (srmActive) {
            if (opCode == 2 || opCode == 3) {
                skipReceive = true;
            } else if (opCode == 131) {
                skipSend = true;
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write((byte) opCode);
        if (head == null) {
            out.write(0);
            out.write(3);
        } else {
            out.write((byte) ((head.length + 3) >> 8));
            out.write((byte) (head.length + 3));
            out.write(head);
        }
        if (V) {
            Log.d(TAG, "start to write socket");
        }
        if (!skipSend) {
            this.mOutput.write(out.toByteArray());
            this.mOutput.flush();
        }
        if (V) {
            Log.d(TAG, "end of writing socket");
        }
        if (skipReceive) {
            return true;
        }
        if (V) {
            Log.d(TAG, "start to read input");
        }
        header.responseCode = this.mInput.read();
        if (V) {
            Log.d(TAG, "end to reading input. response code = " + header.responseCode);
        }
        int length = (this.mInput.read() << 8) | this.mInput.read();
        if (length > ObexHelper.getMaxRxPacketSize(this.mTransport)) {
            throw new IOException("Packet received exceeds packet size limit");
        }
        if (length <= 3) {
            return true;
        }
        if (opCode == 128) {
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
            if (length <= 7) {
                return true;
            }
            data = new byte[length - 7];
            int bytesReceived = this.mInput.read(data);
            while (bytesReceived != length - 7) {
                bytesReceived += this.mInput.read(data, bytesReceived, data.length - bytesReceived);
            }
        } else {
            data = new byte[length - 3];
            int bytesReceived2 = this.mInput.read(data);
            while (bytesReceived2 != length - 3) {
                bytesReceived2 += this.mInput.read(data, bytesReceived2, data.length - bytesReceived2);
            }
            if (opCode == 255) {
                return true;
            }
        }
        byte[] body = ObexHelper.updateHeaderSet(header, data);
        if (privateInput != null && body != null) {
            privateInput.writeBytes(body, 1);
        }
        if (header.mConnectionID != null) {
            this.mConnectionId = new byte[4];
            System.arraycopy(header.mConnectionID, 0, this.mConnectionId, 0, 4);
        }
        if (header.mAuthResp != null && !handleAuthResp(header.mAuthResp)) {
            setRequestInactive();
            throw new IOException("Authentication Failed");
        }
        if (header.responseCode != 193 || header.mAuthChall == null || !handleAuthChall(header)) {
            return true;
        }
        out.write(78);
        out.write((byte) ((header.mAuthResp.length + 3) >> 8));
        out.write((byte) (header.mAuthResp.length + 3));
        out.write(header.mAuthResp);
        header.mAuthChall = null;
        header.mAuthResp = null;
        byte[] sendHeaders = new byte[out.size() - 3];
        System.arraycopy(out.toByteArray(), 3, sendHeaders, 0, sendHeaders.length);
        return sendRequest(opCode, sendHeaders, header, privateInput, false);
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
