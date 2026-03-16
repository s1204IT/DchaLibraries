package javax.obex;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class ClientOperation implements Operation, BaseStream {
    private String mExceptionMessage;
    private boolean mGetOperation;
    private int mMaxPacketSize;
    private ClientSession mParent;
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
        if (header.mConnectionID != null) {
            this.mRequestHeader.mConnectionID = new byte[4];
            System.arraycopy(header.mConnectionID, 0, this.mRequestHeader.mConnectionID, 0, 4);
        }
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
            this.mParent.sendRequest(255, null, this.mReplyHeader, null);
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
        if (headerList != null) {
            for (int i = 0; i < headerList.length; i++) {
                this.mRequestHeader.setHeader(headerList[i], headers.getHeader(headerList[i]));
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
        if (this.mPrivateInput == null) {
            startProcessing();
        }
    }

    private boolean sendRequest(int opCode) throws Throwable {
        boolean returnValue = false;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int bodyLength = -1;
        byte[] headerArray = ObexHelper.createHeader(this.mRequestHeader, true);
        if (this.mPrivateOutput != null) {
            bodyLength = this.mPrivateOutput.size();
        }
        if (headerArray.length + 3 > this.mMaxPacketSize) {
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
                if (!this.mParent.sendRequest(opCode, sendHeader, this.mReplyHeader, this.mPrivateInput) || this.mReplyHeader.responseCode != 144) {
                    return false;
                }
                start = end;
            }
            return bodyLength > 0;
        }
        out.write(headerArray);
        if (bodyLength > 0) {
            if (bodyLength > (this.mMaxPacketSize - headerArray.length) - 6) {
                returnValue = true;
                bodyLength = (this.mMaxPacketSize - headerArray.length) - 6;
            }
            byte[] body = this.mPrivateOutput.readBytes(bodyLength);
            if (this.mPrivateOutput.isClosed() && !returnValue && !this.mEndOfBodySent && (opCode & ObexHelper.OBEX_OPCODE_CONNECT) != 0) {
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
            if ((opCode & ObexHelper.OBEX_OPCODE_CONNECT) == 0) {
                out.write(72);
            } else {
                out.write(73);
                this.mEndOfBodySent = true;
            }
            out.write((byte) 0);
            out.write((byte) 3);
        }
        if (out.size() == 0) {
            if (this.mParent.sendRequest(opCode, null, this.mReplyHeader, this.mPrivateInput)) {
                return returnValue;
            }
            return false;
        }
        if (out.size() > 0 && !this.mParent.sendRequest(opCode, out.toByteArray(), this.mReplyHeader, this.mPrivateInput)) {
            return false;
        }
        if (this.mPrivateOutput != null && this.mPrivateOutput.size() > 0) {
            returnValue = true;
        }
        return returnValue;
    }

    private synchronized void startProcessing() throws IOException {
        if (this.mPrivateInput == null) {
            this.mPrivateInput = new PrivateInputStream(this);
        }
        boolean more = true;
        if (this.mGetOperation) {
            if (!this.mOperationDone) {
                if (!this.mGetFinalFlag) {
                    this.mReplyHeader.responseCode = ResponseCodes.OBEX_HTTP_CONTINUE;
                    while (more && this.mReplyHeader.responseCode == 144) {
                        more = sendRequest(3);
                    }
                    if (this.mReplyHeader.responseCode == 144) {
                        this.mParent.sendRequest(ObexHelper.OBEX_OPCODE_GET_FINAL, null, this.mReplyHeader, this.mPrivateInput);
                    }
                    if (this.mReplyHeader.responseCode != 144) {
                        this.mOperationDone = true;
                    }
                } else {
                    if (sendRequest(ObexHelper.OBEX_OPCODE_GET_FINAL)) {
                        throw new IOException("FINAL_GET forced but data did not fit into single packet!");
                    }
                    this.mOperationDone = true;
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
                this.mParent.sendRequest(ObexHelper.OBEX_OPCODE_PUT_FINAL, null, this.mReplyHeader, this.mPrivateInput);
            }
            if (this.mReplyHeader.responseCode != 144) {
                this.mOperationDone = true;
            }
        }
    }

    @Override
    public synchronized boolean continueOperation(boolean sendEmpty, boolean inStream) throws IOException {
        boolean z = true;
        synchronized (this) {
            if (this.mGetOperation) {
                if (inStream && !this.mOperationDone) {
                    this.mParent.sendRequest(ObexHelper.OBEX_OPCODE_GET_FINAL, null, this.mReplyHeader, this.mPrivateInput);
                    if (this.mReplyHeader.responseCode != 144) {
                        this.mOperationDone = true;
                    }
                } else if (!inStream && !this.mOperationDone) {
                    if (this.mPrivateInput == null) {
                        this.mPrivateInput = new PrivateInputStream(this);
                    }
                    if (!this.mGetFinalFlag) {
                        sendRequest(3);
                    } else {
                        sendRequest(ObexHelper.OBEX_OPCODE_GET_FINAL);
                        if (this.mReplyHeader.responseCode != 144) {
                            this.mOperationDone = true;
                        }
                    }
                } else {
                    z = this.mOperationDone ? false : false;
                }
            } else if (!inStream && !this.mOperationDone) {
                if (this.mReplyHeader.responseCode == -1) {
                    this.mReplyHeader.responseCode = ResponseCodes.OBEX_HTTP_CONTINUE;
                }
                sendRequest(2);
            } else if ((!inStream || this.mOperationDone) && this.mOperationDone) {
                z = false;
            }
        }
        return z;
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
            if (inStream && this.mOperationDone) {
                this.mOperationDone = true;
                return;
            }
            return;
        }
        if (inStream && !this.mOperationDone) {
            if (this.mReplyHeader.responseCode == -1) {
                this.mReplyHeader.responseCode = ResponseCodes.OBEX_HTTP_CONTINUE;
            }
            while (this.mReplyHeader.responseCode == 144 && sendRequest(ObexHelper.OBEX_OPCODE_GET_FINAL)) {
            }
            while (this.mReplyHeader.responseCode == 144) {
                this.mParent.sendRequest(ObexHelper.OBEX_OPCODE_GET_FINAL, null, this.mReplyHeader, this.mPrivateInput);
            }
            this.mOperationDone = true;
            return;
        }
        if (!inStream && !this.mOperationDone) {
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
            if (this.mReplyHeader.responseCode != 144) {
                this.mOperationDone = true;
            }
        }
    }

    @Override
    public void noBodyHeader() {
    }
}
