package com.android.org.bouncycastle.crypto.io;

import com.android.org.bouncycastle.crypto.Mac;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class MacInputStream extends FilterInputStream {
    protected Mac mac;

    public MacInputStream(InputStream inputStream, Mac mac) {
        super(inputStream);
        this.mac = mac;
    }

    @Override
    public int read() throws IOException {
        int i = this.in.read();
        if (i >= 0) {
            this.mac.update((byte) i);
        }
        return i;
    }

    @Override
    public int read(byte[] bArr, int i, int i2) throws IOException {
        int i3 = this.in.read(bArr, i, i2);
        if (i3 >= 0) {
            this.mac.update(bArr, i, i3);
        }
        return i3;
    }

    public Mac getMac() {
        return this.mac;
    }
}
