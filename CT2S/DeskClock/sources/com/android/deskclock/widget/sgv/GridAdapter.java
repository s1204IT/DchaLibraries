package com.android.deskclock.widget.sgv;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public abstract class GridAdapter extends BaseAdapter {
    private static final int GRID_ID_TAG = "gridIdTag".hashCode();
    private View mFooterView;

    public abstract int getItemColumnSpan(Object obj, int i);

    public boolean isDraggable(int position) {
        return false;
    }

    public int getReorderingArea(int position, boolean isLastColumnInGrid) {
        return 0;
    }

    public int getReorderingDirection() {
        return 3;
    }

    public void setFooterView(View view) {
        this.mFooterView = view;
    }

    public long getItemIdFromView(View view, int position) {
        Object id = view.getTag(GRID_ID_TAG);
        return id != null ? ((Long) id).longValue() : getItemId(position);
    }

    public long getItemId(Object item, int position) {
        return getItemId(position);
    }

    public int getItemViewType(Object item, int position) {
        return getItemViewType(position);
    }

    public View getView(Object item, int position, View scrap, ViewGroup parent, int measuredWidth) {
        return getView(position, scrap, parent);
    }

    public int getFirstChangedPosition() {
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
