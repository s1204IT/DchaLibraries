package com.mediatek.contacts.ext;

import android.graphics.drawable.Drawable;
import android.net.Uri;

public class DefaultOp01Extension implements IOp01Extension {
    @Override
    public Drawable getArrowIcon(int i, Drawable drawable) {
        return drawable;
    }

    @Override
    public boolean isVideoButtonEnabled(boolean z, Uri uri, Object... objArr) {
        return z;
    }

    @Override
    public void resetVideoState() {
    }
}
