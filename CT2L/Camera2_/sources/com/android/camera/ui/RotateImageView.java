package com.android.camera.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.media.ThumbnailUtils;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import com.android.camera.debug.Log;

public class RotateImageView extends TwoStateImageView implements Rotatable {
    private static final int ANIMATION_SPEED = 270;
    private static final Log.Tag TAG = new Log.Tag("RotateImageView");
    private long mAnimationEndTime;
    private long mAnimationStartTime;
    private boolean mClockwise;
    private int mCurrentDegree;
    private boolean mEnableAnimation;
    private int mStartDegree;
    private int mTargetDegree;
    private Bitmap mThumb;
    private TransitionDrawable mThumbTransition;
    private Drawable[] mThumbs;

    public RotateImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mCurrentDegree = 0;
        this.mStartDegree = 0;
        this.mTargetDegree = 0;
        this.mClockwise = false;
        this.mEnableAnimation = true;
        this.mAnimationStartTime = 0L;
        this.mAnimationEndTime = 0L;
    }

    public RotateImageView(Context context) {
        super(context);
        this.mCurrentDegree = 0;
        this.mStartDegree = 0;
        this.mTargetDegree = 0;
        this.mClockwise = false;
        this.mEnableAnimation = true;
        this.mAnimationStartTime = 0L;
        this.mAnimationEndTime = 0L;
    }

    protected int getDegree() {
        return this.mTargetDegree;
    }

    @Override
    public void setOrientation(int degree, boolean animation) {
        this.mEnableAnimation = animation;
        int degree2 = degree >= 0 ? degree % 360 : (degree % 360) + 360;
        if (degree2 != this.mTargetDegree) {
            this.mTargetDegree = degree2;
            if (this.mEnableAnimation) {
                this.mStartDegree = this.mCurrentDegree;
                this.mAnimationStartTime = AnimationUtils.currentAnimationTimeMillis();
                int diff = this.mTargetDegree - this.mCurrentDegree;
                if (diff < 0) {
                    diff += 360;
                }
                if (diff > 180) {
                    diff -= 360;
                }
                this.mClockwise = diff >= 0;
                this.mAnimationEndTime = this.mAnimationStartTime + ((long) ((Math.abs(diff) * 1000) / ANIMATION_SPEED));
            } else {
                this.mCurrentDegree = this.mTargetDegree;
            }
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Drawable drawable = getDrawable();
        if (drawable != null) {
            Rect bounds = drawable.getBounds();
            int w = bounds.right - bounds.left;
            int h = bounds.bottom - bounds.top;
            if (w != 0 && h != 0) {
                if (this.mCurrentDegree != this.mTargetDegree) {
                    long time = AnimationUtils.currentAnimationTimeMillis();
                    if (time < this.mAnimationEndTime) {
                        int deltaTime = (int) (time - this.mAnimationStartTime);
                        int i = this.mStartDegree;
                        if (!this.mClockwise) {
                            deltaTime = -deltaTime;
                        }
                        int degree = i + ((deltaTime * ANIMATION_SPEED) / 1000);
                        this.mCurrentDegree = degree >= 0 ? degree % 360 : (degree % 360) + 360;
                        invalidate();
                    } else {
                        this.mCurrentDegree = this.mTargetDegree;
                    }
                }
                int left = getPaddingLeft();
                int top = getPaddingTop();
                int right = getPaddingRight();
                int bottom = getPaddingBottom();
                int width = (getWidth() - left) - right;
                int height = (getHeight() - top) - bottom;
                int saveCount = canvas.getSaveCount();
                if (getScaleType() == ImageView.ScaleType.FIT_CENTER && (width < w || height < h)) {
                    float ratio = Math.min(width / w, height / h);
                    canvas.scale(ratio, ratio, width / 2.0f, height / 2.0f);
                }
                canvas.translate((width / 2) + left, (height / 2) + top);
                canvas.rotate(-this.mCurrentDegree);
                canvas.translate((-w) / 2, (-h) / 2);
                drawable.draw(canvas);
                canvas.restoreToCount(saveCount);
            }
        }
    }

    public void setBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            this.mThumb = null;
            this.mThumbs = null;
            setImageDrawable(null);
            setVisibility(8);
            return;
        }
        ViewGroup.LayoutParams param = getLayoutParams();
        int miniThumbWidth = (param.width - getPaddingLeft()) - getPaddingRight();
        int miniThumbHeight = (param.height - getPaddingTop()) - getPaddingBottom();
        this.mThumb = ThumbnailUtils.extractThumbnail(bitmap, miniThumbWidth, miniThumbHeight);
        if (this.mThumbs == null || !this.mEnableAnimation) {
            this.mThumbs = new Drawable[2];
            this.mThumbs[1] = new BitmapDrawable(getContext().getResources(), this.mThumb);
            setImageDrawable(this.mThumbs[1]);
        } else {
            this.mThumbs[0] = this.mThumbs[1];
            this.mThumbs[1] = new BitmapDrawable(getContext().getResources(), this.mThumb);
            this.mThumbTransition = new TransitionDrawable(this.mThumbs);
            setImageDrawable(this.mThumbTransition);
            this.mThumbTransition.startTransition(500);
        }
        setVisibility(0);
    }
}
