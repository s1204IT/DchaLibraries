package com.mediatek.contacts.ext;

import android.graphics.drawable.Drawable;
import android.net.Uri;

public interface IOp01Extension {
    Drawable getArrowIcon(int i, Drawable drawable);

    boolean isVideoButtonEnabled(boolean z, Uri uri, Object... objArr);

    void resetVideoState();
}
