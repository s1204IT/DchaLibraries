package com.google.protobuf.nano;

import java.io.IOException;
import java.util.Arrays;

final class UnknownFieldData {
    final byte[] bytes;
    final int tag;

    UnknownFieldData(int tag, byte[] bytes) {
        this.tag = tag;
        this.bytes = bytes;
    }

    int computeSerializedSize() {
        int size = CodedOutputByteBufferNano.computeRawVarint32Size(this.tag) + 0;
        return size + this.bytes.length;
    }

    void writeTo(CodedOutputByteBufferNano output) throws IOException {
        output.writeRawVarint32(this.tag);
        output.writeRawBytes(this.bytes);
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof UnknownFieldData)) {
            return false;
        }
        UnknownFieldData other = (UnknownFieldData) o;
        if (this.tag == other.tag) {
            return Arrays.equals(this.bytes, other.bytes);
        }
        return false;
    }

    public int hashCode() {
        int result = this.tag + 527;
        return (result * 31) + Arrays.hashCode(this.bytes);
    }
}
