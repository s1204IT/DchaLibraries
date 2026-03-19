package com.android.server.pm;

import android.content.pm.PackageParser;
import android.os.SystemProperties;
import java.util.ArrayList;
import java.util.HashMap;

final class ResmonFilter {
    ResmonFilter() {
    }

    void filt(Settings settings, HashMap<String, PackageParser.Package> packages) {
        if (!"eng".equals(SystemProperties.get("ro.build.type"))) {
            return;
        }
        ResmonWhitelistPackage rsp = new ResmonWhitelistPackage();
        rsp.readList();
        if (rsp.mPackages.isEmpty() || rsp.mPackages.size() <= 0) {
            return;
        }
        ArrayList<Integer> excludeUidList = new ArrayList<>();
        for (int i = 0; i < rsp.mPackages.size(); i++) {
            PackageSetting ps = settings.mPackages.get(rsp.mPackages.get(i));
            if (ps != null && ps.pkg != null && !excludeUidList.contains(Integer.valueOf(ps.pkg.applicationInfo.uid))) {
                excludeUidList.add(Integer.valueOf(ps.pkg.applicationInfo.uid));
            }
        }
        ArrayList<Integer> uidList = new ArrayList<>();
        for (PackageParser.Package pkg : packages.values()) {
            PackageSetting ps2 = settings.mPackages.get(pkg.packageName);
            if (ps2 == null || (ps2.pkgFlags & 1) != 0) {
                int curUid = pkg.applicationInfo.uid;
                if (!excludeUidList.contains(Integer.valueOf(curUid)) && !uidList.contains(Integer.valueOf(curUid))) {
                    uidList.add(Integer.valueOf(curUid));
                }
            }
        }
        ResmonUidList rsu = new ResmonUidList();
        rsu.updateList(uidList);
    }
}
