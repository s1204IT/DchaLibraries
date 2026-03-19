package com.android.server.am;

import android.R;
import android.content.res.Resources;
import android.graphics.Point;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Build;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Slog;
import com.android.internal.util.MemInfoReader;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.job.controllers.JobStatus;
import com.android.server.usage.AppStandbyController;
import com.android.server.wm.WindowManagerService;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;

public final class ProcessList {
    static final int BACKUP_APP_ADJ = 300;
    static final int CACHED_APP_MAX_ADJ = 906;
    static final int CACHED_APP_MIN_ADJ = 900;
    public static final int FOREGROUND_APP_ADJ = 0;
    static final int HEAVY_WEIGHT_APP_ADJ = 400;
    static final int HOME_APP_ADJ = 600;
    static final int INVALID_ADJ = -10000;
    static final byte LMK_PROCPRIO = 1;
    static final byte LMK_PROCREMOVE = 2;
    static final byte LMK_TARGET = 0;
    static final long MAX_EMPTY_TIME = 1800000;
    static final int MIN_CACHED_APPS = 2;
    static final int MIN_CRASH_INTERVAL = 60000;
    static final int NATIVE_ADJ = -1000;
    static final int PAGE_SIZE = 4096;
    public static final int PERCEPTIBLE_APP_ADJ = 200;
    static final int PERSISTENT_PROC_ADJ = -800;
    static final int PERSISTENT_SERVICE_ADJ = -700;
    static final int PREVIOUS_APP_ADJ = 700;
    public static final int PROC_MEM_CACHED = 4;
    public static final int PROC_MEM_IMPORTANT = 2;
    public static final int PROC_MEM_NUM = 5;
    public static final int PROC_MEM_PERSISTENT = 0;
    public static final int PROC_MEM_SERVICE = 3;
    public static final int PROC_MEM_TOP = 1;
    public static final int PSS_ALL_INTERVAL = 1200000;
    private static final int PSS_FIRST_ASLEEP_BACKGROUND_INTERVAL = 30000;
    private static final int PSS_FIRST_ASLEEP_CACHED_INTERVAL = 60000;
    private static final int PSS_FIRST_ASLEEP_PERSISTENT_INTERVAL = 60000;
    private static final int PSS_FIRST_ASLEEP_TOP_INTERVAL = 20000;
    private static final int PSS_FIRST_BACKGROUND_INTERVAL = 20000;
    private static final int PSS_FIRST_CACHED_INTERVAL = 20000;
    private static final int PSS_FIRST_PERSISTENT_INTERVAL = 30000;
    private static final int PSS_FIRST_TOP_INTERVAL = 10000;
    public static final int PSS_MAX_INTERVAL = 3600000;
    public static final int PSS_MIN_TIME_FROM_STATE_CHANGE = 15000;
    public static final int PSS_SAFE_TIME_FROM_STATE_CHANGE = 1000;
    private static final int PSS_SAME_CACHED_INTERVAL = 600000;
    private static final int PSS_SAME_IMPORTANT_INTERVAL = 600000;
    private static final int PSS_SAME_PERSISTENT_INTERVAL = 600000;
    private static final int PSS_SAME_SERVICE_INTERVAL = 300000;
    private static final int PSS_SAME_TOP_INTERVAL = 60000;
    private static final int PSS_TEST_FIRST_BACKGROUND_INTERVAL = 5000;
    private static final int PSS_TEST_FIRST_TOP_INTERVAL = 3000;
    public static final int PSS_TEST_MIN_TIME_FROM_STATE_CHANGE = 10000;
    private static final int PSS_TEST_SAME_BACKGROUND_INTERVAL = 15000;
    private static final int PSS_TEST_SAME_IMPORTANT_INTERVAL = 10000;
    static final int SCHED_GROUP_BACKGROUND = 0;
    static final int SCHED_GROUP_DEFAULT = 2;
    static final int SCHED_GROUP_RESTRICTED = 1;
    static final int SCHED_GROUP_TOP_APP = 3;
    static final int SCHED_GROUP_TOP_APP_BOUND = 4;
    static final int SERVICE_ADJ = 500;
    static final int SERVICE_B_ADJ = 800;
    static final int SYSTEM_ADJ = -900;
    private static final String TAG = "ActivityManager";
    static final int TRIM_CRITICAL_THRESHOLD = 3;
    static final int TRIM_LOW_THRESHOLD = 5;
    static final int UNKNOWN_ADJ = 1001;
    static final int VISIBLE_APP_ADJ = 100;
    static final int VISIBLE_APP_LAYER_MAX = 99;
    private static OutputStream sLmkdOutputStream;
    private static LocalSocket sLmkdSocket;
    private long mCachedRestoreLevel;
    private boolean mHaveDisplaySize;
    private final long mTotalMemMb;
    private static final int[] sProcStateToProcMem = {0, 0, 1, 2, 2, 2, 2, 2, 2, 3, 4, 1, 2, 4, 4, 4, 4, 4, 4};
    private static final long[] sFirstAwakePssTimes = {30000, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY, 20000, 20000, 20000};
    private static final long[] sSameAwakePssTimes = {600000, 60000, 600000, BackupAgentTimeoutParameters.DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS, 600000};
    private static final long[] sFirstAsleepPssTimes = {60000, 20000, 30000, 30000, 60000};
    private static final long[] sSameAsleepPssTimes = {600000, 60000, 600000, BackupAgentTimeoutParameters.DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS, 600000};
    private static final long[] sTestFirstPssTimes = {3000, 3000, 5000, 5000, 5000};
    private static final long[] sTestSamePssTimes = {15000, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY, 15000, 15000};
    private final int[] mOomAdj = {0, 100, 200, 300, CACHED_APP_MIN_ADJ, CACHED_APP_MAX_ADJ};
    private final int[] mOomMinFreeLow = {12288, 18432, 24576, 36864, 43008, 49152};
    private final int[] mOomMinFreeHigh = {73728, 92160, 110592, 129024, 147456, 184320};
    private final int[] mOomMinFree = new int[this.mOomAdj.length];

