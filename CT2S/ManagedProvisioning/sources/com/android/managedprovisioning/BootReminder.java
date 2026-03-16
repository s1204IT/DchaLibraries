package com.android.managedprovisioning;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class BootReminder extends BroadcastReceiver {
    private static final String[] PROFILE_OWNER_STRING_EXTRAS = {"android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_NAME"};
    private static final String[] PROFILE_OWNER_PERSISTABLE_BUNDLE_EXTRAS = {"android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE"};
    private static final String[] PROFILE_OWNER_ACCOUNT_EXTRAS = {"android.app.extra.PROVISIONING_ACCOUNT_TO_MIGRATE"};
    private static final ComponentName PROFILE_OWNER_INTENT_TARGET = ProfileOwnerPreProvisioningActivity.ALIAS_NO_CHECK_CALLER;
    private static final ComponentName DEVICE_OWNER_INTENT_TARGET = new ComponentName("com.android.managedprovisioning", "com.android.managedprovisioning.DeviceOwnerProvisioningActivity");

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            IntentStore profileOwnerIntentStore = getProfileOwnerIntentStore(context);
            Intent resumeProfileOwnerPrvIntent = profileOwnerIntentStore.load();
            if (resumeProfileOwnerPrvIntent != null && EncryptDeviceActivity.isDeviceEncrypted()) {
                profileOwnerIntentStore.clear();
                setNotification(context, resumeProfileOwnerPrvIntent);
            }
            IntentStore deviceOwnerIntentStore = getDeviceOwnerIntentStore(context);
            Intent resumeDeviceOwnerPrvIntent = deviceOwnerIntentStore.load();
            if (resumeDeviceOwnerPrvIntent != null) {
                deviceOwnerIntentStore.clear();
                resumeDeviceOwnerPrvIntent.setFlags(268435456);
                context.startActivity(resumeDeviceOwnerPrvIntent);
            }
        }
    }

    public static void setProvisioningReminder(Context context, Bundle extras) {
        IntentStore intentStore;
        String resumeTarget = extras.getString("com.android.managedprovisioning.RESUME_TARGET", null);
        if (resumeTarget != null) {
            if (resumeTarget.equals("profile_owner")) {
                intentStore = getProfileOwnerIntentStore(context);
            } else if (resumeTarget.equals("device_owner")) {
                intentStore = getDeviceOwnerIntentStore(context);
            } else {
                ProvisionLogger.loge("Unknown resume target for bootreminder.");
                return;
            }
            intentStore.save(extras);
        }
    }

    public static void cancelProvisioningReminder(Context context) {
        getProfileOwnerIntentStore(context).clear();
        getDeviceOwnerIntentStore(context).clear();
        setNotification(context, null);
    }

    private static IntentStore getProfileOwnerIntentStore(Context context) {
        return new IntentStore(context, PROFILE_OWNER_INTENT_TARGET, "profile-owner-provisioning-resume").setStringKeys(PROFILE_OWNER_STRING_EXTRAS).setPersistableBundleKeys(PROFILE_OWNER_PERSISTABLE_BUNDLE_EXTRAS).setAccountKeys(PROFILE_OWNER_ACCOUNT_EXTRAS);
    }

    private static IntentStore getDeviceOwnerIntentStore(Context context) {
        return new IntentStore(context, DEVICE_OWNER_INTENT_TARGET, "device-owner-provisioning-resume").setStringKeys(MessageParser.DEVICE_OWNER_STRING_EXTRAS).setLongKeys(MessageParser.DEVICE_OWNER_LONG_EXTRAS).setIntKeys(MessageParser.DEVICE_OWNER_INT_EXTRAS).setBooleanKeys(MessageParser.DEVICE_OWNER_BOOLEAN_EXTRAS).setPersistableBundleKeys(MessageParser.DEVICE_OWNER_PERSISTABLE_BUNDLE_EXTRAS);
    }

    private static void setNotification(Context context, Intent intent) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService("notification");
        if (intent == null) {
            notificationManager.cancel(1);
            return;
        }
        PendingIntent resumePendingIntent = PendingIntent.getActivity(context, 0, intent, 134217728);
        Notification.Builder notify = new Notification.Builder(context).setContentIntent(resumePendingIntent).setContentTitle(context.getString(R.string.continue_provisioning_notify_title)).setContentText(context.getString(R.string.continue_provisioning_notify_text)).setSmallIcon(android.R.drawable.emulator_circular_window_overlay).setVisibility(1).setColor(context.getResources().getColor(android.R.color.system_accent3_600)).setAutoCancel(true);
        notificationManager.notify(1, notify.build());
    }
}
