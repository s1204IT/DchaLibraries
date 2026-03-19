package com.android.documentsui;

import android.app.Fragment;
import android.view.KeyboardShortcutGroup;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.Menus;
import com.android.documentsui.base.RootInfo;
import com.android.documentsui.base.State;
import com.android.documentsui.queries.SearchViewManager;
import com.android.internal.annotations.VisibleForTesting;
import java.util.List;
import java.util.function.IntFunction;

public abstract class MenuManager {
    static final boolean $assertionsDisabled = false;
    protected final DirectoryDetails mDirDetails;
    protected final SearchViewManager mSearchManager;
    protected final State mState;

    public interface SelectionDetails {
        boolean canDelete();

        boolean canExtract();

        boolean canOpenWith();

        boolean canPasteInto();

        boolean canRename();

        boolean canViewInOwner();

        boolean containsDirectories();

        boolean containsFiles();

        boolean containsFilesInArchive();

        boolean containsPartialFiles();

        int size();
    }

    protected abstract void updateCreateDir(MenuItem menuItem);

    public abstract void updateKeyboardShortcutsMenu(List<KeyboardShortcutGroup> list, IntFunction<String> intFunction);

    protected abstract void updateOpenInContextMenu(MenuItem menuItem, SelectionDetails selectionDetails);

    protected abstract void updateSelectAll(MenuItem menuItem);

    public MenuManager(SearchViewManager searchViewManager, State state, DirectoryDetails directoryDetails) {
        this.mSearchManager = searchViewManager;
        this.mState = state;
        this.mDirDetails = directoryDetails;
    }

    public void updateActionMenu(Menu menu, SelectionDetails selectionDetails) {
        updateOpenInActionMode(menu.findItem(R.id.action_menu_open), selectionDetails);
        updateOpenWith(menu.findItem(R.id.action_menu_open_with), selectionDetails);
        updateDelete(menu.findItem(R.id.action_menu_delete), selectionDetails);
        updateShare(menu.findItem(R.id.action_menu_share), selectionDetails);
        updateRename(menu.findItem(R.id.action_menu_rename), selectionDetails);
        updateSelectAll(menu.findItem(R.id.action_menu_select_all));
        updateMoveTo(menu.findItem(R.id.action_menu_move_to), selectionDetails);
        updateCopyTo(menu.findItem(R.id.action_menu_copy_to), selectionDetails);
        updateCompress(menu.findItem(R.id.action_menu_compress), selectionDetails);
        updateExtractTo(menu.findItem(R.id.action_menu_extract_to), selectionDetails);
        updateInspect(menu.findItem(R.id.action_menu_inspect), selectionDetails);
        updateViewInOwner(menu.findItem(R.id.action_menu_view_in_owner), selectionDetails);
        Menus.disableHiddenItems(menu, new MenuItem[0]);
    }

    public void updateOptionMenu(Menu menu) {
        updateCreateDir(menu.findItem(R.id.option_menu_create_dir));
        updateSettings(menu.findItem(R.id.option_menu_settings));
        updateSelectAll(menu.findItem(R.id.option_menu_select_all));
        updateNewWindow(menu.findItem(R.id.option_menu_new_window));
        updateModePicker(menu.findItem(R.id.option_menu_grid), menu.findItem(R.id.option_menu_list));
        updateAdvanced(menu.findItem(R.id.option_menu_advanced));
        updateDebug(menu.findItem(R.id.option_menu_debug));
        updateInspect(menu.findItem(R.id.option_menu_inspect));
        Menus.disableHiddenItems(menu, new MenuItem[0]);
    }

    public void showContextMenu(Fragment fragment, View view, float f, float f2) {
    }

    public void inflateContextMenuForContainer(Menu menu, MenuInflater menuInflater) {
        throw new UnsupportedOperationException("Pickers don't allow context menu.");
    }

    public void inflateContextMenuForDocs(Menu menu, MenuInflater menuInflater, SelectionDetails selectionDetails) {
        throw new UnsupportedOperationException("Pickers don't allow context menu.");
    }

