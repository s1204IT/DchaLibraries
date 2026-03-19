package org.tukaani.xz;

import java.io.InputStream;

class LZMA2Decoder extends LZMA2Coder implements FilterDecoder {
    private int dictSize;

    LZMA2Decoder(byte[] bArr) throws UnsupportedOptionsException {
        if (bArr.length != 1 || (bArr[0] & 255) > 37) {
            throw new UnsupportedOptionsException("Unsupported LZMA2 properties");
        }
        this.dictSize = 2 | (bArr[0] & 1);
        this.dictSize <<= (bArr[0] >>> 1) + 11;
    }

    @Override
    public int getMemoryUsage() {
        return LZMA2InputStream.getMemoryUsage(this.dictSize);
    }

    @Override
    public InputStream getInputStream(InputStream inputStream) {
        return new LZMA2InputStream(inputStream, this.dictSize);
    }
}
