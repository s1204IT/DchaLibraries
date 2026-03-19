package gov.nist.javax.sip.header;

import gov.nist.core.Host;
import gov.nist.core.HostPort;
import gov.nist.core.NameValue;
import gov.nist.core.NameValueList;
import gov.nist.core.Separators;
import gov.nist.javax.sip.stack.HopImpl;
import java.text.ParseException;
import javax.sip.InvalidArgumentException;
import javax.sip.address.Hop;
import javax.sip.header.ViaHeader;

public class Via extends ParametersHeader implements ViaHeader, ViaHeaderExt {
    public static final String BRANCH = "branch";
    public static final String MADDR = "maddr";
    public static final String RECEIVED = "received";
    public static final String RPORT = "rport";
    public static final String TTL = "ttl";
    private static final long serialVersionUID = 5281728373401351378L;
    protected String comment;
    private boolean rPortFlag;
    protected HostPort sentBy;
    protected Protocol sentProtocol;

    public Via() {
        super("Via");
        this.rPortFlag = false;
        this.sentProtocol = new Protocol();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof ViaHeader)) {
            return false;
        }
        ViaHeader viaHeader = (ViaHeader) obj;
        return getProtocol().equalsIgnoreCase(viaHeader.getProtocol()) && getTransport().equalsIgnoreCase(viaHeader.getTransport()) && getHost().equalsIgnoreCase(viaHeader.getHost()) && getPort() == viaHeader.getPort() && equalParameters(viaHeader);
    }

    public String getProtocolVersion() {
        if (this.sentProtocol == null) {
            return null;
        }
        return this.sentProtocol.getProtocolVersion();
    }

    public Protocol getSentProtocol() {
        return this.sentProtocol;
    }

    public HostPort getSentBy() {
        return this.sentBy;
    }

    public Hop getHop() {
        return new HopImpl(this.sentBy.getHost().getHostname(), this.sentBy.getPort(), this.sentProtocol.getTransport());
    }

    public NameValueList getViaParms() {
        return this.parameters;
    }

    public String getComment() {
        return this.comment;
    }

    public boolean hasPort() {
        return getSentBy().hasPort();
    }

    public boolean hasComment() {
        return this.comment != null;
    }

    public void removePort() {
        this.sentBy.removePort();
    }

    public void removeComment() {
        this.comment = null;
    }

    public void setProtocolVersion(String str) {
        if (this.sentProtocol == null) {
            this.sentProtocol = new Protocol();
        }
        this.sentProtocol.setProtocolVersion(str);
    }

    public void setHost(Host host) {
        if (this.sentBy == null) {
            this.sentBy = new HostPort();
        }
        this.sentBy.setHost(host);
    }

    public void setSentProtocol(Protocol protocol) {
        this.sentProtocol = protocol;
    }

    public void setSentBy(HostPort hostPort) {
        this.sentBy = hostPort;
    }

    public void setComment(String str) {
        this.comment = str;
    }

    @Override
    protected String encodeBody() {
        return encodeBody(new StringBuffer()).toString();
    }

    @Override
    protected StringBuffer encodeBody(StringBuffer stringBuffer) {
        this.sentProtocol.encode(stringBuffer);
        stringBuffer.append(Separators.SP);
        this.sentBy.encode(stringBuffer);
        if (!this.parameters.isEmpty()) {
            stringBuffer.append(Separators.SEMICOLON);
            this.parameters.encode(stringBuffer);
        }
        if (this.comment != null) {
            stringBuffer.append(Separators.SP);
            stringBuffer.append(Separators.LPAREN);
            stringBuffer.append(this.comment);
            stringBuffer.append(Separators.RPAREN);
        }
        if (this.rPortFlag) {
            stringBuffer.append(";rport");
        }
        return stringBuffer;
    }

    @Override
    public void setHost(String str) throws ParseException {
        if (this.sentBy == null) {
            this.sentBy = new HostPort();
        }
        try {
            this.sentBy.setHost(new Host(str));
        } catch (Exception e) {
            throw new NullPointerException(" host parameter is null");
        }
    }

    @Override
    public String getHost() {
        Host host;
        if (this.sentBy == null || (host = this.sentBy.getHost()) == null) {
            return null;
        }
        return host.getHostname();
    }

    @Override
    public void setPort(int i) throws InvalidArgumentException {
        if (i != -1 && (i < 1 || i > 65535)) {
            throw new InvalidArgumentException("Port value out of range -1, [1..65535]");
        }
        if (this.sentBy == null) {
            this.sentBy = new HostPort();
        }
        this.sentBy.setPort(i);
    }

    @Override
    public void setRPort() {
        this.rPortFlag = true;
    }

    @Override
    public int getPort() {
        if (this.sentBy == null) {
            return -1;
        }
        return this.sentBy.getPort();
    }

    @Override
    public int getRPort() {
        String parameter = getParameter("rport");
        if (parameter != null && !parameter.equals("")) {
            return Integer.valueOf(parameter).intValue();
        }
        return -1;
    }

    @Override
    public String getTransport() {
        if (this.sentProtocol == null) {
            return null;
        }
        return this.sentProtocol.getTransport();
    }

    @Override
    public void setTransport(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("JAIN-SIP Exception, Via, setTransport(), the transport parameter is null.");
        }
        if (this.sentProtocol == null) {
            this.sentProtocol = new Protocol();
        }
        this.sentProtocol.setTransport(str);
    }

    @Override
    public String getProtocol() {
        if (this.sentProtocol == null) {
            return null;
        }
        return this.sentProtocol.getProtocol();
    }

    @Override
    public void setProtocol(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("JAIN-SIP Exception, Via, setProtocol(), the protocol parameter is null.");
        }
        if (this.sentProtocol == null) {
            this.sentProtocol = new Protocol();
        }
        this.sentProtocol.setProtocol(str);
    }

    @Override
    public int getTTL() {
        return getParameterAsInt("ttl");
    }

    @Override
    public void setTTL(int i) throws InvalidArgumentException {
        if (i < 0 && i != -1) {
            throw new InvalidArgumentException("JAIN-SIP Exception, Via, setTTL(), the ttl parameter is < 0");
        }
        setParameter(new NameValue("ttl", Integer.valueOf(i)));
    }

    @Override
    public String getMAddr() {
        return getParameter("maddr");
    }

    @Override
    public void setMAddr(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("JAIN-SIP Exception, Via, setMAddr(), the mAddr parameter is null.");
        }
        Host host = new Host();
        host.setAddress(str);
        setParameter(new NameValue("maddr", host));
    }

    @Override
    public String getReceived() {
        return getParameter("received");
    }

    @Override
    public void setReceived(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("JAIN-SIP Exception, Via, setReceived(), the received parameter is null.");
        }
        setParameter("received", str);
    }

    @Override
    public String getBranch() {
        return getParameter("branch");
    }

    @Override
    public void setBranch(String str) throws ParseException {
        if (str == null || str.length() == 0) {
            throw new NullPointerException("JAIN-SIP Exception, Via, setBranch(), the branch parameter is null or length 0.");
        }
        setParameter("branch", str);
    }

    @Override
    public Object clone() {
        Via via = (Via) super.clone();
        if (this.sentProtocol != null) {
            via.sentProtocol = (Protocol) this.sentProtocol.clone();
        }
        if (this.sentBy != null) {
            via.sentBy = (HostPort) this.sentBy.clone();
        }
        if (getRPort() != -1) {
            via.setParameter("rport", getRPort());
        }
        return via;
    }

    @Override
    public String getSentByField() {
        if (this.sentBy != null) {
            return this.sentBy.encode();
        }
        return null;
    }

    @Override
    public String getSentProtocolField() {
        if (this.sentProtocol != null) {
            return this.sentProtocol.encode();
        }
        return null;
    }
}
