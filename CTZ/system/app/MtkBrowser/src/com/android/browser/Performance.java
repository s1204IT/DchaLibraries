package com.android.browser;

import android.app.ActivityManager;
import android.content.Context;
import android.net.ParseException;
import android.net.WebAddress;
import android.os.Debug;
import android.os.SystemProperties;
import android.util.Log;
import com.android.internal.util.MemInfoReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.CopyOnWriteArrayList;

/* loaded from: classes.dex */
public class Performance {
    private static boolean mInTrace;
    private static ActivityManager.MemoryInfo mSysMemThreshold;
    private static final boolean LOGD_ENABLED = Browser.DEBUG;
    private static long mTotalMem = 0;
    private static long mVisibleAppThreshold = 0;
    private static final Object mLock = new Object();
    private static final int[] SYSTEM_CPU_FORMAT = {288, 8224, 8224, 8224, 8224, 8224, 8224, 8224};

    static void tracePageStart(String str) {
        String host;
        if (BrowserSettings.getInstance().isTracing()) {
            try {
                host = new WebAddress(str).getHost();
            } catch (ParseException e) {
                host = "browser";
            }
            String str2 = host.replace('.', '_') + ".trace";
            mInTrace = true;
            Debug.startMethodTracing(str2, 20971520);
        }
    }

    static void tracePageFinished() {
        if (mInTrace) {
            mInTrace = false;
            Debug.stopMethodTracing();
        }
    }

    static String encodeToJSON(Debug.MemoryInfo memoryInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\r\n");
        sb.append("    \"Browser app (MB)\": {\r\n");
        sb.append("        \"Browser\": {\r\n");
        sb.append("            \"Pss\": {\r\n");
        sb.append(String.format("                \"DVM\": %.2f,\r\n", Double.valueOf(memoryInfo.dalvikPss / 1024.0d)));
        sb.append(String.format("                \"Native\": %.2f,\r\n", Double.valueOf(memoryInfo.nativePss / 1024.0d)));
        sb.append(String.format("                \"Other\": %.2f,\r\n", Double.valueOf(memoryInfo.otherPss / 1024.0d)));
        sb.append(String.format("                \"Total\": %.2f\r\n", Double.valueOf(memoryInfo.getTotalPss() / 1024.0d)));
        sb.append("            },\r\n");
        sb.append("            \"Private\": {\r\n");
        sb.append(String.format("                \"DVM\": %.2f,\r\n", Double.valueOf(memoryInfo.dalvikPrivateDirty / 1024.0d)));
        sb.append(String.format("                \"Native\": %.2f,\r\n", Double.valueOf(memoryInfo.nativePrivateDirty / 1024.0d)));
        sb.append(String.format("                \"Other\": %.2f,\r\n", Double.valueOf(memoryInfo.otherPrivateDirty / 1024.0d)));
        sb.append(String.format("                \"Total\": %.2f\r\n", Double.valueOf(memoryInfo.getTotalPrivateDirty() / 1024.0d)));
        sb.append("            },\r\n");
        sb.append("            \"Swapped\": {\r\n");
        sb.append(String.format("                \"DVM\": %.2f,\r\n", Double.valueOf(memoryInfo.dalvikSwappedOut / 1024.0d)));
        sb.append(String.format("                \"Native\": %.2f,\r\n", Double.valueOf(memoryInfo.nativeSwappedOut / 1024.0d)));
        sb.append(String.format("                \"Other\": %.2f,\r\n", Double.valueOf(memoryInfo.otherSwappedOut / 1024.0d)));
        sb.append(String.format("                \"Total\": %.2f\r\n", Double.valueOf(memoryInfo.getTotalSwappedOut() / 1024.0d)));
        sb.append("            },\r\n");
        sb.append("            \"Shared\": {\r\n");
        sb.append(String.format("                \"DVM\": %.2f,\r\n", Double.valueOf(memoryInfo.dalvikSharedDirty / 1024.0d)));
        sb.append(String.format("                \"Native\": %.2f,\r\n", Double.valueOf(memoryInfo.nativeSharedDirty / 1024.0d)));
        sb.append(String.format("                \"Other\": %.2f,\r\n", Double.valueOf(memoryInfo.otherSharedDirty / 1024.0d)));
        sb.append(String.format("                \"Total\": %.2f\r\n", Double.valueOf(memoryInfo.getTotalSharedDirty() / 1024.0d)));
        sb.append("            }\r\n");
        sb.append("        },\r\n");
        int i = 0;
        while (i < 17) {
            sb.append("        \"" + Debug.MemoryInfo.getOtherLabel(i) + "\": {\r\n");
            sb.append("            \"Pss\": {\r\n");
            sb.append(String.format("                \"Total\": %.2f\r\n", Double.valueOf(((double) memoryInfo.getOtherPss(i)) / 1024.0d)));
            sb.append("            },\r\n");
            sb.append("            \"Private\": {\r\n");
            sb.append(String.format("                \"Total\": %.2f\r\n", Double.valueOf(memoryInfo.getOtherPrivateDirty(i) / 1024.0d)));
            sb.append("            },\r\n");
            sb.append("            \"Shared\": {\r\n");
            sb.append(String.format("                \"Total\": %.2f\r\n", Double.valueOf(memoryInfo.getOtherSharedDirty(i) / 1024.0d)));
            sb.append("            }\r\n");
            i++;
            if (i == 17) {
                sb.append("        }\r\n");
                sb.append("    }\r\n");
                sb.append("}\r\n");
            } else {
                sb.append("        },\r\n");
            }
        }
        return sb.toString();
    }

