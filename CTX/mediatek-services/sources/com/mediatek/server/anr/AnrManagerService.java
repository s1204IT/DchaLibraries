package com.mediatek.server.anr;

import android.app.ApplicationErrorReport;
import android.app.IActivityController;
import android.app.IApplicationThread;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.FileUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.SELinux;
import android.os.StrictMode;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Slog;
import android.util.SparseArray;
import android.util.StatsLog;
import com.android.internal.os.ProcessCpuTracker;
import com.android.server.Watchdog;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.ActivityRecord;
import com.android.server.am.AppNotRespondingDialog;
import com.android.server.am.ProcessRecord;
import com.mediatek.aee.ExceptionLog;
import com.mediatek.anr.AnrAppManagerImpl;
import com.mediatek.anr.AnrManagerNative;
import com.mediatek.datashaping.DataShapingServiceImpl;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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

public final class AnrManagerService extends AnrManagerNative {
    private static final String ACTIVE_SERVICES = "com.android.server.am.ActiveServices";
    private static final String ACTIVITY_MANAGER = "com.android.server.am.ActivityManagerService";
    private static final String ACTIVITY_RECORD = "com.android.server.am.ActivityRecord";
    private static final long ANR_BOOT_DEFER_TIME = 30000;
    private static final long ANR_CPU_DEFER_TIME = 8000;
    private static final float ANR_CPU_THRESHOLD = 90.0f;
    private static final String APP_ERRORS = "com.android.server.am.AppErrors";
    private static final String BATTERY_STATS = "com.android.server.am.BatteryStatsService";
    private static final int DISABLE_ALL_ANR_MECHANISM = 0;
    private static final int DISABLE_PARTIAL_ANR_MECHANISM = 1;
    private static final int ENABLE_ALL_ANR_MECHANISM = 2;
    private static final int ENABLE_ANR_DUMP_FOR_3RD_APP = 1;
    private static final int EVENT_BOOT_COMPLETED = 9001;
    private static final int INVALID_ANR_FLOW = -1;
    private static final int INVALID_ANR_OPTION = -1;
    private static final boolean IS_USER_BUILD;
    private static final boolean IS_USER_LOAD;
    private static final int MAX_MTK_TRACE_COUNT = 10;
    private static final int MESSAGE_MAP_BUFFER_COUNT_MAX = 5;
    private static final int MESSAGE_MAP_BUFFER_SIZE_MAX = 50000;
    private static final long MONITOR_CPU_MIN_TIME = 2500;
    private static String[] NATIVE_STACKS_OF_INTEREST = null;
    private static final int NORMAL_ANR_FLOW = 0;
    private static final String PROCESS_RECORD = "com.android.server.am.ProcessRecord";
    private static final int REMOVE_KEYDISPATCHING_TIMEOUT_MSG = 1005;
    private static final int SERVICE_TIMEOUT = 20000;
    private static final int SKIP_ANR_FLOW = 1;
    private static final int SKIP_ANR_FLOW_AND_KILL = 2;
    private static final int START_ANR_DUMP_MSG = 1003;
    private static final int START_MONITOR_BROADCAST_TIMEOUT_MSG = 1001;
    private static final int START_MONITOR_KEYDISPATCHING_TIMEOUT_MSG = 1004;
    private static final int START_MONITOR_SERVICE_TIMEOUT_MSG = 1002;
    private static final String TAG = "AnrManager";
    private static Object lock;
    private static final ProcessCpuTracker mAnrProcessStats;
    private static final Object mDumpStackTraces;
    private static ConcurrentHashMap<Integer, String> mMessageMap;
    private static int[] mZygotePids;
    private static boolean sEnhanceEnable = false;
    private static AnrManagerService sInstance;
    private int mAmsPid;
    private AnrDumpManager mAnrDumpManager;
    private AnrMonitorHandler mAnrHandler;
    private ActivityManagerService mService;
    private final AtomicLong mLastCpuUpdateTime = new AtomicLong(0);
    private long mEventBootCompleted = 0;
    private long mCpuDeferred = 0;
    private int mAnrFlow = -1;
    private int mAnrOption = -1;
    private ExceptionLog exceptionLog = null;
    private File mTracesFile = null;
    private Class<?> mProcessRecord = getProcessRecord();
    private Class<?> mAMS = getActivityManagerService();
    private Method mKill = getProcessRecordMethod("kill", new Class[]{String.class, Boolean.TYPE});
    private Method mUpdateCpuStatsNow = getAMSMethod("updateCpuStatsNow");
    private Method mNoteProcessANR = getBatteryStatsServiceMethod("noteProcessAnr", new Class[]{String.class, Integer.TYPE});
    private Method mScheduleServiceTimeoutLocked = getActiveServicesMethod("scheduleServiceTimeoutLocked", new Class[]{ProcessRecord.class});
    private Method mMakeAppNotRespondingLocked = getAppErrorsMethod("makeAppNotRespondingLocked", new Class[]{ProcessRecord.class, String.class, String.class, String.class});
    private Field mPidField = getProcessRecordField("pid");
    private Field mProcessNameField = getProcessRecordField("processName");
    private Field mThreadField = getProcessRecordField("thread");
    private Field mNotRespondingField = getProcessRecordField("notResponding");
    private Field mCrashingField = getProcessRecordField("crashing");
    private Field mUserIdField = getProcessRecordField("userId");
    private Field mUidField = getProcessRecordField("uid");
    private Field mInfoField = getProcessRecordField("info");
    private Field mPersistentField = getProcessRecordField("persistent");
    private Field mParentPidField = getProcessRecordField("pid");
    private Field mParentAppField = getActivityRecordField("app");
    private Field mShortCopNameField = getActivityRecordField("shortComponentName");
    private Field mParentShortCopNameField = getActivityRecordField("shortComponentName");
    private Field mShuttingDownField = getAMSField("mShuttingDown");
    private Field mControllerField = getAMSField("mController");
    private Field mLruProcessesField = getAMSField("mLruProcesses");
    private Field mProcessCpuTrackerField = getAMSField("mProcessCpuTracker");
    private Field mMonitorCpuUsageField = getAMSField("MONITOR_CPU_USAGE");
    private Field mShowNotRespondingUiMsgField = getAMSField("SHOW_NOT_RESPONDING_UI_MSG");
    private Field mBatteryStatsServiceField = getAMSField("mBatteryStatsService");
    private Field mActiveServicesField = getAMSField("mServices");
    private Field mUiHandlerField = getAMSField("mUiHandler");
    private Field mAppErrorsField = getAMSField("mAppErrors");

