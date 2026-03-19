package com.android.server.pm;

import android.app.AppGlobals;
import android.content.Intent;
import android.content.pm.PackageParser;
import android.content.pm.ResolveInfo;
import android.os.RemoteException;
import android.system.ErrnoException;
import android.util.ArraySet;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import libcore.io.Libcore;

public class PackageManagerServiceUtils {
    private static final long SEVEN_DAYS_IN_MILLISECONDS = 604800000;

    private static ArraySet<String> getPackageNamesForIntent(Intent intent, int userId) {
        List<ResolveInfo> ris = null;
        try {
            ris = AppGlobals.getPackageManager().queryIntentReceivers(intent, (String) null, 0, userId).getList();
        } catch (RemoteException e) {
        }
        ArraySet<String> pkgNames = new ArraySet<>();
        if (ris != null) {
            for (ResolveInfo ri : ris) {
                pkgNames.add(ri.activityInfo.packageName);
            }
        }
        return pkgNames;
    }

    private static void filterRecentlyUsedApps(Collection<PackageParser.Package> pkgs, long estimatedPreviousSystemUseTime, long dexOptLRUThresholdInMills) {
        int total = pkgs.size();
        int skipped = 0;
        Iterator<PackageParser.Package> i = pkgs.iterator();
        while (i.hasNext()) {
            PackageParser.Package pkg = i.next();
            long then = pkg.getLatestForegroundPackageUseTimeInMills();
            if (then < estimatedPreviousSystemUseTime - dexOptLRUThresholdInMills) {
                if (PackageManagerService.DEBUG_DEXOPT) {
                    Log.i("PackageManager", "Skipping dexopt of " + pkg.packageName + " last used in foreground: " + (then == 0 ? "never" : new Date(then)));
                }
                i.remove();
                skipped++;
            } else if (PackageManagerService.DEBUG_DEXOPT) {
                Log.i("PackageManager", "Will dexopt " + pkg.packageName + " last used in foreground: " + (then == 0 ? "never" : new Date(then)));
            }
        }
        if (!PackageManagerService.DEBUG_DEXOPT) {
            return;
        }
        Log.i("PackageManager", "Skipped dexopt " + skipped + " of " + total);
    }

    public static List<PackageParser.Package> getPackagesForDexopt(Collection<PackageParser.Package> packages, PackageManagerService packageManagerService) {
        ArrayList<PackageParser.Package> remainingPkgs = new ArrayList<>((Collection<? extends PackageParser.Package>) packages);
        LinkedList<PackageParser.Package> result = new LinkedList<>();
        for (PackageParser.Package pkg : remainingPkgs) {
            if (pkg.coreApp) {
                if (PackageManagerService.DEBUG_DEXOPT) {
                    Log.i("PackageManager", "Adding core app " + result.size() + ": " + pkg.packageName);
                }
                result.add(pkg);
            }
        }
        remainingPkgs.removeAll(result);
        Intent intent = new Intent("android.intent.action.PRE_BOOT_COMPLETED");
        ArraySet<String> pkgNames = getPackageNamesForIntent(intent, 0);
        for (PackageParser.Package pkg2 : remainingPkgs) {
            if (pkgNames.contains(pkg2.packageName)) {
                if (PackageManagerService.DEBUG_DEXOPT) {
                    Log.i("PackageManager", "Adding pre boot system app " + result.size() + ": " + pkg2.packageName);
                }
                result.add(pkg2);
            }
        }
        remainingPkgs.removeAll(result);
        for (PackageParser.Package pkg3 : remainingPkgs) {
            if (PackageDexOptimizer.isUsedByOtherApps(pkg3)) {
                if (PackageManagerService.DEBUG_DEXOPT) {
                    Log.i("PackageManager", "Adding app used by other apps " + result.size() + ": " + pkg3.packageName);
                }
                result.add(pkg3);
            }
        }
        remainingPkgs.removeAll(result);
        if (!remainingPkgs.isEmpty() && packageManagerService.isHistoricalPackageUsageAvailable()) {
            if (PackageManagerService.DEBUG_DEXOPT) {
                Log.i("PackageManager", "Looking at historical package use");
            }
            PackageParser.Package lastUsed = (PackageParser.Package) Collections.max(remainingPkgs, new Comparator() {
                @Override
                public int compare(Object arg0, Object arg1) {
                    return PackageManagerServiceUtils.m2551com_android_server_pm_PackageManagerServiceUtils_lambda$1((PackageParser.Package) arg0, (PackageParser.Package) arg1);
                }
            });
            if (PackageManagerService.DEBUG_DEXOPT) {
                Log.i("PackageManager", "Taking package " + lastUsed.packageName + " as reference in time use");
            }
            long estimatedPreviousSystemUseTime = lastUsed.getLatestForegroundPackageUseTimeInMills();
            if (estimatedPreviousSystemUseTime != 0) {
                filterRecentlyUsedApps(remainingPkgs, estimatedPreviousSystemUseTime, 604800000L);
            }
        }
        result.addAll(remainingPkgs);
        Set<PackageParser.Package> dependencies = new HashSet<>();
        for (PackageParser.Package p : result) {
            dependencies.addAll(packageManagerService.findSharedNonSystemLibraries(p));
        }
        if (!dependencies.isEmpty()) {
            dependencies.removeAll(result);
        }
        result.addAll(dependencies);
        if (PackageManagerService.DEBUG_DEXOPT) {
            StringBuilder sb = new StringBuilder();
            for (PackageParser.Package pkg4 : result) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(pkg4.packageName);
            }
            Log.i("PackageManager", "Packages to be dexopted: " + sb.toString());
        }
        return result;
    }

    static int m2551com_android_server_pm_PackageManagerServiceUtils_lambda$1(PackageParser.Package pkg1, PackageParser.Package pkg2) {
        return Long.compare(pkg1.getLatestForegroundPackageUseTimeInMills(), pkg2.getLatestForegroundPackageUseTimeInMills());
    }

    public static String realpath(File path) throws IOException {
        try {
            return Libcore.os.realpath(path.getAbsolutePath());
        } catch (ErrnoException ee) {
            throw ee.rethrowAsIOException();
        }
    }
}
