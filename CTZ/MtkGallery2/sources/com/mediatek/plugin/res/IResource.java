package com.mediatek.plugin.res;

import android.graphics.drawable.Drawable;

public interface IResource {
    Drawable getDrawable(String str);

    String getString(String str);

    String[] getString(String str, String str2);
}
