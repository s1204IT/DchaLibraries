package com.android.printspooler.widget;

import android.content.Context;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import com.android.printspooler.R;

public final class PrintContentView extends ViewGroup implements View.OnClickListener {
    private int mClosedOptionsOffsetY;
    private int mCurrentOptionsOffsetY;
    private float mDragProgress;
    private View mDraggableContent;
    private final ViewDragHelper mDragger;
    private View mDynamicContent;
    private View mEmbeddedContentContainer;
    private View mEmbeddedContentScrim;
    private View mExpandCollapseHandle;
    private View mExpandCollapseIcon;
    private View mMoreOptionsButton;
    private int mOldDraggableHeight;
    private ViewGroup mOptionsContainer;
    private OptionsStateChangeListener mOptionsStateChangeListener;
    private OptionsStateController mOptionsStateController;
    private View mPrintButton;
    private final int mScrimColor;
    private View mStaticContent;
    private ViewGroup mSummaryContent;

    public interface OptionsStateChangeListener {
        void onOptionsClosed();

        void onOptionsOpened();
    }

    public interface OptionsStateController {
        boolean canCloseOptions();

        boolean canOpenOptions();
    }

    static int access$412(PrintContentView x0, int x1) {
        int i = x0.mCurrentOptionsOffsetY + x1;
        x0.mCurrentOptionsOffsetY = i;
        return i;
    }

