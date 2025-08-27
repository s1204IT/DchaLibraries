package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.KeyEvent;
import android.view.View;
import com.android.systemui.Interpolators;
import com.android.systemui.plugins.statusbar.phone.NavBarButtonProvider;
import com.android.systemui.statusbar.policy.KeyButtonDrawable;
import java.util.ArrayList;

/* loaded from: classes.dex */
public class ButtonDispatcher {
    private View.AccessibilityDelegate mAccessibilityDelegate;
    private Float mAlpha;
    private View.OnClickListener mClickListener;
    private View mCurrentView;
    private Float mDarkIntensity;
    private Boolean mDelayTouchFeedback;
    private ValueAnimator mFadeAnimator;
    private final int mId;
    private KeyButtonDrawable mImageDrawable;
    private View.OnLongClickListener mLongClickListener;
    private Boolean mLongClickable;
    private View.OnHoverListener mOnHoverListener;
    private View.OnTouchListener mTouchListener;
    private boolean mVertical;
    private final ArrayList<View> mViews = new ArrayList<>();
    private Integer mVisibility = -1;
    private final ValueAnimator.AnimatorUpdateListener mAlphaListener = new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$ButtonDispatcher$YQ5xchhAskLzgLUT3UrgvCxrRAQ
        @Override // android.animation.ValueAnimator.AnimatorUpdateListener
        public final void onAnimationUpdate(ValueAnimator valueAnimator) {
            this.f$0.setAlpha(((Float) valueAnimator.getAnimatedValue()).floatValue());
        }
    };
    private final AnimatorListenerAdapter mFadeListener = new AnimatorListenerAdapter() { // from class: com.android.systemui.statusbar.phone.ButtonDispatcher.1
        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            ButtonDispatcher.this.setVisibility(ButtonDispatcher.this.getAlpha() == 1.0f ? 0 : 4);
        }
    };

    public ButtonDispatcher(int i) {
        this.mId = i;
    }

    void clear() {
        this.mViews.clear();
    }

    /* JADX DEBUG: Multi-variable search result rejected for r3v0, resolved type: android.view.View */
    /* JADX WARN: Multi-variable type inference failed */
    void addView(View view) {
        this.mViews.add(view);
        view.setOnClickListener(this.mClickListener);
        view.setOnTouchListener(this.mTouchListener);
        view.setOnLongClickListener(this.mLongClickListener);
        view.setOnHoverListener(this.mOnHoverListener);
        if (this.mLongClickable != null) {
            view.setLongClickable(this.mLongClickable.booleanValue());
        }
        if (this.mAlpha != null) {
            view.setAlpha(this.mAlpha.floatValue());
        }
        if (this.mVisibility != null && this.mVisibility.intValue() != -1) {
            view.setVisibility(this.mVisibility.intValue());
        }
        if (this.mAccessibilityDelegate != null) {
            view.setAccessibilityDelegate(this.mAccessibilityDelegate);
        }
        if (view instanceof NavBarButtonProvider.ButtonInterface) {
            NavBarButtonProvider.ButtonInterface buttonInterface = (NavBarButtonProvider.ButtonInterface) view;
            if (this.mDarkIntensity != null) {
                buttonInterface.setDarkIntensity(this.mDarkIntensity.floatValue());
            }
            if (this.mImageDrawable != null) {
                buttonInterface.setImageDrawable(this.mImageDrawable);
            }
            if (this.mDelayTouchFeedback != null) {
                buttonInterface.setDelayTouchFeedback(this.mDelayTouchFeedback.booleanValue());
            }
            buttonInterface.setVertical(this.mVertical);
        }
    }

    public int getId() {
        return this.mId;
    }

    public int getVisibility() {
        if (this.mVisibility != null) {
            return this.mVisibility.intValue();
        }
        return 0;
    }

    public boolean isVisible() {
        return getVisibility() == 0;
    }

    public float getAlpha() {
        if (this.mAlpha != null) {
            return this.mAlpha.floatValue();
        }
        return 1.0f;
    }

    public KeyButtonDrawable getImageDrawable() {
        return this.mImageDrawable;
    }

    public void setImageDrawable(KeyButtonDrawable keyButtonDrawable) {
        this.mImageDrawable = keyButtonDrawable;
        int size = this.mViews.size();
        for (int i = 0; i < size; i++) {
            if (this.mViews.get(i) instanceof NavBarButtonProvider.ButtonInterface) {
                ((NavBarButtonProvider.ButtonInterface) this.mViews.get(i)).setImageDrawable(this.mImageDrawable);
            }
        }
    }

    public void setVisibility(int i) {
        if (this.mVisibility.intValue() == i) {
            return;
        }
        this.mVisibility = Integer.valueOf(i);
        int size = this.mViews.size();
        for (int i2 = 0; i2 < size; i2++) {
            this.mViews.get(i2).setVisibility(this.mVisibility.intValue());
        }
    }

    public void abortCurrentGesture() {
        int size = this.mViews.size();
        for (int i = 0; i < size; i++) {
            if (this.mViews.get(i) instanceof NavBarButtonProvider.ButtonInterface) {
                ((NavBarButtonProvider.ButtonInterface) this.mViews.get(i)).abortCurrentGesture();
            }
        }
    }

    public void setAlpha(float f) {
        setAlpha(f, false);
    }

    public void setAlpha(float f, boolean z) {
        if (z) {
            if (this.mFadeAnimator != null) {
                this.mFadeAnimator.cancel();
            }
            this.mFadeAnimator = ValueAnimator.ofFloat(getAlpha(), f);
            this.mFadeAnimator.setDuration(getAlpha() < f ? 150L : 100L);
            this.mFadeAnimator.setInterpolator(getAlpha() < f ? Interpolators.ALPHA_IN : Interpolators.ALPHA_OUT);
            this.mFadeAnimator.addListener(this.mFadeListener);
            this.mFadeAnimator.addUpdateListener(this.mAlphaListener);
            this.mFadeAnimator.start();
            setVisibility(0);
            return;
        }
        this.mAlpha = Float.valueOf(f);
        int size = this.mViews.size();
        for (int i = 0; i < size; i++) {
            this.mViews.get(i).setAlpha(f);
        }
    }

    public void setDarkIntensity(float f) {
        this.mDarkIntensity = Float.valueOf(f);
        int size = this.mViews.size();
        for (int i = 0; i < size; i++) {
            if (this.mViews.get(i) instanceof NavBarButtonProvider.ButtonInterface) {
                ((NavBarButtonProvider.ButtonInterface) this.mViews.get(i)).setDarkIntensity(f);
            }
        }
    }

    public void setDelayTouchFeedback(boolean z) {
        this.mDelayTouchFeedback = Boolean.valueOf(z);
        int size = this.mViews.size();
        for (int i = 0; i < size; i++) {
            if (this.mViews.get(i) instanceof NavBarButtonProvider.ButtonInterface) {
                ((NavBarButtonProvider.ButtonInterface) this.mViews.get(i)).setDelayTouchFeedback(z);
            }
        }
    }

    public void setOnClickListener(View.OnClickListener onClickListener) {
        this.mClickListener = onClickListener;
        int size = this.mViews.size();
        for (int i = 0; i < size; i++) {
            this.mViews.get(i).setOnClickListener(this.mClickListener);
        }
    }

    public void setOnTouchListener(View.OnTouchListener onTouchListener) {
        this.mTouchListener = onTouchListener;
        int size = this.mViews.size();
        for (int i = 0; i < size; i++) {
            this.mViews.get(i).setOnTouchListener(this.mTouchListener);
        }
    }

    public void setLongClickable(boolean z) {
        this.mLongClickable = Boolean.valueOf(z);
        int size = this.mViews.size();
        for (int i = 0; i < size; i++) {
            this.mViews.get(i).setLongClickable(this.mLongClickable.booleanValue());
        }
    }

    public void setOnLongClickListener(View.OnLongClickListener onLongClickListener) {
        this.mLongClickListener = onLongClickListener;
        int size = this.mViews.size();
        for (int i = 0; i < size; i++) {
            this.mViews.get(i).setOnLongClickListener(this.mLongClickListener);
        }
    }

    public void setOnHoverListener(View.OnHoverListener onHoverListener) {
        this.mOnHoverListener = onHoverListener;
        int size = this.mViews.size();
        for (int i = 0; i < size; i++) {
            this.mViews.get(i).setOnHoverListener(this.mOnHoverListener);
        }
    }

    public void setAccessibilityDelegate(View.AccessibilityDelegate accessibilityDelegate) {
        this.mAccessibilityDelegate = accessibilityDelegate;
        int size = this.mViews.size();
        for (int i = 0; i < size; i++) {
            this.mViews.get(i).setAccessibilityDelegate(accessibilityDelegate);
        }
    }

    public View getCurrentView() {
        return this.mCurrentView;
    }

    public void setCurrentView(View view) {
        this.mCurrentView = view.findViewById(this.mId);
    }

    public void setVertical(boolean z) {
        this.mVertical = z;
        int size = this.mViews.size();
        for (int i = 0; i < size; i++) {
            KeyEvent.Callback callback = (View) this.mViews.get(i);
            if (callback instanceof NavBarButtonProvider.ButtonInterface) {
                ((NavBarButtonProvider.ButtonInterface) callback).setVertical(z);
            }
        }
    }
}
