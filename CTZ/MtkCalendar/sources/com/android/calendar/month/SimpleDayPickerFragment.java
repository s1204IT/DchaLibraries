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
    protected boolean mUserScrolled = false;
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
            Time time = new Time(SimpleDayPickerFragment.this.mFirstVisibleDay.timezone);
            time.setToNow();
            long millis = time.toMillis(true);
            time.hour = 0;
            time.minute = 0;
            time.second = 0;
            time.monthDay++;
            SimpleDayPickerFragment.this.mHandler.postDelayed(this, time.normalize(true) - millis);
            if (SimpleDayPickerFragment.this.mAdapter != null) {
                SimpleDayPickerFragment.this.mAdapter.notifyDataSetChanged();
            }
        }
    };
    protected DataSetObserver mObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            Time selectedDay = SimpleDayPickerFragment.this.mAdapter.getSelectedDay();
            if (selectedDay.year != SimpleDayPickerFragment.this.mSelectedDay.year || selectedDay.yearDay != SimpleDayPickerFragment.this.mSelectedDay.yearDay) {
                SimpleDayPickerFragment.this.goTo(selectedDay.toMillis(true), true, true, false);
            }
        }
    };
    protected ScrollStateRunnable mScrollStateChangedRunnable = new ScrollStateRunnable();

    public SimpleDayPickerFragment(long j) {
        goTo(j, false, true, true);
        this.mHandler = new Handler();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mContext = activity;
        String currentTimezone = Time.getCurrentTimezone();
        this.mMinimumFlingVelocity = ViewConfiguration.get(activity).getScaledMinimumFlingVelocity();
        this.mSelectedDay.switchTimezone(currentTimezone);
        this.mSelectedDay.normalize(true);
        this.mFirstDayOfMonth.timezone = currentTimezone;
        this.mFirstDayOfMonth.normalize(true);
        this.mFirstVisibleDay.timezone = currentTimezone;
        this.mFirstVisibleDay.normalize(true);
        this.mTempTime.timezone = currentTimezone;
        Resources resources = activity.getResources();
        this.mSaturdayColor = resources.getColor(R.color.month_saturday);
        this.mSundayColor = resources.getColor(R.color.month_sunday);
        this.mDayNameColor = resources.getColor(R.color.month_day_names_color);
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
        HashMap<String, Integer> map = new HashMap<>();
        map.put("num_weeks", Integer.valueOf(this.mNumWeeks));
        map.put("week_numbers", Integer.valueOf(this.mShowWeekNumber ? 1 : 0));
        map.put("week_start", Integer.valueOf(this.mFirstDayOfWeek));
        map.put("selected_day", Integer.valueOf(Time.getJulianDay(this.mSelectedDay.toMillis(false), this.mSelectedDay.gmtoff)));
        if (this.mAdapter == null) {
            this.mAdapter = new SimpleWeeksAdapter(getActivity(), map);
            this.mAdapter.registerDataSetObserver(this.mObserver);
        } else {
            this.mAdapter.updateParams(map);
        }
        this.mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (bundle != null && bundle.containsKey("current_time")) {
            goTo(bundle.getLong("current_time"), false, true, true);
        }
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        setUpListView();
        setUpHeader();
        this.mMonthName = (TextView) getView().findViewById(R.id.month_name);
        SimpleWeekView simpleWeekView = (SimpleWeekView) this.mListView.getChildAt(0);
        if (simpleWeekView == null) {
            return;
        }
        int firstJulianDay = simpleWeekView.getFirstJulianDay();
        this.mFirstVisibleDay.setJulianDay(firstJulianDay);
        this.mTempTime.setJulianDay(firstJulianDay + 7);
        setMonthDisplayed(this.mTempTime, true);
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
    public void onSaveInstanceState(Bundle bundle) {
        bundle.putLong("current_time", this.mSelectedDay.toMillis(true));
    }

    protected void doResumeUpdates() {
        this.mFirstDayOfWeek = Calendar.getInstance(Locale.getDefault()).getFirstDayOfWeek() - 1;
        this.mShowWeekNumber = false;
        updateHeader();
        goTo(this.mSelectedDay.toMillis(true), false, false, false);
        this.mAdapter.setSelectedDay(this.mSelectedDay);
        this.mTodayUpdater.run();
    }

    protected void updateHeader() {
        TextView textView = (TextView) this.mDayNamesHeader.findViewById(R.id.wk_label);
        if (this.mShowWeekNumber) {
            textView.setVisibility(0);
        } else {
            textView.setVisibility(8);
        }
        int i = this.mFirstDayOfWeek - 1;
        for (int i2 = 1; i2 < 8; i2++) {
            TextView textView2 = (TextView) this.mDayNamesHeader.getChildAt(i2);
            if (i2 < this.mDaysPerWeek + 1) {
                int i3 = (i + i2) % 7;
                textView2.setText(this.mDayLabels[i3]);
                textView2.setVisibility(0);
                if (i3 == 6) {
                    textView2.setTextColor(this.mSaturdayColor);
                } else if (i3 == 0) {
                    textView2.setTextColor(this.mSundayColor);
                } else {
                    textView2.setTextColor(this.mDayNameColor);
                }
            } else {
                textView2.setVisibility(8);
            }
        }
        this.mDayNamesHeader.invalidate();
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View viewInflate = layoutInflater.inflate(R.layout.month_by_week, viewGroup, false);
        this.mDayNamesHeader = (ViewGroup) viewInflate.findViewById(R.id.day_names);
        return viewInflate;
    }

    public boolean goTo(long j, boolean z, boolean z2, boolean z3) {
        View childAt;
        int positionForView;
        if (j == -1) {
            Log.e("MonthFragment", "time is invalid");
            return false;
        }
        if (z2) {
            this.mSelectedDay.set(j);
            this.mSelectedDay.normalize(true);
        }
        if (!isResumed()) {
            if (Log.isLoggable("MonthFragment", 3)) {
                Log.d("MonthFragment", "We're not visible yet");
            }
            return false;
        }
        this.mTempTime.set(j);
        int weeksSinceEpochFromJulianDay = Utils.getWeeksSinceEpochFromJulianDay(Time.getJulianDay(this.mTempTime.normalize(true), this.mTempTime.gmtoff), this.mFirstDayOfWeek);
        int i = 0;
        int top = 0;
        while (true) {
            int i2 = i + 1;
            childAt = this.mListView.getChildAt(i);
            if (childAt == null) {
                break;
            }
            top = childAt.getTop();
            if (Log.isLoggable("MonthFragment", 3)) {
                StringBuilder sb = new StringBuilder();
                sb.append("child at ");
                sb.append(i2 - 1);
                sb.append(" has top ");
                sb.append(top);
                Log.d("MonthFragment", sb.toString());
            }
            if (top >= 0) {
                break;
            }
            i = i2;
        }
        if (childAt != null) {
            positionForView = this.mListView.getPositionForView(childAt);
        } else {
            positionForView = 0;
        }
        int i3 = (this.mNumWeeks + positionForView) - 1;
        if (top > this.BOTTOM_BUFFER) {
            i3--;
        }
        if (z2) {
            this.mAdapter.setSelectedDay(this.mSelectedDay);
        }
        if (Log.isLoggable("MonthFragment", 3)) {
            Log.d("MonthFragment", "GoTo position " + weeksSinceEpochFromJulianDay);
        }
        if (weeksSinceEpochFromJulianDay < positionForView || weeksSinceEpochFromJulianDay > i3 || z3) {
            this.mFirstDayOfMonth.set(this.mTempTime);
            this.mFirstDayOfMonth.monthDay = 1;
            long jNormalize = this.mFirstDayOfMonth.normalize(true);
            setMonthDisplayed(this.mTempTime, true);
            int weeksSinceEpochFromJulianDay2 = Utils.getWeeksSinceEpochFromJulianDay(Time.getJulianDay(jNormalize, this.mFirstDayOfMonth.gmtoff), this.mFirstDayOfWeek);
            this.mPreviousScrollState = 2;
            if (z) {
                this.mListView.smoothScrollToPositionFromTop(weeksSinceEpochFromJulianDay2, LIST_TOP_OFFSET, 500);
                return true;
            }
            this.mListView.setSelectionFromTop(weeksSinceEpochFromJulianDay2, LIST_TOP_OFFSET);
            onScrollStateChanged(this.mListView, 0);
        } else if (z2) {
            setMonthDisplayed(this.mSelectedDay, true);
        }
        return false;
    }

    @Override
    public void onScroll(AbsListView absListView, int i, int i2, int i3) {
        SimpleWeekView simpleWeekView;
        if (!this.mUserScrolled || (simpleWeekView = (SimpleWeekView) absListView.getChildAt(0)) == null) {
            return;
        }
        long firstVisiblePosition = (absListView.getFirstVisiblePosition() * simpleWeekView.getHeight()) - simpleWeekView.getBottom();
        this.mFirstVisibleDay.setJulianDay(simpleWeekView.getFirstJulianDay());
        if (firstVisiblePosition < this.mPreviousScrollPosition) {
            this.mIsScrollingUp = true;
        } else if (firstVisiblePosition > this.mPreviousScrollPosition) {
            this.mIsScrollingUp = false;
        } else {
            return;
        }
        this.mPreviousScrollPosition = firstVisiblePosition;
        this.mPreviousScrollState = this.mCurrentScrollState;
        updateMonthHighlight(this.mListView);
    }

    private void updateMonthHighlight(AbsListView absListView) {
        int lastMonth;
        SimpleWeekView simpleWeekView = (SimpleWeekView) absListView.getChildAt(0);
        if (simpleWeekView == null) {
            return;
        }
        int i = 1;
        SimpleWeekView simpleWeekView2 = (SimpleWeekView) absListView.getChildAt(2 + (simpleWeekView.getBottom() < this.WEEK_MIN_VISIBLE_HEIGHT ? 1 : 0));
        if (simpleWeekView2 == null) {
            return;
        }
        if (this.mIsScrollingUp) {
            lastMonth = simpleWeekView2.getFirstMonth();
        } else {
            lastMonth = simpleWeekView2.getLastMonth();
        }
        if (this.mCurrentMonthDisplayed != 11 || lastMonth != 0) {
            if (this.mCurrentMonthDisplayed == 0 && lastMonth == 11) {
                i = -1;
            } else {
                i = lastMonth - this.mCurrentMonthDisplayed;
            }
        }
        if (i != 0) {
            int firstJulianDay = simpleWeekView2.getFirstJulianDay();
            if (!this.mIsScrollingUp) {
                firstJulianDay += 7;
            }
            this.mTempTime.setJulianDay(firstJulianDay);
            setMonthDisplayed(this.mTempTime, false);
        }
    }

    protected void setMonthDisplayed(Time time, boolean z) {
        CharSequence text = this.mMonthName.getText();
        this.mMonthName.setText(Utils.formatMonthYear(this.mContext, time));
        this.mMonthName.invalidate();
        if (!TextUtils.equals(text, this.mMonthName.getText())) {
            this.mMonthName.sendAccessibilityEvent(8);
        }
        this.mCurrentMonthDisplayed = time.month;
        if (z) {
            this.mAdapter.updateFocusMonth(this.mCurrentMonthDisplayed);
        }
    }

    public void onScrollStateChanged(AbsListView absListView, int i) {
        this.mScrollStateChangedRunnable.doScrollStateChange(absListView, i);
    }

    protected class ScrollStateRunnable implements Runnable {
        private int mNewState;

        protected ScrollStateRunnable() {
        }

        public void doScrollStateChange(AbsListView absListView, int i) {
            SimpleDayPickerFragment.this.mHandler.removeCallbacks(this);
            this.mNewState = i;
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
