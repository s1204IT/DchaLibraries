package com.android.server.wm;

import android.graphics.Rect;
import android.view.SurfaceControl;
import java.util.function.Supplier;

public class Letterbox {
    private static final Rect EMPTY_RECT = new Rect();
    private final Supplier<SurfaceControl.Builder> mFactory;
    private final Rect mOuter = new Rect();
    private final Rect mInner = new Rect();
    private final LetterboxSurface mTop = new LetterboxSurface("top");
    private final LetterboxSurface mLeft = new LetterboxSurface("left");
    private final LetterboxSurface mBottom = new LetterboxSurface("bottom");
    private final LetterboxSurface mRight = new LetterboxSurface("right");

    public Letterbox(Supplier<SurfaceControl.Builder> supplier) {
        this.mFactory = supplier;
    }

    public void layout(Rect rect, Rect rect2) {
        this.mOuter.set(rect);
        this.mInner.set(rect2);
        this.mTop.layout(rect.left, rect.top, rect2.right, rect2.top);
        this.mLeft.layout(rect.left, rect2.top, rect2.left, rect.bottom);
        this.mBottom.layout(rect2.left, rect2.bottom, rect.right, rect.bottom);
        this.mRight.layout(rect2.right, rect.top, rect.right, rect2.bottom);
    }

    public Rect getInsets() {
        return new Rect(this.mLeft.getWidth(), this.mTop.getHeight(), this.mRight.getWidth(), this.mBottom.getHeight());
    }

    public boolean isOverlappingWith(Rect rect) {
        return this.mTop.isOverlappingWith(rect) || this.mLeft.isOverlappingWith(rect) || this.mBottom.isOverlappingWith(rect) || this.mRight.isOverlappingWith(rect);
    }

    public void hide() {
        layout(EMPTY_RECT, EMPTY_RECT);
    }

    public void destroy() {
        this.mOuter.setEmpty();
        this.mInner.setEmpty();
        this.mTop.destroy();
        this.mLeft.destroy();
        this.mBottom.destroy();
        this.mRight.destroy();
    }

    public boolean needsApplySurfaceChanges() {
        return this.mTop.needsApplySurfaceChanges() || this.mLeft.needsApplySurfaceChanges() || this.mBottom.needsApplySurfaceChanges() || this.mRight.needsApplySurfaceChanges();
    }

    public void applySurfaceChanges(SurfaceControl.Transaction transaction) {
        this.mTop.applySurfaceChanges(transaction);
        this.mLeft.applySurfaceChanges(transaction);
        this.mBottom.applySurfaceChanges(transaction);
        this.mRight.applySurfaceChanges(transaction);
    }

    private class LetterboxSurface {
        private SurfaceControl mSurface;
        private final String mType;
        private final Rect mSurfaceFrame = new Rect();
        private final Rect mLayoutFrame = new Rect();

        public LetterboxSurface(String str) {
            this.mType = str;
        }

        public void layout(int i, int i2, int i3, int i4) {
            if (this.mLayoutFrame.left == i && this.mLayoutFrame.top == i2 && this.mLayoutFrame.right == i3 && this.mLayoutFrame.bottom == i4) {
                return;
            }
            this.mLayoutFrame.set(i, i2, i3, i4);
        }

        private void createSurface() {
            this.mSurface = ((SurfaceControl.Builder) Letterbox.this.mFactory.get()).setName("Letterbox - " + this.mType).setFlags(4).setColorLayer(true).build();
            this.mSurface.setLayer(-1);
            this.mSurface.setColor(new float[]{0.0f, 0.0f, 0.0f});
        }

        public void destroy() {
            if (this.mSurface != null) {
                this.mSurface.destroy();
                this.mSurface = null;
            }
        }

        public int getWidth() {
            return Math.max(0, this.mLayoutFrame.width());
        }

        public int getHeight() {
            return Math.max(0, this.mLayoutFrame.height());
        }

        public boolean isOverlappingWith(Rect rect) {
            if (getWidth() <= 0 || getHeight() <= 0) {
                return false;
            }
            return Rect.intersects(rect, this.mLayoutFrame);
        }

        public void applySurfaceChanges(SurfaceControl.Transaction transaction) {
            if (this.mSurfaceFrame.equals(this.mLayoutFrame)) {
                return;
            }
            this.mSurfaceFrame.set(this.mLayoutFrame);
            if (!this.mSurfaceFrame.isEmpty()) {
                if (this.mSurface == null) {
                    createSurface();
                }
                transaction.setPosition(this.mSurface, this.mSurfaceFrame.left, this.mSurfaceFrame.top);
                transaction.setSize(this.mSurface, this.mSurfaceFrame.width(), this.mSurfaceFrame.height());
                transaction.show(this.mSurface);
                return;
            }
            if (this.mSurface != null) {
                transaction.hide(this.mSurface);
            }
        }

        public boolean needsApplySurfaceChanges() {
            return !this.mSurfaceFrame.equals(this.mLayoutFrame);
        }
    }
}
