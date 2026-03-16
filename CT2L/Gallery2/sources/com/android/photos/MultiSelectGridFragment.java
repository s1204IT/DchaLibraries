package com.android.photos;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.TextView;
import com.android.gallery3d.R;
import com.android.photos.MultiChoiceManager;

public abstract class MultiSelectGridFragment extends Fragment implements AdapterView.OnItemClickListener, MultiChoiceManager.Delegate {
    ListAdapter mAdapter;
    TextView mEmptyView;
    GridView mGrid;
    View mGridContainer;
    boolean mGridShown;
    MultiChoiceManager.Provider mHost;
    View mProgressContainer;
    private final Handler mHandler = new Handler();
    private final Runnable mRequestFocus = new Runnable() {
        @Override
        public void run() {
            MultiSelectGridFragment.this.mGrid.focusableViewAvailable(MultiSelectGridFragment.this.mGrid);
        }
    };

    public abstract Object getPathForItem(Object obj);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.multigrid_content, container, false);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mHost = (MultiChoiceManager.Provider) activity;
        if (this.mGrid != null) {
            this.mGrid.setMultiChoiceModeListener(this.mHost.getMultiChoiceManager());
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.mHost = null;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ensureGrid();
    }

    @Override
    public void onDestroyView() {
        this.mHandler.removeCallbacks(this.mRequestFocus);
        this.mGrid = null;
        this.mGridShown = false;
        this.mEmptyView = null;
        this.mGridContainer = null;
        this.mProgressContainer = null;
        super.onDestroyView();
    }

    public void onGridItemClick(GridView g, View v, int position, long id) {
    }

    public void setAdapter(ListAdapter adapter) {
        boolean hadAdapter = this.mAdapter != null;
        this.mAdapter = adapter;
        if (this.mGrid != null) {
            this.mGrid.setAdapter(adapter);
            if (!this.mGridShown && !hadAdapter) {
                setGridShown(true, getView().getWindowToken() != null);
            }
        }
    }

    public GridView getGridView() {
        ensureGrid();
        return this.mGrid;
    }

    private void setGridShown(boolean shown, boolean animate) {
        ensureGrid();
        if (this.mProgressContainer == null) {
            throw new IllegalStateException("Can't be used with a custom content view");
        }
        if (this.mGridShown != shown) {
            this.mGridShown = shown;
            if (shown) {
                if (animate) {
                    this.mProgressContainer.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out));
                    this.mGridContainer.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in));
                } else {
                    this.mProgressContainer.clearAnimation();
                    this.mGridContainer.clearAnimation();
                }
                this.mProgressContainer.setVisibility(8);
                this.mGridContainer.setVisibility(0);
                return;
            }
            if (animate) {
                this.mProgressContainer.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in));
                this.mGridContainer.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out));
            } else {
                this.mProgressContainer.clearAnimation();
                this.mGridContainer.clearAnimation();
            }
            this.mProgressContainer.setVisibility(0);
            this.mGridContainer.setVisibility(8);
        }
    }

    public ListAdapter getAdapter() {
        return this.mGrid.getAdapter();
    }

    private void ensureGrid() {
        if (this.mGrid == null) {
            View root = getView();
            if (root == null) {
                throw new IllegalStateException("Content view not yet created");
            }
            if (root instanceof GridView) {
                this.mGrid = (GridView) root;
            } else {
                View empty = root.findViewById(android.R.id.empty);
                if (empty != null && (empty instanceof TextView)) {
                    this.mEmptyView = (TextView) empty;
                }
                this.mProgressContainer = root.findViewById(R.id.progressContainer);
                this.mGridContainer = root.findViewById(R.id.gridContainer);
                View rawGridView = root.findViewById(android.R.id.list);
                if (!(rawGridView instanceof GridView)) {
                    throw new RuntimeException("Content has view with id attribute 'android.R.id.list' that is not a GridView class");
                }
                this.mGrid = (GridView) rawGridView;
                if (this.mGrid == null) {
                    throw new RuntimeException("Your content must have a GridView whose id attribute is 'android.R.id.list'");
                }
                if (this.mEmptyView != null) {
                    this.mGrid.setEmptyView(this.mEmptyView);
                }
            }
            this.mGridShown = true;
            this.mGrid.setOnItemClickListener(this);
            this.mGrid.setMultiChoiceModeListener(this.mHost.getMultiChoiceManager());
            if (this.mAdapter != null) {
                ListAdapter adapter = this.mAdapter;
                this.mAdapter = null;
                setAdapter(adapter);
            } else if (this.mProgressContainer != null) {
                setGridShown(false, false);
            }
            this.mHandler.post(this.mRequestFocus);
        }
    }

    @Override
    public Object getItemAtPosition(int position) {
        return getAdapter().getItem(position);
    }

    @Override
    public Object getPathForItemAtPosition(int position) {
        return getPathForItem(getItemAtPosition(position));
    }

    @Override
    public SparseBooleanArray getSelectedItemPositions() {
        return this.mGrid.getCheckedItemPositions();
    }

    @Override
    public int getSelectedItemCount() {
        return this.mGrid.getCheckedItemCount();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        onGridItemClick((GridView) parent, v, position, id);
    }
}
