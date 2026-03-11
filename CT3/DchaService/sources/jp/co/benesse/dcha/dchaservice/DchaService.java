package jp.co.benesse.dcha.dchaservice;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.ActivityThread;
import android.app.AlarmManager;
import android.app.AppOpsManager;
import android.app.INotificationManager;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.IPackageInstallObserver;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.content.res.Configuration;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.NetworkPolicyManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.Process;
import android.os.RecoverySystem;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.IMountService;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.IWindowManager;
import android.view.textservice.TextServicesManager;
import com.android.internal.app.LocalePicker;
import com.android.internal.widget.LockPatternUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.channels.FileChannel;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import jp.co.benesse.dcha.dchaservice.IDchaService;
import jp.co.benesse.dcha.dchaservice.util.Log;

public class DchaService extends Service {
    private static boolean doCancelDigichalizedFlg = false;
    private static Signature[] sSystemSignature;
    protected ListPreference mAnimatorDurationScale;
    protected IDchaService.Stub mDchaServiceStub = new IDchaService.Stub() {
        @Override
        public boolean verifyUpdateImage(String updateFile) {
            Log.d("DchaService", "verifyUpdateImage 0001");
            boolean ret = false;
            try {
                File update = new File(updateFile);
                RecoverySystem.verifyPackage(update, null, null);
                ret = true;
            } catch (IOException e) {
                Log.e("DchaService", "verifyUpdateImege IO Exception", e);
            } catch (GeneralSecurityException e2) {
                Log.e("DchaService", "verifyUpdateImege GeneralSecurityException", e2);
            } catch (Exception e3) {
                Log.e("DchaService", "verifyUpdateImege Exception", e3);
            }
            Log.d("DchaService", "verifyUpdateImage 0002");
            return ret;
        }

        @Override
        public boolean copyUpdateImage(String srcFile, String dstFile) {
            Log.d("DchaService", "copyUpdateImage 0001");
            if (TextUtils.isEmpty(srcFile) || TextUtils.isEmpty(dstFile)) {
                Log.d("DchaService", "copyUpdateImage 0009");
                return false;
            }
            if (!dstFile.startsWith("/cache")) {
                Log.d("DchaService", "copyUpdateImage 0010");
                return false;
            }
            FileChannel srcChannel = null;
            FileChannel dstChannel = null;
            try {
                try {
                    try {
                        File src = new File(srcFile);
                        File dst = new File(dstFile);
                        srcChannel = new FileInputStream(src).getChannel();
                        dstChannel = new FileOutputStream(dst).getChannel();
                        srcChannel.transferTo(0L, srcChannel.size(), dstChannel);
                        Log.d("DchaService", "copyUpdateImage 0005");
                        if (srcChannel != null) {
                            Log.d("DchaService", "copyUpdateImage 0006");
                            try {
                                srcChannel.close();
                            } catch (IOException e) {
                            }
                        }
                        if (dstChannel != null) {
                            Log.d("DchaService", "copyUpdateImage 0007");
                            try {
                                dstChannel.close();
                            } catch (IOException e2) {
                            }
                        }
                        Log.d("DchaService", "copyUpdateImage 0008");
                        return true;
                    } catch (Throwable th) {
                        Log.d("DchaService", "copyUpdateImage 0005");
                        if (srcChannel != null) {
                            Log.d("DchaService", "copyUpdateImage 0006");
                            try {
                                srcChannel.close();
                            } catch (IOException e3) {
                            }
                        }
                        if (dstChannel != null) {
                            Log.d("DchaService", "copyUpdateImage 0007");
                            try {
                                dstChannel.close();
                            } catch (IOException e4) {
                            }
                        }
                        Log.d("DchaService", "copyUpdateImage 0008");
                        return false;
                    }
                } catch (FileNotFoundException e5) {
                    Log.e("DchaService", "copyUpdateImage 0002", e5);
                    Log.d("DchaService", "copyUpdateImage 0005");
                    if (srcChannel != null) {
                        Log.d("DchaService", "copyUpdateImage 0006");
                        try {
                            srcChannel.close();
                        } catch (IOException e6) {
                        }
                    }
                    if (dstChannel != null) {
                        Log.d("DchaService", "copyUpdateImage 0007");
                        try {
                            dstChannel.close();
                        } catch (IOException e7) {
                        }
                    }
                    Log.d("DchaService", "copyUpdateImage 0008");
                    return false;
                }
            } catch (IOException e8) {
                Log.e("DchaService", "copyUpdateImage 0003", e8);
                Log.d("DchaService", "copyUpdateImage 0005");
                if (srcChannel != null) {
                    Log.d("DchaService", "copyUpdateImage 0006");
                    try {
                        srcChannel.close();
                    } catch (IOException e9) {
                    }
                }
                if (dstChannel != null) {
                    Log.d("DchaService", "copyUpdateImage 0007");
                    try {
                        dstChannel.close();
                    } catch (IOException e10) {
                    }
                }
                Log.d("DchaService", "copyUpdateImage 0008");
                return false;
            } catch (Exception e11) {
                Log.e("DchaService", "copyUpdateImage 0004", e11);
                Log.d("DchaService", "copyUpdateImage 0005");
                if (srcChannel != null) {
                    Log.d("DchaService", "copyUpdateImage 0006");
                    try {
                        srcChannel.close();
                    } catch (IOException e12) {
                    }
                }
                if (dstChannel != null) {
                    Log.d("DchaService", "copyUpdateImage 0007");
                    try {
                        dstChannel.close();
                    } catch (IOException e13) {
                    }
                }
                Log.d("DchaService", "copyUpdateImage 0008");
                return false;
            }
        }

        @Override
        public void rebootPad(int rebootMode, String srcFile) throws RemoteException {
            Log.d("DchaService", "rebootPad 0001");
            try {
                PowerManager pm = (PowerManager) DchaService.this.getSystemService("power");
                switch (rebootMode) {
                    case 0:
                        Log.d("DchaService", "rebootPad 0002");
                        pm.reboot(null);
                        break;
                    case 1:
                        Log.d("DchaService", "rebootPad 0003");
                        RecoverySystem.rebootWipeUserData(DchaService.this.getBaseContext());
                        break;
                    case 2:
                        Log.d("DchaService", "rebootPad 0004");
                        if (srcFile != null) {
                            RecoverySystem.installPackage(DchaService.this.getBaseContext(), new File(srcFile));
                        }
                        break;
                    default:
                        Log.d("DchaService", "rebootPad 0005");
                        break;
                }
                Log.d("DchaService", "rebootPad 0007");
            } catch (Exception e) {
                Log.e("DchaService", "rebootPad 0006", e);
                throw new RemoteException();
            }
        }

        @Override
        public void setDefaultPreferredHomeApp(String packageName) throws RemoteException {
            try {
                Log.d("DchaService", "setDefalutPreferredHomeApp 0001");
                Log.d("DchaService", "setDefalutPreferredHomeApp packageName:" + packageName);
                PackageManager pm = DchaService.this.getPackageManager();
                ComponentName defaultHomeComponentName = null;
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.intent.action.MAIN");
                filter.addCategory("android.intent.category.HOME");
                filter.addCategory("android.intent.category.DEFAULT");
                Intent intent = new Intent("android.intent.action.MAIN");
                intent.addCategory("android.intent.category.HOME");
                intent.addCategory("android.intent.category.DEFAULT");
                List<ResolveInfo> homeApps = pm.queryIntentActivities(intent, 0);
                List<ComponentName> compNames = new ArrayList<>();
                for (ResolveInfo homeApp : homeApps) {
                    String packName = homeApp.activityInfo.applicationInfo.packageName;
                    String activityName = homeApp.activityInfo.name;
                    Log.d("DchaService", "setDefalutPreferredHomeApp packName:" + packName);
                    Log.d("DchaService", "setDefalutPreferredHomeApp activityName:" + activityName);
                    ComponentName compname = new ComponentName(packName, activityName);
                    compNames.add(compname);
                    if (packName.equalsIgnoreCase(packageName)) {
                        Log.d("DchaService", "setDefalutPreferredHomeApp 0002");
                        defaultHomeComponentName = compname;
                        Log.d("DchaService", "setDefalutPreferredHomeApp defaultHomeComponentName:" + compname);
                    }
                }
                ComponentName[] homeAppSet = (ComponentName[]) compNames.toArray(new ComponentName[compNames.size()]);
                if (defaultHomeComponentName != null) {
                    Log.d("DchaService", "setDefalutPreferredHomeApp 0003");
                    pm.addPreferredActivityAsUser(filter, 1081344, homeAppSet, defaultHomeComponentName, 0);
                }
                Log.d("DchaService", "setDefalutPreferredHomeApp 0005");
            } catch (Exception e) {
                Log.e("DchaService", "setDefalutPrefferredHomeApp 0004", e);
                throw new RemoteException(e.toString());
            }
        }

        @Override
        public void clearDefaultPreferredApp(String packageName) throws RemoteException {
            Log.d("DchaService", "clearDefaultPreferredApp 0001");
            try {
                PackageManager pm = DchaService.this.getPackageManager();
                pm.clearPackagePreferredActivities(packageName);
            } catch (Exception e) {
                Log.e("DchaService", "clearDefaultPreferredApp 0002", e);
                throw new RemoteException(e.toString());
            }
        }

        @Override
        public void disableADB() {
            Log.d("DchaService", "disableADB 0001");
            Settings.Secure.putInt(DchaService.this.getContentResolver(), "adb_enabled", 0);
        }

        @Override
        public boolean checkPadRooted() throws RemoteException {
            Log.d("DchaService", "checkPadRooted 0001");
            return false;
        }

        class PackageInstallObserver extends IPackageInstallObserver.Stub {
            boolean finished;
            int result;

            PackageInstallObserver() {
            }

            public void packageInstalled(String name, int status) {
                Log.d("DchaService", "packageInstalled 0001");
                synchronized (this) {
                    this.finished = true;
                    this.result = status;
                    notifyAll();
                }
            }
        }

        private String installFailureToString(int result) {
            Log.d("DchaService", "installFailureToString 0001");
            Field[] fields = PackageManager.class.getFields();
            for (Field f : fields) {
                if (f.getType() == Integer.TYPE) {
                    Log.d("DchaService", "installFailureToString 0002");
                    int modifiers = f.getModifiers();
                    if ((modifiers & 16) != 0 && (modifiers & 1) != 0 && (modifiers & 8) != 0) {
                        Log.d("DchaService", "installFailureToString 0003");
                        String fieldName = f.getName();
                        if (fieldName.startsWith("INSTALL_FAILED_") || fieldName.startsWith("INSTALL_PARSE_FAILED_")) {
                            Log.d("DchaService", "installFailureToString 0004");
                            try {
                                if (result == f.getInt(null)) {
                                    Log.d("DchaService", "installFailureToString 0005");
                                    return fieldName;
                                }
                                continue;
                            } catch (IllegalAccessException e) {
                                Log.e("DchaService", "installFailureToString 0006", e);
                            }
                        }
                    }
                }
            }
            Log.d("DchaService", "installFailureToString 0007");
            return Integer.toString(result);
        }

        @Override
        public boolean installApp(String path, int installFlag) throws RemoteException {
            Log.d("DchaService", "installApp 0001");
            try {
                PackageManager pm = DchaService.this.getPackageManager();
                int installFlags = 64;
                switch (installFlag) {
                    case 0:
                        Log.d("DchaService", "installApp 0003");
                        break;
                    case 1:
                        Log.d("DchaService", "installApp 0004");
                        installFlags = 66;
                        break;
                    case 2:
                        Log.d("DchaService", "installApp 0005");
                        installFlags = 66 | 128;
                        break;
                    default:
                        Log.d("DchaService", "installApp 0006");
                        break;
                }
                PackageInstallObserver obs = new PackageInstallObserver();
                Uri apkURI = Uri.fromFile(new File(path));
                pm.installPackage(apkURI, obs, installFlags, null);
                synchronized (obs) {
                    while (!obs.finished) {
                        try {
                            obs.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                    if (obs.result != 1) {
                        Log.d("DchaService", "installApp 0007");
                        Log.e("DchaService", "apk install failure:" + installFailureToString(obs.result));
                        Log.d("DchaService", "installApp return: false");
                        return false;
                    }
                    Log.d("DchaService", "installApp 0008");
                    return true;
                }
            } catch (Exception e2) {
                Log.e("DchaService", "installApp 0009", e2);
                throw new RemoteException();
            }
        }

        class PackageDeleteObserver extends IPackageDeleteObserver.Stub {
            boolean finished;
            boolean result;

            PackageDeleteObserver() {
            }

            public void packageDeleted(String packageName, int returnCode) {
                Log.d("DchaService", "packageDeleted 0001");
                synchronized (this) {
                    this.finished = true;
                    this.result = returnCode == 1;
                    notifyAll();
                }
            }
        }

        @Override
        public boolean uninstallApp(String packageName, int uninstallFlag) throws RuntimeException {
            Log.d("DchaService", "uninstallApp 0001");
            try {
                String callPkg = DchaService.this.getPackageNameFromPid(getCallingPid());
                EmergencyLog.write(DchaService.this, "ELK002", packageName + " " + callPkg);
                PackageManager pm = DchaService.this.getPackageManager();
                int unInstallFlags = 2;
                switch (uninstallFlag) {
                    case 1:
                        Log.d("DchaService", "uninstallApp 0002");
                        unInstallFlags = 3;
                        break;
                }
                PackageDeleteObserver obs = new PackageDeleteObserver();
                pm.deletePackage(packageName, obs, unInstallFlags);
                synchronized (obs) {
                    while (!obs.finished) {
                        try {
                            obs.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                }
                Log.d("DchaService", "uninstallApp 0003");
                return obs.result;
            } catch (Exception e2) {
                Log.e("DchaService", "uninstallApp 0004", e2);
                throw new RuntimeException();
            }
        }

        @Override
        public void cancelSetup() throws RemoteException {
            Log.d("DchaService", "cancelSetup 0001");
            DchaService.this.doCancelDigichalized();
        }

        @Override
        public void setSetupStatus(int status) {
            Log.d("DchaService", "setSetupStatus 0001");
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(DchaService.this);
            sp.edit().putInt("DigichalizedStatus", status).commit();
            Settings.System.putInt(DchaService.this.getContentResolver(), "dcha_state", status);
        }

        @Override
        public int getSetupStatus() {
            Log.d("DchaService", "getSetupStatus 0001");
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(DchaService.this);
            int status = sp.getInt("DigichalizedStatus", -1);
            return status;
        }

        @Override
        public void setSystemTime(String time, String timeFormat) {
            Log.d("DchaService", "setSystemTime 0001");
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(timeFormat, Locale.JAPAN);
                Date serverTime = sdf.parse(time);
                String callPkg = DchaService.this.getPackageNameFromPid(Binder.getCallingPid());
                Calendar cal1 = Calendar.getInstance(Locale.JAPAN);
                cal1.set(2016, 1, 1, 0, 0);
                Calendar cal2 = Calendar.getInstance(Locale.JAPAN);
                cal2.setTime(serverTime);
                if (cal1.compareTo(cal2) > 0) {
                    Log.d("DchaService", "setSystemTime 0002");
                    EmergencyLog.write(DchaService.this, "ELK008", time + " " + callPkg);
                } else {
                    Log.d("DchaService", "setSystemTime 0003");
                    SystemClock.setCurrentTimeMillis(serverTime.getTime());
                }
                Log.d("DchaService", "setSystemTime set time :" + serverTime);
            } catch (Exception e) {
                Log.e("DchaService", "setSystemTime 0004", e);
            }
            Log.d("DchaService", "setSystemTime 0005");
        }

        @Override
        public void removeTask(String packageName) throws RemoteException {
            Log.d("DchaService", "removeTask 0001");
            try {
                ActivityManager activityManager = (ActivityManager) DchaService.this.getSystemService("activity");
                List<ActivityManager.RecentTaskInfo> recentTaskList = activityManager.getRecentTasksForUser(30, 1, UserHandle.myUserId());
                if (packageName != null) {
                    Log.d("DchaService", "removeTask 0002");
                    for (ActivityManager.RecentTaskInfo recentTask : recentTaskList) {
                        Log.d("DchaService", "removeTask " + recentTask.baseIntent.getComponent().getPackageName());
                        if (packageName.equals(recentTask.baseIntent.getComponent().getPackageName())) {
                            Log.d("DchaService", "removeTask 0003");
                            activityManager.removeTask(recentTask.persistentId);
                        }
                    }
                } else {
                    Log.d("DchaService", "removeTask 0004");
                    Iterator recentTask$iterator = recentTaskList.iterator();
                    while (recentTask$iterator.hasNext()) {
                        activityManager.removeTask(((ActivityManager.RecentTaskInfo) recentTask$iterator.next()).persistentId);
                    }
                }
                Log.d("DchaService", "removeTask 0006");
            } catch (Exception e) {
                Log.e("DchaService", "removeTask 0005", e);
                throw new RemoteException();
            }
        }

        @Override
        public void sdUnmount() throws RemoteException {
            Log.d("DchaService", "sdUnmount 0001");
            String extStoragePath = System.getenv("SECONDARY_STORAGE");
            IMountService mountService = IMountService.Stub.asInterface(ServiceManager.getService("mount"));
            try {
                mountService.unmountVolume(getCanonicalExternalPath(extStoragePath), true, false);
                Log.d("DchaService", "sdUnmount 0003");
            } catch (RemoteException e) {
                Log.e("DchaService", "sdUnmount 0002", e);
                throw new RemoteException();
            }
        }

        @Override
        public void setDefaultParam() throws RemoteException {
            Log.d("DchaService", "setDefaultParam 0001");
            try {
                DchaService.this.setInitialSettingsWirelessNetwork();
                DchaService.this.setInitialSettingsTerminal();
                DchaService.this.setInitialSettingsUser();
                DchaService.this.setInitialSettingsAccount();
                DchaService.this.setInitialSettingsSystem();
                DchaService.this.setInitialSettingsDevelopmentOptions();
                Log.d("DchaService", "setDefaultParam 0003");
            } catch (RemoteException e) {
                Log.e("DchaService", "setDefaultParam 0002", e);
                throw new RemoteException();
            }
        }

        @Override
        public String getForegroundPackageName() throws RemoteException {
            Log.d("DchaService", "getForegroundPackageName 0001");
            try {
                List<ActivityManager.RunningTaskInfo> appInfo = ActivityManagerNative.getDefault().getTasks(1, 0);
                String packageName = appInfo.get(0).baseActivity.getPackageName();
                Log.d("DchaService", "Foreground package name :" + packageName);
                appInfo.clear();
                Log.d("DchaService", "getForegroundPackageName 0003");
                return packageName;
            } catch (Exception e) {
                Log.e("DchaService", "getForegroundPackageName 0002", e);
                throw new RemoteException();
            }
        }

        @Override
        public boolean copyFile(String srcFilePath, String dstFilePath) throws RemoteException {
            Log.d("DchaService", "copyFile 0001");
            return false;
        }

        @Override
        public boolean deleteFile(String path) throws RemoteException {
            Log.d("DchaService", "deleteFile 0001");
            return false;
        }

        @Override
        public int getUserCount() {
            Log.d("DchaService", "getUserCount 0001");
            UserManager um = (UserManager) DchaService.this.getSystemService("user");
            int count = um.getUserCount();
            Log.d("DchaService", "getUserCount return: " + count);
            return count;
        }

        @Override
        public boolean isDeviceEncryptionEnabled() {
            Log.d("DchaService", "isDeviceEncryptionEnabled 0001");
            String status = SystemProperties.get("ro.crypto.state", "unsupported");
            boolean enabled = "encrypted".equalsIgnoreCase(status);
            Log.d("DchaService", "isDeviceEncryptionEnabled return: " + enabled);
            return enabled;
        }

        @Override
        public void hideNavigationBar(boolean hide) {
            Log.d("DchaService", "hideNavigationBar 0001");
            DchaService.this.hideNavigationBar(hide);
        }

        @Override
        public void setPermissionEnforced(boolean enforced) throws RemoteException {
            Log.d("DchaService", "setPermissionEnforced 0001");
            ActivityThread.getPackageManager().setPermissionEnforced("android.permission.READ_EXTERNAL_STORAGE", enforced);
            Log.d("DchaService", "setPermissionEnforced 0002");
        }

        @Override
        public String getCanonicalExternalPath(String linkPath) throws RemoteException {
            String canonicalPath;
            Log.d("DchaService", "getCanonicalExternalPath 0001");
            if (TextUtils.isEmpty(linkPath)) {
                Log.d("DchaService", "getCanonicalExternalPath 0002");
                return linkPath;
            }
            try {
                canonicalPath = new File(linkPath).getCanonicalPath();
            } catch (Exception e) {
                Log.e("DchaService", "getCanonicalExternalPath 0003", e);
                canonicalPath = linkPath;
            }
            Log.d("DchaService", "getCanonicalExternalPath 0004 return: " + canonicalPath);
            return canonicalPath;
        }
    };
    protected String mDebugApp;
    protected boolean mDontPokeProperties;
    protected boolean mLastEnabledState;
    protected ListPreference mTransitionAnimationScale;
    protected ListPreference mWindowAnimationScale;
    protected IWindowManager mWindowManager;

    protected void writeDebuggerOptions() {
        Log.d("DchaService", "writeDebuggerOptions 0001");
        try {
            this.mDebugApp = Settings.Global.getString(getContentResolver(), "debug_app");
            ActivityManagerNative.getDefault().setDebugApp(this.mDebugApp, false, true);
        } catch (RemoteException e) {
        }
    }

    protected static void resetDebuggerOptions() {
        Log.d("DchaService", "resetDebuggerOptions 0001");
        try {
            ActivityManagerNative.getDefault().setDebugApp((String) null, false, true);
        } catch (RemoteException e) {
        }
    }

    protected void writeStrictModeVisualOptions() {
        Log.d("DchaService", "writeStrictModeVisualOptions 0001");
        try {
            this.mWindowManager.setStrictModeVisualIndicatorPreference("");
        } catch (RemoteException e) {
        }
    }

    protected void writeShowUpdatesOption() {
        Log.d("DchaService", "writeShowUpdatesOption 0001");
        try {
            IBinder flinger = ServiceManager.getService("SurfaceFlinger");
            if (flinger == null) {
                return;
            }
            Log.d("DchaService", "writeShowUpdatesOption 0002");
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            data.writeInterfaceToken("android.ui.ISurfaceComposer");
            flinger.transact(1010, data, reply, 0);
            reply.readInt();
            reply.readInt();
            int showUpdates = reply.readInt();
            reply.readInt();
            reply.readInt();
            reply.recycle();
            data.recycle();
            if (showUpdates == 0) {
                return;
            }
            Log.d("DchaService", "writeShowUpdatesOption 0003");
            Parcel data2 = Parcel.obtain();
            data2.writeInterfaceToken("android.ui.ISurfaceComposer");
            data2.writeInt(0);
            flinger.transact(1002, data2, null, 0);
            data2.recycle();
        } catch (RemoteException e) {
        }
    }

    protected void writeDisableOverlaysOption() {
        Log.d("DchaService", "writeDisableOverlaysOption 0001");
        try {
            IBinder flinger = ServiceManager.getService("SurfaceFlinger");
            if (flinger == null) {
                return;
            }
            Log.d("DchaService", "writeDisableOverlaysOption 0002");
            Parcel data = Parcel.obtain();
            data.writeInterfaceToken("android.ui.ISurfaceComposer");
            data.writeInt(0);
            flinger.transact(1008, data, null, 0);
            data.recycle();
        } catch (RemoteException e) {
        }
    }

    protected void writeHardwareUiOptions() {
        Log.d("DchaService", "writeHardwareUiOptions 0001");
        SystemProperties.set("persist.sys.ui.hw", "false");
        pokeSystemProperties();
    }

    protected void writeMsaaOptions() {
        Log.d("DchaService", "writeMsaaOptions 0001");
        SystemProperties.set("debug.egl.force_msaa", "false");
        pokeSystemProperties();
    }

    protected void writeTrackFrameTimeOptions() {
        Log.d("DchaService", "writeTrackFrameTimeOptions 0001");
        SystemProperties.set("debug.hwui.profile", "false");
        pokeSystemProperties();
    }

    protected void writeShowHwScreenUpdatesOptions() {
        Log.d("DchaService", "writeShowHwScreenUpdatesOptions 0001");
        SystemProperties.set("debug.hwui.show_dirty_regions", (String) null);
        pokeSystemProperties();
    }

    protected void writeShowHwLayersUpdatesOptions() {
        Log.d("DchaService", "writeShowHwLayersUpdatesOptions 0001");
        SystemProperties.set("debug.hwui.show_layers_updates", (String) null);
        pokeSystemProperties();
    }

    protected void writeShowHwOverdrawOptions() {
        Log.d("DchaService", "writeShowHwOverdrawOptions 0001");
        SystemProperties.set("debug.hwui.overdraw", "0");
        pokeSystemProperties();
    }

    protected void writeDebugLayoutOptions() {
        Log.d("DchaService", "writeDebugLayoutOptions 0001");
        SystemProperties.set("debug.layout", "false");
        pokeSystemProperties();
    }

    protected void writeCpuUsageOptions() {
        Log.d("DchaService", "writeCpuUsageOptions 0001");
        Settings.Global.putInt(getContentResolver(), "show_processes", 0);
        Intent service = new Intent().setClassName("com.android.systemui", "com.android.systemui.LoadAverageService");
        if (0 != 0) {
            Log.d("DchaService", "writeCpuUsageOptions 0002");
            startService(service);
        } else {
            Log.d("DchaService", "writeCpuUsageOptions 0003");
            stopService(service);
        }
    }

    protected void writeImmediatelyDestroyActivitiesOptions() {
        Log.d("DchaService", "writeImmediatelyDestroyActivitiesOptions 0001");
        try {
            ActivityManagerNative.getDefault().setAlwaysFinish(false);
        } catch (RemoteException e) {
        }
    }

    protected void writeAnimationScaleOption(int which, ListPreference pref, Object newValue) {
        float scale;
        Log.d("DchaService", "writeAnimationScaleOption 0001");
        if (newValue != null) {
            try {
                scale = Float.parseFloat(newValue.toString());
            } catch (RemoteException e) {
                return;
            }
        } else {
            scale = 1.0f;
        }
        this.mWindowManager.setAnimationScale(which, scale);
    }

    protected void writeOverlayDisplayDevicesOptions(Object newValue) {
        Log.d("DchaService", "writeOverlayDisplayDevicesOptions 0001");
        Settings.Global.putString(getContentResolver(), "overlay_display_devices", (String) newValue);
    }

    protected void writeAppProcessLimitOptions(Object newValue) {
        int limit;
        Log.d("DchaService", "writeAppProcessLimitOptions 0001");
        if (newValue != null) {
            try {
                limit = Integer.parseInt(newValue.toString());
            } catch (RemoteException e) {
                return;
            }
        } else {
            limit = -1;
        }
        ActivityManagerNative.getDefault().setProcessLimit(limit);
    }

    void pokeSystemProperties() {
        Log.d("DchaService", "pokeSystemProperties 0001");
        if (this.mDontPokeProperties) {
            return;
        }
        Log.d("DchaService", "pokeSystemProperties 0002");
        new SystemPropPoker().execute(new Void[0]);
    }

    static class SystemPropPoker extends AsyncTask<Void, Void, Void> {
        private final String TAG = "SystemPropPoker";

        SystemPropPoker() {
        }

        @Override
        public Void doInBackground(Void... params) {
            Log.d("SystemPropPoker", "doInBackground 0001");
            String[] services = ServiceManager.listServices();
            for (String service : services) {
                IBinder obj = ServiceManager.checkService(service);
                if (obj != null) {
                    Log.d("SystemPropPoker", "doInBackground 0003");
                    Parcel data = Parcel.obtain();
                    try {
                        obj.transact(1599295570, data, null, 0);
                    } catch (RemoteException e) {
                        Log.e("SystemPropPoker", "doInBackground 0004", e);
                    } catch (Exception e2) {
                        Log.v("DevSettings", "Somone wrote a bad service '" + service + "' that doesn't like to be poked: " + e2);
                        Log.e("SystemPropPoker", "doInBackground 0005", e2);
                    }
                    data.recycle();
                }
            }
            Log.d("SystemPropPoker", "doInBackground 0006");
            return null;
        }
    }

    public void doCancelDigichalized() throws RemoteException {
        try {
            Log.d("DchaService", "doCancelDigichalized 0001");
            int status = this.mDchaServiceStub.getSetupStatus();
            Log.d("DchaService", "status:" + Integer.toString(status));
            String callPkg = getPackageNameFromPid(Binder.getCallingPid());
            EmergencyLog.write(this, "ELK000", status + " " + isFinishDigichalize() + " " + callPkg);
            if (status != 3 && !isFinishDigichalize()) {
                Log.d("DchaService", "doCancelDigichalized 0002");
                EmergencyLog.write(this, "ELK004", status + " " + isFinishDigichalize() + " " + callPkg);
                Intent wipeDataBoxIntent = new Intent();
                wipeDataBoxIntent.setAction("jp.co.benesse.dcha.databox.intent.action.COMMAND");
                wipeDataBoxIntent.addCategory("jp.co.benesse.dcha.databox.intent.category.WIPE");
                wipeDataBoxIntent.putExtra("send_service", "DchaService");
                sendBroadcastAsUser(wipeDataBoxIntent, UserHandle.ALL);
                Log.d("DchaService", "doCancelDigichalized send wipeDataBoxIntent intent");
                HandlerThread handlerThread = new HandlerThread("handlerThread");
                handlerThread.start();
                Handler handler = new Handler(handlerThread.getLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!DchaService.doCancelDigichalizedFlg) {
                            Log.d("DchaService", "doCancelDigichalized 0003");
                            try {
                                Log.d("DchaService", "start uninstallApp");
                                boolean unused = DchaService.doCancelDigichalizedFlg = true;
                                PackageManager pm = DchaService.this.getPackageManager();
                                List<ApplicationInfo> infos = pm.getInstalledApplications(128);
                                for (ApplicationInfo info : infos) {
                                    if ((info.flags & 1) == 1) {
                                        Log.d("DchaService", "doCancelDigichalized 0004");
                                    } else {
                                        String packageName = info.packageName;
                                        DchaService.this.mDchaServiceStub.uninstallApp(packageName, 0);
                                    }
                                }
                                DchaService.this.mDchaServiceStub.setSetupStatus(0);
                                Log.d("DchaService", "end uninstallApp");
                            } catch (RemoteException e) {
                                Log.e("DchaService", "doCancelDigichalized 0005", e);
                            } finally {
                                Log.d("DchaService", "doCancelDigichalized 0006");
                                boolean unused2 = DchaService.doCancelDigichalizedFlg = false;
                            }
                        }
                        Log.d("DchaService", "doCancelDigichalized 0007");
                    }
                });
            } else {
                Log.d("DchaService", "doCancelDigichalized 0008");
                EmergencyLog.write(this, "ELK005", status + " " + isFinishDigichalize() + " " + callPkg);
            }
            Log.d("DchaService", "doCancelDigichalized 0010");
        } catch (Exception e) {
            Log.e("DchaService", "doCancelDigichalized 0009", e);
            throw new RemoteException();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("DchaService", "onBind 0001");
        return this.mDchaServiceStub;
    }

    @Override
    public void onCreate() {
        Log.d("DchaService", "onCreate 0001");
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        Log.d("DchaService", "onDestroy 0001");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("DchaService", "onStartCommand 0001");
        if (intent != null) {
            Log.d("DchaService", "onStartCommand 0002");
            int command = intent.getIntExtra("REQ_COMMAND", 0);
            Log.d("DchaService", "onStartCommand intent command:" + command);
            try {
                switch (command) {
                    case 1:
                        Log.d("DchaService", "onStartCommand 0003");
                        hideNavigationBar(false);
                        doCancelDigichalized();
                        break;
                    case 2:
                        Log.d("DchaService", "onStartCommand 0004");
                        hideNavigationBar(false);
                        break;
                    case 3:
                        Log.d("DchaService", "onStartCommand 0005");
                        hideNavigationBar(true);
                        break;
                    default:
                        Log.d("DchaService", "onStartCommand 0006");
                        break;
                }
            } catch (Exception e) {
                Log.e("DchaService", "onStartCommand 0007", e);
            }
        }
        Log.d("DchaService", "onStartCommand 0008");
        return super.onStartCommand(intent, flags, startId);
    }

    protected boolean isFinishDigichalize() {
        Log.d("DchaService", "isFinishDigichalize 0001");
        return UpdateLog.exists();
    }

    protected String getPackageNameFromPid(int pid) {
        Log.d("DchaService", "getPackageNameFromPid 0001");
        String pkgName = "Unknown";
        ActivityManager manager = (ActivityManager) getSystemService("activity");
        List<ActivityManager.RunningAppProcessInfo> appList = manager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo info : appList) {
            if (pid == info.pid) {
                Log.d("DchaService", "getPackageNameFromPid 0002");
                pkgName = info.processName;
            }
        }
        Log.d("DchaService", "getPackageNameFromPid 0003");
        return pkgName;
    }

