package com.android.deskclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.widget.AppCompatImageView;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

public class AnalogClock extends FrameLayout {
    private final Runnable mClockTick;
    private String mDescFormat;
    private boolean mEnableSeconds;
    private final ImageView mHourHand;
    private final BroadcastReceiver mIntentReceiver;
    private final ImageView mMinuteHand;
    private final ImageView mSecondHand;
    private Calendar mTime;
    private TimeZone mTimeZone;

    public AnalogClock(Context context) {
        this(context, null);
    }

    public AnalogClock(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public AnalogClock(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if (AnalogClock.this.mTimeZone == null && "android.intent.action.TIMEZONE_CHANGED".equals(intent.getAction())) {
                    String stringExtra = intent.getStringExtra("time-zone");
                    AnalogClock.this.mTime = Calendar.getInstance(TimeZone.getTimeZone(stringExtra));
                }
                AnalogClock.this.onTimeChanged();
            }
        };
        this.mClockTick = new Runnable() {
            @Override
            public void run() {
                AnalogClock.this.onTimeChanged();
                if (AnalogClock.this.mEnableSeconds) {
                    AnalogClock.this.postDelayed(this, 1000 - (System.currentTimeMillis() % 1000));
                }
            }
        };
        this.mEnableSeconds = true;
        this.mTime = Calendar.getInstance();
        this.mDescFormat = ((SimpleDateFormat) DateFormat.getTimeFormat(context)).toLocalizedPattern();
        AppCompatImageView appCompatImageView = new AppCompatImageView(context);
        appCompatImageView.setImageResource(R.drawable.clock_analog_dial);
        appCompatImageView.getDrawable().mutate();
        addView(appCompatImageView);
        this.mHourHand = new AppCompatImageView(context);
        this.mHourHand.setImageResource(R.drawable.clock_analog_hour);
        this.mHourHand.getDrawable().mutate();
        addView(this.mHourHand);
        this.mMinuteHand = new AppCompatImageView(context);
        this.mMinuteHand.setImageResource(R.drawable.clock_analog_minute);
        this.mMinuteHand.getDrawable().mutate();
        addView(this.mMinuteHand);
        this.mSecondHand = new AppCompatImageView(context);
        this.mSecondHand.setImageResource(R.drawable.clock_analog_second);
        this.mSecondHand.getDrawable().mutate();
        addView(this.mSecondHand);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.TIME_TICK");
        intentFilter.addAction("android.intent.action.TIME_SET");
        intentFilter.addAction("android.intent.action.TIMEZONE_CHANGED");
        getContext().registerReceiver(this.mIntentReceiver, intentFilter);
        this.mTime = Calendar.getInstance(this.mTimeZone != null ? this.mTimeZone : TimeZone.getDefault());
        onTimeChanged();
        if (this.mEnableSeconds) {
            this.mClockTick.run();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getContext().unregisterReceiver(this.mIntentReceiver);
        removeCallbacks(this.mClockTick);
    }

    private void onTimeChanged() {
        this.mTime.setTimeInMillis(System.currentTimeMillis());
        this.mHourHand.setRotation(this.mTime.get(10) * 30.0f);
        this.mMinuteHand.setRotation(this.mTime.get(12) * 6.0f);
        if (this.mEnableSeconds) {
            this.mSecondHand.setRotation(this.mTime.get(13) * 6.0f);
        }
        setContentDescription(DateFormat.format(this.mDescFormat, this.mTime));
        invalidate();
    }

    public void setTimeZone(String str) {
        this.mTimeZone = TimeZone.getTimeZone(str);
        this.mTime.setTimeZone(this.mTimeZone);
        onTimeChanged();
    }

    public void enableSeconds(boolean z) {
        this.mEnableSeconds = z;
        if (this.mEnableSeconds) {
            this.mSecondHand.setVisibility(0);
            this.mClockTick.run();
        } else {
            this.mSecondHand.setVisibility(8);
        }
    }
}