    ProcessList() {
        MemInfoReader memInfoReader = new MemInfoReader();
        memInfoReader.readMemInfo();
        this.mTotalMemMb = memInfoReader.getTotalSize() / 1048576;
        updateOomLevels(0, 0, false);
    }

    void applyDisplaySize(WindowManagerService windowManagerService) {
        if (!this.mHaveDisplaySize) {
            Point point = new Point();
            windowManagerService.getBaseDisplaySize(0, point);
            if (point.x != 0 && point.y != 0) {
                updateOomLevels(point.x, point.y, true);
                this.mHaveDisplaySize = true;
            }
        }
    }

    private void updateOomLevels(int i, int i2, boolean z) {
        float f = (this.mTotalMemMb - 350) / 350.0f;
        int i3 = i * i2;
        float f2 = (i3 - 384000) / 640000;
        if (f > f2) {
            f2 = f;
        }
        if (f2 < 0.0f) {
            f2 = 0.0f;
        } else if (f2 > 1.0f) {
            f2 = 1.0f;
        }
        int integer = Resources.getSystem().getInteger(R.integer.config_defaultHapticFeedbackIntensity);
        int integer2 = Resources.getSystem().getInteger(R.integer.config_defaultDisplayDefaultColorMode);
        boolean z2 = Build.SUPPORTED_64_BIT_ABIS.length > 0;
        for (int i4 = 0; i4 < this.mOomAdj.length; i4++) {
            int i5 = this.mOomMinFreeLow[i4];
            int i6 = this.mOomMinFreeHigh[i4];
            if (z2) {
                if (i4 == 4) {
                    i6 = (i6 * 3) / 2;
                } else if (i4 == 5) {
                    i6 = (i6 * 7) / 4;
                }
            }
            this.mOomMinFree[i4] = (int) (i5 + ((i6 - i5) * f2));
        }
        if (integer2 >= 0) {
            for (int i7 = 0; i7 < this.mOomAdj.length; i7++) {
                this.mOomMinFree[i7] = (int) ((integer2 * this.mOomMinFree[i7]) / this.mOomMinFree[this.mOomAdj.length - 1]);
            }
        }
        if (integer != 0) {
            for (int i8 = 0; i8 < this.mOomAdj.length; i8++) {
                int[] iArr = this.mOomMinFree;
                iArr[i8] = iArr[i8] + ((int) ((integer * this.mOomMinFree[i8]) / this.mOomMinFree[this.mOomAdj.length - 1]));
                if (this.mOomMinFree[i8] < 0) {
                    this.mOomMinFree[i8] = 0;
                }
            }
        }
        this.mCachedRestoreLevel = (getMemLevel(CACHED_APP_MAX_ADJ) / 1024) / 3;
        int i9 = ((i3 * 4) * 3) / 1024;
        int integer3 = Resources.getSystem().getInteger(R.integer.config_carDockRotation);
        int integer4 = Resources.getSystem().getInteger(R.integer.config_carDockKeepsScreenOn);
        if (integer4 >= 0) {
            i9 = integer4;
        }
        if (integer3 != 0 && (i9 = i9 + integer3) < 0) {
            i9 = 0;
        }
        if (z) {
            ByteBuffer byteBufferAllocate = ByteBuffer.allocate(4 * ((2 * this.mOomAdj.length) + 1));
            byteBufferAllocate.putInt(0);
            for (int i10 = 0; i10 < this.mOomAdj.length; i10++) {
                byteBufferAllocate.putInt((this.mOomMinFree[i10] * 1024) / 4096);
                byteBufferAllocate.putInt(this.mOomAdj[i10]);
            }
            writeLmkd(byteBufferAllocate);
            SystemProperties.set("sys.sysctl.extra_free_kbytes", Integer.toString(i9));
        }
    }

