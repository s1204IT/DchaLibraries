package android.support.v17.leanback.widget.picker;

import android.content.Context;
import android.graphics.Rect;
import android.support.v17.leanback.R$dimen;
import android.support.v17.leanback.R$id;
import android.support.v17.leanback.R$layout;
import android.support.v17.leanback.widget.OnChildViewHolderSelectedListener;
import android.support.v17.leanback.widget.VerticalGridView;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class Picker extends FrameLayout {
    private Interpolator mAccelerateInterpolator;
    private int mAlphaAnimDuration;
    private final OnChildViewHolderSelectedListener mColumnChangeListener;
    private final List<VerticalGridView> mColumnViews;
    private ArrayList<PickerColumn> mColumns;
    private Interpolator mDecelerateInterpolator;
    private float mFocusedAlpha;
    private float mInvisibleColumnAlpha;
    private ArrayList<PickerValueListener> mListeners;
    private int mPickerItemLayoutId;
    private int mPickerItemTextViewId;
    private ViewGroup mPickerView;
    private ViewGroup mRootView;
    private int mSelectedColumn;
    private CharSequence mSeparator;
    private float mUnfocusedAlpha;
    private float mVisibleColumnAlpha;
    private float mVisibleItems;
    private float mVisibleItemsActivated;

    public interface PickerValueListener {
        void onValueChanged(Picker picker, int i);
    }

    public final CharSequence getSeparator() {
        return this.mSeparator;
    }

    public final void setSeparator(CharSequence seperator) {
        this.mSeparator = seperator;
    }

    public final int getPickerItemLayoutId() {
        return this.mPickerItemLayoutId;
    }

    public final int getPickerItemTextViewId() {
        return this.mPickerItemTextViewId;
    }

    public Picker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mColumnViews = new ArrayList();
        this.mVisibleItemsActivated = 3.0f;
        this.mVisibleItems = 1.0f;
        this.mSelectedColumn = 0;
        this.mPickerItemLayoutId = R$layout.lb_picker_item;
        this.mPickerItemTextViewId = 0;
        this.mColumnChangeListener = new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(RecyclerView parent, RecyclerView.ViewHolder child, int position, int subposition) {
                int colIndex = Picker.this.mColumnViews.indexOf(parent);
                Picker.this.updateColumnAlpha(colIndex, true);
                if (child == null) {
                    return;
                }
                int newValue = ((PickerColumn) Picker.this.mColumns.get(colIndex)).getMinValue() + position;
                Picker.this.onColumnValueChanged(colIndex, newValue);
            }
        };
        setEnabled(true);
        this.mFocusedAlpha = 1.0f;
        this.mUnfocusedAlpha = 1.0f;
        this.mVisibleColumnAlpha = 0.5f;
        this.mInvisibleColumnAlpha = 0.0f;
        this.mAlphaAnimDuration = 200;
        this.mDecelerateInterpolator = new DecelerateInterpolator(2.5f);
        this.mAccelerateInterpolator = new AccelerateInterpolator(2.5f);
        LayoutInflater inflater = LayoutInflater.from(getContext());
        this.mRootView = (ViewGroup) inflater.inflate(R$layout.lb_picker, (ViewGroup) this, true);
        this.mPickerView = (ViewGroup) this.mRootView.findViewById(R$id.picker);
    }

    public PickerColumn getColumnAt(int colIndex) {
        if (this.mColumns == null) {
            return null;
        }
        return this.mColumns.get(colIndex);
    }

    public int getColumnsCount() {
        if (this.mColumns == null) {
            return 0;
        }
        return this.mColumns.size();
    }

    public void setColumns(List<PickerColumn> columns) {
        this.mColumnViews.clear();
        this.mPickerView.removeAllViews();
        this.mColumns = new ArrayList<>(columns);
        if (this.mSelectedColumn > this.mColumns.size() - 1) {
            this.mSelectedColumn = this.mColumns.size() - 1;
        }
        LayoutInflater inflater = LayoutInflater.from(getContext());
        int totalCol = getColumnsCount();
        for (int i = 0; i < totalCol; i++) {
            int colIndex = i;
            VerticalGridView columnView = (VerticalGridView) inflater.inflate(R$layout.lb_picker_column, this.mPickerView, false);
            updateColumnSize(columnView);
            columnView.setWindowAlignment(0);
            columnView.setHasFixedSize(false);
            this.mColumnViews.add(columnView);
            this.mPickerView.addView(columnView);
            if (i != totalCol - 1 && getSeparator() != null) {
                TextView separator = (TextView) inflater.inflate(R$layout.lb_picker_separator, this.mPickerView, false);
                separator.setText(getSeparator());
                this.mPickerView.addView(separator);
            }
            columnView.setAdapter(new PickerScrollArrayAdapter(getContext(), getPickerItemLayoutId(), getPickerItemTextViewId(), colIndex));
            columnView.setOnChildViewHolderSelectedListener(this.mColumnChangeListener);
        }
    }

    public void setColumnAt(int columnIndex, PickerColumn column) {
        this.mColumns.set(columnIndex, column);
        VerticalGridView columnView = this.mColumnViews.get(columnIndex);
        PickerScrollArrayAdapter adapter = (PickerScrollArrayAdapter) columnView.getAdapter();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        columnView.setSelectedPosition(column.getCurrentValue() - column.getMinValue());
    }

    public void setColumnValue(int columnIndex, int value, boolean runAnimation) {
        PickerColumn column = this.mColumns.get(columnIndex);
        if (column.getCurrentValue() == value) {
            return;
        }
        column.setCurrentValue(value);
        notifyValueChanged(columnIndex);
        VerticalGridView columnView = this.mColumnViews.get(columnIndex);
        if (columnView == null) {
            return;
        }
        int position = value - this.mColumns.get(columnIndex).getMinValue();
        if (runAnimation) {
            columnView.setSelectedPositionSmooth(position);
        } else {
            columnView.setSelectedPosition(position);
        }
    }

    private void notifyValueChanged(int columnIndex) {
        if (this.mListeners == null) {
            return;
        }
        for (int i = this.mListeners.size() - 1; i >= 0; i--) {
            this.mListeners.get(i).onValueChanged(this, columnIndex);
        }
    }

    public void updateColumnAlpha(int colIndex, boolean animate) {
        VerticalGridView column = this.mColumnViews.get(colIndex);
        int selected = column.getSelectedPosition();
        int i = 0;
        while (i < column.getAdapter().getItemCount()) {
            View item = column.getLayoutManager().findViewByPosition(i);
            if (item != null) {
                setOrAnimateAlpha(item, selected == i, colIndex, animate);
            }
            i++;
        }
    }

    public void setOrAnimateAlpha(View view, boolean selected, int colIndex, boolean animate) {
        boolean columnShownAsActivated = colIndex == this.mSelectedColumn || !hasFocus();
        if (selected) {
            if (columnShownAsActivated) {
                setOrAnimateAlpha(view, animate, this.mFocusedAlpha, -1.0f, this.mDecelerateInterpolator);
                return;
            } else {
                setOrAnimateAlpha(view, animate, this.mUnfocusedAlpha, -1.0f, this.mDecelerateInterpolator);
                return;
            }
        }
        if (columnShownAsActivated) {
            setOrAnimateAlpha(view, animate, this.mVisibleColumnAlpha, -1.0f, this.mDecelerateInterpolator);
        } else {
            setOrAnimateAlpha(view, animate, this.mInvisibleColumnAlpha, -1.0f, this.mDecelerateInterpolator);
        }
    }

    private void setOrAnimateAlpha(View view, boolean animate, float destAlpha, float startAlpha, Interpolator interpolator) {
        view.animate().cancel();
        if (!animate) {
            view.setAlpha(destAlpha);
            return;
        }
        if (startAlpha >= 0.0f) {
            view.setAlpha(startAlpha);
        }
        view.animate().alpha(destAlpha).setDuration(this.mAlphaAnimDuration).setInterpolator(interpolator).start();
    }

    public void onColumnValueChanged(int columnIndex, int newValue) {
        PickerColumn column = this.mColumns.get(columnIndex);
        if (column.getCurrentValue() == newValue) {
            return;
        }
        column.setCurrentValue(newValue);
        notifyValueChanged(columnIndex);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textView;

        ViewHolder(View v, TextView textView) {
            super(v);
            this.textView = textView;
        }
    }

    class PickerScrollArrayAdapter extends RecyclerView.Adapter<ViewHolder> {
        private final int mColIndex;
        private PickerColumn mData;
        private final int mResource;
        private final int mTextViewResourceId;

        PickerScrollArrayAdapter(Context context, int resource, int textViewResourceId, int colIndex) {
            this.mResource = resource;
            this.mColIndex = colIndex;
            this.mTextViewResourceId = textViewResourceId;
            this.mData = (PickerColumn) Picker.this.mColumns.get(this.mColIndex);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TextView textView;
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View v = inflater.inflate(this.mResource, parent, false);
            if (this.mTextViewResourceId != 0) {
                textView = (TextView) v.findViewById(this.mTextViewResourceId);
            } else {
                textView = (TextView) v;
            }
            ViewHolder vh = new ViewHolder(v, textView);
            return vh;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            if (holder.textView != null && this.mData != null) {
                holder.textView.setText(this.mData.getLabelFor(this.mData.getMinValue() + position));
            }
            Picker.this.setOrAnimateAlpha(holder.itemView, ((VerticalGridView) Picker.this.mColumnViews.get(this.mColIndex)).getSelectedPosition() == position, this.mColIndex, false);
        }

        @Override
        public void onViewAttachedToWindow(ViewHolder holder) {
            holder.itemView.setFocusable(Picker.this.isActivated());
        }

        @Override
        public int getItemCount() {
            if (this.mData == null) {
                return 0;
            }
            return this.mData.getCount();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (isActivated()) {
            int keyCode = event.getKeyCode();
            switch (keyCode) {
                case 23:
                case 66:
                    if (event.getAction() == 1) {
                        performClick();
                    }
                    return true;
                default:
                    return super.dispatchKeyEvent(event);
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        int column = getSelectedColumn();
        if (column < this.mColumnViews.size()) {
            return this.mColumnViews.get(column).requestFocus(direction, previouslyFocusedRect);
        }
        return false;
    }

    protected int getPickerItemHeightPixels() {
        return getContext().getResources().getDimensionPixelSize(R$dimen.picker_item_height);
    }

    private void updateColumnSize() {
        for (int i = 0; i < getColumnsCount(); i++) {
            updateColumnSize(this.mColumnViews.get(i));
        }
    }

    private void updateColumnSize(VerticalGridView columnView) {
        ViewGroup.LayoutParams lp = columnView.getLayoutParams();
        lp.height = (int) ((isActivated() ? getActivatedVisibleItemCount() : getVisibleItemCount()) * getPickerItemHeightPixels());
        columnView.setLayoutParams(lp);
    }

    private void updateItemFocusable() {
        boolean activated = isActivated();
        for (int i = 0; i < getColumnsCount(); i++) {
            VerticalGridView grid = this.mColumnViews.get(i);
            for (int j = 0; j < grid.getChildCount(); j++) {
                View view = grid.getChildAt(j);
                view.setFocusable(activated);
            }
        }
    }

    public float getActivatedVisibleItemCount() {
        return this.mVisibleItemsActivated;
    }

    public float getVisibleItemCount() {
        return 1.0f;
    }

    @Override
    public void setActivated(boolean activated) {
        if (activated != isActivated()) {
            super.setActivated(activated);
            updateColumnSize();
            updateItemFocusable();
            return;
        }
        super.setActivated(activated);
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        super.requestChildFocus(child, focused);
        for (int i = 0; i < this.mColumnViews.size(); i++) {
            if (this.mColumnViews.get(i).hasFocus()) {
                setSelectedColumn(i);
            }
        }
    }

    public void setSelectedColumn(int columnIndex) {
        if (this.mSelectedColumn == columnIndex) {
            return;
        }
        this.mSelectedColumn = columnIndex;
        for (int i = 0; i < this.mColumnViews.size(); i++) {
            updateColumnAlpha(i, true);
        }
    }

    public int getSelectedColumn() {
        return this.mSelectedColumn;
    }
}
