package android.support.v4.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.AbsSavedState;
import android.support.v4.view.AccessibilityDelegateCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes.dex */
public class DrawerLayout extends ViewGroup {
    static final boolean CAN_HIDE_DESCENDANTS;
    private static final boolean SET_DRAWER_SHADOW_FROM_ELEVATION;
    private final ChildAccessibilityDelegate mChildAccessibilityDelegate;
    private Rect mChildHitRect;
    private Matrix mChildInvertedMatrix;
    private boolean mChildrenCanceledTouch;
    private boolean mDisallowInterceptRequested;
    private boolean mDrawStatusBarBackground;
    private float mDrawerElevation;
    private int mDrawerState;
    private boolean mFirstLayout;
    private boolean mInLayout;
    private float mInitialMotionX;
    private float mInitialMotionY;
    private Object mLastInsets;
    private final ViewDragCallback mLeftCallback;
    private final ViewDragHelper mLeftDragger;
    private List<DrawerListener> mListeners;
    private int mLockModeEnd;
    private int mLockModeLeft;
    private int mLockModeRight;
    private int mLockModeStart;
    private int mMinDrawerMargin;
    private final ArrayList<View> mNonDrawerViews;
    private final ViewDragCallback mRightCallback;
    private final ViewDragHelper mRightDragger;
    private int mScrimColor;
    private float mScrimOpacity;
    private Paint mScrimPaint;
    private Drawable mShadowEnd;
    private Drawable mShadowLeft;
    private Drawable mShadowLeftResolved;
    private Drawable mShadowRight;
    private Drawable mShadowRightResolved;
    private Drawable mShadowStart;
    private Drawable mStatusBarBackground;
    private static final int[] THEME_ATTRS = {16843828};
    static final int[] LAYOUT_ATTRS = {16842931};

    /* loaded from: classes.dex */
    public interface DrawerListener {
        void onDrawerClosed(View view);

        void onDrawerOpened(View view);

        void onDrawerSlide(View view, float f);

        void onDrawerStateChanged(int i);
    }

    static {
        CAN_HIDE_DESCENDANTS = Build.VERSION.SDK_INT >= 19;
        SET_DRAWER_SHADOW_FROM_ELEVATION = Build.VERSION.SDK_INT >= 21;
    }

    public void setDrawerLockMode(int lockMode, int edgeGravity) {
        int absGravity = GravityCompat.getAbsoluteGravity(edgeGravity, ViewCompat.getLayoutDirection(this));
        if (edgeGravity == 3) {
            this.mLockModeLeft = lockMode;
        } else if (edgeGravity == 5) {
            this.mLockModeRight = lockMode;
        } else if (edgeGravity == 8388611) {
            this.mLockModeStart = lockMode;
        } else if (edgeGravity == 8388613) {
            this.mLockModeEnd = lockMode;
        }
        if (lockMode != 0) {
            ViewDragHelper helper = absGravity == 3 ? this.mLeftDragger : this.mRightDragger;
            helper.cancel();
        }
        switch (lockMode) {
            case 1:
                View toClose = findDrawerWithGravity(absGravity);
                if (toClose != null) {
                    closeDrawer(toClose);
                    return;
                }
                return;
            case 2:
                View toOpen = findDrawerWithGravity(absGravity);
                if (toOpen != null) {
                    openDrawer(toOpen);
                    return;
                }
                return;
            default:
                return;
        }
    }

    public int getDrawerLockMode(int edgeGravity) {
        int layoutDirection = ViewCompat.getLayoutDirection(this);
        if (edgeGravity == 3) {
            if (this.mLockModeLeft != 3) {
                return this.mLockModeLeft;
            }
            int leftLockMode = layoutDirection == 0 ? this.mLockModeStart : this.mLockModeEnd;
            if (leftLockMode != 3) {
                return leftLockMode;
            }
            return 0;
        } else if (edgeGravity == 5) {
            if (this.mLockModeRight != 3) {
                return this.mLockModeRight;
            }
            int rightLockMode = layoutDirection == 0 ? this.mLockModeEnd : this.mLockModeStart;
            if (rightLockMode != 3) {
                return rightLockMode;
            }
            return 0;
        } else if (edgeGravity == 8388611) {
            if (this.mLockModeStart != 3) {
                return this.mLockModeStart;
            }
            int startLockMode = layoutDirection == 0 ? this.mLockModeLeft : this.mLockModeRight;
            if (startLockMode != 3) {
                return startLockMode;
            }
            return 0;
        } else if (edgeGravity == 8388613) {
            if (this.mLockModeEnd != 3) {
                return this.mLockModeEnd;
            }
            int endLockMode = layoutDirection == 0 ? this.mLockModeRight : this.mLockModeLeft;
            if (endLockMode != 3) {
                return endLockMode;
            }
            return 0;
        } else {
            return 0;
        }
    }

    public int getDrawerLockMode(View drawerView) {
        if (!isDrawerView(drawerView)) {
            throw new IllegalArgumentException("View " + drawerView + " is not a drawer");
        }
        int drawerGravity = ((LayoutParams) drawerView.getLayoutParams()).gravity;
        return getDrawerLockMode(drawerGravity);
    }

    private boolean isInBoundsOfChild(float x, float y, View child) {
        if (this.mChildHitRect == null) {
            this.mChildHitRect = new Rect();
        }
        child.getHitRect(this.mChildHitRect);
        return this.mChildHitRect.contains((int) x, (int) y);
    }

    private boolean dispatchTransformedGenericPointerEvent(MotionEvent event, View child) {
        Matrix childMatrix = child.getMatrix();
        if (!childMatrix.isIdentity()) {
            MotionEvent transformedEvent = getTransformedMotionEvent(event, child);
            boolean handled = child.dispatchGenericMotionEvent(transformedEvent);
            transformedEvent.recycle();
            return handled;
        }
        float offsetX = getScrollX() - child.getLeft();
        float offsetY = getScrollY() - child.getTop();
        event.offsetLocation(offsetX, offsetY);
        boolean handled2 = child.dispatchGenericMotionEvent(event);
        event.offsetLocation(-offsetX, -offsetY);
        return handled2;
    }

