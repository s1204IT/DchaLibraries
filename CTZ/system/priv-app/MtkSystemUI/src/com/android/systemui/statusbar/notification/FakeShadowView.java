package com.android.systemui.statusbar.notification;

import android.content.Context;
import android.graphics.Outline;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.LinearLayout;
import com.android.systemui.R;
import com.android.systemui.statusbar.AlphaOptimizedFrameLayout;

/* loaded from: classes.dex */
public class FakeShadowView extends AlphaOptimizedFrameLayout {
    private View mFakeShadow;
    private float mOutlineAlpha;
    private final int mShadowMinHeight;

    public FakeShadowView(Context context) {
        this(context, null);
    }

    public FakeShadowView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public FakeShadowView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public FakeShadowView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mFakeShadow = new View(context);
        this.mFakeShadow.setVisibility(4);
        this.mFakeShadow.setLayoutParams(new LinearLayout.LayoutParams(-1, (int) (48.0f * getResources().getDisplayMetrics().density)));
        this.mFakeShadow.setOutlineProvider(new ViewOutlineProvider() { // from class: com.android.systemui.statusbar.notification.FakeShadowView.1
            @Override // android.view.ViewOutlineProvider
            public void getOutline(View view, Outline outline) {
                outline.setRect(0, 0, FakeShadowView.this.getWidth(), FakeShadowView.this.mFakeShadow.getHeight());
                outline.setAlpha(FakeShadowView.this.mOutlineAlpha);
            }
        });
        addView(this.mFakeShadow);
        this.mShadowMinHeight = Math.max(1, context.getResources().getDimensionPixelSize(R.dimen.notification_divider_height));
    }

    public void setFakeShadowTranslationZ(float f, float f2, int i, int i2) {
        if (f == 0.0f) {
            this.mFakeShadow.setVisibility(4);
            return;
        }
        this.mFakeShadow.setVisibility(0);
        this.mFakeShadow.setTranslationZ(Math.max(this.mShadowMinHeight, f));
        this.mFakeShadow.setTranslationX(i2);
        this.mFakeShadow.setTranslationY(i - this.mFakeShadow.getHeight());
        if (f2 != this.mOutlineAlpha) {
            this.mOutlineAlpha = f2;
            this.mFakeShadow.invalidateOutline();
        }
    }
}
