package com.google.common.base;

final class Platform {
    private static final ThreadLocal<char[]> DEST_TL = new ThreadLocal<char[]>() {
        @Override
        protected char[] initialValue() {
            return new char[1024];
        }
    };

    static long systemNanoTime() {
        return System.nanoTime();
    }
}
