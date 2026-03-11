package android.support.v4.view;

import android.view.View;

public class NestedScrollingParentHelper {
    private int mNestedScrollAxes;

    public int getNestedScrollAxes() {
        return this.mNestedScrollAxes;
    }

    public void onNestedScrollAccepted(View view, View view2, int i, int i2) {
        this.mNestedScrollAxes = i;
    }

    public void onStopNestedScroll(View view, int i) {
        this.mNestedScrollAxes = 0;
    }
}
