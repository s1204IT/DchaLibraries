package com.android.calendar.month;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.format.Time;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import com.android.calendar.R;
import com.android.calendar.Utils;
import java.security.InvalidParameterException;
import java.util.HashMap;

public class SimpleWeekView extends View {
    protected int mBGColor;
    protected String[] mDayNumbers;
    protected int mDaySeparatorColor;
    protected int mFirstJulianDay;
    protected int mFirstMonth;
    protected boolean[] mFocusDay;
    protected int mFocusMonthColor;
    protected boolean mHasSelectedDay;
    protected boolean mHasToday;
    protected int mHeight;
    Time mLastHoverTime;
    protected int mLastMonth;
    protected Paint mMonthNumPaint;
    protected int mNumCells;
    protected int mNumDays;
    protected boolean[] mOddMonth;
    protected int mOtherMonthColor;
    protected int mPadding;
    protected int mSelectedDay;
    protected Drawable mSelectedDayLine;
    protected int mSelectedLeft;
    protected int mSelectedRight;
    protected int mSelectedWeekBGColor;
    protected boolean mShowWeekNum;
    protected String mTimeZone;
    protected int mToday;
    protected int mTodayOutlineColor;
    protected int mWeek;
    protected int mWeekNumColor;
    protected int mWeekStart;
    protected int mWidth;
    protected Paint p;
    protected Rect r;
    protected static int DEFAULT_HEIGHT = 32;
    protected static int MIN_HEIGHT = 10;
    protected static int DAY_SEPARATOR_WIDTH = 1;
    protected static int MINI_DAY_NUMBER_TEXT_SIZE = 14;
    protected static int MINI_WK_NUMBER_TEXT_SIZE = 12;
    protected static int MINI_TODAY_NUMBER_TEXT_SIZE = 18;
    protected static int MINI_TODAY_OUTLINE_WIDTH = 2;
    protected static int WEEK_NUM_MARGIN_BOTTOM = 4;
    protected static float mScale = 0.0f;

    public SimpleWeekView(Context context) {
        super(context);
        this.mPadding = 0;
        this.r = new Rect();
        this.p = new Paint();
        this.mFirstJulianDay = -1;
        this.mFirstMonth = -1;
        this.mLastMonth = -1;
        this.mWeek = -1;
        this.mHeight = DEFAULT_HEIGHT;
        this.mShowWeekNum = false;
        this.mHasSelectedDay = false;
        this.mHasToday = false;
        this.mSelectedDay = -1;
        this.mToday = -1;
        this.mWeekStart = 0;
        this.mNumDays = 7;
        this.mNumCells = this.mNumDays;
        this.mSelectedLeft = -1;
        this.mSelectedRight = -1;
        this.mTimeZone = Time.getCurrentTimezone();
        this.mLastHoverTime = null;
        Resources resources = context.getResources();
        this.mBGColor = resources.getColor(R.color.month_bgcolor);
        this.mSelectedWeekBGColor = resources.getColor(R.color.month_selected_week_bgcolor);
        this.mFocusMonthColor = resources.getColor(R.color.month_mini_day_number);
        this.mOtherMonthColor = resources.getColor(R.color.month_other_month_day_number);
        this.mDaySeparatorColor = resources.getColor(R.color.month_grid_lines);
        this.mTodayOutlineColor = resources.getColor(R.color.mini_month_today_outline_color);
        this.mWeekNumColor = resources.getColor(R.color.month_week_num_color);
        this.mSelectedDayLine = resources.getDrawable(R.drawable.dayline_minical_holo_light);
        if (mScale == 0.0f) {
            mScale = context.getResources().getDisplayMetrics().density;
            if (mScale != 1.0f) {
                DEFAULT_HEIGHT = (int) (DEFAULT_HEIGHT * mScale);
                MIN_HEIGHT = (int) (MIN_HEIGHT * mScale);
                MINI_DAY_NUMBER_TEXT_SIZE = (int) (MINI_DAY_NUMBER_TEXT_SIZE * mScale);
                MINI_TODAY_NUMBER_TEXT_SIZE = (int) (MINI_TODAY_NUMBER_TEXT_SIZE * mScale);
                MINI_TODAY_OUTLINE_WIDTH = (int) (MINI_TODAY_OUTLINE_WIDTH * mScale);
                WEEK_NUM_MARGIN_BOTTOM = (int) (WEEK_NUM_MARGIN_BOTTOM * mScale);
                DAY_SEPARATOR_WIDTH = (int) (DAY_SEPARATOR_WIDTH * mScale);
                MINI_WK_NUMBER_TEXT_SIZE = (int) (MINI_WK_NUMBER_TEXT_SIZE * mScale);
            }
        }
        initView();
    }

