package android.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.util.MathUtils;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.AbsListView;
import android.widget.SimpleMonthAdapter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

class DayPickerView extends ListView implements AbsListView.OnScrollListener {
    private static final int GOTO_SCROLL_DURATION = 250;
    private static final int LIST_TOP_OFFSET = -1;
    private static final int SCROLL_CHANGE_DELAY = 40;
    private static final String TAG = "DayPickerView";
    private final SimpleMonthAdapter mAdapter;
    private int mCurrentMonthDisplayed;
    private int mCurrentScrollState;
    private Calendar mMaxDate;
    private Calendar mMinDate;
    private OnDaySelectedListener mOnDaySelectedListener;
    private boolean mPerformingScroll;
    private int mPreviousScrollState;
    private final SimpleMonthAdapter.OnDaySelectedListener mProxyOnDaySelectedListener;
    private final ScrollStateRunnable mScrollStateChangedRunnable;
    private Calendar mSelectedDay;
    private Calendar mTempCalendar;
    private Calendar mTempDay;
    private SimpleDateFormat mYearFormat;

    public interface OnDaySelectedListener {
        void onDaySelected(DayPickerView dayPickerView, Calendar calendar);
    }

    public DayPickerView(Context context) {
        super(context);
        this.mAdapter = new SimpleMonthAdapter(getContext());
        this.mScrollStateChangedRunnable = new ScrollStateRunnable(this);
        this.mYearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
        this.mSelectedDay = Calendar.getInstance();
        this.mTempDay = Calendar.getInstance();
        this.mMinDate = Calendar.getInstance();
        this.mMaxDate = Calendar.getInstance();
        this.mPreviousScrollState = 0;
        this.mCurrentScrollState = 0;
        this.mProxyOnDaySelectedListener = new SimpleMonthAdapter.OnDaySelectedListener() {
            @Override
            public void onDaySelected(SimpleMonthAdapter adapter, Calendar day) {
                if (DayPickerView.this.mOnDaySelectedListener != null) {
                    DayPickerView.this.mOnDaySelectedListener.onDaySelected(DayPickerView.this, day);
                }
            }
        };
        setAdapter((ListAdapter) this.mAdapter);
        setLayoutParams(new AbsListView.LayoutParams(-1, -1));
        setDrawSelectorOnTop(false);
        setUpListView();
        goTo(this.mSelectedDay.getTimeInMillis(), false, false, true);
        this.mAdapter.setOnDaySelectedListener(this.mProxyOnDaySelectedListener);
    }

    public void setDate(long timeInMillis) {
        setDate(timeInMillis, false, true);
    }

    public void setDate(long timeInMillis, boolean animate, boolean forceScroll) {
        goTo(timeInMillis, animate, true, forceScroll);
    }

    public long getDate() {
        return this.mSelectedDay.getTimeInMillis();
    }

    public void setFirstDayOfWeek(int firstDayOfWeek) {
        this.mAdapter.setFirstDayOfWeek(firstDayOfWeek);
    }

    public int getFirstDayOfWeek() {
        return this.mAdapter.getFirstDayOfWeek();
    }

    public void setMinDate(long timeInMillis) {
        this.mMinDate.setTimeInMillis(timeInMillis);
        onRangeChanged();
    }

    public long getMinDate() {
        return this.mMinDate.getTimeInMillis();
    }

    public void setMaxDate(long timeInMillis) {
        this.mMaxDate.setTimeInMillis(timeInMillis);
        onRangeChanged();
    }

    public long getMaxDate() {
        return this.mMaxDate.getTimeInMillis();
    }

    public void onRangeChanged() {
        this.mAdapter.setRange(this.mMinDate, this.mMaxDate);
        goTo(this.mSelectedDay.getTimeInMillis(), false, false, true);
    }

    public void setOnDaySelectedListener(OnDaySelectedListener listener) {
        this.mOnDaySelectedListener = listener;
    }

