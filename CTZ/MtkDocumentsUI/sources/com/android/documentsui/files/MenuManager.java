package com.android.documentsui.files;

import android.app.Fragment;
import android.content.Context;
import android.net.Uri;
import android.view.KeyboardShortcutGroup;
import android.view.KeyboardShortcutInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import com.android.documentsui.MenuManager;
import com.android.documentsui.R;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.Features;
import com.android.documentsui.base.Lookup;
import com.android.documentsui.base.RootInfo;
import com.android.documentsui.base.State;
import com.android.documentsui.queries.SearchViewManager;
import com.android.documentsui.selection.SelectionHelper;
import java.util.List;
import java.util.function.IntFunction;

public final class MenuManager extends com.android.documentsui.MenuManager {
    static final boolean $assertionsDisabled = false;
    private final Lookup<String, String> mAppNameLookup;
    private final Context mContext;
    private final Features mFeatures;
    private final SelectionHelper mSelectionManager;
    private final Lookup<String, Uri> mUriLookup;

    public MenuManager(Features features, SearchViewManager searchViewManager, State state, MenuManager.DirectoryDetails directoryDetails, Context context, SelectionHelper selectionHelper, Lookup<String, String> lookup, Lookup<String, Uri> lookup2) {
        super(searchViewManager, state, directoryDetails);
        this.mFeatures = features;
        this.mContext = context;
        this.mSelectionManager = selectionHelper;
        this.mAppNameLookup = lookup;
        this.mUriLookup = lookup2;
    }

    @Override
    public void updateOptionMenu(Menu menu) {
        super.updateOptionMenu(menu);
        this.mSearchManager.updateMenu();
    }

    @Override
    public void updateKeyboardShortcutsMenu(List<KeyboardShortcutGroup> list, IntFunction<String> intFunction) {
        KeyboardShortcutGroup keyboardShortcutGroup = new KeyboardShortcutGroup(intFunction.apply(R.string.app_label));
        keyboardShortcutGroup.addItem(new KeyboardShortcutInfo(intFunction.apply(R.string.menu_cut_to_clipboard), 52, 4096));
        keyboardShortcutGroup.addItem(new KeyboardShortcutInfo(intFunction.apply(R.string.menu_copy_to_clipboard), 31, 4096));
        keyboardShortcutGroup.addItem(new KeyboardShortcutInfo(intFunction.apply(R.string.menu_paste_from_clipboard), 50, 4096));
        keyboardShortcutGroup.addItem(new KeyboardShortcutInfo(intFunction.apply(R.string.menu_create_dir), 33, 4096));
        keyboardShortcutGroup.addItem(new KeyboardShortcutInfo(intFunction.apply(R.string.menu_select_all), 29, 4096));
        keyboardShortcutGroup.addItem(new KeyboardShortcutInfo(intFunction.apply(R.string.menu_new_window), 42, 4096));
        list.add(keyboardShortcutGroup);
    }

    @Override
    public void showContextMenu(Fragment fragment, View view, float f, float f2) {
        fragment.registerForContextMenu(view);
        view.showContextMenu(f, f2);
        fragment.unregisterForContextMenu(view);
    }

