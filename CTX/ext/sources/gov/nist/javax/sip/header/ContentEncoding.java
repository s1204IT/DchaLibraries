package gov.nist.javax.sip.header;

import java.text.ParseException;
import javax.sip.header.ContentEncodingHeader;

public class ContentEncoding extends SIPHeader implements ContentEncodingHeader {
    private static final long serialVersionUID = 2034230276579558857L;
    protected String contentEncoding;

    public ContentEncoding() {
        super("Content-Encoding");
    }

    public ContentEncoding(String str) {
        super("Content-Encoding");
        this.contentEncoding = str;
    }

    @Override
    public String encodeBody() {
        return this.contentEncoding;
    }

    @Override
    public String getEncoding() {
        return this.contentEncoding;
    }

    @Override
    public void setEncoding(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("JAIN-SIP Exception,  encoding is null");
        }
        this.contentEncoding = str;
    }
}
