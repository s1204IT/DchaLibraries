package com.android.storagemanager.deletionhelper;

import android.view.View;

public class LoadingSpinnerController {
    private boolean mHasLoadedACategory;
    private View mListView;
    private DeletionHelperActivity mParentActivity;

    public LoadingSpinnerController(DeletionHelperActivity deletionHelperActivity) {
        this.mParentActivity = deletionHelperActivity;
    }

    public void initializeLoading(View view) {
        this.mListView = view;
        if (!this.mHasLoadedACategory) {
            setLoading(true);
        }
    }

    public void onCategoryLoad() {
        this.mHasLoadedACategory = true;
        setLoading(false);
    }

    private void setLoading(boolean z) {
        if (this.mListView != null && this.mParentActivity.isLoadingVisible() != z) {
            this.mParentActivity.setLoading(this.mListView, z, true);
        }
    }
}
