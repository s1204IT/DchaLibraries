package com.android.deskclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.AttributeSet;
import android.view.View;
import java.util.TimeZone;

public class AnalogClock extends View {
    private boolean mAttached;
    private Time mCalendar;
    private boolean mChanged;
    private final Runnable mClockTick;
    private final Context mContext;
    private final Drawable mDial;
    private final int mDialHeight;
    private final int mDialWidth;
    private final float mDotOffset;
    private Paint mDotPaint;
    private final float mDotRadius;
    private final Handler mHandler;
    private float mHour;
    private final Drawable mHourHand;
    private final BroadcastReceiver mIntentReceiver;
    private final Drawable mMinuteHand;
    private float mMinutes;
    private boolean mNoSeconds;
    private final Drawable mSecondHand;
    private float mSeconds;
    private String mTimeZoneId;

    public AnalogClock(Context context) {
        this(context, null);
    }

    public AnalogClock(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AnalogClock(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mHandler = new Handler();
        this.mNoSeconds = false;
        this.mIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if (intent.getAction().equals("android.intent.action.TIMEZONE_CHANGED")) {
                    String tz = intent.getStringExtra("time-zone");
                    AnalogClock.this.mCalendar = new Time(TimeZone.getTimeZone(tz).getID());
                }
                AnalogClock.this.onTimeChanged();
                AnalogClock.this.invalidate();
            }
        };
        this.mClockTick = new Runnable() {
            @Override
            public void run() {
                AnalogClock.this.onTimeChanged();
                AnalogClock.this.invalidate();
                AnalogClock.this.postDelayed(AnalogClock.this.mClockTick, 1000L);
            }
        };
        this.mContext = context;
        Resources r = this.mContext.getResources();
        this.mDial = r.getDrawable(R.drawable.clock_analog_dial_mipmap);
        this.mHourHand = r.getDrawable(R.drawable.clock_analog_hour_mipmap);
        this.mMinuteHand = r.getDrawable(R.drawable.clock_analog_minute_mipmap);
        this.mSecondHand = r.getDrawable(R.drawable.clock_analog_second_mipmap);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AnalogClock);
        this.mDotRadius = a.getDimension(0, 0.0f);
        this.mDotOffset = a.getDimension(1, 0.0f);
        int dotColor = a.getColor(2, -1);
        if (dotColor != 0) {
            this.mDotPaint = new Paint(1);
            this.mDotPaint.setColor(dotColor);
        }
        this.mCalendar = new Time();
        this.mDialWidth = this.mDial.getIntrinsicWidth();
        this.mDialHeight = this.mDial.getIntrinsicHeight();
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
            getContext().registerReceiver(this.mIntentReceiver, filter, null, this.mHandler);
        }
        this.mCalendar = new Time();
        onTimeChanged();
        post(this.mClockTick);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.mAttached) {
            getContext().unregisterReceiver(this.mIntentReceiver);
            removeCallbacks(this.mClockTick);
            this.mAttached = false;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);
        float hScale = 1.0f;
        float vScale = 1.0f;
        if (widthMode != 0 && widthSize < this.mDialWidth) {
            hScale = widthSize / this.mDialWidth;
        }
        if (heightMode != 0 && heightSize < this.mDialHeight) {
            vScale = heightSize / this.mDialHeight;
        }
        float scale = Math.min(hScale, vScale);
        setMeasuredDimension(resolveSizeAndState((int) (this.mDialWidth * scale), widthMeasureSpec, 0), resolveSizeAndState((int) (this.mDialHeight * scale), heightMeasureSpec, 0));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.mChanged = true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        boolean changed = this.mChanged;
        if (changed) {
            this.mChanged = false;
        }
        int availableWidth = getWidth();
        int availableHeight = getHeight();
        int x = availableWidth / 2;
        int y = availableHeight / 2;
        Drawable dial = this.mDial;
        int w = dial.getIntrinsicWidth();
        int h = dial.getIntrinsicHeight();
        boolean scaled = false;
        if (availableWidth < w || availableHeight < h) {
            scaled = true;
            float scale = Math.min(availableWidth / w, availableHeight / h);
            canvas.save();
            canvas.scale(scale, scale, x, y);
        }
        if (changed) {
            dial.setBounds(x - (w / 2), y - (h / 2), (w / 2) + x, (h / 2) + y);
        }
        dial.draw(canvas);
        if (this.mDotRadius > 0.0f && this.mDotPaint != null) {
            canvas.drawCircle(x, (y - (h / 2)) + this.mDotOffset, this.mDotRadius, this.mDotPaint);
        }
        drawHand(canvas, this.mHourHand, x, y, (this.mHour / 12.0f) * 360.0f, changed);
        drawHand(canvas, this.mMinuteHand, x, y, (this.mMinutes / 60.0f) * 360.0f, changed);
        if (!this.mNoSeconds) {
            drawHand(canvas, this.mSecondHand, x, y, (this.mSeconds / 60.0f) * 360.0f, changed);
        }
        if (scaled) {
            canvas.restore();
        }
    }

    private void drawHand(Canvas canvas, Drawable hand, int x, int y, float angle, boolean changed) {
        canvas.save();
        canvas.rotate(angle, x, y);
        if (changed) {
            int w = hand.getIntrinsicWidth();
            int h = hand.getIntrinsicHeight();
            hand.setBounds(x - (w / 2), y - (h / 2), (w / 2) + x, (h / 2) + y);
        }
        hand.draw(canvas);
        canvas.restore();
    }

    private void onTimeChanged() {
        this.mCalendar.setToNow();
        if (this.mTimeZoneId != null) {
            this.mCalendar.switchTimezone(this.mTimeZoneId);
        }
        int hour = this.mCalendar.hour;
        int minute = this.mCalendar.minute;
        int second = this.mCalendar.second;
        this.mSeconds = second;
        this.mMinutes = minute + (second / 60.0f);
        this.mHour = hour + (this.mMinutes / 60.0f);
        this.mChanged = true;
        updateContentDescription(this.mCalendar);
    }

    private void updateContentDescription(Time time) {
        String contentDescription = DateUtils.formatDateTime(this.mContext, time.toMillis(false), 129);
        setContentDescription(contentDescription);
    }

    public void setTimeZone(String id) {
        this.mTimeZoneId = id;
        onTimeChanged();
    }

    public void enableSeconds(boolean enable) {
        this.mNoSeconds = !enable;
    }
}
