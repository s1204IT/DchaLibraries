package com.android.systemui;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

public class BitmapHelper {
    public static Bitmap createCircularClip(Bitmap input, int width, int height) {
        if (input == null) {
            return null;
        }
        int inWidth = input.getWidth();
        int inHeight = input.getHeight();
        Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        paint.setShader(new BitmapShader(input, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        paint.setAntiAlias(true);
        RectF srcRect = new RectF(0.0f, 0.0f, inWidth, inHeight);
        RectF dstRect = new RectF(0.0f, 0.0f, width, height);
        Matrix m = new Matrix();
        m.setRectToRect(srcRect, dstRect, Matrix.ScaleToFit.CENTER);
        canvas.setMatrix(m);
        canvas.drawCircle(inWidth / 2, inHeight / 2, inWidth / 2, paint);
        return output;
    }
}
