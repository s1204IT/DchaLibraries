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

    public void setContentLength(long len) {
        this.length = len;
    }

    public void setContent(InputStream instream) {
        this.content = instream;
        this.contentObtained = false;
    }

    @Override
    public void writeTo(OutputStream outstream) throws IOException {
        if (outstream == null) {
            throw new IllegalArgumentException("Output stream may not be null");
        }
        InputStream instream = getContent();
        byte[] tmp = new byte[2048];
        while (true) {
            int l = instream.read(tmp);
            if (l == -1) {
                return;
            } else {
                outstream.write(tmp, 0, l);
            }
        }
    }

    @Override
    public boolean isStreaming() {
        return (this.contentObtained || this.content == null) ? false : true;
    }

    @Override
    public void consumeContent() throws IOException {
        if (this.content == null) {
            return;
        }
        this.content.close();
    }
}
