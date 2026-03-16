package com.bumptech.glide.request.target;

import android.graphics.drawable.Drawable;
import com.bumptech.glide.request.Request;

public abstract class BaseTarget<Z> implements Target<Z> {
    private Request request;

    @Override
    public void setRequest(Request request) {
        this.request = request;
    }

    @Override
    public Request getRequest() {
        return this.request;
    }

    @Override
    public void setPlaceholder(Drawable placeholder) {
    }
}
