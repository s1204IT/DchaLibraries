package com.android.contacts.common.vcard;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.widget.Toast;
import com.android.contacts.R;
import com.android.vcard.VCardEntry;
import java.text.NumberFormat;

public class NotificationImportExportListener implements Handler.Callback, VCardImportExportListener {
    private final Activity mContext;
    private final Handler mHandler = new Handler(this);
    private final NotificationManager mNotificationManager;

    public NotificationImportExportListener(Activity activity) {
        this.mContext = activity;
        this.mNotificationManager = (NotificationManager) activity.getSystemService("notification");
    }

    @Override
    public boolean handleMessage(Message msg) {
        String text = (String) msg.obj;
        Toast.makeText(this.mContext, text, 1).show();
        return true;
    }

    @Override
    public void onImportProcessed(ImportRequest request, int jobId, int sequence) {
        String displayName;
        String message;
        if (request.displayName != null) {
            displayName = request.displayName;
            message = this.mContext.getString(R.string.vcard_import_will_start_message, new Object[]{displayName});
        } else {
            displayName = this.mContext.getString(R.string.vcard_unknown_filename);
            message = this.mContext.getString(R.string.vcard_import_will_start_message_with_default_name);
        }
        if (sequence == 0) {
            this.mHandler.obtainMessage(0, message).sendToTarget();
        }
        Notification notification = constructProgressNotification(this.mContext, 1, message, message, jobId, displayName, -1, 0);
        this.mNotificationManager.notify("VCardServiceProgress", jobId, notification);
    }

    @Override
    public void onImportParsed(ImportRequest request, int jobId, VCardEntry entry, int currentCount, int totalCount) {
        if (!entry.isIgnorable()) {
            String totalCountString = String.valueOf(totalCount);
            String tickerText = this.mContext.getString(R.string.progress_notifier_message, new Object[]{String.valueOf(currentCount), totalCountString, entry.getDisplayName()});
            String description = this.mContext.getString(R.string.importing_vcard_description, new Object[]{entry.getDisplayName()});
            Notification notification = constructProgressNotification(this.mContext.getApplicationContext(), 1, description, tickerText, jobId, request.displayName, totalCount, currentCount);
            this.mNotificationManager.notify("VCardServiceProgress", jobId, notification);
        }
    }

    @Override
    public void onImportFinished(ImportRequest request, int jobId, Uri createdUri) {
        Intent intent;
        String description = this.mContext.getString(R.string.importing_vcard_finished_title, new Object[]{request.displayName});
        if (createdUri != null) {
            long rawContactId = ContentUris.parseId(createdUri);
            Uri contactUri = ContactsContract.RawContacts.getContactLookupUri(this.mContext.getContentResolver(), ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, rawContactId));
            intent = new Intent("android.intent.action.VIEW", contactUri);
        } else {
            intent = new Intent("android.intent.action.VIEW");
            intent.setType("vnd.android.cursor.dir/contact");
        }
        Notification notification = constructFinishNotification(this.mContext, description, null, intent);
        this.mNotificationManager.notify("VCardServiceProgress", jobId, notification);
    }

    @Override
    public void onImportFailed(ImportRequest request) {
        this.mHandler.obtainMessage(0, this.mContext.getString(R.string.vcard_import_request_rejected_message)).sendToTarget();
    }

    @Override
    public void onImportCanceled(ImportRequest request, int jobId) {
        String description = this.mContext.getString(R.string.importing_vcard_canceled_title, new Object[]{request.displayName});
        Notification notification = constructCancelNotification(this.mContext, description);
        this.mNotificationManager.notify("VCardServiceProgress", jobId, notification);
    }

    @Override
    public void onExportProcessed(ExportRequest request, int jobId) {
        String displayName = request.destUri.getLastPathSegment();
        String message = this.mContext.getString(R.string.vcard_export_will_start_message, new Object[]{displayName});
        this.mHandler.obtainMessage(0, message).sendToTarget();
        Notification notification = constructProgressNotification(this.mContext, 2, message, message, jobId, displayName, -1, 0);
        this.mNotificationManager.notify("VCardServiceProgress", jobId, notification);
    }

    @Override
    public void onExportFailed(ExportRequest request) {
        this.mHandler.obtainMessage(0, this.mContext.getString(R.string.vcard_export_request_rejected_message)).sendToTarget();
    }

    @Override
    public void onCancelRequest(CancelRequest request, int type) {
        String description = type == 1 ? this.mContext.getString(R.string.importing_vcard_canceled_title, new Object[]{request.displayName}) : this.mContext.getString(R.string.exporting_vcard_canceled_title, new Object[]{request.displayName});
        Notification notification = constructCancelNotification(this.mContext, description);
        this.mNotificationManager.notify("VCardServiceProgress", request.jobId, notification);
    }

    static Notification constructProgressNotification(Context context, int type, String description, String tickerText, int jobId, String displayName, int totalCount, int currentCount) {
        Intent intent = new Intent(context, (Class<?>) CancelActivity.class);
        Uri uri = new Uri.Builder().scheme("invalidscheme").authority("invalidauthority").appendQueryParameter("job_id", String.valueOf(jobId)).appendQueryParameter("display_name", displayName).appendQueryParameter("type", String.valueOf(type)).build();
        intent.setData(uri);
        Notification.Builder builder = new Notification.Builder(context);
        builder.setOngoing(true).setProgress(totalCount, currentCount, totalCount == -1).setTicker(tickerText).setContentTitle(description).setColor(context.getResources().getColor(R.color.dialtacts_theme_color)).setSmallIcon(type == 1 ? android.R.drawable.stat_sys_download : android.R.drawable.stat_sys_upload).setContentIntent(PendingIntent.getActivity(context, 0, intent, 0));
        if (totalCount > 0) {
            String percentage = NumberFormat.getPercentInstance().format(((double) currentCount) / ((double) totalCount));
            builder.setContentText(percentage);
        }
        return builder.getNotification();
    }

    static Notification constructCancelNotification(Context context, String description) {
        return new Notification.Builder(context).setAutoCancel(true).setSmallIcon(android.R.drawable.stat_notify_error).setColor(context.getResources().getColor(R.color.dialtacts_theme_color)).setContentTitle(description).setContentText(description).setContentIntent(PendingIntent.getActivity(context, 0, new Intent(), 0)).getNotification();
    }

    static Notification constructFinishNotification(Context context, String title, String description, Intent intent) {
        Notification.Builder contentText = new Notification.Builder(context).setAutoCancel(true).setColor(context.getResources().getColor(R.color.dialtacts_theme_color)).setSmallIcon(android.R.drawable.stat_sys_download_done).setContentTitle(title).setContentText(description);
        if (intent == null) {
            intent = new Intent();
        }
        return contentText.setContentIntent(PendingIntent.getActivity(context, 0, intent, 0)).getNotification();
    }

    static Notification constructImportFailureNotification(Context context, String reason) {
        return new Notification.Builder(context).setAutoCancel(true).setColor(context.getResources().getColor(R.color.dialtacts_theme_color)).setSmallIcon(android.R.drawable.stat_notify_error).setContentTitle(context.getString(R.string.vcard_import_failed)).setContentText(reason).setContentIntent(PendingIntent.getActivity(context, 0, new Intent(), 0)).getNotification();
    }
}
