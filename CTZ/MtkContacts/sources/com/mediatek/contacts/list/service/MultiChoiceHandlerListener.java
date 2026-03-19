package com.mediatek.contacts.list.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import com.android.contacts.R;
import com.android.contacts.activities.PeopleActivity;
import com.android.contacts.model.account.BaseAccountType;
import com.android.contacts.util.ContactsNotificationChannelsUtil;
import com.mediatek.contacts.util.Log;

public class MultiChoiceHandlerListener {
    private String mCallingActivityName;
    private final Service mContext;
    private long mLastReportTime;
    private final NotificationManager mNotificationManager;

    public MultiChoiceHandlerListener(Service service, String str) {
        this(service);
        this.mCallingActivityName = str;
    }

    public MultiChoiceHandlerListener(Service service) {
        this.mCallingActivityName = null;
        this.mContext = service;
        this.mNotificationManager = (NotificationManager) this.mContext.getSystemService("notification");
    }

    synchronized void onProcessed(int i, int i2, int i3, int i4, String str) {
        String string;
        String string2;
        int i5;
        Log.i("MultiChoiceHandlerListener", "[onProcessed]requestType = " + i + ",jobId = " + i2 + ",currentCount = " + i3 + ",totalCount = " + i4 + ",contactName = " + Log.anonymize(str));
        long jCurrentTimeMillis = System.currentTimeMillis();
        if (jCurrentTimeMillis - this.mLastReportTime < 400 && i3 != 0 && i3 != 1 && i3 != i4) {
            Log.d("MultiChoiceHandlerListener", "[onProcessed]return. currentTime=" + jCurrentTimeMillis + ", mLastReportTime=" + this.mLastReportTime);
            return;
        }
        String strValueOf = String.valueOf(i4);
        if (i == 2) {
            string = this.mContext.getString(R.string.notifier_progress_delete_message, new Object[]{String.valueOf(i3), strValueOf, str});
            if (i4 == -1) {
                string2 = this.mContext.getString(R.string.notifier_progress__delete_will_start_message);
            } else {
                string2 = this.mContext.getString(R.string.notifier_progress_delete_description, new Object[]{str});
            }
            i5 = android.R.drawable.ic_menu_delete;
        } else {
            string = this.mContext.getString(R.string.notifier_progress_copy_message, new Object[]{String.valueOf(i3), strValueOf, str});
            if (i4 == -1) {
                string2 = this.mContext.getString(R.string.notifier_progress__copy_will_start_message);
            } else {
                string2 = this.mContext.getString(R.string.notifier_progress_copy_description, new Object[]{str});
            }
            i5 = R.drawable.mtk_ic_menu_copy_holo_dark;
        }
        String str2 = string;
        int i6 = i5;
        String str3 = string2;
        Log.sensitive("MultiChoiceHandlerListener", "[onProcessed] notify DEFAULT_NOTIFICATION_TAG,description: " + str3);
        if (i3 == 0) {
            ContactsNotificationChannelsUtil.createDefaultChannel(this.mContext.getApplicationContext());
        }
        this.mNotificationManager.notify("MultiChoiceServiceProgress", i2, constructProgressNotification(this.mContext.getApplicationContext(), i, str3, str2, i2, i4, i3, i6));
        this.mLastReportTime = jCurrentTimeMillis;
    }

    synchronized void onFinished(int i, int i2, int i3) {
        String string;
        String string2;
        int i4;
        long jCurrentTimeMillis = System.currentTimeMillis();
        Log.i("MultiChoiceHandlerListener", "[onFinished] jobId = " + i2 + " total = " + i3 + " requestType = " + i);
        long jCurrentTimeMillis2 = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        sb.append("[CMCC Performance test][Contacts] delete 1500 contacts end [");
        sb.append(jCurrentTimeMillis2);
        sb.append("]");
        Log.d("MultiChoiceHandlerListener", sb.toString());
        Intent action = new Intent().setAction("com.mediatek.intent.action.contacts.multichoice.process.finish");
        action.putExtra("key_finish_time", jCurrentTimeMillis);
        this.mContext.sendBroadcast(action);
        if (i == 2) {
            string = this.mContext.getString(R.string.notifier_finish_delete_title);
            string2 = this.mContext.getString(R.string.notifier_finish_delete_content, new Object[]{Integer.valueOf(i3)});
            i4 = android.R.drawable.ic_menu_delete;
        } else {
            string = this.mContext.getString(R.string.notifier_finish_copy_title);
            string2 = this.mContext.getString(R.string.notifier_finish_copy_content, new Object[]{Integer.valueOf(i3)});
            i4 = R.drawable.mtk_ic_menu_copy_holo_dark;
        }
        Intent intent = new Intent();
        intent.setClassName(this.mContext, PeopleActivity.class.getName());
        Log.i("MultiChoiceHandlerListener", "[onFinished] mCallingActivityName = " + this.mCallingActivityName + ",intent = " + intent.toString());
        this.mNotificationManager.notify("MultiChoiceServiceProgress", i2, constructFinishNotification(this.mContext, string, string2, intent, i4));
        Log.d("MultiChoiceHandlerListener", "[onFinished] notify DEFAULT_NOTIFICATION_TAG");
    }

