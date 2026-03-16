package android.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.SimpleMonthView;
import com.android.internal.R;
import java.util.Calendar;

class SimpleMonthAdapter extends BaseAdapter {
    private final Context mContext;
    private int mFirstDayOfWeek;
    private OnDaySelectedListener mOnDaySelectedListener;
    private final Calendar mMinDate = Calendar.getInstance();
    private final Calendar mMaxDate = Calendar.getInstance();
    private Calendar mSelectedDay = Calendar.getInstance();
    private ColorStateList mCalendarTextColors = ColorStateList.valueOf(-16777216);
    private final SimpleMonthView.OnDayClickListener mOnDayClickListener = new SimpleMonthView.OnDayClickListener() {
        @Override
        public void onDayClick(SimpleMonthView view, Calendar day) {
            if (day != null && SimpleMonthAdapter.this.isCalendarInRange(day)) {
                SimpleMonthAdapter.this.setSelectedDay(day);
                if (SimpleMonthAdapter.this.mOnDaySelectedListener != null) {
                    SimpleMonthAdapter.this.mOnDaySelectedListener.onDaySelected(SimpleMonthAdapter.this, day);
                }
            }
        }
    };

    public interface OnDaySelectedListener {
        void onDaySelected(SimpleMonthAdapter simpleMonthAdapter, Calendar calendar);
    }

    public SimpleMonthAdapter(Context context) {
        this.mContext = context;
    }

    public void setRange(Calendar min, Calendar max) {
        this.mMinDate.setTimeInMillis(min.getTimeInMillis());
        this.mMaxDate.setTimeInMillis(max.getTimeInMillis());
        notifyDataSetInvalidated();
    }

    public void setFirstDayOfWeek(int firstDayOfWeek) {
        this.mFirstDayOfWeek = firstDayOfWeek;
        notifyDataSetInvalidated();
    }

    public int getFirstDayOfWeek() {
        return this.mFirstDayOfWeek;
    }

    public void setSelectedDay(Calendar day) {
        this.mSelectedDay = day;
        notifyDataSetChanged();
    }

    public void setOnDaySelectedListener(OnDaySelectedListener listener) {
        this.mOnDaySelectedListener = listener;
    }

    void setCalendarTextColor(ColorStateList colors) {
        this.mCalendarTextColors = colors;
    }

    void setCalendarTextAppearance(int resId) {
        TypedArray a = this.mContext.obtainStyledAttributes(resId, R.styleable.TextAppearance);
        ColorStateList textColor = a.getColorStateList(3);
        if (textColor != null) {
            this.mCalendarTextColors = textColor;
        }
        a.recycle();
    }

    @Override
    public int getCount() {
        int diffYear = this.mMaxDate.get(1) - this.mMinDate.get(1);
        int diffMonth = this.mMaxDate.get(2) - this.mMinDate.get(2);
        return (diffYear * 12) + diffMonth + 1;
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
    public View getView(int position, View convertView, ViewGroup parent) {
        SimpleMonthView v;
        int selectedDay;
        int enabledDayRangeStart;
        int enabledDayRangeEnd;
        if (convertView != null) {
            v = (SimpleMonthView) convertView;
        } else {
            v = new SimpleMonthView(this.mContext);
            AbsListView.LayoutParams params = new AbsListView.LayoutParams(-1, -1);
            v.setLayoutParams(params);
            v.setClickable(true);
            v.setOnDayClickListener(this.mOnDayClickListener);
            if (this.mCalendarTextColors != null) {
                v.setTextColor(this.mCalendarTextColors);
            }
        }
        int minMonth = this.mMinDate.get(2);
        int minYear = this.mMinDate.get(1);
        int currentMonth = position + minMonth;
        int month = currentMonth % 12;
        int year = (currentMonth / 12) + minYear;
        if (isSelectedDayInMonth(year, month)) {
            selectedDay = this.mSelectedDay.get(5);
        } else {
            selectedDay = -1;
        }
        v.reuse();
        if (minMonth == month && minYear == year) {
            enabledDayRangeStart = this.mMinDate.get(5);
        } else {
            enabledDayRangeStart = 1;
        }
        if (this.mMaxDate.get(2) == month && this.mMaxDate.get(1) == year) {
            enabledDayRangeEnd = this.mMaxDate.get(5);
        } else {
            enabledDayRangeEnd = 31;
        }
        v.setMonthParams(selectedDay, month, year, this.mFirstDayOfWeek, enabledDayRangeStart, enabledDayRangeEnd);
        v.invalidate();
        return v;
    }

    private boolean isSelectedDayInMonth(int year, int month) {
        return this.mSelectedDay.get(1) == year && this.mSelectedDay.get(2) == month;
    }

    private boolean isCalendarInRange(Calendar value) {
        return value.compareTo(this.mMinDate) >= 0 && value.compareTo(this.mMaxDate) <= 0;
    }
}
