package com.android.server.input;

public final class InputApplicationHandle {
    public final Object appWindowToken;
    public long dispatchingTimeoutNanos;
    public String name;
    private long ptr;

    private native void nativeDispose();

    public InputApplicationHandle(Object obj) {
        this.appWindowToken = obj;
    }

    protected void finalize() throws Throwable {
        try {
            nativeDispose();
        } finally {
            super.finalize();
        }
    }
}
