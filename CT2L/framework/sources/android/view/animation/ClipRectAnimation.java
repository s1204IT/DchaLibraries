package android.view.animation;

import android.graphics.Rect;

public class ClipRectAnimation extends Animation {
    private Rect mFromRect = new Rect();
    private Rect mToRect = new Rect();

    public ClipRectAnimation(Rect fromClip, Rect toClip) {
        if (fromClip == null || toClip == null) {
            throw new RuntimeException("Expected non-null animation clip rects");
        }
        this.mFromRect.set(fromClip);
        this.mToRect.set(toClip);
    }

    @Override
    protected void applyTransformation(float it, Transformation tr) {
        int l = this.mFromRect.left + ((int) ((this.mToRect.left - this.mFromRect.left) * it));
        int t = this.mFromRect.top + ((int) ((this.mToRect.top - this.mFromRect.top) * it));
        int r = this.mFromRect.right + ((int) ((this.mToRect.right - this.mFromRect.right) * it));
        int b = this.mFromRect.bottom + ((int) ((this.mToRect.bottom - this.mFromRect.bottom) * it));
        tr.setClipRect(l, t, r, b);
    }

    @Override
    public boolean willChangeTransformationMatrix() {
        return false;
    }
}
