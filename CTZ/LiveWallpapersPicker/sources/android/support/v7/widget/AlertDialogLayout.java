package android.support.v7.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.appcompat.R;
import android.support.v7.widget.LinearLayoutCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class AlertDialogLayout extends LinearLayoutCompat {
    public AlertDialogLayout(Context context) {
        super(context);
    }

    public AlertDialogLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (!tryOnMeasure(widthMeasureSpec, heightMeasureSpec)) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    private boolean tryOnMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int childHeightSpec;
        int count = getChildCount();
        View middlePanel = null;
        View buttonPanel = null;
        View topPanel = null;
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != 8) {
                int id = child.getId();
                if (id == R.id.topPanel) {
                    topPanel = child;
                } else if (id == R.id.buttonPanel) {
                    buttonPanel = child;
                } else {
                    if ((id != R.id.contentPanel && id != R.id.customPanel) || middlePanel != null) {
                        return false;
                    }
                    middlePanel = child;
                }
            }
        }
        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);
        int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int childState = 0;
        int usedHeight = getPaddingTop() + getPaddingBottom();
        if (topPanel != null) {
            topPanel.measure(widthMeasureSpec, 0);
            usedHeight += topPanel.getMeasuredHeight();
            childState = View.combineMeasuredStates(0, topPanel.getMeasuredState());
        }
        int buttonHeight = 0;
        int buttonWantsHeight = 0;
        if (buttonPanel != null) {
            buttonPanel.measure(widthMeasureSpec, 0);
            buttonHeight = resolveMinimumHeight(buttonPanel);
            buttonWantsHeight = buttonPanel.getMeasuredHeight() - buttonHeight;
            usedHeight += buttonHeight;
            childState = View.combineMeasuredStates(childState, buttonPanel.getMeasuredState());
        }
        int middleHeight = 0;
        if (middlePanel != null) {
            if (heightMode == 0) {
                childHeightSpec = 0;
            } else {
                childHeightSpec = View.MeasureSpec.makeMeasureSpec(Math.max(0, heightSize - usedHeight), heightMode);
            }
            middlePanel.measure(widthMeasureSpec, childHeightSpec);
            middleHeight = middlePanel.getMeasuredHeight();
            usedHeight += middleHeight;
            childState = View.combineMeasuredStates(childState, middlePanel.getMeasuredState());
        }
        int remainingHeight = heightSize - usedHeight;
        if (buttonPanel != null) {
            int usedHeight2 = usedHeight - buttonHeight;
            int heightToGive = Math.min(remainingHeight, buttonWantsHeight);
            if (heightToGive > 0) {
                remainingHeight -= heightToGive;
                buttonHeight += heightToGive;
            }
            buttonPanel.measure(widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(buttonHeight, 1073741824));
            usedHeight = usedHeight2 + buttonPanel.getMeasuredHeight();
            childState = View.combineMeasuredStates(childState, buttonPanel.getMeasuredState());
            remainingHeight = remainingHeight;
        }
        if (middlePanel != null && remainingHeight > 0) {
            int heightToGive2 = remainingHeight;
            middlePanel.measure(widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(middleHeight + heightToGive2, heightMode));
            usedHeight = (usedHeight - middleHeight) + middlePanel.getMeasuredHeight();
            childState = View.combineMeasuredStates(childState, middlePanel.getMeasuredState());
            remainingHeight -= heightToGive2;
        }
        int maxWidth = 0;
        int maxWidth2 = 0;
        while (maxWidth2 < count) {
            int remainingHeight2 = remainingHeight;
            View child2 = getChildAt(maxWidth2);
            View buttonPanel2 = buttonPanel;
            View middlePanel2 = middlePanel;
            if (child2.getVisibility() != 8) {
                maxWidth = Math.max(maxWidth, child2.getMeasuredWidth());
            }
            maxWidth2++;
            remainingHeight = remainingHeight2;
            buttonPanel = buttonPanel2;
            middlePanel = middlePanel2;
        }
        int widthSizeAndState = View.resolveSizeAndState(maxWidth + getPaddingLeft() + getPaddingRight(), widthMeasureSpec, childState);
        int heightSizeAndState = View.resolveSizeAndState(usedHeight, heightMeasureSpec, 0);
        setMeasuredDimension(widthSizeAndState, heightSizeAndState);
        if (widthMode != 1073741824) {
            forceUniformWidth(count, heightMeasureSpec);
            return true;
        }
        return true;
    }

    private void forceUniformWidth(int count, int heightMeasureSpec) {
        int uniformMeasureSpec = View.MeasureSpec.makeMeasureSpec(getMeasuredWidth(), 1073741824);
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != 8) {
                LinearLayoutCompat.LayoutParams lp = (LinearLayoutCompat.LayoutParams) child.getLayoutParams();
                if (((ViewGroup.LayoutParams) lp).width == -1) {
                    int oldHeight = ((ViewGroup.LayoutParams) lp).height;
                    ((ViewGroup.LayoutParams) lp).height = child.getMeasuredHeight();
                    measureChildWithMargins(child, uniformMeasureSpec, 0, heightMeasureSpec, 0);
                    ((ViewGroup.LayoutParams) lp).height = oldHeight;
                }
            }
        }
    }

    private static int resolveMinimumHeight(View v) {
        int minHeight = ViewCompat.getMinimumHeight(v);
        if (minHeight > 0) {
            return minHeight;
        }
        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            if (vg.getChildCount() == 1) {
                return resolveMinimumHeight(vg.getChildAt(0));
            }
        }
        return 0;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int childTop;
        int i;
        Drawable dividerDrawable;
        int majorGravity;
        int count;
        int childLeft;
        AlertDialogLayout alertDialogLayout = this;
        int paddingLeft = getPaddingLeft();
        int width = right - left;
        int childRight = width - getPaddingRight();
        int childSpace = (width - paddingLeft) - getPaddingRight();
        int totalLength = getMeasuredHeight();
        int count2 = getChildCount();
        int gravity = getGravity();
        int majorGravity2 = gravity & 112;
        int minorGravity = gravity & 8388615;
        if (majorGravity2 == 16) {
            int childTop2 = getPaddingTop();
            childTop = childTop2 + (((bottom - top) - totalLength) / 2);
        } else if (majorGravity2 == 80) {
            childTop = ((getPaddingTop() + bottom) - top) - totalLength;
        } else {
            childTop = getPaddingTop();
        }
        Drawable dividerDrawable2 = getDividerDrawable();
        int i2 = 0;
        int dividerHeight = dividerDrawable2 == null ? 0 : dividerDrawable2.getIntrinsicHeight();
        while (i2 < count2) {
            View child = alertDialogLayout.getChildAt(i2);
            if (child != null) {
                dividerDrawable = dividerDrawable2;
                majorGravity = majorGravity2;
                if (child.getVisibility() != 8) {
                    int childWidth = child.getMeasuredWidth();
                    int childHeight = child.getMeasuredHeight();
                    LinearLayoutCompat.LayoutParams lp = (LinearLayoutCompat.LayoutParams) child.getLayoutParams();
                    int layoutGravity = lp.gravity;
                    if (layoutGravity < 0) {
                        layoutGravity = minorGravity;
                    }
                    int layoutDirection = ViewCompat.getLayoutDirection(this);
                    int absoluteGravity = GravityCompat.getAbsoluteGravity(layoutGravity, layoutDirection);
                    int i3 = absoluteGravity & 7;
                    if (i3 == 1) {
                        int childLeft2 = childSpace - childWidth;
                        childLeft = (((childLeft2 / 2) + paddingLeft) + ((ViewGroup.MarginLayoutParams) lp).leftMargin) - ((ViewGroup.MarginLayoutParams) lp).rightMargin;
                    } else if (i3 == 5) {
                        childLeft = (childRight - childWidth) - ((ViewGroup.MarginLayoutParams) lp).rightMargin;
                    } else {
                        childLeft = ((ViewGroup.MarginLayoutParams) lp).leftMargin + paddingLeft;
                    }
                    if (alertDialogLayout.hasDividerBeforeChildAt(i2)) {
                        childTop += dividerHeight;
                    }
                    int childTop3 = childTop + ((ViewGroup.MarginLayoutParams) lp).topMargin;
                    i = i2;
                    count = count2;
                    alertDialogLayout.setChildFrame(child, childLeft, childTop3, childWidth, childHeight);
                    childTop = childTop3 + childHeight + ((ViewGroup.MarginLayoutParams) lp).bottomMargin;
                } else {
                    i = i2;
                    count = count2;
                }
            } else {
                i = i2;
                dividerDrawable = dividerDrawable2;
                majorGravity = majorGravity2;
                count = count2;
            }
            i2 = i + 1;
            dividerDrawable2 = dividerDrawable;
            majorGravity2 = majorGravity;
            count2 = count;
            alertDialogLayout = this;
        }
    }

    private void setChildFrame(View child, int left, int top, int width, int height) {
        child.layout(left, top, left + width, top + height);
    }
}
