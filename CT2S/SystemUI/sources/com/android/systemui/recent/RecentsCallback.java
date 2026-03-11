package com.android.systemui.recent;

import android.view.View;

public interface RecentsCallback {
    void dismiss();

    void handleLongPress(View view, View view2, View view3);

    void handleOnClick(View view);

    void handleSwipe(View view);
}