    public static int computeEmptyProcessLimit(int i) {
        return i / 2;
    }

    private static String buildOomTag(String str, String str2, int i, int i2) {
        if (i == i2) {
            if (str2 == null) {
                return str;
            }
            return str + "  ";
        }
        return str + "+" + Integer.toString(i - i2);
    }

    public static String makeOomAdjString(int i) {
        if (i >= CACHED_APP_MIN_ADJ) {
            return buildOomTag("cch", "  ", i, CACHED_APP_MIN_ADJ);
        }
        if (i >= SERVICE_B_ADJ) {
            return buildOomTag("svcb ", null, i, SERVICE_B_ADJ);
        }
        if (i >= PREVIOUS_APP_ADJ) {
            return buildOomTag("prev ", null, i, PREVIOUS_APP_ADJ);
        }
        if (i >= 600) {
            return buildOomTag("home ", null, i, 600);
        }
        if (i >= 500) {
            return buildOomTag("svc  ", null, i, 500);
        }
        if (i >= HEAVY_WEIGHT_APP_ADJ) {
            return buildOomTag("hvy  ", null, i, HEAVY_WEIGHT_APP_ADJ);
        }
        if (i >= 300) {
            return buildOomTag("bkup ", null, i, 300);
        }
        if (i >= 200) {
            return buildOomTag("prcp ", null, i, 200);
        }
        if (i >= 100) {
            return buildOomTag("vis  ", null, i, 100);
        }
        if (i >= 0) {
            return buildOomTag("fore ", null, i, 0);
        }
        if (i >= PERSISTENT_SERVICE_ADJ) {
            return buildOomTag("psvc ", null, i, PERSISTENT_SERVICE_ADJ);
        }
        if (i >= PERSISTENT_PROC_ADJ) {
            return buildOomTag("pers ", null, i, PERSISTENT_PROC_ADJ);
        }
        if (i >= SYSTEM_ADJ) {
            return buildOomTag("sys  ", null, i, SYSTEM_ADJ);
        }
        if (i >= -1000) {
            return buildOomTag("ntv  ", null, i, -1000);
        }
        return Integer.toString(i);
    }

