package com.android.deskclock;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextClock;
import com.android.deskclock.Utils;

public class ScreensaverActivity extends Activity {
    private View mAnalogClock;
    private String mClockStyle;
    private View mContentView;
    private String mDateFormat;
    private String mDateFormatForAccessibility;
    private View mDigitalClock;
    private View mSaverView;
    private final Handler mHandler = new Handler();
    private boolean mPluggedIn = true;
    private final int mFlags = 4718721;
    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean changed = intent.getAction().equals("android.intent.action.TIME_SET") || intent.getAction().equals("android.intent.action.TIMEZONE_CHANGED");
            if (intent.getAction().equals("android.intent.action.ACTION_POWER_CONNECTED")) {
                ScreensaverActivity.this.mPluggedIn = true;
                ScreensaverActivity.this.setWakeLock();
            } else if (intent.getAction().equals("android.intent.action.ACTION_POWER_DISCONNECTED")) {
                ScreensaverActivity.this.mPluggedIn = false;
                ScreensaverActivity.this.setWakeLock();
            } else if (intent.getAction().equals("android.intent.action.USER_PRESENT")) {
                ScreensaverActivity.this.finish();
            }
            if (changed) {
                Utils.updateDate(ScreensaverActivity.this.mDateFormat, ScreensaverActivity.this.mDateFormatForAccessibility, ScreensaverActivity.this.mContentView);
                Utils.refreshAlarm(ScreensaverActivity.this, ScreensaverActivity.this.mContentView);
                Utils.setMidnightUpdater(ScreensaverActivity.this.mHandler, ScreensaverActivity.this.mMidnightUpdater);
            }
            if (intent.getAction().equals("android.app.action.NEXT_ALARM_CLOCK_CHANGED")) {
                Utils.refreshAlarm(ScreensaverActivity.this, ScreensaverActivity.this.mContentView);
            }
        }
    };
    private final Runnable mMidnightUpdater = new Runnable() {
        @Override
        public void run() {
            Utils.updateDate(ScreensaverActivity.this.mDateFormat, ScreensaverActivity.this.mDateFormatForAccessibility, ScreensaverActivity.this.mContentView);
            Utils.setMidnightUpdater(ScreensaverActivity.this.mHandler, ScreensaverActivity.this.mMidnightUpdater);
        }
    };
    private final Utils.ScreensaverMoveSaverRunnable mMoveSaverRunnable = new Utils.ScreensaverMoveSaverRunnable(this.mHandler);

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.ACTION_POWER_CONNECTED");
        filter.addAction("android.intent.action.ACTION_POWER_DISCONNECTED");
        filter.addAction("android.intent.action.USER_PRESENT");
        filter.addAction("android.intent.action.TIME_SET");
        filter.addAction("android.intent.action.TIMEZONE_CHANGED");
        registerReceiver(this.mIntentReceiver, filter);
    }

    @Override
    public void onResume() {
        boolean z = true;
        super.onResume();
        Intent chargingIntent = registerReceiver(null, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
        int plugged = chargingIntent.getIntExtra("plugged", -1);
        if (plugged != 1 && plugged != 2 && plugged != 4) {
            z = false;
        }
        this.mPluggedIn = z;
        this.mDateFormat = getString(R.string.abbrev_wday_month_day_no_year);
        this.mDateFormatForAccessibility = getString(R.string.full_wday_month_day_no_year);
        setWakeLock();
        layoutClockSaver();
        this.mHandler.post(this.mMoveSaverRunnable);
        Utils.setMidnightUpdater(this.mHandler, this.mMidnightUpdater);
    }

    @Override
    public void onPause() {
        this.mHandler.removeCallbacks(this.mMoveSaverRunnable);
        Utils.cancelMidnightUpdater(this.mHandler, this.mMidnightUpdater);
        finish();
        super.onPause();
    }

    @Override
    public void onStop() {
        unregisterReceiver(this.mIntentReceiver);
        super.onStop();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.mHandler.removeCallbacks(this.mMoveSaverRunnable);
        layoutClockSaver();
        this.mHandler.postDelayed(this.mMoveSaverRunnable, 250L);
    }

    @Override
    public void onUserInteraction() {
        finish();
    }

    private void setWakeLock() {
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        winParams.flags |= 1024;
        if (this.mPluggedIn) {
            winParams.flags |= 4718721;
        } else {
            winParams.flags &= -4718722;
        }
        win.setAttributes(winParams);
    }

    private void setClockStyle() {
        Utils.setClockStyle(this, this.mDigitalClock, this.mAnalogClock, "clock_style");
        this.mSaverView = findViewById(R.id.main_clock);
        this.mClockStyle = this.mSaverView == this.mDigitalClock ? "digital" : "analog";
        Utils.dimClockView(true, this.mSaverView);
    }

    private void layoutClockSaver() {
        setContentView(R.layout.desk_clock_saver);
        this.mDigitalClock = findViewById(R.id.digital_clock);
        this.mAnalogClock = findViewById(R.id.analog_clock);
        setClockStyle();
        Utils.setTimeFormat((TextClock) this.mDigitalClock, (int) getResources().getDimension(R.dimen.main_ampm_font_size));
        this.mContentView = (View) this.mSaverView.getParent();
        this.mContentView.forceLayout();
        this.mSaverView.forceLayout();
        this.mSaverView.setAlpha(0.0f);
        this.mMoveSaverRunnable.registerViews(this.mContentView, this.mSaverView);
        this.mContentView.setSystemUiVisibility(1029);
        Utils.updateDate(this.mDateFormat, this.mDateFormatForAccessibility, this.mContentView);
        Utils.refreshAlarm(this, this.mContentView);
    }
}
