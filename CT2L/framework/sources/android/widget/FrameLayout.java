package android.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.RemotableViewMethod;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.RemoteViews;
import com.android.internal.R;
import java.util.ArrayList;

@RemoteViews.RemoteView
public class FrameLayout extends ViewGroup {
    private static final int DEFAULT_CHILD_GRAVITY = 8388659;

    @ViewDebug.ExportedProperty(category = "drawing")
    private Drawable mForeground;
    boolean mForegroundBoundsChanged;

    @ViewDebug.ExportedProperty(category = "drawing")
    private int mForegroundGravity;

    @ViewDebug.ExportedProperty(category = "drawing")
    protected boolean mForegroundInPadding;

    @ViewDebug.ExportedProperty(category = "padding")
    private int mForegroundPaddingBottom;

    @ViewDebug.ExportedProperty(category = "padding")
    private int mForegroundPaddingLeft;

    @ViewDebug.ExportedProperty(category = "padding")
    private int mForegroundPaddingRight;

    @ViewDebug.ExportedProperty(category = "padding")
    private int mForegroundPaddingTop;
    private ColorStateList mForegroundTintList;
    private PorterDuff.Mode mForegroundTintMode;
    private boolean mHasForegroundTint;
    private boolean mHasForegroundTintMode;
    private final ArrayList<View> mMatchParentChildren;

    @ViewDebug.ExportedProperty(category = "measurement")
    boolean mMeasureAllChildren;
    private final Rect mOverlayBounds;
    private final Rect mSelfBounds;

    public FrameLayout(Context context) {
        super(context);
        this.mMeasureAllChildren = false;
        this.mForegroundTintList = null;
        this.mForegroundTintMode = null;
        this.mHasForegroundTint = false;
        this.mHasForegroundTintMode = false;
        this.mForegroundPaddingLeft = 0;
        this.mForegroundPaddingTop = 0;
        this.mForegroundPaddingRight = 0;
        this.mForegroundPaddingBottom = 0;
        this.mSelfBounds = new Rect();
        this.mOverlayBounds = new Rect();
        this.mForegroundGravity = 119;
        this.mForegroundInPadding = true;
        this.mForegroundBoundsChanged = false;
        this.mMatchParentChildren = new ArrayList<>(1);
    }

