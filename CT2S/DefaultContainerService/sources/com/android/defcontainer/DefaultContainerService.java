package com.android.defcontainer;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.PackageCleanItem;
import android.content.pm.PackageInfoLite;
import android.content.pm.PackageParser;
import android.content.res.ObbInfo;
import android.content.res.ObbScanner;
import android.os.Environment;
import android.os.FileUtils;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.system.ErrnoException;
import android.system.Os;
import android.system.StructStatVfs;
import android.util.Slog;
import com.android.internal.app.IMediaContainerService;
import com.android.internal.content.NativeLibraryHelper;
import com.android.internal.content.PackageHelper;
import com.android.internal.os.IParcelFileDescriptorFactory;
import com.android.internal.util.ArrayUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import libcore.io.IoUtils;
import libcore.io.Streams;

public class DefaultContainerService extends IntentService {
    private IMediaContainerService.Stub mBinder;

    public DefaultContainerService() {
        super("DefaultContainerService");
        this.mBinder = new IMediaContainerService.Stub() {
            public String copyPackageToContainer(String packagePath, String containerId, String key, boolean isExternal, boolean isForwardLocked, String abiOverride) {
                Exception e;
                String strCopyPackageToContainerInner;
                if (packagePath == null || containerId == null) {
                    return null;
                }
                if (isExternal) {
                    String status = Environment.getExternalStorageState();
                    if (!status.equals("mounted")) {
                        Slog.w("DefContainer", "Make sure sdcard is mounted.");
                        return null;
                    }
                }
                NativeLibraryHelper.Handle handle = null;
                try {
                    try {
                        File packageFile = new File(packagePath);
                        PackageParser.PackageLite pkg = PackageParser.parsePackageLite(packageFile, 0);
                        handle = NativeLibraryHelper.Handle.create(pkg);
                        strCopyPackageToContainerInner = DefaultContainerService.this.copyPackageToContainerInner(pkg, handle, containerId, key, isExternal, isForwardLocked, abiOverride);
                    } finally {
                        IoUtils.closeQuietly(handle);
                    }
                } catch (IOException e2) {
                    e = e2;
                    Slog.w("DefContainer", "Failed to copy package at " + packagePath, e);
                    strCopyPackageToContainerInner = null;
                    IoUtils.closeQuietly(handle);
                } catch (PackageParser.PackageParserException e3) {
                    e = e3;
                    Slog.w("DefContainer", "Failed to copy package at " + packagePath, e);
                    strCopyPackageToContainerInner = null;
                    IoUtils.closeQuietly(handle);
                }
                return strCopyPackageToContainerInner;
            }

            public int copyPackage(String packagePath, IParcelFileDescriptorFactory target) {
                if (packagePath == null || target == null) {
                    return -3;
                }
                try {
                    File packageFile = new File(packagePath);
                    PackageParser.PackageLite pkg = PackageParser.parsePackageLite(packageFile, 0);
                    return DefaultContainerService.this.copyPackageInner(pkg, target);
                } catch (PackageParser.PackageParserException | RemoteException | IOException e) {
                    Slog.w("DefContainer", "Failed to copy package at " + packagePath + ": " + e);
                    return -4;
                }
            }

            public PackageInfoLite getMinimalPackageInfo(String packagePath, int flags, String abiOverride) {
                Context context = DefaultContainerService.this;
                boolean isForwardLocked = (flags & 1) != 0;
                PackageInfoLite ret = new PackageInfoLite();
                if (packagePath == null) {
                    Slog.i("DefContainer", "Invalid package file " + packagePath);
                    ret.recommendedInstallLocation = -2;
                } else {
                    File packageFile = new File(packagePath);
                    try {
                        PackageParser.PackageLite pkg = PackageParser.parsePackageLite(packageFile, 0);
                        long sizeBytes = PackageHelper.calculateInstalledSize(pkg, isForwardLocked, abiOverride);
                        ret.packageName = pkg.packageName;
                        ret.splitNames = pkg.splitNames;
                        ret.versionCode = pkg.versionCode;
                        ret.baseRevisionCode = pkg.baseRevisionCode;
                        ret.splitRevisionCodes = pkg.splitRevisionCodes;
                        ret.installLocation = pkg.installLocation;
                        ret.verifiers = pkg.verifiers;
                        ret.recommendedInstallLocation = PackageHelper.resolveInstallLocation(context, pkg.packageName, pkg.installLocation, sizeBytes, flags);
                        ret.multiArch = pkg.multiArch;
                    } catch (IOException | PackageParser.PackageParserException e) {
                        Slog.w("DefContainer", "Failed to parse package at " + packagePath + ": " + e);
                        if (!packageFile.exists()) {
                            ret.recommendedInstallLocation = -6;
                        } else {
                            ret.recommendedInstallLocation = -2;
                        }
                    }
                }
                return ret;
            }

            public ObbInfo getObbInfo(String filename) {
                try {
                    return ObbScanner.getObbInfo(filename);
                } catch (IOException e) {
                    Slog.d("DefContainer", "Couldn't get OBB info for " + filename);
                    return null;
                }
            }

            public long calculateDirectorySize(String path) throws RemoteException {
                Process.setThreadPriority(10);
                File dir = Environment.maybeTranslateEmulatedPathToInternal(new File(path));
                if (!dir.exists() || !dir.isDirectory()) {
                    return 0L;
                }
                String targetPath = dir.getAbsolutePath();
                return MeasurementUtils.measureDirectory(targetPath);
            }

            public long[] getFileSystemStats(String path) {
                Process.setThreadPriority(10);
                try {
                    StructStatVfs stat = Os.statvfs(path);
                    long totalSize = stat.f_blocks * stat.f_bsize;
                    long availSize = stat.f_bavail * stat.f_bsize;
                    return new long[]{totalSize, availSize};
                } catch (ErrnoException e) {
                    throw new IllegalStateException(e);
                }
            }

            public void clearDirectory(String path) throws RemoteException {
                Process.setThreadPriority(10);
                File directory = new File(path);
                if (directory.exists() && directory.isDirectory()) {
                    DefaultContainerService.this.eraseFiles(directory);
                }
            }

            public long calculateInstalledSize(String packagePath, boolean isForwardLocked, String abiOverride) throws RemoteException {
                File packageFile = new File(packagePath);
                try {
                    PackageParser.PackageLite pkg = PackageParser.parsePackageLite(packageFile, 0);
                    return PackageHelper.calculateInstalledSize(pkg, isForwardLocked, abiOverride);
                } catch (IOException | PackageParser.PackageParserException e) {
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
            IPackageManager pm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
            PackageCleanItem item = null;
            while (true) {
                try {
                    item = pm.nextPackageToClean(item);
                    if (item != null) {
                        Environment.UserEnvironment userEnv = new Environment.UserEnvironment(item.userId);
                        eraseFiles(userEnv.buildExternalStorageAppDataDirs(item.packageName));
                        eraseFiles(userEnv.buildExternalStorageAppMediaDirs(item.packageName));
                        if (item.andCode) {
                            eraseFiles(userEnv.buildExternalStorageAppObbDirs(item.packageName));
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

    void eraseFiles(File[] paths) {
        for (File path : paths) {
            eraseFiles(path);
        }
    }

    void eraseFiles(File path) {
        String[] files;
        if (path.isDirectory() && (files = path.list()) != null) {
            for (String file : files) {
                eraseFiles(new File(path, file));
            }
        }
        path.delete();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return this.mBinder;
    }

    private String copyPackageToContainerInner(PackageParser.PackageLite pkg, NativeLibraryHelper.Handle handle, String newCid, String key, boolean isExternal, boolean isForwardLocked, String abiOverride) throws IOException {
        long sizeBytes = PackageHelper.calculateInstalledSize(pkg, handle, isForwardLocked, abiOverride);
        String newMountPath = PackageHelper.createSdDir(sizeBytes, newCid, key, Process.myUid(), isExternal);
        if (newMountPath == null) {
            throw new IOException("Failed to create container " + newCid);
        }
        File targetDir = new File(newMountPath);
        try {
            copyFile(pkg.baseCodePath, targetDir, "base.apk", isForwardLocked);
            if (!ArrayUtils.isEmpty(pkg.splitNames)) {
                for (int i = 0; i < pkg.splitNames.length; i++) {
                    copyFile(pkg.splitCodePaths[i], targetDir, "split_" + pkg.splitNames[i] + ".apk", isForwardLocked);
                }
            }
            File libraryRoot = new File(targetDir, "lib");
            int res = NativeLibraryHelper.copyNativeBinariesWithOverride(handle, libraryRoot, abiOverride);
            if (res != 1) {
                throw new IOException("Failed to extract native code, res=" + res);
            }
            if (!PackageHelper.finalizeSdDir(newCid)) {
                throw new IOException("Failed to finalize " + newCid);
            }
            if (PackageHelper.isContainerMounted(newCid)) {
                PackageHelper.unMountSdDir(newCid);
            }
            return newMountPath;
        } catch (ErrnoException e) {
            PackageHelper.destroySdDir(newCid);
            throw e.rethrowAsIOException();
        } catch (IOException e2) {
            PackageHelper.destroySdDir(newCid);
            throw e2;
        }
    }

    private int copyPackageInner(PackageParser.PackageLite pkg, IParcelFileDescriptorFactory target) throws Throwable {
        copyFile(pkg.baseCodePath, target, "base.apk");
        if (!ArrayUtils.isEmpty(pkg.splitNames)) {
            for (int i = 0; i < pkg.splitNames.length; i++) {
                copyFile(pkg.splitCodePaths[i], target, "split_" + pkg.splitNames[i] + ".apk");
            }
            return 1;
        }
        return 1;
    }

    private void copyFile(String sourcePath, IParcelFileDescriptorFactory target, String targetName) throws Throwable {
        OutputStream out;
        Slog.d("DefContainer", "Copying " + sourcePath + " to " + targetName);
        InputStream in = null;
        OutputStream out2 = null;
        try {
            InputStream in2 = new FileInputStream(sourcePath);
            try {
                out = new ParcelFileDescriptor.AutoCloseOutputStream(target.open(targetName, 805306368));
            } catch (Throwable th) {
                th = th;
                in = in2;
            }
            try {
                Streams.copy(in2, out);
                IoUtils.closeQuietly(out);
                IoUtils.closeQuietly(in2);
            } catch (Throwable th2) {
                th = th2;
                out2 = out;
                in = in2;
                IoUtils.closeQuietly(out2);
                IoUtils.closeQuietly(in);
                throw th;
            }
        } catch (Throwable th3) {
            th = th3;
        }
    }

    private void copyFile(String sourcePath, File targetDir, String targetName, boolean isForwardLocked) throws IOException, ErrnoException {
        File sourceFile = new File(sourcePath);
        File targetFile = new File(targetDir, targetName);
        Slog.d("DefContainer", "Copying " + sourceFile + " to " + targetFile);
        if (!FileUtils.copyFile(sourceFile, targetFile)) {
            throw new IOException("Failed to copy " + sourceFile + " to " + targetFile);
        }
        if (isForwardLocked) {
            String publicTargetName = PackageHelper.replaceEnd(targetName, ".apk", ".zip");
            File publicTargetFile = new File(targetDir, publicTargetName);
            PackageHelper.extractPublicFiles(sourceFile, publicTargetFile);
            Os.chmod(targetFile.getAbsolutePath(), 416);
            Os.chmod(publicTargetFile.getAbsolutePath(), 420);
            return;
        }
        Os.chmod(targetFile.getAbsolutePath(), 420);
    }
}
