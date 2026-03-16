package javax.obex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class ClientSession extends ObexSession {
    private final InputStream mInput;
    private boolean mObexConnected;
    private final OutputStream mOutput;
    private byte[] mConnectionId = null;
    private int maxPacketSize = 256;
    private boolean mOpen = true;
    private boolean mRequestActive = false;

    public ClientSession(ObexTransport trans) throws IOException {
        this.mInput = trans.openInputStream();
        this.mOutput = trans.openOutputStream();
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
            totalLength = 4 + head.length;
        }
        byte[] requestPacket = new byte[totalLength];
        requestPacket[0] = 16;
        requestPacket[1] = 0;
        requestPacket[2] = -1;
        requestPacket[3] = -2;
        if (head != null) {
            System.arraycopy(head, 0, requestPacket, 4, head.length);
        }
        if (requestPacket.length + 3 > 65534) {
            throw new IOException("Packet size exceeds max packet size");
        }
        HeaderSet returnHeaderSet = new HeaderSet();
        sendRequest(ObexHelper.OBEX_OPCODE_CONNECT, requestPacket, returnHeaderSet, null);
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
            if (head.nonce != null) {
                this.mChallengeDigest = new byte[16];
                System.arraycopy(head.nonce, 0, this.mChallengeDigest, 0, 16);
            }
        }
        if (this.mConnectionId != null) {
            head.mConnectionID = new byte[4];
            System.arraycopy(this.mConnectionId, 0, head.mConnectionID, 0, 4);
        }
        return new ClientOperation(this.maxPacketSize, this, head, true);
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
            if (head.length + 3 > this.maxPacketSize) {
                throw new IOException("Packet size exceeds max packet size");
            }
        } else if (this.mConnectionId != null) {
            head = new byte[5];
            head[0] = -53;
            System.arraycopy(this.mConnectionId, 0, head, 1, 4);
        }
        HeaderSet returnHeaderSet = new HeaderSet();
        sendRequest(ObexHelper.OBEX_OPCODE_DISCONNECT, head, returnHeaderSet, null);
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
            if (head.nonce != null) {
                this.mChallengeDigest = new byte[16];
                System.arraycopy(head.nonce, 0, this.mChallengeDigest, 0, 16);
            }
        }
        if (this.mConnectionId != null) {
            head.mConnectionID = new byte[4];
            System.arraycopy(this.mConnectionId, 0, head.mConnectionID, 0, 4);
        }
        return new ClientOperation(this.maxPacketSize, this, head, false);
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
            if (headset.nonce != null) {
                this.mChallengeDigest = new byte[16];
                System.arraycopy(headset.nonce, 0, this.mChallengeDigest, 0, 16);
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
        int totalLength = 2 + head.length;
        if (totalLength > this.maxPacketSize) {
            throw new IOException("Packet size exceeds max packet size");
        }
        int flags = 0;
        if (backup) {
            flags = 0 + 1;
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
        sendRequest(ObexHelper.OBEX_OPCODE_SETPATH, packet, returnHeaderSet, null);
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

    public boolean sendRequest(int opCode, byte[] head, HeaderSet header, PrivateInputStream privateInput) throws IOException {
        byte[] data;
        if (head != null && head.length + 3 > 65534) {
            throw new IOException("header too large ");
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
        this.mOutput.write(out.toByteArray());
        this.mOutput.flush();
        header.responseCode = this.mInput.read();
        int length = (this.mInput.read() << 8) | this.mInput.read();
        if (length > 65534) {
            throw new IOException("Packet received exceeds packet size limit");
        }
        if (length > 3) {
            if (opCode == 128) {
                this.mInput.read();
                this.mInput.read();
                this.maxPacketSize = (this.mInput.read() << 8) + this.mInput.read();
                if (this.maxPacketSize > 64512) {
                    this.maxPacketSize = ObexHelper.MAX_CLIENT_PACKET_SIZE;
                }
                if (length > 7) {
                    data = new byte[length - 7];
                    int bytesReceived = this.mInput.read(data);
                    while (bytesReceived != length - 7) {
                        bytesReceived += this.mInput.read(data, bytesReceived, data.length - bytesReceived);
                    }
                } else {
                    return true;
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
            if (header.responseCode == 193 && header.mAuthChall != null && handleAuthChall(header)) {
                out.write(78);
                out.write((byte) ((header.mAuthResp.length + 3) >> 8));
                out.write((byte) (header.mAuthResp.length + 3));
                out.write(header.mAuthResp);
                header.mAuthChall = null;
                header.mAuthResp = null;
                byte[] sendHeaders = new byte[out.size() - 3];
                System.arraycopy(out.toByteArray(), 3, sendHeaders, 0, sendHeaders.length);
                return sendRequest(opCode, sendHeaders, header, privateInput);
            }
        }
        return true;
    }

    public void close() throws IOException {
        this.mOpen = false;
        this.mInput.close();
        this.mOutput.close();
    }
}
