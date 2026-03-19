package com.android.managedprovisioning.preprovisioning;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Looper;
import android.os.UserHandle;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import com.android.managedprovisioning.R;
import com.android.managedprovisioning.common.ProvisionLogger;
import com.android.managedprovisioning.common.SettingsFacade;
import com.android.managedprovisioning.common.Utils;
import com.android.managedprovisioning.model.ProvisioningParams;
import java.io.File;

public class EncryptionController {

    @VisibleForTesting
    public static final String CHANNEL_ID = "encrypt";

    @VisibleForTesting
    public static final int NOTIFICATION_ID = 1;
    private static EncryptionController sInstance;
    private final Context mContext;
    private final ComponentName mHomeReceiver;
    private final PackageManager mPackageManager;
    private boolean mProvisioningResumed;
    private final ResumeNotificationHelper mResumeNotificationHelper;
    private final SettingsFacade mSettingsFacade;
    private final int mUserId;
    private final Utils mUtils;

    public static synchronized EncryptionController getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new EncryptionController(context);
        }
        return sInstance;
    }

    private EncryptionController(Context context) {
        this(context, new Utils(), new SettingsFacade(), new ComponentName(context, (Class<?>) PostEncryptionActivity.class), new ResumeNotificationHelper(context), UserHandle.myUserId());
    }

    @VisibleForTesting
    EncryptionController(Context context, Utils utils, SettingsFacade settingsFacade, ComponentName componentName, ResumeNotificationHelper resumeNotificationHelper, int i) {
        this.mProvisioningResumed = false;
        this.mContext = ((Context) Preconditions.checkNotNull(context, "Context must not be null")).getApplicationContext();
        this.mSettingsFacade = (SettingsFacade) Preconditions.checkNotNull(settingsFacade);
        this.mUtils = (Utils) Preconditions.checkNotNull(utils, "Utils must not be null");
        this.mHomeReceiver = (ComponentName) Preconditions.checkNotNull(componentName, "HomeReceiver must not be null");
        this.mResumeNotificationHelper = (ResumeNotificationHelper) Preconditions.checkNotNull(resumeNotificationHelper, "ResumeNotificationHelper must not be null");
        this.mUserId = i;
        this.mPackageManager = context.getPackageManager();
    }

    public void setEncryptionReminder(ProvisioningParams provisioningParams) {
        ProvisionLogger.logd("Setting provisioning reminder for action: " + provisioningParams.provisioningAction);
        provisioningParams.save(getProvisioningParamsFile(this.mContext));
        if (!this.mSettingsFacade.isUserSetupCompleted(this.mContext)) {
            ProvisionLogger.logd("Enabling PostEncryptionActivity");
            this.mUtils.enableComponent(this.mHomeReceiver, this.mUserId);
            this.mPackageManager.flushPackageRestrictionsAsUser(this.mUserId);
        }
    }

    public void cancelEncryptionReminder() {
        ProvisionLogger.logd("Cancelling provisioning reminder.");
        getProvisioningParamsFile(this.mContext).delete();
        this.mUtils.disableComponent(this.mHomeReceiver, this.mUserId);
    }

    public void resumeProvisioning() {
        ProvisioningParams provisioningParamsLoad;
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("resumeProvisioning must be called on the main thread");
        }
        if (!this.mProvisioningResumed && (provisioningParamsLoad = ProvisioningParams.load(getProvisioningParamsFile(this.mContext))) != null) {
            Intent intent = new Intent("com.android.managedprovisioning.action.RESUME_PROVISIONING");
            intent.putExtra("provisioningParams", provisioningParamsLoad);
            this.mProvisioningResumed = true;
            String str = provisioningParamsLoad.provisioningAction;
            ProvisionLogger.logd("Provisioning resumed after encryption with action: " + str);
            if (!this.mUtils.isPhysicalDeviceEncrypted()) {
                ProvisionLogger.loge("Device is not encrypted after provisioning with action " + str + " but it should");
                return;
            }
            intent.addFlags(268435456);
            if (this.mUtils.isProfileOwnerAction(str)) {
                if (this.mSettingsFacade.isUserSetupCompleted(this.mContext)) {
                    this.mResumeNotificationHelper.showResumeNotification(intent);
                    return;
                } else {
                    this.mContext.startActivity(intent);
                    return;
                }
            }
            if (this.mUtils.isDeviceOwnerAction(str)) {
                this.mContext.startActivity(intent);
                return;
            }
            ProvisionLogger.loge("Unknown intent action loaded from the intent store: " + str);
        }
    }

    @VisibleForTesting
    File getProvisioningParamsFile(Context context) {
        return new File(context.getFilesDir(), "encryption_controller_provisioning_params.xml");
    }

    @VisibleForTesting
    public static class ResumeNotificationHelper {
        private final Context mContext;

        public ResumeNotificationHelper(Context context) {
            this.mContext = context;
        }

        public void showResumeNotification(Intent intent) {
            NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService("notification");
            notificationManager.createNotificationChannel(new NotificationChannel(EncryptionController.CHANNEL_ID, this.mContext.getString(R.string.encrypt), 4));
            notificationManager.notify(1, new Notification.Builder(this.mContext).setChannel(EncryptionController.CHANNEL_ID).setContentIntent(PendingIntent.getActivity(this.mContext, 0, intent, 134217728)).setContentTitle(this.mContext.getString(R.string.continue_provisioning_notify_title)).setContentText(this.mContext.getString(R.string.continue_provisioning_notify_text)).setSmallIcon(android.R.drawable.emo_im_angel).setVisibility(1).setColor(this.mContext.getResources().getColor(android.R.color.car_colorPrimary)).setAutoCancel(true).build());
        }
    }
}