    @Override
    public void inflateContextMenuForContainer(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.container_context_menu, menu);
        updateContextMenuForContainer(menu);
    }

    @Override
    public void inflateContextMenuForDocs(Menu menu, MenuInflater menuInflater, MenuManager.SelectionDetails selectionDetails) {
        boolean zContainsDirectories = selectionDetails.containsDirectories();
        boolean zContainsFiles = selectionDetails.containsFiles();
        if (!zContainsDirectories) {
            menuInflater.inflate(R.menu.file_context_menu, menu);
            updateContextMenuForFiles(menu, selectionDetails);
        } else if (!zContainsFiles) {
            menuInflater.inflate(R.menu.dir_context_menu, menu);
            updateContextMenuForDirs(menu, selectionDetails);
        } else {
            menuInflater.inflate(R.menu.mixed_context_menu, menu);
            updateContextMenu(menu, selectionDetails);
        }
    }

    @Override
    protected void updateSettings(MenuItem menuItem, RootInfo rootInfo) {
        menuItem.setVisible(true);
        menuItem.setEnabled(rootInfo.hasSettings());
    }

    @Override
    protected void updateEject(MenuItem menuItem, RootInfo rootInfo) {
        menuItem.setVisible(rootInfo.supportsEject());
        menuItem.setEnabled(!rootInfo.ejecting);
    }

    @Override
    protected void updateSettings(MenuItem menuItem) {
        boolean zHasRootSettings = this.mDirDetails.hasRootSettings();
        menuItem.setVisible(zHasRootSettings);
        menuItem.setEnabled(zHasRootSettings);
    }

    @Override
    protected void updateNewWindow(MenuItem menuItem) {
        menuItem.setVisible(true);
    }

    @Override
    protected void updateOpenInContextMenu(MenuItem menuItem, MenuManager.SelectionDetails selectionDetails) {
        menuItem.setVisible(true);
        menuItem.setEnabled(selectionDetails.size() == 1 && !selectionDetails.containsPartialFiles());
    }

    @Override
    protected void updateOpenWith(MenuItem menuItem, MenuManager.SelectionDetails selectionDetails) {
        menuItem.setVisible(true);
        menuItem.setEnabled(selectionDetails.canOpenWith());
    }

    @Override
    protected void updateOpenInNewWindow(MenuItem menuItem, MenuManager.SelectionDetails selectionDetails) {
        menuItem.setVisible(true);
        menuItem.setEnabled(selectionDetails.size() == 1 && !selectionDetails.containsPartialFiles());
    }

    @Override
    protected void updateOpenInNewWindow(MenuItem menuItem, RootInfo rootInfo) {
    }

    @Override
    protected void updateMoveTo(MenuItem menuItem, MenuManager.SelectionDetails selectionDetails) {
        menuItem.setVisible(true);
        menuItem.setEnabled(!selectionDetails.containsPartialFiles() && selectionDetails.canDelete());
    }

    @Override
    protected void updateCopyTo(MenuItem menuItem, MenuManager.SelectionDetails selectionDetails) {
        menuItem.setVisible(true);
        menuItem.setEnabled((selectionDetails.containsPartialFiles() || selectionDetails.canExtract()) ? false : true);
    }

    @Override
    protected void updateCompress(MenuItem menuItem, MenuManager.SelectionDetails selectionDetails) {
        boolean z = !this.mDirDetails.canCreateDoc();
        menuItem.setVisible(this.mFeatures.isArchiveCreationEnabled());
        menuItem.setEnabled((z || selectionDetails.containsPartialFiles() || selectionDetails.canExtract()) ? false : true);
    }

    @Override
    protected void updateExtractTo(MenuItem menuItem, MenuManager.SelectionDetails selectionDetails) {
        boolean zCanExtract = selectionDetails.canExtract();
        menuItem.setVisible(zCanExtract);
        menuItem.setEnabled(zCanExtract);
    }

    @Override
    protected void updatePasteInto(MenuItem menuItem, MenuManager.SelectionDetails selectionDetails) {
        menuItem.setVisible(true);
        menuItem.setEnabled(selectionDetails.canPasteInto() && this.mDirDetails.hasItemsToPaste());
    }

    @Override
    protected void updatePasteInto(MenuItem menuItem, RootInfo rootInfo, DocumentInfo documentInfo) {
        menuItem.setVisible(true);
        menuItem.setEnabled(rootInfo.supportsCreate() && documentInfo != null && documentInfo.isCreateSupported() && this.mDirDetails.hasItemsToPaste());
    }

    @Override
    protected void updateSelectAll(MenuItem menuItem) {
        menuItem.setVisible(true);
        menuItem.setEnabled(true);
    }

    @Override
    protected void updateCreateDir(MenuItem menuItem) {
        menuItem.setVisible(true);
        menuItem.setEnabled(this.mDirDetails.canCreateDirectory());
    }

    @Override
    protected void updateShare(MenuItem menuItem, MenuManager.SelectionDetails selectionDetails) {
        boolean z = (selectionDetails.containsDirectories() || selectionDetails.containsPartialFiles() || selectionDetails.canExtract()) ? false : true;
        menuItem.setVisible(z);
        menuItem.setEnabled(z);
    }

    @Override
    protected void updateDelete(MenuItem menuItem, MenuManager.SelectionDetails selectionDetails) {
        boolean zCanDelete = selectionDetails.canDelete();
        menuItem.setVisible(zCanDelete);
        menuItem.setEnabled(zCanDelete);
    }

    @Override
    protected void updateRename(MenuItem menuItem, MenuManager.SelectionDetails selectionDetails) {
        menuItem.setVisible(true);
        menuItem.setEnabled(!selectionDetails.containsPartialFiles() && selectionDetails.canRename());
    }

    @Override
    protected void updateInspect(MenuItem menuItem) {
        boolean zIsInspectorEnabled = this.mFeatures.isInspectorEnabled();
        menuItem.setVisible(zIsInspectorEnabled);
        menuItem.setEnabled(zIsInspectorEnabled && this.mState.stack.peek() != null);
    }

    @Override
    protected void updateInspect(MenuItem menuItem, MenuManager.SelectionDetails selectionDetails) {
        boolean zIsInspectorEnabled = this.mFeatures.isInspectorEnabled();
        menuItem.setVisible(zIsInspectorEnabled);
        menuItem.setEnabled(zIsInspectorEnabled && selectionDetails.size() <= 1);
    }

    @Override
    protected void updateViewInOwner(MenuItem menuItem, MenuManager.SelectionDetails selectionDetails) {
        if (selectionDetails.canViewInOwner()) {
            menuItem.setVisible(true);
            menuItem.setEnabled(true);
            menuItem.setTitle(this.mContext.getResources().getString(R.string.menu_view_in_owner, this.mAppNameLookup.lookup(this.mUriLookup.lookup(this.mSelectionManager.getSelection().iterator().next()).getAuthority())));
            return;
        }
        menuItem.setVisible(false);
    }
}
