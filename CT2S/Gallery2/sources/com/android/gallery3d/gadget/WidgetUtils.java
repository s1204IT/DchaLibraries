package com.android.gallery3d.gadget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;
import com.android.gallery3d.R;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.util.ThreadPool;

public class WidgetUtils {
    private static int sStackPhotoWidth = 220;
    private static int sStackPhotoHeight = 170;

    public static void initialize(Context context) {
        Resources r = context.getResources();
        sStackPhotoWidth = r.getDimensionPixelSize(R.dimen.stack_photo_width);
        sStackPhotoHeight = r.getDimensionPixelSize(R.dimen.stack_photo_height);
    }

    public static Bitmap createWidgetBitmap(MediaItem image) {
        Bitmap bitmap = image.requestImage(1).run(ThreadPool.JOB_CONTEXT_STUB);
        if (bitmap != null) {
            return createWidgetBitmap(bitmap, image.getRotation());
        }
        Log.w("WidgetUtils", "fail to get image of " + image.toString());
        return null;
    }

    public static Bitmap createWidgetBitmap(Bitmap bitmap, int rotation) {
        float scale;
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        if (((rotation / 90) & 1) == 0) {
            scale = Math.max(sStackPhotoWidth / w, sStackPhotoHeight / h);
        } else {
            scale = Math.max(sStackPhotoWidth / h, sStackPhotoHeight / w);
        }
        Bitmap target = Bitmap.createBitmap(sStackPhotoWidth, sStackPhotoHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(target);
        canvas.translate(sStackPhotoWidth / 2, sStackPhotoHeight / 2);
        canvas.rotate(rotation);
        canvas.scale(scale, scale);
        Paint paint = new Paint(6);
        canvas.drawBitmap(bitmap, (-w) / 2, (-h) / 2, paint);
        return target;
    }
}
