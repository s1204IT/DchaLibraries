package com.mediatek.aee;

import android.content.Intent;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.SystemProperties;
import android.provider.Telephony;
import android.telecom.TelecomManager;
import android.util.Log;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExceptionLog {
    public static final byte AEE_EXCEPTION_JNI = 1;
    public static final byte AEE_WARNING_JNI = 0;
    public static final String TAG = "AES";
    private final String SEND_NON_PROTECTED_BROADCAST = "Sending non-protected broadcast";
    private final String[] protectedBroadcastFilter = {"com.android.systemui.action.FINISH_WIZARD", "android.intent.action.MASTER_CLEAR ", Telephony.Sms.Intents.SMS_REJECTED_ACTION, "android.btopp.intent.action.ACCEPT", "com.android.bluetooth.BluetoothMapContentObserver.action.MESSAGE_SENT", Intent.ACTION_UMS_DISCONNECTED, Intent.ACTION_CALL_EMERGENCY, "com.android.stk.DIALOG_ALARM_TIMEOUT", "com.android.server.action.LOCKDOWN_RESET", TelecomManager.ACTION_TTY_PREFERRED_MODE_CHANGED, "com.mediatek.mtklogger.ADB_CMD", "com.mediatek.log2server.EXCEPTION_HAPPEND", "com.mediatek.autounlock", "com.mtk.autotest.heartset.stop", "com.mtk.fts.ACTION", "com.android.systemui.demo", "permission check ok", "permission check error", "network error"};
    private final String FILE_OBSERVER_NULL_PATH = "Unhandled exception in FileObserver com.android.server.BootReceiver";

    private static native long SFMatter(long j, long j2);

    private static native void WDTMatter(long j);

    private static native boolean getNativeExceptionPidListImpl(int[] iArr);

    private static native void report(String str, String str2, String str3, String str4, String str5, long j);

    private static native void switchFtraceImpl(int i);

    private static native void systemreportImpl(byte b, String str, String str2, String str3, String str4);

    static {
        Log.i("AES", "load Exception Log jni");
        System.loadLibrary("mediatek_exceptionlog");
    }

    public void handle(String str, String str2, String str3) {
        Log.w("AES", "Exception Log handling...");
        if (str.startsWith("data_app") && !str2.contains("com.android.development") && SystemProperties.getInt("persist.vendor.mtk.aee.filter", 1) == 1) {
            Log.w("AES", "Skipped - do not care third party apk");
            return;
        }
        String strGroup = "";
        String str4 = "";
        long j = 0;
        String[] strArrSplit = str2.split("\n+");
        Pattern patternCompile = Pattern.compile("^Process:\\s+(.*)");
        Pattern patternCompile2 = Pattern.compile("^Package:\\s+(.*)");
        for (String str5 : strArrSplit) {
            Matcher matcher = patternCompile.matcher(str5);
            if (matcher.matches()) {
                strGroup = matcher.group(1);
            }
            Matcher matcher2 = patternCompile2.matcher(str5);
            if (matcher2.matches()) {
                str4 = str4 + matcher2.group(1) + "\n";
            }
        }
        if (!str3.equals("")) {
            j = Long.parseLong(str3);
        }
        if (!str.equals("system_server_wtf") || !isSkipSystemWtfReport(str2)) {
            report(strGroup, str4, str2, "Backtrace of all threads:\n\n", str, j);
        }
    }

    public void systemreport(byte b, String str, String str2, String str3) {
        systemreportImpl(b, str, getThreadStackTrace(), str2, str3);
    }

    public boolean getNativeExceptionPidList(int[] iArr) {
        return getNativeExceptionPidListImpl(iArr);
    }

    public void switchFtrace(int i) {
        switchFtraceImpl(i);
    }

    private static String getThreadStackTrace() {
        StringWriter stringWriter = new StringWriter();
        try {
            Thread threadCurrentThread = Thread.currentThread();
            StackTraceElement[] stackTrace = threadCurrentThread.getStackTrace();
            StringBuilder sb = new StringBuilder();
            sb.append("\"");
            sb.append(threadCurrentThread.getName());
            sb.append("\" ");
            sb.append(threadCurrentThread.isDaemon() ? "daemon" : "");
            sb.append(" prio=");
            sb.append(threadCurrentThread.getPriority());
            sb.append(" Thread id=");
            sb.append(threadCurrentThread.getId());
            sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
            sb.append(threadCurrentThread.getState());
            sb.append("\n");
            stringWriter.write(sb.toString());
            for (StackTraceElement stackTraceElement : stackTrace) {
                stringWriter.write("\t" + stackTraceElement + "\n");
            }
            stringWriter.write("\n");
            return stringWriter.toString();
        } catch (IOException e) {
            return "IOException";
        } catch (OutOfMemoryError e2) {
            return "java.lang.OutOfMemoryError";
        }
    }

    private static String getAllThreadStackTraces() {
        Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
        StringWriter stringWriter = new StringWriter();
        try {
            for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
                StackTraceElement[] value = entry.getValue();
                Thread key = entry.getKey();
                StringBuilder sb = new StringBuilder();
                sb.append("\"");
                sb.append(key.getName());
                sb.append("\" ");
                sb.append(key.isDaemon() ? "daemon" : "");
                sb.append(" prio=");
                sb.append(key.getPriority());
                sb.append(" Thread id=");
                sb.append(key.getId());
                sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                sb.append(key.getState());
                sb.append("\n");
                stringWriter.write(sb.toString());
                for (StackTraceElement stackTraceElement : value) {
                    stringWriter.write("\t" + stackTraceElement + "\n");
                }
                stringWriter.write("\n");
            }
            return stringWriter.toString();
        } catch (IOException e) {
            return "IOException";
        } catch (OutOfMemoryError e2) {
            return "java.lang.OutOfMemoryError";
        }
    }

    public void WDTMatterJava(long j) {
        WDTMatter(j);
    }

    public long SFMatterJava(long j, long j2) {
        return SFMatter(j, j2);
    }

    private boolean isSkipSystemWtfReport(String str) {
        return isSkipReportFromProtectedBroadcast(str) || isSkipReportFromNullFilePath(str);
    }

    private boolean isSkipReportFromProtectedBroadcast(String str) {
        if (str.contains("Sending non-protected broadcast")) {
            for (int i = 0; i < this.protectedBroadcastFilter.length; i++) {
                if (str.contains(this.protectedBroadcastFilter[i])) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isSkipReportFromNullFilePath(String str) {
        if (str.contains("Unhandled exception in FileObserver com.android.server.BootReceiver")) {
            return true;
        }
        return false;
    }
}
