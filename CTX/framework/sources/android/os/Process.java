package android.os;

import android.os.StrictMode;
import android.system.Os;
import android.system.OsConstants;
import android.webkit.WebViewZygote;
import dalvik.system.VMRuntime;

public class Process {
    public static final int AUDIOSERVER_UID = 1041;
    public static final int BLUETOOTH_UID = 1002;
    public static final int CAMERASERVER_UID = 1047;
    public static final int DRM_UID = 1019;
    public static final int FIRST_APPLICATION_CACHE_GID = 20000;
    public static final int FIRST_APPLICATION_UID = 10000;
    public static final int FIRST_ISOLATED_UID = 99000;
    public static final int FIRST_SHARED_APPLICATION_GID = 50000;
    public static final int INCIDENTD_UID = 1067;
    public static final int KEYSTORE_UID = 1017;
    public static final int LAST_APPLICATION_CACHE_GID = 29999;
    public static final int LAST_APPLICATION_UID = 19999;
    public static final int LAST_ISOLATED_UID = 99999;
    public static final int LAST_SHARED_APPLICATION_GID = 59999;
    private static final String LOG_TAG = "Process";
    public static final int LOG_UID = 1007;
    public static final int MEDIA_RW_GID = 1023;
    public static final int MEDIA_UID = 1013;
    public static final int NFC_UID = 1027;
    public static final int NOBODY_UID = 9999;
    public static final int OTA_UPDATE_UID = 1061;
    public static final int PACKAGE_INFO_GID = 1032;
    public static final int PHONE_UID = 1001;
    public static final int PROC_CHAR = 2048;
    public static final int PROC_COMBINE = 256;
    public static final int PROC_OUT_FLOAT = 16384;
    public static final int PROC_OUT_LONG = 8192;
    public static final int PROC_OUT_STRING = 4096;
    public static final int PROC_PARENS = 512;
    public static final int PROC_QUOTES = 1024;
    public static final int PROC_SPACE_TERM = 32;
    public static final int PROC_TAB_TERM = 9;
    public static final int PROC_TERM_MASK = 255;
    public static final int PROC_ZERO_TERM = 0;
    public static final int ROOT_UID = 0;
    public static final int SCHED_BATCH = 3;
    public static final int SCHED_FIFO = 1;
    public static final int SCHED_IDLE = 5;
    public static final int SCHED_OTHER = 0;
    public static final int SCHED_RESET_ON_FORK = 1073741824;
    public static final int SCHED_RR = 2;
    public static final int SE_UID = 1068;
    public static final int SHARED_RELRO_UID = 1037;
    public static final int SHARED_USER_GID = 9997;
    public static final int SHELL_UID = 2000;
    public static final int SIGNAL_KILL = 9;
    public static final int SIGNAL_QUIT = 3;
    public static final int SIGNAL_USR1 = 10;
    public static final int SYSTEM_UID = 1000;
    public static final int THREAD_GROUP_AUDIO_APP = 3;
    public static final int THREAD_GROUP_AUDIO_SYS = 4;
    public static final int THREAD_GROUP_BG_NONINTERACTIVE = 0;
    public static final int THREAD_GROUP_DEFAULT = -1;
    private static final int THREAD_GROUP_FOREGROUND = 1;
    public static final int THREAD_GROUP_RESTRICTED = 7;
    public static final int THREAD_GROUP_RT_APP = 6;
    public static final int THREAD_GROUP_SYSTEM = 2;
    public static final int THREAD_GROUP_TOP_APP = 5;
    public static final int THREAD_PRIORITY_AUDIO = -16;
    public static final int THREAD_PRIORITY_BACKGROUND = 10;
    public static final int THREAD_PRIORITY_DEFAULT = 0;
    public static final int THREAD_PRIORITY_DISPLAY = -4;
    public static final int THREAD_PRIORITY_FOREGROUND = -2;
    public static final int THREAD_PRIORITY_LESS_FAVORABLE = 1;
    public static final int THREAD_PRIORITY_LOWEST = 19;
    public static final int THREAD_PRIORITY_MORE_FAVORABLE = -1;
    public static final int THREAD_PRIORITY_URGENT_AUDIO = -19;
    public static final int THREAD_PRIORITY_URGENT_DISPLAY = -8;
    public static final int THREAD_PRIORITY_VIDEO = -10;
    public static final int VPN_UID = 1016;
    public static final int WEBVIEW_ZYGOTE_UID = 1053;
    public static final int WIFI_UID = 1010;
    private static long sStartElapsedRealtime;
    private static long sStartUptimeMillis;
    public static final String ZYGOTE_SOCKET = "zygote";
    public static final String SECONDARY_ZYGOTE_SOCKET = "zygote_secondary";
    public static final ZygoteProcess zygoteProcess = new ZygoteProcess(ZYGOTE_SOCKET, SECONDARY_ZYGOTE_SOCKET);

    public static final class ProcessStartResult {
        public int pid;
        public boolean usingWrapper;
    }

    public static final native long getElapsedCpuTime();

    public static final native int[] getExclusiveCores();

    public static final native long getFreeMemory();

