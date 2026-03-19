package gov.nist.javax.sip.header.ims;

import gov.nist.core.Separators;
import gov.nist.core.Token;
import gov.nist.javax.sip.header.ParametersHeader;
import java.text.ParseException;
import javax.sip.header.ExtensionHeader;

public class PVisitedNetworkID extends ParametersHeader implements PVisitedNetworkIDHeader, SIPHeaderNamesIms, ExtensionHeader {
    private boolean isQuoted;
    private String networkID;

    public PVisitedNetworkID() {
        super("P-Visited-Network-ID");
    }

    public PVisitedNetworkID(String str) {
        super("P-Visited-Network-ID");
        setVisitedNetworkID(str);
    }

    public PVisitedNetworkID(Token token) {
        super("P-Visited-Network-ID");
        setVisitedNetworkID(token.getTokenValue());
    }

    @Override
    protected String encodeBody() {
        StringBuffer stringBuffer = new StringBuffer();
        if (getVisitedNetworkID() != null) {
            if (this.isQuoted) {
                stringBuffer.append(Separators.DOUBLE_QUOTE + getVisitedNetworkID() + Separators.DOUBLE_QUOTE);
            } else {
                stringBuffer.append(getVisitedNetworkID());
            }
        }
        if (!this.parameters.isEmpty()) {
            stringBuffer.append(Separators.SEMICOLON + this.parameters.encode());
        }
        return stringBuffer.toString();
    }

    @Override
    public void setVisitedNetworkID(String str) {
        if (str == null) {
            throw new NullPointerException(" the networkID parameter is null");
        }
        this.networkID = str;
        this.isQuoted = true;
    }

    @Override
    public void setVisitedNetworkID(Token token) {
        if (token == null) {
            throw new NullPointerException(" the networkID parameter is null");
        }
        this.networkID = token.getTokenValue();
        this.isQuoted = false;
    }

    @Override
    public String getVisitedNetworkID() {
        return this.networkID;
    }

    @Override
    public void setValue(String str) throws ParseException {
        throw new ParseException(str, 0);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PVisitedNetworkIDHeader)) {
            return false;
        }
        PVisitedNetworkIDHeader pVisitedNetworkIDHeader = (PVisitedNetworkIDHeader) obj;
        return getVisitedNetworkID().equals(pVisitedNetworkIDHeader.getVisitedNetworkID()) && equalParameters(pVisitedNetworkIDHeader);
    }

    @Override
    public Object clone() {
        PVisitedNetworkID pVisitedNetworkID = (PVisitedNetworkID) super.clone();
        if (this.networkID != null) {
            pVisitedNetworkID.networkID = this.networkID;
        }
        pVisitedNetworkID.isQuoted = this.isQuoted;
        return pVisitedNetworkID;
    }
}