    public void hideNavigationBar(boolean hide) {
        Log.d("DchaService", "hideNavigationBar 0001");
        Settings.System.putInt(getContentResolver(), "hide_navigation_bar", hide ? 1 : 0);
    }

    protected void setInitialSettingsWirelessNetwork() throws RemoteException {
        Log.d("DchaService", "setInitialSettingsWirelessNetwork 0001");
        Context context = getApplicationContext();
        WifiManager wifiManager = (WifiManager) context.getSystemService("wifi");
        if (wifiManager != null) {
            Log.d("DchaService", "setInitialSettingsWirelessNetwork 0002");
            wifiManager.setWifiEnabled(true);
            wifiManager.enableVerboseLogging(0);
            wifiManager.enableAggressiveHandover(0);
            wifiManager.setAllowScansWithTraffic(0);
        }
        Settings.Secure.putInt(getContentResolver(), "wifi_networks_available_notification_on", 0);
        Settings.Global.putInt(getContentResolver(), "wifi_scan_always_enabled", 0);
        Settings.Global.putInt(getContentResolver(), "wifi_sleep_policy", 0);
        Settings.Global.putInt(getContentResolver(), "wifi_display_on", 0);
        Settings.Global.putInt(getContentResolver(), "wifi_display_certification_on", 0);
        Settings.Global.putInt(getContentResolver(), "bluetooth_on", 0);
        BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
        bta.disable();
        Settings.Global.putInt(getContentResolver(), "airplane_mode_on", 0);
        Intent intent = new Intent("android.intent.action.AIRPLANE_MODE");
        intent.putExtra("state", false);
        sendBroadcast(intent);
        Log.d("DchaService", "setInitialSettingsWirelessNetwork 0003");
    }

