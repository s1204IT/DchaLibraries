package com.android.datetimepicker.date;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.widget.ExploreByTouchHelper;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import com.android.datetimepicker.R;
import com.android.datetimepicker.Utils;
import com.android.datetimepicker.date.MonthAdapter;
import java.security.InvalidParameterException;
import java.util.Calendar;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public abstract class MonthView extends View {
    protected static int DAY_SELECTED_CIRCLE_SIZE;
    protected static int MINI_DAY_NUMBER_TEXT_SIZE;
    protected static int MONTH_DAY_LABEL_TEXT_SIZE;
    protected static int MONTH_HEADER_SIZE;
    protected static int MONTH_LABEL_TEXT_SIZE;
    private final Calendar mCalendar;
    protected DatePickerController mController;
    protected final Calendar mDayLabelCalendar;
    private int mDayOfWeekStart;
    private String mDayOfWeekTypeface;
    protected int mDayTextColor;
    protected int mDisabledDayTextColor;
    protected int mEdgePadding;
    protected int mFirstJulianDay;
    protected int mFirstMonth;
    private final Formatter mFormatter;
    protected boolean mHasToday;
    protected int mLastMonth;
    private boolean mLockAccessibilityDelegate;
    protected int mMonth;
    protected Paint mMonthDayLabelPaint;
    protected Paint mMonthNumPaint;
    protected int mMonthTitleBGColor;
    protected Paint mMonthTitleBGPaint;
    protected int mMonthTitleColor;
    protected Paint mMonthTitlePaint;
    private String mMonthTitleTypeface;
    protected int mNumCells;
    protected int mNumDays;
    protected int mNumRows;
    protected OnDayClickListener mOnDayClickListener;
    protected int mRowHeight;
    protected Paint mSelectedCirclePaint;
    protected int mSelectedDay;
    protected int mSelectedLeft;
    protected int mSelectedRight;
    private final StringBuilder mStringBuilder;
    protected int mToday;
    protected int mTodayNumberColor;
    private final MonthViewTouchHelper mTouchHelper;
    protected int mWeekStart;
    protected int mWidth;
    protected int mYear;
    protected static int DEFAULT_HEIGHT = 32;
    protected static int MIN_HEIGHT = 10;
    protected static int DAY_SEPARATOR_WIDTH = 1;
    protected static float mScale = 0.0f;

    public interface OnDayClickListener {
        void onDayClick(MonthView monthView, MonthAdapter.CalendarDay calendarDay);
    }

    public abstract void drawMonthDay(Canvas canvas, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9);

    public MonthView(Context context) {
        this(context, null);
    }

    public MonthView(Context context, AttributeSet attr) {
        super(context, attr);
        this.mEdgePadding = 0;
        this.mFirstJulianDay = -1;
        this.mFirstMonth = -1;
        this.mLastMonth = -1;
        this.mRowHeight = DEFAULT_HEIGHT;
        this.mHasToday = false;
        this.mSelectedDay = -1;
        this.mToday = -1;
        this.mWeekStart = 1;
        this.mNumDays = 7;
        this.mNumCells = this.mNumDays;
        this.mSelectedLeft = -1;
        this.mSelectedRight = -1;
        this.mNumRows = 6;
        this.mDayOfWeekStart = 0;
        Resources res = context.getResources();
        this.mDayLabelCalendar = Calendar.getInstance();
        this.mCalendar = Calendar.getInstance();
        this.mDayOfWeekTypeface = res.getString(R.string.day_of_week_label_typeface);
        this.mMonthTitleTypeface = res.getString(R.string.sans_serif);
        this.mDayTextColor = res.getColor(R.color.date_picker_text_normal);
        this.mTodayNumberColor = res.getColor(R.color.blue);
        this.mDisabledDayTextColor = res.getColor(R.color.date_picker_text_disabled);
        this.mMonthTitleColor = res.getColor(R.color.white);
        this.mMonthTitleBGColor = res.getColor(R.color.circle_background);
        this.mStringBuilder = new StringBuilder(50);
        this.mFormatter = new Formatter(this.mStringBuilder, Locale.getDefault());
        MINI_DAY_NUMBER_TEXT_SIZE = res.getDimensionPixelSize(R.dimen.day_number_size);
        MONTH_LABEL_TEXT_SIZE = res.getDimensionPixelSize(R.dimen.month_label_size);
        MONTH_DAY_LABEL_TEXT_SIZE = res.getDimensionPixelSize(R.dimen.month_day_label_text_size);
        MONTH_HEADER_SIZE = res.getDimensionPixelOffset(R.dimen.month_list_item_header_height);
        DAY_SELECTED_CIRCLE_SIZE = res.getDimensionPixelSize(R.dimen.day_number_select_circle_radius);
        this.mRowHeight = (res.getDimensionPixelOffset(R.dimen.date_picker_view_animator_height) - getMonthHeaderSize()) / 6;
        this.mTouchHelper = getMonthViewTouchHelper();
        ViewCompat.setAccessibilityDelegate(this, this.mTouchHelper);
        ViewCompat.setImportantForAccessibility(this, 1);
        this.mLockAccessibilityDelegate = true;
        initView();
    }

    public void setDatePickerController(DatePickerController controller) {
        this.mController = controller;
    }

    protected MonthViewTouchHelper getMonthViewTouchHelper() {
        return new MonthViewTouchHelper(this);
    }

    @Override
    public void setAccessibilityDelegate(View.AccessibilityDelegate delegate) {
        if (!this.mLockAccessibilityDelegate) {
            super.setAccessibilityDelegate(delegate);
        }
    }

    public void setOnDayClickListener(OnDayClickListener listener) {
        this.mOnDayClickListener = listener;
    }

    @Override
    public boolean dispatchHoverEvent(MotionEvent event) {
        if (this.mTouchHelper.dispatchHoverEvent(event)) {
            return true;
        }
        return super.dispatchHoverEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case 1:
                int day = getDayFromLocation(event.getX(), event.getY());
                if (day >= 0) {
                    onDayClick(day);
                }
                break;
        }
        return true;
    }

    protected void initView() {
        this.mMonthTitlePaint = new Paint();
        this.mMonthTitlePaint.setFakeBoldText(true);
        this.mMonthTitlePaint.setAntiAlias(true);
        this.mMonthTitlePaint.setTextSize(MONTH_LABEL_TEXT_SIZE);
        this.mMonthTitlePaint.setTypeface(Typeface.create(this.mMonthTitleTypeface, 1));
        this.mMonthTitlePaint.setColor(this.mDayTextColor);
        this.mMonthTitlePaint.setTextAlign(Paint.Align.CENTER);
        this.mMonthTitlePaint.setStyle(Paint.Style.FILL);
        this.mMonthTitleBGPaint = new Paint();
        this.mMonthTitleBGPaint.setFakeBoldText(true);
        this.mMonthTitleBGPaint.setAntiAlias(true);
        this.mMonthTitleBGPaint.setColor(this.mMonthTitleBGColor);
        this.mMonthTitleBGPaint.setTextAlign(Paint.Align.CENTER);
        this.mMonthTitleBGPaint.setStyle(Paint.Style.FILL);
        this.mSelectedCirclePaint = new Paint();
        this.mSelectedCirclePaint.setFakeBoldText(true);
        this.mSelectedCirclePaint.setAntiAlias(true);
        this.mSelectedCirclePaint.setColor(this.mTodayNumberColor);
        this.mSelectedCirclePaint.setTextAlign(Paint.Align.CENTER);
        this.mSelectedCirclePaint.setStyle(Paint.Style.FILL);
        this.mSelectedCirclePaint.setAlpha(60);
        this.mMonthDayLabelPaint = new Paint();
        this.mMonthDayLabelPaint.setAntiAlias(true);
        this.mMonthDayLabelPaint.setTextSize(MONTH_DAY_LABEL_TEXT_SIZE);
        this.mMonthDayLabelPaint.setColor(this.mDayTextColor);
        this.mMonthDayLabelPaint.setTypeface(Typeface.create(this.mDayOfWeekTypeface, 0));
        this.mMonthDayLabelPaint.setStyle(Paint.Style.FILL);
        this.mMonthDayLabelPaint.setTextAlign(Paint.Align.CENTER);
        this.mMonthDayLabelPaint.setFakeBoldText(true);
        this.mMonthNumPaint = new Paint();
        this.mMonthNumPaint.setAntiAlias(true);
        this.mMonthNumPaint.setTextSize(MINI_DAY_NUMBER_TEXT_SIZE);
        this.mMonthNumPaint.setStyle(Paint.Style.FILL);
        this.mMonthNumPaint.setTextAlign(Paint.Align.CENTER);
        this.mMonthNumPaint.setFakeBoldText(false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawMonthTitle(canvas);
        drawMonthDayLabels(canvas);
        drawMonthNums(canvas);
    }

    public void setMonthParams(HashMap<String, Integer> params) {
        if (!params.containsKey("month") && !params.containsKey("year")) {
            throw new InvalidParameterException("You must specify month and year for this view");
        }
        setTag(params);
        if (params.containsKey("height")) {
            this.mRowHeight = params.get("height").intValue();
            if (this.mRowHeight < MIN_HEIGHT) {
                this.mRowHeight = MIN_HEIGHT;
            }
        }
        if (params.containsKey("selected_day")) {
            this.mSelectedDay = params.get("selected_day").intValue();
        }
        this.mMonth = params.get("month").intValue();
        this.mYear = params.get("year").intValue();
        Time today = new Time(Time.getCurrentTimezone());
        today.setToNow();
        this.mHasToday = false;
        this.mToday = -1;
        this.mCalendar.set(2, this.mMonth);
        this.mCalendar.set(1, this.mYear);
        this.mCalendar.set(5, 1);
        this.mDayOfWeekStart = this.mCalendar.get(7);
        if (params.containsKey("week_start")) {
            this.mWeekStart = params.get("week_start").intValue();
        } else {
            this.mWeekStart = this.mCalendar.getFirstDayOfWeek();
        }
        this.mNumCells = Utils.getDaysInMonth(this.mMonth, this.mYear);
        for (int i = 0; i < this.mNumCells; i++) {
            int day = i + 1;
            if (sameDay(day, today)) {
                this.mHasToday = true;
                this.mToday = day;
            }
        }
        this.mNumRows = calculateNumRows();
        this.mTouchHelper.invalidateRoot();
    }

    public void reuse() {
        this.mNumRows = 6;
        requestLayout();
    }

    private int calculateNumRows() {
        int offset = findDayOffset();
        int dividend = (this.mNumCells + offset) / this.mNumDays;
        int remainder = (this.mNumCells + offset) % this.mNumDays;
        return (remainder > 0 ? 1 : 0) + dividend;
    }

    private boolean sameDay(int day, Time today) {
        return this.mYear == today.year && this.mMonth == today.month && day == today.monthDay;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(View.MeasureSpec.getSize(widthMeasureSpec), (this.mRowHeight * this.mNumRows) + getMonthHeaderSize());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        this.mWidth = w;
        this.mTouchHelper.invalidateRoot();
    }

    protected int getMonthHeaderSize() {
        return MONTH_HEADER_SIZE;
    }

    private String getMonthAndYearString() {
        this.mStringBuilder.setLength(0);
        long millis = this.mCalendar.getTimeInMillis();
        return DateUtils.formatDateRange(getContext(), this.mFormatter, millis, millis, 52, Time.getCurrentTimezone()).toString();
    }

    protected void drawMonthTitle(Canvas canvas) {
        int x = (this.mWidth + (this.mEdgePadding * 2)) / 2;
        int y = ((getMonthHeaderSize() - MONTH_DAY_LABEL_TEXT_SIZE) / 2) + (MONTH_LABEL_TEXT_SIZE / 3);
        canvas.drawText(getMonthAndYearString(), x, y, this.mMonthTitlePaint);
    }

    protected void drawMonthDayLabels(Canvas canvas) {
        int y = getMonthHeaderSize() - (MONTH_DAY_LABEL_TEXT_SIZE / 2);
        int dayWidthHalf = (this.mWidth - (this.mEdgePadding * 2)) / (this.mNumDays * 2);
        for (int i = 0; i < this.mNumDays; i++) {
            int calendarDay = (this.mWeekStart + i) % this.mNumDays;
            int x = (((i * 2) + 1) * dayWidthHalf) + this.mEdgePadding;
            this.mDayLabelCalendar.set(7, calendarDay);
            canvas.drawText(this.mDayLabelCalendar.getDisplayName(7, 1, Locale.getDefault()).toUpperCase(Locale.getDefault()), x, y, this.mMonthDayLabelPaint);
        }
    }

    protected void drawMonthNums(Canvas canvas) {
        int y = (((this.mRowHeight + MINI_DAY_NUMBER_TEXT_SIZE) / 2) - DAY_SEPARATOR_WIDTH) + getMonthHeaderSize();
        float dayWidthHalf = (this.mWidth - (this.mEdgePadding * 2)) / (this.mNumDays * 2.0f);
        int j = findDayOffset();
        for (int dayNumber = 1; dayNumber <= this.mNumCells; dayNumber++) {
            int x = (int) ((((j * 2) + 1) * dayWidthHalf) + this.mEdgePadding);
            int yRelativeToDay = ((this.mRowHeight + MINI_DAY_NUMBER_TEXT_SIZE) / 2) - DAY_SEPARATOR_WIDTH;
            int startX = (int) (x - dayWidthHalf);
            int stopX = (int) (x + dayWidthHalf);
            int startY = y - yRelativeToDay;
            int stopY = startY + this.mRowHeight;
            drawMonthDay(canvas, this.mYear, this.mMonth, dayNumber, x, y, startX, stopX, startY, stopY);
            j++;
            if (j == this.mNumDays) {
                j = 0;
                y += this.mRowHeight;
            }
        }
    }

    protected int findDayOffset() {
        return (this.mDayOfWeekStart < this.mWeekStart ? this.mDayOfWeekStart + this.mNumDays : this.mDayOfWeekStart) - this.mWeekStart;
    }

    public int getDayFromLocation(float x, float y) {
        int day = getInternalDayFromLocation(x, y);
        if (day < 1 || day > this.mNumCells) {
            return -1;
        }
        return day;
    }

    protected int getInternalDayFromLocation(float x, float y) {
        int dayStart = this.mEdgePadding;
        if (x < dayStart || x > this.mWidth - this.mEdgePadding) {
            return -1;
        }
        int row = ((int) (y - getMonthHeaderSize())) / this.mRowHeight;
        int column = (int) (((x - dayStart) * this.mNumDays) / ((this.mWidth - dayStart) - this.mEdgePadding));
        int day = (column - findDayOffset()) + 1;
        return day + (this.mNumDays * row);
    }

    private void onDayClick(int day) {
        if (!isOutOfRange(this.mYear, this.mMonth, day)) {
            if (this.mOnDayClickListener != null) {
                this.mOnDayClickListener.onDayClick(this, new MonthAdapter.CalendarDay(this.mYear, this.mMonth, day));
            }
            this.mTouchHelper.sendEventForVirtualView(day, 1);
        }
    }

    protected boolean isOutOfRange(int year, int month, int day) {
        return isBeforeMin(year, month, day) || isAfterMax(year, month, day);
    }

    private boolean isBeforeMin(int year, int month, int day) {
        Calendar minDate;
        if (this.mController == null || (minDate = this.mController.getMinDate()) == null) {
            return false;
        }
        if (year < minDate.get(1)) {
            return true;
        }
        if (year > minDate.get(1)) {
            return false;
        }
        if (month < minDate.get(2)) {
            return true;
        }
        return month <= minDate.get(2) && day < minDate.get(5);
    }

    private boolean isAfterMax(int year, int month, int day) {
        Calendar maxDate;
        if (this.mController == null || (maxDate = this.mController.getMaxDate()) == null) {
            return false;
        }
        if (year > maxDate.get(1)) {
            return true;
        }
        if (year < maxDate.get(1)) {
            return false;
        }
        if (month > maxDate.get(2)) {
            return true;
        }
        return month >= maxDate.get(2) && day > maxDate.get(5);
    }

    public MonthAdapter.CalendarDay getAccessibilityFocus() {
        int day = this.mTouchHelper.getFocusedVirtualView();
        if (day >= 0) {
            return new MonthAdapter.CalendarDay(this.mYear, this.mMonth, day);
        }
        return null;
    }

    public void clearAccessibilityFocus() {
        this.mTouchHelper.clearFocusedVirtualView();
    }

    public boolean restoreAccessibilityFocus(MonthAdapter.CalendarDay day) {
        if (day.year != this.mYear || day.month != this.mMonth || day.day > this.mNumCells) {
            return false;
        }
        this.mTouchHelper.setFocusedVirtualView(day.day);
        return true;
    }

    protected class MonthViewTouchHelper extends ExploreByTouchHelper {
        private final Calendar mTempCalendar;
        private final Rect mTempRect;

        public MonthViewTouchHelper(View host) {
            super(host);
            this.mTempRect = new Rect();
            this.mTempCalendar = Calendar.getInstance();
        }

        public void setFocusedVirtualView(int virtualViewId) {
            getAccessibilityNodeProvider(MonthView.this).performAction(virtualViewId, 64, null);
        }

        public void clearFocusedVirtualView() {
            int focusedVirtualView = getFocusedVirtualView();
            if (focusedVirtualView != Integer.MIN_VALUE) {
                getAccessibilityNodeProvider(MonthView.this).performAction(focusedVirtualView, 128, null);
            }
        }

        @Override
        protected int getVirtualViewAt(float x, float y) {
            int day = MonthView.this.getDayFromLocation(x, y);
            if (day >= 0) {
                return day;
            }
            return Integer.MIN_VALUE;
        }

        @Override
        protected void getVisibleVirtualViews(List<Integer> virtualViewIds) {
            for (int day = 1; day <= MonthView.this.mNumCells; day++) {
                virtualViewIds.add(Integer.valueOf(day));
            }
        }

        @Override
        protected void onPopulateEventForVirtualView(int virtualViewId, AccessibilityEvent event) {
            event.setContentDescription(getItemDescription(virtualViewId));
        }

        @Override
        protected void onPopulateNodeForVirtualView(int virtualViewId, AccessibilityNodeInfoCompat node) {
            getItemBounds(virtualViewId, this.mTempRect);
            node.setContentDescription(getItemDescription(virtualViewId));
            node.setBoundsInParent(this.mTempRect);
            node.addAction(16);
            if (virtualViewId == MonthView.this.mSelectedDay) {
                node.setSelected(true);
            }
        }

        @Override
        protected boolean onPerformActionForVirtualView(int virtualViewId, int action, Bundle arguments) {
            switch (action) {
                case 16:
                    MonthView.this.onDayClick(virtualViewId);
                    return true;
                default:
                    return false;
            }
        }

        protected void getItemBounds(int day, Rect rect) {
            int offsetX = MonthView.this.mEdgePadding;
            int offsetY = MonthView.this.getMonthHeaderSize();
            int cellHeight = MonthView.this.mRowHeight;
            int cellWidth = (MonthView.this.mWidth - (MonthView.this.mEdgePadding * 2)) / MonthView.this.mNumDays;
            int index = (day - 1) + MonthView.this.findDayOffset();
            int row = index / MonthView.this.mNumDays;
            int column = index % MonthView.this.mNumDays;
            int x = offsetX + (column * cellWidth);
            int y = offsetY + (row * cellHeight);
            rect.set(x, y, x + cellWidth, y + cellHeight);
        }

        protected CharSequence getItemDescription(int day) {
            this.mTempCalendar.set(MonthView.this.mYear, MonthView.this.mMonth, day);
            CharSequence date = DateFormat.format("dd MMMM yyyy", this.mTempCalendar.getTimeInMillis());
            return day == MonthView.this.mSelectedDay ? MonthView.this.getContext().getString(R.string.item_is_selected, date) : date;
        }
    }
}
