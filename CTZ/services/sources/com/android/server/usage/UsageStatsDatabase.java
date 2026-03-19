package com.android.server.usage;

import android.app.usage.ConfigurationStats;
import android.app.usage.TimeSparseArray;
import android.app.usage.UsageStats;
import android.content.res.Configuration;
import android.os.Build;
import android.os.SystemProperties;
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
    private static final String RETENTION_LEN_KEY = "ro.usagestats.chooser.retention";
    private static final int SELECTION_LOG_RETENTION_LEN = SystemProperties.getInt(RETENTION_LEN_KEY, 14);
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

    public UsageStatsDatabase(File file) {
        this.mIntervalDirs = new File[]{new File(file, "daily"), new File(file, "weekly"), new File(file, "monthly"), new File(file, "yearly")};
        this.mVersionFile = new File(file, "version");
        this.mSortedStatFiles = new TimeSparseArray[this.mIntervalDirs.length];
    }

    public void init(long j) {
        synchronized (this.mLock) {
            for (File file : this.mIntervalDirs) {
                file.mkdirs();
                if (!file.exists()) {
                    throw new IllegalStateException("Failed to create directory " + file.getAbsolutePath());
                }
            }
            checkVersionAndBuildLocked();
            indexFilesLocked();
            for (TimeSparseArray<AtomicFile> timeSparseArray : this.mSortedStatFiles) {
                int iClosestIndexOnOrAfter = timeSparseArray.closestIndexOnOrAfter(j);
                if (iClosestIndexOnOrAfter >= 0) {
                    int size = timeSparseArray.size();
                    for (int i = iClosestIndexOnOrAfter; i < size; i++) {
                        ((AtomicFile) timeSparseArray.valueAt(i)).delete();
                    }
                    while (iClosestIndexOnOrAfter < size) {
                        timeSparseArray.removeAt(iClosestIndexOnOrAfter);
                        iClosestIndexOnOrAfter++;
                    }
                }
            }
        }
    }

    public boolean checkinDailyFiles(CheckinAction checkinAction) {
        int i;
        synchronized (this.mLock) {
            TimeSparseArray<AtomicFile> timeSparseArray = this.mSortedStatFiles[0];
            int size = timeSparseArray.size();
            int i2 = -1;
            int i3 = 0;
            while (true) {
                i = size - 1;
                if (i3 >= i) {
                    break;
                }
                if (((AtomicFile) timeSparseArray.valueAt(i3)).getBaseFile().getPath().endsWith(CHECKED_IN_SUFFIX)) {
                    i2 = i3;
                }
                i3++;
            }
            int i4 = i2 + 1;
            if (i4 == i) {
                return true;
            }
            try {
                IntervalStats intervalStats = new IntervalStats();
                for (int i5 = i4; i5 < i; i5++) {
                    UsageStatsXml.read((AtomicFile) timeSparseArray.valueAt(i5), intervalStats);
                    if (!checkinAction.checkin(intervalStats)) {
                        return false;
                    }
                }
                while (i4 < i) {
                    AtomicFile atomicFile = (AtomicFile) timeSparseArray.valueAt(i4);
                    File file = new File(atomicFile.getBaseFile().getPath() + CHECKED_IN_SUFFIX);
                    if (!atomicFile.getBaseFile().renameTo(file)) {
                        Slog.e(TAG, "Failed to mark file " + atomicFile.getBaseFile().getPath() + " as checked-in");
                        return true;
                    }
                    timeSparseArray.setValueAt(i4, new AtomicFile(file));
                    i4++;
                }
                return true;
            } catch (IOException e) {
                Slog.e(TAG, "Failed to check-in", e);
                return false;
            }
        }
    }

    private void indexFilesLocked() {
        FilenameFilter filenameFilter = new FilenameFilter() {
            @Override
            public boolean accept(File file, String str) {
                return !str.endsWith(UsageStatsDatabase.BAK_SUFFIX);
            }
        };
        for (int i = 0; i < this.mSortedStatFiles.length; i++) {
            if (this.mSortedStatFiles[i] == null) {
                this.mSortedStatFiles[i] = new TimeSparseArray<>();
            } else {
                this.mSortedStatFiles[i].clear();
            }
            File[] fileArrListFiles = this.mIntervalDirs[i].listFiles(filenameFilter);
            if (fileArrListFiles != null) {
                for (File file : fileArrListFiles) {
                    AtomicFile atomicFile = new AtomicFile(file);
                    try {
                        this.mSortedStatFiles[i].put(UsageStatsXml.parseBeginTime(atomicFile), atomicFile);
                    } catch (IOException e) {
                        Slog.e(TAG, "failed to index file: " + file, e);
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

    private void checkVersionAndBuildLocked() throws Exception {
        BufferedReader bufferedReader;
        Throwable th;
        Throwable th2;
        String buildFingerprint = getBuildFingerprint();
        this.mFirstUpdate = true;
        this.mNewUpdate = true;
        int i = 0;
        try {
            bufferedReader = new BufferedReader(new FileReader(this.mVersionFile));
        } catch (IOException | NumberFormatException e) {
        }
        try {
            int i2 = Integer.parseInt(bufferedReader.readLine());
            String line = bufferedReader.readLine();
            if (line != null) {
                this.mFirstUpdate = false;
            }
            if (buildFingerprint.equals(line)) {
                this.mNewUpdate = false;
            }
            $closeResource(null, bufferedReader);
            i = i2;
            if (i != 3) {
                Slog.i(TAG, "Upgrading from version " + i + " to 3");
                doUpgradeLocked(i);
            }
            if (i != 3 || this.mNewUpdate) {
                try {
                    BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(this.mVersionFile));
                    try {
                        bufferedWriter.write(Integer.toString(3));
                        bufferedWriter.write("\n");
                        bufferedWriter.write(buildFingerprint);
                        bufferedWriter.write("\n");
                        bufferedWriter.flush();
                    } finally {
                        $closeResource(null, bufferedWriter);
                    }
                } catch (IOException e2) {
                    Slog.e(TAG, "Failed to write new version");
                    throw new RuntimeException(e2);
                }
            }
        } catch (Throwable th3) {
            try {
                throw th3;
            } catch (Throwable th4) {
                th = th3;
                th2 = th4;
                $closeResource(th, bufferedReader);
                throw th2;
            }
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

    private String getBuildFingerprint() {
        return Build.VERSION.RELEASE + ";" + Build.VERSION.CODENAME + ";" + Build.VERSION.INCREMENTAL;
    }

    private void doUpgradeLocked(int i) {
        if (i < 2) {
            Slog.i(TAG, "Deleting all usage stats files");
            for (int i2 = 0; i2 < this.mIntervalDirs.length; i2++) {
                File[] fileArrListFiles = this.mIntervalDirs[i2].listFiles();
                if (fileArrListFiles != null) {
                    for (File file : fileArrListFiles) {
                        file.delete();
                    }
                }
            }
        }
    }

    public void onTimeChanged(long j) {
        synchronized (this.mLock) {
            StringBuilder sb = new StringBuilder();
            sb.append("Time changed by ");
            TimeUtils.formatDuration(j, sb);
            sb.append(".");
            TimeSparseArray<AtomicFile>[] timeSparseArrayArr = this.mSortedStatFiles;
            int length = timeSparseArrayArr.length;
            int i = 0;
            int i2 = 0;
            int i3 = 0;
            while (i2 < length) {
                TimeSparseArray<AtomicFile> timeSparseArray = timeSparseArrayArr[i2];
                int size = timeSparseArray.size();
                int i4 = i;
                int i5 = i3;
                int i6 = 0;
                while (i6 < size) {
                    AtomicFile atomicFile = (AtomicFile) timeSparseArray.valueAt(i6);
                    int i7 = i2;
                    long jKeyAt = timeSparseArray.keyAt(i6) + j;
                    if (jKeyAt < 0) {
                        i4++;
                        atomicFile.delete();
                    } else {
                        try {
                            atomicFile.openRead().close();
                        } catch (IOException e) {
                        }
                        String string = Long.toString(jKeyAt);
                        if (atomicFile.getBaseFile().getName().endsWith(CHECKED_IN_SUFFIX)) {
                            string = string + CHECKED_IN_SUFFIX;
                        }
                        i5++;
                        atomicFile.getBaseFile().renameTo(new File(atomicFile.getBaseFile().getParentFile(), string));
                    }
                    i6++;
                    i2 = i7;
                }
                timeSparseArray.clear();
                i2++;
                i = i4;
                i3 = i5;
            }
            sb.append(" files deleted: ");
            sb.append(i);
            sb.append(" files moved: ");
            sb.append(i3);
            Slog.i(TAG, sb.toString());
            indexFilesLocked();
        }
    }

    public IntervalStats getLatestUsageStats(int i) {
        synchronized (this.mLock) {
            if (i >= 0) {
                try {
                    if (i < this.mIntervalDirs.length) {
                        int size = this.mSortedStatFiles[i].size();
                        if (size == 0) {
                            return null;
                        }
                        try {
                            AtomicFile atomicFile = (AtomicFile) this.mSortedStatFiles[i].valueAt(size - 1);
                            IntervalStats intervalStats = new IntervalStats();
                            UsageStatsXml.read(atomicFile, intervalStats);
                            return intervalStats;
                        } catch (IOException e) {
                            Slog.e(TAG, "Failed to read usage stats file", e);
                            return null;
                        }
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
            throw new IllegalArgumentException("Bad interval type " + i);
        }
    }

    public <T> List<T> queryUsageStats(int i, long j, long j2, StatCombiner<T> statCombiner) {
        synchronized (this.mLock) {
            if (i >= 0) {
                try {
                    if (i < this.mIntervalDirs.length) {
                        TimeSparseArray<AtomicFile> timeSparseArray = this.mSortedStatFiles[i];
                        if (j2 <= j) {
                            return null;
                        }
                        int iClosestIndexOnOrBefore = timeSparseArray.closestIndexOnOrBefore(j);
                        if (iClosestIndexOnOrBefore < 0) {
                            iClosestIndexOnOrBefore = 0;
                        }
                        int iClosestIndexOnOrBefore2 = timeSparseArray.closestIndexOnOrBefore(j2);
                        if (iClosestIndexOnOrBefore2 < 0) {
                            return null;
                        }
                        if (timeSparseArray.keyAt(iClosestIndexOnOrBefore2) == j2 && iClosestIndexOnOrBefore2 - 1 < 0) {
                            return null;
                        }
                        IntervalStats intervalStats = new IntervalStats();
                        ArrayList arrayList = new ArrayList();
                        while (iClosestIndexOnOrBefore <= iClosestIndexOnOrBefore2) {
                            try {
                                UsageStatsXml.read((AtomicFile) timeSparseArray.valueAt(iClosestIndexOnOrBefore), intervalStats);
                                if (j < intervalStats.endTime) {
                                    statCombiner.combine(intervalStats, false, arrayList);
                                }
                            } catch (IOException e) {
                                Slog.e(TAG, "Failed to read usage stats file", e);
                            }
                            iClosestIndexOnOrBefore++;
                        }
                        return arrayList;
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
            throw new IllegalArgumentException("Bad interval type " + i);
        }
    }

    public int findBestFitBucket(long j, long j2) {
        int i;
        synchronized (this.mLock) {
            long j3 = JobStatus.NO_LATEST_RUNTIME;
            i = -1;
            for (int length = this.mSortedStatFiles.length - 1; length >= 0; length--) {
                int iClosestIndexOnOrBefore = this.mSortedStatFiles[length].closestIndexOnOrBefore(j);
                int size = this.mSortedStatFiles[length].size();
                if (iClosestIndexOnOrBefore >= 0 && iClosestIndexOnOrBefore < size) {
                    long jAbs = Math.abs(this.mSortedStatFiles[length].keyAt(iClosestIndexOnOrBefore) - j);
                    if (jAbs < j3) {
                        i = length;
                        j3 = jAbs;
                    }
                }
            }
        }
        return i;
    }

    public void prune(long j) {
        synchronized (this.mLock) {
            this.mCal.setTimeInMillis(j);
            this.mCal.addYears(-3);
            pruneFilesOlderThan(this.mIntervalDirs[3], this.mCal.getTimeInMillis());
            this.mCal.setTimeInMillis(j);
            this.mCal.addMonths(-6);
            pruneFilesOlderThan(this.mIntervalDirs[2], this.mCal.getTimeInMillis());
            this.mCal.setTimeInMillis(j);
            this.mCal.addWeeks(-4);
            pruneFilesOlderThan(this.mIntervalDirs[1], this.mCal.getTimeInMillis());
            this.mCal.setTimeInMillis(j);
            this.mCal.addDays(-10);
            pruneFilesOlderThan(this.mIntervalDirs[0], this.mCal.getTimeInMillis());
            this.mCal.setTimeInMillis(j);
            this.mCal.addDays(-SELECTION_LOG_RETENTION_LEN);
            for (int i = 0; i < this.mIntervalDirs.length; i++) {
                pruneChooserCountsOlderThan(this.mIntervalDirs[i], this.mCal.getTimeInMillis());
            }
            indexFilesLocked();
        }
    }

    private static void pruneFilesOlderThan(File file, long j) {
        long beginTime;
        File[] fileArrListFiles = file.listFiles();
        if (fileArrListFiles != null) {
            int length = fileArrListFiles.length;
            for (int i = 0; i < length; i++) {
                File file2 = fileArrListFiles[i];
                String path = file2.getPath();
                if (path.endsWith(BAK_SUFFIX)) {
                    file2 = new File(path.substring(0, path.length() - BAK_SUFFIX.length()));
                }
                try {
                    beginTime = UsageStatsXml.parseBeginTime(file2);
                } catch (IOException e) {
                    beginTime = 0;
                }
                if (beginTime < j) {
                    new AtomicFile(file2).delete();
                }
            }
        }
    }

    private static void pruneChooserCountsOlderThan(File file, long j) {
        long beginTime;
        File[] fileArrListFiles = file.listFiles();
        if (fileArrListFiles != null) {
            int length = fileArrListFiles.length;
            for (int i = 0; i < length; i++) {
                File file2 = fileArrListFiles[i];
                String path = file2.getPath();
                if (path.endsWith(BAK_SUFFIX)) {
                    file2 = new File(path.substring(0, path.length() - BAK_SUFFIX.length()));
                }
                try {
                    beginTime = UsageStatsXml.parseBeginTime(file2);
                } catch (IOException e) {
                    beginTime = 0;
                }
                if (beginTime < j) {
                    try {
                        AtomicFile atomicFile = new AtomicFile(file2);
                        IntervalStats intervalStats = new IntervalStats();
                        UsageStatsXml.read(atomicFile, intervalStats);
                        int size = intervalStats.packageStats.size();
                        for (int i2 = 0; i2 < size; i2++) {
                            UsageStats usageStatsValueAt = intervalStats.packageStats.valueAt(i2);
                            if (usageStatsValueAt.mChooserCounts != null) {
                                usageStatsValueAt.mChooserCounts.clear();
                            }
                        }
                        UsageStatsXml.write(atomicFile, intervalStats);
                    } catch (IOException e2) {
                        Slog.e(TAG, "Failed to delete chooser counts from usage stats file", e2);
                    }
                }
            }
        }
    }

    public void putUsageStats(int i, IntervalStats intervalStats) throws IOException {
        if (intervalStats == null) {
            return;
        }
        synchronized (this.mLock) {
            if (i >= 0) {
                try {
                    if (i < this.mIntervalDirs.length) {
                        AtomicFile atomicFile = (AtomicFile) this.mSortedStatFiles[i].get(intervalStats.beginTime);
                        if (atomicFile == null) {
                            atomicFile = new AtomicFile(new File(this.mIntervalDirs[i], Long.toString(intervalStats.beginTime)));
                            this.mSortedStatFiles[i].put(intervalStats.beginTime, atomicFile);
                        }
                        UsageStatsXml.write(atomicFile, intervalStats);
                        intervalStats.lastTimeSaved = atomicFile.getLastModifiedTime();
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
            throw new IllegalArgumentException("Bad interval type " + i);
        }
    }

    byte[] getBackupPayload(String str) {
        byte[] byteArray;
        synchronized (this.mLock) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            if (KEY_USAGE_STATS.equals(str)) {
                prune(System.currentTimeMillis());
                DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
                try {
                    dataOutputStream.writeInt(1);
                    dataOutputStream.writeInt(this.mSortedStatFiles[0].size());
                    for (int i = 0; i < this.mSortedStatFiles[0].size(); i++) {
                        writeIntervalStatsToStream(dataOutputStream, (AtomicFile) this.mSortedStatFiles[0].valueAt(i));
                    }
                    dataOutputStream.writeInt(this.mSortedStatFiles[1].size());
                    for (int i2 = 0; i2 < this.mSortedStatFiles[1].size(); i2++) {
                        writeIntervalStatsToStream(dataOutputStream, (AtomicFile) this.mSortedStatFiles[1].valueAt(i2));
                    }
                    dataOutputStream.writeInt(this.mSortedStatFiles[2].size());
                    for (int i3 = 0; i3 < this.mSortedStatFiles[2].size(); i3++) {
                        writeIntervalStatsToStream(dataOutputStream, (AtomicFile) this.mSortedStatFiles[2].valueAt(i3));
                    }
                    dataOutputStream.writeInt(this.mSortedStatFiles[3].size());
                    for (int i4 = 0; i4 < this.mSortedStatFiles[3].size(); i4++) {
                        writeIntervalStatsToStream(dataOutputStream, (AtomicFile) this.mSortedStatFiles[3].valueAt(i4));
                    }
                } catch (IOException e) {
                    Slog.d(TAG, "Failed to write data to output stream", e);
                    byteArrayOutputStream.reset();
                }
                byteArray = byteArrayOutputStream.toByteArray();
            } else {
                byteArray = byteArrayOutputStream.toByteArray();
            }
        }
        return byteArray;
    }

    void applyRestoredPayload(String str, byte[] bArr) {
        DataInputStream dataInputStream;
        int i;
        synchronized (this.mLock) {
            if (KEY_USAGE_STATS.equals(str)) {
                IntervalStats latestUsageStats = getLatestUsageStats(0);
                IntervalStats latestUsageStats2 = getLatestUsageStats(1);
                IntervalStats latestUsageStats3 = getLatestUsageStats(2);
                IntervalStats latestUsageStats4 = getLatestUsageStats(3);
                try {
                    try {
                        dataInputStream = new DataInputStream(new ByteArrayInputStream(bArr));
                        i = dataInputStream.readInt();
                    } catch (IOException e) {
                        Slog.d(TAG, "Failed to read data from input stream", e);
                    }
                    if (i >= 1 && i <= 1) {
                        for (int i2 = 0; i2 < this.mIntervalDirs.length; i2++) {
                            deleteDirectoryContents(this.mIntervalDirs[i2]);
                        }
                        int i3 = dataInputStream.readInt();
                        for (int i4 = 0; i4 < i3; i4++) {
                            putUsageStats(0, mergeStats(deserializeIntervalStats(getIntervalStatsBytes(dataInputStream)), latestUsageStats));
                        }
                        int i5 = dataInputStream.readInt();
                        for (int i6 = 0; i6 < i5; i6++) {
                            putUsageStats(1, mergeStats(deserializeIntervalStats(getIntervalStatsBytes(dataInputStream)), latestUsageStats2));
                        }
                        int i7 = dataInputStream.readInt();
                        for (int i8 = 0; i8 < i7; i8++) {
                            putUsageStats(2, mergeStats(deserializeIntervalStats(getIntervalStatsBytes(dataInputStream)), latestUsageStats3));
                        }
                        int i9 = dataInputStream.readInt();
                        for (int i10 = 0; i10 < i9; i10++) {
                            putUsageStats(3, mergeStats(deserializeIntervalStats(getIntervalStatsBytes(dataInputStream)), latestUsageStats4));
                        }
                        indexFilesLocked();
                    }
                } finally {
                    indexFilesLocked();
                }
            }
        }
    }

    private IntervalStats mergeStats(IntervalStats intervalStats, IntervalStats intervalStats2) {
        if (intervalStats2 == null) {
            return intervalStats;
        }
        if (intervalStats == null) {
            return null;
        }
        intervalStats.activeConfiguration = intervalStats2.activeConfiguration;
        intervalStats.configurations.putAll((ArrayMap<? extends Configuration, ? extends ConfigurationStats>) intervalStats2.configurations);
        intervalStats.events = intervalStats2.events;
        return intervalStats;
    }

    private void writeIntervalStatsToStream(DataOutputStream dataOutputStream, AtomicFile atomicFile) throws IOException {
        IntervalStats intervalStats = new IntervalStats();
        try {
            UsageStatsXml.read(atomicFile, intervalStats);
            sanitizeIntervalStatsForBackup(intervalStats);
            byte[] bArrSerializeIntervalStats = serializeIntervalStats(intervalStats);
            dataOutputStream.writeInt(bArrSerializeIntervalStats.length);
            dataOutputStream.write(bArrSerializeIntervalStats);
        } catch (IOException e) {
            Slog.e(TAG, "Failed to read usage stats file", e);
            dataOutputStream.writeInt(0);
        }
    }

    private static byte[] getIntervalStatsBytes(DataInputStream dataInputStream) throws IOException {
        int i = dataInputStream.readInt();
        byte[] bArr = new byte[i];
        dataInputStream.read(bArr, 0, i);
        return bArr;
    }

    private static void sanitizeIntervalStatsForBackup(IntervalStats intervalStats) {
        if (intervalStats == null) {
            return;
        }
        intervalStats.activeConfiguration = null;
        intervalStats.configurations.clear();
        if (intervalStats.events != null) {
            intervalStats.events.clear();
        }
    }

    private static byte[] serializeIntervalStats(IntervalStats intervalStats) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        try {
            dataOutputStream.writeLong(intervalStats.beginTime);
            UsageStatsXml.write(dataOutputStream, intervalStats);
        } catch (IOException e) {
            Slog.d(TAG, "Serializing IntervalStats Failed", e);
            byteArrayOutputStream.reset();
        }
        return byteArrayOutputStream.toByteArray();
    }

    private static IntervalStats deserializeIntervalStats(byte[] bArr) {
        DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(bArr));
        IntervalStats intervalStats = new IntervalStats();
        try {
            intervalStats.beginTime = dataInputStream.readLong();
            UsageStatsXml.read(dataInputStream, intervalStats);
            return intervalStats;
        } catch (IOException e) {
            Slog.d(TAG, "DeSerializing IntervalStats Failed", e);
            return null;
        }
    }

    private static void deleteDirectoryContents(File file) {
        for (File file2 : file.listFiles()) {
            deleteDirectory(file2);
        }
    }

    private static void deleteDirectory(File file) {
        File[] fileArrListFiles = file.listFiles();
        if (fileArrListFiles != null) {
            for (File file2 : fileArrListFiles) {
                if (!file2.isDirectory()) {
                    file2.delete();
                } else {
                    deleteDirectory(file2);
                }
            }
        }
        file.delete();
    }
}
