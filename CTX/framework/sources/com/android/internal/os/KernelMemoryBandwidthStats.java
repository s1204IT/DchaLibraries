package com.android.internal.os;

import android.os.StrictMode;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.LongSparseLongArray;
import android.util.Slog;
import android.util.TimeUtils;
import com.android.internal.annotations.VisibleForTesting;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class KernelMemoryBandwidthStats {
    private static final boolean DEBUG = false;
    private static final String TAG = "KernelMemoryBandwidthStats";
    private static final String mSysfsFile = "/sys/kernel/memory_state_time/show_stat";
    protected final LongSparseLongArray mBandwidthEntries = new LongSparseLongArray();
    private boolean mStatsDoNotExist = false;

    public void updateStats() {
        if (this.mStatsDoNotExist) {
            return;
        }
        long jUptimeMillis = SystemClock.uptimeMillis();
        StrictMode.ThreadPolicy threadPolicyAllowThreadDiskReads = StrictMode.allowThreadDiskReads();
        try {
            try {
                try {
                    BufferedReader bufferedReader = new BufferedReader(new FileReader(mSysfsFile));
                    Throwable th = null;
                    try {
                        parseStats(bufferedReader);
                        bufferedReader.close();
                    } catch (Throwable th2) {
                        if (0 != 0) {
                            try {
                                bufferedReader.close();
                            } catch (Throwable th3) {
                                th.addSuppressed(th3);
                            }
                        } else {
                            bufferedReader.close();
                        }
                        throw th2;
                    }
                } catch (IOException e) {
                    Slog.e(TAG, "Failed to read memory bandwidth: " + e.getMessage());
                    this.mBandwidthEntries.clear();
                }
            } catch (FileNotFoundException e2) {
                Slog.w(TAG, "No kernel memory bandwidth stats available");
                this.mBandwidthEntries.clear();
                this.mStatsDoNotExist = true;
            }
            StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskReads);
            long jUptimeMillis2 = SystemClock.uptimeMillis() - jUptimeMillis;
            if (jUptimeMillis2 > 100) {
                Slog.w(TAG, "Reading memory bandwidth file took " + jUptimeMillis2 + "ms");
            }
        } catch (Throwable th4) {
            StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskReads);
            throw th4;
        }
    }

    @VisibleForTesting
    public void parseStats(BufferedReader bufferedReader) throws IOException {
        TextUtils.SimpleStringSplitter simpleStringSplitter = new TextUtils.SimpleStringSplitter(' ');
        this.mBandwidthEntries.clear();
        while (true) {
            String line = bufferedReader.readLine();
            if (line != null) {
                simpleStringSplitter.setString(line);
                simpleStringSplitter.next();
                int i = 0;
                do {
                    long j = i;
                    int iIndexOfKey = this.mBandwidthEntries.indexOfKey(j);
                    if (iIndexOfKey >= 0) {
                        this.mBandwidthEntries.put(j, this.mBandwidthEntries.valueAt(iIndexOfKey) + (Long.parseLong(simpleStringSplitter.next()) / TimeUtils.NANOS_PER_MS));
                    } else {
                        this.mBandwidthEntries.put(j, Long.parseLong(simpleStringSplitter.next()) / TimeUtils.NANOS_PER_MS);
                    }
                    i++;
                } while (simpleStringSplitter.hasNext());
            } else {
                return;
            }
        }
    }

    public LongSparseLongArray getBandwidthEntries() {
        return this.mBandwidthEntries;
    }
}
