package com.android.calendar.month;

import android.content.Context;
import android.graphics.Rect;
import android.os.SystemClock;
import android.text.format.Time;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.ListView;
import com.android.calendar.Utils;

public class MonthListView extends ListView {
    private long mDownActionTime;
    private final Rect mFirstViewRect;
    Context mListContext;
    protected Time mTempTime;
    private final Runnable mTimezoneUpdater;
    VelocityTracker mTracker;
    private static float mScale = 0.0f;
    private static int MIN_VELOCITY_FOR_FLING = 1500;
    private static int MULTIPLE_MONTH_VELOCITY_THRESHOLD = 2000;
    private static int FLING_VELOCITY_DIVIDER = 500;
    private static int FLING_TIME = 1000;

    public MonthListView(Context context) {
        super(context);
        this.mFirstViewRect = new Rect();
        this.mTimezoneUpdater = new Runnable() {
            @Override
            public void run() {
                if (MonthListView.this.mTempTime != null && MonthListView.this.mListContext != null) {
                    MonthListView.this.mTempTime.timezone = Utils.getTimeZone(MonthListView.this.mListContext, MonthListView.this.mTimezoneUpdater);
                }
            }
        };
        init(context);
    }

    public MonthListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mFirstViewRect = new Rect();
        this.mTimezoneUpdater = new Runnable() {
            @Override
            public void run() {
                if (MonthListView.this.mTempTime != null && MonthListView.this.mListContext != null) {
                    MonthListView.this.mTempTime.timezone = Utils.getTimeZone(MonthListView.this.mListContext, MonthListView.this.mTimezoneUpdater);
                }
            }
        };
        init(context);
    }

    public MonthListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mFirstViewRect = new Rect();
        this.mTimezoneUpdater = new Runnable() {
            @Override
            public void run() {
                if (MonthListView.this.mTempTime != null && MonthListView.this.mListContext != null) {
                    MonthListView.this.mTempTime.timezone = Utils.getTimeZone(MonthListView.this.mListContext, MonthListView.this.mTimezoneUpdater);
                }
            }
        };
        init(context);
    }

    private void init(Context c) {
        this.mListContext = c;
        this.mTracker = VelocityTracker.obtain();
        this.mTempTime = new Time(Utils.getTimeZone(c, this.mTimezoneUpdater));
        if (mScale == 0.0f) {
            mScale = c.getResources().getDisplayMetrics().density;
            if (mScale != 1.0f) {
                MIN_VELOCITY_FOR_FLING = (int) (MIN_VELOCITY_FOR_FLING * mScale);
                MULTIPLE_MONTH_VELOCITY_THRESHOLD = (int) (MULTIPLE_MONTH_VELOCITY_THRESHOLD * mScale);
                FLING_VELOCITY_DIVIDER = (int) (FLING_VELOCITY_DIVIDER * mScale);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return processEvent(ev) || super.onTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return processEvent(ev) || super.onInterceptTouchEvent(ev);
    }

    private boolean processEvent(MotionEvent ev) {
        switch (ev.getAction() & 255) {
            case 0:
                this.mTracker.clear();
                this.mDownActionTime = SystemClock.uptimeMillis();
                break;
            case 1:
                this.mTracker.addMovement(ev);
                this.mTracker.computeCurrentVelocity(1000);
                float vel = this.mTracker.getYVelocity();
                if (Math.abs(vel) > MIN_VELOCITY_FOR_FLING) {
                    doFling(vel);
                }
                break;
            case 2:
            default:
                this.mTracker.addMovement(ev);
                break;
            case 3:
                break;
        }
        return false;
    }

    private void doFling(float velocityY) {
        int monthsToJump;
        MotionEvent cancelEvent = MotionEvent.obtain(this.mDownActionTime, SystemClock.uptimeMillis(), 3, 0.0f, 0.0f, 0);
        onTouchEvent(cancelEvent);
        if (Math.abs(velocityY) < MULTIPLE_MONTH_VELOCITY_THRESHOLD) {
            if (velocityY < 0.0f) {
                monthsToJump = 1;
            } else {
                monthsToJump = 0;
            }
        } else if (velocityY < 0.0f) {
            monthsToJump = 1 - ((int) ((MULTIPLE_MONTH_VELOCITY_THRESHOLD + velocityY) / FLING_VELOCITY_DIVIDER));
        } else {
            monthsToJump = -((int) ((velocityY - MULTIPLE_MONTH_VELOCITY_THRESHOLD) / FLING_VELOCITY_DIVIDER));
        }
        int day = getUpperRightJulianDay();
        this.mTempTime.setJulianDay(day);
        this.mTempTime.monthDay = 1;
        this.mTempTime.month += monthsToJump;
        long timeInMillis = this.mTempTime.normalize(false);
        int scrollToDay = Time.getJulianDay(timeInMillis, this.mTempTime.gmtoff) + (monthsToJump > 0 ? 6 : 0);
        View firstView = getChildAt(0);
        int firstViewHeight = firstView.getHeight();
        firstView.getLocalVisibleRect(this.mFirstViewRect);
        int topViewVisiblePart = this.mFirstViewRect.bottom - this.mFirstViewRect.top;
        int viewsToFling = ((scrollToDay - day) / 7) - (monthsToJump <= 0 ? 1 : 0);
        int offset = viewsToFling > 0 ? -((firstViewHeight - topViewVisiblePart) + SimpleDayPickerFragment.LIST_TOP_OFFSET) : topViewVisiblePart - SimpleDayPickerFragment.LIST_TOP_OFFSET;
        smoothScrollBy((viewsToFling * firstViewHeight) + offset, FLING_TIME);
    }

    private int getUpperRightJulianDay() {
        SimpleWeekView child = (SimpleWeekView) getChildAt(0);
        if (child == null) {
            return -1;
        }
        return (child.getFirstJulianDay() + 7) - 1;
    }
}