    synchronized void onFailed(int i, int i2, int i3, int i4, int i5) {
        int i6;
        Log.i("MultiChoiceHandlerListener", "[onFailed] requestType =" + i + " jobId = " + i2 + " total = " + i3 + " succeeded = " + i4 + " failed = " + i5);
        if (i == 2) {
            i6 = R.string.notifier_fail_delete_title;
        } else {
            i6 = R.string.notifier_fail_copy_title;
        }
        ReportDialogInfo reportDialogInfo = new ReportDialogInfo();
        reportDialogInfo.setmTitleId(i6);
        reportDialogInfo.setmContentId(R.string.notifier_multichoice_process_report);
        reportDialogInfo.setmJobId(i2);
        reportDialogInfo.setmTotalNumber(i3);
        reportDialogInfo.setmSucceededNumber(i4);
        reportDialogInfo.setmFailedNumber(i5);
        this.mNotificationManager.notify("MultiChoiceServiceProgress", i2, constructReportNotification(this.mContext, reportDialogInfo));
        Log.d("MultiChoiceHandlerListener", "[onFailed] onProcessed notify DEFAULT_NOTIFICATION_TAG");
    }

    synchronized void onFailed(int i, int i2, int i3, int i4, int i5, int i6) {
        int i7;
        Log.d("MultiChoiceHandlerListener", "[onFailed] requestType =" + i + " jobId = " + i2 + " total = " + i3 + " succeeded = " + i4 + " failed = " + i5 + " errorCause = " + i6 + " ");
        int i8 = R.string.notifier_multichoice_process_report;
        if (i == 2) {
            i7 = R.string.notifier_fail_delete_title;
        } else {
            i7 = R.string.notifier_fail_copy_title;
            if (i6 == 3) {
                i8 = R.string.notifier_failure_sim_notready;
            } else if (i6 == -3) {
                i8 = R.string.notifier_failure_by_sim_full;
            } else if (i6 == 6) {
                if (i5 == 0) {
                    i7 = R.string.notifier_finish_copy_title;
                }
                i8 = R.string.error_import_usim_contact_email_lost;
            }
        }
        ReportDialogInfo reportDialogInfo = new ReportDialogInfo();
        reportDialogInfo.setmTitleId(i7);
        reportDialogInfo.setmContentId(i8);
        reportDialogInfo.setmJobId(i2);
        reportDialogInfo.setmTotalNumber(i3);
        reportDialogInfo.setmSucceededNumber(i4);
        reportDialogInfo.setmFailedNumber(i5);
        reportDialogInfo.setmErrorCauseId(i6);
        this.mNotificationManager.notify("MultiChoiceServiceProgress", i2, constructReportNotification(this.mContext, reportDialogInfo));
        Log.d("MultiChoiceHandlerListener", "[onFailed]onProcessed notify DEFAULT_NOTIFICATION_TAG");
    }

    synchronized void onCanceled(int i, int i2, int i3, int i4, int i5) {
        int i6;
        Log.i("MultiChoiceHandlerListener", "[onCanceled] requestType =" + i + " jobId = " + i2 + " total = " + i3 + " succeeded = " + i4 + " failed = " + i5);
        if (i == 2) {
            i6 = R.string.notifier_cancel_delete_title;
        } else {
            i6 = R.string.notifier_cancel_copy_title;
        }
        int i7 = -1;
        if (i3 != -1) {
            i7 = R.string.notifier_multichoice_process_report;
        }
        ReportDialogInfo reportDialogInfo = new ReportDialogInfo();
        reportDialogInfo.setmTitleId(i6);
        reportDialogInfo.setmContentId(i7);
        reportDialogInfo.setmJobId(i2);
        reportDialogInfo.setmTotalNumber(i3);
        reportDialogInfo.setmSucceededNumber(i4);
        reportDialogInfo.setmFailedNumber(i5);
        this.mNotificationManager.notify("MultiChoiceServiceProgress", i2, constructReportNotification(this.mContext, reportDialogInfo));
        Log.d("MultiChoiceHandlerListener", "[onCanceled]onProcessed notify DEFAULT_NOTIFICATION_TAG: " + this.mContext.getString(i6));
    }