    private MotionEvent getTransformedMotionEvent(MotionEvent event, View child) {
        float offsetX = getScrollX() - child.getLeft();
        float offsetY = getScrollY() - child.getTop();
        MotionEvent transformedEvent = MotionEvent.obtain(event);
        transformedEvent.offsetLocation(offsetX, offsetY);
        Matrix childMatrix = child.getMatrix();
        if (!childMatrix.isIdentity()) {
            if (this.mChildInvertedMatrix == null) {
                this.mChildInvertedMatrix = new Matrix();
            }
            childMatrix.invert(this.mChildInvertedMatrix);
            transformedEvent.transform(this.mChildInvertedMatrix);
        }
        return transformedEvent;
    }

    void updateDrawerState(int forGravity, int activeState, View activeDrawer) {
        int state;
        int leftState = this.mLeftDragger.getViewDragState();
        int rightState = this.mRightDragger.getViewDragState();
        if (leftState == 1 || rightState == 1) {
            state = 1;
        } else if (leftState == 2 || rightState == 2) {
            state = 2;
        } else {
            state = 0;
        }
        if (activeDrawer != null && activeState == 0) {
            LayoutParams lp = (LayoutParams) activeDrawer.getLayoutParams();
            if (lp.onScreen == 0.0f) {
                dispatchOnDrawerClosed(activeDrawer);
            } else if (lp.onScreen == 1.0f) {
                dispatchOnDrawerOpened(activeDrawer);
            }
        }
        if (state != this.mDrawerState) {
            this.mDrawerState = state;
            if (this.mListeners != null) {
                int listenerCount = this.mListeners.size();
                for (int i = listenerCount - 1; i >= 0; i--) {
                    this.mListeners.get(i).onDrawerStateChanged(state);
                }
            }
        }
    }

    void dispatchOnDrawerClosed(View drawerView) {
        View rootView;
        LayoutParams lp = (LayoutParams) drawerView.getLayoutParams();
        if ((lp.openState & 1) == 1) {
            lp.openState = 0;
            if (this.mListeners != null) {
                int listenerCount = this.mListeners.size();
                for (int i = listenerCount - 1; i >= 0; i--) {
                    this.mListeners.get(i).onDrawerClosed(drawerView);
                }
            }
            updateChildrenImportantForAccessibility(drawerView, false);
            if (hasWindowFocus() && (rootView = getRootView()) != null) {
                rootView.sendAccessibilityEvent(32);
            }
        }
    }

    void dispatchOnDrawerOpened(View drawerView) {
        LayoutParams lp = (LayoutParams) drawerView.getLayoutParams();
        if ((lp.openState & 1) == 0) {
            lp.openState = 1;
            if (this.mListeners != null) {
                int listenerCount = this.mListeners.size();
                for (int i = listenerCount - 1; i >= 0; i--) {
                    this.mListeners.get(i).onDrawerOpened(drawerView);
                }
            }
            updateChildrenImportantForAccessibility(drawerView, true);
            if (hasWindowFocus()) {
                sendAccessibilityEvent(32);
            }
        }
    }

