package com.android.server.telecom;

public interface ClockProxy {
    long currentTimeMillis();

    long elapsedRealtime();
}
