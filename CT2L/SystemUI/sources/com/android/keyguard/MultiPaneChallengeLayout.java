package com.android.keyguard;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import com.android.keyguard.ChallengeLayout;

public class MultiPaneChallengeLayout extends ViewGroup implements ChallengeLayout {
    private ChallengeLayout.OnBouncerStateChangedListener mBouncerListener;
    private KeyguardSecurityContainer mChallengeView;
    private final DisplayMetrics mDisplayMetrics;
    private final Rect mInsets;
    private boolean mIsBouncing;
    final int mOrientation;
    private final View.OnClickListener mScrimClickListener;
    private View mScrimView;
    private final Rect mTempRect;
    private View mUserSwitcherView;
    private final Rect mZeroPadding;

    public MultiPaneChallengeLayout(Context context) {
        this(context, null);
    }

    public MultiPaneChallengeLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MultiPaneChallengeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mTempRect = new Rect();
        this.mZeroPadding = new Rect();
        this.mInsets = new Rect();
        this.mScrimClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MultiPaneChallengeLayout.this.hideBouncer();
            }
        };
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MultiPaneChallengeLayout, defStyleAttr, 0);
        this.mOrientation = a.getInt(R.styleable.MultiPaneChallengeLayout_android_orientation, 0);
        a.recycle();
        Resources res = getResources();
        this.mDisplayMetrics = res.getDisplayMetrics();
        setSystemUiVisibility(768);
    }

    public void setInsets(Rect insets) {
        this.mInsets.set(insets);
    }

    @Override
    public boolean isChallengeShowing() {
        return true;
    }

    @Override
    public boolean isChallengeOverlapping() {
        return false;
    }

    @Override
    public int getBouncerAnimationDuration() {
        return 350;
    }

    @Override
    public void showBouncer() {
        if (!this.mIsBouncing) {
            this.mIsBouncing = true;
            if (this.mScrimView != null) {
                if (this.mChallengeView != null) {
                    this.mChallengeView.showBouncer(350);
                }
                Animator anim = ObjectAnimator.ofFloat(this.mScrimView, "alpha", 1.0f);
                anim.setDuration(350L);
                anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        MultiPaneChallengeLayout.this.mScrimView.setVisibility(0);
                    }
                });
                anim.start();
            }
            if (this.mBouncerListener != null) {
                this.mBouncerListener.onBouncerStateChanged(true);
            }
        }
    }

    public void hideBouncer() {
        if (this.mIsBouncing) {
            this.mIsBouncing = false;
            if (this.mScrimView != null) {
                if (this.mChallengeView != null) {
                    this.mChallengeView.hideBouncer(350);
                }
                Animator anim = ObjectAnimator.ofFloat(this.mScrimView, "alpha", 0.0f);
                anim.setDuration(350L);
                anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        MultiPaneChallengeLayout.this.mScrimView.setVisibility(4);
                    }
                });
                anim.start();
            }
            if (this.mBouncerListener != null) {
                this.mBouncerListener.onBouncerStateChanged(false);
            }
        }
    }

    @Override
    public boolean isBouncing() {
        return this.mIsBouncing;
    }

    @Override
    public void setOnBouncerStateChangedListener(ChallengeLayout.OnBouncerStateChangedListener listener) {
        this.mBouncerListener = listener;
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        if (this.mIsBouncing && child != this.mChallengeView) {
            hideBouncer();
        }
        super.requestChildFocus(child, focused);
    }

    void setScrimView(View scrim) {
        if (this.mScrimView != null) {
            this.mScrimView.setOnClickListener(null);
        }
        this.mScrimView = scrim;
        if (this.mScrimView != null) {
            this.mScrimView.setAlpha(this.mIsBouncing ? 1.0f : 0.0f);
            this.mScrimView.setVisibility(this.mIsBouncing ? 0 : 4);
            this.mScrimView.setFocusable(true);
            this.mScrimView.setOnClickListener(this.mScrimClickListener);
        }
    }

    private int getVirtualHeight(LayoutParams lp, int height, int heightUsed) {
        int virtualHeight = height;
        View root = getRootView();
        if (root != null) {
            virtualHeight = (this.mDisplayMetrics.heightPixels - root.getPaddingTop()) - this.mInsets.top;
        }
        if (lp.childType == 3) {
            return virtualHeight - heightUsed;
        }
        return lp.childType != 7 ? Math.min(virtualHeight - heightUsed, height) : height;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int adjustedWidthSpec;
        int adjustedHeightSpec;
        if (View.MeasureSpec.getMode(widthSpec) != 1073741824 || View.MeasureSpec.getMode(heightSpec) != 1073741824) {
            throw new IllegalArgumentException("MultiPaneChallengeLayout must be measured with an exact size");
        }
        int width = View.MeasureSpec.getSize(widthSpec);
        int height = View.MeasureSpec.getSize(heightSpec);
        setMeasuredDimension(width, height);
        int insetHeight = (height - this.mInsets.top) - this.mInsets.bottom;
        int insetHeightSpec = View.MeasureSpec.makeMeasureSpec(insetHeight, 1073741824);
        int widthUsed = 0;
        int heightUsed = 0;
        this.mChallengeView = null;
        this.mUserSwitcherView = null;
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (lp.childType == 2) {
                if (this.mChallengeView != null) {
                    throw new IllegalStateException("There may only be one child of type challenge");
                }
                if (!(child instanceof KeyguardSecurityContainer)) {
                    throw new IllegalArgumentException("Challenge must be a KeyguardSecurityContainer");
                }
                this.mChallengeView = (KeyguardSecurityContainer) child;
            } else if (lp.childType == 3) {
                if (this.mUserSwitcherView != null) {
                    throw new IllegalStateException("There may only be one child of type userSwitcher");
                }
                this.mUserSwitcherView = child;
                if (child.getVisibility() != 8) {
                    int adjustedWidthSpec2 = widthSpec;
                    int adjustedHeightSpec2 = insetHeightSpec;
                    if (lp.maxWidth >= 0) {
                        adjustedWidthSpec2 = View.MeasureSpec.makeMeasureSpec(Math.min(lp.maxWidth, width), 1073741824);
                    }
                    if (lp.maxHeight >= 0) {
                        adjustedHeightSpec2 = View.MeasureSpec.makeMeasureSpec(Math.min(lp.maxHeight, insetHeight), 1073741824);
                    }
                    measureChildWithMargins(child, adjustedWidthSpec2, 0, adjustedHeightSpec2, 0);
                    if (Gravity.isVertical(lp.gravity)) {
                        heightUsed = (int) (heightUsed + (child.getMeasuredHeight() * 1.5f));
                    } else if (Gravity.isHorizontal(lp.gravity)) {
                        widthUsed = (int) (widthUsed + (child.getMeasuredWidth() * 1.5f));
                    }
                }
            } else if (lp.childType == 4) {
                setScrimView(child);
                child.measure(widthSpec, heightSpec);
            }
        }
        for (int i2 = 0; i2 < count; i2++) {
            View child2 = getChildAt(i2);
            LayoutParams lp2 = (LayoutParams) child2.getLayoutParams();
            if (lp2.childType != 3 && lp2.childType != 4 && child2.getVisibility() != 8) {
                int virtualHeight = getVirtualHeight(lp2, insetHeight, heightUsed);
                if (lp2.centerWithinArea > 0.0f) {
                    if (this.mOrientation == 0) {
                        adjustedWidthSpec = View.MeasureSpec.makeMeasureSpec((int) (((width - widthUsed) * lp2.centerWithinArea) + 0.5f), 1073741824);
                        adjustedHeightSpec = View.MeasureSpec.makeMeasureSpec(virtualHeight, 1073741824);
                    } else {
                        adjustedWidthSpec = View.MeasureSpec.makeMeasureSpec(width - widthUsed, 1073741824);
                        adjustedHeightSpec = View.MeasureSpec.makeMeasureSpec((int) ((virtualHeight * lp2.centerWithinArea) + 0.5f), 1073741824);
                    }
                } else {
                    adjustedWidthSpec = View.MeasureSpec.makeMeasureSpec(width - widthUsed, 1073741824);
                    adjustedHeightSpec = View.MeasureSpec.makeMeasureSpec(virtualHeight, 1073741824);
                }
                if (lp2.maxWidth >= 0) {
                    adjustedWidthSpec = View.MeasureSpec.makeMeasureSpec(Math.min(lp2.maxWidth, View.MeasureSpec.getSize(adjustedWidthSpec)), 1073741824);
                }
                if (lp2.maxHeight >= 0) {
                    adjustedHeightSpec = View.MeasureSpec.makeMeasureSpec(Math.min(lp2.maxHeight, View.MeasureSpec.getSize(adjustedHeightSpec)), 1073741824);
                }
                measureChildWithMargins(child2, adjustedWidthSpec, 0, adjustedHeightSpec, 0);
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        Rect padding = this.mTempRect;
        padding.left = getPaddingLeft();
        padding.top = getPaddingTop();
        padding.right = getPaddingRight();
        padding.bottom = getPaddingBottom();
        int width = r - l;
        int height = b - t;
        int insetHeight = (height - this.mInsets.top) - this.mInsets.bottom;
        if (this.mUserSwitcherView != null && this.mUserSwitcherView.getVisibility() != 8) {
            layoutWithGravity(width, insetHeight, this.mUserSwitcherView, padding, true);
        }
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (child != this.mUserSwitcherView && child.getVisibility() != 8) {
                if (child == this.mScrimView) {
                    child.layout(0, 0, width, height);
                } else if (lp.childType == 7) {
                    layoutWithGravity(width, insetHeight, child, this.mZeroPadding, false);
                } else {
                    layoutWithGravity(width, insetHeight, child, padding, false);
                }
            }
        }
    }

    private void layoutWithGravity(int width, int height, View child, Rect padding, boolean adjustPadding) {
        int adjustedWidth;
        int adjustedHeight;
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        int heightUsed = ((padding.top + padding.bottom) - getPaddingTop()) - getPaddingBottom();
        int height2 = getVirtualHeight(lp, height, heightUsed);
        int gravity = Gravity.getAbsoluteGravity(lp.gravity, getLayoutDirection());
        boolean fixedLayoutSize = lp.centerWithinArea > 0.0f;
        boolean fixedLayoutHorizontal = fixedLayoutSize && this.mOrientation == 0;
        boolean fixedLayoutVertical = fixedLayoutSize && this.mOrientation == 1;
        if (fixedLayoutHorizontal) {
            int paddedWidth = (width - padding.left) - padding.right;
            adjustedWidth = (int) ((paddedWidth * lp.centerWithinArea) + 0.5f);
            adjustedHeight = height2;
        } else if (fixedLayoutVertical) {
            int paddedHeight = (height2 - getPaddingTop()) - getPaddingBottom();
            adjustedWidth = width;
            adjustedHeight = (int) ((paddedHeight * lp.centerWithinArea) + 0.5f);
        } else {
            adjustedWidth = width;
            adjustedHeight = height2;
        }
        boolean isVertical = Gravity.isVertical(gravity);
        boolean isHorizontal = Gravity.isHorizontal(gravity);
        int childWidth = child.getMeasuredWidth();
        int childHeight = child.getMeasuredHeight();
        int left = padding.left;
        int top = padding.top;
        int right = left + childWidth;
        int bottom = top + childHeight;
        switch (gravity & 112) {
            case 16:
                top = padding.top + ((height2 - childHeight) / 2);
                bottom = top + childHeight;
                break;
            case 48:
                top = fixedLayoutVertical ? padding.top + ((adjustedHeight - childHeight) / 2) : padding.top;
                bottom = top + childHeight;
                if (adjustPadding && isVertical) {
                    padding.top = bottom;
                    padding.bottom += childHeight / 2;
                }
                break;
            case 80:
                bottom = fixedLayoutVertical ? (padding.top + height2) - ((adjustedHeight - childHeight) / 2) : padding.top + height2;
                top = bottom - childHeight;
                if (adjustPadding && isVertical) {
                    padding.bottom = height2 - top;
                    padding.top += childHeight / 2;
                }
                break;
        }
        switch (gravity & 7) {
            case 1:
                int paddedWidth2 = (width - padding.left) - padding.right;
                left = (paddedWidth2 - childWidth) / 2;
                right = left + childWidth;
                break;
            case 3:
                left = fixedLayoutHorizontal ? padding.left + ((adjustedWidth - childWidth) / 2) : padding.left;
                right = left + childWidth;
                if (adjustPadding && isHorizontal && !isVertical) {
                    padding.left = right;
                    padding.right += childWidth / 2;
                }
                break;
            case 5:
                right = fixedLayoutHorizontal ? (width - padding.right) - ((adjustedWidth - childWidth) / 2) : width - padding.right;
                left = right - childWidth;
                if (adjustPadding && isHorizontal && !isVertical) {
                    padding.right = width - left;
                    padding.left += childWidth / 2;
                }
                break;
        }
        child.layout(left, top + this.mInsets.top, right, bottom + this.mInsets.top);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs, this);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams ? new LayoutParams((LayoutParams) p) : p instanceof ViewGroup.MarginLayoutParams ? new LayoutParams((ViewGroup.MarginLayoutParams) p) : new LayoutParams(p);
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams();
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        public float centerWithinArea;
        public int childType;
        public int gravity;
        public int maxHeight;
        public int maxWidth;

        public LayoutParams() {
            this(-2, -2);
        }

        LayoutParams(Context c, AttributeSet attrs, MultiPaneChallengeLayout parent) {
            super(c, attrs);
            this.centerWithinArea = 0.0f;
            this.childType = 0;
            this.gravity = 0;
            this.maxWidth = -1;
            this.maxHeight = -1;
            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.MultiPaneChallengeLayout_Layout);
            this.centerWithinArea = a.getFloat(R.styleable.MultiPaneChallengeLayout_Layout_layout_centerWithinArea, 0.0f);
            this.childType = a.getInt(R.styleable.MultiPaneChallengeLayout_Layout_layout_childType, 0);
            this.gravity = a.getInt(R.styleable.MultiPaneChallengeLayout_Layout_layout_gravity, 0);
            this.maxWidth = a.getDimensionPixelSize(R.styleable.MultiPaneChallengeLayout_Layout_layout_maxWidth, -1);
            this.maxHeight = a.getDimensionPixelSize(R.styleable.MultiPaneChallengeLayout_Layout_layout_maxHeight, -1);
            if (this.gravity == 0) {
                if (parent.mOrientation == 0) {
                    switch (this.childType) {
                        case 1:
                            this.gravity = 19;
                            break;
                        case 2:
                            this.gravity = 21;
                            break;
                        case 3:
                            this.gravity = 81;
                            break;
                    }
                } else {
                    switch (this.childType) {
                        case 1:
                            this.gravity = 49;
                            break;
                        case 2:
                            this.gravity = 81;
                            break;
                        case 3:
                            this.gravity = 81;
                            break;
                    }
                }
            }
            a.recycle();
        }

        public LayoutParams(int width, int height) {
            super(width, height);
            this.centerWithinArea = 0.0f;
            this.childType = 0;
            this.gravity = 0;
            this.maxWidth = -1;
            this.maxHeight = -1;
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
            this.centerWithinArea = 0.0f;
            this.childType = 0;
            this.gravity = 0;
            this.maxWidth = -1;
            this.maxHeight = -1;
        }

        public LayoutParams(ViewGroup.MarginLayoutParams source) {
            super(source);
            this.centerWithinArea = 0.0f;
            this.childType = 0;
            this.gravity = 0;
            this.maxWidth = -1;
            this.maxHeight = -1;
        }

        public LayoutParams(LayoutParams source) {
            this((ViewGroup.MarginLayoutParams) source);
            this.centerWithinArea = source.centerWithinArea;
            this.childType = source.childType;
            this.gravity = source.gravity;
            this.maxWidth = source.maxWidth;
            this.maxHeight = source.maxHeight;
        }
    }
}
