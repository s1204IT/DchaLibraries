package gov.nist.javax.sip.stack;

import gov.nist.core.InternalErrorHandler;
import gov.nist.core.Separators;
import gov.nist.javax.sip.address.ParameterNames;
import gov.nist.javax.sip.header.CSeq;
import gov.nist.javax.sip.header.CallID;
import gov.nist.javax.sip.header.From;
import gov.nist.javax.sip.header.RequestLine;
import gov.nist.javax.sip.header.RetryAfter;
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
import java.net.InetAddress;
import java.net.Socket;
import java.text.ParseException;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLSocket;
import javax.sip.ListeningPoint;
import javax.sip.address.Hop;
import javax.sip.message.Response;

public final class TLSMessageChannel extends MessageChannel implements SIPMessageListener, Runnable, RawMessageChannel {
    private HandshakeCompletedListener handshakeCompletedListener;
    protected boolean isCached;
    protected boolean isRunning;
    private String key;
    private String myAddress;
    private InputStream myClientInputStream;
    private PipelinedMsgParser myParser;
    private int myPort;
    private Socket mySock;
    private Thread mythread;
    private InetAddress peerAddress;
    private int peerPort;
    private String peerProtocol;
    private SIPTransactionStack sipStack;
    private TLSMessageProcessor tlsMessageProcessor;

    protected TLSMessageChannel(Socket socket, SIPTransactionStack sIPTransactionStack, TLSMessageProcessor tLSMessageProcessor) throws IOException {
        if (sIPTransactionStack.isLoggingEnabled()) {
            sIPTransactionStack.getStackLogger().logDebug("creating new TLSMessageChannel (incoming)");
            sIPTransactionStack.getStackLogger().logStackTrace();
        }
        SSLSocket sSLSocket = (SSLSocket) socket;
        this.mySock = sSLSocket;
        if (socket instanceof SSLSocket) {
            sSLSocket.setNeedClientAuth(true);
            this.handshakeCompletedListener = new HandshakeCompletedListenerImpl(this);
            sSLSocket.addHandshakeCompletedListener(this.handshakeCompletedListener);
            sSLSocket.startHandshake();
        }
        this.peerAddress = this.mySock.getInetAddress();
        this.myAddress = tLSMessageProcessor.getIpAddress().getHostAddress();
        this.myClientInputStream = this.mySock.getInputStream();
        this.mythread = new Thread(this);
        this.mythread.setDaemon(true);
        this.mythread.setName("TLSMessageChannelThread");
        this.sipStack = sIPTransactionStack;
        this.tlsMessageProcessor = tLSMessageProcessor;
        this.myPort = this.tlsMessageProcessor.getPort();
        this.peerPort = this.mySock.getPort();
        this.messageProcessor = tLSMessageProcessor;
        this.mythread.start();
    }

    protected TLSMessageChannel(InetAddress inetAddress, int i, SIPTransactionStack sIPTransactionStack, TLSMessageProcessor tLSMessageProcessor) throws IOException {
        if (sIPTransactionStack.isLoggingEnabled()) {
            sIPTransactionStack.getStackLogger().logDebug("creating new TLSMessageChannel (outgoing)");
            sIPTransactionStack.getStackLogger().logStackTrace();
        }
        this.peerAddress = inetAddress;
        this.peerPort = i;
        this.myPort = tLSMessageProcessor.getPort();
        this.peerProtocol = ListeningPoint.TLS;
        this.sipStack = sIPTransactionStack;
        this.tlsMessageProcessor = tLSMessageProcessor;
        this.myAddress = tLSMessageProcessor.getIpAddress().getHostAddress();
        this.key = MessageChannel.getKey(this.peerAddress, this.peerPort, ListeningPoint.TLS);
        this.messageProcessor = tLSMessageProcessor;
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
        return ParameterNames.TLS;
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
        Socket socketSendBytes = this.sipStack.ioHandler.sendBytes(getMessageProcessor().getIpAddress(), this.peerAddress, this.peerPort, this.peerProtocol, bArr, z, this);
        if (socketSendBytes != this.mySock && socketSendBytes != null) {
            try {
                if (this.mySock != null) {
                    this.mySock.close();
                }
            } catch (IOException e) {
            }
            this.mySock = socketSendBytes;
            this.myClientInputStream = this.mySock.getInputStream();
            Thread thread = new Thread(this);
            thread.setDaemon(true);
            thread.setName("TLSMessageChannelThread");
            thread.start();
        }
    }

    @Override
    public void sendMessage(SIPMessage sIPMessage) throws IOException {
        byte[] bArrEncodeAsBytes = sIPMessage.encodeAsBytes(getTransport());
        long jCurrentTimeMillis = System.currentTimeMillis();
        sendMessage(bArrEncodeAsBytes, sIPMessage instanceof SIPRequest);
        if (this.sipStack.getStackLogger().isLoggingEnabled(16)) {
            logMessage(sIPMessage, this.peerAddress, this.peerPort, jCurrentTimeMillis);
        }
    }

