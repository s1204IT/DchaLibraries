package com.android.server.pm.permission;

import android.R;
import android.content.Context;
import android.content.pm.PackageParser;
import android.util.ArrayMap;
import android.util.ArraySet;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.XmlUtils;
import com.android.server.pm.DumpState;
import com.android.server.pm.PackageManagerService;
import com.mediatek.cta.CtaManager;
import com.mediatek.cta.CtaManagerFactory;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Iterator;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class PermissionSettings {
    private static final CtaManager sCtaManager = CtaManagerFactory.getInstance().makeCtaManager();
    private final Object mLock;
    public final boolean mPermissionReviewRequired;

    @GuardedBy("mLock")
    final ArrayMap<String, BasePermission> mPermissions = new ArrayMap<>();

    @GuardedBy("mLock")
    final ArrayMap<String, BasePermission> mPermissionTrees = new ArrayMap<>();

    @GuardedBy("mLock")
    final ArrayMap<String, PackageParser.PermissionGroup> mPermissionGroups = new ArrayMap<>();

    @GuardedBy("mLock")
    final ArrayMap<String, ArraySet<String>> mAppOpPermissionPackages = new ArrayMap<>();

    PermissionSettings(Context context, Object obj) {
        this.mPermissionReviewRequired = sCtaManager.isCtaSupported() ? true : context.getResources().getBoolean(R.^attr-private.magnifierColorOverlay);
        this.mLock = obj;
    }

    public BasePermission getPermission(String str) {
        BasePermission permissionLocked;
        synchronized (this.mLock) {
            permissionLocked = getPermissionLocked(str);
        }
        return permissionLocked;
    }

    public void addAppOpPackage(String str, String str2) {
        ArraySet<String> arraySet = this.mAppOpPermissionPackages.get(str);
        if (arraySet == null) {
            arraySet = new ArraySet<>();
            this.mAppOpPermissionPackages.put(str, arraySet);
        }
        arraySet.add(str2);
    }

    public void transferPermissions(String str, String str2) {
        synchronized (this.mLock) {
            int i = 0;
            while (i < 2) {
                Iterator<BasePermission> it = (i == 0 ? this.mPermissionTrees : this.mPermissions).values().iterator();
                while (it.hasNext()) {
                    it.next().transfer(str, str2);
                }
                i++;
            }
        }
    }

    public boolean canPropagatePermissionToInstantApp(String str) {
        boolean z;
        synchronized (this.mLock) {
            BasePermission basePermission = this.mPermissions.get(str);
            z = basePermission != null && (basePermission.isRuntime() || basePermission.isDevelopment()) && basePermission.isInstant();
        }
        return z;
    }

    public void readPermissions(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        synchronized (this.mLock) {
            readPermissions(this.mPermissions, xmlPullParser);
        }
    }

    public void readPermissionTrees(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        synchronized (this.mLock) {
            readPermissions(this.mPermissionTrees, xmlPullParser);
        }
    }

    public void writePermissions(XmlSerializer xmlSerializer) throws IOException {
        synchronized (this.mLock) {
            Iterator<BasePermission> it = this.mPermissions.values().iterator();
            while (it.hasNext()) {
                it.next().writeLPr(xmlSerializer);
            }
        }
    }

    public void writePermissionTrees(XmlSerializer xmlSerializer) throws IOException {
        synchronized (this.mLock) {
            Iterator<BasePermission> it = this.mPermissionTrees.values().iterator();
            while (it.hasNext()) {
                it.next().writeLPr(xmlSerializer);
            }
        }
    }

    public static void readPermissions(ArrayMap<String, BasePermission> arrayMap, XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            if (next != 1) {
                if (next != 3 || xmlPullParser.getDepth() > depth) {
                    if (next != 3 && next != 4) {
                        if (!BasePermission.readLPw(arrayMap, xmlPullParser)) {
                            PackageManagerService.reportSettingsProblem(5, "Unknown element reading permissions: " + xmlPullParser.getName() + " at " + xmlPullParser.getPositionDescription());
                        }
                        XmlUtils.skipCurrentTag(xmlPullParser);
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    public void dumpPermissions(PrintWriter printWriter, String str, ArraySet<String> arraySet, boolean z, DumpState dumpState) {
        synchronized (this.mLock) {
            Iterator<BasePermission> it = this.mPermissions.values().iterator();
            boolean zDumpPermissionsLPr = false;
            while (it.hasNext()) {
                zDumpPermissionsLPr = it.next().dumpPermissionsLPr(printWriter, str, arraySet, z, zDumpPermissionsLPr, dumpState);
            }
            if (str == null && arraySet == null) {
                for (int i = 0; i < this.mAppOpPermissionPackages.size(); i++) {
                    if (i == 0) {
                        if (dumpState.onTitlePrinted()) {
                            printWriter.println();
                        }
                        printWriter.println("AppOp Permissions:");
                    }
                    printWriter.print("  AppOp Permission ");
                    printWriter.print(this.mAppOpPermissionPackages.keyAt(i));
                    printWriter.println(":");
                    ArraySet<String> arraySetValueAt = this.mAppOpPermissionPackages.valueAt(i);
                    for (int i2 = 0; i2 < arraySetValueAt.size(); i2++) {
                        printWriter.print("    ");
                        printWriter.println(arraySetValueAt.valueAt(i2));
                    }
                }
            }
        }
    }

    @GuardedBy("mLock")
    BasePermission getPermissionLocked(String str) {
        return this.mPermissions.get(str);
    }

    @GuardedBy("mLock")
    BasePermission getPermissionTreeLocked(String str) {
        return this.mPermissionTrees.get(str);
    }

    @GuardedBy("mLock")
    void putPermissionLocked(String str, BasePermission basePermission) {
        this.mPermissions.put(str, basePermission);
    }

    @GuardedBy("mLock")
    void putPermissionTreeLocked(String str, BasePermission basePermission) {
        this.mPermissionTrees.put(str, basePermission);
    }

    @GuardedBy("mLock")
    void removePermissionLocked(String str) {
        this.mPermissions.remove(str);
    }

    @GuardedBy("mLock")
    void removePermissionTreeLocked(String str) {
        this.mPermissionTrees.remove(str);
    }

    @GuardedBy("mLock")
    Collection<BasePermission> getAllPermissionsLocked() {
        return this.mPermissions.values();
    }

    @GuardedBy("mLock")
    Collection<BasePermission> getAllPermissionTreesLocked() {
        return this.mPermissionTrees.values();
    }

    BasePermission enforcePermissionTree(String str, int i) {
        BasePermission basePermissionEnforcePermissionTree;
        synchronized (this.mLock) {
            basePermissionEnforcePermissionTree = BasePermission.enforcePermissionTree(this.mPermissionTrees.values(), str, i);
        }
        return basePermissionEnforcePermissionTree;
    }

    public boolean isPermissionInstant(String str) {
        boolean z;
        synchronized (this.mLock) {
            BasePermission basePermission = this.mPermissions.get(str);
            z = basePermission != null && basePermission.isInstant();
        }
        return z;
    }

    boolean isPermissionAppOp(String str) {
        boolean z;
        synchronized (this.mLock) {
            BasePermission basePermission = this.mPermissions.get(str);
            z = basePermission != null && basePermission.isAppOp();
        }
        return z;
    }
}
