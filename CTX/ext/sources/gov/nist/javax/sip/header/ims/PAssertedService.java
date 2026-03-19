package gov.nist.javax.sip.header.ims;

import gov.nist.core.Separators;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;
import javax.sip.header.ExtensionHeader;

public class PAssertedService extends SIPHeader implements PAssertedServiceHeader, SIPHeaderNamesIms, ExtensionHeader {
    private String subAppIds;
    private String subServiceIds;

    protected PAssertedService(String str) {
        super("P-Asserted-Service");
    }

    public PAssertedService() {
        super("P-Asserted-Service");
    }

    @Override
    protected String encodeBody() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(ParameterNamesIms.SERVICE_ID);
        if (this.subServiceIds != null) {
            stringBuffer.append(ParameterNamesIms.SERVICE_ID_LABEL);
            stringBuffer.append(Separators.DOT);
            stringBuffer.append(getSubserviceIdentifiers());
        } else if (this.subAppIds != null) {
            stringBuffer.append(ParameterNamesIms.APPLICATION_ID_LABEL);
            stringBuffer.append(Separators.DOT);
            stringBuffer.append(getApplicationIdentifiers());
        }
        return stringBuffer.toString();
    }

    @Override
    public void setValue(String str) throws ParseException {
        throw new ParseException(str, 0);
    }

    @Override
    public String getApplicationIdentifiers() {
        if (this.subAppIds.charAt(0) == '.') {
            return this.subAppIds.substring(1);
        }
        return this.subAppIds;
    }

    @Override
    public String getSubserviceIdentifiers() {
        if (this.subServiceIds.charAt(0) == '.') {
            return this.subServiceIds.substring(1);
        }
        return this.subServiceIds;
    }

    @Override
    public void setApplicationIdentifiers(String str) {
        this.subAppIds = str;
    }

    @Override
    public void setSubserviceIdentifiers(String str) {
        this.subServiceIds = str;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof PAssertedServiceHeader) && super.equals(obj);
    }

    @Override
    public Object clone() {
        return (PAssertedService) super.clone();
    }
}
