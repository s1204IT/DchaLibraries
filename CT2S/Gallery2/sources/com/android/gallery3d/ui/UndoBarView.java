package com.android.gallery3d.ui;

import android.content.Context;
import android.view.MotionEvent;
import com.android.gallery3d.R;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.glrenderer.NinePatchTexture;
import com.android.gallery3d.glrenderer.ResourceTexture;
import com.android.gallery3d.glrenderer.StringTexture;
import com.android.gallery3d.ui.GLView;
import com.android.gallery3d.util.GalleryUtils;

public class UndoBarView extends GLView {
    private static long ANIM_TIME = 200;
    private float mAlpha;
    private final int mClickRegion;
    private final StringTexture mDeletedText;
    private boolean mDownOnButton;
    private float mFromAlpha;
    private GLView.OnClickListener mOnClickListener;
    private final NinePatchTexture mPanel;
    private float mToAlpha;
    private final ResourceTexture mUndoIcon;
    private final StringTexture mUndoText;
    private long mAnimationStartTime = -1;
    private final int mBarHeight = GalleryUtils.dpToPixel(48);
    private final int mBarMargin = GalleryUtils.dpToPixel(4);
    private final int mUndoTextMargin = GalleryUtils.dpToPixel(16);
    private final int mIconMargin = GalleryUtils.dpToPixel(8);
    private final int mIconSize = GalleryUtils.dpToPixel(32);
    private final int mSeparatorRightMargin = GalleryUtils.dpToPixel(12);
    private final int mSeparatorTopMargin = GalleryUtils.dpToPixel(10);
    private final int mSeparatorBottomMargin = GalleryUtils.dpToPixel(10);
    private final int mSeparatorWidth = GalleryUtils.dpToPixel(1);
    private final int mDeletedTextMargin = GalleryUtils.dpToPixel(16);

    public UndoBarView(Context context) {
        this.mPanel = new NinePatchTexture(context, R.drawable.panel_undo_holo);
        this.mUndoText = StringTexture.newInstance(context.getString(R.string.undo), GalleryUtils.dpToPixel(12), -5592406, 0.0f, true);
        this.mDeletedText = StringTexture.newInstance(context.getString(R.string.deleted), GalleryUtils.dpToPixel(16), -1);
        this.mUndoIcon = new ResourceTexture(context, R.drawable.ic_menu_revert_holo_dark);
        this.mClickRegion = this.mBarMargin + this.mUndoTextMargin + this.mUndoText.getWidth() + this.mIconMargin + this.mIconSize + this.mSeparatorRightMargin;
    }

    public void setOnClickListener(GLView.OnClickListener listener) {
        this.mOnClickListener = listener;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredSize(0, this.mBarHeight);
    }

    @Override
    protected void render(GLCanvas canvas) {
        super.render(canvas);
        advanceAnimation();
        canvas.save(1);
        canvas.multiplyAlpha(this.mAlpha);
        int w = getWidth();
        getHeight();
        this.mPanel.draw(canvas, this.mBarMargin, 0, w - (this.mBarMargin * 2), this.mBarHeight);
        int x = (w - this.mBarMargin) - (this.mUndoTextMargin + this.mUndoText.getWidth());
        int y = (this.mBarHeight - this.mUndoText.getHeight()) / 2;
        this.mUndoText.draw(canvas, x, y);
        int x2 = x - (this.mIconMargin + this.mIconSize);
        int y2 = (this.mBarHeight - this.mIconSize) / 2;
        this.mUndoIcon.draw(canvas, x2, y2, this.mIconSize, this.mIconSize);
        int x3 = x2 - (this.mSeparatorRightMargin + this.mSeparatorWidth);
        int y3 = this.mSeparatorTopMargin;
        canvas.fillRect(x3, y3, this.mSeparatorWidth, (this.mBarHeight - this.mSeparatorTopMargin) - this.mSeparatorBottomMargin, -5592406);
        int x4 = this.mBarMargin + this.mDeletedTextMargin;
        int y4 = (this.mBarHeight - this.mDeletedText.getHeight()) / 2;
        this.mDeletedText.draw(canvas, x4, y4);
        canvas.restore();
    }

    @Override
    protected boolean onTouch(MotionEvent event) {
        switch (event.getAction()) {
            case 0:
                this.mDownOnButton = inUndoButton(event);
                break;
            case 1:
                if (this.mDownOnButton) {
                    if (this.mOnClickListener != null && inUndoButton(event)) {
                        this.mOnClickListener.onClick(this);
                    }
                    this.mDownOnButton = false;
                }
                break;
            case 3:
                this.mDownOnButton = false;
                break;
        }
        return true;
    }

    private boolean inUndoButton(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int w = getWidth();
        int h = getHeight();
        return x >= ((float) (w - this.mClickRegion)) && x < ((float) w) && y >= 0.0f && y < ((float) h);
    }

    private static float getTargetAlpha(int visibility) {
        return visibility == 0 ? 1.0f : 0.0f;
    }

    @Override
    public void setVisibility(int visibility) {
        this.mAlpha = getTargetAlpha(visibility);
        this.mAnimationStartTime = -1L;
        super.setVisibility(visibility);
        invalidate();
    }

    public void animateVisibility(int visibility) {
        float target = getTargetAlpha(visibility);
        if (this.mAnimationStartTime == -1 && this.mAlpha == target) {
            return;
        }
        if (this.mAnimationStartTime == -1 || this.mToAlpha != target) {
            this.mFromAlpha = this.mAlpha;
            this.mToAlpha = target;
            this.mAnimationStartTime = AnimationTime.startTime();
            super.setVisibility(0);
            invalidate();
        }
    }

    private void advanceAnimation() {
        if (this.mAnimationStartTime != -1) {
            float delta = (AnimationTime.get() - this.mAnimationStartTime) / ANIM_TIME;
            float f = this.mFromAlpha;
            if (this.mToAlpha <= this.mFromAlpha) {
                delta = -delta;
            }
            this.mAlpha = f + delta;
            this.mAlpha = Utils.clamp(this.mAlpha, 0.0f, 1.0f);
            if (this.mAlpha == this.mToAlpha) {
                this.mAnimationStartTime = -1L;
                if (this.mAlpha == 0.0f) {
                    super.setVisibility(1);
                }
            }
            invalidate();
        }
    }
}
