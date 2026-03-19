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

    public RadialPickerLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mIsTouchingAmOrPm = -1;
        this.mHandler = new Handler();
        setOnTouchListener(this);
        this.TOUCH_SLOP = ViewConfiguration.get(context).getScaledTouchSlop();
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
    public void onMeasure(int i, int i2) {
        int size = View.MeasureSpec.getSize(i);
        int mode = View.MeasureSpec.getMode(i);
        int size2 = View.MeasureSpec.getSize(i2);
        int mode2 = View.MeasureSpec.getMode(i2);
        int iMin = Math.min(size, size2);
        super.onMeasure(View.MeasureSpec.makeMeasureSpec(iMin, mode), View.MeasureSpec.makeMeasureSpec(iMin, mode2));
    }

    public void setOnValueSelectedListener(OnValueSelectedListener onValueSelectedListener) {
        this.mListener = onValueSelectedListener;
    }

    public void initialize(Context context, HapticFeedbackController hapticFeedbackController, int i, int i2, boolean z) {
        char c;
        String str;
        if (this.mTimeInitialized) {
            Log.e("RadialPickerLayout", "Time has already been initialized.");
            return;
        }
        this.mHapticFeedbackController = hapticFeedbackController;
        this.mIs24HourMode = z;
        int i3 = 1;
        this.mHideAmPm = this.mAccessibilityManager.isTouchExplorationEnabled() ? true : this.mIs24HourMode;
        this.mCircleView.initialize(context, this.mHideAmPm);
        this.mCircleView.invalidate();
        int i4 = 12;
        if (!this.mHideAmPm) {
            this.mAmPmCirclesView.initialize(context, i < 12 ? 0 : 1);
            this.mAmPmCirclesView.invalidate();
        }
        Resources resources = context.getResources();
        int[] iArr = {12, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
        int[] iArr2 = {0, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23};
        int[] iArr3 = {0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55};
        String[] strArr = new String[12];
        String[] strArr2 = new String[12];
        String[] strArr3 = new String[12];
        int i5 = 0;
        while (i5 < i4) {
            if (z) {
                Object[] objArr = new Object[i3];
                c = 0;
                objArr[0] = Integer.valueOf(iArr2[i5]);
                str = String.format("%02d", objArr);
            } else {
                c = 0;
                str = String.format("%d", Integer.valueOf(iArr[i5]));
            }
            strArr[i5] = str;
            Object[] objArr2 = new Object[1];
            objArr2[c] = Integer.valueOf(iArr[i5]);
            strArr2[i5] = String.format("%d", objArr2);
            Object[] objArr3 = new Object[1];
            objArr3[c] = Integer.valueOf(iArr3[i5]);
            strArr3[i5] = String.format("%02d", objArr3);
            i5++;
            i4 = 12;
            i3 = 1;
        }
        this.mHourRadialTextsView.initialize(resources, strArr, z ? strArr2 : null, this.mHideAmPm, true);
        this.mHourRadialTextsView.invalidate();
        this.mMinuteRadialTextsView.initialize(resources, strArr3, null, this.mHideAmPm, false);
        this.mMinuteRadialTextsView.invalidate();
        setValueForItem(0, i);
        setValueForItem(1, i2);
        this.mHourRadialSelectorView.initialize(context, this.mHideAmPm, z, true, (i % 12) * 30, isHourInnerCircle(i));
        this.mMinuteRadialSelectorView.initialize(context, this.mHideAmPm, false, false, i2 * 6, false);
        this.mTimeInitialized = true;
    }

    void setTheme(Context context, boolean z) {
        this.mCircleView.setTheme(context, z);
        this.mAmPmCirclesView.setTheme(context, z);
        this.mHourRadialTextsView.setTheme(context, z);
        this.mMinuteRadialTextsView.setTheme(context, z);
        this.mHourRadialSelectorView.setTheme(context, z);
        this.mMinuteRadialSelectorView.setTheme(context, z);
    }

    public void setTime(int i, int i2) {
        setItem(0, i);
        setItem(1, i2);
    }

    private void setItem(int i, int i2) {
        if (i == 0) {
            setValueForItem(0, i2);
            this.mHourRadialSelectorView.setSelection((i2 % 12) * 30, isHourInnerCircle(i2), false);
            this.mHourRadialSelectorView.invalidate();
            return;
        }
        if (i == 1) {
            setValueForItem(1, i2);
            this.mMinuteRadialSelectorView.setSelection(i2 * 6, false, false);
            this.mMinuteRadialSelectorView.invalidate();
        }
    }

    private boolean isHourInnerCircle(int i) {
        return this.mIs24HourMode && i <= 12 && i != 0;
    }

    public int getHours() {
        return this.mCurrentHoursOfDay;
    }

    public int getMinutes() {
        return this.mCurrentMinutes;
    }

    private int getCurrentlyShowingValue() {
        int currentItemShowing = getCurrentItemShowing();
        if (currentItemShowing == 0) {
            return this.mCurrentHoursOfDay;
        }
        if (currentItemShowing == 1) {
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

    private void setValueForItem(int i, int i2) {
        if (i == 0) {
            this.mCurrentHoursOfDay = i2;
            return;
        }
        if (i == 1) {
            this.mCurrentMinutes = i2;
            return;
        }
        if (i == 2) {
            if (i2 == 0) {
                this.mCurrentHoursOfDay %= 12;
            } else if (i2 == 1) {
                this.mCurrentHoursOfDay = (this.mCurrentHoursOfDay % 12) + 12;
            }
        }
    }

    public void setAmOrPm(int i) {
        this.mAmPmCirclesView.setAmOrPm(i);
        this.mAmPmCirclesView.invalidate();
        setValueForItem(2, i);
    }

    private void preparePrefer30sMap() {
        int i;
        this.mSnapPrefer30sMap = new int[361];
        int i2 = 1;
        int i3 = 8;
        int i4 = 0;
        for (int i5 = 0; i5 < 361; i5++) {
            this.mSnapPrefer30sMap[i5] = i4;
            if (i2 == i3) {
                i4 += 6;
                if (i4 == 360) {
                    i = 7;
                } else if (i4 % 30 == 0) {
                    i = 14;
                } else {
                    i = 4;
                }
                i3 = i;
                i2 = 1;
            } else {
                i2++;
            }
        }
    }

    private int snapPrefer30s(int i) {
        if (this.mSnapPrefer30sMap == null) {
            return -1;
        }
        return this.mSnapPrefer30sMap[i];
    }

    private static int snapOnly30s(int i, int i2) {
        int i3 = (i / 30) * 30;
        int i4 = i3 + 30;
        if (i2 != 1) {
            if (i2 == -1) {
                return i == i3 ? i3 - 30 : i3;
            }
            if (i - i3 < i4 - i) {
                return i3;
            }
        }
        return i4;
    }

    private int reselectSelector(int i, boolean z, boolean z2, boolean z3) {
        int iSnapOnly30s;
        RadialSelectorView radialSelectorView;
        int i2;
        if (i == -1) {
            return -1;
        }
        int currentItemShowing = getCurrentItemShowing();
        if (!z2 && currentItemShowing == 1) {
            iSnapOnly30s = snapPrefer30s(i);
        } else {
            iSnapOnly30s = snapOnly30s(i, 0);
        }
        if (currentItemShowing == 0) {
            radialSelectorView = this.mHourRadialSelectorView;
            i2 = 30;
        } else {
            radialSelectorView = this.mMinuteRadialSelectorView;
            i2 = 6;
        }
        radialSelectorView.setSelection(iSnapOnly30s, z, z3);
        radialSelectorView.invalidate();
        int i3 = 360;
        if (currentItemShowing == 0) {
            if (this.mIs24HourMode) {
                if (iSnapOnly30s != 0 || !z) {
                    i3 = (iSnapOnly30s != 360 || z) ? iSnapOnly30s : 0;
                }
            } else if (iSnapOnly30s != 0) {
            }
        } else if (iSnapOnly30s != 360 || currentItemShowing != 1) {
        }
        int i4 = i3 / i2;
        if (currentItemShowing == 0 && this.mIs24HourMode && !z && i3 != 0) {
            return i4 + 12;
        }
        return i4;
    }

    private int getDegreesFromCoords(float f, float f2, boolean z, Boolean[] boolArr) {
        int currentItemShowing = getCurrentItemShowing();
        if (currentItemShowing == 0) {
            return this.mHourRadialSelectorView.getDegreesFromCoords(f, f2, z, boolArr);
        }
        if (currentItemShowing == 1) {
            return this.mMinuteRadialSelectorView.getDegreesFromCoords(f, f2, z, boolArr);
        }
        return -1;
    }

    public int getCurrentItemShowing() {
        if (this.mCurrentItemShowing != 0 && this.mCurrentItemShowing != 1) {
            Log.e("RadialPickerLayout", "Current item showing was unfortunately set to " + this.mCurrentItemShowing);
            return -1;
        }
        return this.mCurrentItemShowing;
    }

    public void setCurrentItemShowing(int i, boolean z) {
        if (i != 0 && i != 1) {
            Log.e("RadialPickerLayout", "TimePicker does not support view at index " + i);
            return;
        }
        int currentItemShowing = getCurrentItemShowing();
        this.mCurrentItemShowing = i;
        if (z && i != currentItemShowing) {
            ObjectAnimator[] objectAnimatorArr = new ObjectAnimator[4];
            if (i == 1) {
                objectAnimatorArr[0] = this.mHourRadialTextsView.getDisappearAnimator();
                objectAnimatorArr[1] = this.mHourRadialSelectorView.getDisappearAnimator();
                objectAnimatorArr[2] = this.mMinuteRadialTextsView.getReappearAnimator();
                objectAnimatorArr[3] = this.mMinuteRadialSelectorView.getReappearAnimator();
            } else if (i == 0) {
                objectAnimatorArr[0] = this.mHourRadialTextsView.getReappearAnimator();
                objectAnimatorArr[1] = this.mHourRadialSelectorView.getReappearAnimator();
                objectAnimatorArr[2] = this.mMinuteRadialTextsView.getDisappearAnimator();
                objectAnimatorArr[3] = this.mMinuteRadialSelectorView.getDisappearAnimator();
            }
            if (this.mTransition != null && this.mTransition.isRunning()) {
                this.mTransition.end();
            }
            this.mTransition = new AnimatorSet();
            this.mTransition.playTogether(objectAnimatorArr);
            this.mTransition.start();
            return;
        }
        int i2 = 255;
        int i3 = i == 0 ? 255 : 0;
        if (i != 1) {
            i2 = 0;
        }
        float f = i3;
        this.mHourRadialTextsView.setAlpha(f);
        this.mHourRadialSelectorView.setAlpha(f);
        float f2 = i2;
        this.mMinuteRadialTextsView.setAlpha(f2);
        this.mMinuteRadialSelectorView.setAlpha(f2);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        int degreesFromCoords;
        int iReselectSelector;
        float x = motionEvent.getX();
        float y = motionEvent.getY();
        final Boolean[] boolArr = {false};
        switch (motionEvent.getAction()) {
            case 0:
                if (!this.mInputEnabled) {
                    return true;
                }
                this.mDownX = x;
                this.mDownY = y;
                this.mLastValueSelected = -1;
                this.mDoingMove = false;
                this.mDoingTouch = true;
                if (!this.mHideAmPm) {
                    this.mIsTouchingAmOrPm = this.mAmPmCirclesView.getIsTouchingAmOrPm(x, y);
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
                    this.mDownDegrees = getDegreesFromCoords(x, y, this.mAccessibilityManager.isTouchExplorationEnabled(), boolArr);
                    if (this.mDownDegrees != -1) {
                        this.mHapticFeedbackController.tryVibrate();
                        this.mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                RadialPickerLayout.this.mDoingMove = true;
                                int iReselectSelector2 = RadialPickerLayout.this.reselectSelector(RadialPickerLayout.this.mDownDegrees, boolArr[0].booleanValue(), false, true);
                                RadialPickerLayout.this.mLastValueSelected = iReselectSelector2;
                                RadialPickerLayout.this.mListener.onValueSelected(RadialPickerLayout.this.getCurrentItemShowing(), iReselectSelector2, false);
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
                    int isTouchingAmOrPm = this.mAmPmCirclesView.getIsTouchingAmOrPm(x, y);
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
                if (this.mDownDegrees != -1 && (degreesFromCoords = getDegreesFromCoords(x, y, this.mDoingMove, boolArr)) != -1) {
                    int iReselectSelector2 = reselectSelector(degreesFromCoords, boolArr[0].booleanValue(), !this.mDoingMove, false);
                    if (getCurrentItemShowing() == 0 && !this.mIs24HourMode) {
                        int isCurrentlyAmOrPm = getIsCurrentlyAmOrPm();
                        if (isCurrentlyAmOrPm != 0 || iReselectSelector2 != 12) {
                            if (isCurrentlyAmOrPm == 1 && iReselectSelector2 != 12) {
                                iReselectSelector2 += 12;
                            }
                        } else {
                            iReselectSelector2 = 0;
                        }
                    }
                    setValueForItem(getCurrentItemShowing(), iReselectSelector2);
                    this.mListener.onValueSelected(getCurrentItemShowing(), iReselectSelector2, true);
                }
                this.mDoingMove = false;
                return true;
            case 2:
                if (!this.mInputEnabled) {
                    Log.e("RadialPickerLayout", "Input was disabled, but received ACTION_MOVE.");
                    return true;
                }
                float fAbs = Math.abs(y - this.mDownY);
                float fAbs2 = Math.abs(x - this.mDownX);
                if (this.mDoingMove || fAbs2 > this.TOUCH_SLOP || fAbs > this.TOUCH_SLOP) {
                    if (this.mIsTouchingAmOrPm == 0 || this.mIsTouchingAmOrPm == 1) {
                        this.mHandler.removeCallbacksAndMessages(null);
                        if (this.mAmPmCirclesView.getIsTouchingAmOrPm(x, y) != this.mIsTouchingAmOrPm) {
                            this.mAmPmCirclesView.setAmOrPmPressed(-1);
                            this.mAmPmCirclesView.invalidate();
                            this.mIsTouchingAmOrPm = -1;
                        }
                    } else if (this.mDownDegrees != -1) {
                        this.mDoingMove = true;
                        this.mHandler.removeCallbacksAndMessages(null);
                        int degreesFromCoords2 = getDegreesFromCoords(x, y, true, boolArr);
                        if (degreesFromCoords2 != -1 && (iReselectSelector = reselectSelector(degreesFromCoords2, boolArr[0].booleanValue(), false, true)) != this.mLastValueSelected) {
                            this.mHapticFeedbackController.tryVibrate();
                            this.mLastValueSelected = iReselectSelector;
                            this.mListener.onValueSelected(getCurrentItemShowing(), iReselectSelector, false);
                        }
                        return true;
                    }
                }
                return false;
            default:
                return false;
        }
    }

    public boolean trySettingInputEnabled(boolean z) {
        if (this.mDoingTouch && !z) {
            return false;
        }
        this.mInputEnabled = z;
        this.mGrayBox.setVisibility(z ? 4 : 0);
        return true;
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfo);
        accessibilityNodeInfo.addAction(4096);
        accessibilityNodeInfo.addAction(8192);
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        int i;
        if (accessibilityEvent.getEventType() == 32) {
            accessibilityEvent.getText().clear();
            Time time = new Time();
            time.hour = getHours();
            time.minute = getMinutes();
            long jNormalize = time.normalize(true);
            if (this.mIs24HourMode) {
                i = 129;
            } else {
                i = 1;
            }
            accessibilityEvent.getText().add(DateUtils.formatDateTime(getContext(), jNormalize, i));
            return true;
        }
        return super.dispatchPopulateAccessibilityEvent(accessibilityEvent);
    }

    @Override
    @SuppressLint({"NewApi"})
    public boolean performAccessibilityAction(int i, Bundle bundle) {
        int i2;
        int i3;
        int i4;
        if (super.performAccessibilityAction(i, bundle)) {
            return true;
        }
        int i5 = i == 4096 ? 1 : i == 8192 ? -1 : 0;
        if (i5 == 0) {
            return false;
        }
        int currentlyShowingValue = getCurrentlyShowingValue();
        int currentItemShowing = getCurrentItemShowing();
        if (currentItemShowing == 0) {
            i2 = 30;
            currentlyShowingValue %= 12;
        } else if (currentItemShowing == 1) {
            i2 = 6;
        } else {
            i2 = 0;
        }
        int iSnapOnly30s = snapOnly30s(currentlyShowingValue * i2, i5) / i2;
        if (currentItemShowing == 0) {
            if (this.mIs24HourMode) {
                i3 = 23;
            } else {
                i3 = 12;
                i4 = 1;
                if (iSnapOnly30s > i3) {
                    if (iSnapOnly30s < i4) {
                        iSnapOnly30s = i3;
                    }
                } else {
                    iSnapOnly30s = i4;
                }
                setItem(currentItemShowing, iSnapOnly30s);
                this.mListener.onValueSelected(currentItemShowing, iSnapOnly30s, false);
                return true;
            }
        } else {
            i3 = 55;
        }
        i4 = 0;
        if (iSnapOnly30s > i3) {
        }
        setItem(currentItemShowing, iSnapOnly30s);
        this.mListener.onValueSelected(currentItemShowing, iSnapOnly30s, false);
        return true;
    }
}
