package com.android.systemui.screenshot;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

/* compiled from: GlobalScreenshot.java */
/* renamed from: com.android.systemui.screenshot.SaveImageInBackgroundData, reason: use source file name */
/* loaded from: classes.dex */
class GlobalScreenshot3 {
    Context context;
    int errorMsgResId;
    Runnable finisher;
    int iconSize;
    Bitmap image;
    Uri imageUri;
    int previewWidth;
    int previewheight;

    GlobalScreenshot3() {
    }

    void clearImage() {
        this.image = null;
        this.imageUri = null;
        this.iconSize = 0;
    }

    void clearContext() {
        this.context = null;
    }
}
