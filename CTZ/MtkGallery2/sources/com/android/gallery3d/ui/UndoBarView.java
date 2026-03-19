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
    private static final int GRAY = -5592406;
    private static final long NO_ANIMATION = -1;
    private static final String TAG = "Gallery2/UndoBarView";
    private static final int WHITE = -1;
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
        this.mUndoText = StringTexture.newInstance(context.getString(R.string.undo), GalleryUtils.dpToPixel(12), GRAY, 0.0f, true);
        this.mDeletedText = StringTexture.newInstance(context.getString(R.string.deleted), GalleryUtils.dpToPixel(16), -1);
        this.mUndoIcon = new ResourceTexture(context, R.drawable.ic_menu_revert_holo_dark);
        this.mClickRegion = this.mBarMargin + this.mUndoTextMargin + this.mUndoText.getWidth() + this.mIconMargin + this.mIconSize + this.mSeparatorRightMargin;
    }

    public void setOnClickListener(GLView.OnClickListener onClickListener) {
        this.mOnClickListener = onClickListener;
    }

    @Override
    protected void onMeasure(int i, int i2) {
        setMeasuredSize(0, this.mBarHeight);
    }

    @Override
    protected void render(GLCanvas gLCanvas) {
        super.render(gLCanvas);
        advanceAnimation();
        gLCanvas.save(1);
        gLCanvas.multiplyAlpha(this.mAlpha);
        int width = getWidth();
        getHeight();
        this.mPanel.draw(gLCanvas, this.mBarMargin, 0, width - (this.mBarMargin * 2), this.mBarHeight);
        int width2 = (width - this.mBarMargin) - (this.mUndoTextMargin + this.mUndoText.getWidth());
        this.mUndoText.draw(gLCanvas, width2, (this.mBarHeight - this.mUndoText.getHeight()) / 2);
        this.mUndoIcon.draw(gLCanvas, width2 - (this.mIconMargin + this.mIconSize), (this.mBarHeight - this.mIconSize) / 2, this.mIconSize, this.mIconSize);
        gLCanvas.fillRect(r0 - (this.mSeparatorRightMargin + this.mSeparatorWidth), this.mSeparatorTopMargin, this.mSeparatorWidth, (this.mBarHeight - this.mSeparatorTopMargin) - this.mSeparatorBottomMargin, GRAY);
        this.mDeletedText.draw(gLCanvas, this.mBarMargin + this.mDeletedTextMargin, (this.mBarHeight - this.mDeletedText.getHeight()) / 2);
        gLCanvas.restore();
    }

    @Override
    protected boolean onTouch(MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        if (action != 3) {
            switch (action) {
                case 0:
                    this.mDownOnButton = inUndoButton(motionEvent);
                    break;
                case 1:
                    if (this.mDownOnButton) {
                        if (this.mOnClickListener != null && inUndoButton(motionEvent)) {
                            this.mOnClickListener.onClick(this);
                        }
                        this.mDownOnButton = false;
                    }
                    break;
            }
            return true;
        }
        this.mDownOnButton = false;
        return true;
    }

    private boolean inUndoButton(MotionEvent motionEvent) {
        float x = motionEvent.getX();
        float y = motionEvent.getY();
        int width = getWidth();
        return x >= ((float) (width - this.mClickRegion)) && x < ((float) width) && y >= 0.0f && y < ((float) getHeight());
    }

    private static float getTargetAlpha(int i) {
        return i == 0 ? 1.0f : 0.0f;
    }

    @Override
    public void setVisibility(int i) {
        this.mAlpha = getTargetAlpha(i);
        this.mAnimationStartTime = -1L;
        super.setVisibility(i);
        invalidate();
    }

    public void animateVisibility(int i) {
        float targetAlpha = getTargetAlpha(i);
        if (this.mAnimationStartTime == -1 && this.mAlpha == targetAlpha) {
            return;
        }
        if (this.mAnimationStartTime == -1 || this.mToAlpha != targetAlpha) {
            this.mFromAlpha = this.mAlpha;
            this.mToAlpha = targetAlpha;
            this.mAnimationStartTime = AnimationTime.startTime();
            super.setVisibility(0);
            invalidate();
        }
    }

    private void advanceAnimation() {
        if (this.mAnimationStartTime == -1) {
            return;
        }
        float f = (AnimationTime.get() - this.mAnimationStartTime) / ANIM_TIME;
        float f2 = this.mFromAlpha;
        if (this.mToAlpha <= this.mFromAlpha) {
            f = -f;
        }
        this.mAlpha = f2 + f;
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
