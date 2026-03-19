package com.android.documentsui.sorting;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import com.android.documentsui.R;
import com.android.documentsui.sorting.SortController;
import com.android.documentsui.sorting.SortModel;

public final class DropdownSortWidgetController implements SortController.WidgetController {
    static final boolean $assertionsDisabled = false;
    private final ImageView mArrow;
    private final TextView mDimensionButton;
    private final SortModel.UpdateListener mListener;
    private final PopupMenu mMenu;
    private final SortModel mModel;
    private final View mWidget;

    public DropdownSortWidgetController(SortModel sortModel, View view) {
        this.mModel = sortModel;
        this.mWidget = view;
        this.mDimensionButton = (TextView) this.mWidget.findViewById(R.id.sort_dimen_dropdown);
        this.mDimensionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view2) {
                this.f$0.showMenu(view2);
            }
        });
        this.mMenu = new PopupMenu(view.getContext(), this.mDimensionButton, 8388661);
        this.mMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public final boolean onMenuItemClick(MenuItem menuItem) {
                return this.f$0.onSelectDimension(menuItem);
            }
        });
        this.mArrow = (ImageView) this.mWidget.findViewById(R.id.sort_arrow);
        this.mArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view2) {
                this.f$0.onChangeDirection(view2);
            }
        });
        populateMenuItems();
        onModelUpdate(this.mModel, -1);
        this.mListener = new SortModel.UpdateListener() {
            @Override
            public final void onModelUpdate(SortModel sortModel2, int i) {
                this.f$0.onModelUpdate(sortModel2, i);
            }
        };
        this.mModel.addListener(this.mListener);
    }

    @Override
    public void setVisibility(int i) {
        this.mWidget.setVisibility(i);
    }

    @Override
    public void destroy() {
        this.mModel.removeListener(this.mListener);
    }

    private void populateMenuItems() {
        Menu menu = this.mMenu.getMenu();
        menu.clear();
        for (int i = 0; i < this.mModel.getSize(); i++) {
            SortDimension dimensionAt = this.mModel.getDimensionAt(i);
            if (dimensionAt.getSortCapability() != 0) {
                menu.add(0, dimensionAt.getId(), 0, dimensionAt.getLabelId());
            }
        }
    }

    private void showMenu(View view) {
        this.mMenu.show();
    }

    private void onModelUpdate(SortModel sortModel, int i) {
        int sortedDimensionId = sortModel.getSortedDimensionId();
        if ((i & 1) != 0) {
            updateVisibility();
        }
        if ((i & 2) != 0) {
            bindSortedDimension(sortedDimensionId);
            bindSortDirection(sortedDimensionId);
        }
    }

    private void updateVisibility() {
        Menu menu = this.mMenu.getMenu();
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            item.setVisible(this.mModel.getDimensionById(item.getItemId()).getVisibility() == 0);
        }
    }

    private void bindSortedDimension(int i) {
        if (i == 0) {
            this.mDimensionButton.setText(R.string.not_sorted);
        } else {
            this.mDimensionButton.setText(this.mModel.getDimensionById(i).getLabelId());
        }
    }

    private void bindSortDirection(int i) {
        if (i == 0) {
            this.mArrow.setVisibility(4);
            return;
        }
        SortDimension dimensionById = this.mModel.getDimensionById(i);
        switch (dimensionById.getSortDirection()) {
            case 0:
                this.mArrow.setVisibility(4);
                return;
            case 1:
                showArrow(0, R.string.sort_direction_ascending);
                return;
            case 2:
                showArrow(10000, R.string.sort_direction_descending);
                return;
            default:
                throw new IllegalStateException("Unknown sort direction: " + dimensionById.getSortDirection() + ".");
        }
    }

    private void showArrow(int i, int i2) {
        this.mArrow.setVisibility(0);
        this.mArrow.getDrawable().mutate();
        this.mArrow.setImageLevel(i);
        this.mArrow.setContentDescription(this.mArrow.getContext().getString(i2));
    }

    private boolean onSelectDimension(MenuItem menuItem) {
        int currentSortDirection = this.mModel.getCurrentSortDirection();
        SortDimension dimensionById = this.mModel.getDimensionById(menuItem.getItemId());
        if ((dimensionById.getSortCapability() & currentSortDirection) <= 0) {
            currentSortDirection = dimensionById.getDefaultSortDirection();
        }
        this.mModel.sortByUser(dimensionById.getId(), currentSortDirection);
        return true;
    }

    private void onChangeDirection(View view) {
        SortDimension dimensionById = this.mModel.getDimensionById(this.mModel.getSortedDimensionId());
        this.mModel.sortByUser(dimensionById.getId(), dimensionById.getNextDirection());
    }
}
