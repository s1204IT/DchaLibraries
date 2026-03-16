package com.android.calendar.alerts;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
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
import android.os.PowerManager;
import android.provider.CalendarContract;
import android.telephony.TelephonyManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.text.style.TextAppearanceSpan;
import android.text.style.URLSpan;
import android.util.Log;
import android.widget.RemoteViews;
import com.android.calendar.R;
import com.android.calendar.Utils;
import com.android.calendar.alerts.AlertService;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class AlertReceiver extends BroadcastReceiver {
    private static final String[] ATTENDEES_PROJECTION;
    private static final String[] EVENT_PROJECTION;
    static PowerManager.WakeLock mStartingService;
    private static Handler sAsyncHandler;
    static final Object mStartingServiceSync = new Object();
    private static final Pattern mBlankLinePattern = Pattern.compile("^\\s*$[\n\r]", 8);

    static {
        HandlerThread thr = new HandlerThread("AlertReceiver async");
        thr.start();
        sAsyncHandler = new Handler(thr.getLooper());
        ATTENDEES_PROJECTION = new String[]{"attendeeEmail", "attendeeStatus"};
        EVENT_PROJECTION = new String[]{"ownerAccount", "account_name", "title", "organizer"};
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("AlertReceiver", "onReceive: a=" + intent.getAction() + " " + intent.toString());
        if ("com.android.calendar.MAP".equals(intent.getAction())) {
            long eventId = intent.getLongExtra("eventid", -1L);
            if (eventId != -1) {
                URLSpan[] urlSpans = getURLSpans(context, eventId);
                Intent geoIntent = createMapActivityIntent(context, urlSpans);
                if (geoIntent != null) {
                    context.startActivity(geoIntent);
                    closeNotificationShade(context);
                    return;
                } else {
                    AlertService.updateAlertNotification(context);
                    return;
                }
            }
            return;
        }
        if ("com.android.calendar.CALL".equals(intent.getAction())) {
            long eventId2 = intent.getLongExtra("eventid", -1L);
            if (eventId2 != -1) {
                URLSpan[] urlSpans2 = getURLSpans(context, eventId2);
                Intent callIntent = createCallActivityIntent(context, urlSpans2);
                if (callIntent != null) {
                    context.startActivity(callIntent);
                    closeNotificationShade(context);
                    return;
                } else {
                    AlertService.updateAlertNotification(context);
                    return;
                }
            }
            return;
        }
        if ("com.android.calendar.MAIL".equals(intent.getAction())) {
            closeNotificationShade(context);
            long eventId3 = intent.getLongExtra("eventid", -1L);
            if (eventId3 != -1) {
                Intent i = new Intent(context, (Class<?>) QuickResponseActivity.class);
                i.putExtra("eventId", eventId3);
                i.addFlags(268435456);
                context.startActivity(i);
                return;
            }
            return;
        }
        Intent i2 = new Intent();
        i2.setClass(context, AlertService.class);
        i2.putExtras(intent);
        i2.putExtra("action", intent.getAction());
        Uri uri = intent.getData();
        if (uri != null) {
            i2.putExtra("uri", uri.toString());
        }
        beginStartingService(context, i2);
    }

    public static void beginStartingService(Context context, Intent intent) {
        synchronized (mStartingServiceSync) {
            if (mStartingService == null) {
                PowerManager pm = (PowerManager) context.getSystemService("power");
                mStartingService = pm.newWakeLock(1, "StartingAlertService");
                mStartingService.setReferenceCounted(false);
            }
            mStartingService.acquire();
            context.startService(intent);
        }
    }

    public static void finishStartingService(Service service, int startId) {
        synchronized (mStartingServiceSync) {
            if (mStartingService != null && service.stopSelfResult(startId)) {
                mStartingService.release();
            }
        }
    }

    private static PendingIntent createClickEventIntent(Context context, long eventId, long startMillis, long endMillis, int notificationId) {
        return createDismissAlarmsIntent(context, eventId, startMillis, endMillis, notificationId, "com.android.calendar.SHOW");
    }

    private static PendingIntent createDeleteEventIntent(Context context, long eventId, long startMillis, long endMillis, int notificationId) {
        return createDismissAlarmsIntent(context, eventId, startMillis, endMillis, notificationId, "com.android.calendar.DISMISS");
    }

    private static PendingIntent createDismissAlarmsIntent(Context context, long eventId, long startMillis, long endMillis, int notificationId, String action) {
        Intent intent = new Intent();
        intent.setClass(context, DismissAlarmsService.class);
        intent.setAction(action);
        intent.putExtra("eventid", eventId);
        intent.putExtra("eventstart", startMillis);
        intent.putExtra("eventend", endMillis);
        intent.putExtra("notificationid", notificationId);
        Uri.Builder builder = CalendarContract.Events.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, eventId);
        ContentUris.appendId(builder, startMillis);
        intent.setData(builder.build());
        return PendingIntent.getService(context, 0, intent, 134217728);
    }

    private static PendingIntent createSnoozeIntent(Context context, long eventId, long startMillis, long endMillis, int notificationId) {
        Intent intent = new Intent();
        intent.setClass(context, SnoozeAlarmsService.class);
        intent.putExtra("eventid", eventId);
        intent.putExtra("eventstart", startMillis);
        intent.putExtra("eventend", endMillis);
        intent.putExtra("notificationid", notificationId);
        Uri.Builder builder = CalendarContract.Events.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, eventId);
        ContentUris.appendId(builder, startMillis);
        intent.setData(builder.build());
        return PendingIntent.getService(context, 0, intent, 134217728);
    }

    private static PendingIntent createAlertActivityIntent(Context context) {
        Intent clickIntent = new Intent();
        clickIntent.setClass(context, AlertActivity.class);
        clickIntent.addFlags(268435456);
        return PendingIntent.getActivity(context, 0, clickIntent, 1207959552);
    }

    public static AlertService.NotificationWrapper makeBasicNotification(Context context, String title, String summaryText, long startMillis, long endMillis, long eventId, int notificationId, boolean doPopup, int priority) {
        Notification n = buildBasicNotification(new Notification.Builder(context), context, title, summaryText, startMillis, endMillis, eventId, notificationId, doPopup, priority, false);
        return new AlertService.NotificationWrapper(n, notificationId, eventId, startMillis, endMillis, doPopup);
    }

    private static Notification buildBasicNotification(Notification.Builder notificationBuilder, Context context, String title, String summaryText, long startMillis, long endMillis, long eventId, int notificationId, boolean doPopup, int priority, boolean addActionButtons) {
        Resources resources = context.getResources();
        if (title == null || title.length() == 0) {
            title = resources.getString(R.string.no_title_label);
        }
        PendingIntent clickIntent = createClickEventIntent(context, eventId, startMillis, endMillis, notificationId);
        PendingIntent deleteIntent = createDeleteEventIntent(context, eventId, startMillis, endMillis, notificationId);
        notificationBuilder.setContentTitle(title);
        notificationBuilder.setContentText(summaryText);
        notificationBuilder.setSmallIcon(R.drawable.stat_notify_calendar);
        notificationBuilder.setContentIntent(clickIntent);
        notificationBuilder.setDeleteIntent(deleteIntent);
        if (doPopup) {
            notificationBuilder.setFullScreenIntent(createAlertActivityIntent(context), true);
        }
        PendingIntent mapIntent = null;
        PendingIntent callIntent = null;
        PendingIntent snoozeIntent = null;
        PendingIntent emailIntent = null;
        if (addActionButtons) {
            URLSpan[] urlSpans = getURLSpans(context, eventId);
            mapIntent = createMapBroadcastIntent(context, urlSpans, eventId);
            callIntent = createCallBroadcastIntent(context, urlSpans, eventId);
            emailIntent = createBroadcastMailIntent(context, eventId, title);
            snoozeIntent = createSnoozeIntent(context, eventId, startMillis, endMillis, notificationId);
        }
        if (Utils.isJellybeanOrLater()) {
            notificationBuilder.setWhen(0L);
            notificationBuilder.setPriority(priority);
            int numActions = 0;
            if (mapIntent != null && 0 < 3) {
                notificationBuilder.addAction(R.drawable.ic_map, resources.getString(R.string.map_label), mapIntent);
                numActions = 0 + 1;
            }
            if (callIntent != null && numActions < 3) {
                notificationBuilder.addAction(R.drawable.ic_call, resources.getString(R.string.call_label), callIntent);
                numActions++;
            }
            if (emailIntent != null && numActions < 3) {
                notificationBuilder.addAction(R.drawable.ic_menu_email_holo_dark, resources.getString(R.string.email_guests_label), emailIntent);
                numActions++;
            }
            if (snoozeIntent != null && numActions < 3) {
                notificationBuilder.addAction(R.drawable.ic_alarm_holo_dark, resources.getString(R.string.snooze_label), snoozeIntent);
                int i = numActions + 1;
            }
            return notificationBuilder.getNotification();
        }
        Notification n = notificationBuilder.getNotification();
        RemoteViews contentView = new RemoteViews(context.getPackageName(), R.layout.notification);
        contentView.setImageViewResource(R.id.image, R.drawable.stat_notify_calendar);
        contentView.setTextViewText(R.id.title, title);
        contentView.setTextViewText(R.id.text, summaryText);
        int numActions2 = 0;
        if (mapIntent == null || 0 >= 3) {
            contentView.setViewVisibility(R.id.map_button, 8);
        } else {
            contentView.setViewVisibility(R.id.map_button, 0);
            contentView.setOnClickPendingIntent(R.id.map_button, mapIntent);
            contentView.setViewVisibility(R.id.end_padding, 8);
            numActions2 = 0 + 1;
        }
        if (callIntent == null || numActions2 >= 3) {
            contentView.setViewVisibility(R.id.call_button, 8);
        } else {
            contentView.setViewVisibility(R.id.call_button, 0);
            contentView.setOnClickPendingIntent(R.id.call_button, callIntent);
            contentView.setViewVisibility(R.id.end_padding, 8);
            numActions2++;
        }
        if (emailIntent == null || numActions2 >= 3) {
            contentView.setViewVisibility(R.id.email_button, 8);
        } else {
            contentView.setViewVisibility(R.id.email_button, 0);
            contentView.setOnClickPendingIntent(R.id.email_button, emailIntent);
            contentView.setViewVisibility(R.id.end_padding, 8);
            numActions2++;
        }
        if (snoozeIntent == null || numActions2 >= 3) {
            contentView.setViewVisibility(R.id.snooze_button, 8);
        } else {
            contentView.setViewVisibility(R.id.snooze_button, 0);
            contentView.setOnClickPendingIntent(R.id.snooze_button, snoozeIntent);
            contentView.setViewVisibility(R.id.end_padding, 8);
            int i2 = numActions2 + 1;
        }
        n.contentView = contentView;
        return n;
    }

    public static AlertService.NotificationWrapper makeExpandingNotification(Context context, String title, String summaryText, String description, long startMillis, long endMillis, long eventId, int notificationId, boolean doPopup, int priority) {
        CharSequence text;
        Notification.Builder builder = new Notification.Builder(context);
        Notification notification = buildBasicNotification(builder, context, title, summaryText, startMillis, endMillis, eventId, notificationId, doPopup, priority, true);
        if (Utils.isJellybeanOrLater()) {
            Notification.BigTextStyle expandedBuilder = new Notification.BigTextStyle();
            if (description != null) {
                description = mBlankLinePattern.matcher(description).replaceAll("").trim();
            }
            if (TextUtils.isEmpty(description)) {
                text = summaryText;
            } else {
                SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
                stringBuilder.append((CharSequence) summaryText);
                stringBuilder.append((CharSequence) "\n\n");
                stringBuilder.setSpan(new RelativeSizeSpan(0.5f), summaryText.length(), stringBuilder.length(), 0);
                stringBuilder.append((CharSequence) description);
                text = stringBuilder;
            }
            expandedBuilder.bigText(text);
            builder.setStyle(expandedBuilder);
            notification = builder.build();
        }
        return new AlertService.NotificationWrapper(notification, notificationId, eventId, startMillis, endMillis, doPopup);
    }

    public static AlertService.NotificationWrapper makeDigestNotification(Context context, ArrayList<AlertService.NotificationInfo> notificationInfos, String digestTitle, boolean expandable) {
        Notification n;
        if (notificationInfos == null || notificationInfos.size() < 1) {
            return null;
        }
        Resources res = context.getResources();
        int numEvents = notificationInfos.size();
        long[] eventIds = new long[notificationInfos.size()];
        long[] startMillis = new long[notificationInfos.size()];
        for (int i = 0; i < notificationInfos.size(); i++) {
            eventIds[i] = notificationInfos.get(i).eventId;
            startMillis[i] = notificationInfos.get(i).startMillis;
        }
        PendingIntent pendingClickIntent = createAlertActivityIntent(context);
        Intent deleteIntent = new Intent();
        deleteIntent.setClass(context, DismissAlarmsService.class);
        deleteIntent.setAction("com.android.calendar.DISMISS");
        deleteIntent.putExtra("eventids", eventIds);
        deleteIntent.putExtra("starts", startMillis);
        PendingIntent pendingDeleteIntent = PendingIntent.getService(context, 0, deleteIntent, 134217728);
        if (digestTitle == null || digestTitle.length() == 0) {
            digestTitle = res.getString(R.string.no_title_label);
        }
        Notification.Builder builder = new Notification.Builder(context);
        builder.setContentText(digestTitle);
        builder.setSmallIcon(R.drawable.stat_notify_calendar_multiple);
        builder.setContentIntent(pendingClickIntent);
        builder.setDeleteIntent(pendingDeleteIntent);
        String nEventsStr = res.getQuantityString(R.plurals.Nevents, numEvents, Integer.valueOf(numEvents));
        builder.setContentTitle(nEventsStr);
        if (Utils.isJellybeanOrLater()) {
            builder.setPriority(-2);
            if (expandable) {
                Notification.InboxStyle expandedBuilder = new Notification.InboxStyle();
                int i2 = 0;
                for (AlertService.NotificationInfo info : notificationInfos) {
                    if (i2 >= 3) {
                        break;
                    }
                    String name = info.eventName;
                    if (TextUtils.isEmpty(name)) {
                        name = context.getResources().getString(R.string.no_title_label);
                    }
                    String timeLocation = AlertUtils.formatTimeLocation(context, info.startMillis, info.allDay, info.location);
                    TextAppearanceSpan primaryTextSpan = new TextAppearanceSpan(context, R.style.NotificationPrimaryText);
                    TextAppearanceSpan secondaryTextSpan = new TextAppearanceSpan(context, R.style.NotificationSecondaryText);
                    SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
                    stringBuilder.append((CharSequence) name);
                    stringBuilder.setSpan(primaryTextSpan, 0, stringBuilder.length(), 0);
                    stringBuilder.append((CharSequence) "  ");
                    int secondaryIndex = stringBuilder.length();
                    stringBuilder.append((CharSequence) timeLocation);
                    stringBuilder.setSpan(secondaryTextSpan, secondaryIndex, stringBuilder.length(), 0);
                    expandedBuilder.addLine(stringBuilder);
                    i2++;
                }
                int remaining = numEvents - i2;
                if (remaining > 0) {
                    String nMoreEventsStr = res.getQuantityString(R.plurals.N_remaining_events, remaining, Integer.valueOf(remaining));
                    expandedBuilder.setSummaryText(nMoreEventsStr);
                }
                expandedBuilder.setBigContentTitle("");
                builder.setStyle(expandedBuilder);
            }
            n = builder.build();
        } else {
            n = builder.getNotification();
            RemoteViews contentView = new RemoteViews(context.getPackageName(), R.layout.notification);
            contentView.setImageViewResource(R.id.image, R.drawable.stat_notify_calendar_multiple);
            contentView.setTextViewText(R.id.title, nEventsStr);
            contentView.setTextViewText(R.id.text, digestTitle);
            contentView.setViewVisibility(R.id.time, 0);
            contentView.setViewVisibility(R.id.map_button, 8);
            contentView.setViewVisibility(R.id.call_button, 8);
            contentView.setViewVisibility(R.id.email_button, 8);
            contentView.setViewVisibility(R.id.snooze_button, 8);
            contentView.setViewVisibility(R.id.end_padding, 0);
            n.contentView = contentView;
            n.when = 1L;
        }
        AlertService.NotificationWrapper nw = new AlertService.NotificationWrapper(n);
        for (AlertService.NotificationInfo info2 : notificationInfos) {
            nw.add(new AlertService.NotificationWrapper(null, 0, info2.eventId, info2.startMillis, info2.endMillis, false));
        }
        return nw;
    }

    private void closeNotificationShade(Context context) {
        Intent closeNotificationShadeIntent = new Intent("android.intent.action.CLOSE_SYSTEM_DIALOGS");
        context.sendBroadcast(closeNotificationShadeIntent);
    }

    private static Cursor getEventCursor(Context context, long eventId) {
        return context.getContentResolver().query(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId), EVENT_PROJECTION, null, null, null);
    }

    private static Cursor getAttendeesCursor(Context context, long eventId) {
        return context.getContentResolver().query(CalendarContract.Attendees.CONTENT_URI, ATTENDEES_PROJECTION, "event_id=?", new String[]{Long.toString(eventId)}, "attendeeName ASC, attendeeEmail ASC");
    }

    private static Cursor getLocationCursor(Context context, long eventId) {
        return context.getContentResolver().query(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId), new String[]{"eventLocation"}, null, null, null);
    }

    private static PendingIntent createBroadcastMailIntent(Context context, long eventId, String eventTitle) {
        PendingIntent broadcast;
        String syncAccount = null;
        Cursor eventCursor = getEventCursor(context, eventId);
        if (eventCursor != null) {
            try {
                if (eventCursor.moveToFirst()) {
                    syncAccount = eventCursor.getString(1);
                }
            } finally {
                if (eventCursor != null) {
                    eventCursor.close();
                }
            }
        }
        Cursor attendeesCursor = getAttendeesCursor(context, eventId);
        if (attendeesCursor != null) {
            try {
                if (attendeesCursor.moveToFirst()) {
                    while (!Utils.isEmailableFrom(email, syncAccount)) {
                        if (!attendeesCursor.moveToNext()) {
                            broadcast = null;
                            if (attendeesCursor != null) {
                                attendeesCursor.close();
                            }
                        }
                    }
                    Intent broadcastIntent = new Intent("com.android.calendar.MAIL");
                    broadcastIntent.setClass(context, AlertReceiver.class);
                    broadcastIntent.putExtra("eventid", eventId);
                    broadcast = PendingIntent.getBroadcast(context, Long.valueOf(eventId).hashCode(), broadcastIntent, 268435456);
                } else {
                    broadcast = null;
                    if (attendeesCursor != null) {
                    }
                }
            } finally {
                if (attendeesCursor != null) {
                    attendeesCursor.close();
                }
            }
        }
        return broadcast;
    }

    static Intent createEmailIntent(Context context, long eventId, String body) {
        String ownerAccount = null;
        String syncAccount = null;
        String eventTitle = null;
        String eventOrganizer = null;
        Cursor eventCursor = getEventCursor(context, eventId);
        if (eventCursor != null) {
            try {
                if (eventCursor.moveToFirst()) {
                    ownerAccount = eventCursor.getString(0);
                    syncAccount = eventCursor.getString(1);
                    eventTitle = eventCursor.getString(2);
                    eventOrganizer = eventCursor.getString(3);
                }
            } finally {
                if (eventCursor != null) {
                    eventCursor.close();
                }
            }
        }
        if (TextUtils.isEmpty(eventTitle)) {
            eventTitle = context.getResources().getString(R.string.no_title_label);
        }
        List<String> toEmails = new ArrayList<>();
        List<String> ccEmails = new ArrayList<>();
        Cursor attendeesCursor = getAttendeesCursor(context, eventId);
        if (attendeesCursor != null) {
            try {
                if (attendeesCursor.moveToFirst()) {
                    do {
                        int status = attendeesCursor.getInt(1);
                        String email = attendeesCursor.getString(0);
                        switch (status) {
                            case 2:
                                addIfEmailable(ccEmails, email, syncAccount);
                                break;
                            default:
                                addIfEmailable(toEmails, email, syncAccount);
                                break;
                        }
                    } while (attendeesCursor.moveToNext());
                }
            } finally {
                if (attendeesCursor != null) {
                    attendeesCursor.close();
                }
            }
        }
        if (toEmails.size() == 0 && ccEmails.size() == 0 && eventOrganizer != null) {
            addIfEmailable(toEmails, eventOrganizer, syncAccount);
        }
        Intent intent = null;
        if (ownerAccount != null && (toEmails.size() > 0 || ccEmails.size() > 0)) {
            intent = Utils.createEmailAttendeesIntent(context.getResources(), eventTitle, body, toEmails, ccEmails, ownerAccount);
        }
        if (intent == null) {
            return null;
        }
        intent.addFlags(268468224);
        return intent;
    }

    private static void addIfEmailable(List<String> emailList, String email, String syncAccount) {
        if (Utils.isEmailableFrom(email, syncAccount)) {
            emailList.add(email);
        }
    }

    private static URLSpan[] getURLSpans(Context context, long eventId) {
        Cursor locationCursor = getLocationCursor(context, eventId);
        URLSpan[] urlSpans = new URLSpan[0];
        if (locationCursor != null && locationCursor.moveToFirst()) {
            String location = locationCursor.getString(0);
            if (location != null && !location.isEmpty()) {
                Spannable text = Utils.extendedLinkify(location, true);
                urlSpans = (URLSpan[]) text.getSpans(0, text.length(), URLSpan.class);
            }
            locationCursor.close();
        }
        return urlSpans;
    }

    private static PendingIntent createMapBroadcastIntent(Context context, URLSpan[] urlSpans, long eventId) {
        for (URLSpan urlSpan : urlSpans) {
            String urlString = urlSpan.getURL();
            if (urlString.startsWith("geo:")) {
                Intent broadcastIntent = new Intent("com.android.calendar.MAP");
                broadcastIntent.setClass(context, AlertReceiver.class);
                broadcastIntent.putExtra("eventid", eventId);
                return PendingIntent.getBroadcast(context, Long.valueOf(eventId).hashCode(), broadcastIntent, 268435456);
            }
        }
        return null;
    }

    private static Intent createMapActivityIntent(Context context, URLSpan[] urlSpans) {
        if (BenesseExtension.getDchaState() != 0) {
            return null;
        }
        for (URLSpan urlSpan : urlSpans) {
            String urlString = urlSpan.getURL();
            if (urlString.startsWith("geo:")) {
                Intent geoIntent = new Intent("android.intent.action.VIEW", Uri.parse(urlString));
                geoIntent.addFlags(268435456);
                return geoIntent;
            }
        }
        return null;
    }

    private static PendingIntent createCallBroadcastIntent(Context context, URLSpan[] urlSpans, long eventId) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService("phone");
        if (tm.getPhoneType() == 0) {
            return null;
        }
        for (URLSpan urlSpan : urlSpans) {
            String urlString = urlSpan.getURL();
            if (urlString.startsWith("tel:")) {
                Intent broadcastIntent = new Intent("com.android.calendar.CALL");
                broadcastIntent.setClass(context, AlertReceiver.class);
                broadcastIntent.putExtra("eventid", eventId);
                return PendingIntent.getBroadcast(context, Long.valueOf(eventId).hashCode(), broadcastIntent, 268435456);
            }
        }
        return null;
    }

    private static Intent createCallActivityIntent(Context context, URLSpan[] urlSpans) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService("phone");
        if (tm.getPhoneType() == 0) {
            return null;
        }
        for (URLSpan urlSpan : urlSpans) {
            String urlString = urlSpan.getURL();
            if (urlString.startsWith("tel:")) {
                Intent callIntent = new Intent("android.intent.action.DIAL", Uri.parse(urlString));
                callIntent.addFlags(268435456);
                return callIntent;
            }
        }
        return null;
    }
}
