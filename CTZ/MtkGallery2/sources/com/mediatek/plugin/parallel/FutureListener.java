package com.mediatek.plugin.parallel;

public interface FutureListener<T> {
    void onFutureDone(Future<T> future);
}
