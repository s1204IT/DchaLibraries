package com.android.gallery3d.gadget;

import android.graphics.Bitmap;
import android.net.Uri;
import com.android.gallery3d.data.ContentListener;

public interface WidgetSource {
    void close();

    void forceNotifyDirty();

    Uri getContentUri(int i);

    Bitmap getImage(int i);

    void reload();

    void setContentListener(ContentListener contentListener);

    int size();
}
