package com.android.deskclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.service.dreams.DreamService;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextClock;
import com.android.deskclock.LogUtils;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.uidata.UiDataModel;

public final class Screensaver extends DreamService {
    private static final LogUtils.Logger LOGGER = new LogUtils.Logger("Screensaver");
    private final BroadcastReceiver mAlarmChangedReceiver;
    private AnalogClock mAnalogClock;
    private View mContentView;
    private String mDateFormat;
    private String mDateFormatForAccessibility;
    private TextClock mDigitalClock;
    private View mMainClockView;
    private final Runnable mMidnightUpdater;
    private MoveScreensaverRunnable mPositionUpdater;
    private final ContentObserver mSettingsContentObserver;
    private final ViewTreeObserver.OnPreDrawListener mStartPositionUpdater = new StartPositionUpdater();

    public Screensaver() {
        this.mSettingsContentObserver = Utils.isLOrLater() ? null : new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean z) {
                Utils.refreshAlarm(Screensaver.this, Screensaver.this.mContentView);
            }
        };
        this.mMidnightUpdater = new Runnable() {
            @Override
            public void run() {
                Utils.updateDate(Screensaver.this.mDateFormat, Screensaver.this.mDateFormatForAccessibility, Screensaver.this.mContentView);
            }
        };
        this.mAlarmChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Utils.refreshAlarm(Screensaver.this, Screensaver.this.mContentView);
            }
        };
    }

    @Override
    public void onCreate() {
        LOGGER.v("Screensaver created", new Object[0]);
        setTheme(2131886451);
        super.onCreate();
        this.mDateFormat = getString(R.string.abbrev_wday_month_day_no_year);
        this.mDateFormatForAccessibility = getString(R.string.full_wday_month_day_no_year);
    }

    @Override
    public void onAttachedToWindow() {
        LOGGER.v("Screensaver attached to window", new Object[0]);
        super.onAttachedToWindow();
        setContentView(R.layout.desk_clock_saver);
        this.mContentView = findViewById(R.id.saver_container);
        this.mMainClockView = this.mContentView.findViewById(R.id.main_clock);
        this.mDigitalClock = (TextClock) this.mMainClockView.findViewById(R.id.digital_clock);
        this.mAnalogClock = (AnalogClock) this.mMainClockView.findViewById(R.id.analog_clock);
        setClockStyle();
        Utils.setClockIconTypeface(this.mContentView);
        Utils.setTimeFormat(this.mDigitalClock, false);
        this.mAnalogClock.enableSeconds(false);
        this.mContentView.setSystemUiVisibility(3079);
        this.mPositionUpdater = new MoveScreensaverRunnable(this.mContentView, this.mMainClockView);
        setInteractive(false);
        setFullscreen(true);
        if (Utils.isLOrLater()) {
            registerReceiver(this.mAlarmChangedReceiver, new IntentFilter("android.app.action.NEXT_ALARM_CLOCK_CHANGED"));
        }
        if (this.mSettingsContentObserver != null) {
            getContentResolver().registerContentObserver(Settings.System.getUriFor("next_alarm_formatted"), false, this.mSettingsContentObserver);
        }
        Utils.updateDate(this.mDateFormat, this.mDateFormatForAccessibility, this.mContentView);
        Utils.refreshAlarm(this, this.mContentView);
        startPositionUpdater();
        UiDataModel.getUiDataModel().addMidnightCallback(this.mMidnightUpdater, 100L);
    }

    @Override
    public void onDetachedFromWindow() {
        LOGGER.v("Screensaver detached from window", new Object[0]);
        super.onDetachedFromWindow();
        if (this.mSettingsContentObserver != null) {
            getContentResolver().unregisterContentObserver(this.mSettingsContentObserver);
        }
        UiDataModel.getUiDataModel().removePeriodicCallback(this.mMidnightUpdater);
        stopPositionUpdater();
        if (Utils.isLOrLater()) {
            unregisterReceiver(this.mAlarmChangedReceiver);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        LOGGER.v("Screensaver configuration changed", new Object[0]);
        super.onConfigurationChanged(configuration);
        startPositionUpdater();
    }

    private void setClockStyle() {
        Utils.setScreensaverClockStyle(this.mDigitalClock, this.mAnalogClock);
        boolean screensaverNightModeOn = DataModel.getDataModel().getScreensaverNightModeOn();
        Utils.dimClockView(screensaverNightModeOn, this.mMainClockView);
        setScreenBright(!screensaverNightModeOn);
    }

    private void startPositionUpdater() {
        if (this.mContentView != null) {
            this.mContentView.getViewTreeObserver().addOnPreDrawListener(this.mStartPositionUpdater);
        }
    }

    private void stopPositionUpdater() {
        if (this.mContentView != null) {
            this.mContentView.getViewTreeObserver().removeOnPreDrawListener(this.mStartPositionUpdater);
        }
        this.mPositionUpdater.stop();
    }

    private final class StartPositionUpdater implements ViewTreeObserver.OnPreDrawListener {
        private StartPositionUpdater() {
        }

        @Override
        public boolean onPreDraw() {
            if (Screensaver.this.mContentView.getViewTreeObserver().isAlive()) {
                Screensaver.this.mPositionUpdater.start();
                Screensaver.this.mContentView.getViewTreeObserver().removeOnPreDrawListener(Screensaver.this.mStartPositionUpdater);
                return true;
            }
            return true;
        }
    }
}
