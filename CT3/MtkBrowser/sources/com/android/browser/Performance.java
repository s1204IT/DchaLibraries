package com.android.browser;

import android.app.ActivityManager;
import android.content.Context;
import android.net.ParseException;
import android.net.WebAddress;
import android.os.Debug;
import android.util.Log;
import com.android.internal.util.MemInfoReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.CopyOnWriteArrayList;

public class Performance {
    private static boolean mInTrace;
    private static ActivityManager.MemoryInfo mSysMemThreshold;
    private static final boolean LOGD_ENABLED = Browser.DEBUG;
    private static long mTotalMem = 0;
    private static long mVisibleAppThreshold = 0;
    private static final Object mLock = new Object();
    private static final int[] SYSTEM_CPU_FORMAT = {288, 8224, 8224, 8224, 8224, 8224, 8224, 8224};

    static void tracePageStart(String url) {
        String host;
        if (!BrowserSettings.getInstance().isTracing()) {
            return;
        }
        try {
            WebAddress uri = new WebAddress(url);
            host = uri.getHost();
        } catch (ParseException e) {
            host = "browser";
        }
        String host2 = host.replace('.', '_') + ".trace";
        mInTrace = true;
        Debug.startMethodTracing(host2, 20971520);
    }

    static void tracePageFinished() {
        if (!mInTrace) {
            return;
        }
        mInTrace = false;
        Debug.stopMethodTracing();
    }

    static String encodeToJSON(Debug.MemoryInfo memoryInfo) {
        StringBuilder memoryUsage = new StringBuilder();
        memoryUsage.append("{\r\n").append("    \"Browser app (MB)\": {\r\n").append("        \"Browser\": {\r\n").append("            \"Pss\": {\r\n").append(String.format("                \"DVM\": %.2f,\r\n", Double.valueOf(((double) memoryInfo.dalvikPss) / 1024.0d))).append(String.format("                \"Native\": %.2f,\r\n", Double.valueOf(((double) memoryInfo.nativePss) / 1024.0d))).append(String.format("                \"Other\": %.2f,\r\n", Double.valueOf(((double) memoryInfo.otherPss) / 1024.0d))).append(String.format("                \"Total\": %.2f\r\n", Double.valueOf(((double) memoryInfo.getTotalPss()) / 1024.0d))).append("            },\r\n").append("            \"Private\": {\r\n").append(String.format("                \"DVM\": %.2f,\r\n", Double.valueOf(((double) memoryInfo.dalvikPrivateDirty) / 1024.0d))).append(String.format("                \"Native\": %.2f,\r\n", Double.valueOf(((double) memoryInfo.nativePrivateDirty) / 1024.0d))).append(String.format("                \"Other\": %.2f,\r\n", Double.valueOf(((double) memoryInfo.otherPrivateDirty) / 1024.0d))).append(String.format("                \"Total\": %.2f\r\n", Double.valueOf(((double) memoryInfo.getTotalPrivateDirty()) / 1024.0d))).append("            },\r\n").append("            \"Swapped\": {\r\n").append(String.format("                \"DVM\": %.2f,\r\n", Double.valueOf(((double) memoryInfo.dalvikSwappedOut) / 1024.0d))).append(String.format("                \"Native\": %.2f,\r\n", Double.valueOf(((double) memoryInfo.nativeSwappedOut) / 1024.0d))).append(String.format("                \"Other\": %.2f,\r\n", Double.valueOf(((double) memoryInfo.otherSwappedOut) / 1024.0d))).append(String.format("                \"Total\": %.2f\r\n", Double.valueOf(((double) memoryInfo.getTotalSwappedOut()) / 1024.0d))).append("            },\r\n").append("            \"Shared\": {\r\n").append(String.format("                \"DVM\": %.2f,\r\n", Double.valueOf(((double) memoryInfo.dalvikSharedDirty) / 1024.0d))).append(String.format("                \"Native\": %.2f,\r\n", Double.valueOf(((double) memoryInfo.nativeSharedDirty) / 1024.0d))).append(String.format("                \"Other\": %.2f,\r\n", Double.valueOf(((double) memoryInfo.otherSharedDirty) / 1024.0d))).append(String.format("                \"Total\": %.2f\r\n", Double.valueOf(((double) memoryInfo.getTotalSharedDirty()) / 1024.0d))).append("            }\r\n").append("        },\r\n");
        for (int i = 0; i < 17; i++) {
            memoryUsage.append("        \"").append(Debug.MemoryInfo.getOtherLabel(i)).append("\": {\r\n").append("            \"Pss\": {\r\n").append(String.format("                \"Total\": %.2f\r\n", Double.valueOf(((double) memoryInfo.getOtherPss(i)) / 1024.0d))).append("            },\r\n").append("            \"Private\": {\r\n").append(String.format("                \"Total\": %.2f\r\n", Double.valueOf(((double) memoryInfo.getOtherPrivateDirty(i)) / 1024.0d))).append("            },\r\n").append("            \"Shared\": {\r\n").append(String.format("                \"Total\": %.2f\r\n", Double.valueOf(((double) memoryInfo.getOtherSharedDirty(i)) / 1024.0d))).append("            }\r\n");
            if (i + 1 == 17) {
                memoryUsage.append("        }\r\n").append("    }\r\n").append("}\r\n");
            } else {
                memoryUsage.append("        },\r\n");
            }
        }
        return memoryUsage.toString();
    }

