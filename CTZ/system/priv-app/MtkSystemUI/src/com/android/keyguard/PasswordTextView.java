package com.android.keyguard;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.EditText;
import java.util.ArrayList;
import java.util.Stack;

/* loaded from: classes.dex */
public class PasswordTextView extends View {
    private static char DOT = 8226;
    private Interpolator mAppearInterpolator;
    private int mCharPadding;
    private Stack<CharState> mCharPool;
    private Interpolator mDisappearInterpolator;
    private int mDotSize;
    private final Paint mDrawPaint;
    private Interpolator mFastOutSlowInInterpolator;
    private final int mGravity;
    private PowerManager mPM;
    private boolean mShowPassword;
    private String mText;
    private ArrayList<CharState> mTextChars;
    private final int mTextHeightRaw;
    private UserActivityListener mUserActivityListener;

    public interface UserActivityListener {
        void onUserActivity();
    }

    public PasswordTextView(Context context) {
        this(context, null);
    }

    public PasswordTextView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public PasswordTextView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public PasswordTextView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mTextChars = new ArrayList<>();
        this.mText = "";
        this.mCharPool = new Stack<>();
        this.mDrawPaint = new Paint();
        setFocusableInTouchMode(true);
        setFocusable(true);
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.PasswordTextView);
        try {
            this.mTextHeightRaw = typedArrayObtainStyledAttributes.getInt(4, 0);
            this.mGravity = typedArrayObtainStyledAttributes.getInt(1, 17);
            this.mDotSize = typedArrayObtainStyledAttributes.getDimensionPixelSize(3, getContext().getResources().getDimensionPixelSize(com.android.systemui.R.dimen.password_dot_size));
            this.mCharPadding = typedArrayObtainStyledAttributes.getDimensionPixelSize(2, getContext().getResources().getDimensionPixelSize(com.android.systemui.R.dimen.password_char_padding));
            this.mDrawPaint.setColor(typedArrayObtainStyledAttributes.getColor(0, -1));
            typedArrayObtainStyledAttributes.recycle();
            this.mDrawPaint.setFlags(129);
            this.mDrawPaint.setTextAlign(Paint.Align.CENTER);
            this.mDrawPaint.setTypeface(Typeface.create(context.getString(android.R.string.android_upgrading_complete), 0));
            this.mShowPassword = Settings.System.getInt(this.mContext.getContentResolver(), "show_password", 1) == 1;
            this.mAppearInterpolator = AnimationUtils.loadInterpolator(this.mContext, android.R.interpolator.linear_out_slow_in);
            this.mDisappearInterpolator = AnimationUtils.loadInterpolator(this.mContext, android.R.interpolator.fast_out_linear_in);
            this.mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(this.mContext, android.R.interpolator.fast_out_slow_in);
            this.mPM = (PowerManager) this.mContext.getSystemService("power");
        } catch (Throwable th) {
            typedArrayObtainStyledAttributes.recycle();
            throw th;
        }
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        float width;
        float drawingWidth = getDrawingWidth();
        if ((this.mGravity & 7) == 3) {
            if ((this.mGravity & 8388608) != 0 && getLayoutDirection() == 1) {
                width = (getWidth() - getPaddingRight()) - drawingWidth;
            } else {
                width = getPaddingLeft();
            }
        } else {
            width = (getWidth() / 2) - (drawingWidth / 2.0f);
        }
        int size = this.mTextChars.size();
        Rect charBounds = getCharBounds();
        int i = charBounds.bottom - charBounds.top;
        float height = (((getHeight() - getPaddingBottom()) - getPaddingTop()) / 2) + getPaddingTop();
        canvas.clipRect(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(), getHeight() - getPaddingBottom());
        float f = charBounds.right - charBounds.left;
        float fDraw = width;
        for (int i2 = 0; i2 < size; i2++) {
            fDraw += this.mTextChars.get(i2).draw(canvas, fDraw, i, height, f);
        }
    }

    @Override // android.view.View
    public boolean hasOverlappingRendering() {
        return false;
    }

    private Rect getCharBounds() {
        this.mDrawPaint.setTextSize(this.mTextHeightRaw * getResources().getDisplayMetrics().scaledDensity);
        Rect rect = new Rect();
        this.mDrawPaint.getTextBounds("0", 0, 1, rect);
        return rect;
    }

    private float getDrawingWidth() {
        int size = this.mTextChars.size();
        Rect charBounds = getCharBounds();
        int i = charBounds.right - charBounds.left;
        int i2 = 0;
        for (int i3 = 0; i3 < size; i3++) {
            CharState charState = this.mTextChars.get(i3);
            if (i3 != 0) {
                i2 = (int) (i2 + (this.mCharPadding * charState.currentWidthFactor));
            }
            i2 = (int) (i2 + (i * charState.currentWidthFactor));
        }
        return i2;
    }

    public void append(char c) {
        CharState charStateObtainCharState;
        int size = this.mTextChars.size();
        CharSequence transformedText = getTransformedText();
        this.mText += c;
        int length = this.mText.length();
        if (length > size) {
            charStateObtainCharState = obtainCharState(c);
            this.mTextChars.add(charStateObtainCharState);
        } else {
            CharState charState = this.mTextChars.get(length - 1);
            charState.whichChar = c;
            charStateObtainCharState = charState;
        }
        charStateObtainCharState.startAppearAnimation();
        if (length > 1) {
            CharState charState2 = this.mTextChars.get(length - 2);
            if (charState2.isDotSwapPending) {
                charState2.swapToDotWhenAppearFinished();
            }
        }
        userActivity();
        sendAccessibilityEventTypeViewTextChanged(transformedText, transformedText.length(), 0, 1);
    }

    public void setUserActivityListener(UserActivityListener userActivityListener) {
        this.mUserActivityListener = userActivityListener;
    }

    private void userActivity() {
        this.mPM.userActivity(SystemClock.uptimeMillis(), false);
        if (this.mUserActivityListener != null) {
            this.mUserActivityListener.onUserActivity();
        }
    }

    public void deleteLastChar() {
        int length = this.mText.length();
        CharSequence transformedText = getTransformedText();
        if (length > 0) {
            int i = length - 1;
            this.mText = this.mText.substring(0, i);
            this.mTextChars.get(i).startRemoveAnimation(0L, 0L);
            sendAccessibilityEventTypeViewTextChanged(transformedText, transformedText.length() - 1, 1, 0);
        }
        userActivity();
    }

    public String getText() {
        return this.mText;
    }

    private CharSequence getTransformedText() {
        int size = this.mTextChars.size();
        StringBuilder sb = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            CharState charState = this.mTextChars.get(i);
            if (charState.dotAnimator == null || charState.dotAnimationIsGrowing) {
                sb.append(charState.isCharVisibleForA11y() ? charState.whichChar : DOT);
            }
        }
        return sb;
    }

    private CharState obtainCharState(char c) {
        CharState charStatePop;
        if (this.mCharPool.isEmpty()) {
            charStatePop = new CharState();
        } else {
            charStatePop = this.mCharPool.pop();
            charStatePop.reset();
        }
        charStatePop.whichChar = c;
        return charStatePop;
    }

    public void reset(boolean z, boolean z2) {
        int i;
        int i2;
        CharSequence transformedText = getTransformedText();
        this.mText = "";
        int size = this.mTextChars.size();
        int i3 = size - 1;
        int i4 = i3 / 2;
        int i5 = 0;
        while (i5 < size) {
            CharState charState = this.mTextChars.get(i5);
            if (z) {
                if (i5 <= i4) {
                    i2 = i5 * 2;
                } else {
                    i2 = i3 - (((i5 - i4) - 1) * 2);
                }
                i = i5;
                charState.startRemoveAnimation(Math.min(i2 * 40, 200L), Math.min(40 * i3, 200L) + 160);
                charState.removeDotSwapCallbacks();
            } else {
                i = i5;
                this.mCharPool.push(charState);
            }
            i5 = i + 1;
        }
        if (!z) {
            this.mTextChars.clear();
        }
        if (z2) {
            sendAccessibilityEventTypeViewTextChanged(transformedText, 0, transformedText.length(), 0);
        }
    }

    void sendAccessibilityEventTypeViewTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        if (AccessibilityManager.getInstance(this.mContext).isEnabled()) {
            if (isFocused() || (isSelected() && isShown())) {
                AccessibilityEvent accessibilityEventObtain = AccessibilityEvent.obtain(16);
                accessibilityEventObtain.setFromIndex(i);
                accessibilityEventObtain.setRemovedCount(i2);
                accessibilityEventObtain.setAddedCount(i3);
                accessibilityEventObtain.setBeforeText(charSequence);
                CharSequence transformedText = getTransformedText();
                if (!TextUtils.isEmpty(transformedText)) {
                    accessibilityEventObtain.getText().add(transformedText);
                }
                accessibilityEventObtain.setPassword(true);
                sendAccessibilityEventUnchecked(accessibilityEventObtain);
            }
        }
    }

    @Override // android.view.View
    public void onInitializeAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        super.onInitializeAccessibilityEvent(accessibilityEvent);
        accessibilityEvent.setClassName(EditText.class.getName());
        accessibilityEvent.setPassword(true);
    }

    @Override // android.view.View
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfo);
        accessibilityNodeInfo.setClassName(EditText.class.getName());
        accessibilityNodeInfo.setPassword(true);
        accessibilityNodeInfo.setText(getTransformedText());
        accessibilityNodeInfo.setEditable(true);
        accessibilityNodeInfo.setInputType(16);
    }

    private class CharState {
        float currentDotSizeFactor;
        float currentTextSizeFactor;
        float currentTextTranslationY;
        float currentWidthFactor;
        boolean dotAnimationIsGrowing;
        Animator dotAnimator;
        Animator.AnimatorListener dotFinishListener;
        private ValueAnimator.AnimatorUpdateListener dotSizeUpdater;
        private Runnable dotSwapperRunnable;
        boolean isDotSwapPending;
        Animator.AnimatorListener removeEndListener;
        boolean textAnimationIsGrowing;
        ValueAnimator textAnimator;
        Animator.AnimatorListener textFinishListener;
        private ValueAnimator.AnimatorUpdateListener textSizeUpdater;
        ValueAnimator textTranslateAnimator;
        Animator.AnimatorListener textTranslateFinishListener;
        private ValueAnimator.AnimatorUpdateListener textTranslationUpdater;
        char whichChar;
        boolean widthAnimationIsGrowing;
        ValueAnimator widthAnimator;
        Animator.AnimatorListener widthFinishListener;
        private ValueAnimator.AnimatorUpdateListener widthUpdater;

        private CharState() {
            this.currentTextTranslationY = 1.0f;
            this.removeEndListener = new AnimatorListenerAdapter() { // from class: com.android.keyguard.PasswordTextView.CharState.1
                private boolean mCancelled;

                @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                public void onAnimationCancel(Animator animator) {
                    this.mCancelled = true;
                }

                @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                public void onAnimationEnd(Animator animator) {
                    if (!this.mCancelled) {
                        PasswordTextView.this.mTextChars.remove(CharState.this);
                        PasswordTextView.this.mCharPool.push(CharState.this);
                        CharState.this.reset();
                        CharState.this.cancelAnimator(CharState.this.textTranslateAnimator);
                        CharState.this.textTranslateAnimator = null;
                    }
                }

                @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                public void onAnimationStart(Animator animator) {
                    this.mCancelled = false;
                }
            };
            this.dotFinishListener = new AnimatorListenerAdapter() { // from class: com.android.keyguard.PasswordTextView.CharState.2
                @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                public void onAnimationEnd(Animator animator) {
                    CharState.this.dotAnimator = null;
                }
            };
            this.textFinishListener = new AnimatorListenerAdapter() { // from class: com.android.keyguard.PasswordTextView.CharState.3
                @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                public void onAnimationEnd(Animator animator) {
                    CharState.this.textAnimator = null;
                }
            };
            this.textTranslateFinishListener = new AnimatorListenerAdapter() { // from class: com.android.keyguard.PasswordTextView.CharState.4
                @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                public void onAnimationEnd(Animator animator) {
                    CharState.this.textTranslateAnimator = null;
                }
            };
            this.widthFinishListener = new AnimatorListenerAdapter() { // from class: com.android.keyguard.PasswordTextView.CharState.5
                @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                public void onAnimationEnd(Animator animator) {
                    CharState.this.widthAnimator = null;
                }
            };
            this.dotSizeUpdater = new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.keyguard.PasswordTextView.CharState.6
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    CharState.this.currentDotSizeFactor = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                    PasswordTextView.this.invalidate();
                }
            };
            this.textSizeUpdater = new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.keyguard.PasswordTextView.CharState.7
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    boolean zIsCharVisibleForA11y = CharState.this.isCharVisibleForA11y();
                    float f = CharState.this.currentTextSizeFactor;
                    CharState.this.currentTextSizeFactor = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                    if (zIsCharVisibleForA11y != CharState.this.isCharVisibleForA11y()) {
                        CharState.this.currentTextSizeFactor = f;
                        CharSequence transformedText = PasswordTextView.this.getTransformedText();
                        CharState.this.currentTextSizeFactor = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                        int iIndexOf = PasswordTextView.this.mTextChars.indexOf(CharState.this);
                        if (iIndexOf >= 0) {
                            PasswordTextView.this.sendAccessibilityEventTypeViewTextChanged(transformedText, iIndexOf, 1, 1);
                        }
                    }
                    PasswordTextView.this.invalidate();
                }
            };
            this.textTranslationUpdater = new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.keyguard.PasswordTextView.CharState.8
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    CharState.this.currentTextTranslationY = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                    PasswordTextView.this.invalidate();
                }
            };
            this.widthUpdater = new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.keyguard.PasswordTextView.CharState.9
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    CharState.this.currentWidthFactor = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                    PasswordTextView.this.invalidate();
                }
            };
            this.dotSwapperRunnable = new Runnable() { // from class: com.android.keyguard.PasswordTextView.CharState.10
                @Override // java.lang.Runnable
                public void run() {
                    CharState.this.performSwap();
                    CharState.this.isDotSwapPending = false;
                }
            };
        }

        void reset() {
            this.whichChar = (char) 0;
            this.currentTextSizeFactor = 0.0f;
            this.currentDotSizeFactor = 0.0f;
            this.currentWidthFactor = 0.0f;
            cancelAnimator(this.textAnimator);
            this.textAnimator = null;
            cancelAnimator(this.dotAnimator);
            this.dotAnimator = null;
            cancelAnimator(this.widthAnimator);
            this.widthAnimator = null;
            this.currentTextTranslationY = 1.0f;
            removeDotSwapCallbacks();
        }

        void startRemoveAnimation(long j, long j2) {
            boolean z = false;
            boolean z2 = (this.currentDotSizeFactor > 0.0f && this.dotAnimator == null) || (this.dotAnimator != null && this.dotAnimationIsGrowing);
            boolean z3 = (this.currentTextSizeFactor > 0.0f && this.textAnimator == null) || (this.textAnimator != null && this.textAnimationIsGrowing);
            if ((this.currentWidthFactor > 0.0f && this.widthAnimator == null) || (this.widthAnimator != null && this.widthAnimationIsGrowing)) {
                z = true;
            }
            if (z2) {
                startDotDisappearAnimation(j);
            }
            if (z3) {
                startTextDisappearAnimation(j);
            }
            if (z) {
                startWidthDisappearAnimation(j2);
            }
        }

        void startAppearAnimation() {
            boolean z = !PasswordTextView.this.mShowPassword && (this.dotAnimator == null || !this.dotAnimationIsGrowing);
            boolean z2 = PasswordTextView.this.mShowPassword && (this.textAnimator == null || !this.textAnimationIsGrowing);
            boolean z3 = this.widthAnimator == null || !this.widthAnimationIsGrowing;
            if (z) {
                startDotAppearAnimation(0L);
            }
            if (z2) {
                startTextAppearAnimation();
            }
            if (z3) {
                startWidthAppearAnimation();
            }
            if (PasswordTextView.this.mShowPassword) {
                postDotSwap(1300L);
            }
        }

        private void postDotSwap(long j) {
            removeDotSwapCallbacks();
            PasswordTextView.this.postDelayed(this.dotSwapperRunnable, j);
            this.isDotSwapPending = true;
        }

        private void removeDotSwapCallbacks() {
            PasswordTextView.this.removeCallbacks(this.dotSwapperRunnable);
            this.isDotSwapPending = false;
        }

        void swapToDotWhenAppearFinished() {
            removeDotSwapCallbacks();
            if (this.textAnimator != null) {
                postDotSwap((this.textAnimator.getDuration() - this.textAnimator.getCurrentPlayTime()) + 100);
            } else {
                performSwap();
            }
        }

        private void performSwap() {
            startTextDisappearAnimation(0L);
            startDotAppearAnimation(30L);
        }

        private void startWidthDisappearAnimation(long j) {
            cancelAnimator(this.widthAnimator);
            this.widthAnimator = ValueAnimator.ofFloat(this.currentWidthFactor, 0.0f);
            this.widthAnimator.addUpdateListener(this.widthUpdater);
            this.widthAnimator.addListener(this.widthFinishListener);
            this.widthAnimator.addListener(this.removeEndListener);
            this.widthAnimator.setDuration((long) (160.0f * this.currentWidthFactor));
            this.widthAnimator.setStartDelay(j);
            this.widthAnimator.start();
            this.widthAnimationIsGrowing = false;
        }

        private void startTextDisappearAnimation(long j) {
            cancelAnimator(this.textAnimator);
            this.textAnimator = ValueAnimator.ofFloat(this.currentTextSizeFactor, 0.0f);
            this.textAnimator.addUpdateListener(this.textSizeUpdater);
            this.textAnimator.addListener(this.textFinishListener);
            this.textAnimator.setInterpolator(PasswordTextView.this.mDisappearInterpolator);
            this.textAnimator.setDuration((long) (160.0f * this.currentTextSizeFactor));
            this.textAnimator.setStartDelay(j);
            this.textAnimator.start();
            this.textAnimationIsGrowing = false;
        }

        private void startDotDisappearAnimation(long j) {
            cancelAnimator(this.dotAnimator);
            ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(this.currentDotSizeFactor, 0.0f);
            valueAnimatorOfFloat.addUpdateListener(this.dotSizeUpdater);
            valueAnimatorOfFloat.addListener(this.dotFinishListener);
            valueAnimatorOfFloat.setInterpolator(PasswordTextView.this.mDisappearInterpolator);
            valueAnimatorOfFloat.setDuration((long) (160.0f * Math.min(this.currentDotSizeFactor, 1.0f)));
            valueAnimatorOfFloat.setStartDelay(j);
            valueAnimatorOfFloat.start();
            this.dotAnimator = valueAnimatorOfFloat;
            this.dotAnimationIsGrowing = false;
        }

        private void startWidthAppearAnimation() {
            cancelAnimator(this.widthAnimator);
            this.widthAnimator = ValueAnimator.ofFloat(this.currentWidthFactor, 1.0f);
            this.widthAnimator.addUpdateListener(this.widthUpdater);
            this.widthAnimator.addListener(this.widthFinishListener);
            this.widthAnimator.setDuration((long) (160.0f * (1.0f - this.currentWidthFactor)));
            this.widthAnimator.start();
            this.widthAnimationIsGrowing = true;
        }

        private void startTextAppearAnimation() {
            cancelAnimator(this.textAnimator);
            this.textAnimator = ValueAnimator.ofFloat(this.currentTextSizeFactor, 1.0f);
            this.textAnimator.addUpdateListener(this.textSizeUpdater);
            this.textAnimator.addListener(this.textFinishListener);
            this.textAnimator.setInterpolator(PasswordTextView.this.mAppearInterpolator);
            this.textAnimator.setDuration((long) (160.0f * (1.0f - this.currentTextSizeFactor)));
            this.textAnimator.start();
            this.textAnimationIsGrowing = true;
            if (this.textTranslateAnimator == null) {
                this.textTranslateAnimator = ValueAnimator.ofFloat(1.0f, 0.0f);
                this.textTranslateAnimator.addUpdateListener(this.textTranslationUpdater);
                this.textTranslateAnimator.addListener(this.textTranslateFinishListener);
                this.textTranslateAnimator.setInterpolator(PasswordTextView.this.mAppearInterpolator);
                this.textTranslateAnimator.setDuration(160L);
                this.textTranslateAnimator.start();
            }
        }

        private void startDotAppearAnimation(long j) {
            cancelAnimator(this.dotAnimator);
            if (!PasswordTextView.this.mShowPassword) {
                ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(this.currentDotSizeFactor, 1.5f);
                valueAnimatorOfFloat.addUpdateListener(this.dotSizeUpdater);
                valueAnimatorOfFloat.setInterpolator(PasswordTextView.this.mAppearInterpolator);
                valueAnimatorOfFloat.setDuration(160L);
                ValueAnimator valueAnimatorOfFloat2 = ValueAnimator.ofFloat(1.5f, 1.0f);
                valueAnimatorOfFloat2.addUpdateListener(this.dotSizeUpdater);
                valueAnimatorOfFloat2.setDuration(160L);
                valueAnimatorOfFloat2.addListener(this.dotFinishListener);
                AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.playSequentially(valueAnimatorOfFloat, valueAnimatorOfFloat2);
                animatorSet.setStartDelay(j);
                animatorSet.start();
                this.dotAnimator = animatorSet;
            } else {
                ValueAnimator valueAnimatorOfFloat3 = ValueAnimator.ofFloat(this.currentDotSizeFactor, 1.0f);
                valueAnimatorOfFloat3.addUpdateListener(this.dotSizeUpdater);
                valueAnimatorOfFloat3.setDuration((long) (160.0f * (1.0f - this.currentDotSizeFactor)));
                valueAnimatorOfFloat3.addListener(this.dotFinishListener);
                valueAnimatorOfFloat3.setStartDelay(j);
                valueAnimatorOfFloat3.start();
                this.dotAnimator = valueAnimatorOfFloat3;
            }
            this.dotAnimationIsGrowing = true;
        }

        private void cancelAnimator(Animator animator) {
            if (animator != null) {
                animator.cancel();
            }
        }

        public float draw(Canvas canvas, float f, int i, float f2, float f3) {
            boolean z = this.currentTextSizeFactor > 0.0f;
            boolean z2 = this.currentDotSizeFactor > 0.0f;
            float f4 = f3 * this.currentWidthFactor;
            if (z) {
                float f5 = i;
                float f6 = ((f5 / 2.0f) * this.currentTextSizeFactor) + f2 + (f5 * this.currentTextTranslationY * 0.8f);
                canvas.save();
                canvas.translate((f4 / 2.0f) + f, f6);
                canvas.scale(this.currentTextSizeFactor, this.currentTextSizeFactor);
                canvas.drawText(Character.toString(this.whichChar), 0.0f, 0.0f, PasswordTextView.this.mDrawPaint);
                canvas.restore();
            }
            if (z2) {
                canvas.save();
                canvas.translate(f + (f4 / 2.0f), f2);
                canvas.drawCircle(0.0f, 0.0f, (PasswordTextView.this.mDotSize / 2) * this.currentDotSizeFactor, PasswordTextView.this.mDrawPaint);
                canvas.restore();
            }
            return f4 + (PasswordTextView.this.mCharPadding * this.currentWidthFactor);
        }

        public boolean isCharVisibleForA11y() {
            return this.currentTextSizeFactor > 0.0f || (this.textAnimator != null && this.textAnimationIsGrowing);
        }
    }
}
