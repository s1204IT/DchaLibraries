package com.android.deskclock;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextClock;
import com.android.deskclock.worldclock.CitiesActivity;
import com.android.deskclock.worldclock.WorldClockAdapter;

public class ClockFragment extends DeskClockFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    private WorldClockAdapter mAdapter;
    private View mAnalogClock;
    private View mClockFrame;
    private String mClockStyle;
    private String mDateFormat;
    private String mDateFormatForAccessibility;
    private String mDefaultClockStyle;
    private View mDigitalClock;
    private View mHairline;
    private ListView mList;
    private SharedPreferences mPrefs;
    private boolean mButtonsHidden = false;
    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            boolean changed = action.equals("android.intent.action.TIME_SET") || action.equals("android.intent.action.TIMEZONE_CHANGED") || action.equals("android.intent.action.LOCALE_CHANGED");
            if (changed) {
                Utils.updateDate(ClockFragment.this.mDateFormat, ClockFragment.this.mDateFormatForAccessibility, ClockFragment.this.mClockFrame);
                if (ClockFragment.this.mAdapter != null) {
                    if (ClockFragment.this.mAdapter.hasHomeCity() != ClockFragment.this.mAdapter.needHomeCity()) {
                        ClockFragment.this.mAdapter.reloadData(context);
                    } else {
                        ClockFragment.this.mAdapter.notifyDataSetChanged();
                    }
                    if (action.equals("android.intent.action.LOCALE_CHANGED")) {
                        if (ClockFragment.this.mDigitalClock != null) {
                            Utils.setTimeFormat((TextClock) ClockFragment.this.mDigitalClock.findViewById(R.id.digital_clock), (int) context.getResources().getDimension(R.dimen.main_ampm_font_size));
                        }
                        ClockFragment.this.mAdapter.loadCitiesDb(context);
                        ClockFragment.this.mAdapter.notifyDataSetChanged();
                    }
                }
                Utils.setQuarterHourUpdater(ClockFragment.this.mHandler, ClockFragment.this.mQuarterHourUpdater);
            }
            if (changed || action.equals("android.app.action.NEXT_ALARM_CLOCK_CHANGED")) {
                Utils.refreshAlarm(ClockFragment.this.getActivity(), ClockFragment.this.mClockFrame);
            }
        }
    };
    private final Handler mHandler = new Handler();
    private final Runnable mQuarterHourUpdater = new Runnable() {
        @Override
        public void run() {
            Utils.updateDate(ClockFragment.this.mDateFormat, ClockFragment.this.mDateFormatForAccessibility, ClockFragment.this.mClockFrame);
            if (ClockFragment.this.mAdapter != null) {
                ClockFragment.this.mAdapter.notifyDataSetChanged();
            }
            Utils.setQuarterHourUpdater(ClockFragment.this.mHandler, ClockFragment.this.mQuarterHourUpdater);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle icicle) {
        View v = inflater.inflate(R.layout.clock_fragment, container, false);
        if (icicle != null) {
            this.mButtonsHidden = icicle.getBoolean("buttons_hidden", false);
        }
        this.mList = (ListView) v.findViewById(R.id.cities);
        this.mList.setDivider(null);
        View.OnTouchListener longPressNightMode = new View.OnTouchListener() {
            private float mLastTouchX;
            private float mLastTouchY;
            private float mMaxMovementAllowed = -1.0f;
            private int mLongPressTimeout = -1;

            @Override
            public boolean onTouch(View v2, MotionEvent event) {
                if (this.mMaxMovementAllowed == -1.0f) {
                    this.mMaxMovementAllowed = ViewConfiguration.get(ClockFragment.this.getActivity()).getScaledTouchSlop();
                    this.mLongPressTimeout = ViewConfiguration.getLongPressTimeout();
                }
                switch (event.getAction()) {
                    case 0:
                        Utils.getTimeNow();
                        ClockFragment.this.mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                ClockFragment.this.startActivity(new Intent(ClockFragment.this.getActivity(), (Class<?>) ScreensaverActivity.class));
                            }
                        }, this.mLongPressTimeout);
                        this.mLastTouchX = event.getX();
                        this.mLastTouchY = event.getY();
                        return true;
                    case 1:
                    default:
                        ClockFragment.this.mHandler.removeCallbacksAndMessages(null);
                        return false;
                    case 2:
                        float xDiff = Math.abs(event.getX() - this.mLastTouchX);
                        float yDiff = Math.abs(event.getY() - this.mLastTouchY);
                        if (xDiff >= this.mMaxMovementAllowed || yDiff >= this.mMaxMovementAllowed) {
                            ClockFragment.this.mHandler.removeCallbacksAndMessages(null);
                        }
                        return false;
                }
            }
        };
        this.mClockFrame = v.findViewById(R.id.main_clock_left_pane);
        this.mHairline = v.findViewById(R.id.hairline);
        if (this.mClockFrame == null) {
            this.mClockFrame = inflater.inflate(R.layout.main_clock_frame, (ViewGroup) this.mList, false);
            this.mHairline = this.mClockFrame.findViewById(R.id.hairline);
            this.mHairline.setVisibility(0);
            this.mList.addHeaderView(this.mClockFrame, null, false);
        } else {
            this.mHairline.setVisibility(8);
            v.setOnTouchListener(longPressNightMode);
        }
        this.mList.setOnTouchListener(longPressNightMode);
        View menuButton = v.findViewById(R.id.menu_button);
        if (menuButton != null) {
            setupFakeOverflowMenuButton(menuButton);
        }
        this.mDigitalClock = this.mClockFrame.findViewById(R.id.digital_clock);
        this.mAnalogClock = this.mClockFrame.findViewById(R.id.analog_clock);
        Utils.setTimeFormat((TextClock) this.mDigitalClock.findViewById(R.id.digital_clock), (int) getResources().getDimension(R.dimen.main_ampm_font_size));
        View footerView = inflater.inflate(R.layout.blank_footer_view, (ViewGroup) this.mList, false);
        this.mList.addFooterView(footerView, null, false);
        this.mAdapter = new WorldClockAdapter(getActivity());
        if (this.mAdapter.getCount() == 0) {
            this.mHairline.setVisibility(8);
        }
        this.mList.setAdapter((ListAdapter) this.mAdapter);
        this.mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        this.mDefaultClockStyle = getActivity().getResources().getString(R.string.default_clock_style);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        DeskClock activity = (DeskClock) getActivity();
        if (activity.getSelectedTab() == 1) {
            setFabAppearance();
            setLeftRightButtonAppearance();
        }
        this.mPrefs.registerOnSharedPreferenceChangeListener(this);
        this.mDateFormat = getString(R.string.abbrev_wday_month_day_no_year);
        this.mDateFormatForAccessibility = getString(R.string.full_wday_month_day_no_year);
        Utils.setQuarterHourUpdater(this.mHandler, this.mQuarterHourUpdater);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.app.action.NEXT_ALARM_CLOCK_CHANGED");
        filter.addAction("android.intent.action.TIME_SET");
        filter.addAction("android.intent.action.TIMEZONE_CHANGED");
        filter.addAction("android.intent.action.LOCALE_CHANGED");
        activity.registerReceiver(this.mIntentReceiver, filter);
        if (this.mAdapter != null) {
            this.mAdapter.loadCitiesDb(activity);
            this.mAdapter.reloadData(activity);
        }
        View clockView = Utils.setClockStyle(activity, this.mDigitalClock, this.mAnalogClock, "clock_style");
        this.mClockStyle = clockView == this.mDigitalClock ? "digital" : "analog";
        if (getView().findViewById(R.id.main_clock_left_pane) != null && this.mAdapter.getCount() == 0) {
            this.mList.setVisibility(8);
        } else {
            this.mList.setVisibility(0);
        }
        this.mAdapter.notifyDataSetChanged();
        Utils.updateDate(this.mDateFormat, this.mDateFormatForAccessibility, this.mClockFrame);
        Utils.refreshAlarm(activity, this.mClockFrame);
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        Utils.cancelQuarterHourUpdater(this.mHandler, this.mQuarterHourUpdater);
        Activity activity = getActivity();
        activity.unregisterReceiver(this.mIntentReceiver);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("buttons_hidden", this.mButtonsHidden);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key == "clock_style") {
            this.mClockStyle = prefs.getString("clock_style", this.mDefaultClockStyle);
            this.mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onFabClick(View view) {
        Activity activity = getActivity();
        startActivity(new Intent(activity, (Class<?>) CitiesActivity.class));
    }

    @Override
    public void setFabAppearance() {
        DeskClock activity = (DeskClock) getActivity();
        if (this.mFab != null && activity.getSelectedTab() == 1) {
            this.mFab.setVisibility(0);
            this.mFab.setImageResource(R.drawable.ic_globe);
            this.mFab.setContentDescription(getString(R.string.button_cities));
        }
    }

    @Override
    public void setLeftRightButtonAppearance() {
        DeskClock activity = (DeskClock) getActivity();
        if (this.mLeftButton != null && this.mRightButton != null && activity.getSelectedTab() == 1) {
            this.mLeftButton.setVisibility(4);
            this.mRightButton.setVisibility(4);
        }
    }
}