    static {
        IS_USER_BUILD = "user".equals(Build.TYPE) || "userdebug".equals(Build.TYPE);
        IS_USER_LOAD = "user".equals(Build.TYPE);
        mZygotePids = null;
        mDumpStackTraces = new Object();
        NATIVE_STACKS_OF_INTEREST = new String[]{"/system/bin/netd", "/system/bin/audioserver", "/system/bin/cameraserver", "/system/bin/drmserver", "/system/bin/mediadrmserver", "/system/bin/mediaserver", "/system/bin/sdcard", "/system/bin/surfaceflinger", "vendor/bin/hw/camerahalserver", "media.extractor", "media.codec", "com.android.bluetooth"};
        mAnrProcessStats = new ProcessCpuTracker(false);
        mMessageMap = new ConcurrentHashMap<>();
        lock = new Object();
        sInstance = null;
    }

    private Class<?> getProcessRecord() {
        try {
            return Class.forName(PROCESS_RECORD);
        } catch (Exception e) {
            return null;
        }
    }

    private Class<?> getActivityManagerService() {
        try {
            return Class.forName(ACTIVITY_MANAGER);
        } catch (Exception e) {
            return null;
        }
    }

    private Method getProcessRecordMethod(String str, Class[] clsArr) {
        try {
            Method declaredMethod = this.mProcessRecord.getDeclaredMethod(str, clsArr);
            declaredMethod.setAccessible(true);
            return declaredMethod;
        } catch (Exception e) {
            Slog.w(TAG, "getProcessRecordMethod Exception: " + e);
            return null;
        }
    }

    private Method getAMSMethod(String str) {
        try {
            Method declaredMethod = this.mAMS.getDeclaredMethod(str, new Class[0]);
            declaredMethod.setAccessible(true);
            return declaredMethod;
        } catch (Exception e) {
            return null;
        }
    }

    private Method getBatteryStatsServiceMethod(String str, Class[] clsArr) {
        try {
            Method declaredMethod = Class.forName(BATTERY_STATS).getDeclaredMethod(str, clsArr);
            declaredMethod.setAccessible(true);
            return declaredMethod;
        } catch (Exception e) {
            return null;
        }
    }

    private Method getActiveServicesMethod(String str, Class[] clsArr) {
        try {
            return Class.forName(ACTIVE_SERVICES).getDeclaredMethod(str, clsArr);
        } catch (Exception e) {
            return null;
        }
    }

    private Method getAppErrorsMethod(String str, Class[] clsArr) {
        try {
            Method declaredMethod = Class.forName(APP_ERRORS).getDeclaredMethod(str, clsArr);
            declaredMethod.setAccessible(true);
            return declaredMethod;
        } catch (Exception e) {
            return null;
        }
    }

    private Field getProcessRecordField(String str) {
        try {
            Field declaredField = this.mProcessRecord.getDeclaredField(str);
            declaredField.setAccessible(true);
            return declaredField;
        } catch (Exception e) {
            return null;
        }
    }

    private Field getActivityRecordField(String str) {
        try {
            Field declaredField = Class.forName(ACTIVITY_RECORD).getDeclaredField(str);
            declaredField.setAccessible(true);
            return declaredField;
        } catch (Exception e) {
            return null;
        }
    }

    private Field getAMSField(String str) {
        try {
            Field declaredField = this.mAMS.getDeclaredField(str);
            declaredField.setAccessible(true);
            return declaredField;
        } catch (Exception e) {
            return null;
        }
    }

    public static AnrManagerService getInstance() {
        if (sInstance == null) {
            synchronized (lock) {
                if (sInstance == null) {
                    sInstance = new AnrManagerService();
                }
            }
        }
        return sInstance;
    }

    public void startAnrManagerService(int i) {
        Slog.i(TAG, "startAnrManagerService");
        this.mAmsPid = i;
        HandlerThread handlerThread = new HandlerThread("AnrMonitorThread");
        handlerThread.start();
        this.mAnrHandler = new AnrMonitorHandler(handlerThread.getLooper());
        this.mAnrDumpManager = new AnrDumpManager();
        mAnrProcessStats.init();
        prepareStackTraceFile(SystemProperties.get("dalvik.vm.mtk-stack-trace-file", (String) null));
        prepareStackTraceFile(SystemProperties.get("dalvik.vm.stack-trace-file", (String) null));
        File parentFile = new File(SystemProperties.get("dalvik.vm.stack-trace-file", (String) null)).getParentFile();
        if (parentFile != null && !SELinux.restoreconRecursive(parentFile)) {
            Slog.i(TAG, "startAnrManagerService SELinux.restoreconRecursive fail dir = " + parentFile.toString());
        }
        if (SystemProperties.get("ro.vendor.have_aee_feature").equals("1")) {
            this.exceptionLog = new ExceptionLog();
        }
        if (!IS_USER_BUILD) {
            Looper looperMyLooper = Looper.myLooper();
            AnrAppManagerImpl.getInstance();
            looperMyLooper.setMessageLogging(AnrAppManagerImpl.newMessageLogger(false, Thread.currentThread().getName()));
        }
    }

    public void sendBroadcastMonitorMessage(long j, long j2) {
        if (2 == checkAnrDebugMechanism()) {
            this.mAnrHandler.sendMessageAtTime(this.mAnrHandler.obtainMessage(START_MONITOR_BROADCAST_TIMEOUT_MSG), j - (j2 / 2));
        }
    }

    public void removeBroadcastMonitorMessage() {
        if (2 == checkAnrDebugMechanism()) {
            this.mAnrHandler.removeMessages(START_MONITOR_BROADCAST_TIMEOUT_MSG);
        }
    }

    public void sendServiceMonitorMessage() {
        long jUptimeMillis = SystemClock.uptimeMillis();
        if (2 == checkAnrDebugMechanism()) {
            this.mAnrHandler.sendMessageAtTime(this.mAnrHandler.obtainMessage(START_MONITOR_SERVICE_TIMEOUT_MSG), jUptimeMillis + 13333);
        }
    }

    public void removeServiceMonitorMessage() {
        if (2 == checkAnrDebugMechanism()) {
            this.mAnrHandler.removeMessages(START_MONITOR_SERVICE_TIMEOUT_MSG);
        }
    }

