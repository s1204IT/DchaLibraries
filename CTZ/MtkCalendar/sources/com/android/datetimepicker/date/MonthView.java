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

    public MonthView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
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
        Resources resources = context.getResources();
        this.mDayLabelCalendar = Calendar.getInstance();
        this.mCalendar = Calendar.getInstance();
        this.mDayOfWeekTypeface = resources.getString(R.string.day_of_week_label_typeface);
        this.mMonthTitleTypeface = resources.getString(R.string.sans_serif);
        this.mDayTextColor = resources.getColor(R.color.date_picker_text_normal);
        this.mTodayNumberColor = resources.getColor(R.color.blue);
        this.mDisabledDayTextColor = resources.getColor(R.color.date_picker_text_disabled);
        this.mMonthTitleColor = resources.getColor(android.R.color.white);
        this.mMonthTitleBGColor = resources.getColor(R.color.circle_background);
        this.mStringBuilder = new StringBuilder(50);
        this.mFormatter = new Formatter(this.mStringBuilder, Locale.getDefault());
        MINI_DAY_NUMBER_TEXT_SIZE = resources.getDimensionPixelSize(R.dimen.day_number_size);
        MONTH_LABEL_TEXT_SIZE = resources.getDimensionPixelSize(R.dimen.month_label_size);
        MONTH_DAY_LABEL_TEXT_SIZE = resources.getDimensionPixelSize(R.dimen.month_day_label_text_size);
        MONTH_HEADER_SIZE = resources.getDimensionPixelOffset(R.dimen.month_list_item_header_height);
        DAY_SELECTED_CIRCLE_SIZE = resources.getDimensionPixelSize(R.dimen.day_number_select_circle_radius);
        this.mRowHeight = (resources.getDimensionPixelOffset(R.dimen.date_picker_view_animator_height) - getMonthHeaderSize()) / 6;
        this.mTouchHelper = getMonthViewTouchHelper();
        ViewCompat.setAccessibilityDelegate(this, this.mTouchHelper);
        ViewCompat.setImportantForAccessibility(this, 1);
        this.mLockAccessibilityDelegate = true;
        initView();
    }

    public void setDatePickerController(DatePickerController datePickerController) {
        this.mController = datePickerController;
    }

    protected MonthViewTouchHelper getMonthViewTouchHelper() {
        return new MonthViewTouchHelper(this);
    }

    @Override
    public void setAccessibilityDelegate(View.AccessibilityDelegate accessibilityDelegate) {
        if (!this.mLockAccessibilityDelegate) {
            super.setAccessibilityDelegate(accessibilityDelegate);
        }
    }

    public void setOnDayClickListener(OnDayClickListener onDayClickListener) {
        this.mOnDayClickListener = onDayClickListener;
    }

    @Override
    public boolean dispatchHoverEvent(MotionEvent motionEvent) {
        if (this.mTouchHelper.dispatchHoverEvent(motionEvent)) {
            return true;
        }
        return super.dispatchHoverEvent(motionEvent);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        int dayFromLocation;
        if (motionEvent.getAction() == 1 && (dayFromLocation = getDayFromLocation(motionEvent.getX(), motionEvent.getY())) >= 0) {
            onDayClick(dayFromLocation);
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

    public void setMonthParams(HashMap<String, Integer> map) {
        if (!map.containsKey("month") && !map.containsKey("year")) {
            throw new InvalidParameterException("You must specify month and year for this view");
        }
        setTag(map);
        if (map.containsKey("height")) {
            this.mRowHeight = map.get("height").intValue();
            if (this.mRowHeight < MIN_HEIGHT) {
                this.mRowHeight = MIN_HEIGHT;
            }
        }
        if (map.containsKey("selected_day")) {
            this.mSelectedDay = map.get("selected_day").intValue();
        }
        this.mMonth = map.get("month").intValue();
        this.mYear = map.get("year").intValue();
        Time time = new Time(Time.getCurrentTimezone());
        time.setToNow();
        int i = 0;
        this.mHasToday = false;
        this.mToday = -1;
        this.mCalendar.set(2, this.mMonth);
        this.mCalendar.set(1, this.mYear);
        this.mCalendar.set(5, 1);
        this.mDayOfWeekStart = this.mCalendar.get(7);
        if (map.containsKey("week_start")) {
            this.mWeekStart = map.get("week_start").intValue();
        } else {
            this.mWeekStart = this.mCalendar.getFirstDayOfWeek();
        }
        this.mNumCells = Utils.getDaysInMonth(this.mMonth, this.mYear);
        while (i < this.mNumCells) {
            i++;
            if (sameDay(i, time)) {
                this.mHasToday = true;
                this.mToday = i;
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
        int iFindDayOffset = findDayOffset();
        return ((this.mNumCells + iFindDayOffset) / this.mNumDays) + ((iFindDayOffset + this.mNumCells) % this.mNumDays > 0 ? 1 : 0);
    }

    private boolean sameDay(int i, Time time) {
        return this.mYear == time.year && this.mMonth == time.month && i == time.monthDay;
    }

    @Override
    protected void onMeasure(int i, int i2) {
        setMeasuredDimension(View.MeasureSpec.getSize(i), (this.mRowHeight * this.mNumRows) + getMonthHeaderSize());
    }

    @Override
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        this.mWidth = i;
        this.mTouchHelper.invalidateRoot();
    }

    protected int getMonthHeaderSize() {
        return MONTH_HEADER_SIZE;
    }

    private String getMonthAndYearString() {
        this.mStringBuilder.setLength(0);
        long timeInMillis = this.mCalendar.getTimeInMillis();
        return DateUtils.formatDateRange(getContext(), this.mFormatter, timeInMillis, timeInMillis, 52, Time.getCurrentTimezone()).toString();
    }

    protected void drawMonthTitle(Canvas canvas) {
        canvas.drawText(getMonthAndYearString(), (this.mWidth + (this.mEdgePadding * 2)) / 2, ((getMonthHeaderSize() - MONTH_DAY_LABEL_TEXT_SIZE) / 2) + (MONTH_LABEL_TEXT_SIZE / 3), this.mMonthTitlePaint);
    }

    protected void drawMonthDayLabels(Canvas canvas) {
        int monthHeaderSize = getMonthHeaderSize() - (MONTH_DAY_LABEL_TEXT_SIZE / 2);
        int i = (this.mWidth - (this.mEdgePadding * 2)) / (this.mNumDays * 2);
        for (int i2 = 0; i2 < this.mNumDays; i2++) {
            int i3 = (this.mWeekStart + i2) % this.mNumDays;
            int i4 = (((2 * i2) + 1) * i) + this.mEdgePadding;
            this.mDayLabelCalendar.set(7, i3);
            canvas.drawText(this.mDayLabelCalendar.getDisplayName(7, 1, Locale.getDefault()).toUpperCase(Locale.getDefault()), i4, monthHeaderSize, this.mMonthDayLabelPaint);
        }
    }

    protected void drawMonthNums(Canvas canvas) {
        float f = (this.mWidth - (this.mEdgePadding * 2)) / (this.mNumDays * 2.0f);
        int monthHeaderSize = (((this.mRowHeight + MINI_DAY_NUMBER_TEXT_SIZE) / 2) - DAY_SEPARATOR_WIDTH) + getMonthHeaderSize();
        int iFindDayOffset = findDayOffset();
        for (int i = 1; i <= this.mNumCells; i++) {
            int i2 = (int) ((((2 * iFindDayOffset) + 1) * f) + this.mEdgePadding);
            float f2 = i2;
            int i3 = monthHeaderSize - (((this.mRowHeight + MINI_DAY_NUMBER_TEXT_SIZE) / 2) - DAY_SEPARATOR_WIDTH);
            drawMonthDay(canvas, this.mYear, this.mMonth, i, i2, monthHeaderSize, (int) (f2 - f), (int) (f2 + f), i3, i3 + this.mRowHeight);
            int i4 = iFindDayOffset + 1;
            if (i4 == this.mNumDays) {
                i4 = 0;
                monthHeaderSize += this.mRowHeight;
            }
            iFindDayOffset = i4;
        }
    }

    protected int findDayOffset() {
        return (this.mDayOfWeekStart < this.mWeekStart ? this.mDayOfWeekStart + this.mNumDays : this.mDayOfWeekStart) - this.mWeekStart;
    }

    public int getDayFromLocation(float f, float f2) {
        int internalDayFromLocation = getInternalDayFromLocation(f, f2);
        if (internalDayFromLocation < 1 || internalDayFromLocation > this.mNumCells) {
            return -1;
        }
        return internalDayFromLocation;
    }

    protected int getInternalDayFromLocation(float f, float f2) {
        float f3 = this.mEdgePadding;
        if (f < f3 || f > this.mWidth - this.mEdgePadding) {
            return -1;
        }
        return (((int) (((f - f3) * this.mNumDays) / ((this.mWidth - r0) - this.mEdgePadding))) - findDayOffset()) + 1 + ((((int) (f2 - getMonthHeaderSize())) / this.mRowHeight) * this.mNumDays);
    }

    private void onDayClick(int i) {
        if (isOutOfRange(this.mYear, this.mMonth, i)) {
            return;
        }
        if (this.mOnDayClickListener != null) {
            this.mOnDayClickListener.onDayClick(this, new MonthAdapter.CalendarDay(this.mYear, this.mMonth, i));
        }
        this.mTouchHelper.sendEventForVirtualView(i, 1);
    }

    protected boolean isOutOfRange(int i, int i2, int i3) {
        return isBeforeMin(i, i2, i3) || isAfterMax(i, i2, i3);
    }

    private boolean isBeforeMin(int i, int i2, int i3) {
        Calendar minDate;
        if (this.mController == null || (minDate = this.mController.getMinDate()) == null) {
            return false;
        }
        if (i < minDate.get(1)) {
            return true;
        }
        if (i > minDate.get(1)) {
            return false;
        }
        if (i2 < minDate.get(2)) {
            return true;
        }
        return i2 <= minDate.get(2) && i3 < minDate.get(5);
    }

    private boolean isAfterMax(int i, int i2, int i3) {
        Calendar maxDate;
        if (this.mController == null || (maxDate = this.mController.getMaxDate()) == null) {
            return false;
        }
        if (i > maxDate.get(1)) {
            return true;
        }
        if (i < maxDate.get(1)) {
            return false;
        }
        if (i2 > maxDate.get(2)) {
            return true;
        }
        return i2 >= maxDate.get(2) && i3 > maxDate.get(5);
    }

    public MonthAdapter.CalendarDay getAccessibilityFocus() {
        int focusedVirtualView = this.mTouchHelper.getFocusedVirtualView();
        if (focusedVirtualView >= 0) {
            return new MonthAdapter.CalendarDay(this.mYear, this.mMonth, focusedVirtualView);
        }
        return null;
    }

    public void clearAccessibilityFocus() {
        this.mTouchHelper.clearFocusedVirtualView();
    }

    public boolean restoreAccessibilityFocus(MonthAdapter.CalendarDay calendarDay) {
        if (calendarDay.year != this.mYear || calendarDay.month != this.mMonth || calendarDay.day > this.mNumCells) {
            return false;
        }
        this.mTouchHelper.setFocusedVirtualView(calendarDay.day);
        return true;
    }

    protected class MonthViewTouchHelper extends ExploreByTouchHelper {
        private final Calendar mTempCalendar;
        private final Rect mTempRect;

        public MonthViewTouchHelper(View view) {
            super(view);
            this.mTempRect = new Rect();
            this.mTempCalendar = Calendar.getInstance();
        }

        public void setFocusedVirtualView(int i) {
            getAccessibilityNodeProvider(MonthView.this).performAction(i, 64, null);
        }

        public void clearFocusedVirtualView() {
            int focusedVirtualView = getFocusedVirtualView();
            if (focusedVirtualView != Integer.MIN_VALUE) {
                getAccessibilityNodeProvider(MonthView.this).performAction(focusedVirtualView, 128, null);
            }
        }

        @Override
        protected int getVirtualViewAt(float f, float f2) {
            int dayFromLocation = MonthView.this.getDayFromLocation(f, f2);
            if (dayFromLocation >= 0) {
                return dayFromLocation;
            }
            return Integer.MIN_VALUE;
        }

        @Override
        protected void getVisibleVirtualViews(List<Integer> list) {
            for (int i = 1; i <= MonthView.this.mNumCells; i++) {
                list.add(Integer.valueOf(i));
            }
        }

        @Override
        protected void onPopulateEventForVirtualView(int i, AccessibilityEvent accessibilityEvent) {
            accessibilityEvent.setContentDescription(getItemDescription(i));
        }

        @Override
        protected void onPopulateNodeForVirtualView(int i, AccessibilityNodeInfoCompat accessibilityNodeInfoCompat) {
            getItemBounds(i, this.mTempRect);
            accessibilityNodeInfoCompat.setContentDescription(getItemDescription(i));
            accessibilityNodeInfoCompat.setBoundsInParent(this.mTempRect);
            accessibilityNodeInfoCompat.addAction(16);
            if (i == MonthView.this.mSelectedDay) {
                accessibilityNodeInfoCompat.setSelected(true);
            }
        }

        @Override
        protected boolean onPerformActionForVirtualView(int i, int i2, Bundle bundle) {
            if (i2 == 16) {
                MonthView.this.onDayClick(i);
                return true;
            }
            return false;
        }

        protected void getItemBounds(int i, Rect rect) {
            int i2 = MonthView.this.mEdgePadding;
            int monthHeaderSize = MonthView.this.getMonthHeaderSize();
            int i3 = MonthView.this.mRowHeight;
            int i4 = (MonthView.this.mWidth - (2 * MonthView.this.mEdgePadding)) / MonthView.this.mNumDays;
            int iFindDayOffset = (i - 1) + MonthView.this.findDayOffset();
            int i5 = iFindDayOffset / MonthView.this.mNumDays;
            int i6 = i2 + ((iFindDayOffset % MonthView.this.mNumDays) * i4);
            int i7 = monthHeaderSize + (i5 * i3);
            rect.set(i6, i7, i4 + i6, i3 + i7);
        }

        protected CharSequence getItemDescription(int i) {
            this.mTempCalendar.set(MonthView.this.mYear, MonthView.this.mMonth, i);
            CharSequence charSequence = DateFormat.format("dd MMMM yyyy", this.mTempCalendar.getTimeInMillis());
            return i == MonthView.this.mSelectedDay ? MonthView.this.getContext().getString(R.string.item_is_selected, charSequence) : charSequence;
        }
    }
}
