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
import com.android.internal.util.ProgressReporter;
import com.android.server.UiThread;
import com.android.server.pm.PackageManagerService;
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
        public void handleMessage(Message msg) {
            Context context = PreBootBroadcaster.this.mService.mContext;
            NotificationManager notifManager = (NotificationManager) context.getSystemService(NotificationManager.class);
            int max = msg.arg1;
            int index = msg.arg2;
            switch (msg.what) {
                case 1:
                    CharSequence title = context.getText(R.string.enable_explore_by_touch_warning_title);
                    Intent intent = new Intent();
                    intent.setClassName("com.android.settings", "com.android.settings.HelpTrampoline");
                    intent.putExtra("android.intent.extra.TEXT", "help_url_upgrading");
                    Notification notif = new Notification.Builder(PreBootBroadcaster.this.mService.mContext).setSmallIcon(R.drawable.list_selector_background_longpress_light).setWhen(0L).setOngoing(true).setTicker(title).setDefaults(0).setPriority(2).setColor(context.getColor(R.color.system_accent3_600)).setContentTitle(title).setContentIntent((context.getPackageManager().resolveActivity(intent, 0) == null || BenesseExtension.getDchaState() != 0) ? null : PendingIntent.getActivity(context, 0, intent, 0)).setVisibility(1).setProgress(max, index, false).build();
                    notifManager.notifyAsUser(PreBootBroadcaster.TAG, 0, notif, UserHandle.of(PreBootBroadcaster.this.mUserId));
                    break;
                case 2:
                    notifManager.cancelAsUser(PreBootBroadcaster.TAG, 0, UserHandle.of(PreBootBroadcaster.this.mUserId));
                    break;
            }
        }
    };
    private final Intent mIntent = new Intent("android.intent.action.PRE_BOOT_COMPLETED");

    public abstract void onFinished();

    public PreBootBroadcaster(ActivityManagerService service, int userId, ProgressReporter progress, boolean quiet) {
        this.mService = service;
        this.mUserId = userId;
        this.mProgress = progress;
        this.mQuiet = quiet;
        this.mIntent.addFlags(33554688);
        this.mTargets = this.mService.mContext.getPackageManager().queryBroadcastReceiversAsUser(this.mIntent, PackageManagerService.DumpState.DUMP_DEXOPT, UserHandle.of(userId));
    }

    public void sendNext() {
        if (this.mIndex >= this.mTargets.size()) {
            this.mHandler.obtainMessage(2).sendToTarget();
            onFinished();
            return;
        }
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
        ResolveInfo ri = list.get(i);
        ComponentName componentName = ri.activityInfo.getComponentName();
        if (this.mProgress != null) {
            CharSequence label = ri.activityInfo.loadLabel(this.mService.mContext.getPackageManager());
            this.mProgress.setProgress(this.mIndex, this.mTargets.size(), this.mService.mContext.getString(R.string.etws_primary_default_message_others, label));
        }
        Slog.i(TAG, "Pre-boot of " + componentName.toShortString() + " for user " + this.mUserId);
        EventLogTags.writeAmPreBoot(this.mUserId, componentName.getPackageName());
        this.mIntent.setComponent(componentName);
        this.mService.broadcastIntentLocked(null, null, this.mIntent, null, this, 0, null, null, null, -1, null, true, false, ActivityManagerService.MY_PID, 1000, this.mUserId);
    }

    public void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser) {
        sendNext();
    }
}
