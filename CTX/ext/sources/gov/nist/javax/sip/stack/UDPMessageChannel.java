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

        public PingBackTimerTask(String str, int i) {
            this.ipAddress = str;
            this.port = i;
            UDPMessageChannel.this.pingBackRecord.put(str + Separators.COLON + i, this);
        }

        @Override
        public void run() {
            UDPMessageChannel.this.pingBackRecord.remove(this.ipAddress + Separators.COLON + this.port);
        }

        public int hashCode() {
            return (this.ipAddress + Separators.COLON + this.port).hashCode();
        }
    }

    protected UDPMessageChannel(SIPTransactionStack sIPTransactionStack, UDPMessageProcessor uDPMessageProcessor) {
        this.pingBackRecord = new Hashtable<>();
        this.messageProcessor = uDPMessageProcessor;
        this.sipStack = sIPTransactionStack;
        Thread thread = new Thread(this);
        this.myAddress = uDPMessageProcessor.getIpAddress().getHostAddress();
        this.myPort = uDPMessageProcessor.getPort();
        thread.setName("UDPMessageChannelThread");
        thread.setDaemon(true);
        thread.start();
    }

    protected UDPMessageChannel(SIPTransactionStack sIPTransactionStack, UDPMessageProcessor uDPMessageProcessor, DatagramPacket datagramPacket) {
        this.pingBackRecord = new Hashtable<>();
        this.incomingPacket = datagramPacket;
        this.messageProcessor = uDPMessageProcessor;
        this.sipStack = sIPTransactionStack;
        this.myAddress = uDPMessageProcessor.getIpAddress().getHostAddress();
        this.myPort = uDPMessageProcessor.getPort();
        Thread thread = new Thread(this);
        thread.setDaemon(true);
        thread.setName("UDPMessageChannelThread");
        thread.start();
    }

    protected UDPMessageChannel(InetAddress inetAddress, int i, SIPTransactionStack sIPTransactionStack, UDPMessageProcessor uDPMessageProcessor) {
        this.pingBackRecord = new Hashtable<>();
        this.peerAddress = inetAddress;
        this.peerPort = i;
        this.peerProtocol = ListeningPoint.UDP;
        this.messageProcessor = uDPMessageProcessor;
        this.myAddress = uDPMessageProcessor.getIpAddress().getHostAddress();
        this.myPort = uDPMessageProcessor.getPort();
        this.sipStack = sIPTransactionStack;
        if (sIPTransactionStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("Creating message channel " + inetAddress.getHostAddress() + Separators.SLASH + i);
        }
    }

    @Override
    public void run() {
        DatagramPacket datagramPacket;
        ThreadAuditor.ThreadHandle threadHandleAddCurrentThread = null;
        do {
            if (this.myParser == null) {
                this.myParser = new StringMsgParser();
                this.myParser.setParseExceptionListener(this);
            }
            if (this.sipStack.threadPoolSize != -1) {
                synchronized (((UDPMessageProcessor) this.messageProcessor).messageQueue) {
                    while (((UDPMessageProcessor) this.messageProcessor).messageQueue.isEmpty()) {
                        if (!((UDPMessageProcessor) this.messageProcessor).isRunning) {
                            return;
                        }
                        if (threadHandleAddCurrentThread == null) {
                            try {
                                threadHandleAddCurrentThread = this.sipStack.getThreadAuditor().addCurrentThread();
                            } catch (InterruptedException e) {
                                if (!((UDPMessageProcessor) this.messageProcessor).isRunning) {
                                    return;
                                }
                            }
                        }
                        threadHandleAddCurrentThread.ping();
                        ((UDPMessageProcessor) this.messageProcessor).messageQueue.wait(threadHandleAddCurrentThread.getPingIntervalInMillisecs());
                    }
                    datagramPacket = (DatagramPacket) ((UDPMessageProcessor) this.messageProcessor).messageQueue.removeFirst();
                    this.incomingPacket = datagramPacket;
                }
            } else {
                datagramPacket = this.incomingPacket;
            }
            try {
                processIncomingDataPacket(datagramPacket);
            } catch (Exception e2) {
                this.sipStack.getStackLogger().logError("Error while processing incoming UDP packet", e2);
            }
        } while (this.sipStack.threadPoolSize != -1);
    }

    private void processIncomingDataPacket(DatagramPacket datagramPacket) throws Exception {
        this.peerAddress = datagramPacket.getAddress();
        int length = datagramPacket.getLength();
        byte[] bArr = new byte[length];
        System.arraycopy(datagramPacket.getData(), 0, bArr, 0, length);
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("UDPMessageChannel: processIncomingDataPacket : peerAddress = " + this.peerAddress.getHostAddress() + Separators.SLASH + datagramPacket.getPort() + " Length = " + length);
        }
        try {
            this.receptionTime = System.currentTimeMillis();
            SIPMessage sIPMessage = this.myParser.parseSIPMessage(bArr);
            this.myParser = null;
            if (sIPMessage == null) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("Rejecting message !  + Null message parsed.");
                }
                if (this.pingBackRecord.get(datagramPacket.getAddress().getHostAddress() + Separators.COLON + datagramPacket.getPort()) == null) {
                    byte[] bytes = "\r\n\r\n".getBytes();
                    ((UDPMessageProcessor) this.messageProcessor).sock.send(new DatagramPacket(bytes, 0, bytes.length, datagramPacket.getAddress(), datagramPacket.getPort()));
                    this.sipStack.getTimer().schedule(new PingBackTimerTask(datagramPacket.getAddress().getHostAddress(), datagramPacket.getPort()), 1000L);
                    return;
                }
                return;
            }
            ViaList viaHeaders = sIPMessage.getViaHeaders();
            if (sIPMessage.getFrom() == null || sIPMessage.getTo() == null || sIPMessage.getCallId() == null || sIPMessage.getCSeq() == null || sIPMessage.getViaHeaders() == null) {
                String str = new String(bArr);
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logError("bad message " + str);
                    this.sipStack.getStackLogger().logError(">>> Dropped Bad Msg From = " + sIPMessage.getFrom() + "To = " + sIPMessage.getTo() + "CallId = " + sIPMessage.getCallId() + "CSeq = " + sIPMessage.getCSeq() + "Via = " + sIPMessage.getViaHeaders());
                    return;
                }
                return;
            }
            if (sIPMessage instanceof SIPRequest) {
                Via via = (Via) viaHeaders.getFirst();
                Hop hopResolveAddress = this.sipStack.addressResolver.resolveAddress(via.getHop());
                this.peerPort = hopResolveAddress.getPort();
                this.peerProtocol = via.getTransport();
                this.peerPacketSourceAddress = datagramPacket.getAddress();
                this.peerPacketSourcePort = datagramPacket.getPort();
                try {
                    this.peerAddress = datagramPacket.getAddress();
                    boolean zHasParameter = via.hasParameter("rport");
                    if (zHasParameter || !hopResolveAddress.getHost().equals(this.peerAddress.getHostAddress())) {
                        via.setParameter("received", this.peerAddress.getHostAddress());
                    }
                    if (zHasParameter) {
                        via.setParameter("rport", Integer.toString(this.peerPacketSourcePort));
                    }
                } catch (ParseException e) {
                    InternalErrorHandler.handleException(e);
                }
            } else {
                this.peerPacketSourceAddress = datagramPacket.getAddress();
                this.peerPacketSourcePort = datagramPacket.getPort();
                this.peerAddress = datagramPacket.getAddress();
                this.peerPort = datagramPacket.getPort();
                this.peerProtocol = ((Via) viaHeaders.getFirst()).getTransport();
            }
            processMessage(sIPMessage);
        } catch (ParseException e2) {
            this.myParser = null;
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("Rejecting message !  " + new String(bArr));
                this.sipStack.getStackLogger().logDebug("error message " + e2.getMessage());
                this.sipStack.getStackLogger().logException(e2);
            }
            String str2 = new String(bArr, 0, length);
            if (!str2.startsWith("SIP/") && !str2.startsWith("ACK ")) {
                String strCreateBadReqRes = createBadReqRes(str2, e2);
                if (strCreateBadReqRes != null) {
                    if (this.sipStack.isLoggingEnabled()) {
                        this.sipStack.getStackLogger().logDebug("Sending automatic 400 Bad Request:");
                        this.sipStack.getStackLogger().logDebug(strCreateBadReqRes);
                    }
                    try {
                        sendMessage(strCreateBadReqRes.getBytes(), this.peerAddress, datagramPacket.getPort(), ListeningPoint.UDP, false);
                        return;
                    } catch (IOException e3) {
                        this.sipStack.getStackLogger().logException(e3);
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
    public void processMessage(SIPMessage sIPMessage) {
        boolean z;
        boolean zPassToListener;
        if (sIPMessage instanceof SIPRequest) {
            SIPRequest sIPRequest = (SIPRequest) sIPMessage;
            if (this.sipStack.getStackLogger().isLoggingEnabled(16)) {
                this.sipStack.serverLogger.logMessage(sIPMessage, getPeerHostPort().toString(), getHost() + Separators.COLON + this.myPort, false, this.receptionTime);
            }
            ServerRequestInterface serverRequestInterfaceNewSIPServerRequest = this.sipStack.newSIPServerRequest(sIPRequest, this);
            if (serverRequestInterfaceNewSIPServerRequest == 0) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logWarning("Null request interface returned -- dropping request");
                    return;
                }
                return;
            }
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("About to process " + sIPRequest.getFirstLine() + Separators.SLASH + serverRequestInterfaceNewSIPServerRequest);
            }
            try {
                serverRequestInterfaceNewSIPServerRequest.processRequest(sIPRequest, this);
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("Done processing " + sIPRequest.getFirstLine() + Separators.SLASH + serverRequestInterfaceNewSIPServerRequest);
                    return;
                }
                return;
            } finally {
                if ((serverRequestInterfaceNewSIPServerRequest instanceof SIPTransaction) && !((SIPServerTransaction) serverRequestInterfaceNewSIPServerRequest).passToListener()) {
                    ((SIPTransaction) serverRequestInterfaceNewSIPServerRequest).releaseSem();
                }
            }
        }
        SIPResponse sIPResponse = (SIPResponse) sIPMessage;
        try {
            sIPResponse.checkHeaders();
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
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("null sipServerResponse!");
            }
        } catch (ParseException e) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logError("Dropping Badly formatted response message >>> " + sIPResponse);
            }
        }
    }

    @Override
    public void handleException(ParseException parseException, SIPMessage sIPMessage, Class cls, String str, String str2) throws ParseException {
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logException(parseException);
        }
        if (cls != null && (cls.equals(From.class) || cls.equals(To.class) || cls.equals(CSeq.class) || cls.equals(Via.class) || cls.equals(CallID.class) || cls.equals(RequestLine.class) || cls.equals(StatusLine.class))) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logError("BAD MESSAGE!");
                this.sipStack.getStackLogger().logError(str2);
                throw parseException;
            }
            throw parseException;
        }
        sIPMessage.addUnparsed(str);
    }

    @Override
    public void sendMessage(SIPMessage sIPMessage) throws IOException {
        if (this.sipStack.isLoggingEnabled() && this.sipStack.isLogStackTraceOnMessageSend()) {
            if (!(sIPMessage instanceof SIPRequest) || ((SIPRequest) sIPMessage).getRequestLine() == null) {
                this.sipStack.getStackLogger().logStackTrace(16);
            } else {
                this.sipStack.getStackLogger().logStackTrace(16);
            }
        }
        long jCurrentTimeMillis = System.currentTimeMillis();
        try {
            try {
                for (MessageProcessor messageProcessor : this.sipStack.getMessageProcessors()) {
                    if (messageProcessor.getIpAddress().equals(this.peerAddress) && messageProcessor.getPort() == this.peerPort && messageProcessor.getTransport().equals(this.peerProtocol)) {
                        Object objCreateMessageChannel = messageProcessor.createMessageChannel(this.peerAddress, this.peerPort);
                        if (objCreateMessageChannel instanceof RawMessageChannel) {
                            ((RawMessageChannel) objCreateMessageChannel).processMessage(sIPMessage);
                            if (this.sipStack.isLoggingEnabled()) {
                                this.sipStack.getStackLogger().logDebug("Self routing message");
                            }
                            if (this.sipStack.getStackLogger().isLoggingEnabled(16) && !sIPMessage.isNullRequest()) {
                                logMessage(sIPMessage, this.peerAddress, this.peerPort, jCurrentTimeMillis);
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
                sendMessage(sIPMessage.encodeAsBytes(getTransport()), this.peerAddress, this.peerPort, this.peerProtocol, sIPMessage instanceof SIPRequest);
                if (this.sipStack.getStackLogger().isLoggingEnabled(16) && !sIPMessage.isNullRequest()) {
                    logMessage(sIPMessage, this.peerAddress, this.peerPort, jCurrentTimeMillis);
                } else if (this.sipStack.getStackLogger().isLoggingEnabled(32)) {
                    this.sipStack.getStackLogger().logDebug("Sent EMPTY Message");
                }
            } catch (IOException e) {
                throw e;
            } catch (Exception e2) {
                this.sipStack.getStackLogger().logError("An exception occured while sending message", e2);
                throw new IOException("An exception occured while sending message");
            }
        } catch (Throwable th) {
            if (this.sipStack.getStackLogger().isLoggingEnabled(16) && !sIPMessage.isNullRequest()) {
                logMessage(sIPMessage, this.peerAddress, this.peerPort, jCurrentTimeMillis);
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
    protected void sendMessage(byte[] bArr, InetAddress inetAddress, int i, boolean z) throws IOException {
        DatagramSocket datagramSocket;
        if (this.sipStack.isLoggingEnabled() && this.sipStack.isLogStackTraceOnMessageSend()) {
            this.sipStack.getStackLogger().logStackTrace(16);
        }
        if (i == -1) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug(getClass().getName() + ":sendMessage: Dropping reply!");
            }
            throw new IOException("Receiver port not set ");
        }
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("sendMessage " + inetAddress.getHostAddress() + Separators.SLASH + i + "\nmessageSize =  " + bArr.length + " message = " + new String(bArr));
            this.sipStack.getStackLogger().logDebug("*******************\n");
        }
        DatagramPacket datagramPacket = new DatagramPacket(bArr, bArr.length, inetAddress, i);
        boolean z2 = false;
        try {
            if (this.sipStack.udpFlag) {
                datagramSocket = ((UDPMessageProcessor) this.messageProcessor).sock;
            } else {
                datagramSocket = new DatagramSocket();
                z2 = true;
            }
            datagramSocket.send(datagramPacket);
            if (z2) {
                datagramSocket.close();
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e2) {
            InternalErrorHandler.handleException(e2);
        }
    }

    protected void sendMessage(byte[] bArr, InetAddress inetAddress, int i, String str, boolean z) throws IOException {
        DatagramSocket datagramSocketCreateDatagramSocket;
        if (i == -1) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug(getClass().getName() + ":sendMessage: Dropping reply!");
            }
            throw new IOException("Receiver port not set ");
        }
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug(":sendMessage " + inetAddress.getHostAddress() + Separators.SLASH + i + "\n messageSize = " + bArr.length);
        }
        if (str.compareToIgnoreCase(ListeningPoint.UDP) == 0) {
            DatagramPacket datagramPacket = new DatagramPacket(bArr, bArr.length, inetAddress, i);
            try {
                if (this.sipStack.udpFlag) {
                    datagramSocketCreateDatagramSocket = ((UDPMessageProcessor) this.messageProcessor).sock;
                } else {
                    datagramSocketCreateDatagramSocket = this.sipStack.getNetworkLayer().createDatagramSocket();
                }
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("sendMessage " + inetAddress.getHostAddress() + Separators.SLASH + i + Separators.RETURN + new String(bArr));
                }
                datagramSocketCreateDatagramSocket.send(datagramPacket);
                if (!this.sipStack.udpFlag) {
                    datagramSocketCreateDatagramSocket.close();
                    return;
                }
                return;
            } catch (IOException e) {
                throw e;
            } catch (Exception e2) {
                InternalErrorHandler.handleException(e2);
                return;
            }
        }
        OutputStream outputStream = this.sipStack.ioHandler.sendBytes(this.messageProcessor.getIpAddress(), inetAddress, i, ParameterNames.TCP, bArr, z, this).getOutputStream();
        outputStream.write(bArr, 0, bArr.length);
        outputStream.flush();
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

    public boolean equals(Object obj) {
        if (obj == null || !getClass().equals(obj.getClass())) {
            return false;
        }
        return getKey().equals(((UDPMessageChannel) obj).getKey());
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
