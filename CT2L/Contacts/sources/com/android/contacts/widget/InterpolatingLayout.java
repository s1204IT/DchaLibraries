package com.android.contacts.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.android.contacts.R;

public class InterpolatingLayout extends ViewGroup {
    private Rect mInRect;
    private Rect mOutRect;

    public InterpolatingLayout(Context context) {
        super(context);
        this.mInRect = new Rect();
        this.mOutRect = new Rect();
    }

    public InterpolatingLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mInRect = new Rect();
        this.mOutRect = new Rect();
    }

    public InterpolatingLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mInRect = new Rect();
        this.mOutRect = new Rect();
    }

    public static final class LayoutParams extends LinearLayout.LayoutParams {
        private int leftMarginConstant;
        private float leftMarginMultiplier;
        private int leftPaddingConstant;
        private float leftPaddingMultiplier;
        public int narrowMarginLeft;
        public int narrowMarginRight;
        public int narrowPaddingLeft;
        public int narrowPaddingRight;
        public int narrowParentWidth;
        public int narrowWidth;
        private int rightMarginConstant;
        private float rightMarginMultiplier;
        private int rightPaddingConstant;
        private float rightPaddingMultiplier;
        public int wideMarginLeft;
        public int wideMarginRight;
        public int widePaddingLeft;
        public int widePaddingRight;
        public int wideParentWidth;
        public int wideWidth;
        private int widthConstant;
        private float widthMultiplier;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.InterpolatingLayout_Layout);
            this.narrowParentWidth = a.getDimensionPixelSize(0, -1);
            this.narrowWidth = a.getDimensionPixelSize(1, -1);
            this.narrowMarginLeft = a.getDimensionPixelSize(2, -1);
            this.narrowPaddingLeft = a.getDimensionPixelSize(4, -1);
            this.narrowMarginRight = a.getDimensionPixelSize(3, -1);
            this.narrowPaddingRight = a.getDimensionPixelSize(5, -1);
            this.wideParentWidth = a.getDimensionPixelSize(6, -1);
            this.wideWidth = a.getDimensionPixelSize(7, -1);
            this.wideMarginLeft = a.getDimensionPixelSize(8, -1);
            this.widePaddingLeft = a.getDimensionPixelSize(10, -1);
            this.wideMarginRight = a.getDimensionPixelSize(9, -1);
            this.widePaddingRight = a.getDimensionPixelSize(11, -1);
            a.recycle();
            if (this.narrowWidth != -1) {
                this.widthMultiplier = (this.wideWidth - this.narrowWidth) / (this.wideParentWidth - this.narrowParentWidth);
                this.widthConstant = (int) (this.narrowWidth - (this.narrowParentWidth * this.widthMultiplier));
            }
            if (this.narrowMarginLeft != -1) {
                this.leftMarginMultiplier = (this.wideMarginLeft - this.narrowMarginLeft) / (this.wideParentWidth - this.narrowParentWidth);
                this.leftMarginConstant = (int) (this.narrowMarginLeft - (this.narrowParentWidth * this.leftMarginMultiplier));
            }
            if (this.narrowPaddingLeft != -1) {
                this.leftPaddingMultiplier = (this.widePaddingLeft - this.narrowPaddingLeft) / (this.wideParentWidth - this.narrowParentWidth);
                this.leftPaddingConstant = (int) (this.narrowPaddingLeft - (this.narrowParentWidth * this.leftPaddingMultiplier));
            }
            if (this.narrowMarginRight != -1) {
                this.rightMarginMultiplier = (this.wideMarginRight - this.narrowMarginRight) / (this.wideParentWidth - this.narrowParentWidth);
                this.rightMarginConstant = (int) (this.narrowMarginRight - (this.narrowParentWidth * this.rightMarginMultiplier));
            }
            if (this.narrowPaddingRight != -1) {
                this.rightPaddingMultiplier = (this.widePaddingRight - this.narrowPaddingRight) / (this.wideParentWidth - this.narrowParentWidth);
                this.rightPaddingConstant = (int) (this.narrowPaddingRight - (this.narrowParentWidth * this.rightPaddingMultiplier));
            }
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public int resolveWidth(int parentSize) {
            if (this.narrowWidth == -1) {
                return this.width;
            }
            int w = ((int) (parentSize * this.widthMultiplier)) + this.widthConstant;
            if (w <= 0) {
                return -2;
            }
            return w;
        }

        public int resolveLeftMargin(int parentSize) {
            if (this.narrowMarginLeft == -1) {
                return this.leftMargin;
            }
            int w = ((int) (parentSize * this.leftMarginMultiplier)) + this.leftMarginConstant;
            if (w < 0) {
                return 0;
            }
            return w;
        }

        public int resolveLeftPadding(int parentSize) {
            int w = ((int) (parentSize * this.leftPaddingMultiplier)) + this.leftPaddingConstant;
            if (w < 0) {
                return 0;
            }
            return w;
        }

        public int resolveRightMargin(int parentSize) {
            if (this.narrowMarginRight == -1) {
                return this.rightMargin;
            }
            int w = ((int) (parentSize * this.rightMarginMultiplier)) + this.rightMarginConstant;
            if (w < 0) {
                return 0;
            }
            return w;
        }

        public int resolveRightPadding(int parentSize) {
            int w = ((int) (parentSize * this.rightPaddingMultiplier)) + this.rightPaddingConstant;
            if (w < 0) {
                return 0;
            }
            return w;
        }
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(-2, -2);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int childWidthMeasureSpec;
        int childHeightMeasureSpec;
        int parentWidth = View.MeasureSpec.getSize(widthMeasureSpec);
        int parentHeight = View.MeasureSpec.getSize(heightMeasureSpec);
        int width = 0;
        int height = 0;
        View fillChild = null;
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != 8) {
                LayoutParams params = (LayoutParams) child.getLayoutParams();
                if (params.width == -1) {
                    if (fillChild != null) {
                        throw new RuntimeException("Interpolating layout allows at most one child with layout_width='match_parent'");
                    }
                    fillChild = child;
                } else {
                    int childWidth = params.resolveWidth(parentWidth);
                    switch (childWidth) {
                        case -2:
                            childWidthMeasureSpec = 0;
                            break;
                        default:
                            childWidthMeasureSpec = View.MeasureSpec.makeMeasureSpec(childWidth, 1073741824);
                            break;
                    }
                    switch (params.height) {
                        case -2:
                            childHeightMeasureSpec = 0;
                            break;
                        case -1:
                            childHeightMeasureSpec = View.MeasureSpec.makeMeasureSpec((parentHeight - params.topMargin) - params.bottomMargin, 1073741824);
                            break;
                        default:
                            childHeightMeasureSpec = View.MeasureSpec.makeMeasureSpec(params.height, 1073741824);
                            break;
                    }
                    child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
                    width += child.getMeasuredWidth();
                    height = Math.max(child.getMeasuredHeight(), height);
                }
                width += params.resolveLeftMargin(parentWidth) + params.resolveRightMargin(parentWidth);
            }
        }
        if (fillChild != null) {
            int remainder = parentWidth - width;
            int childMeasureSpec = remainder > 0 ? View.MeasureSpec.makeMeasureSpec(remainder, 1073741824) : 0;
            fillChild.measure(childMeasureSpec, heightMeasureSpec);
            width += fillChild.getMeasuredWidth();
            height = Math.max(fillChild.getMeasuredHeight(), height);
        }
        setMeasuredDimension(resolveSize(width, widthMeasureSpec), resolveSize(height, heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int offset = 0;
        int width = right - left;
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != 8) {
                LayoutParams params = (LayoutParams) child.getLayoutParams();
                int gravity = params.gravity;
                if (gravity == -1) {
                    gravity = 8388659;
                }
                if (params.narrowPaddingLeft != -1 || params.narrowPaddingRight != -1) {
                    int leftPadding = params.narrowPaddingLeft == -1 ? child.getPaddingLeft() : params.resolveLeftPadding(width);
                    int rightPadding = params.narrowPaddingRight == -1 ? child.getPaddingRight() : params.resolveRightPadding(width);
                    child.setPadding(leftPadding, child.getPaddingTop(), rightPadding, child.getPaddingBottom());
                }
                int leftMargin = params.resolveLeftMargin(width);
                int rightMargin = params.resolveRightMargin(width);
                this.mInRect.set(offset + leftMargin, params.topMargin, right - rightMargin, bottom - params.bottomMargin);
                Gravity.apply(gravity, child.getMeasuredWidth(), child.getMeasuredHeight(), this.mInRect, this.mOutRect);
                child.layout(this.mOutRect.left, this.mOutRect.top, this.mOutRect.right, this.mOutRect.bottom);
                offset = this.mOutRect.right + rightMargin;
            }
        }
    }
}
