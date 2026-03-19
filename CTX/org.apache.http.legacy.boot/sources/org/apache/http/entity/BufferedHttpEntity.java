package org.apache.http.entity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;

@Deprecated
public class BufferedHttpEntity extends HttpEntityWrapper {
    private final byte[] buffer;

    public BufferedHttpEntity(HttpEntity httpEntity) throws IOException {
        super(httpEntity);
        if (!httpEntity.isRepeatable() || httpEntity.getContentLength() < 0) {
            this.buffer = EntityUtils.toByteArray(httpEntity);
        } else {
            this.buffer = null;
        }
    }

    @Override
    public long getContentLength() {
        if (this.buffer != null) {
            return this.buffer.length;
        }
        return this.wrappedEntity.getContentLength();
    }

    @Override
    public InputStream getContent() throws IOException {
        if (this.buffer != null) {
            return new ByteArrayInputStream(this.buffer);
        }
        return this.wrappedEntity.getContent();
    }

    @Override
    public boolean isChunked() {
        return this.buffer == null && this.wrappedEntity.isChunked();
    }

    @Override
    public boolean isRepeatable() {
        return true;
    }

    @Override
    public void writeTo(OutputStream outputStream) throws IOException {
        if (outputStream == null) {
            throw new IllegalArgumentException("Output stream may not be null");
        }
        if (this.buffer != null) {
            outputStream.write(this.buffer);
        } else {
            this.wrappedEntity.writeTo(outputStream);
        }
    }

    @Override
    public boolean isStreaming() {
        return this.buffer == null && this.wrappedEntity.isStreaming();
    }
}
