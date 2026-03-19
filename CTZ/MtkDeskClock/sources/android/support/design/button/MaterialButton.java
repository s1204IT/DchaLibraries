package android.support.design.button;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.design.internal.ThemeEnforcement;
import android.support.design.internal.ViewUtils;
import android.support.design.resources.MaterialResources;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.AppCompatButton;
import android.util.AttributeSet;
import android.util.Log;

public class MaterialButton extends AppCompatButton {
    private static final String LOG_TAG = "MaterialButton";
    private int additionalPaddingLeftForIcon;
    private int additionalPaddingRightForIcon;
    private Drawable icon;
    private int iconPadding;
    private ColorStateList iconTint;
    private PorterDuff.Mode iconTintMode;
    private int insetBottom;
    private int insetLeft;
    private int insetRight;
    private int insetTop;

    @Nullable
    private final MaterialButtonHelper materialButtonHelper;
    private int paddingBottom;
    private int paddingEnd;
    private int paddingStart;
    private int paddingTop;

    public MaterialButton(Context context) {
        this(context, null);
    }

    public MaterialButton(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.materialButtonStyle);
    }

    public MaterialButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray attributes = ThemeEnforcement.obtainStyledAttributes(context, attrs, R.styleable.MaterialButton, defStyleAttr, R.style.Widget_MaterialComponents_Button);
        int padding = attributes.getDimensionPixelOffset(R.styleable.MaterialButton_android_padding, 0);
        int paddingLeft = attributes.getDimensionPixelOffset(R.styleable.MaterialButton_android_paddingLeft, padding);
        if (Build.VERSION.SDK_INT >= 17) {
            this.paddingStart = attributes.getDimensionPixelOffset(R.styleable.MaterialButton_android_paddingStart, paddingLeft);
        } else {
            this.paddingStart = paddingLeft;
        }
        int paddingRight = attributes.getDimensionPixelOffset(R.styleable.MaterialButton_android_paddingRight, padding);
        if (Build.VERSION.SDK_INT >= 17) {
            this.paddingEnd = attributes.getDimensionPixelOffset(R.styleable.MaterialButton_android_paddingEnd, paddingRight);
        } else {
            this.paddingEnd = paddingRight;
        }
        this.paddingTop = attributes.getDimensionPixelOffset(R.styleable.MaterialButton_android_paddingTop, padding);
        this.paddingBottom = attributes.getDimensionPixelOffset(R.styleable.MaterialButton_android_paddingBottom, padding);
        this.insetLeft = attributes.getDimensionPixelOffset(R.styleable.MaterialButton_android_insetLeft, 0);
        this.insetRight = attributes.getDimensionPixelOffset(R.styleable.MaterialButton_android_insetRight, 0);
        this.insetTop = attributes.getDimensionPixelOffset(R.styleable.MaterialButton_android_insetTop, 0);
        this.insetBottom = attributes.getDimensionPixelOffset(R.styleable.MaterialButton_android_insetBottom, 0);
        this.additionalPaddingLeftForIcon = attributes.getDimensionPixelOffset(R.styleable.MaterialButton_additionalPaddingLeftForIcon, 0);
        this.additionalPaddingRightForIcon = attributes.getDimensionPixelOffset(R.styleable.MaterialButton_additionalPaddingRightForIcon, 0);
        this.iconPadding = attributes.getDimensionPixelSize(R.styleable.MaterialButton_iconPadding, 0);
        this.iconTintMode = ViewUtils.parseTintMode(attributes.getInt(R.styleable.MaterialButton_iconTintMode, -1), PorterDuff.Mode.SRC_IN);
        this.iconTint = MaterialResources.getColorStateList(getContext(), attributes, R.styleable.MaterialButton_iconTint);
        this.icon = MaterialResources.getDrawable(getContext(), attributes, R.styleable.MaterialButton_icon);
        this.materialButtonHelper = new MaterialButtonHelper(this);
        this.materialButtonHelper.loadFromAttributes(attributes);
        attributes.recycle();
        setCompoundDrawablePadding(this.iconPadding);
        updatePadding();
        updateIcon();
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (Build.VERSION.SDK_INT < 21 && isUsingOriginalBackground()) {
            this.materialButtonHelper.drawStroke(canvas);
        }
    }

    @Override
    @RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
    public void setSupportBackgroundTintList(@Nullable ColorStateList tint) {
        if (isUsingOriginalBackground()) {
            this.materialButtonHelper.setSupportBackgroundTintList(tint);
        } else if (this.materialButtonHelper != null) {
            super.setSupportBackgroundTintList(tint);
        }
    }

    @Override
    @Nullable
    @RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
    public ColorStateList getSupportBackgroundTintList() {
        if (isUsingOriginalBackground()) {
            return this.materialButtonHelper.getSupportBackgroundTintList();
        }
        return super.getSupportBackgroundTintList();
    }

    @Override
    @RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
    public void setSupportBackgroundTintMode(@Nullable PorterDuff.Mode tintMode) {
        if (isUsingOriginalBackground()) {
            this.materialButtonHelper.setSupportBackgroundTintMode(tintMode);
        } else if (this.materialButtonHelper != null) {
            super.setSupportBackgroundTintMode(tintMode);
        }
    }

    @Override
    @Nullable
    @RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
    public PorterDuff.Mode getSupportBackgroundTintMode() {
        if (isUsingOriginalBackground()) {
            return this.materialButtonHelper.getSupportBackgroundTintMode();
        }
        return super.getSupportBackgroundTintMode();
    }

    @Override
    public void setBackgroundTintList(@Nullable ColorStateList tintList) {
        setSupportBackgroundTintList(tintList);
    }

    @Override
    @Nullable
    public ColorStateList getBackgroundTintList() {
        return getSupportBackgroundTintList();
    }

    @Override
    public void setBackgroundTintMode(@Nullable PorterDuff.Mode tintMode) {
        setSupportBackgroundTintMode(tintMode);
    }

    @Override
    @Nullable
    public PorterDuff.Mode getBackgroundTintMode() {
        return getSupportBackgroundTintMode();
    }

    @Override
    public void setBackgroundColor(int color) {
        if (isUsingOriginalBackground()) {
            this.materialButtonHelper.setBackgroundColor(color);
        } else {
            super.setBackgroundColor(color);
        }
    }

    @Override
    public void setBackground(Drawable background) {
        setBackgroundDrawable(background);
    }

    @Override
    public void setBackgroundResource(@DrawableRes int backgroundResourceId) {
        Drawable background = null;
        if (backgroundResourceId != 0) {
            background = AppCompatResources.getDrawable(getContext(), backgroundResourceId);
        }
        setBackgroundDrawable(background);
    }

    @Override
    public void setBackgroundDrawable(Drawable background) {
        if (isUsingOriginalBackground()) {
            if (background != getBackground()) {
                Log.i(LOG_TAG, "Setting a custom background is not supported.");
                this.materialButtonHelper.setBackgroundOverwritten();
                super.setBackgroundDrawable(background);
                return;
            }
            getBackground().setState(background.getState());
            return;
        }
        super.setBackgroundDrawable(background);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (Build.VERSION.SDK_INT == 21 && this.materialButtonHelper != null) {
            this.materialButtonHelper.updateMaskBounds(bottom - top, right - left);
        }
    }

    void setInternalBackground(Drawable background) {
        super.setBackgroundDrawable(background);
    }

    public void setButtonPadding(int start, int top, int end, int bottom) {
        this.paddingStart = start;
        this.paddingTop = top;
        this.paddingEnd = end;
        this.paddingBottom = bottom;
        updatePadding();
    }

    public int getButtonPaddingStart() {
        return this.paddingStart;
    }

    public int getButtonPaddingTop() {
        return this.paddingTop;
    }

    public int getButtonPaddingEnd() {
        return this.paddingEnd;
    }

    public int getButtonPaddingBottom() {
        return this.paddingBottom;
    }

    public void setAdditionalPaddingLeftForIcon(int additionalPaddingLeftForIcon) {
        if (this.additionalPaddingLeftForIcon != additionalPaddingLeftForIcon) {
            this.additionalPaddingLeftForIcon = additionalPaddingLeftForIcon;
            updatePadding();
        }
    }

    public int getAdditionalPaddingLeftForIcon() {
        return this.additionalPaddingLeftForIcon;
    }

    public void setAdditionalPaddingRightForIcon(int additionalPaddingRightForIcon) {
        if (this.additionalPaddingRightForIcon != additionalPaddingRightForIcon) {
            this.additionalPaddingRightForIcon = additionalPaddingRightForIcon;
            updatePadding();
        }
    }

    public int getAdditionalPaddingRightForIcon() {
        return this.additionalPaddingRightForIcon;
    }

    private void updatePadding() {
        ViewCompat.setPaddingRelative(this, this.paddingStart + (this.icon != null ? this.additionalPaddingLeftForIcon : 0) + this.insetLeft, this.paddingTop + this.insetTop, this.paddingEnd + (this.icon != null ? this.additionalPaddingRightForIcon : 0) + this.insetRight, this.paddingBottom + this.insetBottom);
    }

    public void setIconPadding(int iconPadding) {
        if (this.iconPadding != iconPadding) {
            this.iconPadding = iconPadding;
            setCompoundDrawablePadding(iconPadding);
        }
    }

    public int getIconPadding() {
        return this.iconPadding;
    }

    public void setIcon(Drawable icon) {
        if (this.icon != icon) {
            this.icon = icon;
            updateIcon();
        }
    }

    public void setIconResource(@DrawableRes int iconResourceId) {
        Drawable icon = null;
        if (iconResourceId != 0) {
            icon = AppCompatResources.getDrawable(getContext(), iconResourceId);
        }
        setIcon(icon);
    }

    public Drawable getIcon() {
        return this.icon;
    }

    public void setIconTint(@Nullable ColorStateList iconTint) {
        if (this.iconTint != iconTint) {
            this.iconTint = iconTint;
            updateIcon();
        }
    }

    public void setIconTintResource(@ColorRes int iconTintResourceId) {
        setIconTint(AppCompatResources.getColorStateList(getContext(), iconTintResourceId));
    }

    public ColorStateList getIconTint() {
        return this.iconTint;
    }

    public void setIconTintMode(PorterDuff.Mode iconTintMode) {
        if (this.iconTintMode != iconTintMode) {
            this.iconTintMode = iconTintMode;
            updateIcon();
        }
    }

    public PorterDuff.Mode getIconTintMode() {
        return this.iconTintMode;
    }

    private void updateIcon() {
        if (this.icon != null) {
            this.icon = this.icon.mutate();
            DrawableCompat.setTintList(this.icon, this.iconTint);
            if (this.iconTintMode != null) {
                DrawableCompat.setTintMode(this.icon, this.iconTintMode);
            }
        }
        TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(this, this.icon, (Drawable) null, (Drawable) null, (Drawable) null);
    }

    public void setRippleColor(@Nullable ColorStateList rippleColor) {
        if (isUsingOriginalBackground()) {
            this.materialButtonHelper.setRippleColor(rippleColor);
        }
    }

    public void setRippleColorResource(@ColorRes int rippleColorResourceId) {
        if (isUsingOriginalBackground()) {
            setRippleColor(AppCompatResources.getColorStateList(getContext(), rippleColorResourceId));
        }
    }

    public ColorStateList getRippleColor() {
        if (isUsingOriginalBackground()) {
            return this.materialButtonHelper.getRippleColor();
        }
        return null;
    }

    public void setStrokeColor(@Nullable ColorStateList strokeColor) {
        if (isUsingOriginalBackground()) {
            this.materialButtonHelper.setStrokeColor(strokeColor);
        }
    }

    public void setStrokeColorResource(@ColorRes int strokeColorResourceId) {
        if (isUsingOriginalBackground()) {
            setStrokeColor(AppCompatResources.getColorStateList(getContext(), strokeColorResourceId));
        }
    }

    public ColorStateList getStrokeColor() {
        if (isUsingOriginalBackground()) {
            return this.materialButtonHelper.getStrokeColor();
        }
        return null;
    }

    public void setStrokeWidth(int strokeWidth) {
        if (isUsingOriginalBackground()) {
            this.materialButtonHelper.setStrokeWidth(strokeWidth);
        }
    }

    public void setStrokeWidthResource(@DimenRes int strokeWidthResourceId) {
        if (isUsingOriginalBackground()) {
            setStrokeWidth(getResources().getDimensionPixelSize(strokeWidthResourceId));
        }
    }

    public int getStrokeWidth() {
        if (isUsingOriginalBackground()) {
            return this.materialButtonHelper.getStrokeWidth();
        }
        return 0;
    }

    public void setCornerRadius(int cornerRadius) {
        if (isUsingOriginalBackground()) {
            this.materialButtonHelper.setCornerRadius(cornerRadius);
        }
    }

    public void setCornerRadiusResource(@DimenRes int cornerRadiusResourceId) {
        if (isUsingOriginalBackground()) {
            setCornerRadius(getResources().getDimensionPixelSize(cornerRadiusResourceId));
        }
    }

    public int getCornerRadius() {
        if (isUsingOriginalBackground()) {
            return this.materialButtonHelper.getCornerRadius();
        }
        return 0;
    }

    private boolean isUsingOriginalBackground() {
        return (this.materialButtonHelper == null || this.materialButtonHelper.isBackgroundOverwritten()) ? false : true;
    }
}
