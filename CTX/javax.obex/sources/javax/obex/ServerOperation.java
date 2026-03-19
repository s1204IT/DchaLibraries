package javax.obex;

import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class ServerOperation implements Operation, BaseStream {
    private static final String TAG = "ServerOperation";
    private static final boolean V = ObexHelper.VDBG;
    public boolean finalBitSet;
    private String mExceptionString;
    private boolean mGetOperation;
    private InputStream mInput;
    private ServerRequestHandler mListener;
    private int mMaxPacketLength;
    private ServerSession mParent;
    private PrivateOutputStream mPrivateOutput;
    private boolean mRequestFinished;
    private ObexTransport mTransport;
    private boolean mSendBodyHeader = true;
    private boolean mSrmEnabled = false;
    private boolean mSrmActive = false;
    private boolean mSrmResponseSent = false;
    private boolean mSrmWaitingForRemote = true;
    private boolean mSrmLocalWait = false;
    public boolean isAborted = false;
    private boolean mClosed = false;
    public HeaderSet requestHeader = new HeaderSet();
    public HeaderSet replyHeader = new HeaderSet();
    private PrivateInputStream mPrivateInput = new PrivateInputStream(this);
    private int mResponseSize = 3;
    private boolean mPrivateOutputOpen = false;
    private boolean mHasBody = false;

    public ServerOperation(ServerSession serverSession, InputStream inputStream, int i, int i2, ServerRequestHandler serverRequestHandler) throws IOException {
        this.mParent = serverSession;
        this.mInput = inputStream;
        this.mMaxPacketLength = i2;
        this.mListener = serverRequestHandler;
        this.mRequestFinished = false;
        this.mTransport = serverSession.getTransport();
        if (i != 2 && i != 130) {
            if (i == 3 || i == 131) {
                this.mGetOperation = true;
                this.finalBitSet = false;
                if (i == 131) {
                    this.mRequestFinished = true;
                }
            } else {
                throw new IOException("ServerOperation can not handle such request");
            }
        } else {
            this.mGetOperation = false;
            if ((i & 128) == 0) {
                this.finalBitSet = false;
            } else {
                this.finalBitSet = true;
                this.mRequestFinished = true;
            }
        }
        ObexPacket obexPacket = ObexPacket.read(i, this.mInput);
        if (obexPacket.mLength <= ObexHelper.getMaxRxPacketSize(this.mTransport)) {
            if (obexPacket.mLength > 3) {
                if (!handleObexPacket(obexPacket)) {
                    return;
                }
                if (V) {
                    Log.v(TAG, "Get App confirmation if SRM ENABLED case: " + this.mSrmEnabled + " not hasBody case: " + this.mHasBody);
                }
                if (!this.mHasBody && !this.mSrmEnabled) {
                    while (!this.mGetOperation && !this.finalBitSet) {
                        sendReply(ResponseCodes.OBEX_HTTP_CONTINUE);
                        if (this.mPrivateInput.available() > 0) {
                            break;
                        }
                    }
                }
            }
            if (V) {
                Log.v(TAG, "Get App confirmation if SRM ENABLED case: " + this.mSrmEnabled + " not finalPacket: " + this.finalBitSet + " not GETOp Case: " + this.mGetOperation);
            }
            while (!this.mSrmEnabled && !this.mGetOperation && !this.finalBitSet && this.mPrivateInput.available() == 0) {
                sendReply(ResponseCodes.OBEX_HTTP_CONTINUE);
                if (this.mPrivateInput.available() > 0) {
                    break;
                }
            }
            while (this.mGetOperation && !this.mRequestFinished) {
                sendReply(ResponseCodes.OBEX_HTTP_CONTINUE);
            }
            return;
        }
        this.mParent.sendResponse(ResponseCodes.OBEX_HTTP_REQ_TOO_LARGE, null);
        throw new IOException("Packet received was too large. Length: " + obexPacket.mLength + " maxLength: " + ObexHelper.getMaxRxPacketSize(this.mTransport));
    }

    private boolean handleObexPacket(ObexPacket obexPacket) throws IOException {
        byte[] bArrUpdateRequestHeaders = updateRequestHeaders(obexPacket);
        if (bArrUpdateRequestHeaders != null) {
            this.mHasBody = true;
        }
        if (V) {
            Log.d(TAG, "handleObexPacket has body = " + this.mHasBody);
        }
        if (this.mListener.getConnectionId() != -1 && this.requestHeader.mConnectionID != null) {
            this.mListener.setConnectionId(ObexHelper.convertToLong(this.requestHeader.mConnectionID));
        } else {
            this.mListener.setConnectionId(1L);
        }
        if (this.requestHeader.mAuthResp != null) {
            if (!this.mParent.handleAuthResp(this.requestHeader.mAuthResp)) {
                this.mExceptionString = "Authentication Failed";
                this.mParent.sendResponse(ResponseCodes.OBEX_HTTP_UNAUTHORIZED, null);
                this.mClosed = true;
                this.requestHeader.mAuthResp = null;
                return false;
            }
            this.requestHeader.mAuthResp = null;
        }
        if (this.requestHeader.mAuthChall != null) {
            this.mParent.handleAuthChall(this.requestHeader);
            this.replyHeader.mAuthResp = new byte[this.requestHeader.mAuthResp.length];
            System.arraycopy(this.requestHeader.mAuthResp, 0, this.replyHeader.mAuthResp, 0, this.replyHeader.mAuthResp.length);
            this.requestHeader.mAuthResp = null;
            this.requestHeader.mAuthChall = null;
        }
        if (bArrUpdateRequestHeaders != null) {
            this.mPrivateInput.writeBytes(bArrUpdateRequestHeaders, 1);
        }
        return true;
    }

    private byte[] updateRequestHeaders(ObexPacket obexPacket) throws IOException {
        byte[] bArrUpdateHeaderSet;
        if (obexPacket.mPayload != null) {
            bArrUpdateHeaderSet = ObexHelper.updateHeaderSet(this.requestHeader, obexPacket.mPayload);
        } else {
            bArrUpdateHeaderSet = null;
        }
        Byte b = (Byte) this.requestHeader.getHeader(HeaderSet.SINGLE_RESPONSE_MODE);
        if (this.mTransport.isSrmSupported() && b != null && b.byteValue() == 1) {
            this.mSrmEnabled = true;
            if (V) {
                Log.d(TAG, "SRM is now ENABLED (but not active) for this operation");
            }
        }
        checkForSrmWait(obexPacket.mHeaderId);
        if (!this.mSrmWaitingForRemote && this.mSrmEnabled) {
            if (V) {
                Log.d(TAG, "SRM is now ACTIVE for this operation");
            }
            this.mSrmActive = true;
        }
        return bArrUpdateHeaderSet;
    }

    private void checkForSrmWait(int i) {
        if (this.mSrmEnabled) {
            if (i == 3 || i == 131 || i == 2) {
                try {
                    this.mSrmWaitingForRemote = false;
                    Byte b = (Byte) this.requestHeader.getHeader(HeaderSet.SINGLE_RESPONSE_MODE_PARAMETER);
                    if (b != null && b.byteValue() == 1) {
                        this.mSrmWaitingForRemote = true;
                        this.requestHeader.setHeader(HeaderSet.SINGLE_RESPONSE_MODE_PARAMETER, null);
                    }
                } catch (IOException e) {
                    if (V) {
                        Log.w(TAG, "Exception while extracting header", e);
                    }
                }
            }
        }
    }

    public boolean isValidBody() {
        return this.mHasBody;
    }

    @Override
    public synchronized boolean continueOperation(boolean z, boolean z2) throws IOException {
        if (!this.mGetOperation) {
            if (this.finalBitSet) {
                return false;
            }
            if (z) {
                sendReply(ResponseCodes.OBEX_HTTP_CONTINUE);
                return true;
            }
            if (this.mResponseSize <= 3 && this.mPrivateOutput.size() <= 0) {
                return false;
            }
            sendReply(ResponseCodes.OBEX_HTTP_CONTINUE);
            return true;
        }
        sendReply(ResponseCodes.OBEX_HTTP_CONTINUE);
        return true;
    }

    public synchronized boolean sendReply(int i) throws IOException {
        boolean z;
        int length;
        boolean z2;
        boolean z3;
        if (V) {
            Log.d(TAG, "sendReply type = " + i);
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        long connectionId = this.mListener.getConnectionId();
        if (connectionId == -1) {
            this.replyHeader.mConnectionID = null;
        } else {
            this.replyHeader.mConnectionID = ObexHelper.convertToByteArray(connectionId);
        }
        if (!this.mSrmEnabled || this.mSrmResponseSent) {
            z = false;
        } else {
            if (V) {
                Log.v(TAG, "mSrmEnabled==true, sending SRM enable response.");
            }
            this.replyHeader.setHeader(HeaderSet.SINGLE_RESPONSE_MODE, (byte) 1);
            z = true;
        }
        if (this.mSrmEnabled && !this.mGetOperation && this.mSrmLocalWait) {
            this.replyHeader.setHeader(HeaderSet.SINGLE_RESPONSE_MODE, (byte) 1);
        }
        byte[] bArrCreateHeader = ObexHelper.createHeader(this.replyHeader, true);
        if (this.mPrivateOutput != null) {
            length = this.mPrivateOutput.size();
        } else {
            length = -1;
        }
        int i2 = length;
        if (bArrCreateHeader.length + 3 > this.mMaxPacketLength) {
            int iFindHeaderEnd = 0;
            while (true) {
                int i3 = iFindHeaderEnd;
                if (iFindHeaderEnd == bArrCreateHeader.length) {
                    return length > 0;
                }
                iFindHeaderEnd = ObexHelper.findHeaderEnd(bArrCreateHeader, i3, this.mMaxPacketLength - 3);
                if (iFindHeaderEnd == -1) {
                    this.mClosed = true;
                    if (this.mPrivateInput != null) {
                        this.mPrivateInput.close();
                    }
                    if (this.mPrivateOutput != null) {
                        this.mPrivateOutput.close();
                    }
                    this.mParent.sendResponse(ResponseCodes.OBEX_HTTP_INTERNAL_ERROR, null);
                    throw new IOException("OBEX Packet exceeds max packet size");
                }
                byte[] bArr = new byte[iFindHeaderEnd - i3];
                System.arraycopy(bArrCreateHeader, i3, bArr, 0, bArr.length);
                this.mParent.sendResponse(i, bArr);
            }
        } else {
            byteArrayOutputStream.write(bArrCreateHeader);
            if (this.mGetOperation && i == 160) {
                this.finalBitSet = true;
            }
            if (this.mSrmActive) {
                if ((this.mGetOperation || i != 144 || !this.mSrmResponseSent) && (!this.mGetOperation || this.mRequestFinished || !this.mSrmResponseSent)) {
                    if (this.mGetOperation && this.mRequestFinished) {
                        z2 = false;
                        z3 = true;
                    } else {
                        z2 = false;
                        z3 = false;
                    }
                } else {
                    z3 = false;
                    z2 = true;
                }
                if (V) {
                    Log.v(TAG, "type==" + i + " skipSend==" + z2 + " skipReceive==" + z3);
                }
            } else {
                z2 = false;
                z3 = false;
            }
            if (z) {
                if (V) {
                    Log.v(TAG, "SRM Enabled (srmRespSendPending == true)- sending SRM Enable response");
                }
                this.mSrmResponseSent = true;
            }
            if ((this.finalBitSet || bArrCreateHeader.length < this.mMaxPacketLength - 20) && length > 0) {
                if (length > (this.mMaxPacketLength - bArrCreateHeader.length) - 6) {
                    length = (this.mMaxPacketLength - bArrCreateHeader.length) - 6;
                }
                if (V) {
                    Log.d(TAG, "readBytes +++");
                }
                byte[] bytes = this.mPrivateOutput.readBytes(length);
                if (V) {
                    Log.d(TAG, "readBytes --- body = " + bytes.length);
                }
                if (this.finalBitSet || this.mPrivateOutput.isClosed()) {
                    if (this.mSendBodyHeader) {
                        byteArrayOutputStream.write(73);
                        int i4 = length + 3;
                        byteArrayOutputStream.write((byte) (i4 >> 8));
                        byteArrayOutputStream.write((byte) i4);
                        byteArrayOutputStream.write(bytes);
                    }
                } else if (this.mSendBodyHeader) {
                    byteArrayOutputStream.write(72);
                    int i5 = length + 3;
                    byteArrayOutputStream.write((byte) (i5 >> 8));
                    byteArrayOutputStream.write((byte) i5);
                    byteArrayOutputStream.write(bytes);
                }
            }
            if (this.finalBitSet && i == 160 && i2 <= 0 && this.mSendBodyHeader) {
                byteArrayOutputStream.write(73);
                byteArrayOutputStream.write((byte) 0);
                byteArrayOutputStream.write((byte) 3);
            }
            if (!z2) {
                this.mResponseSize = 3;
                this.mParent.sendResponse(i, byteArrayOutputStream.toByteArray());
            }
            if (i != 144) {
                return false;
            }
            if (this.mGetOperation && z3) {
                checkSrmRemoteAbort();
            } else {
                ObexPacket obexPacket = ObexPacket.read(this.mInput);
                if (V) {
                    Log.d(TAG, "read packet finished, packet length = " + obexPacket.mLength);
                }
                int i6 = obexPacket.mHeaderId;
                if (i6 != 2 && i6 != 130 && i6 != 3 && i6 != 131) {
                    if (i6 == 255) {
                        handleRemoteAbort();
                    } else {
                        this.mParent.sendResponse(192, null);
                        this.mClosed = true;
                        this.mExceptionString = "Bad Request Received";
                        throw new IOException("Bad Request Received");
                    }
                } else {
                    if (i6 == 130) {
                        this.finalBitSet = true;
                    } else if (i6 == 131) {
                        this.mRequestFinished = true;
                    }
                    if (obexPacket.mLength > ObexHelper.getMaxRxPacketSize(this.mTransport)) {
                        this.mParent.sendResponse(ResponseCodes.OBEX_HTTP_REQ_TOO_LARGE, null);
                        throw new IOException("Packet received was too large");
                    }
                    if ((obexPacket.mLength > 3 || (this.mSrmEnabled && obexPacket.mLength == 3)) && !handleObexPacket(obexPacket)) {
                        return false;
                    }
                }
            }
            if (V) {
                Log.d(TAG, "sendReply completed");
            }
            return true;
        }
    }

    private void checkSrmRemoteAbort() throws IOException {
        if (this.mInput.available() > 0) {
            ObexPacket obexPacket = ObexPacket.read(this.mInput);
            if (obexPacket.mHeaderId == 255) {
                handleRemoteAbort();
                return;
            }
            Log.w(TAG, "Received unexpected request from client - discarding...\n   headerId: " + obexPacket.mHeaderId + " length: " + obexPacket.mLength);
        }
    }

    private void handleRemoteAbort() throws IOException {
        this.mParent.sendResponse(ResponseCodes.OBEX_HTTP_OK, null);
        this.mClosed = true;
        this.isAborted = true;
        this.mExceptionString = "Abort Received";
        throw new IOException("Abort Received");
    }

    @Override
    public void abort() throws IOException {
        throw new IOException("Called from a server");
    }

    @Override
    public HeaderSet getReceivedHeader() throws IOException {
        ensureOpen();
        return this.requestHeader;
    }

    @Override
    public void sendHeaders(HeaderSet headerSet) throws IOException {
        ensureOpen();
        if (headerSet == null) {
            throw new IOException("Headers may not be null");
        }
        int[] headerList = headerSet.getHeaderList();
        if (headerList != null) {
            for (int i = 0; i < headerList.length; i++) {
                this.replyHeader.setHeader(headerList[i], headerSet.getHeader(headerList[i]));
            }
        }
    }

    @Override
    public int getResponseCode() throws IOException {
        throw new IOException("Called from a server");
    }

    @Override
    public String getEncoding() {
        return null;
    }

    @Override
    public String getType() {
        try {
            return (String) this.requestHeader.getHeader(66);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public long getLength() {
        try {
            Long l = (Long) this.requestHeader.getHeader(195);
            if (l == null) {
                return -1L;
            }
            return l.longValue();
        } catch (IOException e) {
            return -1L;
        }
    }

    @Override
    public int getMaxPacketSize() {
        return (this.mMaxPacketLength - 6) - getHeaderLength();
    }

    @Override
    public int getHeaderLength() {
        long connectionId = this.mListener.getConnectionId();
        if (connectionId == -1) {
            this.replyHeader.mConnectionID = null;
        } else {
            this.replyHeader.mConnectionID = ObexHelper.convertToByteArray(connectionId);
        }
        return ObexHelper.createHeader(this.replyHeader, false).length;
    }

    @Override
    public InputStream openInputStream() throws IOException {
        ensureOpen();
        return this.mPrivateInput;
    }

    @Override
    public DataInputStream openDataInputStream() throws IOException {
        return new DataInputStream(openInputStream());
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        ensureOpen();
        if (this.mPrivateOutputOpen) {
            throw new IOException("no more input streams available, stream already opened");
        }
        if (!this.mRequestFinished) {
            throw new IOException("no  output streams available ,request not finished");
        }
        if (this.mPrivateOutput == null) {
            this.mPrivateOutput = new PrivateOutputStream(this, getMaxPacketSize());
        }
        this.mPrivateOutputOpen = true;
        return this.mPrivateOutput;
    }

    @Override
    public DataOutputStream openDataOutputStream() throws IOException {
        return new DataOutputStream(openOutputStream());
    }

    @Override
    public void close() throws IOException {
        ensureOpen();
        this.mClosed = true;
    }

    @Override
    public void ensureOpen() throws IOException {
        if (this.mExceptionString != null) {
            throw new IOException(this.mExceptionString);
        }
        if (this.mClosed) {
            throw new IOException("Operation has already ended");
        }
    }

    @Override
    public void ensureNotDone() throws IOException {
    }

    @Override
    public void streamClosed(boolean z) throws IOException {
    }

    @Override
    public void noBodyHeader() {
        this.mSendBodyHeader = false;
    }
}
