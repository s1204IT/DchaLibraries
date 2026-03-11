package com.android.setupwizardlib.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import com.android.setupwizardlib.R;

public class StatusBarBackgroundLayout extends FrameLayout {
    private Object mLastInsets;
    private Drawable mStatusBarBackground;

    public StatusBarBackgroundLayout(Context context) {
        super(context);
        init(context, null, 0);
    }

    public StatusBarBackgroundLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context, attributeSet, 0);
    }

    @TargetApi(11)
    public StatusBarBackgroundLayout(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        init(context, attributeSet, i);
    }

    private void init(Context context, AttributeSet attributeSet, int i) {
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.SuwStatusBarBackgroundLayout, i, 0);
        setStatusBarBackground(typedArrayObtainStyledAttributes.getDrawable(R.styleable.SuwStatusBarBackgroundLayout_suwStatusBarBackground));
        typedArrayObtainStyledAttributes.recycle();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (Build.VERSION.SDK_INT >= 21 && this.mLastInsets == null) {
            requestApplyInsets();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int systemWindowInsetTop;
        super.onDraw(canvas);
        if (Build.VERSION.SDK_INT >= 21 && this.mLastInsets != null && (systemWindowInsetTop = ((WindowInsets) this.mLastInsets).getSystemWindowInsetTop()) > 0) {
            this.mStatusBarBackground.setBounds(0, 0, getWidth(), systemWindowInsetTop);
            this.mStatusBarBackground.draw(canvas);
        }
    }

    public void setStatusBarBackground(Drawable drawable) {
        this.mStatusBarBackground = drawable;
        if (Build.VERSION.SDK_INT >= 21) {
            setWillNotDraw(drawable == null);
            setFitsSystemWindows(drawable != null);
            invalidate();
        }
    }

    public Drawable getStatusBarBackground() {
        return this.mStatusBarBackground;
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets windowInsets) {
        this.mLastInsets = windowInsets;
        return super.onApplyWindowInsets(windowInsets);
    }
}
