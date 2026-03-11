package android.support.v17.leanback.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.support.v17.leanback.R$styleable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

public class VerticalGridView extends BaseGridView {
    @Override
    public boolean dispatchGenericFocusedEvent(MotionEvent event) {
        return super.dispatchGenericFocusedEvent(event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        return super.dispatchTouchEvent(event);
    }

    @Override
    public View focusSearch(int direction) {
        return super.focusSearch(direction);
    }

    @Override
    public int getChildDrawingOrder(int childCount, int i) {
        return super.getChildDrawingOrder(childCount, i);
    }

    @Override
    public int getSelectedPosition() {
        return super.getSelectedPosition();
    }

    @Override
    public boolean hasOverlappingRendering() {
        return super.hasOverlappingRendering();
    }

    @Override
    public boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        return super.onRequestFocusInDescendants(direction, previouslyFocusedRect);
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
    }

    @Override
    public void setGravity(int gravity) {
        super.setGravity(gravity);
    }

    @Override
    public void setOnChildViewHolderSelectedListener(OnChildViewHolderSelectedListener listener) {
        super.setOnChildViewHolderSelectedListener(listener);
    }

    @Override
    public void setRecyclerListener(RecyclerView.RecyclerListener listener) {
        super.setRecyclerListener(listener);
    }

    @Override
    public void setSelectedPosition(int position) {
        super.setSelectedPosition(position);
    }

    @Override
    public void setSelectedPositionSmooth(int position) {
        super.setSelectedPositionSmooth(position);
    }

    @Override
    public void setWindowAlignment(int windowAlignment) {
        super.setWindowAlignment(windowAlignment);
    }

    public VerticalGridView(Context context) {
        this(context, null);
    }

    public VerticalGridView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VerticalGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mLayoutManager.setOrientation(1);
        initAttributes(context, attrs);
    }

    protected void initAttributes(Context context, AttributeSet attrs) {
        initBaseGridViewAttributes(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R$styleable.lbVerticalGridView);
        setColumnWidth(a);
        setNumColumns(a.getInt(R$styleable.lbVerticalGridView_numberOfColumns, 1));
        a.recycle();
    }

    void setColumnWidth(TypedArray array) {
        TypedValue typedValue = array.peekValue(R$styleable.lbVerticalGridView_columnWidth);
        if (typedValue == null) {
            return;
        }
        int size = array.getLayoutDimension(R$styleable.lbVerticalGridView_columnWidth, 0);
        setColumnWidth(size);
    }

    public void setNumColumns(int numColumns) {
        this.mLayoutManager.setNumRows(numColumns);
        requestLayout();
    }

    public void setColumnWidth(int width) {
        this.mLayoutManager.setRowHeight(width);
        requestLayout();
    }
}
