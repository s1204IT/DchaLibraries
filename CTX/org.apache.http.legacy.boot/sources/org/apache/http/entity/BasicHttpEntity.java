package org.apache.http.entity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Deprecated
public class BasicHttpEntity extends AbstractHttpEntity {
    private InputStream content;
    private boolean contentObtained;
    private long length = -1;

    @Override
    public long getContentLength() {
        return this.length;
    }

    @Override
    public InputStream getContent() throws IllegalStateException {
        if (this.content == null) {
            throw new IllegalStateException("Content has not been provided");
        }
        if (this.contentObtained) {
            throw new IllegalStateException("Content has been consumed");
        }
        this.contentObtained = true;
        return this.content;
    }

    @Override
    public boolean isRepeatable() {
        return false;
    }

    public void setContentLength(long j) {
        this.length = j;
    }

    public void setContent(InputStream inputStream) {
        this.content = inputStream;
        this.contentObtained = false;
    }

    @Override
    public void writeTo(OutputStream outputStream) throws IOException {
        if (outputStream == null) {
            throw new IllegalArgumentException("Output stream may not be null");
        }
        InputStream content = getContent();
        byte[] bArr = new byte[2048];
        while (true) {
            int i = content.read(bArr);
            if (i != -1) {
                outputStream.write(bArr, 0, i);
            } else {
                return;
            }
        }
    }

    @Override
    public boolean isStreaming() {
        return (this.contentObtained || this.content == null) ? false : true;
    }

    @Override
    public void consumeContent() throws IOException {
        if (this.content != null) {
            this.content.close();
        }
    }
}
