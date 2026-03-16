package com.bumptech.glide.request.target;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import com.bumptech.glide.request.GlideAnimation;

public class BitmapImageViewTarget extends ViewTarget<ImageView, Bitmap> {
    private final ImageView view;

    @Override
    public void onResourceReady(Object obj, GlideAnimation glideAnimation) {
        onResourceReady((Bitmap) obj, (GlideAnimation<Bitmap>) glideAnimation);
    }

    public BitmapImageViewTarget(ImageView view) {
        super(view);
        this.view = view;
    }

    public void onResourceReady(Bitmap resource, GlideAnimation<Bitmap> glideAnimation) {
        if (glideAnimation == null || !glideAnimation.animate(this.view.getDrawable(), resource, this.view, this)) {
            this.view.setImageBitmap(resource);
        }
    }

    @Override
    public void setPlaceholder(Drawable placeholder) {
        this.view.setImageDrawable(placeholder);
    }
}
