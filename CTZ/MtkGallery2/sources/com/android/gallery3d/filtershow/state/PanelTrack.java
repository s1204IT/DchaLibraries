package com.android.gallery3d.filtershow.state;

import android.graphics.Point;
import android.view.View;
import android.widget.Adapter;

public interface PanelTrack {
    void checkEndState();

    void fillContent(boolean z);

    int findChild(View view);

    View findChildAt(int i, int i2);

    Adapter getAdapter();

    View getChildAt(int i);

    StateView getCurrentView();

    int getOrientation();

    Point getTouchPoint();

    void setCurrentView(View view);

    void setExited(boolean z);
}
