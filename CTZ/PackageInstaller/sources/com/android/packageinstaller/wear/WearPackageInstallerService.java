package com.android.packageinstaller.wear;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.FeatureInfo;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.support.v4.app.DialogFragment;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;
import com.android.packageinstaller.DeviceUtils;
import com.android.packageinstaller.PackageUtil;
import com.android.packageinstaller.R;
import com.android.packageinstaller.wear.PackageInstallerImpl;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class WearPackageInstallerService extends Service {
    private static volatile PowerManager.WakeLock lockStatic = null;
    private final int START_INSTALL = 1;
    private final int START_UNINSTALL = 2;
    private int mInstallNotificationId = 1;
    private final Map<String, Integer> mNotifIdMap = new ArrayMap();
    private NotificationChannel mNotificationChannel;
    private ServiceHandler mServiceHandler;

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) throws Throwable {
            switch (message.what) {
                case DialogFragment.STYLE_NO_TITLE:
                    WearPackageInstallerService.this.installPackage(message.getData());
                    break;
                case DialogFragment.STYLE_NO_FRAME:
                    WearPackageInstallerService.this.uninstallPackage(message.getData());
                    break;
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread handlerThread = new HandlerThread("PackageInstallerThread", 10);
        handlerThread.start();
        this.mServiceHandler = new ServiceHandler(handlerThread.getLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int i, int i2) {
        Message messageObtainMessage;
        String string;
        if (!DeviceUtils.isWear(this)) {
            Log.w("WearPkgInstallerService", "Not running on wearable.");
            finishServiceEarly(i2);
            return 2;
        }
        if (intent == null) {
            Log.w("WearPkgInstallerService", "Got null intent.");
            finishServiceEarly(i2);
            return 2;
        }
        if (Log.isLoggable("WearPkgInstallerService", 3)) {
            Log.d("WearPkgInstallerService", "Got install/uninstall request " + intent);
        }
        Uri data = intent.getData();
        if (data == null) {
            Log.e("WearPkgInstallerService", "No package URI in intent");
            finishServiceEarly(i2);
            return 2;
        }
        String sanitizedPackageName = WearPackageUtil.getSanitizedPackageName(data);
        if (sanitizedPackageName == null) {
            Log.e("WearPkgInstallerService", "Invalid package name in URI (expected package:<pkgName>): " + data);
            finishServiceEarly(i2);
            return 2;
        }
        PowerManager.WakeLock lock = getLock(getApplicationContext());
        if (!lock.isHeld()) {
            lock.acquire();
        }
        Bundle extras = intent.getExtras();
        if (extras == null) {
            extras = new Bundle();
        }
        WearPackageArgs.setStartId(extras, i2);
        WearPackageArgs.setPackageName(extras, sanitizedPackageName);
        if ("android.intent.action.INSTALL_PACKAGE".equals(intent.getAction())) {
            messageObtainMessage = this.mServiceHandler.obtainMessage(1);
            string = getString(R.string.installing);
        } else if ("android.intent.action.UNINSTALL_PACKAGE".equals(intent.getAction())) {
            messageObtainMessage = this.mServiceHandler.obtainMessage(2);
            string = getString(R.string.uninstalling);
        } else {
            Log.e("WearPkgInstallerService", "Unknown action : " + intent.getAction());
            finishServiceEarly(i2);
            return 2;
        }
        Pair<Integer, Notification> pairBuildNotification = buildNotification(sanitizedPackageName, string);
        startForeground(((Integer) pairBuildNotification.first).intValue(), (Notification) pairBuildNotification.second);
        messageObtainMessage.setData(extras);
        this.mServiceHandler.sendMessage(messageObtainMessage);
        return 2;
    }

    private void installPackage(Bundle bundle) throws Throwable {
        WearPackageInstallerService wearPackageInstallerService;
        File fileFromFd;
        boolean z;
        char c;
        PackageInfo packageInfo;
        Uri uri;
        int i;
        int i2;
        boolean z2;
        int startId = WearPackageArgs.getStartId(bundle);
        String packageName = WearPackageArgs.getPackageName(bundle);
        Uri assetUri = WearPackageArgs.getAssetUri(bundle);
        Uri permUri = WearPackageArgs.getPermUri(bundle);
        boolean zCheckPerms = WearPackageArgs.checkPerms(bundle);
        boolean zSkipIfSameVersion = WearPackageArgs.skipIfSameVersion(bundle);
        int companionSdkVersion = WearPackageArgs.getCompanionSdkVersion(bundle);
        int companionDeviceVersion = WearPackageArgs.getCompanionDeviceVersion(bundle);
        String compressionAlg = WearPackageArgs.getCompressionAlg(bundle);
        boolean zSkipIfLowerVersion = WearPackageArgs.skipIfLowerVersion(bundle);
        if (Log.isLoggable("WearPkgInstallerService", 3)) {
            Log.d("WearPkgInstallerService", "Installing package: " + packageName + ", assetUri: " + assetUri + ",permUri: " + permUri + ", startId: " + startId + ", checkPerms: " + zCheckPerms + ", skipIfSameVersion: " + zSkipIfSameVersion + ", compressionAlg: " + compressionAlg + ", companionSdkVersion: " + companionSdkVersion + ", companionDeviceVersion: " + companionDeviceVersion + ", skipIfLowerVersion: " + zSkipIfLowerVersion);
        }
        PackageManager packageManager = getPackageManager();
        PowerManager.WakeLock lock = getLock(getApplicationContext());
        boolean z3 = false;
        try {
            try {
                packageInfo = packageManager.getPackageInfo(packageName, 4198400);
                c = packageInfo != null ? (char) 2 : (char) 0;
            } catch (PackageManager.NameNotFoundException e) {
                c = 0;
                packageInfo = null;
            }
            if ((c & 2) == 0 || !Log.isLoggable("WearPkgInstallerService", 3)) {
                uri = permUri;
            } else {
                StringBuilder sb = new StringBuilder();
                uri = permUri;
                sb.append("Replacing package:");
                sb.append(packageName);
                Log.d("WearPkgInstallerService", sb.toString());
            }
            fileFromFd = WearPackageUtil.getFileFromFd(this, getContentResolver().openFileDescriptor(assetUri, "r"), packageName, compressionAlg);
            try {
            } catch (FileNotFoundException e2) {
                e = e2;
                wearPackageInstallerService = this;
            } catch (Throwable th) {
                th = th;
                wearPackageInstallerService = this;
            }
        } catch (FileNotFoundException e3) {
            e = e3;
            wearPackageInstallerService = this;
            z = false;
            fileFromFd = null;
            try {
                Log.e("WearPkgInstallerService", "Could not find the file with URI " + assetUri, e);
                if (z) {
                }
            } catch (Throwable th2) {
                th = th2;
                z3 = z;
            }
        } catch (Throwable th3) {
            th = th3;
            wearPackageInstallerService = this;
            fileFromFd = null;
        }
        if (fileFromFd == null) {
            Log.e("WearPkgInstallerService", "Could not create a temp file from FD for " + packageName);
            if (fileFromFd != null) {
                fileFromFd.delete();
            }
            finishService(lock, startId);
            return;
        }
        PackageParser.Package packageInfo2 = PackageUtil.getPackageInfo(this, fileFromFd);
        if (packageInfo2 == null) {
            Log.e("WearPkgInstallerService", "Could not parse apk information for " + packageName);
            if (fileFromFd != null) {
                fileFromFd.delete();
            }
            finishService(lock, startId);
            return;
        }
        if (!packageInfo2.packageName.equals(packageName)) {
            Log.e("WearPkgInstallerService", "Wearable Package Name has to match what is provided for " + packageName);
            if (fileFromFd != null) {
                fileFromFd.delete();
            }
            finishService(lock, startId);
            return;
        }
        packageInfo2.applicationInfo.sourceDir = fileFromFd.getPath();
        packageInfo2.applicationInfo.publicSourceDir = fileFromFd.getPath();
        getLabelAndUpdateNotification(packageName, getString(R.string.installing_app, new Object[]{packageInfo2.applicationInfo.loadLabel(packageManager)}));
        ArrayList arrayList = packageInfo2.requestedPermissions;
        if (packageInfo != null) {
            if (packageInfo.getLongVersionCode() != packageInfo2.getLongVersionCode()) {
                i = companionSdkVersion;
                i2 = companionDeviceVersion;
                if (packageInfo.getLongVersionCode() > packageInfo2.getLongVersionCode()) {
                    if (zSkipIfLowerVersion) {
                        Log.w("WearPkgInstallerService", "Version number of new app (" + packageInfo2.getLongVersionCode() + ") is lower than existing app ( " + packageInfo.getLongVersionCode() + ") for " + packageName + "; not installing due to versionCheck");
                        if (fileFromFd != null) {
                            fileFromFd.delete();
                        }
                        finishService(lock, startId);
                        return;
                    }
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("Version number of new app (");
                    try {
                        try {
                            sb2.append(packageInfo2.getLongVersionCode());
                            sb2.append(") is lower than existing app ( ");
                            sb2.append(packageInfo.getLongVersionCode());
                            sb2.append(") for ");
                            sb2.append(packageName);
                            Log.w("WearPkgInstallerService", sb2.toString());
                        } catch (FileNotFoundException e4) {
                            e = e4;
                            z = false;
                            wearPackageInstallerService = this;
                            Log.e("WearPkgInstallerService", "Could not find the file with URI " + assetUri, e);
                            if (z) {
                                if (fileFromFd != null) {
                                    fileFromFd.delete();
                                }
                                wearPackageInstallerService.finishService(lock, startId);
                                return;
                            }
                            return;
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        wearPackageInstallerService = this;
                    }
                }
            } else {
                if (zSkipIfSameVersion) {
                    Log.w("WearPkgInstallerService", "Version number (" + packageInfo2.getLongVersionCode() + ") of new app is equal to existing app for " + packageName + "; not installing due to versionCheck");
                    if (fileFromFd != null) {
                        fileFromFd.delete();
                    }
                    finishService(lock, startId);
                    return;
                }
                StringBuilder sb3 = new StringBuilder();
                sb3.append("Version number of new app (");
                i = companionSdkVersion;
                i2 = companionDeviceVersion;
                sb3.append(packageInfo2.getLongVersionCode());
                sb3.append(") is equal to existing app for ");
                sb3.append(packageName);
                Log.w("WearPkgInstallerService", sb3.toString());
            }
            if (packageInfo.requestedPermissions != null) {
                for (int i3 = 0; i3 < packageInfo.requestedPermissions.length; i3++) {
                    if ((packageInfo.requestedPermissionsFlags[i3] & 2) != 0) {
                        if (Log.isLoggable("WearPkgInstallerService", 3)) {
                            Log.d("WearPkgInstallerService", packageInfo.requestedPermissions[i3] + " is already granted for " + packageName);
                        }
                        arrayList.remove(packageInfo.requestedPermissions[i3]);
                    }
                }
            }
        } else {
            i = companionSdkVersion;
            i2 = companionDeviceVersion;
        }
        try {
            if (packageInfo2.reqFeatures != null) {
                z2 = true;
                for (FeatureInfo featureInfo : packageInfo2.reqFeatures) {
                    if (featureInfo.name != null && !packageManager.hasSystemFeature(featureInfo.name)) {
                        if ((featureInfo.flags & 1) != 0) {
                            Log.e("WearPkgInstallerService", "Wearable does not have required feature: " + featureInfo + " for " + packageName);
                            z2 = false;
                        }
                    }
                }
            } else {
                z2 = true;
            }
            if (!z2) {
                if (fileFromFd != null) {
                    fileFromFd.delete();
                }
                finishService(lock, startId);
                return;
            }
            wearPackageInstallerService = this;
            if (!zCheckPerms) {
                PackageInstallerFactory.getPackageInstaller(this).install(packageName, getContentResolver().openFileDescriptor(assetUri, "r"), new PackageInstallListener(wearPackageInstallerService, lock, startId, packageName));
                Log.i("WearPkgInstallerService", "Sent installation request for " + packageName);
                return;
            }
            try {
                if (!wearPackageInstallerService.checkPermissions(packageInfo2, i, i2, uri, arrayList, fileFromFd)) {
                    Log.w("WearPkgInstallerService", "Wearable does not have enough permissions.");
                    if (fileFromFd != null) {
                        fileFromFd.delete();
                    }
                    wearPackageInstallerService.finishService(lock, startId);
                    return;
                }
                PackageInstallerFactory.getPackageInstaller(this).install(packageName, getContentResolver().openFileDescriptor(assetUri, "r"), new PackageInstallListener(wearPackageInstallerService, lock, startId, packageName));
                try {
                    Log.i("WearPkgInstallerService", "Sent installation request for " + packageName);
                    return;
                } catch (FileNotFoundException e5) {
                    e = e5;
                    z = true;
                    Log.e("WearPkgInstallerService", "Could not find the file with URI " + assetUri, e);
                    if (z) {
                    }
                } catch (Throwable th5) {
                    th = th5;
                    z3 = true;
                }
            } catch (FileNotFoundException e6) {
                e = e6;
                z = false;
                Log.e("WearPkgInstallerService", "Could not find the file with URI " + assetUri, e);
                if (z) {
                }
            } catch (Throwable th6) {
                th = th6;
            }
            if (!z3) {
                if (fileFromFd != null) {
                    fileFromFd.delete();
                }
                wearPackageInstallerService.finishService(lock, startId);
            }
            throw th;
        } catch (FileNotFoundException e7) {
            e = e7;
            wearPackageInstallerService = this;
        }
    }

    private void uninstallPackage(Bundle bundle) {
        int startId = WearPackageArgs.getStartId(bundle);
        String packageName = WearPackageArgs.getPackageName(bundle);
        PowerManager.WakeLock lock = getLock(getApplicationContext());
        PackageManager packageManager = getPackageManager();
        try {
            getLabelAndUpdateNotification(packageName, getString(R.string.uninstalling_app, new Object[]{packageManager.getPackageInfo(packageName, 0).applicationInfo.loadLabel(packageManager)}));
            packageManager.deletePackage(packageName, new PackageDeleteObserver(lock, startId), 2);
            Log.i("WearPkgInstallerService", "Sent delete request for " + packageName);
        } catch (PackageManager.NameNotFoundException | IllegalArgumentException e) {
            Log.w("WearPkgInstallerService", "Could not find package, not deleting " + packageName, e);
            finishService(lock, startId);
        }
    }

    private boolean checkPermissions(PackageParser.Package r2, int i, int i2, Uri uri, List<String> list, File file) {
        if (r2.applicationInfo.targetSdkVersion >= 23 || !doesWearHaveUngrantedPerms(r2.packageName, uri, list)) {
            return true;
        }
        if (i == 0 || i >= 23) {
            Log.e("WearPkgInstallerService", "MNC: Wear app's targetSdkVersion should be at least 23, if phone app is targeting at least 23, will continue.");
            return false;
        }
        return false;
    }

    private boolean doesWearHaveUngrantedPerms(String str, Uri uri, List<String> list) {
        boolean z;
        if (uri == null) {
            Log.e("WearPkgInstallerService", "Permission URI is null");
            return true;
        }
        Cursor cursorQuery = getContentResolver().query(uri, null, null, null, null);
        if (cursorQuery == null) {
            Log.e("WearPkgInstallerService", "Could not get the cursor for the permissions");
            return true;
        }
        HashSet hashSet = new HashSet();
        HashSet hashSet2 = new HashSet();
        while (true) {
            z = false;
            if (!cursorQuery.moveToNext()) {
                break;
            }
            if (cursorQuery.getColumnCount() == 2 && 3 == cursorQuery.getType(0) && 1 == cursorQuery.getType(1)) {
                String string = cursorQuery.getString(0);
                if (Integer.valueOf(cursorQuery.getInt(1)).intValue() == 1) {
                    hashSet.add(string);
                } else {
                    hashSet2.add(string);
                }
            }
        }
        cursorQuery.close();
        for (String str2 : list) {
            if (!hashSet.contains(str2)) {
                if (!hashSet2.contains(str2)) {
                    Log.e("WearPkgInstallerService", "Wearable " + str + " has a permission \"" + str2 + "\" that is not defined in the host application's manifest.");
                } else {
                    Log.w("WearPkgInstallerService", "Wearable " + str + " has a permission \"" + str2 + "\" that is not granted in the host application.");
                }
                z = true;
            }
        }
        return z;
    }

    private void finishServiceEarly(int i) {
        Pair<Integer, Notification> pairBuildNotification = buildNotification(getApplicationContext().getPackageName(), "");
        startForeground(((Integer) pairBuildNotification.first).intValue(), (Notification) pairBuildNotification.second);
        finishService(null, i);
    }

    private void finishService(PowerManager.WakeLock wakeLock, int i) {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        stopSelf(i);
    }

    private synchronized PowerManager.WakeLock getLock(Context context) {
        if (lockStatic == null) {
            lockStatic = ((PowerManager) context.getSystemService("power")).newWakeLock(1, context.getClass().getSimpleName());
            lockStatic.setReferenceCounted(true);
        }
        return lockStatic;
    }

    private class PackageInstallListener implements PackageInstallerImpl.InstallListener {
        private String mApplicationPackageName;
        private Context mContext;
        private int mStartId;
        private PowerManager.WakeLock mWakeLock;

        private PackageInstallListener(Context context, PowerManager.WakeLock wakeLock, int i, String str) {
            this.mContext = context;
            this.mWakeLock = wakeLock;
            this.mStartId = i;
            this.mApplicationPackageName = str;
        }

        @Override
        public void installBeginning() {
            Log.i("WearPkgInstallerService", "Package " + this.mApplicationPackageName + " is being installed.");
        }

        @Override
        public void installSucceeded() {
            try {
                Log.i("WearPkgInstallerService", "Package " + this.mApplicationPackageName + " was installed.");
                File temporaryFile = WearPackageUtil.getTemporaryFile(this.mContext, this.mApplicationPackageName);
                if (temporaryFile != null) {
                    temporaryFile.delete();
                }
            } finally {
                WearPackageInstallerService.this.finishService(this.mWakeLock, this.mStartId);
            }
        }

        @Override
        public void installFailed(int i, String str) {
            Log.e("WearPkgInstallerService", "Package install failed " + this.mApplicationPackageName + ", errorCode " + i);
            WearPackageInstallerService.this.finishService(this.mWakeLock, this.mStartId);
        }
    }

    private class PackageDeleteObserver extends IPackageDeleteObserver.Stub {
        private int mStartId;
        private PowerManager.WakeLock mWakeLock;

        private PackageDeleteObserver(PowerManager.WakeLock wakeLock, int i) {
            this.mWakeLock = wakeLock;
            this.mStartId = i;
        }

        public void packageDeleted(String str, int i) {
            try {
                if (i >= 0) {
                    Log.i("WearPkgInstallerService", "Package " + str + " was uninstalled.");
                } else {
                    Log.e("WearPkgInstallerService", "Package uninstall failed " + str + ", returnCode " + i);
                }
                WearPackageInstallerService.this.finishService(this.mWakeLock, this.mStartId);
            } catch (Throwable th) {
                WearPackageInstallerService.this.finishService(this.mWakeLock, this.mStartId);
                throw th;
            }
        }
    }

    private synchronized Pair<Integer, Notification> buildNotification(String str, String str2) {
        int iIntValue;
        if (this.mNotifIdMap.containsKey(str)) {
            iIntValue = this.mNotifIdMap.get(str).intValue();
        } else {
            int i = this.mInstallNotificationId;
            this.mInstallNotificationId = i + 1;
            this.mNotifIdMap.put(str, Integer.valueOf(i));
            iIntValue = i;
        }
        if (this.mNotificationChannel == null) {
            this.mNotificationChannel = new NotificationChannel("wear_app_install_uninstall", getString(R.string.wear_app_channel), 1);
            ((NotificationManager) getSystemService(NotificationManager.class)).createNotificationChannel(this.mNotificationChannel);
        }
        return new Pair<>(Integer.valueOf(iIntValue), new Notification.Builder(this, "wear_app_install_uninstall").setSmallIcon(R.drawable.ic_file_download).setContentTitle(str2).build());
    }

    private void getLabelAndUpdateNotification(String str, String str2) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NotificationManager.class);
        Pair<Integer, Notification> pairBuildNotification = buildNotification(str, str2);
        notificationManager.notify(((Integer) pairBuildNotification.first).intValue(), (Notification) pairBuildNotification.second);
    }
}
