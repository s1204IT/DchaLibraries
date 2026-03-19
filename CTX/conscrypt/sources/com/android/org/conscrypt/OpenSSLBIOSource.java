package com.android.org.conscrypt;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

final class OpenSSLBIOSource {
    private OpenSSLBIOInputStream source;

    static OpenSSLBIOSource wrap(ByteBuffer byteBuffer) {
        return new OpenSSLBIOSource(new OpenSSLBIOInputStream(new ByteBufferInputStream(byteBuffer), false));
    }

    private OpenSSLBIOSource(OpenSSLBIOInputStream openSSLBIOInputStream) {
        this.source = openSSLBIOInputStream;
    }

    long getContext() {
        return this.source.getBioContext();
    }

    private synchronized void release() {
        if (this.source != null) {
            NativeCrypto.BIO_free_all(this.source.getBioContext());
            this.source = null;
        }
    }

    protected void finalize() throws Throwable {
        try {
            release();
        } finally {
            super.finalize();
        }
    }

    private static class ByteBufferInputStream extends InputStream {
        private final ByteBuffer source;

        ByteBufferInputStream(ByteBuffer byteBuffer) {
            this.source = byteBuffer;
        }

        @Override
        public int read() throws IOException {
            if (this.source.remaining() > 0) {
                return this.source.get();
            }
            return -1;
        }

        @Override
        public int available() throws IOException {
            return this.source.limit() - this.source.position();
        }

        @Override
        public int read(byte[] bArr) throws IOException {
            int iPosition = this.source.position();
            this.source.get(bArr);
            return this.source.position() - iPosition;
        }

        @Override
        public int read(byte[] bArr, int i, int i2) throws IOException {
            int iMin = Math.min(this.source.remaining(), i2);
            int iPosition = this.source.position();
            this.source.get(bArr, i, iMin);
            return this.source.position() - iPosition;
        }

        @Override
        public void reset() throws IOException {
            this.source.reset();
        }

        @Override
        public long skip(long j) throws IOException {
            long jPosition = this.source.position();
            this.source.position((int) (j + jPosition));
            return ((long) this.source.position()) - jPosition;
        }
    }
}
