package com.android.providers.contacts.util;

public class Clock {
    private static final Clock INSTANCE = new Clock();
    private static Clock sInstance = INSTANCE;

    public static final Clock getInstance() {
        return sInstance;
    }

    static void injectInstance(Clock clock) {
        sInstance = clock;
    }

    static void resetInstance() {
        sInstance = INSTANCE;
    }

    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
