package android.icu.impl;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public final class Trie2_16 extends Trie2 {
    Trie2_16() {
    }

    public static Trie2_16 createFromSerialized(ByteBuffer bytes) throws IOException {
        return (Trie2_16) Trie2.createFromSerialized(bytes);
    }

    @Override
    public final int get(int codePoint) {
        if (codePoint >= 0) {
            if (codePoint < 55296 || (codePoint > 56319 && codePoint <= 65535)) {
                int ix = (this.index[codePoint >> 5] << 2) + (codePoint & 31);
                return this.index[ix];
            }
            if (codePoint <= 65535) {
                int ix2 = (this.index[((codePoint - 55296) >> 5) + 2048] << 2) + (codePoint & 31);
                return this.index[ix2];
            }
            if (codePoint < this.highStart) {
                int ix3 = (codePoint >> 11) + 2080;
                return this.index[(this.index[this.index[ix3] + ((codePoint >> 5) & 63)] << 2) + (codePoint & 31)];
            }
            if (codePoint <= 1114111) {
                return this.index[this.highValueIndex];
            }
        }
        return this.errorValue;
    }

    @Override
    public int getFromU16SingleLead(char codeUnit) {
        int ix = (this.index[codeUnit >> 5] << 2) + (codeUnit & 31);
        return this.index[ix];
    }

    public int serialize(OutputStream os) throws IOException {
        DataOutputStream dos = new DataOutputStream(os);
        int bytesWritten = serializeHeader(dos) + 0;
        for (int i = 0; i < this.dataLength; i++) {
            dos.writeChar(this.index[this.data16 + i]);
        }
        return bytesWritten + (this.dataLength * 2);
    }

    public int getSerializedLength() {
        return ((this.header.indexLength + this.dataLength) * 2) + 16;
    }

    @Override
    int rangeEnd(int startingCP, int limit, int value) {
        char c;
        int block;
        int cp = startingCP;
        loop0: while (true) {
            if (cp >= limit) {
                break;
            }
            if (cp < 55296 || (cp > 56319 && cp <= 65535)) {
                c = 0;
                block = this.index[cp >> 5] << 2;
            } else if (cp < 65535) {
                c = 2048;
                block = this.index[((cp - 55296) >> 5) + 2048] << 2;
            } else if (cp < this.highStart) {
                int ix = (cp >> 11) + 2080;
                c = this.index[ix];
                block = this.index[((cp >> 5) & 63) + c] << 2;
            } else if (value == this.index[this.highValueIndex]) {
                cp = limit;
            }
            if (c == this.index2NullOffset) {
                if (value != this.initialValue) {
                    break;
                }
                cp += 2048;
            } else if (block == this.dataNullOffset) {
                if (value != this.initialValue) {
                    break;
                }
                cp += 32;
            } else {
                int startIx = block + (cp & 31);
                int limitIx = block + 32;
                for (int ix2 = startIx; ix2 < limitIx; ix2++) {
                    if (this.index[ix2] != value) {
                        cp += ix2 - startIx;
                        break loop0;
                    }
                }
                cp += limitIx - startIx;
            }
        }
        if (cp > limit) {
            cp = limit;
        }
        return cp - 1;
    }
}
