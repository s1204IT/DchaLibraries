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

    public CategoryAdapter(Context context, int i) {
        super(context, i);
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
            getItem(i).clearBitmap();
        }
        super.clear();
    }

    public void setItemHeight(int i) {
        this.mItemHeight = i;
    }

    public void setItemWidth(int i) {
        this.mItemWidth = i;
    }

    @Override
    public void add(Action action) {
        super.add(action);
        action.setAdapter(this);
    }

    public void initializeSelection(int i) {
        this.mCategory = i;
        this.mSelectedPosition = -1;
        if (i == 0) {
            this.mSelectedPosition = 0;
            this.mAddButtonText = getContext().getString(R.string.filtershow_add_button_looks);
        }
        if (i == 1) {
            this.mSelectedPosition = 0;
        }
        if (i == 4) {
            this.mAddButtonText = getContext().getString(R.string.filtershow_add_button_versions);
        }
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = new CategoryView(getContext());
        }
        CategoryView categoryView = (CategoryView) view;
        categoryView.setOrientation(this.mOrientation);
        Action item = getItem(i);
        categoryView.setAction(item, this);
        int i2 = this.mItemWidth;
        int i3 = this.mItemHeight;
        if (item.getType() == 3) {
            if (this.mOrientation == 1) {
                i2 /= 2;
            } else {
                i3 /= 2;
            }
        }
        if (item.getType() == 2 && this.mOrientation == 0) {
            i3 /= 2;
        }
        categoryView.setLayoutParams(new AbsListView.LayoutParams(i2, i3));
        categoryView.setTag(Integer.valueOf(i));
        categoryView.invalidate();
        return categoryView;
    }

    public void setSelected(View view) {
        int i = this.mSelectedPosition;
        this.mSelectedPosition = ((Integer) view.getTag()).intValue();
        if (i != -1) {
            invalidateView(i);
        }
        invalidateView(this.mSelectedPosition);
    }

    public boolean isSelected(View view) {
        return ((Integer) view.getTag()).intValue() == this.mSelectedPosition;
    }

    private void invalidateView(int i) {
        View childAt;
        if (this.mContainer instanceof ListView) {
            ListView listView = (ListView) this.mContainer;
            childAt = listView.getChildAt(i - listView.getFirstVisiblePosition());
        } else {
            childAt = ((CategoryTrack) this.mContainer).getChildAt(i);
        }
        if (childAt != null) {
            childAt.invalidate();
        }
    }

    public void setContainer(View view) {
        this.mContainer = view;
    }

    public void imageLoaded() {
        notifyDataSetChanged();
    }

    public FilterRepresentation getTinyPlanet() {
        for (int i = 0; i < getCount(); i++) {
            Action item = getItem(i);
            if (item.getRepresentation() != null && (item.getRepresentation() instanceof FilterTinyPlanetRepresentation)) {
                return item.getRepresentation();
            }
        }
        return null;
    }

    public void removeTinyPlanet() {
        for (int i = 0; i < getCount(); i++) {
            Action item = getItem(i);
            if (item.getRepresentation() != null && (item.getRepresentation() instanceof FilterTinyPlanetRepresentation)) {
                super.remove(item);
                return;
            }
        }
    }

    @Override
    public void remove(Action action) {
        if (this.mCategory != 4 && this.mCategory != 0) {
            return;
        }
        super.remove(action);
        FilterShowActivity filterShowActivity = (FilterShowActivity) getContext();
        if (this.mCategory == 0) {
            filterShowActivity.removeLook(action);
        } else if (this.mCategory == 4) {
            filterShowActivity.removeVersion(action);
        }
    }

    public void setOrientation(int i) {
        this.mOrientation = i;
    }

    public void reflectImagePreset(ImagePreset imagePreset) {
        int positionForType;
        if (imagePreset == null) {
            return;
        }
        FilterRepresentation filterRepresentation = null;
        if (this.mCategory == 0) {
            int positionForType2 = imagePreset.getPositionForType(2);
            if (positionForType2 != -1) {
                filterRepresentation = imagePreset.getFilterRepresentation(positionForType2);
            }
        } else if (this.mCategory == 1 && (positionForType = imagePreset.getPositionForType(1)) != -1) {
            filterRepresentation = imagePreset.getFilterRepresentation(positionForType);
        }
        int i = 0;
        if (filterRepresentation != null) {
            int i2 = 0;
            while (true) {
                if (i2 >= getCount()) {
                    break;
                }
                FilterRepresentation representation = getItem(i2).getRepresentation();
                if (representation == null || !filterRepresentation.getName().equalsIgnoreCase(representation.getName())) {
                    i2++;
                } else {
                    i = i2;
                    break;
                }
            }
        }
        if (this.mSelectedPosition != i) {
            this.mSelectedPosition = i;
            notifyDataSetChanged();
        }
    }

    public boolean showAddButton() {
        return this.mShowAddButton;
    }

    public void setShowAddButton(boolean z) {
        this.mShowAddButton = z;
    }

    public String getAddButtonText() {
        return this.mAddButtonText;
    }
}
