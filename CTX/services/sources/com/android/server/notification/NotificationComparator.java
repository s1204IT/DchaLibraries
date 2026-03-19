package com.android.server.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telecom.TelecomManager;
import com.android.internal.util.NotificationMessagingUtil;
import java.util.Comparator;
import java.util.Objects;

public class NotificationComparator implements Comparator<NotificationRecord> {
    private final Context mContext;
    private String mDefaultPhoneApp;
    private final NotificationMessagingUtil mMessagingUtil;
    private final BroadcastReceiver mPhoneAppBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            NotificationComparator.this.mDefaultPhoneApp = intent.getStringExtra("android.telecom.extra.CHANGE_DEFAULT_DIALER_PACKAGE_NAME");
        }
    };

    public NotificationComparator(Context context) {
        this.mContext = context;
        this.mContext.registerReceiver(this.mPhoneAppBroadcastReceiver, new IntentFilter("android.telecom.action.DEFAULT_DIALER_CHANGED"));
        this.mMessagingUtil = new NotificationMessagingUtil(this.mContext);
    }

    @Override
    public int compare(NotificationRecord notificationRecord, NotificationRecord notificationRecord2) {
        boolean zIsImportantColorized = isImportantColorized(notificationRecord);
        boolean zIsImportantColorized2 = isImportantColorized(notificationRecord2);
        if (zIsImportantColorized != zIsImportantColorized2) {
            return (-1) * Boolean.compare(zIsImportantColorized, zIsImportantColorized2);
        }
        boolean zIsImportantOngoing = isImportantOngoing(notificationRecord);
        boolean zIsImportantOngoing2 = isImportantOngoing(notificationRecord2);
        if (zIsImportantOngoing != zIsImportantOngoing2) {
            return (-1) * Boolean.compare(zIsImportantOngoing, zIsImportantOngoing2);
        }
        boolean zIsImportantMessaging = isImportantMessaging(notificationRecord);
        boolean zIsImportantMessaging2 = isImportantMessaging(notificationRecord2);
        if (zIsImportantMessaging != zIsImportantMessaging2) {
            return (-1) * Boolean.compare(zIsImportantMessaging, zIsImportantMessaging2);
        }
        boolean zIsImportantPeople = isImportantPeople(notificationRecord);
        boolean zIsImportantPeople2 = isImportantPeople(notificationRecord2);
        int iCompare = Float.compare(notificationRecord.getContactAffinity(), notificationRecord2.getContactAffinity());
        if (zIsImportantPeople && zIsImportantPeople2) {
            if (iCompare != 0) {
                return (-1) * iCompare;
            }
        } else if (zIsImportantPeople != zIsImportantPeople2) {
            return (-1) * Boolean.compare(zIsImportantPeople, zIsImportantPeople2);
        }
        int importance = notificationRecord.getImportance();
        int importance2 = notificationRecord2.getImportance();
        if (importance != importance2) {
            return (-1) * Integer.compare(importance, importance2);
        }
        if (iCompare != 0) {
            return (-1) * iCompare;
        }
        int packagePriority = notificationRecord.getPackagePriority();
        int packagePriority2 = notificationRecord2.getPackagePriority();
        if (packagePriority != packagePriority2) {
            return (-1) * Integer.compare(packagePriority, packagePriority2);
        }
        int i = notificationRecord.sbn.getNotification().priority;
        int i2 = notificationRecord2.sbn.getNotification().priority;
        return i != i2 ? (-1) * Integer.compare(i, i2) : (-1) * Long.compare(notificationRecord.getRankingTimeMs(), notificationRecord2.getRankingTimeMs());
    }

    private boolean isImportantColorized(NotificationRecord notificationRecord) {
        if (notificationRecord.getImportance() < 2) {
            return false;
        }
        return notificationRecord.getNotification().isColorized();
    }

    private boolean isImportantOngoing(NotificationRecord notificationRecord) {
        if (isOngoing(notificationRecord) && notificationRecord.getImportance() >= 2) {
            return isCall(notificationRecord) || isMediaNotification(notificationRecord);
        }
        return false;
    }

    protected boolean isImportantPeople(NotificationRecord notificationRecord) {
        return notificationRecord.getImportance() >= 2 && notificationRecord.getContactAffinity() > 0.0f;
    }

    protected boolean isImportantMessaging(NotificationRecord notificationRecord) {
        return this.mMessagingUtil.isImportantMessaging(notificationRecord.sbn, notificationRecord.getImportance());
    }

    private boolean isOngoing(NotificationRecord notificationRecord) {
        return (notificationRecord.getNotification().flags & 64) != 0;
    }

    private boolean isMediaNotification(NotificationRecord notificationRecord) {
        return notificationRecord.getNotification().hasMediaSession();
    }

    private boolean isCall(NotificationRecord notificationRecord) {
        return notificationRecord.isCategory("call") && isDefaultPhoneApp(notificationRecord.sbn.getPackageName());
    }

    private boolean isDefaultPhoneApp(String str) {
        if (this.mDefaultPhoneApp == null) {
            TelecomManager telecomManager = (TelecomManager) this.mContext.getSystemService("telecom");
            this.mDefaultPhoneApp = telecomManager != null ? telecomManager.getDefaultDialerPackage() : null;
        }
        return Objects.equals(str, this.mDefaultPhoneApp);
    }
}
