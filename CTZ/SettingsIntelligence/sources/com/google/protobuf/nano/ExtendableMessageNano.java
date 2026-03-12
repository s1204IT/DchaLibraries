package com.google.protobuf.nano;

import com.google.protobuf.nano.ExtendableMessageNano;

public abstract class ExtendableMessageNano<M extends ExtendableMessageNano<M>> extends MessageNano {
    protected FieldArray unknownFieldData;

    @Override
    public M mo2clone() throws CloneNotSupportedException {
        M m = (M) super.mo2clone();
        InternalNano.cloneUnknownFieldData(this, m);
        return m;
    }
}
