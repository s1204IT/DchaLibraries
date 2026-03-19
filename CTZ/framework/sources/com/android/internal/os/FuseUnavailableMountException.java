package com.android.internal.os;

public class FuseUnavailableMountException extends Exception {
    public FuseUnavailableMountException(int i) {
        super("AppFuse mount point " + i + " is unavailable");
    }
}
