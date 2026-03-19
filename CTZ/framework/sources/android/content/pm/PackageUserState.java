package android.content.pm;

import android.os.BaseBundle;
import android.os.PersistableBundle;
import android.util.ArraySet;
import com.android.internal.util.ArrayUtils;
import java.util.Arrays;
import java.util.Objects;

public class PackageUserState {
    public int appLinkGeneration;
    public int categoryHint;
    public long ceDataInode;
    public String dialogMessage;
    public ArraySet<String> disabledComponents;
    public int domainVerificationStatus;
    public int enabled;
    public ArraySet<String> enabledComponents;
    public String harmfulAppWarning;
    public boolean hidden;
    public int installReason;
    public boolean installed;
    public boolean instantApp;
    public String lastDisableAppCaller;
    public boolean notLaunched;
    public String[] overlayPaths;
    public boolean stopped;
    public boolean suspended;
    public PersistableBundle suspendedAppExtras;
    public PersistableBundle suspendedLauncherExtras;
    public String suspendingPackage;
    public boolean virtualPreload;

    public PackageUserState() {
        this.categoryHint = -1;
        this.installed = true;
        this.hidden = false;
        this.suspended = false;
        this.enabled = 0;
        this.domainVerificationStatus = 0;
        this.installReason = 0;
    }

    public PackageUserState(PackageUserState packageUserState) {
        this.categoryHint = -1;
        this.ceDataInode = packageUserState.ceDataInode;
        this.installed = packageUserState.installed;
        this.stopped = packageUserState.stopped;
        this.notLaunched = packageUserState.notLaunched;
        this.hidden = packageUserState.hidden;
        this.suspended = packageUserState.suspended;
        this.suspendingPackage = packageUserState.suspendingPackage;
        this.dialogMessage = packageUserState.dialogMessage;
        this.suspendedAppExtras = packageUserState.suspendedAppExtras;
        this.suspendedLauncherExtras = packageUserState.suspendedLauncherExtras;
        this.instantApp = packageUserState.instantApp;
        this.virtualPreload = packageUserState.virtualPreload;
        this.enabled = packageUserState.enabled;
        this.lastDisableAppCaller = packageUserState.lastDisableAppCaller;
        this.domainVerificationStatus = packageUserState.domainVerificationStatus;
        this.appLinkGeneration = packageUserState.appLinkGeneration;
        this.categoryHint = packageUserState.categoryHint;
        this.installReason = packageUserState.installReason;
        this.disabledComponents = ArrayUtils.cloneOrNull(packageUserState.disabledComponents);
        this.enabledComponents = ArrayUtils.cloneOrNull(packageUserState.enabledComponents);
        this.overlayPaths = packageUserState.overlayPaths == null ? null : (String[]) Arrays.copyOf(packageUserState.overlayPaths, packageUserState.overlayPaths.length);
        this.harmfulAppWarning = packageUserState.harmfulAppWarning;
    }

    public boolean isAvailable(int i) {
        boolean z = (4194304 & i) != 0;
        boolean z2 = (i & 8192) != 0;
        if (!z) {
            if (!this.installed) {
                return false;
            }
            if (this.hidden && !z2) {
                return false;
            }
        }
        return true;
    }

    public boolean isMatch(ComponentInfo componentInfo, int i) {
        boolean zIsSystemApp = componentInfo.applicationInfo.isSystemApp();
        boolean z = (4202496 & i) != 0;
        if ((!isAvailable(i) && (!zIsSystemApp || !z)) || !isEnabled(componentInfo, i)) {
            return false;
        }
        if ((1048576 & i) == 0 || zIsSystemApp) {
            return ((262144 & i) != 0 && !componentInfo.directBootAware) || ((i & 524288) != 0 && componentInfo.directBootAware);
        }
        return false;
    }

    public boolean isEnabled(ComponentInfo componentInfo, int i) {
        if ((i & 512) != 0) {
            return true;
        }
        int i2 = this.enabled;
        if (i2 != 0) {
            switch (i2) {
                case 2:
                case 3:
                    return false;
                case 4:
                    if ((i & 32768) == 0) {
                        return false;
                    }
                    break;
            }
            if (!componentInfo.applicationInfo.enabled) {
                return false;
            }
        } else if (!componentInfo.applicationInfo.enabled) {
        }
        if (ArrayUtils.contains(this.enabledComponents, componentInfo.name)) {
            return true;
        }
        if (ArrayUtils.contains(this.disabledComponents, componentInfo.name)) {
            return false;
        }
        return componentInfo.enabled;
    }

    public final boolean equals(Object obj) {
        if (!(obj instanceof PackageUserState)) {
            return false;
        }
        PackageUserState packageUserState = (PackageUserState) obj;
        if (this.ceDataInode != packageUserState.ceDataInode || this.installed != packageUserState.installed || this.stopped != packageUserState.stopped || this.notLaunched != packageUserState.notLaunched || this.hidden != packageUserState.hidden || this.suspended != packageUserState.suspended) {
            return false;
        }
        if ((this.suspended && (this.suspendingPackage == null || !this.suspendingPackage.equals(packageUserState.suspendingPackage) || !Objects.equals(this.dialogMessage, packageUserState.dialogMessage) || !BaseBundle.kindofEquals(this.suspendedAppExtras, packageUserState.suspendedAppExtras) || !BaseBundle.kindofEquals(this.suspendedLauncherExtras, packageUserState.suspendedLauncherExtras))) || this.instantApp != packageUserState.instantApp || this.virtualPreload != packageUserState.virtualPreload || this.enabled != packageUserState.enabled) {
            return false;
        }
        if ((this.lastDisableAppCaller == null && packageUserState.lastDisableAppCaller != null) || ((this.lastDisableAppCaller != null && !this.lastDisableAppCaller.equals(packageUserState.lastDisableAppCaller)) || this.domainVerificationStatus != packageUserState.domainVerificationStatus || this.appLinkGeneration != packageUserState.appLinkGeneration || this.categoryHint != packageUserState.categoryHint || this.installReason != packageUserState.installReason)) {
            return false;
        }
        if ((this.disabledComponents == null && packageUserState.disabledComponents != null) || (this.disabledComponents != null && packageUserState.disabledComponents == null)) {
            return false;
        }
        if (this.disabledComponents != null) {
            if (this.disabledComponents.size() != packageUserState.disabledComponents.size()) {
                return false;
            }
            for (int size = this.disabledComponents.size() - 1; size >= 0; size--) {
                if (!packageUserState.disabledComponents.contains(this.disabledComponents.valueAt(size))) {
                    return false;
                }
            }
        }
        if ((this.enabledComponents == null && packageUserState.enabledComponents != null) || (this.enabledComponents != null && packageUserState.enabledComponents == null)) {
            return false;
        }
        if (this.enabledComponents != null) {
            if (this.enabledComponents.size() != packageUserState.enabledComponents.size()) {
                return false;
            }
            for (int size2 = this.enabledComponents.size() - 1; size2 >= 0; size2--) {
                if (!packageUserState.enabledComponents.contains(this.enabledComponents.valueAt(size2))) {
                    return false;
                }
            }
        }
        return (this.harmfulAppWarning != null || packageUserState.harmfulAppWarning == null) && (this.harmfulAppWarning == null || this.harmfulAppWarning.equals(packageUserState.harmfulAppWarning));
    }
}
