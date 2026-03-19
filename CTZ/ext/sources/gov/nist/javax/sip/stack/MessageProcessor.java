package gov.nist.javax.sip.stack;

import gov.nist.core.Host;
import gov.nist.core.HostPort;
import gov.nist.core.InternalErrorHandler;
import gov.nist.core.Separators;
import gov.nist.javax.sip.ListeningPointImpl;
import gov.nist.javax.sip.header.Via;
import java.io.IOException;
import java.net.InetAddress;
import java.text.ParseException;
import javax.sip.InvalidArgumentException;
import javax.sip.ListeningPoint;

public abstract class MessageProcessor implements Runnable {
    protected static final String IN6_ADDR_ANY = "::0";
    protected static final String IN_ADDR_ANY = "0.0.0.0";
    private InetAddress ipAddress;
    private ListeningPointImpl listeningPoint;
    private int port;
    private String savedIpAddress;
    private String sentBy;
    private HostPort sentByHostPort;
    private boolean sentBySet;
    protected SIPTransactionStack sipStack;
    protected String transport;

    public abstract MessageChannel createMessageChannel(HostPort hostPort) throws IOException;

    public abstract MessageChannel createMessageChannel(InetAddress inetAddress, int i) throws IOException;

    public abstract int getDefaultTargetPort();

    public abstract int getMaximumMessageSize();

    public abstract SIPTransactionStack getSIPStack();

    public abstract boolean inUse();

    public abstract boolean isSecure();

    @Override
    public abstract void run();

    public abstract void start() throws IOException;

    public abstract void stop();

    protected MessageProcessor(String str) {
        this.transport = str;
    }

    protected MessageProcessor(InetAddress inetAddress, int i, String str, SIPTransactionStack sIPTransactionStack) {
        this(str);
        initialize(inetAddress, i, sIPTransactionStack);
    }

    public final void initialize(InetAddress inetAddress, int i, SIPTransactionStack sIPTransactionStack) {
        this.sipStack = sIPTransactionStack;
        this.savedIpAddress = inetAddress.getHostAddress();
        this.ipAddress = inetAddress;
        this.port = i;
        this.sentByHostPort = new HostPort();
        this.sentByHostPort.setHost(new Host(inetAddress.getHostAddress()));
        this.sentByHostPort.setPort(i);
    }

    public String getTransport() {
        return this.transport;
    }

    public int getPort() {
        return this.port;
    }

    public Via getViaHeader() {
        try {
            Via via = new Via();
            if (this.sentByHostPort != null) {
                via.setSentBy(this.sentByHostPort);
                via.setTransport(getTransport());
            } else {
                Host host = new Host();
                host.setHostname(getIpAddress().getHostAddress());
                via.setHost(host);
                via.setPort(getPort());
                via.setTransport(getTransport());
            }
            return via;
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        } catch (InvalidArgumentException e2) {
            e2.printStackTrace();
            return null;
        }
    }

    public ListeningPointImpl getListeningPoint() {
        if (this.listeningPoint == null && getSIPStack().isLoggingEnabled()) {
            getSIPStack().getStackLogger().logError("getListeningPoint" + this + " returning null listeningpoint");
        }
        return this.listeningPoint;
    }

    public void setListeningPoint(ListeningPointImpl listeningPointImpl) {
        if (getSIPStack().isLoggingEnabled()) {
            getSIPStack().getStackLogger().logDebug("setListeningPoint" + this + " listeningPoint = " + listeningPointImpl);
        }
        if (listeningPointImpl.getPort() != getPort()) {
            InternalErrorHandler.handleException("lp mismatch with provider", getSIPStack().getStackLogger());
        }
        this.listeningPoint = listeningPointImpl;
    }

    public String getSavedIpAddress() {
        return this.savedIpAddress;
    }

    public InetAddress getIpAddress() {
        return this.ipAddress;
    }

    protected void setIpAddress(InetAddress inetAddress) {
        this.sentByHostPort.setHost(new Host(inetAddress.getHostAddress()));
        this.ipAddress = inetAddress;
    }

    public void setSentBy(String str) throws ParseException {
        int iIndexOf = str.indexOf(Separators.COLON);
        if (iIndexOf == -1) {
            this.sentByHostPort = new HostPort();
            this.sentByHostPort.setHost(new Host(str));
        } else {
            this.sentByHostPort = new HostPort();
            this.sentByHostPort.setHost(new Host(str.substring(0, iIndexOf)));
            try {
                this.sentByHostPort.setPort(Integer.parseInt(str.substring(iIndexOf + 1)));
            } catch (NumberFormatException e) {
                throw new ParseException("Bad format encountered at ", iIndexOf);
            }
        }
        this.sentBySet = true;
        this.sentBy = str;
    }

    public String getSentBy() {
        if (this.sentBy == null && this.sentByHostPort != null) {
            this.sentBy = this.sentByHostPort.toString();
        }
        return this.sentBy;
    }

    public boolean isSentBySet() {
        return this.sentBySet;
    }

    public static int getDefaultPort(String str) {
        return str.equalsIgnoreCase(ListeningPoint.TLS) ? 5061 : 5060;
    }
}
