package com.google.common.base;

public abstract class Ticker {
    private static final Ticker SYSTEM_TICKER = new Ticker() {
        @Override
        public long read() {
            return Platform.systemNanoTime();
        }
    };

    public abstract long read();

    protected Ticker() {
    }

    public static Ticker systemTicker() {
        return SYSTEM_TICKER;
    }
}