    synchronized void onCanceling(int i, int i2) {
        String string;
        Log.i("MultiChoiceHandlerListener", "[onCanceling] requestType : " + i + " | jobId : " + i2);
        int i3 = 0;
        if (i == 2) {
            string = this.mContext.getString(R.string.multichoice_confirmation_title_delete);
            i3 = android.R.drawable.ic_menu_delete;
        } else {
            string = "";
        }
        this.mNotificationManager.notify("MultiChoiceServiceProgress", i2, constructCancelingNotification(this.mContext, string, i2, i3));
        Log.sensitive("MultiChoiceHandlerListener", "[onCanceling] description: " + string);
    }

    public static Notification constructFinishNotification(Context context, String str, String str2, Intent intent, int i) {
        Log.i("MultiChoiceHandlerListener", "[constructFinishNotification] title : " + str + " | description : " + str2 + ",statIconId = " + i);
        ContactsNotificationChannelsUtil.createDefaultChannel(context);
        Notification.Builder contentText = new Notification.Builder(context).setChannelId(ContactsNotificationChannelsUtil.DEFAULT_CHANNEL).setAutoCancel(true).setSmallIcon(i).setContentTitle(str).setContentText(str2);
        StringBuilder sb = new StringBuilder();
        sb.append(str);
        sb.append("\n");
        sb.append(str2);
        Notification.Builder ticker = contentText.setTicker(sb.toString());
        if (intent == null) {
            intent = new Intent(context, (Class<?>) PeopleActivity.class);
        }
        return ticker.setContentIntent(PendingIntent.getActivity(context, 0, intent, 0)).getNotification();
    }

    public static Notification constructProgressNotification(Context context, int i, String str, String str2, int i2, int i3, int i4, int i5) {
        Log.i("MultiChoiceHandlerListener", "[constructProgressNotification]requestType = " + i + ",jobId = " + i2 + ",totalCount = " + i3 + ",currentCount = " + i4 + ",statIconId = " + i5);
        Intent intent = new Intent(context, (Class<?>) MultiChoiceConfirmActivity.class);
        intent.setFlags(268435456);
        intent.putExtra("job_id", i2);
        intent.putExtra("account_info", "TODO finish");
        intent.putExtra(BaseAccountType.Attr.TYPE, i);
        Notification.Builder builder = new Notification.Builder(context);
        builder.setOngoing(true).setChannelId(ContactsNotificationChannelsUtil.DEFAULT_CHANNEL).setProgress(i3, i4, i3 == -1).setContentTitle(str).setSmallIcon(i5).setContentIntent(PendingIntent.getActivity(context, i2, intent, 134217728));
        if (i3 > 0) {
            builder.setContentText(context.getString(R.string.percentage, String.valueOf((i4 * 100) / i3)));
        }
        return builder.getNotification();
    }

