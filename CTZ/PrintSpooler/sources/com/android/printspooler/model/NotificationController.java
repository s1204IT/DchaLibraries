package com.android.printspooler.model;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BenesseExtension;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.print.IPrintManager;
import android.print.PrintJobId;
import android.print.PrintJobInfo;
import android.util.ArraySet;
import android.util.Log;
import com.android.printspooler.R;
import java.util.ArrayList;
import java.util.List;

final class NotificationController {
    private final Context mContext;
    private final NotificationManager mNotificationManager;
    private final ArraySet<PrintJobId> mNotifications = new ArraySet<>(0);

    public NotificationController(Context context) {
        this.mContext = context;
        this.mNotificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        this.mNotificationManager.createNotificationChannel(new NotificationChannel("PRINT_PROGRESS", context.getString(R.string.notification_channel_progress), 2));
        this.mNotificationManager.createNotificationChannel(new NotificationChannel("PRINT_FAILURES", context.getString(R.string.notification_channel_failure), 3));
    }

    public void onUpdateNotifications(List<PrintJobInfo> list) {
        ArrayList arrayList = new ArrayList();
        int size = list.size();
        for (int i = 0; i < size; i++) {
            PrintJobInfo printJobInfo = list.get(i);
            if (shouldNotifyForState(printJobInfo.getState())) {
                arrayList.add(printJobInfo);
            }
        }
        updateNotifications(arrayList);
    }

    private void updateNotifications(List<PrintJobInfo> list) {
        ArraySet arraySet = new ArraySet((ArraySet) this.mNotifications);
        int size = list.size();
        for (int i = 0; i < size; i++) {
            PrintJobInfo printJobInfo = list.get(i);
            PrintJobId id = printJobInfo.getId();
            arraySet.remove(id);
            this.mNotifications.add(id);
            createSimpleNotification(printJobInfo);
        }
        int size2 = arraySet.size();
        for (int i2 = 0; i2 < size2; i2++) {
            PrintJobId printJobId = (PrintJobId) arraySet.valueAt(i2);
            this.mNotificationManager.cancel(printJobId.flattenToString(), 0);
            this.mNotifications.remove(printJobId);
        }
    }

    private void createSimpleNotification(PrintJobInfo printJobInfo) {
        int state = printJobInfo.getState();
        if (state == 4) {
            if (!printJobInfo.isCancelling()) {
                createBlockedNotification(printJobInfo);
                return;
            } else {
                createCancellingNotification(printJobInfo);
                return;
            }
        }
        if (state == 6) {
            createFailedNotification(printJobInfo);
        } else if (!printJobInfo.isCancelling()) {
            createPrintingNotification(printJobInfo);
        } else {
            createCancellingNotification(printJobInfo);
        }
    }

    private Notification.Action createCancelAction(PrintJobInfo printJobInfo) {
        return new Notification.Action.Builder(Icon.createWithResource(this.mContext, R.drawable.stat_notify_cancelling), this.mContext.getString(R.string.cancel), createCancelIntent(printJobInfo)).build();
    }

    private void createNotification(PrintJobInfo printJobInfo, Notification.Action action, Notification.Action action2) {
        Notification.Builder color = new Notification.Builder(this.mContext, computeChannel(printJobInfo)).setContentIntent(createContentIntent(printJobInfo.getId())).setSmallIcon(computeNotificationIcon(printJobInfo)).setContentTitle(computeNotificationTitle(printJobInfo)).setWhen(System.currentTimeMillis()).setOngoing(true).setShowWhen(true).setOnlyAlertOnce(true).setColor(this.mContext.getColor(android.R.color.car_colorPrimary));
        if (action != null) {
            color.addAction(action);
        }
        if (action2 != null) {
            color.addAction(action2);
        }
        if (printJobInfo.getState() == 3 || printJobInfo.getState() == 2) {
            float progress = printJobInfo.getProgress();
            if (progress >= 0.0f) {
                color.setProgress(Integer.MAX_VALUE, (int) (2.1474836E9f * progress), false);
            } else {
                color.setProgress(Integer.MAX_VALUE, 0, true);
            }
        }
        CharSequence status = printJobInfo.getStatus(this.mContext.getPackageManager());
        if (status != null) {
            color.setContentText(status);
        } else {
            color.setContentText(printJobInfo.getPrinterName());
        }
        this.mNotificationManager.notify(printJobInfo.getId().flattenToString(), 0, color.build());
    }

    private void createPrintingNotification(PrintJobInfo printJobInfo) {
        createNotification(printJobInfo, createCancelAction(printJobInfo), null);
    }

    private void createFailedNotification(PrintJobInfo printJobInfo) {
        createNotification(printJobInfo, createCancelAction(printJobInfo), new Notification.Action.Builder(Icon.createWithResource(this.mContext, R.drawable.ic_restart), this.mContext.getString(R.string.restart), createRestartIntent(printJobInfo.getId())).build());
    }

    private void createBlockedNotification(PrintJobInfo printJobInfo) {
        createNotification(printJobInfo, createCancelAction(printJobInfo), null);
    }

    private void createCancellingNotification(PrintJobInfo printJobInfo) {
        createNotification(printJobInfo, null, null);
    }

