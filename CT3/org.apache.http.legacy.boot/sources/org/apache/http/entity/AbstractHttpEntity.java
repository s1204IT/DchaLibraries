package org.apache.http.entity;

import java.io.IOException;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;

@Deprecated
public abstract class AbstractHttpEntity implements HttpEntity {
    protected boolean chunked;
    protected Header contentEncoding;
    protected Header contentType;

    protected AbstractHttpEntity() {
    }

    @Override
    public Header getContentType() {
        return this.contentType;
    }

    @Override
    public Header getContentEncoding() {
        return this.contentEncoding;
    }

    @Override
    public boolean isChunked() {
        return this.chunked;
    }

    public void setContentType(Header contentType) {
        this.contentType = contentType;
    }

    public void setContentType(String ctString) {
        BasicHeader basicHeader = null;
        if (ctString != null) {
            basicHeader = new BasicHeader(HTTP.CONTENT_TYPE, ctString);
        }
        setContentType(basicHeader);
    }

    public void setContentEncoding(Header contentEncoding) {
        this.contentEncoding = contentEncoding;
    }

    public void setContentEncoding(String ceString) {
        BasicHeader basicHeader = null;
        if (ceString != null) {
            basicHeader = new BasicHeader(HTTP.CONTENT_ENCODING, ceString);
        }
        setContentEncoding(basicHeader);
    }

    public void setChunked(boolean b) {
        this.chunked = b;
    }

    @Override
    public void consumeContent() throws UnsupportedOperationException, IOException {
        if (!isStreaming()) {
        } else {
            throw new UnsupportedOperationException("streaming entity does not implement consumeContent()");
        }
    }
}
