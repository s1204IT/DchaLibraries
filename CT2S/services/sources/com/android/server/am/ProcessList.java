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
import com.android.server.wm.WindowManagerService;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

final class ProcessList {
    static final int BACKUP_APP_ADJ = 3;
    static final int CACHED_APP_MAX_ADJ = 15;
    static final int CACHED_APP_MIN_ADJ = 9;
    static final int FOREGROUND_APP_ADJ = 0;
    static final int HEAVY_WEIGHT_APP_ADJ = 4;
    static final int HOME_APP_ADJ = 6;
    static final byte LMK_PROCPRIO = 1;
    static final byte LMK_PROCREMOVE = 2;
    static final byte LMK_TARGET = 0;
    static final int MAX_CACHED_APPS = 32;
    static final int MIN_CACHED_APPS = 2;
    static final int MIN_CRASH_INTERVAL = 60000;
    static final int NATIVE_ADJ = -17;
    static final int PAGE_SIZE = 4096;
    static final int PERCEPTIBLE_APP_ADJ = 2;
    static final int PERSISTENT_PROC_ADJ = -12;
    static final int PERSISTENT_SERVICE_ADJ = -11;
    static final int PREVIOUS_APP_ADJ = 7;
    public static final int PROC_MEM_CACHED = 4;
    public static final int PROC_MEM_IMPORTANT = 2;
    public static final int PROC_MEM_PERSISTENT = 0;
    public static final int PROC_MEM_SERVICE = 3;
    public static final int PROC_MEM_TOP = 1;
    public static final int PSS_ALL_INTERVAL = 600000;
    private static final int PSS_FIRST_BACKGROUND_INTERVAL = 20000;
    private static final int PSS_FIRST_CACHED_INTERVAL = 30000;
    private static final int PSS_FIRST_TOP_INTERVAL = 10000;
    public static final int PSS_MAX_INTERVAL = 1800000;
    public static final int PSS_MIN_TIME_FROM_STATE_CHANGE = 15000;
    public static final int PSS_SAFE_TIME_FROM_STATE_CHANGE = 1000;
    private static final int PSS_SAME_CACHED_INTERVAL = 1800000;
    private static final int PSS_SAME_IMPORTANT_INTERVAL = 900000;
    private static final int PSS_SAME_SERVICE_INTERVAL = 1200000;
    private static final int PSS_SHORT_INTERVAL = 120000;
    private static final int PSS_TEST_FIRST_BACKGROUND_INTERVAL = 5000;
    private static final int PSS_TEST_FIRST_TOP_INTERVAL = 3000;
    public static final int PSS_TEST_MIN_TIME_FROM_STATE_CHANGE = 10000;
    private static final int PSS_TEST_SAME_BACKGROUND_INTERVAL = 15000;
    private static final int PSS_TEST_SAME_IMPORTANT_INTERVAL = 10000;
    static final int SERVICE_ADJ = 5;
    static final int SERVICE_B_ADJ = 8;
    static final int SYSTEM_ADJ = -16;
    static final int TRIM_CRITICAL_THRESHOLD = 3;
    static final int TRIM_LOW_THRESHOLD = 5;
    static final int UNKNOWN_ADJ = 16;
    static final int VISIBLE_APP_ADJ = 1;
    private static OutputStream sLmkdOutputStream;
    private static LocalSocket sLmkdSocket;
    private long mCachedRestoreLevel;
    private boolean mHaveDisplaySize;
    private final long mTotalMemMb;
    private static final int MAX_EMPTY_APPS = computeEmptyProcessLimit(32);
    static final int TRIM_EMPTY_APPS = MAX_EMPTY_APPS / 2;
    static final int TRIM_CACHED_APPS = (32 - MAX_EMPTY_APPS) / 3;
    private static final int[] sProcStateToProcMem = {0, 0, 1, 2, 2, 2, 2, 3, 4, 4, 4, 4, 4, 4};
    private static final long[] sFirstAwakePssTimes = {120000, 120000, 10000, 20000, 20000, 20000, 20000, 20000, 30000, 30000, 30000, 30000, 30000, 30000};
    static final long MAX_EMPTY_TIME = 1800000;
    private static final long[] sSameAwakePssTimes = {900000, 900000, 120000, 900000, 900000, 900000, 900000, 1200000, 1200000, MAX_EMPTY_TIME, MAX_EMPTY_TIME, MAX_EMPTY_TIME, MAX_EMPTY_TIME, MAX_EMPTY_TIME};
    private static final long[] sTestFirstAwakePssTimes = {3000, 3000, 3000, 5000, 5000, 5000, 5000, 5000, 5000, 5000, 5000, 5000, 5000, 5000};
    private static final long[] sTestSameAwakePssTimes = {15000, 15000, 10000, 10000, 10000, 10000, 10000, 15000, 15000, 15000, 15000, 15000, 15000, 15000};
    private final int[] mOomAdj = {0, 1, 2, 3, 9, 15};
    private final int[] mOomMinFreeLow = {12288, 18432, 24576, 36864, 43008, 49152};
    private final int[] mOomMinFreeHigh = {73728, 92160, 110592, 129024, 147456, 184320};
    private final int[] mOomMinFree = new int[this.mOomAdj.length];

