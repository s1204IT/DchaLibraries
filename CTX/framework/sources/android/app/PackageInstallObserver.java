package android.app;

import android.content.Intent;
import android.content.pm.IPackageInstallObserver2;
import android.os.Bundle;

public class PackageInstallObserver {
    private final IPackageInstallObserver2.Stub mBinder = new IPackageInstallObserver2.Stub() {
        @Override
        public void onUserActionRequired(Intent intent) {
            PackageInstallObserver.this.onUserActionRequired(intent);
        }

        @Override
        public void onPackageInstalled(String str, int i, String str2, Bundle bundle) {
            PackageInstallObserver.this.onPackageInstalled(str, i, str2, bundle);
        }
    };

    public IPackageInstallObserver2 getBinder() {
        return this.mBinder;
    }

    public void onUserActionRequired(Intent intent) {
    }

    public void onPackageInstalled(String str, int i, String str2, Bundle bundle) {
    }
}
