package gov.nist.javax.sip.header;

import javax.sip.InvalidArgumentException;
import javax.sip.header.ContentLengthHeader;

public class ContentLength extends SIPHeader implements ContentLengthHeader {
    private static final long serialVersionUID = 1187190542411037027L;
    protected Integer contentLength;

    public ContentLength() {
        super("Content-Length");
    }

    public ContentLength(int i) {
        super("Content-Length");
        this.contentLength = Integer.valueOf(i);
    }

    @Override
    public int getContentLength() {
        return this.contentLength.intValue();
    }

    @Override
    public void setContentLength(int i) throws InvalidArgumentException {
        if (i < 0) {
            throw new InvalidArgumentException("JAIN-SIP Exception, ContentLength, setContentLength(), the contentLength parameter is <0");
        }
        this.contentLength = Integer.valueOf(i);
    }

    @Override
    public String encodeBody() {
        return encodeBody(new StringBuffer()).toString();
    }

    @Override
    protected StringBuffer encodeBody(StringBuffer stringBuffer) {
        if (this.contentLength == null) {
            stringBuffer.append("0");
        } else {
            stringBuffer.append(this.contentLength.toString());
        }
        return stringBuffer;
    }

    @Override
    public boolean match(Object obj) {
        if (obj instanceof ContentLength) {
            return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof ContentLengthHeader) && getContentLength() == ((ContentLengthHeader) obj).getContentLength();
    }
}
