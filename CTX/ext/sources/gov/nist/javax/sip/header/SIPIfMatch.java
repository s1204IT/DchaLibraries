package gov.nist.javax.sip.header;

import java.text.ParseException;
import javax.sip.header.ExtensionHeader;
import javax.sip.header.SIPIfMatchHeader;

public class SIPIfMatch extends SIPHeader implements SIPIfMatchHeader, ExtensionHeader {
    private static final long serialVersionUID = 3833745477828359730L;
    protected String entityTag;

    public SIPIfMatch() {
        super("SIP-If-Match");
    }

    public SIPIfMatch(String str) throws ParseException {
        this();
        setETag(str);
    }

    @Override
    public String encodeBody() {
        return this.entityTag;
    }

    @Override
    public String getETag() {
        return this.entityTag;
    }

    @Override
    public void setETag(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("JAIN-SIP Exception,SIP-If-Match, setETag(), the etag parameter is null");
        }
        this.entityTag = str;
    }

    @Override
    public void setValue(String str) throws ParseException {
        setETag(str);
    }
}
