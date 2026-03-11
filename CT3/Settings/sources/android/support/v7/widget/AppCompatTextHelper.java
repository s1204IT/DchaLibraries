package android.support.v7.widget;

import android.R;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v7.appcompat.R$styleable;
import android.support.v7.text.AllCapsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.widget.TextView;

class AppCompatTextHelper {
    private static final int[] VIEW_ATTRS = {R.attr.textAppearance, R.attr.drawableLeft, R.attr.drawableTop, R.attr.drawableRight, R.attr.drawableBottom};
    private TintInfo mDrawableBottomTint;
    private TintInfo mDrawableLeftTint;
    private TintInfo mDrawableRightTint;
    private TintInfo mDrawableTopTint;
    final TextView mView;

    static AppCompatTextHelper create(TextView textView) {
        if (Build.VERSION.SDK_INT >= 17) {
            return new AppCompatTextHelperV17(textView);
        }
        return new AppCompatTextHelper(textView);
    }

    AppCompatTextHelper(TextView view) {
        this.mView = view;
    }

    void loadFromAttributes(AttributeSet attrs, int defStyleAttr) {
        Context context = this.mView.getContext();
        AppCompatDrawableManager drawableManager = AppCompatDrawableManager.get();
        TintTypedArray a = TintTypedArray.obtainStyledAttributes(context, attrs, VIEW_ATTRS, defStyleAttr, 0);
        int ap = a.getResourceId(0, -1);
        if (a.hasValue(1)) {
            this.mDrawableLeftTint = createTintInfo(context, drawableManager, a.getResourceId(1, 0));
        }
        if (a.hasValue(2)) {
            this.mDrawableTopTint = createTintInfo(context, drawableManager, a.getResourceId(2, 0));
        }
        if (a.hasValue(3)) {
            this.mDrawableRightTint = createTintInfo(context, drawableManager, a.getResourceId(3, 0));
        }
        if (a.hasValue(4)) {
            this.mDrawableBottomTint = createTintInfo(context, drawableManager, a.getResourceId(4, 0));
        }
        a.recycle();
        boolean hasPwdTm = this.mView.getTransformationMethod() instanceof PasswordTransformationMethod;
        boolean allCaps = false;
        boolean allCapsSet = false;
        ColorStateList textColor = null;
        if (ap != -1) {
            TintTypedArray a2 = TintTypedArray.obtainStyledAttributes(context, ap, R$styleable.TextAppearance);
            if (!hasPwdTm && a2.hasValue(R$styleable.TextAppearance_textAllCaps)) {
                allCapsSet = true;
                allCaps = a2.getBoolean(R$styleable.TextAppearance_textAllCaps, false);
            }
            if (Build.VERSION.SDK_INT < 23 && a2.hasValue(R$styleable.TextAppearance_android_textColor)) {
                textColor = a2.getColorStateList(R$styleable.TextAppearance_android_textColor);
            }
            a2.recycle();
        }
        TintTypedArray a3 = TintTypedArray.obtainStyledAttributes(context, attrs, R$styleable.TextAppearance, defStyleAttr, 0);
        if (!hasPwdTm && a3.hasValue(R$styleable.TextAppearance_textAllCaps)) {
            allCapsSet = true;
            allCaps = a3.getBoolean(R$styleable.TextAppearance_textAllCaps, false);
        }
        if (Build.VERSION.SDK_INT < 23 && a3.hasValue(R$styleable.TextAppearance_android_textColor)) {
            textColor = a3.getColorStateList(R$styleable.TextAppearance_android_textColor);
        }
        a3.recycle();
        if (textColor != null) {
            this.mView.setTextColor(textColor);
        }
        if (hasPwdTm || !allCapsSet) {
            return;
        }
        setAllCaps(allCaps);
    }

    void onSetTextAppearance(Context context, int resId) {
        ColorStateList textColor;
        TintTypedArray a = TintTypedArray.obtainStyledAttributes(context, resId, R$styleable.TextAppearance);
        if (a.hasValue(R$styleable.TextAppearance_textAllCaps)) {
            setAllCaps(a.getBoolean(R$styleable.TextAppearance_textAllCaps, false));
        }
        if (Build.VERSION.SDK_INT < 23 && a.hasValue(R$styleable.TextAppearance_android_textColor) && (textColor = a.getColorStateList(R$styleable.TextAppearance_android_textColor)) != null) {
            this.mView.setTextColor(textColor);
        }
        a.recycle();
    }

    void setAllCaps(boolean allCaps) {
        AllCapsTransformationMethod allCapsTransformationMethod;
        TextView textView = this.mView;
        if (allCaps) {
            allCapsTransformationMethod = new AllCapsTransformationMethod(this.mView.getContext());
        } else {
            allCapsTransformationMethod = null;
        }
        textView.setTransformationMethod(allCapsTransformationMethod);
    }

    void applyCompoundDrawablesTints() {
        if (this.mDrawableLeftTint == null && this.mDrawableTopTint == null && this.mDrawableRightTint == null && this.mDrawableBottomTint == null) {
            return;
        }
        Drawable[] compoundDrawables = this.mView.getCompoundDrawables();
        applyCompoundDrawableTint(compoundDrawables[0], this.mDrawableLeftTint);
        applyCompoundDrawableTint(compoundDrawables[1], this.mDrawableTopTint);
        applyCompoundDrawableTint(compoundDrawables[2], this.mDrawableRightTint);
        applyCompoundDrawableTint(compoundDrawables[3], this.mDrawableBottomTint);
    }

    final void applyCompoundDrawableTint(Drawable drawable, TintInfo info) {
        if (drawable == null || info == null) {
            return;
        }
        AppCompatDrawableManager.tintDrawable(drawable, info, this.mView.getDrawableState());
    }

    protected static TintInfo createTintInfo(Context context, AppCompatDrawableManager drawableManager, int drawableId) {
        ColorStateList tintList = drawableManager.getTintList(context, drawableId);
        if (tintList == null) {
            return null;
        }
        TintInfo tintInfo = new TintInfo();
        tintInfo.mHasTintList = true;
        tintInfo.mTintList = tintList;
        return tintInfo;
    }
}
