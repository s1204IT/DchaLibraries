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

    public BufferedHttpEntity(HttpEntity entity) throws IOException {
        super(entity);
        if (!entity.isRepeatable() || entity.getContentLength() < 0) {
            this.buffer = EntityUtils.toByteArray(entity);
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
        if (this.buffer == null) {
            return this.wrappedEntity.isChunked();
        }
        return false;
    }

    @Override
    public boolean isRepeatable() {
        return true;
    }

    @Override
    public void writeTo(OutputStream outstream) throws IOException {
        if (outstream == null) {
            throw new IllegalArgumentException("Output stream may not be null");
        }
        if (this.buffer != null) {
            outstream.write(this.buffer);
        } else {
            this.wrappedEntity.writeTo(outstream);
        }
    }

    @Override
    public boolean isStreaming() {
        if (this.buffer == null) {
            return this.wrappedEntity.isStreaming();
        }
        return false;
    }
}
