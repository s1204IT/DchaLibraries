package com.android.datetimepicker.time;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import com.android.datetimepicker.HapticFeedbackController;
import com.android.datetimepicker.R;

public class RadialPickerLayout extends FrameLayout implements View.OnTouchListener {
    private final int TAP_TIMEOUT;
    private final int TOUCH_SLOP;
    private AccessibilityManager mAccessibilityManager;
    private AmPmCirclesView mAmPmCirclesView;
    private CircleView mCircleView;
    private int mCurrentHoursOfDay;
    private int mCurrentItemShowing;
    private int mCurrentMinutes;
    private boolean mDoingMove;
    private boolean mDoingTouch;
    private int mDownDegrees;
    private float mDownX;
    private float mDownY;
    private View mGrayBox;
    private Handler mHandler;
    private HapticFeedbackController mHapticFeedbackController;
    private boolean mHideAmPm;
    private RadialSelectorView mHourRadialSelectorView;
    private RadialTextsView mHourRadialTextsView;
    private boolean mInputEnabled;
    private boolean mIs24HourMode;
    private int mIsTouchingAmOrPm;
    private int mLastValueSelected;
    private OnValueSelectedListener mListener;
    private RadialSelectorView mMinuteRadialSelectorView;
    private RadialTextsView mMinuteRadialTextsView;
    private int[] mSnapPrefer30sMap;
    private boolean mTimeInitialized;
    private AnimatorSet mTransition;

    public interface OnValueSelectedListener {
        void onValueSelected(int i, int i2, boolean z);
    }

    public RadialPickerLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mIsTouchingAmOrPm = -1;
        this.mHandler = new Handler();
        setOnTouchListener(this);
        ViewConfiguration vc = ViewConfiguration.get(context);
        this.TOUCH_SLOP = vc.getScaledTouchSlop();
        this.TAP_TIMEOUT = ViewConfiguration.getTapTimeout();
        this.mDoingMove = false;
        this.mCircleView = new CircleView(context);
        addView(this.mCircleView);
        this.mAmPmCirclesView = new AmPmCirclesView(context);
        addView(this.mAmPmCirclesView);
        this.mHourRadialTextsView = new RadialTextsView(context);
        addView(this.mHourRadialTextsView);
        this.mMinuteRadialTextsView = new RadialTextsView(context);
        addView(this.mMinuteRadialTextsView);
        this.mHourRadialSelectorView = new RadialSelectorView(context);
        addView(this.mHourRadialSelectorView);
        this.mMinuteRadialSelectorView = new RadialSelectorView(context);
        addView(this.mMinuteRadialSelectorView);
        preparePrefer30sMap();
        this.mLastValueSelected = -1;
        this.mInputEnabled = true;
        this.mGrayBox = new View(context);
        this.mGrayBox.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
        this.mGrayBox.setBackgroundColor(getResources().getColor(R.color.transparent_black));
        this.mGrayBox.setVisibility(4);
        addView(this.mGrayBox);
        this.mAccessibilityManager = (AccessibilityManager) context.getSystemService("accessibility");
        this.mTimeInitialized = false;
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

    public void setOnValueSelectedListener(OnValueSelectedListener listener) {
        this.mListener = listener;
    }

