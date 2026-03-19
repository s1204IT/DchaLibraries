package com.android.server.am;

import android.os.FileUtils;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class MemoryStatUtil {
    private static final String MEMCG_TEST_PATH = "/dev/memcg/apps/memory.stat";
    private static final String MEMORY_STAT_FILE_FMT = "/dev/memcg/apps/uid_%d/pid_%d/memory.stat";
    private static final int PGFAULT_INDEX = 9;
    private static final int PGMAJFAULT_INDEX = 11;
    private static final String PROC_STAT_FILE_FMT = "/proc/%d/stat";
    private static final int RSS_IN_BYTES_INDEX = 23;
    private static final String TAG = "ActivityManager";
    private static volatile Boolean sDeviceHasMemCg;
    private static final Pattern PGFAULT = Pattern.compile("total_pgfault (\\d+)");
    private static final Pattern PGMAJFAULT = Pattern.compile("total_pgmajfault (\\d+)");
    private static final Pattern RSS_IN_BYTES = Pattern.compile("total_rss (\\d+)");
    private static final Pattern CACHE_IN_BYTES = Pattern.compile("total_cache (\\d+)");
    private static final Pattern SWAP_IN_BYTES = Pattern.compile("total_swap (\\d+)");

    private MemoryStatUtil() {
    }

    static MemoryStat readMemoryStatFromFilesystem(int i, int i2) {
        return hasMemcg() ? readMemoryStatFromMemcg(i, i2) : readMemoryStatFromProcfs(i2);
    }

    static MemoryStat readMemoryStatFromMemcg(int i, int i2) {
        return parseMemoryStatFromMemcg(readFileContents(String.format(Locale.US, MEMORY_STAT_FILE_FMT, Integer.valueOf(i), Integer.valueOf(i2))));
    }

    static MemoryStat readMemoryStatFromProcfs(int i) {
        return parseMemoryStatFromProcfs(readFileContents(String.format(Locale.US, PROC_STAT_FILE_FMT, Integer.valueOf(i))));
    }

    private static String readFileContents(String str) {
        File file = new File(str);
        if (!file.exists()) {
            if (ActivityManagerDebugConfig.DEBUG_METRICS) {
                Slog.i(TAG, str + " not found");
            }
            return null;
        }
        try {
            return FileUtils.readTextFile(file, 0, null);
        } catch (IOException e) {
            Slog.e(TAG, "Failed to read file:", e);
            return null;
        }
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    static MemoryStat parseMemoryStatFromMemcg(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }
        MemoryStat memoryStat = new MemoryStat();
        Matcher matcher = PGFAULT.matcher(str);
        memoryStat.pgfault = matcher.find() ? Long.valueOf(matcher.group(1)).longValue() : 0L;
        Matcher matcher2 = PGMAJFAULT.matcher(str);
        memoryStat.pgmajfault = matcher2.find() ? Long.valueOf(matcher2.group(1)).longValue() : 0L;
        Matcher matcher3 = RSS_IN_BYTES.matcher(str);
        memoryStat.rssInBytes = matcher3.find() ? Long.valueOf(matcher3.group(1)).longValue() : 0L;
        Matcher matcher4 = CACHE_IN_BYTES.matcher(str);
        memoryStat.cacheInBytes = matcher4.find() ? Long.valueOf(matcher4.group(1)).longValue() : 0L;
        Matcher matcher5 = SWAP_IN_BYTES.matcher(str);
        memoryStat.swapInBytes = matcher5.find() ? Long.valueOf(matcher5.group(1)).longValue() : 0L;
        return memoryStat;
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    static MemoryStat parseMemoryStatFromProcfs(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }
        String[] strArrSplit = str.split(" ");
        if (strArrSplit.length < 24) {
            return null;
        }
        MemoryStat memoryStat = new MemoryStat();
        memoryStat.pgfault = Long.valueOf(strArrSplit[9]).longValue();
        memoryStat.pgmajfault = Long.valueOf(strArrSplit[11]).longValue();
        memoryStat.rssInBytes = Long.valueOf(strArrSplit[23]).longValue();
        return memoryStat;
    }

    static boolean hasMemcg() {
        if (sDeviceHasMemCg == null) {
            sDeviceHasMemCg = Boolean.valueOf(new File(MEMCG_TEST_PATH).exists());
        }
        return sDeviceHasMemCg.booleanValue();
    }

    static final class MemoryStat {
        long cacheInBytes;
        long pgfault;
        long pgmajfault;
        long rssInBytes;
        long swapInBytes;

        MemoryStat() {
        }
    }
}
