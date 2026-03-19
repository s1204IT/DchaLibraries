package android.icu.impl;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class Trie2_32 extends Trie2 {
    Trie2_32() {
    }

    public static Trie2_32 createFromSerialized(ByteBuffer bytes) throws IOException {
        return (Trie2_32) Trie2.createFromSerialized(bytes);
    }

    @Override
    public final int get(int codePoint) {
        if (codePoint >= 0) {
            if (codePoint < 55296 || (codePoint > 56319 && codePoint <= 65535)) {
                int ix = (this.index[codePoint >> 5] << 2) + (codePoint & 31);
                int value = this.data32[ix];
                return value;
            }
            if (codePoint <= 65535) {
                int ix2 = (this.index[((codePoint - 55296) >> 5) + 2048] << 2) + (codePoint & 31);
                int value2 = this.data32[ix2];
                return value2;
            }
            if (codePoint < this.highStart) {
                int ix3 = (codePoint >> 11) + 2080;
                int value3 = this.data32[(this.index[this.index[ix3] + ((codePoint >> 5) & 63)] << 2) + (codePoint & 31)];
                return value3;
            }
            if (codePoint <= 1114111) {
                int value4 = this.data32[this.highValueIndex];
                return value4;
            }
        }
        return this.errorValue;
    }

    @Override
    public int getFromU16SingleLead(char codeUnit) {
        int ix = (this.index[codeUnit >> 5] << 2) + (codeUnit & 31);
        int value = this.data32[ix];
        return value;
    }

    public int serialize(OutputStream os) throws IOException {
        DataOutputStream dos = new DataOutputStream(os);
        int bytesWritten = serializeHeader(dos) + 0;
        for (int i = 0; i < this.dataLength; i++) {
            dos.writeInt(this.data32[i]);
        }
        return bytesWritten + (this.dataLength * 4);
    }

    public int getSerializedLength() {
        return (this.header.indexLength * 2) + 16 + (this.dataLength * 4);
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
            } else if (value == this.data32[this.highValueIndex]) {
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
                    if (this.data32[ix2] != value) {
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
