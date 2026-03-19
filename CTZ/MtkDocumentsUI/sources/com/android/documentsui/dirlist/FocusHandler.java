package com.android.documentsui.dirlist;

import android.view.KeyEvent;
import android.view.View;

public interface FocusHandler extends View.OnFocusChangeListener {
    boolean advanceFocusArea();

    void clearFocus();

    boolean focusDirectoryList();

    void focusDocument(String str);

    String getFocusModelId();

    int getFocusPosition();

    boolean handleKey(DocumentHolder documentHolder, int i, KeyEvent keyEvent);

    boolean hasFocusedItem();
}
