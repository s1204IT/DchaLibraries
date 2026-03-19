package com.android.server.backup;

import android.util.Slog;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileMetadata {
    public String domain;
    public boolean hasApk;
    public String installerPackageName;
    public long mode;
    public long mtime;
    public String packageName;
    public String path;
    public long size;
    public int type;
    public long version;

    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("FileMetadata{");
        sb.append(this.packageName);
        sb.append(',');
        sb.append(this.type);
        sb.append(',');
        sb.append(this.domain);
        sb.append(':');
        sb.append(this.path);
        sb.append(',');
        sb.append(this.size);
        sb.append('}');
        return sb.toString();
    }

    public void dump() {
        StringBuilder sb = new StringBuilder(128);
        sb.append(this.type == 2 ? 'd' : '-');
        sb.append((this.mode & 256) != 0 ? 'r' : '-');
        sb.append((this.mode & 128) != 0 ? 'w' : '-');
        sb.append((this.mode & 64) != 0 ? 'x' : '-');
        sb.append((this.mode & 32) != 0 ? 'r' : '-');
        sb.append((this.mode & 16) != 0 ? 'w' : '-');
        sb.append((this.mode & 8) != 0 ? 'x' : '-');
        sb.append((this.mode & 4) == 0 ? '-' : 'r');
        sb.append((this.mode & 2) == 0 ? '-' : 'w');
        sb.append((this.mode & 1) != 0 ? 'x' : '-');
        sb.append(String.format(" %9d ", Long.valueOf(this.size)));
        sb.append(new SimpleDateFormat("MMM dd HH:mm:ss ").format(new Date(this.mtime)));
        sb.append(this.packageName);
        sb.append(" :: ");
        sb.append(this.domain);
        sb.append(" :: ");
        sb.append(this.path);
        Slog.i(BackupManagerService.TAG, sb.toString());
    }
}
