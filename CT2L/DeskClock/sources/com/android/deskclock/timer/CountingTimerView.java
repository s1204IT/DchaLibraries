package com.android.deskclock.timer;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.Utils;

public class CountingTimerView extends View {
    private final int mAccentColor;
    private final AccessibilityManager mAccessibilityManager;
    private final float mBigFontSize;
    private final SignedTime mBigHours;
    private final SignedTime mBigMinutes;
    private final UnsignedTime mBigSeconds;
    Runnable mBlinkThread;
    private int mDefaultColor;
    private String mHours;
    private String mHundredths;
    private final Hundredths mMedHundredths;
    private String mMinutes;
    private final Paint mPaintBigThin;
    private final Paint mPaintMed;
    private final int mPressedColor;
    private final float mRadiusOffset;
    private boolean mRemeasureText;
    private String mSeconds;
    private boolean mShowTimeStr;
    private final float mSmallFontSize;
    private float mTextHeight;
    private float mTotalTextWidth;
    private boolean mVirtualButtonEnabled;
    private boolean mVirtualButtonPressedOn;
    private final int mWhiteColor;

    static class UnsignedTime {
        protected float mEm;
        private float mLabelWidth;
        protected Paint mPaint;
        protected final float mSpacingRatio;
        private final String mWidest;
        protected float mWidth;

        public UnsignedTime(Paint paint, float spacingRatio, String allDigits) {
            this.mWidth = 0.0f;
            this.mLabelWidth = 0.0f;
            this.mPaint = paint;
            this.mSpacingRatio = spacingRatio;
            if (TextUtils.isEmpty(allDigits)) {
                LogUtils.wtf("Locale digits missing - using English", new Object[0]);
                allDigits = "0123456789";
            }
            float[] widths = new float[allDigits.length()];
            int ll = this.mPaint.getTextWidths(allDigits, widths);
            int largest = 0;
            for (int ii = 1; ii < ll; ii++) {
                if (widths[ii] > widths[largest]) {
                    largest = ii;
                }
            }
            this.mEm = widths[largest];
            this.mWidest = allDigits.substring(largest, largest + 1);
        }

        public UnsignedTime(UnsignedTime unsignedTime, float spacingRatio) {
            this.mWidth = 0.0f;
            this.mLabelWidth = 0.0f;
            this.mPaint = unsignedTime.mPaint;
            this.mEm = unsignedTime.mEm;
            this.mWidth = unsignedTime.mWidth;
            this.mWidest = unsignedTime.mWidest;
            this.mSpacingRatio = spacingRatio;
        }

        protected void updateWidth(String time) {
            this.mEm = this.mPaint.measureText(this.mWidest);
            this.mLabelWidth = this.mSpacingRatio * this.mEm;
            this.mWidth = time.length() * this.mEm;
        }

        protected void resetWidth() {
            this.mLabelWidth = 0.0f;
            this.mWidth = 0.0f;
        }

        public float calcTotalWidth(String time) {
            if (time != null) {
                updateWidth(time);
                return this.mWidth + this.mLabelWidth;
            }
            resetWidth();
            return 0.0f;
        }

        public float getLabelWidth() {
            return this.mLabelWidth;
        }

        protected float drawTime(Canvas canvas, String time, int ii, float x, float y) {
            float textEm = this.mEm / 2.0f;
            while (ii < time.length()) {
                float x2 = x + textEm;
                canvas.drawText(time.substring(ii, ii + 1), x2, y, this.mPaint);
                x = x2 + textEm;
                ii++;
            }
            return x;
        }

        public float draw(Canvas canvas, String time, float x, float y) {
            return drawTime(canvas, time, 0, x, y) + getLabelWidth();
        }
    }

    static class Hundredths extends UnsignedTime {
        public Hundredths(Paint paint, float spacingRatio, String allDigits) {
            super(paint, spacingRatio, allDigits);
        }

        @Override
        public float draw(Canvas canvas, String time, float x, float y) {
            return drawTime(canvas, time, 0, x + getLabelWidth(), y);
        }
    }

    static class SignedTime extends UnsignedTime {
        private float mMinusWidth;

        public SignedTime(UnsignedTime unsignedTime, float spacingRatio) {
            super(unsignedTime, spacingRatio);
            this.mMinusWidth = 0.0f;
        }

        @Override
        protected void updateWidth(String time) {
            super.updateWidth(time);
            if (time.contains("-")) {
                this.mMinusWidth = this.mPaint.measureText("-");
                this.mWidth += this.mMinusWidth - this.mEm;
            } else {
                this.mMinusWidth = 0.0f;
            }
        }

