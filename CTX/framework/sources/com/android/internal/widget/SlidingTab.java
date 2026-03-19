package com.android.internal.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.R;

public class SlidingTab extends ViewGroup {
    private static final int ANIM_DURATION = 250;
    private static final int ANIM_TARGET_TIME = 500;
    private static final boolean DBG = false;
    private static final int HORIZONTAL = 0;
    private static final String LOG_TAG = "SlidingTab";
    private static final float THRESHOLD = 0.6666667f;
    private static final int TRACKING_MARGIN = 50;
    private static final int VERTICAL = 1;
    private static final long VIBRATE_LONG = 40;
    private static final long VIBRATE_SHORT = 30;
    private static final AudioAttributes VIBRATION_ATTRIBUTES = new AudioAttributes.Builder().setContentType(4).setUsage(13).build();
    private boolean mAnimating;
    private final Animation.AnimationListener mAnimationDoneListener;
    private Slider mCurrentSlider;
    private final float mDensity;
    private int mGrabbedState;
    private boolean mHoldLeftOnTransition;
    private boolean mHoldRightOnTransition;
    private final Slider mLeftSlider;
    private OnTriggerListener mOnTriggerListener;
    private final int mOrientation;
    private Slider mOtherSlider;
    private final Slider mRightSlider;
    private float mThreshold;
    private final Rect mTmpRect;
    private boolean mTracking;
    private boolean mTriggered;
    private Vibrator mVibrator;

    public interface OnTriggerListener {
        public static final int LEFT_HANDLE = 1;
        public static final int NO_HANDLE = 0;
        public static final int RIGHT_HANDLE = 2;

        void onGrabbedStateChange(View view, int i);

        void onTrigger(View view, int i);
    }

    private static class Slider {
        public static final int ALIGN_BOTTOM = 3;
        public static final int ALIGN_LEFT = 0;
        public static final int ALIGN_RIGHT = 1;
        public static final int ALIGN_TOP = 2;
        public static final int ALIGN_UNKNOWN = 4;
        private static final int STATE_ACTIVE = 2;
        private static final int STATE_NORMAL = 0;
        private static final int STATE_PRESSED = 1;
        private int alignment_value;
        private final ImageView tab;
        private final ImageView target;
        private final TextView text;
        private int currentState = 0;
        private int alignment = 4;

        Slider(ViewGroup viewGroup, int i, int i2, int i3) {
            this.tab = new ImageView(viewGroup.getContext());
            this.tab.setBackgroundResource(i);
            this.tab.setScaleType(ImageView.ScaleType.CENTER);
            this.tab.setLayoutParams(new ViewGroup.LayoutParams(-2, -2));
            this.text = new TextView(viewGroup.getContext());
            this.text.setLayoutParams(new ViewGroup.LayoutParams(-2, -1));
            this.text.setBackgroundResource(i2);
            this.text.setTextAppearance(viewGroup.getContext(), R.style.TextAppearance_SlidingTabNormal);
            this.target = new ImageView(viewGroup.getContext());
            this.target.setImageResource(i3);
            this.target.setScaleType(ImageView.ScaleType.CENTER);
            this.target.setLayoutParams(new ViewGroup.LayoutParams(-2, -2));
            this.target.setVisibility(4);
            viewGroup.addView(this.target);
            viewGroup.addView(this.tab);
            viewGroup.addView(this.text);
        }

        void setIcon(int i) {
            this.tab.setImageResource(i);
        }

        void setTabBackgroundResource(int i) {
            this.tab.setBackgroundResource(i);
        }

        void setBarBackgroundResource(int i) {
            this.text.setBackgroundResource(i);
        }

        void setHintText(int i) {
            this.text.setText(i);
        }

        void hide() {
            int right;
            int bottom = 0;
            boolean z = this.alignment == 0 || this.alignment == 1;
            if (z) {
                right = this.alignment == 0 ? this.alignment_value - this.tab.getRight() : this.alignment_value - this.tab.getLeft();
            } else {
                right = 0;
            }
            if (!z) {
                bottom = this.alignment == 2 ? this.alignment_value - this.tab.getBottom() : this.alignment_value - this.tab.getTop();
            }
            TranslateAnimation translateAnimation = new TranslateAnimation(0.0f, right, 0.0f, bottom);
            translateAnimation.setDuration(250L);
            translateAnimation.setFillAfter(true);
            this.tab.startAnimation(translateAnimation);
            this.text.startAnimation(translateAnimation);
            this.target.setVisibility(4);
        }

