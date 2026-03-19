package com.mediatek.camera.feature.mode.panorama;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.view.View;
import com.mediatek.camera.R;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;

class ProgressBarDrawable extends Drawable {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(ProgressBarDrawable.class.getSimpleName());
    private View mAttachedView;
    private int[] mBlockSizes;
    private Drawable mCleanBlock;
    private Drawable mDirtyBlock;
    private int mPadding;
    private final Paint mPaint = new Paint();

    public ProgressBarDrawable(Context context, View view, int[] iArr, int i) {
        this.mBlockSizes = null;
        Resources resources = context.getResources();
        this.mBlockSizes = iArr;
        this.mPadding = i;
        this.mCleanBlock = resources.getDrawable(R.drawable.ic_panorama_block);
        this.mDirtyBlock = resources.getDrawable(R.drawable.ic_panorama_block_highlight);
        this.mAttachedView = view;
    }

    @Override
    protected boolean onLevelChange(int i) {
        LogHelper.d(TAG, "[onLevelChange:]level = " + i);
        invalidateSelf();
        return true;
    }

    @Override
    public int getIntrinsicWidth() {
        int length = this.mBlockSizes.length;
        int i = 0;
        for (int i2 = 0; i2 < length - 1; i2++) {
            i += this.mBlockSizes[i2] + this.mPadding;
        }
        int i3 = i + this.mBlockSizes[this.mBlockSizes.length - 1];
        LogHelper.d(TAG, "[getIntrinsicWidth]" + i3);
        return i3;
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        this.mPaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return -3;
    }

    @Override
    public void setAlpha(int i) {
        this.mPaint.setAlpha(i);
    }

    @Override
    public void draw(Canvas canvas) {
        int level = getLevel();
        int i = 0;
        for (int i2 = 0; i2 < level; i2++) {
            int height = (this.mAttachedView.getHeight() - this.mBlockSizes[i2]) / 2;
            this.mDirtyBlock.setBounds(i, height, this.mBlockSizes[i2] + i, this.mBlockSizes[i2] + height);
            this.mDirtyBlock.draw(canvas);
            LogHelper.v(TAG, "[draw]dirty block,i=" + i2 + " xoffset = " + i + " yoffset = " + height);
            i += this.mBlockSizes[i2] + this.mPadding;
        }
        int length = this.mBlockSizes.length;
        while (level < length) {
            int height2 = (this.mAttachedView.getHeight() - this.mBlockSizes[level]) / 2;
            this.mCleanBlock.setBounds(i, height2, this.mBlockSizes[level] + i, this.mBlockSizes[level] + height2);
            this.mCleanBlock.draw(canvas);
            LogHelper.d(TAG, "[draw]rest,i=" + level + " xoffset = " + i + " yoffset = " + height2);
            i += this.mBlockSizes[level] + this.mPadding;
            level++;
        }
    }
}