    @VisibleForTesting
    public void updateContextMenuForFiles(Menu menu, SelectionDetails selectionDetails) {
        MenuItem menuItemFindItem = menu.findItem(R.id.dir_menu_share);
        MenuItem menuItemFindItem2 = menu.findItem(R.id.dir_menu_open);
        MenuItem menuItemFindItem3 = menu.findItem(R.id.dir_menu_open_with);
        MenuItem menuItemFindItem4 = menu.findItem(R.id.dir_menu_rename);
        MenuItem menuItemFindItem5 = menu.findItem(R.id.dir_menu_view_in_owner);
        updateShare(menuItemFindItem, selectionDetails);
        updateOpenInContextMenu(menuItemFindItem2, selectionDetails);
        updateOpenWith(menuItemFindItem3, selectionDetails);
        updateRename(menuItemFindItem4, selectionDetails);
        updateViewInOwner(menuItemFindItem5, selectionDetails);
        updateContextMenu(menu, selectionDetails);
    }

    @VisibleForTesting
    public void updateContextMenuForDirs(Menu menu, SelectionDetails selectionDetails) {
        MenuItem menuItemFindItem = menu.findItem(R.id.dir_menu_open_in_new_window);
        MenuItem menuItemFindItem2 = menu.findItem(R.id.dir_menu_rename);
        MenuItem menuItemFindItem3 = menu.findItem(R.id.dir_menu_paste_into_folder);
        updateOpenInNewWindow(menuItemFindItem, selectionDetails);
        updateRename(menuItemFindItem2, selectionDetails);
        updatePasteInto(menuItemFindItem3, selectionDetails);
        updateContextMenu(menu, selectionDetails);
    }

    @VisibleForTesting
    public void updateContextMenu(Menu menu, SelectionDetails selectionDetails) {
        MenuItem menuItemFindItem = menu.findItem(R.id.dir_menu_cut_to_clipboard);
        MenuItem menuItemFindItem2 = menu.findItem(R.id.dir_menu_copy_to_clipboard);
        MenuItem menuItemFindItem3 = menu.findItem(R.id.dir_menu_delete);
        MenuItem menuItemFindItem4 = menu.findItem(R.id.dir_menu_inspect);
        boolean z = selectionDetails.size() > 0 && !selectionDetails.containsPartialFiles();
        boolean zCanDelete = selectionDetails.canDelete();
        menuItemFindItem.setEnabled(z && zCanDelete);
        menuItemFindItem2.setEnabled(z);
        menuItemFindItem3.setEnabled(zCanDelete);
        menuItemFindItem4.setEnabled(selectionDetails.size() == 1);
    }

    @VisibleForTesting
    public void updateContextMenuForContainer(Menu menu) {
        MenuItem menuItemFindItem = menu.findItem(R.id.dir_menu_paste_from_clipboard);
        MenuItem menuItemFindItem2 = menu.findItem(R.id.dir_menu_select_all);
        MenuItem menuItemFindItem3 = menu.findItem(R.id.dir_menu_create_dir);
        MenuItem menuItemFindItem4 = menu.findItem(R.id.dir_menu_inspect);
        menuItemFindItem.setEnabled(this.mDirDetails.hasItemsToPaste() && this.mDirDetails.canCreateDoc());
        updateSelectAll(menuItemFindItem2);
        updateCreateDir(menuItemFindItem3);
        updateInspect(menuItemFindItem4);
    }

    public void updateRootContextMenu(Menu menu, RootInfo rootInfo, DocumentInfo documentInfo) {
        MenuItem menuItemFindItem = menu.findItem(R.id.root_menu_eject_root);
        MenuItem menuItemFindItem2 = menu.findItem(R.id.root_menu_paste_into_folder);
        MenuItem menuItemFindItem3 = menu.findItem(R.id.root_menu_open_in_new_window);
        MenuItem menuItemFindItem4 = menu.findItem(R.id.root_menu_settings);
        updateEject(menuItemFindItem, rootInfo);
        updatePasteInto(menuItemFindItem2, rootInfo, documentInfo);
        updateOpenInNewWindow(menuItemFindItem3, rootInfo);
        updateSettings(menuItemFindItem4, rootInfo);
    }

