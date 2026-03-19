package android.support.design.widget;

import android.graphics.Rect;
import android.graphics.drawable.InsetDrawable;
import android.os.Build;
import com.android.contacts.ContactPhotoManager;

class FloatingActionButtonImplLollipop extends FloatingActionButtonImpl {
    private InsetDrawable insetDrawable;

    FloatingActionButtonImplLollipop(VisibilityAwareImageButton view, ShadowViewDelegate shadowViewDelegate) {
        super(view, shadowViewDelegate);
    }

    @Override
    public float getElevation() {
        return this.view.getElevation();
    }

    @Override
    void onPaddingUpdated(Rect padding) {
        if (this.shadowViewDelegate.isCompatPaddingEnabled()) {
            this.insetDrawable = new InsetDrawable(this.rippleDrawable, padding.left, padding.top, padding.right, padding.bottom);
            this.shadowViewDelegate.setBackgroundDrawable(this.insetDrawable);
        } else {
            this.shadowViewDelegate.setBackgroundDrawable(this.rippleDrawable);
        }
    }

    @Override
    void onDrawableStateChanged(int[] state) {
        if (Build.VERSION.SDK_INT == 21) {
            if (this.view.isEnabled()) {
                this.view.setElevation(this.elevation);
                if (this.view.isPressed()) {
                    this.view.setTranslationZ(this.pressedTranslationZ);
                    return;
                } else if (this.view.isFocused() || this.view.isHovered()) {
                    this.view.setTranslationZ(this.hoveredFocusedTranslationZ);
                    return;
                } else {
                    this.view.setTranslationZ(ContactPhotoManager.OFFSET_DEFAULT);
                    return;
                }
            }
            this.view.setElevation(ContactPhotoManager.OFFSET_DEFAULT);
            this.view.setTranslationZ(ContactPhotoManager.OFFSET_DEFAULT);
        }
    }

    @Override
    void jumpDrawableToCurrentState() {
    }

    @Override
    boolean requirePreDrawListener() {
        return false;
    }

    @Override
    void getPadding(Rect rect) {
        if (this.shadowViewDelegate.isCompatPaddingEnabled()) {
            float radius = this.shadowViewDelegate.getRadius();
            float maxShadowSize = getElevation() + this.pressedTranslationZ;
            int hPadding = (int) Math.ceil(ShadowDrawableWrapper.calculateHorizontalPadding(maxShadowSize, radius, false));
            int vPadding = (int) Math.ceil(ShadowDrawableWrapper.calculateVerticalPadding(maxShadowSize, radius, false));
            rect.set(hPadding, vPadding, hPadding, vPadding);
            return;
        }
        rect.set(0, 0, 0, 0);
    }
}
