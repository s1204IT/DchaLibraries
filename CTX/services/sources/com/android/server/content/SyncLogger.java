package com.android.server.content;

import android.app.job.JobParameters;
import android.os.Build;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Process;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.server.backup.BackupManagerConstants;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import libcore.io.IoUtils;

public class SyncLogger {
    public static final int CALLING_UID_SELF = -1;
    private static final String TAG = "SyncLogger";
    private static SyncLogger sInstance;

    SyncLogger() {
    }

    public static synchronized SyncLogger getInstance() {
        if (sInstance == null) {
            if (Build.IS_DEBUGGABLE || "1".equals(SystemProperties.get("debug.synclog")) || Log.isLoggable(TAG, 2)) {
                sInstance = new RotatingFileLogger();
            } else {
                sInstance = new SyncLogger();
            }
        }
        return sInstance;
    }

    public void log(Object... objArr) {
    }

    public void purgeOldLogs() {
    }

    public String jobParametersToString(JobParameters jobParameters) {
        return BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
    }

    public void dumpAll(PrintWriter printWriter) {
    }

    public boolean enabled() {
        return false;
    }

    private static class RotatingFileLogger extends SyncLogger {

        @GuardedBy("mLock")
        private long mCurrentLogFileDayTimestamp;

        @GuardedBy("mLock")
        private boolean mErrorShown;

        @GuardedBy("mLock")
        private Writer mLogWriter;
        private static final SimpleDateFormat sTimestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        private static final SimpleDateFormat sFilenameDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        private static final boolean DO_LOGCAT = Log.isLoggable(SyncLogger.TAG, 3);
        private final Object mLock = new Object();
        private final long mKeepAgeMs = TimeUnit.DAYS.toMillis(7);

        @GuardedBy("mLock")
        private final Date mCachedDate = new Date();

        @GuardedBy("mLock")
        private final StringBuilder mStringBuilder = new StringBuilder();
        private final File mLogPath = new File(Environment.getDataSystemDirectory(), "syncmanager-log");

        RotatingFileLogger() {
        }

        @Override
        public boolean enabled() {
            return true;
        }

        private void handleException(String str, Exception exc) {
            if (!this.mErrorShown) {
                Slog.e(SyncLogger.TAG, str, exc);
                this.mErrorShown = true;
            }
        }

        @Override
        public void log(Object... objArr) {
            if (objArr == null) {
                return;
            }
            synchronized (this.mLock) {
                long jCurrentTimeMillis = System.currentTimeMillis();
                openLogLocked(jCurrentTimeMillis);
                if (this.mLogWriter == null) {
                    return;
                }
                this.mStringBuilder.setLength(0);
                this.mCachedDate.setTime(jCurrentTimeMillis);
                this.mStringBuilder.append(sTimestampFormat.format(this.mCachedDate));
                this.mStringBuilder.append(' ');
                this.mStringBuilder.append(Process.myTid());
                this.mStringBuilder.append(' ');
                int length = this.mStringBuilder.length();
                for (Object obj : objArr) {
                    this.mStringBuilder.append(obj);
                }
                this.mStringBuilder.append('\n');
                try {
                    this.mLogWriter.append((CharSequence) this.mStringBuilder);
                    this.mLogWriter.flush();
                    if (DO_LOGCAT) {
                        Log.d(SyncLogger.TAG, this.mStringBuilder.substring(length));
                    }
                } catch (IOException e) {
                    handleException("Failed to write log", e);
                }
            }
        }

        @GuardedBy("mLock")
        private void openLogLocked(long j) {
            long j2 = j % 86400000;
            if (this.mLogWriter != null && j2 == this.mCurrentLogFileDayTimestamp) {
                return;
            }
            closeCurrentLogLocked();
            this.mCurrentLogFileDayTimestamp = j2;
            this.mCachedDate.setTime(j);
            File file = new File(this.mLogPath, "synclog-" + sFilenameDateFormat.format(this.mCachedDate) + ".log");
            file.getParentFile().mkdirs();
            try {
                this.mLogWriter = new FileWriter(file, true);
            } catch (IOException e) {
                handleException("Failed to open log file: " + file, e);
            }
        }

        @GuardedBy("mLock")
        private void closeCurrentLogLocked() {
            IoUtils.closeQuietly(this.mLogWriter);
            this.mLogWriter = null;
        }

        @Override
        public void purgeOldLogs() {
            synchronized (this.mLock) {
                FileUtils.deleteOlderFiles(this.mLogPath, 1, this.mKeepAgeMs);
            }
        }

        @Override
        public String jobParametersToString(JobParameters jobParameters) {
            return SyncJobService.jobParametersToString(jobParameters);
        }

        @Override
        public void dumpAll(PrintWriter printWriter) {
            synchronized (this.mLock) {
                String[] list = this.mLogPath.list();
                if (list != null && list.length != 0) {
                    Arrays.sort(list);
                    for (String str : list) {
                        dumpFile(printWriter, new File(this.mLogPath, str));
                    }
                }
            }
        }

        private void dumpFile(PrintWriter printWriter, File file) {
            Slog.w(SyncLogger.TAG, "Dumping " + file);
            char[] cArr = new char[32768];
            try {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
                Throwable th = null;
                while (true) {
                    try {
                        try {
                            int i = bufferedReader.read(cArr);
                            if (i >= 0) {
                                if (i > 0) {
                                    printWriter.write(cArr, 0, i);
                                }
                            } else {
                                bufferedReader.close();
                                return;
                            }
                        } finally {
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        throw th;
                    }
                }
            } catch (IOException e) {
            }
        }
    }
}
