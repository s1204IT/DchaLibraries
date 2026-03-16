package com.android.server.pm;

import android.content.pm.PackageParser;

public class PackageManagerException extends Exception {
    public final int error;

    public PackageManagerException(int error, String detailMessage) {
        super(detailMessage);
        this.error = error;
    }

    public PackageManagerException(int error, String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
        this.error = error;
    }

    public static PackageManagerException from(PackageParser.PackageParserException e) throws PackageManagerException {
        throw new PackageManagerException(e.error, e.getMessage(), e.getCause());
    }
}
