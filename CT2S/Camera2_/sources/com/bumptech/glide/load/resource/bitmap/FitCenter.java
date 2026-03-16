package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;

public class FitCenter implements Transformation<Bitmap> {
    private BitmapPool pool;

    public FitCenter(BitmapPool pool) {
        this.pool = pool;
    }

    @Override
    public Resource<Bitmap> transform(Resource<Bitmap> resource, int outWidth, int outHeight) {
        if (outWidth <= 0 || outHeight <= 0) {
            throw new IllegalArgumentException("Cannot fit center image to within width=" + outWidth + " or height=" + outHeight);
        }
        Bitmap transformed = TransformationUtils.fitCenter(resource.get(), this.pool, outWidth, outHeight);
        return transformed == resource.get() ? resource : new BitmapResource(transformed, this.pool);
    }

    @Override
    public String getId() {
        return "FitCenter.com.bumptech.glide.load.resource.bitmap";
    }
}
