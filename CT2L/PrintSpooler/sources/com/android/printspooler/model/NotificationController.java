package com.android.printspooler.model;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.print.IPrintManager;
import android.print.PrintJobId;
import android.print.PrintJobInfo;
import android.util.Log;
import com.android.printspooler.R;
import java.util.ArrayList;
import java.util.List;

final class NotificationController {
    private final Context mContext;
    private final NotificationManager mNotificationManager;

    public NotificationController(Context context) {
        this.mContext = context;
        this.mNotificationManager = (NotificationManager) this.mContext.getSystemService("notification");
    }

    public void onUpdateNotifications(List<PrintJobInfo> printJobs) {
        List<PrintJobInfo> notifyPrintJobs = new ArrayList<>();
        int printJobCount = printJobs.size();
        for (int i = 0; i < printJobCount; i++) {
            PrintJobInfo printJob = printJobs.get(i);
            if (shouldNotifyForState(printJob.getState())) {
                notifyPrintJobs.add(printJob);
            }
        }
        updateNotification(notifyPrintJobs);
    }

    private void updateNotification(List<PrintJobInfo> printJobs) {
        if (printJobs.size() <= 0) {
            removeNotification();
        } else if (printJobs.size() == 1) {
            createSimpleNotification(printJobs.get(0));
        } else {
            createStackedNotification(printJobs);
        }
    }

    private void createSimpleNotification(PrintJobInfo printJob) {
        switch (printJob.getState()) {
            case 4:
                if (!printJob.isCancelling()) {
                    createBlockedNotification(printJob);
                } else {
                    createCancellingNotification(printJob);
                }
                break;
            case 5:
            default:
                if (!printJob.isCancelling()) {
                    createPrintingNotification(printJob);
                } else {
                    createCancellingNotification(printJob);
                }
                break;
            case 6:
                createFailedNotification(printJob);
                break;
        }
    }

    private void createPrintingNotification(PrintJobInfo printJob) {
        Notification.Builder builder = new Notification.Builder(this.mContext).setContentIntent(createContentIntent(printJob.getId())).setSmallIcon(computeNotificationIcon(printJob)).setContentTitle(computeNotificationTitle(printJob)).addAction(R.drawable.stat_notify_cancelling, this.mContext.getString(R.string.cancel), createCancelIntent(printJob)).setContentText(printJob.getPrinterName()).setWhen(System.currentTimeMillis()).setOngoing(true).setShowWhen(true).setColor(this.mContext.getResources().getColor(android.R.color.system_accent3_600));
        this.mNotificationManager.notify(0, builder.build());
    }

    private void createFailedNotification(PrintJobInfo printJob) {
        Notification.Builder builder = new Notification.Builder(this.mContext).setContentIntent(createContentIntent(printJob.getId())).setSmallIcon(computeNotificationIcon(printJob)).setContentTitle(computeNotificationTitle(printJob)).addAction(R.drawable.stat_notify_cancelling, this.mContext.getString(R.string.cancel), createCancelIntent(printJob)).addAction(R.drawable.ic_restart, this.mContext.getString(R.string.restart), createRestartIntent(printJob.getId())).setContentText(printJob.getPrinterName()).setWhen(System.currentTimeMillis()).setOngoing(true).setShowWhen(true).setColor(this.mContext.getResources().getColor(android.R.color.system_accent3_600));
        this.mNotificationManager.notify(0, builder.build());
    }

    private void createBlockedNotification(PrintJobInfo printJob) {
        Notification.Builder builder = new Notification.Builder(this.mContext).setContentIntent(createContentIntent(printJob.getId())).setSmallIcon(computeNotificationIcon(printJob)).setContentTitle(computeNotificationTitle(printJob)).addAction(R.drawable.stat_notify_cancelling, this.mContext.getString(R.string.cancel), createCancelIntent(printJob)).setContentText(printJob.getPrinterName()).setWhen(System.currentTimeMillis()).setOngoing(true).setShowWhen(true).setColor(this.mContext.getResources().getColor(android.R.color.system_accent3_600));
        this.mNotificationManager.notify(0, builder.build());
    }

    private void createCancellingNotification(PrintJobInfo printJob) {
        Notification.Builder builder = new Notification.Builder(this.mContext).setContentIntent(createContentIntent(printJob.getId())).setSmallIcon(computeNotificationIcon(printJob)).setContentTitle(computeNotificationTitle(printJob)).setContentText(printJob.getPrinterName()).setWhen(System.currentTimeMillis()).setOngoing(true).setShowWhen(true).setColor(this.mContext.getResources().getColor(android.R.color.system_accent3_600));
        this.mNotificationManager.notify(0, builder.build());
    }

    private void createStackedNotification(List<PrintJobInfo> printJobs) {
        Notification.Builder builder = new Notification.Builder(this.mContext).setContentIntent(createContentIntent(null)).setWhen(System.currentTimeMillis()).setOngoing(true).setShowWhen(true);
        int printJobCount = printJobs.size();
        Notification.InboxStyle inboxStyle = new Notification.InboxStyle();
        inboxStyle.setBigContentTitle(String.format(this.mContext.getResources().getQuantityText(R.plurals.composite_notification_title_template, printJobCount).toString(), Integer.valueOf(printJobCount)));
        for (int i = printJobCount - 1; i >= 0; i--) {
            PrintJobInfo printJob = printJobs.get(i);
            if (i == printJobCount - 1) {
                builder.setLargeIcon(((BitmapDrawable) this.mContext.getResources().getDrawable(computeNotificationIcon(printJob))).getBitmap());
                builder.setSmallIcon(computeNotificationIcon(printJob));
                builder.setContentTitle(computeNotificationTitle(printJob));
                builder.setContentText(printJob.getPrinterName());
            }
            inboxStyle.addLine(computeNotificationTitle(printJob));
        }
        builder.setNumber(printJobCount);
        builder.setStyle(inboxStyle);
        builder.setColor(this.mContext.getResources().getColor(android.R.color.system_accent3_600));
        this.mNotificationManager.notify(0, builder.build());
    }

