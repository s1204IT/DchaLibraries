package com.android.server.om;

import android.content.om.OverlayInfo;
import android.content.pm.PackageInfo;
import android.os.UserHandle;
import android.util.Slog;
import com.android.server.pm.Installer;
import java.io.File;

class IdmapManager {
    private final Installer mInstaller;

    IdmapManager(Installer installer) {
        this.mInstaller = installer;
    }

    boolean createIdmap(PackageInfo packageInfo, PackageInfo packageInfo2, int i) {
        int sharedAppGid = UserHandle.getSharedAppGid(packageInfo.applicationInfo.uid);
        String baseCodePath = packageInfo.applicationInfo.getBaseCodePath();
        String baseCodePath2 = packageInfo2.applicationInfo.getBaseCodePath();
        try {
            this.mInstaller.idmap(baseCodePath, baseCodePath2, sharedAppGid);
            return true;
        } catch (Installer.InstallerException e) {
            Slog.w("OverlayManager", "failed to generate idmap for " + baseCodePath + " and " + baseCodePath2 + ": " + e.getMessage());
            return false;
        }
    }

    boolean removeIdmap(OverlayInfo overlayInfo, int i) {
        try {
            this.mInstaller.removeIdmap(overlayInfo.baseCodePath);
            return true;
        } catch (Installer.InstallerException e) {
            Slog.w("OverlayManager", "failed to remove idmap for " + overlayInfo.baseCodePath + ": " + e.getMessage());
            return false;
        }
    }

    boolean idmapExists(OverlayInfo overlayInfo) {
        return new File(getIdmapPath(overlayInfo.baseCodePath)).isFile();
    }

    boolean idmapExists(PackageInfo packageInfo, int i) {
        return new File(getIdmapPath(packageInfo.applicationInfo.getBaseCodePath())).isFile();
    }

    private String getIdmapPath(String str) {
        return "/data/resource-cache/" + str.substring(1).replace('/', '@') + "@idmap";
    }
}
