package com.android.contacts.vcard;

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
import android.support.v4.app.NotificationCompat;
import com.android.contacts.R;
import com.android.contacts.model.account.BaseAccountType;
import com.android.contacts.util.ContactsNotificationChannelsUtil;
import com.android.vcard.VCardEntry;
import com.mediatek.contacts.util.Log;
import com.mediatek.contacts.util.MtkToast;
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
    public boolean handleMessage(Message message) {
        MtkToast.toast(this.mContext, (String) message.obj, 1);
        return true;
    }

    @Override
    public Notification onImportProcessed(ImportRequest importRequest, int i, int i2) {
        String string;
        String string2;
        if (importRequest.displayName != null) {
            string = importRequest.displayName;
            string2 = this.mContext.getString(R.string.vcard_import_will_start_message, new Object[]{string});
        } else {
            string = this.mContext.getString(R.string.vcard_unknown_filename);
            string2 = this.mContext.getString(R.string.vcard_import_will_start_message_with_default_name);
        }
        String str = string;
        String str2 = string2;
        if (i2 == 0) {
            this.mHandler.obtainMessage(0, str2).sendToTarget();
        }
        ContactsNotificationChannelsUtil.createDefaultChannel(this.mContext);
        return constructProgressNotification(this.mContext, 1, str2, str2, i, str, -1, 0);
    }

    @Override
    public Notification onImportParsed(ImportRequest importRequest, int i, VCardEntry vCardEntry, int i2, int i3) {
        if (vCardEntry.isIgnorable()) {
            return null;
        }
        return constructProgressNotification(this.mContext.getApplicationContext(), 1, this.mContext.getString(R.string.importing_vcard_description, new Object[]{vCardEntry.getDisplayName()}), this.mContext.getString(R.string.progress_notifier_message, new Object[]{String.valueOf(i2), String.valueOf(i3), vCardEntry.getDisplayName()}), i, importRequest.displayName, i3, i2);
    }

    @Override
    public void onImportFinished(ImportRequest importRequest, int i, Uri uri) {
        Intent intent;
        String string = this.mContext.getString(R.string.importing_vcard_finished_title, new Object[]{importRequest.displayName});
        if (uri != null) {
            intent = new Intent("android.intent.action.VIEW", ContactsContract.RawContacts.getContactLookupUri(this.mContext.getContentResolver(), ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, ContentUris.parseId(uri))));
        } else {
            intent = new Intent("android.intent.action.VIEW");
            intent.setType("vnd.android.cursor.dir/contact");
        }
        intent.setPackage(this.mContext.getPackageName());
        this.mNotificationManager.notify("VCardServiceProgress", i, constructFinishNotification(1, this.mContext, string, null, intent));
    }

    @Override
    public void onImportFailed(ImportRequest importRequest) {
        this.mHandler.obtainMessage(0, this.mContext.getString(R.string.vcard_import_request_rejected_message)).sendToTarget();
    }

    @Override
    public void onImportCanceled(ImportRequest importRequest, int i) {
        Notification notificationConstructCancelNotification = constructCancelNotification(this.mContext, this.mContext.getString(R.string.importing_vcard_canceled_title, new Object[]{importRequest.displayName}));
        Log.d("VCardServiceProgress", "[onImportCanceled] displayName:" + Log.anonymize(importRequest.displayName) + ",jobId: " + i);
        this.mNotificationManager.notify("VCardServiceProgress", i, notificationConstructCancelNotification);
    }

    @Override
    public Notification onExportProcessed(ExportRequest exportRequest, int i) {
        String openableUriDisplayName = ExportVCardActivity.getOpenableUriDisplayName(this.mContext, exportRequest.destUri);
        String string = this.mContext.getString(R.string.contacts_export_will_start_message);
        this.mHandler.obtainMessage(0, string).sendToTarget();
        ContactsNotificationChannelsUtil.createDefaultChannel(this.mContext);
        return constructProgressNotification(this.mContext, 2, string, string, i, openableUriDisplayName, -1, 0);
    }

    @Override
    public void onExportFailed(ExportRequest exportRequest) {
        this.mHandler.obtainMessage(0, this.mContext.getString(R.string.vcard_export_request_rejected_message)).sendToTarget();
    }

    @Override
    public void onCancelRequest(CancelRequest cancelRequest, int i) {
        String string;
        if (i == 1) {
            string = this.mContext.getString(R.string.importing_vcard_canceled_title, new Object[]{cancelRequest.displayName});
        } else {
            string = this.mContext.getString(R.string.exporting_vcard_canceled_title, new Object[]{cancelRequest.displayName});
        }
        this.mNotificationManager.notify("VCardServiceProgress", cancelRequest.jobId, constructCancelNotification(this.mContext, string));
    }

    static Notification constructProgressNotification(Context context, int i, String str, String str2, int i2, String str3, int i3, int i4) {
        int i5;
        Intent intent = new Intent(context, (Class<?>) CancelActivity.class);
        intent.setData(new Uri.Builder().scheme("invalidscheme").authority("invalidauthority").appendQueryParameter("job_id", String.valueOf(i2)).appendQueryParameter("display_name", str3).appendQueryParameter(BaseAccountType.Attr.TYPE, String.valueOf(i)).build());
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        NotificationCompat.Builder color = builder.setOngoing(true).setChannelId(ContactsNotificationChannelsUtil.DEFAULT_CHANNEL).setOnlyAlertOnce(true).setProgress(i3, i4, i3 == -1).setContentTitle(str).setColor(context.getResources().getColor(R.color.dialtacts_theme_color));
        if (i == 1) {
            i5 = android.R.drawable.stat_sys_download_done;
        } else {
            i5 = R.drawable.mtk_stat_sys_upload_done;
        }
        color.setSmallIcon(i5).setContentIntent(PendingIntent.getActivity(context, 0, intent, 67108864));
        if (i3 > 0) {
            builder.setContentText(NumberFormat.getPercentInstance().format(((double) i4) / ((double) i3)));
        }
        return builder.build();
    }

    static Notification constructCancelNotification(Context context, String str) {
        ContactsNotificationChannelsUtil.createDefaultChannel(context);
        return new NotificationCompat.Builder(context, ContactsNotificationChannelsUtil.DEFAULT_CHANNEL).setAutoCancel(true).setSmallIcon(android.R.drawable.stat_notify_error).setColor(context.getResources().getColor(R.color.dialtacts_theme_color)).setContentTitle(str).setContentText(str).build();
    }

    static Notification constructFinishNotification(Context context, String str, String str2, Intent intent) {
        ContactsNotificationChannelsUtil.createDefaultChannel(context);
        return new NotificationCompat.Builder(context, ContactsNotificationChannelsUtil.DEFAULT_CHANNEL).setAutoCancel(true).setColor(context.getResources().getColor(R.color.dialtacts_theme_color)).setSmallIcon(R.drawable.quantum_ic_done_vd_theme_24).setContentTitle(str).setContentText(str2).setContentIntent(PendingIntent.getActivity(context, 0, intent, 67108864)).build();
    }

    static Notification constructFinishNotification(int i, Context context, String str, String str2, Intent intent) {
        return constructFinishNotificationWithFlags(i, context, str, str2, intent, 0);
    }

    static Notification constructFinishNotificationWithFlags(int i, Context context, String str, String str2, Intent intent, int i2) {
        ContactsNotificationChannelsUtil.createDefaultChannel(context);
        NotificationCompat.Builder contentText = new NotificationCompat.Builder(context).setChannelId(ContactsNotificationChannelsUtil.DEFAULT_CHANNEL).setAutoCancel(true).setColor(context.getResources().getColor(R.color.dialtacts_theme_color)).setSmallIcon(i == 1 ? android.R.drawable.stat_sys_download_done : R.drawable.mtk_stat_sys_upload_done).setContentTitle(str).setContentText(str2);
        if (intent == null) {
            intent = new Intent(context.getPackageName(), (Uri) null);
        }
        return contentText.setContentIntent(PendingIntent.getActivity(context, 0, intent, i2)).getNotification();
    }

    static Notification constructImportFailureNotification(Context context, String str) {
        ContactsNotificationChannelsUtil.createDefaultChannel(context);
        return new NotificationCompat.Builder(context, ContactsNotificationChannelsUtil.DEFAULT_CHANNEL).setAutoCancel(true).setColor(context.getResources().getColor(R.color.dialtacts_theme_color)).setSmallIcon(android.R.drawable.stat_notify_error).setContentTitle(context.getString(R.string.vcard_import_failed)).setContentText(str).build();
    }
}