    public void setWeekParams(HashMap<String, Integer> map, String str) {
        int i;
        if (!map.containsKey("week")) {
            throw new InvalidParameterException("You must specify the week number for this view");
        }
        setTag(map);
        this.mTimeZone = str;
        if (map.containsKey("height")) {
            this.mHeight = map.get("height").intValue();
            if (this.mHeight < MIN_HEIGHT) {
                this.mHeight = MIN_HEIGHT;
            }
        }
        if (map.containsKey("selected_day")) {
            this.mSelectedDay = map.get("selected_day").intValue();
        }
        this.mHasSelectedDay = this.mSelectedDay != -1;
        if (map.containsKey("num_days")) {
            this.mNumDays = map.get("num_days").intValue();
        }
        if (map.containsKey("show_wk_num")) {
            if (map.get("show_wk_num").intValue() != 0) {
                this.mShowWeekNum = true;
            } else {
                this.mShowWeekNum = false;
            }
        }
        this.mNumCells = this.mShowWeekNum ? this.mNumDays + 1 : this.mNumDays;
        this.mDayNumbers = new String[this.mNumCells];
        this.mFocusDay = new boolean[this.mNumCells];
        this.mOddMonth = new boolean[this.mNumCells];
        this.mWeek = map.get("week").intValue();
        int julianMondayFromWeeksSinceEpoch = Utils.getJulianMondayFromWeeksSinceEpoch(this.mWeek);
        Time time = new Time(str);
        Utils.setJulianDayInGeneral(time, julianMondayFromWeeksSinceEpoch);
        if (this.mShowWeekNum) {
            this.mDayNumbers[0] = Integer.toString(time.getWeekNumber());
            i = 1;
        } else {
            i = 0;
        }
        if (map.containsKey("week_start")) {
            this.mWeekStart = map.get("week_start").intValue();
        }
        if (time.weekDay != this.mWeekStart) {
            int i2 = time.weekDay - this.mWeekStart;
            if (i2 < 0) {
                i2 += 7;
            }
            time.monthDay -= i2;
            time.normalize(true);
        }
        this.mFirstJulianDay = Time.getJulianDay(time.toMillis(true), time.gmtoff);
        this.mFirstMonth = time.month;
        Time time2 = new Time(str);
        time2.setToNow();
        this.mHasToday = false;
        this.mToday = -1;
        int iIntValue = map.containsKey("focus_month") ? map.get("focus_month").intValue() : -1;
        while (i < this.mNumCells) {
            if (time.monthDay == 1) {
                this.mFirstMonth = time.month;
            }
            this.mOddMonth[i] = time.month % 2 == 1;
            if (time.month == iIntValue) {
                this.mFocusDay[i] = true;
            } else {
                this.mFocusDay[i] = false;
            }
            if (time.year == time2.year && time.yearDay == time2.yearDay) {
                this.mHasToday = true;
                this.mToday = i;
            }
            String[] strArr = this.mDayNumbers;
            int i3 = time.monthDay;
            time.monthDay = i3 + 1;
            strArr[i] = Integer.toString(i3);
            time.normalize(true);
            i++;
        }
        if (time.monthDay == 1) {
            time.monthDay--;
            time.normalize(true);
        }
        this.mLastMonth = time.month;
        updateSelectionPositions();
    }

    protected void initView() {
        this.p.setFakeBoldText(false);
        this.p.setAntiAlias(true);
        this.p.setTextSize(MINI_DAY_NUMBER_TEXT_SIZE);
        this.p.setStyle(Paint.Style.FILL);
        this.mMonthNumPaint = new Paint();
        this.mMonthNumPaint.setFakeBoldText(true);
        this.mMonthNumPaint.setAntiAlias(true);
        this.mMonthNumPaint.setTextSize(MINI_DAY_NUMBER_TEXT_SIZE);
        this.mMonthNumPaint.setColor(this.mFocusMonthColor);
        this.mMonthNumPaint.setStyle(Paint.Style.FILL);
        this.mMonthNumPaint.setTextAlign(Paint.Align.CENTER);
    }

    public int getFirstMonth() {
        return this.mFirstMonth;
    }

    public int getLastMonth() {
        return this.mLastMonth;
    }

    public int getFirstJulianDay() {
        return this.mFirstJulianDay;
    }

    public Time getDayFromLocation(float f) {
        float f2 = this.mShowWeekNum ? ((this.mWidth - (this.mPadding * 2)) / this.mNumCells) + this.mPadding : this.mPadding;
        if (f < f2 || f > this.mWidth - this.mPadding) {
            return null;
        }
        int i = this.mFirstJulianDay + ((int) (((f - f2) * this.mNumDays) / ((this.mWidth - r0) - this.mPadding)));
        Time time = new Time(this.mTimeZone);
        time.setJulianDay(i);
        return time;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawBackground(canvas);
        drawWeekNums(canvas);
        drawDaySeparators(canvas);
    }

    protected void drawBackground(Canvas canvas) {
        if (this.mHasSelectedDay) {
            this.p.setColor(this.mSelectedWeekBGColor);
            this.p.setStyle(Paint.Style.FILL);
            this.r.top = 1;
            this.r.bottom = this.mHeight - 1;
            this.r.left = this.mPadding;
            this.r.right = this.mSelectedLeft;
            canvas.drawRect(this.r, this.p);
            this.r.left = this.mSelectedRight;
            this.r.right = this.mWidth - this.mPadding;
            canvas.drawRect(this.r, this.p);
        }
    }