    private String computeNotificationTitle(PrintJobInfo printJobInfo) {
        int state = printJobInfo.getState();
        if (state == 4) {
            if (!printJobInfo.isCancelling()) {
                return this.mContext.getString(R.string.blocked_notification_title_template, printJobInfo.getLabel());
            }
            return this.mContext.getString(R.string.cancelling_notification_title_template, printJobInfo.getLabel());
        }
        if (state == 6) {
            return this.mContext.getString(R.string.failed_notification_title_template, printJobInfo.getLabel());
        }
        if (!printJobInfo.isCancelling()) {
            return this.mContext.getString(R.string.printing_notification_title_template, printJobInfo.getLabel());
        }
        return this.mContext.getString(R.string.cancelling_notification_title_template, printJobInfo.getLabel());
    }

    private PendingIntent createContentIntent(PrintJobId printJobId) {
        if (BenesseExtension.getDchaState() != 0) {
            return null;
        }
        Intent intent = new Intent("android.settings.ACTION_PRINT_SETTINGS");
        if (printJobId != null) {
            intent.putExtra("EXTRA_PRINT_JOB_ID", printJobId.flattenToString());
            intent.setData(Uri.fromParts("printjob", printJobId.flattenToString(), null));
        }
        return PendingIntent.getActivity(this.mContext, 0, intent, 0);
    }

    private PendingIntent createCancelIntent(PrintJobInfo printJobInfo) {
        Intent intent = new Intent(this.mContext, (Class<?>) NotificationBroadcastReceiver.class);
        intent.setAction("INTENT_ACTION_CANCEL_PRINTJOB_" + printJobInfo.getId().flattenToString());
        intent.putExtra("EXTRA_PRINT_JOB_ID", printJobInfo.getId());
        return PendingIntent.getBroadcast(this.mContext, 0, intent, 1073741824);
    }

    private PendingIntent createRestartIntent(PrintJobId printJobId) {
        Intent intent = new Intent(this.mContext, (Class<?>) NotificationBroadcastReceiver.class);
        intent.setAction("INTENT_ACTION_RESTART_PRINTJOB_" + printJobId.flattenToString());
        intent.putExtra("EXTRA_PRINT_JOB_ID", printJobId);
        return PendingIntent.getBroadcast(this.mContext, 0, intent, 1073741824);
    }

    private static boolean shouldNotifyForState(int i) {
        switch (i) {
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

    private static int computeNotificationIcon(PrintJobInfo printJobInfo) {
        int state = printJobInfo.getState();
        if (state == 4 || state == 6) {
            return android.R.drawable.ic_media_route_connected_light_04_mtrl;
        }
        if (!printJobInfo.isCancelling()) {
            return android.R.drawable.ic_media_route_connected_light_03_mtrl;
        }
        return R.drawable.stat_notify_cancelling;
    }

    private static String computeChannel(PrintJobInfo printJobInfo) {
        if (printJobInfo.isCancelling()) {
            return "PRINT_PROGRESS";
        }
        int state = printJobInfo.getState();
        if (state == 4 || state == 6) {
            return "PRINT_FAILURES";
        }
        return "PRINT_PROGRESS";
    }

    public static final class NotificationBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.startsWith("INTENT_ACTION_CANCEL_PRINTJOB")) {
                handleCancelPrintJob(context, (PrintJobId) intent.getExtras().getParcelable("EXTRA_PRINT_JOB_ID"));
            } else if (action != null && action.startsWith("INTENT_ACTION_RESTART_PRINTJOB")) {
                handleRestartPrintJob(context, (PrintJobId) intent.getExtras().getParcelable("EXTRA_PRINT_JOB_ID"));
            }
        }

        private void handleCancelPrintJob(Context context, final PrintJobId printJobId) {
            final PowerManager.WakeLock wakeLockNewWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(1, "NotificationBroadcastReceiver");
            wakeLockNewWakeLock.acquire();
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voidArr) {
                    try {
                        try {
                            IPrintManager.Stub.asInterface(ServiceManager.getService("print")).cancelPrintJob(printJobId, -2, UserHandle.myUserId());
                        } catch (RemoteException e) {
                            Log.i("NotificationBroadcastReceiver", "Error requesting print job cancellation", e);
                        }
                        wakeLockNewWakeLock.release();
                        return null;
                    } catch (Throwable th) {
                        wakeLockNewWakeLock.release();
                        throw th;
                    }
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
        }

        private void handleRestartPrintJob(Context context, final PrintJobId printJobId) {
            final PowerManager.WakeLock wakeLockNewWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(1, "NotificationBroadcastReceiver");
            wakeLockNewWakeLock.acquire();
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voidArr) {
                    try {
                        try {
                            IPrintManager.Stub.asInterface(ServiceManager.getService("print")).restartPrintJob(printJobId, -2, UserHandle.myUserId());
                        } catch (RemoteException e) {
                            Log.i("NotificationBroadcastReceiver", "Error requesting print job restart", e);
                        }
                        wakeLockNewWakeLock.release();
                        return null;
                    } catch (Throwable th) {
                        wakeLockNewWakeLock.release();
                        throw th;
                    }
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
        }
    }
}
