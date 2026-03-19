package gov.nist.javax.sip.header.ims;

import gov.nist.core.Separators;
import gov.nist.javax.sip.header.ParametersHeader;
import java.text.ParseException;
import javax.sip.header.ExtensionHeader;

public class PAccessNetworkInfo extends ParametersHeader implements PAccessNetworkInfoHeader, ExtensionHeader {
    private String accessType;
    private Object extendAccessInfo;

    public PAccessNetworkInfo() {
        super("P-Access-Network-Info");
        this.parameters.setSeparator(Separators.SEMICOLON);
    }

    public PAccessNetworkInfo(String accessTypeVal) {
        this();
        setAccessType(accessTypeVal);
    }

    @Override
    public void setAccessType(String accessTypeVal) {
        if (accessTypeVal == null) {
            throw new NullPointerException("JAIN-SIP Exception, P-Access-Network-Info, setAccessType(), the accessType parameter is null.");
        }
        this.accessType = accessTypeVal;
    }

    @Override
    public String getAccessType() {
        return this.accessType;
    }

    @Override
    public void setCGI3GPP(String cgi) throws ParseException {
        if (cgi == null) {
            throw new NullPointerException("JAIN-SIP Exception, P-Access-Network-Info, setCGI3GPP(), the cgi parameter is null.");
        }
        setParameter(ParameterNamesIms.CGI_3GPP, cgi);
    }

    @Override
    public String getCGI3GPP() {
        return getParameter(ParameterNamesIms.CGI_3GPP);
    }

    @Override
    public void setUtranCellID3GPP(String utranCellID) throws ParseException {
        if (utranCellID == null) {
            throw new NullPointerException("JAIN-SIP Exception, P-Access-Network-Info, setUtranCellID3GPP(), the utranCellID parameter is null.");
        }
        setParameter(ParameterNamesIms.UTRAN_CELL_ID_3GPP, utranCellID);
    }

    @Override
    public String getUtranCellID3GPP() {
        return getParameter(ParameterNamesIms.UTRAN_CELL_ID_3GPP);
    }

    @Override
    public void setDSLLocation(String dslLocation) throws ParseException {
        if (dslLocation == null) {
            throw new NullPointerException("JAIN-SIP Exception, P-Access-Network-Info, setDSLLocation(), the dslLocation parameter is null.");
        }
        setParameter(ParameterNamesIms.DSL_LOCATION, dslLocation);
    }

    @Override
    public String getDSLLocation() {
        return getParameter(ParameterNamesIms.DSL_LOCATION);
    }

    @Override
    public void setCI3GPP2(String ci3Gpp2) throws ParseException {
        if (ci3Gpp2 == null) {
            throw new NullPointerException("JAIN-SIP Exception, P-Access-Network-Info, setCI3GPP2(), the ci3Gpp2 parameter is null.");
        }
        setParameter(ParameterNamesIms.CI_3GPP2, ci3Gpp2);
    }

    @Override
    public String getCI3GPP2() {
        return getParameter(ParameterNamesIms.CI_3GPP2);
    }

    @Override
    public void setParameter(String name, Object value) {
        if (!name.equalsIgnoreCase(ParameterNamesIms.CGI_3GPP) && !name.equalsIgnoreCase(ParameterNamesIms.UTRAN_CELL_ID_3GPP) && !name.equalsIgnoreCase(ParameterNamesIms.DSL_LOCATION) && !name.equalsIgnoreCase(ParameterNamesIms.CI_3GPP2)) {
            super.setParameter(name, value);
        } else {
            try {
                super.setQuotedParameter(name, value.toString());
            } catch (ParseException e) {
            }
        }
    }

    @Override
    public void setExtensionAccessInfo(Object extendAccessInfo) throws ParseException {
        if (extendAccessInfo == null) {
            throw new NullPointerException("JAIN-SIP Exception, P-Access-Network-Info, setExtendAccessInfo(), the extendAccessInfo parameter is null.");
        }
        this.extendAccessInfo = extendAccessInfo;
    }

    @Override
    public Object getExtensionAccessInfo() {
        return this.extendAccessInfo;
    }

    @Override
    protected String encodeBody() {
        StringBuffer encoding = new StringBuffer();
        if (getAccessType() != null) {
            encoding.append(getAccessType());
        }
        if (!this.parameters.isEmpty()) {
            encoding.append("; " + this.parameters.encode());
        }
        if (getExtensionAccessInfo() != null) {
            encoding.append("; " + getExtensionAccessInfo().toString());
        }
        return encoding.toString();
    }

    @Override
    public void setValue(String value) throws ParseException {
        throw new ParseException(value, 0);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof PAccessNetworkInfoHeader) {
            return super.equals(other);
        }
        return false;
    }

    @Override
    public Object clone() {
        PAccessNetworkInfo retval = (PAccessNetworkInfo) super.clone();
        return retval;
    }
}
