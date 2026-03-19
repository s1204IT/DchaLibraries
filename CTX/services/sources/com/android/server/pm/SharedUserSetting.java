package com.android.server.pm;

import android.content.pm.PackageParser;
import android.util.ArraySet;
import android.util.proto.ProtoOutputStream;
import com.android.server.pm.permission.PermissionsState;
import com.android.server.slice.SliceClientPermissions;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class SharedUserSetting extends SettingBase {
    final String name;
    final ArraySet<PackageSetting> packages;
    int seInfoTargetSdkVersion;
    final PackageSignatures signatures;
    Boolean signaturesChanged;
    int uidFlags;
    int uidPrivateFlags;
    int userId;

    @Override
    public void copyFrom(SettingBase settingBase) {
        super.copyFrom(settingBase);
    }

    @Override
    public PermissionsState getPermissionsState() {
        return super.getPermissionsState();
    }

    SharedUserSetting(String str, int i, int i2) {
        super(i, i2);
        this.packages = new ArraySet<>();
        this.signatures = new PackageSignatures();
        this.uidFlags = i;
        this.uidPrivateFlags = i2;
        this.name = str;
        this.seInfoTargetSdkVersion = 10000;
    }

    public String toString() {
        return "SharedUserSetting{" + Integer.toHexString(System.identityHashCode(this)) + " " + this.name + SliceClientPermissions.SliceAuthority.DELIMITER + this.userId + "}";
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1120986464257L, this.userId);
        protoOutputStream.write(1138166333442L, this.name);
        protoOutputStream.end(jStart);
    }

    void removePackage(PackageSetting packageSetting) {
        if (this.packages.remove(packageSetting)) {
            if ((this.pkgFlags & packageSetting.pkgFlags) != 0) {
                int i = this.uidFlags;
                Iterator<PackageSetting> it = this.packages.iterator();
                while (it.hasNext()) {
                    i |= it.next().pkgFlags;
                }
                setFlags(i);
            }
            if ((packageSetting.pkgPrivateFlags & this.pkgPrivateFlags) != 0) {
                int i2 = this.uidPrivateFlags;
                Iterator<PackageSetting> it2 = this.packages.iterator();
                while (it2.hasNext()) {
                    i2 |= it2.next().pkgPrivateFlags;
                }
                setPrivateFlags(i2);
            }
        }
    }

    void addPackage(PackageSetting packageSetting) {
        if (this.packages.size() == 0 && packageSetting.pkg != null) {
            this.seInfoTargetSdkVersion = packageSetting.pkg.applicationInfo.targetSdkVersion;
        }
        if (this.packages.add(packageSetting)) {
            setFlags(this.pkgFlags | packageSetting.pkgFlags);
            setPrivateFlags(packageSetting.pkgPrivateFlags | this.pkgPrivateFlags);
        }
    }

    public List<PackageParser.Package> getPackages() {
        if (this.packages == null || this.packages.size() == 0) {
            return null;
        }
        ArrayList arrayList = new ArrayList(this.packages.size());
        for (PackageSetting packageSetting : this.packages) {
            if (packageSetting != null && packageSetting.pkg != null) {
                arrayList.add(packageSetting.pkg);
            }
        }
        return arrayList;
    }

    public boolean isPrivileged() {
        return (this.pkgPrivateFlags & 8) != 0;
    }

    public void fixSeInfoLocked() {
        List<PackageParser.Package> packages = getPackages();
        if (packages == null || packages.size() == 0) {
            return;
        }
        for (PackageParser.Package r2 : packages) {
            if (r2.applicationInfo.targetSdkVersion < this.seInfoTargetSdkVersion) {
                this.seInfoTargetSdkVersion = r2.applicationInfo.targetSdkVersion;
            }
        }
        for (PackageParser.Package r1 : packages) {
            boolean zIsPrivileged = isPrivileged() | r1.isPrivileged();
            r1.applicationInfo.seInfo = SELinuxMMAC.getSeInfo(r1, zIsPrivileged, r1.applicationInfo.targetSandboxVersion, this.seInfoTargetSdkVersion);
        }
    }
}
