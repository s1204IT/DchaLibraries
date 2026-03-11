package android.support.v7.widget;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.appcompat.R$styleable;
import android.util.AttributeSet;
import android.widget.ImageView;

public class AppCompatImageHelper {
    private final AppCompatDrawableManager mDrawableManager;
    private final ImageView mView;

    public AppCompatImageHelper(ImageView view, AppCompatDrawableManager drawableManager) {
        this.mView = view;
        this.mDrawableManager = drawableManager;
    }

    public void loadFromAttributes(AttributeSet attrs, int defStyleAttr) {
        int id;
        TintTypedArray a = null;
        try {
            Drawable drawable = this.mView.getDrawable();
            if (drawable == null && (id = (a = TintTypedArray.obtainStyledAttributes(this.mView.getContext(), attrs, R$styleable.AppCompatImageView, defStyleAttr, 0)).getResourceId(R$styleable.AppCompatImageView_srcCompat, -1)) != -1 && (drawable = this.mDrawableManager.getDrawable(this.mView.getContext(), id)) != null) {
                this.mView.setImageDrawable(drawable);
            }
            if (drawable != null) {
                DrawableUtils.fixDrawable(drawable);
            }
        } finally {
            if (a != null) {
                a.recycle();
            }
        }
    }

    public void setImageResource(int resId) {
        Drawable d;
        if (resId != 0) {
            if (this.mDrawableManager != null) {
                d = this.mDrawableManager.getDrawable(this.mView.getContext(), resId);
            } else {
                d = ContextCompat.getDrawable(this.mView.getContext(), resId);
            }
            if (d != null) {
                DrawableUtils.fixDrawable(d);
            }
            this.mView.setImageDrawable(d);
            return;
        }
        this.mView.setImageDrawable(null);
    }

    boolean hasOverlappingRendering() {
        Drawable background = this.mView.getBackground();
        if (Build.VERSION.SDK_INT >= 21 && (background instanceof RippleDrawable)) {
            return false;
        }
        return true;
    }
}
