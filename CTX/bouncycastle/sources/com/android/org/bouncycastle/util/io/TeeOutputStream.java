package com.android.org.bouncycastle.util.io;

import java.io.IOException;
import java.io.OutputStream;

public class TeeOutputStream extends OutputStream {
    private OutputStream output1;
    private OutputStream output2;

    public TeeOutputStream(OutputStream outputStream, OutputStream outputStream2) {
        this.output1 = outputStream;
        this.output2 = outputStream2;
    }

    @Override
    public void write(byte[] bArr) throws IOException {
        this.output1.write(bArr);
        this.output2.write(bArr);
    }

    @Override
    public void write(byte[] bArr, int i, int i2) throws IOException {
        this.output1.write(bArr, i, i2);
        this.output2.write(bArr, i, i2);
    }

    @Override
    public void write(int i) throws IOException {
        this.output1.write(i);
        this.output2.write(i);
    }

    @Override
    public void flush() throws IOException {
        this.output1.flush();
        this.output2.flush();
    }

    @Override
    public void close() throws IOException {
        this.output1.close();
        this.output2.close();
    }
}