    ProcessList() {
        MemInfoReader minfo = new MemInfoReader();
        minfo.readMemInfo();
        this.mTotalMemMb = minfo.getTotalSize() / 1048576;
        updateOomLevels(0, 0, false);
    }

    void applyDisplaySize(WindowManagerService wm) {
        if (!this.mHaveDisplaySize) {
            Point p = new Point();
            wm.getBaseDisplaySize(0, p);
            if (p.x != 0 && p.y != 0) {
                updateOomLevels(p.x, p.y, true);
                this.mHaveDisplaySize = true;
            }
        }
    }

    private void updateOomLevels(int displayWidth, int displayHeight, boolean write) {
        float scaleMem = (this.mTotalMemMb - 350) / 350.0f;
        float scaleDisp = ((displayWidth * displayHeight) - 384000) / 640000;
        float scale = scaleMem > scaleDisp ? scaleMem : scaleDisp;
        if (scale < 0.0f) {
            scale = 0.0f;
        } else if (scale > 1.0f) {
            scale = 1.0f;
        }
        int minfree_adj = Resources.getSystem().getInteger(R.integer.autofill_max_visible_datasets);
        int minfree_abs = Resources.getSystem().getInteger(R.integer.auto_data_switch_validation_max_retry);
        boolean is64bit = Build.SUPPORTED_64_BIT_ABIS.length > 0;
        for (int i = 0; i < this.mOomAdj.length; i++) {
            int low = this.mOomMinFreeLow[i];
            int high = this.mOomMinFreeHigh[i];
            if (is64bit) {
                if (i == 4) {
                    high = (high * 3) / 2;
                } else if (i == 5) {
                    high = (high * 7) / 4;
                }
            }
            this.mOomMinFree[i] = (int) (low + ((high - low) * scale));
        }
        if (minfree_abs >= 0) {
            for (int i2 = 0; i2 < this.mOomAdj.length; i2++) {
                this.mOomMinFree[i2] = (int) ((minfree_abs * this.mOomMinFree[i2]) / this.mOomMinFree[this.mOomAdj.length - 1]);
            }
        }
        if (minfree_adj != 0) {
            for (int i3 = 0; i3 < this.mOomAdj.length; i3++) {
                int[] iArr = this.mOomMinFree;
                iArr[i3] = iArr[i3] + ((int) ((minfree_adj * this.mOomMinFree[i3]) / this.mOomMinFree[this.mOomAdj.length - 1]));
                if (this.mOomMinFree[i3] < 0) {
                    this.mOomMinFree[i3] = 0;
                }
            }
        }
        this.mCachedRestoreLevel = (getMemLevel(15) / 1024) / 3;
        int reserve = (((displayWidth * displayHeight) * 4) * 3) / 1024;
        int reserve_adj = Resources.getSystem().getInteger(R.integer.bugreport_state_show);
        int reserve_abs = Resources.getSystem().getInteger(R.integer.bugreport_state_hide);
        if (reserve_abs >= 0) {
            reserve = reserve_abs;
        }
        if (reserve_adj != 0 && (reserve = reserve + reserve_adj) < 0) {
            reserve = 0;
        }
        if (write) {
            ByteBuffer buf = ByteBuffer.allocate(((this.mOomAdj.length * 2) + 1) * 4);
            buf.putInt(0);
            for (int i4 = 0; i4 < this.mOomAdj.length; i4++) {
                buf.putInt((this.mOomMinFree[i4] * 1024) / 4096);
                buf.putInt(this.mOomAdj[i4]);
            }
            writeLmkd(buf);
            SystemProperties.set("sys.sysctl.extra_free_kbytes", Integer.toString(reserve));
        }
    }

    public static int computeEmptyProcessLimit(int totalProcessLimit) {
        return totalProcessLimit / 2;
    }

    private static String buildOomTag(String prefix, String space, int val, int base) {
        if (val == base) {
            if (space != null) {
                return prefix + "  ";
            }
            return prefix;
        }
        return prefix + "+" + Integer.toString(val - base);
    }