    static String printMemoryInfo(boolean z, String str) {
        Debug.MemoryInfo memoryInfo = new Debug.MemoryInfo();
        Debug.getMemoryInfo(memoryInfo);
        String str2 = "Browser other mem statistics: \r\n";
        for (int i = 0; i < 17; i++) {
            str2 = str2 + " [" + String.valueOf(i) + "] " + Debug.MemoryInfo.getOtherLabel(i) + ", pss=" + String.format("%.2fMB", Double.valueOf(memoryInfo.getOtherPss(i) / 1024.0d)) + ", private=" + String.format("%.2fMB", Double.valueOf(memoryInfo.getOtherPrivateDirty(i) / 1024.0d)) + ", shared=" + String.format("%.2fMB", Double.valueOf(memoryInfo.getOtherSharedDirty(i) / 1024.0d)) + "\r\n";
        }
        if (z) {
            try {
                String str3 = "/storage/emulated/0/memDumpLog" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".txt";
                PrintWriter printWriter = new PrintWriter(str3);
                printWriter.print(encodeToJSON(memoryInfo));
                printWriter.close();
                return str3;
            } catch (IOException e) {
                Log.d("browser", "Failed to save memory logs to file, " + e.getMessage());
                return "";
            }
        }
        Log.d("browser", str + " Browser Memory usage: (Total/DVM/Native/Other) \r\n" + str + String.format(" Pss=%.2f/%.2f/%.2f/%.2f MB\r\n", Double.valueOf(memoryInfo.getTotalPss() / 1024.0d), Double.valueOf(memoryInfo.dalvikPss / 1024.0d), Double.valueOf(memoryInfo.nativePss / 1024.0d), Double.valueOf(memoryInfo.otherPss / 1024.0d)) + str + String.format(" Private=%.2f/%.2f/%.2f/%.2f MB\r\n", Double.valueOf(memoryInfo.getTotalPrivateDirty() / 1024.0d), Double.valueOf(memoryInfo.dalvikPrivateDirty / 1024.0d), Double.valueOf(memoryInfo.nativePrivateDirty / 1024.0d), Double.valueOf(memoryInfo.otherPrivateDirty / 1024.0d)) + str + String.format(" Shared=%.2f/%.2f/%.2f/%.2f MB\r\n", Double.valueOf(memoryInfo.getTotalSharedDirty() / 1024.0d), Double.valueOf(memoryInfo.dalvikSharedDirty / 1024.0d), Double.valueOf(memoryInfo.nativeSharedDirty / 1024.0d), Double.valueOf(memoryInfo.otherSharedDirty / 1024.0d)) + str + String.format(" Swapped=%.2f/%.2f/%.2f/%.2f MB", Double.valueOf(memoryInfo.getTotalSwappedOut() / 1024.0d), Double.valueOf(memoryInfo.dalvikSwappedOut / 1024.0d), Double.valueOf(memoryInfo.nativeSwappedOut / 1024.0d), Double.valueOf(memoryInfo.otherSwappedOut / 1024.0d)));
        Log.d("browser", str2);
        return "";
    }

