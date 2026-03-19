package gov.nist.javax.sip.stack;

import gov.nist.core.Host;
import gov.nist.core.HostPort;
import gov.nist.core.InternalErrorHandler;
import gov.nist.core.Separators;
import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.header.ContentLength;
import gov.nist.javax.sip.header.ContentType;
import gov.nist.javax.sip.header.ParameterNames;
import gov.nist.javax.sip.header.Via;
import gov.nist.javax.sip.message.MessageFactoryImpl;
import gov.nist.javax.sip.message.SIPMessage;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.text.ParseException;
import javax.sip.address.Hop;
import javax.sip.header.ContactHeader;
import javax.sip.header.ServerHeader;
import org.ccil.cowan.tagsoup.HTMLModels;

public abstract class MessageChannel {
    protected transient MessageProcessor messageProcessor;
    protected int useCount;

    public abstract void close();

    public abstract String getKey();

    public abstract String getPeerAddress();

    protected abstract InetAddress getPeerInetAddress();

    public abstract InetAddress getPeerPacketSourceAddress();

    public abstract int getPeerPacketSourcePort();

    public abstract int getPeerPort();

    protected abstract String getPeerProtocol();

    public abstract SIPTransactionStack getSIPStack();

    public abstract String getTransport();

    public abstract String getViaHost();

    public abstract int getViaPort();

    public abstract boolean isReliable();

    public abstract boolean isSecure();

    public abstract void sendMessage(SIPMessage sIPMessage) throws IOException;

    protected abstract void sendMessage(byte[] bArr, InetAddress inetAddress, int i, boolean z) throws IOException;

    protected void uncache() {
    }

    public String getHost() {
        return getMessageProcessor().getIpAddress().getHostAddress();
    }

    public int getPort() {
        if (this.messageProcessor != null) {
            return this.messageProcessor.getPort();
        }
        return -1;
    }

    public void sendMessage(SIPMessage sIPMessage, Hop hop) throws IOException {
        long jCurrentTimeMillis = System.currentTimeMillis();
        InetAddress byName = InetAddress.getByName(hop.getHost());
        try {
            try {
                for (MessageProcessor messageProcessor : getSIPStack().getMessageProcessors()) {
                    if (messageProcessor.getIpAddress().equals(byName) && messageProcessor.getPort() == hop.getPort() && messageProcessor.getTransport().equals(hop.getTransport())) {
                        Object objCreateMessageChannel = messageProcessor.createMessageChannel(byName, hop.getPort());
                        if (objCreateMessageChannel instanceof RawMessageChannel) {
                            ((RawMessageChannel) objCreateMessageChannel).processMessage(sIPMessage);
                            if (getSIPStack().isLoggingEnabled()) {
                                getSIPStack().getStackLogger().logDebug("Self routing message");
                            }
                            if (getSIPStack().getStackLogger().isLoggingEnabled(16)) {
                                logMessage(sIPMessage, byName, hop.getPort(), jCurrentTimeMillis);
                                return;
                            }
                            return;
                        }
                    }
                }
                sendMessage(sIPMessage.encodeAsBytes(getTransport()), byName, hop.getPort(), sIPMessage instanceof SIPRequest);
                if (getSIPStack().getStackLogger().isLoggingEnabled(16)) {
                    logMessage(sIPMessage, byName, hop.getPort(), jCurrentTimeMillis);
                }
            } catch (IOException e) {
                throw e;
            } catch (Exception e2) {
                if (getSIPStack().getStackLogger().isLoggingEnabled(4)) {
                    getSIPStack().getStackLogger().logError("Error self routing message cause by: ", e2);
                }
                throw new IOException("Error self routing message");
            }
        } finally {
        }
    }

    public void sendMessage(SIPMessage sIPMessage, InetAddress inetAddress, int i) throws IOException {
        long jCurrentTimeMillis = System.currentTimeMillis();
        sendMessage(sIPMessage.encodeAsBytes(getTransport()), inetAddress, i, sIPMessage instanceof SIPRequest);
        logMessage(sIPMessage, inetAddress, i, jCurrentTimeMillis);
    }

    public String getRawIpSourceAddress() {
        try {
            return InetAddress.getByName(getPeerAddress()).getHostAddress();
        } catch (Exception e) {
            InternalErrorHandler.handleException(e);
            return null;
        }
    }

    public static String getKey(InetAddress inetAddress, int i, String str) {
        return (str + Separators.COLON + inetAddress.getHostAddress() + Separators.COLON + i).toLowerCase();
    }

    public static String getKey(HostPort hostPort, String str) {
        return (str + Separators.COLON + hostPort.getHost().getHostname() + Separators.COLON + hostPort.getPort()).toLowerCase();
    }

