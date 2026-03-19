package com.android.internal.policy;

import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.view.Choreographer;
import android.view.DisplayListCanvas;
import android.view.RenderNode;
import android.view.ThreadedRenderer;

public class BackdropFrameRenderer extends Thread implements Choreographer.FrameCallback {
    private Drawable mCaptionBackgroundDrawable;
    private Choreographer mChoreographer;
    private DecorView mDecorView;
    private RenderNode mFrameAndBackdropNode;
    private boolean mFullscreen;
    private int mLastCaptionHeight;
    private int mLastContentHeight;
    private int mLastContentWidth;
    private int mLastXOffset;
    private int mLastYOffset;
    private ColorDrawable mNavigationBarColor;
    private boolean mOldFullscreen;
    private ThreadedRenderer mRenderer;
    private boolean mReportNextDraw;
    private final int mResizeMode;
    private Drawable mResizingBackgroundDrawable;
    private ColorDrawable mStatusBarColor;
    private RenderNode mSystemBarBackgroundNode;
    private Drawable mUserCaptionBackgroundDrawable;
    private final Rect mTargetRect = new Rect();
    private final Rect mOldTargetRect = new Rect();
    private final Rect mNewTargetRect = new Rect();
    private final Rect mOldSystemInsets = new Rect();
    private final Rect mOldStableInsets = new Rect();
    private final Rect mSystemInsets = new Rect();
    private final Rect mStableInsets = new Rect();
    private final Rect mTmpRect = new Rect();

    public BackdropFrameRenderer(DecorView decorView, ThreadedRenderer threadedRenderer, Rect rect, Drawable drawable, Drawable drawable2, Drawable drawable3, int i, int i2, boolean z, Rect rect2, Rect rect3, int i3) {
        setName("ResizeFrame");
        this.mRenderer = threadedRenderer;
        onResourcesLoaded(decorView, drawable, drawable2, drawable3, i, i2);
        this.mFrameAndBackdropNode = RenderNode.create("FrameAndBackdropNode", null);
        this.mRenderer.addRenderNode(this.mFrameAndBackdropNode, true);
        this.mTargetRect.set(rect);
        this.mFullscreen = z;
        this.mOldFullscreen = z;
        this.mSystemInsets.set(rect2);
        this.mStableInsets.set(rect3);
        this.mOldSystemInsets.set(rect2);
        this.mOldStableInsets.set(rect3);
        this.mResizeMode = i3;
        start();
    }

    void onResourcesLoaded(DecorView decorView, Drawable drawable, Drawable drawable2, Drawable drawable3, int i, int i2) {
        Drawable drawableNewDrawable;
        Drawable drawableNewDrawable2;
        Drawable drawableNewDrawable3;
        this.mDecorView = decorView;
        if (drawable != null && drawable.getConstantState() != null) {
            drawableNewDrawable = drawable.getConstantState().newDrawable();
        } else {
            drawableNewDrawable = null;
        }
        this.mResizingBackgroundDrawable = drawableNewDrawable;
        if (drawable2 != null && drawable2.getConstantState() != null) {
            drawableNewDrawable2 = drawable2.getConstantState().newDrawable();
        } else {
            drawableNewDrawable2 = null;
        }
        this.mCaptionBackgroundDrawable = drawableNewDrawable2;
        if (drawable3 != null && drawable3.getConstantState() != null) {
            drawableNewDrawable3 = drawable3.getConstantState().newDrawable();
        } else {
            drawableNewDrawable3 = null;
        }
        this.mUserCaptionBackgroundDrawable = drawableNewDrawable3;
        if (this.mCaptionBackgroundDrawable == null) {
            this.mCaptionBackgroundDrawable = this.mResizingBackgroundDrawable;
        }
        if (i != 0) {
            this.mStatusBarColor = new ColorDrawable(i);
            addSystemBarNodeIfNeeded();
        } else {
            this.mStatusBarColor = null;
        }
        if (i2 != 0) {
            this.mNavigationBarColor = new ColorDrawable(i2);
            addSystemBarNodeIfNeeded();
        } else {
            this.mNavigationBarColor = null;
        }
    }

    private void addSystemBarNodeIfNeeded() {
        if (this.mSystemBarBackgroundNode != null) {
            return;
        }
        this.mSystemBarBackgroundNode = RenderNode.create("SystemBarBackgroundNode", null);
        this.mRenderer.addRenderNode(this.mSystemBarBackgroundNode, false);
    }

    public void setTargetRect(Rect rect, boolean z, Rect rect2, Rect rect3) {
        synchronized (this) {
            this.mFullscreen = z;
            this.mTargetRect.set(rect);
            this.mSystemInsets.set(rect2);
            this.mStableInsets.set(rect3);
            pingRenderLocked(false);
        }
    }

    public void onConfigurationChange() {
        synchronized (this) {
            if (this.mRenderer != null) {
                this.mOldTargetRect.set(0, 0, 0, 0);
                pingRenderLocked(false);
            }
        }
    }

    public void releaseRenderer() {
        synchronized (this) {
            if (this.mRenderer != null) {
                this.mRenderer.setContentDrawBounds(0, 0, 0, 0);
                this.mRenderer.removeRenderNode(this.mFrameAndBackdropNode);
                if (this.mSystemBarBackgroundNode != null) {
                    this.mRenderer.removeRenderNode(this.mSystemBarBackgroundNode);
                }
                this.mRenderer = null;
                pingRenderLocked(false);
            }
        }
    }

    @Override
    public void run() {
        try {
            Looper.prepare();
            synchronized (this) {
                this.mChoreographer = Choreographer.getInstance();
            }
            Looper.loop();
            synchronized (this) {
                this.mChoreographer = null;
                Choreographer.releaseInstance();
            }
        } finally {
            releaseRenderer();
        }
    }

