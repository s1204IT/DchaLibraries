package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;

public class CenterCrop implements Transformation<Bitmap> {
    private BitmapPool pool;

    public CenterCrop(BitmapPool pool) {
        this.pool = pool;
    }

    @Override
    public Resource<Bitmap> transform(Resource<Bitmap> resource, int outWidth, int outHeight) {
        if (outWidth <= 0 || outHeight <= 0) {
            throw new IllegalArgumentException("Cannot center crop image to width=" + outWidth + " and height=" + outHeight);
        }
        Bitmap toReuse = this.pool.get(outWidth, outHeight, resource.get().getConfig());
        Bitmap transformed = TransformationUtils.centerCrop(toReuse, resource.get(), outWidth, outHeight);
        if (toReuse != null && toReuse != transformed && !this.pool.put(toReuse)) {
            toReuse.recycle();
        }
        return transformed == resource.get() ? resource : new BitmapResource(transformed, this.pool);
    }

    @Override
    public String getId() {
        return "CenterCrop.com.bumptech.glide.load.resource.bitmap";
    }
}
