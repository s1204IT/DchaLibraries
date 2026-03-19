package com.google.common.util.concurrent;

import java.lang.Exception;
import java.util.concurrent.TimeUnit;

public interface CheckedFuture<V, X extends Exception> extends ListenableFuture<V> {
    V checkedGet() throws Exception;

    V checkedGet(long j, TimeUnit timeUnit) throws Exception;
}
