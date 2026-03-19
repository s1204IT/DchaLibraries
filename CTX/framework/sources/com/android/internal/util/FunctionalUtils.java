package com.android.internal.util;

import android.os.RemoteException;
import android.util.ExceptionUtils;
import java.util.function.Consumer;

public class FunctionalUtils {

    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T getOrThrow() throws Exception;
    }

    private FunctionalUtils() {
    }

    public static <T> Consumer<T> uncheckExceptions(ThrowingConsumer<T> throwingConsumer) {
        return throwingConsumer;
    }

    public static <T> Consumer<T> ignoreRemoteException(RemoteExceptionIgnoringConsumer<T> remoteExceptionIgnoringConsumer) {
        return remoteExceptionIgnoringConsumer;
    }

    public static Runnable handleExceptions(final ThrowingRunnable throwingRunnable, final Consumer<Throwable> consumer) {
        return new Runnable() {
            @Override
            public final void run() {
                FunctionalUtils.lambda$handleExceptions$0(throwingRunnable, consumer);
            }
        };
    }

    static void lambda$handleExceptions$0(ThrowingRunnable throwingRunnable, Consumer consumer) {
        try {
            throwingRunnable.run();
        } catch (Throwable th) {
            consumer.accept(th);
        }
    }

    @FunctionalInterface
    public interface ThrowingRunnable extends Runnable {
        void runOrThrow() throws Exception;

        @Override
        default void run() {
            try {
                runOrThrow();
            } catch (Exception e) {
                throw ExceptionUtils.propagate(e);
            }
        }
    }

    @FunctionalInterface
    public interface ThrowingConsumer<T> extends Consumer<T> {
        void acceptOrThrow(T t) throws Exception;

        @Override
        default void accept(T t) {
            try {
                acceptOrThrow(t);
            } catch (Exception e) {
                throw ExceptionUtils.propagate(e);
            }
        }
    }

    @FunctionalInterface
    public interface RemoteExceptionIgnoringConsumer<T> extends Consumer<T> {
        void acceptOrThrow(T t) throws RemoteException;

        @Override
        default void accept(T t) {
            try {
                acceptOrThrow(t);
            } catch (RemoteException e) {
            }
        }
    }
}
