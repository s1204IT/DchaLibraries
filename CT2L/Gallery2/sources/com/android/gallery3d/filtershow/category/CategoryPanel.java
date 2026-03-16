package com.android.gallery3d.filtershow.category;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.FilterShowActivity;

public class CategoryPanel extends Fragment implements View.OnClickListener {
    private CategoryAdapter mAdapter;
    private IconView mAddButton;
    private int mCurrentAdapter = 0;

    public void setAdapter(int value) {
        this.mCurrentAdapter = value;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        loadAdapter(this.mCurrentAdapter);
    }

    public void loadAdapter(int adapter) {
        FilterShowActivity activity = (FilterShowActivity) getActivity();
        switch (adapter) {
            case 0:
                this.mAdapter = activity.getCategoryLooksAdapter();
                if (this.mAdapter != null) {
                    this.mAdapter.initializeSelection(0);
                }
                activity.updateCategories();
                break;
            case 1:
                this.mAdapter = activity.getCategoryBordersAdapter();
                if (this.mAdapter != null) {
                    this.mAdapter.initializeSelection(1);
                }
                activity.updateCategories();
                break;
            case 2:
                this.mAdapter = activity.getCategoryGeometryAdapter();
                if (this.mAdapter != null) {
                    this.mAdapter.initializeSelection(2);
                }
                break;
            case 3:
                this.mAdapter = activity.getCategoryFiltersAdapter();
                if (this.mAdapter != null) {
                    this.mAdapter.initializeSelection(3);
                }
                break;
            case 4:
                this.mAdapter = activity.getCategoryVersionsAdapter();
                if (this.mAdapter != null) {
                    this.mAdapter.initializeSelection(4);
                }
                break;
        }
        updateAddButtonVisibility();
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putInt("currentPanel", this.mCurrentAdapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LinearLayout main = (LinearLayout) inflater.inflate(R.layout.filtershow_category_panel_new, container, false);
        if (savedInstanceState != null) {
            int selectedPanel = savedInstanceState.getInt("currentPanel");
            loadAdapter(selectedPanel);
        }
        View panelView = main.findViewById(R.id.listItems);
        if (panelView instanceof CategoryTrack) {
            CategoryTrack panel = (CategoryTrack) panelView;
            if (this.mAdapter != null) {
                this.mAdapter.setOrientation(1);
                panel.setAdapter(this.mAdapter);
                this.mAdapter.setContainer(panel);
            }
        } else if (this.mAdapter != null) {
            ListView panel2 = (ListView) main.findViewById(R.id.listItems);
            panel2.setAdapter((ListAdapter) this.mAdapter);
            this.mAdapter.setContainer(panel2);
        }
        this.mAddButton = (IconView) main.findViewById(R.id.addButton);
        if (this.mAddButton != null) {
            this.mAddButton.setOnClickListener(this);
            updateAddButtonVisibility();
        }
        return main;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.addButton:
                FilterShowActivity activity = (FilterShowActivity) getActivity();
                activity.addCurrentVersion();
                break;
        }
    }

    public void updateAddButtonVisibility() {
        if (this.mAddButton != null) {
            FilterShowActivity activity = (FilterShowActivity) getActivity();
            if (activity.isShowingImageStatePanel() && this.mAdapter.showAddButton()) {
                this.mAddButton.setVisibility(0);
                if (this.mAdapter != null) {
                    this.mAddButton.setText(this.mAdapter.getAddButtonText());
                    return;
                }
                return;
            }
            this.mAddButton.setVisibility(8);
        }
    }
}
