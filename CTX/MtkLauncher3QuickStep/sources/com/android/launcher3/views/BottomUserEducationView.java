package com.android.launcher3.views;

import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import com.android.launcher3.Insettable;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.anim.Interpolators;
import com.android.launcher3.compat.AccessibilityManagerCompat;

public class BottomUserEducationView extends AbstractSlideInView implements Insettable {
    private static final int DEFAULT_CLOSE_DURATION = 200;
    private static final String KEY_SHOWED_BOTTOM_USER_EDUCATION = "showed_bottom_user_education";
    private View mCloseButton;
    private final Rect mInsets;

    public BottomUserEducationView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public BottomUserEducationView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mInsets = new Rect();
        this.mContent = this;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mCloseButton = findViewById(R.id.close_bottom_user_tip);
        this.mCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                this.f$0.handleClose(true);
            }
        });
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        setTranslationShift(this.mTranslationShift);
        expandTouchAreaOfCloseButton();
    }

    @Override
    public void logActionCommand(int i) {
    }

    @Override
    protected boolean isOfType(int i) {
        return (i & 32) != 0;
    }

    @Override
    public void setInsets(Rect rect) {
        int i = rect.left - this.mInsets.left;
        int i2 = rect.right - this.mInsets.right;
        int i3 = rect.bottom - this.mInsets.bottom;
        this.mInsets.set(rect);
        setPadding(getPaddingLeft() + i, getPaddingTop(), getPaddingRight() + i2, getPaddingBottom() + i3);
    }

    @Override
    public void handleClose(boolean z) {
        handleClose(z, 200L);
        if (z) {
            this.mLauncher.getSharedPrefs().edit().putBoolean(KEY_SHOWED_BOTTOM_USER_EDUCATION, true).apply();
            AccessibilityManagerCompat.sendCustomAccessibilityEvent(this, 32, getContext().getString(R.string.bottom_work_tab_user_education_closed));
        }
    }

    private void open(boolean z) {
        if (this.mIsOpen || this.mOpenCloseAnimator.isRunning()) {
            return;
        }
        this.mIsOpen = true;
        if (z) {
            this.mOpenCloseAnimator.setValues(PropertyValuesHolder.ofFloat(TRANSLATION_SHIFT, 0.0f));
            this.mOpenCloseAnimator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
            this.mOpenCloseAnimator.start();
            return;
        }
        setTranslationShift(0.0f);
    }

    public static void showIfNeeded(Launcher launcher) {
        if (launcher.getSharedPrefs().getBoolean(KEY_SHOWED_BOTTOM_USER_EDUCATION, false)) {
            return;
        }
        BottomUserEducationView bottomUserEducationView = (BottomUserEducationView) LayoutInflater.from(launcher).inflate(R.layout.work_tab_bottom_user_education_view, (ViewGroup) launcher.getDragLayer(), false);
        launcher.getDragLayer().addView(bottomUserEducationView);
        bottomUserEducationView.open(true);
    }

    private void expandTouchAreaOfCloseButton() {
        Rect rect = new Rect();
        this.mCloseButton.getHitRect(rect);
        rect.left -= this.mCloseButton.getWidth();
        rect.top -= this.mCloseButton.getHeight();
        rect.right += this.mCloseButton.getWidth();
        rect.bottom += this.mCloseButton.getHeight();
        ((View) this.mCloseButton.getParent()).setTouchDelegate(new TouchDelegate(rect, this.mCloseButton));
    }
}
