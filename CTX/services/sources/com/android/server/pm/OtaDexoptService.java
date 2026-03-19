package com.android.server.pm;

import android.content.Context;
import android.content.pm.IOtaDexopt;
import android.content.pm.PackageParser;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.storage.StorageManager;
import android.util.Log;
import android.util.Slog;
import com.android.internal.logging.MetricsLogger;
import com.android.server.pm.Installer;
import com.android.server.pm.PackageDexOptimizer;
import com.android.server.pm.dex.DexoptOptions;
import com.android.server.slice.SliceClientPermissions;
import java.io.File;
import java.io.FileDescriptor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class OtaDexoptService extends IOtaDexopt.Stub {
    private static final long BULK_DELETE_THRESHOLD = 1073741824;
    private static final boolean DEBUG_DEXOPT = true;
    private static final String[] NO_LIBRARIES = {PackageDexOptimizer.SKIP_SHARED_LIBRARY_CHECK};
    private static final String TAG = "OTADexopt";
    private long availableSpaceAfterBulkDelete;
    private long availableSpaceAfterDexopt;
    private long availableSpaceBefore;
    private int completeSize;
    private int dexoptCommandCountExecuted;
    private int dexoptCommandCountTotal;
    private int importantPackageCount;
    private final Context mContext;
    private List<String> mDexoptCommands;
    private final PackageManagerService mPackageManagerService;
    private long otaDexoptTimeStart;
    private int otherPackageCount;

    public OtaDexoptService(Context context, PackageManagerService packageManagerService) {
        this.mContext = context;
        this.mPackageManagerService = packageManagerService;
    }

    public static OtaDexoptService main(Context context, PackageManagerService packageManagerService) {
        ?? otaDexoptService = new OtaDexoptService(context, packageManagerService);
        ServiceManager.addService("otadexopt", (IBinder) otaDexoptService);
        otaDexoptService.moveAbArtifacts(packageManagerService.mInstaller);
        return otaDexoptService;
    }

    public void onShellCommand(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, FileDescriptor fileDescriptor3, String[] strArr, ShellCallback shellCallback, ResultReceiver resultReceiver) {
        new OtaDexoptShellCommand(this).exec(this, fileDescriptor, fileDescriptor2, fileDescriptor3, strArr, shellCallback, resultReceiver);
    }

    public synchronized void prepare() throws RemoteException {
        List<PackageParser.Package> packagesForDexopt;
        ArrayList<PackageParser.Package> arrayList;
        if (this.mDexoptCommands != null) {
            throw new IllegalStateException("already called prepare()");
        }
        synchronized (this.mPackageManagerService.mPackages) {
            packagesForDexopt = PackageManagerServiceUtils.getPackagesForDexopt(this.mPackageManagerService.mPackages.values(), this.mPackageManagerService);
            arrayList = new ArrayList(this.mPackageManagerService.mPackages.values());
            arrayList.removeAll(packagesForDexopt);
            this.mDexoptCommands = new ArrayList((3 * this.mPackageManagerService.mPackages.size()) / 2);
        }
        Iterator<PackageParser.Package> it = packagesForDexopt.iterator();
        while (it.hasNext()) {
            this.mDexoptCommands.addAll(generatePackageDexopts(it.next(), 4));
        }
        for (PackageParser.Package r3 : arrayList) {
            if (r3.coreApp) {
                throw new IllegalStateException("Found a core app that's not important");
            }
            this.mDexoptCommands.addAll(generatePackageDexopts(r3, 0));
        }
        this.completeSize = this.mDexoptCommands.size();
        long availableSpace = getAvailableSpace();
        if (availableSpace < BULK_DELETE_THRESHOLD) {
            Log.i(TAG, "Low on space, deleting oat files in an attempt to free up space: " + PackageManagerServiceUtils.packagesToString(arrayList));
            Iterator it2 = arrayList.iterator();
            while (it2.hasNext()) {
                this.mPackageManagerService.deleteOatArtifactsOfPackage(((PackageParser.Package) it2.next()).packageName);
            }
        }
        prepareMetricsLogging(packagesForDexopt.size(), arrayList.size(), availableSpace, getAvailableSpace());
    }

    public synchronized void cleanup() throws RemoteException {
        Log.i(TAG, "Cleaning up OTA Dexopt state.");
        this.mDexoptCommands = null;
        this.availableSpaceAfterDexopt = getAvailableSpace();
        performMetricsLogging();
    }

    public synchronized boolean isDone() throws RemoteException {
        if (this.mDexoptCommands == null) {
            throw new IllegalStateException("done() called before prepare()");
        }
        return this.mDexoptCommands.isEmpty();
    }

    public synchronized float getProgress() throws RemoteException {
        if (this.completeSize == 0) {
            return 1.0f;
        }
        return (this.completeSize - this.mDexoptCommands.size()) / this.completeSize;
    }

    public synchronized String nextDexoptCommand() throws RemoteException {
        if (this.mDexoptCommands == null) {
            throw new IllegalStateException("dexoptNextPackage() called before prepare()");
        }
        if (this.mDexoptCommands.isEmpty()) {
            return "(all done)";
        }
        String strRemove = this.mDexoptCommands.remove(0);
        if (getAvailableSpace() > 0) {
            this.dexoptCommandCountExecuted++;
            Log.d(TAG, "Next command: " + strRemove);
            return strRemove;
        }
        Log.w(TAG, "Not enough space for OTA dexopt, stopping with " + (this.mDexoptCommands.size() + 1) + " commands left.");
        this.mDexoptCommands.clear();
        return "(no free space)";
    }

    private long getMainLowSpaceThreshold() {
        long storageLowBytes = StorageManager.from(this.mContext).getStorageLowBytes(Environment.getDataDirectory());
        if (storageLowBytes == 0) {
            throw new IllegalStateException("Invalid low memory threshold");
        }
        return storageLowBytes;
    }

    private long getAvailableSpace() {
        return Environment.getDataDirectory().getUsableSpace() - getMainLowSpaceThreshold();
    }

    private synchronized List<String> generatePackageDexopts(PackageParser.Package r12, int i) {
        final ArrayList arrayList;
        arrayList = new ArrayList();
        OTADexoptPackageDexOptimizer oTADexoptPackageDexOptimizer = new OTADexoptPackageDexOptimizer(new Installer(this.mContext, true) {
            @Override
            public void dexopt(String str, int i2, String str2, String str3, int i3, String str4, int i4, String str5, String str6, String str7, String str8, boolean z, int i5, String str9, String str10, String str11) throws Installer.InstallerException {
                StringBuilder sb = new StringBuilder();
                sb.append("9 ");
                sb.append("dexopt");
                encodeParameter(sb, str);
                encodeParameter(sb, Integer.valueOf(i2));
                encodeParameter(sb, str2);
                encodeParameter(sb, str3);
                encodeParameter(sb, Integer.valueOf(i3));
                encodeParameter(sb, str4);
                encodeParameter(sb, Integer.valueOf(i4));
                encodeParameter(sb, str5);
                encodeParameter(sb, str6);
                encodeParameter(sb, str7);
                encodeParameter(sb, str8);
                encodeParameter(sb, Boolean.valueOf(z));
                encodeParameter(sb, Integer.valueOf(i5));
                encodeParameter(sb, str9);
                encodeParameter(sb, str10);
                encodeParameter(sb, str11);
                arrayList.add(sb.toString());
            }

            private void encodeParameter(StringBuilder sb, Object obj) {
                sb.append(' ');
                if (obj == null) {
                    sb.append('!');
                    return;
                }
                String strValueOf = String.valueOf(obj);
                if (strValueOf.indexOf(0) != -1 || strValueOf.indexOf(32) != -1 || "!".equals(strValueOf)) {
                    throw new IllegalArgumentException("Invalid argument while executing " + obj);
                }
                sb.append(strValueOf);
            }
        }, this.mPackageManagerService.mInstallLock, this.mContext);
        String[] strArr = r12.usesLibraryFiles;
        if (r12.isSystem()) {
            strArr = NO_LIBRARIES;
        }
        oTADexoptPackageDexOptimizer.performDexOpt(r12, strArr, null, null, this.mPackageManagerService.getDexManager().getPackageUseInfoOrDefault(r12.packageName), new DexoptOptions(r12.packageName, i, 4));
        return arrayList;
    }

    public synchronized void dexoptNextPackage() throws RemoteException {
        throw new UnsupportedOperationException();
    }

    private void moveAbArtifacts(Installer installer) {
        if (this.mDexoptCommands != null) {
            throw new IllegalStateException("Should not be ota-dexopting when trying to move.");
        }
        if (!this.mPackageManagerService.isUpgrade()) {
            Slog.d(TAG, "No upgrade, skipping A/B artifacts check.");
            return;
        }
        int i = 0;
        int i2 = 0;
        for (PackageParser.Package r4 : this.mPackageManagerService.getPackages()) {
            if (r4 != null && PackageDexOptimizer.canOptimizePackage(r4)) {
                if (r4.codePath == null) {
                    Slog.w(TAG, "Package " + r4 + " can be optimized but has null codePath");
                } else if (!r4.codePath.startsWith("/system") && !r4.codePath.startsWith("/vendor") && !r4.codePath.startsWith("/product")) {
                    String[] appDexInstructionSets = InstructionSets.getAppDexInstructionSets(r4.applicationInfo);
                    List allCodePathsExcludingResourceOnly = r4.getAllCodePathsExcludingResourceOnly();
                    int i3 = i;
                    for (String str : InstructionSets.getDexCodeInstructionSets(appDexInstructionSets)) {
                        Iterator it = allCodePathsExcludingResourceOnly.iterator();
                        while (it.hasNext()) {
                            i2++;
                            try {
                                installer.moveAb((String) it.next(), str, PackageDexOptimizer.getOatDir(new File(r4.codePath)).getAbsolutePath());
                                i3++;
                            } catch (Installer.InstallerException e) {
                            }
                        }
                    }
                    i = i3;
                }
            }
        }
        Slog.i(TAG, "Moved " + i + SliceClientPermissions.SliceAuthority.DELIMITER + i2);
    }

    private void prepareMetricsLogging(int i, int i2, long j, long j2) {
        this.availableSpaceBefore = j;
        this.availableSpaceAfterBulkDelete = j2;
        this.availableSpaceAfterDexopt = 0L;
        this.importantPackageCount = i;
        this.otherPackageCount = i2;
        this.dexoptCommandCountTotal = this.mDexoptCommands.size();
        this.dexoptCommandCountExecuted = 0;
        this.otaDexoptTimeStart = System.nanoTime();
    }

    private static int inMegabytes(long j) {
        long j2 = j / 1048576;
        if (j2 > 2147483647L) {
            Log.w(TAG, "Recording " + j2 + "MB of free space, overflowing range");
            return Integer.MAX_VALUE;
        }
        return (int) j2;
    }

    private void performMetricsLogging() {
        long jNanoTime = System.nanoTime();
        MetricsLogger.histogram(this.mContext, "ota_dexopt_available_space_before_mb", inMegabytes(this.availableSpaceBefore));
        MetricsLogger.histogram(this.mContext, "ota_dexopt_available_space_after_bulk_delete_mb", inMegabytes(this.availableSpaceAfterBulkDelete));
        MetricsLogger.histogram(this.mContext, "ota_dexopt_available_space_after_dexopt_mb", inMegabytes(this.availableSpaceAfterDexopt));
        MetricsLogger.histogram(this.mContext, "ota_dexopt_num_important_packages", this.importantPackageCount);
        MetricsLogger.histogram(this.mContext, "ota_dexopt_num_other_packages", this.otherPackageCount);
        MetricsLogger.histogram(this.mContext, "ota_dexopt_num_commands", this.dexoptCommandCountTotal);
        MetricsLogger.histogram(this.mContext, "ota_dexopt_num_commands_executed", this.dexoptCommandCountExecuted);
        MetricsLogger.histogram(this.mContext, "ota_dexopt_time_s", (int) TimeUnit.NANOSECONDS.toSeconds(jNanoTime - this.otaDexoptTimeStart));
    }

    private static class OTADexoptPackageDexOptimizer extends PackageDexOptimizer.ForcedUpdatePackageDexOptimizer {
        public OTADexoptPackageDexOptimizer(Installer installer, Object obj, Context context) {
            super(installer, obj, context, "*otadexopt*");
        }
    }
}
