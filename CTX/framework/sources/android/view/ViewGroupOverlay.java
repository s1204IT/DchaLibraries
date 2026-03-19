package android.view;

import android.content.Context;

public class ViewGroupOverlay extends ViewOverlay {
    ViewGroupOverlay(Context context, View view) {
        super(context, view);
    }

    public void add(View view) {
        this.mOverlayViewGroup.add(view);
    }

    public void remove(View view) {
        this.mOverlayViewGroup.remove(view);
    }
}
