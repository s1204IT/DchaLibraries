package android.support.transition;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroupOverlay;

@RequiresApi(18)
class ViewGroupOverlayApi18 implements ViewGroupOverlayImpl {
    private final ViewGroupOverlay mViewGroupOverlay;

    ViewGroupOverlayApi18(@NonNull ViewGroup group) {
        this.mViewGroupOverlay = group.getOverlay();
    }

    @Override
    public void add(@NonNull Drawable drawable) {
        this.mViewGroupOverlay.add(drawable);
    }

    @Override
    public void clear() {
        this.mViewGroupOverlay.clear();
    }

    @Override
    public void remove(@NonNull Drawable drawable) {
        this.mViewGroupOverlay.remove(drawable);
    }

    @Override
    public void add(@NonNull View view) {
        this.mViewGroupOverlay.add(view);
    }

    @Override
    public void remove(@NonNull View view) {
        this.mViewGroupOverlay.remove(view);
    }
}
