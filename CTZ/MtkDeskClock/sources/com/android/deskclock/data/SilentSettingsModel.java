package com.android.deskclock.data;

import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.NotificationManagerCompat;
import com.android.deskclock.Utils;
import com.android.deskclock.data.DataModel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class SilentSettingsModel {
    private static final Uri VOLUME_URI = Uri.withAppendedPath(Settings.System.CONTENT_URI, "volume_alarm_speaker");
    private final AudioManager mAudioManager;
    private CheckSilenceSettingsTask mCheckSilenceSettingsTask;
    private final Context mContext;
    private final List<OnSilentSettingsListener> mListeners = new ArrayList(1);
    private final NotificationManager mNotificationManager;
    private final NotificationModel mNotificationModel;
    private DataModel.SilentSetting mSilentSetting;

    SilentSettingsModel(Context context, NotificationModel notificationModel) {
        this.mContext = context;
        this.mNotificationModel = notificationModel;
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
        this.mNotificationManager = (NotificationManager) context.getSystemService("notification");
        ContentResolver contentResolver = context.getContentResolver();
        ContentChangeWatcher contentChangeWatcher = new ContentChangeWatcher();
        contentResolver.registerContentObserver(VOLUME_URI, false, contentChangeWatcher);
        contentResolver.registerContentObserver(Settings.System.DEFAULT_ALARM_ALERT_URI, false, contentChangeWatcher);
        if (Utils.isMOrLater()) {
            context.registerReceiver(new DoNotDisturbChangeReceiver(), new IntentFilter("android.app.action.INTERRUPTION_FILTER_CHANGED"));
        }
    }

    void addSilentSettingsListener(OnSilentSettingsListener onSilentSettingsListener) {
        this.mListeners.add(onSilentSettingsListener);
    }

    void removeSilentSettingsListener(OnSilentSettingsListener onSilentSettingsListener) {
        this.mListeners.remove(onSilentSettingsListener);
    }

    void updateSilentState() {
        if (this.mCheckSilenceSettingsTask != null) {
            this.mCheckSilenceSettingsTask.cancel(true);
            this.mCheckSilenceSettingsTask = null;
        }
        if (this.mNotificationModel.isApplicationInForeground()) {
            this.mCheckSilenceSettingsTask = new CheckSilenceSettingsTask();
            this.mCheckSilenceSettingsTask.execute(new Void[0]);
        } else {
            setSilentState(null);
        }
    }

    private void setSilentState(DataModel.SilentSetting silentSetting) {
        if (this.mSilentSetting != silentSetting) {
            DataModel.SilentSetting silentSetting2 = this.mSilentSetting;
            this.mSilentSetting = silentSetting;
            Iterator<OnSilentSettingsListener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onSilentSettingsChange(silentSetting2, silentSetting);
            }
        }
    }

    private final class CheckSilenceSettingsTask extends AsyncTask<Void, Void, DataModel.SilentSetting> {
        private CheckSilenceSettingsTask() {
        }

        @Override
        protected DataModel.SilentSetting doInBackground(Void... voidArr) {
            if (!isCancelled() && isDoNotDisturbBlockingAlarms()) {
                return DataModel.SilentSetting.DO_NOT_DISTURB;
            }
            if (!isCancelled() && isAlarmStreamMuted()) {
                return DataModel.SilentSetting.MUTED_VOLUME;
            }
            if (!isCancelled() && isSystemAlarmRingtoneSilent()) {
                return DataModel.SilentSetting.SILENT_RINGTONE;
            }
            if (!isCancelled() && isAppNotificationBlocked()) {
                return DataModel.SilentSetting.BLOCKED_NOTIFICATIONS;
            }
            return null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            if (SilentSettingsModel.this.mCheckSilenceSettingsTask == this) {
                SilentSettingsModel.this.mCheckSilenceSettingsTask = null;
            }
        }

        @Override
        protected void onPostExecute(DataModel.SilentSetting silentSetting) {
            if (SilentSettingsModel.this.mCheckSilenceSettingsTask == this) {
                SilentSettingsModel.this.mCheckSilenceSettingsTask = null;
                SilentSettingsModel.this.setSilentState(silentSetting);
            }
        }

        @TargetApi(23)
        private boolean isDoNotDisturbBlockingAlarms() {
            if (!Utils.isMOrLater()) {
                return false;
            }
            try {
                return SilentSettingsModel.this.mNotificationManager.getCurrentInterruptionFilter() == 3;
            } catch (Exception e) {
                return false;
            }
        }

        private boolean isAlarmStreamMuted() {
            try {
                return SilentSettingsModel.this.mAudioManager.getStreamVolume(4) <= 0;
            } catch (Exception e) {
                return false;
            }
        }

        private boolean isSystemAlarmRingtoneSilent() {
            try {
                return RingtoneManager.getActualDefaultRingtoneUri(SilentSettingsModel.this.mContext, 4) == null;
            } catch (Exception e) {
                return false;
            }
        }

        private boolean isAppNotificationBlocked() {
            try {
                return !NotificationManagerCompat.from(SilentSettingsModel.this.mContext).areNotificationsEnabled();
            } catch (Exception e) {
                return false;
            }
        }
    }

    private final class ContentChangeWatcher extends ContentObserver {
        private ContentChangeWatcher() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean z) {
            SilentSettingsModel.this.updateSilentState();
        }
    }

    private final class DoNotDisturbChangeReceiver extends BroadcastReceiver {
        private DoNotDisturbChangeReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            SilentSettingsModel.this.updateSilentState();
        }
    }
}
