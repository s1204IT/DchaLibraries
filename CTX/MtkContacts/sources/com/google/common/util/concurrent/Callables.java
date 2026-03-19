package com.google.common.util.concurrent;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import java.util.concurrent.Callable;

public final class Callables {
    private Callables() {
    }

    public static <T> Callable<T> returning(final T t) {
        return new Callable<T>() {
            @Override
            public T call() {
                return (T) t;
            }
        };
    }

    static <T> Callable<T> threadRenaming(final Callable<T> callable, final Supplier<String> supplier) {
        Preconditions.checkNotNull(supplier);
        Preconditions.checkNotNull(callable);
        return new Callable<T>() {
            @Override
            public T call() throws Exception {
                Thread threadCurrentThread = Thread.currentThread();
                String name = threadCurrentThread.getName();
                boolean zTrySetName = Callables.trySetName((String) supplier.get(), threadCurrentThread);
                try {
                    return (T) callable.call();
                } finally {
                    if (zTrySetName) {
                        Callables.trySetName(name, threadCurrentThread);
                    }
                }
            }
        };
    }

    static Runnable threadRenaming(final Runnable runnable, final Supplier<String> supplier) {
        Preconditions.checkNotNull(supplier);
        Preconditions.checkNotNull(runnable);
        return new Runnable() {
            @Override
            public void run() {
                Thread threadCurrentThread = Thread.currentThread();
                String name = threadCurrentThread.getName();
                boolean zTrySetName = Callables.trySetName((String) supplier.get(), threadCurrentThread);
                try {
                    runnable.run();
                } finally {
                    if (zTrySetName) {
                        Callables.trySetName(name, threadCurrentThread);
                    }
                }
            }
        };
    }

    private static boolean trySetName(String str, Thread thread) {
        try {
            thread.setName(str);
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }
}
