package com.android.managedprovisioning.analytics;

import android.content.Context;
import android.content.Intent;
import com.android.managedprovisioning.model.ProvisioningParams;
import com.android.managedprovisioning.preprovisioning.EncryptionController;
import com.android.managedprovisioning.task.AbstractProvisioningTask;
import java.util.Iterator;

public class ProvisioningAnalyticsTracker {
    private static final ProvisioningAnalyticsTracker sInstance = new ProvisioningAnalyticsTracker();
    private final MetricsLoggerWrapper mMetricsLoggerWrapper = new MetricsLoggerWrapper();

    public static ProvisioningAnalyticsTracker getInstance() {
        return sInstance;
    }

    private ProvisioningAnalyticsTracker() {
    }

    public void logProvisioningStarted(Context context, ProvisioningParams provisioningParams) {
        logDpcPackageInformation(context, provisioningParams.inferDeviceAdminPackageName());
        logNetworkType(context);
        logProvisioningAction(context, provisioningParams.provisioningAction);
    }

    public void logPreProvisioningStarted(Context context, Intent intent) {
        logProvisioningExtras(context, intent);
        maybeLogEntryPoint(context, intent);
    }

    public void logCopyAccountStatus(Context context, int i) {
        this.mMetricsLoggerWrapper.logAction(context, 626, i);
    }

    public void logProvisioningCancelled(Context context, int i) {
        this.mMetricsLoggerWrapper.logAction(context, 624, i);
    }

    public void logProvisioningError(Context context, AbstractProvisioningTask abstractProvisioningTask, int i) {
        this.mMetricsLoggerWrapper.logAction(context, 625, AnalyticsUtils.getErrorString(abstractProvisioningTask, i));
    }

    public void logProvisioningNotAllowed(Context context, int i) {
        this.mMetricsLoggerWrapper.logAction(context, 625, i);
    }

    public void logProvisioningSessionStarted(Context context) {
        this.mMetricsLoggerWrapper.logAction(context, 734);
    }

    public void logProvisioningSessionCompleted(Context context) {
        this.mMetricsLoggerWrapper.logAction(context, 735);
    }

    public void logNumberOfTermsDisplayed(Context context, int i) {
        this.mMetricsLoggerWrapper.logAction(context, 810, i);
    }

    public void logNumberOfTermsRead(Context context, int i) {
        this.mMetricsLoggerWrapper.logAction(context, 811, i);
    }

    private void logProvisioningExtras(Context context, Intent intent) {
        Iterator<String> it = AnalyticsUtils.getAllProvisioningExtras(intent).iterator();
        while (it.hasNext()) {
            this.mMetricsLoggerWrapper.logAction(context, 612, it.next());
        }
    }

    private void maybeLogEntryPoint(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        String action = intent.getAction();
        byte b = -1;
        int iHashCode = action.hashCode();
        if (iHashCode != -2037430843) {
            if (iHashCode == 1865807226 && action.equals("android.nfc.action.NDEF_DISCOVERED")) {
                b = 0;
            }
        } else if (action.equals("android.app.action.PROVISION_MANAGED_DEVICE_FROM_TRUSTED_SOURCE")) {
            b = 1;
        }
        switch (b) {
            case 0:
                this.mMetricsLoggerWrapper.logAction(context, 615);
                break;
            case EncryptionController.NOTIFICATION_ID:
                this.mMetricsLoggerWrapper.logAction(context, 618);
                break;
        }
    }

    private void logDpcPackageInformation(Context context, String str) {
        this.mMetricsLoggerWrapper.logAction(context, 517, str);
        this.mMetricsLoggerWrapper.logAction(context, 518, AnalyticsUtils.getInstallerPackageName(context, str));
    }

    private void logNetworkType(Context context) {
        new NetworkTypeLogger(context).log();
    }

    private void logProvisioningAction(Context context, String str) {
        this.mMetricsLoggerWrapper.logAction(context, 611, str);
    }
}
