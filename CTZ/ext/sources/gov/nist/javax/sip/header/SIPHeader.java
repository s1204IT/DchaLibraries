package gov.nist.javax.sip.header;

import gov.nist.core.Separators;
import javax.sip.header.Header;

public abstract class SIPHeader extends SIPObject implements SIPHeaderNames, Header, HeaderExt {
    protected String headerName;

    protected abstract String encodeBody();

    protected SIPHeader(String str) {
        this.headerName = str;
    }

    public SIPHeader() {
    }

    public String getHeaderName() {
        return this.headerName;
    }

    public String getName() {
        return this.headerName;
    }

    public void setHeaderName(String str) {
        this.headerName = str;
    }

    public String getHeaderValue() {
        try {
            StringBuffer stringBuffer = new StringBuffer(encode());
            while (stringBuffer.length() > 0 && stringBuffer.charAt(0) != ':') {
                stringBuffer.deleteCharAt(0);
            }
            if (stringBuffer.length() > 0) {
                stringBuffer.deleteCharAt(0);
            }
            return stringBuffer.toString().trim();
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isHeaderList() {
        return false;
    }

    @Override
    public String encode() {
        return encode(new StringBuffer()).toString();
    }

    @Override
    public StringBuffer encode(StringBuffer stringBuffer) {
        stringBuffer.append(this.headerName);
        stringBuffer.append(Separators.COLON);
        stringBuffer.append(Separators.SP);
        encodeBody(stringBuffer);
        stringBuffer.append(Separators.NEWLINE);
        return stringBuffer;
    }

    protected StringBuffer encodeBody(StringBuffer stringBuffer) {
        stringBuffer.append(encodeBody());
        return stringBuffer;
    }

    @Override
    public String getValue() {
        return getHeaderValue();
    }

    public int hashCode() {
        return this.headerName.hashCode();
    }

    @Override
    public final String toString() {
        return encode();
    }
}
