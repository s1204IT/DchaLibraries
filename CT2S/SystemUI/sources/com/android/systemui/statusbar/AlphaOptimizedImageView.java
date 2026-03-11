package com.android.systemui.statusbar;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class AlphaOptimizedImageView extends ImageView {
    public AlphaOptimizedImageView(Context context) {
        super(context);
    }

    public AlphaOptimizedImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AlphaOptimizedImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public AlphaOptimizedImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }
}
