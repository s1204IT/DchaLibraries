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
import com.mediatek.calendar.ext.ILunarExt;
import com.mediatek.calendar.ext.OpCalendarCustomizationFactoryBase;
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

    public CalendarViewAdapter(Context context, int i, boolean z) {
        this.mMidnightHandler = null;
        this.mMidnightHandler = new Handler();
        this.mCurrentMainView = i;
        this.mContext = context;
        this.mShowDate = z;
        this.mButtonNames = context.getResources().getStringArray(R.array.buttons_list);
        this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        if (z) {
            refresh(context);
        }
    }

    public void refresh(Context context) {
        this.mTimeZone = Utils.getTimeZone(context, this.mTimeUpdater);
        new Time(this.mTimeZone).set(System.currentTimeMillis());
        this.mTodayJulianDay = Time.getJulianDay(r0, r5.gmtoff);
        notifyDataSetChanged();
        setMidnightHandler();
    }

    private void setMidnightHandler() {
        this.mMidnightHandler.removeCallbacks(this.mTimeUpdater);
        long jCurrentTimeMillis = System.currentTimeMillis();
        new Time(this.mTimeZone).set(jCurrentTimeMillis);
        this.mMidnightHandler.postDelayed(this.mTimeUpdater, ((((86400 - (r2.hour * 3600)) - (r2.minute * 60)) - r2.second) + 1) * 1000);
    }

    public void onPause() {
        this.mMidnightHandler.removeCallbacks(this.mTimeUpdater);
    }

    @Override
    public int getCount() {
        return this.mButtonNames.length;
    }

    @Override
    public Object getItem(int i) {
        if (i < this.mButtonNames.length) {
            return this.mButtonNames[i];
        }
        return null;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (this.mShowDate) {
            if (view == null || ((Integer) view.getTag()).intValue() != R.layout.actionbar_pulldown_menu_top_button) {
                view = this.mInflater.inflate(R.layout.actionbar_pulldown_menu_top_button, viewGroup, false);
                view.setTag(new Integer(R.layout.actionbar_pulldown_menu_top_button));
            }
            TextView textView = (TextView) view.findViewById(R.id.top_button_weekday);
            TextView textView2 = (TextView) view.findViewById(R.id.top_button_date);
            switch (this.mCurrentMainView) {
                case 1:
                    ILunarExt iLunarExtMakeLunarCalendar = OpCalendarCustomizationFactoryBase.getOpFactory(this.mContext).makeLunarCalendar(this.mContext);
                    textView.setVisibility(0);
                    textView.setText(buildDayOfWeek() + iLunarExtMakeLunarCalendar.buildLunarDate(null, this.mTimeZone, this.mMilliTime));
                    textView2.setText(buildFullDate());
                    break;
                case 2:
                    textView.setVisibility(0);
                    textView.setText(buildDayOfWeek());
                    textView2.setText(buildFullDate());
                    break;
                case 3:
                    if (Utils.getShowWeekNumber(this.mContext)) {
                        textView.setVisibility(0);
                        textView.setText(buildWeekNum());
                    } else {
                        textView.setVisibility(8);
                    }
                    textView2.setText(buildMonthYearDate());
                    break;
                case 4:
                    textView.setVisibility(8);
                    textView2.setText(buildMonthYearDate());
                    break;
                default:
                    return null;
            }
            return view;
        }
        if (view == null || ((Integer) view.getTag()).intValue() != R.layout.actionbar_pulldown_menu_top_button_no_date) {
            view = this.mInflater.inflate(R.layout.actionbar_pulldown_menu_top_button_no_date, viewGroup, false);
            view.setTag(new Integer(R.layout.actionbar_pulldown_menu_top_button_no_date));
        }
        TextView textView3 = (TextView) view;
        switch (this.mCurrentMainView) {
            case 1:
                textView3.setText(this.mButtonNames[3]);
                break;
            case 2:
                textView3.setText(this.mButtonNames[0]);
                break;
            case 3:
                textView3.setText(this.mButtonNames[1]);
                break;
            case 4:
                textView3.setText(this.mButtonNames[2]);
                break;
            default:
                return null;
        }
        return view;
    }

    @Override
    public int getItemViewType(int i) {
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
    public View getDropDownView(int i, View view, ViewGroup viewGroup) {
        View viewInflate = this.mInflater.inflate(R.layout.actionbar_pulldown_menu_button, viewGroup, false);
        TextView textView = (TextView) viewInflate.findViewById(R.id.button_view);
        TextView textView2 = (TextView) viewInflate.findViewById(R.id.button_date);
        switch (i) {
            case 0:
                textView.setText(this.mButtonNames[0]);
                if (this.mShowDate) {
                    textView2.setText(buildMonthDayDate());
                }
                break;
            case 1:
                textView.setText(this.mButtonNames[1]);
                if (this.mShowDate) {
                    textView2.setText(buildWeekDate());
                }
                break;
            case 2:
                textView.setText(this.mButtonNames[2]);
                if (this.mShowDate) {
                    textView2.setText(buildMonthDate());
                }
                break;
            case 3:
                textView.setText(this.mButtonNames[3]);
                if (this.mShowDate) {
                    textView2.setText(buildMonthDayDate());
                }
                break;
            default:
                return view;
        }
        return viewInflate;
    }

    public void setMainView(int i) {
        this.mCurrentMainView = i;
        notifyDataSetChanged();
    }

    public void setTime(long j) {
        this.mMilliTime = j;
        notifyDataSetChanged();
    }

    private String buildDayOfWeek() {
        String string;
        Time time = new Time(this.mTimeZone);
        time.set(this.mMilliTime);
        long julianDay = Time.getJulianDay(this.mMilliTime, time.gmtoff);
        this.mStringBuilder.setLength(0);
        if (julianDay == this.mTodayJulianDay) {
            string = this.mContext.getString(R.string.agenda_today, DateUtils.formatDateRange(this.mContext, this.mFormatter, this.mMilliTime, this.mMilliTime, 2, this.mTimeZone).toString());
        } else if (julianDay == this.mTodayJulianDay - 1) {
            string = this.mContext.getString(R.string.agenda_yesterday, DateUtils.formatDateRange(this.mContext, this.mFormatter, this.mMilliTime, this.mMilliTime, 2, this.mTimeZone).toString());
        } else if (julianDay == this.mTodayJulianDay + 1) {
            string = this.mContext.getString(R.string.agenda_tomorrow, DateUtils.formatDateRange(this.mContext, this.mFormatter, this.mMilliTime, this.mMilliTime, 2, this.mTimeZone).toString());
        } else {
            string = DateUtils.formatDateRange(this.mContext, this.mFormatter, this.mMilliTime, this.mMilliTime, 2, this.mTimeZone).toString();
        }
        return string.toUpperCase();
    }

    private String buildFullDate() {
        this.mStringBuilder.setLength(0);
        return DateUtils.formatDateRange(this.mContext, this.mFormatter, this.mMilliTime, this.mMilliTime, 20, this.mTimeZone).toString();
    }

    private String buildMonthYearDate() {
        this.mStringBuilder.setLength(0);
        return DateUtils.formatDateRange(this.mContext, this.mFormatter, this.mMilliTime, this.mMilliTime, 52, this.mTimeZone).toString();
    }

    private String buildMonthDayDate() {
        this.mStringBuilder.setLength(0);
        return DateUtils.formatDateRange(this.mContext, this.mFormatter, this.mMilliTime, this.mMilliTime, 24, this.mTimeZone).toString();
    }

    private String buildMonthDate() {
        this.mStringBuilder.setLength(0);
        return DateUtils.formatDateRange(this.mContext, this.mFormatter, this.mMilliTime, this.mMilliTime, 56, this.mTimeZone).toString();
    }

    private String buildWeekDate() {
        int i;
        Time time = new Time(this.mTimeZone);
        time.set(this.mMilliTime);
        time.hour = 9;
        time.minute = 0;
        time.second = 0;
        int firstDayOfWeek = time.weekDay - Utils.getFirstDayOfWeek(this.mContext);
        if (firstDayOfWeek != 0) {
            if (firstDayOfWeek < 0) {
                firstDayOfWeek += 7;
            }
            time.monthDay -= firstDayOfWeek;
            time.normalize(true);
        }
        long millis = time.toMillis(true);
        long j = (604800000 + millis) - 86400000;
        Time time2 = new Time(this.mTimeZone);
        time2.set(j);
        if (time.month == time2.month) {
            i = 24;
        } else {
            i = 65560;
        }
        this.mStringBuilder.setLength(0);
        return DateUtils.formatDateRange(this.mContext, this.mFormatter, millis, j, i, this.mTimeZone).toString();
    }

    private String buildWeekNum() {
        int weekNumberFromTime = Utils.getWeekNumberFromTime(this.mMilliTime, this.mContext);
        return this.mContext.getResources().getQuantityString(R.plurals.weekN, weekNumberFromTime, Integer.valueOf(weekNumberFromTime));
    }
}
