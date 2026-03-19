package com.android.server.usage;

import android.app.usage.ConfigurationStats;
import android.app.usage.TimeSparseArray;
import android.content.res.Configuration;
import android.os.Build;
import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.TimeUtils;
import com.android.server.job.controllers.JobStatus;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class UsageStatsDatabase {
    static final int BACKUP_VERSION = 1;
    private static final String BAK_SUFFIX = ".bak";
    private static final String CHECKED_IN_SUFFIX = "-c";
    private static final int CURRENT_VERSION = 3;
    private static final boolean DEBUG = false;
    static final String KEY_USAGE_STATS = "usage_stats";
    private static final String TAG = "UsageStatsDatabase";
    private boolean mFirstUpdate;
    private final File[] mIntervalDirs;
    private boolean mNewUpdate;
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
            for (File f : this.mIntervalDirs) {
                f.mkdirs();
                if (!f.exists()) {
                    throw new IllegalStateException("Failed to create directory " + f.getAbsolutePath());
                }
            }
            checkVersionAndBuildLocked();
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
                        return false;
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
                return false;
            }
        }
    }

    private void indexFilesLocked() {
        FilenameFilter backupFileFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return !name.endsWith(UsageStatsDatabase.BAK_SUFFIX);
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
                    try {
                        this.mSortedStatFiles[i].put(UsageStatsXml.parseBeginTime(af), af);
                    } catch (IOException e) {
                        Slog.e(TAG, "failed to index file: " + f, e);
                    }
                }
            }
        }
    }

    boolean isFirstUpdate() {
        return this.mFirstUpdate;
    }

    boolean isNewUpdate() {
        return this.mNewUpdate;
    }

    private void checkVersionAndBuildLocked() throws Throwable {
        Throwable th;
        int version;
        BufferedWriter writer;
        Throwable th2 = null;
        String currentFingerprint = getBuildFingerprint();
        this.mFirstUpdate = true;
        this.mNewUpdate = true;
        BufferedReader reader = null;
        try {
            BufferedReader reader2 = new BufferedReader(new FileReader(this.mVersionFile));
            try {
                version = Integer.parseInt(reader2.readLine());
                String buildFingerprint = reader2.readLine();
                if (buildFingerprint != null) {
                    this.mFirstUpdate = false;
                }
                if (currentFingerprint.equals(buildFingerprint)) {
                    this.mNewUpdate = false;
                }
                if (reader2 != null) {
                    try {
                        try {
                            reader2.close();
                            th = null;
                        } catch (Throwable th3) {
                            th = th3;
                        }
                        if (th == null) {
                            throw th;
                        }
                    } catch (IOException | NumberFormatException e) {
                        version = 0;
                    }
                } else {
                    th = null;
                    if (th == null) {
                    }
                }
                if (version != 3) {
                    Slog.i(TAG, "Upgrading from version " + version + " to 3");
                    doUpgradeLocked(version);
                }
                if (version != 3 && !this.mNewUpdate) {
                    return;
                }
                BufferedWriter writer2 = null;
                try {
                    writer = new BufferedWriter(new FileWriter(this.mVersionFile));
                } catch (Throwable th4) {
                    th = th4;
                }
                try {
                    writer.write(Integer.toString(3));
                    writer.write("\n");
                    writer.write(currentFingerprint);
                    writer.write("\n");
                    writer.flush();
                    if (writer != null) {
                        try {
                            try {
                                writer.close();
                            } catch (Throwable th5) {
                                th2 = th5;
                            }
                        } catch (IOException e2) {
                            e = e2;
                            Slog.e(TAG, "Failed to write new version");
                            throw new RuntimeException(e);
                        }
                    }
                    if (th2 == null) {
                        throw th2;
                    }
                } catch (Throwable th6) {
                    th = th6;
                    writer2 = writer;
                    try {
                        throw th;
                    } catch (Throwable th7) {
                        th2 = th;
                        th = th7;
                        if (writer2 != null) {
                            try {
                                try {
                                    writer2.close();
                                } catch (Throwable th8) {
                                    if (th2 == null) {
                                        th2 = th8;
                                    } else if (th2 != th8) {
                                        th2.addSuppressed(th8);
                                    }
                                }
                            } catch (IOException e3) {
                                e = e3;
                                Slog.e(TAG, "Failed to write new version");
                                throw new RuntimeException(e);
                            }
                        }
                        if (th2 != null) {
                            throw th;
                        }
                        throw th2;
                    }
                }
            } catch (Throwable th9) {
                th = th9;
                th = null;
                reader = reader2;
                if (reader != null) {
                }
                if (th != null) {
                }
            }
        } catch (Throwable th10) {
            th = th10;
        }
    }

    private String getBuildFingerprint() {
        return Build.VERSION.RELEASE + ";" + Build.VERSION.CODENAME + ";" + Build.VERSION.INCREMENTAL;
    }

    private void doUpgradeLocked(int thisVersion) {
        if (thisVersion >= 2) {
            return;
        }
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

    public void onTimeChanged(long timeDiffMillis) {
        synchronized (this.mLock) {
            StringBuilder logBuilder = new StringBuilder();
            logBuilder.append("Time changed by ");
            TimeUtils.formatDuration(timeDiffMillis, logBuilder);
            logBuilder.append(".");
            int filesDeleted = 0;
            int filesMoved = 0;
            for (TimeSparseArray<AtomicFile> files : this.mSortedStatFiles) {
                int fileCount = files.size();
                for (int i = 0; i < fileCount; i++) {
                    AtomicFile file = (AtomicFile) files.valueAt(i);
                    long newTime = files.keyAt(i) + timeDiffMillis;
                    if (newTime < 0) {
                        filesDeleted++;
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
                        filesMoved++;
                        file.getBaseFile().renameTo(newFile);
                    }
                }
                files.clear();
            }
            logBuilder.append(" files deleted: ").append(filesDeleted);
            logBuilder.append(" files moved: ").append(filesMoved);
            Slog.i(TAG, logBuilder.toString());
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

    public <T> List<T> queryUsageStats(int intervalType, long beginTime, long endTime, StatCombiner<T> combiner) {
        synchronized (this.mLock) {
            if (intervalType >= 0) {
                if (intervalType < this.mIntervalDirs.length) {
                    TimeSparseArray<AtomicFile> intervalStats = this.mSortedStatFiles[intervalType];
                    if (endTime <= beginTime) {
                        return null;
                    }
                    int startIndex = intervalStats.closestIndexOnOrBefore(beginTime);
                    if (startIndex < 0) {
                        startIndex = 0;
                    }
                    int endIndex = intervalStats.closestIndexOnOrBefore(endTime);
                    if (endIndex < 0) {
                        return null;
                    }
                    if (intervalStats.keyAt(endIndex) == endTime && endIndex - 1 < 0) {
                        return null;
                    }
                    IntervalStats stats = new IntervalStats();
                    ArrayList<T> results = new ArrayList<>();
                    for (int i = startIndex; i <= endIndex; i++) {
                        AtomicFile f = (AtomicFile) intervalStats.valueAt(i);
                        try {
                            UsageStatsXml.read(f, stats);
                            if (beginTime < stats.endTime) {
                                combiner.combine(stats, false, results);
                            }
                        } catch (IOException e) {
                            Slog.e(TAG, "Failed to read usage stats file", e);
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
            indexFilesLocked();
        }
    }

    private static void pruneFilesOlderThan(File dir, long expiryTime) {
        long beginTime;
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        int length = files.length;
        for (int i = 0; i < length; i++) {
            File f = files[i];
            String path = f.getPath();
            if (path.endsWith(BAK_SUFFIX)) {
                f = new File(path.substring(0, path.length() - BAK_SUFFIX.length()));
            }
            try {
                beginTime = UsageStatsXml.parseBeginTime(f);
            } catch (IOException e) {
                beginTime = 0;
            }
            if (beginTime < expiryTime) {
                new AtomicFile(f).delete();
            }
        }
    }

    public void putUsageStats(int intervalType, IntervalStats stats) throws IOException {
        if (stats == null) {
            return;
        }
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

    byte[] getBackupPayload(String key) {
        byte[] byteArray;
        synchronized (this.mLock) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (KEY_USAGE_STATS.equals(key)) {
                prune(System.currentTimeMillis());
                DataOutputStream out = new DataOutputStream(baos);
                try {
                    out.writeInt(1);
                    out.writeInt(this.mSortedStatFiles[0].size());
                    for (int i = 0; i < this.mSortedStatFiles[0].size(); i++) {
                        writeIntervalStatsToStream(out, (AtomicFile) this.mSortedStatFiles[0].valueAt(i));
                    }
                    out.writeInt(this.mSortedStatFiles[1].size());
                    for (int i2 = 0; i2 < this.mSortedStatFiles[1].size(); i2++) {
                        writeIntervalStatsToStream(out, (AtomicFile) this.mSortedStatFiles[1].valueAt(i2));
                    }
                    out.writeInt(this.mSortedStatFiles[2].size());
                    for (int i3 = 0; i3 < this.mSortedStatFiles[2].size(); i3++) {
                        writeIntervalStatsToStream(out, (AtomicFile) this.mSortedStatFiles[2].valueAt(i3));
                    }
                    out.writeInt(this.mSortedStatFiles[3].size());
                    for (int i4 = 0; i4 < this.mSortedStatFiles[3].size(); i4++) {
                        writeIntervalStatsToStream(out, (AtomicFile) this.mSortedStatFiles[3].valueAt(i4));
                    }
                } catch (IOException ioe) {
                    Slog.d(TAG, "Failed to write data to output stream", ioe);
                    baos.reset();
                }
                byteArray = baos.toByteArray();
            } else {
                byteArray = baos.toByteArray();
            }
        }
        return byteArray;
    }

    void applyRestoredPayload(String key, byte[] payload) {
        synchronized (this.mLock) {
            if (KEY_USAGE_STATS.equals(key)) {
                IntervalStats dailyConfigSource = getLatestUsageStats(0);
                IntervalStats weeklyConfigSource = getLatestUsageStats(1);
                IntervalStats monthlyConfigSource = getLatestUsageStats(2);
                IntervalStats yearlyConfigSource = getLatestUsageStats(3);
                try {
                    try {
                        DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload));
                        int backupDataVersion = in.readInt();
                        if (backupDataVersion < 1 || backupDataVersion > 1) {
                            return;
                        }
                        for (int i = 0; i < this.mIntervalDirs.length; i++) {
                            deleteDirectoryContents(this.mIntervalDirs[i]);
                        }
                        int fileCount = in.readInt();
                        for (int i2 = 0; i2 < fileCount; i2++) {
                            IntervalStats stats = deserializeIntervalStats(getIntervalStatsBytes(in));
                            putUsageStats(0, mergeStats(stats, dailyConfigSource));
                        }
                        int fileCount2 = in.readInt();
                        for (int i3 = 0; i3 < fileCount2; i3++) {
                            IntervalStats stats2 = deserializeIntervalStats(getIntervalStatsBytes(in));
                            putUsageStats(1, mergeStats(stats2, weeklyConfigSource));
                        }
                        int fileCount3 = in.readInt();
                        for (int i4 = 0; i4 < fileCount3; i4++) {
                            IntervalStats stats3 = deserializeIntervalStats(getIntervalStatsBytes(in));
                            putUsageStats(2, mergeStats(stats3, monthlyConfigSource));
                        }
                        int fileCount4 = in.readInt();
                        for (int i5 = 0; i5 < fileCount4; i5++) {
                            IntervalStats stats4 = deserializeIntervalStats(getIntervalStatsBytes(in));
                            putUsageStats(3, mergeStats(stats4, yearlyConfigSource));
                        }
                    } catch (IOException ioe) {
                        Slog.d(TAG, "Failed to read data from input stream", ioe);
                        indexFilesLocked();
                    }
                } finally {
                    indexFilesLocked();
                }
                indexFilesLocked();
            }
        }
    }

    private IntervalStats mergeStats(IntervalStats beingRestored, IntervalStats onDevice) {
        if (onDevice == null) {
            return beingRestored;
        }
        if (beingRestored == null) {
            return null;
        }
        beingRestored.activeConfiguration = onDevice.activeConfiguration;
        beingRestored.configurations.putAll((ArrayMap<? extends Configuration, ? extends ConfigurationStats>) onDevice.configurations);
        beingRestored.events = onDevice.events;
        return beingRestored;
    }

    private void writeIntervalStatsToStream(DataOutputStream out, AtomicFile statsFile) throws IOException {
        IntervalStats stats = new IntervalStats();
        try {
            UsageStatsXml.read(statsFile, stats);
            sanitizeIntervalStatsForBackup(stats);
            byte[] data = serializeIntervalStats(stats);
            out.writeInt(data.length);
            out.write(data);
        } catch (IOException e) {
            Slog.e(TAG, "Failed to read usage stats file", e);
            out.writeInt(0);
        }
    }

    private static byte[] getIntervalStatsBytes(DataInputStream in) throws IOException {
        int length = in.readInt();
        byte[] buffer = new byte[length];
        in.read(buffer, 0, length);
        return buffer;
    }

    private static void sanitizeIntervalStatsForBackup(IntervalStats stats) {
        if (stats == null) {
            return;
        }
        stats.activeConfiguration = null;
        stats.configurations.clear();
        if (stats.events != null) {
            stats.events.clear();
        }
    }

    private static byte[] serializeIntervalStats(IntervalStats stats) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        try {
            out.writeLong(stats.beginTime);
            UsageStatsXml.write(out, stats);
        } catch (IOException ioe) {
            Slog.d(TAG, "Serializing IntervalStats Failed", ioe);
            baos.reset();
        }
        return baos.toByteArray();
    }

    private static IntervalStats deserializeIntervalStats(byte[] data) {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream in = new DataInputStream(bais);
        IntervalStats stats = new IntervalStats();
        try {
            stats.beginTime = in.readLong();
            UsageStatsXml.read(in, stats);
            return stats;
        } catch (IOException ioe) {
            Slog.d(TAG, "DeSerializing IntervalStats Failed", ioe);
            return null;
        }
    }

    private static void deleteDirectoryContents(File directory) {
        File[] files = directory.listFiles();
        for (File file : files) {
            deleteDirectory(file);
        }
    }

    private static void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.isDirectory()) {
                    file.delete();
                } else {
                    deleteDirectory(file);
                }
            }
        }
        directory.delete();
    }
}
