package com.android.server.wm;

import java.util.ArrayList;

class WindowList extends ArrayList<WindowState> {
    WindowList() {
    }

    WindowList(WindowList windowList) {
        super(windowList);
    }
}