    protected void setInitialSettingsTerminal() throws RemoteException {
        Log.d("DchaService", "setInitialSettingsTerminal start");
        try {
            Settings.System.putInt(getContentResolver(), "screen_brightness_mode", 0);
            Settings.System.putLong(getContentResolver(), "screen_off_timeout", 900000L);
            Settings.System.putInt(getContentResolver(), "screen_dim_timeout", 300000);
            Settings.Secure.putInt(getContentResolver(), "screensaver_enabled", 0);
            Configuration mCurConfig = new Configuration();
            mCurConfig.fontScale = Float.parseFloat("1.0");
            ActivityManagerNative.getDefault().updatePersistentConfiguration(mCurConfig);
            Settings.System.putInt(getContentResolver(), "accelerometer_rotation", 0);
            INotificationManager inm = INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
            Context context = getApplicationContext();
            NotificationManager.Policy policy = NotificationManager.from(context).getNotificationPolicy();
            NotificationManager.Policy policy2 = savePolicy(policy, getNewPriorityCategories(policy, false, 1), policy.priorityCallSenders, policy.priorityMessageSenders, policy.suppressedVisualEffects);
            NotificationManager.Policy policy3 = savePolicy(policy2, getNewPriorityCategories(policy2, false, 2), policy2.priorityCallSenders, policy2.priorityMessageSenders, policy2.suppressedVisualEffects);
            NotificationManager.Policy policy4 = savePolicy(policy3, getNewPriorityCategories(policy3, false, 4), policy3.priorityCallSenders, policy3.priorityMessageSenders, policy3.suppressedVisualEffects);
            savePolicy(policy4, getNewPriorityCategories(policy4, false, 8), policy4.priorityCallSenders, policy4.priorityMessageSenders, policy4.suppressedVisualEffects);
            int status = this.mDchaServiceStub.getSetupStatus();
            if (status != 3) {
                AppOpsManager aom = (AppOpsManager) context.getSystemService("appops");
                IPackageManager mIPm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
                try {
                    mIPm.resetApplicationPreferences(UserHandle.myUserId());
                } catch (RemoteException e) {
                }
                aom.resetAllModes();
            }
            PackageManager pm = getPackageManager();
            List<ApplicationInfo> apps = pm.getInstalledApplications(512);
            for (int i = 0; i < apps.size(); i++) {
                ApplicationInfo app = apps.get(i);
                try {
                    PackageInfo pkgInfo = pm.getPackageInfo(app.packageName, 64);
                    if (!isSystemPackage(pm, pkgInfo)) {
                        Log.d("DchaService", "setInitialSettingsTerminal 0002:" + app.packageName);
                        inm.setNotificationsEnabledForPackage(app.packageName, app.uid, false);
                        inm.setImportance(app.packageName, pkgInfo.applicationInfo.uid, -1000);
                    }
                } catch (Exception e2) {
                }
                if (!app.enabled) {
                    Log.d("DchaService", "setInitialSettingsTerminal 0003");
                    if (pm.getApplicationEnabledSetting(app.packageName) == 3) {
                        Log.d("DchaService", "setInitialSettingsTerminal 0004:" + app.packageName);
                        pm.setApplicationEnabledSetting(app.packageName, 0, 1);
                    }
                }
                try {
                    PackageInfo pkgInfoPermissin = pm.getPackageInfo(app.packageName, 4096);
                    if (!isSystemPackage(pm, pkgInfoPermissin)) {
                        int permissionCount = pkgInfoPermissin.requestedPermissions.length;
                        for (int j = 0; j < permissionCount; j++) {
                            String requestedPermission = pkgInfoPermissin.requestedPermissions[j];
                            int flags = context.getPackageManager().getPermissionFlags(requestedPermission, pkgInfoPermissin.packageName, Process.myUserHandle());
                            if ((flags & 16) == 0 && "android.permission.READ_EXTERNAL_STORAGE".equals(requestedPermission)) {
                                pm.grantRuntimePermission(pkgInfoPermissin.packageName, requestedPermission, Process.myUserHandle());
                            }
                        }
                    }
                } catch (Exception e3) {
                }
            }
            AppOpsManager aom2 = (AppOpsManager) context.getSystemService("appops");
            List<ApplicationInfo> infos = pm.getInstalledApplications(128);
            for (ApplicationInfo info : infos) {
                pm.updateIntentVerificationStatusAsUser(info.packageName, 4, UserHandle.myUserId());
                aom2.setMode(24, info.uid, info.packageName, 0);
                aom2.setMode(23, info.uid, info.packageName, 0);
            }
            NetworkPolicyManager nm = NetworkPolicyManager.from(context);
            int[] restrictedUids = nm.getUidsWithPolicy(1);
            int currentUserId = ActivityManager.getCurrentUser();
            for (int uid : restrictedUids) {
                if (UserHandle.getUserId(uid) == currentUserId) {
                    nm.setUidPolicy(uid, 0);
                }
            }
            RingtoneManager.setActualDefaultRingtoneUri(context, 2, null);
            RingtoneManager.setActualDefaultRingtoneUri(context, 1, null);
            RingtoneManager.setActualDefaultRingtoneUri(context, 4, null);
            AudioManager audioManager = (AudioManager) getSystemService("audio");
            audioManager.loadSoundEffects();
            Settings.System.putInt(getContentResolver(), "sound_effects_enabled", 1);
            UsbManager usbManager = (UsbManager) getSystemService("usb");
            usbManager.setCurrentFunction("ptp");
            PowerManager powerManager = (PowerManager) getSystemService("power");
            powerManager.setPowerSaveMode(false);
            Settings.Global.putInt(getContentResolver(), "low_power_trigger_level", 0);
            Log.d("DchaService", "setInitialSettingsTerminal 0006");
        } catch (RemoteException e4) {
            Log.e("DchaService", "setInitialSettingsTerminal 0005", e4);
            throw e4;
        }
    }

