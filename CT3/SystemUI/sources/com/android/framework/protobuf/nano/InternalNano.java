package com.android.framework.protobuf.nano;

public final class InternalNano {
    public static final Object LAZY_INIT_LOCK = new Object();

    private InternalNano() {
    }

    public static void cloneUnknownFieldData(ExtendableMessageNano original, ExtendableMessageNano cloned) {
        if (original.unknownFieldData == null) {
            return;
        }
        cloned.unknownFieldData = original.unknownFieldData.m427clone();
    }
}
