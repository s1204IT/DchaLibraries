package com.android.server.om;

import android.content.om.OverlayInfo;
import android.content.pm.PackageInfo;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import com.android.server.om.OverlayManagerSettings;
import com.android.server.pm.PackageManagerService;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class OverlayManagerServiceImpl {
    private static final int FLAG_OVERLAY_IS_UPGRADING = 2;
    private static final int FLAG_TARGET_IS_UPGRADING = 1;
    private final String[] mDefaultOverlays;
    private final IdmapManager mIdmapManager;
    private final OverlayChangeListener mListener;
    private final PackageManagerHelper mPackageManager;
    private final OverlayManagerSettings mSettings;

    interface OverlayChangeListener {
        void onOverlaysChanged(String str, int i);
    }

    interface PackageManagerHelper {
        List<PackageInfo> getOverlayPackages(int i);

        PackageInfo getPackageInfo(String str, int i);

        boolean signaturesMatching(String str, String str2, int i);
    }

    private static boolean mustReinitializeOverlay(PackageInfo packageInfo, OverlayInfo overlayInfo) {
        if (overlayInfo == null || !Objects.equals(packageInfo.overlayTarget, overlayInfo.targetPackageName) || packageInfo.isStaticOverlayPackage() != overlayInfo.isStatic) {
            return true;
        }
        if (packageInfo.isStaticOverlayPackage() && packageInfo.overlayPriority != overlayInfo.priority) {
            return true;
        }
        return false;
    }

    OverlayManagerServiceImpl(PackageManagerHelper packageManagerHelper, IdmapManager idmapManager, OverlayManagerSettings overlayManagerSettings, String[] strArr, OverlayChangeListener overlayChangeListener) {
        this.mPackageManager = packageManagerHelper;
        this.mIdmapManager = idmapManager;
        this.mSettings = overlayManagerSettings;
        this.mDefaultOverlays = strArr;
        this.mListener = overlayChangeListener;
    }

    ArrayList<String> updateOverlaysForUser(int i) {
        PackageInfo packageInfo;
        ArraySet arraySet = new ArraySet();
        ArrayMap<String, List<OverlayInfo>> overlaysForUser = this.mSettings.getOverlaysForUser(i);
        int size = overlaysForUser.size();
        ArrayMap arrayMap = new ArrayMap(size);
        for (int i2 = 0; i2 < size; i2++) {
            List<OverlayInfo> listValueAt = overlaysForUser.valueAt(i2);
            int size2 = listValueAt.size();
            for (int i3 = 0; i3 < size2; i3++) {
                OverlayInfo overlayInfo = listValueAt.get(i3);
                arrayMap.put(overlayInfo.packageName, overlayInfo);
            }
        }
        List<PackageInfo> overlayPackages = this.mPackageManager.getOverlayPackages(i);
        int size3 = overlayPackages.size();
        for (int i4 = 0; i4 < size3; i4++) {
            PackageInfo packageInfo2 = overlayPackages.get(i4);
            OverlayInfo overlayInfo2 = (OverlayInfo) arrayMap.get(packageInfo2.packageName);
            if (mustReinitializeOverlay(packageInfo2, overlayInfo2)) {
                if (overlayInfo2 != null) {
                    arraySet.add(overlayInfo2.targetPackageName);
                }
                packageInfo = packageInfo2;
                this.mSettings.init(packageInfo2.packageName, i, packageInfo2.overlayTarget, packageInfo2.applicationInfo.getBaseCodePath(), packageInfo2.isStaticOverlayPackage(), packageInfo2.overlayPriority, packageInfo2.overlayCategory);
            } else {
                packageInfo = packageInfo2;
            }
            arrayMap.remove(packageInfo.packageName);
        }
        int size4 = arrayMap.size();
        for (int i5 = 0; i5 < size4; i5++) {
            OverlayInfo overlayInfo3 = (OverlayInfo) arrayMap.valueAt(i5);
            this.mSettings.remove(overlayInfo3.packageName, overlayInfo3.userId);
            removeIdmapIfPossible(overlayInfo3);
            arraySet.add(overlayInfo3.targetPackageName);
        }
        for (int i6 = 0; i6 < size3; i6++) {
            PackageInfo packageInfo3 = overlayPackages.get(i6);
            try {
                updateState(packageInfo3.overlayTarget, packageInfo3.packageName, i, 0);
            } catch (OverlayManagerSettings.BadKeyException e) {
                Slog.e("OverlayManager", "failed to update settings", e);
                this.mSettings.remove(packageInfo3.packageName, i);
            }
            arraySet.add(packageInfo3.overlayTarget);
        }
        Iterator it = arraySet.iterator();
        while (it.hasNext()) {
            if (this.mPackageManager.getPackageInfo((String) it.next(), i) == null) {
                it.remove();
            }
        }
        ArraySet arraySet2 = new ArraySet();
        ArrayMap<String, List<OverlayInfo>> overlaysForUser2 = this.mSettings.getOverlaysForUser(i);
        int size5 = overlaysForUser2.size();
        for (int i7 = 0; i7 < size5; i7++) {
            List<OverlayInfo> listValueAt2 = overlaysForUser2.valueAt(i7);
            int size6 = listValueAt2 != null ? listValueAt2.size() : 0;
            for (int i8 = 0; i8 < size6; i8++) {
                OverlayInfo overlayInfo4 = listValueAt2.get(i8);
                if (overlayInfo4.isEnabled()) {
                    arraySet2.add(overlayInfo4.category);
                }
            }
        }
        for (String str : this.mDefaultOverlays) {
            try {
                OverlayInfo overlayInfo5 = this.mSettings.getOverlayInfo(str, i);
                if (!arraySet2.contains(overlayInfo5.category)) {
                    Slog.w("OverlayManager", "Enabling default overlay '" + str + "' for target '" + overlayInfo5.targetPackageName + "' in category '" + overlayInfo5.category + "' for user " + i);
                    this.mSettings.setEnabled(overlayInfo5.packageName, i, true);
                    try {
                        if (updateState(overlayInfo5.targetPackageName, overlayInfo5.packageName, i, 0)) {
                            arraySet.add(overlayInfo5.targetPackageName);
                        }
                    } catch (OverlayManagerSettings.BadKeyException e2) {
                        e = e2;
                        Slog.e("OverlayManager", "Failed to set default overlay '" + str + "' for user " + i, e);
                    }
                }
            } catch (OverlayManagerSettings.BadKeyException e3) {
                e = e3;
            }
        }
        return new ArrayList<>(arraySet);
    }

    void onUserRemoved(int i) {
        this.mSettings.removeUser(i);
    }

    void onTargetPackageAdded(String str, int i) {
        if (updateAllOverlaysForTarget(str, i, 0)) {
            this.mListener.onOverlaysChanged(str, i);
        }
    }

    void onTargetPackageChanged(String str, int i) {
        updateAllOverlaysForTarget(str, i, 0);
    }

    void onTargetPackageUpgrading(String str, int i) {
        updateAllOverlaysForTarget(str, i, 1);
    }

    void onTargetPackageUpgraded(String str, int i) {
        updateAllOverlaysForTarget(str, i, 0);
    }

    void onTargetPackageRemoved(String str, int i) {
        if (updateAllOverlaysForTarget(str, i, 0)) {
            this.mListener.onOverlaysChanged(str, i);
        }
    }

    private boolean updateAllOverlaysForTarget(String str, int i, int i2) {
        List<OverlayInfo> overlaysForTarget = this.mSettings.getOverlaysForTarget(str, i);
        int size = overlaysForTarget.size();
        boolean zRemove = false;
        for (int i3 = 0; i3 < size; i3++) {
            OverlayInfo overlayInfo = overlaysForTarget.get(i3);
            if (this.mPackageManager.getPackageInfo(overlayInfo.packageName, i) == null) {
                zRemove |= this.mSettings.remove(overlayInfo.packageName, overlayInfo.userId);
                removeIdmapIfPossible(overlayInfo);
            } else {
                try {
                    zRemove |= updateState(str, overlayInfo.packageName, i, i2);
                } catch (OverlayManagerSettings.BadKeyException e) {
                    Slog.e("OverlayManager", "failed to update settings", e);
                    zRemove |= this.mSettings.remove(overlayInfo.packageName, i);
                }
            }
        }
        if (!zRemove && getEnabledOverlayPackageNames(PackageManagerService.PLATFORM_PACKAGE_NAME, i).isEmpty()) {
            return false;
        }
        return true;
    }

    void onOverlayPackageAdded(String str, int i) {
        PackageInfo packageInfo = this.mPackageManager.getPackageInfo(str, i);
        if (packageInfo == null) {
            Slog.w("OverlayManager", "overlay package " + str + " was added, but couldn't be found");
            onOverlayPackageRemoved(str, i);
            return;
        }
        this.mSettings.init(str, i, packageInfo.overlayTarget, packageInfo.applicationInfo.getBaseCodePath(), packageInfo.isStaticOverlayPackage(), packageInfo.overlayPriority, packageInfo.overlayCategory);
        try {
            if (updateState(packageInfo.overlayTarget, str, i, 0)) {
                this.mListener.onOverlaysChanged(packageInfo.overlayTarget, i);
            }
        } catch (OverlayManagerSettings.BadKeyException e) {
            Slog.e("OverlayManager", "failed to update settings", e);
            this.mSettings.remove(str, i);
        }
    }

    void onOverlayPackageChanged(String str, int i) {
        try {
            OverlayInfo overlayInfo = this.mSettings.getOverlayInfo(str, i);
            if (updateState(overlayInfo.targetPackageName, str, i, 0)) {
                this.mListener.onOverlaysChanged(overlayInfo.targetPackageName, i);
            }
        } catch (OverlayManagerSettings.BadKeyException e) {
            Slog.e("OverlayManager", "failed to update settings", e);
        }
    }

    void onOverlayPackageUpgrading(String str, int i) {
        try {
            OverlayInfo overlayInfo = this.mSettings.getOverlayInfo(str, i);
            if (updateState(overlayInfo.targetPackageName, str, i, 2)) {
                removeIdmapIfPossible(overlayInfo);
                this.mListener.onOverlaysChanged(overlayInfo.targetPackageName, i);
            }
        } catch (OverlayManagerSettings.BadKeyException e) {
            Slog.e("OverlayManager", "failed to update settings", e);
        }
    }

    void onOverlayPackageUpgraded(String str, int i) {
        PackageInfo packageInfo = this.mPackageManager.getPackageInfo(str, i);
        if (packageInfo == null) {
            Slog.w("OverlayManager", "overlay package " + str + " was upgraded, but couldn't be found");
            onOverlayPackageRemoved(str, i);
            return;
        }
        try {
            OverlayInfo overlayInfo = this.mSettings.getOverlayInfo(str, i);
            if (mustReinitializeOverlay(packageInfo, overlayInfo)) {
                if (overlayInfo != null && !overlayInfo.targetPackageName.equals(packageInfo.overlayTarget)) {
                    this.mListener.onOverlaysChanged(packageInfo.overlayTarget, i);
                }
                this.mSettings.init(str, i, packageInfo.overlayTarget, packageInfo.applicationInfo.getBaseCodePath(), packageInfo.isStaticOverlayPackage(), packageInfo.overlayPriority, packageInfo.overlayCategory);
            }
            if (updateState(packageInfo.overlayTarget, str, i, 0)) {
                this.mListener.onOverlaysChanged(packageInfo.overlayTarget, i);
            }
        } catch (OverlayManagerSettings.BadKeyException e) {
            Slog.e("OverlayManager", "failed to update settings", e);
        }
    }

    void onOverlayPackageRemoved(String str, int i) {
        try {
            OverlayInfo overlayInfo = this.mSettings.getOverlayInfo(str, i);
            if (this.mSettings.remove(str, i)) {
                removeIdmapIfPossible(overlayInfo);
                if (overlayInfo.isEnabled()) {
                    this.mListener.onOverlaysChanged(overlayInfo.targetPackageName, i);
                }
            }
        } catch (OverlayManagerSettings.BadKeyException e) {
            Slog.e("OverlayManager", "failed to remove overlay", e);
        }
    }

    OverlayInfo getOverlayInfo(String str, int i) {
        try {
            return this.mSettings.getOverlayInfo(str, i);
        } catch (OverlayManagerSettings.BadKeyException e) {
            return null;
        }
    }

    List<OverlayInfo> getOverlayInfosForTarget(String str, int i) {
        return this.mSettings.getOverlaysForTarget(str, i);
    }

    Map<String, List<OverlayInfo>> getOverlaysForUser(int i) {
        return this.mSettings.getOverlaysForUser(i);
    }

    boolean setEnabled(String str, boolean z, int i) {
        PackageInfo packageInfo = this.mPackageManager.getPackageInfo(str, i);
        if (packageInfo == null || packageInfo.isStaticOverlayPackage()) {
            return false;
        }
        try {
            OverlayInfo overlayInfo = this.mSettings.getOverlayInfo(str, i);
            if (this.mSettings.setEnabled(str, i, z) | updateState(overlayInfo.targetPackageName, overlayInfo.packageName, i, 0)) {
                this.mListener.onOverlaysChanged(overlayInfo.targetPackageName, i);
                return true;
            }
            return true;
        } catch (OverlayManagerSettings.BadKeyException e) {
            return false;
        }
    }

    boolean setEnabledExclusive(String str, boolean z, int i) {
        if (this.mPackageManager.getPackageInfo(str, i) == null) {
            return false;
        }
        try {
            OverlayInfo overlayInfo = this.mSettings.getOverlayInfo(str, i);
            String str2 = overlayInfo.targetPackageName;
            List<OverlayInfo> overlayInfosForTarget = getOverlayInfosForTarget(str2, i);
            overlayInfosForTarget.remove(overlayInfo);
            boolean enabled = false;
            for (int i2 = 0; i2 < overlayInfosForTarget.size(); i2++) {
                String str3 = overlayInfosForTarget.get(i2).packageName;
                PackageInfo packageInfo = this.mPackageManager.getPackageInfo(str3, i);
                if (packageInfo == null) {
                    enabled |= this.mSettings.remove(str3, i);
                } else if (!packageInfo.isStaticOverlayPackage() && (!z || Objects.equals(packageInfo.overlayCategory, overlayInfo.category))) {
                    enabled = enabled | this.mSettings.setEnabled(str3, i, false) | updateState(str2, str3, i, 0);
                }
            }
            if (updateState(str2, str, i, 0) | this.mSettings.setEnabled(str, i, true) | enabled) {
                this.mListener.onOverlaysChanged(str2, i);
            }
            return true;
        } catch (OverlayManagerSettings.BadKeyException e) {
            return false;
        }
    }

    private boolean isPackageUpdatableOverlay(String str, int i) {
        PackageInfo packageInfo = this.mPackageManager.getPackageInfo(str, i);
        if (packageInfo == null || packageInfo.isStaticOverlayPackage()) {
            return false;
        }
        return true;
    }

    boolean setPriority(String str, String str2, int i) {
        PackageInfo packageInfo;
        if (!isPackageUpdatableOverlay(str, i) || (packageInfo = this.mPackageManager.getPackageInfo(str, i)) == null) {
            return false;
        }
        if (this.mSettings.setPriority(str, str2, i)) {
            this.mListener.onOverlaysChanged(packageInfo.overlayTarget, i);
            return true;
        }
        return true;
    }

    boolean setHighestPriority(String str, int i) {
        PackageInfo packageInfo;
        if (!isPackageUpdatableOverlay(str, i) || (packageInfo = this.mPackageManager.getPackageInfo(str, i)) == null) {
            return false;
        }
        if (this.mSettings.setHighestPriority(str, i)) {
            this.mListener.onOverlaysChanged(packageInfo.overlayTarget, i);
            return true;
        }
        return true;
    }

    boolean setLowestPriority(String str, int i) {
        PackageInfo packageInfo;
        if (!isPackageUpdatableOverlay(str, i) || (packageInfo = this.mPackageManager.getPackageInfo(str, i)) == null) {
            return false;
        }
        if (this.mSettings.setLowestPriority(str, i)) {
            this.mListener.onOverlaysChanged(packageInfo.overlayTarget, i);
            return true;
        }
        return true;
    }

    void onDump(PrintWriter printWriter) {
        this.mSettings.dump(printWriter);
        printWriter.println("Default overlays: " + TextUtils.join(";", this.mDefaultOverlays));
    }

    List<String> getEnabledOverlayPackageNames(String str, int i) {
        List<OverlayInfo> overlaysForTarget = this.mSettings.getOverlaysForTarget(str, i);
        ArrayList arrayList = new ArrayList(overlaysForTarget.size());
        int size = overlaysForTarget.size();
        for (int i2 = 0; i2 < size; i2++) {
            OverlayInfo overlayInfo = overlaysForTarget.get(i2);
            if (overlayInfo.isEnabled()) {
                arrayList.add(overlayInfo.packageName);
            }
        }
        return arrayList;
    }

    private boolean updateState(String str, String str2, int i, int i2) throws OverlayManagerSettings.BadKeyException {
        PackageInfo packageInfo = this.mPackageManager.getPackageInfo(str, i);
        PackageInfo packageInfo2 = this.mPackageManager.getPackageInfo(str2, i);
        if (packageInfo != null && packageInfo2 != null && (!PackageManagerService.PLATFORM_PACKAGE_NAME.equals(str) || !packageInfo2.isStaticOverlayPackage())) {
            this.mIdmapManager.createIdmap(packageInfo, packageInfo2, i);
        }
        boolean baseCodePath = false;
        if (packageInfo2 != null) {
            baseCodePath = false | this.mSettings.setBaseCodePath(str2, i, packageInfo2.applicationInfo.getBaseCodePath()) | this.mSettings.setCategory(str2, i, packageInfo2.overlayCategory);
        }
        int state = this.mSettings.getState(str2, i);
        int iCalculateNewState = calculateNewState(packageInfo, packageInfo2, i, i2);
        if (state != iCalculateNewState) {
            return baseCodePath | this.mSettings.setState(str2, i, iCalculateNewState);
        }
        return baseCodePath;
    }

    private int calculateNewState(PackageInfo packageInfo, PackageInfo packageInfo2, int i, int i2) throws OverlayManagerSettings.BadKeyException {
        if ((i2 & 1) != 0) {
            return 4;
        }
        if ((i2 & 2) != 0) {
            return 5;
        }
        if (packageInfo == null) {
            return 0;
        }
        if (!this.mIdmapManager.idmapExists(packageInfo2, i)) {
            return 1;
        }
        if (packageInfo2.isStaticOverlayPackage()) {
            return 6;
        }
        return this.mSettings.getEnabled(packageInfo2.packageName, i) ? 3 : 2;
    }

    private void removeIdmapIfPossible(OverlayInfo overlayInfo) {
        OverlayInfo overlayInfo2;
        if (!this.mIdmapManager.idmapExists(overlayInfo)) {
            return;
        }
        for (int i : this.mSettings.getUsers()) {
            try {
                overlayInfo2 = this.mSettings.getOverlayInfo(overlayInfo.packageName, i);
            } catch (OverlayManagerSettings.BadKeyException e) {
            }
            if (overlayInfo2 != null && overlayInfo2.isEnabled()) {
                return;
            }
        }
        this.mIdmapManager.removeIdmap(overlayInfo, overlayInfo.userId);
    }
}