    protected void setInitialSettingsUser() {
        Log.d("DchaService", "setInitialSettingsUser start");
        Settings.Secure.putInt(getContentResolver(), "location_mode", 0);
        LockPatternUtils lpu = new LockPatternUtils(this);
        int userHandle = UserHandle.getCallingUserId();
        lpu.clearLock(userHandle);
        lpu.setLockScreenDisabled(true, userHandle);
        Settings.System.putInt(getContentResolver(), "show_password", 1);
        Settings.Secure.putInt(getContentResolver(), "install_non_market_apps", 0);
        Log.d("DchaService", "setInitialSettingsUser 0002");
    }

    protected void setInitialSettingsAccount() {
        Log.d("DchaService", "setInitialSettingsAccount start");
        Locale japan = new Locale("ja", "JP");
        LocalePicker.updateLocale(japan);
        TextServicesManager tsm = (TextServicesManager) getSystemService("textservices");
        tsm.setSpellCheckerEnabled(true);
        Log.d("DchaService", "setInitialSettingsAccount 0002");
    }

    protected void setInitialSettingsSystem() {
        Log.d("DchaService", "setInitialSettingsSystem start");
        Settings.Global.putInt(getContentResolver(), "auto_time", 1);
        AlarmManager am = (AlarmManager) getSystemService("alarm");
        am.setTimeZone("Asia/Tokyo");
        Settings.System.putString(getContentResolver(), "time_12_24", "12");
        Settings.System.putString(getContentResolver(), "date_format", "");
        Settings.Secure.putInt(getContentResolver(), "accessibility_captioning_enabled", 0);
        Settings.Secure.putInt(getContentResolver(), "accessibility_display_magnification_enabled", 0);
        Settings.Secure.putInt(getContentResolver(), "high_text_contrast_enabled", 0);
        Settings.Secure.putInt(getContentResolver(), "speak_password", 0);
        Settings.Global.putInt(getContentResolver(), "enable_accessibility_global_gesture_enabled", 0);
        Settings.Secure.putString(getContentResolver(), "long_press_timeout", "500");
        Settings.Secure.putInt(getContentResolver(), "accessibility_display_inversion_enabled", 0);
        Settings.Secure.putInt(getContentResolver(), "accessibility_display_daltonizer_enabled", 0);
        Log.d("DchaService", "setInitialSettingsSystem 0003");
    }

