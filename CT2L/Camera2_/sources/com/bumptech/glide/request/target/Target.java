package com.bumptech.glide.request.target;

import android.graphics.drawable.Drawable;
import com.bumptech.glide.request.GlideAnimation;
import com.bumptech.glide.request.Request;

public interface Target<R> {

    public interface SizeReadyCallback {
        void onSizeReady(int i, int i2);
    }

    Request getRequest();

    void getSize(SizeReadyCallback sizeReadyCallback);

    void onResourceReady(R r, GlideAnimation<R> glideAnimation);

    void setPlaceholder(Drawable drawable);

    void setRequest(Request request);
}
