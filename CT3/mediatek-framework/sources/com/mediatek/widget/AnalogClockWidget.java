package com.mediatek.widget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Process;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RemoteViews;
import java.util.TimeZone;

@RemoteViews.RemoteView
public class AnalogClockWidget extends View {
    private static final float HOUR_RADIUS_SCALE = 0.5f;
    private static final float MINUTE_RADIUS_SCALE = 0.8f;
    private static final float OFFSET_RADIUS_SCALE = 0.1f;
    private static final float RADIUS_SCALE = 0.9f;
    private static final float STROKE_WIDTH = 4.0f;
    private static final float STROKE_WIDTH_MS = 3.0f;
    private boolean mAttached;
    private Time mCalendar;
    private final float mDensity;
    private final Handler mHandler;
    private float mHour;
    private final BroadcastReceiver mIntentReceiver;
    private float mMinutes;
    private final Paint mPaint;

    public AnalogClockWidget(Context context) {
        this(context, null);
    }

    public AnalogClockWidget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AnalogClockWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mPaint = new Paint();
        this.mHandler = new Handler();
        this.mIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if (intent.getAction().equals("android.intent.action.TIMEZONE_CHANGED")) {
                    String tz = intent.getStringExtra("time-zone");
                    AnalogClockWidget.this.mCalendar = new Time(TimeZone.getTimeZone(tz).getID());
                }
                AnalogClockWidget.this.onTimeChanged();
                AnalogClockWidget.this.invalidate();
            }
        };
        this.mDensity = context.getResources().getDisplayMetrics().density;
        this.mCalendar = new Time();
        this.mPaint.setStyle(Paint.Style.STROKE);
        this.mPaint.setStrokeWidth(this.mDensity * STROKE_WIDTH);
        this.mPaint.setColor(-1);
        this.mPaint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int x = (((View) this).mRight - ((View) this).mLeft) / 2;
        int y = (((View) this).mBottom - ((View) this).mTop) / 2;
        float radius = Math.min(x, y) * RADIUS_SCALE;
        this.mPaint.setStrokeWidth(this.mDensity * STROKE_WIDTH);
        canvas.drawCircle(x, y, radius, this.mPaint);
        this.mPaint.setStrokeWidth(this.mDensity * STROKE_WIDTH_MS);
        canvas.save();
        canvas.rotate((this.mHour / 12.0f) * 360.0f, x, y);
        canvas.drawLine(x, (radius * OFFSET_RADIUS_SCALE) + y, x, y - (HOUR_RADIUS_SCALE * radius), this.mPaint);
        canvas.restore();
        canvas.save();
        canvas.rotate((this.mMinutes / 60.0f) * 360.0f, x, y);
        canvas.drawLine(x, (radius * OFFSET_RADIUS_SCALE) + y, x, y - (MINUTE_RADIUS_SCALE * radius), this.mPaint);
        canvas.restore();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!this.mAttached) {
            this.mAttached = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.TIME_TICK");
            filter.addAction("android.intent.action.TIME_SET");
            filter.addAction("android.intent.action.TIMEZONE_CHANGED");
            getContext().registerReceiverAsUser(this.mIntentReceiver, Process.myUserHandle(), filter, null, this.mHandler);
        }
        this.mCalendar = new Time();
        onTimeChanged();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (!this.mAttached) {
            return;
        }
        getContext().unregisterReceiver(this.mIntentReceiver);
        this.mAttached = false;
    }

    private void onTimeChanged() {
        this.mCalendar.setToNow();
        int hour = this.mCalendar.hour;
        int minute = this.mCalendar.minute;
        int second = this.mCalendar.second;
        this.mMinutes = minute + (second / 60.0f);
        this.mHour = hour + (this.mMinutes / 60.0f);
        updateContentDescription(this.mCalendar);
    }

    private void updateContentDescription(Time time) {
        String contentDescription = DateUtils.formatDateTime(((View) this).mContext, time.toMillis(false), 129);
        setContentDescription(contentDescription);
    }
}
