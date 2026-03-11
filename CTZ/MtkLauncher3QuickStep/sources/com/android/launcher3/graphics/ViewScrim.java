package com.android.launcher3.graphics;

import android.graphics.Canvas;
import android.support.v4.app.NotificationCompat;
import android.util.Property;
import android.view.View;
import com.android.launcher3.R;

public abstract class ViewScrim<T extends View> {
    public static Property<ViewScrim, Float> PROGRESS = new Property<ViewScrim, Float>(Float.TYPE, NotificationCompat.CATEGORY_PROGRESS) {
        @Override
        public Float get(ViewScrim viewScrim) {
            return Float.valueOf(viewScrim.mProgress);
        }

        @Override
        public void set(ViewScrim viewScrim, Float f) {
            viewScrim.setProgress(f.floatValue());
        }
    };
    protected float mProgress = 0.0f;
    protected final T mView;

    public abstract void draw(Canvas canvas, int i, int i2);

    public ViewScrim(T t) {
        this.mView = t;
    }

    public void attach() {
        this.mView.setTag(R.id.view_scrim, this);
    }

    public void setProgress(float f) {
        if (this.mProgress != f) {
            this.mProgress = f;
            onProgressChanged();
            invalidate();
        }
    }

    protected void onProgressChanged() {
    }

    public void invalidate() {
        Object parent = this.mView.getParent();
        if (parent != null) {
            ((View) parent).invalidate();
        }
    }

    public static ViewScrim get(View view) {
        return (ViewScrim) view.getTag(R.id.view_scrim);
    }
}