    public static String makeOomAdjString(int setAdj) {
        if (setAdj >= 9) {
            return buildOomTag("cch", "  ", setAdj, 9);
        }
        if (setAdj >= 8) {
            return buildOomTag("svcb ", null, setAdj, 8);
        }
        if (setAdj >= 7) {
            return buildOomTag("prev ", null, setAdj, 7);
        }
        if (setAdj >= 6) {
            return buildOomTag("home ", null, setAdj, 6);
        }
        if (setAdj >= 5) {
            return buildOomTag("svc  ", null, setAdj, 5);
        }
        if (setAdj >= 4) {
            return buildOomTag("hvy  ", null, setAdj, 4);
        }
        if (setAdj >= 3) {
            return buildOomTag("bkup ", null, setAdj, 3);
        }
        if (setAdj >= 2) {
            return buildOomTag("prcp ", null, setAdj, 2);
        }
        if (setAdj >= 1) {
            return buildOomTag("vis  ", null, setAdj, 1);
        }
        if (setAdj >= 0) {
            return buildOomTag("fore ", null, setAdj, 0);
        }
        if (setAdj >= PERSISTENT_SERVICE_ADJ) {
            return buildOomTag("psvc ", null, setAdj, PERSISTENT_SERVICE_ADJ);
        }
        if (setAdj >= PERSISTENT_PROC_ADJ) {
            return buildOomTag("pers ", null, setAdj, PERSISTENT_PROC_ADJ);
        }
        if (setAdj >= SYSTEM_ADJ) {
            return buildOomTag("sys  ", null, setAdj, SYSTEM_ADJ);
        }
        if (setAdj >= NATIVE_ADJ) {
            return buildOomTag("ntv  ", null, setAdj, NATIVE_ADJ);
        }
        return Integer.toString(setAdj);
    }

    public static String makeProcStateString(int curProcState) {
        switch (curProcState) {
            case -1:
                return "N ";
            case 0:
                return "P ";
            case 1:
                return "PU";
            case 2:
                return "T ";
            case 3:
                return "IF";
            case 4:
                return "IB";
            case 5:
                return "BU";
            case 6:
                return "HW";
            case 7:
                return "S ";
            case 8:
                return "R ";
            case 9:
                return "HO";
            case 10:
                return "LA";
            case 11:
                return "CA";
            case 12:
                return "Ca";
            case 13:
                return "CE";
            default:
                return "??";
        }
    }

    public static void appendRamKb(StringBuilder sb, long ramKb) {
        int j = 0;
        int fact = 10;
        while (j < 6) {
            if (ramKb < fact) {
                sb.append(' ');
            }
            j++;
            fact *= 10;
        }
        sb.append(ramKb);
    }

    public static boolean procStatesDifferForMem(int procState1, int procState2) {
        return sProcStateToProcMem[procState1] != sProcStateToProcMem[procState2];
    }

    public static long minTimeFromStateChange(boolean test) {
        return test ? 10000L : 15000L;
    }

    public static long computeNextPssTime(int procState, boolean first, boolean test, boolean sleeping, long now) {
        long[] table;
        if (test) {
            table = first ? sTestFirstAwakePssTimes : sTestSameAwakePssTimes;
        } else {
            table = first ? sFirstAwakePssTimes : sSameAwakePssTimes;
        }
        return table[procState] + now;
    }

    long getMemLevel(int adjustment) {
        for (int i = 0; i < this.mOomAdj.length; i++) {
            if (adjustment <= this.mOomAdj[i]) {
                return this.mOomMinFree[i] * 1024;
            }
        }
        return this.mOomMinFree[this.mOomAdj.length - 1] * 1024;
    }

    long getCachedRestoreThresholdKb() {
        return this.mCachedRestoreLevel;
    }

    public static final void setOomAdj(int pid, int uid, int amt) {
        if (amt != 16) {
            long start = SystemClock.elapsedRealtime();
            ByteBuffer buf = ByteBuffer.allocate(16);
            buf.putInt(1);
            buf.putInt(pid);
            buf.putInt(uid);
            buf.putInt(amt);
            writeLmkd(buf);
            long now = SystemClock.elapsedRealtime();
            if (now - start > 250) {
                Slog.w("ActivityManager", "SLOW OOM ADJ: " + (now - start) + "ms for pid " + pid + " = " + amt);
            }
        }
    }

    public static final void remove(int pid) {
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.putInt(2);
        buf.putInt(pid);
        writeLmkd(buf);
    }

    private static boolean openLmkdSocket() {
        try {
            sLmkdSocket = new LocalSocket(3);
            sLmkdSocket.connect(new LocalSocketAddress("lmkd", LocalSocketAddress.Namespace.RESERVED));
            sLmkdOutputStream = sLmkdSocket.getOutputStream();
            return true;
        } catch (IOException e) {
            Slog.w("ActivityManager", "lowmemorykiller daemon socket open failed");
            sLmkdSocket = null;
            return false;
        }
    }

    private static void writeLmkd(ByteBuffer buf) {
        for (int i = 0; i < 3; i++) {
            if (sLmkdSocket == null && !openLmkdSocket()) {
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                }
            } else {
                try {
                    sLmkdOutputStream.write(buf.array(), 0, buf.position());
                    return;
                } catch (IOException e2) {
                    Slog.w("ActivityManager", "Error writing to lowmemorykiller socket");
                    try {
                        sLmkdSocket.close();
                    } catch (IOException e3) {
                    }
                    sLmkdSocket = null;
                }
            }
        }
    }
}
