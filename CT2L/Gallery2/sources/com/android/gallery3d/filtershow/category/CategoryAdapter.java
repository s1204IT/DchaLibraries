package com.android.gallery3d.filtershow.category;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.filters.FilterTinyPlanetRepresentation;
import com.android.gallery3d.filtershow.pipeline.ImagePreset;

public class CategoryAdapter extends ArrayAdapter<Action> {
    private String mAddButtonText;
    int mCategory;
    private View mContainer;
    private int mItemHeight;
    private int mItemWidth;
    private int mOrientation;
    private int mSelectedPosition;
    private boolean mShowAddButton;

    public CategoryAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
        this.mItemWidth = -1;
        this.mShowAddButton = false;
        this.mItemHeight = (int) (context.getResources().getDisplayMetrics().density * 100.0f);
    }

    public CategoryAdapter(Context context) {
        this(context, 0);
    }

    @Override
    public void clear() {
        for (int i = 0; i < getCount(); i++) {
            Action action = getItem(i);
            action.clearBitmap();
        }
        super.clear();
    }

    public void setItemHeight(int height) {
        this.mItemHeight = height;
    }

    public void setItemWidth(int width) {
        this.mItemWidth = width;
    }

    @Override
    public void add(Action action) {
        super.add(action);
        action.setAdapter(this);
    }

    public void initializeSelection(int category) {
        this.mCategory = category;
        this.mSelectedPosition = -1;
        if (category == 0) {
            this.mSelectedPosition = 0;
            this.mAddButtonText = getContext().getString(R.string.filtershow_add_button_looks);
        }
        if (category == 1) {
            this.mSelectedPosition = 0;
        }
        if (category == 4) {
            this.mAddButtonText = getContext().getString(R.string.filtershow_add_button_versions);
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = new CategoryView(getContext());
        }
        CategoryView view = (CategoryView) convertView;
        view.setOrientation(this.mOrientation);
        Action action = getItem(position);
        view.setAction(action, this);
        int width = this.mItemWidth;
        int height = this.mItemHeight;
        if (action.getType() == 3) {
            if (this.mOrientation == 1) {
                width /= 2;
            } else {
                height /= 2;
            }
        }
        if (action.getType() == 2 && this.mOrientation == 0) {
            height /= 2;
        }
        view.setLayoutParams(new AbsListView.LayoutParams(width, height));
        view.setTag(Integer.valueOf(position));
        view.invalidate();
        return view;
    }

    public void setSelected(View v) {
        int old = this.mSelectedPosition;
        this.mSelectedPosition = ((Integer) v.getTag()).intValue();
        if (old != -1) {
            invalidateView(old);
        }
        invalidateView(this.mSelectedPosition);
    }

    public boolean isSelected(View v) {
        return ((Integer) v.getTag()).intValue() == this.mSelectedPosition;
    }

    private void invalidateView(int position) {
        View child;
        if (this.mContainer instanceof ListView) {
            ListView lv = (ListView) this.mContainer;
            child = lv.getChildAt(position - lv.getFirstVisiblePosition());
        } else {
            CategoryTrack ct = (CategoryTrack) this.mContainer;
            child = ct.getChildAt(position);
        }
        if (child != null) {
            child.invalidate();
        }
    }

    public void setContainer(View container) {
        this.mContainer = container;
    }

    public void imageLoaded() {
        notifyDataSetChanged();
    }

    public FilterRepresentation getTinyPlanet() {
        for (int i = 0; i < getCount(); i++) {
            Action action = getItem(i);
            if (action.getRepresentation() != null && (action.getRepresentation() instanceof FilterTinyPlanetRepresentation)) {
                return action.getRepresentation();
            }
        }
        return null;
    }

    public void removeTinyPlanet() {
        for (int i = 0; i < getCount(); i++) {
            Action action = getItem(i);
            if (action.getRepresentation() != null && (action.getRepresentation() instanceof FilterTinyPlanetRepresentation)) {
                super.remove(action);
                return;
            }
        }
    }

    @Override
    public void remove(Action action) {
        if (this.mCategory == 4 || this.mCategory == 0) {
            super.remove(action);
            FilterShowActivity activity = (FilterShowActivity) getContext();
            if (this.mCategory == 0) {
                activity.removeLook(action);
            } else if (this.mCategory == 4) {
                activity.removeVersion(action);
            }
        }
    }

    public void setOrientation(int orientation) {
        this.mOrientation = orientation;
    }

    public void reflectImagePreset(ImagePreset preset) {
        int pos;
        if (preset != null) {
            int selected = 0;
            FilterRepresentation rep = null;
            if (this.mCategory == 0) {
                int pos2 = preset.getPositionForType(2);
                if (pos2 != -1) {
                    rep = preset.getFilterRepresentation(pos2);
                }
            } else if (this.mCategory == 1 && (pos = preset.getPositionForType(1)) != -1) {
                rep = preset.getFilterRepresentation(pos);
            }
            if (rep != null) {
                int i = 0;
                while (true) {
                    if (i >= getCount()) {
                        break;
                    }
                    FilterRepresentation itemRep = getItem(i).getRepresentation();
                    if (itemRep == null || !rep.getName().equalsIgnoreCase(itemRep.getName())) {
                        i++;
                    } else {
                        selected = i;
                        break;
                    }
                }
            }
            if (this.mSelectedPosition != selected) {
                this.mSelectedPosition = selected;
                notifyDataSetChanged();
            }
        }
    }

    public boolean showAddButton() {
        return this.mShowAddButton;
    }

    public void setShowAddButton(boolean showAddButton) {
        this.mShowAddButton = showAddButton;
    }

    public String getAddButtonText() {
        return this.mAddButtonText;
    }
}
