package com.android.server.pm;

import android.content.pm.IntentFilterVerificationInfo;
import android.content.pm.PackageParser;
import android.content.pm.PackageUserState;
import android.content.pm.Signature;
import android.os.PersistableBundle;
import android.util.ArraySet;
import android.util.SparseArray;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.pm.permission.PermissionsState;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public abstract class PackageSettingBase extends SettingBase {
    int categoryHint;
    List<String> childPackageNames;
    File codePath;
    String codePathString;
    String cpuAbiOverrideString;
    long firstInstallTime;
    boolean installPermissionsFixed;
    String installerPackageName;
    boolean isOrphaned;
    PackageKeySetData keySetData;
    long lastUpdateTime;

    @Deprecated
    String legacyNativeLibraryPathString;
    public final String name;
    Set<String> oldCodePaths;
    String parentPackageName;
    String primaryCpuAbiString;
    final String realName;
    File resourcePath;
    String resourcePathString;
    String secondaryCpuAbiString;
    PackageSignatures signatures;
    long timeStamp;
    boolean uidError;
    boolean updateAvailable;
    private final SparseArray<PackageUserState> userState;
    String[] usesStaticLibraries;
    long[] usesStaticLibrariesVersions;
    IntentFilterVerificationInfo verificationInfo;
    long versionCode;
    String volumeUuid;
    private static final int[] EMPTY_INT_ARRAY = new int[0];
    static final PackageUserState DEFAULT_USER_STATE = new PackageUserState();

    @Override
    public void copyFrom(SettingBase settingBase) {
        super.copyFrom(settingBase);
    }

    @Override
    public PermissionsState getPermissionsState() {
        return super.getPermissionsState();
    }

    PackageSettingBase(String str, String str2, File file, File file2, String str3, String str4, String str5, String str6, long j, int i, int i2, String str7, List<String> list, String[] strArr, long[] jArr) {
        super(i, i2);
        this.keySetData = new PackageKeySetData();
        this.userState = new SparseArray<>();
        this.categoryHint = -1;
        this.name = str;
        this.realName = str2;
        this.parentPackageName = str7;
        this.childPackageNames = list != null ? new ArrayList(list) : null;
        this.usesStaticLibraries = strArr;
        this.usesStaticLibrariesVersions = jArr;
        init(file, file2, str3, str4, str5, str6, j);
    }

    PackageSettingBase(PackageSettingBase packageSettingBase, String str) {
        super(packageSettingBase);
        this.keySetData = new PackageKeySetData();
        this.userState = new SparseArray<>();
        this.categoryHint = -1;
        this.name = packageSettingBase.name;
        this.realName = str;
        doCopy(packageSettingBase);
    }

    void init(File file, File file2, String str, String str2, String str3, String str4, long j) {
        this.codePath = file;
        this.codePathString = file.toString();
        this.resourcePath = file2;
        this.resourcePathString = file2.toString();
        this.legacyNativeLibraryPathString = str;
        this.primaryCpuAbiString = str2;
        this.secondaryCpuAbiString = str3;
        this.cpuAbiOverrideString = str4;
        this.versionCode = j;
        this.signatures = new PackageSignatures();
    }

    public void setInstallerPackageName(String str) {
        this.installerPackageName = str;
    }

    public String getInstallerPackageName() {
        return this.installerPackageName;
    }

    public void setVolumeUuid(String str) {
        this.volumeUuid = str;
    }

    public String getVolumeUuid() {
        return this.volumeUuid;
    }

    public void setTimeStamp(long j) {
        this.timeStamp = j;
    }

    public void setUpdateAvailable(boolean z) {
        this.updateAvailable = z;
    }

    public boolean isUpdateAvailable() {
        return this.updateAvailable;
    }

    public boolean isSharedUser() {
        return false;
    }

    public Signature[] getSignatures() {
        return this.signatures.mSigningDetails.signatures;
    }

    public PackageParser.SigningDetails getSigningDetails() {
        return this.signatures.mSigningDetails;
    }

    public void copyFrom(PackageSettingBase packageSettingBase) {
        super.copyFrom((SettingBase) packageSettingBase);
        doCopy(packageSettingBase);
    }

    private void doCopy(PackageSettingBase packageSettingBase) {
        String[] strArr;
        this.childPackageNames = packageSettingBase.childPackageNames != null ? new ArrayList(packageSettingBase.childPackageNames) : null;
        this.codePath = packageSettingBase.codePath;
        this.codePathString = packageSettingBase.codePathString;
        this.cpuAbiOverrideString = packageSettingBase.cpuAbiOverrideString;
        this.firstInstallTime = packageSettingBase.firstInstallTime;
        this.installPermissionsFixed = packageSettingBase.installPermissionsFixed;
        this.installerPackageName = packageSettingBase.installerPackageName;
        this.isOrphaned = packageSettingBase.isOrphaned;
        this.keySetData = packageSettingBase.keySetData;
        this.lastUpdateTime = packageSettingBase.lastUpdateTime;
        this.legacyNativeLibraryPathString = packageSettingBase.legacyNativeLibraryPathString;
        this.parentPackageName = packageSettingBase.parentPackageName;
        this.primaryCpuAbiString = packageSettingBase.primaryCpuAbiString;
        this.resourcePath = packageSettingBase.resourcePath;
        this.resourcePathString = packageSettingBase.resourcePathString;
        this.secondaryCpuAbiString = packageSettingBase.secondaryCpuAbiString;
        this.signatures = packageSettingBase.signatures;
        this.timeStamp = packageSettingBase.timeStamp;
        this.uidError = packageSettingBase.uidError;
        this.userState.clear();
        for (int i = 0; i < packageSettingBase.userState.size(); i++) {
            this.userState.put(packageSettingBase.userState.keyAt(i), packageSettingBase.userState.valueAt(i));
        }
        this.verificationInfo = packageSettingBase.verificationInfo;
        this.versionCode = packageSettingBase.versionCode;
        this.volumeUuid = packageSettingBase.volumeUuid;
        this.categoryHint = packageSettingBase.categoryHint;
        if (packageSettingBase.usesStaticLibraries != null) {
            strArr = (String[]) Arrays.copyOf(packageSettingBase.usesStaticLibraries, packageSettingBase.usesStaticLibraries.length);
        } else {
            strArr = null;
        }
        this.usesStaticLibraries = strArr;
        this.usesStaticLibrariesVersions = packageSettingBase.usesStaticLibrariesVersions != null ? Arrays.copyOf(packageSettingBase.usesStaticLibrariesVersions, packageSettingBase.usesStaticLibrariesVersions.length) : null;
        this.updateAvailable = packageSettingBase.updateAvailable;
    }

    private PackageUserState modifyUserState(int i) {
        PackageUserState packageUserState = this.userState.get(i);
        if (packageUserState == null) {
            PackageUserState packageUserState2 = new PackageUserState();
            this.userState.put(i, packageUserState2);
            return packageUserState2;
        }
        return packageUserState;
    }

    public PackageUserState readUserState(int i) {
        PackageUserState packageUserState = this.userState.get(i);
        if (packageUserState == null) {
            return DEFAULT_USER_STATE;
        }
        packageUserState.categoryHint = this.categoryHint;
        return packageUserState;
    }

    public void setEnabled(int i, int i2, String str) {
        PackageUserState packageUserStateModifyUserState = modifyUserState(i2);
        packageUserStateModifyUserState.enabled = i;
        packageUserStateModifyUserState.lastDisableAppCaller = str;
    }

    int getEnabled(int i) {
        return readUserState(i).enabled;
    }

    String getLastDisabledAppCaller(int i) {
        return readUserState(i).lastDisableAppCaller;
    }

    public void setInstalled(boolean z, int i) {
        modifyUserState(i).installed = z;
    }

    boolean getInstalled(int i) {
        return readUserState(i).installed;
    }

    int getInstallReason(int i) {
        return readUserState(i).installReason;
    }

    void setInstallReason(int i, int i2) {
        modifyUserState(i2).installReason = i;
    }

    void setOverlayPaths(List<String> list, int i) {
        modifyUserState(i).overlayPaths = list == null ? null : (String[]) list.toArray(new String[list.size()]);
    }

    String[] getOverlayPaths(int i) {
        return readUserState(i).overlayPaths;
    }

    @VisibleForTesting
    SparseArray<PackageUserState> getUserState() {
        return this.userState;
    }

    boolean isAnyInstalled(int[] iArr) {
        for (int i : iArr) {
            if (readUserState(i).installed) {
                return true;
            }
        }
        return false;
    }

    public int[] queryInstalledUsers(int[] iArr, boolean z) {
        int i = 0;
        for (int i2 : iArr) {
            if (getInstalled(i2) == z) {
                i++;
            }
        }
        int[] iArr2 = new int[i];
        int i3 = 0;
        for (int i4 : iArr) {
            if (getInstalled(i4) == z) {
                iArr2[i3] = i4;
                i3++;
            }
        }
        return iArr2;
    }

    long getCeDataInode(int i) {
        return readUserState(i).ceDataInode;
    }

    void setCeDataInode(long j, int i) {
        modifyUserState(i).ceDataInode = j;
    }

    boolean getStopped(int i) {
        return readUserState(i).stopped;
    }

    void setStopped(boolean z, int i) {
        modifyUserState(i).stopped = z;
    }

    boolean getNotLaunched(int i) {
        return readUserState(i).notLaunched;
    }

    void setNotLaunched(boolean z, int i) {
        modifyUserState(i).notLaunched = z;
    }

    boolean getHidden(int i) {
        return readUserState(i).hidden;
    }

    void setHidden(boolean z, int i) {
        modifyUserState(i).hidden = z;
    }

    boolean getSuspended(int i) {
        return readUserState(i).suspended;
    }

    void setSuspended(boolean z, String str, String str2, PersistableBundle persistableBundle, PersistableBundle persistableBundle2, int i) {
        PackageUserState packageUserStateModifyUserState = modifyUserState(i);
        packageUserStateModifyUserState.suspended = z;
        if (!z) {
            str = null;
        }
        packageUserStateModifyUserState.suspendingPackage = str;
        if (!z) {
            str2 = null;
        }
        packageUserStateModifyUserState.dialogMessage = str2;
        if (!z) {
            persistableBundle = null;
        }
        packageUserStateModifyUserState.suspendedAppExtras = persistableBundle;
        if (!z) {
            persistableBundle2 = null;
        }
        packageUserStateModifyUserState.suspendedLauncherExtras = persistableBundle2;
    }

    public boolean getInstantApp(int i) {
        return readUserState(i).instantApp;
    }

    void setInstantApp(boolean z, int i) {
        modifyUserState(i).instantApp = z;
    }

    boolean getVirtulalPreload(int i) {
        return readUserState(i).virtualPreload;
    }

    void setVirtualPreload(boolean z, int i) {
        modifyUserState(i).virtualPreload = z;
    }

    void setUserState(int i, long j, int i2, boolean z, boolean z2, boolean z3, boolean z4, boolean z5, String str, String str2, PersistableBundle persistableBundle, PersistableBundle persistableBundle2, boolean z6, boolean z7, String str3, ArraySet<String> arraySet, ArraySet<String> arraySet2, int i3, int i4, int i5, String str4) {
        PackageUserState packageUserStateModifyUserState = modifyUserState(i);
        packageUserStateModifyUserState.ceDataInode = j;
        packageUserStateModifyUserState.enabled = i2;
        packageUserStateModifyUserState.installed = z;
        packageUserStateModifyUserState.stopped = z2;
        packageUserStateModifyUserState.notLaunched = z3;
        packageUserStateModifyUserState.hidden = z4;
        packageUserStateModifyUserState.suspended = z5;
        packageUserStateModifyUserState.suspendingPackage = str;
        packageUserStateModifyUserState.dialogMessage = str2;
        packageUserStateModifyUserState.suspendedAppExtras = persistableBundle;
        packageUserStateModifyUserState.suspendedLauncherExtras = persistableBundle2;
        packageUserStateModifyUserState.lastDisableAppCaller = str3;
        packageUserStateModifyUserState.enabledComponents = arraySet;
        packageUserStateModifyUserState.disabledComponents = arraySet2;
        packageUserStateModifyUserState.domainVerificationStatus = i3;
        packageUserStateModifyUserState.appLinkGeneration = i4;
        packageUserStateModifyUserState.installReason = i5;
        packageUserStateModifyUserState.instantApp = z6;
        packageUserStateModifyUserState.virtualPreload = z7;
        packageUserStateModifyUserState.harmfulAppWarning = str4;
    }

    ArraySet<String> getEnabledComponents(int i) {
        return readUserState(i).enabledComponents;
    }

    ArraySet<String> getDisabledComponents(int i) {
        return readUserState(i).disabledComponents;
    }

    void setEnabledComponents(ArraySet<String> arraySet, int i) {
        modifyUserState(i).enabledComponents = arraySet;
    }

    void setDisabledComponents(ArraySet<String> arraySet, int i) {
        modifyUserState(i).disabledComponents = arraySet;
    }

    void setEnabledComponentsCopy(ArraySet<String> arraySet, int i) {
        modifyUserState(i).enabledComponents = arraySet != null ? new ArraySet((ArraySet) arraySet) : null;
    }

    void setDisabledComponentsCopy(ArraySet<String> arraySet, int i) {
        modifyUserState(i).disabledComponents = arraySet != null ? new ArraySet((ArraySet) arraySet) : null;
    }

    PackageUserState modifyUserStateComponents(int i, boolean z, boolean z2) {
        PackageUserState packageUserStateModifyUserState = modifyUserState(i);
        if (z && packageUserStateModifyUserState.disabledComponents == null) {
            packageUserStateModifyUserState.disabledComponents = new ArraySet(1);
        }
        if (z2 && packageUserStateModifyUserState.enabledComponents == null) {
            packageUserStateModifyUserState.enabledComponents = new ArraySet(1);
        }
        return packageUserStateModifyUserState;
    }

    void addDisabledComponent(String str, int i) {
        modifyUserStateComponents(i, true, false).disabledComponents.add(str);
    }

    void addEnabledComponent(String str, int i) {
        modifyUserStateComponents(i, false, true).enabledComponents.add(str);
    }

    boolean enableComponentLPw(String str, int i) {
        boolean zRemove = false;
        PackageUserState packageUserStateModifyUserStateComponents = modifyUserStateComponents(i, false, true);
        if (packageUserStateModifyUserStateComponents.disabledComponents != null) {
            zRemove = packageUserStateModifyUserStateComponents.disabledComponents.remove(str);
        }
        return packageUserStateModifyUserStateComponents.enabledComponents.add(str) | zRemove;
    }

    boolean disableComponentLPw(String str, int i) {
        boolean zRemove = false;
        PackageUserState packageUserStateModifyUserStateComponents = modifyUserStateComponents(i, true, false);
        if (packageUserStateModifyUserStateComponents.enabledComponents != null) {
            zRemove = packageUserStateModifyUserStateComponents.enabledComponents.remove(str);
        }
        return packageUserStateModifyUserStateComponents.disabledComponents.add(str) | zRemove;
    }

    boolean restoreComponentLPw(String str, int i) {
        boolean zRemove;
        PackageUserState packageUserStateModifyUserStateComponents = modifyUserStateComponents(i, true, true);
        if (packageUserStateModifyUserStateComponents.disabledComponents != null) {
            zRemove = packageUserStateModifyUserStateComponents.disabledComponents.remove(str);
        } else {
            zRemove = false;
        }
        return zRemove | (packageUserStateModifyUserStateComponents.enabledComponents != null ? packageUserStateModifyUserStateComponents.enabledComponents.remove(str) : false);
    }

    int getCurrentEnabledStateLPr(String str, int i) {
        PackageUserState userState = readUserState(i);
        if (userState.enabledComponents != null && userState.enabledComponents.contains(str)) {
            return 1;
        }
        if (userState.disabledComponents != null && userState.disabledComponents.contains(str)) {
            return 2;
        }
        return 0;
    }

    void removeUser(int i) {
        this.userState.delete(i);
    }

    public int[] getNotInstalledUserIds() {
        int size = this.userState.size();
        int i = 0;
        for (int i2 = 0; i2 < size; i2++) {
            if (!this.userState.valueAt(i2).installed) {
                i++;
            }
        }
        if (i == 0) {
            return EMPTY_INT_ARRAY;
        }
        int[] iArr = new int[i];
        int i3 = 0;
        for (int i4 = 0; i4 < size; i4++) {
            if (!this.userState.valueAt(i4).installed) {
                iArr[i3] = this.userState.keyAt(i4);
                i3++;
            }
        }
        return iArr;
    }

    IntentFilterVerificationInfo getIntentFilterVerificationInfo() {
        return this.verificationInfo;
    }

    void setIntentFilterVerificationInfo(IntentFilterVerificationInfo intentFilterVerificationInfo) {
        this.verificationInfo = intentFilterVerificationInfo;
    }

    long getDomainVerificationStatusForUser(int i) {
        PackageUserState userState = readUserState(i);
        return ((long) userState.appLinkGeneration) | (((long) userState.domainVerificationStatus) << 32);
    }

    void setDomainVerificationStatusForUser(int i, int i2, int i3) {
        PackageUserState packageUserStateModifyUserState = modifyUserState(i3);
        packageUserStateModifyUserState.domainVerificationStatus = i;
        if (i == 2) {
            packageUserStateModifyUserState.appLinkGeneration = i2;
        }
    }

    void clearDomainVerificationStatusForUser(int i) {
        modifyUserState(i).domainVerificationStatus = 0;
    }

    protected void writeUsersInfoToProto(ProtoOutputStream protoOutputStream, long j) {
        int i;
        int size = this.userState.size();
        for (int i2 = 0; i2 < size; i2++) {
            long jStart = protoOutputStream.start(j);
            int iKeyAt = this.userState.keyAt(i2);
            PackageUserState packageUserStateValueAt = this.userState.valueAt(i2);
            protoOutputStream.write(1120986464257L, iKeyAt);
            if (packageUserStateValueAt.instantApp) {
                i = 2;
            } else {
                i = packageUserStateValueAt.installed ? 1 : 0;
            }
            protoOutputStream.write(1159641169922L, i);
            protoOutputStream.write(1133871366147L, packageUserStateValueAt.hidden);
            protoOutputStream.write(1133871366148L, packageUserStateValueAt.suspended);
            if (packageUserStateValueAt.suspended) {
                protoOutputStream.write(1138166333449L, packageUserStateValueAt.suspendingPackage);
            }
            protoOutputStream.write(1133871366149L, packageUserStateValueAt.stopped);
            protoOutputStream.write(1133871366150L, !packageUserStateValueAt.notLaunched);
            protoOutputStream.write(1159641169927L, packageUserStateValueAt.enabled);
            protoOutputStream.write(1138166333448L, packageUserStateValueAt.lastDisableAppCaller);
            protoOutputStream.end(jStart);
        }
    }

    void setHarmfulAppWarning(int i, String str) {
        modifyUserState(i).harmfulAppWarning = str;
    }

    String getHarmfulAppWarning(int i) {
        return readUserState(i).harmfulAppWarning;
    }
}
