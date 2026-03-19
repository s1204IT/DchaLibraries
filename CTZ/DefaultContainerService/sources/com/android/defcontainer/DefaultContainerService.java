package com.android.defcontainer;

import android.app.IntentService;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.PackageCleanItem;
import android.content.pm.PackageInfoLite;
import android.content.pm.PackageParser;
import android.content.res.ObbInfo;
import android.content.res.ObbScanner;
import android.os.Binder;
import android.os.Environment;
import android.os.FileUtils;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Slog;
import com.android.internal.app.IMediaContainerService;
import com.android.internal.content.PackageHelper;
import com.android.internal.os.IParcelFileDescriptorFactory;
import com.android.internal.util.ArrayUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import libcore.io.IoUtils;

public class DefaultContainerService extends IntentService {
    private IMediaContainerService.Stub mBinder;

    public DefaultContainerService() {
        super("DefaultContainerService");
        this.mBinder = new IMediaContainerService.Stub() {
            public int copyPackage(String str, IParcelFileDescriptorFactory iParcelFileDescriptorFactory) {
                if (str == null || iParcelFileDescriptorFactory == null) {
                    return -3;
                }
                try {
                    return DefaultContainerService.this.copyPackageInner(PackageParser.parsePackageLite(new File(str), 0), iParcelFileDescriptorFactory);
                } catch (PackageParser.PackageParserException | RemoteException | IOException e) {
                    Slog.w("DefContainer", "Failed to copy package at " + str + ": " + e);
                    return -4;
                }
            }

            public PackageInfoLite getMinimalPackageInfo(String str, int i, String str2) {
                DefaultContainerService defaultContainerService = DefaultContainerService.this;
                PackageInfoLite packageInfoLite = new PackageInfoLite();
                if (str == null) {
                    Slog.i("DefContainer", "Invalid package file " + str);
                    packageInfoLite.recommendedInstallLocation = -2;
                    return packageInfoLite;
                }
                File file = new File(str);
                try {
                    PackageParser.PackageLite packageLite = PackageParser.parsePackageLite(file, 0);
                    long jCalculateInstalledSize = PackageHelper.calculateInstalledSize(packageLite, str2);
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        int iResolveInstallLocation = PackageHelper.resolveInstallLocation(defaultContainerService, packageLite.packageName, packageLite.installLocation, jCalculateInstalledSize, i);
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                        packageInfoLite.packageName = packageLite.packageName;
                        packageInfoLite.splitNames = packageLite.splitNames;
                        packageInfoLite.versionCode = packageLite.versionCode;
                        packageInfoLite.versionCodeMajor = packageLite.versionCodeMajor;
                        packageInfoLite.baseRevisionCode = packageLite.baseRevisionCode;
                        packageInfoLite.splitRevisionCodes = packageLite.splitRevisionCodes;
                        packageInfoLite.installLocation = packageLite.installLocation;
                        packageInfoLite.verifiers = packageLite.verifiers;
                        packageInfoLite.recommendedInstallLocation = iResolveInstallLocation;
                        packageInfoLite.multiArch = packageLite.multiArch;
                        return packageInfoLite;
                    } catch (Throwable th) {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                        throw th;
                    }
                } catch (PackageParser.PackageParserException | IOException e) {
                    Slog.w("DefContainer", "Failed to parse package at " + str + ": " + e);
                    if (!file.exists()) {
                        packageInfoLite.recommendedInstallLocation = -6;
                    } else {
                        packageInfoLite.recommendedInstallLocation = -2;
                    }
                    return packageInfoLite;
                }
            }

            public ObbInfo getObbInfo(String str) {
                try {
                    return ObbScanner.getObbInfo(str);
                } catch (IOException e) {
                    Slog.d("DefContainer", "Couldn't get OBB info for " + str);
                    return null;
                }
            }

            public void clearDirectory(String str) throws RemoteException {
                Process.setThreadPriority(10);
                File file = new File(str);
                if (file.exists() && file.isDirectory()) {
                    DefaultContainerService.this.eraseFiles(file);
                }
            }

            public long calculateInstalledSize(String str, String str2) throws RemoteException {
                try {
                    return PackageHelper.calculateInstalledSize(PackageParser.parsePackageLite(new File(str), 0), str2);
                } catch (PackageParser.PackageParserException | IOException e) {
                    Slog.w("DefContainer", "Failed to calculate installed size: " + e);
                    return Long.MAX_VALUE;
                }
            }
        };
        setIntentRedelivery(true);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if ("android.content.pm.CLEAN_EXTERNAL_STORAGE".equals(intent.getAction())) {
            IPackageManager iPackageManagerAsInterface = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
            PackageCleanItem packageCleanItemNextPackageToClean = null;
            while (true) {
                try {
                    packageCleanItemNextPackageToClean = iPackageManagerAsInterface.nextPackageToClean(packageCleanItemNextPackageToClean);
                    if (packageCleanItemNextPackageToClean != null) {
                        Environment.UserEnvironment userEnvironment = new Environment.UserEnvironment(packageCleanItemNextPackageToClean.userId);
                        eraseFiles(userEnvironment.buildExternalStorageAppDataDirs(packageCleanItemNextPackageToClean.packageName));
                        eraseFiles(userEnvironment.buildExternalStorageAppMediaDirs(packageCleanItemNextPackageToClean.packageName));
                        if (packageCleanItemNextPackageToClean.andCode) {
                            eraseFiles(userEnvironment.buildExternalStorageAppObbDirs(packageCleanItemNextPackageToClean.packageName));
                        }
                    } else {
                        return;
                    }
                } catch (RemoteException e) {
                    return;
                }
            }
        }
    }

    void eraseFiles(File[] fileArr) {
        for (File file : fileArr) {
            eraseFiles(file);
        }
    }

    void eraseFiles(File file) {
        String[] list;
        if (file.isDirectory() && (list = file.list()) != null) {
            for (String str : list) {
                eraseFiles(new File(file, str));
            }
        }
        file.delete();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return this.mBinder;
    }

    private int copyPackageInner(PackageParser.PackageLite packageLite, IParcelFileDescriptorFactory iParcelFileDescriptorFactory) throws Throwable {
        copyFile(packageLite.baseCodePath, iParcelFileDescriptorFactory, "base.apk");
        if (!ArrayUtils.isEmpty(packageLite.splitNames)) {
            for (int i = 0; i < packageLite.splitNames.length; i++) {
                copyFile(packageLite.splitCodePaths[i], iParcelFileDescriptorFactory, "split_" + packageLite.splitNames[i] + ".apk");
            }
            return 1;
        }
        return 1;
    }

    private void copyFile(String str, IParcelFileDescriptorFactory iParcelFileDescriptorFactory, String str2) throws Throwable {
        FileInputStream fileInputStream;
        Slog.d("DefContainer", "Copying " + str + " to " + str2);
        ParcelFileDescriptor.AutoCloseOutputStream autoCloseOutputStream = null;
        try {
            fileInputStream = new FileInputStream(str);
            try {
                ParcelFileDescriptor.AutoCloseOutputStream autoCloseOutputStream2 = new ParcelFileDescriptor.AutoCloseOutputStream(iParcelFileDescriptorFactory.open(str2, 805306368));
                try {
                    FileUtils.copy(fileInputStream, autoCloseOutputStream2);
                    IoUtils.closeQuietly(autoCloseOutputStream2);
                    IoUtils.closeQuietly(fileInputStream);
                } catch (Throwable th) {
                    th = th;
                    autoCloseOutputStream = autoCloseOutputStream2;
                    IoUtils.closeQuietly(autoCloseOutputStream);
                    IoUtils.closeQuietly(fileInputStream);
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (Throwable th3) {
            th = th3;
            fileInputStream = null;
        }
    }
}
