package com.android.documentsui;

import android.app.Activity;
import android.text.TextUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import com.android.documentsui.MenuManager;
import com.android.documentsui.base.EventHandler;
import com.android.documentsui.base.Menus;
import com.android.documentsui.base.SharedMinimal;
import com.android.documentsui.selection.Selection;
import com.android.documentsui.selection.SelectionHelper;
import com.android.documentsui.ui.MessageBuilder;

public class ActionModeController extends SelectionHelper.SelectionObserver implements ActionMode.Callback, ActionModeAddons {
    static final boolean $assertionsDisabled = false;
    private ActionMode mActionMode;
    private final Activity mActivity;
    private Menu mMenu;
    private final MenuManager mMenuManager;
    private final MessageBuilder mMessages;
    private final ContentScope mScope = new ContentScope();
    private final Selection mSelected = new Selection();
    private final SelectionHelper mSelectionMgr;

    @FunctionalInterface
    private interface AccessibilityImportanceSetter {
        void setAccessibilityImportance(int i, int... iArr);
    }

    public ActionModeController(Activity activity, SelectionHelper selectionHelper, MenuManager menuManager, MessageBuilder messageBuilder) {
        this.mActivity = activity;
        this.mSelectionMgr = selectionHelper;
        this.mMenuManager = menuManager;
        this.mMessages = messageBuilder;
    }

    @Override
    public void onSelectionChanged() {
        this.mSelectionMgr.copySelection(this.mSelected);
        if (this.mSelected.size() > 0) {
            if (this.mActionMode == null) {
                if (SharedMinimal.DEBUG) {
                    Log.d("ActionModeController", "Starting action mode.");
                }
                this.mActionMode = this.mActivity.startActionMode(this);
            }
            updateActionMenu();
        } else if (this.mActionMode != null) {
            if (SharedMinimal.DEBUG) {
                Log.d("ActionModeController", "Finishing action mode.");
            }
            this.mActionMode.finish();
        }
        if (this.mActionMode != null) {
            String quantityString = this.mMessages.getQuantityString(R.plurals.elements_selected, this.mSelected.size());
            this.mActionMode.setTitle(quantityString);
            this.mActivity.getWindow().setTitle(quantityString);
        }
    }

    @Override
    public void onSelectionRestored() {
        onSelectionChanged();
    }

    @Override
    public void onDestroyActionMode(ActionMode actionMode) {
        if (this.mActionMode == null && SharedMinimal.DEBUG) {
            Log.w("ActionModeController", "Received call to destroy action mode on alien mode object.");
        }
        if (SharedMinimal.DEBUG) {
            Log.d("ActionModeController", "Handling action mode destroyed.");
        }
        this.mActionMode = null;
        this.mMenu = null;
        this.mSelectionMgr.clearSelection();
        this.mActivity.getWindow().setTitle(this.mActivity.getTitle());
        this.mScope.accessibilityImportanceSetter.setAccessibilityImportance(0, R.id.toolbar, R.id.roots_toolbar);
    }

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        int size = this.mSelectionMgr.getSelection().size();
        actionMode.getMenuInflater().inflate(R.menu.action_mode_menu, menu);
        actionMode.setTitle(TextUtils.formatSelectedCount(size));
        if (size <= 0) {
            return false;
        }
        this.mScope.accessibilityImportanceSetter.setAccessibilityImportance(4, R.id.toolbar, R.id.roots_toolbar);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        this.mMenu = menu;
        updateActionMenu();
        return true;
    }

    private void updateActionMenu() {
        this.mMenuManager.updateActionMenu(this.mMenu, this.mScope.selectionDetails);
        Menus.disableHiddenItems(this.mMenu, new MenuItem[0]);
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
        return this.mScope.menuItemClicker.accept(menuItem);
    }

    private static void setImportantForAccessibility(Activity activity, int i, int[] iArr) {
        for (int i2 : iArr) {
            View viewFindViewById = activity.findViewById(i2);
            if (viewFindViewById != null) {
                viewFindViewById.setImportantForAccessibility(i);
            }
        }
    }

    @Override
    public void finishActionMode() {
        if (this.mActionMode != null) {
            this.mActionMode.finish();
            this.mActionMode = null;
        } else {
            Log.w("ActionModeController", "Tried to finish a null action mode.");
        }
    }

    @Override
    public void finishOnConfirmed(int i) {
        if (i == 0) {
            finishActionMode();
        }
    }

    public ActionModeController reset(MenuManager.SelectionDetails selectionDetails, EventHandler<MenuItem> eventHandler) {
        if (this.mActionMode != null) {
            Log.w("ActionModeController", "mActionMode is already not null.");
        }
        if (this.mMenu != null) {
            Log.w("ActionModeController", "mMenu is already not null.");
        }
        this.mScope.menuItemClicker = eventHandler;
        this.mScope.selectionDetails = selectionDetails;
        this.mScope.accessibilityImportanceSetter = new AccessibilityImportanceSetter() {
            @Override
            public final void setAccessibilityImportance(int i, int[] iArr) {
                ActionModeController.setImportantForAccessibility(this.f$0.mActivity, i, iArr);
            }
        };
        return this;
    }

    private static final class ContentScope {
        private AccessibilityImportanceSetter accessibilityImportanceSetter;
        private EventHandler<MenuItem> menuItemClicker;
        private MenuManager.SelectionDetails selectionDetails;

        private ContentScope() {
        }
    }
}
