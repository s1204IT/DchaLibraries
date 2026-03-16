package gov.nist.javax.sip.stack;

import gov.nist.core.InternalErrorHandler;
import gov.nist.core.Separators;
import gov.nist.core.ThreadAuditor;
import gov.nist.javax.sip.address.ParameterNames;
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
import gov.nist.javax.sip.parser.ParseExceptionListener;
import gov.nist.javax.sip.parser.StringMsgParser;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.text.ParseException;
import java.util.Hashtable;
import java.util.TimerTask;
import javax.sip.ListeningPoint;
import javax.sip.address.Hop;

public class UDPMessageChannel extends MessageChannel implements ParseExceptionListener, Runnable, RawMessageChannel {
    private DatagramPacket incomingPacket;
    private String myAddress;
    protected StringMsgParser myParser;
    protected int myPort;
    private InetAddress peerAddress;
    private InetAddress peerPacketSourceAddress;
    private int peerPacketSourcePort;
    private int peerPort;
    private String peerProtocol;
    private Hashtable<String, PingBackTimerTask> pingBackRecord;
    private long receptionTime;
    protected SIPTransactionStack sipStack;

    class PingBackTimerTask extends TimerTask {
        String ipAddress;
        int port;

        public PingBackTimerTask(String ipAddress, int port) {
            this.ipAddress = ipAddress;
            this.port = port;
            UDPMessageChannel.this.pingBackRecord.put(ipAddress + Separators.COLON + port, this);
        }

        @Override
        public void run() {
            UDPMessageChannel.this.pingBackRecord.remove(this.ipAddress + Separators.COLON + this.port);
        }

        public int hashCode() {
            return (this.ipAddress + Separators.COLON + this.port).hashCode();
        }
    }

    protected UDPMessageChannel(SIPTransactionStack stack, UDPMessageProcessor messageProcessor) {
        this.pingBackRecord = new Hashtable<>();
        this.messageProcessor = messageProcessor;
        this.sipStack = stack;
        Thread mythread = new Thread(this);
        this.myAddress = messageProcessor.getIpAddress().getHostAddress();
        this.myPort = messageProcessor.getPort();
        mythread.setName("UDPMessageChannelThread");
        mythread.setDaemon(true);
        mythread.start();
    }

    protected UDPMessageChannel(SIPTransactionStack stack, UDPMessageProcessor messageProcessor, DatagramPacket packet) {
        this.pingBackRecord = new Hashtable<>();
        this.incomingPacket = packet;
        this.messageProcessor = messageProcessor;
        this.sipStack = stack;
        this.myAddress = messageProcessor.getIpAddress().getHostAddress();
        this.myPort = messageProcessor.getPort();
        Thread mythread = new Thread(this);
        mythread.setDaemon(true);
        mythread.setName("UDPMessageChannelThread");
        mythread.start();
    }

    protected UDPMessageChannel(InetAddress targetAddr, int port, SIPTransactionStack sipStack, UDPMessageProcessor messageProcessor) {
        this.pingBackRecord = new Hashtable<>();
        this.peerAddress = targetAddr;
        this.peerPort = port;
        this.peerProtocol = ListeningPoint.UDP;
        this.messageProcessor = messageProcessor;
        this.myAddress = messageProcessor.getIpAddress().getHostAddress();
        this.myPort = messageProcessor.getPort();
        this.sipStack = sipStack;
        if (sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("Creating message channel " + targetAddr.getHostAddress() + Separators.SLASH + port);
        }
    }

