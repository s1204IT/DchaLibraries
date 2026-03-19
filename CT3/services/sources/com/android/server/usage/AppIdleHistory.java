package com.android.server.usage;

import android.os.Environment;
import android.os.SystemClock;
import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import android.util.Xml;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.voiceinteraction.DatabaseHelper;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class AppIdleHistory {
    static final String APP_IDLE_FILENAME = "app_idle_stats.xml";
    private static final String ATTR_ELAPSED_IDLE = "elapsedIdleTime";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_SCREEN_IDLE = "screenIdleTime";
    private static final int FLAG_LAST_STATE = 2;
    private static final int FLAG_PARTIAL_ACTIVE = 1;
    private static final int HISTORY_SIZE = 100;
    private static final long ONE_MINUTE = 60000;
    private static final long PERIOD_DURATION = 3600000;
    private static final String TAG = "AppIdleHistory";
    private static final String TAG_PACKAGE = "package";
    private static final String TAG_PACKAGES = "packages";
    private long mElapsedDuration;
    private long mElapsedSnapshot;
    private long mElapsedTimeThreshold;
    private SparseArray<ArrayMap<String, PackageHistory>> mIdleHistory;
    private long mLastPeriod;
    private boolean mScreenOn;
    private long mScreenOnDuration;
    private long mScreenOnSnapshot;
    private long mScreenOnTimeThreshold;
    private final File mStorageDir;

    private static class PackageHistory {
        long lastUsedElapsedTime;
        long lastUsedScreenTime;
        final byte[] recent;

        PackageHistory(PackageHistory packageHistory) {
            this();
        }

        private PackageHistory() {
            this.recent = new byte[100];
        }
    }

    AppIdleHistory(long elapsedRealtime) {
        this(Environment.getDataSystemDirectory(), elapsedRealtime);
    }

    AppIdleHistory(File storageDir, long elapsedRealtime) {
        this.mIdleHistory = new SparseArray<>();
        this.mLastPeriod = 0L;
        this.mElapsedSnapshot = elapsedRealtime;
        this.mScreenOnSnapshot = elapsedRealtime;
        this.mStorageDir = storageDir;
        readScreenOnTimeLocked();
    }

    public void setThresholds(long elapsedTimeThreshold, long screenOnTimeThreshold) {
        this.mElapsedTimeThreshold = elapsedTimeThreshold;
        this.mScreenOnTimeThreshold = screenOnTimeThreshold;
    }

    public void updateDisplayLocked(boolean screenOn, long elapsedRealtime) {
        if (screenOn == this.mScreenOn) {
            return;
        }
        this.mScreenOn = screenOn;
        if (this.mScreenOn) {
            this.mScreenOnSnapshot = elapsedRealtime;
            return;
        }
        this.mScreenOnDuration += elapsedRealtime - this.mScreenOnSnapshot;
        this.mElapsedDuration += elapsedRealtime - this.mElapsedSnapshot;
        writeScreenOnTimeLocked();
        this.mElapsedSnapshot = elapsedRealtime;
    }

    public long getScreenOnTimeLocked(long elapsedRealtime) {
        long screenOnTime = this.mScreenOnDuration;
        if (this.mScreenOn) {
            return screenOnTime + (elapsedRealtime - this.mScreenOnSnapshot);
        }
        return screenOnTime;
    }

    File getScreenOnTimeFile() {
        return new File(this.mStorageDir, "screen_on_time");
    }

    private void readScreenOnTimeLocked() {
        File screenOnTimeFile = getScreenOnTimeFile();
        if (screenOnTimeFile.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(screenOnTimeFile));
                this.mScreenOnDuration = Long.parseLong(reader.readLine());
                this.mElapsedDuration = Long.parseLong(reader.readLine());
                reader.close();
                return;
            } catch (IOException | NumberFormatException e) {
                return;
            }
        }
        writeScreenOnTimeLocked();
    }

    private void writeScreenOnTimeLocked() {
        AtomicFile screenOnTimeFile = new AtomicFile(getScreenOnTimeFile());
        FileOutputStream fos = null;
        try {
            fos = screenOnTimeFile.startWrite();
            fos.write((Long.toString(this.mScreenOnDuration) + "\n" + Long.toString(this.mElapsedDuration) + "\n").getBytes());
            screenOnTimeFile.finishWrite(fos);
        } catch (IOException e) {
            screenOnTimeFile.failWrite(fos);
        }
    }

    public void writeElapsedTimeLocked() {
        long elapsedRealtime = SystemClock.elapsedRealtime();
        this.mElapsedDuration += elapsedRealtime - this.mElapsedSnapshot;
        this.mElapsedSnapshot = elapsedRealtime;
        writeScreenOnTimeLocked();
    }

    public void reportUsageLocked(String packageName, int userId, long elapsedRealtime) {
        ArrayMap<String, PackageHistory> userHistory = getUserHistoryLocked(userId);
        PackageHistory packageHistory = getPackageHistoryLocked(userHistory, packageName, elapsedRealtime);
        shiftHistoryToNow(userHistory, elapsedRealtime);
        packageHistory.lastUsedElapsedTime = this.mElapsedDuration + (elapsedRealtime - this.mElapsedSnapshot);
        packageHistory.lastUsedScreenTime = getScreenOnTimeLocked(elapsedRealtime);
        packageHistory.recent[99] = 3;
    }

    public void setIdle(String packageName, int userId, long elapsedRealtime) {
        ArrayMap<String, PackageHistory> userHistory = getUserHistoryLocked(userId);
        PackageHistory packageHistory = getPackageHistoryLocked(userHistory, packageName, elapsedRealtime);
        shiftHistoryToNow(userHistory, elapsedRealtime);
        byte[] bArr = packageHistory.recent;
        bArr[99] = (byte) (bArr[99] & (-3));
    }

    private void shiftHistoryToNow(ArrayMap<String, PackageHistory> userHistory, long elapsedRealtime) {
        long thisPeriod = elapsedRealtime / PERIOD_DURATION;
        if (this.mLastPeriod != 0 && this.mLastPeriod < thisPeriod && thisPeriod - this.mLastPeriod < 99) {
            int diff = (int) (thisPeriod - this.mLastPeriod);
            int NUSERS = this.mIdleHistory.size();
            for (int u = 0; u < NUSERS; u++) {
                ArrayMap<String, PackageHistory> userHistory2 = this.mIdleHistory.valueAt(u);
                for (PackageHistory idleState : userHistory2.values()) {
                    System.arraycopy(idleState.recent, diff, idleState.recent, 0, 100 - diff);
                    for (int i = 0; i < diff; i++) {
                        idleState.recent[(100 - i) - 1] = (byte) (idleState.recent[(100 - diff) - 1] & 2);
                    }
                }
            }
        }
        this.mLastPeriod = thisPeriod;
    }

    private ArrayMap<String, PackageHistory> getUserHistoryLocked(int userId) {
        ArrayMap<String, PackageHistory> userHistory = this.mIdleHistory.get(userId);
        if (userHistory == null) {
            ArrayMap<String, PackageHistory> userHistory2 = new ArrayMap<>();
            this.mIdleHistory.put(userId, userHistory2);
            readAppIdleTimesLocked(userId, userHistory2);
            return userHistory2;
        }
        return userHistory;
    }

    private PackageHistory getPackageHistoryLocked(ArrayMap<String, PackageHistory> userHistory, String packageName, long elapsedRealtime) {
        PackageHistory packageHistory = null;
        PackageHistory packageHistory2 = userHistory.get(packageName);
        if (packageHistory2 == null) {
            PackageHistory packageHistory3 = new PackageHistory(packageHistory);
            packageHistory3.lastUsedElapsedTime = getElapsedTimeLocked(elapsedRealtime);
            packageHistory3.lastUsedScreenTime = getScreenOnTimeLocked(elapsedRealtime);
            userHistory.put(packageName, packageHistory3);
            return packageHistory3;
        }
        return packageHistory2;
    }

    public void onUserRemoved(int userId) {
        this.mIdleHistory.remove(userId);
    }

    public boolean isIdleLocked(String packageName, int userId, long elapsedRealtime) {
        ArrayMap<String, PackageHistory> userHistory = getUserHistoryLocked(userId);
        PackageHistory packageHistory = getPackageHistoryLocked(userHistory, packageName, elapsedRealtime);
        if (packageHistory == null) {
            return false;
        }
        return hasPassedThresholdsLocked(packageHistory, elapsedRealtime);
    }

    private long getElapsedTimeLocked(long elapsedRealtime) {
        return (elapsedRealtime - this.mElapsedSnapshot) + this.mElapsedDuration;
    }

    public void setIdleLocked(String packageName, int userId, boolean idle, long elapsedRealtime) {
        ArrayMap<String, PackageHistory> userHistory = getUserHistoryLocked(userId);
        PackageHistory packageHistory = getPackageHistoryLocked(userHistory, packageName, elapsedRealtime);
        packageHistory.lastUsedElapsedTime = getElapsedTimeLocked(elapsedRealtime) - this.mElapsedTimeThreshold;
        packageHistory.lastUsedScreenTime = (getScreenOnTimeLocked(elapsedRealtime) - (idle ? this.mScreenOnTimeThreshold : 0L)) - 1000;
    }

    public void clearUsageLocked(String packageName, int userId) {
        ArrayMap<String, PackageHistory> userHistory = getUserHistoryLocked(userId);
        userHistory.remove(packageName);
    }

    private boolean hasPassedThresholdsLocked(PackageHistory packageHistory, long elapsedRealtime) {
        return packageHistory.lastUsedScreenTime <= getScreenOnTimeLocked(elapsedRealtime) - this.mScreenOnTimeThreshold && packageHistory.lastUsedElapsedTime <= getElapsedTimeLocked(elapsedRealtime) - this.mElapsedTimeThreshold;
    }

    private File getUserFile(int userId) {
        return new File(new File(new File(this.mStorageDir, DatabaseHelper.SoundModelContract.KEY_USERS), Integer.toString(userId)), APP_IDLE_FILENAME);
    }

    private void readAppIdleTimesLocked(int userId, ArrayMap<String, PackageHistory> userHistory) {
        int type;
        try {
            try {
                AtomicFile appIdleFile = new AtomicFile(getUserFile(userId));
                FileInputStream fis = appIdleFile.openRead();
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(fis, StandardCharsets.UTF_8.name());
                do {
                    type = parser.next();
                    if (type == 2) {
                        break;
                    }
                } while (type != 1);
                if (type != 2) {
                    Slog.e(TAG, "Unable to read app idle file for user " + userId);
                    IoUtils.closeQuietly(fis);
                    return;
                }
                if (!parser.getName().equals(TAG_PACKAGES)) {
                    IoUtils.closeQuietly(fis);
                    return;
                }
                while (true) {
                    int type2 = parser.next();
                    if (type2 == 1) {
                        IoUtils.closeQuietly(fis);
                        return;
                    }
                    if (type2 == 2) {
                        String name = parser.getName();
                        if (name.equals(TAG_PACKAGE)) {
                            String packageName = parser.getAttributeValue(null, ATTR_NAME);
                            PackageHistory packageHistory = new PackageHistory(null);
                            packageHistory.lastUsedElapsedTime = Long.parseLong(parser.getAttributeValue(null, ATTR_ELAPSED_IDLE));
                            packageHistory.lastUsedScreenTime = Long.parseLong(parser.getAttributeValue(null, ATTR_SCREEN_IDLE));
                            userHistory.put(packageName, packageHistory);
                        }
                    }
                }
            } catch (IOException | XmlPullParserException e) {
                Slog.e(TAG, "Unable to read app idle file for user " + userId);
                IoUtils.closeQuietly((AutoCloseable) null);
            }
        } catch (Throwable th) {
            IoUtils.closeQuietly((AutoCloseable) null);
            throw th;
        }
    }

    public void writeAppIdleTimesLocked(int userId) {
        FileOutputStream fos = null;
        AtomicFile appIdleFile = new AtomicFile(getUserFile(userId));
        try {
            fos = appIdleFile.startWrite();
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            FastXmlSerializer xml = new FastXmlSerializer();
            xml.setOutput(bos, StandardCharsets.UTF_8.name());
            xml.startDocument((String) null, true);
            xml.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            xml.startTag((String) null, TAG_PACKAGES);
            ArrayMap<String, PackageHistory> userHistory = getUserHistoryLocked(userId);
            int N = userHistory.size();
            for (int i = 0; i < N; i++) {
                String packageName = userHistory.keyAt(i);
                PackageHistory history = userHistory.valueAt(i);
                xml.startTag((String) null, TAG_PACKAGE);
                xml.attribute((String) null, ATTR_NAME, packageName);
                xml.attribute((String) null, ATTR_ELAPSED_IDLE, Long.toString(history.lastUsedElapsedTime));
                xml.attribute((String) null, ATTR_SCREEN_IDLE, Long.toString(history.lastUsedScreenTime));
                xml.endTag((String) null, TAG_PACKAGE);
            }
            xml.endTag((String) null, TAG_PACKAGES);
            xml.endDocument();
            appIdleFile.finishWrite(fos);
        } catch (Exception e) {
            appIdleFile.failWrite(fos);
            Slog.e(TAG, "Error writing app idle file for user " + userId);
        }
    }

    public void dump(IndentingPrintWriter idpw, int userId) {
        idpw.println("Package idle stats:");
        idpw.increaseIndent();
        ArrayMap<String, PackageHistory> userHistory = this.mIdleHistory.get(userId);
        long elapsedRealtime = SystemClock.elapsedRealtime();
        long totalElapsedTime = getElapsedTimeLocked(elapsedRealtime);
        long screenOnTime = getScreenOnTimeLocked(elapsedRealtime);
        if (userHistory == null) {
            return;
        }
        int P = userHistory.size();
        for (int p = 0; p < P; p++) {
            String packageName = userHistory.keyAt(p);
            PackageHistory packageHistory = userHistory.valueAt(p);
            idpw.print("package=" + packageName);
            idpw.print(" lastUsedElapsed=");
            TimeUtils.formatDuration(totalElapsedTime - packageHistory.lastUsedElapsedTime, idpw);
            idpw.print(" lastUsedScreenOn=");
            TimeUtils.formatDuration(screenOnTime - packageHistory.lastUsedScreenTime, idpw);
            idpw.print(" idle=" + (isIdleLocked(packageName, userId, elapsedRealtime) ? "y" : "n"));
            idpw.println();
        }
        idpw.println();
        idpw.print("totalElapsedTime=");
        TimeUtils.formatDuration(getElapsedTimeLocked(elapsedRealtime), idpw);
        idpw.println();
        idpw.print("totalScreenOnTime=");
        TimeUtils.formatDuration(getScreenOnTimeLocked(elapsedRealtime), idpw);
        idpw.println();
        idpw.decreaseIndent();
    }

    public void dumpHistory(IndentingPrintWriter idpw, int userId) {
        ArrayMap<String, PackageHistory> userHistory = this.mIdleHistory.get(userId);
        long elapsedRealtime = SystemClock.elapsedRealtime();
        if (userHistory == null) {
            return;
        }
        int P = userHistory.size();
        for (int p = 0; p < P; p++) {
            String packageName = userHistory.keyAt(p);
            byte[] history = userHistory.valueAt(p).recent;
            for (int i = 0; i < 100; i++) {
                idpw.print(history[i] == 0 ? '.' : 'A');
            }
            idpw.print(" idle=" + (isIdleLocked(packageName, userId, elapsedRealtime) ? "y" : "n"));
            idpw.print("  " + packageName);
            idpw.println();
        }
    }
}
