package android.support.v4.widget;

import android.widget.EdgeEffect;
/* loaded from: a.zip:android/support/v4/widget/EdgeEffectCompatLollipop.class */
class EdgeEffectCompatLollipop {
    EdgeEffectCompatLollipop() {
    }

    public static boolean onPull(Object obj, float f, float f2) {
        ((EdgeEffect) obj).onPull(f, f2);
        return true;
    }
}
