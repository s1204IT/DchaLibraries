package com.android.browser;

import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;

public class UrlSelectionActionMode implements ActionMode.Callback {
    private UiController mUiController;

    public UrlSelectionActionMode(UiController controller) {
        this.mUiController = controller;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.setTitleOptionalHint(false);
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.shareText:
                if (this.mUiController.getCurrentTopWebView() != null) {
                    InputMethodManager inputMethod = (InputMethodManager) this.mUiController.getActivity().getSystemService("input_method");
                    inputMethod.hideSoftInputFromWindow(this.mUiController.getCurrentTopWebView().getWindowToken(), 0);
                }
                this.mUiController.shareCurrentPage();
                mode.finish();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return true;
    }
}
