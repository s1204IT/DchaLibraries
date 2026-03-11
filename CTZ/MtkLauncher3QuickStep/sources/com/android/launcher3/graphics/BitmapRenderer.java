package com.android.launcher3.graphics;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Picture;
import com.android.launcher3.Utilities;

public class BitmapRenderer {
    public static final boolean USE_HARDWARE_BITMAP = Utilities.ATLEAST_P;

    public interface Renderer {
        void draw(Canvas canvas);
    }

    public static Bitmap createSoftwareBitmap(int i, int i2, Renderer renderer) {
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(i, i2, Bitmap.Config.ARGB_8888);
        renderer.draw(new Canvas(bitmapCreateBitmap));
        return bitmapCreateBitmap;
    }

    @TargetApi(28)
    public static Bitmap createHardwareBitmap(int i, int i2, Renderer renderer) {
        if (!USE_HARDWARE_BITMAP) {
            return createSoftwareBitmap(i, i2, renderer);
        }
        Picture picture = new Picture();
        renderer.draw(picture.beginRecording(i, i2));
        picture.endRecording();
        return Bitmap.createBitmap(picture);
    }
}
