package com.android.quicksearchbox.util;

import java.io.Closeable;

public interface QuietlyCloseable extends Closeable {
    @Override
    void close();
}
