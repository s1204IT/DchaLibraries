package javax.obex;

import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class ClientOperation implements Operation, BaseStream {
    private static final String TAG = "ClientOperation";
    private static final boolean V = ObexHelper.VDBG;
    private String mExceptionMessage;
    private boolean mGetOperation;
    private int mMaxPacketSize;
    private ClientSession mParent;
    private boolean mSendBodyHeader = true;
    private boolean mSrmActive = false;
    private boolean mSrmEnabled = false;
    private boolean mSrmWaitingForRemote = true;
    private boolean mEndOfBodySent = false;
    private boolean mInputOpen = true;
    private boolean mOperationDone = false;
    private boolean mGetFinalFlag = false;
    private boolean mPrivateInputOpen = false;
    private boolean mPrivateOutputOpen = false;
    private PrivateInputStream mPrivateInput = null;
    private PrivateOutputStream mPrivateOutput = null;
    private HeaderSet mReplyHeader = new HeaderSet();
    private HeaderSet mRequestHeader = new HeaderSet();

    public ClientOperation(int i, ClientSession clientSession, HeaderSet headerSet, boolean z) throws IOException {
        this.mParent = clientSession;
        this.mMaxPacketSize = i;
        this.mGetOperation = z;
        int[] headerList = headerSet.getHeaderList();
        if (headerList != null) {
            for (int i2 = 0; i2 < headerList.length; i2++) {
                this.mRequestHeader.setHeader(headerList[i2], headerSet.getHeader(headerList[i2]));
            }
        }
        if (headerSet.mAuthChall != null) {
            this.mRequestHeader.mAuthChall = new byte[headerSet.mAuthChall.length];
            System.arraycopy(headerSet.mAuthChall, 0, this.mRequestHeader.mAuthChall, 0, headerSet.mAuthChall.length);
        }
        if (headerSet.mAuthResp != null) {
            this.mRequestHeader.mAuthResp = new byte[headerSet.mAuthResp.length];
            System.arraycopy(headerSet.mAuthResp, 0, this.mRequestHeader.mAuthResp, 0, headerSet.mAuthResp.length);
        }
        if (headerSet.mConnectionID != null) {
            this.mRequestHeader.mConnectionID = new byte[4];
            System.arraycopy(headerSet.mConnectionID, 0, this.mRequestHeader.mConnectionID, 0, 4);
        }
    }

    public void setGetFinalFlag(boolean z) {
        this.mGetFinalFlag = z;
    }

    @Override
    public synchronized void abort() throws IOException {
        ensureOpen();
        if (this.mOperationDone && this.mReplyHeader.responseCode != 144) {
            throw new IOException("Operation has already ended");
        }
        this.mExceptionMessage = "Operation aborted";
        if (!this.mOperationDone && this.mReplyHeader.responseCode == 144) {
            this.mOperationDone = true;
            this.mParent.sendRequest(255, null, this.mReplyHeader, null, false);
            if (this.mReplyHeader.responseCode != 160) {
                throw new IOException("Invalid response code from server");
            }
            this.mExceptionMessage = null;
        }
        close();
    }

    @Override
    public synchronized int getResponseCode() throws IOException {
        if (V) {
            Log.d(TAG, "getResponseCode()");
        }
        if (this.mReplyHeader.responseCode == -1 || this.mReplyHeader.responseCode == 144) {
            validateConnection();
        }
        if (V) {
            Log.d(TAG, "getResponseCode() return = " + this.mReplyHeader.responseCode);
        }
        return this.mReplyHeader.responseCode;
    }

    @Override
    public String getEncoding() {
        return null;
    }

    @Override
    public String getType() {
        try {
            return (String) this.mReplyHeader.getHeader(66);
        } catch (IOException e) {
            if (V) {
                Log.d(TAG, "Exception occured - returning null", e);
                return null;
            }
            return null;
        }
    }

    @Override
    public long getLength() {
        try {
            Long l = (Long) this.mReplyHeader.getHeader(195);
            if (l == null) {
                return -1L;
            }
            return l.longValue();
        } catch (IOException e) {
            if (V) {
                Log.d(TAG, "Exception occured - returning -1", e);
            }
            return -1L;
        }
    }

    @Override
    public InputStream openInputStream() throws IOException {
        ensureOpen();
        if (this.mPrivateInputOpen) {
            throw new IOException("no more input streams available");
        }
        if (this.mGetOperation) {
            validateConnection();
        } else if (this.mPrivateInput == null) {
            this.mPrivateInput = new PrivateInputStream(this);
        }
        this.mPrivateInputOpen = true;
        return this.mPrivateInput;
    }

    @Override
    public DataInputStream openDataInputStream() throws IOException {
        return new DataInputStream(openInputStream());
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        ensureOpen();
        ensureNotDone();
        if (this.mPrivateOutputOpen) {
            throw new IOException("no more output streams available");
        }
        if (this.mPrivateOutput == null) {
            this.mPrivateOutput = new PrivateOutputStream(this, getMaxPacketSize());
        }
        this.mPrivateOutputOpen = true;
        return this.mPrivateOutput;
    }

    @Override
    public int getMaxPacketSize() {
        return (this.mMaxPacketSize - 6) - getHeaderLength();
    }

    @Override
    public int getHeaderLength() {
        return ObexHelper.createHeader(this.mRequestHeader, false).length;
    }

    @Override
    public DataOutputStream openDataOutputStream() throws IOException {
        return new DataOutputStream(openOutputStream());
    }

    @Override
    public void close() throws IOException {
        this.mInputOpen = false;
        this.mPrivateInputOpen = false;
        this.mPrivateOutputOpen = false;
        this.mParent.setRequestInactive();
    }

    @Override
    public HeaderSet getReceivedHeader() throws IOException {
        ensureOpen();
        return this.mReplyHeader;
    }

    @Override
    public void sendHeaders(HeaderSet headerSet) throws IOException {
        ensureOpen();
        if (this.mOperationDone) {
            throw new IOException("Operation has already exchanged all data");
        }
        if (headerSet == null) {
            throw new IOException("Headers may not be null");
        }
        int[] headerList = headerSet.getHeaderList();
        if (headerList != null) {
            for (int i = 0; i < headerList.length; i++) {
                this.mRequestHeader.setHeader(headerList[i], headerSet.getHeader(headerList[i]));
            }
        }
    }

    @Override
    public void ensureNotDone() throws IOException {
        if (this.mOperationDone) {
            throw new IOException("Operation has completed");
        }
    }

    @Override
    public void ensureOpen() throws IOException {
        this.mParent.ensureOpen();
        if (this.mExceptionMessage != null) {
            throw new IOException(this.mExceptionMessage);
        }
        if (!this.mInputOpen) {
            throw new IOException("Operation has already ended");
        }
    }

    private void validateConnection() throws IOException {
        ensureOpen();
        if (this.mPrivateInput == null || this.mReplyHeader.responseCode == -1) {
            startProcessing();
        }
    }

    private boolean sendRequest(int i) throws IOException {
        int length;
        int i2;
        boolean z;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] bArrCreateHeader = ObexHelper.createHeader(this.mRequestHeader, true);
        if (this.mPrivateOutput != null) {
            length = this.mPrivateOutput.size();
        } else {
            length = -1;
        }
        if (bArrCreateHeader.length + 3 + 3 > this.mMaxPacketSize) {
            int iFindHeaderEnd = 0;
            do {
                int i3 = iFindHeaderEnd;
                if (iFindHeaderEnd != bArrCreateHeader.length) {
                    iFindHeaderEnd = ObexHelper.findHeaderEnd(bArrCreateHeader, i3, this.mMaxPacketSize - 3);
                    if (iFindHeaderEnd == -1) {
                        this.mOperationDone = true;
                        abort();
                        this.mExceptionMessage = "Header larger then can be sent in a packet";
                        this.mInputOpen = false;
                        if (this.mPrivateInput != null) {
                            this.mPrivateInput.close();
                        }
                        if (this.mPrivateOutput != null) {
                            this.mPrivateOutput.close();
                        }
                        throw new IOException("OBEX Packet exceeds max packet size");
                    }
                    byte[] bArr = new byte[iFindHeaderEnd - i3];
                    System.arraycopy(bArrCreateHeader, i3, bArr, 0, bArr.length);
                    if (!this.mParent.sendRequest(i, bArr, this.mReplyHeader, this.mPrivateInput, false)) {
                        return false;
                    }
                } else {
                    checkForSrm();
                    return length > 0;
                }
            } while (this.mReplyHeader.responseCode == 144);
            return false;
        }
        if (!this.mSendBodyHeader) {
            i2 = i | 128;
        } else {
            i2 = i;
        }
        int i4 = i2;
        byteArrayOutputStream.write(bArrCreateHeader);
        if (length > 0) {
            if (length > (this.mMaxPacketSize - bArrCreateHeader.length) - 6) {
                length = (this.mMaxPacketSize - bArrCreateHeader.length) - 6;
                z = true;
            } else {
                z = false;
            }
            byte[] bytes = this.mPrivateOutput.readBytes(length);
            if (this.mPrivateOutput.isClosed() && !z && !this.mEndOfBodySent && (i4 & 128) != 0) {
                byteArrayOutputStream.write(73);
                this.mEndOfBodySent = true;
            } else {
                byteArrayOutputStream.write(72);
            }
            length += 3;
            byteArrayOutputStream.write((byte) (length >> 8));
            byteArrayOutputStream.write((byte) length);
            if (bytes != null) {
                byteArrayOutputStream.write(bytes);
            }
        } else {
            z = false;
        }
        if (this.mPrivateOutputOpen && length <= 0 && !this.mEndOfBodySent) {
            if ((i4 & 128) == 0) {
                byteArrayOutputStream.write(72);
            } else {
                byteArrayOutputStream.write(73);
                this.mEndOfBodySent = true;
            }
            byteArrayOutputStream.write((byte) 0);
            byteArrayOutputStream.write((byte) 3);
        }
        if (byteArrayOutputStream.size() == 0) {
            if (!this.mParent.sendRequest(i4, null, this.mReplyHeader, this.mPrivateInput, this.mSrmActive)) {
                return false;
            }
            checkForSrm();
            return z;
        }
        if (byteArrayOutputStream.size() > 0 && !this.mParent.sendRequest(i4, byteArrayOutputStream.toByteArray(), this.mReplyHeader, this.mPrivateInput, this.mSrmActive)) {
            return false;
        }
        checkForSrm();
        if (this.mPrivateOutput == null || this.mPrivateOutput.size() <= 0) {
            return z;
        }
        return true;
    }

    private void checkForSrm() throws IOException {
        Byte b = (Byte) this.mReplyHeader.getHeader(HeaderSet.SINGLE_RESPONSE_MODE);
        if (this.mParent.isSrmSupported() && b != null && b.byteValue() == 1) {
            this.mSrmEnabled = true;
        }
        if (this.mSrmEnabled) {
            this.mSrmWaitingForRemote = false;
            Byte b2 = (Byte) this.mReplyHeader.getHeader(HeaderSet.SINGLE_RESPONSE_MODE_PARAMETER);
            if (b2 != null && b2.byteValue() == 1) {
                this.mSrmWaitingForRemote = true;
                this.mReplyHeader.setHeader(HeaderSet.SINGLE_RESPONSE_MODE_PARAMETER, null);
            }
        }
        if (!this.mSrmWaitingForRemote && this.mSrmEnabled) {
            this.mSrmActive = true;
        }
    }

    private synchronized void startProcessing() throws IOException {
        if (this.mPrivateInput == null) {
            this.mPrivateInput = new PrivateInputStream(this);
        }
        if (this.mGetOperation) {
            if (!this.mOperationDone) {
                if (!this.mGetFinalFlag) {
                    this.mReplyHeader.responseCode = ResponseCodes.OBEX_HTTP_CONTINUE;
                    boolean zSendRequest = true;
                    while (zSendRequest && this.mReplyHeader.responseCode == 144) {
                        zSendRequest = sendRequest(3);
                    }
                    if (this.mReplyHeader.responseCode == 144) {
                        this.mParent.sendRequest(ObexHelper.OBEX_OPCODE_GET_FINAL, null, this.mReplyHeader, this.mPrivateInput, this.mSrmActive);
                    }
                    if (this.mReplyHeader.responseCode != 144) {
                        this.mOperationDone = true;
                    } else {
                        checkForSrm();
                    }
                } else {
                    if (sendRequest(ObexHelper.OBEX_OPCODE_GET_FINAL)) {
                        throw new IOException("FINAL_GET forced, data didn't fit into one packet");
                    }
                    this.mOperationDone = true;
                }
            }
        } else {
            if (!this.mOperationDone) {
                this.mReplyHeader.responseCode = ResponseCodes.OBEX_HTTP_CONTINUE;
                boolean zSendRequest2 = true;
                while (zSendRequest2 && this.mReplyHeader.responseCode == 144) {
                    zSendRequest2 = sendRequest(2);
                }
            }
            if (this.mReplyHeader.responseCode == 144) {
                this.mParent.sendRequest(ObexHelper.OBEX_OPCODE_PUT_FINAL, null, this.mReplyHeader, this.mPrivateInput, this.mSrmActive);
            }
            if (this.mReplyHeader.responseCode != 144) {
                this.mOperationDone = true;
            }
        }
    }

    @Override
    public synchronized boolean continueOperation(boolean z, boolean z2) throws IOException {
        if (this.mGetOperation) {
            if (z2 && !this.mOperationDone) {
                this.mParent.sendRequest(ObexHelper.OBEX_OPCODE_GET_FINAL, null, this.mReplyHeader, this.mPrivateInput, this.mSrmActive);
                if (this.mReplyHeader.responseCode != 144) {
                    this.mOperationDone = true;
                } else {
                    checkForSrm();
                }
                return true;
            }
            if (!z2 && !this.mOperationDone) {
                if (this.mPrivateInput == null) {
                    this.mPrivateInput = new PrivateInputStream(this);
                }
                if (!this.mGetFinalFlag) {
                    sendRequest(3);
                } else {
                    sendRequest(ObexHelper.OBEX_OPCODE_GET_FINAL);
                }
                if (this.mReplyHeader.responseCode != 144) {
                    this.mOperationDone = true;
                }
                return true;
            }
            if (this.mOperationDone) {
                return false;
            }
        } else {
            if (!z2 && !this.mOperationDone) {
                if (this.mReplyHeader.responseCode == -1) {
                    this.mReplyHeader.responseCode = ResponseCodes.OBEX_HTTP_CONTINUE;
                }
                sendRequest(2);
                return true;
            }
            if (z2 && !this.mOperationDone) {
                return false;
            }
            if (this.mOperationDone) {
                return false;
            }
        }
        return false;
    }

    @Override
    public void streamClosed(boolean z) throws IOException {
        if (!this.mGetOperation) {
            if (z || this.mOperationDone) {
                if (z && this.mOperationDone) {
                    this.mOperationDone = true;
                    return;
                }
                return;
            }
            boolean zSendRequest = this.mPrivateOutput == null || this.mPrivateOutput.size() > 0 || ObexHelper.createHeader(this.mRequestHeader, false).length > 0;
            if (this.mReplyHeader.responseCode == -1) {
                this.mReplyHeader.responseCode = ResponseCodes.OBEX_HTTP_CONTINUE;
            }
            while (zSendRequest && this.mReplyHeader.responseCode == 144) {
                zSendRequest = sendRequest(2);
            }
            while (this.mReplyHeader.responseCode == 144) {
                sendRequest(ObexHelper.OBEX_OPCODE_PUT_FINAL);
            }
            this.mOperationDone = true;
            return;
        }
        if (z && !this.mOperationDone) {
            if (this.mReplyHeader.responseCode == -1) {
                this.mReplyHeader.responseCode = ResponseCodes.OBEX_HTTP_CONTINUE;
            }
            while (this.mReplyHeader.responseCode == 144 && !this.mOperationDone && sendRequest(ObexHelper.OBEX_OPCODE_GET_FINAL)) {
            }
            while (this.mReplyHeader.responseCode == 144 && !this.mOperationDone) {
                this.mParent.sendRequest(ObexHelper.OBEX_OPCODE_GET_FINAL, null, this.mReplyHeader, this.mPrivateInput, false);
            }
            this.mOperationDone = true;
            return;
        }
        if (!z && !this.mOperationDone) {
            boolean zSendRequest2 = this.mPrivateOutput == null || this.mPrivateOutput.size() > 0 || ObexHelper.createHeader(this.mRequestHeader, false).length > 0;
            if (this.mPrivateInput == null) {
                this.mPrivateInput = new PrivateInputStream(this);
            }
            if (this.mPrivateOutput != null && this.mPrivateOutput.size() <= 0) {
                zSendRequest2 = false;
            }
            this.mReplyHeader.responseCode = ResponseCodes.OBEX_HTTP_CONTINUE;
            while (zSendRequest2 && this.mReplyHeader.responseCode == 144) {
                zSendRequest2 = sendRequest(3);
            }
            sendRequest(ObexHelper.OBEX_OPCODE_GET_FINAL);
            if (this.mReplyHeader.responseCode != 144) {
                this.mOperationDone = true;
            }
        }
    }

    @Override
    public void noBodyHeader() {
        this.mSendBodyHeader = false;
    }
}
