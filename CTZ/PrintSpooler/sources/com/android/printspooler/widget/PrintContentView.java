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

    static int access$412(PrintContentView printContentView, int i) {
        int i2 = printContentView.mCurrentOptionsOffsetY + i;
        printContentView.mCurrentOptionsOffsetY = i2;
        return i2;
    }

    public PrintContentView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mCurrentOptionsOffsetY = Integer.MIN_VALUE;
        this.mDragger = ViewDragHelper.create(this, new DragCallbacks());
        this.mScrimColor = context.getColor(R.color.print_preview_scrim_color);
        setChildrenDrawingOrderEnabled(true);
    }

    public void setOptionsStateChangeListener(OptionsStateChangeListener optionsStateChangeListener) {
        this.mOptionsStateChangeListener = optionsStateChangeListener;
    }

    public void setOpenOptionsController(OptionsStateController optionsStateController) {
        this.mOptionsStateController = optionsStateController;
    }

    public boolean isOptionsOpened() {
        return this.mCurrentOptionsOffsetY == 0;
    }

    private boolean isOptionsClosed() {
        return this.mCurrentOptionsOffsetY == this.mClosedOptionsOffsetY;
    }

    public void openOptions() {
        if (isOptionsOpened()) {
            return;
        }
        this.mDragger.smoothSlideViewTo(this.mDynamicContent, this.mDynamicContent.getLeft(), getOpenedOptionsY());
        invalidate();
    }

    public void closeOptions() {
        if (isOptionsClosed()) {
            return;
        }
        this.mDragger.smoothSlideViewTo(this.mDynamicContent, this.mDynamicContent.getLeft(), getClosedOptionsY());
        invalidate();
    }

    @Override
    protected int getChildDrawingOrder(int i, int i2) {
        return (i - i2) - 1;
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
    public void focusableViewAvailable(View view) {
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
    public void requestDisallowInterceptTouchEvent(boolean z) {
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        this.mDragger.processTouchEvent(motionEvent);
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        return this.mDragger.shouldInterceptTouchEvent(motionEvent) || super.onInterceptTouchEvent(motionEvent);
    }

    @Override
    public void computeScroll() {
        if (this.mDragger.continueSettling(true)) {
            postInvalidateOnAnimation();
        }
    }

    private int computeScrimColor() {
        return (((int) (((this.mScrimColor & (-16777216)) >>> 24) * (1.0f - this.mDragProgress))) << 24) | (this.mScrimColor & 16777215);
    }

    private int getOpenedOptionsY() {
        return this.mStaticContent.getBottom();
    }

    private int getClosedOptionsY() {
        return getOpenedOptionsY() + this.mClosedOptionsOffsetY;
    }

    @Override
    protected void onMeasure(int i, int i2) {
        boolean zIsOptionsOpened = isOptionsOpened();
        measureChild(this.mStaticContent, i, i2);
        if (this.mSummaryContent.getVisibility() != 8) {
            measureChild(this.mSummaryContent, i, i2);
        }
        measureChild(this.mDynamicContent, i, i2);
        measureChild(this.mPrintButton, i, i2);
        this.mClosedOptionsOffsetY = this.mSummaryContent.getMeasuredHeight() - this.mDraggableContent.getMeasuredHeight();
        if (this.mCurrentOptionsOffsetY == Integer.MIN_VALUE) {
            this.mCurrentOptionsOffsetY = this.mClosedOptionsOffsetY;
        }
        int size = View.MeasureSpec.getSize(i2);
        this.mEmbeddedContentContainer.getLayoutParams().height = (((size - this.mStaticContent.getMeasuredHeight()) - this.mSummaryContent.getMeasuredHeight()) - this.mDynamicContent.getMeasuredHeight()) + this.mDraggableContent.getMeasuredHeight();
        if (this.mOldDraggableHeight != this.mDraggableContent.getMeasuredHeight()) {
            if (this.mOldDraggableHeight != 0) {
                this.mCurrentOptionsOffsetY = zIsOptionsOpened ? 0 : this.mClosedOptionsOffsetY;
            }
            this.mOldDraggableHeight = this.mDraggableContent.getMeasuredHeight();
        }
        measureChild(this.mEmbeddedContentContainer, i, View.MeasureSpec.makeMeasureSpec(0, 0));
        setMeasuredDimension(resolveSize(View.MeasureSpec.getSize(i), i), resolveSize(size, i2));
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        int marginStart;
        this.mStaticContent.layout(i, i2, i3, this.mStaticContent.getMeasuredHeight());
        if (this.mSummaryContent.getVisibility() != 8) {
            this.mSummaryContent.layout(i, this.mStaticContent.getMeasuredHeight(), i3, this.mStaticContent.getMeasuredHeight() + this.mSummaryContent.getMeasuredHeight());
        }
        int measuredHeight = this.mStaticContent.getMeasuredHeight() + this.mCurrentOptionsOffsetY;
        int measuredHeight2 = this.mDynamicContent.getMeasuredHeight() + measuredHeight;
        this.mDynamicContent.layout(i, measuredHeight, i3, measuredHeight2);
        ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) this.mPrintButton.getLayoutParams();
        if (getLayoutDirection() == 0) {
            marginStart = (i3 - this.mPrintButton.getMeasuredWidth()) - marginLayoutParams.getMarginStart();
        } else {
            marginStart = i + marginLayoutParams.getMarginStart();
        }
        int measuredHeight3 = measuredHeight2 - (this.mPrintButton.getMeasuredHeight() / 2);
        this.mPrintButton.layout(marginStart, measuredHeight3, this.mPrintButton.getMeasuredWidth() + marginStart, this.mPrintButton.getMeasuredHeight() + measuredHeight3);
        int measuredHeight4 = this.mStaticContent.getMeasuredHeight() + this.mClosedOptionsOffsetY + this.mDynamicContent.getMeasuredHeight();
        this.mEmbeddedContentContainer.layout(i, measuredHeight4, i3, this.mEmbeddedContentContainer.getMeasuredHeight() + measuredHeight4);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attributeSet) {
        return new ViewGroup.MarginLayoutParams(getContext(), attributeSet);
    }

    private void onDragProgress(float f) {
        if (Float.compare(this.mDragProgress, f) == 0) {
            return;
        }
        if ((this.mDragProgress == 0.0f && f > 0.0f) || (this.mDragProgress == 1.0f && f < 1.0f)) {
            this.mSummaryContent.setLayerType(2, null);
            this.mDraggableContent.setLayerType(2, null);
            this.mMoreOptionsButton.setLayerType(2, null);
            ensureImeClosedAndInputFocusCleared();
        }
        if ((this.mDragProgress > 0.0f && f == 0.0f) || (this.mDragProgress < 1.0f && f == 1.0f)) {
            this.mSummaryContent.setLayerType(0, null);
            this.mDraggableContent.setLayerType(0, null);
            this.mMoreOptionsButton.setLayerType(0, null);
            this.mMoreOptionsButton.setLayerType(0, null);
        }
        this.mDragProgress = f;
        this.mSummaryContent.setAlpha(f);
        float f2 = 1.0f - f;
        this.mOptionsContainer.setAlpha(f2);
        this.mMoreOptionsButton.setAlpha(f2);
        this.mEmbeddedContentScrim.setBackgroundColor(computeScrimColor());
        if (f == 0.0f) {
            if (this.mOptionsStateChangeListener != null) {
                this.mOptionsStateChangeListener.onOptionsOpened();
            }
            this.mExpandCollapseHandle.setContentDescription(((View) this).mContext.getString(R.string.collapse_handle));
            announceForAccessibility(((View) this).mContext.getString(R.string.print_options_expanded));
            this.mSummaryContent.setVisibility(8);
            this.mEmbeddedContentScrim.setOnClickListener(this);
            this.mExpandCollapseIcon.setBackgroundResource(R.drawable.ic_expand_less);
        } else {
            this.mSummaryContent.setVisibility(0);
        }
        if (f == 1.0f) {
            if (this.mOptionsStateChangeListener != null) {
                this.mOptionsStateChangeListener.onOptionsClosed();
            }
            this.mExpandCollapseHandle.setContentDescription(((View) this).mContext.getString(R.string.expand_handle));
            announceForAccessibility(((View) this).mContext.getString(R.string.print_options_collapsed));
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

    private void ensureImeClosedAndInputFocusCleared() {
        View viewFindFocus = findFocus();
        if (viewFindFocus != null && viewFindFocus.isFocused()) {
            InputMethodManager inputMethodManager = (InputMethodManager) ((View) this).mContext.getSystemService("input_method");
            if (inputMethodManager.isActive(viewFindFocus)) {
                inputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
            }
            viewFindFocus.clearFocus();
        }
    }

    private final class DragCallbacks extends ViewDragHelper.Callback {
        private DragCallbacks() {
        }

        @Override
        public boolean tryCaptureView(View view, int i) {
            return (!PrintContentView.this.isOptionsOpened() || PrintContentView.this.mOptionsStateController.canCloseOptions()) && (!PrintContentView.this.isOptionsClosed() || PrintContentView.this.mOptionsStateController.canOpenOptions()) && view == PrintContentView.this.mDynamicContent && i == 0;
        }

        @Override
        public void onViewPositionChanged(View view, int i, int i2, int i3, int i4) {
            if ((PrintContentView.this.isOptionsClosed() || PrintContentView.this.isOptionsClosed()) && i4 <= 0) {
                return;
            }
            PrintContentView.access$412(PrintContentView.this, i4);
            PrintContentView.this.mPrintButton.offsetTopAndBottom(i4);
            PrintContentView.this.mDraggableContent.notifySubtreeAccessibilityStateChangedIfNeeded();
            PrintContentView.this.onDragProgress((i2 - PrintContentView.this.getOpenedOptionsY()) / (PrintContentView.this.getClosedOptionsY() - PrintContentView.this.getOpenedOptionsY()));
        }

        @Override
        public void onViewReleased(View view, float f, float f2) {
            int top = view.getTop();
            int openedOptionsY = PrintContentView.this.getOpenedOptionsY();
            int closedOptionsY = PrintContentView.this.getClosedOptionsY();
            if (top == openedOptionsY || top == closedOptionsY) {
                return;
            }
            if (top < ((openedOptionsY - closedOptionsY) / 2) + closedOptionsY) {
                PrintContentView.this.mDragger.smoothSlideViewTo(view, view.getLeft(), closedOptionsY);
            } else {
                PrintContentView.this.mDragger.smoothSlideViewTo(view, view.getLeft(), openedOptionsY);
            }
            PrintContentView.this.invalidate();
        }

        @Override
        public int getOrderedChildIndex(int i) {
            return (PrintContentView.this.getChildCount() - i) - 1;
        }

        @Override
        public int getViewVerticalDragRange(View view) {
            return PrintContentView.this.mDraggableContent.getHeight();
        }

        @Override
        public int clampViewPositionVertical(View view, int i, int i2) {
            PrintContentView.this.mStaticContent.getBottom();
            return Math.max(Math.min(i, PrintContentView.this.getOpenedOptionsY()), PrintContentView.this.getClosedOptionsY());
        }
    }
}
