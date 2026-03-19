package org.apache.http.entity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

@Deprecated
public class StringEntity extends AbstractHttpEntity implements Cloneable {
    protected final byte[] content;

    public StringEntity(String str, String str2) throws UnsupportedEncodingException {
        if (str == null) {
            throw new IllegalArgumentException("Source string may not be null");
        }
        str2 = str2 == null ? "ISO-8859-1" : str2;
        this.content = str.getBytes(str2);
        setContentType("text/plain; charset=" + str2);
    }

    public StringEntity(String str) throws UnsupportedEncodingException {
        this(str, null);
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
    public InputStream getContent() throws IOException {
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
