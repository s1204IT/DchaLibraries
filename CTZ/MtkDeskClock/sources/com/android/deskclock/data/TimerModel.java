package com.android.deskclock.data;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.StringRes;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.ArraySet;
import com.android.deskclock.AlarmAlertWakeLock;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.data.Timer;
import com.android.deskclock.events.Events;
import com.android.deskclock.settings.SettingsActivity;
import com.android.deskclock.timer.TimerKlaxon;
import com.android.deskclock.timer.TimerService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

final class TimerModel {
    private static final long MISSED_THRESHOLD = -60000;
    private final AlarmManager mAlarmManager;
    private final Context mContext;
    private List<Timer> mExpiredTimers;
    private final BroadcastReceiver mLocaleChangedReceiver;
    private List<Timer> mMissedTimers;
    private final NotificationManagerCompat mNotificationManager;
    private final NotificationModel mNotificationModel;
    private final SharedPreferences.OnSharedPreferenceChangeListener mPreferenceListener;
    private final SharedPreferences mPrefs;
    private final RingtoneModel mRingtoneModel;
    private Service mService;
    private final SettingsModel mSettingsModel;
    private String mTimerRingtoneTitle;
    private Uri mTimerRingtoneUri;
    private List<Timer> mTimers;
    private final List<TimerListener> mTimerListeners = new ArrayList();
    private final TimerNotificationBuilder mNotificationBuilder = new TimerNotificationBuilder();

    @SuppressLint({"NewApi"})
    private final Set<Integer> mRingingIds = new ArraySet();

