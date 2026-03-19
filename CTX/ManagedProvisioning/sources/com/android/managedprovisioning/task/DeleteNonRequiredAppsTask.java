package com.android.managedprovisioning.task;

import android.content.Context;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.PackageManager;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import com.android.managedprovisioning.R;
import com.android.managedprovisioning.common.ProvisionLogger;
import com.android.managedprovisioning.model.ProvisioningParams;
import com.android.managedprovisioning.task.AbstractProvisioningTask;
import com.android.managedprovisioning.task.nonrequiredapps.NonRequiredAppsLogic;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class DeleteNonRequiredAppsTask extends AbstractProvisioningTask {
    private final NonRequiredAppsLogic mLogic;
    private final PackageManager mPm;

    public DeleteNonRequiredAppsTask(boolean z, Context context, ProvisioningParams provisioningParams, AbstractProvisioningTask.Callback callback) {
        this(context, provisioningParams, callback, new NonRequiredAppsLogic(context, z, provisioningParams));
    }

    @VisibleForTesting
    DeleteNonRequiredAppsTask(Context context, ProvisioningParams provisioningParams, AbstractProvisioningTask.Callback callback, NonRequiredAppsLogic nonRequiredAppsLogic) {
        super(context, provisioningParams, callback);
        this.mPm = (PackageManager) Preconditions.checkNotNull(context.getPackageManager());
        this.mLogic = (NonRequiredAppsLogic) Preconditions.checkNotNull(nonRequiredAppsLogic);
    }

    @Override
    public void run(int i) {
        Set<String> systemAppsToRemove = this.mLogic.getSystemAppsToRemove(i);
        this.mLogic.maybeTakeSystemAppsSnapshot(i);
        removeNonInstalledPackages(systemAppsToRemove, i);
        if (systemAppsToRemove.isEmpty()) {
            success();
            return;
        }
        IPackageDeleteObserver packageDeleteObserver = new PackageDeleteObserver(systemAppsToRemove.size());
        for (String str : systemAppsToRemove) {
            ProvisionLogger.logd("Deleting package [" + str + "] as user " + i);
            this.mPm.deletePackageAsUser(str, packageDeleteObserver, 4, i);
        }
    }

    private void removeNonInstalledPackages(Set<String> set, int i) {
        HashSet hashSet = new HashSet();
        for (String str : set) {
            try {
                if (this.mPm.getPackageInfoAsUser(str, 0, i) == null) {
                    hashSet.add(str);
                }
            } catch (PackageManager.NameNotFoundException e) {
                hashSet.add(str);
            }
        }
        set.removeAll(hashSet);
    }

    @Override
    public int getStatusMsgId() {
        return R.string.progress_delete_non_required_apps;
    }

    class PackageDeleteObserver extends IPackageDeleteObserver.Stub {
        private final AtomicInteger mPackageCount = new AtomicInteger(0);

        public PackageDeleteObserver(int i) {
            this.mPackageCount.set(i);
        }

        public void packageDeleted(String str, int i) {
            if (i != 1) {
                ProvisionLogger.logw("Could not finish the provisioning: package deletion failed");
                DeleteNonRequiredAppsTask.this.error(0);
            } else if (this.mPackageCount.decrementAndGet() == 0) {
                ProvisionLogger.logi("All non-required system apps with launcher icon, and all disallowed apps have been uninstalled.");
                DeleteNonRequiredAppsTask.this.success();
            }
        }
    }
}