    public static String makeProcStateString(int i) {
        switch (i) {
            case 0:
                return "PER ";
            case 1:
                return "PERU";
            case 2:
                return "TOP ";
            case 3:
                return "FGS ";
            case 4:
                return "BFGS";
            case 5:
                return "IMPF";
            case 6:
                return "IMPB";
            case 7:
                return "TRNB";
            case 8:
                return "BKUP";
            case 9:
                return "SVC ";
            case 10:
                return "RCVR";
            case 11:
                return "TPSL";
            case 12:
                return "HVY ";
            case 13:
                return "HOME";
            case 14:
                return "LAST";
            case 15:
                return "CAC ";
            case 16:
                return "CACC";
            case 17:
                return "CRE ";
            case 18:
                return "CEM ";
            case WindowManagerService.H.REPORT_WINDOWS_CHANGE:
                return "NONE";
            default:
                return "??";
        }
    }

    public static int makeProcStateProtoEnum(int i) {
        switch (i) {
            case -1:
                return 999;
            case 0:
                return 1000;
            case 1:
                return 1001;
            case 2:
                return 1002;
            case 3:
                return 1003;
            case 4:
                return 1004;
            case 5:
                return 1005;
            case 6:
                return 1006;
            case 7:
                return 1007;
            case 8:
                return 1008;
            case 9:
                return 1009;
            case 10:
                return 1010;
            case 11:
                return 1011;
            case 12:
                return 1012;
            case 13:
                return 1013;
            case 14:
                return 1014;
            case 15:
                return 1015;
            case 16:
                return 1016;
            case 17:
                return 1017;
            case 18:
                return 1018;
            case WindowManagerService.H.REPORT_WINDOWS_CHANGE:
                return 1019;
            default:
                return 998;
        }
    }

    public static void appendRamKb(StringBuilder sb, long j) {
        int i = 10;
        int i2 = 0;
        while (i2 < 6) {
            if (j < i) {
                sb.append(' ');
            }
            i2++;
            i *= 10;
        }
        sb.append(j);
    }

    public static final class ProcStateMemTracker {
        int mPendingHighestMemState;
        int mPendingMemState;
        float mPendingScalingFactor;
        final int[] mHighestMem = new int[5];
        final float[] mScalingFactor = new float[5];
        int mTotalHighestMem = 4;

        public ProcStateMemTracker() {
            for (int i = 0; i < 5; i++) {
                this.mHighestMem[i] = 5;
                this.mScalingFactor[i] = 1.0f;
            }
            this.mPendingMemState = -1;
        }

        public void dumpLine(PrintWriter printWriter) {
            printWriter.print("best=");
            printWriter.print(this.mTotalHighestMem);
            printWriter.print(" (");
            boolean z = false;
            for (int i = 0; i < 5; i++) {
                if (this.mHighestMem[i] < 5) {
                    if (z) {
                        printWriter.print(", ");
                    }
                    printWriter.print(i);
                    printWriter.print("=");
                    printWriter.print(this.mHighestMem[i]);
                    printWriter.print(" ");
                    printWriter.print(this.mScalingFactor[i]);
                    printWriter.print("x");
                    z = true;
                }
            }
            printWriter.print(")");
            if (this.mPendingMemState >= 0) {
                printWriter.print(" / pending state=");
                printWriter.print(this.mPendingMemState);
                printWriter.print(" highest=");
                printWriter.print(this.mPendingHighestMemState);
                printWriter.print(" ");
                printWriter.print(this.mPendingScalingFactor);
                printWriter.print("x");
            }
            printWriter.println();
        }
    }

    public static boolean procStatesDifferForMem(int i, int i2) {
        return sProcStateToProcMem[i] != sProcStateToProcMem[i2];
    }

    public static long minTimeFromStateChange(boolean z) {
        if (z) {
            return JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY;
        }
        return 15000L;
    }

    public static void commitNextPssTime(ProcStateMemTracker procStateMemTracker) {
        if (procStateMemTracker.mPendingMemState >= 0) {
            procStateMemTracker.mHighestMem[procStateMemTracker.mPendingMemState] = procStateMemTracker.mPendingHighestMemState;
            procStateMemTracker.mScalingFactor[procStateMemTracker.mPendingMemState] = procStateMemTracker.mPendingScalingFactor;
            procStateMemTracker.mTotalHighestMem = procStateMemTracker.mPendingHighestMemState;
            procStateMemTracker.mPendingMemState = -1;
        }
    }