    TimerModel(Context context, SharedPreferences sharedPreferences, SettingsModel settingsModel, RingtoneModel ringtoneModel, NotificationModel notificationModel) {
        this.mLocaleChangedReceiver = new LocaleChangedReceiver();
        this.mPreferenceListener = new PreferenceListener();
        this.mContext = context;
        this.mPrefs = sharedPreferences;
        this.mSettingsModel = settingsModel;
        this.mRingtoneModel = ringtoneModel;
        this.mNotificationModel = notificationModel;
        this.mNotificationManager = NotificationManagerCompat.from(context);
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService(NotificationCompat.CATEGORY_ALARM);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this.mPreferenceListener);
        this.mContext.registerReceiver(this.mLocaleChangedReceiver, new IntentFilter("android.intent.action.LOCALE_CHANGED"));
    }

    void addTimerListener(TimerListener timerListener) {
        this.mTimerListeners.add(timerListener);
    }

    void removeTimerListener(TimerListener timerListener) {
        this.mTimerListeners.remove(timerListener);
    }

    List<Timer> getTimers() {
        return Collections.unmodifiableList(getMutableTimers());
    }

    List<Timer> getExpiredTimers() {
        return Collections.unmodifiableList(getMutableExpiredTimers());
    }

    private List<Timer> getMissedTimers() {
        return Collections.unmodifiableList(getMutableMissedTimers());
    }

    Timer getTimer(int i) {
        for (Timer timer : getMutableTimers()) {
            if (timer.getId() == i) {
                return timer;
            }
        }
        return null;
    }

    Timer getMostRecentExpiredTimer() {
        List<Timer> mutableExpiredTimers = getMutableExpiredTimers();
        if (mutableExpiredTimers.isEmpty()) {
            return null;
        }
        return mutableExpiredTimers.get(mutableExpiredTimers.size() - 1);
    }

    Timer addTimer(long j, String str, boolean z) {
        Timer timerAddTimer = TimerDAO.addTimer(this.mPrefs, new Timer(-1, Timer.State.RESET, j, j, Long.MIN_VALUE, Long.MIN_VALUE, j, str, z));
        getMutableTimers().add(0, timerAddTimer);
        updateNotification();
        Iterator<TimerListener> it = this.mTimerListeners.iterator();
        while (it.hasNext()) {
            it.next().timerAdded(timerAddTimer);
        }
        return timerAddTimer;
    }

    void expireTimer(Service service, Timer timer) {
        if (this.mService == null) {
            this.mService = service;
        } else if (this.mService != service) {
            LogUtils.wtf("Expected TimerServices to be identical", new Object[0]);
        }
        updateTimer(timer.expire());
    }

    void updateTimer(Timer timer) {
        Timer timerDoUpdateTimer = doUpdateTimer(timer);
        updateNotification();
        if (timerDoUpdateTimer.getState() != timer.getState()) {
            if (timerDoUpdateTimer.isExpired() || timer.isExpired()) {
                updateHeadsUpNotification();
            }
        }
    }

    void removeTimer(Timer timer) {
        doRemoveTimer(timer);
        if (timer.isExpired()) {
            updateHeadsUpNotification();
        } else {
            updateNotification();
        }
    }

    Timer resetTimer(Timer timer, boolean z, @StringRes int i) {
        Timer timerDoResetOrDeleteTimer = doResetOrDeleteTimer(timer, z, i);
        if (timer.isMissed()) {
            updateMissedNotification();
        } else if (timer.isExpired()) {
            updateHeadsUpNotification();
        } else {
            updateNotification();
        }
        return timerDoResetOrDeleteTimer;
    }

    void updateTimersAfterReboot() {
        Iterator it = new ArrayList(getTimers()).iterator();
        while (it.hasNext()) {
            doUpdateAfterRebootTimer((Timer) it.next());
        }
        updateNotification();
        updateMissedNotification();
        updateHeadsUpNotification();
    }

    void updateTimersAfterTimeSet() {
        Iterator it = new ArrayList(getTimers()).iterator();
        while (it.hasNext()) {
            doUpdateAfterTimeSetTimer((Timer) it.next());
        }
        updateNotification();
        updateMissedNotification();
        updateHeadsUpNotification();
    }

    void resetExpiredTimers(@StringRes int i) {
        for (Timer timer : new ArrayList(getTimers())) {
            if (timer.isExpired()) {
                doResetOrDeleteTimer(timer, true, i);
            }
        }
        updateHeadsUpNotification();
    }

    void resetMissedTimers(@StringRes int i) {
        for (Timer timer : new ArrayList(getTimers())) {
            if (timer.isMissed()) {
                doResetOrDeleteTimer(timer, true, i);
            }
        }
        updateMissedNotification();
    }

    void resetUnexpiredTimers(@StringRes int i) {
        for (Timer timer : new ArrayList(getTimers())) {
            if (timer.isRunning() || timer.isPaused()) {
                doResetOrDeleteTimer(timer, true, i);
            }
        }
        updateNotification();
    }

    Uri getDefaultTimerRingtoneUri() {
        return this.mSettingsModel.getDefaultTimerRingtoneUri();
    }

    boolean isTimerRingtoneSilent() {
        return Uri.EMPTY.equals(getTimerRingtoneUri());
    }

    Uri getTimerRingtoneUri() {
        if (this.mTimerRingtoneUri == null) {
            this.mTimerRingtoneUri = this.mSettingsModel.getTimerRingtoneUri();
        }
        return this.mTimerRingtoneUri;
    }

    void setTimerRingtoneUri(Uri uri) {
        this.mSettingsModel.setTimerRingtoneUri(uri);
    }

    String getTimerRingtoneTitle() {
        if (this.mTimerRingtoneTitle == null) {
            if (isTimerRingtoneSilent()) {
                this.mTimerRingtoneTitle = this.mContext.getString(R.string.silent_ringtone_title);
            } else {
                Uri defaultTimerRingtoneUri = getDefaultTimerRingtoneUri();
                Uri timerRingtoneUri = getTimerRingtoneUri();
                if (defaultTimerRingtoneUri.equals(timerRingtoneUri)) {
                    this.mTimerRingtoneTitle = this.mContext.getString(R.string.default_timer_ringtone_title);
                } else {
                    this.mTimerRingtoneTitle = this.mRingtoneModel.getRingtoneTitle(timerRingtoneUri);
                }
            }
        }
        return this.mTimerRingtoneTitle;
    }

    long getTimerCrescendoDuration() {
        return this.mSettingsModel.getTimerCrescendoDuration();
    }

    boolean getTimerVibrate() {
        return this.mSettingsModel.getTimerVibrate();
    }

    void setTimerVibrate(boolean z) {
        this.mSettingsModel.setTimerVibrate(z);
    }

    private List<Timer> getMutableTimers() {
        if (this.mTimers == null) {
            this.mTimers = TimerDAO.getTimers(this.mPrefs);
            Collections.sort(this.mTimers, Timer.ID_COMPARATOR);
        }
        return this.mTimers;
    }

    private List<Timer> getMutableExpiredTimers() {
        if (this.mExpiredTimers == null) {
            this.mExpiredTimers = new ArrayList();
            for (Timer timer : getMutableTimers()) {
                if (timer.isExpired()) {
                    this.mExpiredTimers.add(timer);
                }
            }
            Collections.sort(this.mExpiredTimers, Timer.EXPIRY_COMPARATOR);
        }
        return this.mExpiredTimers;
    }

    private List<Timer> getMutableMissedTimers() {
        if (this.mMissedTimers == null) {
            this.mMissedTimers = new ArrayList();
            for (Timer timer : getMutableTimers()) {
                if (timer.isMissed()) {
                    this.mMissedTimers.add(timer);
                }
            }
            Collections.sort(this.mMissedTimers, Timer.EXPIRY_COMPARATOR);
        }
        return this.mMissedTimers;
    }

    private Timer doUpdateTimer(Timer timer) {
        List<Timer> mutableTimers = getMutableTimers();
        int iIndexOf = mutableTimers.indexOf(timer);
        Timer timer2 = mutableTimers.get(iIndexOf);
        if (timer == timer2) {
            return timer;
        }
        TimerDAO.updateTimer(this.mPrefs, timer);
        Timer timer3 = mutableTimers.set(iIndexOf, timer);
        if (timer2.isExpired() || timer.isExpired()) {
            this.mExpiredTimers = null;
        }
        if (timer2.isMissed() || timer.isMissed()) {
            this.mMissedTimers = null;
        }
        updateAlarmManager();
        updateRinger(timer2, timer);
        Iterator<TimerListener> it = this.mTimerListeners.iterator();
        while (it.hasNext()) {
            it.next().timerUpdated(timer2, timer);
        }
        return timer3;
    }

    private void doRemoveTimer(Timer timer) {
        TimerDAO.removeTimer(this.mPrefs, timer);
        List<Timer> mutableTimers = getMutableTimers();
        int iIndexOf = mutableTimers.indexOf(timer);
        if (iIndexOf == -1) {
            return;
        }
        Timer timerRemove = mutableTimers.remove(iIndexOf);
        if (timerRemove.isExpired()) {
            this.mExpiredTimers = null;
        }
        if (timerRemove.isMissed()) {
            this.mMissedTimers = null;
        }
        updateAlarmManager();
        updateRinger(timerRemove, null);
        Iterator<TimerListener> it = this.mTimerListeners.iterator();
        while (it.hasNext()) {
            it.next().timerRemoved(timerRemove);
        }
    }

    private Timer doResetOrDeleteTimer(Timer timer, boolean z, @StringRes int i) {
        if (z && ((timer.isExpired() || timer.isMissed()) && timer.getDeleteAfterUse())) {
            doRemoveTimer(timer);
            if (i != 0) {
                Events.sendTimerEvent(R.string.action_delete, i);
                return null;
            }
            return null;
        }
        if (!timer.isReset()) {
            Timer timerReset = timer.reset();
            doUpdateTimer(timerReset);
            if (i != 0) {
                Events.sendTimerEvent(R.string.action_reset, i);
            }
            return timerReset;
        }
        return timer;
    }

    private void doUpdateAfterRebootTimer(Timer timer) {
        Timer timerUpdateAfterReboot = timer.updateAfterReboot();
        if (timerUpdateAfterReboot.getRemainingTime() < MISSED_THRESHOLD && timerUpdateAfterReboot.isRunning()) {
            timerUpdateAfterReboot = timerUpdateAfterReboot.miss();
        }
        doUpdateTimer(timerUpdateAfterReboot);
    }

    private void doUpdateAfterTimeSetTimer(Timer timer) {
        doUpdateTimer(timer.updateAfterTimeSet());
    }

    private void updateAlarmManager() {
        Timer timer = null;
        for (Timer timer2 : getMutableTimers()) {
            if (timer2.isRunning() && (timer == null || timer2.getExpirationTime() < timer.getExpirationTime())) {
                timer = timer2;
            }
        }
        Intent intentCreateTimerExpiredIntent = TimerService.createTimerExpiredIntent(this.mContext, timer);
        if (timer == null) {
            PendingIntent service = PendingIntent.getService(this.mContext, 0, intentCreateTimerExpiredIntent, 1610612736);
            if (service != null) {
                this.mAlarmManager.cancel(service);
                service.cancel();
                return;
            }
            return;
        }
        schedulePendingIntent(this.mAlarmManager, timer.getExpirationTime(), PendingIntent.getService(this.mContext, 0, intentCreateTimerExpiredIntent, 1207959552));
    }

    private void updateRinger(Timer timer, Timer timer2) {
        Timer.State state;
        if (timer != null) {
            state = timer.getState();
        } else {
            state = null;
        }
        Timer.State state2 = timer2 != null ? timer2.getState() : null;
        if (state == state2) {
            return;
        }
        if (state2 == Timer.State.EXPIRED && this.mRingingIds.add(Integer.valueOf(timer2.getId())) && this.mRingingIds.size() == 1) {
            AlarmAlertWakeLock.acquireScreenCpuWakeLock(this.mContext);
            TimerKlaxon.start(this.mContext);
        }
        if (state == Timer.State.EXPIRED && this.mRingingIds.remove(Integer.valueOf(timer.getId())) && this.mRingingIds.isEmpty()) {
            TimerKlaxon.stop(this.mContext);
            AlarmAlertWakeLock.releaseCpuLock();
        }
    }

    void updateNotification() {
        if (this.mNotificationModel.isApplicationInForeground()) {
            this.mNotificationManager.cancel(this.mNotificationModel.getUnexpiredTimerNotificationId());
            return;
        }
        ArrayList arrayList = new ArrayList();
        for (Timer timer : getMutableTimers()) {
            if (timer.isRunning() || timer.isPaused()) {
                arrayList.add(timer);
            }
        }
        if (arrayList.isEmpty()) {
            this.mNotificationManager.cancel(this.mNotificationModel.getUnexpiredTimerNotificationId());
            return;
        }
        Collections.sort(arrayList, Timer.EXPIRY_COMPARATOR);
        Notification notificationBuild = this.mNotificationBuilder.build(this.mContext, this.mNotificationModel, arrayList);
        this.mNotificationManager.notify(this.mNotificationModel.getUnexpiredTimerNotificationId(), notificationBuild);
    }

    void updateMissedNotification() {
        if (this.mNotificationModel.isApplicationInForeground()) {
            this.mNotificationManager.cancel(this.mNotificationModel.getMissedTimerNotificationId());
            return;
        }
        List<Timer> missedTimers = getMissedTimers();
        if (missedTimers.isEmpty()) {
            this.mNotificationManager.cancel(this.mNotificationModel.getMissedTimerNotificationId());
            return;
        }
        Notification notificationBuildMissed = this.mNotificationBuilder.buildMissed(this.mContext, this.mNotificationModel, missedTimers);
        this.mNotificationManager.notify(this.mNotificationModel.getMissedTimerNotificationId(), notificationBuildMissed);
    }

    private void updateHeadsUpNotification() {
        if (this.mService == null) {
            return;
        }
        List<Timer> expiredTimers = getExpiredTimers();
        if (expiredTimers.isEmpty()) {
            this.mService.stopSelf();
            this.mService = null;
        } else {
            Notification notificationBuildHeadsUp = this.mNotificationBuilder.buildHeadsUp(this.mContext, expiredTimers);
            this.mService.startForeground(this.mNotificationModel.getExpiredTimerNotificationId(), notificationBuildHeadsUp);
        }
    }

    private final class LocaleChangedReceiver extends BroadcastReceiver {
        private LocaleChangedReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            TimerModel.this.mTimerRingtoneTitle = null;
            TimerModel.this.updateNotification();
            TimerModel.this.updateMissedNotification();
            TimerModel.this.updateHeadsUpNotification();
        }
    }

    private final class PreferenceListener implements SharedPreferences.OnSharedPreferenceChangeListener {
        private PreferenceListener() {
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String str) {
            if (((str.hashCode() == -1249918116 && str.equals(SettingsActivity.KEY_TIMER_RINGTONE)) ? (byte) 0 : (byte) -1) == 0) {
                TimerModel.this.mTimerRingtoneUri = null;
                TimerModel.this.mTimerRingtoneTitle = null;
            }
        }
    }

    static void schedulePendingIntent(AlarmManager alarmManager, long j, PendingIntent pendingIntent) {
        if (Utils.isMOrLater()) {
            alarmManager.setExactAndAllowWhileIdle(2, j, pendingIntent);
        } else {
            alarmManager.setExact(2, j, pendingIntent);
        }
    }
}
