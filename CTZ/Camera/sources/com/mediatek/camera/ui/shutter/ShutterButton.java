package com.mediatek.camera.ui.shutter;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import com.mediatek.camera.common.widget.RotateImageView;

class ShutterButton extends RotateImageView implements View.OnLongClickListener {
    private OnShutterButtonListener mListener;
    private boolean mLongPressed;
    private boolean mOldPressed;
    private ObjectAnimator mScaleAnimation;

    public interface OnShutterButtonListener {
        void onShutterButtonClicked();

        void onShutterButtonFocused(boolean z);

        void onShutterButtonLongPressed();
    }

    public ShutterButton(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setOnLongClickListener(this);
        this.mScaleAnimation = ObjectAnimator.ofPropertyValuesHolder(this, PropertyValuesHolder.ofFloat("scaleX", 1.0f, 0.9f), PropertyValuesHolder.ofFloat("scaleY", 1.0f, 0.9f));
        this.mScaleAnimation.setInterpolator(new AccelerateInterpolator());
        this.mScaleAnimation.setDuration(60L);
    }

    public void setOnShutterButtonListener(OnShutterButtonListener onShutterButtonListener) {
        this.mListener = onShutterButtonListener;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        final boolean zIsPressed = isPressed();
        if (zIsPressed != this.mOldPressed) {
            if (!zIsPressed) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        ShutterButton.this.callShutterButtonFocus(zIsPressed);
                    }
                });
            } else {
                callShutterButtonFocus(zIsPressed);
            }
            this.mOldPressed = zIsPressed;
        }
    }

    private void callShutterButtonFocus(boolean z) {
        if (this.mListener != null && isEnabled() && isClickable()) {
            this.mListener.onShutterButtonFocused(z);
        }
        this.mLongPressed = false;
        if (z) {
            this.mScaleAnimation.start();
        } else {
            this.mScaleAnimation.reverse();
        }
    }

    @Override
    public boolean performClick() {
        boolean zPerformClick = super.performClick();
        if (this.mListener != null && isEnabled() && isClickable() && !this.mLongPressed) {
            this.mListener.onShutterButtonClicked();
        }
        return zPerformClick;
    }

    @Override
    public void setEnabled(boolean z) {
        if (getAlpha() > 0.9d) {
            super.setEnabled(z);
        } else {
            super.setEnabled(false);
        }
    }

    @Override
    public boolean onLongClick(View view) {
        if (this.mListener != null && isEnabled() && isClickable()) {
            this.mListener.onShutterButtonLongPressed();
            this.mLongPressed = true;
            return false;
        }
        return false;
    }
}
