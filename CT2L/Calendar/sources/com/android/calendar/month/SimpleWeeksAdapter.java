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
    protected int mFirstDayOfWeek;
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

    public SimpleWeeksAdapter(Context context, HashMap<String, Integer> params) {
        this.mContext = context;
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        this.mFirstDayOfWeek = cal.getFirstDayOfWeek() - 1;
        if (mScale == 0.0f) {
            mScale = context.getResources().getDisplayMetrics().density;
            if (mScale != 1.0f) {
                WEEK_7_OVERHANG_HEIGHT = (int) (WEEK_7_OVERHANG_HEIGHT * mScale);
            }
        }
        init();
        updateParams(params);
    }

    protected void init() {
        this.mGestureDetector = new GestureDetector(this.mContext, new CalendarGestureListener());
        this.mSelectedDay = new Time();
        this.mSelectedDay.setToNow();
    }

    public void updateParams(HashMap<String, Integer> params) {
        if (params == null) {
            Log.e("MonthByWeek", "WeekParameters are null! Cannot update adapter.");
            return;
        }
        if (params.containsKey("focus_month")) {
            this.mFocusMonth = params.get("focus_month").intValue();
        }
        if (params.containsKey("focus_month")) {
            this.mNumWeeks = params.get("num_weeks").intValue();
        }
        if (params.containsKey("week_numbers")) {
            this.mShowWeekNumber = params.get("week_numbers").intValue() != 0;
        }
        if (params.containsKey("week_start")) {
            this.mFirstDayOfWeek = params.get("week_start").intValue();
        }
        if (params.containsKey("selected_day")) {
            int julianDay = params.get("selected_day").intValue();
            this.mSelectedDay.setJulianDay(julianDay);
            this.mSelectedWeek = Utils.getWeeksSinceEpochFromJulianDay(julianDay, this.mFirstDayOfWeek);
        }
        if (params.containsKey("days_per_week")) {
            this.mDaysPerWeek = params.get("days_per_week").intValue();
        }
        refresh();
    }

    public void setSelectedDay(Time selectedTime) {
        this.mSelectedDay.set(selectedTime);
        long millis = this.mSelectedDay.normalize(true);
        this.mSelectedWeek = Utils.getWeeksSinceEpochFromJulianDay(Time.getJulianDay(millis, this.mSelectedDay.gmtoff), this.mFirstDayOfWeek);
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
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SimpleWeekView v;
        HashMap<String, Integer> drawingParams = null;
        if (convertView != null) {
            v = (SimpleWeekView) convertView;
            drawingParams = (HashMap) v.getTag();
        } else {
            v = new SimpleWeekView(this.mContext);
            AbsListView.LayoutParams params = new AbsListView.LayoutParams(-1, -1);
            v.setLayoutParams(params);
            v.setClickable(true);
            v.setOnTouchListener(this);
        }
        if (drawingParams == null) {
            drawingParams = new HashMap<>();
        }
        drawingParams.clear();
        int selectedDay = -1;
        if (this.mSelectedWeek == position) {
            selectedDay = this.mSelectedDay.weekDay;
        }
        drawingParams.put("height", Integer.valueOf((parent.getHeight() - WEEK_7_OVERHANG_HEIGHT) / this.mNumWeeks));
        drawingParams.put("selected_day", Integer.valueOf(selectedDay));
        drawingParams.put("show_wk_num", Integer.valueOf(this.mShowWeekNumber ? 1 : 0));
        drawingParams.put("week_start", Integer.valueOf(this.mFirstDayOfWeek));
        drawingParams.put("num_days", Integer.valueOf(this.mDaysPerWeek));
        drawingParams.put("week", Integer.valueOf(position));
        drawingParams.put("focus_month", Integer.valueOf(this.mFocusMonth));
        v.setWeekParams(drawingParams, this.mSelectedDay.timezone);
        v.invalidate();
        return v;
    }

    public void updateFocusMonth(int month) {
        this.mFocusMonth = month;
        notifyDataSetChanged();
    }

    public boolean onTouch(View v, MotionEvent event) {
        if (!this.mGestureDetector.onTouchEvent(event)) {
            return false;
        }
        SimpleWeekView view = (SimpleWeekView) v;
        Time day = ((SimpleWeekView) v).getDayFromLocation(event.getX());
        if (Log.isLoggable("MonthByWeek", 3)) {
            Log.d("MonthByWeek", "Touched day at Row=" + view.mWeek + " day=" + day.toString());
        }
        if (day != null) {
            onDayTapped(day);
        }
        return true;
    }

    protected void onDayTapped(Time day) {
        day.hour = this.mSelectedDay.hour;
        day.minute = this.mSelectedDay.minute;
        day.second = this.mSelectedDay.second;
        setSelectedDay(day);
    }

    protected class CalendarGestureListener extends GestureDetector.SimpleOnGestureListener {
        protected CalendarGestureListener() {
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return true;
        }
    }

    public void setListView(ListView lv) {
        this.mListView = lv;
    }
}
