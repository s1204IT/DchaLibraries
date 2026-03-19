package com.android.server.pm.permission;

import android.content.pm.PackageParser;
import android.content.pm.PermissionInfo;
import android.content.pm.Signature;
import android.os.UserHandle;
import android.util.Log;
import android.util.Slog;
import com.android.server.pm.DumpState;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.PackageSettingBase;
import com.android.server.pm.Settings;
import com.android.server.voiceinteraction.DatabaseHelper;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

public final class BasePermission {
    static final String TAG = "PackageManager";
    public static final int TYPE_BUILTIN = 1;
    public static final int TYPE_DYNAMIC = 2;
    public static final int TYPE_NORMAL = 0;
    private int[] gids;
    private boolean mPermissionDefinitionChanged;
    final String name;
    PermissionInfo pendingPermissionInfo;
    private boolean perUser;
    PackageParser.Permission perm;
    int protectionLevel = 2;
    String sourcePackageName;
    PackageSettingBase sourcePackageSetting;
    final int type;
    int uid;

    @Retention(RetentionPolicy.SOURCE)
    public @interface PermissionType {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface ProtectionLevel {
    }

    public BasePermission(String str, String str2, int i) {
        this.name = str;
        this.sourcePackageName = str2;
        this.type = i;
    }

    public String toString() {
        return "BasePermission{" + Integer.toHexString(System.identityHashCode(this)) + " " + this.name + "}";
    }

    public String getName() {
        return this.name;
    }

    public int getProtectionLevel() {
        return this.protectionLevel;
    }

    public String getSourcePackageName() {
        return this.sourcePackageName;
    }

    public PackageSettingBase getSourcePackageSetting() {
        return this.sourcePackageSetting;
    }

    public Signature[] getSourceSignatures() {
        return this.sourcePackageSetting.getSignatures();
    }

    public boolean isPermissionDefinitionChanged() {
        return this.mPermissionDefinitionChanged;
    }

    public int getType() {
        return this.type;
    }

    public int getUid() {
        return this.uid;
    }

    public void setGids(int[] iArr, boolean z) {
        this.gids = iArr;
        this.perUser = z;
    }

    public void setPermission(PackageParser.Permission permission) {
        this.perm = permission;
    }

    public void setSourcePackageSetting(PackageSettingBase packageSettingBase) {
        this.sourcePackageSetting = packageSettingBase;
    }

    public void setPermissionDefinitionChanged(boolean z) {
        this.mPermissionDefinitionChanged = z;
    }

    public int[] computeGids(int i) {
        if (this.perUser) {
            int[] iArr = new int[this.gids.length];
            for (int i2 = 0; i2 < this.gids.length; i2++) {
                iArr[i2] = UserHandle.getUid(i, this.gids[i2]);
            }
            return iArr;
        }
        return this.gids;
    }

    public int calculateFootprint(BasePermission basePermission) {
        if (this.uid == basePermission.uid) {
            return basePermission.name.length() + basePermission.perm.info.calculateFootprint();
        }
        return 0;
    }

    public boolean isPermission(PackageParser.Permission permission) {
        return this.perm == permission;
    }

    public boolean isDynamic() {
        return this.type == 2;
    }

    public boolean isNormal() {
        return (this.protectionLevel & 15) == 0;
    }

    public boolean isRuntime() {
        return (this.protectionLevel & 15) == 1;
    }

    public boolean isSignature() {
        return (this.protectionLevel & 15) == 2;
    }

    public boolean isAppOp() {
        return (this.protectionLevel & 64) != 0;
    }

    public boolean isDevelopment() {
        return isSignature() && (this.protectionLevel & 32) != 0;
    }

    public boolean isInstaller() {
        return (this.protectionLevel & 256) != 0;
    }

    public boolean isInstant() {
        return (this.protectionLevel & 4096) != 0;
    }

    public boolean isOEM() {
        return (this.protectionLevel & 16384) != 0;
    }

    public boolean isPre23() {
        return (this.protectionLevel & 128) != 0;
    }

    public boolean isPreInstalled() {
        return (this.protectionLevel & 1024) != 0;
    }

    public boolean isPrivileged() {
        return (this.protectionLevel & 16) != 0;
    }

    public boolean isRuntimeOnly() {
        return (this.protectionLevel & 8192) != 0;
    }

    public boolean isSetup() {
        return (this.protectionLevel & 2048) != 0;
    }

    public boolean isVerifier() {
        return (this.protectionLevel & 512) != 0;
    }

    public boolean isVendorPrivileged() {
        return (this.protectionLevel & 32768) != 0;
    }

    public boolean isSystemTextClassifier() {
        return (this.protectionLevel & 65536) != 0;
    }

