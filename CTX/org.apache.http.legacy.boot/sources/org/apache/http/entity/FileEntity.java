package org.apache.http.entity;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Deprecated
public class FileEntity extends AbstractHttpEntity implements Cloneable {
    protected final File file;

    public FileEntity(File file, String str) {
        if (file == null) {
            throw new IllegalArgumentException("File may not be null");
        }
        this.file = file;
        setContentType(str);
    }

    @Override
    public boolean isRepeatable() {
        return true;
    }

    @Override
    public long getContentLength() {
        return this.file.length();
    }

    @Override
    public InputStream getContent() throws IOException {
        return new FileInputStream(this.file);
    }

    @Override
    public void writeTo(OutputStream outputStream) throws IOException {
        if (outputStream == null) {
            throw new IllegalArgumentException("Output stream may not be null");
        }
        FileInputStream fileInputStream = new FileInputStream(this.file);
        try {
            byte[] bArr = new byte[4096];
            while (true) {
                int i = fileInputStream.read(bArr);
                if (i != -1) {
                    outputStream.write(bArr, 0, i);
                } else {
                    outputStream.flush();
                    return;
                }
            }
        } finally {
            fileInputStream.close();
        }
    }

    @Override
    public boolean isStreaming() {
        return false;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
