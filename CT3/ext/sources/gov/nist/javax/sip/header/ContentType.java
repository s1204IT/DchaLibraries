package gov.nist.javax.sip.header;

import gov.nist.core.Separators;
import java.text.ParseException;
import javax.sip.header.ContentTypeHeader;

public class ContentType extends ParametersHeader implements ContentTypeHeader {
    private static final long serialVersionUID = 8475682204373446610L;
    protected MediaRange mediaRange;

    public ContentType() {
        super("Content-Type");
    }

    public ContentType(String contentType, String contentSubtype) {
        this();
        setContentType(contentType, contentSubtype);
    }

    public int compareMediaRange(String media) {
        return (this.mediaRange.type + Separators.SLASH + this.mediaRange.subtype).compareToIgnoreCase(media);
    }

    @Override
    public String encodeBody() {
        return encodeBody(new StringBuffer()).toString();
    }

    @Override
    protected StringBuffer encodeBody(StringBuffer buffer) {
        this.mediaRange.encode(buffer);
        if (hasParameters()) {
            buffer.append(Separators.SEMICOLON);
            this.parameters.encode(buffer);
        }
        return buffer;
    }

    public MediaRange getMediaRange() {
        return this.mediaRange;
    }

    public String getMediaType() {
        return this.mediaRange.type;
    }

    public String getMediaSubType() {
        return this.mediaRange.subtype;
    }

    @Override
    public String getContentSubType() {
        if (this.mediaRange == null) {
            return null;
        }
        return this.mediaRange.getSubtype();
    }

    @Override
    public String getContentType() {
        if (this.mediaRange == null) {
            return null;
        }
        return this.mediaRange.getType();
    }

    @Override
    public String getCharset() {
        return getParameter("charset");
    }

    public void setMediaRange(MediaRange m) {
        this.mediaRange = m;
    }

    @Override
    public void setContentType(String contentType, String contentSubType) {
        if (this.mediaRange == null) {
            this.mediaRange = new MediaRange();
        }
        this.mediaRange.setType(contentType);
        this.mediaRange.setSubtype(contentSubType);
    }

    @Override
    public void setContentType(String contentType) throws ParseException {
        if (contentType == null) {
            throw new NullPointerException("null arg");
        }
        if (this.mediaRange == null) {
            this.mediaRange = new MediaRange();
        }
        this.mediaRange.setType(contentType);
    }

    @Override
    public void setContentSubType(String contentType) throws ParseException {
        if (contentType == null) {
            throw new NullPointerException("null arg");
        }
        if (this.mediaRange == null) {
            this.mediaRange = new MediaRange();
        }
        this.mediaRange.setSubtype(contentType);
    }

    @Override
    public Object clone() {
        ContentType retval = (ContentType) super.clone();
        if (this.mediaRange != null) {
            retval.mediaRange = (MediaRange) this.mediaRange.clone();
        }
        return retval;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ContentTypeHeader)) {
            return false;
        }
        ContentTypeHeader o = (ContentTypeHeader) other;
        if (getContentType().equalsIgnoreCase(o.getContentType()) && getContentSubType().equalsIgnoreCase(o.getContentSubType())) {
            return equalParameters(o);
        }
        return false;
    }
}
