package com.android.org.bouncycastle.util.io;

import java.io.IOException;
import java.io.OutputStream;

public abstract class SimpleOutputStream extends OutputStream {
    @Override
    public void close() {
    }

    @Override
    public void flush() {
    }

    @Override
    public void write(int i) throws IOException {
        write(new byte[]{(byte) i}, 0, 1);
    }
}
