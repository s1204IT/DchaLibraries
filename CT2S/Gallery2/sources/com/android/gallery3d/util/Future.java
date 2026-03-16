package com.android.gallery3d.util;

public interface Future<T> {
    void cancel();

    T get();

    boolean isCancelled();

    void waitDone();
}