    public void initialize(Context context, HapticFeedbackController hapticFeedbackController, int initialHoursOfDay, int initialMinutes, boolean is24HourMode) {
        if (this.mTimeInitialized) {
            Log.e("RadialPickerLayout", "Time has already been initialized.");
            return;
        }
        this.mHapticFeedbackController = hapticFeedbackController;
        this.mIs24HourMode = is24HourMode;
        this.mHideAmPm = this.mAccessibilityManager.isTouchExplorationEnabled() ? true : this.mIs24HourMode;
        this.mCircleView.initialize(context, this.mHideAmPm);
        this.mCircleView.invalidate();
        if (!this.mHideAmPm) {
            this.mAmPmCirclesView.initialize(context, initialHoursOfDay < 12 ? 0 : 1);
            this.mAmPmCirclesView.invalidate();
        }
        Resources res = context.getResources();
        int[] hours = {12, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
        int[] hours_24 = {0, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23};
        int[] minutes = {0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55};
        String[] hoursTexts = new String[12];
        String[] innerHoursTexts = new String[12];
        String[] minutesTexts = new String[12];
        for (int i = 0; i < 12; i++) {
            hoursTexts[i] = is24HourMode ? String.format("%02d", Integer.valueOf(hours_24[i])) : String.format("%d", Integer.valueOf(hours[i]));
            innerHoursTexts[i] = String.format("%d", Integer.valueOf(hours[i]));
            minutesTexts[i] = String.format("%02d", Integer.valueOf(minutes[i]));
        }
        this.mHourRadialTextsView.initialize(res, hoursTexts, is24HourMode ? innerHoursTexts : null, this.mHideAmPm, true);
        this.mHourRadialTextsView.invalidate();
        this.mMinuteRadialTextsView.initialize(res, minutesTexts, null, this.mHideAmPm, false);
        this.mMinuteRadialTextsView.invalidate();
        setValueForItem(0, initialHoursOfDay);
        setValueForItem(1, initialMinutes);
        int hourDegrees = (initialHoursOfDay % 12) * 30;
        this.mHourRadialSelectorView.initialize(context, this.mHideAmPm, is24HourMode, true, hourDegrees, isHourInnerCircle(initialHoursOfDay));
        int minuteDegrees = initialMinutes * 6;
        this.mMinuteRadialSelectorView.initialize(context, this.mHideAmPm, false, false, minuteDegrees, false);
        this.mTimeInitialized = true;
    }

    void setTheme(Context context, boolean themeDark) {
        this.mCircleView.setTheme(context, themeDark);
        this.mAmPmCirclesView.setTheme(context, themeDark);
        this.mHourRadialTextsView.setTheme(context, themeDark);
        this.mMinuteRadialTextsView.setTheme(context, themeDark);
        this.mHourRadialSelectorView.setTheme(context, themeDark);
        this.mMinuteRadialSelectorView.setTheme(context, themeDark);
    }

    public void setTime(int hours, int minutes) {
        setItem(0, hours);
        setItem(1, minutes);
    }

    private void setItem(int index, int value) {
        if (index == 0) {
            setValueForItem(0, value);
            int hourDegrees = (value % 12) * 30;
            this.mHourRadialSelectorView.setSelection(hourDegrees, isHourInnerCircle(value), false);
            this.mHourRadialSelectorView.invalidate();
            return;
        }
        if (index == 1) {
            setValueForItem(1, value);
            int minuteDegrees = value * 6;
            this.mMinuteRadialSelectorView.setSelection(minuteDegrees, false, false);
            this.mMinuteRadialSelectorView.invalidate();
        }
    }

    private boolean isHourInnerCircle(int hourOfDay) {
        return this.mIs24HourMode && hourOfDay <= 12 && hourOfDay != 0;
    }

    public int getHours() {
        return this.mCurrentHoursOfDay;
    }

    public int getMinutes() {
        return this.mCurrentMinutes;
    }

    private int getCurrentlyShowingValue() {
        int currentIndex = getCurrentItemShowing();
        if (currentIndex == 0) {
            return this.mCurrentHoursOfDay;
        }
        if (currentIndex == 1) {
            return this.mCurrentMinutes;
        }
        return -1;
    }

    public int getIsCurrentlyAmOrPm() {
        if (this.mCurrentHoursOfDay < 12) {
            return 0;
        }
        if (this.mCurrentHoursOfDay < 24) {
            return 1;
        }
        return -1;
    }

    private void setValueForItem(int index, int value) {
        if (index == 0) {
            this.mCurrentHoursOfDay = value;
            return;
        }
        if (index == 1) {
            this.mCurrentMinutes = value;
            return;
        }
        if (index == 2) {
            if (value == 0) {
                this.mCurrentHoursOfDay %= 12;
            } else if (value == 1) {
                this.mCurrentHoursOfDay = (this.mCurrentHoursOfDay % 12) + 12;
            }
        }
    }

    public void setAmOrPm(int amOrPm) {
        this.mAmPmCirclesView.setAmOrPm(amOrPm);
        this.mAmPmCirclesView.invalidate();
        setValueForItem(2, amOrPm);
    }

    private void preparePrefer30sMap() {
        this.mSnapPrefer30sMap = new int[361];
        int snappedOutputDegrees = 0;
        int count = 1;
        int expectedCount = 8;
        for (int degrees = 0; degrees < 361; degrees++) {
            this.mSnapPrefer30sMap[degrees] = snappedOutputDegrees;
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

    private int snapPrefer30s(int degrees) {
        if (this.mSnapPrefer30sMap == null) {
            return -1;
        }
        return this.mSnapPrefer30sMap[degrees];
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

    private int reselectSelector(int degrees, boolean isInnerCircle, boolean forceToVisibleValue, boolean forceDrawDot) {
        int degrees2;
        RadialSelectorView radialSelectorView;
        int stepSize;
        if (degrees == -1) {
            return -1;
        }
        int currentShowing = getCurrentItemShowing();
        boolean allowFineGrained = !forceToVisibleValue && currentShowing == 1;
        if (allowFineGrained) {
            degrees2 = snapPrefer30s(degrees);
        } else {
            degrees2 = snapOnly30s(degrees, 0);
        }
        if (currentShowing == 0) {
            radialSelectorView = this.mHourRadialSelectorView;
            stepSize = 30;
        } else {
            radialSelectorView = this.mMinuteRadialSelectorView;
            stepSize = 6;
        }
        radialSelectorView.setSelection(degrees2, isInnerCircle, forceDrawDot);
        radialSelectorView.invalidate();
        if (currentShowing == 0) {
            if (this.mIs24HourMode) {
                if (degrees2 == 0 && isInnerCircle) {
                    degrees2 = 360;
                } else if (degrees2 == 360 && !isInnerCircle) {
                    degrees2 = 0;
                }
            } else if (degrees2 == 0) {
                degrees2 = 360;
            }
        } else if (degrees2 == 360 && currentShowing == 1) {
            degrees2 = 0;
        }
        int value = degrees2 / stepSize;
        if (currentShowing == 0 && this.mIs24HourMode && !isInnerCircle && degrees2 != 0) {
            return value + 12;
        }
        return value;
    }

    private int getDegreesFromCoords(float pointX, float pointY, boolean forceLegal, Boolean[] isInnerCircle) {
        int currentItem = getCurrentItemShowing();
        if (currentItem == 0) {
            return this.mHourRadialSelectorView.getDegreesFromCoords(pointX, pointY, forceLegal, isInnerCircle);
        }
        if (currentItem == 1) {
            return this.mMinuteRadialSelectorView.getDegreesFromCoords(pointX, pointY, forceLegal, isInnerCircle);
        }
        return -1;
    }

    public int getCurrentItemShowing() {
        if (this.mCurrentItemShowing == 0 || this.mCurrentItemShowing == 1) {
            return this.mCurrentItemShowing;
        }
        Log.e("RadialPickerLayout", "Current item showing was unfortunately set to " + this.mCurrentItemShowing);
        return -1;
    }

    public void setCurrentItemShowing(int index, boolean animate) {
        if (index != 0 && index != 1) {
            Log.e("RadialPickerLayout", "TimePicker does not support view at index " + index);
            return;
        }
        int lastIndex = getCurrentItemShowing();
        this.mCurrentItemShowing = index;
        if (animate && index != lastIndex) {
            ObjectAnimator[] anims = new ObjectAnimator[4];
            if (index == 1) {
                anims[0] = this.mHourRadialTextsView.getDisappearAnimator();
                anims[1] = this.mHourRadialSelectorView.getDisappearAnimator();
                anims[2] = this.mMinuteRadialTextsView.getReappearAnimator();
                anims[3] = this.mMinuteRadialSelectorView.getReappearAnimator();
            } else if (index == 0) {
                anims[0] = this.mHourRadialTextsView.getReappearAnimator();
                anims[1] = this.mHourRadialSelectorView.getReappearAnimator();
                anims[2] = this.mMinuteRadialTextsView.getDisappearAnimator();
                anims[3] = this.mMinuteRadialSelectorView.getDisappearAnimator();
            }
            if (this.mTransition != null && this.mTransition.isRunning()) {
                this.mTransition.end();
            }
            this.mTransition = new AnimatorSet();
            this.mTransition.playTogether(anims);
            this.mTransition.start();
            return;
        }
        int hourAlpha = index == 0 ? 255 : 0;
        int minuteAlpha = index != 1 ? 0 : 255;
        this.mHourRadialTextsView.setAlpha(hourAlpha);
        this.mHourRadialSelectorView.setAlpha(hourAlpha);
        this.mMinuteRadialTextsView.setAlpha(minuteAlpha);
        this.mMinuteRadialSelectorView.setAlpha(minuteAlpha);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int degrees;
        int value;
        float eventX = event.getX();
        float eventY = event.getY();
        final Boolean[] isInnerCircle = {false};
        switch (event.getAction()) {
            case 0:
                if (!this.mInputEnabled) {
                    return true;
                }
                this.mDownX = eventX;
                this.mDownY = eventY;
                this.mLastValueSelected = -1;
                this.mDoingMove = false;
                this.mDoingTouch = true;
                if (!this.mHideAmPm) {
                    this.mIsTouchingAmOrPm = this.mAmPmCirclesView.getIsTouchingAmOrPm(eventX, eventY);
                } else {
                    this.mIsTouchingAmOrPm = -1;
                }
                if (this.mIsTouchingAmOrPm == 0 || this.mIsTouchingAmOrPm == 1) {
                    this.mHapticFeedbackController.tryVibrate();
                    this.mDownDegrees = -1;
                    this.mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            RadialPickerLayout.this.mAmPmCirclesView.setAmOrPmPressed(RadialPickerLayout.this.mIsTouchingAmOrPm);
                            RadialPickerLayout.this.mAmPmCirclesView.invalidate();
                        }
                    }, this.TAP_TIMEOUT);
                } else {
                    boolean forceLegal = this.mAccessibilityManager.isTouchExplorationEnabled();
                    this.mDownDegrees = getDegreesFromCoords(eventX, eventY, forceLegal, isInnerCircle);
                    if (this.mDownDegrees != -1) {
                        this.mHapticFeedbackController.tryVibrate();
                        this.mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                RadialPickerLayout.this.mDoingMove = true;
                                int value2 = RadialPickerLayout.this.reselectSelector(RadialPickerLayout.this.mDownDegrees, isInnerCircle[0].booleanValue(), false, true);
                                RadialPickerLayout.this.mLastValueSelected = value2;
                                RadialPickerLayout.this.mListener.onValueSelected(RadialPickerLayout.this.getCurrentItemShowing(), value2, false);
                            }
                        }, this.TAP_TIMEOUT);
                    }
                }
                return true;
            case 1:
                if (!this.mInputEnabled) {
                    Log.d("RadialPickerLayout", "Input was disabled, but received ACTION_UP.");
                    this.mListener.onValueSelected(3, 1, false);
                    return true;
                }
                this.mHandler.removeCallbacksAndMessages(null);
                this.mDoingTouch = false;
                if (this.mIsTouchingAmOrPm == 0 || this.mIsTouchingAmOrPm == 1) {
                    int isTouchingAmOrPm = this.mAmPmCirclesView.getIsTouchingAmOrPm(eventX, eventY);
                    this.mAmPmCirclesView.setAmOrPmPressed(-1);
                    this.mAmPmCirclesView.invalidate();
                    if (isTouchingAmOrPm == this.mIsTouchingAmOrPm) {
                        this.mAmPmCirclesView.setAmOrPm(isTouchingAmOrPm);
                        if (getIsCurrentlyAmOrPm() != isTouchingAmOrPm) {
                            this.mListener.onValueSelected(2, this.mIsTouchingAmOrPm, false);
                            setValueForItem(2, isTouchingAmOrPm);
                        }
                    }
                    this.mIsTouchingAmOrPm = -1;
                    return false;
                }
                if (this.mDownDegrees != -1 && (degrees = getDegreesFromCoords(eventX, eventY, this.mDoingMove, isInnerCircle)) != -1) {
                    int value2 = reselectSelector(degrees, isInnerCircle[0].booleanValue(), !this.mDoingMove, false);
                    if (getCurrentItemShowing() == 0 && !this.mIs24HourMode) {
                        int amOrPm = getIsCurrentlyAmOrPm();
                        if (amOrPm == 0 && value2 == 12) {
                            value2 = 0;
                        } else if (amOrPm == 1 && value2 != 12) {
                            value2 += 12;
                        }
                    }
                    setValueForItem(getCurrentItemShowing(), value2);
                    this.mListener.onValueSelected(getCurrentItemShowing(), value2, true);
                }
                this.mDoingMove = false;
                return true;
            case 2:
                if (!this.mInputEnabled) {
                    Log.e("RadialPickerLayout", "Input was disabled, but received ACTION_MOVE.");
                    return true;
                }
                float dY = Math.abs(eventY - this.mDownY);
                float dX = Math.abs(eventX - this.mDownX);
                if (this.mDoingMove || dX > this.TOUCH_SLOP || dY > this.TOUCH_SLOP) {
                    if (this.mIsTouchingAmOrPm == 0 || this.mIsTouchingAmOrPm == 1) {
                        this.mHandler.removeCallbacksAndMessages(null);
                        if (this.mAmPmCirclesView.getIsTouchingAmOrPm(eventX, eventY) != this.mIsTouchingAmOrPm) {
                            this.mAmPmCirclesView.setAmOrPmPressed(-1);
                            this.mAmPmCirclesView.invalidate();
                            this.mIsTouchingAmOrPm = -1;
                        }
                    } else if (this.mDownDegrees != -1) {
                        this.mDoingMove = true;
                        this.mHandler.removeCallbacksAndMessages(null);
                        int degrees2 = getDegreesFromCoords(eventX, eventY, true, isInnerCircle);
                        if (degrees2 != -1 && (value = reselectSelector(degrees2, isInnerCircle[0].booleanValue(), false, true)) != this.mLastValueSelected) {
                            this.mHapticFeedbackController.tryVibrate();
                            this.mLastValueSelected = value;
                            this.mListener.onValueSelected(getCurrentItemShowing(), value, false);
                        }
                        return true;
                    }
                }
                return false;
            default:
                return false;
        }
    }

    public boolean trySettingInputEnabled(boolean inputEnabled) {
        if (this.mDoingTouch && !inputEnabled) {
            return false;
        }
        this.mInputEnabled = inputEnabled;
        this.mGrayBox.setVisibility(inputEnabled ? 4 : 0);
        return true;
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.addAction(4096);
        info.addAction(8192);
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != 32) {
            return super.dispatchPopulateAccessibilityEvent(event);
        }
        event.getText().clear();
        Time time = new Time();
        time.hour = getHours();
        time.minute = getMinutes();
        long millis = time.normalize(true);
        int flags = 1;
        if (this.mIs24HourMode) {
            flags = 1 | 128;
        }
        String timeString = DateUtils.formatDateTime(getContext(), millis, flags);
        event.getText().add(timeString);
        return true;
    }

    @Override
    @SuppressLint({"NewApi"})
    public boolean performAccessibilityAction(int action, Bundle arguments) {
        int maxValue;
        if (super.performAccessibilityAction(action, arguments)) {
            return true;
        }
        int changeMultiplier = 0;
        if (action == 4096) {
            changeMultiplier = 1;
        } else if (action == 8192) {
            changeMultiplier = -1;
        }
        if (changeMultiplier == 0) {
            return false;
        }
        int value = getCurrentlyShowingValue();
        int stepSize = 0;
        int currentItemShowing = getCurrentItemShowing();
        if (currentItemShowing == 0) {
            stepSize = 30;
            value %= 12;
        } else if (currentItemShowing == 1) {
            stepSize = 6;
        }
        int degrees = value * stepSize;
        int value2 = snapOnly30s(degrees, changeMultiplier) / stepSize;
        int minValue = 0;
        if (currentItemShowing == 0) {
            if (this.mIs24HourMode) {
                maxValue = 23;
            } else {
                maxValue = 12;
                minValue = 1;
            }
        } else {
            maxValue = 55;
        }
        if (value2 > maxValue) {
            value2 = minValue;
        } else if (value2 < minValue) {
            value2 = maxValue;
        }
        setItem(currentItemShowing, value2);
        this.mListener.onValueSelected(currentItemShowing, value2, false);
        return true;
    }
}