    private String computeNotificationTitle(PrintJobInfo printJob) {
        switch (printJob.getState()) {
            case 4:
                return !printJob.isCancelling() ? this.mContext.getString(R.string.blocked_notification_title_template, printJob.getLabel()) : this.mContext.getString(R.string.cancelling_notification_title_template, printJob.getLabel());
            case 5:
            default:
                return !printJob.isCancelling() ? this.mContext.getString(R.string.printing_notification_title_template, printJob.getLabel()) : this.mContext.getString(R.string.cancelling_notification_title_template, printJob.getLabel());
            case 6:
                return this.mContext.getString(R.string.failed_notification_title_template, printJob.getLabel());
        }
    }

    private void removeNotification() {
        this.mNotificationManager.cancel(0);
    }

    private PendingIntent createContentIntent(PrintJobId printJobId) {
        Intent intent = new Intent("android.settings.ACTION_PRINT_SETTINGS");
        if (printJobId != null) {
            intent.putExtra("EXTRA_PRINT_JOB_ID", printJobId.flattenToString());
            intent.setData(Uri.fromParts("printjob", printJobId.flattenToString(), null));
        }
        return PendingIntent.getActivity(this.mContext, 0, intent, 0);
    }

    private PendingIntent createCancelIntent(PrintJobInfo printJob) {
        Intent intent = new Intent(this.mContext, (Class<?>) NotificationBroadcastReceiver.class);
        intent.setAction("INTENT_ACTION_CANCEL_PRINTJOB_" + printJob.getId().flattenToString());
        intent.putExtra("EXTRA_PRINT_JOB_ID", printJob.getId());
        return PendingIntent.getBroadcast(this.mContext, 0, intent, 1073741824);
    }

    private PendingIntent createRestartIntent(PrintJobId printJobId) {
        Intent intent = new Intent(this.mContext, (Class<?>) NotificationBroadcastReceiver.class);
        intent.setAction("INTENT_ACTION_RESTART_PRINTJOB_" + printJobId.flattenToString());
        intent.putExtra("EXTRA_PRINT_JOB_ID", printJobId);
        return PendingIntent.getBroadcast(this.mContext, 0, intent, 1073741824);
    }

    private static boolean shouldNotifyForState(int state) {
        switch (state) {
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
                return true;
            default:
                return false;
        }
    }

    private static int computeNotificationIcon(PrintJobInfo printJob) {
        switch (printJob.getState()) {
            case 4:
            case 6:
                return android.R.drawable.ic_dual_screen;
            case 5:
            default:
                if (!printJob.isCancelling()) {
                    return android.R.drawable.ic_drag_handle;
                }
                return R.drawable.stat_notify_cancelling;
        }
    }

    public static final class NotificationBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.startsWith("INTENT_ACTION_CANCEL_PRINTJOB")) {
                PrintJobId printJobId = (PrintJobId) intent.getExtras().getParcelable("EXTRA_PRINT_JOB_ID");
                handleCancelPrintJob(context, printJobId);
            } else if (action != null && action.startsWith("INTENT_ACTION_RESTART_PRINTJOB")) {
                PrintJobId printJobId2 = (PrintJobId) intent.getExtras().getParcelable("EXTRA_PRINT_JOB_ID");
                handleRestartPrintJob(context, printJobId2);
            }
        }

        private void handleCancelPrintJob(Context context, final PrintJobId printJobId) {
            PowerManager powerManager = (PowerManager) context.getSystemService("power");
            final PowerManager.WakeLock wakeLock = powerManager.newWakeLock(1, "NotificationBroadcastReceiver");
            wakeLock.acquire();
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        try {
                            IPrintManager printManager = IPrintManager.Stub.asInterface(ServiceManager.getService("print"));
                            printManager.cancelPrintJob(printJobId, -2, UserHandle.myUserId());
                            wakeLock.release();
                            return null;
                        } catch (RemoteException re) {
                            Log.i("NotificationBroadcastReceiver", "Error requesting print job cancellation", re);
                            wakeLock.release();
                            return null;
                        }
                    } catch (Throwable th) {
                        wakeLock.release();
                        throw th;
                    }
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
        }

        private void handleRestartPrintJob(Context context, final PrintJobId printJobId) {
            PowerManager powerManager = (PowerManager) context.getSystemService("power");
            final PowerManager.WakeLock wakeLock = powerManager.newWakeLock(1, "NotificationBroadcastReceiver");
            wakeLock.acquire();
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        try {
                            IPrintManager printManager = IPrintManager.Stub.asInterface(ServiceManager.getService("print"));
                            printManager.restartPrintJob(printJobId, -2, UserHandle.myUserId());
                            wakeLock.release();
                            return null;
                        } catch (RemoteException re) {
                            Log.i("NotificationBroadcastReceiver", "Error requesting print job restart", re);
                            wakeLock.release();
                            return null;
                        }
                    } catch (Throwable th) {
                        wakeLock.release();
                        throw th;
                    }
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
        }
    }
}
