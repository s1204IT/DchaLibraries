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

    public void setContentType(Header header) {
        this.contentType = header;
    }

    public void setContentType(String str) {
        BasicHeader basicHeader;
        if (str != null) {
            basicHeader = new BasicHeader(HTTP.CONTENT_TYPE, str);
        } else {
            basicHeader = null;
        }
        setContentType(basicHeader);
    }

    public void setContentEncoding(Header header) {
        this.contentEncoding = header;
    }

    public void setContentEncoding(String str) {
        BasicHeader basicHeader;
        if (str != null) {
            basicHeader = new BasicHeader(HTTP.CONTENT_ENCODING, str);
        } else {
            basicHeader = null;
        }
        setContentEncoding(basicHeader);
    }

    public void setChunked(boolean z) {
        this.chunked = z;
    }

    @Override
    public void consumeContent() throws UnsupportedOperationException, IOException {
        if (isStreaming()) {
            throw new UnsupportedOperationException("streaming entity does not implement consumeContent()");
        }
    }
}
