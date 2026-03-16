package android.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.MathUtils;
import android.widget.CalendarView;
import android.widget.DayPickerView;
import com.android.internal.R;
import java.util.Calendar;
import java.util.Locale;
import libcore.icu.LocaleData;

class CalendarViewMaterialDelegate extends CalendarView.AbstractCalendarViewDelegate {
    private final DayPickerView mDayPickerView;
    private CalendarView.OnDateChangeListener mOnDateChangeListener;
    private final DayPickerView.OnDaySelectedListener mOnDaySelectedListener;

    public CalendarViewMaterialDelegate(CalendarView delegator, Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(delegator, context);
        this.mOnDaySelectedListener = new DayPickerView.OnDaySelectedListener() {
            @Override
            public void onDaySelected(DayPickerView view, Calendar day) {
                if (CalendarViewMaterialDelegate.this.mOnDateChangeListener != null) {
                    int year = day.get(1);
                    int month = day.get(2);
                    int dayOfMonth = day.get(5);
                    CalendarViewMaterialDelegate.this.mOnDateChangeListener.onSelectedDayChange(CalendarViewMaterialDelegate.this.mDelegator, year, month, dayOfMonth);
                }
            }
        };
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CalendarView, defStyleAttr, defStyleRes);
        int firstDayOfWeek = a.getInt(0, LocaleData.get(Locale.getDefault()).firstDayOfWeek.intValue());
        long minDate = parseDateToMillis(a.getString(2), "01/01/1900");
        long maxDate = parseDateToMillis(a.getString(3), "01/01/2100");
        if (maxDate < minDate) {
            throw new IllegalArgumentException("max date cannot be before min date");
        }
        long setDate = MathUtils.constrain(System.currentTimeMillis(), minDate, maxDate);
        int dateTextAppearanceResId = a.getResourceId(12, 16974259);
        a.recycle();
        this.mDayPickerView = new DayPickerView(context);
        this.mDayPickerView.setFirstDayOfWeek(firstDayOfWeek);
        this.mDayPickerView.setCalendarTextAppearance(dateTextAppearanceResId);
        this.mDayPickerView.setMinDate(minDate);
        this.mDayPickerView.setMaxDate(maxDate);
        this.mDayPickerView.setDate(setDate, false, true);
        this.mDayPickerView.setOnDaySelectedListener(this.mOnDaySelectedListener);
        delegator.addView(this.mDayPickerView);
    }

    private long parseDateToMillis(String dateStr, String defaultDateStr) {
        Calendar tempCalendar = Calendar.getInstance();
        if (TextUtils.isEmpty(dateStr) || !parseDate(dateStr, tempCalendar)) {
            parseDate(defaultDateStr, tempCalendar);
        }
        return tempCalendar.getTimeInMillis();
    }

    @Override
    public void setShownWeekCount(int count) {
    }

    @Override
    public int getShownWeekCount() {
        return 0;
    }

    @Override
    public void setSelectedWeekBackgroundColor(int color) {
    }

    @Override
    public int getSelectedWeekBackgroundColor() {
        return 0;
    }

    @Override
    public void setFocusedMonthDateColor(int color) {
    }

    @Override
    public int getFocusedMonthDateColor() {
        return 0;
    }

    @Override
    public void setUnfocusedMonthDateColor(int color) {
    }

    @Override
    public int getUnfocusedMonthDateColor() {
        return 0;
    }

    @Override
    public void setWeekDayTextAppearance(int resourceId) {
    }

    @Override
    public int getWeekDayTextAppearance() {
        return 0;
    }

    @Override
    public void setDateTextAppearance(int resourceId) {
    }

    @Override
    public int getDateTextAppearance() {
        return 0;
    }

    @Override
    public void setWeekNumberColor(int color) {
    }

    @Override
    public int getWeekNumberColor() {
        return 0;
    }

    @Override
    public void setWeekSeparatorLineColor(int color) {
    }

    @Override
    public int getWeekSeparatorLineColor() {
        return 0;
    }

    @Override
    public void setSelectedDateVerticalBar(int resourceId) {
    }

    @Override
    public void setSelectedDateVerticalBar(Drawable drawable) {
    }

    @Override
    public Drawable getSelectedDateVerticalBar() {
        return null;
    }

    @Override
    public void setMinDate(long minDate) {
        this.mDayPickerView.setMinDate(minDate);
    }

    @Override
    public long getMinDate() {
        return this.mDayPickerView.getMinDate();
    }

    @Override
    public void setMaxDate(long maxDate) {
        this.mDayPickerView.setMaxDate(maxDate);
    }

    @Override
    public long getMaxDate() {
        return this.mDayPickerView.getMaxDate();
    }

    @Override
    public void setShowWeekNumber(boolean showWeekNumber) {
    }

    @Override
    public boolean getShowWeekNumber() {
        return false;
    }

    @Override
    public void setFirstDayOfWeek(int firstDayOfWeek) {
        this.mDayPickerView.setFirstDayOfWeek(firstDayOfWeek);
    }

    @Override
    public int getFirstDayOfWeek() {
        return this.mDayPickerView.getFirstDayOfWeek();
    }

    @Override
    public void setDate(long date) {
        this.mDayPickerView.setDate(date, true, false);
    }

    @Override
    public void setDate(long date, boolean animate, boolean center) {
        this.mDayPickerView.setDate(date, animate, center);
    }

    @Override
    public long getDate() {
        return this.mDayPickerView.getDate();
    }

    @Override
    public void setOnDateChangeListener(CalendarView.OnDateChangeListener listener) {
        this.mOnDateChangeListener = listener;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    }
}