    protected void setInitialSettingsDevelopmentOptions() throws RemoteException {
        Log.d("DchaService", "setInitialSettingDevelopmentOptions start");
        this.mWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
        this.mDontPokeProperties = true;
        resetDebuggerOptions();
        Settings.Global.putInt(getContentResolver(), "development_settings_enabled", 0);
        Settings.Global.putInt(getContentResolver(), "stay_on_while_plugged_in", 0);
        Settings.Secure.putInt(getContentResolver(), "bluetooth_hci_log", 0);
        Settings.Global.putInt(getContentResolver(), "adb_enabled", 0);
        Settings.Secure.putInt(getContentResolver(), "bugreport_in_power_menu", 0);
        AppOpsManager aom = (AppOpsManager) getSystemService("appops");
        List<AppOpsManager.PackageOps> packageOps = aom.getPackagesForOps(new int[]{58});
        if (packageOps != null) {
            for (AppOpsManager.PackageOps packageOp : packageOps) {
                if (((AppOpsManager.OpEntry) packageOp.getOps().get(0)).getMode() != 2) {
                    String oldMockLocationApp = packageOp.getPackageName();
                    try {
                        ApplicationInfo ai = getPackageManager().getApplicationInfo(oldMockLocationApp, 512);
                        aom.setMode(58, ai.uid, oldMockLocationApp, 2);
                    } catch (PackageManager.NameNotFoundException e) {
                    }
                }
            }
        }
        Settings.Global.putInt(getContentResolver(), "debug_view_attributes", 0);
        Settings.Global.putString(getContentResolver(), "debug_app", "");
        Settings.Global.putInt(getContentResolver(), "wait_for_debugger", 0);
        Settings.Global.putInt(getContentResolver(), "verifier_verify_adb_installs", 0);
        Settings.System.putInt(getContentResolver(), "screen_capture_on", 0);
        SystemProperties.set("persist.logd.size", "256K");
        Settings.System.putInt(getContentResolver(), "show_touches", 0);
        Settings.System.putInt(getContentResolver(), "pointer_location", 0);
        writeShowUpdatesOption();
        writeDebugLayoutOptions();
        writeDebuggerOptions();
        Settings.Global.putInt(getContentResolver(), "debug.force_rtl", 0);
        SystemProperties.set("debug.force_rtl", "0");
        writeAnimationScaleOption(0, this.mWindowAnimationScale, null);
        writeAnimationScaleOption(1, this.mTransitionAnimationScale, null);
        writeAnimationScaleOption(2, this.mAnimatorDurationScale, null);
        writeOverlayDisplayDevicesOptions(null);
        writeHardwareUiOptions();
        writeShowHwScreenUpdatesOptions();
        writeShowHwLayersUpdatesOptions();
        writeShowHwOverdrawOptions();
        SystemProperties.set("debug.hwui.show_non_rect_clip", "0");
        writeMsaaOptions();
        writeDisableOverlaysOption();
        Settings.Secure.putInt(getContentResolver(), "accessibility_display_daltonizer_enabled", 0);
        SystemProperties.set("persist.sys.media.use-awesome", "false");
        Settings.Secure.putInt(getContentResolver(), "usb_audio_automatic_routing_disabled", 0);
        writeStrictModeVisualOptions();
        writeCpuUsageOptions();
        writeTrackFrameTimeOptions();
        SystemProperties.set("debug.egl.trace", "");
        writeImmediatelyDestroyActivitiesOptions();
        writeAppProcessLimitOptions(null);
        Settings.Secure.putInt(getContentResolver(), "anr_show_background", 0);
        this.mDontPokeProperties = false;
        pokeSystemProperties();
        Settings.Global.putInt(getContentResolver(), "development_settings_enabled", 0);
        this.mLastEnabledState = Settings.Global.getInt(getContentResolver(), "development_settings_enabled", 0) != 0;
        if (this.mLastEnabledState) {
            Log.d("DchaService", "setInitialSettingDevelopmentOptions 0002");
            writeShowUpdatesOption();
            Settings.Global.putInt(getContentResolver(), "development_settings_enabled", 0);
        }
        Log.d("DchaService", "setInitialSettingDevelopmentOptions 0003");
    }

