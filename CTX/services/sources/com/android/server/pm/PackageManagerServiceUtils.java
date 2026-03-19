package com.android.server.pm;

import android.app.AppGlobals;
import android.content.Intent;
import android.content.pm.PackageParser;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.hardware.biometrics.fingerprint.V2_1.RequestStatus;
import android.os.Build;
import android.os.Debug;
import android.os.Environment;
import android.os.FileUtils;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.system.ErrnoException;
import android.system.Os;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.internal.util.FastPrintWriter;
import com.android.server.EventLogTags;
import com.android.server.pm.dex.DexManager;
import com.android.server.pm.dex.PackageDexUsage;
import dalvik.system.VMRuntime;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.zip.GZIPInputStream;

public class PackageManagerServiceUtils {
    private static final long SEVEN_DAYS_IN_MILLISECONDS = 604800000;

    private static ArraySet<String> getPackageNamesForIntent(Intent intent, int i) {
        List list;
        try {
            list = AppGlobals.getPackageManager().queryIntentReceivers(intent, (String) null, 0, i).getList();
        } catch (RemoteException e) {
            list = null;
        }
        ArraySet<String> arraySet = new ArraySet<>();
        if (list != null) {
            Iterator it = list.iterator();
            while (it.hasNext()) {
                arraySet.add(((ResolveInfo) it.next()).activityInfo.packageName);
            }
        }
        return arraySet;
    }

    public static void sortPackagesByUsageDate(List<PackageParser.Package> list, PackageManagerService packageManagerService) {
        if (!packageManagerService.isHistoricalPackageUsageAvailable()) {
            return;
        }
        Collections.sort(list, new Comparator() {
            @Override
            public final int compare(Object obj, Object obj2) {
                return Long.compare(((PackageParser.Package) obj2).getLatestForegroundPackageUseTimeInMills(), ((PackageParser.Package) obj).getLatestForegroundPackageUseTimeInMills());
            }
        });
    }

    private static void applyPackageFilter(Predicate<PackageParser.Package> predicate, Collection<PackageParser.Package> collection, Collection<PackageParser.Package> collection2, List<PackageParser.Package> list, PackageManagerService packageManagerService) {
        for (PackageParser.Package r1 : collection2) {
            if (predicate.test(r1)) {
                list.add(r1);
            }
        }
        sortPackagesByUsageDate(list, packageManagerService);
        collection2.removeAll(list);
        for (PackageParser.Package r0 : list) {
            collection.add(r0);
            List<PackageParser.Package> listFindSharedNonSystemLibraries = packageManagerService.findSharedNonSystemLibraries(r0);
            if (!listFindSharedNonSystemLibraries.isEmpty()) {
                listFindSharedNonSystemLibraries.removeAll(collection);
                collection.addAll(listFindSharedNonSystemLibraries);
                collection2.removeAll(listFindSharedNonSystemLibraries);
            }
        }
        list.clear();
    }

