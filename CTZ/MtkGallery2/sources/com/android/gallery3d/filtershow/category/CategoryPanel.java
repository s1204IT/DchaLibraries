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

    public void setAdapter(int i) {
        this.mCurrentAdapter = i;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        loadAdapter(this.mCurrentAdapter);
    }

    public void loadAdapter(int i) {
        FilterShowActivity filterShowActivity = (FilterShowActivity) getActivity();
        switch (i) {
            case 0:
                this.mAdapter = filterShowActivity.getCategoryLooksAdapter();
                if (this.mAdapter != null) {
                    this.mAdapter.initializeSelection(0);
                }
                filterShowActivity.updateCategories();
                break;
            case 1:
                this.mAdapter = filterShowActivity.getCategoryBordersAdapter();
                if (this.mAdapter != null) {
                    this.mAdapter.initializeSelection(1);
                }
                filterShowActivity.updateCategories();
                break;
            case 2:
                this.mAdapter = filterShowActivity.getCategoryGeometryAdapter();
                if (this.mAdapter != null) {
                    this.mAdapter.initializeSelection(2);
                }
                break;
            case 3:
                this.mAdapter = filterShowActivity.getCategoryFiltersAdapter();
                if (this.mAdapter != null) {
                    this.mAdapter.initializeSelection(3);
                }
                break;
            case 4:
                this.mAdapter = filterShowActivity.getCategoryVersionsAdapter();
                if (this.mAdapter != null) {
                    this.mAdapter.initializeSelection(4);
                }
                break;
        }
        updateAddButtonVisibility();
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putInt("currentPanel", this.mCurrentAdapter);
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        LinearLayout linearLayout = (LinearLayout) layoutInflater.inflate(R.layout.filtershow_category_panel_new, viewGroup, false);
        if (bundle != null) {
            loadAdapter(bundle.getInt("currentPanel"));
        }
        ?? FindViewById = linearLayout.findViewById(R.id.listItems);
        if (FindViewById instanceof CategoryTrack) {
            if (this.mAdapter != null) {
                this.mAdapter.setOrientation(1);
                FindViewById.setAdapter(this.mAdapter);
                this.mAdapter.setContainer(FindViewById);
            }
        } else if (this.mAdapter != null) {
            ListView listView = (ListView) linearLayout.findViewById(R.id.listItems);
            listView.setAdapter((ListAdapter) this.mAdapter);
            this.mAdapter.setContainer(listView);
        }
        this.mAddButton = (IconView) linearLayout.findViewById(R.id.addButton);
        if (this.mAddButton != null) {
            this.mAddButton.setOnClickListener(this);
            updateAddButtonVisibility();
        }
        return linearLayout;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.addButton) {
            view.setEnabled(false);
            FilterShowActivity filterShowActivity = (FilterShowActivity) getActivity();
            if (filterShowActivity.hasFinishApplyVersionOperation()) {
                filterShowActivity.addCurrentVersion();
                filterShowActivity.addLabel();
            }
            view.setEnabled(true);
        }
    }

    public void updateAddButtonVisibility() {
        if (this.mAddButton == null) {
            return;
        }
        if (((FilterShowActivity) getActivity()).isShowingImageStatePanel() && this.mAdapter.showAddButton()) {
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
