package com.android.server.telecom;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.AsyncQueryHandler;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.UserHandle;
import android.provider.CallLog;
import android.telecom.DisconnectCause;
import android.telephony.PhoneNumberUtils;
import android.text.BidiFormatter;
import android.text.TextDirectionHeuristics;
import android.text.TextUtils;
import com.android.server.telecom.Call;

class MissedCallNotifier extends CallsManagerListenerBase {
    private static final String[] CALL_LOG_PROJECTION = {"_id", "number", "presentation", "date", "duration", "type"};
    private final Context mContext;
    private int mMissedCallCount = 0;
    private final NotificationManager mNotificationManager;

    MissedCallNotifier(Context context) {
        this.mContext = context;
        this.mNotificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        updateOnStartup();
    }

    @Override
    public void onCallStateChanged(Call call, int i, int i2) {
        if (i == 4 && i2 == 7 && call.getDisconnectCause().getCode() == 5) {
            showMissedCallNotification(call);
        }
    }

    void clearMissedCalls() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                ContentValues contentValues = new ContentValues();
                contentValues.put("new", (Integer) 0);
                contentValues.put("is_read", (Integer) 1);
                MissedCallNotifier.this.mContext.getContentResolver().update(CallLog.Calls.CONTENT_URI, contentValues, "new = 1 AND type = ?", new String[]{Integer.toString(3)});
            }
        });
        cancelMissedCallNotification();
    }

    void showMissedCallNotification(Call call) {
        int i;
        String string;
        this.mMissedCallCount++;
        if (this.mMissedCallCount == 1) {
            i = R.string.notification_missedCallTitle;
            string = getNameForCall(call);
        } else {
            i = R.string.notification_missedCallsTitle;
            string = this.mContext.getString(R.string.notification_missedCallsMsg, Integer.valueOf(this.mMissedCallCount));
        }
        Notification.Builder builder = new Notification.Builder(this.mContext);
        builder.setSmallIcon(android.R.drawable.stat_notify_missed_call).setColor(this.mContext.getResources().getColor(R.color.theme_color)).setWhen(call.getCreationTimeMillis()).setContentTitle(this.mContext.getText(i)).setContentText(string).setContentIntent(createCallLogPendingIntent()).setAutoCancel(true).setDeleteIntent(createClearMissedCallsPendingIntent());
        Uri handle = call.getHandle();
        String schemeSpecificPart = handle == null ? null : handle.getSchemeSpecificPart();
        if (this.mMissedCallCount == 1) {
            Log.d(this, "Add actions with number %s.", Log.piiHandle(schemeSpecificPart));
            if (!TextUtils.isEmpty(schemeSpecificPart) && !TextUtils.equals(schemeSpecificPart, this.mContext.getString(R.string.handle_restricted))) {
                builder.addAction(R.drawable.stat_sys_phone_call, this.mContext.getString(R.string.notification_missedCall_call_back), createCallBackPendingIntent(handle));
                builder.addAction(R.drawable.ic_text_holo_dark, this.mContext.getString(R.string.notification_missedCall_message), createSendSmsFromNotificationPendingIntent(handle));
            }
            Bitmap photoIcon = call.getPhotoIcon();
            if (photoIcon != null) {
                builder.setLargeIcon(photoIcon);
            } else {
                Drawable photo = call.getPhoto();
                if (photo != null && (photo instanceof BitmapDrawable)) {
                    builder.setLargeIcon(((BitmapDrawable) photo).getBitmap());
                }
            }
        } else {
            Log.d(this, "Suppress actions. handle: %s, missedCalls: %d.", Log.piiHandle(schemeSpecificPart), Integer.valueOf(this.mMissedCallCount));
        }
        Notification notificationBuild = builder.build();
        configureLedOnNotification(notificationBuild);
        Log.i(this, "Adding missed call notification for %s.", call);
        this.mNotificationManager.notifyAsUser(null, 1, notificationBuild, UserHandle.CURRENT);
    }

    private void cancelMissedCallNotification() {
        this.mMissedCallCount = 0;
        this.mNotificationManager.cancel(1);
    }

    private String getNameForCall(Call call) {
        String schemeSpecificPart = call.getHandle() == null ? null : call.getHandle().getSchemeSpecificPart();
        String name = call.getName();
        if (!TextUtils.isEmpty(name) && TextUtils.isGraphic(name)) {
            return name;
        }
        if (!TextUtils.isEmpty(schemeSpecificPart)) {
            return BidiFormatter.getInstance().unicodeWrap(schemeSpecificPart, TextDirectionHeuristics.LTR);
        }
        return this.mContext.getString(R.string.unknown);
    }

    private PendingIntent createCallLogPendingIntent() {
        Intent intent = new Intent("android.intent.action.VIEW", (Uri) null);
        intent.setType("vnd.android.cursor.dir/calls");
        TaskStackBuilder taskStackBuilderCreate = TaskStackBuilder.create(this.mContext);
        taskStackBuilderCreate.addNextIntent(intent);
        return taskStackBuilderCreate.getPendingIntent(0, 0);
    }

    private PendingIntent createClearMissedCallsPendingIntent() {
        return createTelecomPendingIntent("com.android.server.telecom.ACTION_CLEAR_MISSED_CALLS", null);
    }

    private PendingIntent createCallBackPendingIntent(Uri uri) {
        return createTelecomPendingIntent("com.android.server.telecom.ACTION_CALL_BACK_FROM_NOTIFICATION", uri);
    }

    private PendingIntent createSendSmsFromNotificationPendingIntent(Uri uri) {
        return createTelecomPendingIntent("com.android.server.telecom.ACTION_SEND_SMS_FROM_NOTIFICATION", Uri.fromParts("smsto", uri.getSchemeSpecificPart(), null));
    }

    private PendingIntent createTelecomPendingIntent(String str, Uri uri) {
        return PendingIntent.getBroadcast(this.mContext, 0, new Intent(str, uri, this.mContext, TelecomBroadcastReceiver.class), 0);
    }

    private void configureLedOnNotification(Notification notification) {
        notification.flags |= 1;
        notification.defaults |= 4;
    }

    private void updateOnStartup() {
        Log.d(this, "updateOnStartup()...", new Object[0]);
        new AsyncQueryHandler(this.mContext.getContentResolver()) {
            @Override
            protected void onQueryComplete(int i, Object obj, Cursor cursor) {
                Uri uriFromParts;
                Log.d(MissedCallNotifier.this, "onQueryComplete()...", new Object[0]);
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        try {
                            String string = cursor.getString(1);
                            int i2 = cursor.getInt(2);
                            long j = cursor.getLong(3);
                            if (i2 != 1 || TextUtils.isEmpty(string)) {
                                uriFromParts = null;
                            } else {
                                uriFromParts = Uri.fromParts(PhoneNumberUtils.isUriNumber(string) ? "sip" : "tel", string, null);
                            }
                            Call call = new Call(MissedCallNotifier.this.mContext, null, null, null, null, null, true, false);
                            call.setDisconnectCause(new DisconnectCause(5));
                            call.setState(7);
                            call.setCreationTimeMillis(j);
                            call.addListener(new Call.ListenerBase() {
                                @Override
                                public void onCallerInfoChanged(Call call2) {
                                    call2.removeListener(this);
                                    MissedCallNotifier.this.showMissedCallNotification(call2);
                                }
                            });
                            call.setHandle(uriFromParts, i2);
                        } finally {
                            cursor.close();
                        }
                    }
                }
            }
        }.startQuery(0, null, CallLog.Calls.CONTENT_URI, CALL_LOG_PROJECTION, "type=3 AND new=1", null, "date DESC");
    }
}
