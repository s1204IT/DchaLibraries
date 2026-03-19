package android.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.icu.util.Calendar;
import android.util.AttributeSet;
import android.util.Log;
import com.android.internal.R;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class CalendarView extends FrameLayout {
    private static final String DATE_FORMAT = "MM/dd/yyyy";
    private static final DateFormat DATE_FORMATTER = new SimpleDateFormat(DATE_FORMAT);
    private static final String LOG_TAG = "CalendarView";
    private static final int MODE_HOLO = 0;
    private static final int MODE_MATERIAL = 1;
    private final CalendarViewDelegate mDelegate;

    private interface CalendarViewDelegate {
        boolean getBoundsForDate(long j, Rect rect);

        long getDate();

        int getDateTextAppearance();

        int getFirstDayOfWeek();

        int getFocusedMonthDateColor();

        long getMaxDate();

        long getMinDate();

        Drawable getSelectedDateVerticalBar();

        int getSelectedWeekBackgroundColor();

        boolean getShowWeekNumber();

        int getShownWeekCount();

        int getUnfocusedMonthDateColor();

        int getWeekDayTextAppearance();

        int getWeekNumberColor();

        int getWeekSeparatorLineColor();

        void onConfigurationChanged(Configuration configuration);

        void setDate(long j);

        void setDate(long j, boolean z, boolean z2);

        void setDateTextAppearance(int i);

        void setFirstDayOfWeek(int i);

        void setFocusedMonthDateColor(int i);

        void setMaxDate(long j);

        void setMinDate(long j);

        void setOnDateChangeListener(OnDateChangeListener onDateChangeListener);

        void setSelectedDateVerticalBar(int i);

        void setSelectedDateVerticalBar(Drawable drawable);

        void setSelectedWeekBackgroundColor(int i);

        void setShowWeekNumber(boolean z);

        void setShownWeekCount(int i);

        void setUnfocusedMonthDateColor(int i);

        void setWeekDayTextAppearance(int i);

        void setWeekNumberColor(int i);

        void setWeekSeparatorLineColor(int i);
    }

    public interface OnDateChangeListener {
        void onSelectedDayChange(CalendarView calendarView, int i, int i2, int i3);
    }

    public CalendarView(Context context) {
        this(context, null);
    }

    public CalendarView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 16843613);
    }

    public CalendarView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public CalendarView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.CalendarView, i, i2);
        int i3 = typedArrayObtainStyledAttributes.getInt(13, 0);
        typedArrayObtainStyledAttributes.recycle();
        switch (i3) {
            case 0:
                this.mDelegate = new CalendarViewLegacyDelegate(this, context, attributeSet, i, i2);
                return;
            case 1:
                this.mDelegate = new CalendarViewMaterialDelegate(this, context, attributeSet, i, i2);
                return;
            default:
                throw new IllegalArgumentException("invalid calendarViewMode attribute");
        }
    }

    @Deprecated
    public void setShownWeekCount(int i) {
        this.mDelegate.setShownWeekCount(i);
    }

    @Deprecated
    public int getShownWeekCount() {
        return this.mDelegate.getShownWeekCount();
    }

    @Deprecated
    public void setSelectedWeekBackgroundColor(int i) {
        this.mDelegate.setSelectedWeekBackgroundColor(i);
    }

    @Deprecated
    public int getSelectedWeekBackgroundColor() {
        return this.mDelegate.getSelectedWeekBackgroundColor();
    }

    @Deprecated
    public void setFocusedMonthDateColor(int i) {
        this.mDelegate.setFocusedMonthDateColor(i);
    }

    @Deprecated
    public int getFocusedMonthDateColor() {
        return this.mDelegate.getFocusedMonthDateColor();
    }

    @Deprecated
    public void setUnfocusedMonthDateColor(int i) {
        this.mDelegate.setUnfocusedMonthDateColor(i);
    }

    @Deprecated
    public int getUnfocusedMonthDateColor() {
        return this.mDelegate.getUnfocusedMonthDateColor();
    }

    @Deprecated
    public void setWeekNumberColor(int i) {
        this.mDelegate.setWeekNumberColor(i);
    }

    @Deprecated
    public int getWeekNumberColor() {
        return this.mDelegate.getWeekNumberColor();
    }

    @Deprecated
    public void setWeekSeparatorLineColor(int i) {
        this.mDelegate.setWeekSeparatorLineColor(i);
    }

    @Deprecated
    public int getWeekSeparatorLineColor() {
        return this.mDelegate.getWeekSeparatorLineColor();
    }

    @Deprecated
    public void setSelectedDateVerticalBar(int i) {
        this.mDelegate.setSelectedDateVerticalBar(i);
    }

    @Deprecated
    public void setSelectedDateVerticalBar(Drawable drawable) {
        this.mDelegate.setSelectedDateVerticalBar(drawable);
    }

    @Deprecated
    public Drawable getSelectedDateVerticalBar() {
        return this.mDelegate.getSelectedDateVerticalBar();
    }

    public void setWeekDayTextAppearance(int i) {
        this.mDelegate.setWeekDayTextAppearance(i);
    }

    public int getWeekDayTextAppearance() {
        return this.mDelegate.getWeekDayTextAppearance();
    }

    public void setDateTextAppearance(int i) {
        this.mDelegate.setDateTextAppearance(i);
    }

    public int getDateTextAppearance() {
        return this.mDelegate.getDateTextAppearance();
    }

    public long getMinDate() {
        return this.mDelegate.getMinDate();
    }

    public void setMinDate(long j) {
        this.mDelegate.setMinDate(j);
    }

    public long getMaxDate() {
        return this.mDelegate.getMaxDate();
    }

    public void setMaxDate(long j) {
        this.mDelegate.setMaxDate(j);
    }

    @Deprecated
    public void setShowWeekNumber(boolean z) {
        this.mDelegate.setShowWeekNumber(z);
    }

    @Deprecated
    public boolean getShowWeekNumber() {
        return this.mDelegate.getShowWeekNumber();
    }

    public int getFirstDayOfWeek() {
        return this.mDelegate.getFirstDayOfWeek();
    }

    public void setFirstDayOfWeek(int i) {
        this.mDelegate.setFirstDayOfWeek(i);
    }

    public void setOnDateChangeListener(OnDateChangeListener onDateChangeListener) {
        this.mDelegate.setOnDateChangeListener(onDateChangeListener);
    }

    public long getDate() {
        return this.mDelegate.getDate();
    }

    public void setDate(long j) {
        this.mDelegate.setDate(j);
    }

    public void setDate(long j, boolean z, boolean z2) {
        this.mDelegate.setDate(j, z, z2);
    }

    public boolean getBoundsForDate(long j, Rect rect) {
        return this.mDelegate.getBoundsForDate(j, rect);
    }

    @Override
    protected void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        this.mDelegate.onConfigurationChanged(configuration);
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return CalendarView.class.getName();
    }

    static abstract class AbstractCalendarViewDelegate implements CalendarViewDelegate {
        protected static final String DEFAULT_MAX_DATE = "01/01/2100";
        protected static final String DEFAULT_MIN_DATE = "01/01/1900";
        protected Context mContext;
        protected Locale mCurrentLocale;
        protected CalendarView mDelegator;

        AbstractCalendarViewDelegate(CalendarView calendarView, Context context) {
            this.mDelegator = calendarView;
            this.mContext = context;
            setCurrentLocale(Locale.getDefault());
        }

        protected void setCurrentLocale(Locale locale) {
            if (locale.equals(this.mCurrentLocale)) {
                return;
            }
            this.mCurrentLocale = locale;
        }

        @Override
        public void setShownWeekCount(int i) {
        }

        @Override
        public int getShownWeekCount() {
            return 0;
        }

        @Override
        public void setSelectedWeekBackgroundColor(int i) {
        }

        @Override
        public int getSelectedWeekBackgroundColor() {
            return 0;
        }

        @Override
        public void setFocusedMonthDateColor(int i) {
        }

        @Override
        public int getFocusedMonthDateColor() {
            return 0;
        }

        @Override
        public void setUnfocusedMonthDateColor(int i) {
        }

        @Override
        public int getUnfocusedMonthDateColor() {
            return 0;
        }

        @Override
        public void setWeekNumberColor(int i) {
        }

        @Override
        public int getWeekNumberColor() {
            return 0;
        }

        @Override
        public void setWeekSeparatorLineColor(int i) {
        }

        @Override
        public int getWeekSeparatorLineColor() {
            return 0;
        }

        @Override
        public void setSelectedDateVerticalBar(int i) {
        }

        @Override
        public void setSelectedDateVerticalBar(Drawable drawable) {
        }

        @Override
        public Drawable getSelectedDateVerticalBar() {
            return null;
        }

        @Override
        public void setShowWeekNumber(boolean z) {
        }

        @Override
        public boolean getShowWeekNumber() {
            return false;
        }

        @Override
        public void onConfigurationChanged(Configuration configuration) {
        }
    }

    public static boolean parseDate(String str, Calendar calendar) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            calendar.setTime(DATE_FORMATTER.parse(str));
            return true;
        } catch (ParseException e) {
            Log.w(LOG_TAG, "Date: " + str + " not in format: " + DATE_FORMAT);
            return false;
        }
    }
}
