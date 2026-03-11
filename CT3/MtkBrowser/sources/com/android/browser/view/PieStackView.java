package com.android.browser.view;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;
import com.android.browser.R;

public class PieStackView extends BasePieView {
    private OnCurrentListener mCurrentListener;
    private int mMinHeight;

    public interface OnCurrentListener {
        void onSetCurrent(int i);
    }

    public PieStackView(Context ctx) {
        this.mMinHeight = (int) ctx.getResources().getDimension(R.dimen.qc_tab_title_height);
    }

    public void setOnCurrentListener(OnCurrentListener l) {
        this.mCurrentListener = l;
    }

    @Override
    public void setCurrent(int ix) {
        super.setCurrent(ix);
        if (this.mCurrentListener == null) {
            return;
        }
        this.mCurrentListener.onSetCurrent(ix);
    }

    @Override
    public void layout(int anchorX, int anchorY, boolean left, float angle, int pHeight) {
        super.layout(anchorX, anchorY, left, angle, pHeight);
        buildViews();
        this.mWidth = this.mChildWidth;
        this.mHeight = this.mChildHeight + ((this.mViews.size() - 1) * this.mMinHeight);
        this.mLeft = (left ? 5 : -(this.mChildWidth + 5)) + anchorX;
        this.mTop = anchorY - (this.mHeight / 2);
        if (this.mViews == null) {
            return;
        }
        layoutChildrenLinear();
    }

    private void layoutChildrenLinear() {
        int n = this.mViews.size();
        int top = this.mTop;
        int dy = n == 1 ? 0 : (this.mHeight - this.mChildHeight) / (n - 1);
        for (View view : this.mViews) {
            int x = this.mLeft;
            view.layout(x, top, this.mChildWidth + x, this.mChildHeight + top);
            top += dy;
        }
    }

    @Override
    public void draw(Canvas canvas) {
        if (this.mViews == null || this.mCurrent <= -1) {
            return;
        }
        int n = this.mViews.size();
        for (int i = 0; i < this.mCurrent; i++) {
            drawView(this.mViews.get(i), canvas);
        }
        for (int i2 = n - 1; i2 > this.mCurrent; i2--) {
            drawView(this.mViews.get(i2), canvas);
        }
        drawView(this.mViews.get(this.mCurrent), canvas);
    }

    @Override
    protected int findChildAt(int y) {
        int ix = ((y - this.mTop) * this.mViews.size()) / this.mHeight;
        return ix;
    }
}
