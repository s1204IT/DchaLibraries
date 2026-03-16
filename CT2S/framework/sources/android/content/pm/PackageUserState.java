package android.content.pm;

import android.util.ArraySet;

public class PackageUserState {
    public boolean blockUninstall;
    public ArraySet<String> disabledComponents;
    public int enabled;
    public ArraySet<String> enabledComponents;
    public boolean hidden;
    public boolean installed;
    public String lastDisableAppCaller;
    public boolean notLaunched;
    public boolean stopped;

    public PackageUserState() {
        this.installed = true;
        this.hidden = false;
        this.enabled = 0;
    }

    public PackageUserState(PackageUserState o) {
        this.installed = o.installed;
        this.stopped = o.stopped;
        this.notLaunched = o.notLaunched;
        this.enabled = o.enabled;
        this.hidden = o.hidden;
        this.lastDisableAppCaller = o.lastDisableAppCaller;
        this.disabledComponents = o.disabledComponents != null ? new ArraySet<>((ArraySet) o.disabledComponents) : null;
        this.enabledComponents = o.enabledComponents != null ? new ArraySet<>((ArraySet) o.enabledComponents) : null;
        this.blockUninstall = o.blockUninstall;
    }
}
