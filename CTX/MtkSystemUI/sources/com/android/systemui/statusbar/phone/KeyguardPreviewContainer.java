package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.WindowInsets;
import android.widget.FrameLayout;

public class KeyguardPreviewContainer extends FrameLayout {
    private Drawable mBlackBarDrawable;

    public KeyguardPreviewContainer(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mBlackBarDrawable = new Drawable() {
            @Override
            public void draw(Canvas canvas) {
                canvas.save();
                canvas.clipRect(0, KeyguardPreviewContainer.this.getHeight() - KeyguardPreviewContainer.this.getPaddingBottom(), KeyguardPreviewContainer.this.getWidth(), KeyguardPreviewContainer.this.getHeight());
                canvas.drawColor(-16777216);
                canvas.restore();
            }

            @Override
            public void setAlpha(int i) {
            }

            @Override
            public void setColorFilter(ColorFilter colorFilter) {
            }

            @Override
            public int getOpacity() {
                return -1;
            }
        };
        setBackground(this.mBlackBarDrawable);
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets windowInsets) {
        setPadding(0, 0, 0, windowInsets.getStableInsetBottom());
        return super.onApplyWindowInsets(windowInsets);
    }
}
