package android.app;

import android.content.Intent;
import android.content.pm.IPackageDeleteObserver2;

public class PackageDeleteObserver {
    private final IPackageDeleteObserver2.Stub mBinder = new IPackageDeleteObserver2.Stub() {
        @Override
        public void onUserActionRequired(Intent intent) {
            PackageDeleteObserver.this.onUserActionRequired(intent);
        }

        @Override
        public void onPackageDeleted(String str, int i, String str2) {
            PackageDeleteObserver.this.onPackageDeleted(str, i, str2);
        }
    };

    public IPackageDeleteObserver2 getBinder() {
        return this.mBinder;
    }

    public void onUserActionRequired(Intent intent) {
    }

    public void onPackageDeleted(String str, int i, String str2) {
    }
}