    @Override
    public void run() {
        DatagramPacket packet;
        ThreadAuditor.ThreadHandle threadHandle = null;
        do {
            if (this.myParser == null) {
                this.myParser = new StringMsgParser();
                this.myParser.setParseExceptionListener(this);
            }
            if (this.sipStack.threadPoolSize != -1) {
                synchronized (((UDPMessageProcessor) this.messageProcessor).messageQueue) {
                    while (((UDPMessageProcessor) this.messageProcessor).messageQueue.isEmpty()) {
                        if (((UDPMessageProcessor) this.messageProcessor).isRunning) {
                            if (threadHandle == null) {
                                try {
                                    threadHandle = this.sipStack.getThreadAuditor().addCurrentThread();
                                } catch (InterruptedException e) {
                                    if (!((UDPMessageProcessor) this.messageProcessor).isRunning) {
                                        return;
                                    }
                                }
                            }
                            threadHandle.ping();
                            ((UDPMessageProcessor) this.messageProcessor).messageQueue.wait(threadHandle.getPingIntervalInMillisecs());
                        } else {
                            return;
                        }
                    }
                    packet = (DatagramPacket) ((UDPMessageProcessor) this.messageProcessor).messageQueue.removeFirst();
                    this.incomingPacket = packet;
                }
            } else {
                packet = this.incomingPacket;
            }
            try {
                processIncomingDataPacket(packet);
            } catch (Exception e2) {
                this.sipStack.getStackLogger().logError("Error while processing incoming UDP packet", e2);
            }
        } while (this.sipStack.threadPoolSize != -1);
    }

