package android.support.v4.graphics.drawable;

import android.graphics.drawable.Drawable;
/* loaded from: classes.dex */
class DrawableCompatApi23 {
    DrawableCompatApi23() {
    }

    public static boolean setLayoutDirection(Drawable drawable, int layoutDirection) {
        return drawable.setLayoutDirection(layoutDirection);
    }

    public static int getLayoutDirection(Drawable drawable) {
        return drawable.getLayoutDirection();
    }
}
