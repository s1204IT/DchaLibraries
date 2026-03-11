package com.android.settings.widget;

import android.animation.AnimatorInflater;
import android.content.Context;
import android.graphics.Outline;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import com.android.settings.R;

public class FloatingActionButton extends ImageView {
    public FloatingActionButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        setScaleType(ImageView.ScaleType.CENTER);
        setStateListAnimator(AnimatorInflater.loadStateListAnimator(context, R.anim.fab_elevation));
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setOval(0, 0, FloatingActionButton.this.getWidth(), FloatingActionButton.this.getHeight());
            }
        });
        setClipToOutline(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        invalidateOutline();
    }
}
