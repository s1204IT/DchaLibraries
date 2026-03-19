package com.android.storagemanager.deletionhelper;

import android.content.pm.IPackageDeleteObserver;
import android.content.pm.PackageManager;
import android.os.Process;
import android.os.UserHandle;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class PackageDeletionTask {
    private Callback mCallback;
    private Set<String> mPackages;
    private PackageManager mPm;
    private UserHandle mUser = Process.myUserHandle();

    public static abstract class Callback {
        public abstract void onError();

        public abstract void onSuccess();
    }

    public PackageDeletionTask(PackageManager packageManager, Set<String> set, Callback callback) {
        this.mPackages = set;
        this.mCallback = callback;
        this.mPm = packageManager;
    }

    public void run() {
        IPackageDeleteObserver packageDeletionObserver = new PackageDeletionObserver(this.mPackages.size());
        Iterator<String> it = this.mPackages.iterator();
        while (it.hasNext()) {
            this.mPm.deletePackageAsUser(it.next(), packageDeletionObserver, 0, this.mUser.getIdentifier());
        }
    }

    private class PackageDeletionObserver extends IPackageDeleteObserver.Stub {
        private final AtomicInteger mPackagesRemaining = new AtomicInteger(0);

        public PackageDeletionObserver(int i) {
            this.mPackagesRemaining.set(i);
        }

        public void packageDeleted(String str, int i) {
            if (i != 1) {
                PackageDeletionTask.this.mCallback.onError();
            } else if (this.mPackagesRemaining.decrementAndGet() == 0) {
                PackageDeletionTask.this.mCallback.onSuccess();
            }
        }
    }
}
