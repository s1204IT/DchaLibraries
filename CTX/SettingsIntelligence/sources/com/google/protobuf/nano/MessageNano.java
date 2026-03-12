package com.google.protobuf.nano;

import java.io.IOException;

public abstract class MessageNano {
    protected volatile int cachedSize = -1;

    public int getCachedSize() {
        if (this.cachedSize < 0) {
            getSerializedSize();
        }
        return this.cachedSize;
    }

    public int getSerializedSize() {
        int iComputeSerializedSize = computeSerializedSize();
        this.cachedSize = iComputeSerializedSize;
        return iComputeSerializedSize;
    }

    protected int computeSerializedSize() {
        return 0;
    }

    public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
    }

    public String toString() {
        return MessageNanoPrinter.print(this);
    }

    @Override
    public MessageNano mo2clone() throws CloneNotSupportedException {
        return (MessageNano) super.clone();
    }
}
