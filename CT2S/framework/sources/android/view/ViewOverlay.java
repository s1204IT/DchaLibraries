package android.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import java.util.ArrayList;

public class ViewOverlay {
    OverlayViewGroup mOverlayViewGroup;

    ViewOverlay(Context context, View hostView) {
        this.mOverlayViewGroup = new OverlayViewGroup(context, hostView);
    }

    ViewGroup getOverlayView() {
        return this.mOverlayViewGroup;
    }

    public void add(Drawable drawable) {
        this.mOverlayViewGroup.add(drawable);
    }

    public void remove(Drawable drawable) {
        this.mOverlayViewGroup.remove(drawable);
    }

    public void clear() {
        this.mOverlayViewGroup.clear();
    }

    boolean isEmpty() {
        return this.mOverlayViewGroup.isEmpty();
    }

    static class OverlayViewGroup extends ViewGroup {
        ArrayList<Drawable> mDrawables;
        View mHostView;

        OverlayViewGroup(Context context, View hostView) {
            super(context);
            this.mDrawables = null;
            this.mHostView = hostView;
            this.mAttachInfo = this.mHostView.mAttachInfo;
            this.mRight = hostView.getWidth();
            this.mBottom = hostView.getHeight();
            this.mRenderNode.setLeftTopRightBottom(0, 0, this.mRight, this.mBottom);
        }

        public void add(Drawable drawable) {
            if (this.mDrawables == null) {
                this.mDrawables = new ArrayList<>();
            }
            if (!this.mDrawables.contains(drawable)) {
                this.mDrawables.add(drawable);
                invalidate(drawable.getBounds());
                drawable.setCallback(this);
            }
        }

        public void remove(Drawable drawable) {
            if (this.mDrawables != null) {
                this.mDrawables.remove(drawable);
                invalidate(drawable.getBounds());
                drawable.setCallback(null);
            }
        }

        @Override
        protected boolean verifyDrawable(Drawable who) {
            return super.verifyDrawable(who) || (this.mDrawables != null && this.mDrawables.contains(who));
        }

        public void add(View child) {
            if (child.getParent() instanceof ViewGroup) {
                ViewGroup parent = (ViewGroup) child.getParent();
                if (parent != this.mHostView && parent.getParent() != null && parent.mAttachInfo != null) {
                    int[] parentLocation = new int[2];
                    int[] hostViewLocation = new int[2];
                    parent.getLocationOnScreen(parentLocation);
                    this.mHostView.getLocationOnScreen(hostViewLocation);
                    child.offsetLeftAndRight(parentLocation[0] - hostViewLocation[0]);
                    child.offsetTopAndBottom(parentLocation[1] - hostViewLocation[1]);
                }
                parent.removeView(child);
                if (parent.getLayoutTransition() != null) {
                    parent.getLayoutTransition().cancel(3);
                }
                if (child.getParent() != null) {
                    child.mParent = null;
                }
            }
            super.addView(child);
        }

        public void remove(View view) {
            super.removeView(view);
        }

        public void clear() {
            removeAllViews();
            if (this.mDrawables != null) {
                this.mDrawables.clear();
            }
        }

        boolean isEmpty() {
            return getChildCount() == 0 && (this.mDrawables == null || this.mDrawables.size() == 0);
        }

        @Override
        public void invalidateDrawable(Drawable drawable) {
            invalidate(drawable.getBounds());
        }

        @Override
        protected void dispatchDraw(Canvas canvas) {
            super.dispatchDraw(canvas);
            int numDrawables = this.mDrawables == null ? 0 : this.mDrawables.size();
            for (int i = 0; i < numDrawables; i++) {
                this.mDrawables.get(i).draw(canvas);
            }
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
        }

        @Override
        public void invalidate(Rect dirty) {
            super.invalidate(dirty);
            if (this.mHostView != null) {
                this.mHostView.invalidate(dirty);
            }
        }

        @Override
        public void invalidate(int l, int t, int r, int b) {
            super.invalidate(l, t, r, b);
            if (this.mHostView != null) {
                this.mHostView.invalidate(l, t, r, b);
            }
        }

        @Override
        public void invalidate() {
            super.invalidate();
            if (this.mHostView != null) {
                this.mHostView.invalidate();
            }
        }

        @Override
        void invalidate(boolean invalidateCache) {
            super.invalidate(invalidateCache);
            if (this.mHostView != null) {
                this.mHostView.invalidate(invalidateCache);
            }
        }

        @Override
        void invalidateViewProperty(boolean invalidateParent, boolean forceRedraw) {
            super.invalidateViewProperty(invalidateParent, forceRedraw);
            if (this.mHostView != null) {
                this.mHostView.invalidateViewProperty(invalidateParent, forceRedraw);
            }
        }

        @Override
        protected void invalidateParentCaches() {
            super.invalidateParentCaches();
            if (this.mHostView != null) {
                this.mHostView.invalidateParentCaches();
            }
        }

        @Override
        protected void invalidateParentIfNeeded() {
            super.invalidateParentIfNeeded();
            if (this.mHostView != null) {
                this.mHostView.invalidateParentIfNeeded();
            }
        }

        @Override
        public void damageChild(View child, Rect dirty) {
            if (this.mHostView != null) {
                int left = child.mLeft;
                int top = child.mTop;
                if (!child.getMatrix().isIdentity()) {
                    child.transformRect(dirty);
                }
                dirty.offset(left, top);
                this.mHostView.invalidate(dirty);
            }
        }

        @Override
        protected ViewParent damageChildInParent(int left, int top, Rect dirty) {
            if (this.mHostView instanceof ViewGroup) {
                return ((ViewGroup) this.mHostView).damageChildInParent(left, top, dirty);
            }
            return null;
        }

        @Override
        public ViewParent invalidateChildInParent(int[] location, Rect dirty) {
            if (this.mHostView != null) {
                dirty.offset(location[0], location[1]);
                if (this.mHostView instanceof ViewGroup) {
                    location[0] = 0;
                    location[1] = 0;
                    super.invalidateChildInParent(location, dirty);
                    return ((ViewGroup) this.mHostView).invalidateChildInParent(location, dirty);
                }
                invalidate(dirty);
            }
            return null;
        }
    }
}
