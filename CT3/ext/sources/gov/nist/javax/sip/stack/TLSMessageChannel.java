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

    protected TLSMessageChannel(Socket socket, SIPTransactionStack sipStack, TLSMessageProcessor msgProcessor) throws IOException {
        if (sipStack.isLoggingEnabled()) {
            sipStack.getStackLogger().logDebug("creating new TLSMessageChannel (incoming)");
            sipStack.getStackLogger().logStackTrace();
        }
        this.mySock = (SSLSocket) socket;
        if (socket instanceof SSLSocket) {
            socket.setNeedClientAuth(true);
            this.handshakeCompletedListener = new HandshakeCompletedListenerImpl(this);
            socket.addHandshakeCompletedListener(this.handshakeCompletedListener);
            socket.startHandshake();
        }
        this.peerAddress = this.mySock.getInetAddress();
        this.myAddress = msgProcessor.getIpAddress().getHostAddress();
        this.myClientInputStream = this.mySock.getInputStream();
        this.mythread = new Thread(this);
        this.mythread.setDaemon(true);
        this.mythread.setName("TLSMessageChannelThread");
        this.sipStack = sipStack;
        this.tlsMessageProcessor = msgProcessor;
        this.myPort = this.tlsMessageProcessor.getPort();
        this.peerPort = this.mySock.getPort();
        this.messageProcessor = msgProcessor;
        this.mythread.start();
    }

    protected TLSMessageChannel(InetAddress inetAddr, int port, SIPTransactionStack sipStack, TLSMessageProcessor messageProcessor) throws IOException {
        if (sipStack.isLoggingEnabled()) {
            sipStack.getStackLogger().logDebug("creating new TLSMessageChannel (outgoing)");
            sipStack.getStackLogger().logStackTrace();
        }
        this.peerAddress = inetAddr;
        this.peerPort = port;
        this.myPort = messageProcessor.getPort();
        this.peerProtocol = ListeningPoint.TLS;
        this.sipStack = sipStack;
        this.tlsMessageProcessor = messageProcessor;
        this.myAddress = messageProcessor.getIpAddress().getHostAddress();
        this.key = MessageChannel.getKey(this.peerAddress, this.peerPort, ListeningPoint.TLS);
        this.messageProcessor = messageProcessor;
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
            if (!this.sipStack.isLoggingEnabled()) {
                return;
            }
            this.sipStack.getStackLogger().logDebug("Closing message Channel " + this);
        } catch (IOException ex) {
            if (!this.sipStack.isLoggingEnabled()) {
                return;
            }
            this.sipStack.getStackLogger().logDebug("Error closing socket " + ex);
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

    private void sendMessage(byte[] msg, boolean retry) throws IOException {
        Socket sock = this.sipStack.ioHandler.sendBytes(getMessageProcessor().getIpAddress(), this.peerAddress, this.peerPort, this.peerProtocol, msg, retry, this);
        if (sock == this.mySock || sock == null) {
            return;
        }
        try {
            if (this.mySock != null) {
                this.mySock.close();
            }
        } catch (IOException e) {
        }
        this.mySock = sock;
        this.myClientInputStream = this.mySock.getInputStream();
        Thread thread = new Thread(this);
        thread.setDaemon(true);
        thread.setName("TLSMessageChannelThread");
        thread.start();
    }

    @Override
    public void sendMessage(SIPMessage sipMessage) throws IOException {
        byte[] msg = sipMessage.encodeAsBytes(getTransport());
        long time = System.currentTimeMillis();
        sendMessage(msg, sipMessage instanceof SIPRequest);
        if (!this.sipStack.getStackLogger().isLoggingEnabled(16)) {
            return;
        }
        logMessage(sipMessage, this.peerAddress, this.peerPort, time);
    }

    @Override
    public void sendMessage(byte[] message, InetAddress receiverAddress, int receiverPort, boolean retry) throws IOException {
        if (message == null || receiverAddress == null) {
            throw new IllegalArgumentException("Null argument");
        }
        Socket sock = this.sipStack.ioHandler.sendBytes(this.messageProcessor.getIpAddress(), receiverAddress, receiverPort, ListeningPoint.TLS, message, retry, this);
        if (sock == this.mySock || sock == null) {
            return;
        }
        try {
            if (this.mySock != null) {
                this.mySock.close();
            }
        } catch (IOException e) {
        }
        this.mySock = sock;
        this.myClientInputStream = this.mySock.getInputStream();
        Thread mythread = new Thread(this);
        mythread.setDaemon(true);
        mythread.setName("TLSMessageChannelThread");
        mythread.start();
    }

    @Override
    public void handleException(ParseException ex, SIPMessage sipMessage, Class hdrClass, String header, String message) throws ParseException {
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logException(ex);
        }
        if (hdrClass == null || !(hdrClass.equals(From.class) || hdrClass.equals(To.class) || hdrClass.equals(CSeq.class) || hdrClass.equals(Via.class) || hdrClass.equals(CallID.class) || hdrClass.equals(RequestLine.class) || hdrClass.equals(StatusLine.class))) {
            sipMessage.addUnparsed(header);
            return;
        }
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("Encountered bad message \n" + message);
        }
        String msgString = sipMessage.toString();
        if (msgString.startsWith("SIP/") || msgString.startsWith("ACK ")) {
            throw ex;
        }
        String badReqRes = createBadReqRes(msgString, ex);
        if (badReqRes == null) {
            if (!this.sipStack.isLoggingEnabled()) {
                throw ex;
            }
            this.sipStack.getStackLogger().logDebug("Could not formulate automatic 400 Bad Request");
            throw ex;
        }
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("Sending automatic 400 Bad Request:");
            this.sipStack.getStackLogger().logDebug(badReqRes);
        }
        try {
            sendMessage(badReqRes.getBytes(), getPeerInetAddress(), getPeerPort(), false);
            throw ex;
        } catch (IOException e) {
            this.sipStack.getStackLogger().logException(e);
            throw ex;
        }
    }

    @Override
    public void processMessage(SIPMessage sIPMessage) throws Exception {
        boolean z;
        boolean zPassToListener;
        if (sIPMessage.getFrom() == null || sIPMessage.getTo() == null || sIPMessage.getCallId() == null || sIPMessage.getCSeq() == null || sIPMessage.getViaHeaders() == null) {
            String badmsg = sIPMessage.encode();
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logError("bad message " + badmsg);
                this.sipStack.getStackLogger().logError(">>> Dropped Bad Msg");
                return;
            }
            return;
        }
        ViaList viaList = sIPMessage.getViaHeaders();
        if (sIPMessage instanceof SIPRequest) {
            Via v = (Via) viaList.getFirst();
            Hop hop = this.sipStack.addressResolver.resolveAddress(v.getHop());
            this.peerProtocol = v.getTransport();
            try {
                this.peerAddress = this.mySock.getInetAddress();
                if (v.hasParameter("rport") || !hop.getHost().equals(this.peerAddress.getHostAddress())) {
                    v.setParameter("received", this.peerAddress.getHostAddress());
                }
                v.setParameter("rport", Integer.toString(this.peerPort));
            } catch (ParseException ex) {
                InternalErrorHandler.handleException(ex);
            }
            if (!this.isCached) {
                ((TLSMessageProcessor) this.messageProcessor).cacheMessageChannel(this);
                this.isCached = true;
                String key = IOHandler.makeKey(this.mySock.getInetAddress(), this.peerPort);
                this.sipStack.ioHandler.putSocket(key, this.mySock);
            }
            long receptionTime = System.currentTimeMillis();
            if (!(sIPMessage instanceof SIPRequest)) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("----Processing Message---");
                }
                if (this.sipStack.getStackLogger().isLoggingEnabled(16)) {
                    this.sipStack.serverLogger.logMessage(sIPMessage, getPeerHostPort().toString(), this.messageProcessor.getIpAddress().getHostAddress() + Separators.COLON + this.messageProcessor.getPort(), false, receptionTime);
                }
                if (this.sipStack.getMaxMessageSize() > 0) {
                    if ((sIPMessage.getContentLength() == null ? 0 : sIPMessage.getContentLength().getContentLength()) + sIPMessage.getSize() > this.sipStack.getMaxMessageSize()) {
                        byte[] resp = sIPMessage.createResponse(Response.MESSAGE_TOO_LARGE).encodeAsBytes(getTransport());
                        sendMessage(resp, false);
                        throw new Exception("Message size exceeded");
                    }
                }
                ServerRequestInterface serverRequestInterfaceNewSIPServerRequest = this.sipStack.newSIPServerRequest(sIPMessage, this);
                if (serverRequestInterfaceNewSIPServerRequest != 0) {
                    try {
                        serverRequestInterfaceNewSIPServerRequest.processRequest(sIPMessage, this);
                        if (z) {
                            if (!zPassToListener) {
                                return;
                            } else {
                                return;
                            }
                        }
                        return;
                    } finally {
                        if (serverRequestInterfaceNewSIPServerRequest instanceof SIPTransaction) {
                            SIPServerTransaction sipServerTx = (SIPServerTransaction) serverRequestInterfaceNewSIPServerRequest;
                            if (!sipServerTx.passToListener()) {
                                ((SIPTransaction) serverRequestInterfaceNewSIPServerRequest).releaseSem();
                            }
                        }
                    }
                }
                SIPMessage response = sIPMessage.createResponse(Response.SERVICE_UNAVAILABLE);
                RetryAfter retryAfter = new RetryAfter();
                try {
                    retryAfter.setRetryAfter((int) (Math.random() * 10.0d));
                    response.setHeader(retryAfter);
                    sendMessage(response);
                } catch (Exception e) {
                }
                if (!this.sipStack.isLoggingEnabled()) {
                    return;
                }
                this.sipStack.getStackLogger().logWarning("Dropping message -- could not acquire semaphore");
                return;
            }
            SIPResponse sipResponse = (SIPResponse) sIPMessage;
            try {
                sipResponse.checkHeaders();
                if (this.sipStack.getMaxMessageSize() > 0) {
                    if ((sipResponse.getContentLength() == null ? 0 : sipResponse.getContentLength().getContentLength()) + sipResponse.getSize() > this.sipStack.getMaxMessageSize()) {
                        if (this.sipStack.isLoggingEnabled()) {
                            this.sipStack.getStackLogger().logDebug("Message size exceeded");
                            return;
                        }
                        return;
                    }
                }
                ?? NewSIPServerResponse = this.sipStack.newSIPServerResponse(sipResponse, this);
                if (NewSIPServerResponse != 0) {
                    try {
                        if ((NewSIPServerResponse instanceof SIPClientTransaction) && !NewSIPServerResponse.checkFromTag(sipResponse)) {
                            if (this.sipStack.isLoggingEnabled()) {
                                this.sipStack.getStackLogger().logError("Dropping response message with invalid tag >>> " + sipResponse);
                            }
                            if (!(NewSIPServerResponse instanceof SIPTransaction) || NewSIPServerResponse.passToListener()) {
                                return;
                            }
                            NewSIPServerResponse.releaseSem();
                            return;
                        }
                        NewSIPServerResponse.processResponse(sipResponse, this);
                        if (!(NewSIPServerResponse instanceof SIPTransaction) || ((SIPTransaction) NewSIPServerResponse).passToListener()) {
                            return;
                        }
                        ((SIPTransaction) NewSIPServerResponse).releaseSem();
                        return;
                    } catch (Throwable th) {
                        if (!(NewSIPServerResponse instanceof SIPTransaction) || ((SIPTransaction) NewSIPServerResponse).passToListener()) {
                            throw th;
                        }
                        ((SIPTransaction) NewSIPServerResponse).releaseSem();
                        throw th;
                    }
                }
                this.sipStack.getStackLogger().logWarning("Could not get semaphore... dropping response");
                return;
            } catch (ParseException e2) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logError("Dropping Badly formatted response message >>> " + sipResponse);
                    return;
                }
                return;
            }
        }
        long receptionTime2 = System.currentTimeMillis();
        if (!(sIPMessage instanceof SIPRequest)) {
        }
    }

    @Override
    public void run() {
        byte[] msg;
        int nbytes;
        Pipeline hispipe = new Pipeline(this.myClientInputStream, this.sipStack.readTimeout, this.sipStack.getTimer());
        this.myParser = new PipelinedMsgParser(this, hispipe, this.sipStack.getMaxMessageSize());
        this.myParser.processInput();
        this.tlsMessageProcessor.useCount++;
        this.isRunning = true;
        while (true) {
            try {
                try {
                    try {
                        msg = new byte[4096];
                        nbytes = this.myClientInputStream.read(msg, 0, 4096);
                    } catch (Exception ex) {
                        InternalErrorHandler.handleException(ex);
                    }
                    if (nbytes == -1) {
                        break;
                    } else {
                        hispipe.write(msg, 0, nbytes);
                    }
                } catch (IOException ex2) {
                    try {
                        hispipe.write("\r\n\r\n".getBytes("UTF-8"));
                    } catch (Exception e) {
                    }
                    try {
                        if (this.sipStack.isLoggingEnabled()) {
                            this.sipStack.getStackLogger().logDebug("IOException  closing sock " + ex2);
                        }
                        try {
                            if (this.sipStack.maxConnections != -1) {
                                synchronized (this.tlsMessageProcessor) {
                                    TLSMessageProcessor tLSMessageProcessor = this.tlsMessageProcessor;
                                    tLSMessageProcessor.nConnections--;
                                    this.tlsMessageProcessor.notify();
                                }
                            }
                            this.mySock.close();
                            hispipe.close();
                        } catch (IOException e2) {
                        }
                    } catch (Exception e3) {
                    }
                    return;
                }
            } finally {
                this.isRunning = false;
                this.tlsMessageProcessor.remove(this);
                TLSMessageProcessor tLSMessageProcessor2 = this.tlsMessageProcessor;
                tLSMessageProcessor2.useCount--;
                this.myParser.close();
            }
        }
        hispipe.write("\r\n\r\n".getBytes("UTF-8"));
        try {
            if (this.sipStack.maxConnections != -1) {
                synchronized (this.tlsMessageProcessor) {
                    TLSMessageProcessor tLSMessageProcessor3 = this.tlsMessageProcessor;
                    tLSMessageProcessor3.nConnections--;
                    this.tlsMessageProcessor.notify();
                }
            }
            hispipe.close();
            this.mySock.close();
        } catch (IOException e4) {
        }
    }

    @Override
    protected void uncache() {
        if (!this.isCached || this.isRunning) {
            return;
        }
        this.tlsMessageProcessor.remove(this);
    }

    public boolean equals(Object other) {
        if (!getClass().equals(other.getClass())) {
            return false;
        }
        TLSMessageChannel that = (TLSMessageChannel) other;
        return this.mySock == that.mySock;
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

    public void setHandshakeCompletedListener(HandshakeCompletedListener handshakeCompletedListenerImpl) {
        this.handshakeCompletedListener = handshakeCompletedListenerImpl;
    }

    public HandshakeCompletedListenerImpl getHandshakeCompletedListener() {
        return (HandshakeCompletedListenerImpl) this.handshakeCompletedListener;
    }
}
