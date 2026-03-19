package gov.nist.javax.sip.header;

import java.text.ParseException;
import javax.sip.header.ExtensionHeader;
import javax.sip.header.SIPETagHeader;

public class SIPETag extends SIPHeader implements SIPETagHeader, ExtensionHeader {
    private static final long serialVersionUID = 3837543366074322107L;
    protected String entityTag;

    public SIPETag() {
        super("SIP-ETag");
    }

    public SIPETag(String str) throws ParseException {
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
            throw new NullPointerException("JAIN-SIP Exception,SIP-ETag, setETag(), the etag parameter is null");
        }
        this.entityTag = str;
    }

    @Override
    public void setValue(String str) throws ParseException {
        setETag(str);
    }
}
