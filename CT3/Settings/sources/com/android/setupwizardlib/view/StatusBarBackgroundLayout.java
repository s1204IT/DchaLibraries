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
import com.android.setupwizardlib.R$styleable;

public class StatusBarBackgroundLayout extends FrameLayout {
    private Object mLastInsets;
    private Drawable mStatusBarBackground;

    public StatusBarBackgroundLayout(Context context) {
        super(context);
        init(context, null, 0);
    }

    public StatusBarBackgroundLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    @TargetApi(11)
    public StatusBarBackgroundLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.obtainStyledAttributes(attrs, R$styleable.SuwStatusBarBackgroundLayout, defStyleAttr, 0);
        Drawable statusBarBackground = a.getDrawable(R$styleable.SuwStatusBarBackgroundLayout_suwStatusBarBackground);
        setStatusBarBackground(statusBarBackground);
        a.recycle();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (Build.VERSION.SDK_INT < 21 || this.mLastInsets != null) {
            return;
        }
        requestApplyInsets();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int insetTop;
        super.onDraw(canvas);
        if (Build.VERSION.SDK_INT < 21 || this.mLastInsets == null || (insetTop = ((WindowInsets) this.mLastInsets).getSystemWindowInsetTop()) <= 0) {
            return;
        }
        this.mStatusBarBackground.setBounds(0, 0, getWidth(), insetTop);
        this.mStatusBarBackground.draw(canvas);
    }

    public void setStatusBarBackground(Drawable background) {
        this.mStatusBarBackground = background;
        if (Build.VERSION.SDK_INT < 21) {
            return;
        }
        setWillNotDraw(background == null);
        setFitsSystemWindows(background != null);
        invalidate();
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        this.mLastInsets = insets;
        return super.onApplyWindowInsets(insets);
    }
}
