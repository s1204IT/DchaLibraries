package android.content.pm;

import android.content.pm.PackageManagerInternal;
import com.android.server.LocalServices;
import java.util.List;

public class PackageList implements PackageManagerInternal.PackageListObserver, AutoCloseable {
    private final List<String> mPackageNames;
    private final PackageManagerInternal.PackageListObserver mWrappedObserver;

    public PackageList(List<String> list, PackageManagerInternal.PackageListObserver packageListObserver) {
        this.mPackageNames = list;
        this.mWrappedObserver = packageListObserver;
    }

    @Override
    public void onPackageAdded(String str) {
        if (this.mWrappedObserver != null) {
            this.mWrappedObserver.onPackageAdded(str);
        }
    }

    @Override
    public void onPackageRemoved(String str) {
        if (this.mWrappedObserver != null) {
            this.mWrappedObserver.onPackageRemoved(str);
        }
    }

    @Override
    public void close() throws Exception {
        ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).removePackageListObserver(this);
    }

    public List<String> getPackageNames() {
        return this.mPackageNames;
    }
}