        void show(boolean z) {
            int width;
            this.text.setVisibility(0);
            this.tab.setVisibility(0);
            if (z) {
                boolean z2 = true;
                if (this.alignment != 0 && this.alignment != 1) {
                    z2 = false;
                }
                if (z2) {
                    width = this.alignment == 0 ? this.tab.getWidth() : -this.tab.getWidth();
                } else {
                    width = 0;
                }
                TranslateAnimation translateAnimation = new TranslateAnimation(-width, 0.0f, -(z2 ? 0 : this.alignment == 2 ? this.tab.getHeight() : -this.tab.getHeight()), 0.0f);
                translateAnimation.setDuration(250L);
                this.tab.startAnimation(translateAnimation);
                this.text.startAnimation(translateAnimation);
            }
        }

        void setState(int i) {
            this.text.setPressed(i == 1);
            this.tab.setPressed(i == 1);
            if (i == 2) {
                int[] iArr = {16842914};
                if (this.text.getBackground().isStateful()) {
                    this.text.getBackground().setState(iArr);
                }
                if (this.tab.getBackground().isStateful()) {
                    this.tab.getBackground().setState(iArr);
                }
                this.text.setTextAppearance(this.text.getContext(), R.style.TextAppearance_SlidingTabActive);
            } else {
                this.text.setTextAppearance(this.text.getContext(), R.style.TextAppearance_SlidingTabNormal);
            }
            this.currentState = i;
        }

        void showTarget() {
            AlphaAnimation alphaAnimation = new AlphaAnimation(0.0f, 1.0f);
            alphaAnimation.setDuration(500L);
            this.target.startAnimation(alphaAnimation);
            this.target.setVisibility(0);
        }

        void reset(boolean z) {
            int left;
            int top;
            setState(0);
            this.text.setVisibility(0);
            this.text.setTextAppearance(this.text.getContext(), R.style.TextAppearance_SlidingTabNormal);
            this.tab.setVisibility(0);
            this.target.setVisibility(4);
            boolean z2 = true;
            if (this.alignment != 0 && this.alignment != 1) {
                z2 = false;
            }
            if (z2) {
                left = this.alignment == 0 ? this.alignment_value - this.tab.getLeft() : this.alignment_value - this.tab.getRight();
            } else {
                left = 0;
            }
            if (!z2) {
                top = this.alignment == 2 ? this.alignment_value - this.tab.getTop() : this.alignment_value - this.tab.getBottom();
            } else {
                top = 0;
            }
            if (z) {
                TranslateAnimation translateAnimation = new TranslateAnimation(0.0f, left, 0.0f, top);
                translateAnimation.setDuration(250L);
                translateAnimation.setFillAfter(false);
                this.text.startAnimation(translateAnimation);
                this.tab.startAnimation(translateAnimation);
                return;
            }
            if (z2) {
                this.text.offsetLeftAndRight(left);
                this.tab.offsetLeftAndRight(left);
            } else {
                this.text.offsetTopAndBottom(top);
                this.tab.offsetTopAndBottom(top);
            }
            this.text.clearAnimation();
            this.tab.clearAnimation();
            this.target.clearAnimation();
        }

        void setTarget(int i) {
            this.target.setImageResource(i);
        }

        void layout(int i, int i2, int i3, int i4, int i5) {
            this.alignment = i5;
            Drawable background = this.tab.getBackground();
            int intrinsicWidth = background.getIntrinsicWidth();
            int intrinsicHeight = background.getIntrinsicHeight();
            Drawable drawable = this.target.getDrawable();
            int intrinsicWidth2 = drawable.getIntrinsicWidth();
            int intrinsicHeight2 = drawable.getIntrinsicHeight();
            int i6 = i3 - i;
            int i7 = i4 - i2;
            float f = i6;
            int i8 = intrinsicWidth / 2;
            int i9 = (((int) (SlidingTab.THRESHOLD * f)) - intrinsicWidth2) + i8;
            int i10 = ((int) (f * 0.3333333f)) - i8;
            int i11 = i6 - intrinsicWidth;
            int i12 = i11 / 2;
            int i13 = i12 + intrinsicWidth;
            if (i5 == 0 || i5 == 1) {
                int i14 = (i7 - intrinsicHeight2) / 2;
                int i15 = intrinsicHeight2 + i14;
                int i16 = (i7 - intrinsicHeight) / 2;
                int i17 = (i7 + intrinsicHeight) / 2;
                if (i5 == 0) {
                    this.tab.layout(0, i16, intrinsicWidth, i17);
                    this.text.layout(0 - i6, i16, 0, i17);
                    this.text.setGravity(5);
                    this.target.layout(i9, i14, intrinsicWidth2 + i9, i15);
                    this.alignment_value = i;
                    return;
                }
                this.tab.layout(i11, i16, i6, i17);
                this.text.layout(i6, i16, i6 + i6, i17);
                this.target.layout(i10, i14, i10 + intrinsicWidth2, i15);
                this.text.setGravity(48);
                this.alignment_value = i3;
                return;
            }
            int i18 = (i6 - intrinsicWidth2) / 2;
            int i19 = (i6 + intrinsicWidth2) / 2;
            float f2 = i7;
            int i20 = intrinsicHeight / 2;
            int i21 = (((int) (SlidingTab.THRESHOLD * f2)) + i20) - intrinsicHeight2;
            int i22 = ((int) (f2 * 0.3333333f)) - i20;
            if (i5 == 2) {
                this.tab.layout(i12, 0, i13, intrinsicHeight);
                this.text.layout(i12, 0 - i7, i13, 0);
                this.target.layout(i18, i21, i19, intrinsicHeight2 + i21);
                this.alignment_value = i2;
                return;
            }
            this.tab.layout(i12, i7 - intrinsicHeight, i13, i7);
            this.text.layout(i12, i7, i13, i7 + i7);
            this.target.layout(i18, i22, i19, intrinsicHeight2 + i22);
            this.alignment_value = i4;
        }

