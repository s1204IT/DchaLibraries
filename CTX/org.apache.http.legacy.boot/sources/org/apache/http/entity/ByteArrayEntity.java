package org.apache.http.entity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Deprecated
public class ByteArrayEntity extends AbstractHttpEntity implements Cloneable {
    protected final byte[] content;

    public ByteArrayEntity(byte[] bArr) {
        if (bArr == null) {
            throw new IllegalArgumentException("Source byte array may not be null");
        }
        this.content = bArr;
    }

    @Override
    public boolean isRepeatable() {
        return true;
    }

    @Override
    public long getContentLength() {
        return this.content.length;
    }

    @Override
    public InputStream getContent() {
        return new ByteArrayInputStream(this.content);
    }

    @Override
    public void writeTo(OutputStream outputStream) throws IOException {
        if (outputStream == null) {
            throw new IllegalArgumentException("Output stream may not be null");
        }
        outputStream.write(this.content);
        outputStream.flush();
    }

    @Override
    public boolean isStreaming() {
        return false;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
