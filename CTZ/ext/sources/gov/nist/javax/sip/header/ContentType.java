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

    public ContentType(String str, String str2) {
        this();
        setContentType(str, str2);
    }

    public int compareMediaRange(String str) {
        return (this.mediaRange.type + Separators.SLASH + this.mediaRange.subtype).compareToIgnoreCase(str);
    }

    @Override
    public String encodeBody() {
        return encodeBody(new StringBuffer()).toString();
    }

    @Override
    protected StringBuffer encodeBody(StringBuffer stringBuffer) {
        this.mediaRange.encode(stringBuffer);
        if (hasParameters()) {
            stringBuffer.append(Separators.SEMICOLON);
            this.parameters.encode(stringBuffer);
        }
        return stringBuffer;
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

    public void setMediaRange(MediaRange mediaRange) {
        this.mediaRange = mediaRange;
    }

    @Override
    public void setContentType(String str, String str2) {
        if (this.mediaRange == null) {
            this.mediaRange = new MediaRange();
        }
        this.mediaRange.setType(str);
        this.mediaRange.setSubtype(str2);
    }

    @Override
    public void setContentType(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("null arg");
        }
        if (this.mediaRange == null) {
            this.mediaRange = new MediaRange();
        }
        this.mediaRange.setType(str);
    }

    @Override
    public void setContentSubType(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("null arg");
        }
        if (this.mediaRange == null) {
            this.mediaRange = new MediaRange();
        }
        this.mediaRange.setSubtype(str);
    }

    @Override
    public Object clone() {
        ContentType contentType = (ContentType) super.clone();
        if (this.mediaRange != null) {
            contentType.mediaRange = (MediaRange) this.mediaRange.clone();
        }
        return contentType;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ContentTypeHeader)) {
            return false;
        }
        ContentTypeHeader contentTypeHeader = (ContentTypeHeader) obj;
        return getContentType().equalsIgnoreCase(contentTypeHeader.getContentType()) && getContentSubType().equalsIgnoreCase(contentTypeHeader.getContentSubType()) && equalParameters(contentTypeHeader);
    }
}