    private void setUpListView() {
        setCacheColorHint(0);
        setDivider(null);
        setItemsCanFocus(true);
        setFastScrollEnabled(false);
        setVerticalScrollBarEnabled(false);
        setOnScrollListener(this);
        setFadingEdgeLength(0);
        setFriction(ViewConfiguration.getScrollFriction());
    }

    private int getDiffMonths(Calendar start, Calendar end) {
        int diffYears = end.get(1) - start.get(1);
        int diffMonths = (end.get(2) - start.get(2)) + (diffYears * 12);
        return diffMonths;
    }

    private int getPositionFromDay(long timeInMillis) {
        int diffMonthMax = getDiffMonths(this.mMinDate, this.mMaxDate);
        int diffMonth = getDiffMonths(this.mMinDate, getTempCalendarForTime(timeInMillis));
        return MathUtils.constrain(diffMonth, 0, diffMonthMax);
    }

    private Calendar getTempCalendarForTime(long timeInMillis) {
        if (this.mTempCalendar == null) {
            this.mTempCalendar = Calendar.getInstance();
        }
        this.mTempCalendar.setTimeInMillis(timeInMillis);
        return this.mTempCalendar;
    }

    private boolean goTo(long day, boolean animate, boolean setSelected, boolean forceScroll) {
        View child;
        int selectedPosition;
        if (setSelected) {
            this.mSelectedDay.setTimeInMillis(day);
        }
        this.mTempDay.setTimeInMillis(day);
        int position = getPositionFromDay(day);
        int i = 0;
        while (true) {
            int i2 = i + 1;
            child = getChildAt(i);
            if (child != null) {
                int top = child.getTop();
                if (top >= 0) {
                    break;
                }
                i = i2;
            } else {
                break;
            }
        }
        if (child != null) {
            selectedPosition = getPositionForView(child);
        } else {
            selectedPosition = 0;
        }
        if (setSelected) {
            this.mAdapter.setSelectedDay(this.mSelectedDay);
        }
        if (position != selectedPosition || forceScroll) {
            setMonthDisplayed(this.mTempDay);
            this.mPreviousScrollState = 2;
            if (animate) {
                smoothScrollToPositionFromTop(position, -1, 250);
                return true;
            }
            postSetSelection(position);
        } else if (setSelected) {
            setMonthDisplayed(this.mSelectedDay);
        }
        return false;
    }

    public void postSetSelection(final int position) {
        clearFocus();
        post(new Runnable() {
            @Override
            public void run() {
                DayPickerView.this.setSelection(position);
            }
        });
        onScrollStateChanged(this, 0);
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        SimpleMonthView child = (SimpleMonthView) view.getChildAt(0);
        if (child != null) {
            this.mPreviousScrollState = this.mCurrentScrollState;
        }
    }

