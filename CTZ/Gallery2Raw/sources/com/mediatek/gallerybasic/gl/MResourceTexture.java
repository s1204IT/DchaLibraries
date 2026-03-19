package com.mediatek.gallerybasic.gl;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class MResourceTexture extends MUploadedTexture {
    protected final Context mContext;
    protected final int mResId;
    protected final Resources mResources;

    public MResourceTexture(Context context, int i) {
        com.android.gallery3d.common.Utils.assertTrue(context != null);
        this.mContext = context;
        this.mResources = this.mContext.getResources();
        this.mResId = i;
        setOpaque(false);
    }

    public MResourceTexture(Resources resources, int i) {
        com.android.gallery3d.common.Utils.assertTrue(resources != null);
        this.mContext = null;
        this.mResources = resources;
        this.mResId = i;
        setOpaque(false);
    }

    @Override
    protected Bitmap onGetBitmap() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeResource(this.mResources, this.mResId, options);
    }

    @Override
    protected void onFreeBitmap(Bitmap bitmap) {
        if (!inFinalizer()) {
            bitmap.recycle();
        }
    }
}
