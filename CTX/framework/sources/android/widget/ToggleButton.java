package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import com.android.internal.R;

public class ToggleButton extends CompoundButton {
    private static final int NO_ALPHA = 255;
    private float mDisabledAlpha;
    private Drawable mIndicatorDrawable;
    private CharSequence mTextOff;
    private CharSequence mTextOn;

    public ToggleButton(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.ToggleButton, i, i2);
        this.mTextOn = typedArrayObtainStyledAttributes.getText(1);
        this.mTextOff = typedArrayObtainStyledAttributes.getText(2);
        this.mDisabledAlpha = typedArrayObtainStyledAttributes.getFloat(0, 0.5f);
        syncTextState();
        typedArrayObtainStyledAttributes.recycle();
    }

    public ToggleButton(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public ToggleButton(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 16842827);
    }

    public ToggleButton(Context context) {
        this(context, null);
    }

    @Override
    public void setChecked(boolean z) {
        super.setChecked(z);
        syncTextState();
    }

    private void syncTextState() {
        boolean zIsChecked = isChecked();
        if (zIsChecked && this.mTextOn != null) {
            setText(this.mTextOn);
        } else if (!zIsChecked && this.mTextOff != null) {
            setText(this.mTextOff);
        }
    }

    public CharSequence getTextOn() {
        return this.mTextOn;
    }

    public void setTextOn(CharSequence charSequence) {
        this.mTextOn = charSequence;
    }

    public CharSequence getTextOff() {
        return this.mTextOff;
    }

    public void setTextOff(CharSequence charSequence) {
        this.mTextOff = charSequence;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        updateReferenceToIndicatorDrawable(getBackground());
    }

    @Override
    public void setBackgroundDrawable(Drawable drawable) {
        super.setBackgroundDrawable(drawable);
        updateReferenceToIndicatorDrawable(drawable);
    }

    private void updateReferenceToIndicatorDrawable(Drawable drawable) {
        if (drawable instanceof LayerDrawable) {
            this.mIndicatorDrawable = ((LayerDrawable) drawable).findDrawableByLayerId(16908311);
        } else {
            this.mIndicatorDrawable = null;
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (this.mIndicatorDrawable != null) {
            this.mIndicatorDrawable.setAlpha(isEnabled() ? 255 : (int) (255.0f * this.mDisabledAlpha));
        }
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return ToggleButton.class.getName();
    }
}
