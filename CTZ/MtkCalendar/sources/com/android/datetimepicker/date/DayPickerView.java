package com.android.datetimepicker.date;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.AbsListView;
import android.widget.ListAdapter;
import android.widget.ListView;
import com.android.datetimepicker.Utils;
import com.android.datetimepicker.date.DatePickerDialog;
import com.android.datetimepicker.date.MonthAdapter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public abstract class DayPickerView extends ListView implements AbsListView.OnScrollListener, DatePickerDialog.OnDateChangedListener {
    public static int LIST_TOP_OFFSET = -1;
    private static SimpleDateFormat YEAR_FORMAT = new SimpleDateFormat("yyyy", Locale.getDefault());
    protected MonthAdapter mAdapter;
    protected Context mContext;
    private DatePickerController mController;
    protected int mCurrentMonthDisplayed;
    protected int mCurrentScrollState;
    protected int mDaysPerWeek;
    protected float mFriction;
    protected Handler mHandler;
    protected int mNumWeeks;
    private boolean mPerformingScroll;
    protected long mPreviousScrollPosition;
    protected int mPreviousScrollState;
    protected ScrollStateRunnable mScrollStateChangedRunnable;
    protected MonthAdapter.CalendarDay mSelectedDay;
    protected boolean mShowWeekNumber;
    protected MonthAdapter.CalendarDay mTempDay;

    public abstract MonthAdapter createMonthAdapter(Context context, DatePickerController datePickerController);

    public DayPickerView(Context context, DatePickerController datePickerController) {
        super(context);
        this.mNumWeeks = 6;
        this.mShowWeekNumber = false;
        this.mDaysPerWeek = 7;
        this.mFriction = 1.0f;
        this.mSelectedDay = new MonthAdapter.CalendarDay();
        this.mTempDay = new MonthAdapter.CalendarDay();
        this.mPreviousScrollState = 0;
        this.mCurrentScrollState = 0;
        this.mScrollStateChangedRunnable = new ScrollStateRunnable();
        init(context);
        setController(datePickerController);
        setFocusable(false);
    }

    public void setController(DatePickerController datePickerController) {
        this.mController = datePickerController;
        this.mController.registerOnDateChangedListener(this);
        refreshAdapter();
        onDateChanged();
    }

    public void init(Context context) {
        this.mHandler = new Handler();
        setLayoutParams(new AbsListView.LayoutParams(-1, -1));
        setDrawSelectorOnTop(false);
        this.mContext = context;
        setUpListView();
    }

    public void onChange() {
        refreshAdapter();
    }

    protected void refreshAdapter() {
        if (this.mAdapter == null) {
            this.mAdapter = createMonthAdapter(getContext(), this.mController);
        } else {
            this.mAdapter.setSelectedDay(this.mSelectedDay);
        }
        setAdapter((ListAdapter) this.mAdapter);
    }

    protected void setUpListView() {
        setCacheColorHint(0);
        setDivider(null);
        setItemsCanFocus(true);
        setFastScrollEnabled(false);
        setVerticalScrollBarEnabled(false);
        setOnScrollListener(this);
        setFadingEdgeLength(0);
        setFriction(ViewConfiguration.getScrollFriction() * this.mFriction);
    }

    public boolean goTo(MonthAdapter.CalendarDay calendarDay, boolean z, boolean z2, boolean z3) {
        View childAt;
        int positionForView;
        if (z2) {
            this.mSelectedDay.set(calendarDay);
        }
        this.mTempDay.set(calendarDay);
        int minYear = ((calendarDay.year - this.mController.getMinYear()) * 12) + calendarDay.month;
        int i = 0;
        while (true) {
            int i2 = i + 1;
            childAt = getChildAt(i);
            if (childAt == null) {
                break;
            }
            int top = childAt.getTop();
            if (Log.isLoggable("MonthFragment", 3)) {
                StringBuilder sb = new StringBuilder();
                sb.append("child at ");
                sb.append(i2 - 1);
                sb.append(" has top ");
                sb.append(top);
                Log.d("MonthFragment", sb.toString());
            }
            if (top >= 0) {
                break;
            }
            i = i2;
        }
        if (childAt != null) {
            positionForView = getPositionForView(childAt);
        } else {
            positionForView = 0;
        }
        if (z2) {
            this.mAdapter.setSelectedDay(this.mSelectedDay);
        }
        if (Log.isLoggable("MonthFragment", 3)) {
            Log.d("MonthFragment", "GoTo position " + minYear);
        }
        if (minYear != positionForView || z3) {
            setMonthDisplayed(this.mTempDay);
            this.mPreviousScrollState = 2;
            if (z) {
                smoothScrollToPositionFromTop(minYear, LIST_TOP_OFFSET, 250);
                return true;
            }
            postSetSelection(minYear);
        } else if (z2) {
            setMonthDisplayed(this.mSelectedDay);
        }
        return false;
    }

    public void postSetSelection(final int i) {
        clearFocus();
        post(new Runnable() {
            @Override
            public void run() {
                DayPickerView.this.setSelection(i);
            }
        });
        onScrollStateChanged(this, 0);
    }

    @Override
    public void onScroll(AbsListView absListView, int i, int i2, int i3) {
        MonthView monthView = (MonthView) absListView.getChildAt(0);
        if (monthView == null) {
            return;
        }
        this.mPreviousScrollPosition = (((long) absListView.getFirstVisiblePosition()) * ((long) monthView.getHeight())) - ((long) monthView.getBottom());
        this.mPreviousScrollState = this.mCurrentScrollState;
    }

    protected void setMonthDisplayed(MonthAdapter.CalendarDay calendarDay) {
        this.mCurrentMonthDisplayed = calendarDay.month;
        invalidateViews();
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int i) {
        this.mScrollStateChangedRunnable.doScrollStateChange(absListView, i);
    }

    protected class ScrollStateRunnable implements Runnable {
        private int mNewState;

        protected ScrollStateRunnable() {
        }

        public void doScrollStateChange(AbsListView absListView, int i) {
            DayPickerView.this.mHandler.removeCallbacks(this);
            this.mNewState = i;
            DayPickerView.this.mHandler.postDelayed(this, 40L);
        }

        @Override
        public void run() {
            DayPickerView.this.mCurrentScrollState = this.mNewState;
            if (Log.isLoggable("MonthFragment", 3)) {
                Log.d("MonthFragment", "new scroll state: " + this.mNewState + " old state: " + DayPickerView.this.mPreviousScrollState);
            }
            if (this.mNewState == 0 && DayPickerView.this.mPreviousScrollState != 0) {
                if (DayPickerView.this.mPreviousScrollState != 1) {
                    DayPickerView.this.mPreviousScrollState = this.mNewState;
                    View childAt = DayPickerView.this.getChildAt(0);
                    int i = 0;
                    while (childAt != null && childAt.getBottom() <= 0) {
                        i++;
                        childAt = DayPickerView.this.getChildAt(i);
                    }
                    if (childAt == null) {
                        return;
                    }
                    boolean z = (DayPickerView.this.getFirstVisiblePosition() == 0 || DayPickerView.this.getLastVisiblePosition() == DayPickerView.this.getCount() - 1) ? false : true;
                    int top = childAt.getTop();
                    int bottom = childAt.getBottom();
                    int height = DayPickerView.this.getHeight() / 2;
                    if (!z || top >= DayPickerView.LIST_TOP_OFFSET) {
                        return;
                    }
                    if (bottom > height) {
                        DayPickerView.this.smoothScrollBy(top, 250);
                        return;
                    } else {
                        DayPickerView.this.smoothScrollBy(bottom, 250);
                        return;
                    }
                }
            }
            DayPickerView.this.mPreviousScrollState = this.mNewState;
        }
    }

    public int getMostVisiblePosition() {
        int firstVisiblePosition = getFirstVisiblePosition();
        int height = getHeight();
        int i = 0;
        int i2 = 0;
        int i3 = 0;
        int i4 = 0;
        while (i < height) {
            View childAt = getChildAt(i2);
            if (childAt == null) {
                break;
            }
            int bottom = childAt.getBottom();
            int iMin = Math.min(bottom, height) - Math.max(0, childAt.getTop());
            if (iMin > i4) {
                i3 = i2;
                i4 = iMin;
            }
            i2++;
            i = bottom;
        }
        return firstVisiblePosition + i3;
    }

    @Override
    public void onDateChanged() {
        goTo(this.mController.getSelectedDay(), false, true, true);
    }

    private MonthAdapter.CalendarDay findAccessibilityFocus() {
        MonthAdapter.CalendarDay accessibilityFocus;
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            ?? childAt = getChildAt(i);
            if ((childAt instanceof MonthView) && (accessibilityFocus = childAt.getAccessibilityFocus()) != null) {
                if (Build.VERSION.SDK_INT == 17) {
                    childAt.clearAccessibilityFocus();
                }
                return accessibilityFocus;
            }
        }
        return null;
    }

    private boolean restoreAccessibilityFocus(MonthAdapter.CalendarDay calendarDay) {
        if (calendarDay == null) {
            return false;
        }
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            ?? childAt = getChildAt(i);
            if ((childAt instanceof MonthView) && childAt.restoreAccessibilityFocus(calendarDay)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void layoutChildren() {
        MonthAdapter.CalendarDay calendarDayFindAccessibilityFocus = findAccessibilityFocus();
        super.layoutChildren();
        if (this.mPerformingScroll) {
            this.mPerformingScroll = false;
        } else {
            restoreAccessibilityFocus(calendarDayFindAccessibilityFocus);
        }
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        super.onInitializeAccessibilityEvent(accessibilityEvent);
        accessibilityEvent.setItemCount(-1);
    }

    private static String getMonthAndYearString(MonthAdapter.CalendarDay calendarDay) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(calendarDay.year, calendarDay.month, calendarDay.day);
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(calendar.getDisplayName(2, 2, Locale.getDefault()));
        stringBuffer.append(" ");
        stringBuffer.append(YEAR_FORMAT.format(calendar.getTime()));
        return stringBuffer.toString();
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfo);
        accessibilityNodeInfo.addAction(4096);
        accessibilityNodeInfo.addAction(8192);
    }

    @Override
    @SuppressLint({"NewApi"})
    public boolean performAccessibilityAction(int i, Bundle bundle) {
        View childAt;
        if (i != 4096 && i != 8192) {
            return super.performAccessibilityAction(i, bundle);
        }
        int firstVisiblePosition = getFirstVisiblePosition();
        MonthAdapter.CalendarDay calendarDay = new MonthAdapter.CalendarDay((firstVisiblePosition / 12) + this.mController.getMinYear(), firstVisiblePosition % 12, 1);
        if (i == 4096) {
            calendarDay.month++;
            if (calendarDay.month == 12) {
                calendarDay.month = 0;
                calendarDay.year++;
            }
        } else if (i == 8192 && (childAt = getChildAt(0)) != null && childAt.getTop() >= -1) {
            calendarDay.month--;
            if (calendarDay.month == -1) {
                calendarDay.month = 11;
                calendarDay.year--;
            }
        }
        Utils.tryAccessibilityAnnounce(this, getMonthAndYearString(calendarDay));
        goTo(calendarDay, true, false, true);
        this.mPerformingScroll = true;
        return true;
    }
}
