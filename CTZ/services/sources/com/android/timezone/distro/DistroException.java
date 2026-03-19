package com.android.timezone.distro;

public class DistroException extends Exception {
    public DistroException(String str) {
        super(str);
    }

    public DistroException(String str, Throwable th) {
        super(str, th);
    }
}