    static String printMemoryInfo(boolean log2File, String flag) {
        Debug.MemoryInfo memoryInfo = new Debug.MemoryInfo();
        Debug.getMemoryInfo(memoryInfo);
        StringBuilder memMessage = new StringBuilder();
        memMessage.append(flag).append(" Browser Memory usage: (Total/DVM/Native/Other) \r\n").append(flag).append(String.format(" Pss=%.2f/%.2f/%.2f/%.2f MB\r\n", Double.valueOf(((double) memoryInfo.getTotalPss()) / 1024.0d), Double.valueOf(((double) memoryInfo.dalvikPss) / 1024.0d), Double.valueOf(((double) memoryInfo.nativePss) / 1024.0d), Double.valueOf(((double) memoryInfo.otherPss) / 1024.0d))).append(flag).append(String.format(" Private=%.2f/%.2f/%.2f/%.2f MB\r\n", Double.valueOf(((double) memoryInfo.getTotalPrivateDirty()) / 1024.0d), Double.valueOf(((double) memoryInfo.dalvikPrivateDirty) / 1024.0d), Double.valueOf(((double) memoryInfo.nativePrivateDirty) / 1024.0d), Double.valueOf(((double) memoryInfo.otherPrivateDirty) / 1024.0d))).append(flag).append(String.format(" Shared=%.2f/%.2f/%.2f/%.2f MB\r\n", Double.valueOf(((double) memoryInfo.getTotalSharedDirty()) / 1024.0d), Double.valueOf(((double) memoryInfo.dalvikSharedDirty) / 1024.0d), Double.valueOf(((double) memoryInfo.nativeSharedDirty) / 1024.0d), Double.valueOf(((double) memoryInfo.otherSharedDirty) / 1024.0d))).append(flag).append(String.format(" Swapped=%.2f/%.2f/%.2f/%.2f MB", Double.valueOf(((double) memoryInfo.getTotalSwappedOut()) / 1024.0d), Double.valueOf(((double) memoryInfo.dalvikSwappedOut) / 1024.0d), Double.valueOf(((double) memoryInfo.nativeSwappedOut) / 1024.0d), Double.valueOf(((double) memoryInfo.otherSwappedOut) / 1024.0d)));
        String otherMemMsg = "Browser other mem statistics: \r\n";
        for (int i = 0; i < 17; i++) {
            otherMemMsg = otherMemMsg + " [" + String.valueOf(i) + "] " + Debug.MemoryInfo.getOtherLabel(i) + ", pss=" + String.format("%.2fMB", Double.valueOf(((double) memoryInfo.getOtherPss(i)) / 1024.0d)) + ", private=" + String.format("%.2fMB", Double.valueOf(((double) memoryInfo.getOtherPrivateDirty(i)) / 1024.0d)) + ", shared=" + String.format("%.2fMB", Double.valueOf(((double) memoryInfo.getOtherSharedDirty(i)) / 1024.0d)) + "\r\n";
        }
        if (!log2File) {
            Log.d("browser", memMessage.toString());
            Log.d("browser", otherMemMsg);
            return "";
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String outputFileName = "/storage/emulated/0/memDumpLog" + sdf.format(new Date()) + ".txt";
            PrintWriter printWriter = new PrintWriter(outputFileName);
            printWriter.print(encodeToJSON(memoryInfo));
            printWriter.close();
            return outputFileName;
        } catch (IOException ex) {
            Log.d("browser", "Failed to save memory logs to file, " + ex.getMessage());
            return "";
        }
    }

