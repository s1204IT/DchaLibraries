package com.mediatek.runningbooster;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.INotificationManager;
import android.app.Notification;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Binder;
import android.os.Build;
import android.os.FileUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.service.notification.StatusBarNotification;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.SparseArray;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.pm.PackageManagerService;
import com.mediatek.am.AMEventHookData;
import com.mediatek.anrmanager.ANRManager;
import com.mediatek.apm.frc.FocusRelationshipChainPolicy;
import com.mediatek.apm.suppression.SuppressionPolicy;
import com.mediatek.runningbooster.IRunningBoosterManager;
import com.mediatek.runningbooster.RbConfiguration;
import com.mediatek.suppression.service.SuppressionInternal;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class RunningBoosterService extends IRunningBoosterManager.Stub {
    public static final String ACTION_START_RUNNING_BOOSTER = "android.intent.action.ACTION_START_RUNNING_BOOSTER";
    private static IPackageManager B = null;
    private static boolean H = false;
    private static RunningBoosterService I = null;
    private static final boolean IS_USER_BUILD;
    public static final String RUNNING_BOOSTER_APP = "com.android.runningbooster";
    private static AmsEventHandler W;
    private static boolean at;
    private final boolean DEBUG;
    private final Object J;
    private final Object K;
    private final Object L;
    private String M;
    private String N;
    private BroadcastReceiver O;
    private boolean P;
    private boolean Q;
    private boolean R;
    private boolean S;
    private boolean T;
    private final String TAG = "RunningBoosterService";
    private boolean U;
    private String V;
    private FocusRelationshipChainPolicy X;
    private SuppressionPolicy Y;
    private SuppressionInternal Z;
    private HashMap<Integer, String> aa;
    private HashMap<String, ArrayList<RbConfiguration.SuppressionPoint>> ab;
    private HashMap<String, ArrayList<RbConfiguration.SuppressionPoint>> ac;
    private HashMap<String, ArrayList<String>> ad;
    private SparseArray<RbConfiguration.PolicyList> ae;
    private HashMap<String, ArrayList<String>> af;
    private HashMap<String, ArrayList<String>> ag;
    private HashMap<String, ArrayList<String>> ah;
    private HashMap<String, ArrayList<String>> ai;
    private ArrayList<String> aj;
    private ArrayList<String> ak;
    private HashMap<String, Boolean> al;
    private HashMap<String, Boolean> am;
    private ArrayList<String> an;
    private ArrayMap<Integer, ArrayList<Integer>> ao;
    private List<ActivityManager.RecentTaskInfo> ap;
    private HashMap<String, ArrayList<String>> aq;
    private RbConfiguration ar;
    private RbConfiguration.SuppressionPoint as;
    private ArrayList<String> au;
    private ArrayList<String> av;
    private Context mContext;
    private int mCurrentUserId;

    static {
        IS_USER_BUILD = "user".equals(Build.TYPE) || "userdebug".equals(Build.TYPE);
        H = 1 == SystemProperties.getInt("persist.disable.licensecheck", 0);
        I = null;
        B = null;
        at = false;
    }

    public RunningBoosterService(Context context) throws Throwable {
        this.DEBUG = 1 == SystemProperties.getInt("persist.runningbooster.debug", 0);
        this.J = new Object();
        this.K = new Object();
        this.L = new Object();
        this.M = "/data/runningbooster/package_list.txt";
        this.N = "/system/etc/runningbooster/platform_list.txt";
        this.O = null;
        this.P = false;
        this.Q = false;
        this.R = false;
        this.S = false;
        this.T = false;
        this.U = false;
        this.X = FocusRelationshipChainPolicy.getInstance();
        this.Y = SuppressionPolicy.getInstance();
        this.Z = (SuppressionInternal) LocalServices.getService(SuppressionInternal.class);
        this.aa = new HashMap<>();
        this.ab = new HashMap<>();
        this.ac = new HashMap<>();
        this.ad = new HashMap<>();
        this.ae = new SparseArray<>();
        this.af = new HashMap<>();
        this.ag = new HashMap<>();
        this.ah = new HashMap<>();
        this.ai = new HashMap<>();
        this.aj = new ArrayList<>();
        this.ak = new ArrayList<>();
        this.al = new HashMap<>();
        this.am = new HashMap<>();
        this.an = new ArrayList<>();
        this.ao = new ArrayMap<>();
        this.ap = new ArrayList();
        this.aq = new HashMap<>();
        this.ar = new RbConfiguration();
        this.as = new RbConfiguration.SuppressionPoint("initial", -1);
        this.au = new ArrayList<>();
        this.av = new ArrayList<>();
        this.mContext = context;
        f();
        h();
        this.mCurrentUserId = ActivityManager.getCurrentUser();
        HandlerThread handlerThread = new HandlerThread("AmsEventThread");
        handlerThread.start();
        W = new AmsEventHandler(handlerThread.getLooper());
        I = this;
        this.mCurrentUserId = ActivityManager.getCurrentUser();
        Slog.d("RunningBoosterService", "mCurrentUserId = " + this.mCurrentUserId);
        j(this.M);
        if (!IS_USER_BUILD || this.DEBUG) {
            j("/data/runningbooster/allow_list_bytype.txt");
            j("/data/runningbooster/kill_list.txt");
            j("/data/runningbooster/allow_list.txt");
            j("/data/runningbooster/adj_allow_list.txt");
            j("/data/runningbooster/adj_kill_list.txt");
            j("/data/runningbooster/filter_list.txt");
            j("/data/runningbooster/recent_list.txt");
            j("/data/runningbooster/notification_list.txt");
            j("/data/runningbooster/appwidget_list.txt");
            j("/data/runningbooster/location_list.txt");
        }
    }

    public void systemRunning() {
        Slog.d("RunningBoosterService", "[systemRunning]");
        e();
    }

    public void applyUserConfig(String str, RbConfiguration rbConfiguration) {
        RbConfiguration rbConfigurationA;
        if (!H) {
            a.a(this.mContext).f("applyUserConfig");
        }
        Slog.d("RunningBoosterService", "[applyUserConfig] Enter");
        a(rbConfiguration);
        int callingUid = Binder.getCallingUid();
        new RbConfiguration();
        RbConfiguration.PolicyList policyList = this.ae.get(callingUid);
        if (this.aa.get(Integer.valueOf(callingUid)) == null) {
            a(callingUid, str);
            this.aa.put(Integer.valueOf(callingUid), str);
        }
        RbConfiguration rbConfiguration2 = new RbConfiguration();
        if (policyList == null) {
            rbConfigurationA = a(rbConfiguration, rbConfiguration2, callingUid);
            Slog.d("RunningBoosterService", "[applyUserConfig] null == oldUserPolicy");
        } else {
            rbConfigurationA = a(rbConfiguration, policyList.mConfig, callingUid);
            Slog.d("RunningBoosterService", "[applyUserConfig] new UserPolicy");
        }
        RbConfiguration.PolicyList policyList2 = new RbConfiguration.PolicyList(callingUid, this.mCurrentUserId, str, false, rbConfigurationA);
        a(rbConfigurationA);
        this.ae.put(callingUid, policyList2);
        boolean z = 1 == rbConfigurationA.suppressPoint.size() && ((RbConfiguration.SuppressionPoint) rbConfigurationA.suppressPoint.get(0)).equal(RbConfiguration.DEAULT_STARTPOINT);
        Slog.d("RunningBoosterService", "[applyUserConfig] isInitial = " + z);
        if (!z) {
            Iterator it = rbConfigurationA.suppressPoint.iterator();
            while (it.hasNext()) {
                ((RbConfiguration.SuppressionPoint) it.next()).mConfig = rbConfigurationA;
            }
            if (true == rbConfigurationA.enableRunningBooster) {
                Slog.d("RunningBoosterService", "[applyUserConfig] enableRunningBooster is enable, pkg = " + str);
                this.ab.put(str, rbConfigurationA.suppressPoint);
            } else {
                Slog.d("RunningBoosterService", "[applyUserConfig] enableRunningBooster is disable, pkg = " + str);
                if (this.ab.size() > 0) {
                    i(str);
                    this.ab.remove(str);
                    this.ac.remove(str);
                }
            }
            ArrayList<RbConfiguration.SuppressionPoint> arrayList = new ArrayList<>();
            for (RbConfiguration.SuppressionPoint suppressionPoint : rbConfigurationA.suppressPoint) {
                Slog.d("RunningBoosterService", "[applyUserConfig] SupressPoint tag = " + suppressionPoint.mSuppressTag);
                RbConfiguration.SuppressionPoint suppressionPoint2 = new RbConfiguration.SuppressionPoint();
                if (suppressionPoint.mAppState == 0) {
                    suppressionPoint2.mAppState = 1;
                } else {
                    suppressionPoint2.mAppState = 0;
                }
                suppressionPoint2.mPackageName = suppressionPoint.mPackageName;
                suppressionPoint2.mSuppressTag = suppressionPoint.mSuppressTag;
                arrayList.add(suppressionPoint2);
                Slog.d("RunningBoosterService", "[applyUserConfig] unSupressPoint tag = " + suppressionPoint2.mSuppressTag);
            }
            Slog.d("RunningBoosterService", "[applyUserConfig] mCurrentUnSuppressPointList pkg = " + str);
            this.ac.put(str, arrayList);
        }
    }

    public String getAPIVersion() {
        if (!H) {
            a.a(this.mContext).f("getAPIVersion");
        }
        Slog.d("RunningBoosterService", "getAPIVersion");
        return "1.0";
    }

    public List<String> getPlatformWhiteList() throws Throwable {
        String str;
        String str2;
        BufferedReader bufferedReader;
        Exception e;
        if (!H) {
            a.a(this.mContext).f("getPlatformWhiteList");
        }
        int callingUid = Binder.getCallingUid();
        try {
            if (B == null) {
                B = a();
            }
        } catch (RuntimeException e2) {
            Slog.e("RunningBoosterService", "[getPlatformWhiteList]can't get PMS " + e2);
        }
        try {
            String[] packagesForUid = B.getPackagesForUid(callingUid);
            if (packagesForUid == null) {
                str2 = null;
            } else {
                str2 = packagesForUid[0];
                try {
                    Slog.d("RunningBoosterService", "[getPlatformWhiteList] callerPkgName = " + str2);
                } catch (RemoteException e3) {
                    str = str2;
                    e = e3;
                    Slog.e("RunningBoosterService", "[getPlatformWhiteList] get callerPkgName fail " + e);
                    str2 = str;
                }
            }
        } catch (RemoteException e4) {
            e = e4;
            str = null;
        }
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(this.N), "utf-8"));
            while (true) {
                try {
                    try {
                        String line = bufferedReader.readLine();
                        if (line != null) {
                            this.aj.add(line);
                            Slog.d("RunningBoosterService", "[getPlatformWhiteList] package name = " + line);
                        } else {
                            try {
                                break;
                            } catch (Exception e5) {
                                Slog.e("RunningBoosterService", "file closed fail ", e5);
                            }
                        }
                    } catch (Throwable th) {
                        th = th;
                        try {
                            bufferedReader.close();
                        } catch (Exception e6) {
                            Slog.e("RunningBoosterService", "file closed fail ", e6);
                        }
                        throw th;
                    }
                } catch (Exception e7) {
                    e = e7;
                    Slog.e("RunningBoosterService", "getPlatformWhiteList fail ", e);
                    try {
                        bufferedReader.close();
                    } catch (Exception e8) {
                        Slog.e("RunningBoosterService", "file closed fail ", e8);
                    }
                }
            }
            bufferedReader.close();
        } catch (Exception e9) {
            bufferedReader = null;
            e = e9;
        } catch (Throwable th2) {
            th = th2;
            bufferedReader = null;
        }
        if (str2 != null) {
            this.aj.add(str2);
        }
        return this.aj;
    }

    private void e() {
        Slog.d("RunningBoosterService", "startRunningBoosterApp");
        Intent intent = new Intent(ACTION_START_RUNNING_BOOSTER);
        if ("1".equals(SystemProperties.get("persist.runningbooster.app.on"))) {
            intent.putExtra("APP_DEFAULT_ON", true);
        } else {
            intent.putExtra("APP_DEFAULT_ON", false);
        }
        this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
    }

    private void f() {
        Slog.d("RunningBoosterService", "[registerReceiver]");
        if (this.O == null) {
            Slog.d("RunningBoosterService", "[registerReceiver] start");
            this.O = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    Slog.d("RunningBoosterService", "received broadcast, action is: " + action);
                    if ("android.intent.action.PACKAGE_ADDED" == action || "android.intent.action.PACKAGE_REPLACED" == action || "android.intent.action.PACKAGE_DATA_CLEARED" == action || "android.intent.action.PACKAGE_CHANGED" == action) {
                        int intExtra = intent.getIntExtra("android.intent.extra.UID", -1);
                        String schemeSpecificPart = intent.getData().getSchemeSpecificPart();
                        Slog.d("RunningBoosterService", "[PACKAGE_ADDED|PACKAGE_REPLACED|DATA_CLEARED] uid = " + intExtra);
                        if (RunningBoosterService.this.aa.containsValue(schemeSpecificPart)) {
                            Slog.d("RunningBoosterService", "[ADDED|REPLACED|DATA_CLEARED] mSupportAppPackage = " + schemeSpecificPart);
                            RunningBoosterService.this.e();
                            return;
                        }
                        return;
                    }
                    if ("android.intent.action.PACKAGE_RESTARTED" == action) {
                        int intExtra2 = intent.getIntExtra("android.intent.extra.UID", -1);
                        String schemeSpecificPart2 = intent.getData().getSchemeSpecificPart();
                        if (!RunningBoosterService.IS_USER_BUILD || RunningBoosterService.this.DEBUG) {
                            Slog.d("RunningBoosterService", "[ACTION_PACKAGE_RESTARTED] uid = " + intExtra2);
                        }
                        if (RunningBoosterService.this.aa.get(Integer.valueOf(intExtra2)) != null) {
                            Slog.d("RunningBoosterService", "[ACTION_PACKAGE_RESTARTED] mSupportAppPackage = " + ((String) RunningBoosterService.this.aa.get(Integer.valueOf(intExtra2))));
                            RbConfiguration.PolicyList policyList = (RbConfiguration.PolicyList) RunningBoosterService.this.ae.get(intExtra2);
                            if (policyList != null) {
                                policyList.mForceStopState = true;
                                RunningBoosterService.this.ae.put(intExtra2, policyList);
                                RunningBoosterService.this.a(intExtra2, schemeSpecificPart2, true);
                                return;
                            }
                            return;
                        }
                        return;
                    }
                    if ("android.intent.action.USER_SWITCHED" == action) {
                        int i = RunningBoosterService.this.mCurrentUserId;
                        RunningBoosterService.this.mCurrentUserId = ActivityManager.getCurrentUser();
                        Slog.d("RunningBoosterService", "[ACTION_USER_SWITCHED] oldUserId = " + i + " mCurrentUserId = " + RunningBoosterService.this.mCurrentUserId);
                        if (i != RunningBoosterService.this.mCurrentUserId) {
                            RunningBoosterService.W.sendMessage(RunningBoosterService.W.obtainMessage(1004));
                        }
                        RunningBoosterService.this.e();
                        return;
                    }
                    if ("android.intent.action.PACKAGE_REMOVED" == action) {
                        int intExtra3 = intent.getIntExtra("android.intent.extra.UID", -1);
                        String schemeSpecificPart3 = intent.getData().getSchemeSpecificPart();
                        Slog.d("RunningBoosterService", "[ACTION_PACKAGE_REMOVED] uid = " + intExtra3 + "pkg = " + schemeSpecificPart3);
                        if (RunningBoosterService.this.aa.get(Integer.valueOf(intExtra3)) != null) {
                            RunningBoosterService.this.a(intExtra3, schemeSpecificPart3, false);
                            return;
                        }
                        return;
                    }
                    if ("android.intent.action.ACTION_PREBOOT_IPO".equals(action)) {
                        Slog.d("RunningBoosterService", "IPO reboot");
                        RunningBoosterService.this.e();
                    } else if ("android.intent.action.BOOT_COMPLETED" == action) {
                        Slog.d("RunningBoosterService", "reboot complete");
                        RunningBoosterService.this.e();
                    }
                }
            };
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_ADDED");
        intentFilter.addAction("android.intent.action.PACKAGE_REPLACED");
        intentFilter.addAction("android.intent.action.PACKAGE_RESTARTED");
        intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter.addAction("android.intent.action.PACKAGE_DATA_CLEARED");
        intentFilter.addAction("android.intent.action.PACKAGE_CHANGED");
        intentFilter.addDataScheme("package");
        this.mContext.registerReceiverAsUser(this.O, UserHandle.ALL, intentFilter, null, null);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.intent.action.USER_SWITCHED");
        intentFilter2.addAction("android.intent.action.ACTION_PREBOOT_IPO");
        intentFilter2.addAction("android.intent.action.BOOT_COMPLETED");
        this.mContext.registerReceiverAsUser(this.O, UserHandle.ALL, intentFilter2, null, null);
    }

    public void onBeforeActivitySwitch(AMEventHookData.BeforeActivitySwitch beforeActivitySwitch) {
        RbConfiguration.SuppressionPoint suppressionPointA;
        int i;
        if (this.ab.size() == 0) {
            Slog.d("RunningBoosterService", "[onBeforeActivitySwitch] mCurrentSuppressPointList is null");
            return;
        }
        if (this.DEBUG) {
            Slog.d("RunningBoosterService", "=> onBeforeActivitySwitch");
        }
        String string = beforeActivitySwitch.getString(AMEventHookData.BeforeActivitySwitch.Index.lastResumedActivityName);
        String string2 = beforeActivitySwitch.getString(AMEventHookData.BeforeActivitySwitch.Index.nextResumedActivityName);
        String string3 = beforeActivitySwitch.getString(AMEventHookData.BeforeActivitySwitch.Index.lastResumedPackageName);
        String string4 = beforeActivitySwitch.getString(AMEventHookData.BeforeActivitySwitch.Index.nextResumedPackageName);
        boolean z = beforeActivitySwitch.getBoolean(AMEventHookData.BeforeActivitySwitch.Index.isNeedToPauseActivityFirst);
        int i2 = beforeActivitySwitch.getInt(AMEventHookData.BeforeActivitySwitch.Index.lastResumedActivityType);
        int i3 = beforeActivitySwitch.getInt(AMEventHookData.BeforeActivitySwitch.Index.nextResumedActivityType);
        if (string3 == null) {
            RbConfiguration.SuppressionPoint suppressionPoint = RbConfiguration.DEAULT_STARTPOINT;
            Slog.d("RunningBoosterService", "[onBeforeActivitySwitch]lastResumed pkg is null");
            suppressionPointA = suppressionPoint;
        } else {
            if (string3.equals(string4)) {
                Slog.d("RunningBoosterService", "[onBeforeActivitySwitch]lastResumed pkg is the same as nextResumed pkg");
                return;
            }
            suppressionPointA = a(i2, string3, 0);
        }
        RbConfiguration.SuppressionPoint suppressionPointA2 = a(i3, string4, 1);
        if (suppressionPointA.mPackageName.equals(suppressionPointA2.mPackageName)) {
            Slog.d("RunningBoosterService", "[onBeforeActivitySwitch] Suppress package is the same, pkg = " + suppressionPointA.mPackageName);
            return;
        }
        Slog.d("RunningBoosterService", "[onBeforeActivitySwitch] mScreenStateChange = " + this.R + " mScreenOffState = " + this.Q);
        if (true == this.R && !this.Q) {
            this.R = false;
            h(string4);
            Slog.d("RunningBoosterService", "[onBeforeActivitySwitch] mPausePkgNameAfterScreenOff = " + this.V + " nextResumedPackageName = " + string4);
        }
        this.V = string4;
        if (this.DEBUG) {
            Slog.d("RunningBoosterService", "[onBeforeActivitySwitch] lastResumedActivityName = " + string);
            Slog.d("RunningBoosterService", "[onBeforeActivitySwitch] nextResumedActivityName = " + string2);
            Slog.d("RunningBoosterService", "[onBeforeActivitySwitch] lastResumedPackageName = " + string3);
            Slog.d("RunningBoosterService", "[onBeforeActivitySwitch] nextResumedPackageName = " + string4);
            Slog.d("RunningBoosterService", "[onBeforeActivitySwitch] isNeedToPauseActivityFirst = " + z);
        }
        if (this.aa.containsValue(string4)) {
            Iterator<Map.Entry<Integer, String>> it = this.aa.entrySet().iterator();
            while (true) {
                if (!it.hasNext()) {
                    i = 0;
                    break;
                }
                Map.Entry<Integer, String> next = it.next();
                String value = next.getValue();
                if (value.equals(string4)) {
                    int iIntValue = next.getKey().intValue();
                    Slog.d("RunningBoosterService", "[onBeforeActivitySwitch] mSupportAppPackage = " + value);
                    i = iIntValue;
                    break;
                }
            }
            RbConfiguration.PolicyList policyList = this.ae.get(i);
            if (policyList != null && true == policyList.mForceStopState) {
                policyList.mForceStopState = false;
                this.ae.put(i, policyList);
                e();
            }
        }
        if (this.DEBUG) {
            Slog.d("RunningBoosterService", "[onBeforeActivitySwitch] updateOrSuppress Point1 = " + suppressionPointA.mAppState + " pkgName = " + suppressionPointA.mPackageName);
            Slog.d("RunningBoosterService", "[onBeforeActivitySwitch] updateOrSuppress Point2 = " + suppressionPointA2.mAppState + " pkgName = " + suppressionPointA2.mPackageName);
        }
        new ArrayList();
        new ArrayList();
        Iterator<Map.Entry<String, ArrayList<RbConfiguration.SuppressionPoint>>> it2 = this.ab.entrySet().iterator();
        while (it2.hasNext()) {
            for (RbConfiguration.SuppressionPoint suppressionPoint2 : it2.next().getValue()) {
                if (suppressionPointA.equal(suppressionPoint2) || suppressionPointA2.equal(suppressionPoint2)) {
                    if (true == z || (!z && !at)) {
                        ActivityManager activityManager = (ActivityManager) this.mContext.getSystemService("activity");
                        synchronized (this.K) {
                            if (this.ao != null) {
                                this.ao.clear();
                            }
                            if (this.DEBUG) {
                                Slog.d("RunningBoosterService", "[onBeforeActivitySwitch] => getProcessesWithAdj");
                            }
                            this.ao = activityManager.getProcessesWithAdj();
                            if (this.DEBUG) {
                                Slog.d("RunningBoosterService", "[onBeforeActivitySwitch] <= getProcessesWithAdj");
                            }
                        }
                        synchronized (this.J) {
                            try {
                                try {
                                    this.ap.clear();
                                    if (this.DEBUG) {
                                        Slog.d("RunningBoosterService", "[onBeforeActivitySwitch] => getRecentTasks");
                                    }
                                    this.ap = activityManager.getRecentTasksForUser(suppressionPoint2.mConfig.keepRecentTaskNumner, 63, this.mCurrentUserId);
                                    if (this.DEBUG) {
                                        Slog.d("RunningBoosterService", "[onBeforeActivitySwitch] <= getRecentTasks");
                                    }
                                } catch (SecurityException e) {
                                    Slog.e("RunningBoosterService", "getRecentTasks fail ", e);
                                    if (this.DEBUG) {
                                        Slog.d("RunningBoosterService", "[onBeforeActivitySwitch] <= getRecentTasks");
                                    }
                                }
                            } catch (Throwable th) {
                                if (this.DEBUG) {
                                    Slog.d("RunningBoosterService", "[onBeforeActivitySwitch] <= getRecentTasks");
                                }
                                throw th;
                            }
                        }
                    }
                    if (this.DEBUG) {
                        Slog.d("RunningBoosterService", "[onBeforeActivitySwitch] updateOrSuppress point=" + suppressionPoint2.mAppState + "pkgName = " + suppressionPoint2.mPackageName);
                    }
                    if (!z) {
                        synchronized (this.L) {
                            this.aq.put(suppressionPoint2.mSuppressTag, (ArrayList) beforeActivitySwitch.get(AMEventHookData.BeforeActivitySwitch.Index.nextTaskPackageList));
                        }
                    }
                    a(z, suppressionPoint2);
                }
            }
        }
        Iterator<Map.Entry<String, ArrayList<RbConfiguration.SuppressionPoint>>> it3 = this.ac.entrySet().iterator();
        while (it3.hasNext()) {
            for (RbConfiguration.SuppressionPoint suppressionPoint3 : it3.next().getValue()) {
                if (suppressionPointA.equal(suppressionPoint3) || suppressionPointA2.equal(suppressionPoint3)) {
                    if (this.DEBUG) {
                        Slog.d("RunningBoosterService", "[onBeforeActivitySwitch] stopSuppressByTag tag = " + suppressionPoint3.mSuppressTag);
                    }
                    Slog.d("RunningBoosterService", "[onBeforeActivitySwitch] stopSuppressByTag");
                    W.sendMessage(W.obtainMessage(1003, suppressionPoint3));
                }
            }
        }
        at = z;
        if (this.DEBUG) {
            Slog.d("RunningBoosterService", "<= onBeforeActivitySwitch");
        }
    }

    public void onWakefulnessChanged(AMEventHookData.WakefulnessChanged wakefulnessChanged) {
        int i = wakefulnessChanged.getInt(AMEventHookData.WakefulnessChanged.Index.wakefulness);
        boolean z = this.Q;
        Slog.d("RunningBoosterService", "[onWakefulnessChanged] wakefulness = " + i);
        if (1 == i) {
            this.Q = false;
            Slog.d("RunningBoosterService", "[onWakefulnessChanged] SCREEN_ON ");
        }
        if (i == 0) {
            this.Q = true;
            Slog.d("RunningBoosterService", "[onWakefulnessChanged] SCREEN_OFF ");
            W.sendMessage(W.obtainMessage(1005));
        }
        if (this.Q != z) {
            this.R = true;
        }
    }

    public void onAfterActivityResumed(AMEventHookData.AfterActivityResumed afterActivityResumed) {
        Slog.d("RunningBoosterService", "[onAfterActivityResumed] mScreenStateChange = " + this.R + " mScreenOffState = " + this.Q);
        if (this.ab.size() == 0) {
            Slog.d("RunningBoosterService", "[onAfterActivityResumed] mCurrentSuppressPointList is null");
            return;
        }
        if (true == this.R && !this.Q) {
            Slog.d("RunningBoosterService", "[onAfterActivityResumed] mPausePkgNameAfterScreenOff = " + this.V);
            String string = afterActivityResumed.getString(AMEventHookData.AfterActivityResumed.Index.packageName);
            Slog.d("RunningBoosterService", "[onAfterActivityResumed] packageName = " + string + "activityType = " + afterActivityResumed.getInt(AMEventHookData.AfterActivityResumed.Index.activityType));
            Message messageObtainMessage = W.obtainMessage(ANRManager.RENAME_TRACE_FILES_MSG, string);
            int i = SystemProperties.getInt("duraspeed.event.delaytime", SystemService.PHASE_SYSTEM_SERVICES_READY);
            Slog.d("RunningBoosterService", "[onAfterActivityResumed] delaytime = " + i);
            W.removeMessages(ANRManager.RENAME_TRACE_FILES_MSG);
            W.sendMessageDelayed(messageObtainMessage, i);
            this.R = false;
        }
    }

    public void onActivityThreadResumedDone(AMEventHookData.ActivityThreadResumedDone activityThreadResumedDone) {
        Slog.d("RunningBoosterService", "[onActivityThreadResumedDone]");
        Message messageObtainMessage = W.obtainMessage(1007);
        W.removeMessages(1007);
        W.sendMessageDelayed(messageObtainMessage, 500L);
    }

    public void onSystemUserUnlock(AMEventHookData.SystemUserUnlock systemUserUnlock) {
        Slog.d("RunningBoosterService", "[onSystemUserUnlock]");
        e();
    }

    private void g() {
        Trace.traceBegin(64L, "handleActivityResumeDone");
        if (this.am.size() > 0) {
            for (Map.Entry<String, Boolean> entry : this.am.entrySet()) {
                if (entry.getValue().booleanValue()) {
                    String key = entry.getKey();
                    Slog.d("RunningBoosterService", "[onActivityThreadResumedDone] Second kill phase tag = " + key);
                    if (this.ai.get(key).size() > 0 && this.Z != null) {
                        this.Z.suppressPackages(this.ai.get(key), 1064374545, key);
                    }
                    this.am.put(key, false);
                }
            }
        }
        Trace.traceEnd(64L);
    }

    private RbConfiguration a(RbConfiguration rbConfiguration, RbConfiguration rbConfiguration2, int i) {
        Slog.d("RunningBoosterService", "[updateUserConfiguration] SDK version = " + Build.VERSION.SDK_INT);
        SystemProperties.set("mtk.duraspeed.on", String.valueOf(rbConfiguration.enableRunningBooster));
        if (rbConfiguration.adj >= RbConfiguration.AdjValue.PerceptibleAppAdj.getAdjValue() && rbConfiguration.adj <= RbConfiguration.AdjValue.PreviousAppAdj.getAdjValue()) {
            if (Build.VERSION.SDK_INT <= 23) {
                rbConfiguration.adj /= 100;
            }
            rbConfiguration2.adj = rbConfiguration.adj;
        } else {
            Slog.d("RunningBoosterService", "[updateUserConfiguration] adj values is wrong, so use default adj");
            if (Build.VERSION.SDK_INT <= 23) {
                rbConfiguration2.adj = RbConfiguration.AdjValue.PerceptibleAppAdj.getAdjValue() / 100;
            } else {
                rbConfiguration2.adj = RbConfiguration.AdjValue.PerceptibleAppAdj.getAdjValue();
            }
        }
        Slog.d("RunningBoosterService", "[updateUserConfiguration] newConfig.adj = " + rbConfiguration2.adj);
        if (-1000 != rbConfiguration.keepRecentTaskNumner && rbConfiguration.keepRecentTaskNumner < 10) {
            rbConfiguration2.keepRecentTaskNumner = rbConfiguration.keepRecentTaskNumner;
            Slog.d("RunningBoosterService", "[updateUserConfiguration] keepRecentTaskNumner= " + rbConfiguration2.keepRecentTaskNumner);
        }
        if (-1000 != rbConfiguration.keepNotificationAPPNumber) {
            rbConfiguration2.keepNotificationAPPNumber = rbConfiguration.keepNotificationAPPNumber;
            Slog.d("RunningBoosterService", "[updateUserConfiguration] keepNotificationAPPNumber= " + rbConfiguration2.keepNotificationAPPNumber);
        }
        if (rbConfiguration2.checkLocationServiceApp != rbConfiguration.checkLocationServiceApp) {
            rbConfiguration2.checkLocationServiceApp = rbConfiguration.checkLocationServiceApp;
            Slog.d("RunningBoosterService", "[updateUserConfiguration] keepNotificationAPPNumber= " + rbConfiguration2.keepNotificationAPPNumber);
        }
        if (rbConfiguration2.enableLauncherWidget != rbConfiguration.enableLauncherWidget) {
            rbConfiguration2.enableLauncherWidget = rbConfiguration.enableLauncherWidget;
            Slog.d("RunningBoosterService", "[updateUserConfiguration] enableLauncherWidget= " + rbConfiguration2.enableLauncherWidget);
        }
        if (rbConfiguration2.enableRunningBooster != rbConfiguration.enableRunningBooster) {
            rbConfiguration2.enableRunningBooster = rbConfiguration.enableRunningBooster;
            Slog.d("RunningBoosterService", "[updateUserConfiguration] enableRunningBooster= " + rbConfiguration2.enableRunningBooster);
        }
        rbConfiguration2.whiteList = a(rbConfiguration.whiteList, rbConfiguration2.whiteList);
        if (!IS_USER_BUILD || this.DEBUG) {
            Iterator it = rbConfiguration2.whiteList.iterator();
            while (it.hasNext()) {
                Slog.d("RunningBoosterService", "[updateUserConfiguration] whiteList= " + ((String) it.next()));
            }
        }
        rbConfiguration2.blackList = a(rbConfiguration.blackList, rbConfiguration2.blackList);
        rbConfiguration2.suppressPoint = updateSuppressionPoint(i, rbConfiguration.suppressPoint, rbConfiguration2.suppressPoint);
        Slog.d("RunningBoosterService", "[updateUserConfiguration] suppressPoint size = " + rbConfiguration2.suppressPoint.size());
        Iterator it2 = rbConfiguration2.suppressPoint.iterator();
        while (it2.hasNext()) {
            Slog.d("RunningBoosterService", "[updateUserConfiguration] mSuppressTag = " + ((RbConfiguration.SuppressionPoint) it2.next()).mSuppressTag);
        }
        return rbConfiguration2;
    }

    private ArrayList<String> a(ArrayList<String> arrayList, ArrayList<String> arrayList2) {
        if (1 == arrayList.size() && arrayList.contains("initial")) {
            return arrayList2;
        }
        arrayList.remove("initial");
        return arrayList;
    }

    public ArrayList<RbConfiguration.SuppressionPoint> updateSuppressionPoint(int i, ArrayList<RbConfiguration.SuppressionPoint> arrayList, ArrayList<RbConfiguration.SuppressionPoint> arrayList2) {
        Slog.d("RunningBoosterService", "[updateSuppressionPoint] enter");
        if (1 == arrayList.size() && arrayList.get(0).equal(RbConfiguration.DEAULT_STARTPOINT)) {
            return arrayList2;
        }
        arrayList.remove(RbConfiguration.DEAULT_STARTPOINT);
        int i2 = 0;
        for (RbConfiguration.SuppressionPoint suppressionPoint : arrayList) {
            suppressionPoint.mSuppressTag = Integer.toString(i) + "_" + Integer.toString(i2);
            i2++;
            this.al.put(suppressionPoint.mSuppressTag, false);
            Slog.d("RunningBoosterService", "[updateSuppressionPoint] point.mSuppressTag = " + suppressionPoint.mSuppressTag);
        }
        return arrayList;
    }

    private void a(int i, String str) {
        File file = new File(this.M);
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
            try {
                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(this.M, true));
                bufferedWriter.write(String.valueOf(i));
                bufferedWriter.newLine();
                bufferedWriter.write(str);
                bufferedWriter.newLine();
                bufferedWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e2) {
            Slog.w("RunningBoosterService", "Unable to prepare stack trace file ", e2);
        }
    }

    private void h() throws Throwable {
        BufferedReader bufferedReader;
        BufferedReader bufferedReader2 = null;
        try {
            try {
                bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(this.M), "utf-8"));
                while (true) {
                    try {
                        String line = bufferedReader.readLine();
                        if (line != null) {
                            int iIntValue = Integer.valueOf(line).intValue();
                            if (this.aa.get(Integer.valueOf(iIntValue)) == null) {
                                this.aa.put(Integer.valueOf(iIntValue), bufferedReader.readLine());
                            }
                        } else {
                            try {
                                bufferedReader.close();
                                return;
                            } catch (Exception e) {
                                Slog.e("RunningBoosterService", "file closed fail ", e);
                                return;
                            }
                        }
                    } catch (Exception e2) {
                        e = e2;
                        Slog.e("RunningBoosterService", "getPackageList fail ", e);
                        try {
                            bufferedReader.close();
                            return;
                        } catch (Exception e3) {
                            Slog.e("RunningBoosterService", "file closed fail ", e3);
                            return;
                        }
                    }
                }
            } catch (Throwable th) {
                th = th;
                try {
                    bufferedReader2.close();
                } catch (Exception e4) {
                    Slog.e("RunningBoosterService", "file closed fail ", e4);
                }
                throw th;
            }
        } catch (Exception e5) {
            e = e5;
            bufferedReader = null;
        } catch (Throwable th2) {
            th = th2;
            bufferedReader2.close();
            throw th;
        }
    }

    private void a(int i, String str, boolean z) {
        ArrayList<RbConfiguration.SuppressionPoint> arrayList;
        if (this.ab.size() != 0 && (arrayList = this.ab.get(str)) != null) {
            Slog.d("RunningBoosterService", "[clearPolicyAndSuppressList] pkgname = " + str);
            Iterator<RbConfiguration.SuppressionPoint> it = arrayList.iterator();
            while (it.hasNext()) {
                c(it.next());
            }
        }
        i(str);
        this.ab.remove(str);
        this.ac.remove(str);
        if (!z) {
            this.ae.remove(i);
            this.aa.remove(Integer.valueOf(i));
        }
    }

    private void a(RbConfiguration.SuppressionPoint suppressionPoint) throws Throwable {
        BufferedWriter bufferedWriter;
        Trace.traceBegin(64L, "updateAllowList");
        Slog.d("RunningBoosterService", "[updateAllowList] enter");
        RbConfiguration rbConfiguration = suppressionPoint.mConfig;
        ArrayList<String> arrayList = new ArrayList<>();
        ArrayList<String> arrayList2 = new ArrayList<>();
        ArrayList<String> arrayList3 = new ArrayList<>();
        arrayList2.addAll(rbConfiguration.blackList);
        arrayList3.addAll(rbConfiguration.whiteList);
        if (this.DEBUG) {
            Slog.d("RunningBoosterService", "[updateAllowList] => setPackageListByAdj");
        }
        b(rbConfiguration.adj);
        if (this.DEBUG) {
            Slog.d("RunningBoosterService", "[updateAllowList] <= setPackageListByAdj");
        }
        arrayList.addAll(this.au);
        if (this.DEBUG) {
            Slog.d("RunningBoosterService", "[updateAllowList] => getPackageListByRecentTask");
        }
        arrayList.addAll(c(rbConfiguration.keepRecentTaskNumner));
        if (this.DEBUG) {
            Slog.d("RunningBoosterService", "[updateAllowList] <= getPackageListByRecentTask");
        }
        if (this.DEBUG) {
            Slog.d("RunningBoosterService", "[updateAllowList] => getPackageListByNotification");
        }
        arrayList.addAll(d(rbConfiguration.keepNotificationAPPNumber));
        if (this.DEBUG) {
            Slog.d("RunningBoosterService", "[updateAllowList] <= getPackageListByNotification");
        }
        Slog.d("RunningBoosterService", "[updateAllowList] mAllowPkgList.remove mSuppressTag = " + suppressionPoint.mSuppressTag);
        this.af.remove(suppressionPoint.mSuppressTag);
        this.ag.remove(suppressionPoint.mSuppressTag);
        this.ah.remove(suppressionPoint.mSuppressTag);
        this.ai.remove(suppressionPoint.mSuppressTag);
        if (rbConfiguration.enableLauncherWidget) {
            if (this.DEBUG) {
                Slog.d("RunningBoosterService", "[updateAllowList] => getPackageListByAPPWidget");
            }
            arrayList.addAll(i());
            if (this.DEBUG) {
                Slog.d("RunningBoosterService", "[updateAllowList] <= getPackageListByAPPWidget");
            }
        }
        if (rbConfiguration.checkLocationServiceApp) {
            if (this.DEBUG) {
                Slog.d("RunningBoosterService", "[updateAllowList] => getPackageListByLocation");
            }
            arrayList.addAll(j());
            if (this.DEBUG) {
                Slog.d("RunningBoosterService", "[updateAllowList] <= getPackageListByLocation");
            }
        }
        if (!arrayList.contains(this.V)) {
            arrayList.add(this.V);
            Slog.d("RunningBoosterService", "[updateAllowList] mPausePkgNameAfterScreenOff = " + this.V);
        }
        if (!IS_USER_BUILD || this.DEBUG) {
            b("/data/runningbooster/adj_allow_list.txt", this.au);
            b("/data/runningbooster/adj_kill_list.txt", this.av);
            b("/data/runningbooster/recent_list.txt", c(rbConfiguration.keepRecentTaskNumner));
            b("/data/runningbooster/notification_list.txt", d(rbConfiguration.keepNotificationAPPNumber));
            b("/data/runningbooster/appwidget_list.txt", i());
            b("/data/runningbooster/location_list.txt", j());
        }
        if (!IS_USER_BUILD || this.DEBUG) {
            List<ApplicationInfo> installedApplications = this.mContext.getPackageManager().getInstalledApplications(41472);
            if (this.DEBUG) {
                try {
                    bufferedWriter = new BufferedWriter(new FileWriter("/data/runningbooster/install_list.txt", false));
                    try {
                        try {
                            Slog.d("RunningBoosterService", "write install_list.txt");
                            Iterator<ApplicationInfo> it = installedApplications.iterator();
                            while (it.hasNext()) {
                                bufferedWriter.write(it.next().packageName);
                                bufferedWriter.newLine();
                            }
                            try {
                                bufferedWriter.close();
                            } catch (Exception e) {
                                Slog.e("RunningBoosterService", "file closed fail ", e);
                            }
                        } catch (IOException e2) {
                            e = e2;
                            Slog.e("RunningBoosterService", "write install_list.txt fail");
                            e.printStackTrace();
                            try {
                                bufferedWriter.close();
                            } catch (Exception e3) {
                                Slog.e("RunningBoosterService", "file closed fail ", e3);
                            }
                        }
                    } catch (Throwable th) {
                        th = th;
                        try {
                            bufferedWriter.close();
                        } catch (Exception e4) {
                            Slog.e("RunningBoosterService", "file closed fail ", e4);
                        }
                        throw th;
                    }
                } catch (IOException e5) {
                    e = e5;
                    bufferedWriter = null;
                } catch (Throwable th2) {
                    th = th2;
                    bufferedWriter = null;
                    bufferedWriter.close();
                    throw th;
                }
            }
        }
        ArrayList<String> arrayList4 = new ArrayList();
        arrayList4.addAll(arrayList2);
        for (String str : arrayList4) {
            if (arrayList.contains(str)) {
                arrayList3.add(str);
                arrayList2.remove(str);
            }
        }
        b("/data/runningbooster/allow_list_bytype.txt", arrayList);
        this.af.put(suppressionPoint.mSuppressTag, arrayList3);
        this.ag.put(suppressionPoint.mSuppressTag, arrayList2);
        this.al.put(suppressionPoint.mSuppressTag, true);
        Slog.d("RunningBoosterService", "[updateAllowList] exit mSuppressTag = " + suppressionPoint.mSuppressTag);
        Trace.traceEnd(64L);
    }

    private void b(RbConfiguration.SuppressionPoint suppressionPoint) throws Throwable {
        Trace.traceBegin(64L, "startSuppress");
        if (true == this.al.get(suppressionPoint.mSuppressTag).booleanValue()) {
            Slog.d("RunningBoosterService", "[startSuppress] start to suppress tag = " + suppressionPoint.mSuppressTag + " mIsUpdateAllowList = " + this.al.get(suppressionPoint.mSuppressTag));
            this.al.put(suppressionPoint.mSuppressTag, false);
        } else {
            Slog.d("RunningBoosterService", "[startSuppress] Need to update allow list mSuppressTag = " + suppressionPoint.mSuppressTag);
            a(suppressionPoint);
        }
        synchronized (this.L) {
            g(suppressionPoint.mSuppressTag);
        }
        this.X.startFrc(suppressionPoint.mSuppressTag, 3, this.af.get(suppressionPoint.mSuppressTag));
        this.Y.startSuppression(suppressionPoint.mSuppressTag, 7, 1064374545, suppressionPoint.mSuppressTag, this.af.get(suppressionPoint.mSuppressTag));
        Slog.d("RunningBoosterService", "[startSuppress] mKillPkgList size = " + this.ah.get(suppressionPoint.mSuppressTag).size());
        Slog.d("RunningBoosterService", "[startSuppress] first kill phase");
        if (this.Z != null) {
            this.Z.suppressPackages(this.ah.get(suppressionPoint.mSuppressTag), 1064374545, suppressionPoint.mSuppressTag);
        }
        this.am.put(suppressionPoint.mSuppressTag, true);
        if (!this.ak.contains(suppressionPoint.mSuppressTag)) {
            this.ak.add(suppressionPoint.mSuppressTag);
        }
        Slog.d("RunningBoosterService", "[startSuppress] Suppress exit");
        Trace.traceEnd(64L);
    }

    private void g(String str) throws Throwable {
        Slog.d("RunningBoosterService", "[updateTwoPhaseKillList] suppressTag = " + str);
        ArrayList<String> arrayList = this.ag.get(str);
        ArrayList<String> arrayList2 = this.af.get(str);
        ArrayList<String> arrayList3 = this.aq.get(str);
        if (arrayList3 != null) {
            for (String str2 : arrayList3) {
                Slog.d("RunningBoosterService", "[updateTwoPhaseKillList] pkgName = " + str2);
                if (!arrayList2.contains(str2)) {
                    arrayList2.add(str2);
                    arrayList.remove(str2);
                }
            }
        }
        ArrayList<String> arrayList4 = new ArrayList<>();
        ArrayList<String> arrayList5 = new ArrayList<>();
        for (String str3 : arrayList) {
            if (this.av.contains(str3)) {
                if (!IS_USER_BUILD || this.DEBUG) {
                    Slog.d("RunningBoosterService", "[updateTwoPhaseKillList] tmpFirstKillPkgList pkg = " + str3);
                }
                arrayList4.add(str3);
            } else {
                if (!IS_USER_BUILD || this.DEBUG) {
                    Slog.d("RunningBoosterService", "[updateTwoPhaseKillList] tmpSecondKillPkgList pjg = " + str3);
                }
                arrayList5.add(str3);
            }
        }
        this.af.put(str, arrayList2);
        this.ag.put(str, arrayList);
        this.ah.put(str, arrayList4);
        this.ai.put(str, arrayList5);
        b("/data/runningbooster/allow_list.txt", arrayList2);
        b("/data/runningbooster/kill_list.txt", arrayList);
    }

    private void c(RbConfiguration.SuppressionPoint suppressionPoint) {
        Slog.d("RunningBoosterService", "[stopSuppressByTag] point.mSuppressTag = " + suppressionPoint.mSuppressTag);
        this.X.stopFrc(suppressionPoint.mSuppressTag);
        this.Y.stopSuppression(suppressionPoint.mSuppressTag);
        if (this.Z != null) {
            this.Z.unsuppressPackages(suppressionPoint.mSuppressTag);
        }
        this.ak.remove(suppressionPoint.mSuppressTag);
        this.am.put(suppressionPoint.mSuppressTag, false);
    }

    private void b(int i) {
        ActivityManager activityManager = (ActivityManager) this.mContext.getSystemService("activity");
        this.au.clear();
        this.av.clear();
        synchronized (this.K) {
            for (Map.Entry<Integer, ArrayList<Integer>> entry : this.ao.entrySet()) {
                int iIntValue = entry.getKey().intValue();
                Iterator<Integer> it = entry.getValue().iterator();
                while (it.hasNext()) {
                    String[] packageListFromPid = activityManager.getPackageListFromPid(it.next().intValue());
                    if (packageListFromPid != null) {
                        if (iIntValue <= i) {
                            for (String str : packageListFromPid) {
                                this.au.add(str);
                            }
                        } else {
                            for (String str2 : packageListFromPid) {
                                this.av.add(str2);
                            }
                        }
                    }
                }
            }
        }
    }

    private ArrayList<String> c(int i) {
        ArrayList<String> arrayList = new ArrayList<>();
        synchronized (this.J) {
            if (this.ap.size() == 0) {
                return arrayList;
            }
            Iterator<ActivityManager.RecentTaskInfo> it = this.ap.iterator();
            for (int i2 = 0; it.hasNext() && i2 < i; i2++) {
                ActivityManager.RecentTaskInfo next = it.next();
                if (next.realActivity != null) {
                    arrayList.add(next.realActivity.getPackageName());
                    Slog.d("RunningBoosterService", "recentPackage realActivity : " + next.realActivity.getPackageName());
                    if (next.topActivity != null && !next.realActivity.getPackageName().equals(next.topActivity.getPackageName())) {
                        arrayList.add(next.topActivity.getPackageName());
                        Slog.d("RunningBoosterService", "recentPackage topActivity : " + next.topActivity.getPackageName());
                    }
                }
            }
            return arrayList;
        }
    }

    private ArrayList<String> d(int i) {
        Notification notification;
        StatusBarNotification[] activeNotifications = null;
        INotificationManager iNotificationManagerAsInterface = INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
        ArrayList<String> arrayList = new ArrayList<>();
        try {
            activeNotifications = iNotificationManagerAsInterface.getActiveNotifications(this.mContext.getPackageName());
        } catch (RemoteException e) {
            Slog.e("RunningBoosterService", "getPackageListByNotification fail ", e);
        }
        if (activeNotifications == null) {
            return arrayList;
        }
        if (i > activeNotifications.length || i == -1) {
            i = activeNotifications.length;
        }
        for (int i2 = 0; i2 < i; i2++) {
            if (!arrayList.contains(activeNotifications[i2].getPackageName()) && (notification = activeNotifications[i2].getNotification()) != null && notification.getSmallIcon() != null && (notification.flags & 268435456) == 0) {
                arrayList.add(activeNotifications[i2].getPackageName());
                Slog.d("RunningBoosterService", "notificationPackagelist : " + activeNotifications[i2].getPackageName());
            }
        }
        return arrayList;
    }

    private ArrayList<String> i() {
        AppWidgetManager appWidgetManager = (AppWidgetManager) this.mContext.getSystemService("appwidget");
        ArrayList<String> arrayList = new ArrayList<>();
        PackageManager packageManager = this.mContext.getPackageManager();
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        ResolveInfo resolveInfoResolveActivity = packageManager.resolveActivity(intent, PackageManagerService.DumpState.DUMP_INSTALLS);
        if (resolveInfoResolveActivity != null) {
            String str = resolveInfoResolveActivity.activityInfo.packageName;
            Slog.d("RunningBoosterService", "Current launcherName : " + str);
            List<ComponentName> appWidgetOfHost = appWidgetManager.getAppWidgetOfHost(str, this.mCurrentUserId);
            if (appWidgetOfHost == null) {
                return arrayList;
            }
            for (ComponentName componentName : appWidgetOfHost) {
                arrayList.add(componentName.getPackageName());
                Slog.d("RunningBoosterService", "widgetPackagelist : " + componentName.getPackageName());
            }
            return arrayList;
        }
        Slog.d("RunningBoosterService", "No Launcher");
        return arrayList;
    }

    private ArrayList<String> j() {
        List packagesForOps = ((AppOpsManager) this.mContext.getSystemService("appops")).getPackagesForOps(new int[]{42});
        ArrayList<String> arrayList = new ArrayList<>();
        if (packagesForOps != null) {
            int size = packagesForOps.size();
            for (int i = 0; i < size; i++) {
                AppOpsManager.PackageOps packageOps = (AppOpsManager.PackageOps) packagesForOps.get(i);
                List ops = packageOps.getOps();
                if (ops != null) {
                    int size2 = ops.size();
                    int i2 = 0;
                    while (true) {
                        if (i2 < size2) {
                            AppOpsManager.OpEntry opEntry = (AppOpsManager.OpEntry) ops.get(i2);
                            if (opEntry.getOp() != 42 || !opEntry.isRunning()) {
                                i2++;
                            } else {
                                arrayList.add(packageOps.getPackageName());
                                Slog.d("RunningBoosterService", "locationPackagelist : " + packageOps.getPackageName());
                                break;
                            }
                        }
                    }
                }
            }
        }
        return arrayList;
    }

    private void a(boolean z, RbConfiguration.SuppressionPoint suppressionPoint) {
        Slog.d("RunningBoosterService", "[updateOrSuppress] finishPause= " + z);
        if (true == z) {
            synchronized (this.af) {
                Slog.d("RunningBoosterService", "[updateOrSuppress] update allow list");
                W.sendMessage(W.obtainMessage(ANRManager.START_MONITOR_BROADCAST_TIMEOUT_MSG, suppressionPoint));
            }
            return;
        }
        synchronized (this.af) {
            Slog.d("RunningBoosterService", "[updateOrSuppress] start suppress");
            W.sendMessage(W.obtainMessage(ANRManager.START_MONITOR_SERVICE_TIMEOUT_MSG, suppressionPoint));
        }
    }

    public class AmsEventHandler extends Handler {
        public AmsEventHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) throws Throwable {
            switch (message.what) {
                case ANRManager.START_MONITOR_BROADCAST_TIMEOUT_MSG:
                    RunningBoosterService.this.a((RbConfiguration.SuppressionPoint) message.obj);
                    break;
                case ANRManager.START_MONITOR_SERVICE_TIMEOUT_MSG:
                    RunningBoosterService.this.b((RbConfiguration.SuppressionPoint) message.obj);
                    break;
                case 1003:
                    RunningBoosterService.this.c((RbConfiguration.SuppressionPoint) message.obj);
                    break;
                case 1004:
                    RunningBoosterService.this.l();
                    break;
                case 1005:
                    RunningBoosterService.this.k();
                    break;
                case ANRManager.RENAME_TRACE_FILES_MSG:
                    RunningBoosterService.this.h((String) message.obj);
                    break;
                case 1007:
                    RunningBoosterService.this.g();
                    break;
            }
        }
    }

    public static RunningBoosterService getInstance(AMEventHookData.SystemReady systemReady) {
        if (I == null) {
            I = new RunningBoosterService((Context) systemReady.get(AMEventHookData.SystemReady.Index.context));
        }
        return I;
    }

    private RbConfiguration.SuppressionPoint a(int i, String str, int i2) {
        switch (i) {
            case 1:
            case 2:
                str = "launcher";
                break;
        }
        return new RbConfiguration.SuppressionPoint(str, i2);
    }

    private void k() {
        Slog.d("RunningBoosterService", "[handleScreenOffEvent] enter");
        W.removeMessages(1007);
        W.removeMessages(ANRManager.RENAME_TRACE_FILES_MSG);
        if (this.ak.size() == 0) {
            Slog.d("RunningBoosterService", "[handleScreenOffEvent] mSuppressTagList is null");
            return;
        }
        this.ad.clear();
        ArrayList<String> arrayList = new ArrayList();
        arrayList.addAll(this.ak);
        for (String str : arrayList) {
            Slog.d("RunningBoosterService", "[handleScreenOffEvent] unsuppress tag = " + str);
            ArrayList<String> frcPackageList = this.X.getFrcPackageList(str);
            if (frcPackageList != null) {
                this.ad.put(str, frcPackageList);
            }
            this.Y.stopSuppression(str);
            if (this.Z != null) {
                this.Z.unsuppressPackages(str);
            }
            this.ak.remove(str);
        }
        Slog.d("RunningBoosterService", "[handleScreenOffEvent] end");
    }

    private void h(String str) {
        Trace.traceBegin(64L, "handleScreenOnEvent");
        if (this.ad.size() == 0) {
            Slog.d("RunningBoosterService", "[handleScreenOnEvent] mCurrentFrcList is null");
            return;
        }
        W.removeMessages(1007);
        Slog.d("RunningBoosterService", "[handleScreenOnEvent] resumePkgName = " + str);
        new ArrayList();
        ArrayList<String> arrayList = new ArrayList<>();
        new ArrayList();
        for (Map.Entry<String, ArrayList<String>> entry : this.ad.entrySet()) {
            String key = entry.getKey();
            ArrayList<String> value = entry.getValue();
            if (this.DEBUG && value != null) {
                Iterator<String> it = value.iterator();
                while (it.hasNext()) {
                    Slog.d("RunningBoosterService", "[handleScreenOnEvent] tmpFrcList pkg = " + it.next());
                }
            }
            arrayList.addAll(this.af.get(key));
            arrayList.addAll(value);
            Slog.d("RunningBoosterService", "[handleScreenOnEvent] suppress tag = " + key);
            if (!this.V.equals(str)) {
                arrayList.add(str);
            }
            this.af.put(key, arrayList);
            ArrayList<String> arrayList2 = this.ag.get(key);
            for (String str2 : arrayList) {
                if (arrayList2.contains(str2)) {
                    arrayList2.remove(str2);
                    Slog.d("RunningBoosterService", "[handleScreenOnEvent] tmpKillList remove pkg = " + str2);
                }
            }
            this.ag.put(key, arrayList2);
            this.X.updateFrcExtraAllowList(key, arrayList);
            this.Y.startSuppression(key, 7, 1064374545, key, arrayList);
            Slog.d("RunningBoosterService", "[handleScreenOnEvent] kill process start");
            if (this.Z != null) {
                this.Z.suppressPackages(arrayList2, 1064374545, key);
            }
            if (!this.ak.contains(key)) {
                this.ak.add(key);
            }
            Slog.d("RunningBoosterService", "[handleScreenOnEvent] kill process end");
        }
        Trace.traceEnd(64L);
    }

    private void l() {
        Slog.d("RunningBoosterService", "[handleUserSwitchEvent]");
        if (this.ak.size() != 0) {
            for (String str : this.ak) {
                Slog.d("RunningBoosterService", "[handleUserSwitchEvent] unsuppress tag = " + str);
                this.X.stopFrc(str);
                this.Y.stopSuppression(str);
                if (this.Z != null) {
                    this.Z.unsuppressPackages(str);
                }
            }
        }
        this.ab.clear();
        this.ac.clear();
        this.ak.clear();
        this.ad.clear();
        this.af.clear();
        this.ag.clear();
        this.ah.clear();
        this.ai.clear();
        this.am.clear();
        this.al.clear();
    }

    private void i(String str) {
        Slog.d("RunningBoosterService", "[clearSuppressionData]");
        if (this.ab.size() != 0) {
            for (RbConfiguration.SuppressionPoint suppressionPoint : this.ab.get(str)) {
                Slog.d("RunningBoosterService", "[clearSuppressionData] remove tag = " + suppressionPoint.mSuppressTag);
                this.ak.remove(suppressionPoint.mSuppressTag);
                this.ad.remove(suppressionPoint.mSuppressTag);
            }
        }
    }

    private void j(String str) {
        Slog.i("RunningBoosterService", "prepareStackTraceFile: " + str);
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
            Slog.e("RunningBoosterService", "Unable to prepare stack trace file: " + str, e);
        }
    }

    private void a(RbConfiguration rbConfiguration) {
        Slog.d("RunningBoosterService", "PerceptibleAppAdj = " + RbConfiguration.AdjValue.PerceptibleAppAdj.getAdjValue());
        Slog.d("RunningBoosterService", "adj=" + rbConfiguration.adj);
        Slog.d("RunningBoosterService", "keepRecentTaskNumner = " + rbConfiguration.keepRecentTaskNumner);
        Slog.d("RunningBoosterService", "keepNotificationAPPNumber = " + rbConfiguration.keepNotificationAPPNumber);
        Slog.d("RunningBoosterService", "checkLocationServiceApp = " + rbConfiguration.checkLocationServiceApp);
        Slog.d("RunningBoosterService", "enableLauncherWidget = " + rbConfiguration.enableLauncherWidget);
        Slog.d("RunningBoosterService", "enableRunningBooster = " + rbConfiguration.enableRunningBooster);
        if (this.DEBUG) {
            Iterator it = rbConfiguration.whiteList.iterator();
            while (it.hasNext()) {
                Slog.d("RunningBoosterService", "whiteList = " + ((String) it.next()));
            }
            Iterator it2 = rbConfiguration.blackList.iterator();
            while (it2.hasNext()) {
                Slog.d("RunningBoosterService", "blackList = " + ((String) it2.next()));
            }
        }
        for (RbConfiguration.SuppressionPoint suppressionPoint : rbConfiguration.suppressPoint) {
            Slog.d("RunningBoosterService", "point.mPackageName = " + suppressionPoint.mPackageName + " point.mAppState = " + suppressionPoint.mAppState + " point.mSuppressTag = " + suppressionPoint.mSuppressTag);
        }
    }

    private void b(String str, ArrayList<String> arrayList) throws Throwable {
        BufferedWriter bufferedWriter;
        if ((!IS_USER_BUILD || this.DEBUG) && arrayList != null) {
            try {
                bufferedWriter = new BufferedWriter(new FileWriter(str, false));
                try {
                    try {
                        Slog.d("RunningBoosterService", "saveDataToFile");
                        for (String str2 : arrayList) {
                            if (str2 != null) {
                                bufferedWriter.write(str2);
                                bufferedWriter.newLine();
                            }
                        }
                        try {
                            bufferedWriter.close();
                        } catch (Exception e) {
                            Slog.e("RunningBoosterService", "file closed fail ", e);
                        }
                    } catch (IOException e2) {
                        e = e2;
                        Slog.e("RunningBoosterService", "saveDataToFile fail");
                        e.printStackTrace();
                        try {
                            bufferedWriter.close();
                        } catch (Exception e3) {
                            Slog.e("RunningBoosterService", "file closed fail ", e3);
                        }
                    }
                } catch (Throwable th) {
                    th = th;
                    try {
                        bufferedWriter.close();
                    } catch (Exception e4) {
                        Slog.e("RunningBoosterService", "file closed fail ", e4);
                    }
                    throw th;
                }
            } catch (IOException e5) {
                e = e5;
                bufferedWriter = null;
            } catch (Throwable th2) {
                th = th2;
                bufferedWriter = null;
                bufferedWriter.close();
                throw th;
            }
        }
    }

    private static IPackageManager a() {
        IPackageManager iPackageManagerAsInterface = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        if (iPackageManagerAsInterface == null) {
            throw new RuntimeException("null package manager service");
        }
        return iPackageManagerAsInterface;
    }
}
