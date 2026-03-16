package com.android.calendar;

import android.content.Context;
import android.os.Handler;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.Formatter;
import java.util.Locale;

public class CalendarViewAdapter extends BaseAdapter {
    private final String[] mButtonNames;
    private final Context mContext;
    private int mCurrentMainView;
    private final LayoutInflater mInflater;
    private Handler mMidnightHandler;
    private long mMilliTime;
    private final boolean mShowDate;
    private String mTimeZone;
    private long mTodayJulianDay;
    private final Runnable mTimeUpdater = new Runnable() {
        @Override
        public void run() {
            CalendarViewAdapter.this.refresh(CalendarViewAdapter.this.mContext);
        }
    };
    private final StringBuilder mStringBuilder = new StringBuilder(50);
    private final Formatter mFormatter = new Formatter(this.mStringBuilder, Locale.getDefault());

    public CalendarViewAdapter(Context context, int viewType, boolean showDate) {
        this.mMidnightHandler = null;
        this.mMidnightHandler = new Handler();
        this.mCurrentMainView = viewType;
        this.mContext = context;
        this.mShowDate = showDate;
        this.mButtonNames = context.getResources().getStringArray(R.array.buttons_list);
        this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        if (showDate) {
            refresh(context);
        }
    }

    public void refresh(Context context) {
        this.mTimeZone = Utils.getTimeZone(context, this.mTimeUpdater);
        Time time = new Time(this.mTimeZone);
        long now = System.currentTimeMillis();
        time.set(now);
        this.mTodayJulianDay = Time.getJulianDay(now, time.gmtoff);
        notifyDataSetChanged();
        setMidnightHandler();
    }

    private void setMidnightHandler() {
        this.mMidnightHandler.removeCallbacks(this.mTimeUpdater);
        long now = System.currentTimeMillis();
        Time time = new Time(this.mTimeZone);
        time.set(now);
        long runInMillis = ((((86400 - (time.hour * 3600)) - (time.minute * 60)) - time.second) + 1) * 1000;
        this.mMidnightHandler.postDelayed(this.mTimeUpdater, runInMillis);
    }

    public void onPause() {
        this.mMidnightHandler.removeCallbacks(this.mTimeUpdater);
    }

    @Override
    public int getCount() {
        return this.mButtonNames.length;
    }