    public FrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public FrameLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mMeasureAllChildren = false;
        this.mForegroundTintList = null;
        this.mForegroundTintMode = null;
        this.mHasForegroundTint = false;
        this.mHasForegroundTintMode = false;
        this.mForegroundPaddingLeft = 0;
        this.mForegroundPaddingTop = 0;
        this.mForegroundPaddingRight = 0;
        this.mForegroundPaddingBottom = 0;
        this.mSelfBounds = new Rect();
        this.mOverlayBounds = new Rect();
        this.mForegroundGravity = 119;
        this.mForegroundInPadding = true;
        this.mForegroundBoundsChanged = false;
        this.mMatchParentChildren = new ArrayList<>(1);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FrameLayout, defStyleAttr, defStyleRes);
        this.mForegroundGravity = a.getInt(2, this.mForegroundGravity);
        Drawable d = a.getDrawable(0);
        if (d != null) {
            setForeground(d);
        }
        if (a.getBoolean(1, false)) {
            setMeasureAllChildren(true);
        }
        if (a.hasValue(4)) {
            this.mForegroundTintMode = Drawable.parseTintMode(a.getInt(4, -1), this.mForegroundTintMode);
            this.mHasForegroundTintMode = true;
        }
        if (a.hasValue(3)) {
            this.mForegroundTintList = a.getColorStateList(3);
            this.mHasForegroundTint = true;
        }
        this.mForegroundInPadding = a.getBoolean(5, true);
        a.recycle();
        applyForegroundTint();
    }

    public int getForegroundGravity() {
        return this.mForegroundGravity;
    }

    @RemotableViewMethod
    public void setForegroundGravity(int foregroundGravity) {
        if (this.mForegroundGravity != foregroundGravity) {
            if ((8388615 & foregroundGravity) == 0) {
                foregroundGravity |= Gravity.START;
            }
            if ((foregroundGravity & 112) == 0) {
                foregroundGravity |= 48;
            }
            this.mForegroundGravity = foregroundGravity;
            if (this.mForegroundGravity == 119 && this.mForeground != null) {
                Rect padding = new Rect();
                if (this.mForeground.getPadding(padding)) {
                    this.mForegroundPaddingLeft = padding.left;
                    this.mForegroundPaddingTop = padding.top;
                    this.mForegroundPaddingRight = padding.right;
                    this.mForegroundPaddingBottom = padding.bottom;
                }
            } else {
                this.mForegroundPaddingLeft = 0;
                this.mForegroundPaddingTop = 0;
                this.mForegroundPaddingRight = 0;
                this.mForegroundPaddingBottom = 0;
            }
            requestLayout();
        }
    }

    @Override
    @RemotableViewMethod
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (this.mForeground != null) {
            this.mForeground.setVisible(visibility == 0, false);
        }
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who) || who == this.mForeground;
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        if (this.mForeground != null) {
            this.mForeground.jumpToCurrentState();
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (this.mForeground != null && this.mForeground.isStateful()) {
            this.mForeground.setState(getDrawableState());
        }
    }

    @Override
    public void drawableHotspotChanged(float x, float y) {
        super.drawableHotspotChanged(x, y);
        if (this.mForeground != null) {
            this.mForeground.setHotspot(x, y);
        }
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(-1, -1);
    }

    public void setForeground(Drawable d) {
        if (this.mForeground != d) {
            if (this.mForeground != null) {
                this.mForeground.setCallback(null);
                unscheduleDrawable(this.mForeground);
            }
            this.mForeground = d;
            this.mForegroundPaddingLeft = 0;
            this.mForegroundPaddingTop = 0;
            this.mForegroundPaddingRight = 0;
            this.mForegroundPaddingBottom = 0;
            if (d != null) {
                setWillNotDraw(false);
                d.setCallback(this);
                d.setLayoutDirection(getLayoutDirection());
                if (d.isStateful()) {
                    d.setState(getDrawableState());
                }
                applyForegroundTint();
                if (this.mForegroundGravity == 119) {
                    Rect padding = new Rect();
                    if (d.getPadding(padding)) {
                        this.mForegroundPaddingLeft = padding.left;
                        this.mForegroundPaddingTop = padding.top;
                        this.mForegroundPaddingRight = padding.right;
                        this.mForegroundPaddingBottom = padding.bottom;
                    }
                }
            } else {
                setWillNotDraw(true);
            }
            requestLayout();
            invalidate();
        }
    }

    public Drawable getForeground() {
        return this.mForeground;
    }

    public void setForegroundTintList(ColorStateList tint) {
        this.mForegroundTintList = tint;
        this.mHasForegroundTint = true;
        applyForegroundTint();
    }

    public ColorStateList getForegroundTintList() {
        return this.mForegroundTintList;
    }

    public void setForegroundTintMode(PorterDuff.Mode tintMode) {
        this.mForegroundTintMode = tintMode;
        this.mHasForegroundTintMode = true;
        applyForegroundTint();
    }

    public PorterDuff.Mode getForegroundTintMode() {
        return this.mForegroundTintMode;
    }

    private void applyForegroundTint() {
        if (this.mForeground != null) {
            if (this.mHasForegroundTint || this.mHasForegroundTintMode) {
                this.mForeground = this.mForeground.mutate();
                if (this.mHasForegroundTint) {
                    this.mForeground.setTintList(this.mForegroundTintList);
                }
                if (this.mHasForegroundTintMode) {
                    this.mForeground.setTintMode(this.mForegroundTintMode);
                }
                if (this.mForeground.isStateful()) {
                    this.mForeground.setState(getDrawableState());
                }
            }
        }
    }

    int getPaddingLeftWithForeground() {
        return this.mForegroundInPadding ? Math.max(this.mPaddingLeft, this.mForegroundPaddingLeft) : this.mPaddingLeft + this.mForegroundPaddingLeft;
    }

    int getPaddingRightWithForeground() {
        return this.mForegroundInPadding ? Math.max(this.mPaddingRight, this.mForegroundPaddingRight) : this.mPaddingRight + this.mForegroundPaddingRight;
    }

    private int getPaddingTopWithForeground() {
        return this.mForegroundInPadding ? Math.max(this.mPaddingTop, this.mForegroundPaddingTop) : this.mPaddingTop + this.mForegroundPaddingTop;
    }

    private int getPaddingBottomWithForeground() {
        return this.mForegroundInPadding ? Math.max(this.mPaddingBottom, this.mForegroundPaddingBottom) : this.mPaddingBottom + this.mForegroundPaddingBottom;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int childWidthMeasureSpec;
        int childHeightMeasureSpec;
        int count = getChildCount();
        boolean measureMatchParentChildren = (View.MeasureSpec.getMode(widthMeasureSpec) == 1073741824 && View.MeasureSpec.getMode(heightMeasureSpec) == 1073741824) ? false : true;
        this.mMatchParentChildren.clear();
        int maxHeight = 0;
        int maxWidth = 0;
        int childState = 0;
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (this.mMeasureAllChildren || child.getVisibility() != 8) {
                measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                maxWidth = Math.max(maxWidth, child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin);
                maxHeight = Math.max(maxHeight, child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin);
                childState = combineMeasuredStates(childState, child.getMeasuredState());
                if (measureMatchParentChildren && (lp.width == -1 || lp.height == -1)) {
                    this.mMatchParentChildren.add(child);
                }
            }
        }
        int maxWidth2 = maxWidth + getPaddingLeftWithForeground() + getPaddingRightWithForeground();
        int maxHeight2 = Math.max(maxHeight + getPaddingTopWithForeground() + getPaddingBottomWithForeground(), getSuggestedMinimumHeight());
        int maxWidth3 = Math.max(maxWidth2, getSuggestedMinimumWidth());
        Drawable drawable = getForeground();
        if (drawable != null) {
            maxHeight2 = Math.max(maxHeight2, drawable.getMinimumHeight());
            maxWidth3 = Math.max(maxWidth3, drawable.getMinimumWidth());
        }
        setMeasuredDimension(resolveSizeAndState(maxWidth3, widthMeasureSpec, childState), resolveSizeAndState(maxHeight2, heightMeasureSpec, childState << 16));
        int count2 = this.mMatchParentChildren.size();
        if (count2 > 1) {
            for (int i2 = 0; i2 < count2; i2++) {
                View child2 = this.mMatchParentChildren.get(i2);
                ViewGroup.MarginLayoutParams lp2 = (ViewGroup.MarginLayoutParams) child2.getLayoutParams();
                if (lp2.width == -1) {
                    childWidthMeasureSpec = View.MeasureSpec.makeMeasureSpec((((getMeasuredWidth() - getPaddingLeftWithForeground()) - getPaddingRightWithForeground()) - lp2.leftMargin) - lp2.rightMargin, 1073741824);
                } else {
                    childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, getPaddingLeftWithForeground() + getPaddingRightWithForeground() + lp2.leftMargin + lp2.rightMargin, lp2.width);
                }
                if (lp2.height == -1) {
                    childHeightMeasureSpec = View.MeasureSpec.makeMeasureSpec((((getMeasuredHeight() - getPaddingTopWithForeground()) - getPaddingBottomWithForeground()) - lp2.topMargin) - lp2.bottomMargin, 1073741824);
                } else {
                    childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec, getPaddingTopWithForeground() + getPaddingBottomWithForeground() + lp2.topMargin + lp2.bottomMargin, lp2.height);
                }
                child2.measure(childWidthMeasureSpec, childHeightMeasureSpec);
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        layoutChildren(left, top, right, bottom, false);
    }

    void layoutChildren(int left, int top, int right, int bottom, boolean forceLeftGravity) {
        int childLeft;
        int childTop;
        int count = getChildCount();
        int parentLeft = getPaddingLeftWithForeground();
        int parentRight = (right - left) - getPaddingRightWithForeground();
        int parentTop = getPaddingTopWithForeground();
        int parentBottom = (bottom - top) - getPaddingBottomWithForeground();
        this.mForegroundBoundsChanged = true;
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != 8) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                int width = child.getMeasuredWidth();
                int height = child.getMeasuredHeight();
                int gravity = lp.gravity;
                if (gravity == -1) {
                    gravity = DEFAULT_CHILD_GRAVITY;
                }
                int layoutDirection = getLayoutDirection();
                int absoluteGravity = Gravity.getAbsoluteGravity(gravity, layoutDirection);
                int verticalGravity = gravity & 112;
                switch (absoluteGravity & 7) {
                    case 1:
                        childLeft = (((((parentRight - parentLeft) - width) / 2) + parentLeft) + lp.leftMargin) - lp.rightMargin;
                        break;
                    case 5:
                        if (!forceLeftGravity) {
                            childLeft = (parentRight - width) - lp.rightMargin;
                            break;
                        }
                    default:
                        childLeft = parentLeft + lp.leftMargin;
                        break;
                }
                switch (verticalGravity) {
                    case 16:
                        childTop = (((((parentBottom - parentTop) - height) / 2) + parentTop) + lp.topMargin) - lp.bottomMargin;
                        break;
                    case 48:
                        childTop = parentTop + lp.topMargin;
                        break;
                    case 80:
                        childTop = (parentBottom - height) - lp.bottomMargin;
                        break;
                    default:
                        childTop = parentTop + lp.topMargin;
                        break;
                }
                child.layout(childLeft, childTop, childLeft + width, childTop + height);
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.mForegroundBoundsChanged = true;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (this.mForeground != null) {
            Drawable foreground = this.mForeground;
            if (this.mForegroundBoundsChanged) {
                this.mForegroundBoundsChanged = false;
                Rect selfBounds = this.mSelfBounds;
                Rect overlayBounds = this.mOverlayBounds;
                int w = this.mRight - this.mLeft;
                int h = this.mBottom - this.mTop;
                if (this.mForegroundInPadding) {
                    selfBounds.set(0, 0, w, h);
                } else {
                    selfBounds.set(this.mPaddingLeft, this.mPaddingTop, w - this.mPaddingRight, h - this.mPaddingBottom);
                }
                int layoutDirection = getLayoutDirection();
                Gravity.apply(this.mForegroundGravity, foreground.getIntrinsicWidth(), foreground.getIntrinsicHeight(), selfBounds, overlayBounds, layoutDirection);
                foreground.setBounds(overlayBounds);
            }
            foreground.draw(canvas);
        }
    }

    @Override
    public boolean gatherTransparentRegion(Region region) {
        boolean opaque = super.gatherTransparentRegion(region);
        if (region != null && this.mForeground != null) {
            applyDrawableToTransparentRegion(this.mForeground, region);
        }
        return opaque;
    }

    @RemotableViewMethod
    public void setMeasureAllChildren(boolean measureAll) {
        this.mMeasureAllChildren = measureAll;
    }

    @Deprecated
    public boolean getConsiderGoneChildrenWhenMeasuring() {
        return getMeasureAllChildren();
    }

    public boolean getMeasureAllChildren() {
        return this.mMeasureAllChildren;
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(FrameLayout.class.getName());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(FrameLayout.class.getName());
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        public int gravity;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            this.gravity = -1;
            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.FrameLayout_Layout);
            this.gravity = a.getInt(0, -1);
            a.recycle();
        }

        public LayoutParams(int width, int height) {
            super(width, height);
            this.gravity = -1;
        }

        public LayoutParams(int width, int height, int gravity) {
            super(width, height);
            this.gravity = -1;
            this.gravity = gravity;
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
            this.gravity = -1;
        }

        public LayoutParams(ViewGroup.MarginLayoutParams source) {
            super(source);
            this.gravity = -1;
        }

        public LayoutParams(LayoutParams source) {
            super((ViewGroup.MarginLayoutParams) source);
            this.gravity = -1;
            this.gravity = source.gravity;
        }
    }
}