    public static Notification constructReportNotification(Context context, ReportDialogInfo reportDialogInfo) {
        String string;
        String string2;
        Log.i("MultiChoiceHandlerListener", "[constructReportNotification]");
        Intent intent = new Intent(context, (Class<?>) MultiChoiceConfirmActivity.class);
        intent.setFlags(268435456);
        intent.putExtra("report_dialog", true);
        intent.putExtra("report_dialog_info", reportDialogInfo);
        int i = reportDialogInfo.mTitleId;
        int i2 = reportDialogInfo.mContentId;
        int i3 = reportDialogInfo.mTotalNumber;
        int i4 = reportDialogInfo.mSucceededNumber;
        int i5 = reportDialogInfo.mFailedNumber;
        int i6 = reportDialogInfo.mJobId;
        if (reportDialogInfo.mErrorCauseId == 6 && i5 == 0) {
            string = context.getString(i);
        } else {
            string = context.getString(i, Integer.valueOf(i3));
        }
        if (i2 == -1) {
            string2 = "";
        } else {
            string2 = context.getString(i2, Integer.valueOf(i4), Integer.valueOf(i5));
        }
        ContactsNotificationChannelsUtil.createDefaultChannel(context);
        if (string2 == null || string2.isEmpty()) {
            return new Notification.Builder(context).setChannelId(ContactsNotificationChannelsUtil.DEFAULT_CHANNEL).setAutoCancel(true).setSmallIcon(android.R.drawable.stat_notify_error).setContentTitle(string).setTicker(string).setContentIntent(PendingIntent.getActivity(context, i6, new Intent(context.getPackageName(), (Uri) null), 134217728)).getNotification();
        }
        return new Notification.Builder(context).setChannelId(ContactsNotificationChannelsUtil.DEFAULT_CHANNEL).setAutoCancel(true).setSmallIcon(android.R.drawable.stat_notify_error).setContentTitle(string).setContentText(string2).setTicker(string + "\n" + string2).setContentIntent(PendingIntent.getActivity(context, i6, intent, 134217728)).getNotification();
    }

    public static Notification constructCancelingNotification(Context context, String str, int i, int i2) {
        Log.sensitive("MultiChoiceHandlerListener", "[constructCancelingNotification]description = " + str + ",jobId = " + i + ",statIconId = " + i2);
        ContactsNotificationChannelsUtil.createDefaultChannel(context);
        Notification.Builder builder = new Notification.Builder(context);
        builder.setOngoing(true).setChannelId(ContactsNotificationChannelsUtil.DEFAULT_CHANNEL).setProgress(-1, -1, true).setContentTitle(str).setSmallIcon(i2).setContentIntent(PendingIntent.getActivity(context, i, new Intent(context.getPackageName(), (Uri) null), 134217728));
        return builder.getNotification();
    }

    public static class ReportDialogInfo implements Parcelable {
        public static final Parcelable.Creator<ReportDialogInfo> CREATOR = new Parcelable.Creator<ReportDialogInfo>() {
            @Override
            public ReportDialogInfo createFromParcel(Parcel parcel) {
                ReportDialogInfo reportDialogInfo = new ReportDialogInfo();
                reportDialogInfo.mTitleId = parcel.readInt();
                reportDialogInfo.mContentId = parcel.readInt();
                reportDialogInfo.mJobId = parcel.readInt();
                reportDialogInfo.mErrorCauseId = parcel.readInt();
                reportDialogInfo.mTotalNumber = parcel.readInt();
                reportDialogInfo.mSucceededNumber = parcel.readInt();
                reportDialogInfo.mFailedNumber = parcel.readInt();
                return reportDialogInfo;
            }

            @Override
            public ReportDialogInfo[] newArray(int i) {
                return new ReportDialogInfo[i];
            }
        };
        private int mContentId;
        private int mErrorCauseId = -1;
        private int mFailedNumber;
        private int mJobId;
        private int mSucceededNumber;
        private int mTitleId;
        private int mTotalNumber;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.mTitleId);
            parcel.writeInt(this.mContentId);
            parcel.writeInt(this.mJobId);
            parcel.writeInt(this.mErrorCauseId);
            parcel.writeInt(this.mTotalNumber);
            parcel.writeInt(this.mSucceededNumber);
            parcel.writeInt(this.mFailedNumber);
        }

        public int getmTitleId() {
            return this.mTitleId;
        }

        public void setmTitleId(int i) {
            this.mTitleId = i;
        }

        public int getmContentId() {
            return this.mContentId;
        }

        public void setmContentId(int i) {
            this.mContentId = i;
        }

        public int getmJobId() {
            return this.mJobId;
        }

        public void setmJobId(int i) {
            this.mJobId = i;
        }

        public void setmErrorCauseId(int i) {
            this.mErrorCauseId = i;
        }

        public int getmTotalNumber() {
            return this.mTotalNumber;
        }

        public void setmTotalNumber(int i) {
            this.mTotalNumber = i;
        }

        public int getmSucceededNumber() {
            return this.mSucceededNumber;
        }

        public void setmSucceededNumber(int i) {
            this.mSucceededNumber = i;
        }

        public int getmFailedNumber() {
            return this.mFailedNumber;
        }

        public void setmFailedNumber(int i) {
            this.mFailedNumber = i;
        }
    }
}
