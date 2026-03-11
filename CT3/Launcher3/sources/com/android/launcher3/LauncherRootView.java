package com.android.launcher3;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class LauncherRootView extends InsettableFrameLayout {
    private View mAlignedView;
    private boolean mDrawRightInsetBar;
    private final Paint mOpaquePaint;
    private int mRightInsetBarWidth;

    public LauncherRootView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mOpaquePaint = new Paint(1);
        this.mOpaquePaint.setColor(-16777216);
        this.mOpaquePaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onFinishInflate() {
        if (getChildCount() > 0) {
            this.mAlignedView = getChildAt(0);
        }
        super.onFinishInflate();
    }

    @Override
    @TargetApi(23)
    protected boolean fitSystemWindows(Rect insets) {
        boolean zIsLowRamDevice;
        if (insets.right > 0) {
            zIsLowRamDevice = Utilities.ATLEAST_MARSHMALLOW ? ((ActivityManager) getContext().getSystemService(ActivityManager.class)).isLowRamDevice() : true;
        } else {
            zIsLowRamDevice = false;
        }
        this.mDrawRightInsetBar = zIsLowRamDevice;
        this.mRightInsetBarWidth = insets.right;
        setInsets(this.mDrawRightInsetBar ? new Rect(0, insets.top, 0, insets.bottom) : insets);
        if (this.mAlignedView != null && this.mDrawRightInsetBar) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) this.mAlignedView.getLayoutParams();
            if (lp.leftMargin != insets.left || lp.rightMargin != insets.right) {
                lp.leftMargin = insets.left;
                lp.rightMargin = insets.right;
                this.mAlignedView.setLayoutParams(lp);
            }
        }
        return true;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (!this.mDrawRightInsetBar) {
            return;
        }
        int width = getWidth();
        canvas.drawRect(width - this.mRightInsetBarWidth, 0.0f, width, getHeight(), this.mOpaquePaint);
    }
}
