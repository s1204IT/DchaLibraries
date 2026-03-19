package com.android.server;

import android.content.Context;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.IStoraged;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.StatFs;
import android.os.SystemClock;
import android.os.storage.StorageManager;
import android.util.Log;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.internal.util.DumpUtils;
import com.android.server.storage.DiskStatsFileLogger;
import com.android.server.storage.DiskStatsLoggingService;
import com.android.server.utils.PriorityDump;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import libcore.io.IoUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DiskStatsService extends Binder {
    private static final String DISKSTATS_DUMP_FILE = "/data/system/diskstats_cache.json";
    private static final String TAG = "DiskStatsService";
    private final Context mContext;

    public DiskStatsService(Context context) {
        this.mContext = context;
        DiskStatsLoggingService.schedule(context);
    }

    @Override
    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) throws Throwable {
        Throwable th;
        IOException iOException;
        FileOutputStream fileOutputStream;
        PrintWriter printWriter2;
        ProtoOutputStream protoOutputStream;
        if (DumpUtils.checkDumpAndUsageStatsPermission(this.mContext, TAG, printWriter)) {
            byte[] bArr = new byte[512];
            for (int i = 0; i < bArr.length; i++) {
                bArr[i] = (byte) i;
            }
            File file = new File(Environment.getDataDirectory(), "system/perftest.tmp");
            long jUptimeMillis = SystemClock.uptimeMillis();
            FileOutputStream fileOutputStream2 = null;
            try {
                FileOutputStream fileOutputStream3 = new FileOutputStream(file);
                try {
                    fileOutputStream3.write(bArr);
                    try {
                        fileOutputStream3.close();
                    } catch (IOException e) {
                    }
                    iOException = null;
                } catch (IOException e2) {
                    iOException = e2;
                    fileOutputStream = fileOutputStream3;
                    if (fileOutputStream != null) {
                        try {
                            fileOutputStream.close();
                        } catch (IOException e3) {
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                    fileOutputStream2 = fileOutputStream3;
                    if (fileOutputStream2 == null) {
                        throw th;
                    }
                    try {
                        fileOutputStream2.close();
                        throw th;
                    } catch (IOException e4) {
                        throw th;
                    }
                }
            } catch (IOException e5) {
                iOException = e5;
                fileOutputStream = null;
            } catch (Throwable th3) {
                th = th3;
            }
            long jUptimeMillis2 = SystemClock.uptimeMillis();
            if (file.exists()) {
                file.delete();
            }
            boolean zHasOption = hasOption(strArr, PriorityDump.PROTO_ARG);
            if (zHasOption) {
                ProtoOutputStream protoOutputStream2 = new ProtoOutputStream(fileDescriptor);
                protoOutputStream2.write(1133871366145L, iOException != null);
                if (iOException != null) {
                    protoOutputStream2.write(1138166333442L, iOException.toString());
                } else {
                    protoOutputStream2.write(1120986464259L, jUptimeMillis2 - jUptimeMillis);
                }
                protoOutputStream = protoOutputStream2;
                printWriter2 = null;
            } else {
                if (iOException != null) {
                    printWriter.print("Test-Error: ");
                    printWriter.println(iOException.toString());
                } else {
                    printWriter.print("Latency: ");
                    printWriter.print(jUptimeMillis2 - jUptimeMillis);
                    printWriter.println("ms [512B Data Write]");
                }
                printWriter2 = printWriter;
                protoOutputStream = null;
            }
            if (zHasOption) {
                reportDiskWriteSpeedProto(protoOutputStream);
            } else {
                reportDiskWriteSpeed(printWriter2);
            }
            PrintWriter printWriter3 = printWriter2;
            ProtoOutputStream protoOutputStream3 = protoOutputStream;
            reportFreeSpace(Environment.getDataDirectory(), "Data", printWriter3, protoOutputStream3, 0);
            reportFreeSpace(Environment.getDownloadCacheDirectory(), "Cache", printWriter3, protoOutputStream3, 1);
            reportFreeSpace(new File("/system"), "System", printWriter3, protoOutputStream3, 2);
            boolean zIsFileEncryptedNativeOnly = StorageManager.isFileEncryptedNativeOnly();
            boolean zIsBlockEncrypted = zIsFileEncryptedNativeOnly ? false : StorageManager.isBlockEncrypted();
            if (zHasOption) {
                if (zIsFileEncryptedNativeOnly) {
                    protoOutputStream.write(1159641169925L, 3);
                } else if (zIsBlockEncrypted) {
                    protoOutputStream.write(1159641169925L, 2);
                } else {
                    protoOutputStream.write(1159641169925L, 1);
                }
            } else if (zIsFileEncryptedNativeOnly) {
                printWriter2.println("File-based Encryption: true");
            }
            if (zHasOption) {
                reportCachedValuesProto(protoOutputStream);
            } else {
                reportCachedValues(printWriter2);
            }
            if (zHasOption) {
                protoOutputStream.flush();
            }
        }
    }

    private void reportFreeSpace(File file, String str, PrintWriter printWriter, ProtoOutputStream protoOutputStream, int i) {
        try {
            StatFs statFs = new StatFs(file.getPath());
            long blockSize = statFs.getBlockSize();
            long availableBlocks = statFs.getAvailableBlocks();
            long blockCount = statFs.getBlockCount();
            if (blockSize <= 0 || blockCount <= 0) {
                throw new IllegalArgumentException("Invalid stat: bsize=" + blockSize + " avail=" + availableBlocks + " total=" + blockCount);
            }
            if (protoOutputStream != null) {
                long jStart = protoOutputStream.start(2246267895812L);
                protoOutputStream.write(1159641169921L, i);
                protoOutputStream.write(1112396529666L, (availableBlocks * blockSize) / 1024);
                protoOutputStream.write(1112396529667L, (blockCount * blockSize) / 1024);
                protoOutputStream.end(jStart);
                return;
            }
            printWriter.print(str);
            printWriter.print("-Free: ");
            printWriter.print((availableBlocks * blockSize) / 1024);
            printWriter.print("K / ");
            printWriter.print((blockSize * blockCount) / 1024);
            printWriter.print("K total = ");
            printWriter.print((availableBlocks * 100) / blockCount);
            printWriter.println("% free");
        } catch (IllegalArgumentException e) {
            if (protoOutputStream == null) {
                printWriter.print(str);
                printWriter.print("-Error: ");
                printWriter.println(e.toString());
            }
        }
    }

    private boolean hasOption(String[] strArr, String str) {
        for (String str2 : strArr) {
            if (str.equals(str2)) {
                return true;
            }
        }
        return false;
    }

    private void reportCachedValues(PrintWriter printWriter) {
        try {
            JSONObject jSONObject = new JSONObject(IoUtils.readFileAsString("/data/system/diskstats_cache.json"));
            printWriter.print("App Size: ");
            printWriter.println(jSONObject.getLong(DiskStatsFileLogger.APP_SIZE_AGG_KEY));
            printWriter.print("App Data Size: ");
            printWriter.println(jSONObject.getLong(DiskStatsFileLogger.APP_DATA_SIZE_AGG_KEY));
            printWriter.print("App Cache Size: ");
            printWriter.println(jSONObject.getLong(DiskStatsFileLogger.APP_CACHE_AGG_KEY));
            printWriter.print("Photos Size: ");
            printWriter.println(jSONObject.getLong(DiskStatsFileLogger.PHOTOS_KEY));
            printWriter.print("Videos Size: ");
            printWriter.println(jSONObject.getLong(DiskStatsFileLogger.VIDEOS_KEY));
            printWriter.print("Audio Size: ");
            printWriter.println(jSONObject.getLong(DiskStatsFileLogger.AUDIO_KEY));
            printWriter.print("Downloads Size: ");
            printWriter.println(jSONObject.getLong(DiskStatsFileLogger.DOWNLOADS_KEY));
            printWriter.print("System Size: ");
            printWriter.println(jSONObject.getLong(DiskStatsFileLogger.SYSTEM_KEY));
            printWriter.print("Other Size: ");
            printWriter.println(jSONObject.getLong(DiskStatsFileLogger.MISC_KEY));
            printWriter.print("Package Names: ");
            printWriter.println(jSONObject.getJSONArray(DiskStatsFileLogger.PACKAGE_NAMES_KEY));
            printWriter.print("App Sizes: ");
            printWriter.println(jSONObject.getJSONArray(DiskStatsFileLogger.APP_SIZES_KEY));
            printWriter.print("App Data Sizes: ");
            printWriter.println(jSONObject.getJSONArray(DiskStatsFileLogger.APP_DATA_KEY));
            printWriter.print("Cache Sizes: ");
            printWriter.println(jSONObject.getJSONArray(DiskStatsFileLogger.APP_CACHES_KEY));
        } catch (IOException | JSONException e) {
            Log.w(TAG, "exception reading diskstats cache file", e);
        }
    }

    private void reportCachedValuesProto(ProtoOutputStream protoOutputStream) {
        try {
            JSONObject jSONObject = new JSONObject(IoUtils.readFileAsString("/data/system/diskstats_cache.json"));
            long jStart = protoOutputStream.start(1146756268038L);
            protoOutputStream.write(1112396529665L, jSONObject.getLong(DiskStatsFileLogger.APP_SIZE_AGG_KEY));
            protoOutputStream.write(1112396529674L, jSONObject.getLong(DiskStatsFileLogger.APP_DATA_SIZE_AGG_KEY));
            long j = 1112396529666L;
            protoOutputStream.write(1112396529666L, jSONObject.getLong(DiskStatsFileLogger.APP_CACHE_AGG_KEY));
            protoOutputStream.write(1112396529667L, jSONObject.getLong(DiskStatsFileLogger.PHOTOS_KEY));
            protoOutputStream.write(1112396529668L, jSONObject.getLong(DiskStatsFileLogger.VIDEOS_KEY));
            protoOutputStream.write(1112396529669L, jSONObject.getLong(DiskStatsFileLogger.AUDIO_KEY));
            protoOutputStream.write(1112396529670L, jSONObject.getLong(DiskStatsFileLogger.DOWNLOADS_KEY));
            protoOutputStream.write(1112396529671L, jSONObject.getLong(DiskStatsFileLogger.SYSTEM_KEY));
            protoOutputStream.write(1112396529672L, jSONObject.getLong(DiskStatsFileLogger.MISC_KEY));
            JSONArray jSONArray = jSONObject.getJSONArray(DiskStatsFileLogger.PACKAGE_NAMES_KEY);
            JSONArray jSONArray2 = jSONObject.getJSONArray(DiskStatsFileLogger.APP_SIZES_KEY);
            JSONArray jSONArray3 = jSONObject.getJSONArray(DiskStatsFileLogger.APP_DATA_KEY);
            JSONArray jSONArray4 = jSONObject.getJSONArray(DiskStatsFileLogger.APP_CACHES_KEY);
            int length = jSONArray.length();
            if (length == jSONArray2.length() && length == jSONArray3.length() && length == jSONArray4.length()) {
                int i = 0;
                while (i < length) {
                    long jStart2 = protoOutputStream.start(2246267895817L);
                    protoOutputStream.write(1138166333441L, jSONArray.getString(i));
                    protoOutputStream.write(j, jSONArray2.getLong(i));
                    protoOutputStream.write(1112396529668L, jSONArray3.getLong(i));
                    protoOutputStream.write(1112396529667L, jSONArray4.getLong(i));
                    protoOutputStream.end(jStart2);
                    i++;
                    j = 1112396529666L;
                }
            } else {
                Slog.wtf(TAG, "Sizes of packageNamesArray, appSizesArray, appDataSizesArray  and cacheSizesArray are not the same");
            }
            protoOutputStream.end(jStart);
        } catch (IOException | JSONException e) {
            Log.w(TAG, "exception reading diskstats cache file", e);
        }
    }

    private int getRecentPerf() throws IllegalStateException, RemoteException {
        IBinder service = ServiceManager.getService("storaged");
        if (service == null) {
            throw new IllegalStateException("storaged not found");
        }
        return IStoraged.Stub.asInterface(service).getRecentPerf();
    }

    private void reportDiskWriteSpeed(PrintWriter printWriter) {
        try {
            long recentPerf = getRecentPerf();
            if (recentPerf != 0) {
                printWriter.print("Recent Disk Write Speed (kB/s) = ");
                printWriter.println(recentPerf);
            } else {
                printWriter.println("Recent Disk Write Speed data unavailable");
                Log.w(TAG, "Recent Disk Write Speed data unavailable!");
            }
        } catch (RemoteException | IllegalStateException e) {
            printWriter.println(e.toString());
            Log.e(TAG, e.toString());
        }
    }

    private void reportDiskWriteSpeedProto(ProtoOutputStream protoOutputStream) {
        try {
            long recentPerf = getRecentPerf();
            if (recentPerf != 0) {
                protoOutputStream.write(1120986464263L, recentPerf);
            } else {
                Log.w(TAG, "Recent Disk Write Speed data unavailable!");
            }
        } catch (RemoteException | IllegalStateException e) {
            Log.e(TAG, e.toString());
        }
    }
}
