package jp.co.benesse.dcha.dchaservice;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.ActivityThread;
import android.app.AlarmManager;
import android.app.AppOpsManager;
import android.app.IActivityManager;
import android.app.INotificationManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.WallpaperManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.Signature;
import android.content.res.Configuration;
import android.hardware.input.InputManager;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.NetworkPolicyManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
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
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.IWindowManager;
import com.android.internal.app.LocalePicker;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.widget.LockPatternUtils;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import jp.co.benesse.dcha.dchaservice.IDchaService;
import jp.co.benesse.dcha.dchaservice.util.Log;

/* loaded from: classes.dex */
public class DchaService extends Service {
    private static boolean doCancelDigichalizedFlg = false;
    private static Signature[] sSystemSignature;
    protected IDchaService.Stub mDchaServiceStub = new IDchaService.Stub() { // from class: jp.co.benesse.dcha.dchaservice.DchaService.1
        @Override // jp.co.benesse.dcha.dchaservice.IDchaService
        public boolean verifyUpdateImage(String str) {
            Log.d("DchaService", "verifyUpdateImage 0001 updateFile:" + str);
            return true;
        }

        /*  JADX ERROR: Type inference failed
            jadx.core.utils.exceptions.JadxOverflowException: Type inference error: updates count limit reached
            	at jadx.core.utils.ErrorsCounter.addError(ErrorsCounter.java:59)
            	at jadx.core.utils.ErrorsCounter.error(ErrorsCounter.java:31)
            	at jadx.core.dex.attributes.nodes.NotificationAttrNode.addError(NotificationAttrNode.java:19)
            	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.visit(TypeInferenceVisitor.java:77)
            */
        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [288=8, 289=5, 290=5, 292=5, 293=5, 294=5, 296=5, 297=5, 299=5] */
        @Override // jp.co.benesse.dcha.dchaservice.IDchaService
        public boolean copyUpdateImage(java.lang.String r11, java.lang.String r12) {
            /*
                r10 = this;
                java.lang.String r0 = "DchaService"
                java.lang.StringBuilder r1 = new java.lang.StringBuilder
                r1.<init>()
                java.lang.String r2 = "copyUpdateImage 0001 srcFile:"
                r1.append(r2)
                r1.append(r11)
                java.lang.String r2 = " dstFile:"
                r1.append(r2)
                r1.append(r12)
                java.lang.String r1 = r1.toString()
                jp.co.benesse.dcha.dchaservice.util.Log.d(r0, r1)
                boolean r0 = android.text.TextUtils.isEmpty(r11)
                r1 = 0
                if (r0 != 0) goto L181
                boolean r0 = android.text.TextUtils.isEmpty(r12)
                if (r0 == 0) goto L2d
                goto L181
            L2d:
                java.lang.String r0 = "/cache"
                boolean r0 = r12.startsWith(r0)
                if (r0 != 0) goto L3d
                java.lang.String r11 = "DchaService"
                java.lang.String r12 = "copyUpdateImage 0010"
                jp.co.benesse.dcha.dchaservice.util.Log.d(r11, r12)
                return r1
            L3d:
                r0 = 0
                java.io.File r2 = new java.io.File     // Catch: java.lang.Throwable -> Lbe java.lang.Exception -> Lc2 java.io.IOException -> Led java.io.FileNotFoundException -> L119
                r2.<init>(r11)     // Catch: java.lang.Throwable -> Lbe java.lang.Exception -> Lc2 java.io.IOException -> Led java.io.FileNotFoundException -> L119
                java.io.File r11 = new java.io.File     // Catch: java.lang.Throwable -> Lbe java.lang.Exception -> Lc2 java.io.IOException -> Led java.io.FileNotFoundException -> L119
                r11.<init>(r12)     // Catch: java.lang.Throwable -> Lbe java.lang.Exception -> Lc2 java.io.IOException -> Led java.io.FileNotFoundException -> L119
                java.io.FileInputStream r12 = new java.io.FileInputStream     // Catch: java.lang.Throwable -> Lbe java.lang.Exception -> Lc2 java.io.IOException -> Led java.io.FileNotFoundException -> L119
                r12.<init>(r2)     // Catch: java.lang.Throwable -> Lbe java.lang.Exception -> Lc2 java.io.IOException -> Led java.io.FileNotFoundException -> L119
                java.nio.channels.FileChannel r12 = r12.getChannel()     // Catch: java.lang.Throwable -> Lbe java.lang.Exception -> Lc2 java.io.IOException -> Led java.io.FileNotFoundException -> L119
                java.io.FileOutputStream r2 = new java.io.FileOutputStream     // Catch: java.lang.Throwable -> La9 java.lang.Exception -> Laf java.io.IOException -> Lb4 java.io.FileNotFoundException -> Lb9
                r2.<init>(r11)     // Catch: java.lang.Throwable -> La9 java.lang.Exception -> Laf java.io.IOException -> Lb4 java.io.FileNotFoundException -> Lb9
                java.nio.channels.FileChannel r11 = r2.getChannel()     // Catch: java.lang.Throwable -> La9 java.lang.Exception -> Laf java.io.IOException -> Lb4 java.io.FileNotFoundException -> Lb9
                r4 = 0
                long r6 = r12.size()     // Catch: java.lang.Throwable -> L8e java.lang.Exception -> L95 java.io.IOException -> L9b java.io.FileNotFoundException -> La2
                r3 = r12
                r8 = r11
                r3.transferTo(r4, r6, r8)     // Catch: java.lang.Throwable -> L8e java.lang.Exception -> L95 java.io.IOException -> L9b java.io.FileNotFoundException -> La2
                r1 = 1
                java.lang.String r0 = "DchaService"
                java.lang.String r2 = "copyUpdateImage 0005"
                jp.co.benesse.dcha.dchaservice.util.Log.d(r0, r2)
                if (r12 == 0) goto L7e
                java.lang.String r0 = "DchaService"
                java.lang.String r2 = "copyUpdateImage 0006"
                jp.co.benesse.dcha.dchaservice.util.Log.d(r0, r2)
                r12.close()     // Catch: java.io.IOException -> L7d
                goto L7e
            L7d:
                r12 = move-exception
            L7e:
                if (r11 == 0) goto L145
                java.lang.String r12 = "DchaService"
                java.lang.String r0 = "copyUpdateImage 0007"
                jp.co.benesse.dcha.dchaservice.util.Log.d(r12, r0)
                r11.close()     // Catch: java.io.IOException -> L8c
            L8a:
                goto L145
            L8c:
                r11 = move-exception
                goto L8a
            L8e:
                r0 = move-exception
                r9 = r12
                r12 = r11
                r11 = r0
                r0 = r9
                goto L15d
            L95:
                r0 = move-exception
                r9 = r12
                r12 = r11
                r11 = r0
                r0 = r9
                goto Lc4
            L9b:
                r0 = move-exception
                r9 = r12
                r12 = r11
                r11 = r0
                r0 = r9
                goto Lef
            La2:
                r0 = move-exception
                r9 = r12
                r12 = r11
                r11 = r0
                r0 = r9
                goto L11b
            La9:
                r11 = move-exception
                r9 = r0
                r0 = r12
                r12 = r9
                goto L15d
            Laf:
                r11 = move-exception
                r9 = r0
                r0 = r12
                r12 = r9
                goto Lc4
            Lb4:
                r11 = move-exception
                r9 = r0
                r0 = r12
                r12 = r9
                goto Lef
            Lb9:
                r11 = move-exception
                r9 = r0
                r0 = r12
                r12 = r9
                goto L11b
            Lbe:
                r11 = move-exception
                r12 = r0
                goto L15d
            Lc2:
                r11 = move-exception
                r12 = r0
            Lc4:
                java.lang.String r2 = "DchaService"
                java.lang.String r3 = "copyUpdateImage 0004"
                jp.co.benesse.dcha.dchaservice.util.Log.e(r2, r3, r11)     // Catch: java.lang.Throwable -> L15c
                java.lang.String r11 = "DchaService"
                java.lang.String r2 = "copyUpdateImage 0005"
                jp.co.benesse.dcha.dchaservice.util.Log.d(r11, r2)
                if (r0 == 0) goto Le0
                java.lang.String r11 = "DchaService"
                java.lang.String r2 = "copyUpdateImage 0006"
                jp.co.benesse.dcha.dchaservice.util.Log.d(r11, r2)
                r0.close()     // Catch: java.io.IOException -> Ldf
                goto Le0
            Ldf:
                r11 = move-exception
            Le0:
                if (r12 == 0) goto L145
                java.lang.String r11 = "DchaService"
                java.lang.String r0 = "copyUpdateImage 0007"
                jp.co.benesse.dcha.dchaservice.util.Log.d(r11, r0)
                r12.close()     // Catch: java.io.IOException -> L8c
                goto L8a
            Led:
                r11 = move-exception
                r12 = r0
            Lef:
                java.lang.String r2 = "DchaService"
                java.lang.String r3 = "copyUpdateImage 0003"
                jp.co.benesse.dcha.dchaservice.util.Log.e(r2, r3, r11)     // Catch: java.lang.Throwable -> L15c
                java.lang.String r11 = "DchaService"
                java.lang.String r2 = "copyUpdateImage 0005"
                jp.co.benesse.dcha.dchaservice.util.Log.d(r11, r2)
                if (r0 == 0) goto L10b
                java.lang.String r11 = "DchaService"
                java.lang.String r2 = "copyUpdateImage 0006"
                jp.co.benesse.dcha.dchaservice.util.Log.d(r11, r2)
                r0.close()     // Catch: java.io.IOException -> L10a
                goto L10b
            L10a:
                r11 = move-exception
            L10b:
                if (r12 == 0) goto L145
                java.lang.String r11 = "DchaService"
                java.lang.String r0 = "copyUpdateImage 0007"
                jp.co.benesse.dcha.dchaservice.util.Log.d(r11, r0)
                r12.close()     // Catch: java.io.IOException -> L8c
                goto L8a
            L119:
                r11 = move-exception
                r12 = r0
            L11b:
                java.lang.String r2 = "DchaService"
                java.lang.String r3 = "copyUpdateImage 0002"
                jp.co.benesse.dcha.dchaservice.util.Log.e(r2, r3, r11)     // Catch: java.lang.Throwable -> L15c
                java.lang.String r11 = "DchaService"
                java.lang.String r2 = "copyUpdateImage 0005"
                jp.co.benesse.dcha.dchaservice.util.Log.d(r11, r2)
                if (r0 == 0) goto L137
                java.lang.String r11 = "DchaService"
                java.lang.String r2 = "copyUpdateImage 0006"
                jp.co.benesse.dcha.dchaservice.util.Log.d(r11, r2)
                r0.close()     // Catch: java.io.IOException -> L136
                goto L137
            L136:
                r11 = move-exception
            L137:
                if (r12 == 0) goto L145
                java.lang.String r11 = "DchaService"
                java.lang.String r0 = "copyUpdateImage 0007"
                jp.co.benesse.dcha.dchaservice.util.Log.d(r11, r0)
                r12.close()     // Catch: java.io.IOException -> L8c
                goto L8a
            L145:
                java.lang.String r11 = "DchaService"
                java.lang.StringBuilder r12 = new java.lang.StringBuilder
                r12.<init>()
                java.lang.String r0 = "copyUpdateImage 0008 return:"
                r12.append(r0)
                r12.append(r1)
                java.lang.String r12 = r12.toString()
                jp.co.benesse.dcha.dchaservice.util.Log.d(r11, r12)
                return r1
            L15c:
                r11 = move-exception
            L15d:
                java.lang.String r1 = "DchaService"
                java.lang.String r2 = "copyUpdateImage 0005"
                jp.co.benesse.dcha.dchaservice.util.Log.d(r1, r2)
                if (r0 == 0) goto L172
                java.lang.String r1 = "DchaService"
                java.lang.String r2 = "copyUpdateImage 0006"
                jp.co.benesse.dcha.dchaservice.util.Log.d(r1, r2)
                r0.close()     // Catch: java.io.IOException -> L171
                goto L172
            L171:
                r0 = move-exception
            L172:
                if (r12 == 0) goto L180
                java.lang.String r0 = "DchaService"
                java.lang.String r1 = "copyUpdateImage 0007"
                jp.co.benesse.dcha.dchaservice.util.Log.d(r0, r1)
                r12.close()     // Catch: java.io.IOException -> L17f
                goto L180
            L17f:
                r12 = move-exception
            L180:
                throw r11
            L181:
                java.lang.String r11 = "DchaService"
                java.lang.String r12 = "copyUpdateImage 0009"
                jp.co.benesse.dcha.dchaservice.util.Log.d(r11, r12)
                return r1
            */
            throw new UnsupportedOperationException("Method not decompiled: jp.co.benesse.dcha.dchaservice.DchaService.AnonymousClass1.copyUpdateImage(java.lang.String, java.lang.String):boolean");
        }

        @Override // jp.co.benesse.dcha.dchaservice.IDchaService
        public void rebootPad(int i, String str) throws IOException, RemoteException {
            Log.d("DchaService", "rebootPad 0001 rebootMode:" + i + " srcFile:" + str);
            try {
                PowerManager powerManager = (PowerManager) DchaService.this.getSystemService("power");
                switch (i) {
                    case 0:
                        Log.d("DchaService", "rebootPad 0002");
                        powerManager.reboot(null);
                        break;
                    case 1:
                        Log.d("DchaService", "rebootPad 0003");
                        RecoverySystem.rebootWipeUserData(DchaService.this.getBaseContext());
                        break;
                    case 2:
                        Log.d("DchaService", "rebootPad 0004");
                        if (str != null) {
                            RecoverySystem.installPackage(DchaService.this.getBaseContext(), new File(str));
                            break;
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

        @Override // jp.co.benesse.dcha.dchaservice.IDchaService
        public void setDefaultPreferredHomeApp(String str) throws RemoteException {
            try {
                Log.d("DchaService", "setDefalutPreferredHomeApp 0001 packageName:" + str);
                PackageManager packageManager = DchaService.this.getPackageManager();
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction("android.intent.action.MAIN");
                intentFilter.addCategory("android.intent.category.HOME");
                intentFilter.addCategory("android.intent.category.DEFAULT");
                Intent intent = new Intent("android.intent.action.MAIN");
                intent.addCategory("android.intent.category.HOME");
                intent.addCategory("android.intent.category.DEFAULT");
                List<ResolveInfo> listQueryIntentActivities = packageManager.queryIntentActivities(intent, 0);
                ArrayList arrayList = new ArrayList();
                ComponentName componentName = null;
                for (ResolveInfo resolveInfo : listQueryIntentActivities) {
                    String str2 = resolveInfo.activityInfo.applicationInfo.packageName;
                    String str3 = resolveInfo.activityInfo.name;
                    Log.d("DchaService", "setDefalutPreferredHomeApp packName:" + str2);
                    Log.d("DchaService", "setDefalutPreferredHomeApp activityName:" + str3);
                    ComponentName componentName2 = new ComponentName(str2, str3);
                    arrayList.add(componentName2);
                    if (str2.equalsIgnoreCase(str)) {
                        Log.d("DchaService", "setDefalutPreferredHomeApp 0002");
                        Log.d("DchaService", "setDefalutPreferredHomeApp defaultHomeComponentName:" + componentName2);
                        componentName = componentName2;
                    }
                }
                ComponentName[] componentNameArr = (ComponentName[]) arrayList.toArray(new ComponentName[arrayList.size()]);
                if (componentName != null) {
                    Log.d("DchaService", "setDefalutPreferredHomeApp 0003");
                    packageManager.addPreferredActivityAsUser(intentFilter, 1081344, componentNameArr, componentName, 0);
                }
                Log.d("DchaService", "setDefalutPreferredHomeApp 0005");
            } catch (Exception e) {
                Log.e("DchaService", "setDefalutPrefferredHomeApp 0004", e);
                throw new RemoteException(e.toString());
            }
        }

        @Override // jp.co.benesse.dcha.dchaservice.IDchaService
        public void clearDefaultPreferredApp(String str) throws RemoteException {
            Log.d("DchaService", "clearDefaultPreferredApp 0001 packageName:" + str);
            try {
                DchaService.this.getPackageManager().clearPackagePreferredActivities(str);
            } catch (Exception e) {
                Log.e("DchaService", "clearDefaultPreferredApp 0002", e);
                throw new RemoteException(e.toString());
            }
        }

        @Override // jp.co.benesse.dcha.dchaservice.IDchaService
        public void disableADB() {
            Log.d("DchaService", "disableADB 0001");
            Settings.Secure.putInt(DchaService.this.getContentResolver(), "adb_enabled", 0);
        }

        @Override // jp.co.benesse.dcha.dchaservice.IDchaService
        public boolean checkPadRooted() throws RemoteException {
            Log.d("DchaService", "checkPadRooted 0001");
            return false;
        }

        /* renamed from: jp.co.benesse.dcha.dchaservice.DchaService$1$PackageInstallSessionCallback */
        class PackageInstallSessionCallback extends PackageInstaller.SessionCallback {
            boolean finished;
            boolean result;

            PackageInstallSessionCallback() {
            }

            @Override // android.content.pm.PackageInstaller.SessionCallback
            public void onCreated(int i) {
                Log.d("DchaService", "PackageInstallSessionCallback#onCreated 0001");
            }

            @Override // android.content.pm.PackageInstaller.SessionCallback
            public void onBadgingChanged(int i) {
                Log.d("DchaService", "PackageInstallSessionCallback#onBadgingChanged 0001");
            }

            @Override // android.content.pm.PackageInstaller.SessionCallback
            public void onActiveChanged(int i, boolean z) {
                Log.d("DchaService", "PackageInstallSessionCallback#onActiveChanged 0001");
            }

            @Override // android.content.pm.PackageInstaller.SessionCallback
            public void onProgressChanged(int i, float f) {
                Log.d("DchaService", "PackageInstallSessionCallback#onProgressChanged 0001");
            }

            @Override // android.content.pm.PackageInstaller.SessionCallback
            public void onFinished(int i, boolean z) {
                Log.d("DchaService", "PackageInstallSessionCallback#onFinished 0001");
                synchronized (this) {
                    this.finished = true;
                    this.result = z;
                    notifyAll();
                }
            }
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [573=5] */
        @Override // jp.co.benesse.dcha.dchaservice.IDchaService
        public boolean installApp(String str, int i) throws Throwable {
            HandlerThread handlerThread;
            Log.d("DchaService", "installApp 0001 path:" + str + " installFlag:" + i);
            try {
                try {
                    PackageInstaller packageInstaller = DchaService.this.getPackageManager().getPackageInstaller();
                    boolean z = true;
                    PackageInstaller.SessionParams sessionParams = new PackageInstaller.SessionParams(1);
                    sessionParams.setInstallLocation(1);
                    if (i != 2) {
                        z = false;
                    }
                    sessionParams.setAllowDowngrade(z);
                    int iCreateSession = packageInstaller.createSession(sessionParams);
                    PackageInstaller.Session sessionOpenSession = packageInstaller.openSession(iCreateSession);
                    if (sessionOpenSession == null) {
                        Log.d("DchaService", "installApp 0002");
                        return false;
                    }
                    handlerThread = new HandlerThread("installAppThread");
                    try {
                        handlerThread.start();
                        Handler handler = new Handler(handlerThread.getLooper());
                        PackageInstallSessionCallback packageInstallSessionCallback = new PackageInstallSessionCallback();
                        packageInstaller.registerSessionCallback(packageInstallSessionCallback, handler);
                        try {
                            installAppSession(sessionOpenSession, str);
                            Intent intent = new Intent("jp.co.benesse.dcha.dchaservice.INSTALL_COMPLETE");
                            intent.setPackage(DchaService.this.getPackageNameFromPid(Binder.getCallingPid()));
                            sessionOpenSession.commit(PendingIntent.getBroadcast(DchaService.this.getBaseContext(), iCreateSession, intent, 0).getIntentSender());
                            synchronized (packageInstallSessionCallback) {
                                while (!packageInstallSessionCallback.finished) {
                                    try {
                                        Log.d("DchaService", "installApp 0003");
                                        packageInstallSessionCallback.wait();
                                    } catch (InterruptedException e) {
                                        Log.d("DchaService", "installApp 0004");
                                    }
                                }
                            }
                            Log.d("DchaService", "installApp 0005");
                            packageInstaller.unregisterSessionCallback(packageInstallSessionCallback);
                            sessionOpenSession.close();
                            Log.d("DchaService", "installApp 0006");
                            boolean z2 = packageInstallSessionCallback.result;
                            Log.d("DchaService", "installApp 0008");
                            handlerThread.quit();
                            return z2;
                        } catch (Throwable th) {
                            Log.d("DchaService", "installApp 0005");
                            packageInstaller.unregisterSessionCallback(packageInstallSessionCallback);
                            sessionOpenSession.close();
                            throw th;
                        }
                    } catch (Exception e2) {
                        e = e2;
                        Log.e("DchaService", "installApp 0007", e);
                        throw new RemoteException();
                    } catch (Throwable th2) {
                        th = th2;
                        if (handlerThread != null) {
                            Log.d("DchaService", "installApp 0008");
                            handlerThread.quit();
                        }
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    handlerThread = null;
                }
            } catch (Exception e3) {
                e = e3;
            }
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [619=4] */
        /* JADX WARN: Removed duplicated region for block: B:63:0x009a A[EXC_TOP_SPLITTER, SYNTHETIC] */
        /* JADX WARN: Removed duplicated region for block: B:65:0x008a A[EXC_TOP_SPLITTER, SYNTHETIC] */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        private void installAppSession(PackageInstaller.Session session, String str) throws Throwable {
            long length;
            OutputStream outputStreamOpenWrite;
            Log.d("DchaService", "installAppSession 0001");
            File file = new File(str);
            if (file.isFile()) {
                Log.d("DchaService", "installAppSession 0002");
                length = file.length();
            } else {
                length = -1;
            }
            long j = length;
            FileInputStream fileInputStream = null;
            try {
                FileInputStream fileInputStream2 = new FileInputStream(str);
                try {
                    outputStreamOpenWrite = session.openWrite("jp.co.benesse.dcha.dchaservice", 0L, j);
                    try {
                        byte[] bArr = new byte[65536];
                        while (true) {
                            int i = fileInputStream2.read(bArr);
                            if (i == -1) {
                                break;
                            } else {
                                outputStreamOpenWrite.write(bArr, 0, i);
                            }
                        }
                        session.fsync(outputStreamOpenWrite);
                        if (outputStreamOpenWrite != null) {
                            try {
                                outputStreamOpenWrite.close();
                            } catch (IOException e) {
                                Log.e("DchaService", "installAppSession 0004", e);
                                throw e;
                            }
                        }
                        try {
                            fileInputStream2.close();
                            Log.d("DchaService", "installAppSession 0006");
                        } catch (IOException e2) {
                            Log.e("DchaService", "installAppSession 0005", e2);
                            throw e2;
                        }
                    } catch (IOException e3) {
                        e = e3;
                        fileInputStream = fileInputStream2;
                        try {
                            Log.e("DchaService", "installAppSession 0003", e);
                            throw e;
                        } catch (Throwable th) {
                            th = th;
                            if (outputStreamOpenWrite != null) {
                                try {
                                    outputStreamOpenWrite.close();
                                } catch (IOException e4) {
                                    Log.e("DchaService", "installAppSession 0004", e4);
                                    throw e4;
                                }
                            }
                            if (fileInputStream != null) {
                                try {
                                    fileInputStream.close();
                                } catch (IOException e5) {
                                    Log.e("DchaService", "installAppSession 0005", e5);
                                    throw e5;
                                }
                            }
                            throw th;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        fileInputStream = fileInputStream2;
                        if (outputStreamOpenWrite != null) {
                        }
                        if (fileInputStream != null) {
                        }
                        throw th;
                    }
                } catch (IOException e6) {
                    e = e6;
                    outputStreamOpenWrite = null;
                } catch (Throwable th3) {
                    th = th3;
                    outputStreamOpenWrite = null;
                }
            } catch (IOException e7) {
                e = e7;
                outputStreamOpenWrite = null;
            } catch (Throwable th4) {
                th = th4;
                outputStreamOpenWrite = null;
            }
        }

        /* renamed from: jp.co.benesse.dcha.dchaservice.DchaService$1$PackageDeleteObserver */
        class PackageDeleteObserver extends IPackageDeleteObserver.Stub {
            boolean finished;
            boolean result;

            PackageDeleteObserver() {
            }

            public void packageDeleted(String str, int i) {
                Log.d("DchaService", "packageDeleted 0001");
                synchronized (this) {
                    boolean z = true;
                    this.finished = true;
                    if (i != 1) {
                        z = false;
                    }
                    this.result = z;
                    notifyAll();
                }
            }
        }

        @Override // jp.co.benesse.dcha.dchaservice.IDchaService
        public boolean uninstallApp(String str, int i) throws RuntimeException {
            Log.d("DchaService", "uninstallApp 0001 packageName:" + str + " uninstallFlag:" + i);
            try {
                String packageNameFromPid = DchaService.this.getPackageNameFromPid(getCallingPid());
                EmergencyLog.write(DchaService.this, "ELK002", str + " " + packageNameFromPid);
                PackageManager packageManager = DchaService.this.getPackageManager();
                int i2 = 2;
                if (i == 1) {
                    Log.d("DchaService", "uninstallApp 0002");
                    i2 = 3;
                } else {
                    Log.d("DchaService", "uninstallApp 0003");
                }
                PackageDeleteObserver packageDeleteObserver = new PackageDeleteObserver();
                packageManager.deletePackage(str, packageDeleteObserver, i2);
                synchronized (packageDeleteObserver) {
                    while (!packageDeleteObserver.finished) {
                        try {
                            packageDeleteObserver.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                }
                Log.d("DchaService", "uninstallApp 0004");
                return packageDeleteObserver.result;
            } catch (Exception e2) {
                Log.e("DchaService", "uninstallApp 0005", e2);
                throw new RuntimeException();
            }
        }

        @Override // jp.co.benesse.dcha.dchaservice.IDchaService
        public void cancelSetup() throws RemoteException {
            Log.d("DchaService", "cancelSetup 0001");
            DchaService.this.doCancelDigichalized();
        }

        @Override // jp.co.benesse.dcha.dchaservice.IDchaService
        public void setSetupStatus(int i) {
            Log.d("DchaService", "setSetupStatus 0001 status:" + i);
            PreferenceManager.getDefaultSharedPreferences(DchaService.this).edit().putInt("DigichalizedStatus", i).commit();
            Settings.System.putInt(DchaService.this.getContentResolver(), "dcha_state", i);
        }

        @Override // jp.co.benesse.dcha.dchaservice.IDchaService
        public int getSetupStatus() {
            Log.d("DchaService", "getSetupStatus 0001");
            int i = PreferenceManager.getDefaultSharedPreferences(DchaService.this).getInt("DigichalizedStatus", -1);
            Log.d("DchaService", "getSetupStatus 0002 return:" + i);
            return i;
        }

        @Override // jp.co.benesse.dcha.dchaservice.IDchaService
        public void setSystemTime(String str, String str2) {
            Log.d("DchaService", "setSystemTime 0001 time:" + str + " timeFormat:" + str2);
            try {
                Date date = new SimpleDateFormat(str2, Locale.JAPAN).parse(str);
                String packageNameFromPid = DchaService.this.getPackageNameFromPid(Binder.getCallingPid());
                Calendar calendar = Calendar.getInstance(Locale.JAPAN);
                calendar.set(2016, 1, 1, 0, 0);
                Calendar calendar2 = Calendar.getInstance(Locale.JAPAN);
                calendar2.setTime(date);
                if (calendar.compareTo(calendar2) > 0) {
                    Log.d("DchaService", "setSystemTime 0002");
                    EmergencyLog.write(DchaService.this, "ELK008", str + " " + packageNameFromPid);
                } else {
                    Log.d("DchaService", "setSystemTime 0003");
                    SystemClock.setCurrentTimeMillis(date.getTime());
                }
                Log.d("DchaService", "setSystemTime set time :" + date);
            } catch (Exception e) {
                Log.e("DchaService", "setSystemTime 0004", e);
            }
            Log.d("DchaService", "setSystemTime 0005");
        }

        @Override // jp.co.benesse.dcha.dchaservice.IDchaService
        public void removeTask(String str) throws RemoteException {
            Log.d("DchaService", "removeTask 0001 packageName:" + str);
            try {
                IActivityManager service = ActivityManager.getService();
                List<ActivityManager.RecentTaskInfo> list = service.getRecentTasks(30, 1, UserHandle.myUserId()).getList();
                if (str != null) {
                    Log.d("DchaService", "removeTask 0002");
                    for (ActivityManager.RecentTaskInfo recentTaskInfo : list) {
                        if (str.equals(recentTaskInfo.baseIntent.getComponent().getPackageName())) {
                            Log.d("DchaService", "removeTask 0003");
                            service.removeTask(recentTaskInfo.persistentId);
                        }
                    }
                } else {
                    Log.d("DchaService", "removeTask 0004");
                    Iterator it = list.iterator();
                    while (it.hasNext()) {
                        service.removeTask(((ActivityManager.RecentTaskInfo) it.next()).persistentId);
                    }
                }
                Log.d("DchaService", "removeTask 0006");
            } catch (Exception e) {
                Log.e("DchaService", "removeTask 0005", e);
                throw new RemoteException();
            }
        }

        @Override // jp.co.benesse.dcha.dchaservice.IDchaService
        public void sdUnmount() throws RemoteException {
            Log.d("DchaService", "sdUnmount 0001");
            try {
                StorageManager storageManager = (StorageManager) DchaService.this.getSystemService("storage");
                for (StorageVolume storageVolume : storageManager.getStorageVolumes()) {
                    if (storageVolume.isRemovable()) {
                        Log.d("DchaService", "sdUnmount 0002");
                        storageManager.unmount(storageVolume.getId());
                    }
                }
                Log.d("DchaService", "sdUnmount 0004");
            } catch (Exception e) {
                Log.e("DchaService", "sdUnmount 0003", e);
                throw new RemoteException();
            }
        }

        @Override // jp.co.benesse.dcha.dchaservice.IDchaService
        public void setDefaultParam() throws PackageManager.NameNotFoundException, NumberFormatException, RemoteException {
            Log.d("DchaService", "setDefaultParam 0001");
            try {
                DchaService.this.setInitialSettingsWirelessNetwork();
                DchaService.this.setInitialSettingsTerminal();
                DchaService.this.setInitialSettingsUser();
                DchaService.this.setInitialSettingsAccount();
                DchaService.this.setInitialSettingsSystem();
                if (!TextUtils.equals(Build.TYPE, "eng")) {
                    Log.d("DchaService", "setDefaultParam 0002");
                    DchaService.this.setInitialSettingsDevelopmentOptions();
                }
                DchaService.this.pokeSystemProperties();
                Log.d("DchaService", "setDefaultParam 0004");
            } catch (RemoteException e) {
                Log.e("DchaService", "setDefaultParam 0003", e);
                throw new RemoteException();
            }
        }

        @Override // jp.co.benesse.dcha.dchaservice.IDchaService
        public String getForegroundPackageName() throws RemoteException {
            Log.d("DchaService", "getForegroundPackageName 0001");
            try {
                List tasks = ActivityManagerNative.getDefault().getTasks(1);
                String packageName = ((ActivityManager.RunningTaskInfo) tasks.get(0)).baseActivity.getPackageName();
                Log.d("DchaService", "Foreground package name :" + packageName);
                tasks.clear();
                Log.d("DchaService", "getForegroundPackageName 0003 return:" + packageName);
                return packageName;
            } catch (Exception e) {
                Log.e("DchaService", "getForegroundPackageName 0002", e);
                throw new RemoteException();
            }
        }

        @Override // jp.co.benesse.dcha.dchaservice.IDchaService
        public boolean copyFile(String str, String str2) throws Throwable {
            boolean zFileCopy;
            String canonicalExternalPath;
            Log.d("DchaService", "copyFile 0001 srcFilePath:" + str + " dstFilePath:" + str2);
            try {
                canonicalExternalPath = getCanonicalExternalPath(System.getenv("SECONDARY_STORAGE"));
            } catch (Exception e) {
                Log.e("DchaService", "copyFile 0006", e);
                zFileCopy = false;
            }
            if (TextUtils.isEmpty(str) || TextUtils.isEmpty(str2)) {
                Log.d("DchaService", "copyFile 0002");
                throw new IllegalArgumentException();
            }
            String canonicalExternalPath2 = getCanonicalExternalPath(str);
            String canonicalExternalPath3 = getCanonicalExternalPath(str2);
            if (!canonicalExternalPath2.startsWith(canonicalExternalPath) && !canonicalExternalPath3.startsWith(canonicalExternalPath)) {
                Log.d("DchaService", "copyFile 0003");
                throw new SecurityException("The path is not a external storage.");
            }
            File file = new File(canonicalExternalPath2);
            File file2 = new File(canonicalExternalPath3);
            if (file.isFile() && (file2.isDirectory() || file2.isFile() || (!file2.exists() && file2.getParentFile().isDirectory()))) {
                Log.d("DchaService", "copyFile 0004");
                zFileCopy = fileCopy(file, file2);
                Log.d("DchaService", "copyFile 0007 result:" + zFileCopy);
                return zFileCopy;
            }
            Log.d("DchaService", "copyFile 0005");
            throw new IllegalArgumentException();
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1050=5, 1053=8] */
        /* JADX DEBUG: Failed to insert an additional move for type inference into block B:15:0x0093 */
        /* JADX DEBUG: Failed to insert an additional move for type inference into block B:17:0x0095 */
        /* JADX WARN: Multi-variable type inference failed */
        /* JADX WARN: Type inference failed for: r10v1 */
        /* JADX WARN: Type inference failed for: r10v10, types: [java.io.Closeable, java.io.FileOutputStream] */
        /* JADX WARN: Type inference failed for: r10v11 */
        /* JADX WARN: Type inference failed for: r10v12 */
        /* JADX WARN: Type inference failed for: r10v3 */
        /* JADX WARN: Type inference failed for: r10v4, types: [java.io.Closeable] */
        /* JADX WARN: Type inference failed for: r10v5, types: [java.io.Closeable] */
        /* JADX WARN: Type inference failed for: r10v7 */
        /* JADX WARN: Type inference failed for: r10v8 */
        /* JADX WARN: Type inference failed for: r10v9 */
        /* JADX WARN: Type inference failed for: r12v0, types: [jp.co.benesse.dcha.dchaservice.DchaService$1] */
        protected boolean fileCopy(File file, File file2) throws Throwable {
            FileInputStream fileInputStream;
            FileChannel channel;
            FileChannel fileChannel;
            ?? fileOutputStream;
            FileChannel channel2;
            Log.d("DchaService", "fileCopy 0001");
            if (file2.isDirectory()) {
                Log.d("DchaService", "fileCopy 0002");
                file2 = new File(file2.getPath() + File.separator + file.getName());
            } else {
                Log.d("DchaService", "fileCopy 0003");
            }
            FileChannel fileChannel2 = null;
            try {
                file2.createNewFile();
                fileInputStream = new FileInputStream(file);
            } catch (Exception e) {
                e = e;
                fileInputStream = null;
                fileChannel = null;
            } catch (Throwable th) {
                th = th;
                fileInputStream = null;
                channel = null;
            }
            try {
                channel = fileInputStream.getChannel();
            } catch (Exception e2) {
                e = e2;
                fileChannel = null;
                channel2 = fileChannel;
                fileOutputStream = fileChannel;
                try {
                    Log.e("DchaService", "fileCopy 0005", e);
                    Log.d("DchaService", "fileCopy 0006");
                    close(fileChannel2);
                    close(channel2);
                    close(fileInputStream);
                    close(fileOutputStream);
                    Log.d("DchaService", "fileCopy 0007");
                    return false;
                } catch (Throwable th2) {
                    th = th2;
                    channel = fileChannel2;
                    Log.d("DchaService", "fileCopy 0006");
                    close(channel);
                    close(channel2);
                    close(fileInputStream);
                    close(fileOutputStream);
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                channel = null;
                fileOutputStream = channel;
                channel2 = fileOutputStream;
                Log.d("DchaService", "fileCopy 0006");
                close(channel);
                close(channel2);
                close(fileInputStream);
                close(fileOutputStream);
                throw th;
            }
            try {
                fileOutputStream = new FileOutputStream(file2);
                try {
                    channel2 = fileOutputStream.getChannel();
                } catch (Exception e3) {
                    e = e3;
                    channel2 = null;
                } catch (Throwable th4) {
                    th = th4;
                    channel2 = null;
                }
                try {
                    channel.transferTo(0L, channel.size(), channel2);
                    file2.setLastModified(file.lastModified());
                    file2.setReadable(true, false);
                    file2.setWritable(true, false);
                    file2.setExecutable(true, false);
                    Log.d("DchaService", "fileCopy 0004");
                    Log.d("DchaService", "fileCopy 0006");
                    close(channel);
                    close(channel2);
                    close(fileInputStream);
                    close(fileOutputStream);
                    return true;
                } catch (Exception e4) {
                    e = e4;
                    fileChannel2 = channel;
                    fileOutputStream = fileOutputStream;
                    Log.e("DchaService", "fileCopy 0005", e);
                    Log.d("DchaService", "fileCopy 0006");
                    close(fileChannel2);
                    close(channel2);
                    close(fileInputStream);
                    close(fileOutputStream);
                    Log.d("DchaService", "fileCopy 0007");
                    return false;
                } catch (Throwable th5) {
                    th = th5;
                    Log.d("DchaService", "fileCopy 0006");
                    close(channel);
                    close(channel2);
                    close(fileInputStream);
                    close(fileOutputStream);
                    throw th;
                }
            } catch (Exception e5) {
                e = e5;
                fileOutputStream = 0;
                channel2 = null;
            } catch (Throwable th6) {
                th = th6;
                fileOutputStream = 0;
                channel2 = fileOutputStream;
                Log.d("DchaService", "fileCopy 0006");
                close(channel);
                close(channel2);
                close(fileInputStream);
                close(fileOutputStream);
                throw th;
            }
        }

        protected void close(Closeable closeable) throws IOException {
            Log.d("DchaService", "close 0001");
            if (closeable == null) {
                Log.d("DchaService", "close 0002");
                return;
            }
            try {
                closeable.close();
            } catch (IOException e) {
                Log.e("DchaService", "close 0003", e);
            }
            Log.d("DchaService", "close 0004");
        }

        @Override // jp.co.benesse.dcha.dchaservice.IDchaService
        public boolean deleteFile(String str) throws RemoteException {
            boolean zDelete;
            String canonicalExternalPath;
            Log.d("DchaService", "deleteFile 0001 path:" + str);
            try {
                canonicalExternalPath = getCanonicalExternalPath(System.getenv("SECONDARY_STORAGE"));
            } catch (Exception e) {
                Log.e("DchaService", "deleteFile 0007", e);
                zDelete = false;
            }
            if (TextUtils.isEmpty(str)) {
                Log.d("DchaService", "deleteFile 0002");
                throw new IllegalArgumentException();
            }
            String canonicalExternalPath2 = getCanonicalExternalPath(str);
            if (!canonicalExternalPath2.startsWith(canonicalExternalPath)) {
                Log.d("DchaService", "deleteFile 0003");
                throw new SecurityException("The path is not a external storage.");
            }
            File file = new File(canonicalExternalPath2);
            if (file.isDirectory()) {
                Log.d("DchaService", "deleteFile 0004");
                File[] fileArrListFiles = file.listFiles();
                if (fileArrListFiles == null) {
                    Log.d("DchaService", "deleteFile 0005");
                    return false;
                }
                for (File file2 : fileArrListFiles) {
                    if (!deleteFile(file2.getCanonicalPath())) {
                        Log.d("DchaService", "deleteFile 0006");
                        return false;
                    }
                }
            }
            zDelete = file.delete();
            Log.d("DchaService", "deleteFile 0008 result:" + zDelete);
            return zDelete;
        }

        @Override // jp.co.benesse.dcha.dchaservice.IDchaService
        public int getUserCount() {
            Log.d("DchaService", "getUserCount 0001");
            int userCount = ((UserManager) DchaService.this.getSystemService("user")).getUserCount();
            Log.d("DchaService", "getUserCount 0002 return:" + userCount);
            return userCount;
        }

        @Override // jp.co.benesse.dcha.dchaservice.IDchaService
        public boolean isDeviceEncryptionEnabled() {
            Log.d("DchaService", "isDeviceEncryptionEnabled 0001");
            return false;
        }

        @Override // jp.co.benesse.dcha.dchaservice.IDchaService
        public void hideNavigationBar(boolean z) {
            Log.d("DchaService", "hideNavigationBar 0001 hide:" + z);
            DchaService.this.hideNavigationBar(z);
        }

        @Override // jp.co.benesse.dcha.dchaservice.IDchaService
        public void setPermissionEnforced(boolean z) throws RemoteException {
            Log.d("DchaService", "setPermissionEnforced 0001 enforced:" + z);
            ActivityThread.getPackageManager().setPermissionEnforced("android.permission.READ_EXTERNAL_STORAGE", z);
            Log.d("DchaService", "setPermissionEnforced 0002");
        }

        /* JADX WARN: Code restructure failed: missing block: B:21:0x0077, code lost:
        
            r1 = r2.getInternalPath();
            jp.co.benesse.dcha.dchaservice.util.Log.d("DchaService", "getCanonicalExternalPath 0005 path:" + r1);
         */
        /* JADX WARN: Code restructure failed: missing block: B:22:0x0096, code lost:
        
            r6 = r6.replace(r0, r1);
         */
        @Override // jp.co.benesse.dcha.dchaservice.IDchaService
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        public String getCanonicalExternalPath(String str) throws IOException, RemoteException {
            Log.d("DchaService", "getCanonicalExternalPath 0001 linkPath:" + str);
            if (TextUtils.isEmpty(str)) {
                Log.d("DchaService", "getCanonicalExternalPath 0002");
                return str;
            }
            try {
                str = new File(str).getCanonicalPath();
            } catch (Exception e) {
                Log.e("DchaService", "getCanonicalExternalPath 0003", e);
            }
            String str2 = System.getenv("SECONDARY_STORAGE");
            if (!TextUtils.isEmpty(str) && str.startsWith(str2)) {
                Log.d("DchaService", "getCanonicalExternalPath 0004");
                try {
                    Iterator<StorageVolume> it = ((StorageManager) DchaService.this.getSystemService("storage")).getStorageVolumes().iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        }
                        StorageVolume next = it.next();
                        if (next.isRemovable()) {
                            break;
                        }
                    }
                } catch (Exception e2) {
                    Log.e("DchaService", "getCanonicalExternalPath 0006", e2);
                }
            }
            Log.d("DchaService", "getCanonicalExternalPath 0007 return:" + str);
            return str;
        }
    };

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
            IWindowManager.Stub.asInterface(ServiceManager.getService("window")).setStrictModeVisualIndicatorPreference("");
        } catch (RemoteException e) {
        }
    }

    protected void writeShowUpdatesOption() throws RemoteException {
        Log.d("DchaService", "writeShowUpdatesOption 0001");
        try {
            IBinder service = ServiceManager.getService("SurfaceFlinger");
            if (service != null) {
                Log.d("DchaService", "writeShowUpdatesOption 0002");
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                parcelObtain.writeInterfaceToken("android.ui.ISurfaceComposer");
                service.transact(1010, parcelObtain, parcelObtain2, 0);
                parcelObtain2.readInt();
                parcelObtain2.readInt();
                int i = parcelObtain2.readInt();
                parcelObtain2.readInt();
                parcelObtain2.readInt();
                parcelObtain2.recycle();
                parcelObtain.recycle();
                if (i != 0) {
                    Log.d("DchaService", "writeShowUpdatesOption 0003");
                    Parcel parcelObtain3 = Parcel.obtain();
                    parcelObtain3.writeInterfaceToken("android.ui.ISurfaceComposer");
                    parcelObtain3.writeInt(0);
                    service.transact(1002, parcelObtain3, null, 0);
                    parcelObtain3.recycle();
                }
            }
        } catch (RemoteException e) {
        }
    }

    protected void writeDisableOverlaysOption() throws RemoteException {
        Log.d("DchaService", "writeDisableOverlaysOption 0001");
        try {
            IBinder service = ServiceManager.getService("SurfaceFlinger");
            if (service != null) {
                Log.d("DchaService", "writeDisableOverlaysOption 0002");
                Parcel parcelObtain = Parcel.obtain();
                parcelObtain.writeInterfaceToken("android.ui.ISurfaceComposer");
                parcelObtain.writeInt(0);
                service.transact(1008, parcelObtain, null, 0);
                parcelObtain.recycle();
            }
        } catch (RemoteException e) {
        }
    }

    protected void writeCpuUsageOptions() {
        Log.d("DchaService", "writeCpuUsageOptions 0001");
        Settings.Global.putInt(getContentResolver(), "show_processes", 0);
        Intent className = new Intent().setClassName("com.android.systemui", "com.android.systemui.LoadAverageService");
        Log.d("DchaService", "writeCpuUsageOptions 0003");
        stopService(className);
    }

    protected void writeImmediatelyDestroyActivitiesOptions() {
        Log.d("DchaService", "writeImmediatelyDestroyActivitiesOptions 0001");
        try {
            ActivityManagerNative.getDefault().setAlwaysFinish(false);
        } catch (RemoteException e) {
        }
    }

    protected void writeAnimationScaleOption(int i, ListPreference listPreference, Object obj) {
        Log.d("DchaService", "writeAnimationScaleOption 0001");
        try {
            IWindowManager.Stub.asInterface(ServiceManager.getService("window")).setAnimationScale(i, obj != null ? Float.parseFloat(obj.toString()) : 1.0f);
        } catch (RemoteException e) {
        }
    }

    protected void writeAppProcessLimitOptions(Object obj) throws NumberFormatException {
        int i;
        Log.d("DchaService", "writeAppProcessLimitOptions 0001");
        if (obj == null) {
            i = -1;
        } else {
            try {
                i = Integer.parseInt(obj.toString());
            } catch (RemoteException e) {
                return;
            }
        }
        ActivityManagerNative.getDefault().setProcessLimit(i);
    }

    protected void writeQsTileDisable() {
        Log.d("DchaService", "writeQsTileDisable 0001");
        PackageManager packageManager = getApplicationContext().getPackageManager();
        IStatusBarService iStatusBarServiceAsInterface = IStatusBarService.Stub.asInterface(ServiceManager.checkService("statusbar"));
        Iterator<ResolveInfo> it = packageManager.queryIntentServices(new Intent("android.service.quicksettings.action.QS_TILE"), 512).iterator();
        while (it.hasNext()) {
            ServiceInfo serviceInfo = it.next().serviceInfo;
            ComponentName componentName = new ComponentName(serviceInfo.packageName, serviceInfo.name);
            packageManager.setComponentEnabledSetting(componentName, 2, 1);
            if (iStatusBarServiceAsInterface != null) {
                try {
                    Log.d("DchaService", "writeQsTileDisable 0002");
                    iStatusBarServiceAsInterface.remTile(componentName);
                } catch (RemoteException e) {
                    Log.e("DchaService", "writeQsTileDisable 0003", e);
                }
            }
        }
    }

    void pokeSystemProperties() {
        Log.d("DchaService", "pokeSystemProperties 0001");
        new SystemPropPoker().execute(new Void[0]);
    }

    static class SystemPropPoker extends AsyncTask<Void, Void, Void> {
        private final String TAG = "SystemPropPoker";

        SystemPropPoker() {
        }

        /* JADX DEBUG: Method merged with bridge method: doInBackground([Ljava/lang/Object;)Ljava/lang/Object; */
        @Override // android.os.AsyncTask
        protected Void doInBackground(Void... voidArr) throws RemoteException {
            Log.d("SystemPropPoker", "doInBackground 0001");
            for (String str : ServiceManager.listServices()) {
                IBinder iBinderCheckService = ServiceManager.checkService(str);
                if (iBinderCheckService != null) {
                    Log.d("SystemPropPoker", "doInBackground 0003");
                    Parcel parcelObtain = Parcel.obtain();
                    try {
                        iBinderCheckService.transact(1599295570, parcelObtain, null, 0);
                    } catch (RemoteException e) {
                        Log.e("SystemPropPoker", "doInBackground 0004", e);
                    } catch (Exception e2) {
                        Log.v("DevSettings", "Somone wrote a bad service '" + str + "' that doesn't like to be poked: " + e2);
                        Log.e("SystemPropPoker", "doInBackground 0005", e2);
                    }
                    parcelObtain.recycle();
                }
            }
            Log.d("SystemPropPoker", "doInBackground 0006");
            return null;
        }
    }

    public void doCancelDigichalized() throws RemoteException {
        try {
            Log.d("DchaService", "doCancelDigichalized 0001");
            int setupStatus = this.mDchaServiceStub.getSetupStatus();
            Log.d("DchaService", "status:" + Integer.toString(setupStatus));
            String packageNameFromPid = getPackageNameFromPid(Binder.getCallingPid());
            EmergencyLog.write(this, "ELK000", setupStatus + " " + isFinishDigichalize() + " " + packageNameFromPid);
            if (setupStatus != 3 && !isFinishDigichalize()) {
                Log.d("DchaService", "doCancelDigichalized 0002");
                EmergencyLog.write(this, "ELK004", setupStatus + " " + isFinishDigichalize() + " " + packageNameFromPid);
                Intent intent = new Intent();
                intent.setPackage("jp.co.benesse.dcha.databox");
                intent.setAction("jp.co.benesse.dcha.databox.intent.action.COMMAND");
                intent.addCategory("jp.co.benesse.dcha.databox.intent.category.WIPE");
                intent.putExtra("send_service", "DchaService");
                sendBroadcast(intent);
                Log.d("DchaService", "doCancelDigichalized send wipeDataBoxIntent intent");
                HandlerThread handlerThread = new HandlerThread("handlerThread");
                handlerThread.start();
                new Handler(handlerThread.getLooper()).post(new Runnable() { // from class: jp.co.benesse.dcha.dchaservice.DchaService.2
                    @Override // java.lang.Runnable
                    public void run() {
                        if (!DchaService.doCancelDigichalizedFlg) {
                            Log.d("DchaService", "doCancelDigichalized 0003");
                            try {
                                try {
                                    Log.d("DchaService", "start uninstallApp");
                                    boolean unused = DchaService.doCancelDigichalizedFlg = true;
                                    for (ApplicationInfo applicationInfo : DchaService.this.getPackageManager().getInstalledApplications(128)) {
                                        if ((applicationInfo.flags & 1) != 1) {
                                            DchaService.this.mDchaServiceStub.uninstallApp(applicationInfo.packageName, 0);
                                        } else {
                                            Log.d("DchaService", "doCancelDigichalized 0004");
                                        }
                                    }
                                    DchaService.this.mDchaServiceStub.setSetupStatus(0);
                                    Log.d("DchaService", "end uninstallApp");
                                } catch (RemoteException e) {
                                    Log.e("DchaService", "doCancelDigichalized 0005", e);
                                }
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
                EmergencyLog.write(this, "ELK005", setupStatus + " " + isFinishDigichalize() + " " + packageNameFromPid);
            }
            Log.d("DchaService", "doCancelDigichalized 0010");
        } catch (Exception e) {
            Log.e("DchaService", "doCancelDigichalized 0009", e);
            throw new RemoteException();
        }
    }

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        Log.d("DchaService", "onBind 0001");
        return this.mDchaServiceStub;
    }

    @Override // android.app.Service
    public void onCreate() {
        Log.d("DchaService", "onCreate 0001");
        super.onCreate();
    }

    @Override // android.app.Service
    public void onDestroy() {
        Log.d("DchaService", "onDestroy 0001");
        super.onDestroy();
    }

    @Override // android.app.Service
    public int onStartCommand(Intent intent, int i, int i2) {
        Log.d("DchaService", "onStartCommand 0001");
        if (intent != null) {
            Log.d("DchaService", "onStartCommand 0002");
            int intExtra = intent.getIntExtra("REQ_COMMAND", 0);
            Log.d("DchaService", "onStartCommand intent command:" + intExtra);
            try {
                switch (intExtra) {
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
        return 2;
    }

    protected boolean isFinishDigichalize() {
        Log.d("DchaService", "isFinishDigichalize 0001");
        return UpdateLog.exists();
    }

    protected String getPackageNameFromPid(int i) {
        Log.d("DchaService", "getPackageNameFromPid 0001");
        String str = "Unknown";
        for (ActivityManager.RunningAppProcessInfo runningAppProcessInfo : ((ActivityManager) getSystemService("activity")).getRunningAppProcesses()) {
            if (i == runningAppProcessInfo.pid) {
                Log.d("DchaService", "getPackageNameFromPid 0002");
                str = runningAppProcessInfo.processName;
            }
        }
        Log.d("DchaService", "getPackageNameFromPid 0003");
        return str;
    }

    public void hideNavigationBar(boolean z) {
        Log.d("DchaService", "hideNavigationBar 0001");
        Settings.System.putInt(getContentResolver(), "hide_navigation_bar", z ? 1 : 0);
    }

    protected void setInitialSettingsWirelessNetwork() throws RemoteException {
        Log.d("DchaService", "setInitialSettingsWirelessNetwork 0001");
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService("wifi");
        if (wifiManager != null) {
            Log.d("DchaService", "setInitialSettingsWirelessNetwork 0002");
            wifiManager.setWifiEnabled(true);
            wifiManager.enableVerboseLogging(0);
            wifiManager.stopSoftAp();
        }
        Settings.Secure.putInt(getContentResolver(), "wifi_networks_available_notification_on", 0);
        Settings.Global.putInt(getContentResolver(), "wifi_scan_always_enabled", 0);
        Settings.Global.putInt(getContentResolver(), "wifi_sleep_policy", 0);
        Settings.Global.putInt(getContentResolver(), "wifi_display_on", 0);
        Settings.Global.putInt(getContentResolver(), "wifi_display_certification_on", 0);
        Settings.Global.putInt(getContentResolver(), "bluetooth_on", 0);
        BluetoothAdapter.getDefaultAdapter().disable();
        Settings.Global.putInt(getContentResolver(), "ble_scan_always_enabled", 0);
        Settings.Global.putInt(getContentResolver(), "airplane_mode_on", 0);
        Intent intent = new Intent("android.intent.action.AIRPLANE_MODE");
        intent.putExtra("state", false);
        sendBroadcast(intent);
        Log.d("DchaService", "setInitialSettingsWirelessNetwork 0003");
    }

    protected void setInitialSettingsTerminal() throws PackageManager.NameNotFoundException, RemoteException {
        Log.d("DchaService", "setInitialSettingsTerminal 0001");
        try {
            Settings.System.putInt(getContentResolver(), "screen_brightness_mode", 0);
            Settings.System.putLong(getContentResolver(), "screen_off_timeout", 900000L);
            Settings.System.putInt(getContentResolver(), "screen_dim_timeout", 300000);
            Settings.Secure.putInt(getContentResolver(), "screensaver_enabled", 0);
            Configuration configuration = new Configuration();
            configuration.fontScale = Float.parseFloat("1.0");
            ActivityManagerNative.getDefault().updatePersistentConfiguration(configuration);
            Settings.System.putInt(getContentResolver(), "accelerometer_rotation", 0);
            INotificationManager iNotificationManagerAsInterface = INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
            Context applicationContext = getApplicationContext();
            NotificationManager.Policy notificationPolicy = NotificationManager.from(applicationContext).getNotificationPolicy();
            NotificationManager.Policy policySavePolicy = savePolicy(notificationPolicy, getNewPriorityCategories(notificationPolicy, false, 1), notificationPolicy.priorityCallSenders, notificationPolicy.priorityMessageSenders, notificationPolicy.suppressedVisualEffects);
            NotificationManager.Policy policySavePolicy2 = savePolicy(policySavePolicy, getNewPriorityCategories(policySavePolicy, false, 2), policySavePolicy.priorityCallSenders, policySavePolicy.priorityMessageSenders, policySavePolicy.suppressedVisualEffects);
            NotificationManager.Policy policySavePolicy3 = savePolicy(policySavePolicy2, getNewPriorityCategories(policySavePolicy2, false, 4), policySavePolicy2.priorityCallSenders, policySavePolicy2.priorityMessageSenders, policySavePolicy2.suppressedVisualEffects);
            savePolicy(policySavePolicy3, getNewPriorityCategories(policySavePolicy3, false, 8), policySavePolicy3.priorityCallSenders, policySavePolicy3.priorityMessageSenders, policySavePolicy3.suppressedVisualEffects);
            int i = 3;
            if (this.mDchaServiceStub.getSetupStatus() != 3) {
                Log.d("DchaService", "setInitialSettingsTerminal 0002");
                AppOpsManager appOpsManager = (AppOpsManager) applicationContext.getSystemService("appops");
                try {
                    IPackageManager.Stub.asInterface(ServiceManager.getService("package")).resetApplicationPreferences(UserHandle.myUserId());
                } catch (RemoteException e) {
                }
                appOpsManager.resetAllModes();
            }
            PackageManager packageManager = getPackageManager();
            List<ApplicationInfo> installedApplications = packageManager.getInstalledApplications(512);
            int i2 = 0;
            while (i2 < installedApplications.size()) {
                ApplicationInfo applicationInfo = installedApplications.get(i2);
                try {
                    if (!isSystemPackage(packageManager, packageManager.getPackageInfo(applicationInfo.packageName, 64))) {
                        Log.d("DchaService", "setInitialSettingsTerminal 0003:" + applicationInfo.packageName);
                        iNotificationManagerAsInterface.setNotificationsEnabledWithImportanceLockForPackage(applicationInfo.packageName, applicationInfo.uid, true);
                    }
                } catch (Exception e2) {
                }
                if (!TextUtils.equals("com.mediatek.mtklogger", applicationInfo.packageName)) {
                    Log.d("DchaService", "setInitialSettingsTerminal 0004");
                    if (!applicationInfo.enabled && packageManager.getApplicationEnabledSetting(applicationInfo.packageName) == i) {
                        Log.d("DchaService", "setInitialSettingsTerminal 0005:" + applicationInfo.packageName);
                        packageManager.setApplicationEnabledSetting(applicationInfo.packageName, 0, 1);
                    }
                } else {
                    Log.d("DchaService", "setInitialSettingsTerminal 0006:" + applicationInfo.packageName);
                    packageManager.setApplicationEnabledSetting(applicationInfo.packageName, i, 0);
                }
                try {
                    PackageInfo packageInfo = packageManager.getPackageInfo(applicationInfo.packageName, 4096);
                    if (!isSystemPackage(packageManager, packageInfo)) {
                        int length = packageInfo.requestedPermissions.length;
                        for (int i3 = 0; i3 < length; i3++) {
                            String str = packageInfo.requestedPermissions[i3];
                            if ((applicationContext.getPackageManager().getPermissionFlags(str, packageInfo.packageName, Process.myUserHandle()) & 16) == 0 && "android.permission.READ_EXTERNAL_STORAGE".equals(str)) {
                                packageManager.grantRuntimePermission(packageInfo.packageName, str, Process.myUserHandle());
                            }
                        }
                    }
                } catch (Exception e3) {
                }
                i2++;
                i = 3;
            }
            AppOpsManager appOpsManager2 = (AppOpsManager) applicationContext.getSystemService("appops");
            for (ApplicationInfo applicationInfo2 : packageManager.getInstalledApplications(128)) {
                packageManager.updateIntentVerificationStatusAsUser(applicationInfo2.packageName, 4, UserHandle.myUserId());
                appOpsManager2.setMode(24, applicationInfo2.uid, applicationInfo2.packageName, 0);
                appOpsManager2.setMode(23, applicationInfo2.uid, applicationInfo2.packageName, 0);
            }
            NetworkPolicyManager networkPolicyManagerFrom = NetworkPolicyManager.from(applicationContext);
            int[] uidsWithPolicy = networkPolicyManagerFrom.getUidsWithPolicy(1);
            int currentUser = ActivityManager.getCurrentUser();
            for (int i4 : uidsWithPolicy) {
                if (UserHandle.getUserId(i4) == currentUser) {
                    networkPolicyManagerFrom.setUidPolicy(i4, 0);
                }
            }
            Settings.Secure.putInt(getContentResolver(), "lock_screen_show_notifications", 1);
            Settings.Secure.putInt(getContentResolver(), "lock_screen_allow_private_notifications", 1);
            Settings.Secure.putInt(getContentResolver(), "instant_apps_enabled", 0);
            Settings.Secure.putInt(getContentResolver(), "notification_badging", 0);
            Settings.System.putInt(getContentResolver(), "vibrate_when_ringing", 0);
            ((NotificationManager) applicationContext.getSystemService("notification")).setZenMode(0, null, "DchaService");
            Settings.Secure.putString(getContentResolver(), "assistant", "");
            Settings.Secure.putString(getContentResolver(), "voice_interaction_service", "");
            Settings.Secure.putString(getContentResolver(), "voice_recognition_service", "");
            RingtoneManager.setActualDefaultRingtoneUri(applicationContext, 1, null);
            RingtoneManager.setActualDefaultRingtoneUri(applicationContext, 2, null);
            RingtoneManager.setActualDefaultRingtoneUri(applicationContext, 4, null);
            AudioManager audioManager = (AudioManager) getSystemService("audio");
            audioManager.setStreamVolume(3, 5, 0);
            audioManager.adjustStreamVolume(3, 100, 0);
            audioManager.setStreamVolume(4, 12, 0);
            audioManager.adjustStreamVolume(4, 100, 0);
            audioManager.setStreamVolume(5, 8, 0);
            audioManager.adjustStreamVolume(5, 100, 0);
            audioManager.setStreamVolume(1, 8, 0);
            audioManager.adjustStreamVolume(1, 100, 0);
            audioManager.setStreamVolume(2, 8, 0);
            audioManager.adjustStreamVolume(2, 100, 0);
            audioManager.loadSoundEffects();
            Settings.System.putInt(getContentResolver(), "sound_effects_enabled", 1);
            Settings.System.putInt(getContentResolver(), "lockscreen_sounds_enabled", 0);
            UsbManager usbManager = (UsbManager) getSystemService("usb");
            usbManager.setCurrentFunctions(0L);
            usbManager.setScreenUnlockedFunctions(0L);
            ((PowerManager) getSystemService("power")).setPowerSaveMode(false);
            Settings.Global.putInt(getContentResolver(), "low_power_trigger_level", 0);
            Settings.Global.putInt(getContentResolver(), "adaptive_battery_management_enabled", 0);
            Settings.Global.putInt(getContentResolver(), "app_auto_restriction_enabled", 0);
            Settings.Secure.putInt(getContentResolver(), "night_display_activated", 0);
            WallpaperManager.getInstance(applicationContext).clearWallpaper();
            Log.d("DchaService", "setInitialSettingsTerminal 0008");
        } catch (RemoteException e4) {
            Log.e("DchaService", "setInitialSettingsTerminal 0007", e4);
            throw e4;
        }
    }

    protected void setInitialSettingsUser() {
        Log.d("DchaService", "setInitialSettingsUser start");
        Settings.Secure.putInt(getContentResolver(), "location_mode", 0);
        LockPatternUtils lockPatternUtils = new LockPatternUtils(this);
        int callingUserId = UserHandle.getCallingUserId();
        lockPatternUtils.resetKeyStore(callingUserId);
        lockPatternUtils.setLockScreenDisabled(true, callingUserId);
        Settings.System.putInt(getContentResolver(), "show_password", 1);
        Settings.Secure.putInt(getContentResolver(), "install_non_market_apps", 0);
        Settings.System.putInt(getContentResolver(), "lock_to_app_enabled", 0);
        Settings.Secure.putInt(getContentResolver(), "lock_to_app_exit_locked", 0);
        Log.d("DchaService", "setInitialSettingsUser 0002");
    }

    protected void setInitialSettingsAccount() {
        Log.d("DchaService", "setInitialSettingsAccount start");
        Context applicationContext = getApplicationContext();
        LocalePicker.updateLocale(new Locale("ja", "JP"));
        Settings.Secure.putInt(getContentResolver(), "show_ime_with_hard_keyboard", 0);
        Settings.Secure.putInt(getContentResolver(), "spell_checker_enabled", 0);
        Settings.Secure.putString(getContentResolver(), "autofill_service", "");
        ((InputManager) applicationContext.getSystemService("input")).setPointerSpeed(applicationContext, 0);
        ContentResolver.setMasterSyncAutomaticallyAsUser(false, Process.myUserHandle().getIdentifier());
        Log.d("DchaService", "setInitialSettingsAccount 0002");
    }

    protected void setInitialSettingsSystem() {
        Log.d("DchaService", "setInitialSettingsSystem start");
        Settings.Global.putInt(getContentResolver(), "auto_time", 1);
        ((AlarmManager) getSystemService("alarm")).setTimeZone("Asia/Tokyo");
        Settings.System.putString(getContentResolver(), "time_12_24", "12");
        Settings.System.putString(getContentResolver(), "date_format", "");
        Settings.Secure.putInt(getContentResolver(), "accessibility_captioning_enabled", 0);
        Settings.Secure.putInt(getContentResolver(), "accessibility_display_magnification_enabled", 0);
        Settings.Secure.putInt(getContentResolver(), "accessibility_display_magnification_navbar_enabled", 0);
        Settings.Secure.putInt(getContentResolver(), "high_text_contrast_enabled", 0);
        Settings.Secure.putInt(getContentResolver(), "speak_password", 0);
        Settings.Global.putInt(getContentResolver(), "enable_accessibility_global_gesture_enabled", 0);
        Settings.Secure.putInt(getContentResolver(), "long_press_timeout", 400);
        Settings.Secure.putInt(getContentResolver(), "accessibility_display_inversion_enabled", 0);
        Settings.Secure.putInt(getContentResolver(), "accessibility_display_daltonizer_enabled", 0);
        Log.d("DchaService", "setInitialSettingsSystem 0003");
    }

    protected void setInitialSettingsDevelopmentOptions() throws RemoteException, NumberFormatException {
        Log.d("DchaService", "setInitialSettingsDevelopmentOptions start");
        getApplicationContext();
        Settings.Global.putInt(getContentResolver(), "development_settings_enabled", 0);
        Settings.Global.putInt(getContentResolver(), "stay_on_while_plugged_in", 0);
        writeQsTileDisable();
        Settings.Global.putInt(getContentResolver(), "adb_enabled", 0);
        Settings.Global.putInt(getContentResolver(), "bugreport_in_power_menu", 0);
        AppOpsManager appOpsManager = (AppOpsManager) getSystemService("appops");
        boolean z = true;
        List<AppOpsManager.PackageOps> packagesForOps = appOpsManager.getPackagesForOps(new int[]{58});
        if (packagesForOps != null) {
            for (AppOpsManager.PackageOps packageOps : packagesForOps) {
                if (((AppOpsManager.OpEntry) packageOps.getOps().get(0)).getMode() != 2) {
                    String packageName = packageOps.getPackageName();
                    try {
                        appOpsManager.setMode(58, getPackageManager().getApplicationInfo(packageName, 512).uid, packageName, 2);
                    } catch (PackageManager.NameNotFoundException e) {
                    }
                }
            }
        }
        Settings.Global.putInt(getContentResolver(), "debug_view_attributes", 0);
        Settings.Global.putString(getContentResolver(), "debug_app", "");
        resetDebuggerOptions();
        Settings.Global.putInt(getContentResolver(), "wait_for_debugger", 0);
        Settings.Global.putInt(getContentResolver(), "verifier_verify_adb_installs", 0);
        Settings.System.putInt(getContentResolver(), "allow_screen_shot", 0);
        SystemProperties.set("persist.logd.size", "256K");
        Settings.System.putInt(getContentResolver(), "show_touches", 0);
        Settings.System.putInt(getContentResolver(), "pointer_location", 0);
        writeShowUpdatesOption();
        SystemProperties.set("debug.layout", "false");
        Settings.Global.putInt(getContentResolver(), "debug.force_rtl", 0);
        SystemProperties.set("debug.force_rtl", "0");
        writeAnimationScaleOption(0, null, null);
        writeAnimationScaleOption(1, null, null);
        writeAnimationScaleOption(2, null, null);
        Settings.Global.putString(getContentResolver(), "overlay_display_devices", null);
        SystemProperties.set("persist.sys.ui.hw", "false");
        SystemProperties.set("debug.hwui.show_dirty_regions", (String) null);
        SystemProperties.set("debug.hwui.show_layers_updates", (String) null);
        SystemProperties.set("debug.hwui.overdraw", "0");
        SystemProperties.set("debug.hwui.show_non_rect_clip", "0");
        SystemProperties.set("debug.egl.force_msaa", "false");
        writeDisableOverlaysOption();
        Settings.Secure.putInt(getContentResolver(), "accessibility_display_daltonizer_enabled", 0);
        Settings.Secure.putInt(getContentResolver(), "usb_audio_automatic_routing_disabled", 0);
        writeStrictModeVisualOptions();
        writeCpuUsageOptions();
        SystemProperties.set("debug.hwui.profile", "");
        SystemProperties.set("debug.egl.trace", "");
        writeImmediatelyDestroyActivitiesOptions();
        writeAppProcessLimitOptions(null);
        Settings.Secure.putInt(getContentResolver(), "anr_show_background", 0);
        Settings.Global.putInt(getContentResolver(), "show_notification_channel_warnings", 0);
        SystemProperties.set("persist.bluetooth.showdeviceswithoutnames", "false");
        Settings.Global.putInt(getContentResolver(), "development_settings_enabled", 0);
        if (Settings.Global.getInt(getContentResolver(), "development_settings_enabled", 0) == 0) {
            z = false;
        }
        if (z) {
            Log.d("DchaService", "setInitialSettingsDevelopmentOptions 0002");
            writeShowUpdatesOption();
            Settings.Global.putInt(getContentResolver(), "development_settings_enabled", 0);
        }
        Log.d("DchaService", "setInitialSettingsDevelopmentOptions 0003");
    }

    protected static boolean isSystemPackage(PackageManager packageManager, PackageInfo packageInfo) {
        if (sSystemSignature == null) {
            sSystemSignature = new Signature[]{getSystemSignature(packageManager)};
        }
        return sSystemSignature[0] != null && sSystemSignature[0].equals(getFirstSignature(packageInfo));
    }

    private static Signature getFirstSignature(PackageInfo packageInfo) {
        if (packageInfo != null && packageInfo.signatures != null && packageInfo.signatures.length > 0) {
            return packageInfo.signatures[0];
        }
        return null;
    }

    private static Signature getSystemSignature(PackageManager packageManager) {
        try {
            return getFirstSignature(packageManager.getPackageInfo("android", 64));
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private int getNewPriorityCategories(NotificationManager.Policy policy, boolean z, int i) {
        int i2 = policy.priorityCategories;
        if (z) {
            return i2 | i;
        }
        return i2 & (~i);
    }

    private NotificationManager.Policy savePolicy(NotificationManager.Policy policy, int i, int i2, int i3, int i4) {
        NotificationManager.Policy policy2 = new NotificationManager.Policy(i, i2, i3, i4);
        NotificationManager.from(getApplicationContext()).setNotificationPolicy(policy2);
        return policy2;
    }
}
