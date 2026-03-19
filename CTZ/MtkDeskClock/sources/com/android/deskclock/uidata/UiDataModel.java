package com.android.deskclock.uidata;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import com.android.deskclock.AlarmClockFragment;
import com.android.deskclock.ClockFragment;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.stopwatch.StopwatchFragment;
import com.android.deskclock.timer.TimerFragment;

public final class UiDataModel {
    private static final UiDataModel sUiDataModel = new UiDataModel();
    private Context mContext;
    private FormattedStringModel mFormattedStringModel;
    private PeriodicCallbackModel mPeriodicCallbackModel;
    private TabModel mTabModel;

    public enum Tab {
        ALARMS(AlarmClockFragment.class, R.drawable.ic_tab_alarm, R.string.menu_alarm),
        CLOCKS(ClockFragment.class, R.drawable.ic_tab_clock, R.string.menu_clock),
        TIMERS(TimerFragment.class, R.drawable.ic_tab_timer, R.string.menu_timer),
        STOPWATCH(StopwatchFragment.class, R.drawable.ic_tab_stopwatch, R.string.menu_stopwatch);

        private final String mFragmentClassName;

        @DrawableRes
        private final int mIconResId;

        @StringRes
        private final int mLabelResId;

        Tab(Class cls, @DrawableRes int i, @StringRes int i2) {
            this.mFragmentClassName = cls.getName();
            this.mIconResId = i;
            this.mLabelResId = i2;
        }

        public String getFragmentClassName() {
            return this.mFragmentClassName;
        }

        @DrawableRes
        public int getIconResId() {
            return this.mIconResId;
        }

        @StringRes
        public int getLabelResId() {
            return this.mLabelResId;
        }
    }

    public static UiDataModel getUiDataModel() {
        return sUiDataModel;
    }

    private UiDataModel() {
    }

    public void init(Context context, SharedPreferences sharedPreferences) {
        if (this.mContext != context) {
            this.mContext = context.getApplicationContext();
            this.mPeriodicCallbackModel = new PeriodicCallbackModel(this.mContext);
            this.mFormattedStringModel = new FormattedStringModel(this.mContext);
            this.mTabModel = new TabModel(sharedPreferences);
        }
    }

    public Typeface getAlarmIconTypeface() {
        return Typeface.createFromAsset(this.mContext.getAssets(), "fonts/clock.ttf");
    }

    public String getFormattedNumber(int i) {
        Utils.enforceMainLooper();
        return this.mFormattedStringModel.getFormattedNumber(i);
    }

    public String getFormattedNumber(int i, int i2) {
        Utils.enforceMainLooper();
        return this.mFormattedStringModel.getFormattedNumber(i, i2);
    }

    public String getFormattedNumber(boolean z, int i, int i2) {
        Utils.enforceMainLooper();
        return this.mFormattedStringModel.getFormattedNumber(z, i, i2);
    }

    public String getShortWeekday(int i) {
        Utils.enforceMainLooper();
        return this.mFormattedStringModel.getShortWeekday(i);
    }

    public String getLongWeekday(int i) {
        Utils.enforceMainLooper();
        return this.mFormattedStringModel.getLongWeekday(i);
    }

    public long getShortAnimationDuration() {
        Utils.enforceMainLooper();
        return this.mContext.getResources().getInteger(android.R.integer.config_shortAnimTime);
    }

    public long getLongAnimationDuration() {
        Utils.enforceMainLooper();
        return this.mContext.getResources().getInteger(android.R.integer.config_longAnimTime);
    }

    public void addTabListener(TabListener tabListener) {
        Utils.enforceMainLooper();
        this.mTabModel.addTabListener(tabListener);
    }

    public void removeTabListener(TabListener tabListener) {
        Utils.enforceMainLooper();
        this.mTabModel.removeTabListener(tabListener);
    }

    public int getTabCount() {
        Utils.enforceMainLooper();
        return this.mTabModel.getTabCount();
    }

    public Tab getTab(int i) {
        Utils.enforceMainLooper();
        return this.mTabModel.getTab(i);
    }

    public Tab getTabAt(int i) {
        Utils.enforceMainLooper();
        return this.mTabModel.getTabAt(i);
    }

    public Tab getSelectedTab() {
        Utils.enforceMainLooper();
        return this.mTabModel.getSelectedTab();
    }

    public void setSelectedTab(Tab tab) {
        Utils.enforceMainLooper();
        this.mTabModel.setSelectedTab(tab);
    }

    public void addTabScrollListener(TabScrollListener tabScrollListener) {
        Utils.enforceMainLooper();
        this.mTabModel.addTabScrollListener(tabScrollListener);
    }

    public void removeTabScrollListener(TabScrollListener tabScrollListener) {
        Utils.enforceMainLooper();
        this.mTabModel.removeTabScrollListener(tabScrollListener);
    }

    public void setTabScrolledToTop(Tab tab, boolean z) {
        Utils.enforceMainLooper();
        this.mTabModel.setTabScrolledToTop(tab, z);
    }

    public boolean isSelectedTabScrolledToTop() {
        Utils.enforceMainLooper();
        return this.mTabModel.isTabScrolledToTop(getSelectedTab());
    }

    public String getShortcutId(@StringRes int i, @StringRes int i2) {
        if (i == R.string.category_stopwatch) {
            return this.mContext.getString(i);
        }
        return this.mContext.getString(i) + "_" + this.mContext.getString(i2);
    }

    public void addMinuteCallback(Runnable runnable, long j) {
        Utils.enforceMainLooper();
        this.mPeriodicCallbackModel.addMinuteCallback(runnable, j);
    }

    public void addQuarterHourCallback(Runnable runnable, long j) {
        Utils.enforceMainLooper();
        this.mPeriodicCallbackModel.addQuarterHourCallback(runnable, j);
    }

    public void addHourCallback(Runnable runnable, long j) {
        Utils.enforceMainLooper();
        this.mPeriodicCallbackModel.addHourCallback(runnable, j);
    }

    public void addMidnightCallback(Runnable runnable, long j) {
        Utils.enforceMainLooper();
        this.mPeriodicCallbackModel.addMidnightCallback(runnable, j);
    }

    public void removePeriodicCallback(Runnable runnable) {
        Utils.enforceMainLooper();
        this.mPeriodicCallbackModel.removePeriodicCallback(runnable);
    }
}
