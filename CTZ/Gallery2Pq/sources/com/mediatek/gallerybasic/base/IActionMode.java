package com.mediatek.gallerybasic.base;

import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

public interface IActionMode {
    boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem);

    void onCreateActionMode(ActionMode actionMode, Menu menu);

    void onDestroyActionMode(ActionMode actionMode);

    void onPrepareActionMode(ActionMode actionMode, Menu menu);

    void onSelectionChange(MediaData[] mediaDataArr);
}
