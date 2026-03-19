package com.android.documentsui.picker;

import android.view.KeyboardShortcutGroup;
import android.view.Menu;
import android.view.MenuItem;
import com.android.documentsui.MenuManager;
import com.android.documentsui.base.State;
import com.android.documentsui.queries.SearchViewManager;
import java.util.List;
import java.util.function.IntFunction;

public final class MenuManager extends com.android.documentsui.MenuManager {
    public MenuManager(SearchViewManager searchViewManager, State state, MenuManager.DirectoryDetails directoryDetails) {
        super(searchViewManager, state, directoryDetails);
    }

    @Override
    public void updateKeyboardShortcutsMenu(List<KeyboardShortcutGroup> list, IntFunction<String> intFunction) {
    }

    private boolean picking() {
        return this.mState.action == 4 || this.mState.action == 6 || this.mState.action == 2;
    }

    @Override
    public void updateOptionMenu(Menu menu) {
        super.updateOptionMenu(menu);
        if (picking()) {
            this.mSearchManager.showMenu(null);
        }
    }

    @Override
    protected void updateModePicker(MenuItem menuItem, MenuItem menuItem2) {
        if (picking() && this.mDirDetails.isInRecents()) {
            menuItem.setVisible(false);
            menuItem2.setVisible(false);
        } else {
            super.updateModePicker(menuItem, menuItem2);
        }
    }

    @Override
    protected void updateSelectAll(MenuItem menuItem) {
        boolean z = this.mState.allowMultiple;
        menuItem.setVisible(z);
        menuItem.setEnabled(z);
    }

    @Override
    protected void updateCreateDir(MenuItem menuItem) {
        menuItem.setVisible(picking());
        menuItem.setEnabled(picking() && this.mDirDetails.canCreateDirectory());
    }

    @Override
    protected void updateOpenInActionMode(MenuItem menuItem, MenuManager.SelectionDetails selectionDetails) {
        updateOpen(menuItem, selectionDetails);
    }

    @Override
    protected void updateOpenInContextMenu(MenuItem menuItem, MenuManager.SelectionDetails selectionDetails) {
        updateOpen(menuItem, selectionDetails);
    }

    private void updateOpen(MenuItem menuItem, MenuManager.SelectionDetails selectionDetails) {
        menuItem.setVisible(this.mState.action == 5 || this.mState.action == 3);
        menuItem.setEnabled(selectionDetails.size() > 0);
    }
}
