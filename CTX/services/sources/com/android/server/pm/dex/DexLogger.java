package com.android.server.pm.dex;

import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.os.RemoteException;
import android.util.ArraySet;
import android.util.ByteStringUtils;
import android.util.EventLog;
import android.util.PackageUtils;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.pm.Installer;
import com.android.server.pm.dex.DexManager;
import com.android.server.pm.dex.PackageDexUsage;
import java.io.File;
import java.util.Iterator;
import java.util.Set;

public class DexLogger implements DexManager.Listener {
    private static final String DCL_SUBTAG = "dcl";
    private static final int SNET_TAG = 1397638484;
    private static final String TAG = "DexLogger";
    private final Object mInstallLock;

    @GuardedBy("mInstallLock")
    private final Installer mInstaller;
    private final IPackageManager mPackageManager;

    public static DexManager.Listener getListener(IPackageManager iPackageManager, Installer installer, Object obj) {
        return new DexLogger(iPackageManager, installer, obj);
    }

    @VisibleForTesting
    DexLogger(IPackageManager iPackageManager, Installer installer, Object obj) {
        this.mPackageManager = iPackageManager;
        this.mInstaller = installer;
        this.mInstallLock = obj;
    }

    @Override
    public void onReconcileSecondaryDexFile(ApplicationInfo applicationInfo, PackageDexUsage.DexUseInfo dexUseInfo, String str, int i) {
        byte[] bArrHashSecondaryDexFile;
        int i2 = applicationInfo.uid;
        synchronized (this.mInstallLock) {
            try {
                bArrHashSecondaryDexFile = this.mInstaller.hashSecondaryDexFile(str, applicationInfo.packageName, i2, applicationInfo.volumeUuid, i);
            } catch (Installer.InstallerException e) {
                Slog.e(TAG, "Got InstallerException when hashing dex " + str + " : " + e.getMessage());
                bArrHashSecondaryDexFile = null;
            }
        }
        if (bArrHashSecondaryDexFile == null) {
            return;
        }
        String strComputeSha256Digest = PackageUtils.computeSha256Digest(new File(str).getName().getBytes());
        if (bArrHashSecondaryDexFile.length == 32) {
            strComputeSha256Digest = strComputeSha256Digest + ' ' + ByteStringUtils.toHexString(bArrHashSecondaryDexFile);
        }
        writeDclEvent(i2, strComputeSha256Digest);
        if (dexUseInfo.isUsedByOtherApps()) {
            Set<String> loadingPackages = dexUseInfo.getLoadingPackages();
            ArraySet arraySet = new ArraySet(loadingPackages.size());
            Iterator<String> it = loadingPackages.iterator();
            while (it.hasNext()) {
                try {
                    int packageUid = this.mPackageManager.getPackageUid(it.next(), 0, dexUseInfo.getOwnerUserId());
                    if (packageUid != -1 && packageUid != i2) {
                        arraySet.add(Integer.valueOf(packageUid));
                    }
                } catch (RemoteException e2) {
                }
            }
            Iterator it2 = arraySet.iterator();
            while (it2.hasNext()) {
                writeDclEvent(((Integer) it2.next()).intValue(), strComputeSha256Digest);
            }
        }
    }

    @VisibleForTesting
    void writeDclEvent(int i, String str) {
        EventLog.writeEvent(SNET_TAG, DCL_SUBTAG, Integer.valueOf(i), str);
    }
}
