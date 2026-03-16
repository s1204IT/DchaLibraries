package android.widget;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.Keyframe;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.IntArray;
import android.util.Log;
import android.util.MathUtils;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import com.android.ims.ImsReasonInfo;
import com.android.internal.R;
import com.android.internal.widget.ExploreByTouchHelper;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class RadialTimePickerView extends View implements View.OnTouchListener {
    private static final int ALPHA_OPAQUE = 255;
    private static final int ALPHA_SELECTOR = 60;
    private static final int ALPHA_TRANSPARENT = 0;
    private static final int AM = 0;
    private static final int CENTER_RADIUS = 2;
    private static final boolean DEBUG = false;
    private static final int DEBUG_COLOR = 553582592;
    private static final int DEBUG_STROKE_WIDTH = 2;
    private static final int DEBUG_TEXT_COLOR = 1627324416;
    private static final int DEGREES_FOR_ONE_HOUR = 30;
    private static final int DEGREES_FOR_ONE_MINUTE = 6;
    private static final int HOURS = 0;
    private static final int HOURS_INNER = 2;
    private static final int MINUTES = 1;
    private static final int PM = 1;
    private static final int SELECTOR_CIRCLE = 0;
    private static final int SELECTOR_DOT = 1;
    private static final int SELECTOR_LINE = 2;
    private static final String TAG = "ClockView";
    private final IntHolder[] mAlpha;
    private final IntHolder[][] mAlphaSelector;
    private int mAmOrPm;
    private final float[] mAnimationRadiusMultiplier;
    boolean mChangedDuringTouch;
    private final float[] mCircleRadius;
    private final float[] mCircleRadiusMultiplier;
    private final int[] mColor;
    private final int[][] mColorSelector;
    private int mDisabledAlpha;
    private int mHalfwayHypotenusePoint;
    private final String[] mHours12Texts;
    private final ArrayList<Animator> mHoursToMinutesAnims;
    private final String[] mInnerHours24Texts;
    private final float[] mInnerTextGridHeights;
    private final float[] mInnerTextGridWidths;
    private String[] mInnerTextHours;
    private float mInnerTextSize;
    private boolean mInputEnabled;
    private final InvalidateUpdateListener mInvalidateUpdateListener;
    private boolean mIs24HourMode;
    private boolean mIsOnInnerCircle;
    private final int[] mLineLength;
    private OnValueSelectedListener mListener;
    private int mMaxHypotenuseForOuterNumber;
    private int mMinHypotenuseForInnerNumber;
    private final ArrayList<Animator> mMinuteToHoursAnims;
    private final String[] mMinutesTexts;
    private final float[] mNumbersRadiusMultiplier;
    private final String[] mOuterHours24Texts;
    private String[] mOuterTextHours;
    private String[] mOuterTextMinutes;
    private final Paint[] mPaint;
    private final Paint mPaintBackground;
    private final Paint mPaintCenter;
    private final Paint mPaintDebug;
    private final Paint[][] mPaintSelector;
    private final int[] mSelectionDegrees;
    private final int[] mSelectionRadius;
    private final float mSelectionRadiusMultiplier;
    private boolean mShowHours;
    private final float[][] mTextGridHeights;
    private final float[][] mTextGridWidths;
    private final float[] mTextSize;
    private final float[] mTextSizeMultiplier;
    private final RadialPickerTouchHelper mTouchHelper;
    private AnimatorSet mTransition;
    private final float mTransitionEndRadiusMultiplier;
    private final float mTransitionMidRadiusMultiplier;
    private final Typeface mTypeface;
    private int mXCenter;
    private int mYCenter;
    private static final float SINE_30_DEGREES = 0.5f;
    private static final float COSINE_30_DEGREES = ((float) Math.sqrt(3.0d)) * SINE_30_DEGREES;
    private static final int[] HOURS_NUMBERS = {12, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
    private static final int[] HOURS_NUMBERS_24 = {0, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23};
    private static final int[] MINUTES_NUMBERS = {0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55};
    private static int[] sSnapPrefer30sMap = new int[ImsReasonInfo.CODE_SIP_USER_REJECTED];

    public interface OnValueSelectedListener {
        void onValueSelected(int i, int i2, boolean z);
    }

    static {
        preparePrefer30sMap();
    }

    private static void preparePrefer30sMap() {
        int snappedOutputDegrees = 0;
        int count = 1;
        int expectedCount = 8;
        for (int degrees = 0; degrees < 361; degrees++) {
            sSnapPrefer30sMap[degrees] = snappedOutputDegrees;
            if (count == expectedCount) {
                snappedOutputDegrees += 6;
                if (snappedOutputDegrees == 360) {
                    expectedCount = 7;
                } else if (snappedOutputDegrees % 30 == 0) {
                    expectedCount = 14;
                } else {
                    expectedCount = 4;
                }
                count = 1;
            } else {
                count++;
            }
        }
    }

    private static int snapPrefer30s(int degrees) {
        if (sSnapPrefer30sMap == null) {
            return -1;
        }
        return sSnapPrefer30sMap[degrees];
    }

    private static int snapOnly30s(int degrees, int forceHigherOrLower) {
        int floor = (degrees / 30) * 30;
        int ceiling = floor + 30;
        if (forceHigherOrLower == 1) {
            return ceiling;
        }
        if (forceHigherOrLower == -1) {
            if (degrees == floor) {
                floor -= 30;
            }
            return floor;
        }
        if (degrees - floor < ceiling - degrees) {
            return floor;
        }
        return ceiling;
    }

    public RadialTimePickerView(Context context) {
        this(context, null);
    }

    public RadialTimePickerView(Context context, AttributeSet attrs) {
        this(context, attrs, 16843933);
    }

    public RadialTimePickerView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public RadialTimePickerView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs);
        this.mInvalidateUpdateListener = new InvalidateUpdateListener();
        this.mHours12Texts = new String[12];
        this.mOuterHours24Texts = new String[12];
        this.mInnerHours24Texts = new String[12];
        this.mMinutesTexts = new String[12];
        this.mPaint = new Paint[2];
        this.mColor = new int[2];
        this.mAlpha = new IntHolder[2];
        this.mPaintCenter = new Paint();
        this.mPaintSelector = (Paint[][]) Array.newInstance((Class<?>) Paint.class, 2, 3);
        this.mColorSelector = (int[][]) Array.newInstance((Class<?>) Integer.TYPE, 2, 3);
        this.mAlphaSelector = (IntHolder[][]) Array.newInstance((Class<?>) IntHolder.class, 2, 3);
        this.mPaintBackground = new Paint();
        this.mPaintDebug = new Paint();
        this.mCircleRadius = new float[3];
        this.mTextSize = new float[2];
        this.mTextGridHeights = (float[][]) Array.newInstance((Class<?>) Float.TYPE, 2, 7);
        this.mTextGridWidths = (float[][]) Array.newInstance((Class<?>) Float.TYPE, 2, 7);
        this.mInnerTextGridHeights = new float[7];
        this.mInnerTextGridWidths = new float[7];
        this.mCircleRadiusMultiplier = new float[2];
        this.mNumbersRadiusMultiplier = new float[3];
        this.mTextSizeMultiplier = new float[3];
        this.mAnimationRadiusMultiplier = new float[3];
        this.mLineLength = new int[3];
        this.mSelectionRadius = new int[3];
        this.mSelectionDegrees = new int[3];
        this.mHoursToMinutesAnims = new ArrayList<>();
        this.mMinuteToHoursAnims = new ArrayList<>();
        this.mInputEnabled = true;
        this.mChangedDuringTouch = false;
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(16842803, outValue, true);
        this.mDisabledAlpha = (int) ((outValue.getFloat() * 255.0f) + SINE_30_DEGREES);
        Resources res = getResources();
        TypedArray a = this.mContext.obtainStyledAttributes(attrs, R.styleable.TimePicker, defStyleAttr, defStyleRes);
        this.mTypeface = Typeface.create("sans-serif", 0);
        for (int i = 0; i < this.mAlpha.length; i++) {
            this.mAlpha[i] = new IntHolder(255);
        }
        for (int i2 = 0; i2 < this.mAlphaSelector.length; i2++) {
            for (int j = 0; j < this.mAlphaSelector[i2].length; j++) {
                this.mAlphaSelector[i2][j] = new IntHolder(255);
            }
        }
        int numbersTextColor = a.getColor(3, res.getColor(R.color.timepicker_default_text_color_material));
        this.mPaint[0] = new Paint();
        this.mPaint[0].setAntiAlias(true);
        this.mPaint[0].setTextAlign(Paint.Align.CENTER);
        this.mColor[0] = numbersTextColor;
        this.mPaint[1] = new Paint();
        this.mPaint[1].setAntiAlias(true);
        this.mPaint[1].setTextAlign(Paint.Align.CENTER);
        this.mColor[1] = numbersTextColor;
        this.mPaintCenter.setColor(numbersTextColor);
        this.mPaintCenter.setAntiAlias(true);
        this.mPaintCenter.setTextAlign(Paint.Align.CENTER);
        this.mPaintSelector[0][0] = new Paint();
        this.mPaintSelector[0][0].setAntiAlias(true);
        this.mColorSelector[0][0] = a.getColor(5, R.color.timepicker_default_selector_color_material);
        this.mPaintSelector[0][1] = new Paint();
        this.mPaintSelector[0][1].setAntiAlias(true);
        this.mColorSelector[0][1] = a.getColor(5, R.color.timepicker_default_selector_color_material);
        this.mPaintSelector[0][2] = new Paint();
        this.mPaintSelector[0][2].setAntiAlias(true);
        this.mPaintSelector[0][2].setStrokeWidth(2.0f);
        this.mColorSelector[0][2] = a.getColor(5, R.color.timepicker_default_selector_color_material);
        this.mPaintSelector[1][0] = new Paint();
        this.mPaintSelector[1][0].setAntiAlias(true);
        this.mColorSelector[1][0] = a.getColor(5, R.color.timepicker_default_selector_color_material);
        this.mPaintSelector[1][1] = new Paint();
        this.mPaintSelector[1][1].setAntiAlias(true);
        this.mColorSelector[1][1] = a.getColor(5, R.color.timepicker_default_selector_color_material);
        this.mPaintSelector[1][2] = new Paint();
        this.mPaintSelector[1][2].setAntiAlias(true);
        this.mPaintSelector[1][2].setStrokeWidth(2.0f);
        this.mColorSelector[1][2] = a.getColor(5, R.color.timepicker_default_selector_color_material);
        this.mPaintBackground.setColor(a.getColor(4, res.getColor(R.color.timepicker_default_numbers_background_color_material)));
        this.mPaintBackground.setAntiAlias(true);
        this.mShowHours = true;
        this.mIs24HourMode = false;
        this.mAmOrPm = 0;
        this.mTouchHelper = new RadialPickerTouchHelper();
        setAccessibilityDelegate(this.mTouchHelper);
        if (getImportantForAccessibility() == 0) {
            setImportantForAccessibility(1);
        }
        initHoursAndMinutesText();
        initData();
        this.mTransitionMidRadiusMultiplier = Float.parseFloat(res.getString(R.string.timepicker_transition_mid_radius_multiplier));
        this.mTransitionEndRadiusMultiplier = Float.parseFloat(res.getString(R.string.timepicker_transition_end_radius_multiplier));
        this.mTextGridHeights[0] = new float[7];
        this.mTextGridHeights[1] = new float[7];
        this.mSelectionRadiusMultiplier = Float.parseFloat(res.getString(R.string.timepicker_selection_radius_multiplier));
        a.recycle();
        setOnTouchListener(this);
        setClickable(true);
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        int currentHour = calendar.get(11);
        int currentMinute = calendar.get(12);
        setCurrentHourInternal(currentHour, false, false);
        setCurrentMinuteInternal(currentMinute, false);
        setHapticFeedbackEnabled(true);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredWidth = View.MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int measuredHeight = View.MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
        int minDimension = Math.min(measuredWidth, measuredHeight);
        super.onMeasure(View.MeasureSpec.makeMeasureSpec(minDimension, widthMode), View.MeasureSpec.makeMeasureSpec(minDimension, heightMode));
    }

    public void initialize(int hour, int minute, boolean is24HourMode) {
        if (this.mIs24HourMode != is24HourMode) {
            this.mIs24HourMode = is24HourMode;
            initData();
        }
        setCurrentHourInternal(hour, false, false);
        setCurrentMinuteInternal(minute, false);
    }

    public void setCurrentItemShowing(int item, boolean animate) {
        switch (item) {
            case 0:
                showHours(animate);
                break;
            case 1:
                showMinutes(animate);
                break;
            default:
                Log.e(TAG, "ClockView does not support showing item " + item);
                break;
        }
    }

    public int getCurrentItemShowing() {
        return this.mShowHours ? 0 : 1;
    }

    public void setOnValueSelectedListener(OnValueSelectedListener listener) {
        this.mListener = listener;
    }

    public void setCurrentHour(int hour) {
        setCurrentHourInternal(hour, true, false);
    }

    private void setCurrentHourInternal(int hour, boolean callback, boolean autoAdvance) {
        int degrees = (hour % 12) * 30;
        this.mSelectionDegrees[0] = degrees;
        this.mSelectionDegrees[2] = degrees;
        int amOrPm = (hour == 0 || hour % 24 < 12) ? 0 : 1;
        boolean isOnInnerCircle = this.mIs24HourMode && hour >= 1 && hour <= 12;
        if (this.mAmOrPm != amOrPm || this.mIsOnInnerCircle != isOnInnerCircle) {
            this.mAmOrPm = amOrPm;
            this.mIsOnInnerCircle = isOnInnerCircle;
            initData();
            updateLayoutData();
            this.mTouchHelper.invalidateRoot();
        }
        invalidate();
        if (callback && this.mListener != null) {
            this.mListener.onValueSelected(0, hour, autoAdvance);
        }
    }

    public int getCurrentHour() {
        return getHourForDegrees(this.mSelectionDegrees[this.mIsOnInnerCircle ? (char) 2 : (char) 0], this.mIsOnInnerCircle);
    }

    private int getHourForDegrees(int degrees, boolean innerCircle) {
        int hour = (degrees / 30) % 12;
        if (this.mIs24HourMode) {
            if (innerCircle && hour == 0) {
                return 12;
            }
            if (!innerCircle && hour != 0) {
                return hour + 12;
            }
            return hour;
        }
        if (this.mAmOrPm == 1) {
            return hour + 12;
        }
        return hour;
    }

    private int getDegreesForHour(int hour) {
        if (this.mIs24HourMode) {
            if (hour >= 12) {
                hour -= 12;
            }
        } else if (hour == 12) {
            hour = 0;
        }
        return hour * 30;
    }

    public void setCurrentMinute(int minute) {
        setCurrentMinuteInternal(minute, true);
    }

    private void setCurrentMinuteInternal(int minute, boolean callback) {
        this.mSelectionDegrees[1] = (minute % 60) * 6;
        invalidate();
        if (callback && this.mListener != null) {
            this.mListener.onValueSelected(1, minute, false);
        }
    }

    public int getCurrentMinute() {
        return getMinuteForDegrees(this.mSelectionDegrees[1]);
    }

    private int getMinuteForDegrees(int degrees) {
        return degrees / 6;
    }

    private int getDegreesForMinute(int minute) {
        return minute * 6;
    }

    public void setAmOrPm(int val) {
        this.mAmOrPm = val % 2;
        invalidate();
        this.mTouchHelper.invalidateRoot();
    }

    public int getAmOrPm() {
        return this.mAmOrPm;
    }

    public void showHours(boolean animate) {
        if (!this.mShowHours) {
            this.mShowHours = true;
            if (animate) {
                startMinutesToHoursAnimation();
            }
            initData();
            updateLayoutData();
            invalidate();
        }
    }

    public void showMinutes(boolean animate) {
        if (this.mShowHours) {
            this.mShowHours = false;
            if (animate) {
                startHoursToMinutesAnimation();
            }
            initData();
            updateLayoutData();
            invalidate();
        }
    }

    private void initHoursAndMinutesText() {
        for (int i = 0; i < 12; i++) {
            this.mHours12Texts[i] = String.format("%d", Integer.valueOf(HOURS_NUMBERS[i]));
            this.mOuterHours24Texts[i] = String.format("%02d", Integer.valueOf(HOURS_NUMBERS_24[i]));
            this.mInnerHours24Texts[i] = String.format("%d", Integer.valueOf(HOURS_NUMBERS[i]));
            this.mMinutesTexts[i] = String.format("%02d", Integer.valueOf(MINUTES_NUMBERS[i]));
        }
    }

    private void initData() {
        if (this.mIs24HourMode) {
            this.mOuterTextHours = this.mOuterHours24Texts;
            this.mInnerTextHours = this.mInnerHours24Texts;
        } else {
            this.mOuterTextHours = this.mHours12Texts;
            this.mInnerTextHours = null;
        }
        this.mOuterTextMinutes = this.mMinutesTexts;
        Resources res = getResources();
        if (this.mShowHours) {
            if (this.mIs24HourMode) {
                this.mCircleRadiusMultiplier[0] = Float.parseFloat(res.getString(R.string.timepicker_circle_radius_multiplier_24HourMode));
                this.mNumbersRadiusMultiplier[0] = Float.parseFloat(res.getString(R.string.timepicker_numbers_radius_multiplier_outer));
                this.mTextSizeMultiplier[0] = Float.parseFloat(res.getString(R.string.timepicker_text_size_multiplier_outer));
                this.mNumbersRadiusMultiplier[2] = Float.parseFloat(res.getString(R.string.timepicker_numbers_radius_multiplier_inner));
                this.mTextSizeMultiplier[2] = Float.parseFloat(res.getString(R.string.timepicker_text_size_multiplier_inner));
            } else {
                this.mCircleRadiusMultiplier[0] = Float.parseFloat(res.getString(R.string.timepicker_circle_radius_multiplier));
                this.mNumbersRadiusMultiplier[0] = Float.parseFloat(res.getString(R.string.timepicker_numbers_radius_multiplier_normal));
                this.mTextSizeMultiplier[0] = Float.parseFloat(res.getString(R.string.timepicker_text_size_multiplier_normal));
            }
        } else {
            this.mCircleRadiusMultiplier[1] = Float.parseFloat(res.getString(R.string.timepicker_circle_radius_multiplier));
            this.mNumbersRadiusMultiplier[1] = Float.parseFloat(res.getString(R.string.timepicker_numbers_radius_multiplier_normal));
            this.mTextSizeMultiplier[1] = Float.parseFloat(res.getString(R.string.timepicker_text_size_multiplier_normal));
        }
        this.mAnimationRadiusMultiplier[0] = 1.0f;
        this.mAnimationRadiusMultiplier[2] = 1.0f;
        this.mAnimationRadiusMultiplier[1] = 1.0f;
        this.mAlpha[0].setValue(this.mShowHours ? 255 : 0);
        this.mAlpha[1].setValue(this.mShowHours ? 0 : 255);
        this.mAlphaSelector[0][0].setValue(this.mShowHours ? 60 : 0);
        this.mAlphaSelector[0][1].setValue(this.mShowHours ? 255 : 0);
        this.mAlphaSelector[0][2].setValue(this.mShowHours ? 60 : 0);
        this.mAlphaSelector[1][0].setValue(this.mShowHours ? 0 : 60);
        this.mAlphaSelector[1][1].setValue(this.mShowHours ? 0 : 255);
        this.mAlphaSelector[1][2].setValue(this.mShowHours ? 0 : 60);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        updateLayoutData();
    }

    private void updateLayoutData() {
        this.mXCenter = getWidth() / 2;
        this.mYCenter = getHeight() / 2;
        int min = Math.min(this.mXCenter, this.mYCenter);
        this.mCircleRadius[0] = min * this.mCircleRadiusMultiplier[0];
        this.mCircleRadius[2] = min * this.mCircleRadiusMultiplier[0];
        this.mCircleRadius[1] = min * this.mCircleRadiusMultiplier[1];
        this.mMinHypotenuseForInnerNumber = ((int) (this.mCircleRadius[0] * this.mNumbersRadiusMultiplier[2])) - this.mSelectionRadius[0];
        this.mMaxHypotenuseForOuterNumber = ((int) (this.mCircleRadius[0] * this.mNumbersRadiusMultiplier[0])) + this.mSelectionRadius[0];
        this.mHalfwayHypotenusePoint = (int) (this.mCircleRadius[0] * ((this.mNumbersRadiusMultiplier[0] + this.mNumbersRadiusMultiplier[2]) / 2.0f));
        this.mTextSize[0] = this.mCircleRadius[0] * this.mTextSizeMultiplier[0];
        this.mTextSize[1] = this.mCircleRadius[1] * this.mTextSizeMultiplier[1];
        if (this.mIs24HourMode) {
            this.mInnerTextSize = this.mCircleRadius[0] * this.mTextSizeMultiplier[2];
        }
        calculateGridSizesHours();
        calculateGridSizesMinutes();
        this.mSelectionRadius[0] = (int) (this.mCircleRadius[0] * this.mSelectionRadiusMultiplier);
        this.mSelectionRadius[2] = this.mSelectionRadius[0];
        this.mSelectionRadius[1] = (int) (this.mCircleRadius[1] * this.mSelectionRadiusMultiplier);
        this.mTouchHelper.invalidateRoot();
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (!this.mInputEnabled) {
            canvas.saveLayerAlpha(0.0f, 0.0f, getWidth(), getHeight(), this.mDisabledAlpha);
        } else {
            canvas.save();
        }
        calculateGridSizesHours();
        calculateGridSizesMinutes();
        drawCircleBackground(canvas);
        drawSelector(canvas);
        drawTextElements(canvas, this.mTextSize[0], this.mTypeface, this.mOuterTextHours, this.mTextGridWidths[0], this.mTextGridHeights[0], this.mPaint[0], this.mColor[0], this.mAlpha[0].getValue());
        if (this.mIs24HourMode && this.mInnerTextHours != null) {
            drawTextElements(canvas, this.mInnerTextSize, this.mTypeface, this.mInnerTextHours, this.mInnerTextGridWidths, this.mInnerTextGridHeights, this.mPaint[0], this.mColor[0], this.mAlpha[0].getValue());
        }
        drawTextElements(canvas, this.mTextSize[1], this.mTypeface, this.mOuterTextMinutes, this.mTextGridWidths[1], this.mTextGridHeights[1], this.mPaint[1], this.mColor[1], this.mAlpha[1].getValue());
        drawCenter(canvas);
        canvas.restore();
    }

    private void drawCircleBackground(Canvas canvas) {
        canvas.drawCircle(this.mXCenter, this.mYCenter, this.mCircleRadius[0], this.mPaintBackground);
    }

    private void drawCenter(Canvas canvas) {
        canvas.drawCircle(this.mXCenter, this.mYCenter, 2.0f, this.mPaintCenter);
    }

    private void drawSelector(Canvas canvas) {
        drawSelector(canvas, this.mIsOnInnerCircle ? 2 : 0);
        drawSelector(canvas, 1);
    }

    private int getMultipliedAlpha(int argb, int alpha) {
        return (int) ((((double) Color.alpha(argb)) * (((double) alpha) / 255.0d)) + 0.5d);
    }

    private void drawSelector(Canvas canvas, int index) {
        this.mLineLength[index] = (int) (this.mCircleRadius[index] * this.mNumbersRadiusMultiplier[index] * this.mAnimationRadiusMultiplier[index]);
        double selectionRadians = Math.toRadians(this.mSelectionDegrees[index]);
        int pointX = this.mXCenter + ((int) (((double) this.mLineLength[index]) * Math.sin(selectionRadians)));
        int pointY = this.mYCenter - ((int) (((double) this.mLineLength[index]) * Math.cos(selectionRadians)));
        int color = this.mColorSelector[index % 2][0];
        int alpha = this.mAlphaSelector[index % 2][0].getValue();
        Paint paint = this.mPaintSelector[index % 2][0];
        paint.setColor(color);
        paint.setAlpha(getMultipliedAlpha(color, alpha));
        canvas.drawCircle(pointX, pointY, this.mSelectionRadius[index], paint);
        if (this.mSelectionDegrees[index] % 30 != 0) {
            int color2 = this.mColorSelector[index % 2][1];
            int alpha2 = this.mAlphaSelector[index % 2][1].getValue();
            Paint paint2 = this.mPaintSelector[index % 2][1];
            paint2.setColor(color2);
            paint2.setAlpha(getMultipliedAlpha(color2, alpha2));
            canvas.drawCircle(pointX, pointY, (this.mSelectionRadius[index] * 2) / 7, paint2);
        } else {
            int lineLength = this.mLineLength[index] - this.mSelectionRadius[index];
            pointX = this.mXCenter + ((int) (((double) lineLength) * Math.sin(selectionRadians)));
            pointY = this.mYCenter - ((int) (((double) lineLength) * Math.cos(selectionRadians)));
        }
        int color3 = this.mColorSelector[index % 2][2];
        int alpha3 = this.mAlphaSelector[index % 2][2].getValue();
        Paint paint3 = this.mPaintSelector[index % 2][2];
        paint3.setColor(color3);
        paint3.setAlpha(getMultipliedAlpha(color3, alpha3));
        canvas.drawLine(this.mXCenter, this.mYCenter, pointX, pointY, paint3);
    }

    private void drawDebug(Canvas canvas) {
        float outerRadius = this.mCircleRadius[0] * this.mNumbersRadiusMultiplier[0];
        canvas.drawCircle(this.mXCenter, this.mYCenter, outerRadius, this.mPaintDebug);
        float innerRadius = this.mCircleRadius[0] * this.mNumbersRadiusMultiplier[2];
        canvas.drawCircle(this.mXCenter, this.mYCenter, innerRadius, this.mPaintDebug);
        canvas.drawCircle(this.mXCenter, this.mYCenter, this.mCircleRadius[0], this.mPaintDebug);
        float left = this.mXCenter - outerRadius;
        float top = this.mYCenter - outerRadius;
        float right = this.mXCenter + outerRadius;
        float bottom = this.mYCenter + outerRadius;
        canvas.drawRect(left, top, right, bottom, this.mPaintDebug);
        float left2 = this.mXCenter - this.mCircleRadius[0];
        float top2 = this.mYCenter - this.mCircleRadius[0];
        float right2 = this.mXCenter + this.mCircleRadius[0];
        float bottom2 = this.mYCenter + this.mCircleRadius[0];
        canvas.drawRect(left2, top2, right2, bottom2, this.mPaintDebug);
        canvas.drawRect(0.0f, 0.0f, getWidth(), getHeight(), this.mPaintDebug);
        String selected = String.format("%02d:%02d", Integer.valueOf(getCurrentHour()), Integer.valueOf(getCurrentMinute()));
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(-2, -2);
        TextView tv = new TextView(getContext());
        tv.setLayoutParams(lp);
        tv.setText(selected);
        tv.measure(0, 0);
        Paint paint = tv.getPaint();
        paint.setColor(DEBUG_TEXT_COLOR);
        int width = tv.getMeasuredWidth();
        float height = paint.descent() - paint.ascent();
        float x = this.mXCenter - (width / 2);
        float y = this.mYCenter + (1.5f * height);
        canvas.drawText(selected, x, y, paint);
    }

    private void calculateGridSizesHours() {
        float numbersRadius = this.mCircleRadius[0] * this.mNumbersRadiusMultiplier[0] * this.mAnimationRadiusMultiplier[0];
        calculateGridSizes(this.mPaint[0], numbersRadius, this.mXCenter, this.mYCenter, this.mTextSize[0], this.mTextGridHeights[0], this.mTextGridWidths[0]);
        if (this.mIs24HourMode) {
            float innerNumbersRadius = this.mCircleRadius[2] * this.mNumbersRadiusMultiplier[2] * this.mAnimationRadiusMultiplier[2];
            calculateGridSizes(this.mPaint[0], innerNumbersRadius, this.mXCenter, this.mYCenter, this.mInnerTextSize, this.mInnerTextGridHeights, this.mInnerTextGridWidths);
        }
    }

    private void calculateGridSizesMinutes() {
        float numbersRadius = this.mCircleRadius[1] * this.mNumbersRadiusMultiplier[1] * this.mAnimationRadiusMultiplier[1];
        calculateGridSizes(this.mPaint[1], numbersRadius, this.mXCenter, this.mYCenter, this.mTextSize[1], this.mTextGridHeights[1], this.mTextGridWidths[1]);
    }

    private static void calculateGridSizes(Paint paint, float numbersRadius, float xCenter, float yCenter, float textSize, float[] textGridHeights, float[] textGridWidths) {
        float offset2 = numbersRadius * COSINE_30_DEGREES;
        float offset3 = numbersRadius * SINE_30_DEGREES;
        paint.setTextSize(textSize);
        float yCenter2 = yCenter - ((paint.descent() + paint.ascent()) / 2.0f);
        textGridHeights[0] = yCenter2 - numbersRadius;
        textGridWidths[0] = xCenter - numbersRadius;
        textGridHeights[1] = yCenter2 - offset2;
        textGridWidths[1] = xCenter - offset2;
        textGridHeights[2] = yCenter2 - offset3;
        textGridWidths[2] = xCenter - offset3;
        textGridHeights[3] = yCenter2;
        textGridWidths[3] = xCenter;
        textGridHeights[4] = yCenter2 + offset3;
        textGridWidths[4] = xCenter + offset3;
        textGridHeights[5] = yCenter2 + offset2;
        textGridWidths[5] = xCenter + offset2;
        textGridHeights[6] = yCenter2 + numbersRadius;
        textGridWidths[6] = xCenter + numbersRadius;
    }

    private void drawTextElements(Canvas canvas, float textSize, Typeface typeface, String[] texts, float[] textGridWidths, float[] textGridHeights, Paint paint, int color, int alpha) {
        paint.setTextSize(textSize);
        paint.setTypeface(typeface);
        paint.setColor(color);
        paint.setAlpha(getMultipliedAlpha(color, alpha));
        canvas.drawText(texts[0], textGridWidths[3], textGridHeights[0], paint);
        canvas.drawText(texts[1], textGridWidths[4], textGridHeights[1], paint);
        canvas.drawText(texts[2], textGridWidths[5], textGridHeights[2], paint);
        canvas.drawText(texts[3], textGridWidths[6], textGridHeights[3], paint);
        canvas.drawText(texts[4], textGridWidths[5], textGridHeights[4], paint);
        canvas.drawText(texts[5], textGridWidths[4], textGridHeights[5], paint);
        canvas.drawText(texts[6], textGridWidths[3], textGridHeights[6], paint);
        canvas.drawText(texts[7], textGridWidths[2], textGridHeights[5], paint);
        canvas.drawText(texts[8], textGridWidths[1], textGridHeights[4], paint);
        canvas.drawText(texts[9], textGridWidths[0], textGridHeights[3], paint);
        canvas.drawText(texts[10], textGridWidths[1], textGridHeights[2], paint);
        canvas.drawText(texts[11], textGridWidths[2], textGridHeights[1], paint);
    }

    private void setAnimationRadiusMultiplierHours(float animationRadiusMultiplier) {
        this.mAnimationRadiusMultiplier[0] = animationRadiusMultiplier;
        this.mAnimationRadiusMultiplier[2] = animationRadiusMultiplier;
    }

    private void setAnimationRadiusMultiplierMinutes(float animationRadiusMultiplier) {
        this.mAnimationRadiusMultiplier[1] = animationRadiusMultiplier;
    }

    private static ObjectAnimator getRadiusDisappearAnimator(Object target, String radiusPropertyName, InvalidateUpdateListener updateListener, float midRadiusMultiplier, float endRadiusMultiplier) {
        Keyframe kf0 = Keyframe.ofFloat(0.0f, 1.0f);
        Keyframe kf1 = Keyframe.ofFloat(0.2f, midRadiusMultiplier);
        Keyframe kf2 = Keyframe.ofFloat(1.0f, endRadiusMultiplier);
        PropertyValuesHolder radiusDisappear = PropertyValuesHolder.ofKeyframe(radiusPropertyName, kf0, kf1, kf2);
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(target, radiusDisappear).setDuration(500);
        animator.addUpdateListener(updateListener);
        return animator;
    }

    private static ObjectAnimator getRadiusReappearAnimator(Object target, String radiusPropertyName, InvalidateUpdateListener updateListener, float midRadiusMultiplier, float endRadiusMultiplier) {
        float totalDurationMultiplier = 1.0f + 0.25f;
        int totalDuration = (int) (500 * totalDurationMultiplier);
        float delayPoint = (500 * 0.25f) / totalDuration;
        float midwayPoint = 1.0f - ((1.0f - delayPoint) * 0.2f);
        Keyframe kf0 = Keyframe.ofFloat(0.0f, endRadiusMultiplier);
        Keyframe kf1 = Keyframe.ofFloat(delayPoint, endRadiusMultiplier);
        Keyframe kf2 = Keyframe.ofFloat(midwayPoint, midRadiusMultiplier);
        Keyframe kf3 = Keyframe.ofFloat(1.0f, 1.0f);
        PropertyValuesHolder radiusReappear = PropertyValuesHolder.ofKeyframe(radiusPropertyName, kf0, kf1, kf2, kf3);
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(target, radiusReappear).setDuration(totalDuration);
        animator.addUpdateListener(updateListener);
        return animator;
    }

    private static ObjectAnimator getFadeOutAnimator(IntHolder target, int startAlpha, int endAlpha, InvalidateUpdateListener updateListener) {
        ObjectAnimator animator = ObjectAnimator.ofInt(target, "value", startAlpha, endAlpha);
        animator.setDuration(500);
        animator.addUpdateListener(updateListener);
        return animator;
    }

    private static ObjectAnimator getFadeInAnimator(IntHolder target, int startAlpha, int endAlpha, InvalidateUpdateListener updateListener) {
        float totalDurationMultiplier = 1.0f + 0.25f;
        int totalDuration = (int) (500 * totalDurationMultiplier);
        float delayPoint = (500 * 0.25f) / totalDuration;
        Keyframe kf0 = Keyframe.ofInt(0.0f, startAlpha);
        Keyframe kf1 = Keyframe.ofInt(delayPoint, startAlpha);
        Keyframe kf2 = Keyframe.ofInt(1.0f, endAlpha);
        PropertyValuesHolder fadeIn = PropertyValuesHolder.ofKeyframe("value", kf0, kf1, kf2);
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(target, fadeIn).setDuration(totalDuration);
        animator.addUpdateListener(updateListener);
        return animator;
    }

    private class InvalidateUpdateListener implements ValueAnimator.AnimatorUpdateListener {
        private InvalidateUpdateListener() {
        }

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            RadialTimePickerView.this.invalidate();
        }
    }

    private void startHoursToMinutesAnimation() {
        if (this.mHoursToMinutesAnims.size() == 0) {
            this.mHoursToMinutesAnims.add(getRadiusDisappearAnimator(this, "animationRadiusMultiplierHours", this.mInvalidateUpdateListener, this.mTransitionMidRadiusMultiplier, this.mTransitionEndRadiusMultiplier));
            this.mHoursToMinutesAnims.add(getFadeOutAnimator(this.mAlpha[0], 255, 0, this.mInvalidateUpdateListener));
            this.mHoursToMinutesAnims.add(getFadeOutAnimator(this.mAlphaSelector[0][0], 60, 0, this.mInvalidateUpdateListener));
            this.mHoursToMinutesAnims.add(getFadeOutAnimator(this.mAlphaSelector[0][1], 255, 0, this.mInvalidateUpdateListener));
            this.mHoursToMinutesAnims.add(getFadeOutAnimator(this.mAlphaSelector[0][2], 60, 0, this.mInvalidateUpdateListener));
            this.mHoursToMinutesAnims.add(getRadiusReappearAnimator(this, "animationRadiusMultiplierMinutes", this.mInvalidateUpdateListener, this.mTransitionMidRadiusMultiplier, this.mTransitionEndRadiusMultiplier));
            this.mHoursToMinutesAnims.add(getFadeInAnimator(this.mAlpha[1], 0, 255, this.mInvalidateUpdateListener));
            this.mHoursToMinutesAnims.add(getFadeInAnimator(this.mAlphaSelector[1][0], 0, 60, this.mInvalidateUpdateListener));
            this.mHoursToMinutesAnims.add(getFadeInAnimator(this.mAlphaSelector[1][1], 0, 255, this.mInvalidateUpdateListener));
            this.mHoursToMinutesAnims.add(getFadeInAnimator(this.mAlphaSelector[1][2], 0, 60, this.mInvalidateUpdateListener));
        }
        if (this.mTransition != null && this.mTransition.isRunning()) {
            this.mTransition.end();
        }
        this.mTransition = new AnimatorSet();
        this.mTransition.playTogether(this.mHoursToMinutesAnims);
        this.mTransition.start();
    }

    private void startMinutesToHoursAnimation() {
        if (this.mMinuteToHoursAnims.size() == 0) {
            this.mMinuteToHoursAnims.add(getRadiusDisappearAnimator(this, "animationRadiusMultiplierMinutes", this.mInvalidateUpdateListener, this.mTransitionMidRadiusMultiplier, this.mTransitionEndRadiusMultiplier));
            this.mMinuteToHoursAnims.add(getFadeOutAnimator(this.mAlpha[1], 255, 0, this.mInvalidateUpdateListener));
            this.mMinuteToHoursAnims.add(getFadeOutAnimator(this.mAlphaSelector[1][0], 60, 0, this.mInvalidateUpdateListener));
            this.mMinuteToHoursAnims.add(getFadeOutAnimator(this.mAlphaSelector[1][1], 255, 0, this.mInvalidateUpdateListener));
            this.mMinuteToHoursAnims.add(getFadeOutAnimator(this.mAlphaSelector[1][2], 60, 0, this.mInvalidateUpdateListener));
            this.mMinuteToHoursAnims.add(getRadiusReappearAnimator(this, "animationRadiusMultiplierHours", this.mInvalidateUpdateListener, this.mTransitionMidRadiusMultiplier, this.mTransitionEndRadiusMultiplier));
            this.mMinuteToHoursAnims.add(getFadeInAnimator(this.mAlpha[0], 0, 255, this.mInvalidateUpdateListener));
            this.mMinuteToHoursAnims.add(getFadeInAnimator(this.mAlphaSelector[0][0], 0, 60, this.mInvalidateUpdateListener));
            this.mMinuteToHoursAnims.add(getFadeInAnimator(this.mAlphaSelector[0][1], 0, 255, this.mInvalidateUpdateListener));
            this.mMinuteToHoursAnims.add(getFadeInAnimator(this.mAlphaSelector[0][2], 0, 60, this.mInvalidateUpdateListener));
        }
        if (this.mTransition != null && this.mTransition.isRunning()) {
            this.mTransition.end();
        }
        this.mTransition = new AnimatorSet();
        this.mTransition.playTogether(this.mMinuteToHoursAnims);
        this.mTransition.start();
    }

    private int getDegreesFromXY(float x, float y) {
        double hypotenuse = Math.sqrt(((y - this.mYCenter) * (y - this.mYCenter)) + ((x - this.mXCenter) * (x - this.mXCenter)));
        if (hypotenuse > this.mCircleRadius[0]) {
            return -1;
        }
        if (!this.mIs24HourMode || !this.mShowHours) {
            int index = this.mShowHours ? 0 : 1;
            float length = this.mCircleRadius[index] * this.mNumbersRadiusMultiplier[index];
            int distanceToNumber = (int) Math.abs(hypotenuse - ((double) length));
            int maxAllowedDistance = (int) (this.mCircleRadius[index] * (1.0f - this.mNumbersRadiusMultiplier[index]));
            if (distanceToNumber > maxAllowedDistance) {
                return -1;
            }
        } else if (hypotenuse >= this.mMinHypotenuseForInnerNumber && hypotenuse <= this.mHalfwayHypotenusePoint) {
            this.mIsOnInnerCircle = true;
        } else if (hypotenuse <= this.mMaxHypotenuseForOuterNumber && hypotenuse >= this.mHalfwayHypotenusePoint) {
            this.mIsOnInnerCircle = false;
        } else {
            return -1;
        }
        float opposite = Math.abs(y - this.mYCenter);
        int degrees = (int) (Math.toDegrees(Math.asin(((double) opposite) / hypotenuse)) + 0.5d);
        boolean rightSide = x > ((float) this.mXCenter);
        boolean topSide = y < ((float) this.mYCenter);
        if (rightSide) {
            if (topSide) {
                return 90 - degrees;
            }
            return degrees + 90;
        }
        if (topSide) {
            return degrees + R.styleable.Theme_textUnderlineColor;
        }
        return 270 - degrees;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action;
        if (this.mInputEnabled && ((action = event.getActionMasked()) == 2 || action == 1 || action == 0)) {
            boolean forceSelection = false;
            boolean autoAdvance = false;
            if (action == 0) {
                this.mChangedDuringTouch = false;
            } else if (action == 1) {
                autoAdvance = true;
                if (!this.mChangedDuringTouch) {
                    forceSelection = true;
                }
            }
            this.mChangedDuringTouch |= handleTouchInput(event.getX(), event.getY(), forceSelection, autoAdvance);
        }
        return true;
    }

    private boolean handleTouchInput(float x, float y, boolean forceSelection, boolean autoAdvance) {
        boolean valueChanged;
        int type;
        int newValue;
        boolean wasOnInnerCircle = this.mIsOnInnerCircle;
        int degrees = getDegreesFromXY(x, y);
        if (degrees == -1) {
            return false;
        }
        int[] selectionDegrees = this.mSelectionDegrees;
        if (this.mShowHours) {
            int snapDegrees = snapOnly30s(degrees, 0) % 360;
            valueChanged = (selectionDegrees[0] == snapDegrees && selectionDegrees[2] == snapDegrees && wasOnInnerCircle == this.mIsOnInnerCircle) ? false : true;
            selectionDegrees[0] = snapDegrees;
            selectionDegrees[2] = snapDegrees;
            type = 0;
            newValue = getCurrentHour();
        } else {
            int snapDegrees2 = snapPrefer30s(degrees) % 360;
            valueChanged = selectionDegrees[1] != snapDegrees2;
            selectionDegrees[1] = snapDegrees2;
            type = 1;
            newValue = getCurrentMinute();
        }
        if (!valueChanged && !forceSelection && !autoAdvance) {
            return false;
        }
        if (this.mListener != null) {
            this.mListener.onValueSelected(type, newValue, autoAdvance);
        }
        if (valueChanged || forceSelection) {
            performHapticFeedback(4);
            invalidate();
        }
        return true;
    }

    @Override
    public boolean dispatchHoverEvent(MotionEvent event) {
        if (this.mTouchHelper.dispatchHoverEvent(event)) {
            return true;
        }
        return super.dispatchHoverEvent(event);
    }

    public void setInputEnabled(boolean inputEnabled) {
        this.mInputEnabled = inputEnabled;
        invalidate();
    }

    private class RadialPickerTouchHelper extends ExploreByTouchHelper {
        private final int MASK_TYPE;
        private final int MASK_VALUE;
        private final int MINUTE_INCREMENT;
        private final int SHIFT_TYPE;
        private final int SHIFT_VALUE;
        private final int TYPE_HOUR;
        private final int TYPE_MINUTE;
        private final Rect mTempRect;

        public RadialPickerTouchHelper() {
            super(RadialTimePickerView.this);
            this.mTempRect = new Rect();
            this.TYPE_HOUR = 1;
            this.TYPE_MINUTE = 2;
            this.SHIFT_TYPE = 0;
            this.MASK_TYPE = 15;
            this.SHIFT_VALUE = 8;
            this.MASK_VALUE = 255;
            this.MINUTE_INCREMENT = 5;
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(host, info);
            info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);
            info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);
        }

        @Override
        public boolean performAccessibilityAction(View host, int action, Bundle arguments) {
            if (super.performAccessibilityAction(host, action, arguments)) {
                return true;
            }
            switch (action) {
                case 4096:
                    adjustPicker(1);
                    break;
                case 8192:
                    adjustPicker(-1);
                    break;
            }
            return true;
        }

        private void adjustPicker(int step) {
            int stepSize;
            int initialValue;
            int maxValue;
            int minValue;
            if (RadialTimePickerView.this.mShowHours) {
                stepSize = 30;
                initialValue = RadialTimePickerView.this.getCurrentHour() % 12;
                if (RadialTimePickerView.this.mIs24HourMode) {
                    maxValue = 23;
                    minValue = 0;
                } else {
                    maxValue = 12;
                    minValue = 1;
                }
            } else {
                stepSize = 6;
                initialValue = RadialTimePickerView.this.getCurrentMinute();
                maxValue = 55;
                minValue = 0;
            }
            int steppedValue = RadialTimePickerView.snapOnly30s(initialValue * stepSize, step) / stepSize;
            int clampedValue = MathUtils.constrain(steppedValue, minValue, maxValue);
            if (RadialTimePickerView.this.mShowHours) {
                RadialTimePickerView.this.setCurrentHour(clampedValue);
            } else {
                RadialTimePickerView.this.setCurrentMinute(clampedValue);
            }
        }

        @Override
        protected int getVirtualViewAt(float x, float y) {
            int minute;
            boolean wasOnInnerCircle = RadialTimePickerView.this.mIsOnInnerCircle;
            int degrees = RadialTimePickerView.this.getDegreesFromXY(x, y);
            boolean isOnInnerCircle = RadialTimePickerView.this.mIsOnInnerCircle;
            RadialTimePickerView.this.mIsOnInnerCircle = wasOnInnerCircle;
            if (degrees != -1) {
                int snapDegrees = RadialTimePickerView.snapOnly30s(degrees, 0) % 360;
                if (RadialTimePickerView.this.mShowHours) {
                    int hour24 = RadialTimePickerView.this.getHourForDegrees(snapDegrees, isOnInnerCircle);
                    int hour = RadialTimePickerView.this.mIs24HourMode ? hour24 : hour24To12(hour24);
                    int id = makeId(1, hour);
                    return id;
                }
                int current = RadialTimePickerView.this.getCurrentMinute();
                int touched = RadialTimePickerView.this.getMinuteForDegrees(degrees);
                int snapped = RadialTimePickerView.this.getMinuteForDegrees(snapDegrees);
                if (Math.abs(current - touched) < Math.abs(snapped - touched)) {
                    minute = current;
                } else {
                    minute = snapped;
                }
                int id2 = makeId(2, minute);
                return id2;
            }
            return Integer.MIN_VALUE;
        }

        @Override
        protected void getVisibleVirtualViews(IntArray virtualViewIds) {
            if (RadialTimePickerView.this.mShowHours) {
                int min = RadialTimePickerView.this.mIs24HourMode ? 0 : 1;
                int max = RadialTimePickerView.this.mIs24HourMode ? 23 : 12;
                for (int i = min; i <= max; i++) {
                    virtualViewIds.add(makeId(1, i));
                }
                return;
            }
            int current = RadialTimePickerView.this.getCurrentMinute();
            for (int i2 = 0; i2 < 60; i2 += 5) {
                virtualViewIds.add(makeId(2, i2));
                if (current > i2 && current < i2 + 5) {
                    virtualViewIds.add(makeId(2, current));
                }
            }
        }

        @Override
        protected void onPopulateEventForVirtualView(int virtualViewId, AccessibilityEvent event) {
            event.setClassName(getClass().getName());
            int type = getTypeFromId(virtualViewId);
            int value = getValueFromId(virtualViewId);
            CharSequence description = getVirtualViewDescription(type, value);
            event.setContentDescription(description);
        }

        @Override
        protected void onPopulateNodeForVirtualView(int virtualViewId, AccessibilityNodeInfo node) {
            node.setClassName(getClass().getName());
            node.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK);
            int type = getTypeFromId(virtualViewId);
            int value = getValueFromId(virtualViewId);
            CharSequence description = getVirtualViewDescription(type, value);
            node.setContentDescription(description);
            getBoundsForVirtualView(virtualViewId, this.mTempRect);
            node.setBoundsInParent(this.mTempRect);
            boolean selected = isVirtualViewSelected(type, value);
            node.setSelected(selected);
            int nextId = getVirtualViewIdAfter(type, value);
            if (nextId != Integer.MIN_VALUE) {
                node.setTraversalBefore(RadialTimePickerView.this, nextId);
            }
        }

        private int getVirtualViewIdAfter(int type, int value) {
            if (type == 1) {
                int nextValue = value + 1;
                int max = RadialTimePickerView.this.mIs24HourMode ? 23 : 12;
                if (nextValue <= max) {
                    return makeId(type, nextValue);
                }
            } else if (type == 2) {
                int current = RadialTimePickerView.this.getCurrentMinute();
                int snapValue = value - (value % 5);
                int nextValue2 = snapValue + 5;
                if (value < current && nextValue2 > current) {
                    return makeId(type, current);
                }
                if (nextValue2 < 60) {
                    return makeId(type, nextValue2);
                }
            }
            return Integer.MIN_VALUE;
        }

        @Override
        protected boolean onPerformActionForVirtualView(int virtualViewId, int action, Bundle arguments) {
            if (action == 16) {
                int type = getTypeFromId(virtualViewId);
                int value = getValueFromId(virtualViewId);
                if (type == 1) {
                    int hour = RadialTimePickerView.this.mIs24HourMode ? value : hour12To24(value, RadialTimePickerView.this.mAmOrPm);
                    RadialTimePickerView.this.setCurrentHour(hour);
                    return true;
                }
                if (type == 2) {
                    RadialTimePickerView.this.setCurrentMinute(value);
                    return true;
                }
            }
            return false;
        }

        private int hour12To24(int hour12, int amOrPm) {
            if (hour12 == 12) {
                if (amOrPm != 0) {
                    return hour12;
                }
                return 0;
            }
            if (amOrPm != 1) {
                return hour12;
            }
            int hour24 = hour12 + 12;
            return hour24;
        }

        private int hour24To12(int hour24) {
            if (hour24 == 0) {
                return 12;
            }
            if (hour24 > 12) {
                return hour24 - 12;
            }
            return hour24;
        }

        private void getBoundsForVirtualView(int virtualViewId, Rect bounds) {
            float centerRadius;
            float degrees;
            float radius;
            int type = getTypeFromId(virtualViewId);
            int value = getValueFromId(virtualViewId);
            if (type == 1) {
                boolean innerCircle = RadialTimePickerView.this.mIs24HourMode && value > 0 && value <= 12;
                if (innerCircle) {
                    centerRadius = RadialTimePickerView.this.mCircleRadius[2] * RadialTimePickerView.this.mNumbersRadiusMultiplier[2];
                    radius = RadialTimePickerView.this.mSelectionRadius[2];
                } else {
                    centerRadius = RadialTimePickerView.this.mCircleRadius[0] * RadialTimePickerView.this.mNumbersRadiusMultiplier[0];
                    radius = RadialTimePickerView.this.mSelectionRadius[0];
                }
                degrees = RadialTimePickerView.this.getDegreesForHour(value);
            } else if (type == 2) {
                centerRadius = RadialTimePickerView.this.mCircleRadius[1] * RadialTimePickerView.this.mNumbersRadiusMultiplier[1];
                degrees = RadialTimePickerView.this.getDegreesForMinute(value);
                radius = RadialTimePickerView.this.mSelectionRadius[1];
            } else {
                centerRadius = 0.0f;
                degrees = 0.0f;
                radius = 0.0f;
            }
            double radians = Math.toRadians(degrees);
            float xCenter = RadialTimePickerView.this.mXCenter + (((float) Math.sin(radians)) * centerRadius);
            float yCenter = RadialTimePickerView.this.mYCenter - (((float) Math.cos(radians)) * centerRadius);
            bounds.set((int) (xCenter - radius), (int) (yCenter - radius), (int) (xCenter + radius), (int) (yCenter + radius));
        }

        private CharSequence getVirtualViewDescription(int type, int value) {
            if (type == 1 || type == 2) {
                CharSequence description = Integer.toString(value);
                return description;
            }
            return null;
        }

        private boolean isVirtualViewSelected(int type, int value) {
            return type == 1 ? RadialTimePickerView.this.getCurrentHour() == value : type == 2 && RadialTimePickerView.this.getCurrentMinute() == value;
        }

        private int makeId(int type, int value) {
            return (type << 0) | (value << 8);
        }

        private int getTypeFromId(int id) {
            return (id >>> 0) & 15;
        }

        private int getValueFromId(int id) {
            return (id >>> 8) & 255;
        }
    }

    private static class IntHolder {
        private int mValue;

        public IntHolder(int value) {
            this.mValue = value;
        }

        public void setValue(int value) {
            this.mValue = value;
        }

        public int getValue() {
            return this.mValue;
        }
    }
}
