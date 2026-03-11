package com.android.systemui.recents.views.grid;

import android.view.View;
import com.android.systemui.shared.recents.view.AnimateableViewBounds;

class AnimateableGridViewBounds extends AnimateableViewBounds {
    public AnimateableGridViewBounds(View view, int i) {
        super(view, i);
    }

    @Override
    protected void updateClipBounds() {
    }
}
