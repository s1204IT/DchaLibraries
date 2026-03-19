package com.android.photos;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;

class SimpleBitmapRegionDecoderWrapper implements SimpleBitmapRegionDecoder {
    BitmapRegionDecoder mDecoder;

    private SimpleBitmapRegionDecoderWrapper(BitmapRegionDecoder bitmapRegionDecoder) {
        this.mDecoder = bitmapRegionDecoder;
    }

    public static SimpleBitmapRegionDecoderWrapper newInstance(InputStream inputStream, boolean z) {
        try {
            BitmapRegionDecoder bitmapRegionDecoderNewInstance = BitmapRegionDecoder.newInstance(inputStream, z);
            if (bitmapRegionDecoderNewInstance == null) {
                return null;
            }
            return new SimpleBitmapRegionDecoderWrapper(bitmapRegionDecoderNewInstance);
        } catch (IOException e) {
            Log.w("BitmapRegionTileSource", "getting decoder failed", e);
            return null;
        }
    }

    @Override
    public int getWidth() {
        return this.mDecoder.getWidth();
    }

    @Override
    public int getHeight() {
        return this.mDecoder.getHeight();
    }

    @Override
    public Bitmap decodeRegion(Rect rect, BitmapFactory.Options options) {
        return this.mDecoder.decodeRegion(rect, options);
    }
}
