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

    public ClientOperation(int maxSize, ClientSession p, HeaderSet header, boolean type) throws IOException {
        this.mParent = p;
        this.mMaxPacketSize = maxSize;
        this.mGetOperation = type;
        int[] headerList = header.getHeaderList();
        if (headerList != null) {
            for (int i = 0; i < headerList.length; i++) {
                this.mRequestHeader.setHeader(headerList[i], header.getHeader(headerList[i]));
            }
        }
        if (header.mAuthChall != null) {
            this.mRequestHeader.mAuthChall = new byte[header.mAuthChall.length];
            System.arraycopy(header.mAuthChall, 0, this.mRequestHeader.mAuthChall, 0, header.mAuthChall.length);
        }
        if (header.mAuthResp != null) {
            this.mRequestHeader.mAuthResp = new byte[header.mAuthResp.length];
            System.arraycopy(header.mAuthResp, 0, this.mRequestHeader.mAuthResp, 0, header.mAuthResp.length);
        }
        if (header.mConnectionID == null) {
            return;
        }
        this.mRequestHeader.mConnectionID = new byte[4];
        System.arraycopy(header.mConnectionID, 0, this.mRequestHeader.mConnectionID, 0, 4);
    }

    public void setGetFinalFlag(boolean flag) {
        this.mGetFinalFlag = flag;
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
        if (this.mReplyHeader.responseCode == -1 || this.mReplyHeader.responseCode == 144) {
            validateConnection();
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
            Long temp = (Long) this.mReplyHeader.getHeader(195);
            if (temp == null) {
                return -1L;
            }
            return temp.longValue();
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
    public int getHeaderLength() throws Throwable {
        byte[] headerArray = ObexHelper.createHeader(this.mRequestHeader, false);
        return headerArray.length;
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
    public void sendHeaders(HeaderSet headers) throws IOException {
        ensureOpen();
        if (this.mOperationDone) {
            throw new IOException("Operation has already exchanged all data");
        }
        if (headers == null) {
            throw new IOException("Headers may not be null");
        }
        int[] headerList = headers.getHeaderList();
        if (headerList == null) {
            return;
        }
        for (int i = 0; i < headerList.length; i++) {
            this.mRequestHeader.setHeader(headerList[i], headers.getHeader(headerList[i]));
        }
    }

    @Override
    public void ensureNotDone() throws IOException {
        if (!this.mOperationDone) {
        } else {
            throw new IOException("Operation has completed");
        }
    }

    @Override
    public void ensureOpen() throws IOException {
        this.mParent.ensureOpen();
        if (this.mExceptionMessage != null) {
            throw new IOException(this.mExceptionMessage);
        }
        if (this.mInputOpen) {
        } else {
            throw new IOException("Operation has already ended");
        }
    }

    private void validateConnection() throws IOException {
        ensureOpen();
        if (this.mPrivateInput != null) {
            return;
        }
        startProcessing();
    }

    private boolean sendRequest(int opCode) throws Throwable {
        boolean returnValue = false;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int bodyLength = -1;
        byte[] headerArray = ObexHelper.createHeader(this.mRequestHeader, true);
        if (this.mPrivateOutput != null) {
            bodyLength = this.mPrivateOutput.size();
        }
        if (headerArray.length + 3 + 3 > this.mMaxPacketSize) {
            int end = 0;
            int start = 0;
            while (end != headerArray.length) {
                end = ObexHelper.findHeaderEnd(headerArray, start, this.mMaxPacketSize - 3);
                if (end == -1) {
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
                byte[] sendHeader = new byte[end - start];
                System.arraycopy(headerArray, start, sendHeader, 0, sendHeader.length);
                if (!this.mParent.sendRequest(opCode, sendHeader, this.mReplyHeader, this.mPrivateInput, false) || this.mReplyHeader.responseCode != 144) {
                    return false;
                }
                start = end;
            }
            checkForSrm();
            if (bodyLength > 0) {
                return true;
            }
            return false;
        }
        if (!this.mSendBodyHeader) {
            opCode |= 128;
        }
        out.write(headerArray);
        if (bodyLength > 0) {
            if (bodyLength > (this.mMaxPacketSize - headerArray.length) - 6) {
                returnValue = true;
                bodyLength = (this.mMaxPacketSize - headerArray.length) - 6;
            }
            byte[] body = this.mPrivateOutput.readBytes(bodyLength);
            if (this.mPrivateOutput.isClosed() && !returnValue && !this.mEndOfBodySent && (opCode & 128) != 0) {
                out.write(73);
                this.mEndOfBodySent = true;
            } else {
                out.write(72);
            }
            bodyLength += 3;
            out.write((byte) (bodyLength >> 8));
            out.write((byte) bodyLength);
            if (body != null) {
                out.write(body);
            }
        }
        if (this.mPrivateOutputOpen && bodyLength <= 0 && !this.mEndOfBodySent) {
            if ((opCode & 128) == 0) {
                out.write(72);
            } else {
                out.write(73);
                this.mEndOfBodySent = true;
            }
            out.write((byte) 0);
            out.write(3);
        }
        if (out.size() == 0) {
            if (!this.mParent.sendRequest(opCode, null, this.mReplyHeader, this.mPrivateInput, this.mSrmActive)) {
                return false;
            }
            checkForSrm();
            return returnValue;
        }
        if (out.size() > 0) {
            if (!this.mParent.sendRequest(opCode, out.toByteArray(), this.mReplyHeader, this.mPrivateInput, this.mSrmActive)) {
                return false;
            }
        }
        checkForSrm();
        if (this.mPrivateOutput != null && this.mPrivateOutput.size() > 0) {
            return true;
        }
        return returnValue;
    }

    private void checkForSrm() throws IOException {
        Byte srmMode = (Byte) this.mReplyHeader.getHeader(HeaderSet.SINGLE_RESPONSE_MODE);
        if (this.mParent.isSrmSupported() && srmMode != null && srmMode.byteValue() == 1) {
            this.mSrmEnabled = true;
        }
        if (this.mSrmEnabled) {
            this.mSrmWaitingForRemote = false;
            Byte srmp = (Byte) this.mReplyHeader.getHeader(HeaderSet.SINGLE_RESPONSE_MODE_PARAMETER);
            if (srmp != null && srmp.byteValue() == 1) {
                this.mSrmWaitingForRemote = true;
                this.mReplyHeader.setHeader(HeaderSet.SINGLE_RESPONSE_MODE_PARAMETER, null);
            }
        }
        if (this.mSrmWaitingForRemote || !this.mSrmEnabled) {
            return;
        }
        this.mSrmActive = true;
    }

    private synchronized void startProcessing() throws IOException {
        if (this.mPrivateInput == null) {
            this.mPrivateInput = new PrivateInputStream(this);
        }
        boolean more = true;
        if (this.mGetOperation) {
            if (!this.mOperationDone) {
                this.mReplyHeader.responseCode = ResponseCodes.OBEX_HTTP_CONTINUE;
                while (more && this.mReplyHeader.responseCode == 144) {
                    more = sendRequest(3);
                }
                if (this.mReplyHeader.responseCode == 144) {
                    this.mParent.sendRequest(ObexHelper.OBEX_OPCODE_GET_FINAL, null, this.mReplyHeader, this.mPrivateInput, this.mSrmActive);
                }
                if (this.mReplyHeader.responseCode != 144) {
                    this.mOperationDone = true;
                } else {
                    checkForSrm();
                }
            }
        } else {
            if (!this.mOperationDone) {
                this.mReplyHeader.responseCode = ResponseCodes.OBEX_HTTP_CONTINUE;
                while (more && this.mReplyHeader.responseCode == 144) {
                    more = sendRequest(2);
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
    public synchronized boolean continueOperation(boolean sendEmpty, boolean inStream) throws IOException {
        if (this.mGetOperation) {
            if (inStream && !this.mOperationDone) {
                this.mParent.sendRequest(ObexHelper.OBEX_OPCODE_GET_FINAL, null, this.mReplyHeader, this.mPrivateInput, this.mSrmActive);
                if (this.mReplyHeader.responseCode != 144) {
                    this.mOperationDone = true;
                } else {
                    checkForSrm();
                }
                return true;
            }
            if (!inStream && !this.mOperationDone) {
                if (this.mPrivateInput == null) {
                    this.mPrivateInput = new PrivateInputStream(this);
                }
                sendRequest(3);
                return true;
            }
            if (this.mOperationDone) {
                return false;
            }
        } else {
            if (!inStream && !this.mOperationDone) {
                if (this.mReplyHeader.responseCode == -1) {
                    this.mReplyHeader.responseCode = ResponseCodes.OBEX_HTTP_CONTINUE;
                }
                sendRequest(2);
                return true;
            }
            if (inStream && !this.mOperationDone) {
                return false;
            }
            if (this.mOperationDone) {
                return false;
            }
        }
        return false;
    }

    @Override
    public void streamClosed(boolean inStream) throws Throwable {
        if (!this.mGetOperation) {
            if (!inStream && !this.mOperationDone) {
                boolean more = true;
                if (this.mPrivateOutput != null && this.mPrivateOutput.size() <= 0) {
                    byte[] headerArray = ObexHelper.createHeader(this.mRequestHeader, false);
                    if (headerArray.length <= 0) {
                        more = false;
                    }
                }
                if (this.mReplyHeader.responseCode == -1) {
                    this.mReplyHeader.responseCode = ResponseCodes.OBEX_HTTP_CONTINUE;
                }
                while (more && this.mReplyHeader.responseCode == 144) {
                    more = sendRequest(2);
                }
                while (this.mReplyHeader.responseCode == 144) {
                    sendRequest(ObexHelper.OBEX_OPCODE_PUT_FINAL);
                }
                this.mOperationDone = true;
                return;
            }
            if (!inStream || !this.mOperationDone) {
                return;
            }
            this.mOperationDone = true;
            return;
        }
        if (inStream && !this.mOperationDone) {
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
        if (inStream || this.mOperationDone) {
            return;
        }
        boolean more2 = true;
        if (this.mPrivateOutput != null && this.mPrivateOutput.size() <= 0) {
            byte[] headerArray2 = ObexHelper.createHeader(this.mRequestHeader, false);
            if (headerArray2.length <= 0) {
                more2 = false;
            }
        }
        if (this.mPrivateInput == null) {
            this.mPrivateInput = new PrivateInputStream(this);
        }
        if (this.mPrivateOutput != null && this.mPrivateOutput.size() <= 0) {
            more2 = false;
        }
        this.mReplyHeader.responseCode = ResponseCodes.OBEX_HTTP_CONTINUE;
        while (more2 && this.mReplyHeader.responseCode == 144) {
            more2 = sendRequest(3);
        }
        sendRequest(ObexHelper.OBEX_OPCODE_GET_FINAL);
        if (this.mReplyHeader.responseCode == 144) {
            return;
        }
        this.mOperationDone = true;
    }

    @Override
    public void noBodyHeader() {
        this.mSendBodyHeader = false;
    }
}
