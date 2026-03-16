package com.android.server.usage;

import android.app.usage.TimeSparseArray;
import android.util.AtomicFile;
import android.util.Slog;
import com.android.server.job.controllers.JobStatus;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class UsageStatsDatabase {
    private static final String BAK_SUFFIX = ".bak";
    private static final String CHECKED_IN_SUFFIX = "-c";
    private static final int CURRENT_VERSION = 2;
    private static final boolean DEBUG = false;
    private static final String TAG = "UsageStatsDatabase";
    private final File[] mIntervalDirs;
    private final TimeSparseArray<AtomicFile>[] mSortedStatFiles;
    private final File mVersionFile;
    private final Object mLock = new Object();
    private final UnixCalendar mCal = new UnixCalendar(0);

    public interface CheckinAction {
        boolean checkin(IntervalStats intervalStats);
    }

    interface StatCombiner<T> {
        void combine(IntervalStats intervalStats, boolean z, List<T> list);
    }

    public UsageStatsDatabase(File dir) {
        this.mIntervalDirs = new File[]{new File(dir, "daily"), new File(dir, "weekly"), new File(dir, "monthly"), new File(dir, "yearly")};
        this.mVersionFile = new File(dir, "version");
        this.mSortedStatFiles = new TimeSparseArray[this.mIntervalDirs.length];
    }

    public void init(long currentTimeMillis) {
        synchronized (this.mLock) {
            File[] arr$ = this.mIntervalDirs;
            for (File f : arr$) {
                f.mkdirs();
                if (!f.exists()) {
                    throw new IllegalStateException("Failed to create directory " + f.getAbsolutePath());
                }
            }
            checkVersionLocked();
            indexFilesLocked();
            for (TimeSparseArray<AtomicFile> files : this.mSortedStatFiles) {
                int startIndex = files.closestIndexOnOrAfter(currentTimeMillis);
                if (startIndex >= 0) {
                    int fileCount = files.size();
                    for (int i = startIndex; i < fileCount; i++) {
                        ((AtomicFile) files.valueAt(i)).delete();
                    }
                    for (int i2 = startIndex; i2 < fileCount; i2++) {
                        files.removeAt(i2);
                    }
                }
            }
        }
    }

    public boolean checkinDailyFiles(CheckinAction checkinAction) {
        synchronized (this.mLock) {
            TimeSparseArray<AtomicFile> files = this.mSortedStatFiles[0];
            int fileCount = files.size();
            int lastCheckin = -1;
            for (int i = 0; i < fileCount - 1; i++) {
                if (((AtomicFile) files.valueAt(i)).getBaseFile().getPath().endsWith(CHECKED_IN_SUFFIX)) {
                    lastCheckin = i;
                }
            }
            int start = lastCheckin + 1;
            if (start == fileCount - 1) {
                return true;
            }
            try {
                IntervalStats stats = new IntervalStats();
                for (int i2 = start; i2 < fileCount - 1; i2++) {
                    UsageStatsXml.read((AtomicFile) files.valueAt(i2), stats);
                    if (!checkinAction.checkin(stats)) {
                        return DEBUG;
                    }
                }
                for (int i3 = start; i3 < fileCount - 1; i3++) {
                    AtomicFile file = (AtomicFile) files.valueAt(i3);
                    File checkedInFile = new File(file.getBaseFile().getPath() + CHECKED_IN_SUFFIX);
                    if (!file.getBaseFile().renameTo(checkedInFile)) {
                        Slog.e(TAG, "Failed to mark file " + file.getBaseFile().getPath() + " as checked-in");
                        return true;
                    }
                    files.setValueAt(i3, new AtomicFile(checkedInFile));
                }
                return true;
            } catch (IOException e) {
                Slog.e(TAG, "Failed to check-in", e);
                return DEBUG;
            }
        }
    }

    private void indexFilesLocked() {
        FilenameFilter backupFileFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if (name.endsWith(UsageStatsDatabase.BAK_SUFFIX)) {
                    return UsageStatsDatabase.DEBUG;
                }
                return true;
            }
        };
        for (int i = 0; i < this.mSortedStatFiles.length; i++) {
            if (this.mSortedStatFiles[i] == null) {
                this.mSortedStatFiles[i] = new TimeSparseArray<>();
            } else {
                this.mSortedStatFiles[i].clear();
            }
            File[] files = this.mIntervalDirs[i].listFiles(backupFileFilter);
            if (files != null) {
                for (File f : files) {
                    AtomicFile af = new AtomicFile(f);
                    this.mSortedStatFiles[i].put(UsageStatsXml.parseBeginTime(af), af);
                }
            }
        }
    }

    private void checkVersionLocked() throws Throwable {
        int version;
        BufferedReader reader;
        Throwable th;
        Throwable th2;
        Throwable th3;
        Throwable th4 = null;
        try {
            reader = new BufferedReader(new FileReader(this.mVersionFile));
            th = null;
        } catch (IOException | NumberFormatException e) {
            version = 0;
        }
        try {
            version = Integer.parseInt(reader.readLine());
            if (reader != null) {
                if (0 != 0) {
                    try {
                        reader.close();
                    } catch (Throwable x2) {
                        th.addSuppressed(x2);
                    }
                } else {
                    reader.close();
                }
            }
            if (version != 2) {
                Slog.i(TAG, "Upgrading from version " + version + " to 2");
                doUpgradeLocked(version);
                try {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(this.mVersionFile));
                    Throwable th5 = null;
                    try {
                        writer.write(Integer.toString(2));
                        if (writer != null) {
                            if (0 == 0) {
                                writer.close();
                                return;
                            }
                            try {
                                writer.close();
                            } catch (Throwable x22) {
                                th5.addSuppressed(x22);
                            }
                        }
                    } catch (Throwable th6) {
                        th = th6;
                        if (writer != null) {
                        }
                        throw th;
                    }
                } catch (IOException e2) {
                    Slog.e(TAG, "Failed to write new version");
                    throw new RuntimeException(e2);
                }
            }
        } catch (Throwable th7) {
            try {
                throw th7;
            } catch (Throwable th8) {
                th2 = th7;
                th3 = th8;
                if (reader != null) {
                    throw th3;
                }
                if (th2 == null) {
                    reader.close();
                    throw th3;
                }
                try {
                    reader.close();
                    throw th3;
                } catch (Throwable x23) {
                    th2.addSuppressed(x23);
                    throw th3;
                }
            }
        }
    }

    private void doUpgradeLocked(int thisVersion) {
        if (thisVersion < 2) {
            Slog.i(TAG, "Deleting all usage stats files");
            for (int i = 0; i < this.mIntervalDirs.length; i++) {
                File[] files = this.mIntervalDirs[i].listFiles();
                if (files != null) {
                    for (File f : files) {
                        f.delete();
                    }
                }
            }
        }
    }

    public void onTimeChanged(long timeDiffMillis) {
        synchronized (this.mLock) {
            for (TimeSparseArray<AtomicFile> files : this.mSortedStatFiles) {
                int fileCount = files.size();
                for (int i = 0; i < fileCount; i++) {
                    AtomicFile file = (AtomicFile) files.valueAt(i);
                    long newTime = files.keyAt(i) + timeDiffMillis;
                    if (newTime < 0) {
                        Slog.i(TAG, "Deleting file " + file.getBaseFile().getAbsolutePath() + " for it is in the future now.");
                        file.delete();
                    } else {
                        try {
                            file.openRead().close();
                        } catch (IOException e) {
                        }
                        String newName = Long.toString(newTime);
                        if (file.getBaseFile().getName().endsWith(CHECKED_IN_SUFFIX)) {
                            newName = newName + CHECKED_IN_SUFFIX;
                        }
                        File newFile = new File(file.getBaseFile().getParentFile(), newName);
                        Slog.i(TAG, "Moving file " + file.getBaseFile().getAbsolutePath() + " to " + newFile.getAbsolutePath());
                        file.getBaseFile().renameTo(newFile);
                    }
                }
                files.clear();
            }
            indexFilesLocked();
        }
    }

    public IntervalStats getLatestUsageStats(int intervalType) {
        synchronized (this.mLock) {
            if (intervalType >= 0) {
                if (intervalType < this.mIntervalDirs.length) {
                    int fileCount = this.mSortedStatFiles[intervalType].size();
                    if (fileCount == 0) {
                        return null;
                    }
                    try {
                        AtomicFile f = (AtomicFile) this.mSortedStatFiles[intervalType].valueAt(fileCount - 1);
                        IntervalStats stats = new IntervalStats();
                        UsageStatsXml.read(f, stats);
                        return stats;
                    } catch (IOException e) {
                        Slog.e(TAG, "Failed to read usage stats file", e);
                        return null;
                    }
                }
            }
            throw new IllegalArgumentException("Bad interval type " + intervalType);
        }
    }

    public long getLatestUsageStatsBeginTime(int intervalType) {
        synchronized (this.mLock) {
            if (intervalType >= 0) {
                if (intervalType < this.mIntervalDirs.length) {
                    int statsFileCount = this.mSortedStatFiles[intervalType].size();
                    return statsFileCount > 0 ? this.mSortedStatFiles[intervalType].keyAt(statsFileCount - 1) : -1L;
                }
            }
            throw new IllegalArgumentException("Bad interval type " + intervalType);
        }
    }

    public <T> List<T> queryUsageStats(int intervalType, long beginTime, long endTime, StatCombiner<T> combiner) {
        ArrayList<T> results;
        synchronized (this.mLock) {
            if (intervalType >= 0) {
                if (intervalType < this.mIntervalDirs.length) {
                    TimeSparseArray<AtomicFile> intervalStats = this.mSortedStatFiles[intervalType];
                    if (endTime <= beginTime) {
                        results = null;
                    } else {
                        int startIndex = intervalStats.closestIndexOnOrBefore(beginTime);
                        if (startIndex < 0) {
                            startIndex = 0;
                        }
                        int endIndex = intervalStats.closestIndexOnOrBefore(endTime);
                        if (endIndex < 0) {
                            results = null;
                        } else if (intervalStats.keyAt(endIndex) == endTime && endIndex - 1 < 0) {
                            results = null;
                        } else {
                            try {
                                IntervalStats stats = new IntervalStats();
                                results = new ArrayList<>();
                                for (int i = startIndex; i <= endIndex; i++) {
                                    AtomicFile f = (AtomicFile) intervalStats.valueAt(i);
                                    UsageStatsXml.read(f, stats);
                                    if (beginTime < stats.endTime) {
                                        combiner.combine(stats, DEBUG, results);
                                    }
                                }
                            } catch (IOException e) {
                                Slog.e(TAG, "Failed to read usage stats file", e);
                                results = null;
                            }
                        }
                    }
                    return results;
                }
            }
            throw new IllegalArgumentException("Bad interval type " + intervalType);
        }
    }

    public int findBestFitBucket(long beginTimeStamp, long endTimeStamp) {
        int bestBucket;
        synchronized (this.mLock) {
            bestBucket = -1;
            long smallestDiff = JobStatus.NO_LATEST_RUNTIME;
            for (int i = this.mSortedStatFiles.length - 1; i >= 0; i--) {
                int index = this.mSortedStatFiles[i].closestIndexOnOrBefore(beginTimeStamp);
                int size = this.mSortedStatFiles[i].size();
                if (index >= 0 && index < size) {
                    long diff = Math.abs(this.mSortedStatFiles[i].keyAt(index) - beginTimeStamp);
                    if (diff < smallestDiff) {
                        smallestDiff = diff;
                        bestBucket = i;
                    }
                }
            }
        }
        return bestBucket;
    }

    public void prune(long currentTimeMillis) {
        synchronized (this.mLock) {
            this.mCal.setTimeInMillis(currentTimeMillis);
            this.mCal.addYears(-3);
            pruneFilesOlderThan(this.mIntervalDirs[3], this.mCal.getTimeInMillis());
            this.mCal.setTimeInMillis(currentTimeMillis);
            this.mCal.addMonths(-6);
            pruneFilesOlderThan(this.mIntervalDirs[2], this.mCal.getTimeInMillis());
            this.mCal.setTimeInMillis(currentTimeMillis);
            this.mCal.addWeeks(-4);
            pruneFilesOlderThan(this.mIntervalDirs[1], this.mCal.getTimeInMillis());
            this.mCal.setTimeInMillis(currentTimeMillis);
            this.mCal.addDays(-7);
            pruneFilesOlderThan(this.mIntervalDirs[0], this.mCal.getTimeInMillis());
        }
    }

    private static void pruneFilesOlderThan(File dir, long expiryTime) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                String path = f.getPath();
                if (path.endsWith(BAK_SUFFIX)) {
                    f = new File(path.substring(0, path.length() - BAK_SUFFIX.length()));
                }
                long beginTime = UsageStatsXml.parseBeginTime(f);
                if (beginTime < expiryTime) {
                    new AtomicFile(f).delete();
                }
            }
        }
    }

    public void putUsageStats(int intervalType, IntervalStats stats) throws IOException {
        synchronized (this.mLock) {
            if (intervalType >= 0) {
                if (intervalType < this.mIntervalDirs.length) {
                    AtomicFile f = (AtomicFile) this.mSortedStatFiles[intervalType].get(stats.beginTime);
                    if (f == null) {
                        f = new AtomicFile(new File(this.mIntervalDirs[intervalType], Long.toString(stats.beginTime)));
                        this.mSortedStatFiles[intervalType].put(stats.beginTime, f);
                    }
                    UsageStatsXml.write(f, stats);
                    stats.lastTimeSaved = f.getLastModifiedTime();
                }
            }
            throw new IllegalArgumentException("Bad interval type " + intervalType);
        }
    }
}
