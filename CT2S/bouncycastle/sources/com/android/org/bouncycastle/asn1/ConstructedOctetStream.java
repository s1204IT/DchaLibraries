package com.android.org.bouncycastle.asn1;

import java.io.IOException;
import java.io.InputStream;

class ConstructedOctetStream extends InputStream {
    private InputStream _currentStream;
    private boolean _first = true;
    private final ASN1StreamParser _parser;

    ConstructedOctetStream(ASN1StreamParser parser) {
        this._parser = parser;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        ASN1OctetStringParser s;
        if (this._currentStream == null) {
            if (!this._first || (s = (ASN1OctetStringParser) this._parser.readObject()) == null) {
                return -1;
            }
            this._first = false;
            this._currentStream = s.getOctetStream();
        }
        int totalRead = 0;
        while (true) {
            int numRead = this._currentStream.read(b, off + totalRead, len - totalRead);
            if (numRead >= 0) {
                totalRead += numRead;
                if (totalRead == len) {
                    return totalRead;
                }
            } else {
                ASN1OctetStringParser aos = (ASN1OctetStringParser) this._parser.readObject();
                if (aos == null) {
                    this._currentStream = null;
                    if (totalRead < 1) {
                        totalRead = -1;
                    }
                    return totalRead;
                }
                this._currentStream = aos.getOctetStream();
            }
        }
    }

    @Override
    public int read() throws IOException {
        ASN1OctetStringParser s;
        if (this._currentStream == null) {
            if (this._first && (s = (ASN1OctetStringParser) this._parser.readObject()) != null) {
                this._first = false;
                this._currentStream = s.getOctetStream();
            }
            return -1;
        }
        while (true) {
            int b = this._currentStream.read();
            if (b < 0) {
                ASN1OctetStringParser s2 = (ASN1OctetStringParser) this._parser.readObject();
                if (s2 == null) {
                    this._currentStream = null;
                    return -1;
                }
                this._currentStream = s2.getOctetStream();
            } else {
                return b;
            }
        }
    }
}
