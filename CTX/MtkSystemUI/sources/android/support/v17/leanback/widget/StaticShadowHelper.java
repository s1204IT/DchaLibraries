package android.support.v17.leanback.widget;

import android.os.Build;

final class StaticShadowHelper {
    static boolean supportsShadow() {
        return Build.VERSION.SDK_INT >= 21;
    }
}
