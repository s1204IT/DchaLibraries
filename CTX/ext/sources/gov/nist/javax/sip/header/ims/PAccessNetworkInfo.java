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

    public PAccessNetworkInfo(String str) {
        this();
        setAccessType(str);
    }

    @Override
    public void setAccessType(String str) {
        if (str == null) {
            throw new NullPointerException("JAIN-SIP Exception, P-Access-Network-Info, setAccessType(), the accessType parameter is null.");
        }
        this.accessType = str;
    }

    @Override
    public String getAccessType() {
        return this.accessType;
    }

    @Override
    public void setCGI3GPP(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("JAIN-SIP Exception, P-Access-Network-Info, setCGI3GPP(), the cgi parameter is null.");
        }
        setParameter(ParameterNamesIms.CGI_3GPP, str);
    }

    @Override
    public String getCGI3GPP() {
        return getParameter(ParameterNamesIms.CGI_3GPP);
    }

    @Override
    public void setUtranCellID3GPP(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("JAIN-SIP Exception, P-Access-Network-Info, setUtranCellID3GPP(), the utranCellID parameter is null.");
        }
        setParameter(ParameterNamesIms.UTRAN_CELL_ID_3GPP, str);
    }

    @Override
    public String getUtranCellID3GPP() {
        return getParameter(ParameterNamesIms.UTRAN_CELL_ID_3GPP);
    }

    @Override
    public void setDSLLocation(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("JAIN-SIP Exception, P-Access-Network-Info, setDSLLocation(), the dslLocation parameter is null.");
        }
        setParameter(ParameterNamesIms.DSL_LOCATION, str);
    }

    @Override
    public String getDSLLocation() {
        return getParameter(ParameterNamesIms.DSL_LOCATION);
    }

    @Override
    public void setCI3GPP2(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("JAIN-SIP Exception, P-Access-Network-Info, setCI3GPP2(), the ci3Gpp2 parameter is null.");
        }
        setParameter(ParameterNamesIms.CI_3GPP2, str);
    }

    @Override
    public String getCI3GPP2() {
        return getParameter(ParameterNamesIms.CI_3GPP2);
    }

    @Override
    public void setParameter(String str, Object obj) {
        if (str.equalsIgnoreCase(ParameterNamesIms.CGI_3GPP) || str.equalsIgnoreCase(ParameterNamesIms.UTRAN_CELL_ID_3GPP) || str.equalsIgnoreCase(ParameterNamesIms.DSL_LOCATION) || str.equalsIgnoreCase(ParameterNamesIms.CI_3GPP2)) {
            try {
                super.setQuotedParameter(str, obj.toString());
            } catch (ParseException e) {
            }
        } else {
            super.setParameter(str, obj);
        }
    }

    @Override
    public void setExtensionAccessInfo(Object obj) throws ParseException {
        if (obj == null) {
            throw new NullPointerException("JAIN-SIP Exception, P-Access-Network-Info, setExtendAccessInfo(), the extendAccessInfo parameter is null.");
        }
        this.extendAccessInfo = obj;
    }

    @Override
    public Object getExtensionAccessInfo() {
        return this.extendAccessInfo;
    }

    @Override
    protected String encodeBody() {
        StringBuffer stringBuffer = new StringBuffer();
        if (getAccessType() != null) {
            stringBuffer.append(getAccessType());
        }
        if (!this.parameters.isEmpty()) {
            stringBuffer.append("; " + this.parameters.encode());
        }
        if (getExtensionAccessInfo() != null) {
            stringBuffer.append("; " + getExtensionAccessInfo().toString());
        }
        return stringBuffer.toString();
    }

    @Override
    public void setValue(String str) throws ParseException {
        throw new ParseException(str, 0);
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof PAccessNetworkInfoHeader) && super.equals(obj);
    }

    @Override
    public Object clone() {
        return (PAccessNetworkInfo) super.clone();
    }
}
