package com.android.gallery3d.ui;

import android.graphics.Rect;
import android.os.SystemClock;
import android.view.MotionEvent;
import com.android.gallery3d.anim.CanvasAnimation;
import com.android.gallery3d.anim.StateTransitionAnimation;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.glrenderer.GLCanvas;
import java.util.ArrayList;

public class GLView {
    private static final int FLAG_INVISIBLE = 1;
    private static final int FLAG_LAYOUT_REQUESTED = 4;
    private static final int FLAG_SET_MEASURED_SIZE = 2;
    public static final int INVISIBLE = 1;
    private static final String TAG = "GLView";
    public static final int VISIBLE = 0;
    private CanvasAnimation mAnimation;
    private float[] mBackgroundColor;
    private ArrayList<GLView> mComponents;
    private GLView mMotionTarget;
    protected GLView mParent;
    private GLRoot mRoot;
    private StateTransitionAnimation mTransition;
    protected final Rect mBounds = new Rect();
    protected final Rect mPaddings = new Rect();
    private int mViewFlags = 0;
    protected int mMeasuredWidth = 0;
    protected int mMeasuredHeight = 0;
    private int mLastWidthSpec = -1;
    private int mLastHeightSpec = -1;
    protected int mScrollY = 0;
    protected int mScrollX = 0;
    protected int mScrollHeight = 0;
    protected int mScrollWidth = 0;

    public interface OnClickListener {
        void onClick(GLView gLView);
    }

    public void startAnimation(CanvasAnimation animation) {
        GLRoot root = getGLRoot();
        if (root == null) {
            throw new IllegalStateException();
        }
        this.mAnimation = animation;
        if (this.mAnimation != null) {
            this.mAnimation.start();
            root.registerLaunchedAnimation(this.mAnimation);
        }
        invalidate();
    }

    public void setVisibility(int visibility) {
        if (visibility != getVisibility()) {
            if (visibility == 0) {
                this.mViewFlags &= -2;
            } else {
                this.mViewFlags |= 1;
            }
            onVisibilityChanged(visibility);
            invalidate();
        }
    }

    public int getVisibility() {
        return (this.mViewFlags & 1) == 0 ? 0 : 1;
    }

    public void attachToRoot(GLRoot root) {
        Utils.assertTrue(this.mParent == null && this.mRoot == null);
        onAttachToRoot(root);
    }

    public void detachFromRoot() {
        Utils.assertTrue(this.mParent == null && this.mRoot != null);
        onDetachFromRoot();
    }

    public int getComponentCount() {
        if (this.mComponents == null) {
            return 0;
        }
        return this.mComponents.size();
    }

    public GLView getComponent(int index) {
        if (this.mComponents == null) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        return this.mComponents.get(index);
    }

    public void addComponent(GLView component) {
        if (component.mParent != null) {
            throw new IllegalStateException();
        }
        if (this.mComponents == null) {
            this.mComponents = new ArrayList<>();
        }
        this.mComponents.add(component);
        component.mParent = this;
        if (this.mRoot != null) {
            component.onAttachToRoot(this.mRoot);
        }
    }

    public boolean removeComponent(GLView component) {
        if (this.mComponents == null || !this.mComponents.remove(component)) {
            return false;
        }
        removeOneComponent(component);
        return true;
    }

    public void removeAllComponents() {
        int n = this.mComponents.size();
        for (int i = 0; i < n; i++) {
            removeOneComponent(this.mComponents.get(i));
        }
        this.mComponents.clear();
    }

    private void removeOneComponent(GLView component) {
        if (this.mMotionTarget == component) {
            long now = SystemClock.uptimeMillis();
            MotionEvent cancelEvent = MotionEvent.obtain(now, now, 3, 0.0f, 0.0f, 0);
            dispatchTouchEvent(cancelEvent);
            cancelEvent.recycle();
        }
        component.onDetachFromRoot();
        component.mParent = null;
    }

    public Rect bounds() {
        return this.mBounds;
    }

    public int getWidth() {
        return this.mBounds.right - this.mBounds.left;
    }

    public int getHeight() {
        return this.mBounds.bottom - this.mBounds.top;
    }

    public GLRoot getGLRoot() {
        return this.mRoot;
    }

    public void invalidate() {
        GLRoot root = getGLRoot();
        if (root != null) {
            root.requestRender();
        }
    }

    public void requestLayout() {
        this.mViewFlags |= 4;
        this.mLastHeightSpec = -1;
        this.mLastWidthSpec = -1;
        if (this.mParent != null) {
            this.mParent.requestLayout();
            return;
        }
        GLRoot root = getGLRoot();
        if (root != null) {
            root.requestLayoutContentPane();
        }
    }

    protected void render(GLCanvas canvas) {
        boolean transitionActive = false;
        if (this.mTransition != null && this.mTransition.calculate(AnimationTime.get())) {
            invalidate();
            transitionActive = this.mTransition.isActive();
        }
        renderBackground(canvas);
        canvas.save();
        if (transitionActive) {
            this.mTransition.applyContentTransform(this, canvas);
        }
        int n = getComponentCount();
        for (int i = 0; i < n; i++) {
            renderChild(canvas, getComponent(i));
        }
        canvas.restore();
        if (transitionActive) {
            this.mTransition.applyOverlay(this, canvas);
        }
    }

    public void setIntroAnimation(StateTransitionAnimation intro) {
        this.mTransition = intro;
        if (this.mTransition != null) {
            this.mTransition.start();
        }
    }

    public float[] getBackgroundColor() {
        return this.mBackgroundColor;
    }

    public void setBackgroundColor(float[] color) {
        this.mBackgroundColor = color;
    }

