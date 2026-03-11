package android.support.design.widget;

import android.graphics.Outline;

class CircularBorderDrawableLollipop extends CircularBorderDrawable {
    CircularBorderDrawableLollipop() {
    }

    @Override
    public void getOutline(Outline outline) {
        copyBounds(this.rect);
        outline.setOval(this.rect);
    }
}
