package com.android.browser.view;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;
import com.android.browser.R;
import java.util.Iterator;

/* loaded from: classes.dex */
public class PieStackView extends BasePieView {
    private OnCurrentListener mCurrentListener;
    private int mMinHeight;

    public interface OnCurrentListener {
        void onSetCurrent(int i);
    }

    public PieStackView(Context context) {
        this.mMinHeight = (int) context.getResources().getDimension(R.dimen.qc_tab_title_height);
    }

    public void setOnCurrentListener(OnCurrentListener onCurrentListener) {
        this.mCurrentListener = onCurrentListener;
    }

    @Override // com.android.browser.view.BasePieView
    public void setCurrent(int i) {
        super.setCurrent(i);
        if (this.mCurrentListener != null) {
            this.mCurrentListener.onSetCurrent(i);
        }
    }

    @Override // com.android.browser.view.BasePieView, com.android.browser.view.PieMenu.PieView
    public void layout(int i, int i2, boolean z, float f, int i3) {
        super.layout(i, i2, z, f, i3);
        buildViews();
        this.mWidth = this.mChildWidth;
        this.mHeight = this.mChildHeight + ((this.mViews.size() - 1) * this.mMinHeight);
        this.mLeft = i + (z ? 5 : -(5 + this.mChildWidth));
        this.mTop = i2 - (this.mHeight / 2);
        if (this.mViews != null) {
            layoutChildrenLinear();
        }
    }

    private void layoutChildrenLinear() {
        int size = this.mViews.size();
        int i = this.mTop;
        int i2 = size == 1 ? 0 : (this.mHeight - this.mChildHeight) / (size - 1);
        Iterator<View> it = this.mViews.iterator();
        while (it.hasNext()) {
            View next = it.next();
            int i3 = this.mLeft;
            next.layout(i3, i, this.mChildWidth + i3, this.mChildHeight + i);
            i += i2;
        }
    }

    /* JADX DEBUG: Move duplicate insns, count: 1 to block B:11:0x0024 */
    @Override // com.android.browser.view.PieMenu.PieView
    public void draw(Canvas canvas) {
        if (this.mViews != null && this.mCurrent > -1) {
            int size = this.mViews.size();
            for (int i = 0; i < this.mCurrent; i++) {
                drawView(this.mViews.get(i), canvas);
            }
            while (true) {
                size--;
                if (size > this.mCurrent) {
                    drawView(this.mViews.get(size), canvas);
                } else {
                    drawView(this.mViews.get(this.mCurrent), canvas);
                    return;
                }
            }
        }
    }

    @Override // com.android.browser.view.BasePieView
    protected int findChildAt(int i) {
        return ((i - this.mTop) * this.mViews.size()) / this.mHeight;
    }
}
