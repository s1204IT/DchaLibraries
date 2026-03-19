package gov.nist.javax.sip.header;

import gov.nist.core.Separators;

public class MediaRange extends SIPObject {
    private static final long serialVersionUID = -6297125815438079210L;
    protected String subtype;
    protected String type;

    public String getType() {
        return this.type;
    }

    public String getSubtype() {
        return this.subtype;
    }

    public void setType(String str) {
        this.type = str;
    }

    public void setSubtype(String str) {
        this.subtype = str;
    }

    @Override
    public String encode() {
        return encode(new StringBuffer()).toString();
    }

    @Override
    public StringBuffer encode(StringBuffer stringBuffer) {
        stringBuffer.append(this.type);
        stringBuffer.append(Separators.SLASH);
        stringBuffer.append(this.subtype);
        return stringBuffer;
    }
}