    public void transfer(String str, String str2) {
        if (!str.equals(this.sourcePackageName)) {
            return;
        }
        this.sourcePackageName = str2;
        this.sourcePackageSetting = null;
        this.perm = null;
        if (this.pendingPermissionInfo != null) {
            this.pendingPermissionInfo.packageName = str2;
        }
        this.uid = 0;
        setGids(null, false);
    }

    public boolean addToTree(int i, PermissionInfo permissionInfo, BasePermission basePermission) {
        boolean z = (this.protectionLevel == i && this.perm != null && this.uid == basePermission.uid && this.perm.owner.equals(basePermission.perm.owner) && comparePermissionInfos(this.perm.info, permissionInfo)) ? false : true;
        this.protectionLevel = i;
        PermissionInfo permissionInfo2 = new PermissionInfo(permissionInfo);
        permissionInfo2.protectionLevel = i;
        this.perm = new PackageParser.Permission(basePermission.perm.owner, permissionInfo2);
        this.perm.info.packageName = basePermission.perm.info.packageName;
        this.uid = basePermission.uid;
        return z;
    }

    public void updateDynamicPermission(Collection<BasePermission> collection) {
        BasePermission basePermissionFindPermissionTree;
        if (PackageManagerService.DEBUG_SETTINGS) {
            Log.v(TAG, "Dynamic permission: name=" + getName() + " pkg=" + getSourcePackageName() + " info=" + this.pendingPermissionInfo);
        }
        if (this.sourcePackageSetting == null && this.pendingPermissionInfo != null && (basePermissionFindPermissionTree = findPermissionTree(collection, this.name)) != null && basePermissionFindPermissionTree.perm != null) {
            this.sourcePackageSetting = basePermissionFindPermissionTree.sourcePackageSetting;
            this.perm = new PackageParser.Permission(basePermissionFindPermissionTree.perm.owner, new PermissionInfo(this.pendingPermissionInfo));
            this.perm.info.packageName = basePermissionFindPermissionTree.perm.info.packageName;
            this.perm.info.name = this.name;
            this.uid = basePermissionFindPermissionTree.uid;
        }
    }

    static BasePermission createOrUpdate(BasePermission basePermission, PackageParser.Permission permission, PackageParser.Package r11, Collection<BasePermission> collection, boolean z) {
        boolean z2;
        PackageSettingBase packageSettingBase = (PackageSettingBase) r11.mExtras;
        StringBuilder sb = null;
        if (basePermission == null || Objects.equals(basePermission.sourcePackageName, permission.info.packageName)) {
            z2 = false;
        } else {
            boolean z3 = basePermission.perm != null && basePermission.perm.owner.isSystem();
            if (permission.owner.isSystem()) {
                if (basePermission.type == 1 && basePermission.perm == null) {
                    basePermission.sourcePackageSetting = packageSettingBase;
                    basePermission.perm = permission;
                    basePermission.uid = r11.applicationInfo.uid;
                    basePermission.sourcePackageName = permission.info.packageName;
                    permission.info.flags |= 1073741824;
                } else if (!z3) {
                    PackageManagerService.reportSettingsProblem(5, "New decl " + permission.owner + " of permission  " + permission.info.name + " is system; overriding " + basePermission.sourcePackageName);
                    basePermission = null;
                    z2 = true;
                }
                z2 = false;
            }
        }
        if (basePermission == null) {
            basePermission = new BasePermission(permission.info.name, permission.info.packageName, 0);
        }
        boolean z4 = !basePermission.isRuntime();
        if (basePermission.perm == null) {
            if (basePermission.sourcePackageName == null || basePermission.sourcePackageName.equals(permission.info.packageName)) {
                BasePermission basePermissionFindPermissionTree = findPermissionTree(collection, permission.info.name);
                if (basePermissionFindPermissionTree == null || basePermissionFindPermissionTree.sourcePackageName.equals(permission.info.packageName)) {
                    basePermission.sourcePackageSetting = packageSettingBase;
                    basePermission.perm = permission;
                    basePermission.uid = r11.applicationInfo.uid;
                    basePermission.sourcePackageName = permission.info.packageName;
                    permission.info.flags |= 1073741824;
                    if (z) {
                        StringBuilder sb2 = new StringBuilder(256);
                        sb2.append(permission.info.name);
                        sb = sb2;
                    }
                } else {
                    Slog.w(TAG, "Permission " + permission.info.name + " from package " + permission.info.packageName + " ignored: base tree " + basePermissionFindPermissionTree.name + " is from package " + basePermissionFindPermissionTree.sourcePackageName);
                }
            } else {
                Slog.w(TAG, "Permission " + permission.info.name + " from package " + permission.info.packageName + " ignored: original from " + basePermission.sourcePackageName);
            }
        } else if (z) {
            sb = new StringBuilder(256);
            sb.append("DUP:");
            sb.append(permission.info.name);
        }
        if (basePermission.perm == permission) {
            basePermission.protectionLevel = permission.info.protectionLevel;
        }
        if (basePermission.isRuntime() && (z2 || z4)) {
            basePermission.mPermissionDefinitionChanged = true;
        }
        if (PackageManagerService.DEBUG_PACKAGE_SCANNING && sb != null) {
            Log.d(TAG, "  Permissions: " + ((Object) sb));
        }
        return basePermission;
    }

