package gov.nist.javax.sip.header;

import gov.nist.core.Separators;
import java.text.ParseException;
import javax.sip.ListeningPoint;

public class Protocol extends SIPObject {
    private static final long serialVersionUID = 2216758055974073280L;
    protected String protocolName = "SIP";
    protected String protocolVersion = "2.0";
    protected String transport = ListeningPoint.UDP;

    @Override
    public String encode() {
        return encode(new StringBuffer()).toString();
    }

    @Override
    public StringBuffer encode(StringBuffer stringBuffer) {
        stringBuffer.append(this.protocolName.toUpperCase());
        stringBuffer.append(Separators.SLASH);
        stringBuffer.append(this.protocolVersion);
        stringBuffer.append(Separators.SLASH);
        stringBuffer.append(this.transport.toUpperCase());
        return stringBuffer;
    }

    public String getProtocolName() {
        return this.protocolName;
    }

    public String getProtocolVersion() {
        return this.protocolVersion;
    }

    public String getProtocol() {
        return this.protocolName + '/' + this.protocolVersion;
    }

    public void setProtocol(String str) throws ParseException {
        int iIndexOf = str.indexOf(47);
        if (iIndexOf > 0) {
            this.protocolName = str.substring(0, iIndexOf);
            this.protocolVersion = str.substring(iIndexOf + 1);
            return;
        }
        throw new ParseException("Missing '/' in protocol", 0);
    }

    public String getTransport() {
        return this.transport;
    }

    public void setProtocolName(String str) {
        this.protocolName = str;
    }

    public void setProtocolVersion(String str) {
        this.protocolVersion = str;
    }

    public void setTransport(String str) {
        this.transport = str;
    }
}
