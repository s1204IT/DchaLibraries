package com.android.gallery3d.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.ui.TileImageView;
import com.android.photos.data.GalleryBitmapPool;
import java.util.ArrayList;

public class BitmapTileProvider implements TileImageView.TileSource {
    private final Bitmap.Config mConfig;
    private final int mImageHeight;
    private final int mImageWidth;
    private final Bitmap[] mMipmaps;
    private boolean mRecycled = false;
    private final ScreenNail mScreenNail;

    public BitmapTileProvider(Bitmap bitmap, int i) {
        this.mImageWidth = bitmap.getWidth();
        this.mImageHeight = bitmap.getHeight();
        ArrayList arrayList = new ArrayList();
        arrayList.add(bitmap);
        while (true) {
            if (bitmap.getWidth() > i || bitmap.getHeight() > i) {
                bitmap = BitmapUtils.resizeBitmapByScale(bitmap, 0.5f, false);
                arrayList.add(bitmap);
            } else {
                this.mScreenNail = new BitmapScreenNail((Bitmap) arrayList.remove(arrayList.size() - 1));
                this.mMipmaps = (Bitmap[]) arrayList.toArray(new Bitmap[arrayList.size()]);
                this.mConfig = Bitmap.Config.ARGB_8888;
                return;
            }
        }
    }

    @Override
    public ScreenNail getScreenNail() {
        return this.mScreenNail;
    }

    @Override
    public int getImageHeight() {
        return this.mImageHeight;
    }

    @Override
    public int getImageWidth() {
        return this.mImageWidth;
    }

    @Override
    public int getLevelCount() {
        return this.mMipmaps.length;
    }

    @Override
    public Bitmap getTile(int i, int i2, int i3, int i4) {
        int i5 = i2 >> i;
        int i6 = i3 >> i;
        Bitmap bitmapCreateBitmap = GalleryBitmapPool.getInstance().get(i4, i4);
        if (bitmapCreateBitmap == null) {
            bitmapCreateBitmap = Bitmap.createBitmap(i4, i4, this.mConfig);
        } else {
            bitmapCreateBitmap.eraseColor(0);
        }
        new Canvas(bitmapCreateBitmap).drawBitmap(this.mMipmaps[i], -i5, -i6, (Paint) null);
        return bitmapCreateBitmap;
    }

    public void recycle() {
        if (this.mRecycled) {
            return;
        }
        this.mRecycled = true;
        for (Bitmap bitmap : this.mMipmaps) {
            BitmapUtils.recycleSilently(bitmap);
        }
        if (this.mScreenNail != null) {
            this.mScreenNail.recycle();
        }
    }
}
