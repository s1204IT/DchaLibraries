package com.android.keyguard;

import android.R;
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
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import java.util.ArrayList;
import java.util.Stack;

public class PasswordTextView extends View {
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

    public PasswordTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PasswordTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public PasswordTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mTextChars = new ArrayList<>();
        this.mText = "";
        this.mCharPool = new Stack<>();
        this.mDrawPaint = new Paint();
        setFocusableInTouchMode(true);
        setFocusable(true);
        TypedArray a = context.obtainStyledAttributes(attrs, R$styleable.PasswordTextView);
        try {
            this.mTextHeightRaw = a.getInt(R$styleable.PasswordTextView_scaledTextSize, 0);
            this.mGravity = a.getInt(R$styleable.PasswordTextView_android_gravity, 17);
            this.mDotSize = a.getDimensionPixelSize(R$styleable.PasswordTextView_dotSize, getContext().getResources().getDimensionPixelSize(R$dimen.password_dot_size));
            this.mCharPadding = a.getDimensionPixelSize(R$styleable.PasswordTextView_charPadding, getContext().getResources().getDimensionPixelSize(R$dimen.password_char_padding));
            a.recycle();
            this.mDrawPaint.setFlags(129);
            this.mDrawPaint.setTextAlign(Paint.Align.CENTER);
            this.mDrawPaint.setColor(-1);
            this.mDrawPaint.setTypeface(Typeface.create("sans-serif-light", 0));
            this.mShowPassword = Settings.System.getInt(this.mContext.getContentResolver(), "show_password", 1) == 1;
            this.mAppearInterpolator = AnimationUtils.loadInterpolator(this.mContext, R.interpolator.linear_out_slow_in);
            this.mDisappearInterpolator = AnimationUtils.loadInterpolator(this.mContext, R.interpolator.fast_out_linear_in);
            this.mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(this.mContext, R.interpolator.fast_out_slow_in);
            this.mPM = (PowerManager) this.mContext.getSystemService("power");
        } catch (Throwable th) {
            a.recycle();
            throw th;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float currentDrawPosition;
        float totalDrawingWidth = getDrawingWidth();
        if ((this.mGravity & 7) == 3) {
            if ((this.mGravity & 8388608) != 0 && getLayoutDirection() == 1) {
                currentDrawPosition = (getWidth() - getPaddingRight()) - totalDrawingWidth;
            } else {
                currentDrawPosition = getPaddingLeft();
            }
        } else {
            currentDrawPosition = (getWidth() / 2) - (totalDrawingWidth / 2.0f);
        }
        int length = this.mTextChars.size();
        Rect bounds = getCharBounds();
        int charHeight = bounds.bottom - bounds.top;
        float yPosition = (((getHeight() - getPaddingBottom()) - getPaddingTop()) / 2) + getPaddingTop();
        canvas.clipRect(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(), getHeight() - getPaddingBottom());
        float charLength = bounds.right - bounds.left;
        for (int i = 0; i < length; i++) {
            CharState charState = this.mTextChars.get(i);
            float charWidth = charState.draw(canvas, currentDrawPosition, charHeight, yPosition, charLength);
            currentDrawPosition += charWidth;
        }
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    private Rect getCharBounds() {
        float textHeight = this.mTextHeightRaw * getResources().getDisplayMetrics().scaledDensity;
        this.mDrawPaint.setTextSize(textHeight);
        Rect bounds = new Rect();
        this.mDrawPaint.getTextBounds("0", 0, 1, bounds);
        return bounds;
    }

    private float getDrawingWidth() {
        int width = 0;
        int length = this.mTextChars.size();
        Rect bounds = getCharBounds();
        int charLength = bounds.right - bounds.left;
        for (int i = 0; i < length; i++) {
            CharState charState = this.mTextChars.get(i);
            if (i != 0) {
                width = (int) (width + (this.mCharPadding * charState.currentWidthFactor));
            }
            width = (int) (width + (charLength * charState.currentWidthFactor));
        }
        return width;
    }

    public void append(char c) {
        CharState charState;
        int visibleChars = this.mTextChars.size();
        String textbefore = this.mText;
        this.mText += c;
        int newLength = this.mText.length();
        if (newLength > visibleChars) {
            charState = obtainCharState(c);
            this.mTextChars.add(charState);
        } else {
            charState = this.mTextChars.get(newLength - 1);
            charState.whichChar = c;
        }
        charState.startAppearAnimation();
        if (newLength > 1) {
            CharState previousState = this.mTextChars.get(newLength - 2);
            if (previousState.isDotSwapPending) {
                previousState.swapToDotWhenAppearFinished();
            }
        }
        userActivity();
        sendAccessibilityEventTypeViewTextChanged(textbefore, textbefore.length(), 0, 1);
    }

    public void setUserActivityListener(UserActivityListener userActivitiListener) {
        this.mUserActivityListener = userActivitiListener;
    }

    private void userActivity() {
        this.mPM.userActivity(SystemClock.uptimeMillis(), false);
        if (this.mUserActivityListener == null) {
            return;
        }
        this.mUserActivityListener.onUserActivity();
    }

    public void deleteLastChar() {
        int length = this.mText.length();
        String textbefore = this.mText;
        if (length > 0) {
            this.mText = this.mText.substring(0, length - 1);
            CharState charState = this.mTextChars.get(length - 1);
            charState.startRemoveAnimation(0L, 0L);
        }
        userActivity();
        sendAccessibilityEventTypeViewTextChanged(textbefore, textbefore.length() - 1, 1, 0);
    }

    public String getText() {
        return this.mText;
    }

    private CharState obtainCharState(char c) {
        CharState charState;
        if (this.mCharPool.isEmpty()) {
            charState = new CharState(this, null);
        } else {
            charState = this.mCharPool.pop();
            charState.reset();
        }
        charState.whichChar = c;
        return charState;
    }

    public void reset(boolean animated, boolean announce) {
        int delayIndex;
        Log.d("PasswordTextView", "reset() is called, set PwEntry true.");
        String textbefore = this.mText;
        this.mText = "";
        int length = this.mTextChars.size();
        int middleIndex = (length - 1) / 2;
        for (int i = 0; i < length; i++) {
            CharState charState = this.mTextChars.get(i);
            if (animated) {
                if (i <= middleIndex) {
                    delayIndex = i * 2;
                } else {
                    int distToMiddle = i - middleIndex;
                    delayIndex = (length - 1) - ((distToMiddle - 1) * 2);
                }
                long startDelay = ((long) delayIndex) * 40;
                long maxDelay = 40 * ((long) (length - 1));
                charState.startRemoveAnimation(Math.min(startDelay, 200L), Math.min(maxDelay, 200L) + 160);
                charState.removeDotSwapCallbacks();
            } else {
                this.mCharPool.push(charState);
            }
        }
        if (!animated) {
            this.mTextChars.clear();
        }
        if (!announce) {
            return;
        }
        sendAccessibilityEventTypeViewTextChanged(textbefore, 0, textbefore.length(), 0);
    }

    void sendAccessibilityEventTypeViewTextChanged(String beforeText, int fromIndex, int removedCount, int addedCount) {
        if (!AccessibilityManager.getInstance(this.mContext).isEnabled()) {
            return;
        }
        if (!isFocused() && (!isSelected() || !isShown())) {
            return;
        }
        if (!shouldSpeakPasswordsForAccessibility()) {
            beforeText = null;
        }
        AccessibilityEvent event = AccessibilityEvent.obtain(16);
        event.setFromIndex(fromIndex);
        event.setRemovedCount(removedCount);
        event.setAddedCount(addedCount);
        event.setBeforeText(beforeText);
        event.setPassword(true);
        sendAccessibilityEventUnchecked(event);
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(PasswordTextView.class.getName());
        event.setPassword(true);
    }

    @Override
    public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
        super.onPopulateAccessibilityEvent(event);
        if (!shouldSpeakPasswordsForAccessibility()) {
            return;
        }
        CharSequence text = this.mText;
        if (TextUtils.isEmpty(text)) {
            return;
        }
        event.getText().add(text);
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(PasswordTextView.class.getName());
        info.setPassword(true);
        if (shouldSpeakPasswordsForAccessibility()) {
            info.setText(this.mText);
        }
        info.setEditable(true);
        info.setInputType(16);
    }

