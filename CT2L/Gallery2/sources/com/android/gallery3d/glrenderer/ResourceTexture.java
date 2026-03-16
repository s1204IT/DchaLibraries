package com.android.gallery3d.glrenderer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import junit.framework.Assert;

public class ResourceTexture extends UploadedTexture {
    protected final Context mContext;
    protected final int mResId;

    public ResourceTexture(Context context, int resId) {
        Assert.assertNotNull(context);
        this.mContext = context;
        this.mResId = resId;
        setOpaque(false);
    }

    @Override
    protected Bitmap onGetBitmap() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeResource(this.mContext.getResources(), this.mResId, options);
    }

    @Override
    protected void onFreeBitmap(Bitmap bitmap) {
        if (!inFinalizer()) {
            bitmap.recycle();
        }
    }
}
