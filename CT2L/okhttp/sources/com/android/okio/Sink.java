package com.android.okio;

import java.io.Closeable;
import java.io.IOException;

public interface Sink extends Closeable {
    @Override
    void close() throws IOException;

    Sink mo2deadline(Deadline deadline);

    void flush() throws IOException;

    void write(OkBuffer okBuffer, long j) throws IOException;
}