    @Override
    public void doFrame(long j) {
        synchronized (this) {
            if (this.mRenderer == null) {
                reportDrawIfNeeded();
                Looper.myLooper().quit();
            } else {
                doFrameUncheckedLocked();
            }
        }
    }

    private void doFrameUncheckedLocked() {
        this.mNewTargetRect.set(this.mTargetRect);
        if (!this.mNewTargetRect.equals(this.mOldTargetRect) || this.mOldFullscreen != this.mFullscreen || !this.mStableInsets.equals(this.mOldStableInsets) || !this.mSystemInsets.equals(this.mOldSystemInsets) || this.mReportNextDraw) {
            this.mOldFullscreen = this.mFullscreen;
            this.mOldTargetRect.set(this.mNewTargetRect);
            this.mOldSystemInsets.set(this.mSystemInsets);
            this.mOldStableInsets.set(this.mStableInsets);
            redrawLocked(this.mNewTargetRect, this.mFullscreen, this.mSystemInsets, this.mStableInsets);
        }
    }

    public boolean onContentDrawn(int i, int i2, int i3, int i4) {
        boolean z;
        synchronized (this) {
            z = false;
            boolean z2 = this.mLastContentWidth == 0;
            this.mLastContentWidth = i3;
            this.mLastContentHeight = i4 - this.mLastCaptionHeight;
            this.mLastXOffset = i;
            this.mLastYOffset = i2;
            this.mRenderer.setContentDrawBounds(this.mLastXOffset, this.mLastYOffset, this.mLastXOffset + this.mLastContentWidth, this.mLastYOffset + this.mLastCaptionHeight + this.mLastContentHeight);
            if (z2 && (this.mLastCaptionHeight != 0 || !this.mDecorView.isShowingCaption())) {
                z = true;
            }
        }
        return z;
    }

    public void onRequestDraw(boolean z) {
        synchronized (this) {
            this.mReportNextDraw = z;
            this.mOldTargetRect.set(0, 0, 0, 0);
            pingRenderLocked(true);
        }
    }

    private void redrawLocked(Rect rect, boolean z, Rect rect2, Rect rect3) {
        int captionHeight = this.mDecorView.getCaptionHeight();
        if (captionHeight != 0) {
            this.mLastCaptionHeight = captionHeight;
        }
        if ((this.mLastCaptionHeight == 0 && this.mDecorView.isShowingCaption()) || this.mLastContentWidth == 0 || this.mLastContentHeight == 0) {
            return;
        }
        int i = this.mLastXOffset + rect.left;
        int i2 = this.mLastYOffset + rect.top;
        int iWidth = rect.width();
        int iHeight = rect.height();
        int i3 = i + iWidth;
        int i4 = i2 + iHeight;
        this.mFrameAndBackdropNode.setLeftTopRightBottom(i, i2, i3, i4);
        DisplayListCanvas displayListCanvasStart = this.mFrameAndBackdropNode.start(iWidth, iHeight);
        Drawable drawable = this.mUserCaptionBackgroundDrawable != null ? this.mUserCaptionBackgroundDrawable : this.mCaptionBackgroundDrawable;
        if (drawable != null) {
            drawable.setBounds(0, 0, i3, this.mLastCaptionHeight + i2);
            drawable.draw(displayListCanvasStart);
        }
        if (this.mResizingBackgroundDrawable != null) {
            this.mResizingBackgroundDrawable.setBounds(0, this.mLastCaptionHeight, i3, i4);
            this.mResizingBackgroundDrawable.draw(displayListCanvasStart);
        }
        this.mFrameAndBackdropNode.end(displayListCanvasStart);
        drawColorViews(i, i2, iWidth, iHeight, z, rect2, rect3);
        this.mRenderer.drawRenderNode(this.mFrameAndBackdropNode);
        reportDrawIfNeeded();
    }

    private void drawColorViews(int i, int i2, int i3, int i4, boolean z, Rect rect, Rect rect2) {
        if (this.mSystemBarBackgroundNode == null) {
            return;
        }
        DisplayListCanvas displayListCanvasStart = this.mSystemBarBackgroundNode.start(i3, i4);
        int i5 = i + i3;
        this.mSystemBarBackgroundNode.setLeftTopRightBottom(i, i2, i5, i2 + i4);
        int colorViewTopInset = DecorView.getColorViewTopInset(this.mStableInsets.top, this.mSystemInsets.top);
        if (this.mStatusBarColor != null) {
            this.mStatusBarColor.setBounds(0, 0, i5, colorViewTopInset);
            this.mStatusBarColor.draw(displayListCanvasStart);
        }
        if (this.mNavigationBarColor != null && z) {
            DecorView.getNavigationBarRect(i3, i4, rect2, rect, this.mTmpRect);
            this.mNavigationBarColor.setBounds(this.mTmpRect);
            this.mNavigationBarColor.draw(displayListCanvasStart);
        }
        this.mSystemBarBackgroundNode.end(displayListCanvasStart);
        this.mRenderer.drawRenderNode(this.mSystemBarBackgroundNode);
    }

    private void reportDrawIfNeeded() {
        if (this.mReportNextDraw) {
            if (this.mDecorView.isAttachedToWindow()) {
                this.mDecorView.getViewRootImpl().reportDrawFinish();
            }
            this.mReportNextDraw = false;
        }
    }

    private void pingRenderLocked(boolean z) {
        if (this.mChoreographer != null && !z) {
            this.mChoreographer.postFrameCallback(this);
        } else {
            doFrameUncheckedLocked();
        }
    }

    void setUserCaptionBackgroundDrawable(Drawable drawable) {
        this.mUserCaptionBackgroundDrawable = drawable;
    }
}