        @Override
        protected void resetWidth() {
            super.resetWidth();
            this.mMinusWidth = 0.0f;
        }

        @Override
        public float draw(Canvas canvas, String time, float x, float y) {
            int ii = 0;
            if (this.mMinusWidth != 0.0f) {
                float minusWidth = this.mMinusWidth / 2.0f;
                float x2 = x + minusWidth;
                canvas.drawText(time.substring(0, 1), x2, y, this.mPaint);
                x = x2 + minusWidth;
                ii = 0 + 1;
            }
            return drawTime(canvas, time, ii, x, y) + getLabelWidth();
        }
    }

    public CountingTimerView(Context context) {
        this(context, null);
    }

    public CountingTimerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mShowTimeStr = true;
        this.mPaintBigThin = new Paint();
        this.mPaintMed = new Paint();
        this.mTextHeight = 0.0f;
        this.mRemeasureText = true;
        this.mVirtualButtonEnabled = false;
        this.mVirtualButtonPressedOn = false;
        this.mBlinkThread = new Runnable() {
            private boolean mVisible = true;

            @Override
            public void run() {
                this.mVisible = !this.mVisible;
                CountingTimerView.this.showTime(this.mVisible);
                CountingTimerView.this.postDelayed(CountingTimerView.this.mBlinkThread, 500L);
            }
        };
        this.mAccessibilityManager = (AccessibilityManager) context.getSystemService("accessibility");
        Resources r = context.getResources();
        this.mWhiteColor = r.getColor(R.color.clock_white);
        this.mDefaultColor = this.mWhiteColor;
        this.mPressedColor = r.getColor(R.color.hot_pink);
        this.mAccentColor = r.getColor(R.color.hot_pink);
        this.mBigFontSize = r.getDimension(R.dimen.big_font_size);
        this.mSmallFontSize = r.getDimension(R.dimen.small_font_size);
        Typeface androidClockMonoThin = Typeface.createFromAsset(context.getAssets(), "fonts/AndroidClockMono-Thin.ttf");
        this.mPaintBigThin.setAntiAlias(true);
        this.mPaintBigThin.setStyle(Paint.Style.STROKE);
        this.mPaintBigThin.setTextAlign(Paint.Align.CENTER);
        this.mPaintBigThin.setTypeface(androidClockMonoThin);
        Typeface androidClockMonoLight = Typeface.createFromAsset(context.getAssets(), "fonts/AndroidClockMono-Light.ttf");
        this.mPaintMed.setAntiAlias(true);
        this.mPaintMed.setStyle(Paint.Style.STROKE);
        this.mPaintMed.setTextAlign(Paint.Align.CENTER);
        this.mPaintMed.setTypeface(androidClockMonoLight);
        resetTextSize();
        setTextColor(this.mDefaultColor);
        String allDigits = String.format("%010d", 123456789);
        this.mBigSeconds = new UnsignedTime(this.mPaintBigThin, 0.0f, allDigits);
        this.mBigHours = new SignedTime(this.mBigSeconds, 0.4f);
        this.mBigMinutes = new SignedTime(this.mBigSeconds, 0.4f);
        this.mMedHundredths = new Hundredths(this.mPaintMed, 0.5f, allDigits);
        this.mRadiusOffset = Utils.calculateRadiusOffset(r);
    }

    protected void resetTextSize() {
        this.mTextHeight = this.mBigFontSize;
        this.mPaintBigThin.setTextSize(this.mBigFontSize);
        this.mPaintMed.setTextSize(this.mSmallFontSize);
    }

    protected void setTextColor(int textColor) {
        this.mPaintBigThin.setColor(textColor);
        this.mPaintMed.setColor(textColor);
    }

    public void setTime(long time, boolean showHundredths, boolean update) {
        int oldLength = getDigitsLength();
        boolean neg = false;
        boolean showNeg = false;
        if (time < 0) {
            time = -time;
            showNeg = true;
            neg = true;
        }
        long seconds = time / 1000;
        long hundreds = (time - (1000 * seconds)) / 10;
        long minutes = seconds / 60;
        long seconds2 = seconds - (60 * minutes);
        long hours = minutes / 60;
        long minutes2 = minutes - (60 * hours);
        if (hours > 999) {
            hours = 0;
        }
        if (hours == 0 && minutes2 == 0 && seconds2 == 0) {
            showNeg = false;
        }
        if (!showHundredths) {
            if (!neg && hundreds != 0) {
                seconds2++;
                if (seconds2 == 60) {
                    seconds2 = 0;
                    minutes2++;
                    if (minutes2 == 60) {
                        minutes2 = 0;
                        hours++;
                    }
                }
            }
            if (hundreds < 10 || hundreds > 90) {
                update = true;
            }
        }
        if (hours >= 10) {
            String format = showNeg ? "-%02d" : "%02d";
            this.mHours = String.format(format, Long.valueOf(hours));
        } else if (hours > 0) {
            String format2 = showNeg ? "-%01d" : "%01d";
            this.mHours = String.format(format2, Long.valueOf(hours));
        } else {
            this.mHours = null;
        }
        if (minutes2 >= 10 || hours > 0) {
            String format3 = (showNeg && hours == 0) ? "-%02d" : "%02d";
            this.mMinutes = String.format(format3, Long.valueOf(minutes2));
        } else {
            String format4 = (showNeg && hours == 0) ? "-%01d" : "%01d";
            this.mMinutes = String.format(format4, Long.valueOf(minutes2));
        }
        this.mSeconds = String.format("%02d", Long.valueOf(seconds2));
        if (showHundredths) {
            this.mHundredths = String.format("%02d", Long.valueOf(hundreds));
        } else {
            this.mHundredths = null;
        }
        int newLength = getDigitsLength();
        if (oldLength != newLength) {
            if (oldLength > newLength) {
                resetTextSize();
            }
            this.mRemeasureText = true;
        }
        if (update) {
            setContentDescription(getTimeStringForAccessibility((int) hours, (int) minutes2, (int) seconds2, showNeg, getResources()));
            invalidate();
        }
    }

    private int getDigitsLength() {
        return (this.mSeconds == null ? 0 : this.mSeconds.length()) + (this.mMinutes == null ? 0 : this.mMinutes.length()) + (this.mHours == null ? 0 : this.mHours.length()) + (this.mHundredths != null ? this.mHundredths.length() : 0);
    }

    private void calcTotalTextWidth() {
        this.mTotalTextWidth = this.mBigHours.calcTotalWidth(this.mHours) + this.mBigMinutes.calcTotalWidth(this.mMinutes) + this.mBigSeconds.calcTotalWidth(this.mSeconds) + this.mMedHundredths.calcTotalWidth(this.mHundredths);
    }

    private void setTotalTextWidth() {
        calcTotalTextWidth();
        int width = Math.min(getWidth(), getHeight());
        if (width != 0) {
            int width2 = width - ((int) ((4.0f * this.mRadiusOffset) + 0.5f));
            float wantDiameter2 = 0.85f * width2 * width2;
            float totalDiameter2 = getHypotenuseSquared();
            while (totalDiameter2 > wantDiameter2) {
                float sizeRatio = 0.99f * ((float) Math.sqrt(wantDiameter2 / totalDiameter2));
                this.mPaintBigThin.setTextSize(this.mPaintBigThin.getTextSize() * sizeRatio);
                this.mPaintMed.setTextSize(this.mPaintMed.getTextSize() * sizeRatio);
                this.mTextHeight = this.mPaintBigThin.getTextSize();
                calcTotalTextWidth();
                totalDiameter2 = getHypotenuseSquared();
            }
        }
    }

    private float getHypotenuseSquared() {
        return (this.mTotalTextWidth * this.mTotalTextWidth) + (this.mTextHeight * this.mTextHeight);
    }

    public void blinkTimeStr(boolean blink) {
        if (blink) {
            removeCallbacks(this.mBlinkThread);
            post(this.mBlinkThread);
        } else {
            removeCallbacks(this.mBlinkThread);
            showTime(true);
        }
    }

    public void showTime(boolean visible) {
        this.mShowTimeStr = visible;
        invalidate();
    }

    public void setTimeStrTextColor(boolean active, boolean forceUpdate) {
        this.mDefaultColor = active ? this.mAccentColor : this.mWhiteColor;
        setTextColor(this.mDefaultColor);
        if (forceUpdate) {
            invalidate();
        }
    }

    public String getTimeString() {
        if (this.mHundredths == null) {
            if (this.mHours == null) {
                return String.format("%s:%s", this.mMinutes, this.mSeconds);
            }
            return String.format("%s:%s:%s", this.mHours, this.mMinutes, this.mSeconds);
        }
        if (this.mHours == null) {
            return String.format("%s:%s.%s", this.mMinutes, this.mSeconds, this.mHundredths);
        }
        return String.format("%s:%s:%s.%s", this.mHours, this.mMinutes, this.mSeconds, this.mHundredths);
    }

    private static String getTimeStringForAccessibility(int hours, int minutes, int seconds, boolean showNeg, Resources r) {
        StringBuilder s = new StringBuilder();
        if (showNeg) {
            s.append("-");
        }
        if (showNeg && hours == 0 && minutes == 0) {
            s.append(String.format(r.getQuantityText(R.plurals.Nseconds_description, seconds).toString(), Integer.valueOf(seconds)));
        } else if (hours == 0) {
            s.append(String.format(r.getQuantityText(R.plurals.Nminutes_description, minutes).toString(), Integer.valueOf(minutes)));
            s.append(" ");
            s.append(String.format(r.getQuantityText(R.plurals.Nseconds_description, seconds).toString(), Integer.valueOf(seconds)));
        } else {
            s.append(String.format(r.getQuantityText(R.plurals.Nhours_description, hours).toString(), Integer.valueOf(hours)));
            s.append(" ");
            s.append(String.format(r.getQuantityText(R.plurals.Nminutes_description, minutes).toString(), Integer.valueOf(minutes)));
            s.append(" ");
            s.append(String.format(r.getQuantityText(R.plurals.Nseconds_description, seconds).toString(), Integer.valueOf(seconds)));
        }
        return s.toString();
    }

    public void setVirtualButtonEnabled(boolean enabled) {
        this.mVirtualButtonEnabled = enabled;
    }

    private void virtualButtonPressed(boolean pressedOn) {
        this.mVirtualButtonPressedOn = pressedOn;
        invalidate();
    }

    private boolean withinVirtualButtonBounds(float x, float y) {
        int width = getWidth();
        int height = getHeight();
        float centerX = width / 2;
        float centerY = height / 2;
        float radius = Math.min(width, height) / 2;
        double distance = Math.sqrt(Math.pow(centerX - x, 2.0d) + Math.pow(centerY - y, 2.0d));
        return distance < ((double) radius);
    }

    public void registerVirtualButtonAction(final Runnable runnable) {
        if (!this.mAccessibilityManager.isEnabled()) {
            setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (CountingTimerView.this.mVirtualButtonEnabled) {
                        switch (event.getAction()) {
                            case 0:
                                if (CountingTimerView.this.withinVirtualButtonBounds(event.getX(), event.getY())) {
                                    CountingTimerView.this.virtualButtonPressed(true);
                                    return true;
                                }
                                CountingTimerView.this.virtualButtonPressed(false);
                                return false;
                            case 1:
                                CountingTimerView.this.virtualButtonPressed(false);
                                if (!CountingTimerView.this.withinVirtualButtonBounds(event.getX(), event.getY())) {
                                    return true;
                                }
                                runnable.run();
                                return true;
                            case 3:
                                CountingTimerView.this.virtualButtonPressed(false);
                                return true;
                            case 4:
                                CountingTimerView.this.virtualButtonPressed(false);
                                return false;
                        }
                    }
                    return false;
                }
            });
        } else {
            setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    runnable.run();
                }
            });
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (this.mShowTimeStr || this.mVirtualButtonPressedOn) {
            int width = getWidth();
            if (this.mRemeasureText && width != 0) {
                setTotalTextWidth();
                width = getWidth();
                this.mRemeasureText = false;
            }
            int xCenter = width / 2;
            int yCenter = getHeight() / 2;
            float xTextStart = xCenter - (this.mTotalTextWidth / 2.0f);
            float yTextStart = (yCenter + (this.mTextHeight / 2.0f)) - (this.mTextHeight * 0.14f);
            int textColor = this.mVirtualButtonPressedOn ? this.mPressedColor : this.mDefaultColor;
            this.mPaintBigThin.setColor(textColor);
            this.mPaintMed.setColor(textColor);
            if (this.mHours != null) {
                xTextStart = this.mBigHours.draw(canvas, this.mHours, xTextStart, yTextStart);
            }
            if (this.mMinutes != null) {
                xTextStart = this.mBigMinutes.draw(canvas, this.mMinutes, xTextStart, yTextStart);
            }
            if (this.mSeconds != null) {
                xTextStart = this.mBigSeconds.draw(canvas, this.mSeconds, xTextStart, yTextStart);
            }
            if (this.mHundredths != null) {
                this.mMedHundredths.draw(canvas, this.mHundredths, xTextStart, yTextStart);
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.mRemeasureText = true;
        resetTextSize();
    }
}