        public void updateDrawableStates() {
            setState(this.currentState);
        }

        public void measure(int i, int i2) {
            int size = View.MeasureSpec.getSize(i);
            int size2 = View.MeasureSpec.getSize(i2);
            this.tab.measure(View.MeasureSpec.makeSafeMeasureSpec(size, 0), View.MeasureSpec.makeSafeMeasureSpec(size2, 0));
            this.text.measure(View.MeasureSpec.makeSafeMeasureSpec(size, 0), View.MeasureSpec.makeSafeMeasureSpec(size2, 0));
        }

        public int getTabWidth() {
            return this.tab.getMeasuredWidth();
        }

        public int getTabHeight() {
            return this.tab.getMeasuredHeight();
        }

        public void startAnimation(Animation animation, Animation animation2) {
            this.tab.startAnimation(animation);
            this.text.startAnimation(animation2);
        }

        public void hideTarget() {
            this.target.clearAnimation();
            this.target.setVisibility(4);
        }
    }

    public SlidingTab(Context context) {
        this(context, null);
    }

    public SlidingTab(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mHoldLeftOnTransition = true;
        this.mHoldRightOnTransition = true;
        this.mGrabbedState = 0;
        this.mTriggered = false;
        this.mAnimationDoneListener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                SlidingTab.this.onAnimationDone();
            }
        };
        this.mTmpRect = new Rect();
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.SlidingTab);
        this.mOrientation = typedArrayObtainStyledAttributes.getInt(0, 0);
        typedArrayObtainStyledAttributes.recycle();
        this.mDensity = getResources().getDisplayMetrics().density;
        this.mLeftSlider = new Slider(this, R.drawable.jog_tab_left_generic, R.drawable.jog_tab_bar_left_generic, R.drawable.jog_tab_target_gray);
        this.mRightSlider = new Slider(this, R.drawable.jog_tab_right_generic, R.drawable.jog_tab_bar_right_generic, R.drawable.jog_tab_target_gray);
    }

    @Override
    protected void onMeasure(int i, int i2) {
        int iMax;
        int iMax2;
        View.MeasureSpec.getMode(i);
        int size = View.MeasureSpec.getSize(i);
        View.MeasureSpec.getMode(i2);
        int size2 = View.MeasureSpec.getSize(i2);
        this.mLeftSlider.measure(i, i2);
        this.mRightSlider.measure(i, i2);
        int tabWidth = this.mLeftSlider.getTabWidth();
        int tabWidth2 = this.mRightSlider.getTabWidth();
        int tabHeight = this.mLeftSlider.getTabHeight();
        int tabHeight2 = this.mRightSlider.getTabHeight();
        if (isHorizontal()) {
            iMax = Math.max(size, tabWidth + tabWidth2);
            iMax2 = Math.max(tabHeight, tabHeight2);
        } else {
            iMax = Math.max(tabWidth, tabHeight2);
            iMax2 = Math.max(size2, tabHeight + tabHeight2);
        }
        setMeasuredDimension(iMax, iMax2);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        float x = motionEvent.getX();
        float y = motionEvent.getY();
        if (this.mAnimating) {
            return false;
        }
        this.mLeftSlider.tab.getHitRect(this.mTmpRect);
        int i = (int) x;
        int i2 = (int) y;
        boolean zContains = this.mTmpRect.contains(i, i2);
        this.mRightSlider.tab.getHitRect(this.mTmpRect);
        boolean zContains2 = this.mTmpRect.contains(i, i2);
        if (!this.mTracking && !zContains && !zContains2) {
            return false;
        }
        if (action == 0) {
            this.mTracking = true;
            this.mTriggered = false;
            vibrate(VIBRATE_SHORT);
            if (zContains) {
                this.mCurrentSlider = this.mLeftSlider;
                this.mOtherSlider = this.mRightSlider;
                this.mThreshold = isHorizontal() ? 0.6666667f : 0.3333333f;
                setGrabbedState(1);
            } else {
                this.mCurrentSlider = this.mRightSlider;
                this.mOtherSlider = this.mLeftSlider;
                this.mThreshold = isHorizontal() ? 0.3333333f : 0.6666667f;
                setGrabbedState(2);
            }
            this.mCurrentSlider.setState(1);
            this.mCurrentSlider.showTarget();
            this.mOtherSlider.hide();
        }
        return true;
    }

    public void reset(boolean z) {
        this.mLeftSlider.reset(z);
        this.mRightSlider.reset(z);
        if (!z) {
            this.mAnimating = false;
        }
    }

    @Override
    public void setVisibility(int i) {
        if (i != getVisibility() && i == 4) {
            reset(false);
        }
        super.setVisibility(i);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (this.mTracking) {
            int action = motionEvent.getAction();
            float x = motionEvent.getX();
            float y = motionEvent.getY();
            switch (action) {
                case 2:
                    if (withinView(x, y, this)) {
                        moveHandle(x, y);
                        if (!isHorizontal()) {
                            x = y;
                        }
                        float width = this.mThreshold * (isHorizontal() ? getWidth() : getHeight());
                        boolean z = !isHorizontal() ? this.mCurrentSlider != this.mLeftSlider ? x <= width : x >= width : this.mCurrentSlider != this.mLeftSlider ? x >= width : x <= width;
                        if (!this.mTriggered && z) {
                            this.mTriggered = true;
                            this.mTracking = false;
                            this.mCurrentSlider.setState(2);
                            boolean z2 = this.mCurrentSlider == this.mLeftSlider;
                            dispatchTriggerEvent(z2 ? 1 : 2);
                            startAnimating(z2 ? this.mHoldLeftOnTransition : this.mHoldRightOnTransition);
                            setGrabbedState(0);
                        }
                        break;
                    }
                case 1:
                case 3:
                    cancelGrab();
                    break;
            }
        }
        return this.mTracking || super.onTouchEvent(motionEvent);
    }

    private void cancelGrab() {
        this.mTracking = false;
        this.mTriggered = false;
        this.mOtherSlider.show(true);
        this.mCurrentSlider.reset(false);
        this.mCurrentSlider.hideTarget();
        this.mCurrentSlider = null;
        this.mOtherSlider = null;
        setGrabbedState(0);
    }

    void startAnimating(final boolean z) {
        final int i;
        this.mAnimating = true;
        Slider slider = this.mCurrentSlider;
        Slider slider2 = this.mOtherSlider;
        final int i2 = 0;
        if (isHorizontal()) {
            int right = slider.tab.getRight();
            int width = slider.tab.getWidth();
            int left = slider.tab.getLeft();
            int width2 = getWidth();
            if (z) {
                width = 0;
            }
            i = slider == this.mRightSlider ? -((right + width2) - width) : ((width2 - left) + width2) - width;
        } else {
            int top = slider.tab.getTop();
            int bottom = slider.tab.getBottom();
            int height = slider.tab.getHeight();
            int height2 = getHeight();
            if (z) {
                height = 0;
            }
            i2 = slider == this.mRightSlider ? (top + height2) - height : -(((height2 - bottom) + height2) - height);
            i = 0;
        }
        float f = i;
        float f2 = i2;
        TranslateAnimation translateAnimation = new TranslateAnimation(0.0f, f, 0.0f, f2);
        translateAnimation.setDuration(250L);
        translateAnimation.setInterpolator(new LinearInterpolator());
        translateAnimation.setFillAfter(true);
        TranslateAnimation translateAnimation2 = new TranslateAnimation(0.0f, f, 0.0f, f2);
        translateAnimation2.setDuration(250L);
        translateAnimation2.setInterpolator(new LinearInterpolator());
        translateAnimation2.setFillAfter(true);
        translateAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                Animation alphaAnimation;
                if (z) {
                    alphaAnimation = new TranslateAnimation(i, i, i2, i2);
                    alphaAnimation.setDuration(1000L);
                    SlidingTab.this.mAnimating = false;
                } else {
                    alphaAnimation = new AlphaAnimation(0.5f, 1.0f);
                    alphaAnimation.setDuration(250L);
                    SlidingTab.this.resetView();
                }
                alphaAnimation.setAnimationListener(SlidingTab.this.mAnimationDoneListener);
                SlidingTab.this.mLeftSlider.startAnimation(alphaAnimation, alphaAnimation);
                SlidingTab.this.mRightSlider.startAnimation(alphaAnimation, alphaAnimation);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationStart(Animation animation) {
            }
        });
        slider.hideTarget();
        slider.startAnimation(translateAnimation, translateAnimation2);
    }

    private void onAnimationDone() {
        resetView();
        this.mAnimating = false;
    }

    private boolean withinView(float f, float f2, View view) {
        return (isHorizontal() && f2 > -50.0f && f2 < ((float) (view.getHeight() + 50))) || (!isHorizontal() && f > -50.0f && f < ((float) (50 + view.getWidth())));
    }

    private boolean isHorizontal() {
        return this.mOrientation == 0;
    }

    private void resetView() {
        this.mLeftSlider.reset(false);
        this.mRightSlider.reset(false);
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        if (z) {
            this.mLeftSlider.layout(i, i2, i3, i4, isHorizontal() ? 0 : 3);
            this.mRightSlider.layout(i, i2, i3, i4, isHorizontal() ? 1 : 2);
        }
    }

    private void moveHandle(float f, float f2) {
        ImageView imageView = this.mCurrentSlider.tab;
        TextView textView = this.mCurrentSlider.text;
        if (isHorizontal()) {
            int left = (((int) f) - imageView.getLeft()) - (imageView.getWidth() / 2);
            imageView.offsetLeftAndRight(left);
            textView.offsetLeftAndRight(left);
        } else {
            int top = (((int) f2) - imageView.getTop()) - (imageView.getHeight() / 2);
            imageView.offsetTopAndBottom(top);
            textView.offsetTopAndBottom(top);
        }
        invalidate();
    }

    public void setLeftTabResources(int i, int i2, int i3, int i4) {
        this.mLeftSlider.setIcon(i);
        this.mLeftSlider.setTarget(i2);
        this.mLeftSlider.setBarBackgroundResource(i3);
        this.mLeftSlider.setTabBackgroundResource(i4);
        this.mLeftSlider.updateDrawableStates();
    }

    public void setLeftHintText(int i) {
        if (isHorizontal()) {
            this.mLeftSlider.setHintText(i);
        }
    }

    public void setRightTabResources(int i, int i2, int i3, int i4) {
        this.mRightSlider.setIcon(i);
        this.mRightSlider.setTarget(i2);
        this.mRightSlider.setBarBackgroundResource(i3);
        this.mRightSlider.setTabBackgroundResource(i4);
        this.mRightSlider.updateDrawableStates();
    }

    public void setRightHintText(int i) {
        if (isHorizontal()) {
            this.mRightSlider.setHintText(i);
        }
    }

    public void setHoldAfterTrigger(boolean z, boolean z2) {
        this.mHoldLeftOnTransition = z;
        this.mHoldRightOnTransition = z2;
    }

    private synchronized void vibrate(long j) {
        boolean z = true;
        if (Settings.System.getIntForUser(this.mContext.getContentResolver(), Settings.System.HAPTIC_FEEDBACK_ENABLED, 1, -2) == 0) {
            z = false;
        }
        if (z) {
            if (this.mVibrator == null) {
                this.mVibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
            }
            this.mVibrator.vibrate(j, VIBRATION_ATTRIBUTES);
        }
    }

    public void setOnTriggerListener(OnTriggerListener onTriggerListener) {
        this.mOnTriggerListener = onTriggerListener;
    }

    private void dispatchTriggerEvent(int i) {
        vibrate(VIBRATE_LONG);
        if (this.mOnTriggerListener != null) {
            this.mOnTriggerListener.onTrigger(this, i);
        }
    }

    @Override
    protected void onVisibilityChanged(View view, int i) {
        super.onVisibilityChanged(view, i);
        if (view == this && i != 0 && this.mGrabbedState != 0) {
            cancelGrab();
        }
    }

    private void setGrabbedState(int i) {
        if (i != this.mGrabbedState) {
            this.mGrabbedState = i;
            if (this.mOnTriggerListener != null) {
                this.mOnTriggerListener.onGrabbedStateChange(this, this.mGrabbedState);
            }
        }
    }

    private void log(String str) {
        Log.d(LOG_TAG, str);
    }
}