    @Override
    public Object getItem(int position) {
        if (position < this.mButtonNames.length) {
            return this.mButtonNames[position];
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v;
        View v2;
        if (this.mShowDate) {
            if (convertView == null || ((Integer) convertView.getTag()).intValue() != R.layout.actionbar_pulldown_menu_top_button) {
                v2 = this.mInflater.inflate(R.layout.actionbar_pulldown_menu_top_button, parent, false);
                v2.setTag(new Integer(R.layout.actionbar_pulldown_menu_top_button));
            } else {
                v2 = convertView;
            }
            TextView weekDay = (TextView) v2.findViewById(R.id.top_button_weekday);
            TextView date = (TextView) v2.findViewById(R.id.top_button_date);
            switch (this.mCurrentMainView) {
                case 1:
                    weekDay.setVisibility(0);
                    weekDay.setText(buildDayOfWeek());
                    date.setText(buildFullDate());
                    break;
                case 2:
                    weekDay.setVisibility(0);
                    weekDay.setText(buildDayOfWeek());
                    date.setText(buildFullDate());
                    break;
                case 3:
                    if (Utils.getShowWeekNumber(this.mContext)) {
                        weekDay.setVisibility(0);
                        weekDay.setText(buildWeekNum());
                    } else {
                        weekDay.setVisibility(8);
                    }
                    date.setText(buildMonthYearDate());
                    break;
                case 4:
                    weekDay.setVisibility(8);
                    date.setText(buildMonthYearDate());
                    break;
            }
            return v2;
        }
        if (convertView == null || ((Integer) convertView.getTag()).intValue() != R.layout.actionbar_pulldown_menu_top_button_no_date) {
            v = this.mInflater.inflate(R.layout.actionbar_pulldown_menu_top_button_no_date, parent, false);
            v.setTag(new Integer(R.layout.actionbar_pulldown_menu_top_button_no_date));
        } else {
            v = convertView;
        }
        TextView title = (TextView) v;
        switch (this.mCurrentMainView) {
            case 1:
                title.setText(this.mButtonNames[3]);
                break;
            case 2:
                title.setText(this.mButtonNames[0]);
                break;
            case 3:
                title.setText(this.mButtonNames[1]);
                break;
            case 4:
                title.setText(this.mButtonNames[2]);
                break;
        }
        return v2;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return this.mButtonNames.length == 0;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View v = this.mInflater.inflate(R.layout.actionbar_pulldown_menu_button, parent, false);
        TextView viewType = (TextView) v.findViewById(R.id.button_view);
        TextView date = (TextView) v.findViewById(R.id.button_date);
        switch (position) {
            case 0:
                viewType.setText(this.mButtonNames[0]);
                if (this.mShowDate) {
                    date.setText(buildMonthDayDate());
                    return v;
                }
                return v;
            case 1:
                viewType.setText(this.mButtonNames[1]);
                if (this.mShowDate) {
                    date.setText(buildWeekDate());
                    return v;
                }
                return v;
            case 2:
                viewType.setText(this.mButtonNames[2]);
                if (this.mShowDate) {
                    date.setText(buildMonthDate());
                    return v;
                }
                return v;
            case 3:
                viewType.setText(this.mButtonNames[3]);
                if (this.mShowDate) {
                    date.setText(buildMonthDayDate());
                    return v;
                }
                return v;
            default:
                return convertView;
        }
    }

    public void setMainView(int viewType) {
        this.mCurrentMainView = viewType;
        notifyDataSetChanged();
    }

    public void setTime(long time) {
        this.mMilliTime = time;
        notifyDataSetChanged();
    }

    private String buildDayOfWeek() {
        String dayOfWeek;
        Time t = new Time(this.mTimeZone);
        t.set(this.mMilliTime);
        long julianDay = Time.getJulianDay(this.mMilliTime, t.gmtoff);
        this.mStringBuilder.setLength(0);
        if (julianDay == this.mTodayJulianDay) {
            dayOfWeek = this.mContext.getString(R.string.agenda_today, DateUtils.formatDateRange(this.mContext, this.mFormatter, this.mMilliTime, this.mMilliTime, 2, this.mTimeZone).toString());
        } else if (julianDay == this.mTodayJulianDay - 1) {
            dayOfWeek = this.mContext.getString(R.string.agenda_yesterday, DateUtils.formatDateRange(this.mContext, this.mFormatter, this.mMilliTime, this.mMilliTime, 2, this.mTimeZone).toString());
        } else if (julianDay == this.mTodayJulianDay + 1) {
            dayOfWeek = this.mContext.getString(R.string.agenda_tomorrow, DateUtils.formatDateRange(this.mContext, this.mFormatter, this.mMilliTime, this.mMilliTime, 2, this.mTimeZone).toString());
        } else {
            dayOfWeek = DateUtils.formatDateRange(this.mContext, this.mFormatter, this.mMilliTime, this.mMilliTime, 2, this.mTimeZone).toString();
        }
        return dayOfWeek.toUpperCase();
    }

    private String buildFullDate() {
        this.mStringBuilder.setLength(0);
        String date = DateUtils.formatDateRange(this.mContext, this.mFormatter, this.mMilliTime, this.mMilliTime, 20, this.mTimeZone).toString();
        return date;
    }

    private String buildMonthYearDate() {
        this.mStringBuilder.setLength(0);
        String date = DateUtils.formatDateRange(this.mContext, this.mFormatter, this.mMilliTime, this.mMilliTime, 52, this.mTimeZone).toString();
        return date;
    }

    private String buildMonthDayDate() {
        this.mStringBuilder.setLength(0);
        String date = DateUtils.formatDateRange(this.mContext, this.mFormatter, this.mMilliTime, this.mMilliTime, 24, this.mTimeZone).toString();
        return date;
    }

    private String buildMonthDate() {
        this.mStringBuilder.setLength(0);
        String date = DateUtils.formatDateRange(this.mContext, this.mFormatter, this.mMilliTime, this.mMilliTime, 56, this.mTimeZone).toString();
        return date;
    }

    private String buildWeekDate() {
        Time t = new Time(this.mTimeZone);
        t.set(this.mMilliTime);
        int firstDayOfWeek = Utils.getFirstDayOfWeek(this.mContext);
        int dayOfWeek = t.weekDay;
        int diff = dayOfWeek - firstDayOfWeek;
        if (diff != 0) {
            if (diff < 0) {
                diff += 7;
            }
            t.monthDay -= diff;
            t.normalize(true);
        }
        long weekStartTime = t.toMillis(true);
        long weekEndTime = (604800000 + weekStartTime) - 86400000;
        Time t1 = new Time(this.mTimeZone);
        t.set(weekEndTime);
        int flags = t.month != t1.month ? 24 | 65536 : 24;
        this.mStringBuilder.setLength(0);
        String date = DateUtils.formatDateRange(this.mContext, this.mFormatter, weekStartTime, weekEndTime, flags, this.mTimeZone).toString();
        return date;
    }

    private String buildWeekNum() {
        int week = Utils.getWeekNumberFromTime(this.mMilliTime, this.mContext);
        return this.mContext.getResources().getQuantityString(R.plurals.weekN, week, Integer.valueOf(week));
    }
}
