package com.android.datetimepicker.date;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import com.android.datetimepicker.date.MonthView;
import java.util.Calendar;
import java.util.HashMap;

public abstract class MonthAdapter extends BaseAdapter implements MonthView.OnDayClickListener {
    protected static int WEEK_7_OVERHANG_HEIGHT = 7;
    private final Context mContext;
    protected final DatePickerController mController;
    private CalendarDay mSelectedDay;

    public abstract MonthView createMonthView(Context context);

    public static class CalendarDay {
        private Calendar calendar;
        int day;
        int month;
        int year;

        public CalendarDay() {
            setTime(System.currentTimeMillis());
        }

        public CalendarDay(long timeInMillis) {
            setTime(timeInMillis);
        }

        public CalendarDay(Calendar calendar) {
            this.year = calendar.get(1);
            this.month = calendar.get(2);
            this.day = calendar.get(5);
        }

        public CalendarDay(int year, int month, int day) {
            setDay(year, month, day);
        }

        public void set(CalendarDay date) {
            this.year = date.year;
            this.month = date.month;
            this.day = date.day;
        }

        public void setDay(int year, int month, int day) {
            this.year = year;
            this.month = month;
            this.day = day;
        }

        private void setTime(long timeInMillis) {
            if (this.calendar == null) {
                this.calendar = Calendar.getInstance();
            }
            this.calendar.setTimeInMillis(timeInMillis);
            this.month = this.calendar.get(2);
            this.year = this.calendar.get(1);
            this.day = this.calendar.get(5);
        }
    }

    public MonthAdapter(Context context, DatePickerController controller) {
        this.mContext = context;
        this.mController = controller;
        init();
        setSelectedDay(this.mController.getSelectedDay());
    }

    public void setSelectedDay(CalendarDay day) {
        this.mSelectedDay = day;
        notifyDataSetChanged();
    }

    protected void init() {
        this.mSelectedDay = new CalendarDay(System.currentTimeMillis());
    }

    @Override
    public int getCount() {
        return ((this.mController.getMaxYear() - this.mController.getMinYear()) + 1) * 12;
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
    public boolean hasStableIds() {
        return true;
    }

    @Override
    @SuppressLint({"NewApi"})
    public View getView(int position, View convertView, ViewGroup parent) {
        MonthView v;
        HashMap<String, Integer> drawingParams = null;
        if (convertView != null) {
            v = (MonthView) convertView;
            drawingParams = (HashMap) v.getTag();
        } else {
            v = createMonthView(this.mContext);
            AbsListView.LayoutParams params = new AbsListView.LayoutParams(-1, -1);
            v.setLayoutParams(params);
            v.setClickable(true);
            v.setOnDayClickListener(this);
        }
        if (drawingParams == null) {
            drawingParams = new HashMap<>();
        }
        drawingParams.clear();
        int month = position % 12;
        int year = (position / 12) + this.mController.getMinYear();
        int selectedDay = -1;
        if (isSelectedDayInMonth(year, month)) {
            selectedDay = this.mSelectedDay.day;
        }
        v.reuse();
        drawingParams.put("selected_day", Integer.valueOf(selectedDay));
        drawingParams.put("year", Integer.valueOf(year));
        drawingParams.put("month", Integer.valueOf(month));
        drawingParams.put("week_start", Integer.valueOf(this.mController.getFirstDayOfWeek()));
        v.setMonthParams(drawingParams);
        v.invalidate();
        return v;
    }

    private boolean isSelectedDayInMonth(int year, int month) {
        return this.mSelectedDay.year == year && this.mSelectedDay.month == month;
    }

    @Override
    public void onDayClick(MonthView view, CalendarDay day) {
        if (day != null) {
            onDayTapped(day);
        }
    }

    protected void onDayTapped(CalendarDay day) {
        this.mController.tryVibrate();
        this.mController.onDayOfMonthSelected(day.year, day.month, day.day);
        setSelectedDay(day);
    }
}
