package gov.nist.javax.sip.message;

import gov.nist.core.Separators;
import javax.sip.header.ContentDispositionHeader;
import javax.sip.header.ContentTypeHeader;

public class ContentImpl implements Content {
    private String boundary;
    private Object content;
    private ContentDispositionHeader contentDispositionHeader;
    private ContentTypeHeader contentTypeHeader;

    public ContentImpl(String str, String str2) {
        this.content = str;
        this.boundary = str2;
    }

    @Override
    public void setContent(Object obj) {
        this.content = obj;
    }

    @Override
    public ContentTypeHeader getContentTypeHeader() {
        return this.contentTypeHeader;
    }

    @Override
    public Object getContent() {
        return this.content;
    }

    @Override
    public String toString() {
        if (this.boundary == null) {
            return this.content.toString();
        }
        if (this.contentDispositionHeader != null) {
            return "--" + this.boundary + Separators.NEWLINE + getContentTypeHeader() + getContentDispositionHeader().toString() + Separators.NEWLINE + this.content.toString();
        }
        return "--" + this.boundary + Separators.NEWLINE + getContentTypeHeader() + Separators.NEWLINE + this.content.toString();
    }

    public void setContentDispositionHeader(ContentDispositionHeader contentDispositionHeader) {
        this.contentDispositionHeader = contentDispositionHeader;
    }

    @Override
    public ContentDispositionHeader getContentDispositionHeader() {
        return this.contentDispositionHeader;
    }

    public void setContentTypeHeader(ContentTypeHeader contentTypeHeader) {
        this.contentTypeHeader = contentTypeHeader;
    }
}
