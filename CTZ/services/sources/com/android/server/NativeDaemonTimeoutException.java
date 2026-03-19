package com.android.server;

public class NativeDaemonTimeoutException extends NativeDaemonConnectorException {
    public NativeDaemonTimeoutException(String str, NativeDaemonEvent nativeDaemonEvent) {
        super(str, nativeDaemonEvent);
    }
}
