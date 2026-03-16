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

    public PVisitedNetworkID(String networkID) {
        super("P-Visited-Network-ID");
        setVisitedNetworkID(networkID);
    }

    public PVisitedNetworkID(Token tok) {
        super("P-Visited-Network-ID");
        setVisitedNetworkID(tok.getTokenValue());
    }

    @Override
    protected String encodeBody() {
        StringBuffer retval = new StringBuffer();
        if (getVisitedNetworkID() != null) {
            if (this.isQuoted) {
                retval.append(Separators.DOUBLE_QUOTE + getVisitedNetworkID() + Separators.DOUBLE_QUOTE);
            } else {
                retval.append(getVisitedNetworkID());
            }
        }
        if (!this.parameters.isEmpty()) {
            retval.append(Separators.SEMICOLON + this.parameters.encode());
        }
        return retval.toString();
    }

    @Override
    public void setVisitedNetworkID(String networkID) {
        if (networkID == null) {
            throw new NullPointerException(" the networkID parameter is null");
        }
        this.networkID = networkID;
        this.isQuoted = true;
    }

    @Override
    public void setVisitedNetworkID(Token networkID) {
        if (networkID == null) {
            throw new NullPointerException(" the networkID parameter is null");
        }
        this.networkID = networkID.getTokenValue();
        this.isQuoted = false;
    }

    @Override
    public String getVisitedNetworkID() {
        return this.networkID;
    }

    @Override
    public void setValue(String value) throws ParseException {
        throw new ParseException(value, 0);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof PVisitedNetworkIDHeader)) {
            return false;
        }
        PVisitedNetworkIDHeader o = (PVisitedNetworkIDHeader) other;
        return getVisitedNetworkID().equals(o.getVisitedNetworkID()) && equalParameters(o);
    }

    @Override
    public Object clone() {
        PVisitedNetworkID retval = (PVisitedNetworkID) super.clone();
        if (this.networkID != null) {
            retval.networkID = this.networkID;
        }
        retval.isQuoted = this.isQuoted;
        return retval;
    }
}