    protected void updateModePicker(MenuItem menuItem, MenuItem menuItem2) {
        menuItem.setVisible(this.mState.derivedMode != 2);
        menuItem2.setVisible(this.mState.derivedMode != 1);
    }

    protected void updateAdvanced(MenuItem menuItem) {
        menuItem.setVisible(this.mState.showDeviceStorageOption);
        menuItem.setTitle((this.mState.showDeviceStorageOption && this.mState.showAdvanced) ? R.string.menu_advanced_hide : R.string.menu_advanced_show);
    }

    protected void updateDebug(MenuItem menuItem) {
        menuItem.setVisible(this.mState.debugMode);
    }

    protected void updateSettings(MenuItem menuItem) {
        menuItem.setVisible(false);
    }

    protected void updateSettings(MenuItem menuItem, RootInfo rootInfo) {
        menuItem.setVisible(false);
    }

    protected void updateEject(MenuItem menuItem, RootInfo rootInfo) {
        menuItem.setVisible(false);
    }

    protected void updateNewWindow(MenuItem menuItem) {
        menuItem.setVisible(false);
    }

    protected void updateOpenInActionMode(MenuItem menuItem, SelectionDetails selectionDetails) {
        menuItem.setVisible(false);
    }

    protected void updateOpenWith(MenuItem menuItem, SelectionDetails selectionDetails) {
        menuItem.setVisible(false);
    }

    protected void updateOpenInNewWindow(MenuItem menuItem, SelectionDetails selectionDetails) {
        menuItem.setVisible(false);
    }

    protected void updateOpenInNewWindow(MenuItem menuItem, RootInfo rootInfo) {
        menuItem.setVisible(false);
    }

    protected void updateShare(MenuItem menuItem, SelectionDetails selectionDetails) {
        menuItem.setVisible(false);
    }

    protected void updateDelete(MenuItem menuItem, SelectionDetails selectionDetails) {
        menuItem.setVisible(false);
    }

    protected void updateRename(MenuItem menuItem, SelectionDetails selectionDetails) {
        menuItem.setVisible(false);
    }

    protected void updateInspect(MenuItem menuItem) {
        menuItem.setVisible(false);
    }

    protected void updateInspect(MenuItem menuItem, SelectionDetails selectionDetails) {
        menuItem.setVisible(false);
    }

    protected void updateViewInOwner(MenuItem menuItem, SelectionDetails selectionDetails) {
        menuItem.setVisible(false);
    }

    protected void updateMoveTo(MenuItem menuItem, SelectionDetails selectionDetails) {
        menuItem.setVisible(false);
    }

    protected void updateCopyTo(MenuItem menuItem, SelectionDetails selectionDetails) {
        menuItem.setVisible(false);
    }

    protected void updateCompress(MenuItem menuItem, SelectionDetails selectionDetails) {
        menuItem.setVisible(false);
    }

    protected void updateExtractTo(MenuItem menuItem, SelectionDetails selectionDetails) {
        menuItem.setVisible(false);
    }

    protected void updatePasteInto(MenuItem menuItem, SelectionDetails selectionDetails) {
        menuItem.setVisible(false);
    }

    protected void updatePasteInto(MenuItem menuItem, RootInfo rootInfo, DocumentInfo documentInfo) {
        menuItem.setVisible(false);
    }

    public static class DirectoryDetails {
        private final BaseActivity mActivity;

        public DirectoryDetails(BaseActivity baseActivity) {
            this.mActivity = baseActivity;
        }

        public boolean hasRootSettings() {
            return this.mActivity.getCurrentRoot().hasSettings();
        }

        public boolean hasItemsToPaste() {
            return false;
        }

        public boolean canCreateDoc() {
            if (isInRecents()) {
                return false;
            }
            return this.mActivity.getCurrentDirectory().isCreateSupported();
        }

        public boolean isInRecents() {
            return this.mActivity.getCurrentDirectory() == null;
        }

        public boolean canCreateDirectory() {
            return this.mActivity.canCreateDirectory();
        }
    }
}