    public static List<PackageParser.Package> getPackagesForDexopt(Collection<PackageParser.Package> collection, PackageManagerService packageManagerService) {
        Predicate predicate;
        ArrayList arrayList = new ArrayList(collection);
        LinkedList linkedList = new LinkedList();
        ArrayList arrayList2 = new ArrayList(arrayList.size());
        applyPackageFilter(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return ((PackageParser.Package) obj).coreApp;
            }
        }, linkedList, arrayList, arrayList2, packageManagerService);
        final ArraySet<String> packageNamesForIntent = getPackageNamesForIntent(new Intent("android.intent.action.PRE_BOOT_COMPLETED"), 0);
        applyPackageFilter(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return packageNamesForIntent.contains(((PackageParser.Package) obj).packageName);
            }
        }, linkedList, arrayList, arrayList2, packageManagerService);
        final DexManager dexManager = packageManagerService.getDexManager();
        applyPackageFilter(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return dexManager.getPackageUseInfoOrDefault(((PackageParser.Package) obj).packageName).isAnyCodePathUsedByOtherApps();
            }
        }, linkedList, arrayList, arrayList2, packageManagerService);
        if (!arrayList.isEmpty() && packageManagerService.isHistoricalPackageUsageAvailable()) {
            if (PackageManagerService.DEBUG_DEXOPT) {
                Log.i("PackageManager", "Looking at historical package use");
            }
            PackageParser.Package r2 = (PackageParser.Package) Collections.max(arrayList, new Comparator() {
                @Override
                public final int compare(Object obj, Object obj2) {
                    return Long.compare(((PackageParser.Package) obj).getLatestForegroundPackageUseTimeInMills(), ((PackageParser.Package) obj2).getLatestForegroundPackageUseTimeInMills());
                }
            });
            if (PackageManagerService.DEBUG_DEXOPT) {
                Log.i("PackageManager", "Taking package " + r2.packageName + " as reference in time use");
            }
            long latestForegroundPackageUseTimeInMills = r2.getLatestForegroundPackageUseTimeInMills();
            if (latestForegroundPackageUseTimeInMills != 0) {
                final long j = latestForegroundPackageUseTimeInMills - 604800000;
                predicate = new Predicate() {
                    @Override
                    public final boolean test(Object obj) {
                        return PackageManagerServiceUtils.lambda$getPackagesForDexopt$5(j, (PackageParser.Package) obj);
                    }
                };
            } else {
                predicate = new Predicate() {
                    @Override
                    public final boolean test(Object obj) {
                        return PackageManagerServiceUtils.lambda$getPackagesForDexopt$6((PackageParser.Package) obj);
                    }
                };
            }
            sortPackagesByUsageDate(arrayList, packageManagerService);
        } else {
            predicate = new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return PackageManagerServiceUtils.lambda$getPackagesForDexopt$7((PackageParser.Package) obj);
                }
            };
        }
        applyPackageFilter(predicate, linkedList, arrayList, arrayList2, packageManagerService);
        if (PackageManagerService.DEBUG_DEXOPT) {
            Log.i("PackageManager", "Packages to be dexopted: " + packagesToString(linkedList));
            Log.i("PackageManager", "Packages skipped from dexopt: " + packagesToString(arrayList));
        }
        return linkedList;
    }

    static boolean lambda$getPackagesForDexopt$5(long j, PackageParser.Package r4) {
        return r4.getLatestForegroundPackageUseTimeInMills() >= j;
    }

    static boolean lambda$getPackagesForDexopt$6(PackageParser.Package r0) {
        return true;
    }

    static boolean lambda$getPackagesForDexopt$7(PackageParser.Package r0) {
        return true;
    }

    public static boolean isUnusedSinceTimeInMillis(long j, long j2, long j3, PackageDexUsage.PackageUseInfo packageUseInfo, long j4, long j5) {
        boolean z = false;
        if (j2 - j < j3) {
            return false;
        }
        if (j2 - j5 < j3) {
            return false;
        }
        if (j2 - j4 < j3 && packageUseInfo.isAnyCodePathUsedByOtherApps()) {
            z = true;
        }
        return !z;
    }

    public static String realpath(File file) throws IOException {
        try {
            return Os.realpath(file.getAbsolutePath());
        } catch (ErrnoException e) {
            throw e.rethrowAsIOException();
        }
    }

    public static String packagesToString(Collection<PackageParser.Package> collection) {
        StringBuilder sb = new StringBuilder();
        for (PackageParser.Package r1 : collection) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(r1.packageName);
        }
        return sb.toString();
    }

    public static boolean checkISA(String str) {
        for (String str2 : Build.SUPPORTED_ABIS) {
            if (VMRuntime.getInstructionSet(str2).equals(str)) {
                return true;
            }
        }
        return false;
    }

    public static long getLastModifiedTime(PackageParser.Package r5) {
        File file = new File(r5.codePath);
        if (!file.isDirectory()) {
            return file.lastModified();
        }
        long jLastModified = new File(r5.baseCodePath).lastModified();
        if (r5.splitCodePaths != null) {
            for (int length = r5.splitCodePaths.length - 1; length >= 0; length--) {
                jLastModified = Math.max(jLastModified, new File(r5.splitCodePaths[length]).lastModified());
            }
        }
        return jLastModified;
    }

    private static File getSettingsProblemFile() {
        return new File(new File(Environment.getDataDirectory(), "system"), "uiderrors.txt");
    }

    public static void dumpCriticalInfo(ProtoOutputStream protoOutputStream) throws Exception {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(getSettingsProblemFile()));
            Throwable th = null;
            while (true) {
                try {
                    try {
                        String line = bufferedReader.readLine();
                        if (line != null) {
                            if (!line.contains("ignored: updated version")) {
                                protoOutputStream.write(2237677961223L, line);
                            }
                        } else {
                            return;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        throw th;
                    }
                } finally {
                    $closeResource(th, bufferedReader);
                }
            }
        } catch (IOException e) {
        }
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    public static void dumpCriticalInfo(PrintWriter printWriter, String str) throws Exception {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(getSettingsProblemFile()));
            Throwable th = null;
            while (true) {
                try {
                    try {
                        String line = bufferedReader.readLine();
                        if (line != null) {
                            if (!line.contains("ignored: updated version")) {
                                if (str != null) {
                                    printWriter.print(str);
                                }
                                printWriter.println(line);
                            }
                        } else {
                            return;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        throw th;
                    }
                } finally {
                    $closeResource(th, bufferedReader);
                }
            }
        } catch (IOException e) {
        }
    }

    public static void logCriticalInfo(int i, String str) {
        Slog.println(i, "PackageManager", str);
        EventLogTags.writePmCriticalInfo(str);
        try {
            File settingsProblemFile = getSettingsProblemFile();
            FastPrintWriter fastPrintWriter = new FastPrintWriter(new FileOutputStream(settingsProblemFile, true));
            fastPrintWriter.println(new SimpleDateFormat().format(new Date(System.currentTimeMillis())) + ": " + str);
            fastPrintWriter.close();
            FileUtils.setPermissions(settingsProblemFile.toString(), 508, -1, -1);
        } catch (IOException e) {
        }
    }

    public static void enforceShellRestriction(String str, int i, int i2) {
        if (i == 2000) {
            if (i2 >= 0 && PackageManagerService.sUserManager.hasUserRestriction(str, i2)) {
                throw new SecurityException("Shell does not have permission to access user " + i2);
            }
            if (i2 < 0) {
                Slog.e("PackageManager", "Unable to check shell permission for user " + i2 + "\n\t" + Debug.getCallers(3));
            }
        }
    }

    public static String deriveAbiOverride(String str, PackageSetting packageSetting) {
        if (!"-".equals(str)) {
            if (str == null) {
                if (packageSetting != null) {
                    return packageSetting.cpuAbiOverrideString;
                }
            } else {
                return str;
            }
        }
        return null;
    }

    public static int compareSignatures(Signature[] signatureArr, Signature[] signatureArr2) {
        if (signatureArr == null) {
            if (signatureArr2 == null) {
                return 1;
            }
            return -1;
        }
        if (signatureArr2 == null) {
            return -2;
        }
        if (signatureArr.length != signatureArr2.length) {
            return -3;
        }
        if (signatureArr.length == 1) {
            return signatureArr[0].equals(signatureArr2[0]) ? 0 : -3;
        }
        ArraySet arraySet = new ArraySet();
        for (Signature signature : signatureArr) {
            arraySet.add(signature);
        }
        ArraySet arraySet2 = new ArraySet();
        for (Signature signature2 : signatureArr2) {
            arraySet2.add(signature2);
        }
        return arraySet.equals(arraySet2) ? 0 : -3;
    }

    private static boolean matchSignaturesCompat(String str, PackageSignatures packageSignatures, PackageParser.SigningDetails signingDetails) {
        ArraySet arraySet = new ArraySet();
        for (Signature signature : packageSignatures.mSigningDetails.signatures) {
            arraySet.add(signature);
        }
        ArraySet arraySet2 = new ArraySet();
        for (Signature signature2 : signingDetails.signatures) {
            try {
                for (Signature signature3 : signature2.getChainSignatures()) {
                    arraySet2.add(signature3);
                }
            } catch (CertificateEncodingException e) {
                arraySet2.add(signature2);
            }
        }
        if (arraySet2.equals(arraySet)) {
            packageSignatures.mSigningDetails = signingDetails;
            return true;
        }
        if (signingDetails.hasPastSigningCertificates()) {
            logCriticalInfo(4, "Existing package " + str + " has flattened signing certificate chain. Unable to install newer version with rotated signing certificate.");
        }
        return false;
    }

    private static boolean matchSignaturesRecover(String str, PackageParser.SigningDetails signingDetails, PackageParser.SigningDetails signingDetails2, @PackageParser.SigningDetails.CertCapabilities int i) {
        String message;
        try {
            if (signingDetails2.checkCapabilityRecover(signingDetails, i)) {
                logCriticalInfo(4, "Recovered effectively matching certificates for " + str);
                return true;
            }
            message = null;
        } catch (CertificateException e) {
            message = e.getMessage();
        }
        logCriticalInfo(4, "Failed to recover certificates for " + str + ": " + message);
        return false;
    }

    private static boolean matchSignatureInSystem(PackageSetting packageSetting, PackageSetting packageSetting2) {
        try {
            PackageParser.collectCertificates(packageSetting2.pkg, true);
            if (!packageSetting.signatures.mSigningDetails.checkCapability(packageSetting2.signatures.mSigningDetails, 1) && !packageSetting2.signatures.mSigningDetails.checkCapability(packageSetting.signatures.mSigningDetails, 8)) {
                logCriticalInfo(6, "Updated system app mismatches cert on /system: " + packageSetting.name);
                return false;
            }
            return true;
        } catch (PackageParser.PackageParserException e) {
            logCriticalInfo(6, "Failed to collect cert for " + packageSetting.name + ": " + e.getMessage());
            return false;
        }
    }

    static boolean isApkVerityEnabled() {
        return SystemProperties.getInt("ro.apk_verity.mode", 0) != 0;
    }

    static boolean isApkVerificationForced(PackageSetting packageSetting) {
        return packageSetting != null && packageSetting.isPrivileged() && isApkVerityEnabled();
    }

    public static boolean verifySignatures(PackageSetting packageSetting, PackageSetting packageSetting2, PackageParser.SigningDetails signingDetails, boolean z, boolean z2) throws PackageManagerException {
        boolean z3;
        String str = packageSetting.name;
        if (packageSetting.signatures.mSigningDetails.signatures != null) {
            boolean zMatchSignatureInSystem = signingDetails.checkCapability(packageSetting.signatures.mSigningDetails, 1) || packageSetting.signatures.mSigningDetails.checkCapability(signingDetails, 8);
            if (zMatchSignatureInSystem || !z) {
                z3 = false;
            } else {
                zMatchSignatureInSystem = matchSignaturesCompat(str, packageSetting.signatures, signingDetails);
                z3 = zMatchSignatureInSystem;
            }
            if (!zMatchSignatureInSystem && z2) {
                zMatchSignatureInSystem = matchSignaturesRecover(str, packageSetting.signatures.mSigningDetails, signingDetails, 1) || matchSignaturesRecover(str, signingDetails, packageSetting.signatures.mSigningDetails, 8);
            }
            if (!zMatchSignatureInSystem && isApkVerificationForced(packageSetting2)) {
                zMatchSignatureInSystem = matchSignatureInSystem(packageSetting, packageSetting2);
            }
            if (!zMatchSignatureInSystem) {
                throw new PackageManagerException(-7, "Package " + str + " signatures do not match previously installed version; ignoring!");
            }
        } else {
            z3 = false;
        }
        if (packageSetting.sharedUser != null && packageSetting.sharedUser.signatures.mSigningDetails != PackageParser.SigningDetails.UNKNOWN) {
            boolean zMatchSignaturesCompat = signingDetails.checkCapability(packageSetting.sharedUser.signatures.mSigningDetails, 2) || packageSetting.sharedUser.signatures.mSigningDetails.checkCapability(signingDetails, 2);
            if (!zMatchSignaturesCompat && z) {
                zMatchSignaturesCompat = matchSignaturesCompat(str, packageSetting.sharedUser.signatures, signingDetails);
            }
            if (!zMatchSignaturesCompat && z2) {
                zMatchSignaturesCompat = matchSignaturesRecover(str, packageSetting.sharedUser.signatures.mSigningDetails, signingDetails, 2) || matchSignaturesRecover(str, signingDetails, packageSetting.sharedUser.signatures.mSigningDetails, 2);
                z3 |= zMatchSignaturesCompat;
            }
            if (!zMatchSignaturesCompat) {
                throw new PackageManagerException(-8, "Package " + str + " has no signatures that match those in shared user " + packageSetting.sharedUser.name + "; ignoring!");
            }
        }
        return z3;
    }

    public static int decompressFile(File file, File file2) throws ErrnoException {
        Throwable th;
        Throwable th2;
        if (PackageManagerService.DEBUG_COMPRESSION) {
            Slog.i("PackageManager", "Decompress file; src: " + file.getAbsolutePath() + ", dst: " + file2.getAbsolutePath());
        }
        try {
            GZIPInputStream gZIPInputStream = new GZIPInputStream(new FileInputStream(file));
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(file2, false);
                try {
                    FileUtils.copy(gZIPInputStream, fileOutputStream);
                    Os.chmod(file2.getAbsolutePath(), 420);
                    $closeResource(null, fileOutputStream);
                    return 1;
                } catch (Throwable th3) {
                    try {
                        throw th3;
                    } catch (Throwable th4) {
                        th = th3;
                        th2 = th4;
                        $closeResource(th, fileOutputStream);
                        throw th2;
                    }
                }
            } finally {
                $closeResource(null, gZIPInputStream);
            }
        } catch (IOException e) {
            logCriticalInfo(6, "Failed to decompress file; src: " + file.getAbsolutePath() + ", dst: " + file2.getAbsolutePath());
            return RequestStatus.SYS_ETIMEDOUT;
        }
    }

    public static File[] getCompressedFiles(String str) {
        File file = new File(str);
        String name = file.getName();
        int iLastIndexOf = name.lastIndexOf(PackageManagerService.STUB_SUFFIX);
        if (iLastIndexOf < 0 || name.length() != PackageManagerService.STUB_SUFFIX.length() + iLastIndexOf) {
            return null;
        }
        File parentFile = file.getParentFile();
        if (parentFile == null) {
            Slog.e("PackageManager", "Unable to determine stub parent dir for codePath: " + str);
            return null;
        }
        File[] fileArrListFiles = new File(parentFile, name.substring(0, iLastIndexOf)).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file2, String str2) {
                return str2.toLowerCase().endsWith(PackageManagerService.COMPRESSED_EXTENSION);
            }
        });
        if (PackageManagerService.DEBUG_COMPRESSION && fileArrListFiles != null && fileArrListFiles.length > 0) {
            Slog.i("PackageManager", "getCompressedFiles[" + str + "]: " + Arrays.toString(fileArrListFiles));
        }
        return fileArrListFiles;
    }

    public static boolean compressedFileExists(String str) {
        File[] compressedFiles = getCompressedFiles(str);
        return compressedFiles != null && compressedFiles.length > 0;
    }
}
