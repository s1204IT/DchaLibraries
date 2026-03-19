package com.mediatek.gallerybasic.base;

public interface Work<T> {
    boolean isCanceled();

    T run();
}
