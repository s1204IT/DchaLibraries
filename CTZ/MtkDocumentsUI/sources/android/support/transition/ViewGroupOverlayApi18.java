package android.support.transition;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroupOverlay;

class ViewGroupOverlayApi18 implements ViewGroupOverlayImpl {
    private final ViewGroupOverlay mViewGroupOverlay;

    ViewGroupOverlayApi18(ViewGroup group) {
        this.mViewGroupOverlay = group.getOverlay();
    }

    @Override
    public void add(Drawable drawable) {
        this.mViewGroupOverlay.add(drawable);
    }

    @Override
    public void remove(Drawable drawable) {
        this.mViewGroupOverlay.remove(drawable);
    }

    @Override
    public void add(View view) {
        this.mViewGroupOverlay.add(view);
    }

    @Override
    public void remove(View view) {
        this.mViewGroupOverlay.remove(view);
    }
}
