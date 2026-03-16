package com.android.deskclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.service.dreams.DreamService;
import android.view.View;
import android.widget.TextClock;
import com.android.deskclock.Utils;

public class Screensaver extends DreamService {
    private View mAnalogClock;
    private View mContentView;
    private String mDateFormat;
    private String mDateFormatForAccessibility;
    private View mDigitalClock;
    private View mSaverView;
    private final Handler mHandler = new Handler();
    private final Runnable mMidnightUpdater = new Runnable() {
        @Override
        public void run() {
            Utils.updateDate(Screensaver.this.mDateFormat, Screensaver.this.mDateFormatForAccessibility, Screensaver.this.mContentView);
            Utils.setMidnightUpdater(Screensaver.this.mHandler, Screensaver.this.mMidnightUpdater);
        }
    };
    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals("android.intent.action.TIME_SET") || action.equals("android.intent.action.TIMEZONE_CHANGED")) {
                    Utils.updateDate(Screensaver.this.mDateFormat, Screensaver.this.mDateFormatForAccessibility, Screensaver.this.mContentView);
                    Utils.refreshAlarm(Screensaver.this, Screensaver.this.mContentView);
                    Utils.setMidnightUpdater(Screensaver.this.mHandler, Screensaver.this.mMidnightUpdater);
                } else if (action.equals("android.app.action.NEXT_ALARM_CLOCK_CHANGED")) {
                    Utils.refreshAlarm(Screensaver.this, Screensaver.this.mContentView);
                }
            }
        }
    };
    private final Utils.ScreensaverMoveSaverRunnable mMoveSaverRunnable = new Utils.ScreensaverMoveSaverRunnable(this.mHandler);

    @Override
    public void onCreate() {
        super.onCreate();
        setTheme(R.style.DeskClockParentTheme);
        this.mDateFormat = getString(R.string.abbrev_wday_month_day_no_year);
        this.mDateFormatForAccessibility = getString(R.string.full_wday_month_day_no_year);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.mHandler.removeCallbacks(this.mMoveSaverRunnable);
        layoutClockSaver();
        this.mHandler.postDelayed(this.mMoveSaverRunnable, 250L);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        layoutClockSaver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.TIME_SET");
        filter.addAction("android.intent.action.TIMEZONE_CHANGED");
        registerReceiver(this.mIntentReceiver, filter);
        Utils.setMidnightUpdater(this.mHandler, this.mMidnightUpdater);
        this.mHandler.post(this.mMoveSaverRunnable);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mHandler.removeCallbacks(this.mMoveSaverRunnable);
        Utils.cancelMidnightUpdater(this.mHandler, this.mMidnightUpdater);
        unregisterReceiver(this.mIntentReceiver);
    }

    private void setClockStyle() {
        Utils.setClockStyle(this, this.mDigitalClock, this.mAnalogClock, "screensaver_clock_style");
        this.mSaverView = findViewById(R.id.main_clock);
        boolean dimNightMode = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("screensaver_night_mode", false);
        Utils.dimClockView(dimNightMode, this.mSaverView);
        setScreenBright(dimNightMode ? false : true);
    }

    private void layoutClockSaver() {
        setContentView(R.layout.desk_clock_saver);
        this.mDigitalClock = findViewById(R.id.digital_clock);
        this.mAnalogClock = findViewById(R.id.analog_clock);
        setClockStyle();
        Utils.setTimeFormat((TextClock) this.mDigitalClock, (int) getResources().getDimension(R.dimen.main_ampm_font_size));
        this.mContentView = (View) this.mSaverView.getParent();
        this.mSaverView.setAlpha(0.0f);
        this.mMoveSaverRunnable.registerViews(this.mContentView, this.mSaverView);
        Utils.updateDate(this.mDateFormat, this.mDateFormatForAccessibility, this.mContentView);
        Utils.refreshAlarm(this, this.mContentView);
    }
}