    static BasePermission enforcePermissionTree(Collection<BasePermission> collection, String str, int i) {
        BasePermission basePermissionFindPermissionTree;
        if (str != null && (basePermissionFindPermissionTree = findPermissionTree(collection, str)) != null) {
            if (basePermissionFindPermissionTree.uid == UserHandle.getAppId(i)) {
                return basePermissionFindPermissionTree;
            }
            throw new SecurityException("Calling uid " + i + " is not allowed to add to permission tree " + basePermissionFindPermissionTree.name + " owned by uid " + basePermissionFindPermissionTree.uid);
        }
        throw new SecurityException("No permission tree found for " + str);
    }

    public void enforceDeclaredUsedAndRuntimeOrDevelopment(PackageParser.Package r4) {
        if (r4.requestedPermissions.indexOf(this.name) == -1) {
            throw new SecurityException("Package " + r4.packageName + " has not requested permission " + this.name);
        }
        if (!isRuntime() && !isDevelopment()) {
            throw new SecurityException("Permission " + this.name + " is not a changeable permission type");
        }
    }

    private static BasePermission findPermissionTree(Collection<BasePermission> collection, String str) {
        for (BasePermission basePermission : collection) {
            if (str.startsWith(basePermission.name) && str.length() > basePermission.name.length() && str.charAt(basePermission.name.length()) == '.') {
                return basePermission;
            }
        }
        return null;
    }

    public PermissionInfo generatePermissionInfo(String str, int i) {
        if (str == null) {
            if (this.perm == null || this.perm.info.group == null) {
                return generatePermissionInfo(this.protectionLevel, i);
            }
            return null;
        }
        if (this.perm != null && str.equals(this.perm.info.group)) {
            return PackageParser.generatePermissionInfo(this.perm, i);
        }
        return null;
    }

    public PermissionInfo generatePermissionInfo(int i, int i2) {
        if (this.perm != null) {
            boolean z = this.protectionLevel != i;
            PermissionInfo permissionInfoGeneratePermissionInfo = PackageParser.generatePermissionInfo(this.perm, i2);
            if (!z || permissionInfoGeneratePermissionInfo != this.perm.info) {
                return permissionInfoGeneratePermissionInfo;
            }
            PermissionInfo permissionInfo = new PermissionInfo(permissionInfoGeneratePermissionInfo);
            permissionInfo.protectionLevel = i;
            return permissionInfo;
        }
        PermissionInfo permissionInfo2 = new PermissionInfo();
        permissionInfo2.name = this.name;
        permissionInfo2.packageName = this.sourcePackageName;
        permissionInfo2.nonLocalizedLabel = this.name;
        permissionInfo2.protectionLevel = this.protectionLevel;
        return permissionInfo2;
    }

    public static boolean readLPw(Map<String, BasePermission> map, XmlPullParser xmlPullParser) {
        if (!xmlPullParser.getName().equals(Settings.TAG_ITEM)) {
            return false;
        }
        String attributeValue = xmlPullParser.getAttributeValue(null, Settings.ATTR_NAME);
        String attributeValue2 = xmlPullParser.getAttributeValue(null, Settings.ATTR_PACKAGE);
        String attributeValue3 = xmlPullParser.getAttributeValue(null, DatabaseHelper.SoundModelContract.KEY_TYPE);
        if (attributeValue == null || attributeValue2 == null) {
            PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: permissions has no name at " + xmlPullParser.getPositionDescription());
            return false;
        }
        boolean zEquals = "dynamic".equals(attributeValue3);
        BasePermission basePermission = map.get(attributeValue);
        if (basePermission == null || basePermission.type != 1) {
            basePermission = new BasePermission(attributeValue.intern(), attributeValue2, zEquals ? 2 : 0);
        }
        basePermission.protectionLevel = readInt(xmlPullParser, null, "protection", 0);
        basePermission.protectionLevel = PermissionInfo.fixProtectionLevel(basePermission.protectionLevel);
        if (zEquals) {
            PermissionInfo permissionInfo = new PermissionInfo();
            permissionInfo.packageName = attributeValue2.intern();
            permissionInfo.name = attributeValue.intern();
            permissionInfo.icon = readInt(xmlPullParser, null, "icon", 0);
            permissionInfo.nonLocalizedLabel = xmlPullParser.getAttributeValue(null, "label");
            permissionInfo.protectionLevel = basePermission.protectionLevel;
            basePermission.pendingPermissionInfo = permissionInfo;
        }
        map.put(basePermission.name, basePermission);
        return true;
    }

