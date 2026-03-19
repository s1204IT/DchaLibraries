package com.android.server.locksettings.recoverablekeystore;

public class RecoverableKeyStorageException extends Exception {
    public RecoverableKeyStorageException(String str) {
        super(str);
    }

    public RecoverableKeyStorageException(String str, Throwable th) {
        super(str, th);
    }
}
