package com.android.internal.app;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.IWallpaperManager;
import android.app.WallpaperInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ComponentInfo;
import android.content.pm.IPackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.wifi.IWifiManager;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.StrictMode;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import android.view.IWindowManager;
import com.android.server.pm.PackageManagerService;
import com.mediatek.ipomanager.ActivityManagerPlusConnection;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ShutdownManager {
    private static final String IPOWiFiEnable = "persist.sys.ipo.wifi";
    private static final String TAG = "ShutdownManager";
    private static int airplaneModeState;
    private static boolean isBSPPackage;
    private static PowerManager mPowerManager;
    private static Handler sHandler;
    private static int sdState;
    private static int wifiState;
    private static boolean doAudioUnmute = false;
    private static boolean setMusicMuted = false;
    private static boolean mMerged = false;
    private static ShutdownManager sInstance = null;
    public static ArrayList<String> sShutdownWhiteList = new ArrayList<>();
    static final String[] mHardCodeShutdownList = {"system", "com.mediatek.bluetooth", "com.android.phone", "android.process.acore", "com.android.wallpaper", "com.android.systemui", "com.mediatek.mobilelog", "com.mediatek.atci.service"};
    static ArrayList<String> sPrebootKillList = new ArrayList<>();
    static final String[] mHardCodePrebootKillList = {"com.google.android.setupwizard", "com.android.phone", "com.mediatek.ims", "com.mediatek.wfo.impl"};

    public static native int GetMasterMute();

    public static native int GetStreamMute(int i);

    public static native int SetMasterMute(boolean z);

    public static native int SetStreamMute(int i, boolean z);

    static {
        isBSPPackage = false;
        Slog.v(TAG, "ShutdownManager constructor is called");
        for (int i = 0; i < mHardCodeShutdownList.length; i++) {
            sShutdownWhiteList.add(mHardCodeShutdownList[i]);
        }
        String str = SystemProperties.get("persist.ipo.shutdown.process.wl", (String) null);
        if (str != null) {
            Slog.i(TAG, "Process whitelist: " + str);
            ArrayList arrayList = new ArrayList();
            parseStringIntoArrary("/", str, arrayList);
            for (int i2 = 0; i2 < arrayList.size(); i2++) {
                String str2 = (String) arrayList.get(i2);
                if (str2.length() > 0) {
                    if (str2.startsWith("!") && str2.length() > 1 && sShutdownWhiteList.contains(str2.substring(1))) {
                        sShutdownWhiteList.remove(str2.substring(1));
                    } else if (!str2.startsWith("!") && !str2.matches("^\\s*$") && !sShutdownWhiteList.contains(str2)) {
                        sShutdownWhiteList.add(str2);
                    }
                }
            }
        }
        for (int i3 = 0; i3 < mHardCodePrebootKillList.length; i3++) {
            sPrebootKillList.add(mHardCodePrebootKillList[i3]);
        }
        String str3 = SystemProperties.get("persist.ipo.prebootkill.list", (String) null);
        if (str3 != null) {
            Slog.i(TAG, "Process PrebootKillList: " + str3);
            ArrayList arrayList2 = new ArrayList();
            parseStringIntoArrary("/", str3, arrayList2);
            for (int i4 = 0; i4 < arrayList2.size(); i4++) {
                String str4 = (String) arrayList2.get(i4);
                if (str4.length() > 0) {
                    if (str4.startsWith("!") && str4.length() > 1 && sPrebootKillList.contains(str4.substring(1))) {
                        sPrebootKillList.remove(str4.substring(1));
                    } else if (!str4.startsWith("!") && !str4.matches("^\\s*$") && !sPrebootKillList.contains(str4)) {
                        sPrebootKillList.add(str4);
                    }
                }
            }
        }
        isBSPPackage = SystemProperties.get("ro.mtk_bsp_package").equals("1");
        sHandler = new Handler() {
        };
    }

    private static void parseStringIntoArrary(String str, String str2, ArrayList<String> arrayList) {
        for (String str3 : str2.split(str)) {
            arrayList.add(str3);
        }
    }

    public static boolean addShutdownWhiteList(String str) {
        if (sShutdownWhiteList.contains(str)) {
            Slog.w(TAG, "duplicated whitelist: " + str);
            return false;
        }
        sShutdownWhiteList.add(str);
        Slog.v(TAG, "add whitelist: " + str);
        return true;
    }

    public static boolean removeShutdownWhiteList(String str) {
        if (!sShutdownWhiteList.contains(str)) {
            return false;
        }
        sShutdownWhiteList.remove(str);
        Slog.v(TAG, "remove whitelist: " + str);
        return true;
    }

    public static boolean inShutdownWhiteList(String str) {
        return str != null && sShutdownWhiteList.contains(str);
    }

    public static boolean addPrebootKillProcess(String str) {
        if (sPrebootKillList.contains(str)) {
            return false;
        }
        sPrebootKillList.add(str);
        Slog.v(TAG, "add PrebootKill: " + str);
        return true;
    }

    public static boolean removePrebootKillProcess(String str) {
        if (!sPrebootKillList.contains(str)) {
            return false;
        }
        sPrebootKillList.remove(str);
        Slog.v(TAG, "remove PrebootKill: " + str);
        return true;
    }

    public void ShutdownManager() {
    }

    public static ShutdownManager getInstance() {
        if (sInstance == null) {
            sInstance = new ShutdownManager();
        }
        return sInstance;
    }

    private void muteSystem(Context context) {
        if (!isBSPPackage) {
            if (1 == GetMasterMute()) {
                doAudioUnmute = false;
            } else {
                doAudioUnmute = true;
                SetMasterMute(true);
            }
        }
    }

    private void unmuteSystem(Context context) {
        if (!isBSPPackage && doAudioUnmute) {
            SetMasterMute(false);
        }
    }

    public void recoverSystem(Context context) {
    }

    String getCurrentIME(Context context) {
        String string = Settings.Secure.getString(context.getContentResolver(), "default_input_method");
        if (string == null) {
            return null;
        }
        return string.substring(0, string.indexOf("/"));
    }

    public ArrayList<String> getAccessibilityServices(Context context) {
        if (Settings.Secure.getInt(context.getContentResolver(), "accessibility_enabled", 0) == 0) {
            Slog.i(TAG, "accessibility is disabled");
            return null;
        }
        String string = Settings.Secure.getString(context.getContentResolver(), "enabled_accessibility_services");
        if (string == null || string.equals("")) {
            Slog.i(TAG, "no accessibility services exist");
            return null;
        }
        TextUtils.SimpleStringSplitter simpleStringSplitter = new TextUtils.SimpleStringSplitter(':');
        simpleStringSplitter.setString(string);
        ArrayList<String> arrayList = new ArrayList<>();
        while (simpleStringSplitter.hasNext()) {
            String next = simpleStringSplitter.next();
            if (next != null && next.length() > 0) {
                ComponentName componentNameUnflattenFromString = ComponentName.unflattenFromString(next);
                arrayList.add(componentNameUnflattenFromString.getPackageName());
                Log.v(TAG, "AccessibilityService Package Name = " + componentNameUnflattenFromString.getPackageName());
            }
        }
        return arrayList;
    }

    public static int prebootKillProcessListSize() {
        return sPrebootKillList.size();
    }

    public void prebootKillProcess(Context context) {
        IActivityManager iActivityManagerAsInterface = ActivityManagerNative.asInterface(ServiceManager.checkService("activity"));
        Iterator<String> it = sPrebootKillList.iterator();
        while (it.hasNext()) {
            Slog.v(TAG, "PrebootKill = " + it.next());
        }
        if (iActivityManagerAsInterface != null && sPrebootKillList.size() > 0) {
            try {
                for (ActivityManager.RunningAppProcessInfo runningAppProcessInfo : iActivityManagerAsInterface.getRunningAppProcesses()) {
                    if (sPrebootKillList.contains(runningAppProcessInfo.processName)) {
                        Slog.i(TAG, "killProcess: " + runningAppProcessInfo.processName);
                        Process.killProcess(runningAppProcessInfo.pid);
                    }
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "RemoteException: " + e);
            }
        }
    }

    public void forceStopKillPackages(Context context) {
        int i;
        boolean z;
        IActivityManager iActivityManagerAsInterface = ActivityManagerNative.asInterface(ServiceManager.checkService("activity"));
        IPackageManager iPackageManagerAsInterface = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        IWallpaperManager iWallpaperManagerAsInterface = IWallpaperManager.Stub.asInterface(ServiceManager.getService("wallpaper"));
        Iterator<String> it = sShutdownWhiteList.iterator();
        while (it.hasNext()) {
            Slog.v(TAG, "whitelist = " + it.next());
        }
        if (iPackageManagerAsInterface != null && iActivityManagerAsInterface != null && iWallpaperManagerAsInterface != null) {
            try {
                WallpaperInfo wallpaperInfo = iWallpaperManagerAsInterface.getWallpaperInfo();
                String packageName = wallpaperInfo == null ? null : wallpaperInfo.getPackageName();
                String str = wallpaperInfo == null ? null : wallpaperInfo.getServiceInfo().processName;
                int packageUid = iPackageManagerAsInterface.getPackageUid(packageName, PackageManagerService.DumpState.DUMP_INTENT_FILTER_VERIFIERS, ActivityManager.getCurrentUser());
                Slog.v(TAG, "Current Wallpaper = " + packageName + "(" + str + "), uid = " + packageUid);
                String currentIME = getCurrentIME(context);
                Slog.v(TAG, "Current IME: " + currentIME);
                for (ActivityManager.RunningServiceInfo runningServiceInfo : iActivityManagerAsInterface.getServices(30, 0)) {
                    if (0 != runningServiceInfo.restarting && !sShutdownWhiteList.contains(runningServiceInfo.service.getPackageName()) && !runningServiceInfo.service.getPackageName().equals(packageName) && !runningServiceInfo.service.getPackageName().equals(currentIME) && !runningServiceInfo.service.getPackageName().contains(currentIME)) {
                        Slog.v(TAG, "force stop the scheduling service:" + runningServiceInfo.service.getPackageName());
                        iActivityManagerAsInterface.forceKillPackage(runningServiceInfo.service.getPackageName(), -1, "IPO_FORCEKILL");
                    }
                }
                List runningAppProcesses = iActivityManagerAsInterface.getRunningAppProcesses();
                ArrayList<String> accessibilityServices = getAccessibilityServices(context);
                ArrayList arrayList = new ArrayList();
                Intent intent = new Intent("android.intent.action.MAIN");
                intent.addCategory("android.intent.category.HOME");
                List<ResolveInfo> list = iPackageManagerAsInterface.queryIntentActivities(intent, (String) null, 0, 0).getList();
                if (list.size() > 0) {
                    for (ResolveInfo resolveInfo : list) {
                        ComponentInfo componentInfo = resolveInfo.activityInfo == null ? resolveInfo.serviceInfo : resolveInfo.activityInfo;
                        if (componentInfo.processName != null) {
                            Slog.i(TAG, "home process: " + componentInfo.processName);
                            Iterator it2 = runningAppProcesses.iterator();
                            while (true) {
                                if (it2.hasNext()) {
                                    ActivityManager.RunningAppProcessInfo runningAppProcessInfo = (ActivityManager.RunningAppProcessInfo) it2.next();
                                    if (runningAppProcessInfo.processName.equals(componentInfo.processName)) {
                                        Slog.i(TAG, "found running home process shown in above log");
                                        runningAppProcesses.remove(runningAppProcessInfo);
                                        runningAppProcesses.add(0, runningAppProcessInfo);
                                        arrayList.add(runningAppProcessInfo.processName);
                                        break;
                                    }
                                }
                            }
                        } else {
                            Slog.i(TAG, "query home process name fail!");
                        }
                    }
                } else {
                    Slog.i(TAG, "query home activity fail!");
                }
                Iterator it3 = runningAppProcesses.iterator();
                while (true) {
                    if (!it3.hasNext()) {
                        i = -1;
                        break;
                    }
                    ActivityManager.RunningAppProcessInfo runningAppProcessInfo2 = (ActivityManager.RunningAppProcessInfo) it3.next();
                    if (runningAppProcessInfo2.processName.contains(currentIME)) {
                        int i2 = runningAppProcessInfo2.uid;
                        Slog.v(TAG, "Current IME uid: " + i2);
                        i = i2;
                        break;
                    }
                }
                Iterator it4 = runningAppProcesses.iterator();
                while (true) {
                    if (!it4.hasNext()) {
                        break;
                    }
                    ActivityManager.RunningAppProcessInfo runningAppProcessInfo3 = (ActivityManager.RunningAppProcessInfo) it4.next();
                    boolean z2 = false;
                    Slog.v(TAG, "processName: " + runningAppProcessInfo3.processName + " pid: " + runningAppProcessInfo3.pid + " uid: " + runningAppProcessInfo3.uid);
                    if (sShutdownWhiteList.contains(runningAppProcessInfo3.processName) || runningAppProcessInfo3.processName.equals(str) || runningAppProcessInfo3.processName.contains(currentIME) || (runningAppProcessInfo3.processName.equals("com.google.android.apps.genie.geniewidget") && str != null && str.equals("com.google.android.apps.maps:MapsWallpaper"))) {
                        if (!runningAppProcessInfo3.processName.contains(currentIME)) {
                            z = false;
                        } else {
                            z2 = true;
                            z = false;
                        }
                    } else if (runningAppProcessInfo3.uid == 1000) {
                        Slog.v(TAG, "process = " + runningAppProcessInfo3.processName);
                        z = true;
                    } else if (runningAppProcessInfo3.uid == packageUid) {
                        if (runningAppProcessInfo3.processName.equals(str)) {
                            z = true;
                        } else {
                            Slog.i(TAG, "wallpaper related process = " + runningAppProcessInfo3.processName);
                            z = false;
                            z2 = true;
                        }
                    } else if (i != -1 && runningAppProcessInfo3.uid == i) {
                        if (runningAppProcessInfo3.processName.contains(currentIME)) {
                            z = true;
                        } else {
                            Slog.i(TAG, "IME related process = " + runningAppProcessInfo3.processName);
                            z = false;
                            z2 = true;
                        }
                    } else {
                        String[] packagesForUid = iPackageManagerAsInterface.getPackagesForUid(runningAppProcessInfo3.uid);
                        int length = packagesForUid != null ? packagesForUid.length : 0;
                        int i3 = 0;
                        while (true) {
                            if (i3 >= length) {
                                z = true;
                                break;
                            } else if (!sShutdownWhiteList.contains(packagesForUid[i3])) {
                                i3++;
                            } else {
                                Slog.v(TAG, "uid-process = " + runningAppProcessInfo3.processName + ", whitelist item = " + packagesForUid[i3]);
                                z = false;
                                break;
                            }
                        }
                    }
                    if (z) {
                        for (int i4 = 0; i4 < runningAppProcessInfo3.pkgList.length; i4++) {
                            if (accessibilityServices != null && accessibilityServices.contains(runningAppProcessInfo3.pkgList[i4])) {
                                Slog.i(TAG, "skip accessibility service: " + runningAppProcessInfo3.pkgList[i4]);
                            } else {
                                Slog.i(TAG, "forceStopPackage: " + runningAppProcessInfo3.processName + " pid: " + runningAppProcessInfo3.pid);
                                iActivityManagerAsInterface.forceKillPackage(runningAppProcessInfo3.pkgList[i4], -1, "IPO_FORCEKILL");
                            }
                        }
                    }
                    if (z2) {
                        Slog.i(TAG, "killProcess: " + runningAppProcessInfo3.processName);
                        Process.killProcess(runningAppProcessInfo3.pid);
                    }
                    if (ActivityManagerPlusConnection.inBooting()) {
                        Slog.w(TAG, "stop killing for IPO boot");
                        break;
                    }
                }
                if (!ActivityManagerPlusConnection.inBooting()) {
                    List list2 = iActivityManagerAsInterface.getRecentTasks(30, 15, -2).getList();
                    for (int i5 = 0; i5 < list2.size(); i5++) {
                        if (((ActivityManager.RecentTaskInfo) list2.get(i5)).baseActivity != null) {
                            String packageName2 = ((ActivityManager.RecentTaskInfo) list2.get(i5)).baseActivity.getPackageName();
                            if (!sShutdownWhiteList.contains(packageName2)) {
                                Slog.i(TAG, "forceStopPackage: " + packageName2 + " in recentTaskList");
                                iActivityManagerAsInterface.forceKillPackage(packageName2, -1, "IPO_FORCEKILL");
                            }
                        }
                    }
                } else {
                    Slog.w(TAG, "Stop killing recentTaskList");
                }
                List<ActivityManager.ProcessErrorStateInfo> processesInErrorState = iActivityManagerAsInterface.getProcessesInErrorState();
                if (processesInErrorState != null) {
                    for (ActivityManager.ProcessErrorStateInfo processErrorStateInfo : processesInErrorState) {
                        Slog.i(TAG, "killProcess " + processErrorStateInfo.processName + " in '" + processErrorStateInfo.shortMsg + " state");
                        Process.killProcess(processErrorStateInfo.pid);
                    }
                    return;
                }
                Slog.i(TAG, "No process in error state");
            } catch (RemoteException e) {
                Slog.e(TAG, "RemoteException: " + e);
            }
        }
    }

    public void shutdown(Context context) {
        muteSystem(context);
        Intent intent = new Intent("android.intent.action.black.mode");
        intent.putExtra("_black_mode", true);
        context.sendBroadcast(intent);
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        mPowerManager = (PowerManager) context.getSystemService("power");
        mPowerManager.goToSleep(SystemClock.uptimeMillis());
        try {
            boolean z = Settings.System.getIntForUser(context.getContentResolver(), "accelerometer_rotation", 1, -2) != 0;
            IWindowManager iWindowManagerAsInterface = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
            if (iWindowManagerAsInterface != null && z) {
                iWindowManagerAsInterface.freezeRotation(0);
                Settings.System.putIntForUser(context.getContentResolver(), "accelerometer_rotation", 0, -2);
                Settings.System.putIntForUser(context.getContentResolver(), "accelerometer_rotation_restore", 1, -2);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NullPointerException e2) {
            Log.e(TAG, "check Rotation: context object is null when get Rotation");
        }
        Slog.v(TAG, "start ipod");
        SystemProperties.set("ctl.start", "ipod");
        for (int i = 0; i < 5 && !SystemProperties.get("init.svc.ipod", (String) null).equals("running"); i++) {
            Slog.v(TAG, "waiting ipod (" + i + ")");
            try {
                Thread.sleep(20L);
            } catch (InterruptedException e3) {
                Slog.e(TAG, "interrupted while waiting ipod: " + e3);
            }
        }
        long jElapsedRealtime2 = (jElapsedRealtime + 1500) - SystemClock.elapsedRealtime();
        if (!(jElapsedRealtime2 <= 0)) {
            try {
                Slog.v(TAG, "sleep " + jElapsedRealtime2 + "ms for ipowin");
                Thread.sleep(jElapsedRealtime2);
            } catch (InterruptedException e4) {
                Log.e(TAG, "Thread sleep exception: ", e4);
            }
        }
    }

    public void enterShutdown(Context context) {
        if (!isBSPPackage && GetStreamMute(3) == 0) {
            setMusicMuted = true;
            SetStreamMute(3, true);
        }
        IActivityManager iActivityManagerAsInterface = ActivityManagerNative.asInterface(ServiceManager.checkService("activity"));
        Slog.i(TAG, "Force-stop GMap");
        try {
            iActivityManagerAsInterface.forceKillPackage("com.google.android.apps.maps", -1, "IPO_FORCEKILL");
        } catch (RemoteException e) {
            Slog.i(TAG, "RemoteExcepiton while forcekill google maps: " + e);
        }
    }

    public void finishShutdown(final Context context) {
        if (UserManager.supportsMultipleUsers()) {
            int currentUser = ActivityManager.getCurrentUser();
            Slog.i(TAG, "current userId: " + currentUser);
            if (currentUser != 0) {
                try {
                    ActivityManagerNative.getDefault().switchUser(0);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Couldn't switch user.", e);
                }
            }
        }
        sHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                ShutdownManager.this.forceStopKillPackages(context);
            }
        }, 0L);
        if (!isBSPPackage && setMusicMuted) {
            setMusicMuted = false;
            SetStreamMute(3, false);
        }
    }

    public void saveStates(Context context) {
        IWifiManager iWifiManagerAsInterface = IWifiManager.Stub.asInterface(ServiceManager.getService("wifi"));
        try {
            if (iWifiManagerAsInterface != null) {
                iWifiManagerAsInterface.setWifiEnabledForIPO(false);
            } else {
                Slog.i(TAG, " can not get the IWifiManager binder");
            }
        } catch (RemoteException e) {
            Slog.i(TAG, "Wi-Fi operation failed: " + e);
        }
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
        if (connectivityManager != null) {
            connectivityManager.stopTethering(0);
            Slog.i(TAG, " Turn off WIFI AP");
        } else {
            Slog.i(TAG, " can not get ConnectivityManager");
        }
    }

    public void preRestoreStates(Context context) {
        unmuteSystem(context);
    }

    public void restoreStates(Context context) {
        IWifiManager iWifiManagerAsInterface = IWifiManager.Stub.asInterface(ServiceManager.getService("wifi"));
        try {
            if (iWifiManagerAsInterface != null) {
                iWifiManagerAsInterface.setWifiEnabledForIPO(true);
            } else {
                Slog.i(TAG, " can not get the IWifiManager binder");
            }
        } catch (RemoteException e) {
            Slog.i(TAG, "Wi-Fi operation failed: " + e);
        }
    }

    private static void copyFileTo(String str, String str2) {
        if (str == null || str2 == null) {
            return;
        }
        StrictMode.ThreadPolicy threadPolicyAllowThreadDiskReads = StrictMode.allowThreadDiskReads();
        StrictMode.allowThreadDiskWrites();
        try {
            File file = new File(str);
            File file2 = new File(str2);
            if (!file.exists()) {
                Slog.d(TAG, str + " not exist...");
                return;
            }
            FileInputStream fileInputStream = new FileInputStream(file);
            FileOutputStream fileOutputStream = new FileOutputStream(file2);
            byte[] bArr = new byte[5120];
            while (true) {
                int i = fileInputStream.read(bArr);
                if (i <= 0) {
                    fileInputStream.close();
                    fileOutputStream.close();
                    return;
                }
                fileOutputStream.write(bArr, 0, i);
            }
        } catch (FileNotFoundException e) {
            Slog.e(TAG, "file not found: " + e);
        } catch (IOException e2) {
            Slog.e(TAG, "IO exception: " + e2);
        } finally {
            StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskReads);
        }
    }

    private static void writeStringToFile(String str, String str2) throws Throwable {
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
                                String str3 = TAG;
                                Slog.e(TAG, "IO exception: " + e);
                                obj = str3;
                            }
                        }
                        StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskReads);
                        r1 = obj;
                    } catch (FileNotFoundException e2) {
                        e = e2;
                        Slog.e(TAG, e.toString());
                        Object obj2 = fileOutputStream;
                        if (fileOutputStream != null) {
                            try {
                                fileOutputStream.close();
                                obj2 = fileOutputStream;
                            } catch (IOException e3) {
                                String str4 = TAG;
                                Slog.e(TAG, "IO exception: " + e3);
                                obj2 = str4;
                            }
                        }
                        StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskReads);
                        r1 = obj2;
                    } catch (IOException e4) {
                        e = e4;
                        Slog.e(TAG, e.toString());
                        Object obj3 = fileOutputStream;
                        if (fileOutputStream != null) {
                            try {
                                fileOutputStream.close();
                                obj3 = fileOutputStream;
                            } catch (IOException e5) {
                                String str5 = TAG;
                                Slog.e(TAG, "IO exception: " + e5);
                                obj3 = str5;
                            }
                        }
                        StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskReads);
                        r1 = obj3;
                    }
                } catch (Throwable th) {
                    th = th;
                    if (r1 != 0) {
                        try {
                            r1.close();
                        } catch (IOException e6) {
                            Slog.e(TAG, "IO exception: " + e6);
                        }
                    }
                    StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskReads);
                    throw th;
                }
            } catch (FileNotFoundException e7) {
                e = e7;
                fileOutputStream = null;
            } catch (IOException e8) {
                e = e8;
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

    public static void stopFtraceCapture() throws Throwable {
        if (SystemProperties.get("sys.shutdown.ftrace").equals("1")) {
            Slog.d(TAG, "stop ftrace");
            writeStringToFile("/sys/kernel/debug/tracing/tracing_on", "0");
            Slog.d(TAG, "saving ftrace to /data/misc/shutdown_ftrace.txt");
            copyFileTo("/sys/kernel/debug/tracing/trace", "/data/misc/shutdown_ftrace.txt");
            Slog.d(TAG, "ftrace saving done, restart ftrace");
            writeStringToFile("/sys/kernel/debug/tracing/tracing_on", "1");
        }
    }

    public static void startFtraceCapture() throws Throwable {
        if (SystemProperties.get("sys.shutdown.ftrace").equals("1")) {
            Slog.d(TAG, "shutdown ftrace enabled!");
            String str = SystemProperties.get("sys.shutdown.ftrace.size");
            if (str.matches("^\\d+$")) {
                Slog.d(TAG, "buffer_size_kb = " + str);
            } else {
                Slog.d(TAG, "buffer_size_kb = " + str + ", restore to 11MB");
                str = "11256";
            }
            writeStringToFile("/sys/kernel/debug/tracing/buffer_size_kb", str);
            writeStringToFile("/sys/kernel/debug/tracing/trace", "");
        }
    }
}
