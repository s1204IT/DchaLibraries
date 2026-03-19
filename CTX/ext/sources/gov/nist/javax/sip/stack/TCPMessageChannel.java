package gov.nist.javax.sip.stack;

import gov.nist.core.InternalErrorHandler;
import gov.nist.core.Separators;
import gov.nist.javax.sip.header.CSeq;
import gov.nist.javax.sip.header.CallID;
import gov.nist.javax.sip.header.From;
import gov.nist.javax.sip.header.RequestLine;
import gov.nist.javax.sip.header.StatusLine;
import gov.nist.javax.sip.header.To;
import gov.nist.javax.sip.header.Via;
import gov.nist.javax.sip.header.ViaList;
import gov.nist.javax.sip.message.SIPMessage;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import gov.nist.javax.sip.parser.Pipeline;
import gov.nist.javax.sip.parser.PipelinedMsgParser;
import gov.nist.javax.sip.parser.SIPMessageListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.ParseException;
import java.util.TimerTask;
import javax.sip.ListeningPoint;
import javax.sip.address.Hop;
import javax.sip.message.Response;

public class TCPMessageChannel extends MessageChannel implements SIPMessageListener, Runnable, RawMessageChannel {
    protected boolean isCached;
    protected boolean isRunning;
    protected String key;
    protected String myAddress;
    protected InputStream myClientInputStream;
    protected OutputStream myClientOutputStream;
    private PipelinedMsgParser myParser;
    protected int myPort;
    private Socket mySock;
    private Thread mythread;
    protected InetAddress peerAddress;
    protected int peerPort;
    protected String peerProtocol;
    protected SIPTransactionStack sipStack;
    private TCPMessageProcessor tcpMessageProcessor;

    protected TCPMessageChannel(SIPTransactionStack sIPTransactionStack) {
        this.sipStack = sIPTransactionStack;
    }

    protected TCPMessageChannel(Socket socket, SIPTransactionStack sIPTransactionStack, TCPMessageProcessor tCPMessageProcessor) throws IOException {
        if (sIPTransactionStack.isLoggingEnabled()) {
            sIPTransactionStack.getStackLogger().logDebug("creating new TCPMessageChannel ");
            sIPTransactionStack.getStackLogger().logStackTrace();
        }
        this.mySock = socket;
        this.peerAddress = this.mySock.getInetAddress();
        this.myAddress = tCPMessageProcessor.getIpAddress().getHostAddress();
        this.myClientInputStream = this.mySock.getInputStream();
        this.myClientOutputStream = this.mySock.getOutputStream();
        this.mythread = new Thread(this);
        this.mythread.setDaemon(true);
        this.mythread.setName("TCPMessageChannelThread");
        this.sipStack = sIPTransactionStack;
        this.peerPort = this.mySock.getPort();
        this.tcpMessageProcessor = tCPMessageProcessor;
        this.myPort = this.tcpMessageProcessor.getPort();
        this.messageProcessor = tCPMessageProcessor;
        this.mythread.start();
    }

    protected TCPMessageChannel(InetAddress inetAddress, int i, SIPTransactionStack sIPTransactionStack, TCPMessageProcessor tCPMessageProcessor) throws IOException {
        if (sIPTransactionStack.isLoggingEnabled()) {
            sIPTransactionStack.getStackLogger().logDebug("creating new TCPMessageChannel ");
            sIPTransactionStack.getStackLogger().logStackTrace();
        }
        this.peerAddress = inetAddress;
        this.peerPort = i;
        this.myPort = tCPMessageProcessor.getPort();
        this.peerProtocol = ListeningPoint.TCP;
        this.sipStack = sIPTransactionStack;
        this.tcpMessageProcessor = tCPMessageProcessor;
        this.myAddress = tCPMessageProcessor.getIpAddress().getHostAddress();
        this.key = MessageChannel.getKey(this.peerAddress, this.peerPort, ListeningPoint.TCP);
        this.messageProcessor = tCPMessageProcessor;
    }

    @Override
    public boolean isReliable() {
        return true;
    }

