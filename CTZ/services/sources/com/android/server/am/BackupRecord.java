package com.android.server.am;

import android.content.pm.ApplicationInfo;
import com.android.internal.os.BatteryStatsImpl;

final class BackupRecord {
    public static final int BACKUP_FULL = 1;
    public static final int BACKUP_NORMAL = 0;
    public static final int RESTORE = 2;
    public static final int RESTORE_FULL = 3;
    ProcessRecord app;
    final ApplicationInfo appInfo;
    final int backupMode;
    final BatteryStatsImpl.Uid.Pkg.Serv stats;
    String stringName;

    BackupRecord(BatteryStatsImpl.Uid.Pkg.Serv serv, ApplicationInfo applicationInfo, int i) {
        this.stats = serv;
        this.appInfo = applicationInfo;
        this.backupMode = i;
    }

    public String toString() {
        if (this.stringName != null) {
            return this.stringName;
        }
        StringBuilder sb = new StringBuilder(128);
        sb.append("BackupRecord{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(' ');
        sb.append(this.appInfo.packageName);
        sb.append(' ');
        sb.append(this.appInfo.name);
        sb.append(' ');
        sb.append(this.appInfo.backupAgentName);
        sb.append('}');
        String string = sb.toString();
        this.stringName = string;
        return string;
    }
}
