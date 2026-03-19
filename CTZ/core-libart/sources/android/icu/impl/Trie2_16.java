package android.icu.impl;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public final class Trie2_16 extends Trie2 {
    Trie2_16() {
    }

    public static Trie2_16 createFromSerialized(ByteBuffer byteBuffer) throws IOException {
        return (Trie2_16) Trie2.createFromSerialized(byteBuffer);
    }

    @Override
    public final int get(int i) {
        if (i >= 0) {
            if (i < 55296 || (i > 56319 && i <= 65535)) {
                return this.index[(this.index[i >> 5] << 2) + (i & 31)];
            }
            if (i <= 65535) {
                return this.index[(this.index[2048 + ((i - 55296) >> 5)] << 2) + (i & 31)];
            }
            if (i < this.highStart) {
                return this.index[(this.index[this.index[2080 + (i >> 11)] + ((i >> 5) & 63)] << 2) + (i & 31)];
            }
            if (i <= 1114111) {
                return this.index[this.highValueIndex];
            }
        }
        return this.errorValue;
    }

    @Override
    public int getFromU16SingleLead(char c) {
        return this.index[(this.index[c >> 5] << 2) + (c & 31)];
    }

    public int serialize(OutputStream outputStream) throws IOException {
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
        int iSerializeHeader = serializeHeader(dataOutputStream) + 0;
        for (int i = 0; i < this.dataLength; i++) {
            dataOutputStream.writeChar(this.index[this.data16 + i]);
        }
        return iSerializeHeader + (this.dataLength * 2);
    }

    public int getSerializedLength() {
        return 16 + ((this.header.indexLength + this.dataLength) * 2);
    }

    @Override
    int rangeEnd(int i, int i2, int i3) {
        int i4;
        loop0: while (true) {
            if (i >= i2) {
                break;
            }
            char c = 2048;
            if (i < 55296 || (i > 56319 && i <= 65535)) {
                c = 0;
                i4 = this.index[i >> 5] << 2;
            } else if (i < 65535) {
                i4 = this.index[((i - 55296) >> 5) + 2048] << 2;
            } else if (i < this.highStart) {
                c = this.index[2080 + (i >> 11)];
                i4 = this.index[((i >> 5) & 63) + c] << 2;
            } else if (i3 == this.index[this.highValueIndex]) {
                i = i2;
            }
            if (c == this.index2NullOffset) {
                if (i3 != this.initialValue) {
                    break;
                }
                i += 2048;
            } else if (i4 == this.dataNullOffset) {
                if (i3 != this.initialValue) {
                    break;
                }
                i += 32;
            } else {
                int i5 = (i & 31) + i4;
                int i6 = i4 + 32;
                for (int i7 = i5; i7 < i6; i7++) {
                    if (this.index[i7] != i3) {
                        i += i7 - i5;
                        break loop0;
                    }
                }
                i += i6 - i5;
            }
        }
        if (i > i2) {
            i = i2;
        }
        return i - 1;
    }
}