    public HostPort getHostPort() {
        HostPort hostPort = new HostPort();
        hostPort.setHost(new Host(getHost()));
        hostPort.setPort(getPort());
        return hostPort;
    }

    public HostPort getPeerHostPort() {
        HostPort hostPort = new HostPort();
        hostPort.setHost(new Host(getPeerAddress()));
        hostPort.setPort(getPeerPort());
        return hostPort;
    }

    public Via getViaHeader() {
        Via via = new Via();
        try {
            via.setTransport(getTransport());
        } catch (ParseException e) {
        }
        via.setSentBy(getHostPort());
        return via;
    }

    public HostPort getViaHostPort() {
        HostPort hostPort = new HostPort();
        hostPort.setHost(new Host(getViaHost()));
        hostPort.setPort(getViaPort());
        return hostPort;
    }

    protected void logMessage(SIPMessage sIPMessage, InetAddress inetAddress, int i, long j) {
        if (!getSIPStack().getStackLogger().isLoggingEnabled(16)) {
            return;
        }
        if (i == -1) {
            i = 5060;
        }
        getSIPStack().serverLogger.logMessage(sIPMessage, getHost() + Separators.COLON + getPort(), inetAddress.getHostAddress().toString() + Separators.COLON + i, true, j);
    }

    public void logResponse(SIPResponse sIPResponse, long j, String str) {
        int peerPort = getPeerPort();
        if (peerPort == 0 && sIPResponse.getContactHeaders() != null) {
            peerPort = ((AddressImpl) ((ContactHeader) sIPResponse.getContactHeaders().getFirst()).getAddress()).getPort();
        }
        getSIPStack().serverLogger.logMessage(sIPResponse, getPeerAddress().toString() + Separators.COLON + peerPort, getHost() + Separators.COLON + getPort(), str, false, j);
    }

    protected final String createBadReqRes(String str, ParseException parseException) {
        StringBuffer stringBuffer = new StringBuffer(HTMLModels.M_FRAME);
        stringBuffer.append("SIP/2.0 400 Bad Request (" + parseException.getLocalizedMessage() + ')');
        if (!copyViaHeaders(str, stringBuffer) || !copyHeader("CSeq", str, stringBuffer) || !copyHeader("Call-ID", str, stringBuffer) || !copyHeader("From", str, stringBuffer) || !copyHeader("To", str, stringBuffer)) {
            return null;
        }
        int iIndexOf = stringBuffer.indexOf("To");
        if (iIndexOf != -1 && stringBuffer.indexOf(ParameterNames.TAG, iIndexOf) == -1) {
            stringBuffer.append(";tag=badreq");
        }
        ServerHeader defaultServerHeader = MessageFactoryImpl.getDefaultServerHeader();
        if (defaultServerHeader != null) {
            stringBuffer.append(Separators.NEWLINE + defaultServerHeader.toString());
        }
        int length = str.length();
        if (!(this instanceof UDPMessageChannel) || stringBuffer.length() + length + "Content-Type".length() + ": message/sipfrag\r\n".length() + "Content-Length".length() < 1300) {
            stringBuffer.append(Separators.NEWLINE + new ContentType("message", "sipfrag").toString());
            stringBuffer.append(Separators.NEWLINE + new ContentLength(length).toString());
            stringBuffer.append("\r\n\r\n" + str);
        } else {
            stringBuffer.append(Separators.NEWLINE + new ContentLength(0).toString());
        }
        return stringBuffer.toString();
    }

    private static final boolean copyHeader(String str, String str2, StringBuffer stringBuffer) {
        int iIndexOf;
        int iIndexOf2 = str2.indexOf(str);
        if (iIndexOf2 != -1 && (iIndexOf = str2.indexOf(Separators.NEWLINE, iIndexOf2)) != -1) {
            stringBuffer.append(str2.subSequence(iIndexOf2 - 2, iIndexOf));
            return true;
        }
        return false;
    }

    private static final boolean copyViaHeaders(String str, StringBuffer stringBuffer) {
        int iIndexOf = str.indexOf("Via");
        boolean z = false;
        while (iIndexOf != -1) {
            int iIndexOf2 = str.indexOf(Separators.NEWLINE, iIndexOf);
            if (iIndexOf2 == -1) {
                return false;
            }
            stringBuffer.append(str.subSequence(iIndexOf - 2, iIndexOf2));
            int iIndexOf3 = str.indexOf("Via", iIndexOf2);
            z = true;
            iIndexOf = iIndexOf3;
        }
        return z;
    }

    public MessageProcessor getMessageProcessor() {
        return this.messageProcessor;
    }
}
