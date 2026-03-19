package org.apache.http.entity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.http.Header;
import org.apache.http.HttpEntity;

@Deprecated
public class HttpEntityWrapper implements HttpEntity {
    protected HttpEntity wrappedEntity;

    public HttpEntityWrapper(HttpEntity httpEntity) {
        if (httpEntity == null) {
            throw new IllegalArgumentException("wrapped entity must not be null");
        }
        this.wrappedEntity = httpEntity;
    }

    @Override
    public boolean isRepeatable() {
        return this.wrappedEntity.isRepeatable();
    }

    @Override
    public boolean isChunked() {
        return this.wrappedEntity.isChunked();
    }

    @Override
    public long getContentLength() {
        return this.wrappedEntity.getContentLength();
    }

    @Override
    public Header getContentType() {
        return this.wrappedEntity.getContentType();
    }

    @Override
    public Header getContentEncoding() {
        return this.wrappedEntity.getContentEncoding();
    }

    @Override
    public InputStream getContent() throws IOException {
        return this.wrappedEntity.getContent();
    }

    @Override
    public void writeTo(OutputStream outputStream) throws IOException {
        this.wrappedEntity.writeTo(outputStream);
    }

    @Override
    public boolean isStreaming() {
        return this.wrappedEntity.isStreaming();
    }

    @Override
    public void consumeContent() throws IOException {
        this.wrappedEntity.consumeContent();
    }
}
