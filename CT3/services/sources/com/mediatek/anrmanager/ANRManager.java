package com.mediatek.anrmanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Debug;
import android.os.FileUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.SELinux;
import android.os.StrictMode;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.os.ProcessCpuTracker;
import com.mediatek.aee.ExceptionLog;
import com.mediatek.anrappframeworks.ANRAppFrameworks;
import com.mediatek.anrappmanager.ANRAppManager;
import com.mediatek.anrappmanager.ANRManagerNative;
import com.mediatek.common.jpe.a;
import com.mediatek.datashaping.DataShapingUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ANRManager extends ANRManagerNative {
    public static int AnrOption = 1;
    public static final int DISABLE_ALL_ANR_MECHANISM = 0;
    public static final int DISABLE_PARTIAL_ANR_MECHANISM = 1;
    public static final int ENABLE_ALL_ANR_MECHANISM = 2;
    public static final int EVENT_BOOT_COMPLETED = 9001;
    public static final boolean IS_USER_BUILD;
    protected static final int MAX_MTK_TRACE_COUNT = 10;
    private static String[] NATIVE_STACKS_OF_INTEREST = null;
    protected static final int REMOVE_KEYDISPATCHING_TIMEOUT_MSG = 1005;
    public static final int RENAME_TRACE_FILES_MSG = 1006;
    protected static final int START_ANR_DUMP_MSG = 1003;
    public static final int START_MONITOR_BROADCAST_TIMEOUT_MSG = 1001;
    protected static final int START_MONITOR_KEYDISPATCHING_TIMEOUT_MSG = 1004;
    public static final int START_MONITOR_SERVICE_TIMEOUT_MSG = 1002;
    protected static ArrayList<Integer> additionNBTList;
    private static IAnrActivityManagerService b;
    private static final Object c;
    private static final ProcessCpuTracker d;
    public static ConcurrentHashMap<Integer, String> mMessageMap;
    public static int[] mZygotePids;
    private int a;
    private Context f;
    public AnrDumpMgr mAnrDumpMgr;
    public AnrMonitorHandler mAnrHandler;
    private final AtomicLong e = new AtomicLong(0);
    private long g = 0;
    private long h = 0;
    private int i = -1;

    public interface IAnrActivityManagerService {
        File dumpStackTraces(boolean z, ArrayList<Integer> arrayList, ProcessCpuTracker processCpuTracker, SparseArray<Boolean> sparseArray, String[] strArr);

        ArrayList<Integer> getInterestingPids();

        boolean getMonitorCpuUsage();

        void getPidFromLruProcesses(int i, int i2, ArrayList<Integer> arrayList, SparseArray<Boolean> sparseArray);

        ProcessCpuTracker getProcessCpuTracker();

        int getProcessRecordPid(Object obj);

        boolean getShuttingDown();

        void updateCpuStatsNow();
    }

    public interface IAnrBroadcastQueue {
        int getOrderedBroadcastsPid();
    }

    static {
        IS_USER_BUILD = "user".equals(Build.TYPE) || "userdebug".equals(Build.TYPE);
        mZygotePids = null;
        c = new Object();
        NATIVE_STACKS_OF_INTEREST = new String[]{"/system/bin/mediaserver", "/system/bin/surfaceflinger", "/system/bin/netd"};
        d = new ProcessCpuTracker(false);
        additionNBTList = new ArrayList<>();
        mMessageMap = new ConcurrentHashMap<>();
    }

    private ANRManager() {
    }

    public ANRManager(IAnrActivityManagerService iAnrActivityManagerService, int i, Context context) {
        this.a = i;
        b = iAnrActivityManagerService;
        this.f = context;
        if (!IS_USER_BUILD) {
            Looper.myLooper().setMessageLogging(ANRAppManager.getDefault(new ANRAppFrameworks()).newMessageLogger(false, Thread.currentThread().getName()));
        }
    }

    public void startANRManager() {
        new a().a();
        HandlerThread handlerThread = new HandlerThread("AnrMonitorThread");
        handlerThread.start();
        this.mAnrHandler = new AnrMonitorHandler(handlerThread.getLooper());
        this.mAnrDumpMgr = new AnrDumpMgr();
        d.init();
        prepareStackTraceFile(SystemProperties.get("dalvik.vm.mtk-stack-trace-file", (String) null));
        prepareStackTraceFile(SystemProperties.get("dalvik.vm.stack-trace-file", (String) null));
        prepareStackTraceFile("/data/anr/native1.txt");
        prepareStackTraceFile("/data/anr/native2.txt");
        File parentFile = new File(SystemProperties.get("dalvik.vm.stack-trace-file", (String) null)).getParentFile();
        if (parentFile != null && !SELinux.restoreconRecursive(parentFile)) {
            Slog.d("ANRManager", "startANRManager SELinux.restoreconRecursive fail dir = " + parentFile.toString());
        }
    }

    public class AnrMonitorHandler extends Handler {
        public AnrMonitorHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) throws Throwable {
            switch (message.what) {
                case ANRManager.START_MONITOR_BROADCAST_TIMEOUT_MSG:
                    IAnrBroadcastQueue iAnrBroadcastQueue = (IAnrBroadcastQueue) message.obj;
                    if (iAnrBroadcastQueue != null) {
                        Slog.i("ANRManager", "monitor Broadcast ANR process (" + iAnrBroadcastQueue.getOrderedBroadcastsPid() + ")");
                        ANRManager.this.updateProcessStats();
                        ANRManager.this.setZramTag("5");
                    } else {
                        Slog.i("ANRManager", "monitor Broadcast ANR process failed");
                    }
                    break;
                case ANRManager.START_MONITOR_SERVICE_TIMEOUT_MSG:
                    Slog.i("ANRManager", "monitor Service ANR process (" + ANRManager.b.getProcessRecordPid(message.obj) + ")");
                    ANRManager.this.updateProcessStats();
                    ANRManager.this.setZramTag("5");
                    break;
                case ANRManager.START_ANR_DUMP_MSG:
                    AnrDumpRecord anrDumpRecord = (AnrDumpRecord) message.obj;
                    Slog.i("ANRManager", "START_ANR_DUMP_MSG: " + anrDumpRecord);
                    ANRManager.this.mAnrDumpMgr.dumpAnrDebugInfo(anrDumpRecord, true);
                    break;
                case ANRManager.START_MONITOR_KEYDISPATCHING_TIMEOUT_MSG:
                    Slog.i("ANRManager", "Monitor KeyDispatching ANR process (" + message.arg1 + ")");
                    ANRManager.this.updateProcessStats();
                    ANRManager.this.setZramTag("5");
                    break;
                case ANRManager.RENAME_TRACE_FILES_MSG:
                    String str = SystemProperties.get("dalvik.vm.stack-trace-file", (String) null);
                    if (str != null && str.length() != 0) {
                        ANRManager.renameFiles(true, str, "/data/anr/traces_");
                        ANRManager.renameFiles(true, "/data/anr/native1.txt", "/data/anr/native1_");
                        ANRManager.renameFiles(true, "/data/anr/native2.txt", "/data/anr/native2_");
                    }
                    break;
            }
        }
    }

    protected static final class BinderWatchdog {
        protected BinderWatchdog() {
        }

        protected static class BinderInfo {
            protected static final int INDEX_FROM = 1;
            protected static final int INDEX_TO = 3;
            protected int mDstPid;
            protected int mDstTid;
            protected int mSrcPid;
            protected int mSrcTid;
            protected String mText;

            protected BinderInfo(String str) {
                if (str == null || str.length() <= 0) {
                    return;
                }
                this.mText = new String(str);
                String[] strArrSplit = str.split(" ");
                String[] strArrSplit2 = strArrSplit[1].split(":");
                if (strArrSplit2 != null && strArrSplit2.length == 2) {
                    this.mSrcPid = Integer.parseInt(strArrSplit2[0]);
                    this.mSrcTid = Integer.parseInt(strArrSplit2[1]);
                }
                String[] strArrSplit3 = strArrSplit[3].split(":");
                if (strArrSplit3 != null && strArrSplit3.length == 2) {
                    this.mDstPid = Integer.parseInt(strArrSplit3[0]);
                    this.mDstTid = Integer.parseInt(strArrSplit3[1]);
                }
            }
        }

        public static final ArrayList<Integer> getTimeoutBinderPidList(int i, int i2) {
            int i3 = 0;
            if (i <= 0) {
                return null;
            }
            ArrayList<BinderInfo> arrayListD = d();
            ArrayList<Integer> arrayList = new ArrayList<>();
            for (BinderInfo binderInfoA = a(i, i2, arrayListD); binderInfoA != null; binderInfoA = a(binderInfoA.mDstPid, binderInfoA.mDstTid, arrayListD)) {
                if (binderInfoA.mDstPid > 0) {
                    i3++;
                    if (!arrayList.contains(Integer.valueOf(binderInfoA.mDstPid))) {
                        Slog.d("ANRManager", "getTimeoutBinderPidList pid added: " + binderInfoA.mDstPid + " " + binderInfoA.mText);
                        arrayList.add(Integer.valueOf(binderInfoA.mDstPid));
                    } else {
                        Slog.d("ANRManager", "getTimeoutBinderPidList pid existed: " + binderInfoA.mDstPid + " " + binderInfoA.mText);
                    }
                    if (i3 >= 5) {
                        break;
                    }
                }
            }
            if (arrayList == null || arrayList.size() == 0) {
                return getTimeoutBinderFromPid(i, arrayListD);
            }
            return arrayList;
        }

        public static final ArrayList<Integer> getTimeoutBinderFromPid(int i, ArrayList<BinderInfo> arrayList) {
            int i2 = 0;
            if (i <= 0 || arrayList == null) {
                return null;
            }
            Slog.d("ANRManager", "getTimeoutBinderFromPid " + i + " list size: " + arrayList.size());
            ArrayList<Integer> arrayList2 = new ArrayList<>();
            Iterator<BinderInfo> it = arrayList.iterator();
            while (true) {
                int i3 = i2;
                if (!it.hasNext()) {
                    break;
                }
                BinderInfo next = it.next();
                if (next != null && next.mSrcPid == i) {
                    i3++;
                    if (!arrayList2.contains(Integer.valueOf(next.mDstPid))) {
                        Slog.d("ANRManager", "getTimeoutBinderFromPid pid added: " + next.mDstPid + " " + next.mText);
                        arrayList2.add(Integer.valueOf(next.mDstPid));
                    } else {
                        Slog.d("ANRManager", "getTimeoutBinderFromPid pid existed: " + next.mDstPid + " " + next.mText);
                    }
                    if (i3 >= 5) {
                        break;
                    }
                }
                i2 = i3;
            }
            return arrayList2;
        }

        private static BinderInfo a(int i, int i2, ArrayList<BinderInfo> arrayList) {
            if (arrayList == null || arrayList.size() == 0 || i == 0) {
                return null;
            }
            arrayList.size();
            for (BinderInfo binderInfo : arrayList) {
                if (binderInfo.mSrcPid == i && binderInfo.mSrcTid == i2) {
                    Slog.d("ANRManager", "Timeout binder pid found: " + binderInfo.mDstPid + " " + binderInfo.mText);
                    return binderInfo;
                }
            }
            return null;
        }

        private static final ArrayList<BinderInfo> d() {
            BufferedReader bufferedReader;
            ArrayList<BinderInfo> arrayList;
            ArrayList<BinderInfo> arrayList2 = null;
            arrayList2 = null;
            BufferedReader bufferedReader2 = null;
            arrayList2 = null;
            arrayList2 = null;
            try {
                try {
                    File file = new File("/sys/kernel/debug/binder/timeout_log");
                    if (file == null || !file.exists()) {
                        return null;
                    }
                    bufferedReader = new BufferedReader(new FileReader(file));
                    try {
                        arrayList = new ArrayList<>();
                        do {
                            try {
                                String line = bufferedReader.readLine();
                                if (line == null) {
                                    break;
                                }
                                BinderInfo binderInfo = new BinderInfo(line);
                                if (binderInfo != null && binderInfo.mSrcPid > 0) {
                                    arrayList.add(binderInfo);
                                }
                            } catch (FileNotFoundException e) {
                                e = e;
                                bufferedReader2 = bufferedReader;
                                try {
                                    Slog.e("ANRManager", "FileNotFoundException", e);
                                    if (bufferedReader2 != null) {
                                        try {
                                            bufferedReader2.close();
                                        } catch (IOException e2) {
                                            Slog.e("ANRManager", "IOException when close buffer reader:", e2);
                                        }
                                    }
                                    return arrayList;
                                } catch (Throwable th) {
                                    bufferedReader = bufferedReader2;
                                    arrayList2 = arrayList;
                                }
                            } catch (IOException e3) {
                                e = e3;
                                arrayList2 = arrayList;
                                Slog.e("ANRManager", "IOException when gettting Binder. ", e);
                                if (bufferedReader != null) {
                                    try {
                                        bufferedReader.close();
                                    } catch (IOException e4) {
                                        Slog.e("ANRManager", "IOException when close buffer reader:", e4);
                                    }
                                }
                                return arrayList2;
                            } catch (Throwable th2) {
                                arrayList2 = arrayList;
                            }
                        } while (arrayList.size() <= 64);
                        if (bufferedReader != null) {
                            try {
                                bufferedReader.close();
                            } catch (IOException e5) {
                                Slog.e("ANRManager", "IOException when close buffer reader:", e5);
                            }
                        }
                        return arrayList;
                    } catch (FileNotFoundException e6) {
                        e = e6;
                        arrayList = null;
                        bufferedReader2 = bufferedReader;
                    } catch (IOException e7) {
                        e = e7;
                    }
                } catch (Throwable th3) {
                }
            } catch (FileNotFoundException e8) {
                e = e8;
                arrayList = null;
            } catch (IOException e9) {
                e = e9;
                bufferedReader = null;
            } catch (Throwable th4) {
                bufferedReader = null;
            }
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e10) {
                    Slog.e("ANRManager", "IOException when close buffer reader:", e10);
                }
            }
            return arrayList2;
        }

        protected static class TransactionInfo {
            protected String atime;
            protected String direction;
            protected String ktime;
            protected String rcv_pid;
            protected String rcv_tid;
            protected String snd_pid;
            protected String snd_tid;
            protected long spent_time;

            protected TransactionInfo() {
            }
        }

        private static final void a(int i, ArrayList<Integer> arrayList) throws Throwable {
            BufferedReader bufferedReader;
            BufferedReader bufferedReader2 = null;
            Pattern patternCompile = Pattern.compile("(\\S+.+transaction).+from\\s+(\\d+):(\\d+)\\s+to\\s+(\\d+):(\\d+).+start\\s+(\\d+\\.+\\d+).+android\\s+(\\d+-\\d+-\\d+\\s+\\d+:\\d+:\\d+\\.\\d+)");
            ArrayList arrayList2 = new ArrayList();
            ArrayList arrayList3 = new ArrayList();
            try {
                try {
                    File file = new File("/sys/kernel/debug/binder/proc/" + Integer.toString(i));
                    if (file == null || !file.exists()) {
                        Log.d("ANRManager", "Filepath isn't exist");
                        return;
                    }
                    bufferedReader = new BufferedReader(new FileReader(file));
                    while (true) {
                        try {
                            String line = bufferedReader.readLine();
                            if (line == null) {
                                break;
                            }
                            if (!line.contains("transaction")) {
                                if (line.indexOf("node") != -1 && line.indexOf("node") < 20) {
                                    break;
                                }
                            } else {
                                Matcher matcher = patternCompile.matcher(line);
                                if (matcher.find()) {
                                    TransactionInfo transactionInfo = new TransactionInfo();
                                    transactionInfo.direction = matcher.group(1);
                                    transactionInfo.snd_pid = matcher.group(2);
                                    transactionInfo.snd_tid = matcher.group(3);
                                    transactionInfo.rcv_pid = matcher.group(4);
                                    transactionInfo.rcv_tid = matcher.group(5);
                                    transactionInfo.ktime = matcher.group(6);
                                    transactionInfo.atime = matcher.group(7);
                                    transactionInfo.spent_time = SystemClock.uptimeMillis() - ((long) (Float.valueOf(transactionInfo.ktime).floatValue() * 1000.0f));
                                    arrayList2.add(transactionInfo);
                                    if (!(transactionInfo.spent_time < 1000) && !arrayList.contains(Integer.valueOf(transactionInfo.rcv_pid))) {
                                        arrayList.add(Integer.valueOf(transactionInfo.rcv_pid));
                                        if (!arrayList3.contains(Integer.valueOf(transactionInfo.rcv_pid))) {
                                            arrayList3.add(Integer.valueOf(transactionInfo.rcv_pid));
                                            Log.d("ANRManager", "Transcation binderList pid=" + transactionInfo.rcv_pid);
                                        }
                                    }
                                    Log.d("ANRManager", transactionInfo.direction + " from " + transactionInfo.snd_pid + ":" + transactionInfo.snd_tid + " to " + transactionInfo.rcv_pid + ":" + transactionInfo.rcv_tid + " start " + transactionInfo.ktime + " android time " + transactionInfo.atime + " spent time " + transactionInfo.spent_time + " ms");
                                }
                            }
                        } catch (FileNotFoundException e) {
                            e = e;
                            Log.e("ANRManager", "FileNotFoundException", e);
                            if (bufferedReader != null) {
                                try {
                                    bufferedReader.close();
                                    return;
                                } catch (IOException e2) {
                                    Slog.e("ANRManager", "IOException when close buffer reader:", e2);
                                    return;
                                }
                            }
                            return;
                        } catch (IOException e3) {
                            e = e3;
                            Log.e("ANRManager", "IOException when gettting Binder. ", e);
                            if (bufferedReader != null) {
                                try {
                                    bufferedReader.close();
                                    return;
                                } catch (IOException e4) {
                                    Slog.e("ANRManager", "IOException when close buffer reader:", e4);
                                    return;
                                }
                            }
                            return;
                        }
                    }
                    Iterator it = arrayList3.iterator();
                    while (it.hasNext()) {
                        a(((Integer) it.next()).intValue(), arrayList);
                    }
                    if (bufferedReader == null) {
                        return;
                    }
                    try {
                        bufferedReader.close();
                    } catch (IOException e5) {
                        Slog.e("ANRManager", "IOException when close buffer reader:", e5);
                    }
                } catch (Throwable th) {
                    th = th;
                    if (0 != 0) {
                        try {
                            bufferedReader2.close();
                        } catch (IOException e6) {
                            Slog.e("ANRManager", "IOException when close buffer reader:", e6);
                        }
                    }
                    throw th;
                }
            } catch (FileNotFoundException e7) {
                e = e7;
                bufferedReader = null;
            } catch (IOException e8) {
                e = e8;
                bufferedReader = null;
            } catch (Throwable th2) {
                th = th2;
                if (0 != 0) {
                }
                throw th;
            }
        }

        private static final void a(int i, ArrayList<Integer> arrayList, SparseArray<Boolean> sparseArray) throws Throwable {
            int iIntValue;
            ArrayList<Integer> arrayList2 = new ArrayList();
            a(i, arrayList2);
            if (arrayList2 != null && arrayList2.size() > 0) {
                for (Integer num : arrayList2) {
                    if (num != null && (iIntValue = num.intValue()) != i && !arrayList.contains(Integer.valueOf(iIntValue))) {
                        arrayList.add(Integer.valueOf(iIntValue));
                        if (sparseArray != null) {
                            sparseArray.remove(iIntValue);
                        }
                    }
                }
            }
        }
    }

    public void prepareStackTraceFile(String str) {
        Slog.i("ANRManager", "prepareStackTraceFile: " + str);
        if (str == null || str.length() == 0) {
            return;
        }
        File file = new File(str);
        try {
            File parentFile = file.getParentFile();
            if (parentFile != null) {
                if (!parentFile.exists()) {
                    parentFile.mkdirs();
                }
                FileUtils.setPermissions(parentFile.getPath(), 509, -1, -1);
            }
            if (!file.exists()) {
                file.createNewFile();
            }
            FileUtils.setPermissions(file.getPath(), 438, -1, -1);
        } catch (IOException e) {
            Slog.w("ANRManager", "Unable to prepare stack trace file: " + str, e);
        }
    }

    public class AnrDumpRecord {
        protected String mAnnotation;
        protected long mAnrTime;
        protected boolean mAppCrashing;
        protected int mAppPid;
        protected String mAppString;
        public String mCpuInfo = null;
        public StringBuilder mInfo = new StringBuilder(256);
        protected boolean mIsCancelled;
        protected boolean mIsCompleted;
        protected int mParentAppPid;
        protected String mParentShortComponentName;
        protected String mProcessName;
        protected String mShortComponentName;

        public AnrDumpRecord(int i, boolean z, String str, String str2, String str3, int i2, String str4, String str5, long j) {
            this.mAppPid = i;
            this.mAppCrashing = z;
            this.mProcessName = str;
            this.mAppString = str2;
            this.mShortComponentName = str3;
            this.mParentAppPid = i2;
            this.mParentShortComponentName = str4;
            this.mAnnotation = str5;
            this.mAnrTime = j;
        }

        private boolean isValid() {
            return (this.mAppPid <= 0 || this.mIsCancelled || this.mIsCompleted) ? false : true;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("AnrDumpRecord{ ");
            sb.append(this.mAnnotation);
            sb.append(" ");
            sb.append(this.mAppString);
            sb.append(" IsCompleted:" + this.mIsCompleted);
            sb.append(" IsCancelled:" + this.mIsCancelled);
            sb.append(" }");
            return sb.toString();
        }
    }

    public void delayRenameTraceFiles(int i) {
        this.mAnrHandler.removeMessages(RENAME_TRACE_FILES_MSG);
        this.mAnrHandler.sendEmptyMessageDelayed(RENAME_TRACE_FILES_MSG, i);
    }

    public static File renameFiles(boolean z, String str, String str2) {
        File file = new File(str);
        Slog.d("ANRManager", "renameFiles Begin, clearTraces=" + z + ", nativetracesPath=" + str + ", subnativetracesPath=" + str2);
        try {
            File parentFile = file.getParentFile();
            if (parentFile != null) {
                if (!parentFile.exists()) {
                    parentFile.mkdirs();
                }
                FileUtils.setPermissions(parentFile.getPath(), 509, -1, -1);
            }
            if (z && file.exists()) {
                synchronized (c) {
                    for (int i = 8; i > 0; i--) {
                        File file2 = new File(str2 + Integer.toString(i) + ".txt");
                        if (file2.exists()) {
                            file2.renameTo(new File(str2 + Integer.toString(i + 1) + ".txt"));
                        }
                    }
                    file.renameTo(new File(str2 + "1.txt"));
                }
            }
            file.createNewFile();
            FileUtils.setPermissions(file.getPath(), 438, -1, -1);
            Slog.d("ANRManager", "renameFiles End");
            return file;
        } catch (IOException e) {
            Slog.w("ANRManager", "Unable to prepare ANR traces file: " + str, e);
            return null;
        }
    }

    public class DumpThread extends Thread {
        private int[] k;
        private String l;
        public boolean mResult = false;

        public DumpThread(int[] iArr, String str) {
            this.k = iArr;
            this.l = str;
        }

        @Override
        public void run() {
            for (int i : this.k) {
                if (!ANRManager.this.isJavaProcess(i) && !ANRManager.this.isProcDoCoredump(i).booleanValue()) {
                    Slog.i("ANRManager", "[DumpNative] DumpThread native process =" + i);
                    Debug.dumpNativeBacktraceToFile(i, this.l);
                }
            }
            this.mResult = true;
        }
    }

    public class AnrDumpMgr {
        public HashMap<Integer, AnrDumpRecord> mDumpList = new HashMap<>();

        public AnrDumpMgr() {
        }

        public void cancelDump(AnrDumpRecord anrDumpRecord) {
            if (anrDumpRecord == null || anrDumpRecord.mAppPid == -1) {
                return;
            }
            synchronized (this.mDumpList) {
                AnrDumpRecord anrDumpRecordRemove = this.mDumpList.remove(Integer.valueOf(anrDumpRecord.mAppPid));
                if (anrDumpRecordRemove != null) {
                    anrDumpRecordRemove.mIsCancelled = true;
                }
            }
        }

        public void removeDumpRecord(AnrDumpRecord anrDumpRecord) {
            if (anrDumpRecord == null || anrDumpRecord.mAppPid == -1) {
                return;
            }
            synchronized (this.mDumpList) {
                this.mDumpList.remove(Integer.valueOf(anrDumpRecord.mAppPid));
            }
        }

        public void startAsyncDump(AnrDumpRecord anrDumpRecord) {
            if (anrDumpRecord == null || anrDumpRecord.mAppPid == -1) {
                return;
            }
            Slog.i("ANRManager", "startAsyncDump: " + anrDumpRecord);
            int i = anrDumpRecord.mAppPid;
            synchronized (this.mDumpList) {
                if (this.mDumpList.containsKey(Integer.valueOf(i))) {
                    return;
                }
                this.mDumpList.put(Integer.valueOf(i), anrDumpRecord);
                ANRManager.this.mAnrHandler.sendMessageAtTime(ANRManager.this.mAnrHandler.obtainMessage(ANRManager.START_ANR_DUMP_MSG, anrDumpRecord), SystemClock.uptimeMillis() + 500);
            }
        }

        private boolean a(AnrDumpRecord anrDumpRecord) {
            synchronized (this.mDumpList) {
                if (anrDumpRecord != null) {
                    if (this.mDumpList.containsKey(Integer.valueOf(anrDumpRecord.mAppPid)) && anrDumpRecord.isValid()) {
                        return true;
                    }
                }
                return false;
            }
        }

        public void dumpAnrDebugInfo(AnrDumpRecord anrDumpRecord, boolean z) throws Throwable {
            if (anrDumpRecord == null) {
                Slog.i("ANRManager", "dumpAnrDebugInfo: " + anrDumpRecord);
                return;
            }
            Slog.i("ANRManager", "dumpAnrDebugInfo begin: " + anrDumpRecord + ", isAsyncDump = " + z);
            if (ANRManager.b.getShuttingDown()) {
                Slog.i("ANRManager", "dumpAnrDebugInfo During shutdown skipping ANR: " + anrDumpRecord.mAppString);
                return;
            }
            if (anrDumpRecord.mAppCrashing) {
                Slog.i("ANRManager", "dumpAnrDebugInfo Crashing app skipping ANR: " + anrDumpRecord.mAppString);
                return;
            }
            if (!a(anrDumpRecord)) {
                Slog.i("ANRManager", "dumpAnrDebugInfo dump stopped: " + anrDumpRecord);
                return;
            }
            ANRManager.this.setZramTag("6");
            ANRManager.this.setZramMonitor(false);
            dumpAnrDebugInfoLocked(anrDumpRecord, z);
            Slog.i("ANRManager", "dumpAnrDebugInfo end: " + anrDumpRecord + ", isAsyncDump = " + z);
        }

        protected void dumpAnrDebugInfoLocked(AnrDumpRecord anrDumpRecord, boolean z) {
            int iIntValue;
            String str;
            synchronized (anrDumpRecord) {
                Slog.i("ANRManager", "dumpAnrDebugInfoLocked: " + anrDumpRecord + ", isAsyncDump = " + z);
                if (a(anrDumpRecord)) {
                    int i = anrDumpRecord.mAppPid;
                    int i2 = anrDumpRecord.mParentAppPid;
                    ArrayList<Integer> arrayList = new ArrayList<>();
                    SparseArray<Boolean> sparseArray = new SparseArray<>(20);
                    ArrayList<Integer> timeoutBinderPidList = i != -1 ? BinderWatchdog.getTimeoutBinderPidList(i, i) : null;
                    arrayList.add(Integer.valueOf(i));
                    int i3 = i2 <= 0 ? i : i2;
                    if (i3 != i) {
                        arrayList.add(Integer.valueOf(i3));
                    }
                    if (ANRManager.this.a != i && ANRManager.this.a != i3) {
                        arrayList.add(Integer.valueOf(ANRManager.this.a));
                    }
                    if (!z) {
                        ANRManager.b.getPidFromLruProcesses(i, i3, arrayList, sparseArray);
                    }
                    if (timeoutBinderPidList != null && timeoutBinderPidList.size() > 0) {
                        for (Integer num : timeoutBinderPidList) {
                            if (num != null && (iIntValue = num.intValue()) != i && iIntValue != i3 && iIntValue != ANRManager.this.a && !arrayList.contains(Integer.valueOf(iIntValue))) {
                                arrayList.add(Integer.valueOf(iIntValue));
                                sparseArray.remove(iIntValue);
                            }
                        }
                    }
                    if (i != -1) {
                        BinderWatchdog.a(i, arrayList, sparseArray);
                    }
                    ANRManager.b.getInterestingPids().clear();
                    ANRManager.b.getInterestingPids().add(Integer.valueOf(i));
                    int i4 = i2 <= 0 ? i3 : i2;
                    if (i4 != i && !ANRManager.this.isJavaProcess(i4)) {
                        ANRManager.b.getInterestingPids().add(Integer.valueOf(i4));
                    }
                    Iterator<Integer> it = ANRManager.additionNBTList.iterator();
                    while (it.hasNext()) {
                        int iIntValue2 = it.next().intValue();
                        if (!ANRManager.b.getInterestingPids().contains(Integer.valueOf(iIntValue2))) {
                            ANRManager.b.getInterestingPids().add(Integer.valueOf(iIntValue2));
                        }
                    }
                    String str2 = anrDumpRecord.mAnnotation;
                    StringBuilder sb = anrDumpRecord.mInfo;
                    sb.setLength(0);
                    sb.append("ANR in ").append(anrDumpRecord.mProcessName);
                    if (anrDumpRecord.mShortComponentName != null) {
                        sb.append(" (").append(anrDumpRecord.mShortComponentName).append(")");
                    }
                    sb.append(", time=").append(anrDumpRecord.mAnrTime);
                    sb.append("\n");
                    if (str2 != null) {
                        sb.append("Reason: ").append(str2).append("\n");
                    }
                    if (anrDumpRecord.mParentAppPid != -1 && anrDumpRecord.mParentAppPid != anrDumpRecord.mAppPid) {
                        sb.append("Parent: ").append(anrDumpRecord.mParentShortComponentName).append("\n");
                    }
                    ProcessCpuTracker processCpuTracker = new ProcessCpuTracker(true);
                    if (a(anrDumpRecord)) {
                        File fileDumpStackTraces = ANRManager.b.dumpStackTraces(true, arrayList, processCpuTracker, sparseArray, ANRManager.NATIVE_STACKS_OF_INTEREST);
                        if (a(anrDumpRecord)) {
                            if (ANRManager.b.getMonitorCpuUsage()) {
                                synchronized (ANRManager.b.getProcessCpuTracker()) {
                                    str = ANRManager.this.getAndroidTime() + ANRManager.b.getProcessCpuTracker().printCurrentState(anrDumpRecord.mAnrTime);
                                    anrDumpRecord.mCpuInfo += str;
                                }
                                ANRManager.b.updateCpuStatsNow();
                                sb.append(processCpuTracker.printCurrentLoad());
                                sb.append(str);
                            }
                            Slog.e("ANRManager", sb.toString());
                            if (a(anrDumpRecord)) {
                                if (fileDumpStackTraces == null) {
                                    Process.sendSignal(i, 3);
                                }
                                anrDumpRecord.mIsCompleted = true;
                            }
                        }
                    }
                }
            }
        }
    }

    public Boolean isProcDoCoredump(int i) {
        ExceptionLog exceptionLog = null;
        try {
            if (SystemProperties.get("ro.have_aee_feature").equals("1")) {
                exceptionLog = new ExceptionLog();
            }
        } catch (Exception e) {
        }
        if (exceptionLog != null && exceptionLog.isNativeException(i)) {
            Slog.i("ANRManager", "[coredump] Process " + i + " is doing coredump");
            return true;
        }
        return false;
    }

    private Boolean a() {
        ExceptionLog exceptionLog = null;
        try {
            if (SystemProperties.get("ro.have_aee_feature").equals("1")) {
                exceptionLog = new ExceptionLog();
            }
        } catch (Exception e) {
            Slog.d("ANRManager", "AEE is disabled or failed to allocate AEE object");
        }
        if (exceptionLog != null && exceptionLog.isException()) {
            return true;
        }
        return false;
    }

    public void informMessageDump(String str, int i) {
        if (mMessageMap.containsKey(Integer.valueOf(i))) {
            String str2 = mMessageMap.get(Integer.valueOf(i));
            if (str2.length() > 50000) {
                str2 = "";
            }
            mMessageMap.put(Integer.valueOf(i), str2 + str);
        } else {
            if (mMessageMap.size() > 5) {
                mMessageMap.clear();
            }
            mMessageMap.put(Integer.valueOf(i), str);
        }
        Slog.i("ANRManager", "informMessageDump pid= " + i);
    }

    public static int enableANRDebuggingMechanism() {
        switch (AnrOption) {
        }
        return 2;
    }

    public boolean isJavaProcess(int i) {
        if (i <= 0) {
            return false;
        }
        if (mZygotePids == null) {
            mZygotePids = Process.getPidsForCommands(new String[]{"zygote64", "zygote"});
        }
        if (mZygotePids != null) {
            int parentPid = Process.getParentPid(i);
            for (int i2 : mZygotePids) {
                if (parentPid == i2) {
                    return true;
                }
            }
        }
        Slog.i("ANRManager", "pid: " + i + " is not a Java process");
        return false;
    }

    public void writeEvent(int i) {
        switch (i) {
            case EVENT_BOOT_COMPLETED:
                this.g = SystemClock.uptimeMillis();
                break;
        }
    }

    public boolean isAnrDeferrable() {
        if (enableANRDebuggingMechanism() == 0) {
            return false;
        }
        if ("dexopt".equals(SystemProperties.get("anr.autotest"))) {
            Slog.d("ANRManager", "We are doing TestDexOptSkipANR; return true in this case");
            return true;
        }
        if ("enable".equals(SystemProperties.get("anr.autotest"))) {
            Slog.d("ANRManager", "Do Auto Test, don't skip ANR");
            return false;
        }
        long jUptimeMillis = SystemClock.uptimeMillis();
        if (!IS_USER_BUILD) {
            if (this.g != 0) {
                if (jUptimeMillis - this.g >= 30000) {
                    if (a().booleanValue()) {
                        Slog.d("ANRManager", "isAnrDeferrable(): true since exception");
                        return true;
                    }
                    float totalCpuPercent = d.getTotalCpuPercent();
                    updateProcessStats();
                    float totalCpuPercent2 = d.getTotalCpuPercent();
                    if (totalCpuPercent > 90.0f && totalCpuPercent2 > 90.0f) {
                        if (this.h == 0) {
                            this.h = jUptimeMillis;
                            Slog.d("ANRManager", "isAnrDeferrable(): true since CpuUsage = " + totalCpuPercent2 + ", mCpuDeferred = " + this.h);
                            return true;
                        }
                        if (!(jUptimeMillis - this.h >= 8000)) {
                            Slog.d("ANRManager", "isAnrDeferrable(): true since CpuUsage = " + totalCpuPercent2 + ", mCpuDeferred = " + this.h + ", now = " + jUptimeMillis);
                            return true;
                        }
                    }
                    this.h = 0L;
                }
            }
            Slog.d("ANRManager", "isAnrDeferrable(): true since mEventBootCompleted = " + this.g + " now = " + jUptimeMillis);
            return true;
        }
        return false;
    }

    public boolean isANRFlowSkipped(int i, String str, String str2, boolean z, boolean z2, boolean z3) {
        if (this.i == -1) {
            this.i = SystemProperties.getInt("persist.dbg.anrflow", 0);
        }
        Slog.d("ANRManager", "isANRFlowSkipped() AnrFlow = " + this.i);
        switch (this.i) {
            case 1:
                Slog.i("ANRManager", "Skipping ANR flow: " + i + " " + str + " " + str2);
                break;
            case 2:
                if (i != Process.myPid()) {
                    Slog.i("ANRManager", "Skipping ANR flow: " + i + " " + str + " " + str2);
                    if (z) {
                        Slog.i("ANRManager", "During shutdown skipping ANR: " + i + " " + str + " " + str2);
                    } else if (z2) {
                        Slog.i("ANRManager", "Skipping duplicate ANR: " + i + " " + str + " " + str2);
                    } else if (z3) {
                        Slog.i("ANRManager", "Crashing app skipping ANR: " + i + " " + str + " " + str2);
                    } else {
                        Slog.w("ANRManager", "Kill process (" + i + ") due to ANR");
                        Process.killProcess(i);
                    }
                }
                break;
        }
        return false;
    }

    public void notifyLightWeightANR(int i, String str, int i2) {
        if (2 != enableANRDebuggingMechanism()) {
            return;
        }
        switch (i2) {
            case START_MONITOR_KEYDISPATCHING_TIMEOUT_MSG:
                this.mAnrHandler.sendMessageAtTime(this.mAnrHandler.obtainMessage(START_MONITOR_KEYDISPATCHING_TIMEOUT_MSG, i, 0), SystemClock.uptimeMillis() + DataShapingUtils.CLOSING_DELAY_BUFFER_FOR_MUSIC);
                break;
            case REMOVE_KEYDISPATCHING_TIMEOUT_MSG:
                if (this.mAnrHandler.hasMessages(START_MONITOR_KEYDISPATCHING_TIMEOUT_MSG)) {
                    this.mAnrHandler.removeMessages(START_MONITOR_KEYDISPATCHING_TIMEOUT_MSG);
                }
                break;
        }
    }

    public void updateProcessStats() {
        synchronized (d) {
            long jUptimeMillis = SystemClock.uptimeMillis();
            if (!(jUptimeMillis - this.e.get() <= 2500)) {
                this.e.set(jUptimeMillis);
                d.update();
            }
        }
    }

    public String getProcessState() {
        String strPrintCurrentState;
        synchronized (d) {
            Slog.i("ANRManager", "getProcessState");
            strPrintCurrentState = d.printCurrentState(SystemClock.uptimeMillis());
        }
        return strPrintCurrentState;
    }

    public String getAndroidTime() {
        return "Android time :[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SS").format(new Date(System.currentTimeMillis())) + "] [" + new Formatter().format("%.3f", Float.valueOf(SystemClock.uptimeMillis() / 1000.0f)) + "]\n";
    }

    public void registerDumpNBTReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.ACTION_ADD_NBT_DUMP_PID");
        intentFilter.addAction("android.intent.action.ACTION_REMOVE_NBT_DUMP_PID");
        this.f.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String stringExtra = intent.getStringExtra("NBT_DUMP_PROCESS");
                if (stringExtra != null) {
                    String[] strArr = {""};
                    strArr[0] = stringExtra;
                    int[] pidsForCommands = Process.getPidsForCommands(strArr);
                    if (pidsForCommands.length > 0) {
                        if ("android.intent.action.ACTION_ADD_NBT_DUMP_PID".equals(intent.getAction())) {
                            ANRManager.this.checkNBTDumpPid(pidsForCommands[0]);
                            return;
                        } else {
                            if ("android.intent.action.ACTION_REMOVE_NBT_DUMP_PID".equals(intent.getAction())) {
                                ANRManager.this.removeNBTDumpPid(pidsForCommands[0]);
                                return;
                            }
                            return;
                        }
                    }
                    Slog.i("ANRManager", "No process corresponds to " + stringExtra);
                    return;
                }
                Slog.i("ANRManager", "Process name is null");
            }
        }, intentFilter);
    }

    public void checkNBTDumpPid(int i) {
        if (!isJavaProcess(i) && !additionNBTList.contains(Integer.valueOf(i))) {
            additionNBTList.add(Integer.valueOf(i));
            Slog.i("ANRManager", "Add NBTDumpPid pid=" + i);
        }
    }

    public void removeNBTDumpPid(int i) {
        if (additionNBTList.contains(Integer.valueOf(i))) {
            additionNBTList.remove(additionNBTList.indexOf(Integer.valueOf(i)));
            Slog.i("ANRManager", "Remove NBTDumpPid pid=" + i);
        }
    }

    public File createFile(String str) {
        File file = new File(str);
        if (file == null || !file.exists()) {
            Log.d("ANRManager", "file isn't exist");
            return null;
        }
        return file;
    }

    public boolean copyFile(File file, File file2) {
        try {
            if (!file.exists()) {
                return false;
            }
            if (!file2.exists()) {
                file2.createNewFile();
                FileUtils.setPermissions(file2.getPath(), 438, -1, -1);
            }
            FileInputStream fileInputStream = new FileInputStream(file);
            try {
                return copyToFile(fileInputStream, file2);
            } finally {
                fileInputStream.close();
            }
        } catch (IOException e) {
            Log.d("ANRManager", "createFile fail");
            return false;
        }
    }

    public boolean copyToFile(InputStream inputStream, File file) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file, true);
            try {
                byte[] bArr = new byte[4096];
                while (true) {
                    int i = inputStream.read(bArr);
                    if (i < 0) {
                        break;
                    }
                    fileOutputStream.write(bArr, 0, i);
                }
                fileOutputStream.flush();
                try {
                    fileOutputStream.getFD().sync();
                } catch (IOException e) {
                    Log.d("ANRManager", "copyToFile: getFD fail");
                }
                fileOutputStream.close();
                return true;
            } catch (Throwable th) {
                fileOutputStream.flush();
                try {
                    fileOutputStream.getFD().sync();
                } catch (IOException e2) {
                    Log.d("ANRManager", "copyToFile: getFD fail");
                }
                fileOutputStream.close();
                throw th;
            }
        } catch (IOException e3) {
            Log.d("ANRManager", "copyToFile fail");
            return false;
        }
    }

    public void stringToFile(String str, String str2) throws IOException {
        FileWriter fileWriter = new FileWriter(str, true);
        try {
            fileWriter.write(str2);
        } finally {
            fileWriter.close();
        }
    }

    public class BinderDumpThread extends Thread {
        private int mPid;

        public BinderDumpThread(int i) {
            this.mPid = i;
        }

        @Override
        public void run() {
            ANRManager.this.dumpBinderInfo(this.mPid);
        }
    }

    public void dumpBinderInfo(int i) {
        try {
            File file = new File("/data/anr/binderinfo");
            if (file.exists()) {
                if (!file.delete()) {
                    Log.d("ANRManager", "dumpBinderInfo fail due to file likely to be locked by others");
                    return;
                } else {
                    if (!file.createNewFile()) {
                        Log.d("ANRManager", "dumpBinderInfo fail due to file cannot be created");
                        return;
                    }
                    FileUtils.setPermissions(file.getPath(), 438, -1, -1);
                }
            }
            File fileCreateFile = createFile("/sys/kernel/debug/binder/failed_transaction_log");
            if (fileCreateFile != null) {
                stringToFile("/data/anr/binderinfo", "------ BINDER FAILED TRANSACTION LOG ------\n");
                copyFile(fileCreateFile, file);
            }
            File fileCreateFile2 = createFile("sys/kernel/debug/binder/timeout_log");
            if (fileCreateFile2 != null) {
                stringToFile("/data/anr/binderinfo", "------ BINDER TIMEOUT LOG ------\n");
                copyFile(fileCreateFile2, file);
            }
            File fileCreateFile3 = createFile("/sys/kernel/debug/binder/transaction_log");
            if (fileCreateFile3 != null) {
                stringToFile("/data/anr/binderinfo", "------ BINDER TRANSACTION LOG ------\n");
                copyFile(fileCreateFile3, file);
            }
            File fileCreateFile4 = createFile("/sys/kernel/debug/binder/transactions");
            if (fileCreateFile4 != null) {
                stringToFile("/data/anr/binderinfo", "------ BINDER TRANSACTIONS ------\n");
                copyFile(fileCreateFile4, file);
            }
            File fileCreateFile5 = createFile("/sys/kernel/debug/binder/stats");
            if (fileCreateFile5 != null) {
                stringToFile("/data/anr/binderinfo", "------ BINDER STATS ------\n");
                copyFile(fileCreateFile5, file);
            }
            File file2 = new File("/sys/kernel/debug/binder/proc/" + Integer.toString(i));
            if (file2 != null) {
                stringToFile("/data/anr/binderinfo", "------ BINDER PROCESS STATE: $i ------\n");
                copyFile(file2, file);
            }
        } catch (IOException e) {
            Log.d("ANRManager", "dumpBinderInfo fail");
        }
    }

    public void enableBinderLog(boolean z) throws Throwable {
        Slog.i("ANRManager", "enableBinderLog: " + z);
        a("/sys/kernel/debug/binder/transaction_log_enable", !z ? "2" : "1");
    }

    public void enableTraceLog(boolean z) throws Throwable {
        Slog.i("ANRManager", "enableTraceLog: " + z);
        a("/sys/kernel/debug/tracing/tracing_on", !z ? "0" : "1");
    }

    public void setZramMonitor(boolean z) throws Throwable {
        Slog.i("ANRManager", "setZramMonitor: " + z);
        a("/sys/module/mlog/parameters/timer_intval", !z ? "6000" : "100");
    }

    public void setZramTag(String str) throws Throwable {
        Slog.i("ANRManager", "setZramTag: " + str);
        a("/sys/module/mlog/parameters/do_mlog", str);
    }

    private void a(String str, String str2) throws Throwable {
        ?? r1;
        FileOutputStream fileOutputStream;
        if (str != null) {
            File file = new File(str);
            StrictMode.ThreadPolicy threadPolicyAllowThreadDiskReads = StrictMode.allowThreadDiskReads();
            StrictMode.allowThreadDiskWrites();
            try {
                try {
                    fileOutputStream = new FileOutputStream(file);
                    try {
                        fileOutputStream.write(str2.getBytes());
                        fileOutputStream.flush();
                        Object obj = fileOutputStream;
                        if (fileOutputStream != null) {
                            try {
                                fileOutputStream.close();
                                obj = fileOutputStream;
                            } catch (IOException e) {
                                Slog.e("ANRManager", "writeStringToFile close error: " + str + " " + e.toString());
                                obj = "ANRManager";
                            }
                        }
                        StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskReads);
                        r1 = obj;
                    } catch (IOException e2) {
                        e = e2;
                        Slog.e("ANRManager", "writeStringToFile error: " + str + " " + e.toString());
                        Object obj2 = fileOutputStream;
                        if (fileOutputStream != null) {
                            try {
                                fileOutputStream.close();
                                obj2 = fileOutputStream;
                            } catch (IOException e3) {
                                Slog.e("ANRManager", "writeStringToFile close error: " + str + " " + e3.toString());
                                obj2 = "ANRManager";
                            }
                        }
                        StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskReads);
                        r1 = obj2;
                    }
                } catch (Throwable th) {
                    th = th;
                    if (r1 != 0) {
                        try {
                            r1.close();
                        } catch (IOException e4) {
                            Slog.e("ANRManager", "writeStringToFile close error: " + str + " " + e4.toString());
                        }
                    }
                    StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskReads);
                    throw th;
                }
            } catch (IOException e5) {
                e = e5;
                fileOutputStream = null;
            } catch (Throwable th2) {
                th = th2;
                r1 = 0;
                if (r1 != 0) {
                }
                StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskReads);
                throw th;
            }
        }
    }
}
