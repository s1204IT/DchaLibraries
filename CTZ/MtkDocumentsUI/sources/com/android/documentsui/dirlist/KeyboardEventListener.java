package com.android.documentsui.dirlist;

import android.view.KeyEvent;
import com.android.documentsui.selection.ItemDetailsLookup;

public abstract class KeyboardEventListener {
    public abstract boolean onKey(ItemDetailsLookup.ItemDetails itemDetails, int i, KeyEvent keyEvent);
}