    public boolean startAnrDump(ActivityManagerService activityManagerService, ProcessRecord processRecord, ActivityRecord activityRecord, ActivityRecord activityRecord2, boolean z, String str, boolean z2) throws Exception {
        String str2;
        ProcessRecord processRecord2;
        boolean z3;
        int i;
        IActivityController iActivityController;
        ApplicationInfo applicationInfo;
        String str3;
        int i2;
        int i3;
        AnrDumpRecord anrDumpRecord;
        String str4;
        boolean z4;
        String str5;
        ?? r1;
        String str6;
        String string;
        boolean z5;
        int i4;
        AnrDumpRecord anrDumpRecord2;
        Slog.i(TAG, "startAnrDump");
        if (checkAnrDebugMechanism() == 0) {
            return false;
        }
        this.mService = activityManagerService;
        long jUptimeMillis = SystemClock.uptimeMillis();
        int iIntValue = ((Integer) this.mPidField.get(processRecord)).intValue();
        String str7 = (String) this.mProcessNameField.get(processRecord);
        ApplicationInfo applicationInfo2 = (ApplicationInfo) this.mInfoField.get(processRecord);
        ((Boolean) this.mShuttingDownField.get(this.mService)).booleanValue();
        IActivityController iActivityController2 = (IActivityController) this.mControllerField.get(this.mService);
        if (activityRecord2 != null) {
            ProcessRecord processRecord3 = (ProcessRecord) this.mParentAppField.get(activityRecord2);
            str2 = (String) this.mParentShortCopNameField.get(activityRecord2);
            processRecord2 = processRecord3;
        } else {
            str2 = "";
            processRecord2 = null;
        }
        int iIntValue2 = processRecord2 != null ? ((Integer) this.mParentPidField.get(processRecord2)).intValue() : -1;
        String str8 = activityRecord != null ? (String) this.mShortCopNameField.get(activityRecord) : "";
        int i5 = iIntValue2;
        if (isAnrFlowSkipped(iIntValue, str7, str)) {
            return true;
        }
        if (!IS_USER_LOAD) {
            try {
                ((IApplicationThread) this.mThreadField.get(processRecord)).dumpMessage(iIntValue == this.mAmsPid);
            } catch (Exception e) {
                Slog.e(TAG, "Error happens when dumping message history", e);
            }
        }
        synchronized (this.mService) {
            if (!z2) {
                try {
                    z3 = (processRecord.isInterestingToUserLocked() || iIntValue == this.mAmsPid) ? false : true;
                } finally {
                }
            }
        }
        if (needReduceAnrDump(applicationInfo2)) {
            i = 2;
            iActivityController = iActivityController2;
            applicationInfo = applicationInfo2;
            str3 = str7;
            i2 = iIntValue;
            i3 = 0;
            anrDumpRecord = null;
        } else {
            enableTraceLog(false);
            new BinderDumpThread(iIntValue).start();
            if (this.mAnrDumpManager.mDumpList.containsKey(processRecord)) {
                i = 2;
                iActivityController = iActivityController2;
                applicationInfo = applicationInfo2;
                str3 = str7;
                i4 = iIntValue;
                anrDumpRecord2 = null;
            } else {
                i = 2;
                iActivityController = iActivityController2;
                applicationInfo = applicationInfo2;
                str3 = str7;
                i4 = iIntValue;
                AnrDumpRecord anrDumpRecord3 = new AnrDumpRecord(processRecord != null ? iIntValue : -1, false, processRecord != null ? str7 : null, processRecord != null ? processRecord.toString() : null, activityRecord != null ? str8 : null, (activityRecord2 == null || processRecord2 == null) ? -1 : i5, activityRecord2 != null ? str2 : null, str, jUptimeMillis);
                if (2 == checkAnrDebugMechanism()) {
                    updateProcessStats();
                    String str9 = getAndroidTime() + getProcessState() + "\n";
                    anrDumpRecord3.mCpuInfo = str9;
                    Slog.i(TAG, str9.toString());
                }
                this.mAnrDumpManager.startAsyncDump(anrDumpRecord3);
                anrDumpRecord2 = anrDumpRecord3;
            }
            if (anrDumpRecord2 != null) {
                synchronized (anrDumpRecord2) {
                    i3 = 0;
                    this.mAnrDumpManager.dumpAnrDebugInfo(anrDumpRecord2, false);
                }
            } else {
                i3 = 0;
            }
            this.mAnrDumpManager.removeDumpRecord(anrDumpRecord2);
            StringBuilder sb = new StringBuilder();
            sb.append(anrDumpRecord2.mCpuInfo);
            i2 = i4;
            sb.append(mMessageMap.get(Integer.valueOf(i2)));
            anrDumpRecord2.mCpuInfo = sb.toString();
            anrDumpRecord = anrDumpRecord2;
        }
        int i6 = i;
        ApplicationInfo applicationInfo3 = applicationInfo;
        StatsLog.write(79, ((Integer) this.mUidField.get(processRecord)).intValue(), str3, activityRecord == null ? "unknown" : str8, str, applicationInfo3 != null ? applicationInfo3.isInstantApp() ? i6 : 1 : i3, processRecord != null ? processRecord.isInterestingToUserLocked() ? i6 : 1 : i3);
        AnrDumpRecord anrDumpRecord4 = anrDumpRecord;
        this.mService.addErrorToDropBox("anr", processRecord, str3, activityRecord, activityRecord2, str, anrDumpRecord != null ? anrDumpRecord.mCpuInfo : "", this.mTracesFile, (ApplicationErrorReport.CrashInfo) null);
        ?? r12 = iActivityController;
        if (r12 != 0) {
            if (anrDumpRecord4 != null) {
                try {
                    string = anrDumpRecord4.mInfo.toString();
                } catch (RemoteException e2) {
                    str4 = str3;
                    r12 = 1;
                    str5 = null;
                    this.mControllerField.set(this.mService, null);
                    Watchdog.getInstance().setActivityController((IActivityController) null);
                    r1 = r12;
                    synchronized (this.mService) {
                    }
                }
            } else {
                string = "";
            }
            str4 = str3;
            try {
                int iAppNotResponding = r12.appNotResponding(str4, i2, string);
                if (iAppNotResponding != 0) {
                    try {
                        if (iAppNotResponding >= 0 || i2 == this.mAmsPid) {
                            r12 = 1;
                            r12 = 1;
                            z5 = true;
                            synchronized (this.mService) {
                                this.mScheduleServiceTimeoutLocked.invoke(this.mActiveServicesField.get(this.mService), processRecord);
                            }
                        } else {
                            Method method = this.mKill;
                            Object[] objArr = new Object[2];
                            objArr[0] = "anr";
                            z5 = true;
                            objArr[1] = true;
                            method.invoke(processRecord, objArr);
                        }
                        return z5;
                    } catch (RemoteException e3) {
                    }
                } else {
                    z4 = true;
                }
            } catch (RemoteException e4) {
                r12 = 1;
            }
            str5 = null;
            this.mControllerField.set(this.mService, null);
            Watchdog.getInstance().setActivityController((IActivityController) null);
            r1 = r12;
            synchronized (this.mService) {
                Method method2 = this.mNoteProcessANR;
                Object obj = this.mBatteryStatsServiceField.get(this.mService);
                Object[] objArr2 = new Object[2];
                objArr2[0] = str4;
                objArr2[r1] = Integer.valueOf(((Integer) this.mUidField.get(processRecord)).intValue());
                method2.invoke(obj, objArr2);
                if (z3) {
                    Method method3 = this.mKill;
                    Object[] objArr3 = new Object[2];
                    objArr3[0] = "bg anr";
                    objArr3[r1] = Boolean.valueOf((boolean) r1);
                    method3.invoke(processRecord, objArr3);
                    return r1;
                }
                Method method4 = this.mMakeAppNotRespondingLocked;
                Object obj2 = this.mAppErrorsField.get(this.mService);
                Object[] objArr4 = new Object[4];
                objArr4[0] = processRecord;
                if (activityRecord == null) {
                    str8 = str5;
                }
                objArr4[r1] = str8;
                if (str != null) {
                    str6 = "ANR " + str;
                } else {
                    str6 = "ANR";
                }
                objArr4[2] = str6;
                objArr4[3] = anrDumpRecord4 != null ? anrDumpRecord4.mInfo.toString() : "";
                method4.invoke(obj2, objArr4);
                Message messageObtain = Message.obtain();
                messageObtain.what = ((Integer) this.mShowNotRespondingUiMsgField.get(this.mAMS)).intValue();
                messageObtain.obj = new AppNotRespondingDialog.Data(processRecord, activityRecord, z);
                ((Handler) this.mUiHandlerField.get(this.mService)).sendMessage(messageObtain);
                return r1;
            }
        }
        str4 = str3;
        z4 = true;
        str5 = null;
        r1 = z4;
        synchronized (this.mService) {
        }
    }