    private void processIncomingDataPacket(DatagramPacket packet) throws Exception {
        this.peerAddress = packet.getAddress();
        int packetLength = packet.getLength();
        byte[] bytes = packet.getData();
        byte[] msgBytes = new byte[packetLength];
        System.arraycopy(bytes, 0, msgBytes, 0, packetLength);
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("UDPMessageChannel: processIncomingDataPacket : peerAddress = " + this.peerAddress.getHostAddress() + Separators.SLASH + packet.getPort() + " Length = " + packetLength);
        }
        try {
            this.receptionTime = System.currentTimeMillis();
            SIPMessage sipMessage = this.myParser.parseSIPMessage(msgBytes);
            this.myParser = null;
            if (sipMessage == null) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("Rejecting message !  + Null message parsed.");
                }
                if (this.pingBackRecord.get(packet.getAddress().getHostAddress() + Separators.COLON + packet.getPort()) == null) {
                    byte[] retval = "\r\n\r\n".getBytes();
                    DatagramPacket keepalive = new DatagramPacket(retval, 0, retval.length, packet.getAddress(), packet.getPort());
                    ((UDPMessageProcessor) this.messageProcessor).sock.send(keepalive);
                    this.sipStack.getTimer().schedule(new PingBackTimerTask(packet.getAddress().getHostAddress(), packet.getPort()), 1000L);
                    return;
                }
                return;
            }
            ViaList viaList = sipMessage.getViaHeaders();
            if (sipMessage.getFrom() == null || sipMessage.getTo() == null || sipMessage.getCallId() == null || sipMessage.getCSeq() == null || sipMessage.getViaHeaders() == null) {
                String badmsg = new String(msgBytes);
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logError("bad message " + badmsg);
                    this.sipStack.getStackLogger().logError(">>> Dropped Bad Msg From = " + sipMessage.getFrom() + "To = " + sipMessage.getTo() + "CallId = " + sipMessage.getCallId() + "CSeq = " + sipMessage.getCSeq() + "Via = " + sipMessage.getViaHeaders());
                    return;
                }
                return;
            }
            if (sipMessage instanceof SIPRequest) {
                Via v = (Via) viaList.getFirst();
                Hop hop = this.sipStack.addressResolver.resolveAddress(v.getHop());
                this.peerPort = hop.getPort();
                this.peerProtocol = v.getTransport();
                this.peerPacketSourceAddress = packet.getAddress();
                this.peerPacketSourcePort = packet.getPort();
                try {
                    this.peerAddress = packet.getAddress();
                    boolean hasRPort = v.hasParameter("rport");
                    if (hasRPort || !hop.getHost().equals(this.peerAddress.getHostAddress())) {
                        v.setParameter("received", this.peerAddress.getHostAddress());
                    }
                    if (hasRPort) {
                        v.setParameter("rport", Integer.toString(this.peerPacketSourcePort));
                    }
                } catch (ParseException ex1) {
                    InternalErrorHandler.handleException(ex1);
                }
            } else {
                this.peerPacketSourceAddress = packet.getAddress();
                this.peerPacketSourcePort = packet.getPort();
                this.peerAddress = packet.getAddress();
                this.peerPort = packet.getPort();
                this.peerProtocol = ((Via) viaList.getFirst()).getTransport();
            }
            processMessage(sipMessage);
        } catch (ParseException ex) {
            this.myParser = null;
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("Rejecting message !  " + new String(msgBytes));
                this.sipStack.getStackLogger().logDebug("error message " + ex.getMessage());
                this.sipStack.getStackLogger().logException(ex);
            }
            String msgString = new String(msgBytes, 0, packetLength);
            if (!msgString.startsWith("SIP/") && !msgString.startsWith("ACK ")) {
                String badReqRes = createBadReqRes(msgString, ex);
                if (badReqRes != null) {
                    if (this.sipStack.isLoggingEnabled()) {
                        this.sipStack.getStackLogger().logDebug("Sending automatic 400 Bad Request:");
                        this.sipStack.getStackLogger().logDebug(badReqRes);
                    }
                    try {
                        sendMessage(badReqRes.getBytes(), this.peerAddress, packet.getPort(), ListeningPoint.UDP, false);
                        return;
                    } catch (IOException e) {
                        this.sipStack.getStackLogger().logException(e);
                        return;
                    }
                }
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("Could not formulate automatic 400 Bad Request");
                }
            }
        }
    }

    @Override
    public void processMessage(SIPMessage sipMessage) {
        if (sipMessage instanceof SIPRequest) {
            SIPRequest sipRequest = (SIPRequest) sipMessage;
            if (this.sipStack.getStackLogger().isLoggingEnabled(16)) {
                this.sipStack.serverLogger.logMessage(sipMessage, getPeerHostPort().toString(), getHost() + Separators.COLON + this.myPort, false, this.receptionTime);
            }
            ServerRequestInterface serverRequestInterfaceNewSIPServerRequest = this.sipStack.newSIPServerRequest(sipRequest, this);
            if (serverRequestInterfaceNewSIPServerRequest == 0) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logWarning("Null request interface returned -- dropping request");
                    return;
                }
                return;
            }
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("About to process " + sipRequest.getFirstLine() + Separators.SLASH + serverRequestInterfaceNewSIPServerRequest);
            }
            try {
                serverRequestInterfaceNewSIPServerRequest.processRequest(sipRequest, this);
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("Done processing " + sipRequest.getFirstLine() + Separators.SLASH + serverRequestInterfaceNewSIPServerRequest);
                    return;
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
        SIPResponse sipResponse = (SIPResponse) sipMessage;
        try {
            sipResponse.checkHeaders();
            ServerResponseInterface serverResponseInterfaceNewSIPServerResponse = this.sipStack.newSIPServerResponse(sipResponse, this);
            if (serverResponseInterfaceNewSIPServerResponse != 0) {
                try {
                    if ((serverResponseInterfaceNewSIPServerResponse instanceof SIPClientTransaction) && !((SIPClientTransaction) serverResponseInterfaceNewSIPServerResponse).checkFromTag(sipResponse)) {
                        if (this.sipStack.isLoggingEnabled()) {
                            this.sipStack.getStackLogger().logError("Dropping response message with invalid tag >>> " + sipResponse);
                        }
                        if ((serverResponseInterfaceNewSIPServerResponse instanceof SIPTransaction) && !((SIPTransaction) serverResponseInterfaceNewSIPServerResponse).passToListener()) {
                            ((SIPTransaction) serverResponseInterfaceNewSIPServerResponse).releaseSem();
                            return;
                        }
                        return;
                    }
                    serverResponseInterfaceNewSIPServerResponse.processResponse(sipResponse, this);
                    if ((serverResponseInterfaceNewSIPServerResponse instanceof SIPTransaction) && !((SIPTransaction) serverResponseInterfaceNewSIPServerResponse).passToListener()) {
                        ((SIPTransaction) serverResponseInterfaceNewSIPServerResponse).releaseSem();
                    }
                } finally {
                }
            } else if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("null sipServerResponse!");
            }
        } catch (ParseException e) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logError("Dropping Badly formatted response message >>> " + sipResponse);
            }
        }
    }

    @Override
    public void handleException(ParseException ex, SIPMessage sipMessage, Class hdrClass, String header, String message) throws ParseException {
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logException(ex);
        }
        if (hdrClass != null && (hdrClass.equals(From.class) || hdrClass.equals(To.class) || hdrClass.equals(CSeq.class) || hdrClass.equals(Via.class) || hdrClass.equals(CallID.class) || hdrClass.equals(RequestLine.class) || hdrClass.equals(StatusLine.class))) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logError("BAD MESSAGE!");
                this.sipStack.getStackLogger().logError(message);
                throw ex;
            }
            throw ex;
        }
        sipMessage.addUnparsed(header);
    }

    @Override
    public void sendMessage(SIPMessage sipMessage) throws IOException {
        if (this.sipStack.isLoggingEnabled() && this.sipStack.isLogStackTraceOnMessageSend()) {
            if (!(sipMessage instanceof SIPRequest) || ((SIPRequest) sipMessage).getRequestLine() == null) {
                this.sipStack.getStackLogger().logStackTrace(16);
            } else {
                this.sipStack.getStackLogger().logStackTrace(16);
            }
        }
        long time = System.currentTimeMillis();
        try {
            try {
                try {
                    MessageProcessor[] arr$ = this.sipStack.getMessageProcessors();
                    for (MessageProcessor messageProcessor : arr$) {
                        if (messageProcessor.getIpAddress().equals(this.peerAddress) && messageProcessor.getPort() == this.peerPort && messageProcessor.getTransport().equals(this.peerProtocol)) {
                            Object objCreateMessageChannel = messageProcessor.createMessageChannel(this.peerAddress, this.peerPort);
                            if (objCreateMessageChannel instanceof RawMessageChannel) {
                                ((RawMessageChannel) objCreateMessageChannel).processMessage(sipMessage);
                                if (this.sipStack.isLoggingEnabled()) {
                                    this.sipStack.getStackLogger().logDebug("Self routing message");
                                }
                                if (this.sipStack.getStackLogger().isLoggingEnabled(16) && !sipMessage.isNullRequest()) {
                                    logMessage(sipMessage, this.peerAddress, this.peerPort, time);
                                    return;
                                } else {
                                    if (this.sipStack.getStackLogger().isLoggingEnabled(32)) {
                                        this.sipStack.getStackLogger().logDebug("Sent EMPTY Message");
                                        return;
                                    }
                                    return;
                                }
                            }
                        }
                    }
                    byte[] msg = sipMessage.encodeAsBytes(getTransport());
                    sendMessage(msg, this.peerAddress, this.peerPort, this.peerProtocol, sipMessage instanceof SIPRequest);
                    if (this.sipStack.getStackLogger().isLoggingEnabled(16) && !sipMessage.isNullRequest()) {
                        logMessage(sipMessage, this.peerAddress, this.peerPort, time);
                    } else if (this.sipStack.getStackLogger().isLoggingEnabled(32)) {
                        this.sipStack.getStackLogger().logDebug("Sent EMPTY Message");
                    }
                } catch (Exception ex) {
                    this.sipStack.getStackLogger().logError("An exception occured while sending message", ex);
                    throw new IOException("An exception occured while sending message");
                }
            } catch (IOException ex2) {
                throw ex2;
            }
        } catch (Throwable th) {
            if (this.sipStack.getStackLogger().isLoggingEnabled(16) && !sipMessage.isNullRequest()) {
                logMessage(sipMessage, this.peerAddress, this.peerPort, time);
                throw th;
            }
            if (!this.sipStack.getStackLogger().isLoggingEnabled(32)) {
                throw th;
            }
            this.sipStack.getStackLogger().logDebug("Sent EMPTY Message");
            throw th;
        }
    }

    @Override
    protected void sendMessage(byte[] msg, InetAddress peerAddress, int peerPort, boolean reConnect) throws IOException {
        DatagramSocket sock;
        if (this.sipStack.isLoggingEnabled() && this.sipStack.isLogStackTraceOnMessageSend()) {
            this.sipStack.getStackLogger().logStackTrace(16);
        }
        if (peerPort == -1) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug(getClass().getName() + ":sendMessage: Dropping reply!");
            }
            throw new IOException("Receiver port not set ");
        }
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("sendMessage " + peerAddress.getHostAddress() + Separators.SLASH + peerPort + Separators.RETURN + "messageSize =  " + msg.length + " message = " + new String(msg));
            this.sipStack.getStackLogger().logDebug("*******************\n");
        }
        DatagramPacket reply = new DatagramPacket(msg, msg.length, peerAddress, peerPort);
        boolean created = false;
        try {
            if (this.sipStack.udpFlag) {
                sock = ((UDPMessageProcessor) this.messageProcessor).sock;
            } else {
                sock = new DatagramSocket();
                created = true;
            }
            sock.send(reply);
            if (created) {
                sock.close();
            }
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex2) {
            InternalErrorHandler.handleException(ex2);
        }
    }

    protected void sendMessage(byte[] msg, InetAddress peerAddress, int peerPort, String peerProtocol, boolean retry) throws IOException {
        DatagramSocket sock;
        if (peerPort == -1) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug(getClass().getName() + ":sendMessage: Dropping reply!");
            }
            throw new IOException("Receiver port not set ");
        }
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug(":sendMessage " + peerAddress.getHostAddress() + Separators.SLASH + peerPort + Separators.RETURN + " messageSize = " + msg.length);
        }
        if (peerProtocol.compareToIgnoreCase(ListeningPoint.UDP) == 0) {
            DatagramPacket reply = new DatagramPacket(msg, msg.length, peerAddress, peerPort);
            try {
                if (this.sipStack.udpFlag) {
                    sock = ((UDPMessageProcessor) this.messageProcessor).sock;
                } else {
                    sock = this.sipStack.getNetworkLayer().createDatagramSocket();
                }
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("sendMessage " + peerAddress.getHostAddress() + Separators.SLASH + peerPort + Separators.RETURN + new String(msg));
                }
                sock.send(reply);
                if (!this.sipStack.udpFlag) {
                    sock.close();
                    return;
                }
                return;
            } catch (IOException ex) {
                throw ex;
            } catch (Exception ex2) {
                InternalErrorHandler.handleException(ex2);
                return;
            }
        }
        Socket outputSocket = this.sipStack.ioHandler.sendBytes(this.messageProcessor.getIpAddress(), peerAddress, peerPort, ParameterNames.TCP, msg, retry, this);
        OutputStream myOutputStream = outputSocket.getOutputStream();
        myOutputStream.write(msg, 0, msg.length);
        myOutputStream.flush();
    }

    @Override
    public SIPTransactionStack getSIPStack() {
        return this.sipStack;
    }

    @Override
    public String getTransport() {
        return ParameterNames.UDP;
    }

    @Override
    public String getHost() {
        return this.messageProcessor.getIpAddress().getHostAddress();
    }

    @Override
    public int getPort() {
        return ((UDPMessageProcessor) this.messageProcessor).getPort();
    }

    public String getPeerName() {
        return this.peerAddress.getHostName();
    }

    @Override
    public String getPeerAddress() {
        return this.peerAddress.getHostAddress();
    }

    @Override
    protected InetAddress getPeerInetAddress() {
        return this.peerAddress;
    }

    public boolean equals(Object other) {
        if (other == null || !getClass().equals(other.getClass())) {
            return false;
        }
        UDPMessageChannel that = (UDPMessageChannel) other;
        return getKey().equals(that.getKey());
    }

    @Override
    public String getKey() {
        return getKey(this.peerAddress, this.peerPort, ListeningPoint.UDP);
    }

    @Override
    public int getPeerPacketSourcePort() {
        return this.peerPacketSourcePort;
    }

    @Override
    public InetAddress getPeerPacketSourceAddress() {
        return this.peerPacketSourceAddress;
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
    public boolean isReliable() {
        return false;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public int getPeerPort() {
        return this.peerPort;
    }

    @Override
    public String getPeerProtocol() {
        return this.peerProtocol;
    }

    @Override
    public void close() {
    }
}
