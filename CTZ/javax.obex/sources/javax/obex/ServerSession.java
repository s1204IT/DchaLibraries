package javax.obex;

import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class ServerSession extends ObexSession implements Runnable {
    private static final String TAG = "Obex ServerSession";
    private static final boolean V = ObexHelper.VDBG;
    private boolean mClosed;
    private InputStream mInput;
    private ServerRequestHandler mListener;
    private int mMaxPacketLength;
    private OutputStream mOutput;
    private Thread mProcessThread;
    private ObexTransport mTransport;

    public ServerSession(ObexTransport obexTransport, ServerRequestHandler serverRequestHandler, Authenticator authenticator) throws IOException {
        this.mAuthenticator = authenticator;
        this.mTransport = obexTransport;
        this.mInput = this.mTransport.openInputStream();
        this.mOutput = this.mTransport.openOutputStream();
        this.mListener = serverRequestHandler;
        this.mMaxPacketLength = 256;
        this.mClosed = false;
        this.mProcessThread = new Thread(this);
        this.mProcessThread.start();
    }

    @Override
    public void run() {
        boolean z = false;
        while (!z) {
            try {
            } catch (NullPointerException e) {
                Log.d(TAG, "Exception occured - ignoring", e);
            } catch (Exception e2) {
                Log.d(TAG, "Exception occured - ignoring", e2);
            }
            if (!this.mClosed) {
                if (V) {
                    Log.v(TAG, "Waiting for incoming request...");
                }
                int i = this.mInput.read();
                if (V) {
                    Log.v(TAG, "Read request: " + i);
                }
                if (i == -1) {
                    z = true;
                } else if (i == 133) {
                    handleSetPathRequest();
                } else if (i != 255) {
                    switch (i) {
                        case 2:
                            handlePutRequest(i);
                            break;
                        case 3:
                            handleGetRequest(i);
                            break;
                        default:
                            switch (i) {
                                case 128:
                                    handleConnectRequest();
                                    break;
                                case ObexHelper.OBEX_OPCODE_DISCONNECT:
                                    handleDisconnectRequest();
                                    break;
                                case ObexHelper.OBEX_OPCODE_PUT_FINAL:
                                    handlePutRequest(i);
                                    break;
                                case ObexHelper.OBEX_OPCODE_GET_FINAL:
                                    handleGetRequest(i);
                                    break;
                                default:
                                    int i2 = (this.mInput.read() << 8) + this.mInput.read();
                                    for (int i3 = 3; i3 < i2; i3++) {
                                        this.mInput.read();
                                    }
                                    sendResponse(ResponseCodes.OBEX_HTTP_NOT_IMPLEMENTED, null);
                                    break;
                            }
                            break;
                    }
                } else {
                    handleAbortRequest();
                }
            } else {
                close();
            }
        }
        close();
    }

    private void handleAbortRequest() throws IOException {
        int iValidateResponseCode;
        HeaderSet headerSet = new HeaderSet();
        HeaderSet headerSet2 = new HeaderSet();
        int i = (this.mInput.read() << 8) + this.mInput.read();
        if (i > ObexHelper.getMaxRxPacketSize(this.mTransport)) {
            iValidateResponseCode = ResponseCodes.OBEX_HTTP_REQ_TOO_LARGE;
        } else {
            for (int i2 = 3; i2 < i; i2++) {
                this.mInput.read();
            }
            int iOnAbort = this.mListener.onAbort(headerSet, headerSet2);
            Log.v(TAG, "onAbort request handler return value- " + iOnAbort);
            iValidateResponseCode = validateResponseCode(iOnAbort);
        }
        sendResponse(iValidateResponseCode, null);
    }

    private void handlePutRequest(int i) throws IOException {
        int iValidateResponseCode;
        ServerOperation serverOperation = new ServerOperation(this, this.mInput, i, this.mMaxPacketLength, this.mListener);
        try {
            if (serverOperation.finalBitSet && !serverOperation.isValidBody()) {
                iValidateResponseCode = validateResponseCode(this.mListener.onDelete(serverOperation.requestHeader, serverOperation.replyHeader));
            } else {
                iValidateResponseCode = validateResponseCode(this.mListener.onPut(serverOperation));
            }
            if (V) {
                Log.d(TAG, "handlePutRequest, response = " + iValidateResponseCode);
            }
            if (iValidateResponseCode != 160 && !serverOperation.isAborted) {
                serverOperation.sendReply(iValidateResponseCode);
            } else if (!serverOperation.isAborted) {
                while (!serverOperation.finalBitSet) {
                    serverOperation.sendReply(ResponseCodes.OBEX_HTTP_CONTINUE);
                }
                serverOperation.sendReply(iValidateResponseCode);
            }
        } catch (Exception e) {
            if (V) {
                Log.d(TAG, "Exception occured - sending OBEX_HTTP_INTERNAL_ERROR reply", e);
            }
            if (!serverOperation.isAborted) {
                sendResponse(ResponseCodes.OBEX_HTTP_INTERNAL_ERROR, null);
            }
        }
    }

    private void handleGetRequest(int i) throws IOException {
        ServerOperation serverOperation = new ServerOperation(this, this.mInput, i, this.mMaxPacketLength, this.mListener);
        try {
            int iValidateResponseCode = validateResponseCode(this.mListener.onGet(serverOperation));
            if (!serverOperation.isAborted) {
                serverOperation.sendReply(iValidateResponseCode);
            }
        } catch (Exception e) {
            if (V) {
                Log.d(TAG, "Exception occured - sending OBEX_HTTP_INTERNAL_ERROR reply", e);
            }
            sendResponse(ResponseCodes.OBEX_HTTP_INTERNAL_ERROR, null);
        }
    }

    public void sendResponse(int i, byte[] bArr) throws IOException {
        byte[] bArr2;
        OutputStream outputStream = this.mOutput;
        if (outputStream == null) {
            return;
        }
        if (bArr != null) {
            int length = bArr.length + 3;
            bArr2 = new byte[length];
            bArr2[0] = (byte) i;
            bArr2[1] = (byte) (length >> 8);
            bArr2[2] = (byte) length;
            System.arraycopy(bArr, 0, bArr2, 3, bArr.length);
        } else {
            bArr2 = new byte[]{(byte) i, 0, (byte) 3};
        }
        outputStream.write(bArr2);
        outputStream.flush();
    }

    private void handleSetPathRequest() throws IOException {
        int iValidateResponseCode;
        int length;
        int i;
        HeaderSet headerSet = new HeaderSet();
        HeaderSet headerSet2 = new HeaderSet();
        int i2 = (this.mInput.read() << 8) + this.mInput.read();
        int i3 = this.mInput.read();
        this.mInput.read();
        byte[] bArr = null;
        if (i2 > ObexHelper.getMaxRxPacketSize(this.mTransport)) {
            iValidateResponseCode = ResponseCodes.OBEX_HTTP_REQ_TOO_LARGE;
        } else {
            if (i2 > 5) {
                byte[] bArr2 = new byte[i2 - 5];
                int i4 = this.mInput.read(bArr2);
                while (i4 != bArr2.length) {
                    i4 += this.mInput.read(bArr2, i4, bArr2.length - i4);
                }
                ObexHelper.updateHeaderSet(headerSet, bArr2);
                if (this.mListener.getConnectionId() != -1 && headerSet.mConnectionID != null) {
                    this.mListener.setConnectionId(ObexHelper.convertToLong(headerSet.mConnectionID));
                } else {
                    this.mListener.setConnectionId(1L);
                }
                if (headerSet.mAuthResp != null) {
                    if (handleAuthResp(headerSet.mAuthResp)) {
                        i = -1;
                    } else {
                        this.mListener.onAuthenticationFailure(ObexHelper.getTagValue((byte) 1, headerSet.mAuthResp));
                        i = 193;
                    }
                    headerSet.mAuthResp = null;
                    iValidateResponseCode = i;
                } else {
                    iValidateResponseCode = -1;
                }
                if (iValidateResponseCode != 193) {
                    if (headerSet.mAuthChall != null) {
                        handleAuthChall(headerSet);
                        headerSet2.mAuthResp = new byte[headerSet.mAuthResp.length];
                        System.arraycopy(headerSet.mAuthResp, 0, headerSet2.mAuthResp, 0, headerSet2.mAuthResp.length);
                        headerSet.mAuthChall = null;
                        headerSet.mAuthResp = null;
                    }
                    try {
                        iValidateResponseCode = validateResponseCode(this.mListener.onSetPath(headerSet, headerSet2, (i3 & 1) != 0, (i3 & 2) == 0));
                        if (headerSet2.nonce != null) {
                            this.mChallengeDigest = new byte[16];
                            System.arraycopy(headerSet2.nonce, 0, this.mChallengeDigest, 0, 16);
                        } else {
                            this.mChallengeDigest = null;
                        }
                        long connectionId = this.mListener.getConnectionId();
                        if (connectionId == -1) {
                            headerSet2.mConnectionID = null;
                        } else {
                            headerSet2.mConnectionID = ObexHelper.convertToByteArray(connectionId);
                        }
                        byte[] bArrCreateHeader = ObexHelper.createHeader(headerSet2, false);
                        length = bArrCreateHeader.length + 3;
                        if (length > this.mMaxPacketLength) {
                            length = 3;
                            iValidateResponseCode = ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
                        } else {
                            bArr = bArrCreateHeader;
                        }
                    } catch (Exception e) {
                        if (V) {
                            Log.d(TAG, "Exception occured - sending OBEX_HTTP_INTERNAL_ERROR reply", e);
                        }
                        sendResponse(ResponseCodes.OBEX_HTTP_INTERNAL_ERROR, null);
                        return;
                    }
                }
            }
            byte[] bArr3 = new byte[length];
            bArr3[0] = (byte) iValidateResponseCode;
            bArr3[1] = (byte) (length >> 8);
            bArr3[2] = (byte) length;
            if (bArr != null) {
                System.arraycopy(bArr, 0, bArr3, 3, bArr.length);
            }
            this.mOutput.write(bArr3);
            this.mOutput.flush();
        }
        length = 3;
        byte[] bArr32 = new byte[length];
        bArr32[0] = (byte) iValidateResponseCode;
        bArr32[1] = (byte) (length >> 8);
        bArr32[2] = (byte) length;
        if (bArr != null) {
        }
        this.mOutput.write(bArr32);
        this.mOutput.flush();
    }

    private void handleDisconnectRequest() throws IOException {
        int length;
        byte[] bArr;
        HeaderSet headerSet = new HeaderSet();
        HeaderSet headerSet2 = new HeaderSet();
        int i = (this.mInput.read() << 8) + this.mInput.read();
        int maxRxPacketSize = ObexHelper.getMaxRxPacketSize(this.mTransport);
        int i2 = ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
        if (i > maxRxPacketSize) {
            i2 = ResponseCodes.OBEX_HTTP_REQ_TOO_LARGE;
        } else {
            if (i > 3) {
                byte[] bArr2 = new byte[i - 3];
                int i3 = this.mInput.read(bArr2);
                while (i3 != bArr2.length) {
                    i3 += this.mInput.read(bArr2, i3, bArr2.length - i3);
                }
                ObexHelper.updateHeaderSet(headerSet, bArr2);
            }
            if (this.mListener.getConnectionId() != -1 && headerSet.mConnectionID != null) {
                this.mListener.setConnectionId(ObexHelper.convertToLong(headerSet.mConnectionID));
            } else {
                this.mListener.setConnectionId(1L);
            }
            byte[] bArr3 = headerSet.mAuthResp;
            int i4 = ResponseCodes.OBEX_HTTP_OK;
            if (bArr3 != null) {
                if (!handleAuthResp(headerSet.mAuthResp)) {
                    this.mListener.onAuthenticationFailure(ObexHelper.getTagValue((byte) 1, headerSet.mAuthResp));
                    i4 = 193;
                }
                headerSet.mAuthResp = null;
            }
            if (i4 != 193) {
                if (headerSet.mAuthChall != null) {
                    handleAuthChall(headerSet);
                    headerSet.mAuthChall = null;
                }
                try {
                    this.mListener.onDisconnect(headerSet, headerSet2);
                    long connectionId = this.mListener.getConnectionId();
                    if (connectionId == -1) {
                        headerSet2.mConnectionID = null;
                    } else {
                        headerSet2.mConnectionID = ObexHelper.convertToByteArray(connectionId);
                    }
                    byte[] bArrCreateHeader = ObexHelper.createHeader(headerSet2, false);
                    length = bArrCreateHeader.length + 3;
                    bArr = length <= this.mMaxPacketLength ? bArrCreateHeader : null;
                } catch (Exception e) {
                    if (V) {
                        Log.d(TAG, "Exception occured - sending OBEX_HTTP_INTERNAL_ERROR reply", e);
                    }
                    sendResponse(ResponseCodes.OBEX_HTTP_INTERNAL_ERROR, null);
                    return;
                }
            } else {
                length = 3;
            }
            i2 = i4;
            if (bArr == null) {
                bArr = new byte[bArr.length + 3];
            } else {
                bArr = new byte[3];
            }
            bArr[0] = (byte) i2;
            bArr[1] = (byte) (length >> 8);
            bArr[2] = (byte) length;
            if (bArr != null) {
                System.arraycopy(bArr, 0, bArr, 3, bArr.length);
            }
            this.mOutput.write(bArr);
            this.mOutput.flush();
        }
        length = 3;
        if (bArr == null) {
        }
        bArr[0] = (byte) i2;
        bArr[1] = (byte) (length >> 8);
        bArr[2] = (byte) length;
        if (bArr != null) {
        }
        this.mOutput.write(bArr);
        this.mOutput.flush();
    }

    private void handleConnectRequest() throws IOException {
        int length;
        int maxRxPacketSize;
        HeaderSet headerSet = new HeaderSet();
        HeaderSet headerSet2 = new HeaderSet();
        if (V) {
            Log.v(TAG, "handleConnectRequest()");
        }
        int i = (this.mInput.read() << 8) + this.mInput.read();
        if (V) {
            Log.v(TAG, "handleConnectRequest() - packetLength: " + i);
        }
        int i2 = this.mInput.read();
        int i3 = this.mInput.read();
        this.mMaxPacketLength = this.mInput.read();
        this.mMaxPacketLength = (this.mMaxPacketLength << 8) + this.mInput.read();
        if (V) {
            Log.v(TAG, "handleConnectRequest() - version: " + i2 + " MaxLength: " + this.mMaxPacketLength + " flags: " + i3);
        }
        if (this.mMaxPacketLength > 65534) {
            this.mMaxPacketLength = ObexHelper.MAX_PACKET_SIZE_INT;
        }
        if (this.mMaxPacketLength > ObexHelper.getMaxTxPacketSize(this.mTransport)) {
            Log.w(TAG, "Requested MaxObexPacketSize " + this.mMaxPacketLength + " is larger than the max size supported by the transport: " + ObexHelper.getMaxTxPacketSize(this.mTransport) + " Reducing to this size.");
            this.mMaxPacketLength = ObexHelper.getMaxTxPacketSize(this.mTransport);
        }
        int maxRxPacketSize2 = ObexHelper.getMaxRxPacketSize(this.mTransport);
        int i4 = ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
        byte[] bArr = null;
        if (i > maxRxPacketSize2) {
            i4 = ResponseCodes.OBEX_HTTP_REQ_TOO_LARGE;
        } else {
            if (i > 7) {
                byte[] bArr2 = new byte[i - 7];
                int i5 = this.mInput.read(bArr2);
                while (i5 != bArr2.length) {
                    i5 += this.mInput.read(bArr2, i5, bArr2.length - i5);
                }
                ObexHelper.updateHeaderSet(headerSet, bArr2);
            }
            if (this.mListener.getConnectionId() != -1 && headerSet.mConnectionID != null) {
                this.mListener.setConnectionId(ObexHelper.convertToLong(headerSet.mConnectionID));
            } else {
                this.mListener.setConnectionId(1L);
            }
            int i6 = -1;
            if (headerSet.mAuthResp != null) {
                if (!handleAuthResp(headerSet.mAuthResp)) {
                    this.mListener.onAuthenticationFailure(ObexHelper.getTagValue((byte) 1, headerSet.mAuthResp));
                    i6 = 193;
                }
                headerSet.mAuthResp = null;
            }
            if (i6 != 193) {
                if (headerSet.mAuthChall != null) {
                    handleAuthChall(headerSet);
                    headerSet2.mAuthResp = new byte[headerSet.mAuthResp.length];
                    System.arraycopy(headerSet.mAuthResp, 0, headerSet2.mAuthResp, 0, headerSet2.mAuthResp.length);
                    headerSet.mAuthChall = null;
                    headerSet.mAuthResp = null;
                }
                try {
                    int iValidateResponseCode = validateResponseCode(this.mListener.onConnect(headerSet, headerSet2));
                    if (headerSet2.nonce != null) {
                        this.mChallengeDigest = new byte[16];
                        System.arraycopy(headerSet2.nonce, 0, this.mChallengeDigest, 0, 16);
                    } else {
                        this.mChallengeDigest = null;
                    }
                    long connectionId = this.mListener.getConnectionId();
                    if (connectionId == -1) {
                        headerSet2.mConnectionID = null;
                    } else {
                        headerSet2.mConnectionID = ObexHelper.convertToByteArray(connectionId);
                    }
                    byte[] bArrCreateHeader = ObexHelper.createHeader(headerSet2, false);
                    length = bArrCreateHeader.length + 7;
                    if (length > this.mMaxPacketLength) {
                        length = 7;
                    } else {
                        i4 = iValidateResponseCode;
                        bArr = bArrCreateHeader;
                    }
                } catch (Exception e) {
                    if (V) {
                        Log.d(TAG, "Exception occured - sending OBEX_HTTP_INTERNAL_ERROR reply", e);
                    }
                    length = 7;
                }
            } else {
                length = 7;
                i4 = i6;
            }
            byte[] bArrConvertToByteArray = ObexHelper.convertToByteArray(length);
            byte[] bArr3 = new byte[length];
            maxRxPacketSize = ObexHelper.getMaxRxPacketSize(this.mTransport);
            if (maxRxPacketSize > this.mMaxPacketLength) {
                if (V) {
                    Log.v(TAG, "Set maxRxLength to min of maxRxServrLen:" + maxRxPacketSize + " and MaxNegotiated from Client: " + this.mMaxPacketLength);
                }
                maxRxPacketSize = this.mMaxPacketLength;
            }
            bArr3[0] = (byte) i4;
            bArr3[1] = bArrConvertToByteArray[2];
            bArr3[2] = bArrConvertToByteArray[3];
            bArr3[3] = 16;
            bArr3[4] = 0;
            bArr3[5] = (byte) (maxRxPacketSize >> 8);
            bArr3[6] = (byte) (maxRxPacketSize & 255);
            if (bArr != null) {
                System.arraycopy(bArr, 0, bArr3, 7, bArr.length);
            }
            this.mOutput.write(bArr3);
            this.mOutput.flush();
        }
        length = 7;
        byte[] bArrConvertToByteArray2 = ObexHelper.convertToByteArray(length);
        byte[] bArr32 = new byte[length];
        maxRxPacketSize = ObexHelper.getMaxRxPacketSize(this.mTransport);
        if (maxRxPacketSize > this.mMaxPacketLength) {
        }
        bArr32[0] = (byte) i4;
        bArr32[1] = bArrConvertToByteArray2[2];
        bArr32[2] = bArrConvertToByteArray2[3];
        bArr32[3] = 16;
        bArr32[4] = 0;
        bArr32[5] = (byte) (maxRxPacketSize >> 8);
        bArr32[6] = (byte) (maxRxPacketSize & 255);
        if (bArr != null) {
        }
        this.mOutput.write(bArr32);
        this.mOutput.flush();
    }

    public synchronized void close() {
        if (this.mListener != null) {
            this.mListener.onClose();
        }
        try {
            this.mClosed = true;
            if (this.mInput != null) {
                this.mInput.close();
            }
            if (this.mOutput != null) {
                this.mOutput.close();
            }
        } catch (Exception e) {
            if (V) {
                Log.d(TAG, "Exception occured during close() - ignore", e);
            }
        }
        if (this.mTransport != null) {
            this.mTransport.close();
            this.mTransport = null;
            this.mInput = null;
            this.mOutput = null;
            this.mListener = null;
        } else {
            this.mTransport = null;
            this.mInput = null;
            this.mOutput = null;
            this.mListener = null;
        }
    }

    private int validateResponseCode(int i) {
        if (i >= 160 && i <= 166) {
            return i;
        }
        if (i >= 176 && i <= 181) {
            return i;
        }
        if (i >= 192 && i <= 207) {
            return i;
        }
        if (i >= 208 && i <= 213) {
            return i;
        }
        if (i < 224 || i > 225) {
            return ResponseCodes.OBEX_HTTP_INTERNAL_ERROR;
        }
        return i;
    }

    public ObexTransport getTransport() {
        return this.mTransport;
    }
}