    static void dumpSystemMemInfo(Context context) {
        if (context == null || mSysMemThreshold != null) {
            return;
        }
        mSysMemThreshold = new ActivityManager.MemoryInfo();
        ((ActivityManager) context.getSystemService("activity")).getMemoryInfo(mSysMemThreshold);
        mTotalMem = mSysMemThreshold.totalMem;
        mVisibleAppThreshold = mSysMemThreshold.visibleAppThreshold;
        if (!LOGD_ENABLED) {
            return;
        }
        String flag = "MemoryDumpInfo" + System.currentTimeMillis();
        Log.d("browser", "Browser Current Memory Dump time = " + flag);
        printSysMemInfo(mSysMemThreshold, flag);
    }

    static boolean checkShouldReleaseTabs(int visibleWebviewNums, ArrayList<Integer> tabIndex, boolean isFreeMemory, String url, CopyOnWriteArrayList<Integer> freeTabIndexs, boolean isRemoveTab) {
        boolean isReleaseTabs;
        synchronized (mLock) {
            isReleaseTabs = false;
            String flag = "MemoryDumpInfo" + System.currentTimeMillis();
            Log.d("browser", "Browser Current Memory Dump time = " + flag);
            if (LOGD_ENABLED) {
                if (isFreeMemory) {
                    if (!isRemoveTab) {
                        Log.d("browser", flag + " Performance#checkShouldReleaseTabs()-->tabPosition = " + tabIndex + ", url = " + url);
                    }
                } else if (isRemoveTab) {
                    Log.d("browser", flag + " Perfromance#checkShouldReleaseTabs()--->removeTabIndex = " + tabIndex);
                } else {
                    Log.d("browser", flag + " Performance#checkShouldReleaseTabs()-->freeTabIndex = " + freeTabIndexs);
                }
            }
            MemInfoReader sysMemInfo = new MemInfoReader();
            sysMemInfo.readMemInfo();
            if (LOGD_ENABLED) {
                printProcessMemInfo(sysMemInfo, flag);
                printMemoryInfo(false, flag);
            }
            Debug.MemoryInfo processMemoryInfo = new Debug.MemoryInfo();
            Debug.getMemoryInfo(processMemoryInfo);
            double totalPss = ((double) (processMemoryInfo.getTotalPss() + processMemoryInfo.getSummaryTotalSwap())) * 1024.0d;
            double useage = totalPss / mTotalMem;
            if (LOGD_ENABLED) {
                NumberFormat nf = NumberFormat.getInstance();
                nf.setMaximumFractionDigits(3);
                Log.d("browser", flag + " current porcess take up the memory percent is " + nf.format(useage));
            }
            if (Math.max(sysMemInfo.getFreeSize(), sysMemInfo.getCachedSize()) < mVisibleAppThreshold) {
                if (LOGD_ENABLED) {
                    Log.d("browser", "Browser Pss =: " + (((double) processMemoryInfo.getTotalPss()) / 1024.0d) + " PSwap =: " + (processMemoryInfo.getTotalSwappedOut() / 1024.0f) + " SwappablePss =: " + (processMemoryInfo.getTotalSwappablePss() / 1024.0f));
                }
                if (useage > 0.4000000059604645d && visibleWebviewNums > 5 && isFreeMemory) {
                    isReleaseTabs = true;
                }
            }
        }
        return isReleaseTabs;
    }

