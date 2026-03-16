package gov.nist.javax.sip.header.ims;

import gov.nist.core.Separators;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;
import javax.sip.header.ExtensionHeader;

public class PPreferredService extends SIPHeader implements PPreferredServiceHeader, SIPHeaderNamesIms, ExtensionHeader {
    private String subAppIds;
    private String subServiceIds;

    protected PPreferredService(String name) {
        super("P-Preferred-Service");
    }

    public PPreferredService() {
        super("P-Preferred-Service");
    }

    @Override
    protected String encodeBody() {
        StringBuffer retval = new StringBuffer();
        retval.append(ParameterNamesIms.SERVICE_ID);
        if (this.subServiceIds != null) {
            retval.append(ParameterNamesIms.SERVICE_ID_LABEL).append(Separators.DOT);
            retval.append(getSubserviceIdentifiers());
        } else if (this.subAppIds != null) {
            retval.append(ParameterNamesIms.APPLICATION_ID_LABEL).append(Separators.DOT);
            retval.append(getApplicationIdentifiers());
        }
        return retval.toString();
    }

    @Override
    public void setValue(String value) throws ParseException {
        throw new ParseException(value, 0);
    }

    @Override
    public String getApplicationIdentifiers() {
        return this.subAppIds.charAt(0) == '.' ? this.subAppIds.substring(1) : this.subAppIds;
    }

    @Override
    public String getSubserviceIdentifiers() {
        return this.subServiceIds.charAt(0) == '.' ? this.subServiceIds.substring(1) : this.subServiceIds;
    }

    @Override
    public void setApplicationIdentifiers(String appids) {
        this.subAppIds = appids;
    }

    @Override
    public void setSubserviceIdentifiers(String subservices) {
        this.subServiceIds = Separators.DOT.concat(subservices);
    }

    @Override
    public boolean equals(Object other) {
        return (other instanceof PPreferredServiceHeader) && super.equals(other);
    }

    @Override
    public Object clone() {
        PPreferredService retval = (PPreferredService) super.clone();
        return retval;
    }
}