    public static final native int getGidForName(String str);

    public static final native long getLruAnonMemory();

    public static final native int[] getPids(String str, int[] iArr);

    public static final native int[] getPidsForCommands(String[] strArr);

    public static final native int getProcessGroup(int i) throws SecurityException, IllegalArgumentException;

    public static final native long getPss(int i);

    public static final native int getThreadPriority(int i) throws IllegalArgumentException;

    public static final native int getThreadScheduler(int i) throws IllegalArgumentException;

    public static final native long getTotalMemory();

    public static final native int getUidForName(String str);

    public static final native int killProcessGroup(int i, int i2);

    public static final native boolean parseProcLine(byte[] bArr, int i, int i2, int[] iArr, String[] strArr, long[] jArr, float[] fArr);

    public static final native boolean readProcFile(String str, int[] iArr, String[] strArr, long[] jArr, float[] fArr);

    public static final native void readProcLines(String str, String[] strArr, long[] jArr);

    public static final native void removeAllProcessGroups();

    public static final native void sendSignal(int i, int i2);

    public static final native void sendSignalQuiet(int i, int i2);

    public static final native void setArgV0(String str);

    public static final native void setCanSelfBackground(boolean z);

    public static final native int setGid(int i);

    public static final native void setProcessGroup(int i, int i2) throws SecurityException, IllegalArgumentException;

    public static final native boolean setSwappiness(int i, boolean z);

    public static final native void setThreadGroup(int i, int i2) throws SecurityException, IllegalArgumentException;

    public static final native void setThreadGroupAndCpuset(int i, int i2) throws SecurityException, IllegalArgumentException;

    public static final native void setThreadPriority(int i) throws SecurityException, IllegalArgumentException;

    public static final native void setThreadPriority(int i, int i2) throws SecurityException, IllegalArgumentException;

    public static final native void setThreadScheduler(int i, int i2, int i3) throws IllegalArgumentException;

    public static final native int setUid(int i);

    public static final ProcessStartResult start(String str, String str2, int i, int i2, int[] iArr, int i3, int i4, int i5, String str3, String str4, String str5, String str6, String str7, String[] strArr) {
        return zygoteProcess.start(str, str2, i, i2, iArr, i3, i4, i5, str3, str4, str5, str6, str7, strArr);
    }

    public static final ProcessStartResult startWebView(String str, String str2, int i, int i2, int[] iArr, int i3, int i4, int i5, String str3, String str4, String str5, String str6, String str7, String[] strArr) {
        return WebViewZygote.getProcess().start(str, str2, i, i2, iArr, i3, i4, i5, str3, str4, str5, str6, str7, strArr);
    }

    public static final long getStartElapsedRealtime() {
        return sStartElapsedRealtime;
    }

    public static final long getStartUptimeMillis() {
        return sStartUptimeMillis;
    }

    public static final void setStartTimes(long j, long j2) {
        sStartElapsedRealtime = j;
        sStartUptimeMillis = j2;
    }

    public static final boolean is64Bit() {
        return VMRuntime.getRuntime().is64Bit();
    }

    public static final int myPid() {
        return Os.getpid();
    }

    public static final int myPpid() {
        return Os.getppid();
    }

    public static final int myTid() {
        return Os.gettid();
    }

    public static final int myUid() {
        return Os.getuid();
    }

    public static UserHandle myUserHandle() {
        return UserHandle.of(UserHandle.getUserId(myUid()));
    }

    public static boolean isCoreUid(int i) {
        return UserHandle.isCore(i);
    }

    public static boolean isApplicationUid(int i) {
        return UserHandle.isApp(i);
    }

    public static final boolean isIsolated() {
        return isIsolated(myUid());
    }

    public static final boolean isIsolated(int i) {
        int appId = UserHandle.getAppId(i);
        return appId >= 99000 && appId <= 99999;
    }

    public static final int getUidForPid(int i) {
        long[] jArr = {-1};
        readProcLines("/proc/" + i + "/status", new String[]{"Uid:"}, jArr);
        return (int) jArr[0];
    }

    public static final int getParentPid(int i) {
        long[] jArr = {-1};
        readProcLines("/proc/" + i + "/status", new String[]{"PPid:"}, jArr);
        return (int) jArr[0];
    }

    public static final int getThreadGroupLeader(int i) {
        long[] jArr = {-1};
        readProcLines("/proc/" + i + "/status", new String[]{"Tgid:"}, jArr);
        return (int) jArr[0];
    }

    @Deprecated
    public static final boolean supportsProcesses() {
        return true;
    }

    public static final void killProcess(int i) {
        sendSignal(i, 9);
    }

    public static final void killProcessQuiet(int i) {
        sendSignalQuiet(i, 9);
    }

    public static final boolean isThreadInProcess(int i, int i2) {
        StrictMode.ThreadPolicy threadPolicyAllowThreadDiskReads = StrictMode.allowThreadDiskReads();
        try {
            if (Os.access("/proc/" + i + "/task/" + i2, OsConstants.F_OK)) {
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        } finally {
            StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskReads);
        }
    }
}
