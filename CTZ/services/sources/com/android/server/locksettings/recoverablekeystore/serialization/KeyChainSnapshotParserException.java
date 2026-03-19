package com.android.server.locksettings.recoverablekeystore.serialization;

public class KeyChainSnapshotParserException extends Exception {
    public KeyChainSnapshotParserException(String str, Throwable th) {
        super(str, th);
    }

    public KeyChainSnapshotParserException(String str) {
        super(str);
    }
}
