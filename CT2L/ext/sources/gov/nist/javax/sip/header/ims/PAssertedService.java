package gov.nist.javax.sip.header.ims;

import gov.nist.core.Separators;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;
import javax.sip.header.ExtensionHeader;

public class PAssertedService extends SIPHeader implements PAssertedServiceHeader, SIPHeaderNamesIms, ExtensionHeader {
    private String subAppIds;
    private String subServiceIds;

    protected PAssertedService(String name) {
        super("P-Asserted-Service");
    }

    public PAssertedService() {
        super("P-Asserted-Service");
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
        this.subServiceIds = subservices;
    }

    @Override
    public boolean equals(Object other) {
        return (other instanceof PAssertedServiceHeader) && super.equals(other);
    }

    @Override
    public Object clone() {
        PAssertedService retval = (PAssertedService) super.clone();
        return retval;
    }
}
