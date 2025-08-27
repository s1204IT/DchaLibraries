package com.android.quicksearchbox.util;

import java.io.Closeable;

/* loaded from: classes.dex */
public interface QuietlyCloseable extends Closeable {
    @Override // java.io.Closeable, java.lang.AutoCloseable
    void close();
}
