package org.apache.http.entity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Deprecated
public class InputStreamEntity extends AbstractHttpEntity {
    private static final int BUFFER_SIZE = 2048;
    private boolean consumed = false;
    private final InputStream content;
    private final long length;

    public InputStreamEntity(InputStream inputStream, long j) {
        if (inputStream == null) {
            throw new IllegalArgumentException("Source input stream may not be null");
        }
        this.content = inputStream;
        this.length = j;
    }

    @Override
    public boolean isRepeatable() {
        return false;
    }

    @Override
    public long getContentLength() {
        return this.length;
    }

    @Override
    public InputStream getContent() throws IOException {
        return this.content;
    }

    @Override
    public void writeTo(OutputStream outputStream) throws IOException {
        int i;
        if (outputStream == null) {
            throw new IllegalArgumentException("Output stream may not be null");
        }
        InputStream inputStream = this.content;
        byte[] bArr = new byte[BUFFER_SIZE];
        if (this.length < 0) {
            while (true) {
                int i2 = inputStream.read(bArr);
                if (i2 == -1) {
                    break;
                } else {
                    outputStream.write(bArr, 0, i2);
                }
            }
        } else {
            long j = this.length;
            while (j > 0 && (i = inputStream.read(bArr, 0, (int) Math.min(2048L, j))) != -1) {
                outputStream.write(bArr, 0, i);
                j -= (long) i;
            }
        }
        this.consumed = true;
    }

    @Override
    public boolean isStreaming() {
        return !this.consumed;
    }

    @Override
    public void consumeContent() throws IOException {
        this.consumed = true;
        this.content.close();
    }
}
