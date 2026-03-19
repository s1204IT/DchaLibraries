package com.android.server.pm;

import android.content.pm.PackageParser;
import android.content.pm.UserInfo;
import android.util.proto.ProtoOutputStream;
import com.android.server.pm.permission.PermissionsState;
import com.android.server.slice.SliceClientPermissions;
import java.io.File;
import java.util.List;

public final class PackageSetting extends PackageSettingBase {
    int appId;
    PackageParser.Package pkg;
    SharedUserSetting sharedUser;
    private int sharedUserId;

    PackageSetting(String str, String str2, File file, File file2, String str3, String str4, String str5, String str6, long j, int i, int i2, String str7, List<String> list, int i3, String[] strArr, long[] jArr) {
        super(str, str2, file, file2, str3, str4, str5, str6, j, i, i2, str7, list, strArr, jArr);
        this.sharedUserId = i3;
    }

    PackageSetting(PackageSetting packageSetting) {
        super(packageSetting, packageSetting.realName);
        doCopy(packageSetting);
    }

    PackageSetting(PackageSetting packageSetting, String str) {
        super(packageSetting, str);
        doCopy(packageSetting);
    }

    public int getSharedUserId() {
        if (this.sharedUser != null) {
            return this.sharedUser.userId;
        }
        return this.sharedUserId;
    }

    public SharedUserSetting getSharedUser() {
        return this.sharedUser;
    }

    public String toString() {
        return "PackageSetting{" + Integer.toHexString(System.identityHashCode(this)) + " " + this.name + SliceClientPermissions.SliceAuthority.DELIMITER + this.appId + "}";
    }

    public void copyFrom(PackageSetting packageSetting) {
        super.copyFrom((PackageSettingBase) packageSetting);
        doCopy(packageSetting);
    }

    private void doCopy(PackageSetting packageSetting) {
        this.appId = packageSetting.appId;
        this.pkg = packageSetting.pkg;
        this.sharedUser = packageSetting.sharedUser;
        this.sharedUserId = packageSetting.sharedUserId;
    }

    @Override
    public PermissionsState getPermissionsState() {
        if (this.sharedUser != null) {
            return this.sharedUser.getPermissionsState();
        }
        return super.getPermissionsState();
    }

    public PackageParser.Package getPackage() {
        return this.pkg;
    }

    public int getAppId() {
        return this.appId;
    }

    public void setInstallPermissionsFixed(boolean z) {
        this.installPermissionsFixed = z;
    }

    public boolean areInstallPermissionsFixed() {
        return this.installPermissionsFixed;
    }

    public boolean isPrivileged() {
        return (this.pkgPrivateFlags & 8) != 0;
    }

    public boolean isOem() {
        return (this.pkgPrivateFlags & DumpState.DUMP_INTENT_FILTER_VERIFIERS) != 0;
    }

    public boolean isVendor() {
        return (this.pkgPrivateFlags & DumpState.DUMP_DOMAIN_PREFERRED) != 0;
    }

    public boolean isProduct() {
        return (this.pkgPrivateFlags & DumpState.DUMP_FROZEN) != 0;
    }

    public boolean isForwardLocked() {
        return (this.pkgPrivateFlags & 4) != 0;
    }

    public boolean isSystem() {
        return (this.pkgFlags & 1) != 0;
    }

    public boolean isUpdatedSystem() {
        return (this.pkgFlags & 128) != 0;
    }

    @Override
    public boolean isSharedUser() {
        return this.sharedUser != null;
    }

    public boolean isMatch(int i) {
        if ((i & DumpState.DUMP_DEXOPT) != 0) {
            return isSystem();
        }
        return true;
    }

    public boolean hasChildPackages() {
        return (this.childPackageNames == null || this.childPackageNames.isEmpty()) ? false : true;
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j, List<UserInfo> list) {
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1138166333441L, this.realName != null ? this.realName : this.name);
        protoOutputStream.write(1120986464258L, this.appId);
        protoOutputStream.write(1120986464259L, this.versionCode);
        protoOutputStream.write(1138166333444L, this.pkg.mVersionName);
        protoOutputStream.write(1112396529669L, this.firstInstallTime);
        protoOutputStream.write(1112396529670L, this.lastUpdateTime);
        protoOutputStream.write(1138166333447L, this.installerPackageName);
        if (this.pkg != null) {
            long jStart2 = protoOutputStream.start(2246267895816L);
            protoOutputStream.write(1138166333441L, "base");
            protoOutputStream.write(1120986464258L, this.pkg.baseRevisionCode);
            protoOutputStream.end(jStart2);
            if (this.pkg.splitNames != null) {
                for (int i = 0; i < this.pkg.splitNames.length; i++) {
                    long jStart3 = protoOutputStream.start(2246267895816L);
                    protoOutputStream.write(1138166333441L, this.pkg.splitNames[i]);
                    protoOutputStream.write(1120986464258L, this.pkg.splitRevisionCodes[i]);
                    protoOutputStream.end(jStart3);
                }
            }
        }
        writeUsersInfoToProto(protoOutputStream, 2246267895817L);
        protoOutputStream.end(jStart);
    }
}
