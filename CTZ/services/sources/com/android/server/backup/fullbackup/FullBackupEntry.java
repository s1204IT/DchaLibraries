package com.android.server.backup.fullbackup;

public class FullBackupEntry implements Comparable<FullBackupEntry> {
    public long lastBackup;
    public String packageName;

    public FullBackupEntry(String str, long j) {
        this.packageName = str;
        this.lastBackup = j;
    }

    @Override
    public int compareTo(FullBackupEntry fullBackupEntry) {
        if (this.lastBackup < fullBackupEntry.lastBackup) {
            return -1;
        }
        if (this.lastBackup > fullBackupEntry.lastBackup) {
            return 1;
        }
        return 0;
    }
}
