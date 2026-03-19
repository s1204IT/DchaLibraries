package com.android.internal.os;

import android.mtp.MtpConstants;
import android.os.Process;
import android.os.StrictMode;
import android.os.SystemClock;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.KernelWakelockStats;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

public class KernelWakelockReader {
    private static final String TAG = "KernelWakelockReader";
    private static final String sWakelockFile = "/proc/wakelocks";
    private static final String sWakeupSourceFile = "/d/wakeup_sources";
    private static int sKernelWakelockUpdateVersion = 0;
    private static final int[] PROC_WAKELOCKS_FORMAT = {5129, MtpConstants.RESPONSE_INVALID_OBJECT_HANDLE, 9, 9, 9, MtpConstants.RESPONSE_INVALID_OBJECT_HANDLE};
    private static final int[] WAKEUP_SOURCES_FORMAT = {4105, 8457, 265, 265, 265, 265, 8457};
    private final String[] mProcWakelocksName = new String[3];
    private final long[] mProcWakelocksData = new long[3];

    public final KernelWakelockStats readKernelWakelockStats(KernelWakelockStats kernelWakelockStats) {
        FileInputStream fileInputStream;
        boolean z;
        byte[] bArr = new byte[32768];
        long jUptimeMillis = SystemClock.uptimeMillis();
        int iAllowThreadDiskReadsMask = StrictMode.allowThreadDiskReadsMask();
        int i = 0;
        try {
            try {
                fileInputStream = new FileInputStream(sWakelockFile);
                z = false;
            } catch (FileNotFoundException e) {
                try {
                    fileInputStream = new FileInputStream(sWakeupSourceFile);
                    z = true;
                } catch (FileNotFoundException e2) {
                    Slog.wtf(TAG, "neither /proc/wakelocks nor /d/wakeup_sources exists");
                    return null;
                }
            }
            int i2 = fileInputStream.read(bArr);
            fileInputStream.close();
            StrictMode.setThreadPolicyMask(iAllowThreadDiskReadsMask);
            long jUptimeMillis2 = SystemClock.uptimeMillis() - jUptimeMillis;
            if (jUptimeMillis2 > 100) {
                Slog.w(TAG, "Reading wakelock stats took " + jUptimeMillis2 + "ms");
            }
            if (i2 <= 0) {
                i = i2;
            } else {
                if (i2 >= bArr.length) {
                    Slog.wtf(TAG, "Kernel wake locks exceeded buffer size " + bArr.length);
                }
                while (i < i2) {
                    if (bArr[i] == 0) {
                        break;
                    }
                    i++;
                }
                i = i2;
            }
            return parseProcWakelocks(bArr, i, z, kernelWakelockStats);
        } catch (IOException e3) {
            Slog.wtf(TAG, "failed to read kernel wakelocks", e3);
            return null;
        } finally {
            StrictMode.setThreadPolicyMask(iAllowThreadDiskReadsMask);
        }
    }

    @VisibleForTesting
    public KernelWakelockStats parseProcWakelocks(byte[] bArr, int i, boolean z, KernelWakelockStats kernelWakelockStats) {
        byte b;
        int i2 = 0;
        while (true) {
            b = 10;
            if (i2 >= i || bArr[i2] == 10 || bArr[i2] == 0) {
                break;
            }
            i2++;
        }
        int i3 = i2 + 1;
        synchronized (this) {
            sKernelWakelockUpdateVersion++;
            int i4 = i3;
            while (i3 < i) {
                int i5 = i4;
                while (i5 < i && bArr[i5] != b && bArr[i5] != 0) {
                    i5++;
                }
                if (i5 > i - 1) {
                    break;
                }
                String[] strArr = this.mProcWakelocksName;
                long[] jArr = this.mProcWakelocksData;
                for (int i6 = i4; i6 < i5; i6++) {
                    if ((bArr[i6] & 128) != 0) {
                        bArr[i6] = 63;
                    }
                }
                int i7 = i5;
                int i8 = i4;
                boolean procLine = Process.parseProcLine(bArr, i4, i5, z ? WAKEUP_SOURCES_FORMAT : PROC_WAKELOCKS_FORMAT, strArr, jArr, null);
                String str = strArr[0];
                int i9 = (int) jArr[1];
                long j = z ? jArr[2] * 1000 : (jArr[2] + 500) / 1000;
                if (procLine && str.length() > 0) {
                    if (!kernelWakelockStats.containsKey(str)) {
                        kernelWakelockStats.put(str, new KernelWakelockStats.Entry(i9, j, sKernelWakelockUpdateVersion));
                    } else {
                        KernelWakelockStats.Entry entry = (KernelWakelockStats.Entry) kernelWakelockStats.get(str);
                        if (entry.mVersion == sKernelWakelockUpdateVersion) {
                            entry.mCount += i9;
                            entry.mTotalTime += j;
                        } else {
                            entry.mCount = i9;
                            entry.mTotalTime = j;
                            entry.mVersion = sKernelWakelockUpdateVersion;
                        }
                    }
                } else if (!procLine) {
                    try {
                        Slog.wtf(TAG, "Failed to parse proc line: " + new String(bArr, i8, i7 - i8));
                    } catch (Exception e) {
                        Slog.wtf(TAG, "Failed to parse proc line!");
                    }
                }
                i4 = i7 + 1;
                i3 = i7;
                b = 10;
            }
            Iterator it = kernelWakelockStats.values().iterator();
            while (it.hasNext()) {
                if (((KernelWakelockStats.Entry) it.next()).mVersion != sKernelWakelockUpdateVersion) {
                    it.remove();
                }
            }
            kernelWakelockStats.kernelWakelockVersion = sKernelWakelockUpdateVersion;
        }
        return kernelWakelockStats;
    }
}
