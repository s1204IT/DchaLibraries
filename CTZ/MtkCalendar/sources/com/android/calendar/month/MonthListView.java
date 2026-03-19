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

    public MonthListView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
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

    public MonthListView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
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

    private void init(Context context) {
        this.mListContext = context;
        this.mTracker = VelocityTracker.obtain();
        this.mTempTime = new Time(Utils.getTimeZone(context, this.mTimezoneUpdater));
        if (mScale == 0.0f) {
            mScale = context.getResources().getDisplayMetrics().density;
            if (mScale != 1.0f) {
                MIN_VELOCITY_FOR_FLING = (int) (MIN_VELOCITY_FOR_FLING * mScale);
                MULTIPLE_MONTH_VELOCITY_THRESHOLD = (int) (MULTIPLE_MONTH_VELOCITY_THRESHOLD * mScale);
                FLING_VELOCITY_DIVIDER = (int) (FLING_VELOCITY_DIVIDER * mScale);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        return processEvent(motionEvent) || super.onTouchEvent(motionEvent);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        return processEvent(motionEvent) || super.onInterceptTouchEvent(motionEvent);
    }

    private boolean processEvent(MotionEvent motionEvent) {
        int action = motionEvent.getAction() & 255;
        if (action == 3) {
            return false;
        }
        switch (action) {
            case 0:
                this.mTracker.clear();
                this.mDownActionTime = SystemClock.uptimeMillis();
                return false;
            case 1:
                this.mTracker.addMovement(motionEvent);
                this.mTracker.computeCurrentVelocity(1000);
                float yVelocity = this.mTracker.getYVelocity();
                if (Math.abs(yVelocity) > MIN_VELOCITY_FOR_FLING) {
                    doFling(yVelocity);
                    return true;
                }
                return false;
            default:
                this.mTracker.addMovement(motionEvent);
                return false;
        }
    }

    private void doFling(float f) {
        int i;
        onTouchEvent(MotionEvent.obtain(this.mDownActionTime, SystemClock.uptimeMillis(), 3, 0.0f, 0.0f, 0));
        if (Math.abs(f) < MULTIPLE_MONTH_VELOCITY_THRESHOLD) {
            i = f < 0.0f ? 1 : 0;
        } else if (f < 0.0f) {
            i = 1 - ((int) ((f + MULTIPLE_MONTH_VELOCITY_THRESHOLD) / FLING_VELOCITY_DIVIDER));
        } else {
            i = -((int) ((f - MULTIPLE_MONTH_VELOCITY_THRESHOLD) / FLING_VELOCITY_DIVIDER));
        }
        int upperRightJulianDay = getUpperRightJulianDay();
        this.mTempTime.setJulianDay(upperRightJulianDay);
        this.mTempTime.monthDay = 1;
        this.mTempTime.month += i;
        long jNormalize = this.mTempTime.normalize(false);
        if (jNormalize < 0) {
            return;
        }
        int julianDay = Time.getJulianDay(jNormalize, this.mTempTime.gmtoff) + (i > 0 ? 6 : 0);
        View childAt = getChildAt(0);
        int height = childAt.getHeight();
        childAt.getLocalVisibleRect(this.mFirstViewRect);
        int i2 = this.mFirstViewRect.bottom - this.mFirstViewRect.top;
        int i3 = ((julianDay - upperRightJulianDay) / 7) - (i > 0 ? 0 : 1);
        smoothScrollBy((i3 * height) + (i3 > 0 ? -((height - i2) + SimpleDayPickerFragment.LIST_TOP_OFFSET) : i2 - SimpleDayPickerFragment.LIST_TOP_OFFSET), FLING_TIME);
    }

    private int getUpperRightJulianDay() {
        if (((SimpleWeekView) getChildAt(0)) == null) {
            return -1;
        }
        return (r0.getFirstJulianDay() + 7) - 1;
    }
}
