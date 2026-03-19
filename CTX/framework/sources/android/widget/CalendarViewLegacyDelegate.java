package android.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.icu.util.Calendar;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.CalendarView;
import com.android.internal.R;
import java.util.Locale;
import libcore.icu.LocaleData;

class CalendarViewLegacyDelegate extends CalendarView.AbstractCalendarViewDelegate {
    private static final int ADJUSTMENT_SCROLL_DURATION = 500;
    private static final int DAYS_PER_WEEK = 7;
    private static final int DEFAULT_DATE_TEXT_SIZE = 14;
    private static final int DEFAULT_SHOWN_WEEK_COUNT = 6;
    private static final boolean DEFAULT_SHOW_WEEK_NUMBER = true;
    private static final int DEFAULT_WEEK_DAY_TEXT_APPEARANCE_RES_ID = -1;
    private static final int GOTO_SCROLL_DURATION = 1000;
    private static final long MILLIS_IN_DAY = 86400000;
    private static final long MILLIS_IN_WEEK = 604800000;
    private static final int SCROLL_CHANGE_DELAY = 40;
    private static final int SCROLL_HYST_WEEKS = 2;
    private static final int UNSCALED_BOTTOM_BUFFER = 20;
    private static final int UNSCALED_LIST_SCROLL_TOP_OFFSET = 2;
    private static final int UNSCALED_SELECTED_DATE_VERTICAL_BAR_WIDTH = 6;
    private static final int UNSCALED_WEEK_MIN_VISIBLE_HEIGHT = 12;
    private static final int UNSCALED_WEEK_SEPARATOR_LINE_WIDTH = 1;
    private WeeksAdapter mAdapter;
    private int mBottomBuffer;
    private int mCurrentMonthDisplayed;
    private int mCurrentScrollState;
    private int mDateTextAppearanceResId;
    private int mDateTextSize;
    private ViewGroup mDayNamesHeader;
    private String[] mDayNamesLong;
    private String[] mDayNamesShort;
    private int mDaysPerWeek;
    private Calendar mFirstDayOfMonth;
    private int mFirstDayOfWeek;
    private int mFocusedMonthDateColor;
    private float mFriction;
    private boolean mIsScrollingUp;
    private int mListScrollTopOffset;
    private ListView mListView;
    private Calendar mMaxDate;
    private Calendar mMinDate;
    private TextView mMonthName;
    private CalendarView.OnDateChangeListener mOnDateChangeListener;
    private long mPreviousScrollPosition;
    private int mPreviousScrollState;
    private ScrollStateRunnable mScrollStateChangedRunnable;
    private Drawable mSelectedDateVerticalBar;
    private final int mSelectedDateVerticalBarWidth;
    private int mSelectedWeekBackgroundColor;
    private boolean mShowWeekNumber;
    private int mShownWeekCount;
    private Calendar mTempDate;
    private int mUnfocusedMonthDateColor;
    private float mVelocityScale;
    private int mWeekDayTextAppearanceResId;
    private int mWeekMinVisibleHeight;
    private int mWeekNumberColor;
    private int mWeekSeparatorLineColor;
    private final int mWeekSeparatorLineWidth;