    private void updateChildrenImportantForAccessibility(View drawerView, boolean isDrawerOpen) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if ((!isDrawerOpen && !isDrawerView(child)) || (isDrawerOpen && child == drawerView)) {
                ViewCompat.setImportantForAccessibility(child, 1);
            } else {
                ViewCompat.setImportantForAccessibility(child, 4);
            }
        }
    }

    void dispatchOnDrawerSlide(View drawerView, float slideOffset) {
        if (this.mListeners != null) {
            int listenerCount = this.mListeners.size();
            for (int i = listenerCount - 1; i >= 0; i--) {
                this.mListeners.get(i).onDrawerSlide(drawerView, slideOffset);
            }
        }
    }

    void setDrawerViewOffset(View drawerView, float slideOffset) {
        LayoutParams lp = (LayoutParams) drawerView.getLayoutParams();
        if (slideOffset == lp.onScreen) {
            return;
        }
        lp.onScreen = slideOffset;
        dispatchOnDrawerSlide(drawerView, slideOffset);
    }

    float getDrawerViewOffset(View drawerView) {
        return ((LayoutParams) drawerView.getLayoutParams()).onScreen;
    }

    int getDrawerViewAbsoluteGravity(View drawerView) {
        int gravity = ((LayoutParams) drawerView.getLayoutParams()).gravity;
        return GravityCompat.getAbsoluteGravity(gravity, ViewCompat.getLayoutDirection(this));
    }

    boolean checkDrawerViewAbsoluteGravity(View drawerView, int checkFor) {
        int absGravity = getDrawerViewAbsoluteGravity(drawerView);
        return (absGravity & checkFor) == checkFor;
    }

    View findOpenDrawer() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            LayoutParams childLp = (LayoutParams) child.getLayoutParams();
            if ((childLp.openState & 1) == 1) {
                return child;
            }
        }
        return null;
    }

    void moveDrawerToOffset(View drawerView, float slideOffset) {
        int i;
        float oldOffset = getDrawerViewOffset(drawerView);
        int width = drawerView.getWidth();
        int oldPos = (int) (width * oldOffset);
        int newPos = (int) (width * slideOffset);
        int dx = newPos - oldPos;
        if (checkDrawerViewAbsoluteGravity(drawerView, 3)) {
            i = dx;
        } else {
            i = -dx;
        }
        drawerView.offsetLeftAndRight(i);
        setDrawerViewOffset(drawerView, slideOffset);
    }

    View findDrawerWithGravity(int gravity) {
        int absHorizGravity = GravityCompat.getAbsoluteGravity(gravity, ViewCompat.getLayoutDirection(this)) & 7;
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            int childAbsGravity = getDrawerViewAbsoluteGravity(child);
            if ((childAbsGravity & 7) == absHorizGravity) {
                return child;
            }
        }
        return null;
    }

    static String gravityToString(int gravity) {
        if ((gravity & 3) == 3) {
            return "LEFT";
        }
        if ((gravity & 5) == 5) {
            return "RIGHT";
        }
        return Integer.toHexString(gravity);
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mFirstLayout = true;
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mFirstLayout = true;
    }

    /* JADX WARN: Removed duplicated region for block: B:53:0x0122  */
    /* JADX WARN: Removed duplicated region for block: B:55:0x0144  */
    @Override // android.view.View
    @SuppressLint({"WrongConstant"})
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode;
        int heightMode;
        boolean applyInsets;
        boolean z;
        DrawerLayout drawerLayout = this;
        int widthMode2 = View.MeasureSpec.getMode(widthMeasureSpec);
        int heightMode2 = View.MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);
        char c = 0;
        if (widthMode2 != 1073741824 || heightMode2 != 1073741824) {
            if (!isInEditMode()) {
                throw new IllegalArgumentException("DrawerLayout must be measured with MeasureSpec.EXACTLY.");
            }
            if (widthMode2 == Integer.MIN_VALUE) {
                widthMode2 = 1073741824;
            } else if (widthMode2 == 0) {
                widthMode2 = 1073741824;
                widthSize = 300;
            }
            if (heightMode2 == Integer.MIN_VALUE) {
                heightMode2 = 1073741824;
            } else if (heightMode2 == 0) {
                heightMode2 = 1073741824;
                heightSize = 300;
            }
        }
        drawerLayout.setMeasuredDimension(widthSize, heightSize);
        boolean applyInsets2 = drawerLayout.mLastInsets != null && ViewCompat.getFitsSystemWindows(this);
        int layoutDirection = ViewCompat.getLayoutDirection(this);
        int childCount = getChildCount();
        boolean hasDrawerOnRightEdge = false;
        boolean hasDrawerOnRightEdge2 = false;
        int i = 0;
        while (i < childCount) {
            View child = drawerLayout.getChildAt(i);
            if (child.getVisibility() == 8) {
                widthMode = widthMode2;
                heightMode = heightMode2;
                applyInsets = applyInsets2;
            } else {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (applyInsets2) {
                    int cgrav = GravityCompat.getAbsoluteGravity(lp.gravity, layoutDirection);
                    if (!ViewCompat.getFitsSystemWindows(child)) {
                        widthMode = widthMode2;
                        heightMode = heightMode2;
                        applyInsets = applyInsets2;
                        if (Build.VERSION.SDK_INT >= 21) {
                            WindowInsets wi = (WindowInsets) drawerLayout.mLastInsets;
                            if (cgrav == 3) {
                                z = false;
                                wi = wi.replaceSystemWindowInsets(wi.getSystemWindowInsetLeft(), wi.getSystemWindowInsetTop(), 0, wi.getSystemWindowInsetBottom());
                            } else {
                                z = false;
                                if (cgrav == 5) {
                                    wi = wi.replaceSystemWindowInsets(0, wi.getSystemWindowInsetTop(), wi.getSystemWindowInsetRight(), wi.getSystemWindowInsetBottom());
                                }
                            }
                            lp.leftMargin = wi.getSystemWindowInsetLeft();
                            lp.topMargin = wi.getSystemWindowInsetTop();
                            lp.rightMargin = wi.getSystemWindowInsetRight();
                            lp.bottomMargin = wi.getSystemWindowInsetBottom();
                            if (drawerLayout.isContentView(child)) {
                                int contentWidthSpec = View.MeasureSpec.makeMeasureSpec((widthSize - lp.leftMargin) - lp.rightMargin, 1073741824);
                                int contentHeightSpec = View.MeasureSpec.makeMeasureSpec((heightSize - lp.topMargin) - lp.bottomMargin, 1073741824);
                                child.measure(contentWidthSpec, contentHeightSpec);
                            } else if (!drawerLayout.isDrawerView(child)) {
                                throw new IllegalStateException("Child " + child + " at index " + i + " does not have a valid layout_gravity - must be Gravity.LEFT, Gravity.RIGHT or Gravity.NO_GRAVITY");
                            } else {
                                if (SET_DRAWER_SHADOW_FROM_ELEVATION && ViewCompat.getElevation(child) != drawerLayout.mDrawerElevation) {
                                    ViewCompat.setElevation(child, drawerLayout.mDrawerElevation);
                                }
                                int childGravity = drawerLayout.getDrawerViewAbsoluteGravity(child) & 7;
                                boolean isLeftEdgeDrawer = childGravity == 3 ? true : z;
                                if ((isLeftEdgeDrawer && hasDrawerOnRightEdge2) || (!isLeftEdgeDrawer && hasDrawerOnRightEdge)) {
                                    throw new IllegalStateException("Child drawer has absolute gravity " + gravityToString(childGravity) + " but this DrawerLayout already has a drawer view along that edge");
                                }
                                if (isLeftEdgeDrawer) {
                                    hasDrawerOnRightEdge2 = true;
                                } else {
                                    hasDrawerOnRightEdge = true;
                                }
                                int drawerWidthSpec = getChildMeasureSpec(widthMeasureSpec, drawerLayout.mMinDrawerMargin + lp.leftMargin + lp.rightMargin, lp.width);
                                int drawerHeightSpec = getChildMeasureSpec(heightMeasureSpec, lp.topMargin + lp.bottomMargin, lp.height);
                                child.measure(drawerWidthSpec, drawerHeightSpec);
                                i++;
                                widthMode2 = widthMode;
                                heightMode2 = heightMode;
                                applyInsets2 = applyInsets;
                                drawerLayout = this;
                                c = 0;
                            }
                        }
                    } else if (Build.VERSION.SDK_INT >= 21) {
                        WindowInsets wi2 = (WindowInsets) drawerLayout.mLastInsets;
                        if (cgrav == 3) {
                            widthMode = widthMode2;
                            heightMode = heightMode2;
                            applyInsets = applyInsets2;
                            wi2 = wi2.replaceSystemWindowInsets(wi2.getSystemWindowInsetLeft(), wi2.getSystemWindowInsetTop(), 0, wi2.getSystemWindowInsetBottom());
                        } else {
                            widthMode = widthMode2;
                            heightMode = heightMode2;
                            applyInsets = applyInsets2;
                            if (cgrav == 5) {
                                wi2 = wi2.replaceSystemWindowInsets(0, wi2.getSystemWindowInsetTop(), wi2.getSystemWindowInsetRight(), wi2.getSystemWindowInsetBottom());
                            }
                        }
                        child.dispatchApplyWindowInsets(wi2);
                    }
                    z = false;
                    if (drawerLayout.isContentView(child)) {
                    }
                }
                widthMode = widthMode2;
                heightMode = heightMode2;
                applyInsets = applyInsets2;
                z = false;
                if (drawerLayout.isContentView(child)) {
                }
            }
            i++;
            widthMode2 = widthMode;
            heightMode2 = heightMode;
            applyInsets2 = applyInsets;
            drawerLayout = this;
            c = 0;
        }
    }

    private void resolveShadowDrawables() {
        if (SET_DRAWER_SHADOW_FROM_ELEVATION) {
            return;
        }
        this.mShadowLeftResolved = resolveLeftShadow();
        this.mShadowRightResolved = resolveRightShadow();
    }

    private Drawable resolveLeftShadow() {
        int layoutDirection = ViewCompat.getLayoutDirection(this);
        if (layoutDirection == 0) {
            if (this.mShadowStart != null) {
                mirror(this.mShadowStart, layoutDirection);
                return this.mShadowStart;
            }
        } else if (this.mShadowEnd != null) {
            mirror(this.mShadowEnd, layoutDirection);
            return this.mShadowEnd;
        }
        return this.mShadowLeft;
    }

    private Drawable resolveRightShadow() {
        int layoutDirection = ViewCompat.getLayoutDirection(this);
        if (layoutDirection == 0) {
            if (this.mShadowEnd != null) {
                mirror(this.mShadowEnd, layoutDirection);
                return this.mShadowEnd;
            }
        } else if (this.mShadowStart != null) {
            mirror(this.mShadowStart, layoutDirection);
            return this.mShadowStart;
        }
        return this.mShadowRight;
    }

    private boolean mirror(Drawable drawable, int layoutDirection) {
        if (drawable == null || !DrawableCompat.isAutoMirrored(drawable)) {
            return false;
        }
        DrawableCompat.setLayoutDirection(drawable, layoutDirection);
        return true;
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childLeft;
        float newOffset;
        int width;
        int childCount;
        this.mInLayout = true;
        int width2 = r - l;
        int childCount2 = getChildCount();
        int i = 0;
        while (i < childCount2) {
            View child = getChildAt(i);
            if (child.getVisibility() != 8) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (isContentView(child)) {
                    child.layout(lp.leftMargin, lp.topMargin, lp.leftMargin + child.getMeasuredWidth(), lp.topMargin + child.getMeasuredHeight());
                } else {
                    int childWidth = child.getMeasuredWidth();
                    int childHeight = child.getMeasuredHeight();
                    if (checkDrawerViewAbsoluteGravity(child, 3)) {
                        childLeft = (-childWidth) + ((int) (childWidth * lp.onScreen));
                        newOffset = (childWidth + childLeft) / childWidth;
                    } else {
                        float newOffset2 = childWidth;
                        childLeft = width2 - ((int) (newOffset2 * lp.onScreen));
                        newOffset = (width2 - childLeft) / childWidth;
                    }
                    boolean changeOffset = newOffset != lp.onScreen;
                    int vgrav = lp.gravity & 112;
                    if (vgrav == 16) {
                        width = width2;
                        childCount = childCount2;
                        int height = b - t;
                        int childTop = (height - childHeight) / 2;
                        if (childTop < lp.topMargin) {
                            childTop = lp.topMargin;
                        } else if (childTop + childHeight > height - lp.bottomMargin) {
                            childTop = (height - lp.bottomMargin) - childHeight;
                        }
                        child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
                    } else if (vgrav != 80) {
                        width = width2;
                        int width3 = lp.topMargin;
                        child.layout(childLeft, lp.topMargin, childLeft + childWidth, width3 + childHeight);
                        childCount = childCount2;
                    } else {
                        width = width2;
                        int height2 = b - t;
                        childCount = childCount2;
                        int childCount3 = lp.bottomMargin;
                        child.layout(childLeft, (height2 - lp.bottomMargin) - child.getMeasuredHeight(), childLeft + childWidth, height2 - childCount3);
                    }
                    if (changeOffset) {
                        setDrawerViewOffset(child, newOffset);
                    }
                    int newVisibility = lp.onScreen > 0.0f ? 0 : 4;
                    if (child.getVisibility() != newVisibility) {
                        child.setVisibility(newVisibility);
                    }
                    i++;
                    width2 = width;
                    childCount2 = childCount;
                }
            }
            width = width2;
            childCount = childCount2;
            i++;
            width2 = width;
            childCount2 = childCount;
        }
        this.mInLayout = false;
        this.mFirstLayout = false;
    }

    @Override // android.view.View, android.view.ViewParent
    public void requestLayout() {
        if (!this.mInLayout) {
            super.requestLayout();
        }
    }

    @Override // android.view.View
    public void computeScroll() {
        int childCount = getChildCount();
        float scrimOpacity = 0.0f;
        for (int i = 0; i < childCount; i++) {
            float onscreen = ((LayoutParams) getChildAt(i).getLayoutParams()).onScreen;
            scrimOpacity = Math.max(scrimOpacity, onscreen);
        }
        this.mScrimOpacity = scrimOpacity;
        boolean leftDraggerSettling = this.mLeftDragger.continueSettling(true);
        boolean rightDraggerSettling = this.mRightDragger.continueSettling(true);
        if (leftDraggerSettling || rightDraggerSettling) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    private static boolean hasOpaqueBackground(View v) {
        Drawable bg = v.getBackground();
        return bg != null && bg.getOpacity() == -1;
    }

    @Override // android.view.View
    public void onRtlPropertiesChanged(int layoutDirection) {
        resolveShadowDrawables();
    }

    @Override // android.view.View
    public void onDraw(Canvas c) {
        int inset;
        super.onDraw(c);
        if (this.mDrawStatusBarBackground && this.mStatusBarBackground != null) {
            if (Build.VERSION.SDK_INT >= 21) {
                inset = this.mLastInsets != null ? ((WindowInsets) this.mLastInsets).getSystemWindowInsetTop() : 0;
            } else {
                inset = 0;
            }
            if (inset > 0) {
                this.mStatusBarBackground.setBounds(0, 0, getWidth(), inset);
                this.mStatusBarBackground.draw(c);
            }
        }
    }

    @Override // android.view.ViewGroup
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        int clipLeft;
        int clipRight;
        int height = getHeight();
        boolean drawingContent = isContentView(child);
        int clipRight2 = getWidth();
        int restoreCount = canvas.save();
        if (!drawingContent) {
            clipLeft = 0;
            clipRight = clipRight2;
        } else {
            int childCount = getChildCount();
            int clipRight3 = clipRight2;
            int clipLeft2 = 0;
            for (int clipLeft3 = 0; clipLeft3 < childCount; clipLeft3++) {
                View v = getChildAt(clipLeft3);
                if (v != child && v.getVisibility() == 0 && hasOpaqueBackground(v) && isDrawerView(v) && v.getHeight() >= height) {
                    if (checkDrawerViewAbsoluteGravity(v, 3)) {
                        int vright = v.getRight();
                        if (vright > clipLeft2) {
                            clipLeft2 = vright;
                        }
                    } else {
                        int vleft = v.getLeft();
                        if (vleft < clipRight3) {
                            clipRight3 = vleft;
                        }
                    }
                }
            }
            int i = getHeight();
            canvas.clipRect(clipLeft2, 0, clipRight3, i);
            clipLeft = clipLeft2;
            clipRight = clipRight3;
        }
        boolean result = super.drawChild(canvas, child, drawingTime);
        canvas.restoreToCount(restoreCount);
        if (this.mScrimOpacity > 0.0f && drawingContent) {
            int baseAlpha = (this.mScrimColor & (-16777216)) >>> 24;
            int imag = (int) (baseAlpha * this.mScrimOpacity);
            int color = (imag << 24) | (this.mScrimColor & 16777215);
            this.mScrimPaint.setColor(color);
            canvas.drawRect(clipLeft, 0.0f, clipRight, getHeight(), this.mScrimPaint);
        } else if (this.mShadowLeftResolved != null && checkDrawerViewAbsoluteGravity(child, 3)) {
            int shadowWidth = this.mShadowLeftResolved.getIntrinsicWidth();
            int childRight = child.getRight();
            int drawerPeekDistance = this.mLeftDragger.getEdgeSize();
            float alpha = Math.max(0.0f, Math.min(childRight / drawerPeekDistance, 1.0f));
            Drawable drawable = this.mShadowLeftResolved;
            int top = child.getTop();
            int i2 = childRight + shadowWidth;
            int shadowWidth2 = child.getBottom();
            drawable.setBounds(childRight, top, i2, shadowWidth2);
            this.mShadowLeftResolved.setAlpha((int) (255.0f * alpha));
            this.mShadowLeftResolved.draw(canvas);
        } else if (this.mShadowRightResolved != null && checkDrawerViewAbsoluteGravity(child, 5)) {
            int shadowWidth3 = this.mShadowRightResolved.getIntrinsicWidth();
            int childLeft = child.getLeft();
            int showing = getWidth() - childLeft;
            int drawerPeekDistance2 = this.mRightDragger.getEdgeSize();
            float alpha2 = Math.max(0.0f, Math.min(showing / drawerPeekDistance2, 1.0f));
            Drawable drawable2 = this.mShadowRightResolved;
            int i3 = childLeft - shadowWidth3;
            int shadowWidth4 = child.getTop();
            int showing2 = child.getBottom();
            drawable2.setBounds(i3, shadowWidth4, childLeft, showing2);
            this.mShadowRightResolved.setAlpha((int) (255.0f * alpha2));
            this.mShadowRightResolved.draw(canvas);
        }
        return result;
    }

    boolean isContentView(View child) {
        return ((LayoutParams) child.getLayoutParams()).gravity == 0;
    }

    boolean isDrawerView(View child) {
        int gravity = ((LayoutParams) child.getLayoutParams()).gravity;
        int absGravity = GravityCompat.getAbsoluteGravity(gravity, ViewCompat.getLayoutDirection(child));
        return ((absGravity & 3) == 0 && (absGravity & 5) == 0) ? false : true;
    }

    @Override // android.view.ViewGroup
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        View child;
        int action = ev.getActionMasked();
        boolean interceptForDrag = this.mLeftDragger.shouldInterceptTouchEvent(ev) | this.mRightDragger.shouldInterceptTouchEvent(ev);
        boolean interceptForTap = false;
        switch (action) {
            case 0:
                float x = ev.getX();
                float y = ev.getY();
                this.mInitialMotionX = x;
                this.mInitialMotionY = y;
                if (this.mScrimOpacity > 0.0f && (child = this.mLeftDragger.findTopChildUnder((int) x, (int) y)) != null && isContentView(child)) {
                    interceptForTap = true;
                }
                this.mDisallowInterceptRequested = false;
                this.mChildrenCanceledTouch = false;
                break;
            case 1:
            case 3:
                closeDrawers(true);
                this.mDisallowInterceptRequested = false;
                this.mChildrenCanceledTouch = false;
                break;
            case 2:
                if (this.mLeftDragger.checkTouchSlop(3)) {
                    this.mLeftCallback.removeCallbacks();
                    this.mRightCallback.removeCallbacks();
                    break;
                }
                break;
        }
        return interceptForDrag || interceptForTap || hasPeekingDrawer() || this.mChildrenCanceledTouch;
    }

    @Override // android.view.View
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        if ((event.getSource() & 2) == 0 || event.getAction() == 10 || this.mScrimOpacity <= 0.0f) {
            return super.dispatchGenericMotionEvent(event);
        }
        int childrenCount = getChildCount();
        if (childrenCount != 0) {
            float x = event.getX();
            float y = event.getY();
            for (int i = childrenCount - 1; i >= 0; i--) {
                View child = getChildAt(i);
                if (isInBoundsOfChild(x, y, child) && !isContentView(child) && dispatchTransformedGenericPointerEvent(event, child)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent ev) {
        View openDrawer;
        this.mLeftDragger.processTouchEvent(ev);
        this.mRightDragger.processTouchEvent(ev);
        int action = ev.getAction();
        int i = action & 255;
        if (i != 3) {
            switch (i) {
                case 0:
                    float x = ev.getX();
                    float y = ev.getY();
                    this.mInitialMotionX = x;
                    this.mInitialMotionY = y;
                    this.mDisallowInterceptRequested = false;
                    this.mChildrenCanceledTouch = false;
                    break;
                case 1:
                    float x2 = ev.getX();
                    float y2 = ev.getY();
                    boolean peekingOnly = true;
                    View touchedView = this.mLeftDragger.findTopChildUnder((int) x2, (int) y2);
                    if (touchedView != null && isContentView(touchedView)) {
                        float dx = x2 - this.mInitialMotionX;
                        float dy = y2 - this.mInitialMotionY;
                        int slop = this.mLeftDragger.getTouchSlop();
                        if ((dx * dx) + (dy * dy) < slop * slop && (openDrawer = findOpenDrawer()) != null) {
                            peekingOnly = getDrawerLockMode(openDrawer) == 2;
                        }
                    }
                    closeDrawers(peekingOnly);
                    this.mDisallowInterceptRequested = false;
                    break;
            }
        } else {
            closeDrawers(true);
            this.mDisallowInterceptRequested = false;
            this.mChildrenCanceledTouch = false;
        }
        return true;
    }

    @Override // android.view.ViewGroup, android.view.ViewParent
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
        this.mDisallowInterceptRequested = disallowIntercept;
        if (disallowIntercept) {
            closeDrawers(true);
        }
    }

    public void closeDrawers() {
        closeDrawers(false);
    }

    void closeDrawers(boolean peekingOnly) {
        int childCount = getChildCount();
        boolean needsInvalidate = false;
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (isDrawerView(child) && (!peekingOnly || lp.isPeeking)) {
                int childWidth = child.getWidth();
                if (checkDrawerViewAbsoluteGravity(child, 3)) {
                    needsInvalidate |= this.mLeftDragger.smoothSlideViewTo(child, -childWidth, child.getTop());
                } else {
                    needsInvalidate |= this.mRightDragger.smoothSlideViewTo(child, getWidth(), child.getTop());
                }
                lp.isPeeking = false;
            }
        }
        this.mLeftCallback.removeCallbacks();
        this.mRightCallback.removeCallbacks();
        if (needsInvalidate) {
            invalidate();
        }
    }

    public void openDrawer(View drawerView) {
        openDrawer(drawerView, true);
    }

    public void openDrawer(View drawerView, boolean animate) {
        if (!isDrawerView(drawerView)) {
            throw new IllegalArgumentException("View " + drawerView + " is not a sliding drawer");
        }
        LayoutParams lp = (LayoutParams) drawerView.getLayoutParams();
        if (this.mFirstLayout) {
            lp.onScreen = 1.0f;
            lp.openState = 1;
            updateChildrenImportantForAccessibility(drawerView, true);
        } else if (animate) {
            lp.openState |= 2;
            if (checkDrawerViewAbsoluteGravity(drawerView, 3)) {
                this.mLeftDragger.smoothSlideViewTo(drawerView, 0, drawerView.getTop());
            } else {
                this.mRightDragger.smoothSlideViewTo(drawerView, getWidth() - drawerView.getWidth(), drawerView.getTop());
            }
        } else {
            moveDrawerToOffset(drawerView, 1.0f);
            updateDrawerState(lp.gravity, 0, drawerView);
            drawerView.setVisibility(0);
        }
        invalidate();
    }

    public void closeDrawer(View drawerView) {
        closeDrawer(drawerView, true);
    }

    public void closeDrawer(View drawerView, boolean animate) {
        if (!isDrawerView(drawerView)) {
            throw new IllegalArgumentException("View " + drawerView + " is not a sliding drawer");
        }
        LayoutParams lp = (LayoutParams) drawerView.getLayoutParams();
        if (this.mFirstLayout) {
            lp.onScreen = 0.0f;
            lp.openState = 0;
        } else if (animate) {
            lp.openState = 4 | lp.openState;
            if (checkDrawerViewAbsoluteGravity(drawerView, 3)) {
                this.mLeftDragger.smoothSlideViewTo(drawerView, -drawerView.getWidth(), drawerView.getTop());
            } else {
                this.mRightDragger.smoothSlideViewTo(drawerView, getWidth(), drawerView.getTop());
            }
        } else {
            moveDrawerToOffset(drawerView, 0.0f);
            updateDrawerState(lp.gravity, 0, drawerView);
            drawerView.setVisibility(4);
        }
        invalidate();
    }

    public boolean isDrawerOpen(View drawer) {
        if (!isDrawerView(drawer)) {
            throw new IllegalArgumentException("View " + drawer + " is not a drawer");
        }
        LayoutParams drawerLp = (LayoutParams) drawer.getLayoutParams();
        return (drawerLp.openState & 1) == 1;
    }

    public boolean isDrawerVisible(View drawer) {
        if (isDrawerView(drawer)) {
            return ((LayoutParams) drawer.getLayoutParams()).onScreen > 0.0f;
        }
        throw new IllegalArgumentException("View " + drawer + " is not a drawer");
    }

    private boolean hasPeekingDrawer() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            LayoutParams lp = (LayoutParams) getChildAt(i).getLayoutParams();
            if (lp.isPeeking) {
                return true;
            }
        }
        return false;
    }

    @Override // android.view.ViewGroup
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(-1, -1);
    }

    @Override // android.view.ViewGroup
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams ? new LayoutParams((LayoutParams) p) : p instanceof ViewGroup.MarginLayoutParams ? new LayoutParams((ViewGroup.MarginLayoutParams) p) : new LayoutParams(p);
    }

    @Override // android.view.ViewGroup
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return (p instanceof LayoutParams) && super.checkLayoutParams(p);
    }

    @Override // android.view.ViewGroup
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override // android.view.ViewGroup, android.view.View
    public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
        if (getDescendantFocusability() == 393216) {
            return;
        }
        int childCount = getChildCount();
        boolean isDrawerOpen = false;
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (isDrawerView(child)) {
                if (isDrawerOpen(child)) {
                    isDrawerOpen = true;
                    child.addFocusables(views, direction, focusableMode);
                }
            } else {
                this.mNonDrawerViews.add(child);
            }
        }
        if (!isDrawerOpen) {
            int nonDrawerViewsCount = this.mNonDrawerViews.size();
            for (int i2 = 0; i2 < nonDrawerViewsCount; i2++) {
                View child2 = this.mNonDrawerViews.get(i2);
                if (child2.getVisibility() == 0) {
                    child2.addFocusables(views, direction, focusableMode);
                }
            }
        }
        this.mNonDrawerViews.clear();
    }

    private boolean hasVisibleDrawer() {
        return findVisibleDrawer() != null;
    }

    View findVisibleDrawer() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (isDrawerView(child) && isDrawerVisible(child)) {
                return child;
            }
        }
        return null;
    }

    @Override // android.view.View, android.view.KeyEvent.Callback
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == 4 && hasVisibleDrawer()) {
            event.startTracking();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override // android.view.View, android.view.KeyEvent.Callback
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == 4) {
            View visibleDrawer = findVisibleDrawer();
            if (visibleDrawer != null && getDrawerLockMode(visibleDrawer) == 0) {
                closeDrawers();
            }
            return visibleDrawer != null;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override // android.view.View
    protected void onRestoreInstanceState(Parcelable state) {
        View toOpen;
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        if (ss.openDrawerGravity != 0 && (toOpen = findDrawerWithGravity(ss.openDrawerGravity)) != null) {
            openDrawer(toOpen);
        }
        if (ss.lockModeLeft != 3) {
            setDrawerLockMode(ss.lockModeLeft, 3);
        }
        if (ss.lockModeRight != 3) {
            setDrawerLockMode(ss.lockModeRight, 5);
        }
        if (ss.lockModeStart != 3) {
            setDrawerLockMode(ss.lockModeStart, 8388611);
        }
        if (ss.lockModeEnd != 3) {
            setDrawerLockMode(ss.lockModeEnd, 8388613);
        }
    }

    @Override // android.view.View
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            boolean isOpenedAndNotClosing = lp.openState == 1;
            boolean isClosedAndOpening = lp.openState == 2;
            if (isOpenedAndNotClosing || isClosedAndOpening) {
                ss.openDrawerGravity = lp.gravity;
                break;
            }
        }
        ss.lockModeLeft = this.mLockModeLeft;
        ss.lockModeRight = this.mLockModeRight;
        ss.lockModeStart = this.mLockModeStart;
        ss.lockModeEnd = this.mLockModeEnd;
        return ss;
    }

    @Override // android.view.ViewGroup
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        View openDrawer = findOpenDrawer();
        if (openDrawer != null || isDrawerView(child)) {
            ViewCompat.setImportantForAccessibility(child, 4);
        } else {
            ViewCompat.setImportantForAccessibility(child, 1);
        }
        if (!CAN_HIDE_DESCENDANTS) {
            ViewCompat.setAccessibilityDelegate(child, this.mChildAccessibilityDelegate);
        }
    }

    static boolean includeChildForAccessibility(View child) {
        return (ViewCompat.getImportantForAccessibility(child) == 4 || ViewCompat.getImportantForAccessibility(child) == 2) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes.dex */
    public static class SavedState extends AbsSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.ClassLoaderCreator<SavedState>() { // from class: android.support.v4.widget.DrawerLayout.SavedState.1
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.ClassLoaderCreator
            public SavedState createFromParcel(Parcel in, ClassLoader loader) {
                return new SavedState(in, loader);
            }

            @Override // android.os.Parcelable.Creator
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in, null);
            }

            @Override // android.os.Parcelable.Creator
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        int lockModeEnd;
        int lockModeLeft;
        int lockModeRight;
        int lockModeStart;
        int openDrawerGravity;

        public SavedState(Parcel in, ClassLoader loader) {
            super(in, loader);
            this.openDrawerGravity = 0;
            this.openDrawerGravity = in.readInt();
            this.lockModeLeft = in.readInt();
            this.lockModeRight = in.readInt();
            this.lockModeStart = in.readInt();
            this.lockModeEnd = in.readInt();
        }

        public SavedState(Parcelable superState) {
            super(superState);
            this.openDrawerGravity = 0;
        }

        @Override // android.support.v4.view.AbsSavedState, android.os.Parcelable
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(this.openDrawerGravity);
            dest.writeInt(this.lockModeLeft);
            dest.writeInt(this.lockModeRight);
            dest.writeInt(this.lockModeStart);
            dest.writeInt(this.lockModeEnd);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class ViewDragCallback extends ViewDragHelper.Callback {
        private final int mAbsGravity;
        private ViewDragHelper mDragger;
        private final Runnable mPeekRunnable;
        final /* synthetic */ DrawerLayout this$0;

        public void removeCallbacks() {
            this.this$0.removeCallbacks(this.mPeekRunnable);
        }

        @Override // android.support.v4.widget.ViewDragHelper.Callback
        public boolean tryCaptureView(View child, int pointerId) {
            return this.this$0.isDrawerView(child) && this.this$0.checkDrawerViewAbsoluteGravity(child, this.mAbsGravity) && this.this$0.getDrawerLockMode(child) == 0;
        }

        @Override // android.support.v4.widget.ViewDragHelper.Callback
        public void onViewDragStateChanged(int state) {
            this.this$0.updateDrawerState(this.mAbsGravity, state, this.mDragger.getCapturedView());
        }

        @Override // android.support.v4.widget.ViewDragHelper.Callback
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            float offset;
            int childWidth = changedView.getWidth();
            if (this.this$0.checkDrawerViewAbsoluteGravity(changedView, 3)) {
                offset = (childWidth + left) / childWidth;
            } else {
                int width = this.this$0.getWidth();
                offset = (width - left) / childWidth;
            }
            this.this$0.setDrawerViewOffset(changedView, offset);
            changedView.setVisibility(offset == 0.0f ? 4 : 0);
            this.this$0.invalidate();
        }

        @Override // android.support.v4.widget.ViewDragHelper.Callback
        public void onViewCaptured(View capturedChild, int activePointerId) {
            LayoutParams lp = (LayoutParams) capturedChild.getLayoutParams();
            lp.isPeeking = false;
            closeOtherDrawer();
        }

        private void closeOtherDrawer() {
            int otherGrav = this.mAbsGravity == 3 ? 5 : 3;
            View toClose = this.this$0.findDrawerWithGravity(otherGrav);
            if (toClose != null) {
                this.this$0.closeDrawer(toClose);
            }
        }

        @Override // android.support.v4.widget.ViewDragHelper.Callback
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            int left;
            float offset = this.this$0.getDrawerViewOffset(releasedChild);
            int childWidth = releasedChild.getWidth();
            if (this.this$0.checkDrawerViewAbsoluteGravity(releasedChild, 3)) {
                left = (xvel > 0.0f || (xvel == 0.0f && offset > 0.5f)) ? 0 : -childWidth;
            } else {
                int width = this.this$0.getWidth();
                left = (xvel < 0.0f || (xvel == 0.0f && offset > 0.5f)) ? width - childWidth : width;
            }
            this.mDragger.settleCapturedViewAt(left, releasedChild.getTop());
            this.this$0.invalidate();
        }

        @Override // android.support.v4.widget.ViewDragHelper.Callback
        public void onEdgeTouched(int edgeFlags, int pointerId) {
            this.this$0.postDelayed(this.mPeekRunnable, 160L);
        }

        @Override // android.support.v4.widget.ViewDragHelper.Callback
        public boolean onEdgeLock(int edgeFlags) {
            return false;
        }

        @Override // android.support.v4.widget.ViewDragHelper.Callback
        public void onEdgeDragStarted(int edgeFlags, int pointerId) {
            View toCapture;
            if ((edgeFlags & 1) == 1) {
                toCapture = this.this$0.findDrawerWithGravity(3);
            } else {
                toCapture = this.this$0.findDrawerWithGravity(5);
            }
            if (toCapture != null && this.this$0.getDrawerLockMode(toCapture) == 0) {
                this.mDragger.captureChildView(toCapture, pointerId);
            }
        }

        @Override // android.support.v4.widget.ViewDragHelper.Callback
        public int getViewHorizontalDragRange(View child) {
            if (this.this$0.isDrawerView(child)) {
                return child.getWidth();
            }
            return 0;
        }

        @Override // android.support.v4.widget.ViewDragHelper.Callback
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            if (this.this$0.checkDrawerViewAbsoluteGravity(child, 3)) {
                return Math.max(-child.getWidth(), Math.min(left, 0));
            }
            int width = this.this$0.getWidth();
            return Math.max(width - child.getWidth(), Math.min(left, width));
        }

        @Override // android.support.v4.widget.ViewDragHelper.Callback
        public int clampViewPositionVertical(View child, int top, int dy) {
            return child.getTop();
        }
    }

    /* loaded from: classes.dex */
    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        public int gravity;
        boolean isPeeking;
        float onScreen;
        int openState;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            this.gravity = 0;
            TypedArray a = c.obtainStyledAttributes(attrs, DrawerLayout.LAYOUT_ATTRS);
            this.gravity = a.getInt(0, 0);
            a.recycle();
        }

        public LayoutParams(int width, int height) {
            super(width, height);
            this.gravity = 0;
        }

        public LayoutParams(LayoutParams source) {
            super((ViewGroup.MarginLayoutParams) source);
            this.gravity = 0;
            this.gravity = source.gravity;
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
            this.gravity = 0;
        }

        public LayoutParams(ViewGroup.MarginLayoutParams source) {
            super(source);
            this.gravity = 0;
        }
    }

    /* loaded from: classes.dex */
    static final class ChildAccessibilityDelegate extends AccessibilityDelegateCompat {
        @Override // android.support.v4.view.AccessibilityDelegateCompat
        public void onInitializeAccessibilityNodeInfo(View child, AccessibilityNodeInfoCompat info) {
            super.onInitializeAccessibilityNodeInfo(child, info);
            if (!DrawerLayout.includeChildForAccessibility(child)) {
                info.setParent(null);
            }
        }
    }
}