    @Override
    public void sendMessage(byte[] bArr, InetAddress inetAddress, int i, boolean z) throws IOException {
        if (bArr == null || inetAddress == null) {
            throw new IllegalArgumentException("Null argument");
        }
        Socket socketSendBytes = this.sipStack.ioHandler.sendBytes(this.messageProcessor.getIpAddress(), inetAddress, i, ListeningPoint.TLS, bArr, z, this);
        if (socketSendBytes != this.mySock && socketSendBytes != null) {
            try {
                if (this.mySock != null) {
                    this.mySock.close();
                }
            } catch (IOException e) {
            }
            this.mySock = socketSendBytes;
            this.myClientInputStream = this.mySock.getInputStream();
            Thread thread = new Thread(this);
            thread.setDaemon(true);
            thread.setName("TLSMessageChannelThread");
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
                this.sipStack.getStackLogger().logDebug("Encountered bad message \n" + str2);
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
                this.sipStack.getStackLogger().logError("bad message " + strEncode);
                this.sipStack.getStackLogger().logError(">>> Dropped Bad Msg");
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
                InternalErrorHandler.handleException(e);
            }
            if (!this.isCached) {
                ((TLSMessageProcessor) this.messageProcessor).cacheMessageChannel(this);
                this.isCached = true;
                this.sipStack.ioHandler.putSocket(IOHandler.makeKey(this.mySock.getInetAddress(), this.peerPort), this.mySock);
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
                this.sipStack.serverLogger.logMessage(sIPMessage, getPeerHostPort().toString(), this.messageProcessor.getIpAddress().getHostAddress() + Separators.COLON + this.messageProcessor.getPort(), false, jCurrentTimeMillis);
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
            SIPMessage sIPMessageCreateResponse = sIPRequest.createResponse(Response.SERVICE_UNAVAILABLE);
            RetryAfter retryAfter = new RetryAfter();
            try {
                retryAfter.setRetryAfter((int) (10.0d * Math.random()));
                sIPMessageCreateResponse.setHeader(retryAfter);
                sendMessage(sIPMessageCreateResponse);
            } catch (Exception e2) {
            }
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logWarning("Dropping message -- could not acquire semaphore");
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
            this.sipStack.getStackLogger().logWarning("Could not get semaphore... dropping response");
        } catch (ParseException e3) {
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
        this.tlsMessageProcessor.useCount++;
        this.isRunning = true;
        while (true) {
            try {
                try {
                    try {
                        bArr = new byte[4096];
                        i = this.myClientInputStream.read(bArr, 0, 4096);
                    } finally {
                        this.isRunning = false;
                        this.tlsMessageProcessor.remove(this);
                        this.tlsMessageProcessor.useCount--;
                        this.myParser.close();
                    }
                } catch (Exception e) {
                    InternalErrorHandler.handleException(e);
                }
                if (i == -1) {
                    break;
                } else {
                    pipeline.write(bArr, 0, i);
                }
            } catch (IOException e2) {
                try {
                    pipeline.write("\r\n\r\n".getBytes("UTF-8"));
                } catch (Exception e3) {
                }
                try {
                    if (this.sipStack.isLoggingEnabled()) {
                        this.sipStack.getStackLogger().logDebug("IOException  closing sock " + e2);
                    }
                    try {
                        if (this.sipStack.maxConnections != -1) {
                            synchronized (this.tlsMessageProcessor) {
                                this.tlsMessageProcessor.nConnections--;
                                this.tlsMessageProcessor.notify();
                            }
                        }
                        this.mySock.close();
                        pipeline.close();
                    } catch (IOException e4) {
                    }
                } catch (Exception e5) {
                }
                return;
            }
        }
        pipeline.write("\r\n\r\n".getBytes("UTF-8"));
        try {
            if (this.sipStack.maxConnections != -1) {
                synchronized (this.tlsMessageProcessor) {
                    this.tlsMessageProcessor.nConnections--;
                    this.tlsMessageProcessor.notify();
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
            this.tlsMessageProcessor.remove(this);
        }
    }

    public boolean equals(Object obj) {
        return getClass().equals(obj.getClass()) && this.mySock == ((TLSMessageChannel) obj).mySock;
    }

    @Override
    public String getKey() {
        if (this.key != null) {
            return this.key;
        }
        this.key = MessageChannel.getKey(this.peerAddress, this.peerPort, ListeningPoint.TLS);
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
        return true;
    }

    public void setHandshakeCompletedListener(HandshakeCompletedListener handshakeCompletedListener) {
        this.handshakeCompletedListener = handshakeCompletedListener;
    }

    public HandshakeCompletedListenerImpl getHandshakeCompletedListener() {
        return (HandshakeCompletedListenerImpl) this.handshakeCompletedListener;
    }
}
