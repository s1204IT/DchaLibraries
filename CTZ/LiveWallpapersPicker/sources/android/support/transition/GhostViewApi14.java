package android.support.transition;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.support.v4.view.ViewCompat;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

@SuppressLint({"ViewConstructor"})
class GhostViewApi14 extends View implements GhostViewImpl {
    Matrix mCurrentMatrix;
    private int mDeltaX;
    private int mDeltaY;
    private final Matrix mMatrix;
    private final ViewTreeObserver.OnPreDrawListener mOnPreDrawListener;
    int mReferences;
    ViewGroup mStartParent;
    View mStartView;
    final View mView;

    static GhostViewImpl addGhost(View view, ViewGroup viewGroup) {
        GhostViewApi14 ghostView = getGhostView(view);
        if (ghostView == null) {
            FrameLayout frameLayout = findFrameLayout(viewGroup);
            if (frameLayout == null) {
                return null;
            }
            ghostView = new GhostViewApi14(view);
            frameLayout.addView(ghostView);
        }
        ghostView.mReferences++;
        return ghostView;
    }

    static void removeGhost(View view) {
        GhostViewApi14 ghostView = getGhostView(view);
        if (ghostView != null) {
            ghostView.mReferences--;
            if (ghostView.mReferences <= 0) {
                ?? parent = ghostView.getParent();
                if (parent instanceof ViewGroup) {
                    parent.endViewTransition(ghostView);
                    parent.removeView(ghostView);
                }
            }
        }
    }

    private static FrameLayout findFrameLayout(ViewGroup viewGroup) {
        ?? r2 = viewGroup;
        while (!(r2 instanceof FrameLayout)) {
            ViewParent parent = r2.getParent();
            if (!(parent instanceof ViewGroup)) {
                return null;
            }
            r2 = parent;
        }
        return r2;
    }

    GhostViewApi14(View view) {
        super(view.getContext());
        this.mMatrix = new Matrix();
        this.mOnPreDrawListener = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                GhostViewApi14.this.mCurrentMatrix = GhostViewApi14.this.mView.getMatrix();
                ViewCompat.postInvalidateOnAnimation(GhostViewApi14.this);
                if (GhostViewApi14.this.mStartParent != null && GhostViewApi14.this.mStartView != null) {
                    GhostViewApi14.this.mStartParent.endViewTransition(GhostViewApi14.this.mStartView);
                    ViewCompat.postInvalidateOnAnimation(GhostViewApi14.this.mStartParent);
                    GhostViewApi14.this.mStartParent = null;
                    GhostViewApi14.this.mStartView = null;
                    return true;
                }
                return true;
            }
        };
        this.mView = view;
        setLayerType(2, null);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setGhostView(this.mView, this);
        int[] location = new int[2];
        int[] viewLocation = new int[2];
        getLocationOnScreen(location);
        this.mView.getLocationOnScreen(viewLocation);
        viewLocation[0] = (int) (viewLocation[0] - this.mView.getTranslationX());
        viewLocation[1] = (int) (viewLocation[1] - this.mView.getTranslationY());
        this.mDeltaX = viewLocation[0] - location[0];
        this.mDeltaY = viewLocation[1] - location[1];
        this.mView.getViewTreeObserver().addOnPreDrawListener(this.mOnPreDrawListener);
        this.mView.setVisibility(4);
    }

    @Override
    protected void onDetachedFromWindow() {
        this.mView.getViewTreeObserver().removeOnPreDrawListener(this.mOnPreDrawListener);
        this.mView.setVisibility(0);
        setGhostView(this.mView, null);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        this.mMatrix.set(this.mCurrentMatrix);
        this.mMatrix.postTranslate(this.mDeltaX, this.mDeltaY);
        canvas.setMatrix(this.mMatrix);
        this.mView.draw(canvas);
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        this.mView.setVisibility(visibility == 0 ? 4 : 0);
    }

    @Override
    public void reserveEndViewTransition(ViewGroup viewGroup, View view) {
        this.mStartParent = viewGroup;
        this.mStartView = view;
    }

    private static void setGhostView(View view, GhostViewApi14 ghostView) {
        view.setTag(R.id.ghost_view, ghostView);
    }

    static GhostViewApi14 getGhostView(View view) {
        return (GhostViewApi14) view.getTag(R.id.ghost_view);
    }
}
