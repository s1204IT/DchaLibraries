package javax.obex;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class ServerOperation implements Operation, BaseStream {
    public boolean finalBitSet;
    private boolean mClosed;
    private String mExceptionString;
    private boolean mGetOperation;
    private boolean mHasBody;
    private InputStream mInput;
    private ServerRequestHandler mListener;
    private int mMaxPacketLength;
    private ServerSession mParent;
    private PrivateOutputStream mPrivateOutput;
    private boolean mRequestFinished;
    private boolean mSendBodyHeader = true;
    public boolean isAborted = false;
    public HeaderSet requestHeader = new HeaderSet();
    public HeaderSet replyHeader = new HeaderSet();
    private PrivateInputStream mPrivateInput = new PrivateInputStream(this);
    private int mResponseSize = 3;
    private boolean mPrivateOutputOpen = false;

    public ServerOperation(ServerSession p, InputStream in, int request, int maxSize, ServerRequestHandler listen) throws IOException {
        this.mParent = p;
        this.mInput = in;
        this.mMaxPacketLength = maxSize;
        this.mClosed = false;
        this.mListener = listen;
        this.mRequestFinished = false;
        this.mHasBody = false;
        if (request == 2 || request == 130) {
            this.mGetOperation = false;
            if ((request & ObexHelper.OBEX_OPCODE_CONNECT) == 0) {
                this.finalBitSet = false;
            } else {
                this.finalBitSet = true;
                this.mRequestFinished = true;
            }
        } else if (request == 3 || request == 131) {
            this.mGetOperation = true;
            this.finalBitSet = false;
            if (request == 131) {
                this.mRequestFinished = true;
            }
        } else {
            throw new IOException("ServerOperation can not handle such request");
        }
        int length = (in.read() << 8) + in.read();
        if (length > 65534) {
            this.mParent.sendResponse(ResponseCodes.OBEX_HTTP_REQ_TOO_LARGE, null);
            throw new IOException("Packet received was too large");
        }
        if (length > 3) {
            byte[] data = new byte[length - 3];
            int bytesReceived = in.read(data);
            while (bytesReceived != data.length) {
                bytesReceived += in.read(data, bytesReceived, data.length - bytesReceived);
            }
            byte[] body = ObexHelper.updateHeaderSet(this.requestHeader, data);
            if (body != null) {
                this.mHasBody = true;
            }
            if (this.mListener.getConnectionId() != -1 && this.requestHeader.mConnectionID != null) {
                this.mListener.setConnectionId(ObexHelper.convertToLong(this.requestHeader.mConnectionID));
            } else {
                this.mListener.setConnectionId(1L);
            }
            if (this.requestHeader.mAuthResp != null && !this.mParent.handleAuthResp(this.requestHeader.mAuthResp)) {
                this.mExceptionString = "Authentication Failed";
                this.mParent.sendResponse(ResponseCodes.OBEX_HTTP_UNAUTHORIZED, null);
                this.mClosed = true;
                this.requestHeader.mAuthResp = null;
                return;
            }
            if (this.requestHeader.mAuthChall != null) {
                this.mParent.handleAuthChall(this.requestHeader);
                this.replyHeader.mAuthResp = new byte[this.requestHeader.mAuthResp.length];
                System.arraycopy(this.requestHeader.mAuthResp, 0, this.replyHeader.mAuthResp, 0, this.replyHeader.mAuthResp.length);
                this.requestHeader.mAuthResp = null;
                this.requestHeader.mAuthChall = null;
            }
            if (body != null) {
                this.mPrivateInput.writeBytes(body, 1);
            } else {
                while (!this.mGetOperation && !this.finalBitSet) {
                    sendReply(ResponseCodes.OBEX_HTTP_CONTINUE);
                    if (this.mPrivateInput.available() > 0) {
                        break;
                    }
                }
            }
        }
        while (!this.mGetOperation && !this.finalBitSet && this.mPrivateInput.available() == 0) {
            sendReply(ResponseCodes.OBEX_HTTP_CONTINUE);
            if (this.mPrivateInput.available() > 0) {
                break;
            }
        }
        while (this.mGetOperation && !this.mRequestFinished) {
            sendReply(ResponseCodes.OBEX_HTTP_CONTINUE);
        }
    }

    public boolean isValidBody() {
        return this.mHasBody;
    }

    @Override
    public synchronized boolean continueOperation(boolean sendEmpty, boolean inStream) throws IOException {
        boolean z = true;
        synchronized (this) {
            if (!this.mGetOperation) {
                if (this.finalBitSet) {
                    z = false;
                } else if (!sendEmpty && this.mResponseSize <= 3 && this.mPrivateOutput.size() <= 0) {
                    z = false;
                } else {
                    sendReply(ResponseCodes.OBEX_HTTP_CONTINUE);
                }
            } else {
                sendReply(ResponseCodes.OBEX_HTTP_CONTINUE);
            }
        }
        return z;
    }

    public synchronized boolean sendReply(int type) throws IOException {
        boolean z;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        long id = this.mListener.getConnectionId();
        if (id == -1) {
            this.replyHeader.mConnectionID = null;
        } else {
            this.replyHeader.mConnectionID = ObexHelper.convertToByteArray(id);
        }
        byte[] headerArray = ObexHelper.createHeader(this.replyHeader, true);
        int bodyLength = -1;
        int orginalBodyLength = -1;
        if (this.mPrivateOutput != null) {
            bodyLength = this.mPrivateOutput.size();
            orginalBodyLength = bodyLength;
        }
        if (headerArray.length + 3 > this.mMaxPacketLength) {
            int end = 0;
            int start = 0;
            while (end != headerArray.length) {
                end = ObexHelper.findHeaderEnd(headerArray, start, this.mMaxPacketLength - 3);
                if (end == -1) {
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
                byte[] sendHeader = new byte[end - start];
                System.arraycopy(headerArray, start, sendHeader, 0, sendHeader.length);
                this.mParent.sendResponse(type, sendHeader);
                start = end;
            }
            if (bodyLength > 0) {
                z = true;
            } else {
                z = false;
            }
        } else {
            out.write(headerArray);
            if (this.mGetOperation && type == 160) {
                this.finalBitSet = true;
            }
            if ((this.finalBitSet || headerArray.length < this.mMaxPacketLength - 20) && bodyLength > 0) {
                if (bodyLength > (this.mMaxPacketLength - headerArray.length) - 6) {
                    bodyLength = (this.mMaxPacketLength - headerArray.length) - 6;
                }
                byte[] body = this.mPrivateOutput.readBytes(bodyLength);
                if (this.finalBitSet || this.mPrivateOutput.isClosed()) {
                    if (this.mSendBodyHeader) {
                        out.write(73);
                        int bodyLength2 = bodyLength + 3;
                        out.write((byte) (bodyLength2 >> 8));
                        out.write((byte) bodyLength2);
                        out.write(body);
                    }
                } else if (this.mSendBodyHeader) {
                    out.write(72);
                    int bodyLength3 = bodyLength + 3;
                    out.write((byte) (bodyLength3 >> 8));
                    out.write((byte) bodyLength3);
                    out.write(body);
                }
            }
            if (this.finalBitSet && type == 160 && orginalBodyLength <= 0 && this.mSendBodyHeader) {
                out.write(73);
                out.write((byte) 0);
                out.write((byte) 3);
            }
            this.mResponseSize = 3;
            this.mParent.sendResponse(type, out.toByteArray());
            if (type == 144) {
                int headerID = this.mInput.read();
                int length = (this.mInput.read() << 8) + this.mInput.read();
                if (headerID != 2 && headerID != 130 && headerID != 3 && headerID != 131) {
                    if (length > 3) {
                        byte[] temp = new byte[length - 3];
                        int bytesReceived = this.mInput.read(temp);
                        while (bytesReceived != temp.length) {
                            bytesReceived += this.mInput.read(temp, bytesReceived, temp.length - bytesReceived);
                        }
                    }
                    if (headerID == 255) {
                        this.mParent.sendResponse(ResponseCodes.OBEX_HTTP_OK, null);
                        this.mClosed = true;
                        this.isAborted = true;
                        this.mExceptionString = "Abort Received";
                        throw new IOException("Abort Received");
                    }
                    this.mParent.sendResponse(192, null);
                    this.mClosed = true;
                    this.mExceptionString = "Bad Request Received";
                    throw new IOException("Bad Request Received");
                }
                if (headerID == 130) {
                    this.finalBitSet = true;
                } else if (headerID == 131) {
                    this.mRequestFinished = true;
                }
                if (length > 65534) {
                    this.mParent.sendResponse(ResponseCodes.OBEX_HTTP_REQ_TOO_LARGE, null);
                    throw new IOException("Packet received was too large");
                }
                if (length > 3) {
                    byte[] data = new byte[length - 3];
                    int bytesReceived2 = this.mInput.read(data);
                    while (bytesReceived2 != data.length) {
                        bytesReceived2 += this.mInput.read(data, bytesReceived2, data.length - bytesReceived2);
                    }
                    byte[] body2 = ObexHelper.updateHeaderSet(this.requestHeader, data);
                    if (body2 != null) {
                        this.mHasBody = true;
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
                            z = false;
                        } else {
                            this.requestHeader.mAuthResp = null;
                        }
                    }
                    if (this.requestHeader.mAuthChall != null) {
                        this.mParent.handleAuthChall(this.requestHeader);
                        this.replyHeader.mAuthResp = new byte[this.requestHeader.mAuthResp.length];
                        System.arraycopy(this.requestHeader.mAuthResp, 0, this.replyHeader.mAuthResp, 0, this.replyHeader.mAuthResp.length);
                        this.requestHeader.mAuthResp = null;
                        this.requestHeader.mAuthChall = null;
                    }
                    if (body2 != null) {
                        this.mPrivateInput.writeBytes(body2, 1);
                    }
                    z = true;
                } else {
                    z = true;
                }
            } else {
                z = false;
            }
        }
        return z;
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
    public void sendHeaders(HeaderSet headers) throws IOException {
        ensureOpen();
        if (headers == null) {
            throw new IOException("Headers may not be null");
        }
        int[] headerList = headers.getHeaderList();
        if (headerList != null) {
            for (int i = 0; i < headerList.length; i++) {
                this.replyHeader.setHeader(headerList[i], headers.getHeader(headerList[i]));
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
            Long temp = (Long) this.requestHeader.getHeader(195);
            if (temp == null) {
                return -1L;
            }
            return temp.longValue();
        } catch (IOException e) {
            return -1L;
        }
    }

    @Override
    public int getMaxPacketSize() {
        return (this.mMaxPacketLength - 6) - getHeaderLength();
    }

    @Override
    public int getHeaderLength() throws Throwable {
        long id = this.mListener.getConnectionId();
        if (id == -1) {
            this.replyHeader.mConnectionID = null;
        } else {
            this.replyHeader.mConnectionID = ObexHelper.convertToByteArray(id);
        }
        byte[] headerArray = ObexHelper.createHeader(this.replyHeader, false);
        return headerArray.length;
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
    public void streamClosed(boolean inStream) throws IOException {
    }

    @Override
    public void noBodyHeader() {
        this.mSendBodyHeader = false;
    }
}
