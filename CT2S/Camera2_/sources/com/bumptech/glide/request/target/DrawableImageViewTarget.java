package com.bumptech.glide.request.target;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import com.bumptech.glide.request.GlideAnimation;

public class DrawableImageViewTarget extends ViewTarget<ImageView, Drawable> {
    private static final float SQUARE_RATIO_MARGIN = 0.05f;
    private final ImageView view;

    @Override
    public void onResourceReady(Object obj, GlideAnimation glideAnimation) {
        onResourceReady((Drawable) obj, (GlideAnimation<Drawable>) glideAnimation);
    }

    public DrawableImageViewTarget(ImageView view) {
        super(view);
        this.view = view;
    }

    public void onResourceReady(Drawable resource, GlideAnimation<Drawable> animation) {
        float viewRatio = this.view.getWidth() / this.view.getHeight();
        float drawableRatio = resource.getIntrinsicWidth() / resource.getIntrinsicHeight();
        if (Math.abs(viewRatio - 1.0f) <= SQUARE_RATIO_MARGIN && Math.abs(drawableRatio - 1.0f) <= SQUARE_RATIO_MARGIN) {
            resource = new SquaringDrawable(resource, this.view.getWidth());
        }
        if (animation == null || !animation.animate(this.view.getDrawable(), resource, this.view, this)) {
            this.view.setImageDrawable(resource);
        }
    }

    @Override
    public void setPlaceholder(Drawable placeholder) {
        this.view.setImageDrawable(placeholder);
    }
}
