package androidx.slice.core;

import android.support.v4.graphics.drawable.IconCompat;

public interface SliceAction {
    IconCompat getIcon();

    int getImageMode();

    boolean isToggle();
}
