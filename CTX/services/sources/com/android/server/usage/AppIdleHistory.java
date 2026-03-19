package com.android.server.usage;

import android.app.usage.AppStandbyInfo;
import android.app.usage.UsageStatsManager;
import android.os.SystemClock;
import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import android.util.Xml;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.job.controllers.JobStatus;
import com.android.server.usb.descriptors.UsbTerminalTypes;
import com.android.server.voiceinteraction.DatabaseHelper;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class AppIdleHistory {

    @VisibleForTesting
    static final String APP_IDLE_FILENAME = "app_idle_stats.xml";
    private static final String ATTR_BUCKETING_REASON = "bucketReason";
    private static final String ATTR_BUCKET_ACTIVE_TIMEOUT_TIME = "activeTimeoutTime";
    private static final String ATTR_BUCKET_WORKING_SET_TIMEOUT_TIME = "workingSetTimeoutTime";
    private static final String ATTR_CURRENT_BUCKET = "appLimitBucket";
    private static final String ATTR_ELAPSED_IDLE = "elapsedIdleTime";
    private static final String ATTR_LAST_PREDICTED_TIME = "lastPredictedTime";
    private static final String ATTR_LAST_RUN_JOB_TIME = "lastJobRunTime";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_SCREEN_IDLE = "screenIdleTime";
    private static final boolean DEBUG = false;
    private static final long ONE_MINUTE = 60000;
    private static final int STANDBY_BUCKET_UNKNOWN = -1;
    private static final String TAG = "AppIdleHistory";
    private static final String TAG_PACKAGE = "package";
    private static final String TAG_PACKAGES = "packages";
    private long mElapsedDuration;
    private long mElapsedSnapshot;
    private SparseArray<ArrayMap<String, AppUsageHistory>> mIdleHistory = new SparseArray<>();
    private boolean mScreenOn;
    private long mScreenOnDuration;
    private long mScreenOnSnapshot;
    private final File mStorageDir;

    static class AppUsageHistory {
        long bucketActiveTimeoutTime;
        long bucketWorkingSetTimeoutTime;
        int bucketingReason;
        int currentBucket;
        int lastInformedBucket;
        long lastJobRunTime;
        int lastPredictedBucket = -1;
        long lastPredictedTime;
        long lastUsedElapsedTime;
        long lastUsedScreenTime;

        AppUsageHistory() {
        }
    }

    AppIdleHistory(File file, long j) {
        this.mElapsedSnapshot = j;
        this.mScreenOnSnapshot = j;
        this.mStorageDir = file;
        readScreenOnTime();
    }

    public void updateDisplay(boolean z, long j) {
        if (z == this.mScreenOn) {
            return;
        }
        this.mScreenOn = z;
        if (this.mScreenOn) {
            this.mScreenOnSnapshot = j;
            return;
        }
        this.mScreenOnDuration += j - this.mScreenOnSnapshot;
        this.mElapsedDuration += j - this.mElapsedSnapshot;
        this.mElapsedSnapshot = j;
    }

    public long getScreenOnTime(long j) {
        long j2 = this.mScreenOnDuration;
        if (this.mScreenOn) {
            return j2 + (j - this.mScreenOnSnapshot);
        }
        return j2;
    }

    @VisibleForTesting
    File getScreenOnTimeFile() {
        return new File(this.mStorageDir, "screen_on_time");
    }

    private void readScreenOnTime() {
        File screenOnTimeFile = getScreenOnTimeFile();
        if (screenOnTimeFile.exists()) {
            try {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(screenOnTimeFile));
                this.mScreenOnDuration = Long.parseLong(bufferedReader.readLine());
                this.mElapsedDuration = Long.parseLong(bufferedReader.readLine());
                bufferedReader.close();
                return;
            } catch (IOException | NumberFormatException e) {
                return;
            }
        }
        writeScreenOnTime();
    }

    private void writeScreenOnTime() {
        FileOutputStream fileOutputStreamStartWrite;
        AtomicFile atomicFile = new AtomicFile(getScreenOnTimeFile());
        try {
            fileOutputStreamStartWrite = atomicFile.startWrite();
        } catch (IOException e) {
            fileOutputStreamStartWrite = null;
        }
        try {
            fileOutputStreamStartWrite.write((Long.toString(this.mScreenOnDuration) + "\n" + Long.toString(this.mElapsedDuration) + "\n").getBytes());
            atomicFile.finishWrite(fileOutputStreamStartWrite);
        } catch (IOException e2) {
            atomicFile.failWrite(fileOutputStreamStartWrite);
        }
    }

    public void writeAppIdleDurations() {
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        this.mElapsedDuration += jElapsedRealtime - this.mElapsedSnapshot;
        this.mElapsedSnapshot = jElapsedRealtime;
        writeScreenOnTime();
    }

    public AppUsageHistory reportUsage(AppUsageHistory appUsageHistory, String str, int i, int i2, long j, long j2) {
        if (j2 > j) {
            long j3 = this.mElapsedDuration + (j2 - this.mElapsedSnapshot);
            if (i == 10) {
                appUsageHistory.bucketActiveTimeoutTime = Math.max(j3, appUsageHistory.bucketActiveTimeoutTime);
            } else if (i == 20) {
                appUsageHistory.bucketWorkingSetTimeoutTime = Math.max(j3, appUsageHistory.bucketWorkingSetTimeoutTime);
            } else {
                throw new IllegalArgumentException("Cannot set a timeout on bucket=" + i);
            }
        }
        if (j != 0) {
            appUsageHistory.lastUsedElapsedTime = this.mElapsedDuration + (j - this.mElapsedSnapshot);
            appUsageHistory.lastUsedScreenTime = getScreenOnTime(j);
        }
        if (appUsageHistory.currentBucket > i) {
            appUsageHistory.currentBucket = i;
        }
        appUsageHistory.bucketingReason = 768 | i2;
        return appUsageHistory;
    }

    public AppUsageHistory reportUsage(String str, int i, int i2, int i3, long j, long j2) {
        return reportUsage(getPackageHistory(getUserHistory(i), str, j, true), str, i2, i3, j, j2);
    }

    private ArrayMap<String, AppUsageHistory> getUserHistory(int i) throws Throwable {
        ArrayMap<String, AppUsageHistory> arrayMap = this.mIdleHistory.get(i);
        if (arrayMap == null) {
            ArrayMap<String, AppUsageHistory> arrayMap2 = new ArrayMap<>();
            this.mIdleHistory.put(i, arrayMap2);
            readAppIdleTimes(i, arrayMap2);
            return arrayMap2;
        }
        return arrayMap;
    }

    private AppUsageHistory getPackageHistory(ArrayMap<String, AppUsageHistory> arrayMap, String str, long j, boolean z) {
        AppUsageHistory appUsageHistory = arrayMap.get(str);
        if (appUsageHistory == null && z) {
            AppUsageHistory appUsageHistory2 = new AppUsageHistory();
            appUsageHistory2.lastUsedElapsedTime = getElapsedTime(j);
            appUsageHistory2.lastUsedScreenTime = getScreenOnTime(j);
            appUsageHistory2.lastPredictedTime = getElapsedTime(0L);
            appUsageHistory2.currentBucket = 50;
            appUsageHistory2.bucketingReason = 256;
            appUsageHistory2.lastInformedBucket = -1;
            appUsageHistory2.lastJobRunTime = Long.MIN_VALUE;
            arrayMap.put(str, appUsageHistory2);
            return appUsageHistory2;
        }
        return appUsageHistory;
    }

    public void onUserRemoved(int i) {
        this.mIdleHistory.remove(i);
    }

    public boolean isIdle(String str, int i, long j) {
        AppUsageHistory packageHistory = getPackageHistory(getUserHistory(i), str, j, true);
        return packageHistory != null && packageHistory.currentBucket >= 40;
    }

    public AppUsageHistory getAppUsageHistory(String str, int i, long j) {
        return getPackageHistory(getUserHistory(i), str, j, true);
    }

    public void setAppStandbyBucket(String str, int i, long j, int i2, int i3) {
        setAppStandbyBucket(str, i, j, i2, i3, false);
    }

    public void setAppStandbyBucket(String str, int i, long j, int i2, int i3, boolean z) {
        AppUsageHistory packageHistory = getPackageHistory(getUserHistory(i), str, j, true);
        packageHistory.currentBucket = i2;
        packageHistory.bucketingReason = i3;
        long elapsedTime = getElapsedTime(j);
        if ((65280 & i3) == 1280) {
            packageHistory.lastPredictedTime = elapsedTime;
            packageHistory.lastPredictedBucket = i2;
        }
        if (z) {
            packageHistory.bucketActiveTimeoutTime = elapsedTime;
            packageHistory.bucketWorkingSetTimeoutTime = elapsedTime;
        }
    }

    public void updateLastPrediction(AppUsageHistory appUsageHistory, long j, int i) {
        appUsageHistory.lastPredictedTime = j;
        appUsageHistory.lastPredictedBucket = i;
    }

    public void setLastJobRunTime(String str, int i, long j) {
        getPackageHistory(getUserHistory(i), str, j, true).lastJobRunTime = getElapsedTime(j);
    }

    public long getTimeSinceLastJobRun(String str, int i, long j) {
        AppUsageHistory packageHistory = getPackageHistory(getUserHistory(i), str, j, true);
        return packageHistory.lastJobRunTime == Long.MIN_VALUE ? JobStatus.NO_LATEST_RUNTIME : getElapsedTime(j) - packageHistory.lastJobRunTime;
    }

    public int getAppStandbyBucket(String str, int i, long j) {
        return getPackageHistory(getUserHistory(i), str, j, true).currentBucket;
    }

    public ArrayList<AppStandbyInfo> getAppStandbyBuckets(int i, boolean z) throws Throwable {
        ArrayMap<String, AppUsageHistory> userHistory = getUserHistory(i);
        int size = userHistory.size();
        ArrayList<AppStandbyInfo> arrayList = new ArrayList<>(size);
        for (int i2 = 0; i2 < size; i2++) {
            arrayList.add(new AppStandbyInfo(userHistory.keyAt(i2), z ? userHistory.valueAt(i2).currentBucket : 10));
        }
        return arrayList;
    }

    public int getAppStandbyReason(String str, int i, long j) {
        AppUsageHistory packageHistory = getPackageHistory(getUserHistory(i), str, j, false);
        if (packageHistory != null) {
            return packageHistory.bucketingReason;
        }
        return 0;
    }

    public long getElapsedTime(long j) {
        return (j - this.mElapsedSnapshot) + this.mElapsedDuration;
    }

    public int setIdle(String str, int i, boolean z, long j) {
        AppUsageHistory packageHistory = getPackageHistory(getUserHistory(i), str, j, true);
        if (z) {
            packageHistory.currentBucket = 40;
            packageHistory.bucketingReason = 1024;
        } else {
            packageHistory.currentBucket = 10;
            packageHistory.bucketingReason = UsbTerminalTypes.TERMINAL_OUT_HEADMOUNTED;
        }
        return packageHistory.currentBucket;
    }

    public void clearUsage(String str, int i) {
        getUserHistory(i).remove(str);
    }

    boolean shouldInformListeners(String str, int i, long j, int i2) {
        AppUsageHistory packageHistory = getPackageHistory(getUserHistory(i), str, j, true);
        if (packageHistory.lastInformedBucket != i2) {
            packageHistory.lastInformedBucket = i2;
            return true;
        }
        return false;
    }

    int getThresholdIndex(String str, int i, long j, long[] jArr, long[] jArr2) {
        AppUsageHistory packageHistory = getPackageHistory(getUserHistory(i), str, j, false);
        if (packageHistory == null) {
            return jArr.length - 1;
        }
        long screenOnTime = getScreenOnTime(j) - packageHistory.lastUsedScreenTime;
        long elapsedTime = getElapsedTime(j) - packageHistory.lastUsedElapsedTime;
        for (int length = jArr.length - 1; length >= 0; length--) {
            if (screenOnTime >= jArr[length] && elapsedTime >= jArr2[length]) {
                return length;
            }
        }
        return 0;
    }

    @VisibleForTesting
    File getUserFile(int i) {
        return new File(new File(new File(this.mStorageDir, DatabaseHelper.SoundModelContract.KEY_USERS), Integer.toString(i)), APP_IDLE_FILENAME);
    }

    private void readAppIdleTimes(int i, ArrayMap<String, AppUsageHistory> arrayMap) throws Throwable {
        FileInputStream fileInputStreamOpenRead;
        int next;
        FileInputStream fileInputStream = null;
        try {
            try {
                fileInputStreamOpenRead = new AtomicFile(getUserFile(i)).openRead();
            } catch (IOException | XmlPullParserException e) {
            }
            try {
                try {
                    XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                    xmlPullParserNewPullParser.setInput(fileInputStreamOpenRead, StandardCharsets.UTF_8.name());
                    do {
                        next = xmlPullParserNewPullParser.next();
                        if (next == 2) {
                            break;
                        }
                    } while (next != 1);
                    if (next != 2) {
                        Slog.e(TAG, "Unable to read app idle file for user " + i);
                        IoUtils.closeQuietly(fileInputStreamOpenRead);
                        return;
                    }
                    if (!xmlPullParserNewPullParser.getName().equals(TAG_PACKAGES)) {
                        IoUtils.closeQuietly(fileInputStreamOpenRead);
                        return;
                    }
                    while (true) {
                        int next2 = xmlPullParserNewPullParser.next();
                        if (next2 == 1) {
                            IoUtils.closeQuietly(fileInputStreamOpenRead);
                            return;
                        }
                        if (next2 == 2 && xmlPullParserNewPullParser.getName().equals("package")) {
                            String attributeValue = xmlPullParserNewPullParser.getAttributeValue(null, "name");
                            AppUsageHistory appUsageHistory = new AppUsageHistory();
                            appUsageHistory.lastUsedElapsedTime = Long.parseLong(xmlPullParserNewPullParser.getAttributeValue(null, ATTR_ELAPSED_IDLE));
                            appUsageHistory.lastUsedScreenTime = Long.parseLong(xmlPullParserNewPullParser.getAttributeValue(null, ATTR_SCREEN_IDLE));
                            appUsageHistory.lastPredictedTime = getLongValue(xmlPullParserNewPullParser, ATTR_LAST_PREDICTED_TIME, 0L);
                            String attributeValue2 = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_CURRENT_BUCKET);
                            appUsageHistory.currentBucket = attributeValue2 == null ? 10 : Integer.parseInt(attributeValue2);
                            String attributeValue3 = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_BUCKETING_REASON);
                            appUsageHistory.lastJobRunTime = getLongValue(xmlPullParserNewPullParser, ATTR_LAST_RUN_JOB_TIME, Long.MIN_VALUE);
                            appUsageHistory.bucketActiveTimeoutTime = getLongValue(xmlPullParserNewPullParser, ATTR_BUCKET_ACTIVE_TIMEOUT_TIME, 0L);
                            appUsageHistory.bucketWorkingSetTimeoutTime = getLongValue(xmlPullParserNewPullParser, ATTR_BUCKET_WORKING_SET_TIMEOUT_TIME, 0L);
                            appUsageHistory.bucketingReason = 256;
                            if (attributeValue3 != null) {
                                try {
                                    appUsageHistory.bucketingReason = Integer.parseInt(attributeValue3, 16);
                                } catch (NumberFormatException e2) {
                                }
                            }
                            appUsageHistory.lastInformedBucket = -1;
                            arrayMap.put(attributeValue, appUsageHistory);
                        }
                    }
                } catch (Throwable th) {
                    th = th;
                    IoUtils.closeQuietly(fileInputStreamOpenRead);
                    throw th;
                }
            } catch (IOException | XmlPullParserException e3) {
                fileInputStream = fileInputStreamOpenRead;
                Slog.e(TAG, "Unable to read app idle file for user " + i);
                IoUtils.closeQuietly(fileInputStream);
            }
        } catch (Throwable th2) {
            th = th2;
            fileInputStreamOpenRead = fileInputStream;
        }
    }

    private long getLongValue(XmlPullParser xmlPullParser, String str, long j) {
        String attributeValue = xmlPullParser.getAttributeValue(null, str);
        return attributeValue == null ? j : Long.parseLong(attributeValue);
    }

    public void writeAppIdleTimes(int i) throws Throwable {
        FileOutputStream fileOutputStreamStartWrite;
        AtomicFile atomicFile = new AtomicFile(getUserFile(i));
        try {
            fileOutputStreamStartWrite = atomicFile.startWrite();
        } catch (Exception e) {
            fileOutputStreamStartWrite = null;
        }
        try {
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStreamStartWrite);
            FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(bufferedOutputStream, StandardCharsets.UTF_8.name());
            fastXmlSerializer.startDocument((String) null, true);
            fastXmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            fastXmlSerializer.startTag((String) null, TAG_PACKAGES);
            ArrayMap<String, AppUsageHistory> userHistory = getUserHistory(i);
            int size = userHistory.size();
            for (int i2 = 0; i2 < size; i2++) {
                String strKeyAt = userHistory.keyAt(i2);
                AppUsageHistory appUsageHistoryValueAt = userHistory.valueAt(i2);
                fastXmlSerializer.startTag((String) null, "package");
                fastXmlSerializer.attribute((String) null, "name", strKeyAt);
                fastXmlSerializer.attribute((String) null, ATTR_ELAPSED_IDLE, Long.toString(appUsageHistoryValueAt.lastUsedElapsedTime));
                fastXmlSerializer.attribute((String) null, ATTR_SCREEN_IDLE, Long.toString(appUsageHistoryValueAt.lastUsedScreenTime));
                fastXmlSerializer.attribute((String) null, ATTR_LAST_PREDICTED_TIME, Long.toString(appUsageHistoryValueAt.lastPredictedTime));
                fastXmlSerializer.attribute((String) null, ATTR_CURRENT_BUCKET, Integer.toString(appUsageHistoryValueAt.currentBucket));
                fastXmlSerializer.attribute((String) null, ATTR_BUCKETING_REASON, Integer.toHexString(appUsageHistoryValueAt.bucketingReason));
                if (appUsageHistoryValueAt.bucketActiveTimeoutTime > 0) {
                    fastXmlSerializer.attribute((String) null, ATTR_BUCKET_ACTIVE_TIMEOUT_TIME, Long.toString(appUsageHistoryValueAt.bucketActiveTimeoutTime));
                }
                if (appUsageHistoryValueAt.bucketWorkingSetTimeoutTime > 0) {
                    fastXmlSerializer.attribute((String) null, ATTR_BUCKET_WORKING_SET_TIMEOUT_TIME, Long.toString(appUsageHistoryValueAt.bucketWorkingSetTimeoutTime));
                }
                if (appUsageHistoryValueAt.lastJobRunTime != Long.MIN_VALUE) {
                    fastXmlSerializer.attribute((String) null, ATTR_LAST_RUN_JOB_TIME, Long.toString(appUsageHistoryValueAt.lastJobRunTime));
                }
                fastXmlSerializer.endTag((String) null, "package");
            }
            fastXmlSerializer.endTag((String) null, TAG_PACKAGES);
            fastXmlSerializer.endDocument();
            atomicFile.finishWrite(fileOutputStreamStartWrite);
        } catch (Exception e2) {
            atomicFile.failWrite(fileOutputStreamStartWrite);
            Slog.e(TAG, "Error writing app idle file for user " + i);
        }
    }

    public void dump(IndentingPrintWriter indentingPrintWriter, int i, String str) {
        ArrayMap<String, AppUsageHistory> arrayMap;
        String str2 = str;
        indentingPrintWriter.println("App Standby States:");
        indentingPrintWriter.increaseIndent();
        ArrayMap<String, AppUsageHistory> arrayMap2 = this.mIdleHistory.get(i);
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        long elapsedTime = getElapsedTime(jElapsedRealtime);
        long screenOnTime = getScreenOnTime(jElapsedRealtime);
        if (arrayMap2 == null) {
            return;
        }
        int size = arrayMap2.size();
        int i2 = 0;
        while (i2 < size) {
            String strKeyAt = arrayMap2.keyAt(i2);
            AppUsageHistory appUsageHistoryValueAt = arrayMap2.valueAt(i2);
            if (str2 != null && !str2.equals(strKeyAt)) {
                arrayMap = arrayMap2;
            } else {
                indentingPrintWriter.print("package=" + strKeyAt);
                indentingPrintWriter.print(" u=" + i);
                indentingPrintWriter.print(" bucket=" + appUsageHistoryValueAt.currentBucket + " reason=" + UsageStatsManager.reasonToString(appUsageHistoryValueAt.bucketingReason));
                indentingPrintWriter.print(" used=");
                arrayMap = arrayMap2;
                TimeUtils.formatDuration(elapsedTime - appUsageHistoryValueAt.lastUsedElapsedTime, indentingPrintWriter);
                indentingPrintWriter.print(" usedScr=");
                TimeUtils.formatDuration(screenOnTime - appUsageHistoryValueAt.lastUsedScreenTime, indentingPrintWriter);
                indentingPrintWriter.print(" lastPred=");
                TimeUtils.formatDuration(elapsedTime - appUsageHistoryValueAt.lastPredictedTime, indentingPrintWriter);
                indentingPrintWriter.print(" activeLeft=");
                TimeUtils.formatDuration(appUsageHistoryValueAt.bucketActiveTimeoutTime - elapsedTime, indentingPrintWriter);
                indentingPrintWriter.print(" wsLeft=");
                TimeUtils.formatDuration(appUsageHistoryValueAt.bucketWorkingSetTimeoutTime - elapsedTime, indentingPrintWriter);
                indentingPrintWriter.print(" lastJob=");
                TimeUtils.formatDuration(elapsedTime - appUsageHistoryValueAt.lastJobRunTime, indentingPrintWriter);
                StringBuilder sb = new StringBuilder();
                sb.append(" idle=");
                sb.append(isIdle(strKeyAt, i, jElapsedRealtime) ? "y" : "n");
                indentingPrintWriter.print(sb.toString());
                indentingPrintWriter.println();
            }
            i2++;
            arrayMap2 = arrayMap;
            str2 = str;
        }
        indentingPrintWriter.println();
        indentingPrintWriter.print("totalElapsedTime=");
        TimeUtils.formatDuration(getElapsedTime(jElapsedRealtime), indentingPrintWriter);
        indentingPrintWriter.println();
        indentingPrintWriter.print("totalScreenOnTime=");
        TimeUtils.formatDuration(getScreenOnTime(jElapsedRealtime), indentingPrintWriter);
        indentingPrintWriter.println();
        indentingPrintWriter.decreaseIndent();
    }
}
