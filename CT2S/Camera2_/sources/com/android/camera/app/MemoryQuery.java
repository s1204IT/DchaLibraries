package com.android.camera.app;

import android.app.ActivityManager;
import android.os.Debug;
import android.os.Process;
import android.os.SystemClock;
import android.support.v4.media.session.PlaybackStateCompat;
import com.android.camera.debug.Log;
import java.util.HashMap;

public class MemoryQuery {
    public static final String KEY_DALVIK_PSS = "dalvikPSS";
    public static final String KEY_LARGE_MEMORY_CLASS = "largeMemoryClass";
    public static final String KEY_LAST_TRIM_LEVEL = "lastTrimLevel";
    public static final String KEY_LOW_MEMORY = "lowMemory";
    public static final String KEY_MEMORY_AVAILABLE = "availMem";
    public static final String KEY_MEMORY_CLASS = "memoryClass";
    public static final String KEY_NATIVE_PSS = "nativePSS";
    public static final String KEY_OTHER_PSS = "otherPSS";
    public static final String KEY_THRESHOLD = "threshold";
    public static final String KEY_TIMESTAMP = "timestamp";
    public static final String KEY_TOTAL_MEMORY = "totalMem";
    public static final String KEY_TOTAL_PRIVATE_DIRTY = "totalPrivateDirty";
    public static final String KEY_TOTAL_PSS = "totalPSS";
    public static final String KEY_TOTAL_SHARED_DIRTY = "totalSharedDirty";
    public static final String REPORT_LABEL_LAUNCH = "launch";
    private static final Log.Tag TAG = new Log.Tag("MemoryQuery");
    private final long BYTES_IN_KILOBYTE = PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID;
    private final long BYTES_IN_MEGABYTE = 1048576;
    private ActivityManager mActivityManager;

    public MemoryQuery(ActivityManager activityManager) {
        this.mActivityManager = activityManager;
    }

    public HashMap queryMemory() {
        int memoryClass = this.mActivityManager.getMemoryClass();
        int largeMemoryClass = this.mActivityManager.getLargeMemoryClass();
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        this.mActivityManager.getMemoryInfo(memoryInfo);
        long availMem = memoryInfo.availMem / 1048576;
        long totalMem = memoryInfo.totalMem / 1048576;
        long threshold = memoryInfo.threshold / 1048576;
        boolean lowMemory = memoryInfo.lowMemory;
        ActivityManager.RunningAppProcessInfo info = new ActivityManager.RunningAppProcessInfo();
        ActivityManager.getMyMemoryState(info);
        int appPID = Process.myPid();
        long timestamp = SystemClock.elapsedRealtime();
        long totalPrivateDirty = 0;
        long totalSharedDirty = 0;
        long totalPSS = 0;
        long nativePSS = 0;
        long dalvikPSS = 0;
        long otherPSS = 0;
        if (appPID != 0) {
            int[] pids = {appPID};
            Debug.MemoryInfo[] memoryInfoArray = this.mActivityManager.getProcessMemoryInfo(pids);
            totalPrivateDirty = ((long) memoryInfoArray[0].getTotalPrivateDirty()) / PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID;
            totalSharedDirty = ((long) memoryInfoArray[0].getTotalSharedDirty()) / PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID;
            totalPSS = ((long) memoryInfoArray[0].getTotalPss()) / PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID;
            nativePSS = ((long) memoryInfoArray[0].nativePss) / PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID;
            dalvikPSS = ((long) memoryInfoArray[0].dalvikPss) / PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID;
            otherPSS = ((long) memoryInfoArray[0].otherPss) / PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID;
        }
        HashMap outputData = new HashMap();
        outputData.put(KEY_TIMESTAMP, new Long(timestamp));
        outputData.put(KEY_MEMORY_AVAILABLE, new Long(availMem));
        outputData.put(KEY_TOTAL_MEMORY, new Long(totalMem));
        outputData.put(KEY_TOTAL_PSS, new Long(totalPSS));
        outputData.put(KEY_LAST_TRIM_LEVEL, new Integer(info.lastTrimLevel));
        outputData.put(KEY_TOTAL_PRIVATE_DIRTY, new Long(totalPrivateDirty));
        outputData.put(KEY_TOTAL_SHARED_DIRTY, new Long(totalSharedDirty));
        outputData.put(KEY_MEMORY_CLASS, new Long(memoryClass));
        outputData.put(KEY_LARGE_MEMORY_CLASS, new Long(largeMemoryClass));
        outputData.put(KEY_NATIVE_PSS, new Long(nativePSS));
        outputData.put(KEY_DALVIK_PSS, new Long(dalvikPSS));
        outputData.put(KEY_OTHER_PSS, new Long(otherPSS));
        outputData.put(KEY_THRESHOLD, new Long(threshold));
        outputData.put(KEY_LOW_MEMORY, new Boolean(lowMemory));
        Log.d(TAG, String.format("timestamp=%d, availMem=%d, totalMem=%d, totalPSS=%d, lastTrimLevel=%d, largeMemoryClass=%d, nativePSS=%d, dalvikPSS=%d, otherPSS=%d,threshold=%d, lowMemory=%s", Long.valueOf(timestamp), Long.valueOf(availMem), Long.valueOf(totalMem), Long.valueOf(totalPSS), Integer.valueOf(info.lastTrimLevel), Integer.valueOf(largeMemoryClass), Long.valueOf(nativePSS), Long.valueOf(dalvikPSS), Long.valueOf(otherPSS), Long.valueOf(threshold), Boolean.valueOf(lowMemory)));
        return outputData;
    }
}
