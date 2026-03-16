package com.android.deskclock.timer;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.deskclock.R;

public class TimerView extends LinearLayout {
    private final Typeface mAndroidClockMonoThin;
    private final int mGrayColor;
    private TextView mHoursOnes;
    private TextView mMinutesOnes;
    private TextView mMinutesTens;
    private Typeface mOriginalHoursTypeface;
    private Typeface mOriginalMinutesTypeface;
    private TextView mSeconds;
    private final int mWhiteColor;

    public TimerView(Context context) {
        this(context, null);
    }

    public TimerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mAndroidClockMonoThin = Typeface.createFromAsset(context.getAssets(), "fonts/AndroidClockMono-Thin.ttf");
        Resources resources = context.getResources();
        this.mWhiteColor = resources.getColor(R.color.clock_white);
        this.mGrayColor = resources.getColor(R.color.clock_gray);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mHoursOnes = (TextView) findViewById(R.id.hours_ones);
        if (this.mHoursOnes != null) {
            this.mOriginalHoursTypeface = this.mHoursOnes.getTypeface();
        }
        this.mMinutesTens = (TextView) findViewById(R.id.minutes_tens);
        if (this.mHoursOnes != null && this.mMinutesTens != null) {
            addStartPadding(this.mMinutesTens);
        }
        this.mMinutesOnes = (TextView) findViewById(R.id.minutes_ones);
        if (this.mMinutesOnes != null) {
            this.mOriginalMinutesTypeface = this.mMinutesOnes.getTypeface();
        }
        this.mSeconds = (TextView) findViewById(R.id.seconds);
        if (this.mSeconds != null) {
            addStartPadding(this.mSeconds);
        }
    }

    private void addStartPadding(TextView textView) {
        String allDigits = String.format("%010d", 123456789);
        Paint paint = new Paint(1);
        paint.setTextSize(textView.getTextSize());
        paint.setTypeface(textView.getTypeface());
        float[] widths = new float[allDigits.length()];
        int ll = paint.getTextWidths(allDigits, widths);
        int largest = 0;
        for (int ii = 1; ii < ll; ii++) {
            if (widths[ii] > widths[largest]) {
                largest = ii;
            }
        }
        textView.setPadding((int) (0.45f * widths[largest]), 0, 0, 0);
    }

    public void setTime(int hoursOnesDigit, int minutesTensDigit, int minutesOnesDigit, int seconds) {
        if (this.mHoursOnes != null) {
            if (hoursOnesDigit == -1) {
                this.mHoursOnes.setText("-");
                this.mHoursOnes.setTypeface(this.mAndroidClockMonoThin);
                this.mHoursOnes.setTextColor(this.mGrayColor);
            } else {
                this.mHoursOnes.setText(String.format("%d", Integer.valueOf(hoursOnesDigit)));
                this.mHoursOnes.setTypeface(this.mOriginalHoursTypeface);
                this.mHoursOnes.setTextColor(this.mWhiteColor);
            }
        }
        if (this.mMinutesTens != null) {
            if (minutesTensDigit == -1) {
                this.mMinutesTens.setText("-");
                this.mMinutesTens.setTypeface(this.mAndroidClockMonoThin);
                this.mMinutesTens.setTextColor(this.mGrayColor);
            } else {
                this.mMinutesTens.setText(String.format("%d", Integer.valueOf(minutesTensDigit)));
                this.mMinutesTens.setTypeface(this.mOriginalMinutesTypeface);
                this.mMinutesTens.setTextColor(this.mWhiteColor);
            }
        }
        if (this.mMinutesOnes != null) {
            if (minutesOnesDigit == -1) {
                this.mMinutesOnes.setText("-");
                this.mMinutesOnes.setTypeface(this.mAndroidClockMonoThin);
                this.mMinutesOnes.setTextColor(this.mGrayColor);
            } else {
                this.mMinutesOnes.setText(String.format("%d", Integer.valueOf(minutesOnesDigit)));
                this.mMinutesOnes.setTypeface(this.mOriginalMinutesTypeface);
                this.mMinutesOnes.setTextColor(this.mWhiteColor);
            }
        }
        if (this.mSeconds != null) {
            this.mSeconds.setText(String.format("%02d", Integer.valueOf(seconds)));
        }
    }
}
