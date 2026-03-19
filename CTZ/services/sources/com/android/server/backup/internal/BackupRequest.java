package com.android.server.backup.internal;

public class BackupRequest {
    public String packageName;

    public BackupRequest(String str) {
        this.packageName = str;
    }

    public String toString() {
        return "BackupRequest{pkg=" + this.packageName + "}";
    }
}