    public class AnrMonitorHandler extends Handler {
        public AnrMonitorHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case AnrManagerService.START_MONITOR_BROADCAST_TIMEOUT_MSG:
                case AnrManagerService.START_MONITOR_SERVICE_TIMEOUT_MSG:
                case AnrManagerService.START_MONITOR_KEYDISPATCHING_TIMEOUT_MSG:
                    AnrManagerService.this.updateProcessStats();
                    break;
                case AnrManagerService.START_ANR_DUMP_MSG:
                    AnrDumpRecord anrDumpRecord = (AnrDumpRecord) message.obj;
                    Slog.i(AnrManagerService.TAG, "START_ANR_DUMP_MSG: " + anrDumpRecord);
                    AnrManagerService.this.mAnrDumpManager.dumpAnrDebugInfo(anrDumpRecord, true);
                    break;
            }
        }
    }

    protected static final class BinderWatchdog {
        private static final int MAX_LINES = 64;
        private static final int MAX_TIMEOUT_PIDS = 5;

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
            if (i <= 0) {
                return null;
            }
            ArrayList<BinderInfo> timeoutBinderListFromFile = readTimeoutBinderListFromFile();
            int i3 = 0;
            ArrayList<Integer> arrayList = new ArrayList<>();
            for (BinderInfo binderInfo = getBinderInfo(i, i2, timeoutBinderListFromFile); binderInfo != null; binderInfo = getBinderInfo(binderInfo.mDstPid, binderInfo.mDstTid, timeoutBinderListFromFile)) {
                if (binderInfo.mDstPid > 0) {
                    i3++;
                    if (!arrayList.contains(Integer.valueOf(binderInfo.mDstPid))) {
                        Slog.i(AnrManagerService.TAG, "getTimeoutBinderPidList pid added: " + binderInfo.mDstPid + " " + binderInfo.mText);
                        arrayList.add(Integer.valueOf(binderInfo.mDstPid));
                    } else {
                        Slog.i(AnrManagerService.TAG, "getTimeoutBinderPidList pid existed: " + binderInfo.mDstPid + " " + binderInfo.mText);
                    }
                    if (i3 >= MAX_TIMEOUT_PIDS) {
                        break;
                    }
                }
            }
            if (arrayList.size() == 0) {
                return getTimeoutBinderFromPid(i, timeoutBinderListFromFile);
            }
            return arrayList;
        }

        public static final ArrayList<Integer> getTimeoutBinderFromPid(int i, ArrayList<BinderInfo> arrayList) {
            if (i <= 0 || arrayList == null) {
                return null;
            }
            Slog.i(AnrManagerService.TAG, "getTimeoutBinderFromPid " + i + " list size: " + arrayList.size());
            int i2 = 0;
            ArrayList<Integer> arrayList2 = new ArrayList<>();
            for (BinderInfo binderInfo : arrayList) {
                if (binderInfo != null && binderInfo.mSrcPid == i) {
                    i2++;
                    if (!arrayList2.contains(Integer.valueOf(binderInfo.mDstPid))) {
                        Slog.i(AnrManagerService.TAG, "getTimeoutBinderFromPid pid added: " + binderInfo.mDstPid + " " + binderInfo.mText);
                        arrayList2.add(Integer.valueOf(binderInfo.mDstPid));
                    } else {
                        Slog.i(AnrManagerService.TAG, "getTimeoutBinderFromPid pid existed: " + binderInfo.mDstPid + " " + binderInfo.mText);
                    }
                    if (i2 >= MAX_TIMEOUT_PIDS) {
                        break;
                    }
                }
            }
            return arrayList2;
        }

        private static BinderInfo getBinderInfo(int i, int i2, ArrayList<BinderInfo> arrayList) {
            if (arrayList == null || arrayList.size() == 0 || i == 0) {
                return null;
            }
            arrayList.size();
            for (BinderInfo binderInfo : arrayList) {
                if (binderInfo.mSrcPid == i && binderInfo.mSrcTid == i2) {
                    Slog.i(AnrManagerService.TAG, "Timeout binder pid found: " + binderInfo.mDstPid + " " + binderInfo.mText);
                    return binderInfo;
                }
            }
            return null;
        }

        private static final ArrayList<BinderInfo> readTimeoutBinderListFromFile() {
            ArrayList<BinderInfo> arrayList;
            ArrayList<BinderInfo> arrayList2;
            ArrayList<BinderInfo> arrayList3;
            ArrayList<BinderInfo> arrayList4;
            BufferedReader bufferedReader = null;
            try {
                try {
                    File file = new File("/sys/kernel/debug/binder/timeout_log");
                    if (!file.exists()) {
                        return null;
                    }
                    BufferedReader bufferedReader2 = new BufferedReader(new FileReader(file));
                    try {
                        arrayList = new ArrayList<>();
                        try {
                            do {
                                try {
                                    String line = bufferedReader2.readLine();
                                    if (line != null) {
                                        BinderInfo binderInfo = new BinderInfo(line);
                                        if (binderInfo.mSrcPid > 0) {
                                            arrayList.add(binderInfo);
                                        }
                                    }
                                    break;
                                } catch (FileNotFoundException e) {
                                    e = e;
                                    bufferedReader = bufferedReader2;
                                    arrayList3 = arrayList;
                                } catch (IOException e2) {
                                    e = e2;
                                    bufferedReader = bufferedReader2;
                                    arrayList2 = arrayList;
                                    Slog.e(AnrManagerService.TAG, "IOException when gettting Binder. ", e);
                                    if (bufferedReader != null) {
                                        try {
                                            bufferedReader.close();
                                        } catch (IOException e3) {
                                            Slog.e(AnrManagerService.TAG, "IOException when close buffer reader:", e3);
                                        }
                                    }
                                    return arrayList2;
                                } catch (Throwable th) {
                                    bufferedReader = bufferedReader2;
                                    if (bufferedReader != null) {
                                        try {
                                            bufferedReader.close();
                                        } catch (IOException e4) {
                                            Slog.e(AnrManagerService.TAG, "IOException when close buffer reader:", e4);
                                        }
                                    }
                                    return arrayList;
                                }
                            } while (arrayList.size() <= MAX_LINES);
                            break;
                            bufferedReader2.close();
                        } catch (IOException e5) {
                            Slog.e(AnrManagerService.TAG, "IOException when close buffer reader:", e5);
                        }
                        return arrayList;
                    } catch (FileNotFoundException e6) {
                        e = e6;
                        arrayList3 = null;
                        bufferedReader = bufferedReader2;
                    } catch (IOException e7) {
                        e = e7;
                        arrayList2 = null;
                        bufferedReader = bufferedReader2;
                    } catch (Throwable th2) {
                        arrayList = null;
                    }
                } catch (Throwable th3) {
                    arrayList = arrayList4;
                }
            } catch (FileNotFoundException e8) {
                e = e8;
                arrayList3 = null;
            } catch (IOException e9) {
                e = e9;
                arrayList2 = null;
            } catch (Throwable th4) {
                arrayList = null;
            }
            Slog.e(AnrManagerService.TAG, "FileNotFoundException", e);
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e10) {
                    Slog.e(AnrManagerService.TAG, "IOException when close buffer reader:", e10);
                }
            }
            return arrayList3;
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

        private static final void readTransactionInfoFromFile(int i, ArrayList<Integer> arrayList) throws Throwable {
            IOException e;
            FileNotFoundException e2;
            Throwable th;
            BufferedReader bufferedReader;
            File file;
            Pattern patternCompile = Pattern.compile("(\\S+.+transaction).+from\\s+(\\d+):(\\d+)\\s+to\\s+(\\d+):(\\d+).+start\\s+(\\d+\\.+\\d+).+android\\s+(\\d+-\\d+-\\d+\\s+\\d+:\\d+:\\d+\\.\\d+)");
            ArrayList arrayList2 = new ArrayList();
            ArrayList arrayList3 = new ArrayList();
            BufferedReader bufferedReader2 = null;
            try {
                try {
                    try {
                        file = new File("/sys/kernel/debug/binder/proc/" + Integer.toString(i));
                    } catch (Throwable th2) {
                        th = th2;
                        bufferedReader = null;
                    }
                } catch (FileNotFoundException e3) {
                    e2 = e3;
                } catch (IOException e4) {
                    e = e4;
                }
                if (!file.exists()) {
                    Slog.d(AnrManagerService.TAG, "Filepath isn't exist");
                    return;
                }
                bufferedReader = new BufferedReader(new FileReader(file));
                while (true) {
                    try {
                        String line = bufferedReader.readLine();
                        if (line != null) {
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
                                    transactionInfo.rcv_tid = matcher.group(MAX_TIMEOUT_PIDS);
                                    transactionInfo.ktime = matcher.group(6);
                                    transactionInfo.atime = matcher.group(7);
                                    transactionInfo.spent_time = SystemClock.uptimeMillis() - ((long) (Float.valueOf(transactionInfo.ktime).floatValue() * 1000.0f));
                                    arrayList2.add(transactionInfo);
                                    if (transactionInfo.spent_time >= 1000 && !arrayList.contains(Integer.valueOf(transactionInfo.rcv_pid))) {
                                        arrayList.add(Integer.valueOf(transactionInfo.rcv_pid));
                                        if (!arrayList3.contains(Integer.valueOf(transactionInfo.rcv_pid))) {
                                            arrayList3.add(Integer.valueOf(transactionInfo.rcv_pid));
                                            Slog.i(AnrManagerService.TAG, "Transcation binderList pid=" + transactionInfo.rcv_pid);
                                        }
                                    }
                                    Slog.i(AnrManagerService.TAG, transactionInfo.direction + " from " + transactionInfo.snd_pid + ":" + transactionInfo.snd_tid + " to " + transactionInfo.rcv_pid + ":" + transactionInfo.rcv_tid + " start " + transactionInfo.ktime + " android time " + transactionInfo.atime + " spent time " + transactionInfo.spent_time + " ms");
                                }
                            }
                        } else {
                            break;
                        }
                    } catch (FileNotFoundException e5) {
                        e2 = e5;
                        bufferedReader2 = bufferedReader;
                        Slog.e(AnrManagerService.TAG, "FileNotFoundException", e2);
                        if (bufferedReader2 == null) {
                            return;
                        } else {
                            bufferedReader2.close();
                        }
                    } catch (IOException e6) {
                        e = e6;
                        bufferedReader2 = bufferedReader;
                        Slog.e(AnrManagerService.TAG, "IOException when gettting Binder. ", e);
                        if (bufferedReader2 == null) {
                            return;
                        } else {
                            bufferedReader2.close();
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        if (bufferedReader != null) {
                            try {
                                bufferedReader.close();
                            } catch (IOException e7) {
                                Slog.e(AnrManagerService.TAG, "IOException when close buffer reader:", e7);
                            }
                        }
                        throw th;
                    }
                }
                Iterator it = arrayList3.iterator();
                while (it.hasNext()) {
                    readTransactionInfoFromFile(((Integer) it.next()).intValue(), arrayList);
                }
                bufferedReader.close();
            } catch (IOException e8) {
                Slog.e(AnrManagerService.TAG, "IOException when close buffer reader:", e8);
            }
        }

        private static final void setTransactionTimeoutPids(int i, ArrayList<Integer> arrayList, SparseArray<Boolean> sparseArray) throws Throwable {
            int iIntValue;
            ArrayList<Integer> arrayList2 = new ArrayList();
            readTransactionInfoFromFile(i, arrayList2);
            if (arrayList2.size() > 0) {
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
        Slog.i(TAG, "prepareStackTraceFile: " + str);
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
            Slog.e(TAG, "Unable to prepare stack trace file: " + str, e);
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
            if (this.mAppPid <= 0 || this.mIsCancelled || this.mIsCompleted) {
                Slog.e(AnrManagerService.TAG, "isValid! mAppPid: " + this.mAppPid + "mIsCancelled: " + this.mIsCancelled + "mIsCompleted: " + this.mIsCompleted);
                return false;
            }
            return true;
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

    public class AnrDumpManager {
        public HashMap<Integer, AnrDumpRecord> mDumpList = new HashMap<>();

        public AnrDumpManager() {
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
            Slog.i(AnrManagerService.TAG, "startAsyncDump: " + anrDumpRecord);
            if (anrDumpRecord == null || anrDumpRecord.mAppPid == -1) {
                return;
            }
            int i = anrDumpRecord.mAppPid;
            synchronized (this.mDumpList) {
                if (this.mDumpList.containsKey(Integer.valueOf(i))) {
                    return;
                }
                this.mDumpList.put(Integer.valueOf(i), anrDumpRecord);
                AnrManagerService.this.mAnrHandler.sendMessageAtTime(AnrManagerService.this.mAnrHandler.obtainMessage(AnrManagerService.START_ANR_DUMP_MSG, anrDumpRecord), SystemClock.uptimeMillis() + 500);
            }
        }

        private boolean isDumpable(AnrDumpRecord anrDumpRecord) {
            synchronized (this.mDumpList) {
                if (anrDumpRecord != null) {
                    try {
                        if (this.mDumpList.containsKey(Integer.valueOf(anrDumpRecord.mAppPid)) && anrDumpRecord.isValid()) {
                            return true;
                        }
                    } catch (Throwable th) {
                        throw th;
                    }
                }
                return false;
            }
        }

        public void dumpAnrDebugInfo(AnrDumpRecord anrDumpRecord, boolean z) {
            Slog.i(AnrManagerService.TAG, "dumpAnrDebugInfo begin: " + anrDumpRecord + ", isAsyncDump = " + z);
            if (anrDumpRecord == null) {
                return;
            }
            try {
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (!isDumpable(anrDumpRecord)) {
                Slog.i(AnrManagerService.TAG, "dumpAnrDebugInfo dump stopped: " + anrDumpRecord);
                return;
            }
            dumpAnrDebugInfoLocked(anrDumpRecord, z);
            Slog.i(AnrManagerService.TAG, "dumpAnrDebugInfo end: " + anrDumpRecord + ", isAsyncDump = " + z);
        }

        protected void dumpAnrDebugInfoLocked(AnrDumpRecord anrDumpRecord, boolean z) throws Exception {
            String str;
            StringBuilder sb;
            String str2;
            int iIntValue;
            synchronized (anrDumpRecord) {
                Slog.i(AnrManagerService.TAG, "dumpAnrDebugInfoLocked: " + anrDumpRecord + ", isAsyncDump = " + z);
                if (isDumpable(anrDumpRecord)) {
                    int i = anrDumpRecord.mAppPid;
                    int i2 = anrDumpRecord.mParentAppPid;
                    ArrayList arrayList = new ArrayList();
                    SparseArray sparseArray = new SparseArray(20);
                    arrayList.add(Integer.valueOf(i));
                    if (i2 <= 0) {
                        i2 = i;
                    }
                    if (i2 != i) {
                        arrayList.add(Integer.valueOf(i2));
                    }
                    if (AnrManagerService.this.mAmsPid != i && AnrManagerService.this.mAmsPid != i2) {
                        arrayList.add(Integer.valueOf(AnrManagerService.this.mAmsPid));
                    }
                    if (!z) {
                        synchronized (AnrManagerService.this.mService) {
                            ArrayList arrayList2 = (ArrayList) AnrManagerService.this.mLruProcessesField.get(AnrManagerService.this.mService);
                            for (int size = arrayList2.size() - 1; size >= 0; size--) {
                                ProcessRecord processRecord = (ProcessRecord) arrayList2.get(size);
                                if (processRecord != null && ((IApplicationThread) AnrManagerService.this.mThreadField.get(processRecord)) != null && (iIntValue = ((Integer) AnrManagerService.this.mPidField.get(processRecord)).intValue()) > 0 && iIntValue != i && iIntValue != i2 && iIntValue != AnrManagerService.this.mAmsPid) {
                                    if (((Boolean) AnrManagerService.this.mPersistentField.get(processRecord)).booleanValue()) {
                                        arrayList.add(Integer.valueOf(iIntValue));
                                    } else {
                                        sparseArray.put(iIntValue, Boolean.TRUE);
                                    }
                                }
                            }
                        }
                        ArrayList<Integer> arrayList3 = new ArrayList();
                        if (i != -1) {
                            BinderWatchdog.setTransactionTimeoutPids(i, arrayList3, sparseArray);
                        }
                        str = anrDumpRecord.mAnnotation;
                        sb = anrDumpRecord.mInfo;
                        sb.setLength(0);
                        sb.append("ANR in ");
                        sb.append(anrDumpRecord.mProcessName);
                        if (anrDumpRecord.mShortComponentName != null) {
                            sb.append(" (");
                            sb.append(anrDumpRecord.mShortComponentName);
                            sb.append(")");
                        }
                        sb.append(", time=");
                        sb.append(anrDumpRecord.mAnrTime);
                        sb.append("\n");
                        if (str != null) {
                            sb.append("Reason: ");
                            sb.append(str);
                            sb.append("\n");
                        }
                        if (anrDumpRecord.mParentAppPid != -1 && anrDumpRecord.mParentAppPid != anrDumpRecord.mAppPid) {
                            sb.append("Parent: ");
                            sb.append(anrDumpRecord.mParentShortComponentName);
                            sb.append("\n");
                        }
                        ProcessCpuTracker processCpuTracker = new ProcessCpuTracker(true);
                        if (isDumpable(anrDumpRecord)) {
                            return;
                        }
                        int[] pidsForCommands = Process.getPidsForCommands(AnrManagerService.NATIVE_STACKS_OF_INTEREST);
                        ArrayList arrayList4 = null;
                        if (pidsForCommands != null) {
                            arrayList4 = new ArrayList(pidsForCommands.length);
                            for (int i3 : pidsForCommands) {
                                arrayList4.add(Integer.valueOf(i3));
                            }
                        }
                        for (Integer num : arrayList3) {
                            if (AnrManagerService.this.isJavaProcess(num.intValue())) {
                                if (!arrayList.contains(num)) {
                                    arrayList.add(num);
                                }
                            } else {
                                if (arrayList4 == null) {
                                    arrayList4 = new ArrayList();
                                }
                                if (!arrayList4.contains(num)) {
                                    arrayList4.add(num);
                                }
                            }
                        }
                        Slog.i(AnrManagerService.TAG, "dumpStackTraces begin!");
                        AnrManagerService anrManagerService = AnrManagerService.this;
                        ActivityManagerService unused = AnrManagerService.this.mService;
                        anrManagerService.mTracesFile = ActivityManagerService.dumpStackTraces(true, arrayList, processCpuTracker, sparseArray, arrayList4);
                        Slog.i(AnrManagerService.TAG, "dumpStackTraces end!");
                        if (isDumpable(anrDumpRecord)) {
                            if (((Boolean) AnrManagerService.this.mMonitorCpuUsageField.get(AnrManagerService.this.mAMS)).booleanValue()) {
                                ProcessCpuTracker processCpuTracker2 = (ProcessCpuTracker) AnrManagerService.this.mProcessCpuTrackerField.get(AnrManagerService.this.mService);
                                synchronized (processCpuTracker2) {
                                    str2 = AnrManagerService.this.getAndroidTime() + processCpuTracker2.printCurrentState(anrDumpRecord.mAnrTime);
                                    anrDumpRecord.mCpuInfo += str2;
                                }
                                AnrManagerService.this.mUpdateCpuStatsNow.invoke(AnrManagerService.this.mService, new Object[0]);
                                sb.append(processCpuTracker.printCurrentLoad());
                                sb.append(str2);
                            }
                            Slog.i(AnrManagerService.TAG, sb.toString());
                            if (isDumpable(anrDumpRecord)) {
                                if (AnrManagerService.this.mTracesFile == null) {
                                    Process.sendSignal(i, 3);
                                }
                                anrDumpRecord.mIsCompleted = true;
                                return;
                            }
                            return;
                        }
                        return;
                    }
                    ArrayList<Integer> arrayList32 = new ArrayList();
                    if (i != -1) {
                    }
                    str = anrDumpRecord.mAnnotation;
                    sb = anrDumpRecord.mInfo;
                    sb.setLength(0);
                    sb.append("ANR in ");
                    sb.append(anrDumpRecord.mProcessName);
                    if (anrDumpRecord.mShortComponentName != null) {
                    }
                    sb.append(", time=");
                    sb.append(anrDumpRecord.mAnrTime);
                    sb.append("\n");
                    if (str != null) {
                    }
                    if (anrDumpRecord.mParentAppPid != -1) {
                        sb.append("Parent: ");
                        sb.append(anrDumpRecord.mParentShortComponentName);
                        sb.append("\n");
                    }
                    ProcessCpuTracker processCpuTracker3 = new ProcessCpuTracker(true);
                    if (isDumpable(anrDumpRecord)) {
                    }
                }
            }
        }
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
        Slog.i(TAG, "pid: " + i + " is not a Java process");
        return false;
    }

    private Boolean isException() {
        try {
            if ("free".equals(SystemProperties.get("vendor.debug.mtk.aee.status", "free")) && "free".equals(SystemProperties.get("vendor.debug.mtk.aee.status64", "free")) && "free".equals(SystemProperties.get("vendor.debug.mtk.aee.vstatus", "free")) && "free".equals(SystemProperties.get("vendor.debug.mtk.aee.vstatus64", "free"))) {
                return false;
            }
        } catch (Exception e) {
            Slog.e(TAG, "isException: " + e.toString());
        }
        return true;
    }

    public void informMessageDump(String str, int i) {
        if (mMessageMap.containsKey(Integer.valueOf(i))) {
            String str2 = mMessageMap.get(Integer.valueOf(i));
            if (str2.length() > MESSAGE_MAP_BUFFER_SIZE_MAX) {
                str2 = "";
            }
            mMessageMap.put(Integer.valueOf(i), str2 + str);
        } else {
            if (mMessageMap.size() > MESSAGE_MAP_BUFFER_COUNT_MAX) {
                mMessageMap.clear();
            }
            mMessageMap.put(Integer.valueOf(i), str);
        }
        Slog.i(TAG, "informMessageDump pid= " + i);
    }

    public int checkAnrDebugMechanism() {
        if (!sEnhanceEnable) {
            return 0;
        }
        if (-1 == this.mAnrOption) {
            this.mAnrOption = SystemProperties.getInt("persist.vendor.anr.enhancement", IS_USER_BUILD ? 1 : 2);
        }
        switch (this.mAnrOption) {
            case 0:
                return 0;
            case DataShapingServiceImpl.DATA_SHAPING_STATE_OPEN_LOCKED:
                return 1;
            case DataShapingServiceImpl.DATA_SHAPING_STATE_OPEN:
                return 2;
            default:
                return 2;
        }
    }

    public void writeEvent(int i) {
        if (i == EVENT_BOOT_COMPLETED) {
            this.mEventBootCompleted = SystemClock.uptimeMillis();
        }
    }

    public boolean isAnrDeferrable() {
        if (checkAnrDebugMechanism() == 0) {
            return false;
        }
        if ("dexopt".equals(SystemProperties.get("vendor.anr.autotest"))) {
            Slog.i(TAG, "We are doing TestDexOptSkipANR; return true in this case");
            return true;
        }
        if ("enable".equals(SystemProperties.get("vendor.anr.autotest"))) {
            Slog.i(TAG, "Do Auto Test, don't skip ANR");
            return false;
        }
        long jUptimeMillis = SystemClock.uptimeMillis();
        if (!IS_USER_BUILD) {
            if (this.mEventBootCompleted == 0 || jUptimeMillis - this.mEventBootCompleted < ANR_BOOT_DEFER_TIME) {
                Slog.i(TAG, "isAnrDeferrable(): true since mEventBootCompleted = " + this.mEventBootCompleted + " now = " + jUptimeMillis);
                return true;
            }
            if (isException().booleanValue()) {
                Slog.i(TAG, "isAnrDeferrable(): true since exception");
                return true;
            }
            float totalCpuPercent = mAnrProcessStats.getTotalCpuPercent();
            updateProcessStats();
            float totalCpuPercent2 = mAnrProcessStats.getTotalCpuPercent();
            if (totalCpuPercent > ANR_CPU_THRESHOLD && totalCpuPercent2 > ANR_CPU_THRESHOLD) {
                if (this.mCpuDeferred == 0) {
                    this.mCpuDeferred = jUptimeMillis;
                    Slog.i(TAG, "isAnrDeferrable(): true since CpuUsage = " + totalCpuPercent2 + ", mCpuDeferred = " + this.mCpuDeferred);
                    return true;
                }
                if (jUptimeMillis - this.mCpuDeferred < ANR_CPU_DEFER_TIME) {
                    Slog.i(TAG, "isAnrDeferrable(): true since CpuUsage = " + totalCpuPercent2 + ", mCpuDeferred = " + this.mCpuDeferred + ", now = " + jUptimeMillis);
                    return true;
                }
            }
            this.mCpuDeferred = 0L;
        }
        return false;
    }

    public boolean isAnrFlowSkipped(int i, String str, String str2) {
        if (-1 == this.mAnrFlow) {
            this.mAnrFlow = SystemProperties.getInt("persist.vendor.dbg.anrflow", 0);
        }
        Slog.i(TAG, "isANRFlowSkipped() AnrFlow = " + this.mAnrFlow);
        switch (this.mAnrFlow) {
            case 0:
                return false;
            case DataShapingServiceImpl.DATA_SHAPING_STATE_OPEN_LOCKED:
                Slog.i(TAG, "Skipping ANR flow: " + i + " " + str + " " + str2);
                return true;
            case DataShapingServiceImpl.DATA_SHAPING_STATE_OPEN:
                if (i != Process.myPid()) {
                    Slog.i(TAG, "Skipping ANR flow: " + i + " " + str + " " + str2);
                    StringBuilder sb = new StringBuilder();
                    sb.append("Kill process (");
                    sb.append(i);
                    sb.append(") due to ANR");
                    Slog.w(TAG, sb.toString());
                    Process.killProcess(i);
                }
                return true;
            default:
                return false;
        }
    }

    public void updateProcessStats() {
        synchronized (mAnrProcessStats) {
            long jUptimeMillis = SystemClock.uptimeMillis();
            if (jUptimeMillis - this.mLastCpuUpdateTime.get() > MONITOR_CPU_MIN_TIME) {
                this.mLastCpuUpdateTime.set(jUptimeMillis);
                mAnrProcessStats.update();
            }
        }
    }

    public String getProcessState() {
        String strPrintCurrentState;
        synchronized (mAnrProcessStats) {
            strPrintCurrentState = mAnrProcessStats.printCurrentState(SystemClock.uptimeMillis());
        }
        return strPrintCurrentState;
    }

    public String getAndroidTime() {
        return "Android time :[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SS").format(new Date(System.currentTimeMillis())) + "] [" + new Formatter().format("%.3f", Float.valueOf(SystemClock.uptimeMillis() / 1000.0f)) + "]\n";
    }

    public File createFile(String str) {
        File file = new File(str);
        if (!file.exists()) {
            Slog.i(TAG, str + " isn't exist");
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
                boolean zCopyToFile = copyToFile(fileInputStream, file2);
                fileInputStream.close();
                return zCopyToFile;
            } catch (Throwable th) {
                fileInputStream.close();
                throw th;
            }
        } catch (IOException e) {
            Slog.e(TAG, "createFile fail");
            return false;
        }
    }

    public boolean copyToFile(InputStream inputStream, File file) throws Throwable {
        FileOutputStream fileOutputStream;
        FileOutputStream fileOutputStream2 = null;
        try {
            try {
                fileOutputStream = new FileOutputStream(file, true);
            } catch (IOException e) {
                e = e;
            }
        } catch (Throwable th) {
            th = th;
            fileOutputStream = fileOutputStream2;
        }
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
            fileOutputStream.getFD().sync();
            try {
                fileOutputStream.close();
            } catch (IOException e2) {
                Slog.w(TAG, "close failed..");
            }
            return true;
        } catch (IOException e3) {
            e = e3;
            fileOutputStream2 = fileOutputStream;
            Slog.w(TAG, "copyToFile fail", e);
            if (fileOutputStream2 != null) {
                try {
                    fileOutputStream2.close();
                } catch (IOException e4) {
                    Slog.w(TAG, "close failed..");
                }
            }
            return false;
        } catch (Throwable th2) {
            th = th2;
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e5) {
                    Slog.w(TAG, "close failed..");
                }
            }
            throw th;
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
            AnrManagerService.this.dumpBinderInfo(this.mPid);
        }
    }

    public void dumpBinderInfo(int i) {
        try {
            File file = new File("/data/anr/binderinfo");
            if (file.exists()) {
                if (!file.delete()) {
                    Slog.e(TAG, "dumpBinderInfo fail due to file likely to be locked by others");
                    return;
                } else {
                    if (!file.createNewFile()) {
                        Slog.e(TAG, "dumpBinderInfo fail due to file cannot be created");
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
            stringToFile("/data/anr/binderinfo", "------ BINDER PROCESS STATE: $i ------\n");
            copyFile(file2, file);
        } catch (IOException e) {
            Slog.e(TAG, "dumpBinderInfo fail");
        }
    }

    public void enableTraceLog(boolean z) {
        Slog.i(TAG, "enableTraceLog: " + z);
        if (this.exceptionLog != null) {
            this.exceptionLog.switchFtrace(z ? 1 : 0);
        }
    }

    private void writeStringToFile(String str, String str2) throws Throwable {
        String str3;
        StringBuilder sb;
        FileOutputStream fileOutputStream;
        if (str == null) {
            return;
        }
        File file = new File(str);
        FileOutputStream fileOutputStream2 = null;
        StrictMode.ThreadPolicy threadPolicyAllowThreadDiskReads = StrictMode.allowThreadDiskReads();
        StrictMode.allowThreadDiskWrites();
        try {
            try {
                fileOutputStream = new FileOutputStream(file);
            } catch (Throwable th) {
                th = th;
            }
        } catch (IOException e) {
            e = e;
        }
        try {
            fileOutputStream.write(str2.getBytes());
            fileOutputStream.flush();
            try {
                fileOutputStream.close();
            } catch (IOException e2) {
                e = e2;
                str3 = TAG;
                sb = new StringBuilder();
                sb.append("writeStringToFile close error: ");
                sb.append(str);
                sb.append(" ");
                sb.append(e.toString());
                Slog.e(str3, sb.toString());
            }
        } catch (IOException e3) {
            e = e3;
            fileOutputStream2 = fileOutputStream;
            Slog.e(TAG, "writeStringToFile error: " + str + " " + e.toString());
            if (fileOutputStream2 != null) {
                try {
                    fileOutputStream2.close();
                } catch (IOException e4) {
                    e = e4;
                    str3 = TAG;
                    sb = new StringBuilder();
                    sb.append("writeStringToFile close error: ");
                    sb.append(str);
                    sb.append(" ");
                    sb.append(e.toString());
                    Slog.e(str3, sb.toString());
                }
            }
        } catch (Throwable th2) {
            th = th2;
            fileOutputStream2 = fileOutputStream;
            if (fileOutputStream2 != null) {
                try {
                    fileOutputStream2.close();
                } catch (IOException e5) {
                    Slog.e(TAG, "writeStringToFile close error: " + str + " " + e5.toString());
                }
            }
            StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskReads);
            throw th;
        }
        StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskReads);
    }

    private boolean isBuiltinApp(ApplicationInfo applicationInfo) {
        return ((applicationInfo.flags & 1) == 0 && (applicationInfo.flags & 128) == 0) ? false : true;
    }

    private boolean needReduceAnrDump(ApplicationInfo applicationInfo) {
        return (!IS_USER_BUILD || isBuiltinApp(applicationInfo) || SystemProperties.getInt("persist.vendor.anr.dumpthr", 0) == 1) ? false : true;
    }
}
