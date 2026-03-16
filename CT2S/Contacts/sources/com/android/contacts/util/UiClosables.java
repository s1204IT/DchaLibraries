package com.android.contacts.util;

import android.widget.ListPopupWindow;

public class UiClosables {
    public static boolean closeQuietly(ListPopupWindow popup) {
        if (popup == null || !popup.isShowing()) {
            return false;
        }
        popup.dismiss();
        return true;
    }
}
