package com.android.server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Build;
import android.os.DropBoxManager;
import android.os.Environment;
import android.os.FileObserver;
import android.os.FileUtils;
import android.os.RecoverySystem;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.storage.StorageManager;
import android.provider.Downloads;
import android.provider.SettingsStringUtil;
import android.text.TextUtils;
import android.util.AtomicFile;
import android.util.EventLog;
import android.util.Slog;
import android.util.StatsLog;
import android.util.Xml;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class BootReceiver extends BroadcastReceiver {
    private static final String FSCK_FS_MODIFIED = "FILE SYSTEM WAS MODIFIED";
    private static final String FSCK_PASS_PATTERN = "Pass ([1-9]E?):";
    private static final String FSCK_TREE_OPTIMIZATION_PATTERN = "Inode [0-9]+ extent tree.*could be shorter";
    private static final int FS_STAT_FS_FIXED = 1024;
    private static final String FS_STAT_PATTERN = "fs_stat,[^,]*/([^/,]+),(0x[0-9a-fA-F]+)";
    private static final String LAST_HEADER_FILE = "last-header.txt";
    private static final String[] LAST_KMSG_FILES;
    private static final String LAST_SHUTDOWN_TIME_PATTERN = "powerctl_shutdown_time_ms:([0-9]+):([0-9]+)";
    private static final String LOG_FILES_FILE = "log-files.xml";
    private static final int LOG_SIZE;
    private static final String METRIC_SHUTDOWN_TIME_START = "begin_shutdown";
    private static final String METRIC_SYSTEM_SERVER = "shutdown_system_server";
    private static final String[] MOUNT_DURATION_PROPS_POSTFIX;
    private static final String OLD_UPDATER_CLASS = "com.google.android.systemupdater.SystemUpdateReceiver";
    private static final String OLD_UPDATER_PACKAGE = "com.google.android.systemupdater";
    private static final String SHUTDOWN_METRICS_FILE = "/data/system/shutdown-metrics.txt";
    private static final String SHUTDOWN_TRON_METRICS_PREFIX = "shutdown_";
    private static final String TAG = "BootReceiver";
    private static final String TAG_TOMBSTONE = "SYSTEM_TOMBSTONE";
    private static final File TOMBSTONE_DIR;
    private static final int UMOUNT_STATUS_NOT_AVAILABLE = 4;
    private static final File lastHeaderFile;
    private static final AtomicFile sFile;
    private static FileObserver sTombstoneObserver;

    static {
        LOG_SIZE = SystemProperties.getInt("ro.debuggable", 0) == 1 ? 98304 : 65536;
        TOMBSTONE_DIR = new File("/data/tombstones");
        sTombstoneObserver = null;
        sFile = new AtomicFile(new File(Environment.getDataSystemDirectory(), LOG_FILES_FILE), "log-files");
        lastHeaderFile = new File(Environment.getDataSystemDirectory(), LAST_HEADER_FILE);
        MOUNT_DURATION_PROPS_POSTFIX = new String[]{"early", PhoneConstants.APN_TYPE_DEFAULT, "late"};
        LAST_KMSG_FILES = new String[]{"/sys/fs/pstore/console-ramoops", "/proc/last_kmsg"};
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        new Thread() {
            @Override
            public void run() throws RemoteException {
                try {
                    BootReceiver.this.logBootEvents(context);
                } catch (Exception e) {
                    Slog.e(BootReceiver.TAG, "Can't log boot events", e);
                }
                boolean zIsOnlyCoreApps = false;
                try {
                    try {
                        zIsOnlyCoreApps = IPackageManager.Stub.asInterface(ServiceManager.getService("package")).isOnlyCoreApps();
                    } catch (RemoteException e2) {
                    }
                    if (!zIsOnlyCoreApps) {
                        BootReceiver.this.removeOldUpdatePackages(context);
                    }
                } catch (Exception e3) {
                    Slog.e(BootReceiver.TAG, "Can't remove old update packages", e3);
                }
            }
        }.start();
    }

    private void removeOldUpdatePackages(Context context) {
        Downloads.removeAllDownloadsByPackage(context, OLD_UPDATER_PACKAGE, OLD_UPDATER_CLASS);
    }

    private String getPreviousBootHeaders() {
        try {
            return FileUtils.readTextFile(lastHeaderFile, 0, null);
        } catch (IOException e) {
            return null;
        }
    }

    private String getCurrentBootHeaders() throws IOException {
        StringBuilder sb = new StringBuilder(512);
        sb.append("Build: ");
        sb.append(Build.FINGERPRINT);
        sb.append("\n");
        sb.append("Hardware: ");
        sb.append(Build.BOARD);
        sb.append("\n");
        sb.append("Revision: ");
        sb.append(SystemProperties.get("ro.revision", ""));
        sb.append("\n");
        sb.append("Bootloader: ");
        sb.append(Build.BOOTLOADER);
        sb.append("\n");
        sb.append("Radio: ");
        sb.append(Build.getRadioVersion());
        sb.append("\n");
        sb.append("Kernel: ");
        sb.append(FileUtils.readTextFile(new File("/proc/version"), 1024, "...\n"));
        sb.append("\n");
        return sb.toString();
    }

    private String getBootHeadersToLogAndUpdate() throws Exception {
        String previousBootHeaders = getPreviousBootHeaders();
        String currentBootHeaders = getCurrentBootHeaders();
        try {
            FileUtils.stringToFile(lastHeaderFile, currentBootHeaders);
        } catch (IOException e) {
            Slog.e(TAG, "Error writing " + lastHeaderFile, e);
        }
        if (previousBootHeaders == null) {
            return "isPrevious: false\n" + currentBootHeaders;
        }
        return "isPrevious: true\n" + previousBootHeaders;
    }

    private void logBootEvents(Context context) throws Exception {
        final DropBoxManager dropBoxManager = (DropBoxManager) context.getSystemService(Context.DROPBOX_SERVICE);
        final String bootHeadersToLogAndUpdate = getBootHeadersToLogAndUpdate();
        String str = SystemProperties.get("ro.boot.bootreason", null);
        String strHandleAftermath = RecoverySystem.handleAftermath(context);
        if (strHandleAftermath != null && dropBoxManager != null) {
            dropBoxManager.addText("SYSTEM_RECOVERY_LOG", bootHeadersToLogAndUpdate + strHandleAftermath);
        }
        String string = "";
        if (str != null) {
            StringBuilder sb = new StringBuilder(512);
            sb.append("\n");
            sb.append("Boot info:\n");
            sb.append("Last boot reason: ");
            sb.append(str);
            sb.append("\n");
            string = sb.toString();
        }
        HashMap<String, Long> timestamps = readTimestamps();
        if (SystemProperties.getLong("ro.runtime.firstboot", 0L) == 0) {
            if (!StorageManager.inCryptKeeperBounce()) {
                SystemProperties.set("ro.runtime.firstboot", Long.toString(System.currentTimeMillis()));
            }
            if (dropBoxManager != null) {
                dropBoxManager.addText("SYSTEM_BOOT", bootHeadersToLogAndUpdate);
            }
            String str2 = string;
            addFileWithFootersToDropBox(dropBoxManager, timestamps, bootHeadersToLogAndUpdate, str2, "/proc/last_kmsg", -LOG_SIZE, "SYSTEM_LAST_KMSG");
            addFileWithFootersToDropBox(dropBoxManager, timestamps, bootHeadersToLogAndUpdate, str2, "/sys/fs/pstore/console-ramoops", -LOG_SIZE, "SYSTEM_LAST_KMSG");
            addFileWithFootersToDropBox(dropBoxManager, timestamps, bootHeadersToLogAndUpdate, str2, "/sys/fs/pstore/console-ramoops-0", -LOG_SIZE, "SYSTEM_LAST_KMSG");
            addFileToDropBox(dropBoxManager, timestamps, bootHeadersToLogAndUpdate, "/cache/recovery/log", -LOG_SIZE, "SYSTEM_RECOVERY_LOG");
            addFileToDropBox(dropBoxManager, timestamps, bootHeadersToLogAndUpdate, "/cache/recovery/last_kmsg", -LOG_SIZE, "SYSTEM_RECOVERY_KMSG");
            addAuditErrorsToDropBox(dropBoxManager, timestamps, bootHeadersToLogAndUpdate, -LOG_SIZE, "SYSTEM_AUDIT");
        } else if (dropBoxManager != null) {
            dropBoxManager.addText("SYSTEM_RESTART", bootHeadersToLogAndUpdate);
        }
        logFsShutdownTime();
        logFsMountTime();
        addFsckErrorsToDropBoxAndLogFsStat(dropBoxManager, timestamps, bootHeadersToLogAndUpdate, -LOG_SIZE, "SYSTEM_FSCK");
        logSystemServerShutdownTimeMetrics();
        File[] fileArrListFiles = TOMBSTONE_DIR.listFiles();
        for (int i = 0; fileArrListFiles != null && i < fileArrListFiles.length; i++) {
            if (fileArrListFiles[i].isFile()) {
                addFileToDropBox(dropBoxManager, timestamps, bootHeadersToLogAndUpdate, fileArrListFiles[i].getPath(), LOG_SIZE, TAG_TOMBSTONE);
            }
        }
        writeTimestamps(timestamps);
        sTombstoneObserver = new FileObserver(TOMBSTONE_DIR.getPath(), 8) {
            @Override
            public void onEvent(int i2, String str3) {
                HashMap timestamps2 = BootReceiver.readTimestamps();
                try {
                    File file = new File(BootReceiver.TOMBSTONE_DIR, str3);
                    if (file.isFile()) {
                        BootReceiver.addFileToDropBox(dropBoxManager, timestamps2, bootHeadersToLogAndUpdate, file.getPath(), BootReceiver.LOG_SIZE, BootReceiver.TAG_TOMBSTONE);
                    }
                } catch (IOException e) {
                    Slog.e(BootReceiver.TAG, "Can't log tombstone", e);
                }
                BootReceiver.this.writeTimestamps(timestamps2);
            }
        };
        sTombstoneObserver.startWatching();
    }

    private static void addFileToDropBox(DropBoxManager dropBoxManager, HashMap<String, Long> map, String str, String str2, int i, String str3) throws IOException {
        addFileWithFootersToDropBox(dropBoxManager, map, str, "", str2, i, str3);
    }

    private static void addFileWithFootersToDropBox(DropBoxManager dropBoxManager, HashMap<String, Long> map, String str, String str2, String str3, int i, String str4) throws IOException {
        if (dropBoxManager == null || !dropBoxManager.isTagEnabled(str4)) {
            return;
        }
        File file = new File(str3);
        long jLastModified = file.lastModified();
        if (jLastModified <= 0) {
            return;
        }
        if (map.containsKey(str3) && map.get(str3).longValue() == jLastModified) {
            return;
        }
        map.put(str3, Long.valueOf(jLastModified));
        String textFile = FileUtils.readTextFile(file, i, "[[TRUNCATED]]\n");
        String str5 = str + textFile + str2;
        if (str4.equals(TAG_TOMBSTONE) && textFile.contains(">>> system_server <<<")) {
            addTextToDropBox(dropBoxManager, "system_server_native_crash", str5, str3, i);
        }
        addTextToDropBox(dropBoxManager, str4, str5, str3, i);
    }

    private static void addTextToDropBox(DropBoxManager dropBoxManager, String str, String str2, String str3, int i) {
        Slog.i(TAG, "Copying " + str3 + " to DropBox (" + str + ")");
        dropBoxManager.addText(str, str2);
        EventLog.writeEvent(DropboxLogTags.DROPBOX_FILE_COPY, str3, Integer.valueOf(i), str);
    }

    private static void addAuditErrorsToDropBox(DropBoxManager dropBoxManager, HashMap<String, Long> map, String str, int i, String str2) throws IOException {
        if (dropBoxManager == null || !dropBoxManager.isTagEnabled(str2)) {
            return;
        }
        Slog.i(TAG, "Copying audit failures to DropBox");
        File file = new File("/proc/last_kmsg");
        long jLastModified = file.lastModified();
        if (jLastModified <= 0) {
            file = new File("/sys/fs/pstore/console-ramoops");
            jLastModified = file.lastModified();
            if (jLastModified <= 0) {
                file = new File("/sys/fs/pstore/console-ramoops-0");
                jLastModified = file.lastModified();
            }
        }
        if (jLastModified <= 0) {
            return;
        }
        if (map.containsKey(str2) && map.get(str2).longValue() == jLastModified) {
            return;
        }
        map.put(str2, Long.valueOf(jLastModified));
        String textFile = FileUtils.readTextFile(file, i, "[[TRUNCATED]]\n");
        StringBuilder sb = new StringBuilder();
        for (String str3 : textFile.split("\n")) {
            if (str3.contains("audit")) {
                sb.append(str3 + "\n");
            }
        }
        Slog.i(TAG, "Copied " + sb.toString().length() + " worth of audits to DropBox");
        StringBuilder sb2 = new StringBuilder();
        sb2.append(str);
        sb2.append(sb.toString());
        dropBoxManager.addText(str2, sb2.toString());
    }

    private static void addFsckErrorsToDropBoxAndLogFsStat(DropBoxManager dropBoxManager, HashMap<String, Long> map, String str, int i, String str2) throws IOException {
        String str3;
        boolean z;
        File file;
        if (dropBoxManager == null) {
            str3 = str2;
        } else {
            str3 = str2;
            if (dropBoxManager.isTagEnabled(str3)) {
                z = true;
            }
            Slog.i(TAG, "Checking for fsck errors");
            file = new File("/dev/fscklogs/log");
            if (file.lastModified() > 0) {
                return;
            }
            String textFile = FileUtils.readTextFile(file, i, "[[TRUNCATED]]\n");
            Pattern patternCompile = Pattern.compile(FS_STAT_PATTERN);
            String[] strArrSplit = textFile.split("\n");
            boolean z2 = false;
            int i2 = 0;
            int i3 = 0;
            for (String str4 : strArrSplit) {
                if (!str4.contains(FSCK_FS_MODIFIED)) {
                    if (str4.contains("fs_stat")) {
                        Matcher matcher = patternCompile.matcher(str4);
                        if (matcher.find()) {
                            handleFsckFsStat(matcher, strArrSplit, i2, i3);
                            i2 = i3;
                        } else {
                            Slog.w(TAG, "cannot parse fs_stat:" + str4);
                        }
                    }
                } else {
                    z2 = true;
                }
                i3++;
            }
            if (z && z2) {
                addFileToDropBox(dropBoxManager, map, str, "/dev/fscklogs/log", i, str3);
            }
            file.delete();
            return;
        }
        z = false;
        Slog.i(TAG, "Checking for fsck errors");
        file = new File("/dev/fscklogs/log");
        if (file.lastModified() > 0) {
        }
    }

    private static void logFsMountTime() {
        for (String str : MOUNT_DURATION_PROPS_POSTFIX) {
            int i = SystemProperties.getInt("ro.boottime.init.mount_all." + str, 0);
            if (i != 0) {
                MetricsLogger.histogram(null, "boot_mount_all_duration_" + str, i);
            }
        }
    }

    private static void logSystemServerShutdownTimeMetrics() {
        String textFile;
        File file = new File(SHUTDOWN_METRICS_FILE);
        if (file.exists()) {
            try {
                textFile = FileUtils.readTextFile(file, 0, null);
            } catch (IOException e) {
                Slog.e(TAG, "Problem reading " + file, e);
                textFile = null;
            }
        } else {
            textFile = null;
        }
        if (!TextUtils.isEmpty(textFile)) {
            String str = null;
            String str2 = null;
            String str3 = null;
            String str4 = null;
            for (String str5 : textFile.split(",")) {
                String[] strArrSplit = str5.split(SettingsStringUtil.DELIMITER);
                if (strArrSplit.length != 2) {
                    Slog.e(TAG, "Wrong format of shutdown metrics - " + textFile);
                } else {
                    if (strArrSplit[0].startsWith(SHUTDOWN_TRON_METRICS_PREFIX)) {
                        logTronShutdownMetric(strArrSplit[0], strArrSplit[1]);
                        if (strArrSplit[0].equals(METRIC_SYSTEM_SERVER)) {
                            str4 = strArrSplit[1];
                        }
                    }
                    if (strArrSplit[0].equals("reboot")) {
                        str = strArrSplit[1];
                    } else if (strArrSplit[0].equals("reason")) {
                        str2 = strArrSplit[1];
                    } else if (strArrSplit[0].equals(METRIC_SHUTDOWN_TIME_START)) {
                        str3 = strArrSplit[1];
                    }
                }
            }
            logStatsdShutdownAtom(str, str2, str3, str4);
        }
        file.delete();
    }

    private static void logTronShutdownMetric(String str, String str2) {
        try {
            int i = Integer.parseInt(str2);
            if (i >= 0) {
                MetricsLogger.histogram(null, str, i);
            }
        } catch (NumberFormatException e) {
            Slog.e(TAG, "Cannot parse metric " + str + " int value - " + str2);
        }
    }

    private static void logStatsdShutdownAtom(String str, String str2, String str3, String str4) {
        boolean z;
        String str5;
        long j;
        long j2;
        if (str != null) {
            if (str.equals("y")) {
                z = true;
                boolean z2 = z;
                if (str2 != null) {
                    Slog.e(TAG, "No value received for shutdown reason");
                    str5 = "<EMPTY>";
                } else {
                    str5 = str2;
                }
                if (str3 == null) {
                    try {
                        j = Long.parseLong(str3);
                    } catch (NumberFormatException e) {
                        Slog.e(TAG, "Cannot parse shutdown start time: " + str3);
                        j = 0;
                    }
                    if (str4 != null) {
                        try {
                            j2 = Long.parseLong(str4);
                        } catch (NumberFormatException e2) {
                            Slog.e(TAG, "Cannot parse shutdown duration: " + str3);
                            j2 = 0;
                        }
                        StatsLog.write(56, z2, str5, j, j2);
                    }
                    Slog.e(TAG, "No value received for shutdown duration");
                    j2 = 0;
                    StatsLog.write(56, z2, str5, j, j2);
                }
                Slog.e(TAG, "No value received for shutdown start time");
                j = 0;
                if (str4 != null) {
                }
                j2 = 0;
                StatsLog.write(56, z2, str5, j, j2);
            }
            if (!str.equals("n")) {
                Slog.e(TAG, "Unexpected value for reboot : " + str);
            }
        } else {
            Slog.e(TAG, "No value received for reboot");
        }
        z = false;
        boolean z22 = z;
        if (str2 != null) {
        }
        if (str3 == null) {
        }
        j = 0;
        if (str4 != null) {
        }
        j2 = 0;
        StatsLog.write(56, z22, str5, j, j2);
    }

    private static void logFsShutdownTime() {
        File file;
        String[] strArr = LAST_KMSG_FILES;
        int length = strArr.length;
        int i = 0;
        while (true) {
            if (i < length) {
                file = new File(strArr[i]);
                if (file.exists()) {
                    break;
                } else {
                    i++;
                }
            } else {
                file = null;
                break;
            }
        }
        if (file == null) {
            return;
        }
        try {
            Matcher matcher = Pattern.compile(LAST_SHUTDOWN_TIME_PATTERN, 8).matcher(FileUtils.readTextFile(file, -16384, null));
            if (matcher.find()) {
                MetricsLogger.histogram(null, "boot_fs_shutdown_duration", Integer.parseInt(matcher.group(1)));
                MetricsLogger.histogram(null, "boot_fs_shutdown_umount_stat", Integer.parseInt(matcher.group(2)));
                Slog.i(TAG, "boot_fs_shutdown," + matcher.group(1) + "," + matcher.group(2));
                return;
            }
            MetricsLogger.histogram(null, "boot_fs_shutdown_umount_stat", 4);
            Slog.w(TAG, "boot_fs_shutdown, string not found");
        } catch (IOException e) {
            Slog.w(TAG, "cannot read last msg", e);
        }
    }

    @VisibleForTesting
    public static int fixFsckFsStat(String str, int i, String[] strArr, int i2, int i3) {
        boolean z;
        String str2;
        if ((i & 1024) != 0) {
            Pattern patternCompile = Pattern.compile(FSCK_PASS_PATTERN);
            Pattern patternCompile2 = Pattern.compile(FSCK_TREE_OPTIMIZATION_PATTERN);
            String strGroup = "";
            boolean z2 = false;
            boolean z3 = false;
            boolean z4 = false;
            int i4 = i2;
            while (true) {
                z = true;
                if (i4 >= i3) {
                    break;
                }
                str2 = strArr[i4];
                if (str2.contains(FSCK_FS_MODIFIED)) {
                    break;
                }
                if (str2.startsWith("Pass ")) {
                    Matcher matcher = patternCompile.matcher(str2);
                    if (matcher.find()) {
                        strGroup = matcher.group(1);
                    }
                } else if (str2.startsWith("Inode ")) {
                    if (!patternCompile2.matcher(str2).find() || !strGroup.equals(WifiEnterpriseConfig.ENGINE_ENABLE)) {
                        break;
                    }
                    Slog.i(TAG, "fs_stat, partition:" + str + " found tree optimization:" + str2);
                    z2 = true;
                } else if (str2.startsWith("[QUOTA WARNING]") && strGroup.equals("5")) {
                    Slog.i(TAG, "fs_stat, partition:" + str + " found quota warning:" + str2);
                    if (!z2) {
                        z3 = true;
                        z = false;
                        break;
                    }
                    z3 = true;
                } else if (!str2.startsWith("Update quota info") || !strGroup.equals("5")) {
                    if (!str2.startsWith("Timestamp(s) on inode") || !str2.contains("beyond 2310-04-04 are likely pre-1970") || !strGroup.equals(WifiEnterpriseConfig.ENGINE_ENABLE)) {
                        String strTrim = str2.trim();
                        if (!strTrim.isEmpty() && !strGroup.isEmpty()) {
                            str2 = strTrim;
                            break;
                        }
                    } else {
                        Slog.i(TAG, "fs_stat, partition:" + str + " found timestamp adjustment:" + str2);
                        int i5 = i4 + 1;
                        if (strArr[i5].contains("Fix? yes")) {
                            i4 = i5;
                        }
                        z4 = true;
                    }
                }
                i4++;
            }
            if (z) {
                if (str2 != null) {
                    Slog.i(TAG, "fs_stat, partition:" + str + " fix:" + str2);
                }
            } else if (z3 && !z2) {
                Slog.i(TAG, "fs_stat, got quota fix without tree optimization, partition:" + str);
            } else if ((z2 && z3) || z4) {
                Slog.i(TAG, "fs_stat, partition:" + str + " fix ignored");
                return i & (-1025);
            }
        }
        return i;
    }

    private static void handleFsckFsStat(Matcher matcher, String[] strArr, int i, int i2) {
        String strGroup = matcher.group(1);
        try {
            int iFixFsckFsStat = fixFsckFsStat(strGroup, Integer.decode(matcher.group(2)).intValue(), strArr, i, i2);
            MetricsLogger.histogram(null, "boot_fs_stat_" + strGroup, iFixFsckFsStat);
            Slog.i(TAG, "fs_stat, partition:" + strGroup + " stat:0x" + Integer.toHexString(iFixFsckFsStat));
        } catch (NumberFormatException e) {
            Slog.w(TAG, "cannot parse fs_stat: partition:" + strGroup + " stat:" + matcher.group(2));
        }
    }

    private static HashMap<String, Long> readTimestamps() {
        HashMap<String, Long> map;
        FileInputStream fileInputStreamOpenRead;
        XmlPullParser xmlPullParserNewPullParser;
        int next;
        synchronized (sFile) {
            map = new HashMap<>();
            boolean z = false;
            try {
                try {
                    fileInputStreamOpenRead = sFile.openRead();
                    try {
                        xmlPullParserNewPullParser = Xml.newPullParser();
                        xmlPullParserNewPullParser.setInput(fileInputStreamOpenRead, StandardCharsets.UTF_8.name());
                        do {
                            next = xmlPullParserNewPullParser.next();
                            if (next == 2) {
                                break;
                            }
                        } while (next != 1);
                    } finally {
                    }
                } catch (Throwable th) {
                    th = th;
                }
            } catch (FileNotFoundException e) {
            } catch (IOException e2) {
                e = e2;
            } catch (IllegalStateException e3) {
                e = e3;
            } catch (NullPointerException e4) {
                e = e4;
            } catch (XmlPullParserException e5) {
                e = e5;
            }
            if (next != 2) {
                throw new IllegalStateException("no start tag found");
            }
            int depth = xmlPullParserNewPullParser.getDepth();
            while (true) {
                int next2 = xmlPullParserNewPullParser.next();
                if (next2 == 1 || (next2 == 3 && xmlPullParserNewPullParser.getDepth() <= depth)) {
                    break;
                }
                if (next2 != 3 && next2 != 4) {
                    if (xmlPullParserNewPullParser.getName().equals("log")) {
                        map.put(xmlPullParserNewPullParser.getAttributeValue(null, "filename"), Long.valueOf(Long.valueOf(xmlPullParserNewPullParser.getAttributeValue(null, "timestamp")).longValue()));
                    } else {
                        Slog.w(TAG, "Unknown tag: " + xmlPullParserNewPullParser.getName());
                        XmlUtils.skipCurrentTag(xmlPullParserNewPullParser);
                    }
                }
            }
            if (fileInputStreamOpenRead != null) {
                try {
                    fileInputStreamOpenRead.close();
                } catch (FileNotFoundException e6) {
                    z = true;
                    Slog.i(TAG, "No existing last log timestamp file " + sFile.getBaseFile() + "; starting empty");
                    if (!z) {
                        map.clear();
                    }
                } catch (IOException e7) {
                    e = e7;
                    z = true;
                    Slog.w(TAG, "Failed parsing " + e);
                    if (!z) {
                        map.clear();
                    }
                } catch (IllegalStateException e8) {
                    e = e8;
                    z = true;
                    Slog.w(TAG, "Failed parsing " + e);
                    if (!z) {
                        map.clear();
                    }
                } catch (NullPointerException e9) {
                    e = e9;
                    z = true;
                    Slog.w(TAG, "Failed parsing " + e);
                    if (!z) {
                        map.clear();
                    }
                } catch (XmlPullParserException e10) {
                    e = e10;
                    z = true;
                    Slog.w(TAG, "Failed parsing " + e);
                    if (!z) {
                    }
                } catch (Throwable th2) {
                    th = th2;
                    z = true;
                    if (!z) {
                        map.clear();
                    }
                    throw th;
                }
            }
        }
        return map;
    }

    private void writeTimestamps(HashMap<String, Long> map) {
        synchronized (sFile) {
            try {
                try {
                    FileOutputStream fileOutputStreamStartWrite = sFile.startWrite();
                    try {
                        FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
                        fastXmlSerializer.setOutput(fileOutputStreamStartWrite, StandardCharsets.UTF_8.name());
                        fastXmlSerializer.startDocument(null, true);
                        fastXmlSerializer.startTag(null, "log-files");
                        for (String str : map.keySet()) {
                            fastXmlSerializer.startTag(null, "log");
                            fastXmlSerializer.attribute(null, "filename", str);
                            fastXmlSerializer.attribute(null, "timestamp", map.get(str).toString());
                            fastXmlSerializer.endTag(null, "log");
                        }
                        fastXmlSerializer.endTag(null, "log-files");
                        fastXmlSerializer.endDocument();
                        sFile.finishWrite(fileOutputStreamStartWrite);
                    } catch (IOException e) {
                        Slog.w(TAG, "Failed to write timestamp file, using the backup: " + e);
                        sFile.failWrite(fileOutputStreamStartWrite);
                    }
                } catch (IOException e2) {
                    Slog.w(TAG, "Failed to write timestamp file: " + e2);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }
}
