package com.android.org.bouncycastle.asn1;

import java.io.IOException;
import java.io.OutputStream;

public class BEROctetStringGenerator extends BERGenerator {
    public BEROctetStringGenerator(OutputStream outputStream) throws IOException {
        super(outputStream);
        writeBERHeader(36);
    }

    public BEROctetStringGenerator(OutputStream outputStream, int i, boolean z) throws IOException {
        super(outputStream, i, z);
        writeBERHeader(36);
    }

    public OutputStream getOctetOutputStream() {
        return getOctetOutputStream(new byte[1000]);
    }

    public OutputStream getOctetOutputStream(byte[] bArr) {
        return new BufferedBEROctetStream(bArr);
    }

    private class BufferedBEROctetStream extends OutputStream {
        private byte[] _buf;
        private DEROutputStream _derOut;
        private int _off = 0;

        BufferedBEROctetStream(byte[] bArr) {
            this._buf = bArr;
            this._derOut = new DEROutputStream(BEROctetStringGenerator.this._out);
        }

        @Override
        public void write(int i) throws IOException {
            byte[] bArr = this._buf;
            int i2 = this._off;
            this._off = i2 + 1;
            bArr[i2] = (byte) i;
            if (this._off == this._buf.length) {
                DEROctetString.encode(this._derOut, this._buf);
                this._off = 0;
            }
        }

        @Override
        public void write(byte[] bArr, int i, int i2) throws IOException {
            while (i2 > 0) {
                int iMin = Math.min(i2, this._buf.length - this._off);
                System.arraycopy(bArr, i, this._buf, this._off, iMin);
                this._off += iMin;
                if (this._off >= this._buf.length) {
                    DEROctetString.encode(this._derOut, this._buf);
                    this._off = 0;
                    i += iMin;
                    i2 -= iMin;
                } else {
                    return;
                }
            }
        }

        @Override
        public void close() throws IOException {
            if (this._off != 0) {
                byte[] bArr = new byte[this._off];
                System.arraycopy(this._buf, 0, bArr, 0, this._off);
                DEROctetString.encode(this._derOut, bArr);
            }
            BEROctetStringGenerator.this.writeBEREnd();
        }
    }
}
