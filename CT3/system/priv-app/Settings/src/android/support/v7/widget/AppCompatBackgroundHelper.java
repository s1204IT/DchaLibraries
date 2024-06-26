package android.support.v7.widget;

import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v7.appcompat.R$styleable;
import android.util.AttributeSet;
import android.view.View;
/* loaded from: classes.dex */
class AppCompatBackgroundHelper {
    private TintInfo mBackgroundTint;
    private final AppCompatDrawableManager mDrawableManager;
    private TintInfo mInternalBackgroundTint;
    private TintInfo mTmpInfo;
    private final View mView;

    /* JADX INFO: Access modifiers changed from: package-private */
    public AppCompatBackgroundHelper(View view, AppCompatDrawableManager drawableManager) {
        this.mView = view;
        this.mDrawableManager = drawableManager;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void loadFromAttributes(AttributeSet attrs, int defStyleAttr) {
        ColorStateList tint;
        TintTypedArray a = TintTypedArray.obtainStyledAttributes(this.mView.getContext(), attrs, R$styleable.ViewBackgroundHelper, defStyleAttr, 0);
        try {
            if (a.hasValue(R$styleable.ViewBackgroundHelper_android_background) && (tint = this.mDrawableManager.getTintList(this.mView.getContext(), a.getResourceId(R$styleable.ViewBackgroundHelper_android_background, -1))) != null) {
                setInternalBackgroundTint(tint);
            }
            if (a.hasValue(R$styleable.ViewBackgroundHelper_backgroundTint)) {
                ViewCompat.setBackgroundTintList(this.mView, a.getColorStateList(R$styleable.ViewBackgroundHelper_backgroundTint));
            }
            if (a.hasValue(R$styleable.ViewBackgroundHelper_backgroundTintMode)) {
                ViewCompat.setBackgroundTintMode(this.mView, DrawableUtils.parseTintMode(a.getInt(R$styleable.ViewBackgroundHelper_backgroundTintMode, -1), null));
            }
        } finally {
            a.recycle();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSetBackgroundResource(int resId) {
        setInternalBackgroundTint(this.mDrawableManager != null ? this.mDrawableManager.getTintList(this.mView.getContext(), resId) : null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSetBackgroundDrawable(Drawable background) {
        setInternalBackgroundTint(null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSupportBackgroundTintList(ColorStateList tint) {
        if (this.mBackgroundTint == null) {
            this.mBackgroundTint = new TintInfo();
        }
        this.mBackgroundTint.mTintList = tint;
        this.mBackgroundTint.mHasTintList = true;
        applySupportBackgroundTint();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ColorStateList getSupportBackgroundTintList() {
        if (this.mBackgroundTint != null) {
            return this.mBackgroundTint.mTintList;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSupportBackgroundTintMode(PorterDuff.Mode tintMode) {
        if (this.mBackgroundTint == null) {
            this.mBackgroundTint = new TintInfo();
        }
        this.mBackgroundTint.mTintMode = tintMode;
        this.mBackgroundTint.mHasTintMode = true;
        applySupportBackgroundTint();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PorterDuff.Mode getSupportBackgroundTintMode() {
        if (this.mBackgroundTint != null) {
            return this.mBackgroundTint.mTintMode;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void applySupportBackgroundTint() {
        Drawable background = this.mView.getBackground();
        if (background == null) {
            return;
        }
        if (Build.VERSION.SDK_INT == 21 && applyFrameworkTintUsingColorFilter(background)) {
            return;
        }
        if (this.mBackgroundTint != null) {
            AppCompatDrawableManager.tintDrawable(background, this.mBackgroundTint, this.mView.getDrawableState());
        } else if (this.mInternalBackgroundTint == null) {
        } else {
            AppCompatDrawableManager.tintDrawable(background, this.mInternalBackgroundTint, this.mView.getDrawableState());
        }
    }

    void setInternalBackgroundTint(ColorStateList tint) {
        if (tint != null) {
            if (this.mInternalBackgroundTint == null) {
                this.mInternalBackgroundTint = new TintInfo();
            }
            this.mInternalBackgroundTint.mTintList = tint;
            this.mInternalBackgroundTint.mHasTintList = true;
        } else {
            this.mInternalBackgroundTint = null;
        }
        applySupportBackgroundTint();
    }

    private boolean applyFrameworkTintUsingColorFilter(@NonNull Drawable background) {
        if (this.mTmpInfo == null) {
            this.mTmpInfo = new TintInfo();
        }
        TintInfo info = this.mTmpInfo;
        info.clear();
        ColorStateList tintList = ViewCompat.getBackgroundTintList(this.mView);
        if (tintList != null) {
            info.mHasTintList = true;
            info.mTintList = tintList;
        }
        PorterDuff.Mode mode = ViewCompat.getBackgroundTintMode(this.mView);
        if (mode != null) {
            info.mHasTintMode = true;
            info.mTintMode = mode;
        }
        if (info.mHasTintList || info.mHasTintMode) {
            AppCompatDrawableManager.tintDrawable(background, info, this.mView.getDrawableState());
            return true;
        }
        return false;
    }
}
