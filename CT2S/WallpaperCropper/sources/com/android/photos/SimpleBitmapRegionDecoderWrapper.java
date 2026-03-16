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

    private SimpleBitmapRegionDecoderWrapper(BitmapRegionDecoder decoder) {
        this.mDecoder = decoder;
    }

    public static SimpleBitmapRegionDecoderWrapper newInstance(InputStream is, boolean isShareable) {
        try {
            BitmapRegionDecoder d = BitmapRegionDecoder.newInstance(is, isShareable);
            if (d != null) {
                return new SimpleBitmapRegionDecoderWrapper(d);
            }
            return null;
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
    public Bitmap decodeRegion(Rect wantRegion, BitmapFactory.Options options) {
        return this.mDecoder.decodeRegion(wantRegion, options);
    }
}
