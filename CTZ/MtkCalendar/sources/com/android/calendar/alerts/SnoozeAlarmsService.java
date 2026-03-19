package com.android.calendar.alerts;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.provider.CalendarContract;
import android.widget.Toast;
import com.android.calendar.R;

public class SnoozeAlarmsService extends IntentService {
    private Handler mHandler;
    private static final String[] PROJECTION = {"state"};
    private static final String[] CALENDAR_PERMISSION = {"android.permission.READ_CALENDAR", "android.permission.WRITE_CALENDAR"};

    public SnoozeAlarmsService() {
        super("SnoozeAlarmsService");
        this.mHandler = new Handler();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private boolean hasRequiredPermission(String[] strArr) {
        for (String str : strArr) {
            if (checkSelfPermission(str) != 0) {
                return false;
            }
        }
        return true;
    }

    private boolean checkPermissions() {
        if (!hasRequiredPermission(CALENDAR_PERMISSION)) {
            return false;
        }
        return true;
    }

    @Override
    public void onHandleIntent(Intent intent) {
        if (!checkPermissions()) {
            closeNotificationShade(this);
            this.mHandler.post(new DisplayToast(this));
            return;
        }
        long longExtra = intent.getLongExtra("eventid", -1L);
        long longExtra2 = intent.getLongExtra("eventstart", -1L);
        long longExtra3 = intent.getLongExtra("eventend", -1L);
        int intExtra = intent.getIntExtra("notificationid", 0);
        if (longExtra != -1) {
            ContentResolver contentResolver = getContentResolver();
            if (intExtra != 0) {
                ((NotificationManager) getSystemService("notification")).cancel(intExtra);
            }
            Uri uri = CalendarContract.CalendarAlerts.CONTENT_URI;
            ContentValues contentValues = new ContentValues();
            contentValues.put(PROJECTION[0], (Integer) 2);
            contentResolver.update(uri, contentValues, "state=1 AND event_id=" + longExtra, null);
            long jCurrentTimeMillis = System.currentTimeMillis() + 300000;
            contentResolver.insert(uri, AlertUtils.makeContentValues(longExtra, longExtra2, longExtra3, jCurrentTimeMillis, 0));
            AlertUtils.scheduleAlarm(this, AlertUtils.createAlarmManager(this), jCurrentTimeMillis);
        }
        AlertService.updateAlertNotification(this);
        stopSelf();
    }

    private class DisplayToast implements Runnable {
        private final Context mContext;

        public DisplayToast(Context context) {
            this.mContext = context;
        }

        @Override
        public void run() {
            Toast.makeText(this.mContext, R.string.denied_required_permission, 1).show();
        }
    }

    private void closeNotificationShade(Context context) {
        context.sendBroadcast(new Intent("android.intent.action.CLOSE_SYSTEM_DIALOGS"));
    }
}
