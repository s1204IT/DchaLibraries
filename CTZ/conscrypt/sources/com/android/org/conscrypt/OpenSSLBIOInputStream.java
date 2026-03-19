package com.android.org.conscrypt;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

class OpenSSLBIOInputStream extends FilterInputStream {
    private long ctx;

    OpenSSLBIOInputStream(InputStream inputStream, boolean z) {
        super(inputStream);
        this.ctx = NativeCrypto.create_BIO_InputStream(this, z);
    }

    long getBioContext() {
        return this.ctx;
    }

    void release() {
        NativeCrypto.BIO_free_all(this.ctx);
    }

    int gets(byte[] bArr) throws IOException {
        int i;
        int i2 = 0;
        if (bArr == null || bArr.length == 0) {
            return 0;
        }
        while (i2 < bArr.length && (i = read()) != -1) {
            if (i == 10) {
                if (i2 != 0) {
                    break;
                }
            } else {
                bArr[i2] = (byte) i;
                i2++;
            }
        }
        return i2;
    }
}
