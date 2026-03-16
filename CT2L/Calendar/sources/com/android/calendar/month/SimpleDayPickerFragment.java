package com.android.calendar.month;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;
import com.android.calendar.R;
import com.android.calendar.Utils;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

public class SimpleDayPickerFragment extends ListFragment implements AbsListView.OnScrollListener {
    public static int LIST_TOP_OFFSET = -1;
    private static float mScale = 0.0f;
    protected SimpleWeeksAdapter mAdapter;
    protected Context mContext;
    protected int mCurrentMonthDisplayed;
    protected String[] mDayLabels;
    protected ViewGroup mDayNamesHeader;
    protected int mFirstDayOfWeek;
    protected Handler mHandler;
    protected ListView mListView;
    protected float mMinimumFlingVelocity;
    protected TextView mMonthName;
    protected long mPreviousScrollPosition;
    protected int WEEK_MIN_VISIBLE_HEIGHT = 12;
    protected int BOTTOM_BUFFER = 20;
    protected int mSaturdayColor = 0;
    protected int mSundayColor = 0;
    protected int mDayNameColor = 0;
    protected int mNumWeeks = 6;
    protected boolean mShowWeekNumber = false;
    protected int mDaysPerWeek = 7;
    protected float mFriction = 1.0f;
    protected Time mSelectedDay = new Time();
    protected Time mTempTime = new Time();
    protected Time mFirstDayOfMonth = new Time();
    protected Time mFirstVisibleDay = new Time();
    protected boolean mIsScrollingUp = false;
    protected int mPreviousScrollState = 0;
    protected int mCurrentScrollState = 0;
    protected Runnable mTodayUpdater = new Runnable() {
        @Override
        public void run() {
            Time midnight = new Time(SimpleDayPickerFragment.this.mFirstVisibleDay.timezone);
            midnight.setToNow();
            long currentMillis = midnight.toMillis(true);
            midnight.hour = 0;
            midnight.minute = 0;
            midnight.second = 0;
            midnight.monthDay++;
            long millisToMidnight = midnight.normalize(true) - currentMillis;
            SimpleDayPickerFragment.this.mHandler.postDelayed(this, millisToMidnight);
            if (SimpleDayPickerFragment.this.mAdapter != null) {
                SimpleDayPickerFragment.this.mAdapter.notifyDataSetChanged();
            }
        }
    };
    protected DataSetObserver mObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            Time day = SimpleDayPickerFragment.this.mAdapter.getSelectedDay();
            if (day.year != SimpleDayPickerFragment.this.mSelectedDay.year || day.yearDay != SimpleDayPickerFragment.this.mSelectedDay.yearDay) {
                SimpleDayPickerFragment.this.goTo(day.toMillis(true), true, true, false);
            }
        }
    };
    protected ScrollStateRunnable mScrollStateChangedRunnable = new ScrollStateRunnable();

    public SimpleDayPickerFragment(long initialTime) {
        goTo(initialTime, false, true, true);
        this.mHandler = new Handler();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mContext = activity;
        String tz = Time.getCurrentTimezone();
        ViewConfiguration viewConfig = ViewConfiguration.get(activity);
        this.mMinimumFlingVelocity = viewConfig.getScaledMinimumFlingVelocity();
        this.mSelectedDay.switchTimezone(tz);
        this.mSelectedDay.normalize(true);
        this.mFirstDayOfMonth.timezone = tz;
        this.mFirstDayOfMonth.normalize(true);
        this.mFirstVisibleDay.timezone = tz;
        this.mFirstVisibleDay.normalize(true);
        this.mTempTime.timezone = tz;
        Resources res = activity.getResources();
        this.mSaturdayColor = res.getColor(R.color.month_saturday);
        this.mSundayColor = res.getColor(R.color.month_sunday);
        this.mDayNameColor = res.getColor(R.color.month_day_names_color);
        if (mScale == 0.0f) {
            mScale = activity.getResources().getDisplayMetrics().density;
            if (mScale != 1.0f) {
                this.WEEK_MIN_VISIBLE_HEIGHT = (int) (this.WEEK_MIN_VISIBLE_HEIGHT * mScale);
                this.BOTTOM_BUFFER = (int) (this.BOTTOM_BUFFER * mScale);
                LIST_TOP_OFFSET = (int) (LIST_TOP_OFFSET * mScale);
            }
        }
        setUpAdapter();
        setListAdapter(this.mAdapter);
    }

    protected void setUpAdapter() {
        HashMap<String, Integer> weekParams = new HashMap<>();
        weekParams.put("num_weeks", Integer.valueOf(this.mNumWeeks));
        weekParams.put("week_numbers", Integer.valueOf(this.mShowWeekNumber ? 1 : 0));
        weekParams.put("week_start", Integer.valueOf(this.mFirstDayOfWeek));
        weekParams.put("selected_day", Integer.valueOf(Time.getJulianDay(this.mSelectedDay.toMillis(false), this.mSelectedDay.gmtoff)));
        if (this.mAdapter == null) {
            this.mAdapter = new SimpleWeeksAdapter(getActivity(), weekParams);
            this.mAdapter.registerDataSetObserver(this.mObserver);
        } else {
            this.mAdapter.updateParams(weekParams);
        }
        this.mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.containsKey("current_time")) {
            goTo(savedInstanceState.getLong("current_time"), false, true, true);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setUpListView();
        setUpHeader();
        this.mMonthName = (TextView) getView().findViewById(R.id.month_name);
        SimpleWeekView child = (SimpleWeekView) this.mListView.getChildAt(0);
        if (child != null) {
            int julianDay = child.getFirstJulianDay();
            this.mFirstVisibleDay.setJulianDay(julianDay);
            this.mTempTime.setJulianDay(julianDay + 7);
            setMonthDisplayed(this.mTempTime, true);
        }
    }

    protected void setUpHeader() {
        this.mDayLabels = new String[7];
        for (int i = 1; i <= 7; i++) {
            this.mDayLabels[i - 1] = DateUtils.getDayOfWeekString(i, 50).toUpperCase();
        }
    }

    protected void setUpListView() {
        this.mListView = getListView();
        this.mListView.setCacheColorHint(0);
        this.mListView.setDivider(null);
        this.mListView.setItemsCanFocus(true);
        this.mListView.setFastScrollEnabled(false);
        this.mListView.setVerticalScrollBarEnabled(false);
        this.mListView.setOnScrollListener(this);
        this.mListView.setFadingEdgeLength(0);
        this.mListView.setFriction(ViewConfiguration.getScrollFriction() * this.mFriction);
    }

    @Override
    public void onResume() {
        super.onResume();
        setUpAdapter();
        doResumeUpdates();
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mHandler.removeCallbacks(this.mTodayUpdater);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putLong("current_time", this.mSelectedDay.toMillis(true));
    }

    protected void doResumeUpdates() {
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        this.mFirstDayOfWeek = cal.getFirstDayOfWeek() - 1;
        this.mShowWeekNumber = false;
        updateHeader();
        goTo(this.mSelectedDay.toMillis(true), false, false, false);
        this.mAdapter.setSelectedDay(this.mSelectedDay);
        this.mTodayUpdater.run();
    }

    protected void updateHeader() {
        TextView label = (TextView) this.mDayNamesHeader.findViewById(R.id.wk_label);
        if (this.mShowWeekNumber) {
            label.setVisibility(0);
        } else {
            label.setVisibility(8);
        }
        int offset = this.mFirstDayOfWeek - 1;
        for (int i = 1; i < 8; i++) {
            TextView label2 = (TextView) this.mDayNamesHeader.getChildAt(i);
            if (i < this.mDaysPerWeek + 1) {
                int position = (offset + i) % 7;
                label2.setText(this.mDayLabels[position]);
                label2.setVisibility(0);
                if (position == 6) {
                    label2.setTextColor(this.mSaturdayColor);
                } else if (position == 0) {
                    label2.setTextColor(this.mSundayColor);
                } else {
                    label2.setTextColor(this.mDayNameColor);
                }
            } else {
                label2.setVisibility(8);
            }
        }
        this.mDayNamesHeader.invalidate();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.month_by_week, container, false);
        this.mDayNamesHeader = (ViewGroup) v.findViewById(R.id.day_names);
        return v;
    }

    public boolean goTo(long time, boolean animate, boolean setSelected, boolean forceScroll) {
        View child;
        int firstPosition;
        if (time == -1) {
            Log.e("MonthFragment", "time is invalid");
            return false;
        }
        if (setSelected) {
            this.mSelectedDay.set(time);
            this.mSelectedDay.normalize(true);
        }
        if (!isResumed()) {
            if (Log.isLoggable("MonthFragment", 3)) {
                Log.d("MonthFragment", "We're not visible yet");
            }
            return false;
        }
        this.mTempTime.set(time);
        long millis = this.mTempTime.normalize(true);
        int position = Utils.getWeeksSinceEpochFromJulianDay(Time.getJulianDay(millis, this.mTempTime.gmtoff), this.mFirstDayOfWeek);
        int i = 0;
        int top = 0;
        while (true) {
            int i2 = i + 1;
            child = this.mListView.getChildAt(i);
            if (child != null) {
                top = child.getTop();
                if (Log.isLoggable("MonthFragment", 3)) {
                    Log.d("MonthFragment", "child at " + (i2 - 1) + " has top " + top);
                }
                if (top >= 0) {
                    break;
                }
                i = i2;
            } else {
                break;
            }
        }
        if (child != null) {
            firstPosition = this.mListView.getPositionForView(child);
        } else {
            firstPosition = 0;
        }
        int lastPosition = (this.mNumWeeks + firstPosition) - 1;
        if (top > this.BOTTOM_BUFFER) {
            lastPosition--;
        }
        if (setSelected) {
            this.mAdapter.setSelectedDay(this.mSelectedDay);
        }
        if (Log.isLoggable("MonthFragment", 3)) {
            Log.d("MonthFragment", "GoTo position " + position);
        }
        if (position < firstPosition || position > lastPosition || forceScroll) {
            this.mFirstDayOfMonth.set(this.mTempTime);
            this.mFirstDayOfMonth.monthDay = 1;
            long millis2 = this.mFirstDayOfMonth.normalize(true);
            setMonthDisplayed(this.mFirstDayOfMonth, true);
            int position2 = Utils.getWeeksSinceEpochFromJulianDay(Time.getJulianDay(millis2, this.mFirstDayOfMonth.gmtoff), this.mFirstDayOfWeek);
            this.mPreviousScrollState = 2;
            if (animate) {
                this.mListView.smoothScrollToPositionFromTop(position2, LIST_TOP_OFFSET, 500);
                return true;
            }
            this.mListView.setSelectionFromTop(position2, LIST_TOP_OFFSET);
            onScrollStateChanged(this.mListView, 0);
        } else if (setSelected) {
            setMonthDisplayed(this.mSelectedDay, true);
        }
        return false;
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        SimpleWeekView child = (SimpleWeekView) view.getChildAt(0);
        if (child != null) {
            long currScroll = (view.getFirstVisiblePosition() * child.getHeight()) - child.getBottom();
            this.mFirstVisibleDay.setJulianDay(child.getFirstJulianDay());
            if (currScroll < this.mPreviousScrollPosition) {
                this.mIsScrollingUp = true;
            } else if (currScroll > this.mPreviousScrollPosition) {
                this.mIsScrollingUp = false;
            } else {
                return;
            }
            this.mPreviousScrollPosition = currScroll;
            this.mPreviousScrollState = this.mCurrentScrollState;
            updateMonthHighlight(this.mListView);
        }
    }

    private void updateMonthHighlight(AbsListView view) {
        int month;
        int monthDiff;
        SimpleWeekView child = (SimpleWeekView) view.getChildAt(0);
        if (child != null) {
            int offset = child.getBottom() < this.WEEK_MIN_VISIBLE_HEIGHT ? 1 : 0;
            SimpleWeekView child2 = (SimpleWeekView) view.getChildAt(offset + 2);
            if (child2 != null) {
                if (this.mIsScrollingUp) {
                    month = child2.getFirstMonth();
                } else {
                    month = child2.getLastMonth();
                }
                if (this.mCurrentMonthDisplayed == 11 && month == 0) {
                    monthDiff = 1;
                } else if (this.mCurrentMonthDisplayed == 0 && month == 11) {
                    monthDiff = -1;
                } else {
                    monthDiff = month - this.mCurrentMonthDisplayed;
                }
                if (monthDiff != 0) {
                    int julianDay = child2.getFirstJulianDay();
                    if (!this.mIsScrollingUp) {
                        julianDay += 7;
                    }
                    this.mTempTime.setJulianDay(julianDay);
                    setMonthDisplayed(this.mTempTime, false);
                }
            }
        }
    }

    protected void setMonthDisplayed(Time time, boolean updateHighlight) {
        CharSequence oldMonth = this.mMonthName.getText();
        this.mMonthName.setText(Utils.formatMonthYear(this.mContext, time));
        this.mMonthName.invalidate();
        if (!TextUtils.equals(oldMonth, this.mMonthName.getText())) {
            this.mMonthName.sendAccessibilityEvent(8);
        }
        this.mCurrentMonthDisplayed = time.month;
        if (updateHighlight) {
            this.mAdapter.updateFocusMonth(this.mCurrentMonthDisplayed);
        }
    }

    public void onScrollStateChanged(AbsListView view, int scrollState) {
        this.mScrollStateChangedRunnable.doScrollStateChange(view, scrollState);
    }

    protected class ScrollStateRunnable implements Runnable {
        private int mNewState;

        protected ScrollStateRunnable() {
        }

        public void doScrollStateChange(AbsListView view, int scrollState) {
            SimpleDayPickerFragment.this.mHandler.removeCallbacks(this);
            this.mNewState = scrollState;
            SimpleDayPickerFragment.this.mHandler.postDelayed(this, 40L);
        }

        @Override
        public void run() {
            SimpleDayPickerFragment.this.mCurrentScrollState = this.mNewState;
            if (Log.isLoggable("MonthFragment", 3)) {
                Log.d("MonthFragment", "new scroll state: " + this.mNewState + " old state: " + SimpleDayPickerFragment.this.mPreviousScrollState);
            }
            if (this.mNewState == 0 && SimpleDayPickerFragment.this.mPreviousScrollState != 0) {
                SimpleDayPickerFragment.this.mPreviousScrollState = this.mNewState;
                SimpleDayPickerFragment.this.mAdapter.updateFocusMonth(SimpleDayPickerFragment.this.mCurrentMonthDisplayed);
            } else {
                SimpleDayPickerFragment.this.mPreviousScrollState = this.mNewState;
            }
        }
    }
}
