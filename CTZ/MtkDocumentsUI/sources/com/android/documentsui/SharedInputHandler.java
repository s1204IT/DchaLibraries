package com.android.documentsui;

import android.util.Log;
import android.view.KeyEvent;
import com.android.documentsui.base.Events;
import com.android.documentsui.base.Features;
import com.android.documentsui.base.Procedure;
import com.android.documentsui.base.SharedMinimal;
import com.android.documentsui.dirlist.FocusHandler;
import com.android.documentsui.selection.SelectionHelper;

public class SharedInputHandler {
    private final Procedure mDirPopper;
    private final Features mFeatures;
    private final FocusHandler mFocusManager;
    private final Procedure mSearchCanceler;
    private final SelectionHelper mSelectionMgr;

    public SharedInputHandler(FocusHandler focusHandler, SelectionHelper selectionHelper, Procedure procedure, Procedure procedure2, Features features) {
        this.mFocusManager = focusHandler;
        this.mSearchCanceler = procedure;
        this.mSelectionMgr = selectionHelper;
        this.mDirPopper = procedure2;
        this.mFeatures = features;
    }

    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (i == 4) {
            return onBack();
        }
        if (i == 61) {
            return onTab();
        }
        if (i == 67) {
            return onDelete();
        }
        if (i == 111) {
            return onEscape();
        }
        if (Events.isNavigationKeyCode(i)) {
            this.mFocusManager.focusDirectoryList();
            return true;
        }
        return false;
    }

    private boolean onTab() {
        if (!this.mFeatures.isSystemKeyboardNavigationEnabled()) {
            this.mFocusManager.advanceFocusArea();
            return true;
        }
        return false;
    }

    private boolean onDelete() {
        this.mDirPopper.run();
        return true;
    }

    private boolean onBack() {
        if (this.mSearchCanceler.run()) {
            return true;
        }
        if (this.mSelectionMgr.hasSelection()) {
            if (SharedMinimal.DEBUG) {
                Log.d("SharedInputHandler", "Back pressed. Clearing existing selection.");
            }
            this.mSelectionMgr.clearSelection();
            return true;
        }
        return this.mDirPopper.run();
    }

    private boolean onEscape() {
        if (this.mSearchCanceler.run() || !this.mSelectionMgr.hasSelection()) {
            return true;
        }
        if (SharedMinimal.DEBUG) {
            Log.d("SharedInputHandler", "ESC pressed. Clearing existing selection.");
        }
        this.mSelectionMgr.clearSelection();
        return true;
    }
}