    private static int readInt(XmlPullParser xmlPullParser, String str, String str2, int i) {
        String attributeValue = xmlPullParser.getAttributeValue(str, str2);
        if (attributeValue == null) {
            return i;
        }
        try {
            return Integer.parseInt(attributeValue);
        } catch (NumberFormatException e) {
            PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: attribute " + str2 + " has bad integer value " + attributeValue + " at " + xmlPullParser.getPositionDescription());
            return i;
        }
    }

    public void writeLPr(XmlSerializer xmlSerializer) throws IOException {
        if (this.sourcePackageName == null) {
            return;
        }
        xmlSerializer.startTag(null, Settings.TAG_ITEM);
        xmlSerializer.attribute(null, Settings.ATTR_NAME, this.name);
        xmlSerializer.attribute(null, Settings.ATTR_PACKAGE, this.sourcePackageName);
        if (this.protectionLevel != 0) {
            xmlSerializer.attribute(null, "protection", Integer.toString(this.protectionLevel));
        }
        if (this.type == 2) {
            PermissionInfo permissionInfo = this.perm != null ? this.perm.info : this.pendingPermissionInfo;
            if (permissionInfo != null) {
                xmlSerializer.attribute(null, DatabaseHelper.SoundModelContract.KEY_TYPE, "dynamic");
                if (permissionInfo.icon != 0) {
                    xmlSerializer.attribute(null, "icon", Integer.toString(permissionInfo.icon));
                }
                if (permissionInfo.nonLocalizedLabel != null) {
                    xmlSerializer.attribute(null, "label", permissionInfo.nonLocalizedLabel.toString());
                }
            }
        }
        xmlSerializer.endTag(null, Settings.TAG_ITEM);
    }

    private static boolean compareStrings(CharSequence charSequence, CharSequence charSequence2) {
        if (charSequence == null) {
            return charSequence2 == null;
        }
        if (charSequence2 == null || charSequence.getClass() != charSequence2.getClass()) {
            return false;
        }
        return charSequence.equals(charSequence2);
    }

    private static boolean comparePermissionInfos(PermissionInfo permissionInfo, PermissionInfo permissionInfo2) {
        return permissionInfo.icon == permissionInfo2.icon && permissionInfo.logo == permissionInfo2.logo && permissionInfo.protectionLevel == permissionInfo2.protectionLevel && compareStrings(permissionInfo.name, permissionInfo2.name) && compareStrings(permissionInfo.nonLocalizedLabel, permissionInfo2.nonLocalizedLabel) && compareStrings(permissionInfo.packageName, permissionInfo2.packageName);
    }

    public boolean dumpPermissionsLPr(PrintWriter printWriter, String str, Set<String> set, boolean z, boolean z2, DumpState dumpState) {
        if (str != null && !str.equals(this.sourcePackageName)) {
            return false;
        }
        if (set != null && !set.contains(this.name)) {
            return false;
        }
        if (!z2) {
            if (dumpState.onTitlePrinted()) {
                printWriter.println();
            }
            printWriter.println("Permissions:");
        }
        printWriter.print("  Permission [");
        printWriter.print(this.name);
        printWriter.print("] (");
        printWriter.print(Integer.toHexString(System.identityHashCode(this)));
        printWriter.println("):");
        printWriter.print("    sourcePackage=");
        printWriter.println(this.sourcePackageName);
        printWriter.print("    uid=");
        printWriter.print(this.uid);
        printWriter.print(" gids=");
        printWriter.print(Arrays.toString(computeGids(0)));
        printWriter.print(" type=");
        printWriter.print(this.type);
        printWriter.print(" prot=");
        printWriter.println(PermissionInfo.protectionToString(this.protectionLevel));
        if (this.perm != null) {
            printWriter.print("    perm=");
            printWriter.println(this.perm);
            if ((this.perm.info.flags & 1073741824) == 0 || (this.perm.info.flags & 2) != 0) {
                printWriter.print("    flags=0x");
                printWriter.println(Integer.toHexString(this.perm.info.flags));
            }
        }
        if (this.sourcePackageSetting != null) {
            printWriter.print("    packageSetting=");
            printWriter.println(this.sourcePackageSetting);
        }
        if ("android.permission.READ_EXTERNAL_STORAGE".equals(this.name)) {
            printWriter.print("    enforced=");
            printWriter.println(z);
            return true;
        }
        return true;
    }
}
