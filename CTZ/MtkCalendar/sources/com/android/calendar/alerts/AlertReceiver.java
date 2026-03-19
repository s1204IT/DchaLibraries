package com.android.calendar.alerts;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.CalendarContract;
import android.support.v4.app.JobIntentService;
import android.telephony.TelephonyManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.text.style.TextAppearanceSpan;
import android.text.style.URLSpan;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;
import com.android.calendar.R;
import com.android.calendar.Utils;
import com.android.calendar.alerts.AlertService;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class AlertReceiver extends BroadcastReceiver {
    private static final String[] ATTENDEES_PROJECTION;
    private static final String[] EVENT_PROJECTION;
    private static Handler sAsyncHandler;
    private static final Pattern mBlankLinePattern = Pattern.compile("^\\s*$[\n\r]", 8);
    private static final String[] CALENDAR_PERMISSION = {"android.permission.READ_CALENDAR", "android.permission.WRITE_CALENDAR"};

    static {
        HandlerThread handlerThread = new HandlerThread("AlertReceiver async");
        handlerThread.start();
        sAsyncHandler = new Handler(handlerThread.getLooper());
        ATTENDEES_PROJECTION = new String[]{"attendeeEmail", "attendeeStatus"};
        EVENT_PROJECTION = new String[]{"ownerAccount", "account_name", "title", "organizer"};
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("AlertReceiver", "onReceive: a=" + intent.getAction() + " " + intent.toString());
        if (!checkPermissions(context) && ("com.android.calendar.MAP".equals(intent.getAction()) || "com.android.calendar.CALL".equals(intent.getAction()) || "com.android.calendar.MAIL".equals(intent.getAction()))) {
            Log.d("AlertReceiver", "onReceive permission failed");
            closeNotificationShade(context);
            Toast.makeText(context, R.string.denied_required_permission, 1).show();
            return;
        }
        if ("com.android.calendar.MAP".equals(intent.getAction())) {
            long longExtra = intent.getLongExtra("eventid", -1L);
            if (longExtra != -1) {
                Intent intentCreateMapActivityIntent = createMapActivityIntent(context, getURLSpans(context, longExtra));
                if (intentCreateMapActivityIntent != null) {
                    try {
                        if (BenesseExtension.getDchaState() == 0) {
                            context.startActivity(intentCreateMapActivityIntent);
                        }
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(context, R.string.noApplications, 0).show();
                        Log.w("AlertReceiver", "onReceive: fail to start activity, no activity found to handle intent " + intentCreateMapActivityIntent.toString());
                    }
                    closeNotificationShade(context);
                    return;
                }
                AlertService.updateAlertNotification(context);
                return;
            }
            return;
        }
        if ("com.android.calendar.CALL".equals(intent.getAction())) {
            long longExtra2 = intent.getLongExtra("eventid", -1L);
            if (longExtra2 != -1) {
                Intent intentCreateCallActivityIntent = createCallActivityIntent(context, getURLSpans(context, longExtra2));
                if (intentCreateCallActivityIntent != null) {
                    if (BenesseExtension.getDchaState() == 0) {
                        context.startActivity(intentCreateCallActivityIntent);
                    }
                    closeNotificationShade(context);
                    return;
                }
                AlertService.updateAlertNotification(context);
                return;
            }
            return;
        }
        if ("com.android.calendar.MAIL".equals(intent.getAction())) {
            closeNotificationShade(context);
            long longExtra3 = intent.getLongExtra("eventid", -1L);
            if (longExtra3 != -1) {
                Intent intent2 = new Intent(context, (Class<?>) QuickResponseActivity.class);
                intent2.putExtra("eventId", longExtra3);
                intent2.addFlags(268435456);
                context.startActivity(intent2);
                return;
            }
            return;
        }
        Intent intent3 = new Intent();
        intent3.setClass(context, AlertService.class);
        intent3.putExtras(intent);
        intent3.putExtra("action", intent.getAction());
        Uri data = intent.getData();
        if (data != null) {
            intent3.putExtra("uri", data.toString());
        }
        JobIntentService.enqueueWork(context, AlertService.class, 4096, intent);
    }

    private static PendingIntent createClickEventIntent(Context context, long j, long j2, long j3, int i, boolean z) {
        return createDismissAlarmsIntent(context, j, j2, j3, i, "com.android.calendar.SHOW", z);
    }

    private static PendingIntent createDeleteEventIntent(Context context, long j, long j2, long j3, int i, boolean z) {
        return createDismissAlarmsIntent(context, j, j2, j3, i, "com.android.calendar.DISMISS", z);
    }

    private static PendingIntent createDismissAlarmsIntent(Context context, long j, long j2, long j3, int i, String str, boolean z) {
        Intent intent = new Intent();
        intent.setClass(context, DismissAlarmsService.class);
        intent.setAction(str);
        intent.putExtra("eventid", j);
        intent.putExtra("eventstart", j2);
        intent.putExtra("eventend", j3);
        intent.putExtra("notificationid", i);
        if (str.equals("com.android.calendar.SHOW")) {
            intent.putExtra("eventshowed", true);
        }
        Uri.Builder builderBuildUpon = CalendarContract.Events.CONTENT_URI.buildUpon();
        ContentUris.appendId(builderBuildUpon, j);
        ContentUris.appendId(builderBuildUpon, j2);
        intent.setData(builderBuildUpon.build());
        return PendingIntent.getService(context, 0, intent, 134217728);
    }

    private static PendingIntent createSnoozeIntent(Context context, long j, long j2, long j3, int i) {
        Intent intent = new Intent();
        intent.setClass(context, SnoozeAlarmsService.class);
        intent.putExtra("eventid", j);
        intent.putExtra("eventstart", j2);
        intent.putExtra("eventend", j3);
        intent.putExtra("notificationid", i);
        Uri.Builder builderBuildUpon = CalendarContract.Events.CONTENT_URI.buildUpon();
        ContentUris.appendId(builderBuildUpon, j);
        ContentUris.appendId(builderBuildUpon, j2);
        intent.setData(builderBuildUpon.build());
        return PendingIntent.getService(context, 0, intent, 134217728);
    }

    private static PendingIntent createAlertActivityIntent(Context context) {
        Intent intent = new Intent();
        intent.setClass(context, AlertActivity.class);
        intent.addFlags(268435456);
        return PendingIntent.getActivity(context, 0, intent, 134217728);
    }

    public static AlertService.NotificationWrapper makeBasicNotification(Context context, String str, String str2, long j, long j2, long j3, int i, boolean z, int i2) {
        return new AlertService.NotificationWrapper(buildBasicNotification(new Notification.Builder(context), context, str, str2, j, j2, j3, i, z, i2, false), i, j3, j, j2, z);
    }

    private static Notification buildBasicNotification(Notification.Builder builder, Context context, String str, String str2, long j, long j2, long j3, int i, boolean z, int i2, boolean z2) {
        String string;
        int i3;
        PendingIntent pendingIntent;
        PendingIntent pendingIntent2;
        PendingIntent pendingIntentCreateSnoozeIntent;
        int i4;
        Resources resources;
        Resources resources2 = context.getResources();
        if (str == null || str.length() == 0) {
            string = resources2.getString(R.string.no_title_label);
        } else {
            string = str;
        }
        String str3 = string;
        PendingIntent pendingIntentCreateClickEventIntent = createClickEventIntent(context, j3, j, j2, i, i2 == -2);
        PendingIntent pendingIntentCreateDeleteEventIntent = createDeleteEventIntent(context, j3, j, j2, i, i2 == -2);
        builder.setContentTitle(str3);
        builder.setContentText(str2);
        builder.setSmallIcon(R.drawable.stat_notify_calendar);
        builder.setContentIntent(pendingIntentCreateClickEventIntent);
        builder.setDeleteIntent(pendingIntentCreateDeleteEventIntent);
        if (AlertService.isEventAlreadyFired) {
            builder.setChannelId("calendar_notif_channel_of_fired_notification");
        } else {
            builder.setChannelId("calendar_notif_channel_default");
        }
        if (z) {
            i3 = 1;
            builder.setFullScreenIntent(createAlertActivityIntent(context), true);
        } else {
            i3 = 1;
        }
        PendingIntent pendingIntent3 = null;
        if (!z2) {
            pendingIntent = null;
            pendingIntent2 = null;
            pendingIntentCreateSnoozeIntent = null;
        } else {
            URLSpan[] uRLSpans = getURLSpans(context, j3);
            PendingIntent pendingIntentCreateMapBroadcastIntent = createMapBroadcastIntent(context, uRLSpans, j3);
            PendingIntent pendingIntentCreateCallBroadcastIntent = createCallBroadcastIntent(context, uRLSpans, j3);
            PendingIntent pendingIntentCreateBroadcastMailIntent = createBroadcastMailIntent(context, j3, str3);
            pendingIntentCreateSnoozeIntent = createSnoozeIntent(context, j3, j, j2, i);
            pendingIntent3 = pendingIntentCreateMapBroadcastIntent;
            pendingIntent = pendingIntentCreateCallBroadcastIntent;
            pendingIntent2 = pendingIntentCreateBroadcastMailIntent;
        }
        if (Utils.isJellybeanOrLater()) {
            builder.setWhen(0L);
            builder.setPriority(i2);
            if (pendingIntent3 != null) {
                resources = resources2;
                builder.addAction(R.drawable.ic_map, resources.getString(R.string.map_label), pendingIntent3);
            } else {
                resources = resources2;
                i3 = 0;
            }
            if (pendingIntent != null && i3 < 3) {
                builder.addAction(R.drawable.ic_call, resources.getString(R.string.call_label), pendingIntent);
                i3++;
            }
            if (pendingIntent2 != null && i3 < 3) {
                builder.addAction(R.drawable.ic_menu_email_holo_dark, resources.getString(R.string.email_guests_label), pendingIntent2);
                i3++;
            }
            if (pendingIntentCreateSnoozeIntent != null && i3 < 3) {
                builder.addAction(R.drawable.ic_alarm_holo_dark, resources.getString(R.string.snooze_label), pendingIntentCreateSnoozeIntent);
            }
            builder.setCategory("event");
            return builder.getNotification();
        }
        Notification notification = builder.getNotification();
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.notification);
        remoteViews.setImageViewResource(R.id.image, R.drawable.stat_notify_calendar);
        remoteViews.setTextViewText(R.id.title, str3);
        remoteViews.setTextViewText(R.id.text, str2);
        if (pendingIntent3 == null) {
            i4 = 0;
            remoteViews.setViewVisibility(R.id.map_button, 8);
            i3 = 0;
        } else {
            i4 = 0;
            remoteViews.setViewVisibility(R.id.map_button, 0);
            remoteViews.setOnClickPendingIntent(R.id.map_button, pendingIntent3);
            remoteViews.setViewVisibility(R.id.end_padding, 8);
        }
        if (pendingIntent == null || i3 >= 3) {
            remoteViews.setViewVisibility(R.id.call_button, 8);
        } else {
            remoteViews.setViewVisibility(R.id.call_button, i4);
            remoteViews.setOnClickPendingIntent(R.id.call_button, pendingIntent);
            remoteViews.setViewVisibility(R.id.end_padding, 8);
            i3++;
        }
        if (pendingIntent2 == null || i3 >= 3) {
            remoteViews.setViewVisibility(R.id.email_button, 8);
        } else {
            remoteViews.setViewVisibility(R.id.email_button, i4);
            remoteViews.setOnClickPendingIntent(R.id.email_button, pendingIntent2);
            remoteViews.setViewVisibility(R.id.end_padding, 8);
            i3++;
        }
        if (pendingIntentCreateSnoozeIntent == null || i3 >= 3) {
            remoteViews.setViewVisibility(R.id.snooze_button, 8);
        } else {
            remoteViews.setViewVisibility(R.id.snooze_button, i4);
            remoteViews.setOnClickPendingIntent(R.id.snooze_button, pendingIntentCreateSnoozeIntent);
            remoteViews.setViewVisibility(R.id.end_padding, 8);
        }
        notification.contentView = remoteViews;
        return notification;
    }

    public static AlertService.NotificationWrapper makeExpandingNotification(Context context, String str, String str2, String str3, long j, long j2, long j3, int i, boolean z, int i2) {
        CharSequence charSequence;
        String strTrim = str3;
        Notification.Builder builder = new Notification.Builder(context);
        Notification notificationBuildBasicNotification = buildBasicNotification(builder, context, str, str2, j, j2, j3, i, z, i2, true);
        if (Utils.isJellybeanOrLater()) {
            Notification.BigTextStyle bigTextStyle = new Notification.BigTextStyle();
            if (strTrim != null) {
                strTrim = mBlankLinePattern.matcher(strTrim).replaceAll("").trim();
            }
            if (!TextUtils.isEmpty(strTrim)) {
                SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
                spannableStringBuilder.append((CharSequence) str2);
                spannableStringBuilder.append((CharSequence) "\n\n");
                spannableStringBuilder.setSpan(new RelativeSizeSpan(0.5f), str2.length(), spannableStringBuilder.length(), 0);
                spannableStringBuilder.append((CharSequence) strTrim);
                charSequence = spannableStringBuilder;
            } else {
                charSequence = str2;
            }
            bigTextStyle.bigText(charSequence);
            builder.setStyle(bigTextStyle);
            notificationBuildBasicNotification = builder.build();
        }
        return new AlertService.NotificationWrapper(notificationBuildBasicNotification, i, j3, j, j2, z);
    }

    public static AlertService.NotificationWrapper makeDigestNotification(Context context, ArrayList<AlertService.NotificationInfo> arrayList, String str, boolean z) {
        Notification notificationBuild;
        if (arrayList == null || arrayList.size() < 1) {
            return null;
        }
        Resources resources = context.getResources();
        int size = arrayList.size();
        long[] jArr = new long[arrayList.size()];
        long[] jArr2 = new long[arrayList.size()];
        for (int i = 0; i < arrayList.size(); i++) {
            jArr[i] = arrayList.get(i).eventId;
            jArr2[i] = arrayList.get(i).startMillis;
        }
        PendingIntent pendingIntentCreateAlertActivityIntent = createAlertActivityIntent(context);
        Intent intent = new Intent();
        intent.setClass(context, DismissAlarmsService.class);
        intent.setAction("com.android.calendar.DISMISS");
        intent.putExtra("eventids", jArr);
        intent.putExtra("starts", jArr2);
        PendingIntent service = PendingIntent.getService(context, 0, intent, 134217728);
        if (str == null || str.length() == 0) {
            str = resources.getString(R.string.no_title_label);
        }
        Notification.Builder builder = new Notification.Builder(context);
        builder.setContentText(str);
        builder.setSmallIcon(R.drawable.stat_notify_calendar_multiple);
        builder.setContentIntent(pendingIntentCreateAlertActivityIntent);
        builder.setDeleteIntent(service);
        CharSequence quantityString = resources.getQuantityString(R.plurals.Nevents, size, Integer.valueOf(size));
        builder.setContentTitle(quantityString);
        if (Utils.isJellybeanOrLater()) {
            builder.setPriority(-2);
            if (z) {
                Notification.InboxStyle inboxStyle = new Notification.InboxStyle();
                int i2 = 0;
                for (AlertService.NotificationInfo notificationInfo : arrayList) {
                    if (i2 >= 3) {
                        break;
                    }
                    String string = notificationInfo.eventName;
                    if (TextUtils.isEmpty(string)) {
                        string = context.getResources().getString(R.string.no_title_label);
                    }
                    String timeLocation = AlertUtils.formatTimeLocation(context, notificationInfo.startMillis, notificationInfo.allDay, notificationInfo.location);
                    TextAppearanceSpan textAppearanceSpan = new TextAppearanceSpan(context, R.style.NotificationPrimaryText);
                    TextAppearanceSpan textAppearanceSpan2 = new TextAppearanceSpan(context, R.style.NotificationSecondaryText);
                    SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
                    spannableStringBuilder.append((CharSequence) string);
                    spannableStringBuilder.setSpan(textAppearanceSpan, 0, spannableStringBuilder.length(), 0);
                    spannableStringBuilder.append((CharSequence) "  ");
                    int length = spannableStringBuilder.length();
                    spannableStringBuilder.append((CharSequence) timeLocation);
                    spannableStringBuilder.setSpan(textAppearanceSpan2, length, spannableStringBuilder.length(), 0);
                    inboxStyle.addLine(spannableStringBuilder);
                    i2++;
                }
                int i3 = size - i2;
                if (i3 > 0) {
                    inboxStyle.setSummaryText(resources.getQuantityString(R.plurals.N_remaining_events, i3, Integer.valueOf(i3)));
                }
                inboxStyle.setBigContentTitle("");
                builder.setStyle(inboxStyle);
            }
            builder.setCategory("event");
            notificationBuild = builder.build();
        } else {
            Notification notification = builder.getNotification();
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.notification);
            remoteViews.setImageViewResource(R.id.image, R.drawable.stat_notify_calendar_multiple);
            remoteViews.setTextViewText(R.id.title, quantityString);
            remoteViews.setTextViewText(R.id.text, str);
            remoteViews.setViewVisibility(R.id.time, 0);
            remoteViews.setViewVisibility(R.id.map_button, 8);
            remoteViews.setViewVisibility(R.id.call_button, 8);
            remoteViews.setViewVisibility(R.id.email_button, 8);
            remoteViews.setViewVisibility(R.id.snooze_button, 8);
            remoteViews.setViewVisibility(R.id.end_padding, 0);
            notification.contentView = remoteViews;
            notification.when = 1L;
            notificationBuild = notification;
        }
        AlertService.NotificationWrapper notificationWrapper = new AlertService.NotificationWrapper(notificationBuild);
        for (AlertService.NotificationInfo notificationInfo2 : arrayList) {
            notificationWrapper.add(new AlertService.NotificationWrapper(null, 0, notificationInfo2.eventId, notificationInfo2.startMillis, notificationInfo2.endMillis, false));
        }
        return notificationWrapper;
    }

    private void closeNotificationShade(Context context) {
        context.sendBroadcast(new Intent("android.intent.action.CLOSE_SYSTEM_DIALOGS"));
    }

    private static Cursor getEventCursor(Context context, long j) {
        return context.getContentResolver().query(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, j), EVENT_PROJECTION, null, null, null);
    }

    private static Cursor getAttendeesCursor(Context context, long j) {
        return context.getContentResolver().query(CalendarContract.Attendees.CONTENT_URI, ATTENDEES_PROJECTION, "event_id=?", new String[]{Long.toString(j)}, "attendeeName ASC, attendeeEmail ASC");
    }

    private static Cursor getLocationCursor(Context context, long j) {
        return context.getContentResolver().query(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, j), new String[]{"eventLocation"}, null, null, null);
    }

    private static PendingIntent createBroadcastMailIntent(Context context, long j, String str) {
        String string;
        Cursor eventCursor = getEventCursor(context, j);
        if (eventCursor != null) {
            try {
                if (eventCursor.moveToFirst()) {
                    string = eventCursor.getString(1);
                } else {
                    string = null;
                }
            } finally {
                if (eventCursor != null) {
                    eventCursor.close();
                }
            }
        }
        Cursor attendeesCursor = getAttendeesCursor(context, j);
        if (attendeesCursor != null) {
            try {
                if (attendeesCursor.moveToFirst()) {
                    while (!Utils.isEmailableFrom(attendeesCursor.getString(0), string)) {
                        if (!attendeesCursor.moveToNext()) {
                        }
                    }
                    Intent intent = new Intent("com.android.calendar.MAIL");
                    intent.setClass(context, AlertReceiver.class);
                    intent.putExtra("eventid", j);
                    return PendingIntent.getBroadcast(context, Long.valueOf(j).hashCode(), intent, 268435456);
                }
            } finally {
                if (attendeesCursor != null) {
                    attendeesCursor.close();
                }
            }
        }
        if (attendeesCursor != null) {
            attendeesCursor.close();
        }
        return null;
    }

    static Intent createEmailIntent(Context context, long j, String str) {
        String string;
        String string2;
        String string3;
        String str2;
        Intent intentCreateEmailAttendeesIntent;
        Cursor eventCursor = getEventCursor(context, j);
        if (eventCursor != null) {
            try {
                if (eventCursor.moveToFirst()) {
                    String string4 = eventCursor.getString(0);
                    string = eventCursor.getString(1);
                    string2 = eventCursor.getString(2);
                    string3 = eventCursor.getString(3);
                    str2 = string4;
                } else {
                    string = null;
                    string2 = null;
                    string3 = null;
                    str2 = null;
                }
            } finally {
                if (eventCursor != null) {
                    eventCursor.close();
                }
            }
        }
        if (TextUtils.isEmpty(string2)) {
            string2 = context.getResources().getString(R.string.no_title_label);
        }
        String str3 = string2;
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        Cursor attendeesCursor = getAttendeesCursor(context, j);
        if (attendeesCursor != null) {
            try {
                if (attendeesCursor.moveToFirst()) {
                    do {
                        int i = attendeesCursor.getInt(1);
                        String string5 = attendeesCursor.getString(0);
                        if (i == 2) {
                            addIfEmailable(arrayList2, string5, string);
                        } else {
                            addIfEmailable(arrayList, string5, string);
                        }
                    } while (attendeesCursor.moveToNext());
                }
            } finally {
                if (attendeesCursor != null) {
                    attendeesCursor.close();
                }
            }
        }
        if (arrayList.size() == 0 && arrayList2.size() == 0 && string3 != null) {
            addIfEmailable(arrayList, string3, string);
        }
        if (str2 != null && (arrayList.size() > 0 || arrayList2.size() > 0)) {
            intentCreateEmailAttendeesIntent = Utils.createEmailAttendeesIntent(context.getResources(), str3, str, arrayList, arrayList2, str2);
        } else {
            intentCreateEmailAttendeesIntent = null;
        }
        if (intentCreateEmailAttendeesIntent == null) {
            return null;
        }
        intentCreateEmailAttendeesIntent.addFlags(268468224);
        return intentCreateEmailAttendeesIntent;
    }

    private static void addIfEmailable(List<String> list, String str, String str2) {
        if (Utils.isEmailableFrom(str, str2)) {
            list.add(str);
        }
    }

    private static URLSpan[] getURLSpans(Context context, long j) {
        String string;
        Cursor locationCursor = getLocationCursor(context, j);
        URLSpan[] uRLSpanArr = new URLSpan[0];
        if (locationCursor != null) {
            try {
                if (locationCursor.moveToFirst() && (string = locationCursor.getString(0)) != null && !string.isEmpty()) {
                    Spannable spannableExtendedLinkify = Utils.extendedLinkify(string, true);
                    uRLSpanArr = (URLSpan[]) spannableExtendedLinkify.getSpans(0, spannableExtendedLinkify.length(), URLSpan.class);
                }
            } finally {
                if (locationCursor != null) {
                    locationCursor.close();
                }
            }
        }
        return uRLSpanArr;
    }

    private static PendingIntent createMapBroadcastIntent(Context context, URLSpan[] uRLSpanArr, long j) {
        for (URLSpan uRLSpan : uRLSpanArr) {
            if (uRLSpan.getURL().startsWith("geo:")) {
                Intent intent = new Intent("com.android.calendar.MAP");
                intent.setClass(context, AlertReceiver.class);
                intent.putExtra("eventid", j);
                return PendingIntent.getBroadcast(context, Long.valueOf(j).hashCode(), intent, 268435456);
            }
        }
        return null;
    }

    private static Intent createMapActivityIntent(Context context, URLSpan[] uRLSpanArr) {
        for (URLSpan uRLSpan : uRLSpanArr) {
            String url = uRLSpan.getURL();
            if (url.startsWith("geo:")) {
                Intent intent = new Intent("android.intent.action.VIEW", Uri.parse(url));
                intent.addFlags(268435456);
                return intent;
            }
        }
        return null;
    }

    private static PendingIntent createCallBroadcastIntent(Context context, URLSpan[] uRLSpanArr, long j) {
        if (((TelephonyManager) context.getSystemService("phone")).getPhoneType() == 0) {
            return null;
        }
        for (URLSpan uRLSpan : uRLSpanArr) {
            if (uRLSpan.getURL().startsWith("tel:")) {
                Intent intent = new Intent("com.android.calendar.CALL");
                intent.setClass(context, AlertReceiver.class);
                intent.putExtra("eventid", j);
                return PendingIntent.getBroadcast(context, Long.valueOf(j).hashCode(), intent, 268435456);
            }
        }
        return null;
    }

    private static Intent createCallActivityIntent(Context context, URLSpan[] uRLSpanArr) {
        if (((TelephonyManager) context.getSystemService("phone")).getPhoneType() == 0) {
            return null;
        }
        for (URLSpan uRLSpan : uRLSpanArr) {
            String url = uRLSpan.getURL();
            if (url.startsWith("tel:")) {
                Intent intent = new Intent("android.intent.action.DIAL", Uri.parse(url));
                intent.addFlags(268435456);
                return intent;
            }
        }
        return null;
    }

    private static boolean checkPermissions(Context context) {
        if (!hasRequiredPermission(CALENDAR_PERMISSION, context)) {
            return false;
        }
        return true;
    }

    protected static boolean hasRequiredPermission(String[] strArr, Context context) {
        for (String str : strArr) {
            if (context.checkSelfPermission(str) != 0) {
                return false;
            }
        }
        return true;
    }
}
