package com.android.managedprovisioning.common;

public class IllegalProvisioningArgumentException extends Exception {
    public IllegalProvisioningArgumentException(String str) {
        super(str);
    }

    public IllegalProvisioningArgumentException(String str, Throwable th) {
        super(str, th);
    }
}
