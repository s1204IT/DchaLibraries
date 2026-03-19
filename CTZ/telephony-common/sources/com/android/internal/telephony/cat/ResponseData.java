package com.android.internal.telephony.cat;

import java.io.ByteArrayOutputStream;

public abstract class ResponseData {
    public abstract void format(ByteArrayOutputStream byteArrayOutputStream);

    public static void writeLength(ByteArrayOutputStream byteArrayOutputStream, int i) {
        if (i > 127) {
            byteArrayOutputStream.write(129);
        }
        byteArrayOutputStream.write(i);
    }
}
