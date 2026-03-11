package com.android.framework.protobuf.nano;

import com.android.framework.protobuf.nano.ExtendableMessageNano;
import java.io.IOException;

public abstract class ExtendableMessageNano<M extends ExtendableMessageNano<M>> extends MessageNano {
    protected FieldArray unknownFieldData;

    @Override
    protected int computeSerializedSize() {
        int size = 0;
        if (this.unknownFieldData != null) {
            for (int i = 0; i < this.unknownFieldData.size(); i++) {
                FieldData field = this.unknownFieldData.dataAt(i);
                size += field.computeSerializedSize();
            }
        }
        return size;
    }

    @Override
    public void writeTo(CodedOutputByteBufferNano output) throws IOException {
        if (this.unknownFieldData == null) {
            return;
        }
        for (int i = 0; i < this.unknownFieldData.size(); i++) {
            FieldData field = this.unknownFieldData.dataAt(i);
            field.writeTo(output);
        }
    }

    @Override
    public M m429clone() throws CloneNotSupportedException {
        M m = (M) super.m429clone();
        InternalNano.cloneUnknownFieldData(this, m);
        return m;
    }
}