    static void printSysMemInfo(ActivityManager.MemoryInfo sysMemThreshold, String flag) {
        if (sysMemThreshold != null) {
            long total = sysMemThreshold.totalMem;
            long threshold = sysMemThreshold.threshold;
            long availMem = sysMemThreshold.availMem;
            long hiddenAppThreshold = sysMemThreshold.hiddenAppThreshold;
            long secondaryServerThreshold = sysMemThreshold.secondaryServerThreshold;
            long visibleAppThreshold = sysMemThreshold.visibleAppThreshold;
            long foregroundAppThreshold = sysMemThreshold.foregroundAppThreshold;
            StringBuilder sysMemUsage = new StringBuilder();
            sysMemUsage.append("{\r\n").append(flag).append("    \"System Memory Usage (MB)\": {\r\n").append(flag).append(String.format("                total=: %.2f,\r\n", Double.valueOf((total / 1024.0d) / 1024.0d))).append(flag).append(String.format("                threshold=: %.2f,\r\n", Double.valueOf((threshold / 1024.0d) / 1024.0d))).append(flag).append(String.format("                availMem=: %.2f,\r\n", Double.valueOf((availMem / 1024.0d) / 1024.0d))).append(flag).append(String.format("                hiddenAppThreshold=: %.2f,\r\n", Double.valueOf((hiddenAppThreshold / 1024.0d) / 1024.0d))).append(flag).append(String.format("                secondaryServerThreshold=: %.2f,\r\n", Double.valueOf((secondaryServerThreshold / 1024.0d) / 1024.0d))).append(flag).append(String.format("                visibleAppThreshold=: %.2f,\r\n", Double.valueOf((visibleAppThreshold / 1024.0d) / 1024.0d))).append(flag).append(String.format("                foregroundAppThreshold=: %.2f,\r\n", Double.valueOf((foregroundAppThreshold / 1024.0d) / 1024.0d)));
            Log.d("browser", sysMemUsage.toString());
        }
    }

    static void printProcessMemInfo(MemInfoReader processMemInfo, String flag) {
        if (processMemInfo != null) {
            StringBuilder processMemUsage = new StringBuilder();
            processMemUsage.append(flag).append("{\r\n").append(flag).append("    \"Process Memory Usage (MB)\": {\r\n").append(flag).append(String.format("                TotalSize =: %.2f,\r\n", Double.valueOf((processMemInfo.getTotalSize() / 1024.0d) / 1024.0d))).append(flag).append(String.format("                FreeSize =: %.2f,\r\n", Double.valueOf((processMemInfo.getFreeSize() / 1024.0d) / 1024.0d))).append(flag).append(String.format("                MappedSize =: %.2f,\r\n", Double.valueOf((processMemInfo.getMappedSize() / 1024.0d) / 1024.0d))).append(flag).append(String.format("                BuffersSize =: %.2f,\r\n", Double.valueOf((processMemInfo.getBuffersSize() / 1024.0d) / 1024.0d))).append(flag).append(String.format("                CachedSize =: %.2f,\r\n", Double.valueOf((processMemInfo.getCachedSize() / 1024.0d) / 1024.0d))).append(flag).append(String.format("                SwapTotalSizeKb =: %.2f,\r\n", Double.valueOf(processMemInfo.getSwapTotalSizeKb() / 1024.0d))).append(flag).append(String.format("                SwapFreeSizeKb =: %.2f,\r\n", Double.valueOf(processMemInfo.getSwapFreeSizeKb() / 1024.0d))).append(flag).append(String.format("                KernelUsedSize =: %.2f,\r\n", Double.valueOf((processMemInfo.getKernelUsedSize() / 1024.0d) / 1024.0d)));
            Log.d("browser", processMemUsage.toString());
        }
    }
}
