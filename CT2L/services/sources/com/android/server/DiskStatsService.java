package com.android.server;

import android.content.Context;
import android.os.Binder;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemClock;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class DiskStatsService extends Binder {
    private static final String TAG = "DiskStatsService";
    private final Context mContext;

    public DiskStatsService(Context context) {
        this.mContext = context;
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) throws Throwable {
        FileOutputStream fos;
        this.mContext.enforceCallingOrSelfPermission("android.permission.DUMP", TAG);
        byte[] junk = new byte[512];
        for (int i = 0; i < junk.length; i++) {
            junk[i] = (byte) i;
        }
        File tmp = new File(Environment.getDataDirectory(), "system/perftest.tmp");
        FileOutputStream fos2 = null;
        IOException error = null;
        long before = SystemClock.uptimeMillis();
        try {
            fos = new FileOutputStream(tmp);
        } catch (IOException e) {
            e = e;
        } catch (Throwable th) {
            th = th;
        }
        try {
            fos.write(junk);
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e2) {
                }
            }
        } catch (IOException e3) {
            e = e3;
            fos2 = fos;
            error = e;
            if (fos2 != null) {
                try {
                    fos2.close();
                } catch (IOException e4) {
                }
            }
        } catch (Throwable th2) {
            th = th2;
            fos2 = fos;
            if (fos2 != null) {
                try {
                    fos2.close();
                } catch (IOException e5) {
                }
            }
            throw th;
        }
        long after = SystemClock.uptimeMillis();
        if (tmp.exists()) {
            tmp.delete();
        }
        if (error != null) {
            pw.print("Test-Error: ");
            pw.println(error.toString());
        } else {
            pw.print("Latency: ");
            pw.print(after - before);
            pw.println("ms [512B Data Write]");
        }
        reportFreeSpace(Environment.getDataDirectory(), "Data", pw);
        reportFreeSpace(Environment.getDownloadCacheDirectory(), "Cache", pw);
        reportFreeSpace(new File("/system"), "System", pw);
    }

    private void reportFreeSpace(File path, String name, PrintWriter pw) {
        try {
            StatFs statfs = new StatFs(path.getPath());
            long bsize = statfs.getBlockSize();
            long avail = statfs.getAvailableBlocks();
            long total = statfs.getBlockCount();
            if (bsize <= 0 || total <= 0) {
                throw new IllegalArgumentException("Invalid stat: bsize=" + bsize + " avail=" + avail + " total=" + total);
            }
            pw.print(name);
            pw.print("-Free: ");
            pw.print((avail * bsize) / 1024);
            pw.print("K / ");
            pw.print((total * bsize) / 1024);
            pw.print("K total = ");
            pw.print((100 * avail) / total);
            pw.println("% free");
        } catch (IllegalArgumentException e) {
            pw.print(name);
            pw.print("-Error: ");
            pw.println(e.toString());
        }
    }
}
