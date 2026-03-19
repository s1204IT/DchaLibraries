package com.android.calendar.month;

import android.content.Context;
import android.text.format.Time;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import com.android.calendar.Utils;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

public class SimpleWeeksAdapter extends BaseAdapter implements View.OnTouchListener {
    protected Context mContext;
    protected GestureDetector mGestureDetector;
    ListView mListView;
    protected Time mSelectedDay;
    protected int mSelectedWeek;
    protected static int DEFAULT_NUM_WEEKS = 6;
    protected static int DEFAULT_MONTH_FOCUS = 0;
    protected static int DEFAULT_DAYS_PER_WEEK = 7;
    protected static int DEFAULT_WEEK_HEIGHT = 32;
    protected static int WEEK_7_OVERHANG_HEIGHT = 7;
    protected static float mScale = 0.0f;
    protected boolean mShowWeekNumber = false;
    protected int mNumWeeks = DEFAULT_NUM_WEEKS;
    protected int mDaysPerWeek = DEFAULT_DAYS_PER_WEEK;
    protected int mFocusMonth = DEFAULT_MONTH_FOCUS;
    protected int mFirstDayOfWeek = Calendar.getInstance(Locale.getDefault()).getFirstDayOfWeek() - 1;

    public SimpleWeeksAdapter(Context context, HashMap<String, Integer> map) {
        this.mContext = context;
        if (mScale == 0.0f) {
            mScale = context.getResources().getDisplayMetrics().density;
            if (mScale != 1.0f) {
                WEEK_7_OVERHANG_HEIGHT = (int) (WEEK_7_OVERHANG_HEIGHT * mScale);
            }
        }
        init();
        updateParams(map);
    }

    protected void init() {
        this.mGestureDetector = new GestureDetector(this.mContext, new CalendarGestureListener());
        this.mSelectedDay = new Time();
        this.mSelectedDay.setToNow();
    }

    public void updateParams(HashMap<String, Integer> map) {
        if (map == null) {
            Log.e("MonthByWeek", "WeekParameters are null! Cannot update adapter.");
            return;
        }
        if (map.containsKey("focus_month")) {
            this.mFocusMonth = map.get("focus_month").intValue();
        }
        if (map.containsKey("focus_month")) {
            this.mNumWeeks = map.get("num_weeks").intValue();
        }
        if (map.containsKey("week_numbers")) {
            this.mShowWeekNumber = map.get("week_numbers").intValue() != 0;
        }
        if (map.containsKey("week_start")) {
            this.mFirstDayOfWeek = map.get("week_start").intValue();
        }
        if (map.containsKey("selected_day")) {
            int iIntValue = map.get("selected_day").intValue();
            Utils.setJulianDayInGeneral(this.mSelectedDay, iIntValue);
            this.mSelectedWeek = Utils.getWeeksSinceEpochFromJulianDay(iIntValue, this.mFirstDayOfWeek);
        }
        if (map.containsKey("days_per_week")) {
            this.mDaysPerWeek = map.get("days_per_week").intValue();
        }
        refresh();
    }

    public void setSelectedDay(Time time) {
        this.mSelectedDay.set(time);
        this.mSelectedWeek = Utils.getWeeksSinceEpochFromJulianDay(Time.getJulianDay(this.mSelectedDay.normalize(true), this.mSelectedDay.gmtoff), this.mFirstDayOfWeek);
        notifyDataSetChanged();
    }

    public Time getSelectedDay() {
        return this.mSelectedDay;
    }

    protected void refresh() {
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return 3497;
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        SimpleWeekView simpleWeekView;
        HashMap<String, Integer> map;
        int i2 = -1;
        if (view != null) {
            simpleWeekView = (SimpleWeekView) view;
            map = (HashMap) simpleWeekView.getTag();
        } else {
            simpleWeekView = new SimpleWeekView(this.mContext);
            simpleWeekView.setLayoutParams(new AbsListView.LayoutParams(-1, -1));
            simpleWeekView.setClickable(true);
            simpleWeekView.setOnTouchListener(this);
            map = null;
        }
        if (map == null) {
            map = new HashMap<>();
        }
        map.clear();
        if (this.mSelectedWeek == i) {
            i2 = this.mSelectedDay.weekDay;
        }
        map.put("height", Integer.valueOf((viewGroup.getHeight() - WEEK_7_OVERHANG_HEIGHT) / this.mNumWeeks));
        map.put("selected_day", Integer.valueOf(i2));
        map.put("show_wk_num", Integer.valueOf(this.mShowWeekNumber ? 1 : 0));
        map.put("week_start", Integer.valueOf(this.mFirstDayOfWeek));
        map.put("num_days", Integer.valueOf(this.mDaysPerWeek));
        map.put("week", Integer.valueOf(i));
        map.put("focus_month", Integer.valueOf(this.mFocusMonth));
        simpleWeekView.setWeekParams(map, this.mSelectedDay.timezone);
        simpleWeekView.invalidate();
        return simpleWeekView;
    }

    public void updateFocusMonth(int i) {
        this.mFocusMonth = i;
        notifyDataSetChanged();
    }

    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (this.mGestureDetector.onTouchEvent(motionEvent)) {
            SimpleWeekView simpleWeekView = (SimpleWeekView) view;
            Time dayFromLocation = simpleWeekView.getDayFromLocation(motionEvent.getX());
            if (Log.isLoggable("MonthByWeek", 3)) {
                Log.d("MonthByWeek", "Touched day at Row=" + simpleWeekView.mWeek + " day=" + dayFromLocation.toString());
            }
            if (dayFromLocation != null) {
                onDayTapped(dayFromLocation);
                return true;
            }
            return true;
        }
        return false;
    }

    protected void onDayTapped(Time time) {
        time.hour = this.mSelectedDay.hour;
        time.minute = this.mSelectedDay.minute;
        time.second = this.mSelectedDay.second;
        setSelectedDay(time);
    }

    protected class CalendarGestureListener extends GestureDetector.SimpleOnGestureListener {
        protected CalendarGestureListener() {
        }

        @Override
        public boolean onSingleTapUp(MotionEvent motionEvent) {
            return true;
        }
    }

    public void setListView(ListView listView) {
        this.mListView = listView;
    }
}
