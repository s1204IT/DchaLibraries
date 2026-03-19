package com.android.internal.http.multipart;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class ByteArrayPartSource implements PartSource {
    private byte[] bytes;
    private String fileName;

    public ByteArrayPartSource(String str, byte[] bArr) {
        this.fileName = str;
        this.bytes = bArr;
    }

    @Override
    public long getLength() {
        return this.bytes.length;
    }

    @Override
    public String getFileName() {
        return this.fileName;
    }

    @Override
    public InputStream createInputStream() {
        return new ByteArrayInputStream(this.bytes);
    }
}