    protected void drawWeekNums(Canvas canvas) {
        int i;
        int i2 = ((this.mHeight + MINI_DAY_NUMBER_TEXT_SIZE) / 2) - DAY_SEPARATOR_WIDTH;
        int i3 = this.mNumCells;
        int i4 = 2 * i3;
        if (this.mShowWeekNum) {
            this.p.setTextSize(MINI_WK_NUMBER_TEXT_SIZE);
            this.p.setStyle(Paint.Style.FILL);
            this.p.setTextAlign(Paint.Align.CENTER);
            this.p.setAntiAlias(true);
            this.p.setColor(this.mWeekNumColor);
            canvas.drawText(this.mDayNumbers[0], ((this.mWidth - (this.mPadding * 2)) / i4) + this.mPadding, i2, this.p);
            i = 1;
        } else {
            i = 0;
        }
        boolean z = this.mFocusDay[i];
        this.mMonthNumPaint.setColor(z ? this.mFocusMonthColor : this.mOtherMonthColor);
        this.mMonthNumPaint.setFakeBoldText(false);
        while (i < i3) {
            if (this.mFocusDay[i] != z) {
                z = this.mFocusDay[i];
                this.mMonthNumPaint.setColor(z ? this.mFocusMonthColor : this.mOtherMonthColor);
            }
            if (this.mHasToday && this.mToday == i) {
                this.mMonthNumPaint.setTextSize(MINI_TODAY_NUMBER_TEXT_SIZE);
                this.mMonthNumPaint.setFakeBoldText(true);
            }
            canvas.drawText(this.mDayNumbers[i], ((((2 * i) + 1) * (this.mWidth - (this.mPadding * 2))) / i4) + this.mPadding, i2, this.mMonthNumPaint);
            if (this.mHasToday && this.mToday == i) {
                this.mMonthNumPaint.setTextSize(MINI_DAY_NUMBER_TEXT_SIZE);
                this.mMonthNumPaint.setFakeBoldText(false);
            }
            i++;
        }
    }

    protected void drawDaySeparators(Canvas canvas) {
        if (this.mHasSelectedDay) {
            this.r.top = 1;
            this.r.bottom = this.mHeight - 1;
            this.r.left = this.mSelectedLeft + 1;
            this.r.right = this.mSelectedRight - 1;
            this.p.setStrokeWidth(MINI_TODAY_OUTLINE_WIDTH);
            this.p.setStyle(Paint.Style.STROKE);
            this.p.setColor(this.mTodayOutlineColor);
            canvas.drawRect(this.r, this.p);
        }
        if (this.mShowWeekNum) {
            this.p.setColor(this.mDaySeparatorColor);
            this.p.setStrokeWidth(DAY_SEPARATOR_WIDTH);
            float f = ((this.mWidth - (this.mPadding * 2)) / this.mNumCells) + this.mPadding;
            canvas.drawLine(f, 0.0f, f, this.mHeight, this.p);
        }
    }

    @Override
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        this.mWidth = i;
        updateSelectionPositions();
    }

    protected void updateSelectionPositions() {
        if (this.mHasSelectedDay) {
            int i = this.mSelectedDay - this.mWeekStart;
            if (i < 0) {
                i += 7;
            }
            if (this.mShowWeekNum) {
                i++;
            }
            this.mSelectedLeft = (((this.mWidth - (this.mPadding * 2)) * i) / this.mNumCells) + this.mPadding;
            this.mSelectedRight = (((i + 1) * (this.mWidth - (this.mPadding * 2))) / this.mNumCells) + this.mPadding;
        }
    }

    @Override
    protected void onMeasure(int i, int i2) {
        setMeasuredDimension(View.MeasureSpec.getSize(i), this.mHeight);
    }

    @Override
    public boolean onHoverEvent(MotionEvent motionEvent) {
        Time dayFromLocation;
        Context context = getContext();
        AccessibilityManager accessibilityManager = (AccessibilityManager) context.getSystemService("accessibility");
        if (!accessibilityManager.isEnabled() || !accessibilityManager.isTouchExplorationEnabled()) {
            return super.onHoverEvent(motionEvent);
        }
        if (motionEvent.getAction() != 10 && (dayFromLocation = getDayFromLocation(motionEvent.getX())) != null && (this.mLastHoverTime == null || Time.compare(dayFromLocation, this.mLastHoverTime) != 0)) {
            Long lValueOf = Long.valueOf(dayFromLocation.toMillis(true));
            String dateRange = Utils.formatDateRange(context, lValueOf.longValue(), lValueOf.longValue(), 16);
            AccessibilityEvent accessibilityEventObtain = AccessibilityEvent.obtain(64);
            accessibilityEventObtain.getText().add(dateRange);
            sendAccessibilityEventUnchecked(accessibilityEventObtain);
            this.mLastHoverTime = dayFromLocation;
        }
        return true;
    }
}
