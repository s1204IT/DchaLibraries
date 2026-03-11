package com.android.quicksearchbox;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import com.android.quicksearchbox.util.NowOrLater;

public interface IconLoader {
    NowOrLater<Drawable> getIcon(String str);

    Uri getIconUri(String str);
}
