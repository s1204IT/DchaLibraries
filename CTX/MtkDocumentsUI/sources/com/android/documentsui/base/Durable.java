package com.android.documentsui.base;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface Durable {
    void read(DataInputStream dataInputStream) throws IOException;

    void reset();

    void write(DataOutputStream dataOutputStream) throws IOException;
}