    private boolean shouldSpeakPasswordsForAccessibility() {
        return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "speak_password", 0, -3) == 1;
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

        CharState(PasswordTextView this$0, CharState charState) {
            this();
        }

        private CharState() {
            this.currentTextTranslationY = 1.0f;
            this.removeEndListener = new AnimatorListenerAdapter() {
                private boolean mCancelled;

                @Override
                public void onAnimationCancel(Animator animation) {
                    this.mCancelled = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (this.mCancelled) {
                        return;
                    }
                    PasswordTextView.this.mTextChars.remove(CharState.this);
                    PasswordTextView.this.mCharPool.push(CharState.this);
                    CharState.this.reset();
                    CharState.this.cancelAnimator(CharState.this.textTranslateAnimator);
                    CharState.this.textTranslateAnimator = null;
                }

                @Override
                public void onAnimationStart(Animator animation) {
                    this.mCancelled = false;
                }
            };
            this.dotFinishListener = new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    CharState.this.dotAnimator = null;
                }
            };
            this.textFinishListener = new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    CharState.this.textAnimator = null;
                }
            };
            this.textTranslateFinishListener = new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    CharState.this.textTranslateAnimator = null;
                }
            };
            this.widthFinishListener = new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    CharState.this.widthAnimator = null;
                }
            };
            this.dotSizeUpdater = new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    CharState.this.currentDotSizeFactor = ((Float) animation.getAnimatedValue()).floatValue();
                    PasswordTextView.this.invalidate();
                }
            };
            this.textSizeUpdater = new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    CharState.this.currentTextSizeFactor = ((Float) animation.getAnimatedValue()).floatValue();
                    PasswordTextView.this.invalidate();
                }
            };
            this.textTranslationUpdater = new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    CharState.this.currentTextTranslationY = ((Float) animation.getAnimatedValue()).floatValue();
                    PasswordTextView.this.invalidate();
                }
            };
            this.widthUpdater = new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    CharState.this.currentWidthFactor = ((Float) animation.getAnimatedValue()).floatValue();
                    PasswordTextView.this.invalidate();
                }
            };
            this.dotSwapperRunnable = new Runnable() {
                @Override
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

        void startRemoveAnimation(long startDelay, long widthDelay) {
            boolean dotNeedsAnimation;
            boolean textNeedsAnimation;
            boolean widthNeedsAnimation;
            if (this.currentDotSizeFactor <= 0.0f || this.dotAnimator != null) {
                dotNeedsAnimation = this.dotAnimator != null ? this.dotAnimationIsGrowing : false;
            } else {
                dotNeedsAnimation = true;
            }
            if (this.currentTextSizeFactor <= 0.0f || this.textAnimator != null) {
                textNeedsAnimation = this.textAnimator != null ? this.textAnimationIsGrowing : false;
            } else {
                textNeedsAnimation = true;
            }
            if (this.currentWidthFactor <= 0.0f || this.widthAnimator != null) {
                widthNeedsAnimation = this.widthAnimator != null ? this.widthAnimationIsGrowing : false;
            } else {
                widthNeedsAnimation = true;
            }
            if (dotNeedsAnimation) {
                startDotDisappearAnimation(startDelay);
            }
            if (textNeedsAnimation) {
                startTextDisappearAnimation(startDelay);
            }
            if (!widthNeedsAnimation) {
                return;
            }
            startWidthDisappearAnimation(widthDelay);
        }

        void startAppearAnimation() {
            boolean dotNeedsAnimation;
            boolean textNeedsAnimation;
            if (PasswordTextView.this.mShowPassword) {
                dotNeedsAnimation = false;
            } else {
                dotNeedsAnimation = this.dotAnimator == null || !this.dotAnimationIsGrowing;
            }
            if (!PasswordTextView.this.mShowPassword) {
                textNeedsAnimation = false;
            } else {
                textNeedsAnimation = this.textAnimator == null || !this.textAnimationIsGrowing;
            }
            boolean widthNeedsAnimation = this.widthAnimator == null || !this.widthAnimationIsGrowing;
            if (dotNeedsAnimation) {
                startDotAppearAnimation(0L);
            }
            if (textNeedsAnimation) {
                startTextAppearAnimation();
            }
            if (widthNeedsAnimation) {
                startWidthAppearAnimation();
            }
            if (!PasswordTextView.this.mShowPassword) {
                return;
            }
            postDotSwap(1300L);
        }

        private void postDotSwap(long delay) {
            removeDotSwapCallbacks();
            PasswordTextView.this.postDelayed(this.dotSwapperRunnable, delay);
            this.isDotSwapPending = true;
        }

        public void removeDotSwapCallbacks() {
            PasswordTextView.this.removeCallbacks(this.dotSwapperRunnable);
            this.isDotSwapPending = false;
        }

        void swapToDotWhenAppearFinished() {
            removeDotSwapCallbacks();
            if (this.textAnimator != null) {
                long remainingDuration = this.textAnimator.getDuration() - this.textAnimator.getCurrentPlayTime();
                postDotSwap(100 + remainingDuration);
            } else {
                performSwap();
            }
        }

        public void performSwap() {
            startTextDisappearAnimation(0L);
            startDotAppearAnimation(30L);
        }

        private void startWidthDisappearAnimation(long widthDelay) {
            cancelAnimator(this.widthAnimator);
            this.widthAnimator = ValueAnimator.ofFloat(this.currentWidthFactor, 0.0f);
            this.widthAnimator.addUpdateListener(this.widthUpdater);
            this.widthAnimator.addListener(this.widthFinishListener);
            this.widthAnimator.addListener(this.removeEndListener);
            this.widthAnimator.setDuration((long) (this.currentWidthFactor * 160.0f));
            this.widthAnimator.setStartDelay(widthDelay);
            this.widthAnimator.start();
            this.widthAnimationIsGrowing = false;
        }

        private void startTextDisappearAnimation(long startDelay) {
            cancelAnimator(this.textAnimator);
            this.textAnimator = ValueAnimator.ofFloat(this.currentTextSizeFactor, 0.0f);
            this.textAnimator.addUpdateListener(this.textSizeUpdater);
            this.textAnimator.addListener(this.textFinishListener);
            this.textAnimator.setInterpolator(PasswordTextView.this.mDisappearInterpolator);
            this.textAnimator.setDuration((long) (this.currentTextSizeFactor * 160.0f));
            this.textAnimator.setStartDelay(startDelay);
            this.textAnimator.start();
            this.textAnimationIsGrowing = false;
        }

        private void startDotDisappearAnimation(long startDelay) {
            cancelAnimator(this.dotAnimator);
            ValueAnimator animator = ValueAnimator.ofFloat(this.currentDotSizeFactor, 0.0f);
            animator.addUpdateListener(this.dotSizeUpdater);
            animator.addListener(this.dotFinishListener);
            animator.setInterpolator(PasswordTextView.this.mDisappearInterpolator);
            long duration = (long) (Math.min(this.currentDotSizeFactor, 1.0f) * 160.0f);
            animator.setDuration(duration);
            animator.setStartDelay(startDelay);
            animator.start();
            this.dotAnimator = animator;
            this.dotAnimationIsGrowing = false;
        }

        private void startWidthAppearAnimation() {
            cancelAnimator(this.widthAnimator);
            this.widthAnimator = ValueAnimator.ofFloat(this.currentWidthFactor, 1.0f);
            this.widthAnimator.addUpdateListener(this.widthUpdater);
            this.widthAnimator.addListener(this.widthFinishListener);
            this.widthAnimator.setDuration((long) ((1.0f - this.currentWidthFactor) * 160.0f));
            this.widthAnimator.start();
            this.widthAnimationIsGrowing = true;
        }

        private void startTextAppearAnimation() {
            cancelAnimator(this.textAnimator);
            this.textAnimator = ValueAnimator.ofFloat(this.currentTextSizeFactor, 1.0f);
            this.textAnimator.addUpdateListener(this.textSizeUpdater);
            this.textAnimator.addListener(this.textFinishListener);
            this.textAnimator.setInterpolator(PasswordTextView.this.mAppearInterpolator);
            this.textAnimator.setDuration((long) ((1.0f - this.currentTextSizeFactor) * 160.0f));
            this.textAnimator.start();
            this.textAnimationIsGrowing = true;
            if (this.textTranslateAnimator != null) {
                return;
            }
            this.textTranslateAnimator = ValueAnimator.ofFloat(1.0f, 0.0f);
            this.textTranslateAnimator.addUpdateListener(this.textTranslationUpdater);
            this.textTranslateAnimator.addListener(this.textTranslateFinishListener);
            this.textTranslateAnimator.setInterpolator(PasswordTextView.this.mAppearInterpolator);
            this.textTranslateAnimator.setDuration(160L);
            this.textTranslateAnimator.start();
        }

        private void startDotAppearAnimation(long delay) {
            cancelAnimator(this.dotAnimator);
            if (!PasswordTextView.this.mShowPassword) {
                ValueAnimator overShootAnimator = ValueAnimator.ofFloat(this.currentDotSizeFactor, 1.5f);
                overShootAnimator.addUpdateListener(this.dotSizeUpdater);
                overShootAnimator.setInterpolator(PasswordTextView.this.mAppearInterpolator);
                overShootAnimator.setDuration(160L);
                ValueAnimator settleBackAnimator = ValueAnimator.ofFloat(1.5f, 1.0f);
                settleBackAnimator.addUpdateListener(this.dotSizeUpdater);
                settleBackAnimator.setDuration(160L);
                settleBackAnimator.addListener(this.dotFinishListener);
                AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.playSequentially(overShootAnimator, settleBackAnimator);
                animatorSet.setStartDelay(delay);
                animatorSet.start();
                this.dotAnimator = animatorSet;
            } else {
                ValueAnimator growAnimator = ValueAnimator.ofFloat(this.currentDotSizeFactor, 1.0f);
                growAnimator.addUpdateListener(this.dotSizeUpdater);
                growAnimator.setDuration((long) ((1.0f - this.currentDotSizeFactor) * 160.0f));
                growAnimator.addListener(this.dotFinishListener);
                growAnimator.setStartDelay(delay);
                growAnimator.start();
                this.dotAnimator = growAnimator;
            }
            this.dotAnimationIsGrowing = true;
        }

        public void cancelAnimator(Animator animator) {
            if (animator == null) {
                return;
            }
            animator.cancel();
        }

        public float draw(Canvas canvas, float currentDrawPosition, int charHeight, float yPosition, float charLength) {
            boolean textVisible = this.currentTextSizeFactor > 0.0f;
            boolean dotVisible = this.currentDotSizeFactor > 0.0f;
            float charWidth = charLength * this.currentWidthFactor;
            if (textVisible) {
                float currYPosition = ((charHeight / 2.0f) * this.currentTextSizeFactor) + yPosition + (charHeight * this.currentTextTranslationY * 0.8f);
                canvas.save();
                float centerX = currentDrawPosition + (charWidth / 2.0f);
                canvas.translate(centerX, currYPosition);
                canvas.scale(this.currentTextSizeFactor, this.currentTextSizeFactor);
                canvas.drawText(Character.toString(this.whichChar), 0.0f, 0.0f, PasswordTextView.this.mDrawPaint);
                canvas.restore();
            }
            if (dotVisible) {
                canvas.save();
                float centerX2 = currentDrawPosition + (charWidth / 2.0f);
                canvas.translate(centerX2, yPosition);
                canvas.drawCircle(0.0f, 0.0f, (PasswordTextView.this.mDotSize / 2) * this.currentDotSizeFactor, PasswordTextView.this.mDrawPaint);
                canvas.restore();
            }
            return (PasswordTextView.this.mCharPadding * this.currentWidthFactor) + charWidth;
        }
    }
}