    protected void renderBackground(GLCanvas view) {
        if (this.mBackgroundColor != null) {
            view.clearBuffer(this.mBackgroundColor);
        }
        if (this.mTransition != null && this.mTransition.isActive()) {
            this.mTransition.applyBackground(this, view);
        }
    }

    protected void renderChild(GLCanvas canvas, GLView component) {
        if (component.getVisibility() == 0 || component.mAnimation != null) {
            int xoffset = component.mBounds.left - this.mScrollX;
            int yoffset = component.mBounds.top - this.mScrollY;
            canvas.translate(xoffset, yoffset);
            CanvasAnimation anim = component.mAnimation;
            if (anim != null) {
                canvas.save(anim.getCanvasSaveFlags());
                if (anim.calculate(AnimationTime.get())) {
                    invalidate();
                } else {
                    component.mAnimation = null;
                }
                anim.apply(canvas);
            }
            component.render(canvas);
            if (anim != null) {
                canvas.restore();
            }
            canvas.translate(-xoffset, -yoffset);
        }
    }

    protected boolean onTouch(MotionEvent event) {
        return false;
    }

    protected boolean dispatchTouchEvent(MotionEvent event, int x, int y, GLView component, boolean checkBounds) {
        Rect rect = component.mBounds;
        int left = rect.left;
        int top = rect.top;
        if (!checkBounds || rect.contains(x, y)) {
            event.offsetLocation(-left, -top);
            if (component.dispatchTouchEvent(event)) {
                event.offsetLocation(left, top);
                return true;
            }
            event.offsetLocation(left, top);
        }
        return false;
    }

    protected boolean dispatchTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        int action = event.getAction();
        if (this.mMotionTarget != null) {
            if (action == 0) {
                MotionEvent cancel = MotionEvent.obtain(event);
                cancel.setAction(3);
                dispatchTouchEvent(cancel, x, y, this.mMotionTarget, false);
                this.mMotionTarget = null;
            } else {
                dispatchTouchEvent(event, x, y, this.mMotionTarget, false);
                if (action == 3 || action == 1) {
                    this.mMotionTarget = null;
                }
                return true;
            }
        }
        if (action == 0) {
            for (int i = getComponentCount() - 1; i >= 0; i--) {
                GLView component = getComponent(i);
                if (component.getVisibility() == 0 && dispatchTouchEvent(event, x, y, component, true)) {
                    this.mMotionTarget = component;
                    return true;
                }
            }
        }
        return onTouch(event);
    }

    public Rect getPaddings() {
        return this.mPaddings;
    }

    public void layout(int left, int top, int right, int bottom) {
        boolean sizeChanged = setBounds(left, top, right, bottom);
        this.mViewFlags &= -5;
        onLayout(sizeChanged, left, top, right, bottom);
    }

    private boolean setBounds(int left, int top, int right, int bottom) {
        boolean sizeChanged = (right - left == this.mBounds.right - this.mBounds.left && bottom - top == this.mBounds.bottom - this.mBounds.top) ? false : true;
        this.mBounds.set(left, top, right, bottom);
        return sizeChanged;
    }

    public void measure(int widthSpec, int heightSpec) {
        if (widthSpec != this.mLastWidthSpec || heightSpec != this.mLastHeightSpec || (this.mViewFlags & 4) != 0) {
            this.mLastWidthSpec = widthSpec;
            this.mLastHeightSpec = heightSpec;
            this.mViewFlags &= -3;
            onMeasure(widthSpec, heightSpec);
            if ((this.mViewFlags & 2) == 0) {
                throw new IllegalStateException(getClass().getName() + " should call setMeasuredSize() in onMeasure()");
            }
        }
    }

    protected void onMeasure(int widthSpec, int heightSpec) {
    }

    protected void setMeasuredSize(int width, int height) {
        this.mViewFlags |= 2;
        this.mMeasuredWidth = width;
        this.mMeasuredHeight = height;
    }

    public int getMeasuredWidth() {
        return this.mMeasuredWidth;
    }

    public int getMeasuredHeight() {
        return this.mMeasuredHeight;
    }

    protected void onLayout(boolean changeSize, int left, int top, int right, int bottom) {
    }

    public boolean getBoundsOf(GLView descendant, Rect out) {
        int xoffset = 0;
        int yoffset = 0;
        for (GLView view = descendant; view != this; view = view.mParent) {
            if (view == null) {
                return false;
            }
            Rect bounds = view.mBounds;
            xoffset += bounds.left;
            yoffset += bounds.top;
        }
        out.set(xoffset, yoffset, descendant.getWidth() + xoffset, descendant.getHeight() + yoffset);
        return true;
    }

    protected void onVisibilityChanged(int visibility) {
        int n = getComponentCount();
        for (int i = 0; i < n; i++) {
            GLView child = getComponent(i);
            if (child.getVisibility() == 0) {
                child.onVisibilityChanged(visibility);
            }
        }
    }

    protected void onAttachToRoot(GLRoot root) {
        this.mRoot = root;
        int n = getComponentCount();
        for (int i = 0; i < n; i++) {
            getComponent(i).onAttachToRoot(root);
        }
    }

    protected void onDetachFromRoot() {
        int n = getComponentCount();
        for (int i = 0; i < n; i++) {
            getComponent(i).onDetachFromRoot();
        }
        this.mRoot = null;
    }

    public void lockRendering() {
        if (this.mRoot != null) {
            this.mRoot.lockRenderThread();
        }
    }

    public void unlockRendering() {
        if (this.mRoot != null) {
            this.mRoot.unlockRenderThread();
        }
    }

    void dumpTree(String prefix) {
        Log.d(TAG, prefix + getClass().getSimpleName());
        int n = getComponentCount();
        for (int i = 0; i < n; i++) {
            getComponent(i).dumpTree(prefix + "....");
        }
    }
}
