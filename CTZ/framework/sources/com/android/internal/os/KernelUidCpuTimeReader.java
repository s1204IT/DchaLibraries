package com.android.internal.os;

import android.os.StrictMode;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Slog;
import android.util.SparseLongArray;
import android.util.TimeUtils;
import com.android.internal.content.NativeLibraryHelper;
import com.android.internal.os.KernelUidCpuTimeReaderBase;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class KernelUidCpuTimeReader extends KernelUidCpuTimeReaderBase<Callback> {
    private static final String TAG = KernelUidCpuTimeReader.class.getSimpleName();
    private static final String sProcFile = "/proc/uid_cputime/show_uid_stat";
    private static final String sRemoveUidProcFile = "/proc/uid_cputime/remove_uid_range";
    private SparseLongArray mLastUserTimeUs = new SparseLongArray();
    private SparseLongArray mLastSystemTimeUs = new SparseLongArray();
    private long mLastTimeReadUs = 0;

    public interface Callback extends KernelUidCpuTimeReaderBase.Callback {
        void onUidCpuTime(int i, long j, long j2);
    }

    @Override
    protected void readDeltaImpl(Callback callback) {
        long j;
        BufferedReader bufferedReader;
        Throwable th;
        long j2;
        long j3;
        long j4;
        long j5;
        long j6;
        boolean z;
        long jValueAt;
        long jValueAt2;
        int iAllowThreadDiskReadsMask = StrictMode.allowThreadDiskReadsMask();
        long j7 = 1000;
        long jElapsedRealtime = SystemClock.elapsedRealtime() * 1000;
        try {
            try {
                try {
                    bufferedReader = new BufferedReader(new FileReader(sProcFile));
                } catch (Throwable th2) {
                    StrictMode.setThreadPolicyMask(iAllowThreadDiskReadsMask);
                    throw th2;
                }
            } catch (IOException e) {
                e = e;
                j = jElapsedRealtime;
                Slog.e(TAG, "Failed to read uid_cputime: " + e.getMessage());
                StrictMode.setThreadPolicyMask(iAllowThreadDiskReadsMask);
                this.mLastTimeReadUs = j;
            }
        } catch (IOException e2) {
            e = e2;
            Slog.e(TAG, "Failed to read uid_cputime: " + e.getMessage());
            StrictMode.setThreadPolicyMask(iAllowThreadDiskReadsMask);
            this.mLastTimeReadUs = j;
        }
        try {
            TextUtils.SimpleStringSplitter simpleStringSplitter = new TextUtils.SimpleStringSplitter(' ');
            while (true) {
                String line = bufferedReader.readLine();
                if (line == null) {
                    break;
                }
                simpleStringSplitter.setString(line);
                String next = simpleStringSplitter.next();
                int i = Integer.parseInt(next.substring(0, next.length() - 1), 10);
                long j8 = Long.parseLong(simpleStringSplitter.next(), 10);
                long j9 = Long.parseLong(simpleStringSplitter.next(), 10);
                try {
                    if (callback == null) {
                        j2 = jElapsedRealtime;
                        j3 = j7;
                        j4 = j9;
                    } else if (this.mLastTimeReadUs != 0) {
                        int iIndexOfKey = this.mLastUserTimeUs.indexOfKey(i);
                        if (iIndexOfKey >= 0) {
                            jValueAt = j8 - this.mLastUserTimeUs.valueAt(iIndexOfKey);
                            jValueAt2 = j9 - this.mLastSystemTimeUs.valueAt(iIndexOfKey);
                            j4 = j9;
                            long j10 = jElapsedRealtime - this.mLastTimeReadUs;
                            if (jValueAt < 0 || jValueAt2 < 0) {
                                StringBuilder sb = new StringBuilder("Malformed cpu data for UID=");
                                sb.append(i);
                                sb.append("!\n");
                                sb.append("Time between reads: ");
                                TimeUtils.formatDuration(j10 / 1000, sb);
                                sb.append("\n");
                                sb.append("Previous times: u=");
                                TimeUtils.formatDuration(this.mLastUserTimeUs.valueAt(iIndexOfKey) / 1000, sb);
                                sb.append(" s=");
                                TimeUtils.formatDuration(this.mLastSystemTimeUs.valueAt(iIndexOfKey) / 1000, sb);
                                sb.append("\nCurrent times: u=");
                                j2 = jElapsedRealtime;
                                TimeUtils.formatDuration(j8 / 1000, sb);
                                sb.append(" s=");
                                TimeUtils.formatDuration(j4 / 1000, sb);
                                sb.append("\nDelta: u=");
                                TimeUtils.formatDuration(jValueAt / 1000, sb);
                                sb.append(" s=");
                                j3 = 1000;
                                TimeUtils.formatDuration(jValueAt2 / 1000, sb);
                                Slog.e(TAG, sb.toString());
                                jValueAt = 0;
                                jValueAt2 = 0;
                            } else {
                                j2 = jElapsedRealtime;
                                j3 = 1000;
                            }
                        } else {
                            j2 = jElapsedRealtime;
                            j4 = j9;
                            j3 = 1000;
                            jValueAt = j8;
                            jValueAt2 = j4;
                        }
                        z = (jValueAt == 0 && jValueAt2 == 0) ? false : true;
                        j5 = jValueAt;
                        j6 = jValueAt2;
                        this.mLastUserTimeUs.put(i, j8);
                        this.mLastSystemTimeUs.put(i, j4);
                        if (!z) {
                            callback.onUidCpuTime(i, j5, j6);
                        }
                        j7 = j3;
                        jElapsedRealtime = j2;
                    } else {
                        j2 = jElapsedRealtime;
                        j4 = j9;
                        j3 = 1000;
                    }
                    this.mLastUserTimeUs.put(i, j8);
                    this.mLastSystemTimeUs.put(i, j4);
                    if (!z) {
                    }
                    j7 = j3;
                    jElapsedRealtime = j2;
                } catch (Throwable th3) {
                    th = th3;
                    Throwable th4 = th;
                    try {
                        throw th4;
                    } catch (Throwable th5) {
                        th = th5;
                        th = th4;
                        $closeResource(th, bufferedReader);
                        throw th;
                    }
                }
                j5 = j8;
                j6 = j4;
                z = false;
            }
            j = jElapsedRealtime;
            $closeResource(null, bufferedReader);
            StrictMode.setThreadPolicyMask(iAllowThreadDiskReadsMask);
            this.mLastTimeReadUs = j;
        } catch (Throwable th6) {
            th = th6;
        }
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    public void readAbsolute(Callback callback) {
        BufferedReader bufferedReader;
        Throwable th;
        int iAllowThreadDiskReadsMask = StrictMode.allowThreadDiskReadsMask();
        try {
            try {
                bufferedReader = new BufferedReader(new FileReader(sProcFile));
                th = null;
            } catch (IOException e) {
                Slog.e(TAG, "Failed to read uid_cputime: " + e.getMessage());
            }
            try {
                try {
                    TextUtils.SimpleStringSplitter simpleStringSplitter = new TextUtils.SimpleStringSplitter(' ');
                    while (true) {
                        String line = bufferedReader.readLine();
                        if (line == null) {
                            break;
                        }
                        simpleStringSplitter.setString(line);
                        callback.onUidCpuTime(Integer.parseInt(simpleStringSplitter.next().substring(0, r4.length() - 1), 10), Long.parseLong(simpleStringSplitter.next(), 10), Long.parseLong(simpleStringSplitter.next(), 10));
                    }
                } finally {
                }
            } finally {
                $closeResource(th, bufferedReader);
            }
        } finally {
            StrictMode.setThreadPolicyMask(iAllowThreadDiskReadsMask);
        }
    }

    public void removeUid(int i) {
        int iIndexOfKey = this.mLastSystemTimeUs.indexOfKey(i);
        if (iIndexOfKey >= 0) {
            this.mLastSystemTimeUs.removeAt(iIndexOfKey);
            this.mLastUserTimeUs.removeAt(iIndexOfKey);
        }
        removeUidsFromKernelModule(i, i);
    }

    public void removeUidsInRange(int i, int i2) {
        if (i2 < i) {
            return;
        }
        this.mLastSystemTimeUs.put(i, 0L);
        this.mLastUserTimeUs.put(i, 0L);
        this.mLastSystemTimeUs.put(i2, 0L);
        this.mLastUserTimeUs.put(i2, 0L);
        int iIndexOfKey = this.mLastSystemTimeUs.indexOfKey(i);
        int iIndexOfKey2 = (this.mLastSystemTimeUs.indexOfKey(i2) - iIndexOfKey) + 1;
        this.mLastSystemTimeUs.removeAtRange(iIndexOfKey, iIndexOfKey2);
        this.mLastUserTimeUs.removeAtRange(iIndexOfKey, iIndexOfKey2);
        removeUidsFromKernelModule(i, i2);
    }

    private void removeUidsFromKernelModule(int i, int i2) {
        Slog.d(TAG, "Removing uids " + i + NativeLibraryHelper.CLEAR_ABI_OVERRIDE + i2);
        int iAllowThreadDiskWritesMask = StrictMode.allowThreadDiskWritesMask();
        try {
            try {
                FileWriter fileWriter = new FileWriter(sRemoveUidProcFile);
                Throwable th = null;
                try {
                    fileWriter.write(i + NativeLibraryHelper.CLEAR_ABI_OVERRIDE + i2);
                    fileWriter.flush();
                } finally {
                    $closeResource(th, fileWriter);
                }
            } finally {
                StrictMode.setThreadPolicyMask(iAllowThreadDiskWritesMask);
            }
        } catch (IOException e) {
            Slog.e(TAG, "failed to remove uids " + i + " - " + i2 + " from uid_cputime module", e);
        }
    }
}
