package com.android.server;

public class NativeDaemonConnectorException extends Exception {
    private String mCmd;
    private NativeDaemonEvent mEvent;

    public NativeDaemonConnectorException(String str) {
        super(str);
    }

    public NativeDaemonConnectorException(String str, Throwable th) {
        super(str, th);
    }

    public NativeDaemonConnectorException(String str, NativeDaemonEvent nativeDaemonEvent) {
        super("command '" + str + "' failed with '" + nativeDaemonEvent + "'");
        this.mCmd = str;
        this.mEvent = nativeDaemonEvent;
    }

    public int getCode() {
        if (this.mEvent != null) {
            return this.mEvent.getCode();
        }
        return -1;
    }

    public String getCmd() {
        return this.mCmd;
    }

    public IllegalArgumentException rethrowAsParcelableException() {
        throw new IllegalStateException(getMessage(), this);
    }
}