    protected void setMonthDisplayed(Calendar date) {
        if (this.mCurrentMonthDisplayed != date.get(2)) {
            this.mCurrentMonthDisplayed = date.get(2);
            invalidateViews();
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        this.mScrollStateChangedRunnable.doScrollStateChange(view, scrollState);
    }

    void setCalendarTextColor(ColorStateList colors) {
        this.mAdapter.setCalendarTextColor(colors);
    }

    void setCalendarTextAppearance(int resId) {
        this.mAdapter.setCalendarTextAppearance(resId);
    }

    protected class ScrollStateRunnable implements Runnable {
        private int mNewState;
        private View mParent;

        ScrollStateRunnable(View view) {
            this.mParent = view;
        }

        public void doScrollStateChange(AbsListView view, int scrollState) {
            this.mParent.removeCallbacks(this);
            this.mNewState = scrollState;
            this.mParent.postDelayed(this, 40L);
        }

        @Override
        public void run() {
            DayPickerView.this.mCurrentScrollState = this.mNewState;
            if (Log.isLoggable(DayPickerView.TAG, 3)) {
                Log.d(DayPickerView.TAG, "new scroll state: " + this.mNewState + " old state: " + DayPickerView.this.mPreviousScrollState);
            }
            if (this.mNewState != 0 || DayPickerView.this.mPreviousScrollState == 0 || DayPickerView.this.mPreviousScrollState == 1) {
                DayPickerView.this.mPreviousScrollState = this.mNewState;
                return;
            }
            DayPickerView.this.mPreviousScrollState = this.mNewState;
            int i = 0;
            View child = DayPickerView.this.getChildAt(0);
            while (child != null && child.getBottom() <= 0) {
                i++;
                child = DayPickerView.this.getChildAt(i);
            }
            if (child != null) {
                int firstPosition = DayPickerView.this.getFirstVisiblePosition();
                int lastPosition = DayPickerView.this.getLastVisiblePosition();
                boolean scroll = (firstPosition == 0 || lastPosition == DayPickerView.this.getCount() + (-1)) ? false : true;
                int top = child.getTop();
                int bottom = child.getBottom();
                int midpoint = DayPickerView.this.getHeight() / 2;
                if (scroll && top < -1) {
                    if (bottom > midpoint) {
                        DayPickerView.this.smoothScrollBy(top, 250);
                    } else {
                        DayPickerView.this.smoothScrollBy(bottom, 250);
                    }
                }
            }
        }
    }

    public int getMostVisiblePosition() {
        int firstPosition = getFirstVisiblePosition();
        int height = getHeight();
        int maxDisplayedHeight = 0;
        int mostVisibleIndex = 0;
        int i = 0;
        int bottom = 0;
        while (bottom < height) {
            View child = getChildAt(i);
            if (child == null) {
                break;
            }
            bottom = child.getBottom();
            int displayedHeight = Math.min(bottom, height) - Math.max(0, child.getTop());
            if (displayedHeight > maxDisplayedHeight) {
                mostVisibleIndex = i;
                maxDisplayedHeight = displayedHeight;
            }
            i++;
        }
        return firstPosition + mostVisibleIndex;
    }

    private Calendar findAccessibilityFocus() {
        Calendar focus;
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if ((child instanceof SimpleMonthView) && (focus = ((SimpleMonthView) child).getAccessibilityFocus()) != null) {
                return focus;
            }
        }
        return null;
    }

    private boolean restoreAccessibilityFocus(Calendar day) {
        if (day == null) {
            return false;
        }
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if ((child instanceof SimpleMonthView) && ((SimpleMonthView) child).restoreAccessibilityFocus(day)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void layoutChildren() {
        Calendar focusedDay = findAccessibilityFocus();
        super.layoutChildren();
        if (this.mPerformingScroll) {
            this.mPerformingScroll = false;
        } else {
            restoreAccessibilityFocus(focusedDay);
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        this.mYearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setItemCount(-1);
    }

    private String getMonthAndYearString(Calendar day) {
        return day.getDisplayName(2, 2, Locale.getDefault()) + " " + this.mYearFormat.format(day.getTime());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);
    }

    @Override
    public boolean performAccessibilityAction(int action, Bundle arguments) {
        View firstVisibleView;
        if (action != 4096 && action != 8192) {
            return super.performAccessibilityAction(action, arguments);
        }
        int firstVisiblePosition = getFirstVisiblePosition();
        int month = firstVisiblePosition % 12;
        int year = (firstVisiblePosition / 12) + this.mMinDate.get(1);
        Calendar day = Calendar.getInstance();
        day.set(year, month, 1);
        if (action == 4096) {
            day.add(2, 1);
            if (day.get(2) == 12) {
                day.set(2, 0);
                day.add(1, 1);
            }
        } else if (action == 8192 && (firstVisibleView = getChildAt(0)) != null && firstVisibleView.getTop() >= -1) {
            day.add(2, -1);
            if (day.get(2) == -1) {
                day.set(2, 11);
                day.add(1, -1);
            }
        }
        announceForAccessibility(getMonthAndYearString(day));
        goTo(day.getTimeInMillis(), true, false, true);
        this.mPerformingScroll = true;
        return true;
    }
}
