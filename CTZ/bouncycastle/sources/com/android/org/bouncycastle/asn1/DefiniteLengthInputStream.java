package com.android.org.bouncycastle.asn1;

import com.android.org.bouncycastle.util.io.Streams;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

class DefiniteLengthInputStream extends LimitedInputStream {
    private static final byte[] EMPTY_BYTES = new byte[0];
    private final int _originalLength;
    private int _remaining;

    DefiniteLengthInputStream(InputStream inputStream, int i) {
        super(inputStream, i);
        if (i < 0) {
            throw new IllegalArgumentException("negative lengths not allowed");
        }
        this._originalLength = i;
        this._remaining = i;
        if (i == 0) {
            setParentEofDetect(true);
        }
    }

    @Override
    int getRemaining() {
        return this._remaining;
    }

    @Override
    public int read() throws IOException {
        if (this._remaining == 0) {
            return -1;
        }
        int i = this._in.read();
        if (i < 0) {
            throw new EOFException("DEF length " + this._originalLength + " object truncated by " + this._remaining);
        }
        int i2 = this._remaining - 1;
        this._remaining = i2;
        if (i2 == 0) {
            setParentEofDetect(true);
        }
        return i;
    }

    @Override
    public int read(byte[] bArr, int i, int i2) throws IOException {
        if (this._remaining == 0) {
            return -1;
        }
        int i3 = this._in.read(bArr, i, Math.min(i2, this._remaining));
        if (i3 < 0) {
            throw new EOFException("DEF length " + this._originalLength + " object truncated by " + this._remaining);
        }
        int i4 = this._remaining - i3;
        this._remaining = i4;
        if (i4 == 0) {
            setParentEofDetect(true);
        }
        return i3;
    }

    byte[] toByteArray() throws IOException {
        if (this._remaining == 0) {
            return EMPTY_BYTES;
        }
        byte[] bArr = new byte[this._remaining];
        int fully = this._remaining - Streams.readFully(this._in, bArr);
        this._remaining = fully;
        if (fully != 0) {
            throw new EOFException("DEF length " + this._originalLength + " object truncated by " + this._remaining);
        }
        setParentEofDetect(true);
        return bArr;
    }
}