    @Override
    public void close() {
        try {
            if (this.mySock != null) {
                this.mySock.close();
                this.mySock = null;
            }
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("Closing message Channel " + this);
            }
        } catch (IOException e) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("Error closing socket " + e);
            }
        }
    }

    @Override
    public SIPTransactionStack getSIPStack() {
        return this.sipStack;
    }

    @Override
    public String getTransport() {
        return ListeningPoint.TCP;
    }

    @Override
    public String getPeerAddress() {
        if (this.peerAddress != null) {
            return this.peerAddress.getHostAddress();
        }
        return getHost();
    }

    @Override
    protected InetAddress getPeerInetAddress() {
        return this.peerAddress;
    }

    @Override
    public String getPeerProtocol() {
        return this.peerProtocol;
    }

    private void sendMessage(byte[] bArr, boolean z) throws IOException {
        Socket socketSendBytes = this.sipStack.ioHandler.sendBytes(this.messageProcessor.getIpAddress(), this.peerAddress, this.peerPort, this.peerProtocol, bArr, z, this);
        if (socketSendBytes != this.mySock && socketSendBytes != null) {
            try {
                if (this.mySock != null) {
                    this.mySock.close();
                }
            } catch (IOException e) {
            }
            this.mySock = socketSendBytes;
            this.myClientInputStream = this.mySock.getInputStream();
            this.myClientOutputStream = this.mySock.getOutputStream();
            Thread thread = new Thread(this);
            thread.setDaemon(true);
            thread.setName("TCPMessageChannelThread");
            thread.start();
        }
    }

    @Override
    public void sendMessage(SIPMessage sIPMessage) throws IOException {
        byte[] bArrEncodeAsBytes = sIPMessage.encodeAsBytes(getTransport());
        long jCurrentTimeMillis = System.currentTimeMillis();
        sendMessage(bArrEncodeAsBytes, true);
        if (this.sipStack.getStackLogger().isLoggingEnabled(16)) {
            logMessage(sIPMessage, this.peerAddress, this.peerPort, jCurrentTimeMillis);
        }
    }

    @Override
    public void sendMessage(byte[] bArr, InetAddress inetAddress, int i, boolean z) throws IOException {
        if (bArr == null || inetAddress == null) {
            throw new IllegalArgumentException("Null argument");
        }
        Socket socketSendBytes = this.sipStack.ioHandler.sendBytes(this.messageProcessor.getIpAddress(), inetAddress, i, ListeningPoint.TCP, bArr, z, this);
        if (socketSendBytes != this.mySock && socketSendBytes != null) {
            if (this.mySock != null) {
                this.sipStack.getTimer().schedule(new TimerTask() {
                    @Override
                    public boolean cancel() {
                        try {
                            TCPMessageChannel.this.mySock.close();
                            super.cancel();
                            return true;
                        } catch (IOException e) {
                            return true;
                        }
                    }

                    @Override
                    public void run() {
                        try {
                            TCPMessageChannel.this.mySock.close();
                        } catch (IOException e) {
                        }
                    }
                }, 8000L);
            }
            this.mySock = socketSendBytes;
            this.myClientInputStream = this.mySock.getInputStream();
            this.myClientOutputStream = this.mySock.getOutputStream();
            Thread thread = new Thread(this);
            thread.setDaemon(true);
            thread.setName("TCPMessageChannelThread");
            thread.start();
        }
    }

    @Override
    public void handleException(ParseException parseException, SIPMessage sIPMessage, Class cls, String str, String str2) throws ParseException {
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logException(parseException);
        }
        if (cls != null && (cls.equals(From.class) || cls.equals(To.class) || cls.equals(CSeq.class) || cls.equals(Via.class) || cls.equals(CallID.class) || cls.equals(RequestLine.class) || cls.equals(StatusLine.class))) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("Encountered Bad Message \n" + sIPMessage.toString());
            }
            String string = sIPMessage.toString();
            if (!string.startsWith("SIP/") && !string.startsWith("ACK ")) {
                String strCreateBadReqRes = createBadReqRes(string, parseException);
                if (strCreateBadReqRes != null) {
                    if (this.sipStack.isLoggingEnabled()) {
                        this.sipStack.getStackLogger().logDebug("Sending automatic 400 Bad Request:");
                        this.sipStack.getStackLogger().logDebug(strCreateBadReqRes);
                    }
                    try {
                        sendMessage(strCreateBadReqRes.getBytes(), getPeerInetAddress(), getPeerPort(), false);
                        throw parseException;
                    } catch (IOException e) {
                        this.sipStack.getStackLogger().logException(e);
                        throw parseException;
                    }
                }
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("Could not formulate automatic 400 Bad Request");
                    throw parseException;
                }
                throw parseException;
            }
            throw parseException;
        }
        sIPMessage.addUnparsed(str);
    }

    @Override
    public void processMessage(SIPMessage sIPMessage) throws Exception {
        boolean z;
        boolean zPassToListener;
        boolean z2;
        boolean zPassToListener2;
        if (sIPMessage.getFrom() == null || sIPMessage.getTo() == null || sIPMessage.getCallId() == null || sIPMessage.getCSeq() == null || sIPMessage.getViaHeaders() == null) {
            String strEncode = sIPMessage.encode();
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug(">>> Dropped Bad Msg");
                this.sipStack.getStackLogger().logDebug(strEncode);
                return;
            }
            return;
        }
        ViaList viaHeaders = sIPMessage.getViaHeaders();
        boolean z3 = sIPMessage instanceof SIPRequest;
        if (z3) {
            Via via = (Via) viaHeaders.getFirst();
            Hop hopResolveAddress = this.sipStack.addressResolver.resolveAddress(via.getHop());
            this.peerProtocol = via.getTransport();
            try {
                this.peerAddress = this.mySock.getInetAddress();
                if (via.hasParameter("rport") || !hopResolveAddress.getHost().equals(this.peerAddress.getHostAddress())) {
                    via.setParameter("received", this.peerAddress.getHostAddress());
                }
                via.setParameter("rport", Integer.toString(this.peerPort));
            } catch (ParseException e) {
                InternalErrorHandler.handleException(e, this.sipStack.getStackLogger());
            }
            if (!this.isCached) {
                ((TCPMessageProcessor) this.messageProcessor).cacheMessageChannel(this);
                this.isCached = true;
                this.sipStack.ioHandler.putSocket(IOHandler.makeKey(this.mySock.getInetAddress(), ((InetSocketAddress) this.mySock.getRemoteSocketAddress()).getPort()), this.mySock);
            }
        }
        long jCurrentTimeMillis = System.currentTimeMillis();
        int contentLength = 0;
        if (z3) {
            SIPRequest sIPRequest = (SIPRequest) sIPMessage;
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("----Processing Message---");
            }
            if (this.sipStack.getStackLogger().isLoggingEnabled(16)) {
                this.sipStack.serverLogger.logMessage(sIPMessage, getPeerHostPort().toString(), getMessageProcessor().getIpAddress().getHostAddress() + Separators.COLON + getMessageProcessor().getPort(), false, jCurrentTimeMillis);
            }
            if (this.sipStack.getMaxMessageSize() > 0) {
                if (sIPRequest.getSize() + (sIPRequest.getContentLength() == null ? 0 : sIPRequest.getContentLength().getContentLength()) > this.sipStack.getMaxMessageSize()) {
                    sendMessage(sIPRequest.createResponse(Response.MESSAGE_TOO_LARGE).encodeAsBytes(getTransport()), false);
                    throw new Exception("Message size exceeded");
                }
            }
            ServerRequestInterface serverRequestInterfaceNewSIPServerRequest = this.sipStack.newSIPServerRequest(sIPRequest, this);
            if (serverRequestInterfaceNewSIPServerRequest != 0) {
                try {
                    serverRequestInterfaceNewSIPServerRequest.processRequest(sIPRequest, this);
                    if (z2) {
                        if (!zPassToListener2) {
                            return;
                        } else {
                            return;
                        }
                    }
                    return;
                } finally {
                    if ((serverRequestInterfaceNewSIPServerRequest instanceof SIPTransaction) && !((SIPServerTransaction) serverRequestInterfaceNewSIPServerRequest).passToListener()) {
                        ((SIPTransaction) serverRequestInterfaceNewSIPServerRequest).releaseSem();
                    }
                }
            }
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logWarning("Dropping request -- could not acquire semaphore in 10 sec");
                return;
            }
            return;
        }
        SIPResponse sIPResponse = (SIPResponse) sIPMessage;
        try {
            sIPResponse.checkHeaders();
            if (this.sipStack.getMaxMessageSize() > 0) {
                int size = sIPResponse.getSize();
                if (sIPResponse.getContentLength() != null) {
                    contentLength = sIPResponse.getContentLength().getContentLength();
                }
                if (size + contentLength > this.sipStack.getMaxMessageSize()) {
                    if (this.sipStack.isLoggingEnabled()) {
                        this.sipStack.getStackLogger().logDebug("Message size exceeded");
                        return;
                    }
                    return;
                }
            }
            ServerResponseInterface serverResponseInterfaceNewSIPServerResponse = this.sipStack.newSIPServerResponse(sIPResponse, this);
            if (serverResponseInterfaceNewSIPServerResponse != 0) {
                try {
                    if ((serverResponseInterfaceNewSIPServerResponse instanceof SIPClientTransaction) && !((SIPClientTransaction) serverResponseInterfaceNewSIPServerResponse).checkFromTag(sIPResponse)) {
                        if (this.sipStack.isLoggingEnabled()) {
                            this.sipStack.getStackLogger().logError("Dropping response message with invalid tag >>> " + sIPResponse);
                        }
                        if (z) {
                            if (!zPassToListener) {
                                return;
                            } else {
                                return;
                            }
                        }
                        return;
                    }
                    serverResponseInterfaceNewSIPServerResponse.processResponse(sIPResponse, this);
                    if (serverResponseInterfaceNewSIPServerResponse instanceof SIPTransaction) {
                        SIPTransaction sIPTransaction = (SIPTransaction) serverResponseInterfaceNewSIPServerResponse;
                        if (!sIPTransaction.passToListener()) {
                            sIPTransaction.releaseSem();
                            return;
                        }
                        return;
                    }
                    return;
                } finally {
                    if (serverResponseInterfaceNewSIPServerResponse instanceof SIPTransaction) {
                        SIPTransaction sIPTransaction2 = (SIPTransaction) serverResponseInterfaceNewSIPServerResponse;
                        if (!sIPTransaction2.passToListener()) {
                            sIPTransaction2.releaseSem();
                        }
                    }
                }
            }
            this.sipStack.getStackLogger().logWarning("Application is blocked -- could not acquire semaphore -- dropping response");
        } catch (ParseException e2) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logError("Dropping Badly formatted response message >>> " + sIPResponse);
            }
        }
    }

    @Override
    public void run() {
        byte[] bArr;
        int i;
        Pipeline pipeline = new Pipeline(this.myClientInputStream, this.sipStack.readTimeout, this.sipStack.getTimer());
        this.myParser = new PipelinedMsgParser(this, pipeline, this.sipStack.getMaxMessageSize());
        this.myParser.processInput();
        this.tcpMessageProcessor.useCount++;
        this.isRunning = true;
        while (true) {
            try {
                try {
                    try {
                        bArr = new byte[4096];
                        i = this.myClientInputStream.read(bArr, 0, 4096);
                    } finally {
                        this.isRunning = false;
                        this.tcpMessageProcessor.remove(this);
                        this.tcpMessageProcessor.useCount--;
                        this.myParser.close();
                    }
                } catch (IOException e) {
                    try {
                        pipeline.write("\r\n\r\n".getBytes("UTF-8"));
                    } catch (Exception e2) {
                    }
                    try {
                        if (this.sipStack.isLoggingEnabled()) {
                            this.sipStack.getStackLogger().logDebug("IOException  closing sock " + e);
                        }
                        try {
                            if (this.sipStack.maxConnections != -1) {
                                synchronized (this.tcpMessageProcessor) {
                                    this.tcpMessageProcessor.nConnections--;
                                    this.tcpMessageProcessor.notify();
                                }
                            }
                            this.mySock.close();
                            pipeline.close();
                        } catch (IOException e3) {
                        }
                    } catch (Exception e4) {
                    }
                    return;
                }
            } catch (Exception e5) {
                InternalErrorHandler.handleException(e5, this.sipStack.getStackLogger());
            }
            if (i == -1) {
                break;
            } else {
                pipeline.write(bArr, 0, i);
            }
        }
        pipeline.write("\r\n\r\n".getBytes("UTF-8"));
        try {
            if (this.sipStack.maxConnections != -1) {
                synchronized (this.tcpMessageProcessor) {
                    this.tcpMessageProcessor.nConnections--;
                    this.tcpMessageProcessor.notify();
                }
            }
            pipeline.close();
            this.mySock.close();
        } catch (IOException e6) {
        }
    }

    @Override
    protected void uncache() {
        if (this.isCached && !this.isRunning) {
            this.tcpMessageProcessor.remove(this);
        }
    }

    public boolean equals(Object obj) {
        return getClass().equals(obj.getClass()) && this.mySock == ((TCPMessageChannel) obj).mySock;
    }

    @Override
    public String getKey() {
        if (this.key != null) {
            return this.key;
        }
        this.key = MessageChannel.getKey(this.peerAddress, this.peerPort, ListeningPoint.TCP);
        return this.key;
    }

    @Override
    public String getViaHost() {
        return this.myAddress;
    }

    @Override
    public int getViaPort() {
        return this.myPort;
    }

    @Override
    public int getPeerPort() {
        return this.peerPort;
    }

    @Override
    public int getPeerPacketSourcePort() {
        return this.peerPort;
    }

    @Override
    public InetAddress getPeerPacketSourceAddress() {
        return this.peerAddress;
    }

    @Override
    public boolean isSecure() {
        return false;
    }
}
