package com.android.packageinstaller;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.IDevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.UserInfo;
import android.graphics.drawable.Icon;
import android.os.BenesseExtension;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import android.widget.Toast;
import java.util.Iterator;
import java.util.List;

public class UninstallFinish extends BroadcastReceiver {
    private static final String LOG_TAG = UninstallFinish.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        int i;
        UserInfo userInfo;
        int intExtra = intent.getIntExtra("android.content.pm.extra.STATUS", 0);
        Log.i(LOG_TAG, "Uninstall finished extras=" + intent.getExtras());
        if (intExtra == -1) {
            context.startActivity((Intent) intent.getParcelableExtra("android.intent.extra.INTENT"));
            return;
        }
        int intExtra2 = intent.getIntExtra("com.android.packageinstaller.extra.UNINSTALL_ID", 0);
        ApplicationInfo applicationInfo = (ApplicationInfo) intent.getParcelableExtra("com.android.packageinstaller.applicationInfo");
        String stringExtra = intent.getStringExtra("com.android.packageinstaller.extra.APP_LABEL");
        boolean booleanExtra = intent.getBooleanExtra("android.intent.extra.UNINSTALL_ALL_USERS", false);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NotificationManager.class);
        UserManager userManager = (UserManager) context.getSystemService(UserManager.class);
        notificationManager.createNotificationChannel(new NotificationChannel("uninstall failure", context.getString(R.string.uninstall_failure_notification_channel), 3));
        Notification.Builder builder = new Notification.Builder(context, "uninstall failure");
        if (intExtra == 0) {
            notificationManager.cancel(intExtra2);
            Toast.makeText(context, context.getString(R.string.uninstall_done_app, stringExtra), 1).show();
            return;
        }
        if (intExtra == 2) {
            int intExtra3 = intent.getIntExtra("android.content.pm.extra.LEGACY_STATUS", 0);
            if (intExtra3 == -4) {
                IPackageManager iPackageManagerAsInterface = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
                List users = userManager.getUsers();
                int i2 = 0;
                while (true) {
                    if (i2 < users.size()) {
                        UserInfo userInfo2 = (UserInfo) users.get(i2);
                        try {
                            if (iPackageManagerAsInterface.getBlockUninstallForUser(applicationInfo.packageName, userInfo2.id)) {
                                i = userInfo2.id;
                                break;
                            }
                            continue;
                        } catch (RemoteException e) {
                            Log.e(LOG_TAG, "Failed to talk to package manager", e);
                        }
                        i2++;
                    } else {
                        i = -10000;
                        break;
                    }
                }
                if (isProfileOfOrSame(userManager, UserHandle.myUserId(), i)) {
                    addDeviceManagerButton(context, builder);
                } else {
                    addManageUsersButton(context, builder);
                }
                if (i == -10000) {
                    Log.d(LOG_TAG, "Uninstall failed for " + applicationInfo.packageName + " with code " + intExtra + " no blocking user");
                } else if (i == 0) {
                    setBigText(builder, context.getString(R.string.uninstall_blocked_device_owner));
                } else if (booleanExtra) {
                    setBigText(builder, context.getString(R.string.uninstall_all_blocked_profile_owner));
                } else {
                    setBigText(builder, context.getString(R.string.uninstall_blocked_profile_owner));
                }
            } else if (intExtra3 == -2) {
                IDevicePolicyManager iDevicePolicyManagerAsInterface = IDevicePolicyManager.Stub.asInterface(ServiceManager.getService("device_policy"));
                int iMyUserId = UserHandle.myUserId();
                Iterator it = userManager.getUsers().iterator();
                while (true) {
                    if (it.hasNext()) {
                        userInfo = (UserInfo) it.next();
                        if (!isProfileOfOrSame(userManager, iMyUserId, userInfo.id)) {
                            try {
                                if (iDevicePolicyManagerAsInterface.packageHasActiveAdmins(applicationInfo.packageName, userInfo.id)) {
                                    break;
                                }
                            } catch (RemoteException e2) {
                                Log.e(LOG_TAG, "Failed to talk to package manager", e2);
                            }
                        }
                    } else {
                        userInfo = null;
                        break;
                    }
                }
                if (userInfo == null) {
                    Log.d(LOG_TAG, "Uninstall failed because " + applicationInfo.packageName + " is a device admin");
                    addDeviceManagerButton(context, builder);
                    setBigText(builder, context.getString(R.string.uninstall_failed_device_policy_manager));
                } else {
                    Log.d(LOG_TAG, "Uninstall failed because " + applicationInfo.packageName + " is a device admin of user " + userInfo);
                    setBigText(builder, String.format(context.getString(R.string.uninstall_failed_device_policy_manager_of_user), userInfo.name));
                }
            } else {
                Log.d(LOG_TAG, "Uninstall blocked for " + applicationInfo.packageName + " with legacy code " + intExtra3);
            }
        } else {
            Log.d(LOG_TAG, "Uninstall failed for " + applicationInfo.packageName + " with code " + intExtra);
        }
        builder.setContentTitle(context.getString(R.string.uninstall_failed_app, stringExtra));
        builder.setOngoing(false);
        builder.setSmallIcon(R.drawable.ic_error);
        notificationManager.notify(intExtra2, builder.build());
    }

    private boolean isProfileOfOrSame(UserManager userManager, int i, int i2) {
        if (i == i2) {
            return true;
        }
        UserInfo profileParent = userManager.getProfileParent(i2);
        return profileParent != null && profileParent.id == i;
    }

    private void setBigText(Notification.Builder builder, CharSequence charSequence) {
        builder.setStyle(new Notification.BigTextStyle().bigText(charSequence));
    }

    private void addManageUsersButton(Context context, Notification.Builder builder) {
        Intent intent = new Intent("android.settings.USER_SETTINGS");
        intent.setFlags(1342177280);
        if (BenesseExtension.getDchaState() != 0) {
            builder.addAction(new Notification.Action.Builder(Icon.createWithResource(context, R.drawable.ic_settings_multiuser), context.getString(R.string.manage_users), (PendingIntent) null).build());
        } else {
            builder.addAction(new Notification.Action.Builder(Icon.createWithResource(context, R.drawable.ic_settings_multiuser), context.getString(R.string.manage_users), PendingIntent.getActivity(context, 0, intent, 134217728)).build());
        }
    }

    private void addDeviceManagerButton(Context context, Notification.Builder builder) {
        Intent intent = new Intent();
        intent.setClassName("com.android.settings", "com.android.settings.Settings$DeviceAdminSettingsActivity");
        intent.setFlags(1342177280);
        if (BenesseExtension.getDchaState() != 0) {
            builder.addAction(new Notification.Action.Builder(Icon.createWithResource(context, R.drawable.ic_lock), context.getString(R.string.manage_device_administrators), (PendingIntent) null).build());
        } else {
            builder.addAction(new Notification.Action.Builder(Icon.createWithResource(context, R.drawable.ic_lock), context.getString(R.string.manage_device_administrators), PendingIntent.getActivity(context, 0, intent, 134217728)).build());
        }
    }
}
