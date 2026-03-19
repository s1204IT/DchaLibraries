package com.android.server.am;

import android.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.util.Slog;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.util.ProgressReporter;
import com.android.server.UiThread;
import com.android.server.pm.DumpState;
import java.util.List;

public abstract class PreBootBroadcaster extends IIntentReceiver.Stub {
    private static final int MSG_HIDE = 2;
    private static final int MSG_SHOW = 1;
    private static final String TAG = "PreBootBroadcaster";
    private final ProgressReporter mProgress;
    private final boolean mQuiet;
    private final ActivityManagerService mService;
    private final List<ResolveInfo> mTargets;
    private final int mUserId;
    private int mIndex = 0;
    private Handler mHandler = new Handler(UiThread.get().getLooper(), null, true) {
        @Override
        public void handleMessage(Message message) {
            Context context = PreBootBroadcaster.this.mService.mContext;
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(NotificationManager.class);
            int i = message.arg1;
            int i2 = message.arg2;
            switch (message.what) {
                case 1:
                    CharSequence text = context.getText(R.string.PERSOSUBSTATE_RUIM_NETWORK1_PUK_ENTRY);
                    Intent intent = new Intent();
                    intent.setClassName("com.android.settings", "com.android.settings.HelpTrampoline");
                    intent.putExtra("android.intent.extra.TEXT", "help_url_upgrading");
                    PendingIntent activity = null;
                    if (context.getPackageManager().resolveActivity(intent, 0) != null && BenesseExtension.getDchaState() == 0) {
                        activity = PendingIntent.getActivity(context, 0, intent, 0);
                    }
                    notificationManager.notifyAsUser(PreBootBroadcaster.TAG, 13, new Notification.Builder(PreBootBroadcaster.this.mService.mContext, SystemNotificationChannels.UPDATES).setSmallIcon(R.drawable.pointer_hand_large_icon).setWhen(0L).setOngoing(true).setTicker(text).setColor(context.getColor(R.color.car_colorPrimary)).setContentTitle(text).setContentIntent(activity).setVisibility(1).setProgress(i, i2, false).build(), UserHandle.of(PreBootBroadcaster.this.mUserId));
                    break;
                case 2:
                    notificationManager.cancelAsUser(PreBootBroadcaster.TAG, 13, UserHandle.of(PreBootBroadcaster.this.mUserId));
                    break;
            }
        }
    };
    private final Intent mIntent = new Intent("android.intent.action.PRE_BOOT_COMPLETED");

    public abstract void onFinished();

    public PreBootBroadcaster(ActivityManagerService activityManagerService, int i, ProgressReporter progressReporter, boolean z) {
        this.mService = activityManagerService;
        this.mUserId = i;
        this.mProgress = progressReporter;
        this.mQuiet = z;
        this.mIntent.addFlags(33554688);
        this.mTargets = this.mService.mContext.getPackageManager().queryBroadcastReceiversAsUser(this.mIntent, DumpState.DUMP_DEXOPT, UserHandle.of(i));
    }

    public void sendNext() {
        if (this.mIndex < this.mTargets.size()) {
            if (!this.mService.isUserRunning(this.mUserId, 0)) {
                Slog.i(TAG, "User " + this.mUserId + " is no longer running; skipping remaining receivers");
                this.mHandler.obtainMessage(2).sendToTarget();
                onFinished();
                return;
            }
            if (!this.mQuiet) {
                this.mHandler.obtainMessage(1, this.mTargets.size(), this.mIndex).sendToTarget();
            }
            List<ResolveInfo> list = this.mTargets;
            int i = this.mIndex;
            this.mIndex = i + 1;
            ResolveInfo resolveInfo = list.get(i);
            ComponentName componentName = resolveInfo.activityInfo.getComponentName();
            if (this.mProgress != null) {
                this.mProgress.setProgress(this.mIndex, this.mTargets.size(), this.mService.mContext.getString(R.string.PERSOSUBSTATE_RUIM_HRPD_PUK_IN_PROGRESS, resolveInfo.activityInfo.loadLabel(this.mService.mContext.getPackageManager())));
            }
            Slog.i(TAG, "Pre-boot of " + componentName.toShortString() + " for user " + this.mUserId);
            EventLogTags.writeAmPreBoot(this.mUserId, componentName.getPackageName());
            this.mIntent.setComponent(componentName);
            this.mService.broadcastIntentLocked(null, null, this.mIntent, null, this, 0, null, null, null, -1, null, true, false, ActivityManagerService.MY_PID, 1000, this.mUserId);
            return;
        }
        this.mHandler.obtainMessage(2).sendToTarget();
        onFinished();
    }

    public void performReceive(Intent intent, int i, String str, Bundle bundle, boolean z, boolean z2, int i2) {
        sendNext();
    }
}
