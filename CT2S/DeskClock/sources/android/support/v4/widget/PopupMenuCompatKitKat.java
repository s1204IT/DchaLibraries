package android.support.v4.widget;

import android.view.View;
import android.widget.PopupMenu;

class PopupMenuCompatKitKat {
    public static View.OnTouchListener getDragToOpenListener(Object popupMenu) {
        return ((PopupMenu) popupMenu).getDragToOpenListener();
    }
}
