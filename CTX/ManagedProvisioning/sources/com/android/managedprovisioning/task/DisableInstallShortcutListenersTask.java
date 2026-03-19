package com.android.managedprovisioning.task;

import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import com.android.managedprovisioning.R;
import com.android.managedprovisioning.common.ProvisionLogger;
import com.android.managedprovisioning.common.Utils;
import com.android.managedprovisioning.model.ProvisioningParams;
import com.android.managedprovisioning.task.AbstractProvisioningTask;
import java.util.Iterator;

public class DisableInstallShortcutListenersTask extends AbstractProvisioningTask {
    private final PackageManager mPm;
    private int mUserId;
    private final Utils mUtils;

    public DisableInstallShortcutListenersTask(Context context, ProvisioningParams provisioningParams, AbstractProvisioningTask.Callback callback) {
        super(context, provisioningParams, callback);
        this.mUtils = new Utils();
        this.mPm = context.getPackageManager();
    }

    @Override
    public void run(int i) {
        this.mUserId = i;
        ProvisionLogger.logd("Disabling install shortcut listeners.");
        Intent intent = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
        Iterator<String> it = this.mUtils.getCurrentSystemApps(AppGlobals.getPackageManager(), this.mUserId).iterator();
        while (it.hasNext()) {
            intent.setPackage(it.next());
            disableReceivers(intent);
        }
        success();
    }

    @Override
    public int getStatusMsgId() {
        return R.string.progress_finishing_touches;
    }

    private void disableReceivers(Intent intent) {
        ComponentInfo componentInfo;
        for (ResolveInfo resolveInfo : this.mPm.queryBroadcastReceiversAsUser(intent, 786432, this.mUserId)) {
            if (resolveInfo.activityInfo != null) {
                componentInfo = resolveInfo.activityInfo;
            } else if (resolveInfo.serviceInfo != null) {
                componentInfo = resolveInfo.serviceInfo;
            } else {
                componentInfo = resolveInfo.providerInfo;
            }
            this.mUtils.disableComponent(new ComponentName(componentInfo.packageName, componentInfo.name), this.mUserId);
        }
    }
}