    public PrintContentView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mCurrentOptionsOffsetY = Integer.MIN_VALUE;
        this.mDragger = ViewDragHelper.create(this, new DragCallbacks());
        this.mScrimColor = context.getResources().getColor(R.color.print_preview_scrim_color);
        setChildrenDrawingOrderEnabled(true);
    }

    public void setOptionsStateChangeListener(OptionsStateChangeListener listener) {
        this.mOptionsStateChangeListener = listener;
    }

    public void setOpenOptionsController(OptionsStateController controller) {
        this.mOptionsStateController = controller;
    }

    public boolean isOptionsOpened() {
        return this.mCurrentOptionsOffsetY == 0;
    }

    private boolean isOptionsClosed() {
        return this.mCurrentOptionsOffsetY == this.mClosedOptionsOffsetY;
    }

    public void openOptions() {
        if (!isOptionsOpened()) {
            this.mDragger.smoothSlideViewTo(this.mDynamicContent, this.mDynamicContent.getLeft(), getOpenedOptionsY());
            invalidate();
        }
    }

    public void closeOptions() {
        if (!isOptionsClosed()) {
            this.mDragger.smoothSlideViewTo(this.mDynamicContent, this.mDynamicContent.getLeft(), getClosedOptionsY());
            invalidate();
        }
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        return (childCount - i) - 1;
    }

    @Override
    protected void onFinishInflate() {
        this.mStaticContent = findViewById(R.id.static_content);
        this.mSummaryContent = (ViewGroup) findViewById(R.id.summary_content);
        this.mDynamicContent = findViewById(R.id.dynamic_content);
        this.mDraggableContent = findViewById(R.id.draggable_content);
        this.mPrintButton = findViewById(R.id.print_button);
        this.mMoreOptionsButton = findViewById(R.id.more_options_button);
        this.mOptionsContainer = (ViewGroup) findViewById(R.id.options_container);
        this.mEmbeddedContentContainer = findViewById(R.id.embedded_content_container);
        this.mEmbeddedContentScrim = findViewById(R.id.embedded_content_scrim);
        this.mExpandCollapseHandle = findViewById(R.id.expand_collapse_handle);
        this.mExpandCollapseIcon = findViewById(R.id.expand_collapse_icon);
        this.mExpandCollapseHandle.setOnClickListener(this);
        this.mSummaryContent.setOnClickListener(this);
        onDragProgress(1.0f);
        setFocusableInTouchMode(true);
    }

    @Override
    public void focusableViewAvailable(View v) {
    }

    @Override
    public void onClick(View view) {
        if (view == this.mExpandCollapseHandle || view == this.mSummaryContent) {
            if (isOptionsClosed() && this.mOptionsStateController.canOpenOptions()) {
                openOptions();
                return;
            } else {
                if (isOptionsOpened() && this.mOptionsStateController.canCloseOptions()) {
                    closeOptions();
                    return;
                }
                return;
            }
        }
        if (view == this.mEmbeddedContentScrim && isOptionsOpened() && this.mOptionsStateController.canCloseOptions()) {
            closeOptions();
        }
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        this.mDragger.processTouchEvent(event);
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return this.mDragger.shouldInterceptTouchEvent(event) || super.onInterceptTouchEvent(event);
    }

    @Override
    public void computeScroll() {
        if (this.mDragger.continueSettling(true)) {
            postInvalidateOnAnimation();
        }
    }

    private int computeScrimColor() {
        int baseAlpha = (this.mScrimColor & (-16777216)) >>> 24;
        int adjustedAlpha = (int) (baseAlpha * (1.0f - this.mDragProgress));
        return (adjustedAlpha << 24) | (this.mScrimColor & 16777215);
    }

    private int getOpenedOptionsY() {
        return this.mStaticContent.getBottom();
    }

    private int getClosedOptionsY() {
        return getOpenedOptionsY() + this.mClosedOptionsOffsetY;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        boolean wasOpened = isOptionsOpened();
        measureChild(this.mStaticContent, widthMeasureSpec, heightMeasureSpec);
        if (this.mSummaryContent.getVisibility() != 8) {
            measureChild(this.mSummaryContent, widthMeasureSpec, heightMeasureSpec);
        }
        measureChild(this.mDynamicContent, widthMeasureSpec, heightMeasureSpec);
        measureChild(this.mPrintButton, widthMeasureSpec, heightMeasureSpec);
        this.mClosedOptionsOffsetY = this.mSummaryContent.getMeasuredHeight() - this.mDraggableContent.getMeasuredHeight();
        if (this.mCurrentOptionsOffsetY == Integer.MIN_VALUE) {
            this.mCurrentOptionsOffsetY = this.mClosedOptionsOffsetY;
        }
        int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);
        ViewGroup.LayoutParams params = this.mEmbeddedContentContainer.getLayoutParams();
        params.height = (((heightSize - this.mStaticContent.getMeasuredHeight()) - this.mSummaryContent.getMeasuredHeight()) - this.mDynamicContent.getMeasuredHeight()) + this.mDraggableContent.getMeasuredHeight();
        if (this.mOldDraggableHeight != this.mDraggableContent.getMeasuredHeight()) {
            if (this.mOldDraggableHeight != 0) {
                this.mCurrentOptionsOffsetY = wasOpened ? 0 : this.mClosedOptionsOffsetY;
            }
            this.mOldDraggableHeight = this.mDraggableContent.getMeasuredHeight();
        }
        int hostHeightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, 0);
        measureChild(this.mEmbeddedContentContainer, widthMeasureSpec, hostHeightMeasureSpec);
        setMeasuredDimension(resolveSize(View.MeasureSpec.getSize(widthMeasureSpec), widthMeasureSpec), resolveSize(heightSize, heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int printButtonLeft;
        this.mStaticContent.layout(left, top, right, this.mStaticContent.getMeasuredHeight());
        if (this.mSummaryContent.getVisibility() != 8) {
            this.mSummaryContent.layout(left, this.mStaticContent.getMeasuredHeight(), right, this.mStaticContent.getMeasuredHeight() + this.mSummaryContent.getMeasuredHeight());
        }
        int dynContentTop = this.mStaticContent.getMeasuredHeight() + this.mCurrentOptionsOffsetY;
        int dynContentBottom = dynContentTop + this.mDynamicContent.getMeasuredHeight();
        this.mDynamicContent.layout(left, dynContentTop, right, dynContentBottom);
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) this.mPrintButton.getLayoutParams();
        if (getLayoutDirection() == 0) {
            printButtonLeft = (right - this.mPrintButton.getMeasuredWidth()) - params.getMarginStart();
        } else {
            printButtonLeft = left + params.getMarginStart();
        }
        int printButtonTop = dynContentBottom - (this.mPrintButton.getMeasuredHeight() / 2);
        int printButtonRight = printButtonLeft + this.mPrintButton.getMeasuredWidth();
        int printButtonBottom = printButtonTop + this.mPrintButton.getMeasuredHeight();
        this.mPrintButton.layout(printButtonLeft, printButtonTop, printButtonRight, printButtonBottom);
        int embContentTop = this.mStaticContent.getMeasuredHeight() + this.mClosedOptionsOffsetY + this.mDynamicContent.getMeasuredHeight();
        int embContentBottom = embContentTop + this.mEmbeddedContentContainer.getMeasuredHeight();
        this.mEmbeddedContentContainer.layout(left, embContentTop, right, embContentBottom);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new ViewGroup.MarginLayoutParams(getContext(), attrs);
    }

    private void onDragProgress(float progress) {
        if (Float.compare(this.mDragProgress, progress) != 0) {
            if ((this.mDragProgress == 0.0f && progress > 0.0f) || (this.mDragProgress == 1.0f && progress < 1.0f)) {
                this.mSummaryContent.setLayerType(2, null);
                this.mDraggableContent.setLayerType(2, null);
                this.mMoreOptionsButton.setLayerType(2, null);
                ensureImeClosedAndInputFocusCleared();
            }
            if ((this.mDragProgress > 0.0f && progress == 0.0f) || (this.mDragProgress < 1.0f && progress == 1.0f)) {
                this.mSummaryContent.setLayerType(0, null);
                this.mDraggableContent.setLayerType(0, null);
                this.mMoreOptionsButton.setLayerType(0, null);
                this.mMoreOptionsButton.setLayerType(0, null);
            }
            this.mDragProgress = progress;
            this.mSummaryContent.setAlpha(progress);
            float inverseAlpha = 1.0f - progress;
            this.mOptionsContainer.setAlpha(inverseAlpha);
            this.mMoreOptionsButton.setAlpha(inverseAlpha);
            this.mEmbeddedContentScrim.setBackgroundColor(computeScrimColor());
            if (progress == 0.0f) {
                if (this.mOptionsStateChangeListener != null) {
                    this.mOptionsStateChangeListener.onOptionsOpened();
                }
                this.mExpandCollapseHandle.setContentDescription(this.mContext.getString(R.string.collapse_handle));
                announceForAccessibility(this.mContext.getString(R.string.print_options_expanded));
                this.mSummaryContent.setVisibility(8);
                this.mEmbeddedContentScrim.setOnClickListener(this);
                this.mExpandCollapseIcon.setBackgroundResource(R.drawable.ic_expand_less);
            } else {
                this.mSummaryContent.setVisibility(0);
            }
            if (progress == 1.0f) {
                if (this.mOptionsStateChangeListener != null) {
                    this.mOptionsStateChangeListener.onOptionsClosed();
                }
                this.mExpandCollapseHandle.setContentDescription(this.mContext.getString(R.string.expand_handle));
                announceForAccessibility(this.mContext.getString(R.string.print_options_collapsed));
                if (this.mMoreOptionsButton.getVisibility() != 8) {
                    this.mMoreOptionsButton.setVisibility(4);
                }
                this.mDraggableContent.setVisibility(4);
                this.mEmbeddedContentScrim.setOnClickListener(null);
                this.mEmbeddedContentScrim.setClickable(false);
                this.mExpandCollapseIcon.setBackgroundResource(R.drawable.ic_expand_more);
                return;
            }
            if (this.mMoreOptionsButton.getVisibility() != 8) {
                this.mMoreOptionsButton.setVisibility(0);
            }
            this.mDraggableContent.setVisibility(0);
        }
    }

    private void ensureImeClosedAndInputFocusCleared() {
        View focused = findFocus();
        if (focused != null && focused.isFocused()) {
            InputMethodManager imm = (InputMethodManager) this.mContext.getSystemService("input_method");
            if (imm.isActive(focused)) {
                imm.hideSoftInputFromWindow(getWindowToken(), 0);
            }
            focused.clearFocus();
        }
    }

    private final class DragCallbacks extends ViewDragHelper.Callback {
        private DragCallbacks() {
        }

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            if (!PrintContentView.this.isOptionsOpened() || PrintContentView.this.mOptionsStateController.canCloseOptions()) {
                return (!PrintContentView.this.isOptionsClosed() || PrintContentView.this.mOptionsStateController.canOpenOptions()) && child == PrintContentView.this.mDynamicContent && pointerId == 0;
            }
            return false;
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            if ((!PrintContentView.this.isOptionsClosed() && !PrintContentView.this.isOptionsClosed()) || dy > 0) {
                PrintContentView.access$412(PrintContentView.this, dy);
                float progress = (top - PrintContentView.this.getOpenedOptionsY()) / (PrintContentView.this.getClosedOptionsY() - PrintContentView.this.getOpenedOptionsY());
                PrintContentView.this.mPrintButton.offsetTopAndBottom(dy);
                PrintContentView.this.mDraggableContent.notifySubtreeAccessibilityStateChangedIfNeeded();
                PrintContentView.this.onDragProgress(progress);
            }
        }

        @Override
        public void onViewReleased(View child, float velocityX, float velocityY) {
            int childTop = child.getTop();
            int openedOptionsY = PrintContentView.this.getOpenedOptionsY();
            int closedOptionsY = PrintContentView.this.getClosedOptionsY();
            if (childTop != openedOptionsY && childTop != closedOptionsY) {
                int halfRange = closedOptionsY + ((openedOptionsY - closedOptionsY) / 2);
                if (childTop < halfRange) {
                    PrintContentView.this.mDragger.smoothSlideViewTo(child, child.getLeft(), closedOptionsY);
                } else {
                    PrintContentView.this.mDragger.smoothSlideViewTo(child, child.getLeft(), openedOptionsY);
                }
                PrintContentView.this.invalidate();
            }
        }

        @Override
        public int getOrderedChildIndex(int index) {
            return (PrintContentView.this.getChildCount() - index) - 1;
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            return PrintContentView.this.mDraggableContent.getHeight();
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            PrintContentView.this.mStaticContent.getBottom();
            return Math.max(Math.min(top, PrintContentView.this.getOpenedOptionsY()), PrintContentView.this.getClosedOptionsY());
        }
    }
}
