package gov.nist.javax.sip.header;

import gov.nist.core.Separators;
import javax.sip.header.ExtensionHeader;

public class ExtensionHeaderImpl extends SIPHeader implements ExtensionHeader {
    private static final long serialVersionUID = -8693922839612081849L;
    protected String value;

    public ExtensionHeaderImpl() {
    }

    public ExtensionHeaderImpl(String str) {
        super(str);
    }

    public void setName(String str) {
        this.headerName = str;
    }

    @Override
    public void setValue(String str) {
        this.value = str;
    }

    @Override
    public String getHeaderValue() {
        if (this.value != null) {
            return this.value;
        }
        try {
            StringBuffer stringBuffer = new StringBuffer(encode());
            while (stringBuffer.length() > 0 && stringBuffer.charAt(0) != ':') {
                stringBuffer.deleteCharAt(0);
            }
            stringBuffer.deleteCharAt(0);
            this.value = stringBuffer.toString().trim();
            return this.value;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String encode() {
        StringBuffer stringBuffer = new StringBuffer(this.headerName);
        stringBuffer.append(Separators.COLON);
        stringBuffer.append(Separators.SP);
        stringBuffer.append(this.value);
        stringBuffer.append(Separators.NEWLINE);
        return stringBuffer.toString();
    }

    @Override
    public String encodeBody() {
        return getHeaderValue();
    }
}