    CalendarViewLegacyDelegate(CalendarView calendarView, Context context, AttributeSet attributeSet, int i, int i2) {
        super(calendarView, context);
        this.mListScrollTopOffset = 2;
        this.mWeekMinVisibleHeight = 12;
        this.mBottomBuffer = 20;
        this.mDaysPerWeek = 7;
        this.mFriction = 0.05f;
        this.mVelocityScale = 0.333f;
        this.mCurrentMonthDisplayed = -1;
        this.mIsScrollingUp = false;
        this.mPreviousScrollState = 0;
        this.mCurrentScrollState = 0;
        this.mScrollStateChangedRunnable = new ScrollStateRunnable();
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.CalendarView, i, i2);
        this.mShowWeekNumber = typedArrayObtainStyledAttributes.getBoolean(1, true);
        this.mFirstDayOfWeek = typedArrayObtainStyledAttributes.getInt(0, LocaleData.get(Locale.getDefault()).firstDayOfWeek.intValue());
        if (!CalendarView.parseDate(typedArrayObtainStyledAttributes.getString(2), this.mMinDate)) {
            CalendarView.parseDate("01/01/1900", this.mMinDate);
        }
        if (!CalendarView.parseDate(typedArrayObtainStyledAttributes.getString(3), this.mMaxDate)) {
            CalendarView.parseDate("01/01/2100", this.mMaxDate);
        }
        if (this.mMaxDate.before(this.mMinDate)) {
            throw new IllegalArgumentException("Max date cannot be before min date.");
        }
        this.mShownWeekCount = typedArrayObtainStyledAttributes.getInt(4, 6);
        this.mSelectedWeekBackgroundColor = typedArrayObtainStyledAttributes.getColor(5, 0);
        this.mFocusedMonthDateColor = typedArrayObtainStyledAttributes.getColor(6, 0);
        this.mUnfocusedMonthDateColor = typedArrayObtainStyledAttributes.getColor(7, 0);
        this.mWeekSeparatorLineColor = typedArrayObtainStyledAttributes.getColor(9, 0);
        this.mWeekNumberColor = typedArrayObtainStyledAttributes.getColor(8, 0);
        this.mSelectedDateVerticalBar = typedArrayObtainStyledAttributes.getDrawable(10);
        this.mDateTextAppearanceResId = typedArrayObtainStyledAttributes.getResourceId(12, 16973894);
        updateDateTextSize();
        this.mWeekDayTextAppearanceResId = typedArrayObtainStyledAttributes.getResourceId(11, -1);
        typedArrayObtainStyledAttributes.recycle();
        DisplayMetrics displayMetrics = this.mDelegator.getResources().getDisplayMetrics();
        this.mWeekMinVisibleHeight = (int) TypedValue.applyDimension(1, 12.0f, displayMetrics);
        this.mListScrollTopOffset = (int) TypedValue.applyDimension(1, 2.0f, displayMetrics);
        this.mBottomBuffer = (int) TypedValue.applyDimension(1, 20.0f, displayMetrics);
        this.mSelectedDateVerticalBarWidth = (int) TypedValue.applyDimension(1, 6.0f, displayMetrics);
        this.mWeekSeparatorLineWidth = (int) TypedValue.applyDimension(1, 1.0f, displayMetrics);
        View viewInflate = ((LayoutInflater) this.mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.calendar_view, (ViewGroup) null, false);
        this.mDelegator.addView(viewInflate);
        this.mListView = (ListView) this.mDelegator.findViewById(16908298);
        this.mDayNamesHeader = (ViewGroup) viewInflate.findViewById(R.id.day_names);
        this.mMonthName = (TextView) viewInflate.findViewById(R.id.month_name);
        setUpHeader();
        setUpListView();
        setUpAdapter();
        this.mTempDate.setTimeInMillis(System.currentTimeMillis());
        if (this.mTempDate.before(this.mMinDate)) {
            goTo(this.mMinDate, false, true, true);
        } else if (this.mMaxDate.before(this.mTempDate)) {
            goTo(this.mMaxDate, false, true, true);
        } else {
            goTo(this.mTempDate, false, true, true);
        }
        this.mDelegator.invalidate();
    }

    @Override
    public void setShownWeekCount(int i) {
        if (this.mShownWeekCount != i) {
            this.mShownWeekCount = i;
            this.mDelegator.invalidate();
        }
    }

    @Override
    public int getShownWeekCount() {
        return this.mShownWeekCount;
    }

    @Override
    public void setSelectedWeekBackgroundColor(int i) {
        if (this.mSelectedWeekBackgroundColor != i) {
            this.mSelectedWeekBackgroundColor = i;
            int childCount = this.mListView.getChildCount();
            for (int i2 = 0; i2 < childCount; i2++) {
                WeekView weekView = (WeekView) this.mListView.getChildAt(i2);
                if (weekView.mHasSelectedDay) {
                    weekView.invalidate();
                }
            }
        }
    }

    @Override
    public int getSelectedWeekBackgroundColor() {
        return this.mSelectedWeekBackgroundColor;
    }

    @Override
    public void setFocusedMonthDateColor(int i) {
        if (this.mFocusedMonthDateColor != i) {
            this.mFocusedMonthDateColor = i;
            int childCount = this.mListView.getChildCount();
            for (int i2 = 0; i2 < childCount; i2++) {
                WeekView weekView = (WeekView) this.mListView.getChildAt(i2);
                if (weekView.mHasFocusedDay) {
                    weekView.invalidate();
                }
            }
        }
    }

    @Override
    public int getFocusedMonthDateColor() {
        return this.mFocusedMonthDateColor;
    }

    @Override
    public void setUnfocusedMonthDateColor(int i) {
        if (this.mUnfocusedMonthDateColor != i) {
            this.mUnfocusedMonthDateColor = i;
            int childCount = this.mListView.getChildCount();
            for (int i2 = 0; i2 < childCount; i2++) {
                WeekView weekView = (WeekView) this.mListView.getChildAt(i2);
                if (weekView.mHasUnfocusedDay) {
                    weekView.invalidate();
                }
            }
        }
    }

    @Override
    public int getUnfocusedMonthDateColor() {
        return this.mUnfocusedMonthDateColor;
    }

    @Override
    public void setWeekNumberColor(int i) {
        if (this.mWeekNumberColor != i) {
            this.mWeekNumberColor = i;
            if (this.mShowWeekNumber) {
                invalidateAllWeekViews();
            }
        }
    }

    @Override
    public int getWeekNumberColor() {
        return this.mWeekNumberColor;
    }

    @Override
    public void setWeekSeparatorLineColor(int i) {
        if (this.mWeekSeparatorLineColor != i) {
            this.mWeekSeparatorLineColor = i;
            invalidateAllWeekViews();
        }
    }

    @Override
    public int getWeekSeparatorLineColor() {
        return this.mWeekSeparatorLineColor;
    }

    @Override
    public void setSelectedDateVerticalBar(int i) {
        setSelectedDateVerticalBar(this.mDelegator.getContext().getDrawable(i));
    }

    @Override
    public void setSelectedDateVerticalBar(Drawable drawable) {
        if (this.mSelectedDateVerticalBar != drawable) {
            this.mSelectedDateVerticalBar = drawable;
            int childCount = this.mListView.getChildCount();
            for (int i = 0; i < childCount; i++) {
                WeekView weekView = (WeekView) this.mListView.getChildAt(i);
                if (weekView.mHasSelectedDay) {
                    weekView.invalidate();
                }
            }
        }
    }

    @Override
    public Drawable getSelectedDateVerticalBar() {
        return this.mSelectedDateVerticalBar;
    }

    @Override
    public void setWeekDayTextAppearance(int i) {
        if (this.mWeekDayTextAppearanceResId != i) {
            this.mWeekDayTextAppearanceResId = i;
            setUpHeader();
        }
    }

    @Override
    public int getWeekDayTextAppearance() {
        return this.mWeekDayTextAppearanceResId;
    }

    @Override
    public void setDateTextAppearance(int i) {
        if (this.mDateTextAppearanceResId != i) {
            this.mDateTextAppearanceResId = i;
            updateDateTextSize();
            invalidateAllWeekViews();
        }
    }

    @Override
    public int getDateTextAppearance() {
        return this.mDateTextAppearanceResId;
    }

    @Override
    public void setMinDate(long j) {
        this.mTempDate.setTimeInMillis(j);
        if (isSameDate(this.mTempDate, this.mMinDate)) {
            return;
        }
        this.mMinDate.setTimeInMillis(j);
        Calendar calendar = this.mAdapter.mSelectedDate;
        if (calendar.before(this.mMinDate)) {
            this.mAdapter.setSelectedDay(this.mMinDate);
        }
        this.mAdapter.init();
        if (calendar.before(this.mMinDate)) {
            setDate(this.mTempDate.getTimeInMillis());
        } else {
            goTo(calendar, false, true, false);
        }
    }

    @Override
    public long getMinDate() {
        return this.mMinDate.getTimeInMillis();
    }

    @Override
    public void setMaxDate(long j) {
        this.mTempDate.setTimeInMillis(j);
        if (isSameDate(this.mTempDate, this.mMaxDate)) {
            return;
        }
        this.mMaxDate.setTimeInMillis(j);
        this.mAdapter.init();
        Calendar calendar = this.mAdapter.mSelectedDate;
        if (calendar.after(this.mMaxDate)) {
            setDate(this.mMaxDate.getTimeInMillis());
        } else {
            goTo(calendar, false, true, false);
        }
    }

    @Override
    public long getMaxDate() {
        return this.mMaxDate.getTimeInMillis();
    }

    @Override
    public void setShowWeekNumber(boolean z) {
        if (this.mShowWeekNumber == z) {
            return;
        }
        this.mShowWeekNumber = z;
        this.mAdapter.notifyDataSetChanged();
        setUpHeader();
    }

    @Override
    public boolean getShowWeekNumber() {
        return this.mShowWeekNumber;
    }

    @Override
    public void setFirstDayOfWeek(int i) {
        if (this.mFirstDayOfWeek == i) {
            return;
        }
        this.mFirstDayOfWeek = i;
        this.mAdapter.init();
        this.mAdapter.notifyDataSetChanged();
        setUpHeader();
    }

    @Override
    public int getFirstDayOfWeek() {
        return this.mFirstDayOfWeek;
    }

    @Override
    public void setDate(long j) {
        setDate(j, false, false);
    }

    @Override
    public void setDate(long j, boolean z, boolean z2) {
        this.mTempDate.setTimeInMillis(j);
        if (isSameDate(this.mTempDate, this.mAdapter.mSelectedDate)) {
            return;
        }
        goTo(this.mTempDate, z, true, z2);
    }

    @Override
    public long getDate() {
        return this.mAdapter.mSelectedDate.getTimeInMillis();
    }

    @Override
    public void setOnDateChangeListener(CalendarView.OnDateChangeListener onDateChangeListener) {
        this.mOnDateChangeListener = onDateChangeListener;
    }

    @Override
    public boolean getBoundsForDate(long j, Rect rect) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(j);
        int count = this.mListView.getCount();
        for (int i = 0; i < count; i++) {
            WeekView weekView = (WeekView) this.mListView.getChildAt(i);
            if (weekView.getBoundsForDate(calendar, rect)) {
                int[] iArr = new int[2];
                int[] iArr2 = new int[2];
                weekView.getLocationOnScreen(iArr);
                this.mDelegator.getLocationOnScreen(iArr2);
                int i2 = iArr[1] - iArr2[1];
                rect.top += i2;
                rect.bottom += i2;
                return true;
            }
        }
        return false;
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        setCurrentLocale(configuration.locale);
    }

    @Override
    protected void setCurrentLocale(Locale locale) {
        super.setCurrentLocale(locale);
        this.mTempDate = getCalendarForLocale(this.mTempDate, locale);
        this.mFirstDayOfMonth = getCalendarForLocale(this.mFirstDayOfMonth, locale);
        this.mMinDate = getCalendarForLocale(this.mMinDate, locale);
        this.mMaxDate = getCalendarForLocale(this.mMaxDate, locale);
    }

    private void updateDateTextSize() {
        TypedArray typedArrayObtainStyledAttributes = this.mDelegator.getContext().obtainStyledAttributes(this.mDateTextAppearanceResId, R.styleable.TextAppearance);
        this.mDateTextSize = typedArrayObtainStyledAttributes.getDimensionPixelSize(0, 14);
        typedArrayObtainStyledAttributes.recycle();
    }

    private void invalidateAllWeekViews() {
        int childCount = this.mListView.getChildCount();
        for (int i = 0; i < childCount; i++) {
            this.mListView.getChildAt(i).invalidate();
        }
    }

    private static Calendar getCalendarForLocale(Calendar calendar, Locale locale) {
        if (calendar == null) {
            return Calendar.getInstance(locale);
        }
        long timeInMillis = calendar.getTimeInMillis();
        Calendar calendar2 = Calendar.getInstance(locale);
        calendar2.setTimeInMillis(timeInMillis);
        return calendar2;
    }

    private static boolean isSameDate(Calendar calendar, Calendar calendar2) {
        return calendar.get(6) == calendar2.get(6) && calendar.get(1) == calendar2.get(1);
    }

    private void setUpAdapter() {
        if (this.mAdapter == null) {
            this.mAdapter = new WeeksAdapter(this.mContext);
            this.mAdapter.registerDataSetObserver(new DataSetObserver() {
                @Override
                public void onChanged() {
                    if (CalendarViewLegacyDelegate.this.mOnDateChangeListener != null) {
                        Calendar selectedDay = CalendarViewLegacyDelegate.this.mAdapter.getSelectedDay();
                        CalendarViewLegacyDelegate.this.mOnDateChangeListener.onSelectedDayChange(CalendarViewLegacyDelegate.this.mDelegator, selectedDay.get(1), selectedDay.get(2), selectedDay.get(5));
                    }
                }
            });
            this.mListView.setAdapter((ListAdapter) this.mAdapter);
        }
        this.mAdapter.notifyDataSetChanged();
    }

    private void setUpHeader() {
        this.mDayNamesShort = new String[this.mDaysPerWeek];
        this.mDayNamesLong = new String[this.mDaysPerWeek];
        int i = this.mFirstDayOfWeek;
        int i2 = this.mFirstDayOfWeek + this.mDaysPerWeek;
        while (i < i2) {
            int i3 = i > 7 ? i - 7 : i;
            this.mDayNamesShort[i - this.mFirstDayOfWeek] = DateUtils.getDayOfWeekString(i3, 50);
            this.mDayNamesLong[i - this.mFirstDayOfWeek] = DateUtils.getDayOfWeekString(i3, 10);
            i++;
        }
        TextView textView = (TextView) this.mDayNamesHeader.getChildAt(0);
        if (this.mShowWeekNumber) {
            textView.setVisibility(0);
        } else {
            textView.setVisibility(8);
        }
        int childCount = this.mDayNamesHeader.getChildCount();
        for (int i4 = 1; i4 < childCount; i4++) {
            TextView textView2 = (TextView) this.mDayNamesHeader.getChildAt(i4);
            if (this.mWeekDayTextAppearanceResId > -1) {
                textView2.setTextAppearance(this.mWeekDayTextAppearanceResId);
            }
            if (i4 < this.mDaysPerWeek + 1) {
                int i5 = i4 - 1;
                textView2.setText(this.mDayNamesShort[i5]);
                textView2.setContentDescription(this.mDayNamesLong[i5]);
                textView2.setVisibility(0);
            } else {
                textView2.setVisibility(8);
            }
        }
        this.mDayNamesHeader.invalidate();
    }

    private void setUpListView() {
        this.mListView.setDivider(null);
        this.mListView.setItemsCanFocus(true);
        this.mListView.setVerticalScrollBarEnabled(false);
        this.mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {
                CalendarViewLegacyDelegate.this.onScrollStateChanged(absListView, i);
            }

            @Override
            public void onScroll(AbsListView absListView, int i, int i2, int i3) {
                CalendarViewLegacyDelegate.this.onScroll(absListView, i, i2, i3);
            }
        });
        this.mListView.setFriction(this.mFriction);
        this.mListView.setVelocityScale(this.mVelocityScale);
    }

    private void goTo(Calendar calendar, boolean z, boolean z2, boolean z3) {
        int weeksSinceMinDate;
        if (calendar.before(this.mMinDate) || calendar.after(this.mMaxDate)) {
            throw new IllegalArgumentException("timeInMillis must be between the values of getMinDate() and getMaxDate()");
        }
        int firstVisiblePosition = this.mListView.getFirstVisiblePosition();
        View childAt = this.mListView.getChildAt(0);
        if (childAt != null && childAt.getTop() < 0) {
            firstVisiblePosition++;
        }
        int i = (this.mShownWeekCount + firstVisiblePosition) - 1;
        if (childAt != null && childAt.getTop() > this.mBottomBuffer) {
            i--;
        }
        if (z2) {
            this.mAdapter.setSelectedDay(calendar);
        }
        int weeksSinceMinDate2 = getWeeksSinceMinDate(calendar);
        if (weeksSinceMinDate2 < firstVisiblePosition || weeksSinceMinDate2 > i || z3) {
            this.mFirstDayOfMonth.setTimeInMillis(calendar.getTimeInMillis());
            this.mFirstDayOfMonth.set(5, 1);
            setMonthDisplayed(this.mFirstDayOfMonth);
            if (!this.mFirstDayOfMonth.before(this.mMinDate)) {
                weeksSinceMinDate = getWeeksSinceMinDate(this.mFirstDayOfMonth);
            } else {
                weeksSinceMinDate = 0;
            }
            this.mPreviousScrollState = 2;
            if (z) {
                this.mListView.smoothScrollToPositionFromTop(weeksSinceMinDate, this.mListScrollTopOffset, 1000);
                return;
            } else {
                this.mListView.setSelectionFromTop(weeksSinceMinDate, this.mListScrollTopOffset);
                onScrollStateChanged(this.mListView, 0);
                return;
            }
        }
        if (z2) {
            setMonthDisplayed(calendar);
        }
    }

    private void onScrollStateChanged(AbsListView absListView, int i) {
        this.mScrollStateChangedRunnable.doScrollStateChange(absListView, i);
    }

    private void onScroll(AbsListView absListView, int i, int i2, int i3) {
        int monthOfLastWeekDay;
        int i4 = 0;
        WeekView weekView = (WeekView) absListView.getChildAt(0);
        if (weekView == null) {
            return;
        }
        long firstVisiblePosition = (absListView.getFirstVisiblePosition() * weekView.getHeight()) - weekView.getBottom();
        int i5 = 1;
        if (firstVisiblePosition < this.mPreviousScrollPosition) {
            this.mIsScrollingUp = true;
        } else if (firstVisiblePosition > this.mPreviousScrollPosition) {
            this.mIsScrollingUp = false;
        } else {
            return;
        }
        if (weekView.getBottom() < this.mWeekMinVisibleHeight) {
            i4 = 1;
        }
        if (this.mIsScrollingUp) {
            weekView = (WeekView) absListView.getChildAt(2 + i4);
        } else if (i4 != 0) {
            weekView = (WeekView) absListView.getChildAt(i4);
        }
        if (weekView != null) {
            if (this.mIsScrollingUp) {
                monthOfLastWeekDay = weekView.getMonthOfFirstWeekDay();
            } else {
                monthOfLastWeekDay = weekView.getMonthOfLastWeekDay();
            }
            if (this.mCurrentMonthDisplayed != 11 || monthOfLastWeekDay != 0) {
                if (this.mCurrentMonthDisplayed == 0 && monthOfLastWeekDay == 11) {
                    i5 = -1;
                } else {
                    i5 = monthOfLastWeekDay - this.mCurrentMonthDisplayed;
                }
            }
            if ((!this.mIsScrollingUp && i5 > 0) || (this.mIsScrollingUp && i5 < 0)) {
                Calendar firstDay = weekView.getFirstDay();
                if (this.mIsScrollingUp) {
                    firstDay.add(5, -7);
                } else {
                    firstDay.add(5, 7);
                }
                setMonthDisplayed(firstDay);
            }
        }
        this.mPreviousScrollPosition = firstVisiblePosition;
        this.mPreviousScrollState = this.mCurrentScrollState;
    }

    private void setMonthDisplayed(Calendar calendar) {
        this.mCurrentMonthDisplayed = calendar.get(2);
        this.mAdapter.setFocusMonth(this.mCurrentMonthDisplayed);
        long timeInMillis = calendar.getTimeInMillis();
        this.mMonthName.setText(DateUtils.formatDateRange(this.mContext, timeInMillis, timeInMillis, 52));
        this.mMonthName.invalidate();
    }

    private int getWeeksSinceMinDate(Calendar calendar) {
        if (calendar.before(this.mMinDate)) {
            throw new IllegalArgumentException("fromDate: " + this.mMinDate.getTime() + " does not precede toDate: " + calendar.getTime());
        }
        return (int) ((((calendar.getTimeInMillis() + ((long) calendar.getTimeZone().getOffset(calendar.getTimeInMillis()))) - (this.mMinDate.getTimeInMillis() + ((long) this.mMinDate.getTimeZone().getOffset(this.mMinDate.getTimeInMillis())))) + (((long) (this.mMinDate.get(7) - this.mFirstDayOfWeek)) * 86400000)) / 604800000);
    }

    private class ScrollStateRunnable implements Runnable {
        private int mNewState;
        private AbsListView mView;

        private ScrollStateRunnable() {
        }

        public void doScrollStateChange(AbsListView absListView, int i) {
            this.mView = absListView;
            this.mNewState = i;
            CalendarViewLegacyDelegate.this.mDelegator.removeCallbacks(this);
            CalendarViewLegacyDelegate.this.mDelegator.postDelayed(this, 40L);
        }

        @Override
        public void run() {
            CalendarViewLegacyDelegate.this.mCurrentScrollState = this.mNewState;
            if (this.mNewState == 0 && CalendarViewLegacyDelegate.this.mPreviousScrollState != 0) {
                View childAt = this.mView.getChildAt(0);
                if (childAt != null) {
                    int bottom = childAt.getBottom() - CalendarViewLegacyDelegate.this.mListScrollTopOffset;
                    if (bottom > CalendarViewLegacyDelegate.this.mListScrollTopOffset) {
                        if (CalendarViewLegacyDelegate.this.mIsScrollingUp) {
                            this.mView.smoothScrollBy(bottom - childAt.getHeight(), 500);
                        } else {
                            this.mView.smoothScrollBy(bottom, 500);
                        }
                    }
                } else {
                    return;
                }
            }
            CalendarViewLegacyDelegate.this.mPreviousScrollState = this.mNewState;
        }
    }

    private class WeeksAdapter extends BaseAdapter implements View.OnTouchListener {
        private int mFocusedMonth;
        private GestureDetector mGestureDetector;
        private final Calendar mSelectedDate = Calendar.getInstance();
        private int mSelectedWeek;
        private int mTotalWeekCount;

        public WeeksAdapter(Context context) {
            CalendarViewLegacyDelegate.this.mContext = context;
            this.mGestureDetector = new GestureDetector(CalendarViewLegacyDelegate.this.mContext, new CalendarGestureListener());
            init();
        }

        private void init() {
            this.mSelectedWeek = CalendarViewLegacyDelegate.this.getWeeksSinceMinDate(this.mSelectedDate);
            this.mTotalWeekCount = CalendarViewLegacyDelegate.this.getWeeksSinceMinDate(CalendarViewLegacyDelegate.this.mMaxDate);
            if (CalendarViewLegacyDelegate.this.mMinDate.get(7) != CalendarViewLegacyDelegate.this.mFirstDayOfWeek || CalendarViewLegacyDelegate.this.mMaxDate.get(7) != CalendarViewLegacyDelegate.this.mFirstDayOfWeek) {
                this.mTotalWeekCount++;
            }
            notifyDataSetChanged();
        }

        public void setSelectedDay(Calendar calendar) {
            if (calendar.get(6) == this.mSelectedDate.get(6) && calendar.get(1) == this.mSelectedDate.get(1)) {
                return;
            }
            this.mSelectedDate.setTimeInMillis(calendar.getTimeInMillis());
            this.mSelectedWeek = CalendarViewLegacyDelegate.this.getWeeksSinceMinDate(this.mSelectedDate);
            this.mFocusedMonth = this.mSelectedDate.get(2);
            notifyDataSetChanged();
        }

        public Calendar getSelectedDay() {
            return this.mSelectedDate;
        }

        @Override
        public int getCount() {
            return this.mTotalWeekCount;
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
            WeekView weekView;
            if (view != null) {
                weekView = (WeekView) view;
            } else {
                weekView = CalendarViewLegacyDelegate.this.new WeekView(CalendarViewLegacyDelegate.this.mContext);
                weekView.setLayoutParams(new AbsListView.LayoutParams(-2, -2));
                weekView.setClickable(true);
                weekView.setOnTouchListener(this);
            }
            weekView.init(i, this.mSelectedWeek == i ? this.mSelectedDate.get(7) : -1, this.mFocusedMonth);
            return weekView;
        }

        public void setFocusMonth(int i) {
            if (this.mFocusedMonth == i) {
                return;
            }
            this.mFocusedMonth = i;
            notifyDataSetChanged();
        }

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (CalendarViewLegacyDelegate.this.mListView.isEnabled() && this.mGestureDetector.onTouchEvent(motionEvent)) {
                if (!((WeekView) view).getDayFromLocation(motionEvent.getX(), CalendarViewLegacyDelegate.this.mTempDate) || CalendarViewLegacyDelegate.this.mTempDate.before(CalendarViewLegacyDelegate.this.mMinDate) || CalendarViewLegacyDelegate.this.mTempDate.after(CalendarViewLegacyDelegate.this.mMaxDate)) {
                    return true;
                }
                onDateTapped(CalendarViewLegacyDelegate.this.mTempDate);
                return true;
            }
            return false;
        }

        private void onDateTapped(Calendar calendar) {
            setSelectedDay(calendar);
            CalendarViewLegacyDelegate.this.setMonthDisplayed(calendar);
        }

        class CalendarGestureListener extends GestureDetector.SimpleOnGestureListener {
            CalendarGestureListener() {
            }

            @Override
            public boolean onSingleTapUp(MotionEvent motionEvent) {
                return true;
            }
        }
    }

    private class WeekView extends View {
        private String[] mDayNumbers;
        private final Paint mDrawPaint;
        private Calendar mFirstDay;
        private boolean[] mFocusDay;
        private boolean mHasFocusedDay;
        private boolean mHasSelectedDay;
        private boolean mHasUnfocusedDay;
        private int mHeight;
        private int mLastWeekDayMonth;
        private final Paint mMonthNumDrawPaint;
        private int mMonthOfFirstWeekDay;
        private int mNumCells;
        private int mSelectedDay;
        private int mSelectedLeft;
        private int mSelectedRight;
        private final Rect mTempRect;
        private int mWeek;
        private int mWidth;

        public WeekView(Context context) {
            super(context);
            this.mTempRect = new Rect();
            this.mDrawPaint = new Paint();
            this.mMonthNumDrawPaint = new Paint();
            this.mMonthOfFirstWeekDay = -1;
            this.mLastWeekDayMonth = -1;
            this.mWeek = -1;
            this.mHasSelectedDay = false;
            this.mSelectedDay = -1;
            this.mSelectedLeft = -1;
            this.mSelectedRight = -1;
            initializePaints();
        }

        public void init(int i, int i2, int i3) {
            int i4;
            this.mSelectedDay = i2;
            this.mHasSelectedDay = this.mSelectedDay != -1;
            this.mNumCells = CalendarViewLegacyDelegate.this.mShowWeekNumber ? CalendarViewLegacyDelegate.this.mDaysPerWeek + 1 : CalendarViewLegacyDelegate.this.mDaysPerWeek;
            this.mWeek = i;
            CalendarViewLegacyDelegate.this.mTempDate.setTimeInMillis(CalendarViewLegacyDelegate.this.mMinDate.getTimeInMillis());
            CalendarViewLegacyDelegate.this.mTempDate.add(3, this.mWeek);
            CalendarViewLegacyDelegate.this.mTempDate.setFirstDayOfWeek(CalendarViewLegacyDelegate.this.mFirstDayOfWeek);
            this.mDayNumbers = new String[this.mNumCells];
            this.mFocusDay = new boolean[this.mNumCells];
            if (CalendarViewLegacyDelegate.this.mShowWeekNumber) {
                this.mDayNumbers[0] = String.format(Locale.getDefault(), "%d", Integer.valueOf(CalendarViewLegacyDelegate.this.mTempDate.get(3)));
                i4 = 1;
            } else {
                i4 = 0;
            }
            CalendarViewLegacyDelegate.this.mTempDate.add(5, CalendarViewLegacyDelegate.this.mFirstDayOfWeek - CalendarViewLegacyDelegate.this.mTempDate.get(7));
            this.mFirstDay = (Calendar) CalendarViewLegacyDelegate.this.mTempDate.clone();
            this.mMonthOfFirstWeekDay = CalendarViewLegacyDelegate.this.mTempDate.get(2);
            this.mHasUnfocusedDay = true;
            while (i4 < this.mNumCells) {
                boolean z = CalendarViewLegacyDelegate.this.mTempDate.get(2) == i3;
                this.mFocusDay[i4] = z;
                this.mHasFocusedDay |= z;
                this.mHasUnfocusedDay = (!z) & this.mHasUnfocusedDay;
                if (CalendarViewLegacyDelegate.this.mTempDate.before(CalendarViewLegacyDelegate.this.mMinDate) || CalendarViewLegacyDelegate.this.mTempDate.after(CalendarViewLegacyDelegate.this.mMaxDate)) {
                    this.mDayNumbers[i4] = "";
                } else {
                    this.mDayNumbers[i4] = String.format(Locale.getDefault(), "%d", Integer.valueOf(CalendarViewLegacyDelegate.this.mTempDate.get(5)));
                }
                CalendarViewLegacyDelegate.this.mTempDate.add(5, 1);
                i4++;
            }
            if (CalendarViewLegacyDelegate.this.mTempDate.get(5) == 1) {
                CalendarViewLegacyDelegate.this.mTempDate.add(5, -1);
            }
            this.mLastWeekDayMonth = CalendarViewLegacyDelegate.this.mTempDate.get(2);
            updateSelectionPositions();
        }

        private void initializePaints() {
            this.mDrawPaint.setFakeBoldText(false);
            this.mDrawPaint.setAntiAlias(true);
            this.mDrawPaint.setStyle(Paint.Style.FILL);
            this.mMonthNumDrawPaint.setFakeBoldText(true);
            this.mMonthNumDrawPaint.setAntiAlias(true);
            this.mMonthNumDrawPaint.setStyle(Paint.Style.FILL);
            this.mMonthNumDrawPaint.setTextAlign(Paint.Align.CENTER);
            this.mMonthNumDrawPaint.setTextSize(CalendarViewLegacyDelegate.this.mDateTextSize);
        }

        public int getMonthOfFirstWeekDay() {
            return this.mMonthOfFirstWeekDay;
        }

        public int getMonthOfLastWeekDay() {
            return this.mLastWeekDayMonth;
        }

        public Calendar getFirstDay() {
            return this.mFirstDay;
        }

        public boolean getDayFromLocation(float f, Calendar calendar) {
            int i;
            int i2;
            boolean zIsLayoutRtl = isLayoutRtl();
            if (zIsLayoutRtl) {
                i2 = CalendarViewLegacyDelegate.this.mShowWeekNumber ? this.mWidth - (this.mWidth / this.mNumCells) : this.mWidth;
                i = 0;
            } else {
                i = CalendarViewLegacyDelegate.this.mShowWeekNumber ? this.mWidth / this.mNumCells : 0;
                i2 = this.mWidth;
            }
            float f2 = i;
            if (f >= f2 && f <= i2) {
                int i3 = (int) (((f - f2) * CalendarViewLegacyDelegate.this.mDaysPerWeek) / (i2 - i));
                if (zIsLayoutRtl) {
                    i3 = (CalendarViewLegacyDelegate.this.mDaysPerWeek - 1) - i3;
                }
                calendar.setTimeInMillis(this.mFirstDay.getTimeInMillis());
                calendar.add(5, i3);
                return true;
            }
            calendar.clear();
            return false;
        }

        public boolean getBoundsForDate(Calendar calendar, Rect rect) {
            int i;
            Calendar calendar2 = Calendar.getInstance();
            calendar2.setTime(this.mFirstDay.getTime());
            int i2 = 0;
            while (i2 < CalendarViewLegacyDelegate.this.mDaysPerWeek) {
                if (calendar.get(1) == calendar2.get(1) && calendar.get(2) == calendar2.get(2) && calendar.get(5) == calendar2.get(5)) {
                    int i3 = this.mWidth / this.mNumCells;
                    if (isLayoutRtl()) {
                        if (!CalendarViewLegacyDelegate.this.mShowWeekNumber) {
                            i = (this.mNumCells - i2) - 1;
                        } else {
                            i = (this.mNumCells - i2) - 2;
                        }
                        rect.left = i * i3;
                    } else {
                        if (CalendarViewLegacyDelegate.this.mShowWeekNumber) {
                            i2++;
                        }
                        rect.left = i2 * i3;
                    }
                    rect.top = 0;
                    rect.right = rect.left + i3;
                    rect.bottom = getHeight();
                    return true;
                }
                calendar2.add(5, 1);
                i2++;
            }
            return false;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            drawBackground(canvas);
            drawWeekNumbersAndDates(canvas);
            drawWeekSeparators(canvas);
            drawSelectedDateVerticalBars(canvas);
        }

        private void drawBackground(Canvas canvas) {
            if (this.mHasSelectedDay) {
                this.mDrawPaint.setColor(CalendarViewLegacyDelegate.this.mSelectedWeekBackgroundColor);
                this.mTempRect.top = CalendarViewLegacyDelegate.this.mWeekSeparatorLineWidth;
                this.mTempRect.bottom = this.mHeight;
                boolean zIsLayoutRtl = isLayoutRtl();
                if (!zIsLayoutRtl) {
                    this.mTempRect.left = CalendarViewLegacyDelegate.this.mShowWeekNumber ? this.mWidth / this.mNumCells : 0;
                    this.mTempRect.right = this.mSelectedLeft - 2;
                } else {
                    this.mTempRect.left = 0;
                    this.mTempRect.right = this.mSelectedLeft - 2;
                }
                canvas.drawRect(this.mTempRect, this.mDrawPaint);
                if (zIsLayoutRtl) {
                    this.mTempRect.left = this.mSelectedRight + 3;
                    this.mTempRect.right = CalendarViewLegacyDelegate.this.mShowWeekNumber ? this.mWidth - (this.mWidth / this.mNumCells) : this.mWidth;
                } else {
                    this.mTempRect.left = this.mSelectedRight + 3;
                    this.mTempRect.right = this.mWidth;
                }
                canvas.drawRect(this.mTempRect, this.mDrawPaint);
            }
        }

        private void drawWeekNumbersAndDates(Canvas canvas) {
            int textSize = ((int) ((this.mHeight + this.mDrawPaint.getTextSize()) / 2.0f)) - CalendarViewLegacyDelegate.this.mWeekSeparatorLineWidth;
            int i = this.mNumCells;
            int i2 = 2 * i;
            this.mDrawPaint.setTextAlign(Paint.Align.CENTER);
            this.mDrawPaint.setTextSize(CalendarViewLegacyDelegate.this.mDateTextSize);
            int i3 = 0;
            if (!isLayoutRtl()) {
                if (CalendarViewLegacyDelegate.this.mShowWeekNumber) {
                    this.mDrawPaint.setColor(CalendarViewLegacyDelegate.this.mWeekNumberColor);
                    canvas.drawText(this.mDayNumbers[0], this.mWidth / i2, textSize, this.mDrawPaint);
                    i3 = 1;
                }
                while (i3 < i) {
                    this.mMonthNumDrawPaint.setColor(this.mFocusDay[i3] ? CalendarViewLegacyDelegate.this.mFocusedMonthDateColor : CalendarViewLegacyDelegate.this.mUnfocusedMonthDateColor);
                    canvas.drawText(this.mDayNumbers[i3], (((2 * i3) + 1) * this.mWidth) / i2, textSize, this.mMonthNumDrawPaint);
                    i3++;
                }
                return;
            }
            int i4 = 0;
            while (true) {
                int i5 = i - 1;
                if (i4 >= i5) {
                    break;
                }
                this.mMonthNumDrawPaint.setColor(this.mFocusDay[i4] ? CalendarViewLegacyDelegate.this.mFocusedMonthDateColor : CalendarViewLegacyDelegate.this.mUnfocusedMonthDateColor);
                canvas.drawText(this.mDayNumbers[i5 - i4], (((2 * i4) + 1) * this.mWidth) / i2, textSize, this.mMonthNumDrawPaint);
                i4++;
            }
            if (CalendarViewLegacyDelegate.this.mShowWeekNumber) {
                this.mDrawPaint.setColor(CalendarViewLegacyDelegate.this.mWeekNumberColor);
                canvas.drawText(this.mDayNumbers[0], this.mWidth - (this.mWidth / i2), textSize, this.mDrawPaint);
            }
        }

        private void drawWeekSeparators(Canvas canvas) {
            float f;
            int firstVisiblePosition = CalendarViewLegacyDelegate.this.mListView.getFirstVisiblePosition();
            if (CalendarViewLegacyDelegate.this.mListView.getChildAt(0).getTop() < 0) {
                firstVisiblePosition++;
            }
            if (firstVisiblePosition != this.mWeek) {
                this.mDrawPaint.setColor(CalendarViewLegacyDelegate.this.mWeekSeparatorLineColor);
                this.mDrawPaint.setStrokeWidth(CalendarViewLegacyDelegate.this.mWeekSeparatorLineWidth);
                if (isLayoutRtl()) {
                    f = CalendarViewLegacyDelegate.this.mShowWeekNumber ? this.mWidth - (this.mWidth / this.mNumCells) : this.mWidth;
                } else {
                    f = CalendarViewLegacyDelegate.this.mShowWeekNumber ? this.mWidth / this.mNumCells : 0.0f;
                    f = this.mWidth;
                }
                canvas.drawLine(f, 0.0f, f, 0.0f, this.mDrawPaint);
            }
        }

        private void drawSelectedDateVerticalBars(Canvas canvas) {
            if (!this.mHasSelectedDay) {
                return;
            }
            CalendarViewLegacyDelegate.this.mSelectedDateVerticalBar.setBounds(this.mSelectedLeft - (CalendarViewLegacyDelegate.this.mSelectedDateVerticalBarWidth / 2), CalendarViewLegacyDelegate.this.mWeekSeparatorLineWidth, this.mSelectedLeft + (CalendarViewLegacyDelegate.this.mSelectedDateVerticalBarWidth / 2), this.mHeight);
            CalendarViewLegacyDelegate.this.mSelectedDateVerticalBar.draw(canvas);
            CalendarViewLegacyDelegate.this.mSelectedDateVerticalBar.setBounds(this.mSelectedRight - (CalendarViewLegacyDelegate.this.mSelectedDateVerticalBarWidth / 2), CalendarViewLegacyDelegate.this.mWeekSeparatorLineWidth, this.mSelectedRight + (CalendarViewLegacyDelegate.this.mSelectedDateVerticalBarWidth / 2), this.mHeight);
            CalendarViewLegacyDelegate.this.mSelectedDateVerticalBar.draw(canvas);
        }

        @Override
        protected void onSizeChanged(int i, int i2, int i3, int i4) {
            this.mWidth = i;
            updateSelectionPositions();
        }

        private void updateSelectionPositions() {
            if (this.mHasSelectedDay) {
                boolean zIsLayoutRtl = isLayoutRtl();
                int i = this.mSelectedDay - CalendarViewLegacyDelegate.this.mFirstDayOfWeek;
                if (i < 0) {
                    i += 7;
                }
                if (CalendarViewLegacyDelegate.this.mShowWeekNumber && !zIsLayoutRtl) {
                    i++;
                }
                if (zIsLayoutRtl) {
                    this.mSelectedLeft = (((CalendarViewLegacyDelegate.this.mDaysPerWeek - 1) - i) * this.mWidth) / this.mNumCells;
                } else {
                    this.mSelectedLeft = (i * this.mWidth) / this.mNumCells;
                }
                this.mSelectedRight = this.mSelectedLeft + (this.mWidth / this.mNumCells);
            }
        }

        @Override
        protected void onMeasure(int i, int i2) {
            this.mHeight = ((CalendarViewLegacyDelegate.this.mListView.getHeight() - CalendarViewLegacyDelegate.this.mListView.getPaddingTop()) - CalendarViewLegacyDelegate.this.mListView.getPaddingBottom()) / CalendarViewLegacyDelegate.this.mShownWeekCount;
            setMeasuredDimension(View.MeasureSpec.getSize(i), this.mHeight);
        }
    }
}
