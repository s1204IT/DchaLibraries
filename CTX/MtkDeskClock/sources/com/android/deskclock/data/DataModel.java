package com.android.deskclock.data;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.StringRes;
import android.view.View;
import com.android.deskclock.Predicate;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.data.Weekdays;
import com.android.deskclock.timer.TimerService;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public final class DataModel {
    public static final String ACTION_WORLD_CITIES_CHANGED = "com.android.deskclock.WORLD_CITIES_CHANGED";
    private static final DataModel sDataModel = new DataModel();
    private AlarmModel mAlarmModel;
    private CityModel mCityModel;
    private Context mContext;
    private Handler mHandler;
    private NotificationModel mNotificationModel;
    private RingtoneModel mRingtoneModel;
    private SettingsModel mSettingsModel;
    private SilentSettingsModel mSilentSettingsModel;
    private StopwatchModel mStopwatchModel;
    private TimeModel mTimeModel;
    private TimerModel mTimerModel;
    private WidgetModel mWidgetModel;

    public enum AlarmVolumeButtonBehavior {
        NOTHING,
        SNOOZE,
        DISMISS
    }

    public enum CitySort {
        NAME,
        UTC_OFFSET
    }

    public enum ClockStyle {
        ANALOG,
        DIGITAL
    }

    public static final class SilentSetting {
        private static final SilentSetting[] $VALUES;
        public static final SilentSetting BLOCKED_NOTIFICATIONS;
        public static final SilentSetting DO_NOT_DISTURB = new SilentSetting("DO_NOT_DISTURB", 0, R.string.alarms_blocked_by_dnd, 0, Predicate.FALSE, null);
        public static final SilentSetting MUTED_VOLUME;
        public static final SilentSetting SILENT_RINGTONE;
        private final Predicate<Context> mActionEnabled;
        private final View.OnClickListener mActionListener;

        @StringRes
        private final int mActionResId;

        @StringRes
        private final int mLabelResId;

        public static SilentSetting valueOf(String str) {
            return (SilentSetting) Enum.valueOf(SilentSetting.class, str);
        }

        public static SilentSetting[] values() {
            return (SilentSetting[]) $VALUES.clone();
        }

        static {
            MUTED_VOLUME = new SilentSetting("MUTED_VOLUME", 1, R.string.alarm_volume_muted, R.string.unmute_alarm_volume, Predicate.TRUE, new UnmuteAlarmVolumeListener());
            SILENT_RINGTONE = new SilentSetting("SILENT_RINGTONE", 2, R.string.silent_default_alarm_ringtone, R.string.change_setting_action, new ChangeSoundActionPredicate(), new ChangeSoundSettingsListener());
            BLOCKED_NOTIFICATIONS = new SilentSetting("BLOCKED_NOTIFICATIONS", 3, R.string.app_notifications_blocked, R.string.change_setting_action, Predicate.TRUE, new ChangeAppNotificationSettingsListener());
            $VALUES = new SilentSetting[]{DO_NOT_DISTURB, MUTED_VOLUME, SILENT_RINGTONE, BLOCKED_NOTIFICATIONS};
        }

        private SilentSetting(String str, int i, int i2, int i3, Predicate predicate, View.OnClickListener onClickListener) {
            this.mLabelResId = i2;
            this.mActionResId = i3;
            this.mActionEnabled = predicate;
            this.mActionListener = onClickListener;
        }

        @StringRes
        public int getLabelResId() {
            return this.mLabelResId;
        }

        @StringRes
        public int getActionResId() {
            return this.mActionResId;
        }

        public View.OnClickListener getActionListener() {
            return this.mActionListener;
        }

        public boolean isActionEnabled(Context context) {
            return this.mLabelResId != 0 && this.mActionEnabled.apply(context);
        }

        private static class UnmuteAlarmVolumeListener implements View.OnClickListener {
            private UnmuteAlarmVolumeListener() {
            }

            @Override
            public void onClick(View view) {
                ((AudioManager) view.getContext().getSystemService("audio")).setStreamVolume(4, Math.round((r4.getStreamMaxVolume(4) * 11.0f) / 16.0f), 1);
            }
        }

        private static class ChangeSoundSettingsListener implements View.OnClickListener {
            private ChangeSoundSettingsListener() {
            }

            @Override
            public void onClick(View view) {
                if (BenesseExtension.getDchaState() != 0) {
                    return;
                }
                view.getContext().startActivity(new Intent("android.settings.SOUND_SETTINGS").addFlags(268435456));
            }
        }

        private static class ChangeSoundActionPredicate implements Predicate<Context> {
            private ChangeSoundActionPredicate() {
            }

            @Override
            public boolean apply(Context context) {
                return new Intent("android.settings.SOUND_SETTINGS").resolveActivity(context.getPackageManager()) != null;
            }
        }

        private static class ChangeAppNotificationSettingsListener implements View.OnClickListener {
            private ChangeAppNotificationSettingsListener() {
            }

            @Override
            public void onClick(View view) {
                if (BenesseExtension.getDchaState() != 0) {
                    return;
                }
                Context context = view.getContext();
                if (Utils.isLOrLater()) {
                    try {
                        context.startActivity(new Intent("android.settings.APP_NOTIFICATION_SETTINGS").putExtra("app_package", context.getPackageName()).putExtra("app_uid", context.getApplicationInfo().uid).addFlags(268435456));
                        return;
                    } catch (Exception e) {
                    }
                }
                context.startActivity(new Intent("android.settings.APPLICATION_DETAILS_SETTINGS").setData(Uri.fromParts("package", context.getPackageName(), null)).addFlags(268435456));
            }
        }
    }

    public static DataModel getDataModel() {
        return sDataModel;
    }

    private DataModel() {
    }

    public void init(Context context, SharedPreferences sharedPreferences) {
        if (this.mContext != context) {
            this.mContext = context.getApplicationContext();
            this.mTimeModel = new TimeModel(this.mContext);
            this.mWidgetModel = new WidgetModel(sharedPreferences);
            this.mNotificationModel = new NotificationModel();
            this.mRingtoneModel = new RingtoneModel(this.mContext, sharedPreferences);
            this.mSettingsModel = new SettingsModel(this.mContext, sharedPreferences, this.mTimeModel);
            this.mCityModel = new CityModel(this.mContext, sharedPreferences, this.mSettingsModel);
            this.mAlarmModel = new AlarmModel(this.mContext, this.mSettingsModel);
            this.mSilentSettingsModel = new SilentSettingsModel(this.mContext, this.mNotificationModel);
            this.mStopwatchModel = new StopwatchModel(this.mContext, sharedPreferences, this.mNotificationModel);
            this.mTimerModel = new TimerModel(this.mContext, sharedPreferences, this.mSettingsModel, this.mRingtoneModel, this.mNotificationModel);
        }
    }

    public void run(Runnable runnable) {
        try {
            run(runnable, 0L);
        } catch (InterruptedException e) {
        }
    }

    public void updateAfterReboot() {
        Utils.enforceMainLooper();
        this.mTimerModel.updateTimersAfterReboot();
        this.mStopwatchModel.setStopwatch(getStopwatch().updateAfterReboot());
    }

    public void updateAfterTimeSet() {
        Utils.enforceMainLooper();
        this.mTimerModel.updateTimersAfterTimeSet();
        this.mStopwatchModel.setStopwatch(getStopwatch().updateAfterTimeSet());
    }

    public void run(Runnable runnable, long j) throws InterruptedException {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
            return;
        }
        ExecutedRunnable executedRunnable = new ExecutedRunnable(runnable);
        getHandler().post(executedRunnable);
        synchronized (executedRunnable) {
            if (!executedRunnable.isExecuted()) {
                executedRunnable.wait(j);
            }
        }
    }

    private synchronized Handler getHandler() {
        if (this.mHandler == null) {
            this.mHandler = new Handler(Looper.getMainLooper());
        }
        return this.mHandler;
    }

    public void setApplicationInForeground(boolean z) {
        Utils.enforceMainLooper();
        if (this.mNotificationModel.isApplicationInForeground() != z) {
            this.mNotificationModel.setApplicationInForeground(z);
            this.mTimerModel.updateNotification();
            this.mTimerModel.updateMissedNotification();
            this.mStopwatchModel.updateNotification();
            this.mSilentSettingsModel.updateSilentState();
        }
    }

    public boolean isApplicationInForeground() {
        Utils.enforceMainLooper();
        return this.mNotificationModel.isApplicationInForeground();
    }

    public void updateAllNotifications() {
        Utils.enforceMainLooper();
        this.mTimerModel.updateNotification();
        this.mTimerModel.updateMissedNotification();
        this.mStopwatchModel.updateNotification();
    }

    public List<City> getAllCities() {
        Utils.enforceMainLooper();
        return this.mCityModel.getAllCities();
    }

    public City getHomeCity() {
        Utils.enforceMainLooper();
        return this.mCityModel.getHomeCity();
    }

    public List<City> getUnselectedCities() {
        Utils.enforceMainLooper();
        return this.mCityModel.getUnselectedCities();
    }

    public List<City> getSelectedCities() {
        Utils.enforceMainLooper();
        return this.mCityModel.getSelectedCities();
    }

    public void setSelectedCities(Collection<City> collection) {
        Utils.enforceMainLooper();
        this.mCityModel.setSelectedCities(collection);
    }

    public Comparator<City> getCityIndexComparator() {
        Utils.enforceMainLooper();
        return this.mCityModel.getCityIndexComparator();
    }

    public CitySort getCitySort() {
        Utils.enforceMainLooper();
        return this.mCityModel.getCitySort();
    }

    public void toggleCitySort() {
        Utils.enforceMainLooper();
        this.mCityModel.toggleCitySort();
    }

    public void addCityListener(CityListener cityListener) {
        Utils.enforceMainLooper();
        this.mCityModel.addCityListener(cityListener);
    }

    public void removeCityListener(CityListener cityListener) {
        Utils.enforceMainLooper();
        this.mCityModel.removeCityListener(cityListener);
    }

    public void addTimerListener(TimerListener timerListener) {
        Utils.enforceMainLooper();
        this.mTimerModel.addTimerListener(timerListener);
    }

    public void removeTimerListener(TimerListener timerListener) {
        Utils.enforceMainLooper();
        this.mTimerModel.removeTimerListener(timerListener);
    }

    public List<Timer> getTimers() {
        Utils.enforceMainLooper();
        return this.mTimerModel.getTimers();
    }

    public List<Timer> getExpiredTimers() {
        Utils.enforceMainLooper();
        return this.mTimerModel.getExpiredTimers();
    }

    public Timer getTimer(int i) {
        Utils.enforceMainLooper();
        return this.mTimerModel.getTimer(i);
    }

    public Timer getMostRecentExpiredTimer() {
        Utils.enforceMainLooper();
        return this.mTimerModel.getMostRecentExpiredTimer();
    }

    public Timer addTimer(long j, String str, boolean z) {
        Utils.enforceMainLooper();
        return this.mTimerModel.addTimer(j, str, z);
    }

    public void removeTimer(Timer timer) {
        Utils.enforceMainLooper();
        this.mTimerModel.removeTimer(timer);
    }

    public void startTimer(Timer timer) {
        startTimer(null, timer);
    }

    public void startTimer(Service service, Timer timer) {
        Utils.enforceMainLooper();
        Timer timerStart = timer.start();
        this.mTimerModel.updateTimer(timerStart);
        if (timer.getRemainingTime() <= 0) {
            if (service != null) {
                expireTimer(service, timerStart);
            } else {
                this.mContext.startService(TimerService.createTimerExpiredIntent(this.mContext, timerStart));
            }
        }
    }

    public void pauseTimer(Timer timer) {
        Utils.enforceMainLooper();
        this.mTimerModel.updateTimer(timer.pause());
    }

    public void expireTimer(Service service, Timer timer) {
        Utils.enforceMainLooper();
        this.mTimerModel.expireTimer(service, timer);
    }

    public Timer resetTimer(Timer timer) {
        Utils.enforceMainLooper();
        return this.mTimerModel.resetTimer(timer, false, 0);
    }

    public Timer resetOrDeleteTimer(Timer timer, @StringRes int i) {
        Utils.enforceMainLooper();
        return this.mTimerModel.resetTimer(timer, true, i);
    }

    public void resetExpiredTimers(@StringRes int i) {
        Utils.enforceMainLooper();
        this.mTimerModel.resetExpiredTimers(i);
    }

    public void resetUnexpiredTimers(@StringRes int i) {
        Utils.enforceMainLooper();
        this.mTimerModel.resetUnexpiredTimers(i);
    }

    public void resetMissedTimers(@StringRes int i) {
        Utils.enforceMainLooper();
        this.mTimerModel.resetMissedTimers(i);
    }

    public void addTimerMinute(Timer timer) {
        Utils.enforceMainLooper();
        this.mTimerModel.updateTimer(timer.addMinute());
    }

    public void setTimerLabel(Timer timer, String str) {
        Utils.enforceMainLooper();
        this.mTimerModel.updateTimer(timer.setLabel(str));
    }

    public void setTimerLength(Timer timer, long j) {
        Utils.enforceMainLooper();
        this.mTimerModel.updateTimer(timer.setLength(j));
    }

    public void setRemainingTime(Timer timer, long j) {
        Utils.enforceMainLooper();
        Timer remainingTime = timer.setRemainingTime(j);
        this.mTimerModel.updateTimer(remainingTime);
        if (timer.isRunning() && timer.getRemainingTime() <= 0) {
            this.mContext.startService(TimerService.createTimerExpiredIntent(this.mContext, remainingTime));
        }
    }

    public void updateTimerNotification() {
        Utils.enforceMainLooper();
        this.mTimerModel.updateNotification();
    }

    public Uri getDefaultTimerRingtoneUri() {
        Utils.enforceMainLooper();
        return this.mTimerModel.getDefaultTimerRingtoneUri();
    }

    public boolean isTimerRingtoneSilent() {
        Utils.enforceMainLooper();
        return this.mTimerModel.isTimerRingtoneSilent();
    }

    public Uri getTimerRingtoneUri() {
        Utils.enforceMainLooper();
        return this.mTimerModel.getTimerRingtoneUri();
    }

    public void setTimerRingtoneUri(Uri uri) {
        Utils.enforceMainLooper();
        this.mTimerModel.setTimerRingtoneUri(uri);
    }

    public String getTimerRingtoneTitle() {
        Utils.enforceMainLooper();
        return this.mTimerModel.getTimerRingtoneTitle();
    }

    public long getTimerCrescendoDuration() {
        Utils.enforceMainLooper();
        return this.mTimerModel.getTimerCrescendoDuration();
    }

    public boolean getTimerVibrate() {
        Utils.enforceMainLooper();
        return this.mTimerModel.getTimerVibrate();
    }

    public void setTimerVibrate(boolean z) {
        Utils.enforceMainLooper();
        this.mTimerModel.setTimerVibrate(z);
    }

    public Uri getDefaultAlarmRingtoneUri() {
        Utils.enforceMainLooper();
        return this.mAlarmModel.getDefaultAlarmRingtoneUri();
    }

    public void setDefaultAlarmRingtoneUri(Uri uri) {
        Utils.enforceMainLooper();
        this.mAlarmModel.setDefaultAlarmRingtoneUri(uri);
    }

    public long getAlarmCrescendoDuration() {
        Utils.enforceMainLooper();
        return this.mAlarmModel.getAlarmCrescendoDuration();
    }

    public AlarmVolumeButtonBehavior getAlarmVolumeButtonBehavior() {
        Utils.enforceMainLooper();
        return this.mAlarmModel.getAlarmVolumeButtonBehavior();
    }

    public int getAlarmTimeout() {
        return this.mAlarmModel.getAlarmTimeout();
    }

    public int getSnoozeLength() {
        return this.mAlarmModel.getSnoozeLength();
    }

    public void addStopwatchListener(StopwatchListener stopwatchListener) {
        Utils.enforceMainLooper();
        this.mStopwatchModel.addStopwatchListener(stopwatchListener);
    }

    public void removeStopwatchListener(StopwatchListener stopwatchListener) {
        Utils.enforceMainLooper();
        this.mStopwatchModel.removeStopwatchListener(stopwatchListener);
    }

    public Stopwatch getStopwatch() {
        Utils.enforceMainLooper();
        return this.mStopwatchModel.getStopwatch();
    }

    public Stopwatch startStopwatch() {
        Utils.enforceMainLooper();
        return this.mStopwatchModel.setStopwatch(getStopwatch().start());
    }

    public Stopwatch pauseStopwatch() {
        Utils.enforceMainLooper();
        return this.mStopwatchModel.setStopwatch(getStopwatch().pause());
    }

    public Stopwatch resetStopwatch() {
        Utils.enforceMainLooper();
        return this.mStopwatchModel.setStopwatch(getStopwatch().reset());
    }

    public List<Lap> getLaps() {
        Utils.enforceMainLooper();
        return this.mStopwatchModel.getLaps();
    }

    public Lap addLap() {
        Utils.enforceMainLooper();
        return this.mStopwatchModel.addLap();
    }

    public boolean canAddMoreLaps() {
        Utils.enforceMainLooper();
        return this.mStopwatchModel.canAddMoreLaps();
    }

    public long getLongestLapTime() {
        Utils.enforceMainLooper();
        return this.mStopwatchModel.getLongestLapTime();
    }

    public long getCurrentLapTime(long j) {
        Utils.enforceMainLooper();
        return this.mStopwatchModel.getCurrentLapTime(j);
    }

    public long currentTimeMillis() {
        return this.mTimeModel.currentTimeMillis();
    }

    public long elapsedRealtime() {
        return this.mTimeModel.elapsedRealtime();
    }

    public boolean is24HourFormat() {
        return this.mTimeModel.is24HourFormat();
    }

    public Calendar getCalendar() {
        return this.mTimeModel.getCalendar();
    }

    public void loadRingtoneTitles() {
        Utils.enforceNotMainLooper();
        this.mRingtoneModel.loadRingtoneTitles();
    }

    public void loadRingtonePermissions() {
        Utils.enforceNotMainLooper();
        this.mRingtoneModel.loadRingtonePermissions();
    }

    public String getRingtoneTitle(Uri uri) {
        Utils.enforceMainLooper();
        return this.mRingtoneModel.getRingtoneTitle(uri);
    }

    public CustomRingtone addCustomRingtone(Uri uri, String str) {
        Utils.enforceMainLooper();
        return this.mRingtoneModel.addCustomRingtone(uri, str);
    }

    public void removeCustomRingtone(Uri uri) {
        Utils.enforceMainLooper();
        this.mRingtoneModel.removeCustomRingtone(uri);
    }

    public List<CustomRingtone> getCustomRingtones() {
        Utils.enforceMainLooper();
        return this.mRingtoneModel.getCustomRingtones();
    }

    public void updateWidgetCount(Class cls, int i, @StringRes int i2) {
        Utils.enforceMainLooper();
        this.mWidgetModel.updateWidgetCount(cls, i, i2);
    }

    public void addSilentSettingsListener(OnSilentSettingsListener onSilentSettingsListener) {
        Utils.enforceMainLooper();
        this.mSilentSettingsModel.addSilentSettingsListener(onSilentSettingsListener);
    }

    public void removeSilentSettingsListener(OnSilentSettingsListener onSilentSettingsListener) {
        Utils.enforceMainLooper();
        this.mSilentSettingsModel.removeSilentSettingsListener(onSilentSettingsListener);
    }

    public int getGlobalIntentId() {
        return this.mSettingsModel.getGlobalIntentId();
    }

    public void updateGlobalIntentId() {
        Utils.enforceMainLooper();
        this.mSettingsModel.updateGlobalIntentId();
    }

    public ClockStyle getClockStyle() {
        Utils.enforceMainLooper();
        return this.mSettingsModel.getClockStyle();
    }

    public boolean getDisplayClockSeconds() {
        Utils.enforceMainLooper();
        return this.mSettingsModel.getDisplayClockSeconds();
    }

    public void setDisplayClockSeconds(boolean z) {
        Utils.enforceMainLooper();
        this.mSettingsModel.setDisplayClockSeconds(z);
    }

    public ClockStyle getScreensaverClockStyle() {
        Utils.enforceMainLooper();
        return this.mSettingsModel.getScreensaverClockStyle();
    }

    public boolean getScreensaverNightModeOn() {
        Utils.enforceMainLooper();
        return this.mSettingsModel.getScreensaverNightModeOn();
    }

    public boolean getShowHomeClock() {
        Utils.enforceMainLooper();
        return this.mSettingsModel.getShowHomeClock();
    }

    public Weekdays.Order getWeekdayOrder() {
        Utils.enforceMainLooper();
        return this.mSettingsModel.getWeekdayOrder();
    }

    public boolean isRestoreBackupFinished() {
        return this.mSettingsModel.isRestoreBackupFinished();
    }

    public void setRestoreBackupFinished(boolean z) {
        this.mSettingsModel.setRestoreBackupFinished(z);
    }

    public TimeZones getTimeZones() {
        Utils.enforceMainLooper();
        return this.mSettingsModel.getTimeZones();
    }

    private static class ExecutedRunnable implements Runnable {
        private final Runnable mDelegate;
        private boolean mExecuted;

        private ExecutedRunnable(Runnable runnable) {
            this.mDelegate = runnable;
        }

        @Override
        public void run() {
            this.mDelegate.run();
            synchronized (this) {
                this.mExecuted = true;
                notifyAll();
            }
        }

        private boolean isExecuted() {
            return this.mExecuted;
        }
    }
}
