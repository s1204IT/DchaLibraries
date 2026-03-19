package com.android.documentsui.base;

@FunctionalInterface
public interface EventListener<T> {
    void accept(T t);
}
