package gov.nist.javax.sip;

import gov.nist.core.Host;
import gov.nist.core.HostPort;
import gov.nist.core.InternalErrorHandler;
import gov.nist.core.Separators;
import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.header.Contact;
import gov.nist.javax.sip.header.Via;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.stack.MessageChannel;
import gov.nist.javax.sip.stack.MessageProcessor;
import java.io.IOException;
import java.text.ParseException;
import javax.sip.ListeningPoint;
import javax.sip.SipStack;
import javax.sip.header.ContactHeader;
import javax.sip.header.ViaHeader;

public class ListeningPointImpl implements ListeningPoint, ListeningPointExt {
    protected MessageProcessor messageProcessor;
    int port;
    protected SipProviderImpl sipProvider;
    protected SipStackImpl sipStack;
    protected String transport;

    public static String makeKey(String str, int i, String str2) {
        StringBuffer stringBuffer = new StringBuffer(str);
        stringBuffer.append(Separators.COLON);
        stringBuffer.append(i);
        stringBuffer.append(Separators.SLASH);
        stringBuffer.append(str2);
        return stringBuffer.toString().toLowerCase();
    }

    protected String getKey() {
        return makeKey(getIPAddress(), this.port, this.transport);
    }

    protected void setSipProvider(SipProviderImpl sipProviderImpl) {
        this.sipProvider = sipProviderImpl;
    }

    protected void removeSipProvider() {
        this.sipProvider = null;
    }

    protected ListeningPointImpl(SipStack sipStack, int i, String str) {
        this.sipStack = (SipStackImpl) sipStack;
        this.port = i;
        this.transport = str;
    }

    public Object clone() {
        ListeningPointImpl listeningPointImpl = new ListeningPointImpl(this.sipStack, this.port, null);
        listeningPointImpl.sipStack = this.sipStack;
        return listeningPointImpl;
    }

    @Override
    public int getPort() {
        return this.messageProcessor.getPort();
    }

    @Override
    public String getTransport() {
        return this.messageProcessor.getTransport();
    }

    public SipProviderImpl getProvider() {
        return this.sipProvider;
    }

    @Override
    public String getIPAddress() {
        return this.messageProcessor.getIpAddress().getHostAddress();
    }

    @Override
    public void setSentBy(String str) throws ParseException {
        this.messageProcessor.setSentBy(str);
    }

    @Override
    public String getSentBy() {
        return this.messageProcessor.getSentBy();
    }

    public boolean isSentBySet() {
        return this.messageProcessor.isSentBySet();
    }

    public Via getViaHeader() {
        return this.messageProcessor.getViaHeader();
    }

    public MessageProcessor getMessageProcessor() {
        return this.messageProcessor;
    }

    @Override
    public ContactHeader createContactHeader() {
        try {
            String iPAddress = getIPAddress();
            int port = getPort();
            SipUri sipUri = new SipUri();
            sipUri.setHost(iPAddress);
            sipUri.setPort(port);
            sipUri.setTransportParam(this.transport);
            Contact contact = new Contact();
            AddressImpl addressImpl = new AddressImpl();
            addressImpl.setURI(sipUri);
            contact.setAddress(addressImpl);
            return contact;
        } catch (Exception e) {
            InternalErrorHandler.handleException("Unexpected exception", this.sipStack.getStackLogger());
            return null;
        }
    }

    @Override
    public void sendHeartbeat(String str, int i) throws IOException {
        HostPort hostPort = new HostPort();
        hostPort.setHost(new Host(str));
        hostPort.setPort(i);
        MessageChannel messageChannelCreateMessageChannel = this.messageProcessor.createMessageChannel(hostPort);
        SIPRequest sIPRequest = new SIPRequest();
        sIPRequest.setNullRequest();
        messageChannelCreateMessageChannel.sendMessage(sIPRequest);
    }

    @Override
    public ViaHeader createViaHeader() {
        return getViaHeader();
    }
}
