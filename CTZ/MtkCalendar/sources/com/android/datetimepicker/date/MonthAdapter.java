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

        public CalendarDay(long j) {
            setTime(j);
        }

        public CalendarDay(Calendar calendar) {
            this.year = calendar.get(1);
            this.month = calendar.get(2);
            this.day = calendar.get(5);
        }

        public CalendarDay(int i, int i2, int i3) {
            setDay(i, i2, i3);
        }

        public void set(CalendarDay calendarDay) {
            this.year = calendarDay.year;
            this.month = calendarDay.month;
            this.day = calendarDay.day;
        }

        public void setDay(int i, int i2, int i3) {
            this.year = i;
            this.month = i2;
            this.day = i3;
        }

        private void setTime(long j) {
            if (this.calendar == null) {
                this.calendar = Calendar.getInstance();
            }
            this.calendar.setTimeInMillis(j);
            this.month = this.calendar.get(2);
            this.year = this.calendar.get(1);
            this.day = this.calendar.get(5);
        }

        public int getYear() {
            return this.year;
        }

        public int getMonth() {
            return this.month;
        }

        public int getDay() {
            return this.day;
        }
    }

    public MonthAdapter(Context context, DatePickerController datePickerController) {
        this.mContext = context;
        this.mController = datePickerController;
        init();
        setSelectedDay(this.mController.getSelectedDay());
    }

    public void setSelectedDay(CalendarDay calendarDay) {
        this.mSelectedDay = calendarDay;
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
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    @SuppressLint({"NewApi"})
    public View getView(int i, View view, ViewGroup viewGroup) {
        MonthView monthViewCreateMonthView;
        HashMap<String, Integer> map;
        int i2 = -1;
        if (view != null) {
            monthViewCreateMonthView = (MonthView) view;
            map = (HashMap) monthViewCreateMonthView.getTag();
        } else {
            monthViewCreateMonthView = createMonthView(this.mContext);
            monthViewCreateMonthView.setLayoutParams(new AbsListView.LayoutParams(-1, -1));
            monthViewCreateMonthView.setClickable(true);
            monthViewCreateMonthView.setOnDayClickListener(this);
            map = null;
        }
        if (map == null) {
            map = new HashMap<>();
        }
        map.clear();
        int i3 = i % 12;
        int minYear = (i / 12) + this.mController.getMinYear();
        if (isSelectedDayInMonth(minYear, i3)) {
            i2 = this.mSelectedDay.day;
        }
        monthViewCreateMonthView.reuse();
        map.put("selected_day", Integer.valueOf(i2));
        map.put("year", Integer.valueOf(minYear));
        map.put("month", Integer.valueOf(i3));
        map.put("week_start", Integer.valueOf(this.mController.getFirstDayOfWeek()));
        monthViewCreateMonthView.setMonthParams(map);
        monthViewCreateMonthView.invalidate();
        return monthViewCreateMonthView;
    }

    private boolean isSelectedDayInMonth(int i, int i2) {
        return this.mSelectedDay.year == i && this.mSelectedDay.month == i2;
    }

    @Override
    public void onDayClick(MonthView monthView, CalendarDay calendarDay) {
        if (calendarDay != null) {
            onDayTapped(calendarDay);
        }
    }

    protected void onDayTapped(CalendarDay calendarDay) {
        this.mController.tryVibrate();
        this.mController.onDayOfMonthSelected(calendarDay.year, calendarDay.month, calendarDay.day);
        setSelectedDay(calendarDay);
    }
}