    public static void abortNextPssTime(ProcStateMemTracker procStateMemTracker) {
        procStateMemTracker.mPendingMemState = -1;
    }

    public static long computeNextPssTime(int i, ProcStateMemTracker procStateMemTracker, boolean z, boolean z2, long j) {
        long[] jArr;
        int i2;
        int i3 = sProcStateToProcMem[i];
        float f = 1.0f;
        if (procStateMemTracker != null) {
            if (i3 >= procStateMemTracker.mTotalHighestMem) {
                i2 = procStateMemTracker.mTotalHighestMem;
            } else {
                i2 = i3;
            }
            z = i2 < procStateMemTracker.mHighestMem[i3];
            procStateMemTracker.mPendingMemState = i3;
            procStateMemTracker.mPendingHighestMemState = i2;
            if (z) {
                procStateMemTracker.mPendingScalingFactor = 1.0f;
            } else {
                f = procStateMemTracker.mScalingFactor[i3];
                procStateMemTracker.mPendingScalingFactor = 1.5f * f;
            }
        }
        if (z) {
            if (z) {
                jArr = sTestFirstPssTimes;
            } else {
                jArr = sTestSamePssTimes;
            }
        } else if (z) {
            jArr = z2 ? sFirstAsleepPssTimes : sFirstAwakePssTimes;
        } else {
            jArr = z2 ? sSameAsleepPssTimes : sSameAwakePssTimes;
        }
        long j2 = (long) (jArr[i3] * f);
        if (j2 > AppStandbyController.SettingsObserver.DEFAULT_STRONG_USAGE_TIMEOUT) {
            j2 = 3600000;
        }
        return j + j2;
    }

    long getMemLevel(int i) {
        for (int i2 = 0; i2 < this.mOomAdj.length; i2++) {
            if (i <= this.mOomAdj[i2]) {
                return this.mOomMinFree[i2] * 1024;
            }
        }
        return this.mOomMinFree[this.mOomAdj.length - 1] * 1024;
    }

    long getCachedRestoreThresholdKb() {
        return this.mCachedRestoreLevel;
    }

    public static final void setOomAdj(int i, int i2, int i3) {
        if (i <= 0 || i3 == 1001) {
            return;
        }
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(16);
        byteBufferAllocate.putInt(1);
        byteBufferAllocate.putInt(i);
        byteBufferAllocate.putInt(i2);
        byteBufferAllocate.putInt(i3);
        writeLmkd(byteBufferAllocate);
        long jElapsedRealtime2 = SystemClock.elapsedRealtime() - jElapsedRealtime;
        if (jElapsedRealtime2 > 250) {
            Slog.w(TAG, "SLOW OOM ADJ: " + jElapsedRealtime2 + "ms for pid " + i + " = " + i3);
        }
    }

    public static final void remove(int i) {
        if (i <= 0) {
            return;
        }
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(8);
        byteBufferAllocate.putInt(2);
        byteBufferAllocate.putInt(i);
        writeLmkd(byteBufferAllocate);
    }

    private static boolean openLmkdSocket() {
        try {
            sLmkdSocket = new LocalSocket(3);
            sLmkdSocket.connect(new LocalSocketAddress("lmkd", LocalSocketAddress.Namespace.RESERVED));
            sLmkdOutputStream = sLmkdSocket.getOutputStream();
            return true;
        } catch (IOException e) {
            Slog.w(TAG, "lowmemorykiller daemon socket open failed");
            sLmkdSocket = null;
            return false;
        }
    }

    private static void writeLmkd(ByteBuffer byteBuffer) {
        for (int i = 0; i < 3; i++) {
            if (sLmkdSocket != null || openLmkdSocket()) {
                try {
                    sLmkdOutputStream.write(byteBuffer.array(), 0, byteBuffer.position());
                    return;
                } catch (IOException e) {
                    Slog.w(TAG, "Error writing to lowmemorykiller socket");
                    try {
                        sLmkdSocket.close();
                    } catch (IOException e2) {
                    }
                    sLmkdSocket = null;
                }
            } else {
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e3) {
                }
            }
        }
    }
}