    protected static boolean isSystemPackage(PackageManager pm, PackageInfo pkg) {
        if (sSystemSignature == null) {
            sSystemSignature = new Signature[]{getSystemSignature(pm)};
        }
        if (sSystemSignature[0] != null) {
            return sSystemSignature[0].equals(getFirstSignature(pkg));
        }
        return false;
    }

    private static Signature getFirstSignature(PackageInfo pkg) {
        if (pkg == null || pkg.signatures == null || pkg.signatures.length <= 0) {
            return null;
        }
        return pkg.signatures[0];
    }

    private static Signature getSystemSignature(PackageManager pm) {
        try {
            PackageInfo sys = pm.getPackageInfo("android", 64);
            return getFirstSignature(sys);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private int getNewPriorityCategories(NotificationManager.Policy policy, boolean allow, int categoryType) {
        int priorityCategories = policy.priorityCategories;
        if (allow) {
            return priorityCategories | categoryType;
        }
        return priorityCategories & (~categoryType);
    }

    private NotificationManager.Policy savePolicy(NotificationManager.Policy policy, int priorityCategories, int priorityCallSenders, int priorityMessageSenders, int suppressedVisualEffects) {
        NotificationManager.Policy policy2 = new NotificationManager.Policy(priorityCategories, priorityCallSenders, priorityMessageSenders, suppressedVisualEffects);
        NotificationManager.from(getApplicationContext()).setNotificationPolicy(policy2);
        return policy2;
    }
}
