package com.mediatek.camera.feature.mode.panorama;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;

public class NaviLineImageView extends ImageView {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(NaviLineImageView.class.getSimpleName());
    private int mBottom;
    private boolean mFirstDraw;
    private int mLeft;
    private int mRight;
    private int mTop;

    public NaviLineImageView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mLeft = 0;
        this.mTop = 0;
        this.mRight = 0;
        this.mBottom = 0;
        this.mFirstDraw = false;
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        LogHelper.v(TAG, "[onLayout]changed=" + z + " left =" + i + " top = " + i2 + " right = " + i3 + " bottom = " + i4);
        super.onLayout(z, i, i2, i3, i4);
    }

    @Override
    public void layout(int i, int i2, int i3, int i4) {
        LogHelper.v(TAG, "[layout]left =" + i + " top = " + i2 + " right = " + i3 + " bottom = " + i4);
        if (!this.mFirstDraw || (this.mLeft == i && this.mTop == i2 && this.mRight == i3 && this.mBottom == i4)) {
            super.layout(i, i2, i3, i4);
            this.mFirstDraw = true;
        }
    }

    public void setLayoutPosition(int i, int i2, int i3, int i4) {
        LogHelper.v(TAG, "[setLayoutPosition] left =" + i + " top = " + i2 + " right = " + i3 + " bottom = " + i4);
        this.mLeft = i;
        this.mTop = i2;
        this.mRight = i3;
        this.mBottom = i4;
        layout(i, i2, i3, i4);
    }
}
