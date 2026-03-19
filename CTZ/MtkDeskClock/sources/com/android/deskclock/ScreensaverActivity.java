package com.android.deskclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextClock;
import com.android.deskclock.LogUtils;
import com.android.deskclock.events.Events;
import com.android.deskclock.uidata.UiDataModel;

public class ScreensaverActivity extends BaseActivity {
    private static final LogUtils.Logger LOGGER = new LogUtils.Logger("ScreensaverActivity");
    private static final int WINDOW_FLAGS = 4718721;
    private View mContentView;
    private String mDateFormat;
    private String mDateFormatForAccessibility;
    private View mMainClockView;
    private final Runnable mMidnightUpdater;
    private MoveScreensaverRunnable mPositionUpdater;
    private final ContentObserver mSettingsContentObserver;
    private final ViewTreeObserver.OnPreDrawListener mStartPositionUpdater = new StartPositionUpdater();
    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            byte b;
            ScreensaverActivity.LOGGER.v("ScreensaverActivity onReceive, action: " + intent.getAction(), new Object[0]);
            String action = intent.getAction();
            int iHashCode = action.hashCode();
            if (iHashCode != -1886648615) {
                if (iHashCode != -408368299) {
                    if (iHashCode != 823795052) {
                        b = (iHashCode == 1019184907 && action.equals("android.intent.action.ACTION_POWER_CONNECTED")) ? (byte) 0 : (byte) -1;
                    } else if (action.equals("android.intent.action.USER_PRESENT")) {
                        b = 2;
                    }
                } else if (action.equals("android.app.action.NEXT_ALARM_CLOCK_CHANGED")) {
                    b = 3;
                }
            } else if (action.equals("android.intent.action.ACTION_POWER_DISCONNECTED")) {
                b = 1;
            }
            switch (b) {
                case 0:
                    ScreensaverActivity.this.updateWakeLock(true);
                    break;
                case 1:
                    ScreensaverActivity.this.updateWakeLock(false);
                    break;
                case 2:
                    ScreensaverActivity.this.finish();
                    break;
                case 3:
                    Utils.refreshAlarm(ScreensaverActivity.this, ScreensaverActivity.this.mContentView);
                    break;
            }
        }
    };

    public ScreensaverActivity() {
        this.mSettingsContentObserver = Utils.isPreL() ? new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean z) {
                Utils.refreshAlarm(ScreensaverActivity.this, ScreensaverActivity.this.mContentView);
            }
        } : null;
        this.mMidnightUpdater = new Runnable() {
            @Override
            public void run() {
                Utils.updateDate(ScreensaverActivity.this.mDateFormat, ScreensaverActivity.this.mDateFormatForAccessibility, ScreensaverActivity.this.mContentView);
            }
        };
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mDateFormat = getString(R.string.abbrev_wday_month_day_no_year);
        this.mDateFormatForAccessibility = getString(R.string.full_wday_month_day_no_year);
        setContentView(R.layout.desk_clock_saver);
        this.mContentView = findViewById(R.id.saver_container);
        this.mMainClockView = this.mContentView.findViewById(R.id.main_clock);
        View viewFindViewById = this.mMainClockView.findViewById(R.id.digital_clock);
        AnalogClock analogClock = (AnalogClock) this.mMainClockView.findViewById(R.id.analog_clock);
        Utils.setClockIconTypeface(this.mMainClockView);
        Utils.setTimeFormat((TextClock) viewFindViewById, false);
        Utils.setClockStyle(viewFindViewById, analogClock);
        Utils.dimClockView(true, this.mMainClockView);
        analogClock.enableSeconds(false);
        this.mContentView.setSystemUiVisibility(3079);
        this.mContentView.setOnSystemUiVisibilityChangeListener(new InteractionListener());
        this.mPositionUpdater = new MoveScreensaverRunnable(this.mContentView, this.mMainClockView);
        Intent intent = getIntent();
        if (intent != null) {
            Events.sendScreensaverEvent(R.string.action_show, intent.getIntExtra(Events.EXTRA_EVENT_LABEL, 0));
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.ACTION_POWER_CONNECTED");
        intentFilter.addAction("android.intent.action.ACTION_POWER_DISCONNECTED");
        intentFilter.addAction("android.intent.action.USER_PRESENT");
        if (Utils.isLOrLater()) {
            intentFilter.addAction("android.app.action.NEXT_ALARM_CLOCK_CHANGED");
        }
        registerReceiver(this.mIntentReceiver, intentFilter);
        if (this.mSettingsContentObserver != null) {
            getContentResolver().registerContentObserver(Settings.System.getUriFor("next_alarm_formatted"), false, this.mSettingsContentObserver);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Utils.updateDate(this.mDateFormat, this.mDateFormatForAccessibility, this.mContentView);
        Utils.refreshAlarm(this, this.mContentView);
        startPositionUpdater();
        UiDataModel.getUiDataModel().addMidnightCallback(this.mMidnightUpdater, 100L);
        Intent intentRegisterReceiver = registerReceiver(null, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
        boolean z = false;
        if (intentRegisterReceiver != null && intentRegisterReceiver.getIntExtra("plugged", 0) != 0) {
            z = true;
        }
        updateWakeLock(z);
    }

    @Override
    public void onPause() {
        super.onPause();
        UiDataModel.getUiDataModel().removePeriodicCallback(this.mMidnightUpdater);
        stopPositionUpdater();
    }

    @Override
    public void onStop() {
        if (this.mSettingsContentObserver != null) {
            getContentResolver().unregisterContentObserver(this.mSettingsContentObserver);
        }
        unregisterReceiver(this.mIntentReceiver);
        super.onStop();
    }

    @Override
    public void onUserInteraction() {
        finish();
    }

    private void updateWakeLock(boolean z) {
        Window window = getWindow();
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.flags |= 1024;
        if (z) {
            attributes.flags |= WINDOW_FLAGS;
        } else {
            attributes.flags &= -4718722;
        }
        window.setAttributes(attributes);
    }

    private void startPositionUpdater() {
        this.mContentView.getViewTreeObserver().addOnPreDrawListener(this.mStartPositionUpdater);
    }

    private void stopPositionUpdater() {
        this.mContentView.getViewTreeObserver().removeOnPreDrawListener(this.mStartPositionUpdater);
        this.mPositionUpdater.stop();
    }

    private final class StartPositionUpdater implements ViewTreeObserver.OnPreDrawListener {
        private StartPositionUpdater() {
        }

        @Override
        public boolean onPreDraw() {
            if (ScreensaverActivity.this.mContentView.getViewTreeObserver().isAlive()) {
                ScreensaverActivity.this.mPositionUpdater.start();
                ScreensaverActivity.this.mContentView.getViewTreeObserver().removeOnPreDrawListener(ScreensaverActivity.this.mStartPositionUpdater);
                return true;
            }
            return true;
        }
    }

    private final class InteractionListener implements View.OnSystemUiVisibilityChangeListener {
        private InteractionListener() {
        }

        @Override
        public void onSystemUiVisibilityChange(int i) {
            if ((i & 2) == 0) {
                ScreensaverActivity.this.finish();
            }
        }
    }
}
