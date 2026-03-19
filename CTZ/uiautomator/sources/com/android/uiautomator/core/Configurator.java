package com.android.uiautomator.core;

@Deprecated
public final class Configurator {
    private static Configurator sConfigurator;
    private long mWaitForIdleTimeout = 10000;
    private long mWaitForSelector = 10000;
    private long mWaitForActionAcknowledgment = 3000;
    private long mScrollEventWaitTimeout = 200;
    private long mKeyInjectionDelay = 0;

    private Configurator() {
    }

    public static Configurator getInstance() {
        if (sConfigurator == null) {
            sConfigurator = new Configurator();
        }
        return sConfigurator;
    }

    public Configurator setWaitForIdleTimeout(long j) {
        this.mWaitForIdleTimeout = j;
        return this;
    }

    public long getWaitForIdleTimeout() {
        return this.mWaitForIdleTimeout;
    }

    public Configurator setWaitForSelectorTimeout(long j) {
        this.mWaitForSelector = j;
        return this;
    }

    public long getWaitForSelectorTimeout() {
        return this.mWaitForSelector;
    }

    public Configurator setScrollAcknowledgmentTimeout(long j) {
        this.mScrollEventWaitTimeout = j;
        return this;
    }

    public long getScrollAcknowledgmentTimeout() {
        return this.mScrollEventWaitTimeout;
    }

    public Configurator setActionAcknowledgmentTimeout(long j) {
        this.mWaitForActionAcknowledgment = j;
        return this;
    }

    public long getActionAcknowledgmentTimeout() {
        return this.mWaitForActionAcknowledgment;
    }

    public Configurator setKeyInjectionDelay(long j) {
        this.mKeyInjectionDelay = j;
        return this;
    }

    public long getKeyInjectionDelay() {
        return this.mKeyInjectionDelay;
    }
}
