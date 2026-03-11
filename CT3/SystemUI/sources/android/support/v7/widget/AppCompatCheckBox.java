package android.support.v7.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.TintableCompoundButton;
import android.support.v7.appcompat.R$attr;
import android.util.AttributeSet;
import android.widget.CheckBox;

public class AppCompatCheckBox extends CheckBox implements TintableCompoundButton {
    private AppCompatCompoundButtonHelper mCompoundButtonHelper;
    private AppCompatDrawableManager mDrawableManager;

    public AppCompatCheckBox(Context context, AttributeSet attrs) {
        this(context, attrs, R$attr.checkboxStyle);
    }

    public AppCompatCheckBox(Context context, AttributeSet attrs, int defStyleAttr) {
        super(TintContextWrapper.wrap(context), attrs, defStyleAttr);
        this.mDrawableManager = AppCompatDrawableManager.get();
        this.mCompoundButtonHelper = new AppCompatCompoundButtonHelper(this, this.mDrawableManager);
        this.mCompoundButtonHelper.loadFromAttributes(attrs, defStyleAttr);
    }

    @Override
    public void setButtonDrawable(Drawable buttonDrawable) {
        super.setButtonDrawable(buttonDrawable);
        if (this.mCompoundButtonHelper == null) {
            return;
        }
        this.mCompoundButtonHelper.onSetButtonDrawable();
    }

    @Override
    public void setButtonDrawable(@DrawableRes int resId) {
        Drawable drawable;
        if (this.mDrawableManager != null) {
            drawable = this.mDrawableManager.getDrawable(getContext(), resId);
        } else {
            drawable = ContextCompat.getDrawable(getContext(), resId);
        }
        setButtonDrawable(drawable);
    }

    @Override
    public int getCompoundPaddingLeft() {
        int value = super.getCompoundPaddingLeft();
        if (this.mCompoundButtonHelper == null) {
            return value;
        }
        return this.mCompoundButtonHelper.getCompoundPaddingLeft(value);
    }

    @Override
    public void setSupportButtonTintList(@Nullable ColorStateList tint) {
        if (this.mCompoundButtonHelper == null) {
            return;
        }
        this.mCompoundButtonHelper.setSupportButtonTintList(tint);
    }

    @Override
    public void setSupportButtonTintMode(@Nullable PorterDuff.Mode tintMode) {
        if (this.mCompoundButtonHelper == null) {
            return;
        }
        this.mCompoundButtonHelper.setSupportButtonTintMode(tintMode);
    }
}