    static void dumpSystemMemInfo(Context context) {
        if (context != null && mSysMemThreshold == null) {
            mSysMemThreshold = new ActivityManager.MemoryInfo();
            ((ActivityManager) context.getSystemService("activity")).getMemoryInfo(mSysMemThreshold);
            mTotalMem = mSysMemThreshold.totalMem;
            mVisibleAppThreshold = mSysMemThreshold.visibleAppThreshold;
            if (LOGD_ENABLED) {
                String str = "MemoryDumpInfo" + System.currentTimeMillis();
                Log.d("browser", "Browser Current Memory Dump time = " + str);
                printSysMemInfo(mSysMemThreshold, str);
            }
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:38:0x016a  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    static boolean checkShouldReleaseTabs(int i, ArrayList<Integer> arrayList, boolean z, String str, CopyOnWriteArrayList<Integer> copyOnWriteArrayList, boolean z2) {
        boolean z3;
        synchronized (mLock) {
            String str2 = "MemoryDumpInfo" + System.currentTimeMillis();
            Log.d("browser", "Browser Current Memory Dump time = " + str2);
            if (LOGD_ENABLED) {
                if (z) {
                    if (!z2) {
                        Log.d("browser", str2 + " Performance#checkShouldReleaseTabs()-->tabPosition = " + arrayList + ", url = " + str);
                    }
                } else if (z2) {
                    Log.d("browser", str2 + " Perfromance#checkShouldReleaseTabs()--->removeTabIndex = " + arrayList);
                } else {
                    Log.d("browser", str2 + " Performance#checkShouldReleaseTabs()-->freeTabIndex = " + copyOnWriteArrayList);
                }
            }
            MemInfoReader memInfoReader = new MemInfoReader();
            memInfoReader.readMemInfo();
            if (LOGD_ENABLED) {
                printProcessMemInfo(memInfoReader, str2);
                printMemoryInfo(false, str2);
            }
            Debug.getMemoryInfo(new Debug.MemoryInfo());
            double totalPss = ((r2.getTotalPss() + r2.getSummaryTotalSwap()) * 1024.0d) / mTotalMem;
            if (LOGD_ENABLED) {
                NumberFormat numberFormat = NumberFormat.getInstance();
                numberFormat.setMaximumFractionDigits(3);
                Log.d("browser", str2 + " current porcess take up the memory percent is " + numberFormat.format(totalPss));
            }
            z3 = true;
            if (Math.max(memInfoReader.getFreeSize(), memInfoReader.getCachedSize()) < mVisibleAppThreshold) {
                if (LOGD_ENABLED) {
                    Log.d("browser", "Browser Pss =: " + (r2.getTotalPss() / 1024.0d) + " PSwap =: " + (r2.getTotalSwappedOut() / 1024.0f) + " SwappablePss =: " + (r2.getTotalSwappablePss() / 1024.0f));
                }
                String str3 = SystemProperties.get("ro.vendor.gmo.ram_optimize");
                if (str3 == null || !str3.equals("1")) {
                    if (totalPss <= 0.4000000059604645d || i <= 5 || !z) {
                    }
                } else if (totalPss <= 0.30000001192092896d || i <= 3 || !z) {
                }
            } else {
                z3 = false;
            }
        }
        return z3;
    }

    static void printSysMemInfo(ActivityManager.MemoryInfo memoryInfo, String str) {
        if (memoryInfo != null) {
            Log.d("browser", "{\r\n" + str + "    \"System Memory Usage (MB)\": {\r\n" + str + String.format("                total=: %.2f,\r\n", Double.valueOf((memoryInfo.totalMem / 1024.0d) / 1024.0d)) + str + String.format("                threshold=: %.2f,\r\n", Double.valueOf((memoryInfo.threshold / 1024.0d) / 1024.0d)) + str + String.format("                availMem=: %.2f,\r\n", Double.valueOf((memoryInfo.availMem / 1024.0d) / 1024.0d)) + str + String.format("                hiddenAppThreshold=: %.2f,\r\n", Double.valueOf((memoryInfo.hiddenAppThreshold / 1024.0d) / 1024.0d)) + str + String.format("                secondaryServerThreshold=: %.2f,\r\n", Double.valueOf((memoryInfo.secondaryServerThreshold / 1024.0d) / 1024.0d)) + str + String.format("                visibleAppThreshold=: %.2f,\r\n", Double.valueOf((memoryInfo.visibleAppThreshold / 1024.0d) / 1024.0d)) + str + String.format("                foregroundAppThreshold=: %.2f,\r\n", Double.valueOf((memoryInfo.foregroundAppThreshold / 1024.0d) / 1024.0d)));
        }
    }

    static void printProcessMemInfo(MemInfoReader memInfoReader, String str) {
        if (memInfoReader != null) {
            Log.d("browser", str + "{\r\n" + str + "    \"Process Memory Usage (MB)\": {\r\n" + str + String.format("                TotalSize =: %.2f,\r\n", Double.valueOf((memInfoReader.getTotalSize() / 1024.0d) / 1024.0d)) + str + String.format("                FreeSize =: %.2f,\r\n", Double.valueOf((memInfoReader.getFreeSize() / 1024.0d) / 1024.0d)) + str + String.format("                CachedSize =: %.2f,\r\n", Double.valueOf((memInfoReader.getCachedSize() / 1024.0d) / 1024.0d)) + str + String.format("                SwapTotalSizeKb =: %.2f,\r\n", Double.valueOf(memInfoReader.getSwapTotalSizeKb() / 1024.0d)) + str + String.format("                SwapFreeSizeKb =: %.2f,\r\n", Double.valueOf(memInfoReader.getSwapFreeSizeKb() / 1024.0d)) + str + String.format("                KernelUsedSize =: %.2f,\r\n", Double.valueOf((memInfoReader.getKernelUsedSize() / 1024.0d) / 1024.0d)));
        }
    }
}
