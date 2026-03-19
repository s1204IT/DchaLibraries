package com.mediatek.server.am;

import android.app.AppGlobals;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Slog;
import com.android.server.am.ActivityManagerDebugConfig;
import com.android.server.am.ActivityRecord;
import com.android.server.am.ContentProviderRecord;
import com.android.server.am.ProcessRecord;
import com.mediatek.amsAal.AalUtils;
import com.mediatek.duraspeed.manager.IDuraSpeedNative;
import com.mediatek.duraspeed.suppress.ISuppressAction;
import com.mediatek.server.powerhal.PowerHalManagerImpl;
import dalvik.system.PathClassLoader;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AmsExtImpl extends AmsExt {
    private static final String INKERNEL_MINFREE_PATH = "/sys/module/lowmemorykiller/parameters/minfree";
    private static final String TAG = "AmsExtImpl";
    public static PathClassLoader sClassLoader;
    private AalUtils mAalUtils;
    private Context mContext;
    private IDuraSpeedNative mDuraSpeedService;
    private boolean mIsLaunchingProvider;
    public PowerHalManagerImpl mPowerHalManagerImpl;
    private ISuppressAction mSuppressAction;
    private boolean isDebug = false;
    private boolean isDuraSpeedSupport = "1".equals(SystemProperties.get("persist.vendor.duraspeed.support"));
    private final String amsLogProp = "persist.vendor.sys.activitylog";

    public AmsExtImpl() {
        this.mPowerHalManagerImpl = null;
        this.mAalUtils = null;
        this.mPowerHalManagerImpl = new PowerHalManagerImpl();
        if (this.isDuraSpeedSupport) {
            try {
                sClassLoader = new PathClassLoader("/system/framework/duraspeed.jar", AmsExtImpl.class.getClassLoader());
                this.mDuraSpeedService = (IDuraSpeedNative) Class.forName("com.mediatek.duraspeed.manager.DuraSpeedService", false, sClassLoader).getConstructor(new Class[0]).newInstance(new Object[0]);
                this.mSuppressAction = (ISuppressAction) Class.forName("com.mediatek.duraspeed.suppress.SuppressAction", false, sClassLoader).getConstructor(new Class[0]).newInstance(new Object[0]);
            } catch (Exception e) {
                Slog.e(TAG, e.toString());
            }
        }
        if (this.mAalUtils == null && AalUtils.isSupported()) {
            this.mAalUtils = AalUtils.getInstance();
        }
    }

    public void onAddErrorToDropBox(String str, String str2, int i) {
        if (this.isDebug) {
            Slog.d(TAG, "onAddErrorToDropBox, dropboxTag=" + str + ", info=" + str2 + ", pid=" + i);
        }
    }

    public void onSystemReady(Context context) {
        Slog.d(TAG, "onSystemReady");
        if (this.isDuraSpeedSupport && this.mDuraSpeedService != null) {
            this.mDuraSpeedService.onSystemReady();
        }
        this.mContext = context;
    }

    public void onBeforeActivitySwitch(ActivityRecord activityRecord, ActivityRecord activityRecord2, boolean z, int i) {
        if (activityRecord2 == null || activityRecord2.info == null || activityRecord == null) {
            return;
        }
        if (activityRecord2.packageName == activityRecord.packageName && activityRecord2.info.name == activityRecord.info.name) {
            return;
        }
        String str = activityRecord.packageName;
        String str2 = activityRecord2.packageName;
        if (this.isDebug) {
            Slog.d(TAG, "onBeforeActivitySwitch, lastResumedPackageName=" + str + ", nextResumedPackageName=" + str2);
        }
        if (this.mPowerHalManagerImpl != null) {
            this.mPowerHalManagerImpl.amsBoostResume(str, str2);
        }
        if (this.isDuraSpeedSupport && this.mDuraSpeedService != null) {
            this.mDuraSpeedService.onBeforeActivitySwitch(str, str2, z, i);
        }
    }

    public void onAfterActivityResumed(ActivityRecord activityRecord) {
        if (activityRecord.app == null) {
            return;
        }
        int i = activityRecord.app.pid;
        String str = activityRecord.info.name;
        String str2 = activityRecord.info.packageName;
        if (this.isDebug) {
            Slog.d(TAG, "onAfterActivityResumed, pid=" + i + ", activityName=" + str + ", packageName=" + str2);
        }
        if (this.mPowerHalManagerImpl != null) {
            this.mPowerHalManagerImpl.amsBoostNotify(i, str, str2);
        }
        if (this.mAalUtils != null) {
            this.mAalUtils.onAfterActivityResumed(str2, str);
        }
    }

    public void onUpdateSleep(boolean z, boolean z2) {
        if (this.mAalUtils != null) {
            this.mAalUtils.onUpdateSleep(z, z2);
        }
    }

    public void setAalMode(int i) {
        if (this.mAalUtils != null) {
            this.mAalUtils.setAalMode(i);
        }
    }

    public void setAalEnabled(boolean z) {
        if (this.mAalUtils != null) {
            this.mAalUtils.setEnabled(z);
        }
    }

    public int amsAalDump(PrintWriter printWriter, String[] strArr, int i) {
        if (this.mAalUtils != null) {
            return this.mAalUtils.dump(printWriter, strArr, i);
        }
        return i;
    }

    public void onStartProcess(String str, String str2) {
        if (this.isDebug) {
            Slog.d(TAG, "onStartProcess, hostingType=" + str + ", packageName=" + str2);
        }
        if (this.mPowerHalManagerImpl != null) {
            this.mPowerHalManagerImpl.amsBoostProcessCreate(str, str2);
        }
    }

    public void onEndOfActivityIdle(Context context, Intent intent) {
        if (this.isDebug) {
            Slog.d(TAG, "onEndOfActivityIdle, idleIntent=" + intent);
        }
        if (this.mPowerHalManagerImpl != null) {
            this.mPowerHalManagerImpl.amsBoostStop();
        }
        if (this.isDuraSpeedSupport && this.mDuraSpeedService != null) {
            this.mDuraSpeedService.onActivityIdle(context, intent);
        }
    }

    public void enableAmsLog(ArrayList<ProcessRecord> arrayList) {
        String str = SystemProperties.get("persist.vendor.sys.activitylog", (String) null);
        if (str != null && !str.equals("")) {
            if (str.indexOf(" ") != -1 && str.indexOf(" ") + 1 <= str.length()) {
                enableAmsLog(null, new String[]{str.substring(0, str.indexOf(" ")), str.substring(str.indexOf(" ") + 1, str.length())}, 0, arrayList);
            } else {
                SystemProperties.set("persist.vendor.sys.activitylog", "");
            }
        }
    }

    public void enableAmsLog(PrintWriter printWriter, String[] strArr, int i, ArrayList<ProcessRecord> arrayList) {
        int i2 = i + 1;
        if (i2 >= strArr.length) {
            if (printWriter != null) {
                printWriter.println("  Invalid argument!");
            }
            SystemProperties.set("persist.vendor.sys.activitylog", "");
            return;
        }
        String str = strArr[i];
        boolean zEquals = "on".equals(strArr[i2]);
        SystemProperties.set("persist.vendor.sys.activitylog", strArr[i] + " " + strArr[i2]);
        if (str.equals("x")) {
            enableAmsLog(zEquals, arrayList);
            return;
        }
        if (printWriter != null) {
            printWriter.println("  Invalid argument!");
        }
        SystemProperties.set("persist.vendor.sys.activitylog", "");
    }

    private void enableAmsLog(boolean z, ArrayList<ProcessRecord> arrayList) {
        this.isDebug = z;
        ActivityManagerDebugConfig.APPEND_CATEGORY_NAME = z;
        ActivityManagerDebugConfig.DEBUG_ALL = z;
        ActivityManagerDebugConfig.DEBUG_ALL_ACTIVITIES = z;
        ActivityManagerDebugConfig.DEBUG_ADD_REMOVE = z;
        ActivityManagerDebugConfig.DEBUG_ANR = z;
        ActivityManagerDebugConfig.DEBUG_APP = z;
        ActivityManagerDebugConfig.DEBUG_BACKGROUND_CHECK = z;
        ActivityManagerDebugConfig.DEBUG_BACKUP = z;
        ActivityManagerDebugConfig.DEBUG_BROADCAST = z;
        ActivityManagerDebugConfig.DEBUG_BROADCAST_BACKGROUND = z;
        ActivityManagerDebugConfig.DEBUG_BROADCAST_LIGHT = z;
        ActivityManagerDebugConfig.DEBUG_CLEANUP = z;
        ActivityManagerDebugConfig.DEBUG_CONFIGURATION = z;
        ActivityManagerDebugConfig.DEBUG_CONTAINERS = z;
        ActivityManagerDebugConfig.DEBUG_FOCUS = z;
        ActivityManagerDebugConfig.DEBUG_IDLE = z;
        ActivityManagerDebugConfig.DEBUG_IMMERSIVE = z;
        ActivityManagerDebugConfig.DEBUG_LOCKTASK = z;
        ActivityManagerDebugConfig.DEBUG_LRU = z;
        ActivityManagerDebugConfig.DEBUG_MU = z;
        ActivityManagerDebugConfig.DEBUG_NETWORK = z;
        ActivityManagerDebugConfig.DEBUG_PAUSE = z;
        ActivityManagerDebugConfig.DEBUG_POWER = z;
        ActivityManagerDebugConfig.DEBUG_POWER_QUICK = z;
        ActivityManagerDebugConfig.DEBUG_PROCESS_OBSERVERS = z;
        ActivityManagerDebugConfig.DEBUG_PROCESSES = z;
        ActivityManagerDebugConfig.DEBUG_PROVIDER = z;
        ActivityManagerDebugConfig.DEBUG_PSS = z;
        ActivityManagerDebugConfig.DEBUG_RECENTS = z;
        ActivityManagerDebugConfig.DEBUG_RELEASE = z;
        ActivityManagerDebugConfig.DEBUG_RESULTS = z;
        ActivityManagerDebugConfig.DEBUG_SAVED_STATE = z;
        ActivityManagerDebugConfig.DEBUG_RECENTS_TRIM_TASKS = z;
        ActivityManagerDebugConfig.DEBUG_METRICS = z;
        ActivityManagerDebugConfig.DEBUG_SERVICE = z;
        ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE = z;
        ActivityManagerDebugConfig.DEBUG_SERVICE_EXECUTING = z;
        ActivityManagerDebugConfig.DEBUG_STACK = z;
        ActivityManagerDebugConfig.DEBUG_STATES = z;
        ActivityManagerDebugConfig.DEBUG_SWITCH = z;
        ActivityManagerDebugConfig.DEBUG_TASKS = z;
        ActivityManagerDebugConfig.DEBUG_TRANSITION = z;
        ActivityManagerDebugConfig.DEBUG_UID_OBSERVERS = z;
        ActivityManagerDebugConfig.DEBUG_URI_PERMISSION = z;
        ActivityManagerDebugConfig.DEBUG_USER_LEAVING = z;
        ActivityManagerDebugConfig.DEBUG_VISIBILITY = z;
        ActivityManagerDebugConfig.DEBUG_USAGE_STATS = z;
        ActivityManagerDebugConfig.DEBUG_PERMISSIONS_REVIEW = z;
        ActivityManagerDebugConfig.DEBUG_WHITELISTS = z;
        for (int i = 0; i < arrayList.size(); i++) {
            ProcessRecord processRecord = arrayList.get(i);
            if (processRecord != null && processRecord.thread != null) {
                try {
                    processRecord.thread.enableActivityThreadLog(z);
                } catch (Exception e) {
                    Slog.e(TAG, "Error happens when enableActivityThreadLog", e);
                }
            }
        }
    }

    public void onWakefulnessChanged(int i) {
        if (this.isDuraSpeedSupport && this.mDuraSpeedService != null) {
            this.mDuraSpeedService.onWakefulnessChanged(i);
        }
    }

    public void addDuraSpeedService() {
        if (this.isDuraSpeedSupport && this.mDuraSpeedService != null) {
            ServiceManager.addService("duraspeed", (IBinder) this.mDuraSpeedService, true);
        }
    }

    public void startDuraSpeedService(Context context) {
        if (this.isDuraSpeedSupport && this.mDuraSpeedService != null) {
            this.mDuraSpeedService.startDuraSpeedService(context);
            if (!new File(INKERNEL_MINFREE_PATH).exists()) {
                new MemoryServerThread().start();
            }
        }
    }

    public String onReadyToStartComponent(String str, int i, String str2) {
        if (this.mDuraSpeedService != null && this.mDuraSpeedService.isDuraSpeedEnabled()) {
            return this.mSuppressAction.onReadyToStartComponent(str, i, str2);
        }
        return null;
    }

    public boolean onBeforeStartProcessForStaticReceiver(String str) {
        if (this.mDuraSpeedService != null && this.mDuraSpeedService.isDuraSpeedEnabled()) {
            return this.mSuppressAction.onBeforeStartProcessForStaticReceiver(str);
        }
        return false;
    }

    public void addToSuppressRestartList(String str) {
        if (this.mDuraSpeedService != null && this.mDuraSpeedService.isDuraSpeedEnabled() && this.mContext != null) {
            this.mSuppressAction.addToSuppressRestartList(this.mContext, str);
        }
    }

    public boolean notRemoveAlarm(String str) {
        if (this.mDuraSpeedService != null && this.mDuraSpeedService.isDuraSpeedEnabled()) {
            return this.mSuppressAction.notRemoveAlarm(str);
        }
        return false;
    }

    public boolean IsBuildInApp() {
        IPackageManager packageManager = AppGlobals.getPackageManager();
        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageManager.getNameForUid(Binder.getCallingUid()), 0, UserHandle.getCallingUserId());
            if (applicationInfo != null) {
                if ((applicationInfo.flags & 1) == 0) {
                    if ((applicationInfo.flags & 128) != 0) {
                    }
                }
                return true;
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "getCallerProcessName exception :" + e);
        }
        return false;
    }

    public boolean shouldKilledByAm(String str, String str2) {
        if (str != null && str.equals("android.process.media") && str2 != null && str2.startsWith("empty")) {
            if (this.isDebug) {
                Slog.w(TAG, "skip kill " + str + " by am for " + str2);
                return false;
            }
            return false;
        }
        return true;
    }

    public void checkAppInLaunchingProvider(ProcessRecord processRecord, int i, int i2, ArrayList<ContentProviderRecord> arrayList) {
        if (processRecord.pubProviders.size() != 0 && i > 0) {
            int size = arrayList.size();
            for (int i3 = 0; i3 < size; i3++) {
                if (arrayList.get(i3).launchingApp == processRecord) {
                    this.mIsLaunchingProvider = true;
                    return;
                }
            }
        }
    }

    public void setAppInLaunchingProviderAdj(ProcessRecord processRecord, int i, int i2) {
        if (this.mIsLaunchingProvider && i > 200 && i >= i2) {
            this.mIsLaunchingProvider = false;
            processRecord.adjType = "launching-provider";
        }
    }

    private class MemoryServerThread extends Thread {
        public static final String HOST_NAME = "duraspeed_memory";

        private MemoryServerThread() {
        }

        @Override
        public void run() throws Throwable {
            LocalServerSocket localServerSocket;
            Throwable th;
            Exception e;
            ExecutorService executorServiceNewCachedThreadPool = Executors.newCachedThreadPool();
            try {
                try {
                    Slog.d(AmsExtImpl.TAG, "Crate local socket: duraspeed_memory");
                    localServerSocket = new LocalServerSocket(HOST_NAME);
                    while (true) {
                        try {
                            Slog.d(AmsExtImpl.TAG, "Waiting Client connected...");
                            LocalSocket localSocketAccept = localServerSocket.accept();
                            localSocketAccept.setReceiveBufferSize(256);
                            localSocketAccept.setSendBufferSize(256);
                            Slog.i(AmsExtImpl.TAG, "There is a client is accepted: " + localSocketAccept.toString());
                            executorServiceNewCachedThreadPool.execute(AmsExtImpl.this.new ConnectionHandler(localSocketAccept));
                        } catch (Exception e2) {
                            e = e2;
                            Slog.w(AmsExtImpl.TAG, "listenConnection catch Exception");
                            e.printStackTrace();
                            Slog.d(AmsExtImpl.TAG, "listenConnection finally shutdown!!");
                            if (executorServiceNewCachedThreadPool != null) {
                                executorServiceNewCachedThreadPool.shutdown();
                            }
                            if (localServerSocket != null) {
                                try {
                                    localServerSocket.close();
                                } catch (IOException e3) {
                                    e3.printStackTrace();
                                }
                            }
                            Slog.d(AmsExtImpl.TAG, "listenConnection() - end");
                            return;
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                    Slog.d(AmsExtImpl.TAG, "listenConnection finally shutdown!!");
                    if (executorServiceNewCachedThreadPool != null) {
                        executorServiceNewCachedThreadPool.shutdown();
                    }
                    if (localServerSocket != null) {
                        try {
                            localServerSocket.close();
                        } catch (IOException e4) {
                            e4.printStackTrace();
                        }
                    }
                    throw th;
                }
            } catch (Exception e5) {
                localServerSocket = null;
                e = e5;
            } catch (Throwable th3) {
                localServerSocket = null;
                th = th3;
                Slog.d(AmsExtImpl.TAG, "listenConnection finally shutdown!!");
                if (executorServiceNewCachedThreadPool != null) {
                }
                if (localServerSocket != null) {
                }
                throw th;
            }
        }
    }

    public class ConnectionHandler implements Runnable {
        private LocalSocket mSocket;
        private boolean mIsContinue = true;
        private InputStreamReader mInput = null;
        private DataOutputStream mOutput = null;

        public ConnectionHandler(LocalSocket localSocket) {
            this.mSocket = localSocket;
        }

        public void terminate() {
            Slog.d(AmsExtImpl.TAG, "DuraSpeed memory trigger process terminate.");
            this.mIsContinue = false;
        }

        @Override
        public void run() {
            Slog.i(AmsExtImpl.TAG, "DuraSpeed new connection: " + this.mSocket.toString());
            try {
                this.mInput = new InputStreamReader(this.mSocket.getInputStream());
                this.mOutput = new DataOutputStream(this.mSocket.getOutputStream());
                try {
                    BufferedReader bufferedReader = new BufferedReader(this.mInput);
                    while (this.mIsContinue) {
                        String[] strArrSplit = bufferedReader.readLine().split(":");
                        if (strArrSplit[0] == null || strArrSplit[1] == null) {
                            Slog.e(AmsExtImpl.TAG, "Received lmkdData error");
                        } else {
                            String strTrim = strArrSplit[0].trim();
                            String strTrim2 = strArrSplit[1].trim();
                            int i = Integer.parseInt(strTrim);
                            int i2 = Integer.parseInt(strTrim2);
                            if (i >= 0 && i2 > 0 && AmsExtImpl.this.mDuraSpeedService.isDuraSpeedEnabled()) {
                                AmsExtImpl.this.mDuraSpeedService.triggerMemory(i2);
                            }
                        }
                    }
                } catch (Exception e) {
                    Slog.w(AmsExtImpl.TAG, "duraSpeed: memory Exception.");
                    e.printStackTrace();
                    terminate();
                }
                Slog.w(AmsExtImpl.TAG, "duraSpeed: New connection running ending ");
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }
    }
}
