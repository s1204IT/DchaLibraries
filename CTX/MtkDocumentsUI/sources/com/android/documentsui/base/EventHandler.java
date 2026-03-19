package com.android.documentsui.base;

@FunctionalInterface
public interface EventHandler<T> {
    boolean accept(T t);
}
