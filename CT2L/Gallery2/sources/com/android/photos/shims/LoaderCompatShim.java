package com.android.photos.shims;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import java.util.ArrayList;

public interface LoaderCompatShim<T> {
    void deleteItemWithPath(Object obj);

    Drawable drawableForItem(T t, Drawable drawable);

    Object getPathForItem(T t);

    Uri uriForItem(T t);

    ArrayList<Uri> urisForSubItems(T t);
}
